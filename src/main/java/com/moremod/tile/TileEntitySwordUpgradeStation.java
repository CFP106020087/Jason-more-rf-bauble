package com.moremod.tile;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 剑升级站 TileEntity - 宝石镶嵌系统（新版）
 *
 * 核心改动：
 * 1. 使用 GemSocketHelper 镶嵌宝石（不是旧的材料系统）
 * 2. 只接受已鉴定的宝石（GemNBTHelper.isIdentified）
 * 3. NBT结构使用 "SocketedGems" 而不是 "SwordUpgrades"
 * 4. 预览显示宝石词条，而不是材料属性
 */
public class TileEntitySwordUpgradeStation extends TileEntity implements ITickable {

    // ==================== 槽位索引常量 ====================

    public static final int SLOT_OUTPUT = 0;   // 输出/拆除输入槽
    public static final int SLOT_MAT0 = 1;     // 宝石槽1
    public static final int SLOT_MAT1 = 2;     // 宝石槽2
    public static final int SLOT_MAT2 = 3;     // 宝石槽3
    public static final int SLOT_MAT3 = 4;     // 宝石槽4
    public static final int SLOT_MAT4 = 5;     // 宝石槽5
    public static final int SLOT_MAT5 = 6;     // 宝石槽6 (新增)
    public static final int SLOT_SWORD = 7;    // 剑槽
    public static final int SLOT_COUNT = 8;

    // 材料槽范围
    public static final int MATERIAL_SLOT_START = SLOT_MAT0;
    public static final int MATERIAL_SLOT_END = SLOT_MAT5;
    public static final int MAX_INLAY_COUNT = 6; // 最多6个宝石

    // ==================== 操作模式 ====================

    public enum Mode {
        IDLE,      // 空闲
        UPGRADE,   // 升级模式：SWORD槽有剑 + 材料槽有宝石 → OUTPUT预览结果
        REMOVAL    // 拆除模式：OUTPUT有已镶嵌的剑 → 材料槽预览宝石 + SWORD槽预览返还剑
    }

    // ==================== 防止递归标志位 ====================
    private boolean isUpdatingPreview = false;

    // ==================== 预览结果 ====================
    private ItemStack upgradePreviewResult = ItemStack.EMPTY;

    // ==================== 物品处理器 ====================

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (isUpdatingPreview) {
                return;
            }

            // 槽位变化时清理预览
            if (slot == SLOT_SWORD) {
                ItemStack swordStack = items.getStackInSlot(SLOT_SWORD);
                if (swordStack.isEmpty()) {
                    ItemStack outputStack = items.getStackInSlot(SLOT_OUTPUT);
                    if (!outputStack.isEmpty() && isPreview(outputStack)) {
                        items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
                        upgradePreviewResult = ItemStack.EMPTY;
                    }
                }
            }

