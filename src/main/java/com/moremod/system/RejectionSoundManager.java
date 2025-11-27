package com.moremod.system;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 排异系统音效管理器
 * 参考 Lycanites Mobs 的 AssetManager 模式
 */
public class RejectionSoundManager {

    private static final String MODID = "moremod";

    /**
     * 动态获取音效，如果不存在返回 null
     * 参考 Lycanites Mobs 的 AssetManager.getSound() 方法
     */
    public static SoundEvent getSound(String soundName) {
        ResourceLocation location = new ResourceLocation(MODID, soundName);
        
        // 从注册表中查找音效
        if (ForgeRegistries.SOUND_EVENTS.containsKey(location)) {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(location);
            return sound;
        }
        
        // 音效不存在
        return null;
    }

    /**
     * 检查音效是否存在
     */
    public static boolean hasSoundEvent(String soundName) {
        ResourceLocation location = new ResourceLocation(MODID, soundName);
        return ForgeRegistries.SOUND_EVENTS.containsKey(location);
    }

    /**
     * 列出所有已注册的音效（调试用）
     */
    public static void listAllSounds() {
        System.out.println("==========================================");
        System.out.println("[音效管理] " + MODID + " 已注册的音效:");
        
        int count = 0;
        for (ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            if (key.getNamespace().equals(MODID)) {
                System.out.println("  - " + key);
                count++;
            }
        }
        
        if (count == 0) {
            System.out.println("  ⚠️  没有找到任何已注册的音效！");
            System.out.println("  请检查:");
            System.out.println("  1. sounds.json 是否存在于 assets/moremod/");
            System.out.println("  2. 音效文件是否存在于 assets/moremod/sounds/");
            System.out.println("  3. RejectionSoundEvents 是否正确注册");
        } else {
            System.out.println("  ✅ 共找到 " + count + " 个音效");
        }
        
        System.out.println("==========================================");
    }

    /**
     * 初始化音效系统（在游戏启动时调用）
     */
    public static void init() {
        System.out.println("[音效管理] 初始化排异系统音效...");
        
        // 检查关键音效是否存在
        boolean heartbeatExists = hasSoundEvent("heartbeat");
        
        System.out.println("[音效管理] heartbeat 音效: " + (heartbeatExists ? "✅ 已注册" : "❌ 未找到"));
        
        if (!heartbeatExists) {
            System.err.println("==========================================");
            System.err.println("❌ 警告: heartbeat 音效未注册！");
            System.err.println("心跳音效将无法播放！");
            System.err.println("");
            System.err.println("请确保以下文件存在:");
            System.err.println("1. src/main/resources/assets/moremod/sounds.json");
            System.err.println("2. src/main/resources/assets/moremod/sounds/heartbeat.ogg");
            System.err.println("");
            System.err.println("sounds.json 内容应该包含:");
            System.err.println("{");
            System.err.println("  \"heartbeat\": {");
            System.err.println("    \"category\": \"master\",");
            System.err.println("    \"sounds\": [\"moremod:heartbeat\"]");
            System.err.println("  }");
            System.err.println("}");
            System.err.println("==========================================");
        }
        
        // 列出所有音效（调试）
        listAllSounds();
    }
}