package com.moremod.mixin.lycanites;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.lycanitesmobs.core.entity.BaseCreatureEntity", remap = false)
public class BaseCreatureEntityMixin {

    @Shadow
    public float damageLimit;

    @Shadow
    public int damageMax;

    @Shadow
    public float damageTakenThisSec;  // Add this shadow field

    // Method 1: Bypass invulnerability check
    @Inject(method = "func_180431_b", at = @At("HEAD"), cancellable = true)
    private void bypassInvulnerability(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            ItemStack heldItem = player.getHeldItemMainhand();

            if (!heldItem.isEmpty() &&
                    heldItem.getItem().getRegistryName() != null &&
                    heldItem.getItem().getRegistryName().toString().equals("moremod:energy_sword")) {
                cir.setReturnValue(false); // Not invulnerable to energy sword
            }
        }
    }

    // Method 2: Bypass damage limit
    @Inject(method = "func_70097_a", at = @At("HEAD"))
    private void bypassDamageLimit(DamageSource damageSrc, float damageAmount, CallbackInfoReturnable<Boolean> cir) {
        if (damageSrc.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
            ItemStack heldItem = player.getHeldItemMainhand();

            if (!heldItem.isEmpty() &&
                    heldItem.getItem().getRegistryName() != null &&
                    heldItem.getItem().getRegistryName().toString().equals("moremod:energy_sword")) {
                // Reset damage limits when attacked with energy sword
                this.damageLimit = 0;
                this.damageMax = 0;
            }
        }
    }

    // Method 3: Bypass boss range check
    @Inject(method = "isDamageEntityApplicable", at = @At("HEAD"), cancellable = true)
    private void bypassBossRangeCheck(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            ItemStack heldItem = player.getHeldItemMainhand();

            if (!heldItem.isEmpty() &&
                    heldItem.getItem().getRegistryName() != null &&
                    heldItem.getItem().getRegistryName().toString().equals("moremod:energy_sword")) {
                cir.setReturnValue(true); // Always applicable for energy sword
            }
        }
    }

    // Method 4: Reset damage tracking on damage
    @Inject(method = "onDamage", at = @At("HEAD"))
    private void onDamageModify(DamageSource damageSrc, float damage, CallbackInfo ci) {
        if (damageSrc.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
            ItemStack heldItem = player.getHeldItemMainhand();

            if (!heldItem.isEmpty() &&
                    heldItem.getItem().getRegistryName() != null &&
                    heldItem.getItem().getRegistryName().toString().equals("moremod:energy_sword")) {
                // Reset the damage taken this second to bypass the limit
                this.damageTakenThisSec = 0;
            }
        }
    }
}