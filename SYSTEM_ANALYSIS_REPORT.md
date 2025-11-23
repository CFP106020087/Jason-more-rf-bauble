# 机械核心三大系统分析报告

## 📋 概述

本报告详细分析了 Mechanical Core 的三大核心系统：
1. **怕水系统（WetnessSystem）** - 潮湿度与防水机制
2. **能量消耗系统（EnergyConsumptionManager）** - 模块能量消耗计算
3. **能量惩罚系统（EnergyDepletionManager）** - 低电量状态管理

---

## 🌧️ 系统一：怕水系统（WetnessSystem）

### 核心设计理念

机械核心怕水，需要防水模块保护。这是一个完整的**潮湿度管理系统**，包含：
- 雨天潮湿值累积
- 防水模块保护
- 故障状态升级
- 自然/加速干燥

### 数据结构

```java
// 全局静态 Map 存储玩家状态
private static final Map<UUID, Integer> playerWetness;           // 潮湿值 (0-100)
private static final Map<UUID, Integer> updateTickCounter;       // tick 计数器
private static final Map<UUID, Integer> dryingDelayCounter;      // 干燥延迟计数器
private static final Map<UUID, Long> malfunctionStartTime;       // 故障开始时间
private static final Map<UUID, Integer> currentMalfunctionLevel; // 当前故障等级
```

### 关键常量

| 常量 | 值 | 说明 |
|------|------|------|
| `MAX_WETNESS` | 100 | 最大潮湿值 |
| `UPDATE_INTERVAL` | 20 ticks (1秒) | 更新间隔 |
| `RAIN_WETNESS_PER_SEC` | 4 | 普通雨每秒 +4 (25秒满) |
| `THUNDER_WETNESS_PER_SEC` | 5 | 雷雨每秒 +5 (20秒满) |
| `NATURAL_DRY_PER_SEC` | 2 | 自然干燥每秒 -2 |
| `HEAT_DRY_PER_SEC` | 4 | 高温干燥每秒 -4 |
| `WETNESS_MALFUNCTION_THRESHOLD` | 80 | 故障阈值 (20秒达到) |
| `DRYING_DELAY_SECONDS` | 3 | 离开雨3秒后开始干燥 |

### 核心流程

#### 1. 每 Tick 调用流程

```java
updateWetness(EntityPlayer player, ItemStack coreStack)
├─ tick计数器累加（每20 ticks执行一次真正的更新）
├─ 非更新周期：checkMalfunctionEffects() - 确保故障效果持续
└─ 更新周期（每秒）：
   ├─ 获取防水等级：WaterproofUpgrade.getEffectiveWaterproofLevel()
   ├─ 检查是否淋雨：isPlayerInRain()
   ├─ 淋雨 → handleRainWetness() - 增加潮湿值
   │  ├─ LV0: 正常速率
   │  ├─ LV1: 减少50%
   │  └─ LV2+: 完全免疫
   ├─ 不在雨中 → handleDrying() - 自然干燥
   │  ├─ 3秒延迟后开始
   │  ├─ SimpleDifficulty温度系统集成（高温加速）
   │  └─ 潮湿值降至安全值时清除故障
   └─ 潮湿值 >= 80 且防水 < LV2 → applyWetnessMalfunction()
```

#### 2. 防水模块效果

| 防水等级 | 效果 | 说明 |
|---------|------|------|
| **LV0** | 无保护 | 正常受潮速率 (4-5/秒) |
| **LV1** | 减少50% | 受潮速率减半 (2-2.5/秒) |
| **LV2+** | 完全免疫 | 潮湿值增加为0 |

#### 3. 故障机制（重要！）

**触发条件**：
- 潮湿值 >= 80
- 防水等级 < LV2

**故障等级系统**：

```java
applyWetnessMalfunction()
├─ 基础等级：
│  ├─ 潮湿值 >= 100 → LV1
│  └─ 潮湿值 90-99 → LV0
├─ 时间升级（还在雨中且潮湿值满时）：
│  ├─ 30秒后 → +1级 (LV2)
│  └─ 60秒后 → +2级 (LV3)
└─ 效果：
   ├─ 施加 MALFUNCTION 药水效果（40 tick持续）
   ├─ 每秒消耗能量：50 * (等级 + 1) RF/s
   └─ 随机短路（每秒检查）：
      ├─ 几率：15% - (等级 * 4%)
      └─ 损失能量：总能量 / (4 - 等级)
```

