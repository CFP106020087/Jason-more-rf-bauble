package com.moremod.compat.crafttweaker;

import com.moremod.entity.EntitySwordBeam;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import com.google.common.collect.Multimap;

/**
 * CraftTweaker 剑气辅助类 - 全 GeckoLib 版本
 * 
 * 使用方法（ZenScript）：
 * 
 * import mods.moremod.SwordBeamHelper;
 * 
 * // 普通剑气（自动继承武器伤害）
 * SwordBeamHelper.shootBeam(player, 10.0, 1.5);
 * 
 * // 自定义剑气（支持所有7种类型）
 * SwordBeamHelper.shootCustomBeam(player, "DRAGON", 20.0, 1.0, 1.0, 0.8, 0.2, 2.0, 3);
 * SwordBeamHelper.shootCustomBeam(player, "BALL", 15.0, 0.8, 0.2, 1.0, 0.8, 1.5, 5);
 * 
 * // 扇形多重剑气
 * SwordBeamHelper.shootMultiBeam(player, 5, 15.0, 10.0, 1.5);
 * 
 * // 环形剑气
 * SwordBeamHelper.shootCircleBeam(player, 8, 10.0, 1.5);
 * 
 * // 武器伤害倍率剑气
 * SwordBeamHelper.shootBeamWithMultiplier(player, 0.5, 1.5);  // 50% 武器伤害
 * 
 * // 便捷方法
 * SwordBeamHelper.shootDragonBeam(player, 30.0, 1.0);  // 龙形
 * SwordBeamHelper.shootBallBeam(player, 15.0, 0.8);    // 球形
 */
@ZenRegister
@ZenClass("mods.moremod.SwordBeamHelper")
public class SwordBeamHelper {

    /**
     * 发射普通剑气（自动继承武器伤害）
     * 完全兼容原有API
     */
    @ZenMethod
    public static void shootBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        // 设置发射方向
        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        // 计算最终伤害：基础伤害 + 武器伤害
        float finalDamage = baseDamage + getWeaponDamage(p);
        beam.setDamage(finalDamage)
            .setBeamType(EntitySwordBeam.BeamType.NORMAL)
            .setColor(0.5F, 0.8F, 1.0F)  // 淡蓝色
            .setScale(1.0F)
            .setMaxLifetime(60);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射自定义剑气（自动继承武器伤害）
     * 完全兼容原有API，支持所有7种类型
     * 
     * @param type 类型：NORMAL, SPIRAL, CRESCENT, CROSS, DRAGON, PHOENIX, BALL
     */
    @ZenMethod
    public static void shootCustomBeam(IPlayer player, String type,
                                       float baseDamage, float speed,
                                       float red, float green, float blue,
                                       float scale, int penetrate) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        // 设置类型
        EntitySwordBeam.BeamType beamType;
        try {
            beamType = EntitySwordBeam.BeamType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            beamType = EntitySwordBeam.BeamType.NORMAL;
        }

        // 计算最终伤害
        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(beamType)
            .setDamage(finalDamage)
            .setColor(red, green, blue)
            .setScale(scale)
            .setPenetrate(penetrate)
            .setMaxLifetime(getDefaultLifetime(beamType));

        // 发射
        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射多重剑气（扇形）（自动继承武器伤害）
     * 完全兼容原有API
     */
    @ZenMethod
    public static void shootMultiBeam(IPlayer player, int count,
                                      float spreadAngle, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        Vec3d look = p.getLookVec();
        float finalDamage = baseDamage + getWeaponDamage(p);

        for (int i = 0; i < count; i++) {
            EntitySwordBeam beam = new EntitySwordBeam(p.world, p);
            beam.setDamage(finalDamage)
                .setColor(0.5F + i * 0.1F, 0.8F, 1.0F)  // 渐变色
                .setScale(0.9F);

            // 计算偏移角度
            float angleOffset = spreadAngle * (i - (count - 1) / 2.0F);
            Vec3d rotated = rotateVectorAroundY(look, angleOffset);

            beam.shoot(rotated.x, rotated.y, rotated.z, speed, 0.0F);
            p.world.spawnEntity(beam);
        }
    }

