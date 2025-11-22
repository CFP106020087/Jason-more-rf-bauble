# Mechanical Core 模块迁移状态

## 📊 迁移进度总览

**已迁移**: 8/27 (29.6%)
**待迁移**: 19/27 (70.4%)

---

## ✅ 已迁移模块 (8)

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

---

## 🔄 待迁移模块 (19)

### 战斗类模块 (4)
来源: `upgrades/combat/CombatUpgradeManager.java`

9. **DamageBoostModule** (`DAMAGE_BOOST`)
   - 等级: Lv.1-5
   - 功能: 伤害提升 + 暴击系统
   - 旧实现: `CombatUpgradeManager.DamageBoostSystem`

10. **AttackSpeedModule** (`ATTACK_SPEED`)
    - 等级: Lv.1-5
    - 功能: 攻击速度提升 + 连击系统
    - 旧实现: `CombatUpgradeManager.AttackSpeedSystem`

11. **RangeExtensionModule** (`RANGE_EXTENSION`)
    - 等级: Lv.1-3
    - 功能: 攻击范围扩展
    - 旧实现: `CombatUpgradeManager.RangeExtensionSystem`

12. **PursuitModule** (`PURSUIT`)
    - 等级: Lv.1-5
    - 功能: 追击系统（对逃跑敌人造成额外伤害）
    - 旧实现: `CombatUpgradeManager.PursuitSystem`

### 能量类模块 (5)
来源: `upgrades/energy/EnergyUpgradeManager.java`

13. **EnergyEfficiencyModule** (`ENERGY_EFFICIENCY`)
    - 等级: Lv.1-5
    - 功能: 降低能量消耗
    - 旧实现: `EnergyEfficiencyManager.java`

14. **KineticGeneratorModule** (`KINETIC_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 动能发电（移动/跳跃/挖掘产能）
    - 旧实现: `EnergyUpgradeManager.KineticGeneratorSystem`

15. **SolarGeneratorModule** (`SOLAR_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 太阳能发电
    - 旧实现: `EnergyUpgradeManager.SolarGeneratorSystem`

16. **VoidEnergyModule** (`VOID_ENERGY`)
    - 等级: Lv.1-5
    - 功能: 虚空能量（低电量时从虚空吸能）
    - 旧实现: `EnergyUpgradeManager.VoidEnergySystem`

17. **CombatChargerModule** (`COMBAT_CHARGER`)
    - 等级: Lv.1-5
    - 功能: 战斗充能（击杀敌人恢复能量）
    - 旧实现: `EnergyUpgradeManager.CombatChargerSystem`

### 辅助类模块 (4)
来源: `upgrades/auxiliary/AuxiliaryUpgradeManager.java`

18. **OreVisionModule** (`ORE_VISION`)
    - 等级: Lv.1-5
    - 功能: 矿物透视（高亮显示矿石）
    - 旧实现: `AuxiliaryUpgradeManager.OreVisionSystem`

19. **MovementSpeedModule** (`MOVEMENT_SPEED`)
    - 等级: Lv.1-5
    - 功能: 移动速度提升
    - 旧实现: `AuxiliaryUpgradeManager.MovementSpeedSystem`

20. **StealthModule** (`STEALTH`)
    - 等级: Lv.1-5
    - 功能: 隐身系统（降低敌对生物检测范围）
    - 旧实现: `AuxiliaryUpgradeManager.StealthSystem`

21. **ExpAmplifierModule** (`EXP_AMPLIFIER`)
    - 等级: Lv.1-5
    - 功能: 经验放大器（增加经验获取）
    - 旧实现: `AuxiliaryUpgradeManager.ExpAmplifierSystem`

### 特殊模块 (4)
来源: `ItemMechanicalCore.UpgradeType` 和其他

22. **WaterproofModule** (`WATERPROOF_MODULE`)
    - 等级: Lv.1-3
    - 功能: 防水系统（保护核心免受水损害）
    - 旧实现: `WetnessSystem.java`

23. **TemperatureControlModule** (`TEMPERATURE_CONTROL`)
    - 等级: Lv.1-5
    - 功能: 温度调节（抗寒/抗热）
    - 旧实现: 可能在 SurvivalUpgradeManager 中

24. **SpeedBoostModule** (`SPEED_BOOST`)
    - 等级: Lv.1-5
    - 功能: 速度提升（可能和 MOVEMENT_SPEED 重复）
    - 旧实现: ItemMechanicalCore.UpgradeType

### 扩展模块 (2)
来源: `upgrades/module/`

25. **MagicAbsorbModule** (`MAGIC_ABSORB`)
    - 等级: Lv.1-5
    - 功能: 魔法吸收（吸收魔法伤害转化为能量）
    - 旧实现: `upgrades/module/MagicAbsorbModule.java`

26. **NeuralSynchronizerModule** (`NEURAL_SYNCHRONIZER`)
    - 等级: Lv.1-5
    - 功能: 神经同步器（提升反应速度/降低冷却）
    - 旧实现: `upgrades/module/NeuralSynchronizerModule.java`

### 热能发电（可能存在）(1)

27. **ThermalGeneratorModule** (`THERMAL_GENERATOR`)
    - 等级: Lv.1-5
    - 功能: 热能发电（在岩浆附近产能）
    - 旧实现: 可能在 EnergyUpgradeManager 中

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
