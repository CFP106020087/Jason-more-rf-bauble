package com.moremod.client.gui;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.config.BrokenGodConfig;
import com.moremod.config.ShambhalaConfig;
import com.moremod.event.EnergyPunishmentSystem;
import com.moremod.item.ItemBatteryBauble;
import com.moremod.item.ItemCreativeBatteryBauble;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.network.NetworkHandler;
import com.moremod.network.PacketMechanicalCoreUpdate;
import com.moremod.system.ascension.BrokenGodHandler;
import com.moremod.system.ascension.ShambhalaHandler;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import com.moremod.upgrades.energy.EnergyDepletionManager;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

/**
 * 机械核心控制面板 GUI - 完整修复版
 * ✅ 修复：itemMaxLevel 设为 final，防止被覆盖
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreGui extends GuiScreen {

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 180;

    // 升格侧边栏尺寸
    private static final int SIDE_PANEL_WIDTH = 85;
    private static final int SIDE_PANEL_HEIGHT = 90;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("moremod", "textures/gui/mechanical_core_gui.png");

    // 按钮ID
    private static final int BUTTON_CLOSE = 0;
    private static final int BUTTON_PAUSE_ALL = 1;
    private static final int BUTTON_RESUME_ALL = 2;
    private static final int BUTTON_ASCEND = 3;
    private static final int BUTTON_ASCEND_SHAMBHALA = 4;

    private static final int UPGRADES_PER_PAGE = 6;

    private final EntityPlayer player;
    private final Map<String, UpgradeEntry> upgradeEntries = new HashMap<>();
    private final List<String> availableUpgrades = new ArrayList<>();
    private final Set<String> processedUpgrades = new HashSet<>();

    private int scrollOffset = 0;
    private long lastUpdateTime = 0;
    private final Map<String, Long> pendingUpdates = new HashMap<>();

    private int guiLeft;
    private int guiTop;

    private static final Set<String> WATERPROOF_IDS = new HashSet<>(Arrays.asList(
            "WATERPROOF_MODULE","WATERPROOF","waterproof_module","waterproof"
    ));
    private static final Set<String> GENERATOR_MODULES = new HashSet<>(Arrays.asList(
            "SOLAR_GENERATOR", "KINETIC_GENERATOR", "THERMAL_GENERATOR", "VOID_ENERGY", "COMBAT_CHARGER",
            "solar_generator", "kinetic_generator", "thermal_generator", "void_energy", "combat_charger"
    ));

    // ===== 工具方法 =====

    private static String up(String s){ return s==null? "" : s.toUpperCase(); }
    private static String lo(String s){ return s==null? "" : s.toLowerCase(); }

    private static boolean isWaterproofUpgrade(String id) {
        if (id == null) return false;
        String u = up(id);
        return WATERPROOF_IDS.contains(u) || u.contains("WATERPROOF");
    }

    private static boolean isGeneratorModule(String id) {
        if (id == null) return false;
        return GENERATOR_MODULES.contains(id) || GENERATOR_MODULES.contains(up(id));
    }

    /**
     * ✅ 新增：从 NBT 读取 OriginalMax，尝试所有变体
     */
    private int readOriginalMaxFromNBT(NBTTagCompound nbt, String id) {
        if (nbt == null) return 0;

        // 读取所有变体的最大值
        int originalMax = Math.max(
                nbt.getInteger("OriginalMax_" + id),
                Math.max(
                        nbt.getInteger("OriginalMax_" + up(id)),
                        nbt.getInteger("OriginalMax_" + lo(id))
                )
        );

        // 防水模块特殊处理
        if (originalMax <= 0 && isWaterproofUpgrade(id)) {
            for (String wid : WATERPROOF_IDS) {
                originalMax = Math.max(originalMax,
                        Math.max(
                                nbt.getInteger("OriginalMax_" + wid),
                                Math.max(
                                        nbt.getInteger("OriginalMax_" + up(wid)),
                                        nbt.getInteger("OriginalMax_" + lo(wid))
                                )
                        )
                );
            }
        }

        return originalMax;
    }

    /**
     * ✅ 新增：获取模块的默认最大等级
     */
    private int getDefaultMaxLevel(String id) {
        // 基础升级类型
        try {
            ItemMechanicalCore.UpgradeType type = ItemMechanicalCore.UpgradeType.valueOf(up(id));
            return getMaxLevel(type);
        } catch (Exception ignored) {}

        // 扩展升级类型
        try {
            ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
            if (info != null && info.maxLevel > 0) {
                return info.maxLevel;
            }
        } catch (Exception ignored) {}

        // 默认值
        return 3;
    }// ===== 升级状态枚举 =====

    public enum UpgradeStatus {
        ACTIVE,
        PAUSED,
        DEGRADED,
        DAMAGED,
        PENALIZED,
        NOT_OWNED
    }

    // ===== ✅ 修复：UpgradeEntry 类 =====

    public static class UpgradeEntry {
        public final String id;
        public final String displayName;
        public final TextFormatting color;
        public final int maxLevel;
        public final ItemMechanicalCoreExtended.UpgradeCategory category;

        // ✅ 关键修改：itemMaxLevel 改为 final（初始化后不可修改）
        public final int itemMaxLevel;

        public int currentLevel;
        public int ownedMaxLevel;
        public int damageCount;
        public boolean isPaused;
        public boolean canRunWithEnergy;
        public UpgradeStatus status;
        public boolean wasPunished;

        // ✅ 修改构造函数，添加 itemMaxLevel 参数
        public UpgradeEntry(String id, String displayName, TextFormatting color, int maxLevel,
                            ItemMechanicalCoreExtended.UpgradeCategory category,
                            int currentLevel, int ownedMaxLevel,
                            boolean canRunWithEnergy,
                            int itemMaxLevel) {  // ← 新增参数
            this.id = id;
            this.displayName = displayName;
            this.color = color;
            this.maxLevel = maxLevel;
            this.category = category;
            this.currentLevel = currentLevel;
            this.ownedMaxLevel = ownedMaxLevel;
            this.canRunWithEnergy = canRunWithEnergy;
            this.itemMaxLevel = itemMaxLevel;  // ← 初始化后不可修改
            this.damageCount = 0;
            this.isPaused = (currentLevel == 0 && ownedMaxLevel > 0);
            this.status = UpgradeStatus.ACTIVE;
            this.wasPunished = false;
        }

        public boolean canRepair() {
            return status == UpgradeStatus.DAMAGED && wasPunished;
        }
    }

    // ===== 构造函数 =====

    public MechanicalCoreGui(EntityPlayer player) {
        this.player = player;
        initializeUpgradeData();
    }

    private ItemStack getCurrentCoreStack() {
        return ItemMechanicalCore.findEquippedMechanicalCore(player);
    }

    // ===== ✅ 修复：状态判断逻辑 =====

    private UpgradeStatus getUpgradeStatus(NBTTagCompound nbt, String id) {
        if (nbt == null) return UpgradeStatus.NOT_OWNED;

        int currentLevel = getUpgradeLevelAcross(getCurrentCoreStack(), id);
        int ownedMax = getOwnedMaxFromNBT(nbt, id);

        // ✅ 修复：使用正确的方法读取 itemMax
        int itemMax = readOriginalMaxFromNBT(nbt, id);

        // 兜底：如果读取失败，使用 ownedMax
        if (itemMax <= 0) {
            itemMax = ownedMax > 0 ? ownedMax : getDefaultMaxLevel(id);
        }

        ItemStack core = getCurrentCoreStack();
        if (!core.isEmpty() && ItemMechanicalCore.isPenalized(core, id)) {
            return UpgradeStatus.PENALIZED;
        }

        // ✅ 优先检查是否被惩罚过（DAMAGED 优先级最高）
        boolean wasPunished = nbt.getBoolean("WasPunished_" + id) ||
                nbt.getBoolean("WasPunished_" + up(id)) ||
                nbt.getBoolean("WasPunished_" + lo(id));

        if (wasPunished && ownedMax < itemMax) {
            return UpgradeStatus.DAMAGED;
        }

        // 然后检查暂停状态
        boolean isPaused = nbt.getBoolean("IsPaused_" + id) ||
                nbt.getBoolean("IsPaused_" + up(id)) ||
                nbt.getBoolean("IsPaused_" + lo(id));

        if (isPaused && currentLevel == 0) {
            return UpgradeStatus.PAUSED;
        }

        if (ownedMax > 0 && currentLevel < ownedMax) {
            return UpgradeStatus.DEGRADED;
        }

        if (currentLevel > 0) {
            return UpgradeStatus.ACTIVE;
        }

        if (ownedMax > 0) {
            return UpgradeStatus.PAUSED;
        }

        return UpgradeStatus.NOT_OWNED;
    }

    private int getUpgradeLevelAcross(ItemStack core, String id) {
        if (core == null || core.isEmpty()) return 0;
        NBTTagCompound nbt = core.getTagCompound();
        int nbtLevel = 0;
        boolean nbtHasKey = false;

        if (nbt != null) {
            if (nbt.getBoolean("IsPaused_" + id) ||
                    nbt.getBoolean("IsPaused_" + up(id)) ||
                    nbt.getBoolean("IsPaused_" + lo(id))) {
                return 0;
            }

            if (nbt.hasKey("upgrade_" + id) ||
                    nbt.hasKey("upgrade_" + up(id)) ||
                    nbt.hasKey("upgrade_" + lo(id))) {
                nbtHasKey = true;
                nbtLevel = Math.max(nbt.getInteger("upgrade_" + id),
                        Math.max(nbt.getInteger("upgrade_" + up(id)),
                                nbt.getInteger("upgrade_" + lo(id))));
            }
        }

        if (nbtHasKey) return nbtLevel;

        int lv = 0;
        try {
            lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id));
        } catch (Throwable ignored) {}

        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(up(id));
            lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, t));
        } catch (Throwable ignored) {}

        return lv;
    }// ===== ✅ 修复：初始化升级数据 =====

    private void initializeUpgradeData() {
        upgradeEntries.clear();
        availableUpgrades.clear();
        processedUpgrades.clear();

        ItemStack coreStack = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) return;

        NBTTagCompound nbt = coreStack.hasTagCompound() ? coreStack.getTagCompound() : new NBTTagCompound();

        // ===== 加载基础升级 =====
        for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
            String id = type.getKey();

            if (isWaterproofUpgrade(id)) {
                if (processedUpgrades.contains("WATERPROOF_MODULE")) continue;
                id = "WATERPROOF_MODULE";
                processedUpgrades.addAll(WATERPROOF_IDS);
            }

            if (processedUpgrades.contains(up(id))) continue;

            UpgradeStatus status = getUpgradeStatus(nbt, id);

            if (status == UpgradeStatus.NOT_OWNED) {
                int level = getUpgradeLevelAcross(coreStack, id);
                int ownedMaxLevel = getOwnedMaxFromNBT(nbt, id);
                int lastLevel = getLastLevelFromNBT(nbt, id);
                boolean hasMark = nbt.getBoolean("HasUpgrade_" + id) ||
                        nbt.getBoolean("HasUpgrade_" + up(id));

                boolean hasUpgrade = ownedMaxLevel > 0 || hasMark || level > 0 || lastLevel > 0;
                if (!hasUpgrade) continue;
            }

            int level = getUpgradeLevelAcross(coreStack, id);
            int ownedMaxLevel = getOwnedMaxFromNBT(nbt, id);

            if (ownedMaxLevel <= 0 && level > 0) {
                ownedMaxLevel = level;
                nbt.setInteger("OwnedMax_" + id, ownedMaxLevel);
                coreStack.setTagCompound(nbt);
            }

            // ✅ 关键修复：在创建 Entry 前先读取 itemMaxLevel
