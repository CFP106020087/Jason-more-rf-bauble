# Jason-more-rf-bauble 1.12.2 → 1.20.1 迁移计划

## 项目概览

| 指标 | 数值 |
|------|------|
| 总文件数 | 870个Java文件 |
| 总代码行 | ~28,828行 |
| 外部依赖 | 16个MOD |
| 兼容代码 | 67个文件 (7.7%) |

---

## Phase 0: 准备工作 (当前阶段)

### 0.1 创建迁移追踪系统
- [x] 分析项目结构
- [x] 扫描所有依赖
- [x] 生成依赖清单
- [ ] 创建迁移计划文档

### 0.2 依赖分类

#### 必须保留 (需要1.20替代方案)
| 依赖 | 文件数 | 1.20替代方案 |
|------|--------|-------------|
| Baubles 1.5.2 | 117个 | Curios API |
| GeckoLib 3.0.31 | 35个 | GeckoLib 4.x |
| CraftTweaker | 22个 | CraftTweaker 1.20 |

#### 立即解耦 (P1 - 无风险)
| 依赖 | 文件数 | 解耦方式 |
|------|--------|----------|
| Champions | 2个 | 删除反射代码 |
| Infernal Mobs | 1个 | 删除NBT检测 |
| SR Parasites | 1个 | 删除Mixin |
| Refined Storage | 1个 | 删除Mixin |
| Enhanced Visuals | 1个 | 删除@Optional类 |
| Potion Core | 1个 | 删除处理类 |
| MmmMmmMmmMmm | 0个 | 移除libs |
| FermiumBooter | 0个 | 移除libs |
| Redstone Flux | 0个 | 移除libs (间接) |

#### 推荐解耦 (P2 - 低风险)
| 依赖 | 文件数 | 解耦方式 |
|------|--------|----------|
| JEI | 11个 | 提取到可选模块 |
| Ice and Fire | 2个 | 删除兼容代码 |
| Lycanites Mobs | 7个 | 删除Mixin |
| FirstAid | 9个 | 强化@Optional |

---

## Phase 1: 解耦依赖 (预计2-4小时)

### 1.1 移除libs中的可选依赖
```
libs/
├── 删除: MmmMmmMmmMmm-1.12-2.0.5.jar
├── 删除: Ice and Fire-2.1.7.jar
├── 删除: champions-1.12.2-1.0.11.10.jar (已注释)
├── 删除: InfernalMobs-1.12.2.jar
├── 删除: SRParasites-1.12.2v1.9.21.jar
├── 删除: lycanitesmobs-1.12.2-2.0.8.10.jar
├── 删除: firstaid-1.6.22.jar
├── 删除: EnhancedVisuals_v1.4.4_mc1.12.2.jar
├── 删除: PotionCore-1.9_for_1.12.2.jar
├── 删除: `FermiumBooter-1.2.0.jar
├── 保留: Baubles-1.12-1.5.2.jar (核心)
├── 保留: geckolib-forge-1.12.2-3.0.31.jar (核心)
├── 保留: CraftTweaker2-1.12-4.1.20.703.jar (核心)
├── 保留: RedstoneFlux-1.12-2.1.1.1-universal.jar (能量API)
└── 保留: morerefinedstorage-2.3.2.jar (待评估)
```

### 1.2 删除兼容代码
```
src/main/java/com/moremod/compat/
├── 删除: champions/ (如果存在)
├── 删除: infernalmobs/ (如果存在)
├── 删除: srparasites/ (如果存在)
├── 删除: enhancedvisuals/ (如果存在)
├── 删除: potioncore/ (如果存在)
├── 删除: iceandfire/ (如果存在)
├── 保留: jei/ (Phase 2处理)
├── 保留: firstaid/ (Phase 2处理)
└── 保留: baubles/ (转Curios)
```

### 1.3 删除Mixin兼容
```
src/main/java/com/moremod/mixin/
├── 删除: 所有针对外部MOD的Mixin
└── 保留: 核心功能Mixin
```

### 1.4 清理import和反射调用
- 搜索并删除所有引用已删除MOD的import
- 删除Loader.isModLoaded()相关的死代码

---

## Phase 2: 创建1.20项目结构 (预计4-6小时)

### 2.1 创建新项目
```
Jason-more-rf-bauble-1.20/
├── build.gradle (NeoForge 1.20.1)
├── gradle.properties
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/com/moremod/
    │   └── resources/
    └── generated/
