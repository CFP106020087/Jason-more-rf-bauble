package com.moremod.accessorybox.client;

import baubles.client.gui.GuiPlayerExpanded;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * EX 按钮（反射版），位置自动贴在“副手槽位”旁边。
 * - 仅在 GuiPlayerExpanded 中注入
 * - 通过容器 slots 查找 slotIndex == 40 的副手槽位
 * - 按钮放在副手槽右侧，不改变任何 Y 坐标
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class ExtraSlotsToggleButtonHandler {

    private static final int BUTTON_ID = 0xE57A;
    private static final int GUI_W_FALLBACK = 176;
    private static final int GUI_H_FALLBACK = 166;

    private static final int BTN_W = 20;
    private static final int BTN_H = 20;

    // 相对“副手槽位”的偏移（右侧 +4，垂直轻微上移 -1，使 20px 按钮贴合 18px 槽）
    private static final int OFFSET_X_AFTER_OFFHAND = 4;
    private static final int OFFSET_Y_ALIGN = -1;

    // 找不到副手时的兜底：右上角
    private static final int FALLBACK_OFFSET_X_FROM_RIGHT = 2;
    private static final int FALLBACK_OFFSET_Y_FROM_TOP   = 6;

    private static WeakReference<GuiButton> lastButtonRef = new WeakReference<>(null);

    /** 简易无贴图按钮 */
    private static class ButtonToggleExtraPlain extends GuiButton {
        ButtonToggleExtraPlain(int id, int x, int y) {
            super(id, x, y, BTN_W, BTN_H, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            this.hovered = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;

            GlStateManager.pushMatrix();

            final boolean on = ExtraSlotsToggle.isVisible();
            int fill = on ? 0xAA334400 : 0xAA2E2E2E;
            if (this.hovered) fill = on ? 0xCC3D4E00 : 0xCC3A3A3A;

            // 背板
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, fill);

            // 外框 & 内描边
            int dark  = 0xFF3A3A3A;
            int light = 0xFF9E9E9E;
            drawRect(x, y, x + width, y + 1, dark);
            drawRect(x, y + height - 1, x + width, y + height, dark);
            drawRect(x, y, x + 1, y + height, dark);
            drawRect(x + width - 1, y, x + width, y + height, dark);
            drawRect(x + 1, y + 1, x + width - 1, y + 2, light);
            drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, light);
            drawRect(x + 1, y + 1, x + 2, y + height - 1, light);
            drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, light);

            // 文本 “EX”
            String label = "EX";
            int textColor = on ? 0xFFEED87A : 0xFFECECEC;
            int tw = mc.fontRenderer.getStringWidth(label);
            int tx = this.x + (this.width  - tw) / 2;
            int ty = this.y + (this.height - mc.fontRenderer.FONT_HEIGHT) / 2;
            mc.fontRenderer.drawString(label, tx, ty, textColor, true);

            // 开启指示点
            if (on) {
                int d = 3;
                drawRect(this.x + this.width - d - 2, this.y + this.height - d - 2,
                        this.x + this.width - 2,        this.y + this.height - 2,
                        0xFFFFE082);
            }

            GlStateManager.popMatrix();
        }
    }

    /** 初始化时注入按钮，并把位置贴在副手槽边上 */
    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post e) {
        if (!(e.getGui() instanceof GuiPlayerExpanded)) return;

        GuiScreen gui = e.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);

        // 通过反射拿 guiLeft / guiTop（失败则用 176×166 兜底）
        int guiLeft = tryGetIntField(gui, "guiLeft", "field_147003_i",
                (sr.getScaledWidth() - GUI_W_FALLBACK) / 2);
        int guiTop  = tryGetIntField(gui, "guiTop",  "field_147009_r",
                (sr.getScaledHeight() - GUI_H_FALLBACK) / 2);

        // 查找副手槽位屏幕坐标
        int[] offhandXY = findOffhandScreenXY((GuiContainer) gui, guiLeft, guiTop);

        int x, y;
        if (offhandXY != null) {
            // 紧贴副手槽位右侧
            x = offhandXY[0] + 18 + OFFSET_X_AFTER_OFFHAND;
            y = offhandXY[1] + OFFSET_Y_ALIGN;
        } else {
            // 兜底：右上角
            x = guiLeft + GUI_W_FALLBACK - BTN_W - FALLBACK_OFFSET_X_FROM_RIGHT;
            y = guiTop  + FALLBACK_OFFSET_Y_FROM_TOP;
        }

        // 清理旧按钮
        List<GuiButton> list = tryGetButtonList(gui);
        if (list != null) {
            for (Iterator<GuiButton> it = list.iterator(); it.hasNext();) {
                GuiButton b = it.next();
                if (b != null && b.id == BUTTON_ID) it.remove();
            }
        }

        GuiButton btn = new ButtonToggleExtraPlain(BUTTON_ID, x, y);

        // 优先调用 addButton；失败再直接加到 buttonList
        if (!tryCallAddButton(gui, btn)) {
            if (list != null) list.add(btn);
        }

        lastButtonRef = new WeakReference<>(btn);
    }

    /** 点击切换可见性 */
    @SubscribeEvent
    public static void onAction(GuiScreenEvent.ActionPerformedEvent.Pre e) {
        if (!(e.getGui() instanceof GuiPlayerExpanded)) return;
        if (e.getButton() == null) return;
        if (e.getButton().id == BUTTON_ID) {
            ExtraSlotsToggle.toggle();
        }
    }

    /** 悬浮提示 */
    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!(e.getGui() instanceof GuiPlayerExpanded)) return;

        List<GuiButton> list = tryGetButtonList(e.getGui());
        GuiButton target = null;
        if (list != null) {
            for (GuiButton b : list) if (b != null && b.id == BUTTON_ID) { target = b; break; }
        } else {
            target = lastButtonRef.get();
        }
        if (target != null && target.isMouseOver()) {
            e.getGui().drawHoveringText(
                    ExtraSlotsToggle.isVisible()
                            ? java.util.Arrays.asList("隐藏额外饰品栏 (EX)", "Click to hide extra slots")
                            : java.util.Arrays.asList("显示额外饰品栏 (EX)", "Click to show extra slots"),
                    e.getMouseX(), e.getMouseY()
            );
        }
    }

    // ======================
    //   位置/反射工具
    // ======================

    /** 返回副手槽位的屏幕坐标 [x, y]；找不到返回 null */
    private static int[] findOffhandScreenXY(GuiContainer gui, int guiLeft, int guiTop) {
        try {
            Container container = tryGetContainer(gui);
            if (container == null) container = Minecraft.getMinecraft().player.openContainer;
            if (container == null) return null;

            @SuppressWarnings("unchecked")
            List<Slot> slots = (List<Slot>) tryGetField(container, "inventorySlots", "field_75151_b");
            if (slots == null) return null;

            for (Slot s : slots) {
                if (s != null) {
                    try {
                        if (s.getSlotIndex() == 40) { // 副手
                            return new int[] { guiLeft + s.xPos, guiTop + s.yPos };
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Container tryGetContainer(GuiContainer gui) {
        Object o = tryGetField(gui, "inventorySlots", "field_147002_h");
        return (o instanceof Container) ? (Container) o : null;
    }

    @SuppressWarnings("unchecked")
    private static List<GuiButton> tryGetButtonList(GuiScreen gui) {
        Object val = tryGetField(gui, "buttonList", "field_146292_n");
        return (val instanceof List) ? (List<GuiButton>) val : null;
    }

    private static boolean tryCallAddButton(GuiScreen gui, GuiButton btn) {
        Method m = getMethod(gui.getClass(), "addButton", "func_189646_b", GuiButton.class);
        if (m != null) {
            try { m.setAccessible(true); m.invoke(gui, btn); return true; } catch (Throwable ignored) {}
        }
        m = getMethod(GuiScreen.class, "addButton", "func_189646_b", GuiButton.class);
        if (m != null) {
            try { m.setAccessible(true); m.invoke(gui, btn); return true; } catch (Throwable ignored) {}
        }
        return false;
    }

    private static int tryGetIntField(Object obj, String mcp, String srg, int fallback) {
        try {
            Field f = getField(obj.getClass(), mcp, srg);
            if (f != null) {
                f.setAccessible(true);
                return f.getInt(obj);
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static Object tryGetField(Object obj, String mcp, String srg) {
        try {
            Field f = getField(obj.getClass(), mcp, srg);
            if (f != null) { f.setAccessible(true); return f.get(obj); }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Field getField(Class<?> c, String mcp, String srg) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(mcp); } catch (Throwable ignored) {}
            try { return k.getDeclaredField(srg); } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Method getMethod(Class<?> c, String mcp, String srg, Class<?>... params) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredMethod(mcp, params); } catch (Throwable ignored) {}
            try { return k.getDeclaredMethod(srg, params); } catch (Throwable ignored) {}
        }
        return null;
    }
}
