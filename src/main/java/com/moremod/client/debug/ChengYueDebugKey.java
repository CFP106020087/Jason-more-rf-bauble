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
 *
 * 动画: K 循环
 * 位置: I/U(Y), J/L(Z), H/;(X)
 * 旋转: Y/N(X轴), G/B(Y轴), T/V(Z轴)
 * 缩放: O/P
 * 重置: R
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ChengYueDebugKey {

    // 动画
    private static KeyBinding keyAnim;

    // 位置
    private static KeyBinding keyUp, keyDown;       // I/U - Y
    private static KeyBinding keyForward, keyBack;  // J/L - Z
    private static KeyBinding keyLeft, keyRight;    // H/; - X

    // 旋转
    private static KeyBinding keyRotXP, keyRotXN;   // Y/N - X轴旋转
    private static KeyBinding keyRotYP, keyRotYN;   // G/B - Y轴旋转
    private static KeyBinding keyRotZP, keyRotZN;   // T/V - Z轴旋转

    // 缩放
    private static KeyBinding keyScaleUp, keyScaleDown;  // O/P

    // 重置
    private static KeyBinding keyReset;  // R

    private static int firstPersonIndex = 0;
    private static int thirdPersonIndex = 0;

    // 调试偏移量 (渲染器可读取)
    public static float offsetX = 0;
    public static float offsetY = -19;
    public static float offsetZ = -15;
    public static float rotateX = 0;
    public static float rotateY = 0;
    public static float rotateZ = 0;
    public static float scale = 0.05f;

    // 默认值
    private static final float DEF_X = 0, DEF_Y = -19, DEF_Z = -15;
    private static final float DEF_RX = 0, DEF_RY = 0, DEF_RZ = 0;
    private static final float DEF_SCALE = 0.05f;

    private static final float MOVE_STEP = 1f;
    private static final float ROT_STEP = 5f;
    private static final float SCALE_STEP = 0.005f;

    public static void register() {
        keyAnim = reg("澄月: 动画(K)", Keyboard.KEY_K);

        keyUp = reg("澄月: 上(I)", Keyboard.KEY_I);
        keyDown = reg("澄月: 下(U)", Keyboard.KEY_U);
        keyForward = reg("澄月: 前(J)", Keyboard.KEY_J);
        keyBack = reg("澄月: 后(L)", Keyboard.KEY_L);
        keyLeft = reg("澄月: 左(H)", Keyboard.KEY_H);
        keyRight = reg("澄月: 右(;)", Keyboard.KEY_SEMICOLON);

        keyRotXP = reg("澄月: 旋转X+(Y)", Keyboard.KEY_Y);
        keyRotXN = reg("澄月: 旋转X-(N)", Keyboard.KEY_N);
        keyRotYP = reg("澄月: 旋转Y+(G)", Keyboard.KEY_G);
        keyRotYN = reg("澄月: 旋转Y-(B)", Keyboard.KEY_B);
        keyRotZP = reg("澄月: 旋转Z+(T)", Keyboard.KEY_T);
        keyRotZN = reg("澄月: 旋转Z-(V)", Keyboard.KEY_V);

        keyScaleUp = reg("澄月: 放大(O)", Keyboard.KEY_O);
        keyScaleDown = reg("澄月: 缩小(P)", Keyboard.KEY_P);

        keyReset = reg("澄月: 重置(R)", Keyboard.KEY_R);

        System.out.println("[ChengYue] Debug keys registered");
    }

    private static KeyBinding reg(String name, int key) {
        KeyBinding kb = new KeyBinding(name, key, "MoreMod Debug");
        ClientRegistry.registerKeyBinding(kb);
        return kb;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) return;

        boolean changed = false;

        // 位置
        if (pressed(keyUp)) { offsetY += MOVE_STEP; changed = true; }
        if (pressed(keyDown)) { offsetY -= MOVE_STEP; changed = true; }
        if (pressed(keyForward)) { offsetZ += MOVE_STEP; changed = true; }
        if (pressed(keyBack)) { offsetZ -= MOVE_STEP; changed = true; }
        if (pressed(keyLeft)) { offsetX -= MOVE_STEP; changed = true; }
        if (pressed(keyRight)) { offsetX += MOVE_STEP; changed = true; }

        // 旋转
        if (pressed(keyRotXP)) { rotateX += ROT_STEP; changed = true; }
        if (pressed(keyRotXN)) { rotateX -= ROT_STEP; changed = true; }
        if (pressed(keyRotYP)) { rotateY += ROT_STEP; changed = true; }
        if (pressed(keyRotYN)) { rotateY -= ROT_STEP; changed = true; }
        if (pressed(keyRotZP)) { rotateZ += ROT_STEP; changed = true; }
        if (pressed(keyRotZN)) { rotateZ -= ROT_STEP; changed = true; }

        // 缩放
        if (pressed(keyScaleUp)) { scale += SCALE_STEP; changed = true; }
        if (pressed(keyScaleDown)) { scale = Math.max(0.01f, scale - SCALE_STEP); changed = true; }

        // 重置
        if (pressed(keyReset)) {
            offsetX = DEF_X; offsetY = DEF_Y; offsetZ = DEF_Z;
            rotateX = DEF_RX; rotateY = DEF_RY; rotateZ = DEF_RZ;
            scale = DEF_SCALE;
            System.out.println("[ChengYue Debug] 已重置!");
            changed = true;
        }

        if (changed) printState();

        // 动画切换
        if (pressed(keyAnim)) {
            ChengYueCapability cap = mc.player.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap == null) {
                System.out.println("[ChengYue Debug] cap is null!");
                return;
            }

            boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;

            if (isFirstPerson) {
                firstPersonIndex = (firstPersonIndex % 3) + 1;
                cap.activateSkill(firstPersonIndex);
                System.out.println("[ChengYue Debug] 动画: " + getAnimName(firstPersonIndex));
            } else {
                thirdPersonIndex = (thirdPersonIndex % 3) + 4;
                cap.activateSkill(thirdPersonIndex);
                System.out.println("[ChengYue Debug] 动画: " + getAnimName(thirdPersonIndex));
            }
        }
    }

    private static boolean pressed(KeyBinding key) {
        return key != null && key.isPressed();
    }

    private static void printState() {
        System.out.println(String.format(
            "[ChengYue Debug] pos(%.1f, %.1f, %.1f) rot(%.1f, %.1f, %.1f) scale=%.3f",
            offsetX, offsetY, offsetZ, rotateX, rotateY, rotateZ, scale));
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
