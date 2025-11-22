# MoreMod 能力系统框架 (Capability Framework)

## 概述

这是一个完全可拔插的能力系统，具有零耦合和自动 fallback 特性。

## 设计原则

### 1. 零耦合架构
- **主 mod 只能看到接口**：`ICapability`, `ICapabilityContainer`, `ICapabilityProvider`, `ICapabilityDescriptor`, `ICapabilityRegistry`
- **实现层完全可删除**：删除 `com.moremod.capability.framework` 包后，系统自动使用 NoOp 容器
- **通过 Service Locator 访问**：使用 `CapabilityService` 作为唯一入口

### 2. 可拔插性
- 删除整个 framework 包后，主 mod 不会崩溃
- 自动 fallback 到 `NoOpCapabilityContainer`
- 不影响其他系统的运行

### 3. 描述符注册
- 不通过 Class 注册，而是通过 `ICapabilityDescriptor`
- 支持条件附加（根据宿主判断是否附加能力）
- 支持优先级排序

## 包结构

```
com.moremod.api.capability/          # 接口层（必须存在）
├── ICapability.java                 # 能力接口
├── ICapabilityProvider.java         # 能力提供者接口
├── ICapabilityContainer.java        # 能力容器接口
├── ICapabilityDescriptor.java       # 能力描述符接口
├── ICapabilityRegistry.java         # 能力注册表接口
├── NoOpCapabilityContainer.java     # NoOp 容器（fallback）
└── CapabilityService.java           # Service Locator

com.moremod.capability.framework/    # 实现层（可删除）
├── CapabilityRegistryImpl.java      # 注册表实现
├── CapabilityContainerImpl.java     # 容器实现
├── CapabilityDescriptorImpl.java    # 描述符实现
├── BaseCapability.java              # 能力基类
├── CapabilityFrameworkInit.java     # 初始化类
├── CapabilityContainerProvider.java # Forge Capability 桥接
└── example/                         # 示例实现
    ├── IExampleCapability.java
    ├── ExampleCapabilityImpl.java
    └── ExampleCapabilityProvider.java
```

## 使用指南

### 1. 初始化能力系统

在主 mod 类的 `preInit` 方法中：

```java
@EventHandler
public void preInit(FMLPreInitializationEvent event) {
    // 初始化能力系统
    CapabilityFrameworkInit.preInit();
}

@EventHandler
public void init(FMLInitializationEvent event) {
    // 冻结注册表
    CapabilityFrameworkInit.init();
}
```

### 2. 定义自定义能力

**步骤 1：定义能力接口**

```java
public interface IMyCapability extends ICapability<EntityPlayer> {
    int getValue();
    void setValue(int value);
}
```

**步骤 2：实现能力**

```java
public class MyCapabilityImpl extends BaseCapability<EntityPlayer> implements IMyCapability {
    private int value = 0;

    public MyCapabilityImpl() {
        super("moremod:my_capability");
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
        markDirty(); // 标记为脏，触发同步
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt) {
        nbt.setInteger("Value", value);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        value = nbt.getInteger("Value");
    }

    @Override
    public boolean shouldSync() {
        return true; // 需要同步到客户端
    }
}
```

**步骤 3：创建提供者**

```java
public class MyCapabilityProvider implements ICapabilityProvider<EntityPlayer, IMyCapability> {
    @Override
    public IMyCapability createCapability(EntityPlayer host) {
        return new MyCapabilityImpl();
    }

    @Override
    public Class<IMyCapability> getCapabilityType() {
        return IMyCapability.class;
    }

    @Override
    public String getCapabilityId() {
        return "moremod:my_capability";
    }
}
```

**步骤 4：注册能力**

```java
ICapabilityRegistry registry = CapabilityService.getRegistry();

ICapabilityDescriptor<EntityPlayer> descriptor =
    CapabilityDescriptorImpl.builder(
        "moremod:my_capability",
        new MyCapabilityProvider(),
        EntityPlayer.class
    )
    .priority(100)                  // 优先级
    .autoSerialize(true)            // 自动序列化
    .autoSync(true)                 // 自动同步
    .attachCondition(player -> {    // 附加条件
        return !player.isCreative(); // 仅非创造模式玩家
    })
    .description("My custom capability")
    .build();

registry.registerCapability(descriptor);
```

