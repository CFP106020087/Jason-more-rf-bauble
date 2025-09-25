package com.moremod.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.moremod.entity.fx.EntityLightningArc;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ItemArcGaze extends Item {

    private static final UUID DMG_UUID   = UUID.fromString("c8d2b0b0-6b2d-4f1a-9b5e-2f41a5b7a111");
    private static final UUID SPEED_UUID = UUID.fromString("9f2b9a7b-2cc2-4c61-a7f3-58b3b1ddc222");

    // 视野参数
    private static final double VIEW_RANGE = 32.0D;         // 视野范围
    private static final double VIEW_ANGLE = 60.0D;         // 视野角度（度）
    private static final double VIEW_ANGLE_RAD = Math.toRadians(VIEW_ANGLE);
    private static final double VIEW_DOT_THRESHOLD = Math.cos(VIEW_ANGLE_RAD / 2);  // 点积阈值

    // 冷却参数
    private static final int    MAX_DURATION_T = 2000;      // 最大持续时间（100秒）
    private static final int    MIN_COOLDOWN_T = 20;        // 最短冷却（1秒）
    private static final int    MAX_COOLDOWN_T = 200;       // 最长冷却（10秒）
    private static final double COOLDOWN_RATIO = 0.1;        // 冷却时间 = 使用时间 * 0.1

    // 战斗参数
    private static final float  DAMAGE_PER_TICK = 5F;       // 每tick基础伤害
    private static final float  MAX_HP_PERCENT = 0.05F;     // 每tick 5%最大生命值
    private static final int    SOUND_INTERVAL = 10;        // 声音间隔
    private static final double AOE_RANGE = 3.0D;           // 范围伤害半径

    // 智能参数
    private static final int    CHECK_INTERVAL = 10;        // 检查间隔（ticks）
    private static final int    NO_TARGET_THRESHOLD = 20;   // 无目标阈值（1秒）

    // 存储活跃的电弧效果
    private static final Map<UUID, ArcData> activeArcs = new HashMap<>();

    /**
     * 自定义电弧穿透伤害源
     */
    public static class ArcPiercingDamage extends EntityDamageSource {
        private final EntityPlayer sourcePlayer;

        public ArcPiercingDamage(EntityPlayer player) {
            super("arc_piercing", player);
            this.sourcePlayer = player;
            this.setDamageBypassesArmor();
            this.setMagicDamage();
        }

        @Override
        public boolean isUnblockable() {
            return true;
        }

        @Override
        public Entity getTrueSource() {
            return sourcePlayer;
        }

        public boolean isArcDamage() {
            return true;
        }
    }

    private static class ArcData {
        EntityLivingBase primaryTarget;
        List<EntityLivingBase> aoeTargets;
        EntityLightningArc arcEntity;
        int ticksRemaining;
        int ticksUsed;           // 已使用时间
        int soundTicker;
        int particleTicker;
        int checkTicker;         // 检查计时器
        int noTargetTicks;       // 无目标计时
        float totalDamageDealt;
        boolean isAoe;
        int killCount;
        boolean autoMode;        // 自动模式
        Set<Integer> killedEntities;  // 已击杀实体ID

        ArcData(EntityLivingBase target, EntityLightningArc arc, int duration, boolean aoe) {
            this.primaryTarget = target;
            this.arcEntity = arc;
            this.ticksRemaining = duration;
            this.ticksUsed = 0;
            this.soundTicker = 0;
            this.particleTicker = 0;
            this.checkTicker = 0;
            this.noTargetTicks = 0;
            this.totalDamageDealt = 0;
            this.isAoe = aoe;
            this.aoeTargets = new ArrayList<>();
            this.killCount = 0;
            this.autoMode = true;
            this.killedEntities = new HashSet<>();
        }
    }

    public ItemArcGaze() {
        setMaxStackSize(1);
        setTranslationKey("arc_gaze");
        setRegistryName("arc_gaze");
        setNoRepair();
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(DMG_UUID, "ArcGaze damage", 8.0D, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(SPEED_UUID, "ArcGaze speed", -2.4D, 0));
        }
        return map;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 检查是否在冷却中
        if (player.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        player.swingArm(hand);

        if (!world.isRemote) {
            // 寻找视野内的目标
            EntityLivingBase target = findBestTargetInView(world, player);

            if (target != null && target.isEntityAlive()) {
                boolean enableAoe = player.isSneaking();

                // 创建电弧实体
                EntityLightningArc arc = new EntityLightningArc(world, player, target, MAX_DURATION_T);
                world.spawnEntity(arc);

                // 创建电弧数据
                ArcData arcData = new ArcData(target, arc, MAX_DURATION_T, enableAoe);

                if (enableAoe) {
                    updateAoeTargets(world, player, arcData);
                }

                activeArcs.put(player.getUniqueID(), arcData);

                // 初始化NBT
                if (!stack.hasTagCompound()) {
                    stack.setTagCompound(new NBTTagCompound());
                }
                NBTTagCompound nbt = stack.getTagCompound();
                nbt.setBoolean("ArcActive", true);
                nbt.setInteger("ArcTicks", MAX_DURATION_T);
                nbt.setInteger("TargetID", target.getEntityId());
                nbt.setBoolean("AoeMode", enableAoe);
                nbt.setFloat("TotalDamage", 0F);
                nbt.setInteger("KillCount", 0);
                nbt.setInteger("TicksUsed", 0);

                // 播放激活音效
                world.playSound(null, player.getPosition(),
                        SoundEvents.ENTITY_LIGHTNING_IMPACT,
                        SoundCategory.PLAYERS, 1.2F, 0.6F);

                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "§a⚡ 电弧激活 - 智能模式"), true);
            } else {
                // 没有找到目标
                world.playSound(null, player.getPosition(),
                        SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                        SoundCategory.PLAYERS, 0.4F, 1.2F);

                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "§c视野内无有效目标"), true);

                // 短暂冷却
                player.getCooldownTracker().setCooldown(this, 20);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        UUID playerUUID = player.getUniqueID();

        // 检查物品是否还在玩家身上
        if (!isItemInInventory(player, stack)) {
            if (activeArcs.containsKey(playerUUID)) {
                endArcAndApplyCooldown(player, stack, playerUUID);
            }
            return;
        }

        if (activeArcs.containsKey(playerUUID)) {
            ArcData arcData = activeArcs.get(playerUUID);

            // 更新计时器
            arcData.ticksRemaining--;
            arcData.ticksUsed++;
            arcData.soundTicker++;
            arcData.particleTicker++;
            arcData.checkTicker++;

            // 定期检查视野内的目标
            if (arcData.checkTicker >= CHECK_INTERVAL) {
                arcData.checkTicker = 0;

                // 检查当前目标是否仍在视野内
                if (arcData.primaryTarget != null && !arcData.primaryTarget.isDead) {
                    if (!isInPlayerView(player, arcData.primaryTarget)) {
                        // 目标离开视野，寻找新目标
                        EntityLivingBase newTarget = findBestTargetInView(world, player);
                        if (newTarget != null) {
                            switchToNewTarget(world, player, arcData, newTarget);
                        } else {
                            arcData.noTargetTicks += CHECK_INTERVAL;
                        }
                    }
                }
            }

            // 检查主目标是否死亡
            if (arcData.primaryTarget != null && arcData.primaryTarget.isDead) {
                arcData.killCount++;
                arcData.killedEntities.add(arcData.primaryTarget.getEntityId());

                // 立即寻找视野内的下一个目标
                EntityLivingBase nextTarget = findBestTargetInView(world, player, arcData.killedEntities);

                if (nextTarget != null) {
                    switchToNewTarget(world, player, arcData, nextTarget);
                    arcData.noTargetTicks = 0;

                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            String.format("§e⚡ 锁定新目标 [击杀: %d]", arcData.killCount)), true);
                } else {
                    // 没有找到新目标
                    arcData.primaryTarget = null;
                    arcData.noTargetTicks = NO_TARGET_THRESHOLD;
                }
            }

            // 如果没有目标超过阈值，结束电弧
            if (arcData.noTargetTicks >= NO_TARGET_THRESHOLD ||
                    arcData.primaryTarget == null ||
                    arcData.ticksRemaining <= 0) {

                endArcAndApplyCooldown(player, stack, playerUUID);

                String endReason = arcData.noTargetTicks >= NO_TARGET_THRESHOLD ?
                        "视野清空" : "时间耗尽";

                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        String.format("§a电弧结束(%s) - 击杀: §e%d §a伤害: §c%.1f §a用时: §b%.1f秒",
                                endReason, arcData.killCount, arcData.totalDamageDealt,
                                arcData.ticksUsed / 20.0F)), true);
                return;
            }

            // 如果有有效目标，执行攻击
            if (arcData.primaryTarget != null && arcData.primaryTarget.isEntityAlive()) {
                executeArcAttack(world, player, arcData, stack);
            }

            // 更新NBT
            if (stack.hasTagCompound()) {
                NBTTagCompound nbt = stack.getTagCompound();
                nbt.setInteger("ArcTicks", arcData.ticksRemaining);
                nbt.setFloat("TotalDamage", arcData.totalDamageDealt);
                nbt.setInteger("KillCount", arcData.killCount);
                nbt.setInteger("TicksUsed", arcData.ticksUsed);
            }

        } else {
            // 清理NBT
            if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("ArcActive")) {
                cleanupNBT(stack);
            }
        }
    }

    /**
     * 执行电弧攻击
     */
    private void executeArcAttack(World world, EntityPlayer player, ArcData arcData, ItemStack stack) {
        ArcPiercingDamage piercingDamage = new ArcPiercingDamage(player);

        // 计算伤害
        float baseDamage = DAMAGE_PER_TICK;
        float percentDamage = arcData.primaryTarget.getMaxHealth() * MAX_HP_PERCENT;
        float totalDamage = baseDamage + percentDamage;

        // 血量倍率
        float healthPercent = arcData.primaryTarget.getHealth() / arcData.primaryTarget.getMaxHealth();
        if (healthPercent < 0.5F) {
            totalDamage *= 1.5F;
        }
        if (healthPercent < 0.25F) {
            totalDamage *= 2.0F;
        }

        // 对主目标造成伤害
        arcData.primaryTarget.hurtResistantTime = 0;
        arcData.primaryTarget.attackEntityFrom(piercingDamage, totalDamage);
        arcData.primaryTarget.hurtResistantTime = 0;
        arcData.totalDamageDealt += totalDamage;

        // AOE伤害
        if (arcData.isAoe) {
            executeAoeDamage(world, player, arcData, piercingDamage, totalDamage);
        }

        // 击退效果
        if (arcData.ticksUsed % 5 == 0) {
            applyKnockback(player, arcData.primaryTarget);
        }

        // 音效
        if (arcData.soundTicker >= SOUND_INTERVAL) {
            arcData.soundTicker = 0;
            playArcSound(world, arcData.primaryTarget.getPosition());
        }

        // 粒子效果
        if (arcData.particleTicker >= 2) {
            arcData.particleTicker = 0;
            spawnElectricParticles(world, arcData.primaryTarget);

            if (arcData.isAoe) {
                for (EntityLivingBase aoeTarget : arcData.aoeTargets) {
                    if (world.rand.nextInt(3) == 0) {
                        spawnElectricParticles(world, aoeTarget);
                    }
                }
            }
        }
    }

    /**
     * 结束电弧并应用动态冷却
     */
    private void endArcAndApplyCooldown(EntityPlayer player, ItemStack stack, UUID playerUUID) {
        ArcData arcData = activeArcs.get(playerUUID);
        if (arcData != null) {
            // 计算动态冷却时间
            int cooldownTicks = calculateDynamicCooldown(arcData.ticksUsed);
            player.getCooldownTracker().setCooldown(this, cooldownTicks);

            // 清理电弧
            if (arcData.arcEntity != null && !arcData.arcEntity.isDead) {
                arcData.arcEntity.setDead();
            }
            activeArcs.remove(playerUUID);

            // 显示冷却信息
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    String.format("§7冷却时间: %.1f秒", cooldownTicks / 20.0F)), true);
        }

        cleanupNBT(stack);
    }

    /**
     * 计算动态冷却时间
     */
    private int calculateDynamicCooldown(int ticksUsed) {
        // 冷却 = 使用时间 * 0.1，最短1秒，最长10秒
        int cooldown = (int)(ticksUsed * COOLDOWN_RATIO);
        return Math.max(MIN_COOLDOWN_T, Math.min(MAX_COOLDOWN_T, cooldown));
    }

    /**
     * 寻找视野内最佳目标
     */
    @Nullable
    private EntityLivingBase findBestTargetInView(World world, EntityPlayer player) {
        return findBestTargetInView(world, player, new HashSet<>());
    }

    @Nullable
    private EntityLivingBase findBestTargetInView(World world, EntityPlayer player, Set<Integer> excludeIds) {
        Vec3d eyePos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d lookVec = player.getLookVec();

        // 获取视野范围内的所有实体
        AxisAlignedBB searchBox = player.getEntityBoundingBox().grow(VIEW_RANGE);
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                searchBox,
                entity -> entity != player &&
                        entity.isEntityAlive() &&
                        entity.canBeAttackedWithItem() &&
                        !excludeIds.contains(entity.getEntityId()) &&
                        player.getDistance(entity) <= VIEW_RANGE
        );

        EntityLivingBase bestTarget = null;
        double bestScore = Double.MIN_VALUE;

        for (EntityLivingBase candidate : candidates) {
            // 检查是否在视野内
            if (!isInPlayerView(player, candidate)) {
                continue;
            }

            // 检查是否有视线
            if (!canSeeEntity(world, player, candidate)) {
                continue;
            }

            // 计算优先级分数
            double distance = player.getDistance(candidate);
            Vec3d toTarget = candidate.getPositionVector()
                    .add(0, candidate.height * 0.5, 0)
                    .subtract(eyePos)
                    .normalize();
            double dotProduct = lookVec.dotProduct(toTarget);

            // 分数 = 准心对准度 * 1000 - 距离
            double score = dotProduct * 1000 - distance;

            // 优先攻击低血量目标
            float healthPercent = candidate.getHealth() / candidate.getMaxHealth();
            if (healthPercent < 0.3F) {
                score += 500;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    /**
     * 检查实体是否在玩家视野内
     */
    private boolean isInPlayerView(EntityPlayer player, EntityLivingBase target) {
        Vec3d eyePos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d lookVec = player.getLookVec();
        Vec3d toTarget = target.getPositionVector()
                .add(0, target.height * 0.5, 0)
                .subtract(eyePos);

        double distance = toTarget.length();
        if (distance > VIEW_RANGE) {
            return false;
        }

        toTarget = toTarget.normalize();
        double dotProduct = lookVec.dotProduct(toTarget);

        // 检查是否在视角范围内
        return dotProduct >= VIEW_DOT_THRESHOLD;
    }

    /**
     * 检查是否能看到实体
     */
    private boolean canSeeEntity(World world, EntityPlayer player, EntityLivingBase target) {
        Vec3d eyePos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d targetPos = target.getPositionVector().add(0, target.height * 0.5, 0);

        RayTraceResult result = world.rayTraceBlocks(eyePos, targetPos, false, true, false);
        return result == null || result.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    /**
     * 切换到新目标
     */
    private void switchToNewTarget(World world, EntityPlayer player, ArcData arcData, EntityLivingBase newTarget) {
        arcData.primaryTarget = newTarget;

        // 清理旧电弧
        if (arcData.arcEntity != null && !arcData.arcEntity.isDead) {
            arcData.arcEntity.setDead();
        }

        // 创建新电弧
        arcData.arcEntity = new EntityLightningArc(world, player, newTarget, arcData.ticksRemaining);
        world.spawnEntity(arcData.arcEntity);

        // 更新AOE目标
        if (arcData.isAoe) {
            updateAoeTargets(world, player, arcData);
        }

        // 播放切换音效
        world.playSound(null, newTarget.getPosition(),
                SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.5F);
    }

    /**
     * 更新AOE目标
     */
    private void updateAoeTargets(World world, EntityPlayer player, ArcData arcData) {
        arcData.aoeTargets.clear();

        if (arcData.primaryTarget == null) return;

        AxisAlignedBB aoeBox = arcData.primaryTarget.getEntityBoundingBox().grow(AOE_RANGE);
        List<EntityLivingBase> nearbyEntities = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                aoeBox,
                entity -> entity != player &&
                        entity != arcData.primaryTarget &&
                        entity.isEntityAlive() &&
                        !arcData.killedEntities.contains(entity.getEntityId())
        );
        arcData.aoeTargets.addAll(nearbyEntities);
    }

    /**
     * 执行AOE伤害
     */
    private void executeAoeDamage(World world, EntityPlayer player, ArcData arcData,
                                  ArcPiercingDamage damage, float baseDamage) {
        arcData.aoeTargets.removeIf(e -> e.isDead);

        float aoeDamage = baseDamage * 0.5F;
        for (EntityLivingBase aoeTarget : arcData.aoeTargets) {
            if (aoeTarget.getDistance(arcData.primaryTarget) <= AOE_RANGE + 1) {
                aoeTarget.hurtResistantTime = 0;
                aoeTarget.attackEntityFrom(damage, aoeDamage);
                aoeTarget.hurtResistantTime = 0;
                arcData.totalDamageDealt += aoeDamage;

                if (arcData.particleTicker % 5 == 0) {
                    createChainLightning(world, arcData.primaryTarget, aoeTarget);
                }

                // 记录击杀
                if (aoeTarget.isDead) {
                    arcData.killCount++;
                    arcData.killedEntities.add(aoeTarget.getEntityId());
                }
            }
        }
    }

    /**
     * 检查物品是否在背包中
     */
    private boolean isItemInInventory(EntityPlayer player, ItemStack stack) {
        for (ItemStack invStack : player.inventory.mainInventory) {
            if (invStack == stack) return true;
        }
        for (ItemStack invStack : player.inventory.offHandInventory) {
            if (invStack == stack) return true;
        }
        return false;
    }

    /**
     * 应用击退效果
     */
    private void applyKnockback(EntityPlayer player, EntityLivingBase target) {
        Vec3d knockback = player.getPositionVector()
                .subtract(target.getPositionVector())
                .normalize()
                .scale(-0.25);
        target.motionX += knockback.x;
        target.motionY += 0.08;
        target.motionZ += knockback.z;
    }

    /**
     * 播放电弧音效
     */
    private void playArcSound(World world, BlockPos pos) {
        world.playSound(null, pos,
                SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE,
                SoundCategory.PLAYERS, 0.3F, 1.8F + world.rand.nextFloat() * 0.4F);
    }

    /**
     * 清理NBT
     */
    private void cleanupNBT(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            nbt.removeTag("ArcActive");
            nbt.removeTag("ArcTicks");
            nbt.removeTag("TargetID");
            nbt.removeTag("AoeMode");
            nbt.removeTag("TotalDamage");
            nbt.removeTag("KillCount");
            nbt.removeTag("TicksUsed");
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        if (!player.world.isRemote) {
            UUID playerUUID = player.getUniqueID();
            if (activeArcs.containsKey(playerUUID)) {
                endArcAndApplyCooldown(player, item, playerUUID);
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "§c电弧因物品丢弃而中断"), true);
            }
        }
        return true;
    }

    private void spawnElectricParticles(World world, EntityLivingBase target) {
        for (int i = 0; i < 3; i++) {
            double px = target.posX + (world.rand.nextDouble() - 0.5) * target.width;
            double py = target.posY + world.rand.nextDouble() * target.height;
            double pz = target.posZ + (world.rand.nextDouble() - 0.5) * target.width;

            world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    px, py, pz,
                    (world.rand.nextDouble() - 0.5) * 0.5,
                    world.rand.nextDouble() * 0.5,
                    (world.rand.nextDouble() - 0.5) * 0.5);
        }
    }

    private void createChainLightning(World world, EntityLivingBase from, EntityLivingBase to) {
        Vec3d start = from.getPositionVector().add(0, from.height * 0.5, 0);
        Vec3d end = to.getPositionVector().add(0, to.height * 0.5, 0);

        int particleCount = 5;
        for (int i = 0; i < particleCount; i++) {
            double t = (double)i / particleCount;
            double px = start.x + (end.x - start.x) * t;
            double py = start.y + (end.y - start.y) * t;
            double pz = start.z + (end.z - start.z) * t;

            world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    px, py, pz, 0, 0, 0);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§5§l智能电弧凝视");
        tooltip.add("§b右键: §7启动视野内自动清敌");
        tooltip.add("§eShift+右键: §c范围连锁模式");
        tooltip.add("");
        tooltip.add("§6▶ §a智能视野控制系统");
        tooltip.add("§6▶ §e自动锁定视野内敌人");
        tooltip.add("§6▶ §c动态冷却（用时×10%）");
        tooltip.add("§6▶ DPS: §c100 + 100%最大生命值");
        tooltip.add("§7视野: 32格 60°角");

        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("ArcActive")) {
            int ticksLeft = stack.getTagCompound().getInteger("ArcTicks");
            int ticksUsed = stack.getTagCompound().getInteger("TicksUsed");
            float totalDamage = stack.getTagCompound().getFloat("TotalDamage");
            int killCount = stack.getTagCompound().getInteger("KillCount");
            boolean aoeMode = stack.getTagCompound().getBoolean("AoeMode");

            tooltip.add("");
            tooltip.add(String.format("§e⚡ 激活中: %.1f/%.1f秒",
                    ticksUsed / 20.0F, MAX_DURATION_T / 20.0F));
            if (aoeMode) {
                tooltip.add("§c◆ AOE模式 ◆");
            }
            tooltip.add(String.format("§a击杀: %d", killCount));
            tooltip.add(String.format("§c伤害: %.1f", totalDamage));

            // 预计冷却时间
            int expectedCooldown = calculateDynamicCooldown(ticksUsed);
            tooltip.add(String.format("§7预计冷却: %.1f秒", expectedCooldown / 20.0F));
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean("ArcActive");
    }

    public static void cleanupDisconnectedPlayers() {
        activeArcs.entrySet().removeIf(entry -> {
            ArcData data = entry.getValue();
            return data.arcEntity == null || data.arcEntity.isDead;
        });
    }
}