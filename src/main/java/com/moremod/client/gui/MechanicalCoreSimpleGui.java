package com.moremod.client.gui;

import com.moremod.viewmodel.MechCoreViewModel;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

/**
 * 机械核心简化 GUI - 使用 ViewModel
 *
 * 这是一个示例 GUI，展示如何使用 MechCoreViewModel
 * 替代直接访问 NBT 数据
 *
 * 优势：
 *  - 业务逻辑与 UI 分离
 *  - 数据格式化统一
 *  - 易于测试和维护
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreSimpleGui extends GuiScreen {

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/mechanical_core_gui.png");

    private final MechCoreViewModel viewModel;
    private int guiLeft;
    private int guiTop;

    private int scrollOffset = 0;
    private static final int MODULES_PER_PAGE = 8;

    public MechanicalCoreSimpleGui(EntityPlayer player) {
        this.viewModel = new MechCoreViewModel(player);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.buttonList.clear();

        // 关闭按钮
        this.buttonList.add(new GuiButton(
                0,
                guiLeft + GUI_WIDTH - 60,
                guiTop + 5,
                50, 20,
                "关闭"
        ));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制背景
        this.drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        // 绘制标题
        String title = TextFormatting.BOLD + "机械核心控制面板";
        this.drawCenteredString(
                this.fontRenderer,
                title,
                guiLeft + GUI_WIDTH / 2,
                guiTop + 10,
                0xFFFFFF
        );

        // 绘制能量信息
        drawEnergyInfo();

        // 绘制模块列表
        drawModuleList();

        // 绘制按钮
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制工具提示
        drawTooltips(mouseX, mouseY);
    }

    /**
     * 绘制能量信息
     */
    private void drawEnergyInfo() {
        int x = guiLeft + 10;
        int y = guiTop + 30;

        // 使用 ViewModel 获取格式化的能量文本
        String energyText = viewModel.getEnergyText();
        String percentageText = viewModel.getEnergyPercentageText();
        TextFormatting color = viewModel.getEnergyColor();

        // 标签
        this.fontRenderer.drawString(
                "能量:",
                x, y,
                0xFFFFFF
        );

        // 能量值（带颜色）
        this.fontRenderer.drawString(
                color + energyText,
                x + 40, y,
                0xFFFFFF
        );

        // 百分比
        this.fontRenderer.drawString(
                color + "(" + percentageText + ")",
                x + 150, y,
                0xFFFFFF
        );

        // 绘制能量条
        drawEnergyBar(x, y + 15);
    }

    /**
     * 绘制能量条
     */
    private void drawEnergyBar(int x, int y) {
        int barWidth = 200;
        int barHeight = 10;

        // 背景
        drawRect(x, y, x + barWidth, y + barHeight, 0xFF333333);

        // 能量条
        float percentage = viewModel.getEnergyPercentage();
        int fillWidth = (int) (barWidth * percentage);

        // 根据百分比选择颜色
        int fillColor;
        if (percentage >= 0.7f) {
            fillColor = 0xFF00FF00; // 绿色
        } else if (percentage >= 0.3f) {
            fillColor = 0xFFFFFF00; // 黄色
        } else if (percentage >= 0.1f) {
            fillColor = 0xFFFF0000; // 红色
        } else {
            fillColor = 0xFF880000; // 深红色
        }

        drawRect(x, y, x + fillWidth, y + barHeight, fillColor);

        // 边框
        drawRect(x, y, x + barWidth, y + 1, 0xFFFFFFFF);
        drawRect(x, y + barHeight - 1, x + barWidth, y + barHeight, 0xFFFFFFFF);
        drawRect(x, y, x + 1, y + barHeight, 0xFFFFFFFF);
        drawRect(x + barWidth - 1, y, x + barWidth, y + barHeight, 0xFFFFFFFF);
    }

    /**
     * 绘制模块列表
     */
    private void drawModuleList() {
        int x = guiLeft + 10;
        int y = guiTop + 70;

        // 标题
        this.fontRenderer.drawString(
                TextFormatting.BOLD + "模块列表:",
                x, y,
                0xFFFFFF
        );

        y += 15;

        // 获取所有模块
        List<MechCoreViewModel.ModuleInfo> modules = viewModel.getAllModules();

        // 分页
        int startIndex = scrollOffset * MODULES_PER_PAGE;
        int endIndex = Math.min(startIndex + MODULES_PER_PAGE, modules.size());

        for (int i = startIndex; i < endIndex; i++) {
            MechCoreViewModel.ModuleInfo module = modules.get(i);

            // 模块名称（带颜色）
            String moduleName = module.getDisplayName();
            TextFormatting moduleColor = module.getColor();

            this.fontRenderer.drawString(
                    moduleColor + moduleName,
                    x, y,
                    0xFFFFFF
            );

            // 等级信息
            String levelText = module.getLevelText();
            this.fontRenderer.drawString(
                    TextFormatting.GRAY + levelText,
                    x + 120, y,
                    0xFFFFFF
            );

            // 状态信息
            String statusText = module.getStatusText();
            TextFormatting statusColor = module.getStatusColor();
            this.fontRenderer.drawString(
                    statusColor + statusText,
                    x + 180, y,
                    0xFFFFFF
            );

            y += 12;
        }

        // 显示页码
        if (modules.size() > MODULES_PER_PAGE) {
            int totalPages = (modules.size() + MODULES_PER_PAGE - 1) / MODULES_PER_PAGE;
            String pageInfo = String.format("第 %d/%d 页", scrollOffset + 1, totalPages);
            this.fontRenderer.drawString(
                    TextFormatting.GRAY + pageInfo,
                    x + 100, y + 10,
                    0xFFFFFF
            );
        }
    }

    /**
     * 绘制工具提示
     */
    private void drawTooltips(int mouseX, int mouseY) {
        // 能量条悬停提示
        int barX = guiLeft + 10;
        int barY = guiTop + 45;
        int barWidth = 200;
        int barHeight = 10;

        if (mouseX >= barX && mouseX <= barX + barWidth &&
            mouseY >= barY && mouseY <= barY + barHeight) {

            String tooltip = viewModel.getEnergyText() +
                           " (" + viewModel.getEnergyPercentageText() + ")";

            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // 关闭按钮
            this.mc.player.closeScreen();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 处理滚轮滚动
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            List<MechCoreViewModel.ModuleInfo> modules = viewModel.getAllModules();
            int totalPages = (modules.size() + MODULES_PER_PAGE - 1) / MODULES_PER_PAGE;

            if (wheel > 0) {
                // 向上滚动
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                // 向下滚动
                scrollOffset = Math.min(totalPages - 1, scrollOffset + 1);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
