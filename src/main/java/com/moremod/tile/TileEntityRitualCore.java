package com.moremod.tile;

import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileEntityRitualCore extends TileEntity implements ITickable {

    // --- 庫存系統 (核心輸入 + 成品輸出) ---
    private final ItemStackHandler inv = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            // 當物品變更時，立即同步給客戶端 (為了 Renderer 能馬上看到物品變化)
            if (world != null && !world.isRemote) {
                syncToClient();
                // 如果拿走了輸入物品，儀式必須強制中斷
                if (slot == 0 && getStackInSlot(0).isEmpty() && isActive) {
                    reset();
                }
            }
        }
    };

    // --- 運行狀態變數 ---
    private RitualInfusionRecipe activeRecipe;
    private int process = 0;
    private boolean isActive = false;
    private boolean hasEnoughEnergy = false;

    // 用於客戶端平滑渲染的緩存變量
    public float clientRotation = 0;
    public float lastClientRotation = 0;

    // 8個基座位置定義
    private static final BlockPos[] OFFS8 = new BlockPos[]{
            new BlockPos( 3, 0,  0), new BlockPos(-3, 0,  0),
            new BlockPos( 0, 0,  3), new BlockPos( 0, 0, -3),
            new BlockPos( 2, 0,  2), new BlockPos( 2, 0, -2),
            new BlockPos(-2, 0,  2), new BlockPos(-2, 0, -2)
    };

    // --- 核心邏輯循環 ---
    @Override
    public void update() {
        if (world == null) return;

        // 客戶端邏輯：只處理粒子和渲染動畫數據
        if (world.isRemote) {
            updateClientVisuals();
            return;
        }

        // 服務端邏輯

        // 1. 查找有效基座 (必須有物品)
        List<TileEntityPedestal> peds = findValidPedestals();
        if (peds.isEmpty()) {
            if (isActive) reset();
            return;
        }

        // 2. 配方匹配邏輯
        // 如果當前沒有激活配方，或輸入物品變了，嘗試尋找新配方
        if (activeRecipe == null || !activeRecipe.getCore().apply(inv.getStackInSlot(0))) {
            activeRecipe = findMatchingRecipe(peds);
            process = 0; // 新配方，進度歸零
        }

        // 如果還是找不到配方，歸位
        if (activeRecipe == null) {
            updateState(false, false);
            return;
        }

        // 3. 運行中完整性檢查 (二次確認)
        // 確保基座數量足夠，且中途沒有被玩家偷走基座上的物品
        if (peds.size() < activeRecipe.getPedestalCount() || !isValidRitualStructure(peds, activeRecipe)) {
            // 為了容錯，如果只是暫時缺電可以不重置，但如果物品沒了就必須重置
            reset();
            return;
        }

        // 4. 能量處理
        int time = Math.max(1, activeRecipe.getTime());
        int energyPerTick = Math.max(1, activeRecipe.getEnergyPerPedestal() / time);

        boolean currentTickEnergy = checkAndConsumeEnergy(peds, activeRecipe, energyPerTick, true); // true = 模擬檢查

        if (!currentTickEnergy) {
            // 能量不足，暫停進度，但不重置配方 (給玩家時間去充電)
            updateState(true, false);
            return;
        }

        // 5. 推進儀式
        // 實際消耗能量
        checkAndConsumeEnergy(peds, activeRecipe, energyPerTick, false);

        process++;
        updateState(true, true); // 狀態：激活且有能量

        // 每秒產生一次音效或服務端粒子包
        if (process % 20 == 0) {
            // 這裡可以發包給客戶端播放特定音效
        }

        // 6. 完成或失敗判定
        if (process >= time) {
            finishRitual(peds);
            reset();
        }
    }

    // --- 輔助邏輯方法 ---

    private void updateClientVisuals() {
        // 更新旋轉角度用於渲染插值
        lastClientRotation = clientRotation;
        clientRotation += (isActive ? 10.0f : 1.0f);

        if (isActive && hasEnoughEnergy) {
            spawnParticles();
        }
    }

    private void spawnParticles() {
        // 在這裡生成更加複雜的粒子效果，例如從基座飛向核心的粒子
        if (world.rand.nextInt(3) == 0) {
            world.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    0, 0.1, 0);
        }
    }

    private boolean checkAndConsumeEnergy(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe, int amount, boolean simulate) {
        int count = recipe.getPedestalCount();
        // 檢查前 N 個基座是否有足夠能量
        for (int i = 0; i < count && i < peds.size(); i++) {
            TileEntityPedestal ped = peds.get(i);
            if (ped.getEnergy().extractEnergy(amount, true) < amount) {
                return false;
            }
        }

        if (!simulate) {
            for (int i = 0; i < count && i < peds.size(); i++) {
                peds.get(i).getEnergy().extractEnergy(amount, false);
            }
        }
        return true;
    }

    private void finishRitual(List<TileEntityPedestal> peds) {
        // 先保存配方引用，因为后续操作可能触发reset()
        RitualInfusionRecipe recipe = activeRecipe;

        // 标记为非活动状态，防止onContentsChanged触发reset
        isActive = false;

        // 失敗判定 (Risk mechanics)
        if (recipe.getFailChance() > 0 && world.rand.nextFloat() < recipe.getFailChance()) {
            System.out.println("[Ritual] Failed! Explosion!");
            doFailExplosion();
        } else {
            // 成功：產生產物
            ItemStack output = recipe.getOutput();
            System.out.println("[Ritual] Recipe output: " + output + " isEmpty=" + output.isEmpty());

            if (output.isEmpty()) {
                System.err.println("[Ritual] ERROR: Output is empty! Recipe class: " + recipe.getClass().getSimpleName());
            } else {
                ItemStack outputCopy = output.copy();
                System.out.println("[Ritual] Setting output slot to: " + outputCopy.getDisplayName());
                inv.setStackInSlot(1, outputCopy); // 放入產出槽
            }
        }

        // 消耗原材料（使用保存的配方引用）
        consumeIngredientsWithRecipe(peds, recipe);

        // 視覺通知
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        markDirty();
    }

    private void consumeIngredientsWithRecipe(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe) {
        inv.extractItem(0, 1, false); // 消耗核心
        System.out.println("[Ritual] Core item consumed from slot 0");

        // 安全检查
        if (recipe == null) {
            System.err.println("[Ritual] Warning: recipe is null in consumeIngredients!");
            return;
        }

        List<net.minecraft.item.crafting.Ingredient> pedestalItems = recipe.getPedestalItems();
        System.out.println("[Ritual] Pedestal items to consume: " + (pedestalItems != null ? pedestalItems.size() : "null"));

        if (pedestalItems == null || pedestalItems.isEmpty()) {
            System.err.println("[Ritual] Warning: Recipe has null/empty pedestal items!");
            // 仍然尝试消耗基座上的物品（每个基座消耗1个）
            int consumed = 0;
            for (TileEntityPedestal ped : peds) {
                if (!ped.isEmpty()) {
                    ped.consumeOne();
                    consumed++;
                    if (consumed >= 2) break; // 最多消耗2个基座的物品
                }
            }
            System.out.println("[Ritual] Fallback: consumed " + consumed + " pedestal items");
            return;
        }

        List<net.minecraft.item.crafting.Ingredient> needed = new ArrayList<>(pedestalItems);
        // 簡單的匹配消耗邏輯
        for (TileEntityPedestal ped : peds) {
            ItemStack stack = ped.getInv().getStackInSlot(0);
            System.out.println("[Ritual] Checking pedestal with: " + stack.getDisplayName());
            for (int i = 0; i < needed.size(); i++) {
                boolean matches = needed.get(i).apply(stack);
                System.out.println("[Ritual]   - Ingredient " + i + " matches: " + matches);
                if (matches) {
                    ped.consumeOne();
                    needed.remove(i);
                    System.out.println("[Ritual]   - Consumed! Remaining needed: " + needed.size());
                    break;
                }
            }
        }
    }

    private void doFailExplosion() {
        world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 2.0F, true);
        // 可以添加更噁心的懲罰，比如產生咒蝕泥土(Tainted Soil)
    }

    private void reset() {
        process = 0;
        activeRecipe = null;
        updateState(false, false);
    }

    // 狀態同步優化：只有狀態真的改變時才發包，節省頻寬
    private void updateState(boolean active, boolean energy) {
        if (this.isActive != active || this.hasEnoughEnergy != energy) {
            this.isActive = active;
            this.hasEnoughEnergy = energy;
            markDirty();
            syncToClient();
        }
    }

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    private List<TileEntityPedestal> findValidPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                if (!ped.isEmpty()) list.add(ped);
            }
        }
        return list;
    }

    // 嚴格檢查：確保找到的基座物品真的符合當前配方 (防止中途換物品)
    private boolean isValidRitualStructure(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (TileEntityPedestal p : peds) stacks.add(p.getInv().getStackInSlot(0));
        return recipe.matchPedestalStacks(stacks);
    }

    private RitualInfusionRecipe findMatchingRecipe(List<TileEntityPedestal> peds) {
        List<ItemStack> stacks = new ArrayList<>();
        for (TileEntityPedestal p : peds) stacks.add(p.getInv().getStackInSlot(0));

        for (RitualInfusionRecipe r : RitualInfusionAPI.RITUAL_RECIPES) {
            if (r.getCore().apply(inv.getStackInSlot(0)) && r.matchPedestalStacks(stacks)) {
                return r;
            }
        }
        return null;
    }

    // --- 標準 Getters & Setters ---
    public boolean isActive() { return isActive; }
    public int getProgress() { return process; }
    public int getMaxTime() { return activeRecipe != null ? activeRecipe.getTime() : 100; }
    public ItemStackHandler getInv() { return inv; }
    public BlockPos[] getPedestalOffsets() { return OFFS8; }

    // --- 渲染關鍵優化 ---

    /**
     * [重要] 無限渲染邊界
     * 這是為了 Renderer 的光束效果。如果不重寫這個，
     * 當核心方塊在畫面外（但光束還在畫面內）時，光束會消失。
     */
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536.0D; // 讓它在很遠的地方也能被看見
    }

    // --- NBT 與 數據包 ---

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inv", inv.serializeNBT());
        compound.setInteger("Process", process);
        compound.setBoolean("Active", isActive);
        compound.setBoolean("HasEnergy", hasEnoughEnergy);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inv.deserializeNBT(compound.getCompoundTag("Inv"));
        process = compound.getInteger("Process");
        isActive = compound.getBoolean("Active");
        hasEnoughEnergy = compound.getBoolean("HasEnergy");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        // 強制標記渲染更新，確保光束狀態立刻刷新
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    // --- Capability (自動化支持) ---

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inv);
        }
        return super.getCapability(capability, facing);
    }
}