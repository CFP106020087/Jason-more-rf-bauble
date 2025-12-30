package com.moremod.mixin.lycanites;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.lycanitesmobs.core.entity.goals.actions.abilities.ForceGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;



@Mixin(value = ForceGoal.class, remap = false)
public class MixinForceGoal {

    @Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true)
    private void checkDimensionalAnchor(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;

        if (hasDimensionalAnchor(player)) {
            cir.setReturnValue(false);
        }
    }


    private static boolean hasDimensionalAnchor(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.isEmpty()) continue;


            String itemClass = stack.getItem().getClass().getName();
            if (itemClass.contains("ItemDimensionalAnchor")) return true;


            if (stack.getItem().getRegistryName() != null) {
                String reg = stack.getItem().getRegistryName().toString().toLowerCase(Locale.ROOT);
                if (reg.contains("dimensional_anchor") || reg.contains("dimensionalanchor")) {
                    return true;
                }
            }


            String displayName = stack.getDisplayName().toLowerCase(Locale.ROOT);
            if (displayName.contains("dimensional") && displayName.contains("anchor")) {
                return true;
            }
        }
        return false;
    }
}