package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;
// 移除客户端导入
// import net.minecraft.client.Minecraft;  // 删除这行！
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
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBasicEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 100000;
    public static final int COST_PER_BLOCK = 1000;

    public ItemBasicEnergyBarrier() {
        setRegistryName("basic_energy_barrier");
        setTranslationKey("basic_energy_barrier");
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
                            TextFormatting.AQUA + "[基础护盾] " +
                                    TextFormatting.GREEN + "已激活！50%格挡近战攻击"
                    ), true);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被卸下时调用
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GRAY + "[基础护盾] " +
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
    @SideOnly(Side.CLIENT)  // 添加客户端注解
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);

        // 基础信息
        tooltip.add(TextFormatting.YELLOW + "能量：" + energy + " / " + MAX_ENERGY + " RF");
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");
        tooltip.add(TextFormatting.BLUE + "50% 概率格挡近战攻击");
        tooltip.add(TextFormatting.GREEN + "可放置在任意饰品槽位");

        tooltip.add("");

        // 产品描述
        tooltip.add(TextFormatting.AQUA + "「 基础能量护盾 」");
        tooltip.add(TextFormatting.GRAY + "经过改良的标准化防护设备");
        tooltip.add(TextFormatting.GRAY + "性价比均衡，适合日常使用");

        tooltip.add("");

        // 功能特点 - 修改：安全获取玩家
        EntityPlayer player = null;
        int actualCost = COST_PER_BLOCK;

        // 只在客户端且世界存在时尝试获取玩家
        if (worldIn != null && worldIn.isRemote) {
            try {
                player = net.minecraft.client.Minecraft.getMinecraft().player;
                if (player != null) {
                    actualCost = EnergyEfficiencyManager.calculateActualCost(player, COST_PER_BLOCK);
                }
            } catch (Exception e) {
                // 如果获取失败，使用默认值
                actualCost = COST_PER_BLOCK;
            }
        }

        tooltip.add(TextFormatting.YELLOW + "功能特点：");
        tooltip.add(TextFormatting.GRAY + "  • 50% 概率格挡近战攻击");
        tooltip.add(TextFormatting.GRAY + "  • 稳定的能量转换效率");
        tooltip.add(TextFormatting.GRAY + "  • 较大的能量储存容量");
        tooltip.add(TextFormatting.GRAY + "  • 消耗：" + actualCost + " RF/次" +
                (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (已优化)" : ""));

        tooltip.add("");

        // 技术规格
        tooltip.add(TextFormatting.AQUA + "技术规格：");
        tooltip.add(TextFormatting.GRAY + "  • 容量：" + formatEnergy(MAX_ENERGY) + " RF");
        tooltip.add(TextFormatting.GRAY + "  • 防护类型：近战格挡");
        tooltip.add(TextFormatting.GRAY + "  • 可靠性：中等");
        tooltip.add(TextFormatting.GRAY + "  • 兼容性：通用RF标准");

        tooltip.add("");

        // 使用建议
        tooltip.add(TextFormatting.GREEN + "使用建议：");
        tooltip.add(TextFormatting.GRAY + "  • 适合探险和日常防护");
        tooltip.add(TextFormatting.GRAY + "  • 需要配合充电设备使用");
        tooltip.add(TextFormatting.GRAY + "  • 无法防御远程攻击");

        // 能量状态
        double percentage = (double) energy / MAX_ENERGY * 100;
        String statusColor = percentage > 60 ? TextFormatting.GREEN.toString() :
                percentage > 30 ? TextFormatting.YELLOW.toString() : TextFormatting.RED.toString();

        tooltip.add("");
        tooltip.add(statusColor + "◆ 当前状态：" + String.format("%.1f%%", percentage) + " 充能");

        if (percentage < 15) {
            tooltip.add(TextFormatting.RED + "  能量严重不足，请及时充电");
        } else if (percentage < 30) {
            tooltip.add(TextFormatting.YELLOW + "  能量偏低，建议充电");
        } else if (percentage >= 85) {
            tooltip.add(TextFormatting.GREEN + "  能量充足，运行稳定");
        }

        tooltip.add("");

        // 底部标识
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.BLUE + "\"稳定可靠的防护之选\"");
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // 格式化能量显示
    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    // 修改：显示护盾状态信息时也显示实际消耗
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;

            // 计算实际消耗
            int actualCost = EnergyEfficiencyManager.calculateActualCost(player, COST_PER_BLOCK);

            player.sendMessage(new TextComponentString(
                    TextFormatting.BLUE + "=== 基础能量护盾状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + energy + "/" + MAX_ENERGY + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            if (energy < actualCost) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量不足！无法提供保护！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "需要至少 " + actualCost + " RF 才能格挡一次攻击" +
                                (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (已优化)" : "")
                ));
            } else {
                int blocksLeft = energy / actualCost;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 护盾活跃中，可尝试格挡约 " + blocksLeft + " 次攻击"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✓ 格挡概率：50%"
                ));

                // 显示效率信息
                if (actualCost < COST_PER_BLOCK) {
                    int saved = COST_PER_BLOCK - actualCost;
                    int percentage = (int)((saved / (float)COST_PER_BLOCK) * 100);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "⚡ 能量效率：节省 " + percentage + "% (" + saved + " RF/次)"
                    ));
                }

                // 根据剩余能量给出提醒
                if (energyPercent < 15) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 能量严重不足，建议立即充电！"
                    ));
                } else if (energyPercent < 30) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                    ));
                } else if (energyPercent >= 85) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "✓ 能量充足，护盾运行良好"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "护盾类型：" + TextFormatting.AQUA + "概率近战护盾 (50%)"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "保护范围：仅近战攻击（50% 格挡概率）"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "装备方式：可放置在任意饰品槽位"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "注意：无法防护投射物、魔法、爆炸等远程伤害"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderBasicEnergyBarrier(stack);
    }

    private static class CapabilityProviderBasicEnergyBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderBasicEnergyBarrier(ItemStack stack) {
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
                    return ItemBasicEnergyBarrier.getEnergyStored(stack);
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

        // 50% 概率格挡近战攻击
        if (event.getEntityLiving().getRNG().nextFloat() > 0.5f) {
            // 格挡失败，不消耗能量
            net.minecraft.entity.EntityLivingBase entity = event.getEntityLiving();
            if (!entity.world.isRemote) {
                if (entity instanceof EntityPlayer) {
                    ((EntityPlayer) entity).sendStatusMessage(
                            new TextComponentString(
                                    TextFormatting.RED + "[基础护盾] 格挡失败 " + getDamageTypeName(event.getSource())
                            ), true);
                }
            }
            return false;
        }

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
                                TextFormatting.BLUE + "[基础护盾] 成功格挡 " + getDamageTypeName(event.getSource()) +
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