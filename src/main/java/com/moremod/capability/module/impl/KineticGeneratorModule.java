package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * 动能发电模块
 *
 * 功能：
 *  - 移动时产生能量（疾跑/飞行/跳跃有额外倍率）
 *  - 挖掘方块时产生能量
 *  - Lv.1-5: 每级提升产能
 *
 * 能量产出：
 *  - 基础：5 RF/格 + 8 RF/格/级
 *  - 疾跑：1.5x
 *  - 鞘翅：2.0x
 *  - 跳跃：1.2x
 *  - 挖掘：硬度 * 10 * 等级 RF
 */
public class KineticGeneratorModule extends AbstractMechCoreModule {

    public static final KineticGeneratorModule INSTANCE = new KineticGeneratorModule();

    private KineticGeneratorModule() {
        super(
            "KINETIC_GENERATOR",
            "动能发电",
            "移动和挖掘时产生能量",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化位置数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setDouble("LAST_POS_X", player.posX);
        meta.setDouble("LAST_POS_Y", player.posY);
        meta.setDouble("LAST_POS_Z", player.posZ);
        meta.setInteger("KINETIC_BUFFER", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除位置数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.removeTag("LAST_POS_X");
        meta.removeTag("LAST_POS_Y");
        meta.removeTag("LAST_POS_Z");
        meta.setInteger("KINETIC_BUFFER", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 读取上次位置
        double lastX = meta.getDouble("LAST_POS_X");
        double lastY = meta.getDouble("LAST_POS_Y");
        double lastZ = meta.getDouble("LAST_POS_Z");

        // 初始化：第一次没有位置记录时仅更新位置，不产能
        if (lastX == 0 && lastY == 0 && lastZ == 0) {
            storePosition(player, meta);
            return;
        }

        double distance = player.getDistance(lastX, lastY, lastZ);

        // 过滤传送/超大位移
        if (distance > 0.1 && distance < 100.0) {
            // 基础每格产能
            int perBlock = EnergyBalanceConfig.KineticGenerator.ENERGY_PER_BLOCK
                    + (EnergyBalanceConfig.KineticGenerator.ENERGY_PER_LEVEL * level);
            double mult = 1.0;

            // 疾跑倍率
            if (player.isSprinting()) {
                mult *= EnergyBalanceConfig.KineticGenerator.SPRINT_MULTIPLIER;
            }

            // 鞘翅飞行倍率
            if (player.isElytraFlying()) {
                mult *= EnergyBalanceConfig.KineticGenerator.ELYTRA_MULTIPLIER;
            }

            // 跳跃倍率
            if (!player.onGround) {
                mult *= EnergyBalanceConfig.KineticGenerator.JUMP_MULTIPLIER;
            }

            // 产出 = 距离 * perBlock * 倍率
            int energy = (int) Math.floor(distance * perBlock * mult);

            // 缓冲入账
            int buffer = meta.getInteger("KINETIC_BUFFER");
            buffer += Math.max(0, energy);

            int threshold = EnergyBalanceConfig.KineticGenerator.BUFFER_THRESHOLD;
            if (buffer >= threshold) {
                data.addEnergy(buffer);
                meta.setInteger("KINETIC_BUFFER", 0);
            } else {
                meta.setInteger("KINETIC_BUFFER", buffer);
            }
        }

        // 更新位置
        storePosition(player, meta);

        // 缓冲溢出保护（每秒检查一次）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            int buffer = meta.getInteger("KINETIC_BUFFER");
            int threshold = EnergyBalanceConfig.KineticGenerator.BUFFER_THRESHOLD;
            if (buffer >= threshold * 2) {
                data.addEnergy(buffer);
                meta.setInteger("KINETIC_BUFFER", 0);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置位置
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        storePosition(player, meta);
    }

    /**
     * 存储当前位置
     */
    private void storePosition(EntityPlayer player, NBTTagCompound meta) {
        meta.setDouble("LAST_POS_X", player.posX);
        meta.setDouble("LAST_POS_Y", player.posY);
        meta.setDouble("LAST_POS_Z", player.posZ);
    }

    /**
     * 挖掘方块产能
     *
     * 此方法应该在 BlockBreakEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param hardness 方块硬度
     */
    public void generateFromBlockBreak(EntityPlayer player, IMechCoreData data, float hardness) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0 || hardness < 0) return;

        int base = EnergyBalanceConfig.KineticGenerator.BLOCK_BREAK_BASE;
        int energy = (int) Math.floor(hardness * base * level);

        if (energy > 0) {
            data.addEnergy(energy);

            // 视觉反馈（偶尔显示）
            if (player.world.rand.nextInt(10) == 0) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT,
                        player.posX, player.posY + 1, player.posZ,
                        0, 0, 0
                );
            }
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 动能发电是产能模块，无消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setDouble("LAST_POS_X", 0);
        meta.setDouble("LAST_POS_Y", 0);
        meta.setDouble("LAST_POS_Z", 0);
        meta.setInteger("KINETIC_BUFFER", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("LAST_POS_X")) meta.setDouble("LAST_POS_X", 0);
        if (!meta.hasKey("LAST_POS_Y")) meta.setDouble("LAST_POS_Y", 0);
        if (!meta.hasKey("LAST_POS_Z")) meta.setDouble("LAST_POS_Z", 0);
        if (!meta.hasKey("KINETIC_BUFFER")) meta.setInteger("KINETIC_BUFFER", 0);
        return true;
    }
}
