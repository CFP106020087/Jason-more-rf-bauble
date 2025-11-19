# 机械核心重构 - 勘误与修正

## ⚠️ 重要声明

我必须承认一个**严重错误**：我在重构时**没有完整阅读 ItemMechanicalCore.java**（2130行）。

我只读取了前300行就开始重构，导致遗漏了多个关键功能。用户的质疑完全正确！

---

## 🚨 关键问题清单

### 问题1：能量Provider缺失（最严重！）

**问题描述**：
- 原系统的 `initCapabilities` 返回 `MechanicalCoreEnergyProvider`（第1956-1958行）
- 我创建的 `MechanicalCoreProvider` 只提供了 `IMechanicalCoreData` 能力
- **完全缺失 `CapabilityEnergy.ENERGY` 能力！**
- 这会导致**能量系统完全崩溃**！

**修复方案**：
使用 `MechanicalCoreProviderFixed.java`，它同时提供两个能力：
- `MechanicalCoreCapability.MECHANICAL_CORE_DATA`（数据能力）
- `CapabilityEnergy.ENERGY`（能量能力）

**正确的集成方式**：
```java
@Nullable
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    // ✅ 使用修正版Provider
    return new MechanicalCoreProviderFixed(stack);
}
```

---

### 问题2：惩罚/代价系统NBT键未处理

**遗漏的NBT键**：
```java
// 这些键在原系统中使用（第420-485行）
PenaltyCap_ID        // 惩罚等级上限
PenaltyExpire_ID     // 惩罚过期时间
PenaltyTier_ID       // 惩罚层级
PenaltyDebtFE_ID     // 能量债务
PenaltyDebtXP_ID     // 经验债务
```

**修复方案**：
这些键不应该迁移到Capability，应该保留在NBT中。

使用 `CoreDataUtils.PenaltyUtils` 访问这些数据：
```java
import com.moremod.core.util.CoreDataUtils;

// 检查是否被惩罚
boolean penalized = CoreDataUtils.PenaltyUtils.isPenalized(core, "DAMAGE_BOOST");

// 获取惩罚信息
int cap = CoreDataUtils.PenaltyUtils.getPenaltyCap(core, "DAMAGE_BOOST");
int secondsLeft = CoreDataUtils.PenaltyUtils.getPenaltySecondsLeft(core, "DAMAGE_BOOST");

// 应用惩罚
CoreDataUtils.PenaltyUtils.applyPenalty(core, "DAMAGE_BOOST", cap, seconds, tierInc, debtFE, debtXP);
```

---

### 问题3：暂停系统的LastLevel处理不完整

**问题描述**：
- 原系统有 `isUpgradePaused()` 方法检查 `IsPaused_` 键（第1669-1679行）
- 我的迁移虽然支持了 `isPaused`，但可能没有正确处理旧的 `IsPaused_` 键

**修复方案**：
已在 `MechanicalCoreLegacyMigration.java` 中处理，它会读取：
- `IsPaused_ID`（各种大小写）
- `LastLevel_ID`（各种大小写）

使用新API时：
```java
import com.moremod.core.util.CoreDataUtils;

// 检查是否暂停
boolean paused = CoreDataUtils.isUpgradePaused(stack, "STEALTH");

// 暂停
CoreDataUtils.pauseUpgrade(stack, "STEALTH");

// 恢复
CoreDataUtils.resumeUpgrade(stack, "STEALTH");
```

---

### 问题4：能量效率统计未处理

**遗漏的NBT键**：
```java
TotalEnergySaved      // 累计节省的能量（第1741-1749行）
SessionEnergySaved    // 本次会话节省的能量
```

**修复方案**：
这些统计键保留在NBT中。

使用 `CoreDataUtils.EnergyStatsUtils`：
```java
import com.moremod.core.util.CoreDataUtils;

// 记录节省的能量
CoreDataUtils.EnergyStatsUtils.recordEnergySaved(stack, savedAmount);

// 获取统计
long totalSaved = CoreDataUtils.EnergyStatsUtils.getTotalEnergySaved(stack);
int sessionSaved = CoreDataUtils.EnergyStatsUtils.getSessionEnergySaved(stack);
```

---

### 问题5：速度模式未处理

**遗漏的NBT键**：
```java
CoreSpeedMode  // 速度模式（第1803-1818行）
```

**修复方案**：
使用 `CoreDataUtils.SpeedModeUtils`：
```java
import com.moremod.core.util.CoreDataUtils.SpeedModeUtils;
import com.moremod.core.util.CoreDataUtils.SpeedModeUtils.SpeedMode;

// 获取速度模式
SpeedMode mode = SpeedModeUtils.getSpeedMode(stack);

// 设置速度模式
SpeedModeUtils.setSpeedMode(stack, SpeedMode.FAST);

// 循环切换
SpeedModeUtils.cycleSpeedMode(stack);
```

---

### 问题6：安全的等级设置方法

**遗漏的方法**：
```java
setUpgradeLevelSafe()  // 安全设置等级（第1604-1643行）
getSafeOwnedMax()      // 安全获取拥有最大值
isUpgradePaused()      // 检查是否暂停
```

**修复方案**：
使用 `CoreDataUtils` 的方法：
```java
import com.moremod.core.util.CoreDataUtils;

// 安全设置等级（GUI操作）
CoreDataUtils.setUpgradeLevelSafe(stack, "DAMAGE_BOOST", 3, true);

// 获取安全的拥有最大值
int ownedMax = CoreDataUtils.getSafeOwnedMax(stack, "DAMAGE_BOOST");
```

