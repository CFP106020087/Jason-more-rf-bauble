# MoreMod 事件系统（Event Hub）

## 📖 概述

这是一个完全可拔插、零耦合、独立的事件系统实现，专为 Forge 1.12.2 MoreMod 设计。

### ✨ 核心特性

- **✅ 完全可拔插**：删除 `eventhub` 包后，主 mod 自动 fallback 到 No-Op 实现
- **✅ 零耦合**：主 mod 只依赖接口，不依赖任何实现类
- **✅ Service Locator**：使用 Lazy Provider 模式，避免单例静态硬绑定
- **✅ 独立于 Forge**：不依赖 Forge EventBus，完全自主实现
- **✅ 线程安全**：使用 ConcurrentHashMap 和 CopyOnWriteArrayList
- **✅ 高性能**：事件类型映射，优先级排序
- **✅ 功能完整**：同步事件、优先级、注解、冒泡、可取消

---

## 📂 目录结构

```
src/main/java/com/moremod/
├── api/event/                      # 公共 API 层（主 mod 只能访问这里）
│   ├── IEvent.java                 # 事件接口
│   ├── ICancellableEvent.java      # 可取消事件接口
│   ├── IEventBus.java              # 事件总线接口（唯一入口）
│   ├── IEventListener.java         # 监听器注解
│   ├── EventPriority.java          # 优先级枚举
│   ├── EventService.java           # Service Locator
│   └── internal/
│       └── NoOpEventBus.java       # No-Op 实现（Fallback）
│
└── eventhub/                       # 实现层（可删除）
    ├── EventBusImpl.java           # 事件总线实现
    ├── EventBusProvider.java       # 实现注册器
    ├── internal/
    │   ├── ListenerMethod.java     # 监听器方法封装
    │   └── EventScanner.java       # 注解扫描器
    └── example/                    # 使用示例
        ├── PlayerLoginEvent.java
        ├── PlayerDamageEvent.java
        ├── ExampleListener.java
        └── ExampleUsage.java
```

---

## 🏗️ 架构设计

### 1. 分层设计

```
┌─────────────────────────────────────────┐
│         主 Mod（只能访问 API）            │
│  - 只依赖 IEventBus 和 IEvent 接口      │
│  - 通过 EventService.getBus() 获取实例   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         EventService (Service Locator)  │
│  - 延迟加载实现                          │
│  - 自动检测实现是否存在                   │
│  - 实现不存在时 fallback 到 NoOpEventBus │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         EventBusImpl（可拔插实现）        │
│  - 通过 EventBusProvider 自动注册        │
│  - 如果被删除，系统自动使用 NoOpEventBus  │
└─────────────────────────────────────────┘
```

### 2. 零耦合机制

**问题**：如何让主 mod 在事件系统被删除后仍能正常运行？

**解决方案**：
1. **接口隔离**：主 mod 只依赖 `IEventBus` 和 `IEvent` 接口
2. **Service Locator**：`EventService` 使用反射动态加载实现
3. **Fallback 机制**：如果实现类不存在（ClassNotFoundException），自动使用 `NoOpEventBus`
4. **No-Op 实现**：所有操作都是空操作，不会抛出异常

```java
// EventService.tryLoadImplementation()
try {
    Class<?> providerClass = Class.forName("com.moremod.eventhub.EventBusProvider");
    providerClass.getMethod("register").invoke(null);
} catch (ClassNotFoundException e) {
    // 实现不存在，使用 NoOpEventBus（静默失败）
}
```

### 3. Service Locator 模式

```java
// 主 mod 代码
IEventBus bus = EventService.getBus();  // 永远不会返回 null

// 如果 eventhub 存在 → 返回 EventBusImpl
// 如果 eventhub 被删除 → 返回 NoOpEventBus
```

---

## 🚀 快速开始

### 1. 创建事件类

```java
package com.moremod.event;

import com.moremod.api.event.ICancellableEvent;

public class PlayerLoginEvent implements ICancellableEvent {
    private final EntityPlayer player;
    private boolean cancelled = false;
    private String cancelReason = "";

    public PlayerLoginEvent(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
```

### 2. 创建监听器类

