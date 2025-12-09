package com.moremod.client.gui;

import com.moremod.container.ContainerFakePlayerActivator;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketFakePlayerActivatorConfig;
import com.moremod.tile.TileEntityFakePlayerActivator;
import com.moremod.tile.TileEntityFakePlayerActivator.Mode;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * 假玩家激活器 GUI（代码绘制）
 */
@SideOnly(Side.CLIENT)
public class GuiFakePlayerActivator extends GuiContainer {

    private final TileEntityFakePlayerActivator tile;

    // 按钮 ID
    private static final int BTN_MODE = 0;
    private static final int BTN_INTERVAL_DOWN = 1;
    private static final int BTN_INTERVAL_UP = 2;
    private static final int BTN_CHUNK_LOAD = 3;

    // 颜色常量
    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_DARK = 0xFF555555;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_ENERGY_BG = 0xFF1A1A1A;
    private static final int COLOR_ENERGY = 0xFFCC0000;
    private static final int COLOR_ACTIVE = 0xFF00FF00;
    private static final int COLOR_INACTIVE = 0xFFFF0000;

    public GuiFakePlayerActivator(InventoryPlayer playerInv, TileEntityFakePlayerActivator tile) {
        super(new ContainerFakePlayerActivator(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 184;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = guiLeft;
        int y = guiTop;

        // 模式按钮
        this.buttonList.add(new GuiButton(BTN_MODE, x + 100, y + 30, 68, 20, ""));

        // 间隔调整按钮
        this.buttonList.add(new GuiButton(BTN_INTERVAL_DOWN, x + 100, y + 52, 20, 16, "-"));
        this.buttonList.add(new GuiButton(BTN_INTERVAL_UP, x + 148, y + 52, 20, 16, "+"));

        // 区块加载按钮
        this.buttonList.add(new GuiButton(BTN_CHUNK_LOAD, x + 100, y + 70, 68, 16, ""));

        updateButtonText();
    }

    private void updateButtonText() {
        // 模式按钮
        buttonList.get(0).displayString = tile.getCurrentMode().getDisplayName();

        // 区块加载按钮
        buttonList.get(3).displayString = tile.isChunkLoadingEnabled() ? "区块加载: 开" : "区块加载: 关";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_MODE:
                PacketHandler.INSTANCE.sendToServer(new PacketFakePlayerActivatorConfig(
                    tile.getPos(), PacketFakePlayerActivatorConfig.Action.CYCLE_MODE, 0
                ));
                break;
            case BTN_INTERVAL_DOWN:
                PacketHandler.INSTANCE.sendToServer(new PacketFakePlayerActivatorConfig(
                    tile.getPos(), PacketFakePlayerActivatorConfig.Action.ADJUST_INTERVAL, -1
                ));
                break;
            case BTN_INTERVAL_UP:
                PacketHandler.INSTANCE.sendToServer(new PacketFakePlayerActivatorConfig(
                    tile.getPos(), PacketFakePlayerActivatorConfig.Action.ADJUST_INTERVAL, 1
                ));
                break;
            case BTN_CHUNK_LOAD:
                PacketHandler.INSTANCE.sendToServer(new PacketFakePlayerActivatorConfig(
                    tile.getPos(), PacketFakePlayerActivatorConfig.Action.TOGGLE_CHUNK_LOAD, 0
                ));
                break;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonText();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.disableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = guiLeft;
        int y = guiTop;

        // 主背景
        drawRect(x, y, x + xSize, y + ySize, COLOR_BG);

        // 边框
        drawRect(x, y, x + xSize, y + 2, COLOR_DARK); // 上
        drawRect(x, y + ySize - 2, x + xSize, y + ySize, COLOR_DARK); // 下
        drawRect(x, y, x + 2, y + ySize, COLOR_DARK); // 左
        drawRect(x + xSize - 2, y, x + xSize, y + ySize, COLOR_DARK); // 右

        // 假玩家核心槽位（中上）
        drawSlot(x + 79, y + 7);

        // 工具槽 3x3（左侧）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(x + 7 + col * 18, y + 29 + row * 18);
            }
        }

        // 能量条（左下角）
        drawEnergyBar(x + 8, y + 85, 52, 10);

        // 玩家背包区域分隔线
        drawRect(x + 7, y + 98, x + 169, y + 99, COLOR_DARK);

        // 玩家背包槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + 101 + row * 18);
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + 159);
        }

        GlStateManager.enableTexture2D();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        fontRenderer.drawString("假玩家激活器", 8, 6, 0x404040);

        // 状态指示
        String status = tile.isActive() ? "运行中" : "待机";
        int statusColor = tile.isActive() ? 0x00AA00 : 0xAA0000;
        fontRenderer.drawString(status, 100, 8, statusColor);

        // 间隔显示
        String intervalText = "间隔: " + tile.getActionInterval() + " tick";
        fontRenderer.drawString(intervalText, 120, 56, 0x404040);

        // 能量显示
        String energyText = tile.getEnergyStored() + " / " + tile.getMaxEnergy() + " RF";
        fontRenderer.drawString(energyText, 8, 76, 0x404040);

        // 模式能耗显示
        String costText = "能耗: " + tile.getCurrentMode().getEnergyCost() + " RF";
        fontRenderer.drawString(costText, 100, 88, 0x404040);
    }

    /**
     * 绘制槽位
     */
    private void drawSlot(int x, int y) {
        // 槽位背景
        drawRect(x, y, x + 18, y + 18, COLOR_SLOT);
        // 内部凹陷效果
        drawRect(x + 1, y + 1, x + 17, y + 17, COLOR_DARK);
        drawRect(x + 1, y + 1, x + 17, y + 2, 0xFF373737);
        drawRect(x + 1, y + 1, x + 2, y + 17, 0xFF373737);
    }

    /**
     * 绘制能量条
     */
    private void drawEnergyBar(int x, int y, int width, int height) {
        // 背景
        drawRect(x, y, x + width, y + height, COLOR_ENERGY_BG);

        // 能量条
        float energyRatio = (float) tile.getEnergyStored() / tile.getMaxEnergy();
        int energyWidth = (int) (width * energyRatio);
        if (energyWidth > 0) {
            // 渐变效果
            int energyColor = energyRatio > 0.5f ? 0xFF00CC00 : (energyRatio > 0.2f ? 0xFFCCCC00 : 0xFFCC0000);
            drawRect(x, y, x + energyWidth, y + height, energyColor);
        }

        // 边框
        drawRect(x, y, x + width, y + 1, COLOR_DARK);
        drawRect(x, y + height - 1, x + width, y + height, COLOR_DARK);
        drawRect(x, y, x + 1, y + height, COLOR_DARK);
        drawRect(x + width - 1, y, x + width, y + height, COLOR_DARK);
    }
}