#### 4. SimpleDifficulty 集成（高温加速干燥）

```java
static {
    if (Loader.isModLoaded("simpledifficulty")) {
        // 反射获取温度 Capability
        Class<?> sdCapabilities = Class.forName("...");
        temperatureCapability = sdCapabilities.getField("TEMPERATURE").get(null);
        getTemperatureLevelMethod = ...;
    }
}

// 在干燥时使用温度信息
int temp = getPlayerTemperature(player);
if (temp > 15) dryRate = HEAT_DRY_PER_SEC;      // 4/秒
if (temp > 20) dryRate = HEAT_DRY_PER_SEC * 2;  // 8/秒
```

#### 5. 玩家交互

**毛巾使用**：
```java
useTowel(EntityPlayer player, int dryAmount)
├─ 检查当前潮湿值 > 0
├─ 检查不在雨中
├─ 减少潮湿值
└─ 如果降到阈值以下，清除故障
```

**状态显示**：
- 首次淋雨警告（潮湿值 >= 20）
- 故障阈值警告（潮湿值 >= 80）
- 危险警告（潮湿值 >= 90）
- 定期状态显示（每5秒）

### 迁移要点（WaterproofModule）

#### 需要实现的功能

1. **模块等级效果**：
   - LV1: 50%减免
   - LV2+: 完全免疫

2. **潮湿值管理**：
   - 迁移到 Capability 存储（`ModuleMeta` or `IMechCoreData` 直接字段）
   - 使用 `onTick()` 实现每秒更新逻辑

3. **故障系统**：
   - 施加 `MALFUNCTION` 药水效果
   - 能量消耗通过 `data.consumeEnergy()`
   - 短路事件通过事件系统触发

4. **SimpleDifficulty 集成**：
   - 保留温度读取逻辑
   - 高温加速干燥

5. **UI 提示**：
   - 状态消息通过 `player.sendStatusMessage()`
   - 警告音通过 `world.playSound()`

---

## ⚡ 系统二：能量消耗系统（EnergyConsumptionManager）

### 核心设计理念

这是一个**中心化能量消耗计算器**，负责：
- 统计所有模块的能量消耗
- 计算特殊情况的额外消耗
- 应用能量效率减免
- 提供能量平衡分析

### 数据结构

```java
// 静态 Map 存储各模块的基础消耗（RF/秒）
private static final Map<String, Integer> UPGRADE_CONSUMPTION = new HashMap<>();

static {
    // 基础模块
    UPGRADE_CONSUMPTION.put("ARMOR_ENHANCEMENT", 20);
    UPGRADE_CONSUMPTION.put("SPEED_BOOST", 30);
    UPGRADE_CONSUMPTION.put("REGENERATION", 50);
    UPGRADE_CONSUMPTION.put("FLIGHT_MODULE", 100);
    // ... 共约20种模块
}
```

### 消耗配置表

| 模块类别 | 示例模块 | 每级消耗 (RF/s) |
|---------|---------|----------------|
| **生存类** | YELLOW_SHIELD | 40 |
| | HEALTH_REGEN | 50 |
| | HUNGER_THIRST | 20 |
| | THORNS | 30 |
| **战斗类** | DAMAGE_BOOST | 50 |
| | ATTACK_SPEED | 40 |
| | RANGE_EXTENSION | 30 |
| | PURSUIT | 40 |
| **辅助类** | ORE_VISION | 80 (使用时) |
| | MOVEMENT_SPEED | 40 |
| | STEALTH | 60 (激活时) |
| | EXP_AMPLIFIER | 30 |

### 核心流程

#### 1. 总消耗计算

