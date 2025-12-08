package com.moremod.tile;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.EntityRitualSeat;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedCurseType;
import com.moremod.ritual.AltarTier;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import java.util.List;

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
        // 找到坐在祭坛上的玩家
        EntityPlayer seatedPlayer = findSeatedPlayer();

        if (seatedPlayer == null) {
            // 没有玩家坐着，重置嵌入进度
            if (embeddingRitualActive) {
                embeddingRitualActive = false;
                embeddingProgress = 0;
            }
            return;
        }

        // 检查是否满足嵌入条件
        if (!canPerformEmbedding(seatedPlayer)) {
            if (embeddingRitualActive) {
                embeddingRitualActive = false;
                embeddingProgress = 0;
            }
            return;
        }

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
        // 1. 必须是三阶祭坛
        if (currentTier != AltarTier.TIER_3) {
            return false;
        }

        // 2. 玩家必须有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) {
            return false;
        }

        // 3. 输入槽必须有可嵌入的诅咒饰品
        ItemStack inputStack = inv.getStackInSlot(0);
        if (!EmbeddedCurseManager.isEmbeddableCurseAccessory(inputStack)) {
            return false;
        }

        // 4. 玩家还没有嵌入这个类型的饰品
        EmbeddedCurseType type = EmbeddedCurseManager.getTypeFromItem(inputStack);
        if (type == null) return false;

        if (EmbeddedCurseManager.hasEmbeddedCurse(player, type)) {
            return false;
        }

        return true;
    }

    /**
     * 执行嵌入
     */
    private void performEmbedding(EntityPlayer player) {
        ItemStack inputStack = inv.getStackInSlot(0);
        EmbeddedCurseType type = EmbeddedCurseManager.getTypeFromItem(inputStack);

        if (type == null) return;

        // 嵌入诅咒
        boolean success = EmbeddedCurseManager.embedCurse(player, type);

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
        if (!EmbeddedCurseManager.isEmbeddableCurseAccessory(inputStack)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "请先在祭坛上放置诅咒饰品！"
            ));
            return false;
        }

        // 检查是否已嵌入
        EmbeddedCurseType type = EmbeddedCurseManager.getTypeFromItem(inputStack);
        if (type != null && EmbeddedCurseManager.hasEmbeddedCurse(player, type)) {
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
        EmbeddedCurseType type = EmbeddedCurseManager.getTypeFromItem(inputStack);

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

    // --- 標準 Getters & Setters ---
    public boolean isActive() { return isActive; }
    public int getProgress() { return process; }
    public int getMaxTime() { return activeRecipe != null ? activeRecipe.getTime() : 100; }
    public ItemStackHandler getInv() { return inv; }
    public BlockPos[] getPedestalOffsets() { return OFFS8; }
    public AltarTier getCurrentTier() { return currentTier; }
    public boolean isEmbeddingActive() { return embeddingRitualActive; }
    public int getEmbeddingProgress() { return embeddingProgress; }

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
}