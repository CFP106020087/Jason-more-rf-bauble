// ===== 增强的创造电池充电处理器 =====
// 文件位置: src/main/java/com/moremod/eventHandler/CreativeBatteryChargeHandler.java
// 描述: 以未知的科技，从虚空场中抽取能量

package com.moremod.event.eventHandler;

import com.moremod.item.ItemCreativeBatteryBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 创造电池充电处理器
 *
 * 这个神秘的装置利用未知的科技从虚空场中抽取无限的能量，
 * 能够为玩家身上的所有能量设备持续充电。
 *
 * 主要功能：
 * - 检测玩家背包和饰品栏中的创造电池
 * - 为所有位置的能量设备充电（背包、盔甲、饰品栏、副手）
 * - 智能的能量传输限制，避免游戏卡顿
 * - 客户端显示同步，确保能量条实时更新
 */
public class CreativeBatteryChargeHandler {

    // 虚空能量传输的神秘常数
    private static final int VOID_ENERGY_TRANSFER_RATE = 50000; // 每tick最多传输50k RF
    private static final int ENERGY_UPDATE_INTERVAL = 10; // 客户端更新间隔
    private static final int NOTIFICATION_INTERVAL = 6000; // 神秘消息间隔（5分钟）

    // 神秘的虚空能量消息
    private static final String[] VOID_ENERGY_MESSAGES = {
            TextFormatting.DARK_PURPLE + "虚空场的能量在你周围流淌...",
            TextFormatting.BLUE + "未知科技正在从维度间隙中抽取能量...",
            TextFormatting.AQUA + "神秘装置与虚空产生共鸣...",
            TextFormatting.GRAY + "你感受到了来自虚无的力量...",
            TextFormatting.LIGHT_PURPLE + "维度壁垒被穿透，无限能量涌入...",
            TextFormatting.DARK_AQUA + "虚空中的能量正在重新构建现实...",
            TextFormatting.GOLD + "古老的科技觉醒，连接着无尽的能量源..."
    };

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;

        // 检查玩家背包中是否有创造电池
        ItemStack creativeBattery = findCreativeBatteryInInventory(player);
        if (creativeBattery == null) {
            return;
        }

        // 获取创造电池的能量存储
        IEnergyStorage batteryStorage = creativeBattery.getCapability(CapabilityEnergy.ENERGY, null);
        if (batteryStorage == null) {
            return;
        }

        // 为所有位置的物品充电
        int totalChargedItems = chargeAllItems(player, batteryStorage);

