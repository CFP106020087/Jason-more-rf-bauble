package com.moremod.synergy.gui;

import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyEventType;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.*;

/**
 * 协同效应手册 GUI
 *
 * 一个书本风格的 GUI，展示所有可用的 Synergy 组合。
 * 使用原版书本材质。
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyGuide extends GuiScreen {

    // 使用原版书本材质
    private static final ResourceLocation BOOK_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/book.png");

    // GUI 尺寸 (原版书本)
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;

    // 内容区域
    private static final int CONTENT_X = 36;
    private static final int CONTENT_Y = 30;
    private static final int CONTENT_WIDTH = 116;
    private static final int LINE_HEIGHT = 10;
    private static final int SYNERGIES_PER_PAGE = 2;

    // 类别定义
    private static final String[] CATEGORIES = {
            "all",         // 全部
            "combat",      // 战斗类
            "energy",      // 能量类
            "mechanism"    // 机制类
    };

    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();
    private static final Map<String, Integer> CATEGORY_COLORS = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("all", "全部协同");
        CATEGORY_NAMES.put("combat", "战斗协同");
        CATEGORY_NAMES.put("energy", "能量协同");
        CATEGORY_NAMES.put("mechanism", "机制协同");

        CATEGORY_COLORS.put("all", 0xFFFFFF);
        CATEGORY_COLORS.put("combat", 0xFF5555);
        CATEGORY_COLORS.put("energy", 0xFFFF55);
        CATEGORY_COLORS.put("mechanism", 0x5555FF);
    }

    // 按钮ID
    private static final int BUTTON_PREV_PAGE = 0;
    private static final int BUTTON_NEXT_PAGE = 1;
    private static final int BUTTON_CATEGORY_BASE = 100;

    private final EntityPlayer player;
    private int guiLeft;
    private int guiTop;

    private int currentCategory = 0;
    private int currentPage = 0;
    private List<SynergyDefinition> currentSynergies = new ArrayList<>();

    public GuiSynergyGuide(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - BOOK_WIDTH) / 2;
        this.guiTop = (this.height - BOOK_HEIGHT) / 2;

        // 加载当前类别的 synergies
        loadSynergiesForCategory(CATEGORIES[currentCategory]);

        // 创建按钮
        refreshButtons();
    }

    private void refreshButtons() {
        this.buttonList.clear();

        // 翻页按钮 - 使用原版书本按钮位置
        int totalPages = getTotalPages();

        // 上一页按钮 (左下角)
        if (currentPage > 0) {
            this.buttonList.add(new GuiButtonPageArrow(BUTTON_PREV_PAGE,
                    guiLeft + 18, guiTop + 154, true));
        }

        // 下一页按钮 (右下角)
        if (currentPage < totalPages - 1) {
            this.buttonList.add(new GuiButtonPageArrow(BUTTON_NEXT_PAGE,
                    guiLeft + 141, guiTop + 154, false));
        }

        // 类别标签按钮 (顶部)
        int tabWidth = 40;
        int tabStartX = guiLeft + 20;
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            String name = CATEGORY_NAMES.get(cat);
            boolean selected = (i == currentCategory);

            GuiButton tabBtn = new GuiButton(
                    BUTTON_CATEGORY_BASE + i,
                    tabStartX + i * (tabWidth + 2),
                    guiTop - 18,
                    tabWidth, 16,
                    name
            );
            tabBtn.enabled = !selected;
            this.buttonList.add(tabBtn);
        }
    }

    private void loadSynergiesForCategory(String category) {
        SynergyManager manager = SynergyManager.getInstance();
        Collection<SynergyDefinition> allSynergies = manager.getAllSynergies();

        currentSynergies.clear();

        if (category.equals("all")) {
            currentSynergies.addAll(allSynergies);
        } else {
            for (SynergyDefinition synergy : allSynergies) {
                if (synergy.getCategory().equalsIgnoreCase(category)) {
                    currentSynergies.add(synergy);
                }
            }
        }

        // 按优先级排序
        currentSynergies.sort(Comparator.comparingInt(SynergyDefinition::getPriority));
        currentPage = 0;
    }

    private int getTotalPages() {
        return Math.max(1, (currentSynergies.size() + SYNERGIES_PER_PAGE - 1) / SYNERGIES_PER_PAGE);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_PREV_PAGE) {
            if (currentPage > 0) {
                currentPage--;
                refreshButtons();
            }
        } else if (button.id == BUTTON_NEXT_PAGE) {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
                refreshButtons();
            }
        } else if (button.id >= BUTTON_CATEGORY_BASE && button.id < BUTTON_CATEGORY_BASE + CATEGORIES.length) {
            int newCategory = button.id - BUTTON_CATEGORY_BASE;
            if (newCategory != currentCategory) {
                currentCategory = newCategory;
                loadSynergiesForCategory(CATEGORIES[currentCategory]);
                refreshButtons();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制半透明背景
        drawDefaultBackground();

        // 绘制书本材质
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BOOK_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);

        // 绘制标题
        String category = CATEGORIES[currentCategory];
        String title = CATEGORY_NAMES.get(category);
        int titleColor = CATEGORY_COLORS.getOrDefault(category, 0xFFFFFF);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + title,
                guiLeft + BOOK_WIDTH / 2, guiTop + 12, titleColor);

        // 绘制页码
        int totalPages = getTotalPages();
        String pageText = (currentPage + 1) + " / " + totalPages;
        drawCenteredString(fontRenderer, pageText,
                guiLeft + BOOK_WIDTH / 2, guiTop + 168, 0x888888);

        // 绘制当前页的 Synergy 内容
        drawSynergyContent();

        // 绘制按钮
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制按钮悬停提示
        for (GuiButton button : this.buttonList) {
            if (button.isMouseOver()) {
                if (button.id >= BUTTON_CATEGORY_BASE && button.id < BUTTON_CATEGORY_BASE + CATEGORIES.length) {
                    int catIndex = button.id - BUTTON_CATEGORY_BASE;
                    String cat = CATEGORIES[catIndex];
                    int count = countSynergiesInCategory(cat);
                    drawHoveringText(count + " 个协同效应", mouseX, mouseY);
                }
            }
        }
    }

    private void drawSynergyContent() {
        if (currentSynergies.isEmpty()) {
            drawCenteredString(fontRenderer, TextFormatting.GRAY + "暂无协同效应",
                    guiLeft + BOOK_WIDTH / 2, guiTop + 80, 0xAAAAAA);
            return;
        }

        int startIndex = currentPage * SYNERGIES_PER_PAGE;
        int endIndex = Math.min(startIndex + SYNERGIES_PER_PAGE, currentSynergies.size());

        int y = guiTop + CONTENT_Y;
        String category = CATEGORIES[currentCategory];
        int categoryColor = CATEGORY_COLORS.getOrDefault(category, 0xFFFFFF);

        for (int i = startIndex; i < endIndex; i++) {
            SynergyDefinition synergy = currentSynergies.get(i);
            y = drawSynergyEntry(synergy, guiLeft + CONTENT_X, y, categoryColor);
            y += 8; // 间隔
        }
    }

    private int drawSynergyEntry(SynergyDefinition synergy, int x, int y, int categoryColor) {
        // 名称 (粗体 + 颜色)
        String name = TextFormatting.BOLD + synergy.getDisplayName();
        fontRenderer.drawString(name, x, y, categoryColor);
        y += LINE_HEIGHT + 2;

        // 描述 (灰色，自动换行)
        String desc = synergy.getDescription();
        if (desc != null && !desc.isEmpty()) {
            List<String> descLines = fontRenderer.listFormattedStringToWidth(desc, CONTENT_WIDTH);
            for (String line : descLines) {
                fontRenderer.drawString(line, x, y, 0x666666);
                y += LINE_HEIGHT;
            }
        }
        y += 2;

        // 所需模块
        List<String> modules = synergy.getRequiredModules();
        if (!modules.isEmpty()) {
            fontRenderer.drawString(TextFormatting.GOLD + "模块:", x, y, 0xFFAA00);
            y += LINE_HEIGHT;

            StringBuilder moduleLine = new StringBuilder();
            for (int j = 0; j < modules.size(); j++) {
                if (j > 0) moduleLine.append(" + ");
                moduleLine.append(formatModuleName(modules.get(j)));
            }

            List<String> moduleLines = fontRenderer.listFormattedStringToWidth(
                    TextFormatting.AQUA + moduleLine.toString(), CONTENT_WIDTH);
            for (String line : moduleLines) {
                fontRenderer.drawString(line, x + 4, y, 0x55FFFF);
                y += LINE_HEIGHT;
            }
        }

        // 触发事件
        Set<SynergyEventType> events = synergy.getTriggerEvents();
        if (!events.isEmpty()) {
            StringBuilder eventLine = new StringBuilder();
            eventLine.append(TextFormatting.LIGHT_PURPLE).append("触发: ");
            boolean first = true;
            for (SynergyEventType event : events) {
                if (!first) eventLine.append(", ");
                eventLine.append(TextFormatting.GREEN).append(getEventDisplayName(event));
                first = false;
            }
            fontRenderer.drawString(eventLine.toString(), x, y, 0xFFFFFF);
            y += LINE_HEIGHT;
        }

        return y;
    }

    private int countSynergiesInCategory(String category) {
        if (category.equals("all")) {
            return SynergyManager.getInstance().getAllSynergies().size();
        }

        int count = 0;
        for (SynergyDefinition synergy : SynergyManager.getInstance().getAllSynergies()) {
            if (synergy.getCategory().equalsIgnoreCase(category)) {
                count++;
            }
        }
        return count;
    }

    private String formatModuleName(String moduleId) {
        String[] parts = moduleId.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private String getEventDisplayName(SynergyEventType event) {
        switch (event) {
            case TICK: return "每刻";
            case ATTACK: return "攻击";
            case HURT: return "受伤";
            case KILL: return "击杀";
            case DEATH: return "死亡";
            case ENERGY_CONSUME: return "消耗能量";
            case ENERGY_RECHARGE: return "充能";
            case ENERGY_FULL: return "满能量";
            case ENERGY_LOW: return "低能量";
            case CRITICAL_HIT: return "暴击";
            case ENVIRONMENTAL_DAMAGE: return "环境伤害";
            case LOW_HEALTH: return "低血量";
            case FATAL_DAMAGE: return "致命伤害";
            case SPRINT: return "疾跑";
            case JUMP: return "跳跃";
            case SNEAK: return "潜行";
            case ANY: return "任意";
            default: return event.name();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // ESC 关闭
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }

        // 左右箭头翻页
        if (keyCode == 203) { // Left arrow
            if (currentPage > 0) {
                currentPage--;
                refreshButtons();
            }
        } else if (keyCode == 205) { // Right arrow
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
                refreshButtons();
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    /**
     * 自定义翻页箭头按钮
     */
    @SideOnly(Side.CLIENT)
    private static class GuiButtonPageArrow extends GuiButton {
        private final boolean isLeft;

        public GuiButtonPageArrow(int buttonId, int x, int y, boolean isLeft) {
            super(buttonId, x, y, 23, 13, "");
            this.isLeft = isLeft;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            boolean hovered = mouseX >= this.x && mouseY >= this.y &&
                    mouseX < this.x + this.width && mouseY < this.y + this.height;

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BOOK_TEXTURE);

            int textureX = isLeft ? 3 : 26;
            int textureY = hovered ? 207 : 194;

            this.drawTexturedModalRect(this.x, this.y, textureX, textureY, 23, 13);
        }
    }
}
