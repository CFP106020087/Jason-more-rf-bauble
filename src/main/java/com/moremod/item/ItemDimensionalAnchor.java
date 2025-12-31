package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber
public class ItemDimensionalAnchor extends Item implements IBauble {

    // NBT Keys
    private static final String NBT_ANCHOR_POS = "AnchorPos";
    private static final String NBT_ANCHOR_DIM = "AnchorDim";
    private static final String NBT_LAST_SOLID = "LastSolidPos";
    private static final String NBT_OWNER_UUID = "OwnerUUID";
    private static final String NBT_DISPLACEMENT_IMMUNITY = "DisplacementImmunity";

    // 静态数据存储
    private static final Map<UUID, BlockPos> voidProtection = new HashMap<>();
    private static final Map<UUID, Integer> stillnessTicks = new HashMap<>();
    private static final Map<UUID, BlockPos> lastStillPosition = new HashMap<>();
    private static final Map<UUID, ForceResistance> forceResistanceMap = new HashMap<>();
    private static final Map<UUID, Boolean> displacementImmunity = new HashMap<>();

    // 配置参数
    private static final int REQUIRED_ACTIVE_MODULES = 8;
    private static final int STILLNESS_THRESHOLD = 40;
    private static final float REGEN_AMOUNT = 1.0F;
    private static final float DAMAGE_BOOST = 2.0F;
    private static final int DETECTION_RANGE = 128;

    // 反射字段缓存
    private static Field isInWebField = null;
    private static Field isJumpingField = null;

    static {
        try {
            try {
                isInWebField = Entity.class.getDeclaredField("isInWeb");
            } catch (NoSuchFieldException e) {
                isInWebField = Entity.class.getDeclaredField("field_70134_J");
            }
            if (isInWebField != null) isInWebField.setAccessible(true);
        } catch (Exception ignored) {}

        try {
            try {
                isJumpingField = EntityLivingBase.class.getDeclaredField("isJumping");
            } catch (NoSuchFieldException e) {
                isJumpingField = EntityLivingBase.class.getDeclaredField("field_70703_bu");
            }
            if (isJumpingField != null) isJumpingField.setAccessible(true);
        } catch (Exception ignored) {}
    }

    private static class ForceResistance {
        Vec3d playerControlledMotion = new Vec3d(0, 0, 0);
        Vec3d lastPosition = null;
        boolean isResisting = false;
        String resistanceSource = "";
        int resistanceTicks = 0;
        double lastGroundY = 0;
        boolean wasOnGround = false;
    }

    public ItemDimensionalAnchor() {
        setRegistryName("dimensional_anchor");
        setTranslationKey("dimensional_anchor");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BELT;
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) wearer;

