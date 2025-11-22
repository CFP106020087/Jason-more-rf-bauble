# MoreMod 模块运行时系统（Module Runtime）

完全可拔插、与事件系统和能力系统软集成的模块架构。

## 📋 目录结构

```
com.moremod.module/
├── api/                    # 接口层（稳定API）
│   ├── IModule.java       # 模块接口
│   ├── IModuleHost.java   # 模块宿主接口
│   ├── IModuleContext.java # 模块上下文接口
│   ├── IModuleDescriptor.java # 模块描述符接口
│   └── IModuleContainer.java  # 模块容器接口
├── base/                   # 基础类
│   └── AbstractModule.java # 抽象模块基类
├── impl/                   # 实现层（可拔插）
│   ├── ModuleContainerImpl.java # 容器实现
│   ├── ModuleContextImpl.java   # 上下文实现
│   └── ModuleDescriptorImpl.java # 描述符实现
├── fallback/              # No-Op 层（失败安全）
│   ├── NoOpModuleContainer.java
│   └── NoOpModuleContext.java
├── service/               # Service Locator
│   └── ModuleService.java
├── integration/           # 软集成层
│   ├── EventBusIntegration.java  # 事件系统软集成
│   └── CapabilityIntegration.java # 能力系统软集成
├── host/                  # 宿主实现
│   └── PlayerModuleHost.java # 玩家宿主
├── example/              # 示例模块
│   └── EnergyBoostModule.java
└── ModuleSystemInitializer.java # 初始化器
```

## ✨ 核心特性

### 1. 完全可拔插
- ✅ 删除整个 `module/` 目录后游戏**不会崩溃**
- ✅ 使用 No-Op Fallback 确保所有调用安全
- ✅ 通过 Service Locator 访问，无硬依赖

### 2. 软集成机制
- ✅ **事件系统**: 通过反射检测 Forge EventBus，不存在时自动降级
- ✅ **能力系统**: 通过反射检测 Capability，不存在时自动降级
- ✅ 模块可选地使用事件或能力，无强制要求

### 3. 灵活的宿主系统
- ✅ 支持多种宿主类型（玩家、物品、世界等）
- ✅ 不硬编码宿主，通过接口抽象
- ✅ 宿主数据自动管理和持久化

### 4. 失败安全
- ✅ 单个模块失败不影响其他模块
- ✅ 模块初始化/加载/运行时异常被捕获
- ✅ 所有公共 API 提供 Null-Safe 保证

## 🚀 快速开始

### 1. 初始化模块系统

在你的 Mod 主类中：

```java
@Mod(modid = "moremod", name = "MoreMod", version = "1.0")
public class MoreMod {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 初始化模块系统
        ModuleSystemInitializer.initialize(event.getSide().isClient());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // 加载所有模块
        ModuleSystemInitializer.loadModules();
    }
}
```

### 2. 创建自定义模块

```java
public class MyCustomModule extends AbstractModule {

    public MyCustomModule() {
        super("moremod:my_module", "我的模块");
    }

    @Override
    public void onTick(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // 模块逻辑
        if (host instanceof PlayerModuleHost) {
            EntityPlayer player = ((PlayerModuleHost) host).getPlayer();
            // 对玩家执行操作...
        }
    }
}
```

### 3. 注册模块

在 `ModuleSystemInitializer.registerModules()` 中：

```java
private static void registerModules() {
    container.registerModule(new MyCustomModule());
}
```

### 4. 在游戏中使用模块

```java
// 方法1: 通过 Service Locator
IModule module = ModuleService.getModule("moremod:my_module");
if (module != null) {
    // 使用模块...
}

// 方法2: 为玩家附加模块
IModuleContainer container = ModuleService.getContainer();
IModuleContext context = ModuleService.getContext();
IModuleHost host = new PlayerModuleHost(player);

container.attachAll(host, context);  // 附加所有模块
container.tickAll(host, context);    // tick所有模块
```

## 📚 模块生命周期

