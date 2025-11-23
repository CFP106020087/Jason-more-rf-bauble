package com.moremod.client;

import com.moremod.network.MessageToggleJetpackMode;
import com.moremod.network.PacketHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class JetpackKeyHandler {

    public static final KeyBinding keyToggleJetpack =
            new KeyBinding("key.moremod.jetpack_toggle", Keyboard.KEY_V, "key.categories.moremod");

    public static final KeyBinding keyToggleHover =
            new KeyBinding("key.moremod.hover_toggle", Keyboard.KEY_H, "key.categories.moremod");

    // 新增：速度模式切换按键
    public static final KeyBinding keyToggleSpeedMode =
            new KeyBinding("key.moremod.speed_toggle", Keyboard.KEY_G, "key.categories.moremod");

    public static void registerKeys() {
        ClientRegistry.registerKeyBinding(keyToggleJetpack);
        ClientRegistry.registerKeyBinding(keyToggleHover);
        ClientRegistry.registerKeyBinding(keyToggleSpeedMode); // 新增
    }

    /** ⚠️ 注意：不要加 static，这样事件监听才能被实例化触发 */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyToggleJetpack.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(1)); // 喷气背包开关
        }

        if (keyToggleHover.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(0)); // 悬停模式
        }

        // 新增：速度模式切换
        if (keyToggleSpeedMode.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(2)); // 速度模式
        }
    }
}