package com.moremod.client.gui;

import com.moremod.item.inventory.ContainerVoidBackpack;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiVoidBackpack extends GuiContainer {

    private final IInventory voidInventory;
    private final int numRows;

    /**
     * 构造函数
     * @param playerInv 玩家背包（InventoryPlayer类型，不是IInventory！）
     * @param voidInv 虚空背包库存
     * @param size 虚空背包容量
     */
    public GuiVoidBackpack(InventoryPlayer playerInv, IInventory voidInv, int size) {
        // ✅ 修复：从 playerInv 获取 player，然后传递给 Container
        super(new ContainerVoidBackpack(playerInv.player, voidInv, size));

        this.voidInventory = voidInv;
        this.numRows = size / 9;
        this.ySize = 114 + this.numRows * 18;

        System.out.println("[GuiVoidBackpack] GUI创建");
        System.out.println("[GuiVoidBackpack] 容量: " + size + " 格");
        System.out.println("[GuiVoidBackpack] 行数: " + numRows);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = "虚空背包";
        this.fontRenderer.drawString(title, 8, 6, 0x9933ff);

        // 玩家背包标题
        String invTitle = "背包";
        int playerLabelY = 18 + numRows * 18 + 14 - 10;
        this.fontRenderer.drawString(invTitle, 8, playerLabelY, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // === 绘制虚空背包区域 ===
        int voidHeight = 18 + numRows * 18;

        // 虚空背包背景
        drawRect(x, y, x + xSize, y + voidHeight, 0xFF8B8B8B);

        // 顶部标题栏
        drawGradientRect(x, y, x + xSize, y + 16, 0xFFB0B0B0, 0xFF909090);

        // 虚空背包槽位
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 17 + row * 18;
                drawVoidSlot(slotX, slotY);
            }
        }

        // 虚空背包边框
        drawBorder(x, y, xSize, voidHeight, 0xFFFFFFFF, 0xFF373737);

        // === 绘制玩家背包区域 ===
        int playerY = y + voidHeight + 7;
        int playerHeight = 96;

        // 玩家背包背景
        drawRect(x, playerY, x + xSize, playerY + playerHeight, 0xFFC6C6C6);

        // 玩家背包3行
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = playerY + 7 + row * 18;
                drawNormalSlot(slotX, slotY);
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; col++) {
            int slotX = x + 7 + col * 18;
            int slotY = playerY + 7 + 3 * 18 + 4;
            drawNormalSlot(slotX, slotY);
        }

        // 玩家背包边框
        drawBorder(x, playerY, xSize, playerHeight, 0xFFFFFFFF, 0xFF373737);
    }

    /**
     * 绘制虚空槽位（紫色调）
     */
    private void drawVoidSlot(int x, int y) {
        // 槽位背景 - 深紫色
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF2A1A3A);

        // 亮边（左上）
        drawRect(x, y, x + 17, y + 1, 0xFF555555);
        drawRect(x, y, x + 1, y + 17, 0xFF555555);

        // 暗边（右下）
        drawRect(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);

        // 紫色光晕边框
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 顶部光晕
        drawGradientRect(x + 1, y + 1, x + 17, y + 2, 0x669933FF, 0x00000000);
        // 左侧光晕
        drawGradientRect(x + 1, y + 1, x + 2, y + 17, 0x669933FF, 0x00000000);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 绘制普通槽位（灰色）
     */
    private void drawNormalSlot(int x, int y) {
        // 槽位背景 - 灰色
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);

        // 亮边（左上）
        drawRect(x, y, x + 17, y + 1, 0xFF555555);
        drawRect(x, y, x + 1, y + 17, 0xFF555555);

        // 暗边（右下）
        drawRect(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    /**
     * 绘制3D边框
     */
    private void drawBorder(int x, int y, int width, int height, int lightColor, int darkColor) {
        // 外边框 - 亮色（左上）
        drawRect(x, y, x + width, y + 1, lightColor);
        drawRect(x, y, x + 1, y + height, lightColor);

        // 外边框 - 暗色（右下）
        drawRect(x, y + height - 1, x + width, y + height, darkColor);
        drawRect(x + width - 1, y, x + width, y + height, darkColor);

        // 内边框 - 暗色（左上）
        drawRect(x + 1, y + 1, x + width - 1, y + 2, darkColor);
        drawRect(x + 1, y + 1, x + 2, y + height - 1, darkColor);

        // 内边框 - 亮色（右下）
        drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, lightColor);
        drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, lightColor);
    }
}