```
注册 → 初始化 → 加载 → 附加 → tick → 分离 → 卸载
   ↓      ↓       ↓      ↓      ↓      ↓      ↓
register init   load  attach  onTick detach unload
```

- **register**: 模块注册到容器
- **init**: 一次性初始化（注册监听器等）
- **load**: 加载配置和资源
- **attach**: 附加到具体宿主（玩家等）
- **onTick**: 每tick更新
- **detach**: 从宿主分离
- **unload**: 卸载资源

## 🔌 软集成示例

### 事件系统集成

```java
public class MyModule extends AbstractModule {

    @Override
    public boolean init(@Nonnull IModuleContext context) {
        // 尝试注册事件监听器
        if (EventBusIntegration.isAvailable()) {
            EventBusIntegration.registerModuleListener(this, context);
        }
        return super.init(context);
    }

    @SubscribeEvent  // Forge 事件注解
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 事件处理逻辑
    }
}
```

### Capability 集成

```java
public class MyModule extends AbstractModule {

    @Override
    public void onTick(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        if (CapabilityIntegration.isAvailable()) {
            Object capability = CapabilityIntegration.getCapability(
                host.getNativeHost(), "energy", context
            );
            // 使用 capability...
        }
    }
}
```

## 🛡️ 失败安全机制

### No-Op Fallback

当模块系统不可用时，所有调用自动降级到 No-Op 实现：

```java
// 即使模块系统未初始化，这也不会崩溃
IModuleContainer container = ModuleService.getContainer();
container.tickAll(host, context);  // 安全的空操作
```

### 异常处理

模块容器自动捕获并记录异常：

```java
// 单个模块崩溃不会影响其他模块
public void tickAll() {
    for (IModule module : modules.values()) {
        try {
            module.onTick(host, context);
        } catch (Throwable t) {
            log("error", "Module tick failed: " + t.getMessage());
            // 继续处理其他模块
        }
    }
}
```

## 📝 配置与服务

### 服务注册

```java
ModuleContextImpl context = new ModuleContextImpl(container, false);

// 注册服务
context.registerService(MyService.class, myServiceInstance);
context.registerService("custom_service", customObject);
```

### 服务获取

```java
// 在模块中
MyService service = context.getService(MyService.class);
if (service != null) {
    service.doSomething();
}
```

## 🧪 测试模块系统

### 测试模块系统是否工作

```java
// 检查是否初始化
if (ModuleService.isAvailable()) {
    System.out.println("Module system is ready!");
}

// 获取所有模块
Collection<IModule> modules = ModuleService.getContainer().getAllModules();
System.out.println("Loaded modules: " + modules.size());
```

### 测试模块删除安全性

1. 删除整个 `com.moremod.module/` 目录
2. 重新编译并运行游戏
3. 游戏应正常启动，不报错
4. 所有 `ModuleService` 调用返回 No-Op 实现

## 🎯 最佳实践

1. **模块应该独立**: 每个模块应能独立运行，不依赖其他模块
2. **优雅降级**: 当依赖服务不可用时，模块应优雅降级
3. **异常处理**: 模块内部应捕获并处理异常，不抛出到容器
4. **资源清理**: 在 `unload()` 中清理所有资源
5. **线程安全**: 模块应考虑多线程访问的情况

## 📞 常见问题

### Q: 模块系统崩溃了怎么办？
A: 系统设计为失败安全，会自动降级到 No-Op 实现。检查日志获取详细错误信息。

### Q: 如何让模块在能量耗尽时停止？
A: 在模块的 `onTick()` 中检查能量，返回 false 或调用 `setActive(false)`。

### Q: 模块可以与现有的升级系统共存吗？
A: 完全可以！模块系统独立于现有系统，可以无缝集成。

### Q: 如何调试模块？
A: 启用 debug 模式：`new ModuleContainerImpl(true)` 和 `new ModuleContextImpl(container, false, true)`

## 📄 许可证

本模块系统是 MoreMod 的一部分，遵循项目的开源许可证。

---

**作者**: MoreMod Team
**版本**: 1.0.0
**最后更新**: 2025-01-22
