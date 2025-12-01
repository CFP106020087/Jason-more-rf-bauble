package com.moremod.system.ascension;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.config.ShambhalaConfig;
import com.moremod.core.ShambhalaDeathHook;
import com.moremod.item.shambhala.ItemShambhalaVeil;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import com.moremod.moremod;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 香巴拉事件处理器
 * Shambhala Event Handler
 *
 * 处理所有香巴拉相关的游戏事件：
 * - 能量护盾（伤害吸收）
 * - 反伤（AoE + 循环防护）
 * - 死亡拦截
 * - 伤害输出削弱
 * - 高人性时间累计
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ShambhalaEventHandler {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    // ========== 世界Tick处理（宁静光环） ==========

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        // 处理所有活跃的宁静光环
        ItemShambhalaVeil.tickAuras(event.world);
    }

    // ========== 仇恨目标拦截（宁静光环） ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        // 如果目标在宁静光环内，阻止生物获取新仇恨
        if (event.getTarget() == null) return;

        // 检查攻击者是否在任何活跃的宁静光环中
        if (ItemShambhalaVeil.isInPeaceAura(event.getEntityLiving())) {
            // 清除仇恨目标
            if (event.getEntityLiving() instanceof EntityMob) {
                EntityMob mob = (EntityMob) event.getEntityLiving();
                mob.setAttackTarget(null);
            }
        }
    }

    // ========== 玩家Tick处理 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 未升格玩家：累计高人性时间
        if (data.getAscensionRoute() == AscensionRoute.NONE) {
            float humanity = data.getHumanity();
            if (humanity >= ShambhalaConfig.highHumanityThreshold) {
                data.addHighHumanityTicks(1);
            }
            return;
        }

        // 香巴拉玩家：固定人性值为100
        if (!ShambhalaHandler.isShambhala(player)) return;

        if (data.getHumanity() < 100) {
            data.setHumanity(100);
        }

        // 血量锁定由 First Aid 兼容层处理 (ShambhalaFirstAidCompat)
        // 原版 MC 的死亡由 onLivingDeath 处理
    }

    // ========== 原始伤害捕获（用于比例反伤） ==========
    // LivingAttackEvent 在护甲计算前触发，能拿到原始伤害
    // 这比 ASM 更简单，且不影响现有的减伤逻辑

    private static final ThreadLocal<Float> capturedRawDamage = ThreadLocal.withInitial(() -> 0f);
    private static final ThreadLocal<Entity> capturedAttacker = ThreadLocal.withInitial(() -> null);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // 为香巴拉玩家捕获原始伤害（护甲前）
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (ShambhalaHandler.isShambhala(player)) {
                DamageSource source = event.getSource();

                // 跳过真伤和反伤（不重复捕获）
                if (!TrueDamageHelper.isInTrueDamageContext()
                    && !TrueDamageHelper.isTrueDamageSource(source)
                    && !ShambhalaHandler.isShambhalaReflectDamage(source)) {

                    capturedRawDamage.set(event.getAmount());
                    capturedAttacker.set(source.getTrueSource());
                }
            }
        }
        // 不取消事件，让伤害继续流向护甲计算
    }

    /**
     * 获取并清除捕获的原始伤害
     */
    public static float getAndClearCapturedRawDamage() {
        float raw = capturedRawDamage.get();
        capturedRawDamage.set(0f);
        return raw;
    }

    /**
     * 获取并清除捕获的攻击者
     */
    public static Entity getAndClearCapturedAttacker() {
        Entity attacker = capturedAttacker.get();
        capturedAttacker.set(null);
        return attacker;
    }

    // ========== 伤害处理（HIGHEST - 能量吸收 + 输出削弱） ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 跳过香巴拉反伤造成的伤害（防止循环）
        if (ShambhalaHandler.isShambhalaReflectDamage(event.getSource())) {
            return;
        }

        // 香巴拉受伤：能量护盾吸收（在护甲计算前）
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (ShambhalaHandler.isShambhala(player)) {
                float originalDamage = event.getAmount();

                // 尝试用能量吸收伤害
                float remainingDamage = ShambhalaHandler.tryAbsorbDamage(player, originalDamage);

                if (remainingDamage <= 0) {
                    event.setCanceled(true);
                    return;
                }

                event.setAmount(remainingDamage);
            }
        }

        // 香巴拉造成伤害：削弱输出
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) event.getSource().getTrueSource();

            if (ShambhalaHandler.isShambhala(attacker)) {
                // 跳过反伤伤害的削弱
                if (!ShambhalaHandler.isReflecting(attacker)) {
                    float reduction = (float) ShambhalaConfig.damageOutputReduction;
                    event.setAmount(event.getAmount() * (1 - reduction));
                }
            }
        }
    }

    // ========== 伤害处理（LOWEST - 壁垒减伤 + 友军保护） ==========
    // 注意：反伤已移至 ShambhalaDeathHook（ASM层级），使用真伤绕过护甲和能量吸收

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        // 跳过香巴拉反伤造成的伤害（防止循环）
        if (ShambhalaHandler.isShambhalaReflectDamage(event.getSource())) {
            return;
        }

        EntityLivingBase target = event.getEntityLiving();
        float damage = event.getAmount();

        // ========== 香巴拉受伤：壁垒减伤 ==========
        // 注意：反伤已移至 ShambhalaDeathHook.checkAndAbsorbDamage (ASM层级)
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;

            if (ShambhalaHandler.isShambhala(player)) {
                // 壁垒减伤（Bastion）
                float bastionReduction = (float) ShambhalaConfig.bastionDamageReduction;
                if (bastionReduction > 0) {
                    damage = damage * (1 - bastionReduction);
                    event.setAmount(damage);
                }
                // 反伤现在由 ASM 钩子处理，使用 TrueDamageHelper 造成真伤
            }
        }

        // ========== 圣域友军保护（Sanctuary） ==========
        if (target instanceof EntityLivingBase && !(target instanceof EntityPlayer)) {
            // 检查附近是否有装备圣域的香巴拉玩家
            for (EntityPlayer nearbyPlayer : target.world.getEntitiesWithinAABB(EntityPlayer.class,
                    target.getEntityBoundingBox().grow(ShambhalaConfig.sanctuaryAuraRange))) {

                if (ShambhalaHandler.isShambhala(nearbyPlayer) && ShambhalaItems.hasSanctuary(nearbyPlayer)) {
                    // 检查是否是友方
                    if (com.moremod.item.shambhala.ItemShambhalaSanctuary.isAlly(nearbyPlayer, target)) {
                        // 应用友军减伤
                        float allyProtection = (float) ShambhalaConfig.sanctuaryAllyProtection;
                        damage = damage * (1 - allyProtection);
                        event.setAmount(damage);
                        break; // 只应用一次
                    }
                }
            }
        }
    }

    // ========== 死亡拦截（原版MC后备） ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (ShambhalaHandler.isShambhala(player)) {
            // 检查是否有能量（这是原版MC的后备，First Aid由专门的兼容层处理）
            if (ShambhalaHandler.getCurrentEnergy(player) > 0) {
                // 拦截死亡
                event.setCanceled(true);

                // 锁定血量
                player.setHealth((float) ShambhalaConfig.coreHealthLock);

                // 消耗大量能量作为代价
                ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.energyPerDamage * 10);

                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "[香巴拉] " +
                                TextFormatting.WHITE + "永恒齿轮庇佑...死亡被拒绝"
                ));

                LOGGER.info("[Shambhala] Intercepted death for player {}", player.getName());
            }
            // 没有能量 = 真正死亡（这是香巴拉的核心代价）
        }
    }

    // ========== 玩家登录/退出 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.getAscensionRoute() == AscensionRoute.SHAMBHALA) {
            ShambhalaHandler.registerShambhala(player);
            LOGGER.info("[Shambhala] Registered returning Shambhala player: {}", player.getName());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        ShambhalaHandler.cleanupPlayer(event.player.getUniqueID());
        ShambhalaDeathHook.cleanupPlayer(event.player.getUniqueID());
        ItemShambhalaVeil.cleanupPlayer(event.player.getUniqueID());
    }

    // ========== 世界卸载（防止跨存档数据污染） ==========

    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            ShambhalaHandler.clearAllState();
            ShambhalaDeathHook.clearAllState();
            ItemShambhalaVeil.clearAllState();
            LOGGER.info("[Shambhala] Cleared all static state on world unload");
        }
    }

    // ========== 玩家克隆（死亡重生） ==========

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 香巴拉状态通过 IHumanityData 的 copyFrom 保留
    }

    // ========== 香巴拉饰品死亡不掉落 ==========

    /**
     * 玩家死亡时移除香巴拉饰品掉落
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        event.getDrops().removeIf(item -> ShambhalaItems.isShambhalaItem(item.getItem()));
    }

    /**
     * 实体死亡时移除香巴拉饰品掉落（防止通过其他方式掉落）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            event.getDrops().removeIf(item -> ShambhalaItems.isShambhalaItem(item.getItem()));
        }
    }

    /**
     * 阻止玩家丢弃香巴拉饰品
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack tossed = event.getEntityItem().getItem();
        if (ShambhalaItems.isShambhalaItem(tossed)) {
            event.setCanceled(true);
            if (event.getPlayer() != null && !event.getPlayer().world.isRemote) {
                event.getPlayer().sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "⚠ 香巴拉饰品与你的灵魂绑定，无法丢弃。"
                ));
            }
        }
    }

    /**
     * 阻止香巴拉饰品物品实体生成（防止其他方式产生掉落）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemSpawn(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityItem) {
            EntityItem item = (EntityItem) event.getEntity();
            ItemStack stack = item.getItem();
            if (ShambhalaItems.isShambhalaItem(stack)) {
                event.setCanceled(true);
            }
        }
    }
}
