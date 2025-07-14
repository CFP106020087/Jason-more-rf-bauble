package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemBatteryBauble;
import com.moremod.item.ItemCreativeBatteryBauble;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class BatteryChargeHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack battery = findBattery(player);
        if (battery.isEmpty()) return;

        IEnergyStorage batteryStorage = ItemBatteryBauble.getEnergyStorage(battery);
        if (batteryStorage == null || batteryStorage.getEnergyStored() <= 0) return;

        // 充电所有位置的物品，并跟踪是否有更新
        boolean inventoryChanged = false;
        boolean baublesChanged = false;

        // 1. 充电主背包物品
        for (ItemStack target : player.inventory.mainInventory) {
            if (shouldChargeItem(target, battery)) {
                if (chargeItem(target, batteryStorage)) {
                    inventoryChanged = true;
                }
            }
        }

        // 2. 充电盔甲槽物品
        for (ItemStack target : player.inventory.armorInventory) {
            if (shouldChargeItem(target, battery)) {
                if (chargeItem(target, batteryStorage)) {
                    inventoryChanged = true;
                }
            }
        }

        // 3. 充电副手物品
        for (ItemStack target : player.inventory.offHandInventory) {
            if (shouldChargeItem(target, battery)) {
                if (chargeItem(target, batteryStorage)) {
                    inventoryChanged = true;
                }
            }
        }

        // 4. 🌟 充电饰品栏物品 - 支持实时更新
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack target = baubles.getStackInSlot(i);
                    if (shouldChargeItem(target, battery)) {
                        if (chargeItem(target, batteryStorage)) {
                            baublesChanged = true;
                            // 🔄 单个槽位实时同步
                            baubles.setChanged(i, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理Baubles API异常
        }

        // 🔄 批量同步更新
        if (inventoryChanged) {
            player.inventory.markDirty();
        }

        if (baublesChanged) {
            syncBaublesInventory(player);
        }
    }

    // 检查物品是否应该被充电
    private static boolean shouldChargeItem(ItemStack target, ItemStack battery) {
        return !target.isEmpty() &&
                target != battery && // 排除电池自身
                !(target.getItem() instanceof ItemBatteryBauble) && // 排除普通电池
                !(target.getItem() instanceof ItemCreativeBatteryBauble); // 排除创造电池
    }

    // 为单个物品充电
    private static boolean chargeItem(ItemStack target, IEnergyStorage batteryStorage) {
        IEnergyStorage targetStorage = target.getCapability(CapabilityEnergy.ENERGY, null);
        if (targetStorage != null && targetStorage.canReceive()) {
            // 计算传输量 - 普通电池有传输限制
            int maxTransfer = Math.min(ItemBatteryBauble.MAX_TRANSFER, batteryStorage.getEnergyStored());
            int maxReceive = targetStorage.getMaxEnergyStored() - targetStorage.getEnergyStored();
            int toTransfer = Math.min(maxTransfer, maxReceive);

            if (toTransfer > 0) {
                // 限制每次传输量，避免过快耗尽电池
                int actualTransfer = Math.min(toTransfer, 5000); // 每tick最多5k RF

                int accepted = targetStorage.receiveEnergy(actualTransfer, false);
                if (accepted > 0) {
                    batteryStorage.extractEnergy(accepted, false);
                    return true; // 返回true表示发生了充电
                }
            }
        }
        return false; // 没有充电
    }

    // 找到背包中的电池
    private static ItemStack findBattery(EntityPlayer player) {
        // 优先查找普通电池
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBatteryBauble) {
                return stack;
            }
        }

        // 如果没有普通电池，也检查饰品栏
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemBatteryBauble) {
                        return stack;
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }

        return ItemStack.EMPTY;
    }

    // 🔄 饰品栏同步方法
    private static void syncBaublesInventory(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                // 强制触发物品堆栈变化
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (storage != null) {
                            // 通过访问NBT来触发更新
                            if (stack.hasTagCompound()) {
                                // 添加时间戳，强制客户端识别变化
                                stack.getTagCompound().setLong("LastEnergyUpdate", player.world.getTotalWorldTime());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    // 🔄 客户端显示更新 - 确保能量条实时刷新
    @SubscribeEvent
    public static void onPlayerTickClient(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.world.isRemote) {
            return;
        }

        // 客户端每15tick检查一次是否需要更新显示（比创造电池稍慢一些）
        if (event.player.ticksExisted % 15 == 0) {
            EntityPlayer player = event.player;

            // 检查是否有电池
            if (hasBattery(player)) {
                // 强制更新饰品栏显示
                updateBaublesDisplay(player);
            }
        }
    }

    private static boolean hasBattery(EntityPlayer player) {
        // 检查背包中的普通电池
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBatteryBauble) {
                return true;
            }
        }

        // 检查饰品栏中的普通电池
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemBatteryBauble) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理
        }

        return false;
    }

    private static void updateBaublesDisplay(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                // 触发客户端显示更新
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (storage != null && storage.canReceive()) {
                            // 通过模拟访问来刷新客户端缓存
                            storage.getEnergyStored();
                            storage.getMaxEnergyStored();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理
        }
    }
}