    /**
     * 发射环形剑气（自动继承武器伤害）
     * 完全兼容原有API
     */
    @ZenMethod
    public static void shootCircleBeam(IPlayer player, int count,
                                       float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        float finalDamage = baseDamage + getWeaponDamage(p);

        for (int i = 0; i < count; i++) {
            EntitySwordBeam beam = new EntitySwordBeam(p.world, p);
            beam.setDamage(finalDamage)
                .setColor(0.2F + i * 0.1F, 0.8F, 1.0F - i * 0.1F)  // 彩虹色
                .setScale(0.9F);

            // 360度均匀分布
            double angle = (2 * Math.PI * i) / count;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);

            beam.shoot(vx, 0, vz, speed, 0.0F);
            p.world.spawnEntity(beam);
        }
    }

    /**
     * 发射武器伤害倍率的剑气
     * 完全兼容原有API
     * 
     * @param damageMultiplier 武器伤害的倍率（例如0.5表示50%武器伤害）
     */
    @ZenMethod
    public static void shootBeamWithMultiplier(IPlayer player, float damageMultiplier, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        // 武器伤害 × 倍率
        float damage = getWeaponDamage(p) * damageMultiplier;
        beam.setDamage(damage);

        p.world.spawnEntity(beam);
    }

    // ========================================
    // 便捷方法
    // ========================================

    /**
     * 发射球形光波（便捷方法）
     * 自动使用最佳配置
     */
    @ZenMethod
    public static void shootBallBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.BALL)
            .setDamage(finalDamage)
            .setColor(0.2F, 1.0F, 0.8F)  // 青绿色
            .setScale(1.4F)
            .setPenetrate(5)  // 穿透型
            .setMaxLifetime(100);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射龙形剑气（便捷方法）
     * 自动使用最佳配置
     */
    @ZenMethod
    public static void shootDragonBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.DRAGON)
            .setDamage(finalDamage)
            .setColor(1.0F, 0.8F, 0.2F)  // 金色
            .setScale(2.0F)
            .setPenetrate(3)
            .setMaxLifetime(120);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射螺旋剑气（便捷方法）
     */
    @ZenMethod
    public static void shootSpiralBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.SPIRAL)
            .setDamage(finalDamage)
            .setColor(1.0F, 0.3F, 1.0F)  // 紫色
            .setScale(1.2F)
            .setPenetrate(1)
            .setMaxLifetime(80);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射月牙斩（便捷方法）
     */
    @ZenMethod
    public static void shootCrescentBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.CRESCENT)
            .setDamage(finalDamage)
            .setColor(0.8F, 0.8F, 1.0F)  // 银白色
            .setScale(1.5F)
            .setPenetrate(2)
            .setMaxLifetime(70);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射十字斩（便捷方法）
     */
    @ZenMethod
    public static void shootCrossBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.CROSS)
            .setDamage(finalDamage)
            .setColor(1.0F, 1.0F, 1.0F)  // 纯白色
            .setScale(1.3F)
            .setPenetrate(2)
            .setMaxLifetime(75);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射凤凰剑气（便捷方法）
     */
    @ZenMethod
    public static void shootPhoenixBeam(IPlayer player, float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        EntitySwordBeam beam = new EntitySwordBeam(p.world, p);

        float finalDamage = baseDamage + getWeaponDamage(p);

        beam.setBeamType(EntitySwordBeam.BeamType.PHOENIX)
            .setDamage(finalDamage)
            .setColor(1.0F, 0.3F, 0.1F)  // 火红色
            .setScale(1.8F)
            .setPenetrate(2)
            .setMaxLifetime(90);

        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射剑气阵列（全屏攻击）
     */
    @ZenMethod
    public static void shootBeamArray(IPlayer player, int rows, int cols,
                                      float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        float finalDamage = baseDamage + getWeaponDamage(p);
        Vec3d look = p.getLookVec();
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d up = new Vec3d(0, 1, 0);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                EntitySwordBeam beam = new EntitySwordBeam(p.world, p);
                beam.setDamage(finalDamage * 0.6F)  // 减少单个伤害
                    .setScale(0.8F)
                    .setColor(0.5F + col * 0.1F, 0.8F, 1.0F - row * 0.15F);

                // 计算偏移位置
                float offsetX = (col - cols / 2.0F) * 1.5F;
                float offsetY = (row - rows / 2.0F) * 1.5F;

                Vec3d offset = right.scale(offsetX).add(up.scale(offsetY));
                Vec3d startPos = p.getPositionVector().add(0, p.getEyeHeight(), 0).add(offset);

                beam.setPosition(startPos.x, startPos.y, startPos.z);
                beam.shoot(look.x, look.y, look.z, speed, 0.1F);

                p.world.spawnEntity(beam);
            }
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 获取玩家主手武器的攻击伤害
     */
    private static float getWeaponDamage(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty()) {
            return 0.0F;
        }

        // 获取武器的攻击伤害属性
        Multimap<String, AttributeModifier> attributes =
                heldItem.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);

        java.util.Collection<AttributeModifier> damageModifiers =
                attributes.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

        float weaponDamage = 0.0F;
        for (AttributeModifier modifier : damageModifiers) {
            weaponDamage += (float) modifier.getAmount();
        }

        return weaponDamage;
    }

    /**
     * 绕Y轴旋转向量
     */
    private static Vec3d rotateVectorAroundY(Vec3d vec, float degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double newX = vec.x * cos - vec.z * sin;
        double newZ = vec.x * sin + vec.z * cos;

        return new Vec3d(newX, vec.y, newZ);
    }

    /**
     * 获取剑气类型的默认生命周期
     */
    private static int getDefaultLifetime(EntitySwordBeam.BeamType type) {
        switch (type) {
            case DRAGON:
                return 120;
            case BALL:
                return 100;
            case PHOENIX:
                return 90;
            case SPIRAL:
                return 80;
            case CROSS:
            case CRESCENT:
                return 75;
            default:
                return 60;
        }
    }
}