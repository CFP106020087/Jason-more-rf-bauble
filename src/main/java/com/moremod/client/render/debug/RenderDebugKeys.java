package com.moremod.client.render.debug;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class RenderDebugKeys {

    // 核心控制
    public static final KeyBinding TOGGLE_DEBUG =
            new KeyBinding("Toggle Debug (* NumPad)", Keyboard.KEY_MULTIPLY, "MoreMod Debug");

    public static final KeyBinding SWITCH_MODE =
            new KeyBinding("Switch Mode (/ NumPad)", Keyboard.KEY_DIVIDE, "MoreMod Debug");

    public static final KeyBinding RESET =
            new KeyBinding("Reset (NumPad 0)", Keyboard.KEY_NUMPAD0, "MoreMod Debug");

    public static final KeyBinding SAVE_CONFIG =
            new KeyBinding("Save (NumPad Enter)", Keyboard.KEY_NUMPADENTER, "MoreMod Debug");

    public static final KeyBinding PRINT_VALUES =
            new KeyBinding("Print Values (K)", Keyboard.KEY_K, "MoreMod Debug");

    // 平移 - 使用小键盘
    public static final KeyBinding TRANSLATE_LEFT = 
            new KeyBinding("Move Left (NumPad 4)", Keyboard.KEY_NUMPAD4, "MoreMod Debug");
    public static final KeyBinding TRANSLATE_RIGHT = 
            new KeyBinding("Move Right (NumPad 6)", Keyboard.KEY_NUMPAD6, "MoreMod Debug");
    public static final KeyBinding TRANSLATE_UP = 
            new KeyBinding("Move Up (NumPad 8)", Keyboard.KEY_NUMPAD8, "MoreMod Debug");
    public static final KeyBinding TRANSLATE_DOWN = 
            new KeyBinding("Move Down (NumPad 2)", Keyboard.KEY_NUMPAD2, "MoreMod Debug");
    public static final KeyBinding TRANSLATE_FORWARD = 
            new KeyBinding("Move Forward (NumPad 7)", Keyboard.KEY_NUMPAD7, "MoreMod Debug");
    public static final KeyBinding TRANSLATE_BACKWARD = 
            new KeyBinding("Move Backward (NumPad 9)", Keyboard.KEY_NUMPAD9, "MoreMod Debug");

    // 旋转 - 使用字母键避免冲突
    public static final KeyBinding ROTATE_X_POS = 
            new KeyBinding("Rotate X+ (I)", Keyboard.KEY_I, "MoreMod Debug");
    public static final KeyBinding ROTATE_X_NEG = 
            new KeyBinding("Rotate X- (K)", Keyboard.KEY_K, "MoreMod Debug");
    public static final KeyBinding ROTATE_Y_POS = 
            new KeyBinding("Rotate Y+ (J)", Keyboard.KEY_J, "MoreMod Debug");
    public static final KeyBinding ROTATE_Y_NEG = 
            new KeyBinding("Rotate Y- (L)", Keyboard.KEY_L, "MoreMod Debug");
    public static final KeyBinding ROTATE_Z_POS = 
            new KeyBinding("Rotate Z+ (U)", Keyboard.KEY_U, "MoreMod Debug");
    public static final KeyBinding ROTATE_Z_NEG = 
            new KeyBinding("Rotate Z- (O)", Keyboard.KEY_O, "MoreMod Debug");

    // 缩放 - 使用+/-键
    public static final KeyBinding SCALE_UP = 
            new KeyBinding("Scale Up (=)", Keyboard.KEY_EQUALS, "MoreMod Debug");
    public static final KeyBinding SCALE_DOWN = 
            new KeyBinding("Scale Down (-)", Keyboard.KEY_MINUS, "MoreMod Debug");

    public static void register() {
        // Forge 的 KeyBinding 注册用 ClientRegistry
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TOGGLE_DEBUG);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(SWITCH_MODE);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(RESET);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(SAVE_CONFIG);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(PRINT_VALUES);

        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_LEFT);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_RIGHT);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_UP);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_DOWN);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_FORWARD);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(TRANSLATE_BACKWARD);

        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_X_POS);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_X_NEG);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_Y_POS);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_Y_NEG);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_Z_POS);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(ROTATE_Z_NEG);

        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(SCALE_UP);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(SCALE_DOWN);

        System.out.println("[moremod] RenderDebugKeys registered (KeyBindings OK)");
    }
}