### 3. 使用能力

**获取能力容器：**

```java
EntityPlayer player = ...;
ICapabilityContainer<EntityPlayer> container =
    player.getCapability(CapabilityContainerProvider.CAPABILITY, null);
```

**获取特定能力：**

```java
// 通过类型获取
IMyCapability capability = container.getCapability(IMyCapability.class);

// 通过 ID 获取
ICapability<EntityPlayer> capability = container.getCapability("moremod:my_capability");
```

**检查能力是否存在：**

```java
if (container.hasCapability(IMyCapability.class)) {
    // 能力存在
}
```

**动态附加能力：**

```java
IMyCapability newCapability = new MyCapabilityImpl();
container.attachCapability(newCapability);
```

### 4. 高级功能

**能力 Tick 更新：**

```java
@Override
public void tick(EntityPlayer host) {
    // 每 tick 调用一次
    if (value < 100) {
        value++;
    }
}
```

**能力复制（玩家重生）：**

```java
@Override
public ICapability<EntityPlayer> copyTo(EntityPlayer host) {
    MyCapabilityImpl copy = new MyCapabilityImpl();
    copy.value = this.value;
    return copy;
}
```

**生命周期钩子：**

```java
@Override
public void onAttached(EntityPlayer host) {
    // 能力被附加到宿主时调用
    super.onAttached(host);
    System.out.println("Capability attached to " + host.getName());
}

@Override
public void onDetached(EntityPlayer host) {
    // 能力从宿主移除时调用
    super.onDetached(host);
    System.out.println("Capability detached from " + host.getName());
}
```

## 测试可拔插性

### 验证 Fallback 机制

1. **正常运行**：能力系统完整存在
   ```
   [INFO] CapabilityService initialized with full implementation
   [INFO] Registered 1 capabilities
   ```

2. **删除实现层**：删除 `com.moremod.capability.framework` 包
   ```
   [WARN] Capability framework implementation not found, using NoOp fallback
   ```

3. **系统行为**：
   - 主 mod 正常启动，不会崩溃
   - 所有 `getCapability()` 返回 `null`
   - 所有 `hasCapability()` 返回 `false`
   - 不影响其他系统运行

### 检查 Fallback 状态

```java
if (CapabilityService.isUsingFallback()) {
    System.out.println("Running in NoOp mode - capabilities disabled");
} else {
    System.out.println("Full capability system active");
}
```

## 与模块系统兼容

能力系统设计为与模块系统完全兼容但不耦合：

- **不依赖模块系统**：可以独立使用
- **可被模块扩展**：模块可以注册新能力
- **模块可选**：删除模块不影响能力系统核心

## 最佳实践

1. **接口优先**：始终定义能力接口，不要直接使用实现类
2. **使用描述符**：通过描述符注册，而非直接注册 Class
3. **合理使用同步**：只对需要客户端显示的数据启用同步
4. **序列化优化**：只序列化必要数据
5. **错误处理**：能力可能为 null，始终检查返回值
6. **性能考虑**：避免在 tick 中进行重度计算

## 常见问题

**Q: 为什么能力为 null？**
A: 可能原因：1) 能力未注册 2) 附加条件不满足 3) 运行在 fallback 模式

**Q: 如何确保能力已注册？**
A: 在注册后检查：`CapabilityService.getRegistry().isRegistered("capability_id")`

**Q: 能力数据没有保存？**
A: 确保实现了 `serializeNBT()` 和 `deserializeNBT()`，且 `autoSerialize` 为 true

**Q: 客户端看不到能力数据？**
A: 确保 `shouldSync()` 返回 true 且 `autoSync` 为 true

## 架构优势

1. **零耦合**：主 mod 完全不依赖实现
2. **高可靠**：实现缺失时自动 fallback
3. **易扩展**：通过描述符轻松添加新能力
4. **模块化**：可以单独删除而不影响其他系统
5. **灵活性**：支持条件附加、优先级、动态加载

## 许可证

此能力系统框架遵循 MoreMod 的许可证。
