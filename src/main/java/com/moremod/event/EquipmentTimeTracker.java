// EquipmentTimeTracker.java - 完整修复版
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

            // ✅ 迁移旧数据（从 ForgeData 迁移到 PlayerPersisted）
            migrateOldData(playerData);

            NBTTagCompound persistentData = getOrCreatePersistentData(playerData);

            long currentTime = System.currentTimeMillis();
            boolean isFirstJoin = !persistentData.hasKey(NBT_FIRST_JOIN_TIME);

            // ✅ 调试日志
            System.out.println("========== 玩家登录数据检查 ==========");
            System.out.println("玩家: " + player.getName());
            System.out.println("是否首次加入: " + isFirstJoin);
            System.out.println("首次加入时间: " + persistentData.getLong(NBT_FIRST_JOIN_TIME));
            System.out.println("已佩戴标记: " + persistentData.getBoolean(NBT_EQUIPPED_IN_TIME));
            System.out.println("封禁状态: " + persistentData.getBoolean(NBT_PERMANENTLY_BANNED));
            System.out.println("==================================");

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
            // 超时了！即使此刻装备也已经晚了
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

        // 成功在时间内佩戴
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

        try {
            NBTTagCompound playerData = player.getEntityData();
            NBTTagCompound persistentData = getOrCreatePersistentData(playerData);

            // ✅ 重置为当前时间，给新的时间窗口
            long currentTime = System.currentTimeMillis();
            persistentData.setLong(NBT_FIRST_JOIN_TIME, currentTime);
            persistentData.setBoolean(NBT_EQUIPPED_IN_TIME, false);
            persistentData.setBoolean(NBT_PERMANENTLY_BANNED, false);

            // 清除缓存
            playerDataCache.remove(player.getUniqueID());

            // ✅ 调试日志
            System.out.println("========== 重置玩家数据 ==========");
            System.out.println("玩家: " + player.getName());
            System.out.println("新的首次加入时间: " + currentTime);
            System.out.println("==================================");

            return true;

        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 重置玩家限制失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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

    /**
     * ✅ 迁移旧数据：从 ForgeData 迁移到 PlayerPersisted
     * 确保现有玩家的数据不会丢失
     */
    private static void migrateOldData(NBTTagCompound playerData) {
        if (playerData == null) return;

        try {
            final String OLD_FORGE_DATA = "ForgeData";
            final String OLD_MOREMOD_DATA = "moremod_equipment_time";
            final String PERSISTED_TAG = EntityPlayer.PERSISTED_NBT_TAG;

            // 检查是否有旧数据
            if (!playerData.hasKey(OLD_FORGE_DATA)) return;

            NBTTagCompound forgeData = playerData.getCompoundTag(OLD_FORGE_DATA);
            if (!forgeData.hasKey(OLD_MOREMOD_DATA)) return;

            NBTTagCompound oldData = forgeData.getCompoundTag(OLD_MOREMOD_DATA);

            // 检查旧数据是否有有效内容
            if (!oldData.hasKey(NBT_FIRST_JOIN_TIME)) return;

            // 确保新位置存在
            if (!playerData.hasKey(PERSISTED_TAG)) {
                playerData.setTag(PERSISTED_TAG, new NBTTagCompound());
            }
            NBTTagCompound persistedData = playerData.getCompoundTag(PERSISTED_TAG);

            // 检查新位置是否已有数据（避免覆盖）
            if (persistedData.hasKey(OLD_MOREMOD_DATA)) {
                NBTTagCompound newData = persistedData.getCompoundTag(OLD_MOREMOD_DATA);
                if (newData.hasKey(NBT_FIRST_JOIN_TIME)) {
                    // 新位置已有数据，删除旧数据
                    forgeData.removeTag(OLD_MOREMOD_DATA);
                    System.out.println("[EquipmentTimeTracker] 新位置已有数据，跳过迁移并清理旧数据");
                    return;
                }
            }

            // 迁移数据到新位置
            persistedData.setTag(OLD_MOREMOD_DATA, oldData.copy());

            // 删除旧位置的数据
            forgeData.removeTag(OLD_MOREMOD_DATA);

            System.out.println("[EquipmentTimeTracker] ✅ 已将玩家数据从 ForgeData 迁移到 PlayerPersisted");
            System.out.println("  - FirstJoinTime: " + oldData.getLong(NBT_FIRST_JOIN_TIME));
            System.out.println("  - EquippedInTime: " + oldData.getBoolean(NBT_EQUIPPED_IN_TIME));
            System.out.println("  - PermanentlyBanned: " + oldData.getBoolean(NBT_PERMANENTLY_BANNED));

        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 迁移旧数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ 修复：使用 EntityPlayer.PERSISTED_NBT_TAG 存储数据
     * 这是 Minecraft 专门用于跨会话保留玩家数据的标签
     */
    private static NBTTagCompound getOrCreatePersistentData(NBTTagCompound playerData) {
        if (playerData == null) {
            return new NBTTagCompound();
        }

        try {
            // ✅ 使用 EntityPlayer.PERSISTED_NBT_TAG ("PlayerPersisted")
            // 这个标签会在玩家退出/重新登录时保留
            final String PERSISTED_TAG = EntityPlayer.PERSISTED_NBT_TAG;
            final String MOREMOD_DATA = "moremod_equipment_time";

            // 确保 PlayerPersisted 存在
            if (!playerData.hasKey(PERSISTED_TAG)) {
                playerData.setTag(PERSISTED_TAG, new NBTTagCompound());
            }

            NBTTagCompound persistedData = playerData.getCompoundTag(PERSISTED_TAG);

            // 确保我们的数据标签存在
            if (!persistedData.hasKey(MOREMOD_DATA)) {
                persistedData.setTag(MOREMOD_DATA, new NBTTagCompound());
            }

            return persistedData.getCompoundTag(MOREMOD_DATA);

        } catch (Exception e) {
            System.err.println("[EquipmentTimeTracker] 获取持久化数据失败: " + e.getMessage());
            return new NBTTagCompound();
        }
    }

    /**
     * 发送欢迎消息
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
            String[] lines = message.split("\\\\n");
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
     * 发送警告消息
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
     * 发送封禁消息
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
            String[] lines = message.split("\\\\n");
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
     * 颜色代码转换
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