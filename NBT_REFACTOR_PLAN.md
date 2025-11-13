# NBT 统一管理重构计划

## 项目信息
- **项目**: Jason-more-rf-bauble (Minecraft Mod)
- **重构目标**: 统一所有 NBT 键名管理，消除重复代码
- **核心工具类**: `com.moremod.util.UpgradeKeys`

---

## 一、当前问题总结

### 1.1 NBT 键名混乱问题

| 问题类型 | 严重程度 | 影响范围 |
|---------|---------|---------|
| 重复定义相同的键名常量 | ⚠️⚠️⚠️ 高 | 4个文件 |
| 大小写变体爆炸（每个键3个变体） | ⚠️⚠️⚠️ 高 | 所有文件 |
| NBT 读写代码重复 | ⚠️⚠️ 中 | 所有文件 |
| 键名类型分散（14种） | ⚠️⚠️ 中 | 10个文件+ |

### 1.2 发现的 NBT 键名类型（共14种）

```
✓ = 已在 UpgradeKeys 中定义
✗ = 之前缺失（已补充）
```

| 键名前缀 | 用途 | 状态 | 使用位置 |
|---------|------|------|---------|
| `upgrade_*` | 升级等级 | ✓ | 所有文件 |
| `HasUpgrade_*` | 是否拥有 | ✓ | 所有文件 |
| `OwnedMax_*` | 拥有的最大等级 | ✓ | 所有文件 |
| `OriginalMax_*` | 原始最大等级 | ✗→✓ | DeathHandler, PacketUpdate, SmartHandler, GUI |
| `LastLevel_*` | 上次等级 | ✓ | 所有文件 |
| `IsPaused_*` | 是否暂停 | ✓ | 所有文件 |
| `Disabled_*` | 手动禁用 | ✓ | 所有文件 |
| `WasPunished_*` | 是否被惩罚过 | ✗→✓ | DeathHandler, PacketUpdate, GUI |
| `UpgradeLock_*` | 升级锁定 | ✓ | UpgradeKeys |
| `Destroyed_*` | 模块破坏 | ✓ | UpgradeKeys |
| `DamageCount_*` | 损坏次数 | ✗→✓ | DeathHandler |
| `TotalDamageCount_*` | 总损坏次数 | ✗→✓ | GUI, DeathHandler |
| `Penalty*_*` | 惩罚系统数据 | ✗→✓ | ItemMechanicalCore, EnergyPunishment |
| `Punish_*` | 惩罚时间戳 | ✗→✓ | EnergyPunishment |

### 1.3 重复定义键名的文件

| 文件 | 重复定义的常量 | 行号 |
|------|---------------|------|
| **SoulboundDeathHandler.java** | `K_ORIGINAL_MAX`<br>`K_OWNED_MAX`<br>`K_DAMAGE_COUNT`<br>`K_WAS_PUNISHED` | 40-43 |
| **SmartUpgradeHandler.java** | `K_ORIGINAL_MAX`<br>`K_OWNED_MAX` | 32-33 |
| **EnergyPunishmentSystem.java** | `K_LAST_DOT`<br>`K_LAST_DEGRADE`<br>`K_LAST_DURABILITY`<br>`K_CRITICAL_SINCE` 等 | 57-64 |
| **PacketMechanicalCoreUpdate.java** | 硬编码键名字符串 | 多处 |

---

## 二、解决方案概述

### 2.1 核心策略

**统一使用 `UpgradeKeys` 工具类管理所有 NBT 操作**

### 2.2 已完成的工作 ✅

#### ✅ 完善 UpgradeKeys.java

**新增的键名方法：**

