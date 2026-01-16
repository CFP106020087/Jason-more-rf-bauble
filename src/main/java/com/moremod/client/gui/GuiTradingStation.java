package com.moremod.client.gui;

import com.moremod.client.gui.component.GuiEnergyBar;
import com.moremod.client.gui.component.GuiTradePanel;
import com.moremod.client.gui.component.GuiRenderUtils;
import com.moremod.client.gui.component.GuiSlot;
import com.moremod.container.ContainerTradingStation;
import com.moremod.network.MessageChangeTradeIndex;
import com.moremod.network.NetworkHandler;
import com.moremod.tile.TileTradingStation;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import java.io.IOException;

/**
 * 🏪 村民交易機GUI - 顯示交易預覽
 * ✅ 修复: 支持显示双输入槽位
 */
public class GuiTradingStation extends GuiContainer {

    private final TileTradingStation tile;
    private final EntityPlayer player;

    // GUI 組件
    private GuiEnergyBar energyBar;
    private GuiTradePanel leftPanel;
    private GuiTradePanel centerPanel;
    private GuiTradePanel rightPanel;
    private GuiSlot villagerSlot;
    private GuiSlot inputSlot1;   // ✅ 输入槽1
    private GuiSlot inputSlot2;   // ✅ 输入槽2
    private GuiSlot outputSlot;

    // 按鈕
    private GuiButton prevButton;
    private GuiButton nextButton;

    public GuiTradingStation(EntityPlayer player, TileTradingStation tile) {
        super(new ContainerTradingStation(player, tile));
        this.player = player;
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 207;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // 初始化組件
        initializeComponents(x, y);

        // 初始化按鈕
        initializeButtons(x, y);
    }

    private void initializeComponents(int x, int y) {
        // 能量條
        energyBar = new GuiEnergyBar(x + 150, y + 25, 14, 75);

        // 面板
        leftPanel = new GuiTradePanel(x + 5, y + 20, 70, 85);
        centerPanel = new GuiTradePanel(x + 80, y + 20, 60, 85);
        rightPanel = new GuiTradePanel(x + 145, y + 20, 25, 85);

        // ✅ 槽位 - 对应Container中的位置
        villagerSlot = new GuiSlot(x + 25, y + 52);   // Container: (26, 53)
        inputSlot1 = new GuiSlot(x + 25, y + 88);     // Container: (26, 89) - 输入槽1
        inputSlot2 = new GuiSlot(x + 49, y + 88);     // Container: (50, 89) - 输入槽2
        outputSlot = new GuiSlot(x + 115, y + 88);    // Container: (116, 89) - 输出槽
    }

