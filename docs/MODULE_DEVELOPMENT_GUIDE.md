# 模块开发完整流程手册

本文档详细记录从模块注册到效果生效的完整流程。

---

## 目录

1. [系统架构概览](#系统架构概览)
2. [新系统完整开发流程](#新系统完整开发流程)
   - [步骤1: 添加 UpgradeType 枚举](#步骤1-添加-upgradetype-枚举)
   - [步骤2: 创建 Handler 实现类](#步骤2-创建-handler-实现类)
   - [步骤3: 注册模块定义](#步骤3-注册模块定义)
   - [步骤4: 效果如何生效](#步骤4-效果如何生效)
3. [旧系统开发流程](#旧系统开发流程)
4. [核心类详解](#核心类详解)
5. [数据流图](#数据流图)
6. [常见问题排查](#常见问题排查)

---

## 系统架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            模块系统架构                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌──────────────────┐    ┌─────────────────────────┐    │
│  │ UpgradeType │───>│ ModuleDefinition │───>│ ModuleAutoRegistry      │    │
│  │   (枚举)     │    │    (模块定义)     │    │   (自动注册器)           │    │
│  └─────────────┘    └──────────────────┘    └───────────┬─────────────┘    │
│                              │                          │                   │
│                              │ .handler()               │ 注册物品          │
│                              ▼                          ▼                   │
│                     ┌──────────────────┐    ┌─────────────────────────┐    │
│                     │IModuleEventHandler│    │ ItemUpgradeComponent    │    │
│                     │  (事件处理器接口)  │    │   (升级组件物品)         │    │
│                     └────────┬─────────┘    └───────────┬─────────────┘    │
│                              │                          │                   │
│                              │ 实现                      │ 右键使用          │
│                              ▼                          ▼                   │
│                     ┌──────────────────┐    ┌─────────────────────────┐    │
│                     │AreaMiningHandler │    │ ItemMechanicalCore      │    │
│                     │  (具体实现类)     │    │   (机械核心)             │    │
│                     └────────┬─────────┘    └───────────┬─────────────┘    │
│                              │                          │                   │
│                              │                          │ isUpgradeActive   │
│                              ▼                          ▼                   │
│                     ┌────────────────────────────────────────────────┐     │
│                     │              AutoEffectHandler                  │     │
│                     │           (自动效果分发器)                       │     │
│                     │                                                 │     │
│                     │  • PlayerTickEvent → onTick / onSecondTick     │     │
│                     │  • LivingHurtEvent → onPlayerHurt / onAttack   │     │
│                     │  • BlockBreakEvent → onBlockBreak              │     │
│                     │  • ...其他 Forge 事件                           │     │
│                     └────────────────────────────────────────────────┘     │
│                                          │                                  │
│                                          ▼                                  │
│                              ┌────────────────────┐                        │
│                              │   EventContext     │                        │
│                              │  (事件上下文)       │                        │
│                              │                    │                        │
│                              │ • player           │                        │
│                              │ • coreStack        │                        │
│                              │ • level            │                        │
│                              │ • consumeEnergy()  │                        │
│                              │ • setNBT/getNBT()  │                        │
│                              │ • setCooldown()    │                        │
│                              └────────────────────┘                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 新系统完整开发流程

以 **范围挖掘 (AREA_MINING_BOOST)** 模块为例。

### 步骤1: 添加 UpgradeType 枚举

**文件**: `src/main/java/com/moremod/item/UpgradeType.java`

```java
public enum UpgradeType {
    // ... 其他枚举值 ...

    // 在对应类别下添加
    AREA_MINING_BOOST("范围挖掘", TextFormatting.YELLOW, UpgradeCategory.AUXILIARY),

    // ... 其他枚举值 ...
}
```

**说明**:
- 枚举名称必须是 **大写下划线格式** (如 `AREA_MINING_BOOST`)
- 第一个参数是显示名称
- 第二个参数是颜色
- 第三个参数是类别 (`SURVIVAL`/`AUXILIARY`/`COMBAT`/`ENERGY`)

---

### 步骤2: 创建 Handler 实现类

**文件**: `src/main/java/com/moremod/module/handler/AreaMiningBoostHandler.java`

```java
package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

import java.util.*;

/**
 * 范围挖掘模块处理器 (Vein Mining)
 *
 * 实现 IModuleEventHandler 接口，只需覆盖需要的方法
 */
public class AreaMiningBoostHandler implements IModuleEventHandler {

    // 每级最大连锁数量
    private static final int[] MAX_BLOCKS_PER_LEVEL = {0, 8, 16, 32};

    // 每个额外方块的能耗
    private static final int ENERGY_PER_BLOCK = 50;

    // 防止递归挖掘的标记
    private static final Set<UUID> currentlyMining = new HashSet<>();

    /**
     * 方块破坏事件 - 核心逻辑入口
     *
     * 当玩家破坏方块时，AutoEffectHandler 会自动调用此方法
     * 前提是：模块已激活且有足够能量
     */
    @Override
    public void onBlockBreak(EventContext ctx, BlockEvent.BreakEvent event) {
        // 1. 基础检查
        if (event.isCanceled()) return;
        if (event.getWorld().isRemote) return;

        // 2. 防止递归
        if (currentlyMining.contains(ctx.player.getUniqueID())) return;

        // 3. 执行连锁挖掘
        performVeinMine(ctx, event.getWorld(), event.getPos(), event.getState());
    }

    private void performVeinMine(EventContext ctx, World world, BlockPos startPos, IBlockState targetState) {
        Block targetBlock = targetState.getBlock();

        // 根据等级确定最大连锁数
        int maxBlocks = ctx.level < MAX_BLOCKS_PER_LEVEL.length
            ? MAX_BLOCKS_PER_LEVEL[ctx.level] : 32;

        // BFS 查找相邻同类型方块
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        List<BlockPos> toBreak = new ArrayList<>();

        toCheck.add(startPos);
        checked.add(startPos);

        while (!toCheck.isEmpty() && toBreak.size() < maxBlocks) {
            BlockPos current = toCheck.poll();

            // 检查 26 个方向（包括对角线）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (checked.contains(neighbor)) continue;
                        checked.add(neighbor);

                        IBlockState neighborState = world.getBlockState(neighbor);
                        if (neighborState.getBlock() == targetBlock) {
                            toCheck.add(neighbor);
                            toBreak.add(neighbor);
                            if (toBreak.size() >= maxBlocks) break;
                        }
                    }
                    if (toBreak.size() >= maxBlocks) break;
                }
                if (toBreak.size() >= maxBlocks) break;
            }
        }

        if (toBreak.isEmpty()) return;

        // 根据能量限制实际挖掘数量
        int availableEnergy = ctx.getEnergy();
        int blocksToMine = Math.min(toBreak.size(), availableEnergy / ENERGY_PER_BLOCK);
        if (blocksToMine <= 0) return;

        // 标记正在挖掘，防止递归
        currentlyMining.add(ctx.player.getUniqueID());

        try {
            // 消耗能量
            ctx.consumeEnergy(blocksToMine * ENERGY_PER_BLOCK);

            // 挖掘方块
            for (int i = 0; i < blocksToMine; i++) {
                BlockPos pos = toBreak.get(i);
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock().isAir(state, world, pos)) continue;

                // 使用 destroyBlock 正确处理掉落
                world.destroyBlock(pos, true);
            }
        } finally {
            currentlyMining.remove(ctx.player.getUniqueID());
        }
    }

    /**
     * 被动能耗 (RF/tick)
     * 返回 0 表示无被动消耗，此模块使用主动消耗
     */
    @Override
    public int getPassiveEnergyCost() {
        return 0;
    }

    /**
     * 处理器描述（用于调试）
     */
    @Override
    public String getDescription() {
        return "范围挖掘 - 连锁挖掘相邻同类型方块";
    }
}
```

---

### 步骤3: 注册模块定义

**文件**: `src/main/java/com/moremod/module/ModuleAutoRegistry.java`

在 `registerAllModules()` 方法中添加：

```java
private static void registerAllModules() {
    // ... 其他模块 ...

    // 范围挖掘模块
    ModuleDefinition.builder("AREA_MINING_BOOST")
        .displayName("范围挖掘")
        .color(TextFormatting.YELLOW)
        .category(ModuleDefinition.Category.AUXILIARY)
        .maxLevel(3)
        .levelDescriptions(lv -> {
            switch (lv) {
                case 1: return new String[]{
                    TextFormatting.YELLOW + "范围挖掘 I",
                    TextFormatting.GRAY + "将范围挖掘升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 连锁数量: 8个方块",
                    TextFormatting.AQUA + "▶ 能耗: 50 RF/方块"
                };
                case 2: return new String[]{
                    TextFormatting.YELLOW + "范围挖掘 II",
                    TextFormatting.GRAY + "将范围挖掘升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 连锁数量: 16个方块",
                    TextFormatting.AQUA + "▶ 能耗: 50 RF/方块"
                };
                case 3: return new String[]{
                    TextFormatting.YELLOW + "✦ 范围挖掘 III ✦",
                    TextFormatting.GRAY + "将范围挖掘升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 连锁数量: 32个方块",
                    TextFormatting.AQUA + "▶ 能耗: 50 RF/方块",
                    TextFormatting.RED + "已达最高等级"
                };
                default: return new String[]{};
            }
        })
        .handler(new AreaMiningBoostHandler())  // 关键：绑定 Handler
        .register();
}
```

---

### 步骤4: 效果如何生效

#### 4.1 游戏启动时的注册流程

```
1. Forge 预初始化 (PreInit)
   │
   ▼
2. ModuleAutoRegistry.init() 被调用
   │
   ├─> registerAllModules()
   │   └─> ModuleDefinition.builder(...).register()
   │       └─> ModuleAutoRegistry.register(def)
   │           └─> DEFINITIONS.put(def.id, def)
   │
   ├─> registerToMechanicalCoreExtended(def)
   │   └─> ItemMechanicalCoreExtended.register(...)
   │       └─> 注册模块到扩展系统
   │
   └─> createItemInstances(def)
       └─> 为每个等级创建 ItemUpgradeComponent
           └─> 设置 registryName, translationKey, creativeTab
```

#### 4.2 玩家安装模块的流程

```
1. 玩家手持升级组件右键
   │
   ▼
2. ItemUpgradeComponent.onItemRightClick()
   │
   ├─> 查找装备的机械核心
   │   └─> ItemMechanicalCore.findEquippedMechanicalCore(player)
   │
   ├─> 检查是否可升级
   │   └─> ItemMechanicalCoreExtended.canUpgrade(coreStack, moduleId)
   │
   └─> 应用升级
       └─> ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, moduleId, 1)
           └─> 在核心的 NBT 中写入: upgrade_AREA_MINING_BOOST = level
```

#### 4.3 效果触发流程

```
玩家行为 (破坏方块)
   │
   ▼
Forge 事件: BlockEvent.BreakEvent
   │
   ▼
AutoEffectHandler.onBlockBreak()
   │
   ├─> 获取玩家的机械核心
   │   └─> getCachedCoreStack(player)
   │
   ├─> 获取活跃模块缓存
   │   └─> getCachedActiveModules(player)
   │
   ├─> 遍历所有有 Handler 的模块
   │   └─> ModuleAutoRegistry.getHandlerModules()
   │
   ├─> 检查模块是否激活
   │   └─> cachedModules.get("AREA_MINING_BOOST") != null
   │
   ├─> 创建事件上下文
   │   └─> new EventContext(player, coreStack, moduleId, level)
   │
   └─> 调用 Handler 方法
       └─> def.handler.onBlockBreak(ctx, event)
           └─> AreaMiningBoostHandler.onBlockBreak()
               └─> performVeinMine()
                   ├─> ctx.getEnergy()       // 检查能量
                   ├─> ctx.consumeEnergy()   // 消耗能量
                   └─> world.destroyBlock()  // 破坏方块
```

---

## 旧系统开发流程

旧系统使用手动注册方式，需要在多个位置添加代码。

### 完整注册链路 (6 步)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          旧系统模块注册流程                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  步骤1: ItemMechanicalCoreExtended.registerUpgrades()                        │
│         └─ 注册模块元数据 (ID, 显示名, 颜色, 最大等级, 类别)                    │
│                                                                              │
│  步骤2: UpgradeType 枚举                                                     │
│         └─ 添加枚举值 (用于物品创建和类型识别)                                  │
│                                                                              │
│  步骤3: ItemMechanicalCore.BASE_EXTENDED_UPGRADE_IDS                         │
│         └─ 添加模块ID字符串 (用于tooltip计数和模块识别)                         │
│                                                                              │
│  步骤4: UpgradeItemsExtended (两个位置!)                                     │
│         ├─ 定义物品实例 (static final 字段)                                   │
│         └─ 添加到 getAllExtendedUpgrades() 返回数组                           │
│                                                                              │
│  步骤5: 语言文件                                                              │
│         └─ 添加 item.registryName.name=显示名称                               │
│                                                                              │
│  步骤6: 实现独立事件处理器                                                     │
│         └─ @Mod.EventBusSubscriber 类                                        │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

### 步骤1: ItemMechanicalCoreExtended.registerUpgrades()

**文件**: `src/main/java/com/moremod/item/ItemMechanicalCoreExtended.java`

在 `registerUpgrades()` 方法中添加：

```java
private static void registerUpgrades() {
    // ... 其他模块 ...

    // ===== 战斗类 =====
    register("RANGED_DAMAGE_BOOST", "远程伤害增幅", TextFormatting.GOLD, 3, UpgradeCategory.COMBAT);

    // ... 其他模块 ...
}
```

**参数说明**:
- 参数1: 模块ID (大写下划线格式)
- 参数2: 显示名称
- 参数3: 颜色
- 参数4: 最大等级
- 参数5: 类别 (`SURVIVAL`/`AUXILIARY`/`COMBAT`/`ENERGY`)

---

### 步骤2: UpgradeType 枚举

**文件**: `src/main/java/com/moremod/item/UpgradeType.java`

```java
public enum UpgradeType {
    // ... 其他枚举值 ...

    // ===== 战斗類升級 =====
    RANGED_DAMAGE_BOOST("远程伤害增幅", TextFormatting.GOLD, UpgradeCategory.COMBAT),

    // ... 其他枚举值 ...
}
```

**注意**: 枚举名称必须与步骤1中的模块ID完全一致！

---

### 步骤3: ItemMechanicalCore.BASE_EXTENDED_UPGRADE_IDS

**文件**: `src/main/java/com/moremod/item/ItemMechanicalCore.java`

在 `BASE_EXTENDED_UPGRADE_IDS` 数组中添加：

```java
private static final String[] BASE_EXTENDED_UPGRADE_IDS = {
    "YELLOW_SHIELD","HEALTH_REGEN","HUNGER_THIRST","THORNS","FIRE_EXTINGUISH",
    // ... 其他模块 ...
    "RANGED_DAMAGE_BOOST"  // <-- 添加到这里
};
```

**作用**: 用于核心tooltip的模块计数和模块系统识别。

---

### 步骤4: UpgradeItemsExtended (两个位置!)

**文件**: `src/main/java/com/moremod/item/upgrades/UpgradeItemsExtended.java`

#### 位置 A: 定义物品实例 (static final 字段)

```java
public class UpgradeItemsExtended {

    // ... 其他模块 ...

    // ===== 远程伤害增幅（3级） =====
    public static final ItemUpgradeComponent RANGED_DAMAGE_BOOST_LV1 = createUpgrade(
            UpgradeType.RANGED_DAMAGE_BOOST, "ranged_damage_boost_lv1",
            new String[]{
                    TextFormatting.GOLD + "远程伤害增幅 I",
                    TextFormatting.GRAY + "将远程伤害增幅升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 远程伤害: +15%",
                    TextFormatting.DARK_GRAY + "基础增幅"
            }, 1, 16
    );

    public static final ItemUpgradeComponent RANGED_DAMAGE_BOOST_LV2 = createUpgrade(
            UpgradeType.RANGED_DAMAGE_BOOST, "ranged_damage_boost_lv2",
            new String[]{
                    TextFormatting.GOLD + "远程伤害增幅 II",
                    TextFormatting.GRAY + "将远程伤害增幅升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 远程伤害: +30%",
                    TextFormatting.BLUE + "强化增幅"
            }, 2, 8
    );

    public static final ItemUpgradeComponent RANGED_DAMAGE_BOOST_LV3 = createUpgrade(
            UpgradeType.RANGED_DAMAGE_BOOST, "ranged_damage_boost_lv3",
            new String[]{
                    TextFormatting.GOLD + "✦ 远程伤害增幅 III ✦",
                    TextFormatting.GRAY + "将远程伤害增幅升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 远程伤害: +50%",
                    TextFormatting.LIGHT_PURPLE + "极限增幅",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // ... 其他模块 ...
}
```

#### 位置 B: getAllExtendedUpgrades() 返回数组

在同一文件的 `getAllExtendedUpgrades()` 方法中添加：

```java
public static ItemUpgradeComponent[] getAllExtendedUpgrades() {
    return new ItemUpgradeComponent[]{
            // 生存类
            YELLOW_SHIELD_LV1, YELLOW_SHIELD_LV2, YELLOW_SHIELD_LV3,
            // ... 其他模块 ...

            // 战斗类
            DAMAGE_BOOST_LV1, DAMAGE_BOOST_LV2, DAMAGE_BOOST_LV3, DAMAGE_BOOST_LV4, DAMAGE_BOOST_LV5,
            // ... 其他模块 ...
            RANGED_DAMAGE_BOOST_LV1, RANGED_DAMAGE_BOOST_LV2, RANGED_DAMAGE_BOOST_LV3,  // <-- 添加到这里

            // ... 其他模块 ...
    };
}
```

**重要**: 必须添加到两个位置！物品定义 + 返回数组。

---

### 步骤5: 语言文件

**文件**: `src/main/resources/assets/moremod/lang/zh_cn.lang`

```properties
item.ranged_damage_boost_lv1.name=远程伤害增幅 Lv.1 升级组件
item.ranged_damage_boost_lv2.name=远程伤害增幅 Lv.2 升级组件
item.ranged_damage_boost_lv3.name=远程伤害增幅 Lv.3 升级组件
```

---

### 步骤6: 实现独立事件处理器

**文件**: `src/main/java/com/moremod/event/RangedDamageHandler.java`

创建新的事件处理器类：

```java
package com.moremod.event;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class RangedDamageHandler {

    private static final String MODULE_ID = "RANGED_DAMAGE_BOOST";
    private static final float[] DAMAGE_MULTIPLIERS = {0f, 0.15f, 0.30f, 0.50f};

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 1. 检查是否为远程伤害
        if (!isRangedDamage(source)) return;

        // 2. 获取攻击者
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) source.getTrueSource();

        // 3. 只在服务端处理
        if (player.world.isRemote) return;

        // 4. 检查机械核心
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (coreStack.isEmpty()) return;

        // 5. 检查模块是否激活 (关键!)
        if (!ItemMechanicalCore.isUpgradeActive(coreStack, MODULE_ID)) return;

        // 6. 获取模块等级
        int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, MODULE_ID);
        if (level <= 0) return;

        // 7. 应用效果
        float multiplier = getMultiplier(level);
        float newDamage = event.getAmount() * (1.0f + multiplier);
        event.setAmount(newDamage);
    }

    private static boolean isRangedDamage(DamageSource source) {
        if (source.isProjectile()) return true;
        String type = source.getDamageType();
        return type != null && (type.equals("arrow") || type.equals("thrown"));
    }

    private static float getMultiplier(int level) {
        if (level < 0 || level >= DAMAGE_MULTIPLIERS.length) {
            return DAMAGE_MULTIPLIERS[DAMAGE_MULTIPLIERS.length - 1];
        }
        return DAMAGE_MULTIPLIERS[level];
    }
}
```

### 旧系统 vs 新系统对比

| 步骤 | 旧系统 (独立处理器) | 新系统 (AutoEffectHandler) |
|------|---------------------|---------------------------|
| 检查核心 | 手动: `findEquippedMechanicalCore()` | 自动: `getCachedCoreStack()` |
| 检查激活 | 手动: `isUpgradeActive()` | 自动: 通过缓存 |
| 获取等级 | 手动: `getUpgradeLevel()` | 自动: `ctx.level` |
| 能量管理 | 手动: `consumeEnergy()` | 半自动: `ctx.consumeEnergy()` |
| NBT 存储 | 手动 | 内置: `ctx.setNBT/getNBT()` |
| 冷却管理 | 手动 | 内置: `ctx.setCooldown()` |

---

## 核心类详解

### ModuleDefinition

模块的完整定义，包含：

```java
public final class ModuleDefinition {
    public final String id;                    // 模块ID (如 "AREA_MINING_BOOST")
    public final String displayName;           // 显示名称 (如 "范围挖掘")
    public final TextFormatting color;         // 颜色
    public final Category category;            // 类别 (SURVIVAL/AUXILIARY/COMBAT/ENERGY)
    public final int maxLevel;                 // 最大等级
    public final List<ModuleEffect> effects;   // 简单效果列表
    public final IModuleEventHandler handler;  // 事件处理器
    // ...
}
```

### IModuleEventHandler

事件处理器接口，所有方法都有默认空实现：

| 方法 | 触发时机 | 返回值说明 |
|------|----------|-----------|
| `onTick(ctx)` | 每 tick | 无 |
| `onSecondTick(ctx)` | 每秒 | 无 |
| `onPlayerAttack(ctx, target, damage)` | 玩家攻击 | 返回修改后的伤害 |
| `onPlayerHurt(ctx, source, damage)` | 玩家受伤 | 返回修改后的伤害 |
| `onPlayerAttacked(ctx, source, damage)` | 玩家被攻击前 | 返回 true 取消攻击 |
| `onBlockBreak(ctx, event)` | 破坏方块 | 无 |
| `onPlayerKillEntity(ctx, target, event)` | 击杀实体 | 无 |
| `onModuleActivated(ctx)` | 模块激活 | 无 |
| `onModuleDeactivated(ctx)` | 模块关闭 | 无 |
| `getPassiveEnergyCost()` | 每 tick 调用 | 返回被动能耗 RF/tick |

### EventContext

事件上下文，提供所有必要信息和辅助方法：

```java
public class EventContext {
    // 基础属性
    public final EntityPlayer player;      // 玩家
    public final ItemStack coreStack;      // 机械核心
    public final String moduleId;          // 模块ID
    public final int level;                // 当前等级
    public final long worldTime;           // 世界时间

    // 能量方法
    boolean consumeEnergy(int amount);     // 消耗能量
    int getEnergy();                       // 当前能量
    int getMaxEnergy();                    // 最大能量
    boolean hasEnergy(int amount);         // 是否有足够能量

    // NBT 存储 (自动添加模块前缀)
    void setNBT(String key, int/float/boolean/String value);
    int getNBTInt(String key);
    float getNBTFloat(String key);
    boolean getNBTBoolean(String key);

    // 冷却系统
    void setCooldown(String key, int ticks);
    int getCooldown(String key);
    boolean isOnCooldown(String key);
    boolean tryUseAbility(String key, int cooldownTicks, int energyCost);

    // 工具方法
    void sendActionBar(String message);
    void sendMessage(String message);
    void playSound(SoundEvent sound, float volume, float pitch);
    void spawnParticle(...);
}
```

### AutoEffectHandler

自动效果分发器，订阅 Forge 事件并分发给模块 Handler：

```java
@Mod.EventBusSubscriber(modid = "moremod")
public class AutoEffectHandler {

    // 性能优化缓存
    private static final Map<UUID, Map<String, Integer>> activeModulesCache;
    private static final Map<UUID, ItemStack> coreStackCache;

    // 事件处理
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) { ... }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) { ... }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) { ... }

    // ... 其他事件
}
```

---

## 数据流图

### 模块激活状态检查

```
ItemMechanicalCore.isUpgradeActive(stack, "AREA_MINING_BOOST")
    │
    ├─> 检查核心是否有效
    │
    ├─> 检查是否被 GUI 临时阻止
    │
    ├─> 检查是否被禁用 (Disabled_AREA_MINING_BOOST)
    │
    ├─> 获取升级等级
    │   └─> getUpgradeLevelDirect(stack, moduleId)
    │       └─> NBT: stack.getTagCompound().getInteger("upgrade_AREA_MINING_BOOST")
    │
    └─> 检查能量是否足够 (对于非发电模块)
        └─> EnergyDepletionManager.isUpgradeActive()
```

### NBT 数据结构

```
ItemStack (机械核心)
└─ TagCompound
   ├─ Energy: 1000000           // 当前能量
   ├─ MaxEnergy: 5000000        // 最大能量
   ├─ upgrade_AREA_MINING_BOOST: 3    // 模块等级
   ├─ upgrade_RANGED_DAMAGE_BOOST: 2  // 另一个模块
   ├─ Disabled_AREA_MINING_BOOST: false  // 是否禁用
   ├─ Module_AREA_MINING_BOOST_xxx: ... // 模块专用数据 (通过 ctx.setNBT)
   └─ ...
```

---

## 常见问题排查

### Q1: 模块安装了但效果不生效

**检查清单**:

1. **UpgradeType 枚举是否添加?**
   - 文件: `src/main/java/com/moremod/item/UpgradeType.java`
   - 确保名称与模块 ID 完全一致

2. **模块是否在 registerAllModules() 中注册?**
   - 文件: `src/main/java/com/moremod/module/ModuleAutoRegistry.java`

3. **Handler 是否正确绑定?**
   ```java
   ModuleDefinition.builder("XXX")
       .handler(new YourHandler())  // 必须有这行
       .register();
   ```

4. **事件方法是否正确覆盖?**
   - 确保方法签名与接口定义完全一致
   - 确保没有调用 `super.xxx()` 导致短路

5. **是否检查了服务端/客户端?**
   ```java
   if (event.getWorld().isRemote) return;  // 客户端跳过
   ```

### Q2: 能量消耗不正确

**检查 EventContext 方法使用**:

```java
// 正确用法
if (ctx.consumeEnergy(100)) {
    // 成功消耗，执行效果
}

// 错误用法 - 没有检查返回值
ctx.consumeEnergy(100);  // 可能失败但继续执行
```

### Q3: 模块物品没有出现在创造模式标签页

**检查 ModuleAutoRegistry.createItemInstances()**:

- 确保 `setCreativeTab()` 被调用
- 确保物品被添加到 `getAllItems()` 返回值中

### Q4: 调试技巧

```java
// 在 Handler 中添加日志
@Override
public void onBlockBreak(EventContext ctx, BlockEvent.BreakEvent event) {
    System.out.println("[DEBUG] AREA_MINING_BOOST triggered");
    System.out.println("[DEBUG] Level: " + ctx.level);
    System.out.println("[DEBUG] Energy: " + ctx.getEnergy());
    // ...
}
```

---

## 文件结构总览

```
src/main/java/com/moremod/
├── item/
│   ├── UpgradeType.java              # 升级类型枚举 (步骤1)
│   ├── ItemMechanicalCore.java       # 机械核心主类
│   ├── ItemMechanicalCoreExtended.java # 扩展模块系统
│   └── upgrades/
│       └── ItemUpgradeComponent.java # 升级组件物品
│
├── module/
│   ├── ModuleDefinition.java         # 模块定义类
│   ├── ModuleAutoRegistry.java       # 自动注册器 (步骤3)
│   ├── effect/
│   │   ├── IModuleEventHandler.java  # 事件处理器接口
│   │   ├── EventContext.java         # 事件上下文
│   │   ├── ModuleEffect.java         # 简单效果定义
│   │   └── AutoEffectHandler.java    # 自动效果分发器
│   └── handler/
│       └── AreaMiningBoostHandler.java # Handler 实现 (步骤2)
│
└── event/
    └── RangedDamageHandler.java      # 旧系统独立处理器
```

---

*最后更新: 2025-12-02*
