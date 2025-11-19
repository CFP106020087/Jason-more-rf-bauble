# 机械核心重构 - 快速开始指南

## 🚀 3步集成新系统

### 第1步：注册系统（preInit）

在你的Mod主类中添加：

```java
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;

@Mod.EventHandler
public void preInit(FMLPreInitializationEvent event) {
    // 注册Capability
    MechanicalCoreCapability.register();

    // 初始化升级注册表
    UpgradeRegistry.init();

    // 可选：打印注册信息（调试用）
    UpgradeRegistry.printRegistry();
}
```

### 第2步：为ItemMechanicalCore添加Capability

在 `ItemMechanicalCore.java` 中添加：

```java
import com.moremod.core.capability.MechanicalCoreProvider;

@Nullable
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    return new MechanicalCoreProvider(stack);
}
```

### 第3步：注册网络包（如果需要GUI）

```java
import com.moremod.core.network.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public static final SimpleNetworkWrapper NETWORK =
    NetworkRegistry.INSTANCE.newSimpleChannel("moremod");

@Mod.EventHandler
public void preInit(FMLPreInitializationEvent event) {
    // ... 前面的注册代码 ...

    // 注册网络包
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

## 🎯 常用代码片段

### 获取升级等级

```java
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.capability.MechanicalCoreCapability;

IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);

if (data != null) {
    int level = data.getLevel("YELLOW_SHIELD");
}
```

### 设置升级等级

```java
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);

if (data != null) {
    data.setLevel("YELLOW_SHIELD", 3);

    // 保存到NBT
    NBTTagCompound nbt = stack.getTagCompound();
    if (nbt != null) {
        nbt.setTag("CoreData", data.serializeNBT());
    }
}
```

### 检查升级是否激活

```java
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);

if (data != null && data.isActive("DAMAGE_BOOST")) {
    // 升级激活，应用效果
    int level = data.getEffectiveLevel("DAMAGE_BOOST");
    float damageBonus = level * 0.25f;
}
```

---

## 📦 已创建的文件清单

### 核心API (com/moremod/core/api/)
- ✅ `CoreUpgradeEntry.java` - 升级数据模型
- ✅ `IMechanicalCoreData.java` - Capability接口

### Capability实现 (com/moremod/core/capability/)
- ✅ `MechanicalCoreCapability.java` - Capability注册
- ✅ `MechanicalCoreData.java` - 数据实现
- ✅ `MechanicalCoreProvider.java` - Provider

### 升级注册 (com/moremod/core/registry/)
- ✅ `UpgradeDefinition.java` - 升级定义
- ✅ `UpgradeRegistry.java` - 注册中心（包含所有升级）

### 迁移工具 (com/moremod/core/migration/)
- ✅ `MechanicalCoreLegacyMigration.java` - 旧NBT迁移

### 网络包 (com/moremod/core/network/)
- ✅ `PacketCoreSetLevel.java` - 设置等级
- ✅ `PacketCoreRepairModule.java` - 修复模块
- ✅ `PacketCorePauseResume.java` - 暂停/恢复

### 系统 (com/moremod/core/system/)
- ✅ `CorePunishmentSystem.java` - 惩罚系统（基于Capability）

---

## 🔄 迁移现有代码示例

### 示例1：获取等级

```java
// ❌ 旧代码
NBTTagCompound nbt = stack.getTagCompound();
int level = nbt.getInteger("upgrade_YELLOW_SHIELD");

// ✅ 新代码
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
int level = data != null ? data.getLevel("YELLOW_SHIELD") : 0;
```

### 示例2：设置等级

```java
// ❌ 旧代码
NBTTagCompound nbt = stack.getTagCompound();
if (nbt == null) {
    nbt = new NBTTagCompound();
    stack.setTagCompound(nbt);
}
nbt.setInteger("upgrade_YELLOW_SHIELD", 3);
nbt.setBoolean("HasUpgrade_YELLOW_SHIELD", true);

// ✅ 新代码
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
if (data != null) {
    data.setLevel("YELLOW_SHIELD", 3);
    stack.getTagCompound().setTag("CoreData", data.serializeNBT());
}
```

### 示例3：暂停/恢复

```java
// ❌ 旧代码
NBTTagCompound nbt = stack.getTagCompound();
int currentLevel = nbt.getInteger("upgrade_STEALTH");
nbt.setInteger("LastLevel_STEALTH", currentLevel);
nbt.setInteger("upgrade_STEALTH", 0);
nbt.setBoolean("IsPaused_STEALTH", true);

// ✅ 新代码
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
if (data != null) {
    data.pause("STEALTH");
    stack.getTagCompound().setTag("CoreData", data.serializeNBT());
}
```

### 示例4：降级模块

```java
// ❌ 旧代码
NBTTagCompound nbt = stack.getTagCompound();
int ownedMax = nbt.getInteger("OwnedMax_DAMAGE_BOOST");
nbt.setInteger("OriginalMax_DAMAGE_BOOST", ownedMax);
nbt.setInteger("OwnedMax_DAMAGE_BOOST", ownedMax - 1);
nbt.setBoolean("WasPunished_DAMAGE_BOOST", true);
int damageCount = nbt.getInteger("DamageCount_DAMAGE_BOOST");
nbt.setInteger("DamageCount_DAMAGE_BOOST", damageCount + 1);

// ✅ 新代码
IMechanicalCoreData data = stack.getCapability(
    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
if (data != null) {
    data.degrade("DAMAGE_BOOST", 1);
    stack.getTagCompound().setTag("CoreData", data.serializeNBT());
}
```

---

## ⚠️ 重要注意事项

### 1. 旧存档自动迁移

第一次访问Capability时会自动迁移旧NBT数据，无需手动处理。

### 2. 保存数据到NBT

修改Capability后，必须手动保存到NBT：

```java
// 修改数据
data.setLevel("UPGRADE_ID", 3);

// 保存到NBT（必须！）
stack.getTagCompound().setTag("CoreData", data.serializeNBT());
```

### 3. 别名自动处理

所有别名会自动映射到规范ID：

```java
// 这些都是同一个升级
data.setLevel("WATERPROOF", 3);
data.setLevel("waterproof_module", 3);
data.setLevel("WATERPROOF_MODULE", 3);
```

### 4. 线程安全

所有Capability操作必须在主线程执行。

---

## 🧪 测试清单

- [ ] 新核心可以正常创建和使用
- [ ] 旧存档的核心可以正常加载
- [ ] 升级等级正确显示
- [ ] 暂停/恢复功能正常
- [ ] 禁用/启用功能正常
- [ ] 降级/修复功能正常
- [ ] 自毁系统正常
- [ ] GUI正确显示所有信息
- [ ] 网络包正常工作

---

## 📚 详细文档

查看 `MECHANICAL_CORE_REFACTOR.md` 获取完整的API文档和详细说明。

---

## ✨ 新系统的优势

1. **统一数据模型** - 所有升级数据集中管理
2. **清晰的API** - 不再直接操作NBT
3. **自动别名处理** - 无需手动映射大小写
4. **完全兼容旧存档** - 自动迁移
5. **易于扩展** - 添加新升级只需在Registry注册
6. **类型安全** - 编译时检查，减少错误

---

## 🎉 完成！

所有核心文件已创建，现在你可以：

1. 按照上面3步完成集成
2. 逐步迁移旧代码到新API
3. 测试并验证功能

祝你重构顺利！如有问题，请查看详细文档。