        // 偶尔显示神秘的虚空能量消息
        if (totalChargedItems > 0 && player.ticksExisted % NOTIFICATION_INTERVAL == 0) {
            sendVoidEnergyMessage(player);
        }
    }

    /**
     * 在背包和饰品栏中寻找创造电池
     * 这个装置可能隐藏在任何地方，等待着被虚空能量激活
     */
    private ItemStack findCreativeBatteryInInventory(EntityPlayer player) {
        // 检查主背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                return stack;
            }
        }

        // 检查饰品栏中的创造电池
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                        return stack;
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理Baubles API异常 - 虚空能量有时会干扰现实
        }

        return null;
    }

    /**
     * 为所有位置的能量设备充电
     * 虚空能量无处不在，可以穿透任何障碍为设备充能
     */
    private int chargeAllItems(EntityPlayer player, IEnergyStorage batteryStorage) {
        int totalChargedItems = 0;

        // 1. 充电主背包中的物品
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && !(stack.getItem() instanceof ItemCreativeBatteryBauble)) {
                if (chargeItem(stack, batteryStorage)) {
                    totalChargedItems++;
                    // 标记背包需要同步到客户端
                    player.inventory.markDirty();
                }
            }
        }

        // 2. 充电盔甲槽中的物品
        for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
            ItemStack stack = player.inventory.armorInventory.get(i);
            if (!stack.isEmpty()) {
                if (chargeItem(stack, batteryStorage)) {
                    totalChargedItems++;
                    player.inventory.markDirty();
                }
            }
        }

        // 3. 充电副手物品
        ItemStack offhandStack = player.getHeldItemOffhand();
        if (!offhandStack.isEmpty() && !(offhandStack.getItem() instanceof ItemCreativeBatteryBauble)) {
            if (chargeItem(offhandStack, batteryStorage)) {
                totalChargedItems++;
                player.inventory.markDirty();
            }
        }

        // 4. 🌟 充电饰品栏中的物品（虚空能量的精华所在）
        totalChargedItems += chargeBaublesItems(player, batteryStorage);

        return totalChargedItems;
    }

    /**
     * 为饰品栏中的物品充电
     * 这是虚空能量最容易接触的区域，因为饰品更接近灵魂
     */
    private int chargeBaublesItems(EntityPlayer player, IEnergyStorage batteryStorage) {
        int chargedCount = 0;

        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                boolean needsSync = false;

                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && !(stack.getItem() instanceof ItemCreativeBatteryBauble)) {
                        if (chargeItem(stack, batteryStorage)) {
                            chargedCount++;
                            needsSync = true;

                            // 🔄 正确调用setChanged方法 - 让虚空知道变化已发生
                            baubles.setChanged(i, true);
                        }
                    }
                }

                // 🔄 如果有任何物品被充电，触发整体同步
                if (needsSync) {
                    syncBaublesInventory(player, baubles);
                }
            }
        } catch (Exception e) {
            // 如果虚空能量暂时不稳定，静默处理
            // System.out.println("虚空充能暂时中断: " + e.getMessage());
        }

        return chargedCount;
    }

    /**
     * 为单个物品充电
     * 从虚空场中抽取能量并注入到物品中
     */
    private boolean chargeItem(ItemStack stack, IEnergyStorage batteryStorage) {
        IEnergyStorage itemStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (itemStorage != null && itemStorage.canReceive()) {
            // 计算需要充电的量
            int maxReceive = itemStorage.getMaxEnergyStored() - itemStorage.getEnergyStored();
            if (maxReceive > 0) {
                // 虚空能量无限制传输，但限制每次传输量避免现实撕裂
                int transferAmount = Math.min(maxReceive, VOID_ENERGY_TRANSFER_RATE);

                // 从虚空电池提取能量（虚空总是慷慨地给予）
                int extracted = batteryStorage.extractEnergy(transferAmount, false);
                if (extracted > 0) {
                    // 为物品充电
                    int actualReceived = itemStorage.receiveEnergy(extracted, false);
                    return actualReceived > 0; // 返回是否实际充入了能量
                }
            }
        }
        return false;
    }

    /**
     * 同步饰品栏数据
     * 确保虚空能量的变化在所有维度中都能被感知到
     */
    private void syncBaublesInventory(EntityPlayer player, IBaublesItemHandler baubles) {
        try {
            // 方法1: 强制触发物品堆栈变化
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
                    if (storage != null) {
                        // 通过访问NBT来触发更新 - 在现实中留下虚空的印记
                        if (stack.hasTagCompound()) {
                            // 添加虚空时间戳，让现实认识到变化
                            stack.getTagCompound().setLong("VoidEnergyTimestamp", player.world.getTotalWorldTime());
                        }
                    }
                }
            }

            // 方法2: 向维度间发送同步信号
            if (player instanceof EntityPlayerMP) {
                // 可以在这里添加虚空能量网络包同步逻辑
                // VoidEnergyPacketHandler.sendBaublesEnergySync((EntityPlayerMP) player);
            }

        } catch (Exception e) {
            // 虚空有时会抗拒被理解
        }
    }

    /**
     * 发送神秘的虚空能量消息
     * 让玩家感受到来自未知维度的力量
     */
    private void sendVoidEnergyMessage(EntityPlayer player) {
        if (player.world.rand.nextFloat() < 0.3f) { // 30% 概率显示消息
            String message = VOID_ENERGY_MESSAGES[player.world.rand.nextInt(VOID_ENERGY_MESSAGES.length)];
            player.sendMessage(new TextComponentString(message));
        }
    }

    // 🔄 客户端显示更新 - 确保虚空能量的变化在视觉上可见
    @SubscribeEvent
    public void onPlayerTickClient(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.world.isRemote) {
            return;
        }

        // 客户端定期检查虚空能量的波动
        if (event.player.ticksExisted % ENERGY_UPDATE_INTERVAL == 0) {
            EntityPlayer player = event.player;

            // 检查是否有虚空电池在工作
            if (hasCreativeBattery(player)) {
                // 强制更新饰品栏显示，让能量条反映虚空的馈赠
                updateBaublesDisplay(player);
            }
        }
    }

    /**
     * 检查玩家是否拥有虚空电池
     * 感知虚空能量装置的存在
     */
    private boolean hasCreativeBattery(EntityPlayer player) {
        // 检查背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                return true;
            }
        }

        // 检查饰品栏
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 虚空有时会隐藏自己
        }

        return false;
    }

    /**
     * 更新饰品栏显示
     * 让现实界面反映虚空能量的流动
     */
    private void updateBaublesDisplay(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                // 触发客户端显示更新，让虚空的力量可见
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (storage != null && storage.canReceive()) {
                            // 通过模拟访问来刷新客户端缓存 - 窥视虚空的恩赐
                            storage.getEnergyStored();
                            storage.getMaxEnergyStored();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 有时虚空不愿意被观察
        }
    }
}