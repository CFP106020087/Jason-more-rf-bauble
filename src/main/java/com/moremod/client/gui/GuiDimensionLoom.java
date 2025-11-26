package com.moremod.client.gui;

import com.moremod.tile.TileEntityDimensionLoom;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiDimensionLoom extends GuiContainer {

    private final InventoryPlayer playerInventory;
    private final TileEntityDimensionLoom tileEntity;

    public GuiDimensionLoom(InventoryPlayer playerInventory, TileEntityDimensionLoom tileEntity) {
        super(new ContainerDimensionLoom(playerInventory, tileEntity));
        this.playerInventory = playerInventory;
        this.tileEntity = tileEntity;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = this.tileEntity.getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 0x1A1A1A);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 0x1A1A1A);


    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // 绘制背景（灰色）
        drawRect(i, j, i + this.xSize, j + this.ySize, 0xFFC6C6C6);
        drawRect(i + 3, j + 3, i + this.xSize - 3, j + this.ySize - 3, 0xFF8B8B8B);
        drawRect(i + 4, j + 4, i + this.xSize - 4, j + 83, 0xFF999999);

        // 绘制标题区域
        drawRect(i + 4, j + 5, i + this.xSize - 4, j + 15, 0xFF4B4B4B);

        // 只绘制格子边框，不绘制背景
        // 3x3输入格子
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = i + 29 + col * 18;
                int y = j + 16 + row * 18;
                // 深色边框
                drawRect(x, y, x + 18, y + 1, 0xFF373737);  // 上
                drawRect(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);  // 下
                drawRect(x, y, x + 1, y + 18, 0xFF373737);  // 左
                drawRect(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);  // 右
                // 内部
                drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
            }
        }

        // 输出格子（稍大）
        int outX = i + 123;
        int outY = j + 34;
        drawRect(outX - 1, outY - 1, outX + 19, outY + 19, 0xFF373737);
        drawRect(outX, outY, outX + 18, outY + 18, 0xFF8B8B8B);

        // 进度条背景
        drawRect(i + 90, j + 35, i + 114, j + 45, 0xFF000000);
        drawRect(i + 91, j + 36, i + 113, j + 44, 0xFF8B8B8B);

        // 进度条填充
        if (this.tileEntity.getProcessTime() > 0) {
            int progress = this.getProcessProgressScaled(22);
            drawRect(i + 91, j + 36, i + 91 + progress, j + 44, 0xFF00AA00);
        }

        // 玩家背包格子
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = i + 7 + col * 18;
                int y = j + 83 + row * 18;
                drawRect(x, y, x + 18, y + 1, 0xFF373737);
                drawRect(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
                drawRect(x, y, x + 1, y + 18, 0xFF373737);
                drawRect(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
                drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
            }
        }

        // 快捷栏格子
        for (int col = 0; col < 9; col++) {
            int x = i + 7 + col * 18;
            int y = j + 141;
            drawRect(x, y, x + 18, y + 1, 0xFF373737);
            drawRect(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
            drawRect(x, y, x + 1, y + 18, 0xFF373737);
            drawRect(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
            drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        }
    }

    private int getProcessProgressScaled(int pixels) {
        int processTime = this.tileEntity.getField(0);
        int maxProcessTime = this.tileEntity.getField(1);
        return maxProcessTime != 0 && processTime != 0 ? processTime * pixels / maxProcessTime : 0;
    }
}