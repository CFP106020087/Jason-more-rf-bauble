# 事件处理逻辑分析报告

## 📋 概述

本报告分析了四个 Manager 中的所有事件处理逻辑，为迁移到新模块系统提供指导。

---

## 🎯 事件处理汇总

### 全局事件列表

| 事件类型 | Manager | 用途 | 优先级 |
|---------|---------|------|--------|
| **TickEvent.PlayerTickEvent** | All | 主更新循环 | NORMAL |
| **LivingHurtEvent** | Survival, Combat | 伤害处理 | NORMAL / LOWEST |
| **LivingAttackEvent** | Survival | 伤害前处理 | NORMAL |
| **AttackEntityEvent** | Combat | 攻击事件 | NORMAL |
| **BlockEvent.BreakEvent** | Auxiliary, Energy | 挖掘处理 | NORMAL |
| **BlockEvent.PlaceEvent** | Auxiliary | 放置矿物检测 | NORMAL |
| **LivingDeathEvent** | Auxiliary, Energy | 击杀事件 | HIGH |
| **PlayerPickupXpEvent** | Auxiliary | 拾取经验 | HIGH |
| **RenderWorldLastEvent** | Auxiliary | 客户端渲染 | CLIENT |
| **TickEvent.WorldTickEvent** | Auxiliary | 世界 tick | NORMAL |

---

## 📦 Manager 1: SurvivalUpgradeManager (744行)

### 事件处理器

#### 1. `onPlayerTick(TickEvent.PlayerTickEvent)`

**触发频率**: 每 tick (服务端)

**功能**:
- 根据能量状态调用不同的更新策略
- CRITICAL 模式：仅生命恢复 + 自动灭火
- EMERGENCY 模式：护盾 + 生命恢复 + 自动灭火
- 正常模式：所有系统

**调用流程**:
```java
onPlayerTick()
├─ getEnergyStatus(coreStack)
├─ if (CRITICAL)
│  ├─ HealthRegenSystem.applyRegeneration()
│  └─ FireExtinguishSystem.checkAndExtinguish()
├─ if (EMERGENCY)
│  ├─ YellowShieldSystem.updateShield()
│  ├─ HealthRegenSystem.applyRegeneration()
│  └─ FireExtinguishSystem.checkAndExtinguish()
└─ else (NORMAL / POWER_SAVING)
   ├─ YellowShieldSystem.updateShield()
   ├─ HealthRegenSystem.applyRegeneration()
   ├─ HungerThirstSystem.manageFoodStats()
   └─ FireExtinguishSystem.checkAndExtinguish()
```

#### 2. `onPlayerHurt(LivingHurtEvent)`

**触发时机**: 玩家受到伤害后

**功能**:
- 护盾耗尽检测（延迟检查，因为吸收心在伤害后更新）
- 反伤处理

**调用流程**:
```java
onPlayerHurt(event)
├─ if (damage > 0)
│  └─ MinecraftServer.addScheduledTask(() -> {
│     if (absorptionAmount <= 0)
│        YellowShieldSystem.onShieldDepleted(player)
│  })
└─ if (attacker instanceof EntityLivingBase)
   └─ ThornsSystem.applyThorns(player, attacker, damage, thornsLevel)
```

### 子系统详解

#### YellowShieldSystem (护盾系统)

**核心逻辑**:
- 每秒恢复 0.5 点护盾（最大 level * 7.0）
- 护盾维持消耗：10 * level RF/s
- 护盾恢复消耗：5 RF/次
- 护盾破碎后30秒冷却

**NBT 数据**:
```
MechanicalCoreShieldCooldown - 冷却结束时间
MechanicalCoreShieldLastUpdate - 上次更新时间
MechanicalCoreShieldActive - 护盾激活状态
MechanicalCoreShieldEnergyCheck - 能量检查时间
```

#### HealthRegenSystem (生命恢复)

**核心逻辑**:
- 恢复间隔：80 - level * 20 ticks
- 恢复量：0.5 * level 心
- 能量消耗：15 * level RF/次

