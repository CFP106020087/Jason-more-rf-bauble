package com.moremod.mixin.lycanites;

import com.moremod.item.ItemEnergySword;
import com.moremod.mixinhelper.CapBypassFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2：阿斯摩德仅在能量剑激活/旗标置位时可被玩家伤害（穿过 P2 护盾与距离保护）。
 * 不持剑时保持原版。
 */
@Mixin(targets = "com.lycanitesmobs.core.entity.creature.EntityAsmodeus", remap = false)
public class MixinEntityAsmodeus_EnergySwordPierce {

    /** 兼容两种方法名：1.12 某些分支叫 isEntityInvulnerable */
    @Inject(
            method = {
                    "isInvulnerableTo(Lnet/minecraft/util/DamageSource;)Z",
                    "isEntityInvulnerable(Lnet/minecraft/util/DamageSource;)Z"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void moremod$energySwordBypassInvuln(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (shouldBypass(source)) {
            // 直接判“可受伤”（跳过 isBlocking 的提前 return）
            cir.setReturnValue(false);
        }
    }

    /** 覆盖它的距离/反飞行保护（如果该覆写存在） */
    @Inject(
            method = "isVulnerableTo(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$energySwordAllowPlayer(Entity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (attacker instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) attacker;
            if (isActiveEnergySword(p.getHeldItemMainhand()) || isActiveEnergySword(p.getHeldItemOffhand())
                    || Boolean.TRUE.equals(CapBypassFlag.ASMODEUS_BYPASS.get())) {
                cir.setReturnValue(true);
            }
        }
    }

    /* ---------------- 共用工具 -------------- */

    private static boolean shouldBypass(DamageSource src) {
        if (Boolean.TRUE.equals(CapBypassFlag.ASMODEUS_BYPASS.get())) return true;

        EntityPlayer p = null;
        if (src.getTrueSource() instanceof EntityPlayer) p = (EntityPlayer) src.getTrueSource();
        else if (src.getImmediateSource() instanceof EntityPlayer) p = (EntityPlayer) src.getImmediateSource();
        if (p == null) return false;

        return isActiveEnergySword(p.getHeldItemMainhand()) || isActiveEnergySword(p.getHeldItemOffhand());
    }

    private static boolean isActiveEnergySword(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemEnergySword)) return false;

        IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (es == null || es.getEnergyStored() <= 0) return false;

        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean("CanUnsheathe");
    }
}
