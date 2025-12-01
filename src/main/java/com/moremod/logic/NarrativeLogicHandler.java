package com.moremod.logic;

import com.moremod.client.gui.PlayerNarrativeState;
import com.moremod.system.ascension.BrokenGodHandler;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items; // 測試用

public class NarrativeLogicHandler {

    // 低人性閾值
    private static final float LOW_HUMANITY_THRESHOLD = 20.0f;

    /**
     * 👁️ 判斷當前的 HUD 狀態
     */
    public static PlayerNarrativeState determineState(EntityPlayer player) {

        // 1. 【優先級最高】測試：手持「下界之星」強制顯示香巴拉狀態
        // (因為你還沒給我香巴拉的具體判斷代碼，我們先用這個測試 HUD)
        if (!player.getHeldItemMainhand().isEmpty() 
            && player.getHeldItemMainhand().getItem() == Items.NETHER_STAR) {
            return PlayerNarrativeState.SHAMBHALA;
        }

        // 2. 【優先級第二】破碎之神 / 停機狀態
        // 直接調用你剛寫好的 BrokenGodHandler！
        if (BrokenGodHandler.isBrokenGod(player)) {
            // 如果你之後想給「停機」做特殊 HUD，可以在這裡細分
            // if (BrokenGodHandler.isInShutdown(player)) return PlayerNarrativeState.SHUTDOWN;
            
            return PlayerNarrativeState.BROKEN_GOD;
        }

        // 3. 【優先級第三】低人性狀態
        // 從 Capability 讀取數據
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            // 如果人性低於 20，顯示低人性黑白濾鏡
            if (data.getHumanity() < LOW_HUMANITY_THRESHOLD) {
                return PlayerNarrativeState.HUMAN_LOW;
            }
        } else {
            // 🚧 測試備案：如果 Capability 還沒數據，用等級模擬一下
            // 這樣你用 /xp 0L 清空等級時也能看到效果
            if (player.experienceLevel < 5) {
                // return PlayerNarrativeState.HUMAN_LOW; // 想測等級觸發就取消這行註釋
            }
        }

        // 4. 默認：普通人類
        return PlayerNarrativeState.HUMAN_HIGH;
    }
}