**NBT 数据**:
```
MechanicalCoreLastHeal - 上次恢复时间
MechanicalCoreRegenActive - 系统激活状态
```

#### HungerThirstSystem (饥饿/口渴管理)

**核心逻辑**:
- 饥饿恢复间隔：(160 - level * 40) * 20 ticks
- 饥饿恢复量：level 点 + 0.5 * level 饱和度
- **SimpleDifficulty 集成**（口渴系统）：
  - LV1: 每 60 ticks，维持 18+ 水分
  - LV2: 每 40 ticks，维持 19+ 水分，清零消耗
  - LV3: 每 20 ticks，始终满值 20，完全免疫

**反射方法**:
```java
temperatureCapability = SDCapabilities.THIRST
getThirstLevelMethod
addThirstLevelMethod
setThirstLevelMethod
getThirstSaturationMethod
addThirstSaturationMethod
setThirstSaturationMethod
setThirstExhaustionMethod
isThirstyMethod
```

#### ThornsSystem (反伤)

**核心逻辑**:
- 反伤比例：0.15F * level (15%/30%/45%)
- 无能量消耗（被动系统）
- 使用 `DamageSource.causeThornsDamage()`

#### FireExtinguishSystem (自动灭火)

**核心逻辑**:
- 冷却时间：80 - level * 20 ticks
- 能量消耗：50 RF/次
- 只在燃烧时激活

---

## 📦 Manager 2: CombatUpgradeManager (561行)

### 事件处理器

#### 1. `onPlayerTick(TickEvent.PlayerTickEvent)`

**触发频率**: 每 tick (服务端)

**功能**:
- 根据能量状态应用战斗加成
- CRITICAL 模式：移除所有战斗加成
- EMERGENCY 模式：保留攻击速度，移除范围扩展
- 正常模式：所有战斗加成

**调用流程**:
```java
onPlayerTick()
├─ getEnergyStatus(coreStack)
├─ if (CRITICAL)
│  ├─ AttackSpeedSystem.removeAttackSpeed()
│  ├─ RangeExtensionSystem.removeReachExtension()
│  └─ 清除追击标记
├─ if (EMERGENCY)
│  ├─ AttackSpeedSystem.applyAttackSpeed()
│  └─ RangeExtensionSystem.removeReachExtension()
└─ else
   ├─ AttackSpeedSystem.applyAttackSpeed()
   └─ RangeExtensionSystem.applyReachExtension()
```

#### 2. `onAttack(AttackEntityEvent)`

**触发时机**: 玩家攻击实体时

**功能**:
- 连击检测
- 追击标记
- 追击冲刺（潜行时）

**调用流程**:
```java
onAttack(event)
├─ AttackSpeedSystem.checkCombo()
└─ if (target instanceof EntityLivingBase)
   ├─ PursuitSystem.markTarget()
   └─ if (player.isSneaking())
      └─ PursuitSystem.dashToTarget()
```

#### 3. `onLivingHurtLowest(LivingHurtEvent)` **[LOWEST 优先级]**

**触发时机**: 所有 mod 处理完伤害后

**功能**:
- 应用最终伤害加成（倍率 + 暴击 + 追击）

**调用流程**:
```java
onLivingHurtLowest(event)
├─ DamageBoostSystem.getDamageMultiplier() → damage *= multiplier
├─ DamageBoostSystem.applyCritical() → damage *= 2.0 (暴击)
├─ PursuitSystem.getPursuitDamage() → damage *= (1 + pursuitBonus)
└─ event.setAmount(damage)
```

### 子系统详解

#### DamageBoostSystem (伤害提升)

**核心逻辑**:
- 伤害倍率：1.0 + (0.25 * level) (最高 2.5x)
- 每次攻击消耗：20 * level RF
- 暴击几率：0.1 * level (10%-50%)
- 暴击倍率：2x
- 暴击额外消耗：10 RF

#### AttackSpeedSystem (攻击速度)

**核心逻辑**:
- 攻速加成：0.2 * level (20%/40%/60%)
- 使用 AttributeModifier (MULTIPLY_TOTAL)
- UUID: `d8499b04-2222-4726-ab29-64469d734e0d`
- 连击系统：40 tick 连击窗口，减少疲劳

