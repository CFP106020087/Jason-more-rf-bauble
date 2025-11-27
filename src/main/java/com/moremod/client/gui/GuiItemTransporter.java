package com.moremod.client.gui;

import com.moremod.tile.TileEntityItemTransporter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * 物品传输器GUI - 完全手绘，无外部材质
 */
public class GuiItemTransporter extends GuiContainer {

    private final TileEntityItemTransporter tile;
    private final EntityPlayer player;

    // ===== 布局常量（相对 GUI 顶部 y 的偏移）=====
    // 玩家物品栏整体下移，给过滤按钮第二排留出空间
    private static final int INV_BG_TOP = 160;       // 原 138 -> 160（+22）
    private static final int INV_BG_HEIGHT = 78;     // 保持不变
    private static final int INV_GRID_TOP = INV_BG_TOP + 1;   // 3×9 格开始：+1
    private static final int HOTBAR_TOP  = INV_BG_TOP + 59;   // 热键栏开始：+59（与原逻辑一致）

    // 过滤按钮两排位置（位于过滤格子区域下方、玩家物品栏上方）
    private static final int BTN_ROW1_TOP = 126;     // 原来就是 ~125 附近
    private static final int BTN_ROW2_TOP = BTN_ROW1_TOP + 14; // 第二排高度 12，再加 2px 间隔 = 14

    // 文本输入框
    private GuiTextField pullStartField;
    private GuiTextField pullEndField;
    private GuiTextField pushStartField;
    private GuiTextField pushEndField;

    // 按钮ID
    private static final int BTN_PULL_SIDE = 0;
    private static final int BTN_PUSH_SIDE = 1;
    private static final int BTN_WHITELIST = 2;
    private static final int BTN_META = 3;
    private static final int BTN_NBT = 4;
    private static final int BTN_MOD = 5;
    private static final int BTN_OREDICT = 6;
    private static final int BTN_REDSTONE = 7;
    private static final int BTN_CLEAR = 8;

    public GuiItemTransporter(EntityPlayer player, TileEntityItemTransporter tile) {
        super(new ContainerItemTransporter(player, tile));
        this.tile = tile;
        this.player = player;
        this.xSize = 176;
        // 原 222 -> 244（为下移后的玩家物品栏预留空间）
        this.ySize = 244;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        Keyboard.enableRepeatEvents(true);

        // ===== 方向按钮 =====
        this.buttonList.add(new GuiButton(BTN_PULL_SIDE, x + 100, y + 10, 70, 20, "拉取: ?"));
        this.buttonList.add(new GuiButton(BTN_PUSH_SIDE, x + 100, y + 45, 70, 20, "推送: ?"));

        // ===== 槽位输入框 =====
        pullStartField = new GuiTextField(0, this.fontRenderer, x + 25, y + 13, 25, 14);
        pullEndField   = new GuiTextField(1, this.fontRenderer, x + 53, y + 13, 25, 14);
        pushStartField = new GuiTextField(2, this.fontRenderer, x + 25, y + 48, 25, 14);
        pushEndField   = new GuiTextField(3, this.fontRenderer, x + 53, y + 48, 25, 14);

        pullStartField.setMaxStringLength(5);
        pullEndField.setMaxStringLength(5);
        pushStartField.setMaxStringLength(5);
        pushEndField.setMaxStringLength(5);

        pullStartField.setText(String.valueOf(tile.pullSlotStart));
        pullEndField.setText(tile.pullSlotEnd == Integer.MAX_VALUE ? "" : String.valueOf(tile.pullSlotEnd));
        pushStartField.setText(String.valueOf(tile.pushSlotStart));
        pushEndField.setText(tile.pushSlotEnd == Integer.MAX_VALUE ? "" : String.valueOf(tile.pushSlotEnd));

        // ===== 过滤选项按钮（两排）=====
        int btnY1 = y + BTN_ROW1_TOP;
        int btnY2 = y + BTN_ROW2_TOP;
        int btnW = 35;

        this.buttonList.add(new GuiButton(BTN_WHITELIST, x + 80,  btnY1, btnW, 12, ""));
        this.buttonList.add(new GuiButton(BTN_META,      x + 117, btnY1, btnW, 12, "Meta"));
        this.buttonList.add(new GuiButton(BTN_OREDICT,   x + 154, btnY1, btnW, 12, ""));

        this.buttonList.add(new GuiButton(BTN_NBT,       x + 80,  btnY2, btnW, 12, "NBT"));
        this.buttonList.add(new GuiButton(BTN_MOD,       x + 117, btnY2, btnW, 12, "Mod"));
        this.buttonList.add(new GuiButton(BTN_REDSTONE,  x + 154, btnY2, btnW, 12, "RS"));

        this.buttonList.add(new GuiButton(BTN_CLEAR, x + 8, btnY2, 64, 12, "清空过滤器"));

        updateButtonLabels();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        sendUpdate();
        Keyboard.enableRepeatEvents(false); // 补：关闭键盘重复
        System.out.println("[MoreMod] GUI已关闭，配置已同步");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        // ===== 背景与标题 =====
        drawRect(x, y, x + xSize, y + ySize, 0xFF8B8B8B);
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, 0xFFC6C6C6);
        drawRect(x + 1, y + 1, x + xSize - 1, y + 8, 0xFF4A4A4A);
        this.fontRenderer.drawString("物品传输器", x + 5, y + 2, 0xFFFFFF);