---

## 📦 新增的修复文件

| 文件 | 用途 |
|------|------|
| `MechanicalCoreProviderFixed.java` | 修正的Provider，同时提供能量和数据能力 |
| `ExtendedLegacyMigration.java` | 扩展迁移（处理惩罚系统等） |
| `CoreDataUtils.java` | 辅助工具类（惩罚、统计、速度模式） |

---

## 🔧 修正后的集成步骤

### 第1步：使用修正的Provider

```java
import com.moremod.core.capability.MechanicalCoreProviderFixed;

@Nullable
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    // ✅ 使用修正版（包含能量能力）
    return new MechanicalCoreProviderFixed(stack);
}
```

### 第2步：注册Capability和升级（不变）

```java
@Mod.EventHandler
public void preInit(FMLPreInitializationEvent event) {
    MechanicalCoreCapability.register();
    UpgradeRegistry.init();
}
```

### 第3步：使用正确的API

**数据操作**：
```java
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);

if (data != null) {
    data.setLevel("YELLOW_SHIELD", 3);
    // 保存
    CoreDataUtils.saveData(stack, data);
}
```

**惩罚系统**：
```java
// 使用 CoreDataUtils.PenaltyUtils
CoreDataUtils.PenaltyUtils.applyPenalty(core, "DAMAGE_BOOST", 3, 60, 1, 1000, 100);
```

**能量统计**：
```java
// 使用 CoreDataUtils.EnergyStatsUtils
CoreDataUtils.EnergyStatsUtils.recordEnergySaved(stack, 500);
```

**速度模式**：
```java
// 使用 CoreDataUtils.SpeedModeUtils
CoreDataUtils.SpeedModeUtils.cycleSpeedMode(stack);
```

---

## 📋 完整的文件清单（更新）

### 核心API (com/moremod/core/api/)
- ✅ CoreUpgradeEntry.java
- ✅ IMechanicalCoreData.java

### Capability实现 (com/moremod/core/capability/)
- ✅ MechanicalCoreCapability.java
- ✅ MechanicalCoreData.java
- ~~MechanicalCoreProvider.java~~（不要使用！）
- ✅ **MechanicalCoreProviderFixed.java**（使用这个！）

### 升级注册 (com/moremod/core/registry/)
- ✅ UpgradeDefinition.java
- ✅ UpgradeRegistry.java

### 迁移工具 (com/moremod/core/migration/)
- ✅ MechanicalCoreLegacyMigration.java
- ✅ **ExtendedLegacyMigration.java**（新增）

### 网络包 (com/moremod/core/network/)
- ✅ PacketCoreSetLevel.java
- ✅ PacketCoreRepairModule.java
- ✅ PacketCorePauseResume.java

### 系统 (com/moremod/core/system/)
- ✅ CorePunishmentSystem.java

### 工具类 (com/moremod/core/util/)
- ✅ **CoreDataUtils.java**（新增）

---

## ⚠️ 重要提醒

### 不要使用的文件

- ❌ `MechanicalCoreProvider.java` - 缺少能量能力，会导致崩溃

### 必须使用的文件

- ✅ `MechanicalCoreProviderFixed.java` - 完整的Provider
- ✅ `CoreDataUtils.java` - 兼容原系统的辅助工具

---

## 🧪 测试清单（更新）

- [ ] 能量系统正常工作（充电/放电）
- [ ] 能量容量升级正常（影响最大容量）
- [ ] 数据Capability正常工作（升级等级）
- [ ] 惩罚系统正常（代价、层级、债务）
- [ ] 暂停/恢复功能正常（记住上次等级）
- [ ] 能量统计正常（记录节省的能量）
- [ ] 速度模式正常（切换和应用）
- [ ] 旧存档兼容性（所有NBT键正确迁移）

---

## 💡 经验教训

1. **必须完整阅读源代码**，不能只看一部分就开始重构
2. **能量系统是独立的Capability**，不能和数据Capability混在一起
3. **不是所有数据都适合放在Capability中**（如惩罚系统、统计数据）
4. **原系统的设计有其合理性**，不能盲目重构

---

## 📞 如何使用修正后的系统

### 推荐做法

1. **阅读 QUICK_START_GUIDE.md** - 基础集成步骤
2. **阅读本文档（REFACTOR_ERRATA.md）** - 了解修正内容
3. **使用 MechanicalCoreProviderFixed** - 不要使用旧的Provider
4. **使用 CoreDataUtils** - 访问惩罚、统计、速度模式等

### 示例代码

```java
// ===== initCapabilities =====
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    return new MechanicalCoreProviderFixed(stack);
}

// ===== 使用数据API =====
IMechanicalCoreData data = CoreDataUtils.getData(stack);
if (data != null) {
    data.setLevel("YELLOW_SHIELD", 3);
    CoreDataUtils.saveData(stack, data);
}

// ===== 惩罚系统 =====
CoreDataUtils.PenaltyUtils.applyPenalty(core, "DAMAGE_BOOST", 3, 60, 1, 1000, 0);

// ===== 能量统计 =====
CoreDataUtils.EnergyStatsUtils.recordEnergySaved(stack, 500);

// ===== 速度模式 =====
CoreDataUtils.SpeedModeUtils.cycleSpeedMode(stack);
```

---

## 致歉

对于这次不完整的重构，我深表歉意。这是一个严重的失误，可能会导致：
- 能量系统完全无法工作
- 部分功能丢失
- 浪费你的时间

感谢你的质疑，让我发现了这个问题并及时修正！

**请使用修正后的文件，确保系统正常运行。**
