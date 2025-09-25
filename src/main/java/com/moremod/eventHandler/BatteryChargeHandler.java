package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemBatteryBauble;
import com.moremod.item.ItemCreativeBatteryBauble;
import com.moremod.item.battery.ItemBatteryBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 电池/核心 充放电&被动效果总线
 *
 * 修复版本：
 * - 电池必须有电才能给其他物品充电
 * - 创造电池除外（无限能量）
 * - 避免与 ItemMechanicalCore 的充电逻辑冲突
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BatteryChargeHandler {

    private static final boolean DEBUG = false;
    private static final int BASE_TRANSFER_RATE = 5000;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        final EntityPlayer player = event.player;

        // 减少频率：每 2t 执行一次
        if (player.world.getTotalWorldTime() % 2 != 0) return;

        // 1) 找到装备的核心和佩戴的电池
        ItemStack coreStack = findMechanicalCore(player);
        ItemStack wornBattery = findWornBattery(player);

        // 2) 更新核心的电池效率系数（佩戴就有效，不需要有电）
        updateCoreEfficiencyFromBattery(coreStack, wornBattery);

        // 3) 找到最佳供电电池（必须有电或是创造电池）
        ItemStack primaryBattery = findBestChargedBattery(player);
        if (primaryBattery.isEmpty()) {
            // 没有可用的带电电池
            return;
        }

        // 检查是否是创造电池
        boolean isCreativeBattery = primaryBattery.getItem() instanceof ItemCreativeBatteryBauble;

        IEnergyStorage batteryStorage = primaryBattery.getCapability(CapabilityEnergy.ENERGY, null);
        if (batteryStorage == null && !isCreativeBattery) return;

        // 创造电池特殊处理
        if (isCreativeBattery) {
            handleCreativeBattery(player, coreStack, primaryBattery);
            return;
        }

        // 普通电池必须有电
        if (batteryStorage.getEnergyStored() <= 0) {
            return;
        }

        boolean inventoryChanged = false;
        boolean baublesChanged = false;
        int totalTransferred = 0;

        // 4) 注意：不给机械核心充电，让 ItemMechanicalCore 自己处理
        // 这样避免重复充电的问题

        // 5) 给其它 FE 物品充电
        int chargeRate = getChargeRate(primaryBattery);

        // 主背包
        for (ItemStack target : player.inventory.mainInventory) {
            if (shouldChargeItem(target, primaryBattery, coreStack)) {
                int transferred = chargeItem(target, batteryStorage, chargeRate);
                if (transferred > 0) {
                    inventoryChanged = true;
                    totalTransferred += transferred;
                }
            }
        }

        // 盔甲槽
        for (ItemStack target : player.inventory.armorInventory) {
            if (shouldChargeItem(target, primaryBattery, coreStack)) {
                int transferred = chargeItem(target, batteryStorage, chargeRate);
                if (transferred > 0) {
                    inventoryChanged = true;
                    totalTransferred += transferred;
                }
            }
        }

        // 副手
        ItemStack offhand = player.getHeldItemOffhand();
        if (shouldChargeItem(offhand, primaryBattery, coreStack)) {
            int transferred = chargeItem(offhand, batteryStorage, chargeRate);
            if (transferred > 0) {
                inventoryChanged = true;
                totalTransferred += transferred;
            }
        }

        // 饰品
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack target = baubles.getStackInSlot(i);
                    if (shouldChargeItem(target, primaryBattery, coreStack)) {
                        int transferred = chargeItem(target, batteryStorage, chargeRate);
                        if (transferred > 0) {
                            baublesChanged = true;
                            baubles.setChanged(i, true);
                            totalTransferred += transferred;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 6) 特殊功能（需要佩戴电池且有电）
        if (!wornBattery.isEmpty() && wornBattery.getItem() instanceof ItemBatteryBase) {
            IEnergyStorage wornEnergy = wornBattery.getCapability(CapabilityEnergy.ENERGY, null);
            boolean hasEnergy = (wornEnergy != null && wornEnergy.getEnergyStored() > 0) ||
                    (wornBattery.getItem() instanceof ItemCreativeBatteryBauble);

            if (hasEnergy) {
                ItemBatteryBase bat = (ItemBatteryBase) wornBattery.getItem();
                int tier = bat.getTier();

                // 精英及以上：无线充电其他电池
                if (tier >= 3 && batteryStorage.getEnergyStored() > 10000) {
                    wirelessChargeOtherBatteries(player, batteryStorage, primaryBattery);
                }

                // 终极及以上：环境能量收集
                if (tier >= 4) {
                    collectEnvironmentalEnergy(player, batteryStorage);
                }

                // 量子：量子链路
                if (tier >= 5) {
                    quantumEnergyLink(player, primaryBattery, batteryStorage);
                }
            }
        }

        // 7) 被动加成（需要佩戴且有电）
        if (!wornBattery.isEmpty() && player.world.getTotalWorldTime() % 40 == 0) {
            IEnergyStorage wornEnergy = wornBattery.getCapability(CapabilityEnergy.ENERGY, null);
            boolean hasEnergy = (wornEnergy != null && wornEnergy.getEnergyStored() > 0) ||
                    (wornBattery.getItem() instanceof ItemCreativeBatteryBauble);

            if (hasEnergy && !coreStack.isEmpty()) {
                applyBatteryBonusEffects(coreStack, wornBattery, player);
            }
        }

        // 8) 同步
        if (inventoryChanged) player.inventory.markDirty();
        if (baublesChanged) syncBaublesInventory(player);

        if (DEBUG && totalTransferred > 0) {
            System.out.println(String.format("[BatteryHandler] 总传输: %d RF", totalTransferred));
        }
    }

    /**
     * 处理创造电池的特殊逻辑
     */
    private static void handleCreativeBattery(EntityPlayer player, ItemStack coreStack, ItemStack battery) {
        // 创造电池给其他物品充电（不包括机械核心，让核心自己处理）
        int creativeRate = 100000; // 创造电池超高速率

        // 充电其他物品
        for (ItemStack target : player.inventory.mainInventory) {
            if (shouldChargeItem(target, battery, coreStack)) {
                IEnergyStorage targetEnergy = target.getCapability(CapabilityEnergy.ENERGY, null);
                if (targetEnergy != null && targetEnergy.canReceive()) {
                    targetEnergy.receiveEnergy(creativeRate, false);
                }
            }
        }

        // 充电盔甲
        for (ItemStack target : player.inventory.armorInventory) {
            if (shouldChargeItem(target, battery, coreStack)) {
                IEnergyStorage targetEnergy = target.getCapability(CapabilityEnergy.ENERGY, null);
                if (targetEnergy != null && targetEnergy.canReceive()) {
                    targetEnergy.receiveEnergy(creativeRate, false);
                }
            }
        }
    }

    /**
     * 找到最佳的带电电池（必须有电或是创造电池）
     */
    private static ItemStack findBestChargedBattery(EntityPlayer player) {
        ItemStack bestBattery = ItemStack.EMPTY;
        int highestTier = 0;
        int highestCharge = 0;

        // 优先找创造电池
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                return stack;
            }
        }

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
        } catch (Exception ignored) {}

        // 找有电的普通电池
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ItemBatteryBase) {
                IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (energy != null && energy.getEnergyStored() > 0) {
                    int tier = ((ItemBatteryBase) stack.getItem()).getTier();
                    if (tier > highestTier || (tier == highestTier && energy.getEnergyStored() > highestCharge)) {
                        highestTier = tier;
                        highestCharge = energy.getEnergyStored();
                        bestBattery = stack;
                    }
                }
            } else if (stack.getItem() instanceof ItemBatteryBauble && highestTier == 0) {
                IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (energy != null && energy.getEnergyStored() > 0) {
                    bestBattery = stack;
                    highestCharge = energy.getEnergyStored();
                }
            }
        }

        // 检查饰品栏
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (stack.isEmpty()) continue;

                    if (stack.getItem() instanceof ItemBatteryBase) {
                        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (energy != null && energy.getEnergyStored() > 0) {
                            int tier = ((ItemBatteryBase) stack.getItem()).getTier();
                            if (tier > highestTier || (tier == highestTier && energy.getEnergyStored() > highestCharge)) {
                                highestTier = tier;
                                highestCharge = energy.getEnergyStored();
                                bestBattery = stack;
                            }
                        }
                    } else if (stack.getItem() instanceof ItemBatteryBauble && highestTier == 0) {
                        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (energy != null && energy.getEnergyStored() > 0) {
                            bestBattery = stack;
                            highestCharge = energy.getEnergyStored();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return bestBattery;
    }

    /**
     * 给物品充电，返回实际传输的能量
     */
    private static int chargeItem(ItemStack target, IEnergyStorage batteryStorage, int maxTransfer) {
        if (target.isEmpty() || batteryStorage.getEnergyStored() <= 0) return 0;

        IEnergyStorage targetStorage = target.getCapability(CapabilityEnergy.ENERGY, null);
        if (targetStorage == null || !targetStorage.canReceive()) return 0;

        // 计算可传输量
        int available = batteryStorage.getEnergyStored();
        int needed = targetStorage.getMaxEnergyStored() - targetStorage.getEnergyStored();
        int toTransfer = Math.min(Math.min(maxTransfer, available), needed);

        if (toTransfer > 0) {
            // 先模拟提取
            int canExtract = batteryStorage.extractEnergy(toTransfer, true);
            if (canExtract > 0) {
                // 尝试给目标充电
                int accepted = targetStorage.receiveEnergy(canExtract, false);
                if (accepted > 0) {
                    // 实际从电池提取
                    batteryStorage.extractEnergy(accepted, false);
                    return accepted;
                }
            }
        }
        return 0;
    }

    // ===== 辅助方法保持不变 =====

    private static ItemStack findWornBattery(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                ItemStack best = ItemStack.EMPTY;
                int highest = 0;
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack s = baubles.getStackInSlot(i);
                    if (s.isEmpty()) continue;

                    if (s.getItem() instanceof ItemCreativeBatteryBauble) {
                        return s; // 创造电池优先
                    } else if (s.getItem() instanceof ItemBatteryBase) {
                        int t = ((ItemBatteryBase) s.getItem()).getTier();
                        if (t > highest) {
                            highest = t;
                            best = s;
                        }
                    } else if (s.getItem() instanceof ItemBatteryBauble && highest == 0) {
                        best = s;
                        highest = 1;
                    }
                }
                return best;
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    private static void updateCoreEfficiencyFromBattery(ItemStack coreStack, ItemStack wornBattery) {
        if (coreStack.isEmpty()) return;
        if (!coreStack.hasTagCompound()) coreStack.setTagCompound(new NBTTagCompound());
        NBTTagCompound nbt = coreStack.getTagCompound();

        float eff = 1.0f;
        if (!wornBattery.isEmpty()) {
            if (wornBattery.getItem() instanceof ItemCreativeBatteryBauble) {
                eff = 0.5f; // 创造电池50%省电
            } else if (wornBattery.getItem() instanceof ItemBatteryBase) {
                int tier = ((ItemBatteryBase) wornBattery.getItem()).getTier();
                switch (tier) {
                    case 2: eff = 0.90f; break;
                    case 3: eff = 0.80f; break;
                    case 4: eff = 0.70f; break;
                    case 5: eff = 0.50f; break;
                    default: eff = 1.0f;
                }
            }
        }
        nbt.setFloat("BatteryEfficiency", eff);
    }

    private static int getChargeRate(ItemStack battery) {
        if (battery.getItem() instanceof ItemCreativeBatteryBauble) {
            return 100000; // 创造电池超高速率
        }
        if (battery.getItem() instanceof ItemBatteryBase) {
            switch (((ItemBatteryBase) battery.getItem()).getTier()) {
                case 1: return 1000;
                case 2: return 5000;
                case 3: return 10000;
                case 4: return 50000;
                case 5: return 100000;
            }
        }
        return BASE_TRANSFER_RATE;
    }

    private static void applyBatteryBonusEffects(ItemStack coreStack, ItemStack wornBattery, EntityPlayer player) {
        if (wornBattery.getItem() instanceof ItemCreativeBatteryBauble) {
            // 创造电池最强效果
            player.addExhaustion(-1.0f);
            player.heal(0.5f);
            IEnergyStorage coreEnergy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
            if (coreEnergy != null) coreEnergy.receiveEnergy(5000, false);
            return;
        }

        if (!(wornBattery.getItem() instanceof ItemBatteryBase)) return;

        int tier = ((ItemBatteryBase) wornBattery.getItem()).getTier();
        switch (tier) {
            case 3:
                player.addExhaustion(-0.1f);
                break;
            case 4:
                player.addExhaustion(-0.2f);
                player.heal(0.1f);
                break;
            case 5:
                player.addExhaustion(-0.5f);
                player.heal(0.25f);
                IEnergyStorage coreEnergy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
                if (coreEnergy != null) coreEnergy.receiveEnergy(1000, false);
                break;
        }
    }

    private static void wirelessChargeOtherBatteries(EntityPlayer player, IEnergyStorage source, ItemStack sourceBattery) {
        if (source.getEnergyStored() < 10000) return;

        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == sourceBattery) continue;
            if ((stack.getItem() instanceof ItemBatteryBauble) || (stack.getItem() instanceof ItemBatteryBase)) {
                IEnergyStorage target = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (target != null && target.canReceive()) {
                    int probe = Math.min(1000, source.extractEnergy(1000, true));
                    if (probe > 0) {
                        int received = target.receiveEnergy(probe, false);
                        source.extractEnergy(received, false);
                    }
                }
            }
        }
    }

    private static void collectEnvironmentalEnergy(EntityPlayer player, IEnergyStorage battery) {
        int bonus = 0;
        if (player.world.isDaytime() && player.world.canSeeSky(player.getPosition())) bonus += 100;
        if (player.dimension == -1) bonus += 200;
        if (player.dimension == 1) bonus += 150;
        if (bonus > 0) battery.receiveEnergy(bonus, false);
    }

    private static void quantumEnergyLink(EntityPlayer player, ItemStack batteryStack, IEnergyStorage battery) {
        if (player.world.getTotalWorldTime() % 100 == 0) {
            battery.receiveEnergy(50000, false);
        }
        battery.receiveEnergy(100, false);
    }

    private static ItemStack findMechanicalCore(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack s = baubles.getStackInSlot(i);
                    if (!s.isEmpty() && s.getItem() instanceof ItemMechanicalCore) {
                        return s;
                    }
                }
            }
        } catch (Exception ignored) {}

        for (ItemStack s : player.inventory.mainInventory) {
            if (!s.isEmpty() && s.getItem() instanceof ItemMechanicalCore) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean shouldChargeItem(ItemStack target, ItemStack sourceBattery, ItemStack core) {
        return !target.isEmpty()
                && target != sourceBattery
                && target != core
                && !(target.getItem() instanceof ItemBatteryBauble)
                && !(target.getItem() instanceof ItemBatteryBase)
                && !(target.getItem() instanceof ItemCreativeBatteryBauble)
                && !(target.getItem() instanceof ItemMechanicalCore); // 不给核心充电
    }

    private static void syncBaublesInventory(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.hasTagCompound()) {
                        stack.getTagCompound().setLong("LastEnergyUpdate", player.world.getTotalWorldTime());
                        baubles.setChanged(i, true);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}