```java
// 修复系统相关
kOriginalMax(String cid)      // 原始最大等级
kWasPunished(String cid)       // 是否被惩罚过
kDamageCount(String cid)       // 损坏次数
kTotalDamageCount(String cid)  // 总损坏次数

// 惩罚系统相关
kPenaltyCap(String cid)        // 惩罚上限等级
kPenaltyExpire(String cid)     // 惩罚过期时间戳
kPenaltyTier(String cid)       // 惩罚层级
kPenaltyDebtFE(String cid)     // 惩罚能量债务
kPenaltyDebtXP(String cid)     // 惩罚经验债务

// 能量惩罚时间戳（全局常量）
K_LAST_DOT                     // 上次DoT伤害时间
K_LAST_DEGRADE                 // 上次降级时间
K_LAST_DURABILITY              // 上次耐久损耗时间
K_CRITICAL_SINCE               // 进入临界状态的时间戳
K_WARNING_10S                  // 10秒警告已触发标记
K_WARNING_5S                   // 5秒警告已触发标记
K_SELF_DESTRUCT_DONE           // 自毁已执行标记

// 能量状态标记（全局常量）
K_POWER_SAVING_MODE            // 省电模式标记
K_EMERGENCY_MODE               // 紧急模式标记
K_CRITICAL_MODE                // 临界模式标记
K_CORE_DESTROYED               // 核心自毁标记
K_PREVIOUS_ENERGY_STATUS       // 上一次能量状态
```

**新增的读取方法：**

```java
getOriginalMax(ItemStack, String)      // 获取原始最大等级
getDamageCount(ItemStack, String)      // 获取损坏次数
getTotalDamageCount(ItemStack, String) // 获取总损坏次数
wasPunished(ItemStack, String)         // 检查是否被惩罚过
```

**新增的写入方法：**

```java
setOriginalMax(ItemStack, String, int)     // 设置原始最大等级（只在更高时更新）
setOwnedMax(ItemStack, String, int)        // 设置拥有最大等级
markWasPunished(ItemStack, String, boolean) // 标记为被惩罚过
incrementDamageCount(ItemStack, String)     // 增加损坏次数
resetDamageCount(ItemStack, String)         // 重置损坏次数
```

---

## 三、逐文件重构计划

### 优先级说明

- **P0（最高优先级）**: 核心系统，影响所有其他文件
- **P1（高优先级）**: 直接业务逻辑，频繁使用
- **P2（中优先级）**: 事件处理和网络同步
- **P3（低优先级）**: 辅助功能和特定模块

---

### 3.1 P0 - 核心系统重构

#### 📄 ItemMechanicalCore.java (P0)

**文件路径**: `/com/moremod/item/ItemMechanicalCore.java`
**代码行数**: 2,129 行
**重构复杂度**: ⚠️⚠️⚠️⚠️ 非常高

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 | 数量 |
|---------|---------|---------|------|
| 424-464 | `"PenaltyExpire_" + id` | `UpgradeKeys.kPenaltyExpire(id)` | 7处 |
| 424-464 | `"PenaltyCap_" + id` | `UpgradeKeys.kPenaltyCap(id)` | 4处 |
| 424-464 | `"PenaltyTier_" + id` | `UpgradeKeys.kPenaltyTier(id)` | 4处 |
| 424-464 | `"PenaltyDebtFE_" + id` | `UpgradeKeys.kPenaltyDebtFE(id)` | 4处 |
| 424-464 | `"PenaltyDebtXP_" + id` | `UpgradeKeys.kPenaltyDebtXP(id)` | 4处 |
| 413, 497, 503, 533 | `"Disabled_" + k` | `UpgradeKeys.kDisabled(id)` | 4处 |
| 547 | `"upgrade_" + upgradeId` | `UpgradeKeys.kUpgrade(id)` | 1处 |
| 955, 964, 1414, 1440, 1566, 1594 | `"HasUpgrade_" + id` | `UpgradeKeys.kHasUpgrade(id)` | 6处 |
| 1610-1623 | `"OwnedMax_" + id` | `UpgradeKeys.kOwnedMax(id)` | 6处 |
| 1627-1631 | `"IsPaused_" + id` | `UpgradeKeys.kPaused(id)` | 4处 |
| 1547-1554 | `"upgrade_" + type.getKey()` | `UpgradeKeys.kUpgrade(id)` | 2处 |

##### 重构步骤：

1. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

2. **替换惩罚系统键名** (424-464行):
   - 将所有 `"PenaltyExpire_" + id` 替换为 `UpgradeKeys.kPenaltyExpire(id)`
   - 将所有 `"PenaltyCap_" + id` 替换为 `UpgradeKeys.kPenaltyCap(id)`
   - 将所有 `"PenaltyTier_" + id` 替换为 `UpgradeKeys.kPenaltyTier(id)`
   - 将所有 `"PenaltyDebtFE_" + id` 替换为 `UpgradeKeys.kPenaltyDebtFE(id)`
   - 将所有 `"PenaltyDebtXP_" + id` 替换为 `UpgradeKeys.kPenaltyDebtXP(id)`

