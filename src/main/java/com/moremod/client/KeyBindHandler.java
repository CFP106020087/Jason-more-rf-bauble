package com.moremod.client;

import com.moremod.config.MechanicalCoreHUDConfig;
import com.moremod.enchantment.EnchantmentBoostHelper;
import com.moremod.item.ItemDimensionalRipper;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.network.*;
import com.moremod.upgrades.auxiliary.AuxiliaryUpgradeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端按键绑定处理器 - 独立版
 * 所有按键功能和设置都在这里管理，不依赖配置文件的按键设置
 */
@SideOnly(Side.CLIENT)
public class KeyBindHandler {

    // ===== 游戏内按键绑定 =====
    public static KeyBinding toggleHudKey;       // HUD切换键
    public static KeyBinding openCoreGui;        // 打开GUI
    public static KeyBinding oreVisionKey;       // 矿物透视
    public static KeyBinding oreFilterKey;       // 矿物过滤
    public static KeyBinding stealthKey;         // 隐身模式
    public static KeyBinding dimensionalRipperKey; // 维度撕裂者
    public static KeyBinding personalDimensionKey; // 私人维度
    public static KeyBinding activateEnchantBoost; // 附魔增强
    public static KeyBinding detailInfoKey;      // 显示详细信息（按住）
    public static KeyBinding scrollUpgradesKey;  // 滚动升级列表

    // ===== 按键状态管理 =====
    private static final Map<String, Boolean> keyPressStates = new HashMap<>();
    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN = 300; // 300ms冷却

    // ===== HUD状态管理 =====
    private static boolean hudToggleKeyPressed = false;
    private static boolean hudVisible = true; // HUD显示状态

    // ===== 功能按键状态 =====
    private static boolean openGuiKeyPressed = false;
    private static boolean stealthKeyPressed = false;
    private static boolean oreVisionKeyPressed = false;
    private static boolean oreFilterKeyPressed = false;
    private static boolean ripperKeyPressed = false;
    private static boolean personalDimKeyPressed = false;
    private static boolean enchantBoostKeyPressed = false;
    private static boolean scrollKeyPressed = false;
    private static int scrollOffset = 0; // 升级列表滚动偏移

    // ===== 附魔增强冷却管理 =====
    private static final Map<UUID, Long> enchantBoostCooldowns = new HashMap<>();

    /** 初始化按键绑定 */
    public static void init() {
        System.out.println("[moremod] 初始化按键绑定...");

        // HUD控制
        toggleHudKey = new KeyBinding("切换机械核心HUD显示",
                KeyConflictContext.IN_GAME, Keyboard.KEY_H, "机械核心HUD");
        ClientRegistry.registerKeyBinding(toggleHudKey);

        // 机械核心
        openCoreGui = new KeyBinding("打开机械核心面板",
                KeyConflictContext.IN_GAME, Keyboard.KEY_H, "机械核心");
        ClientRegistry.registerKeyBinding(openCoreGui);

        oreVisionKey = new KeyBinding("切换矿物透视",
                KeyConflictContext.IN_GAME, Keyboard.KEY_V, "机械核心");
        ClientRegistry.registerKeyBinding(oreVisionKey);

        oreFilterKey = new KeyBinding("切换矿物过滤",
                KeyConflictContext.IN_GAME, Keyboard.KEY_B, "机械核心");
        ClientRegistry.registerKeyBinding(oreFilterKey);

        stealthKey = new KeyBinding("切换隐身模式",
                KeyConflictContext.IN_GAME, Keyboard.KEY_G, "机械核心");
        ClientRegistry.registerKeyBinding(stealthKey);

        // 维度工具
        dimensionalRipperKey = new KeyBinding("维度撕裂者操作",
                KeyConflictContext.IN_GAME, Keyboard.KEY_Y, "维度工具");
        ClientRegistry.registerKeyBinding(dimensionalRipperKey);

        personalDimensionKey = new KeyBinding("进入/离开私人维度",
                KeyConflictContext.IN_GAME, Keyboard.KEY_U, "维度工具");
        ClientRegistry.registerKeyBinding(personalDimensionKey);

        // 附魔系统
        activateEnchantBoost = new KeyBinding("激活附魔增强",
                KeyConflictContext.IN_GAME, Keyboard.KEY_R, "附魔系统");
        ClientRegistry.registerKeyBinding(activateEnchantBoost);

        // HUD详细控制
        detailInfoKey = new KeyBinding("显示详细信息（按住）",
                KeyConflictContext.IN_GAME, Keyboard.KEY_LSHIFT, "机械核心HUD");
        ClientRegistry.registerKeyBinding(detailInfoKey);

        scrollUpgradesKey = new KeyBinding("滚动升级列表",
                KeyConflictContext.IN_GAME, Keyboard.KEY_TAB, "机械核心HUD");
        ClientRegistry.registerKeyBinding(scrollUpgradesKey);

        System.out.println("[moremod] 按键绑定完成");
    }

