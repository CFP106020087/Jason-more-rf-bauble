package com.moremod.client.gui;

import com.moremod.container.ContainerOilGenerator;
import com.moremod.tile.TileEntityOilGenerator;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 石油发电机GUI - 纯代码绘制
 */
public class GuiOilGenerator extends GuiContainer {

    private final TileEntityOilGenerator te;
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
    private static final int COLOR_ENERGY_FILL = 0xFFDD3333;
    private static final int COLOR_FLUID_BG = 0xFF1A1A1A;
    private static final int COLOR_OIL_FILL = 0xFF222222;
    private static final int COLOR_BURN_BG = 0xFF4A4A4A;
    private static final int COLOR_BURN_FILL = 0xFFFF8800;
    private static final int COLOR_TEXT_TITLE = 0x404040;

    public GuiOilGenerator(InventoryPlayer playerInv, TileEntityOilGenerator te) {
        super(new ContainerOilGenerator(playerInv, te));
        this.te = te;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        drawCustomTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        int x = guiLeft;
        int y = guiTop;

        // 主背景
        drawColorRect(x, y, x + xSize, y + ySize, COLOR_BACKGROUND);
        drawBorder(x, y, xSize, ySize);

        // 能量条 (左侧)
        drawEnergyBar(x + 10, y + 10, 16, 60);

        // 液体槽 (右侧)
        drawFluidTank(x + 150, y + 10, 16, 60);

        // 燃烧进度 (中间上方)
        drawBurnProgress(x + 76, y + 36, 24, 16);

        // 燃料槽 (中间下方)
        drawSlot(x + 79, y + 52);

        // 增速插件槽 (上方四个)
        drawSlot(x + 43, y + 16);   // 槽1
        drawSlot(x + 61, y + 16);   // 槽2
        drawSlot(x + 97, y + 16);   // 槽3
        drawSlot(x + 115, y + 16);  // 槽4

        // 玩家背包槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + 83 + row * 18);
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + 141);
        }

        GlStateManager.enableTexture2D();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = "石油发电机";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, COLOR_TEXT_TITLE);

        // 能量数值
        String energyText = formatEnergy(te.getEnergyStored());
        fontRenderer.drawString(energyText, 30, 20, 0x404040);

        // RF/t
        int rfPerTick = te.getRFPerTick();
        if (rfPerTick > 0) {
            String rfText = formatEnergy(rfPerTick) + "/t";
            fontRenderer.drawString(rfText, 30, 32, 0x22AA22);
        } else {
            fontRenderer.drawString("待机", 30, 32, 0x808080);
        }

        // 液体量
        String fluidText = (te.getFluidAmount() / 1000) + "B";
        fontRenderer.drawString(fluidText, 130, 20, 0x404040);

        // 增速倍率
        float multiplier = te.getSpeedMultiplier();
        if (multiplier > 1.0f) {
            String speedText = "x" + String.format("%.1f", multiplier);
            fontRenderer.drawString(speedText, 76, 6, 0xFF8800);
        }

        // 燃料槽标签
        fontRenderer.drawString("燃料", 80, 72, 0x606060);

        // 插件槽标签
        fontRenderer.drawString("增速插件", 62, 6, 0x606060);
    }

    /**
     * 绘制能量条
     */
    private void drawEnergyBar(int x, int y, int width, int height) {
        // 背景
        drawColorRect(x, y, x + width, y + height, COLOR_ENERGY_BG);

        // 能量填充
        float ratio = (float) te.getEnergyStored() / te.getMaxEnergyStored();
        int fillHeight = (int) (height * ratio);
        if (fillHeight > 0) {
            int fillY = y + height - fillHeight;
            // 渐变效果
            int topColor = 0xFFFF4444;
            int bottomColor = 0xFFAA2222;
            drawGradientRect(x + 1, fillY, x + width - 1, y + height - 1, topColor, bottomColor);
        }

        // 边框
        drawRectBorder(x, y, width, height, 0xFF000000);

        // 分隔线
        for (int i = 1; i < 5; i++) {
            int lineY = y + (height * i / 5);
            drawHorizontalLine(x, x + width - 1, lineY, 0x80000000);
        }
    }

    /**
     * 绘制液体槽
     */
    private void drawFluidTank(int x, int y, int width, int height) {
        // 背景
        drawColorRect(x, y, x + width, y + height, COLOR_FLUID_BG);

        // 液体填充
        int fluidAmount = te.getFluidAmount();
        int capacity = te.getFluidCapacity();
        if (fluidAmount > 0 && capacity > 0) {
            float ratio = (float) fluidAmount / capacity;
            int fillHeight = (int) (height * ratio);
            int fillY = y + height - fillHeight;

            // 获取液体颜色
            FluidStack fluid = te.getFluidTank().getFluid();
            int fluidColor = (fluid != null) ? fluid.getFluid().getColor(fluid) : COLOR_OIL_FILL;

            // 如果颜色太暗，使用默认油色
            if ((fluidColor & 0x00FFFFFF) < 0x202020) {
                fluidColor = 0xFF303030;
            }

            drawColorRect(x + 1, fillY, x + width - 1, y + height - 1, fluidColor | 0xFF000000);
        }

        // 边框
        drawRectBorder(x, y, width, height, 0xFF000000);

        // 容量刻度
        for (int i = 1; i < 4; i++) {
            int lineY = y + (height * i / 4);
            drawHorizontalLine(x, x + 3, lineY, 0xFFFFFFFF);
            drawHorizontalLine(x + width - 4, x + width - 1, lineY, 0xFFFFFFFF);
        }
    }

    /**
     * 绘制燃烧进度
     */
    private void drawBurnProgress(int x, int y, int width, int height) {
        // 背景
        drawColorRect(x, y, x + width, y + height, COLOR_BURN_BG);

        // 燃烧进度
        int burnTime = te.getBurnTime();
        int maxBurnTime = te.getMaxBurnTime();
        if (burnTime > 0 && maxBurnTime > 0) {
            float ratio = (float) burnTime / maxBurnTime;
            int fillWidth = (int) (width * ratio);
            drawColorRect(x, y, x + fillWidth, y + height, COLOR_BURN_FILL);
        }

        // 边框
        drawRectBorder(x, y, width, height, 0xFF303030);

        GlStateManager.enableTexture2D();
        // 火焰图标
        if (te.isBurning()) {
            fontRenderer.drawString("\u2600", x + 8, y + 4, 0xFFFF00); // 太阳符号代替火焰
        } else {
            fontRenderer.drawString("-", x + 10, y + 4, 0x808080);
        }
        GlStateManager.disableTexture2D();
    }

    /**
     * 绘制槽位
     */
    private void drawSlot(int x, int y) {
        int size = 18;
        drawColorRect(x, y, x + size, y + size, COLOR_SLOT_BG);
        drawHorizontalLine(x, x + size - 1, y, COLOR_SLOT_BORDER_DARK);
        drawVerticalLine(x, y, y + size - 1, COLOR_SLOT_BORDER_DARK);
        drawHorizontalLine(x, x + size - 1, y + size - 1, COLOR_SLOT_BORDER_LIGHT);
        drawVerticalLine(x + size - 1, y, y + size - 1, COLOR_SLOT_BORDER_LIGHT);
    }

    /**
     * 绘制边框
     */
    private void drawBorder(int x, int y, int width, int height) {
        drawHorizontalLine(x, x + width - 1, y, 0xFFFFFFFF);
        drawVerticalLine(x, y, y + height - 1, 0xFFFFFFFF);
        drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFF555555);
        drawVerticalLine(x + width - 1, y, y + height - 1, 0xFF555555);
        drawHorizontalLine(x + 1, x + width - 2, y + 1, 0xFFDDDDDD);
        drawVerticalLine(x + 1, y + 1, y + height - 2, 0xFFDDDDDD);
        drawHorizontalLine(x + 1, x + width - 2, y + height - 2, 0xFF888888);
        drawVerticalLine(x + width - 2, y + 1, y + height - 2, 0xFF888888);
    }

    /**
     * 矩形边框
     */
    private void drawRectBorder(int x, int y, int width, int height, int color) {
        drawHorizontalLine(x, x + width - 1, y, color);
        drawHorizontalLine(x, x + width - 1, y + height - 1, color);
        drawVerticalLine(x, y, y + height - 1, color);
        drawVerticalLine(x + width - 1, y, y + height - 1, color);
    }

    /**
     * 自定义悬停提示
     */
    private void drawCustomTooltips(int mouseX, int mouseY) {
        // 能量条悬停
        if (isPointInRegion(10, 10, 16, 60, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7c能量存储");
            tooltip.add("\u00a77" + formatNumber(te.getEnergyStored()) + " / " + formatNumber(te.getMaxEnergyStored()) + " RF");
            float percent = (float) te.getEnergyStored() / te.getMaxEnergyStored() * 100;
            tooltip.add("\u00a78" + String.format("%.1f%%", percent));
            if (te.getRFPerTick() > 0) {
                tooltip.add("");
                tooltip.add("\u00a7e发电速率: \u00a7a" + formatNumber(te.getRFPerTick()) + " RF/t");
            }
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 液体槽悬停
        if (isPointInRegion(150, 10, 16, 60, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a76燃料储罐");
            FluidStack fluid = te.getFluidTank().getFluid();
            if (fluid != null && fluid.amount > 0) {
                tooltip.add("\u00a77" + fluid.getLocalizedName());
                tooltip.add("\u00a77" + formatNumber(fluid.amount) + " / " + formatNumber(te.getFluidCapacity()) + " mB");
            } else {
                tooltip.add("\u00a78空");
                tooltip.add("\u00a77容量: " + formatNumber(te.getFluidCapacity()) + " mB");
            }
            tooltip.add("");
            tooltip.add("\u00a78可通过管道输入石油");
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 燃烧进度悬停
        if (isPointInRegion(76, 36, 24, 16, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            if (te.isBurning()) {
                tooltip.add("\u00a76燃烧中");
                float burnPercent = (float) te.getBurnTime() / te.getMaxBurnTime() * 100;
                tooltip.add("\u00a77剩余: " + String.format("%.1f%%", burnPercent));
                tooltip.add("\u00a7a+" + formatNumber(te.getRFPerTick()) + " RF/t");
            } else {
                tooltip.add("\u00a78待机");
                tooltip.add("\u00a77放入燃料开始发电");
            }
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 燃料槽悬停
        if (isPointInRegion(79, 52, 18, 18, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7e燃料槽");
            tooltip.add("\u00a77放入: 原油桶、植物油桶");
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 增速插件槽悬停
        int[][] upgradeSlots = {{43, 16}, {61, 16}, {97, 16}, {115, 16}};
        for (int i = 0; i < upgradeSlots.length; i++) {
            if (isPointInRegion(upgradeSlots[i][0], upgradeSlots[i][1], 18, 18, mouseX, mouseY)) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add("\u00a7b增速插件槽 " + (i + 1));
                tooltip.add("\u00a77每个插件: +50% 发电速度");
                tooltip.add("");
                tooltip.add("\u00a78可用材料:");
                tooltip.add("\u00a77- 增速插件");
                tooltip.add("\u00a77- 红石、萤石粉");
                tooltip.add("\u00a77- 烈焰粉、绿宝石");
                drawHoveringText(tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    /**
     * 绘制实心矩形
     */
    private void drawColorRect(int left, int top, int right, int bottom, int color) {
        if (left > right) { int t = left; left = right; right = t; }
        if (top > bottom) { int t = top; top = bottom; bottom = t; }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.color(red, green, blue, alpha);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(left, bottom, 0.0D).endVertex();
        buffer.pos(right, bottom, 0.0D).endVertex();
        buffer.pos(right, top, 0.0D).endVertex();
        buffer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    private String formatNumber(int number) {
        return numberFormat.format(number);
    }
}