3. **替换升级相关键名** (全文):
   - 使用 `UpgradeKeys.kUpgrade(id)` 替代 `"upgrade_" + id`
   - 使用 `UpgradeKeys.kHasUpgrade(id)` 替代 `"HasUpgrade_" + id`
   - 使用 `UpgradeKeys.kOwnedMax(id)` 替代 `"OwnedMax_" + id`
   - 使用 `UpgradeKeys.kPaused(id)` 替代 `"IsPaused_" + id`
   - 使用 `UpgradeKeys.kDisabled(id)` 替代 `"Disabled_" + id`

4. **简化大小写变体检查**:
   - 当前需要检查3个变体的地方，改用 `UpgradeKeys.getLevel()` 等方法

---

#### 📄 ItemMechanicalCoreExtended.java (P0)

**文件路径**: `/com/moremod/item/ItemMechanicalCoreExtended.java`
**代码行数**: 467 行
**重构复杂度**: ⚠️⚠️⚠️ 高

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 | 数量 |
|---------|---------|---------|------|
| 152 | `"Disabled_" + k` | `UpgradeKeys.kDisabled(id)` | 2处 |
| 152 | `"IsPaused_" + k` | `UpgradeKeys.kPaused(id)` | 2处 |
| 180-190 | `"upgrade_" + canon` | `UpgradeKeys.kUpgrade(id)` | 9处 |
| 208-209 | `"upgrade_" + canon` | `UpgradeKeys.kUpgrade(id)` | 1处 |
| 208-209 | `"HasUpgrade_" + canon` | `UpgradeKeys.kHasUpgrade(id)` | 1处 |
| 217 | `"Disabled_" + canon` | `UpgradeKeys.kDisabled(id)` | 1处 |
| 226-235 | `"Disabled_" + canon` | `UpgradeKeys.kDisabled(id)` | 6处 |
| 267 | `"HasUpgrade_" + canon` | `UpgradeKeys.kHasUpgrade(id)` | 1处 |
| 274 | `"upgrade_"` 前缀检查 | `UpgradeKeys.kUpgrade()` | 1处 |

##### 重构步骤：

1. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

2. **重构 getUpgradeLevel() 方法** (171-197行):
   ```java
   // 当前代码：检查3个变体
   level = Math.max(level, nbt.getInteger("upgrade_" + canon));
   level = Math.max(level, nbt.getInteger("upgrade_" + canon.toUpperCase()));
   level = Math.max(level, nbt.getInteger("upgrade_" + canon.toLowerCase()));

   // 改为：直接使用 UpgradeKeys
   return UpgradeKeys.getLevel(stack, upgradeId);
   ```

3. **重构 setUpgradeLevel() 方法** (200-210行):
   ```java
   // 当前代码：
   nbt.setInteger("upgrade_" + canon, level);
   if (level > 0) nbt.setBoolean("HasUpgrade_" + canon, true);

   // 改为：
   UpgradeKeys.setLevel(stack, upgradeId, level);
   if (level > 0) UpgradeKeys.markOwnedActive(stack, upgradeId, level);
   ```

4. **重构 isUpgradeDisabled() 方法** (226-240行):
   ```java
   // 当前代码：检查3个变体
   if (nbt.getBoolean("Disabled_" + canon)) return true;
   if (nbt.getBoolean("Disabled_" + canon.toUpperCase())) return true;
   if (nbt.getBoolean("Disabled_" + canon.toLowerCase())) return true;

   // 改为：
   return UpgradeKeys.isDisabled(stack, upgradeId);
   ```

---

### 3.2 P1 - GUI 和业务逻辑重构

#### 📄 MechanicalCoreGui.java (P1)

**文件路径**: `/com/moremod/client/gui/MechanicalCoreGui.java`
**代码行数**: 1,541 行
**重构复杂度**: ⚠️⚠️⚠️⚠️ 非常高

##### 需要修改的位置：

