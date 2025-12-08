package com.moremod.ritual.fabric;

import com.moremod.fabric.system.FabricWeavingSystem;
import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.init.Items;
import net.minecraft.util.text.TextComponentString;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class UniversalFabricRituals {

    public static void registerRituals() {
        // ========== 高级织布（需要虚空纺锤）==========
        // 深渊布料
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new UniversalFabricRecipe("moremod:abyssal_fabric", 400, 5000)
        );

        // 时序布料
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new UniversalFabricRecipe("moremod:chrono_fabric", 600, 8000)
        );

        // 时空布料
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new UniversalFabricRecipe("moremod:spacetime_fabric", 600, 8000)
        );

        // 异界纤维
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new UniversalFabricRecipe("moremod:otherworldly_fiber", 800, 10000)
        );

        // ========== 基础织布（不需要虚空纺锤）==========
        // 坚韧纤维
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new BasicFabricRecipe("moremod:resilient_fiber", 200, 2000)
        );

        // 活力丝线
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new BasicFabricRecipe("moremod:vital_thread", 200, 2000)
        );

        // 轻盈织物
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new BasicFabricRecipe("moremod:light_weave", 200, 2000)
        );

        // 掠食者布料
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new BasicFabricRecipe("moremod:predator_cloth", 200, 2000)
        );

        // 吸魂织带
        RitualInfusionAPI.RITUAL_RECIPES.add(
                new BasicFabricRecipe("moremod:siphon_wrap", 200, 2000)
        );

        System.out.println("[MoreMod] Registered 4 advanced + 5 basic fabric weaving rituals");
    }

    /**
     * 通用布料织入配方 - 修复版
     */
    public static class UniversalFabricRecipe extends RitualInfusionRecipe {

        private final String fabricId;
        private Item fabricItem;  // 延迟查找
        private Item spindleItem; // 延迟查找
        private ItemStack currentCoreItem = ItemStack.EMPTY;
        private List<ItemStack> currentPedestalItems = new ArrayList<>();

        public UniversalFabricRecipe(String fabricId, int time, int energy) {
            super(
                    new ArmorIngredient(),
                    createPedestalIngredients(fabricId),
                    new ItemStack(Items.DIAMOND_CHESTPLATE), // 占位输出
                    time,
                    energy,
                    0.05f
            );

            this.fabricId = fabricId;
            // 延迟查找物品，避免注册时序问题
        }

        // 延迟获取布料物品
        private Item getFabricItem() {
            if (fabricItem == null) {
                fabricItem = Item.getByNameOrId(fabricId);
                if (fabricItem == null) {
                    System.err.println("[Fabric Ritual] ERROR: Fabric item not found: " + fabricId);
                }
            }
            return fabricItem;
        }

        // 延迟获取纺锤物品
        private Item getSpindleItem() {
            if (spindleItem == null) {
                spindleItem = Item.getByNameOrId("moremod:void_spindle");
            }
            return spindleItem;
        }

        private static List<Ingredient> createPedestalIngredients(String fabricId) {
            Item fabric = Item.getByNameOrId(fabricId);
            Item spindle = Item.getByNameOrId("moremod:void_spindle");

            if (fabric == null || spindle == null) {
                System.err.println("[Fabric Ritual] Failed to create pedestal ingredients for: " + fabricId);
                // 返回空列表而不是包含EMPTY的列表
                return new ArrayList<>();
            }

            return Arrays.asList(
                    Ingredient.fromStacks(new ItemStack(fabric)),
                    Ingredient.fromStacks(new ItemStack(spindle))
            );
        }

        // 重写以确保返回有效列表
        @Override
        public List<Ingredient> getPedestalItems() {
            List<Ingredient> items = super.getPedestalItems();
            if (items == null || items.isEmpty()) {
                // 延迟创建基座材料
                Item fabric = getFabricItem();
                Item spindle = getSpindleItem();
                if (fabric != null && spindle != null) {
                    this.pedestalItems = Arrays.asList(
                            Ingredient.fromStacks(new ItemStack(fabric)),
                            Ingredient.fromStacks(new ItemStack(spindle))
                    );
                    return this.pedestalItems;
                }
                return new ArrayList<>();
            }
            return items;
        }

        /**
         * 重写核心验证 - 保存当前核心物品
         */
        @Override
        public Ingredient getCore() {
            return new ArmorIngredient() {
                @Override
                public boolean apply(ItemStack stack) {
                    boolean matches = super.apply(stack);
                    if (matches && !stack.isEmpty()) {
                        currentCoreItem = stack.copy();
                        System.out.println("[Fabric] Core item cached: " + stack.getDisplayName());
                    }
                    return matches;
                }
            };
        }

        /**
         * 重写基座匹配 - 保存基座物品
         */
        @Override
        public boolean matchPedestalStacks(List<ItemStack> stacks) {
            boolean matches = super.matchPedestalStacks(stacks);
            if (matches) {
                currentPedestalItems.clear();
                for (ItemStack stack : stacks) {
                    currentPedestalItems.add(stack.copy());
                }
                System.out.println("[Fabric] Pedestal items cached: " + stacks.size());

                // 立即生成输出
                generateDynamicOutput();
            }
            return matches;
        }

        /**
         * 生成动态输出
         */
        private void generateDynamicOutput() {
            if (currentCoreItem.isEmpty() || !(currentCoreItem.getItem() instanceof ItemArmor)) {
                System.err.println("[Fabric] Core item is empty or not armor!");
                return;
            }

            if (FabricWeavingSystem.hasFabric(currentCoreItem)) {
                System.err.println("[Fabric] Core item already has fabric!");
                return;
            }

            // 找到布料物品 - 使用延迟获取
            Item fabric = getFabricItem();
            ItemStack fabricStack = ItemStack.EMPTY;
            for (ItemStack stack : currentPedestalItems) {
                if (fabric != null && stack.getItem() == fabric) {
                    fabricStack = stack;
                    break;
                }
            }

            if (fabricStack.isEmpty()) {
                System.err.println("[Fabric] Fabric not found in pedestal items! fabricItem=" + fabric);
                return;
            }

            // 创建输出
            ItemStack result = currentCoreItem.copy();
            result.setCount(1);

            System.out.println("[Fabric] Attempting to weave fabric into: " + result.getDisplayName());

            boolean success = FabricWeavingSystem.weaveIntoArmor(result, fabricStack);

            if (success) {
                System.out.println("[Fabric] Successfully woven! NBT: " + result.getTagCompound());
                // 直接修改父类的output字段
                this.output = result;
            } else {
                System.err.println("[Fabric] Weaving failed!");
                this.output = ItemStack.EMPTY;
            }
        }

        /**
         * 重写getOutput - 返回动态生成的输出
         */
        @Override
        public ItemStack getOutput() {
            // 如果有动态生成的输出，返回它
            if (this.output != null && !this.output.isEmpty() && FabricWeavingSystem.hasFabric(this.output)) {
                System.out.println("[Fabric] Returning dynamic output: " + this.output.getDisplayName());
                return this.output.copy();
            }

            // 否则返回示例输出（用于JEI显示）
            System.out.println("[Fabric] WARNING: Returning fallback example output! output=" + this.output);
            Item fabric = getFabricItem();
            if (fabric == null) {
                return new ItemStack(Items.DIAMOND_CHESTPLATE);
            }
            ItemStack example = new ItemStack(Items.DIAMOND_CHESTPLATE);
            ItemStack fabricStack = new ItemStack(fabric);
            FabricWeavingSystem.weaveIntoArmor(example, fabricStack);
            return example;
        }
    }

    /**
     * 自定义Ingredient - 只匹配未织入布料的盔甲
     */
    public static class ArmorIngredient extends Ingredient {

        public ArmorIngredient() {
            super(0);
        }

        @Override
        public boolean apply(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }

            if (!(stack.getItem() instanceof ItemArmor)) {
                return false;
            }

            if (FabricWeavingSystem.hasFabric(stack)) {
                return false;
            }

            return true;
        }

        @Override
        public ItemStack[] getMatchingStacks() {
            return new ItemStack[] {
                    new ItemStack(Items.DIAMOND_HELMET),
                    new ItemStack(Items.DIAMOND_CHESTPLATE),
                    new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_BOOTS),
                    new ItemStack(Items.IRON_HELMET),
                    new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_BOOTS)
            };
        }

        @Override
        public boolean isSimple() {
            return false;
        }
    }

    /**
     * 基础织布配方 - 不需要虚空纺锤，只需要布料
     */
    public static class BasicFabricRecipe extends RitualInfusionRecipe {

        private final String fabricId;
        private Item fabricItem;  // 延迟查找
        private ItemStack currentCoreItem = ItemStack.EMPTY;
        private List<ItemStack> currentPedestalItems = new ArrayList<>();

        public BasicFabricRecipe(String fabricId, int time, int energy) {
            super(
                    new ArmorIngredient(),
                    createBasicPedestalIngredients(fabricId),
                    new ItemStack(Items.DIAMOND_CHESTPLATE), // 占位输出
                    time,
                    energy,
                    0.02f // 较低的失败率
            );

            this.fabricId = fabricId;
            // 延迟查找物品，避免注册时序问题
        }

        // 延迟获取布料物品
        private Item getFabricItem() {
            if (fabricItem == null) {
                fabricItem = Item.getByNameOrId(fabricId);
                if (fabricItem == null) {
                    System.err.println("[Basic Fabric Ritual] ERROR: Fabric item not found: " + fabricId);
                }
            }
            return fabricItem;
        }

        private static List<Ingredient> createBasicPedestalIngredients(String fabricId) {
            Item fabric = Item.getByNameOrId(fabricId);

            if (fabric == null) {
                System.err.println("[Basic Fabric Ritual] Failed to create pedestal ingredients for: " + fabricId);
                // 返回空列表而不是null
                return new ArrayList<>();
            }

            // 只需要布料，不需要虚空纺锤
            return Arrays.asList(
                    Ingredient.fromStacks(new ItemStack(fabric))
            );
        }

        // 重写以确保返回有效列表
        @Override
        public List<Ingredient> getPedestalItems() {
            List<Ingredient> items = super.getPedestalItems();
            if (items == null || items.isEmpty()) {
                // 延迟创建基座材料
                Item fabric = getFabricItem();
                if (fabric != null) {
                    this.pedestalItems = Arrays.asList(Ingredient.fromStacks(new ItemStack(fabric)));
                    return this.pedestalItems;
                }
                return new ArrayList<>();
            }
            return items;
        }

        @Override
        public Ingredient getCore() {
            return new ArmorIngredient() {
                @Override
                public boolean apply(ItemStack stack) {
                    boolean matches = super.apply(stack);
                    if (matches && !stack.isEmpty()) {
                        currentCoreItem = stack.copy();
                    }
                    return matches;
                }
            };
        }

        @Override
        public boolean matchPedestalStacks(List<ItemStack> stacks) {
            boolean matches = super.matchPedestalStacks(stacks);
            if (matches) {
                currentPedestalItems.clear();
                for (ItemStack stack : stacks) {
                    currentPedestalItems.add(stack.copy());
                }
                generateDynamicOutput();
            }
            return matches;
        }

        private void generateDynamicOutput() {
            if (currentCoreItem.isEmpty() || !(currentCoreItem.getItem() instanceof ItemArmor)) {
                System.err.println("[Basic Fabric] Core item is empty or not armor!");
                return;
            }

            if (FabricWeavingSystem.hasFabric(currentCoreItem)) {
                System.err.println("[Basic Fabric] Core item already has fabric!");
                return;
            }

            // 使用延迟获取
            Item fabric = getFabricItem();
            ItemStack fabricStack = ItemStack.EMPTY;
            for (ItemStack stack : currentPedestalItems) {
                if (fabric != null && stack.getItem() == fabric) {
                    fabricStack = stack;
                    break;
                }
            }

            if (fabricStack.isEmpty()) {
                System.err.println("[Basic Fabric] Fabric not found in pedestal items! fabricItem=" + fabric);
                return;
            }

            ItemStack result = currentCoreItem.copy();
            result.setCount(1);

            System.out.println("[Basic Fabric] Attempting to weave into: " + result.getDisplayName());
            boolean success = FabricWeavingSystem.weaveIntoArmor(result, fabricStack);

            if (success) {
                System.out.println("[Basic Fabric] Successfully woven!");
                this.output = result;
            } else {
                System.err.println("[Basic Fabric] Weaving failed!");
                this.output = ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack getOutput() {
            if (this.output != null && !this.output.isEmpty() && FabricWeavingSystem.hasFabric(this.output)) {
                System.out.println("[Basic Fabric] Returning dynamic output: " + this.output.getDisplayName());
                return this.output.copy();
            }

            // JEI显示用
            System.out.println("[Basic Fabric] WARNING: Returning fallback example output! output=" + this.output);
            Item fabric = getFabricItem();
            if (fabric == null) {
                return new ItemStack(Items.DIAMOND_CHESTPLATE);
            }
            ItemStack example = new ItemStack(Items.DIAMOND_CHESTPLATE);
            ItemStack fabricStack = new ItemStack(fabric);
            FabricWeavingSystem.weaveIntoArmor(example, fabricStack);
            return example;
        }
    }
}