#### RangeExtensionSystem (攻击范围)

**核心逻辑**:
- 触及距离：+3.0 * level 格
- 使用 REACH_DISTANCE 属性
- UUID: `d8499b04-3333-4726-ab29-64469d734e0d`
- 可视化指示器（潜行时显示范围）

#### PursuitSystem (追击系统)

**核心逻辑**:
- 标记目标消耗：5 RF/次
- 追击层数：最大 level * 2
- 每层伤害加成：10%
- 追击过期时间：20 ticks (1秒)
- 冲刺消耗：50 RF

**NBT 数据**:
```
MechanicalCorePursuitTarget - 目标 UUID
MechanicalCorePursuitStacks - 追击层数
MechanicalCoreLastPursuit - 上次追击时间
```

---

## 📦 Manager 3: AuxiliaryUpgradeManager (1107行)

### 事件处理器

#### 1. `onPlayerTick(TickEvent.PlayerTickEvent)`

**触发频率**: 每 tick

**功能**:
- 根据能量百分比管理辅助系统
- < 3%: 关闭所有系统
- < 5%: 仅保留移动速度
- < 15%: 移动速度，禁用透视/隐身
- >= 15%: 所有系统正常

**调用流程**:
```java
onPlayerTick()
├─ getEnergyPercent()
├─ if (< 0.03f)
│  ├─ MovementSpeedSystem.resetSpeed()
│  ├─ StealthSystem.disableStealth()
│  └─ OreVisionSystem.reset()
├─ if (< 0.05f)
│  ├─ MovementSpeedSystem.updateSpeed()
│  ├─ StealthSystem.disableStealth()
│  └─ OreVisionSystem.reset()
├─ if (< 0.15f)
│  ├─ MovementSpeedSystem.updateSpeed()
│  ├─ StealthSystem.disableStealth()
│  └─ OreVisionSystem.reset()
└─ else
   ├─ MovementSpeedSystem.updateSpeed()
   ├─ StealthSystem.updateStealth()
   └─ OreVisionSystem.updateScan()
```

#### 2. `onBlockPlace(BlockEvent.PlaceEvent)` - OreVisionSystem

**触发时机**: 玩家放置方块

**功能**: 将玩家放置的矿物添加到透视缓存

#### 3. `onBlockBreak(BlockEvent.BreakEvent)` - OreVisionSystem

**触发时机**: 方块被破坏

**功能**: 从透视缓存中移除

#### 4. `@SideOnly(CLIENT) onRenderWorldLast(RenderWorldLastEvent)`

**触发时机**: 客户端渲染最后阶段

**功能**: 渲染矿物高亮边框

**渲染逻辑**:
```java
onRenderWorldLast()
├─ if (!renderingOres || oreCache.isEmpty()) return
├─ 收集玩家视野内的矿物（最大 MAX_RENDER_DISTANCE）
├─ 按距离排序
├─ for (ore in oreCache) [最多 MAX_ORES_TO_RENDER]
│  ├─ setColorForOre(ore) - 设置颜色
│  ├─ 计算透明度（距离越远越透明）
│  └─ RenderGlobal.drawSelectionBoundingBox()
```

#### 5. `onEntityDeath(LivingDeathEvent)` **[HIGH 优先级]** - ExpAmplifierSystem

**触发时机**: 实体死亡

**功能**:
- 生成额外经验球
- 连杀系统
- 防重复处理

**逻辑**:
```java
onEntityDeath()
├─ 检查是否重复处理 (processingEntities)
├─ 计算基础经验值 computeBaseExperience()
├─ 能量消耗：max(10, baseExp * 3) RF
├─ 连杀系统：
│  ├─ 检查上次击杀时间（5秒内）
│  ├─ 连杀倍率：combo * 0.1
│  └─ 最大连杀：x10
├─ totalMultiplier = baseMultiplier + comboBonus
├─ bonusExp = baseExp * (totalMultiplier - 1.0)
└─ spawnBonusExperience() - 生成经验球
```

