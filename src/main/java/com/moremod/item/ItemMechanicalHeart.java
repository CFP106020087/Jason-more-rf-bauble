package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.moremod.system.ascension.AscensionTooltips;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMechanicalHeart extends Item {

    private static final String NBT_ENERGY = "energy";
    private static final String NBT_LAST_ENERGY = "lastEnergy";
    private static final String NBT_REVIVE_MODE = "inPlaceRevive";
    private static final int MAX_ENERGY = 1000000;

    // 神秘消息数组
    private static final String[] MYSTICAL_MESSAGES = {
            TextFormatting.DARK_PURPLE + "时间的齿轮在心中转动...",
            TextFormatting.BLUE + "你感受到了永恒的脉搏...",
            TextFormatting.GRAY + "命运的丝线在你周围闪烁...",
            TextFormatting.AQUA + "时光在你的掌控之中...",
            TextFormatting.GOLD + "神秘的力量与你同在...",
            TextFormatting.LIGHT_PURPLE + "时间在你的意志下弯曲...",
            TextFormatting.DARK_AQUA + "永恒的秘密在你手中低语...",
            TextFormatting.YELLOW + "你听到了宇宙时钟的滴答声..."
    };

    public ItemMechanicalHeart(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            // 检查是否按住 Shift 键
            if (player.isSneaking()) {
                // 切换复活模式
                toggleReviveMode(stack);

                // 发送模式切换消息
                String currentMode = getReviveModeText(stack);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "时间之心复活模式已切换为：" + currentMode));

                // 播放切换音效
                world.playSound(null, player.getPosition(),
                        SoundEvents.UI_BUTTON_CLICK,
                        SoundCategory.PLAYERS, 0.5F, 1.2F);

                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            // 原来的右键逻辑
            IEnergyStorage energy = getEnergyStorage(stack);
            if (energy != null) {
                boolean isReady = energy.getEnergyStored() >= energy.getMaxEnergyStored();
                String status = isReady ? TextFormatting.LIGHT_PURPLE + "觉醒" : TextFormatting.GRAY + "沉睡";

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "神秘力量状态: " + status));
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "时间能量: " + energy.getEnergyStored() + "/" + energy.getMaxEnergyStored()));

                // 显示当前复活模式
                String modeText = getReviveModeText(stack);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "复活模式: " + modeText));

                if (!isReady) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GRAY + "需要汲取更多能量来唤醒时间之力"));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "神秘的力量正在守护着你..."));

                    // 随机显示神秘消息
                    if (world.rand.nextFloat() < 0.7f) {
                        String randomMessage = MYSTICAL_MESSAGES[world.rand.nextInt(MYSTICAL_MESSAGES.length)];
                        player.sendMessage(new TextComponentString(randomMessage));
                    }
                }
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            IEnergyStorage energy = getEnergyStorage(stack);
            if (energy != null) {
                int currentEnergy = energy.getEnergyStored();
                int lastEnergy = getLastEnergy(stack);

                // 检测满能量时刻
                if (currentEnergy >= MAX_ENERGY && lastEnergy < MAX_ENERGY) {
                    EntityPlayer player = (EntityPlayer) entity;
                    player.sendMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "时间之力已觉醒！神秘的力量开始守护你..."));
                }

                setLastEnergy(stack, currentEnergy);
            }
        }
        super.onUpdate(stack, world, entity, itemSlot, isSelected);
    }

    // 复活模式相关方法
    public static void toggleReviveMode(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        boolean currentMode = nbt.getBoolean(NBT_REVIVE_MODE);
        nbt.setBoolean(NBT_REVIVE_MODE, !currentMode);
    }

    public static boolean getReviveMode(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        // 默认为传送复活模式
        return nbt.getBoolean(NBT_REVIVE_MODE);
    }

    public static String getReviveModeText(ItemStack stack) {
        boolean isInPlace = getReviveMode(stack);
        return isInPlace ?
                TextFormatting.GREEN + "原地复活" :
                TextFormatting.LIGHT_PURPLE + "传送复活";
    }

    // NBT帮助方法
    private int getLastEnergy(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_LAST_ENERGY);
    }

    private void setLastEnergy(ItemStack stack, int energy) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_LAST_ENERGY, energy);
    }

    public static boolean isFullyCharged(ItemStack stack) {
        IEnergyStorage energy = getEnergyStorage(stack);
        return energy != null && energy.getEnergyStored() >= energy.getMaxEnergyStored();
    }

    public static boolean consumeAllEnergy(ItemStack stack) {
        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy != null && energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            energy.extractEnergy(energy.getMaxEnergyStored(), false);
            return true;
        }
        return false;
    }

    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new MechanicalHeartEnergyProvider(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy == null || energy.getMaxEnergyStored() == 0) return 1.0;
        return 1.0 - ((double) energy.getEnergyStored() / energy.getMaxEnergyStored());
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy != null && energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            // 根据复活模式显示不同颜色
            if (getReviveMode(stack)) {
                return 0x32CD32; // 绿色 - 原地复活已觉醒
            } else {
                return 0x9932CC; // 紫色 - 传送复活已觉醒
            }
        } else if (energy != null && energy.getEnergyStored() > 0) {
            return 0x4169E1; // 蓝色 - 充能中
        }
        return 0x696969; // 灰色 - 沉睡
    }

    // 华丽的发光效果
    @Override
    public boolean hasEffect(ItemStack stack) {
        // 满能量时持续发光
        if (isFullyCharged(stack)) {
            return true;
        } else {
            // 根据能量比例偶尔闪烁
            IEnergyStorage energy = getEnergyStorage(stack);
            if (energy != null) {
                float energyRatio = (float) energy.getEnergyStored() / energy.getMaxEnergyStored();
                return Math.random() < energyRatio * 0.3; // 能量越高闪烁概率越大
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy != null) {
            boolean isReady = energy.getEnergyStored() >= energy.getMaxEnergyStored();

            tooltip.add(TextFormatting.AQUA + "时间能量: " + energy.getEnergyStored() + " / " + energy.getMaxEnergyStored());

            if (isReady) {
                tooltip.add(TextFormatting.LIGHT_PURPLE + "状态: " + TextFormatting.BOLD + "觉醒");
                tooltip.add(TextFormatting.GOLD + "神秘的力量会在危机时回溯时间");
            } else {
                tooltip.add(TextFormatting.GRAY + "状态: " + TextFormatting.ITALIC + "沉睡");
                tooltip.add(TextFormatting.DARK_GRAY + "需要充满能量才能觉醒神秘力量");
            }

            // 显示当前复活模式
            String modeText = getReviveModeText(stack);
            tooltip.add(TextFormatting.GRAY + "复活模式: " + modeText);

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_PURPLE + "右键: 感知时间之力");
            tooltip.add(TextFormatting.YELLOW + "" + TextFormatting.ITALIC + "Shift + 右键: 切换复活模式");
            tooltip.add(TextFormatting.YELLOW + "将其携带在身，时间会庇护你");
            tooltip.add(TextFormatting.BLUE + "与神秘能量装置产生共鸣");
        }

        // ═══ 升格专属tooltip ═══
        AscensionTooltips.addHeartAscensionTooltip(tooltip);
    }

    // 能量存储实现类
    private static class MechanicalHeartEnergyProvider implements ICapabilityProvider {
        private final ItemStack container;
        private final MechanicalHeartEnergyStorage storage;

        public MechanicalHeartEnergyProvider(ItemStack stack) {
            this.container = stack;
            this.storage = new MechanicalHeartEnergyStorage(stack);
        }

        @Override
        public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Override
        @Nullable
        public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
        }
    }

    private static class MechanicalHeartEnergyStorage implements IEnergyStorage {
        private final ItemStack container;

        public MechanicalHeartEnergyStorage(ItemStack stack) {
            this.container = stack;
            initNBT();
        }

        private void initNBT() {
            if (!container.hasTagCompound()) {
                container.setTagCompound(new NBTTagCompound());
            }
            if (!container.getTagCompound().hasKey(NBT_ENERGY)) {
                container.getTagCompound().setInteger(NBT_ENERGY, 0);
            }
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            int received = Math.min(MAX_ENERGY - energy, maxReceive);
            if (!simulate && received > 0) {
                setEnergy(energy + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy, maxExtract);
            if (!simulate && extracted > 0) {
                setEnergy(energy - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return container.hasTagCompound() ? container.getTagCompound().getInteger(NBT_ENERGY) : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_ENERGY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private void setEnergy(int energy) {
            if (!container.hasTagCompound()) {
                container.setTagCompound(new NBTTagCompound());
            }
            container.getTagCompound().setInteger(NBT_ENERGY, Math.max(0, Math.min(MAX_ENERGY, energy)));
        }
    }
}