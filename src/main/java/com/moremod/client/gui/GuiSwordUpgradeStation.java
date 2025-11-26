package com.moremod.client.gui;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.container.ContainerSwordUpgradeStation;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketStarUpgrade;
import com.moremod.network.PacketRemoveAllGems;
import com.moremod.network.PacketRemoveSingleGem;
import com.moremod.tile.TileEntitySwordUpgradeStation;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 剑升级站 GUI - 宝石系统（新版）
 * 
 * 核心改动：
 * - 显示宝石词条信息（而不是材料ID）
 * - 显示宝石等级和品质
 * - 完整的词条Tooltip
 */
public class GuiSwordUpgradeStation extends GuiContainer {

    // ==================== GUI资源 ====================
    
    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/sword_upgrade_station.png");

    // ==================== 坐标常量 ====================
    
    public static final int ARROW_X0 = 33;
    public static final int ARROW_Y0 = 15;
    public static final int ARROW_X1 = 53;
    public static final int ARROW_Y1 = 29;
    
    public static final int MAT_GRID_X = ContainerSwordUpgradeStation.MATERIAL_GRID_X;
    public static final int MAT_GRID_Y = ContainerSwordUpgradeStation.MATERIAL_GRID_Y;
    public static final int MAT_SLOT_SIZE = 18;
    
    public static final int TITLE_Y = 6;
    public static final int EXP_DISPLAY_X = 8;
    public static final int EXP_DISPLAY_Y = 6;
    public static final int HINT_X = 8;
    public static final int HINT_START_Y = 20;
    public static final int HINT_LINE_HEIGHT = 10;

    // ==================== 实例变量 ====================
    
    private final TileEntitySwordUpgradeStation tile;
    private final ContainerSwordUpgradeStation container;

    // 宝石槽点击区域
    private static class MaterialSlotArea {
        int x, y, size, slotIndex;

        MaterialSlotArea(int x, int y, int size, int slotIndex) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.slotIndex = slotIndex;
        }

        boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + size &&
                    mouseY >= y && mouseY <= y + size;
        }
    }

    private List<MaterialSlotArea> materialSlotAreas = new ArrayList<>();

    // ==================== 构造函数 ====================
    
    public GuiSwordUpgradeStation(InventoryPlayer playerInv, TileEntitySwordUpgradeStation tile) {
        super(new ContainerSwordUpgradeStation(playerInv, tile));
        this.tile = tile;
        this.container = (ContainerSwordUpgradeStation) this.inventorySlots;
        this.xSize = ContainerSwordUpgradeStation.GUI_WIDTH;
        this.ySize = ContainerSwordUpgradeStation.GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        updateMaterialSlotAreas();
    }

    /**
     * 更新宝石槽点击区域
     */
    private void updateMaterialSlotAreas() {
        materialSlotAreas.clear();

        int[] slotIndices = {
            ContainerSwordUpgradeStation.SLOT_MAT0,
            ContainerSwordUpgradeStation.SLOT_MAT1,
            ContainerSwordUpgradeStation.SLOT_MAT2,
            ContainerSwordUpgradeStation.SLOT_MAT3,
            ContainerSwordUpgradeStation.SLOT_MAT4,
            ContainerSwordUpgradeStation.SLOT_MAT5
        };

        for (int i = 0; i < slotIndices.length; i++) {
            int row = i / 2;
            int col = i % 2;
            
            int x = guiLeft + MAT_GRID_X + col * MAT_SLOT_SIZE;
            int y = guiTop + MAT_GRID_Y + row * MAT_SLOT_SIZE;
            
            materialSlotAreas.add(new MaterialSlotArea(x, y, MAT_SLOT_SIZE, slotIndices[i]));
        }
    }

    // ==================== 渲染 ====================
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        drawArrowTooltip(mouseX, mouseY);
        drawMaterialSlotTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        
        drawMaterialSlotHighlights(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        TileEntitySwordUpgradeStation.Mode mode = container.getCurrentMode();
        
        // 箭头悬停高亮
        if (isInArrow(mouseX, mouseY)) {
            int color;
            if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE && container.canPerformStarUpgrade()) {
                color = 0x6000FF00; // 绿色
            } else if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL && container.canPerformRemoveAll()) {
                color = 0x60FFAA00; // 橙色
            } else {
                color = 0x60FF0000; // 红色
            }
            drawRect(ARROW_X0, ARROW_Y0, ARROW_X1, ARROW_Y1, color);
        }

        // 标题
        String title = getTitleByMode(mode);
        this.fontRenderer.drawString(title, 
            this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2, 
            TITLE_Y, 4210752);

        // 经验显示
        String expText = "经验: " + mc.player.experienceLevel + " 级";
        this.fontRenderer.drawString(expText, 
            EXP_DISPLAY_X,
            EXP_DISPLAY_Y, 4210752);
    }

    private String getTitleByMode(TileEntitySwordUpgradeStation.Mode mode) {
        switch (mode) {
            case UPGRADE:
                return "§e镶嵌台 §7- §a升级模式";
            case REMOVAL:
                return "§e镶嵌台 §7- §6拆除模式";
            default:
                return "§7镶嵌台";
        }
    }

    /**
     * 绘制宝石槽高亮
     */
    private void drawMaterialSlotHighlights(int mouseX, int mouseY) {
        TileEntitySwordUpgradeStation.Mode mode = container.getCurrentMode();
        
        if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
            // 拆除模式：显示可拆除的宝石
            List<TileEntitySwordUpgradeStation.InlayInfo> inlays = container.getInlayList();

            for (MaterialSlotArea area : materialSlotAreas) {
                int inlayIndex = area.slotIndex - ContainerSwordUpgradeStation.MATERIAL_SLOT_START;
                
                if (inlayIndex < inlays.size()) {
                    int x = area.x;
                    int y = area.y;
                    
                    // ✅ 修改：不需要检查经验（暂时移除经验消耗）
                    int color = area.isMouseOver(mouseX, mouseY) ? 0x8000FF00 : 0x4000FF00;
                    
                    drawRect(x, y, x + area.size, y + area.size, color);
                }
            }
        } else if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE) {
            // 升级模式：已有镶嵌显示绿色
            for (MaterialSlotArea area : materialSlotAreas) {
                ItemStack stackInSlot = tile.getStackInSlot(area.slotIndex);
                
                if (!stackInSlot.isEmpty() && 
                    stackInSlot.hasTagCompound() && 
                    stackInSlot.getTagCompound().getBoolean("Preview") &&
                    stackInSlot.getTagCompound().getBoolean("ExistingInlay")) {
                    
                    int x = area.x;
                    int y = area.y;
                    
                    int color = area.isMouseOver(mouseX, mouseY) ? 0x8000FF00 : 0x4000FF00;
                    drawRect(x, y, x + area.size, y + area.size, color);
                }
            }
        }
    }

    // ==================== 鼠标交互 ====================
    
    private boolean isInArrow(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= ARROW_X0 && x <= ARROW_X1 && y >= ARROW_Y0 && y <= ARROW_Y1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && isInArrow(mouseX, mouseY)) {
            onArrowClicked();
        }

        if (mouseButton == 1 && container.getCurrentMode() == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
            onMaterialSlotRightClicked(mouseX, mouseY);
        }
    }

    protected void onArrowClicked() {
        TileEntitySwordUpgradeStation.Mode mode = container.getCurrentMode();
        
        if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE && container.canPerformStarUpgrade()) {
            PacketHandler.INSTANCE.sendToServer(new PacketStarUpgrade(tile.getPos()));
            mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        } else if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL && container.canPerformRemoveAll()) {
            PacketHandler.INSTANCE.sendToServer(new PacketRemoveAllGems(tile.getPos()));
            mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        } else {
            mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
        }
    }

    protected void onMaterialSlotRightClicked(int mouseX, int mouseY) {
        List<TileEntitySwordUpgradeStation.InlayInfo> inlays = container.getInlayList();

        for (MaterialSlotArea area : materialSlotAreas) {
            if (area.isMouseOver(mouseX, mouseY)) {
                int inlayIndex = area.slotIndex - ContainerSwordUpgradeStation.MATERIAL_SLOT_START;
                
                if (inlayIndex < inlays.size()) {
                    // ✅ 修改：不检查经验，直接拆除
                    PacketHandler.INSTANCE.sendToServer(
                        new PacketRemoveSingleGem(tile.getPos(), area.slotIndex));
                    mc.player.playSound(net.minecraft.init.SoundEvents.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                }
                break;
            }
        }
    }

    // ==================== 工具提示 ====================
    
    /**
     * 绘制箭头工具提示
     */
    private void drawArrowTooltip(int mouseX, int mouseY) {
        if (!isInArrow(mouseX, mouseY)) return;

        TileEntitySwordUpgradeStation.Mode mode = container.getCurrentMode();
        List<String> tooltip = new ArrayList<>();
        
        if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE) {
            drawUpgradeArrowTooltip(tooltip);
        } else if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
            drawRemovalArrowTooltip(tooltip);
        } else {
            tooltip.add("§c§l⬆ 统一操作");
            tooltip.add("§7" + repeat("━", 15));
            tooltip.add("§7请先放入剑和宝石");
        }

        if (!tooltip.isEmpty()) {
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    private void drawUpgradeArrowTooltip(List<String> tooltip) {
        if (container.canPerformStarUpgrade()) {
            tooltip.add("§e§l⬆ 统一升级");
            tooltip.add("§7" + repeat("━", 15));
            tooltip.add("§7点击立即镶嵌宝石");
            tooltip.add("§7使用所有宝石槽的材料");
            tooltip.add("");
            tooltip.add("§a§l✔ 点击执行升级");
        } else {
            tooltip.add("§c§l⬆ 统一升级");
            tooltip.add("§7" + repeat("━", 15));
            tooltip.add("§c需要放入剑和至少一个宝石");
            
            ItemStack sword = tile.getStackInSlot(ContainerSwordUpgradeStation.SLOT_SWORD);
            if (sword.isEmpty()) {
                tooltip.add("§7• §c缺少剑");
            }
            
            boolean hasMaterial = false;
            for (int i = ContainerSwordUpgradeStation.MATERIAL_SLOT_START; 
                     i <= ContainerSwordUpgradeStation.MATERIAL_SLOT_END; i++) {
                ItemStack mat = tile.getStackInSlot(i);
                if (!mat.isEmpty() && GemNBTHelper.isGem(mat) && GemNBTHelper.isIdentified(mat)) {
                    hasMaterial = true;
                    break;
                }
            }
            if (!hasMaterial) {
                tooltip.add("§7• §c缺少已鉴定宝石");
            }
        }
    }

    /**
     * ✅ 修改：拆除模式工具提示（新宝石系统）
     */
    private void drawRemovalArrowTooltip(List<String> tooltip) {
        List<TileEntitySwordUpgradeStation.InlayInfo> inlays = container.getInlayList();
        
        if (!inlays.isEmpty()) {
            tooltip.add("§6§l⬆ 拆除所有宝石");
            tooltip.add("§7" + repeat("━", 15));
            tooltip.add("§7拆除 §e" + inlays.size() + " §7个宝石");
            tooltip.add("");
            tooltip.add("§a§l✔ 点击拆除全部");
            tooltip.add("§7宝石将直接到背包");
        } else {
            tooltip.add("§c§l⬆ 拆除所有宝石");
            tooltip.add("§7" + repeat("━", 15));
            tooltip.add("§7这把剑没有镶嵌宝石");
        }
    }

    /**
     * ✅ 核心改动：绘制宝石槽工具提示（显示词条信息）
     */
    private void drawMaterialSlotTooltips(int mouseX, int mouseY) {
        TileEntitySwordUpgradeStation.Mode mode = container.getCurrentMode();
        
        if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
            // 拆除模式：显示宝石词条
            List<TileEntitySwordUpgradeStation.InlayInfo> inlays = container.getInlayList();

            for (MaterialSlotArea area : materialSlotAreas) {
                if (area.isMouseOver(mouseX, mouseY)) {
                    int inlayIndex = area.slotIndex - ContainerSwordUpgradeStation.MATERIAL_SLOT_START;
                    
                    if (inlayIndex < inlays.size()) {
                        TileEntitySwordUpgradeStation.InlayInfo info = inlays.get(inlayIndex);
                        List<String> tooltip = buildGemTooltip(info, inlayIndex, true);
                        drawHoveringText(tooltip, mouseX, mouseY);
                    }
                    break;
                }
            }
        } else if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE) {
            // 升级模式：显示已有镶嵌的宝石
            for (MaterialSlotArea area : materialSlotAreas) {
                if (area.isMouseOver(mouseX, mouseY)) {
                    ItemStack stackInSlot = tile.getStackInSlot(area.slotIndex);
                    
                    if (!stackInSlot.isEmpty() && 
                        stackInSlot.hasTagCompound() && 
                        stackInSlot.getTagCompound().getBoolean("Preview") &&
                        stackInSlot.getTagCompound().getBoolean("ExistingInlay")) {
                        
                        int inlayIndex = area.slotIndex - ContainerSwordUpgradeStation.MATERIAL_SLOT_START;
                        List<String> tooltip = buildExistingGemTooltip(stackInSlot, inlayIndex);
                        drawHoveringText(tooltip, mouseX, mouseY);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 构建宝石工具提示（拆除模式）
     */
    private List<String> buildGemTooltip(TileEntitySwordUpgradeStation.InlayInfo info, int inlayIndex, boolean canRemove) {
        List<String> tooltip = new ArrayList<>();
        
        ItemStack gem = info.gem;
        
        // 标题
        tooltip.add("§6§l已镶嵌槽位 " + (inlayIndex + 1));
        tooltip.add("§7" + repeat("━", 15));
        
        // 宝石名称
        tooltip.add("§f" + gem.getDisplayName());
        
        // 宝石等级
        int gemLevel = GemNBTHelper.getGemLevel(gem);
        tooltip.add("§7等级: §e" + gemLevel);
        
        // 词条列表
        if (!info.affixes.isEmpty()) {
            tooltip.add("");
            tooltip.add("§6词条效果:");
            for (IdentifiedAffix affix : info.affixes) {
                String affixLine = formatAffixLine(affix);
                tooltip.add("  " + affixLine);
            }
        }
        
        if (canRemove) {
            tooltip.add("");
            tooltip.add("§a§l✔ 右键点击拆除");
            tooltip.add("§7宝石将直接到背包");
        }
        
        return tooltip;
    }

    /**
     * 构建已有镶嵌的宝石工具提示（升级模式）
     */
    private List<String> buildExistingGemTooltip(ItemStack gem, int inlayIndex) {
        List<String> tooltip = new ArrayList<>();
        
        tooltip.add("§a§l已镶嵌槽位 " + (inlayIndex + 1));
        tooltip.add("§7" + repeat("━", 15));
        tooltip.add("§f" + gem.getDisplayName());
        
        int gemLevel = GemNBTHelper.getGemLevel(gem);
        tooltip.add("§7等级: §e" + gemLevel);
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        if (!affixes.isEmpty()) {
            tooltip.add("");
            tooltip.add("§6词条效果:");
            for (IdentifiedAffix affix : affixes) {
                String affixLine = formatAffixLine(affix);
                tooltip.add("  " + affixLine);
            }
        }
        
        tooltip.add("");
        tooltip.add("§7此宝石已镶嵌在剑上");
        tooltip.add("§7不能再次放入或移除");
        tooltip.add("");
        tooltip.add("§e提示：在拆除模式下可以拆除");
        
        return tooltip;
    }

    /**
     * 格式化词条显示
     */
    private String formatAffixLine(IdentifiedAffix affix) {
        String displayName = affix.getDisplayName();
        int quality = affix.getQuality();
        
        // 品质颜色
        String qualityColor;
        if (quality >= 90) {
            qualityColor = "§d"; // 传说（粉色）
        } else if (quality >= 75) {
            qualityColor = "§6"; // 史诗（金色）
        } else if (quality >= 60) {
            qualityColor = "§5"; // 稀有（紫色）
        } else if (quality >= 40) {
            qualityColor = "§9"; // 优秀（蓝色）
        } else if (quality >= 20) {
            qualityColor = "§a"; // 良好（绿色）
        } else {
            qualityColor = "§7"; // 普通（灰色）
        }
        
        return qualityColor + displayName + " §7(" + quality + "%)";
    }

    // ==================== 工具方法 ====================
    
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}