| 方法名 | 行号范围 | 当前问题 | 重构方案 |
|-------|---------|---------|---------|
| `readOriginalMaxFromNBT()` | 92-120 | 60行重复代码<br>检查3个变体 | 改用 `UpgradeKeys.getOriginalMax(stack, id)` |
| `getOwnedMaxFromNBT()` | 490-495 | 检查3个变体 | 改用 `UpgradeKeys.getOwnedMax(stack, id)` |
| `getLastLevelFromNBT()` | 497-502 | 检查3个变体 | 改用 `UpgradeKeys.getLastLevel(stack, id)` |
| `getUpgradeStatus()` | 212-262 | 硬编码键名 | 使用 `UpgradeKeys.getStatus()` + 补充逻辑 |
| `initializeUpgradeData()` | 302-488 | 大量硬编码键名 | 使用 UpgradeKeys 方法 |
| `updateUpgradeStates()` | 504-543 | 硬编码键名 | 使用 UpgradeKeys 方法 |
| `setLevelEverywhere()` | 1360-1420 | 防水模块特殊处理<br>60行复杂代码 | 简化为 UpgradeKeys 调用 |
| `writePauseMeta()` | 1422-1465 | 硬编码键名 | 使用 UpgradeKeys.pause() |
| `adjustUpgradeLevel()` | 1176-1356 | 多处硬编码键名 | 使用 UpgradeKeys 方法 |
| `calculateRepairCost()` | 1118-1148 | `"TotalDamageCount_"` 硬编码 | 改用 `UpgradeKeys.getTotalDamageCount()` |

##### 重构步骤：

1. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

2. **删除重复方法** (92-120, 490-502行):
   ```java
   // 删除这些方法：
   // - readOriginalMaxFromNBT()
   // - getOwnedMaxFromNBT()
   // - getLastLevelFromNBT()

   // 所有调用处改为：
   UpgradeKeys.getOriginalMax(stack, id)
   UpgradeKeys.getOwnedMax(stack, id)
   UpgradeKeys.getLastLevel(stack, id)
   ```

3. **简化 getUpgradeStatus() 方法** (212-262行):
   ```java
   // 当前代码：
   boolean wasPunished = nbt.getBoolean("WasPunished_" + id) ||
       nbt.getBoolean("WasPunished_" + up(id)) ||
       nbt.getBoolean("WasPunished_" + lo(id));

   // 改为：
   boolean wasPunished = UpgradeKeys.wasPunished(core, id);
   ```

4. **重构 initializeUpgradeData() 方法** (302-488行):
   - 使用 `UpgradeKeys.getOriginalMax()` 替代自定义读取
   - 使用 `UpgradeKeys.setOriginalMax()` 替代手动写入
   - 使用 `UpgradeKeys.wasPunished()` 检查惩罚状态
   - 使用 `UpgradeKeys.getDamageCount()` 获取损坏次数

5. **大幅简化 setLevelEverywhere() 方法** (1360-1420行):
   ```java
   // 当前代码：60行，需要处理防水模块的所有别名和变体
   // 改为：
   private void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel) {
       if (core == null || core.isEmpty()) return;

       // 使用 UpgradeKeys 统一写入
       UpgradeKeys.setLevel(core, upgradeId, newLevel);
       if (newLevel > 0) {
           UpgradeKeys.markOwnedActive(core, upgradeId, newLevel);
       }

       // 同步到旧系统（ItemMechanicalCore 和 Extended）
       syncToLegacySystems(core, upgradeId, newLevel);
   }
   ```

6. **简化 writePauseMeta() 方法** (1422-1465行):
   ```java
   // 当前代码：44行
   // 改为：
   private void writePauseMeta(ItemStack core, String upgradeId, int lastLevel, boolean paused) {
       if (paused) {
           UpgradeKeys.pause(core, upgradeId, lastLevel);
       } else {
           UpgradeKeys.markOwnedActive(core, upgradeId, lastLevel);
       }
   }
   ```

7. **简化 calculateRepairCost() 方法** (1118-1148行):
   ```java
   // 当前代码：
   int totalDamageCount = Math.max(
       nbt.getInteger("TotalDamageCount_" + entry.id),
       Math.max(
           nbt.getInteger("TotalDamageCount_" + up(entry.id)),
           nbt.getInteger("TotalDamageCount_" + lo(entry.id))
       )
   );

   // 改为：
   int totalDamageCount = UpgradeKeys.getTotalDamageCount(coreStack, entry.id);
   ```

