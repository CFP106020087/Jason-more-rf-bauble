package com.moremod.fabric.system;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 异界织印 - 灵视/理智平衡控制系统
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class OtherworldBalanceSystem {

    // 恢复物品配置
    private static final int SANITY_RESTORE_COOLDOWN = 200; // 10秒冷却

    /**
     * 玩家Tick事件 - 处理自然恢复和环境影响
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;

        // 检查是否穿戴异界装备
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0) {
            // 穿戴时的持续效果已由其他系统处理
            // 这里处理特殊环境效果
            handleEnvironmentalEffects(player, otherworldCount);
        } else {
            // 未穿戴时的自然恢复
            handleNaturalRecovery(player);
        }

        // 处理特殊物品使用
        handleSpecialItems(player);
    }

    /**
     * 自然恢复机制（未穿戴异界装备时）
     */
    private static void handleNaturalRecovery(EntityPlayer player) {
        NBTTagCompound data = getPlayerOtherworldData(player);
        if (data == null) return;

        int sanity = data.getInteger("Sanity");
        int insight = data.getInteger("Insight");

        // 每20秒恢复1点理智
        if (player.ticksExisted % 400 == 0 && sanity < 100) {
            data.setInteger("Sanity", Math.min(100, sanity + 1));
            updatePlayerOtherworldData(player, data);
        }

        // 每30秒降低1点灵视
        if (player.ticksExisted % 600 == 0 && insight > 0) {
            data.setInteger("Insight", Math.max(0, insight - 1));
            updatePlayerOtherworldData(player, data);
        }
    }

    /**
     * 环境影响效果
     */
    private static void handleEnvironmentalEffects(EntityPlayer player, int otherworldCount) {
        NBTTagCompound data = getPlayerOtherworldData(player);
        if (data == null) return;

        World world = player.world;

        // ========== 理智恢复环境 ==========

        // 1. 阳光下恢复理智（白天，露天）
        if (world.isDaytime() && world.canSeeSky(player.getPosition()) && player.ticksExisted % 100 == 0) {
            int sanity = data.getInteger("Sanity");
            if (sanity < 100) {
                data.setInteger("Sanity", Math.min(100, sanity + 1));
                updatePlayerOtherworldData(player, data);

                if (player.ticksExisted % 400 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "阳光让你感到平静..."), true);
                }
            }
        }

        // 2. 村庄范围恢复理智
        if (world.villageCollection.getNearestVillage(player.getPosition(), 32) != null) {
            if (player.ticksExisted % 200 == 0) {
                int sanity = data.getInteger("Sanity");
                if (sanity < 100) {
                    data.setInteger("Sanity", Math.min(100, sanity + 2));
                    updatePlayerOtherworldData(player, data);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "文明的气息让你恢复理智..."), true);
                }
            }
        }

        // ========== 灵视增长环境 ==========

        // 1. 末地增加灵视
        if (player.dimension == 1 && player.ticksExisted % 100 == 0) {
            int insight = data.getInteger("Insight");
            data.setInteger("Insight", Math.min(100, insight + otherworldCount));
            updatePlayerOtherworldData(player, data);
        }

        // 2. 深度影响（Y<20加速灵视增长）
        if (player.posY < 20 && player.ticksExisted % 200 == 0) {
            int insight = data.getInteger("Insight");
            data.setInteger("Insight", Math.min(100, insight + 1));
            updatePlayerOtherworldData(player, data);
        }

        // 3. 黑暗环境（光照<4）
        if (world.getLight(player.getPosition()) < 4 && player.ticksExisted % 300 == 0) {
            int sanity = data.getInteger("Sanity");
            data.setInteger("Sanity", Math.max(0, sanity - 1));
            updatePlayerOtherworldData(player, data);
        }
    }

    /**
     * 特殊物品使用系统
     */
    private static void handleSpecialItems(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty()) return;

        Item item = heldItem.getItem();

        // 检查冷却
        if (player.getCooldownTracker().hasCooldown(item)) {
            return;
        }

        NBTTagCompound data = getPlayerOtherworldData(player);
        if (data == null) return;

        // ========== 理智恢复物品 ==========

        // 1. 金苹果 - 大幅恢复理智
        if (item == Items.GOLDEN_APPLE) {
            // 右键使用时恢复（需要配合事件）
        }

        // 2. 牛奶 - 清除负面效果，小幅恢复理智
        if (item == Items.MILK_BUCKET) {
            // 饮用时触发
        }

        // 3. 蛋糕 - 恢复理智
        if (item == Item.getItemFromBlock(Blocks.CAKE)) {
            // 食用时触发
        }
    }

    /**
     * 主动技能：冥想（潜行+不动3秒）
     */
    public static void checkMeditation(EntityPlayer player) {
        if (!player.isSneaking()) return;

        NBTTagCompound playerData = player.getEntityData();
        if (!playerData.hasKey("MeditationTime")) {
            playerData.setInteger("MeditationTime", 0);
        }

        // 检查是否在移动
        double speed = Math.abs(player.motionX) + Math.abs(player.motionZ);
        if (speed < 0.01) {
            int meditationTime = playerData.getInteger("MeditationTime");
            meditationTime++;
            playerData.setInteger("MeditationTime", meditationTime);

            // 3秒后开始冥想效果
            if (meditationTime >= 60) {
                if (meditationTime % 40 == 0) { // 每2秒
                    NBTTagCompound data = getPlayerOtherworldData(player);
                    if (data != null) {
                        int sanity = data.getInteger("Sanity");
                        int insight = data.getInteger("Insight");

                        // 恢复理智，降低灵视
                        data.setInteger("Sanity", Math.min(100, sanity + 2));
                        data.setInteger("Insight", Math.max(0, insight - 1));
                        updatePlayerOtherworldData(player, data);

                        // 视觉反馈
                        if (meditationTime % 80 == 0) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.AQUA + "冥想中... 理智恢复中"), true);
                        }
                    }
                }
            }
        } else {
            // 移动时重置冥想
            playerData.setInteger("MeditationTime", 0);
        }
    }

    /**
     * 特殊配方：理智药水
     */
    public static class SanityPotion {

        public static void useSanityPotion(EntityPlayer player) {
            NBTTagCompound data = getPlayerOtherworldData(player);
            if (data == null) return;

            int sanity = data.getInteger("Sanity");
            int insight = data.getInteger("Insight");

            // 大幅恢复理智
            data.setInteger("Sanity", Math.min(100, sanity + 30));
            // 小幅降低灵视
            data.setInteger("Insight", Math.max(0, insight - 10));

            updatePlayerOtherworldData(player, data);

            // 清除负面效果
            player.removePotionEffect(MobEffects.NAUSEA);
            player.removePotionEffect(MobEffects.BLINDNESS);

            // 给予短暂的清明效果
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 600, 0));

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "你的意识变得清晰..."), false);
        }
    }

    /**
     * 特殊配方：洞察药水
     */
    public static class InsightPotion {

        public static void useInsightPotion(EntityPlayer player) {
            NBTTagCompound data = getPlayerOtherworldData(player);
            if (data == null) return;

            int sanity = data.getInteger("Sanity");
            int insight = data.getInteger("Insight");

            // 大幅增加灵视
            data.setInteger("Insight", Math.min(100, insight + 20));
            // 小幅降低理智
            data.setInteger("Sanity", Math.max(0, sanity - 10));

            updatePlayerOtherworldData(player, data);

            // 给予夜视效果
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 1200, 0));

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "异界的知识涌入脑海..."), false);
        }
    }

    /**
     * 获取玩家异界数据
     */
    private static NBTTagCompound getPlayerOtherworldData(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                return FabricWeavingSystem.getFabricData(armor);
            }
        }

        // 即使没有穿戴，也可能有残留数据
        NBTTagCompound playerData = player.getEntityData();
        if (playerData.hasKey("LastOtherworldData")) {
            return playerData.getCompoundTag("LastOtherworldData");
        }

        return null;
    }

    /**
     * 更新玩家异界数据
     */
    private static void updatePlayerOtherworldData(EntityPlayer player, NBTTagCompound data) {
        // 更新到装备
        boolean updated = false;
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                FabricWeavingSystem.updateFabricData(armor, data);
                updated = true;
                break;
            }
        }

        // 保存到玩家数据（用于脱下装备后的持续效果）
        player.getEntityData().setTag("LastOtherworldData", data);
    }
}