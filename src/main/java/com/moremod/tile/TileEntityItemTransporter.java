package com.moremod.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 高级物品传输器 TileEntity - 完整功能实现
 * 包含：两阶段提交传输、槽位控制、过滤系统、红石控制
 */
public class TileEntityItemTransporter extends TileEntity implements ITickable {

    // ===== 内部物品栏 =====
    public ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            syncToClient();
        }
    };

    // ===== 方向配置 =====
    public EnumFacing pullSide = EnumFacing.DOWN;
    public EnumFacing pushSide = EnumFacing.UP;

    // ===== 槽位范围配置 =====
    public int pullSlotStart = 0;
    public int pullSlotEnd = Integer.MAX_VALUE;
    public int pushSlotStart = 0;
    public int pushSlotEnd = Integer.MAX_VALUE;

    // ===== 过滤系统 =====
    private ItemStackHandler filterInventory = new ItemStackHandler(12);  // 12个过滤槽位
    public boolean isWhitelist = true;          // 白名单模式
    public boolean respectMeta = false;         // 匹配元数据
    public boolean respectNBT = false;          // 匹配NBT
    public boolean respectMod = false;          // 匹配Mod
    public int respectOredict = 0;              // 矿辞匹配: 0=关闭, 1=任意, 2=全部

    // ===== 红石控制 =====
    public boolean redstoneControlled = false;  // 是否受红石控制

    private int tickCounter = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return;  // 每秒执行一次

        // 红石控制检查
        if (redstoneControlled && world.isBlockPowered(pos)) {
            return;  // 被红石激活时停止工作
        }

        // 传输逻辑
        if (inventory.getStackInSlot(0).isEmpty()) {
            pullItems();
        }

        if (!inventory.getStackInSlot(0).isEmpty()) {
            pushItems();
        }
    }

    // ========================================
    // 传输核心逻辑 - 两阶段提交
    // ========================================

    /**
     * 从指定方向拉取物品（带过滤）
     */
    private void pullItems() {
        TileEntity target = world.getTileEntity(pos.offset(pullSide));
        if (target == null) return;

        IItemHandler targetInv = target.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                pullSide.getOpposite()
        );

        if (targetInv == null) return;

        int startSlot = Math.max(0, pullSlotStart);
        int endSlot = Math.min(pullSlotEnd, targetInv.getSlots() - 1);

        for (int i = startSlot; i <= endSlot; i++) {
            // 阶段1：模拟提取
            ItemStack extracted = targetInv.extractItem(i, 64, true);
            if (extracted.isEmpty()) continue;

            // 过滤检查
            if (!checkFilter(extracted)) continue;

            // 阶段2：尝试插入
            ItemStack remainder = inventory.insertItem(0, extracted, false);

            // 阶段3：真实提取
            if (remainder.getCount() < extracted.getCount()) {
                int toExtract = extracted.getCount() - remainder.getCount();
                targetInv.extractItem(i, toExtract, false);
                break;
            }
        }
    }

    /**
     * 向指定方向推送物品
     */
    private void pushItems() {
        TileEntity target = world.getTileEntity(pos.offset(pushSide));
        if (target == null) return;

        IItemHandler targetInv = target.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                pushSide.getOpposite()
        );

        if (targetInv == null) return;

        // 阶段1：模拟提取
        ItemStack toInsert = inventory.extractItem(0, 64, true);
        if (toInsert.isEmpty()) return;

        // 阶段2：尝试插入到目标
        ItemStack remainder = insertToRange(targetInv, toInsert, pushSlotStart, pushSlotEnd);

        // 阶段3：真实提取
        if (remainder.getCount() < toInsert.getCount()) {
            int transferred = toInsert.getCount() - remainder.getCount();
            inventory.extractItem(0, transferred, false);
        }
    }

    /**
     * 插入物品到指定槽位范围
     */
    private ItemStack insertToRange(IItemHandler handler, ItemStack stack, int start, int end) {
        ItemStack remaining = stack.copy();
        int startSlot = Math.max(0, start);
        int endSlot = Math.min(end, handler.getSlots() - 1);

        for (int i = startSlot; i <= endSlot; i++) {
            remaining = handler.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }

        return remaining;
    }

    // ========================================
    // 过滤系统 - 完整匹配逻辑
    // ========================================

    /**
     * 检查物品是否通过过滤器
     */
    public boolean checkFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 如果没有过滤物品，根据白名单模式决定
        if (!hasFilterItems()) {
            return !isWhitelist;  // 白名单无物品=拒绝，黑名单无物品=接受
        }

        // 检查是否匹配任何过滤物品
        boolean matched = false;
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack filter = filterInventory.getStackInSlot(i);
            if (!filter.isEmpty() && itemsMatch(filter, stack)) {
                matched = true;
                break;
            }
        }

        // 白名单：匹配=通过，黑名单：匹配=拒绝
        return isWhitelist == matched;
    }

    /**
     * 物品匹配逻辑（支持多种模式）
     */
    private boolean itemsMatch(ItemStack filter, ItemStack stack) {
        Item filterItem = filter.getItem();
        Item stackItem = stack.getItem();

        // Mod匹配模式：只检查是否同一个Mod
        if (respectMod) {
            String filterMod = filterItem.getRegistryName().getNamespace();
            String stackMod = stackItem.getRegistryName().getNamespace();
            return filterMod.equals(stackMod);
        }

        // 矿辞匹配模式
        if (respectOredict > 0) {
            int[] filterIds = OreDictionary.getOreIDs(filter);
            int[] stackIds = OreDictionary.getOreIDs(stack);

            boolean filterEmpty = ArrayUtils.isEmpty(filterIds);
            boolean stackEmpty = ArrayUtils.isEmpty(stackIds);

            if (filterEmpty && stackEmpty) return true;
            if (filterEmpty || stackEmpty) return false;

            if (respectOredict == 1) {
                // 模式1：至少一个矿辞匹配
                for (int id : filterIds) {
                    if (ArrayUtils.contains(stackIds, id)) return true;
                }
                return false;
            } else if (respectOredict == 2) {
                // 模式2：所有矿辞都要匹配
                for (int id : filterIds) {
                    if (!ArrayUtils.contains(stackIds, id)) return false;
                }
                return true;
            }
        }

        // 物品类型必须相同
        if (filterItem != stackItem) return false;

        // 元数据匹配
        if (respectMeta && filter.getItemDamage() != stack.getItemDamage()) {
            return false;
        }

        // NBT匹配
        if (respectNBT && !ItemStack.areItemStackTagsEqual(filter, stack)) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否有过滤物品
     */
    private boolean hasFilterItems() {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (!filterInventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ========================================
    // 配置管理方法
    // ========================================

    /**
     * 添加过滤物品
     */
    public boolean addFilterItem(ItemStack stack) {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (filterInventory.getStackInSlot(i).isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);  // 只保存1个作为样本
                filterInventory.setStackInSlot(i, copy);
                markDirty();
                syncToClient();
                return true;
            }
        }
        return false;
    }

    /**
     * 清空过滤器
     */
    public void clearFilter() {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            filterInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        markDirty();
        syncToClient();
    }

    /**
     * 获取过滤物品数量
     */
    public int getFilterItemCount() {
        int count = 0;
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (!filterInventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取所有过滤物品
     */
    public List<ItemStack> getFilterItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack stack = filterInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    /**
     * 切换白名单/黑名单模式
     */
    public void toggleWhitelist() {
        isWhitelist = !isWhitelist;
        markDirty();
        syncToClient();
    }

    /**
     * 切换匹配模式
     */
    public void toggleRespectMeta() {
        respectMeta = !respectMeta;
        markDirty();
        syncToClient();
    }

    public void toggleRespectNBT() {
        respectNBT = !respectNBT;
        markDirty();
        syncToClient();
    }

    public void toggleRespectMod() {
        respectMod = !respectMod;
        if (respectMod) {
            // Mod模式下，其他模式自动关闭
            respectMeta = false;
            respectNBT = false;
            respectOredict = 0;
        }
        markDirty();
        syncToClient();
    }

    public void cycleRespectOredict() {
        respectOredict = (respectOredict + 1) % 3;  // 0 -> 1 -> 2 -> 0
        markDirty();
        syncToClient();
    }

    /**
     * 设置槽位范围
     */
    public void setPullSlotRange(int start, int end) {
        this.pullSlotStart = Math.max(0, start);
        this.pullSlotEnd = Math.max(start, end);
        markDirty();
        syncToClient();
    }

    public void setPushSlotRange(int start, int end) {
        this.pushSlotStart = Math.max(0, start);
        this.pushSlotEnd = Math.max(start, end);
        markDirty();
        syncToClient();
    }

    /**
     * 切换方向
     */
    public void cyclePullSide() {
        pullSide = getNextFacing(pullSide);
        markDirty();
        syncToClient();
    }

    public void cyclePushSide() {
        pushSide = getNextFacing(pushSide);
        markDirty();
        syncToClient();
    }

    private EnumFacing getNextFacing(EnumFacing current) {
        EnumFacing[] facings = EnumFacing.values();
        int index = current.ordinal();
        return facings[(index + 1) % facings.length];
    }

    // ========================================
    // NBT 保存/读取
    // ========================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        // 保存内部物品栏
        compound.setTag("Inventory", inventory.serializeNBT());

        // 保存过滤器
        compound.setTag("FilterInventory", filterInventory.serializeNBT());

        // 保存方向
        compound.setInteger("PullSide", pullSide.getIndex());
        compound.setInteger("PushSide", pushSide.getIndex());

        // 保存槽位范围
        compound.setInteger("PullSlotStart", pullSlotStart);
        compound.setInteger("PullSlotEnd", pullSlotEnd);
        compound.setInteger("PushSlotStart", pushSlotStart);
        compound.setInteger("PushSlotEnd", pushSlotEnd);

        // 保存过滤设置
        compound.setBoolean("Whitelist", isWhitelist);
        compound.setBoolean("RespectMeta", respectMeta);
        compound.setBoolean("RespectNBT", respectNBT);
        compound.setBoolean("RespectMod", respectMod);
        compound.setInteger("RespectOredict", respectOredict);

        // 红石控制
        compound.setBoolean("RedstoneControlled", redstoneControlled);

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        // 读取内部物品栏
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }

        // 读取过滤器
        if (compound.hasKey("FilterInventory")) {
            filterInventory.deserializeNBT(compound.getCompoundTag("FilterInventory"));
        }

        // 读取方向
        pullSide = EnumFacing.byIndex(compound.getInteger("PullSide"));
        pushSide = EnumFacing.byIndex(compound.getInteger("PushSide"));

        // 读取槽位范围
        pullSlotStart = compound.getInteger("PullSlotStart");
        pullSlotEnd = compound.getInteger("PullSlotEnd");
        pushSlotStart = compound.getInteger("PushSlotStart");
        pushSlotEnd = compound.getInteger("PushSlotEnd");

        // 读取过滤设置
        isWhitelist = compound.getBoolean("Whitelist");
        respectMeta = compound.getBoolean("RespectMeta");
        respectNBT = compound.getBoolean("RespectNBT");
        respectMod = compound.getBoolean("RespectMod");
        respectOredict = compound.getInteger("RespectOredict");

        // 红石控制
        redstoneControlled = compound.getBoolean("RedstoneControlled");
    }

    // ========================================
    // 数据同步与持久化
    // ========================================

    /**
     * 防止方块状态更新时 TileEntity 被意外销毁
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /**
     * 获取初始区块数据（客户端同步）
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    /**
     * 处理更新标签
     */
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
    }

    /**
     * 获取更新数据包
     */
    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.writeToNBT(new NBTTagCompound()));
    }

    /**
     * 处理数据包
     */
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    /**
     * 同步到客户端
     */
    public void syncToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    // ========================================
    // Capability 支持
    // ========================================

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    /**
     * 获取过滤器物品栏（供GUI访问）
     */
    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}