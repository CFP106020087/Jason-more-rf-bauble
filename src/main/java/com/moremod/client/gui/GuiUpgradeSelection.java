package com.moremod.client.gui;

import com.moremod.item.UpgradeType;
import com.moremod.network.NetworkHandler;
import com.moremod.network.PacketUpgradeSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.*;

/**
 * 升级选择GUI（重绘修正版）
 * 修复：
 * - 背景缺纹理时不再显示紫黑格，自动退回渐变背景
 * - 调整提示条/分类标签布局，避免文字重叠
 * - 标签与升级项文字根据宽度截断并加省略号
 */
@SideOnly(Side.CLIENT)
public class GuiUpgradeSelection extends GuiScreen {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/upgrade_selector.png");

    // 尺寸与布局
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 220;

    // 顶部提示条与标签区（提高间距）
    private static final int TITLE_BAR_HEIGHT = 18;
    private static final int HINT_BAR_TOP = 22;   // 提示条顶部
    private static final int HINT_BAR_BOTTOM = 42;// 提示条底部
    private static final int TABS_Y = 46;         // 分类标签Y（原来太靠上会和提示重叠）

    // 列表区
    private static final int LIST_MARGIN_X = 14;
    private static final int LIST_MARGIN_TOP = 70; // 列表顶部（下移，避开标签）
    private static final int CARD_HEIGHT = 22;
    private static final int CARD_GAP_Y = 4;
    private static final int CARD_GAP_X = 10;
    private static final int LIST_COLS = 2;
    private static final int LIST_ROWS = 4; // 每页显示 2x4 = 8 项
    private static final int UPGRADES_PER_PAGE = LIST_COLS * LIST_ROWS;
    private static final int SCROLLBAR_WIDTH = 6;

    // 按钮ID
    private static final int ID_CONFIRM = 100;
    private static final int ID_CANCEL = 101;
    private static final int ID_SCROLL_UP = 300;
    private static final int ID_SCROLL_DOWN = 301;

    private final ItemStack selectorStack;
    private final NBTTagCompound selectorNBT;

    // 状态
    private UpgradeType.UpgradeCategory currentCategory = UpgradeType.UpgradeCategory.ENERGY;
    private final Map<UpgradeType.UpgradeCategory, List<UpgradeType>> availableUpgrades = new HashMap<>();
    private final Map<UpgradeType, Boolean> selectedUpgrades = new HashMap<>();
    private final Map<UpgradeType.UpgradeCategory, Integer> selectionCounts = new HashMap<>();

    // 控件
    private GuiButton confirmButton;
    private GuiButton cancelButton;
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;
    private final List<CategoryTabButton> categoryTabs = new ArrayList<>();
    private final List<UpgradeButton> upgradeButtons = new ArrayList<>();

    // 滚动
    private int scrollOffset = 0;

