package com.moremod.tile;

import com.mojang.authlib.GameProfile;
import com.moremod.block.BlockFakePlayerActivator;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
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
 * 假玩家激活器 TileEntity
 * 使用假玩家核心模拟玩家操作
 */
public class TileEntityFakePlayerActivator extends TileEntity implements ITickable {

    // 操作模式
    public enum Mode {
        RIGHT_CLICK("右键点击", 50),      // 右键方块
        USE_ITEM("使用物品", 30),          // 使用手持物品（如骨粉）
        ATTACK("攻击生物", 100);           // 攻击前方生物

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
    private static final int MAX_ENERGY = 50000;
    private static final int COOLDOWN_TICKS = 10; // 0.5秒冷却

    // 当前模式
    private Mode currentMode = Mode.RIGHT_CLICK;

    // 能量存储
    private final EnergyStorageInternal energyStorage = new EnergyStorageInternal(MAX_ENERGY, 1000, 0);

    // 物品存储: 0=假玩家核心, 1=工具/物品
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof ItemFakePlayerCore;
            }
            return true; // 工具槽接受任何物品
        }
    };

    // 运行状态
    private int cooldown = 0;
    private boolean isActive = false;

    // 缓存的假玩家
    private WeakReference<ModFakePlayer> cachedFakePlayer = new WeakReference<>(null);

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 冷却计时
        if (cooldown > 0) {
            cooldown--;
            return;
        }

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
            // 消耗能量
            energyStorage.extractEnergyInternal(energyNeeded);
            // 消耗核心耐久
            coreStack.damageItem(1, getFakePlayer(coreStack));
            if (coreStack.isEmpty()) {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            // 增加使用次数
            ItemFakePlayerCore.incrementActivation(coreStack);
            setActiveState(true);
        }

        cooldown = COOLDOWN_TICKS;
        markDirty();
    }

    /**
     * 执行当前模式的操作
     */
    private boolean performAction(ItemStack coreStack) {
        ModFakePlayer fakePlayer = getFakePlayer(coreStack);
        if (fakePlayer == null) return false;

        // 获取朝向
        IBlockState state = world.getBlockState(pos);
        EnumFacing facing = state.getValue(BlockFakePlayerActivator.FACING);

        // 设置假玩家位置和朝向
        BlockPos targetPos = pos.offset(facing);
        fakePlayer.setLocationAndFacing(targetPos, facing);

        // 给假玩家装备工具
        ItemStack toolStack = inventory.getStackInSlot(1);
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, toolStack.copy());
        fakePlayer.updateEquipmentAttributes();
        fakePlayer.updateCooldown();

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
        }

        // 更新工具耐久
        ItemStack newTool = fakePlayer.getHeldItemMainhand();
        if (newTool.isEmpty() || newTool.getItemDamage() >= newTool.getMaxDamage()) {
            inventory.setStackInSlot(1, ItemStack.EMPTY);
        } else if (!ItemStack.areItemStacksEqual(toolStack, newTool)) {
            inventory.setStackInSlot(1, newTool);
        }

        // 收集假玩家产生的掉落物
        collectDroppedItems(fakePlayer);

        return success;
    }

    /**
     * 右键点击方块
     */
    private boolean performRightClick(ModFakePlayer fakePlayer, BlockPos targetPos, EnumFacing facing) {
        IBlockState targetState = world.getBlockState(targetPos);

        // 如果目标位置是空气，尝试放置方块
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

        // 右键点击方块
        try {
            boolean result = targetState.getBlock().onBlockActivated(
                world, targetPos, targetState, fakePlayer, EnumHand.MAIN_HAND,
                facing.getOpposite(), 0.5F, 0.5F, 0.5F
            );
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 使用物品（如骨粉）
     */
    private boolean performUseItem(ModFakePlayer fakePlayer, BlockPos targetPos, EnumFacing facing) {
        ItemStack tool = fakePlayer.getHeldItemMainhand();
        if (tool.isEmpty()) return false;

        // 尝试对方块使用物品
        EnumActionResult result = tool.getItem().onItemUse(
            fakePlayer, world, targetPos, EnumHand.MAIN_HAND,
            facing.getOpposite(), 0.5F, 0.5F, 0.5F
        );

        if (result == EnumActionResult.SUCCESS) {
            // 更新物品数量
            inventory.setStackInSlot(1, fakePlayer.getHeldItemMainhand());
            return true;
        }

        // 尝试右键使用物品
        tool.getItem().onItemRightClick(world, fakePlayer, EnumHand.MAIN_HAND);
        inventory.setStackInSlot(1, fakePlayer.getHeldItemMainhand());

        return true;
    }

    /**
     * 攻击前方生物
     */
    private boolean performAttack(ModFakePlayer fakePlayer, BlockPos targetPos) {
        // 搜索前方的生物
        AxisAlignedBB searchBox = new AxisAlignedBB(targetPos).grow(1.5);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
            EntityLivingBase.class, searchBox,
            e -> e != null && e.isEntityAlive() && !(e instanceof EntityPlayer)
        );

        if (entities.isEmpty()) return false;

        // 攻击最近的生物
        EntityLivingBase target = entities.get(0);
        fakePlayer.attackTargetEntityWithCurrentItem(target);

        // 攻击后立即清除周围生物对假玩家的仇恨
        com.moremod.fakeplayer.FakePlayerAggroHandler.clearAggroInArea(fakePlayer, 10.0);

        return true;
    }

    /**
     * 收集假玩家周围的掉落物
     */
    private void collectDroppedItems(ModFakePlayer fakePlayer) {
        AxisAlignedBB collectBox = new AxisAlignedBB(pos).grow(3);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, collectBox);

        for (EntityItem entityItem : items) {
            if (entityItem.isDead) continue;

            ItemStack stack = entityItem.getItem();
            // 尝试放入工具槽（如果是同类物品）
            ItemStack toolStack = inventory.getStackInSlot(1);
            if (!toolStack.isEmpty() && ItemStack.areItemsEqual(toolStack, stack)) {
                int space = toolStack.getMaxStackSize() - toolStack.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    toolStack.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        entityItem.setDead();
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

    /**
     * 切换模式
     */
    public void cycleMode() {
        Mode[] modes = Mode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[nextIndex];
        markDirty();
    }

    /**
     * 获取当前模式名称
     */
    public String getModeName() {
        return currentMode.getDisplayName();
    }

    /**
     * 显示状态信息
     */
    public void showStatus(EntityPlayer player) {
        ItemStack coreStack = inventory.getStackInSlot(0);
        ItemStack toolStack = inventory.getStackInSlot(1);

        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "═══ 假玩家激活器状态 ═══"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "模式: " + TextFormatting.WHITE + currentMode.getDisplayName() +
            TextFormatting.GRAY + " (潜行+空手切换)"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "能量: " + TextFormatting.GREEN + energyStorage.getEnergyStored() +
            TextFormatting.GRAY + " / " + MAX_ENERGY + " RF"
        ));

        if (!coreStack.isEmpty()) {
            String playerName = ItemFakePlayerCore.getStoredPlayerName(coreStack);
            int durability = coreStack.getMaxDamage() - coreStack.getItemDamage();
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "核心: " + TextFormatting.LIGHT_PURPLE + playerName +
                TextFormatting.GRAY + " (耐久: " + durability + ")"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "未安装假玩家核心!"
            ));
        }

        if (!toolStack.isEmpty()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "工具: " + TextFormatting.WHITE + toolStack.getDisplayName() +
                " x" + toolStack.getCount()
            ));
        }

        player.sendMessage(new TextComponentString(
            TextFormatting.YELLOW + "需要红石信号激活"
        ));
    }

    // === NBT ===

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Mode", currentMode.ordinal());
        compound.setInteger("Cooldown", cooldown);
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
        cooldown = compound.getInteger("Cooldown");
    }

    // === Capabilities ===

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
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

    // === Getters ===

    public ItemStackHandler getInventory() { return inventory; }
    public IEnergyStorage getEnergyStorage() { return energyStorage; }
    public Mode getCurrentMode() { return currentMode; }
    public boolean isActive() { return isActive; }

    // === 内部能量存储类 ===

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