```java
calculateTotalConsumption(ItemStack coreStack, EntityPlayer player)
├─ 遍历所有基础模块（ItemMechanicalCore.UpgradeType）
│  └─ totalConsumption += UPGRADE_CONSUMPTION.get(key) * level
├─ 遍历所有扩展模块（ItemMechanicalCoreExtended）
│  └─ totalConsumption += UPGRADE_CONSUMPTION.get(key) * level
├─ 特殊消耗：calculateSpecialConsumption()
│  ├─ 飞行额外消耗：200 * level * speedMode.getMultiplier()
│  ├─ 矿物透视激活：100 * level
│  ├─ 隐身激活：150 * level
│  └─ 战斗状态：+100 RF/s
├─ 非线性增长：multiplier = 1.0 + (nonEnergyUpgrades / 5) * 0.1
├─ 应用能量效率减免：efficiency = EnergyEfficiencyManager.getEfficiencyMultiplier()
└─ return totalConsumption * efficiency
```

#### 2. 特殊消耗详解

**飞行模式消耗**：
```java
if (player.capabilities.isFlying) {
    SpeedMode mode = ItemMechanicalCore.getSpeedMode(coreStack);
    extra += 200 * flightLevel * mode.getMultiplier();
    // NORMAL(1.0) = 200/400/600 RF/s
    // FAST(2.0) = 400/800/1200 RF/s
    // SUPER(3.0) = 600/1200/1800 RF/s
}
```

**主动模块消耗**：
```java
// 矿物透视激活（通过 EntityData 标记）
if (player.getEntityData().getBoolean("MechanicalCoreOreVision")) {
    extra += 100 * oreLevel; // 100/200/300/400/500 RF/s
}

// 隐身激活
if (player.getEntityData().getBoolean("MechanicalCoreStealth")) {
    extra += 150 * stealthLevel; // 150/300/450/600/750 RF/s
}

// 战斗状态
if (player.getLastAttackedEntityTime() < 100) { // 5秒内攻击过
    extra += 100; // 额外 100 RF/s
}
```

#### 3. 能量平衡计算

```java
calculateEnergyBalance(ItemStack coreStack, EntityPlayer player)
├─ production = calculateTotalProduction()
│  ├─ KINETIC_GENERATOR: 150 * level (移动时)
│  ├─ SOLAR_GENERATOR: 100 * level (白天+视野通天)
│  ├─ VOID_ENERGY: 250 * level (末地或Y<30)
│  └─ 应用效率加成：production * efficiencyBonus
├─ consumption = calculateTotalConsumption()
└─ return production - consumption
```

#### 4. 每秒能量消耗应用

```java
applyEnergyConsumption(EntityPlayer player, ItemStack coreStack)
├─ consumption = calculateTotalConsumption()
├─ consumed = ItemMechanicalCore.consumeEnergy(coreStack, consumption)
└─ if (!consumed && 每5秒) {
       显示警告："能量不足！消耗: XXX RF/s"
   }
```

#### 5. 消耗明细系统

```java
getConsumptionBreakdown(ItemStack coreStack, EntityPlayer player)
├─ 收集所有模块的消耗
├─ breakdown.totalBase = 基础消耗总和
├─ breakdown.specialConsumption = 特殊消耗
├─ breakdown.efficiency = 效率倍率
└─ breakdown.totalFinal = 最终消耗
```

**ConsumptionBreakdown 结构**：
```java
class ConsumptionBreakdown {
    List<ConsumptionItem> items;  // 每个模块的消耗明细
    int totalBase;                 // 基础消耗总和
    int specialConsumption;        // 特殊消耗（飞行/透视等）
    double efficiency;             // 效率倍率
    int totalFinal;                // 最终消耗（应用所有倍率后）
}
```

### 与配置系统的关系

`EnergyConsumptionManager` 使用 `EnergyBalanceConfig` 中定义的常量：
- `BasicUpgrades.*` - 基础模块消耗
- `ExtendedUpgrades.*` - 扩展模块消耗
- `AuxiliaryActive.*` - 主动技能消耗
- `CombatActive.*` - 战斗技能消耗
- `SurvivalActive.*` - 生存技能消耗

### 迁移要点

#### 需要整合的功能

1. **被动消耗**：
   - 每个模块的 `getPassiveEnergyCost(int level)` 应返回配置表的值
   - 在 `ModuleTickHandler` 中统一收集所有激活模块的消耗

