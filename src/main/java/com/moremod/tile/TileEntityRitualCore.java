package com.moremod.tile;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.EntityRitualSeat;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import com.moremod.ritual.AltarTier;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
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
    private static final int ENCHANT_INFUSION_TIME = 200; // 10秒注魔时间
    private static final float ENCHANT_SUCCESS_RATE = 0.10f; // 10%成功率

    // 复制仪式系统 (Duplication Ritual)
    private boolean duplicationRitualActive = false;
    private int duplicationProgress = 0;
    private static final int DUPLICATION_TIME = 300; // 15秒复制时间
    private static final float DUPLICATION_SUCCESS_RATE = 0.01f; // 1%成功率

    // 灵魂绑定仪式系统 (Soul Binding Ritual) - 从玩家头颅创建假玩家核心
    private boolean soulBindingActive = false;
    private int soulBindingProgress = 0;
    private static final int SOUL_BINDING_TIME = 400; // 20秒绑定时间
    private static final float SOUL_BINDING_SUCCESS_RATE = 0.50f; // 50%成功率

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
        }

        // 如果還是找不到配方，歸位
        if (activeRecipe == null) {
            updateState(false, false);
            return;
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

    private void finishRitual(List<TileEntityPedestal> peds) {
        // 先保存配方引用，因为后续操作可能触发reset()
        RitualInfusionRecipe recipe = activeRecipe;

        // 标记为非活动状态，防止onContentsChanged触发reset
        isActive = false;

        // 使用祭坛阶层调整后的失败率
        float adjustedFailChance = recipe.getAdjustedFailChance(currentTier);
        System.out.println("[Ritual] Tier: " + currentTier.getDisplayName() +
                         ", Base fail: " + recipe.getFailChance() +
                         ", Adjusted fail: " + adjustedFailChance);

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

        // 找到坐在祭坛上的玩家
        EntityPlayer seatedPlayer = findSeatedPlayer();

        if (seatedPlayer == null) {
            // 没有玩家坐着，重置所有嵌入相关状态
            if (embeddingRitualActive) {
                embeddingRitualActive = false;
                embeddingProgress = 0;
            }
            embeddingFailCounter = 0;
            return;
        }

        // 检查是否满足嵌入条件，获取失败原因
        String failReason = checkEmbeddingConditions(seatedPlayer);

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
                seatedPlayer.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + failReason
                ), true);
                embeddingFailFeedbackCooldown = 20; // 1秒冷却
            }

            // 如果持续失败超过5秒，自动让玩家下马
            if (embeddingFailCounter >= EMBEDDING_FAIL_DISMOUNT_TIME) {
                seatedPlayer.sendMessage(new TextComponentString(
                    TextFormatting.RED + "嵌入条件无法满足，仪式已取消。"
                ));
                seatedPlayer.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "原因: " + failReason
                ));
                seatedPlayer.dismountRidingEntity();
                embeddingFailCounter = 0;
            }

            return;
        }

        // 条件满足，重置失败计数
        embeddingFailCounter = 0;

        // 开始/继续嵌入仪式
        if (!embeddingRitualActive) {
            embeddingRitualActive = true;
            embeddingProgress = 0;
            notifyEmbeddingStart(seatedPlayer);
        }

        embeddingProgress++;

        // 进度效果
        if (embeddingProgress % 20 == 0) {
            // 每秒显示进度
            int seconds = (EMBEDDING_TIME - embeddingProgress) / 20;
            seatedPlayer.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "嵌入仪式进行中... " +
                TextFormatting.GOLD + seconds + "秒"
            ), true);

            // 粒子效果
            spawnEmbeddingParticles();
        }

        // 完成嵌入
        if (embeddingProgress >= EMBEDDING_TIME) {
            performEmbedding(seatedPlayer);
            embeddingRitualActive = false;
            embeddingProgress = 0;
        }
    }

    /**
     * 找到坐在祭坛上的玩家
     */
    private EntityPlayer findSeatedPlayer() {
        // 查找祭坛核心位置附近的座椅实体
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(1);
        List<EntityRitualSeat> seats = world.getEntitiesWithinAABB(EntityRitualSeat.class, area);

        for (EntityRitualSeat seat : seats) {
            if (pos.equals(seat.getCorePos())) {
                EntityPlayer player = seat.getSeatedPlayer();
                if (player != null) return player;
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

        if (type == null) return;

        // 嵌入遗物
        boolean success = EmbeddedCurseManager.embedRelic(player, type);

        if (success) {
            // 消耗物品
            inv.extractItem(0, 1, false);

            // 播放效果
            spawnEmbeddingCompleteEffects(player);

            // 让玩家站起来
            player.dismountRidingEntity();
        }
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
        }

        enchantInfusionProgress++;

        // 进度效果
        if (enchantInfusionProgress % 20 == 0) {
            int seconds = (ENCHANT_INFUSION_TIME - enchantInfusionProgress) / 20;
            notifyEnchantInfusionProgress(seconds, bookPedestals.size());
            spawnEnchantInfusionParticles();
        }

        // 完成注魔
        if (enchantInfusionProgress >= ENCHANT_INFUSION_TIME) {
            performEnchantInfusion(coreItem, bookPedestals);
            enchantInfusionActive = false;
            enchantInfusionProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着附魔书的基座
     */
    private List<TileEntityPedestal> findEnchantedBookPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
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
        return list;
    }

    /**
     * 执行注魔仪式
     */
    private void performEnchantInfusion(ItemStack coreItem, List<TileEntityPedestal> bookPedestals) {
        // 计算成功率 (基础10%)
        float successRate = ENCHANT_SUCCESS_RATE;

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
                TextFormatting.RED + "⚠ 成功率: " + TextFormatting.YELLOW + "10%"
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
        }

        duplicationProgress++;

        // 进度效果
        if (duplicationProgress % 20 == 0) {
            int seconds = (DUPLICATION_TIME - duplicationProgress) / 20;
            notifyDuplicationProgress(seconds);
            spawnDuplicationParticles();
        }

        // 完成复制
        if (duplicationProgress >= DUPLICATION_TIME) {
            performDuplication(coreItem, essencePedestals);
            duplicationRitualActive = false;
            duplicationProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着虚空精华的基座
     */
    private List<TileEntityPedestal> findVoidEssencePedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
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

        // 判定成功/失败 (1%成功率)
        boolean success = world.rand.nextFloat() < DUPLICATION_SUCCESS_RATE;

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
        }

        soulBindingProgress++;

        // 进度效果
        if (soulBindingProgress % 20 == 0) {
            int seconds = (SOUL_BINDING_TIME - soulBindingProgress) / 20;
            notifySoulBindingProgress(seconds, skullProfile);
            spawnSoulBindingParticles();
        }

        // 完成灵魂绑定
        if (soulBindingProgress >= SOUL_BINDING_TIME) {
            performSoulBinding(coreItem, skullProfile, soulMaterialPedestals);
            soulBindingActive = false;
            soulBindingProgress = 0;
        }

        return true;
    }

    /**
     * 查找所有放着灵魂材料的基座
     * 灵魂材料：灵魂果实、虚空精华、凝视碎片、灵魂锚点等
     */
    private List<TileEntityPedestal> findSoulMaterialPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
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
        return list;
    }

    /**
     * 判断是否是灵魂材料
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

        // 判定成功/失败 (50%成功率)
        boolean success = world.rand.nextFloat() < SOUL_BINDING_SUCCESS_RATE;

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
            double radius = 2.0 - (soulBindingProgress / (float) SOUL_BINDING_TIME);
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
}