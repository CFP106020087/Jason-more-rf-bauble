package com.moremod.module.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                     模块事件处理器接口 (IModuleEventHandler)                   ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  实现此接口以定义模块的完整行为逻辑。                                            ║
 * ║  所有方法都有默认空实现，只需覆盖你需要的方法。                                    ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              可用事件钩子                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  【Tick 事件】                                                                ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • onTick(ctx)              - 每tick调用 (20次/秒)                            ║
 * ║  • onSecondTick(ctx)        - 每秒调用 (1次/秒)                               ║
 * ║  • getTickInterval()        - 自定义tick间隔 (返回0使用默认)                    ║
 * ║                                                                              ║
 * ║  【战斗事件 - 玩家攻击】                                                        ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • onPlayerAttack(ctx, target, damage)     - 玩家攻击时 (可修改伤害)           ║
 * ║  • onPlayerHitEntity(ctx, target, event)   - 玩家命中实体时                    ║
 * ║  • onPlayerKillEntity(ctx, target, event)  - 玩家击杀实体时                    ║
 * ║                                                                              ║
 * ║  【战斗事件 - 玩家受伤】                                                        ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • onPlayerHurt(ctx, source, damage)       - 玩家受伤时 (可修改伤害)           ║
 * ║  • onPlayerDeath(ctx, source)              - 玩家死亡时                        ║
 * ║                                                                              ║
 * ║  【交互事件】                                                                  ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • onRightClickBlock(ctx, event)           - 右键方块时                        ║
 * ║  • onRightClickItem(ctx, event)            - 右键物品时                        ║
 * ║  • onLeftClickBlock(ctx, event)            - 左键方块时                        ║
 * ║                                                                              ║
 * ║  【状态事件】                                                                  ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • onModuleActivated(ctx)                  - 模块激活时                        ║
 * ║  • onModuleDeactivated(ctx)                - 模块关闭时                        ║
 * ║  • onLevelChanged(ctx, oldLevel, newLevel) - 等级变化时                        ║
 * ║                                                                              ║
 * ║  【能量事件】                                                                  ║
 * ║  ─────────────────────────────────────────────────                           ║
 * ║  • getPassiveEnergyCost()                  - 被动能耗 (RF/tick)               ║
 * ║  • onEnergyDepleted(ctx)                   - 能量耗尽时                        ║
 * ║  • onEnergyRestored(ctx)                   - 能量恢复时                        ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              EventContext 说明                                ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  EventContext 包含以下信息:                                                    ║
 * ║  • player      - EntityPlayer 玩家实例                                        ║
 * ║  • coreStack   - ItemStack 机械核心物品                                        ║
 * ║  • moduleId    - String 模块ID (如 "MAGIC_ABSORB")                            ║
 * ║  • level       - int 当前模块等级 (1-maxLevel)                                 ║
 * ║  • worldTime   - long 当前世界时间 (tick)                                      ║
 * ║                                                                              ║
 * ║  辅助方法:                                                                    ║
 * ║  • ctx.consumeEnergy(amount)    - 消耗能量，返回是否成功                        ║
 * ║  • ctx.getEnergy()              - 获取当前能量                                 ║
 * ║  • ctx.getMaxEnergy()           - 获取最大能量                                 ║
 * ║  • ctx.setNBT(key, value)       - 设置模块专用NBT数据                          ║
 * ║  • ctx.getNBT(key)              - 获取模块专用NBT数据                          ║
 * ║  • ctx.getCooldown(key)         - 获取冷却剩余时间                             ║
 * ║  • ctx.setCooldown(key, ticks)  - 设置冷却时间                                 ║
 * ║  • ctx.isOnCooldown(key)        - 检查是否在冷却中                             ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              完整示例                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  // 示例：魔力吸收模块                                                         ║
 * ║  public class MagicAbsorbHandler implements IModuleEventHandler {            ║
 * ║                                                                              ║
 * ║      @Override                                                               ║
 * ║      public float onPlayerHurt(EventContext ctx, DamageSource src,           ║
 * ║                                 float damage) {                              ║
 * ║          // 只处理魔法伤害                                                     ║
 * ║          if (!src.isMagicDamage()) return damage;                            ║
 * ║                                                                              ║
 * ║          // 根据等级计算吸收率                                                  ║
 * ║          float absorbRate = 0.1f + (ctx.level * 0.1f);                       ║
 * ║          float absorbed = damage * absorbRate;                               ║
 * ║                                                                              ║
 * ║          // 累积余灼                                                          ║
 * ║          float ember = ctx.getNBTFloat("ember") + absorbed;                  ║
 * ║          ctx.setNBT("ember", ember);                                         ║
 * ║                                                                              ║
 * ║          // Lv3: 余灼满载触发爆心                                              ║
 * ║          if (ctx.level >= 3 && ember >= 50f) {                               ║
 * ║              triggerMagicBurst(ctx);                                         ║
 * ║              ctx.setNBT("ember", 0f);                                        ║
 * ║          }                                                                   ║
 * ║                                                                              ║
 * ║          return damage - absorbed;                                           ║
 * ║      }                                                                       ║
 * ║                                                                              ║
 * ║      @Override                                                               ║
 * ║      public int getPassiveEnergyCost() {                                     ║
 * ║          return 5; // 5 RF/tick                                              ║
 * ║      }                                                                       ║
 * ║  }                                                                           ║
 * ║                                                                              ║
 * ║  // 注册:                                                                     ║
 * ║  ModuleDefinition.builder("MAGIC_ABSORB")                                    ║
 * ║      .displayName("魔力吸收")                                                  ║
 * ║      .handler(new MagicAbsorbHandler())                                      ║
 * ║      .register();                                                            ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
