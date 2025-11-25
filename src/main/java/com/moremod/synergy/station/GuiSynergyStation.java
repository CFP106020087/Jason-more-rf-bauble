package com.moremod.synergy.station;

import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

/**
 * Synergy 链结站 GUI
 *
 * 功能：
 * - 左侧显示玩家已安装的模块列表（可滚动）
 * - 右侧显示 6 个链结槽位（六边形布局）
 * - 拖拽模块到槽位进行链结
 * - 右键点击槽位移除模块
 * - 显示匹配的 Synergy 效果
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyStation extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/synergy_station.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;

    // 模块列表区域
    private static final int MODULE_LIST_X = 10;
    private static final int MODULE_LIST_Y = 20;
    private static final int MODULE_LIST_WIDTH = 80;
    private static final int MODULE_LIST_HEIGHT = 120;
    private static final int MODULE_ENTRY_HEIGHT = 16;
    private static final int MAX_VISIBLE_MODULES = 7;

    // 链结槽位区域（六边形中心）
    private static final int LINK_CENTER_X = 175;
    private static final int LINK_CENTER_Y = 70;
    private static final int LINK_SLOT_RADIUS = 35;
    private static final int SLOT_SIZE = 20;

    // 按钮 ID
    private static final int BTN_CLEAR_ALL = 0;
    private static final int BTN_ACTIVATE = 1;
    private static final int BTN_SCROLL_UP = 2;
    private static final int BTN_SCROLL_DOWN = 3;

    private final TileEntitySynergyStation tileEntity;
    private final EntityPlayer player;

    // 已安装模块列表
    private List<ModuleEntry> installedModules = new ArrayList<>();
    private int scrollOffset = 0;

    // 拖拽状态
    private ModuleEntry draggingModule = null;
    private int dragX, dragY;

    // 链结槽位位置（预计算）
    private final int[][] slotPositions = new int[6][2];

    // 匹配的 Synergy
    private List<SynergyDefinition> matchingSynergies = new ArrayList<>();

    /**
     * 模块条目
     */
    private static class ModuleEntry {
        final String moduleId;
        final String displayName;
        final int level;
        final boolean active;

        ModuleEntry(String id, String name, int level, boolean active) {
            this.moduleId = id;
            this.displayName = name;
            this.level = level;
            this.active = active;
        }
    }

    public GuiSynergyStation(InventoryPlayer playerInventory, TileEntitySynergyStation te) {
        super(new ContainerSynergyStation(playerInventory, te));
        this.tileEntity = te;
        this.player = playerInventory.player;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;

        // 计算六边形槽位位置
        calculateSlotPositions();

        // 加载已安装模块
        refreshInstalledModules();
    }

    /**
     * 计算六边形布局的槽位位置
     */
    private void calculateSlotPositions() {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 2; // 从顶部开始
            slotPositions[i][0] = (int) (LINK_CENTER_X + Math.cos(angle) * LINK_SLOT_RADIUS);
            slotPositions[i][1] = (int) (LINK_CENTER_Y + Math.sin(angle) * LINK_SLOT_RADIUS);
        }
    }

    /**
     * 刷新已安装模块列表
     */
    private void refreshInstalledModules() {
        installedModules.clear();

        ItemStack coreStack = ItemMechanicalCoreExtended.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取所有已安装的模块
        List<String> installedIds = ItemMechanicalCoreExtended.getInstalledUpgradeIds(coreStack);
        Map<String, ItemMechanicalCoreExtended.UpgradeInfo> allUpgrades =
                ItemMechanicalCoreExtended.getAllUpgrades();

        for (String id : installedIds) {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, id);
            boolean active = ItemMechanicalCoreExtended.isUpgradeActive(coreStack, id);

            // 获取显示名称
            String displayName = id;
            ItemMechanicalCoreExtended.UpgradeInfo info = allUpgrades.get(id);
            if (info != null && info.displayName != null) {
                displayName = info.displayName;
            }

            installedModules.add(new ModuleEntry(id, displayName, level, active));
        }

        // 按名称排序
        installedModules.sort(Comparator.comparing(e -> e.displayName));

        // 更新匹配的 Synergy
        updateMatchingSynergies();
    }

    /**
     * 更新匹配的 Synergy 列表
     */
    private void updateMatchingSynergies() {
        matchingSynergies.clear();

        List<String> linkedModules = tileEntity.getLinkedModules();
        if (linkedModules.size() < 2) return;

        // 查找包含这些模块的 Synergy
        SynergyManager manager = SynergyManager.getInstance();
        Set<String> linkedSet = new HashSet<>(linkedModules);

        for (SynergyDefinition synergy : manager.getAllSynergies()) {
            Set<String> required = synergy.getRequiredModules();
            if (linkedSet.containsAll(required)) {
                matchingSynergies.add(synergy);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        int left = guiLeft;
        int top = guiTop;

        // 清空按钮
        this.buttonList.add(new GuiButton(BTN_CLEAR_ALL, left + 130, top + 170, 50, 16, "Clear"));

        // 激活按钮
        this.buttonList.add(new GuiButton(BTN_ACTIVATE, left + 185, top + 170, 60, 16,
                tileEntity.isActivated() ? "Deactivate" : "Activate"));

        // 滚动按钮
        this.buttonList.add(new GuiButton(BTN_SCROLL_UP, left + MODULE_LIST_X + MODULE_LIST_WIDTH - 12,
                top + MODULE_LIST_Y - 2, 12, 12, "^"));
        this.buttonList.add(new GuiButton(BTN_SCROLL_DOWN, left + MODULE_LIST_X + MODULE_LIST_WIDTH - 12,
                top + MODULE_LIST_Y + MODULE_LIST_HEIGHT - 10, 12, 12, "v"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BTN_CLEAR_ALL:
                tileEntity.clearAllSlots();
                // 发送网络包（简化版：直接操作服务器端）
                sendSlotUpdate(-1, "");
                updateMatchingSynergies();
                break;

            case BTN_ACTIVATE:
                tileEntity.toggleActivated();
                button.displayString = tileEntity.isActivated() ? "Deactivate" : "Activate";
                break;

            case BTN_SCROLL_UP:
                if (scrollOffset > 0) scrollOffset--;
                break;

            case BTN_SCROLL_DOWN:
                if (scrollOffset < Math.max(0, installedModules.size() - MAX_VISIBLE_MODULES)) {
                    scrollOffset++;
                }
                break;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制背景（如果有贴图）
        // this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        // this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        // 简化版：绘制纯色背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFC6C6C6);

        // 绘制模块列表区域
        drawModuleListBackground();

        // 绘制链结区域
        drawLinkArea();
    }

    /**
     * 绘制模块列表背景
     */
    private void drawModuleListBackground() {
        int x = guiLeft + MODULE_LIST_X;
        int y = guiTop + MODULE_LIST_Y;

        // 标题
        fontRenderer.drawString("Modules", x, y - 12, 0x404040);

        // 背景
        drawRect(x - 2, y - 2, x + MODULE_LIST_WIDTH + 2, y + MODULE_LIST_HEIGHT + 2, 0xFF000000);
        drawRect(x - 1, y - 1, x + MODULE_LIST_WIDTH + 1, y + MODULE_LIST_HEIGHT + 1, 0xFF8B8B8B);
    }

    /**
     * 绘制链结区域
     */
    private void drawLinkArea() {
        int centerX = guiLeft + LINK_CENTER_X;
        int centerY = guiTop + LINK_CENTER_Y;

        // 标题
        fontRenderer.drawString("Link Slots", guiLeft + 130, guiTop + 8, 0x404040);

        // 绘制连接线
        GlStateManager.disableTexture2D();
        GlStateManager.color(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINES);

        List<Integer> occupiedSlots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (!tileEntity.isSlotEmpty(i)) {
                occupiedSlots.add(i);
            }
        }

        // 绘制已占用槽位之间的连线
        for (int i = 0; i < occupiedSlots.size(); i++) {
            for (int j = i + 1; j < occupiedSlots.size(); j++) {
                int slot1 = occupiedSlots.get(i);
                int slot2 = occupiedSlots.get(j);

                int x1 = guiLeft + slotPositions[slot1][0] + SLOT_SIZE / 2;
                int y1 = guiTop + slotPositions[slot1][1] + SLOT_SIZE / 2;
                int x2 = guiLeft + slotPositions[slot2][0] + SLOT_SIZE / 2;
                int y2 = guiTop + slotPositions[slot2][1] + SLOT_SIZE / 2;

                GL11.glVertex2f(x1, y1);
                GL11.glVertex2f(x2, y2);
            }
        }

        GL11.glEnd();
        GlStateManager.enableTexture2D();

        // 绘制中心圆
        drawCenteredCircle(centerX, centerY, 8, tileEntity.isActivated() ? 0xFF00FF00 : 0xFF808080);

        // 绘制槽位
        for (int i = 0; i < 6; i++) {
            drawSlot(i);
        }
    }

    /**
     * 绘制单个链结槽位
     */
    private void drawSlot(int slotIndex) {
        int x = guiLeft + slotPositions[slotIndex][0];
        int y = guiTop + slotPositions[slotIndex][1];

        String moduleId = tileEntity.getModuleInSlot(slotIndex);
        boolean hasModule = moduleId != null && !moduleId.isEmpty();

        // 槽位背景
        int bgColor = hasModule ? 0xFF4040FF : 0xFF404040;
        drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF000000);
        drawRect(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);

        // 显示模块名称（缩写）
        if (hasModule) {
            String abbr = moduleId.length() > 2 ? moduleId.substring(0, 2) : moduleId;
            fontRenderer.drawString(abbr, x + 4, y + 6, 0xFFFFFF);
        } else {
            fontRenderer.drawString(String.valueOf(slotIndex + 1), x + 7, y + 6, 0x808080);
        }
    }

    /**
     * 绘制中心圆形
     */
    private void drawCenteredCircle(int cx, int cy, int radius, int color) {
        GlStateManager.disableTexture2D();

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        GlStateManager.color(r, g, b, a);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            GL11.glVertex2f(cx + (float) (Math.cos(angle) * radius),
                    cy + (float) (Math.sin(angle) * radius));
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 绘制模块列表条目
        drawModuleList(mouseX, mouseY);

        // 绘制匹配的 Synergy
        drawMatchingSynergies();

        // 绘制拖拽中的模块
        if (draggingModule != null) {
            int dx = mouseX - guiLeft;
            int dy = mouseY - guiTop;
            fontRenderer.drawString(draggingModule.displayName, dx + 2, dy + 2, 0xFFFFFF);
        }

        // 绘制悬停提示
        drawHoverTooltips(mouseX, mouseY);
    }

    /**
     * 绘制模块列表
     */
    private void drawModuleList(int mouseX, int mouseY) {
        int x = MODULE_LIST_X;
        int y = MODULE_LIST_Y;

        for (int i = 0; i < MAX_VISIBLE_MODULES && scrollOffset + i < installedModules.size(); i++) {
            ModuleEntry entry = installedModules.get(scrollOffset + i);
            int entryY = y + i * MODULE_ENTRY_HEIGHT;

            // 检查是否已在链结槽位中
            boolean inLink = tileEntity.containsModule(entry.moduleId);

            // 背景
            int bgColor = inLink ? 0x80008000 : 0x80000000;
            drawRect(x, entryY, x + MODULE_LIST_WIDTH - 14, entryY + MODULE_ENTRY_HEIGHT - 1, bgColor);

            // 文字
            int textColor = entry.active ? 0xFFFFFF : 0x808080;
            String displayText = entry.displayName;
            if (displayText.length() > 10) {
                displayText = displayText.substring(0, 9) + "..";
            }
            fontRenderer.drawString(displayText, x + 2, entryY + 4, textColor);

            // 等级
            fontRenderer.drawString("L" + entry.level, x + MODULE_LIST_WIDTH - 28, entryY + 4, 0xFFFF00);
        }
    }

    /**
     * 绘制匹配的 Synergy 列表
     */
    private void drawMatchingSynergies() {
        int x = 130;
        int y = 115;

        fontRenderer.drawString("Synergies:", x, y, 0x404040);
        y += 12;

        if (matchingSynergies.isEmpty()) {
            fontRenderer.drawString("(none)", x, y, 0x808080);
        } else {
            for (int i = 0; i < Math.min(3, matchingSynergies.size()); i++) {
                SynergyDefinition synergy = matchingSynergies.get(i);
                String name = synergy.getDisplayName();
                if (name.length() > 14) name = name.substring(0, 13) + "..";
                fontRenderer.drawString("- " + name, x, y + i * 10, 0x00AA00);
            }
            if (matchingSynergies.size() > 3) {
                fontRenderer.drawString("...", x, y + 30, 0x808080);
            }
        }
    }

    /**
     * 绘制悬停提示
     */
    private void drawHoverTooltips(int mouseX, int mouseY) {
        int mx = mouseX - guiLeft;
        int my = mouseY - guiTop;

        // 检查链结槽位悬停
        for (int i = 0; i < 6; i++) {
            int sx = slotPositions[i][0];
            int sy = slotPositions[i][1];
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
                String moduleId = tileEntity.getModuleInSlot(i);
                if (moduleId != null && !moduleId.isEmpty()) {
                    List<String> tooltip = new ArrayList<>();
                    tooltip.add(moduleId);
                    tooltip.add("Right-click to remove");
                    drawHoveringText(tooltip, mx, my);
                }
                break;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int mx = mouseX - guiLeft;
        int my = mouseY - guiTop;

        // 检查模块列表点击（左键开始拖拽）
        if (mouseButton == 0 && isInModuleList(mx, my)) {
            int index = getModuleIndexAt(mx, my);
            if (index >= 0 && index < installedModules.size()) {
                ModuleEntry entry = installedModules.get(index);
                // 检查是否已在链结中
                if (!tileEntity.containsModule(entry.moduleId)) {
                    draggingModule = entry;
                    dragX = mouseX;
                    dragY = mouseY;
                }
            }
        }

        // 检查链结槽位点击
        for (int i = 0; i < 6; i++) {
            int sx = slotPositions[i][0];
            int sy = slotPositions[i][1];
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
                if (mouseButton == 1) {
                    // 右键移除
                    tileEntity.clearSlot(i);
                    sendSlotUpdate(i, "");
                    updateMatchingSynergies();
                }
                break;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if (draggingModule != null && state == 0) {
            int mx = mouseX - guiLeft;
            int my = mouseY - guiTop;

            // 检查是否放置在槽位上
            for (int i = 0; i < 6; i++) {
                int sx = slotPositions[i][0];
                int sy = slotPositions[i][1];
                if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
                    if (tileEntity.isSlotEmpty(i)) {
                        tileEntity.setModuleInSlot(i, draggingModule.moduleId);
                        sendSlotUpdate(i, draggingModule.moduleId);
                        updateMatchingSynergies();
                    }
                    break;
                }
            }

            draggingModule = null;
        }
    }

    /**
     * 发送槽位更新到服务器
     */
    private void sendSlotUpdate(int slot, String moduleId) {
        // 简化版：直接通过 Container 操作
        // 实际应该发送网络包
        if (slot == -1) {
            ((ContainerSynergyStation) inventorySlots).clearAllSlots();
        } else {
            ((ContainerSynergyStation) inventorySlots).setModuleInSlot(slot, moduleId);
        }
    }

    /**
     * 检查坐标是否在模块列表区域内
     */
    private boolean isInModuleList(int mx, int my) {
        return mx >= MODULE_LIST_X && mx < MODULE_LIST_X + MODULE_LIST_WIDTH - 14 &&
                my >= MODULE_LIST_Y && my < MODULE_LIST_Y + MODULE_LIST_HEIGHT;
    }

    /**
     * 获取模块列表中的索引
     */
    private int getModuleIndexAt(int mx, int my) {
        if (!isInModuleList(mx, my)) return -1;
        int relY = my - MODULE_LIST_Y;
        int index = scrollOffset + relY / MODULE_ENTRY_HEIGHT;
        return index < installedModules.size() ? index : -1;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 鼠标滚轮
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                if (scrollOffset > 0) scrollOffset--;
            } else {
                if (scrollOffset < Math.max(0, installedModules.size() - MAX_VISIBLE_MODULES)) {
                    scrollOffset++;
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // 定期刷新模块列表
        if (player.ticksExisted % 20 == 0) {
            refreshInstalledModules();
        }

        // 更新激活按钮文字
        for (GuiButton button : buttonList) {
            if (button.id == BTN_ACTIVATE) {
                button.displayString = tileEntity.isActivated() ? "Deactivate" : "Activate";
            }
        }
    }
}