    private void initializeButtons(int x, int y) {
        // 上一個交易按鈕
        prevButton = new GuiButton(0, x + 50, y + 35, 20, 20, "<");
        this.buttonList.add(prevButton);

        // 下一個交易按鈕
        nextButton = new GuiButton(1, x + 86, y + 35, 20, 20, ">");
        this.buttonList.add(nextButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        System.out.println("[GUI] 按鈕被點擊: ID=" + button.id);

        switch (button.id) {
            case 0:
                System.out.println("[GUI] 發送上一個交易消息");
                NetworkHandler.CHANNEL.sendToServer(
                        new MessageChangeTradeIndex(tile.getPos(), false));
                break;
            case 1:
                System.out.println("[GUI] 發送下一個交易消息");
                NetworkHandler.CHANNEL.sendToServer(
                        new MessageChangeTradeIndex(tile.getPos(), true));
                break;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // 主背景
        GuiRenderUtils.drawRect(x, y, x + xSize, y + ySize, GuiRenderUtils.COLOR_BG);

        // 標題欄
        GuiRenderUtils.drawRect(x, y, x + xSize, y + 16, GuiRenderUtils.COLOR_DARK);

        // 繪製面板
        leftPanel.draw();
        centerPanel.draw();
        rightPanel.draw();

        // ✅ 繪製槽位 - 包括两个输入槽
        villagerSlot.draw();
        inputSlot1.draw();
        inputSlot2.draw();
        outputSlot.draw();

        // 繪製能量條
        energyBar.setEnergy(tile.getEnergyStored(), tile.getMaxEnergyStored());
        energyBar.draw();

        // 背包區域
        GuiRenderUtils.drawRect(x + 7, y + 110, x + xSize - 7, y + ySize - 7, GuiRenderUtils.COLOR_PANEL);

        // 邊框
        GuiRenderUtils.drawBorder(x, y, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題
        String title = I18n.format("container.moremod.trading_station", "村民交易機");
        this.fontRenderer.drawString(title, 8, 6, GuiRenderUtils.COLOR_TEXT);

        // 背包標題
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 113, GuiRenderUtils.COLOR_TEXT);

        // 商人狀態指示 (✅ 支持村民和流浪商人)
        if (tile.hasMerchant()) {
            this.fontRenderer.drawString("✓", 30, 57, GuiRenderUtils.COLOR_GREEN);
        } else {
            this.fontRenderer.drawString("✗", 30, 57, GuiRenderUtils.COLOR_RED);
        }

        // 交易預覽
        drawTradePreview();

        // 能量百分比
        float percentage = (float) tile.getEnergyStored() / tile.getMaxEnergyStored() * 100;
        String energyText = String.format("%.0f%%", percentage);
        this.fontRenderer.drawString(energyText, 155, 103, GuiRenderUtils.COLOR_TEXT);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // 能量條 tooltip
        if (energyBar.isMouseOver(mouseX - guiLeft, mouseY - guiTop)) {
            drawHoveringText(energyBar.getTooltip(), mouseX, mouseY);
        }

        // 交易物品 tooltip
        renderTradeItemTooltips(mouseX, mouseY);
    }

    /**
     * ✅ 修复: 繪製交易預覽 - 支持显示两个输入物品
     * ✅ 支持流浪商人等所有 IMerchant 實體
     */
    private void drawTradePreview() {
        // ✅ 使用 createMerchantFromNBT 而非 createVillagerFromNBT
        IMerchant merchant = tile.createMerchantFromNBT();
        if (merchant == null) {
            this.fontRenderer.drawString("無商人", 85, 50, GuiRenderUtils.COLOR_RED);
            return;
        }

        MerchantRecipeList recipes = merchant.getRecipes(null);
        if (recipes == null || recipes.isEmpty()) {
            this.fontRenderer.drawString("無交易", 95, 50, GuiRenderUtils.COLOR_RED);
            return;
        }

        int index = tile.getCurrentTradeIndex();
        if (index >= recipes.size()) {
            index = 0;
        }

        // 顯示交易編號
        String tradeText = String.format("交易 %d/%d", index + 1, recipes.size());
        int textWidth = this.fontRenderer.getStringWidth(tradeText);
        this.fontRenderer.drawString(tradeText, 110 - textWidth / 2, 25, GuiRenderUtils.COLOR_TEXT);

        // 獲取當前交易
        MerchantRecipe recipe = recipes.get(index);

        // 渲染交易物品
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        RenderHelper.enableGUIStandardItemLighting();

        // ✅ 输入物品1 (左侧中间位置)
        if (recipe.getItemToBuy() != null && !recipe.getItemToBuy().isEmpty()) {
            this.itemRender.renderItemAndEffectIntoGUI(recipe.getItemToBuy(), 80, 60);
            this.itemRender.renderItemOverlayIntoGUI(
                    this.fontRenderer, recipe.getItemToBuy(), 80, 60, null);
        }

        // ✅ 输入物品2 (如果有第二个输入,显示在第一个下方)
        if (recipe.hasSecondItemToBuy() &&
                recipe.getSecondItemToBuy() != null &&
                !recipe.getSecondItemToBuy().isEmpty()) {
            this.itemRender.renderItemAndEffectIntoGUI(recipe.getSecondItemToBuy(), 80, 78);
            this.itemRender.renderItemOverlayIntoGUI(
                    this.fontRenderer, recipe.getSecondItemToBuy(), 80, 78, null);
        }

        // ✅ 箭頭 - 指向输出
        this.fontRenderer.drawString("→", 100, 66, GuiRenderUtils.COLOR_TEXT);

        // ✅ 輸出物品預覽 - 与输出槽对齐
        if (recipe.getItemToSell() != null && !recipe.getItemToSell().isEmpty()) {
            this.itemRender.renderItemAndEffectIntoGUI(recipe.getItemToSell(), 118, 60);
            this.itemRender.renderItemOverlayIntoGUI(
                    this.fontRenderer, recipe.getItemToSell(), 118, 60, null);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /**
     * ✅ 修复: 渲染交易物品的 Tooltip - 支持两个输入物品
     * ✅ 支持流浪商人等所有 IMerchant 實體
     */
    private void renderTradeItemTooltips(int mouseX, int mouseY) {
        if (!tile.hasMerchant()) return;

        // ✅ 使用 createMerchantFromNBT 而非 createVillagerFromNBT
        IMerchant merchant = tile.createMerchantFromNBT();
        if (merchant == null) return;

        MerchantRecipeList recipes = merchant.getRecipes(null);
        if (recipes == null || recipes.isEmpty()) return;

        int index = tile.getCurrentTradeIndex();
        if (index >= recipes.size()) return;

        MerchantRecipe recipe = recipes.get(index);

        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;

        // ✅ 输入物品1 tooltip
        if (x >= 80 && x < 80+16 && y >= 60 && y < 60+16) {
            if (recipe.getItemToBuy() != null && !recipe.getItemToBuy().isEmpty()) {
                renderToolTip(recipe.getItemToBuy(), mouseX, mouseY);
            }
        }

        // ✅ 输入物品2 tooltip
        if (x >= 80 && x < 80+16 && y >= 78 && y < 78+16) {
            if (recipe.hasSecondItemToBuy() && recipe.getSecondItemToBuy() != null) {
                renderToolTip(recipe.getSecondItemToBuy(), mouseX, mouseY);
            }
        }

        // ✅ 輸出物品預覽 tooltip
        if (x >= 118 && x < 118+16 && y >= 60 && y < 60+16) {
            if (recipe.getItemToSell() != null && !recipe.getItemToSell().isEmpty()) {
                renderToolTip(recipe.getItemToSell(), mouseX, mouseY);
            }
        }
    }
}
