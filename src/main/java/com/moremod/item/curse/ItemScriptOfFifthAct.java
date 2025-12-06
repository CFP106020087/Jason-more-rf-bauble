package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
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
 * 外观：一卷总是滴着血的羊皮纸，系在腰间
 *
 * 基础效果【宿命论】：
 * - 伤害延迟：受到的所有伤害不会立即扣除，而是被"记录"在剧本上
 * - 谢幕：脱离战斗（5秒内未受到伤害且未攻击）或记录伤害超过当前血量时，一次性结算
 *
 * 主动能力【改写结局】：
 * - 结算前若血量低于 10%，剧本燃烧
 * - 将即将结算的 50% 伤害反弹给周围敌人
 * - 自己只承受剩余的 50%
 * - 有 60 秒冷却
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemScriptOfFifthAct extends Item implements IBauble {

    // 脱离战斗时间（毫秒）
    private static final long OUT_OF_COMBAT_MS = 5000;
    // 改写结局的血量阈值
    private static final float REWRITE_HEALTH_THRESHOLD = 0.1f;
    // 改写结局的伤害反弹比例
    private static final float REWRITE_REFLECT_RATIO = 0.5f;
    // 改写结局的反弹范围
    private static final double REWRITE_RANGE = 8.0;
    // 改写结局冷却时间（毫秒）
    private static final long REWRITE_COOLDOWN_MS = 60000;

    // 玩家伤害缓存数据
    private static final Map<UUID, ScriptData> SCRIPT_DATA = new ConcurrentHashMap<>();

    private static class ScriptData {
        float bufferedDamage = 0;
        long lastCombatTime = 0;
        long rewriteCooldownEnd = 0;
        boolean isSettling = false; // 防止递归

        void addDamage(float damage) {
            this.bufferedDamage += damage;
            this.lastCombatTime = System.currentTimeMillis();
        }

        void recordAttack() {
            this.lastCombatTime = System.currentTimeMillis();
        }

        boolean isOutOfCombat() {
            return System.currentTimeMillis() - lastCombatTime > OUT_OF_COMBAT_MS;
        }

        boolean canRewrite() {
            return System.currentTimeMillis() > rewriteCooldownEnd;
        }

        void triggerRewriteCooldown() {
            this.rewriteCooldownEnd = System.currentTimeMillis() + REWRITE_COOLDOWN_MS;
        }

        int getRewriteCooldownSeconds() {
            long remaining = rewriteCooldownEnd - System.currentTimeMillis();
            return remaining > 0 ? (int) (remaining / 1000) : 0;
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

        // 条件1: 脱离战斗
        if (data.bufferedDamage > 0 && data.isOutOfCombat()) {
            shouldSettle = true;
            settleReason = "剧终：谢幕";
        }

        // 条件2: 缓存伤害超过当前血量
        if (data.bufferedDamage >= player.getHealth()) {
            shouldSettle = true;
            settleReason = "剧终：命定之死";
        }

        if (shouldSettle) {
            settleDamage(player, data, settleReason);
        }

        // 显示当前状态（每秒更新一次）
        if (entity.world.getTotalWorldTime() % 20 == 0 && data.bufferedDamage > 0) {
            long timeSinceCombat = System.currentTimeMillis() - data.lastCombatTime;
            int secondsLeft = (int) ((OUT_OF_COMBAT_MS - timeSinceCombat) / 1000);
            if (secondsLeft > 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "📜 剧本记录: " +
                        TextFormatting.RED + String.format("%.1f", data.bufferedDamage) +
                        TextFormatting.GRAY + " 伤害 | 谢幕倒计时: " +
                        TextFormatting.GOLD + secondsLeft + "s"
                ), true);
            }
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            UUID uuid = player.getUniqueID();
            ScriptData data = SCRIPT_DATA.get(uuid);

            // 卸下时立即结算所有伤害
            if (data != null && data.bufferedDamage > 0) {
                settleDamage((EntityPlayer) player, data, "剧本被撕毁");
            }

            SCRIPT_DATA.remove(uuid);
        }
    }

    // ========== 事件处理 ==========

    /**
     * 拦截伤害，记录到剧本
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

        // 记录伤害
        float damage = event.getAmount();
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
     * 记录玩家攻击（用于脱离战斗判定）
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
     * 结算所有缓存伤害
     */
    private static void settleDamage(EntityPlayer player, ScriptData data, String reason) {
        if (data.bufferedDamage <= 0) return;

        data.isSettling = true;
        float totalDamage = data.bufferedDamage;
        float currentHealth = player.getHealth();
        float healthRatio = currentHealth / player.getMaxHealth();

        // 检查是否触发【改写结局】
        boolean rewriteTriggered = false;
        if (healthRatio <= REWRITE_HEALTH_THRESHOLD && data.canRewrite()) {
            rewriteTriggered = true;
            reason = "改写结局！";
        }

        if (rewriteTriggered) {
            // 改写结局：反弹 50%，自己承受 50%
            float reflectDamage = totalDamage * REWRITE_REFLECT_RATIO;
            float selfDamage = totalDamage * (1 - REWRITE_REFLECT_RATIO);

            // 反弹给周围敌人
            reflectDamageToNearby(player, reflectDamage);

            // 自己承受剩余伤害
            applySettledDamage(player, selfDamage);

            // 触发冷却
            data.triggerRewriteCooldown();

            // 效果提示
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✨ " + reason +
                    TextFormatting.GRAY + " 反弹 " +
                    TextFormatting.RED + String.format("%.1f", reflectDamage) +
                    TextFormatting.GRAY + " 伤害给周围敌人！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "剧本燃烧，" +
                    TextFormatting.GOLD + "60" + TextFormatting.GRAY + " 秒后可再次改写"
            ));

            // 燃烧粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        player.posX, player.posY + 1, player.posZ,
                        50, 0.5, 1.0, 0.5, 0.1);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        player.posX, player.posY + 1, player.posZ,
                        30, 0.3, 0.8, 0.3, 0.0);
                ws.playSound(null, player.getPosition(),
                        SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.PLAYERS, 1.0F, 0.8F);
            }
        } else {
            // 正常结算：承受全部伤害
            applySettledDamage(player, totalDamage);

            // 效果提示
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "📜 " + reason +
                    TextFormatting.GRAY + " 结算 " +
                    TextFormatting.RED + String.format("%.1f", totalDamage) +
                    TextFormatting.GRAY + " 伤害！"
            ));

            // 如果改写结局在冷却中
            if (!data.canRewrite() && healthRatio <= REWRITE_HEALTH_THRESHOLD) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "改写结局冷却中... " +
                        TextFormatting.GOLD + data.getRewriteCooldownSeconds() + "s"
                ));
            }

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
     * 应用结算伤害（绕过剧本效果）
     */
    private static void applySettledDamage(EntityPlayer player, float damage) {
        // 直接设置血量，避免触发事件
        float newHealth = player.getHealth() - damage;
        if (newHealth <= 0) {
            // 死亡
            player.setHealth(0.001f); // 设置极低血量触发死亡
            player.attackEntityFrom(DamageSource.MAGIC, 1000); // 确保死亡
        } else {
            player.setHealth(newHealth);
        }
    }

    /**
     * 反弹伤害给周围敌人
     */
    private static void reflectDamageToNearby(EntityPlayer player, float totalDamage) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(REWRITE_RANGE);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive() && !e.isOnSameTeam(player)
        );

        if (entities.isEmpty()) {
            // 没有敌人，伤害消散
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "周围没有敌人，反弹伤害消散..."
            ));
            return;
        }

        // 平分伤害
        float damagePerEntity = totalDamage / entities.size();

        for (EntityLivingBase target : entities) {
            TrueDamageHelper.applyTrueDamage(target, player, damagePerEntity);

            // 粒子效果
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;

                // 从玩家到目标的连线
                double dx = target.posX - player.posX;
                double dy = (target.posY + target.height / 2) - (player.posY + 1);
                double dz = target.posZ - player.posZ;

                for (int i = 0; i < 10; i++) {
                    double t = i / 10.0;
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            player.posX + dx * t,
                            player.posY + 1 + dy * t,
                            player.posZ + dz * t,
                            1, 0, 0, 0, 0);
                }

                // 目标位置爆炸效果
                ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        target.posX, target.posY + target.height / 2, target.posZ,
                        10, 0.2, 0.2, 0.2, 0.1);
            }
        }
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
        list.add(TextFormatting.GRAY + "  · 脱离战斗 " + TextFormatting.GOLD + "5" +
                TextFormatting.GRAY + " 秒后结算");
        list.add(TextFormatting.GRAY + "  · 记录伤害 ≥ 当前血量时立即结算");

        list.add("");
        list.add(TextFormatting.LIGHT_PURPLE + "◆ 改写结局 " + TextFormatting.GRAY + "(被动)");
        list.add(TextFormatting.GRAY + "  结算时若血量 < " + TextFormatting.RED + "10%" +
                TextFormatting.GRAY + ":");
        list.add(TextFormatting.GRAY + "  · " + TextFormatting.GOLD + "50%" +
                TextFormatting.GRAY + " 伤害反弹给周围 " + TextFormatting.AQUA + "8" +
                TextFormatting.GRAY + " 格内敌人");
        list.add(TextFormatting.GRAY + "  · 自己只承受剩余 " + TextFormatting.RED + "50%");
        list.add(TextFormatting.DARK_GRAY + "  冷却: 60 秒");

        list.add("");
        list.add(TextFormatting.DARK_RED + "◆ 代价");
        list.add(TextFormatting.RED + "  无法逃离命运");
        list.add(TextFormatting.RED + "  卸下饰品时立即结算所有伤害");

        // 当前状态
        if (player != null && hasScript(player)) {
            ScriptData data = getScriptData(player);
            if (data != null) {
                list.add("");
                list.add(TextFormatting.GOLD + "当前状态:");
                list.add(TextFormatting.GRAY + "  记录伤害: " + TextFormatting.RED + String.format("%.1f", data.bufferedDamage));
                if (!data.canRewrite()) {
                    list.add(TextFormatting.GRAY + "  改写冷却: " + TextFormatting.GOLD + data.getRewriteCooldownSeconds() + "s");
                } else {
                    list.add(TextFormatting.GREEN + "  改写结局就绪");
                }
            }
        }

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "战斗中感觉自己无敌");
            list.add(TextFormatting.GRAY + "但如果不能在剧终前杀光敌人");
            list.add(TextFormatting.GRAY + "最后死的就是你");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多");
        }
    }
}
