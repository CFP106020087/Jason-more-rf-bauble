package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MechanicalHeartEventHandler {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        World world = player.world;

        ItemStack mechanicalHeart = findMechanicalHeartInInventory(player);

        if (mechanicalHeart != null && ItemMechanicalHeart.isFullyCharged(mechanicalHeart)) {
            // 取消死亡事件
            event.setCanceled(true);

            // 消耗所有能量
            ItemMechanicalHeart.consumeAllEnergy(mechanicalHeart);

            // 恢复玩家状态
            player.setHealth(player.getMaxHealth());
            player.getFoodStats().setFoodLevel(20);
            player.getFoodStats().setFoodSaturationLevel(5.0F);
            player.clearActivePotions();

            // 添加瞬间治疗效果来修复First Aid模组的身体部位血量
            player.addPotionEffect(new PotionEffect(MobEffects.INSTANT_HEALTH, 100, 9, false, false));

            // 检查复活模式并执行相应逻辑
            boolean isInPlaceRevive = getReviveMode(mechanicalHeart);

            if (player instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP) player;

                if (isInPlaceRevive) {
                    // 原地复活模式
                    BlockPos currentPos = findSafeReviveLocation(world, player.getPosition());
                    playerMP.connection.setPlayerLocation(
                            currentPos.getX() + 0.5,
                            currentPos.getY(),
                            currentPos.getZ() + 0.5,
                            playerMP.rotationYaw,
                            playerMP.rotationPitch
                    );

                    // 原地复活消息
                    playerMP.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "" + TextFormatting.BOLD + "时间之力在此刻凝聚，你在原地重获新生"));
                    playerMP.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "当前模式：" + TextFormatting.GREEN + "原地复活"));
                } else {
                    // 传送到出生点模式
                    BlockPos spawnPos = getPlayerSpawnPoint(playerMP);
                    playerMP.connection.setPlayerLocation(
                            spawnPos.getX() + 0.5,
                            spawnPos.getY(),
                            spawnPos.getZ() + 0.5,
                            playerMP.rotationYaw,
                            playerMP.rotationPitch
                    );

                    // 出生点复活消息
                    playerMP.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "" + TextFormatting.BOLD + "你感到神秘的力量回溯了时间，保住了你一命"));
                    playerMP.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "当前模式：" + TextFormatting.LIGHT_PURPLE + "传送复活"));
                }

                playerMP.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "时间之力已经耗尽，需要重新汲取能量..."));
                playerMP.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "" + TextFormatting.ITALIC + "使用 Shift + 右键 可切换复活模式"));
            }

            // 🎵 只在复活时播放您的4秒音效！


            // 神秘粒子效果
            if (!world.isRemote) {
                if (isInPlaceRevive) {
                    spawnInPlaceReviveParticles(world, player);
                } else {
                    spawnMysticalReviveParticles(world, player);
                }
            }
        }
    }

    // 获取复活模式 (true = 原地复活, false = 传送复活)
    private boolean getReviveMode(ItemStack mechanicalHeart) {
        NBTTagCompound nbt = mechanicalHeart.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            mechanicalHeart.setTagCompound(nbt);
        }
        // 默认为传送复活模式
        return nbt.getBoolean("inPlaceRevive");
    }

    // 设置复活模式
    public static void toggleReviveMode(ItemStack mechanicalHeart) {
        NBTTagCompound nbt = mechanicalHeart.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            mechanicalHeart.setTagCompound(nbt);
        }
        boolean currentMode = nbt.getBoolean("inPlaceRevive");
        nbt.setBoolean("inPlaceRevive", !currentMode);
    }

    // 获取复活模式的显示文本
    public static String getReviveModeText(ItemStack mechanicalHeart) {
        boolean isInPlace = false;
        NBTTagCompound nbt = mechanicalHeart.getTagCompound();
        if (nbt != null) {
            isInPlace = nbt.getBoolean("inPlaceRevive");
        }
        return isInPlace ?
                TextFormatting.GREEN + "原地复活" :
                TextFormatting.LIGHT_PURPLE + "传送复活";
    }

    // 原地复活的粒子效果
    private void spawnInPlaceReviveParticles(World world, EntityPlayer player) {
        double x = player.posX;
        double y = player.posY + 1.0;
        double z = player.posZ;

        // 生成向上的时间流粒子
        for (int i = 0; i < 25; i++) {
            double angle = (i * 14.4) * Math.PI / 180; // 每14.4度一个粒子
            double radius = 1.0 + (i * 0.05); // 逐渐扩大的螺旋
            double height = i * 0.15;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // 绿色时间粒子
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    x + offsetX, y + offsetY, z + offsetZ,
                    -offsetX * 0.05, 0.3, -offsetZ * 0.05);
        }

        // 中心绿色能量爆发
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 2.0;
            double offsetY = world.rand.nextDouble() * 1.5;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 2.0;

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.HEART,
                    x + offsetX, y + offsetY, z + offsetZ,
                    offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
        }

        // 地面光环效果
        for (int i = 0; i < 16; i++) {
            double angle = (i * 22.5) * Math.PI / 180; // 每22.5度一个粒子，形成圆形
            double radius = 1.5;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                    x + offsetX, y - 0.5, z + offsetZ,
                    0, 0.2, 0);
        }
    }

    // 神秘的复活粒子效果（传送复活）
    private void spawnMysticalReviveParticles(World world, EntityPlayer player) {
        double x = player.posX;
        double y = player.posY + 1.0;
        double z = player.posZ;

        // 生成神秘的紫色粒子螺旋
        for (int i = 0; i < 30; i++) {
            double angle = (i * 12) * Math.PI / 180; // 每12度一个粒子
            double radius = 1.5;
            double height = (i * 0.1) % 2.0; // 螺旋上升

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // 紫色魔法粒子
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    -offsetX * 0.1, 0.2, -offsetZ * 0.1);

            // 神秘光芒
            if (i % 5 == 0) {
                world.spawnParticle(net.minecraft.util.EnumParticleTypes.END_ROD,
                        x + offsetX * 0.5, y + offsetY, z + offsetZ * 0.5,
                        0, 0.1, 0);
            }
        }

        // 中心时间裂缝效果
        for (int i = 0; i < 15; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.3;
            double offsetY = world.rand.nextDouble() * 0.3;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.3;

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL,
                    x + offsetX, y + offsetY, z + offsetZ,
                    offsetX, offsetY + 0.1, offsetZ);
        }

        // 外圈金色粒子环
        for (int i = 0; i < 20; i++) {
            double angle = (i * 18) * Math.PI / 180; // 每18度一个粒子，形成圆形
            double radius = 2.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = world.rand.nextDouble() * 0.5;

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    x + offsetX, y + offsetY, z + offsetZ,
                    offsetX * 0.1, 0.1, offsetZ * 0.1);
        }
    }

    // 寻找背包中的时间之心
    private ItemStack findMechanicalHeartInInventory(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemMechanicalHeart) {
                return stack;
            }
        }
        return null;
    }

    // 获取玩家出生点
    private BlockPos getPlayerSpawnPoint(EntityPlayerMP player) {
        BlockPos spawnPos = player.getBedLocation(player.dimension);
        if (spawnPos == null) {
            spawnPos = player.world.getSpawnPoint();
        }
        return findSafeSpawnLocation(player.world, spawnPos);
    }

    // 寻找安全的出生位置
    private BlockPos findSafeSpawnLocation(World world, BlockPos original) {
        for (int y = 0; y < 5; y++) {
            BlockPos checkPos = original.up(y);
            if (world.isAirBlock(checkPos) && world.isAirBlock(checkPos.up()) &&
                    !world.isAirBlock(checkPos.down())) {
                return checkPos;
            }
        }
        return original;
    }

    // 寻找安全的原地复活位置
    private BlockPos findSafeReviveLocation(World world, BlockPos deathPos) {
        // 首先检查死亡位置是否安全
        if (world.isAirBlock(deathPos) && world.isAirBlock(deathPos.up()) &&
                !world.isAirBlock(deathPos.down())) {
            return deathPos;
        }

        // 检查周围位置
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 3; y++) {
                    BlockPos checkPos = deathPos.add(x, y, z);
                    if (world.isAirBlock(checkPos) && world.isAirBlock(checkPos.up()) &&
                            !world.isAirBlock(checkPos.down())) {
                        return checkPos;
                    }
                }
            }
        }

        // 如果找不到安全位置，返回死亡位置上方
        return deathPos.up();
    }
}