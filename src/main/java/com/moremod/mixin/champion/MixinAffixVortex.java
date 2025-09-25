package com.moremod.mixin.champion;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(
        targets = "c4.champions.common.affix.affix.AffixVortex",
        remap = false
)
public class MixinAffixVortex {

    private EntityLiving currentEntity;

    @ModifyArg(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lc4/champions/common/affix/affix/AffixVortex;isValidAffixTarget(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/entity/EntityLivingBase;Z)Z"
            ),
            index = 1
    )
    private EntityLivingBase checkAnchor(EntityLivingBase target) {
        if (target instanceof EntityPlayer && hasDimensionalAnchor((EntityPlayer)target)) {
            return null; // 返回null让isValidAffixTarget返回false
        }
        return target;
    }

    private static boolean hasDimensionalAnchor(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack != null && !stack.isEmpty()) {
                String cls = stack.getItem().getClass().getName();
                if (cls.contains("ItemDimensionalAnchor")) return true;
            }
        }
        return false;
    }
}