    public GuiUpgradeSelection(ItemStack stack) {
        this.selectorStack = stack;
        this.selectorNBT = stack.getTagCompound();

        if (selectorNBT != null) {
            loadAvailableUpgrades();
            loadCurrentSelections();
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        categoryTabs.clear();
        upgradeButtons.clear();
        buttonList.clear();

        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;

        // 分类标签（动态宽度，避免文字重叠）
        int tabX = guiLeft + 10;
        int tabY = guiTop + TABS_Y;
        int idx = 0;
        int spacing = 6;
        for (UpgradeType.UpgradeCategory category : UpgradeType.UpgradeCategory.values()) {
            List<UpgradeType> list = availableUpgrades.get(category);
            if (list != null && !list.isEmpty()) {
                String text = category.getName();
                int textW = fontRenderer.getStringWidth(text);
                int tabW = Math.max(54, textW + 14); // 14像素内边距

                CategoryTabButton tab = new CategoryTabButton(
                        10 + idx,
                        tabX,
                        tabY,
                        tabW,
                        18,
                        category);
                tab.selected = (category == currentCategory);
                categoryTabs.add(tab);
                buttonList.add(tab);

                tabX += tabW + spacing;
                idx++;
            }
        }

        // 列表与滚动按钮
        int listLeft = guiLeft + LIST_MARGIN_X;
        int listTop = guiTop + LIST_MARGIN_TOP;
        int listW = GUI_WIDTH - LIST_MARGIN_X * 2;

        scrollUpButton = new SmallIconButton(ID_SCROLL_UP, listLeft + listW - SCROLLBAR_WIDTH - 2, listTop - 10, 10, 10, "▲");
        scrollDownButton = new SmallIconButton(ID_SCROLL_DOWN, listLeft + listW - SCROLLBAR_WIDTH - 2, listTop + getListPixelHeight() + 2, 10, 10, "▼");
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);

        // 确认/取消
        confirmButton = new GuiButton(ID_CONFIRM, guiLeft + GUI_WIDTH - 120, guiTop + GUI_HEIGHT - 26, 60, 18, "确认");
        cancelButton = new GuiButton(ID_CANCEL, guiLeft + GUI_WIDTH - 58, guiTop + GUI_HEIGHT - 26, 48, 18, "取消");
        buttonList.add(confirmButton);
        buttonList.add(cancelButton);

        rebuildUpgradeButtons();
        updateConfirmButton();
        updateScrollButtonsEnabled();
    }

    private int getListPixelHeight() {
        return LIST_ROWS * CARD_HEIGHT + (LIST_ROWS - 1) * CARD_GAP_Y;
    }

