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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 口渴处理器 - 直接支持Forge Energy (FE)
 * 自动维持玩家水分，防止脱水
 */
public class ItemThirstProcessor extends Item implements IBauble {

    private static final int MAX_ENERGY = 80000;
    private static final int MAX_TRANSFER = 800;
    private static final int ENERGY_PER_TICK = 10;
    private static final int PROCESS_INTERVAL = 40;
    private static final int MIN_THIRST_LEVEL = 20;
    private static final int TARGET_THIRST_LEVEL = 20;
    private static final float MIN_SATURATION = 5.0f;
    private static final int EMERGENCY_THRESHOLD = 12;

    // 检查SimpleDifficulty是否已加载
    private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");

    // 反射缓存
    private static Class<?> sdCapabilitiesClass;
    private static Object thirstCapability;
    private static Method getCapabilityMethod;
    private static Method getThirstLevelMethod;
    private static Method setThirstLevelMethod;
    private static Method addThirstLevelMethod;
    private static Method getThirstSaturationMethod;
    private static Method setThirstSaturationMethod;
    private static Method addThirstSaturationMethod;
    private static Method getThirstExhaustionMethod;
    private static Method setThirstExhaustionMethod;
    private static Method isThirstyMethod;
    private static Method isDirtyMethod;
    private static Method setCleanMethod;

    static {
        if (SIMPLE_DIFFICULTY_LOADED) {
            initializeReflection();
        }
    }

    private static void initializeReflection() {
        try {
            // 加载SDCapabilities类
            sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

            // 获取THIRST字段
            thirstCapability = sdCapabilitiesClass.getField("THIRST").get(null);

            // 获取getCapability方法
            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            // 加载IThirstCapability接口
            Class<?> thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");

            // 获取口渴相关方法
            getThirstLevelMethod = thirstCapabilityClass.getMethod("getThirstLevel");
            setThirstLevelMethod = thirstCapabilityClass.getMethod("setThirstLevel", int.class);
            addThirstLevelMethod = thirstCapabilityClass.getMethod("addThirstLevel", int.class);
            getThirstSaturationMethod = thirstCapabilityClass.getMethod("getThirstSaturation");
            setThirstSaturationMethod = thirstCapabilityClass.getMethod("setThirstSaturation", float.class);
            addThirstSaturationMethod = thirstCapabilityClass.getMethod("addThirstSaturation", float.class);
            getThirstExhaustionMethod = thirstCapabilityClass.getMethod("getThirstExhaustion");
            setThirstExhaustionMethod = thirstCapabilityClass.getMethod("setThirstExhaustion", float.class);
            isThirstyMethod = thirstCapabilityClass.getMethod("isThirsty");
            isDirtyMethod = thirstCapabilityClass.getMethod("isDirty");
            setCleanMethod = thirstCapabilityClass.getMethod("setClean");

            System.out.println("[ThirstProcessor] SimpleDifficulty反射初始化成功");
        } catch (Exception e) {
            System.err.println("[ThirstProcessor] SimpleDifficulty反射初始化失败: " + e.getMessage());
        }
    }

    public ItemThirstProcessor() {
        setRegistryName("thirst_processor");
        setTranslationKey("thirst_processor");
        setMaxStackSize(1);
        setMaxDamage(1000);
        setNoRepair();
        setCreativeTab(moremodCreativeTab.moremod_TAB);

        System.out.println("[ThirstProcessor] 口渴处理器初始化完成 (FE支持)");
        System.out.println("[ThirstProcessor] SimpleDifficulty状态: " + (SIMPLE_DIFFICULTY_LOADED ? "已加载" : "未加载"));
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

        if (entityPlayer.ticksExisted % PROCESS_INTERVAL == 0) {
            processThirst(itemstack, entityPlayer);
        }
    }

