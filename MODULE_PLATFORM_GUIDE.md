# 机械核心模块封印式包装层 使用指南

> **版本**: 1.0.0
> **适用于**: Minecraft Forge 1.12.2
> **目标**: 封印旧系统，简化新模块开发

---

## 📚 目录

1. [概述](#概述)
2. [快速开始](#快速开始)
3. [架构说明](#架构说明)
4. [创建新模块](#创建新模块)
5. [集成到现有系统](#集成到现有系统)
6. [API 参考](#api-参考)
7. [常见问题](#常见问题)

---

## 概述

### 这是什么？

这是一套**封印式包装层**（Wrapper Platform），用于管理机械核心的升级模块系统。

### 解决什么问题？

- ❌ **旧问题**：升级模块逻辑分散在多个类中，NBT读写混乱，难以维护
- ✅ **新方案**：封装所有底层复杂逻辑，新模块开发只需实现简单接口

### 核心特性

✅ **封印旧系统** - 所有NBT、tick、事件、生命周期都被包装层处理
✅ **简化开发** - 新模块只需继承 `BaseUpgradeModule`
✅ **自动能量管理** - 根据 `getPassiveEnergyCost()` 自动消耗能量
✅ **自动状态管理** - level、paused、disabled、cooldown 自动存取
✅ **兼容旧格式** - 自动读取旧的 NBT 格式（`upgrade_*`、`HasUpgrade_*`）
✅ **无侵入式** - 不修改现有的 `IUpgradeModule` 接口

---

## 快速开始

### 1. 创建新模块（3步）

```java
package com.moremod.upgrades.custom;

import com.moremod.upgrades.platform.BaseUpgradeModule;
import com.moremod.upgrades.platform.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nonnull;

// 第1步：继承 BaseUpgradeModule
public class MyCustomModule extends BaseUpgradeModule {

    public static final MyCustomModule INSTANCE = new MyCustomModule();

    // 第2步：在构造函数中定义模块信息
    private MyCustomModule() {
        super(
            "MY_CUSTOM_MODULE",  // 模块ID（全大写）
            "我的自定义模块",     // 显示名称
            3                     // 最大等级
        );
    }

    // 第3步：实现核心逻辑
    @Override
    protected void onModuleTick(@Nonnull ModuleContext context) {
        // 你的逻辑（每 tick 执行）
        EntityPlayer player = context.getPlayer();
        int level = context.getEffectiveLevel();

        // 例如：添加速度效果
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 40, level - 1));
    }

    @Override
    protected int getBaseEnergyCost() {
        return 10;  // 每级每tick消耗10 RF
    }
}
```

### 2. 注册模块

在 Mod 初始化时注册：

```java
@Mod.EventHandler
public void init(FMLInitializationEvent event) {
    // 注册模块
    ModuleRegistry.getInstance().register(MyCustomModule.INSTANCE);

    // 初始化平台
    ModulePlatform.getInstance().initialize();
}
```

### 3. 创建模块物品（可选）

如果需要让玩家通过物品升级模块，可以创建对应的物品类：

```java
package com.moremod.upgrades.custom;

import com.moremod.upgrades.platform.ModuleUpgradeItem;
import net.minecraft.util.text.TextFormatting;

// 继承 ModuleUpgradeItem
public class MyCustomModuleItem extends ModuleUpgradeItem {

    public MyCustomModuleItem() {
        // 关联到运行时模块，每个物品提供 1 级升级
        super(MyCustomModule.INSTANCE, 1);

        // 设置自定义描述（可选）
        this.setDescriptions(
            TextFormatting.GRAY + "我的自定义模块",
            TextFormatting.GRAY + "提供强大的功能"
        );
    }
}
```

然后在物品注册时注册这个物品：

```java
@Mod.EventBusSubscriber(modid = MoreMod.MODID)
public class ModItems {
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(
            new MyCustomModuleItem()
                .setRegistryName("my_custom_module")
                .setTranslationKey("my_custom_module")
        );
    }
}
```

### 4. 集成到机械核心

在 `ItemMechanicalCore` 中调用：

```java
@Override
public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
    if (entity instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) entity;

        // 调用模块平台（封装层会处理所有逻辑）
        ModulePlatform.getInstance().tickAllModules(player, itemstack);

        // ... 其他旧代码保持不变 ...
    }
}

@Override
public void onEquipped(ItemStack itemstack, EntityLivingBase entity) {
    if (entity instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) entity;

        // 调用装备事件
        ModulePlatform.getInstance().onCoreEquipped(player, itemstack);

        // ... 其他旧代码保持不变 ...
    }
}

@Override
public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
    if (entity instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) entity;

        // 调用卸载事件
        ModulePlatform.getInstance().onCoreUnequipped(player, itemstack);

        // ... 其他旧代码保持不变 ...
    }
}
```

**完成！** 新模块现在可以正常工作了。

---

## 架构说明

### 组件层次

```
                物品层 (Item Layer)
┌─────────────────────────────────────────────┐
│       ModuleUpgradeItem (物品基类)           │  ← 你的物品继承这个
└─────────────────────────────────────────────┘
                      ↓ 关联
               运行时层 (Runtime Layer)
┌─────────────────────────────────────────────┐
│          ModulePlatform (入口)               │  ← 你只需调用这个
├─────────────────────────────────────────────┤
│  ModuleRegistry  │  ModuleDispatcher        │  ← 封装层（自动处理）
├──────────────────┼──────────────────────────┤
│  ModuleDataStorage  │  ModuleContext        │  ← 数据层（自动管理）
├─────────────────────────────────────────────┤
│           ModuleState (状态对象)             │  ← 状态封装
├─────────────────────────────────────────────┤
│       BaseUpgradeModule (基类)              │  ← 你的模块继承这个
├─────────────────────────────────────────────┤
│       IUpgradeModule (接口)                 │  ← 原有接口（不修改）
└─────────────────────────────────────────────┘
```

### 各组件职责

| 组件 | 职责 | 你需要了解吗 |
|------|------|-------------|
| **ModuleUpgradeItem** | 模块升级物品基类 | ✅ 是（物品开发） |
| **ModulePlatform** | 核心入口，管理所有模块 | ✅ 是（调用接口） |
| **ModuleRegistry** | 模块注册表 | ✅ 是（注册模块） |
| **ModuleDispatcher** | 分发tick和事件 | ❌ 否（自动） |
| **ModuleDataStorage** | NBT存取包装器 | ❌ 否（自动） |
| **ModuleContext** | 模块运行上下文 | ✅ 是（常用） |
| **ModuleState** | 模块状态封装 | ⚠️ 可选（高级） |
| **BaseUpgradeModule** | 模块基类 | ✅ 是（继承它） |
| **CoreModuleItemHelper** | 辅助工具类 | ⚠️ 可选（便捷） |

---

## 创建新模块

### 基础模块示例

```java
public class SimpleModule extends BaseUpgradeModule {
    public static final SimpleModule INSTANCE = new SimpleModule();

    private SimpleModule() {
        super("SIMPLE_MODULE", "简单模块", 3);
    }

    @Override
    protected void onModuleTick(@Nonnull ModuleContext context) {
        // Tick 逻辑
    }
}
```

### 完整功能模块示例

```java
public class AdvancedModule extends BaseUpgradeModule {
    public static final AdvancedModule INSTANCE = new AdvancedModule();

    private AdvancedModule() {
        super("ADVANCED_MODULE", "高级模块", 5);
    }

    // === 生命周期 ===

    @Override
    protected void onModuleEquip(@Nonnull ModuleContext context) {
        // 装备时执行
        sendMessage(context.getPlayer(), "模块已激活！");
    }

    @Override
    protected void onModuleTick(@Nonnull ModuleContext context) {
        // 每tick执行

        // 1. 获取基本信息
        EntityPlayer player = context.getPlayer();
        int level = context.getEffectiveLevel();

        // 2. 检查冷却
        if (context.isOnCooldown()) {
            return;
        }

        // 3. 执行逻辑
        if (someCondition()) {
            doSomething(player, level);

            // 4. 设置冷却（20 tick = 1秒）
            context.setCooldown(20);
        }

        // 5. 使用自定义数据
        int count = context.getCustomInt("useCount", 0);
        context.setCustomInt("useCount", count + 1);
    }

    @Override
    protected void onModuleUnequip(@Nonnull ModuleContext context) {
        // 卸载时执行
        cleanupEffects(context.getPlayer());
    }

    @Override
    protected void onModuleEvent(@Nonnull Event event, @Nonnull ModuleContext context) {
        // 处理特定事件
        if (event instanceof LivingHurtEvent) {
            LivingHurtEvent hurtEvent = (LivingHurtEvent) event;
            // 处理伤害事件
        }
    }

    // === 配置 ===

    @Override
    protected int getBaseEnergyCost() {
        return 15;  // 每级每tick 15 RF
    }

    @Override
    protected boolean shouldSendEquipMessage() {
        return false;  // 使用自定义消息
    }

    @Override
    protected boolean isDebugMode() {
        return false;  // 生产环境关闭调试
    }
}
```

### ModuleContext 常用API

```java
// 基础信息
context.getPlayer()              // 获取玩家
context.getCoreStack()           // 获取核心物品
context.getLevel()               // 获取等级
context.getEffectiveLevel()      // 获取有效等级（考虑暂停/禁用）
context.isActive()               // 是否激活

// 能量系统
context.getEnergyStored()        // 当前能量
context.getMaxEnergyStored()     // 最大能量
context.getEnergyPercentage()    // 能量百分比
context.consumeEnergy(100)       // 消耗能量
context.addEnergy(50)            // 添加能量

// 冷却系统
context.isOnCooldown()           // 是否在冷却中
context.setCooldown(20)          // 设置冷却（tick）
context.clearCooldown()          // 清除冷却
context.getRemainingCooldown()   // 剩余冷却时间

// 自定义数据
context.setCustomInt("key", 100)
context.getCustomInt("key", 0)
context.setCustomLong("key", 1000L)
context.getCustomBoolean("key", false)
context.setCustomString("key", "value")

// 玩家状态
context.isPlayerSneaking()
context.isPlayerSprinting()
context.isPlayerFlying()
context.getPlayerHealth()

// 世界信息
context.getWorld()
context.getWorldTime()
context.isClientSide()
context.isServerSide()
```

---

## 集成到现有系统

### 在 Mod 主类中初始化

```java
@Mod(modid = "moremod", name = "MoreMod", version = "1.0")
public class MoreMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册所有模块
        registerModules();

        // 初始化平台
        ModulePlatform.getInstance().initialize();
    }

    private void registerModules() {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        // 注册示例模块
        registry.register(SpeedModule.INSTANCE);

        // 注册你的自定义模块
        registry.register(MyCustomModule.INSTANCE);

        // 批量注册
        registry.registerAll(
            Module1.INSTANCE,
            Module2.INSTANCE,
            Module3.INSTANCE
        );
    }
}
```

### 在机械核心中调用

#### 方式1：完全使用包装层（推荐）

```java
public class ItemMechanicalCore extends ItemBaubleBattery {

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 新系统：使用包装层
        ModulePlatform.getInstance().tickAllModules(player, itemstack);

        // 旧系统：保留原有逻辑（逐步迁移）
        // ... 旧代码 ...
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 新系统
        ModulePlatform.getInstance().onCoreEquipped(player, itemstack);

        // 旧系统
        // ... 旧代码 ...
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 新系统
        ModulePlatform.getInstance().onCoreUnequipped(player, itemstack);

        // 旧系统
        // ... 旧代码 ...
    }
}
```

#### 方式2：事件处理集成

```java
@Mod.EventBusSubscriber
public class ModuleEventHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        ItemStack core = CoreModuleItemHelper.getEquippedCore(player);

        if (!core.isEmpty()) {
            // 使用包装层处理
            ModulePlatform.getInstance().tickAllModules(player, core);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack core = CoreModuleItemHelper.getEquippedCore(player);

        if (!core.isEmpty()) {
            // 分发事件给模块
            ModulePlatform.getInstance().handleEvent(event, player, core);
        }
    }
}
```

---

## API 参考

### ModulePlatform（核心API）

```java
// 初始化
ModulePlatform.getInstance().initialize()

// 生命周期
ModulePlatform.getInstance().onCoreEquipped(player, core)
ModulePlatform.getInstance().onCoreUnequipped(player, core)
ModulePlatform.getInstance().tickAllModules(player, core)
ModulePlatform.getInstance().handleEvent(event, player, core)

// 模块状态操作
ModulePlatform.getInstance().setModuleLevel(core, "MODULE_ID", 3)
ModulePlatform.getInstance().getModuleLevel(core, "MODULE_ID")
ModulePlatform.getInstance().isModuleActive(core, "MODULE_ID")
ModulePlatform.getInstance().pauseModule(core, "MODULE_ID")
ModulePlatform.getInstance().resumeModule(core, "MODULE_ID")
ModulePlatform.getInstance().disableModule(core, "MODULE_ID")
ModulePlatform.getInstance().enableModule(core, "MODULE_ID")

// 批量操作
ModulePlatform.getInstance().pauseAllModules(core)
ModulePlatform.getInstance().resumeAllModules(core)
```

### CoreModuleItemHelper（便捷工具）

```java
// 获取核心
ItemStack core = CoreModuleItemHelper.getEquippedCore(player)
boolean hasCore = CoreModuleItemHelper.hasEquippedCore(player)

// 查询模块
int level = CoreModuleItemHelper.getModuleLevel(player, "MODULE_ID")
boolean active = CoreModuleItemHelper.isModuleActive(player, "MODULE_ID")
ModuleState state = CoreModuleItemHelper.getModuleState(player, "MODULE_ID")

// 统计信息
int installed = CoreModuleItemHelper.getInstalledModuleCount(player)
int active = CoreModuleItemHelper.getActiveModuleCount(player)
List<String> activeIds = CoreModuleItemHelper.getActiveModuleIds(player)

// 操作模块
CoreModuleItemHelper.setModuleLevel(player, "MODULE_ID", 3)
CoreModuleItemHelper.pauseModule(player, "MODULE_ID")
CoreModuleItemHelper.resumeModule(player, "MODULE_ID")

// 调试
CoreModuleItemHelper.debugPrintModules(player)
```

### ModuleUpgradeItem（物品基类）

用于创建模块升级物品，自动处理升级逻辑、tooltip、音效和粒子效果。

```java
// 基础用法
public class MyModuleItem extends ModuleUpgradeItem {
    public MyModuleItem() {
        super(MyModule.INSTANCE, 1);  // 关联模块，提供1级升级

        // 设置描述（可选）
        this.setDescriptions(
            TextFormatting.GRAY + "模块描述",
            TextFormatting.GOLD + "特殊效果"
        );
    }
}

// 可重写的方法
protected void addModuleDescription(List<String> tooltip) {
    // 自定义tooltip描述
}

protected void addEnergyCostInfo(List<String> tooltip) {
    // 自定义能量消耗信息显示
}

protected boolean canUpgrade(EntityPlayer player, ItemStack coreStack) {
    // 添加升级前置条件检查
    // 返回 false 阻止升级
}

protected void onUpgradeSuccess(EntityPlayer player, ItemStack coreStack, int newLevel) {
    // 升级成功后的自定义逻辑（如发送特殊消息）
}

protected void playUpgradeEffect(World world, EntityPlayer player, ItemStack coreStack) {
    // 自定义升级音效和粒子效果
}
```

**关键特性**：
- ✅ 自动关联运行时模块
- ✅ 自动生成 tooltip（显示名称、类别、等级、能量消耗）
- ✅ 自动处理升级逻辑（检查、应用、消耗物品）
- ✅ 自动播放音效和粒子效果
- ✅ 支持自定义描述和升级条件
- ✅ 与旧系统完全兼容

**使用示例**：
```java
// SpeedModuleItem.java - 速度模块物品
public class SpeedModuleItem extends ModuleUpgradeItem {
    public SpeedModuleItem() {
        super(SpeedModule.INSTANCE, 1);
        this.setDescriptions(
            TextFormatting.GRAY + "提升玩家移动速度",
            TextFormatting.GOLD + "• Lv.1-2: 速度 I",
            TextFormatting.GOLD + "• Lv.3-4: 速度 II"
        );
    }
}

// 注册物品
event.getRegistry().register(
    new SpeedModuleItem()
        .setRegistryName("speed_module")
        .setTranslationKey("speed_module")
);
```

---

## 常见问题

### Q1: 如何从旧模块迁移？

**答**：逐步迁移，旧模块保持不变，新模块使用包装层。

```java
// 旧模块（保持不变）
public class OldModule implements IUpgradeModule {
    // ... 旧代码 ...
}

// 新模块（使用包装层）
public class NewModule extends BaseUpgradeModule {
    // ... 简化的代码 ...
}
```

### Q2: 能量消耗如何工作？

**答**：自动消耗，基于 `getPassiveEnergyCost(level)`。

```java
@Override
protected int getBaseEnergyCost() {
    return 10;  // 每级每tick消耗10 RF
}
// 等级3 = 每tick消耗 30 RF
```

如果能量不足，模块的 `onModuleTick()` **不会被调用**。

### Q3: NBT 数据如何存储？

**答**：自动存储在新格式 `ModulePlatform.modules[]`，同时兼容旧格式 `upgrade_*`。

新格式：
```json
{
  "ModulePlatform": {
    "modules": [
      {
        "moduleId": "SPEED_BOOST",
        "level": 3,
        "ownedMax": 5,
        "paused": false,
        "disabled": false,
        "cooldown": 0,
        "custom": {}
      }
    ]
  }
}
```

### Q4: 自定义数据如何持久化？

**答**：使用 `ModuleContext` 的自定义数据API，自动保存到 NBT。

```java
// 写入
context.setCustomInt("killCount", 100);
context.setCustomLong("lastUsedTime", System.currentTimeMillis());
context.setCustomBoolean("isUpgraded", true);

// 读取
int kills = context.getCustomInt("killCount", 0);
long time = context.getCustomLong("lastUsedTime", 0L);
boolean upgraded = context.getCustomBoolean("isUpgraded", false);
```

### Q5: 如何处理事件？

**答**：重写 `onModuleEvent()` 方法。

```java
@Override
protected void onModuleEvent(@Nonnull Event event, @Nonnull ModuleContext context) {
    if (event instanceof LivingHurtEvent) {
        LivingHurtEvent hurtEvent = (LivingHurtEvent) event;
        // 处理伤害事件
    }
}
```

然后在事件处理器中分发：

```java
@SubscribeEvent
public void onLivingHurt(LivingHurtEvent event) {
    ItemStack core = CoreModuleItemHelper.getEquippedCore(player);
    ModulePlatform.getInstance().handleEvent(event, player, core);
}
```

### Q6: 模块之间如何通信？

**答**：通过 `CoreModuleItemHelper` 查询其他模块状态。

```java
@Override
protected void onModuleTick(@Nonnull ModuleContext context) {
    // 检查其他模块是否激活
    if (CoreModuleItemHelper.isModuleActive(context.getPlayer(), "SHIELD_MODULE")) {
        // 护盾模块激活时的特殊逻辑
    }
}
```

### Q7: 如何调试模块？

**答**：启用调试模式 + 使用调试工具。

```java
@Override
protected boolean isDebugMode() {
    return true;  // 启用调试
}

// 打印所有模块状态
CoreModuleItemHelper.debugPrintModules(player);

// 打印模块上下文
System.out.println(context.toString());
```

### Q8: 如何创建模块物品？

**答**：继承 `ModuleUpgradeItem` 并关联到运行时模块。

```java
// 1. 创建物品类
public class MyModuleItem extends ModuleUpgradeItem {
    public MyModuleItem() {
        super(MyModule.INSTANCE, 1);  // 关联模块，提供1级升级
        this.setDescriptions(
            TextFormatting.GRAY + "模块描述"
        );
    }
}

// 2. 注册物品
@SubscribeEvent
public static void registerItems(RegistryEvent.Register<Item> event) {
    event.getRegistry().register(
        new MyModuleItem()
            .setRegistryName("my_module")
            .setTranslationKey("my_module")
    );
}

// 3. 玩家右键使用物品即可升级核心
```

**物品会自动**：
- ✅ 显示模块信息的 tooltip
- ✅ 检查核心是否装备
- ✅ 检查是否可以升级
- ✅ 应用升级并消耗物品
- ✅ 播放音效和粒子效果
- ✅ 发送升级成功消息

---

## 总结

### 开发流程

1. **创建模块类** → 继承 `BaseUpgradeModule`
2. **实现核心逻辑** → 重写 `onModuleTick()`
3. **配置能量消耗** → 重写 `getBaseEnergyCost()`
4. **创建物品类**（可选） → 继承 `ModuleUpgradeItem`
5. **注册模块和物品** → `ModuleRegistry.getInstance().register()`
6. **初始化平台** → `ModulePlatform.getInstance().initialize()`
7. **集成到核心** → 在 `onWornTick()` 中调用平台

### 你只需要关心

- ✅ 创建模块类（继承 `BaseUpgradeModule`）
- ✅ 创建物品类（继承 `ModuleUpgradeItem`）
- ✅ 实现 `onModuleTick()` 等方法
- ✅ 使用 `ModuleContext` 访问信息
- ❌ 不需要关心 NBT
- ❌ 不需要关心能量消耗逻辑
- ❌ 不需要关心状态管理
- ❌ 不需要关心生命周期
- ❌ 不需要关心升级物品的右键逻辑

### 包装层帮你处理

**运行时系统**：
- ✅ 所有 NBT 读写
- ✅ 能量自动消耗
- ✅ 状态自动管理
- ✅ tick 和事件分发
- ✅ 生命周期管理
- ✅ 错误处理和日志

**物品系统**：
- ✅ Tooltip 自动生成（名称、类别、等级、能量消耗）
- ✅ 升级检查和应用逻辑
- ✅ 物品消耗处理
- ✅ 音效和粒子效果
- ✅ 玩家消息反馈

---

**封印完成！** 🎉 从现在开始，创建新模块只需要几行代码！
