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

public class ItemEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 500000;
    public static final int COST_PER_BLOCK = 1000;  // 原始消耗

    public ItemEnergyBarrier() {
        setRegistryName("energy_barrier");
        setTranslationKey("energy_barrier");
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
                            TextFormatting.GOLD + "[神域护盾] " +
                                    TextFormatting.DARK_PURPLE + "伤害已然遥远.... " +
                                    TextFormatting.YELLOW + String.format("(%.1f%%)", percentage)
                    ), true);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被卸下时调用
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GRAY + "[神域护盾] " +
                                    TextFormatting.RED + "已停用"
                    ), true);
        }
    }

    // ===== 🎯 修改：改用 IEnergyStorage 的能量管理 =====
    public static int getEnergyStored(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return energy != null ? energy.getEnergyStored() : 0;
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        // 通过 IEnergyStorage 设置能量（间接方式）
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            // 先清空，再填充到目标值
            energy.extractEnergy(energy.getEnergyStored(), false);
            energy.receiveEnergy(amount, false);
        }
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
                // 每5次格挡显示一次，避免刷屏
                if (player.world.getTotalWorldTime() % 100 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 神域效率: 节省 " + percentage + "% 能量"
                    ), true);
                }
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
        tooltip.add(TextFormatting.GOLD + "完全格挡所有类型伤害");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "可放置在任意饰品槽位");

        tooltip.add("");

        // 史诗级描述
        tooltip.add(TextFormatting.DARK_PURPLE + "「 " + TextFormatting.BOLD + "神域护盾" + TextFormatting.RESET + TextFormatting.DARK_PURPLE + " 」");
        tooltip.add(TextFormatting.GRAY + "人类智慧与科技的巅峰结晶");
        tooltip.add(TextFormatting.GRAY + "一件能与神明造物比肩的传说级装备");

        tooltip.add("");

        // 力量描述
        tooltip.add(TextFormatting.GOLD + "传说中的力量：");
        tooltip.add(TextFormatting.DARK_RED + "  ◆ 抵御爆炸的毁灭之力");
        tooltip.add(TextFormatting.RED + "  ◆ 无视烈焰的灼烧");
        tooltip.add(TextFormatting.DARK_PURPLE + "  ◆ 消解魔法的奥秘");
        tooltip.add(TextFormatting.BLUE + "  ◆ 偏转投射物的轨迹");
        tooltip.add(TextFormatting.DARK_GRAY + "  ◆ 阻挡一切物理攻击");

        tooltip.add("");

        // 神话背景
        tooltip.add(TextFormatting.AQUA + "古老传说：");
        tooltip.add(TextFormatting.GRAY + "\"当末日降临，群星陨落之时，");
        tooltip.add(TextFormatting.GRAY + " 唯有神域屏障能护佑众生。\"");
        tooltip.add(TextFormatting.DARK_GRAY + "   —— 《失落科技编年史》");

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.GREEN + "使用指南：");
        tooltip.add(TextFormatting.GRAY + "  • 需要外部能量设备充电");
        tooltip.add(TextFormatting.GRAY + "  • 可装备在任意饰品槽位");
        tooltip.add(TextFormatting.GRAY + "  • 右键查看详细状态");

        // 根据是否有效率加成显示不同颜色
        if (actualCost < COST_PER_BLOCK) {
            tooltip.add(TextFormatting.GREEN + "  • 消耗：" + String.format("%,d", actualCost) + " RF/次 (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "  • 消耗：" + String.format("%,d", actualCost) + " RF/次");
        }

        // 能量效率支持提示
        tooltip.add(TextFormatting.GREEN + "  • 支持机械核心能量效率加成");

        tooltip.add("");

        // 能量状态指示
        double percentage = (double) energy / MAX_ENERGY * 100;
        String statusColor = percentage > 75 ? TextFormatting.GREEN.toString() :
                percentage > 50 ? TextFormatting.YELLOW.toString() :
                        percentage > 25 ? TextFormatting.GOLD.toString() : TextFormatting.RED.toString();

        tooltip.add(statusColor + "◆ 当前状态：" + String.format("%.1f%%", percentage) + " 充能");

        if (percentage < 10) {
            tooltip.add(TextFormatting.DARK_RED + "  神域护盾即将失效！");
        } else if (percentage < 25) {
            tooltip.add(TextFormatting.RED + "  能量不足，请及时充电");
        } else if (percentage >= 90) {
            tooltip.add(TextFormatting.AQUA + "  神域之力充盈，护佑无虞");
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
                tooltip.add(TextFormatting.GREEN + "每次格挡节省: " + String.format("%,d", saved) + " RF");

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

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "格挡范围:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 所有物理、魔法、投射物伤害");
            tooltip.add(TextFormatting.DARK_GRAY + "• 爆炸、火焰、凋零等特殊伤害");
            tooltip.add(TextFormatting.DARK_RED + "• 无法格挡：虚空、创造模式伤害");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }

        tooltip.add("");

        // 底部签名
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_PURPLE + "\"科技的终极，便是神迹。\"");
        tooltip.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // 格式化能量显示
    private static String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    // 移除右键充能功能 - 现在显示详细信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;
            int actualCost = getActualCost(player);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "=== 神域护盾状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + String.format("%,d", energy) + "/" +
                            String.format("%,d", MAX_ENERGY) + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            if (energy < actualCost) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量严重不足！无法提供任何保护！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "需要至少 " + String.format("%,d", actualCost) + " RF 才能格挡一次攻击" +
                                (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (已优化)" : "")
                ));
            } else {
                int blocksLeft = energy / actualCost;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 护盾活跃中，可完全格挡 " + blocksLeft + " 次攻击"
                ));

                // 显示效率信息
                if (actualCost < COST_PER_BLOCK) {
                    int saved = COST_PER_BLOCK - actualCost;
                    int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 能量效率加成: " + percentage + "% (每次节省 " +
                                    String.format("%,d", saved) + " RF)"
                    ));
                }

                // 根据剩余能量给出不同的提醒
                if (energyPercent < 10) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 能量严重不足，建议立即充电！"
                    ));
                } else if (energyPercent < 25) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                    ));
                } else if (energyPercent >= 90) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "✓ 能量充足，护盾处于最佳状态"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "护盾类型：" + TextFormatting.GOLD + "神域全能护盾 (100%)"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "实际消耗：" + TextFormatting.YELLOW +
                            String.format("%,d", actualCost) + " RF/次" +
                            (actualCost < COST_PER_BLOCK ? TextFormatting.GREEN + " (效率加成)" : "")
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "保护范围：爆炸、火焰、魔法、投射物、近战攻击"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "装备方式：可放置在任意饰品槽位"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "特殊：几乎无敌的全方位保护"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderSuperEnergyBarrier(stack);
    }

    private static class CapabilityProviderSuperEnergyBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderSuperEnergyBarrier(ItemStack stack) {
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

    public static boolean shouldBlockDamage(DamageSource source) {
        // 超级护盾可以格挡几乎所有类型的伤害，但不能格挡一些特殊的绝对伤害
        return !source.canHarmInCreative() &&  // 不能格挡创造模式下的伤害
                !source.isDamageAbsolute() &&   // 不能格挡绝对伤害（如虚空伤害）
                !source.damageType.equals("outOfWorld"); // 不能格挡掉出世界的伤害
    }

    // ===== 🔄 修改：handleDamageBlock 支持能量效率 =====
    public static boolean handleDamageBlock(LivingAttackEvent event, ItemStack stack, EntityPlayer player) {
        if (!shouldBlockDamage(event.getSource())) return false;

        // 计算实际消耗
        int actualCost = getActualCost(player);

        // 检查能量
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.extractEnergy(actualCost, true) < actualCost) {
            return false;
        }

        // 使用改进的 consumeEnergy 方法
        ItemEnergyBarrier barrierItem = (ItemEnergyBarrier) stack.getItem();
        if (barrierItem.consumeEnergy(stack, COST_PER_BLOCK, player)) {
            event.setCanceled(true);

            if (!player.world.isRemote) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GOLD + "[神域护盾] 完全抵御 " + getDamageTypeName(event.getSource()) +
                                        TextFormatting.YELLOW + " (剩余：" + formatEnergy(getEnergyStored(stack)) + " RF)"
                        ), true);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                        player.getSoundCategory(), 0.6F, 0.8F);
            }
            return true;
        }

        return false;
    }

    // 保留旧的方法以保持兼容性
    public static boolean handleDamageBlock(LivingAttackEvent event, ItemStack stack) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            return handleDamageBlock(event, stack, (EntityPlayer) event.getEntityLiving());
        }

        // 非玩家实体使用原始消耗
        if (!shouldBlockDamage(event.getSource())) return false;

        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.extractEnergy(COST_PER_BLOCK, true) < COST_PER_BLOCK) {
            return false;
        }

        energy.extractEnergy(COST_PER_BLOCK, false);
        event.setCanceled(true);

        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                    entity.getSoundCategory(), 0.6F, 0.8F);
        }
        return true;
    }



    // 获取伤害类型的友好名称
    private static String getDamageTypeName(DamageSource source) {
        if (source.isExplosion()) return "爆炸伤害";
        if (source.isFireDamage()) return "火焰伤害";
        if (source.isMagicDamage()) return "魔法伤害";
        if (source.isProjectile()) return "投射物伤害";
        if (source.getImmediateSource() instanceof EntityPlayer) return "玩家攻击";
        if (source.getImmediateSource() instanceof net.minecraft.entity.monster.IMob) return "怪物攻击";
        if (source.getImmediateSource() instanceof net.minecraft.entity.Entity) return "实体攻击";

        // 根据伤害类型返回中文名称
        switch (source.damageType) {
            case "fall": return "跌落伤害";
            case "drowning": return "溺水伤害";
            case "lava": return "岩浆伤害";
            case "inFire": return "火焰伤害";
            case "onFire": return "燃烧伤害";
            case "cactus": return "仙人掌伤害";
            case "starve": return "饥饿伤害";
            case "wither": return "凋零伤害";
            case "anvil": return "铁砧伤害";
            case "fallingBlock": return "下落方块伤害";
            case "thorns": return "荆棘伤害";
            case "player": return "玩家攻击";
            case "mob": return "怪物攻击";
            default: return source.damageType + "伤害";
        }
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