public interface IModuleEventHandler {

    // ==================== Tick 事件 ====================

    /**
     * 每tick调用 (每秒20次)
     * 用于持续性效果、状态检查等
     */
    default void onTick(EventContext ctx) {}

    /**
     * 每秒调用 (每秒1次)
     * 用于周期性效果，如恢复、消耗等
     */
    default void onSecondTick(EventContext ctx) {}

    /**
     * 自定义tick间隔 (单位: tick)
     * 返回 0 表示使用默认 (每tick调用 onTick)
     * 返回 >0 表示每N tick调用一次 onTick
     */
    default int getTickInterval() { return 0; }

    // ==================== 战斗事件 - 玩家攻击 ====================

    /**
     * 玩家攻击实体时调用
     * @param ctx 事件上下文
     * @param target 被攻击的实体
     * @param damage 原始伤害值
     * @return 修改后的伤害值
     */
    default float onPlayerAttack(EventContext ctx, EntityLivingBase target, float damage) {
        return damage;
    }

    /**
     * 玩家命中实体时调用 (LivingHurtEvent)
     * 可以访问完整的事件对象进行更复杂的操作
     */
    default void onPlayerHitEntity(EventContext ctx, EntityLivingBase target, LivingHurtEvent event) {}

    /**
     * 玩家击杀实体时调用
     */
    default void onPlayerKillEntity(EventContext ctx, EntityLivingBase target, LivingDeathEvent event) {}

    // ==================== 战斗事件 - 玩家受伤 ====================

    /**
     * 玩家受到伤害时调用
     * @param ctx 事件上下文
     * @param source 伤害来源
     * @param damage 原始伤害值
     * @return 修改后的伤害值
     */
    default float onPlayerHurt(EventContext ctx, DamageSource source, float damage) {
        return damage;
    }

    /**
     * 玩家受到攻击时调用 (LivingAttackEvent, 在伤害计算之前)
     * 返回 true 取消此次攻击
     */
    default boolean onPlayerAttacked(EventContext ctx, DamageSource source, float damage) {
        return false;
    }

    /**
     * 玩家死亡时调用
     */
    default void onPlayerDeath(EventContext ctx, DamageSource source) {}

    // ==================== 交互事件 ====================

    /**
     * 玩家右键方块时调用
     */
    default void onRightClickBlock(EventContext ctx, PlayerInteractEvent.RightClickBlock event) {}

    /**
     * 玩家右键使用物品时调用
     */
    default void onRightClickItem(EventContext ctx, PlayerInteractEvent.RightClickItem event) {}

    /**
     * 玩家左键方块时调用
     */
    default void onLeftClickBlock(EventContext ctx, PlayerInteractEvent.LeftClickBlock event) {}

    // ==================== 状态事件 ====================

    /**
     * 模块被激活时调用 (玩家装备核心或开启模块)
     */
    default void onModuleActivated(EventContext ctx) {}

    /**
     * 模块被关闭时调用 (玩家卸下核心或关闭模块)
     */
    default void onModuleDeactivated(EventContext ctx) {}

    /**
     * 模块等级变化时调用
     */
    default void onLevelChanged(EventContext ctx, int oldLevel, int newLevel) {}

    // ==================== 能量事件 ====================

    /**
     * 获取模块的被动能耗 (RF/tick)
     * 返回 0 表示无被动消耗
     */
    default int getPassiveEnergyCost() { return 0; }

    /**
     * 能量耗尽时调用
     */
    default void onEnergyDepleted(EventContext ctx) {}

    /**
     * 能量从耗尽恢复时调用
     */
    default void onEnergyRestored(EventContext ctx) {}

    // ==================== 工具方法 ====================

    /**
     * 检查此处理器是否处理指定的伤害类型
     * 用于优化，避免不必要的调用
     */
    default boolean handlesDamageType(String damageType) { return true; }

    /**
     * 获取处理器描述 (用于调试)
     */
    default String getDescription() { return getClass().getSimpleName(); }
}
