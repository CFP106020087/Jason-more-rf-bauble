package com.moremod.client.gui;

import com.moremod.container.ContainerBottlingMachine;
import com.moremod.tile.TileEntityBottlingMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiBottlingMachine extends GuiContainer {

    private final TileEntityBottlingMachine tileEntity;

    // GUI尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 颜色定义
    private static final int COLOR_BACKGROUND = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PROGRESS_BG = 0xFF404040;
    private static final int COLOR_PROGRESS_FG = 0xFF00FF00;
    private static final int COLOR_FLUID_TANK_BG = 0xFF202020;
    private static final int COLOR_FLUID_TANK_BORDER = 0xFF373737;
    private static final int COLOR_TEXT = 0x404040;

    // 组件位置
    private static final int FLUID_TANK_X = 8;
    private static final int FLUID_TANK_Y = 16;
    private static final int FLUID_TANK_WIDTH = 16;
    private static final int FLUID_TANK_HEIGHT = 52;

    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 35;
    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 17;

    public GuiBottlingMachine(InventoryPlayer playerInventory, TileEntityBottlingMachine tileEntity) {
        super(new ContainerBottlingMachine(playerInventory, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // 绘制流体槽的提示
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        if (isPointInRegion(FLUID_TANK_X, FLUID_TANK_Y, FLUID_TANK_WIDTH, FLUID_TANK_HEIGHT, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            FluidTank tank = tileEntity.getFluidTank();
            if (tank.getFluid() != null) {
                tooltip.add(tank.getFluid().getLocalizedName());
                tooltip.add(String.format("%d / %d mB", tank.getFluidAmount(), tank.getCapacity()));
            } else {
                tooltip.add("空");
                tooltip.add(String.format("0 / %d mB", tank.getCapacity()));
            }
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 绘制进度条提示
        if (isPointInRegion(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            if (tileEntity.getProcessTime() > 0) {
                int percent = (int) ((float) tileEntity.getProcessTime() / tileEntity.getMaxProcessTime() * 100);
                tooltip.add(String.format("进度: %d%%", percent));
            } else {
                tooltip.add("空闲");
            }
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "装瓶机";
        this.fontRenderer.drawString(title,
                (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, COLOR_TEXT);
        this.fontRenderer.drawString("物品栏", 8, this.ySize - 96 + 2, COLOR_TEXT);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // 绘制主背景
        drawGradientBackground(guiLeft, guiTop, this.xSize, this.ySize);

        // 绘制物品槽位
        drawItemSlots(guiLeft, guiTop);

        // 绘制流体槽
        drawFluidTank(guiLeft, guiTop);

        // 绘制进度条
        drawProgressBar(guiLeft, guiTop);

        // 绘制装饰性箭头
    }

    /**
     * 绘制渐变背景
     */
    private void drawGradientBackground(int x, int y, int width, int height) {
        // 主背景
        drawRect(x, y, x + width, y + height, COLOR_BACKGROUND);

        // 顶部边框
        drawHorizontalLine(x, x + width - 1, y, 0xFF555555);
        drawHorizontalLine(x, x + width - 1, y + 1, 0xFF8B8B8B);

        // 底部边框
        drawHorizontalLine(x, x + width - 1, y + height - 2, 0xFF555555);
        drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFF000000);

        // 左边框
        drawVerticalLine(x, y, y + height - 1, 0xFF8B8B8B);
        drawVerticalLine(x + 1, y + 1, y + height - 2, 0xFFFFFFFF);

        // 右边框
        drawVerticalLine(x + width - 2, y, y + height - 1, 0xFF555555);
        drawVerticalLine(x + width - 1, y, y + height - 1, 0xFF000000);
    }

    /**
     * 绘制物品槽位
     */
    private void drawItemSlots(int guiLeft, int guiTop) {
        // 绘制所有槽位
        for (Slot slot : this.inventorySlots.inventorySlots) {
            drawSlot(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1);
        }

        // 特殊槽位标记
        // 输入槽（索引0）- 添加输入标记
        drawSlotWithIcon(guiLeft + 56 - 1, guiTop + 35 - 1, "→");

        // 输出槽（索引1）- 添加输出标记
        drawSlotWithIcon(guiLeft + 116 - 1, guiTop + 35 - 1, "←");

        // 流体容器槽（索引2）- 添加水滴标记
        drawSlotWithIcon(guiLeft + 26 - 1, guiTop + 53 - 1, "≈");
    }

    /**
     * 绘制单个槽位
     */
    private void drawSlot(int x, int y) {
        // 槽位背景（18x18）
        drawRect(x, y, x + 18, y + 18, COLOR_SLOT);

        // 凹陷效果 - 顶部和左边深色
        drawHorizontalLine(x, x + 17, y, COLOR_SLOT_DARK);
        drawVerticalLine(x, y, y + 17, COLOR_SLOT_DARK);

        // 凹陷效果 - 底部和右边亮色
        drawHorizontalLine(x + 1, x + 17, y + 17, COLOR_SLOT_LIGHT);
        drawVerticalLine(x + 17, y + 1, y + 17, COLOR_SLOT_LIGHT);

        // 内部深色区域（16x16）
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF3C3C3C);
    }

    /**
     * 绘制带图标的槽位
     */
    private void drawSlotWithIcon(int x, int y, String icon) {
        drawSlot(x, y);
        // 在槽位右下角绘制小图标
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        this.fontRenderer.drawString(icon, (x + 12) * 2, (y + 12) * 2, 0x808080);
        GlStateManager.popMatrix();
    }

    /**
     * 绘制流体槽
     */
    private void drawFluidTank(int guiLeft, int guiTop) {
        int x = guiLeft + FLUID_TANK_X;
        int y = guiTop + FLUID_TANK_Y;

        // 槽背景
        drawRect(x, y, x + FLUID_TANK_WIDTH, y + FLUID_TANK_HEIGHT, COLOR_FLUID_TANK_BG);

        // 绘制流体
        FluidTank tank = tileEntity.getFluidTank();
        if (tank.getFluid() != null && tank.getFluidAmount() > 0) {
            FluidStack fluid = tank.getFluid();
            float fillPercent = (float) tank.getFluidAmount() / tank.getCapacity();
            int fluidHeight = (int) (FLUID_TANK_HEIGHT * fillPercent);

            // 绘制流体纹理
            drawFluid(x, y + FLUID_TANK_HEIGHT - fluidHeight, FLUID_TANK_WIDTH, fluidHeight, fluid);
        }

        // 绘制刻度线
        for (int i = 1; i < 4; i++) {
            int lineY = y + (FLUID_TANK_HEIGHT * i / 4);
            drawHorizontalLine(x, x + 3, lineY, 0xFF808080);
            drawHorizontalLine(x + FLUID_TANK_WIDTH - 4, x + FLUID_TANK_WIDTH - 1, lineY, 0xFF808080);
        }

        // 槽边框
        draw3DBorder(x - 1, y - 1, FLUID_TANK_WIDTH + 2, FLUID_TANK_HEIGHT + 2, true);
    }

    /**
     * 绘制流体
     */
    private void drawFluid(int x, int y, int width, int height, FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) return;

        // 获取流体纹理
        ResourceLocation fluidStill = fluid.getFluid().getStill();
        TextureAtlasSprite sprite = null;

        if (fluidStill != null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluidStill.toString());
        }

        // 如果找不到纹理，使用纯色
        if (sprite == null || sprite == Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite()) {
            int color = fluid.getFluid().getColor();
            drawRect(x, y, x + width, y + height, color | 0xFF000000);
        } else {
            // 绑定方块纹理
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            // 设置流体颜色
            int color = fluid.getFluid().getColor();
            float r = (color >> 16 & 255) / 255.0F;
            float g = (color >> 8 & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            GlStateManager.color(r, g, b, 1.0F);

            // 绘制流体纹理（可能需要平铺）
            drawTexturedModalRect(x, y, sprite, width, height);

            // 恢复颜色
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * 绘制进度条
     */
    private void drawProgressBar(int guiLeft, int guiTop) {
        int x = guiLeft + PROGRESS_X;
        int y = guiTop + PROGRESS_Y;

        // 背景
        drawRect(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, COLOR_PROGRESS_BG);
        draw3DBorder(x - 1, y - 1, PROGRESS_WIDTH + 2, PROGRESS_HEIGHT + 2, true);

        // 进度
        if (tileEntity.getProcessTime() > 0 && tileEntity.getMaxProcessTime() > 0) {
            float progress = (float) tileEntity.getProcessTime() / tileEntity.getMaxProcessTime();
            int progressWidth = (int) (PROGRESS_WIDTH * progress);

            // 绘制进度条（渐变效果）
            drawGradientRect(x, y, x + progressWidth, y + PROGRESS_HEIGHT,
                    COLOR_PROGRESS_FG, darkenColor(COLOR_PROGRESS_FG, 0.7f));

            // 进度条动画效果（闪烁）
            if (progress < 1.0f) {
                float pulse = (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.5f + 0.5f;
                int pulseColor = mixColors(COLOR_PROGRESS_FG, 0xFFFFFF00, pulse);
                drawRect(x + progressWidth - 2, y, x + progressWidth, y + PROGRESS_HEIGHT, pulseColor);
            }
        }

        // 绘制箭头符号
        String arrow = "»»»";
        int arrowX = x + (PROGRESS_WIDTH - this.fontRenderer.getStringWidth(arrow)) / 2;
        int arrowY = y + (PROGRESS_HEIGHT - this.fontRenderer.FONT_HEIGHT) / 2;
        this.fontRenderer.drawString(arrow, arrowX, arrowY, 0x404040);
    }

    /**
     * 绘制装饰性箭头
     */


    /**
     * 绘制箭头
     */
    private void drawArrow(int x1, int y1, int x2, int y2, int color) {
        // 绘制箭头线
        drawHorizontalLine(x1, x2, y1, color);

        // 绘制箭头头部 >
        drawRect(x2 - 3, y2 - 1, x2 - 2, y2, color);
        drawRect(x2 - 2, y2 - 2, x2 - 1, y2 - 1, color);
        drawRect(x2 - 1, y2 - 3, x2, y2 - 2, color);
        drawRect(x2 - 2, y2 + 1, x2 - 1, y2 + 2, color);
        drawRect(x2 - 1, y2 + 2, x2, y2 + 3, color);
    }

    /**
     * 绘制3D边框
     */
    private void draw3DBorder(int x, int y, int width, int height, boolean inset) {
        int colorLight = inset ? COLOR_SLOT_DARK : COLOR_SLOT_LIGHT;
        int colorDark = inset ? COLOR_SLOT_LIGHT : COLOR_SLOT_DARK;

        // 顶部和左边
        drawHorizontalLine(x, x + width - 1, y, colorLight);
        drawVerticalLine(x, y, y + height - 1, colorLight);

        // 底部和右边
        drawHorizontalLine(x, x + width - 1, y + height - 1, colorDark);
        drawVerticalLine(x + width - 1, y, y + height - 1, colorDark);
    }

    /**
     * 混合两种颜色
     */
    private int mixColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 使颜色变暗
     */
    private int darkenColor(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 绘制纹理（用于流体）
     */
    public void drawTexturedModalRect(int x, int y, TextureAtlasSprite textureSprite, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, this.zLevel).tex(textureSprite.getMinU(), textureSprite.getMaxV()).endVertex();
        bufferbuilder.pos(x + width, y + height, this.zLevel).tex(textureSprite.getMaxU(), textureSprite.getMaxV()).endVertex();
        bufferbuilder.pos(x + width, y, this.zLevel).tex(textureSprite.getMaxU(), textureSprite.getMinV()).endVertex();
        bufferbuilder.pos(x, y, this.zLevel).tex(textureSprite.getMinU(), textureSprite.getMinV()).endVertex();
        tessellator.draw();
    }
}