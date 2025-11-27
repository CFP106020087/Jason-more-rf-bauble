package com.moremod.entity.boss.riftwarden;

import com.moremod.util.BossBlockTracker;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 环境控制器 - 处理Boss对环境的影响
 * 
 * 职责：
 * 1. 禁止玩家飞行
 * 2. 破坏玩家放置的方块
 * 3. 场地效果
 */
public class RiftwardenEnvironmentController {
    
    private final EntityRiftwarden boss;
    
    // 被禁飞的玩家
    private final Set<Integer> disabledFlyingPlayers = new HashSet<>();
    
    // 计时器
    private int flightCheckTimer = 0;
    private int blockBreakTimer = 0;
    
    // 配置
    private static final int FLIGHT_CHECK_INTERVAL = 10;
    private static final int BLOCK_BREAK_INTERVAL = 10;
    private static final int FLIGHT_DISABLE_RANGE = 50;
    private static final int BLOCK_BREAK_RANGE = 3;
    
    public RiftwardenEnvironmentController(EntityRiftwarden boss) {
        this.boss = boss;
    }
    
    /**
     * 每tick更新
     */
    public void tick() {
        if (boss.world.isRemote) return;
        
        // 禁飞检查
        flightCheckTimer++;
        if (flightCheckTimer >= FLIGHT_CHECK_INTERVAL) {
            flightCheckTimer = 0;
            checkAndDisableFlight();
        }
        
        // 方块破坏
        blockBreakTimer++;
        if (blockBreakTimer >= BLOCK_BREAK_INTERVAL) {
            blockBreakTimer = 0;
            breakNearbyPlayerBlocks();
        }
    }
    
    /**
     * 检查并禁用附近玩家的飞行
     */
    private void checkAndDisableFlight() {
        List<EntityPlayer> players = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class,
            new AxisAlignedBB(
                boss.posX - FLIGHT_DISABLE_RANGE,
                boss.posY - FLIGHT_DISABLE_RANGE,
                boss.posZ - FLIGHT_DISABLE_RANGE,
                boss.posX + FLIGHT_DISABLE_RANGE,
                boss.posY + FLIGHT_DISABLE_RANGE,
                boss.posZ + FLIGHT_DISABLE_RANGE
            )
        );
        
        for (EntityPlayer player : players) {
            if (player.capabilities.isFlying && !player.isCreative() && !player.isSpectator()) {
                player.capabilities.isFlying = false;
                disabledFlyingPlayers.add(player.getEntityId());
                
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendPlayerAbilities();
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§c虚空守望者的力量禁锢了你的飞行能力！"));
                }
                
                // 效果
                if (boss.world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) boss.world;
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        player.posX, player.posY, player.posZ,
                        30, 0.5, 1.0, 0.5, 0.1);
                }
                player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
            }
        }
    }
    
    /**
     * 破坏Boss身体碰撞范围内的玩家方块
     */
    private void breakNearbyPlayerBlocks() {
        BlockPos center = boss.getPosition();
        
        // Boss身体范围内的方块
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= (int) Math.ceil(boss.height); y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (BossBlockTracker.isBossPlayerBlock(boss, pos)) {
                        destroyBlock(pos);
                    }
                }
            }
        }
        
        // 周围区域的方块（概率破坏）
        List<BlockPos> nearbyBlocks = BossBlockTracker.getBossPlayerBlocks(boss, BLOCK_BREAK_RANGE);
        for (BlockPos pos : nearbyBlocks) {
            if (boss.getRNG().nextFloat() < 0.3F) {  // 30%概率
                destroyBlock(pos);
            }
        }
    }
    
    /**
     * 破坏方块并播放效果
     */
    private void destroyBlock(BlockPos pos) {
        if (boss.world.isAirBlock(pos)) return;
        
        IBlockState state = boss.world.getBlockState(pos);
        boss.world.destroyBlock(pos, false);
        BossBlockTracker.removeBlock(boss, pos);
        
        // 粒子效果
        if (boss.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) boss.world;
            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.25, 0.25, 0.25, 0.05,
                Block.getStateId(state));
        }
    }
    
    /**
     * 强制破坏区域内所有玩家方块
     */
    public void forceBreakAllBlocks(int range) {
        List<BlockPos> blocks = BossBlockTracker.getBossPlayerBlocks(boss, range);
        
        int destroyed = 0;
        for (BlockPos pos : blocks) {
            destroyBlock(pos);
            destroyed++;
        }
        
        if (destroyed > 0) {
            boss.playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.7F, 1.0F);
        }
    }
    
    /**
     * 恢复所有玩家的飞行能力（Boss死亡时调用）
     */
    public void restoreAllFlight() {
        if (boss.world.isRemote) return;
        
        List<EntityPlayer> players = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class,
            boss.getEntityBoundingBox().grow(100)
        );
        
        for (EntityPlayer player : players) {
            if (disabledFlyingPlayers.contains(player.getEntityId())) {
                if (player instanceof EntityPlayerMP) {
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§a虚空守望者已被击败，飞行能力已恢复"));
                    ((EntityPlayerMP) player).sendPlayerAbilities();
                }
            }
        }
        
        disabledFlyingPlayers.clear();
    }
    
    /**
     * 获取被禁飞的玩家ID集合
     */
    public Set<Integer> getDisabledFlyingPlayers() {
        return disabledFlyingPlayers;
    }
    
    /**
     * 清理数据
     */
    public void cleanup() {
        restoreAllFlight();
    }
}