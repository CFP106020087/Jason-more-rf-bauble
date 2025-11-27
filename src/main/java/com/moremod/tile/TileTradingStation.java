// ============================================
// 文件路徑: src/main/java/com/moremod/tile/TileTradingStation.java
// 說明: 村民交易機TileEntity - 核心邏輯處理
// ✅ 修复: 支持双输入槽位,正确处理需要两个物品的交易
// ============================================

package com.moremod.tile;

import com.moremod.item.ItemVillagerCapsule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * 🏪 村民交易機 TileEntity
 *
 * 功能：
 * - 存儲村民數據和交易列表
 * - 自動化執行交易
 * - 管理能量消耗
 * - 提供物品和能量接口
 * 
 * ✅ 修复内容:
 * - 增加第二个输入槽位,支持双物品交易
 * - 修正交易逻辑,正确处理两个不同的输入物品
 */
public class TileTradingStation extends TileEntity implements ITickable {

    // ========== 常量配置 ==========
    private static final int MAX_ENERGY = 100000;        // 最大能量 100,000 RF
    private static final int ENERGY_PER_TRADE = 10000;   // 每次交易消耗 10,000 RF
    private static final int ENERGY_RECEIVE_RATE = 100;  // 每tick接收能量上限
    private static final int AUTO_TRADE_INTERVAL = 100; // 自動交易間隔 (5秒)

    // ========== 物品處理器 ==========
    // ✅ 槽位定義 (修改为4个槽位):
    // 0 = 村民膠囊槽
    // 1 = 輸入物品槽1 (第一个交易物品)
    // 2 = 輸入物品槽2 (第二个交易物品,可选)
    // 3 = 輸出物品槽 (交易結果)
    public final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();

