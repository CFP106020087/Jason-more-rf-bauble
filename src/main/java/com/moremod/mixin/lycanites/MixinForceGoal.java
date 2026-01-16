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
    private void checkDisplacementImmunity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;

        if (hasDisplacementImmunity(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 检查玩家是否有位移免疫（维度锚定器 或 灵魂锚点）
     */
    private static boolean hasDisplacementImmunity(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);

        // 检查维度锚定器（饰品栏）
        if (baubles != null) {
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
        }

        // 检查灵魂锚点（背包和饰品栏）
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isSoulAnchor(stack)) return true;
        }
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && isSoulAnchor(stack)) return true;
            }
        }

        return false;
    }

    private static boolean isSoulAnchor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().getRegistryName() != null) {
            return stack.getItem().getRegistryName().toString().equals("moremod:soul_anchor");
        }
        return false;
    }
}