##### 预期效果：

- **减少代码行数**: ~200 行（从 1,541 → ~1,340）
- **消除重复**: 删除 3 个重复的 NBT 读取方法
- **提高可读性**: 复杂的变体检查逻辑全部封装到 UpgradeKeys

---

### 3.3 P2 - 事件处理重构

#### 📄 SoulboundDeathHandler.java (P2)

**文件路径**: `/com/moremod/event/SoulboundDeathHandler.java`
**代码行数**: ~400 行（预估）
**重构复杂度**: ⚠️⚠️ 中

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 |
|---------|---------|---------|
| 40-43 | 定义了 4 个常量 | **删除**，使用 UpgradeKeys |
| 232-270 | 手动写入 `OriginalMax_` | `UpgradeKeys.setOriginalMax()` |
| 232-270 | 手动写入 `WasPunished_` | `UpgradeKeys.markWasPunished()` |
| 262-270 | 手动写入 `DamageCount_` | `UpgradeKeys.incrementDamageCount()` |
| 268-270 | 手动写入 `TotalDamageCount_` | （已包含在 incrementDamageCount 中） |

##### 重构步骤：

1. **删除重复的常量定义** (40-43行):
   ```java
   // 删除这些行：
   // private static final String K_ORIGINAL_MAX = "OriginalMax_";
   // private static final String K_OWNED_MAX = "OwnedMax_";
   // private static final String K_DAMAGE_COUNT = "DamageCount_";
   // private static final String K_WAS_PUNISHED = "WasPunished_";
   ```

2. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

3. **重构惩罚记录逻辑** (232-270行):
   ```java
   // 当前代码：手动写入所有变体
   nbt.setInteger(K_ORIGINAL_MAX + upperId, currentOwnedMax);
   nbt.setInteger(K_ORIGINAL_MAX + target, currentOwnedMax);
   nbt.setInteger(K_ORIGINAL_MAX + lowerId, currentOwnedMax);
   // ... 防水模块的特殊处理
   nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
   // ...
   int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
   nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + 1);
   // ...

   // 改为：
   UpgradeKeys.setOriginalMax(core, target, currentOwnedMax);
   UpgradeKeys.markWasPunished(core, target, true);
   UpgradeKeys.incrementDamageCount(core, target);
   ```

##### 预期效果：

- **减少代码行数**: ~40 行
- **消除重复**: 删除 4 个重复的常量定义
- **简化逻辑**: 不再需要手动处理大小写变体和防水模块别名

---

#### 📄 SmartUpgradeHandler.java (P2)

**文件路径**: `/com/moremod/eventHandler/SmartUpgradeHandler.java`
**代码行数**: ~600 行（预估）
**重构复杂度**: ⚠️⚠️ 中

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 |
|---------|---------|---------|
| 32-33 | 定义了 2 个常量 | **删除**，使用 UpgradeKeys |
| 80-120 | `recordOriginalMax()` 方法 | 改用 `UpgradeKeys.setOriginalMax()` |

##### 重构步骤：

1. **删除重复的常量定义** (32-33行):
   ```java
   // 删除这些行：
   // private static final String K_ORIGINAL_MAX = "OriginalMax_";
   // private static final String K_OWNED_MAX = "OwnedMax_";
   ```

2. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

3. **简化 recordOriginalMax() 方法** (80-120行):
   ```java
   // 当前代码：40行，手动检查和写入所有变体
   private void recordOriginalMax(ItemStack coreStack, String upgradeId, int newLevel) {
       NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
       String upperId = upgradeId.toUpperCase();
       String lowerId = upgradeId.toLowerCase();
       String[] variants = {upgradeId, upperId, lowerId};

       int currentOriginalMax = 0;
       for (String variant : variants) {
           int val = nbt.getInteger(K_ORIGINAL_MAX + variant);
           currentOriginalMax = Math.max(currentOriginalMax, val);
       }

       if (newLevel > currentOriginalMax) {
           // ...
           nbt.setInteger(K_ORIGINAL_MAX + upgradeId, newLevel);
           nbt.setInteger(K_ORIGINAL_MAX + upperId, newLevel);
           nbt.setInteger(K_ORIGINAL_MAX + lowerId, newLevel);
       }
       // ...
   }

   // 改为：只需2行
   private void recordOriginalMax(ItemStack coreStack, String upgradeId, int newLevel) {
       UpgradeKeys.setOriginalMax(coreStack, upgradeId, newLevel);
   }
   ```

