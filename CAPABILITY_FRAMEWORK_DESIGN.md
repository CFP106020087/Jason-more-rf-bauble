# MoreMod 能力系统框架 - 设计文档

## 设计目标

为 MoreMod 创建一个**完全可拔插的能力系统**，实现与事件系统相同的零耦合和可替换性。

## 核心要求

### ✅ 硬性要求

1. **接口隔离**
   - 主 mod 只能看到 `ICapability` 和 `ICapabilityContainer`
   - 不允许看到具体实现类
   - 通过接口实现完全解耦

2. **可拔插性**
   - 删除整个 `capability.framework` 包后，主 mod 自动 fallback
   - Fallback 到 `NoOpCapabilityContainer`
   - 不影响其他系统运行，不会崩溃

3. **零耦合**
   - 模块间不允许硬绑定
   - 必须通过接口注册与查找
   - 使用 Service Locator 模式

4. **描述符注册**
   - 所有能力以 `Descriptor` 注册
   - 不写死 class
   - 支持条件附加和优先级

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────────┐
│           主 Mod（MoreMod）                  │
│   - 只能看到接口层                            │
│   - 通过 CapabilityService 访问               │
└─────────────────────────────────────────────┘
                    ↓ 使用
┌─────────────────────────────────────────────┐
│          Service Locator 层                  │
│   - CapabilityService                        │
│   - 自动检测实现层是否存在                     │
│   - 实现层缺失时自动 fallback                 │
└─────────────────────────────────────────────┘
                    ↓ 定位
┌─────────────────────────────────────────────┐
│          接口层（API Package）                │
│   - ICapability                              │
│   - ICapabilityProvider                      │
│   - ICapabilityContainer                     │
│   - ICapabilityDescriptor                    │
│   - ICapabilityRegistry                      │
│   - NoOpCapabilityContainer (Fallback)       │
└─────────────────────────────────────────────┘
                    ↑ 实现
┌─────────────────────────────────────────────┐
│       实现层（Framework Package）【可删除】   │
│   - CapabilityRegistryImpl                   │
│   - CapabilityContainerImpl                  │
│   - CapabilityDescriptorImpl                 │
│   - BaseCapability                           │
│   - CapabilityFrameworkInit                  │
└─────────────────────────────────────────────┘
```

### 包结构

```
com.moremod.api.capability/              【必须存在】
├── ICapability.java                     核心接口
├── ICapabilityProvider.java             提供者接口
├── ICapabilityContainer.java            容器接口
├── ICapabilityDescriptor.java           描述符接口
├── ICapabilityRegistry.java             注册表接口
├── NoOpCapabilityContainer.java         Fallback 实现
└── CapabilityService.java               Service Locator

com.moremod.capability.framework/        【可删除】
├── CapabilityRegistryImpl.java          注册表实现
├── CapabilityContainerImpl.java         容器实现
├── CapabilityDescriptorImpl.java        描述符实现
├── BaseCapability.java                  基类（可选）
├── CapabilityFrameworkInit.java         初始化
├── CapabilityContainerProvider.java     Forge 桥接
└── example/                             示例
    ├── IExampleCapability.java
    ├── ExampleCapabilityImpl.java
    ├── ExampleCapabilityProvider.java
    └── ExampleUsage.java
```

## 核心接口

### 1. ICapability<T>

能力基础接口，所有自定义能力必须实现。

```java
public interface ICapability<T> {
    String getCapabilityId();
    void serializeNBT(NBTTagCompound nbt);
    void deserializeNBT(NBTTagCompound nbt);
    boolean shouldSync();
    ICapability<T> copyTo(T host);
    void tick(T host);
}
```

### 2. ICapabilityContainer<T>

能力容器接口，管理附加在对象上的所有能力。

```java
public interface ICapabilityContainer<T> {
    <C extends ICapability<T>> C getCapability(Class<C> capabilityType);
    ICapability<T> getCapability(String capabilityId);
    boolean hasCapability(Class<? extends ICapability<T>> capabilityType);
    boolean attachCapability(ICapability<T> capability);
    Collection<ICapability<T>> getAllCapabilities();
    void serializeNBT(NBTTagCompound nbt);
    void tick();
}
```

### 3. ICapabilityDescriptor<T>

能力描述符接口，用于注册能力而不暴露具体实现。

```java
public interface ICapabilityDescriptor<T> {
    String getCapabilityId();
    ICapabilityProvider<T, ? extends ICapability<T>> getProvider();
    Class<T> getHostType();
    boolean shouldAttachTo(T host);
    int getPriority();
}
```

### 4. ICapabilityRegistry

全局注册表接口。

```java
public interface ICapabilityRegistry {
    <T> boolean registerCapability(ICapabilityDescriptor<T> descriptor);
    <T> ICapabilityDescriptor<T> getDescriptor(String capabilityId);
    <T> Collection<ICapabilityDescriptor<T>> getDescriptorsForHost(Class<T> hostType);
    void freeze();
}
```

## Fallback 机制

### 工作原理

1. **正常模式**：实现层存在
   ```
   CapabilityService.initialize()
   → 检测到 CapabilityRegistryImpl
   → 使用完整实现
   → usingFallback = false
   ```

2. **Fallback 模式**：实现层缺失
   ```
   CapabilityService.initialize()
   → ClassNotFoundException
   → 使用 NoOpCapabilityRegistry
   → 创建 NoOpCapabilityContainer
   → usingFallback = true
   ```

### NoOpCapabilityContainer

所有操作都是无操作的（No-Op）：

```java
public final class NoOpCapabilityContainer<T> implements ICapabilityContainer<T> {
    public <C extends ICapability<T>> C getCapability(Class<C> type) {
        return null;  // 总是返回 null
    }