```java
package com.moremod.listener;

import com.moremod.api.event.IEventListener;
import com.moremod.api.event.EventPriority;
import com.moremod.event.PlayerLoginEvent;

public class SecurityListener {

    @IEventListener(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // 最高优先级，最先执行
        if (isPlayerBanned(event.getPlayer())) {
            event.cancel();
            event.setCancelReason("You are banned!");
        }
    }

    @IEventListener(priority = EventPriority.LOW, receiveCancelled = true)
    public void logPlayerLogin(PlayerLoginEvent event) {
        // 低优先级，用于日志记录
        // receiveCancelled = true 表示即使事件被取消也会执行
        if (event.isCancelled()) {
            System.out.println("Player login cancelled: " + event.getCancelReason());
        } else {
            System.out.println("Player logged in: " + event.getPlayer().getName());
        }
    }
}
```

### 3. 在主 Mod 中注册和使用

```java
package com.moremod;

import com.moremod.api.event.EventService;
import com.moremod.api.event.IEventBus;
import com.moremod.listener.SecurityListener;
import com.moremod.event.PlayerLoginEvent;

@Mod(modid = "moremod")
public class MoreMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 获取事件总线
        IEventBus bus = EventService.getBus();

        // 注册监听器
        bus.register(new SecurityListener());

        // 或者注册类（会自动实例化）
        // bus.registerClass(SecurityListener.class);

        System.out.println("Event system: " + EventService.getProviderInfo());
    }

    // 在需要触发事件的地方
    public void handlePlayerLogin(EntityPlayer player) {
        IEventBus bus = EventService.getBus();

        // 创建并触发事件
        PlayerLoginEvent event = new PlayerLoginEvent(player);
        bus.post(event);

        // 检查事件是否被取消
        if (event.isCancelled()) {
            player.connection.disconnect(event.getCancelReason());
        }
    }
}
```

---

## 📚 API 详解

### IEventBus 接口

```java
public interface IEventBus {
    // 注册监听器对象
    IEventBus register(Object listener);

    // 注销监听器对象
    IEventBus unregister(Object listener);

    // 触发事件
    <T extends IEvent> T post(T event);

    // 注册监听器类（自动实例化）
    IEventBus registerClass(Class<?> listenerClass);

    // 清除所有监听器
    IEventBus clear();
}
```

### EventPriority 枚举

```java
HIGHEST  (0)    // 最高优先级，最先执行
HIGH     (100)  // 高优先级
NORMAL   (500)  // 普通优先级（默认）
LOW      (900)  // 低优先级
LOWEST   (1000) // 最低优先级，最后执行
```

### @IEventListener 注解

```java
@IEventListener(
    priority = EventPriority.NORMAL,  // 优先级（默认 NORMAL）
    receiveCancelled = false          // 是否接收已取消的事件（默认 false）
)
public void onEvent(MyEvent event) {
    // 处理事件
}
```

---

## 🔧 高级用法

### 1. 事件继承

事件系统支持事件继承，父类事件的监听器也会被触发：

```java
public class BaseEvent implements IEvent { }
public class ChildEvent extends BaseEvent { }

// 监听器会同时接收 BaseEvent 和 ChildEvent
@IEventListener
public void onBaseEvent(BaseEvent event) {
    // 会被 BaseEvent 和 ChildEvent 触发
}

@IEventListener
public void onChildEvent(ChildEvent event) {
    // 只会被 ChildEvent 触发
}
```

### 2. 优先级与取消

```java
@IEventListener(priority = EventPriority.HIGHEST)
public void firstListener(MyEvent event) {
    // 最先执行
    if (someCondition) {
        event.cancel();  // 取消事件
    }
}

@IEventListener(priority = EventPriority.NORMAL)
public void normalListener(MyEvent event) {
    // 如果事件被取消，这个方法不会执行（因为 receiveCancelled = false）
}

@IEventListener(priority = EventPriority.LOWEST, receiveCancelled = true)
public void monitorListener(MyEvent event) {
    // 最后执行，即使事件被取消也会执行
    // 适合用于日志记录
}
```

### 3. 运行时检查

```java
// 检查是否使用真正的事件系统实现
if (EventService.isRealImplementation()) {
    System.out.println("Event system enabled");
} else {
    System.out.println("Event system disabled (using No-Op)");
}

// 获取提供者信息
System.out.println(EventService.getProviderInfo());
```

### 4. 调试模式

启用调试模式查看事件触发详情：

```bash
java -DeventBus.debug=true -jar minecraft.jar
```

---

## 🧪 测试可拔插性

### 测试步骤

1. **正常运行**（eventhub 存在）
   ```bash
   # 编译并运行
   ./gradlew build
   # 日志应显示：Event system loaded: EventBusImpl
   ```

2. **删除 eventhub 包**
   ```bash
   rm -rf src/main/java/com/moremod/eventhub
   ```

