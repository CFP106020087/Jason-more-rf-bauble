package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.core.CurseDeathHook;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第五幕剧本 (Script of the Fifth Act)
 * "结局早已写好，哪怕演员对此一无所知。"
 *
 * ═══════════════════════════════════════════════════════════════
 * 核心契约：戴上即与命运绑定，脱下即死
 * ═══════════════════════════════════════════════════════════════
 *
 * 【伤害缓存】受到的伤害不扣血，记录在剧本上
 * 【戏剧张力】缓存越高，伤害加成越高（指数型：ratio² × 100%）
 * 【击杀净化】每次击杀清除25%缓存
 * 【输出抵消】对敌人造成的伤害，10%抵消缓存
 * 【脱战结算】5秒无战斗 → 结算40%缓存伤害
 * 【超载结算】缓存 > 150%最大血量 → 强制结算
 * 【改写结局】结算时血量<30% → 0%自伤 + 200%反弹
 *
 * 【契约代价】脱下饰品 → 立即死亡
 * 【死亡底线】本该死亡 → ASM拦截，留1血，进入「落幕」
 * 【落幕惩罚】30秒：受伤×2，禁止治疗，可能真死
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemScriptOfFifthAct extends Item implements IBauble {

    // ═══════════════════════════════════════════════════════════════
    // 常量配置
    // ═══════════════════════════════════════════════════════════════

    // 检查敌人的范围
    private static final double ENEMY_CHECK_RANGE = 16.0;
    // 改写结局的血量阈值
    private static final float REWRITE_HEALTH_THRESHOLD = 0.3f;
    // 改写结局的伤害反弹倍率
    private static final float REWRITE_REFLECT_MULTIPLIER = 2.0f;
    // 改写结局的反弹范围
    private static final double REWRITE_RANGE = 8.0;
    // 正常结算的伤害比例（40%）
    private static final float SETTLEMENT_DAMAGE_RATIO = 0.4f;
    // 超载阈值（150%最大血量）
    private static final float OVERLOAD_THRESHOLD = 1.5f;
    // 脱战时间（tick）- 5秒
    private static final int OUT_OF_COMBAT_TICKS = 100;
    // 击杀净化比例（25%）
    private static final float KILL_PURGE_RATIO = 0.25f;
    // 输出抵消比例（10%）
    private static final float DAMAGE_OFFSET_RATIO = 0.10f;

    // 落幕状态持续时间（毫秒）- 30秒
    public static final long CURTAIN_FALL_DURATION_MS = 30000;
    // 落幕状态受伤倍率
    public static final float CURTAIN_FALL_DAMAGE_MULTIPLIER = 2.0f;

    // ═══════════════════════════════════════════════════════════════
    // 玩家数据
    // ═══════════════════════════════════════════════════════════════

    // 剧本数据
    private static final Map<UUID, ScriptData> SCRIPT_DATA = new ConcurrentHashMap<>();
    // 落幕状态（死亡拦截后的惩罚期）
    private static final Map<UUID, Long> CURTAIN_FALL_END_TIME = new ConcurrentHashMap<>();
    // 正在结算中（防止递归）
    private static final Set<UUID> SETTLING_PLAYERS = ConcurrentHashMap.newKeySet();

    /**
     * 剧本数据类
     */
    public static class ScriptData {
        public float bufferedDamage = 0;      // 缓存的伤害
        public long lastCombatTime = 0;       // 最后战斗时间（tick）
        public boolean isSettling = false;    // 是否正在结算

        public void addDamage(float damage) {
            this.bufferedDamage += damage;
        }

        public void reduceDamage(float amount) {
            this.bufferedDamage = Math.max(0, this.bufferedDamage - amount);
        }

        public void clearDamage() {
            this.bufferedDamage = 0;
        }

        public void recordCombat(long worldTime) {
            this.lastCombatTime = worldTime;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 构造函数
    // ═══════════════════════════════════════════════════════════════

    public ItemScriptOfFifthAct() {
        this.setMaxStackSize(1);
        this.setTranslationKey("script_of_fifth_act");
        this.setRegistryName("script_of_fifth_act");
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.CHARM;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return false;
        return CurseDeathHook.hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        UUID uuid = p.getUniqueID();

        // 初始化数据
        SCRIPT_DATA.put(uuid, new ScriptData());

        // 契约提示
        p.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "══════════════════════════════════"
        ));
        p.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "  「第五幕剧本」" + TextFormatting.GRAY + " 与你签订契约"
        ));
        p.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "  ⚠ 脱下此饰品将导致立即死亡"
        ));
        p.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "══════════════════════════════════"
        ));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        UUID uuid = p.getUniqueID();

        // 清理数据
        SCRIPT_DATA.remove(uuid);

        // 契约代价：立即死亡（无视任何保护）
        if (!p.isDead && p.getHealth() > 0) {
            p.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "「剧本被撕毁...你的故事到此结束」"
            ));

            // 粒子效果
            if (p.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) p.world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        p.posX, p.posY + 1, p.posZ,
                        50, 0.5, 1.0, 0.5, 0.1);
            }

            // 直接设置死亡（绕过所有保护）
            p.setHealth(0);
            p.onDeath(DamageSource.OUT_OF_WORLD);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 每 Tick 更新
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        UUID uuid = player.getUniqueID();
        ScriptData data = SCRIPT_DATA.get(uuid);

        if (data == null) {
            data = new ScriptData();
            SCRIPT_DATA.put(uuid, data);
        }

        if (data.isSettling) return;

        long currentTime = player.world.getTotalWorldTime();

        // ═══════ 检查结算条件 ═══════

        // 条件1: 超载（缓存 > 150% 最大血量）
        float maxHP = player.getMaxHealth();
        float overloadLimit = maxHP * OVERLOAD_THRESHOLD;

        if (data.bufferedDamage > overloadLimit) {
            settleDamage(player, data, "超载：剧本无法承受更多");
            return;
        }

        // 条件2: 脱战（5秒无战斗且有缓存）
        if (data.bufferedDamage > 0 && data.lastCombatTime > 0) {
            long ticksSinceCombat = currentTime - data.lastCombatTime;
            if (ticksSinceCombat >= OUT_OF_COMBAT_TICKS) {
                // 检查周围是否还有敌人
                if (!hasNearbyEnemies(player)) {
                    // 杀光敌人，伤害清零！
                    float cleared = data.bufferedDamage;
                    data.clearDamage();
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ 敌人已清除！" +
                                    TextFormatting.GRAY + " 缓存伤害 " +
                                    TextFormatting.YELLOW + String.format("%.0f", cleared) +
                                    TextFormatting.GRAY + " 点已消散"
                    ));
                    spawnSuccessEffect(player);
                } else {
                    // 还有敌人但脱战了，结算
                    settleDamage(player, data, "脱战：表演中断");
                }
                return;
            }
        }

        // ═══════ 状态显示 ═══════

        // 每秒更新一次状态栏
        if (currentTime % 20 == 0) {
            displayStatus(player, data);
        }

        // 落幕状态警告（每2秒提醒）
        if (isInCurtainFall(player) && currentTime % 40 == 0) {
            int remaining = getCurtainFallRemaining(player);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠ 【落幕】" +
                            TextFormatting.RED + " 受伤×2 | 禁止治疗 | " +
                            TextFormatting.YELLOW + remaining + "秒"
            ), true);
        }
    }

    /**
     * 显示状态栏信息
     */
    private void displayStatus(EntityPlayer player, ScriptData data) {
        if (data.bufferedDamage <= 0) return;

        float maxHP = player.getMaxHealth();
        float bufferRatio = data.bufferedDamage / maxHP;
        float damageBonus = getDamageBonus(bufferRatio);
        float healthRatio = player.getHealth() / maxHP;

        // 状态颜色
        TextFormatting bufferColor;
        String warningText = "";

        if (bufferRatio >= 1.4f) {
            bufferColor = TextFormatting.DARK_RED;
            warningText = " ⚠超载临界！";
        } else if (bufferRatio >= 1.0f) {
            bufferColor = TextFormatting.RED;
            warningText = " !危险";
        } else if (bufferRatio >= 0.5f) {
            bufferColor = TextFormatting.YELLOW;
        } else {
            bufferColor = TextFormatting.GREEN;
        }

        // 改写结局就绪提示
        String rewriteStatus = healthRatio <= REWRITE_HEALTH_THRESHOLD ?
                TextFormatting.LIGHT_PURPLE + " [改写就绪]" : "";

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "📜 " +
                        bufferColor + String.format("%.0f", data.bufferedDamage) +
                        TextFormatting.GRAY + "/" +
                        TextFormatting.WHITE + String.format("%.0f", maxHP * OVERLOAD_THRESHOLD) +
                        TextFormatting.GRAY + " | 张力: " +
                        TextFormatting.GOLD + "+" + String.format("%.0f%%", damageBonus * 100) +
                        warningText + rewriteStatus
        ), true);
    }

    // ═══════════════════════════════════════════════════════════════
    // 戏剧张力：伤害加成计算（指数型）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 计算戏剧张力伤害加成
     * 公式：(缓存/最大血量)² × 100%
     *
     * @param bufferRatio 缓存比例（缓存伤害 / 最大血量）
     * @return 伤害加成倍率（如 1.0 = +100%）
     */
    public static float getDamageBonus(float bufferRatio) {
        return bufferRatio * bufferRatio; // ratio² × 100% = ratio²
    }

    /**
     * 获取玩家当前的戏剧张力加成
     */
    public static float getPlayerDamageBonus(EntityPlayer player) {
        if (!hasScript(player)) return 0;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data == null) return 0;

        float bufferRatio = data.bufferedDamage / player.getMaxHealth();
        return getDamageBonus(bufferRatio);
    }

    // ═══════════════════════════════════════════════════════════════
    // 事件处理：伤害缓存
    // ═══════════════════════════════════════════════════════════════

    /**
     * 拦截玩家受到的伤害，记录到剧本
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!hasScript(player)) return;
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否在结算中（结算伤害不应被缓存）
        if (SETTLING_PLAYERS.contains(player.getUniqueID())) return;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data == null || data.isSettling) return;

        float damage = event.getAmount();

        // 落幕状态：伤害翻倍且不缓存
        if (isInCurtainFall(player)) {
            event.setAmount(damage * CURTAIN_FALL_DAMAGE_MULTIPLIER);
            return; // 不缓存，直接受伤
        }

        // 缓存伤害
        data.addDamage(damage);
        data.recordCombat(player.world.getTotalWorldTime());

        // 取消实际伤害
        event.setCanceled(true);

        // 视觉效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.REDSTONE,
                    player.posX, player.posY + 1, player.posZ,
                    5, 0.3, 0.3, 0.3, 0);
        }
    }

    /**
     * 玩家攻击：应用戏剧张力加成 + 输出抵消
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        if (!hasScript(player)) return;
        if (!CurseDeathHook.hasCursedRing(player)) return;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data == null) return;

        // 记录战斗时间
        data.recordCombat(player.world.getTotalWorldTime());

        float originalDamage = event.getAmount();

        // 应用戏剧张力加成
        float bufferRatio = data.bufferedDamage / player.getMaxHealth();
        float damageBonus = getDamageBonus(bufferRatio);
        float boostedDamage = originalDamage * (1.0f + damageBonus);
        event.setAmount(boostedDamage);

        // 输出抵消：造成伤害的10%抵消缓存
        float offset = boostedDamage * DAMAGE_OFFSET_RATIO;
        if (offset > 0 && data.bufferedDamage > 0) {
            data.reduceDamage(offset);
        }
    }

    /**
     * 玩家击杀：净化缓存
     */
    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        if (player.world.isRemote) return;

        if (!hasScript(player)) return;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data == null || data.bufferedDamage <= 0) return;

        // 击杀净化：清除25%缓存
        float purgeAmount = data.bufferedDamage * KILL_PURGE_RATIO;
        data.reduceDamage(purgeAmount);

        // 记录战斗时间
        data.recordCombat(player.world.getTotalWorldTime());
    }

    /**
     * 落幕状态：禁止治疗
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (isInCurtainFall(player)) {
            event.setCanceled(true);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 结算逻辑
    // ═══════════════════════════════════════════════════════════════

    /**
     * 结算缓存伤害
     */
    private void settleDamage(EntityPlayer player, ScriptData data, String reason) {
        if (data.bufferedDamage <= 0) return;

        UUID uuid = player.getUniqueID();
        data.isSettling = true;
        SETTLING_PLAYERS.add(uuid);

        try {
            float totalDamage = data.bufferedDamage;
            float healthRatio = player.getHealth() / player.getMaxHealth();

            // 检查是否触发【改写结局】
            boolean rewriteTriggered = healthRatio <= REWRITE_HEALTH_THRESHOLD;

            if (rewriteTriggered) {
                // ═══ 改写结局：0%自伤 + 200%反弹 ═══
                float reflectDamage = totalDamage * REWRITE_REFLECT_MULTIPLIER;
                int enemiesHit = reflectDamageToNearby(player, reflectDamage);

                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✨ 【改写结局】" +
                                TextFormatting.GRAY + " 反弹 " +
                                TextFormatting.RED + String.format("%.0f", reflectDamage) +
                                TextFormatting.GRAY + " 伤害给 " +
                                TextFormatting.GOLD + enemiesHit +
                                TextFormatting.GRAY + " 个敌人！" +
                                TextFormatting.GREEN + " (自身无伤)"
                ));

                spawnRewriteEffect(player);

            } else {
                // ═══ 正常结算：承受40%伤害 ═══
                float settleDamage = totalDamage * SETTLEMENT_DAMAGE_RATIO;

                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "📜 " + reason +
                                TextFormatting.GRAY + " | 结算 " +
                                TextFormatting.RED + String.format("%.0f", settleDamage) +
                                TextFormatting.GRAY + " 伤害 (40%)"
                ));

                // 应用伤害
                applySettlementDamage(player, settleDamage);

                spawnSettleEffect(player);
            }

            // 清空缓存
            data.clearDamage();
            data.lastCombatTime = 0;

        } finally {
            data.isSettling = false;
            SETTLING_PLAYERS.remove(uuid);
        }
    }

    /**
     * 应用结算伤害（分多次，兼容 First Aid）
     */
    private void applySettlementDamage(EntityPlayer player, float totalDamage) {
        // 分5次造成伤害，每次间隔2tick
        int ticks = 5;
        float damagePerTick = totalDamage / ticks;

        for (int i = 0; i < ticks; i++) {
            final float damage = damagePerTick;
            // 使用调度延迟伤害（简化实现，直接造成）
            if (i == 0) {
                TrueDamageHelper.applyWrappedTrueDamage(player, null, damage,
                        TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
            }
            // 注：完整实现需要使用服务端调度器分多tick造成
        }

        // 简化：直接造成全部伤害（TODO: 实现分段伤害）
        TrueDamageHelper.applyWrappedTrueDamage(player, null, totalDamage,
                TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
    }

    /**
     * 反弹伤害给周围敌人
     */
    private static int reflectDamageToNearby(EntityPlayer player, float totalDamage) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(REWRITE_RANGE);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive() && isHostile(e)
        );

        if (entities.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "周围没有敌人，反弹伤害消散..."
            ));
            return 0;
        }

        // 平分伤害
        float damagePerEntity = totalDamage / entities.size();

        for (EntityLivingBase target : entities) {
            TrueDamageHelper.applyWrappedTrueDamage(target, player, damagePerEntity,
                    TrueDamageHelper.TrueDamageFlag.EXECUTE);

            // 粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        target.posX, target.posY + target.height / 2, target.posZ,
                        10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        return entities.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // 落幕状态管理（供 ASM Hook 调用）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 进入落幕状态
     */
    public static void enterCurtainFall(EntityPlayer player) {
        CURTAIN_FALL_END_TIME.put(player.getUniqueID(),
                System.currentTimeMillis() + CURTAIN_FALL_DURATION_MS);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "═══════════════════════════════════"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "  ☠ 【落幕】" + TextFormatting.GRAY + " 死亡已被拦截"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "  ⚠ 接下来 30 秒："
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "    • 受到伤害 ×2"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "    • 禁止一切治疗"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "    • 再次死亡将无法阻止"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "═══════════════════════════════════"
        ));

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 0.5f);
    }

    /**
     * 检查是否在落幕状态
     */
    public static boolean isInCurtainFall(EntityPlayer player) {
        Long endTime = CURTAIN_FALL_END_TIME.get(player.getUniqueID());
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            CURTAIN_FALL_END_TIME.remove(player.getUniqueID());
            return false;
        }
        return true;
    }

    /**
     * 获取落幕剩余时间（秒）
     */
    public static int getCurtainFallRemaining(EntityPlayer player) {
        Long endTime = CURTAIN_FALL_END_TIME.get(player.getUniqueID());
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 检查玩家是否佩戴剧本
     */
    public static boolean hasScript(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() && bauble.getItem() instanceof ItemScriptOfFifthAct) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 获取玩家的剧本数据
     */
    public static ScriptData getScriptData(EntityPlayer player) {
        return SCRIPT_DATA.get(player.getUniqueID());
    }

    /**
     * 检查周围是否有敌人
     */
    private static boolean hasNearbyEnemies(EntityPlayer player) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(ENEMY_CHECK_RANGE);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive() && isHostile(e)
        );
        return !entities.isEmpty();
    }

    /**
     * 判断实体是否敌对
     */
    private static boolean isHostile(EntityLivingBase entity) {
        return entity instanceof IMob;
    }

    // ═══════════════════════════════════════════════════════════════
    // 粒子效果
    // ═══════════════════════════════════════════════════════════════

    private void spawnSuccessEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) player.world;
        ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                player.posX, player.posY + 1, player.posZ,
                30, 0.5, 0.5, 0.5, 0.1);
        ws.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.5F, 1.5F);
    }

    private void spawnSettleEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) player.world;
        ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                player.posX, player.posY + 1, player.posZ,
                20, 0.3, 0.5, 0.3, 0.1);
        ws.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS, 1.0F, 0.5F);
    }

    private static void spawnRewriteEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) player.world;
        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                player.posX, player.posY + 1, player.posZ,
                50, 0.5, 1.0, 0.5, 0.1);
        ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                player.posX, player.posY + 1.5, player.posZ,
                30, 0.3, 0.5, 0.3, 0.5);
        ws.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 1.0F, 0.5F);
    }

    // ═══════════════════════════════════════════════════════════════
    // 清理方法
    // ═══════════════════════════════════════════════════════════════

    public static void cleanupPlayer(UUID playerId) {
        SCRIPT_DATA.remove(playerId);
        CURTAIN_FALL_END_TIME.remove(playerId);
        SETTLING_PLAYERS.remove(playerId);
    }

    public static void clearAllState() {
        SCRIPT_DATA.clear();
        CURTAIN_FALL_END_TIME.clear();
        SETTLING_PLAYERS.clear();
    }

    // ═══════════════════════════════════════════════════════════════
    // Tooltip
    // ═══════════════════════════════════════════════════════════════

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        // ═══════════════════════════════════════════════════════════════
        // 标题与引言
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "═════════════════════════════════════");
        list.add(TextFormatting.LIGHT_PURPLE + "        " + TextFormatting.BOLD + "「第五幕剧本」");
        list.add(TextFormatting.DARK_PURPLE + "═════════════════════════════════════");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "  \"结局早已写好，");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "    哪怕演员对此一无所知。\"");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "                    ——无名剧作家");

        // 装备限制
        if (player == null || !CurseDeathHook.hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "  ⚠ 需要佩戴" + TextFormatting.DARK_PURPLE + "七咒之戒" +
                    TextFormatting.DARK_RED + "才能装备");
        }

        // ═══════════════════════════════════════════════════════════════
        // 契约警告 - 最醒目的部分
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.DARK_RED + "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
        list.add(TextFormatting.DARK_RED + "┃" + TextFormatting.RED + "          ☠ " +
                TextFormatting.BOLD + "不可撤销的契约" + TextFormatting.RESET +
                TextFormatting.RED + " ☠          " + TextFormatting.DARK_RED + "┃");
        list.add(TextFormatting.DARK_RED + "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        list.add(TextFormatting.DARK_RED + "┃" + TextFormatting.RED +
                "  戴上此物，你的灵魂便与之绑定    " + TextFormatting.DARK_RED + "┃");
        list.add(TextFormatting.DARK_RED + "┃" + TextFormatting.DARK_RED + "" + TextFormatting.BOLD +
                "   脱下它？那便是你故事的终章     " + TextFormatting.RESET + TextFormatting.DARK_RED + "┃");
        list.add(TextFormatting.DARK_RED + "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");

        // ═══════════════════════════════════════════════════════════════
        // 核心机制
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.GOLD + "━━━━━━━━━ " + TextFormatting.WHITE + "剧本之力" +
                TextFormatting.GOLD + " ━━━━━━━━━");
        list.add("");

        // 伤害缓存
        list.add(TextFormatting.AQUA + "  ◈ 命运延迟");
        list.add(TextFormatting.GRAY + "    受到的伤害不会立即扣除");
        list.add(TextFormatting.GRAY + "    而是被" + TextFormatting.LIGHT_PURPLE + "「记录」" +
                TextFormatting.GRAY + "在剧本之上");
        list.add("");

        // 戏剧张力
        list.add(TextFormatting.RED + "  ◈ 戏剧张力 " + TextFormatting.DARK_GRAY + "(核心被动)");
        list.add(TextFormatting.GRAY + "    承受的痛苦化为力量");
        list.add(TextFormatting.GRAY + "    缓存越高，" + TextFormatting.GOLD + "伤害加成" +
                TextFormatting.GRAY + "越高");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "    公式: " + TextFormatting.WHITE + "(缓存/血量)²" +
                TextFormatting.DARK_GRAY + " × 100%");
        list.add(TextFormatting.GRAY + "    ├ 50%缓存  → " + TextFormatting.YELLOW + "+25%" +
                TextFormatting.GRAY + " 伤害");
        list.add(TextFormatting.GRAY + "    ├ 100%缓存 → " + TextFormatting.GOLD + "+100%" +
                TextFormatting.GRAY + " 伤害");
        list.add(TextFormatting.GRAY + "    └ 150%缓存 → " + TextFormatting.RED + "+225%" +
                TextFormatting.GRAY + " 伤害");

        // ═══════════════════════════════════════════════════════════════
        // 缓存管理
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.GREEN + "━━━━━━━━━ " + TextFormatting.WHITE + "剧本管理" +
                TextFormatting.GREEN + " ━━━━━━━━━");
        list.add("");
        list.add(TextFormatting.GREEN + "  ◈ 净化之道");
        list.add(TextFormatting.GRAY + "    ├ 击杀敌人    → 清除 " + TextFormatting.GREEN + "25%" +
                TextFormatting.GRAY + " 缓存");
        list.add(TextFormatting.GRAY + "    ├ 造成伤害    → " + TextFormatting.GREEN + "10%" +
                TextFormatting.GRAY + " 抵消缓存");
        list.add(TextFormatting.GRAY + "    └ 消灭所有敌人 → 缓存" + TextFormatting.GREEN + "完全清零");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "    " + TextFormatting.ITALIC +
                "「只要表演不停，剧本就不会翻到最后一页」");

        // ═══════════════════════════════════════════════════════════════
        // 结算规则
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.YELLOW + "━━━━━━━━━ " + TextFormatting.WHITE + "谢幕条件" +
                TextFormatting.YELLOW + " ━━━━━━━━━");
        list.add("");
        list.add(TextFormatting.YELLOW + "  ◈ 强制结算");
        list.add(TextFormatting.GRAY + "    触发条件：");
        list.add(TextFormatting.GRAY + "    • 脱战 " + TextFormatting.YELLOW + "5秒" +
                TextFormatting.GRAY + "（周围仍有敌人）");
        list.add(TextFormatting.GRAY + "    • 缓存超过 " + TextFormatting.RED + "150%" +
                TextFormatting.GRAY + " 最大血量");
        list.add("");
        list.add(TextFormatting.GRAY + "    结算时承受 " + TextFormatting.YELLOW + "40%" +
                TextFormatting.GRAY + " 缓存伤害");

        // ═══════════════════════════════════════════════════════════════
        // 改写结局
        // ═══════════════════════════════════════════════════════════════
        list.add("");
        list.add(TextFormatting.LIGHT_PURPLE + "━━━━━━━━━ " + TextFormatting.WHITE + "改写结局" +
                TextFormatting.LIGHT_PURPLE + " ━━━━━━━━━");
        list.add("");
        list.add(TextFormatting.LIGHT_PURPLE + "  ◈ 命运的反转");
        list.add(TextFormatting.GRAY + "    结算时若血量 < " + TextFormatting.RED + "30%");
        list.add(TextFormatting.GRAY + "    ├ " + TextFormatting.GREEN + "0%" +
                TextFormatting.GRAY + " 自身伤害（完全免疫）");
        list.add(TextFormatting.GRAY + "    └ " + TextFormatting.GOLD + "200%" +
                TextFormatting.GRAY + " 伤害反弹给周围敌人");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "    " + TextFormatting.ITALIC +
                "「在最危险的时刻，剧本将为你改写命运」");

        // ═══════════════════════════════════════════════════════════════
        // 落幕状态（Shift展开）
        // ═══════════════════════════════════════════════════════════════
        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "━━━━━━━━━ " + TextFormatting.RED + "☠ 落幕 ☠" +
                    TextFormatting.DARK_RED + " ━━━━━━━━━");
            list.add("");
            list.add(TextFormatting.GRAY + "  当你本该" + TextFormatting.RED + "死亡" +
                    TextFormatting.GRAY + "时...");
            list.add(TextFormatting.GRAY + "  剧本会拦截你的死亡");
            list.add(TextFormatting.GRAY + "  留下 " + TextFormatting.RED + "1 血" +
                    TextFormatting.GRAY + "，进入" + TextFormatting.DARK_RED + "「落幕」" +
                    TextFormatting.GRAY + "状态");
            list.add("");
            list.add(TextFormatting.DARK_RED + "  ▼ 落幕惩罚（30秒）");
            list.add(TextFormatting.RED + "    • 受到伤害 " + TextFormatting.DARK_RED + "×2");
            list.add(TextFormatting.RED + "    • " + TextFormatting.DARK_RED + "禁止" +
                    TextFormatting.RED + "一切治疗");
            list.add(TextFormatting.DARK_RED + "    • 再次死亡" + TextFormatting.DARK_RED +
                    "" + TextFormatting.BOLD + "无法阻止");
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "    " + TextFormatting.ITALIC +
                    "「这是命运给你的最后机会...」");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "  按住 " + TextFormatting.GRAY + "Shift" +
                    TextFormatting.DARK_GRAY + " 查看" + TextFormatting.DARK_RED + "【落幕状态】");
        }

        // ═══════════════════════════════════════════════════════════════
        // 当前状态（佩戴时显示）
        // ═══════════════════════════════════════════════════════════════
        if (player != null && hasScript(player)) {
            ScriptData data = getScriptData(player);
            if (data != null) {
                list.add("");
                list.add(TextFormatting.AQUA + "━━━━━━━━━ " + TextFormatting.WHITE + "当前状态" +
                        TextFormatting.AQUA + " ━━━━━━━━━");
                list.add("");

                float bufferRatio = data.bufferedDamage / player.getMaxHealth();
                float bonus = getDamageBonus(bufferRatio);
                float overloadLimit = player.getMaxHealth() * OVERLOAD_THRESHOLD;

                // 缓存条
                TextFormatting bufferColor = bufferRatio >= 1.4f ? TextFormatting.DARK_RED :
                        bufferRatio >= 1.0f ? TextFormatting.RED :
                                bufferRatio >= 0.5f ? TextFormatting.YELLOW : TextFormatting.GREEN;

                list.add(TextFormatting.GRAY + "  缓存: " + bufferColor +
                        String.format("%.0f", data.bufferedDamage) +
                        TextFormatting.GRAY + " / " + TextFormatting.WHITE +
                        String.format("%.0f", overloadLimit));

                list.add(TextFormatting.GRAY + "  张力加成: " + TextFormatting.GOLD + "+" +
                        String.format("%.0f%%", bonus * 100));

                // 警告信息
                if (bufferRatio >= 1.4f) {
                    list.add("");
                    list.add(TextFormatting.DARK_RED + "  ⚠ " + TextFormatting.RED +
                            TextFormatting.BOLD + "超载临界！即将强制结算！");
                } else if (bufferRatio >= 1.0f) {
                    list.add(TextFormatting.RED + "  ! 危险区域");
                }

                // 改写结局提示
                float healthRatio = player.getHealth() / player.getMaxHealth();
                if (healthRatio <= REWRITE_HEALTH_THRESHOLD) {
                    list.add(TextFormatting.LIGHT_PURPLE + "  ✦ 改写结局就绪");
                }

                // 落幕状态
                if (isInCurtainFall(player)) {
                    list.add("");
                    list.add(TextFormatting.DARK_RED + "  ☠ 【落幕中】" +
                            TextFormatting.RED + " 剩余 " + getCurtainFallRemaining(player) + " 秒");
                    list.add(TextFormatting.RED + "    受伤×2 | 禁止治疗 | 可能真死");
                }
            }
        }

        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "═════════════════════════════════════");
    }
}