    public boolean hasCapability(String id) {
        return false; // 总是返回 false
    }

    // 其他方法都是 NoOp
}
```

## 使用流程

### 初始化（PreInit 阶段）

```java
// 在主 mod 类中
@EventHandler
public void preInit(FMLPreInitializationEvent event) {
    CapabilityFrameworkInit.preInit();
}
```

### 注册能力

```java
ICapabilityDescriptor<EntityPlayer> descriptor =
    CapabilityDescriptorImpl.builder(
        "moremod:my_capability",
        new MyCapabilityProvider(),
        EntityPlayer.class
    )
    .priority(100)
    .autoSerialize(true)
    .autoSync(true)
    .attachCondition(player -> !player.isCreative())
    .build();

CapabilityService.getRegistry().registerCapability(descriptor);
```

### 使用能力

```java
EntityPlayer player = ...;
ICapabilityContainer<EntityPlayer> container =
    player.getCapability(CapabilityContainerProvider.CAPABILITY, null);

if (container != null) {
    IMyCapability cap = container.getCapability(IMyCapability.class);
    if (cap != null) {
        cap.doSomething();
    }
}
```

## 功能特性

### ✅ 动态挂载

```java
container.attachCapability(new CustomCapability());
```

### ✅ 自动序列化（可选）

```java
.autoSerialize(true)  // 在描述符中配置

// 能力类实现
@Override
public void serializeNBT(NBTTagCompound nbt) {
    nbt.setInteger("data", data);
}
```

### ✅ 自动同步（可选）

```java
.autoSync(true)  // 在描述符中配置

// 能力类实现
@Override
public boolean shouldSync() {
    return true;
}
```

### ✅ 与模块系统兼容（但不耦合）

- 能力系统不依赖模块系统
- 模块可以注册新能力
- 删除模块不影响能力系统核心

## 测试验证

### 测试 1：正常运行

```
预期输出：
[INFO] CapabilityService initialized with full implementation
[INFO] Registered 1 capabilities
[INFO] Capability registry frozen
```

### 测试 2：删除实现层

删除 `com.moremod.capability.framework` 包

```
预期输出：
[WARN] Capability framework implementation not found, using NoOp fallback
```

验证：
- ✅ 主 mod 正常启动
- ✅ 不抛出异常
- ✅ 所有 `getCapability()` 返回 `null`
- ✅ 其他系统正常运行

### 测试 3：检查 Fallback 状态

```java
if (CapabilityService.isUsingFallback()) {
    System.out.println("NoOp mode");
} else {
    System.out.println("Full system active");
}
```

## 设计优势

### 1. 零耦合
- 主 mod 只依赖接口
- 实现层完全可选
- 通过反射动态加载

### 2. 高可靠
- 实现缺失时自动 fallback
- 不会崩溃或抛异常
- 优雅降级

### 3. 易扩展
- 通过描述符注册
- 支持条件附加
- 支持优先级排序

### 4. 模块化
- 可以单独删除
- 不影响其他系统
- 符合单一职责原则

### 5. 灵活性
- 支持动态加载
- 支持运行时附加
- 支持生命周期管理

## 与事件系统的对比

| 特性 | 能力系统 | 事件系统 |
|------|---------|---------|
| 零耦合 | ✅ | ✅ |
| 可拔插 | ✅ | ✅ |
| Fallback | ✅ NoOp Container | ✅ NoOp Bus |
| 注册方式 | Descriptor | Subscriber |
| 动态性 | 支持动态附加 | 支持动态订阅 |
| 序列化 | 内置支持 | 不适用 |
| 同步 | 内置支持 | 不适用 |

## 未来扩展

### 可能的增强功能

1. **网络同步包**
   - 自动生成同步数据包
   - 支持增量同步

2. **能力依赖系统**
   - 能力之间的依赖关系
   - 自动解析依赖顺序

3. **能力组合**
   - 多个能力的组合效果
   - 能力之间的交互

4. **调试工具**
   - 能力可视化
   - 运行时诊断

5. **性能优化**
   - 能力缓存
   - 懒加载机制

## 总结

此能力系统完全满足所有硬性要求：

- ✅ 主 mod 只看到接口，不看到实现
- ✅ 删除实现层后自动 fallback 到 NoOp
- ✅ 零耦合，通过 Service Locator 访问
- ✅ 通过 Descriptor 注册，不写死 class
- ✅ 支持动态挂载、序列化、同步
- ✅ 与模块系统兼容但不耦合

系统设计遵循 SOLID 原则，具有高内聚、低耦合的特性，易于维护和扩展。
