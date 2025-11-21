# Synergy GUI 集成指南

已创建完整的 GUI 和方块代码，注册部分由你自己完成。

## 📦 已创建的文件

### 1. 方块和 TileEntity
- `com.moremod.synergy.block.BlockSynergyLinker` - Synergy Linker 方块
- `com.moremod.synergy.tile.TileEntitySynergyLinker` - TileEntity（简单标记）

### 2. 容器和 GUI
- `com.moremod.synergy.container.ContainerSynergyLinker` - 服务端容器
- `com.moremod.synergy.gui.GuiSynergyLinker` - 客户端 GUI

### 3. 网络通信
- `com.moremod.synergy.network.PacketToggleSynergy` - 切换 Synergy 激活状态的网络包

---

## 🔧 集成步骤

### 步骤 1: 注册方块

在你的方块注册代码中添加：

```java
public static Block SYNERGY_LINKER;

@SubscribeEvent
public static void registerBlocks(RegistryEvent.Register<Block> event) {
    SYNERGY_LINKER = new BlockSynergyLinker();
    event.getRegistry().register(SYNERGY_LINKER);

    // 如果使用 ItemBlock
    GameRegistry.registerTileEntity(
        TileEntitySynergyLinker.class,
        new ResourceLocation("moremod", "synergy_linker")
    );
}
```

### 步骤 2: 注册 ItemBlock

```java
@SubscribeEvent
public static void registerItems(RegistryEvent.Register<Item> event) {
    event.getRegistry().register(
        new ItemBlock(SYNERGY_LINKER)
            .setRegistryName(SYNERGY_LINKER.getRegistryName())
    );
}
```

### 步骤 3: 注册 GUI Handler

在 `GuiHandler.java` 中添加：

```java
// 在常量区域添加
public static final int SYNERGY_LINKER_GUI = 29; // 选择一个未使用的 ID

// 在 getServerGuiElement() 中添加
case SYNERGY_LINKER_GUI: {
    result = new ContainerSynergyLinker(player);
    break;
}

// 在 getClientGuiElement() 中添加
case SYNERGY_LINKER_GUI: {
    result = new GuiSynergyLinker(player);
    break;
}
```

### 步骤 4: 修改 BlockSynergyLinker.onBlockActivated()

在 `BlockSynergyLinker.java` 的 `onBlockActivated()` 方法中，取消注释并修改为你的 mod 实例：

```java
@Override
public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                float hitX, float hitY, float hitZ) {
    if (!worldIn.isRemote) {
        playerIn.openGui(
            moremod.instance,              // 你的 mod 实例
            GuiHandler.SYNERGY_LINKER_GUI, // GUI ID
            worldIn,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
    }
    return true;
}
```

### 步骤 5: 注册网络包

在你的网络通道注册代码中添加：

```java
// 假设你的网络通道是 INSTANCE
INSTANCE.registerMessage(
    PacketToggleSynergy.Handler.class,
    PacketToggleSynergy.class,
    nextId++, // 你的包 ID
    Side.SERVER
);
```

### 步骤 6: 修改 GuiSynergyLinker.toggleSynergy()

在 `GuiSynergyLinker.java` 的 `toggleSynergy()` 方法中，取消注释网络包发送：

```java
private void toggleSynergy(String synergyId) {
    // 发送网络包到服务端
    PacketHandler.INSTANCE.sendToServer(new PacketToggleSynergy(synergyId));

    // 更新按钮显示（客户端预测）
    if (playerData.isSynergyActivated(synergyId)) {
        playerData.deactivateSynergy(synergyId);
    } else {
        playerData.activateSynergy(synergyId);
    }

    updateButtons();
}
```

---

## 🎨 可选：创建 GUI 背景贴图

如果想要自定义背景，创建贴图文件：
```
src/main/resources/assets/moremod/textures/gui/synergy_linker.png
```

尺寸：176x166 像素

如果不需要贴图，GUI 会使用纯色背景（已实现）。

---

## 📝 使用说明

### 玩家使用流程

1. **放置方块**
   - 将 Synergy Linker 方块放置在世界中

2. **打开 GUI**
   - 右键点击方块

3. **激活 Synergy**
   - GUI 显示所有可用的 Synergy
   - 点击按钮切换激活状态
   - `[ON]` 表示已激活（绿色）
   - `[OFF]` 表示未激活（灰色）

4. **滚动列表**
   - 如果 Synergy 超过 5 个，使用 ↑↓ 按钮滚动

5. **查看信息**
   - 鼠标悬停在按钮上查看 Synergy 描述

---

## 🔍 技术细节

### GUI 功能特性

- ✅ 显示所有注册的 Synergy
- ✅ 一键激活/停用
- ✅ 滚动列表支持（超过 5 个时）
- ✅ 实时状态显示（绿色/灰色）
- ✅ 鼠标悬停提示
- ✅ 网络同步到服务端

### 数据存储

- 激活状态存储在 **玩家 NBT** 中
- TileEntity **不存储**任何数据
- 多人游戏安全（每个玩家独立数据）

### 网络通信

- 客户端点击按钮 → 发送 `PacketToggleSynergy` → 服务端处理
- 服务端验证后更新玩家 NBT
- 客户端预测显示（即时反馈）

---

## 🚀 测试步骤

1. **编译并运行游戏**
2. **创造模式获取方块**
   ```
   /give @p moremod:synergy_linker
   ```
3. **放置并右键**
4. **测试激活/停用功能**
5. **重新登录验证数据持久化**

---

## ⚠️ 注意事项

### 如果 GUI 不显示

1. 检查 GuiHandler 是否正确注册 GUI ID
2. 检查 BlockSynergyLinker.onBlockActivated() 中的 mod 实例
3. 检查 TileEntity 是否正确注册

### 如果按钮点击无效

1. 检查网络包是否正确注册
2. 检查 PacketHandler.INSTANCE 是否正确
3. 检查服务端是否收到包（添加日志）

### 如果数据不持久化

1. 检查 PlayerSynergyData.saveToPlayer() 是否被调用
2. 检查玩家 NBT 中是否有 `MoreModSynergies` 键
3. 重启服务器测试

---

## 🎯 与命令系统对比

| 功能 | 命令系统 | GUI 系统 |
|------|---------|---------|
| 易用性 | 需要记住命令 | 可视化界面 |
| 新手友好 | 较难 | 非常友好 |
| 集成难度 | 一行代码 | 需要注册多个组件 |
| 依赖关系 | 零依赖 | 依赖 GuiHandler |
| 游戏体验 | 实用 | 沉浸感强 |

**建议**：同时保留两种方式，玩家可以选择使用。

---

## 📚 相关文档

- `SYNERGY_ACTIVATION_GUIDE.md` - 激活系统使用指南
- `SYNERGY_SYSTEM_README.md` - Synergy 系统完整文档

---

## 🔗 API 调用示例

如果你想在其他地方使用 Synergy 数据：

```java
// 获取玩家数据
PlayerSynergyData data = PlayerSynergyData.get(player);

// 检查激活状态
boolean isActive = data.isSynergyActivated("GLASS_CANNON");

// 程序化激活
data.activateSynergy("BERSERKER");
data.saveToPlayer(player);

// 获取所有已激活的 Synergy
Set<String> activated = data.getActivatedSynergies();
```

---

完成集成后，玩家就可以通过方块 GUI 或命令两种方式管理 Synergy 了！
