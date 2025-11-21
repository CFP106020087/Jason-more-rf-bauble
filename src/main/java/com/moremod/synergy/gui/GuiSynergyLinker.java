package com.moremod.synergy.gui;

import com.moremod.synergy.container.ContainerSynergyLinker;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyRegistry;
import com.moremod.synergy.data.PlayerSynergyData;
import com.moremod.synergy.network.PacketToggleSynergy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Synergy Linker GUI
 *
 * 说明：
 * - 显示所有可用的 Synergy
 * - 点击按钮激活/停用 Synergy
 * - 使用网络包同步到服务端
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyLinker extends GuiContainer {

    // 背景贴图（可选，如果没有贴图可以用纯色背景）
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("moremod", "textures/gui/synergy_linker.png");

    private final EntityPlayer player;
    private final PlayerSynergyData playerData;
    private final List<SynergyDefinition> allSynergies;

    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 5;

    public GuiSynergyLinker(EntityPlayer player) {
        super(new ContainerSynergyLinker(player));
        this.player = player;
        this.playerData = PlayerSynergyData.get(player);
        this.allSynergies = new ArrayList<>(SynergyRegistry.getInstance().getAllSynergies());

        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 清空按钮列表
        this.buttonList.clear();

        // 添加 Synergy 切换按钮
        updateButtons();
    }

    private void updateButtons() {
        this.buttonList.clear();

        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + VISIBLE_ROWS, allSynergies.size());

        for (int i = startIndex; i < endIndex; i++) {
            SynergyDefinition synergy = allSynergies.get(i);
            int displayIndex = i - startIndex;

            boolean isActivated = playerData.isSynergyActivated(synergy.getId());
            String buttonText = (isActivated ? "§a[ON] " : "§7[OFF] ") + synergy.getDisplayName();

            GuiButton button = new GuiButton(
                    i, // button ID = synergy index
                    guiLeft + 10,
                    guiTop + 20 + displayIndex * 22,
                    156,
                    20,
                    buttonText
            );

            this.buttonList.add(button);
        }

        // 滚动按钮（如果 Synergy 太多）
        if (allSynergies.size() > VISIBLE_ROWS) {
            // 向上滚动
            if (scrollOffset > 0) {
                this.buttonList.add(new GuiButton(
                        1000,
                        guiLeft + 10,
                        guiTop + 6,
                        20,
                        12,
                        "↑"
                ));
            }

            // 向下滚动
            if (scrollOffset + VISIBLE_ROWS < allSynergies.size()) {
                this.buttonList.add(new GuiButton(
                        1001,
                        guiLeft + 146,
                        guiTop + 6,
                        20,
                        12,
                        "↓"
                ));
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1000) {
            // 向上滚动
            scrollOffset = Math.max(0, scrollOffset - 1);
            updateButtons();
        } else if (button.id == 1001) {
            // 向下滚动
            scrollOffset = Math.min(allSynergies.size() - VISIBLE_ROWS, scrollOffset + 1);
            updateButtons();
        } else if (button.id >= 0 && button.id < allSynergies.size()) {
            // 切换 Synergy
            SynergyDefinition synergy = allSynergies.get(button.id);
            toggleSynergy(synergy.getId());
        }
    }

    private void toggleSynergy(String synergyId) {
        // 发送网络包到服务端
        // 注意：你需要实现 PacketToggleSynergy 和注册网络通道
        // PacketHandler.INSTANCE.sendToServer(new PacketToggleSynergy(synergyId));

        // 临时客户端处理（仅用于测试，实际需要服务端验证）
        if (playerData.isSynergyActivated(synergyId)) {
            playerData.deactivateSynergy(synergyId);
        } else {
            playerData.activateSynergy(synergyId);
        }

        updateButtons();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制背景
        // 如果没有贴图，可以用纯色：
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xC0101010);

        // 如果有贴图：
        // this.mc.getTextureManager().bindTexture(TEXTURE);
        // this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 绘制标题
        String title = "§6Synergy Linker";
        this.fontRenderer.drawString(title, 8, 6, 0xFFFFFF);

        // 绘制提示信息
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + VISIBLE_ROWS, allSynergies.size());

        for (int i = startIndex; i < endIndex; i++) {
            SynergyDefinition synergy = allSynergies.get(i);
            int displayIndex = i - startIndex;
            int yPos = 135 + displayIndex * 22;

            // 如果鼠标悬停在按钮上，显示描述
            GuiButton button = this.buttonList.get(displayIndex);
            if (button != null && mouseX >= button.x && mouseX <= button.x + button.width &&
                mouseY >= button.y && mouseY <= button.y + button.height) {

                // 显示 Synergy 描述
                List<String> tooltip = new ArrayList<>();
                tooltip.add("§e" + synergy.getDisplayName());
                tooltip.add("§7" + synergy.getDescription());

                // 你可以在这里添加更多信息，比如所需模块

                // 注意：这里应该用 drawHoveringText，但需要调整坐标
                // this.drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
