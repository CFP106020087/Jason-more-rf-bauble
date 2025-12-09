package com.moremod.quarry.tile;

import com.moremod.quarry.QuarryConfig;
import com.moremod.quarry.QuarryMode;
import com.moremod.quarry.simulation.VirtualMiningSimulator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 量子采石场核心 TileEntity
 */
public class TileQuantumQuarry extends TileEntity implements ITickable {
    
    // 能量存储
    private final QuarryEnergyStorage energy = new QuarryEnergyStorage(
        QuarryConfig.ENERGY_CAPACITY, 
        QuarryConfig.ENERGY_TRANSFER_RATE
    );
    
    // 输出物品缓冲
    private final ItemStackHandler outputBuffer = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };
    
    // 附魔书槽位
    private final ItemStackHandler enchantSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isValidEnchantedBook(stack);
        }
        
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            cachedOperationTicks = -1;
            cachedEnergyPerOp = -1;
        }
    };
    
    // 过滤器槽位（如果设置，只收集匹配的物品）
    private final ItemStackHandler filterSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };
    
    // 工作状态
    private QuarryMode mode = QuarryMode.MINING;
    private Biome selectedBiome = null;
    private int selectedBiomeId = -1;
    private boolean redstoneControlEnabled = false;
    private boolean isPoweredByRedstone = false;
    
    // 运行时状态
    private int tickCounter = 0;
    private int cachedOperationTicks = -1;
    private int cachedEnergyPerOp = -1;
    private boolean structureValid = false;
    private long lastStructureCheck = 0;
    
    // 统计
    private long operationsCompleted = 0;
    private long itemsGenerated = 0;
    
    // 待输出物品队列
    private final Queue<ItemStack> pendingOutput = new LinkedList<>();
    
    public TileQuantumQuarry() {
    }
    
    // ==================== 核心逻辑 ====================
    
    @Override
    public void update() {
        if (world.isRemote) return;
        
        // 每秒检查一次结构
        if (world.getTotalWorldTime() - lastStructureCheck > 20) {
            lastStructureCheck = world.getTotalWorldTime();
            structureValid = checkStructure();
        }
        
        if (!structureValid) return;
        
        // 红石控制
        if (redstoneControlEnabled && !isPoweredByRedstone) return;
        
        // 尝试输出待处理物品
        processPendingOutput();
        
        // 检查是否可以操作
        if (!canOperate()) return;
        
        tickCounter++;
        int operationTicks = getOperationTicks();
        
        if (tickCounter >= operationTicks) {
            tickCounter = 0;
            performOperation();
        }
    }
    
    /**
     * 检查六面代理结构
     */
    private boolean checkStructure() {
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (!(te instanceof TileQuarryActuator)) {
                return false;
            }
            TileQuarryActuator actuator = (TileQuarryActuator) te;
            if (actuator.getFacing() != facing.getOpposite()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否可以进行操作
     */
    private boolean canOperate() {
        // 检查能量
        int energyNeeded = getEnergyPerOperation();
        if (energy.getEnergyStored() < energyNeeded) return false;
        
        // 检查生物群系
        if (selectedBiome == null) return false;
        
        // 检查输出空间
        if (pendingOutput.size() > 100) return false;  // 防止内存溢出
        
        return true;
    }
    
    /**
     * 执行一次操作
     */
    private void performOperation() {
        if (!(world instanceof WorldServer)) return;
        
        WorldServer worldServer = (WorldServer) world;
        VirtualMiningSimulator simulator = VirtualMiningSimulator.getInstance();
        
        // 获取附魔书
        ItemStack enchantedBook = enchantSlot.getStackInSlot(0);
        
        // 执行模拟
        List<ItemStack> drops = simulator.simulate(
            worldServer, 
            mode, 
            selectedBiome, 
            enchantedBook, 
            world.rand
        );
        
        // 过滤并添加到待输出队列
        ItemStack filterStack = filterSlot.getStackInSlot(0);
        boolean hasFilter = !filterStack.isEmpty();
        
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            
            if (hasFilter && !matchesFilter(drop, filterStack)) {
                continue;  // 过滤掉不匹配的物品
            }
            
            pendingOutput.offer(drop.copy());
            itemsGenerated += drop.getCount();
        }
        
        // 消耗能量
        energy.extractInternal(getEnergyPerOperation());
        operationsCompleted++;
        
        markDirty();
    }
    
    /**
     * 处理待输出物品
     */
    private void processPendingOutput() {
        // 先尝试放入内部缓冲
        while (!pendingOutput.isEmpty()) {
            ItemStack stack = pendingOutput.peek();
            
            // 尝试插入输出缓冲
            ItemStack remaining = insertToBuffer(stack);
            if (remaining.isEmpty()) {
                pendingOutput.poll();
            } else {
                // 更新队列中的数量
                pendingOutput.poll();
                if (!remaining.isEmpty()) {
                    pendingOutput.offer(remaining);
                }
                break;  // 缓冲满了
            }
        }
        
        // 尝试自动输出到相邻容器
        autoExportToNeighbors();
    }
    
    /**
     * 插入到输出缓冲
     */
    private ItemStack insertToBuffer(ItemStack stack) {
        return ItemHandlerHelper.insertItemStacked(outputBuffer, stack, false);
    }
    
    /**
     * 自动输出到相邻容器
     */
    private void autoExportToNeighbors() {
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te == null) continue;
            
            // 跳过自己的代理方块
            if (te instanceof TileQuarryActuator) continue;
            
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
            if (handler == null) continue;
            
            // 从输出缓冲转移物品
            for (int i = 0; i < outputBuffer.getSlots(); i++) {
                ItemStack stack = outputBuffer.extractItem(i, 64, true);
                if (stack.isEmpty()) continue;
                
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                int transferred = stack.getCount() - remaining.getCount();
                if (transferred > 0) {
                    outputBuffer.extractItem(i, transferred, false);
                }
            }
        }
    }
    
    /**
     * 检查物品是否匹配过滤器
     */
    private boolean matchesFilter(ItemStack stack, ItemStack filter) {
        return ItemStack.areItemsEqual(stack, filter) || 
               OreDictMatches(stack, filter);
    }
    
    /**
     * 检查矿物词典匹配
     */
    private boolean OreDictMatches(ItemStack a, ItemStack b) {
        int[] idsA = net.minecraftforge.oredict.OreDictionary.getOreIDs(a);
        int[] idsB = net.minecraftforge.oredict.OreDictionary.getOreIDs(b);
        
        for (int idA : idsA) {
            for (int idB : idsB) {
                if (idA == idB) return true;
            }
        }
        return false;
    }
    
    // ==================== 附魔处理 ====================
    
    /**
     * 验证附魔书是否有效
     */
    private boolean isValidEnchantedBook(ItemStack stack) {
        if (stack.getItem() != Items.ENCHANTED_BOOK) return false;
        
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        if (enchants.isEmpty()) return false;
        
        // 检查是否有有用的附魔
        return enchants.containsKey(Enchantments.FORTUNE) ||
               enchants.containsKey(Enchantments.SILK_TOUCH) ||
               enchants.containsKey(Enchantments.EFFICIENCY) ||
               enchants.containsKey(Enchantments.LOOTING) ||
               enchants.containsKey(Enchantments.LUCK_OF_THE_SEA);
    }
    
    private int getOperationTicks() {
        if (cachedOperationTicks < 0) {
            cachedOperationTicks = VirtualMiningSimulator.getInstance()
                .calculateOperationTicks(enchantSlot.getStackInSlot(0));
        }
        return cachedOperationTicks;
    }
    
    private int getEnergyPerOperation() {
        if (cachedEnergyPerOp < 0) {
            cachedEnergyPerOp = VirtualMiningSimulator.getInstance()
                .calculateEnergyPerOperation(enchantSlot.getStackInSlot(0));
        }
        return cachedEnergyPerOp;
    }
    
    // ==================== 公共方法（GUI 使用） ====================
    
    public QuarryMode getMode() { return mode; }
    
    public void setMode(QuarryMode mode) {
        this.mode = mode;
        markDirty();
        sendUpdatePacket();
    }
    
    public void cycleMode() {
        setMode(mode.next());
    }
    
    public Biome getSelectedBiome() { return selectedBiome; }
    
    public void setSelectedBiome(Biome biome) {
        this.selectedBiome = biome;
        this.selectedBiomeId = biome != null ? Biome.REGISTRY.getIDForObject(biome) : -1;
        markDirty();
        sendUpdatePacket();
    }
    
    public void setSelectedBiomeById(int id) {
        this.selectedBiomeId = id;
        this.selectedBiome = id >= 0 ? Biome.REGISTRY.getObjectById(id) : null;
        markDirty();
    }
    
    public boolean isRedstoneControlEnabled() { return redstoneControlEnabled; }
    
    public void setRedstoneControlEnabled(boolean enabled) {
        this.redstoneControlEnabled = enabled;
        markDirty();
    }
    
    public void toggleRedstoneControl() {
        setRedstoneControlEnabled(!redstoneControlEnabled);
    }
    
    public boolean isStructureValid() { return structureValid; }
    
    public long getOperationsCompleted() { return operationsCompleted; }
    
    public long getItemsGenerated() { return itemsGenerated; }
    
    public int getEnergyStored() { return energy.getEnergyStored(); }

    public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
    
    public int getProgress() {
        int opTicks = getOperationTicks();
        return opTicks > 0 ? (tickCounter * 100 / opTicks) : 0;
    }
    
    // GUI 槽位访问器
    public ItemStackHandler getEnchantSlot() { return enchantSlot; }
    public ItemStackHandler getFilterSlot() { return filterSlot; }
    public ItemStackHandler getOutputBuffer() { return outputBuffer; }
    
    // 客户端同步方法
    private int clientEnergy = 0;
    private int clientProgress = 0;
    private boolean clientStructureValid = false;
    
    public void setClientEnergy(int energy) {
        this.clientEnergy = energy;
    }
    
    public void setClientProgress(int progress) {
        this.clientProgress = progress;
    }
    
    public void setClientStructureValid(boolean valid) {
        this.clientStructureValid = valid;
    }
    

    
    /**
     * 从代理方块更新红石状态
     */
    public void updateRedstoneState() {
        boolean powered = false;
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te instanceof TileQuarryActuator) {
                if (((TileQuarryActuator) te).isPowered()) {
                    powered = true;
                    break;
                }
            }
        }
        this.isPoweredByRedstone = powered;
    }
    
    // ==================== 数据同步 ====================
    
    private void sendUpdatePacket() {
        if (!world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        
        compound.setTag("Energy", energy.serializeNBT());
        compound.setTag("OutputBuffer", outputBuffer.serializeNBT());
        compound.setTag("EnchantSlot", enchantSlot.serializeNBT());
        compound.setTag("FilterSlot", filterSlot.serializeNBT());
        
        compound.setInteger("Mode", mode.getMeta());
        compound.setInteger("BiomeId", selectedBiomeId);
        compound.setBoolean("RedstoneControl", redstoneControlEnabled);
        
        compound.setLong("OperationsCompleted", operationsCompleted);
        compound.setLong("ItemsGenerated", itemsGenerated);
        compound.setInteger("TickCounter", tickCounter);
        
        // 保存待输出队列
        NBTTagList pendingList = new NBTTagList();
        for (ItemStack stack : pendingOutput) {
            pendingList.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        compound.setTag("PendingOutput", pendingList);
        
        return compound;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        
        energy.deserializeNBT(compound.getCompoundTag("Energy"));
        outputBuffer.deserializeNBT(compound.getCompoundTag("OutputBuffer"));
        enchantSlot.deserializeNBT(compound.getCompoundTag("EnchantSlot"));
        filterSlot.deserializeNBT(compound.getCompoundTag("FilterSlot"));
        
        mode = QuarryMode.fromMeta(compound.getInteger("Mode"));
        selectedBiomeId = compound.getInteger("BiomeId");
        selectedBiome = selectedBiomeId >= 0 ? Biome.REGISTRY.getObjectById(selectedBiomeId) : null;
        redstoneControlEnabled = compound.getBoolean("RedstoneControl");
        
        operationsCompleted = compound.getLong("OperationsCompleted");
        itemsGenerated = compound.getLong("ItemsGenerated");
        tickCounter = compound.getInteger("TickCounter");
        
        // 读取待输出队列
        pendingOutput.clear();
        NBTTagList pendingList = compound.getTagList("PendingOutput", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pendingList.tagCount(); i++) {
            ItemStack stack = new ItemStack(pendingList.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                pendingOutput.offer(stack);
            }
        }
        
        cachedOperationTicks = -1;
        cachedEnergyPerOp = -1;
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("Mode", mode.getMeta());
        tag.setInteger("BiomeId", selectedBiomeId);
        tag.setBoolean("RedstoneControl", redstoneControlEnabled);
        tag.setBoolean("StructureValid", structureValid);
        tag.setInteger("Energy", energy.getEnergyStored());
        tag.setInteger("Progress", getProgress());
        return tag;
    }
    
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        mode = QuarryMode.fromMeta(tag.getInteger("Mode"));
        selectedBiomeId = tag.getInteger("BiomeId");
        selectedBiome = selectedBiomeId >= 0 ? Biome.REGISTRY.getObjectById(selectedBiomeId) : null;
        redstoneControlEnabled = tag.getBoolean("RedstoneControl");
        structureValid = tag.getBoolean("StructureValid");
        // 客户端显示用
    }
    
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }
    
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
    
    // ==================== Capability ====================
    
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }
    
    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energy);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(outputBuffer);
        }
        return super.getCapability(capability, facing);
    }
    
    // ==================== 内部能量存储类 ====================
    
    private static class QuarryEnergyStorage extends EnergyStorage {
        
        public QuarryEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);  // 不允许外部抽取
        }
        
        public void extractInternal(int amount) {
            this.energy -= Math.min(amount, this.energy);
        }
        
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", this.energy);
            return tag;
        }
        
        public void deserializeNBT(NBTTagCompound tag) {
            this.energy = tag.getInteger("Energy");
        }
    }
}
