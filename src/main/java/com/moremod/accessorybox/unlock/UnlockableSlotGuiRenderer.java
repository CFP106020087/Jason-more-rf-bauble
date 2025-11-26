package com.moremod.accessorybox.unlock;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.client.gui.GuiPlayerExpanded;
import com.moremod.accessorybox.DynamicGuiLayout;
import com.moremod.accessorybox.Point;
import com.moremod.accessorybox.SlotLayoutHelper;
import com.moremod.accessorybox.client.ExtraSlotsToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class UnlockableSlotGuiRenderer {

    private static final ResourceLocation SLOT_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/slot_bg.png");

    private static final Map<BaubleType, ResourceLocation> ICON = new HashMap<>();
    static {
        ICON.put(BaubleType.AMULET,  new ResourceLocation("moremod", "textures/gui/icons/amulet.png"));
        ICON.put(BaubleType.RING,    new ResourceLocation("moremod", "textures/gui/icons/ring.png"));
        ICON.put(BaubleType.BELT,    new ResourceLocation("moremod", "textures/gui/icons/belt.png"));
        ICON.put(BaubleType.HEAD,    new ResourceLocation("moremod", "textures/gui/icons/head.png"));
        ICON.put(BaubleType.BODY,    new ResourceLocation("moremod", "textures/gui/icons/body.png"));
        ICON.put(BaubleType.CHARM,   new ResourceLocation("moremod", "textures/gui/icons/charm.png"));
        ICON.put(BaubleType.TRINKET, new ResourceLocation("moremod", "textures/gui/icons/trinket.png"));
    }

    @SubscribeEvent
    public static void onGuiBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.getGui() instanceof GuiPlayerExpanded)) return;
        if (!ExtraSlotsToggle.isVisible()) return; // EX 关闭时不绘制

        GuiPlayerExpanded gui = (GuiPlayerExpanded) event.getGui();
        Minecraft mc = gui.mc;
        EntityPlayer player = mc.player;
        if (player == null) return;

        // 不改 Y 轴：计算 vanilla GUI 原点
        ScaledResolution sr = new ScaledResolution(mc);
        int guiLeft = (sr.getScaledWidth()  - 176) / 2;
        int guiTop  = (sr.getScaledHeight() - 166) / 2;

        // 可用槽位集合（含默认 + 已解锁）
        Set<Integer> available = SlotUnlockManager.getInstance().getAvailableSlots(player.getUniqueID());
        if (available.isEmpty()) return;

        // Baubles handler 用于判断“该槽是否为空”
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        List<int[]> rendered = new ArrayList<>(available.size());

        drawType(gui, guiLeft, guiTop, alloc.amuletSlots,  1, BaubleType.AMULET,  available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.ringSlots,    2, BaubleType.RING,    available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.beltSlots,    1, BaubleType.BELT,    available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.headSlots,    1, BaubleType.HEAD,    available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.bodySlots,    1, BaubleType.BODY,    available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.charmSlots,   1, BaubleType.CHARM,   available, rendered, handler);
        drawType(gui, guiLeft, guiTop, alloc.trinketSlots, 7, BaubleType.TRINKET, available, rendered, handler);

        if (!rendered.isEmpty()) drawBoundingRect(rendered);
    }

    private static void drawType(GuiPlayerExpanded gui, int guiLeft, int guiTop,
                                 int[] slots, int vanillaCount, BaubleType type,
                                 Set<Integer> available, List<int[]> rendered,
                                 IBaublesItemHandler handler) {
        if (slots == null || slots.length <= vanillaCount) return;

        for (int i = vanillaCount; i < slots.length; i++) {
            int id = slots[i];
            if (!available.contains(id)) continue;

            // GUI 坐标（不动 Y）
            Point p = DynamicGuiLayout.getSlotPosition(id);
            int x = guiLeft + p.getX();
            int y = guiTop  + p.getY();

            // 背景（总是画，出现在物品下方）
            drawSlotBg(gui, x, y);

            // 槽位空才画“小图标”，避免覆盖已佩戴物 → 白块
            boolean empty = (id >= 0 && id < handler.getSlots()) && handler.getStackInSlot(id).isEmpty();
            if (empty) {
                ResourceLocation icon = ICON.get(type);
                if (icon != null) drawIcon(icon, x + 1, y + 1);
            }

            rendered.add(new int[]{x, y});
        }
    }

    private static void drawSlotBg(Gui gui, int x, int y) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.color(1F, 1F, 1F, 1F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(SLOT_TEXTURE);
            gui.drawTexturedModalRect(x, y, 0, 0, 18, 18);
        } catch (Throwable t) {
            // 退化：灰色方框
            Gui.drawRect(x, y, x + 18, y + 18, 0xFF8B8B8B);
            Gui.drawRect(x, y, x + 18, y + 1, 0xFF555555);
            Gui.drawRect(x, y + 17, x + 18, y + 18, 0xFF555555);
            Gui.drawRect(x, y, x + 1, y + 18, 0xFF555555);
            Gui.drawRect(x + 17, y, x + 18, y + 18, 0xFF555555);
        } finally {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }
    }

    private static void drawIcon(ResourceLocation tex, int x, int y) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO);
            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
            GlStateManager.color(1F, 1F, 1F, 0.85F);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        } catch (Throwable ignored) {
            // 忽略，保持干净
        } finally {
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }
    }

    private static void drawBoundingRect(List<int[]> rendered) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (int[] pos : rendered) {
            int x = pos[0], y = pos[1];
            minX = Math.min(minX, x - 3);
            minY = Math.min(minY, y - 3);
            maxX = Math.max(maxX, x + 18 + 3);
            maxY = Math.max(maxY, y + 18 + 3);
        }
        if (minX > maxX || minY > maxY) return;

        GlStateManager.pushMatrix();
        try {
            Gui.drawRect(minX, minY, maxX, maxY, 0x33111111);
            int border = 0xFF6A6A6A;
            Gui.drawRect(minX,     minY,     maxX,     minY + 1, border);
            Gui.drawRect(minX,     maxY - 1, maxX,     maxY,     border);
            Gui.drawRect(minX,     minY,     minX + 1, maxY,     border);
            Gui.drawRect(maxX - 1, minY,     maxX,     maxY,     border);
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
