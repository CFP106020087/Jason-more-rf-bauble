package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
 * 饰品类型：杂项 (Charm)
 *
 * 基础效果【宿命论】：
 * - 伤害延迟：受到的所有伤害不会立即扣除，而是被"记录"在剧本上
 * - 记录的是原始伤害（不含倍率）
 * - 谢幕条件：
 *   1. 记录伤害 ≥ 当前血量时立即结算
 *   2. 周围没有敌人时，伤害清零（杀光敌人则不结算）
 *
 * 结算规则：
 * - 正常结算：承受 50% 的记录伤害
 * - 改写结局（血量 < 30%）：300% 反弹，自己不受伤害
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemScriptOfFifthAct extends Item implements IBauble {

    // 检查敌人的范围
    private static final double ENEMY_CHECK_RANGE = 16.0;
    // 改写结局的血量阈值
    private static final float REWRITE_HEALTH_THRESHOLD = 0.3f;
    // 改写结局的伤害反弹倍率
    private static final float REWRITE_REFLECT_MULTIPLIER = 3.0f;
    // 改写结局的反弹范围
    private static final double REWRITE_RANGE = 8.0;
    // 正常结算的伤害比例（50%）
    private static final float SETTLEMENT_DAMAGE_RATIO = 0.5f;

    // 玩家伤害缓存数据
    private static final Map<UUID, ScriptData> SCRIPT_DATA = new ConcurrentHashMap<>();

    private static class ScriptData {
        float bufferedDamage = 0;
        long lastCombatTime = 0;
        boolean isSettling = false; // 防止递归

        void addDamage(float damage) {
            this.bufferedDamage += damage;
            this.lastCombatTime = System.currentTimeMillis();
        }

        void recordAttack() {
            this.lastCombatTime = System.currentTimeMillis();
        }

        void clearDamage() {
            this.bufferedDamage = 0;
        }
    }

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
        return hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            SCRIPT_DATA.put(player.getUniqueID(), new ScriptData());
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        UUID uuid = player.getUniqueID();
        ScriptData data = SCRIPT_DATA.get(uuid);

        if (data == null || data.isSettling) return;

        // 检查是否需要结算
        boolean shouldSettle = false;
        String settleReason = "";

        // 条件1: 缓存伤害超过当前血量 -> 立即结算
        if (data.bufferedDamage >= player.getHealth()) {
            shouldSettle = true;
            settleReason = "剧终：命定之死";
        }

        // 条件2: 周围没有敌人 -> 杀光敌人，伤害清零
        if (data.bufferedDamage > 0 && !shouldSettle) {
            if (!hasNearbyEnemies(player)) {
                // 杀光敌人，不结算，直接清零
                data.clearDamage();
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 剧本改写成功！" +
                        TextFormatting.GRAY + " 所有敌人已被消灭，伤害记录清零"
                ));

                // 胜利粒子效果
                if (player.world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) player.world;
                    ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                            player.posX, player.posY + 1, player.posZ,
                            30, 0.5, 0.5, 0.5, 0.1);
                    ws.playSound(null, player.getPosition(),
                            SoundEvents.ENTITY_PLAYER_LEVELUP,
                            SoundCategory.PLAYERS, 0.5F, 1.5F);
                }
            }
        }

        if (shouldSettle) {
            settleDamage(player, data, settleReason);
        }

        // 显示当前状态（每秒更新一次）
        if (entity.world.getTotalWorldTime() % 20 == 0 && data.bufferedDamage > 0) {
            float healthRatio = player.getHealth() / player.getMaxHealth();
            String healthStatus = healthRatio <= REWRITE_HEALTH_THRESHOLD ?
                    TextFormatting.GREEN + "改写就绪" :
                    TextFormatting.GRAY + "HP>" + (int)(REWRITE_HEALTH_THRESHOLD * 100) + "%";

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "📜 剧本记录: " +
                    TextFormatting.RED + String.format("%.1f", data.bufferedDamage) +
                    TextFormatting.GRAY + " 伤害 | " + healthStatus
            ), true);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            UUID uuid = player.getUniqueID();
            ScriptData data = SCRIPT_DATA.get(uuid);

            // 卸下时立即结算所有伤害（无法触发改写结局）
            if (data != null && data.bufferedDamage > 0) {
                forceSettleDamage((EntityPlayer) player, data);
            }

            SCRIPT_DATA.remove(uuid);
        }
    }

    // ========== 事件处理 ==========

    /**
     * 拦截伤害，记录到剧本（记录原始伤害，不含倍率）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否佩戴剧本
        if (!hasScript(player)) return;

        // 检查是否有七咒联动
        if (!hasCursedRing(player)) return;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data == null || data.isSettling) return;

        // 获取原始伤害（在任何倍率应用之前）
        // 注意：由于我们使用 HIGHEST 优先级，这应该是最早的伤害值
        float damage = event.getAmount();

        // 检查是否是七咒翻倍后的伤害，如果是则还原
        // 七咒之戒会将伤害翻倍，我们需要记录原始值
        // 这里假设七咒翻倍在我们之后处理，所以 damage 应该是原始值

        data.addDamage(damage);

        // 取消实际伤害
        event.setCanceled(true);

        // 显示记录
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "📜 记录伤害: " +
                TextFormatting.RED + String.format("%.1f", damage) +
                TextFormatting.GRAY + " (总计: " +
                TextFormatting.GOLD + String.format("%.1f", data.bufferedDamage) + ")"
        ), true);

        // 血迹粒子效果（视觉假象）
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.REDSTONE,
                    player.posX, player.posY + 1, player.posZ,
                    5, 0.3, 0.3, 0.3, 0);
        }
    }

    /**
     * 记录玩家攻击（用于战斗状态判定）
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        if (!hasScript(player)) return;

        ScriptData data = SCRIPT_DATA.get(player.getUniqueID());
        if (data != null) {
            data.recordAttack();
        }
    }

    // ========== 核心逻辑 ==========

    /**
     * 检查周围是否有敌人
     */
    private static boolean hasNearbyEnemies(EntityPlayer player) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(ENEMY_CHECK_RANGE);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive() && isHostile(e, player)
        );
        return !entities.isEmpty();
    }

    /**
     * 判断实体是否对玩家敌对
     */
    private static boolean isHostile(EntityLivingBase entity, EntityPlayer player) {
        if (entity instanceof EntityMob) return true;
        if (!entity.isOnSameTeam(player)) {
            if (entity instanceof EntityPlayer) return false;
            return true;
        }
        return false;
    }

    /**
     * 结算所有缓存伤害
     */
    private static void settleDamage(EntityPlayer player, ScriptData data, String reason) {
        if (data.bufferedDamage <= 0) return;

        data.isSettling = true;
        float totalDamage = data.bufferedDamage;
        float currentHealth = player.getHealth();
        float healthRatio = currentHealth / player.getMaxHealth();

        // 检查是否触发【改写结局】
        boolean rewriteTriggered = healthRatio <= REWRITE_HEALTH_THRESHOLD;

        if (rewriteTriggered) {
            // 改写结局：300% 反弹，自己也承受 50% 伤害
            float reflectDamage = totalDamage * REWRITE_REFLECT_MULTIPLIER;
            float settleDamage = totalDamage * SETTLEMENT_DAMAGE_RATIO;

            // 反弹给周围敌人
            int enemiesHit = reflectDamageToNearby(player, reflectDamage);

            // 自己也承受 50% 伤害
            applySettledDamage(player, settleDamage);

            // 效果提示
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✨ 改写结局！" +
                    TextFormatting.GRAY + " 反弹 " +
                    TextFormatting.RED + String.format("%.0f", reflectDamage) +
                    TextFormatting.GRAY + " 伤害给 " +
                    TextFormatting.GOLD + enemiesHit +
                    TextFormatting.GRAY + " 个敌人！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "承受 " +
                    TextFormatting.RED + String.format("%.1f", settleDamage) +
                    TextFormatting.YELLOW + " 伤害 (50%)"
            ));

            // 粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        player.posX, player.posY + 1, player.posZ,
                        50, 0.5, 1.0, 0.5, 0.1);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        player.posX, player.posY + 1, player.posZ,
                        40, 0.5, 0.8, 0.5, 0.0);
                ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                        player.posX, player.posY + 1.5, player.posZ,
                        30, 0.3, 0.5, 0.3, 0.5);
                ws.playSound(null, player.getPosition(),
                        SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.PLAYERS, 1.0F, 0.8F);
                ws.playSound(null, player.getPosition(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS, 1.0F, 0.5F);
            }
        } else {
            // 正常结算：承受 50% 伤害
            float settleDamage = totalDamage * SETTLEMENT_DAMAGE_RATIO;
            applySettledDamage(player, settleDamage);

            // 效果提示
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "📜 " + reason +
                    TextFormatting.GRAY + " 结算 " +
                    TextFormatting.RED + String.format("%.1f", settleDamage) +
                    TextFormatting.GRAY + " 伤害 (50%)"
            ));

            // 提示改写结局条件
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "(血量低于 " +
                    TextFormatting.GOLD + "30%" +
                    TextFormatting.GRAY + " 时可触发改写结局)"
            ));

            // 结算粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        player.posX, player.posY + 1, player.posZ,
                        20, 0.3, 0.5, 0.3, 0.1);
                ws.playSound(null, player.getPosition(),
                        SoundEvents.ENTITY_PLAYER_HURT,
                        SoundCategory.PLAYERS, 1.0F, 0.5F);
            }
        }

        // 清空缓存
        data.bufferedDamage = 0;
        data.isSettling = false;
    }

    /**
     * 强制结算伤害（卸下饰品时，无法触发改写结局，但仍然只承受50%）
     */
    private static void forceSettleDamage(EntityPlayer player, ScriptData data) {
        if (data.bufferedDamage <= 0) return;

        data.isSettling = true;
        float totalDamage = data.bufferedDamage;
        float settleDamage = totalDamage * SETTLEMENT_DAMAGE_RATIO;

        // 直接承受 50% 伤害
        applySettledDamage(player, settleDamage);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "📜 剧本被撕毁！" +
                TextFormatting.GRAY + " 强制结算 " +
                TextFormatting.RED + String.format("%.1f", settleDamage) +
                TextFormatting.GRAY + " 伤害 (50%)"
        ));

        data.bufferedDamage = 0;
        data.isSettling = false;
    }

    /**
     * 应用结算伤害（使用真伤）
     */
    private static void applySettledDamage(EntityPlayer player, float damage) {
        TrueDamageHelper.applyWrappedTrueDamage(player, null, damage,
                TrueDamageHelper.TrueDamageFlag.EXECUTE);
    }

    /**
     * 反弹伤害给周围敌人
     * @return 命中的敌人数量
     */
    private static int reflectDamageToNearby(EntityPlayer player, float totalDamage) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(REWRITE_RANGE);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive() && isHostile(e, player)
        );

        if (entities.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "周围没有敌人，反弹伤害消散..."
            ));
            return 0;
        }

        // 平分伤害给所有敌人
        float damagePerEntity = totalDamage / entities.size();

        for (EntityLivingBase target : entities) {
            TrueDamageHelper.applyWrappedTrueDamage(target, player, damagePerEntity,
                    TrueDamageHelper.TrueDamageFlag.EXECUTE);

            // 粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;

                // 从玩家到目标的连线
                double dx = target.posX - player.posX;
                double dy = (target.posY + target.height / 2) - (player.posY + 1);
                double dz = target.posZ - player.posZ;

                for (int i = 0; i < 15; i++) {
                    double t = i / 15.0;
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            player.posX + dx * t,
                            player.posY + 1 + dy * t,
                            player.posZ + dz * t,
                            1, 0, 0, 0, 0);
                }

                // 目标位置爆炸效果
                ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        target.posX, target.posY + target.height / 2, target.posZ,
                        15, 0.3, 0.3, 0.3, 0.1);
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        target.posX, target.posY + target.height / 2, target.posZ,
                        10, 0.2, 0.2, 0.2, 0.1);
            }
        }

        return entities.size();
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴第五幕剧本
     */
    public static boolean hasScript(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemScriptOfFifthAct) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取玩家的剧本数据（用于 tooltip）
     */
    private static ScriptData getScriptData(EntityPlayer player) {
        return SCRIPT_DATA.get(player.getUniqueID());
    }

    // ========== 物品信息 ==========

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "\"结局早已写好，");
        list.add(TextFormatting.DARK_GRAY + "  哪怕演员对此一无所知。\"");

        if (player == null || !hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "⚠ 需要佩戴七咒之戒才能装备");
        }

        list.add("");
        list.add(TextFormatting.GOLD + "◆ 宿命论");
        list.add(TextFormatting.GRAY + "  所有伤害被" + TextFormatting.LIGHT_PURPLE + "记录" +
                TextFormatting.GRAY + "而非立即扣血");
        list.add(TextFormatting.GRAY + "  " + TextFormatting.YELLOW + "谢幕条件：");
        list.add(TextFormatting.GRAY + "  · 记录伤害 ≥ 当前血量时" + TextFormatting.RED + "立即结算");
        list.add(TextFormatting.GRAY + "  · " + TextFormatting.GREEN + "杀光敌人" +
                TextFormatting.GRAY + " → 伤害清零，不结算");

        list.add("");
        list.add(TextFormatting.AQUA + "◆ 结算规则");
        list.add(TextFormatting.GRAY + "  正常结算：承受 " + TextFormatting.YELLOW + "50%" +
                TextFormatting.GRAY + " 记录伤害");

        list.add("");
        list.add(TextFormatting.LIGHT_PURPLE + "◆ 改写结局 " + TextFormatting.GRAY + "(被动)");
        list.add(TextFormatting.GRAY + "  结算时若血量 < " + TextFormatting.RED + "30%" +
                TextFormatting.GRAY + ":");
        list.add(TextFormatting.GRAY + "  · " + TextFormatting.GOLD + "300%" +
                TextFormatting.GRAY + " 伤害反弹给周围 " + TextFormatting.AQUA + "8" +
                TextFormatting.GRAY + " 格内敌人");
        list.add(TextFormatting.YELLOW + "  · 自己仍承受 50% 伤害");
        list.add(TextFormatting.DARK_GRAY + "  无冷却");

        list.add("");
        list.add(TextFormatting.DARK_RED + "◆ 代价");
        list.add(TextFormatting.RED + "  卸下饰品时" + TextFormatting.DARK_RED + "强制结算" +
                TextFormatting.RED + " (仍为50%)");

        // 当前状态
        if (player != null && hasScript(player)) {
            ScriptData data = getScriptData(player);
            if (data != null) {
                list.add("");
                list.add(TextFormatting.GOLD + "当前状态:");
                list.add(TextFormatting.GRAY + "  记录伤害: " + TextFormatting.RED + String.format("%.1f", data.bufferedDamage));
                list.add(TextFormatting.GRAY + "  结算伤害: " + TextFormatting.YELLOW + String.format("%.1f", data.bufferedDamage * SETTLEMENT_DAMAGE_RATIO) + " (50%)");
                float healthRatio = player.getHealth() / player.getMaxHealth();
                if (healthRatio <= REWRITE_HEALTH_THRESHOLD) {
                    list.add(TextFormatting.GREEN + "  ✓ 改写结局就绪");
                } else {
                    list.add(TextFormatting.GRAY + "  HP: " + TextFormatting.YELLOW + String.format("%.0f%%", healthRatio * 100) +
                            TextFormatting.GRAY + " (需<30%)");
                }
            }
        }

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "战斗中不会掉血");
            list.add(TextFormatting.GRAY + "杀光敌人 = 伤害清零");
            list.add(TextFormatting.GRAY + "低血量触发改写 = 反杀翻盘");
            list.add(TextFormatting.GRAY + "正常结算只承受 50% 伤害");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多");
        }
    }
}
