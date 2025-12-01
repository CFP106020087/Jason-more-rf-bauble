package com.moremod.logic;

import com.moremod.client.gui.PlayerNarrativeState;
import com.moremod.system.ascension.BrokenGodHandler;
import com.moremod.system.ascension.ShambhalaHandler;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.player.EntityPlayer;

public class NarrativeLogicHandler {

    // 低人性閾值
    private static final float LOW_HUMANITY_THRESHOLD = 20.0f;

    /**
     * 👁️ 判斷當前的 HUD 狀態
     */
    public static PlayerNarrativeState determineState(EntityPlayer player) {

        // 1. 【優先級最高】香巴拉狀態
        if (ShambhalaHandler.isShambhala(player)) {
            return PlayerNarrativeState.SHAMBHALA;
        }

        // 2. 【優先級第二】破碎之神 / 停機狀態
        if (BrokenGodHandler.isBrokenGod(player)) {
            return PlayerNarrativeState.BROKEN_GOD;
        }

        // 3. 【優先級第三】低人性狀態
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            if (data.getHumanity() < LOW_HUMANITY_THRESHOLD) {
                return PlayerNarrativeState.HUMAN_LOW;
            }
        }

        // 4. 默認：普通人類
        return PlayerNarrativeState.HUMAN_HIGH;
    }
}