    /** 处理按键事件 */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.world == null) return;

        handleKeyInput(player);
    }

    /** 处理所有按键输入 */
    private static void handleKeyInput(EntityPlayer player) {
        // ===== HUD切换 =====
        if (toggleHudKey.isPressed()) {
            toggleHudVisibility(player);
        }

        // ===== 打开GUI =====
        if (openCoreGui.isPressed()) {
            handleOpenCoreGui(player);
        }

        // ===== 滚动升级列表 =====
        if (scrollUpgradesKey.isKeyDown()) {
            if (!scrollKeyPressed) {
                scrollKeyPressed = true;
                handleScrollUpgrades(player);
            }
        } else {
            scrollKeyPressed = false;
        }

        // ===== 附魔增强 =====
        if (activateEnchantBoost.isKeyDown()) {
            EnchantmentBoostHelper.setKeyActive(player, true);

            if (!enchantBoostKeyPressed) {
                enchantBoostKeyPressed = true;
                handleEnchantmentBoost(player);
            }
        } else {
            if (enchantBoostKeyPressed) {
                EnchantmentBoostHelper.setKeyActive(player, false);
            }
            enchantBoostKeyPressed = false;
        }

        // ===== 维度撕裂者 =====
        if (dimensionalRipperKey.isKeyDown()) {
            if (!ripperKeyPressed) {
                ripperKeyPressed = true;
                handleDimensionalRipper(player);
            }
        } else {
            ripperKeyPressed = false;
        }

        // ===== 私人维度 =====
        if (personalDimensionKey.isKeyDown()) {
            if (!personalDimKeyPressed) {
                personalDimKeyPressed = true;
                handlePersonalDimension(player);
            }
        } else {
            personalDimKeyPressed = false;
        }

        // ===== 机械核心相关 =====
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        boolean hasCore = ItemMechanicalCore.isMechanicalCore(coreStack);

        if (!hasCore) {
            if (oreVisionKey.isPressed() || oreFilterKey.isPressed() || stealthKey.isPressed()) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "请先装备机械核心！"
                ), true);
            }
            return;
        }

        // 矿物透视
        if (oreVisionKey.isKeyDown()) {
            if (!oreVisionKeyPressed) {
                oreVisionKeyPressed = true;
                handleOreVision(player, coreStack);
            }
        } else {
            oreVisionKeyPressed = false;
        }

        // 矿物过滤
        if (oreFilterKey.isKeyDown()) {
            if (!oreFilterKeyPressed) {
                oreFilterKeyPressed = true;
                handleOreFilter(player);
            }
        } else {
            oreFilterKeyPressed = false;
        }

        // 隐身模式
        if (stealthKey.isKeyDown()) {
            if (!stealthKeyPressed) {
                stealthKeyPressed = true;
                handleStealth(player, coreStack);
            }
        } else {
            stealthKeyPressed = false;
        }
    }

    /* ===== HUD功能 ===== */

    /** 切换HUD显示状态 */
    private static void toggleHudVisibility(EntityPlayer player) {
        // 检查是否启用了HUD功能
        if (!MechanicalCoreHUDConfig.enabled) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "HUD功能已在配置中禁用"
            ), true);
            return;
        }

        // 切换状态
        hudVisible = !hudVisible;

        // 更新配置中的状态（让其他系统可以读取）
        MechanicalCoreHUDConfig.setHudVisible(hudVisible);

        // 显示提示消息
        String message = hudVisible ?
                TextFormatting.GREEN + "机械核心HUD已启用" :
                TextFormatting.RED + "机械核心HUD已禁用";
        player.sendMessage(new TextComponentString(message));

        // 播放音效
        player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5F, hudVisible ? 1.0F : 0.8F);
    }

    /** 获取HUD是否可见 */
    public static boolean isHudVisible() {
        return MechanicalCoreHUDConfig.enabled && hudVisible;
    }

    /** 是否应该显示详细信息 */
    public static boolean shouldShowDetailedInfo() {
        return detailInfoKey.isKeyDown();
    }

    /** 处理滚动升级列表 */
    private static void handleScrollUpgrades(EntityPlayer player) {
        if (!isHudVisible()) return;

        scrollOffset++;
        int maxUpgrades = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
        if (scrollOffset >= maxUpgrades) {
            scrollOffset = 0;
        }

        if (MechanicalCoreHUDConfig.showScrollHints) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "升级列表偏移: " + scrollOffset
            ), true);
        }
    }

    /** 获取滚动偏移 */
    public static int getScrollOffset() {
        return scrollOffset;
    }

    /** 重置滚动偏移 */
    public static void resetScrollOffset() {
        scrollOffset = 0;
    }

    /* ===== 功能处理方法 ===== */

    /** 处理打开机械核心GUI */
    private static void handleOpenCoreGui(EntityPlayer player) {
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (ItemMechanicalCore.isMechanicalCore(coreStack)) {
            player.openGui("moremod", 0, player.world, 0, 0, 0);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "打开机械核心控制面板"
            ), true);
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "请先装备机械核心！"
            ), true);
        }
    }

    /** 处理附魔增强 */
    private static void handleEnchantmentBoost(EntityPlayer player) {
        if (!EnchantmentBoostHelper.hasBoostBauble(player)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "需要佩戴附魔增强戒指！"
            ), true);
            return;
        }

        if (EnchantmentBoostHelper.hasActiveBoost(player)) {
            int remaining = EnchantmentBoostHelper.getRemainingTime(player);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "附魔增强还有 " + remaining + " 秒"
            ), true);
            return;
        }

        if (isEnchantBoostOnCooldown(player)) {
            int cooldownRemaining = getEnchantBoostCooldownRemaining(player);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "附魔增强冷却中... 还需 " + cooldownRemaining + " 秒"
            ), true);
            return;
        }

        int boostAmount = EnchantmentBoostHelper.getBaubleBoostAmount(player);
        int duration = MechanicalCoreHUDConfig.enchantBoostDurationSec;

        PacketHandler.INSTANCE.sendToServer(new PacketActivateBoost(boostAmount));
        EnchantmentBoostHelper.activateBoost(player, boostAmount, duration);
        setEnchantBoostCooldown(player);

        player.playSound(net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "✦ 附魔增强已激活！ +" + boostAmount + " (" + duration + "秒)"
        ), true);
    }

    /** 处理维度撕裂者 */
    private static void handleDimensionalRipper(EntityPlayer player) {
        ItemStack ripperStack = ItemDimensionalRipper.findEquippedRipper(player);
        if (ripperStack != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketDimensionalRipperKey());
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "请先装备维度撕裂者！"
            ), true);
        }
    }

    /** 处理私人维度 */
    private static void handlePersonalDimension(EntityPlayer player) {
        ItemStack ripperStack = ItemDimensionalRipper.findEquippedRipper(player);
        if (ripperStack != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketPersonalDimensionKey());
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "请先装备维度撕裂者才能进入私人维度！"
            ), true);
        }
    }

    /** 处理矿物透视 */
    private static void handleOreVision(EntityPlayer player, ItemStack coreStack) {
        if (checkCooldown()) {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "ORE_VISION");
            if (level > 0) {
                boolean isActive = AuxiliaryUpgradeManager.OreVisionSystem.isOreVisionActive();
                AuxiliaryUpgradeManager.OreVisionSystem.toggleOreVision(player, !isActive);
            } else {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "未安装矿物透视升级"
                ), true);
            }
        }
    }

    /** 处理矿物过滤 */
    private static void handleOreFilter(EntityPlayer player) {
        if (AuxiliaryUpgradeManager.OreVisionSystem.isOreVisionActive()) {
            AuxiliaryUpgradeManager.OreVisionSystem.cycleOreCategory(player);
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "请先开启矿物透视 (按" + oreVisionKey.getDisplayName() + ")"
            ), true);
        }
    }

    /** 处理隐身模式 */
    private static void handleStealth(EntityPlayer player, ItemStack coreStack) {
        if (checkCooldown()) {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "STEALTH");
            if (level > 0) {
                if (AuxiliaryUpgradeManager.StealthSystem.isInCooldown(player)) {
                    int remaining = AuxiliaryUpgradeManager.StealthSystem.getRemainingCooldown(player);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⏱ 隐身冷却中... 剩余 " + remaining + " 秒"
                    ), true);
                } else {
                    boolean isActive = AuxiliaryUpgradeManager.StealthSystem.isStealthActive(player);
                    if (isActive) {
                        String status = AuxiliaryUpgradeManager.StealthSystem.getStealthStatusInfo(player);
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.AQUA + "隐身状态: " + status
                        ), true);
                    } else {
                        AuxiliaryUpgradeManager.StealthSystem.toggle(player);
                    }
                }
            } else {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "未安装隐身升级"
                ), true);
            }
        }
    }

    /* ===== 冷却管理 ===== */

    private static boolean isEnchantBoostOnCooldown(EntityPlayer player) {
        UUID id = player.getUniqueID();
        Long end = enchantBoostCooldowns.get(id);
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            enchantBoostCooldowns.remove(id);
            return false;
        }
        return true;
    }

    private static int getEnchantBoostCooldownRemaining(EntityPlayer player) {
        UUID id = player.getUniqueID();
        Long end = enchantBoostCooldowns.get(id);
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000L);
    }

    private static void setEnchantBoostCooldown(EntityPlayer player) {
        int cooldownSec = MechanicalCoreHUDConfig.enchantBoostCooldownSec;
        enchantBoostCooldowns.put(player.getUniqueID(),
                System.currentTimeMillis() + cooldownSec * 1000L);
    }

    private static boolean checkCooldown() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime < TOGGLE_COOLDOWN) return false;
        lastToggleTime = currentTime;
        return true;
    }

    /* ===== 显示帮助信息 ===== */

    public static void showKeyBindHelp(EntityPlayer player) {
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 按键绑定帮助 ==="
        ));

        // HUD控制
        player.sendMessage(new TextComponentString(TextFormatting.DARK_GREEN + "【HUD控制】"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                toggleHudKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "切换HUD显示"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                detailInfoKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "显示详细信息（按住）"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                scrollUpgradesKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "滚动升级列表"));

        // 机械核心
        player.sendMessage(new TextComponentString(TextFormatting.AQUA + "【机械核心】"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                openCoreGui.getDisplayName() + "] " +
                TextFormatting.WHITE + "打开控制面板"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                oreVisionKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "切换矿物透视"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                oreFilterKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "切换矿物过滤"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                stealthKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "切换隐身模式"));

        // 维度撕裂者
        player.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "【维度撕裂者】"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                dimensionalRipperKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "设置起点/终点/激活传送门"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                personalDimensionKey.getDisplayName() + "] " +
                TextFormatting.WHITE + "进入/离开私人维度"));

        // 附魔系统
        player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "【附魔系统】"));
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" +
                activateEnchantBoost.getDisplayName() + "] " +
                TextFormatting.WHITE + "按住启用饰品增幅；按下触发临时增幅"));

        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "提示: 可在游戏设置-控制中修改按键"
        ));
    }

    public static String getKeyBindStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("按键绑定状态:\n");
        sb.append("HUD显示: ").append(isHudVisible() ? "开启" : "关闭").append("\n");

        sb.append("\n【HUD控制】\n");
        sb.append("  切换: ").append(toggleHudKey.getDisplayName()).append("\n");
        sb.append("  详细: ").append(detailInfoKey.getDisplayName()).append("\n");
        sb.append("  滚动: ").append(scrollUpgradesKey.getDisplayName()).append("\n");

        sb.append("\n【机械核心】\n");
        sb.append("  GUI: ").append(openCoreGui.getDisplayName()).append("\n");
        sb.append("  矿视: ").append(oreVisionKey.getDisplayName()).append("\n");
        sb.append("  过滤: ").append(oreFilterKey.getDisplayName()).append("\n");
        sb.append("  隐身: ").append(stealthKey.getDisplayName()).append("\n");

        sb.append("\n【维度工具】\n");
        sb.append("  撕裂者: ").append(dimensionalRipperKey.getDisplayName()).append("\n");
        sb.append("  私人维度: ").append(personalDimensionKey.getDisplayName()).append("\n");

        sb.append("\n【附魔系统】\n");
        sb.append("  增强: ").append(activateEnchantBoost.getDisplayName()).append("\n");
        sb.append("  冷却: ").append(MechanicalCoreHUDConfig.enchantBoostCooldownSec).append("秒\n");
        sb.append("  持续: ").append(MechanicalCoreHUDConfig.enchantBoostDurationSec).append("秒\n");

        return sb.toString();
    }
}