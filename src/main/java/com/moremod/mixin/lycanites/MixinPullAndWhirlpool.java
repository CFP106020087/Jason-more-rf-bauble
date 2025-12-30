package com.moremod.mixin.lycanites;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityVelocity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

/**
 * 统一处理 Spectre（拉扯）、Roa 和 Thresher（漩涡）的维度锚免疫
 */
@Pseudo
@Mixin(targets = {
        "com.lycanitesmobs.core.entity.creature.EntitySpectre",
        "com.lycanitesmobs.core.entity.creature.EntityRoa",
        "com.lycanitesmobs.core.entity.creature.EntityThresher"
}, remap = false)
public abstract class MixinPullAndWhirlpool {

    // === 拦截 EntityLivingBase.addVelocity（Spectre 用）：dev ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;addVelocity(DDD)V"),
            require = 0
    )
    private void moremod$blockPull_dev(EntityLivingBase target, double x, double y, double z) {
        if (target instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) target)) {
            return;
        }
        target.addVelocity(x, y, z);
    }

    // === 拦截 EntityLivingBase.addVelocity（Spectre 用）：obf ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;func_70024_g(DDD)V"),
            require = 0
    )
    private void moremod$blockPull_obf(EntityLivingBase target, double x, double y, double z) {
        if (target instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) target)) {
            return;
        }
        target.addVelocity(x, y, z);
    }

    // === 拦截 Entity.addVelocity（Roa/Thresher 用）：dev ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"),
            require = 0
    )
    private void moremod$blockWhirlpool_dev(Entity target, double x, double y, double z) {
        if (target instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) target)) {
            return;
        }
        target.addVelocity(x, y, z);
    }

    // === 拦截 Entity.addVelocity（Roa/Thresher 用）：obf ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;func_70024_g(DDD)V"),
            require = 0
    )
    private void moremod$blockWhirlpool_obf(Entity target, double x, double y, double z) {
        if (target instanceof EntityPlayer && moremod$hasDimensionalAnchor((EntityPlayer) target)) {
            return;
        }
        target.addVelocity(x, y, z);
    }

    // === 抑制速度包：dev ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/NetHandlerPlayServer;sendPacket(Lnet/minecraft/network/Packet;)V"),
            require = 0
    )
    private void moremod$suppressVelocityPacket_dev(NetHandlerPlayServer connection, Packet<?> packet) {
        if (packet instanceof SPacketEntityVelocity
                && connection.player != null
                && moremod$hasDimensionalAnchor(connection.player)) {
            return;
        }
        connection.sendPacket(packet);
    }

    // === 抑制速度包：obf ===
    @Redirect(
            method = {"onLivingUpdate", "func_70636_d"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/NetHandlerPlayServer;func_147359_a(Lnet/minecraft/network/Packet;)V"),
            require = 0
    )
    private void moremod$suppressVelocityPacket_obf(NetHandlerPlayServer connection, Packet<?> packet) {
        if (packet instanceof SPacketEntityVelocity
                && connection.player != null
                && moremod$hasDimensionalAnchor(connection.player)) {
            return;
        }
        connection.sendPacket(packet);
    }

    // === 精简的维度锚检测（只查 Baubles 槽）===
    @Unique
    private static boolean moremod$hasDimensionalAnchor(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty()
                    && "ItemDimensionalAnchor".equals(stack.getItem().getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}