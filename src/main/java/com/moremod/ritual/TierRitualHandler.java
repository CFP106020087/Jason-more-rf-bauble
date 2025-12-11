package com.moremod.ritual;

import com.moremod.init.ModItems;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.ItemStackHandler;

import java.util.*;

/**
 * 進階儀式處理器 - 根據祭壇階層提供不同功能
 *
 * 二階儀式：
 * - 詛咒附魔淨化（移除物品上的詛咒附魔）
 * - 武器經驗加速（澄月/勇者之劍/鉅刃劍）
 *
 * 三階儀式：
 * - 附魔轉移（將一件物品的附魔轉移到另一件）
 * - 詛咒創造（創建無效果的詛咒附魔書）
 * - 鬼妖村正攻擊提升
 */
public class TierRitualHandler {

    // ========== 詛咒附魔列表 ==========
    private static final Set<Enchantment> CURSE_ENCHANTMENTS = new HashSet<>();
    static {
        CURSE_ENCHANTMENTS.add(Enchantments.BINDING_CURSE);
        CURSE_ENCHANTMENTS.add(Enchantments.VANISHING_CURSE);
    }

    // ========== NBT標籤 ==========
    private static final String TAG_EXP_BOOST_TICKS = "RitualExpBoostTicks";
    private static final String TAG_EXP_BOOST_MULT = "RitualExpBoostMult";
    private static final String TAG_ATTACK_BOOST_TICKS = "RitualAttackBoostTicks";
    private static final String TAG_ATTACK_BOOST_AMOUNT = "RitualAttackBoostAmount";

    // ========== 詛咒淨化儀式 (二階) ==========

    /**
     * 檢查是否可以進行詛咒淨化儀式
     * 條件：二階以上 + 中心物品有詛咒附魔 + 基座上有聖水/金蘋果
     */
    public static boolean canPurifyCurse(World world, BlockPos corePos, ItemStack centerItem,
                                         List<ItemStack> pedestalItems, AltarTier tier) {
        // 需要二階以上
        if (tier.getLevel() < 2) return false;

        // 中心物品必須有詛咒附魔
        if (!hasCurseEnchantment(centerItem)) return false;

        // 基座上需要有聖水或金蘋果
        boolean hasHolyItem = false;
        for (ItemStack stack : pedestalItems) {
            if (isHolyPurificationItem(stack)) {
                hasHolyItem = true;
                break;
            }
        }

        return hasHolyItem;
    }

    /**
     * 執行詛咒淨化
     */
    public static PurificationResult performCursePurification(World world, BlockPos corePos,
                                                              ItemStack centerItem, AltarTier tier) {
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(centerItem);
        List<Enchantment> cursesToRemove = new ArrayList<>();

        for (Enchantment ench : enchants.keySet()) {
            if (ench.isCurse()) {
                cursesToRemove.add(ench);
            }
        }

        if (cursesToRemove.isEmpty()) {
            return new PurificationResult(false, 0, "物品沒有詛咒附魔");
        }

        // 根據階層決定移除數量
        int maxRemove = tier.getLevel() >= 3 ? cursesToRemove.size() : 1;
        int removed = 0;

        for (int i = 0; i < maxRemove && i < cursesToRemove.size(); i++) {
            enchants.remove(cursesToRemove.get(i));
            removed++;
        }

        EnchantmentHelper.setEnchantments(enchants, centerItem);

        // 播放效果
        spawnPurificationEffects(world, corePos);

        return new PurificationResult(true, removed, null);
    }

    public static class PurificationResult {
        public final boolean success;
        public final int removedCount;
        public final String errorMessage;

        public PurificationResult(boolean success, int removedCount, String errorMessage) {
            this.success = success;
            this.removedCount = removedCount;
            this.errorMessage = errorMessage;
        }
    }

    // ========== 附魔轉移儀式 (三階) ==========