// ✅ 关键修复：在创建 Entry 前先读取 itemMaxLevel
            int itemMaxLevel = readOriginalMaxFromNBT(nbt, id);

            if (itemMaxLevel > 0) {
                System.out.println("[GUI-Init] ✓ 读取 OriginalMax: " + id + " = " + itemMaxLevel);
            } else {
                // ✅ 修复：优先使用配置默认值，而不是 ownedMaxLevel
                itemMaxLevel = getMaxLevel(type);  // ← 先用配置值
                System.out.println("[GUI-Init] 使用配置默认值: " + id + " = " + itemMaxLevel);

                // ✅ 如果配置值也无效，才用 ownedMaxLevel（最后的兜底）
                if (itemMaxLevel <= 0 && ownedMaxLevel > 0) {
                    itemMaxLevel = ownedMaxLevel;
                    System.out.println("[GUI-Init] 最终兜底使用 OwnedMax: " + id + " = " + itemMaxLevel);
                }

                // ✅ 立即写入 OriginalMax
                if (itemMaxLevel > 0) {
                    nbt.setInteger("OriginalMax_" + id, itemMaxLevel);
                    nbt.setInteger("OriginalMax_" + up(id), itemMaxLevel);
                    nbt.setInteger("OriginalMax_" + lo(id), itemMaxLevel);
                    coreStack.setTagCompound(nbt);
                    System.out.println("[GUI-Init] 补救记录 OriginalMax: " + id + " = " + itemMaxLevel);
                }
            }

