package com.moremod.client;

import com.moremod.client.gui.MechanicalCoreHUD;
import com.moremod.item.ItemExplorerCompass;
import com.moremod.item.RegisterItem;
import com.moremod.network.PacketCompassLeftClick;
import com.moremod.network.PacketCompassRightClick;
import com.moremod.network.PacketHandler;
import com.moremod.util.EnergySwordClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class ClientEventHandler {

    // ===== 右键冷却 =====
    private static long lastRightClickTime = 0;
    private static long lastLeftClickTime = 0;
    private static final long COOLDOWN = 200; // 200ms冷却，防止重复触发

    // ===== 调试模式 =====
    private static boolean debugMode = false;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        // 注册基础模型
        ModelLoader.setCustomModelResourceLocation(
                RegisterItem.ENERGY_SWORD,
                0,
                new ModelResourceLocation(RegisterItem.ENERGY_SWORD.getRegistryName(), "inventory")
        );

        // 注册属性重写 - 这是关键！
        // ⚠️ 修改：调用EnergySwordClientUtils而不是ItemEnergySword
        RegisterItem.ENERGY_SWORD.addPropertyOverride(
                new ResourceLocation("moremod", "sword_state"),
                (stack, world, entity) -> {
                    return EnergySwordClientUtils.getSwordState(stack);
                }
        );
    }

    /**
     * ✨ 监听鼠标输入 - Shift+右键发射罗盘射线
     * 注意：这个方法在每次鼠标事件时触发，包括移动、点击等
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null || mc.currentScreen != null) return;

        // 检查是否装备了罗盘
        ItemStack compass = ItemExplorerCompass.getEquippedCompass(player);
        if (compass.isEmpty()) return;

        long currentTime = System.currentTimeMillis();

        // 检测右键按下（按钮1是右键）
        if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {

            // 检查是否按住Shift
            if (player.isSneaking()) {
                // 冷却检查
                if (currentTime - lastRightClickTime < COOLDOWN) return;
                lastRightClickTime = currentTime;

                if (debugMode) {
                    player.sendMessage(new TextComponentString("§a[调试] Shift+右键触发"));
                }

                // 发送网络包到服务端
                PacketHandler.INSTANCE.sendToServer(new PacketCompassRightClick());
            }
        }

        // 检测左键按下（按钮0是左键）
        if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {

            // 检查是否按住Shift
            if (player.isSneaking()) {
                // 冷却检查
                if (currentTime - lastLeftClickTime < COOLDOWN) return;
                lastLeftClickTime = currentTime;

                if (debugMode) {
                    player.sendMessage(new TextComponentString("§a[调试] Shift+左键触发"));
                }

                // 发送网络包到服务端
                PacketHandler.INSTANCE.sendToServer(new PacketCompassLeftClick());
            }
        }
    }

    /**
     * 监听键盘输入 - 用于调试模式切换
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null || mc.currentScreen != null) return;

        // F3 + C 切换调试模式
        if (Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.getEventKey() == Keyboard.KEY_C) {
            if (Keyboard.getEventKeyState()) {
                debugMode = !debugMode;
                player.sendMessage(new TextComponentString(
                        debugMode ? "§a[罗盘] 调试模式: 开启" : "§c[罗盘] 调试模式: 关闭"
                ));
            }
        }
    }

    /**
     * 客户端Tick事件 - 用于持续检测
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null || !debugMode) return;

        // 每隔一段时间显示状态
        if (player.ticksExisted % 100 == 0) {
            ItemStack compass = ItemExplorerCompass.getEquippedCompass(player);
            if (!compass.isEmpty()) {
                // 显示罗盘状态
                // player.sendMessage(new TextComponentString("§7[调试] 罗盘已装备"));
            }
        }
    }

    public static void init(FMLInitializationEvent event) {
        // 注册机械核心HUD
        MinecraftForge.EVENT_BUS.register(new MechanicalCoreHUD());
    }
}