    /**
     * 重新生成当前分类的升级按钮（双列卡片）
     */
    private void rebuildUpgradeButtons() {
        // 清理旧按钮
        for (UpgradeButton btn : upgradeButtons) {
            buttonList.remove(btn);
        }
        upgradeButtons.clear();

        List<UpgradeType> list = availableUpgrades.get(currentCategory);
        if (list == null || list.isEmpty()) return;

        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;

        int listLeft = guiLeft + LIST_MARGIN_X;
        int listTop = guiTop + LIST_MARGIN_TOP;
        int listW = GUI_WIDTH - LIST_MARGIN_X * 2 - SCROLLBAR_WIDTH - 4; // 留出滚动条空间

        int colW = (listW - CARD_GAP_X) / LIST_COLS;

        int start = scrollOffset;
        int end = Math.min(start + UPGRADES_PER_PAGE, list.size());

        int idx = 0;
        for (int i = start; i < end; i++) {
            UpgradeType up = list.get(i);

            int row = idx / LIST_COLS;
            int col = idx % LIST_COLS;

            int x = listLeft + col * (colW + CARD_GAP_X);
            int y = listTop + row * (CARD_HEIGHT + CARD_GAP_Y);

            UpgradeButton btn = new UpgradeButton(
                    200 + idx,
                    x, y,
                    colW, CARD_HEIGHT,
                    up,
                    selectedUpgrades.getOrDefault(up, false)
            );
            upgradeButtons.add(btn);
            buttonList.add(btn);

            idx++;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);

        if (button instanceof CategoryTabButton) {
            CategoryTabButton tab = (CategoryTabButton) button;
            currentCategory = tab.category;
            for (CategoryTabButton t : categoryTabs) t.selected = (t == tab);
            scrollOffset = 0;
            rebuildUpgradeButtons();
            updateScrollButtonsEnabled();
            updateConfirmButton();
            return;
        }

        if (button instanceof UpgradeButton) {
            UpgradeButton ub = (UpgradeButton) button;
            if (canToggleUpgrade(ub.upgrade)) {
                boolean newState = !selectedUpgrades.getOrDefault(ub.upgrade, false);
                selectedUpgrades.put(ub.upgrade, newState);
                ub.selected = newState;
                ub.updateDisplayString();
                updateSelectionCounts();
                updateConfirmButton();
                mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5F, newState ? 1.2F : 0.8F);
            } else {
                mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND, 0.3F, 1.5F);
                mc.player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "已达该类别上限"), true);
            }
            return;
        }

        if (button.id == ID_SCROLL_UP) {
            scroll(-1);
            return;
        }
        if (button.id == ID_SCROLL_DOWN) {
            scroll(+1);
            return;
        }

        if (button.id == ID_CONFIRM) {
            try {
                if (isSelectionValid()) {
                    sendSelectionsToServer();
                    mc.displayGuiScreen(null);
                } else {
                    mc.player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "请完成选择：能源类2个，其他各1个"));
                }
            } catch (Exception e) {
                System.err.println("[moremod] GUI确认错误: " + e.getMessage());
                e.printStackTrace();
                mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "发生错误: " + e.getMessage()));
            }
            return;
        }

        if (button.id == ID_CANCEL) {
            mc.displayGuiScreen(null);
        }
    }

    private void scroll(int delta) {
        List<UpgradeType> list = availableUpgrades.get(currentCategory);
        if (list == null) return;
        int maxScroll = Math.max(0, list.size() - UPGRADES_PER_PAGE);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
        rebuildUpgradeButtons();
        updateScrollButtonsEnabled();
    }

    private void updateScrollButtonsEnabled() {
        List<UpgradeType> list = availableUpgrades.get(currentCategory);
        boolean has = list != null && list.size() > UPGRADES_PER_PAGE;
        int maxScroll = has ? Math.max(0, list.size() - UPGRADES_PER_PAGE) : 0;
        if (scrollUpButton != null) scrollUpButton.enabled = has && scrollOffset > 0;
        if (scrollDownButton != null) scrollDownButton.enabled = has && scrollOffset < maxScroll;
    }

    private boolean canToggleUpgrade(UpgradeType upgrade) {
        // 已选择则允许取消
        if (selectedUpgrades.getOrDefault(upgrade, false)) return true;

        UpgradeType.UpgradeCategory cat = upgrade.getCategory();
        int current = selectionCounts.getOrDefault(cat, 0);
        int limit = (cat == UpgradeType.UpgradeCategory.ENERGY) ? 2 : 1;
        return current < limit;
    }

    private void updateSelectionCounts() {
        selectionCounts.clear();
        for (Map.Entry<UpgradeType, Boolean> e : selectedUpgrades.entrySet()) {
            if (e.getValue()) {
                UpgradeType.UpgradeCategory cat = e.getKey().getCategory();
                selectionCounts.merge(cat, 1, Integer::sum);
            }
        }
    }

    private boolean isSelectionValid() {
        int energy = selectionCounts.getOrDefault(UpgradeType.UpgradeCategory.ENERGY, 0);
        if (energy != 2) return false;

        for (UpgradeType.UpgradeCategory cat : UpgradeType.UpgradeCategory.values()) {
            if (cat == UpgradeType.UpgradeCategory.ENERGY || cat == UpgradeType.UpgradeCategory.PACKAGE) continue;

            List<UpgradeType> list = availableUpgrades.get(cat);
            if (list != null && !list.isEmpty()) {
                int cnt = selectionCounts.getOrDefault(cat, 0);
                if (cnt != 1) return false;
            }
        }
        return true;
    }

    private void updateConfirmButton() {
        boolean ok = isSelectionValid();
        if (confirmButton != null) {
            confirmButton.enabled = ok;
            confirmButton.displayString = ok ? "确认" : "未完成";
        }
    }

    // ==== 绘制 ====

    private boolean textureExists(ResourceLocation loc) {
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(loc);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;

        // 背板：仅当纹理真实存在才绘制；否则用渐变背景，避免紫黑格
        if (textureExists(BACKGROUND_TEXTURE)) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
            drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        } else {
            drawGradientRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010, 0xD0030303);
            drawGradientRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + TITLE_BAR_HEIGHT, 0xFF202028, 0xFF101014);
        }

        // 标题
        String title = "升级选择器";
        drawCenteredString(fontRenderer, title, guiLeft + GUI_WIDTH / 2, guiTop + 6, 0xFFFFFF);

        // 顶部提示条（不与标签重叠）
        drawGradientRect(guiLeft + 8, guiTop + HINT_BAR_TOP, guiLeft + GUI_WIDTH - 8, guiTop + HINT_BAR_BOTTOM, 0x40222222, 0x40222222);
        String hint = TextFormatting.GRAY + "能源类选 2 个，其它每类各选 1 个";
        fontRenderer.drawString(hint, guiLeft + 12, guiTop + HINT_BAR_TOP + 6, 0xCCCCCC);

        // 列表边框（先画框再画按钮，避免按钮上出现后画的线条）
        int listLeft = guiLeft + LIST_MARGIN_X;
        int listTop = guiTop + LIST_MARGIN_TOP;
        int listW = GUI_WIDTH - LIST_MARGIN_X * 2 - SCROLLBAR_WIDTH - 4;
        int listH = getListPixelHeight();
        drawRect(listLeft - 2, listTop - 2, listLeft + listW + 2, listTop + listH + 2, 0x40000000);

        // 滚动条槽与滑块（先画槽，滑块最后画在按钮之后也可）
        int barX = listLeft + listW + 4;
        drawRect(barX, listTop, barX + SCROLLBAR_WIDTH, listTop + listH, 0x30000000);

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 滑块
        drawScrollbarKnob(barX, listTop, listH);

        // 选择进度（底部信息条）
        drawFooterProgress(guiLeft, guiTop);

        // Tooltip
        for (GuiButton button : buttonList) {
            if (button instanceof UpgradeButton && button.isMouseOver()) {
                UpgradeButton ub = (UpgradeButton) button;
                drawUpgradeTooltip(ub.upgrade, mouseX, mouseY);
            }
        }
    }

    private void drawFooterProgress(int guiLeft, int guiTop) {
        int y = guiTop + GUI_HEIGHT - 44;
        drawGradientRect(guiLeft + 8, y, guiLeft + GUI_WIDTH - 8, y + 16, 0x35222222, 0x35222222);

        int x = guiLeft + 12;
        for (UpgradeType.UpgradeCategory cat : UpgradeType.UpgradeCategory.values()) {
            if (!availableUpgrades.containsKey(cat) || availableUpgrades.get(cat).isEmpty()) continue;
            int max = (cat == UpgradeType.UpgradeCategory.ENERGY) ? 2 : 1;
            int cur = selectionCounts.getOrDefault(cat, 0);
            int color = (cur == max) ? 0x00FF00 : (cur > 0 ? 0xFFFF00 : 0xFF5555);
            String txt = cat.getName() + ": " + cur + "/" + max + "  ";
            fontRenderer.drawString(txt, x, y + 4, color);
            x += fontRenderer.getStringWidth(txt) + 4;
        }
    }

    private void drawScrollbarKnob(int barX, int listTop, int listH) {
        List<UpgradeType> list = availableUpgrades.get(currentCategory);
        if (list == null || list.size() <= UPGRADES_PER_PAGE) return;

        int maxScroll = Math.max(0, list.size() - UPGRADES_PER_PAGE);
        float t = (maxScroll == 0) ? 0f : (scrollOffset / (float) maxScroll);

        int knobH = Math.max(12, (int) (listH * (UPGRADES_PER_PAGE / (float) list.size())));
        int knobY = listTop + (int) ((listH - knobH) * t);

        drawRect(barX, knobY, barX + SCROLLBAR_WIDTH, knobY + knobH, 0xFF666666);
    }

    private void drawUpgradeTooltip(UpgradeType upgrade, int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(upgrade.getColor() + upgrade.getDisplayName());
        tooltip.add(TextFormatting.GRAY + "类别: " + upgrade.getCategory().getName());

        switch (upgrade) {
            case KINETIC_GENERATOR:
                tooltip.add(TextFormatting.AQUA + "移动时产生能量");
                break;
            case SOLAR_GENERATOR:
                tooltip.add(TextFormatting.YELLOW + "阳光下自动充能");
                break;
            case ENERGY_CAPACITY:
                tooltip.add(TextFormatting.GOLD + "增加能量储存上限");
                break;
            case ENERGY_EFFICIENCY:
                tooltip.add(TextFormatting.GREEN + "减少能量消耗");
                break;
            case ARMOR_ENHANCEMENT:
                tooltip.add(TextFormatting.BLUE + "提供额外护甲值");
                break;
            case SPEED_BOOST:
                tooltip.add(TextFormatting.AQUA + "提升移动速度");
                break;
            case YELLOW_SHIELD:
                tooltip.add(TextFormatting.YELLOW + "吸收伤害的能量护盾");
                break;
            case HEALTH_REGEN:
                tooltip.add(TextFormatting.RED + "持续恢复生命值");
                break;
            case DAMAGE_BOOST:
                tooltip.add(TextFormatting.DARK_RED + "增加攻击伤害");
                break;
            case MOVEMENT_SPEED:
                tooltip.add(TextFormatting.AQUA + "增强移动能力");
                break;
            case ORE_VISION:
                tooltip.add(TextFormatting.GOLD + "透视显示矿物");
                break;
            default:
                break;
        }
        drawHoveringText(tooltip, mouseX, mouseY);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) scroll(-1);
            else scroll(+1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter 确认 / Esc 取消
        if (keyCode == 28 /*Enter*/ || keyCode == 156 /*NumEnter*/) {
            if (confirmButton != null && confirmButton.enabled) {
                actionPerformed(confirmButton);
                return;
            }
        }
        if (keyCode == 1 /*Esc*/) {
            actionPerformed(cancelButton);
            return;
        }
        // 上下翻动
        if (keyCode == 200 /*Up*/) { scroll(-1); return; }
        if (keyCode == 208 /*Down*/) { scroll(+1); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ====== 网络/数据 ======

    private void sendSelectionsToServer() {
        NBTTagCompound data = new NBTTagCompound();
        for (Map.Entry<UpgradeType, Boolean> e : selectedUpgrades.entrySet()) {
            if (e.getValue()) data.setBoolean(e.getKey().name(), true);
        }
        if (NetworkHandler.INSTANCE == null) {
            throw new RuntimeException("网络未初始化!");
        }
        NetworkHandler.INSTANCE.sendToServer(new PacketUpgradeSelection(data));
    }

    private void loadAvailableUpgrades() {
        availableUpgrades.clear();
        if (selectorNBT == null) return;
        NBTTagList list = selectorNBT.getTagList("AvailableUpgrades", 8);
        for (int i = 0; i < list.tagCount(); i++) {
            String name = list.getStringTagAt(i);
            UpgradeType type = UpgradeType.fromString(name);
            if (type != null) {
                availableUpgrades.computeIfAbsent(type.getCategory(), k -> new ArrayList<>()).add(type);
            } else {
                System.err.println("[moremod] 无法识别的升级名: " + name);
            }
        }
        // 每个分类按显示名排序（美观）
        for (Map.Entry<UpgradeType.UpgradeCategory, List<UpgradeType>> e : availableUpgrades.entrySet()) {
            e.getValue().sort(Comparator.comparing(UpgradeType::getDisplayName));
        }
    }

    private void loadCurrentSelections() {
        if (selectorNBT == null) return;
        NBTTagCompound selections = selectorNBT.getCompoundTag("CurrentSelections");
        for (String key : selections.getKeySet()) {
            if (selections.getBoolean(key)) {
                UpgradeType type = UpgradeType.fromString(key);
                if (type != null) selectedUpgrades.put(type, true);
            }
        }
        updateSelectionCounts();
    }

    // ====== 自定义按钮 ======

    private class CategoryTabButton extends GuiButton {
        final UpgradeType.UpgradeCategory category;
        boolean selected;

        CategoryTabButton(int id, int x, int y, int w, int h, UpgradeType.UpgradeCategory cat) {
            super(id, x, y, w, h, cat.getName());
            this.category = cat;
            this.selected = (cat == currentCategory);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

            int bg = selected ? 0x8032A852 : (hovered ? 0x804A4A4A : 0x80333333);
            drawRect(x, y, x + width, y + height, bg);
            int line = selected ? 0xFF32A852 : 0xFF777777;
            drawHorizontalLine(x, x + width - 1, y + height - 1, line);

            // 根据按钮宽度截断类别名称
            String text = displayString;
            int maxTextW = width - 8;
            String drawText = mc.fontRenderer.trimStringToWidth(text, maxTextW);
            if (!drawText.equals(text)) {
                if (drawText.length() > 0) {
                    drawText = drawText.substring(0, Math.max(0, drawText.length() - 1)) + "…";
                }
            }

            int color = selected ? 0xFFFFFF : (hovered ? 0xFFFFAA : 0xDDDDDD);
            int tw = mc.fontRenderer.getStringWidth(drawText);
            mc.fontRenderer.drawString(drawText, x + (width - tw) / 2, y + (height - 8) / 2, color);
        }
    }

    private class UpgradeButton extends GuiButton {
        final UpgradeType upgrade;
        boolean selected;

        UpgradeButton(int id, int x, int y, int w, int h, UpgradeType up, boolean sel) {
            super(id, x, y, w, h, "");
            this.upgrade = up;
            this.selected = sel;
            updateDisplayString();
        }

        void updateDisplayString() {
            String prefix = selected ? "✓ " : "○ ";
            String label = prefix + upgrade.getDisplayName();

            // 按宽度截断（避免和右侧边框/色条重叠）
            int maxW = this.width - 10;
            String trimmed = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, maxW);
            if (!trimmed.equals(label)) {
                if (trimmed.length() > 0) {
                    trimmed = trimmed.substring(0, Math.max(0, trimmed.length() - 1)) + "…";
                }
            }
            this.displayString = trimmed;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

            int bg = selected ? 0x8032A852 : (hovered ? 0x80444444 : 0x80333333);
            drawRect(x, y, x + width, y + height, bg);

            // 左侧类别色条
            int catColor = categoryColor(upgrade.getCategory());
            drawRect(x, y, x + 3, y + height, 0xFF000000 | catColor);

            // 文本
            updateDisplayString();
            int color = selected ? 0xFFFFFF : (hovered ? 0xFFFFEE : 0xDDDDDD);
            mc.fontRenderer.drawString(displayString, x + 6, y + 7, color);

            // 边框（选中时更亮）
            int border = selected ? 0xFF32A852 : 0x60000000;
            drawHorizontalLine(x, x + width - 1, y, border);
            drawHorizontalLine(x, x + width - 1, y + height - 1, border);
            drawVerticalLine(x, y, y + height - 1, border);
            drawVerticalLine(x + width - 1, y, y + height - 1, border);
        }

        private int categoryColor(UpgradeType.UpgradeCategory cat) {
            // 仅用于侧边色条
            switch (cat) {
                case ENERGY: return 0x2EC4B6;
                case SURVIVAL: return 0xFFB703;
                case AUXILIARY: return 0x8E44AD;
                case COMBAT: return 0xE63946;
                case PACKAGE: return 0x457B9D;
                default: return 0x999999;
            }
        }
    }

    private static class SmallIconButton extends GuiButton {
        SmallIconButton(int id, int x, int y, int w, int h, String label) {
            super(id, x, y, w, h, label);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            int bg = enabled ? (hovered ? 0x80555555 : 0x80444444) : 0x80333333;
            drawRect(x, y, x + width, y + height, bg);
            int color = enabled ? (hovered ? 0xFFFFFF : 0xDDDDDD) : 0x888888;
            int tw = mc.fontRenderer.getStringWidth(displayString);
            mc.fontRenderer.drawString(displayString, x + (width - tw) / 2, y + (height - 8) / 2, color);
        }
    }
}