            // 當村民膠囊槽變化時,嘗試加載村民數據
            if (slot == 0) {
                loadVillagerFromCapsule();
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // ✅ 輸出槽(3)不允許插入
            if (slot == 3) {
                return stack;
            }

            // 村民槽(0)只允許放入包含村民的膠囊
            if (slot == 0 && !stack.isEmpty()) {
                if (!(stack.getItem() instanceof ItemVillagerCapsule)) {
                    return stack; // 不是村民膠囊,拒絕
                }
                if (!ItemVillagerCapsule.hasVillager(stack)) {
                    return stack; // 空膠囊,拒絕
                }
            }

            return super.insertItem(slot, stack, simulate);
        }
    };

    // ========== 能量存儲 ==========
    private final EnergyStorage energyStorage = new EnergyStorage(
            MAX_ENERGY,           // 最大容量
            ENERGY_RECEIVE_RATE,  // 最大接收速率
            0                     // 最大輸出速率(不輸出)
    );

    // ========== 村民數據 ==========
    private NBTTagCompound villagerData = null;  // 村民完整NBT數據
    private int currentTradeIndex = 0;           // 當前選中的交易索引
    private int workTimer = 0;                   // 自動交易計時器

    // ========== Getters 公開方法 ==========

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public boolean hasVillager() {
        return villagerData != null;
    }

    public int getCurrentTradeIndex() {
        return currentTradeIndex;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    // ========== ITickable 實現 ==========

    /**
     * 每tick執行(每秒20次)
     * 用於自動交易功能
     */
    @Override
    public void update() {
        // 只在服務端執行
        if (world == null || world.isRemote) {
            return;
        }

        // 自動交易定時器
        workTimer++;
        if (workTimer >= AUTO_TRADE_INTERVAL) {
            workTimer = 0;

            // 嘗試執行自動交易
            if (canTrade()) {
                executeTrade();
                System.out.println("[TileTradingStation] ⚡ 自動交易執行成功");
            }
        }
    }

    // ========== 村民管理方法 ==========

    /**
     * 從村民膠囊加載村民數據
     * 當槽位0的物品變化時自動調用
     */
    private void loadVillagerFromCapsule() {
        ItemStack capsule = itemHandler.getStackInSlot(0);

        if (capsule.isEmpty()) {
            // 膠囊被移除,清空村民數據
            if (villagerData != null) {
                villagerData = null;
                currentTradeIndex = 0;
                syncToClient(); // 同步到客戶端
                System.out.println("[TileTradingStation] 村民膠囊已移除,數據已清空");
            }
            return;
        }

        // 檢查是否是村民膠囊
        if (!(capsule.getItem() instanceof ItemVillagerCapsule)) {
            return;
        }

        // 讀取村民數據
        NBTTagCompound newData = ItemVillagerCapsule.getVillagerData(capsule);

        if (newData != null) {
            villagerData = newData;
            currentTradeIndex = 0;
            syncToClient(); // 同步到客戶端
            System.out.println("[TileTradingStation] ✅ 村民數據已加載");
        }
    }

    /**
     * 設置村民(用於程序直接設置,而非通過膠囊)
     * @param villager 村民實體
     */
    public void setVillager(EntityVillager villager) {
        if (villager != null) {
            this.villagerData = new NBTTagCompound();
            villager.writeToNBT(villagerData);
            this.currentTradeIndex = 0;
            markDirty();
            syncToClient();
            System.out.println("[TileTradingStation] 村民已直接設置");
        }
    }

    /**
     * 從NBT創建臨時村民實體
     * 用於讀取交易列表和執行交易驗證
     *
     * @return 村民實體,失敗返回null
     */
    @Nullable
    public EntityVillager createVillagerFromNBT() {
        if (villagerData == null || world == null) {
            return null;
        }

        try {
            EntityVillager villager = new EntityVillager(world);
            villager.readFromNBT(villagerData);
            return villager;
        } catch (Exception e) {
            System.err.println("[TileTradingStation] ❌ 創建村民失敗: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ========== 交易管理方法 ==========

    /**
     * 切換到下一個交易
     * 由GUI按鈕觸發
     */
    public void nextTrade() {
        if (villagerData == null) {
            System.out.println("[TileTradingStation] ⚠️ 沒有村民數據");
            return;
        }

        EntityVillager tempVillager = createVillagerFromNBT();
        if (tempVillager == null) {
            System.out.println("[TileTradingStation] ⚠️ 無法創建村民實例");
            return;
        }

        MerchantRecipeList recipes = tempVillager.getRecipes(null);
        if (recipes != null && !recipes.isEmpty()) {
            currentTradeIndex = (currentTradeIndex + 1) % recipes.size();
            markDirty();
            syncToClient(); // 🎯 同步到客戶端
            System.out.println("[TileTradingStation] ➡️ 切換到交易 " +
                    (currentTradeIndex + 1) + "/" + recipes.size());
        } else {
            System.out.println("[TileTradingStation] ⚠️ 村民沒有交易");
        }
    }

    /**
     * 切換到上一個交易
     * 由GUI按鈕觸發
     */
    public void previousTrade() {
        if (villagerData == null) {
            System.out.println("[TileTradingStation] ⚠️ 沒有村民數據");
            return;
        }

        EntityVillager tempVillager = createVillagerFromNBT();
        if (tempVillager == null) {
            System.out.println("[TileTradingStation] ⚠️ 無法創建村民實例");
            return;
        }

        MerchantRecipeList recipes = tempVillager.getRecipes(null);
        if (recipes != null && !recipes.isEmpty()) {
            currentTradeIndex = (currentTradeIndex - 1 + recipes.size()) % recipes.size();
            markDirty();
            syncToClient(); // 🎯 同步到客戶端
            System.out.println("[TileTradingStation] ⬅️ 切換到交易 " +
                    (currentTradeIndex + 1) + "/" + recipes.size());
        } else {
            System.out.println("[TileTradingStation] ⚠️ 村民沒有交易");
        }
    }

    /**
     * ✅ 修复: 執行當前選中的交易 - 支持双输入槽位
     * 由自動交易觸發
     */
    public void executeTrade() {
        if (!canTrade()) {
            return;
        }

        EntityVillager tempVillager = createVillagerFromNBT();
        if (tempVillager == null) return;

        MerchantRecipeList recipes = tempVillager.getRecipes(null);
        MerchantRecipe recipe = recipes.get(currentTradeIndex);

        // ✅ 扣除第一个输入物品 (槽位1)
        ItemStack input1 = itemHandler.getStackInSlot(1);
        ItemStack required1 = recipe.getItemToBuy();
        input1.shrink(required1.getCount());

        // ✅ 如果需要第二個物品,从槽位2扣除
        ItemStack required2 = recipe.getSecondItemToBuy();
        if (!required2.isEmpty()) {
            ItemStack input2 = itemHandler.getStackInSlot(2);
            input2.shrink(required2.getCount());
        }

        // ✅ 增加输出物品到槽位3
        ItemStack outputSlot = itemHandler.getStackInSlot(3);
        ItemStack result = recipe.getItemToSell().copy();

        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(3, result);
        } else {
            outputSlot.grow(result.getCount());
        }

        // 扣除能量
        energyStorage.extractEnergy(ENERGY_PER_TRADE, false);

        markDirty();
        syncToClient();

        System.out.println("[TileTradingStation] ✅ 交易執行成功!");
    }

    /**
     * ✅ 修复: 检查是否可以执行交易 - 支持双输入槽位
     */
    private boolean canTrade() {
        // 基础检查
        if (villagerData == null) return false;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TRADE) return false;

        EntityVillager tempVillager = createVillagerFromNBT();
        if (tempVillager == null) return false;

        MerchantRecipeList recipes = tempVillager.getRecipes(null);
        if (recipes == null || recipes.isEmpty() || currentTradeIndex >= recipes.size()) {
            return false;
        }

        MerchantRecipe recipe = recipes.get(currentTradeIndex);
        if (recipe.isRecipeDisabled()) return false;

        // ✅ 检查第一个输入物品 (槽位1)
        ItemStack input1 = itemHandler.getStackInSlot(1);
        ItemStack required1 = recipe.getItemToBuy();
        
        if (input1.isEmpty() || 
            !ItemStack.areItemsEqual(input1, required1) || 
            input1.getCount() < required1.getCount()) {
            return false;
        }

        // ✅ 检查第二个输入物品 (槽位2,如果需要)
        ItemStack required2 = recipe.getSecondItemToBuy();
        if (!required2.isEmpty()) {
            ItemStack input2 = itemHandler.getStackInSlot(2);
            
            if (input2.isEmpty() || 
                !ItemStack.areItemsEqual(input2, required2) || 
                input2.getCount() < required2.getCount()) {
                return false;
            }
        }

        // ✅ 检查输出槽位3是否有空间
        ItemStack outputSlot = itemHandler.getStackInSlot(3);
        ItemStack result = recipe.getItemToSell();

        return outputSlot.isEmpty() ||
                (ItemStack.areItemsEqual(outputSlot, result) &&
                        outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize());
    }

    // ========== 客戶端同步支持 ==========

    /**
     * 同步數據到客戶端
     */
    private void syncToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    /**
     * 獲取同步數據包(服務端 -> 客戶端)
     */
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    /**
     * 獲取要同步的NBT數據
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound nbt = super.getUpdateTag();
        nbt.setInteger("TradeIndex", currentTradeIndex);
        nbt.setInteger("Energy", energyStorage.getEnergyStored());

        if (villagerData != null) {
            nbt.setTag("Villager", villagerData);
        }

        return nbt;
    }

    /**
     * 處理同步數據包(客戶端接收)
     */
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.getNbtCompound();

        currentTradeIndex = nbt.getInteger("TradeIndex");

        int energy = nbt.getInteger("Energy");
        energyStorage.receiveEnergy(energy - energyStorage.getEnergyStored(), false);

        if (nbt.hasKey("Villager")) {
            villagerData = nbt.getCompoundTag("Villager");
        } else {
            villagerData = null;
        }

        System.out.println("[TileTradingStation CLIENT] 收到同步數據,交易索引: " + currentTradeIndex);
    }

    // ========== NBT 序列化 ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setTag("Inventory", itemHandler.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("TradeIndex", currentTradeIndex);
        compound.setInteger("WorkTimer", workTimer);

        if (villagerData != null) {
            compound.setTag("Villager", villagerData);
        }

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        itemHandler.deserializeNBT(compound.getCompoundTag("Inventory"));

        int storedEnergy = compound.getInteger("Energy");
        energyStorage.receiveEnergy(storedEnergy, false);

        currentTradeIndex = compound.getInteger("TradeIndex");
        workTimer = compound.getInteger("WorkTimer");

        if (compound.hasKey("Villager")) {
            villagerData = compound.getCompoundTag("Villager");
        }
    }

    // ========== Capability 支持 ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
                capability == CapabilityEnergy.ENERGY ||
                super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return super.getCapability(capability, facing);
    }
}
