package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.EnergyStorage;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 温度调节器 - 直接支持Forge Energy (FE)
 * 自动调节玩家体温，兼容SimpleDifficulty
 */
public class ItemTemperatureRegulator extends Item implements IBauble {

    private static final int MAX_ENERGY = 100000;
    private static final int MAX_TRANSFER = 1000;
    private static final int ENERGY_PER_TICK = 15;
    private static final int REGULATE_INTERVAL = 20;

    private static final int TARGET_TEMP_MIN = 11;
    private static final int TARGET_TEMP_MAX = 14;
    private static final int COLD_THRESHOLD = 10;
    private static final int HOT_THRESHOLD = 15;
    private static final int TEMP_ADJUSTMENT = 2;

    // 检查SimpleDifficulty是否已加载
    private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");

    // 反射缓存
    private static Class<?> sdCapabilitiesClass;
    private static Object temperatureCapability;
    private static Method getCapabilityMethod;
    private static Method getTemperatureLevelMethod;
    private static Method addTemperatureLevelMethod;
    private static Method getTemperatureEnumMethod;

    static {
        if (SIMPLE_DIFFICULTY_LOADED) {
            initializeReflection();
        }
    }

    private static void initializeReflection() {
        try {
            // 加载SDCapabilities类
            sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

            // 获取TEMPERATURE字段
            temperatureCapability = sdCapabilitiesClass.getField("TEMPERATURE").get(null);

            // 获取getCapability方法 - 使用与第一个代码相同的方式
            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            // 加载ITemperatureCapability接口
            Class<?> tempCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.temperature.ITemperatureCapability");

            // 获取温度相关方法
            getTemperatureLevelMethod = tempCapabilityClass.getMethod("getTemperatureLevel");
            addTemperatureLevelMethod = tempCapabilityClass.getMethod("addTemperatureLevel", int.class);
            getTemperatureEnumMethod = tempCapabilityClass.getMethod("getTemperatureEnum");

            System.out.println("[TempRegulator] SimpleDifficulty反射初始化成功");
        } catch (Exception e) {
            System.err.println("[TempRegulator] SimpleDifficulty反射初始化失败: " + e.getMessage());
        }
    }

    public ItemTemperatureRegulator() {
        setRegistryName("temperature_regulator");
        setTranslationKey("temperature_regulator");
        setMaxStackSize(1);
        setMaxDamage(1000);
        setNoRepair();
        setCreativeTab(moremodCreativeTab.moremod_TAB);

        System.out.println("[TempRegulator] 温度调节器初始化完成 (FE支持)");
        System.out.println("[TempRegulator] SimpleDifficulty状态: " + (SIMPLE_DIFFICULTY_LOADED ? "已加载" : "未加载"));
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.CHARM;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer entityPlayer = (EntityPlayer) player;

        if (entityPlayer.ticksExisted % REGULATE_INTERVAL == 0) {
            regulateTemperature(itemstack, entityPlayer);
        }
    }

    /**
     * 主要的温度调节逻辑
     */
    private void regulateTemperature(ItemStack stack, EntityPlayer player) {
        if (!hasEnergy(stack, ENERGY_PER_TICK)) {
            if (player.ticksExisted % 200 == 0) {
                sendMessage(player, "温度调节器能量不足！", TextFormatting.RED);
            }
            return;
        }

        boolean temperatureRegulated = false;

        if (SIMPLE_DIFFICULTY_LOADED && temperatureCapability != null) {
            temperatureRegulated = regulateWithSimpleDifficulty(player);
        } else {
            temperatureRegulated = regulateWithBiomeDetection(player);
        }

        if (temperatureRegulated) {
            consumeEnergy(stack, ENERGY_PER_TICK);
            spawnRegulationParticles(player);
        }
    }

