package com.moremod.synergy.gui;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyRegistry;
import com.moremod.synergy.data.PlayerSynergyData;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

/**
 * Synergy Linker GUI - 拖拽式链结界面
 *
 * 功能：
 * - 左侧：玩家已拥有的模块列表
 * - 中间：链结槽位（可拖拽模块进入）
 * - 连线效果：槽位之间显示连接线
 * - 右侧：检测到的可用 Synergy 和激活按钮
 * - 强反馈：拖拽、连线、高亮、动画
 */
@SideOnly(Side.CLIENT)
public class GuiSynergyLinker extends GuiScreen {

    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 200;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/synergy_linker.png");

    // 链结槽位数量
    private static final int LINK_SLOTS = 8;

    private final EntityPlayer player;
    private final PlayerSynergyData playerData;

    // 玩家拥有的模块列表
    private final List<ModuleEntry> ownedModules = new ArrayList<>();

    // 链结槽位中的模块
    private final String[] linkSlots = new String[LINK_SLOTS];

    // 当前检测到的可用 Synergy
    private final List<SynergyDefinition> detectedSynergies = new ArrayList<>();

    // 拖拽状态
    private String draggingModule = null;
    private int dragX = 0;
    private int dragY = 0;

    // 槽位位置
    private final SlotPosition[] slotPositions = new SlotPosition[LINK_SLOTS];

    private int guiLeft;
    private int guiTop;

    private int moduleListScroll = 0;

    // 按钮ID
    private static final int BUTTON_CLOSE = 0;
    private static final int BUTTON_CLEAR = 1;
    private static final int BUTTON_ACTIVATE_BASE = 100; // 激活按钮起始ID

    public GuiSynergyLinker(EntityPlayer player) {
        this.player = player;
        this.playerData = PlayerSynergyData.get(player);

        // 初始化槽位
        Arrays.fill(linkSlots, null);

        // 加载玩家拥有的模块
        loadOwnedModules();

        // 初始化槽位位置（圆形排列）
        initSlotPositions();
    }

