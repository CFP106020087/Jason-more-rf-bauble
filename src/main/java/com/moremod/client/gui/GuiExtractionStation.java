package com.moremod.client.gui;

import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.container.ContainerExtractionStation;
import com.moremod.network.PacketDecomposeGem;
import com.moremod.network.PacketExtractAffix;
import com.moremod.network.PacketHandler;
import com.moremod.tile.TileEntityExtractionStation;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;

/**
 * 提取台 GUI - UI2布局
 * 上半：左侧输入 + 右侧词条列表
 * 下半：玩家背包
 */
public class GuiExtractionStation extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod:textures/gui/extraction_station.png");

    private final TileEntityExtractionStation tile;
    private final InventoryPlayer playerInv;
    private final ContainerExtractionStation container;

    private List<IdentifiedAffix> affixes;
    private int selectedIndex = -1;

    private GuiButton extractButton;
    private GuiButton decomposeButton;

    public GuiExtractionStation(InventoryPlayer playerInv, TileEntityExtractionStation tile) {
        super(new ContainerExtractionStation(playerInv, tile, playerInv.player));
        this.tile = tile;
        this.playerInv = playerInv;
        this.container = (ContainerExtractionStation) this.inventorySlots;

        this.xSize = 256;
        this.ySize = 400;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // 提取选中按钮（左下）
        extractButton = new GuiButton(0, x + 20, y + 280, 100, 20, "提取选中词条");

        // 全部分解按钮（右下）
        decomposeButton = new GuiButton(1, x + 135, y + 280, 100, 20, "全部分解");

        this.buttonList.add(extractButton);
        this.buttonList.add(decomposeButton);

        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && selectedIndex >= 0) {
            // 提取选中的词条
            PacketHandler.INSTANCE.sendToServer(
                    new PacketExtractAffix(tile.getPos(), selectedIndex));
            selectedIndex = -1;
        } else if (button.id == 1) {
            // 全部分解
            PacketHandler.INSTANCE.sendToServer(
                    new PacketDecomposeGem(tile.getPos()));
            selectedIndex = -1;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        updateButtons();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = "§6词条提取台";
        this.fontRenderer.drawString(title, 8, 6, 0xFFFFFF);

        // 绘制右侧词条列表
        affixes = tile.getAffixes();
        if (affixes != null && !affixes.isEmpty()) {
            // 词条列表区域（右侧）
            int listX = 195;  // 右侧列表起始X
            int listY = 50;   // 起始Y

            for (int i = 0; i < Math.min(affixes.size(), 6); i++) {
                IdentifiedAffix affix = affixes.get(i);
                String display = affix.getDisplayName();

                // 计算当前词条的Y位置（对应右侧槽位）
                int affixY = getAffixDisplayY(i);

                // 高亮选中的词条
                if (i == selectedIndex) {
                    drawRect(listX - 2, affixY - 1, listX + 58, affixY + 10, 0x8000FF00);
                }

                // 绘制词条文本（缩短显示）
                String shortDisplay = display;
                int maxWidth = 55;
                if (this.fontRenderer.getStringWidth(shortDisplay) > maxWidth) {
                    while (this.fontRenderer.getStringWidth(shortDisplay + "...") > maxWidth
                            && shortDisplay.length() > 1) {
                        shortDisplay = shortDisplay.substring(0, shortDisplay.length() - 1);
                    }
                    shortDisplay += "...";
                }

                this.fontRenderer.drawString(shortDisplay, listX, affixY, 0xFFFFFF);
            }
        } else {
            // 提示文本
            String hint = "§7放入已鉴定宝石";
            int hintX = 60;
            int hintY = 145;
            this.fontRenderer.drawString(hint, hintX, hintY, 0xFFFFFF);
        }

        // 显示经验消耗
        if (tile.canExtract()) {
            int xpY = 268;

            if (selectedIndex >= 0) {
                // 单个提取消耗
                String xpText = String.format("§a消耗: %d 级", tile.getSingleExtractXpCost());
                this.fontRenderer.drawString(xpText, 25, xpY, 0xFFFFFF);
            }

            if (affixes != null && !affixes.isEmpty()) {
                // 全部分解消耗
                String xpText = String.format("§a消耗: %d 级", tile.getFullDecomposeXpCost());
                this.fontRenderer.drawString(xpText, 140, xpY, 0xFFFFFF);
            }
        }
    }

    /**
     * 获取词条显示的Y坐标（对应右侧槽位）
     */
    private int getAffixDisplayY(int index) {
        int[] slotYs = {33, 76, 120, 167, 211, 255};
        if (index >= 0 && index < slotYs.length) {
            return slotYs[index] + 4;  // 槽位Y + 偏移，使文字居中
        }
        return 50 + index * 44;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);

        // 绘制完整贴图 256×400
        drawModalRectWithCustomSizedTexture(
                guiLeft, guiTop,
                0, 0,           // UV起点
                256, 400,       // 绘制尺寸
                256, 400        // 贴图实际尺寸
        );

        // 绘制槽位高亮框
        drawSlotFrames();
    }

    /**
     * 绘制槽位高亮框
     */
    private void drawSlotFrames() {
        // 右侧6个输出槽（贴图坐标，20×20格子）
        drawSlotFrame(189, 31, 0x6600FF00);     // R0 - 绿色提示可用
        drawSlotFrame(189, 74, 0x6600FF00);     // R1
        drawSlotFrame(190, 118, 0x6600FF00);    // R2
        drawSlotFrame(189, 165, 0x6600FF00);    // R3
        drawSlotFrame(189, 209, 0x6600FF00);    // R4
        drawSlotFrame(189, 253, 0x6600FF00);    // R5

        // 左侧输入槽
        drawSlotFrame(49, 140, 0x660000FF);     // L0 - 蓝色提示输入
    }

    /**
     * 绘制单个槽位框（20×20）
     */
    private void drawSlotFrame(int relX, int relY, int color) {
        int x = guiLeft + relX;
        int y = guiTop + relY;

        // 外光晕（使用自定义颜色）
        drawRect(x - 2, y - 2, x + 22, y + 22, color);
        // 边框
        drawRect(x - 1, y - 1, x + 21, y + 21, 0xFF111111);
        // 内部
        drawRect(x, y, x + 20, y + 20, 0xFF333333);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);

        affixes = tile.getAffixes();
        if (affixes != null && !affixes.isEmpty()) {
            int x = guiLeft;
            int y = guiTop;

            // 检查点击的是否在词条列表区域
            int listX = x + 193;

            for (int i = 0; i < Math.min(affixes.size(), 6); i++) {
                int affixY = y + getAffixDisplayY(i);

                // 点击区域判定（60宽，12高）
                if (mouseX >= listX && mouseX <= listX + 60 &&
                        mouseY >= affixY - 1 && mouseY <= affixY + 11) {
                    selectedIndex = i;
                    updateButtons();
                    return;
                }
            }
        }
    }

    /**
     * 更新按钮状态
     */
    private void updateButtons() {
        boolean canExtract = tile.canExtract();
        affixes = tile.getAffixes();

        // 提取按钮：需要选中词条
        extractButton.enabled = canExtract && selectedIndex >= 0;

        // 分解按钮：需要有词条
        decomposeButton.enabled = canExtract && affixes != null && !affixes.isEmpty();
    }
}