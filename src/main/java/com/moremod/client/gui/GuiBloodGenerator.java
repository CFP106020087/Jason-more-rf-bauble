package com.moremod.client.gui;

import com.moremod.container.ContainerBloodGenerator;
import com.moremod.energy.BloodEnergyHandler;
import com.moremod.tile.TileEntityBloodGenerator;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 血液发电机GUI - 纯代码绘制
 */
public class GuiBloodGenerator extends GuiContainer {

    private final TileEntityBloodGenerator te;
    private final NumberFormat numberFormat = NumberFormat.getInstance();

    // GUI尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 颜色常量
    private static final int COLOR_BACKGROUND = 0xFFC6C6C6;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    private static final int COLOR_SLOT_BORDER_DARK = 0xFF373737;
    private static final int COLOR_SLOT_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_ENERGY_BG = 0xFF2D2D2D;
    private static final int COLOR_ENERGY_FILL = 0xFFCC2222;
    private static final int COLOR_PROGRESS_BG = 0xFF4A4A4A;
    private static final int COLOR_PROGRESS_FILL = 0xFF22CC22;
    private static final int COLOR_TEXT_TITLE = 0x404040;
    private static final int COLOR_TEXT_INFO = 0xFFFFFF;

    public GuiBloodGenerator(InventoryPlayer playerInv, TileEntityBloodGenerator te) {
        super(new ContainerBloodGenerator(playerInv, te));
        this.te = te;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // 自定义悬停提示
        drawCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        int x = guiLeft;
        int y = guiTop;

        // 绘制主背景
        drawRect(x, y, x + xSize, y + ySize, COLOR_BACKGROUND);

        // 绘制边框效果
        drawBorder(x, y, xSize, ySize);

        // 绘制输入槽 (左边)
        drawSlot(x + 55, y + 34);

        // 绘制输出槽 (右边)
        drawSlot(x + 115, y + 34);

        // 绘制进度箭头区域
        drawProgressArrow(x + 76, y + 35);

        // 绘制能量条
        drawEnergyBar(x + 10, y + 10, 12, 56);

        // 绘制玩家背包槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + 83 + row * 18);
            }
        }

        // 绘制快捷栏
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + 141);
        }

        GlStateManager.enableTexture2D();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = "血液发电机";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, COLOR_TEXT_TITLE);

        // 能量显示
        String energyText = formatNumber(te.getEnergyStored()) + " RF";
        fontRenderer.drawString(energyText, 26, 15, COLOR_TEXT_TITLE);

        // 最大能量
        String maxText = "/ " + formatNumber(te.getMaxEnergy());
        fontRenderer.drawString(maxText, 26, 25, 0x808080);

        // 输出速率
        fontRenderer.drawString("50000 RF/t", 26, 40, 0x606060);

        // 输入槽标签
        fontRenderer.drawString("输入", 55, 55, 0x606060);

        // 输出槽标签
        fontRenderer.drawString("输出", 115, 55, 0x606060);

        // 如果正在处理，显示进度信息
        if (te.getTotalEnergyToExtract() > 0) {
            int progress = (int) (te.getProgress() * 100);
            String progressText = progress + "%";
            fontRenderer.drawString(progressText, 82, 50, 0x22AA22);
        }

        // 显示输入物品的血液信息
        ItemStack inputStack = te.getInventory().getStackInSlot(0);
        if (!inputStack.isEmpty() && BloodEnergyHandler.hasBloodData(inputStack)) {
            int blood = BloodEnergyHandler.getBloodEnergy(inputStack);
            int flesh = BloodEnergyHandler.getFleshChunks(inputStack);
            int total = BloodEnergyHandler.getTotalEnergy(inputStack);

            fontRenderer.drawString("血液: " + formatNumber(blood), 90, 10, 0xCC2222);
            fontRenderer.drawString("肉块: " + flesh, 90, 20, 0xCC6622);
            fontRenderer.drawString("总计: " + formatNumber(total) + " RF", 90, 30, 0x22CC22);
        }
    }

    /**
     * 绘制进度箭头
     */
    private void drawProgressArrow(int x, int y) {
        int width = 24;
        int height = 16;

        // 背景
        drawRect(x, y, x + width, y + height, COLOR_PROGRESS_BG);

        // 进度填充
        float progress = te.getProgress();
        if (progress > 0) {
            int fillWidth = (int) (width * progress);
            drawRect(x, y, x + fillWidth, y + height, COLOR_PROGRESS_FILL);
        }

        // 箭头边框
        drawHorizontalLine(x, x + width - 1, y, 0xFF303030);
        drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFFFFFFFF);
        drawVerticalLine(x, y, y + height - 1, 0xFF303030);
        drawVerticalLine(x + width - 1, y, y + height - 1, 0xFFFFFFFF);

        GlStateManager.enableTexture2D();
        // 绘制箭头符号
        String arrow = ">>>";
        fontRenderer.drawString(arrow, x + 3, y + 4, 0xFFFFFF);
        GlStateManager.disableTexture2D();
    }

    /**
     * 绘制能量条
     */
    private void drawEnergyBar(int x, int y, int width, int height) {
        // 背景
        drawRect(x, y, x + width, y + height, COLOR_ENERGY_BG);

        // 能量填充
        float energyRatio = (float) te.getEnergyStored() / te.getMaxEnergy();
        int fillHeight = (int) (height * energyRatio);
        if (fillHeight > 0) {
            // 从底部向上填充
            int fillY = y + height - fillHeight;
            drawRect(x + 1, fillY, x + width - 1, y + height - 1, COLOR_ENERGY_FILL);
        }

        // 边框
        drawHorizontalLine(x, x + width - 1, y, 0xFF000000);
        drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFF000000);
        drawVerticalLine(x, y, y + height - 1, 0xFF000000);
        drawVerticalLine(x + width - 1, y, y + height - 1, 0xFF000000);
    }

    /**
     * 绘制槽位
     */
    private void drawSlot(int x, int y) {
        int size = 18;

        // 槽位背景
        drawRect(x, y, x + size, y + size, COLOR_SLOT_BG);

        // 3D边框效果
        drawHorizontalLine(x, x + size - 1, y, COLOR_SLOT_BORDER_DARK);
        drawVerticalLine(x, y, y + size - 1, COLOR_SLOT_BORDER_DARK);
        drawHorizontalLine(x, x + size - 1, y + size - 1, COLOR_SLOT_BORDER_LIGHT);
        drawVerticalLine(x + size - 1, y, y + size - 1, COLOR_SLOT_BORDER_LIGHT);
    }

    /**
     * 绘制GUI边框
     */
    private void drawBorder(int x, int y, int width, int height) {
        // 外边框 - 亮色在左上，暗色在右下
        drawHorizontalLine(x, x + width - 1, y, 0xFFFFFFFF);
        drawVerticalLine(x, y, y + height - 1, 0xFFFFFFFF);
        drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFF555555);
        drawVerticalLine(x + width - 1, y, y + height - 1, 0xFF555555);

        // 内边框
        drawHorizontalLine(x + 1, x + width - 2, y + 1, 0xFFDDDDDD);
        drawVerticalLine(x + 1, y + 1, y + height - 2, 0xFFDDDDDD);
        drawHorizontalLine(x + 1, x + width - 2, y + height - 2, 0xFF888888);
        drawVerticalLine(x + width - 2, y + 1, y + height - 2, 0xFF888888);
    }

    /**
     * 绘制自定义悬停提示
     */
    private void drawCustomTooltips(int mouseX, int mouseY) {
        int x = guiLeft;
        int y = guiTop;

        // 能量条悬停
        if (isPointInRegion(10, 10, 12, 56, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7c能量存储");
            tooltip.add("\u00a77" + formatNumber(te.getEnergyStored()) + " / " + formatNumber(te.getMaxEnergy()) + " RF");
            tooltip.add("");
            tooltip.add("\u00a7e输出速率: \u00a7a50,000 RF/t");
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 进度条悬停
        if (isPointInRegion(76, 35, 24, 16, mouseX, mouseY)) {
            if (te.getTotalEnergyToExtract() > 0) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7a提取进度");
                tooltip.add("\u00a77已提取: " + formatNumber(te.getExtractedEnergy()) + " RF");
                tooltip.add("\u00a77总计: " + formatNumber(te.getTotalEnergyToExtract()) + " RF");
                tooltip.add("\u00a77进度: " + (int)(te.getProgress() * 100) + "%");
                drawHoveringText(tooltip, mouseX, mouseY);
            } else {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a77放入沾血武器开始发电");
                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }

        // 输入槽悬停（空槽时）
        if (isPointInRegion(55, 34, 18, 18, mouseX, mouseY)) {
            ItemStack inputStack = te.getInventory().getStackInSlot(0);
            if (inputStack.isEmpty()) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7e输入槽");
                tooltip.add("\u00a77放入沾血的武器");
                tooltip.add("");
                tooltip.add("\u00a78武器需要通过战斗");
                tooltip.add("\u00a78累积血液能量");
                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    /**
     * 绘制实心矩形
     */
    public static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top < bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.color(red, green, blue, alpha);
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, top, 0.0D).endVertex();
        bufferbuilder.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private String formatNumber(int number) {
        return numberFormat.format(number);
    }
}
