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
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

/**
 * Synergy 链结站 GUI - 多页槽位 + 滚动模块列表
 *
 * ✅ 链结槽位：支持多页翻页（每页6个槽位）
 * ✅ 模块列表：滚动显示
 * ✅ 视觉：全息科技风格
 * ✅ 逻辑：完整保留所有交互功能
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyStation extends GuiScreen {

    // ==================== 颜色常量（全息科技风格） ====================
    private static final int COLOR_BG_DARK = 0xCC050505;
    private static final int COLOR_BG_LIGHT = 0xCC101018;
    private static final int COLOR_PANEL_BG = 0xCC0a1520;
    private static final int COLOR_ACCENT = 0xFF00AAAA;
    private static final int COLOR_ACCENT_BRIGHT = 0xFF00FFFF;
    private static final int COLOR_ACCENT_DIM = 0xFF005555;
    private static final int COLOR_TEXT_TITLE = 0xFF00FFFF;
    private static final int COLOR_TEXT_NORMAL = 0xFFe0e0e0;
    private static final int COLOR_TEXT_DIM = 0xFF808080;
    private static final int COLOR_SLOT_EMPTY = 0xFF1a1a2a;
    private static final int COLOR_SLOT_FILLED = 0xFF2a4060;
    private static final int COLOR_SLOT_HOVER = 0xFF3a5080;
    private static final int COLOR_SUCCESS = 0xFF00ff88;
    private static final int COLOR_WARNING = 0xFFffaa00;
    private static final int COLOR_GLOW = 0x40006080;

    // ==================== 布局常量 ====================
    private static final int GUI_WIDTH = 300;
    // 🔥 修改: 增加总高度，给底部腾出空间 (原 200)
    private static final int GUI_HEIGHT = 215;

    // 模块列表区域（左侧，滚动）
    private static final int MODULE_PANEL_X = 8;
    private static final int MODULE_PANEL_Y = 25;
    private static final int MODULE_PANEL_WIDTH = 100;
    private static final int MODULE_PANEL_HEIGHT = 145;
    private static final int MODULE_ENTRY_HEIGHT = 18;
    private static final int MAX_VISIBLE_MODULES = 7;

    // 链结区域（右侧，翻页）
    private static final int LINK_PANEL_X = 115;
    private static final int LINK_PANEL_Y = 25;
    private static final int LINK_PANEL_WIDTH = 177;
    // 🔥 修改: 拉高面板高度，容纳六边形 (原 110)
    private static final int LINK_PANEL_HEIGHT = 138;

    private static final int LINK_CENTER_X = 203;  // 微调水平中心
    // 🔥 修改: 中心点大幅下移，防止顶到标题栏 (原 75)
    private static final int LINK_CENTER_Y = 94;
    // 🔥 修改: 半径微调收缩，防止挡住底部页码 (原 40)
    private static final int LINK_SLOT_RADIUS = 36;
    private static final int SLOT_SIZE = 24;
    private static final int SLOTS_PER_PAGE = 6;

    // Synergy 信息区域
    private static final int INFO_PANEL_X = 115;
    // 🔥 修改: 顺延下移 (原 140)
    private static final int INFO_PANEL_Y = 168;
    private static final int INFO_PANEL_WIDTH = 177;
    private static final int INFO_PANEL_HEIGHT = 32;

    // 按钮 ID
    private static final int BTN_CLEAR_ALL = 0;
    private static final int BTN_ACTIVATE = 1;
    private static final int BTN_PREV_PAGE = 2;
    private static final int BTN_NEXT_PAGE = 3;

    // ==================== 翻页配置 ====================
    /** 总页数（可配置） */
    private static final int MAX_PAGES = 4;
    /** 总槽位数 */
    private static final int TOTAL_SLOTS = MAX_PAGES * SLOTS_PER_PAGE;

    // ==================== 状态 ====================
    private final TileEntitySynergyStation tileEntity;
    private final EntityPlayer player;

    private int guiLeft;
    private int guiTop;

    // 模块列表（滚动）
    private List<ModuleEntry> installedModules = new ArrayList<>();
    private int scrollOffset = 0;

    // 链结槽位（翻页）
    private int currentPage = 0;
    private final int[][] slotPositions = new int[SLOTS_PER_PAGE][2];

    // 交互状态
    private ModuleEntry draggingModule = null;
    private int hoveredSlot = -1;  // 当前页的槽位索引 (0-5)
    private int hoveredModuleIndex = -1;

    private List<SynergyDefinition> matchingSynergies = new ArrayList<>();
    private float animationTick = 0;

    // 翻页按钮
    private GuiButton btnPrevPage;
    private GuiButton btnNextPage;

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

    /**
     * 计算当前页六边形槽位位置
     */
    private void calculateSlotPositions() {
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 2;
            slotPositions[i][0] = (int) (LINK_CENTER_X + Math.cos(angle) * LINK_SLOT_RADIUS - SLOT_SIZE / 2);
            slotPositions[i][1] = (int) (LINK_CENTER_Y + Math.sin(angle) * LINK_SLOT_RADIUS - SLOT_SIZE / 2);
        }
    }

    /**
     * 获取实际槽位索引（考虑翻页）
     */
    private int getRealSlotIndex(int pageSlotIndex) {
        return currentPage * SLOTS_PER_PAGE + pageSlotIndex;
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

        this.buttonList.clear();

        // 清空按钮
        this.buttonList.add(new GuiButton(BTN_CLEAR_ALL,
                guiLeft + 8, guiTop + GUI_HEIGHT - 24, 45, 16, "Clear"));

        // 激活按钮
        this.buttonList.add(new GuiButton(BTN_ACTIVATE,
                guiLeft + 58, guiTop + GUI_HEIGHT - 24, 50, 16,
                tileEntity.isActivated() ? "ON" : "OFF"));

        // 翻页按钮
        this.btnPrevPage = new GuiButton(BTN_PREV_PAGE,
                guiLeft + LINK_PANEL_X + 5, guiTop + LINK_PANEL_Y + LINK_PANEL_HEIGHT - 18, 20, 16, "<");
        this.btnNextPage = new GuiButton(BTN_NEXT_PAGE,
                guiLeft + LINK_PANEL_X + LINK_PANEL_WIDTH - 25, guiTop + LINK_PANEL_Y + LINK_PANEL_HEIGHT - 18, 20, 16, ">");

        this.buttonList.add(btnPrevPage);
        this.buttonList.add(btnNextPage);

        updatePageButtons();
        Keyboard.enableRepeatEvents(true);
    }

    /**
     * 更新翻页按钮状态
     */
    private void updatePageButtons() {
        btnPrevPage.enabled = currentPage > 0;
        btnNextPage.enabled = currentPage < MAX_PAGES - 1;
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
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.CLEAR_ALL
                        )
                );
                tileEntity.clearAllSlots();
                updateMatchingSynergies();
                player.playSound(net.minecraft.init.SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.2f);
                break;

            case BTN_ACTIVATE:
                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.TOGGLE_ACTIVE
                        )
                );
                boolean wasActivated = tileEntity.isActivated();
                button.displayString = wasActivated ? "OFF" : "ON";

                if (!wasActivated) {
                    player.playSound(net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
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
                    player.playSound(net.minecraft.init.SoundEvents.BLOCK_LEVER_CLICK, 0.5f, 0.5f);
                    player.sendMessage(new net.minecraft.util.text.TextComponentString("§c[链结站关闭]"));
                }
                break;

            case BTN_PREV_PAGE:
                if (currentPage > 0) {
                    currentPage--;
                    updatePageButtons();
                    player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
                break;

            case BTN_NEXT_PAGE:
                if (currentPage < MAX_PAGES - 1) {
                    currentPage++;
                    updatePageButtons();
                    player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        animationTick += partialTicks;
        updateHoverState(mouseX, mouseY);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableTexture2D();

        drawMainBackground();
        drawTitleBar();
        drawModulePanel();
        drawLinkPanel();
        drawInfoPanel();

        GlStateManager.enableTexture2D();

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (draggingModule != null) {
            drawDraggingModule(mouseX, mouseY);
        }

        drawTooltips(mouseX, mouseY);
    }

    private void updateHoverState(int mouseX, int mouseY) {
        int mx = mouseX - guiLeft;
        int my = mouseY - guiTop;

        // 检查链结槽位悬停（当前页）
        hoveredSlot = -1;
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int sx = slotPositions[i][0];
            int sy = slotPositions[i][1];
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
                hoveredSlot = i;
                break;
            }
        }

        // 检查模块列表悬停
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

    // ==================== 绘制方法 ====================

    private void drawMainBackground() {
        int x = guiLeft;
        int y = guiTop;
        int w = GUI_WIDTH;
        int h = GUI_HEIGHT;

        drawGradientRect(x, y, x + w, y + h, COLOR_BG_DARK, COLOR_BG_LIGHT);
        drawTechBorder(x, y, w, h, COLOR_ACCENT);

        GlStateManager.enableBlend();
        for (int i = y + 4; i < y + h - 4; i += 4) {
            drawRect(x + 4, i, x + w - 4, i + 1, COLOR_GLOW);
        }
    }

    private void drawTechBorder(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y, x + 1, y + height, color);
        drawRect(x + width - 1, y, x + width, y + height, color);

        drawRect(x, y, x + 8, y + 2, color);
        drawRect(x, y, x + 2, y + 8, color);
        drawRect(x + width - 8, y, x + width, y + 2, color);
        drawRect(x + width - 2, y, x + width, y + 8, color);
        drawRect(x, y + height - 2, x + 8, y + height, color);
        drawRect(x, y + height - 8, x + 2, y + height, color);
        drawRect(x + width - 8, y + height - 2, x + width, y + height, color);
        drawRect(x + width - 2, y + height - 8, x + width, y + height, color);
    }

    private void drawTechPanel(int x, int y, int w, int h, int accentColor) {
        drawGradientRect(x, y, x + w, y + h, COLOR_PANEL_BG, COLOR_BG_DARK);
        drawTechBorder(x, y, w, h, accentColor);
    }

    private void drawTitleBar() {
        int x = guiLeft;
        int y = guiTop;

        drawGradientRect(x + 2, y + 2, x + GUI_WIDTH - 2, y + 20, 0x80005050, COLOR_PANEL_BG);
        drawHLine(x + 2, x + GUI_WIDTH - 3, y + 20, COLOR_ACCENT);

        GlStateManager.enableTexture2D();
        String title = "SYNERGY LINKING STATION";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawStringWithShadow(title, x + (GUI_WIDTH - titleWidth) / 2, y + 7, COLOR_TEXT_TITLE);
        GlStateManager.disableTexture2D();
    }

    // ==================== 模块列表面板（滚动） ====================

    private void drawModulePanel() {
        int x = guiLeft + MODULE_PANEL_X;
        int y = guiTop + MODULE_PANEL_Y;
        int w = MODULE_PANEL_WIDTH;
        int h = MODULE_PANEL_HEIGHT;

        drawTechPanel(x, y, w, h, COLOR_ACCENT_DIM);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + 16, COLOR_ACCENT_DIM, COLOR_PANEL_BG);

        GlStateManager.enableTexture2D();
        fontRenderer.drawStringWithShadow("MODULES", x + 4, y + 4, COLOR_TEXT_TITLE);
        String countStr = String.valueOf(installedModules.size());
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
                bgColor = 0x60006030;
            } else if (isHovered) {
                bgColor = 0x60005050;
            } else {
                bgColor = 0x40000000;
            }
            drawRect(x, entryY, x + entryWidth, entryY + MODULE_ENTRY_HEIGHT - 2, bgColor);

            int statusColor = entry.active ? COLOR_SUCCESS : COLOR_WARNING;
            drawRect(x, entryY, x + 2, entryY + MODULE_ENTRY_HEIGHT - 2, statusColor);
            drawRect(x, entryY + MODULE_ENTRY_HEIGHT - 3, x + entryWidth, entryY + MODULE_ENTRY_HEIGHT - 2,
                    inLink ? COLOR_SUCCESS : COLOR_ACCENT_DIM);

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
                    entryY + 5, COLOR_WARNING);
        }

        GlStateManager.disableTexture2D();
    }

    private void drawScrollbar(int x, int y, int width, int height) {
        if (installedModules.size() <= MAX_VISIBLE_MODULES) return;

        drawRect(x, y, x + width, y + height, 0x40000000);

        int maxScroll = installedModules.size() - MAX_VISIBLE_MODULES;
        int sliderHeight = Math.max(10, height * MAX_VISIBLE_MODULES / installedModules.size());
        int sliderY = y + (height - sliderHeight) * scrollOffset / maxScroll;

        drawRect(x, sliderY, x + width, sliderY + sliderHeight, COLOR_ACCENT);
    }

    // ==================== 链结槽位面板（翻页） ====================

    private void drawLinkPanel() {
        int x = guiLeft + LINK_PANEL_X;
        int y = guiTop + LINK_PANEL_Y;
        int w = LINK_PANEL_WIDTH;
        int h = LINK_PANEL_HEIGHT;

        drawTechPanel(x, y, w, h, COLOR_ACCENT_DIM);

        // 标题栏
        drawGradientRect(x + 1, y + 1, x + w - 1, y + 16, COLOR_ACCENT_DIM, COLOR_PANEL_BG);

        GlStateManager.enableTexture2D();
        String linkTitle = "链结槽位";
        fontRenderer.drawStringWithShadow(linkTitle, x + 4, y + 4, COLOR_TEXT_TITLE);

        // 页码显示
        String pageStr = String.format("页 %d/%d", currentPage + 1, MAX_PAGES);
        int pageStrWidth = fontRenderer.getStringWidth(pageStr);
        fontRenderer.drawString(pageStr, x + (w - pageStrWidth) / 2, y + h - 14, COLOR_TEXT_DIM);

        // 槽位范围提示
        int startSlot = currentPage * SLOTS_PER_PAGE + 1;
        int endSlot = startSlot + SLOTS_PER_PAGE - 1;
        String slotRange = String.format("槽位 %d-%d", startSlot, endSlot);
        fontRenderer.drawString(slotRange, x + w - fontRenderer.getStringWidth(slotRange) - 4, y + 4, COLOR_TEXT_DIM);
        GlStateManager.disableTexture2D();

        // 绘制链结区域
        drawLinkArea();
    }

    private void drawLinkArea() {
        int centerX = guiLeft + LINK_CENTER_X;
        int centerY = guiTop + LINK_CENTER_Y;

        drawConnectionLines(centerX, centerY);
        drawCenterCore(centerX, centerY);

        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            drawLinkSlot(i);
        }
    }

    private void drawConnectionLines(int centerX, int centerY) {
        List<Integer> occupiedSlotsOnPage = new ArrayList<>();
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int realIndex = getRealSlotIndex(i);
            if (!tileEntity.isSlotEmpty(realIndex)) {
                occupiedSlotsOnPage.add(i);
            }
        }

        if (occupiedSlotsOnPage.isEmpty()) return;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int pageSlot : occupiedSlotsOnPage) {
            int sx = guiLeft + slotPositions[pageSlot][0] + SLOT_SIZE / 2;
            int sy = guiTop + slotPositions[pageSlot][1] + SLOT_SIZE / 2;
            drawGlowLine(sx, sy, centerX, centerY, COLOR_ACCENT_BRIGHT, 3.0f);
        }

        for (int i = 0; i < occupiedSlotsOnPage.size(); i++) {
            for (int j = i + 1; j < occupiedSlotsOnPage.size(); j++) {
                int slot1 = occupiedSlotsOnPage.get(i);
                int slot2 = occupiedSlotsOnPage.get(j);

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
        int coreColor = activated ? COLOR_SUCCESS : COLOR_ACCENT;

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

        // 显示当前页码在核心中
        GlStateManager.enableTexture2D();
        String pageNum = String.valueOf(currentPage + 1);
        int numWidth = fontRenderer.getStringWidth(pageNum);
        fontRenderer.drawString(pageNum, cx - numWidth / 2, cy - 4, COLOR_TEXT_DIM);
        GlStateManager.disableTexture2D();
    }

    private void drawLinkSlot(int pageSlotIndex) {
        int x = guiLeft + slotPositions[pageSlotIndex][0];
        int y = guiTop + slotPositions[pageSlotIndex][1];

        int realIndex = getRealSlotIndex(pageSlotIndex);
        String moduleId = tileEntity.getModuleInSlot(realIndex);
        boolean hasModule = moduleId != null && !moduleId.isEmpty();
        boolean isHovered = (pageSlotIndex == hoveredSlot);

        int bgColor;
        if (hasModule) {
            bgColor = isHovered ? 0xFF3a6080 : COLOR_SLOT_FILLED;
        } else {
            bgColor = isHovered ? COLOR_SLOT_HOVER : COLOR_SLOT_EMPTY;
        }

        drawRect(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1,
                hasModule ? COLOR_ACCENT_BRIGHT : COLOR_ACCENT_DIM);
        drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawHollowRect(x + 2, y + 2, SLOT_SIZE - 4, SLOT_SIZE - 4,
                hasModule ? 0x60FFFFFF : 0x30FFFFFF);

        GlStateManager.enableTexture2D();

        if (hasModule) {
            String abbr = moduleId.length() > 2 ? moduleId.substring(0, 2).toUpperCase() : moduleId.toUpperCase();
            int textWidth = fontRenderer.getStringWidth(abbr);
            fontRenderer.drawStringWithShadow(abbr, x + (SLOT_SIZE - textWidth) / 2, y + (SLOT_SIZE - 8) / 2, 0xFFFFFFFF);
        } else {
            // 显示实际槽位编号
            String num = String.valueOf(realIndex + 1);
            int textWidth = fontRenderer.getStringWidth(num);
            fontRenderer.drawString(num, x + (SLOT_SIZE - textWidth) / 2, y + (SLOT_SIZE - 8) / 2, COLOR_TEXT_DIM);
        }

        GlStateManager.disableTexture2D();
    }

    // ==================== 信息面板 ====================

    private void drawInfoPanel() {
        int x = guiLeft + INFO_PANEL_X;
        int y = guiTop + INFO_PANEL_Y;
        int w = INFO_PANEL_WIDTH;
        int h = INFO_PANEL_HEIGHT;

        drawTechPanel(x, y, w, h, COLOR_ACCENT_DIM);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + 12, COLOR_ACCENT_DIM, COLOR_PANEL_BG);

        GlStateManager.enableTexture2D();

        fontRenderer.drawStringWithShadow("模块联动", x + 4, y + 2, COLOR_TEXT_TITLE);

        // 显示已链结模块总数
        int totalLinked = tileEntity.getLinkedModuleCount();
        String linkedStr = totalLinked + " 链结";
        fontRenderer.drawString(linkedStr, x + w - fontRenderer.getStringWidth(linkedStr) - 4, y + 2, COLOR_TEXT_DIM);

        int listY = y + 14;
        if (matchingSynergies.isEmpty()) {
            fontRenderer.drawString("链结两个以上模块触发联动", x + 4, listY + 2, COLOR_TEXT_DIM);
        } else {
            String synergyName = matchingSynergies.get(0).getDisplayName();
            if (fontRenderer.getStringWidth(synergyName) > w - 30) {
                while (fontRenderer.getStringWidth(synergyName + "..") > w - 30 && synergyName.length() > 1) {
                    synergyName = synergyName.substring(0, synergyName.length() - 1);
                }
                synergyName += "..";
            }
            fontRenderer.drawString("> " + synergyName, x + 4, listY + 2, COLOR_SUCCESS);

            if (matchingSynergies.size() > 1) {
                fontRenderer.drawString("+" + (matchingSynergies.size() - 1) + " 更多 ",
                        x + w - fontRenderer.getStringWidth("+" + (matchingSynergies.size() - 1) + " 更多") - 4,
                        listY + 2, COLOR_TEXT_DIM);
            }
        }

        GlStateManager.disableTexture2D();
    }

    // ==================== 拖拽与提示 ====================

    private void drawDraggingModule(int mouseX, int mouseY) {
        int labelWidth = fontRenderer.getStringWidth(draggingModule.displayName) + 8;
        GlStateManager.disableTexture2D();
        drawRect(mouseX - 2, mouseY - 2, mouseX + labelWidth, mouseY + 12, 0xE0000000);
        drawTechBorder(mouseX - 2, mouseY - 2, labelWidth + 2, 14, COLOR_ACCENT_BRIGHT);
        GlStateManager.enableTexture2D();
        fontRenderer.drawStringWithShadow(draggingModule.displayName, mouseX + 2, mouseY + 2, COLOR_TEXT_TITLE);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        GlStateManager.enableTexture2D();

        if (hoveredSlot >= 0) {
            int realIndex = getRealSlotIndex(hoveredSlot);
            String moduleId = tileEntity.getModuleInSlot(realIndex);
            if (moduleId != null && !moduleId.isEmpty()) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7b" + moduleId);
                tooltip.add("\u00a77Slot " + (realIndex + 1));
                tooltip.add("\u00a78Right-click to remove");
                drawHoveringText(tooltip, mouseX, mouseY);
            } else {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a77Slot " + (realIndex + 1));
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
            int realIndex = getRealSlotIndex(hoveredSlot);
            if (!tileEntity.isSlotEmpty(realIndex)) {
                String removedModule = tileEntity.getModuleInSlot(realIndex);

                PacketHandler.INSTANCE.sendToServer(
                        new PacketSynergyStationAction(
                                tileEntity.getPos(),
                                PacketSynergyStationAction.ActionType.CLEAR_SLOT,
                                realIndex
                        )
                );
                tileEntity.clearSlot(realIndex);
                updateMatchingSynergies();

                player.playSound(net.minecraft.init.SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.5f);
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§7[链结] §c移除 §f" + removedModule + " §7从槽位 " + (realIndex + 1)));
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if (draggingModule != null && state == 0) {
            if (hoveredSlot >= 0) {
                int realIndex = getRealSlotIndex(hoveredSlot);
                if (tileEntity.isSlotEmpty(realIndex)) {
                    PacketHandler.INSTANCE.sendToServer(
                            new PacketSynergyStationAction(
                                    tileEntity.getPos(),
                                    PacketSynergyStationAction.ActionType.SET_SLOT,
                                    realIndex,
                                    draggingModule.moduleId
                            )
                    );
                    tileEntity.setModuleInSlot(realIndex, draggingModule.moduleId);
                    updateMatchingSynergies();

                    player.playSound(net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 1.2f);

                    String feedback = "§b[链结] §f" + draggingModule.displayName + " §7-> 槽位 " + (realIndex + 1);
                    if (!matchingSynergies.isEmpty()) {
                        feedback += " §a| 激活: " + matchingSynergies.get(0).getDisplayName();
                        if (matchingSynergies.size() > 1) {
                            feedback += " (+" + (matchingSynergies.size() - 1) + ")";
                        }
                    }
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(feedback));
                }
            }
            draggingModule = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            // 只在模块列表区域滚动
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth - guiLeft;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1 - guiTop;

            if (mx >= MODULE_PANEL_X && mx < MODULE_PANEL_X + MODULE_PANEL_WIDTH &&
                    my >= MODULE_PANEL_Y && my < MODULE_PANEL_Y + MODULE_PANEL_HEIGHT) {
                int maxScroll = Math.max(0, installedModules.size() - MAX_VISIBLE_MODULES);
                if (wheel > 0 && scrollOffset > 0) {
                    scrollOffset--;
                } else if (wheel < 0 && scrollOffset < maxScroll) {
                    scrollOffset++;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
            mc.player.closeScreen();
        }
        // 快捷键翻页
        if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_A) {
            if (currentPage > 0) {
                currentPage--;
                updatePageButtons();
            }
        }
        if (keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_D) {
            if (currentPage < MAX_PAGES - 1) {
                currentPage++;
                updatePageButtons();
            }
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
        if (x2 < x1) { int temp = x1; x1 = x2; x2 = temp; }
        drawRect(x1, y, x2 + 1, y + 1, color);
    }

    private void drawVLine(int x, int y1, int y2, int color) {
        if (y2 < y1) { int temp = y1; y1 = y2; y2 = temp; }
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