    /**
     * 主要的口渴处理逻辑
     */
    private void processThirst(ItemStack stack, EntityPlayer player) {
        if (!hasEnergy(stack, ENERGY_PER_TICK)) {
            if (player.ticksExisted % 200 == 0) {
                sendMessage(player, "口渴处理器能量不足！", TextFormatting.RED);
            }
            return;
        }

        boolean thirstProcessed = false;

        if (SIMPLE_DIFFICULTY_LOADED && thirstCapability != null) {
            thirstProcessed = processWithSimpleDifficulty(player);
        } else {
            thirstProcessed = processWithVanillaHunger(player);
        }

        if (thirstProcessed) {
            consumeEnergy(stack, ENERGY_PER_TICK);
            spawnHydrationParticles(player);
        }
    }

    /**
     * 使用SimpleDifficulty API处理口渴（通过反射）
     */
    private boolean processWithSimpleDifficulty(EntityPlayer player) {
        try {
            // 通过反射获取口渴能力
            Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
            if (thirstCap == null) {
                return processWithVanillaHunger(player);
            }

            // 获取口渴系统完整状态
            int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
            float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);
            float currentExhaustion = (float) getThirstExhaustionMethod.invoke(thirstCap);
            boolean isThirsty = (boolean) isThirstyMethod.invoke(thirstCap);
            boolean isDirty = (boolean) isDirtyMethod.invoke(thirstCap);

            boolean needsHydration = false;
            String action = "";

            // 紧急补水
            if (currentThirst <= EMERGENCY_THRESHOLD || (isThirsty && currentThirst < 12)) {
                int newThirst = Math.min(20, currentThirst + 6);
                float newSaturation = Math.min(20.0f, currentSaturation + 3.0f);

                setThirstLevelMethod.invoke(thirstCap, newThirst);
                setThirstSaturationMethod.invoke(thirstCap, newSaturation);

                action = "紧急补水";
                needsHydration = true;

                if (player.world.rand.nextInt(3) == 0) {
                    sendMessage(player, "💧 紧急补水！净化水源 (" + currentThirst + " → " + newThirst + ")", TextFormatting.RED);
                }
            }
            // 常规补水
            else if (currentThirst < MIN_THIRST_LEVEL) {
                addThirstLevelMethod.invoke(thirstCap, 3);
                addThirstSaturationMethod.invoke(thirstCap, 0.5f);

                action = "常规补水";
                needsHydration = true;

                if (player.world.rand.nextInt(5) == 0) {
                    sendMessage(player, "💧 自动补水: +3 (" + currentThirst + " → " + Math.min(20, currentThirst + 3) + ")", TextFormatting.AQUA);
                }
            }
            // 状态优化
            else if (currentSaturation < MIN_SATURATION || currentExhaustion > 3.0f) {
                if (currentSaturation < MIN_SATURATION) {
                    addThirstSaturationMethod.invoke(thirstCap, 2.0f);
                    action = "饱和度补充";
                }

                if (currentExhaustion > 3.0f) {
                    setThirstExhaustionMethod.invoke(thirstCap, Math.max(0.0f, currentExhaustion - 2.0f));
                    action += (action.isEmpty() ? "" : " + ") + "疲劳缓解";
                }

                needsHydration = true;
                if (player.world.rand.nextInt(8) == 0) {
                    sendMessage(player, "💧 " + action + " (优化水分状态)", TextFormatting.BLUE);
                }
            }
            // 水源净化
            else if (isDirty) {
                setCleanMethod.invoke(thirstCap);
                action = "水源净化";
                needsHydration = true;

                sendMessage(player, "💧 水源净化完成！", TextFormatting.GREEN);
            }

            return needsHydration;

        } catch (Exception e) {
            e.printStackTrace();
            return processWithVanillaHunger(player);
        }
    }

    /**
     * 基于原版饥饿系统的处理（备用方案）
     */
    private boolean processWithVanillaHunger(EntityPlayer player) {
        try {
            int foodLevel = player.getFoodStats().getFoodLevel();
            float saturation = player.getFoodStats().getSaturationLevel();

            boolean needsNourishment = false;
            String action = "";

            if (foodLevel < 16 && saturation < 2.0f) {
                player.getFoodStats().addStats(0, 1.0f);
                action = "营养补充";
                needsNourishment = true;
            } else if (foodLevel < 12) {
                player.getFoodStats().addStats(1, 0.5f);
                action = "应急补给";
                needsNourishment = true;
            }

            if (needsNourishment && player.world.rand.nextInt(5) == 0) {
                String mode = SIMPLE_DIFFICULTY_LOADED ? "[API备用]" : "[原版模式]";
                sendMessage(player, "💧 " + mode + " " + action +
                        " (饥饿度: " + foodLevel + "/20)", TextFormatting.YELLOW);
            }

            return needsNourishment;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成补水效果的粒子
     */
    private void spawnHydrationParticles(EntityPlayer player) {
        if (player.world.rand.nextInt(3) == 0) {
            for (int i = 0; i < 4; i++) {
                double offsetX = (player.world.rand.nextDouble() - 0.5) * 1.0;
                double offsetY = player.world.rand.nextDouble() * 1.2 + 0.5;
                double offsetZ = (player.world.rand.nextDouble() - 0.5) * 1.0;

                double x = player.posX + offsetX;
                double y = player.posY + offsetY;
                double z = player.posZ + offsetZ;

                EnumParticleTypes particleType;
                int randType = player.world.rand.nextInt(3);
                switch (randType) {
                    case 0:
                        particleType = EnumParticleTypes.WATER_DROP;
                        break;
                    case 1:
                        particleType = EnumParticleTypes.WATER_SPLASH;
                        break;
                    default:
                        particleType = EnumParticleTypes.DRIP_WATER;
                        break;
                }

                player.world.spawnParticle(particleType, x, y, z, 0, -0.1, 0);
            }
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
            return 0x00AAFF; // 蓝色 - 水的颜色
        } else if (percent > 0.3) {
            return 0x0088CC; // 深蓝色
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
                    return ItemThirstProcessor.this.getEnergyStored(stack);
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
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        int energy = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);
        double percent = (double) energy / (double) maxEnergy * 100;

        tooltip.add(TextFormatting.AQUA + "💧 智能口渴处理器");
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
            tooltip.add(TextFormatting.GRAY + "智能水分管理系统：");
            tooltip.add(TextFormatting.DARK_RED + "  紧急补水: ≤" + EMERGENCY_THRESHOLD + " 或严重口渴");
            tooltip.add(TextFormatting.AQUA + "  常规补水: <" + MIN_THIRST_LEVEL);
            tooltip.add(TextFormatting.BLUE + "  状态优化: 饱和度/疲劳度管理");
            tooltip.add(TextFormatting.GREEN + "  水源净化: 自动清洁脏污水源");
        } else {
            tooltip.add(TextFormatting.YELLOW + "⚠️ SimpleDifficulty未加载");
            tooltip.add(TextFormatting.GRAY + "基于原版饥饿系统工作");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "能耗: " + ENERGY_PER_TICK + " FE/次");
        tooltip.add(TextFormatting.GRAY + "检查间隔: " + (PROCESS_INTERVAL / 20.0) + "秒");
        tooltip.add(TextFormatting.GRAY + "传输速率: " + String.format("%,d FE/t", MAX_TRANSFER));
        tooltip.add(TextFormatting.DARK_PURPLE + "佩戴在护身符栏位");

        if (hasEnergy(stack, ENERGY_PER_TICK)) {
            tooltip.add(TextFormatting.GREEN + "状态: ✅ 就绪运行");
        } else {
            tooltip.add(TextFormatting.RED + "状态: ⚡ 需要充电");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_GRAY +
                "全面的水分系统管理解决方案");
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            String mode = SIMPLE_DIFFICULTY_LOADED ? "SimpleDifficulty集成" : "原版模式";
            sendMessage((EntityPlayer) player, "💧 口渴处理器已激活 (" + mode + ")", TextFormatting.GREEN);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            sendMessage((EntityPlayer) player, "💧 口渴处理器已关闭", TextFormatting.YELLOW);
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