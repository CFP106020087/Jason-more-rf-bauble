package com.moremod.client.gui;

import com.moremod.container.ContainerTransferStation;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketTransferGem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiTransferStationCodeDrawn extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod:textures/gui/transfer_station.png");

    private final ContainerTransferStation container;
    private final InventoryPlayer playerInv;
    private GuiButton transferButton;

    public GuiTransferStationCodeDrawn(InventoryPlayer playerInv, ContainerTransferStation container) {
        super(container);
        this.container = container;
        this.playerInv = playerInv;

        this.xSize = 256;
        this.ySize = 338;  // 快捷栏底部320+18=338
    }

    @Override
    public void initGui() {
        super.initGui();

        // 按钮位置：指定坐标
        int btnX = this.guiLeft + 88;
        int btnY = this.guiTop + 170;

        this.transferButton = new GuiButton(0, btnX, btnY, 80, 20, "转移词条");
        this.buttonList.add(transferButton);

        updateButtonState();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonState();
    }

    private void updateButtonState() {
        if (transferButton != null) {
            transferButton.enabled = container.canTransfer();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && button.enabled) {
            // 发包到服务端，让 Container / Tile 执行 performTransfer
            PacketHandler.INSTANCE.sendToServer(new PacketTransferGem());
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);

        // 绘制完整贴图 256×344（GUI高度347，底部3px自然留空）
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 256, 344, 256, 344);

        // 高亮显示四个关键槽位边框（20×20格子，贴图左上角位置）
        drawSlotFrame(38, 61);    // 源（贴图格子位置保持不变）
        drawSlotFrame(119, 137);  // 输出
        drawSlotFrame(199, 61);   // 目标（贴图格子位置保持不变）
        drawSlotFrame(45, 195);   // 符文
    }

    private void drawSlotFrame(int relX, int relY) {
        int x = guiLeft + relX;
        int y = guiTop + relY;

        // 外光晕（20×20格子 + 2px外框）
        drawRect(x - 2, y - 2, x + 22, y + 22, 0x6600FFFF);
        // 边框（20×20格子 + 1px）
        drawRect(x - 1, y - 1, x + 21, y + 21, 0xFF111111);
        // 内部（20×20格子）
        drawRect(x, y, x + 20, y + 20, 0xFF333333);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = "§6词条转移台";
        this.fontRenderer.drawString(title, 8, 6, 0xFFFFFF);

        // 状态信息显示位置
        int statusY = 230;  // 往上移动
        if (container.canTransfer()) {
            this.fontRenderer.drawString("§a✓ 可以转移", 10, statusY, 0xFFFFFF);
        } else {
            String error = container.getErrorMessage();
            if (error == null || error.isEmpty()) {
                error = "§7放入合适的物品开始";
            }
            this.fontRenderer.drawString("§c✗ " + error, 10, statusY, 0xFFFFFF);
        }
    }
}