    /**
     * 使用SimpleDifficulty API调节温度（通过反射）
     */
    private boolean regulateWithSimpleDifficulty(EntityPlayer player) {
        try {
            // 通过反射获取温度能力 - 使用null作为EnumFacing参数（与第一个代码相同）
            Object tempCapability = getCapabilityMethod.invoke(player, temperatureCapability, null);
            if (tempCapability == null) {
                System.err.println("[TempRegulator] 无法获取玩家温度能力，使用备用方案");
                return regulateWithBiomeDetection(player);
            }

            // 获取当前温度等级
            int currentTempLevel = (int) getTemperatureLevelMethod.invoke(tempCapability);
            Object tempEnum = getTemperatureEnumMethod.invoke(tempCapability);

            boolean needsRegulation = false;
            String action = "";
            int targetTemp = currentTempLevel;

            if (currentTempLevel <= COLD_THRESHOLD) {
                targetTemp = Math.min(currentTempLevel + TEMP_ADJUSTMENT, TARGET_TEMP_MIN);
                action = "加热";
                needsRegulation = true;
            } else if (currentTempLevel >= HOT_THRESHOLD) {
                targetTemp = Math.max(currentTempLevel - TEMP_ADJUSTMENT, TARGET_TEMP_MAX);
                action = "降温";
                needsRegulation = true;
            }

            if (needsRegulation) {
                int adjustment = targetTemp - currentTempLevel;
                addTemperatureLevelMethod.invoke(tempCapability, adjustment);

                // 使用更低的频率显示消息（与第一个代码类似）
                if (player.world.rand.nextInt(5) == 0) {
                    String tempStatus = tempEnum != null ? tempEnum.toString() + " (" + currentTempLevel + ")" :
                            getTemperatureStatusForLevel(currentTempLevel);
                    String newTempStatus = getTemperatureStatusForLevel(targetTemp);
                    sendMessage(player, "🌡️ 温度调节: " + action +
                            " (" + tempStatus + " → " + newTempStatus + ")", TextFormatting.AQUA);
                }
            }

            return needsRegulation;

        } catch (Exception e) {
            System.err.println("[TempRegulator] SimpleDifficulty API调用失败: " + e.getMessage());
            e.printStackTrace();
            return regulateWithBiomeDetection(player);
        }
    }

