# Mechanical Core 模块迁移状态

## 📊 迁移进度总览

**已迁移**: 23/27 (85.2%)
**待迁移**: 4/27 (14.8%)

---

## ✅ 已迁移模块 (23)

### 核心模块 (5)
1. **FlightModule** (`FLIGHT_MODULE`)
   - 等级: Lv.1-3
   - 功能: 创造模式飞行 + 速度提升 + 悬停模式
   - 文件: `capability/module/impl/FlightModule.java`

2. **ShieldGeneratorModule** (`YELLOW_SHIELD`)
   - 等级: Lv.1-5
   - 功能: 黄条护盾 (每级 +1 护盾点)
   - 文件: `capability/module/impl/ShieldGeneratorModule.java`

3. **EnergyCapacityModule** (`ENERGY_CAPACITY`)
   - 等级: Lv.1-10
   - 功能: 增加能量容量 (每级 +10000 RF)
   - 文件: `capability/module/impl/EnergyCapacityModule.java`

4. **ArmorEnhancementModule** (`ARMOR_ENHANCEMENT`)
   - 等级: Lv.1-5
   - 功能: 护甲强化 (每级 +2 护甲点)
   - 文件: `capability/module/impl/ArmorEnhancementModule.java`

5. **RegenerationModule** (`HEALTH_REGEN`)
   - 等级: Lv.1-5
   - 功能: 自动恢复生命值
   - 文件: `capability/module/impl/RegenerationModule.java`

### 生存类模块 (3)
6. **HungerThirstModule** (`HUNGER_THIRST`)
   - 等级: Lv.1-3
   - 功能: 饥饿/口渴管理 + SimpleDifficulty 集成
   - 文件: `capability/module/impl/HungerThirstModule.java`
   - 特性: 反射集成 SimpleDifficulty 口渴系统

7. **ThornsModule** (`THORNS`)
   - 等级: Lv.1-3
   - 功能: 反伤荆棘 (15%/30%/45%)
   - 文件: `capability/module/impl/ThornsModule.java`
   - 事件: `ModuleEventHandler.onPlayerHurt()`

8. **FireExtinguishModule** (`FIRE_EXTINGUISH`)
   - 等级: Lv.1-3
   - 功能: 自动灭火 (60/40/20 tick 冷却)
   - 文件: `capability/module/impl/FireExtinguishModule.java`

### 战斗类模块 (4)
9. **DamageBoostModule** (`DAMAGE_BOOST`)
   - 等级: Lv.1-5
   - 功能: 伤害提升 (25%~125%) + 暴击系统 (10%~50%)
   - 文件: `capability/module/impl/DamageBoostModule.java`
   - 事件: `ModuleEventHandler.onLivingHurtLowest()`

10. **AttackSpeedModule** (`ATTACK_SPEED`)
    - 等级: Lv.1-3
    - 功能: 攻击速度提升 (20%/40%/60%) + 连击系统
    - 文件: `capability/module/impl/AttackSpeedModule.java`
    - 事件: `ModuleEventHandler.onAttackEntity()`
    - 特性: 使用 AttributeModifier (ATTACK_SPEED)

11. **RangeExtensionModule** (`RANGE_EXTENSION`)
    - 等级: Lv.1-3
    - 功能: 攻击范围扩展 (+3/+6/+9 格)
    - 文件: `capability/module/impl/RangeExtensionModule.java`
    - 特性: 使用 AttributeModifier (REACH_DISTANCE)

12. **PursuitModule** (`PURSUIT`)
    - 等级: Lv.1-3
    - 功能: 追击系统 (2/4/6 层，每层 +10% 伤害) + 追击冲刺
    - 文件: `capability/module/impl/PursuitModule.java`
    - 事件: `ModuleEventHandler.onAttackEntity()`, `onLivingHurtLowest()`

