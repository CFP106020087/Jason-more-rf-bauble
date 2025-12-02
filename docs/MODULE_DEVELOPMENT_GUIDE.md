# 模块开发指南 (Module Development Guide)

本文档记录了两种模块开发方式的流程和区别。

---

## 目录

1. [旧系统 - 独立事件处理器](#旧系统---独立事件处理器)
2. [新系统 - 自动注册系统](#新系统---自动注册系统)
3. [对比与选择建议](#对比与选择建议)

---

## 旧系统 - 独立事件处理器

### 概述

使用 `@Mod.EventBusSubscriber` 注解创建独立的事件处理类，直接订阅 Forge 事件。

### 适用场景

- 需要精细控制事件优先级
- 需要处理复杂的事件逻辑
- 不需要与模块等级系统深度整合

### 文件位置

`src/main/java/com/moremod/event/`

### 开发流程

#### 1. 创建事件处理器类

```java
package com.moremod.event;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 远程伤害增幅处理器
 * 独立事件处理器，不使用自动注册系统
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RangedDamageHandler {

    private static final String MODULE_ID = "RANGED_DAMAGE_BOOST";
    private static final float[] DAMAGE_MULTIPLIERS = {0f, 0.15f, 0.30f, 0.50f};

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 1. 检查条件
        if (!isRangedDamage(source)) return;
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        if (player.world.isRemote) return;

        // 2. 检查机械核心
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (coreStack.isEmpty()) return;

        // 3. 检查模块状态
        if (!ItemMechanicalCore.isUpgradeActive(coreStack, MODULE_ID)) return;

        // 4. 获取模块等级
        int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, MODULE_ID);
        if (level <= 0) return;

        // 5. 应用效果
        float multiplier = getMultiplier(level);
        float newDamage = event.getAmount() * (1.0f + multiplier);
        event.setAmount(newDamage);
    }

    private static boolean isRangedDamage(DamageSource source) {
        if (source.isProjectile()) return true;
        String type = source.getDamageType();
        return type != null && (type.equals("arrow") || type.equals("thrown"));
    }

    private static float getMultiplier(int level) {
        if (level < 0 || level >= DAMAGE_MULTIPLIERS.length) {
            return DAMAGE_MULTIPLIERS[DAMAGE_MULTIPLIERS.length - 1];
        }
        return DAMAGE_MULTIPLIERS[level];
    }
}
```

#### 2. 关键 API

| 方法 | 说明 |
|------|------|
| `ItemMechanicalCore.findEquippedMechanicalCore(player)` | 获取玩家装备的机械核心 |
| `ItemMechanicalCore.isUpgradeActive(stack, moduleId)` | 检查模块是否激活 |
| `ItemMechanicalCoreExtended.getUpgradeLevel(stack, moduleId)` | 获取模块等级 |

#### 3. 注意事项

- 方法必须是 `static`
- 需要手动处理服务端/客户端判断 (`world.isRemote`)
- 需要手动检查机械核心和模块状态
- 无自动能量消耗管理

---

## 新系统 - 自动注册系统

### 概述

使用 `ModuleDefinition` + `IModuleEventHandler` 接口，通过 `ModuleAutoRegistry` 自动注册。系统会自动处理模块状态检查、能量消耗、事件分发等。

### 适用场景

- 标准模块效果（属性加成、药水效果等）
- 需要自动能量管理
- 需要模块专用 NBT 存储
- 需要冷却时间管理

### 文件位置

- 定义: `src/main/java/com/moremod/module/ModuleDefinition.java`
- 接口: `src/main/java/com/moremod/module/effect/IModuleEventHandler.java`
- 处理器: `src/main/java/com/moremod/module/handler/`

### 开发流程

#### 方式一：简单效果 (使用 `.effects()`)

适用于属性修改、药水效果等标准效果。

```java
ModuleDefinition.builder("SPEED_BOOST")
    .displayName("速度提升")
    .category(Category.SURVIVAL)
    .color(TextFormatting.GREEN)
    .maxLevel(3)
    .effects(
        ModuleEffect.attribute(SharedMonsterAttributes.MOVEMENT_SPEED)
            .baseValue(0.2).perLevel(0.2).build(),
        ModuleEffect.potion(MobEffects.SPEED)
            .amplifierPerLevel(1).build()
    )
    .register();
```

#### 方式二：完整事件处理器 (使用 `.handler()`)

适用于复杂逻辑，如连锁挖掘、伤害转换等。

##### 步骤 1: 实现 IModuleEventHandler 接口

```java
package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

public class AreaMiningBoostHandler implements IModuleEventHandler {

    private static final int[] MAX_BLOCKS = {0, 8, 16, 32};
    private static final int ENERGY_PER_BLOCK = 50;

    @Override
    public void onBlockBreak(EventContext ctx, BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        if (event.getWorld().isRemote) return;

        int maxBlocks = MAX_BLOCKS[Math.min(ctx.level, MAX_BLOCKS.length - 1)];

        // 使用 ctx 提供的辅助方法
        if (!ctx.consumeEnergy(ENERGY_PER_BLOCK)) return;

        // 执行连锁挖掘逻辑...
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 主动消耗，无被动消耗
    }

    @Override
    public String getDescription() {
        return "范围挖掘 - 连锁挖掘相邻同类型方块";
    }
}
```

##### 步骤 2: 注册模块

```java
ModuleDefinition.builder("AREA_MINING_BOOST")
    .displayName("范围挖掘")
    .category(Category.AUXILIARY)
    .color(TextFormatting.AQUA)
    .maxLevel(3)
    .handler(new AreaMiningBoostHandler())
    .descriptions(
        "连锁挖掘相邻的同类型方块",
        "Lv1: 最多 8 个方块",
        "Lv2: 最多 16 个方块",
        "Lv3: 最多 32 个方块"
    )
    .register();
```

#### EventContext 常用方法

| 方法 | 说明 |
|------|------|
| `ctx.player` | 玩家实例 |
| `ctx.coreStack` | 机械核心 ItemStack |
| `ctx.level` | 当前模块等级 |
| `ctx.consumeEnergy(amount)` | 消耗能量，返回是否成功 |
| `ctx.getEnergy()` | 获取当前能量 |
| `ctx.setNBT(key, value)` | 设置模块专用 NBT |
| `ctx.getNBTFloat(key)` | 获取 NBT float 值 |
| `ctx.setCooldown(key, ticks)` | 设置冷却时间 |
| `ctx.isOnCooldown(key)` | 检查是否在冷却中 |

#### 可用事件钩子

| 类别 | 方法 |
|------|------|
| **Tick** | `onTick`, `onSecondTick`, `getTickInterval` |
| **攻击** | `onPlayerAttack`, `onPlayerHitEntity`, `onPlayerKillEntity` |
| **受伤** | `onPlayerHurt`, `onPlayerAttacked`, `onPlayerDeath` |
| **交互** | `onRightClickBlock`, `onRightClickItem`, `onLeftClickBlock` |
| **方块** | `onBlockBreak` |
| **状态** | `onModuleActivated`, `onModuleDeactivated`, `onLevelChanged` |
| **能量** | `getPassiveEnergyCost`, `onEnergyDepleted`, `onEnergyRestored` |

---

## 对比与选择建议

| 特性 | 旧系统 (独立处理器) | 新系统 (自动注册) |
|------|---------------------|-------------------|
| **复杂度** | 低 | 中 |
| **自动模块检查** | ❌ 手动 | ✅ 自动 |
| **自动能量管理** | ❌ 手动 | ✅ 自动 |
| **模块 NBT 存储** | ❌ 手动 | ✅ 内置 |
| **冷却时间管理** | ❌ 手动 | ✅ 内置 |
| **事件优先级控制** | ✅ 完全控制 | ⚠️ 有限 |
| **适合场景** | 特殊事件处理 | 标准模块效果 |

### 选择建议

- **使用旧系统**：当你需要精细控制事件优先级，或者模块逻辑非常特殊时
- **使用新系统**：当你开发标准模块效果，希望系统自动处理状态检查和能量管理时

---

## 文件结构

```
src/main/java/com/moremod/
├── event/                          # 旧系统 - 独立事件处理器
│   ├── RangedDamageHandler.java
│   └── ...
├── module/
│   ├── ModuleDefinition.java       # 模块定义类
│   ├── ModuleAutoRegistry.java     # 自动注册器
│   ├── effect/
│   │   ├── IModuleEventHandler.java  # 事件处理器接口
│   │   ├── EventContext.java         # 事件上下文
│   │   ├── ModuleEffect.java         # 效果定义
│   │   └── AutoEffectHandler.java    # 自动效果分发器
│   └── handler/                    # 新系统 - Handler 实现
│       ├── AreaMiningBoostHandler.java
│       └── ...
```

---

*最后更新: 2025-12-02*
