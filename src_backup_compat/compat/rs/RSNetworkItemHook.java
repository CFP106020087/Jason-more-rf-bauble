package com.moremod.compat.rs;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * RS 网络物品钩子 - 1.12.2 版本
 * 处理跨维度访问逻辑
 * 
 * 目标类: com.raoulvdberge.refinedstorage.apiimpl.network.item.NetworkItemHandler
 */
public class RSNetworkItemHook {
    
    // 缓存反射对象
    private static Field networkField;
    private static Field itemsField;
    private static boolean initialized = false;
    private static boolean initFailed = false;
    
    private static void init() {
        if (initialized || initFailed) return;
        
        try {
            Class<?> handlerClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.network.item.NetworkItemHandler");
            
            networkField = handlerClass.getDeclaredField("network");
            networkField.setAccessible(true);
            
            itemsField = handlerClass.getDeclaredField("items");
            itemsField.setAccessible(true);
            
            initialized = true;
            System.out.println("[RSNetworkItemHook] Initialized successfully");
        } catch (Exception e) {
            System.err.println("[RSNetworkItemHook] Failed to init: " + e.getMessage());
            e.printStackTrace();
            initFailed = true;
        }
    }
    
    /**
     * 处理无线物品打开
     * 
     * @param handler NetworkItemHandler 实例
     * @param player 玩家
     * @param stack 物品栈
     * @param slotId 槽位ID
     * @return true 如果已处理（跨维度情况），false 让原始逻辑处理
     */
    @SuppressWarnings("unchecked")
    public static boolean onOpen(Object handler, EntityPlayer player, ItemStack stack, int slotId) {
        init();
        if (initFailed) return false;
        
        try {
            Object network = networkField.get(handler);
            if (network == null) return false;
            
            // 获取 network.getNodeGraph().all()
            Method getNodeGraphMethod = network.getClass().getMethod("getNodeGraph");
            Object nodeGraph = getNodeGraphMethod.invoke(network);
            
            Method allMethod = nodeGraph.getClass().getMethod("all");
            Iterable<?> nodes = (Iterable<?>) allMethod.invoke(nodeGraph);
            
            // 接口类
            Class<?> transmitterInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.IWirelessTransmitter");
            Class<?> networkNodeInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.node.INetworkNode");
            
            int playerDimension = player.dimension;
            Object bestCrossDimensionNode = null;
            
            for (Object node : nodes) {
                if (!transmitterInterface.isInstance(node)) continue;
                
                // 检查 canUpdate
                Method canUpdateMethod = networkNodeInterface.getMethod("canUpdate");
                boolean canUpdate = (boolean) canUpdateMethod.invoke(node);
                if (!canUpdate) continue;
                
                // 获取发射器维度
                Method getDimensionMethod = transmitterInterface.getMethod("getDimension");
                int transmitterDimension = (int) getDimensionMethod.invoke(node);
                
                boolean sameDimension = (transmitterDimension == playerDimension);
                
                // 如果同维度，让原始逻辑处理
                if (sameDimension) {
                    // 检查范围
                    Method getOriginMethod = transmitterInterface.getMethod("getOrigin");
                    Method getRangeMethod = transmitterInterface.getMethod("getRange");
                    
                    Object origin = getOriginMethod.invoke(node);
                    int range = (int) getRangeMethod.invoke(node);
                    
                    Method getXMethod = origin.getClass().getMethod("getX");
                    Method getYMethod = origin.getClass().getMethod("getY");
                    Method getZMethod = origin.getClass().getMethod("getZ");
                    
                    double x = (int) getXMethod.invoke(origin);
                    double y = (int) getYMethod.invoke(origin);
                    double z = (int) getZMethod.invoke(origin);
                    
                    double distance = Math.sqrt(
                        Math.pow(x - player.posX, 2) +
                        Math.pow(y - player.posY, 2) +
                        Math.pow(z - player.posZ, 2)
                    );
                    
                    // 同维度且在范围内，让原始逻辑处理
                    if (distance < range) {
                        return false;
                    }
                    continue;
                }
                
                // 不同维度 - 检查是否有维度卡
                boolean hasDimensionCard = false;
                boolean hasInfinityCard = false;
                try {
                    Field upgradesField = node.getClass().getDeclaredField("upgrades");
                    upgradesField.setAccessible(true);
                    Object upgrades = upgradesField.get(node);
                    hasDimensionCard = RSCardUtil.isDimensionCard(upgrades);
                    hasInfinityCard = RSCardUtil.isInfinityCard(upgrades);
                } catch (NoSuchFieldException e) {
                    try {
                        Method getUpgradesMethod = node.getClass().getMethod("getUpgrades");
                        Object upgrades = getUpgradesMethod.invoke(node);
                        hasDimensionCard = RSCardUtil.isDimensionCard(upgrades);
                        hasInfinityCard = RSCardUtil.isInfinityCard(upgrades);
                    } catch (Exception ignored) {}
                }
                
                // ⭐ 关键：跨维度需要维度卡
                if (hasDimensionCard) {
                    // ⭐ 跨维度时：如果有无限卡则无视距离，否则仍需检查范围
                    // 但是跨维度距离计算没有意义，所以有维度卡就允许
                    bestCrossDimensionNode = node;
                    System.out.println("[RSNetworkItemHook] Found cross-dimension transmitter with Dimension Card");
                    break; // 找到一个就够了
                }
            }
            
            // 如果找到跨维度的发射器，执行打开逻辑
            if (bestCrossDimensionNode != null) {
                return handleCrossDimensionOpen(handler, player, stack, slotId, network);
            }
            
            // 没找到任何可用发射器，让原始逻辑处理（会显示 out of range）
            return false;
            
        } catch (Exception e) {
            System.err.println("[RSNetworkItemHook] Error in onOpen: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 处理跨维度打开
     */
    @SuppressWarnings("unchecked")
    private static boolean handleCrossDimensionOpen(Object handler, EntityPlayer player, 
            ItemStack stack, int slotId, Object network) throws Exception {
        
        // 获取 INetworkItemProvider 并调用 provide
        Class<?> providerInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.item.INetworkItemProvider");
        Class<?> networkItemInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.item.INetworkItem");
        Class<?> handlerInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.item.INetworkItemHandler");
        Class<?> networkInterface = Class.forName("com.raoulvdberge.refinedstorage.api.network.INetwork");
        
        Object itemProvider = stack.getItem();
        if (!providerInterface.isInstance(itemProvider)) {
            return false;
        }
        
        // INetworkItem item = provider.provide(handler, player, stack, slotId)
        Method provideMethod = providerInterface.getMethod("provide", 
            handlerInterface, EntityPlayer.class, ItemStack.class, int.class);
        Object networkItem = provideMethod.invoke(itemProvider, handler, player, stack, slotId);
        
        // if (item.onOpen(network))
        Method onOpenMethod = networkItemInterface.getMethod("onOpen", networkInterface);
        boolean opened = (boolean) onOpenMethod.invoke(networkItem, network);
        
        if (opened) {
            // items.put(player, item)
            Map<EntityPlayer, Object> items = (Map<EntityPlayer, Object>) itemsField.get(handler);
            items.put(player, networkItem);
            System.out.println("[RSNetworkItemHook] Cross-dimension access granted for " + player.getName());
        }
        
        return true; // 已处理
    }
}
