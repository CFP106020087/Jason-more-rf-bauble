# GUI 迁移指南

**目标**: 将现有 GUI 从直接访问 NBT 迁移到使用 ViewModel

**收益**:
- ✅ 业务逻辑与 UI 分离
- ✅ 数据格式化统一
- ✅ 代码可测试性提高
- ✅ 减少重复代码

---

## 📊 迁移对比

### Before: 直接访问 NBT
```java
// 旧方式 - 直接读取 NBT
ItemStack core = findEquippedCore(player);
NBTTagCompound nbt = core.getTagCompound();

int energy = 0;
if (nbt != null) {
    IEnergyStorage storage = core.getCapability(CapabilityEnergy.ENERGY, null);
    if (storage != null) {
        energy = storage.getEnergyStored();
    }
}

// 手动格式化
String energyText;
if (energy >= 1_000_000) {
    energyText = String.format("%.1fM", energy / 1_000_000.0);
} else if (energy >= 1_000) {
    energyText = String.format("%.1fk", energy / 1_000.0);
} else {
    energyText = String.valueOf(energy);
}

// 手动选择颜色
TextFormatting color;
float percentage = (float) energy / maxEnergy;
if (percentage >= 0.7f) {
    color = TextFormatting.GREEN;
} else if (percentage >= 0.3f) {
    color = TextFormatting.YELLOW;
} else {
    color = TextFormatting.RED;
}

drawString(color + energyText, x, y);
```

### After: 使用 ViewModel
```java
// 新方式 - 使用 ViewModel
MechCoreViewModel viewModel = new MechCoreViewModel(player);

// 获取格式化的数据（自动格式化 + 自动颜色）
String energyText = viewModel.getEnergyText();
TextFormatting color = viewModel.getEnergyColor();

drawString(color + energyText, x, y);
```

**代码减少**: 从 30+ 行减少到 3 行！

---

## 🔄 迁移步骤

### 步骤 1: 初始化 ViewModel

在 GUI 构造器中创建 ViewModel：

```java
public class MechanicalCoreGui extends GuiScreen {
    private final MechCoreViewModel viewModel;

    public MechanicalCoreGui(EntityPlayer player) {
        // 创建 ViewModel
        this.viewModel = new MechCoreViewModel(player);
    }
}
```

### 步骤 2: 替换能量相关代码

**旧代码**:
```java
// 查找核心
ItemStack core = findEquippedCore(player);
IEnergyStorage storage = core.getCapability(CapabilityEnergy.ENERGY, null);
int energy = storage.getEnergyStored();
int maxEnergy = storage.getMaxEnergyStored();

// 格式化
String text = formatEnergy(energy) + " / " + formatEnergy(maxEnergy);
```

**新代码**:
```java
// 一行搞定
String text = viewModel.getEnergyText();
```

### 步骤 3: 替换模块列表代码

**旧代码**:
```java
// 读取所有模块
List<String> moduleIds = getAllInstalledModules(core);

for (String moduleId : moduleIds) {
    // 读取等级
    int level = getUpgradeLevel(core, moduleId);
    int maxLevel = getMaxLevel(moduleId);

    // 读取状态
    boolean active = isUpgradeActive(core, moduleId);

    // 获取显示名称
    String displayName = getDisplayName(moduleId);

    // 格式化文本
    String text = displayName + " Lv." + level + "/" + maxLevel;

    // 选择颜色
    TextFormatting color;
    if (!active) {
        color = TextFormatting.GRAY;
    } else if (level >= maxLevel) {
        color = TextFormatting.GOLD;
    } else {
        color = TextFormatting.GREEN;
    }

    drawString(color + text, x, y);
}
```

**新代码**:
```java
// 获取所有模块
List<MechCoreViewModel.ModuleInfo> modules = viewModel.getAllModules();

for (MechCoreViewModel.ModuleInfo module : modules) {
    // 所有数据和格式化都已完成
    String text = module.getDisplayName() + " " + module.getLevelText();
    TextFormatting color = module.getColor();

    drawString(color + text, x, y);
}
```

### 步骤 4: 使用预定义的格式化方法

**能量显示**:
```java
// 基础数据
int energy = viewModel.getEnergy();
int maxEnergy = viewModel.getMaxEnergy();
float percentage = viewModel.getEnergyPercentage();

// 格式化文本
String energyText = viewModel.getEnergyText();           // "1.2k / 10.0k"
String percentageText = viewModel.getEnergyPercentageText(); // "12.0%"

// 智能颜色
TextFormatting color = viewModel.getEnergyColor();
```

**模块信息**:
```java
MechCoreViewModel.ModuleInfo module = viewModel.getModule("FLIGHT_MODULE");

String id = module.getId();                    // "FLIGHT_MODULE"
String name = module.getDisplayName();         // "飞行模块"
String level = module.getLevelText();          // "Lv.3/5"
String status = module.getStatusText();        // "运行中"
TextFormatting color = module.getColor();      // GOLD/GREEN/GRAY
TextFormatting statusColor = module.getStatusColor(); // GREEN/RED/GRAY
boolean active = module.isActive();            // true/false
```

