package com.moremod.client;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.config.MechanicalCoreHUDConfig;
import com.moremod.item.EnchantmentBoostBauble;
import com.moremod.item.ItemDimensionalRipper;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemVoidBackpackLink;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class KeyBindHandler {

    // ===== 游戏内按键绑定 =====
    public static KeyBinding toggleHudKey;
    public static KeyBinding openCoreGui;
    public static KeyBinding oreVisionKey;
    public static KeyBinding oreFilterKey;
    public static KeyBinding stealthKey;
    public static KeyBinding dimensionalRipperKey;
    public static KeyBinding personalDimensionKey;
    public static KeyBinding activateEnchantBoost;
    public static KeyBinding detailInfoKey;
    public static KeyBinding scrollUpgradesKey;
    public static KeyBinding openVoidBackpackKey;  // 🌌 虚空背包按键

    // ===== 按键状态管理 =====
    private static boolean enchantBoostKeyPressed = false;
    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN = 300;

    private static boolean hudToggleKeyPressed = false;
    private static boolean hudVisible = true;
    private static boolean openGuiKeyPressed = false;
    private static boolean stealthKeyPressed = false;
    private static boolean oreVisionKeyPressed = false;
    private static boolean oreFilterKeyPressed = false;
    private static boolean ripperKeyPressed = false;
    private static boolean personalDimKeyPressed = false;
    private static boolean scrollKeyPressed = false;
    private static boolean voidBackpackKeyPressed = false;  // 🌌 虚空背包按键状态
    private static int scrollOffset = 0;

    public static void init() {
        System.out.println("[moremod] 初始化按键绑定...");

        // HUD控制
        toggleHudKey = new KeyBinding("切换机械核心HUD显示",
                KeyConflictContext.IN_GAME, Keyboard.KEY_H, "机械核心HUD");
        ClientRegistry.registerKeyBinding(toggleHudKey);

        // 机械核心
        openCoreGui = new KeyBinding("打开机械核心面板",
                KeyConflictContext.IN_GAME, Keyboard.KEY_P, "机械核心");
        ClientRegistry.registerKeyBinding(openCoreGui);

        oreVisionKey = new KeyBinding("切换矿物透视",
                KeyConflictContext.IN_GAME, Keyboard.KEY_V, "机械核心");
        ClientRegistry.registerKeyBinding(oreVisionKey);

        oreFilterKey = new KeyBinding("切换矿物过滤",
                KeyConflictContext.IN_GAME, Keyboard.KEY_B, "机械核心");
        ClientRegistry.registerKeyBinding(oreFilterKey);

        stealthKey = new KeyBinding("切换隐身模式",
                KeyConflictContext.IN_GAME, Keyboard.KEY_X, "机械核心");
        ClientRegistry.registerKeyBinding(stealthKey);

        // 维度工具
        dimensionalRipperKey = new KeyBinding("维度撕裂者操作",
                KeyConflictContext.IN_GAME, Keyboard.KEY_Y, "维度工具");
        ClientRegistry.registerKeyBinding(dimensionalRipperKey);

        personalDimensionKey = new KeyBinding("进入/离开私人维度",
                KeyConflictContext.IN_GAME, Keyboard.KEY_U, "维度工具");
        ClientRegistry.registerKeyBinding(personalDimensionKey);

        // 附魔系统
        activateEnchantBoost = new KeyBinding("激活附魔增强饰品",
                KeyConflictContext.IN_GAME, Keyboard.KEY_G, "附魔系统");
        ClientRegistry.registerKeyBinding(activateEnchantBoost);

        // HUD详细控制
        detailInfoKey = new KeyBinding("显示详细信息（按住）",
                KeyConflictContext.IN_GAME, Keyboard.KEY_LSHIFT, "机械核心HUD");
        ClientRegistry.registerKeyBinding(detailInfoKey);

        scrollUpgradesKey = new KeyBinding("滚动升级列表",
                KeyConflictContext.IN_GAME, Keyboard.KEY_TAB, "机械核心HUD");
        ClientRegistry.registerKeyBinding(scrollUpgradesKey);

        // 🌌 虚空背包链接
        openVoidBackpackKey = new KeyBinding("打开虚空背包",
                KeyConflictContext.IN_GAME, Keyboard.KEY_K, "饰品系统");
        ClientRegistry.registerKeyBinding(openVoidBackpackKey);

        System.out.println("[moremod] 按键绑定完成");
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.world == null) return;

        handleKeyInput(player);
    }

    private static void handleKeyInput(EntityPlayer player) {
        // HUD切换
        if (toggleHudKey.isPressed()) {
            toggleHudVisibility(player);
        }

        // 打开GUI
        if (openCoreGui.isPressed()) {
            handleOpenCoreGui(player);
        }

        // 滚动升级列表
        if (scrollUpgradesKey.isKeyDown()) {
            if (!scrollKeyPressed) {
                scrollKeyPressed = true;
                handleScrollUpgrades(player);
            }
        } else {
            scrollKeyPressed = false;
        }

        // 附魔增强
        if (activateEnchantBoost.isKeyDown()) {
            if (!enchantBoostKeyPressed) {
                enchantBoostKeyPressed = true;
                handleEnchantBoostActivation(player);
            }
        } else {
            enchantBoostKeyPressed = false;
        }

        // 🌌 虚空背包
        if (openVoidBackpackKey.isKeyDown()) {
            if (!voidBackpackKeyPressed) {
                voidBackpackKeyPressed = true;
                handleOpenVoidBackpack(player);
            }
        } else {
            voidBackpackKeyPressed = false;
        }

        // 其他按键处理
        handleDimensionalKeys(player);
        handleMechanicalCoreKeys(player);
    }

    /** 附魔增强激活处理 */
    private static void handleEnchantBoostActivation(EntityPlayer player) {
        PacketHandler.INSTANCE.sendToServer(new PacketActivateBoost());
    }

    /** 🌌 虚空背包处理 */
    private static void handleOpenVoidBackpack(EntityPlayer player) {
        if (ItemVoidBackpackLink.isEquipped(player)) {
            System.out.println("[MoreMod-Client] 按K键，发送打开虚空背包数据包");
            PacketHandler.INSTANCE.sendToServer(new PacketOpenVoidBackpack());
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 需要装备虚空背包链接！"
            ), true);
        }
    }

    private static void handleDimensionalKeys(EntityPlayer player) {
        // 维度撕裂者
        if (dimensionalRipperKey.isKeyDown()) {
            if (!ripperKeyPressed) {
                ripperKeyPressed = true;
                handleDimensionalRipper(player);
            }
        } else {
            ripperKeyPressed = false;
        }

        // 私人维度
        if (personalDimensionKey.isKeyDown()) {
            if (!personalDimKeyPressed) {
                personalDimKeyPressed = true;
                handlePersonalDimension(player);
            }
        } else {
            personalDimKeyPressed = false;
        }
    }

    private static void handleMechanicalCoreKeys(EntityPlayer player) {
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

    private static void toggleHudVisibility(EntityPlayer player) {
        if (!MechanicalCoreHUDConfig.enabled) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "HUD功能已在配置中禁用"
            ), true);
            return;
        }

        hudVisible = !hudVisible;
        MechanicalCoreHUDConfig.setHudVisible(hudVisible);

        String message = hudVisible ?
                TextFormatting.GREEN + "机械核心HUD已启用" :
                TextFormatting.RED + "机械核心HUD已禁用";
        player.sendMessage(new TextComponentString(message));
        player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.5F, hudVisible ? 1.0F : 0.8F);
    }

    private static void handleOpenCoreGui(EntityPlayer player) {
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (ItemMechanicalCore.isMechanicalCore(coreStack)) {
            player.openGui("moremod", 0, player.world, 0, 0, 0);
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "请先装备机械核心！"
            ), true);
        }
    }

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

    private static void handleOreFilter(EntityPlayer player) {
        if (AuxiliaryUpgradeManager.OreVisionSystem.isOreVisionActive()) {
            AuxiliaryUpgradeManager.OreVisionSystem.cycleOreCategory(player);
        } else {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "请先开启矿物透视"
            ), true);
        }
    }

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
                    AuxiliaryUpgradeManager.StealthSystem.toggle(player);
                }
            } else {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "未安装隐身升级"
                ), true);
            }
        }
    }

    private static void handleScrollUpgrades(EntityPlayer player) {
        if (!isHudVisible()) return;

        scrollOffset++;
        int maxUpgrades = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
        if (scrollOffset >= maxUpgrades) {
            scrollOffset = 0;
        }
    }

    private static boolean checkCooldown() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime < TOGGLE_COOLDOWN) return false;
        lastToggleTime = currentTime;
        return true;
    }

    public static boolean isHudVisible() {
        return MechanicalCoreHUDConfig.enabled && hudVisible;
    }

    public static boolean shouldShowDetailedInfo() {
        return detailInfoKey.isKeyDown();
    }

    public static int getScrollOffset() {
        return scrollOffset;
    }

    public static void resetScrollOffset() {
        scrollOffset = 0;
    }
}