package com.moremod.mixin.parasites;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

@Pseudo
@Mixin(
        targets = {
                "com.dhanantry.scapeandrunparasites.entity.monster.adapted.EntityRanracAdapted",
                "com.dhanantry.scapeandrunparasites.entity.monster.primitive.EntityRanrac"
        },
        remap = false,
        priority = 2000
)
public class MixinEntityRanracAdapted {

    @Inject(method = {"onLivingUpdate", "func_70636_d"}, at = @At("TAIL"), require = 0)
    private void moremod$clearPullTarget(CallbackInfo ci) {
        try {
            // 获取targetedEntity字段
            EntityLivingBase targeted = (EntityLivingBase) moremod$getField(this, "targetedEntity", EntityLivingBase.class);
            if (targeted instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) targeted)) {
                // 清除拉扯相关状态
                moremod$invoke(this, new String[]{"setTargetedEntity"}, 0, int.class);
                moremod$setBooleanField(this, "canPull", false, "pull");
                moremod$setIntField(this, "pulling", 60, "pull");

                // 清除缓存字段
                Field targetedField = moremod$findField(this.getClass(), "targetedEntity", EntityLivingBase.class);
                if (targetedField != null) {
                    targetedField.set(this, null);
                }
            }

            // 也检查getTargetedEntity()方法的返回值
            Object methodTarget = moremod$invoke(this, new String[]{"getTargetedEntity"});
            if (methodTarget instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) methodTarget)) {
                moremod$invoke(this, new String[]{"setTargetedEntity"}, 0, int.class);
                moremod$setBooleanField(this, "canPull", false, "pull");
            }

        } catch (Throwable ignored) {}
    }

    // ===== 阻止pullingE技能对玩家发射 =====
    @Inject(method = "pullingE", at = @At("HEAD"), cancellable = true, require = 0)
    private void moremod$cancelPullSkillForAnchor(CallbackInfo ci) {
        try {
            Object target = moremod$invoke(this, new String[]{"getAttackTarget", "func_70638_az"});
            if (target instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) target)) {
                // 调用resetPullSkill并取消技能
                moremod$invoke(this, new String[]{"resetPullSkill"});
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

    // ===== 饰品检测 =====
    @Unique
    private static boolean moremod$hasDimensionalAnchor(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return false;

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (stack == null || stack.isEmpty()) continue;

                String cls = stack.getItem().getClass().getName();
                if (cls.contains("ItemDimensionalAnchor")) return true;

                if (stack.getItem().getRegistryName() != null) {
                    String reg = stack.getItem().getRegistryName().toString().toLowerCase(Locale.ROOT);
                    if (reg.contains("dimensional_anchor") || reg.contains("dimensionalanchor")) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ===== 反射工具方法 =====
    @Unique
    private static Object moremod$invoke(Object target, String[] names) throws Exception {
        return moremod$invoke(target, names, null, new Class<?>[0]);
    }

    @Unique
    private static Object moremod$invoke(Object target, String[] names, Object arg, Class<?>... types) throws Exception {
        Exception last = null;
        for (String n : names) {
            try {
                Method m = moremod$findMethod(target.getClass(), n, types);
                if (m != null) {
                    return (types.length == 0) ? m.invoke(target) : m.invoke(target, arg);
                }
            } catch (Exception e) { last = e; }
        }
        if (last != null) throw last;
        return null;
    }

    @Unique
    private static Method moremod$findMethod(Class<?> c, String name, Class<?>... types) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == types.length) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    @Unique
    private static Field moremod$findField(Class<?> c, String name, Class<?> type) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    @Unique
    private static Object moremod$getField(Object o, String name, Class<?> type) throws Exception {
        Field f = moremod$findField(o.getClass(), name, type);
        return f == null ? null : f.get(o);
    }

    @Unique
    private static void moremod$setBooleanField(Object o, String exactName, boolean val, String fallback) {
        try {
            Field f = moremod$findField(o.getClass(), exactName, boolean.class);
            if (f != null) f.setBoolean(o, val);
        } catch (Throwable ignored) {}
    }

    @Unique
    private static void moremod$setIntField(Object o, String exactName, int val, String fallback) {
        try {
            Field f = moremod$findField(o.getClass(), exactName, int.class);
            if (f != null) f.setInt(o, val);
        } catch (Throwable ignored) {}
    }
}