### 能量类模块 (4)
13. **KineticGeneratorModule** (`KINETIC_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 动能发电（移动 5+8*level RF/block，挖掘产能）
    - 文件: `capability/module/impl/KineticGeneratorModule.java`
    - 事件: `ModuleEventHandler.onBlockBreak()`
    - 特性: 缓冲系统 + 移动速度倍率（冲刺 1.5x，鞘翅 2.0x）

14. **SolarGeneratorModule** (`SOLAR_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 太阳能发电（40 RF/s * level）
    - 文件: `capability/module/impl/SolarGeneratorModule.java`
    - 特性: 高度加成（Y>100: 1.3x），天气影响（雨 0.4x，雷暴 0.2x）

15. **VoidEnergyModule** (`VOID_ENERGY`)
    - 等级: Lv.1-5
    - 功能: 虚空能量（低层/末地产能）
    - 文件: `capability/module/impl/VoidEnergyModule.java`
    - 特性: 充能系统（100 charge → 25 RF），末地 1.5x，深层 3x（Y<20）

16. **CombatChargerModule** (`COMBAT_CHARGER`)
    - 等级: Lv.1-5
    - 功能: 战斗充能（击杀产能 maxHP * 20 RF/HP * level）
    - 文件: `capability/module/impl/CombatChargerModule.java`
    - 事件: `ModuleEventHandler.onEntityDeath()`
    - 特性: Boss 倍率（3.0x），连杀系统（最大 2.0x），超时 6000 ticks

### 辅助类模块 (4)
17. **MovementSpeedModule** (`MOVEMENT_SPEED`)
    - 等级: Lv.1-5
    - 功能: 移动速度提升（20%/40%/60%/80%/100%）
    - 文件: `capability/module/impl/MovementSpeedModule.java`
    - 特性: 使用 AttributeModifier (MOVEMENT_SPEED)
    - 能量: 8 * level RF/tick

18. **StealthModule** (`STEALTH`)
    - 等级: Lv.1-3
    - 功能: 隐身系统（基础/高级/完美）
    - 文件: `capability/module/impl/StealthModule.java`
    - 特性: 持续时间（30s/45s/60s），冷却（20s/30s/45s），连续使用惩罚
    - 能量: 50 - level*10 + consecutive*10 RF/tick

19. **ExpAmplifierModule** (`EXP_AMPLIFIER`)
    - 等级: Lv.1-5
    - 功能: 经验放大（1.5x~3.5x）+ 附魔等级加成（5~25）
    - 文件: `capability/module/impl/ExpAmplifierModule.java`
    - 事件: `ModuleEventHandler.onEntityDeath()`, `onPlayerPickupXp()`
    - 特性: 连杀系统（+0.1 per kill，最大 +1.0）
    - 能量: Kill: baseExp*3 RF，Pickup: orbValue*2 RF

20. **OreVisionModule** (`ORE_VISION`)
    - 等级: Lv.1-5
    - 功能: 矿物透视（8/16/24/32/40 格范围）
    - 文件: `capability/module/impl/OreVisionModule.java`
    - 特性: 矿物分类过滤，客户端渲染支持
    - 能量: 激活 100 RF，维持 50+level*10 RF/s

### 特殊类模块 (3)
21. **MagicAbsorbModule** (`MAGIC_ABSORB`)
    - 等级: Lv.1-3
    - 功能: 吸收魔法伤害转化为能量（30%/50%/70% 吸收率）
    - 文件: `capability/module/impl/MagicAbsorbModule.java`
    - 事件: `ModuleEventHandler.onPlayerHurt()`
    - 能量转化: 20/30/40 RF/伤害点
    - 特性: 统计吸收总量和能量获取

22. **NeuralSynchronizerModule** (`NEURAL_SYNCHRONIZER`)
    - 等级: Lv.1
    - 功能: 神经同步器（提升适应度 +100，减少排异 -0.005/s）
    - 文件: `capability/module/impl/NeuralSynchronizerModule.java`
    - 依赖: FleshRejectionSystem（血肉排异系统）
    - 能量: 3 RF/tick（50 RF/s）
    - 特性: 清除负面效果，减缓出血

23. **TemperatureControlModule** (`TEMPERATURE_CONTROL`)
    - 等级: Lv.1-5
    - 功能: 温度控制（自动调节体温，抵抗极端温度）
    - 文件: `capability/module/impl/TemperatureControlModule.java`
    - 集成: SimpleDifficulty（反射）+ 生物群系备用
    - 能量: 10 * level RF/tick
    - 特性: Lv.2 抗火，Lv.3 极端温度抗性，Lv.4 水下呼吸，Lv.5 夜视+特殊环境增强

---

## 🔄 待迁移模块 (4)

### 系统整合模块 (3)
这些模块需要与系统层整合，而非作为独立模块

24. **WaterproofModule** (`WATERPROOF_MODULE`) - Phase 3G
    - 等级: Lv.1-3
    - 功能: 防水系统（保护核心免受水损害）
    - 旧实现: `WetnessSystem.java`
    - 状态: 需要整合 WetnessSystem 到模块中

25. **EnergyEfficiency** (系统级) - Phase 3H
    - 等级: Lv.1-5
    - 功能: 降低所有模块的能量消耗（全局倍率）
    - 旧实现: `EnergyEfficiencyManager.java`
    - 状态: 需要在能量消耗系统中全局应用

26. **EnergyPunishment** (系统级) - Phase 3I
    - 功能: 能量惩罚系统
    - 旧实现: `EnergyPunishmentSystem.java`
    - 状态: 需要整合到能量系统中

### 可选模块 (1)
27. **ThermalGeneratorModule** (`THERMAL_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 热能发电（在岩浆附近产能）
    - 状态: 需要确认是否存在完整实现

---

## 📋 迁移优先级建议

### 🔥 高优先级（核心功能）
1. ✅ FLIGHT_MODULE（已完成）
2. ✅ YELLOW_SHIELD（已完成）
3. ✅ HEALTH_REGEN（已完成）
4. ✅ ENERGY_CAPACITY（已完成）
5. ✅ ARMOR_ENHANCEMENT（已完成）
6. ENERGY_EFFICIENCY（影响所有模块）
7. SOLAR_GENERATOR（主要能量来源）
8. KINETIC_GENERATOR（主要能量来源）

### 🎯 中优先级（常用功能）
9. DAMAGE_BOOST（战斗增强）
10. ATTACK_SPEED（战斗增强）
11. MOVEMENT_SPEED（移动增强）
12. HUNGER_THIRST（生存必需）
13. WATERPROOF_MODULE（环境保护）
14. COMBAT_CHARGER（能量恢复）

### 📌 低优先级（特殊功能）
15. ORE_VISION（矿物透视）
16. EXP_AMPLIFIER（经验放大）
17. STEALTH（隐身）
18. THORNS（反伤）
19. FIRE_EXTINGUISH（灭火）
20. VOID_ENERGY（虚空能量）
21. RANGE_EXTENSION（范围扩展）
22. PURSUIT（追击）
23. TEMPERATURE_CONTROL（温度调节）
24. MAGIC_ABSORB（魔法吸收）
25. NEURAL_SYNCHRONIZER（神经同步）
26. THERMAL_GENERATOR（热能发电）
27. SPEED_BOOST（如果和 MOVEMENT_SPEED 不同）

---

## 🔧 迁移技术要点

### 已实现的架构模式
- 继承 `AbstractMechCoreModule`
- 实现完整生命周期回调：
  - `onActivate()` - 激活时
  - `onDeactivate()` - 停用时
  - `onTick()` - 每 tick 执行
  - `onLevelChanged()` - 等级变化时
- 使用 `ModuleContext` 获取执行上下文
- 使用 `NBTTagCompound` 存储模块元数据
- 能量消耗通过 `getPassiveEnergyCost()` 定义

### 需要特殊处理的模块
- **矿物透视**: 需要客户端渲染支持
- **移动速度**: 需要 Attribute Modifier
- **攻击速度**: 需要 Attribute Modifier + 事件监听
- **隐身**: 需要修改实体AI系统
- **发电模块**: 需要事件监听（移动/挖掘/击杀）

---

## 📝 下一步计划

1. **继续迁移高优先级模块**（6-8）
2. **实现网络同步系统**（Phase 4）
3. **实现 ViewModel**（Phase 5）
4. **重构 GUI**（Phase 5）
5. **删除旧代码**（Phase 6）

---

**更新时间**: 2025-01-XX
**当前分支**: `claude/refactor-mechanical-core-016N4rEmqDuAD8PcaLNtuzrZ`
