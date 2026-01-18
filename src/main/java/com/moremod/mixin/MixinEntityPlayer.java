package com.moremod.mixin;

import com.moremod.multiblock.MultiblockRespawnChamber;
import com.moremod.tile.TileEntityRespawnChamberCore;
import com.moremod.tile.TileEntityRespawnChamberCore.RespawnChamberLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 使用 ASM/Mixin 注入 EntityPlayer.onDeath 方法
 * 在方法执行前检查重生仓死亡拦截
 *
 * 比 LivingDeathEvent 更可靠，因为直接在方法层面拦截
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    /**
     * 在 onDeath 方法开始时注入
     * 如果满足死亡拦截条件，取消原方法执行
     */
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDeathIntercept(DamageSource cause, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;

        if (player.world.isRemote) return;

        // 检查玩家是否有绑定的重生仓
        RespawnChamberLocation loc = TileEntityRespawnChamberCore.getBoundChamber(player.getUniqueID());
        if (loc == null) {
            return; // 没有绑定重生仓，正常死亡
        }

        // 获取重生仓所在的世界
        World targetWorld = player.getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            LOGGER.warn("[RespawnChamber] Target dimension {} not available for player {}",
                    loc.dimension, player.getName());
            return;
        }

        // 检查区块是否加载
        if (!targetWorld.isBlockLoaded(loc.pos)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生仓所在区块未加载，无法拦截死亡！"
            ));
            return;
        }

        // 获取重生仓 TileEntity
        TileEntity te = targetWorld.getTileEntity(loc.pos);
        if (!(te instanceof TileEntityRespawnChamberCore)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生仓已被破坏！"
            ));
            return;
        }

        TileEntityRespawnChamberCore chamber = (TileEntityRespawnChamberCore) te;

        // 检查是否可以拦截死亡（结构完整 + 电量>=70%）
        if (!chamber.canInterceptDeath()) {
            int percent = chamber.getEnergyPercent();
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生仓电量不足！" +
                            TextFormatting.GRAY + " (当前: " + percent + "%, 需要: 70%)"
            ));
            return;
        }

        // ========== 拦截死亡 ==========
        // 取消原方法执行（这是 ASM 的核心优势）
        ci.cancel();

        // 记录消耗的能量
        int consumedEnergy = chamber.getEnergyStored();

        // 消耗全部能量
        chamber.consumeAllEnergy();

        // 获取传送位置
        BlockPos teleportPos = MultiblockRespawnChamber.getTeleportPosition(loc.pos);

        // 恢复血量（防止传送后立即死亡）
        player.setHealth(player.getMaxHealth() * 0.5f);

        // 清除负面效果
        player.clearActivePotions();

        // 传送玩家
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            if (playerMP.dimension != loc.dimension) {
                // 跨维度传送
                playerMP.changeDimension(loc.dimension, new net.minecraft.world.Teleporter((WorldServer) targetWorld) {
                    @Override
                    public void placeInPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
                        entity.setPositionAndUpdate(
                                teleportPos.getX() + 0.5,
                                teleportPos.getY(),
                                teleportPos.getZ() + 0.5
                        );
                    }
                });
            } else {
                // 同维度传送
                playerMP.setPositionAndUpdate(
                        teleportPos.getX() + 0.5,
                        teleportPos.getY(),
                        teleportPos.getZ() + 0.5
                );
            }
        }

        // 传送特效
        if (targetWorld instanceof WorldServer) {
            WorldServer ws = (WorldServer) targetWorld;

            // 出现位置的粒子效果
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 0.5, teleportPos.getZ() + 0.5,
                    100, 0.5, 1.0, 0.5, 0.2);
            ws.spawnParticle(EnumParticleTypes.END_ROD,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 1.0, teleportPos.getZ() + 0.5,
                    30, 0.3, 0.5, 0.3, 0.1);
            ws.spawnParticle(EnumParticleTypes.HEART,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 1.5, teleportPos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.0);
        }

        // 音效
        targetWorld.playSound(null,
                teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(),
                SoundEvents.ITEM_TOTEM_USE,
                SoundCategory.PLAYERS, 1.0f, 1.0f);

        // 发送成功消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════\n" +
                        TextFormatting.GREEN + "✓ 死亡被重生仓拦截！(ASM)\n" +
                        TextFormatting.GRAY + "消耗能量: " + TextFormatting.YELLOW + formatEnergy(consumedEnergy) + " RF\n" +
                        TextFormatting.GRAY + "传送至: " + TextFormatting.AQUA +
                        String.format("[%d, %d, %d]", teleportPos.getX(), teleportPos.getY(), teleportPos.getZ()) + "\n" +
                        TextFormatting.DARK_GRAY + "重生仓能量已耗尽，请及时补充！\n" +
                        TextFormatting.GREEN + "═══════════════════════════════"
        ));

        LOGGER.info("[RespawnChamber] ASM intercepted death for player {} at [{}, {}, {}], consumed {} RF",
                player.getName(), teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), consumedEnergy);
    }

    /**
     * 格式化能量显示
     */
    private static String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format("%.2fM", energy / 1_000_000.0);
        } else if (energy >= 1_000) {
            return String.format("%.1fK", energy / 1_000.0);
        }
        return String.valueOf(energy);
    }
}