    private void loadOwnedModules() {
        ownedModules.clear();

        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return;

        // 获取所有升级
        Set<String> processedIds = new HashSet<>();

        // 基础升级
        for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
            String id = type.getKey();
            if (processedIds.contains(id.toUpperCase())) continue;

            int level = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0 || hasUpgradeMarker(nbt, id)) {
                ownedModules.add(new ModuleEntry(id, type.getDisplayName(), type.getColor()));
                processedIds.add(id.toUpperCase());
            }
        }

        // 扩展升级（如果有）
        // 这里可以添加从 ItemMechanicalCoreExtended 读取的逻辑
    }

    private boolean hasUpgradeMarker(NBTTagCompound nbt, String id) {
        return nbt.getBoolean("HasUpgrade_" + id) ||
                nbt.getBoolean("HasUpgrade_" + id.toUpperCase()) ||
                nbt.getInteger("OwnedMax_" + id) > 0;
    }

    private void initSlotPositions() {
        // 中心位置
        int centerX = GUI_WIDTH / 2;
        int centerY = 80;
        int radius = 45;

        // 圆形排列槽位
        for (int i = 0; i < LINK_SLOTS; i++) {
            double angle = (Math.PI * 2 * i / LINK_SLOTS) - Math.PI / 2; // 从顶部开始
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = centerY + (int) (Math.sin(angle) * radius);
            slotPositions[i] = new SlotPosition(x, y);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.buttonList.clear();

        // 关闭按钮
        this.buttonList.add(new GuiButton(BUTTON_CLOSE, guiLeft + GUI_WIDTH - 25, guiTop + 5, 20, 20, "×"));

        // 清空槽位按钮
        this.buttonList.add(new GuiButton(BUTTON_CLEAR, guiLeft + GUI_WIDTH / 2 - 30, guiTop + 145, 60, 15, "清空槽位"));

        // 检测可用 Synergy
        detectSynergies();

        // 创建激活按钮
        updateActivateButtons();
    }

    private void detectSynergies() {
        detectedSynergies.clear();

        // 收集槽位中的模块ID
        Set<String> slotModules = new HashSet<>();
        for (String moduleId : linkSlots) {
            if (moduleId != null) {
                slotModules.add(moduleId);
            }
        }

        if (slotModules.isEmpty()) return;

        // 查找匹配的 Synergy
        List<SynergyDefinition> all = new ArrayList<>(SynergyRegistry.getInstance().getAllSynergies());
        for (SynergyDefinition synergy : all) {
            if (synergy.getChain() == null) continue;

            Set<String> requiredModules = synergy.getChain().getModules();
            if (slotModules.containsAll(requiredModules)) {
                detectedSynergies.add(synergy);
            }
        }
    }

    private void updateActivateButtons() {
        // 移除旧的激活按钮
        buttonList.removeIf(btn -> btn.id >= BUTTON_ACTIVATE_BASE);

        // 为每个检测到的 Synergy 创建激活按钮
        int rightPanelX = guiLeft + GUI_WIDTH - 95;
        int startY = guiTop + 45;

        for (int i = 0; i < Math.min(detectedSynergies.size(), 4); i++) {
            SynergyDefinition synergy = detectedSynergies.get(i);
            boolean isActivated = playerData.isSynergyActivated(synergy.getId());

            String btnText = isActivated ? "§a✓" : "激活";
            GuiButton btn = new GuiButton(
                    BUTTON_ACTIVATE_BASE + i,
                    rightPanelX + 5,
                    startY + i * 25,
                    35,
                    20,
                    btnText
            );
            btn.enabled = !isActivated;
            this.buttonList.add(btn);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawGuiBackground();
        drawTitle();
        drawLeftPanel(mouseX, mouseY);   // 模块列表
        drawCenterPanel(mouseX, mouseY);  // 槽位和连线
        drawRightPanel(mouseX, mouseY);   // 检测到的 Synergy
        drawDraggingModule(mouseX, mouseY); // 拖拽中的模块
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawGuiBackground() {
        GlStateManager.color(1, 1, 1, 1);

        // 外框
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        // 内框
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xC0383838);
        // 标题栏
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + 25, 0xC0505050);
    }

    private void drawTitle() {
        String title = "§6Synergy Linker - 协同链结器";
        int titleX = guiLeft + (GUI_WIDTH - this.fontRenderer.getStringWidth(title)) / 2;
        this.fontRenderer.drawStringWithShadow(title, titleX, guiTop + 10, 0xFFD700);
    }

    private void drawLeftPanel(int mouseX, int mouseY) {
        int panelX = guiLeft + 5;
        int panelY = guiTop + 30;
        int panelW = 90;
        int panelH = 155;

        // 面板背景
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0x80000000);

        // 标题
        this.fontRenderer.drawString("§e可用模块", panelX + 5, panelY + 3, 0xFFFFFF);

        // 模块列表
        int listY = panelY + 15;
        int visible = Math.min(8, ownedModules.size() - moduleListScroll);

        for (int i = 0; i < visible; i++) {
            int idx = moduleListScroll + i;
            if (idx >= ownedModules.size()) break;

            ModuleEntry module = ownedModules.get(idx);
            int y = listY + i * 17;

            // 检查是否已在槽位中
            boolean inSlot = Arrays.asList(linkSlots).contains(module.id);

            // 背景
            int bg = 0x40000000;
            if (inSlot) {
                bg = 0x40404000; // 黄色调（已使用）
            } else if (mouseX >= panelX + 5 && mouseX <= panelX + panelW - 5 &&
                    mouseY >= y && mouseY <= y + 15) {
                bg = 0x60000000; // 悬停高亮
            }

            drawRect(panelX + 5, y, panelX + panelW - 5, y + 15, bg);

            // 模块名称
            String name = module.displayName;
            if (name.length() > 10) {
                name = name.substring(0, 9) + "..";
            }

            int color = inSlot ? 0x888888 : module.color.getColorIndex();
            this.fontRenderer.drawString(name, panelX + 8, y + 3, color);
        }

        // 滚动条（如果需要）
        if (ownedModules.size() > 8) {
            int scrollBarX = panelX + panelW - 8;
            int scrollBarY = panelY + 15;
            int scrollBarH = 135;

            drawRect(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarH, 0x80000000);

            float ratio = (float) moduleListScroll / Math.max(1, ownedModules.size() - 8);
            int sliderH = Math.max(10, scrollBarH * 8 / ownedModules.size());
            int sy = scrollBarY + (int) ((scrollBarH - sliderH) * ratio);
            drawRect(scrollBarX + 1, sy, scrollBarX + 5, sy + sliderH, 0xFFAAAAAA);
        }
    }

    private void drawCenterPanel(int mouseX, int mouseY) {
        // 绘制连线（在槽位背景之前）
        drawLinks();

        // 绘制槽位
        for (int i = 0; i < LINK_SLOTS; i++) {
            SlotPosition pos = slotPositions[i];
            int x = guiLeft + pos.x - 12;
            int y = guiTop + pos.y - 12;

            String moduleId = linkSlots[i];
            boolean isEmpty = moduleId == null;

            // 槽位背景
            int slotBg = isEmpty ? 0x80303030 : 0x80004000; // 空槽/有模块
            if (!isEmpty && mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24) {
                slotBg = 0x80005000; // 悬停高亮
            }

            drawRect(x, y, x + 24, y + 24, slotBg);
            drawRect(x, y, x + 24, y + 1, 0xFF888888); // 边框
            drawRect(x, y, x + 1, y + 24, 0xFF888888);
            drawRect(x + 23, y, x + 24, y + 24, 0xFF444444);
            drawRect(x, y + 23, x + 24, y + 24, 0xFF444444);

            // 槽位编号
            String slotNum = String.valueOf(i + 1);
            this.fontRenderer.drawString(slotNum, x + 2, y + 2, 0x666666);

            // 如果有模块，显示模块名称缩写
            if (!isEmpty) {
                ModuleEntry module = findModule(moduleId);
                if (module != null) {
                    String abbr = getAbbreviation(module.displayName);
                    int textX = x + 12 - this.fontRenderer.getStringWidth(abbr) / 2;
                    int textY = y + 13;
                    this.fontRenderer.drawString(abbr, textX, textY, module.color.getColorIndex());
                }
            } else {
                // 空槽位提示
                this.fontRenderer.drawString("+", x + 9, y + 8, 0x444444);
            }
        }
    }

    private void drawLinks() {
        // 只在有2个或以上模块时绘制连线
        List<Integer> filledSlots = new ArrayList<>();
        for (int i = 0; i < LINK_SLOTS; i++) {
            if (linkSlots[i] != null) {
                filledSlots.add(i);
            }
        }

        if (filledSlots.size() < 2) return;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glLineWidth(2.0f);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(0.0f, 1.0f, 0.5f, 0.6f); // 青绿色连线

        // 连接所有填充的槽位
        for (int i = 0; i < filledSlots.size() - 1; i++) {
            int slot1 = filledSlots.get(i);
            int slot2 = filledSlots.get(i + 1);

            SlotPosition pos1 = slotPositions[slot1];
            SlotPosition pos2 = slotPositions[slot2];

            GL11.glVertex2f(guiLeft + pos1.x, guiTop + pos1.y);
            GL11.glVertex2f(guiLeft + pos2.x, guiTop + pos2.y);
        }

        // 闭合连线（如果有3个以上）
        if (filledSlots.size() >= 3) {
            int firstSlot = filledSlots.get(0);
            int lastSlot = filledSlots.get(filledSlots.size() - 1);

            SlotPosition pos1 = slotPositions[firstSlot];
            SlotPosition pos2 = slotPositions[lastSlot];

            GL11.glVertex2f(guiLeft + pos1.x, guiTop + pos1.y);
            GL11.glVertex2f(guiLeft + pos2.x, guiTop + pos2.y);
        }

        GL11.glEnd();

        GL11.glLineWidth(1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    private void drawRightPanel(int mouseX, int mouseY) {
        int panelX = guiLeft + GUI_WIDTH - 100;
        int panelY = guiTop + 30;
        int panelW = 95;
        int panelH = 155;

        // 面板背景
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0x80000000);

        // 标题
        this.fontRenderer.drawString("§e检测到的 Synergy", panelX + 5, panelY + 3, 0xFFFFFF);

        if (detectedSynergies.isEmpty()) {
            String hint = "拖拽模块到槽位";
            int hintX = panelX + (panelW - this.fontRenderer.getStringWidth(hint)) / 2;
            this.fontRenderer.drawString(hint, hintX, panelY + 70, 0x666666);
            return;
        }

        // Synergy 列表
        int listY = panelY + 45;
        for (int i = 0; i < Math.min(detectedSynergies.size(), 4); i++) {
            SynergyDefinition synergy = detectedSynergies.get(i);
            int y = listY + i * 25;

            boolean isActivated = playerData.isSynergyActivated(synergy.getId());

            // 名称
            String name = synergy.getDisplayName();
            if (name.length() > 9) {
                name = name.substring(0, 8) + "..";
            }

            int nameColor = isActivated ? 0x88FF88 : 0xFFFFFF;
            this.fontRenderer.drawString(name, panelX + 5, y + 5, nameColor);

            // 激活按钮由 buttonList 处理
        }
    }

    private void drawDraggingModule(int mouseX, int mouseY) {
        if (draggingModule == null) return;

        ModuleEntry module = findModule(draggingModule);
        if (module == null) return;

        // 半透明背景
        int x = mouseX - 15;
        int y = mouseY - 10;
        drawRect(x, y, x + 30, y + 20, 0xA0202020);

        // 模块名称缩写
        String abbr = getAbbreviation(module.displayName);
        int textX = x + 15 - this.fontRenderer.getStringWidth(abbr) / 2;
        this.fontRenderer.drawStringWithShadow(abbr, textX, y + 6, module.color.getColorIndex());
    }

    private void drawTooltips(int mouseX, int mouseY) {
        // 左侧模块列表提示
        int leftPanelX = guiLeft + 5;
        int listY = guiTop + 45;

        if (mouseX >= leftPanelX + 5 && mouseX <= leftPanelX + 85) {
            int relY = mouseY - listY;
            if (relY >= 0 && relY < 136) {
                int idx = moduleListScroll + relY / 17;
                if (idx >= 0 && idx < ownedModules.size()) {
                    ModuleEntry module = ownedModules.get(idx);
                    List<String> tooltip = new ArrayList<>();
                    tooltip.add(module.color + module.displayName);
                    tooltip.add(TextFormatting.GRAY + "拖拽到槽位链结");

                    boolean inSlot = Arrays.asList(linkSlots).contains(module.id);
                    if (inSlot) {
                        tooltip.add(TextFormatting.YELLOW + "已在槽位中");
                    }

                    this.drawHoveringText(tooltip, mouseX, mouseY);
                    return;
                }
            }
        }

        // 槽位提示
        for (int i = 0; i < LINK_SLOTS; i++) {
            SlotPosition pos = slotPositions[i];
            int x = guiLeft + pos.x - 12;
            int y = guiTop + pos.y - 12;

            if (mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24) {
                String moduleId = linkSlots[i];
                List<String> tooltip = new ArrayList<>();

                if (moduleId != null) {
                    ModuleEntry module = findModule(moduleId);
                    if (module != null) {
                        tooltip.add(module.color + module.displayName);
                        tooltip.add(TextFormatting.GRAY + "右键移除");
                    }
                } else {
                    tooltip.add(TextFormatting.YELLOW + "链结槽位 #" + (i + 1));
                    tooltip.add(TextFormatting.GRAY + "拖拽模块到此处");
                }

                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }

        // 右侧 Synergy 列表提示
        int rightPanelX = guiLeft + GUI_WIDTH - 100;
        int synergyListY = guiTop + 75;

        for (int i = 0; i < Math.min(detectedSynergies.size(), 4); i++) {
            int y = synergyListY + i * 25;

            if (mouseX >= rightPanelX + 5 && mouseX <= rightPanelX + 90 &&
                    mouseY >= y && mouseY <= y + 20) {

                SynergyDefinition synergy = detectedSynergies.get(i);
                List<String> tooltip = new ArrayList<>();

                tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + synergy.getDisplayName());

                String desc = synergy.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    String[] parts = desc.split("\\|");
                    for (String part : parts) {
                        tooltip.add(TextFormatting.GRAY + part.trim());
                    }
                }

                boolean isActivated = playerData.isSynergyActivated(synergy.getId());
                if (isActivated) {
                    tooltip.add("");
                    tooltip.add(TextFormatting.GREEN + "✓ 已激活");
                } else {
                    tooltip.add("");
                    tooltip.add(TextFormatting.YELLOW + "点击激活按钮启用");
                }

                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 左键开始拖拽
        if (mouseButton == 0) {
            handleLeftClick(mouseX, mouseY);
        }

        // 右键移除槽位
        if (mouseButton == 1) {
            handleRightClick(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if (draggingModule != null) {
            handleDrop(mouseX, mouseY);
            draggingModule = null;
        }
    }

    private void handleLeftClick(int mouseX, int mouseY) {
        // 检查是否点击模块列表
        int leftPanelX = guiLeft + 5;
        int listY = guiTop + 45;

        if (mouseX >= leftPanelX + 5 && mouseX <= leftPanelX + 85) {
            int relY = mouseY - listY;
            if (relY >= 0 && relY < 136) {
                int idx = moduleListScroll + relY / 17;
                if (idx >= 0 && idx < ownedModules.size()) {
                    ModuleEntry module = ownedModules.get(idx);

                    // 检查是否已在槽位中
                    if (!Arrays.asList(linkSlots).contains(module.id)) {
                        draggingModule = module.id;
                    }
                }
            }
        }
    }

    private void handleRightClick(int mouseX, int mouseY) {
        // 检查是否右键槽位
        for (int i = 0; i < LINK_SLOTS; i++) {
            SlotPosition pos = slotPositions[i];
            int x = guiLeft + pos.x - 12;
            int y = guiTop + pos.y - 12;

            if (mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24) {
                if (linkSlots[i] != null) {
                    linkSlots[i] = null;
                    detectSynergies();
                    updateActivateButtons();
                    mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5F, 0.8F);
                }
                break;
            }
        }
    }

    private void handleDrop(int mouseX, int mouseY) {
        // 检查是否放到槽位上
        for (int i = 0; i < LINK_SLOTS; i++) {
            SlotPosition pos = slotPositions[i];
            int x = guiLeft + pos.x - 12;
            int y = guiTop + pos.y - 12;

            if (mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24) {
                if (linkSlots[i] == null) {
                    linkSlots[i] = draggingModule;
                    detectSynergies();
                    updateActivateButtons();
                    mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_NOTE_CHIME, 0.7F, 1.2F);
                } else {
                    // 槽位已满
                    mc.player.playSound(net.minecraft.init.SoundEvents.BLOCK_NOTE_BASS, 0.5F, 0.6F);
                }
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 滚轮滚动模块列表
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && ownedModules.size() > 8) {
            int dir = wheel > 0 ? -1 : 1;
            int maxScroll = Math.max(0, ownedModules.size() - 8);
            moduleListScroll = Math.max(0, Math.min(moduleListScroll + dir, maxScroll));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_CLOSE) {
            this.mc.displayGuiScreen(null);
        } else if (button.id == BUTTON_CLEAR) {
            Arrays.fill(linkSlots, null);
            detectSynergies();
            updateActivateButtons();
            mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.7F, 0.8F);
        } else if (button.id >= BUTTON_ACTIVATE_BASE) {
            int idx = button.id - BUTTON_ACTIVATE_BASE;
            if (idx >= 0 && idx < detectedSynergies.size()) {
                SynergyDefinition synergy = detectedSynergies.get(idx);
                activateSynergy(synergy.getId());
            }
        }
    }

    private void activateSynergy(String synergyId) {
        // 发送网络包到服务端
        com.moremod.network.PacketHandler.INSTANCE.sendToServer(
                new com.moremod.synergy.network.PacketToggleSynergy(synergyId)
        );

        mc.player.playSound(net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
        updateActivateButtons();
    }

    private ModuleEntry findModule(String id) {
        for (ModuleEntry module : ownedModules) {
            if (module.id.equals(id)) {
                return module;
            }
        }
        return null;
    }

    private String getAbbreviation(String name) {
        if (name.length() <= 3) return name;

        // 提取首字母缩写（中文取前2字，英文取前3字母）
        if (name.matches(".*[\\u4e00-\\u9fa5].*")) {
            return name.substring(0, Math.min(2, name.length()));
        } else {
            return name.substring(0, Math.min(3, name.length())).toUpperCase();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ===== 内部类 =====

    private static class ModuleEntry {
        final String id;
        final String displayName;
        final TextFormatting color;

        ModuleEntry(String id, String displayName, TextFormatting color) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
        }
    }

    private static class SlotPosition {
        final int x, y;

        SlotPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
