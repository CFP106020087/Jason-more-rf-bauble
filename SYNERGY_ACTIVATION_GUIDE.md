# Synergy 激活系统使用指南

## 概述

从版本 2.0 开始，Synergy 系统需要**手动激活**才能生效。玩家仅拥有模块组合**不会**自动触发 Synergy 效果。

这个设计确保了：
- ✅ **可控性**：玩家可以选择激活哪些 Synergy
- ✅ **平衡性**：避免过多 Synergy 同时生效
- ✅ **策略性**：玩家需要权衡 Synergy 的正面效果和负面代价（Drawback）

---

## 激活方式

### 方式 1：命令系统（当前可用）

使用 `/synergy` 命令激活和管理 Synergy：

#### 集成步骤

在你的主 mod 类中添加：

```java
@Mod.EventHandler
public void serverStarting(FMLServerStartingEvent event) {
    // 注册 Synergy 命令
    SynergyBootstrap.registerCommands(event);
}
```

#### 可用命令

**1. 列出所有 Synergy**
```
/synergy list
```
显示所有可用的 Synergy 及其激活状态。

**2. 激活 Synergy**
```
/synergy activate <ID>
```
示例：`/synergy activate GLASS_CANNON`

**3. 停用 Synergy**
```
/synergy deactivate <ID>
```
示例：`/synergy deactivate GLASS_CANNON`

**4. 查看已激活的 Synergy**
```
/synergy active
```

**5. 清空所有激活状态**
```
/synergy clear
```

#### 权限要求

- 需要 OP 权限（等级 2）或创造模式
- 仅玩家可用（控制台无法使用）

---

### 方式 2：GUI 系统（计划中）

**注意**：GUI 激活系统是可选的高级功能，需要手动集成。

#### 为什么 GUI 是可选的？

为了保持 Synergy 系统的"高度解耦"特性，GUI 系统需要修改现有的 `GuiHandler.java`，这会创建硬依赖。因此 GUI 系统作为可选功能提供。

#### 如何实现 GUI 激活（未来）

如果你想要实现拖拽式 GUI 激活系统，可以参考以下步骤：

1. **创建 Synergy Linker 方块**
   - 放置在世界中的物理方块
   - 玩家右键打开 GUI

2. **创建 GUI 界面**
   - 显示玩家拥有的模块
   - 显示可用的 Synergy
   - 拖拽模块到链接槽位激活 Synergy

3. **注册到 GuiHandler**
   - 在 `GuiHandler.java` 中添加新的 GUI ID
   - 实现 `getServerGuiElement` 和 `getClientGuiElement`

4. **持久化**
   - 使用 `PlayerSynergyData` 保存激活状态
   - 数据自动同步到客户端

详细实现指南将在未来版本提供。

---

## 数据持久化

### 存储位置

玩家的 Synergy 激活状态存储在：
- **玩家 NBT**: `EntityPlayer` 的 `PERSISTED_NBT_TAG` 中
- **键名**: `MoreModSynergies`

### 数据格式

```json
{
  "MoreModSynergies": {
    "Activated": [
      "ENERGY_LOOP",
      "GLASS_CANNON",
      "BERSERKER"
    ]
  }
}
```

### 自动同步

- 激活/停用后自动保存到玩家 NBT
- 玩家重新登录后自动加载
- 跨服务器/单人世界保持一致

---

## 内置 Synergy 列表

### 原始 3 个 Synergy（已添加 Drawback）

| ID | 名称 | 所需模块 | 正面效果 | 负面效果 |
|----|------|----------|----------|----------|
| `ENERGY_LOOP` | 能量循环 | ENERGY_EFFICIENCY + KINETIC_GENERATOR | 20% 几率返还 50 RF | -10 RF/s |
| `COMBAT_ECHO` | 战斗回响 | DAMAGE_BOOST + ATTACK_SPEED | 伤害 +25% | -15 RF/s |
| `SURVIVAL_SHIELD` | 生存护盾 | YELLOW_SHIELD + HEALTH_REGEN | 低血量时授予护盾 | 饥饿 I |

### 新增 6 个创意 Synergy