2. **主动消耗**：
   - 飞行消耗：在 `FlightModule.onTick()` 中实现
   - 矿物透视/隐身：在对应模块的 `onTick()` 中检查激活状态

3. **特殊消耗**：
   - 战斗状态消耗：在 `MechCoreService` 中检测战斗状态

4. **非线性增长**：
   - 在 `MechCoreService` 统一应用
   - 过载惩罚使用 `EnergyBalanceConfig.OverloadPenalty.getOverloadMultiplier()`

5. **能量效率**：
   - 从 `EnergyEfficiencyManager` 获取倍率
   - 应用到最终消耗计算

---

## 🔋 系统三：能量惩罚系统（EnergyDepletionManager）

### 核心设计理念

这是一个**能量状态管理系统**，负责：
- 根据能量百分比划分状态
- 根据能量状态禁用高耗能模块
- 触发低电量惩罚（接入 `EnergyPunishmentSystem`）
- 管理模块的手动禁用和惩罚锁定

### 能量状态枚举

```java
enum EnergyStatus {
    NORMAL       (0.30f, "正常运行",  GREEN,     "✓"),   // 30%以上
    POWER_SAVING (0.15f, "省电模式", YELLOW,    "⚡"),  // 15-30%
    EMERGENCY    (0.05f, "紧急模式",  RED,       "⚠"),   // 5-15%
    CRITICAL     (0.00f, "生命支持",  DARK_RED,  "💀");  // 0-5%
}
```

### 模块分类系统

#### 1. 高耗能模块（POWER_SAVING 时禁用）

```java
isHighConsumptionUpgrade(String cid)
├─ ORE_VISION
├─ STEALTH
└─ FLIGHT_MODULE
```

#### 2. 重要模块（EMERGENCY 时保留）

```java
isImportantUpgrade(String cid)
├─ 生存: HEALTH_REGEN, FIRE_EXTINGUISH, THORNS
├─ 防护: YELLOW_SHIELD, SHIELD_GENERATOR, HUNGER_THIRST, TEMPERATURE_CONTROL
├─ 战斗: DAMAGE_BOOST, ATTACK_SPEED
└─ 被动: ARMOR_ENHANCEMENT
```

#### 3. 必需模块（CRITICAL 时保留）

```java
isEssentialUpgrade(String cid)
├─ HEALTH_REGEN
├─ REGENERATION
├─ FIRE_EXTINGUISH
├─ THORNS
└─ TEMPERATURE_CONTROL
```

### 模块最低能量线

每个模块有最低能量要求，低于该值时无法使用：

| 模块类别 | 示例模块 | 最低能量 (RF) |
|---------|---------|--------------|
| **生存必需** | HEALTH_REGEN | 0 |
| | FIRE_EXTINGUISH | 50 |
| **防护** | YELLOW_SHIELD | 300 |
| | HUNGER_THIRST | 200 |
| **战斗** | DAMAGE_BOOST | 400 |
| | PURSUIT | 500 |
| **移动** | MOVEMENT_SPEED | 600 |
| | FLIGHT_MODULE | 800 |
| **特殊** | ORE_VISION | 1200 |
| | STEALTH | 1000 |

### 核心流程

#### 1. 模块可用性判定

```java
isUpgradeActive(ItemStack stack, String upgradeId)
├─ 检查手动禁用：nbt.getBoolean(kDisabled(cid))
├─ 检查惩罚锁定：nbt.getBoolean(kLock(cid))
├─ 检查最低能量：energy >= getMinimumEnergyForUpgrade(cid)
└─ 检查能量状态门控：
   ├─ NORMAL: 所有模块可用
   ├─ POWER_SAVING: 只有非高耗能模块
   ├─ EMERGENCY: 只有重要模块
   └─ CRITICAL: 只有必需模块
```

#### 2. 主循环处理

```java
handleEnergyDepletion(ItemStack stack, EntityPlayer player)
├─ current = getCurrentEnergyStatus(stack)
├─ previous = getPreviousEnergyStatus(stack)
├─ if (current != previous) {
│     executeStatusTransition(stack, player, previous, current)
│     setPreviousEnergyStatus(stack, current)
│  }
├─ if (EMERGENCY or CRITICAL) {
│     EnergyPunishmentSystem.tick(stack, player, current)
│  }
└─ if (CRITICAL && 每10秒) {
       产生红石粒子效果
   }
```

