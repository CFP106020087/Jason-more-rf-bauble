package com.moremod.system.humanity.intel;

import com.moremod.network.NetworkHandler;
import com.moremod.network.PacketDeactivateIntelProfile;
import com.moremod.system.humanity.BiologicalProfile;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

/**
 * 情报统计书GUI
 * Intel Statistics Book GUI
 *
 * 显示情报系统状态，可以卸除激活的档案
 */
@SideOnly(Side.CLIENT)
public class GuiIntelStatistics extends GuiScreen {

    private final EntityPlayer player;
    private IHumanityData data;

    // GUI尺寸
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private int guiLeft;
    private int guiTop;

    // 滚动相关
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 24;
    private static final int VISIBLE_ENTRIES = 5;

    // 激活的档案列表（用于按钮映射）
    private List<ResourceLocation> activeProfileList = new ArrayList<>();

    // 已学习的情报列表
    private List<ResourceLocation> learnedIntelList = new ArrayList<>();

    // 刷新计时器（每秒刷新一次）
    private int refreshTimer = 0;
    private static final int REFRESH_INTERVAL = 20; // 20 ticks = 1秒

    public GuiIntelStatistics(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        refreshData();
        initButtons();
    }

    private void refreshData() {
        data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            activeProfileList = new ArrayList<>(data.getActiveProfiles());
            // 收集已学习的情报
            learnedIntelList = new ArrayList<>(data.getLearnedIntel().keySet());
        }
    }

    private void initButtons() {
        buttonList.clear();

        if (data == null) return;

        // 为每个激活的档案创建"卸除"按钮（最多显示3个）
        int buttonId = 0;
        int startY = guiTop + 96; // 调整起始位置

        int maxShow = Math.min(3, activeProfileList.size());
        for (int i = scrollOffset; i < Math.min(activeProfileList.size(), scrollOffset + maxShow); i++) {
            int yPos = startY + (i - scrollOffset) * 11;
            // 卸除按钮
            buttonList.add(new GuiButton(buttonId++, guiLeft + GUI_WIDTH - 55, yPos - 1, 40, 10, "§c卸除"));
        }

        // 关闭按钮
        buttonList.add(new GuiButton(100, guiLeft + GUI_WIDTH / 2 - 40, guiTop + GUI_HEIGHT - 25, 80, 20, "关闭"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) {
            // 关闭按钮
            mc.displayGuiScreen(null);
            return;
        }

        // 卸除按钮
        int index = scrollOffset + button.id;
        if (index >= 0 && index < activeProfileList.size()) {
            ResourceLocation entityId = activeProfileList.get(index);

            // 发送网络包到服务端卸除档案
            NetworkHandler.INSTANCE.sendToServer(new PacketDeactivateIntelProfile(entityId));

            // 本地更新（服务端会同步）
            if (data != null) {
                data.deactivateProfile(entityId);
            }

            // 刷新
            refreshData();
            initButtons();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 每秒刷新一次数据
        refreshTimer++;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            refreshData();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制半透明背景
        drawDefaultBackground();

        // 绘制主面板背景
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC1A1A2E);

        // 绘制边框
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop, 0xFFFFD700);
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFFFFD700);
        drawVerticalLine(guiLeft, guiTop, guiTop + GUI_HEIGHT - 1, 0xFFFFD700);
        drawVerticalLine(guiLeft + GUI_WIDTH - 1, guiTop, guiTop + GUI_HEIGHT - 1, 0xFFFFD700);

        // ========== 标题 ==========
        String title = "§b§l情报整合统计书";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawStringWithShadow(title, guiLeft + (GUI_WIDTH - titleWidth) / 2, guiTop + 8, 0xFFFFFF);

        // 分隔线
        drawHorizontalLine(guiLeft + 10, guiLeft + GUI_WIDTH - 10, guiTop + 22, 0xFFFFD700);

        if (data == null) {
            fontRenderer.drawStringWithShadow("§c无法读取数据", guiLeft + 20, guiTop + 40, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        // ========== 基础状态 ==========
        float humanity = data.getHumanity();
        int maxSlots = data.getMaxActiveProfiles();
        Set<ResourceLocation> activeProfiles = data.getActiveProfiles();
        Map<ResourceLocation, BiologicalProfile> allProfiles = data.getProfiles();

        int y = guiTop + 30;

        fontRenderer.drawStringWithShadow("§e【基础状态】", guiLeft + 10, y, 0xFFFFFF);
        y += 12;

        fontRenderer.drawStringWithShadow(String.format("人性值: §a%.1f%%", humanity), guiLeft + 15, y, 0xFFFFFF);
        fontRenderer.drawStringWithShadow(String.format("槽位: §b%d§7/§b%d", activeProfiles.size(), maxSlots),
                guiLeft + 120, y, 0xFFFFFF);
        fontRenderer.drawStringWithShadow(String.format("记录: §d%d种", allProfiles.size()),
                guiLeft + 190, y, 0xFFFFFF);
        y += 12;

        // ========== 情报加成总览 ==========
        int totalIntelTypes = IntelDataHelper.getIntelTypesLearned(data);
        int totalIntelLevels = IntelDataHelper.getTotalIntelLearned(data);
        float avgDmgBonus = totalIntelTypes > 0 ? (totalIntelLevels * ItemIntelBook.DAMAGE_BONUS_PER_BOOK * 100) / totalIntelTypes : 0;

        fontRenderer.drawStringWithShadow(String.format("§c情报书: §f%d种 §7(共%d级)", totalIntelTypes, totalIntelLevels),
                guiLeft + 15, y, 0xFFFFFF);
        if (totalIntelTypes > 0) {
            fontRenderer.drawStringWithShadow(String.format("§c平均+%.0f%%伤害", avgDmgBonus),
                    guiLeft + 160, y, 0xFFFFFF);
        }
        y += 16;

        // 分隔线
        drawHorizontalLine(guiLeft + 10, guiLeft + GUI_WIDTH - 10, y, 0x88FFFF00);
        y += 6;

        // ========== 激活的档案列表 ==========
        fontRenderer.drawStringWithShadow("§e【激活的猎人档案】 §7(点击卸除释放槽位)", guiLeft + 10, y, 0xFFFFFF);
        y += 12;

        if (activeProfileList.isEmpty()) {
            fontRenderer.drawStringWithShadow("§7（无激活的档案）", guiLeft + 20, y, 0xFFFFFF);
            y += 12;
        } else {
            // 绘制档案列表（最多显示3条）
            int maxShow = Math.min(3, activeProfileList.size());
            for (int i = scrollOffset; i < Math.min(activeProfileList.size(), scrollOffset + maxShow); i++) {
                ResourceLocation entityId = activeProfileList.get(i);
                BiologicalProfile profile = data.getProfile(entityId);

                if (profile != null) {
                    // 背景条
                    int bgColor = (i % 2 == 0) ? 0x44000000 : 0x22000000;
                    drawRect(guiLeft + 10, y, guiLeft + GUI_WIDTH - 60, y + 10, bgColor);

                    // 档案信息
                    String entityName = entityId.getPath().replace("_", " ");
                    String tierName = ItemIntelStatisticsBook.getTierName(profile.getCurrentTier());
                    int tierColor = ItemIntelStatisticsBook.getTierColorInt(profile.getCurrentTier());

                    float dmgBonus = profile.getDamageBonus() * 100;
                    int intelLevel = IntelDataHelper.getIntelLevel(data, entityId);
                    float totalDmg = dmgBonus;
                    if (intelLevel > 0) {
                        totalDmg += (IntelDataHelper.calculateDamageMultiplier(data, entityId) - 1.0f) * 100;
                    }

                    String info = String.format("§f%s §7[%s] §c+%d%% §7样本:%d 击杀:%d",
                            entityName, tierName, (int) totalDmg,
                            profile.getSampleCount(), profile.getKillCount());
                    fontRenderer.drawStringWithShadow(info, guiLeft + 12, y + 1, tierColor);
                    y += 11;
                }
            }

            // 滚动提示
            if (activeProfileList.size() > 3) {
                fontRenderer.drawStringWithShadow(String.format("§7...还有 %d 个档案 (滚轮翻页)",
                        activeProfileList.size() - 3), guiLeft + 15, y, 0xFFFFFF);
                y += 10;
            }
        }

        y += 4;
        // 分隔线
        drawHorizontalLine(guiLeft + 10, guiLeft + GUI_WIDTH - 10, y, 0x88FF55FF);
        y += 6;

        // ========== 已学习的情报书 ==========
        fontRenderer.drawStringWithShadow("§d【已学习的情报书】", guiLeft + 10, y, 0xFFFFFF);
        y += 12;

        if (learnedIntelList.isEmpty()) {
            fontRenderer.drawStringWithShadow("§7（未学习任何情报书）", guiLeft + 20, y, 0xFFFFFF);
        } else {
            // 绘制情报列表
            int intelCount = 0;
            for (ResourceLocation entityId : learnedIntelList) {
                if (intelCount >= 4) {
                    fontRenderer.drawStringWithShadow(String.format("§7...还有 %d 种情报",
                            learnedIntelList.size() - 4), guiLeft + 15, y, 0xFFFFFF);
                    break;
                }

                int level = IntelDataHelper.getIntelLevel(data, entityId);
                float dmgBonus = (IntelDataHelper.calculateDamageMultiplier(data, entityId) - 1.0f) * 100;
                String entityName = entityId.getPath().replace("_", " ");

                String info = String.format("§f%s §5Lv.%d §7(+%d%%伤害)",
                        entityName, level, (int) dmgBonus);
                fontRenderer.drawStringWithShadow(info, guiLeft + 15, y, 0xFFFFFF);
                y += 10;
                intelCount++;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 处理滚轮
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int maxVisible = 3;
            if (scroll > 0 && scrollOffset > 0) {
                scrollOffset--;
                initButtons();
            } else if (scroll < 0 && scrollOffset < activeProfileList.size() - maxVisible) {
                scrollOffset++;
                initButtons();
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
