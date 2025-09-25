package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;  // 添加导入
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
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
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCrudeEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 20000;
    public static final int COST_PER_BLOCK = 500;  // 原始消耗

    public ItemCrudeEnergyBarrier() {
        setRegistryName("crude_energy_barrier");
        setTranslationKey("crude_energy_barrier");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
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
            int energy = getEnergyStored(itemstack);
            double percentage = (double) energy / MAX_ENERGY * 100;
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GRAY + "[粗劣护盾] " +
                                    TextFormatting.GREEN + "已激活！100%格挡物理攻击 " +
                                    TextFormatting.YELLOW + String.format("(%.1f%%)", percentage) +
                                    TextFormatting.AQUA + " [冷却：20秒]"
                    ), true);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被卸下时调用
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GRAY + "[粗劣护盾] " +
                                    TextFormatting.RED + "已停用"
                    ), true);
        }
    }

    // ===== 🎯 修改：改用 IEnergyStorage 接口 =====
    public static int getEnergyStored(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return energy != null ? energy.getEnergyStored() : 0;
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            // 先清空，再填充到目标值
            energy.extractEnergy(energy.getEnergyStored(), false);
            energy.receiveEnergy(amount, false);
        }
    }

    // 获取上次格挡时间
    public static long getLastBlockTime(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getLong("lastBlockTime") : 0;
    }

    // 设置上次格挡时间
    public static void setLastBlockTime(ItemStack stack, long time) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setLong("lastBlockTime", time);
    }

    // ===== 🔄 修改：支持能量效率的 consumeEnergy 方法 =====
    public boolean consumeEnergy(ItemStack stack, int amount) {
        return consumeEnergy(stack, amount, null);
    }

    public boolean consumeEnergy(ItemStack stack, int originalAmount, @Nullable EntityPlayer player) {
        // 如果有玩家，计算实际消耗
        int actualAmount = originalAmount;
        if (player != null) {
            actualAmount = EnergyEfficiencyManager.calculateActualCost(player, originalAmount);
        }

        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null && energy.extractEnergy(actualAmount, true) >= actualAmount) {
            energy.extractEnergy(actualAmount, false);

            // 显示节省提示
            if (player != null && !player.world.isRemote && actualAmount < originalAmount) {
                int saved = originalAmount - actualAmount;
                int percentage = (int)((saved / (float)originalAmount) * 100);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "⚡ 护盾效率提升: 节省 " + percentage + "% 能量"
                ), true);
            }

            return true;
        }
        return false;
    }

    // ===== 🎯 新增：获取实际消耗值（用于显示） =====
    public static int getActualCost(EntityPlayer player) {
        return player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, COST_PER_BLOCK) :
                COST_PER_BLOCK;
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
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            float f = (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
            return MathHelper.hsvToRGB(f / 3.0F, 1.0F, 1.0F);
        }
        return super.getRGBDurabilityForDisplay(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);

        // 🎯 获取实际消耗
        EntityPlayer player = Minecraft.getMinecraft().player;
        int actualCost = getActualCost(player);

        // 基础信息
        tooltip.add(TextFormatting.YELLOW + "能量：" + String.format("%,d", energy) + " / " + String.format("%,d", MAX_ENERGY) + " RF");
        tooltip.add(TextFormatting.GREEN + "100% 格挡物理攻击");
        tooltip.add(TextFormatting.RED + "冷却时间：20秒");
        tooltip.add(TextFormatting.BLUE + "可放置在任意饰品槽位");
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");

        tooltip.add("");

        // 简单描述
        tooltip.add(TextFormatting.GRAY + "「 粗劣能量屏障 」");
        tooltip.add(TextFormatting.DARK_GRAY + "用廉价材料拼凑的防护装置");
        tooltip.add(TextFormatting.DARK_GRAY + "经过改进，现在能可靠地格挡攻击");

        tooltip.add("");

        // 基本功能
        tooltip.add(TextFormatting.YELLOW + "基本功能：");
        tooltip.add(TextFormatting.GRAY + "  • 完全格挡物理攻击");
        tooltip.add(TextFormatting.GRAY + "  • 格挡后进入冷却状态");

        // 根据是否有效率加成显示不同颜色
        if (actualCost < COST_PER_BLOCK) {
            tooltip.add(TextFormatting.GREEN + "  • 消耗：" + actualCost + " RF/次 (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "  • 消耗：" + actualCost + " RF/次");
        }

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.AQUA + "使用说明：");
        tooltip.add(TextFormatting.GRAY + "  • 需要外部充电设备");
        tooltip.add(TextFormatting.GRAY + "  • 每次格挡后冷却20秒");
        tooltip.add(TextFormatting.GRAY + "  • 无法防御远程攻击");

        // 冷却状态显示
        long currentTime = System.currentTimeMillis();
        long lastBlockTime = getLastBlockTime(stack);
        long cooldownRemaining = 20000L - (currentTime - lastBlockTime);

        if (cooldownRemaining > 0) {
            int secondsRemaining = (int) Math.ceil(cooldownRemaining / 1000.0);
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "◆ 护盾冷却中: " + secondsRemaining + "秒");
        } else if (lastBlockTime > 0) {
            tooltip.add("");
            tooltip.add(TextFormatting.GREEN + "◆ 护盾就绪");
        }

        // 能量状态
        double percentage = (double) energy / MAX_ENERGY * 100;
        String statusColor = percentage > 50 ? TextFormatting.GREEN.toString() :
                percentage > 25 ? TextFormatting.YELLOW.toString() : TextFormatting.RED.toString();

        tooltip.add("");
        tooltip.add(statusColor + "◆ 电量：" + String.format("%.0f%%", percentage));

        if (energy < actualCost) {
            tooltip.add(TextFormatting.RED + "  能量不足，无法激活护盾！");
        } else if (percentage < 25) {
            tooltip.add(TextFormatting.RED + "  电量不足，赶紧充电！");
        } else if (percentage >= 80) {
            tooltip.add(TextFormatting.GREEN + "  电量充足，护盾运行良好");
        }

        // Shift显示详细信息
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "=== 详细信息 ===");

            // 显示当前效率
            if (player != null && actualCost < COST_PER_BLOCK) {
                int saved = COST_PER_BLOCK - actualCost;
                int efficiencyPercentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                tooltip.add(TextFormatting.GREEN + "当前效率加成: " + efficiencyPercentage + "%");
                tooltip.add(TextFormatting.GREEN + "每次格挡节省: " + saved + " RF");

                // 计算可格挡次数
                int normalBlocks = energy / COST_PER_BLOCK;
                int efficientBlocks = energy / actualCost;
                if (efficientBlocks > normalBlocks) {
                    tooltip.add(TextFormatting.GREEN + "额外格挡次数: +" + (efficientBlocks - normalBlocks));
                }
            } else {
                tooltip.add(TextFormatting.GRAY + "当前效率加成: 0%");
                tooltip.add(TextFormatting.DARK_GRAY + "装备机械核心可减少能量消耗");
            }

            // 显示可格挡次数
            int blocksLeft = energy / actualCost;
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "剩余格挡次数: " + blocksLeft);
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }

        tooltip.add("");

        // 底部评价
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_GRAY + "\"升级版本，总算靠谱点了\"");
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
    }

    // 移除右键充能功能 - 现在显示详细状态信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;
            int actualCost = getActualCost(player);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "=== 粗劣能量护盾状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + String.format("%,d", energy) + "/" +
                            String.format("%,d", MAX_ENERGY) + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            // 冷却状态检查
            long currentTime = System.currentTimeMillis();
            long lastBlockTime = getLastBlockTime(stack);
            long cooldownRemaining = 20000L - (currentTime - lastBlockTime);

            if (cooldownRemaining > 0) {
                int secondsRemaining = (int) Math.ceil(cooldownRemaining / 1000.0);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⏰ 护盾冷却中：" + secondsRemaining + "秒"
                ));
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 护盾就绪，可以使用"
                ));
            }

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
                        TextFormatting.GREEN + "✓ 护盾活跃中，可格挡约 " + blocksLeft + " 次攻击"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✓ 格挡概率：100%（冷却后）"
                ));

                // 显示效率信息
                if (actualCost < COST_PER_BLOCK) {
                    int saved = COST_PER_BLOCK - actualCost;
                    int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 能量效率加成: " + percentage + "% (每次节省 " + saved + " RF)"
                    ));
                }

                // 根据剩余能量给出提醒
                if (energyPercent < 20) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 能量严重不足，建议立即充电！"
                    ));
                } else if (energyPercent < 40) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                    ));
                } else if (energyPercent >= 80) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "✓ 能量充足，护盾运行良好"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "护盾类型：" + TextFormatting.GREEN + "完全物理护盾 (100%)"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "实际消耗：" + TextFormatting.YELLOW + actualCost + " RF/次" +
                            (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (效率加成)" : "")
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "保护范围：所有物理攻击（100% 格挡概率）"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "冷却机制：每次格挡后冷却20秒"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "装备方式：可放置在任意饰品槽位"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "特点：改进版护盾，可靠但有冷却限制"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderCrudeBarrier(stack);
    }

    private static class CapabilityProviderCrudeBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderCrudeBarrier(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    NBTTagCompound tag = stack.getTagCompound();
                    int stored = tag != null ? tag.getInteger("Energy") : 0;
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) {
                        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
                        stack.getTagCompound().setInteger("Energy", stored + received);
                    }
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    NBTTagCompound tag = stack.getTagCompound();
                    int stored = tag != null ? tag.getInteger("Energy") : 0;
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) {
                        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
                        stack.getTagCompound().setInteger("Energy", stored - extracted);
                    }
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    NBTTagCompound tag = stack.getTagCompound();
                    return tag != null ? tag.getInteger("Energy") : 0;
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

    // ===== 🔄 修改：tryBlock 方法支持能量效率 =====
    public static boolean tryBlock(LivingAttackEvent event, ItemStack stack, EntityPlayer player) {
        if (!isMeleeDamage(event.getSource())) return false;

        // 计算实际消耗
        int actualCost = getActualCost(player);
        int energy = getEnergyStored(stack);

        if (energy < actualCost) return false;

        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        long lastBlockTime = getLastBlockTime(stack);
        long cooldownRemaining = 20000L - (currentTime - lastBlockTime);

        if (cooldownRemaining > 0) {
            // 冷却中，格挡失败
            if (!player.world.isRemote) {
                int secondsRemaining = (int) Math.ceil(cooldownRemaining / 1000.0);
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GRAY + "[粗劣护盾] " +
                                        TextFormatting.YELLOW + "护盾冷却中... (" + secondsRemaining + "秒)"
                        ), true);
            }
            return false;
        }

        // 100% 格挡成功 - 使用改进的 consumeEnergy
        ItemCrudeEnergyBarrier barrierItem = (ItemCrudeEnergyBarrier) stack.getItem();
        if (barrierItem.consumeEnergy(stack, COST_PER_BLOCK, player)) {
            setLastBlockTime(stack, currentTime);
            event.setCanceled(true);

            if (!player.world.isRemote) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GRAY + "[粗劣护盾] 成功格挡 " + getDamageTypeName(event.getSource()) +
                                        TextFormatting.YELLOW + " (剩余：" + getEnergyStored(stack) + " RF)" +
                                        TextFormatting.AQUA + " [冷却：20秒]"
                        ), true);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ITEM_SHIELD_BLOCK,
                        player.getSoundCategory(), 0.3F, 1.4F);
            }
            return true;
        }

        return false;
    }

    // 保留旧的 tryBlock 方法以保持兼容性
    public static boolean tryBlock(LivingAttackEvent event, ItemStack stack) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            return tryBlock(event, stack, (EntityPlayer) event.getEntityLiving());
        }
        return false;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }

    // 创造模式充能
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null) {
                energy.receiveEnergy(MAX_ENERGY, false);
            }
        }
    }
}