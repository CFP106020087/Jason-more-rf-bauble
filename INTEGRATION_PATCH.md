# 机械核心重构 - 集成补丁指南

## ⚠️ 重要提醒

**当前状态**：新的 Capability 架构代码已创建，但**尚未集成**到主类中。

**不能直接编译**，需要按照本文档进行 3 处关键修改。

---

## 📋 必须修改的文件

### 1️⃣ 修改主类：`src/main/java/com/moremod/moremod.java`

**位置**：`preInit()` 方法的第 257 行之后

**需要添加的代码**：

```java
// ========== 机械核心 Capability 注册 ==========
System.out.println("[moremod] 🔧 注册机械核心 Capability...");
com.moremod.core.capability.MechanicalCoreCapability.register();
com.moremod.core.registry.UpgradeRegistry.init();
System.out.println("[moremod] ✅ 机械核心 Capability 注册完成（33个升级已注册）");
```

**插入位置示例**：

```java
// 注册 Capability
CapabilityManager.INSTANCE.register(
        IPlayerTimeData.class,
        new PlayerTimeDataStorage(),
        PlayerTimeDataImpl::new
);
System.out.println("[moremod] ✅ 时光之心Capability注册完成");

// ========== 新增：机械核心 Capability 注册 ==========
System.out.println("[moremod] 🔧 注册机械核心 Capability...");
com.moremod.core.capability.MechanicalCoreCapability.register();
com.moremod.core.registry.UpgradeRegistry.init();
System.out.println("[moremod] ✅ 机械核心 Capability 注册完成（33个升级已注册）");
// =============================================

// ========== Ritual 多方块：创建实例（不在这里注册）==========
System.out.println("[moremod] 🔮 创建 Ritual 多方块实例...");
```

---

### 2️⃣ 修改物品类：`src/main/java/com/moremod/item/ItemMechanicalCore.java`

**位置**：第 1956-1958 行的 `initCapabilities()` 方法

**当前代码**（错误）：
```java
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    return new MechanicalCoreEnergyProvider(stack);  // ❌ 旧的Provider
}
```

**修改为**（正确）：
```java
@Override
public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
    return new com.moremod.core.capability.MechanicalCoreProviderFixed(stack);  // ✅ 新的Provider
}
```

**⚠️ 关键说明**：
- 旧的 `MechanicalCoreEnergyProvider` 只提供能量能力
- 新的 `MechanicalCoreProviderFixed` 同时提供：
  - `MechanicalCoreCapability.MECHANICAL_CORE_DATA`（数据能力）
  - `CapabilityEnergy.ENERGY`（能量能力）

---

### 3️⃣ 注册网络包（可选，如果使用GUI）

**位置**：`moremod.java` 的 `initNetworkPackets()` 方法中

**需要添加的代码**：

```java
private void initNetworkPackets() {
    // ... 现有的网络包注册 ...

    // ========== 机械核心网络包注册 ==========
    int nextPacketId = 100; // 确保不与现有ID冲突

    network.registerMessage(
        com.moremod.core.network.PacketCoreSetLevel.Handler.class,
        com.moremod.core.network.PacketCoreSetLevel.class,
        nextPacketId++,
        Side.SERVER
    );

    network.registerMessage(
        com.moremod.core.network.PacketCoreRepairModule.Handler.class,
        com.moremod.core.network.PacketCoreRepairModule.class,
        nextPacketId++,
        Side.SERVER
    );

    network.registerMessage(
        com.moremod.core.network.PacketCorePauseResume.Handler.class,
        com.moremod.core.network.PacketCorePauseResume.class,
        nextPacketId++,
        Side.SERVER
    );

    System.out.println("[moremod] ✅ 机械核心网络包注册完成");
}
```

**⚠️ 注意**：检查 `nextPacketId` 的起始值，确保不与现有网络包ID冲突。

---

## 🔧 可选修改（推荐）

### 4️⃣ 替换旧的惩罚系统（可选）

如果你想使用新的惩罚系统，可以在 `preInit()` 中：

**查找**：
```java
MinecraftForge.EVENT_BUS.register(EnergyPunishmentSystem.class);
```

**替换为**：
```java
MinecraftForge.EVENT_BUS.register(com.moremod.core.system.CorePunishmentSystem.class);
```

**说明**：新的 `CorePunishmentSystem` 完全基于 Capability API，代码更清晰。但这不是必须的，旧系统仍然可用。

---

## ✅ 集成验证清单

完成上述修改后，检查以下内容：

- [ ] 编译成功（无错误）
- [ ] 游戏启动时控制台显示 "✅ 机械核心 Capability 注册完成（33个升级已注册）"
- [ ] 创建新的机械核心物品，能正常充电/放电（能量系统工作）
- [ ] 设置升级等级，能正常保存/读取（数据能力工作）
- [ ] 旧存档中的机械核心能正常加载（迁移系统工作）

---

## 🔍 快速集成脚本（自动应用补丁）

如果你希望我自动应用这些修改，请告知。我可以：

1. 自动修改 `moremod.java` 添加 Capability 注册
2. 自动修改 `ItemMechanicalCore.java` 替换 Provider
3. 自动添加网络包注册（如果需要）
4. 提交并推送所有修改

---

## 📞 下一步

**选项 A：手动集成**
- 按照上述步骤修改 3 个文件
- 编译测试

**选项 B：自动集成**
- 告诉我："请自动应用集成补丁"
- 我会自动修改所有文件并提交

---

## 📚 相关文档

- **QUICK_START_GUIDE.md** - 快速开始指南
- **REFACTOR_ERRATA.md** - 勘误与修正
- **MECHANICAL_CORE_REFACTOR.md** - 完整API文档

---

## ⚠️ 重要提醒

1. **必须使用 `MechanicalCoreProviderFixed`**，不要使用 `MechanicalCoreProvider`（缺少能量能力）
2. **必须在 preInit 中注册 Capability**，否则运行时会崩溃
3. **网络包注册是可选的**，只有需要GUI时才必须

---

当前状态：**需要集成** ⏳
集成后状态：**可以编译使用** ✅