##### 预期效果：

- **减少代码行数**: ~38 行
- **消除重复**: 删除 2 个重复的常量定义
- **简化逻辑**: recordOriginalMax 从 40 行减少到 2 行

---

#### 📄 PacketMechanicalCoreUpdate.java (P2)

**文件路径**: `/com/moremod/network/PacketMechanicalCoreUpdate.java`
**代码行数**: ~400 行（预估）
**重构复杂度**: ⚠️⚠️⚠️ 高

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 |
|---------|---------|---------|
| 177-179 | 硬编码 `"OriginalMax_"` | `UpgradeKeys.setOriginalMax()` |
| 206-207 | 硬编码 `"IsPaused_"` 和 `"LastLevel_"` | `UpgradeKeys.isPaused()` 和 `getLastLevel()` |
| 228-230 | 硬编码 `"upgrade_"` | `UpgradeKeys.getLevel()` |
| 244-281 | 手动写入暂停标记 | `UpgradeKeys.pause()` / `markOwnedActive()` |
| 302-304 | 硬编码 `"WasPunished_"` | `UpgradeKeys.wasPunished()` |
| 340 | 硬编码 `"OwnedMax_"` | `UpgradeKeys.setOwnedMax()` |

##### 重构步骤：

1. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

2. **重构 SET_LEVEL 动作处理**:
   ```java
   // 当前代码：手动读写多个键
   lv = Math.max(lv, nbt.getInteger(K_UPGRADE + id));
   lv = Math.max(lv, nbt.getInteger(K_UPGRADE + up(id)));
   lv = Math.max(lv, nbt.getInteger(K_UPGRADE + lo(id)));

   // 改为：
   int currentLevel = UpgradeKeys.getLevel(core, id);
   ```

3. **重构 REPAIR_UPGRADE 动作处理**:
   ```java
   // 当前代码：手动检查和写入
   boolean wasPunished = nbt.getBoolean(K_WAS_PUNISHED + upperId) ||
       nbt.getBoolean(K_WAS_PUNISHED + upgradeId) ||
       nbt.getBoolean(K_WAS_PUNISHED + lowerId);
   // ...
   nbt.setInteger(K_OWNED_MAX + upgradeId, targetLevel);

   // 改为：
   boolean wasPunished = UpgradeKeys.wasPunished(core, upgradeId);
   // ...
   UpgradeKeys.setOwnedMax(core, upgradeId, targetLevel);
   ```

##### 预期效果：

- **减少代码行数**: ~50 行
- **提高可读性**: 消除大小写变体检查的重复代码

---

#### 📄 EnergyPunishmentSystem.java (P2)

**文件路径**: `/com/moremod/event/EnergyPunishmentSystem.java`
**代码行数**: ~300 行（预估）
**重构复杂度**: ⚠️⚠️ 中

##### 需要修改的位置：

| 行号范围 | 当前代码 | 需要改为 |
|---------|---------|---------|
| 57-64 | 定义了 7 个常量 | **删除**，使用 UpgradeKeys 常量 |
| 112-124 | 手动读写时间戳键 | 使用 `UpgradeKeys.K_*` 常量 |

##### 重构步骤：

1. **删除重复的常量定义** (57-64行):
   ```java
   // 删除这些行：
   // private static final String K_LAST_DOT = "Punish_LastDot";
   // private static final String K_LAST_DEGRADE = "Punish_LastDegrade";
   // private static final String K_LAST_DURABILITY = "Punish_LastDur";
   // private static final String K_CRITICAL_SINCE = "Punish_CriticalSince";
   // private static final String K_WARNING_10S = "Punish_Warning10s";
   // private static final String K_WARNING_5S = "Punish_Warning5s";
   // private static final String K_SELF_DESTRUCT_DONE = "Punish_SelfDestruct";
   ```

2. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

