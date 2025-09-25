package com.moremod.events;

import com.moremod.item.MerchantPersuader;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "moremod")
public class MerchantTradeEventHandler {

    // 存储玩家的交易监控状态
    private static final Map<UUID, TradeMonitor> TRADE_MONITORS = new HashMap<>();

    // 交易监控器
    public static class TradeMonitor {
        public EntityVillager villager;
        public long startTime;
        public Map<ItemKey, Integer> preTradeInventory;
        public boolean isInTrading = false;
        public int checkDelay = 0; // 延迟检查计数器
        public int pendingTrades = 0; // 待处理的交易计数
        public long lastTradeTime = 0; // 上次交易时间

        public TradeMonitor(EntityVillager villager) {
            this.villager = villager;
            this.startTime = System.currentTimeMillis();
            this.preTradeInventory = new HashMap<>();
        }

        public void captureInventory(EntityPlayer player) {
            preTradeInventory.clear();
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemKey key = new ItemKey(stack);
                    preTradeInventory.put(key, preTradeInventory.getOrDefault(key, 0) + stack.getCount());
                }
            }
        }
    }

    // 物品标识符
    public static class ItemKey {
        public String itemName;
        public int metadata;

        public ItemKey(ItemStack stack) {
            this.itemName = stack.getItem().getRegistryName().toString();
            this.metadata = stack.getMetadata();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ItemKey)) return false;
            ItemKey other = (ItemKey) obj;
            return itemName.equals(other.itemName) && metadata == other.metadata;
        }

        @Override
        public int hashCode() {
            return itemName.hashCode() + metadata;
        }
    }

    @SubscribeEvent
    public static void onPlayerInteractWithVillager(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityVillager)) return;

        EntityPlayer player = event.getEntityPlayer();
        EntityVillager villager = (EntityVillager) event.getTarget();

        // 检查玩家是否持有激活的说服器
        ItemStack persuader = MerchantPersuader.getActivePersuader(player);
        if (!persuader.isEmpty()) {
            UUID playerId = player.getUniqueID();

            // 创建交易监控器
            TradeMonitor monitor = new TradeMonitor(villager);
            monitor.captureInventory(player); // 记录交易前库存
            TRADE_MONITORS.put(playerId, monitor);

            // 给村民标记
            NBTTagCompound villagerData = villager.getEntityData();
            villagerData.setString("persuadedBy", playerId.toString());
            villagerData.setLong("persuadeTime", villager.world.getTotalWorldTime());

            // 显示激活信息
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GOLD + "[说服器] 激活成功，等待交易完成..."), true);

            // 说服特效
            spawnPersuasionParticles(villager);

            // 调试信息
            System.out.println("[DEBUG] 交易监控开始 - 玩家: " + player.getName() + ", 村民: " + villager.getEntityId());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        UUID playerId = player.getUniqueID();

        TradeMonitor monitor = TRADE_MONITORS.get(playerId);
        if (monitor != null) {
            // 检查监控是否过期 (60秒，原来是15秒)
            if (System.currentTimeMillis() - monitor.startTime > 60000) {
                TRADE_MONITORS.remove(playerId);
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GRAY + "[说服器] 交易监控超时 (60秒)"), true);
                return;
            }

            // 检查村民是否还有效 (距离放宽到20格)
            if (monitor.villager.isDead || player.getDistance(monitor.villager) > 20.0) {
                TRADE_MONITORS.remove(playerId);
                return;
            }

            // 检查玩家是否还持有说服器
            ItemStack persuader = MerchantPersuader.getActivePersuader(player);
            if (persuader.isEmpty()) {
                TRADE_MONITORS.remove(playerId);
                return;
            }

            // 检测玩家是否在交易界面中
            boolean currentlyTrading = isPlayerTradingWithVillager(player, monitor.villager);

            if (currentlyTrading && !monitor.isInTrading) {
                // 刚开始交易
                monitor.isInTrading = true;
                monitor.checkDelay = 0;
                System.out.println("[DEBUG] 检测到交易界面打开");

            } else if (!currentlyTrading && monitor.isInTrading) {
                // 交易界面关闭，开始延迟检查
                monitor.isInTrading = false;
                monitor.checkDelay = 40; // 延迟40 tick (2秒) 再检查，原来是10 tick
                System.out.println("[DEBUG] 交易界面关闭，开始延迟检查 (2秒)");

            } else if (!monitor.isInTrading && monitor.checkDelay > 0) {
                // 延迟检查计数
                monitor.checkDelay--;

                if (monitor.checkDelay == 0) {
                    // 延迟结束，检查是否发生了交易
                    if (checkForCompletedTrade(player, monitor, persuader)) {
                        TRADE_MONITORS.remove(playerId);
                    }
                }
            }
        }
    }

    // 处理所有待处理的交易
    private static void processAllPendingTrades(EntityPlayer player, TradeMonitor monitor, ItemStack persuader) {
        if (monitor.pendingTrades == 0) return;

        System.out.println("[DEBUG] 处理 " + monitor.pendingTrades + " 个待处理交易");

        if (checkForCompletedTrade(player, monitor, persuader)) {
            // 显示批量处理信息
            if (monitor.pendingTrades > 1) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.AQUA + "[说服器] 快速连续交易检测 (x" + monitor.pendingTrades + ")"), true);
            }

            // 重置监控状态
            monitor.captureInventory(player); // 更新快照为当前状态
            monitor.startTime = System.currentTimeMillis(); // 重置计时器
            monitor.pendingTrades = 0; // 重置待处理交易计数

            System.out.println("[DEBUG] 批量交易处理完成，准备监控下一次交易");
        } else {
            // 没有检测到交易，重置计数
            monitor.pendingTrades = 0;
        }
    }
    private static boolean isPlayerTradingWithVillager(EntityPlayer player, EntityVillager villager) {
        // 检查玩家当前打开的容器
        if (player.openContainer != null && player.openContainer != player.inventoryContainer) {
            // 检查玩家是否靠近村民 (距离放宽到10格)
            if (player.getDistance(villager) <= 10.0) {
                return true;
            }
        }
        return false;
    }

    // 检查是否完成了交易
    private static boolean checkForCompletedTrade(EntityPlayer player, TradeMonitor monitor, ItemStack persuader) {
        // 获取当前库存
        Map<ItemKey, Integer> currentInventory = new HashMap<>();
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemKey key = new ItemKey(stack);
                currentInventory.put(key, currentInventory.getOrDefault(key, 0) + stack.getCount());
            }
        }

        // 分析库存变化
        Map<ItemKey, Integer> lostItems = new HashMap<>();
        Map<ItemKey, Integer> gainedItems = new HashMap<>();
        boolean hasChanges = false;

        // 检查失去的物品
        for (Map.Entry<ItemKey, Integer> entry : monitor.preTradeInventory.entrySet()) {
            ItemKey key = entry.getKey();
            int oldCount = entry.getValue();
            int newCount = currentInventory.getOrDefault(key, 0);

            if (newCount < oldCount) {
                lostItems.put(key, oldCount - newCount);
                hasChanges = true;
            }
        }

        // 检查获得的物品
        for (Map.Entry<ItemKey, Integer> entry : currentInventory.entrySet()) {
            ItemKey key = entry.getKey();
            int newCount = entry.getValue();
            int oldCount = monitor.preTradeInventory.getOrDefault(key, 0);

            if (newCount > oldCount) {
                gainedItems.put(key, newCount - oldCount);
                hasChanges = true;
            }
        }

        // 如果有变化且有失去物品，说明交易完成
        if (hasChanges && !lostItems.isEmpty() && !gainedItems.isEmpty()) {
            System.out.println("[DEBUG] 检测到交易完成！失去物品: " + lostItems.size() + ", 获得物品: " + gainedItems.size());
            applyTradeDiscount(player, persuader, lostItems);

            // 显示连续交易计数
            if (monitor.villager.getEntityData().hasKey("tradeCount")) {
                int tradeCount = monitor.villager.getEntityData().getInteger("tradeCount") + monitor.pendingTrades;
                monitor.villager.getEntityData().setInteger("tradeCount", tradeCount);
                if (monitor.pendingTrades > 1) {
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.AQUA + "[说服器] 连续交易 #" + (tradeCount - monitor.pendingTrades + 1) + "-" + tradeCount), true);
                } else {
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.AQUA + "[说服器] 连续交易 #" + tradeCount), true);
                }
            } else {
                monitor.villager.getEntityData().setInteger("tradeCount", monitor.pendingTrades);
            }

            return true; // 不移除监控器，继续监控下一次交易
        } else if (hasChanges) {
            System.out.println("[DEBUG] 检测到库存变化，但可能不是交易：失去" + lostItems.size() + "种，获得" + gainedItems.size() + "种");
        }

        return false;
    }

    private static void applyTradeDiscount(EntityPlayer player, ItemStack persuader, Map<ItemKey, Integer> lostItems) {
        if (!(persuader.getItem() instanceof MerchantPersuader)) return;

        MerchantPersuader persuaderItem = (MerchantPersuader) persuader.getItem();

        // 计算折扣
        double discountRate = persuaderItem.getCurrentDiscount(persuader);

        // 检测是否为批量交易
        boolean isBulkTrade = detectBulkTrade(lostItems);
        int tradeCount = 1;

        if (isBulkTrade) {
            tradeCount = calculateTradeCount(lostItems);
            System.out.println("[DEBUG] 检测到批量交易，交易次数: " + tradeCount);
        }

        // 计算总能量消耗
        int totalEnergyCost = 1000 * tradeCount;

        // 检查能量是否足够
        if (persuaderItem.getEnergyStored(persuader) < totalEnergyCost) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.RED + "[说服器] 能量不足！需要 " + totalEnergyCost + " RF (批量交易 x" + tradeCount + ")"), true);
            return;
        }

        // 消耗能量
        persuaderItem.extractEnergy(persuader, totalEnergyCost, false);

        // 计算总交易价值和返还物品
        boolean anyRefund = false;
        StringBuilder refundMessage = new StringBuilder();

        // 返还物品
        for (Map.Entry<ItemKey, Integer> entry : lostItems.entrySet()) {
            ItemKey key = entry.getKey();
            int lostCount = entry.getValue();
            int refundCount = (int) Math.ceil(lostCount * discountRate);

            if (refundCount > 0) {
                // 创建返还物品
                ItemStack refundStack = createItemFromKey(key, refundCount);
                if (!refundStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(refundStack)) {
                        // 背包满了就掉在地上
                        player.dropItem(refundStack, false);
                    }
                    anyRefund = true;

                    // 构建返还信息
                    if (refundMessage.length() > 0) {
                        refundMessage.append(", ");
                    }
                    refundMessage.append(refundStack.getDisplayName())
                            .append(" x").append(refundCount);

                    // 调试信息
                    System.out.println("[DEBUG] 返还物品: " + refundStack.getDisplayName() +
                            " x" + refundCount + " (原始: " + lostCount +
                            ", 折扣率: " + String.format("%.1f", discountRate * 100) + "%)");
                }
            }
        }

        if (anyRefund) {
            // 显示详细的返还信息
            if (isBulkTrade) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GREEN + "[说服器] 批量交易折扣已生效！(x" + tradeCount + ")"), true);

                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.YELLOW + "折扣率: " +
                                String.format("%.1f", discountRate * 100) + "% | 能量消耗: " + totalEnergyCost + " RF"), true);
            } else {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GREEN + "[说服器] 交易折扣已生效！"), true);
            }

            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.YELLOW + "返还: " + refundMessage.toString()), true);

            // 成功特效
            spawnTradeSuccessParticles(player);

        } else {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.YELLOW + "[说服器] 未找到可返还的物品"), true);
        }
    }

    // 检测是否为批量交易
    private static boolean detectBulkTrade(Map<ItemKey, Integer> lostItems) {
        // 检查是否有大量同类物品
        for (Map.Entry<ItemKey, Integer> entry : lostItems.entrySet()) {
            int count = entry.getValue();
            // 如果某种物品数量超过16，可能是批量交易
            if (count >= 16) {
                return true;
            }
        }

        // 检查总物品数量
        int totalItems = lostItems.values().stream().mapToInt(Integer::intValue).sum();
        return totalItems >= 32; // 总计超过32个物品可能是批量交易
    }

    // 计算交易次数
    private static int calculateTradeCount(Map<ItemKey, Integer> lostItems) {
        int maxTradeCount = 1;

        // 基于物品数量估算交易次数
        for (Map.Entry<ItemKey, Integer> entry : lostItems.entrySet()) {
            int count = entry.getValue();

            // 根据物品类型估算单次交易的数量
            int estimatedPerTrade = getEstimatedItemsPerTrade(entry.getKey());
            int estimatedTrades = Math.max(1, count / estimatedPerTrade);

            maxTradeCount = Math.max(maxTradeCount, estimatedTrades);
        }

        // 限制最大交易次数，避免过度消耗能量
        return Math.min(maxTradeCount, 64);
    }

    // 估算单次交易的物品数量
    private static int getEstimatedItemsPerTrade(ItemKey key) {
        // 根据物品类型返回估算的单次交易数量
        String itemName = key.itemName.toLowerCase();

        if (itemName.contains("emerald")) {
            return 2; // 绿宝石通常2-4个一次交易
        } else if (itemName.contains("diamond")) {
            return 1; // 钻石通常1个一次交易
        } else if (itemName.contains("iron") || itemName.contains("gold")) {
            return 4; // 金属锭通常4-8个一次交易
        } else if (itemName.contains("wheat") || itemName.contains("bread")) {
            return 8; // 食物类通常8-16个一次交易
        } else {
            return 4; // 默认估算
        }
    }

    private static ItemStack createItemFromKey(ItemKey key, int count) {
        try {
            net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(key.itemName);
            if (item != null) {
                return new ItemStack(item, count, key.metadata);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 无法创建物品: " + key.itemName + ":" + key.metadata);
        }
        return ItemStack.EMPTY;
    }

    private static void spawnPersuasionParticles(EntityVillager villager) {
        for (int i = 0; i < 10; i++) {
            villager.world.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    villager.posX + (villager.world.rand.nextDouble() - 0.5) * 1.5,
                    villager.posY + 1.5 + villager.world.rand.nextDouble() * 0.5,
                    villager.posZ + (villager.world.rand.nextDouble() - 0.5) * 1.5,
                    0, 0.05, 0);
        }
    }

    private static void spawnTradeSuccessParticles(EntityPlayer player) {
        for (int i = 0; i < 20; i++) {
            player.world.spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH,
                    player.posX + (player.world.rand.nextDouble() - 0.5) * 2,
                    player.posY + player.world.rand.nextDouble() * 2,
                    player.posZ + (player.world.rand.nextDouble() - 0.5) * 2,
                    0, 0.1, 0);
        }
    }

    // 清理断开连接的玩家数据
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        TRADE_MONITORS.remove(event.player.getUniqueID());
    }
}

