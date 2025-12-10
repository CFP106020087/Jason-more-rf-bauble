package com.moremod.printer;

import com.moremod.moremod;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 打印机GUI
 */
@SideOnly(Side.CLIENT)
public class GuiPrinter extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(moremod.MODID, "textures/gui/printer.png");
    private final TileEntityPrinter tile;

    public GuiPrinter(InventoryPlayer playerInventory, TileEntityPrinter tile) {
        super(new ContainerPrinter(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("container.moremod.printer");
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);

        // 状态信息
        int statusY = 6;
        int statusX = this.xSize - 8;

        // 多方块状态
        String multiblockStatus = tile.isMultiblockFormed() ?
            "\u00a7a\u2714" :  // 绿色勾
            "\u00a7c\u2718";   // 红色叉
        this.fontRenderer.drawString(multiblockStatus, statusX - this.fontRenderer.getStringWidth(multiblockStatus), statusY, 0xFFFFFF);

        // 能量显示
        String energyText = formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF";
        int energyWidth = this.fontRenderer.getStringWidth(energyText);

        // 进度显示
        if (tile.isProcessing()) {
            int progress = tile.getProgress();
            int maxProgress = tile.getMaxProgress();
            String progressText = String.format("%.1f%%", (progress * 100.0 / maxProgress));
            this.fontRenderer.drawString(progressText, 134 - this.fontRenderer.getStringWidth(progressText) / 2, 55, 0x00AA00);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        // 绘制能量条
        int energyHeight = getEnergyBarHeight();
        if (energyHeight > 0) {
            this.drawTexturedModalRect(x + 8, y + 17 + (52 - energyHeight), 176, 52 - energyHeight, 14, energyHeight);
        }

        // 绘制进度箭头
        if (tile.isProcessing()) {
            int progressWidth = getProgressWidth();
            this.drawTexturedModalRect(x + 116, y + 35, 176, 52, progressWidth, 16);
        }

        // 如果多方块未形成，绘制警告覆盖
        if (!tile.isMultiblockFormed()) {
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 0.3F, 0.3F, 0.3F);
            this.drawTexturedModalRect(x + 61, y + 16, 0, 166, 56, 56);
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private int getEnergyBarHeight() {
        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max == 0) return 0;
        return stored * 52 / max;
    }

    private int getProgressWidth() {
        int progress = tile.getProgress();
        int maxProgress = tile.getMaxProgress();
        if (maxProgress == 0) return 0;
        return progress * 14 / maxProgress;
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    /**
     * 绘制tooltip
     */
    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        super.renderHoveredToolTip(mouseX, mouseY);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // 能量条tooltip
        if (mouseX >= x + 8 && mouseX <= x + 22 && mouseY >= y + 17 && mouseY <= y + 69) {
            String text = formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF";
            this.drawHoveringText(text, mouseX, mouseY);
        }

        // 多方块状态tooltip
        if (!tile.isMultiblockFormed() && mouseX >= x + 61 && mouseX <= x + 117 && mouseY >= y + 16 && mouseY <= y + 72) {
            this.drawHoveringText(I18n.format("gui.moremod.printer.multiblock_required"), mouseX, mouseY);
        }
    }
}