    /**
     * 檢查是否可以進行附魔轉移
     * 條件：三階 + 中心有附魔物品 + 一個基座有目標物品 + 其他基座有催化劑
     */
    public static boolean canTransferEnchantment(World world, BlockPos corePos, ItemStack centerItem,
                                                  List<ItemStack> pedestalItems, AltarTier tier) {
        // 必須是三階
        if (tier.getLevel() < 3) return false;

        // 中心物品必須有附魔
        if (!centerItem.isItemEnchanted()) return false;

        // 基座上需要有目標物品和催化劑（青金石/龍息）
        boolean hasTarget = false;
        boolean hasCatalyst = false;

        for (ItemStack stack : pedestalItems) {
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.DYE && stack.getMetadata() == 4) { // 青金石
                    hasCatalyst = true;
                } else if (stack.getItem() == Items.DRAGON_BREATH) {
                    hasCatalyst = true;
                } else if (!stack.isItemEnchanted()) {
                    // 非附魔物品作為目標
                    hasTarget = true;
                }
            }
        }

        return hasTarget && hasCatalyst;
    }

    /**
     * 執行附魔轉移
     * @param sourceItem 來源物品（附魔會被移除）
     * @param targetItem 目標物品（獲得附魔）
     * @return 轉移的附魔數量
     */
    public static int performEnchantmentTransfer(ItemStack sourceItem, ItemStack targetItem,
                                                  World world, BlockPos corePos) {
        Map<Enchantment, Integer> sourceEnchants = EnchantmentHelper.getEnchantments(sourceItem);
        Map<Enchantment, Integer> targetEnchants = EnchantmentHelper.getEnchantments(targetItem);

        int transferred = 0;

        for (Map.Entry<Enchantment, Integer> entry : sourceEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();

            // 儀式力量強大，無視附魔適用性限制，直接轉移所有附魔
            // 如果目標已有此附魔，取最高等級
            if (targetEnchants.containsKey(ench)) {
                int existingLevel = targetEnchants.get(ench);
                targetEnchants.put(ench, Math.max(existingLevel, level));
            } else {
                targetEnchants.put(ench, level);
            }
            transferred++;
        }

        // 應用附魔到目標
        EnchantmentHelper.setEnchantments(targetEnchants, targetItem);

        // 清除來源的附魔
        EnchantmentHelper.setEnchantments(new HashMap<>(), sourceItem);

        // 如果目標是書，轉換為附魔書
        if (targetItem.getItem() == Items.BOOK && !targetEnchants.isEmpty()) {
            // 需要外部處理轉換
        }

        spawnTransferEffects(world, corePos);

        return transferred;
    }

    // ========== 詛咒創造儀式 ==========

    /**
     * 創建虛假詛咒附魔書
     * 這些詛咒看起來像真的但沒有任何效果
     * @param count 詛咒數量
     */
    public static ItemStack createFakeCurseBook(int count) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        NBTTagCompound nbt = book.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            book.setTagCompound(nbt);
        }

        NBTTagList storedEnchants = new NBTTagList();

        // 添加綁定詛咒
        if (count >= 1) {
            NBTTagCompound curse1 = new NBTTagCompound();
            curse1.setShort("id", (short) Enchantment.getEnchantmentID(Enchantments.BINDING_CURSE));
            curse1.setShort("lvl", (short) 1);
            storedEnchants.appendTag(curse1);
        }

        // 添加消失詛咒
        if (count >= 2) {
            NBTTagCompound curse2 = new NBTTagCompound();
            curse2.setShort("id", (short) Enchantment.getEnchantmentID(Enchantments.VANISHING_CURSE));
            curse2.setShort("lvl", (short) 1);
            storedEnchants.appendTag(curse2);
        }

        nbt.setTag("StoredEnchantments", storedEnchants);

        // 標記為虛假詛咒（用於識別）
        nbt.setBoolean("FakeCurse", true);
        nbt.setInteger("FakeCurseCount", count);

        return book;
    }

    /**
     * 檢查物品是否有虛假詛咒標記
     */
    public static boolean isFakeCurseBook(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return false;
        return stack.getTagCompound().getBoolean("FakeCurse");
    }

    // ========== 武器經驗加速儀式 ==========

    /**
     * 為武器添加經驗加速效果
     * @param weapon 武器物品（澄月/勇者之劍/鉅刃劍）
     * @param tier 祭壇階層
     * @param durationTicks 持續時間（tick）
     */
    public static boolean applyExpBoost(ItemStack weapon, AltarTier tier, int durationTicks) {
        if (weapon.isEmpty()) return false;

        // 檢查是否為支援的武器
        String itemName = weapon.getItem().getRegistryName().toString();
        if (!isExpBoostableWeapon(weapon)) {
            return false;
        }

        NBTTagCompound nbt = weapon.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            weapon.setTagCompound(nbt);
        }

        // 根據階層設置倍率
        float multiplier = 1.0f;
        switch (tier.getLevel()) {
            case 1: multiplier = 1.5f; break;
            case 2: multiplier = 2.0f; break;
            case 3: multiplier = 3.0f; break;
        }

        nbt.setInteger(TAG_EXP_BOOST_TICKS, durationTicks);
        nbt.setFloat(TAG_EXP_BOOST_MULT, multiplier);

        return true;
    }

    /**
     * 獲取武器的經驗加速倍率
     */
    public static float getExpBoostMultiplier(ItemStack weapon) {
        if (weapon.isEmpty() || !weapon.hasTagCompound()) return 1.0f;

        NBTTagCompound nbt = weapon.getTagCompound();
        int ticks = nbt.getInteger(TAG_EXP_BOOST_TICKS);

        if (ticks <= 0) return 1.0f;

        return nbt.getFloat(TAG_EXP_BOOST_MULT);
    }

    /**
     * 更新武器的經驗加速計時器
     */
    public static void tickExpBoost(ItemStack weapon) {
        if (weapon.isEmpty() || !weapon.hasTagCompound()) return;

        NBTTagCompound nbt = weapon.getTagCompound();
        int ticks = nbt.getInteger(TAG_EXP_BOOST_TICKS);

        if (ticks > 0) {
            nbt.setInteger(TAG_EXP_BOOST_TICKS, ticks - 1);
        }
    }

    /**
     * 檢查武器是否支援經驗加速
     */
    public static boolean isExpBoostableWeapon(ItemStack weapon) {
        if (weapon.isEmpty()) return false;

        String className = weapon.getItem().getClass().getSimpleName();
        return className.contains("ChengYue") ||
               className.contains("HeroSword") ||
               className.contains("SawBlade");
    }

    // ========== 鬼妖村正攻擊提升 ==========

    /**
     * 為村正添加攻擊提升效果
     */
    public static boolean applyMuramasaBoost(ItemStack weapon, AltarTier tier, int durationTicks) {
        if (weapon.isEmpty()) return false;

        // 檢查是否為村正
        if (!isMuramasa(weapon)) {
            return false;
        }

        NBTTagCompound nbt = weapon.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            weapon.setTagCompound(nbt);
        }

        // 根據階層設置攻擊加成
        float attackBoost = 0f;
        switch (tier.getLevel()) {
            case 1: attackBoost = 2.0f; break;
            case 2: attackBoost = 5.0f; break;
            case 3: attackBoost = 10.0f; break;
        }

        nbt.setInteger(TAG_ATTACK_BOOST_TICKS, durationTicks);
        nbt.setFloat(TAG_ATTACK_BOOST_AMOUNT, attackBoost);

        return true;
    }

    /**
     * 獲取村正的攻擊加成
     */
    public static float getMuramasaBoost(ItemStack weapon) {
        if (weapon.isEmpty() || !weapon.hasTagCompound()) return 0f;

        NBTTagCompound nbt = weapon.getTagCompound();
        int ticks = nbt.getInteger(TAG_ATTACK_BOOST_TICKS);

        if (ticks <= 0) return 0f;

        return nbt.getFloat(TAG_ATTACK_BOOST_AMOUNT);
    }

    /**
     * 更新村正攻擊加成計時器
     */
    public static void tickMuramasaBoost(ItemStack weapon) {
        if (weapon.isEmpty() || !weapon.hasTagCompound()) return;

        NBTTagCompound nbt = weapon.getTagCompound();
        int ticks = nbt.getInteger(TAG_ATTACK_BOOST_TICKS);

        if (ticks > 0) {
            nbt.setInteger(TAG_ATTACK_BOOST_TICKS, ticks - 1);
        }
    }

    /**
     * 檢查是否為村正（能量劍）
     */
    public static boolean isMuramasa(ItemStack weapon) {
        if (weapon.isEmpty()) return false;

        String regName = weapon.getItem().getRegistryName().toString().toLowerCase();
        // 村正 = 能量劍 (energy_sword)
        return regName.contains("muramasa") || regName.contains("村正") || regName.contains("energy_sword");
    }

    // ========== 輔助方法 ==========

    private static boolean hasCurseEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (Enchantment ench : enchants.keySet()) {
            if (ench.isCurse()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHolyPurificationItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 聖水
        if (stack.getItem().getRegistryName().toString().contains("holy_water")) {
            return true;
        }

        // 金蘋果
        if (stack.getItem() == Items.GOLDEN_APPLE) {
            return true;
        }

        // 龍息
        if (stack.getItem() == Items.DRAGON_BREATH) {
            return true;
        }

        // 地獄之星
        if (stack.getItem() == Items.NETHER_STAR) {
            return true;
        }

        return false;
    }

    private static void spawnPurificationEffects(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 神聖光芒粒子
        ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            30, 0.5, 0.5, 0.5, 0.1);

        ws.spawnParticle(EnumParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            20, 0.3, 0.8, 0.3, 0.05);

        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 1.5f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 0.8f, 1.2f);
    }

    private static void spawnTransferEffects(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 魔法轉移粒子
        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            50, 1.0, 0.5, 1.0, 0.0);

        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            30, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 0.8f);
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.BLOCKS, 0.5f, 1.0f);
    }

    /**
     * 通知玩家儀式結果
     */
    public static void notifyPlayers(World world, BlockPos pos, String message, TextFormatting color) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(color + message));
        }
    }

    // ========== 織印強化儀式系統 ==========

    // NBT標籤
    private static final String TAG_WOVEN_FABRIC = "WovenFabric";
    private static final String TAG_FABRIC_POWER = "FabricPower";
    private static final String TAG_FABRIC_TIER_BONUS = "FabricTierBonus";
    private static final String TAG_FABRIC_ENERGY_MULT = "FabricEnergyMult";
    private static final String TAG_FABRIC_ABILITY_MULT = "FabricAbilityMult";

    /**
     * 檢查物品是否有織印
     */
    public static boolean hasFabricWeave(ItemStack armor) {
        if (armor.isEmpty() || !armor.hasTagCompound()) return false;
        return armor.getTagCompound().hasKey(TAG_WOVEN_FABRIC);
    }

    /**
     * 檢查是否可以進行織印強化儀式
     * 條件：織印盔甲 + 強化材料（龍息/終界之眼/地獄之星）
     */
    public static boolean canEnhanceFabric(ItemStack armor, List<ItemStack> pedestalItems, AltarTier tier) {
        if (!hasFabricWeave(armor)) return false;

        // 檢查是否已達到當前階層上限
        NBTTagCompound fabricData = armor.getTagCompound().getCompoundTag(TAG_WOVEN_FABRIC);
        int currentTierBonus = fabricData.getInteger(TAG_FABRIC_TIER_BONUS);
        if (currentTierBonus >= tier.getLevel()) {
            return false; // 已達到當前階層上限
        }

        // 需要強化材料
        boolean hasEnhanceMaterial = false;
        for (ItemStack stack : pedestalItems) {
            if (isFabricEnhanceMaterial(stack)) {
                hasEnhanceMaterial = true;
                break;
            }
        }

        return hasEnhanceMaterial;
    }

    /**
     * 執行織印強化
     * @param armor 織印盔甲
     * @param tier 祭壇階層
     * @return 強化結果
     */
    public static FabricEnhanceResult enhanceFabric(ItemStack armor, AltarTier tier, World world, BlockPos pos) {
        if (!hasFabricWeave(armor)) {
            return new FabricEnhanceResult(false, 0, 0, "物品沒有織印");
        }

        NBTTagCompound armorTag = armor.getTagCompound();
        NBTTagCompound fabricData = armorTag.getCompoundTag(TAG_WOVEN_FABRIC);

        int currentTierBonus = fabricData.getInteger(TAG_FABRIC_TIER_BONUS);
        if (currentTierBonus >= tier.getLevel()) {
            return new FabricEnhanceResult(false, 0, 0, "已達到當前祭壇階層上限");
        }

        // 根據階層設置加成
        float energyMult = 1.0f;
        float abilityMult = 1.0f;
        int newPower = 100;

        switch (tier.getLevel()) {
            case 1:
                energyMult = 1.25f;  // 能量+25%
                abilityMult = 1.15f; // 能力+15%
                newPower = 125;
                break;
            case 2:
                energyMult = 1.5f;   // 能量+50%
                abilityMult = 1.3f;  // 能力+30%
                newPower = 150;
                break;
            case 3:
                energyMult = 2.0f;   // 能量+100%
                abilityMult = 1.5f;  // 能力+50%
                newPower = 200;
                break;
        }

        // 更新織印數據
        fabricData.setInteger(TAG_FABRIC_POWER, newPower);
        fabricData.setInteger(TAG_FABRIC_TIER_BONUS, tier.getLevel());
        fabricData.setFloat(TAG_FABRIC_ENERGY_MULT, energyMult);
        fabricData.setFloat(TAG_FABRIC_ABILITY_MULT, abilityMult);

        armorTag.setTag(TAG_WOVEN_FABRIC, fabricData);

        // 播放效果
        spawnFabricEnhanceEffects(world, pos);

        return new FabricEnhanceResult(true, energyMult, abilityMult, null);
    }

    /**
     * 獲取織印能量倍率
     */
    public static float getFabricEnergyMultiplier(ItemStack armor) {
        if (!hasFabricWeave(armor)) return 1.0f;

        NBTTagCompound fabricData = armor.getTagCompound().getCompoundTag(TAG_WOVEN_FABRIC);
        float mult = fabricData.getFloat(TAG_FABRIC_ENERGY_MULT);
        return mult > 0 ? mult : 1.0f;
    }

    /**
     * 獲取織印能力倍率
     */
    public static float getFabricAbilityMultiplier(ItemStack armor) {
        if (!hasFabricWeave(armor)) return 1.0f;

        NBTTagCompound fabricData = armor.getTagCompound().getCompoundTag(TAG_WOVEN_FABRIC);
        float mult = fabricData.getFloat(TAG_FABRIC_ABILITY_MULT);
        return mult > 0 ? mult : 1.0f;
    }

    /**
     * 獲取織印強化等級
     */
    public static int getFabricTierBonus(ItemStack armor) {
        if (!hasFabricWeave(armor)) return 0;

        NBTTagCompound fabricData = armor.getTagCompound().getCompoundTag(TAG_WOVEN_FABRIC);
        return fabricData.getInteger(TAG_FABRIC_TIER_BONUS);
    }

    /**
     * 檢查是否為織印強化材料
     */
    private static boolean isFabricEnhanceMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 龍息
        if (stack.getItem() == Items.DRAGON_BREATH) return true;
        // 終界之眼
        if (stack.getItem() == Items.ENDER_EYE) return true;
        // 地獄之星
        if (stack.getItem() == Items.NETHER_STAR) return true;
        // 海晶碎片
        if (stack.getItem() == Items.PRISMARINE_SHARD) return true;
        // 烈焰粉
        if (stack.getItem() == Items.BLAZE_POWDER) return true;

        return false;
    }

    private static void spawnFabricEnhanceEffects(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 織印強化粒子
        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            40, 0.5, 0.8, 0.5, 0.1);

        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            30, 1.0, 0.5, 1.0, 0.0);

        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            20, 0.3, 0.5, 0.3, 0.2);

        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 0.8f, 0.8f);
    }

    public static class FabricEnhanceResult {
        public final boolean success;
        public final float energyMultiplier;
        public final float abilityMultiplier;
        public final String errorMessage;

        public FabricEnhanceResult(boolean success, float energyMult, float abilityMult, String error) {
            this.success = success;
            this.energyMultiplier = energyMult;
            this.abilityMultiplier = abilityMult;
            this.errorMessage = error;
        }
    }
}
