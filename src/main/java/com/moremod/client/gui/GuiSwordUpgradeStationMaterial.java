
package com.moremod.client.gui;

import com.moremod.container.ContainerSwordUpgradeStationMaterial;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiSwordUpgradeStationMaterial extends GuiContainer {

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("moremod", "textures/gui/sword_upgrade_station_material.png");

    private final TileEntitySwordUpgradeStationMaterial tile;

    public GuiSwordUpgradeStationMaterial(InventoryPlayer playerInv, TileEntitySwordUpgradeStationMaterial tile) {
        super(new ContainerSwordUpgradeStationMaterial(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(guiX, guiY, 0);
        float scaleX = (float) this.xSize / TEXTURE_WIDTH;
        float scaleY = (float) this.ySize / TEXTURE_HEIGHT;
        GlStateManager.scale(scaleX, scaleY, 1.0F);
        this.drawTexturedModalRect(0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        GlStateManager.popMatrix();

        // 槽位覆盖（台本体 3 槽）
        for (int i = 0; i < 3 && i < this.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (slot != null && slot.getHasStack()) {
                this.mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/generic_54.png"));
                int slotX = guiX + slot.xPos - 1;
                int slotY = guiY + slot.yPos - 1;
                this.drawTexturedModalRect(slotX, slotY, 7, 17, 18, 18);
                this.mc.getTextureManager().bindTexture(TEXTURE);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = net.minecraft.client.resources.I18n.format("container.sword_upgrade_station_material");
        this.fontRenderer.drawString(title, 8, 6, 0x404040);
        this.fontRenderer.drawString(net.minecraft.client.resources.I18n.format("container.inventory"), 8, 72, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