#### 6. `onPlayerPickupXp(PlayerPickupXpEvent)` **[HIGH 优先级]** - ExpAmplifierSystem

**触发时机**: 玩家拾取经验球

**功能**: 增幅经验值（跳过 BONUS_ORB_TAG 标记的球）

**逻辑**:
```java
onPlayerPickupXp()
├─ if (orb.hasTag(BONUS_ORB_TAG)) return - 跳过奖励球
├─ 能量消耗：max(5, orbValue * 2) RF
├─ multiplier = 1.0 + (0.5 * level)
├─ orb.xpValue = (int)(original * multiplier) - 直接修改经验球
└─ 显示提示（每秒一次，防刷屏）
```

#### 7. `onWorldTick(TickEvent.WorldTickEvent)` - ExpAmplifierSystem

**触发频率**: 每 200 ticks

**功能**: 清理过期的连杀数据

### 子系统详解

#### OreVisionSystem (矿物透视)

**核心逻辑**:
- 扫描范围：8 * level 格
- 完整扫描间隔：5000ms
- 快速扫描间隔：10 ticks
- 能量消耗：50 + (level * 10) RF/s
- 最大渲染距离：48 格
- 最大渲染数量：500 个矿物

**扫描策略**:
1. 完整扫描（Full Scan）：
   - 扫描所有区块（chunkRange = (range >> 4) + 1）
   - 检查范围内所有 Y 层
   - 更新 oreCache 和 DISCOVERED_ORE_TYPES
2. 快速扫描（Quick Scan）：
   - 仅扫描玩家周围 16 格立方体
   - 补充遗漏的矿物

**矿物识别**:
- 原版矿物：硬编码
- OreDictionary：扫描 "ore*" 前缀
- 注册表扫描：检查方块注册名包含 "ore"

**客户端渲染**:
- 使用 `RenderGlobal.drawSelectionBoundingBox()`
- 颜色映射：钻石=青色，绿宝石=绿色，金=黄色等
- 透明度随距离衰减
- 支持矿物类型过滤（cycleOreCategory）

#### MovementSpeedSystem (移动速度)

**核心逻辑**:
- 速度加成：0.2 * level (20%/40%/60%)
- 使用 MOVEMENT_SPEED 属性
- UUID: `d8499b04-0e66-4726-ab29-64469d734e0d`
- 能量消耗：8 * level RF/s

#### StealthSystem (隐身系统)

**核心逻辑**:
- 持续时间：30s / 45s / 60s (level 1/2/3)
- 冷却时间：20s / 30s / 45s
- 连续使用惩罚：cooldown *= 1.5^uses
- 能量消耗：(50 - level * 10) + (uses * 10) RF/s

**效果**:
- LV1: 基础隐身（Invisibility 药水）
- LV2: + Silent（无声）
- LV3: + Resistance II

**状态管理**:
```
stealthPlayers - 当前隐身玩家 (UUID → level)
stealthStartTime - 隐身开始时间
stealthCooldownEnd - 冷却结束时间
consecutiveUses - 连续使用次数
```

#### ExpAmplifierSystem (经验增幅)

**核心逻辑**:
- 击杀奖励倍率：1.0 + (0.5 * level)
- 拾取增幅倍率：1.0 + (0.5 * level)
- 连杀加成：combo * 0.1 (最大 x10)
- 连杀超时：5000ms

**基础经验表**:
```
Boss (Wither/Dragon): 50 EXP
Elite (Enderman/Creeper/Witch/Blaze): 10 EXP
Mob: 5 EXP
Animal: 1 EXP
Villager: 0 EXP
```

---

## 📦 Manager 4: EnergyUpgradeManager (557行)

### 事件处理器

#### 1. `onPlayerTick(TickEvent.PlayerTickEvent)`

**触发频率**: 每 tick (服务端)

**功能**: 驱动所有发电模块

