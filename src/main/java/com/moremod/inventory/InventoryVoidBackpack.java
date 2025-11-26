package com.moremod.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * 虚空背包全局库存
 * 使用 WorldSavedData 实现持久化存储
 * 服务端单例，客户端使用临时副本
 */
public class InventoryVoidBackpack extends WorldSavedData implements IInventory {

    private static final String DATA_NAME = "VoidBackpackData";
    private static final int MAX_SLOTS = 27;

    // 服务端实例缓存
    private static InventoryVoidBackpack serverInstance = null;

    private NonNullList<ItemStack> inventory;
    private boolean isDirty = false;

    /**
     * 默认构造函数（用于反射创建）
     */
    public InventoryVoidBackpack() {
        super(DATA_NAME);
        this.inventory = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        System.out.println("[InventoryVoidBackpack] 默认构造函数创建实例");
    }

    /**
     * 命名构造函数
     */
    public InventoryVoidBackpack(String name) {
        super(name);
        this.inventory = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        System.out.println("[InventoryVoidBackpack] 命名构造函数创建实例: " + name);
    }

    /**
     * 获取虚空背包实例
     * 服务端：从 WorldSavedData 获取或创建
     * 客户端：创建临时实例
     */
    public static InventoryVoidBackpack get(World world) {
        if (world == null) {
            System.err.println("[InventoryVoidBackpack] 错误：World为null！");
            return createEmptyInstance();
        }

        // 客户端：创建临时实例（GUI显示用）
        if (world.isRemote) {
            System.out.println("[InventoryVoidBackpack] 客户端请求：创建临时GUI实例");
            return createEmptyInstance();
        }

        // 服务端：从存档获取持久化数据
        System.out.println("[InventoryVoidBackpack] 服务端请求：加载持久化数据");

        try {
            MapStorage storage = world.getMapStorage();
            if (storage == null) {
                System.err.println("[InventoryVoidBackpack] 错误：MapStorage为null！");
                return createEmptyInstance();
            }

            InventoryVoidBackpack instance = (InventoryVoidBackpack) storage.getOrLoadData(
                    InventoryVoidBackpack.class,
                    DATA_NAME
            );

            if (instance == null) {
                System.out.println("[InventoryVoidBackpack] 未找到存档数据，创建新实例");
                instance = new InventoryVoidBackpack();
                storage.setData(DATA_NAME, instance);
            } else {
                System.out.println("[InventoryVoidBackpack] 成功加载存档数据");
            }

            serverInstance = instance;
            return instance;

        } catch (Exception e) {
            System.err.println("[InventoryVoidBackpack] 加载数据时发生异常：");
            e.printStackTrace();
            return createEmptyInstance();
        }
    }

    /**
     * 创建空的临时实例（客户端用）
     */
    private static InventoryVoidBackpack createEmptyInstance() {
        InventoryVoidBackpack temp = new InventoryVoidBackpack("temp");
        System.out.println("[InventoryVoidBackpack] 创建临时空实例");
        return temp;
    }

    /**
     * 从 NBT 读取数据
     */
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        System.out.println("[InventoryVoidBackpack] 从NBT读取数据");

        NBTTagList list = nbt.getTagList("Items", 10);
        this.inventory = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);

        int loadedCount = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;

            if (slot >= 0 && slot < this.inventory.size()) {
                ItemStack stack = new ItemStack(itemTag);
                this.inventory.set(slot, stack);
                if (!stack.isEmpty()) {
                    loadedCount++;
                }
            }
        }

        System.out.println("[InventoryVoidBackpack] 读取完成，加载了 " + loadedCount + " 个物品");
    }

    /**
     * 写入 NBT 数据
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        System.out.println("[InventoryVoidBackpack] 写入NBT数据");

        NBTTagList list = new NBTTagList();
        int savedCount = 0;

        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (!stack.isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                stack.writeToNBT(itemTag);
                list.appendTag(itemTag);
                savedCount++;
            }
        }

        nbt.setTag("Items", list);
        System.out.println("[InventoryVoidBackpack] 写入完成，保存了 " + savedCount + " 个物品");
        return nbt;
    }

    // ========== IInventory 接口实现 ==========

    @Override
    public int getSizeInventory() {
        return MAX_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= this.inventory.size()) {
            return ItemStack.EMPTY;
        }
        return this.inventory.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = this.inventory.get(index);
        if (!stack.isEmpty()) {
            ItemStack result;

            if (stack.getCount() <= count) {
                result = stack;
                this.inventory.set(index, ItemStack.EMPTY);
            } else {
                result = stack.splitStack(count);
                if (stack.isEmpty()) {
                    this.inventory.set(index, ItemStack.EMPTY);
                }
            }

            this.markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = this.inventory.get(index);
        if (!stack.isEmpty()) {
            this.inventory.set(index, ItemStack.EMPTY);
            this.markDirty();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= this.inventory.size()) {
            System.err.println("[InventoryVoidBackpack] 警告：槽位索引越界 " + index);
            return;
        }

        this.inventory.set(index, stack);

        if (!stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }

        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.isDirty = true;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        // 虚空背包对所有玩家可用
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        System.out.println("[InventoryVoidBackpack] 玩家 " + player.getName() + " 打开虚空背包");
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        System.out.println("[InventoryVoidBackpack] 玩家 " + player.getName() + " 关闭虚空背包");
        this.markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
        // 不使用字段
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        System.out.println("[InventoryVoidBackpack] 清空所有物品");
        for (int i = 0; i < this.inventory.size(); i++) {
            this.inventory.set(i, ItemStack.EMPTY);
        }
        this.markDirty();
    }

    @Override
    public String getName() {
        return "container.void_backpack";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("虚空背包");
    }

    // ========== 调试方法 ==========

    /**
     * 打印库存内容（调试用）
     */
    public void debugPrint() {
        System.out.println("[InventoryVoidBackpack] ===== 库存内容 =====");
        int itemCount = 0;

        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (!stack.isEmpty()) {
                System.out.println("[InventoryVoidBackpack] 槽位 " + i + ": " +
                        stack.getDisplayName() + " x" + stack.getCount());
                itemCount++;
            }
        }

        System.out.println("[InventoryVoidBackpack] 总物品数: " + itemCount + "/" + MAX_SLOTS);
        System.out.println("[InventoryVoidBackpack] ========================");
    }

    /**
     * 获取物品总数
     */
    public int getItemCount() {
        int count = 0;
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查是否有变化需要保存
     */
    public boolean needsSaving() {
        return this.isDirty;
    }
}