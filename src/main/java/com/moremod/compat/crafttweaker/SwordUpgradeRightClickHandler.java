package com.moremod.compat.crafttweaker;

import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.world.IBlockPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 剑升级右键事件处理器
 * 支持对空右键和对方块右键
 *
 * ✅ 修正版：直接遍历6个槽位，不依赖Count字段
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SwordUpgradeRightClickHandler {

    private static final boolean DEBUG = false; // 调试模式

    /**
     * 对空右键事件（不点击方块）
     */
    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        // 只处理服务端
        if (player.world.isRemote) return;

        // 只处理剑
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSword)) return;

        // 获取所有镶嵌的材料
        List<String> materials = getInlayedMaterials(stack);
        if (materials.isEmpty()) return;

        if (DEBUG) {
            System.out.println("[SwordUpgrade] 对空右键，检测到材料: " + materials);
        }

        // 转换为 CraftTweaker 类型（只转换一次）
        IPlayer ctPlayer = CraftTweakerMC.getIPlayer(player);
        IWorld ctWorld = CraftTweakerMC.getIWorld(player.world);
        IItemStack ctStack = CraftTweakerMC.getIItemStack(stack);

        // 触发每种材料的效果
        for (String materialId : materials) {
            List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);
            if (effects != null) {
                for (IUpgradeEffect effect : effects) {
                    // 只处理对空右键效果
                    if (effect instanceof IOnItemUseEffect) {
                        try {
                            ((IOnItemUseEffect) effect).onItemUse(ctPlayer, ctWorld, ctStack);

                            if (DEBUG) {
                                System.out.println("[SwordUpgrade] 执行 " + materialId + " 的对空右键效果");
                            }
                        } catch (Exception e) {
                            System.err.println("[SwordUpgrade] 执行对空右键效果出错: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 对方块右键事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        // 只处理服务端
        if (player.world.isRemote) return;

        // 只处理剑
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSword)) return;

        // 获取所有镶嵌的材料
        List<String> materials = getInlayedMaterials(stack);
        if (materials.isEmpty()) return;

        if (DEBUG) {
            System.out.println("[SwordUpgrade] 对方块右键，检测到材料: " + materials);
        }

        // 转换为 CraftTweaker 类型（只转换一次）
        IPlayer ctPlayer = CraftTweakerMC.getIPlayer(player);
        IWorld ctWorld = CraftTweakerMC.getIWorld(player.world);
        IBlockPos ctPos = CraftTweakerMC.getIBlockPos(event.getPos());
        IItemStack ctStack = CraftTweakerMC.getIItemStack(stack);

        // 触发每种材料的效果
        for (String materialId : materials) {
            List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);
            if (effects != null) {
                for (IUpgradeEffect effect : effects) {
                    // 只处理对方块右键效果
                    if (effect instanceof IOnRightClickEffect) {
                        try {
                            ((IOnRightClickEffect) effect).onRightClick(ctPlayer, ctWorld, ctPos, ctStack);

                            if (DEBUG) {
                                System.out.println("[SwordUpgrade] 执行 " + materialId + " 的对方块右键效果");
                            }
                        } catch (Exception e) {
                            System.err.println("[SwordUpgrade] 执行对方块右键效果出错: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    // ========================================
    // ✅ 核心修正：正确的NBT读取方法
    // ========================================

    /**
     * 获取剑上所有镶嵌的材料ID（去重）
     *
     * ✅ 修正：直接遍历6个槽位（0-5），不依赖Count字段
     * 这与 ContainerSwordUpgradeStation 的逻辑一致
     */
    private static List<String> getInlayedMaterials(ItemStack sword) {
        List<String> materials = new ArrayList<>();
        Set<String> uniqueMaterials = new HashSet<>(); // 用于去重

        if (!sword.hasTagCompound()) {
            if (DEBUG) System.out.println("[SwordUpgrade] 剑没有NBT");
            return materials;
        }

        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) {
            if (DEBUG) System.out.println("[SwordUpgrade] 没有SwordUpgrades标签");
            return materials;
        }

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) {
            if (DEBUG) System.out.println("[SwordUpgrade] 没有Inlays标签");
            return materials;
        }

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");

        // ✅ 核心修正：遍历所有5个可能的槽位（0-5）
        // 不依赖 Count 字段，直接检查每个槽位是否存在
        for (int i = 0; i < 6; i++) {
            String inlayKey = "Inlay_" + i;
            if (inlaysTag.hasKey(inlayKey)) {
                NBTTagCompound inlay = inlaysTag.getCompoundTag(inlayKey);
                if (inlay.hasKey("Material")) {
                    String material = inlay.getString("Material");

                    if (DEBUG) {
                        System.out.println("[SwordUpgrade] 槽位 " + i + " 材料: " + material);
                    }

                    // 去重：只添加未添加过的材料
                    if (!uniqueMaterials.contains(material)) {
                        uniqueMaterials.add(material);
                        materials.add(material);
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println("[SwordUpgrade] 最终材料列表: " + materials);
        }

        return materials;
    }

    /**
     * 调试方法：打印剑的完整NBT结构
     */
    @SuppressWarnings("unused")
    private static void debugPrintNBT(ItemStack sword) {
        if (!sword.hasTagCompound()) {
            System.out.println("[DEBUG] 剑没有NBT数据");
            return;
        }

        System.out.println("[DEBUG] 剑的NBT: " + sword.getTagCompound().toString());
    }
}