// ✅ 传入正确的 itemMaxLevel
            UpgradeEntry entry = new UpgradeEntry(id,
                    type.getDisplayName(),
                    type.getColor(),
                    getMaxLevel(type),
                    ItemMechanicalCoreExtended.UpgradeCategory.BASIC,
                    level, ownedMaxLevel,
                    checkCanRunWithEnergy(id),
                    itemMaxLevel);  // ← 现在是正确的值了

            entry.damageCount = EnergyPunishmentSystem.getDamageCount(coreStack, id);
            entry.wasPunished = nbt.getBoolean("WasPunished_" + id) ||
                    nbt.getBoolean("WasPunished_" + up(id));

            entry.status = status;
            entry.isPaused = (status == UpgradeStatus.PAUSED);

            upgradeEntries.put(id, entry);
            availableUpgrades.add(id);
            processedUpgrades.add(up(id));
        }

        // ===== 加载扩展升级 =====
        try {
            Map<String, ItemMechanicalCoreExtended.UpgradeInfo> all = ItemMechanicalCoreExtended.getAllUpgrades();
            for (Map.Entry<String, ItemMechanicalCoreExtended.UpgradeInfo> en : all.entrySet()) {
                String id = en.getKey();

                if (processedUpgrades.contains(up(id))) continue;

                if (isWaterproofUpgrade(id)) {
                    processedUpgrades.addAll(WATERPROOF_IDS);
                }

                ItemMechanicalCoreExtended.UpgradeInfo info = en.getValue();
                if (info.category == ItemMechanicalCoreExtended.UpgradeCategory.BASIC) continue;

                UpgradeStatus status = getUpgradeStatus(nbt, id);

                if (status == UpgradeStatus.NOT_OWNED) {
                    int level = getUpgradeLevelAcross(coreStack, id);
                    int ownedMaxLevel = getOwnedMaxFromNBT(nbt, id);
                    int lastLevel = getLastLevelFromNBT(nbt, id);
                    boolean hasMark = nbt.getBoolean("HasUpgrade_" + id);

                    boolean hasUpgrade = ownedMaxLevel > 0 || hasMark || level > 0 || lastLevel > 0;
                    if (!hasUpgrade) continue;
                }

                int level = getUpgradeLevelAcross(coreStack, id);
                int ownedMaxLevel = getOwnedMaxFromNBT(nbt, id);

                if (ownedMaxLevel <= 0 && level > 0) {
                    ownedMaxLevel = level;
                    nbt.setInteger("OwnedMax_" + id, ownedMaxLevel);
                    coreStack.setTagCompound(nbt);
                }

                // ✅ 关键修复：在创建 Entry 前先读取 itemMaxLevel
// ✅ 关键修复：在创建 Entry 前先读取 itemMaxLevel
                int itemMaxLevel = readOriginalMaxFromNBT(nbt, id);

                if (itemMaxLevel > 0) {
                    System.out.println("[GUI-Init] ✓ 读取 OriginalMax: " + id + " = " + itemMaxLevel);
                } else {
                    // ✅ 修复：优先使用配置默认值
                    itemMaxLevel = info.maxLevel;  // ← 先用配置值
                    System.out.println("[GUI-Init] 使用配置默认值: " + id + " = " + itemMaxLevel);

                    // ✅ 如果配置值也无效，才用 ownedMaxLevel
                    if (itemMaxLevel <= 0 && ownedMaxLevel > 0) {
                        itemMaxLevel = ownedMaxLevel;
                        System.out.println("[GUI-Init] 最终兜底使用 OwnedMax: " + id + " = " + itemMaxLevel);
                    }

                    // ✅ 立即写入
                    if (itemMaxLevel > 0) {
                        nbt.setInteger("OriginalMax_" + id, itemMaxLevel);
                        nbt.setInteger("OriginalMax_" + up(id), itemMaxLevel);
                        nbt.setInteger("OriginalMax_" + lo(id), itemMaxLevel);
                        coreStack.setTagCompound(nbt);
                        System.out.println("[GUI-Init] 补救记录 OriginalMax: " + id + " = " + itemMaxLevel);
                    }
                }

// ✅ 传入正确的 itemMaxLevel
                UpgradeEntry entry = new UpgradeEntry(id,
                        info.displayName,
                        info.color,
                        info.maxLevel,
                        info.category,
                        level, ownedMaxLevel,
                        checkCanRunWithEnergy(id),
                        itemMaxLevel);  // ← 现在是正确的值了

                entry.damageCount = EnergyPunishmentSystem.getDamageCount(coreStack, id);
                entry.wasPunished = nbt.getBoolean("WasPunished_" + id) ||
                        nbt.getBoolean("WasPunished_" + up(id));

                entry.status = status;
                entry.isPaused = (status == UpgradeStatus.PAUSED);

                upgradeEntries.put(id, entry);
                availableUpgrades.add(id);
                processedUpgrades.add(up(id));
            }
        } catch (Throwable ignored) {}

        // 排序
        availableUpgrades.sort((a,b)->{
            UpgradeEntry A = upgradeEntries.get(a), B = upgradeEntries.get(b);
            if (A == null || B == null) return 0;
            int c = A.category.compareTo(B.category);
            return c != 0 ? c : A.displayName.compareTo(B.displayName);
        });
    }

    private int getOwnedMaxFromNBT(NBTTagCompound nbt, String id) {
        if (nbt == null) return 0;
        return Math.max(nbt.getInteger("OwnedMax_" + id),
                Math.max(nbt.getInteger("OwnedMax_" + up(id)),
                        nbt.getInteger("OwnedMax_" + lo(id))));
    }

    private int getLastLevelFromNBT(NBTTagCompound nbt, String id) {
        if (nbt == null) return 0;
        return Math.max(nbt.getInteger("LastLevel_" + id),
                Math.max(nbt.getInteger("LastLevel_" + up(id)),
                        nbt.getInteger("LastLevel_" + lo(id))));
    }// ===== ✅ 修复：更新升级状态（不再修改 itemMaxLevel） =====

    private void updateUpgradeStates() {
        long now = System.currentTimeMillis();
        ItemStack coreStack = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) return;

        NBTTagCompound nbt = coreStack.hasTagCompound() ? coreStack.getTagCompound() : new NBTTagCompound();

        for (String id : availableUpgrades) {
            UpgradeEntry e = upgradeEntries.get(id);
            if (e == null) continue;

            if (pendingUpdates.containsKey(id)) {
                if (now - pendingUpdates.get(id) < 1800) continue;
                pendingUpdates.remove(id);
            }

            try {
                e.status = getUpgradeStatus(nbt, id);
                e.canRunWithEnergy = checkCanRunWithEnergy(id);
                e.currentLevel = getUpgradeLevelAcross(coreStack, id);

                int ownedMax = getOwnedMaxFromNBT(nbt, id);
                if (ownedMax > 0) {
                    e.ownedMaxLevel = ownedMax;
                } else if (e.currentLevel > 0 && e.ownedMaxLevel <= 0) {
                    e.ownedMaxLevel = e.currentLevel;
                    nbt.setInteger("OwnedMax_" + id, e.currentLevel);
                    coreStack.setTagCompound(nbt);
                }

                // ✅ 关键修复：完全不修改 e.itemMaxLevel
                // itemMaxLevel 是 final 的，无法修改，只在初始化时设置

                e.damageCount = EnergyPunishmentSystem.getDamageCount(coreStack, id);
                e.wasPunished = nbt.getBoolean("WasPunished_" + id) ||
                        nbt.getBoolean("WasPunished_" + up(id));
                e.isPaused = (e.status == UpgradeStatus.PAUSED);
            } catch (Throwable ignored) {}
        }
    }

    // ===== GUI 初始化 =====

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.buttonList.clear();

        this.buttonList.add(new GuiButton(BUTTON_CLOSE, guiLeft + GUI_WIDTH - 25, guiTop + 5, 20, 20, "×"));
        this.buttonList.add(new GuiButton(BUTTON_PAUSE_ALL, guiLeft + 10, guiTop + 42, 100, 14, "⏸ 暂停非发电模块"));
        this.buttonList.add(new GuiButton(BUTTON_RESUME_ALL, guiLeft + 115, guiTop + 42, 100, 14, "▶ 恢复全部模块"));

        // 添加破碎之神升格按钮（在侧边栏）
        int sidePanelX = guiLeft + GUI_WIDTH + 5;
        int sidePanelY = guiTop + 20;
        GuiButton ascendButton = new GuiButton(BUTTON_ASCEND, sidePanelX + 5, sidePanelY + SIDE_PANEL_HEIGHT - 25, SIDE_PANEL_WIDTH - 10, 18, "");
        ascendButton.visible = false; // 默认隐藏，在drawScreen中控制
        this.buttonList.add(ascendButton);

        // 添加香巴拉升格按钮（在侧边栏下方）
        GuiButton shambhalaButton = new GuiButton(BUTTON_ASCEND_SHAMBHALA, sidePanelX + 5, sidePanelY + SIDE_PANEL_HEIGHT * 2 - 15, SIDE_PANEL_WIDTH - 10, 18, "");
        shambhalaButton.visible = false;
        this.buttonList.add(shambhalaButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > 500) {
            updateUpgradeStates();
            lastUpdateTime = now;
        }

        this.drawDefaultBackground();
        drawGuiBackground();
        drawTitle();
        drawEnergyStatus();
        drawUpgradeList(mouseX, mouseY);
        drawScrollBar();
        drawAscensionSection(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawGuiBackground() {
        GlStateManager.color(1,1,1,1);
        try { this.mc.getTextureManager().bindTexture(GUI_TEXTURE); } catch (Exception ignored) {}
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xC0383838);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + 20, 0xC0505050);
    }

    private void drawTitle() {
        String t = "机械核心控制面板";
        int x = guiLeft + (GUI_WIDTH - this.fontRenderer.getStringWidth(t)) / 2;
        this.fontRenderer.drawStringWithShadow(t, x, guiTop + 8, 0xFFFFFF);
    }

    private void drawEnergyStatus() {
        ItemStack core = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(core)) {
            this.fontRenderer.drawString("未找到机械核心", guiLeft + 10, guiTop + 25, 0xFF0000);
            return;
        }
        IEnergyStorage es = ItemMechanicalCore.getEnergyStorage(core);
        if (es == null) {
            this.fontRenderer.drawString("能量系统错误", guiLeft + 10, guiTop + 25, 0xFF0000);
            return;
        }

        EnergyDepletionManager.EnergyStatus st = EnergyDepletionManager.getCurrentEnergyStatus(core);
        int sx = guiLeft + 10, sy = guiTop + 25;

        this.fontRenderer.drawStringWithShadow(st.icon + " " + st.displayName, sx, sy, st.color.getColorIndex());

        float p = (float) es.getEnergyStored() / Math.max(1, es.getMaxEnergyStored());
        String eText = String.format("%.1f%% (%s / %s FE)", p*100, fmt(es.getEnergyStored()), fmt(es.getMaxEnergyStored()));
        this.fontRenderer.drawString(eText, sx + 80, sy, 0xCCCCCC);
    }

    private void drawUpgradeList(int mouseX, int mouseY) {
        int listX = guiLeft + 10, listY = guiTop + 60, listW = GUI_WIDTH - 40, listH = 105;
        drawRect(listX, listY, listX + listW, listY + listH, 0x80000000);

        if (availableUpgrades.isEmpty()) {
            String s = "未安装任何升级";
            int x = listX + (listW - this.fontRenderer.getStringWidth(s)) / 2;
            int y = listY + listH / 2;
            this.fontRenderer.drawString(s, x, y, 0x888888);
            return;
        }

        int visible = Math.min(UPGRADES_PER_PAGE, availableUpgrades.size() - scrollOffset);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            if (idx >= availableUpgrades.size()) break;
            String id = availableUpgrades.get(idx);
            UpgradeEntry e = upgradeEntries.get(id);
            if (e == null) continue;

            int y = listY + 5 + i * 17;
            drawUpgradeEntry(e, listX + 5, y, listW - 10, mouseX, mouseY);
        }
    }

    private void drawUpgradeEntry(UpgradeEntry entry, int x, int y, int w, int mouseX, int mouseY) {
        int bg = 0x40000000;
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 15) bg = 0x60000000;

        switch (entry.status) {
            case DAMAGED:  bg = 0x60800040; break;
            case DEGRADED: bg = 0x60404000; break;
            case PAUSED:   bg = 0x60606000; break;
            case PENALIZED: bg = 0x60400040; break;
            default:
                if (!entry.canRunWithEnergy && entry.currentLevel > 0) bg = 0x60800000;
                break;
        }

        drawRect(x, y, x + w, y + 15, bg);
        drawRect(x, y, x + 2, y + 15, entry.category.color.getColorIndex() | 0xFF000000);

        String name;
        int nameCol;

        if (entry.status == UpgradeStatus.DAMAGED) {
            name = entry.displayName + " Lv." + entry.currentLevel + "/" + entry.ownedMaxLevel + "/" + entry.itemMaxLevel;
            if (entry.damageCount > 0) {
                name += " [损坏×" + entry.damageCount + "]";
            }
            nameCol = 0xFF88AA;
        } else if (entry.status == UpgradeStatus.DEGRADED) {
            name = entry.displayName + " Lv." + entry.currentLevel + "/" + entry.ownedMaxLevel + " [降级]";
            nameCol = 0xFFCC00;
        } else if (entry.status == UpgradeStatus.PAUSED) {
            name = entry.displayName + " [暂停]";
            nameCol = 0xFFFF00;
        } else if (entry.status == UpgradeStatus.PENALIZED) {
            int left = ItemMechanicalCore.getPenaltySecondsLeft(getCurrentCoreStack(), entry.id);
            int cap  = Math.max(1, ItemMechanicalCore.getPenaltyCap(getCurrentCoreStack(), entry.id));
            name = entry.displayName + " Lv." + entry.currentLevel + "/" + entry.ownedMaxLevel + " [惩罚: ≤" + cap + " | " + left + "s]";
            nameCol = 0xFFAA88;
        } else if (entry.currentLevel == 0) {
            name = entry.displayName + " [未激活]";
            nameCol = 0x666666;
        } else {
            name = entry.displayName + " Lv." + entry.currentLevel + "/" + entry.ownedMaxLevel;
            nameCol = entry.canRunWithEnergy ? 0xFFFFFF : 0x888888;
        }

        this.fontRenderer.drawString(name, x + 5, y + 3, nameCol);

        String statusIcon;
        int iconColor;

        if (entry.status == UpgradeStatus.DAMAGED) {
            statusIcon = "⚒";
            iconColor = 0xFFFF88FF;
        } else if (entry.status == UpgradeStatus.DEGRADED) {
            statusIcon = "↓";
            iconColor = 0xFFFFCC00;
        } else if (entry.status == UpgradeStatus.PAUSED) {
            statusIcon = "||";
            iconColor = 0xFFFFFF00;
        } else if (entry.status == UpgradeStatus.PENALIZED) {
            statusIcon = "🔒";
            iconColor = 0xFFAA88FF;
        } else if (entry.currentLevel == 0) {
            statusIcon = "---";
            iconColor = 0xFF666666;
        } else if (!entry.canRunWithEnergy) {
            statusIcon = "LOW";
            iconColor = 0xFFAAAA00;
        } else {
            statusIcon = "ON";
            iconColor = 0xFF88FF88;
        }

        this.fontRenderer.drawString(statusIcon, x + w - 50, y + 3, iconColor);

        int btnY = y + 1, sz = 13;

        // 减号按钮
        int minusX = x + w - 30;
        boolean canDecrease = entry.currentLevel > 0 && !isGeneratorModule(entry.id);
        drawRect(minusX, btnY, minusX + sz, btnY + sz,
                inBtn(mouseX, mouseY, minusX, btnY, sz) && canDecrease ?
                        0x80FF4444 : (canDecrease ? 0x80444444 : 0x40222222));
        this.fontRenderer.drawString("-", minusX + 5, btnY + 2, canDecrease ? 0xFFFFFF : 0x666666);

        // 加号按钮
        int plusX = x + w - 15;
        boolean canIncrease = false;

        if (entry.status == UpgradeStatus.PAUSED && entry.ownedMaxLevel > 0) {
            canIncrease = true;
        } else if (entry.status == UpgradeStatus.DAMAGED && entry.wasPunished) {
            canIncrease = entry.ownedMaxLevel < entry.itemMaxLevel;
        } else if (entry.currentLevel < entry.ownedMaxLevel) {
            canIncrease = true;
        }

        drawRect(plusX, btnY, plusX + sz, btnY + sz,
                inBtn(mouseX, mouseY, plusX, btnY, sz) && canIncrease ?
                        0x8044FF44 : (canIncrease ? 0x80444444 : 0x40222222));
        this.fontRenderer.drawString("+", plusX + 4, btnY + 2, canIncrease ? 0xFFFFFF : 0x666666);
    }

    private void drawScrollBar() {
        if (availableUpgrades.size() <= UPGRADES_PER_PAGE) return;
        int x = guiLeft + GUI_WIDTH - 25, y = guiTop + 60, h = 105;
        drawRect(x, y, x + 10, y + h, 0x80000000);
        float ratio = (float) scrollOffset / Math.max(1, availableUpgrades.size() - UPGRADES_PER_PAGE);
        int sliderH = Math.max(10, h * UPGRADES_PER_PAGE / availableUpgrades.size());
        int sy = y + (int)((h - sliderH) * ratio);
        drawRect(x + 1, sy, x + 9, sy + sliderH, 0xFFAAAAAA);
    }

    // ===== 升格区域（悬停显示侧边栏） =====
    // 支持两条升格路线：破碎之神（低人性）和香巴拉（高人性）

    private void drawAscensionSection(int mouseX, int mouseY) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) {
            hideAllAscensionButtons();
            return;
        }

        // 已经升格的情况
        if (data.getAscensionRoute() != AscensionRoute.NONE) {
            hideAllAscensionButtons();
            return;
        }

        // 获取通用数据
        float humanity = data.getHumanity();
        ItemStack core = getCurrentCoreStack();
        int installedCount = ItemMechanicalCore.getTotalInstalledUpgrades(core);

        // ========== 破碎之神条件 ==========
        long lowHumanityTicks = data.getLowHumanityTicks();
        long lowHumanitySeconds = lowHumanityTicks / 20;
        boolean brokenHumanityMet = humanity <= BrokenGodConfig.ascensionHumanityThreshold;
        boolean brokenTimeMet = lowHumanitySeconds >= BrokenGodConfig.requiredLowHumanitySeconds;
        boolean brokenModulesMet = installedCount >= BrokenGodConfig.requiredModuleCount;
        boolean canAscendBroken = brokenHumanityMet && brokenTimeMet && brokenModulesMet;

        // ========== 机巧香巴拉条件 ==========
        long highHumanityTicks = data.getHighHumanityTicks();
        long requiredHighTicks = ShambhalaConfig.requiredHighHumanitySeconds * 20L;
        boolean shambhalaHumanityMet = humanity >= ShambhalaConfig.ascensionHumanityThreshold;
        boolean shambhalaTimeMet = highHumanityTicks >= requiredHighTicks;
        boolean shambhalaModulesMet = installedCount >= ShambhalaConfig.requiredModuleCount;
        boolean canAscendShambhala = shambhalaHumanityMet && shambhalaTimeMet && shambhalaModulesMet;

        // 触发区位置（主GUI右侧的小标签）
        int triggerX = guiLeft + GUI_WIDTH;
        int triggerY1 = guiTop + 20;  // 破碎之神
        int triggerY2 = guiTop + 75;  // 香巴拉
        int triggerW = 18;
        int triggerH = 50;

        // 侧边栏位置
        int panelX = triggerX + triggerW;

        // 检测鼠标位置
        boolean hoverBrokenTrigger = mouseX >= triggerX && mouseX <= triggerX + triggerW &&
                mouseY >= triggerY1 && mouseY <= triggerY1 + triggerH;
        boolean hoverShambhalaTrigger = mouseX >= triggerX && mouseX <= triggerX + triggerW &&
                mouseY >= triggerY2 && mouseY <= triggerY2 + triggerH;
        boolean hoverBrokenPanel = mouseX >= panelX && mouseX <= panelX + SIDE_PANEL_WIDTH &&
                mouseY >= triggerY1 && mouseY <= triggerY1 + SIDE_PANEL_HEIGHT;
        boolean hoverShambhalaPanel = mouseX >= panelX && mouseX <= panelX + SIDE_PANEL_WIDTH &&
                mouseY >= triggerY2 && mouseY <= triggerY2 + SIDE_PANEL_HEIGHT;

        boolean showBrokenPanel = hoverBrokenTrigger || hoverBrokenPanel;
        boolean showShambhalaPanel = hoverShambhalaTrigger || hoverShambhalaPanel;

        // ========== 绘制破碎之神触发标签 ==========
        int brokenColor = canAscendBroken ? 0xC0442266 : 0xC0333333;
        int brokenHoverColor = canAscendBroken ? 0xC0663388 : 0xC0444444;
        drawRect(triggerX, triggerY1, triggerX + triggerW, triggerY1 + triggerH,
                showBrokenPanel ? brokenHoverColor : brokenColor);
        String brokenLabel = canAscendBroken ? "✦" : "◇";
        int brokenLabelColor = canAscendBroken ? 0xFFAA88FF : 0xFFAAAAAA;
        this.fontRenderer.drawStringWithShadow(brokenLabel, triggerX + 5, triggerY1 + 20, brokenLabelColor);

        // ========== 绘制香巴拉触发标签 ==========
        int shambhalaColor = canAscendShambhala ? 0xC0226644 : 0xC0333333;
        int shambhalaHoverColor = canAscendShambhala ? 0xC0338866 : 0xC0444444;
        drawRect(triggerX, triggerY2, triggerX + triggerW, triggerY2 + triggerH,
                showShambhalaPanel ? shambhalaHoverColor : shambhalaColor);
        String shambhalaLabel = canAscendShambhala ? "☀" : "○";
        int shambhalaLabelColor = canAscendShambhala ? 0xFFFFDD88 : 0xFFAAAAAA;
        this.fontRenderer.drawStringWithShadow(shambhalaLabel, triggerX + 5, triggerY2 + 20, shambhalaLabelColor);

        // ========== 破碎之神侧边栏 ==========
        if (showBrokenPanel) {
            drawBrokenGodPanel(panelX, triggerY1, humanity, lowHumanitySeconds, installedCount,
                    brokenHumanityMet, brokenTimeMet, brokenModulesMet, canAscendBroken);
            hideShambhalaButton();
        } else {
            hideBrokenGodButton();
        }

        // ========== 香巴拉侧边栏 ==========
        if (showShambhalaPanel) {
            drawShambhalaPanel(panelX, triggerY2, humanity, highHumanityTicks, requiredHighTicks, installedCount,
                    shambhalaHumanityMet, shambhalaTimeMet, shambhalaModulesMet, canAscendShambhala);
            hideBrokenGodButton();
        } else {
            hideShambhalaButton();
        }

        // 两个都没悬停时隐藏所有按钮
        if (!showBrokenPanel && !showShambhalaPanel) {
            hideAllAscensionButtons();
        }
    }

    private void drawBrokenGodPanel(int panelX, int panelY, float humanity, long lowHumanitySeconds,
                                     int installedCount, boolean humanityMet, boolean timeMet, boolean modulesMet, boolean canAscend) {
        // 绘制侧边栏背景
        drawRect(panelX, panelY, panelX + SIDE_PANEL_WIDTH, panelY + SIDE_PANEL_HEIGHT, 0xC0101010);
        drawRect(panelX + 1, panelY + 1, panelX + SIDE_PANEL_WIDTH - 1, panelY + SIDE_PANEL_HEIGHT - 1, 0xC0383838);

        // 标题栏
        drawRect(panelX + 1, panelY + 1, panelX + SIDE_PANEL_WIDTH - 1, panelY + 14, 0xC0442266);
        String title = "破碎之神";
        int titleX = panelX + (SIDE_PANEL_WIDTH - this.fontRenderer.getStringWidth(title)) / 2;
        this.fontRenderer.drawStringWithShadow(title, titleX, panelY + 4, 0xAA88FF);

        // 更新升格按钮状态和位置
        for (GuiButton button : buttonList) {
            if (button.id == BUTTON_ASCEND) {
                button.x = panelX + 5;
                button.y = panelY + SIDE_PANEL_HEIGHT - 22;
                button.width = SIDE_PANEL_WIDTH - 10;
                button.visible = true;
                button.enabled = canAscend;
                button.displayString = canAscend ? TextFormatting.DARK_PURPLE + "✦ 升格 ✦" : TextFormatting.GRAY + "未满足";
                break;
            }
        }

        // 绘制条件列表
        int lineY = panelY + 18;
        int lineX = panelX + 4;

        // 人性值条件
        if (humanityMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "人性≤" + (int)BrokenGodConfig.ascensionHumanityThreshold + "%", lineX, lineY, 0xFFFFFF);
        } else {
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + String.format("%.0f", humanity) + "/" + (int)BrokenGodConfig.ascensionHumanityThreshold + "%", lineX, lineY, 0xFFFFFF);
        }

        // 低人性时间条件
        lineY += 12;
        if (timeMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "时间OK", lineX, lineY, 0xFFFFFF);
        } else {
            String timeStr = formatTimeCompact((int)lowHumanitySeconds) + "/" + formatTimeCompact(BrokenGodConfig.requiredLowHumanitySeconds);
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + timeStr, lineX, lineY, 0xFFFFFF);
        }

        // 模块数量条件
        lineY += 12;
        if (modulesMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "模块≥" + BrokenGodConfig.requiredModuleCount, lineX, lineY, 0xFFFFFF);
        } else {
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + installedCount + "/" + BrokenGodConfig.requiredModuleCount + "模块", lineX, lineY, 0xFFFFFF);
        }
    }

    private void drawShambhalaPanel(int panelX, int panelY, float humanity, long highHumanityTicks,
                                     long requiredTicks, int installedCount, boolean humanityMet, boolean timeMet, boolean modulesMet, boolean canAscend) {
        // 绘制侧边栏背景
        drawRect(panelX, panelY, panelX + SIDE_PANEL_WIDTH, panelY + SIDE_PANEL_HEIGHT, 0xC0101010);
        drawRect(panelX + 1, panelY + 1, panelX + SIDE_PANEL_WIDTH - 1, panelY + SIDE_PANEL_HEIGHT - 1, 0xC0383838);

        // 标题栏
        drawRect(panelX + 1, panelY + 1, panelX + SIDE_PANEL_WIDTH - 1, panelY + 14, 0xC0226644);
        String title = "机巧香巴拉";
        int titleX = panelX + (SIDE_PANEL_WIDTH - this.fontRenderer.getStringWidth(title)) / 2;
        this.fontRenderer.drawStringWithShadow(title, titleX, panelY + 4, 0xFFDD88);

        // 更新升格按钮状态和位置
        for (GuiButton button : buttonList) {
            if (button.id == BUTTON_ASCEND_SHAMBHALA) {
                button.x = panelX + 5;
                button.y = panelY + SIDE_PANEL_HEIGHT - 22;
                button.width = SIDE_PANEL_WIDTH - 10;
                button.visible = true;
                button.enabled = canAscend;
                button.displayString = canAscend ? TextFormatting.GOLD + "☀ 升格 ☀" : TextFormatting.GRAY + "未满足";
                break;
            }
        }

        // 绘制条件列表
        int lineY = panelY + 18;
        int lineX = panelX + 4;

        // 人性值条件（高人性）
        if (humanityMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "人性≥" + (int)ShambhalaConfig.ascensionHumanityThreshold + "%", lineX, lineY, 0xFFFFFF);
        } else {
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + String.format("%.0f", humanity) + "/" + (int)ShambhalaConfig.ascensionHumanityThreshold + "%", lineX, lineY, 0xFFFFFF);
        }

        // 高人性时间条件（秒）
        lineY += 12;
        long secondsProgress = highHumanityTicks / 20;
        if (timeMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "时间OK", lineX, lineY, 0xFFFFFF);
        } else {
            String timeStr = formatTimeCompact((int) secondsProgress) + "/" + formatTimeCompact(ShambhalaConfig.requiredHighHumanitySeconds);
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + timeStr, lineX, lineY, 0xFFFFFF);
        }

        // 模块数量条件
        lineY += 12;
        if (modulesMet) {
            this.fontRenderer.drawString(TextFormatting.GREEN + "✓" + TextFormatting.GRAY + "模块≥" + ShambhalaConfig.requiredModuleCount, lineX, lineY, 0xFFFFFF);
        } else {
            this.fontRenderer.drawString(TextFormatting.RED + "✗" + TextFormatting.GRAY + installedCount + "/" + ShambhalaConfig.requiredModuleCount + "模块", lineX, lineY, 0xFFFFFF);
        }
    }

    /**
     * 紧凑版时间格式化
     */
    private String formatTimeCompact(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else if (totalSeconds < 3600) {
            return (totalSeconds / 60) + "m";
        } else {
            return (totalSeconds / 3600) + "h";
        }
    }

    private void hideAllAscensionButtons() {
        for (GuiButton button : buttonList) {
            if (button.id == BUTTON_ASCEND || button.id == BUTTON_ASCEND_SHAMBHALA) {
                button.visible = false;
            }
        }
    }

    private void hideBrokenGodButton() {
        for (GuiButton button : buttonList) {
            if (button.id == BUTTON_ASCEND) {
                button.visible = false;
                break;
            }
        }
    }

    private void hideShambhalaButton() {
        for (GuiButton button : buttonList) {
            if (button.id == BUTTON_ASCEND_SHAMBHALA) {
                button.visible = false;
                break;
            }
        }
    }

    /**
     * 格式化秒数为可读时间格式 (例如: "30:00" 或 "1:30:00")
     */
    private String formatTime(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else if (totalSeconds < 3600) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    // ===== Tooltip 绘制 =====

    private void drawTooltips(int mouseX, int mouseY) {
        // 批量按钮提示
        for (GuiButton button : buttonList) {
            if (button.isMouseOver() && button.visible) {
                List<String> tooltip = new ArrayList<>();

                if (button.id == BUTTON_PAUSE_ALL) {
                    tooltip.add(TextFormatting.YELLOW + "暂停所有非发电模块");
                    tooltip.add(TextFormatting.GRAY + "保留发电模块运行");
                    tooltip.add(TextFormatting.GRAY + "用于紧急省电");
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                    return;
                } else if (button.id == BUTTON_RESUME_ALL) {
                    tooltip.add(TextFormatting.GREEN + "恢复所有模块");
                    tooltip.add(TextFormatting.GRAY + "将暂停的模块恢复到之前等级");
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                    return;
                }
            }
        }

        // 模块列表提示
        int listX = guiLeft + 10, listY = guiTop + 60, listW = GUI_WIDTH - 40, listH = 105;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) return;

        int relY = mouseY - listY - 5;
        if (relY < 0) return;

        int idx = scrollOffset + relY / 17;
        if (idx < 0 || idx >= availableUpgrades.size()) return;

        UpgradeEntry e = upgradeEntries.get(availableUpgrades.get(idx));
        if (e == null) return;

        List<String> tip = new ArrayList<>();
        tip.add(e.color + e.displayName);
        tip.add(TextFormatting.GRAY + "类别: " + e.category.color + e.category.name);

        if (e.ownedMaxLevel > 0) {
            if (e.canRepair()) {
                tip.add(TextFormatting.GRAY + "当前/拥有/原始: " +
                        TextFormatting.WHITE + e.currentLevel + "/" +
                        TextFormatting.YELLOW + e.ownedMaxLevel + "/" +
                        TextFormatting.GREEN + e.itemMaxLevel);
            } else {
                tip.add(TextFormatting.GRAY + "当前等级: " + TextFormatting.WHITE + e.currentLevel + "/" + e.ownedMaxLevel);
            }
        }

        switch (e.status) {
            case DAMAGED:
                tip.add(TextFormatting.LIGHT_PURPLE + "⚒ 损坏 - 原始等级 Lv." + e.itemMaxLevel);

                if (e.ownedMaxLevel < e.itemMaxLevel) {
                    int repairsNeeded = e.itemMaxLevel - e.ownedMaxLevel;
                    tip.add(TextFormatting.YELLOW + "需要修复 " + repairsNeeded + " 次");
                    tip.add(TextFormatting.GRAY + "每次修复恢复 1 级");

                    int nextLevel = e.ownedMaxLevel + 1;
                    int xpCost = calculateRepairCost(e, nextLevel);

                    tip.add("");
                    tip.add(TextFormatting.YELLOW + "下次修复成本: " +
                            TextFormatting.LIGHT_PURPLE + xpCost + " 经验级");

                    if (player.experienceLevel >= xpCost) {
                        tip.add(TextFormatting.GREEN + "当前经验: " + player.experienceLevel + " 级 ✓");
                    } else {
                        tip.add(TextFormatting.RED + "当前经验: " + player.experienceLevel + " 级 (还需 " +
                                (xpCost - player.experienceLevel) + " 级)");
                    }

                    if (e.damageCount > 0) {
                        tip.add(TextFormatting.DARK_RED + "累计损坏: " + e.damageCount + " 次");
                    }

                    if (e.currentLevel > 0) {
                        tip.add("");
                        tip.add(TextFormatting.GRAY + "提示: 可以先降到 Lv.0 再修复");
                    }
                } else {
                    tip.add(TextFormatting.GREEN + "✓ 已完全修复");
                }
                break;

            case DEGRADED:
                tip.add(TextFormatting.YELLOW + "↓ 被降级 - 可以升回 Lv." + e.ownedMaxLevel);
                break;

            case PAUSED:
                tip.add(TextFormatting.YELLOW + "⏸ 暂停中 - 点击 + 恢复运行");
                break;

            case PENALIZED:
                int left = ItemMechanicalCore.getPenaltySecondsLeft(getCurrentCoreStack(), e.id);
                int cap  = Math.max(1, ItemMechanicalCore.getPenaltyCap(getCurrentCoreStack(), e.id));
                tip.add(TextFormatting.LIGHT_PURPLE + "🔒 惩罚中：临时上限 Lv." + cap + "，剩余 " + left + " 秒");
                NBTTagCompound nbt = getCurrentCoreStack().getTagCompound();
                if (nbt != null) {
                    int debtFE = nbt.getInteger("PenaltyDebtFE_" + e.id);
                    int debtXP = nbt.getInteger("PenaltyDebtXP_" + e.id);
                    if (debtFE > 0 || debtXP > 0) {
                        tip.add(TextFormatting.YELLOW + "可付费提前解除：" +
                                (debtFE > 0 ? (TextFormatting.AQUA + String.valueOf(debtFE) + " FE ") : "") +
                                (debtXP > 0 ? (TextFormatting.LIGHT_PURPLE + String.valueOf(debtXP) + " XP") : ""));
                    }
                }
                break;

            case ACTIVE:
                if (!e.canRunWithEnergy) tip.add(TextFormatting.YELLOW + "⚡ 能量不足");
                else tip.add(TextFormatting.GREEN + "✓ 运行中");

                if (e.currentLevel >= e.ownedMaxLevel && e.ownedMaxLevel < e.maxLevel) {
                    tip.add(TextFormatting.GRAY + "需要升级道具提升到 Lv." + (e.ownedMaxLevel + 1));
                }
                break;

            default:
                tip.add(TextFormatting.GRAY + "未激活");
                break;
        }

        if (isGeneratorModule(e.id)) {
            tip.add("");
            tip.add(TextFormatting.AQUA + "⚡ 发电模块 - 不会被批量暂停");
        }

        this.drawHoveringText(tip, mouseX, mouseY);
    }

    // ===== 鼠标点击处理 =====

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        handleUpgradeClick(mouseX, mouseY, mouseButton);
        handleScrollBarClick(mouseX, mouseY, mouseButton);
    }

    private void handleUpgradeClick(int mouseX, int mouseY, int mouseButton) {
        int listX = guiLeft + 10, listY = guiTop + 60, listW = GUI_WIDTH - 40;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + 105) return;

        int relY = mouseY - listY - 5;
        if (relY < 0) return;

        int idx = scrollOffset + relY / 17;
        if (idx < 0 || idx >= availableUpgrades.size()) return;

        String upgradeId = availableUpgrades.get(idx);
        UpgradeEntry e = upgradeEntries.get(upgradeId);
        if (e == null) return;

        int entryX = listX + 5;
        int entryY = listY + 5 + (idx - scrollOffset) * 17;
        if (mouseY < entryY + 1 || mouseY > entryY + 14) return;

        int sz = 13;
        int minusX = entryX + listW - 40;
        int plusX  = entryX + listW - 25;

        if (inBtn(mouseX, mouseY, minusX, entryY + 1, sz)) {
            if (e.currentLevel > 0 && !isGeneratorModule(e.id)) {
                adjustUpgradeLevel(upgradeId, -1);
            }
            return;
        }

        if (inBtn(mouseX, mouseY, plusX, entryY + 1, sz)) {
            if (e.status == UpgradeStatus.PAUSED && e.ownedMaxLevel > 0) {
                adjustUpgradeLevel(upgradeId, +1);
            } else if (e.status == UpgradeStatus.DAMAGED && e.wasPunished && e.ownedMaxLevel < e.itemMaxLevel) {
                tryRepair(e);
            } else if (e.currentLevel < e.ownedMaxLevel) {
                adjustUpgradeLevel(upgradeId, +1);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_CLOSE) {
            this.mc.displayGuiScreen(null);
        } else if (button.id == BUTTON_PAUSE_ALL) {
            pauseAllNonGeneratorModules();
        } else if (button.id == BUTTON_RESUME_ALL) {
            resumeAllModules();
        } else if (button.id == BUTTON_ASCEND) {
            tryAscendToBrokenGod();
        } else if (button.id == BUTTON_ASCEND_SHAMBHALA) {
            tryAscendToShambhala();
        }
    }

    /**
     * 尝试升格为破碎之神
     */
    private void tryAscendToBrokenGod() {
        if (!BrokenGodHandler.canAscend(player)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "升格条件未满足"
            ), true);
            return;
        }

        // 发送升格请求到服务器
        try {
            NetworkHandler.INSTANCE.sendToServer(new PacketMechanicalCoreUpdate(
                    PacketMechanicalCoreUpdate.Action.BROKEN_GOD_ASCEND,
                    "ASCEND",
                    0,
                    true
            ));

            // 播放音效 (服务器会播放升格音效)
            this.mc.player.playSound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.8f);

            // 关闭GUI
            this.mc.displayGuiScreen(null);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "✦ 升格仪式开始... ✦"
            ), true);

        } catch (Throwable e) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "升格请求发送失败"
            ), true);
        }
    }

    /**
     * 尝试升格为香巴拉
     */
    private void tryAscendToShambhala() {
        if (!ShambhalaHandler.canAscend(player)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "升格条件未满足"
            ), true);
            return;
        }

        // 发送升格请求到服务器
        try {
            NetworkHandler.INSTANCE.sendToServer(new PacketMechanicalCoreUpdate(
                    PacketMechanicalCoreUpdate.Action.SHAMBHALA_ASCEND,
                    "ASCEND",
                    0,
                    true
            ));

            // 播放音效 (服务器会播放升格音效)
            this.mc.player.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 1.0f, 1.2f);

            // 关闭GUI
            this.mc.displayGuiScreen(null);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "☀ 升格仪式开始... ☀"
            ), true);

        } catch (Throwable e) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "升格请求发送失败"
            ), true);
        }
    }

    private void pauseAllNonGeneratorModules() {
        ItemStack core = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        if (!core.hasTagCompound()) core.setTagCompound(nbt);

        int pausedCount = 0;
        List<String> pausedNames = new ArrayList<>();

        for (String id : availableUpgrades) {
            UpgradeEntry entry = upgradeEntries.get(id);
            if (entry == null) continue;

            if (isGeneratorModule(id)) continue;
            if (entry.currentLevel == 0) continue;

            int oldLevel = entry.currentLevel;
            entry.currentLevel = 0;
            entry.status = UpgradeStatus.PAUSED;
            entry.isPaused = true;

            setLevelEverywhere(core, id, 0);
            writePauseMeta(core, id, oldLevel, true);
            sendSetLevel(id, 0);

            pausedCount++;
            pausedNames.add(entry.displayName);
            pendingUpdates.put(id, System.currentTimeMillis());
        }

        if (pausedCount > 0) {
            this.mc.player.playSound(SoundEvents.BLOCK_NOTE_BASS, 0.7F, 0.8F);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⏸ 已暂停 " + pausedCount + " 个非发电模块"
            ), true);

            if (pausedCount <= 5) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "暂停: " + String.join(", ", pausedNames)
                ));
            }
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "没有可暂停的模块"
            ), true);
        }

        updateUpgradeStates();
    }

    private void resumeAllModules() {
        ItemStack core = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        if (!core.hasTagCompound()) core.setTagCompound(nbt);

        int resumedCount = 0;
        List<String> resumedNames = new ArrayList<>();

        for (String id : availableUpgrades) {
            UpgradeEntry entry = upgradeEntries.get(id);
            if (entry == null) continue;

            if (entry.status != UpgradeStatus.PAUSED) continue;
            if (entry.currentLevel > 0) continue;

            int lastLevel = getLastLevelFromNBT(nbt, id);
            if (lastLevel <= 0 && entry.ownedMaxLevel > 0) {
                lastLevel = entry.ownedMaxLevel;
            }
            if (lastLevel <= 0) continue;

            lastLevel = Math.min(lastLevel, entry.ownedMaxLevel);

            setLevelEverywhere(core, id, lastLevel);
            writePauseMeta(core, id, lastLevel, false);

            entry.currentLevel = lastLevel;
            entry.isPaused = false;
            entry.status = UpgradeStatus.ACTIVE;

            sendSetLevel(id, lastLevel);

            resumedCount++;
            resumedNames.add(entry.displayName + " Lv." + lastLevel);
            pendingUpdates.put(id, System.currentTimeMillis());
        }

        if (resumedCount > 0) {
            this.mc.player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 0.7F, 1.2F);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "▶ 已恢复 " + resumedCount + " 个模块"
            ), true);

            if (resumedCount <= 5) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "恢复: " + String.join(", ", resumedNames)
                ));
            }
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "没有需要恢复的模块"
            ), true);
        }

        updateUpgradeStates();
    }

    /**
     * ✅ 修复：使用 REPAIR_UPGRADE 动作，所有修改在服务器端
     */
    private void tryRepair(UpgradeEntry entry) {
        ItemStack core = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        if (entry.ownedMaxLevel >= entry.itemMaxLevel) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 模块已完全修复"
            ), true);
            return;
        }

        int targetLevel = Math.min(entry.ownedMaxLevel + 1, entry.itemMaxLevel);
        int xpCost = calculateRepairCost(entry, targetLevel);

        // 客户端只验证经验是否足够（提前反馈）
        if (!player.capabilities.isCreativeMode && player.experienceLevel < xpCost) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "经验不足！需要 " + xpCost + " 级"
            ), true);
            return;
        }

        // 发送修复请求到服务器
        try {
            NetworkHandler.INSTANCE.sendToServer(new PacketMechanicalCoreUpdate(
                    PacketMechanicalCoreUpdate.Action.REPAIR_UPGRADE,
                    entry.id,
                    xpCost,
                    true
            ));
            System.out.println("[GUI-Repair] 发送修复请求: " + entry.id + ", 成本: " + xpCost);
        } catch (Throwable e) {
            System.err.println("[GUI-Repair] 发送修复请求失败: " + e.getMessage());
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "修复请求发送失败"
            ), true);
            return;
        }

        // 播放音效（乐观更新）
        player.playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.0f);

        // 显示等待提示
        if (targetLevel >= entry.itemMaxLevel) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "⚒ 正在修复..." + entry.displayName
            ), true);
        } else {
            int repairsLeft = entry.itemMaxLevel - targetLevel;
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚒ 正在修复..." + entry.displayName +
                            TextFormatting.GRAY + " (还需 " + repairsLeft + " 次)"
            ), true);
        }

        // 标记为等待更新
        pendingUpdates.put(entry.id, System.currentTimeMillis());
    }

    /**
     * 修复成本计算（使用 TotalDamageCount）
     */
    private int calculateRepairCost(UpgradeEntry entry, int targetLevel) {
        ItemStack core = getCurrentCoreStack();
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 1;

        // 使用 TotalDamageCount（累计总损坏次数）
        int totalDamageCount = Math.max(
                nbt.getInteger("TotalDamageCount_" + entry.id),
                Math.max(
                        nbt.getInteger("TotalDamageCount_" + up(entry.id)),
                        nbt.getInteger("TotalDamageCount_" + lo(entry.id))
                )
        );

        // 如果没有 TotalDamageCount，使用当前的 DamageCount
        if (totalDamageCount <= 0) {
            totalDamageCount = entry.damageCount;
        }

        // 至少为1
        if (totalDamageCount <= 0) {
            totalDamageCount = 1;
        }

        // 成本公式：7.5 * (总损坏次数)^0.42
        double cost = 7.5 * Math.pow(totalDamageCount, 0.42);
        int totalCost = (int) Math.ceil(cost);

        // 限制在1-30级之间
        return Math.max(1, Math.min(30, totalCost));
    }

    private int calculateRepairCost(UpgradeEntry entry) {
        return calculateRepairCost(entry, entry.ownedMaxLevel + 1);
    }

    private void handleScrollBarClick(int mouseX, int mouseY, int mouseButton) {
        if (availableUpgrades.size() <= UPGRADES_PER_PAGE) return;
        int x = guiLeft + GUI_WIDTH - 25, y = guiTop + 60, h = 105;
        if (mouseX >= x && mouseX <= x + 10 && mouseY >= y && mouseY <= y + h) {
            float r = (float) (mouseY - y) / h;
            int maxOffset = Math.max(0, availableUpgrades.size() - UPGRADES_PER_PAGE);
            scrollOffset = Math.max(0, Math.min((int)(r * maxOffset), maxOffset));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0 && availableUpgrades.size() > UPGRADES_PER_PAGE) {
            int dir = w > 0 ? -1 : 1;
            int maxOffset = Math.max(0, availableUpgrades.size() - UPGRADES_PER_PAGE);
            scrollOffset = Math.max(0, Math.min(scrollOffset + dir, maxOffset));
        }
    }/**
     * ✅ 修复：降级逻辑 - 损坏模块降到0时保持DAMAGED状态
     */
    private void adjustUpgradeLevel(String upgradeId, int delta) {
        UpgradeEntry entry = upgradeEntries.get(upgradeId);
        if (entry == null) return;

        ItemStack core = getCurrentCoreStack();
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        if (!core.hasTagCompound()) core.setTagCompound(nbt);

        int ownedMax = entry.ownedMaxLevel > 0 ? entry.ownedMaxLevel : getOwnedMaxFromNBT(nbt, upgradeId);

        if (delta > 0 && ItemMechanicalCore.isPenalized(core, upgradeId)) {
            int cap = Math.max(1, ItemMechanicalCore.getPenaltyCap(core, upgradeId));
            int left = ItemMechanicalCore.getPenaltySecondsLeft(core, upgradeId);

            if (entry.currentLevel >= cap) {
                int debtFE = nbt.getInteger("PenaltyDebtFE_" + upgradeId);
                int debtXP = nbt.getInteger("PenaltyDebtXP_" + upgradeId);

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "🔒 惩罚中：当前最高 Lv." + cap +
                                TextFormatting.GRAY + "（剩余 " + left + " 秒）"
                ), true);

                if (debtFE > 0 || debtXP > 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "可支付修复费用解除：" +
                                    (debtFE > 0 ? (TextFormatting.AQUA + String.valueOf(debtFE) + " FE ") : "") +
                                    (debtXP > 0 ? (TextFormatting.LIGHT_PURPLE + String.valueOf(debtXP) + " XP") : "")
                    ), true);

                    if (ItemMechanicalCore.tryPayPenaltyDebt(player, core, upgradeId)) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.GREEN + "✓ 惩罚解除，可继续升级"
                        ), true);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        if (entry.currentLevel == 0 && delta > 0) {
            int lastLevel = getLastLevelFromNBT(nbt, upgradeId);
            if (lastLevel <= 0 && ownedMax > 0) lastLevel = ownedMax;
            if (lastLevel <= 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "❌ 无法确定恢复等级"
                ), true);
                return;
            }

            lastLevel = Math.min(lastLevel, ownedMax);
            setLevelEverywhere(core, upgradeId, lastLevel);
            writePauseMeta(core, upgradeId, lastLevel, false);

            entry.currentLevel = lastLevel;
            entry.isPaused = false;
            entry.status = UpgradeStatus.ACTIVE;

            sendSetLevel(upgradeId, lastLevel);
            this.mc.player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 0.7F, 1.2F);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ " + entry.displayName + " 已恢复到 Lv." + lastLevel
            ), true);

            pendingUpdates.put(upgradeId, System.currentTimeMillis());
            return;
        }

        if (entry.currentLevel > 0 || (entry.currentLevel == 0 && delta < 0)) {
            int newLevel = entry.currentLevel + delta;

            if (ItemMechanicalCore.isPenalized(core, upgradeId)) {
                int cap = Math.max(1, ItemMechanicalCore.getPenaltyCap(core, upgradeId));
                if (delta > 0 && newLevel > cap) newLevel = cap;
            }

            if (newLevel < 0) newLevel = 0;
            if (delta > 0) {
                if (entry.status == UpgradeStatus.DAMAGED && entry.wasPunished) {
                    if (newLevel > ownedMax) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "模块损坏，需要修复才能升到 Lv." + (ownedMax + 1)
                        ), true);
                        newLevel = ownedMax;
                    }
                } else {
                    if (newLevel > ownedMax) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.YELLOW + entry.displayName + " 当前最高: Lv." + ownedMax +
                                        TextFormatting.GRAY + " (需要升级道具提升上限)"
                        ), true);
                        newLevel = ownedMax;
                    }
                }
            }

            if (newLevel == entry.currentLevel) return;

            int old = entry.currentLevel;
            entry.currentLevel = newLevel;

            setLevelEverywhere(core, upgradeId, newLevel);

            // ✅ 修复：损坏模块降到0时保持DAMAGED状态
            if (old > 0 && newLevel == 0) {
                if (entry.status == UpgradeStatus.DAMAGED && entry.wasPunished) {
                    // 损坏的模块：只更新等级，保持DAMAGED状态
                    entry.currentLevel = 0;
                    entry.status = UpgradeStatus.DAMAGED;
                    entry.isPaused = false;

                    // 记录 LastLevel，方便修复后恢复
                    nbt.setInteger("LastLevel_" + upgradeId, old);
                    nbt.setInteger("LastLevel_" + up(upgradeId), old);
                    nbt.setInteger("LastLevel_" + lo(upgradeId), old);

                    // 确保 OriginalMax 已记录
                    String upperId = up(upgradeId);
                    if (!nbt.hasKey("OriginalMax_" + upperId)) {
                        int originalMax = entry.itemMaxLevel > 0 ? entry.itemMaxLevel : old;
                        nbt.setInteger("OriginalMax_" + upperId, originalMax);
                        nbt.setInteger("OriginalMax_" + upgradeId, originalMax);
                    }
                } else {
                    // 正常模块：设置为暂停状态
                    writePauseMeta(core, upgradeId, old, true);
                    entry.status = UpgradeStatus.PAUSED;
                    entry.isPaused = true;
                }
            } else {
                writePauseMeta(core, upgradeId, newLevel, false);

                // 正确判断状态
                if (ItemMechanicalCore.isPenalized(core, upgradeId)) {
                    entry.status = UpgradeStatus.PENALIZED;
                } else if (entry.wasPunished && entry.ownedMaxLevel < entry.itemMaxLevel) {
                    entry.status = UpgradeStatus.DAMAGED;
                } else if (newLevel < entry.ownedMaxLevel) {
                    entry.status = UpgradeStatus.DEGRADED;
                } else {
                    entry.status = UpgradeStatus.ACTIVE;
                }

                entry.isPaused = false;
            }

            sendSetLevel(upgradeId, newLevel);

            if (newLevel == 0 && old > 0) {
                this.mc.player.playSound(SoundEvents.BLOCK_NOTE_BASS, 0.7F, 0.8F);

                if (entry.status == UpgradeStatus.DAMAGED) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚒ " + entry.displayName + " 已降至 Lv.0 (点击+修复)"
                    ), true);
                } else {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⏸ " + entry.displayName + " 已暂停 (点击+恢复)"
                    ), true);
                }
            } else {
                this.mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5F, 1.0F);
                if (old > newLevel) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "↓ " + entry.displayName + " 降级至 Lv." + newLevel
                    ), true);
                } else {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "↑ " + entry.displayName + " 升级至 Lv." + newLevel
                    ), true);
                }
            }

            pendingUpdates.put(upgradeId, System.currentTimeMillis());
        }
    }

    // ===== 辅助方法 =====

    private void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel) {
        if (core == null || core.isEmpty()) return;

        NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        if (!core.hasTagCompound()) core.setTagCompound(nbt);

        if (isWaterproofUpgrade(upgradeId)) {
            for (String wid : WATERPROOF_IDS) {
                String U = up(wid), L = lo(wid);
                nbt.setInteger("upgrade_" + wid, newLevel);
                nbt.setInteger("upgrade_" + U, newLevel);
                nbt.setInteger("upgrade_" + L, newLevel);

                if (newLevel > 0) {
                    nbt.setBoolean("HasUpgrade_" + wid, true);
                    nbt.setBoolean("HasUpgrade_" + U, true);
                    nbt.setBoolean("HasUpgrade_" + L, true);
                }

                try {
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, wid, newLevel);
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, U, newLevel);
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, L, newLevel);
                } catch (Exception ignored) {}
            }

            try {
                for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                    if (isWaterproofUpgrade(type.getKey())) {
                        ItemMechanicalCore.setUpgradeLevel(core, type, newLevel);
                    }
                }
            } catch (Exception ignored) {}
        } else {
            String U = up(upgradeId), L = lo(upgradeId);
            nbt.setInteger("upgrade_" + upgradeId, newLevel);
            nbt.setInteger("upgrade_" + U, newLevel);
            nbt.setInteger("upgrade_" + L, newLevel);

            if (newLevel > 0) {
                nbt.setBoolean("HasUpgrade_" + upgradeId, true);
                nbt.setBoolean("HasUpgrade_" + U, true);
                nbt.setBoolean("HasUpgrade_" + L, true);
            }

            try {
                ItemMechanicalCoreExtended.setUpgradeLevel(core, upgradeId, newLevel);
                ItemMechanicalCoreExtended.setUpgradeLevel(core, U, newLevel);
                ItemMechanicalCoreExtended.setUpgradeLevel(core, L, newLevel);
            } catch (Exception ignored) {}

            try {
                for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                    if (type.getKey().equalsIgnoreCase(upgradeId)) {
                        ItemMechanicalCore.setUpgradeLevel(core, type, newLevel);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void writePauseMeta(ItemStack core, String upgradeId, int lastLevel, boolean paused) {
        if (core == null || core.isEmpty()) return;

        NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        if (!core.hasTagCompound()) core.setTagCompound(nbt);

        if (isWaterproofUpgrade(upgradeId)) {
            for (String wid : WATERPROOF_IDS) {
                String U = up(wid), L = lo(wid);
                if (paused && lastLevel > 0) {
                    nbt.setInteger("LastLevel_" + wid, lastLevel);
                    nbt.setInteger("LastLevel_" + U, lastLevel);
                    nbt.setInteger("LastLevel_" + L, lastLevel);

                    int ownedMax = getOwnedMaxFromNBT(nbt, wid);
                    if (ownedMax < lastLevel) {
                        nbt.setInteger("OwnedMax_" + wid, lastLevel);
                        nbt.setInteger("OwnedMax_" + U, lastLevel);
                        nbt.setInteger("OwnedMax_" + L, lastLevel);
                    }

                    nbt.setBoolean("HasUpgrade_" + wid, true);
                    nbt.setBoolean("HasUpgrade_" + U, true);
                    nbt.setBoolean("HasUpgrade_" + L, true);
                }
                nbt.setBoolean("IsPaused_" + wid, paused);
                nbt.setBoolean("IsPaused_" + U, paused);
                nbt.setBoolean("IsPaused_" + L, paused);
            }
        } else {
            String U = up(upgradeId), L = lo(upgradeId);
            if (paused && lastLevel > 0) {
                for (String k : Arrays.asList(upgradeId, U, L)) {
                    nbt.setInteger("LastLevel_" + k, lastLevel);
                    if (nbt.getInteger("OwnedMax_" + k) < lastLevel)
                        nbt.setInteger("OwnedMax_" + k, lastLevel);
                    nbt.setBoolean("HasUpgrade_" + k, true);
                }
            }
            for (String k : Arrays.asList(upgradeId, U, L)) {
                nbt.setBoolean("IsPaused_" + k, paused);
            }
        }
    }

    private void sendSetLevel(String id, int level) {
        try {
            NetworkHandler.INSTANCE.sendToServer(new PacketMechanicalCoreUpdate(
                    PacketMechanicalCoreUpdate.Action.SET_LEVEL, id, level, true
            ));
        } catch (Throwable ignored) {}
    }

    private boolean checkCanRunWithEnergy(String upgradeId) {
        try {
            ItemStack core = getCurrentCoreStack();
            EnergyDepletionManager.EnergyStatus st = EnergyDepletionManager.getCurrentEnergyStatus(core);
            switch (st) {
                case NORMAL: return true;
                case POWER_SAVING: return !isHighConsumptionUpgrade(upgradeId);
                case EMERGENCY: return isImportantUpgrade(upgradeId);
                case CRITICAL: return isEssentialUpgrade(upgradeId);
                default: return true;
            }
        } catch (Throwable t) {
            return true;
        }
    }

    private boolean isHighConsumptionUpgrade(String id) {
        String u = up(id);
        return u.equals("ORE_VISION") || u.equals("STEALTH") || u.equals("FLIGHT_MODULE")
                || u.equals("KINETIC_GENERATOR") || u.equals("SOLAR_GENERATOR");
    }

    private boolean isImportantUpgrade(String id) {
        String u = up(id);
        return u.equals("HEALTH_REGEN") || u.equals("REGENERATION")
                || u.equals("YELLOW_SHIELD") || u.equals("SHIELD_GENERATOR")
                || u.equals("DAMAGE_BOOST") || u.equals("ARMOR_ENHANCEMENT")
                || isWaterproofUpgrade(u);
    }

    private boolean isEssentialUpgrade(String id) {
        String u = up(id);
        return u.equals("HEALTH_REGEN") || u.equals("REGENERATION")
                || u.equals("FIRE_EXTINGUISH") || u.equals("THORNS")
                || isWaterproofUpgrade(u);
    }

    private String fmt(int e){
        if (e == Integer.MAX_VALUE) return "∞";
        if (e >= 1_000_000) return String.format("%.1fM", e/1_000_000f);
        if (e >= 1_000) return String.format("%.1fk", e/1_000f);
        return String.valueOf(e);
    }

    private boolean inBtn(int mx, int my, int x, int y, int s){
        return mx>=x && mx<=x+s && my>=y && my<=y+s;
    }

    private int getMaxLevel(ItemMechanicalCore.UpgradeType type) {
        switch (type) {
            case ENERGY_CAPACITY: return 10;
            case ENERGY_EFFICIENCY: return 5;
            case ARMOR_ENHANCEMENT: return 5;
            case SPEED_BOOST: return 3;
            case REGENERATION: return 3;
            case FLIGHT_MODULE: return 3;
            case SHIELD_GENERATOR: return 3;
            case TEMPERATURE_CONTROL: return 5;
            default: return 3;
        }
    }

    @Override
    public boolean doesGuiPauseGame(){ return false; }

    @Override
    public void onGuiClosed(){ super.onGuiClosed(); }
}