    /**
     * 基于生物群系的温度检测（备用方案）
     */
    private boolean regulateWithBiomeDetection(EntityPlayer player) {
        try {
            float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());

            boolean needsRegulation = false;
            String action = "";

            if (biomeTemp < 0.2f) {
                action = "加热";
                needsRegulation = true;
            } else if (biomeTemp > 1.0f) {
                action = "降温";
                needsRegulation = true;
            }

            if (needsRegulation && player.world.rand.nextInt(5) == 0) {
                String mode = SIMPLE_DIFFICULTY_LOADED ? "[API备用]" : "[生物群系]";
                sendMessage(player, "🌡️ " + mode + " 温度调节: " + action, TextFormatting.AQUA);
            }

            return needsRegulation;

        } catch (Exception e) {
            System.err.println("[TempRegulator] 生物群系检测失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据温度等级获取对应的状态文本
     */
    private String getTemperatureStatusForLevel(int level) {
        if (level >= 0 && level <= 5) return "极寒 (" + level + ")";
        else if (level >= 6 && level <= 10) return "寒冷 (" + level + ")";
        else if (level >= 11 && level <= 14) return "正常 (" + level + ")";
        else if (level >= 15 && level <= 19) return "炎热 (" + level + ")";
        else if (level >= 20 && level <= 25) return "灼热 (" + level + ")";
        else return "异常 (" + level + ")";
    }

    /**
     * 生成温度调节的粒子效果
     */
    private void spawnRegulationParticles(EntityPlayer player) {
        if (player.world.rand.nextInt(3) == 0) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (player.world.rand.nextDouble() - 0.5) * 1.2;
                double offsetY = player.world.rand.nextDouble() * 1.5;
                double offsetZ = (player.world.rand.nextDouble() - 0.5) * 1.2;

                double x = player.posX + offsetX;
                double y = player.posY + offsetY;
                double z = player.posZ + offsetZ;

                // 根据生物群系温度选择粒子类型（与第一个代码相同的逻辑）
                EnumParticleTypes particleType = getParticleByBiome(player);

                player.world.spawnParticle(particleType, x, y, z,
                        (player.world.rand.nextDouble() - 0.5) * 0.1,
                        0.1,
                        (player.world.rand.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    /**
     * 根据生物群系获取粒子类型
     */
    private EnumParticleTypes getParticleByBiome(EntityPlayer player) {
        float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());

        if (biomeTemp < 0.3f) {
            return EnumParticleTypes.FLAME;
        } else if (biomeTemp > 1.0f) {
            return EnumParticleTypes.SNOWBALL;
        } else {
            return player.world.rand.nextBoolean() ?
                    EnumParticleTypes.VILLAGER_HAPPY : EnumParticleTypes.ENCHANTMENT_TABLE;
        }
    }

    /**
     * 发送消息给玩家
     */
    private void sendMessage(EntityPlayer player, String text, TextFormatting color) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        player.sendStatusMessage(message, true);
    }

    // ===========================================
    // FE能量系统实现
    // ===========================================

    /**
     * 获取当前存储的能量
     */
    public int getEnergyStored(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0;
        }
        return stack.getTagCompound().getInteger("Energy");
    }

    /**
     * 获取最大能量存储容量
     */
    public int getMaxEnergyStored(ItemStack stack) {
        return MAX_ENERGY;
    }

    /**
     * 检查是否有足够的能量
     */
    public boolean hasEnergy(ItemStack stack, int required) {
        return getEnergyStored(stack) >= required;
    }

    /**
     * 消耗指定数量的能量
     */
    public int consumeEnergy(ItemStack stack, int amount) {
        if (!stack.hasTagCompound()) {
            return 0;
        }

        int stored = getEnergyStored(stack);
        int consumed = Math.min(stored, amount);

        if (consumed > 0) {
            stack.getTagCompound().setInteger("Energy", stored - consumed);
            updateDamageBar(stack);
        }

        return consumed;
    }

    /**
     * 设置能量
     */
    public void setEnergy(ItemStack stack, int energy) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        energy = Math.max(0, Math.min(MAX_ENERGY, energy));
        stack.getTagCompound().setInteger("Energy", energy);
        updateDamageBar(stack);
    }

    /**
     * 更新耐久度条来显示能量状态
     */
    private void updateDamageBar(ItemStack stack) {
        int energy = getEnergyStored(stack);
        int damage = getMaxDamage() - (energy * getMaxDamage() / MAX_ENERGY);
        stack.setItemDamage(Math.max(1, damage));
    }

    /**
     * 总是显示耐久度条来表示能量
     */
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    /**
     * 返回耐久度显示百分比
     */
    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int energy = getEnergyStored(stack);
        return 1.0 - (double) energy / (double) MAX_ENERGY;
    }

    /**
     * 根据能量百分比返回耐久度条颜色
     */
    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        int energy = getEnergyStored(stack);
        double percent = (double) energy / (double) MAX_ENERGY;

