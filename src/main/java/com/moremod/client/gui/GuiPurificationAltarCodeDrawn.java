package com.moremod.client.gui;

import com.moremod.container.ContainerPurificationAltar;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketPurifyGem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * 提纯祭坛 - GUI（纯代码绘制）
 * 
 * 特色设计：
 * - 5个输入槽呈弧形排列
 * - 魔法阵风格的装饰
 * - 动态进度条
 * - 品质预测显示
 */
@SideOnly(Side.CLIENT)
public class GuiPurificationAltarCodeDrawn extends GuiContainer {
    
    private final ContainerPurificationAltar container;
    private final InventoryPlayer playerInv;
    
    private GuiButton purifyButton;
    
    public GuiPurificationAltarCodeDrawn(InventoryPlayer playerInv, ContainerPurificationAltar container) {
        super(container);
        this.container = container;
        this.playerInv = playerInv;
        
        this.xSize = 176;
        this.ySize = 180;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        // 提纯按钮
        int buttonX = guiLeft + 52;
        int buttonY = guiTop + 48;
        purifyButton = new GuiButton(0, buttonX, buttonY, 72, 16, 
            I18n.format("gui.moremod.purify"));
        this.buttonList.add(purifyButton);
        
        updateButtonState();
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && button.enabled) {
            // 发送提纯网络包
            PacketHandler.INSTANCE.sendToServer(new PacketPurifyGem());
        }
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonState();
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonState() {
        if (purifyButton != null) {
            boolean canPurify = container.canPurify() && 
                              !container.isPurifying() &&
                              playerInv.player.experienceLevel >= container.getRequiredXP();
            
            purifyButton.enabled = canPurify;
            
            // 更新按钮文字
            if (container.isPurifying()) {
                int progress = container.getPurifyProgress();
                int max = container.getMaxPurifyTime();
                int percent = (progress * 100) / max;
                purifyButton.displayString = I18n.format("gui.moremod.purifying") + 
                                             " " + percent + "%";
            } else {
                purifyButton.displayString = I18n.format("gui.moremod.purify");
            }
        }
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        
        // 绘制主背景
        drawBackground(x, y);
        
        // 绘制魔法阵装饰
        drawMagicCircle(x, y);
        
        // 绘制输入槽（5个弧形排列）
        int[] xPositions = {26, 44, 62, 80, 98};
        int centerY = 26;
        for (int i = 0; i < 5; i++) {
            drawSlot(x + xPositions[i] - 1, y + centerY - 1);
        }
        
        // 绘制输出槽
        drawOutputSlot(x + 61, y + 69);
        
        // 绘制箭头指示
        drawArrow(x + 62, y + 45);
        
        // 绘制进度条
        if (container.isPurifying()) {
            drawProgressBar(x, y);
        }
        
        // 绘制玩家背包槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + 97 + row * 18);
            }
        }
        
        // 绘制快捷栏槽位
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + 155);
        }
    }
    
    /**
     * 绘制GUI主背景
     */
    private void drawBackground(int x, int y) {
        // 外边框 - 深色
        drawRect(x, y, x + xSize, y + ySize, 0xFF4A3A5A);
        
        // 主背景 - 深紫色渐变
        drawGradientRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, 
            0xFF6B5A7A, 0xFF5A4A6A);
        
        // 顶部标题栏
        drawGradientRect(x + 4, y + 4, x + xSize - 4, y + 16, 
            0xFF8A7A9A, 0xFF7A6A8A);
        
        // 中央祭坛区域背景
        drawRect(x + 10, y + 18, x + xSize - 10, y + 88, 0xFF3A2A4A);
        drawRect(x + 11, y + 19, x + xSize - 11, y + 87, 0xFF2A1A3A);
        
        // 分隔线
        drawRect(x + 4, y + 90, x + xSize - 4, y + 91, 0xFF8A7A9A);
    }
    
    /**
     * 绘制魔法阵装饰
     */
    private void drawMagicCircle(int x, int y) {
        int centerX = x + 62;
        int centerY = y + 48;
        
        // 外圆
        drawCircleOutline(centerX, centerY, 32, 0xFF9A8AAA);
        
        // 中圆
        drawCircleOutline(centerX, centerY, 24, 0xFFAA9ABA);
        
        // 内圆（围绕输出槽）
        drawCircleOutline(x + 70, y + 78, 16, 0xFFBA9ACA);
        
        // 从5个输入槽到中心的连线
        int[] xPositions = {26, 44, 62, 80, 98};
        int inputY = 26;
        for (int i = 0; i < 5; i++) {
            int slotX = x + xPositions[i] + 8;
            int slotY = y + inputY + 8;
            drawLineToCenter(slotX, slotY, centerX, centerY, 0x80AA9ABA);
        }
        
        // 五角星装饰
        drawPentagram(centerX, centerY - 5, 12, 0x60FFAA00);
    }
    
    /**
     * 绘制圆形轮廓
     */
    private void drawCircleOutline(int centerX, int centerY, int radius, int color) {
        int segments = 60;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI * 2 * i / segments;
            double angle2 = Math.PI * 2 * (i + 1) / segments;
            
            int x1 = centerX + (int)(Math.cos(angle1) * radius);
            int y1 = centerY + (int)(Math.sin(angle1) * radius);
            int x2 = centerX + (int)(Math.cos(angle2) * radius);
            int y2 = centerY + (int)(Math.sin(angle2) * radius);
            
            drawLine(x1, y1, x2, y2, color);
        }
    }
    
    /**
     * 绘制五角星
     */
    private void drawPentagram(int centerX, int centerY, int radius, int color) {
        double[] angles = new double[5];
        for (int i = 0; i < 5; i++) {
            angles[i] = -Math.PI / 2 + Math.PI * 2 * i / 5;
        }
        
        // 连接五角星的5个顶点
        for (int i = 0; i < 5; i++) {
            int x1 = centerX + (int)(Math.cos(angles[i]) * radius);
            int y1 = centerY + (int)(Math.sin(angles[i]) * radius);
            int x2 = centerX + (int)(Math.cos(angles[(i + 2) % 5]) * radius);
            int y2 = centerY + (int)(Math.sin(angles[(i + 2) % 5]) * radius);
            
            drawLine(x1, y1, x2, y2, color);
        }
    }
    
    /**
     * 绘制从槽位到中心的连线
     */
    private void drawLineToCenter(int x1, int y1, int x2, int y2, int color) {
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            int px = x1 + (x2 - x1) * i / steps;
            int py = y1 + (y2 - y1) * i / steps;
            drawRect(px, py, px + 1, py + 1, color);
        }
    }
    
    /**
     * 绘制直线
     */
    private void drawLine(int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            drawRect(x, y, x + 1, y + 1, color);
        }
    }
    
    /**
     * 绘制普通槽位
     */
    private void drawSlot(int x, int y) {
        // 外发光效果
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x40AA9ABA);
        
        // 槽位边框
        drawRect(x, y, x + 18, y + 18, 0xFF6A5A7A);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF4A3A5A);
        
        // 槽位背景
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF3A2A4A);
    }
    
    /**
     * 绘制输出槽（带特殊效果）
     */
    private void drawOutputSlot(int x, int y) {
        // 外发光效果（更强）
        drawRect(x - 2, y - 2, x + 20, y + 20, 0x60FFAA00);
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x80FFAA00);
        
        // 槽位边框（金色）
        drawRect(x, y, x + 18, y + 18, 0xFFFFAA00);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8A7A00);
        
        // 槽位背景
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF5A4A00);
    }
    
    /**
     * 绘制箭头指示
     */
    private void drawArrow(int x, int y) {
        // 箭头主体
        drawRect(x - 3, y + 1, x + 3, y + 3, 0xFFAA9ABA);
        
        // 箭头头部
        drawRect(x - 1, y - 1, x + 1, y + 1, 0xFFAA9ABA);
        drawRect(x - 1, y + 3, x + 1, y + 5, 0xFFAA9ABA);
        
        // 向下的三角形
        for (int i = 0; i < 4; i++) {
            drawRect(x - i, y + 5 + i, x + i + 1, y + 6 + i, 0xFFAA9ABA);
        }
    }
    
    /**
     * 绘制进度条
     */
    private void drawProgressBar(int x, int y) {
        int progress = container.getPurifyProgress();
        int max = container.getMaxPurifyTime();
        
        int barX = x + 52;
        int barY = y + 68;
        int barWidth = 72;
        int barHeight = 6;
        
        // 进度条背景
        drawRect(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF8A7A9A);
        drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF2A1A3A);
        
        // 计算填充宽度
        int filledWidth = (progress * barWidth) / max;
        
        // 进度条填充（渐变色）
        if (filledWidth > 0) {
            drawGradientRect(barX, barY, barX + filledWidth, barY + barHeight,
                0xFFFFAA00, 0xFFAA7700);
            
            // 发光效果
            drawRect(barX, barY, barX + filledWidth, barY + 1, 0xFFFFDD77);
        }
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = I18n.format("gui.moremod.purification_altar");
        this.fontRenderer.drawString(title, 
            (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 
            6, 0xFFDDDD);
        
        // 玩家背包标题
        this.fontRenderer.drawString(playerInv.getDisplayName().getUnformattedText(), 
            8, this.ySize - 83, 0xFFDDDD);
        
        // 显示状态信息
        int statusY = 76;
        
        if (container.isPurifying()) {
            // 正在提纯...
            String purifying = "§e" + I18n.format("gui.moremod.purifying");
            int width = this.fontRenderer.getStringWidth(purifying);
            this.fontRenderer.drawString(purifying, (xSize - width) / 2, statusY, 0xFFFFFF);
        } else {
            int gemCount = container.getInputGemCount();
            
            if (gemCount == 0) {
                // 提示：放入宝石
                String hint = "§7" + I18n.format("gui.moremod.insert_gems");
                int width = this.fontRenderer.getStringWidth(hint);
                this.fontRenderer.drawString(hint, (xSize - width) / 2, statusY, 0xFFFFFF);
            } else if (container.canPurify()) {
                // 显示预测品质
                int predictedQuality = container.getPredictedQuality();
                String qualityText = "§a" + I18n.format("gui.moremod.predicted_quality") + 
                                    ": §6" + predictedQuality + "%";
                int width = this.fontRenderer.getStringWidth(qualityText);
                this.fontRenderer.drawString(qualityText, (xSize - width) / 2, statusY, 0xFFFFFF);
                
                // 显示消耗
                int requiredXP = container.getRequiredXP();
                String xpText = "§7" + I18n.format("gui.moremod.cost") + 
                               ": §b" + requiredXP + " " + 
                               I18n.format("gui.moremod.levels");
                int xpWidth = this.fontRenderer.getStringWidth(xpText);
                this.fontRenderer.drawString(xpText, (xSize - xpWidth) / 2, statusY + 10, 0xFFFFFF);
            } else {
                // 显示错误信息
                String error;
                if (gemCount < 2) {
                    error = "§c" + I18n.format("gui.moremod.need_more_gems");
                } else {
                    error = "§c" + I18n.format("gui.moremod.gems_not_same");
                }
                int width = this.fontRenderer.getStringWidth(error);
                this.fontRenderer.drawString(error, (xSize - width) / 2, statusY, 0xFFFFFF);
            }
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        
        // 绘制额外的tooltip
        drawCustomTooltips(mouseX, mouseY);
    }
    
    /**
     * 绘制自定义tooltip
     */
    private void drawCustomTooltips(int mouseX, int mouseY) {
        // 如果鼠标在提纯按钮上且玩家经验不足
        if (purifyButton != null && purifyButton.isMouseOver() && 
            !purifyButton.enabled && container.canPurify()) {
            
            int requiredXP = container.getRequiredXP();
            int playerXP = playerInv.player.experienceLevel;
            
            if (playerXP < requiredXP) {
                String tooltip = "§c" + I18n.format("gui.moremod.not_enough_xp") + 
                               ": " + playerXP + "/" + requiredXP;
                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }
}