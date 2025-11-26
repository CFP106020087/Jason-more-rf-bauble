package com.moremod.synergy.station;

import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketSynergyStationAction;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

/**
 * Synergy 链结站 GUI - 纯代码绘制版（无物品栏）
 *
 * 使用 GuiScreen 而非 GuiContainer，不显示玩家物品栏。
 *
 * 功能：
 * - 左侧显示玩家已安装的模块列表（可滚动）
 * - 右侧显示 6 个链结槽位（六边形布局）
 * - 拖拽模块到槽位进行链结
 * - 右键点击槽位移除模块
 * - 显示匹配的 Synergy 效果
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyStation extends GuiScreen {

    // ==================== 颜色常量 ====================
    private static final int COLOR_BG_DARK = 0xFF1a1a2e;
    private static final int COLOR_BG_LIGHT = 0xFF16213e;
    private static final int COLOR_PANEL_BG = 0xFF0f3460;
    private static final int COLOR_ACCENT = 0xFF00d9ff;
    private static final int COLOR_ACCENT_DIM = 0xFF006080;
    private static final int COLOR_TEXT_TITLE = 0xFF00ffff;
    private static final int COLOR_TEXT_NORMAL = 0xFFe0e0e0;
    private static final int COLOR_TEXT_DIM = 0xFF808080;
    private static final int COLOR_SLOT_EMPTY = 0xFF2a2a4a;
    private static final int COLOR_SLOT_FILLED = 0xFF4040a0;
    private static final int COLOR_SLOT_HOVER = 0xFF6060c0;
    private static final int COLOR_SUCCESS = 0xFF00ff88;
    private static final int COLOR_WARNING = 0xFFffaa00;

    // ==================== 布局常量 ====================
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 180;

    // 模块列表区域
    private static final int MODULE_PANEL_X = 8;
    private static final int MODULE_PANEL_Y = 25;
    private static final int MODULE_PANEL_WIDTH = 100;
    private static final int MODULE_PANEL_HEIGHT = 130;
    private static final int MODULE_ENTRY_HEIGHT = 18;
    private static final int MAX_VISIBLE_MODULES = 7;

    // 链结区域
    private static final int LINK_CENTER_X = 190;
    private static final int LINK_CENTER_Y = 80;
    private static final int LINK_SLOT_RADIUS = 45;
    private static final int SLOT_SIZE = 24;

    // Synergy 信息区域
    private static final int INFO_PANEL_X = 115;
    private static final int INFO_PANEL_Y = 130;
    private static final int INFO_PANEL_WIDTH = 157;
    private static final int INFO_PANEL_HEIGHT = 42;

    // 按钮 ID
    private static final int BTN_CLEAR_ALL = 0;
    private static final int BTN_ACTIVATE = 1;

    // ==================== 状态 ====================
    private final TileEntitySynergyStation tileEntity;
    private final EntityPlayer player;

    private int guiLeft;
    private int guiTop;

    private List<ModuleEntry> installedModules = new ArrayList<>();
    private int scrollOffset = 0;

    private ModuleEntry draggingModule = null;
    private int hoveredSlot = -1;
    private int hoveredModuleIndex = -1;

    private final int[][] slotPositions = new int[6][2];
    private List<SynergyDefinition> matchingSynergies = new ArrayList<>();

    private float animationTick = 0;

    /**
     * 模块条目
     */
    private static class ModuleEntry {
        final String moduleId;
        final String displayName;
        final int level;
        final boolean active;

        ModuleEntry(String id, String name, int level, boolean active) {
            this.moduleId = id;
            this.displayName = name;
            this.level = level;
            this.active = active;
        }
    }

    public GuiSynergyStation(EntityPlayer player, TileEntitySynergyStation te) {
        this.tileEntity = te;
        this.player = player;

        calculateSlotPositions();
        refreshInstalledModules();
    }

    private void calculateSlotPositions() {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 2;
            slotPositions[i][0] = (int) (LINK_CENTER_X + Math.cos(angle) * LINK_SLOT_RADIUS - SLOT_SIZE / 2);
            slotPositions[i][1] = (int) (LINK_CENTER_Y + Math.sin(angle) * LINK_SLOT_RADIUS - SLOT_SIZE / 2);
        }
    }

    private void refreshInstalledModules() {
        installedModules.clear();

        ItemStack coreStack = ItemMechanicalCoreExtended.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        List<String> installedIds = ItemMechanicalCoreExtended.getInstalledUpgradeIds(coreStack);
        Map<String, ItemMechanicalCoreExtended.UpgradeInfo> allUpgrades =
                ItemMechanicalCoreExtended.getAllUpgrades();

        for (String id : installedIds) {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, id);
            boolean active = ItemMechanicalCoreExtended.isUpgradeActive(coreStack, id);

            String displayName = id;
            ItemMechanicalCoreExtended.UpgradeInfo info = allUpgrades.get(id);
            if (info != null && info.displayName != null) {
                displayName = info.displayName;
            }

            installedModules.add(new ModuleEntry(id, displayName, level, active));
        }

        installedModules.sort(Comparator.comparing(e -> e.displayName));
        updateMatchingSynergies();
    }

    private void updateMatchingSynergies() {
        matchingSynergies.clear();

        List<String> linkedModules = tileEntity.getLinkedModules();
        if (linkedModules.size() < 2) return;

        SynergyManager manager = SynergyManager.getInstance();
        Set<String> linkedSet = new HashSet<>(linkedModules);

        for (SynergyDefinition synergy : manager.getAll()) {
            List<String> required = synergy.getRequiredModules();
            if (linkedSet.containsAll(required)) {
                matchingSynergies.add(synergy);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // 清空按钮
        this.buttonList.add(new GuiButton(BTN_CLEAR_ALL,
                guiLeft + 8, guiTop + GUI_HEIGHT - 24, 45, 16, "Clear"));

        // 激活按钮
        this.buttonList.add(new GuiButton(BTN_ACTIVATE,
                guiLeft + 58, guiTop + GUI_HEIGHT - 24, 50, 16,
                tileEntity.isActivated() ? "ON" : "OFF"));

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BTN_CLEAR_ALL:
                // 发送网络包到服务器
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.CLEAR_ALL
                        )
                );
                // 本地更新（服务器会同步回来，但本地先更新以提供即时反馈）
                tileEntity.clearAllSlots();
                updateMatchingSynergies();
                break;

            case BTN_ACTIVATE:
                // 发送网络包到服务器
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.TOGGLE_ACTIVE
                        )
                );
                // 本地更新
                tileEntity.toggleActivated();
                button.displayString = tileEntity.isActivated() ? "ON" : "OFF";

                // 激活状态反馈
                if (tileEntity.isActivated()) {
                    player.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
                    if (!matchingSynergies.isEmpty()) {
                        StringBuilder sb = new StringBuilder("§a[链结站激活] §f生效中的协同效果:");
                        for (SynergyDefinition synergy : matchingSynergies) {
                            sb.append("\n  §b▸ ").append(synergy.getDisplayName());
                        }
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(sb.toString()));
                    } else {
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§e[链结站激活] §7当前无匹配协同（需要2个以上模块）"));
                    }
                } else {
                    player.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 0.5f, 1.0f);
                    player.sendMessage(new net.minecraft.util.text.TextComponentString("§c[链结站关闭]"));
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制暗色背景覆盖
        drawDefaultBackground();

        animationTick += partialTicks;
        updateHoverState(mouseX, mouseY);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableTexture2D();

        // 绘制主背景
        drawMainBackground();

        // 绘制标题栏
        drawTitleBar();

        // 绘制模块列表面板
        drawModulePanel();

        // 绘制链结区域
        drawLinkArea();

        // 绘制信息面板
        drawInfoPanel();

        GlStateManager.enableTexture2D();

        // 绘制按钮
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制拖拽中的模块
        if (draggingModule != null) {
            int labelWidth = fontRenderer.getStringWidth(draggingModule.displayName) + 8;
            GlStateManager.disableTexture2D();
            drawRect(mouseX - 2, mouseY - 2, mouseX + labelWidth, mouseY + 12, 0xE0000000);
            drawHollowRect(mouseX - 2, mouseY - 2, labelWidth + 2, 14, COLOR_ACCENT);
            GlStateManager.enableTexture2D();
            fontRenderer.drawString(draggingModule.displayName, mouseX + 2, mouseY + 2, COLOR_TEXT_TITLE);
        }

        // 绘制悬停提示
        drawTooltips(mouseX, mouseY);
    }

    private void updateHoverState(int mouseX, int mouseY) {
        int mx = mouseX - guiLeft;
        int my = mouseY - guiTop;

        hoveredSlot = -1;
        for (int i = 0; i < 6; i++) {
            int sx = slotPositions[i][0];
            int sy = slotPositions[i][1];
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
                hoveredSlot = i;
                break;
            }
        }

        hoveredModuleIndex = -1;
        if (mx >= MODULE_PANEL_X + 2 && mx < MODULE_PANEL_X + MODULE_PANEL_WIDTH - 2 &&
                my >= MODULE_PANEL_Y + 18 && my < MODULE_PANEL_Y + MODULE_PANEL_HEIGHT - 2) {
            int relY = my - (MODULE_PANEL_Y + 18);
            int index = scrollOffset + relY / MODULE_ENTRY_HEIGHT;
            if (index >= 0 && index < installedModules.size()) {
                hoveredModuleIndex = index;
            }
        }
    }

    private void drawMainBackground() {
        int x = guiLeft;
        int y = guiTop;
        int w = GUI_WIDTH;
        int h = GUI_HEIGHT;

        drawGradientRect(x - 2, y - 2, x + w + 2, y + h + 2, COLOR_ACCENT_DIM, COLOR_ACCENT_DIM);
        drawGradientRect(x, y, x + w, y + h, COLOR_BG_DARK, COLOR_BG_LIGHT);
        drawHollowRect(x + 1, y + 1, w - 2, h - 2, COLOR_ACCENT_DIM);
    }

    private void drawTitleBar() {
        int x = guiLeft;
        int y = guiTop;

        drawGradientRect(x + 2, y + 2, x + GUI_WIDTH - 2, y + 20, COLOR_PANEL_BG, COLOR_BG_DARK);
        drawHLine(x + 2, x + GUI_WIDTH - 3, y + 20, COLOR_ACCENT);

        GlStateManager.enableTexture2D();
        String title = "Synergy Linking Station";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, x + (GUI_WIDTH - titleWidth) / 2, y + 7, COLOR_TEXT_TITLE);
        GlStateManager.disableTexture2D();
    }

    private void drawModulePanel() {
        int x = guiLeft + MODULE_PANEL_X;
        int y = guiTop + MODULE_PANEL_Y;
        int w = MODULE_PANEL_WIDTH;
        int h = MODULE_PANEL_HEIGHT;

        drawGradientRect(x, y, x + w, y + h, COLOR_PANEL_BG, COLOR_BG_DARK);
        drawHollowRect(x, y, w, h, COLOR_ACCENT_DIM);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + 16, COLOR_ACCENT_DIM, COLOR_PANEL_BG);

        GlStateManager.enableTexture2D();
        fontRenderer.drawString("Modules", x + 4, y + 4, COLOR_TEXT_TITLE);
        String countStr = installedModules.size() + "";
        fontRenderer.drawString(countStr, x + w - 8 - fontRenderer.getStringWidth(countStr), y + 4, COLOR_TEXT_DIM);
        GlStateManager.disableTexture2D();

        drawModuleList(x, y);
        drawScrollbar(x + w - 6, y + 18, 4, h - 20);
    }

    private void drawModuleList(int panelX, int panelY) {
        int x = panelX + 2;
        int y = panelY + 18;
        int entryWidth = MODULE_PANEL_WIDTH - 10;

        GlStateManager.enableTexture2D();

        for (int i = 0; i < MAX_VISIBLE_MODULES && scrollOffset + i < installedModules.size(); i++) {
            ModuleEntry entry = installedModules.get(scrollOffset + i);
            int entryY = y + i * MODULE_ENTRY_HEIGHT;
            int actualIndex = scrollOffset + i;

            boolean inLink = tileEntity.containsModule(entry.moduleId);
            boolean isHovered = (actualIndex == hoveredModuleIndex) && !inLink;

            GlStateManager.disableTexture2D();

            int bgColor;
            if (inLink) {
                bgColor = 0x60008800;
            } else if (isHovered) {
                bgColor = 0x60404080;
            } else {
                bgColor = 0x40000000;
            }
            drawRect(x, entryY, x + entryWidth, entryY + MODULE_ENTRY_HEIGHT - 2, bgColor);

            int statusColor = entry.active ? COLOR_SUCCESS : COLOR_WARNING;
            drawRect(x, entryY, x + 2, entryY + MODULE_ENTRY_HEIGHT - 2, statusColor);

            GlStateManager.enableTexture2D();

            int textColor = inLink ? 0xFF88FF88 : (entry.active ? COLOR_TEXT_NORMAL : COLOR_TEXT_DIM);
            String displayText = entry.displayName;
            if (fontRenderer.getStringWidth(displayText) > entryWidth - 28) {
                while (fontRenderer.getStringWidth(displayText + "..") > entryWidth - 28 && displayText.length() > 1) {
                    displayText = displayText.substring(0, displayText.length() - 1);
                }
                displayText += "..";
            }
            fontRenderer.drawString(displayText, x + 4, entryY + 5, textColor);

            String levelStr = "L" + entry.level;
            fontRenderer.drawString(levelStr, x + entryWidth - fontRenderer.getStringWidth(levelStr) - 2,
                    entryY + 5, 0xFFFFAA00);
        }

        GlStateManager.disableTexture2D();
    }

    private void drawScrollbar(int x, int y, int width, int height) {
        if (installedModules.size() <= MAX_VISIBLE_MODULES) return;

        drawRect(x, y, x + width, y + height, 0x40000000);

        int maxScroll = installedModules.size() - MAX_VISIBLE_MODULES;
        int sliderHeight = Math.max(10, height * MAX_VISIBLE_MODULES / installedModules.size());
        int sliderY = y + (height - sliderHeight) * scrollOffset / maxScroll;

        drawRect(x, sliderY, x + width, sliderY + sliderHeight, COLOR_ACCENT_DIM);
    }

    private void drawLinkArea() {
        int centerX = guiLeft + LINK_CENTER_X;
        int centerY = guiTop + LINK_CENTER_Y;

        drawConnectionLines(centerX, centerY);
        drawCenterCore(centerX, centerY);

        for (int i = 0; i < 6; i++) {
            drawLinkSlot(i);
        }
    }

    private void drawConnectionLines(int centerX, int centerY) {
        List<Integer> occupiedSlots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (!tileEntity.isSlotEmpty(i)) {
                occupiedSlots.add(i);
            }
        }

        if (occupiedSlots.isEmpty()) return;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int slot : occupiedSlots) {
            int sx = guiLeft + slotPositions[slot][0] + SLOT_SIZE / 2;
            int sy = guiTop + slotPositions[slot][1] + SLOT_SIZE / 2;
            drawGlowLine(sx, sy, centerX, centerY, COLOR_ACCENT, 3.0f);
        }

        for (int i = 0; i < occupiedSlots.size(); i++) {
            for (int j = i + 1; j < occupiedSlots.size(); j++) {
                int slot1 = occupiedSlots.get(i);
                int slot2 = occupiedSlots.get(j);

                int x1 = guiLeft + slotPositions[slot1][0] + SLOT_SIZE / 2;
                int y1 = guiTop + slotPositions[slot1][1] + SLOT_SIZE / 2;
                int x2 = guiLeft + slotPositions[slot2][0] + SLOT_SIZE / 2;
                int y2 = guiTop + slotPositions[slot2][1] + SLOT_SIZE / 2;

                drawGlowLine(x1, y1, x2, y2, COLOR_ACCENT_DIM, 1.5f);
            }
        }

        GlStateManager.disableBlend();
    }

    private void drawGlowLine(int x1, int y1, int x2, int y2, int color, float width) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GL11.glLineWidth(width);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GlStateManager.color(r, g, b, 0.8f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y1, 0).endVertex();
        buffer.pos(x2, y2, 0).endVertex();
        tessellator.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawCenterCore(int cx, int cy) {
        boolean activated = tileEntity.isActivated();
        int coreColor = activated ? COLOR_SUCCESS : COLOR_ACCENT_DIM;

        float pulse = (float) (Math.sin(animationTick * 0.1) * 0.3 + 0.7);
        if (activated) {
            drawCircle(cx, cy, 14, applyAlpha(coreColor, pulse * 0.3f));
            drawCircle(cx, cy, 12, applyAlpha(coreColor, pulse * 0.5f));
        }

        drawCircle(cx, cy, 10, coreColor);
        drawCircle(cx, cy, 6, COLOR_BG_DARK);

        if (activated) {
            drawCircle(cx, cy, 3, COLOR_SUCCESS);
        }
    }

    private void drawLinkSlot(int slotIndex) {
        int x = guiLeft + slotPositions[slotIndex][0];
        int y = guiTop + slotPositions[slotIndex][1];

        String moduleId = tileEntity.getModuleInSlot(slotIndex);
        boolean hasModule = moduleId != null && !moduleId.isEmpty();
        boolean isHovered = (slotIndex == hoveredSlot);

        int bgColor;
        if (hasModule) {
            bgColor = isHovered ? 0xFF5050c0 : COLOR_SLOT_FILLED;
        } else {
            bgColor = isHovered ? COLOR_SLOT_HOVER : COLOR_SLOT_EMPTY;
        }

        drawRect(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1,
                hasModule ? COLOR_ACCENT : COLOR_ACCENT_DIM);
        drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawHollowRect(x + 2, y + 2, SLOT_SIZE - 4, SLOT_SIZE - 4,
                hasModule ? 0x60FFFFFF : 0x30FFFFFF);

        GlStateManager.enableTexture2D();

        if (hasModule) {
            String abbr = moduleId.length() > 2 ? moduleId.substring(0, 2).toUpperCase() : moduleId.toUpperCase();
            int textWidth = fontRenderer.getStringWidth(abbr);
            fontRenderer.drawString(abbr, x + (SLOT_SIZE - textWidth) / 2, y + (SLOT_SIZE - 8) / 2, 0xFFFFFFFF);
        } else {
            String num = String.valueOf(slotIndex + 1);
            int textWidth = fontRenderer.getStringWidth(num);
            fontRenderer.drawString(num, x + (SLOT_SIZE - textWidth) / 2, y + (SLOT_SIZE - 8) / 2, COLOR_TEXT_DIM);
        }

        GlStateManager.disableTexture2D();
    }

    private void drawInfoPanel() {
        int x = guiLeft + INFO_PANEL_X;
        int y = guiTop + INFO_PANEL_Y;
        int w = INFO_PANEL_WIDTH;
        int h = INFO_PANEL_HEIGHT;

        drawGradientRect(x, y, x + w, y + h, COLOR_PANEL_BG, COLOR_BG_DARK);
        drawHollowRect(x, y, w, h, COLOR_ACCENT_DIM);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + 14, COLOR_ACCENT_DIM, COLOR_PANEL_BG);

        GlStateManager.enableTexture2D();

        fontRenderer.drawString("Active Synergies", x + 4, y + 3, COLOR_TEXT_TITLE);

        int listY = y + 16;
        if (matchingSynergies.isEmpty()) {
            fontRenderer.drawString("Link 2+ modules", x + 4, listY + 4, COLOR_TEXT_DIM);
        } else {
            for (int i = 0; i < Math.min(2, matchingSynergies.size()); i++) {
                SynergyDefinition synergy = matchingSynergies.get(i);
                String name = synergy.getDisplayName();
                if (fontRenderer.getStringWidth(name) > w - 12) {
                    while (fontRenderer.getStringWidth(name + "..") > w - 12 && name.length() > 1) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "..";
                }
                fontRenderer.drawString("> " + name, x + 4, listY + 4 + i * 10, COLOR_SUCCESS);
            }
            if (matchingSynergies.size() > 2) {
                fontRenderer.drawString("+" + (matchingSynergies.size() - 2) + " more...",
                        x + 4, listY + 24, COLOR_TEXT_DIM);
            }
        }

        GlStateManager.disableTexture2D();
    }

    private void drawTooltips(int mouseX, int mouseY) {
        GlStateManager.enableTexture2D();

        if (hoveredSlot >= 0) {
            String moduleId = tileEntity.getModuleInSlot(hoveredSlot);
            if (moduleId != null && !moduleId.isEmpty()) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7b" + moduleId);
                tooltip.add("\u00a77Right-click to remove");
                drawHoveringText(tooltip, mouseX, mouseY);
            } else {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a77Slot " + (hoveredSlot + 1));
                tooltip.add("\u00a78Drag module here");
                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }

        if (hoveredModuleIndex >= 0 && hoveredModuleIndex < installedModules.size()) {
            ModuleEntry entry = installedModules.get(hoveredModuleIndex);
            if (!tileEntity.containsModule(entry.moduleId)) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7b" + entry.displayName);
                tooltip.add("\u00a77Level: " + entry.level);
                tooltip.add("\u00a77Status: " + (entry.active ? "\u00a7aActive" : "\u00a7cInactive"));
                tooltip.add("\u00a78Click and drag to link");
                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }

        GlStateManager.disableTexture2D();
    }

    // ==================== 输入处理 ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 左键拖拽模块
        if (mouseButton == 0 && hoveredModuleIndex >= 0) {
            ModuleEntry entry = installedModules.get(hoveredModuleIndex);
            if (!tileEntity.containsModule(entry.moduleId)) {
                draggingModule = entry;
            }
        }

        // 右键移除槽位
        if (mouseButton == 1 && hoveredSlot >= 0) {
            if (!tileEntity.isSlotEmpty(hoveredSlot)) {
                String removedModule = tileEntity.getModuleInSlot(hoveredSlot);

                // 发送网络包到服务器
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.CLEAR_SLOT,
                                hoveredSlot
                        )
                );
                // 本地更新
                tileEntity.clearSlot(hoveredSlot);
                updateMatchingSynergies();

                // 播放移除音效
                player.playSound(net.minecraft.init.SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.5f);
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§7[链结] §c移除 §f" + removedModule + " §7从槽位 " + (hoveredSlot + 1)));
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if (draggingModule != null && state == 0) {
            if (hoveredSlot >= 0 && tileEntity.isSlotEmpty(hoveredSlot)) {
                // 发送网络包到服务器
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.SET_SLOT,
                                hoveredSlot,
                                draggingModule.moduleId
                        )
                );
                // 本地更新
                tileEntity.setModuleInSlot(hoveredSlot, draggingModule.moduleId);
                updateMatchingSynergies();

                // 播放反馈音效
                player.playSound(net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 1.2f);

                // 显示反馈消息
                String feedback = "§b[链结] §f" + draggingModule.displayName + " §7-> 槽位 " + (hoveredSlot + 1);
                if (!matchingSynergies.isEmpty()) {
                    feedback += " §a| 激活: " + matchingSynergies.get(0).getDisplayName();
                    if (matchingSynergies.size() > 1) {
                        feedback += " (+" + (matchingSynergies.size() - 1) + ")";
                    }
                }
                player.sendMessage(new net.minecraft.util.text.TextComponentString(feedback));
            }
            draggingModule = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int maxScroll = Math.max(0, installedModules.size() - MAX_VISIBLE_MODULES);
            if (wheel > 0 && scrollOffset > 0) {
                scrollOffset--;
            } else if (wheel < 0 && scrollOffset < maxScroll) {
                scrollOffset++;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
            mc.player.closeScreen();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (player.ticksExisted % 20 == 0) {
            refreshInstalledModules();
        }

        for (GuiButton button : buttonList) {
            if (button.id == BTN_ACTIVATE) {
                button.displayString = tileEntity.isActivated() ? "ON" : "OFF";
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ==================== 绘图工具方法 ====================

    private void drawHollowRect(int x, int y, int width, int height, int color) {
        drawHLine(x, x + width - 1, y, color);
        drawHLine(x, x + width - 1, y + height - 1, color);
        drawVLine(x, y, y + height - 1, color);
        drawVLine(x + width - 1, y, y + height - 1, color);
    }

    private void drawHLine(int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        drawRect(x1, y, x2 + 1, y + 1, color);
    }

    private void drawVLine(int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }
        drawRect(x, y1, x + 1, y2 + 1, color);
    }

    private void drawCircle(int cx, int cy, int radius, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(cx, cy, 0).endVertex();
        for (int i = 0; i <= 32; i++) {
            double angle = 2 * Math.PI * i / 32;
            buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tessellator.draw();
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