```

### 2.2 配置build.gradle
- NeoForge 47.1.x
- Curios API (替代Baubles)
- GeckoLib 4.x
- CraftTweaker 1.20

### 2.3 创建基础框架
- Mod主类
- 注册系统 (DeferredRegister)
- 网络系统骨架
- 配置系统

---

## Phase 3: 核心代码迁移 (预计16-24小时)

### 3.1 基础设施 (4小时)
| 模块 | 文件数 | 主要变更 |
|------|--------|----------|
| core/ | ~10个 | Mod主类, 注册 |
| config/ | ~5个 | 配置系统 |
| creativetab/ | 2个 | CreativeModeTabs |

### 3.2 方块系统 (6小时)
| 模块 | 文件数 | 主要变更 |
|------|--------|----------|
| block/ | 34个 | Block注册, BlockEntityTicker |
| tile/ | 29个 | TileEntity→BlockEntity |

### 3.3 物品系统 (4小时)
| 模块 | 文件数 | 主要变更 |
|------|--------|----------|
| item/ | ~50个 | Item注册 |
| upgrades/ | ~20个 | 升级系统 |

### 3.4 网络系统 (6小时)
| 模块 | 文件数 | 主要变更 |
|------|--------|----------|
| network/ | 47个 | IMessage→CustomPacketPayload |

### 3.5 GUI系统 (4小时)
| 模块 | 文件数 | 主要变更 |
|------|--------|----------|
| container/ | ~15个 | AbstractContainerMenu |
| gui/ | ~20个 | AbstractContainerScreen |

---

## Phase 4: 核心依赖迁移 (预计8-12小时)

### 4.1 Baubles → Curios
- 将BaubleType映射到Curios Slot
- 更新所有IBauble实现
- 迁移饰品盒系统

### 4.2 GeckoLib 3.x → 4.x
- 更新动画系统
- 迁移所有GeoModel
- 更新渲染器

### 4.3 CraftTweaker迁移
- 更新ZenScript API
- 迁移配方处理器

---

## Phase 5: 测试与修复 (预计4-8小时)

### 5.1 编译修复
- 修复所有编译错误
- 处理弃用警告

### 5.2 运行时测试
- 启动游戏测试
- 功能验证

### 5.3 回归测试
- 所有方块功能
- 所有物品功能
- 网络同步

---

## 时间估算

| Phase | 预计时间 | 状态 |
|-------|----------|------|
| Phase 0: 准备 | 2小时 | ✅ 进行中 |
| Phase 1: 解耦 | 2-4小时 | ⏳ 待开始 |
| Phase 2: 新项目 | 4-6小时 | ⏳ 待开始 |
| Phase 3: 核心迁移 | 16-24小时 | ⏳ 待开始 |
| Phase 4: 依赖迁移 | 8-12小时 | ⏳ 待开始 |
| Phase 5: 测试 | 4-8小时 | ⏳ 待开始 |
| **总计** | **36-56小时** | - |

---

## 当前进度

```
[████████████░░░░░░░░] 60% - Phase 2 完成
```

## 已完成工作

### Phase 1: 解耦依赖 ✅
1. ✅ 清理libs目录 - 12个可选依赖移至备份
2. ✅ 删除兼容代码 - compat/, integration/jei/ 移至备份
3. ✅ 清理Mixin - 7个外部MOD Mixin配置移除
4. ✅ 清理import引用 - 所有外部MOD直接依赖改为反射

### Phase 2: 1.20项目框架 ✅
1. ✅ 创建项目结构 - /home/user/Jason-more-rf-bauble-1.20/
2. ✅ 配置build.gradle (NeoForge 47.1.106)
3. ✅ 创建Mod主类和注册系统

### 备份目录结构
```
libs_backup_optional/     # 12个可选依赖JAR
src_backup_compat/
├── compat/               # 兼容代码 (PotionCore, Champions, RS, etc.)
├── integration/          # JEI集成
├── mixin/                # 外部MOD Mixin
├── resources/            # Mixin配置JSON
├── causal/               # CombinedSuppressor原版
└── shields/              # EnhancedVisualsHandler
```

## 下一步行动

1. → Phase 3.1: 迁移Block系统 (34个文件)
2. → Phase 3.2: 迁移BlockEntity系统 (29个文件)
3. → Phase 3.3: 迁移Network系统 (47个文件)
4. → Phase 4: Baubles → Curios迁移
