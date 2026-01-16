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
                    target = "Lc4/champions/common.affix/affix/AffixVortex;isValidAffixTarget(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/entity/EntityLivingBase;Z)Z"
            ),
            index = 1
    )
    private EntityLivingBase checkAnchor(EntityLivingBase target) {
        if (target instanceof EntityPlayer && hasDisplacementImmunity((EntityPlayer)target)) {
            return null; // 返回null让isValidAffixTarget返回false
        }
        return target;
    }

    /**
     * 检查玩家是否有位移免疫（维度锚定器 或 灵魂锚点）
     */
    private static boolean hasDisplacementImmunity(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) {
                    String cls = stack.getItem().getClass().getName();
                    if (cls.contains("ItemDimensionalAnchor")) return true;
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