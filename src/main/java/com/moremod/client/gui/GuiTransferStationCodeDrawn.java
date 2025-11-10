package com.moremod.client.gui;

import com.moremod.container.ContainerTransferStation;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketTransferGem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 转移台 GUI（纯代码绘制）
 * 
 * 功能：将源宝石的词条转移到目标宝石上
 * 
 * 槽位布局：
 * - 源宝石槽（左上，金色）
 * - 目标宝石槽（中上，蓝色）
 * - 材料槽（左下，灰色）
 * - 输出槽（右侧，绿色）
 */
public class GuiTransferStationCodeDrawn extends GuiContainer {
    
    private final ContainerTransferStation container;
    private final InventoryPlayer playerInv;
    private GuiButton transferButton;
    
    public GuiTransferStationCodeDrawn(InventoryPlayer playerInv, ContainerTransferStation container) {
        super(container);
        this.container = container;
        this.playerInv = playerInv;
        this.xSize = 176;
        this.ySize = 166;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        // 转移按钮（中间位置）
        int buttonX = guiLeft + 88 - 40; // 居中
        int buttonY = guiTop + 52;
        
        transferButton = new GuiButton(
            0,
            buttonX,
            buttonY,
            80,
            20,
            "转移词条"
        );
        
        this.buttonList.add(transferButton);
        
        updateButton();
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButton();
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButton() {
        if (transferButton != null) {
            transferButton.enabled = container.canTransfer();
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && button.enabled) {
            // 发送转移请求到服务端
            PacketHandler.INSTANCE.sendToServer(new PacketTransferGem());
        }
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        
        // 绘制主背景
        drawBackground(x, y);
        
        // 绘制槽位
        drawSourceSlot(x + 25, y + 24);      // 源宝石槽（金色）
        drawTargetSlot(x + 70, y + 24);      // 目标宝石槽（蓝色）
        drawMaterialSlot(x + 25, y + 52);    // 材料槽（灰色）
        drawOutputSlot(x + 133, y + 34);     // 输出槽（绿色）
        
        // 绘制箭头指示
        if (container.canTransfer()) {
            drawArrows(x, y, true);  // 高亮箭头
        } else {
            drawArrows(x, y, false); // 普通箭头
        }
        
        // 绘制玩家背包槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        
        // 绘制快捷栏槽位
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + 141);
        }
    }
    
    /**
     * 绘制GUI主背景
     */
    private void drawBackground(int x, int y) {
        // 外边框
        drawRect(x, y, x + xSize, y + ySize, 0xFF8B8B8B);
        
        // 主背景
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, 0xFFC6C6C6);
        
        // 顶部标题栏
        drawGradientRect(x + 4, y + 4, x + xSize - 4, y + 16, 0xFFB0B0B0, 0xFFC6C6C6);
        
        // 操作区域背景（上半部分）
        drawRect(x + 4, y + 17, x + xSize - 4, y + 75, 0xFFAAAAAA);
        
        // 分隔线
        drawRect(x + 4, y + 76, x + xSize - 4, y + 77, 0xFF8B8B8B);
        
        // 状态信息区域
        drawRect(x + 4, y + 78, x + xSize - 4, y + 82, 0xFFB0B0B0);
    }
    
    /**
     * 绘制普通槽位
     */
    private void drawSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF555555);
    }
    
    /**
     * 绘制源宝石槽（金色主题）
     */
    private void drawSourceSlot(int x, int y) {
        // 外发光（金色）
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x60FFAA00);
        
        // 槽位边框（金色）
        drawRect(x, y, x + 18, y + 18, 0xFFCC8800);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        
        // 槽位背景（淡金色）
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF6A5A00);
        
        // 四角装饰点（金色）
        for (int[] corner : new int[][]{{3, 3}, {13, 3}, {3, 13}, {13, 13}}) {
            drawRect(x + corner[0], y + corner[1], x + corner[0] + 2, y + corner[1] + 2, 0xFFFFDD77);
        }
    }
    
    /**
     * 绘制目标宝石槽（蓝色主题）
     */
    private void drawTargetSlot(int x, int y) {
        // 外发光（蓝色）
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x6000AAFF);
        
        // 槽位边框（蓝色）
        drawRect(x, y, x + 18, y + 18, 0xFF0088CC);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        
        // 槽位背景（淡蓝色）
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF003A5A);
        
        // 四角装饰点（蓝色）
        for (int[] corner : new int[][]{{3, 3}, {13, 3}, {3, 13}, {13, 13}}) {
            drawRect(x + corner[0], y + corner[1], x + corner[0] + 2, y + corner[1] + 2, 0xFF77DDFF);
        }
    }
    
    /**
     * 绘制材料槽（灰色主题）
     */
    private void drawMaterialSlot(int x, int y) {
        // 外发光（紫色）
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x60AA88CC);
        
        // 槽位边框（紫色）
        drawRect(x, y, x + 18, y + 18, 0xFF8866AA);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        
        // 槽位背景（淡紫色）
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF4A3A5A);
        
        // 十字装饰（材料符号）
        drawRect(x + 8, y + 5, x + 10, y + 13, 0xFFDD99FF);  // 竖
        drawRect(x + 5, y + 8, x + 13, y + 10, 0xFFDD99FF);  // 横
    }
    
    /**
     * 绘制输出槽（绿色主题）
     */
    private void drawOutputSlot(int x, int y) {
        // 外发光（绿色，更强）
        drawRect(x - 2, y - 2, x + 20, y + 20, 0x6000FF00);
        drawRect(x - 1, y - 1, x + 19, y + 19, 0x8000FF00);
        
        // 槽位边框（绿色）
        drawRect(x, y, x + 18, y + 18, 0xFF00CC00);
        
        // 槽位内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        
        // 槽位背景（淡绿色）
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF004A00);
        
        // 对勾装饰（成功符号）
        drawLine(x + 5, y + 9, x + 8, y + 12, 0xFF77FF77);
        drawLine(x + 8, y + 12, x + 13, y + 5, 0xFF77FF77);
    }
    
    /**
     * 绘制箭头指示
     */
    private void drawArrows(int x, int y, boolean highlight) {
        int color = highlight ? 0xFF00FF00 : 0xFF888888;
        int glowColor = highlight ? 0x8000FF00 : 0x80666666;
        
        // 箭头1：源 → 目标
        int arrow1X = x + 48;
        int arrow1Y = y + 32;
        
        // 发光效果（如果高亮）
        if (highlight) {
            drawRect(arrow1X - 1, arrow1Y - 1, arrow1X + 21, arrow1Y + 6, glowColor);
        }
        
        // 箭头主体
        drawRect(arrow1X, arrow1Y, arrow1X + 18, arrow1Y + 4, color);
        
        // 箭头头部
        int[][] arrowHead1 = {
            {arrow1X + 15, arrow1Y - 2},
            {arrow1X + 20, arrow1Y + 2},
            {arrow1X + 15, arrow1Y + 6}
        };
        drawTriangle(arrowHead1, color);
        
        // 箭头2：目标 → 输出
        int arrow2X = x + 95;
        int arrow2Y = y + 38;
        
        // 发光效果（如果高亮）
        if (highlight) {
            drawRect(arrow2X - 1, arrow2Y - 1, arrow2X + 36, arrow2Y + 5, glowColor);
        }
        
        // 箭头主体
        drawRect(arrow2X, arrow2Y, arrow2X + 30, arrow2Y + 3, color);
        
        // 箭头头部
        int[][] arrowHead2 = {
            {arrow2X + 28, arrow2Y - 2},
            {arrow2X + 35, arrow2Y + 1},
            {arrow2X + 28, arrow2Y + 5}
        };
        drawTriangle(arrowHead2, color);
        
        // 如果高亮，添加流动效果点
        if (highlight) {
            long time = System.currentTimeMillis();
            int offset1 = (int)((time / 50) % 20);
            int offset2 = (int)((time / 50) % 35);
            
            // 箭头1上的流动点
            drawRect(arrow1X + offset1, arrow1Y, arrow1X + offset1 + 3, arrow1Y + 4, 0xFFFFFFFF);
            
            // 箭头2上的流动点
            drawRect(arrow2X + offset2, arrow2Y, arrow2X + offset2 + 3, arrow2Y + 3, 0xFFFFFFFF);
        }
    }
    
    /**
     * 绘制三角形
     */
    private void drawTriangle(int[][] points, int color) {
        // 使用直线近似三角形
        drawLine(points[0][0], points[0][1], points[1][0], points[1][1], color);
        drawLine(points[1][0], points[1][1], points[2][0], points[2][1], color);
        drawLine(points[2][0], points[2][1], points[0][0], points[0][1], color);
        
        // 填充
        for (int dy = -2; dy <= 2; dy++) {
            int width = 5 - Math.abs(dy);
            int startX = points[1][0] - width;
            drawRect(startX, points[1][1] + dy, startX + width * 2, points[1][1] + dy + 1, color);
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
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 绘制标题
        String title = "§6词条转移台";
        this.fontRenderer.drawString(title, 8, 6, 0xFFFFFF);
        
        // 绘制"背包"文字
        this.fontRenderer.drawString(
            playerInv.getDisplayName().getUnformattedText(), 
            8, 
            this.ySize - 96 + 2, 
            4210752
        );
        
        // 绘制槽位标签
        drawSlotLabels();
        
        // 绘制状态信息
        drawStatusInfo();
    }
    
    /**
     * 绘制槽位标签
     */
    private void drawSlotLabels() {
        // 源宝石
        this.fontRenderer.drawString("§6源", 30, 14, 0xFFFFFF);
        
        // 目标宝石
        this.fontRenderer.drawString("§b目标", 70, 14, 0xFFFFFF);
        
        // 材料
        this.fontRenderer.drawString("§d材料", 25, 43, 0xFFFFFF);
        
        // 输出
        this.fontRenderer.drawString("§a结果", 130, 24, 0xFFFFFF);
    }
    
    /**
     * 绘制状态信息
     */
    private void drawStatusInfo() {
        int statusY = 79;
        
        if (container.canTransfer()) {
            // 可以转移
            this.fontRenderer.drawString("§a✓ 可以转移", 10, statusY, 0xFFFFFF);
            
            // 显示经验消耗
            int xpCost = container.getRequiredXp();
            String xpText = "§e消耗: §6" + xpCost + "级";
            
            // 检查玩家是否有足够经验
            if (mc.player.experienceLevel < xpCost) {
                xpText += " §c(不足)";
            }
            
            this.fontRenderer.drawString(xpText, 100, statusY, 0xFFFFFF);
        } else {
            // 不能转移 - 显示错误信息
            String error = container.getErrorMessage();
            if (!error.isEmpty()) {
                // 截断过长的错误信息
                if (this.fontRenderer.getStringWidth(error) > 150) {
                    while (this.fontRenderer.getStringWidth(error + "...") > 150 && error.length() > 5) {
                        error = error.substring(0, error.length() - 1);
                    }
                    error += "...";
                }
                this.fontRenderer.drawString("§c✗ " + error, 10, statusY, 0xFFFFFF);
            } else {
                this.fontRenderer.drawString("§7放入物品开始", 10, statusY, 0xFFFFFF);
            }
        }
    }
    
    /**
     * 绘制工具提示
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        
        // 绘制自定义工具提示
        drawCustomTooltips(mouseX, mouseY);
    }
    
    /**
     * 绘制自定义工具提示
     */
    private void drawCustomTooltips(int mouseX, int mouseY) {
        // 源宝石槽提示
        if (isPointInRegion(25, 24, 18, 18, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§6§l源宝石槽");
            tooltip.add("§7放入精炼宝石");
            tooltip.add("§7（单词条宝石）");
            tooltip.add("");
            tooltip.add("§8此槽位的词条将被");
            tooltip.add("§8转移到目标宝石上");
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
        
        // 目标宝石槽提示
        else if (isPointInRegion(70, 24, 18, 18, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§b§l目标宝石槽");
            tooltip.add("§7放入要添加词条的宝石");
            tooltip.add("§7最多可有6个词条");
            tooltip.add("");
            tooltip.add("§8源宝石的词条将");
            tooltip.add("§8添加到此宝石上");
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
        
        // 材料槽提示
        else if (isPointInRegion(25, 52, 18, 18, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§d§l材料槽");
            tooltip.add("§7放入转移符文");
            tooltip.add("§7（稀有材料）");
            tooltip.add("");
            tooltip.add("§8用于词条转移的");
            tooltip.add("§8特殊材料");
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
        
        // 输出槽提示
        else if (isPointInRegion(133, 34, 18, 18, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§a§l输出槽");
            tooltip.add("§7转移后的新宝石");
            tooltip.add("");
            tooltip.add("§8包含原有词条+");
            tooltip.add("§8源宝石转移的词条");
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
    }
}