3. **替换所有常量引用**:
   ```java
   // 当前代码：
   if (!nbt.hasKey(K_CRITICAL_SINCE)) {
       nbt.setLong(K_CRITICAL_SINCE, time);
       // ...
   }

   // 改为：
   if (!nbt.hasKey(UpgradeKeys.K_CRITICAL_SINCE)) {
       nbt.setLong(UpgradeKeys.K_CRITICAL_SINCE, time);
       // ...
   }
   ```

##### 预期效果：

- **减少代码行数**: ~8 行（删除常量定义）
- **统一管理**: 所有时间戳键名集中在 UpgradeKeys

---

### 3.4 P3 - 辅助功能重构

#### 📄 upgrades 包下的文件 (P3)

**涉及文件**:
- `WaterproofUpgrade.java`
- `MechanicalCoreFlightHandler.java`
- `EnergyEfficiencyManager.java`
- `EnergyDepletionManager.java`
- 其他 upgrades 包下的文件

##### 通用重构步骤：

1. **添加 import**:
   ```java
   import com.moremod.util.UpgradeKeys;
   ```

2. **替换所有硬编码键名**:
   - `"upgrade_*"` → `UpgradeKeys.kUpgrade(id)`
   - `"HasUpgrade_*"` → `UpgradeKeys.kHasUpgrade(id)`
   - `"Disabled_*"` → `UpgradeKeys.kDisabled(id)`
   - 等等

3. **使用 UpgradeKeys 读写方法**:
   - 读取等级：`UpgradeKeys.getLevel(stack, id)`
   - 设置等级：`UpgradeKeys.setLevel(stack, id, level)`
   - 检查禁用：`UpgradeKeys.isDisabled(stack, id)`

---

## 四、重构顺序和时间估算

### 4.1 推荐顺序

```
阶段1（基础）:
  ✅ 完善 UpgradeKeys.java（已完成）

阶段2（核心）:
  → SmartUpgradeHandler.java（简单，先做练手）
  → SoulboundDeathHandler.java
  → EnergyPunishmentSystem.java
  → PacketMechanicalCoreUpdate.java

阶段3（复杂）:
  → ItemMechanicalCoreExtended.java
  → ItemMechanicalCore.java
  → MechanicalCoreGui.java

阶段4（收尾）:
  → upgrades 包下的所有文件
```

### 4.2 工作量估算

| 阶段 | 文件数 | 预计时间 | 难度 |
|------|-------|---------|------|
| ✅ 阶段1 | 1 | **已完成** | ⚠️⚠️ |
| 阶段2 | 4 | 2-3 小时 | ⚠️⚠️ |
| 阶段3 | 3 | 4-6 小时 | ⚠️⚠️⚠️⚠️ |
| 阶段4 | 10+ | 3-4 小时 | ⚠️ |
| **总计** | **18+** | **10-14 小时** | - |

---

## 五、重构前后对比示例

### 示例1：读取 OriginalMax

**重构前（GUI 代码，60行）**:
```java
private int readOriginalMaxFromNBT(NBTTagCompound nbt, String id) {
    if (nbt == null) return 0;

    int originalMax = Math.max(
        nbt.getInteger("OriginalMax_" + id),
        Math.max(
            nbt.getInteger("OriginalMax_" + up(id)),
            nbt.getInteger("OriginalMax_" + lo(id))
        )
    );

    if (originalMax <= 0 && isWaterproofUpgrade(id)) {
        for (String wid : WATERPROOF_IDS) {
            originalMax = Math.max(originalMax,
                Math.max(
                    nbt.getInteger("OriginalMax_" + wid),
                    Math.max(
                        nbt.getInteger("OriginalMax_" + up(wid)),
                        nbt.getInteger("OriginalMax_" + lo(wid))
                    )
                )
            );
        }
    }

    return originalMax;
}
```

**重构后（1行）**:
```java
int originalMax = UpgradeKeys.getOriginalMax(coreStack, id);
```

---

### 示例2：设置惩罚标记

