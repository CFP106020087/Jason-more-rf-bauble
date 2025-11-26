package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.network.play.server.SPacketSoundEffect;

/**
 * 音效助手类
 * 提供在 ZenScript 中播放各种音效的功能
 */
@ZenRegister
@ZenClass("mods.moremod.SoundHelper")
public class SoundHelper {

    /**
     * 播放音效给玩家
     * 
     * @param world 世界
     * @param player 玩家
     * @param soundEvent 音效名称（如 "entity.endermen.teleport"）
     * @param volume 音量 (0.0 - 1.0)
     * @param pitch 音调 (0.5 - 2.0，1.0 为正常)
     */
    @ZenMethod
    public static void playSound(IWorld world, IPlayer player, 
                                 String soundEvent, float volume, float pitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        SoundEvent sound = getSoundEvent(soundEvent);
        
        if (sound != null) {
            mcWorld.playSound(null, 
                mcPlayer.posX, mcPlayer.posY, mcPlayer.posZ,
                sound, SoundCategory.PLAYERS, volume, pitch);
        }
    }
    
    /**
     * 在指定位置播放音效
     * 
     * @param world 世界
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param soundEvent 音效名称
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playSoundAt(IWorld world, 
                                   double x, double y, double z,
                                   String soundEvent, float volume, float pitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        SoundEvent sound = getSoundEvent(soundEvent);
        if (sound != null) {
            mcWorld.playSound(null, x, y, z, sound, 
                SoundCategory.PLAYERS, volume, pitch);
        }
    }
    
    /**
     * 播放音效给附近所有玩家
     * 
     * @param world 世界
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param range 范围（方块）
     * @param soundEvent 音效名称
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playSoundToNearby(IWorld world,
                                        double x, double y, double z,
                                        double range,
                                        String soundEvent, float volume, float pitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        SoundEvent sound = getSoundEvent(soundEvent);
        if (sound == null) return;
        
        for (EntityPlayer player : mcWorld.playerEntities) {
            double distance = player.getDistance(x, y, z);
            if (distance <= range) {
                if (player instanceof EntityPlayerMP) {
                    EntityPlayerMP playerMP = (EntityPlayerMP) player;
                    playerMP.connection.sendPacket(
                        new SPacketSoundEffect(sound, SoundCategory.PLAYERS,
                            x, y, z, volume, pitch)
                    );
                }
            }
        }
    }
    
    /**
     * 播放自定义模组音效
     * 
     * @param world 世界
     * @param player 玩家
     * @param modId 模组 ID
     * @param soundName 音效名称
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playCustomSound(IWorld world, IPlayer player,
                                      String modId, String soundName,
                                      float volume, float pitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        ResourceLocation soundLocation = new ResourceLocation(modId, soundName);
        SoundEvent sound = SoundEvent.REGISTRY.getObject(soundLocation);
        
        if (sound != null) {
            mcWorld.playSound(null,
                mcPlayer.posX, mcPlayer.posY, mcPlayer.posZ,
                sound, SoundCategory.PLAYERS, volume, pitch);
        }
    }
    
    /**
     * 播放音效序列（依次播放多个音效）
     * 
     * @param world 世界
     * @param player 玩家
     * @param soundEvents 音效名称数组
     * @param volume 音量
     * @param pitch 音调
     * @param delayTicks 每个音效之间的延迟（tick）
     */
    @ZenMethod
    public static void playSoundSequence(IWorld world, IPlayer player,
                                        String[] soundEvents,
                                        float volume, float pitch,
                                        int delayTicks) {
        // 注意：这需要在服务器端使用调度器
        // 这里提供简化实现，只播放第一个音效
        if (soundEvents.length > 0) {
            playSound(world, player, soundEvents[0], volume, pitch);
        }
        // 完整实现需要使用 Minecraft 的调度系统
    }
    
