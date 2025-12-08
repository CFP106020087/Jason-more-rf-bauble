package com.moremod.tile;

import com.mojang.authlib.GameProfile;
import com.moremod.block.BlockFakePlayerActivator;
import com.moremod.fakeplayer.FakePlayerAggroHandler;
import com.moremod.fakeplayer.ModFakePlayer;
import com.moremod.item.ritual.ItemFakePlayerCore;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 假玩家激活器 TileEntity (增强版)
 * - GUI 界面配置
 * - 可调节打击间隔
 * - 持续打击/点击模式
 * - 内置区块加载
 */
public class TileEntityFakePlayerActivator extends TileEntity implements ITickable {

    // 操作模式
    public enum Mode {
        RIGHT_CLICK("右键点击", 50),
        USE_ITEM("使用物品", 30),
        ATTACK("攻击生物", 100),
        BREAK_BLOCK("挖掘方块", 80);

        private final String displayName;
        private final int energyCost;

        Mode(String displayName, int energyCost) {
            this.displayName = displayName;
            this.energyCost = energyCost;
        }

        public String getDisplayName() { return displayName; }
        public int getEnergyCost() { return energyCost; }
    }

    // 配置
    private static final int MAX_ENERGY = 100000;
    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 100;
    private static final int DEFAULT_INTERVAL = 10;

    // 当前模式
    private Mode currentMode = Mode.RIGHT_CLICK;

    // 操作间隔（tick）
    private int actionInterval = DEFAULT_INTERVAL;

    // 区块加载
    private boolean chunkLoadingEnabled = false;
    private Ticket chunkTicket = null;

    // 能量存储
    private final EnergyStorageInternal energyStorage = new EnergyStorageInternal(MAX_ENERGY, 5000, 0);

