package com.moremod.client.gui;

import com.moremod.container.ContainerSwordUpgradeStationMaterial;
import com.moremod.recipe.SwordUpgradeRegistry;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiSwordUpgradeStationMaterial extends GuiContainer {

    private final TileEntitySwordUpgradeStationMaterial tile;
    private final InventoryPlayer playerInv;

    public GuiSwordUpgradeStationMaterial(InventoryPlayer playerInv, TileEntitySwordUpgradeStationMaterial tile) {
        super(new ContainerSwordUpgradeStationMaterial(playerInv, tile));
        this.tile = tile;
        this.playerInv = playerInv;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        this.renderSlotTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(guiX, guiY, 0);
        
        // 绘制主背景
        drawMainBackground();
        
        // 绘制标题区域
        drawTitleArea();
        
        // 绘制工作区域
        drawWorkArea();
        
        // 绘制三个主要槽位
        drawMainSlots();
        
        // 绘制升级箭头
        drawUpgradeArrow();
        
        // 绘制背包区域
        drawInventoryArea();
        
        GlStateManager.popMatrix();
    }

    /**
     * 绘制主背景 - 深色面板带渐变效果
     */
    private void drawMainBackground() {
        // 外层阴影效果
        drawGradientRect(-2, -2, this.xSize + 2, this.ySize + 2, 
            0x80000000, 0x80000000);
        
        // 主背景 - 深灰色渐变
        drawGradientRect(0, 0, this.xSize, this.ySize, 
            0xFF2B2B2B, 0xFF1E1E1E);
        
        // 内层高光边框
        drawVerticalLine(1, 1, this.ySize - 2, 0xFF404040);
        drawHorizontalLine(1, this.xSize - 2, 1, 0xFF404040);
        
        // 外层边框
        drawBorder(0, 0, this.xSize, this.ySize, 0xFF555555);
    }

    /**
     * 绘制标题区域
     */
    private void drawTitleArea() {
        // 标题背景 - 深色带渐变
        drawGradientRect(4, 4, this.xSize - 4, 20, 
            0xFF383838, 0xFF2A2A2A);
        
        // 标题装饰线
        drawRect(4, 19, this.xSize - 4, 20, 0xFF505050);
        
        // 左侧装饰
        drawRect(6, 6, 8, 18, 0xFFFFD700); // 金色竖条
        
        // 右侧装饰
        drawRect(this.xSize - 8, 6, this.xSize - 6, 18, 0xFFFFD700);
    }

    /**
     * 绘制工作区域
     */
    private void drawWorkArea() {
        // 工作区背景
        drawGradientRect(4, 22, this.xSize - 4, 68, 
            0xFF353535, 0xFF2C2C2C);
        
        // 顶部装饰线
        drawRect(4, 22, this.xSize - 4, 23, 0xFF505050);
        
        // 底部阴影
        drawRect(4, 67, this.xSize - 4, 68, 0xFF1A1A1A);
    }

    /**
     * 绘制三个主要槽位 - 漂亮的3D效果
     */
    private void drawMainSlots() {
        int[] slotX = {43, 79, 133};
        int[] colors = {0xFFFF6B6B, 0xFF4ECDC4, 0xFF95E77D}; // 红、青、绿
        
        for (int i = 0; i < 3; i++) {
            drawFancySlot(slotX[i], 34, colors[i], i == 2); // 最后一个是输出槽
        }
    }

    /**
     * 绘制精美的槽位 - 3D凹陷效果
     */
    private void drawFancySlot(int x, int y, int accentColor, boolean isOutput) {
        int size = 18;
        
        // 外层发光效果（输出槽特殊处理）
        if (isOutput) {
            ItemStack out = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_OUT);
            if (!out.isEmpty()) {
                // 发光动画
                float pulse = (float)(Math.sin(System.currentTimeMillis() / 300.0) * 0.3 + 0.7);
                int glowAlpha = (int)(pulse * 80);
                int glowColor = (glowAlpha << 24) | (accentColor & 0xFFFFFF);
                drawRect(x - 2, y - 2, x + size + 2, y + size + 2, glowColor);
            }
        }
        
        // 主槽位背景 - 深色凹陷
        drawGradientRect(x, y, x + size, y + size, 
            0xFF1A1A1A, 0xFF0F0F0F);
        
        // 内层阴影（顶部和左侧）
        drawRect(x + 1, y + 1, x + size - 1, y + 2, 0xFF0A0A0A);
        drawRect(x + 1, y + 1, x + 2, y + size - 1, 0xFF0A0A0A);
        
        // 内层高光（底部和右侧）
        drawRect(x + 1, y + size - 2, x + size - 1, y + size - 1, 0xFF2A2A2A);
        drawRect(x + size - 2, y + 1, x + size - 1, y + size - 1, 0xFF2A2A2A);
        
        // 外边框 - 使用强调色
        int borderColor = isOutput ? (accentColor | 0xFF000000) : 0xFF505050;
        drawBorder(x - 1, y - 1, size + 2, size + 2, borderColor);
        
        // 角落装饰（小点）
        int cornerColor = (accentColor & 0xFFFFFF) | 0x80000000;
        drawRect(x - 1, y - 1, x, y, cornerColor);
        drawRect(x + size, y - 1, x + size + 1, y, cornerColor);
        drawRect(x - 1, y + size, x, y + size + 1, cornerColor);
        drawRect(x + size, y + size, x + size + 1, y + size + 1, cornerColor);
    }

    /**
     * 绘制升级箭头 - 动态效果
     */
    private void drawUpgradeArrow() {
        ItemStack base = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_BASE);
        ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);
        ItemStack out = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_OUT);

        int x = 102;
        int y = 38;
        
        boolean active = !base.isEmpty() && !mat.isEmpty() && !out.isEmpty();
        
        if (active) {
            // 激活状态 - 绿色带动画
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.2 + 0.8);
            int alpha = (int)(pulse * 255);
            int arrowColor = (alpha << 24) | 0x00FF88;
            
            // 发光背景
            drawGradientRect(x - 2, y - 2, x + 26, y + 11, 
                (alpha / 3 << 24) | 0x00FF88, 0x00000000);
            
            drawArrow(x, y, arrowColor, true);
        } else {
            // 未激活状态 - 灰色
            drawArrow(x, y, 0xFF404040, false);
        }
    }

    /**
     * 绘制箭头形状
     */
    private void drawArrow(int x, int y, int color, boolean glow) {
        // 箭头主体（矩形部分）
        drawRect(x, y + 2, x + 14, y + 7, color);
        
        // 箭头尖端（三角形）
        for (int i = 0; i < 5; i++) {
            drawRect(x + 14 + i, y + 2 + (2 - i), x + 15 + i, y + 7 - (2 - i), color);
        }
        
        if (glow) {
            // 添加内部高光
            int highlightColor = 0xFFFFFFFF;
            drawRect(x + 1, y + 3, x + 13, y + 4, highlightColor);
        }
        
        // 箭头边框
        int borderColor = glow ? 0xFF00DD66 : 0xFF303030;
        
        // 上边框
        drawHorizontalLine(x, x + 13, y + 1, borderColor);
        // 下边框
        drawHorizontalLine(x, x + 13, y + 7, borderColor);
        // 左边框
        drawVerticalLine(x - 1, y + 2, y + 6, borderColor);
        
        // 箭头尖端边框
        for (int i = 0; i < 5; i++) {
            // 上边
            if (i == 0 || 2 - i >= 0) {
                drawRect(x + 14 + i, y + 1 + (2 - i), x + 15 + i, y + 2 + (2 - i), borderColor);
            }
            // 下边
            if (i == 0 || 2 - i >= 0) {
                drawRect(x + 14 + i, y + 7 - (2 - i), x + 15 + i, y + 8 - (2 - i), borderColor);
            }
        }
        // 尖端
        drawRect(x + 19, y + 4, x + 20, y + 5, borderColor);
    }

    /**
     * 绘制背包区域
     */
    private void drawInventoryArea() {
        // 背包标题栏
        drawGradientRect(4, 70, this.xSize - 4, 82, 
            0xFF383838, 0xFF2A2A2A);
        drawRect(4, 81, this.xSize - 4, 82, 0xFF505050);
        
        // 背包槽位区域背景
        drawGradientRect(4, 82, this.xSize - 4, 138, 
            0xFF2C2C2C, 0xFF252525);
        
        // 快捷栏区域背景
        drawGradientRect(4, 138, this.xSize - 4, this.ySize - 4, 
            0xFF323232, 0xFF2A2A2A);
        
        // 底部装饰线
        drawRect(4, this.ySize - 5, this.xSize - 4, this.ySize - 4, 0xFF555555);
        
        // 绘制所有背包槽位
        drawInventorySlots();
    }

    /**
     * 绘制背包槽位
     */
    private void drawInventorySlots() {
        // 主背包 3x9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 7 + col * 18;
                int y = 83 + row * 18;
                drawSimpleSlot(x, y, false);
            }
        }
        
        // 快捷栏 1x9
        for (int col = 0; col < 9; col++) {
            int x = 7 + col * 18;
            int y = 141;
            drawSimpleSlot(x, y, true);
        }
    }

    /**
     * 绘制简单槽位
     */
    private void drawSimpleSlot(int x, int y, boolean hotbar) {
        // 槽位背景
        drawRect(x, y, x + 18, y + 18, 0xFF1A1A1A);
        
        // 内部阴影
        drawRect(x + 1, y + 1, x + 17, y + 2, 0xFF0F0F0F);
        drawRect(x + 1, y + 1, x + 2, y + 17, 0xFF0F0F0F);
        
        // 高光
        drawRect(x + 1, y + 16, x + 17, y + 17, 0xFF252525);
        drawRect(x + 16, y + 1, x + 17, y + 17, 0xFF252525);
        
        // 边框（快捷栏使用金色）
        int borderColor = hotbar ? 0xFFFFAA00 : 0xFF3F3F3F;
        drawBorder(x, y, 18, 18, borderColor);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GlStateManager.disableLighting();
        
        // 标题
        String title = I18n.format("container.sword_upgrade_station_material");
        int titleWidth = this.fontRenderer.getStringWidth(title);
        this.fontRenderer.drawString(title, (this.xSize - titleWidth) / 2, 8, 0xFFFFD700);
        
        // 背包标题
        String invTitle = I18n.format("container.inventory");
        this.fontRenderer.drawString(invTitle, 8, 72, 0xFFCCCCCC);
        
        // 槽位标签
        drawSlotLabels();
        
        // 经验消耗
        drawExperienceCost();
        
        GlStateManager.enableLighting();
    }

    /**
     * 绘制槽位标签
     */
    private void drawSlotLabels() {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.65F, 0.65F, 1.0F);
        
        // 基础剑
        String baseLabel = I18n.format("gui.sword_upgrade.base_sword");
        int baseWidth = this.fontRenderer.getStringWidth(baseLabel);
        this.fontRenderer.drawString(baseLabel, 
            (int)((53 - baseWidth * 0.65F / 2) / 0.65F), 
            (int)(26 / 0.65F), 
            0xFFFF6B6B);
        
        // 材料
        String matLabel = I18n.format("gui.sword_upgrade.material");
        int matWidth = this.fontRenderer.getStringWidth(matLabel);
        this.fontRenderer.drawString(matLabel, 
            (int)((89 - matWidth * 0.65F / 2) / 0.65F), 
            (int)(26 / 0.65F), 
            0xFF4ECDC4);
        
        // 结果
        String outLabel = I18n.format("gui.sword_upgrade.result");
        int outWidth = this.fontRenderer.getStringWidth(outLabel);
        this.fontRenderer.drawString(outLabel, 
            (int)((143 - outWidth * 0.65F / 2) / 0.65F), 
            (int)(26 / 0.65F), 
            0xFF95E77D);
        
        GlStateManager.popMatrix();
    }

    /**
     * 显示经验消耗
     */
    private void drawExperienceCost() {
        ItemStack base = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_BASE);
        ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);

        if (base.isEmpty() || mat.isEmpty()) return;

        SwordUpgradeRegistry.Recipe recipe = SwordUpgradeRegistry.getRecipe(base, mat.getItem());
        if (recipe == null) return;

        int xpCost = recipe.xpCost;
        int playerXp = getPlayerTotalXp(this.mc.player);
        
        int x = 143;
        int y = 56;
        
        String text;
        int color;
        
        if (xpCost == 0) {
            text = I18n.format("gui.sword_upgrade.free");
            color = 0xFF00FF88;
        } else {
            text = xpCost + " " + I18n.format("gui.sword_upgrade.xp");
            color = playerXp >= xpCost ? 0xFF00FF88 : 0xFFFF4444;
        }
        
        // 绘制经验消耗（居中）
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.8F, 0.8F, 1.0F);
        int textWidth = this.fontRenderer.getStringWidth(text);
        this.fontRenderer.drawStringWithShadow(text, 
            (int)((x - textWidth * 0.8F / 2) / 0.8F), 
            (int)(y / 0.8F), 
            color);
        GlStateManager.popMatrix();
        
        // 当前经验（更小字体）
        if (xpCost > 0) {
            String currentXp = playerXp + "";
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.6F, 0.6F, 1.0F);
            int currentWidth = this.fontRenderer.getStringWidth(currentXp);
            this.fontRenderer.drawString(currentXp, 
                (int)((x - currentWidth * 0.6F / 2) / 0.6F), 
                (int)((y + 8) / 0.6F), 
                0xFF999999);
            GlStateManager.popMatrix();
        }
    }

    /**
     * 渲染槽位提示
     */
    private void renderSlotTooltips(int mouseX, int mouseY) {
        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;

        // 输出槽提示
        if (this.isPointInRegion(133, 34, 18, 18, mouseX, mouseY)) {
            ItemStack out = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_OUT);
            ItemStack base = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_BASE);
            ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);

            if (!out.isEmpty() && !base.isEmpty() && !mat.isEmpty()) {
                SwordUpgradeRegistry.Recipe recipe = SwordUpgradeRegistry.getRecipe(base, mat.getItem());
                if (recipe != null) {
                    int playerXp = getPlayerTotalXp(this.mc.player);
                    
                    if (playerXp < recipe.xpCost) {
                        java.util.List<String> tooltip = new java.util.ArrayList<>();
                        tooltip.add("§c" + I18n.format("gui.sword_upgrade.insufficient_xp"));
                        tooltip.add("§7" + I18n.format("gui.sword_upgrade.need") + ": §e" + recipe.xpCost);
                        tooltip.add("§7" + I18n.format("gui.sword_upgrade.have") + ": §7" + playerXp);
                        this.drawHoveringText(tooltip, mouseX, mouseY);
                    } else {
                        java.util.List<String> tooltip = new java.util.ArrayList<>();
                        tooltip.add("§a" + I18n.format("gui.sword_upgrade.ready"));
                        tooltip.add("§7" + I18n.format("gui.sword_upgrade.click_to_upgrade"));
                        this.drawHoveringText(tooltip, mouseX, mouseY);
                    }
                }
            }
        }

        // 基础剑槽提示
        if (this.isPointInRegion(43, 34, 18, 18, mouseX, mouseY)) {
            ItemStack base = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_BASE);
            if (base.isEmpty()) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                tooltip.add("§7" + I18n.format("gui.sword_upgrade.place_sword_here"));
                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }

        // 材料槽提示
        if (this.isPointInRegion(79, 34, 18, 18, mouseX, mouseY)) {
            ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);
            if (mat.isEmpty()) {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                tooltip.add("§7" + I18n.format("gui.sword_upgrade.place_material_here"));
                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 绘制边框
     */
    private void drawBorder(int x, int y, int width, int height, int color) {
        drawHorizontalLine(x, x + width - 1, y, color);
        drawHorizontalLine(x, x + width - 1, y + height - 1, color);
        drawVerticalLine(x, y, y + height - 1, color);
        drawVerticalLine(x + width - 1, y, y + height - 1, color);
    }

    /**
     * 计算玩家总经验
     */
    private int getPlayerTotalXp(net.minecraft.entity.player.EntityPlayer player) {
        int level = player.experienceLevel;
        int total = getExperienceForLevel(level);
        total += Math.round(player.experience * player.xpBarCap());
        return total;
    }

    /**
     * 计算等级经验
     */
    private int getExperienceForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        else if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        else return (int) (4.5 * level * level - 162.5 * level + 2220);
    }
}