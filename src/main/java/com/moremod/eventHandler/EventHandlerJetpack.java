// EventHandlerJetpack.java - 添加飞行时的撞击伤害保护和Shift下降功能
package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemJetpackBauble;
import com.moremod.item.ItemCreativeJetpackBauble;
import com.moremod.network.MessageJetpackJumping;
import com.moremod.network.MessageJetpackSneaking;
import com.moremod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventHandlerJetpack {

    public static final Map<UUID, Boolean> jetpackJumping = new HashMap<UUID, Boolean>();
    // 新增：记录玩家是否按下Shift键
    public static final Map<UUID, Boolean> jetpackSneaking = new HashMap<UUID, Boolean>();
    private static final Map<UUID, Boolean> jetpackWasActive = new HashMap<UUID, Boolean>();
    private static final Map<UUID, Boolean> playerFlying = new HashMap<UUID, Boolean>();

    // 伤害保护事件处理 - 提高优先级确保多人游戏正常工作
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        UUID playerId = player.getUniqueID();

        // 检查是否正在使用喷气背包飞行
        Boolean isFlying = playerFlying.get(playerId);
        if (isFlying != null && isFlying.booleanValue()) {
            DamageSource source = event.getSource();

            // 保护免受以下伤害类型：
            // - 撞击伤害 (kinetic/flyIntoWall)
            // - 摔落伤害 (fall)
            // - 撞墙伤害 (inWall)
            if (source == DamageSource.FLY_INTO_WALL ||
                    source == DamageSource.FALL ||
                    source == DamageSource.IN_WALL ||
                    source.getDamageType().equals("flyIntoWall") ||
                    source.getDamageType().equals("fall") ||
                    source.getDamageType().equals("inWall") ||
                    source.getDamageType().equals("kinetic")) {

                event.setCanceled(true);

                // 🔧 多人游戏调试：可选的调试输出
                // System.out.println("Jetpack damage protection activated for " + player.getName() + " from " + source.getDamageType());
                return;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        boolean wearingJetpack = false;
        for (int i = 0; i < BaublesApi.getBaublesHandler(mc.player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(mc.player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemJetpackBauble || stack.getItem() instanceof ItemCreativeJetpackBauble) {
                wearingJetpack = true;
                break;
            }
        }

        boolean isJumping = mc.gameSettings.keyBindJump.isKeyDown();
        // 新增：检测Shift键状态
        boolean isSneaking = mc.gameSettings.keyBindSneak.isKeyDown();

        PacketHandler.INSTANCE.sendToServer(new MessageJetpackJumping(wearingJetpack && isJumping));
        // 新增：发送Shift键状态到服务端
        PacketHandler.INSTANCE.sendToServer(new MessageJetpackSneaking(wearingJetpack && isSneaking));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        UUID playerId = player.getUniqueID();

        boolean hasJetpackEquipped = false;
        boolean isJetpackActive = false;
        boolean isHovering = false;
        boolean isFlying = false;
        ItemStack activeJetpack = null;
        double currentAscendSpeed = 0;

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);

            if (!(stack.getItem() instanceof ItemJetpackBauble) && !(stack.getItem() instanceof ItemCreativeJetpackBauble)) {
                continue;
            }

            hasJetpackEquipped = true;
            activeJetpack = stack;

            double ascendSpeed, descendSpeed, moveSpeed;
            int energyPerTick;

            if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                ItemCreativeJetpackBauble creativeJetpack = (ItemCreativeJetpackBauble) stack.getItem();
                ascendSpeed = creativeJetpack.getAscendSpeed(stack);
                descendSpeed = creativeJetpack.getDescendSpeed(stack);
                moveSpeed = creativeJetpack.getMoveSpeed(stack);
                energyPerTick = 0;
            } else {
                ItemJetpackBauble jetpackItem = (ItemJetpackBauble) stack.getItem();
                ascendSpeed = jetpackItem.getAscendSpeed();
                descendSpeed = jetpackItem.getDescendSpeed();
                moveSpeed = jetpackItem.getMoveSpeed();
                energyPerTick = jetpackItem.getEnergyPerTick();
            }

            IEnergyStorage energy = null;
            if (stack.getItem() instanceof ItemJetpackBauble) {
                energy = ItemJetpackBauble.getEnergyStorage(stack);
                if (energy == null || energy.getEnergyStored() <= 0) {
                    // 🚀 新增：没电时立即禁用喷气背包和伤害免疫
                    disableJetpack(stack);
                    playerFlying.put(playerId, false);
                    if (!player.capabilities.isCreativeMode) {
                        player.capabilities.isFlying = false;
                        player.capabilities.allowFlying = false;
                        player.sendPlayerAbilities();
                    }
                    continue;
                }
            } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                energy = ItemCreativeJetpackBauble.getEnergyStorage(stack);
            }

            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            boolean enabled = tag.getBoolean("JetpackEnabled");
            boolean hover = tag.getBoolean("HoverEnabled");
            Boolean jumpingObj = jetpackJumping.get(playerId);
            boolean jumping = jumpingObj != null ? jumpingObj.booleanValue() : false;

            // 新增：获取Shift键状态
            Boolean sneakingObj = jetpackSneaking.get(playerId);
            boolean sneaking = sneakingObj != null ? sneakingObj.booleanValue() : false;

            if (enabled && (jumping || hover || sneaking)) {
                boolean canUseEnergy = true;

                if (stack.getItem() instanceof ItemJetpackBauble && energy != null) {
                    canUseEnergy = energy.extractEnergy(energyPerTick, false) > 0;
                } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    canUseEnergy = true;
                    if (energy != null) {
                        energy.extractEnergy(energyPerTick, false);
                    }
                }

                if (canUseEnergy) {
                    // 🔧 修复：确保只在服务端修改运动
                    if (!player.world.isRemote) {
                        if (hover) {
                            isHovering = true;
                            if (jumping && player.motionY < ascendSpeed * 4.0) { // 提高速度限制
                                // 悬停模式下按Space上升
                                player.motionY += ascendSpeed;
                                isFlying = true;
                            } else if (sneaking && player.motionY > -descendSpeed * 4.0) { // 提高下降速度限制
                                // 新增：悬停模式下按Shift下降
                                player.motionY -= descendSpeed;
                                isFlying = true;
                            } else if (!jumping && !sneaking) {
                                // 只有在不按任何键时才悬停（设置为0）
                                player.motionY = 0.0D;
                            }
                        } else if (jumping && player.motionY < ascendSpeed * 4.0) { // 提高速度限制
                            // 非悬停模式下按Space上升
                            player.motionY += ascendSpeed;
                            isFlying = true;
                        } else if (sneaking && player.motionY > -descendSpeed * 4.0) { // 提高下降速度限制
                            // 新增：非悬停模式下按Shift也可以控制下降
                            player.motionY -= descendSpeed * 0.8; // 提高下降速度
                            isFlying = true;
                        }

                        currentAscendSpeed = ascendSpeed;
                        applyHorizontalThrust(player, moveSpeed);
                    }

                    // 🚀 修复：确保在服务端和客户端都标记飞行状态（用于伤害保护）
                    playerFlying.put(playerId, true);

                    if (!player.capabilities.isCreativeMode) {
                        player.capabilities.allowFlying = true;
                        player.capabilities.isFlying = true;
                        player.sendPlayerAbilities();
                    }

                    isJetpackActive = true;
                    jetpackWasActive.put(playerId, Boolean.TRUE);
                }
            }
        }

        // 视觉效果处理 - 只在客户端执行
        if (isJetpackActive && activeJetpack != null && player.world.isRemote) {
            handleJetpackVisualEffects(player, activeJetpack, isHovering, isFlying, currentAscendSpeed);
        }

        // 🔧 修复：处理没有装备喷气背包的情况
        if (!hasJetpackEquipped && !player.capabilities.isCreativeMode) {
            // 🚀 新增：立即禁用背包中的所有喷气背包
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (stack.getItem() instanceof ItemJetpackBauble || stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    if (!stack.hasTagCompound()) continue;
                    stack.getTagCompound().setBoolean("JetpackEnabled", false);
                    stack.getTagCompound().setBoolean("HoverEnabled", false);
                }
            }

            // 🚀 新增：立即禁用飞行状态和伤害免疫
            player.capabilities.isFlying = false;
            player.capabilities.allowFlying = false;
            player.sendPlayerAbilities();
            jetpackWasActive.put(playerId, Boolean.FALSE);
            playerFlying.put(playerId, false);
        }

        // 🔧 修复：处理喷气背包未激活的情况（包括没电情况）
        if (!isJetpackActive && hasJetpackEquipped && !player.capabilities.isCreativeMode) {
            // 🚀 新增：立即禁用飞行状态和伤害免疫
            player.capabilities.isFlying = false;
            player.capabilities.allowFlying = false;
            player.sendPlayerAbilities();
            jetpackWasActive.put(playerId, Boolean.FALSE);
            playerFlying.put(playerId, false);
        }

        // 🚀 新增：如果喷气背包未激活，确保移除飞行保护
        if (!isJetpackActive) {
            playerFlying.put(playerId, false);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void handleJetpackVisualEffects(EntityPlayer player, ItemStack jetpack,
                                                   boolean isHovering, boolean isFlying, double ascendSpeed) {
        if (!player.onGround) {
            // 视觉效果处理
        }

        spawnJetpackParticles(player, jetpack, isHovering, isFlying, ascendSpeed);
    }

    @SideOnly(Side.CLIENT)
    private static void spawnJetpackParticles(EntityPlayer player, ItemStack jetpack,
                                              boolean isHovering, boolean isFlying, double ascendSpeed) {
        World world = player.world;
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;

        if (isHovering && !isFlying) {
            spawnHoverParticles(world, player, x, y, z);
        }

        if (isFlying) {
            float throttle = (float) Math.min(ascendSpeed * 2.0, 1.0);
            spawnThrusterParticles(world, player, x, y, z, jetpack, throttle);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void spawnHoverParticles(World world, EntityPlayer player, double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.6;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.6;
            double offsetY = world.rand.nextDouble() * 0.2;

            world.spawnParticle(EnumParticleTypes.CLOUD,
                    x + offsetX, y + 0.2 + offsetY, z + offsetZ,
                    (world.rand.nextDouble() - 0.5) * 0.02,
                    -0.05,
                    (world.rand.nextDouble() - 0.5) * 0.02);
        }

        if (world.rand.nextInt(20) == 0) {
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    x, y + 0.6, z,
                    0, 0, 0);
        }

        if (world.rand.nextInt(15) == 0) {
            world.spawnParticle(EnumParticleTypes.WATER_DROP,
                    x + (world.rand.nextDouble() - 0.5) * 1.2,
                    y + 0.5,
                    z + (world.rand.nextDouble() - 0.5) * 1.2,
                    0, -0.1, 0);
        }

        if (world.rand.nextInt(8) == 0) {
            double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
            if (speed > 0.1) {
                double behindX = x - player.motionX * 10;
                double behindZ = z - player.motionZ * 10;

                world.spawnParticle(EnumParticleTypes.CLOUD,
                        behindX + (world.rand.nextDouble() - 0.5) * 0.5,
                        y + 0.5 + (world.rand.nextDouble() - 0.5) * 0.3,
                        behindZ + (world.rand.nextDouble() - 0.5) * 0.5,
                        player.motionX * 0.1, 0, player.motionZ * 0.1);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void spawnThrusterParticles(World world, EntityPlayer player, double x, double y, double z,
                                               ItemStack jetpack, float throttle) {
        float yaw = player.rotationYaw;
        double yawRad = Math.toRadians(yaw);
        double thrusterX = x - Math.sin(yawRad) * 0.4;
        double thrusterY = y + 0.5;
        double thrusterZ = z + Math.cos(yawRad) * 0.4;

        int particleCount = Math.max(2, (int)(throttle * 8));

        EnumParticleTypes flameType = EnumParticleTypes.FLAME;
        EnumParticleTypes extraType = null;

        if (jetpack.getItem() instanceof ItemCreativeJetpackBauble) {
            ItemCreativeJetpackBauble creativeJetpack = (ItemCreativeJetpackBauble) jetpack.getItem();
            ItemCreativeJetpackBauble.SpeedMode mode = creativeJetpack.getSpeedMode(jetpack);

            switch (mode) {
                case SLOW:
                    flameType = EnumParticleTypes.FLAME;
                    break;
                case NORMAL:
                    flameType = EnumParticleTypes.FLAME;
                    particleCount = (int)(particleCount * 1.2);
                    break;
                case FAST:
                    flameType = EnumParticleTypes.DRIP_LAVA;
                    extraType = EnumParticleTypes.LAVA;
                    particleCount = (int)(particleCount * 1.5);
                    break;
                case ULTRA:
                    flameType = EnumParticleTypes.ENCHANTMENT_TABLE;
                    extraType = EnumParticleTypes.PORTAL;
                    particleCount = (int)(particleCount * 2.0);
                    break;
            }
        }

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.3;
            double offsetY = (world.rand.nextDouble() - 0.5) * 0.3;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.3;

            double velX = -Math.sin(yawRad) * (0.1 + throttle * 0.2) + offsetX * 0.1;
            double velY = -0.1 - world.rand.nextDouble() * throttle * 0.1;
            double velZ = Math.cos(yawRad) * (0.1 + throttle * 0.2) + offsetZ * 0.1;

            world.spawnParticle(flameType,
                    thrusterX + offsetX, thrusterY + offsetY, thrusterZ + offsetZ,
                    velX, velY, velZ);

            if (extraType != null && world.rand.nextInt(3) == 0) {
                world.spawnParticle(extraType,
                        thrusterX + offsetX, thrusterY + offsetY, thrusterZ + offsetZ,
                        velX * 0.5, velY * 0.5, velZ * 0.5);
            }
        }

        if (world.rand.nextInt(4) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    thrusterX, thrusterY, thrusterZ,
                    -Math.sin(yawRad) * 0.05, -0.02, Math.cos(yawRad) * 0.05);
        }

        if (throttle > 0.8f && world.rand.nextInt(15) == 0) {
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    thrusterX, thrusterY, thrusterZ,
                    0, 0, 0);
        }
    }

    private static void applyHorizontalThrust(EntityPlayer player, double speed) {
        // 🔧 修复：增加水平推进速度
        float yaw = player.rotationYaw;
        double yawRad = Math.toRadians(yaw);
        double forward = player.moveForward;
        double strafe = player.moveStrafing;

        // 计算前进和横移的速度分量
        double moveX = forward * Math.sin(yawRad) + strafe * Math.cos(yawRad);
        double moveZ = forward * Math.cos(yawRad) - strafe * Math.sin(yawRad);

        // 🚀 大幅增加水平推力 - 使用更高的速度倍数
        double horizontalSpeedMultiplier = 2.5; // 可以调整这个值
        player.motionX += moveX * speed * horizontalSpeedMultiplier;
        player.motionZ += moveZ * speed * horizontalSpeedMultiplier;
    }

    private static void disableJetpack(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean("JetpackEnabled", false);
        tag.setBoolean("HoverEnabled", false);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new EventHandlerJetpack());
    }
}