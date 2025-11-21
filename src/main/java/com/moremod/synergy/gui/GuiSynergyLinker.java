package com.moremod.synergy.gui;

import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyRegistry;
import com.moremod.synergy.data.PlayerSynergyData;
import com.moremod.synergy.network.PacketToggleSynergy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Synergy Linker GUI - 参考 MechanicalCoreGui 风格
 *
 * 说明：
 * - 显示所有可用的 Synergy
 * - 点击开关按钮激活/停用 Synergy
 * - 鼠标悬停显示详细信息
 * - 支持滚动和滚轮
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyLinker extends GuiScreen {

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 180;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/synergy_linker.png");

    // 按钮ID
    private static final int BUTTON_CLOSE = 0;
    private static final int SYNERGIES_PER_PAGE = 6;

    private final EntityPlayer player;
    private final PlayerSynergyData playerData;
    private final List<SynergyDefinition> allSynergies;

    private int scrollOffset = 0;
    private int guiLeft;
    private int guiTop;

    public GuiSynergyLinker(EntityPlayer player) {
        this.player = player;
        this.playerData = PlayerSynergyData.get(player);
        this.allSynergies = new ArrayList<>(SynergyRegistry.getInstance().getAllSynergies());
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.buttonList.clear();

        // 关闭按钮
        this.buttonList.add(new GuiButton(BUTTON_CLOSE, guiLeft + GUI_WIDTH - 25, guiTop + 5, 20, 20, "×"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawGuiBackground();
        drawTitle();
        drawSynergyList(mouseX, mouseY);
        drawScrollBar();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawGuiBackground() {
        GlStateManager.color(1, 1, 1, 1);
        try {
            this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        } catch (Exception ignored) {
        }

        // 外框
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        // 内框
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xC0383838);
        // 标题栏
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + 25, 0xC0505050);
    }

    private void drawTitle() {
        String title = "Synergy Linker - 协同链结器";
        int titleX = guiLeft + (GUI_WIDTH - this.fontRenderer.getStringWidth(title)) / 2;
        this.fontRenderer.drawStringWithShadow(title, titleX, guiTop + 10, 0xFFD700);

        // 统计信息
        int activeCount = playerData.getActivatedSynergies().size();
        String stats = TextFormatting.GRAY + "已激活: " + TextFormatting.GREEN + activeCount +
                TextFormatting.GRAY + " / " + allSynergies.size();
        this.fontRenderer.drawString(stats, guiLeft + 10, guiTop + 30, 0xCCCCCC);
    }

    private void drawSynergyList(int mouseX, int mouseY) {
        int listX = guiLeft + 10;
        int listY = guiTop + 45;
        int listW = GUI_WIDTH - 40;
        int listH = 120;

        // 列表背景
        drawRect(listX, listY, listX + listW, listY + listH, 0x80000000);

        if (allSynergies.isEmpty()) {
            String emptyText = "未注册任何 Synergy";
            int textX = listX + (listW - this.fontRenderer.getStringWidth(emptyText)) / 2;
            int textY = listY + listH / 2;
            this.fontRenderer.drawString(emptyText, textX, textY, 0x888888);
            return;
        }

        int visible = Math.min(SYNERGIES_PER_PAGE, allSynergies.size() - scrollOffset);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            if (idx >= allSynergies.size()) break;

            SynergyDefinition synergy = allSynergies.get(idx);
            int y = listY + 5 + i * 19;
            drawSynergyEntry(synergy, listX + 5, y, listW - 10, mouseX, mouseY);
        }
    }

    private void drawSynergyEntry(SynergyDefinition synergy, int x, int y, int w, int mouseX, int mouseY) {
        boolean isActivated = playerData.isSynergyActivated(synergy.getId());

        // 背景色（鼠标悬停高亮）
        int bg = 0x40000000;
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 17) {
            bg = 0x60000000;
        }

        // 激活状态不同颜色
        if (isActivated) {
            bg = 0x60004000; // 绿色调
        }

        drawRect(x, y, x + w, y + 17, bg);

        // 左侧状态指示条
        int indicatorColor = isActivated ? 0xFF00FF00 : 0xFF888888;
        drawRect(x, y, x + 2, y + 17, indicatorColor);

        // Synergy 名称
        String name = synergy.getDisplayName();
        int nameColor = isActivated ? 0xFFFFFF : 0x888888;
        this.fontRenderer.drawString(name, x + 6, y + 2, nameColor);

        // 状态文本
        String statusText = isActivated ? "ON" : "OFF";
        int statusColor = isActivated ? 0x88FF88 : 0x666666;
        this.fontRenderer.drawString(statusText, x + w - 60, y + 2, statusColor);

        // 开关按钮（可点击区域）
        int btnX = x + w - 45;
        int btnY = y + 2;
        int btnW = 40;
        int btnH = 13;

        boolean hover = mouseX >= btnX && mouseX <= btnX + btnW &&
                mouseY >= btnY && mouseY <= btnY + btnH;

        // 按钮背景
        int btnBg = hover ? (isActivated ? 0x80FF4444 : 0x8044FF44) :
                (isActivated ? 0x80444444 : 0x80444444);
        drawRect(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);

        // 按钮文字
        String btnText = isActivated ? "停用" : "激活";
        int btnTextColor = hover ? 0xFFFFFF : 0xCCCCCC;
        int btnTextX = btnX + (btnW - this.fontRenderer.getStringWidth(btnText)) / 2;
        this.fontRenderer.drawString(btnText, btnTextX, btnY + 3, btnTextColor);
    }

    private void drawScrollBar() {
        if (allSynergies.size() <= SYNERGIES_PER_PAGE) return;

        int x = guiLeft + GUI_WIDTH - 25;
        int y = guiTop + 45;
        int h = 120;

        // 滚动条背景
        drawRect(x, y, x + 10, y + h, 0x80000000);

        // 滚动条滑块
        float ratio = (float) scrollOffset / Math.max(1, allSynergies.size() - SYNERGIES_PER_PAGE);
        int sliderH = Math.max(10, h * SYNERGIES_PER_PAGE / allSynergies.size());
        int sy = y + (int) ((h - sliderH) * ratio);
        drawRect(x + 1, sy, x + 9, sy + sliderH, 0xFFAAAAAA);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        // 关闭按钮提示
        for (GuiButton button : buttonList) {
            if (button.isMouseOver() && button.visible && button.id == BUTTON_CLOSE) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(TextFormatting.YELLOW + "关闭界面");
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }

        // Synergy 条目提示
        int listX = guiLeft + 10;
        int listY = guiTop + 45;
        int listW = GUI_WIDTH - 40;
        int listH = 120;

        if (mouseX < listX + 5 || mouseX > listX + listW - 5 ||
                mouseY < listY + 5 || mouseY > listY + listH - 5) return;

        int relY = mouseY - listY - 5;
        if (relY < 0) return;

        int idx = scrollOffset + relY / 19;
        if (idx < 0 || idx >= allSynergies.size()) return;

        SynergyDefinition synergy = allSynergies.get(idx);
        List<String> tooltip = new ArrayList<>();

        // 标题
        tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + synergy.getDisplayName());

        // 描述
        String desc = synergy.getDescription();
        if (desc != null && !desc.isEmpty()) {
            // 分割长描述
            String[] parts = desc.split("\\|");
            for (String part : parts) {
                tooltip.add(TextFormatting.GRAY + part.trim());
            }
        }

        // 状态
        boolean isActivated = playerData.isSynergyActivated(synergy.getId());
        if (isActivated) {
            tooltip.add("");
            tooltip.add(TextFormatting.GREEN + "✓ 已激活");
            tooltip.add(TextFormatting.GRAY + "Synergy 效果正在生效");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "○ 未激活");
            tooltip.add(TextFormatting.GRAY + "点击激活按钮启用");
        }

        // 模块链信息
        if (synergy.getChain() != null && !synergy.getChain().getModules().isEmpty()) {
            tooltip.add("");
            tooltip.add(TextFormatting.AQUA + "所需模块:");
            for (String moduleId : synergy.getChain().getModules()) {
                tooltip.add(TextFormatting.GRAY + "  • " + moduleId);
            }
        }

        this.drawHoveringText(tooltip, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        handleSynergyClick(mouseX, mouseY, mouseButton);
        handleScrollBarClick(mouseX, mouseY, mouseButton);
    }

    private void handleSynergyClick(int mouseX, int mouseY, int mouseButton) {
        int listX = guiLeft + 10;
        int listY = guiTop + 45;
        int listW = GUI_WIDTH - 40;
        int listH = 120;

        if (mouseX < listX + 5 || mouseX > listX + listW - 5 ||
                mouseY < listY + 5 || mouseY > listY + listH - 5) return;

        int relY = mouseY - listY - 5;
        if (relY < 0) return;

        int idx = scrollOffset + relY / 19;
        if (idx < 0 || idx >= allSynergies.size()) return;

        SynergyDefinition synergy = allSynergies.get(idx);

        // 计算按钮位置
        int entryX = listX + 5;
        int entryY = listY + 5 + (idx - scrollOffset) * 19;
        int btnX = entryX + listW - 50;
        int btnY = entryY + 2;
        int btnW = 40;
        int btnH = 13;

        // 检查是否点击按钮
        if (mouseX >= btnX && mouseX <= btnX + btnW &&
                mouseY >= btnY && mouseY <= btnY + btnH) {
            toggleSynergy(synergy.getId());
        }
    }

    private void toggleSynergy(String synergyId) {
        boolean wasActivated = playerData.isSynergyActivated(synergyId);

        // 发送网络包到服务端
        // 注意：需要你在网络通道中注册 PacketToggleSynergy
        // 取消注释下面这行：
        // NetworkHandler.INSTANCE.sendToServer(new PacketToggleSynergy(synergyId));

        // 临时客户端处理（仅用于测试）
        if (wasActivated) {
            playerData.deactivateSynergy(synergyId);
            player.playSound(SoundEvents.BLOCK_NOTE_BASS, 0.7F, 0.8F);
        } else {
            playerData.activateSynergy(synergyId);
            player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 0.7F, 1.2F);
        }

        playerData.saveToPlayer(player);
    }

    private void handleScrollBarClick(int mouseX, int mouseY, int mouseButton) {
        if (allSynergies.size() <= SYNERGIES_PER_PAGE) return;

        int x = guiLeft + GUI_WIDTH - 25;
        int y = guiTop + 45;
        int h = 120;

        if (mouseX >= x && mouseX <= x + 10 && mouseY >= y && mouseY <= y + h) {
            float ratio = (float) (mouseY - y) / h;
            int maxOffset = Math.max(0, allSynergies.size() - SYNERGIES_PER_PAGE);
            scrollOffset = Math.max(0, Math.min((int) (ratio * maxOffset), maxOffset));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 滚轮支持
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && allSynergies.size() > SYNERGIES_PER_PAGE) {
            int dir = wheel > 0 ? -1 : 1;
            int maxOffset = Math.max(0, allSynergies.size() - SYNERGIES_PER_PAGE);
            scrollOffset = Math.max(0, Math.min(scrollOffset + dir, maxOffset));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_CLOSE) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
