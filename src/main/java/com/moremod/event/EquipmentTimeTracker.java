// EquipmentTimeTracker.java - 修复静态调用错误
package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.config.EquipmentTimeConfig;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械核心佩戴时间追踪系统
 * 追踪玩家首次加入时间，并在超时后永久禁止佩戴
 */
public class EquipmentTimeTracker {

    // NBT 键
    private static final String NBT_FIRST_JOIN_TIME = "MechCore_FirstJoinTime";
    private static final String NBT_EQUIPPED_IN_TIME = "MechCore_EquippedInTime";
    private static final String NBT_PERMANENTLY_BANNED = "MechCore_PermanentlyBanned";

    // 运行时缓存
    private static final Map<UUID, PlayerTimeData> playerDataCache = new ConcurrentHashMap<>();

    // 玩家时间数据
    private static class PlayerTimeData {
        long firstJoinTime;
        boolean equippedInTime;
        boolean permanentlyBanned;
        Set<Integer> warningsSent = new HashSet<>();

        PlayerTimeData(long firstJoinTime, boolean equippedInTime, boolean permanentlyBanned) {
            this.firstJoinTime = firstJoinTime;
            this.equippedInTime = equippedInTime;
            this.permanentlyBanned = permanentlyBanned;
        }
    }

    /**
     * 注册事件监听器
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new EquipmentTimeTracker());
    }

    /**
     * 玩家登录事件
     */

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!EquipmentTimeConfig.restriction.enabled) {
            return;
        }

        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) {
            return;
        }

        try {
            // 检查是否已装备机械核心或Enigmatic物品
            boolean hasMechanicalCore = checkHasMechanicalCore(player);
            boolean hasEnigmatic = checkHasEnigmaticItems(player);

            NBTTagCompound playerData = player.getEntityData();
            NBTTagCompound persistentData = getOrCreatePersistentData(playerData);

            long currentTime = System.currentTimeMillis();
            boolean isFirstJoin = !persistentData.hasKey(NBT_FIRST_JOIN_TIME);

            if (isFirstJoin) {
                persistentData.setLong(NBT_FIRST_JOIN_TIME, currentTime);

                // 如果已经装备了机械核心，直接标记为已佩戴
                if (hasMechanicalCore) {
                    persistentData.setBoolean(NBT_EQUIPPED_IN_TIME, true);
                    persistentData.setBoolean(NBT_PERMANENTLY_BANNED, false);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ 检测到你已装备机械核心，时间限制已解除！"
                    ));
                }
                // 如果装备了Enigmatic物品，显示警告但不显示欢迎信息
                else if (hasEnigmatic) {
                    persistentData.setBoolean(NBT_EQUIPPED_IN_TIME, false);
                    persistentData.setBoolean(NBT_PERMANENTLY_BANNED, false);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 检测到Enigmatic物品，请注意与机械核心互斥！"
                    ));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "你需要在 " +
                                    formatTime(EquipmentTimeConfig.restriction.timeLimit) +
                                    " 内卸下Enigmatic并装备机械核心"
                    ));
                }
                // 都没有装备，显示欢迎消息
                else {
                    persistentData.setBoolean(NBT_EQUIPPED_IN_TIME, false);
                    persistentData.setBoolean(NBT_PERMANENTLY_BANNED, false);

                    if (EquipmentTimeConfig.restriction.sendWelcome) {
                        sendWelcomeMessage(player);
                    }
                }
            }

            // 加载玩家数据到缓存
            loadPlayerData(player);

            // 检查是否已经超时
            checkAndUpdateBanStatus(player);

            // 强制同步数据
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).connection.sendPacket(
                        new SPacketEntityProperties(
                                player.getEntityId(),
                                player.getAttributeMap().getAllAttributes()
                        )
                );
            }

        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 处理玩家登录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 玩家tick事件 - 用于倒计时警告
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!EquipmentTimeConfig.restriction.enabled) {
            return;
        }

        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;

        // 每5秒检查一次（100 ticks）
        if (player.world.getTotalWorldTime() % 100 != 0) {
            return;
        }

        checkAndSendWarnings(player);
    }

    /**
     * 检查并发送警告
     */
    private void checkAndSendWarnings(EntityPlayer player) {
        PlayerTimeData data = playerDataCache.get(player.getUniqueID());
        if (data == null || data.equippedInTime || data.permanentlyBanned) {
            return;
        }

        if (!EquipmentTimeConfig.restriction.showWarnings) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - data.firstJoinTime) / 1000;
        long remainingSeconds = EquipmentTimeConfig.restriction.timeLimit - elapsedSeconds;

        if (remainingSeconds <= 0) {
            return; // 已经超时，由其他逻辑处理
        }

        // 检查警告时间点
        for (int warningPoint : EquipmentTimeConfig.restriction.warningPoints) {
            if (remainingSeconds <= warningPoint && !data.warningsSent.contains(warningPoint)) {
                data.warningsSent.add(warningPoint);
                sendWarningMessage(player, remainingSeconds);
                break;
            }
        }
    }

    /**
     * 检查并更新封禁状态
     */
    private static void checkAndUpdateBanStatus(EntityPlayer player) {
        PlayerTimeData data = playerDataCache.get(player.getUniqueID());
        if (data == null || data.equippedInTime || data.permanentlyBanned) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - data.firstJoinTime) / 1000;

        if (elapsedSeconds > EquipmentTimeConfig.restriction.timeLimit) {
            // 超时，永久禁止
            setBanned(player, true);
            sendBanMessage(player);
        }
    }

    /**
     * 当玩家装备机械核心时调用
     */
    public static void onCoreEquipped(EntityPlayer player) {
        if (!EquipmentTimeConfig.restriction.enabled) {
            return;
        }

        PlayerTimeData data = playerDataCache.get(player.getUniqueID());
        if (data == null) {
            loadPlayerData(player);
            data = playerDataCache.get(player.getUniqueID());
        }

        if (data == null) {
            return;
        }

        // 如果已经被永久禁止，不应该执行到这里
        if (data.permanentlyBanned) {
            return;
        }

        // 如果已经标记为"已及时佩戴"，不重复提示
        if (data.equippedInTime) {
            return;
        }

        // 检查当前是否已经超时
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - data.firstJoinTime) / 1000;

        if (elapsedSeconds > EquipmentTimeConfig.restriction.timeLimit) {
            // 3B：超时了！即使此刻装备也已经晚了
            setBanned(player, true);

            if (!player.world.isRemote) {
                long overtimeSeconds = elapsedSeconds - EquipmentTimeConfig.restriction.timeLimit;
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 已超过时间限制！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "你在装备时已经超时 " +
                                formatTime((int)overtimeSeconds)
                ));
                sendBanMessage(player);
            }
            return;
        }

        // 3A：成功在时间内佩戴
        setEquippedInTime(player, true);

        if (!player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 你已成功在规定时间内佩戴机械核心！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "时间限制已解除，你可以随时使用它。"
            ));

            // 显示用了多少时间
            long usedSeconds = elapsedSeconds;
            String usedTime = formatTime((int)usedSeconds);
            long remainSeconds = EquipmentTimeConfig.restriction.timeLimit - elapsedSeconds;
            String remainTime = formatTime((int)remainSeconds);

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "⏱ 已用时间: " + usedTime +
                            TextFormatting.GRAY + " / 剩余: " + remainTime
            ));
        }
    }

    /**
     * 检查玩家是否被永久禁止
     */
    public static boolean isPermanentlyBanned(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        if (!EquipmentTimeConfig.restriction.enabled) {
            return false;
        }

        try {
            PlayerTimeData data = playerDataCache.get(player.getUniqueID());
            if (data == null) {
                loadPlayerData(player);
                data = playerDataCache.get(player.getUniqueID());
            }

            return data != null && data.permanentlyBanned;
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 检查封禁状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查玩家是否已在时间内佩戴过
     */
    public static boolean hasEquippedInTime(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        if (!EquipmentTimeConfig.restriction.enabled) {
            return true; // 未启用限制，视为已佩戴
        }

        try {
            PlayerTimeData data = playerDataCache.get(player.getUniqueID());
            if (data == null) {
                loadPlayerData(player);
                data = playerDataCache.get(player.getUniqueID());
            }

            return data != null && data.equippedInTime;
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 检查佩戴状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取剩余时间（秒）
     * @return 剩余秒数；0表示已超时；-1表示已佩戴过或无限制
     */
    public static long getRemainingTime(EntityPlayer player) {
        if (player == null) {
            return -1;
        }

        if (!EquipmentTimeConfig.restriction.enabled) {
            return -1;
        }

        try {
            PlayerTimeData data = playerDataCache.get(player.getUniqueID());
            if (data == null) {
                loadPlayerData(player);
                data = playerDataCache.get(player.getUniqueID());
            }

            // 已经佩戴过，返回-1表示无限制
            if (data == null || data.equippedInTime) {
                return -1;
            }

            // 已被禁止，返回0
            if (data.permanentlyBanned) {
                return 0;
            }

            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - data.firstJoinTime) / 1000;
            long remaining = EquipmentTimeConfig.restriction.timeLimit - elapsedSeconds;

            // 返回0表示刚好到时间或已超时
            return Math.max(0, remaining);
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 计算剩余时间失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 立即标记玩家为禁止状态
     */
    public static void markAsBanned(EntityPlayer player) {
        if (player == null) {
            return;
        }

        try {
            setBanned(player, true);

            if (!player.world.isRemote) {
                sendBanMessage(player);

                // 记录到日志
                System.out.println("[EquipmentTimeTracker] 玩家 " + player.getName() +
                        " 因超时未佩戴而被永久禁止");
            }
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 标记禁止状态失败: " + e.getMessage());
        }
    }

    /**
     * 管理员重置玩家限制
     */

    public static boolean resetPlayerRestriction(EntityPlayer player) {
        if (!EquipmentTimeConfig.restriction.allowAdminReset) {
            return false;
        }

        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistentData = getOrCreatePersistentData(playerData);

        // 清除所有相关NBT
        persistentData.removeTag(NBT_FIRST_JOIN_TIME);
        persistentData.removeTag(NBT_EQUIPPED_IN_TIME);
        persistentData.removeTag(NBT_PERMANENTLY_BANNED);

        // 清除缓存
        playerDataCache.remove(player.getUniqueID());

        return true;
    }

    // ========== 私有辅助方法 ==========

    private static void loadPlayerData(EntityPlayer player) {
        if (player == null) {
            return;
        }

        try {
            NBTTagCompound playerData = player.getEntityData();
            if (playerData == null) {
                return;
            }

            NBTTagCompound persistentData = getOrCreatePersistentData(playerData);

            long firstJoinTime = persistentData.getLong(NBT_FIRST_JOIN_TIME);
            boolean equippedInTime = persistentData.getBoolean(NBT_EQUIPPED_IN_TIME);
            boolean permanentlyBanned = persistentData.getBoolean(NBT_PERMANENTLY_BANNED);

            playerDataCache.put(player.getUniqueID(),
                    new PlayerTimeData(firstJoinTime, equippedInTime, permanentlyBanned));
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 加载玩家数据异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static boolean checkHasMechanicalCore(EntityPlayer player) {
        if (player == null) return false;

        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof ItemMechanicalCore) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * 检查玩家是否已装备Enigmatic物品
     */
    public static boolean checkHasEnigmaticItems(EntityPlayer player) {
        if (player == null) return false;

        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && isEnigmaticItem(bauble)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * 判断物品是否为Enigmatic系列
     */
    private static boolean isEnigmaticItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) return false;

        String modId = registryName.getNamespace().toLowerCase();
        String itemPath = registryName.getPath().toLowerCase();

        return modId.contains("enigma") || itemPath.contains("enigma");
    }
    private static void setEquippedInTime(EntityPlayer player, boolean equipped) {
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistentData = getOrCreatePersistentData(playerData);
        persistentData.setBoolean(NBT_EQUIPPED_IN_TIME, equipped);

        PlayerTimeData data = playerDataCache.get(player.getUniqueID());
        if (data != null) {
            data.equippedInTime = equipped;
        }
    }

    private static void setBanned(EntityPlayer player, boolean banned) {
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistentData = getOrCreatePersistentData(playerData);
        persistentData.setBoolean(NBT_PERMANENTLY_BANNED, banned);

        PlayerTimeData data = playerDataCache.get(player.getUniqueID());
        if (data != null) {
            data.permanentlyBanned = banned;
        }
    }

    private static NBTTagCompound getOrCreatePersistentData(NBTTagCompound playerData) {
        if (playerData == null) {
            return new NBTTagCompound();
        }

        try {
            if (!playerData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
            }
            return playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 获取持久化数据失败: " + e.getMessage());
            return new NBTTagCompound();
        }
    }

    /**
     * ✅ 改为静态方法：发送欢迎消息
     */
    private static void sendWelcomeMessage(EntityPlayer player) {
        if (player == null) {
            return;
        }

        try {
            String message = EquipmentTimeConfig.messages.welcome;

            // 空消息检查
            if (message == null || message.trim().isEmpty()) {
                message = "&a欢迎来到服务器！\n&e请注意：你需要在 &c{time} &e内佩戴机械核心\n&7否则将永久无法使用该物品！";
            }

            int timeLimit = EquipmentTimeConfig.restriction.timeLimit;

            // 替换时间占位符
            String timeStr = formatTime(timeLimit);
            message = message.replace("{time}", timeStr);

            // 分割并发送每一行
            String[] lines = message.split("\n");
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    player.sendMessage(new TextComponentString(
                            translateColorCodes(line)
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 发送欢迎消息失败: " + e.getMessage());
            e.printStackTrace();

            // 发送简化的错误提示
            try {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚠ 机械核心佩戴时间限制已启用"
                ));
            } catch (Exception ignored) {}
        }
    }

    /**
     * ✅ 改为静态方法：发送警告消息
     */
    private static void sendWarningMessage(EntityPlayer player, long remainingSeconds) {
        if (player == null) {
            return;
        }

        try {
            String timeStr = formatTime((int) remainingSeconds);

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 警告：机械核心佩戴时间还剩 " +
                            TextFormatting.RED + timeStr
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "请尽快佩戴，否则将永久无法使用！"
            ));
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 发送警告消息失败: " + e.getMessage());
        }
    }

    /**
     * ✅ 改为静态方法：发送封禁消息
     */
    private static void sendBanMessage(EntityPlayer player) {
        if (player == null) {
            return;
        }

        try {
            String message = EquipmentTimeConfig.messages.banned;

            // 空消息检查
            if (message == null || message.trim().isEmpty()) {
                message = "&c✗ 你已错过佩戴机械核心的时机！\n&7你在首次加入后的规定时间内未能佩戴它。\n&e联系管理员以获取帮助。";
            }

            // 分割并发送每一行
            String[] lines = message.split("\n");
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    player.sendMessage(new TextComponentString(
                            translateColorCodes(line)
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 发送封禁消息失败: " + e.getMessage());

            // 发送简化的错误提示
            try {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 你已被永久禁止佩戴机械核心"
                ));
            } catch (Exception ignored) {}
        }
    }

    /**
     * ✅ 改为静态方法：颜色代码转换
     */
    private static String translateColorCodes(String text) {
        if (text == null) {
            return "";
        }

        try {
            return text.replaceAll("&([0-9a-fk-or])", "§$1");
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 时间格式化
     */
    private static String formatTime(int seconds) {
        try {
            if (seconds >= 3600) {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
            } else if (seconds >= 60) {
                int minutes = seconds / 60;
                int secs = seconds % 60;
                return minutes + "分钟" + (secs > 0 ? secs + "秒" : "");
            } else {
                return seconds + "秒";
            }
        } catch (Exception e) {
            return "未知时间";
        }
    }
}