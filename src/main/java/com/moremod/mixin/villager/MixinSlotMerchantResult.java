package com.moremod.mixin.villager;

import com.moremod.item.MerchantPersuader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.SlotMerchantResult;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlotMerchantResult.class)
public class MixinSlotMerchantResult {

    /**
     * 在交易完成时处理说服器效果
     * onTake -> func_190901_a
     */
    @Inject(
            method = "func_190901_a",
            at = @At("HEAD")
    )
    private void onTradeTaken(EntityPlayer player, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (player != null && !player.world.isRemote) {
            ItemStack persuader = MerchantPersuader.getActivePersuader(player);

            if (!persuader.isEmpty() && persuader.getItem() instanceof MerchantPersuader) {
                System.out.println("[MerchantPersuader] Trade completed with persuader active");
            }
        }
    }
}