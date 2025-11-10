package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.entity.IEntityLivingBase;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * CombatHelper (1.12.2 · no DamageSource edits)
 * - 无敌帧控制（含反射清/读 lastDamage）
 * - 真实伤害/百分比伤害/护盾处理/治疗
 * - 位移与硬直：击退/拉拽/击飞/冲刺/点燃/受伤动画
 * - 药水封装：增/查/删/清
 * - AOE 真实伤害扫击（可绕过无敌帧）
 * - 目标标记：mark/has/consume
 *
 * 说明：请在服务端调用；从 ZS 调用时先判断 world.remote。
 */
@ZenRegister
@ZenClass("mods.moremod.CombatHelper")
public class CombatHelper {

    /* ===================== 反射：lastDamage ===================== */

    // MCP 名 & SRG 名（1.12.2）
    private static final String LAST_DAMAGE_MCP = "lastDamage";
    private static final String LAST_DAMAGE_SRG = "field_110153_bc";

    // 缓存字段，尽量减少反射开销
    private static final Field LAST_DAMAGE_FIELD;

    static {
        Field f = null;
        try {
            // Dev 环境优先 MCP 名；生产环境退回 SRG 名
            f = ReflectionHelper.findField(EntityLivingBase.class, LAST_DAMAGE_MCP, LAST_DAMAGE_SRG);
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        LAST_DAMAGE_FIELD = f;
    }

    private static void setLastDamage(EntityLivingBase e, float v) {
        if (e == null) return;
        try {
            if (LAST_DAMAGE_FIELD != null) {
                LAST_DAMAGE_FIELD.setFloat(e, v);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            // 兜底：Obf helper（只接受 SRG 名）
            ObfuscationReflectionHelper.setPrivateValue(EntityLivingBase.class, e, Float.valueOf(v), LAST_DAMAGE_SRG);
        } catch (Throwable ignored) {}
    }

    private static float getLastDamageValue(EntityLivingBase e) {
        if (e == null) return -1f;
        try {
            if (LAST_DAMAGE_FIELD != null) {
                return LAST_DAMAGE_FIELD.getFloat(e);
            }
        } catch (Throwable ignored) {}
        try {
            Float v = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, e, LAST_DAMAGE_SRG);
            return v != null ? v.floatValue() : -1f;
        } catch (Throwable ignored) {}
        return -1f;
    }

    /* ========================= 基础判定 ========================= */

    @ZenMethod public static boolean isPlayer(IEntity any)   { return asEntity(any) instanceof EntityPlayer; }
    @ZenMethod public static boolean isMob(IEntity any)      { return asEntity(any) instanceof IMob; }
    @ZenMethod public static boolean isAnimal(IEntity any)   { return asEntity(any) instanceof EntityAnimal; }
    @ZenMethod public static boolean isVillager(IEntity any) { return asEntity(any) instanceof EntityVillager; }

    /* ========================= 无敌帧 I-frames ========================= */

    /** 清空无敌帧：设置 hurtResistantTime=0 且反射清 lastDamage（影响“下一下”） */
    @ZenMethod
    public static boolean clearIFrames(IEntity victim) {
        EntityLivingBase e = asLiving(victim);
        if (e == null) return false;
        e.hurtResistantTime = 0;
        setLastDamage(e, 0f);
        return true;
    }

    /** 设置无敌帧（tick；<=0 等价 clearIFrames） */
    @ZenMethod
    public static boolean setIFrames(IEntity victim, int ticks) {
        EntityLivingBase e = asLiving(victim);
        if (e == null) return false;
        e.hurtResistantTime = Math.max(0, ticks);
        if (ticks <= 0) setLastDamage(e, 0f);
        return true;
    }

    @ZenMethod public static int   getIFrames(IEntity v)     { EntityLivingBase e = asLiving(v); return e == null ? -1 : e.hurtResistantTime; }
    @ZenMethod public static float getLastDamage(IEntity v)  { EntityLivingBase e = asLiving(v); return getLastDamageValue(e); }

    /* ========================= 直接改血 / 护盾处理 ========================= */

    /** 真实伤害（不走护甲/药水/无敌帧）：直接改 health；返回实际伤害量 */
    @ZenMethod
    public static float trueDamage(IEntity victim, float amount, @Optional IEntity attacker) {
        EntityLivingBase v = asLiving(victim);
        if (v == null || amount <= 0f) return 0f;
        float before = v.getHealth();
        float after  = Math.max(0f, before - amount);
        v.setHealth(after);
        return before - after;
    }

    /** 百分比最大生命伤害（真实伤害） */
    @ZenMethod
    public static float percentDamageMax(IEntity victim, float percent, @Optional IEntity attacker) {
        EntityLivingBase v = asLiving(victim);
        if (v == null || percent <= 0f) return 0f;
        float amt = Math.max(0f, v.getMaxHealth() * (percent / 100f));
        return trueDamage(victim, amt, attacker);
    }

    /** 百分比当前生命伤害（真实伤害） */
    @ZenMethod
    public static float percentDamageCurrent(IEntity victim, float percent, @Optional IEntity attacker) {
        EntityLivingBase v = asLiving(victim);
        if (v == null || percent <= 0f) return 0f;
        float amt = Math.max(0f, v.getHealth() * (percent / 100f));
        return trueDamage(victim, amt, attacker);
    }

    /** 先削吸收护盾再伤血；返回总实际伤害 */
    @ZenMethod
    public static float damageWithAbsorb(IEntity victim, float amount) {
        EntityLivingBase v = asLiving(victim);
        if (v == null || amount <= 0f) return 0f;
        float left = amount;

        float ab = v.getAbsorptionAmount();
        if (ab > 0f) {
            float use = Math.min(ab, left);
            v.setAbsorptionAmount(ab - use);
            left -= use;
        }
        if (left > 0f) {
            float hp = v.getHealth();
            float after = Math.max(0f, hp - left);
            v.setHealth(after);
        }
        return amount;
    }

    /** 只削护盾（不伤生命） */
    @ZenMethod
    public static float damageAbsorbOnly(IEntity victim, float amount) {
        EntityLivingBase v = asLiving(victim);
        if (v == null || amount <= 0f) return 0f;
        float ab = v.getAbsorptionAmount();
        float use = Math.min(ab, amount);
        v.setAbsorptionAmount(ab - use);
        return use;
    }

    /** 治疗（直接改 health），返回实际恢复量 */
    @ZenMethod
    public static float heal(IEntity target, float amount) {
        EntityLivingBase v = asLiving(target);
        if (v == null || amount <= 0f) return 0f;
        float before = v.getHealth();
        float after  = Math.min(v.getMaxHealth(), before + amount);
        v.setHealth(after);
        return after - before;
    }

    /** 读取/设置吸收护盾 */
    @ZenMethod public static float  getAbsorption(IEntity target)             { EntityLivingBase v = asLiving(target); return v == null ? 0f : v.getAbsorptionAmount(); }
    @ZenMethod public static boolean setAbsorption(IEntity target, float amt) { EntityLivingBase v = asLiving(target); if (v == null) return false; v.setAbsorptionAmount(Math.max(0f, amt)); return true; }

    /* ========================= 位移/硬直/点燃 ========================= */

    /** 击退（vanilla 方向逻辑） */
    @ZenMethod
    public static boolean knockback(IEntity victim, @Optional IEntity attacker, float strength) {
        EntityLivingBase v = asLiving(victim);
        if (v == null) return false;
        double dx, dz;
        Entity a = asEntity(attacker);
        if (a != null) {
            dx = v.posX - a.posX;
            dz = v.posZ - a.posZ;
        } else {
            double yaw = Math.toRadians(v.rotationYaw);
            dx = -Math.sin(yaw);
            dz =  Math.cos(yaw);
        }
        v.knockBack(a, strength, dx, dz);
        return true;
    }

    /** 叠加速度向量（可标记 velocityChanged 强制同步） */
    @ZenMethod
    public static boolean addMotion(IEntity target, double mx, double my, double mz, boolean markVelocityChanged) {
        Entity e = asEntity(target);
        if (e == null) return false;
        e.motionX += mx; e.motionY += my; e.motionZ += mz;
        if (markVelocityChanged && e instanceof EntityLivingBase) ((EntityLivingBase) e).velocityChanged = true;
        return true;
    }

    /** 垂直击飞 */
    @ZenMethod
    public static boolean knockUp(IEntity target, double yVelocity) {
        Entity e = asEntity(target);
        if (e == null) return false;
        e.motionY += yVelocity;
        if (e instanceof EntityLivingBase) ((EntityLivingBase) e).velocityChanged = true;
        return true;
    }

    /** 拉向 source（带轻微上抬） */
    @ZenMethod
    public static boolean pullTowards(IEntity victim, IEntity source, float strength, float yBoost) {
        Entity v = asEntity(victim), s = asEntity(source);
        if (!(v instanceof EntityLivingBase) || s == null) return false;
        double dx = s.posX - v.posX, dy = s.posY - v.posY, dz = s.posZ - v.posZ;
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6) return false;
        v.motionX += strength * (dx / len);
        v.motionY += strength * (dy / len) + yBoost;
        v.motionZ += strength * (dz / len);
        ((EntityLivingBase) v).velocityChanged = true;
        return true;
    }

