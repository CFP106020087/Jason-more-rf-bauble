package com.moremod.mixin.parasites;

import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase",
        remap = false,
        priority = 2000
)
public class MixinEntityParasiteBaseCap {

    @Unique
    private static final ThreadLocal<Boolean> moremod$shouldBypass =
            ThreadLocal.withInitial(() -> false);

    @Unique
    private static final String moremod_NS = "moremod:"; // 你的命名空间

    @Inject(
            method = {
                    "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"
            },
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void moremod$captureSource(DamageSource source, float amount,
                                       CallbackInfoReturnable<Boolean> cir) {
        moremod$shouldBypass.set(moremod$holdingmoremodWeapon(source));
    }

    @ModifyArg(
            method = {
                    "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"
            },
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", remap = false),
            index = 1,
            require = 0,
            remap = false
    )
    private float moremod$conditionalRaiseCap(float cap) {
        return moremod$shouldBypass.get() ? Float.MAX_VALUE : cap;
    }

    @Inject(
            method = {
                    "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"
            },
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void moremod$cleanup(DamageSource source, float amount,
                                 CallbackInfoReturnable<Boolean> cir) {
        moremod$shouldBypass.remove();
    }

    private boolean moremod$holdingmoremodWeapon(Object damageSource) {
        try {
            Object src = tryInvoke(damageSource, "getTrueSource", "func_76346_g");
            if (!isPlayer(src)) {
                src = tryInvoke(damageSource, "getImmediateSource", "func_76364_f");
                if (!isPlayer(src)) return false;
            }
            Object stack = tryInvoke(src, "getHeldItemMainhand", "func_184614_ca");
            if (stack == null) return false;
            Object empty = tryInvoke(stack, "isEmpty", "func_190926_b");
            if (empty instanceof Boolean && (Boolean) empty) return false;
            Object item = tryInvoke(stack, "getItem", "func_77973_b");
            if (item == null) return false;
            Object rl = item.getClass().getMethod("getRegistryName").invoke(item);
            String reg = rl == null ? "" : rl.toString();
            return reg.startsWith(moremod_NS);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isPlayer(Object o) {
        if (o == null) return false;
        try {
            return Class.forName("net.minecraft.entity.player.EntityPlayer").isInstance(o);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 依次尝试 MCP / SRG 名称
    private static Object tryInvoke(Object target, String mcp, String srg, Object... args) throws Exception {
        Exception last = null;
        for (String name : new String[]{mcp, srg}) {
            try {
                Class<?> c = target.getClass();
                while (c != null) {
                    for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                        if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                            m.setAccessible(true);
                            return m.invoke(target, args);
                        }
                    }
                    c = c.getSuperclass();
                }
            } catch (Exception e) { last = e; }
        }
        if (last != null) throw last;
        return null;
    }
}