            if (slot == SLOT_OUTPUT) {
                ItemStack outputStack = items.getStackInSlot(SLOT_OUTPUT);
                if (outputStack.isEmpty()) {
                    boolean hasPreviewGems = false;
                    for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
                        ItemStack mat = items.getStackInSlot(i);
                        if (!mat.isEmpty() && isPreview(mat)) {
                            hasPreviewGems = true;
                            break;
                        }
                    }

                    if (hasPreviewGems) {
                        for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
                            items.setStackInSlot(i, ItemStack.EMPTY);
                        }
                        ItemStack swordStack = items.getStackInSlot(SLOT_SWORD);
                        if (!swordStack.isEmpty() && isPreview(swordStack)) {
                            items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);
                        }
                    }
                }
            }

            updatePreview();
            markDirty();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 阻止Preview物品被插入
            if (!stack.isEmpty() && isPreview(stack)) {
                return false;
            }

            if (slot == SLOT_OUTPUT) {
                Mode mode = getCurrentMode();
                if (mode == Mode.UPGRADE) {
                    return false; // 升级模式下OUTPUT是预览槽
                }
                return stack.getItem() instanceof ItemSword;
            } else if (slot >= MATERIAL_SLOT_START && slot <= MATERIAL_SLOT_END) {
                Mode mode = getCurrentMode();
                if (mode == Mode.UPGRADE || mode == Mode.IDLE) {
                    // ✅ 核心修改：只接受已鉴定的宝石
                    return GemNBTHelper.isGem(stack) && GemNBTHelper.isIdentified(stack);
                }
                return false; // 拆除模式下材料槽不接受物品
            } else if (slot == SLOT_SWORD) {
                Mode mode = getCurrentMode();
                if (mode == Mode.REMOVAL) {
                    return false; // 拆除模式下剑槽不接受物品
                }
                if (mode == Mode.UPGRADE || mode == Mode.IDLE) {
                    return stack.getItem() instanceof ItemSword;
                }
                return false;
            }
            return false;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = items.getStackInSlot(slot);

            // 防止提取Preview物品
            if (!stack.isEmpty() && isPreview(stack)) {
                return ItemStack.EMPTY;
            }

            Mode mode = getCurrentMode();

            if (slot == SLOT_OUTPUT) {
                if (mode == Mode.UPGRADE) {
                    return ItemStack.EMPTY; // 升级模式下OUTPUT是预览
                }
                return super.extractItem(slot, amount, simulate);
            } else if (slot >= MATERIAL_SLOT_START && slot <= MATERIAL_SLOT_END) {
                if (mode == Mode.REMOVAL) {
                    return ItemStack.EMPTY; // 拆除模式：材料槽不能直接提取
                }
                return super.extractItem(slot, amount, simulate);
            } else if (slot == SLOT_SWORD) {
                if (mode == Mode.REMOVAL) {
                    return ItemStack.EMPTY; // 拆除模式：剑槽不能直接提取
                }
                return super.extractItem(slot, amount, simulate);
            }

            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 阻止插入Preview物品
            if (!stack.isEmpty() && isPreview(stack)) {
                return stack;
            }

            Mode mode = getCurrentMode();

            if (mode == Mode.REMOVAL) {
                if ((slot >= MATERIAL_SLOT_START && slot <= MATERIAL_SLOT_END) ||
                        slot == SLOT_SWORD) {
                    return stack; // 拆除模式：材料槽和剑槽都是预览
                }
            }

            if (mode == Mode.UPGRADE && slot == SLOT_OUTPUT) {
                return stack; // 升级模式：OUTPUT槽是预览
            }

            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // 只有updatePreview可以设置Preview物品
            if (!stack.isEmpty() && isPreview(stack)) {
                if (!isUpdatingPreview) {
                    return;
                }
            }
            super.setStackInSlot(slot, stack);
        }
    };

    private long lastUpdateTick = -1L;

    // ==================== ITickable ====================

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        long currentTick = world.getTotalWorldTime();

        if (lastUpdateTick == -1L) {
            lastUpdateTick = currentTick;
            return;
        }

        if (currentTick - lastUpdateTick >= 20) {
            updatePreview();
            lastUpdateTick = currentTick;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查物品是否是Preview
     */
    private boolean isPreview(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() &&
                stack.getTagCompound().getBoolean("Preview");
    }

    /**
     * 标记物品为Preview
     */
    private ItemStack markAsPreview(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean("Preview", true);
        return stack;
    }

    /**
     * 移除Preview标记
     */
    private ItemStack unmarkPreview(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("Preview");
        }
        return stack;
    }

    // ==================== 模式判定 ====================

    public Mode getCurrentMode() {
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        ItemStack sword = items.getStackInSlot(SLOT_SWORD);

        // 忽略Preview物品
        if (isPreview(output)) output = ItemStack.EMPTY;
        if (isPreview(sword)) sword = ItemStack.EMPTY;

        // 优先级1：拆除模式（OUTPUT有已镶嵌的剑）
        if (!output.isEmpty() && output.getItem() instanceof ItemSword) {
            if (GemSocketHelper.hasSocketedGems(output)) {
                return Mode.REMOVAL;
            }
        }

        // 优先级2：升级模式（SWORD槽有剑）
        if (!sword.isEmpty() && sword.getItem() instanceof ItemSword) {
            return Mode.UPGRADE;
        }

        return Mode.IDLE;
    }

    // ==================== 预览更新 ====================

    public void updatePreview() {
        if (world == null || world.isRemote) return;

        isUpdatingPreview = true;

        try {
            Mode mode = getCurrentMode();

            if (mode == Mode.UPGRADE) {
                updateUpgradePreview();
            } else if (mode == Mode.REMOVAL) {
                updateRemovalPreview();
            } else {
                clearAllPreviews();
            }
        } finally {
            isUpdatingPreview = false;
        }
    }

    /**
     * 更新升级模式预览
     * 显示：已镶嵌的宝石（preview）+ 新宝石 → OUTPUT预览结果
     */
    private void updateUpgradePreview() {
        ItemStack sword = items.getStackInSlot(SLOT_SWORD);
        if (sword.isEmpty()) {
            clearAllPreviews();
            return;
        }

        // 获取剑上已镶嵌的宝石
        ItemStack[] existingGems = GemSocketHelper.getAllSocketedGems(sword);
        int existingCount = existingGems.length;

        // 在材料槽前几个显示已镶嵌的宝石（preview，不能动）
        for (int i = 0; i < existingCount && i < MAX_INLAY_COUNT; i++) {
            ItemStack gemPreview = existingGems[i].copy();
            markAsPreview(gemPreview);
            gemPreview.getTagCompound().setBoolean("ExistingInlay", true);
            items.setStackInSlot(MATERIAL_SLOT_START + i, gemPreview);
        }

        // 收集新宝石（跳过已镶嵌的槽位）
        List<ItemStack> newGems = new ArrayList<>();
        for (int i = existingCount; i <= MATERIAL_SLOT_END - MATERIAL_SLOT_START; i++) {
            ItemStack mat = items.getStackInSlot(MATERIAL_SLOT_START + i);
            // 只收集真实的已鉴定宝石（非preview）
            if (!mat.isEmpty() && !isPreview(mat) &&
                    GemNBTHelper.isGem(mat) && GemNBTHelper.isIdentified(mat)) {
                newGems.add(mat);
            }
        }

        // 生成预览结果
        ItemStack preview = sword.copy();

        // ✅ 核心：使用 GemSocketHelper 镶嵌新宝石
        for (ItemStack gem : newGems) {
            if (GemSocketHelper.canSocketMore(preview)) {
                GemSocketHelper.socketGem(preview, gem);
            } else {
                break; // 已达上限
            }
        }

        // 如果有新宝石或已有镶嵌，显示OUTPUT预览
        if (!newGems.isEmpty() || existingCount > 0) {
            markAsPreview(preview);
            upgradePreviewResult = preview.copy();
            items.setStackInSlot(SLOT_OUTPUT, preview);
        } else {
            ItemStack outputStack = items.getStackInSlot(SLOT_OUTPUT);
            if (!outputStack.isEmpty() && isPreview(outputStack)) {
                items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            }
            upgradePreviewResult = ItemStack.EMPTY;
        }
    }

    /**
     * 更新拆除模式预览
     * 显示：材料槽预览宝石 + SWORD槽预览返还剑
     */
    private void updateRemovalPreview() {
        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty()) {
            clearAllPreviews();
            return;
        }

        // 获取镶嵌的宝石
        ItemStack[] socketedGems = GemSocketHelper.getAllSocketedGems(sword);

        if (socketedGems.length == 0) {
            clearAllPreviews();
            return;
        }

        // 在材料槽显示预览宝石
        for (int i = 0; i < Math.min(socketedGems.length, MAX_INLAY_COUNT); i++) {
            ItemStack gemPreview = socketedGems[i].copy();
            markAsPreview(gemPreview);
            items.setStackInSlot(MATERIAL_SLOT_START + i, gemPreview);
        }

        // 在剑槽显示预览的返还剑（所有宝石被移除）
        ItemStack cleanSword = sword.copy();
        GemSocketHelper.removeAllGems(cleanSword);
        markAsPreview(cleanSword);
        items.setStackInSlot(SLOT_SWORD, cleanSword);
    }

    /**
     * 清空所有预览
     */
    private void clearAllPreviews() {
        // 清空OUTPUT预览
        ItemStack outputStack = items.getStackInSlot(SLOT_OUTPUT);
        if (!outputStack.isEmpty() && isPreview(outputStack)) {
            items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        }

        // 清空材料槽预览
        for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
            ItemStack mat = items.getStackInSlot(i);
            if (!mat.isEmpty() && isPreview(mat)) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        // 清空剑槽预览
        ItemStack swordStack = items.getStackInSlot(SLOT_SWORD);
        if (!swordStack.isEmpty() && isPreview(swordStack)) {
            items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);
        }

        upgradePreviewResult = ItemStack.EMPTY;
    }

    // ==================== 镶嵌信息（用于GUI显示）====================

    public static class InlayInfo {
        public final int index;
        public final ItemStack gem;
        public final List<IdentifiedAffix> affixes;

        public InlayInfo(int index, ItemStack gem, List<IdentifiedAffix> affixes) {
            this.index = index;
            this.gem = gem;
            this.affixes = affixes;
        }
    }

    /**
     * 获取剑上的镶嵌列表（用于GUI显示）
     */
    public List<InlayInfo> getInlayList() {
        List<InlayInfo> result = new ArrayList<>();

        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty() || !GemSocketHelper.hasSocketedGems(sword)) {
            return result;
        }

        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(sword);

        for (int i = 0; i < gems.length; i++) {
            ItemStack gem = gems[i];
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            result.add(new InlayInfo(i, gem, affixes));
        }

        return result;
    }

    // ==================== 单个宝石拆除（右键点击）====================

    /**
     * 右键点击材料槽拆除单个宝石
     */
    public void removeSingleGem(int slotIndex, EntityPlayer player) {
        if (getCurrentMode() != Mode.REMOVAL) return;
        if (slotIndex < MATERIAL_SLOT_START || slotIndex > MATERIAL_SLOT_END) return;

        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty()) return;

        int gemIndex = slotIndex - MATERIAL_SLOT_START;
        ItemStack[] socketedGems = GemSocketHelper.getAllSocketedGems(sword);

        if (gemIndex >= socketedGems.length) return;

        // TODO: 检查经验等级（如果需要消耗经验）
        // int removalCost = ...;
        // if (player.experienceLevel < removalCost) return;
        // player.addExperienceLevel(-removalCost);

        // 移除宝石
        ItemStack removedGem = GemSocketHelper.removeGem(sword, gemIndex);

        if (!removedGem.isEmpty()) {
            unmarkPreview(removedGem);

            // 给玩家宝石
            if (!player.inventory.addItemStackToInventory(removedGem)) {
                player.dropItem(removedGem, false);
            }

            // 更新OUTPUT槽的剑
            items.setStackInSlot(SLOT_OUTPUT, sword);

            updatePreview();
            markDirty();
            syncToClient();
        }
    }

    /**
     * 拆除所有宝石（箭头按钮）
     */
    public void removeAllGems(EntityPlayer player) {
        if (getCurrentMode() != Mode.REMOVAL) return;

        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty()) return;

        ItemStack[] socketedGems = GemSocketHelper.getAllSocketedGems(sword);
        if (socketedGems.length == 0) return;

        // TODO: 检查经验等级
        // int totalCost = ...;
        // if (player.experienceLevel < totalCost) return;
        // player.addExperienceLevel(-totalCost);

        // 移除所有宝石
        ItemStack[] removedGems = GemSocketHelper.removeAllGems(sword);

        // 清空预览
        isUpdatingPreview = true;
        try {
            items.setStackInSlot(SLOT_OUTPUT, sword); // 干净的剑放回OUTPUT

            // 清空预览槽
            for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
            items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);
        } finally {
            isUpdatingPreview = false;
        }

        // 给玩家宝石
        for (ItemStack gem : removedGems) {
            unmarkPreview(gem);
            if (!player.inventory.addItemStackToInventory(gem)) {
                player.dropItem(gem, false);
            }
        }

        markDirty();
        syncToClient();
    }

    // ==================== 星形升级（箭头按钮）====================

    public boolean canPerformStarUpgrade() {
        return getCurrentMode() == Mode.UPGRADE;
    }

    public void performStarUpgrade() {
        if (!canPerformStarUpgrade()) return;

        ItemStack preview = items.getStackInSlot(SLOT_OUTPUT);
        if (preview.isEmpty()) return;

        // 移除Preview标记
        unmarkPreview(preview);

        // 获取已镶嵌数量
        ItemStack sword = items.getStackInSlot(SLOT_SWORD);
        int existingCount = GemSocketHelper.getSocketedGemCount(sword);

        // 只消耗新宝石（跳过已镶嵌的preview槽位）
        for (int i = MATERIAL_SLOT_START + existingCount; i <= MATERIAL_SLOT_END; i++) {
            ItemStack material = items.getStackInSlot(i);
            if (!material.isEmpty() && !isPreview(material)) {
                material.shrink(1);
            }
        }

        // 清空preview槽
        for (int i = MATERIAL_SLOT_START; i < MATERIAL_SLOT_START + existingCount; i++) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }

        // 清空剑槽
        items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);

        markDirty();
        syncToClient();
    }

    public boolean canPerformRemoveAll() {
        return getCurrentMode() == Mode.REMOVAL &&
                GemSocketHelper.getSocketedGemCount(items.getStackInSlot(SLOT_OUTPUT)) > 0;
    }

    // ==================== Capability ====================

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) items;
        }
        return super.getCapability(capability, facing);
    }

    public IItemHandler getItemHandler() {
        return items;
    }

    public ItemStack getStackInSlot(int index) {
        return items.getStackInSlot(index);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null) return false;
        if (world.getTileEntity(pos) != this) return false;
        return player.getDistanceSq(
                (double) pos.getX() + 0.5D,
                (double) pos.getY() + 0.5D,
                (double) pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    public ItemStack getUpgradePreviewResult() {
        return upgradePreviewResult;
    }

    // ==================== NBT ====================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", items.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Items")) {
            items.deserializeNBT(compound.getCompoundTag("Items"));
        }
        if (world != null && !world.isRemote) {
            updatePreview();
        }
    }

    // ==================== Client-Server同步 ====================

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setTag("Items", items.serializeNBT());
        return tag;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);
    }

    private void syncToClient() {
        if (world == null || world.isRemote) return;
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        markDirty();
    }
}