    /** 按朝向冲刺 */
    @ZenMethod
    public static boolean dashForward(IEntity attacker, double speed, double yBoost) {
        Entity a = asEntity(attacker);
        if (a == null) return false;
        double yaw = Math.toRadians(a.rotationYaw);
        a.motionX += -Math.sin(yaw) * speed;
        a.motionZ +=  Math.cos(yaw) * speed;
        a.motionY += yBoost;
        if (a instanceof EntityLivingBase) ((EntityLivingBase) a).velocityChanged = true;
        return true;
    }

    /** 点燃（秒） */
    @ZenMethod
    public static boolean ignite(IEntity victim, int seconds) {
        Entity e = asEntity(victim);
        if (e == null) return false;
        e.setFire(Math.max(0, seconds));
        return true;
    }

    /** 设置受伤动画时长（不影响无敌帧） */
    @ZenMethod
    public static boolean setHurtAnimation(IEntity victim, int ticks) {
        EntityLivingBase v = asLiving(victim);
        if (v == null) return false;
        int t = Math.max(0, ticks);
        v.maxHurtTime = t;
        v.hurtTime = t;
        return true;
    }

    /* ========================= 药水效果 ========================= */

    private static Potion getPotion(String id) {
        if (id == null) return null;
        return Potion.REGISTRY.getObject(new ResourceLocation(id));
    }