#### 3. 状态转换逻辑

```java
executeStatusTransition(stack, player, from, to)

NORMAL:
├─ 清除所有模式标记
└─ 提示："所有系统已恢复"

POWER_SAVING (15-30%):
├─ 设置 PowerSavingMode = true
├─ 高耗能功能降低
└─ 提示："省电模式 [XX%] - 高耗能功能已降低"

EMERGENCY (5-15%):
├─ 设置 EmergencyMode = true
├─ 非必要系统关闭
├─ 播放警报音（BLOCK_NOTE_PLING）
└─ 提示："紧急模式 [XX%] - 非必要系统已关闭"

CRITICAL (0-5%):
├─ 设置 CriticalMode = true
├─ 强制禁用飞行（非创造模式）
├─ 播放严重警告音（ENTITY_WITHER_HURT）
└─ 提示："生命支持 [XX%] - 仅保留生存系统！请立即充能！"
```

#### 4. 与惩罚系统集成

```java
// 在低能量状态下调用外部惩罚系统
if (cur == EnergyStatus.EMERGENCY || cur == EnergyStatus.CRITICAL) {
    EnergyPunishmentSystem.tick(stack, player, cur);
}
```

**EnergyPunishmentSystem 预期功能**：
- 施加负面药水效果（挖掘疲劳、缓慢、虚弱、失明、凋零）
- 根据 `EnergyBalanceConfig.LowEnergyDebuffs` 配置
- 节流机制防止刷屏

#### 5. 详细状态显示

```java
displayDetailedEnergyStatus(EntityPlayer player, ItemStack stack)
├─ 显示当前能量状态图标和名称
├─ 显示能量：当前/最大 (百分比)
└─ 系统状态概览：
   ├─ 遍历重要模块
   ├─ 检查 isUpgradeActive()
   └─ 显示 ✓/✗ + 模块名 + 等级 + 状态
```

### 与配置系统的关系

使用 `EnergyBalanceConfig` 中的：
- `EnergyThresholds.*` - 能量状态阈值
- `LowEnergyPenalty.*` - 低电量效率倍率
- `LowEnergyDebuffs.*` - 低电量负面效果配置

### 迁移要点

#### 需要整合的功能

1. **能量状态监控**：
   - 在 `ModuleTickHandler` 或 `MechCoreService` 中调用
   - 每 tick 检查能量状态变化

2. **模块可用性控制**：
   - 每个模块的 `canExecute()` 应调用 `isUpgradeActive()`
   - 或在 `ModuleTickHandler` 中统一过滤

3. **状态转换通知**：
   - 保留状态转换时的提示和音效
   - 使用事件系统广播状态变化

4. **手动禁用和锁定**：
   - 使用 `UpgradeKeys.kDisabled()` 和 `UpgradeKeys.kLock()` 存储
   - GUI 中提供手动禁用开关

5. **惩罚系统集成**：
   - 实现 `EnergyPunishmentSystem.tick()`
   - 应用 `LowEnergyDebuffs` 配置

---

## 🔗 三大系统的协同关系

### 系统交互图

```
┌──────────────────────────────────────────────────────────────┐
│                      ModuleTickHandler                        │
│                    (每 tick 统一调用)                          │
└─────────┬────────────────────────────────────┬───────────────┘
          │                                    │
          ▼                                    ▼
┌─────────────────────┐              ┌──────────────────────────┐
│  WetnessSystem      │              │ EnergyDepletionManager   │
│  updateWetness()    │              │ handleEnergyDepletion()  │
└──────────┬──────────┘              └────────┬─────────────────┘
           │                                  │
           │ 故障消耗能量                      │ 检查能量状态
           │ consumeEnergy()                  │ getCurrentEnergyStatus()
           │                                  │
           ▼                                  ▼
    ┌──────────────────────────────────────────────────┐
    │          EnergyConsumptionManager                 │
    │       calculateTotalConsumption()                 │
    │       applyEnergyConsumption()                    │
    └──────────┬───────────────────────────────────────┘
               │
               │ 读取配置
               ▼
        ┌────────────────────────┐
        │  EnergyBalanceConfig   │
        │  - 被动消耗配置        │
        │  - 主动消耗配置        │
        │  - 状态阈值配置        │
        │  - 惩罚配置            │
        └────────────────────────┘
```

