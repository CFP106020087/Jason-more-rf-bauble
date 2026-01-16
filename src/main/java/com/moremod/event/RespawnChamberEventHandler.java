package com.moremod.event;

import com.moremod.moremod;
import com.moremod.multiblock.MultiblockRespawnChamber;
import com.moremod.tile.TileEntityRespawnChamberCore;
import com.moremod.tile.TileEntityRespawnChamberCore.RespawnChamberLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 重生倉死亡拦截事件處理器
 *
 * 功能：
 * - 當玩家死亡時，檢查是否有綁定的重生倉
 * - 如果重生倉在加載區塊且電量>=70%，拦截死亡
 * - 消耗全部能量，將玩家傳送回重生倉
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class RespawnChamberEventHandler {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    /**
     * 死亡拦截處理
     * 優先級 HIGHEST：在其他死亡處理之前執行
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 檢查玩家是否有綁定的重生倉
        RespawnChamberLocation loc = TileEntityRespawnChamberCore.getBoundChamber(player.getUniqueID());
        if (loc == null) {
            return; // 沒有綁定重生倉
        }

        // 獲取重生倉所在的世界
        World targetWorld = player.getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            LOGGER.warn("[RespawnChamber] Target dimension {} not available for player {}",
                    loc.dimension, player.getName());
            return;
        }

        // 檢查區塊是否加載
        if (!targetWorld.isBlockLoaded(loc.pos)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉所在區塊未加載，無法拦截死亡！"
            ));
            return;
        }

        // 獲取重生倉 TileEntity
        TileEntity te = targetWorld.getTileEntity(loc.pos);
        if (!(te instanceof TileEntityRespawnChamberCore)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉已被破壞！"
            ));
            return;
        }

        TileEntityRespawnChamberCore chamber = (TileEntityRespawnChamberCore) te;

        // 檢查是否可以拦截死亡（結構完整 + 電量>=70%）
        if (!chamber.canInterceptDeath()) {
            int percent = chamber.getEnergyPercent();
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉電量不足！" +
                            TextFormatting.GRAY + " (當前: " + percent + "%, 需要: 70%)"
            ));
            return;
        }

        // ========== 拦截死亡 ==========
        event.setCanceled(true);

        // 記錄消耗的能量
        int consumedEnergy = chamber.getEnergyStored();

        // 消耗全部能量
        chamber.consumeAllEnergy();

        // 獲取傳送位置
        BlockPos teleportPos = MultiblockRespawnChamber.getTeleportPosition(loc.pos);

        // 傳送玩家
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // 先恢復一些血量（防止傳送後立即死亡）
            player.setHealth(player.getMaxHealth() * 0.5f);

            if (playerMP.dimension != loc.dimension) {
                // 跨維度傳送
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
                // 同維度傳送
                playerMP.setPositionAndUpdate(
                        teleportPos.getX() + 0.5,
                        teleportPos.getY(),
                        teleportPos.getZ() + 0.5
                );
            }
        }

        // 傳送特效
        if (targetWorld instanceof WorldServer) {
            WorldServer ws = (WorldServer) targetWorld;

            // 出現位置的粒子效果
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
                net.minecraft.init.SoundEvents.ITEM_TOTEM_USE,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        // 發送成功消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════\n" +
                        TextFormatting.GREEN + "✓ 死亡被重生倉拦截！\n" +
                        TextFormatting.GRAY + "消耗能量: " + TextFormatting.YELLOW + formatEnergy(consumedEnergy) + " RF\n" +
                        TextFormatting.GRAY + "傳送至: " + TextFormatting.AQUA +
                        String.format("[%d, %d, %d]", teleportPos.getX(), teleportPos.getY(), teleportPos.getZ()) + "\n" +
                        TextFormatting.DARK_GRAY + "重生倉能量已耗盡，請及時補充！\n" +
                        TextFormatting.GREEN + "═══════════════════════════════"
        ));

        LOGGER.info("[RespawnChamber] Intercepted death for player {} at [{}, {}, {}], consumed {} RF",
                player.getName(), teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), consumedEnergy);
    }

    /**
     * 格式化能量顯示
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
