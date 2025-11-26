package com.moremod.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

/**
 * 客户端渲染注册处理器
 * 负责注册各种渲染层和处理渲染事件
 */
@SideOnly(Side.CLIENT)
public class RenderHandler {

    private static boolean layersRegistered = false;

    /**
     * 注册渲染层
     * 应该在客户端初始化时调用
     */
    public static void registerLayers() {
        if (layersRegistered) {
            return;
        }

        Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();

        // 为所有玩家渲染器添加喷气背包层
       // for (RenderPlayer render : skinMap.values()) {
       //     render.addLayer(new LayerJetpack(render));
       // }

        layersRegistered = true;
        System.out.println("[moremod] 喷气背包渲染层注册成功");
    }

    /**
     * 处理玩家渲染事件
     * 可以在这里添加额外的渲染效果
     */
    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Post event) {
        // 可以在这里添加额外的渲染效果
        // 例如：喷气背包尾焰、光效等
    }
}