**重构前（DeathHandler，15行）**:
```java
String upperId = upgradeId.toUpperCase(Locale.ROOT);
String lowerId = upgradeId.toLowerCase(Locale.ROOT);

nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
nbt.setBoolean(K_WAS_PUNISHED + target, true);
nbt.setBoolean(K_WAS_PUNISHED + lowerId, true);

if (isWaterproofUpgrade(target)) {
    for (String wid : WATERPROOF_IDS) {
        nbt.setBoolean(K_WAS_PUNISHED + wid, true);
        nbt.setBoolean(K_WAS_PUNISHED + wid.toUpperCase(Locale.ROOT), true);
        nbt.setBoolean(K_WAS_PUNISHED + wid.toLowerCase(Locale.ROOT), true);
    }
}
```

**重构后（1行）**:
```java
UpgradeKeys.markWasPunished(core, upgradeId, true);
```

---

### 示例3：增加损坏次数

**重构前（DeathHandler，10行）**:
```java
int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + 1);
nbt.setInteger(K_DAMAGE_COUNT + target, damageCount + 1);
nbt.setInteger(K_DAMAGE_COUNT + lowerId, damageCount + 1);

int totalDamageCount = nbt.getInteger("TotalDamageCount_" + upperId);
nbt.setInteger("TotalDamageCount_" + upperId, totalDamageCount + 1);
nbt.setInteger("TotalDamageCount_" + target, totalDamageCount + 1);
nbt.setInteger("TotalDamageCount_" + lowerId, totalDamageCount + 1);
```

**重构后（1行）**:
```java
UpgradeKeys.incrementDamageCount(core, upgradeId);
```

---

## 六、重构收益总结

### 6.1 代码质量提升

| 指标 | 重构前 | 重构后 | 改善 |
|------|-------|-------|------|
| **代码行数** | ~5,000 | ~4,200 | ↓ 16% |
| **重复定义** | 4处×多个常量 | 0 | ✅ 完全消除 |
| **硬编码键名** | 300+ 处 | 0 | ✅ 完全消除 |
| **大小写变体** | 每处3个变体 | 统一处理 | ✅ 完全封装 |

### 6.2 维护性提升

- ✅ **统一入口**: 所有 NBT 操作通过 UpgradeKeys
- ✅ **易于扩展**: 新增键名只需修改 UpgradeKeys
- ✅ **减少错误**: 消除键名拼写错误的可能
- ✅ **向后兼容**: UpgradeKeys 内部处理旧键名兼容

### 6.3 性能提升

- ⚠️ **无明显性能损失**: UpgradeKeys 方法都是简单的封装
- ✅ **可能的优化空间**: 未来可以在 UpgradeKeys 中添加缓存

---

## 七、风险和注意事项

### 7.1 重构风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 破坏现有功能 | ⚠️⚠️⚠️ 高 | 每个文件重构后立即测试 |
| 遗漏某些键名 | ⚠️⚠️ 中 | 使用 grep 全局搜索验证 |
| 兼容性问题 | ⚠️ 低 | UpgradeKeys 已支持旧键名兼容 |

### 7.2 测试计划

**每个文件重构后需要测试：**

1. ✅ **编译测试**: 确保没有编译错误
2. ✅ **功能测试**:
   - 升级系统正常工作
   - 惩罚系统正常工作
   - GUI 显示正确
   - 死亡掉落正常
3. ✅ **兼容性测试**:
   - 读取旧存档数据正常
   - 新旧键名共存时正常工作

### 7.3 回滚方案

- ✅ **使用 Git 分支**: 每个阶段在独立分支开发
- ✅ **保留旧代码**: 注释掉而非删除，便于回滚
- ✅ **分阶段提交**: 每个文件独立提交，便于定位问题

---

## 八、开始重构

### 当前状态

✅ **阶段1 完成**: UpgradeKeys.java 已完善

### 下一步行动

📌 **建议从阶段2开始**:

1. **SmartUpgradeHandler.java** - 最简单，可以练手
2. **SoulboundDeathHandler.java** - 中等难度
3. **EnergyPunishmentSystem.java** - 中等难度
4. **PacketMechanicalCoreUpdate.java** - 较难

### 需要确认

- [ ] 是否立即开始阶段2？
- [ ] 是否需要先进行全面测试？
- [ ] 是否需要调整重构顺序？

---

**文档版本**: v1.0
**创建日期**: 2025-11-13
**状态**: ✅ 阶段1完成，等待开始阶段2
