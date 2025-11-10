package com.moremod.client.gui;

import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.container.ContainerExtractionStation;
import com.moremod.network.PacketDecomposeGem;
import com.moremod.network.PacketExtractAffix;
import com.moremod.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import com.moremod.tile.TileEntityExtractionStation;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.io.IOException;
import java.util.List;

/**
 * 完全用代码绘制的提取站GUI（兼容版本）
 * 支持多种TileEntity实现方式
 */
public class GuiExtractionStation extends GuiContainer {

    private final TileEntityExtractionStation tile;
    private final InventoryPlayer playerInv;

    private List<IdentifiedAffix> affixes;
    private int selectedIndex = -1;

    private GuiButton extractButton;
    private GuiButton decomposeButton;

    public GuiExtractionStation(InventoryPlayer playerInv, TileEntityExtractionStation tile) {
        super(new ContainerExtractionStation(playerInv, tile, playerInv.player));
        this.tile = tile;
        this.playerInv = playerInv;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        extractButton = new GuiButton(0, x + 8, y + 66, 60, 20, I18n.format("gui.extract"));
        decomposeButton = new GuiButton(1, x + 108, y + 66, 60, 20, I18n.format("gui.decompose"));

        this.buttonList.add(extractButton);
        this.buttonList.add(decomposeButton);

        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && selectedIndex >= 0) {
            PacketHandler.INSTANCE.sendToServer(new PacketExtractAffix(tile.getPos(), selectedIndex));
            selectedIndex = -1;
        } else if (button.id == 1) {
            PacketHandler.INSTANCE.sendToServer(new PacketDecomposeGem(tile.getPos()));
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
        // 绘制标题
        String title = I18n.format("container.extraction_station");
        this.fontRenderer.drawString(title, 8, 6, 4210752);

        // 绘制玩家背包标题
        this.fontRenderer.drawString(playerInv.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 4210752);

        // 绘制词条列表
        affixes = tile.getAffixes();
        if (affixes != null && !affixes.isEmpty()) {
            int y = 18;
            for (int i = 0; i < affixes.size(); i++) {
                IdentifiedAffix affix = affixes.get(i);
                String display = affix.getDisplayName();

                // 高亮选中的词条
                if (i == selectedIndex) {
                    drawRect(6, y - 1, 170, y + 9, 0x80FFFFFF);
                }

                // 绘制词条文本
                this.fontRenderer.drawString(display, 8, y, 0xFFFFFF);
                y += 11;
            }
        } else {
            // 显示提示文本
            String hint = I18n.format("gui.insert_gem");
            int hintWidth = this.fontRenderer.getStringWidth(hint);
            this.fontRenderer.drawString(hint, 88 - hintWidth / 2, 30, 0x808080);
        }

        // 显示经验消耗
        if (tile.canExtract()) {
            if (selectedIndex >= 0) {
                String xpCost = I18n.format("gui.cost") + ": 2 " + I18n.format("gui.levels");
                this.fontRenderer.drawString(xpCost, 8, 56, 0x00AA00);
            } else if (affixes != null && affixes.size() > 1) {
                String xpCost = I18n.format("gui.cost") + ": " + affixes.size() + " " + I18n.format("gui.levels");
                this.fontRenderer.drawString(xpCost, 108, 56, 0x00AA00);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // 绘制主背景
        drawBackground(x, y);

        // 绘制槽位
        drawSlot(x + 55, y + 34); // 输入槽
        drawSlot(x + 115, y + 34); // 输出槽

        // 绘制箭头（如果有物品在处理）
        if (hasItemInInputSlot()) {
            drawArrow(x + 78, y + 38);
        }
    }

    /**
     * 绘制GUI主背景
     */
    private void drawBackground(int x, int y) {
        // 外边框 - 深灰色
        drawRect(x, y, x + xSize, y + ySize, 0xFF8B8B8B);

        // 主背景 - 浅灰色
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, 0xFFC6C6C6);

        // 顶部标题栏背景 - 稍深一点
        drawGradientRect(x + 4, y + 4, x + xSize - 4, y + 16, 0xFFB0B0B0, 0xFFC6C6C6);

        // 词条列表区域背景
        drawRect(x + 4, y + 17, x + xSize - 4, y + 65, 0xFF555555);
        drawRect(x + 5, y + 18, x + xSize - 5, y + 64, 0xFF3C3C3C);

        // 玩家背包区域分隔线
        drawRect(x + 4, y + 71, x + xSize - 4, y + 72, 0xFF8B8B8B);

        // 绘制玩家背包槽位
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                drawSlot(x + 7 + j * 18, y + 83 + i * 18);
            }
        }

        // 绘制快捷栏槽位
        for (int i = 0; i < 9; i++) {
            drawSlot(x + 7 + i * 18, y + 141);
        }
    }

    /**
     * 绘制物品槽
     */
    private void drawSlot(int x, int y) {
        // 槽位外边框 - 深色
        drawRect(x, y, x + 18, y + 18, 0xFF373737);

        // 槽位内部 - 稍亮
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);

        // 槽位背景
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF555555);
    }

    /**
     * 绘制处理箭头
     */
    private void drawArrow(int x, int y) {
        // 箭头主体
        drawRect(x, y + 3, x + 22, y + 6, 0xFF00AA00);

        // 箭头头部
        drawRect(x + 17, y + 1, x + 22, y + 3, 0xFF00AA00);
        drawRect(x + 17, y + 6, x + 22, y + 8, 0xFF00AA00);

        // 箭头尖端
        drawRect(x + 22, y, x + 24, y + 2, 0xFF00AA00);
        drawRect(x + 22, y + 7, x + 24, y + 9, 0xFF00AA00);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);

        affixes = tile.getAffixes();
        if (affixes != null && !affixes.isEmpty()) {
            int x = (this.width - this.xSize) / 2;
            int y = (this.height - this.ySize) / 2;

            int listY = y + 18;
            for (int i = 0; i < affixes.size(); i++) {
                if (mouseX >= x + 6 && mouseX <= x + 170 &&
                        mouseY >= listY - 1 && mouseY <= listY + 9) {
                    selectedIndex = i;
                    updateButtons();
                    return;
                }
                listY += 11;
            }
        }
    }

    /**
     * 更新按钮状态
     */
    private void updateButtons() {
        boolean canExtract = tile.canExtract();
        extractButton.enabled = canExtract && selectedIndex >= 0;
        decomposeButton.enabled = canExtract && affixes != null && affixes.size() > 1;
    }

    /**
     * 检查输入槽是否有物品（兼容多种实现方式）
     */
    private boolean hasItemInInputSlot() {
        // 方式1：通过 IItemHandler Capability（推荐）
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                ItemStack stack = handler.getStackInSlot(0);
                return !stack.isEmpty();
            }
        }

        // 方式2：如果TileEntity有自定义的getter方法，可以这样调用
        // 例如: return tile.hasInputItem();

        // 方式3：如果TileEntity实现了IInventory接口
        // 可以直接用: return !tile.getStackInSlot(0).isEmpty();

        return false;
    }
}