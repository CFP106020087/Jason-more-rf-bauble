package com.moremod.client.gui;

import com.moremod.container.ContainerPurificationAltar;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketPurificationReroll;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * 提纯祭坛 - GUI
 * 
 * 槽位布局（与Container一致）：
 * 
 * 左侧6个垂直输入槽：
 * - Slot 0: (46, 30)
 * - Slot 1: (46, 74)
 * - Slot 2: (46, 119)
 * - Slot 3: (46, 167)
 * - Slot 4: (47, 213)
 * - Slot 5: (46, 253)
 * 
 * 右侧主输出槽：
 * - Slot 6: (190, 142)
 * 
 * 玩家背包：起始(47, 318)
 * 快捷栏：起始(47, 376)
 */
public class GuiPurificationAltar extends GuiContainer {
    
    // GUI贴图路径
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation("moremod", "textures/gui/purification_altar.png");
    
    private final ContainerPurificationAltar container;
    
    // GUI尺寸
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 400;
    
    // 贴图实际尺寸（必须与png文件实际尺寸一致！）
    private static final float TEXTURE_WIDTH = 256.0F;
    private static final float TEXTURE_HEIGHT = 400.0F;
    
    // Reroll按钮
    private GuiButton rerollButton;
    private static final int BUTTON_ID_REROLL = 0;
    
    /**
     * 构造函数 - 接收Container
     */
    public GuiPurificationAltar(InventoryPlayer playerInv, ContainerPurificationAltar container) {
        super(container);
        this.container = container;
        
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        // Reroll按钮 - 放在输出槽下方
        int buttonX = guiLeft + 170;
        int buttonY = guiTop + 170;
        int buttonWidth = 60;
        int buttonHeight = 20;
        
        rerollButton = new GuiButton(BUTTON_ID_REROLL, buttonX, buttonY, 
            buttonWidth, buttonHeight, "Reroll");
        this.buttonList.add(rerollButton);
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        
        // 更新按钮状态
        if (rerollButton != null) {
            rerollButton.enabled = container.canPurify() && !container.isPurifying();
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_ID_REROLL) {
            System.out.println("[GUI] Reroll button clicked");
            System.out.println("[GUI] canPurify: " + container.canPurify());
            System.out.println("[GUI] TilePos: " + container.getTilePos());
            
            // 发送网络包到服务器
            PacketHandler.INSTANCE.sendToServer(
                new PacketPurificationReroll(container.getTilePos()));
        }
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        
        // 使用drawModalRectWithCustomSizedTexture绘制大贴图
        Gui.drawModalRectWithCustomSizedTexture(
            guiLeft, guiTop,
            0, 0,
            GUI_WIDTH, GUI_HEIGHT,
            TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
        
        // 绘制进度条（如果正在Reroll）
        if (container.isPurifying()) {
            int progress = container.getPurifyProgress();
            int maxProgress = container.getMaxPurifyTime();
            
            int progressX = guiLeft + 100;
            int progressY = guiTop + 135;
            int progressWidth = 22;
            int progressHeight = 16;
            
            int scaledProgress = (int) ((float) progress / maxProgress * progressWidth);
            drawRect(progressX, progressY, progressX + scaledProgress, progressY + progressHeight, 0xFF00AA00);
        }
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = I18n.format("tile.moremodpurification_altar.name");
        this.fontRenderer.drawString(title, 
            (xSize - fontRenderer.getStringWidth(title)) / 2, 8, 0x404040);
        
        // 显示信息 - 往左上移动
        int gemCount = container.getInputGemCount();
        
        int infoX = 90;    // 往左移
        int infoY = 80;    // 往上移
        
        if (gemCount >= 2) {
            // 显示输入宝石数量
            String countText = "Gems: " + gemCount;
            this.fontRenderer.drawString(countText, infoX, infoY, 0x404040);
            
            // 显示预测的gemLevel范围
            int minLevel = container.getPredictedQuality();
            int maxLevel = minLevel + 2;
            
            String predictText = "Output Lv: " + minLevel + " ~ " + maxLevel;
            this.fontRenderer.drawString(predictText, infoX, infoY + 12, 0x00AA00);
            
            // 显示需要的经验
            int requiredXP = container.getRequiredXP();
            String xpText = "Cost: " + requiredXP + " Lv";
            this.fontRenderer.drawString(xpText, infoX, infoY + 24, 0xAAAA00);
            
        } else if (gemCount == 1) {
            this.fontRenderer.drawString("Need 2+ gems", infoX, infoY, 0xAA0000);
        } else {
            this.fontRenderer.drawString("Insert gems", infoX, infoY, 0x808080);
        }
        
        // Reroll进度
        if (container.isPurifying()) {
            int progress = container.getPurifyProgress();
            int maxProgress = container.getMaxPurifyTime();
            int percent = (int) ((float) progress / maxProgress * 100);
            
            String progressText = "Rerolling... " + percent + "%";
            this.fontRenderer.drawString(progressText, infoX, infoY + 40, 0x5555FF);
        }
        
        // 玩家背包标题 - 白色字体
        this.fontRenderer.drawString(I18n.format("container.inventory"), 
            48, 306, 0xFFFFFF);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}