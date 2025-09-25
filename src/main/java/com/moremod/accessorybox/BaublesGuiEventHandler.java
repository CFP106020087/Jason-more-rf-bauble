package com.moremod.accessorybox;

import baubles.api.IBauble;
import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.container.SlotBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class BaublesGuiEventHandler {

    // 點擊冷卻控制
    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 100; // 100ms冷卻時間

    // 訊息控制
    private static long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN = 2000; // 2秒訊息冷卻

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)) {
            return;
        }

        GuiContainer gui = (GuiContainer) event.getGui();

        // 只處理Baubles容器
        if (!(gui.inventorySlots instanceof ContainerPlayerExpanded)) {
            return;
        }

        // 檢查是否是滑鼠點擊事件
        if (!Mouse.getEventButtonState()) {
            return; // 不是按下事件，是釋放事件
        }

        // 檢查是否按住Shift
        boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (!shiftPressed) {
            return; // 沒有按Shift，允許正常操作
        }

        // 檢查是否是左鍵
        if (Mouse.getEventButton() != 0) {
            return; // 不是左鍵
        }

        // 檢查點擊冷卻
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            event.setCanceled(true);
            return; // 太快了，直接取消
        }

        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;

        // 獲取滑鼠下的槽位
        Slot slot = getSlotAtPosition(gui, mouseX, mouseY);

        if (slot == null) {
            return; // 沒有點擊在槽位上
        }

        // 檢查是否需要取消操作
        boolean shouldCancel = false;
        String warningMessage = null;

        // 情況1：槽位有物品
        if (slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            boolean isBaubleItem = stack.getItem() instanceof IBauble;

            if (!isBaubleItem) {
                // 非飾品物品，取消操作
                shouldCancel = true;
            }
        }
        // 情況2：空槽位但可能有問題的索引範圍
        else {
            // 檢查是否是擴展的槽位（可能導致崩潰）
            if (slot.slotNumber >= 47) {
                shouldCancel = true;
            }
        }

        // 如果需要取消
        if (shouldCancel) {
            event.setCanceled(true);
            lastClickTime = currentTime;

            // 顯示警告訊息
            if (warningMessage != null && currentTime - lastWarningTime > WARNING_COOLDOWN) {
                mc.player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + warningMessage
                ));
                lastWarningTime = currentTime;
            }
        } else {
            // 允許操作但記錄時間
            lastClickTime = currentTime;
        }
    }

    // 額外的保護：攔截鍵盤按下事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)) {
            return;
        }

        GuiContainer gui = (GuiContainer) event.getGui();

        // 只處理Baubles容器
        if (!(gui.inventorySlots instanceof ContainerPlayerExpanded)) {
            return;
        }

        // 如果按下Shift鍵，顯示提醒
        int key = Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() &&
                (key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT)) {

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWarningTime > 5000) { // 5秒顯示一次
                Minecraft mc = Minecraft.getMinecraft();

                lastWarningTime = currentTime;
            }
        }
    }

    private static Slot getSlotAtPosition(GuiContainer gui, int x, int y) {
        for (int i = 0; i < gui.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = gui.inventorySlots.inventorySlots.get(i);
            if (isMouseOverSlot(gui, slot, x, y)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isMouseOverSlot(GuiContainer gui, Slot slot, int mouseX, int mouseY) {
        int guiLeft = (gui.width - 176) / 2;
        int guiTop = (gui.height - 166) / 2;

        int slotX = guiLeft + slot.xPos;
        int slotY = guiTop + slot.yPos;

        return mouseX >= slotX && mouseX < slotX + 16 &&
                mouseY >= slotY && mouseY < slotY + 16;
    }
}