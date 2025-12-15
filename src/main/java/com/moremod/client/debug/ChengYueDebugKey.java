package com.moremod.client.debug;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * 澄月动画调试按键 - 可删除
 * K: 循环播放动画
 * I/U: Y轴 上/下
 * J/L: Z轴 前/后
 * H/;: X轴 左/右
 * O/P: 缩放 大/小
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ChengYueDebugKey {

    private static KeyBinding keyAnim;      // K - 动画
    private static KeyBinding keyUp;        // I - 上
    private static KeyBinding keyDown;      // U - 下
    private static KeyBinding keyForward;   // J - 前
    private static KeyBinding keyBack;      // L - 后
    private static KeyBinding keyLeft;      // H - 左
    private static KeyBinding keyRight;     // ; - 右
    private static KeyBinding keyScaleUp;   // O - 放大
    private static KeyBinding keyScaleDown; // P - 缩小

    private static int firstPersonIndex = 0;
    private static int thirdPersonIndex = 0;

    // 调试偏移量 (渲染器可读取)
    public static float offsetX = 0;
    public static float offsetY = -19;
    public static float offsetZ = -15;
    public static float scale = 0.05f;

    private static final float MOVE_STEP = 1f;
    private static final float SCALE_STEP = 0.005f;

    public static void register() {
        keyAnim = new KeyBinding("澄月: 动画", Keyboard.KEY_K, "MoreMod Debug");
        keyUp = new KeyBinding("澄月: 上(Y+)", Keyboard.KEY_I, "MoreMod Debug");
        keyDown = new KeyBinding("澄月: 下(Y-)", Keyboard.KEY_U, "MoreMod Debug");
        keyForward = new KeyBinding("澄月: 前(Z+)", Keyboard.KEY_J, "MoreMod Debug");
        keyBack = new KeyBinding("澄月: 后(Z-)", Keyboard.KEY_L, "MoreMod Debug");
        keyLeft = new KeyBinding("澄月: 左(X-)", Keyboard.KEY_H, "MoreMod Debug");
        keyRight = new KeyBinding("澄月: 右(X+)", Keyboard.KEY_SEMICOLON, "MoreMod Debug");
        keyScaleUp = new KeyBinding("澄月: 放大", Keyboard.KEY_O, "MoreMod Debug");
        keyScaleDown = new KeyBinding("澄月: 缩小", Keyboard.KEY_P, "MoreMod Debug");

        ClientRegistry.registerKeyBinding(keyAnim);
        ClientRegistry.registerKeyBinding(keyUp);
        ClientRegistry.registerKeyBinding(keyDown);
        ClientRegistry.registerKeyBinding(keyForward);
        ClientRegistry.registerKeyBinding(keyBack);
        ClientRegistry.registerKeyBinding(keyLeft);
        ClientRegistry.registerKeyBinding(keyRight);
        ClientRegistry.registerKeyBinding(keyScaleUp);
        ClientRegistry.registerKeyBinding(keyScaleDown);

        System.out.println("[ChengYue] Debug keys registered: K,I,U,J,L,H,;,O,P");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) return;

        // 位置调整
        if (keyUp != null && keyUp.isPressed()) {
            offsetY += MOVE_STEP;
            printOffset();
        }
        if (keyDown != null && keyDown.isPressed()) {
            offsetY -= MOVE_STEP;
            printOffset();
        }
        if (keyForward != null && keyForward.isPressed()) {
            offsetZ += MOVE_STEP;
            printOffset();
        }
        if (keyBack != null && keyBack.isPressed()) {
            offsetZ -= MOVE_STEP;
            printOffset();
        }
        if (keyLeft != null && keyLeft.isPressed()) {
            offsetX -= MOVE_STEP;
            printOffset();
        }
        if (keyRight != null && keyRight.isPressed()) {
            offsetX += MOVE_STEP;
            printOffset();
        }
        if (keyScaleUp != null && keyScaleUp.isPressed()) {
            scale += SCALE_STEP;
            printOffset();
        }
        if (keyScaleDown != null && keyScaleDown.isPressed()) {
            scale = Math.max(0.01f, scale - SCALE_STEP);
            printOffset();
        }

        // 动画切换
        if (keyAnim != null && keyAnim.isPressed()) {
            ChengYueCapability cap = mc.player.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap == null) {
                System.out.println("[ChengYue Debug] cap is null!");
                return;
            }

            boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;

            if (isFirstPerson) {
                firstPersonIndex = (firstPersonIndex % 3) + 1;
                cap.activateSkill(firstPersonIndex);
                System.out.println("[ChengYue Debug] 动画 #" + firstPersonIndex +
                    " (" + getAnimName(firstPersonIndex) + ")");
            } else {
                thirdPersonIndex = (thirdPersonIndex % 3) + 4;
                cap.activateSkill(thirdPersonIndex);
                System.out.println("[ChengYue Debug] 动画 #" + thirdPersonIndex +
                    " (" + getAnimName(thirdPersonIndex) + ")");
            }
        }
    }

    private static void printOffset() {
        System.out.println(String.format("[ChengYue Debug] X=%.1f Y=%.1f Z=%.1f scale=%.3f",
            offsetX, offsetY, offsetZ, scale));
    }

    private static String getAnimName(int index) {
        switch (index) {
            case 1: return "first_person";
            case 2: return "first_attack";
            case 3: return "first_attack2";
            case 4: return "third_person";
            case 5: return "third_attack";
            case 6: return "third_attack2";
            default: return "unknown";
        }
    }
}
