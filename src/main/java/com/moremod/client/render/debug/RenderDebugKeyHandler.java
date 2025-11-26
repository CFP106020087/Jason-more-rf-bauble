package com.moremod.client.render.debug;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public class RenderDebugKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {

        if (Minecraft.getMinecraft().player == null) return;

        boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        // 开关调试模式
        if (RenderDebugKeys.TOGGLE_DEBUG.isPressed()) {
            RenderDebugConfig.toggleDebug();
            return;
        }

        if (!RenderDebugConfig.isDebugEnabled()) return;

        // 切换 TransformMode（GUI / First / Third）
        if (RenderDebugKeys.SWITCH_MODE.isPressed()) {
            RenderDebugConfig.cycleMode();
            return;
        }

        // 重置
        if (RenderDebugKeys.RESET.isPressed()) {
            if (fine) RenderDebugConfig.resetAll();
            else RenderDebugConfig.resetCurrent();
            return;
        }

        // 保存当前配置
        if (RenderDebugKeys.SAVE_CONFIG.isPressed()) {
            RenderDebugConfig.saveConfig();
            return;
        }

        // 输出信息
        if (RenderDebugKeys.PRINT_VALUES.isPressed()) {
            RenderDebugConfig.printCurrentValues();
            return;
        }

        // 平移
        if (RenderDebugKeys.TRANSLATE_LEFT.isPressed()) RenderDebugConfig.adjustTranslateX(-1, fine);
        if (RenderDebugKeys.TRANSLATE_RIGHT.isPressed()) RenderDebugConfig.adjustTranslateX( 1, fine);
        if (RenderDebugKeys.TRANSLATE_UP.isPressed()) RenderDebugConfig.adjustTranslateY( 1, fine);
        if (RenderDebugKeys.TRANSLATE_DOWN.isPressed()) RenderDebugConfig.adjustTranslateY(-1, fine);
        if (RenderDebugKeys.TRANSLATE_FORWARD.isPressed()) RenderDebugConfig.adjustTranslateZ(-1, fine);
        if (RenderDebugKeys.TRANSLATE_BACKWARD.isPressed()) RenderDebugConfig.adjustTranslateZ(1, fine);

        // 旋转
        if (RenderDebugKeys.ROTATE_X_POS.isPressed()) RenderDebugConfig.adjustRotateX(1, fine);
        if (RenderDebugKeys.ROTATE_X_NEG.isPressed()) RenderDebugConfig.adjustRotateX(-1, fine);
        if (RenderDebugKeys.ROTATE_Y_POS.isPressed()) RenderDebugConfig.adjustRotateY(1, fine);
        if (RenderDebugKeys.ROTATE_Y_NEG.isPressed()) RenderDebugConfig.adjustRotateY(-1, fine);
        if (RenderDebugKeys.ROTATE_Z_POS.isPressed()) RenderDebugConfig.adjustRotateZ(1, fine);
        if (RenderDebugKeys.ROTATE_Z_NEG.isPressed()) RenderDebugConfig.adjustRotateZ(-1, fine);

        // 缩放
        if (RenderDebugKeys.SCALE_UP.isPressed()) RenderDebugConfig.adjustScale(1, fine);
        if (RenderDebugKeys.SCALE_DOWN.isPressed()) RenderDebugConfig.adjustScale(-1, fine);
    }
}
