package com.moremod.accessorybox;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiAccessoryBox extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/gui/accessory_box.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public GuiAccessoryBox(InventoryPlayer playerInv, EntityPlayer player) {
        super(new ContainerAccessoryBox(playerInv, player));
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;

        // 繪製背景圖 - 使用縮放方式
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiX, guiY, 0);

        float scaleX = (float) this.xSize / TEXTURE_WIDTH;
        float scaleY = (float) this.ySize / TEXTURE_HEIGHT;
        GlStateManager.scale(scaleX, scaleY, 1.0F);

        this.drawTexturedModalRect(0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        GlStateManager.popMatrix();

        // 為有物品的飾品槽位繪製覆蓋
        for (int i = 0; i < 7 && i < this.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (slot != null && slot.getHasStack()) {
                int slotX = guiX + slot.xPos - 1;
                int slotY = guiY + slot.yPos - 1;

                // 使用原版深色槽位材質覆蓋圖標
                this.mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/generic_54.png"));
                this.drawTexturedModalRect(slotX, slotY, 7, 17, 18, 18);

                // 切回原材質
                this.mc.getTextureManager().bindTexture(TEXTURE);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "額外飾品欄位";
        this.fontRenderer.drawString(title, 8, 6, 0x404040);

        // 只在空槽位顯示標籤


        // 玩家物品欄標籤
        this.fontRenderer.drawString("玩家背包", 8, this.ySize - 96 + 2, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}