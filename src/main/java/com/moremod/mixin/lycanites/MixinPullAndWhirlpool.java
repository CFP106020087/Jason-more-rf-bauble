package com.moremod.mixin.lycanites;

import com.lycanitesmobs.core.entity.creature.EntitySpectre;
import com.lycanitesmobs.core.entity.creature.EntityRoa;
import com.lycanitesmobs.core.entity.creature.EntityThresher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityVelocity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

/**
 * 统一处理 Spectre（拉扯）、Roa 和 Thresher（漩涡）的维度锚免疫
 */
@Mixin(value = {EntitySpectre.class, EntityRoa.class, EntityThresher.class}, remap = false)
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

    // === 位移免疫检测（维度锚定器 或 灵魂锚点）===
    @Unique
    private static boolean moremod$hasDimensionalAnchor(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);

        // 检查维度锚定器（饰品栏）
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty()
                        && "ItemDimensionalAnchor".equals(stack.getItem().getClass().getSimpleName())) {
                    return true;
                }
            }
        }

        // 检查灵魂锚点（背包和饰品栏）
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && moremod$isSoulAnchor(stack)) return true;
        }
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && moremod$isSoulAnchor(stack)) return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean moremod$isSoulAnchor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().getRegistryName() != null) {
            return stack.getItem().getRegistryName().toString().equals("moremod:soul_anchor");
        }
        return false;
    }
}