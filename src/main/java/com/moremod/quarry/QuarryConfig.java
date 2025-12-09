package com.moremod.quarry;

/**
 * 量子采石场配置
 */
public class QuarryConfig {
    // 能量配置
    public static int ENERGY_CAPACITY = 1000000;           // RF 容量
    public static int ENERGY_PER_OPERATION = 32000;        // 每次操作消耗 (原8000，提升4倍)
    public static int ENERGY_TRANSFER_RATE = 50000;        // 最大输入速率
    
    // 操作配置
    public static int BASE_TICKS_PER_OPERATION = 20;       // 基础操作间隔（1秒）
    public static int MIN_TICKS_PER_OPERATION = 2;         // 最小操作间隔
    
    // 附魔效果
    public static float FORTUNE_MULTIPLIER = 0.33f;        // 时运每级增加产出概率
    public static float EFFICIENCY_SPEED_BONUS = 0.15f;    // 效率每级减少间隔比例
    public static float LOOTING_MULTIPLIER = 0.25f;        // 抢夺每级增加掉落概率
    
    // 模拟配置
    public static int VIRTUAL_CHUNK_SIZE = 16;             // 虚拟区块大小
    public static int VIRTUAL_WORLD_HEIGHT = 256;          // 虚拟世界高度
    public static int BLOCKS_PER_OPERATION = 64;           // 每次操作模拟的方块数
    
    // 怪物模拟
    public static int MOB_SPAWN_WEIGHT_TOTAL = 100;        // 怪物生成权重总值
    
    private QuarryConfig() {}
}
