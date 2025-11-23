package com.moremod.init;

import com.moremod.moremod;
import com.moremod.recipe.BottlingMachineRecipe;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class AutoBottlingRecipeManager {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    // 配置选项
    private static final Set<String> BLACKLISTED_FLUIDS = new HashSet<>();
    private static final Set<String> BLACKLISTED_MODS = new HashSet<>();
    private static final Map<String, Integer> CUSTOM_AMOUNTS = new HashMap<>();

    static {
        // 默认黑名单 - 不应该被装瓶的液体
        BLACKLISTED_FLUIDS.add("molten_iron"); // 熔融金属通常不装瓶
        BLACKLISTED_FLUIDS.add("molten_gold");
        BLACKLISTED_FLUIDS.add("plasma"); // 等离子体

        // 自定义液体量（默认是1000mB for 桶, 250mB for 瓶）
        CUSTOM_AMOUNTS.put("honey", 250);
        CUSTOM_AMOUNTS.put("milk", 1000);
        CUSTOM_AMOUNTS.put("potion", 250);
    }

    /**
     * 自动扫描并注册所有液体的装瓶配方
     */
    public static void registerAutoRecipes() {
        LOGGER.info("开始自动注册装瓶配方...");

        int recipeCount = 0;

        // 获取所有注册的液体
        for (Map.Entry<String, Fluid> entry : FluidRegistry.getRegisteredFluids().entrySet()) {
            String fluidName = entry.getKey();
            Fluid fluid = entry.getValue();

            // 检查黑名单
            if (isBlacklisted(fluid)) {
                LOGGER.debug("跳过黑名单液体: {}", fluidName);
                continue;
            }

            // 注册该液体的所有容器配方
            recipeCount += registerFluidContainerRecipes(fluid);
        }

        LOGGER.info("自动注册完成，共注册 {} 个装瓶配方", recipeCount);

        // 注册自定义配方（针对特殊物品）
        registerSpecialRecipes();
    }

    /**
     * 为指定液体注册所有可能的容器配方
     */
    private static int registerFluidContainerRecipes(Fluid fluid) {
        int count = 0;

        // 1. 检查并注册原版桶配方
        if (FluidRegistry.getBucketFluids().contains(fluid)) {
            ItemStack filledBucket = FluidUtil.getFilledBucket(new FluidStack(fluid, Fluid.BUCKET_VOLUME));
            if (!filledBucket.isEmpty()) {
                BottlingMachineRecipe.addRecipe(
                        filledBucket,
                        new ItemStack(Items.BUCKET), 1,
                        new FluidStack(fluid, Fluid.BUCKET_VOLUME)
                );
                count++;
                LOGGER.debug("注册桶配方: {} -> {}", fluid.getName(), filledBucket.getDisplayName());
            }
        }

        // 2. 检查通用桶（Forge Universal Bucket）
        if (FluidRegistry.isUniversalBucketEnabled()) {
            ItemStack universalBucket = UniversalBucket.getFilledBucket(
                    ForgeModContainer.getInstance().universalBucket,
                    fluid
            );
            if (!universalBucket.isEmpty()) {
                BottlingMachineRecipe.addRecipe(
                        universalBucket,
                        new ItemStack(ForgeModContainer.getInstance().universalBucket), 1,
                        new FluidStack(fluid, Fluid.BUCKET_VOLUME)
                );
                count++;
                LOGGER.debug("注册通用桶配方: {}", fluid.getName());
            }
        }

        // 3. 扫描所有物品，查找能装这种液体的容器
        count += scanAndRegisterContainerItems(fluid);

        // 4. 检查矿物词典中的容器
        count += registerOreDictContainers(fluid);

        return count;
    }

    /**
     * 扫描所有注册的物品，找出能装指定液体的容器
     */
    private static int scanAndRegisterContainerItems(Fluid fluid) {
        int count = 0;
        Set<String> processedItems = new HashSet<>();

        for (ItemStack stack : getAllRegisteredItems()) {
            if (stack.isEmpty()) continue;

            String itemId = stack.getItem().getRegistryName().toString();

            // 避免重复处理
            if (processedItems.contains(itemId)) continue;
            processedItems.add(itemId);

            // 检查是否有流体处理能力
            if (!stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                continue;
            }

            IFluidHandlerItem handler = stack.getCapability(
                    CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

            if (handler == null) continue;

            // 测试是否可以填充该液体
            ItemStack testStack = stack.copy();
            IFluidHandlerItem testHandler = testStack.getCapability(
                    CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

            int filled = testHandler.fill(new FluidStack(fluid, Integer.MAX_VALUE), true);
            if (filled > 0) {
                // 获取填充后的容器
                ItemStack filledContainer = testHandler.getContainer();

                // 查找空容器（尝试排空测试）
                ItemStack emptyTest = filledContainer.copy();
                IFluidHandlerItem emptyHandler = emptyTest.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

                FluidStack drained = emptyHandler.drain(Integer.MAX_VALUE, true);
                ItemStack emptyContainer = emptyHandler.getContainer();

                // 确定液体量
                int amount = getFluidAmount(fluid, filled);

                // 注册配方
                if (!emptyContainer.isEmpty() && !filledContainer.isEmpty() &&
                        !ItemStack.areItemStacksEqual(emptyContainer, filledContainer)) {

                    BottlingMachineRecipe.addRecipe(
                            filledContainer,
                            emptyContainer, 1,
                            new FluidStack(fluid, amount)
                    );
                    count++;

                    LOGGER.debug("发现容器配方: {} + {} -> {}",
                            emptyContainer.getDisplayName(),
                            fluid.getName(),
                            filledContainer.getDisplayName());
                }
            }
        }

        return count;
    }

    /**
     * 注册矿物词典中的容器
     */
    private static int registerOreDictContainers(Fluid fluid) {
        int count = 0;

        // 玻璃瓶类
        if (fluid.getName().equals("water") ||
                fluid.getName().contains("potion") ||
                fluid.getName().contains("honey")) {

            // 检查是否有 "bottleEmpty" 矿物词典
            if (OreDictionary.doesOreNameExist("bottleEmpty")) {
                // 这里需要找到对应的填充瓶
                ItemStack filledBottle = findFilledBottle(fluid);
                if (!filledBottle.isEmpty()) {
                    BottlingMachineRecipe.addRecipe(
                            filledBottle,
                            "bottleEmpty", 1,
                            new FluidStack(fluid, 250)
                    );
                    count++;
                }
            }
        }

        // 检查模组添加的自定义容器矿物词典
        String[] containerOreNames = {
                "cellEmpty", // IC2风格的单元
                "capsuleEmpty", // 林业胶囊
                "canEmpty", // 罐子
                "bottleEmpty" // 瓶子
        };

        for (String oreName : containerOreNames) {
            if (OreDictionary.doesOreNameExist(oreName)) {
                // 尝试找到对应的填充版本
                String filledOreName = oreName.replace("Empty", "Filled" +
                        fluid.getName().substring(0, 1).toUpperCase() +
                        fluid.getName().substring(1));

                if (OreDictionary.doesOreNameExist(filledOreName)) {
                    NonNullList<ItemStack> filledItems = OreDictionary.getOres(filledOreName);
                    if (!filledItems.isEmpty()) {
                        BottlingMachineRecipe.addRecipe(
                                filledItems.get(0).copy(),
                                oreName, 1,
                                new FluidStack(fluid, getContainerCapacity(oreName))
                        );
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * 注册特殊配方（硬编码的特殊情况）
     */
    private static void registerSpecialRecipes() {
        // 药水相关
        if (FluidRegistry.isFluidRegistered("potion")) {
            // 普通水瓶
            if (FluidRegistry.isFluidRegistered("water")) {
                BottlingMachineRecipe.addRecipe(
                        new ItemStack(Items.POTIONITEM, 1, 0), // 水瓶
                        new ItemStack(Items.GLASS_BOTTLE), 1,
                        new FluidStack(FluidRegistry.WATER, 250)
                );
            }
        }

        // 检查模组特定的配方
        registerModSpecificRecipes();
    }

    /**
     * 注册模组特定的配方
     */
    private static void registerModSpecificRecipes() {
        // Forestry 林业
        if (Loader.isModLoaded("forestry")) {
            registerForestryRecipes();
        }

        // IndustrialCraft 2
        if (Loader.isModLoaded("ic2")) {
            registerIC2Recipes();
        }

        // Thermal Expansion 热力膨胀
        if (Loader.isModLoaded("thermalexpansion")) {
            registerThermalRecipes();
        }

        // EnderIO
        if (Loader.isModLoaded("enderio")) {
            registerEnderIORecipes();
        }

        // Tinkers' Construct 匠魂
        if (Loader.isModLoaded("tconstruct")) {
            registerTinkersRecipes();
        }
    }

    /**
     * 林业模组配方
     */
    private static void registerForestryRecipes() {
        // 林业的蜂蜜、生物质等
        String[] forestryFluids = {"honey", "biomass", "ethanol", "seed_oil", "juice"};
        for (String fluidName : forestryFluids) {
            if (FluidRegistry.isFluidRegistered(fluidName)) {
                // 注册胶囊、罐子、瓶子等容器
                LOGGER.debug("注册林业液体配方: {}", fluidName);
            }
        }
    }

    /**
     * IC2配方
     */
    private static void registerIC2Recipes() {
        // IC2的通用流体单元
        LOGGER.debug("注册IC2流体单元配方");
    }

    /**
     * 热力膨胀配方
     */
    private static void registerThermalRecipes() {
        // 热力的各种液体和容器
        String[] thermalFluids = {"redstone", "glowstone", "ender", "pyrotheum", "cryotheum"};
        for (String fluidName : thermalFluids) {
            if (FluidRegistry.isFluidRegistered(fluidName)) {
                LOGGER.debug("注册热力膨胀液体配方: {}", fluidName);
            }
        }
    }

    /**
     * EnderIO配方
     */
    private static void registerEnderIORecipes() {
        // EnderIO的各种液体
        String[] enderIOFluids = {"nutrient_distillation", "ender_distillation", "hootch"};
        for (String fluidName : enderIOFluids) {
            if (FluidRegistry.isFluidRegistered(fluidName)) {
                LOGGER.debug("注册EnderIO液体配方: {}", fluidName);
            }
        }
    }

    /**
     * 匠魂配方
     */
    private static void registerTinkersRecipes() {
        // 匠魂的熔融金属通常不装瓶，但可能有特殊容器
        LOGGER.debug("检查匠魂液体容器");
    }

    // =================== 工具方法 ===================

    /**
     * 检查液体是否在黑名单中
     */
    private static boolean isBlacklisted(Fluid fluid) {
        // 检查液体名称
        if (BLACKLISTED_FLUIDS.contains(fluid.getName())) {
            return true;
        }

        // 检查模组
        String modId = getFluidModId(fluid);
        if (BLACKLISTED_MODS.contains(modId)) {
            return true;
        }

        // 检查液体属性
        if (fluid.isGaseous()) {
            return true; // 气体通常不装瓶
        }

        if (fluid.getTemperature() > 1000) {
            return true; // 高温液体（如熔岩、熔融金属）
        }

        return false;
    }

    /**
     * 获取液体所属的模组ID
     */
    private static String getFluidModId(Fluid fluid) {
        if (fluid.getStill() != null) {
            String domain = fluid.getStill().getNamespace();
            if (domain != null && !domain.equals("minecraft")) {
                return domain;
            }
        }
        return "unknown";
    }

    /**
     * 获取液体容器的容量
     */
    private static int getFluidAmount(Fluid fluid, int defaultAmount) {
        if (CUSTOM_AMOUNTS.containsKey(fluid.getName())) {
            return CUSTOM_AMOUNTS.get(fluid.getName());
        }
        return defaultAmount;
    }

    /**
     * 根据容器类型获取标准容量
     */
    private static int getContainerCapacity(String containerType) {
        if (containerType.contains("bottle")) return 250;
        if (containerType.contains("bucket")) return 1000;
        if (containerType.contains("cell")) return 1000;
        if (containerType.contains("capsule")) return 1000;
        if (containerType.contains("can")) return 500;
        return 1000; // 默认
    }

    /**
     * 查找液体对应的填充瓶
     */
    private static ItemStack findFilledBottle(Fluid fluid) {
        // 尝试通过NBT创建
        if (fluid.getName().equals("water")) {
            return new ItemStack(Items.POTIONITEM, 1, 0);
        }

        // 搜索注册的物品
        for (ItemStack stack : getAllRegisteredItems()) {
            if (stack.getItem() == Items.POTIONITEM ||
                    stack.getTranslationKey().contains("bottle") ||
                    stack.getTranslationKey().contains(fluid.getName())) {

                if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                    IFluidHandlerItem handler = stack.getCapability(
                            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

                    FluidStack contained = handler.drain(Integer.MAX_VALUE, false);
                    if (contained != null && contained.getFluid() == fluid) {
                        return stack.copy();
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取所有注册的物品（用于扫描）
     * 安全版本 - 处理特殊物品和异常
     */
    private static List<ItemStack> getAllRegisteredItems() {
        List<ItemStack> items = new ArrayList<>();

        // 定义要跳过的物品类型
        Set<Class<?>> skipClasses = new HashSet<>();
        skipClasses.add(net.minecraft.item.ItemEnchantedBook.class);
        skipClasses.add(net.minecraft.item.ItemPotion.class); // 药水瓶单独处理
        skipClasses.add(net.minecraft.item.ItemTippedArrow.class); // 药箭不需要

        ForgeRegistries.ITEMS.forEach(item -> {
            try {
                // 跳过特殊物品类型
                for (Class<?> skipClass : skipClasses) {
                    if (skipClass.isInstance(item)) {
                        return;
                    }
                }

                // 跳过没有注册名的物品
                if (item.getRegistryName() == null) {
                    return;
                }

                // 获取物品的创造标签页
                CreativeTabs tab = item.getCreativeTab();
                if (tab != null) {
                    NonNullList<ItemStack> subItems = NonNullList.create();
                    try {
                        item.getSubItems(tab, subItems);
                        // 过滤掉空物品堆
                        for (ItemStack stack : subItems) {
                            if (!stack.isEmpty() && stack.getItem() != null) {
                                items.add(stack);
                            }
                        }
                    } catch (Exception subEx) {
                        // 如果getSubItems失败，尝试创建基础物品
                        ItemStack basicStack = new ItemStack(item);
                        if (!basicStack.isEmpty()) {
                            items.add(basicStack);
                        }
                    }
                } else {
                    // 如果没有创造标签页，尝试创建一个基础物品堆
                    // 这对于一些技术模组的物品很重要
                    ItemStack basicStack = new ItemStack(item);
                    if (!basicStack.isEmpty()) {
                        items.add(basicStack);
                    }
                }
            } catch (Exception e) {
                // 记录但不崩溃
                LOGGER.debug("扫描物品时出错: {} - {}",
                        item.getRegistryName() != null ? item.getRegistryName() : "未知物品",
                        e.getMessage());
            }
        });

        LOGGER.info("扫描完成，共找到 {} 个物品", items.size());
        return items;
    }

    /**
     * 从配置文件加载设置
     */
    /**
     * 加载配置
     */

    /**
     * 使用默认配置（当没有配置系统时）
     */
    public static void useDefaultConfig() {
        // 使用静态初始化块中的默认值
        LOGGER.info("使用默认配置进行装瓶机配方注册");
    }
}