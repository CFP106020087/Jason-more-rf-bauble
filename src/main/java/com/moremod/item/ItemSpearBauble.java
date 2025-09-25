package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import cofh.redstoneflux.api.IEnergyContainerItem;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;  // 添加导入
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
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

public class ItemSpearBauble extends Item implements IBauble, IEnergyContainerItem {

    public static final int MAX_ENERGY = 100000;
    public static final int COST_PER_ATTACK = 0;  // 普通攻击无消耗
    public static final int COST_PER_TRIGGER = 2000;  // 特殊技能原始消耗

    public ItemSpearBauble() {
        setTranslationKey("spear_bauble");
        setRegistryName("spear_bauble");
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET; // 默认类型，通过 canEquip 允许任意槽位
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true; // 允许装备到任意槽位
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true; // 允许从任意槽位卸下
    }

    // ===== 🎯 修改：支持能量效率的方法 =====
    public static boolean tryUseRing(EntityPlayer player) {
        return tryUseRingWithCost(player, COST_PER_ATTACK);
    }

    public static boolean tryUseRingForTrigger(EntityPlayer player) {
        return tryUseRingWithCost(player, COST_PER_TRIGGER);
    }

    private static boolean tryUseRingWithCost(EntityPlayer player, int originalCost) {
        // 计算实际消耗
        int actualCost = originalCost;
        if (originalCost > 0) {  // 只有非零消耗才计算效率
            actualCost = EnergyEfficiencyManager.calculateActualCost(player, originalCost);
        }

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemSpearBauble) {
                IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (energy != null && energy.extractEnergy(actualCost, true) >= actualCost) {
                    energy.extractEnergy(actualCost, false);

                    // 显示节省提示（仅对特殊技能）
                    if (!player.world.isRemote && originalCost > 0 && actualCost < originalCost) {
                        int saved = originalCost - actualCost;
                        int percentage = (int)((saved / (float)originalCost) * 100);
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.GREEN + "⚡ 银枪共鸣: 节省 " + percentage + "% 能量"
                        ), true);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    public static int getStoredFromBaubles(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemSpearBauble) {
                IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                return energy != null ? energy.getEnergyStored() : 0;
            }
        }
        return 0;
    }

    // ===== 🎯 新增：获取实际特殊技能消耗（用于显示） =====
    public static int getActualTriggerCost(EntityPlayer player) {
        return player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, COST_PER_TRIGGER) :
                COST_PER_TRIGGER;
    }

    // ===== 保持原有的 IEnergyContainerItem 实现（向后兼容） =====
    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int stored = getEnergyStored(stack);
        int accepted = Math.min(MAX_ENERGY - stored, maxReceive);
        if (!simulate) {
            setEnergyStored(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int stored = getEnergyStored(stack);
        int extracted = Math.min(stored, maxExtract);
        if (!simulate) {
            setEnergyStored(stack, stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = getOrCreateEnergyTag(stack);
        return tag.getInteger("rf");
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return MAX_ENERGY;
    }

    private void setEnergyStored(ItemStack stack, int amount) {
        getOrCreateEnergyTag(stack).setInteger("rf", Math.max(0, Math.min(MAX_ENERGY, amount)));
    }

    private NBTTagCompound getOrCreateEnergyTag(ItemStack stack) {
        NBTTagCompound nbt = stack.getOrCreateSubCompound("Energy");
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagInfo("Energy", nbt);
        }
        return nbt;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - ((double) getEnergyStored(stack) / MAX_ENERGY);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        double durability = 1.0 - getDurabilityForDisplay(stack);
        return MathHelper.hsvToRGB((float)(durability / 3.0F), 1.0F, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);
        double energyPercent = (double) energy / MAX_ENERGY * 100;

        // 🎯 获取实际消耗
        EntityPlayer player = Minecraft.getMinecraft().player;
        int actualTriggerCost = getActualTriggerCost(player);

        // 物品标题
        tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + "龙胆银枪·虚影");
        tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Phantom Spear of Gentian Silver");
        tooltip.add("");

        // 史诗传说描述
        tooltip.add(TextFormatting.DARK_PURPLE + "这把长枪曾是一把赝品，是对一位无畏英雄");
        tooltip.add(TextFormatting.DARK_PURPLE + "锐利兵器的拙劣模仿。虽然是依照科技工艺");
        tooltip.add(TextFormatting.DARK_PURPLE + "创造出的模拟神器，却依旧能够穿透敌人");
        tooltip.add(TextFormatting.DARK_PURPLE + "最坚固的防护，直刺生物的心脏要害。");
        tooltip.add("");

        // 能量状态与武器状态
        if (energyPercent > 80) {
            tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "⚡ 子龙威名再现");
            tooltip.add(TextFormatting.AQUA + "银枪寒光如雪，似有龙吟之声");
            tooltip.add(TextFormatting.GREEN + "常山赵子龙的胆魄正在觉醒...");
        } else if (energyPercent > 60) {
            tooltip.add(TextFormatting.GREEN + "" + TextFormatting.BOLD + "⚡ 长坂英姿犹存");
            tooltip.add(TextFormatting.GREEN + "枪尖闪烁，仿佛重现当年风采");
            tooltip.add(TextFormatting.YELLOW + "忠义之心开始共鸣");
        } else if (energyPercent > 40) {
            tooltip.add(TextFormatting.YELLOW + "" + TextFormatting.BOLD + "⚡ 虎威渐趋黯淡");
            tooltip.add(TextFormatting.YELLOW + "昔日的无双神勇正在消散");
            tooltip.add(TextFormatting.GOLD + "需要更多力量重塑传奇");
        } else if (energyPercent > 20) {
            tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + "⚡ 将军魂魄微弱");
            tooltip.add(TextFormatting.GOLD + "仅存微弱的常山回响");
            tooltip.add(TextFormatting.RED + "赝品的本质开始显露");
        } else if (energyPercent > 0) {
            tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "⚡ 英雄末路将至");
            tooltip.add(TextFormatting.RED + "子龙的虚影摇摇欲坠");
            tooltip.add(TextFormatting.DARK_RED + "七进七出的传说即将消散");
        } else {
            tooltip.add(TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "⚡ 将星已然陨落");
            tooltip.add(TextFormatting.DARK_RED + "只剩下一把普通的仿制品");
            tooltip.add(TextFormatting.DARK_GRAY + "连传说也无法点燃死灰");
        }

        tooltip.add("");

        // 数值信息
        tooltip.add(TextFormatting.YELLOW + "储存能量: " + String.format("%,d", energy) + " / " + String.format("%,d", MAX_ENERGY) + " RF");
        tooltip.add(TextFormatting.GRAY + "普通攻击: " + COST_PER_ATTACK + " RF (无消耗)");

        // 根据是否有效率加成显示不同颜色
        if (actualTriggerCost < COST_PER_TRIGGER) {
            tooltip.add(TextFormatting.GREEN + "特殊技能: " + actualTriggerCost + " RF (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "特殊技能: " + actualTriggerCost + " RF");
        }

        // 能量效率支持提示
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");
        tooltip.add("");

        // 技能状态
        if (energy >= actualTriggerCost) {
            tooltip.add(TextFormatting.GREEN + "✦ 百鸟朝凤枪: 就绪");
            tooltip.add(TextFormatting.GRAY + "子龙绝技已准备好展现致命威力");
        } else {
            tooltip.add(TextFormatting.RED + "✦ 百鸟朝凤枪: 未就绪");
            tooltip.add(TextFormatting.GRAY + "需要 " + actualTriggerCost + " RF 重现当年神技");
        }

        // Shift显示详细信息
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "=== 详细信息 ===");

            // 显示当前效率
            if (player != null && actualTriggerCost < COST_PER_TRIGGER) {
                int saved = COST_PER_TRIGGER - actualTriggerCost;
                int efficiencyPercentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                tooltip.add(TextFormatting.GREEN + "当前效率加成: " + efficiencyPercentage + "%");
                tooltip.add(TextFormatting.GREEN + "每次技能节省: " + saved + " RF");

                // 计算可使用次数
                int normalUses = energy / COST_PER_TRIGGER;
                int efficientUses = energy / actualTriggerCost;
                if (efficientUses > normalUses) {
                    tooltip.add(TextFormatting.GREEN + "额外技能次数: +" + (efficientUses - normalUses));
                }
            } else {
                tooltip.add(TextFormatting.GRAY + "当前效率加成: 0%");
                tooltip.add(TextFormatting.DARK_GRAY + "装备机械核心可减少技能消耗");
            }

            // 显示可使用次数
            int skillsLeft = energy / actualTriggerCost;
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "剩余技能次数: " + skillsLeft);

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "武器特性:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 普通攻击无需消耗能量");
            tooltip.add(TextFormatting.DARK_GRAY + "• 特殊技能触发致命一击");
            tooltip.add(TextFormatting.DARK_GRAY + "• 穿透一切护甲防御");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }

        tooltip.add("");

        // 传说引语
        if (energyPercent > 50) {
            tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_AQUA + "\"纵横天下三十秋，");
            tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_AQUA + " 常山赵子龙威名传千古。\"");
        } else {
            tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_GRAY + "\"七进七出展雄风，");
            tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_GRAY + " 忠义双全护幼主...\"");
        }
    }

    // 右键显示状态信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;
            int actualTriggerCost = getActualTriggerCost(player);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "=== 龙胆银枪·虚影状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + String.format("%,d", energy) + "/" +
                            String.format("%,d", MAX_ENERGY) + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            // 技能状态
            if (energy >= actualTriggerCost) {
                int skillsLeft = energy / actualTriggerCost;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 百鸟朝凤枪就绪，可使用 " + skillsLeft + " 次"
                ));

                // 显示效率信息
                if (actualTriggerCost < COST_PER_TRIGGER) {
                    int saved = COST_PER_TRIGGER - actualTriggerCost;
                    int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 能量效率加成: " + percentage + "% (每次节省 " + saved + " RF)"
                    ));
                }
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 能量不足，无法使用百鸟朝凤枪"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "需要至少 " + actualTriggerCost + " RF" +
                                (actualTriggerCost < COST_PER_TRIGGER ? TextFormatting.GREEN + " (已优化)" : "")
                ));
            }

            // 能量状态提醒
            if (energyPercent < 20) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量严重不足，英雄之魂即将消散！"
                ));
            } else if (energyPercent < 40) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                ));
            } else if (energyPercent >= 80) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GREEN + "✓ 银枪威力充沛，子龙英灵护佑"
                ));
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "武器类型：" + TextFormatting.GOLD + "传说级能量兵器"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "技能消耗：" + TextFormatting.YELLOW + actualTriggerCost + " RF/次" +
                            (actualTriggerCost < COST_PER_TRIGGER ? TextFormatting.GREEN + " (效率加成)" : "")
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "特性：普通攻击无消耗，特殊技能穿透防御"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderEnergy(stack);
    }

    private static class CapabilityProviderEnergy implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderEnergy(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    return ((IEnergyContainerItem) stack.getItem()).receiveEnergy(stack, maxReceive, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return ((IEnergyContainerItem) stack.getItem()).extractEnergy(stack, maxExtract, simulate);
                }

                @Override
                public int getEnergyStored() {
                    return ((IEnergyContainerItem) stack.getItem()).getEnergyStored(stack);
                }

                @Override
                public int getMaxEnergyStored() {
                    return ((IEnergyContainerItem) stack.getItem()).getMaxEnergyStored(stack);
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? (T) wrapper : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return new NBTTagCompound();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
        }
    }

    // 创造模式充能
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            setEnergyStored(stack, MAX_ENERGY);
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }
}