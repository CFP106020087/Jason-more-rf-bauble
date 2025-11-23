package com.moremod.system.visual;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.RejectionSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 排异视觉管理器
 * 负责心跳音效和暗角渲染
 * 从机械核心NBT读取排异数据
 */
@Mod.EventBusSubscriber(
        modid = "moremod",
        value = Side.CLIENT
)
@SideOnly(Side.CLIENT)
public class RejectionVisualManager {

    private static int tickCounter = 0;
    private static boolean initialized = false;
    private static final boolean DEBUG = false;

    /**
     * 初始化（客户端启动时调用一次）
     */
    private static void init() {
        if (initialized) return;
        
        System.out.println("==========================================");
        System.out.println("[排异视觉] 🎯 系统初始化");
        System.out.println("[排异视觉] Mod ID: moremod");
        System.out.println("[排异视觉] Side: CLIENT");
        System.out.println("[排异视觉] 配置:");
        System.out.println("[排异视觉]   - 心跳阈值: " + VisualConfig.HEARTBEAT_START + "%");
        System.out.println("[排异视觉]   - 暗角阈值: " + VisualConfig.VIGNETTE_START + "%");
        System.out.println("[排异视觉]   - 心跳间隔: " + VisualConfig.HEARTBEAT_MIN_INTERVAL + "-" + VisualConfig.HEARTBEAT_MAX_INTERVAL + "ms");
        System.out.println("==========================================");
        
        // 初始化音效系统
        RejectionSoundManager.init();
        
        initialized = true;
    }

    /**
     * 客户端直接从机械核心NBT读取排异值
     */
    private static float getRejectionClient(EntityPlayerSP player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey("rejection")) return 0;
        
        NBTTagCompound group = nbt.getCompoundTag("rejection");
        return group.getFloat("RejectionLevel");
    }

    /**
     * 客户端直接从机械核心NBT读取适应度
     */
    private static float getAdaptationClient(EntityPlayerSP player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey("rejection")) return 0;
        
        NBTTagCompound group = nbt.getCompoundTag("rejection");
        return group.getFloat("AdaptationLevel");
    }

    /**
     * 每 tick 客户端调用：用于心跳音效
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        
        if (player == null) {
            // 玩家未进入游戏
            return;
        }

        // 首次有玩家时初始化
        if (!initialized) {
            init();
        }

        // 从机械核心读取排异值
        float rej = getRejectionClient(player);
        
        // 没有核心或排异为0，不处理
        if (rej <= 0) {
            return;
        }

        // 调试输出（每秒一次）
        if (DEBUG && rej >= 40 && ++tickCounter >= 20) {
            System.out.println("==========================================");
            System.out.println("[排异视觉] 📊 状态报告");
            System.out.println("[排异视觉] 当前排异值: " + String.format("%.2f", rej) + "%");
            System.out.println("[排异视觉] 适应度: " + String.format("%.2f", getAdaptationClient(player)) + "%");
            System.out.println("[排异视觉] 玩家: " + player.getName());
            
            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
            System.out.println("[排异视觉] 核心存在: " + !core.isEmpty());
            
            if (!core.isEmpty() && core.hasTagCompound()) {
                NBTTagCompound rejGroup = core.getTagCompound().getCompoundTag("rejection");
                System.out.println("[排异视觉] NBT数据: " + rejGroup.toString());
            }
            
            System.out.println("[排异视觉] ----------");
            
            if (rej >= VisualConfig.HEARTBEAT_START) {
                System.out.println("[排异视觉] ✅ 应该播放心跳（排异 >= " + VisualConfig.HEARTBEAT_START + "%）");
            } else {
                System.out.println("[排异视觉] ❌ 不播放心跳（排异 < " + VisualConfig.HEARTBEAT_START + "%）");
            }
            
            if (rej >= VisualConfig.VIGNETTE_START) {
                float alpha = Math.min((rej - VisualConfig.VIGNETTE_START) / (100f - VisualConfig.VIGNETTE_START), 1f);
                System.out.println("[排异视觉] ✅ 应该显示暗角（排异 >= " + VisualConfig.VIGNETTE_START + "%）Alpha: " + String.format("%.2f", alpha));
            } else {
                System.out.println("[排异视觉] ❌ 不显示暗角（排异 < " + VisualConfig.VIGNETTE_START + "%）");
            }
            
            System.out.println("==========================================");
            tickCounter = 0;
        }

        // 更新心跳效果
        HeartbeatEffect.update(rej, player);
    }

    /**
     * 渲染覆盖层：用于暗角效果
     */
    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        // 只在ALL类型时渲染，避免重复绘制
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        // 从机械核心读取排异值
        float rej = getRejectionClient(mc.player);
        
        // 没有排异值，不渲染
        if (rej <= 0) return;

        if (DEBUG) {
            System.out.println("[暗角渲染] 排异值: " + rej + "%");
        }

        // 渲染暗角效果
        BloodVignetteEffect.render(rej, event);
    }
    
    /**
     * 手动刷新视觉效果（用于调试）
     */
    public static void refreshVisuals() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        
        float rejection = getRejectionClient(mc.player);
        float adaptation = getAdaptationClient(mc.player);
        
        System.out.println("[排异视觉] 手动刷新:");
        System.out.println("  - 排异: " + String.format("%.1f", rejection) + "%");
        System.out.println("  - 适应: " + String.format("%.1f", adaptation) + "%");
        
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(mc.player);
        if (!core.isEmpty()) {
            System.out.println("  - 核心: 已装备");
            if (core.hasTagCompound() && core.getTagCompound().hasKey("rejection")) {
                NBTTagCompound group = core.getTagCompound().getCompoundTag("rejection");
                System.out.println("  - NBT: " + group.toString());
            }
        } else {
            System.out.println("  - 核心: 未装备");
        }
    }
}
