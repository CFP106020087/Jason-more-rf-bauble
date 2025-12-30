package com.moremod.mixin.parasites;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.dhanantry.scapeandrunparasites.entity.monster.deterrent.EntityNak",
        remap = false,
        priority = 2000
)
public class MixinEntityNak {

    /**
     * 先阻止设置目标
     */
    @Inject(
            method = "setTargetedEntity",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void blockAnchoredPlayerTarget(int entityId, CallbackInfo ci) {
        if (entityId != 0) {
            try {
                net.minecraft.entity.Entity self = (net.minecraft.entity.Entity)(Object)this;
                net.minecraft.entity.Entity target = self.world.getEntityByID(entityId);

                if (target instanceof EntityPlayer && hasDimensionalAnchor((EntityPlayer)target)) {
                    ci.cancel();
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * 备用方案：在onLivingUpdate开始时清除目标
     */
    @Inject(
            method = "onLivingUpdate",
            at = @At(value = "INVOKE",
                    target = "Lcom/dhanantry/scapeandrunparasites/entity/monster/deterrent/EntityNak;getTargetedEntity()Lnet/minecraft/entity/EntityLivingBase;"),
            require = 0
    )
    private void clearTargetIfAnchor(CallbackInfo ci) {
        try {
            // 获取targetedEntity
            java.lang.reflect.Method getTarget = this.getClass().getDeclaredMethod("getTargetedEntity");
            getTarget.setAccessible(true);
            Object target = getTarget.invoke(this);

            if (target instanceof EntityPlayer && hasDimensionalAnchor((EntityPlayer)target)) {
                // 清除目标
                java.lang.reflect.Method setTarget = this.getClass().getDeclaredMethod("setTargetedEntity", int.class);
                setTarget.setAccessible(true);
                setTarget.invoke(this, 0);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 拦截dismountRidingEntity调用
     */
    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;dismountRidingEntity()V"
            ),
            require = 0
    )
    private void preventDismount(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || !hasDimensionalAnchor((EntityPlayer)entity)) {
            entity.dismountRidingEntity();
        }
    }

    private static boolean hasDimensionalAnchor(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return false;

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (stack == null || stack.isEmpty()) continue;

                String cls = stack.getItem().getClass().getName();
                if (cls.contains("ItemDimensionalAnchor")) return true;

                if (stack.getItem().getRegistryName() != null) {
                    String reg = stack.getItem().getRegistryName().toString().toLowerCase();
                    if (reg.contains("dimensional_anchor") || reg.contains("dimensionalanchor")) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}