### 能量流向分析

```
每 tick 执行流程：

1. 【能量产生】
   ├─ KineticGenerator: 移动产能
   ├─ SolarGenerator: 白天产能
   ├─ VoidEnergy: 深层/末地产能
   └─ CombatCharger: 击杀产能

2. 【能量消耗】
   ├─ 核心待机消耗: 5 RF/s
   ├─ 各模块被动消耗: Σ(level * baseCost)
   ├─ 特殊消耗:
   │  ├─ 飞行额外消耗
   │  ├─ 矿物透视/隐身激活
   │  └─ 战斗状态额外消耗
   ├─ 怕水系统故障消耗: 50 * (malfunctionLevel + 1) RF/s
   ├─ 模块泄漏: moduleTypes * 3 + totalLevels * 1
   └─ 过载惩罚: x OverloadMultiplier(totalLevels)

3. 【能量状态评估】
   ├─ percentage = current / max
   ├─ status = getCurrentEnergyStatus()
   └─ if (status changed) → executeStatusTransition()

4. 【模块禁用】
   ├─ if (POWER_SAVING) → 禁用高耗能模块
   ├─ if (EMERGENCY) → 仅保留重要模块
   └─ if (CRITICAL) → 仅保留必需模块

5. 【惩罚系统】
   └─ if (EMERGENCY or CRITICAL) → EnergyPunishmentSystem.tick()
```

### 数据存储位置

| 数据类型 | 当前存储方式 | 迁移后推荐方式 |
|---------|-------------|--------------|
| **潮湿值** | `Map<UUID, Integer>` | `IMechCoreData` 新字段或 `WaterproofModule` meta |
| **故障等级** | `Map<UUID, Integer>` | `WaterproofModule` meta |
| **能量状态** | `NBTTagCompound` | 保持 NBT（`PreviousEnergyStatus`） |
| **模块禁用标记** | `UpgradeKeys.kDisabled()` | 保持 NBT |
| **模块锁定标记** | `UpgradeKeys.kLock()` | 保持 NBT |
| **能量消耗统计** | 实时计算 | 实时计算（不存储） |

---

## 📝 迁移建议

### Phase 3G: 整合怕水设定到 WaterproofModule

#### 实现要点

1. **创建 WaterproofModule**：
   ```java
   public class WaterproofModule extends AbstractMechCoreModule {
       // 潮湿值数据存储在模块 meta 中
       // "WETNESS" - 当前潮湿值 (0-100)
       // "DRYING_DELAY" - 干燥延迟计数
       // "MALFUNCTION_START" - 故障开始时间
       // "MALFUNCTION_LEVEL" - 当前故障等级
   }
   ```

2. **onTick() 实现**：
   - 每 20 ticks 执行一次真正的更新
   - 调用 `isPlayerInRain()` 检查淋雨
   - 根据模块等级计算受潮速率
   - 实现干燥延迟和自然干燥
   - 触发故障效果和能量消耗

3. **SimpleDifficulty 集成**：
   - 保留温度读取逻辑
   - 在干燥时应用温度加速

4. **毛巾使用**：
   - 通过 GUI 或右键触发
   - 调用模块的 `useTowel()` 方法

### Phase 3H: 整合能量消耗系统

#### 实现要点

1. **被动消耗统一收集**：
   ```java
   // 在 ModuleTickHandler 或 MechCoreService
   int totalPassive = 0;
   for (ModuleContainer container : data.getActiveModules()) {
       IMechCoreModule module = ModuleRegistry.getNew(container.getModuleId());
       totalPassive += module.getPassiveEnergyCost(container.getLevel());
   }
   ```

2. **特殊消耗**：
   - 飞行消耗：在 `FlightModule.onTick()` 中计算并消耗
   - 主动技能：在对应模块的激活逻辑中消耗
   - 战斗状态：在 `MechCoreService` 中检测

