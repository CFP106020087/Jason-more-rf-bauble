// HeartbeatEffect.java - 完整替换版本
package com.moremod.system.visual;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.RejectionSoundManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HeartbeatEffect {

    private static long lastPlay = 0L;
    private static boolean soundChecked = false;
    private static boolean soundAvailable = false;
    
    // 调整后的参数
    private static final int BASE_INTERVAL = 3000;     // 3秒基础间隔（之前太频繁）
    private static final int MIN_INTERVAL = 1000;      // 最快1秒
    private static final float HEARTBEAT_THRESHOLD = 50f; // 50%才开始心跳
    private static final float VOLUME_BASE = 0.25f;    // 更低的基础音量
    private static final float VOLUME_MAX = 0.6f;      // 最大音量也降低
    
    private static final boolean DEBUG = false;

    private static float getRejectionFromCore(EntityPlayerSP player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey("rejection")) return 0;
        
        NBTTagCompound group = nbt.getCompoundTag("rejection");
        return group.getFloat("RejectionLevel");
    }

    public static void update(float rejection, EntityPlayerSP player) {
        if (rejection <= 0) {
            rejection = getRejectionFromCore(player);
            if (rejection <= 0) return;
        }
        
        // 一次性检查音效是否可用
        if (!soundChecked) {
            soundAvailable = RejectionSoundManager.hasSoundEvent("heartbeat");
            soundChecked = true;

            if (soundAvailable) {
                System.out.println("[心跳音效] ✅ heartbeat 音效已找到");
            } else {
                System.err.println("[心跳音效] ❌ heartbeat 音效未找到！请检查 sounds.json");
            }
        }

        if (!soundAvailable) return;

        // 提高阈值到50%
        if (rejection < HEARTBEAT_THRESHOLD) {
            if (DEBUG && rejection > 0) {
                System.out.println("[心跳音效] 排异 " + rejection + "% < 阈值 " + HEARTBEAT_THRESHOLD + "%");
            }
            return;
        }

        long now = System.currentTimeMillis();

        // 使用平方函数让心跳加速更平滑
        float t = Math.min((rejection - HEARTBEAT_THRESHOLD) / (100f - HEARTBEAT_THRESHOLD), 1f);
        t = t * t; // 平方让低排异时心跳更慢

        // 插值计算当前心跳间隔
        int interval = (int) (BASE_INTERVAL - t * (BASE_INTERVAL - MIN_INTERVAL));

        if (now - lastPlay < interval) return;

        // 音量和音高都更温和
        float volume = VOLUME_BASE + t * (VOLUME_MAX - VOLUME_BASE);
        float pitch = 1.0f + t * 0.15f; // 音高变化更小

        SoundEvent heartbeat = RejectionSoundManager.getSound("heartbeat");
        if (heartbeat != null) {
            player.playSound(heartbeat, volume, pitch);
            lastPlay = now;

            if (DEBUG || rejection >= 85) {
                System.out.println("[心跳音效] ♥ 排异: " + String.format("%.1f", rejection) +
                        "% | 音量: " + String.format("%.2f", volume) +
                        " | 音高: " + String.format("%.2f", pitch) +
                        " | 间隔: " + interval + "ms");
            }
        }
    }

    public static void reset() {
        soundChecked = false;
        soundAvailable = false;
        lastPlay = 0L;
    }
    
    public static void forcePlayOnce(EntityPlayerSP player) {
        SoundEvent heartbeat = RejectionSoundManager.getSound("heartbeat");
        if (heartbeat != null) {
            player.playSound(heartbeat, 0.5f, 1.0f);
            System.out.println("[心跳音效] 测试播放");
        } else {
            System.err.println("[心跳音效] 无法播放：音效不存在");
        }
    }
}