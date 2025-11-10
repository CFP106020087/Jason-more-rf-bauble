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
    private final ResourceLocation texture;
    private final int tier;
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public GuiAccessoryBox(InventoryPlayer playerInv, EntityPlayer player, int tier) {
        super(new ContainerAccessoryBox(playerInv, player, tier));
        this.tier = tier;
        this.xSize = 176;
        this.ySize = 166;

        // 根据tier选择不同的纹理
        String tierName = getTierName();
        this.texture = new ResourceLocation("moremod", "textures/gui/accessory_box_" + tierName + ".png");
    }

    // 兼容旧版本的构造函数（默认tier 3）
    public GuiAccessoryBox(InventoryPlayer playerInv, EntityPlayer player) {
        this(playerInv, player, 3);
    }

    private String getTierName() {
        switch(tier) {
            case 1: return "t1";
            case 2: return "t2";
            case 3: return "t3";
            default: return "t3";
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);

        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;

        // 所有tier都使用统一的缩放方式
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiX, guiY, 0);

        float scaleX = (float) this.xSize / TEXTURE_WIDTH;
        float scaleY = (float) this.ySize / TEXTURE_HEIGHT;
        GlStateManager.scale(scaleX, scaleY, 1.0F);

        this.drawTexturedModalRect(0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        GlStateManager.popMatrix();

        // 为有物品的饰品槽位绘制覆盖
        int slotCount = getSlotCountForTier();
        for (int i = 0; i < slotCount && i < this.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (slot != null && slot.getHasStack()) {
                int slotX = guiX + slot.xPos - 1;
                int slotY = guiY + slot.yPos - 1;

                // 使用原版深色槽位材质覆盖图标
                this.mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/generic_54.png"));
                this.drawTexturedModalRect(slotX, slotY, 7, 17, 18, 18);

                // 切回原材质
                this.mc.getTextureManager().bindTexture(texture);
            }
        }
    }

    private int getSlotCountForTier() {
        switch(tier) {
            case 1: return 3;
            case 2: return 5;
            case 3: return 8;
            default: return 8;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = getTitle();

        // 标题居中或左对齐
        int titleX = (tier == 3) ? 8 : (this.xSize - this.fontRenderer.getStringWidth(title)) / 2;
        this.fontRenderer.drawString(title, titleX, 6, 0x404040);

        // 根据tier绘制不同的槽位标签
        drawSlotLabels();

        // 玩家物品栏标签
        this.fontRenderer.drawString("物品栏", 8, this.ySize - 96 + 2, 0x404040);
    }

    private String getTitle() {
        switch(tier) {
            case 1: return "配饰盒 - 基础";
            case 2: return "配饰盒 - 进阶";
            case 3: return "配饰盒 - 高级";
            default: return "配饰盒";
        }
    }

    private void drawSlotLabels() {
        // 可选：为每个槽位添加小标签
        // 如果不需要标签，这个方法可以留空
        switch(tier) {
            case 1:
                // T1: 3个槽位的标签
                break;
            case 2:
                // T2: 5个槽位的标签
                break;
            case 3:
                // T3: 8个槽位的标签
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}