        if (percent > 0.6) {
            return 0x00FF00; // 绿色
        } else if (percent > 0.3) {
            return 0xFFFF00; // 黄色
        } else if (percent > 0.1) {
            return 0xFF8800; // 橙色
        } else {
            return 0xFF0000; // 红色
        }
    }

    /**
     * 创建能量存储Capability
     */
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new EnergyCapabilityProvider(stack);
    }

    /**
     * 能量Capability提供者
     */
    private class EnergyCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final IEnergyStorage energyStorage;

        public EnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
            this.energyStorage = new EnergyStorage(MAX_ENERGY, MAX_TRANSFER, MAX_TRANSFER) {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int currentEnergy = getEnergyStored();
                    int energyReceived = Math.min(maxReceive, Math.min(MAX_TRANSFER, capacity - currentEnergy));

                    if (!simulate && energyReceived > 0) {
                        setEnergy(stack, currentEnergy + energyReceived);
                    }

                    return energyReceived;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int currentEnergy = getEnergyStored();
                    int energyExtracted = Math.min(maxExtract, Math.min(MAX_TRANSFER, currentEnergy));

                    if (!simulate && energyExtracted > 0) {
                        setEnergy(stack, currentEnergy - energyExtracted);
                    }

                    return energyExtracted;
                }

                @Override
                public int getEnergyStored() {
                    return ItemTemperatureRegulator.this.getEnergyStored(stack);
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
        @SuppressWarnings("unchecked")
        public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.util.EnumFacing facing) {
            if (capability == CapabilityEnergy.ENERGY) {
                return (T) energyStorage;
            }
            return null;
        }

        @Override
        public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, net.minecraft.util.EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        int energy = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);
        double percent = (double) energy / (double) maxEnergy * 100;

        tooltip.add(TextFormatting.GOLD + "🌡️ 智能温度调节器");
        tooltip.add("");

        // 显示能量信息
        tooltip.add(TextFormatting.AQUA + "能量: " + TextFormatting.WHITE +
                String.format("%,d / %,d FE", energy, maxEnergy));

        TextFormatting percentColor;
        if (percent > 60) percentColor = TextFormatting.GREEN;
        else if (percent > 30) percentColor = TextFormatting.YELLOW;
        else if (percent > 10) percentColor = TextFormatting.GOLD;
        else percentColor = TextFormatting.RED;

        tooltip.add(TextFormatting.GRAY + "电量: " + percentColor + String.format("%.1f%%", percent));

        if (SIMPLE_DIFFICULTY_LOADED) {
            tooltip.add(TextFormatting.GREEN + "✅ SimpleDifficulty集成");
            tooltip.add(TextFormatting.GRAY + "自动调节体温到NORMAL范围");
            tooltip.add(TextFormatting.GRAY + "目标范围: " + TARGET_TEMP_MIN + "-" + TARGET_TEMP_MAX + " (舒适)");
            tooltip.add(TextFormatting.GRAY + "调节强度: ±" + TEMP_ADJUSTMENT + " 级/次");
        } else {
            tooltip.add(TextFormatting.YELLOW + "⚠️ SimpleDifficulty未加载");
            tooltip.add(TextFormatting.GRAY + "基于生物群系温度工作");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "能耗: " + ENERGY_PER_TICK + " FE/秒");
        tooltip.add(TextFormatting.GRAY + "传输速率: " + String.format("%,d FE/t", MAX_TRANSFER));
        tooltip.add(TextFormatting.DARK_PURPLE + "佩戴在护身符栏位");

        if (hasEnergy(stack, ENERGY_PER_TICK)) {
            tooltip.add(TextFormatting.GREEN + "状态: ✅ 就绪运行");
        } else {
            tooltip.add(TextFormatting.RED + "状态: ⚡ 需要充电");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_GRAY +
                "在极端环境中保持舒适体温");
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            String mode = SIMPLE_DIFFICULTY_LOADED ? "SimpleDifficulty集成" : "生物群系模式";
            sendMessage((EntityPlayer) player, "🌡️ 温度调节器已激活 (" + mode + ")", TextFormatting.GREEN);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            sendMessage((EntityPlayer) player, "🌡️ 温度调节器已关闭", TextFormatting.YELLOW);
        }
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    /**
     * 在创造模式物品栏中显示不同状态的物品
     */
    @Override
    public void getSubItems(net.minecraft.creativetab.CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            // 添加空能量版本
            items.add(new ItemStack(this));

            // 添加满能量版本
            ItemStack fullStack = new ItemStack(this);
            setEnergy(fullStack, MAX_ENERGY);
            items.add(fullStack);
        }
    }
}