3. **非线性增长和效率**：
   ```java
   int totalLevels = data.getTotalModuleLevels();
   int moduleTypes = data.getActiveModules().size();
   float overloadMultiplier = OverloadPenalty.getOverloadMultiplier(totalLevels);
   double efficiency = EnergyEfficiencyManager.getEfficiencyMultiplier(player);

   int finalConsumption = (int)(totalPassive * overloadMultiplier * efficiency);
   ```

4. **每秒应用消耗**：
   ```java
   // 在 ModuleTickHandler，每 20 ticks
   if (tickCounter % 20 == 0) {
       int consumption = calculateTotalConsumption();
       data.consumeEnergy(consumption);
   }
   ```

### Phase 3I: 整合能量惩罚系统

#### 实现要点

1. **能量状态监控**：
   ```java
   // 在 ModuleTickHandler
   EnergyStatus current = EnergyDepletionManager.getCurrentEnergyStatus(stack);
   EnergyStatus previous = getPreviousStatus(data);

   if (current != previous) {
       executeStatusTransition(player, data, previous, current);
       setPreviousStatus(data, current);
   }
   ```

2. **模块过滤**：
   ```java
   // 在执行模块 tick 前检查
   for (ModuleContainer container : data.getActiveModules()) {
       if (!EnergyDepletionManager.isUpgradeActive(stack, container.getModuleId())) {
           continue; // 跳过此模块
       }
       // 执行模块 tick...
   }
   ```

3. **实现 EnergyPunishmentSystem**：
   ```java
   public class EnergyPunishmentSystem {
       public static void tick(ItemStack stack, EntityPlayer player, EnergyStatus status) {
           // 根据 LowEnergyDebuffs 配置施加负面效果
           if (status == EMERGENCY) {
               // 挖掘疲劳、缓慢、虚弱
           } else if (status == CRITICAL) {
               // 更强的负面效果 + 失明
           }
       }
   }
   ```

4. **GUI 集成**：
   - 显示当前能量状态图标
   - 显示各模块的可用状态
   - 提供手动禁用开关

---

## 🎯 总结

### 三大系统的核心特点

| 系统 | 核心机制 | 关键数据 | 触发条件 |
|------|---------|---------|---------|
| **怕水系统** | 潮湿值累积 + 故障升级 | 潮湿值 (0-100) | 淋雨、防水等级 |
| **能量消耗** | 被动消耗 + 主动消耗 + 非线性增长 | 各模块消耗配置 | 模块激活、特殊状态 |
| **能量惩罚** | 状态分级 + 模块禁用 + 负面效果 | 能量百分比 | 能量阈值 |

### 迁移优先级

1. **Phase 3H** (能量消耗) - **最高优先级**
   - 影响所有模块的基础运作
   - 必须先建立统一的能量消耗框架

2. **Phase 3I** (能量惩罚) - **高优先级**
   - 依赖能量消耗系统
   - 提供低电量保护机制

3. **Phase 3G** (怕水系统) - **中优先级**
   - 相对独立的功能
   - 可以作为单独模块实现

### 技术难点

1. **能量消耗的统一收集**
   - 需要在 `ModuleTickHandler` 中遍历所有激活模块
   - 应用过载惩罚和效率倍率
   - 每秒统一消耗能量

2. **能量状态的响应式设计**
   - 状态变化时触发事件
   - 动态禁用/启用模块
   - UI 实时更新

3. **怕水系统的状态管理**
   - 从静态 Map 迁移到 Capability/Meta
   - 保持每秒更新的性能优化
   - SimpleDifficulty 集成的兼容性

4. **多系统协同**
   - 怕水故障消耗能量 → 触发能量惩罚
   - 能量惩罚禁用模块 → 减少能量消耗
   - 循环依赖的解耦设计

---

**报告生成时间**: 2025-01-XX
**当前分支**: `claude/refactor-mechanical-core-016N4rEmqDuAD8PcaLNtuzrZ`
**分析文件**:
- `WetnessSystem.java` (482 行)
- `EnergyConsumptionManager.java` (259 行)
- `EnergyDepletionManager.java` (371 行)
- `EnergyBalanceConfig.java` (429 行)

**总代码量**: 1541 行
