# 机械核心系统重构文档

## 概述

本文档说明如何将旧的基于NBT的机械核心系统迁移到新的Capability架构。

新架构的优势：
- ✅ 统一的数据模型
- ✅ 清晰的API接口
- ✅ 自动别名处理
- ✅ 完全兼容旧存档
- ✅ 易于维护和扩展

---

## 文件结构

```
com/moremod/core/
├── api/
│   ├── CoreUpgradeEntry.java          # 单个升级的数据模型
│   └── IMechanicalCoreData.java       # Capability接口
├── capability/
│   ├── MechanicalCoreCapability.java  # Capability注册
│   ├── MechanicalCoreData.java        # Capability实现
│   └── MechanicalCoreProvider.java    # Capability Provider
├── registry/
│   ├── UpgradeDefinition.java         # 升级定义
│   └── UpgradeRegistry.java           # 升级注册中心
├── migration/
│   └── MechanicalCoreLegacyMigration.java  # 旧存档迁移
├── network/
│   ├── PacketCoreSetLevel.java        # 网络包：设置等级
│   ├── PacketCoreRepairModule.java    # 网络包：修复模块
│   └── PacketCorePauseResume.java     # 网络包：暂停/恢复
└── system/
    └── CorePunishmentSystem.java      # 惩罚系统
```

---

## 第一步：注册Capability和升级

### 在你的 Mod 主类的 `preInit` 方法中添加：

```java
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;

@Mod.EventHandler
public void preInit(FMLPreInitializationEvent event) {
    // 1. 注册Capability
    MechanicalCoreCapability.register();

    // 2. 初始化升级注册表
    UpgradeRegistry.init();

    // 3. 注册网络包（如果使用SimpleNetworkWrapper）
    registerPackets();
}

private void registerPackets() {
    // 假设你有一个网络通道实例
    int packetId = 0;

    NETWORK.registerMessage(
        PacketCoreSetLevel.Handler.class,
        PacketCoreSetLevel.class,
        packetId++,
        Side.SERVER
    );

    NETWORK.registerMessage(
        PacketCoreRepairModule.Handler.class,
        PacketCoreRepairModule.class,
        packetId++,
        Side.SERVER
    );

    NETWORK.registerMessage(
        PacketCorePauseResume.Handler.class,
        PacketCorePauseResume.class,
        packetId++,
        Side.SERVER
    );
}
```

---

## 第二步：为ItemMechanicalCore添加Provider

### 修改 `ItemMechanicalCore.java`

在 `initCapabilities` 方法中添加Provider：

```java
import com.moremod.core.capability.MechanicalCoreProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

@Nullable
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    // 返回Capability Provider
    return new MechanicalCoreProvider(stack);
}
```

---

## 第三步：使用新的API替换旧代码

### 旧代码示例（基于NBT）

```java
// ❌ 旧方式：直接操作NBT
NBTTagCompound nbt = stack.getTagCompound();
int level = nbt.getInteger("upgrade_YELLOW_SHIELD");
nbt.setInteger("upgrade_YELLOW_SHIELD", level + 1);
```

### 新代码示例（使用Capability）

```java
// ✅ 新方式：使用Capability API
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);

if (data != null) {
    int level = data.getLevel("YELLOW_SHIELD");
    data.setLevel("YELLOW_SHIELD", level + 1);
}
```

---

## 常用API示例

### 1. 获取升级等级

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    int level = data.getLevel("YELLOW_SHIELD");
    int effectiveLevel = data.getEffectiveLevel("YELLOW_SHIELD"); // 考虑暂停/禁用
}
```

### 2. 设置升级等级

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    data.setLevel("YELLOW_SHIELD", 3);
}
```

### 3. 暂停/恢复升级

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    data.pause("STEALTH");      // 暂停
    data.resume("STEALTH");     // 恢复
}
```

### 4. 禁用/启用升级

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    data.setDisabled("ORE_VISION", true);  // 禁用
    data.setDisabled("ORE_VISION", false); // 启用
}
```

### 5. 检查升级状态

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    boolean installed = data.isInstalled("DAMAGE_BOOST");
    boolean active = data.isActive("DAMAGE_BOOST");
    boolean paused = data.isPaused("DAMAGE_BOOST");
    boolean disabled = data.isDisabled("DAMAGE_BOOST");
    boolean damaged = data.isDamaged("DAMAGE_BOOST");
}
```

### 6. 降级和修复

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    // 降级
    data.degrade("DAMAGE_BOOST", 1);

    // 修复到指定等级
    data.repair("DAMAGE_BOOST", 3);

    // 完全修复
    data.fullRepair("DAMAGE_BOOST");
}
```