**调用流程**:
```java
onPlayerTick()
├─ if (KINETIC_GENERATOR > 0)
│  └─ KineticGeneratorSystem.generateFromMovement()
├─ if (SOLAR_GENERATOR > 0)
│  └─ SolarGeneratorSystem.generateFromSunlight()
├─ if (VOID_ENERGY > 0)
│  └─ VoidEnergySystem.generateFromVoid()
├─ 连杀重置检查（超时清除）
└─ 动能缓冲溢出保护
```

#### 2. `onBlockBreak(BlockEvent.BreakEvent)` - KineticGeneratorSystem

**触发时机**: 方块被破坏

**功能**: 根据硬度产生能量

**逻辑**:
```java
onBlockBreak()
├─ hardness = blockState.getBlockHardness()
├─ base = BLOCK_BREAK_BASE (10 RF)
├─ energy = floor(hardness * base * level * generationMultiplier)
└─ addEnergy(energy)
```

#### 3. `onEntityKill(LivingDeathEvent)` - CombatChargerSystem

**触发时机**: 实体死亡

**功能**: 战斗充能

**逻辑**:
```java
onEntityKill()
├─ base = maxHP * ENERGY_PER_HP * level
├─ bossMul = 3.0 (Boss) / 2.0 (MiniBoss) / 1.0 (Normal)
├─ 连杀系统：
│  ├─ streak = combatStreak.get(id) + 1
│  ├─ streakMul = min(1.0 + 0.1 * streak, MAX_STREAK_BONUS)
│  └─ 连杀超时：STREAK_TIMEOUT (6000 ticks)
├─ energy = floor(base * bossMul * streakMul * generationMultiplier)
├─ addEnergy(energy)
└─ if (Boss) 掉落能量精华（红石）
```

### 子系统详解

#### KineticGeneratorSystem (动能发电)

**核心逻辑**:
- 基础：ENERGY_PER_BLOCK (5 RF) + ENERGY_PER_LEVEL (8 RF) * level
- 疾跑倍率：1.5x
- 鞘翅飞行倍率：2.0x
- 跳跃倍率：1.2x
- 缓冲阈值：500 RF

**位移检测**:
```
每 tick 计算与上次位置的距离
过滤传送（distance > 100）
缓冲累积到阈值后统一入账
```

**挖掘产能**:
```
energy = hardness * 10 * level * generationMultiplier
```

#### SolarGeneratorSystem (太阳能)

**核心逻辑**:
- 基础：40 RF/s * level
- 高度加成：Y > 100，线性增长，最大 1.3x
- 天气惩罚：雨天 0.4x，雷暴 0.2x
- 最小光照：12
- 更新间隔：20 ticks (1秒)

**条件检查**:
```
canSeeSky() && isDaytime() && skyLight >= 12
```

#### VoidEnergySystem (虚空能量)

**核心逻辑**:
- 充能速率：2/tick * level * zoneMult
- 转换率：100 charge → 25 RF
- 末地倍率：1.5x
- 末地额外奖励：80 RF/5s * level
- 深层加成：Y < 20 (3x), Y < 0 (更高)

**激活条件**:
```
dimension == 1 (末地) || posY < 20 (深层)
```

#### CombatChargerSystem (战斗充能)

**核心逻辑**:
- 基础：maxHP * 20 RF/HP * level
- Boss 倍率：3.0x
- Mini-Boss 倍率：2.0x
- 连杀倍率：1.0 + 0.1 * streak (最大 2.0x)
- 连杀超时：6000 ticks (5分钟)

**特殊掉落**:
```
Boss 击杀 → 红石 * level ("能量精华")
```

---

## 🔗 事件处理依赖关系

### 事件调用顺序

```
PlayerTickEvent (Phase.END)
├─ SurvivalUpgradeManager.onPlayerTick()
├─ CombatUpgradeManager.onPlayerTick()
├─ AuxiliaryUpgradeManager.onPlayerTick()
└─ EnergyUpgradeManager.onPlayerTick()

LivingHurtEvent (NORMAL)
└─ SurvivalUpgradeManager.onPlayerHurt()

LivingHurtEvent (LOWEST)
└─ CombatUpgradeManager.onLivingHurtLowest()

AttackEntityEvent
└─ CombatUpgradeManager.onAttack()

LivingDeathEvent (HIGH)
└─ AuxiliaryUpgradeManager.ExpAmplifierSystem.onEntityDeath()

LivingDeathEvent (NORMAL)
└─ EnergyUpgradeManager.CombatChargerSystem.onEntityKill()

BlockEvent.BreakEvent
├─ AuxiliaryUpgradeManager.OreVisionSystem.onBlockBreak()
└─ EnergyUpgradeManager.KineticGeneratorSystem.onBlockBreak()
```

