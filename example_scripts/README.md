# 宝石掉落配置指南

## 问题诊断

如果你遇到"三级火龙掉落60级宝石"的问题，原因是：

1. **内建规则已禁用**：`GemLootRuleManager.java` 的 static 块已被禁用
2. **需要 ZenScript 配置**：现在所有规则必须通过 ZenScript 配置
3. **必须重新构建**：修改 Java 代码后需要重新编译和部署

## 使用方法

### 步骤 1：重新构建模组

```bash
cd /home/user/Jason-more-rf-bauble
./gradlew build
```

### 步骤 2：复制配置文件

将 `gem_loot_balance.zs` 复制到你的 Minecraft 实例：

```bash
# 示例路径（根据你的实际情况调整）
cp example_scripts/gem_loot_balance.zs ~/.minecraft/scripts/

# 或者如果是服务器
cp example_scripts/gem_loot_balance.zs /path/to/server/scripts/
```

### 步骤 3：测试

1. 重启 Minecraft/服务器，或使用 `/ct reload` 命令
2. 查看日志，应该看到：
   ```
   ✅ 宝石掉落规则已配置（平衡版）
      - 三级龙: Lv25-30
      - Lycanites小Boss: Lv60-75
      - 三王: Lv100固定
   ```
3. 击杀三级火龙，验证掉落等级

## 配置说明

### 默认规则

```zenscript
GemLootRules.setDefault(1, 20, 1, 3, 0.02);
//                      │   │   │  │  └─ 掉落率 (2%)
//                      │   │   │  └──── 最大词条数 (3)
//                      │   │   └─────── 最小词条数 (1)
//                      │   └─────────── 最大等级 (20)
//                      └─────────────── 最小等级 (1)
```

### 龙类配置

- **幼年龙（<200血）**：Lv8-15
- **三级龙（200-399血）**：Lv25-30
- **四级龙（400-599血）**：Lv35-55
- **五级龙（600+血）**：Lv50-70

### Lycanites 配置

- **普通生物**：Lv8-20
- **稀有生物**：Lv20-40
- **小Boss（300-499血）**：Lv60-75
- **Boss（500-999血）**：Lv70-90
- **三王（固定）**：Lv100
- **超级Boss（1000+血）**：Lv85-100

## 自定义配置

如果你想修改等级，编辑 `gem_loot_balance.zs` 文件中的规则，或者在另一个 ZenScript 文件中覆盖：

```zenscript
import mods.moremod.GemLootRules;

// 自定义三级龙等级
GemLootRules.clearAll();  // 清空所有规则
// ... 然后添加你自己的规则
```

## 调试

如果规则不生效，启用调试模式：

```zenscript
import mods.moremod.GemLootConfig;

GemLootConfig.setDebug(true);  // 在配置文件中添加这一行
```

然后查看游戏日志，会显示详细的匹配信息。

## 一键配置预设

如果你想使用快速配置：

```zenscript
import mods.moremod.GemLootRules;
import mods.moremod.GemLootConfig;

// 方式1：使用预设规则
GemLootRules.setupAllRules();  // 加载所有预设规则

// 方式2：使用预设全局配置
GemLootConfig.applyRecommendedSettings();  // 推荐配置
// 或
GemLootConfig.applyLenientSettings();      // 宽松配置
// 或
GemLootConfig.applyStrictSettings();       // 严格配置
```

## 完整的 API 参考

查看源代码了解所有可用方法：
- `CTGemLootRules.java`：规则配置 API
- `CTGemLootConfig.java`：全局配置 API