### 7. 获取已安装的升级列表

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    List<String> installed = data.getInstalledUpgrades();
    for (String upgradeId : installed) {
        int level = data.getLevel(upgradeId);
        System.out.println(upgradeId + ": " + level);
    }
}
```

### 8. 统计信息

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    int installedCount = data.getInstalledCount();
    int activeCount = data.getActiveCount();
    int totalLevel = data.getTotalLevel();
    int totalActiveLevel = data.getTotalActiveLevel();
}
```

---

## 工具方法

### 获取Capability数据

```java
@Nullable
public static IMechanicalCoreData getCoreData(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
        return null;
    }

    return stack.getCapability(
        MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
}
```

### 从玩家获取核心数据

```java
@Nullable
public static IMechanicalCoreData getPlayerCoreData(EntityPlayer player) {
    ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
    if (core.isEmpty()) {
        return null;
    }

    return getCoreData(core);
}
```

---

## 升级注册表使用

### 获取升级信息

```java
import com.moremod.core.registry.UpgradeRegistry;

// 获取规范ID（处理别名）
String canonId = UpgradeRegistry.canonicalIdOf("waterproof"); // 返回 "WATERPROOF_MODULE"

// 获取升级定义
UpgradeDefinition def = UpgradeRegistry.getDefinition("YELLOW_SHIELD");
if (def != null) {
    String displayName = def.getDisplayName();
    int maxLevel = def.getMaxLevel();
    TextFormatting color = def.getColor();
    UpgradeCategory category = def.getCategory();
}

// 快捷方法
String displayName = UpgradeRegistry.getDisplayName("YELLOW_SHIELD");
int maxLevel = UpgradeRegistry.getMaxLevel("YELLOW_SHIELD");
TextFormatting color = UpgradeRegistry.getColor("YELLOW_SHIELD");

// 获取所有升级
Collection<UpgradeDefinition> allUpgrades = UpgradeRegistry.getAllDefinitions();

// 按类别获取
List<UpgradeDefinition> combatUpgrades = UpgradeRegistry.getByCategory(
    UpgradeDefinition.UpgradeCategory.COMBAT);
```

---

## 网络包使用

### 客户端发送设置等级请求

```java
import com.moremod.core.network.PacketCoreSetLevel;

// 发送设置等级的网络包
NETWORK.sendToServer(new PacketCoreSetLevel("YELLOW_SHIELD", 3));
```

### 客户端发送修复请求

```java
import com.moremod.core.network.PacketCoreRepairModule;

// 部分修复到等级3
NETWORK.sendToServer(new PacketCoreRepairModule("DAMAGE_BOOST", 3));

// 完全修复
NETWORK.sendToServer(new PacketCoreRepairModule("DAMAGE_BOOST"));
```

### 客户端发送暂停/恢复请求

```java
import com.moremod.core.network.PacketCorePauseResume;

// 暂停
NETWORK.sendToServer(new PacketCorePauseResume("STEALTH", true));

// 恢复
NETWORK.sendToServer(new PacketCorePauseResume("STEALTH", false));
```

---

## GUI集成示例

### 在GUI中显示升级信息

```java
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.registry.UpgradeRegistry;

public void renderUpgradeList(ItemStack core) {
    IMechanicalCoreData data = getCoreData(core);
    if (data == null) return;

    List<String> installed = data.getInstalledUpgrades();
    int y = 10;

    for (String upgradeId : installed) {
        // 获取显示信息
        String displayName = UpgradeRegistry.getDisplayName(upgradeId);
        TextFormatting color = UpgradeRegistry.getColor(upgradeId);

        int level = data.getLevel(upgradeId);
        int ownedMax = data.getOwnedMax(upgradeId);
        int originalMax = data.getOriginalMax(upgradeId);

        boolean paused = data.isPaused(upgradeId);
        boolean disabled = data.isDisabled(upgradeId);
        boolean damaged = data.isDamaged(upgradeId);

        // 渲染
        String text = color + displayName + " [" + level + "/" + ownedMax + "]";

        if (paused) {
            text += TextFormatting.YELLOW + " (暂停)";
        } else if (disabled) {
            text += TextFormatting.RED + " (禁用)";
        }

        if (damaged) {
            text += TextFormatting.DARK_RED + " (损坏:" + originalMax + ")";
        }

        drawString(text, 10, y, 0xFFFFFF);
        y += 12;
    }
}
```