3. **重新编译和运行**
   ```bash
   ./gradlew build
   # 日志应显示：Event system not available, using No-Op implementation
   # 主 mod 仍能正常编译和运行！
   ```

4. **验证 Fallback**
   ```java
   IEventBus bus = EventService.getBus();
   System.out.println(bus);  // 输出：NoOpEventBus[disabled]

   // 所有操作都是 No-Op，不会抛出异常
   bus.register(new MyListener());  // 不会注册
   bus.post(new MyEvent());         // 不会触发
   ```

---

## 🎯 设计原则

### 1. 依赖倒置原则（DIP）
- 主 mod 只依赖抽象接口（IEventBus、IEvent）
- 不依赖具体实现类（EventBusImpl）

### 2. 开闭原则（OCP）
- 对扩展开放：可以轻松添加新的事件类型和监听器
- 对修改封闭：不需要修改核心代码

### 3. 单一职责原则（SRP）
- EventService：负责定位实现
- EventBusImpl：负责事件分发
- ListenerMethod：负责方法调用
- EventScanner：负责注解扫描

### 4. 接口隔离原则（ISP）
- IEvent：最小化接口
- ICancellableEvent：只在需要时继承
- IEventBus：只暴露必要方法

---

## ⚠️ 注意事项

### 1. 主 mod 依赖规则

**✅ 允许：**
```java
import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventBus;
import com.moremod.api.event.ICancellableEvent;
import com.moremod.api.event.IEventListener;
import com.moremod.api.event.EventPriority;
import com.moremod.api.event.EventService;
```

**❌ 禁止：**
```java
import com.moremod.eventhub.EventBusImpl;           // 禁止！
import com.moremod.eventhub.EventBusProvider;       // 禁止！
import com.moremod.eventhub.internal.*;             // 禁止！
```

### 2. 方法签名要求

监听器方法必须：
- 标记 `@IEventListener` 注解
- 有且仅有一个参数
- 参数必须实现 `IEvent` 接口
- 可以是私有方法（会自动设置为可访问）

```java
// ✅ 正确
@IEventListener
public void onEvent(MyEvent event) { }

@IEventListener
private void onEvent(MyEvent event) { }  // 私有方法也支持

// ❌ 错误
@IEventListener
public void onEvent() { }  // 缺少参数

@IEventListener
public void onEvent(MyEvent e1, MyEvent e2) { }  // 参数过多

@IEventListener
public void onEvent(String event) { }  // 参数不是 IEvent
```

### 3. 线程安全

- EventBusImpl 是线程安全的
- 可以在多线程环境中注册/注销监听器
- 可以在多线程环境中触发事件
- 监听器方法的线程安全需要自己保证

---

## 🔄 与 Forge EventBus 的对比

| 特性 | MoreMod EventHub | Forge EventBus |
|------|------------------|----------------|
| 可拔插 | ✅ 完全可删除 | ❌ 硬绑定 |
| 零耦合 | ✅ 接口隔离 | ❌ 依赖 Forge |
| Service Locator | ✅ Lazy Provider | ❌ 单例静态 |
| Fallback | ✅ No-Op 实现 | ❌ 无 |
| 自主实现 | ✅ 不依赖 Forge | ❌ 依赖 Forge |
| 线程安全 | ✅ 完全线程安全 | ✅ 线程安全 |
| 优先级 | ✅ 支持 | ✅ 支持 |
| 可取消 | ✅ 支持 | ✅ 支持 |

---

## 📝 常见问题

### Q: 删除 eventhub 包后会影响主 mod 吗？
A: 不会。主 mod 会自动 fallback 到 NoOpEventBus，所有操作都变成空操作，不会抛出异常。

### Q: 为什么不直接使用 Forge EventBus？
A: 需求要求"不依赖 Forge EventBus"，并且要求"可拔插、零耦合"。

### Q: 如何确保真正做到零耦合？
A: 主 mod 只能 import `com.moremod.api.event.*`，不能 import `com.moremod.eventhub.*`。

### Q: EventService 如何找到实现类？
A: 使用反射尝试加载 `com.moremod.eventhub.EventBusProvider` 类，如果找不到（ClassNotFoundException），就使用 NoOpEventBus。

### Q: 可以在运行时切换实现吗？
A: 可以，使用 `EventService.reset()` 重置，然后 `EventService.setProvider()` 设置新的提供者。

---

## 📄 许可证

此事件系统是 MoreMod 的一部分，遵循 MoreMod 的许可证。

---

## 👨‍💻 贡献

欢迎提交 Issue 和 Pull Request！

---

**享受事件驱动的编程乐趣！** 🎉
