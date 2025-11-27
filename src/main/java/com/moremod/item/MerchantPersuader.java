package com.moremod.item;

import cofh.redstoneflux.api.IEnergyContainerItem;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;  // 添加导入
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class MerchantPersuader extends Item implements IEnergyContainerItem {

    // 能量配置
    private static int MAX_ENERGY;
    private static int ENERGY_PER_TRADE;
    private static double BASE_DISCOUNT;
    private static double MAX_DISCOUNT;
    private static double RANGE;

    static {
        com.moremod.config.ItemConfig.ensureLoaded();
        MAX_ENERGY = com.moremod.config.ItemConfig.MerchantPersuader.maxEnergy;
        ENERGY_PER_TRADE = com.moremod.config.ItemConfig.MerchantPersuader.energyPerTrade;
        BASE_DISCOUNT = com.moremod.config.ItemConfig.MerchantPersuader.baseDiscount;
        MAX_DISCOUNT = com.moremod.config.ItemConfig.MerchantPersuader.maxDiscount;
        RANGE = com.moremod.config.ItemConfig.MerchantPersuader.range;
    }
    public MerchantPersuader() {
        super();
        this.setMaxStackSize(1);
        this.setRegistryName("moremod", "merchant_persuader");
        this.setTranslationKey("merchant_persuader");
        this.setMaxDamage(1); // 用于显示能量条
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    // 关键！添加能力支持以兼容外部充电设备
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyCapabilityProvider(stack);
    }

    // 能力提供器
    private class EnergyCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;

        public EnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == CapabilityEnergy.ENERGY) {
                return (T) new ForgeEnergyWrapper(stack);
            }
            return null;
        }
    }

    // Forge Energy 包装器
    private class ForgeEnergyWrapper implements IEnergyStorage {
        private final ItemStack stack;

        public ForgeEnergyWrapper(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return MerchantPersuader.this.receiveEnergy(stack, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return MerchantPersuader.this.extractEnergy(stack, maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return MerchantPersuader.this.getEnergyStored(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return MerchantPersuader.this.getMaxEnergyStored(stack);
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;

        // 只在手持或主副手时激活 (移除能量消耗)
        if (isSelected || player.getHeldItemOffhand() == stack) {
            // 每2秒检查一次周围村民 (不消耗能量)
            if (world.getTotalWorldTime() % 40 == 0) {
                checkNearbyVillagers(player, stack);
            }
        }
    }

    private void checkNearbyVillagers(EntityPlayer player, ItemStack stack) {
        List<EntityVillager> villagers = player.world.getEntitiesWithinAABB(
                EntityVillager.class,
                player.getEntityBoundingBox().grow(RANGE)
        );

        if (!villagers.isEmpty()) {
            // 标记附近有受影响的村民
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }
            nbt.setBoolean("villagersNearby", true);
            nbt.setInteger("villagerCount", villagers.size());

            // 给村民添加"被说服"效果
            for (EntityVillager villager : villagers) {
                markVillagerPersuaded(villager, player.getUniqueID());
            }
        } else {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }
            nbt.setBoolean("villagersNearby", false);
            nbt.setInteger("villagerCount", 0);
        }
    }

    private void markVillagerPersuaded(EntityVillager villager, java.util.UUID playerUUID) {
        NBTTagCompound villagerData = villager.getEntityData();
        villagerData.setString("persuadedBy", playerUUID.toString());
        villagerData.setLong("persuadeTime", villager.world.getTotalWorldTime());

        // 添加粒子效果表示被说服
        if (villager.world.rand.nextInt(20) == 0) { // 5%概率显示粒子
            villager.world.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    villager.posX, villager.posY + 2.0, villager.posZ,
                    0, 0, 0);
        }
    }

    // 计算当前折扣比例
    public double getCurrentDiscount(ItemStack stack) {
        int energy = getEnergyStored(stack);
        if (energy <= 0) return 0.0;

        double energyRatio = (double) energy / MAX_ENERGY;
        return BASE_DISCOUNT + (MAX_DISCOUNT - BASE_DISCOUNT) * energyRatio;
    }

    // 检查玩家是否持有激活的说服器 (移除能量检查)
    public static ItemStack getActivePersuader(EntityPlayer player) {
        // 检查主手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof MerchantPersuader) {
            return mainHand;
        }

        // 检查副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (offHand.getItem() instanceof MerchantPersuader) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    // ===== 🎯 新增：获取实际交易消耗（用于显示） =====
    public static int getActualTradeCost(EntityPlayer player) {
        return player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, ENERGY_PER_TRADE) :
                ENERGY_PER_TRADE;
    }

    // ===== 🔄 修改：支持能量效率的交易完成处理 =====
    public void onTradeCompleted(EntityPlayer player, ItemStack stack, EntityVillager villager,
                                 ItemStack soldItem, int soldCount) {
        // 计算实际消耗
        int actualCost = EnergyEfficiencyManager.calculateActualCost(player, ENERGY_PER_TRADE);

        // 检查能量
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.extractEnergy(actualCost, true) < actualCost) {
            return;
        }

        // 扣除能量
        energy.extractEnergy(actualCost, false);

        // 显示节省提示
        if (actualCost < ENERGY_PER_TRADE) {
            int saved = ENERGY_PER_TRADE - actualCost;
            int percentage = (int)((saved / (float)ENERGY_PER_TRADE) * 100);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "⚡ 说服效率: 节省 " + percentage + "% 能量"
            ), true);
        }



            // 显示折扣信息
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "商人说服器生效！ " ), true);

            // 特效
            spawnTradeParticles(villager);

    }

    private void spawnTradeParticles(EntityVillager villager) {
        for (int i = 0; i < 15; i++) {
            villager.world.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    villager.posX + (villager.world.rand.nextDouble() - 0.5) * 2,
                    villager.posY + villager.world.rand.nextDouble() * 2,
                    villager.posZ + (villager.world.rand.nextDouble() - 0.5) * 2,
                    0, 0.1, 0);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        int energy = getEnergyStored(stack);
        double discount = getCurrentDiscount(stack);

        // 🎯 获取实际消耗
        EntityPlayer player = Minecraft.getMinecraft().player;
        int actualTradeCost = getActualTradeCost(player);

        tooltip.add(TextFormatting.AQUA + "⚡ 能量: " +
                String.format("%,d", energy) + " / " + String.format("%,d", MAX_ENERGY) + " RF");
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");

        if (energy > 0) {
            tooltip.add(TextFormatting.GOLD + "💰 当前折扣: " + String.format("%.1f", discount * 100) + "%");

            // 显示可交易次数
            int tradesAvailable = energy / actualTradeCost;
            tooltip.add(TextFormatting.YELLOW + "📊 可交易次数: " + tradesAvailable);
        } else {
            tooltip.add(TextFormatting.GRAY + "💰 当前折扣: 0% (无能量)");
        }

        tooltip.add("");

        // 显示附近村民状态
        if (nbt.getBoolean("villagersNearby")) {
            int count = nbt.getInteger("villagerCount");
            tooltip.add(TextFormatting.GREEN + "📍 影响范围内村民: " + count + " 个");
            tooltip.add(TextFormatting.GREEN + "✓ 说服效果已激活");
        } else {
            tooltip.add(TextFormatting.GRAY + "📍 影响范围内无村民");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "手持激活，与村民交易时生效");
        tooltip.add(TextFormatting.GRAY + "• 影响范围: " + (int)RANGE + " 格");
        tooltip.add(TextFormatting.GRAY + "• 基础折扣: " + (int)(BASE_DISCOUNT * 100) + "%");
        tooltip.add(TextFormatting.GRAY + "• 最大折扣: " + (int)(MAX_DISCOUNT * 100) + "%");

        // 根据是否有效率加成显示不同颜色
        if (actualTradeCost < ENERGY_PER_TRADE) {
            tooltip.add(TextFormatting.GREEN + "• 交易消耗: " + actualTradeCost + " RF/次 (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "• 交易消耗: " + actualTradeCost + " RF/次");
        }

        tooltip.add(TextFormatting.GREEN + "• 无持续能量消耗");

        // Shift显示详细信息
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "=== 详细信息 ===");

            // 显示当前效率
            if (player != null && actualTradeCost < ENERGY_PER_TRADE) {
                int saved = ENERGY_PER_TRADE - actualTradeCost;
                int efficiencyPercentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                tooltip.add(TextFormatting.GREEN + "当前效率加成: " + efficiencyPercentage + "%");
                tooltip.add(TextFormatting.GREEN + "每次交易节省: " + saved + " RF");

                // 计算额外交易次数
                int normalTrades = energy / ENERGY_PER_TRADE;
                int efficientTrades = energy / actualTradeCost;
                if (efficientTrades > normalTrades) {
                    tooltip.add(TextFormatting.GREEN + "额外交易次数: +" + (efficientTrades - normalTrades));
                }
            } else {
                tooltip.add(TextFormatting.GRAY + "当前效率加成: 0%");
                tooltip.add(TextFormatting.DARK_GRAY + "装备机械核心可减少交易消耗");
            }

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "工作原理:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 说服村民降低价格");

            tooltip.add(TextFormatting.DARK_GRAY + "• 能量越多折扣越高");

            // 折扣计算公式
            tooltip.add(TextFormatting.DARK_GRAY + "折扣公式:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 最低: " + (int)(BASE_DISCOUNT * 100) + "% (少量能量)");
            tooltip.add(TextFormatting.DARK_GRAY + "• 最高: " + (int)(MAX_DISCOUNT * 100) + "% (满能量)");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }

        super.addInformation(stack, world, tooltip, flag);
    }

    // 右键显示状态信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;
            int actualTradeCost = getActualTradeCost(player);
            double discount = getCurrentDiscount(stack);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "=== 商人说服器状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + String.format("%,d", energy) + "/" +
                            String.format("%,d", MAX_ENERGY) + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "当前折扣：" + String.format("%.1f", discount * 100) + "%"
            ));

            // 交易状态
            if (energy >= actualTradeCost) {
                int tradesLeft = energy / actualTradeCost;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 说服器就绪，可进行 " + tradesLeft + " 次交易"
                ));

                // 显示效率信息
                if (actualTradeCost < ENERGY_PER_TRADE) {
                    int saved = ENERGY_PER_TRADE - actualTradeCost;
                    int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 能量效率加成: " + percentage + "% (每次节省 " + saved + " RF)"
                    ));
                }
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 能量不足，无法进行交易"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "需要至少 " + actualTradeCost + " RF" +
                                (actualTradeCost < ENERGY_PER_TRADE ? TextFormatting.GREEN + " (已优化)" : "")
                ));
            }

            // 检查附近村民
            List<EntityVillager> villagers = world.getEntitiesWithinAABB(
                    EntityVillager.class,
                    player.getEntityBoundingBox().grow(RANGE)
            );

            if (!villagers.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "📍 检测到 " + villagers.size() + " 个村民在影响范围内"
                ));
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "📍 影响范围内没有村民"
                ));
            }

            // 能量状态提醒
            if (energyPercent < 10) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量严重不足，说服力即将失效！"
                ));
            } else if (energyPercent < 25) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                ));
            } else if (energyPercent >= 90) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GREEN + "✓ 能量充足，说服力达到巅峰"
                ));
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "物品类型：" + TextFormatting.GOLD + "经济辅助工具"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "交易消耗：" + TextFormatting.YELLOW + actualTradeCost + " RF/次" +
                            (actualTradeCost < ENERGY_PER_TRADE ? TextFormatting.GREEN + " (效率加成)" : "")
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "特性：能量越高折扣越大"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // IEnergyContainerItem 实现
    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
        NBTTagCompound nbt = container.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            container.setTagCompound(nbt);
        }
        int stored = nbt.getInteger("Energy");
        int toReceive = Math.min(maxReceive, MAX_ENERGY - stored);

        if (!simulate) {
            nbt.setInteger("Energy", stored + toReceive);
            updateDamage(container);
        }

        return toReceive;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        NBTTagCompound nbt = container.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            container.setTagCompound(nbt);
        }
        int stored = nbt.getInteger("Energy");
        int toExtract = Math.min(maxExtract, stored);

        if (!simulate) {
            nbt.setInteger("Energy", stored - toExtract);
            updateDamage(container);
        }

        return toExtract;
    }

    @Override
    public int getEnergyStored(ItemStack container) {
        NBTTagCompound nbt = container.getTagCompound();
        return nbt == null ? 0 : nbt.getInteger("Energy");
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return MAX_ENERGY;
    }

    private void updateDamage(ItemStack stack) {
        int energy = getEnergyStored(stack);
        int damage = MAX_ENERGY - energy;
        stack.setItemDamage(damage * stack.getMaxDamage() / MAX_ENERGY);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - (double) getEnergyStored(stack) / MAX_ENERGY;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        double durability = 1.0 - getDurabilityForDisplay(stack);
        return MathHelper.hsvToRGB((float)(durability / 3.0F), 1.0F, 1.0F);
    }

    // 创造模式充能
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }
            nbt.setInteger("Energy", MAX_ENERGY);
            updateDamage(stack);
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0 && getCurrentDiscount(stack) >= MAX_DISCOUNT * 0.9;
    }
}