        // ===== 配置区域背景 =====
        drawRect(x + 6, y + 9, x + 95, y + 65, 0xFFAAAAAA);

        // ===== 内部槽位区域（单格展示）=====
        drawRect(x + 76, y + 31, x + 96, y + 51, 0xFF8B8B8B);
        drawSlot(x + 79, y + 34);

        // ===== 过滤槽位区域 =====
        drawRect(x + 6, y + 68, x + 80, y + 124, 0xFFAAAAAA);
        this.fontRenderer.drawString("过滤器", x + 8, y + 71, 0x404040);

        // 12 个过滤格（3×4）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                drawSlot(x + 7 + col * 18, y + 69 + row * 18);
            }
        }

        // ===== 玩家物品栏背景（整体下移）=====
        int invBgTop = y + INV_BG_TOP;
        drawRect(x + 6, invBgTop, x + 170, invBgTop + INV_BG_HEIGHT, 0xFFAAAAAA);

        // 玩家物品栏格子（3 行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + 7 + col * 18, y + INV_GRID_TOP + row * 18);
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            drawSlot(x + 7 + col * 18, y + HOTBAR_TOP);
        }

        // ===== 标签与输入框 =====
        this.fontRenderer.drawString("抽取:", x + 8, y + 15, 0x404040);
        this.fontRenderer.drawString("-", x + 61, y + 15, 0x404040);
        this.fontRenderer.drawString("送出:", x + 8, y + 50, 0x404040);
        this.fontRenderer.drawString("-", x + 61, y + 50, 0x404040);

        pullStartField.drawTextBox();
        pullEndField.drawTextBox();
        pushStartField.drawTextBox();
        pushEndField.drawTextBox();

        // ===== 状态小图标（与第一排按钮对齐）=====
        int iconX = x + 88;
        int iconY = y + BTN_ROW1_TOP + 1;
        drawRect(iconX, iconY, iconX + 8, iconY + 8, tile.isWhitelist ? 0xFF00FF00 : 0xFFFF0000);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        int iconLeft = x + 88, iconTop = y + BTN_ROW1_TOP + 1;

        if (mouseX >= iconLeft && mouseX <= iconLeft + 8 && mouseY >= iconTop && mouseY <= iconTop + 8) {
            this.drawHoveringText(tile.isWhitelist ? "白名单模式" : "黑名单模式",
                    mouseX - x, mouseY - y);
        }
    }

    private void drawSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_PULL_SIDE:  tile.cyclePullSide(); break;
            case BTN_PUSH_SIDE:  tile.cyclePushSide(); break;
            case BTN_WHITELIST:  tile.toggleWhitelist(); break;
            case BTN_META:       tile.toggleRespectMeta(); break;
            case BTN_NBT:        tile.toggleRespectNBT(); break;
            case BTN_MOD:        tile.toggleRespectMod(); break;
            case BTN_OREDICT:    tile.cycleRespectOredict(); break;
            case BTN_REDSTONE:   tile.redstoneControlled = !tile.redstoneControlled; break;
            case BTN_CLEAR:      tile.clearFilter(); break;
        }
        sendUpdate();
        updateButtonLabels();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (pullStartField.textboxKeyTyped(typedChar, keyCode) ||
                pullEndField.textboxKeyTyped(typedChar, keyCode) ||
                pushStartField.textboxKeyTyped(typedChar, keyCode) ||
                pushEndField.textboxKeyTyped(typedChar, keyCode)) {
            updateSlotsFromFields();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        pullStartField.mouseClicked(mouseX, mouseY, mouseButton);
        pullEndField.mouseClicked(mouseX, mouseY, mouseButton);
        pushStartField.mouseClicked(mouseX, mouseY, mouseButton);
        pushEndField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        pullStartField.updateCursorCounter();
        pullEndField.updateCursorCounter();
        pushStartField.updateCursorCounter();
        pushEndField.updateCursorCounter();
    }

    private void updateSlotsFromFields() {
        try {
            int pullStart = Integer.parseInt(pullStartField.getText());
            int pullEnd = pullEndField.getText().equals("末") ? Integer.MAX_VALUE : Integer.parseInt(pullEndField.getText());
            int pushStart = Integer.parseInt(pushStartField.getText());
            int pushEnd = pushEndField.getText().equals("末") ? Integer.MAX_VALUE : Integer.parseInt(pushEndField.getText());

            tile.setPullSlotRange(pullStart, pullEnd);
            tile.setPushSlotRange(pushStart, pushEnd);
            sendUpdate();
        } catch (NumberFormatException ignored) {}
    }

    private void updateButtonLabels() {
        for (GuiButton button : this.buttonList) {
            switch (button.id) {
                case BTN_PULL_SIDE:
                    button.displayString = "拉取: " + getFacingName(tile.pullSide); break;
                case BTN_PUSH_SIDE:
                    button.displayString = "推送: " + getFacingName(tile.pushSide); break;
                case BTN_WHITELIST:
                    button.displayString = tile.isWhitelist ? "白名" : "黑名"; break;
                case BTN_META:
                    button.displayString = (tile.respectMeta ? "§a" : "§7") + "Meta"; break;
                case BTN_NBT:
                    button.displayString = (tile.respectNBT ? "§a" : "§7") + "NBT"; break;
                case BTN_MOD:
                    button.displayString = (tile.respectMod ? "§a" : "§7") + "Mod"; break;
                case BTN_OREDICT:
                    button.displayString = tile.respectOredict == 0 ? "§7矿辞"
                            : (tile.respectOredict == 1 ? "§e矿辞1" : "§a矿辞2"); break;
                case BTN_REDSTONE:
                    button.displayString = (tile.redstoneControlled ? "§c" : "§7") + "RS"; break;
            }
        }
    }

    private String getFacingName(EnumFacing facing) {
        switch (facing) {
            case DOWN: return "下";
            case UP: return "上";
            case NORTH: return "北";
            case SOUTH: return "南";
            case WEST: return "西";
            case EAST: return "东";
            default: return "?";
        }
    }

    /** 发送更新到服务端 */
    private void sendUpdate() {
        tile.markDirty();
        com.moremod.network.PacketHandler.INSTANCE.sendToServer(
                new com.moremod.network.PacketTransporterConfig(tile)
        );
        System.out.println("[MoreMod] 客户端已发送配置更新包");
    }
}
