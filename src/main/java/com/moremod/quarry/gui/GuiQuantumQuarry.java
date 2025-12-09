package com.moremod.quarry.gui;

import com.moremod.quarry.QuarryMode;
import com.moremod.quarry.network.PacketHandler;
import com.moremod.quarry.network.PacketQuarryButton;
import com.moremod.quarry.network.PacketSelectBiome;
import com.moremod.quarry.tile.TileQuantumQuarry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 量子採石場 GUI - 純代碼繪製版本（不使用圖片）
 */
@SideOnly(Side.CLIENT)
public class GuiQuantumQuarry extends GuiContainer {

    private final TileQuantumQuarry tile;

    // 按鈕
    private GuiButton modeButton;
    private GuiButton redstoneButton;
    private GuiButton biomeButton;

    // 生物群系選擇器
    private boolean showBiomeSelector = false;
    private List<Biome> biomeList;
    private int biomeScrollOffset = 0;
    private static final int BIOMES_PER_PAGE = 8;

    // 顏色常量
    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_BORDER_DARK = 0xFF373737;
    private static final int COLOR_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_ENERGY_BG = 0xFF000000;
    private static final int COLOR_ENERGY = 0xFFFF0000;
    private static final int COLOR_PROGRESS = 0xFF00FF00;

    public GuiQuantumQuarry(InventoryPlayer playerInventory, TileQuantumQuarry tile) {
        super(new ContainerQuantumQuarry(playerInventory, tile));
        this.tile = tile;

        this.xSize = 176;
        this.ySize = 166;

        // 初始化生物群系列表
        biomeList = new ArrayList<>();
        for (Biome biome : ForgeRegistries.BIOMES) {
            biomeList.add(biome);
        }
        biomeList.sort(Comparator.comparing(Biome::getBiomeName));
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        // 模式按鈕 - 放在GUI下方
        modeButton = new GuiButton(0, guiLeft + 8, guiTop + ySize + 4, 50, 20, "Mode");
        buttonList.add(modeButton);

        // 紅石按鈕
        redstoneButton = new GuiButton(1, guiLeft + 63, guiTop + ySize + 4, 50, 20, "RS");
        buttonList.add(redstoneButton);

        // 生物群系按鈕
        biomeButton = new GuiButton(2, guiLeft + 118, guiTop + ySize + 4, 50, 20, "Biome");
        buttonList.add(biomeButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        System.out.println("[GuiQuantumQuarry] Button clicked: " + button.id);

        switch (button.id) {
            case 0:  // 切換模式
                System.out.println("[GuiQuantumQuarry] Sending mode packet to server");
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 0));
                break;
            case 1:  // 切換紅石控制
                System.out.println("[GuiQuantumQuarry] Sending redstone packet to server");
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 1));
                break;
            case 2:  // 顯示/隱藏生物群系選擇器
                showBiomeSelector = !showBiomeSelector;
                break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // 處理生物群系選擇
        if (showBiomeSelector && mouseButton == 0) {
            int selectorX = guiLeft + xSize + 4;
            int selectorY = guiTop;
            int selectorWidth = 100;
            int entryHeight = 12;

            if (mouseX >= selectorX && mouseX < selectorX + selectorWidth) {
                int relativeY = mouseY - selectorY;
                if (relativeY >= 0 && relativeY < BIOMES_PER_PAGE * entryHeight) {
                    int index = biomeScrollOffset + relativeY / entryHeight;
                    if (index < biomeList.size()) {
                        Biome selectedBiome = biomeList.get(index);
                        int biomeId = Biome.REGISTRY.getIDForObject(selectedBiome);
                        PacketHandler.INSTANCE.sendToServer(new PacketSelectBiome(tile.getPos(), biomeId));
                        showBiomeSelector = false;
                        return;
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if (showBiomeSelector) {
            int scroll = org.lwjgl.input.Mouse.getDWheel();
            if (scroll != 0) {
                if (scroll > 0) {
                    biomeScrollOffset = Math.max(0, biomeScrollOffset - 1);
                } else {
                    biomeScrollOffset = Math.min(Math.max(0, biomeList.size() - BIOMES_PER_PAGE), biomeScrollOffset + 1);
                }
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // ===== 主背景 =====
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, COLOR_BG);

        // 3D邊框
        drawHorizontalLine(guiLeft, guiLeft + xSize - 1, guiTop, COLOR_BORDER_LIGHT);
        drawVerticalLine(guiLeft, guiTop, guiTop + ySize - 1, COLOR_BORDER_LIGHT);
        drawHorizontalLine(guiLeft, guiLeft + xSize - 1, guiTop + ySize - 1, COLOR_BORDER_DARK);
        drawVerticalLine(guiLeft + xSize - 1, guiTop, guiTop + ySize - 1, COLOR_BORDER_DARK);

        // ===== 槽位繪製 =====
        // 附魔書槽 (8, 17)
        drawSlot(guiLeft + 8, guiTop + 17);

        // 過濾器槽 (8, 53)
        drawSlot(guiLeft + 8, guiTop + 53);

        // 輸出槽 3x6 (62, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                drawSlot(guiLeft + 62 + col * 18, guiTop + 17 + row * 18);
            }
        }

        // 玩家物品欄
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiLeft + 8 + col * 18, guiTop + 84 + row * 18);
            }
        }

        // 快捷欄
        for (int col = 0; col < 9; col++) {
            drawSlot(guiLeft + 8 + col * 18, guiTop + 142);
        }

        // ===== 能量條 =====
        int energyX = guiLeft + 30;
        int energyY = guiTop + 17;
        int energyW = 24;
        int energyH = 52;

        drawRect(energyX, energyY, energyX + energyW, energyY + energyH, COLOR_ENERGY_BG);

        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max > 0 && stored > 0) {
            int fillH = stored * (energyH - 2) / max;
            int color = stored > max / 2 ? 0xFF00FF00 : (stored > max / 4 ? 0xFFFFFF00 : COLOR_ENERGY);
            drawRect(energyX + 1, energyY + energyH - 1 - fillH, energyX + energyW - 1, energyY + energyH - 1, color);
        }

        // ===== 進度指示 =====
        int progress = tile.getProgress();
        if (progress > 0) {
            int progW = progress * 20 / 100;
            drawRect(guiLeft + 30, guiTop + 71, guiLeft + 30 + progW, guiTop + 74, COLOR_PROGRESS);
        }

        // ===== 生物群系選擇器 =====
        if (showBiomeSelector) {
            drawBiomeSelector(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題
        String title = "Quantum Quarry";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);

        // 物品欄標籤
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 0x404040);

        // 槽位標籤
        fontRenderer.drawString("E", 26, 21, 0x404040);  // 附魔
        fontRenderer.drawString("F", 26, 57, 0x404040);  // 過濾

        // 更新按鈕文本
        updateButtonLabels();

        // 狀態
        if (!tile.isStructureValid()) {
            fontRenderer.drawString("\u00a7cInvalid", 30, 76, 0xFFFFFF);
        }

        // 繪製工具提示
        drawTooltips(mouseX - guiLeft, mouseY - guiTop);
    }

    private void updateButtonLabels() {
        // 模式
        QuarryMode mode = tile.getMode();
        switch (mode) {
            case MINING: modeButton.displayString = "Mine"; break;
            case MOB_DROPS: modeButton.displayString = "Mob"; break;
            case LOOT_TABLE: modeButton.displayString = "Loot"; break;
            default: modeButton.displayString = "???"; break;
        }

        // 紅石
        redstoneButton.displayString = tile.isRedstoneControlEnabled() ? "RS:ON" : "RS:OFF";

        // 生物群系
        Biome biome = tile.getSelectedBiome();
        if (biome != null) {
            String name = biome.getBiomeName();
            biomeButton.displayString = name.length() > 5 ? name.substring(0, 4) + ".." : name;
        } else {
            biomeButton.displayString = "Biome";
        }
    }

    private void drawTooltips(int x, int y) {
        // 能量條
        if (x >= 30 && x < 54 && y >= 17 && y < 69) {
            List<String> tip = new ArrayList<>();
            tip.add("Energy: " + formatEnergy(tile.getEnergyStored()) + "/" + formatEnergy(tile.getMaxEnergyStored()) + " RF");
            drawHoveringText(tip, x, y);
        }

        // 附魔槽
        if (x >= 8 && x < 24 && y >= 17 && y < 33) {
            List<String> tip = new ArrayList<>();
            tip.add("Enchant Slot");
            tip.add("\u00a77Fortune/Looting books");
            drawHoveringText(tip, x, y);
        }

        // 過濾槽
        if (x >= 8 && x < 24 && y >= 53 && y < 69) {
            List<String> tip = new ArrayList<>();
            tip.add("Filter Slot");
            drawHoveringText(tip, x, y);
        }
    }

    private void drawBiomeSelector(int mouseX, int mouseY) {
        int x = guiLeft + xSize + 4;
        int y = guiTop;
        int w = 100;
        int h = BIOMES_PER_PAGE * 12 + 4;

        // 背景
        drawRect(x, y, x + w, y + h, 0xFF000000);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_BG);

        // 列表
        for (int i = 0; i < BIOMES_PER_PAGE; i++) {
            int idx = biomeScrollOffset + i;
            if (idx >= biomeList.size()) break;

            Biome biome = biomeList.get(idx);
            int entryY = y + 2 + i * 12;

            boolean selected = biome.equals(tile.getSelectedBiome());
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= entryY && mouseY < entryY + 12;

            if (selected) {
                drawRect(x + 1, entryY, x + w - 1, entryY + 12, 0xFF006600);
            } else if (hover) {
                drawRect(x + 1, entryY, x + w - 1, entryY + 12, 0xFF444444);
            }

            String name = biome.getBiomeName();
            if (fontRenderer.getStringWidth(name) > w - 6) {
                while (fontRenderer.getStringWidth(name + "..") > w - 6 && name.length() > 0) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "..";
            }
            fontRenderer.drawString(name, x + 3, entryY + 2, selected ? 0x00FF00 : 0xFFFFFF);
        }
    }

    private void drawSlot(int x, int y) {
        // 3D槽位效果
        drawRect(x - 1, y - 1, x + 17, y, COLOR_BORDER_DARK);
        drawRect(x - 1, y - 1, x, y + 17, COLOR_BORDER_DARK);
        drawRect(x + 16, y - 1, x + 17, y + 17, COLOR_BORDER_LIGHT);
        drawRect(x - 1, y + 16, x + 17, y + 17, COLOR_BORDER_LIGHT);
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT);
    }

    private String formatEnergy(int e) {
        if (e >= 1000000) return String.format("%.1fM", e / 1000000.0);
        if (e >= 1000) return String.format("%.0fK", e / 1000.0);
        return String.valueOf(e);
    }
}