        ItemStack core = findMechanicalCore(player);
        if (core.isEmpty()) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 需要佩戴机械核心"), true);
            }
            return false;
        }

        int activeModules = countActiveModules(core);
        if (activeModules < REQUIRED_ACTIVE_MODULES) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 激活模块不足 (" + activeModules + "/" + REQUIRED_ACTIVE_MODULES + ")"), true);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) wearer;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setString(NBT_OWNER_UUID, player.getUniqueID().toString());

        // 读取位移免疫状态
        boolean immunity = nbt.getBoolean(NBT_DISPLACEMENT_IMMUNITY);
        displacementImmunity.put(player.getUniqueID(), immunity);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "⚓ 维度锚定器已激活" +
                        (immunity ? TextFormatting.GOLD + " [位移免疫启用]" : TextFormatting.GRAY + " [位移免疫关闭]")));
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) wearer;

        UUID uuid = player.getUniqueID();
        voidProtection.remove(uuid);
        stillnessTicks.remove(uuid);
        lastStillPosition.remove(uuid);
        forceResistanceMap.remove(uuid);
        displacementImmunity.remove(uuid);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "⚓ 维度锚定器已停用"), true);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;

        // 蜘蛛网免疫 - 每tick直接清除，参考BountifulBaubles的简洁做法
        if (isInWebField != null) {
            try { isInWebField.setBoolean(player, false); } catch (Exception ignored) {}
        }

        if (!player.world.isRemote) {
            UUID uuid = player.getUniqueID();
            handleStillnessBonus(player, uuid);
            removeInstabilityEffect(player, uuid);
            handleVoidProtection(player, uuid, stack);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            if (player.isSneaking()) {
                // Shift+右键设置锚点
                BlockPos pos = findNearbyChest(world, player.getPosition());
                if (pos != null) {
                    NBTTagCompound nbt = stack.getTagCompound();
                    if (nbt == null) {
                        nbt = new NBTTagCompound();
                        stack.setTagCompound(nbt);
                    }
                    nbt.setLong(NBT_ANCHOR_POS, pos.toLong());
                    nbt.setInteger(NBT_ANCHOR_DIM, player.dimension);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚓ 锚点已设置: " +
                                    "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));

                    world.playSound(null, player.posX, player.posY, player.posZ,
                            SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.5F, 1.0F);

                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                } else {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "附近没有找到箱子"), true);
                }
            } else {
                // 普通右键切换位移免疫
                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt == null) {
                    nbt = new NBTTagCompound();
                    stack.setTagCompound(nbt);
                }

                boolean currentState = nbt.getBoolean(NBT_DISPLACEMENT_IMMUNITY);
                boolean newState = !currentState;
                nbt.setBoolean(NBT_DISPLACEMENT_IMMUNITY, newState);

                // 如果正在佩戴，更新状态
                if (hasEquippedAnchor(player)) {
                    displacementImmunity.put(player.getUniqueID(), newState);
                }

                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "⚓ 位移免疫已" +
                                (newState ? TextFormatting.GREEN + "启用" : TextFormatting.RED + "关闭")));

                world.playSound(null, player.posX, player.posY, player.posZ,
                        newState ? SoundEvents.BLOCK_ANVIL_PLACE : SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.PLAYERS, 0.5F, newState ? 1.2F : 0.8F);

                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }

        return super.onItemRightClick(world, player, hand);
    }

    // ===== 吸力抵抗系统 =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!hasEquippedAnchor(player)) {
            forceResistanceMap.remove(player.getUniqueID());
            return;
        }

        UUID uuid = player.getUniqueID();

        // 检查是否启用位移免疫
        if (!displacementImmunity.getOrDefault(uuid, false)) {
            forceResistanceMap.remove(uuid);
            return;
        }

        ForceResistance resistance = forceResistanceMap.computeIfAbsent(uuid, k -> new ForceResistance());

        // 记录地面状态
        if (player.onGround) {
            resistance.lastGroundY = player.posY;
            resistance.wasOnGround = true;
        }

        // 记录玩家位置
        if (resistance.lastPosition == null) {
            resistance.lastPosition = new Vec3d(player.posX, player.posY, player.posZ);
        }

        // 计算玩家的自主移动
        double intentionalX = 0, intentionalZ = 0;
        if (player.moveForward != 0 || player.moveStrafing != 0) {
            float yaw = player.rotationYaw * 0.017453292F;
            float speed = player.isSprinting() ? 0.25f : 0.15f;

            if (player.moveForward != 0) {
                intentionalX = -Math.sin(yaw) * speed * player.moveForward;
                intentionalZ = Math.cos(yaw) * speed * player.moveForward;
            }
            if (player.moveStrafing != 0) {
                intentionalX += Math.cos(yaw) * speed * player.moveStrafing;
                intentionalZ += Math.sin(yaw) * speed * player.moveStrafing;
            }
        }
        resistance.playerControlledMotion = new Vec3d(intentionalX, 0, intentionalZ);

        // 检测吸力源
        List<EntityLivingBase> pullSources = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(DETECTION_RANGE),
                entity -> {
                    if (entity == null || entity == player) return false;
                    String className = entity.getClass().getName();
                    return className.contains("Amalgalich") || className.contains("Spectre");
                }
        );

        if (!pullSources.isEmpty()) {
            boolean beingPulled = false;
            String pullSource = "";
            Vec3d pullVector = Vec3d.ZERO;

            for (EntityLivingBase source : pullSources) {
                double distance = player.getDistance(source);
                boolean sourceActive = false;

                // 检测是否激活
                try {
                    Method extraAnimMethod = source.getClass().getMethod("extraAnimation01");
                    if (extraAnimMethod != null) {
                        sourceActive = (boolean) extraAnimMethod.invoke(source);
                    }
                } catch (Exception ignored) {}

                // 检查范围
                boolean inRange = false;
                if (source.getClass().getName().contains("Spectre")) {
                    inRange = distance <= 18;
                    if (sourceActive && inRange) {
                        beingPulled = true;
                        pullSource = "Spectre";
                        double dx = source.posX - player.posX;
                        double dz = source.posZ - player.posZ;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.01) {
                            pullVector = new Vec3d(dx / dist * 0.1, 0, dz / dist * 0.1);
                        }
                    }
                } else if (source.getClass().getName().contains("Amalgalich")) {
                    inRange = distance <= 64;
                    if (sourceActive && inRange) {
                        beingPulled = true;
                        pullSource = "Amalgalich";
                        double dx = source.posX - player.posX;
                        double dz = source.posZ - player.posZ;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.01) {
                            pullVector = new Vec3d(dx / dist * 0.1, 0, dz / dist * 0.1);
                        }
                    }
                }

                if (beingPulled) break;
            }

            if (beingPulled) {
                if (!resistance.isResisting) {
                    resistance.isResisting = true;
                    resistance.resistanceSource = pullSource;
                    if (!player.world.isRemote) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.DARK_PURPLE + "⚓ 锚定激活 - 抵抗" + pullSource + "吸力"), true);
                    }
                }

                // 抵消吸力但保留玩家控制
                applySelectiveResistance(player, resistance, pullVector);
                resistance.resistanceTicks++;

            } else {
                resistance.isResisting = false;
                resistance.resistanceTicks = 0;
            }
        } else {
            resistance.isResisting = false;
            resistance.resistanceTicks = 0;
        }

        resistance.lastPosition = new Vec3d(player.posX, player.posY, player.posZ);
    }

    private static void applySelectiveResistance(EntityPlayer player, ForceResistance resistance, Vec3d pullForce) {
        // 估算当前受到的外力
        Vec3d currentMotion = new Vec3d(player.motionX, player.motionY, player.motionZ);
        Vec3d expectedMotion = resistance.playerControlledMotion;

        // 水平外力
        Vec3d horizontalExternal = new Vec3d(
                currentMotion.x - expectedMotion.x,
                0,
                currentMotion.z - expectedMotion.z
        );

        double horizontalForceLength = Math.sqrt(horizontalExternal.x * horizontalExternal.x + horizontalExternal.z * horizontalExternal.z);
        double pullForceLength = Math.sqrt(pullForce.x * pullForce.x + pullForce.z * pullForce.z);

        // 水平方向抵抗
        if (pullForceLength > 0 && horizontalForceLength > 0.05) {
            Vec3d normalizedExternal = new Vec3d(
                    horizontalExternal.x / horizontalForceLength,
                    0,
                    horizontalExternal.z / horizontalForceLength
            );
            Vec3d normalizedPull = new Vec3d(
                    pullForce.x / pullForceLength,
                    0,
                    pullForce.z / pullForceLength
            );

            double similarity = normalizedExternal.x * normalizedPull.x + normalizedExternal.z * normalizedPull.z;

            if (similarity > 0.5) {
                double reductionFactor = 0.95 * similarity;
                player.motionX = resistance.playerControlledMotion.x + horizontalExternal.x * (1 - reductionFactor);
                player.motionZ = resistance.playerControlledMotion.z + horizontalExternal.z * (1 - reductionFactor);
            }
        }

        // 垂直方向处理
        if ("Amalgalich".equals(resistance.resistanceSource)) {
            // 检测异常垂直运动
            boolean isJumping = false;
            try {
                if (isJumpingField != null) {
                    isJumping = isJumpingField.getBoolean(player);
                }
            } catch (Exception ignored) {}

            // 如果不是主动跳跃且有异常上升
            if (!isJumping && player.motionY > 0.1) {
                player.motionY *= 0.2;
            }

            // 防止异常下坠
            if (player.motionY < -0.5 && !player.onGround) {
                player.motionY = Math.max(player.motionY, -0.5);
            }

            // 在地面时锁定Y
            if (player.onGround && !isJumping) {
                player.motionY = Math.min(player.motionY, 0);
            }
        }

        // 增强玩家的水平控制力
        double controlLength = Math.sqrt(
                resistance.playerControlledMotion.x * resistance.playerControlledMotion.x +
                        resistance.playerControlledMotion.z * resistance.playerControlledMotion.z
        );

        if (controlLength > 0) {
            player.motionX += resistance.playerControlledMotion.x * 0.8;
            player.motionZ += resistance.playerControlledMotion.z * 0.8;
        }

        // 限制最大速度
        double maxHorizontalSpeed = player.isSprinting() ? 0.4 : 0.25;
        double currentHorizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        if (currentHorizontalSpeed > maxHorizontalSpeed) {
            player.motionX = (player.motionX / currentHorizontalSpeed) * maxHorizontalSpeed;
            player.motionZ = (player.motionZ / currentHorizontalSpeed) * maxHorizontalSpeed;
        }

        player.velocityChanged = true;

        // 视觉效果
        if (!player.world.isRemote && resistance.resistanceTicks % 5 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double px = player.posX + Math.cos(angle) * 1.5;
                double pz = player.posZ + Math.sin(angle) * 1.5;
                player.world.spawnParticle(
                        EnumParticleTypes.PORTAL,
                        px, player.posY + 1, pz,
                        0, 0, 0, new int[0]
                );
            }
        }
    }

    private void handleStillnessBonus(EntityPlayer player, UUID uuid) {
        BlockPos currentPos = player.getPosition();
        BlockPos lastPos = lastStillPosition.get(uuid);

        boolean isJumping = false;
        if (isJumpingField != null) {
            try { isJumping = isJumpingField.getBoolean(player); }
            catch (Exception e) { isJumping = player.motionY > 0.1; }
        } else {
            isJumping = player.motionY > 0.1;
        }

        boolean isStill = player.moveForward == 0 && player.moveStrafing == 0 && !isJumping && player.onGround;

        if (isStill && lastPos != null && lastPos.equals(currentPos)) {
            int ticks = stillnessTicks.getOrDefault(uuid, 0) + 1;
            stillnessTicks.put(uuid, ticks);

            if (ticks >= STILLNESS_THRESHOLD) {
                if (ticks % 20 == 0) {
                    player.heal(REGEN_AMOUNT);
                    for (int i = 0; i < 3; i++) {
                        double px = player.posX + (player.world.rand.nextDouble() - 0.5) * 1.5;
                        double py = player.posY + player.world.rand.nextDouble() * 2;
                        double pz = player.posZ + (player.world.rand.nextDouble() - 0.5) * 1.5;
                        player.world.spawnParticle(EnumParticleTypes.HEART, px, py, pz, 0, 0.1, 0, new int[0]);
                    }
                }
                if (ticks % 40 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GOLD + "⚓ 静止锚定: " +
                                    TextFormatting.GREEN + "+" + (int)((DAMAGE_BOOST - 1) * 100) + "% 攻击力"), true);
                }
            }
        } else {
            stillnessTicks.put(uuid, 0);
            lastStillPosition.put(uuid, currentPos);
        }
    }

    private void removeInstabilityEffect(EntityPlayer player, UUID uuid) {
        Potion instabilityPotion = getInstabilityPotion();
        if (instabilityPotion != null && player.isPotionActive(instabilityPotion)) {
            player.removePotionEffect(instabilityPotion);

            double maxVelocity = 0.5;
            if (Math.abs(player.motionX) > maxVelocity ||
                    Math.abs(player.motionY) > maxVelocity ||
                    Math.abs(player.motionZ) > maxVelocity) {

                player.motionX = Math.max(-maxVelocity, Math.min(maxVelocity, player.motionX));
                player.motionY = Math.max(-maxVelocity, Math.min(maxVelocity, player.motionY));
                player.motionZ = Math.max(-maxVelocity, Math.min(maxVelocity, player.motionZ));
                player.velocityChanged = true;
            }
        }
    }

    private void handleVoidProtection(EntityPlayer player, UUID uuid, ItemStack stack) {
        if (player.onGround && !player.world.isAirBlock(player.getPosition().down())) {
            voidProtection.put(uuid, player.getPosition());

            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }
            nbt.setLong(NBT_LAST_SOLID, player.getPosition().toLong());
        }

        if (player.posY < -64) {
            BlockPos safePos = voidProtection.get(uuid);
            if (safePos != null) {
                player.setPositionAndUpdate(safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5);
                player.fallDistance = 0;
                player.motionY = 0;

                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "⚓ 虚空保护已触发"), true);
            }
        }
    }

    // ===== 事件处理 =====

    @SubscribeEvent
    public static void onPlayerTickCoreCheck(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        if (player.world.getTotalWorldTime() % 20 == 0) {
            if (hasEquippedAnchor(player)) {
                ItemStack core = findMechanicalCoreStatic(player);
                if (core.isEmpty()) {
                    forceUnequipAnchor(player, "机械核心异常");
                } else {
                    int activeModules = countActiveModulesStatic(core);
                    if (activeModules < REQUIRED_ACTIVE_MODULES) {
                        forceUnequipAnchor(player, "激活模块不足 (" + activeModules + "/" + REQUIRED_ACTIVE_MODULES + ")");
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotionApplicable(net.minecraftforge.event.entity.living.PotionEvent.PotionApplicableEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (hasEquippedAnchor(player)) {
                PotionEffect effect = event.getPotionEffect();
                if (effect != null && effect.getPotion() != null) {
                    String potionName = effect.getPotion().getRegistryName() != null ?
                            effect.getPotion().getRegistryName().toString() : "";
                    if (potionName.contains("instability")) {
                        event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (hasEquippedAnchor(player)) {
                event.setCanceled(true);
                UUID uuid = player.getUniqueID();
                int stillTicks = stillnessTicks.getOrDefault(uuid, 0);
                if (stillTicks >= STILLNESS_THRESHOLD && !player.world.isRemote) {
                    player.world.spawnParticle(EnumParticleTypes.BARRIER,
                            player.posX, player.posY + 1, player.posZ, 0, 0, 0, new int[0]);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAnchorDamageBoost(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getSource() != null && event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            if (hasEquippedAnchor(player)) {
                UUID uuid = player.getUniqueID();
                int stillTicks = stillnessTicks.getOrDefault(uuid, 0);
                if (stillTicks >= STILLNESS_THRESHOLD) {
                    float boosted = event.getAmount() * DAMAGE_BOOST;
                    event.setAmount(boosted);
                    if (!player.world.isRemote) {
                        EntityLivingBase target = event.getEntityLiving();
                        player.world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                target.posX, target.posY + target.height / 2, target.posZ, 0.5, 0.5, 0.5, new int[0]);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeathEarly(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        ItemStack anchor = findEquippedAnchor(player);
        if (anchor.isEmpty()) return;

        NBTTagCompound nbt = anchor.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_ANCHOR_POS)) return;

        BlockPos anchorPos = BlockPos.fromLong(nbt.getLong(NBT_ANCHOR_POS));
        int dim = nbt.getInteger(NBT_ANCHOR_DIM);

        if (dim == player.dimension) {
            TileEntity te = player.world.getTileEntity(anchorPos);
            if (te instanceof TileEntityChest) {
                saveItemsToChest(player, (TileEntityChest) te);
            }
        }
    }

    // ===== 辅助方法 =====

    private static void saveItemsToChest(EntityPlayer player, TileEntityChest chest) {
        List<ItemStack> savedItems = new ArrayList<>();
        List<ItemStack> cursedItems = new ArrayList<>();

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);
            if (!item.isEmpty()) savedItems.add(item.copy());
        }

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);
                if (!bauble.isEmpty()) {
                    if (isMechanicalCoreStatic(bauble)) cursedItems.add(bauble.copy());
                    else savedItems.add(bauble.copy());
                }
            }
        }

        int saved = 0;
        for (ItemStack item : savedItems) {
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                if (chest.getStackInSlot(i).isEmpty()) {
                    chest.setInventorySlotContents(i, item);
                    saved++;
                    break;
                }
            }
        }

        if (saved > 0) {
            player.inventory.clear();
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && !isMechanicalCoreStatic(bauble)) {
                        baubles.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }

            String message = TextFormatting.GREEN + "⚓ 维度锚定器已保护 " + saved + " 件物品";
            if (!cursedItems.isEmpty()) message += TextFormatting.GRAY + " (诅咒物品保留: " + cursedItems.size() + ")";
            player.sendMessage(new TextComponentString(message));
        }
    }

    private static void forceUnequipAnchor(EntityPlayer player, String reason) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemDimensionalAnchor) {
                    if (player.inventory.addItemStackToInventory(stack.copy())) {
                        baubles.setStackInSlot(i, ItemStack.EMPTY);
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "⚠ " + reason + "，维度锚定器已自动卸下"));
                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.PLAYERS, 0.5F, 1.0F);
                    }
                    break;
                }
            }
        }
    }

    private static Potion getInstabilityPotion() {
        for (Potion p : Potion.REGISTRY) {
            if (p != null && p.getRegistryName() != null) {
                String name = p.getRegistryName().toString();
                if (name.contains("instability")) return p;
            }
        }
        try {
            for (ResourceLocation key : Potion.REGISTRY.getKeys()) {
                if (key.toString().contains("instability")) {
                    return Potion.REGISTRY.getObject(key);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean hasEquippedAnchor(EntityPlayer player) {
        return !findEquippedAnchor(player).isEmpty();
    }

    private static ItemStack findEquippedAnchor(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemDimensionalAnchor) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findMechanicalCore(EntityPlayer player) { return findMechanicalCoreStatic(player); }

    private static ItemStack findMechanicalCoreStatic(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && isMechanicalCoreStatic(stack)) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isMechanicalCoreStatic(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String className = stack.getItem().getClass().getName();
        if (className.contains("ItemMechanicalCore")) return true;
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl != null) {
            String name = rl.toString().toLowerCase();
            return name.contains("mechanical") && name.contains("core");
        }
        return false;
    }

    private int countActiveModules(ItemStack core) { return countActiveModulesStatic(core); }

    private static int countActiveModulesStatic(ItemStack core) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        int count = 0;

        if (nbt.hasKey("Upgrades", 10)) {
            NBTTagCompound upgrades = nbt.getCompoundTag("Upgrades");
            for (String key : upgrades.getKeySet()) {
                NBTTagCompound module = upgrades.getCompoundTag(key);
                int level = module.getInteger("level");
                boolean enabled = module.getBoolean("enabled") ||
                        module.getBoolean("active") ||
                        "ON".equals(module.getString("state"));
                if (enabled && level > 0) count++;
            }
        }

        for (String key : nbt.getKeySet()) {
            if (key.startsWith("upgrade_")) {
                int level = nbt.getInteger(key);
                if (level > 0) {
                    String upgradeName = key.substring(8);
                    boolean disabled = nbt.getBoolean("Disabled_" + upgradeName) ||
                            nbt.getBoolean("IsPaused_" + upgradeName);
                    if (!disabled) count++;
                }
            }
        }
        return count / 2;
    }

    private BlockPos findNearbyChest(World world, BlockPos center) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = center.add(x, y, z);
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof TileEntityChest) return pos;
                }
            }
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "═══ 维度锚定器 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "腰带");
        tip.add("");

        NBTTagCompound nbt = stack.getTagCompound();
        boolean immunityEnabled = nbt != null && nbt.getBoolean(NBT_DISPLACEMENT_IMMUNITY);
        tip.add(TextFormatting.GOLD + "位移免疫: " +
                (immunityEnabled ? TextFormatting.GREEN + "启用" : TextFormatting.RED + "关闭") +
                TextFormatting.GRAY + " (右键切换)");

        if (nbt != null && nbt.hasKey(NBT_ANCHOR_POS)) {
            BlockPos pos = BlockPos.fromLong(nbt.getLong(NBT_ANCHOR_POS));
            int dim = nbt.getInteger(NBT_ANCHOR_DIM);
            tip.add(TextFormatting.GREEN + "锚点位置: " + TextFormatting.WHITE +
                    "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
            tip.add(TextFormatting.GREEN + "锚点维度: " + TextFormatting.WHITE + dim);
        } else {
            tip.add(TextFormatting.YELLOW + "锚点: " + TextFormatting.GRAY + "未设置 (Shift+右键箱子)");
        }

        tip.add("");
        tip.add(TextFormatting.LIGHT_PURPLE + "被动效果:");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.WHITE + "免疫所有击退效果");
        if (immunityEnabled) {
            tip.add(TextFormatting.GRAY + "• " + TextFormatting.GOLD + "完全免疫Amalgalich吸取");
            tip.add(TextFormatting.GRAY + "• " + TextFormatting.DARK_PURPLE + "完全免疫Spectre拉扯");
        }
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.AQUA + "免疫Instability不稳定效果");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.GREEN + "自由穿越蜘蛛网");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.WHITE + "掉入虚空时传送回安全位置");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.WHITE + "死亡时装备传送到锚点箱子");

        tip.add("");
        tip.add(TextFormatting.YELLOW + "静止锚定效果 (2秒后激活):");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.RED + "+" + (int)((DAMAGE_BOOST - 1) * 100) + "% 攻击伤害");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.GREEN + "每秒恢复 " + REGEN_AMOUNT + " 生命值");

        tip.add("");
        tip.add(TextFormatting.RED + "佩戴需求:");
        tip.add(TextFormatting.GRAY + "• " + TextFormatting.WHITE + "机械核心激活模块 ≥ " + REQUIRED_ACTIVE_MODULES);

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "使用说明:");
            tip.add(TextFormatting.GRAY + "• 右键: 切换位移免疫开关");
            tip.add(TextFormatting.GRAY + "• Shift+右键: 设置锚点箱子");
            tip.add("");
            tip.add(TextFormatting.GRAY + "位移免疫关闭时仍保留其他效果");
            tip.add(TextFormatting.GRAY + "但不会抵抗Boss的吸力");
            tip.add("");
            tip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + "「我即是此处的支点」");
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }
}