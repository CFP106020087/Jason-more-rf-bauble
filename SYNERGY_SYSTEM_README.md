# Synergy 系统完整文档

## 📚 目录

1. [系统概述](#系统概述)
2. [架构设计](#架构设计)
3. [快速开始](#快速开始)
4. [如何添加新 Synergy](#如何添加新-synergy)
5. [如何移除 Synergy 包](#如何移除-synergy-包)
6. [API 文档](#api-文档)
7. [示例代码](#示例代码)
8. [故障排查](#故障排查)

---

## 系统概述

### 什么是 Synergy 系统？

Synergy 系统是一个**完全解耦、可拔除**的模块联动系统，专门负责"模块之间的协同效应"。

**核心特性**：
- ✅ **完全解耦**：不修改现有模块系统的任何逻辑
- ✅ **可拔除**：删除整个包不会影响游戏运行
- ✅ **高度扩展**：通过 Builder 模式轻松添加新的 Synergy
- ✅ **为 GUI 预留**：内置 ModuleChain 图结构，便于未来拖拽连线

**设计理念**：
- Synergy 系统是"观察者"，只读取模块状态，不干涉原系统
- 通过独立的事件监听器应用额外效果
- 使用桥接模式连接新旧系统

---

## 架构设计

### 包结构

```
com.moremod.synergy/
├── api/               # 公开接口
│   ├── IInstalledModuleView.java      # 模块只读视图接口
│   ├── IModuleProvider.java           # 模块提供者接口
│   ├── ISynergyCondition.java         # Synergy 条件接口
│   └── ISynergyEffect.java            # Synergy 效果接口
│
├── core/              # 核心逻辑
│   ├── SynergyDefinition.java         # Synergy 定义（不可变）
│   ├── SynergyRegistry.java           # Synergy 注册表（单例）
│   ├── SynergyManager.java            # Synergy 管理器（单例）
│   └── ModuleChain.java               # 模块链结构（为 GUI 预留）
│
├── bridge/            # 适配层
│   └── ExistingModuleBridge.java      # 现有模块系统的桥接器
│
├── condition/         # 条件实现
│   ├── ModuleCombinationCondition.java   # 模块组合条件
│   ├── EventTypeCondition.java           # 事件类型条件
│   └── PlayerStateCondition.java         # 玩家状态条件
│
├── effect/            # 效果实现
│   ├── DamageModifierEffect.java      # 伤害修改效果
│   ├── EnergyRefundEffect.java        # 能量退还效果
│   └── ShieldGrantEffect.java         # 护盾授予效果
│
├── event/             # 事件监听
│   └── SynergyEventHandler.java       # Synergy 事件处理器
│
├── builtin/           # 内置 Synergy 规则
│   ├── EnergyLoopSynergy.java         # 能量循环 Synergy
│   ├── CombatEchoSynergy.java         # 战斗回响 Synergy
│   └── SurvivalShieldSynergy.java     # 生存护盾 Synergy
│
└── init/              # 初始化
    └── SynergyBootstrap.java          # Synergy 启动器
```

### 架构图

```
┌─────────────────────────────────────────────────────┐
│               现有模块系统（不变）                    │
│  ItemMechanicalCore + ModuleRegistry + Managers     │
└────────────────────┬────────────────────────────────┘
                     │ (只读访问)
                     ↓
┌─────────────────────────────────────────────────────┐
│              ExistingModuleBridge                    │
│         (唯一的桥接点，实现 IModuleProvider)          │
└────────────────────┬────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────┐
│              SynergyManager                          │
│    (核心调度器，检测条件 + 应用效果)                  │
└────────────────────┬────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ↓                         ↓
┌───────────────┐         ┌───────────────┐
│ SynergyRegistry│        │ SynergyEventHandler│
│  (注册表)      │        │  (事件监听器)  │
└───────────────┘         └───────────────┘
        │                         │
        ↓                         ↓
  SynergyDefinitions          Forge Events
  (内置 + 自定义)            (LivingHurt, Tick等)
```

---

## 快速开始

### 步骤 1：初始化 Synergy 系统

在你的主 mod 类（带有 `@Mod` 注解的类）中，添加初始化调用：

```java
import com.moremod.synergy.init.SynergyBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "moremod", name = "MoreMod", version = "1.0")
public class MoreMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 初始化 Synergy 系统
        SynergyBootstrap.initialize();

        // ...其他初始化代码
    }
}
```

**就这样！Synergy 系统已经启动了。**

### 步骤 2：测试内置 Synergy

游戏中安装以下模块组合，即可体验 Synergy 效果：

#### 能量循环 Synergy
- 所需模块：`ENERGY_EFFICIENCY` + `KINETIC_GENERATOR`
- 效果：消耗能量时，20% 概率退还 50 RF

#### 战斗回响 Synergy
- 所需模块：`DAMAGE_BOOST` + `ATTACK_SPEED`
- 效果：伤害 +25%

#### 生存护盾 Synergy
- 所需模块：`YELLOW_SHIELD` + `HEALTH_REGEN`
- 效果：生命值 < 50% 时，每秒授予 1.0 点护盾（最多 10.0）

---

## 如何添加新 Synergy

### 方法 1：创建新的 Synergy 类（推荐）

在 `com.moremod.synergy.builtin` 包下创建新类：

```java
package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DamageModifierEffect;

public class MyCustomSynergy {

    public static final String ID = "MY_CUSTOM_SYNERGY";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("我的自定义 Synergy")
                .description("模块 A + 模块 B → 特殊效果")

                // 定义所需模块链
                .chain(ModuleChain.linear(
                        "MODULE_A",
                        "MODULE_B"
                ))

                // 添加条件
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "MODULE_A",
                        "MODULE_B"
                ))

                // 添加效果
                .effect(new DamageModifierEffect(1.5f, 0f, true))

                .priority(100)
                .enabled(true)
                .build();
    }
}
```

然后在 `SynergyBootstrap.java` 的 `registerBuiltinSynergies()` 方法中注册：

```java
registry.register(MyCustomSynergy.create());
```

### 方法 2：直接在代码中注册

也可以在任何地方直接注册：

```java
SynergyRegistry.getInstance().register(
    new SynergyDefinition.Builder("QUICK_SYNERGY")
        .requireModules("MODULE_X", "MODULE_Y")
        .condition(new ModuleCombinationCondition("MODULE_X", "MODULE_Y"))
        .effect(new EnergyRefundEffect(100))
        .build()
);
```

### 创建自定义 Condition

```java
public class MyCondition implements ISynergyCondition {

    @Override
    public boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        // 你的自定义逻辑
        return player.getHealth() < 10.0f;
    }

    @Override
    public String getDescription() {
        return "Health<10";
    }
}
```

### 创建自定义 Effect

```java
public class MyEffect implements ISynergyEffect {

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        // 你的自定义效果
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1));
        return true;
    }

    @Override
    public String getDescription() {
        return "Grant Speed II for 5s";
    }
}
```

---

## 如何移除 Synergy 包

如果未来你想完全移除 Synergy 系统，只需以下步骤：

### 步骤 1：删除初始化调用

在主 mod 类中，注释掉或删除这行：

```java
// SynergyBootstrap.initialize(); // ← 注释掉或删除
```

### 步骤 2：删除整个包

删除 `src/main/java/com/moremod/synergy/` 整个目录。

### 步骤 3：删除文档

删除 `SYNERGY_SYSTEM_README.md`（可选）。

### 完成！

游戏将正常运行，只是失去 Synergy 功能。现有模块系统不受任何影响。

**重要提示**：
- Synergy 系统没有存储任何数据到 NBT，移除后不会留下"垃圾数据"
- 没有修改任何现有模块的逻辑，移除后现有功能完全不变

---

## API 文档

### 核心接口

#### ISynergyCondition
```java
public interface ISynergyCondition {
    boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event);
    default String getDescription() { ... }
}
```

#### ISynergyEffect
```java
public interface ISynergyEffect {
    boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event);
    default String getDescription() { ... }
    default int getPriority() { return 100; }
}
```

### 内置 Condition

| 类名 | 说明 | 示例 |
|-----|------|------|
| `ModuleCombinationCondition` | 检查模块组合 | `new ModuleCombinationCondition("A", "B")` |
| `EventTypeCondition` | 检查事件类型 | `new EventTypeCondition(LivingHurtEvent.class)` |
| `PlayerStateCondition` | 检查玩家状态 | `PlayerStateCondition.healthBelow(0.5f)` |

### 内置 Effect

| 类名 | 说明 | 示例 |
|-----|------|------|
| `DamageModifierEffect` | 修改伤害 | `new DamageModifierEffect(1.5f, 2.0f, true)` |
| `EnergyRefundEffect` | 退还能量 | `new EnergyRefundEffect(100, 0.2f, true)` |
| `ShieldGrantEffect` | 授予护盾 | `new ShieldGrantEffect(5.0f, false, 20.0f, true)` |

---

## 示例代码

### 示例 1：复杂组合 Synergy

```java
public static SynergyDefinition create() {
    return new SynergyDefinition.Builder("TRIPLE_MODULE_SYNERGY")
        .displayName("三重联动")
        .description("A + B + C → 超级效果")

        // 所需 3 个模块
        .requireModules("MODULE_A", "MODULE_B", "MODULE_C")

        // 条件1：所有模块都激活
        .condition(new ModuleCombinationCondition(
            true, // 要求激活
            3,    // 总等级 >= 3
            "MODULE_A", "MODULE_B", "MODULE_C"
        ))

        // 条件2：只在受伤事件中触发
        .condition(new EventTypeCondition(LivingHurtEvent.class))

        // 条件3：生命值低于 30%
        .condition(PlayerStateCondition.healthBelow(0.3f))

        // 效果1：伤害 x2
        .effect(new DamageModifierEffect(2.0f, 0f, true))

        // 效果2：退还 200 RF
        .effect(new EnergyRefundEffect(200, 0.5f, true))

        // 效果3：授予 10 点护盾
        .effect(new ShieldGrantEffect(10.0f))

        .priority(50)
        .build();
}
```

### 示例 2：自定义 Condition + Effect

```java
// 自定义条件：玩家在水中
public class InWaterCondition implements ISynergyCondition {
    @Override
    public boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        return player.isInWater();
    }
}

// 自定义效果：授予水下呼吸
public class WaterBreathingEffect implements ISynergyEffect {
    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        player.setAir(300);
        return true;
    }
}

// 组合使用
SynergyDefinition waterSynergy = new SynergyDefinition.Builder("WATER_MASTER")
    .requireModules("WATERPROOF_MODULE", "SPEED_MODULE")
    .condition(new InWaterCondition())
    .effect(new WaterBreathingEffect())
    .build();
```

---

## 故障排查

### 问题 1：Synergy 没有触发

**检查清单**：
1. 确认 `SynergyBootstrap.initialize()` 已被调用
2. 确认所需模块都已安装且激活（能量充足）
3. 检查条件是否满足（如事件类型、玩家状态等）
4. 启用调试模式：`-Dsynergy.debug=true`

### 问题 2：编译错误

**可能原因**：
- 忘记在某处导入 Synergy 相关类
- Synergy 包未正确放置在 `com.moremod.synergy/` 下

### 问题 3：游戏崩溃

**检查日志**：
- 查找 `[SynergyBootstrap]` 或 `[SynergyManager]` 的错误信息
- 检查 Condition 或 Effect 中是否有空指针异常

### 启用调试模式

在 JVM 参数中添加：
```
-Dsynergy.debug=true
```

或在代码中设置：
```java
System.setProperty("synergy.debug", "true");
```

---

## 未来扩展：拖拽式 GUI

Synergy 系统已经为 GUI 预留了数据结构：`ModuleChain`。

**未来 GUI 设计思路**：
1. 玩家在 GUI 中拖动模块图标到画布
2. 用连线连接模块（形成有向图）
3. 保存为 `ModuleChain` 对象
4. 转换为 `SynergyDefinition` 并注册

**示例代码（未来实现）**：
```java
// GUI 中构建链
ModuleChain chain = new ModuleChain.Builder()
    .addRoot("MODULE_A")
    .addEdge("MODULE_A", "MODULE_B")
    .addEdge("MODULE_B", "MODULE_C")
    .addEdge("MODULE_A", "MODULE_D")
    .build();

// 创建 Synergy
SynergyDefinition customSynergy = new SynergyDefinition.Builder("PLAYER_CUSTOM")
    .chain(chain)
    .effect(new DamageModifierEffect(1.5f))
    .build();

// 注册
SynergyRegistry.getInstance().register(customSynergy);
```

---

## 总结

Synergy 系统是一个**完全解耦、高度扩展、为未来预留**的模块联动系统。

**核心优势**：
- ✅ 不破坏现有系统
- ✅ 随时可以移除
- ✅ 易于扩展新 Synergy
- ✅ 为 GUI 预留了完整的数据结构

**如有问题，欢迎查阅本文档或检查代码注释。**
