package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * FirstAid 連動：高排異時「吞掉」單個部位的一次回血。
 *
 * 設計重點：
 * - 不枚舉道具、不管是繃帶/石像/藥水，只看「部位血量變多」這件事
 * - 排異值越高，吞掉回血的機率越高（但永遠不是 100%）
 * - 每次只處理「其中一個」被治療的部位，保留其他部位的治療
 * - 達成 transcend 後，這整個系統停用（只看 FleshRejectionSystem 的標記）
 * - 不直接修改排異或適應，只是用來「放大高排異的壓力感」
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionFirstAidHooks {

    /**
     * 啟動排異治療干預的最低排異比例（相對 maxRejection）
     * 例如 0.6 表示排異達 60% 以上才有機會吞回血。
     */
    private static final float MIN_REJECTION_RATIO = 0.6F;

    /**
     * 在 MIN_REJECTION_RATIO 時的吞回血基礎機率。
     */
    private static final float BASE_SWALLOW_CHANCE = 0.25F; // 25%

    /**
     * 排異接近滿值時的最大吞回血機率。
     */
    private static final float MAX_SWALLOW_CHANCE = 0.75F;  // 75%

    /**
     * 兩次快照比較時，如果血量增加小於這個值就忽略（避免浮點誤差＋微小變動）。
     */
    private static final float HEAL_DELTA_EPSILON = 0.05F;

    /**
     * 每隔幾 Tick 做一次比較，避免每 Tick 都掃描。
     * 20 = 約 1 秒一次。
     */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /**
     * FA 部位血量快照。
     * 只記錄我們關心的幾個部位：頭、身、雙臂、雙腿。
     */
    private static class PartSnapshot {
        float head;
        float body;
        float leftArm;
        float rightArm;
        float leftLeg;
        float rightLeg;

        PartSnapshot() {}

        PartSnapshot(AbstractPlayerDamageModel model) {
            this.head     = model.HEAD.currentHealth;
            this.body     = model.BODY.currentHealth;
            this.leftArm  = model.LEFT_ARM.currentHealth;
            this.rightArm = model.RIGHT_ARM.currentHealth;
            this.leftLeg  = model.LEFT_LEG.currentHealth;
            this.rightLeg = model.RIGHT_LEG.currentHealth;
        }

        void copyFrom(AbstractPlayerDamageModel model) {
            this.head     = model.HEAD.currentHealth;
            this.body     = model.BODY.currentHealth;
            this.leftArm  = model.LEFT_ARM.currentHealth;
            this.rightArm = model.RIGHT_ARM.currentHealth;
            this.leftLeg  = model.LEFT_LEG.currentHealth;
            this.rightLeg = model.RIGHT_LEG.currentHealth;
        }
    }

    /**
     * 以玩家實體為 key 的弱引用快照表。
     * 玩家離線或被 GC 掉之後，WeakHashMap 會自然釋放記錄。
     */
    private static final Map<EntityPlayer, PartSnapshot> SNAPSHOTS = new WeakHashMap<>();

    private static final Random RANDOM = new Random();

    /**
     * 玩家 Tick：每隔一段時間比對 FA 部位血量，偵測「被治療」事件。
     *
     * 注意：這個 handler 本身不改排異／適應，只讀取 FleshRejectionSystem 的狀態。
     */
    @SubscribeEvent
    @Optional.Method(modid = "firstaid")
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        World world = player.world;

        // 客戶端不處理，只在伺服端做
        if (world.isRemote) return;

        // 主開關：血肉排異系統沒開就整個關掉
        if (!FleshRejectionConfig.enableRejectionSystem) return;

        // 降頻處理：每 CHECK_INTERVAL_TICKS 執行一次
        if (player.ticksExisted % CHECK_INTERVAL_TICKS != 0) return;

        // 核心沒裝就不用處理，避免白做工
        // （如果你希望沒裝核心也能排異，這行可以拿掉）
        if (com.moremod.item.ItemMechanicalCore.getCoreFromPlayer(player).isEmpty()) {
            return;
        }

        // transcend 後完全不再干預治療
        if (FleshRejectionSystem.hasTranscended(player)) {
            return;
        }

        // 取得排異比例
        double maxRejection = FleshRejectionConfig.maxRejection <= 0
                ? 1.0
                : FleshRejectionConfig.maxRejection;
        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float ratio = (float) (rejection / maxRejection);

        // 排異太低，完全不干預治療
        if (ratio < MIN_REJECTION_RATIO) {
            // 更新快照，避免下次誤判
            ensureSnapshot(player, true);
            return;
        }

        // 嘗試取得 FA 的 damage model
        AbstractPlayerDamageModel model = getDamageModel(player);
        if (model == null) {
            // 沒拿到 model，更新快照就好
            ensureSnapshot(player, true);
            return;
        }

        // 取得前一份快照（如果沒有會新建）
        PartSnapshot prev = ensureSnapshot(player, false);
        PartSnapshot current = new PartSnapshot(model);

        // 找出有「明顯回血」的部位
        List<HealedPart> healedParts = detectHealedParts(prev, current);
        if (healedParts.isEmpty()) {
            // 沒有任何部位回血，更新快照後離開
            prev.copyFrom(model);
            return;
        }

        // 根據排異比例計算這一次的吞回血機率
        float swallowChance = computeSwallowChance(ratio);

        // 擲骰子：這一次要不要吞掉其中一個部位的治療
        if (RANDOM.nextFloat() < swallowChance) {
            // 隨機挑一個被治療的部位進行干預（Q3: A 單部位）
            HealedPart target = healedParts.get(RANDOM.nextInt(healedParts.size()));
            applySwallow(player, model, prev, target);
        }

        // 無論有沒有吞掉，最後都更新快照
        prev.copyFrom(model);
    }

    /**
     * 小結構：紀錄某個部位這一輪被治療前後的血量變化。
     */
    private static class HealedPart {
        PartType type;
        float before;
        float after;

        HealedPart(PartType type, float before, float after) {
            this.type = type;
            this.before = before;
            this.after = after;
        }
    }

    /**
     * 我們關心的 FA 部位種類。
     */
    private enum PartType {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG
    }

    // ────────────────────── 內部工具 ──────────────────────

    @Optional.Method(modid = "firstaid")
    private static AbstractPlayerDamageModel getDamageModel(EntityPlayer player) {
        if (!player.hasCapability(CapabilityExtendedHealthSystem.INSTANCE, null)) {
            return null;
        }
        return (AbstractPlayerDamageModel) player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
    }

    /**
     * 確保玩家有一份快照物件，若不存在就建立。
     *
     * @param copyNow 若為 true，則立刻從 FA model 讀取當前血量寫入快照。
     */
    @Optional.Method(modid = "firstaid")
    private static PartSnapshot ensureSnapshot(EntityPlayer player, boolean copyNow) {
        PartSnapshot snapshot = SNAPSHOTS.get(player);
        if (snapshot == null) {
            snapshot = new PartSnapshot();
            SNAPSHOTS.put(player, snapshot);
        }

        if (copyNow) {
            AbstractPlayerDamageModel model = getDamageModel(player);
            if (model != null) {
                snapshot.copyFrom(model);
            }
        }

        return snapshot;
    }

    /**
     * 比較前後快照，找出實際有「血量上升」的部位。
     */
    private static List<HealedPart> detectHealedParts(PartSnapshot prev, PartSnapshot curr) {
        List<HealedPart> list = new ArrayList<>();

        if (curr.head - prev.head > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.HEAD, prev.head, curr.head));
        }
        if (curr.body - prev.body > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.BODY, prev.body, curr.body));
        }
        if (curr.leftArm - prev.leftArm > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.LEFT_ARM, prev.leftArm, curr.leftArm));
        }
        if (curr.rightArm - prev.rightArm > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.RIGHT_ARM, prev.rightArm, curr.rightArm));
        }
        if (curr.leftLeg - prev.leftLeg > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.LEFT_LEG, prev.leftLeg, curr.leftLeg));
        }
        if (curr.rightLeg - prev.rightLeg > HEAL_DELTA_EPSILON) {
            list.add(new HealedPart(PartType.RIGHT_LEG, prev.rightLeg, curr.rightLeg));
        }

        return list;
    }

    /**
     * 根據排異比例計算這次檢查要用的吞回血機率。
     *
     * ratio < MIN_REJECTION_RATIO → 不應該呼叫這裡（已在外層 return）
     */
    private static float computeSwallowChance(float ratio) {
        // ratio 在 MIN_REJECTION_RATIO ~ 1.0 之間線性映射到 BASE_SWALLOW_CHANCE ~ MAX_SWALLOW_CHANCE
        float t = (ratio - MIN_REJECTION_RATIO) / (1.0F - MIN_REJECTION_RATIO);
        if (t < 0F) t = 0F;
        if (t > 1F) t = 1F;

        return BASE_SWALLOW_CHANCE + t * (MAX_SWALLOW_CHANCE - BASE_SWALLOW_CHANCE);
    }

    /**
     * 實際「吞掉」指定部位的那次治療：
     * - 把該部位血量改回治療前的值
     * - 放煙霧粒子
     * - 適度給一點文字提示（避免瘋狂洗屏）
     */
    @Optional.Method(modid = "firstaid")
    private static void applySwallow(EntityPlayer player,
                                     AbstractPlayerDamageModel model,
                                     PartSnapshot prev,
                                     HealedPart healed) {

        // 1. 把該部位血量改回治療前
        switch (healed.type) {
            case HEAD:
                model.HEAD.currentHealth = prev.head;
                break;
            case BODY:
                model.BODY.currentHealth = prev.body;
                break;
            case LEFT_ARM:
                model.LEFT_ARM.currentHealth = prev.leftArm;
                break;
            case RIGHT_ARM:
                model.RIGHT_ARM.currentHealth = prev.rightArm;
                break;
            case LEFT_LEG:
                model.LEFT_LEG.currentHealth = prev.leftLeg;
                break;
            case RIGHT_LEG:
                model.RIGHT_LEG.currentHealth = prev.rightLeg;
                break;
        }

        // 2. 粒子效果：煙霧失敗粒子（Q4）
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;

            for (int i = 0; i < 10; i++) {
                double dx = (ws.rand.nextDouble() - 0.5D) * 0.8D;
                double dy = ws.rand.nextDouble() * 1.6D;
                double dz = (ws.rand.nextDouble() - 0.5D) * 0.8D;

                ws.spawnParticle(
                        EnumParticleTypes.SMOKE_NORMAL,
                        player.posX + dx,
                        player.posY + dy,
                        player.posZ + dz,
                        1,
                        0, 0, 0,
                        0.02D
                );
            }
        }

        // 3. 避免洗屏：這裡只在 debug 或少量機率下給提示
        if (FleshRejectionConfig.debugMode || RANDOM.nextFloat() < 0.25F) {
            String partName;
            switch (healed.type) {
                case HEAD:
                    partName = "頭部";
                    break;
                case BODY:
                    partName = "躯幹";
                    break;
                case LEFT_ARM:
                    partName = "左臂";
                    break;
                case RIGHT_ARM:
                    partName = "右臂";
                    break;
                case LEFT_LEG:
                    partName = "左腿";
                    break;
                case RIGHT_LEG:
                default:
                    partName = "右腿";
                    break;
            }

            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.DARK_RED + "[血肉排異] " +
                                    TextFormatting.GRAY + partName + " 的治療被異常排斥了……"
                    ),
                    true
            );
        }
    }
}