    @ZenMethod
    public static boolean addEffect(IEntity target, String potionId, int durationTicks, int amplifier, boolean ambient, boolean showParticles) {
        EntityLivingBase v = asLiving(target);
        Potion p = getPotion(potionId);
        if (v == null || p == null || durationTicks <= 0) return false;
        v.addPotionEffect(new PotionEffect(p, durationTicks, Math.max(0, amplifier), ambient, showParticles));
        return true;
    }

    @ZenMethod
    public static boolean hasEffect(IEntity target, String potionId) {
        EntityLivingBase v = asLiving(target);
        Potion p = getPotion(potionId);
        if (v == null || p == null) return false;
        return v.isPotionActive(p);
    }

    @ZenMethod
    public static boolean removeEffect(IEntity target, String potionId) {
        EntityLivingBase v = asLiving(target);
        Potion p = getPotion(potionId);
        if (v == null || p == null) return false;
        v.removeActivePotionEffect(p);
        return true;
    }

    @ZenMethod
    public static boolean clearAllEffects(IEntity target) {
        EntityLivingBase v = asLiving(target);
        if (v == null) return false;
        v.clearActivePotions();
        return true;
    }

    /* ========================= AOE（真实伤害版） ========================= */

    /** 以 attacker 为圆心扫击真实伤害；bypassIFrames=true 时先清 i-frame+lastDamage（反射） */
    @ZenMethod
    public static int sweepTrue(IEntity attacker, double radius, float amount, boolean bypassIFrames) {
        Entity a = asEntity(attacker);
        if (!(a instanceof EntityLivingBase) || amount <= 0f || radius <= 0) return 0;
        EntityLivingBase src = (EntityLivingBase) a;
        World w = src.world;

        AxisAlignedBB box = src.getEntityBoundingBox().grow(radius);
        List<EntityLivingBase> list = w.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> e != src && e.isEntityAlive());
        int hits = 0;
        for (EntityLivingBase v : list) {
            if (bypassIFrames) {
                v.hurtResistantTime = 0;
                setLastDamage(v, 0f);
            }
            float before = v.getHealth();
            float after  = Math.max(0f, before - amount);
            v.setHealth(after);
            hits++;
        }
        return hits;
    }

    /* ========================= 标记/连携窗口 ========================= */

    private static String tagKey(String k) { return "moremod$mark$" + k; }

    /** 标记目标：key 在 durationTicks 内有效（可做“下一击暴击窗口”） */
    @ZenMethod
    public static boolean mark(IEntity target, String key, int durationTicks) {
        Entity e = asEntity(target);
        if (e == null || key == null || key.isEmpty() || durationTicks <= 0) return false;
        long now = e.world.getTotalWorldTime();
        e.getEntityData().setLong(tagKey(key), now + durationTicks);
        return true;
    }

    @ZenMethod
    public static boolean hasMark(IEntity target, String key) {
        Entity e = asEntity(target);
        if (e == null || key == null || key.isEmpty()) return false;
        long now = e.world.getTotalWorldTime();
        return e.getEntityData().getLong(tagKey(key)) > now;
    }

    @ZenMethod
    public static boolean consumeMark(IEntity target, String key) {
        Entity e = asEntity(target);
        if (e == null || key == null || key.isEmpty()) return false;
        long now = e.world.getTotalWorldTime();
        String k = tagKey(key);
        long until = e.getEntityData().getLong(k);
        if (until > now) { e.getEntityData().removeTag(k); return true; }
        return false;
    }

    /* ========================= 私有工具 ========================= */

    private static Entity asEntity(IEntity any) {
        if (any == null) return null;
        Object o = any.getInternal();
        return (o instanceof Entity) ? (Entity) o : null;
    }
    private static EntityLivingBase asLiving(IEntity any) {
        if (any == null) return null;
        Object o = any.getInternal();
        return (o instanceof EntityLivingBase) ? (EntityLivingBase) o : null;
    }
    private static EntityLivingBase asLiving(IEntityLivingBase any) {
        if (any == null) return null;
        Object o = any.getInternal();
        return (o instanceof EntityLivingBase) ? (EntityLivingBase) o : null;
    }
}
