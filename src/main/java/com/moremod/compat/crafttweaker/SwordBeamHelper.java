// SwordBeamHelper.java
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

@ZenRegister
@ZenClass("mods.moremod.SwordBeamHelper")
public class SwordBeamHelper {

    /**
     * 发射普通剑气（自动继承武器伤害）
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
        beam.setDamage(finalDamage);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射自定义剑气（自动继承武器伤害）
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
                .setPenetrate(penetrate);

        // 发射
        Vec3d look = p.getLookVec();
        beam.shoot(look.x, look.y, look.z, speed, 0.0F);

        p.world.spawnEntity(beam);
    }

    /**
     * 发射多重剑气（扇形）（自动继承武器伤害）
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
            beam.setDamage(finalDamage);

            // 计算偏移角度
            float angleOffset = spreadAngle * (i - (count - 1) / 2.0F);
            Vec3d rotated = rotateVectorAroundY(look, angleOffset);

            beam.shoot(rotated.x, rotated.y, rotated.z, speed, 0.0F);
            p.world.spawnEntity(beam);
        }
    }

    /**
     * 发射环形剑气（自动继承武器伤害）
     */
    @ZenMethod
    public static void shootCircleBeam(IPlayer player, int count,
                                       float baseDamage, float speed) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        if (p.world.isRemote) return;

        float finalDamage = baseDamage + getWeaponDamage(p);

        for (int i = 0; i < count; i++) {
            EntitySwordBeam beam = new EntitySwordBeam(p.world, p);
            beam.setDamage(finalDamage);

            // 360度均匀分布
            double angle = (2 * Math.PI * i) / count;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);

            beam.shoot(vx, 0, vz, speed, 0.0F);
            p.world.spawnEntity(beam);
        }
    }

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
     * 新方法：发射武器伤害倍率的剑气
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

    // 辅助：绕Y轴旋转向量
    private static Vec3d rotateVectorAroundY(Vec3d vec, float degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double newX = vec.x * cos - vec.z * sin;
        double newZ = vec.x * sin + vec.z * cos;

        return new Vec3d(newX, vec.y, newZ);
    }
}