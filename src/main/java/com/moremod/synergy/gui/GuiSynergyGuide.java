package com.moremod.synergy.gui;

import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyEventType;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.Minecraft;
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

@SideOnly(Side.CLIENT)
public class GuiSynergyGuide extends GuiScreen {

    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("textures/gui/book.png");

    private static final int BOOK_IMAGE_WIDTH = 192;
    private static final int BOOK_IMAGE_HEIGHT = 192;

    // 🔥 Erica 調整：換行寬度與安全寬度 (右頁空間較小，保守一點)
    private static final int WRAP_WIDTH = 60; 
    private static final int MAX_PAGE_SAFE_WIDTH = 70; 

    // 關鍵坐標：左右兩頁的視覺中心點 X 坐標
    private static final int LEFT_PAGE_CENTER = 61;
    private static final int RIGHT_PAGE_CENTER = 131;

    private static final String[] CATEGORIES = { "all", "combat", "energy", "mechanism" };
    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();
    private static final Map<String, Integer> CATEGORY_COLORS = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("all", "全部");
        CATEGORY_NAMES.put("combat", "战斗");
        CATEGORY_NAMES.put("energy", "能量");
        CATEGORY_NAMES.put("mechanism", "机制");

        CATEGORY_COLORS.put("all", 0x555555);
        CATEGORY_COLORS.put("combat", 0xAA0000);
        CATEGORY_COLORS.put("energy", 0xFFAA00);
        CATEGORY_COLORS.put("mechanism", 0x0000AA);
    }

    private final EntityPlayer player;
    private int guiLeft;
    private int guiTop;
    
    private int currentCategoryIndex = 0;
    private int currentPage = 0;
    private List<SynergyDefinition> filteredSynergies = new ArrayList<>();

    private static final int BTN_NEXT = 1;
    private static final int BTN_PREV = 2;
    private static final int BTN_CAT_START = 10;

    public GuiSynergyGuide(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - BOOK_IMAGE_WIDTH) / 2;
        this.guiTop = (this.height - BOOK_IMAGE_HEIGHT) / 2;
        loadCategory(currentCategoryIndex);
        initButtons();
    }

    private void loadCategory(int index) {
        this.currentCategoryIndex = index;
        String catKey = CATEGORIES[index];
        SynergyManager manager = SynergyManager.getInstance();
        Collection<SynergyDefinition> all = manager.getAllSynergies();
        
        filteredSynergies.clear();
        if (catKey.equals("all")) {
            filteredSynergies.addAll(all);
        } else {
            for (SynergyDefinition s : all) {
                if (s.getCategory().equalsIgnoreCase(catKey)) {
                    filteredSynergies.add(s);
                }
            }
        }
        filteredSynergies.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        this.currentPage = 0;
        initButtons();
    }

    private void initButtons() {
        this.buttonList.clear();
        int maxPages = filteredSynergies.size();
        
        if (currentPage < maxPages - 1) {
            this.buttonList.add(new GuiButtonPageArrow(BTN_NEXT, guiLeft + 120, guiTop + 154, false));
        }
        if (currentPage > 0) {
            this.buttonList.add(new GuiButtonPageArrow(BTN_PREV, guiLeft + 38, guiTop + 154, true));
        }

        int tabY = guiTop + 15;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean isSelected = (i == currentCategoryIndex);
            String key = CATEGORIES[i];
            int tabWidth = 28;
            int xPos = guiLeft - tabWidth + (isSelected ? 2 : 0); 
            
            this.buttonList.add(new GuiButtonCategory(BTN_CAT_START + i, 
                    xPos, tabY, tabWidth, 22,
                    CATEGORY_NAMES.get(key), 
                    CATEGORY_COLORS.getOrDefault(key, 0xFFFFFF),
                    isSelected));
            tabY += 24;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        this.mc.getTextureManager().bindTexture(BOOK_TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, BOOK_IMAGE_WIDTH, BOOK_IMAGE_HEIGHT);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (filteredSynergies.isEmpty()) {
            drawCenteredString(fontRenderer, "暂无内容", width / 2, guiTop + 80, 0x808080);
            return;
        }

        SynergyDefinition synergy = filteredSynergies.get(currentPage);

        // ========== 左页 (Left Page) ==========
        int leftCenterX = guiLeft + LEFT_PAGE_CENTER;
        int leftY = guiTop + 16;
        
        // 1. 标题
        String title = synergy.getDisplayName();
        int titleColor = CATEGORY_COLORS.getOrDefault(synergy.getCategory(), 0x000000);
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(leftCenterX, leftY + 5, 0);
        GlStateManager.scale(1.3, 1.3, 1);
        drawCenteredString(fontRenderer, TextFormatting.BOLD + title, 0, 0, titleColor);
        GlStateManager.popMatrix();
        leftY += 20;
        
        // 2. 装饰线
        drawRect(leftCenterX - 30, leftY, leftCenterX + 30, leftY + 1, 0xFFAAAAAA);
        leftY += 10;
        
        // 3. 描述文本 (動態計算高度)
        String desc = synergy.getDescription();
        if (desc == null || desc.isEmpty()) desc = "无详细描述。";
        // drawCenteredSplitString 現在回傳畫完的高度，雖然這裡用不到返回值，但這是個好習慣
        drawCenteredSplitString(desc, leftCenterX, leftY, WRAP_WIDTH, 0);


        // ========== 右页 (Right Page) ==========
        int rightCenterX = guiLeft + RIGHT_PAGE_CENTER;
        int rightY = guiTop + 16;

        // 1. 模块需求标题
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "- 核心组件 -", rightCenterX, rightY, 0);
        rightY += 14;

        // 2. 模块列表
        for (String mod : synergy.getRequiredModules()) {
            String modName = formatName(mod);
            // 🔥 Erica 優化：讓方法回傳「這一行實際佔用的高度」，這樣就算換行也不會重疊！
            int heightUsed = drawCenteredSplitString(TextFormatting.BLUE + modName, rightCenterX, rightY, MAX_PAGE_SAFE_WIDTH, 0);
            rightY += heightUsed + 2; // +2 間距
        }
        
        rightY += 8; // 段落間隔

        // 3. 触发条件 (Trigger Mechanism) - 這次一定要加上！
        Set<SynergyEventType> events = synergy.getTriggerEvents();
        if (events != null && !events.isEmpty()) {
            drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "- 触发机制 -", rightCenterX, rightY, 0);
            rightY += 14;
            
            for (SynergyEventType e : events) {
                String eventName = getEventName(e);
                int heightUsed = drawCenteredSplitString(TextFormatting.DARK_RED + eventName, rightCenterX, rightY, MAX_PAGE_SAFE_WIDTH, 0);
                rightY += heightUsed + 2;
            }
        }

        // 底部页码
        String pageStr = (currentPage + 1) + " / " + Math.max(1, filteredSynergies.size());
        this.drawCenteredString(fontRenderer, pageStr, this.width / 2, guiTop + 158, 0x333333);
        
        // Tooltips
        for (GuiButton btn : this.buttonList) {
            if (btn instanceof GuiButtonCategory && btn.isMouseOver()) {
                drawHoveringText(((GuiButtonCategory)btn).categoryName, mouseX, mouseY);
            }
        }
    }

    /**
     * 🔥 改良版核心魔法 V2：智能置中 + 自動縮放 + 回傳高度
     * @return 繪製這些文字所佔用的總高度 (像素)
     */
    private int drawCenteredSplitString(String text, int centerX, int startY, int wrapWidth, int color) {
        if (text == null || text.isEmpty()) return 0;

        List<String> lines = this.fontRenderer.listFormattedStringToWidth(text, wrapWidth);
        int totalHeight = 0;
        int y = startY;

        for (String line : lines) {
            int lineWidth = fontRenderer.getStringWidth(line);
            float scale = 1.0f;
            
            // 檢查縮放 (雙重保險)
            if (lineWidth > MAX_PAGE_SAFE_WIDTH) {
                scale = (float) MAX_PAGE_SAFE_WIDTH / lineWidth;
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, y, 0);
            
            if (scale < 1.0f) {
                GlStateManager.scale(scale, scale, 1.0f);
            }

            fontRenderer.drawString(line, -lineWidth / 2, 0, color);
            GlStateManager.popMatrix();

            int lineHeight = fontRenderer.FONT_HEIGHT + 2;
            y += lineHeight;
            totalHeight += lineHeight;
        }
        return totalHeight;
    }

    private String formatName(String input) {
        String[] parts = input.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
    
    private String getEventName(SynergyEventType type) {
        switch(type) {
            case TICK: return "每刻 (Passive)";
            case ATTACK: return "攻击时 (On Attack)";
            case HURT: return "受伤时 (On Hurt)";
            case KILL: return "击杀时 (On Kill)";
            case DEATH: return "死亡时 (On Death)";
            case JUMP: return "跳跃时 (On Jump)";
            case SNEAK: return "潜行时 (On Sneak)";
            default: return type.name();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_NEXT) {
            if (currentPage < filteredSynergies.size() - 1) {
                currentPage++;
                initButtons();
            }
        } else if (button.id == BTN_PREV) {
            if (currentPage > 0) {
                currentPage--;
                initButtons();
            }
        } else if (button.id >= BTN_CAT_START) {
            int index = button.id - BTN_CAT_START;
            if (index != currentCategoryIndex) {
                loadCategory(index);
                mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        }
    }

    private class GuiButtonCategory extends GuiButton {
        private final String categoryName;
        private final int color;
        private final boolean isSelected;
        public GuiButtonCategory(int id, int x, int y, int w, int h, String name, int color, boolean selected) {
            super(id, x, y, w, h, "");
            this.categoryName = name;
            this.color = color;
            this.isSelected = selected;
        }
        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                boolean hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + width && mouseY < this.y + height;
                int drawX = this.x;
                if (isSelected) drawX += 2; 
                drawRect(drawX, this.y, drawX + width, this.y + height, 0xFF000000);
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                if (!isSelected && !hovered) { r *= 0.6f; g *= 0.6f; b *= 0.6f; }
                GlStateManager.color(r, g, b, 1.0f);
                drawRect(drawX + 1, this.y + 1, drawX + width - 1, this.y + height - 1, 
                        (0xFF << 24) | ((int)(r*255)<<16) | ((int)(g*255)<<8) | (int)(b*255));
                String letter = categoryName.substring(0, 1);
                drawCenteredString(mc.fontRenderer, letter, drawX + width / 2, this.y + (height - 8) / 2, 0xFFFFFFFF);
            }
        }
    }

    private static class GuiButtonPageArrow extends GuiButton {
        private final boolean isPrevious;
        public GuiButtonPageArrow(int id, int x, int y, boolean isPrevious) {
            super(id, x, y, 23, 13, "");
            this.isPrevious = isPrevious;
        }
        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                boolean hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + width && mouseY < this.y + height;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BOOK_TEXTURE);
                int u = isPrevious ? 3 : 26;
                int v = hovered ? 207 : 194;
                this.drawTexturedModalRect(this.x, this.y, u, v, 23, 13);
            }
        }
    }
}