### 潜在冲突

1. **LivingDeathEvent** - 两个监听器
   - ExpAmplifierSystem (HIGH) - 生成经验球
   - CombatChargerSystem (NORMAL) - 产生能量
   - **结论**: 无冲突，优先级不同

2. **BlockEvent.BreakEvent** - 两个监听器
   - OreVisionSystem - 移除缓存
   - KineticGeneratorSystem - 产生能量
   - **结论**: 无冲突，功能独立

---

## 📝 迁移策略

### 方案 A：统一事件处理器（推荐）

创建统一的事件处理器，调用模块方法：

```java
@Mod.EventBusSubscriber
public class MechCoreEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 调用 ModuleTickHandler（已存在）
        // 额外处理：能量状态管理、SimpleDifficulty 集成等
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 调用相关模块的伤害处理方法
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtLowest(LivingHurtEvent event) {
        // 伤害加成处理
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        // 攻击事件处理
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityDeath(LivingDeathEvent event) {
        // 击杀事件处理（经验 + 战斗充能）
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 挖掘事件处理
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        // 客户端渲染
    }
}
```

### 方案 B：保留旧 Manager 作为桥接（过渡）

保留旧 Manager，但修改为调用新模块：

```java
@Deprecated
public class SurvivalUpgradeManager {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 桥接到新模块
        IMechCoreData data = player.getCapability(...);
        for (ModuleContainer container : data.getActiveModules()) {
            IMechCoreModule module = ModuleRegistry.getNew(container.getModuleId());
            module.onTick(player, data, context);
        }
    }
}
```

### 迁移优先级

1. **Phase 1**: 创建 MechCoreEventHandler 统一事件处理器
2. **Phase 2**: 迁移生存类模块事件（护盾、生命恢复、反伤、灭火）
3. **Phase 3**: 迁移战斗类模块事件（伤害加成、攻速、范围、追击）
4. **Phase 4**: 迁移能量类模块事件（动能、太阳能、虚空、战斗充能）
5. **Phase 5**: 迁移辅助类模块事件（透视、速度、隐身、经验）
6. **Phase 6**: 删除旧 Manager 中的 @SubscribeEvent 方法

---

## 🎯 关键技术点

### 1. SimpleDifficulty 集成

**现状**: 在 HungerThirstSystem 中使用反射

**迁移方案**:
- 保留反射逻辑
- 移植到 HungerThirstModule
- 或创建独立的 SimpleDifficultyIntegration 工具类

### 2. 客户端渲染

**现状**: OreVisionSystem 在 AuxiliaryUpgradeManager 中

**迁移方案**:
- 创建客户端事件处理器
- 或将渲染逻辑移到 OreVisionModule（如果创建）
- 保持 `@SideOnly(Side.CLIENT)` 注解

### 3. AttributeModifier 管理

**现状**: 多个系统使用固定 UUID

**迁移方案**:
- 统一 UUID 管理
- 在模块 onDeactivate() 时移除 modifier
- 在模块 onActivate() 时应用 modifier

### 4. 连杀/连击系统

**现状**: 静态 Map 存储

**迁移方案**:
- 迁移到 IMechCoreData 或 ModuleMeta
- 或创建独立的 ComboTracker 服务

---

**报告生成时间**: 2025-01-XX
**分析文件**:
- SurvivalUpgradeManager.java (744 行)
- CombatUpgradeManager.java (561 行)
- AuxiliaryUpgradeManager.java (1107 行)
- EnergyUpgradeManager.java (557 行)

**总代码量**: 2969 行
**事件处理器数量**: 15 个