---

## 📝 完整示例

查看 `MechanicalCoreSimpleGui.java` 了解完整的使用示例。

### 关键代码片段

**能量条绘制**:
```java
private void drawEnergyBar(int x, int y) {
    int barWidth = 200;
    int barHeight = 10;

    // 背景
    drawRect(x, y, x + barWidth, y + barHeight, 0xFF333333);

    // 填充（使用 ViewModel 数据）
    float percentage = viewModel.getEnergyPercentage();
    int fillWidth = (int) (barWidth * percentage);

    // 颜色自动根据百分比选择
    int fillColor = getEnergyBarColor(percentage);
    drawRect(x, y, x + fillWidth, y + barHeight, fillColor);

    // 边框...
}
```

**模块列表绘制**:
```java
private void drawModuleList() {
    List<MechCoreViewModel.ModuleInfo> modules = viewModel.getAllModules();

    for (MechCoreViewModel.ModuleInfo module : modules) {
        // 名称（带颜色）
        this.fontRenderer.drawString(
            module.getColor() + module.getDisplayName(),
            x, y,
            0xFFFFFF
        );

        // 等级
        this.fontRenderer.drawString(
            TextFormatting.GRAY + module.getLevelText(),
            x + 120, y,
            0xFFFFFF
        );

        // 状态
        this.fontRenderer.drawString(
            module.getStatusColor() + module.getStatusText(),
            x + 180, y,
            0xFFFFFF
        );

        y += 12;
    }
}
```

---

## 🎨 ViewModel API 速查表

### 能量系统

| 方法 | 返回类型 | 示例输出 | 说明 |
|------|---------|---------|------|
| `getEnergy()` | `int` | `12345` | 当前能量 |
| `getMaxEnergy()` | `int` | `100000` | 最大能量 |
| `getEnergyPercentage()` | `float` | `0.12` | 能量百分比 (0.0-1.0) |
| `getEnergyText()` | `String` | `"12.3k / 100.0k"` | 格式化能量文本 |
| `getEnergyPercentageText()` | `String` | `"12.3%"` | 格式化百分比 |
| `getEnergyColor()` | `TextFormatting` | `YELLOW` | 智能颜色 |

### 模块系统

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getAllModules()` | `List<ModuleInfo>` | 所有模块列表 |
| `getActiveModules()` | `List<ModuleInfo>` | 激活的模块 |
| `getModule(String id)` | `ModuleInfo` | 获取特定模块 |

### ModuleInfo

| 方法 | 返回类型 | 示例输出 | 说明 |
|------|---------|---------|------|
| `getId()` | `String` | `"FLIGHT_MODULE"` | 模块ID |
| `getLevel()` | `int` | `3` | 当前等级 |
| `getMaxLevel()` | `int` | `5` | 最大等级 |
| `isActive()` | `boolean` | `true` | 是否激活 |
| `getDisplayName()` | `String` | `"飞行模块"` | 显示名称 |
| `getColor()` | `TextFormatting` | `GOLD` | 模块颜色 |
| `getLevelText()` | `String` | `"Lv.3/5"` | 等级文本 |
| `getStatusText()` | `String` | `"运行中"` | 状态文本 |
| `getStatusColor()` | `TextFormatting` | `GREEN` | 状态颜色 |

---

## 🚀 渐进式迁移策略

不需要一次性重写整个 GUI，可以采用渐进式迁移：

### 阶段 1: 部分功能迁移
- 先迁移能量显示
- 保留其他功能不变

### 阶段 2: 扩展迁移
- 迁移模块列表显示
- 保留交互逻辑不变

### 阶段 3: 完全迁移
- 所有数据读取使用 ViewModel
- 移除直接 NBT 访问

### 阶段 4: 清理
- 移除重复代码
- 简化逻辑

---

## ✅ 迁移检查清单

- [ ] 创建 ViewModel 实例
- [ ] 替换能量数据读取
- [ ] 替换模块列表读取
- [ ] 移除手动格式化代码
- [ ] 移除手动颜色选择代码
- [ ] 测试所有显示功能
- [ ] 移除未使用的辅助方法
- [ ] 更新注释和文档

---

## 📊 收益对比

| 方面 | Before | After | 改进 |
|------|--------|-------|------|
| **代码行数** | ~1500 行 | ~800 行 | -47% |
| **重复代码** | 大量格式化重复 | 统一格式化 | 消除 |
| **可维护性** | 低（逻辑混杂） | 高（分离清晰） | ↑↑ |
| **可测试性** | 难（GUI 耦合） | 易（ViewModel 独立） | ↑↑ |
| **数据同步** | 手动 | 自动 | ↑↑ |

---

## 🎯 最佳实践

1. **单一数据源**: 所有数据从 ViewModel 获取
2. **不要缓存**: ViewModel 会处理数据更新
3. **使用格式化方法**: 不要手动格式化
4. **使用智能颜色**: 不要手动选择颜色
5. **保持简单**: GUI 只负责显示

---

**参考实现**: `MechanicalCoreSimpleGui.java`
**ViewModel 源码**: `MechCoreViewModel.java`