| ID | 名称 | 所需模块 | 正面效果 | 负面效果 |
|----|------|----------|----------|----------|
| `GLASS_CANNON` | 玻璃大炮 | CRITICAL_STRIKE + DAMAGE_BOOST | 伤害 +50% | 受伤 +30% |
| `BERSERKER` | 狂战士 | DAMAGE_BOOST + HEALTH_REGEN | 低血时伤害 +60% | -0.5 HP/s |
| `ARCANE_OVERLOAD` | 奥术过载 | MAGIC_ABSORB + ENERGY_EFFICIENCY | 50% 返还 100 RF | 虚弱 II + 挖掘疲劳 I |
| `SPEED_DEMON` | 速度恶魔 | ATTACK_SPEED + KINETIC_GENERATOR | 速度 II + 急迫 I | -30 RF/s + 饥饿 II |
| `VAMPIRE` | 吸血鬼 | DAMAGE_BOOST + HEALTH_REGEN | 吸血 25% | 虚弱 I + 饥饿 I |
| `FORTRESS` | 堡垒 | YELLOW_SHIELD + ARMOR_BOOST | 护盾 + 减伤 40% | 缓慢 II |

---

## 开发者 API

### 检查 Synergy 激活状态

```java
import com.moremod.synergy.data.PlayerSynergyData;

// 获取玩家数据
PlayerSynergyData data = PlayerSynergyData.get(player);

// 检查是否激活
boolean isActivated = data.isSynergyActivated("GLASS_CANNON");

// 获取所有已激活的 Synergy
Set<String> activatedIds = data.getActivatedSynergies();
```

### 程序化激活 Synergy

```java
PlayerSynergyData data = PlayerSynergyData.get(player);

// 激活
if (data.activateSynergy("BERSERKER")) {
    data.saveToPlayer(player);
    player.sendMessage(new TextComponentString("Berserker 已激活！"));
}

// 停用
if (data.deactivateSynergy("BERSERKER")) {
    data.saveToPlayer(player);
    player.sendMessage(new TextComponentString("Berserker 已停用！"));
}
```

### 清空所有激活状态

```java
PlayerSynergyData data = PlayerSynergyData.get(player);
data.clearAll();
data.saveToPlayer(player);
```

---

## 故障排查

### Q: 为什么我拥有模块但 Synergy 不生效？

A: 从 2.0 版本开始，Synergy 需要手动激活。使用 `/synergy activate <ID>` 激活。

### Q: 激活后数据会丢失吗？

A: 不会。激活状态保存在玩家的持久化 NBT 中，即使退出游戏也会保留。

### Q: 如何批量激活 Synergy？

A: 目前需要逐个激活。未来可能添加批量操作命令或 GUI。

### Q: 可以同时激活多个 Synergy 吗？

A: 可以！没有数量限制。但要注意 Drawback 的叠加效果可能会很严重。

### Q: Drawback 会致死吗？

A: 不会。`HealthDrainEffect` 最多扣到 1.0 HP，不会直接杀死玩家。

---

## 移除 Synergy 系统

如果你想完全移除 Synergy 系统：

1. **删除调用**：
   ```java
   // 在主 mod 类中删除或注释掉：
   // SynergyBootstrap.initialize();
   // SynergyBootstrap.registerCommands(event);
   ```

2. **删除包**：
   ```bash
   rm -rf src/main/java/com/moremod/synergy
   ```

3. **删除文档**：
   ```bash
   rm SYNERGY_SYSTEM_README.md
   rm SYNERGY_ACTIVATION_GUIDE.md
   ```

4. **玩家数据清理**（可选）：
   玩家 NBT 中的 `MoreModSynergies` 键会保留，但不会影响游戏。如果想清理：
   ```java
   NBTTagCompound playerData = player.getEntityData();
   NBTTagCompound persistedData = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
   persistedData.removeTag("MoreModSynergies");
   ```

---

## 版本历史

### v2.0 - 激活系统 + Drawback
- 添加玩家激活状态管理
- 添加 `/synergy` 命令系统
- 为所有 Synergy 添加负面效果（Drawback）
- 新增 6 个创意 Synergy
- 更新 SynergyManager 支持激活检查

### v1.0 - 初始版本
- 基础 Synergy 系统
- 3 个示例 Synergy
- 完全解耦架构

---

## 反馈与贡献

如有问题或建议，欢迎提交 Issue 或 Pull Request！
