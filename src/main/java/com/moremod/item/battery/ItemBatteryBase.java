package com.moremod.item.battery;

import com.moremod.creativetab.moremodCreativeTab;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用电池基类：
 * - FE 能量存储（写入 ItemStack NBT）
 * - 背包/饰品都能被动工作：子类在 handleBatteryLogic 中扩展
 * - 耐久条显示电量、Tooltip 实时显示
 */
public abstract class ItemBatteryBase extends Item implements IBauble {

    protected final int tier;
    protected final int capacity;
    protected final int maxExtract;
    protected final int maxReceive;
    protected final String tierName;
    protected final TextFormatting tierColor;

    public ItemBatteryBase(String registryName,
                           int tier, int capacity, int maxExtract, int maxReceive,
                           String tierName, TextFormatting tierColor) {
        this.tier = tier;
        this.capacity = capacity;
        this.maxExtract = maxExtract;
        this.maxReceive = maxReceive;
        this.tierName = tierName;
        this.tierColor = tierColor;

        setRegistryName(registryName);
        setTranslationKey(registryName);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    public int getTier() { return tier; }

    // ========== Baubles ==========
    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.TRINKET;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase living) {
        if (!living.world.isRemote && living instanceof EntityPlayer) {
            handleBatteryLogic(stack, (EntityPlayer) living);
        }
    }

    // 背包内也工作
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {
        super.onUpdate(stack, world, entity, slot, isSelected);
        if (!world.isRemote && entity instanceof EntityPlayer) {
            handleBatteryLogic(stack, (EntityPlayer) entity);
        }
    }

    /**
     * 子类扩展被动逻辑（充别的物品、量子取电等）
     */
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) { }

    // ========== 能力：把能量存进 ItemStack 自己的 NBT ==========
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new BatteryEnergyProvider(stack, capacity, maxReceive, maxExtract);
    }

    public static IEnergyStorage getEnergy(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    // ========== 耐久条显示电量 ==========
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage es = getEnergy(stack);
        if (es == null || es.getMaxEnergyStored() <= 0) return 1.0;
        double ratio = (double) es.getEnergyStored() / es.getMaxEnergyStored();
        return 1.0 - ratio; // 满电→条短；低电→条长
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage es = getEnergy(stack);
        if (es == null || es.getMaxEnergyStored() <= 0) return 0x696969;
        float pct = (float) es.getEnergyStored() / es.getMaxEnergyStored(); // 0..1
        // 绿色→黄→红（HSB: 0.33..0）
        int rgb = java.awt.Color.HSBtoRGB(0.33f * pct, 1f, 1f);
        return rgb & 0xFFFFFF;
    }

    // ========== Tooltip ==========
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        IEnergyStorage es = getEnergy(stack);
        tip.add(tierColor + tierName + " 电池");
        if (es != null) {
            tip.add(TextFormatting.YELLOW + "能量: " + TextFormatting.WHITE +
                    fmt(es.getEnergyStored()) + " / " + fmt(es.getMaxEnergyStored()) + " RF");
            tip.add(TextFormatting.GREEN + "输入: " + TextFormatting.WHITE + fmt(maxReceive) + " RF/t"
                    + TextFormatting.GRAY + "  |  " +
                    TextFormatting.RED + "输出: " + TextFormatting.WHITE + fmt(maxExtract) + " RF/t");
        }
        addSpecialTooltip(stack, tip);

        if (!GuiScreen.isShiftKeyDown()) {
            tip.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多…");
        } else {
            addDetailedTooltip(stack, tip);
        }
    }

    protected void addSpecialTooltip(ItemStack stack, List<String> tip) { }
    protected void addDetailedTooltip(ItemStack stack, List<String> tip) { }

    // 右键查看电量
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            IEnergyStorage es = getEnergy(stack);
            if (es != null) {
                player.sendMessage(new TextComponentString(
                        tierColor + "[" + tierName + " 电池] " + TextFormatting.WHITE +
                                "能量: " + fmt(es.getEnergyStored()) + " / " + fmt(es.getMaxEnergyStored()) + " RF"));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    protected static String fmt(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000f);
        return String.valueOf(v);
    }

    // ========== Provider/Storage：将能量存在 Stack.NBT ==========
    public static class BatteryEnergyProvider implements ICapabilitySerializable<NBTTagCompound> {
        private final BatteryEnergyStorage storage;

        public BatteryEnergyProvider(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            this.storage = new BatteryEnergyStorage(stack, capacity, maxReceive, maxExtract);
        }
        @Override public boolean hasCapability(Capability<?> cap, EnumFacing f){ return cap==CapabilityEnergy.ENERGY; }
        @Override public <T> T getCapability(Capability<T> cap, EnumFacing f){
            return cap==CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
        }
        @Override public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", storage.getEnergyStored());
            return tag;
        }
        @Override public void deserializeNBT(NBTTagCompound nbt){ storage.setEnergy(nbt.getInteger("Energy")); }
    }

    public static class BatteryEnergyStorage implements IEnergyStorage {
        private static final String KEY = "Energy";
        private final ItemStack container;
        private final int capacity, maxReceive, maxExtract;

        public BatteryEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            this.container = stack;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            ensure();
        }

        private void ensure() {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            if (!container.getTagCompound().hasKey(KEY)) container.getTagCompound().setInteger(KEY, 0);
        }

        private int get() { ensure(); return container.getTagCompound().getInteger(KEY); }
        private void set(int v){ ensure(); container.getTagCompound().setInteger(KEY, Math.max(0, Math.min(capacity, v))); }

        @Override public int receiveEnergy(int mr, boolean sim){
            int stored = get();
            int rec = Math.min(capacity - stored, Math.min(maxReceive, mr));
            if (!sim && rec>0) set(stored + rec);
            return rec;
        }
        @Override public int extractEnergy(int me, boolean sim){
            int stored = get();
            int out = Math.min(stored, Math.min(maxExtract, me));
            if (!sim && out>0) set(stored - out);
            return out;
        }
        @Override public int getEnergyStored(){ return get(); }
        @Override public int getMaxEnergyStored(){ return capacity; }
        @Override public boolean canExtract(){ return maxExtract > 0; }
        @Override public boolean canReceive(){ return maxReceive > 0; }

        public void setEnergy(int v){ set(v); }
    }
}
