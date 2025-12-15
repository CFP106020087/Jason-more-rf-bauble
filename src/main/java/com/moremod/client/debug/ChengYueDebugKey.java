package com.moremod.client.debug;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * 澄月动画调试按键 - 可删除
 * 按 K 键循环播放动画
 * 第一人称: first_person -> first_attack -> first_attack2
 * 第三人称: third_person -> third_attack -> third_attack2
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ChengYueDebugKey {

    private static KeyBinding debugKey;
    private static int firstPersonIndex = 0;  // 0, 1, 2
    private static int thirdPersonIndex = 0;  // 0, 1, 2

    public static void register() {
        debugKey = new KeyBinding(
            "澄月动画调试 (K)",
            Keyboard.KEY_K,
            "MoreMod Debug"
        );
        ClientRegistry.registerKeyBinding(debugKey);
        System.out.println("[ChengYue] Debug key registered: K");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (debugKey == null || !debugKey.isPressed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        ChengYueCapability cap = mc.player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null) {
            System.out.println("[ChengYue Debug] cap is null!");
            return;
        }

        boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;

        if (isFirstPerson) {
            // 第一人称: 循环 1, 2, 3 (对应 first_person, first_attack, first_attack2)
            firstPersonIndex = (firstPersonIndex % 3) + 1;
            cap.activateSkill(firstPersonIndex);
            System.out.println("[ChengYue Debug] 第一人称动画 #" + firstPersonIndex +
                " (" + getFirstPersonAnimName(firstPersonIndex) + ")");
        } else {
            // 第三人称: 循环 4, 5, 6 (对应 third_person, third_attack, third_attack2)
            thirdPersonIndex = (thirdPersonIndex % 3) + 4;
            cap.activateSkill(thirdPersonIndex);
            System.out.println("[ChengYue Debug] 第三人称动画 #" + thirdPersonIndex +
                " (" + getThirdPersonAnimName(thirdPersonIndex) + ")");
        }
    }

    private static String getFirstPersonAnimName(int index) {
        switch (index) {
            case 1: return "first_person";
            case 2: return "first_attack";
            case 3: return "first_attack2";
            default: return "unknown";
        }
    }

    private static String getThirdPersonAnimName(int index) {
        switch (index) {
            case 4: return "third_person";
            case 5: return "third_attack";
            case 6: return "third_attack2";
            default: return "unknown";
        }
    }
}