    /**
     * 播放随机音效（从列表中随机选择一个）
     * 
     * @param world 世界
     * @param player 玩家
     * @param soundEvents 音效名称数组
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playRandomSound(IWorld world, IPlayer player,
                                      String[] soundEvents,
                                      float volume, float pitch) {
        if (soundEvents.length == 0) return;
        
        World mcWorld = CraftTweakerMC.getWorld(world);
        int index = mcWorld.rand.nextInt(soundEvents.length);
        playSound(world, player, soundEvents[index], volume, pitch);
    }
    
    /**
     * 播放随机音调的音效
     * 
     * @param world 世界
     * @param player 玩家
     * @param soundEvent 音效名称
     * @param volume 音量
     * @param minPitch 最小音调
     * @param maxPitch 最大音调
     */
    @ZenMethod
    public static void playSoundRandomPitch(IWorld world, IPlayer player,
                                           String soundEvent, float volume,
                                           float minPitch, float maxPitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        float pitch = minPitch + mcWorld.rand.nextFloat() * (maxPitch - minPitch);
        playSound(world, player, soundEvent, volume, pitch);
    }
    
    /**
     * 播放音效给玩家（指定音效类别）
     * 
     * @param world 世界
     * @param player 玩家
     * @param soundEvent 音效名称
     * @param category 音效类别（"master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"）
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playSoundWithCategory(IWorld world, IPlayer player,
                                            String soundEvent, String category,
                                            float volume, float pitch) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        SoundEvent sound = getSoundEvent(soundEvent);
        SoundCategory soundCategory = getSoundCategory(category);
        
        if (sound != null && soundCategory != null) {
            mcWorld.playSound(null,
                mcPlayer.posX, mcPlayer.posY, mcPlayer.posZ,
                sound, soundCategory, volume, pitch);
        }
    }
    
    /**
     * 停止播放音效给玩家
     * 注意：这个方法在原版 Minecraft 中无法直接实现
     * 作为占位符保留
     * 
     * @param player 玩家
     * @param soundEvent 音效名称
     */
    @ZenMethod
    public static void stopSound(IPlayer player, String soundEvent) {
        // Minecraft 原版不支持直接停止音效
        // 需要通过客户端处理或使用特殊的音效系统
        // 这里作为 API 占位符
    }
    
    /**
     * 获取 SoundEvent 对象
     */
    private static SoundEvent getSoundEvent(String name) {
        ResourceLocation location = new ResourceLocation(name);
        return SoundEvent.REGISTRY.getObject(location);
    }
    
    /**
     * 获取 SoundCategory 对象
     */
    private static SoundCategory getSoundCategory(String name) {
        try {
            return SoundCategory.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return SoundCategory.PLAYERS; // 默认类别
        }
    }
    
    // ============================================
    // 便捷方法 - 常用音效
    // ============================================
    
    /**
     * 播放经验获得音效
     */
    @ZenMethod
    public static void playExperienceSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.experience_orb.pickup", 1.0f, 1.0f);
    }
    
    /**
     * 播放升级音效
     */
    @ZenMethod
    public static void playLevelUpSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.player.levelup", 1.0f, 1.0f);
    }
    
    /**
     * 播放成功音效
     */
    @ZenMethod
    public static void playSuccessSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.player.levelup", 1.0f, 1.5f);
    }
    
    /**
     * 播放失败音效
     */
    @ZenMethod
    public static void playFailSound(IWorld world, IPlayer player) {
        playSound(world, player, "block.anvil.land", 0.5f, 0.5f);
    }
    
    /**
     * 播放击中音效
     */
    @ZenMethod
    public static void playHitSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.player.attack.strong", 1.0f, 1.0f);
    }
    
    /**
     * 播放暴击音效
     */
    @ZenMethod
    public static void playCritSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.player.attack.crit", 1.0f, 1.0f);
    }
    
    /**
     * 播放扫击音效
     */
    @ZenMethod
    public static void playSweepSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.player.attack.sweep", 1.0f, 1.0f);
    }
    
    /**
     * 播放传送音效
     */
    @ZenMethod
    public static void playTeleportSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.endermen.teleport", 1.0f, 1.0f);
    }
    
    /**
     * 播放魔法音效
     */
    @ZenMethod
    public static void playMagicSound(IWorld world, IPlayer player) {
        playSound(world, player, "entity.zombie_villager.cure", 1.0f, 1.5f);
    }
    
    /**
     * 播放爆炸音效
     */
    @ZenMethod
    public static void playExplosionSound(IWorld world, double x, double y, double z) {
        playSoundAt(world, x, y, z, "entity.generic.explode", 4.0f, 1.0f);
    }
}
