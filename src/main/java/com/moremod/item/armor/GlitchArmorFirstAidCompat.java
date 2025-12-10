package com.moremod.item.armor;

import com.moremod.moremod;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 故障盔甲 - FirstAid 模組兼容層
 * Glitch Armor - FirstAid Mod Compatibility Layer
 *
 * FirstAid 模組有獨立的死亡邏輯：當 HEAD 或 BODY 血量到 0 時觸發死亡
 * 此類通過訂閱 FirstAidLivingDamageEvent 並直接修復部位血量來防止死亡
 *
 * NULL異常效果：
 * - 穿戴胸甲時，致命傷害有20%機率被完全消除
 * - 全套增加10%觸發機率（共30%）
 * - 觸發後獲得1秒閃爍無敵
 * - 30秒冷卻時間
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class GlitchArmorFirstAidCompat {

    private static final Random RANDOM = new Random();

    // NULL觸發冷卻
    private static final Map<UUID, Long> NULL_COOLDOWNS = new HashMap<>();
    private static final long NULL_COOLDOWN_MS = 30000; // 30秒冷卻

    // 閃爍無敵狀態（與主Handler共享）
    private static final Map<UUID, Long> FLICKER_END_TIMES = new HashMap<>();

    /**
     * 攔截 FirstAid 的傷害事件
     * 關鍵：必須在傷害分配後、死亡判定前修復致命部位
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "firstaid")
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        try {
            EntityPlayer player = event.getEntityPlayer();
            if (player == null || player.world.isRemote) return;

            // 檢查是否穿戴胸甲
            if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.CHEST)) return;

            // 檢查是否在閃爍無敵狀態
            Long flickerEnd = FLICKER_END_TIMES.get(player.getUniqueID());
            if (flickerEnd != null && System.currentTimeMillis() < flickerEnd) {
                // 完全免疫傷害，恢復所有部位到安全血量
                for (AbstractDamageablePart part : event.getAfterDamage()) {
                    if (part.currentHealth < part.getMaxHealth()) {
                        part.heal(part.getMaxHealth(), null, false);
                    }
                }
                syncDamageModel(player);
                spawnGlitchParticles(player, 5);
                return;
            }

            // 檢查傷害後是否有致命部位
            boolean hasFatalDamage = false;
            List<AbstractDamageablePart> fatalParts = new ArrayList<>();

            for (AbstractDamageablePart part : event.getAfterDamage()) {
                if (part.canCauseDeath && part.currentHealth <= 0) {
                    hasFatalDamage = true;
                    fatalParts.add(part);
                }
            }

            // 如果沒有致命傷害，不處理
            if (!hasFatalDamage) return;

            // 檢查冷卻
            Long lastNull = NULL_COOLDOWNS.get(player.getUniqueID());
            if (lastNull != null && System.currentTimeMillis() - lastNull < NULL_COOLDOWN_MS) {
                long remaining = (NULL_COOLDOWN_MS - (System.currentTimeMillis() - lastNull)) / 1000;
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "[NULL] " +
                        TextFormatting.GRAY + String.format("異常冷卻中... %d秒", remaining)), true);
                return;
            }

            // 計算觸發機率
            float nullChance = 0.20f;
            if (ItemGlitchArmor.hasFullSet(player)) {
                nullChance += 0.10f; // 全套30%
            }

            // 嘗試觸發NULL異常
            if (RANDOM.nextFloat() < nullChance) {
                triggerNullException(player, fatalParts);
            }

        } catch (Throwable ignored) {
            // 防止兼容性錯誤
        }
    }

    /**
     * 觸發NULL異常 - 防止死亡
     */
    @Optional.Method(modid = "firstaid")
    private static void triggerNullException(EntityPlayer player, List<AbstractDamageablePart> fatalParts) {
        // 記錄冷卻
        NULL_COOLDOWNS.put(player.getUniqueID(), System.currentTimeMillis());

        // 設置1秒閃爍無敵
        FLICKER_END_TIMES.put(player.getUniqueID(), System.currentTimeMillis() + 1000);

        // 通知主Handler也設置閃爍狀態
        GlitchArmorEventHandler.setFlickerState(player.getUniqueID(), System.currentTimeMillis() + 1000);

        // 恢復所有致命部位到安全血量
        for (AbstractDamageablePart part : fatalParts) {
            // 恢復到50%最大血量
            float healAmount = part.getMaxHealth() * 0.5f;
            part.heal(healAmount, null, false);
        }

        // 同步傷害模型到客戶端
        syncDamageModel(player);

        // 視覺和音效
        spawnNullEffect(player);

        // 訊息
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "§l[NULL] " +
                TextFormatting.GRAY + "NullPointerException: damage == null"), true);

        // 額外回血（總體效果）
        player.heal(2.0f);
    }

    /**
     * 同步傷害模型到客戶端
     */
    @Optional.Method(modid = "firstaid")
    private static void syncDamageModel(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            AbstractPlayerDamageModel model = (AbstractPlayerDamageModel)
                    player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
            if (model != null) {
                FirstAid.NETWORKING.sendTo(new MessageSyncDamageModel(model, false), (EntityPlayerMP) player);
            }
        }
    }

    /**
     * 生成NULL效果的粒子和音效
     */
    private static void spawnNullEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 播放音效 - 故障聲
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 2.0f);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 0.5f);

        // 故障粒子效果 - 錯位殘影
        for (int i = 0; i < 40; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 3;
            double offsetY = RANDOM.nextDouble() * 2.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 3;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0, 0.1);
        }

        // 中心爆發效果
        world.spawnParticle(EnumParticleTypes.END_ROD,
                player.posX, player.posY + 1, player.posZ,
                30, 0.8, 0.8, 0.8, 0.15);

        // 環形擴散
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i) / 16;
            double x = player.posX + Math.cos(angle) * 2;
            double z = player.posZ + Math.sin(angle) * 2;

            world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    x, player.posY + 0.5, z,
                    2, 0.1, 0.2, 0.1, 0);
        }
    }

    /**
     * 生成故障粒子效果
     */
    private static void spawnGlitchParticles(EntityPlayer player, int count) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < count; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 1.5;
            double offsetY = RANDOM.nextDouble() * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 1.5;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0.0, 0);
        }
    }

    /**
     * 檢查閃爍無敵狀態（供外部查詢）
     */
    public static boolean isInFlickerState(UUID playerId) {
        Long flickerEnd = FLICKER_END_TIMES.get(playerId);
        return flickerEnd != null && System.currentTimeMillis() < flickerEnd;
    }

    /**
     * 設置閃爍狀態（供主Handler調用）
     */
    public static void setFlickerState(UUID playerId, long endTime) {
        FLICKER_END_TIMES.put(playerId, endTime);
    }

    /**
     * 檢查 FirstAid 是否已載入
     */
    public static boolean isFirstAidLoaded() {
        return Loader.isModLoaded("firstaid");
    }

    /**
     * 清理玩家數據（登出時調用）
     */
    public static void cleanupPlayer(UUID playerId) {
        NULL_COOLDOWNS.remove(playerId);
        FLICKER_END_TIMES.remove(playerId);
    }
}
