package com.moremod.tile;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.EntityRitualSeat;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import com.moremod.ritual.AltarTier;
import com.moremod.ritual.LegacyRitualConfig;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import com.moremod.ritual.TierRitualHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import com.moremod.item.ritual.ItemCursedMirror;
import com.moremod.item.ritual.ItemVoidEssence;
import com.moremod.item.ritual.ItemFakePlayerCore;
import com.moremod.init.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileEntityRitualCore extends TileEntity implements ITickable {

    // --- 庫存系統 (核心輸入 + 成品輸出) ---
    private final ItemStackHandler inv = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            // 當物品變更時，立即同步給客戶端 (為了 Renderer 能馬上看到物品變化)
            if (world != null && !world.isRemote) {
                syncToClient();
                // 如果拿走了輸入物品，儀式必須強制中斷
                if (slot == 0 && getStackInSlot(0).isEmpty() && isActive) {
                    reset();
                }
            }
        }
    };

    // --- 運行狀態變數 ---
    private RitualInfusionRecipe activeRecipe;
    private int process = 0;
    private boolean isActive = false;
    private boolean hasEnoughEnergy = false;

    // 祭壇阶层系统
    private AltarTier currentTier = AltarTier.TIER_1;
    private int tierCheckTimer = 0;

    // 嵌入仪式系统
    private boolean embeddingRitualActive = false;
    private int embeddingProgress = 0;
    private static final int EMBEDDING_TIME = 100; // 5秒嵌入时间
    private int embeddingFailFeedbackCooldown = 0; // 防止重复发送失败消息
    private int embeddingFailCounter = 0; // 持续失败计数器，用于自动下马
    private static final int EMBEDDING_FAIL_DISMOUNT_TIME = 100; // 5秒后自动下马

    // 注魔仪式系统 (Enchantment Infusion Ritual)
    private boolean enchantInfusionActive = false;
    private int enchantInfusionProgress = 0;
    // 时间和成功率现在从 LegacyRitualConfig 读取

    // 复制仪式系统 (Duplication Ritual)
    private boolean duplicationRitualActive = false;
    private int duplicationProgress = 0;
    // 时间和成功率现在从 LegacyRitualConfig 读取

    // 灵魂绑定仪式系统 (Soul Binding Ritual) - 从玩家头颅创建假玩家核心
    private boolean soulBindingActive = false;
    private int soulBindingProgress = 0;
    // 时间和成功率现在从 LegacyRitualConfig 读取

    // 詛咒淨化儀式系統 (Curse Purification Ritual) - 二階以上
    private boolean cursePurificationActive = false;
    private int cursePurificationProgress = 0;
    // 时间现在从 LegacyRitualConfig 读取

    // 附魔轉移儀式系統 (Enchantment Transfer Ritual) - 三階
    private boolean enchantTransferActive = false;
    private int enchantTransferProgress = 0;
    // 时间现在从 LegacyRitualConfig 读取

    // 詛咒創造儀式系統 (Curse Creation Ritual)
    private boolean curseCreationActive = false;
    private int curseCreationProgress = 0;
    // 时间现在从 LegacyRitualConfig 读取

    // 武器經驗加速儀式系統 (Weapon Exp Boost Ritual)
    private boolean weaponExpBoostActive = false;
    private int weaponExpBoostProgress = 0;
    private static final int WEAPON_EXP_BOOST_DURATION = 12000; // 10分鐘效果（保留）
    // 仪式时间现在从 LegacyRitualConfig 读取

    // 村正攻擊提升儀式系統 (Muramasa Boost Ritual)
    private boolean muramasaBoostActive = false;
    private int muramasaBoostProgress = 0;
    private static final int MURAMASA_BOOST_DURATION = 12000; // 10分鐘效果（保留）
    // 仪式时间现在从 LegacyRitualConfig 读取

    // 織印強化儀式系統 (Fabric Enhancement Ritual)
    private boolean fabricEnhanceActive = false;
    private int fabricEnhanceProgress = 0;
    // 时间现在从 LegacyRitualConfig 读取

    // 不可破坏仪式系统 (Unbreakable Ritual)
    private boolean unbreakableRitualActive = false;
    private int unbreakableProgress = 0;
    // 时间和成功率现在从 LegacyRitualConfig 读取

    // 灵魂束缚仪式系统 (Soulbound Ritual) - 死亡不掉落
    private boolean soulboundRitualActive = false;
    private int soulboundProgress = 0;
    // 时间和成功率现在从 LegacyRitualConfig 读取

    // 能量超载系统 - 记录仪式开始时的总能量（用于计算超载加成）
    private int initialTotalEnergy = 0;

    // ========== 配置读取辅助方法 ==========

    private int getEnchantInfusionTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.ENCHANT_INFUSION);
    }
    private float getEnchantInfusionSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.ENCHANT_INFUSION);
    }

    private int getDuplicationTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.DUPLICATION);
    }
    private float getDuplicationSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.DUPLICATION);
    }

    private int getSoulBindingTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.SOUL_BINDING);
    }
    private float getSoulBindingSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.SOUL_BINDING);
    }

    private int getCursePurificationTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.CURSE_PURIFICATION);
    }

    private int getEnchantTransferTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.ENCHANT_TRANSFER);
    }
    private float getEnchantTransferSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.ENCHANT_TRANSFER);
    }

    private int getCurseCreationTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.CURSE_CREATION);
    }

    private int getWeaponExpBoostTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.WEAPON_EXP_BOOST);
    }

    private int getMuramasaBoostTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.MURAMASA_BOOST);
    }

    private int getFabricEnhanceTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.FABRIC_ENHANCE);
    }

    private int getUnbreakableTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.UNBREAKABLE);
    }
    private float getUnbreakableSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.UNBREAKABLE);
    }

    private int getSoulboundTime() {
        return LegacyRitualConfig.getDuration(LegacyRitualConfig.SOULBOUND);
    }
    private float getSoulboundSuccessRate() {
        return 1.0f - LegacyRitualConfig.getFailChance(LegacyRitualConfig.SOULBOUND);
    }

    // 成功率显示系统
    private RitualInfusionRecipe lastNotifiedRecipe = null;
    private int successRateDisplayCooldown = 0;
    private static final int SUCCESS_RATE_DISPLAY_INTERVAL = 60; // 每3秒刷新一次成功率显示

    // 用於客戶端平滑渲染的緩存變量
    public float clientRotation = 0;
    public float lastClientRotation = 0;

    // 8個基座位置定義
    private static final BlockPos[] OFFS8 = new BlockPos[]{
            new BlockPos( 3, 0,  0), new BlockPos(-3, 0,  0),
            new BlockPos( 0, 0,  3), new BlockPos( 0, 0, -3),
            new BlockPos( 2, 0,  2), new BlockPos( 2, 0, -2),
            new BlockPos(-2, 0,  2), new BlockPos(-2, 0, -2)
    };

    // --- 核心邏輯循環 ---
    @Override
    public void update() {
        if (world == null) return;

        // 客戶端邏輯：只處理粒子和渲染動畫數據
        if (world.isRemote) {
            updateClientVisuals();
            return;
        }

        // 服務端邏輯

        // 0. 定期检测祭坛阶层
        tierCheckTimer++;
        if (tierCheckTimer >= 40) { // 每2秒检测一次
            tierCheckTimer = 0;
            AltarTier newTier = AltarTier.detectTier(world, pos);
            if (newTier != currentTier) {
                currentTier = newTier;
                notifyTierChange();
            }
        }

        // 0.5 检测嵌入仪式（七咒玩家坐在祭坛上）
        updateEmbeddingRitual();

        // 0.6 检测注魔仪式（三阶祭坛+附魔书）
        if (updateEnchantInfusionRitual()) {
            return; // 注魔仪式进行中，跳过普通配方处理
        }

        // 0.7 检测复制仪式（诅咒之镜+虚空精华）
        if (updateDuplicationRitual()) {
            return; // 复制仪式进行中，跳过普通配方处理
        }

        // 0.8 检测灵魂绑定仪式（玩家头颅+灵魂材料 = 假玩家核心）
        if (updateSoulBindingRitual()) {
            return; // 灵魂绑定仪式进行中，跳过普通配方处理
        }

        // 0.9 检测詛咒淨化儀式（二階+詛咒物品+聖水）
        if (updateCursePurificationRitual()) {
            return;
        }

        // 0.10 检测附魔轉移儀式（三階+附魔物品+目標物品）
        if (updateEnchantTransferRitual()) {
            return;
        }

        // 0.11 检测詛咒創造儀式（墨囊+紙+詛咒材料）
        if (updateCurseCreationRitual()) {
            return;
        }

        // 0.12 检测武器經驗加速儀式（澄月/勇者之劍/鉅刃劍+經驗材料）
        if (updateWeaponExpBoostRitual()) {
            return;
        }

        // 0.13 检测村正攻擊提升儀式
        if (updateMuramasaBoostRitual()) {
            return;
        }

        // 0.14 检测織印強化儀式
        if (updateFabricEnhanceRitual()) {
            return;
        }

        // 0.15 检测不可破坏仪式
        if (updateUnbreakableRitual()) {
            return;
        }

        // 0.16 检测灵魂束缚仪式（死亡不掉落）
        if (updateSoulboundRitual()) {
            return;
        }

        // 1. 查找有效基座 (必須有物品)
        List<TileEntityPedestal> peds = findValidPedestals();
        if (peds.isEmpty()) {
            if (isActive) reset();
            return;
        }

        // 2. 配方匹配邏輯
        // 如果當前沒有激活配方，或輸入物品變了，嘗試尋找新配方
        if (activeRecipe == null || !activeRecipe.getCore().apply(inv.getStackInSlot(0))) {
            activeRecipe = findMatchingRecipe(peds);
            process = 0; // 新配方，進度歸零
            lastNotifiedRecipe = null; // 重置通知状态以便显示新配方信息

            // 能量超载：在仪式开始时记录总能量（此时能量尚未被消耗）
            if (activeRecipe != null) {
                initialTotalEnergy = 0;
                for (TileEntityPedestal ped : peds) {
                    initialTotalEnergy += ped.getEnergy().getEnergyStored();
                }
            }
        }

        // 如果還是找不到配方，歸位
        if (activeRecipe == null) {
            updateState(false, false);
            lastNotifiedRecipe = null;
            return;
        }

        // 显示成功率信息（配方首次匹配或定时刷新）
        successRateDisplayCooldown--;
        if (lastNotifiedRecipe != activeRecipe || successRateDisplayCooldown <= 0) {
            notifySuccessRateInfo(peds, activeRecipe);
            lastNotifiedRecipe = activeRecipe;
            successRateDisplayCooldown = SUCCESS_RATE_DISPLAY_INTERVAL;
        }

        // 3. 運行中完整性檢查 (二次確認)
        // 確保基座數量足夠，且中途沒有被玩家偷走基座上的物品
        if (peds.size() < activeRecipe.getPedestalCount() || !isValidRitualStructure(peds, activeRecipe)) {
            // 為了容錯，如果只是暫時缺電可以不重置，但如果物品沒了就必須重置
            reset();
            return;
        }

        // 4. 能量處理
        int time = Math.max(1, activeRecipe.getTime());
        int energyPerTick = Math.max(1, activeRecipe.getEnergyPerPedestal() / time);

        boolean currentTickEnergy = checkAndConsumeEnergy(peds, activeRecipe, energyPerTick, true); // true = 模擬檢查

        if (!currentTickEnergy) {
            // 能量不足，暫停進度，但不重置配方 (給玩家時間去充電)
            updateState(true, false);
            return;
        }

        // 5. 推進儀式
        // 實際消耗能量
        checkAndConsumeEnergy(peds, activeRecipe, energyPerTick, false);

        process++;
        updateState(true, true); // 狀態：激活且有能量

        // 每秒產生一次音效或服務端粒子包
        if (process % 20 == 0) {
            // 這裡可以發包給客戶端播放特定音效
        }

        // 6. 完成或失敗判定
        if (process >= time) {
            finishRitual(peds);
            reset();
        }
    }

    // --- 輔助邏輯方法 ---

    private void updateClientVisuals() {
        // 更新旋轉角度用於渲染插值
        lastClientRotation = clientRotation;
        clientRotation += (isActive ? 10.0f : 1.0f);

        if (isActive && hasEnoughEnergy) {
            spawnParticles();
        }
    }

    private void spawnParticles() {
        // 在這裡生成更加複雜的粒子效果，例如從基座飛向核心的粒子
        if (world.rand.nextInt(3) == 0) {
            world.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    0, 0.1, 0);
        }
    }

    private boolean checkAndConsumeEnergy(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe, int amount, boolean simulate) {
        int count = recipe.getPedestalCount();
        // 檢查前 N 個基座是否有足夠能量
        for (int i = 0; i < count && i < peds.size(); i++) {
            TileEntityPedestal ped = peds.get(i);
            if (ped.getEnergy().extractEnergy(amount, true) < amount) {
                return false;
            }
        }

        if (!simulate) {
            for (int i = 0; i < count && i < peds.size(); i++) {
                peds.get(i).getEnergy().extractEnergy(amount, false);
            }
        }
        return true;
    }

    /**
     * 特殊仪式（Legacy Ritual）的能量消耗检查与扣除
     *
     * @param ritualId 仪式ID（用于从 LegacyRitualConfig 获取能量配置）
     * @param pedestalCount 参与仪式的基座数量
     * @param duration 仪式总持续时间（tick）
     * @param simulate 是否为模拟检查（true=只检查不扣除）
     * @return true 如果能量足够（或已成功扣除）
     */
    private boolean checkAndConsumeEnergyForLegacyRitual(String ritualId, int pedestalCount, int duration, boolean simulate) {
        List<TileEntityPedestal> peds = findValidPedestals();
        if (peds.isEmpty()) return false;

        // 从配置获取每基座能量消耗
        int totalEnergy = LegacyRitualConfig.getEnergyPerPedestal(ritualId) * pedestalCount;
        if (totalEnergy <= 0) return true; // 不需要能量

        // 计算每tick消耗
        int energyPerTick = Math.max(1, totalEnergy / Math.max(1, duration));

        // 平均分配到各基座
        int perPedestal = Math.max(1, energyPerTick / Math.max(1, peds.size()));

        // 检查每个基座是否有足够能量
        for (TileEntityPedestal ped : peds) {
            if (ped.getEnergy().extractEnergy(perPedestal, true) < perPedestal) {
                return false;
            }
        }

        // 实际扣除
        if (!simulate) {
            for (TileEntityPedestal ped : peds) {
                ped.getEnergy().extractEnergy(perPedestal, false);
            }
        }
        return true;
    }

    private void finishRitual(List<TileEntityPedestal> peds) {
        // 先保存配方引用，因为后续操作可能触发reset()
        RitualInfusionRecipe recipe = activeRecipe;

        // 标记为非活动状态，防止onContentsChanged触发reset
        isActive = false;

        // 使用仪式开始时记录的能量（initialTotalEnergy），而非当前剩余能量
        // 这样超载机制才能正确计算加成
        int totalAvailableEnergy = initialTotalEnergy;

        // 使用能量超载调整后的失败率
        float adjustedFailChance = recipe.getOverloadAdjustedFailChance(currentTier, totalAvailableEnergy);
        float overloadBonus = recipe.getOverloadBonus(totalAvailableEnergy);

        System.out.println("[Ritual] Tier: " + currentTier.getDisplayName() +
                         ", Base fail: " + recipe.getFailChance() +
                         ", Tier adjusted: " + recipe.getAdjustedFailChance(currentTier) +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final fail: " + adjustedFailChance);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 失敗判定 (Risk mechanics)
        if (adjustedFailChance > 0 && world.rand.nextFloat() < adjustedFailChance) {
            System.out.println("[Ritual] Failed! Explosion!");
            doFailExplosion();
            notifyRitualResult(false, false);
        } else {
            // 成功：產生產物
            ItemStack output = recipe.getOutput();
            System.out.println("[Ritual] Recipe output: " + output + " isEmpty=" + output.isEmpty());

            // 检查是否触发效果翻倍
            boolean doubled = currentTier.getDoubleEffectChance() > 0 &&
                            world.rand.nextFloat() < currentTier.getDoubleEffectChance();

            if (output.isEmpty()) {
                System.err.println("[Ritual] ERROR: Output is empty! Recipe class: " + recipe.getClass().getSimpleName());
            } else {
                ItemStack outputCopy = output.copy();
                if (doubled) {
                    outputCopy.setCount(outputCopy.getCount() * 2);
                    System.out.println("[Ritual] DOUBLED! Output count: " + outputCopy.getCount());
                }
                System.out.println("[Ritual] Setting output slot to: " + outputCopy.getDisplayName());
                inv.setStackInSlot(1, outputCopy); // 放入產出槽
            }
            notifyRitualResult(true, doubled);
        }

        // 消耗原材料（使用保存的配方引用）
        consumeIngredientsWithRecipe(peds, recipe);

        // 視覺通知
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        markDirty();
    }

    private void consumeIngredientsWithRecipe(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe) {
        inv.extractItem(0, 1, false); // 消耗核心
        System.out.println("[Ritual] Core item consumed from slot 0");

        // 安全检查
        if (recipe == null) {
            System.err.println("[Ritual] Warning: recipe is null in consumeIngredients!");
            return;
        }

        List<net.minecraft.item.crafting.Ingredient> pedestalItems = recipe.getPedestalItems();
        System.out.println("[Ritual] Pedestal items to consume: " + (pedestalItems != null ? pedestalItems.size() : "null"));

        if (pedestalItems == null || pedestalItems.isEmpty()) {
            System.err.println("[Ritual] Warning: Recipe has null/empty pedestal items!");
            // 仍然尝试消耗基座上的物品（每个基座消耗1个）
            int consumed = 0;
            for (TileEntityPedestal ped : peds) {
                if (!ped.isEmpty()) {
                    ped.consumeOne();
                    consumed++;
                    if (consumed >= 2) break; // 最多消耗2个基座的物品
                }
            }
            System.out.println("[Ritual] Fallback: consumed " + consumed + " pedestal items");
            return;
        }

        List<net.minecraft.item.crafting.Ingredient> needed = new ArrayList<>(pedestalItems);
        // 簡單的匹配消耗邏輯
        for (TileEntityPedestal ped : peds) {
            ItemStack stack = ped.getInv().getStackInSlot(0);
            System.out.println("[Ritual] Checking pedestal with: " + stack.getDisplayName());
            for (int i = 0; i < needed.size(); i++) {
                boolean matches = needed.get(i).apply(stack);
                System.out.println("[Ritual]   - Ingredient " + i + " matches: " + matches);
                if (matches) {
                    ped.consumeOne();
                    needed.remove(i);
                    System.out.println("[Ritual]   - Consumed! Remaining needed: " + needed.size());
                    break;
                }
            }
        }
    }

    private void doFailExplosion() {
        world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 2.0F, true);
        // 可以添加更噁心的懲罰，比如產生咒蝕泥土(Tainted Soil)
    }

    private void reset() {
        process = 0;
        activeRecipe = null;
        initialTotalEnergy = 0; // 重置能量超载记录
        updateState(false, false);
    }

    // 狀態同步優化：只有狀態真的改變時才發包，節省頻寬
    private void updateState(boolean active, boolean energy) {
        if (this.isActive != active || this.hasEnoughEnergy != energy) {
            this.isActive = active;
            this.hasEnoughEnergy = energy;
            markDirty();
            syncToClient();
        }
    }

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    private List<TileEntityPedestal> findValidPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                if (!ped.isEmpty()) list.add(ped);
            }
        }
        return list;
    }

    // 嚴格檢查：確保找到的基座物品真的符合當前配方 (防止中途換物品)
    private boolean isValidRitualStructure(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (TileEntityPedestal p : peds) stacks.add(p.getInv().getStackInSlot(0));
        return recipe.matchPedestalStacks(stacks);
    }

    private RitualInfusionRecipe findMatchingRecipe(List<TileEntityPedestal> peds) {
        List<ItemStack> stacks = new ArrayList<>();
        for (TileEntityPedestal p : peds) stacks.add(p.getInv().getStackInSlot(0));

        for (RitualInfusionRecipe r : RitualInfusionAPI.RITUAL_RECIPES) {
            if (r.getCore().apply(inv.getStackInSlot(0)) && r.matchPedestalStacks(stacks)) {
                // 检查祭坛阶层是否满足配方要求
                if (!r.canCraftAtTier(currentTier)) {
                    // 配方匹配但阶层不足，通知玩家
                    notifyTierTooLow(r.getRequiredTier());
                    continue;
                }
                return r;
            }
        }
        return null;
    }

    /**
     * 通知玩家阶层变化
     */
    private void notifyTierChange() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.GOLD + "祭坛阶层: " + TextFormatting.AQUA + currentTier.getDisplayName() +
                TextFormatting.GRAY + " (成功率+" + (int)(currentTier.getSuccessBonus() * 100) + "%)"), true);
        }
        syncToClient();
    }

    /**
     * 通知玩家阶层不足
     */
    private void notifyTierTooLow(int requiredTier) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.RED + "此配方需要 " + AltarTier.fromLevel(requiredTier).getDisplayName() + "！"), true);
        }
    }

    /**
     * 通知仪式结果
     */
    private void notifyRitualResult(boolean success, boolean doubled) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            if (success) {
                if (doubled) {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "★ 仪式大成功！产出翻倍！★"));
                } else {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 仪式成功完成"));
                }
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 仪式失败！祭坛爆炸！"));
            }
        }
    }

    /**
     * 通知玩家能量超载加成
     */
    private void notifyOverloadBonus(float bonus) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        int bonusPercent = (int)(bonus * 100);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 能量超载！成功率+" + bonusPercent + "%"), true);
        }
    }

    /**
     * 通知玩家当前仪式的成功率信息
     */
    private void notifySuccessRateInfo(List<TileEntityPedestal> peds, RitualInfusionRecipe recipe) {
        // 使用仪式开始时记录的能量（如果已记录），否则计算当前能量
        int totalAvailableEnergy;
        if (initialTotalEnergy > 0) {
            totalAvailableEnergy = initialTotalEnergy;
        } else {
            totalAvailableEnergy = 0;
            for (TileEntityPedestal ped : peds) {
                totalAvailableEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 计算各项数值
        float baseFailChance = recipe.getFailChance();
        float tierAdjustedFailChance = recipe.getAdjustedFailChance(currentTier);
        float overloadBonus = recipe.getOverloadBonus(totalAvailableEnergy);
        float finalFailChance = recipe.getOverloadAdjustedFailChance(currentTier, totalAvailableEnergy);

        // 计算成功率
        int baseSuccessRate = (int)((1.0f - baseFailChance) * 100);
        int tierBonus = (int)(currentTier.getSuccessBonus() * 100);
        int overloadBonusPercent = (int)(overloadBonus * 100);
        int finalSuccessRate = (int)((1.0f - finalFailChance) * 100);

        // 计算能量状态
        int requiredEnergy = recipe.getEnergyPerPedestal() * recipe.getPedestalCount();
        int energyPercent = requiredEnergy > 0 ? (totalAvailableEnergy * 100 / requiredEnergy) : 100;

        // 构建显示消息
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        for (EntityPlayer player : players) {
            StringBuilder sb = new StringBuilder();
            sb.append(TextFormatting.GOLD).append("⚗ ");
            sb.append(TextFormatting.WHITE).append(recipe.getOutput().getDisplayName());
            sb.append(TextFormatting.GRAY).append(" | ");

            // 成功率显示
            TextFormatting successColor = finalSuccessRate >= 90 ? TextFormatting.GREEN :
                                         (finalSuccessRate >= 70 ? TextFormatting.YELLOW :
                                         (finalSuccessRate >= 50 ? TextFormatting.GOLD : TextFormatting.RED));
            sb.append(TextFormatting.GRAY).append("成功:");
            sb.append(successColor).append(finalSuccessRate).append("%");

            // 祭坛加成
            if (tierBonus > 0) {
                sb.append(TextFormatting.AQUA).append(" (+").append(tierBonus).append("% ").append(currentTier.getDisplayName()).append(")");
            }

            // 超载加成
            if (overloadBonusPercent > 0) {
                sb.append(TextFormatting.LIGHT_PURPLE).append(" (+").append(overloadBonusPercent).append("% 超载)");
            }

            // 能量状态
            sb.append(TextFormatting.GRAY).append(" | 能量:");
            TextFormatting energyColor = energyPercent >= 100 ? TextFormatting.GREEN :
                                        (energyPercent >= 50 ? TextFormatting.YELLOW : TextFormatting.RED);
            sb.append(energyColor).append(energyPercent).append("%");

            player.sendStatusMessage(new TextComponentString(sb.toString()), true);
        }
    }

    // ========== 嵌入仪式系统 ==========

    /**
     * 更新嵌入仪式
     * 检测是否有七咒玩家坐在祭坛上，并且祭坛上有诅咒饰品
     */
    private void updateEmbeddingRitual() {
        // 冷却计数器递减
        if (embeddingFailFeedbackCooldown > 0) {
            embeddingFailFeedbackCooldown--;
        }

        // 找到站在祭坛中心的七咒玩家
        EntityPlayer standingPlayer = findStandingCursedPlayer();

        if (standingPlayer == null) {
            // 没有七咒玩家站在祭坛上，重置所有嵌入相关状态
            if (embeddingRitualActive) {
                embeddingRitualActive = false;
                embeddingProgress = 0;
            }
            embeddingFailCounter = 0;
            return;
        }

        // 检查是否满足嵌入条件，获取失败原因
        String failReason = checkEmbeddingConditions(standingPlayer);

        if (failReason != null) {
            // 条件不满足
            if (embeddingRitualActive) {
                embeddingRitualActive = false;
                embeddingProgress = 0;
            }

            // 增加失败计数
            embeddingFailCounter++;

            // 每秒发送一次反馈消息（不要太频繁）
            if (embeddingFailFeedbackCooldown <= 0) {
                standingPlayer.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + failReason
                ), true);
                embeddingFailFeedbackCooldown = 20; // 1秒冷却
            }

            return;
        }

        // 条件满足，重置失败计数
        embeddingFailCounter = 0;

        // 开始/继续嵌入仪式
        if (!embeddingRitualActive) {
            embeddingRitualActive = true;
            embeddingProgress = 0;
            notifyEmbeddingStart(standingPlayer);
        }

        embeddingProgress++;

        // 进度效果
        if (embeddingProgress % 20 == 0) {
            // 每秒显示进度
            int seconds = (EMBEDDING_TIME - embeddingProgress) / 20;
            standingPlayer.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "嵌入仪式进行中... " +
                TextFormatting.GOLD + seconds + "秒"
            ), true);

            // 粒子效果
            spawnEmbeddingParticles();
        }

        // 完成嵌入
        if (embeddingProgress >= EMBEDDING_TIME) {
            performEmbedding(standingPlayer);
            embeddingRitualActive = false;
            embeddingProgress = 0;
        }
    }

    /**
     * 找到站在祭坛中心的七咒玩家
     */
    @Nullable
    private EntityPlayer findStandingCursedPlayer() {
        // 检测站在祭坛核心上方的玩家（Y+1 位置）
        AxisAlignedBB area = new AxisAlignedBB(
            pos.getX(), pos.getY() + 0.5, pos.getZ(),
            pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1
        );

        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        for (EntityPlayer player : players) {
            // 必须有七咒之戒
            if (CurseDeathHook.hasCursedRing(player)) {
                return player;
            }
        }
        return null;
    }

    /**
     * 检查是否可以进行嵌入仪式
     */
    private boolean canPerformEmbedding(EntityPlayer player) {
        return checkEmbeddingConditions(player) == null;
    }

    /**
     * 检查嵌入条件，返回失败原因（如果条件满足则返回null）
     */
    @Nullable
    private String checkEmbeddingConditions(EntityPlayer player) {
        // 1. 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            return "需要三阶大师祭坛！当前: " + currentTier.getDisplayName();
        }

        // 2. 玩家必须有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) {
            return "需要佩戴七咒之戒！";
        }

        // 3. 输入槽必须有可嵌入的七圣遗物
        ItemStack inputStack = inv.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return "请在祭坛上放置七圣遗物！";
        }

        if (!EmbeddedCurseManager.isEmbeddableSacredRelic(inputStack)) {
            return "祭坛上的物品不是可嵌入的七圣遗物！";
        }

        // 4. 检查遗物类型
        EmbeddedRelicType type = EmbeddedCurseManager.getTypeFromItem(inputStack);
        if (type == null) {
            return "无法识别遗物类型！";
        }

        // 5. 玩家还没有嵌入这个类型的遗物
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, type)) {
            return "你已经嵌入了 " + type.getDisplayName() + "！";
        }

        return null; // 所有条件满足
    }

    /**
     * 执行嵌入
     */
    private void performEmbedding(EntityPlayer player) {
        ItemStack inputStack = inv.getStackInSlot(0);
        EmbeddedRelicType type = EmbeddedCurseManager.getTypeFromItem(inputStack);

        if (type == null) {
            // 类型无效
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "嵌入失败：无法识别遗物类型！"
            ));
            return;
        }

        // 嵌入遗物
        boolean success = EmbeddedCurseManager.embedRelic(player, type);

        if (success) {
            // 消耗物品
            inv.extractItem(0, 1, false);

            // 播放效果
            spawnEmbeddingCompleteEffects(player);

            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "✦ " + type.getDisplayName() + " 已成功嵌入你的灵魂！"
            ));
        }
        // 注意：embedRelic 方法内部已经会发送失败消息
    }

    /**
     * 让玩家坐在祭坛上
     * @return true 如果成功
     */
    public boolean seatPlayer(EntityPlayer player) {
        // 检查基本条件
        if (player.isRiding()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "你已经在骑乘其他东西了！"
            ));
            return false;
        }

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "只有佩戴七咒之戒的人才能进行嵌入仪式..."
            ));
            return false;
        }

        // 检查祭坛阶层
        if (currentTier != AltarTier.TIER_3) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "需要三阶大师祭坛才能进行嵌入仪式！"
            ));
            return false;
        }

        // 检查输入槽
        ItemStack inputStack = inv.getStackInSlot(0);
        if (!EmbeddedCurseManager.isEmbeddableSacredRelic(inputStack)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "请先在祭坛上放置七圣遗物！"
            ));
            return false;
        }

        // 检查是否已嵌入
        EmbeddedRelicType type = EmbeddedCurseManager.getTypeFromItem(inputStack);
        if (type != null && EmbeddedCurseManager.hasEmbeddedRelic(player, type)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "你已经嵌入了 " + type.getDisplayName() + "！"
            ));
            return false;
        }

        // 创建座椅实体
        EntityRitualSeat seat = new EntityRitualSeat(world, pos);
        world.spawnEntity(seat);
        seat.seatPlayer(player);

        return true;
    }

    /**
     * 通知嵌入开始
     */
    private void notifyEmbeddingStart(EntityPlayer player) {
        ItemStack inputStack = inv.getStackInSlot(0);
        EmbeddedRelicType type = EmbeddedCurseManager.getTypeFromItem(inputStack);

        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_PURPLE + "════════════════════════════════"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "嵌入仪式开始..."
        ));
        if (type != null) {
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "目标: " + type.getDisplayName()
            ));
        }
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "请保持静止，等待仪式完成"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_PURPLE + "════════════════════════════════"
        ));

        // 播放开始音效
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /**
     * 嵌入粒子效果
     */
    private void spawnEmbeddingParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 紫色粒子从四周向中心汇聚
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                x, pos.getY() + 1.5, z,
                5, 0.1, 0.1, 0.1, 0.0);
        }

        // 中心上升粒子
        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            10, 0.5, 0.5, 0.5, 0.0);
    }

    /**
     * 嵌入完成效果
     */
    private void spawnEmbeddingCompleteEffects(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 大量粒子爆发
        ws.spawnParticle(EnumParticleTypes.TOTEM,
            player.posX, player.posY + 1.0, player.posZ,
            100, 0.5, 1.0, 0.5, 0.5);

        ws.spawnParticle(EnumParticleTypes.PORTAL,
            player.posX, player.posY + 1.0, player.posZ,
            50, 0.3, 0.5, 0.3, 0.1);

        // 音效
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.PLAYERS, 1.0f, 0.5f);
    }

    // ========== 注魔仪式系统 (Enchantment Infusion) ==========

    /**
     * 更新注魔仪式
     * 三阶祭坛 + 中心物品 + 周围全是附魔书 = 注魔仪式
     * @return true 如果正在进行注魔仪式
     */
    private boolean updateEnchantInfusionRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.ENCHANT_INFUSION)) {
            if (enchantInfusionActive) {
                enchantInfusionActive = false;
                enchantInfusionProgress = 0;
            }
            return false;
        }

        // 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            if (enchantInfusionActive) {
                enchantInfusionActive = false;
                enchantInfusionProgress = 0;
            }
            return false;
        }

        // 检查是否满足注魔条件
        List<TileEntityPedestal> bookPedestals = findEnchantedBookPedestals();
        ItemStack coreItem = inv.getStackInSlot(0);

        if (bookPedestals.isEmpty() || coreItem.isEmpty()) {
            if (enchantInfusionActive) {
                enchantInfusionActive = false;
                enchantInfusionProgress = 0;
            }
            return false;
        }

        // 需要至少3本附魔书才能启动仪式
        if (bookPedestals.size() < 3) {
            return false;
        }

        // 开始/继续注魔仪式
        if (!enchantInfusionActive) {
            enchantInfusionActive = true;
            enchantInfusionProgress = 0;
            notifyEnchantInfusionStart(coreItem, bookPedestals.size());

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
            System.out.println("[EnchantInfusion] Recorded initial energy: " + initialTotalEnergy);
        }

        // 能量消耗检查（基于附魔书数量）
        int enchantPedestalCount = bookPedestals.size();
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.ENCHANT_INFUSION, enchantPedestalCount, getEnchantInfusionTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.ENCHANT_INFUSION, enchantPedestalCount, getEnchantInfusionTime(), false);

        enchantInfusionProgress++;

        // 进度效果
        if (enchantInfusionProgress % 20 == 0) {
            int seconds = (getEnchantInfusionTime() - enchantInfusionProgress) / 20;
            notifyEnchantInfusionProgress(seconds, bookPedestals.size());
            spawnEnchantInfusionParticles();
        }

        // 完成注魔
        if (enchantInfusionProgress >= getEnchantInfusionTime()) {
            performEnchantInfusion(coreItem, bookPedestals);
            enchantInfusionActive = false;
            enchantInfusionProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着附魔书的基座
     * 如果有自定义材料配置，则查找匹配自定义材料的基座
     */
    private List<TileEntityPedestal> findEnchantedBookPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();

        // 如果有自定义材料配置，使用配置系统匹配
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.ENCHANT_INFUSION)) {
            List<LegacyRitualConfig.MaterialRequirement> reqs =
                LegacyRitualConfig.getMaterialRequirements(LegacyRitualConfig.ENCHANT_INFUSION);
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty()) {
                        for (LegacyRitualConfig.MaterialRequirement req : reqs) {
                            if (req.matches(stack)) {
                                list.add(ped);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            // 默认：只匹配附魔书
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty() && stack.getItem() == Items.ENCHANTED_BOOK) {
                        list.add(ped);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 执行注魔仪式
     */
    private void performEnchantInfusion(ItemStack coreItem, List<TileEntityPedestal> bookPedestals) {
        // 计算成功率 (基础5%，七咒之戒佩戴者10%)
        float successRate = getEnchantInfusionSuccessRate();

        // 检测附近是否有佩戴七咒之戒的玩家
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        boolean hasCursedRingPlayer = false;
        for (EntityPlayer player : nearbyPlayers) {
            if (CurseDeathHook.hasCursedRing(player)) {
                hasCursedRingPlayer = true;
                break;
            }
        }

        // 七咒之戒佩戴者概率翻倍（5% -> 10%）
        if (hasCursedRingPlayer) {
            successRate = successRate * 2.0f; // 10%
        }

        // 计算能量超载加成
        int pedestalCount = bookPedestals.size();
        float overloadBonus = LegacyRitualConfig.getOverloadBonus(
            LegacyRitualConfig.ENCHANT_INFUSION, pedestalCount, initialTotalEnergy);
        successRate = successRate + overloadBonus;

        System.out.println("[EnchantInfusion] Base success: " + (getEnchantInfusionSuccessRate() * 100) + "%" +
                         ", Cursed ring bonus: " + hasCursedRingPlayer +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final success: " + (int)(successRate * 100) + "%" +
                         ", Initial energy: " + initialTotalEnergy);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 判定成功/失败
        boolean success = world.rand.nextFloat() < successRate;

        if (success) {
            // 成功：收集所有附魔并应用到物品
            Map<Enchantment, Integer> existingEnchants = EnchantmentHelper.getEnchantments(coreItem);
            Map<Enchantment, Integer> newEnchants = new HashMap<>(existingEnchants);

            int totalEnchants = 0;
            for (TileEntityPedestal ped : bookPedestals) {
                ItemStack bookStack = ped.getInv().getStackInSlot(0);
                Map<Enchantment, Integer> bookEnchants = EnchantmentHelper.getEnchantments(bookStack);

                for (Map.Entry<Enchantment, Integer> entry : bookEnchants.entrySet()) {
                    Enchantment ench = entry.getKey();
                    int level = entry.getValue();

                    // 加法形式：叠加等级
                    if (newEnchants.containsKey(ench)) {
                        int existingLevel = newEnchants.get(ench);
                        newEnchants.put(ench, existingLevel + level);
                    } else {
                        newEnchants.put(ench, level);
                    }
                    totalEnchants++;
                }

                // 消耗附魔书
                ped.consumeOne();
            }

            // 应用所有附魔到物品
            EnchantmentHelper.setEnchantments(newEnchants, coreItem);

            // 通知成功
            notifyEnchantInfusionSuccess(coreItem, totalEnchants);
            spawnEnchantSuccessEffects();

        } else {
            // 失败：爆炸，可能损坏物品
            doEnchantFailExplosion();

            // 50%概率毁掉中心物品
            if (world.rand.nextFloat() < 0.5f) {
                inv.setStackInSlot(0, ItemStack.EMPTY);
                notifyEnchantInfusionFailDestroyed();
            } else {
                notifyEnchantInfusionFail();
            }

            // 消耗所有附魔书
            for (TileEntityPedestal ped : bookPedestals) {
                ped.consumeOne();
            }
        }

        syncToClient();
        markDirty();
    }

    /**
     * 注魔失败爆炸
     */
    private void doEnchantFailExplosion() {
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3.0F, true);
    }

    /**
     * 通知注魔开始
     */
    private void notifyEnchantInfusionStart(ItemStack item, int bookCount) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        // 检测是否有佩戴七咒之戒的玩家
        boolean hasCursedRingPlayer = false;
        for (EntityPlayer player : players) {
            if (CurseDeathHook.hasCursedRing(player)) {
                hasCursedRingPlayer = true;
                break;
            }
        }
        String successRateText = hasCursedRingPlayer ? "10% (七咒加持)" : "5%";

        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "════════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "✦ 注魔仪式开始 ✦"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "目标: " + TextFormatting.WHITE + item.getDisplayName()
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "附魔书: " + TextFormatting.GOLD + bookCount + " 本"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 成功率: " + TextFormatting.YELLOW + successRateText
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "════════════════════════════════"
            ));
        }

        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 0.8f);
    }

    /**
     * 通知注魔进度
     */
    private void notifyEnchantInfusionProgress(int secondsLeft, int bookCount) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "注魔仪式进行中... " +
                TextFormatting.GOLD + secondsLeft + "秒 " +
                TextFormatting.GRAY + "(" + bookCount + "本附魔书)"
            ), true);
        }

        // 播放附魔音效
        if (enchantInfusionProgress % 40 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.BLOCKS, 0.5f, 1.2f);
        }
    }

    /**
     * 通知注魔成功
     */
    private void notifyEnchantInfusionSuccess(ItemStack item, int enchantCount) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "★ 注魔成功！★"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + item.getDisplayName() +
                TextFormatting.GRAY + " 获得了 " +
                TextFormatting.AQUA + enchantCount + " 个附魔效果！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "(附魔等级以加法形式叠加)"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════"
            ));
        }
    }

    /**
     * 通知注魔失败
     */
    private void notifyEnchantInfusionFail() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "✗ 注魔失败！祭坛爆炸！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "物品幸免于难，但附魔书全部损毁..."
            ));
        }
    }

    /**
     * 通知注魔失败且物品损毁
     */
    private void notifyEnchantInfusionFailDestroyed() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "✗ 注魔大失败！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "物品在爆炸中被彻底毁灭！"
            ));
        }
    }

    /**
     * 注魔粒子效果
     */
    private void spawnEnchantInfusionParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 附魔符文粒子从基座飞向中心
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                if (!ped.isEmpty() && ped.getInv().getStackInSlot(0).getItem() == Items.ENCHANTED_BOOK) {
                    BlockPos pedPos = pos.add(off);
                    ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                        pedPos.getX() + 0.5, pedPos.getY() + 1.5, pedPos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.0);
                }
            }
        }

        // 中心光柱效果
        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            15, 0.3, 0.5, 0.3, 0.1);
    }

    /**
     * 注魔成功特效
     */
    private void spawnEnchantSuccessEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 金色烟花效果
        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            150, 0.5, 1.0, 0.5, 0.8);

        // 附魔符文爆发
        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            100, 1.0, 1.0, 1.0, 0.5);

        // 音效
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 1.0f, 0.8f);
    }

    // ========== 复制仪式系统 (Duplication Ritual) ==========

    /**
     * 更新复制仪式
     * 三阶祭坛 + 诅咒之镜(含存储物品) + 8个虚空精华 = 复制仪式
     * @return true 如果正在进行复制仪式
     */
    private boolean updateDuplicationRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.DUPLICATION)) {
            if (duplicationRitualActive) {
                duplicationRitualActive = false;
                duplicationProgress = 0;
            }
            return false;
        }

        // 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            if (duplicationRitualActive) {
                duplicationRitualActive = false;
                duplicationProgress = 0;
            }
            return false;
        }

        // 检查中心是否是诅咒之镜且有存储物品
        ItemStack coreItem = inv.getStackInSlot(0);
        if (coreItem.isEmpty() || !(coreItem.getItem() instanceof ItemCursedMirror)) {
            if (duplicationRitualActive) {
                duplicationRitualActive = false;
                duplicationProgress = 0;
            }
            return false;
        }

        if (!ItemCursedMirror.hasStoredItem(coreItem)) {
            return false;
        }

        // 检查周围是否全是虚空精华
        List<TileEntityPedestal> essencePedestals = findVoidEssencePedestals();
        if (essencePedestals.size() < 8) {
            if (duplicationRitualActive) {
                duplicationRitualActive = false;
                duplicationProgress = 0;
            }
            return false;
        }

        // 开始/继续复制仪式
        if (!duplicationRitualActive) {
            duplicationRitualActive = true;
            duplicationProgress = 0;
            ItemStack storedItem = ItemCursedMirror.getStoredItem(coreItem);
            notifyDuplicationStart(storedItem);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
            System.out.println("[Duplication] Recorded initial energy: " + initialTotalEnergy);
        }

        // 能量消耗检查（8个虚空精华基座）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.DUPLICATION, 8, getDuplicationTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.DUPLICATION, 8, getDuplicationTime(), false);

        duplicationProgress++;

        // 进度效果
        if (duplicationProgress % 20 == 0) {
            int seconds = (getDuplicationTime() - duplicationProgress) / 20;
            notifyDuplicationProgress(seconds);
            spawnDuplicationParticles();
        }

        // 完成复制
        if (duplicationProgress >= getDuplicationTime()) {
            performDuplication(coreItem, essencePedestals);
            duplicationRitualActive = false;
            duplicationProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着虚空精华的基座
     * 如果有自定义材料配置，则查找匹配自定义材料的基座
     */
    private List<TileEntityPedestal> findVoidEssencePedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();

        // 如果有自定义材料配置，使用配置系统匹配
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.DUPLICATION)) {
            List<LegacyRitualConfig.MaterialRequirement> reqs =
                LegacyRitualConfig.getMaterialRequirements(LegacyRitualConfig.DUPLICATION);
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty()) {
                        for (LegacyRitualConfig.MaterialRequirement req : reqs) {
                            if (req.matches(stack)) {
                                list.add(ped);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            // 默认：只匹配虚空精华
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty() && stack.getItem() == ModItems.VOID_ESSENCE) {
                        list.add(ped);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 执行复制仪式
     */
    private void performDuplication(ItemStack mirrorStack, List<TileEntityPedestal> essencePedestals) {
        ItemStack storedItem = ItemCursedMirror.getStoredItem(mirrorStack);
        if (storedItem.isEmpty()) return;

        // 消耗所有虚空精华
        for (TileEntityPedestal ped : essencePedestals) {
            ped.consumeOne();
        }

        // 计算能量超载加成
        int pedestalCount = essencePedestals.size();
        float overloadBonus = LegacyRitualConfig.getOverloadBonus(
            LegacyRitualConfig.DUPLICATION, pedestalCount, initialTotalEnergy);
        float finalSuccessRate = getDuplicationSuccessRate() + overloadBonus;

        System.out.println("[Duplication] Base success: " + (getDuplicationSuccessRate() * 100) + "%" +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final success: " + (int)(finalSuccessRate * 100) + "%" +
                         ", Initial energy: " + initialTotalEnergy);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 判定成功/失败
        boolean success = world.rand.nextFloat() < finalSuccessRate;

        if (success) {
            // 成功：复制物品，保留原物品
            ItemStack duplicated = storedItem.copy();

            // 将复制品放入输出槽或掉落
            if (inv.getStackInSlot(1).isEmpty()) {
                inv.setStackInSlot(1, duplicated);
            } else {
                // 掉落在祭坛上方
                net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                    world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, duplicated);
                world.spawnEntity(entityItem);
            }

            // 镜子损耗
            mirrorStack.damageItem(1, null);

            notifyDuplicationSuccess(storedItem);
            spawnDuplicationSuccessEffects();

        } else {
            // 失败：毁掉镜中物品
            ItemCursedMirror.clearStoredItem(mirrorStack);

            // 镜子损耗更大
            mirrorStack.damageItem(3, null);

            // 爆炸
            doDuplicationFailExplosion();
            notifyDuplicationFail(storedItem);
        }

        // 如果镜子损坏，从槽位移除
        if (mirrorStack.getItemDamage() >= mirrorStack.getMaxDamage()) {
            inv.setStackInSlot(0, ItemStack.EMPTY);
            notifyMirrorDestroyed();
        }

        syncToClient();
        markDirty();
    }

    /**
     * 复制失败爆炸
     */
    private void doDuplicationFailExplosion() {
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 4.0F, true);
    }

    /**
     * 通知复制开始
     */
    private void notifyDuplicationStart(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠ 禁忌复制仪式开始 ☠"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "目标: " + TextFormatting.WHITE + item.getDisplayName()
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 成功率: " + TextFormatting.DARK_RED + "1%"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "失败将毁掉镜中物品！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
        }

        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.BLOCKS, 0.5f, 0.5f);
    }

    /**
     * 通知复制进度
     */
    private void notifyDuplicationProgress(int secondsLeft) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "禁忌仪式进行中... " +
                TextFormatting.RED + secondsLeft + "秒"
            ), true);
        }

        // 播放不祥音效
        if (duplicationProgress % 60 == 0) {
            world.playSound(null, pos, SoundEvents.ENTITY_ENDERDRAGON_GROWL,
                SoundCategory.BLOCKS, 0.3f, 0.5f);
        }
    }

    /**
     * 通知复制成功
     */
    private void notifyDuplicationSuccess(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "★★★ 奇迹发生！复制成功！★★★"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + item.getDisplayName() +
                TextFormatting.GREEN + " 被完美复制！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
        }
    }

    /**
     * 通知复制失败
     */
    private void notifyDuplicationFail(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "✗ 复制失败！禁忌之力失控！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + item.getDisplayName() + " 在虚空中消散..."
            ));
        }
    }

    /**
     * 通知镜子损毁
     */
    private void notifyMirrorDestroyed() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "诅咒之镜碎裂了..."
            ));
        }
    }

    /**
     * 复制粒子效果
     */
    private void spawnDuplicationParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 黑色粒子漩涡
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + (duplicationProgress * 0.1);
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                x, pos.getY() + 1.0, z,
                5, 0.1, 0.3, 0.1, 0.0);
        }

        // 中心黑暗粒子
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            10, 0.3, 0.5, 0.3, 0.02);
    }

    /**
     * 复制成功特效
     */
    private void spawnDuplicationSuccessEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 金色爆发
        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            200, 1.0, 1.5, 1.0, 1.0);

        // 音效
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.BLOCKS, 1.0f, 0.8f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    // --- 標準 Getters & Setters ---
    public boolean isActive() { return isActive; }
    public int getProgress() { return process; }
    public int getMaxTime() { return activeRecipe != null ? activeRecipe.getTime() : 100; }
    public ItemStackHandler getInv() { return inv; }
    public BlockPos[] getPedestalOffsets() { return OFFS8; }
    public AltarTier getCurrentTier() { return currentTier; }
    public boolean isEmbeddingActive() { return embeddingRitualActive; }
    public int getEmbeddingProgress() { return embeddingProgress; }
    public boolean isEnchantInfusionActive() { return enchantInfusionActive; }
    public int getEnchantInfusionProgress() { return enchantInfusionProgress; }
    public boolean isDuplicationActive() { return duplicationRitualActive; }
    public int getDuplicationProgress() { return duplicationProgress; }
    public boolean isSoulBindingActive() { return soulBindingActive; }
    public int getSoulBindingProgress() { return soulBindingProgress; }

    // --- 渲染關鍵優化 ---

    /**
     * [重要] 無限渲染邊界
     * 這是為了 Renderer 的光束效果。如果不重寫這個，
     * 當核心方塊在畫面外（但光束還在畫面內）時，光束會消失。
     */
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536.0D; // 讓它在很遠的地方也能被看見
    }

    // --- NBT 與 數據包 ---

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inv", inv.serializeNBT());
        compound.setInteger("Process", process);
        compound.setBoolean("Active", isActive);
        compound.setBoolean("HasEnergy", hasEnoughEnergy);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inv.deserializeNBT(compound.getCompoundTag("Inv"));
        process = compound.getInteger("Process");
        isActive = compound.getBoolean("Active");
        hasEnoughEnergy = compound.getBoolean("HasEnergy");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        // 強制標記渲染更新，確保光束狀態立刻刷新
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    // --- Capability (自動化支持) ---

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inv);
        }
        return super.getCapability(capability, facing);
    }

    // ========== 灵魂绑定仪式系统 (Soul Binding Ritual) ==========

    /**
     * 更新灵魂绑定仪式
     * 三阶祭坛 + 头颅(玩家/骷髅/凋零骷髅) + 灵魂材料 = 假玩家核心
     * @return true 如果正在进行灵魂绑定仪式
     */
    private boolean updateSoulBindingRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.SOUL_BINDING)) {
            if (soulBindingActive) {
                soulBindingActive = false;
                soulBindingProgress = 0;
            }
            return false;
        }

        // 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            if (soulBindingActive) {
                soulBindingActive = false;
                soulBindingProgress = 0;
            }
            return false;
        }

        // 检查中心是否是有效头颅(meta=0骷髅, 1凋零骷髅, 3玩家)
        ItemStack coreItem = inv.getStackInSlot(0);
        if (coreItem.isEmpty() || coreItem.getItem() != Items.SKULL) {
            if (soulBindingActive) {
                soulBindingActive = false;
                soulBindingProgress = 0;
            }
            return false;
        }

        // 检查头颅类型: 0=骷髅, 1=凋零骷髅, 3=玩家
        int skullMeta = coreItem.getMetadata();
        if (skullMeta != 0 && skullMeta != 1 && skullMeta != 3) {
            if (soulBindingActive) {
                soulBindingActive = false;
                soulBindingProgress = 0;
            }
            return false;
        }

        // 获取头颅中的身份信息
        GameProfile skullProfile = getProfileFromSkullStack(coreItem);
        if (skullProfile == null) {
            return false;
        }

        // 检查周围基座是否有正确的灵魂材料
        List<TileEntityPedestal> soulMaterialPedestals = findSoulMaterialPedestals();
        if (soulMaterialPedestals.size() < 4) {
            // 需要至少4个灵魂材料
            return false;
        }

        // 开始/继续灵魂绑定仪式
        if (!soulBindingActive) {
            soulBindingActive = true;
            soulBindingProgress = 0;
            notifySoulBindingStart(skullProfile);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
            System.out.println("[SoulBinding] Recorded initial energy: " + initialTotalEnergy);
        }

        // 能量消耗检查（4个灵魂材料基座）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.SOUL_BINDING, 4, getSoulBindingTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.SOUL_BINDING, 4, getSoulBindingTime(), false);

        soulBindingProgress++;

        // 进度效果
        if (soulBindingProgress % 20 == 0) {
            int seconds = (getSoulBindingTime() - soulBindingProgress) / 20;
            notifySoulBindingProgress(seconds, skullProfile);
            spawnSoulBindingParticles();
        }

        // 完成灵魂绑定
        if (soulBindingProgress >= getSoulBindingTime()) {
            performSoulBinding(coreItem, skullProfile, soulMaterialPedestals);
            soulBindingActive = false;
            soulBindingProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着灵魂材料的基座
     * 灵魂材料：灵魂果实、虚空精华、凝视碎片、灵魂锚点等
     * 如果有自定义材料配置，则查找匹配自定义材料的基座
     */
    private List<TileEntityPedestal> findSoulMaterialPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();

        // 如果有自定义材料配置，使用配置系统匹配
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.SOUL_BINDING)) {
            List<LegacyRitualConfig.MaterialRequirement> reqs =
                LegacyRitualConfig.getMaterialRequirements(LegacyRitualConfig.SOUL_BINDING);
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty()) {
                        for (LegacyRitualConfig.MaterialRequirement req : reqs) {
                            if (req.matches(stack)) {
                                list.add(ped);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            // 默认：使用硬编码灵魂材料
            for (BlockPos off : OFFS8) {
                TileEntity te = world.getTileEntity(pos.add(off));
                if (te instanceof TileEntityPedestal) {
                    TileEntityPedestal ped = (TileEntityPedestal) te;
                    ItemStack stack = ped.getInv().getStackInSlot(0);
                    if (!stack.isEmpty() && isSoulMaterial(stack)) {
                        list.add(ped);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 判断是否是灵魂材料（默认硬编码列表）
     */
    private boolean isSoulMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 灵魂材料：灵魂果实、虚空精华、凝视碎片、灵魂锚点、幽影尘
        return stack.getItem() == ModItems.SOUL_FRUIT ||
               stack.getItem() == ModItems.VOID_ESSENCE ||
               stack.getItem() == ModItems.GAZE_FRAGMENT ||
               stack.getItem() == ModItems.SOUL_ANCHOR ||
               stack.getItem() == ModItems.SPECTRAL_DUST ||
               stack.getItem() == ModItems.ETHEREAL_SHARD;
    }

    /**
     * 执行灵魂绑定仪式
     */
    private void performSoulBinding(ItemStack skullStack, GameProfile profile, List<TileEntityPedestal> materialPedestals) {
        // 消耗所有灵魂材料
        for (TileEntityPedestal ped : materialPedestals) {
            ped.consumeOne();
        }

        // 计算能量超载加成
        int pedestalCount = materialPedestals.size();
        float overloadBonus = LegacyRitualConfig.getOverloadBonus(
            LegacyRitualConfig.SOUL_BINDING, pedestalCount, initialTotalEnergy);
        float finalSuccessRate = getSoulBindingSuccessRate() + overloadBonus;

        System.out.println("[SoulBinding] Base success: " + (getSoulBindingSuccessRate() * 100) + "%" +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final success: " + (int)(finalSuccessRate * 100) + "%" +
                         ", Initial energy: " + initialTotalEnergy);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 判定成功/失败
        boolean success = world.rand.nextFloat() < finalSuccessRate;

        if (success) {
            // 成功：消耗头颅，创建假玩家核心
            inv.setStackInSlot(0, ItemStack.EMPTY);

            // 创建假玩家核心
            ItemStack fakePlayerCore = new ItemStack(ModItems.FAKE_PLAYER_CORE);
            ItemFakePlayerCore.storeProfile(fakePlayerCore, profile);

            // 放入输出槽或掉落
            if (inv.getStackInSlot(1).isEmpty()) {
                inv.setStackInSlot(1, fakePlayerCore);
            } else {
                net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                    world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, fakePlayerCore);
                world.spawnEntity(entityItem);
            }

            notifySoulBindingSuccess(profile);
            spawnSoulBindingSuccessEffects();

        } else {
            // 失败：头颅损毁，爆炸
            inv.setStackInSlot(0, ItemStack.EMPTY);
            doSoulBindingFailExplosion();
            notifySoulBindingFail(profile);
        }

        syncToClient();
        markDirty();
    }

    /**
     * 从头颅物品获取GameProfile
     * 支持: 骷髅头(meta=0), 凋零骷髅头(meta=1), 玩家头颅(meta=3)
     */
    @Nullable
    private GameProfile getProfileFromSkullStack(ItemStack skull) {
        if (skull.isEmpty() || skull.getItem() != Items.SKULL) {
            return null;
        }

        int meta = skull.getMetadata();

        // 玩家头颅 - 尝试获取玩家信息
        if (meta == 3) {
            GameProfile profile = ItemFakePlayerCore.getProfileFromSkull(skull);
            if (profile != null) {
                return profile;
            }
            // 如果没有玩家信息，返回默认玩家profile
            return new GameProfile(
                java.util.UUID.fromString("606e2ff0-ed77-4842-9d6c-e1d3321c7838"),
                "Steve"
            );
        }

        // 骷髅头 - 创建骷髅身份
        if (meta == 0) {
            return new GameProfile(
                java.util.UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5"),
                "Skeleton"
            );
        }

        // 凋零骷髅头 - 创建凋零骷髅身份
        if (meta == 1) {
            return new GameProfile(
                java.util.UUID.fromString("b2b2b2b2-c3c3-d4d4-e5e5-f6f6f6f6f6f6"),
                "Wither_Skeleton"
            );
        }

        return null;
    }

    /**
     * 灵魂绑定失败爆炸
     */
    private void doSoulBindingFailExplosion() {
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2.5F, false);
    }

    /**
     * 通知灵魂绑定开始
     */
    private void notifySoulBindingStart(GameProfile profile) {
        String playerName = profile.getName() != null ? profile.getName() : "Unknown";
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "☠ 灵魂绑定仪式开始 ☠"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "目标灵魂: " + TextFormatting.GOLD + playerName
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 成功率: " + TextFormatting.YELLOW + "50%"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "失败将毁掉头颅！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
        }

        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_AMBIENT,
            SoundCategory.BLOCKS, 0.5f, 0.3f);
    }

    /**
     * 通知灵魂绑定进度
     */
    private void notifySoulBindingProgress(int secondsLeft, GameProfile profile) {
        String playerName = profile.getName() != null ? profile.getName() : "Unknown";
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "灵魂绑定: " +
                TextFormatting.GOLD + playerName +
                TextFormatting.GRAY + " - " +
                TextFormatting.RED + secondsLeft + "秒"
            ), true);
        }

        // 播放灵魂音效
        if (soulBindingProgress % 80 == 0) {
            world.playSound(null, pos, SoundEvents.ENTITY_GHAST_AMBIENT,
                SoundCategory.BLOCKS, 0.3f, 0.5f);
        }
    }

    /**
     * 通知灵魂绑定成功
     */
    private void notifySoulBindingSuccess(GameProfile profile) {
        String playerName = profile.getName() != null ? profile.getName() : "Unknown";
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "★ 灵魂绑定成功！★"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + playerName + TextFormatting.WHITE + " 的灵魂已被封印！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "获得: " + TextFormatting.GOLD + "假玩家核心"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
        }
    }

    /**
     * 通知灵魂绑定失败
     */
    private void notifySoulBindingFail(GameProfile profile) {
        String playerName = profile.getName() != null ? profile.getName() : "Unknown";
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "✗ 灵魂绑定失败！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + playerName + " 的灵魂逃脱了！头颅被毁..."
            ));
        }
    }

    /**
     * 灵魂绑定粒子效果
     */
    private void spawnSoulBindingParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 灵魂漩涡粒子
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + (soulBindingProgress * 0.05);
            double radius = 2.0 - (soulBindingProgress / (float) getSoulBindingTime());
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                x, pos.getY() + 1.5 + Math.sin(angle * 2) * 0.3, z,
                3, 0.05, 0.05, 0.05, 0.0);
        }

        // 中心灵魂粒子
        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            10, 0.2, 0.3, 0.2, 0.0);

        // 头骨效果
        if (soulBindingProgress % 40 == 0) {
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /**
     * 灵魂绑定成功特效
     */
    private void spawnSoulBindingSuccessEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 大量紫色粒子爆发
        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            100, 1.0, 1.5, 1.0, 0.5);

        // 灵魂绿火
        ws.spawnParticle(EnumParticleTypes.FLAME,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            50, 0.5, 0.8, 0.5, 0.05);

        // 金色烟花
        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            80, 0.5, 1.0, 0.5, 0.5);

        // 音效
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_DEATH,
            SoundCategory.BLOCKS, 0.5f, 1.5f);
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    // ========== 詛咒淨化儀式系統 (二階以上) ==========

    /**
     * 更新詛咒淨化儀式
     * 二階以上 + 詛咒物品 + 聖水/金蘋果
     */
    private boolean updateCursePurificationRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.CURSE_PURIFICATION)) {
            if (cursePurificationActive) {
                cursePurificationActive = false;
                cursePurificationProgress = 0;
            }
            return false;
        }

        // 需要二階以上
        if (currentTier.getLevel() < 2) {
            if (cursePurificationActive) {
                cursePurificationActive = false;
                cursePurificationProgress = 0;
            }
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty()) {
            if (cursePurificationActive) resetCursePurification();
            return false;
        }

        // 收集基座物品
        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean materialsValid;
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.CURSE_PURIFICATION)) {
            // 使用自定义材料配置，但仍需检查中心物品是否有诅咒附魔
            materialsValid = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.CURSE_PURIFICATION, pedestalItems)
                          && hasCurseEnchantment(centerItem);
        } else {
            // 默认使用TierRitualHandler检查
            materialsValid = TierRitualHandler.canPurifyCurse(world, pos, centerItem, pedestalItems, currentTier);
        }

        if (!materialsValid) {
            if (cursePurificationActive) resetCursePurification();
            return false;
        }

        // 開始/繼續儀式
        if (!cursePurificationActive) {
            cursePurificationActive = true;
            cursePurificationProgress = 0;
            notifyRitualStart("詛咒淨化", TextFormatting.YELLOW);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.CURSE_PURIFICATION, 4, getCursePurificationTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.CURSE_PURIFICATION, 4, getCursePurificationTime(), false);

        cursePurificationProgress++;

        if (cursePurificationProgress % 20 == 0) {
            int seconds = (getCursePurificationTime() - cursePurificationProgress) / 20;
            notifyRitualProgress("詛咒淨化", seconds, TextFormatting.YELLOW);
            spawnPurificationParticles();
        }

        if (cursePurificationProgress >= getCursePurificationTime()) {
            performCursePurification(centerItem);
            resetCursePurification();
        }

        return true;
    }

    private void resetCursePurification() {
        cursePurificationActive = false;
        cursePurificationProgress = 0;
    }

    private void performCursePurification(ItemStack centerItem) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.CURSE_PURIFICATION);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.CURSE_PURIFICATION, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 詛咒淨化失敗！神聖能量被反噬...", TextFormatting.RED);
                // 消耗材料但不净化
                if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.CURSE_PURIFICATION)) {
                    consumeCustomMaterials(LegacyRitualConfig.CURSE_PURIFICATION);
                } else {
                    consumeAllMatchingPedestalItems(stack -> isHolyItem(stack));
                }
                syncToClient();
                markDirty();
                return;
            }
        }

        TierRitualHandler.PurificationResult result =
            TierRitualHandler.performCursePurification(world, pos, centerItem, currentTier);

        if (result.success) {
            TierRitualHandler.notifyPlayers(world, pos,
                "✓ 成功淨化 " + result.removedCount + " 個詛咒！", TextFormatting.GREEN);
            // 消耗材料（支持CRT自定义）
            if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.CURSE_PURIFICATION)) {
                consumeCustomMaterials(LegacyRitualConfig.CURSE_PURIFICATION);
            } else {
                consumeAllMatchingPedestalItems(stack -> isHolyItem(stack));
            }
        } else {
            TierRitualHandler.notifyPlayers(world, pos,
                "✗ " + result.errorMessage, TextFormatting.RED);
        }

        syncToClient();
        markDirty();
    }

    private void spawnPurificationParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            10, 0.5, 0.5, 0.5, 0.1);
        ws.spawnParticle(EnumParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            5, 0.3, 0.5, 0.3, 0.02);
    }

    // ========== 附魔轉移儀式系統 (三階) ==========

    /**
     * 更新附魔轉移儀式
     * 三階 + 附魔物品（中心）+ 目標物品（基座）+ 青金石（基座）
     */
    private boolean updateEnchantTransferRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.ENCHANT_TRANSFER)) {
            if (enchantTransferActive) resetEnchantTransfer();
            return false;
        }

        // 必須是三階
        if (currentTier != AltarTier.TIER_3) {
            if (enchantTransferActive) resetEnchantTransfer();
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || !centerItem.isItemEnchanted()) {
            if (enchantTransferActive) resetEnchantTransfer();
            return false;
        }

        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean materialsValid;
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.ENCHANT_TRANSFER)) {
            // 使用自定义材料配置，但仍需检查是否有目标物品
            materialsValid = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.ENCHANT_TRANSFER, pedestalItems)
                          && !findTransferTarget().isEmpty();
        } else {
            // 默认使用TierRitualHandler检查
            materialsValid = TierRitualHandler.canTransferEnchantment(world, pos, centerItem, pedestalItems, currentTier);
        }

        if (!materialsValid) {
            if (enchantTransferActive) resetEnchantTransfer();
            return false;
        }

        if (!enchantTransferActive) {
            enchantTransferActive = true;
            enchantTransferProgress = 0;
            notifyRitualStart("附魔轉移", TextFormatting.AQUA);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.ENCHANT_TRANSFER, 4, getEnchantTransferTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.ENCHANT_TRANSFER, 4, getEnchantTransferTime(), false);

        enchantTransferProgress++;

        if (enchantTransferProgress % 20 == 0) {
            int seconds = (getEnchantTransferTime() - enchantTransferProgress) / 20;
            notifyRitualProgress("附魔轉移", seconds, TextFormatting.AQUA);
            spawnTransferParticles();
        }

        if (enchantTransferProgress >= getEnchantTransferTime()) {
            performEnchantTransfer(centerItem);
            resetEnchantTransfer();
        }

        return true;
    }

    private void resetEnchantTransfer() {
        enchantTransferActive = false;
        enchantTransferProgress = 0;
    }

    private void performEnchantTransfer(ItemStack sourceItem) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.ENCHANT_TRANSFER);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.ENCHANT_TRANSFER, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 附魔轉移失敗！魔力共鳴中斷...", TextFormatting.RED);
                // 消耗材料
                if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.ENCHANT_TRANSFER)) {
                    consumeCustomMaterials(LegacyRitualConfig.ENCHANT_TRANSFER);
                } else {
                    consumeAllMatchingPedestalItems(stack ->
                        (stack.getItem() == Items.DYE && stack.getMetadata() == 4) ||
                        stack.getItem() == Items.DRAGON_BREATH);
                }
                syncToClient();
                markDirty();
                return;
            }
        }

        // 找到目標物品
        ItemStack targetItem = findTransferTarget();
        if (targetItem.isEmpty()) {
            TierRitualHandler.notifyPlayers(world, pos, "✗ 找不到目標物品！", TextFormatting.RED);
            return;
        }

        int transferred = TierRitualHandler.performEnchantmentTransfer(sourceItem, targetItem, world, pos);

        if (transferred > 0) {
            TierRitualHandler.notifyPlayers(world, pos,
                "★ 成功轉移 " + transferred + " 個附魔！", TextFormatting.GOLD);
            // 消耗材料（支持CRT自定义）
            if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.ENCHANT_TRANSFER)) {
                consumeCustomMaterials(LegacyRitualConfig.ENCHANT_TRANSFER);
            } else {
                consumeAllMatchingPedestalItems(stack ->
                    (stack.getItem() == Items.DYE && stack.getMetadata() == 4) ||
                    stack.getItem() == Items.DRAGON_BREATH);
            }
        } else {
            TierRitualHandler.notifyPlayers(world, pos, "✗ 無法轉移附魔！", TextFormatting.RED);
        }

        syncToClient();
        markDirty();
    }

    private ItemStack findTransferTarget() {
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                ItemStack stack = ped.getInv().getStackInSlot(0);
                if (!stack.isEmpty() && !stack.isItemEnchanted() &&
                    stack.getItem() != Items.DYE && stack.getItem() != Items.DRAGON_BREATH) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void spawnTransferParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            20, 1.0, 0.5, 1.0, 0.0);
    }

    // ========== 詛咒創造儀式系統 ==========

    /**
     * 更新詛咒創造儀式
     * 書 + 墨囊 + 腐肉/蜘蛛眼
     */
    private boolean updateCurseCreationRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.CURSE_CREATION)) {
            if (curseCreationActive) resetCurseCreation();
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || centerItem.getItem() != Items.BOOK) {
            if (curseCreationActive) resetCurseCreation();
            return false;
        }

        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean materialsValid;
        int curseMaterials = 0;

        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.CURSE_CREATION)) {
            // 使用自定义材料配置
            materialsValid = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.CURSE_CREATION, pedestalItems);
            // 对于自定义材料，默认诅咒数量为1
            curseMaterials = materialsValid ? 1 : 0;
        } else {
            // 默认硬编码检查：需要墨囊和詛咒材料
            boolean hasInk = false;
            for (ItemStack stack : pedestalItems) {
                if (stack.getItem() == Items.DYE && stack.getMetadata() == 0) hasInk = true; // 墨囊
                if (stack.getItem() == Items.ROTTEN_FLESH ||
                    stack.getItem() == Items.SPIDER_EYE ||
                    stack.getItem() == Items.FERMENTED_SPIDER_EYE) {
                    curseMaterials++;
                }
            }
            materialsValid = hasInk && curseMaterials >= 1;
        }

        if (!materialsValid) {
            if (curseCreationActive) resetCurseCreation();
            return false;
        }

        if (!curseCreationActive) {
            curseCreationActive = true;
            curseCreationProgress = 0;
            notifyRitualStart("詛咒創造", TextFormatting.DARK_PURPLE);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.CURSE_CREATION, 4, getCurseCreationTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.CURSE_CREATION, 4, getCurseCreationTime(), false);

        curseCreationProgress++;

        if (curseCreationProgress % 20 == 0) {
            int seconds = (getCurseCreationTime() - curseCreationProgress) / 20;
            notifyRitualProgress("詛咒創造", seconds, TextFormatting.DARK_PURPLE);
            spawnCurseParticles();
        }

        if (curseCreationProgress >= getCurseCreationTime()) {
            performCurseCreation(Math.min(curseMaterials, 2));
            resetCurseCreation();
        }

        return true;
    }

    private void resetCurseCreation() {
        curseCreationActive = false;
        curseCreationProgress = 0;
    }

    private void performCurseCreation(int curseCount) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.CURSE_CREATION);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.CURSE_CREATION, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 詛咒創造失敗！黑暗能量失控...", TextFormatting.RED);
                // 消耗书本
                inv.extractItem(0, 1, false);
                syncToClient();
                markDirty();
                return;
            }
        }

        // 消耗書
        inv.extractItem(0, 1, false);

        // 創建虛假詛咒書
        ItemStack curseBook = TierRitualHandler.createFakeCurseBook(curseCount);

        // 放入輸出槽
        if (inv.getStackInSlot(1).isEmpty()) {
            inv.setStackInSlot(1, curseBook);
        } else {
            // 掉落
            net.minecraft.entity.item.EntityItem entity = new net.minecraft.entity.item.EntityItem(
                world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, curseBook);
            world.spawnEntity(entity);
        }

        // 消耗材料（支持CRT自定义）
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.CURSE_CREATION)) {
            // 使用自定义材料配置消耗
            consumeCustomMaterials(LegacyRitualConfig.CURSE_CREATION);
        } else {
            // 默认材料消耗
            consumeOnePedestalItem(stack -> stack.getItem() == Items.DYE && stack.getMetadata() == 0);
            for (int i = 0; i < curseCount; i++) {
                consumeOnePedestalItem(stack ->
                    stack.getItem() == Items.ROTTEN_FLESH ||
                    stack.getItem() == Items.SPIDER_EYE ||
                    stack.getItem() == Items.FERMENTED_SPIDER_EYE);
            }
        }

        TierRitualHandler.notifyPlayers(world, pos,
            "✦ 創造了帶有 " + curseCount + " 個偽詛咒的附魔書！", TextFormatting.DARK_PURPLE);

        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 0.3f, 1.5f);
        syncToClient();
        markDirty();
    }

    private void spawnCurseParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            15, 0.5, 0.5, 0.5, 0.0);
    }

    // ========== 武器經驗加速儀式系統 ==========

    /**
     * 更新武器經驗加速儀式
     * 澄月/勇者之劍/鉅刃劍 + 經驗瓶/附魔書
     */
    private boolean updateWeaponExpBoostRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.WEAPON_EXP_BOOST)) {
            if (weaponExpBoostActive) resetWeaponExpBoost();
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || !TierRitualHandler.isExpBoostableWeapon(centerItem)) {
            if (weaponExpBoostActive) resetWeaponExpBoost();
            return false;
        }

        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean hasExpMaterial;
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.WEAPON_EXP_BOOST)) {
            // 使用自定义材料配置
            hasExpMaterial = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.WEAPON_EXP_BOOST, pedestalItems);
        } else {
            // 默认硬编码检查：需要經驗瓶或附魔書
            hasExpMaterial = false;
            for (ItemStack stack : pedestalItems) {
                if (stack.getItem() == Items.EXPERIENCE_BOTTLE ||
                    stack.getItem() == Items.ENCHANTED_BOOK ||
                    stack.getItem() == Items.EMERALD) {
                    hasExpMaterial = true;
                    break;
                }
            }
        }

        if (!hasExpMaterial) {
            if (weaponExpBoostActive) resetWeaponExpBoost();
            return false;
        }

        if (!weaponExpBoostActive) {
            weaponExpBoostActive = true;
            weaponExpBoostProgress = 0;
            notifyRitualStart("武器覺醒", TextFormatting.LIGHT_PURPLE);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.WEAPON_EXP_BOOST, 4, getWeaponExpBoostTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.WEAPON_EXP_BOOST, 4, getWeaponExpBoostTime(), false);

        weaponExpBoostProgress++;

        if (weaponExpBoostProgress % 20 == 0) {
            int seconds = (getWeaponExpBoostTime() - weaponExpBoostProgress) / 20;
            notifyRitualProgress("武器覺醒", seconds, TextFormatting.LIGHT_PURPLE);
            spawnWeaponBoostParticles();
        }

        if (weaponExpBoostProgress >= getWeaponExpBoostTime()) {
            performWeaponExpBoost(centerItem);
            resetWeaponExpBoost();
        }

        return true;
    }

    private void resetWeaponExpBoost() {
        weaponExpBoostActive = false;
        weaponExpBoostProgress = 0;
    }

    private void performWeaponExpBoost(ItemStack weapon) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.WEAPON_EXP_BOOST);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.WEAPON_EXP_BOOST, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 武器覺醒失敗！能量共鳴中斷...", TextFormatting.RED);
                // 消耗材料
                if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.WEAPON_EXP_BOOST)) {
                    consumeCustomMaterials(LegacyRitualConfig.WEAPON_EXP_BOOST);
                } else {
                    consumeAllMatchingPedestalItems(stack ->
                        stack.getItem() == Items.EXPERIENCE_BOTTLE ||
                        stack.getItem() == Items.ENCHANTED_BOOK ||
                        stack.getItem() == Items.EMERALD);
                }
                syncToClient();
                markDirty();
                return;
            }
        }

        boolean success = TierRitualHandler.applyExpBoost(weapon, currentTier, WEAPON_EXP_BOOST_DURATION);

        if (success) {
            float mult = 1.0f;
            switch (currentTier.getLevel()) {
                case 1: mult = 1.5f; break;
                case 2: mult = 2.0f; break;
                case 3: mult = 3.0f; break;
            }

            TierRitualHandler.notifyPlayers(world, pos,
                "★ " + weapon.getDisplayName() + " 獲得經驗加速 x" + mult + " (10分鐘)",
                TextFormatting.GOLD);

            // 消耗材料（支持CRT自定义）
            if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.WEAPON_EXP_BOOST)) {
                consumeCustomMaterials(LegacyRitualConfig.WEAPON_EXP_BOOST);
            } else {
                consumeAllMatchingPedestalItems(stack ->
                    stack.getItem() == Items.EXPERIENCE_BOTTLE ||
                    stack.getItem() == Items.ENCHANTED_BOOK ||
                    stack.getItem() == Items.EMERALD);
            }

            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        syncToClient();
        markDirty();
    }

    private void spawnWeaponBoostParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            10, 0.3, 0.5, 0.3, 0.1);
    }

    // ========== 村正攻擊提升儀式系統 ==========

    /**
     * 更新村正攻擊提升儀式
     * 村正 + 凋零骷髏頭/烈焰粉
     */
    private boolean updateMuramasaBoostRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.MURAMASA_BOOST)) {
            if (muramasaBoostActive) resetMuramasaBoost();
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || !TierRitualHandler.isMuramasa(centerItem)) {
            if (muramasaBoostActive) resetMuramasaBoost();
            return false;
        }

        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean hasBoostMaterial;
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.MURAMASA_BOOST)) {
            // 使用自定义材料配置
            hasBoostMaterial = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.MURAMASA_BOOST, pedestalItems);
        } else {
            // 默认硬编码检查：需要攻擊材料
            hasBoostMaterial = false;
            for (ItemStack stack : pedestalItems) {
                if ((stack.getItem() == Items.SKULL && stack.getMetadata() == 1) || // 凋零骷髏頭
                    stack.getItem() == Items.BLAZE_POWDER ||
                    stack.getItem() == Items.NETHER_STAR) {
                    hasBoostMaterial = true;
                    break;
                }
            }
        }

        if (!hasBoostMaterial) {
            if (muramasaBoostActive) resetMuramasaBoost();
            return false;
        }

        if (!muramasaBoostActive) {
            muramasaBoostActive = true;
            muramasaBoostProgress = 0;
            notifyRitualStart("妖刀覺醒", TextFormatting.RED);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.MURAMASA_BOOST, 4, getMuramasaBoostTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.MURAMASA_BOOST, 4, getMuramasaBoostTime(), false);

        muramasaBoostProgress++;

        if (muramasaBoostProgress % 20 == 0) {
            int seconds = (getMuramasaBoostTime() - muramasaBoostProgress) / 20;
            notifyRitualProgress("妖刀覺醒", seconds, TextFormatting.RED);
            spawnMuramasaParticles();
        }

        if (muramasaBoostProgress >= getMuramasaBoostTime()) {
            performMuramasaBoost(centerItem);
            resetMuramasaBoost();
        }

        return true;
    }

    private void resetMuramasaBoost() {
        muramasaBoostActive = false;
        muramasaBoostProgress = 0;
    }

    private void performMuramasaBoost(ItemStack weapon) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.MURAMASA_BOOST);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.MURAMASA_BOOST, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 妖刀覺醒失敗！邪氣反噬...", TextFormatting.RED);
                // 消耗材料
                if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.MURAMASA_BOOST)) {
                    consumeCustomMaterials(LegacyRitualConfig.MURAMASA_BOOST);
                } else {
                    consumeAllMatchingPedestalItems(stack ->
                        (stack.getItem() == Items.SKULL && stack.getMetadata() == 1) ||
                        stack.getItem() == Items.BLAZE_POWDER ||
                        stack.getItem() == Items.NETHER_STAR);
                }
                syncToClient();
                markDirty();
                return;
            }
        }

        boolean success = TierRitualHandler.applyMuramasaBoost(weapon, currentTier, MURAMASA_BOOST_DURATION);

        if (success) {
            float boost = 0f;
            switch (currentTier.getLevel()) {
                case 1: boost = 2.0f; break;
                case 2: boost = 5.0f; break;
                case 3: boost = 10.0f; break;
            }

            TierRitualHandler.notifyPlayers(world, pos,
                "⚔ 村正獲得攻擊加成 +" + boost + " (10分鐘)",
                TextFormatting.RED);

            // 消耗材料（支持CRT自定义）
            if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.MURAMASA_BOOST)) {
                consumeCustomMaterials(LegacyRitualConfig.MURAMASA_BOOST);
            } else {
                consumeAllMatchingPedestalItems(stack ->
                    (stack.getItem() == Items.SKULL && stack.getMetadata() == 1) ||
                    stack.getItem() == Items.BLAZE_POWDER ||
                    stack.getItem() == Items.NETHER_STAR);
            }

            world.playSound(null, pos, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.BLOCKS, 0.5f, 0.5f);
        }

        syncToClient();
        markDirty();
    }

    private void spawnMuramasaParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.REDSTONE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            20, 0.5, 0.5, 0.5, 0.0);
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            5, 0.2, 0.2, 0.2, 0.02);
    }

    // ========== 輔助方法 ==========

    /**
     * 收集所有基座上的物品
     */
    private List<ItemStack> collectPedestalItems() {
        List<ItemStack> items = new ArrayList<>();
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                ItemStack stack = ped.getInv().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }
        return items;
    }

    /**
     * 消耗一個符合條件的基座物品
     */
    private void consumeOnePedestalItem(java.util.function.Predicate<ItemStack> predicate) {
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                ItemStack stack = ped.getInv().getStackInSlot(0);
                if (!stack.isEmpty() && predicate.test(stack)) {
                    ped.consumeOne();
                    return;
                }
            }
        }
    }

    /**
     * 消耗所有符合條件的基座物品
     * 用於特殊儀式需要消耗多個材料的情況
     */
    private void consumeAllMatchingPedestalItems(java.util.function.Predicate<ItemStack> predicate) {
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                ItemStack stack = ped.getInv().getStackInSlot(0);
                if (!stack.isEmpty() && predicate.test(stack)) {
                    ped.consumeOne();
                }
            }
        }
    }

    /**
     * 消耗CRT自定义材料配置中匹配的所有物品
     * 根据材料需求的 count 正确消耗相应数量的物品
     * @param ritualId 仪式ID
     */
    private void consumeCustomMaterials(String ritualId) {
        List<LegacyRitualConfig.MaterialRequirement> requirements =
            LegacyRitualConfig.getMaterialRequirements(ritualId);

        if (requirements.isEmpty()) return;

        // 为每个需求创建剩余消耗计数器
        Map<LegacyRitualConfig.MaterialRequirement, Integer> remainingCounts = new HashMap<>();
        for (LegacyRitualConfig.MaterialRequirement req : requirements) {
            remainingCounts.put(req, req.getCount());
        }

        // 遍历所有基座，按需求数量消耗物品
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                ItemStack stack = ped.getInv().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    // 检查是否匹配任何一个还需要消耗的材料需求
                    for (LegacyRitualConfig.MaterialRequirement req : requirements) {
                        int remaining = remainingCounts.getOrDefault(req, 0);
                        if (remaining > 0 && req.matches(stack)) {
                            ped.consumeOne();
                            remainingCounts.put(req, remaining - 1);
                            break; // 匹配到一个需求即可，继续下一个基座
                        }
                    }
                }
            }
        }
    }

    /**
     * 檢查是否為聖潔物品
     */
    private boolean isHolyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Items.GOLDEN_APPLE ||
               stack.getItem() == Items.DRAGON_BREATH ||
               stack.getItem() == Items.NETHER_STAR ||
               stack.getItem().getRegistryName().toString().contains("holy_water");
    }

    /**
     * 检查物品是否有诅咒附魔
     */
    private boolean hasCurseEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (Enchantment ench : enchants.keySet()) {
            if (ench.isCurse()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通用儀式開始通知
     */
    private void notifyRitualStart(String ritualName, TextFormatting color) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                color + "════════════════════════════════"));
            player.sendMessage(new TextComponentString(
                color + "✦ " + ritualName + "儀式開始 ✦"));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "祭壇階層: " + currentTier.getDisplayName()));
            player.sendMessage(new TextComponentString(
                color + "════════════════════════════════"));
        }
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
    }

    /**
     * 通用儀式進度通知
     */
    private void notifyRitualProgress(String ritualName, int secondsLeft, TextFormatting color) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                color + ritualName + "進行中... " + TextFormatting.GOLD + secondsLeft + "秒"
            ), true);
        }
    }

    // ========== 織印強化儀式系統 ==========

    /**
     * 更新織印強化儀式
     * 織印盔甲 + 強化材料（龍息/終界之眼/地獄之星）
     */
    private boolean updateFabricEnhanceRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.FABRIC_ENHANCE)) {
            if (fabricEnhanceActive) resetFabricEnhance();
            return false;
        }

        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || !TierRitualHandler.hasFabricWeave(centerItem)) {
            if (fabricEnhanceActive) resetFabricEnhance();
            return false;
        }

        List<ItemStack> pedestalItems = collectPedestalItems();

        // 检查材料需求（支持CRT自定义）
        boolean materialsValid;
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.FABRIC_ENHANCE)) {
            // 使用自定义材料配置
            materialsValid = LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.FABRIC_ENHANCE, pedestalItems);
        } else {
            // 默认使用TierRitualHandler检查
            materialsValid = TierRitualHandler.canEnhanceFabric(centerItem, pedestalItems, currentTier);
        }

        if (!materialsValid) {
            if (fabricEnhanceActive) resetFabricEnhance();
            return false;
        }

        if (!fabricEnhanceActive) {
            fabricEnhanceActive = true;
            fabricEnhanceProgress = 0;
            notifyRitualStart("織印強化", TextFormatting.LIGHT_PURPLE);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }

            // 顯示當前階層加成預覽
            String bonusInfo = "";
            switch (currentTier.getLevel()) {
                case 1: bonusInfo = "能量+25% / 能力+15%"; break;
                case 2: bonusInfo = "能量+50% / 能力+30%"; break;
                case 3: bonusInfo = "能量+100% / 能力+50%"; break;
            }
            TierRitualHandler.notifyPlayers(world, pos,
                "預期加成: " + bonusInfo, TextFormatting.AQUA);
        }

        // 能量消耗检查（4个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.FABRIC_ENHANCE, 4, getFabricEnhanceTime(), true)) {
            // 能量不足，暂停进度
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.FABRIC_ENHANCE, 4, getFabricEnhanceTime(), false);

        fabricEnhanceProgress++;

        if (fabricEnhanceProgress % 20 == 0) {
            int seconds = (getFabricEnhanceTime() - fabricEnhanceProgress) / 20;
            notifyRitualProgress("織印強化", seconds, TextFormatting.LIGHT_PURPLE);
            spawnFabricEnhanceParticles();
        }

        if (fabricEnhanceProgress >= getFabricEnhanceTime()) {
            performFabricEnhance(centerItem);
            resetFabricEnhance();
        }

        return true;
    }

    private void resetFabricEnhance() {
        fabricEnhanceActive = false;
        fabricEnhanceProgress = 0;
    }

    private void performFabricEnhance(ItemStack armor) {
        // 检查CRT配置的失败率（默认0%）
        float baseFailChance = LegacyRitualConfig.getFailChance(LegacyRitualConfig.FABRIC_ENHANCE);

        // 如果有失败率，计算超载加成
        if (baseFailChance > 0) {
            int pedestalCount = collectPedestalItems().size();
            float overloadBonus = LegacyRitualConfig.getOverloadBonus(
                LegacyRitualConfig.FABRIC_ENHANCE, pedestalCount, initialTotalEnergy);
            float finalFailChance = Math.max(0, baseFailChance - overloadBonus);

            if (overloadBonus > 0) {
                notifyOverloadBonus(overloadBonus);
            }

            // 随机失败判定
            if (world.rand.nextFloat() < finalFailChance) {
                TierRitualHandler.notifyPlayers(world, pos,
                    "✗ 織印強化失敗！能量不穩定...", TextFormatting.RED);
                // 消耗材料但不强化
                if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.FABRIC_ENHANCE)) {
                    consumeCustomMaterials(LegacyRitualConfig.FABRIC_ENHANCE);
                } else {
                    consumeAllMatchingPedestalItems(stack ->
                        stack.getItem() == Items.DRAGON_BREATH ||
                        stack.getItem() == Items.ENDER_EYE ||
                        stack.getItem() == Items.NETHER_STAR ||
                        stack.getItem() == Items.PRISMARINE_SHARD ||
                        stack.getItem() == Items.BLAZE_POWDER);
                }
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 1.0f);
                syncToClient();
                markDirty();
                return;
            }
        }

        TierRitualHandler.FabricEnhanceResult result =
            TierRitualHandler.enhanceFabric(armor, currentTier, world, pos);

        if (result.success) {
            TierRitualHandler.notifyPlayers(world, pos,
                "★ 織印強化成功！", TextFormatting.GOLD);
            TierRitualHandler.notifyPlayers(world, pos,
                "能量倍率: x" + result.energyMultiplier +
                " / 能力倍率: x" + result.abilityMultiplier, TextFormatting.GREEN);

            // 消耗強化材料（支持CRT自定义）
            if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.FABRIC_ENHANCE)) {
                consumeCustomMaterials(LegacyRitualConfig.FABRIC_ENHANCE);
            } else {
                consumeAllMatchingPedestalItems(stack ->
                    stack.getItem() == Items.DRAGON_BREATH ||
                    stack.getItem() == Items.ENDER_EYE ||
                    stack.getItem() == Items.NETHER_STAR ||
                    stack.getItem() == Items.PRISMARINE_SHARD ||
                    stack.getItem() == Items.BLAZE_POWDER);
            }

            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            TierRitualHandler.notifyPlayers(world, pos,
                "✗ " + result.errorMessage, TextFormatting.RED);
        }

        syncToClient();
        markDirty();
    }

    private void spawnFabricEnhanceParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 織印強化粒子
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + (fabricEnhanceProgress * 0.05);
            double radius = 1.5;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                x, pos.getY() + 1.2, z,
                3, 0.1, 0.1, 0.1, 0.0);
        }

        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            10, 0.5, 0.3, 0.5, 0.0);
    }

    // ========== 不可破坏仪式系统 (Unbreakable Ritual) ==========

    /**
     * 更新不可破坏仪式
     * 三阶祭坛 + 任意有耐久的物品 + 地狱之星×2 + 黑曜石×2 + 钻石×4 = 不可破坏物品
     * @return true 如果正在进行不可破坏仪式
     */
    private boolean updateUnbreakableRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.UNBREAKABLE)) {
            if (unbreakableRitualActive) {
                resetUnbreakableRitual();
            }
            return false;
        }

        // 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            if (unbreakableRitualActive) {
                resetUnbreakableRitual();
            }
            return false;
        }

        // 检查中心物品是否是有耐久的物品
        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty() || !centerItem.isItemStackDamageable()) {
            if (unbreakableRitualActive) {
                resetUnbreakableRitual();
            }
            return false;
        }

        // 检查物品是否已经是不可破坏的
        if (centerItem.hasTagCompound() && centerItem.getTagCompound().getBoolean("Unbreakable")) {
            if (unbreakableRitualActive) {
                resetUnbreakableRitual();
            }
            return false;
        }

        // 检查基座材料：地狱之星×2 + 黑曜石×2 + 钻石×4
        if (!checkUnbreakableMaterials()) {
            if (unbreakableRitualActive) {
                resetUnbreakableRitual();
            }
            return false;
        }

        // 开始/继续不可破坏仪式
        if (!unbreakableRitualActive) {
            unbreakableRitualActive = true;
            unbreakableProgress = 0;
            notifyUnbreakableStart(centerItem);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（8个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.UNBREAKABLE, 8, getUnbreakableTime(), true)) {
            // 能量不足，暂停进度
            updateState(true, false);
            return true;
        }
        // 实际消耗能量
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.UNBREAKABLE, 8, getUnbreakableTime(), false);

        unbreakableProgress++;
        updateState(true, true);

        // 进度效果
        if (unbreakableProgress % 20 == 0) {
            int seconds = (getUnbreakableTime() - unbreakableProgress) / 20;
            notifyUnbreakableProgress(seconds);
            spawnUnbreakableParticles();
        }

        // 完成不可破坏仪式
        if (unbreakableProgress >= getUnbreakableTime()) {
            performUnbreakableRitual(centerItem);
            resetUnbreakableRitual();
        }

        return true;
    }

    private void resetUnbreakableRitual() {
        unbreakableRitualActive = false;
        unbreakableProgress = 0;
    }

    /**
     * 检查不可破坏仪式所需材料
     * 默认: 地狱之星×2 + 黑曜石×2 + 钻石×4
     * 可通过 CraftTweaker 自定义
     */
    private boolean checkUnbreakableMaterials() {
        List<ItemStack> pedestalItems = collectPedestalItems();

        // 如果有自定义材料配置，使用配置系统检查
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.UNBREAKABLE)) {
            return LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.UNBREAKABLE, pedestalItems);
        }

        // 默认硬编码检查
        int netherStarCount = 0;
        int obsidianCount = 0;
        int diamondCount = 0;

        for (ItemStack stack : pedestalItems) {
            if (stack.getItem() == Items.NETHER_STAR) {
                netherStarCount++;
            } else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.OBSIDIAN)) {
                obsidianCount++;
            } else if (stack.getItem() == Items.DIAMOND) {
                diamondCount++;
            }
        }

        return netherStarCount >= 2 && obsidianCount >= 2 && diamondCount >= 4;
    }

    /**
     * 执行不可破坏仪式
     */
    private void performUnbreakableRitual(ItemStack targetItem) {
        int pedestalCount;

        // 消耗材料（支持CRT自定义）
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.UNBREAKABLE)) {
            // 使用自定义材料配置消耗
            consumeCustomMaterials(LegacyRitualConfig.UNBREAKABLE);
            // 统计消耗的材料数量用于能量超载计算
            pedestalCount = 0;
            for (LegacyRitualConfig.MaterialRequirement req : LegacyRitualConfig.getMaterialRequirements(LegacyRitualConfig.UNBREAKABLE)) {
                pedestalCount += req.getCount();
            }
        } else {
            // 默认材料消耗：地狱之星×2 + 黑曜石×2 + 钻石×4
            int starsConsumed = 0;
            int obsidianConsumed = 0;
            int diamondsConsumed = 0;

            // 多轮遍历确保消耗足够数量
            while (starsConsumed < 2 || obsidianConsumed < 2 || diamondsConsumed < 4) {
                boolean consumedAny = false;

                for (BlockPos off : OFFS8) {
                    TileEntity te = world.getTileEntity(pos.add(off));
                    if (te instanceof TileEntityPedestal) {
                        TileEntityPedestal ped = (TileEntityPedestal) te;
                        ItemStack stack = ped.getInv().getStackInSlot(0);
                        if (!stack.isEmpty()) {
                            if (stack.getItem() == Items.NETHER_STAR && starsConsumed < 2) {
                                ped.consumeOne();
                                starsConsumed++;
                                consumedAny = true;
                            } else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.OBSIDIAN) && obsidianConsumed < 2) {
                                ped.consumeOne();
                                obsidianConsumed++;
                                consumedAny = true;
                            } else if (stack.getItem() == Items.DIAMOND && diamondsConsumed < 4) {
                                ped.consumeOne();
                                diamondsConsumed++;
                                consumedAny = true;
                            }
                        }
                    }
                }

                // 防止无限循环（如果没有消耗任何物品则退出）
                if (!consumedAny) break;
            }

            pedestalCount = starsConsumed + obsidianConsumed + diamondsConsumed;
        }

        // 计算能量超载加成
        float overloadBonus = LegacyRitualConfig.getOverloadBonus(
            LegacyRitualConfig.UNBREAKABLE, pedestalCount, initialTotalEnergy);
        float finalSuccessRate = getUnbreakableSuccessRate() + overloadBonus;

        System.out.println("[Unbreakable] Base success: " + (getUnbreakableSuccessRate() * 100) + "%" +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final success: " + (int)(finalSuccessRate * 100) + "%" +
                         ", Initial energy: " + initialTotalEnergy);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 判定成功/失败
        boolean success = world.rand.nextFloat() < finalSuccessRate;

        if (success) {
            // 成功：保留所有NBT，添加Unbreakable标签
            NBTTagCompound nbt = targetItem.hasTagCompound() ? targetItem.getTagCompound() : new NBTTagCompound();
            nbt.setBoolean("Unbreakable", true);
            targetItem.setTagCompound(nbt);

            // 修复耐久（可选）
            targetItem.setItemDamage(0);

            notifyUnbreakableSuccess(targetItem);
            spawnUnbreakableSuccessEffects();
        } else {
            // 失败：物品损坏一半耐久
            int maxDamage = targetItem.getMaxDamage();
            int newDamage = Math.min(targetItem.getItemDamage() + maxDamage / 2, maxDamage - 1);
            targetItem.setItemDamage(newDamage);

            notifyUnbreakableFail(targetItem);
            spawnUnbreakableFailEffects();
        }

        syncToClient();
        markDirty();
    }

    /**
     * 通知不可破坏仪式开始
     */
    private void notifyUnbreakableStart(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "⚒ 不可破坏仪式开始 ⚒"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "目标: " + TextFormatting.WHITE + item.getDisplayName()
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 成功率: " + TextFormatting.YELLOW + "80%"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "物品NBT将完整保留"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "════════════════════════════════"
            ));
        }

        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE,
            SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /**
     * 通知不可破坏仪式进度
     */
    private void notifyUnbreakableProgress(int secondsLeft) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.GOLD + "不可破坏仪式进行中... " +
                TextFormatting.WHITE + secondsLeft + "秒"
            ), true);
        }

        // 播放音效
        if (unbreakableProgress % 40 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
    }

    /**
     * 通知不可破坏仪式成功
     */
    private void notifyUnbreakableSuccess(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "★★★ 仪式成功！★★★"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + item.getDisplayName() +
                TextFormatting.GREEN + " 已变得不可破坏！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "所有NBT数据已完整保留"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════"
            ));
        }
    }

    /**
     * 通知不可破坏仪式失败
     */
    private void notifyUnbreakableFail(ItemStack item) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(10);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "✗ 仪式失败！物品受损！"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + item.getDisplayName() + " 的耐久降低了..."
            ));
        }
    }

    /**
     * 不可破坏仪式粒子效果
     */
    private void spawnUnbreakableParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 金色粒子环绕
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + (unbreakableProgress * 0.08);
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                x, pos.getY() + 1.0, z,
                3, 0.1, 0.2, 0.1, 0.0);
        }

        // 中心上升粒子
        ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            5, 0.2, 0.3, 0.2, 0.0);
    }

    /**
     * 不可破坏仪式成功特效
     */
    private void spawnUnbreakableSuccessEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 金色爆发
        ws.spawnParticle(EnumParticleTypes.TOTEM,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            150, 0.8, 1.0, 0.8, 0.8);

        // 音效
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * 不可破坏仪式失败特效
     */
    private void spawnUnbreakableFailEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 烟雾效果
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            50, 0.5, 0.5, 0.5, 0.1);

        // 失败音效
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK,
            SoundCategory.BLOCKS, 1.0f, 0.8f);
    }

    // Getter for unbreakable ritual
    public boolean isUnbreakableRitualActive() { return unbreakableRitualActive; }
    public int getUnbreakableProgress() { return unbreakableProgress; }

    // ========== 灵魂束缚仪式系统 (Soulbound Ritual) - 死亡不掉落 ==========

    /**
     * 更新灵魂束缚仪式
     * 三阶祭坛 + 任意物品 + 末影珍珠×4 + 恶魂之泪×2 + 金块×2 = 死亡不掉落物品
     * @return true 如果正在进行灵魂束缚仪式
     */
    private boolean updateSoulboundRitual() {
        // 检查仪式是否启用
        if (!LegacyRitualConfig.isEnabled(LegacyRitualConfig.SOULBOUND)) {
            if (soulboundRitualActive) {
                resetSoulboundRitual();
            }
            return false;
        }

        // 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            if (soulboundRitualActive) {
                resetSoulboundRitual();
            }
            return false;
        }

        // 检查中心物品
        ItemStack centerItem = inv.getStackInSlot(0);
        if (centerItem.isEmpty()) {
            if (soulboundRitualActive) {
                resetSoulboundRitual();
            }
            return false;
        }

        // 检查物品是否已经有灵魂束缚
        if (centerItem.hasTagCompound() && centerItem.getTagCompound().getBoolean("Soulbound")) {
            if (soulboundRitualActive) {
                resetSoulboundRitual();
            }
            return false;
        }

        // 检查基座材料：末影珍珠×4 + 恶魂之泪×2 + 金块×2
        if (!checkSoulboundMaterials()) {
            if (soulboundRitualActive) {
                resetSoulboundRitual();
            }
            return false;
        }

        // 开始/继续灵魂束缚仪式
        if (!soulboundRitualActive) {
            soulboundRitualActive = true;
            soulboundProgress = 0;
            notifySoulboundStart(centerItem);

            // 能量超载：记录仪式开始时的总能量
            initialTotalEnergy = 0;
            List<TileEntityPedestal> allPeds = findValidPedestals();
            for (TileEntityPedestal ped : allPeds) {
                initialTotalEnergy += ped.getEnergy().getEnergyStored();
            }
        }

        // 能量消耗检查（8个基座参与）
        if (!checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.SOULBOUND, 8, getSoulboundTime(), true)) {
            updateState(true, false);
            return true;
        }
        checkAndConsumeEnergyForLegacyRitual(LegacyRitualConfig.SOULBOUND, 8, getSoulboundTime(), false);

        soulboundProgress++;
        updateState(true, true);

        // 进度效果
        if (soulboundProgress % 20 == 0) {
            int seconds = (getSoulboundTime() - soulboundProgress) / 20;
            notifySoulboundProgress(seconds);
            spawnSoulboundParticles();
        }

        // 完成灵魂束缚仪式
        if (soulboundProgress >= getSoulboundTime()) {
            performSoulboundRitual(centerItem);
            resetSoulboundRitual();
        }

        return true;
    }

    private void resetSoulboundRitual() {
        soulboundRitualActive = false;
        soulboundProgress = 0;
    }

    /**
     * 检查灵魂束缚仪式所需材料
     * 默认: 末影珍珠×4 + 恶魂之泪×2 + 金块×2
     * 可通过 CraftTweaker 自定义
     */
    private boolean checkSoulboundMaterials() {
        List<ItemStack> pedestalItems = collectPedestalItems();

        // 如果有自定义材料配置，使用配置系统检查
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.SOULBOUND)) {
            return LegacyRitualConfig.checkMaterialRequirements(LegacyRitualConfig.SOULBOUND, pedestalItems);
        }

        // 默认硬编码检查
        int enderPearlCount = 0;
        int ghastTearCount = 0;
        int goldBlockCount = 0;

        for (ItemStack stack : pedestalItems) {
            if (stack.getItem() == Items.ENDER_PEARL) {
                enderPearlCount++;
            } else if (stack.getItem() == Items.GHAST_TEAR) {
                ghastTearCount++;
            } else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.GOLD_BLOCK)) {
                goldBlockCount++;
            }
        }

        return enderPearlCount >= 4 && ghastTearCount >= 2 && goldBlockCount >= 2;
    }

    /**
     * 执行灵魂束缚仪式
     */
    private void performSoulboundRitual(ItemStack targetItem) {
        int pedestalCount;

        // 消耗材料（支持CRT自定义）
        if (LegacyRitualConfig.hasCustomMaterials(LegacyRitualConfig.SOULBOUND)) {
            // 使用自定义材料配置消耗
            consumeCustomMaterials(LegacyRitualConfig.SOULBOUND);
            // 统计消耗的材料数量用于能量超载计算
            pedestalCount = 0;
            for (LegacyRitualConfig.MaterialRequirement req : LegacyRitualConfig.getMaterialRequirements(LegacyRitualConfig.SOULBOUND)) {
                pedestalCount += req.getCount();
            }
        } else {
            // 默认材料消耗：末影珍珠×4 + 恶魂之泪×2 + 金块×2
            int pearlsConsumed = 0;
            int tearsConsumed = 0;
            int goldConsumed = 0;

            // 多轮遍历确保消耗足够数量
            while (pearlsConsumed < 4 || tearsConsumed < 2 || goldConsumed < 2) {
                boolean consumedAny = false;

                for (BlockPos off : OFFS8) {
                    TileEntity te = world.getTileEntity(pos.add(off));
                    if (te instanceof TileEntityPedestal) {
                        TileEntityPedestal ped = (TileEntityPedestal) te;
                        ItemStack stack = ped.getInv().getStackInSlot(0);
                        if (!stack.isEmpty()) {
                            if (stack.getItem() == Items.ENDER_PEARL && pearlsConsumed < 4) {
                                ped.consumeOne();
                                pearlsConsumed++;
                                consumedAny = true;
                            } else if (stack.getItem() == Items.GHAST_TEAR && tearsConsumed < 2) {
                                ped.consumeOne();
                                tearsConsumed++;
                                consumedAny = true;
                            } else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.GOLD_BLOCK) && goldConsumed < 2) {
                                ped.consumeOne();
                                goldConsumed++;
                                consumedAny = true;
                            }
                        }
                    }
                }

                // 防止无限循环
                if (!consumedAny) break;
            }

            pedestalCount = pearlsConsumed + tearsConsumed + goldConsumed;
        }

        // 计算能量超载加成
        float overloadBonus = LegacyRitualConfig.getOverloadBonus(
            LegacyRitualConfig.SOULBOUND, pedestalCount, initialTotalEnergy);
        float finalSuccessRate = getSoulboundSuccessRate() + overloadBonus;

        System.out.println("[Soulbound] Base success: " + (getSoulboundSuccessRate() * 100) + "%" +
                         ", Overload bonus: " + (int)(overloadBonus * 100) + "%" +
                         ", Final success: " + (int)(finalSuccessRate * 100) + "%" +
                         ", Initial energy: " + initialTotalEnergy);

        // 通知玩家超载信息
        if (overloadBonus > 0) {
            notifyOverloadBonus(overloadBonus);
        }

        // 判定成功/失败
        boolean success = world.rand.nextFloat() < finalSuccessRate;

        if (success) {
            // 成功：添加Soulbound标签
            NBTTagCompound nbt = targetItem.hasTagCompound() ? targetItem.getTagCompound() : new NBTTagCompound();
            nbt.setBoolean("Soulbound", true);
            targetItem.setTagCompound(nbt);

            notifySoulboundSuccess(targetItem);
            spawnSoulboundSuccessEffects();
        } else {
            // 失败：物品消失
            inv.setStackInSlot(0, ItemStack.EMPTY);
            notifySoulboundFail(targetItem);
            spawnSoulboundFailEffects();
        }

        syncToClient();
        markDirty();
    }

    // ========== 灵魂束缚仪式通知方法 ==========

    private void notifySoulboundStart(ItemStack item) {
        TierRitualHandler.notifyPlayers(world, pos,
            "✦ 灵魂束缚仪式开始... [" + item.getDisplayName() + "]",
            TextFormatting.DARK_PURPLE);
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.BLOCKS, 0.8f, 0.5f);
    }

    private void notifySoulboundProgress(int secondsLeft) {
        if (secondsLeft > 0 && secondsLeft <= 5) {
            TierRitualHandler.notifyPlayers(world, pos,
                "✦ 灵魂融合中... " + secondsLeft + "秒",
                TextFormatting.LIGHT_PURPLE);
        }
    }

    private void notifySoulboundSuccess(ItemStack item) {
        TierRitualHandler.notifyPlayers(world, pos,
            "✦ 灵魂束缚成功！[" + item.getDisplayName() + "] 已获得死亡保护",
            TextFormatting.DARK_PURPLE);
    }

    private void notifySoulboundFail(ItemStack item) {
        TierRitualHandler.notifyPlayers(world, pos,
            "✦ 灵魂束缚失败... [" + item.getDisplayName() + "] 被虚空吞噬",
            TextFormatting.RED);
    }

    // ========== 灵魂束缚仪式粒子效果 ==========

    private void spawnSoulboundParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 紫色末影粒子环绕
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + (soulboundProgress * 0.1);
            double radius = 1.5;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + 1.0 + Math.sin(soulboundProgress * 0.15) * 0.5;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                x, y, z, 3, 0.1, 0.1, 0.1, 0.0);
        }

        // 中心末影粒子
        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            10, 0.3, 0.3, 0.3, 0.0);
    }

    private void spawnSoulboundSuccessEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 大量末影粒子爆发
        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            100, 1.0, 1.0, 1.0, 0.5);

        // 紫色烟雾
        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            50, 0.5, 0.5, 0.5, 0.0);

        // 成功音效
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.BLOCKS, 0.8f, 0.8f);
    }

    private void spawnSoulboundFailEffects() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 紫黑色烟雾
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            50, 0.5, 0.5, 0.5, 0.1);

        ws.spawnParticle(EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            30, 0.5, 0.5, 0.5, 0.0);

        // 失败音效
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMEN_DEATH,
            SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    // Getter for soulbound ritual
    public boolean isSoulboundRitualActive() { return soulboundRitualActive; }
    public int getSoulboundProgress() { return soulboundProgress; }
}