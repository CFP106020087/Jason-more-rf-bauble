// ItemJetpackBauble.java
package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;  // 添加导入
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemJetpackBauble extends Item implements IBauble {

    // 速度模式枚举（类似创造喷气背包）
    public enum SpeedMode {
        NORMAL("标准", 1.0),
        FAST("快速", 1.5),
        ULTRA("极速", 2.0);

        private final String name;
        private final double multiplier;

        SpeedMode(String name, double multiplier) {
            this.name = name;
            this.multiplier = multiplier;
        }

        public String getName() { return name; }
        public double getMultiplier() { return multiplier; }
    }
    private final String name;
    private final int maxEnergy;
    private final int energyPerTick;
    private final double ascendSpeed;
    private final double descendSpeed;
    private final double moveSpeed;
    private final int tier; // 添加等级属性

    public ItemJetpackBauble(String name, int maxEnergy, int energyPerTick,
                             double ascendSpeed, double descendSpeed, double moveSpeed) {
        this.name = name;
        this.maxEnergy = maxEnergy;
        this.energyPerTick = energyPerTick;
        this.ascendSpeed = ascendSpeed;
        this.descendSpeed = descendSpeed;
        this.moveSpeed = moveSpeed;

        // 根据名称判断等级
        if (name.contains("basic") || name.contains("t1") || name.contains("tier1")) {
            this.tier = 1;
        } else if (name.contains("advanced") || name.contains("t2") || name.contains("tier2")) {
            this.tier = 2;
        } else if (name.contains("ultimate") || name.contains("elite") || name.contains("t3") || name.contains("tier3")) {
            this.tier = 3;
        } else {
            // 根据能量消耗推测
            this.tier = energyPerTick <= 50 ? 1 : energyPerTick <= 100 ? 2 : 3;
        }

        setRegistryName(name);
        setTranslationKey(name);
        setMaxStackSize(1);
        setMaxDamage(100);
        setCreativeTab(moremodCreativeTab.moremod_TAB);

    }

    public int getMaxEnergy() { return maxEnergy; }
    public int getEnergyPerTick() { return energyPerTick; }
    public double getAscendSpeed() { return ascendSpeed; }
    public double getDescendSpeed() { return descendSpeed; }
    public double getMoveSpeed() { return moveSpeed; }
    public int getTier() { return tier; }

    // 获取当前速度模式
    public SpeedMode getSpeedMode(ItemStack stack) {
        if (tier < 3) return SpeedMode.NORMAL; // 只有T3能切换速度

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey("SpeedMode")) {
            return SpeedMode.NORMAL;
        }
        int mode = nbt.getInteger("SpeedMode");
        return SpeedMode.values()[Math.min(mode, SpeedMode.values().length - 1)];
    }

    // 设置速度模式
    public void setSpeedMode(ItemStack stack, SpeedMode mode) {
        if (tier < 3) return; // 只有T3能切换速度

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setInteger("SpeedMode", mode.ordinal());
    }

    // 切换到下一个速度模式
    public void nextSpeedMode(ItemStack stack, EntityPlayer player) {
        if (tier < 3) {
            if (player != null && !player.world.isRemote) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "需要终极喷气背包才能切换速度模式！"
                ));
            }
            return;
        }

        SpeedMode current = getSpeedMode(stack);
        SpeedMode next = SpeedMode.values()[(current.ordinal() + 1) % SpeedMode.values().length];
        setSpeedMode(stack, next);

        if (player != null && !player.world.isRemote) {
            TextFormatting color = TextFormatting.YELLOW;
            if (next == SpeedMode.FAST) color = TextFormatting.GOLD;
            if (next == SpeedMode.ULTRA) color = TextFormatting.LIGHT_PURPLE;

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "速度模式: " + color + next.getName()
            ));

            // 播放音效
            player.world.playSound(null, player.getPosition(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.5F, 1.0F + next.ordinal() * 0.2F);
        }
    }

    // 获取实际速度（应用速度模式倍率）
    public double getActualAscendSpeed(ItemStack stack) {
        SpeedMode mode = getSpeedMode(stack);
        return ascendSpeed * mode.getMultiplier();
    }

    public double getActualDescendSpeed(ItemStack stack) {
        SpeedMode mode = getSpeedMode(stack);
        return descendSpeed * mode.getMultiplier();
    }

    public double getActualMoveSpeed(ItemStack stack) {
        SpeedMode mode = getSpeedMode(stack);
        return moveSpeed * mode.getMultiplier();
    }

    // 获取实际能量消耗（速度模式会增加消耗）
    public int getActualEnergyPerTick(ItemStack stack) {
        SpeedMode mode = getSpeedMode(stack);
        return (int)(energyPerTick * mode.getMultiplier());
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 检查是否是玩家
        if (!(player instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer entityPlayer = (EntityPlayer) player;

        // ✨ 修改：所有等级的喷气背包都需要机械核心
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(entityPlayer);
            if (baubles != null) {
                boolean hasMechanicalCore = false;

                // 遍历所有饰品栏位
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof ItemMechanicalCore) {
                        hasMechanicalCore = true;
                        break;
                    }
                }

                if (!hasMechanicalCore) {
                    // 如果没有装备机械核心，发送提示消息
                    if (!entityPlayer.world.isRemote) {
                        String tierName = tier == 1 ? "基础" : tier == 2 ? "高级" : "终极";
                        entityPlayer.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.RED + "✗ " + tierName + "喷气背包需要先装备机械核心！"
                                ), true);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            // 如果出现异常，默认不允许装备
            return false;
        }

        // 已装备机械核心，允许装备
        return true;
    }
    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        // 初始化NBT，包含等级信息
        if (!stack.hasTagCompound()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("jetpackTier", tier);
            stack.setTagCompound(tag);
        } else {
            stack.getTagCompound().setInteger("jetpackTier", tier);
        }

        return new ICapabilitySerializable<NBTTagCompound>() {
            private final EnergyStorageInternal storage = new EnergyStorageInternal(stack, maxEnergy);

            @Override
            public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
            }

            @Override
            public NBTTagCompound serializeNBT() {
                return storage.serializeNBT();
            }

            @Override
            public void deserializeNBT(NBTTagCompound nbt) {
                storage.deserializeNBT(nbt);
            }
        };
    }

    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage == null || storage.getMaxEnergyStored() == 0) return 1.0;
        return 1.0 - ((double) storage.getEnergyStored() / storage.getMaxEnergyStored());
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        double durability = 1.0 - getDurabilityForDisplay(stack);
        return MathHelper.hsvToRGB((float)(durability / 3.0F), 1.0F, 1.0F);
    }

    // 🎯 新增：获取实际能量消耗（用于显示）
    public static int getActualConsumption(EntityPlayer player, int originalCost) {
        return player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, originalCost) :
                originalCost;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        IEnergyStorage storage = getEnergyStorage(stack);

        // 获取玩家和实际消耗
        EntityPlayer player = Minecraft.getMinecraft().player;
        int baseEnergyUse = getActualEnergyPerTick(stack);
        int actualCost = getActualConsumption(player, baseEnergyUse);

        // 基础信息
        tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + getDisplayName());
        tooltip.add(TextFormatting.GRAY + "等级: " + getTierColor() + "T" + tier);

        // T3显示速度模式
        if (tier >= 3) {
            SpeedMode mode = getSpeedMode(stack);
            TextFormatting modeColor = TextFormatting.YELLOW;
            if (mode == SpeedMode.FAST) modeColor = TextFormatting.GOLD;
            if (mode == SpeedMode.ULTRA) modeColor = TextFormatting.LIGHT_PURPLE;
            tooltip.add(TextFormatting.GRAY + "速度模式: " + modeColor + mode.getName());
        }

        tooltip.add("");

        if (storage != null) {
            int energy = storage.getEnergyStored();
            int maxEnergy = storage.getMaxEnergyStored();
            double percent = (double) energy / maxEnergy * 100;

            // 能量条
            TextFormatting color = percent > 60 ? TextFormatting.GREEN :
                    percent > 30 ? TextFormatting.YELLOW :
                            percent > 10 ? TextFormatting.GOLD : TextFormatting.RED;

            tooltip.add(TextFormatting.AQUA + "能量: " + color +
                    String.format("%,d", energy) + " / " + String.format("%,d", maxEnergy) + " FE");
            tooltip.add(TextFormatting.GRAY + "电量: " + color + String.format("%.1f%%", percent));

            // 飞行时间
            if (actualCost > 0) {
                int ticksRemaining = energy / actualCost;
                int secondsRemaining = ticksRemaining / 20;
                if (secondsRemaining > 60) {
                    int minutes = secondsRemaining / 60;
                    int seconds = secondsRemaining % 60;
                    tooltip.add(TextFormatting.GRAY + "飞行时间: " + color + minutes + "分" + seconds + "秒");
                } else {
                    tooltip.add(TextFormatting.GRAY + "飞行时间: " + color + secondsRemaining + "秒");
                }
            }
        }

        tooltip.add("");

        // 性能参数（显示实际值）
        tooltip.add(TextFormatting.YELLOW + "性能参数:");
        tooltip.add(TextFormatting.GRAY + "• 上升速度: " + TextFormatting.WHITE +
                String.format("%.1f", getActualAscendSpeed(stack) * 20) + " m/s");
        tooltip.add(TextFormatting.GRAY + "• 下降速度: " + TextFormatting.WHITE +
                String.format("%.1f", getActualDescendSpeed(stack) * 20) + " m/s");
        tooltip.add(TextFormatting.GRAY + "• 移动速度: " + TextFormatting.WHITE +
                String.format("%.1f", getActualMoveSpeed(stack) * 20) + " m/s");

        // 能量消耗（显示实际值）
        if (actualCost < baseEnergyUse) {
            tooltip.add(TextFormatting.GREEN + "• 能量消耗: " + actualCost + " FE/tick (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "• 能量消耗: " + actualCost + " FE/tick");
        }

        // 功能提示
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "功能特性:");
        if (tier >= 1) {
            tooltip.add(TextFormatting.GRAY + "• 基础飞行 (空格上升/Shift下降)");
        }
        if (tier >= 2) {
            tooltip.add(TextFormatting.YELLOW + "• 悬停模式 (按H切换)");
        }
        if (tier >= 3) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "• 速度切换 (按G切换)");
        }

        // 能量效率支持
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.AQUA + "使用说明:");
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.moremod.jetpack.instructions"));

        // 启用状态
        NBTTagCompound nbt = stack.getTagCompound();
        boolean enabled = nbt != null && nbt.getBoolean("JetpackEnabled");
        boolean hover = nbt != null && nbt.getBoolean("HoverEnabled");

        if (enabled) {
            tooltip.add(TextFormatting.GREEN + "✓ 喷气背包已启用");
            if (hover && tier >= 2) {
                tooltip.add(TextFormatting.AQUA + "✓ 悬停模式激活");
            }
        } else {
            tooltip.add(TextFormatting.RED + "✗ 喷气背包已禁用");
        }

        // Shift详细信息
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "=== 详细信息 ===");

            // 显示当前效率
            if (player != null && actualCost < baseEnergyUse) {
                int saved = baseEnergyUse - actualCost;
                int efficiencyPercentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                tooltip.add(TextFormatting.GREEN + "当前效率加成: " + efficiencyPercentage + "%");
                tooltip.add(TextFormatting.GREEN + "每tick节省: " + saved + " FE");

                // 计算额外飞行时间
                if (storage != null && storage.getEnergyStored() > 0) {
                    int normalTime = storage.getEnergyStored() / baseEnergyUse / 20;
                    int efficientTime = storage.getEnergyStored() / actualCost / 20;
                    int extraTime = efficientTime - normalTime;
                    if (extraTime > 0) {
                        tooltip.add(TextFormatting.GREEN + "额外飞行时间: +" + extraTime + "秒");
                    }
                }
            } else {
                tooltip.add(TextFormatting.GRAY + "当前效率加成: 0%");
                tooltip.add(TextFormatting.DARK_GRAY + "装备机械核心可减少能量消耗");
            }

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "操作方式:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 空格键 - 上升");
            tooltip.add(TextFormatting.DARK_GRAY + "• Shift键 - 下降");
            tooltip.add(TextFormatting.DARK_GRAY + "• 按V键切换开关");
            if (tier >= 2) {
                tooltip.add(TextFormatting.DARK_GRAY + "• 按H键切换悬停");
            }
            if (tier >= 3) {
                tooltip.add(TextFormatting.DARK_GRAY + "• 按G键切换速度模式");
            }

            // 根据型号显示特色
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "喷气背包特性:");
            if (tier == 1) {
                tooltip.add(TextFormatting.GRAY + "• 基础型号，适合短距离飞行");
                tooltip.add(TextFormatting.GRAY + "• 能量容量较小，需频繁充电");
                tooltip.add(TextFormatting.RED + "• 不支持悬停和速度切换");
            } else if (tier == 2) {
                tooltip.add(TextFormatting.YELLOW + "• 高级型号，性能均衡");
                tooltip.add(TextFormatting.YELLOW + "• 适合中长距离探索");
                tooltip.add(TextFormatting.YELLOW + "• 支持悬停模式");
            } else if (tier >= 3) {
                tooltip.add(TextFormatting.LIGHT_PURPLE + "• 精英型号，顶级性能");
                tooltip.add(TextFormatting.LIGHT_PURPLE + "• 超大容量，持久续航");
                tooltip.add(TextFormatting.LIGHT_PURPLE + "• 支持全部飞行功能");
            }
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }
    }

    // 右键显示状态信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote && player.isSneaking()) {
            IEnergyStorage storage = getEnergyStorage(stack);
            if (storage != null) {
                int energy = storage.getEnergyStored();
                int maxEnergy = storage.getMaxEnergyStored();
                double percent = (double) energy / maxEnergy * 100;
                int baseEnergyUse = getActualEnergyPerTick(stack);
                int actualCost = getActualConsumption(player, baseEnergyUse);

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "=== 喷气背包状态 (T" + tier + ") ==="
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "当前能量：" + String.format("%,d", energy) + "/" +
                                String.format("%,d", maxEnergy) + " RF (" +
                                String.format("%.1f", percent) + "%)"
                ));

                // 飞行时间
                if (actualCost > 0 && energy >= actualCost) {
                    int secondsRemaining = energy / actualCost / 20;
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ 可飞行时间：" + secondsRemaining + "秒"
                    ));

                    // 显示效率信息
                    if (actualCost < baseEnergyUse) {
                        int saved = baseEnergyUse - actualCost;
                        int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                        player.sendMessage(new TextComponentString(
                                TextFormatting.GREEN + "⚡ 能量效率加成: " + percentage + "% (每tick节省 " + saved + " FE)"
                        ));
                    }
                } else if (energy < actualCost) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "✗ 能量不足，无法飞行"
                    ));
                }

                // 启用状态
                NBTTagCompound nbt = stack.getTagCompound();
                boolean enabled = nbt != null && nbt.getBoolean("JetpackEnabled");
                boolean hover = nbt != null && nbt.getBoolean("HoverEnabled");

                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "喷气背包：" +
                                (enabled ? TextFormatting.GREEN + "已启用" : TextFormatting.RED + "已禁用")
                ));

                if (tier >= 2) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "悬停模式：" +
                                    (hover ? TextFormatting.AQUA + "已激活" : TextFormatting.GRAY + "未激活")
                    ));
                }

                if (tier >= 3) {
                    SpeedMode mode = getSpeedMode(stack);
                    TextFormatting modeColor = TextFormatting.YELLOW;
                    if (mode == SpeedMode.FAST) modeColor = TextFormatting.GOLD;
                    if (mode == SpeedMode.ULTRA) modeColor = TextFormatting.LIGHT_PURPLE;

                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "速度模式：" + modeColor + mode.getName()
                    ));
                }

                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "实际消耗：" + TextFormatting.YELLOW + actualCost + " RF/tick" +
                                (actualCost < baseEnergyUse ? TextFormatting.GREEN + " (效率加成)" : "")
                ));
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    private String getDisplayName() {
        if (tier == 1) return "基础喷气背包";
        if (tier == 2) return "高级喷气背包";
        if (tier >= 3) return "终极喷气背包";
        return "喷气背包";
    }

    private TextFormatting getTierColor() {
        if (tier == 1) return TextFormatting.GRAY;
        if (tier == 2) return TextFormatting.YELLOW;
        if (tier >= 3) return TextFormatting.LIGHT_PURPLE;
        return TextFormatting.WHITE;
    }

    private static class EnergyStorageInternal implements IEnergyStorage {
        private final ItemStack container;
        private final int capacity;

        public EnergyStorageInternal(ItemStack stack, int capacity) {
            this.container = stack;
            this.capacity = capacity;
            initNBT();
        }

        private void initNBT() {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            if (!container.getTagCompound().hasKey("Energy")) container.getTagCompound().setInteger("Energy", 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            int received = Math.min(capacity - energy, maxReceive);
            if (!simulate) setEnergy(energy + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy, maxExtract);
            if (!simulate) setEnergy(energy - extracted);
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return container.hasTagCompound() ? container.getTagCompound().getInteger("Energy") : 0;
        }

        public void setEnergy(int value) {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            container.getTagCompound().setInteger("Energy", Math.max(0, Math.min(capacity, value)));
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", getEnergyStored());
            return tag;
        }

        public void deserializeNBT(NBTTagCompound nbt) {
            setEnergy(nbt.getInteger("Energy"));
        }
    }

    // 创造模式充能
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            IEnergyStorage energy = getEnergyStorage(stack);
            if (energy != null && energy instanceof EnergyStorageInternal) {
                ((EnergyStorageInternal) energy).setEnergy(maxEnergy);
            }
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean("JetpackEnabled");
    }
}