    // 物品存储: 0=假玩家核心, 1-9=工具/物品槽
    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof ItemFakePlayerCore;
            }
            return true;
        }
    };

    // 运行状态
    private int tickCounter = 0;
    private boolean isActive = false;
    private int currentToolSlot = 1; // 当前使用的工具槽（1-9）

    // 持续操作状态
    private boolean continuousMode = true;
    private int breakProgress = 0;
    private BlockPos currentBreakPos = null;

    // 缓存的假玩家
    private WeakReference<ModFakePlayer> cachedFakePlayer = new WeakReference<>(null);

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 检查间隔
        if (tickCounter < actionInterval) {
            return;
        }
        tickCounter = 0;

        // 检查是否有有效的假玩家核心
        ItemStack coreStack = inventory.getStackInSlot(0);
        if (coreStack.isEmpty() || !(coreStack.getItem() instanceof ItemFakePlayerCore)) {
            setActiveState(false);
            return;
        }

        if (!ItemFakePlayerCore.hasStoredProfile(coreStack)) {
            setActiveState(false);
            return;
        }

        // 检查红石信号
        if (!world.isBlockPowered(pos)) {
            setActiveState(false);
            return;
        }

        // 检查能量
        int energyNeeded = currentMode.getEnergyCost();
        if (energyStorage.getEnergyStored() < energyNeeded) {
            setActiveState(false);
            return;
        }

        // 执行操作
        boolean success = performAction(coreStack);

        if (success) {
            energyStorage.extractEnergyInternal(energyNeeded);
            // 消耗核心耐久（每100次操作消耗1点）
            if (tickCounter % 100 == 0) {
                coreStack.damageItem(1, getFakePlayer(coreStack));
                if (coreStack.isEmpty()) {
                    inventory.setStackInSlot(0, ItemStack.EMPTY);
                }
            }
            ItemFakePlayerCore.incrementActivation(coreStack);
            setActiveState(true);
        }

        markDirty();
    }

    /**
     * 执行当前模式的操作
     */
    private boolean performAction(ItemStack coreStack) {
        ModFakePlayer fakePlayer = getFakePlayer(coreStack);
        if (fakePlayer == null) return false;

        IBlockState state = world.getBlockState(pos);
        EnumFacing facing = state.getValue(BlockFakePlayerActivator.FACING);

        BlockPos targetPos = pos.offset(facing);
        fakePlayer.setLocationAndFacing(targetPos, facing);

        // 获取当前工具
        ItemStack toolStack = getCurrentTool();
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, toolStack.copy());
        fakePlayer.updateEquipmentAttributes();

        boolean success = false;

        switch (currentMode) {
            case RIGHT_CLICK:
                success = performRightClick(fakePlayer, targetPos, facing);
                break;
            case USE_ITEM:
                success = performUseItem(fakePlayer, targetPos, facing);
                break;
            case ATTACK:
                success = performAttack(fakePlayer, targetPos);
                break;
            case BREAK_BLOCK:
                success = performBreakBlock(fakePlayer, targetPos);
                break;
        }

        // 更新工具
        ItemStack newTool = fakePlayer.getHeldItemMainhand();
        updateCurrentTool(newTool);

        // 收集掉落物
        collectDroppedItems(fakePlayer);

        return success;
    }

    /**
     * 获取当前工具
     */
    private ItemStack getCurrentTool() {
        ItemStack tool = inventory.getStackInSlot(currentToolSlot);
        if (tool.isEmpty()) {
            // 自动切换到下一个有物品的槽位
            for (int i = 1; i < 10; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    currentToolSlot = i;
                    return stack;
                }
            }
        }
        return tool;
    }

    /**
     * 更新当前工具
     */
    private void updateCurrentTool(ItemStack newTool) {
        if (newTool.isEmpty() || newTool.getItemDamage() >= newTool.getMaxDamage()) {
            inventory.setStackInSlot(currentToolSlot, ItemStack.EMPTY);
            // 切换到下一个工具
            for (int i = 1; i < 10; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    currentToolSlot = i;
                    break;
                }
            }
        } else {
            inventory.setStackInSlot(currentToolSlot, newTool);
        }
    }

    /**
     * 右键点击方块
     */
    private boolean performRightClick(ModFakePlayer fakePlayer, BlockPos targetPos, EnumFacing facing) {
        IBlockState targetState = world.getBlockState(targetPos);

        if (targetState.getBlock().isAir(targetState, world, targetPos)) {
            ItemStack tool = fakePlayer.getHeldItemMainhand();
            if (!tool.isEmpty()) {
                EnumActionResult result = tool.getItem().onItemUse(
                    fakePlayer, world, targetPos.offset(facing.getOpposite()), EnumHand.MAIN_HAND,
                    facing, 0.5F, 0.5F, 0.5F
                );
                return result == EnumActionResult.SUCCESS;
            }
            return false;
        }

        try {
            return targetState.getBlock().onBlockActivated(
                world, targetPos, targetState, fakePlayer, EnumHand.MAIN_HAND,
                facing.getOpposite(), 0.5F, 0.5F, 0.5F
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 使用物品
     */
    private boolean performUseItem(ModFakePlayer fakePlayer, BlockPos targetPos, EnumFacing facing) {
        ItemStack tool = fakePlayer.getHeldItemMainhand();
        if (tool.isEmpty()) return false;

        EnumActionResult result = tool.getItem().onItemUse(
            fakePlayer, world, targetPos, EnumHand.MAIN_HAND,
            facing.getOpposite(), 0.5F, 0.5F, 0.5F
        );

        if (result == EnumActionResult.SUCCESS) {
            updateCurrentTool(fakePlayer.getHeldItemMainhand());
            return true;
        }

        tool.getItem().onItemRightClick(world, fakePlayer, EnumHand.MAIN_HAND);
        updateCurrentTool(fakePlayer.getHeldItemMainhand());
        return true;
    }

    /**
     * 攻击生物
     */
    private boolean performAttack(ModFakePlayer fakePlayer, BlockPos targetPos) {
        AxisAlignedBB searchBox = new AxisAlignedBB(targetPos).grow(2.0);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
            EntityLivingBase.class, searchBox,
            e -> e != null && e.isEntityAlive() && !(e instanceof EntityPlayer)
        );

        if (entities.isEmpty()) return false;

        fakePlayer.updateCooldown();
        EntityLivingBase target = entities.get(0);
        fakePlayer.attackTargetEntityWithCurrentItem(target);

        // 清除仇恨
        FakePlayerAggroHandler.clearAggroInArea(fakePlayer, 10.0);

        return true;
    }

    /**
     * 挖掘方块（持续）
     */
    private boolean performBreakBlock(ModFakePlayer fakePlayer, BlockPos targetPos) {
        IBlockState targetState = world.getBlockState(targetPos);

        if (targetState.getBlock().isAir(targetState, world, targetPos)) {
            breakProgress = 0;
            currentBreakPos = null;
            return false;
        }

        // 计算破坏进度
        float hardness = targetState.getBlockHardness(world, targetPos);
        if (hardness < 0) return false; // 不可破坏

        ItemStack tool = fakePlayer.getHeldItemMainhand();
        float breakSpeed = tool.getDestroySpeed(targetState);
        if (breakSpeed <= 1.0F) breakSpeed = 1.0F;

        int ticksToBreak = (int) Math.ceil(hardness * 30 / breakSpeed);
        if (ticksToBreak < 1) ticksToBreak = 1;

        if (!targetPos.equals(currentBreakPos)) {
            breakProgress = 0;
            currentBreakPos = targetPos;
        }

        breakProgress += actionInterval;

        // 发送破坏进度
        int progress = (int) ((float) breakProgress / ticksToBreak * 10);
        world.sendBlockBreakProgress(fakePlayer.getEntityId(), targetPos, Math.min(progress, 9));

        if (breakProgress >= ticksToBreak) {
            // 破坏方块
            fakePlayer.simulateBlockBreak(targetPos);
            breakProgress = 0;
            currentBreakPos = null;

            // 清除破坏进度显示
            world.sendBlockBreakProgress(fakePlayer.getEntityId(), targetPos, -1);
            return true;
        }

        return false;
    }

    /**
     * 收集掉落物
     */
    private void collectDroppedItems(ModFakePlayer fakePlayer) {
        AxisAlignedBB collectBox = new AxisAlignedBB(pos).grow(3);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, collectBox);

        for (EntityItem entityItem : items) {
            if (entityItem.isDead) continue;

            ItemStack stack = entityItem.getItem();
            // 尝试放入库存
            for (int i = 1; i < 10; i++) {
                ItemStack slotStack = inventory.getStackInSlot(i);
                if (slotStack.isEmpty()) {
                    inventory.setStackInSlot(i, stack.copy());
                    entityItem.setDead();
                    break;
                } else if (ItemStack.areItemsEqual(slotStack, stack) &&
                           slotStack.getCount() < slotStack.getMaxStackSize()) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int toAdd = Math.min(space, stack.getCount());
                    slotStack.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        entityItem.setDead();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 获取或创建假玩家
     */
    @Nullable
    private ModFakePlayer getFakePlayer(ItemStack coreStack) {
        if (!(world instanceof WorldServer)) return null;

        ModFakePlayer fakePlayer = cachedFakePlayer.get();
        if (fakePlayer == null || fakePlayer.world != world) {
            GameProfile profile = ItemFakePlayerCore.getStoredProfile(coreStack);
            String playerName = ItemFakePlayerCore.getStoredPlayerName(coreStack);
            fakePlayer = new ModFakePlayer((WorldServer) world, profile, playerName);
            fakePlayer.setControllerPos(pos);
            cachedFakePlayer = new WeakReference<>(fakePlayer);
        }

        return fakePlayer;
    }

    /**
     * 设置激活状态
     */
    private void setActiveState(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            BlockFakePlayerActivator.setActiveState(world, pos, active);
        }
    }

    // ========== 区块加载 ==========

    /**
     * 启用/禁用区块加载
     */
    public void setChunkLoading(boolean enabled) {
        if (this.chunkLoadingEnabled != enabled) {
            this.chunkLoadingEnabled = enabled;
            if (enabled) {
                requestChunkLoad();
            } else {
                releaseChunkLoad();
            }
            markDirty();
            syncToClient();
        }
    }

    private void requestChunkLoad() {
        if (world == null || world.isRemote) return;
        if (chunkTicket != null) return;

        try {
            chunkTicket = ForgeChunkManager.requestTicket(
                net.minecraftforge.fml.common.Loader.instance().activeModContainer(),
                world, Type.NORMAL
            );
            if (chunkTicket != null) {
                chunkTicket.getModData().setInteger("x", pos.getX());
                chunkTicket.getModData().setInteger("y", pos.getY());
                chunkTicket.getModData().setInteger("z", pos.getZ());
                ForgeChunkManager.forceChunk(chunkTicket, new net.minecraft.util.math.ChunkPos(pos));
            }
        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to request chunk loading: " + e.getMessage());
        }
    }

    private void releaseChunkLoad() {
        if (chunkTicket != null) {
            ForgeChunkManager.releaseTicket(chunkTicket);
            chunkTicket = null;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        releaseChunkLoad();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        releaseChunkLoad();
    }

    // ========== GUI 配置方法 ==========

    public void cycleMode() {
        Mode[] modes = Mode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[nextIndex];
        markDirty();
        syncToClient();
    }

    public void setMode(int modeIndex) {
        if (modeIndex >= 0 && modeIndex < Mode.values().length) {
            currentMode = Mode.values()[modeIndex];
            markDirty();
            syncToClient();
        }
    }

    public void setActionInterval(int interval) {
        this.actionInterval = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, interval));
        markDirty();
        syncToClient();
    }

    public void adjustInterval(int delta) {
        setActionInterval(actionInterval + delta);
    }

    // ========== Getters ==========

    public Mode getCurrentMode() { return currentMode; }
    public int getActionInterval() { return actionInterval; }
    public boolean isChunkLoadingEnabled() { return chunkLoadingEnabled; }
    public boolean isActive() { return isActive; }
    public ItemStackHandler getInventory() { return inventory; }
    public IEnergyStorage getEnergyStorage() { return energyStorage; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergy() { return MAX_ENERGY; }

    // ========== 同步 ==========

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
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
    }

    // ========== NBT ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Mode", currentMode.ordinal());
        compound.setInteger("Interval", actionInterval);
        compound.setBoolean("ChunkLoading", chunkLoadingEnabled);
        compound.setInteger("ToolSlot", currentToolSlot);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        energyStorage.setEnergy(compound.getInteger("Energy"));
        int modeOrdinal = compound.getInteger("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < Mode.values().length) {
            currentMode = Mode.values()[modeOrdinal];
        }
        actionInterval = compound.getInteger("Interval");
        if (actionInterval < MIN_INTERVAL) actionInterval = DEFAULT_INTERVAL;
        chunkLoadingEnabled = compound.getBoolean("ChunkLoading");
        currentToolSlot = compound.getInteger("ToolSlot");
        if (currentToolSlot < 1 || currentToolSlot > 9) currentToolSlot = 1;
    }

    // ========== Capabilities ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityEnergy.ENERGY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return super.getCapability(capability, facing);
    }

    // ========== 内部能量存储 ==========

    private static class EnergyStorageInternal extends EnergyStorage {
        public EnergyStorageInternal(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.min(energy, this.capacity);
        }

        public void extractEnergyInternal(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }
}
