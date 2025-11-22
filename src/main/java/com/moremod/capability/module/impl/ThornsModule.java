package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 反伤荆棘模块
 *
 * 功能：
 *  - 受到伤害时反弹部分伤害给攻击者
 *  - Lv.1: 15% 反伤
 *  - Lv.2: 30% 反伤
 *  - Lv.3: 45% 反伤
 *
 * 能量消耗：
 *  - 无被动消耗（纯被动系统）
 *  - 反伤触发时无额外消耗
 */
public class ThornsModule extends AbstractMechCoreModule {

    public static final ThornsModule INSTANCE = new ThornsModule();

    private ThornsModule() {
        super(
            "THORNS",
            "反伤荆棘",
            "受到伤害时反弹部分伤害",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 反伤模块无需激活逻辑
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 反伤模块无需停用逻辑
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 反伤模块不需要 tick 逻辑（纯事件驱动）
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时无需特殊处理
    }

    /**
     * 应用反伤效果
     *
     * 此方法应该在 LivingHurtEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param attacker 攻击者
     * @param originalDamage 原始伤害
     */
    public void applyThorns(EntityPlayer player, IMechCoreData data, EntityLivingBase attacker, float originalDamage) {
        if (attacker == null) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 反伤比例：15%/30%/45%
        float reflectRatio = 0.15F * level;
        float damage = originalDamage * reflectRatio;

        if (damage > 0) {
            // 造成荆棘伤害
            attacker.attackEntityFrom(DamageSource.causeThornsDamage(player), damage);

            // 视觉效果
            player.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    attacker.posX, attacker.posY + attacker.height / 2, attacker.posZ,
                    0, 0, 0
            );

            // 提示信息（降低频率避免刷屏）
            if (player.world.rand.nextInt(5) == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + String.format("⚔ 反伤 %.1f 点", damage)
                ), true);
            }
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 反伤模块无被动消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 反伤模块总是可以执行（无能量需求）
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 反伤模块无需元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