---

## 注意事项

### 1. 旧存档兼容性

旧存档会在**第一次访问Capability时**自动迁移。迁移后：
- 所有旧NBT数据会被读取并转换到Capability
- 会标记 `Core3_Migrated = true`
- 旧NBT键会保留（不删除），以保持向后兼容

如果需要清理旧NBT键（减小NBT大小）：

```java
import com.moremod.core.migration.MechanicalCoreLegacyMigration;

// 清理旧NBT键（可选，谨慎使用）
NBTTagCompound nbt = stack.getTagCompound();
if (nbt != null && nbt.getBoolean("Core3_Migrated")) {
    MechanicalCoreLegacyMigration.cleanupLegacyKeys(nbt);
}
```

### 2. 线程安全

Capability数据不是线程安全的，所有操作必须在**主线程**执行。

### 3. NBT同步

修改Capability数据后，需要手动同步到NBT（用于保存和网络传输）：

```java
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    // 修改数据
    data.setLevel("YELLOW_SHIELD", 3);

    // 同步到NBT
    NBTTagCompound nbt = stack.getTagCompound();
    if (nbt != null) {
        nbt.setTag("CoreData", data.serializeNBT());
    }
}
```

### 4. 别名处理

所有升级ID都会自动规范化，无需手动处理别名：

```java
// 这些都会映射到同一个升级
data.setLevel("WATERPROOF_MODULE", 3);
data.setLevel("waterproof", 3);
data.setLevel("WATERPROOF", 3);
// 最终都存储为 "WATERPROOF_MODULE"
```

---

## 迁移检查清单

- [ ] 在preInit中注册Capability和升级
- [ ] 为ItemMechanicalCore添加initCapabilities
- [ ] 注册网络包
- [ ] 替换所有直接操作NBT的代码为Capability API
- [ ] 更新GUI代码使用新API
- [ ] 更新网络包使用新的Packet类
- [ ] 测试旧存档兼容性
- [ ] 测试所有功能（暂停、修复、降级等）

---

## 常见问题

### Q: 如何测试旧存档迁移？

A:
1. 使用旧版本创建一个存档，装备机械核心并安装一些升级
2. 切换到新版本
3. 打开存档，检查核心是否正常工作
4. 检查NBT中是否有 `Core3_Migrated = true`

### Q: 如何添加新的升级？

A: 在 `UpgradeRegistry.init()` 中注册：

```java
register(UpgradeDefinition.builder("NEW_UPGRADE")
    .displayName("新升级")
    .color(TextFormatting.AQUA)
    .maxLevel(5)
    .category(UpgradeDefinition.UpgradeCategory.COMBAT)
    .build());
```

### Q: 如何处理别名？

A: 在注册时添加别名：

```java
register(UpgradeDefinition.builder("MAIN_ID")
    .displayName("显示名称")
    .aliases("ALIAS1", "ALIAS2", "alias3")
    .build());
```

---

## 性能优化建议

1. **缓存Capability数据**：避免重复调用 `getCapability()`

```java
// ❌ 不好的做法
for (int i = 0; i < 100; i++) {
    int level = stack.getCapability(MECHANICAL_CORE_DATA, null).getLevel("ID");
}

// ✅ 好的做法
IMechanicalCoreData data = stack.getCapability(MECHANICAL_CORE_DATA, null);
if (data != null) {
    for (int i = 0; i < 100; i++) {
        int level = data.getLevel("ID");
    }
}
```

2. **批量操作**：减少NBT序列化次数

```java
// 修改多个升级后，一次性保存
IMechanicalCoreData data = getCoreData(stack);
if (data != null) {
    data.setLevel("UPGRADE1", 3);
    data.setLevel("UPGRADE2", 2);
    data.setLevel("UPGRADE3", 5);

    // 一次性保存到NBT
    stack.getTagCompound().setTag("CoreData", data.serializeNBT());
}
```

---

## 技术支持

如有问题，请查看：
- `CoreUpgradeEntry` - 数据模型定义
- `IMechanicalCoreData` - API文档
- `UpgradeRegistry` - 升级注册表
- `MechanicalCoreLegacyMigration` - 迁移逻辑

祝重构顺利！🎉
