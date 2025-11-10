package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.shields.integrated.IntegratedShieldSystem;
import com.moremod.upgrades.EnergyEfficiencyManager;  // 添加import
import net.minecraft.client.Minecraft;  // 添加import（用于tooltip）
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
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

public class ItemadvEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 300000;
    public static final int COST_PER_BLOCK = 1000;

    public ItemadvEnergyBarrier() {
        setRegistryName("adv_energy_barrier");
        setTranslationKey("adv_energy_barrier");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        // 返回 TRINKET 类型，可以放在任意饰品槽位
        return BaubleType.TRINKET;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 检查是否是玩家
        if (!(player instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer entityPlayer = (EntityPlayer) player;

        // 检查是否已装备机械核心
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
                        entityPlayer.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.RED + "✗ 需要先装备机械核心才能使用高级能量护盾！"
                                ), true);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            // 如果出现异常，默认不允许装备
            return false;
        }

        // 允许装备在任何饰品槽位
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        // 允许从任何饰品槽位卸下
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被佩戴时每tick调用一次
        // 这里可以添加一些佩戴时的特殊效果
        // 目前保持为空，护盾功能在事件处理器中处理
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被装备时调用
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.BLUE + "[高级护盾] " +
                                    TextFormatting.GREEN + "已激活！短冷却100%格挡攻击"
                    ), true);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被卸下时调用
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GRAY + "[高级护盾] " +
                                    TextFormatting.RED + "已停用"
                    ), true);
        }
    }

    public static int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getInteger("Energy") : 0;
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Energy", Math.max(0, Math.min(MAX_ENERGY, amount)));
    }

    // ===== 修改：使用 IEnergyStorage 消耗能量（保持不变，因为这个方法已经废弃） =====
    public boolean consumeEnergy(ItemStack stack, int amount) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null && energy.extractEnergy(amount, true) >= amount) {
            energy.extractEnergy(amount, false);  // 可以被效率系统拦截
            return true;
        }
        return false;
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
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);

        // 获取实际消耗
        EntityPlayer player = Minecraft.getMinecraft().player;
        int actualCost = player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, COST_PER_BLOCK) : COST_PER_BLOCK;

        // 基础信息
        tooltip.add(TextFormatting.YELLOW + "能量：" + energy + " / " + MAX_ENERGY + " RF");
        tooltip.add(TextFormatting.GREEN + "激活时：免疫任意伤害");
        tooltip.add(TextFormatting.GREEN + "激活冷却：5秒（最短）");
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");
        tooltip.add(TextFormatting.GREEN + "可放置在任意饰品槽位");

        tooltip.add("");

        // 高级产品描述
        tooltip.add(TextFormatting.LIGHT_PURPLE + "「 " + TextFormatting.BOLD + "高级能量护盾" + TextFormatting.RESET + TextFormatting.LIGHT_PURPLE + " 」");
        tooltip.add(TextFormatting.GRAY + "人类科技才华的极限体现");
        tooltip.add(TextFormatting.GRAY + "尚未超越天才之境，但已臻于完美");

        tooltip.add("");

        // 工艺特色
        tooltip.add(TextFormatting.YELLOW + "防护机制：");
        tooltip.add(TextFormatting.GRAY + "  • 激活时完全免疫所有伤害");
        tooltip.add(TextFormatting.GRAY + "  • 金色粒子螺旋防护场");
        tooltip.add(TextFormatting.GRAY + "  • 10%伤害转化为黄心");
        tooltip.add(TextFormatting.GREEN + "  • 冷却时间：5秒（最短）");
        tooltip.add(TextFormatting.GOLD + "  • 连续格挡减少冷却时间");
        tooltip.add(TextFormatting.GRAY + "  • 消耗：" + actualCost + " RF/次" +
                (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (已优化)" : ""));

        tooltip.add("");

        // 智能防护系统
        tooltip.add(TextFormatting.LIGHT_PURPLE + "冷却期间被动：");
        tooltip.add(TextFormatting.GOLD + "  • 90%概率无视头部伤害");
        tooltip.add(TextFormatting.GOLD + "  • 80%概率无视身体伤害");
        tooltip.add(TextFormatting.GOLD + "  • 触发时伤害减免90%");
        tooltip.add(TextFormatting.GOLD + "  • 致命伤害转化黄心");
        tooltip.add(TextFormatting.GOLD + "  • 完全免疫所有视觉干扰");
        tooltip.add(TextFormatting.GOLD + "  • 最强被动防护");

        tooltip.add("");

        // 设计理念
        tooltip.add(TextFormatting.LIGHT_PURPLE + "设计理念：");
        tooltip.add(TextFormatting.GRAY + "\"在天才的门槛前，我们用毅力");
        tooltip.add(TextFormatting.GRAY + " 和智慧铸就了这件杰作。\"");
        tooltip.add(TextFormatting.DARK_GRAY + "   —— 首席工程师 Jason577657");

        tooltip.add("");

        // 使用指南
        tooltip.add(TextFormatting.AQUA + "使用指南：");
        tooltip.add(TextFormatting.GRAY + "  • 专业级全方位防护方案");
        tooltip.add(TextFormatting.GRAY + "  • 需要高功率充电设备支持");
        tooltip.add(TextFormatting.GRAY + "  • 建议配合远程防护装备");

        // 能量状态指示
        double percentage = (double) energy / MAX_ENERGY * 100;
        String statusColor = percentage > 70 ? TextFormatting.GREEN.toString() :
                percentage > 40 ? TextFormatting.YELLOW.toString() : TextFormatting.RED.toString();

        tooltip.add("");
        tooltip.add(statusColor + "◆ 系统状态：" + String.format("%.1f%%", percentage) + " 运行效率");

        if (percentage < 15) {
            tooltip.add(TextFormatting.RED + "  警告：能量临界，系统性能下降");
        } else if (percentage < 30) {
            tooltip.add(TextFormatting.YELLOW + "  提示：建议补充能量以维持最佳性能");
        } else if (percentage >= 85) {
            tooltip.add(TextFormatting.AQUA + "  优秀：系统运行在最佳状态");
        }

        tooltip.add("");

        // 技术参数
        tooltip.add(TextFormatting.GOLD + "技术参数：");
        tooltip.add(TextFormatting.GREEN + "  • 低能耗高效率设计");
        tooltip.add(TextFormatting.GREEN + "  • 智能被动防护系统");
        tooltip.add(TextFormatting.GREEN + "  • 全伤害类型免疫");
        tooltip.add(TextFormatting.YELLOW + "  • 依赖外部能源补给系统");

        tooltip.add("");

        // 底部签名
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.LIGHT_PURPLE + "\"凡人智慧的巅峰造物\"");
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderAdvEnergyBarrier(stack);
    }

    private static class CapabilityProviderAdvEnergyBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderAdvEnergyBarrier(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) setEnergyStored(stack, stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) setEnergyStored(stack, stored - extracted);
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return ItemadvEnergyBarrier.getEnergyStored(stack);
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
        public void deserializeNBT(NBTTagCompound nbt) {}
    }

    public static boolean shouldBlockDamage(DamageSource source) {
        // 只格挡近战攻击
        return isMeleeDamage(source);
    }

    public static boolean isMeleeDamage(DamageSource source) {
        return source.getImmediateSource() instanceof net.minecraft.entity.Entity &&
                !source.isProjectile() &&
                !source.isMagicDamage() &&
                !source.isExplosion() &&
                !source.isFireDamage();
    }

    // 获取伤害类型的友好名称
    public static String getDamageTypeName(DamageSource source) {
        if (isMeleeDamage(source)) {
            if (source.getTrueSource() instanceof EntityPlayer) return "玩家近战攻击";
            if (source.getTrueSource() instanceof net.minecraft.entity.monster.IMob) return "怪物近战攻击";
            return "近战攻击";
        }
        return source.damageType + "伤害";
    }

    // ===== 核心修改：使用能量效率处理伤害格挡 =====
    public static boolean handleDamageBlock(net.minecraftforge.event.entity.living.LivingAttackEvent event, ItemStack stack) {
        if (!shouldBlockDamage(event.getSource())) return false;

        // 获取玩家（如果被攻击的是玩家）
        EntityPlayer player = null;
        if (event.getEntityLiving() instanceof EntityPlayer) {
            player = (EntityPlayer) event.getEntityLiving();
        }

        // 计算实际消耗
        int actualCost = player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, COST_PER_BLOCK) : COST_PER_BLOCK;

        // 🔄 改用标准 IEnergyStorage 接口
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.extractEnergy(actualCost, true) < actualCost) {
            return false;
        }

        // 100% 概率格挡近战攻击 - 移除随机数检查
        // 格挡成功，消耗能量
        energy.extractEnergy(actualCost, false);

        // 显示节省提示
        if (player != null && actualCost < COST_PER_BLOCK) {
            EnergyEfficiencyManager.showEfficiencySaving(player, COST_PER_BLOCK, actualCost);
        }

        event.setCanceled(true);

        net.minecraft.entity.EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            if (entity instanceof EntityPlayer) {
                ((EntityPlayer) entity).sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.BLUE + "[高级护盾] 成功格挡 " + getDamageTypeName(event.getSource()) +
                                        TextFormatting.YELLOW + " (剩余：" + energy.getEnergyStored() + " RF)" +
                                        (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " [效率加成]" : "")
                        ), true);
            }
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                    entity.getSoundCategory(), 0.5F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }
}