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
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 量子采石场 GUI
 */
@SideOnly(Side.CLIENT)
public class GuiQuantumQuarry extends GuiContainer {
    
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/gui/quantum_quarry.png");
    
    private final TileQuantumQuarry tile;
    private final ContainerQuantumQuarry container;
    
    // 按钮
    private GuiButton modeButton;
    private GuiButton redstoneButton;
    private GuiButton biomeButton;
    
    // 生物群系选择器
    private boolean showBiomeSelector = false;
    private List<Biome> biomeList;
    private int biomeScrollOffset = 0;
    private static final int BIOMES_PER_PAGE = 8;
    
    public GuiQuantumQuarry(InventoryPlayer playerInventory, TileQuantumQuarry tile) {
        super(new ContainerQuantumQuarry(playerInventory, tile));
        this.tile = tile;
        this.container = (ContainerQuantumQuarry) inventorySlots;
        
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
        
        // 模式切换按钮
        modeButton = new GuiButton(0, guiLeft + 30, guiTop + 17, 26, 16, "");
        buttonList.add(modeButton);
        
        // 红石控制按钮
        redstoneButton = new GuiButton(1, guiLeft + 30, guiTop + 35, 26, 16, "");
        buttonList.add(redstoneButton);
        
        // 生物群系选择按钮
        biomeButton = new GuiButton(2, guiLeft + 30, guiTop + 53, 26, 16, "Biome");
        buttonList.add(biomeButton);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:  // 切换模式
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 0));
                break;
            case 1:  // 切换红石控制
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 1));
                break;
            case 2:  // 显示/隐藏生物群系选择器
                showBiomeSelector = !showBiomeSelector;
                break;
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // 处理生物群系选择
        if (showBiomeSelector) {
            int selectorX = guiLeft + xSize;
            int selectorY = guiTop + 10;
            int selectorWidth = 120;
            int entryHeight = 14;
            
            if (mouseX >= selectorX && mouseX < selectorX + selectorWidth) {
                int relativeY = mouseY - selectorY;
                if (relativeY >= 0 && relativeY < BIOMES_PER_PAGE * entryHeight) {
                    int index = biomeScrollOffset + relativeY / entryHeight;
                    if (index < biomeList.size()) {
                        Biome selectedBiome = biomeList.get(index);
                        int biomeId = Biome.REGISTRY.getIDForObject(selectedBiome);
                        PacketHandler.INSTANCE.sendToServer(new PacketSelectBiome(tile.getPos(), biomeId));
                        showBiomeSelector = false;
                    }
                }
            }
        }
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        // 处理生物群系列表滚动
        if (showBiomeSelector) {
            int scroll = org.lwjgl.input.Mouse.getDWheel();
            if (scroll != 0) {
                if (scroll > 0) {
                    biomeScrollOffset = Math.max(0, biomeScrollOffset - 1);
                } else {
                    biomeScrollOffset = Math.min(biomeList.size() - BIOMES_PER_PAGE, biomeScrollOffset + 1);
                }
            }
        }
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        
        // 绘制主背景
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        
        // 绘制能量条
        int energyHeight = getEnergyBarHeight();
        drawTexturedModalRect(guiLeft + 152, guiTop + 17 + (52 - energyHeight), 
            176, 52 - energyHeight, 16, energyHeight);
        
        // 绘制进度条
        int progress = tile.getProgress();
        int progressWidth = progress * 22 / 100;
        drawTexturedModalRect(guiLeft + 62, guiTop + 35, 176, 52, progressWidth, 16);
        
        // 绘制生物群系选择器
        if (showBiomeSelector) {
            drawBiomeSelector(mouseX, mouseY);
        }
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = I18n.format("tile.quantum_quarry.name");
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        
        // 玩家物品栏标题
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 0x404040);
        
        // 更新按钮文本
        updateButtonText();
        
        // 状态信息
        drawStatusInfo();
        
        // 绘制工具提示
        drawTooltips(mouseX, mouseY);
    }
    
    private void updateButtonText() {
        // 模式按钮
        QuarryMode mode = tile.getMode();
        String modeText;
        switch (mode) {
            case MINING: modeText = "Mine"; break;
            case MOB_DROPS: modeText = "Mob"; break;
            case LOOT_TABLE: modeText = "Loot"; break;
            default: modeText = "???"; break;
        }
        modeButton.displayString = modeText;
        
        // 红石按钮
        redstoneButton.displayString = tile.isRedstoneControlEnabled() ? "RS:ON" : "RS:OFF";
        
        // 生物群系按钮
        Biome biome = tile.getSelectedBiome();
        if (biome != null) {
            String biomeName = biome.getBiomeName();
            if (biomeName.length() > 6) {
                biomeName = biomeName.substring(0, 5) + "..";
            }
            biomeButton.displayString = biomeName;
        } else {
            biomeButton.displayString = "None";
        }
    }
    
    private void drawStatusInfo() {
        // 结构状态
        if (!tile.isStructureValid()) {
            fontRenderer.drawString("\u00a7cStructure Invalid!", 62, 55, 0xFFFFFF);
        }
        
        // 能量显示
        String energyText = formatEnergy(tile.getEnergyStored()) + " RF";
        int energyX = 152 - fontRenderer.getStringWidth(energyText);
        fontRenderer.drawString(energyText, energyX, 72, 0x404040);
    }
    
    private void drawTooltips(int mouseX, int mouseY) {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;
        
        // 能量条工具提示
        if (relX >= 152 && relX < 168 && relY >= 17 && relY < 69) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("Energy: " + formatEnergy(tile.getEnergyStored()) + " / " + 
                       formatEnergy(tile.getMaxEnergyStored()) + " RF");
            drawHoveringText(tooltip, relX, relY);
        }
        
        // 模式按钮工具提示
        if (modeButton.isMouseOver()) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("Current Mode: " + tile.getMode().getName());
            tooltip.add("\u00a77Click to cycle modes");
            drawHoveringText(tooltip, relX, relY);
        }
        
        // 生物群系按钮工具提示
        if (biomeButton.isMouseOver()) {
            List<String> tooltip = new ArrayList<>();
            Biome biome = tile.getSelectedBiome();
            tooltip.add("Biome: " + (biome != null ? biome.getBiomeName() : "Not Selected"));
            tooltip.add("\u00a77Click to select biome");
            drawHoveringText(tooltip, relX, relY);
        }
    }
    
    private void drawBiomeSelector(int mouseX, int mouseY) {
        int selectorX = guiLeft + xSize;
        int selectorY = guiTop + 10;
        int selectorWidth = 120;
        int selectorHeight = BIOMES_PER_PAGE * 14 + 4;
        
        // 背景
        drawRect(selectorX - 2, selectorY - 2, selectorX + selectorWidth + 2, 
                 selectorY + selectorHeight + 2, 0xFF000000);
        drawRect(selectorX, selectorY, selectorX + selectorWidth, 
                 selectorY + selectorHeight, 0xFF404040);
        
        // 生物群系列表
        for (int i = 0; i < BIOMES_PER_PAGE; i++) {
            int index = biomeScrollOffset + i;
            if (index >= biomeList.size()) break;
            
            Biome biome = biomeList.get(index);
            int entryY = selectorY + 2 + i * 14;
            
            // 高亮选中的或鼠标悬停的
            boolean isSelected = biome.equals(tile.getSelectedBiome());
            boolean isHovered = mouseX >= selectorX && mouseX < selectorX + selectorWidth &&
                               mouseY >= entryY && mouseY < entryY + 14;
            
            if (isSelected) {
                drawRect(selectorX, entryY, selectorX + selectorWidth, entryY + 14, 0xFF00AA00);
            } else if (isHovered) {
                drawRect(selectorX, entryY, selectorX + selectorWidth, entryY + 14, 0xFF606060);
            }
            
            String name = biome.getBiomeName();
            if (fontRenderer.getStringWidth(name) > selectorWidth - 4) {
                while (fontRenderer.getStringWidth(name + "...") > selectorWidth - 4 && name.length() > 0) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "...";
            }
            
            fontRenderer.drawString(name, selectorX + 2, entryY + 3, 0xFFFFFF);
        }
        
        // 滚动条
        if (biomeList.size() > BIOMES_PER_PAGE) {
            int scrollBarHeight = selectorHeight - 4;
            int thumbHeight = Math.max(10, scrollBarHeight * BIOMES_PER_PAGE / biomeList.size());
            int maxScroll = biomeList.size() - BIOMES_PER_PAGE;
            int thumbY = selectorY + 2 + (scrollBarHeight - thumbHeight) * biomeScrollOffset / maxScroll;
            
            drawRect(selectorX + selectorWidth - 4, selectorY + 2, 
                     selectorX + selectorWidth, selectorY + selectorHeight - 2, 0xFF202020);
            drawRect(selectorX + selectorWidth - 4, thumbY, 
                     selectorX + selectorWidth, thumbY + thumbHeight, 0xFF808080);
        }
    }
    
    private int getEnergyBarHeight() {
        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max == 0) return 0;
        return stored * 52 / max;
    }
    
    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.2fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}
