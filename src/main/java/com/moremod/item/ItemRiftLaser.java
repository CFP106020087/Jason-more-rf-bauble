package com.moremod.item;

import com.google.common.collect.Multimap;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityPlayerLaserBeam;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemRiftLaser extends Item {
    // ===== 数值参数 =====
    private static final double RANGE            = 80.0;
    private static final float  BEAM_WIDTH       = 0.5F;
    private static final int    BEAM_TTL         = 2;
    private static final float  BASE_DPS         = 500F;
    private static final float  BEAM_RADIUS      = 2.0F;
    private static final float  MAX_FOCUS_MULT   = 1000.0F;
    private static final int    FOCUS_BUILD_RATE = 3;

    // 冷却时间（原版机制）
    private static final int    MAX_USE_TIME     = 1200;   // 最大使用60秒
    private static final int    OVERHEAT_COOLDOWN = 400;  // 过热20秒冷却
    private static final int    NORMAL_COOLDOWN  = 100;   // 正常5秒冷却

    // NBT 键
    private static final String TAG_ACTIVE        = "moremod.laserActive";
    private static final String TAG_USE_TIME      = "moremod.useTime";
    private static final String TAG_TICK_COUNTER  = "moremod.laserTick";
    private static final String TAG_LAST_TARGET   = "moremod.lastTarget";
    private static final String TAG_FOCUS_STACKS  = "moremod.focusStacks";

    public ItemRiftLaser() {
        setMaxStackSize(1);
        setMaxDamage(5000);
        setNoRepair();
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("itemRiftLaser");
        setTranslationKey("itemRiftLaser");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 检查冷却
        if (player.getCooldownTracker().hasCooldown(this)) {
            if (!world.isRemote) {
                float remaining = player.getCooldownTracker().getCooldown(this, 0);
                player.sendStatusMessage(new TextComponentString(
                        String.format("§c系统冷却中... (%.1fs)", remaining / 20.0f)), true);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // Shift+右键 = 关闭
        if (player.isSneaking()) {
            if (isActive(stack)) {
                setActive(stack, false);
                if (!world.isRemote) {
                    world.playSound(null, player.getPosition(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                            SoundCategory.PLAYERS, 0.5F, 0.8F);
                    player.sendStatusMessage(new TextComponentString("§c镭射关闭"), true);

                    // 根据使用时间设置冷却
                    int useTime = getUseTime(stack);
                    int cooldown = Math.max(20, useTime / 3); // 最少1秒，一般是使用时间的1/3
                    player.getCooldownTracker().setCooldown(this, cooldown);

                    // 清零使用时间
                    setUseTime(stack, 0);
                }
                resetFocus(stack);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 普通右键 = 开启
        if (!isActive(stack)) {
            setActive(stack, true);
            setUseTime(stack, 0); // 重置使用时间
            if (!world.isRemote) {
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                        SoundCategory.PLAYERS, 0.5F, 1.5F);
                player.sendStatusMessage(new TextComponentString("§a镭射启动"), true);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 检测是否在任一手中
        boolean isMainhand = (player.getHeldItemMainhand() == stack);
        boolean isOffhand = (player.getHeldItemOffhand() == stack);

        // 主手需要选中，副手始终可用
        boolean canUse = (isMainhand && isSelected) || isOffhand;

        if (!canUse || !isActive(stack)) {
            if (!world.isRemote && entity.ticksExisted % 20 == 0) {
                setTickCounter(stack, 0);
                resetFocus(stack);
            }
            return;
        }

        if (!world.isRemote) {
            // 增加使用时间
            int useTime = getUseTime(stack) + 1;
            setUseTime(stack, useTime);

            // 耐久耗尽
            if (stack.getItemDamage() >= stack.getMaxDamage() - 1) {
                setActive(stack, false);
                world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ITEM_BREAK,
                        SoundCategory.PLAYERS, 1.0F, 0.5F);
                player.sendStatusMessage(new TextComponentString("§c§l能量耗尽！"), true);

                // 设置冷却
                player.getCooldownTracker().setCooldown(this, NORMAL_COOLDOWN);
                setUseTime(stack, 0);
                return;
            }

            // 过热保护（使用时间过长）
            if (useTime >= MAX_USE_TIME) {
                setActive(stack, false);
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_LAVA_EXTINGUISH,
                        SoundCategory.PLAYERS, 1.0F, 0.5F);
                player.sendStatusMessage(new TextComponentString("§c§l系统过热！强制冷却！"), true);

                // 过热时设置长冷却
                player.getCooldownTracker().setCooldown(this, OVERHEAT_COOLDOWN);
                setUseTime(stack, 0);
                resetFocus(stack);
                return;
            }

            // 使用时间警告（显示短暂冷却条作为警告）
            if (useTime > MAX_USE_TIME * 0.7 && !player.getCooldownTracker().hasCooldown(this)) {
                if (useTime % 20 == 0) { // 每秒闪烁一次
                    player.getCooldownTracker().setCooldown(this, 5);
                    player.sendStatusMessage(new TextComponentString("§6⚠ 系统过载警告！"), true);
                }
            }

            int tickCounter = getTickCounter(stack) + 1;
            setTickCounter(stack, tickCounter);

            // 计算发射位置
            Vec3d look = player.getLookVec().normalize();
            Vec3d eyePos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);

            // 根据实际使用的手调整偏移
            double handSign;
            if (isMainhand) {
                handSign = (player.getPrimaryHand() == EnumHandSide.RIGHT) ? 1.0D : -1.0D;
            } else {
                // 副手相反
                handSign = (player.getPrimaryHand() == EnumHandSide.RIGHT) ? -1.0D : 1.0D;
            }

            Vec3d right = new Vec3d(0, 1, 0).crossProduct(look).normalize();

            double forward = 0.55D;
            double lateral = 0.36D * handSign;
            double down = 0.38D + (player.isSneaking() ? 0.10D : 0.0D);

            Vec3d laserStart = new Vec3d(
                    eyePos.x + look.x * forward + right.x * lateral,
                    eyePos.y - down + look.y * forward + right.y * lateral,
                    eyePos.z + look.z * forward + right.z * lateral
            );

            Vec3d laserEnd = laserStart.add(look.scale(RANGE));

            // 方块碰撞
            RayTraceResult blockHit = world.rayTraceBlocks(laserStart, laserEnd, false, true, false);
            if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK) {
                laserEnd = blockHit.hitVec;
            }

            // 视觉光束
            // 视觉光束 - 修正构造函数调用
            EntityPlayerLaserBeam beam = new EntityPlayerLaserBeam(
                    world, player, laserEnd, BEAM_TTL,
                    isMainhand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND
            );
            world.spawnEntity(beam);

            // 搜索目标（所有实体）
            AxisAlignedBB searchBox = new AxisAlignedBB(
                    Math.min(laserStart.x, laserEnd.x) - BEAM_RADIUS,
                    Math.min(laserStart.y, laserEnd.y) - BEAM_RADIUS,
                    Math.min(laserStart.z, laserEnd.z) - BEAM_RADIUS,
                    Math.max(laserStart.x, laserEnd.x) + BEAM_RADIUS,
                    Math.max(laserStart.y, laserEnd.y) + BEAM_RADIUS,
                    Math.max(laserStart.z, laserEnd.z) + BEAM_RADIUS
            );

            List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, searchBox);
            Set<Entity> hitTargets = new HashSet<>();
            Entity primaryTarget = null;
            double closestDist = Double.MAX_VALUE;

            Vec3d beamVec = laserEnd.subtract(laserStart);
            double beamLen = beamVec.length();
            if (beamLen <= 1e-6) return;
            Vec3d beamDir = beamVec.normalize();

            DamageSource voidLaser = new DamageSource("moremod.void_laser")
                    .setDamageBypassesArmor()
                    .setDamageIsAbsolute()
                    .setMagicDamage();

            // 命中判定（包括非生物实体）
            for (Entity target : entities) {
                // 跳过某些类型
                if (target instanceof net.minecraft.entity.item.EntityXPOrb ||
                        target instanceof net.minecraft.entity.item.EntityItem) {
                    continue;
                }

                // 检查实体是否可被击中
                if (!target.canBeCollidedWith() && !(target instanceof EntityLivingBase)) {
                    continue;
                }

                Vec3d targetCenter = target.getPositionVector().add(0, target.height * 0.5, 0);
                Vec3d startToTarget = targetCenter.subtract(laserStart);
                double proj = startToTarget.dotProduct(beamDir);
                if (proj < 0 || proj > beamLen) continue;

                Vec3d closestPoint = laserStart.add(beamDir.scale(proj));
                double distance = targetCenter.distanceTo(closestPoint);
                double hitRadius = BEAM_RADIUS + Math.max(target.width, target.height) * 0.5;

                if (distance <= hitRadius) {
                    hitTargets.add(target);
                    if (proj < closestDist) {
                        closestDist = proj;
                        primaryTarget = target;
                    }
                }
            }

            // 聚焦系统（自动累积）
            int focusStacks = getFocusStacks(stack);

            // 只要镭射开启就自动累积聚焦
            if (tickCounter % FOCUS_BUILD_RATE == 0 && focusStacks < 999999) {
                focusStacks++;
                setFocusStacks(stack, focusStacks);

                // 每10层提示一次
                if (focusStacks % 10 == 0) {
                    player.sendStatusMessage(
                            new TextComponentString(
                                    String.format("§e聚焦: %d层", focusStacks)), true);
                }
            }

            // 如果有主要目标，聚焦加速
            if (primaryTarget != null) {
                int targetId = primaryTarget.getEntityId();
                int lastTargetId = getLastTarget(stack);

                if (targetId == lastTargetId) {
                    // 锁定同一目标，额外加速
                    if (tickCounter % (FOCUS_BUILD_RATE * 2) == 0 && focusStacks < 999999) {
                        focusStacks = Math.min(999999, focusStacks + 2);
                        setFocusStacks(stack, focusStacks);
                    }
                } else {
                    // 切换目标不再重置，只是记录新目标
                    setLastTarget(stack, targetId);
                }
            }

            // 伤害计算
            float damagePerTick = BASE_DPS / 20.0F;

            int power = EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.POWER, stack);
            int sharpness = EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.SHARPNESS, stack);
            damagePerTick *= (1.0F + 0.4F * power + 0.3F * sharpness);

            // 聚焦倍率计算
            float focusMult = 1.0F + (focusStacks * 0.4F);
            focusMult = Math.min(MAX_FOCUS_MULT, focusMult);
            damagePerTick *= focusMult;

            // 造成伤害
            boolean hitAny = false;
            for (Entity target : hitTargets) {
                float finalDamage = (target == primaryTarget) ? damagePerTick : (damagePerTick * 0.5F);

                if (target instanceof EntityLivingBase) {
                    // 生物实体：正常伤害流程
                    EntityLivingBase living = (EntityLivingBase) target;
                    living.attackEntityFrom(voidLaser, finalDamage);
                    living.hurtResistantTime = 0;
                    living.setFire(5);

                    if (tickCounter % 10 == 0) {
                        living.motionY += 0.1;
                        if (target == primaryTarget && focusStacks > 50) {
                            living.motionX += (Math.random() - 0.5) * 0.4;
                            living.motionZ += (Math.random() - 0.5) * 0.4;
                        }
                    }
                } else {
                    // 非生物实体
                    boolean damaged = target.attackEntityFrom(voidLaser, finalDamage);

                    // 如果常规伤害无效且伤害足够高，直接销毁
                    if (!damaged && !target.isEntityInvulnerable(voidLaser)) {
                        if (finalDamage > 50 || focusStacks > 75) {
                            target.setDead();
                            world.playSound(null, target.getPosition(),
                                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                                    SoundCategory.BLOCKS, 0.5F, 1.5F);
                        }
                    }

                    // 对非生物实体施加推力
                    if (tickCounter % 5 == 0) {
                        target.motionX += (Math.random() - 0.5) * 0.3;
                        target.motionY += 0.1;
                        target.motionZ += (Math.random() - 0.5) * 0.3;
                        target.velocityChanged = true;
                    }
                }
                hitAny = true;
            }

            // 声音
            if (tickCounter % 3 == 0) {
                float pitch = 2.5F + focusStacks * 0.005F;
                world.playSound(null, player.getPosition(), SoundEvents.ENTITY_BLAZE_SHOOT,
                        SoundCategory.PLAYERS, 0.1F, pitch);

                if (hitAny && tickCounter % 6 == 0) {
                    world.playSound(null, player.getPosition(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                            SoundCategory.HOSTILE, 0.2F, 2.0F);
                }
            }

            // 耐久消耗
            if (tickCounter % 10 == 0) {
                stack.damageItem(1, player);
            }
        }
    }

    // ===== 使用时间系统 =====
    private static int getUseTime(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(TAG_USE_TIME);
    }

    private static void setUseTime(ItemStack stack, int time) {
        ensureTag(stack);
        stack.getTagCompound().setInteger(TAG_USE_TIME, Math.max(0, time));
    }

    // ===== 聚焦系统 =====
    private static int getLastTarget(ItemStack stack) {
        if (!stack.hasTagCompound()) return -1;
        return stack.getTagCompound().getInteger(TAG_LAST_TARGET);
    }

    private static void setLastTarget(ItemStack stack, int id) {
        ensureTag(stack);
        stack.getTagCompound().setInteger(TAG_LAST_TARGET, id);
    }

    private static int getFocusStacks(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(TAG_FOCUS_STACKS);
    }

    private static void setFocusStacks(ItemStack stack, int stacks) {
        ensureTag(stack);
        stack.getTagCompound().setInteger(TAG_FOCUS_STACKS, Math.max(0, Math.min(999999, stacks)));
    }

    private static void resetFocus(ItemStack stack) {
        setLastTarget(stack, -1);
        setFocusStacks(stack, 0);
    }

    // ===== 基础系统 =====
    private static boolean isActive(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG_ACTIVE);
    }

    private static void setActive(ItemStack stack, boolean active) {
        ensureTag(stack);
        stack.getTagCompound().setBoolean(TAG_ACTIVE, active);
    }

    private static int getTickCounter(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger(TAG_TICK_COUNTER) : 0;
    }

    private static void setTickCounter(ItemStack stack, int t) {
        ensureTag(stack);
        stack.getTagCompound().setInteger(TAG_TICK_COUNTER, t);
    }

    private static void ensureTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
    }

    // ===== UI =====
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("§5§l虚空裂隙镭射炮");
        tooltip.add("§d终极毁灭武器");
        tooltip.add("");
        tooltip.add("§e右键: §a启动镭射");
        tooltip.add("§eShift+右键: §c关闭镭射");
        tooltip.add("");
        tooltip.add(String.format("§6▶ DPS: §c%.0f §7(绝对伤害)", BASE_DPS));
        tooltip.add(String.format("§6▶ 射程: §e%.0f格", RANGE));
        tooltip.add("§6▶ 特性: §d自动聚焦 §7| §b万物破坏 §7| §a双手可用");
        tooltip.add(String.format("§6▶ 最大持续: §e%d秒", MAX_USE_TIME / 20));

        if (isActive(stack)) {
            tooltip.add("");
            tooltip.add("§a§l◆ 镭射激活中 ◆");

            int useTime = getUseTime(stack);
            float usePercent = (float) useTime / MAX_USE_TIME * 100F;
            String color = usePercent > 80 ? "§c" : usePercent > 60 ? "§6" : usePercent > 40 ? "§e" : "§a";
            tooltip.add(String.format("%s⚡ 系统负载: %.1f%%", color, usePercent));

            if (usePercent > 70) {
                int remaining = (MAX_USE_TIME - useTime) / 20;
                tooltip.add(String.format("§c⚠ 过热倒计时: %d秒", remaining));
            }

            int focus = getFocusStacks(stack);
            if (focus > 0) {
                float mult = Math.min(MAX_FOCUS_MULT, 1.0F + (focus * 0.4F));
                String fColor = focus > 75 ? "§c" : focus > 50 ? "§6" : focus > 25 ? "§e" : "§a";
                tooltip.add(String.format("%s➤ 聚焦: %d层 (x%.2f)", fColor, focus, mult));
            }
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isActive(stack);
    }
}