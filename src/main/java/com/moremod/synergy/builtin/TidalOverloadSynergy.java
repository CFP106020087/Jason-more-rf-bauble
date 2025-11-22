package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.*;

/**
 * 潮汐过载：雷云心脏 - 传奇级Synergy
 *
 * 主题：
 * 将机械核心的防水系统与能量管理系统进行病态耦合，
 * 使湿度成为一种原始的、未被驯服的能量源。
 *
 * 所需模块：
 * - ENERGY_EFFICIENCY（能量效率）
 * - WATERPROOF（防水模块）
 *
 * === 正面效果 ===
 * [湿度→能量转换引擎]
 * - 在雨中时，每秒消耗10%湿度，生成 (湿度值 × 50) RF
 * - 最高可达 5,000 RF/s (湿度100%时)
 *
 * === 负面效果（代价机制）===
 * [A] 血液燃料税
 * - 激活时永久降低2颗心(4点)最大生命值
 * - 每1%湿度转换消耗0.5点生命值
 * - 停用时恢复生命值但扣除10级经验
 *
 * [B] 能量状态锁定
 * - 能量状态强制锁定在EMERGENCY模式
 * - 高耗能模块（矿物透视、隐身、飞行）永久禁用
 *
 * [C] 雷击诅咒
 * - 湿度每20%增加雷击吸引概率
 * - 被雷击：8点伤害 + 能量溢出50%
 * - 但获得10秒过载模式（力量III + 速度II + 临时解锁所有模块）
 *
 * [D] 湿度成瘾
 * - 湿度为0时触发"脱水震颤"
 * - 挖掘疲劳IV + 缓慢II
 * - 每5秒强制消耗500 RF
 *
 * === 设计哲学 ===
 * 这是一个"恶魔交易"式的Synergy：
 * - 在能量危机时提供强大的发电能力
 * - 但代价是用生命值作为燃料
 * - 强迫玩家改变对"雨天"的认知
 * - 创造"风暴术士"式的独特玩法
 */
public class TidalOverloadSynergy {

    public static final String ID = "TIDAL_OVERLOAD";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
            .displayName("潮汐过载：雷云心脏")
            .description("湿度→能量转换 | 代价：血液燃料 + 雷击诅咒 + 能量锁定 + 湿度成瘾")

            // 所需模块组合
            .chain(ModuleChain.linear(
                "ENERGY_EFFICIENCY",    // 能量效率模块
                "WATERPROOF"            // 防水模块
            ))

            // 条件：拥有两个模块且都激活
            .condition(new ModuleCombinationCondition(
                true, // 要求激活
                "ENERGY_EFFICIENCY",
                "WATERPROOF"
            ))

            // === 效果列表 ===

            // 1. 湿度→能量转换引擎（核心正面效果）
            .effect(new TidalConversionEffect())

            // 2. 血液燃料税（主要代价）
            .effect(new BloodFuelTaxEffect())

            // 3. 能量状态锁定（限制性代价）
            .effect(new EnergyStateLockEffect())

            // 4. 雷击诅咒（风险与机遇并存）
            .effect(new LightningRodCurseEffect())

            // 5. 湿度成瘾（强制依赖）
            .effect(new MoistureAddictionEffect())

            .priority(200) // 传奇级优先级
            .enabled(true)
            .build();
    }
}
