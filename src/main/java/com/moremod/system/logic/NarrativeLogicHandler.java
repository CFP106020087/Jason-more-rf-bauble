package com.moremod.system.logic;

import com.moremod.client.gui.PlayerNarrativeState;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.player.EntityPlayer;

public class NarrativeLogicHandler {

    // 低人性閾值
    private static final float LOW_HUMANITY_THRESHOLD = 20.0f;

    /**
     * 👁️ 判斷當前的 HUD 狀態
     * 注意：直接使用 IHumanityData 检查升格状态，避免使用 Handler 的服务端静态 Set
     */
    public static PlayerNarrativeState determineState(EntityPlayer player) {
        // 获取人性值数据（客户端和服务端都可用）
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) {
            return PlayerNarrativeState.NONE;
        }

        AscensionRoute route = data.getAscensionRoute();

        // 1. 【優先級最高】香巴拉狀態
        if (route == AscensionRoute.SHAMBHALA) {
            return PlayerNarrativeState.SHAMBHALA;
        }

        // 2. 【優先級第二】破碎之神狀態
        if (route == AscensionRoute.BROKEN_GOD) {
            return PlayerNarrativeState.BROKEN_GOD;
        }

        // 3. 【優先級第三】低人性狀態
        if (data.getHumanity() < LOW_HUMANITY_THRESHOLD) {
            return PlayerNarrativeState.HUMAN_LOW;
        }

        // 4. 默認：普通人類
        return PlayerNarrativeState.HUMAN_HIGH;
    }
}