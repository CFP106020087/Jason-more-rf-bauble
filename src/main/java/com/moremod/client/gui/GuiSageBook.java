package com.moremod.client.gui;

import com.moremod.moremod;
import com.moremod.network.PacketCreateEnchantedBook;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dhanantry.scapeandrunparasites.SRPMain.network;

@SideOnly(Side.CLIENT)
public class GuiSageBook extends GuiScreen {

    private EntityPlayer player;
    private EnumHand hand;
    private List<Enchantment> allEnchantments;
    private List<Enchantment> selectedEnchantments = new ArrayList<>();
    private int guiLeft, guiTop;
    private int xSize = 300;
    private int ySize = 220;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 6;

    public GuiSageBook(EntityPlayer player, EnumHand hand) {
        this.player = player;
        this.hand = hand;
        this.allEnchantments = new ArrayList<>();
        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS.getValuesCollection()) {
            if (ench != null) {
                this.allEnchantments.add(ench);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        refreshButtons();
    }

    private void refreshButtons() {
        this.buttonList.clear();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allEnchantments.size());

        for (int i = startIndex; i < endIndex; i++) {
            Enchantment ench = allEnchantments.get(i);
            int btnIndex = i - startIndex;
            boolean selected = selectedEnchantments.contains(ench);

            this.buttonList.add(new GuiButton(
                    i,
                    guiLeft + 20,
                    guiTop + 35 + (btnIndex * 25),
                    xSize - 40,
                    20,
                    (selected ? "§a● " : "○ ") + ench.getTranslatedName(1).replace(" I", "")
            ));
        }

        int maxPage = (allEnchantments.size() - 1) / ITEMS_PER_PAGE;
        if (page > 0) {
            this.buttonList.add(new GuiButton(1000, guiLeft + 20, guiTop + 190, 30, 20, "<"));
        }
        if (page < maxPage) {
            this.buttonList.add(new GuiButton(1001, guiLeft + xSize - 50, guiTop + 190, 30, 20, ">"));
        }

        GuiButton confirm = new GuiButton(
                2000,
                guiLeft + xSize/2 - 40,
                guiTop + 190,
                80,
                20,
                "确认"
        );
        confirm.enabled = selectedEnchantments.size() == 3;
        this.buttonList.add(confirm);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 0 && button.id < allEnchantments.size()) {
            Enchantment ench = allEnchantments.get(button.id);
            if (selectedEnchantments.contains(ench)) {
                selectedEnchantments.remove(ench);
            } else if (selectedEnchantments.size() < 3) {
                selectedEnchantments.add(ench);
            }
            refreshButtons();
        } else if (button.id == 1000) {
            page--;
            refreshButtons();
        } else if (button.id == 1001) {
            page++;
            refreshButtons();
        } else if (button.id == 2000 && selectedEnchantments.size() == 3) {
            // 发送数据包，包含手的信息
            List<Integer> ids = new ArrayList<>();
            for (Enchantment ench : selectedEnchantments) {
                ids.add(Enchantment.getEnchantmentID(ench));
            }
            network.sendToServer(new PacketCreateEnchantedBook(ids, hand == EnumHand.MAIN_HAND));
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xE0101010);

        drawCenteredString(fontRenderer, "贤者之书", guiLeft + xSize/2, guiTop + 8, 0xFFFFFF);

        String progress = "选择进度: " + selectedEnchantments.size() + "/3";
        drawCenteredString(fontRenderer, progress, guiLeft + xSize/2, guiTop + 20,
                selectedEnchantments.size() == 3 ? 0x55FF55 : 0xAAAAAA);

        int maxPage = (allEnchantments.size() - 1) / ITEMS_PER_PAGE;
        if (maxPage > 0) {
            String pageText = (page + 1) + "/" + (maxPage + 1);
            drawCenteredString(fontRenderer, pageText, guiLeft + xSize/2, guiTop + 195, 0x888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}