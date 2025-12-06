package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.core.CurseDeathHook;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 虚无之眸 - 七咒之戒联动饰品
 * Eye of the Void
 *
 * 效果：
 * - 基础效果（负面）：降低最大生命值 4 点
 * - 七咒联动：致命伤害时消耗经验抵御死亡
 *
 * 机制：
 * - 需要佩戴七咒之戒才能装备
 * - 致命伤害时消耗 3 级经验阻止死亡
 * - 触发后恢复 4 点血量
 * - 30秒冷却时间
 * - 触发后1.5秒无敌
 *
 * 代价：
 * - 装备时永久降低最大生命值
 * - 经验消耗
 */
public class ItemVoidGaze extends Item implements IBauble {

    // 经验消耗量（级数）
    public static final int XP_LEVEL_COST = 3;
    // 触发后无敌时间（tick）- 1.5秒 = 30 tick
    public static final int INVINCIBILITY_TICKS = 30;
    // 冷却时间（秒）
    public static final int COOLDOWN_SECONDS = 30;
    // 经验获取加成
    public static final float XP_BONUS = 0.10f; // +10%
    // 负面效果：降低最大生命值
    private static final double HEALTH_REDUCTION = -4.0;

    // 属性修改器UUID
    private static final UUID VOID_GAZE_HEALTH_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String VOID_GAZE_HEALTH_NAME = "VoidGazeHealthReduction";

    public ItemVoidGaze() {
        this.setMaxStackSize(1);
        this.setTranslationKey("void_gaze");
        this.setRegistryName("void_gaze");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.BELT; // 腰带槽位，避免与其他七咒饰品冲突
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 必须佩戴七咒之戒才能装备
        if (!(player instanceof EntityPlayer))
            return false;

        EntityPlayer p = (EntityPlayer) player;
        return hasCursedRing(p);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;

        // 应用负面效果：降低最大生命值
        applyHealthReduction(p);

        // 装备时的提示
        p.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.DARK_PURPLE + "虚无之眸低语：" +
                TextFormatting.GRAY + "当你凝视深渊，深渊也在侵蚀你..."
        ));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;

        // 移除负面效果
        removeHealthReduction(p);
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 死亡保护逻辑由 CurseDeathHook (ASM) 处理
        // 确保负面效果持续存在
        if (entity.world.isRemote) return;
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;

        // 每5秒检查一次属性是否存在
        if (player.ticksExisted % 100 == 0) {
            IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            if (attr != null && attr.getModifier(VOID_GAZE_HEALTH_UUID) == null) {
                applyHealthReduction(player);
            }
        }
    }

    // ========== 负面效果 ==========

    /**
     * 应用生命值降低效果
     */
    private void applyHealthReduction(EntityPlayer player) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) return;

        // 先移除旧的
        if (attr.getModifier(VOID_GAZE_HEALTH_UUID) != null) {
            attr.removeModifier(VOID_GAZE_HEALTH_UUID);
        }

        // 添加新的
        AttributeModifier modifier = new AttributeModifier(
                VOID_GAZE_HEALTH_UUID,
                VOID_GAZE_HEALTH_NAME,
                HEALTH_REDUCTION,
                0  // Operation 0 = 加法
        );
        attr.applyModifier(modifier);

        // 如果当前血量超过最大血量，调整
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 移除生命值降低效果
     */
    private void removeHealthReduction(EntityPlayer player) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) return;

        if (attr.getModifier(VOID_GAZE_HEALTH_UUID) != null) {
            attr.removeModifier(VOID_GAZE_HEALTH_UUID);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴虚无之眸
     */
    public static boolean hasVoidGaze(EntityPlayer player) {
        return CurseDeathHook.hasVoidGaze(player);
    }

    /**
     * 检查玩家是否佩戴虚无之眸（供事件处理器调用）
     */
    public static boolean isWearing(EntityPlayer player) {
        return CurseDeathHook.hasVoidGaze(player);
    }

    /**
     * 获取经验加成倍率
     */
    public static float getXpBonus() {
        return XP_BONUS;
    }

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        return CurseDeathHook.hasCursedRing(player);
    }

    // ========== 物品属性 ==========

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
        list.add(TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD + "虚无之眸");
        list.add(TextFormatting.DARK_GRAY + "Eye of the Void");
        list.add("");
        list.add(TextFormatting.GRAY + "一颗漆黑的眼珠，仿佛能看透生死");

        // 如果玩家没有佩戴七咒之戒，显示装备条件
        if (player == null || !hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "⚠ 需要佩戴七咒之戒才能装备");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
            return;
        }

        // 正面效果
        list.add("");
        list.add(TextFormatting.GREEN + "▪ 正面效果");
        list.add(TextFormatting.GRAY + "  经验获取 " + TextFormatting.GREEN + "+" + (int)(XP_BONUS * 100) + "%");

        // 负面效果
        list.add("");
        list.add(TextFormatting.RED + "▪ 负面效果");
        list.add(TextFormatting.DARK_RED + "  装备时最大生命值 " + (int)HEALTH_REDUCTION);

        // 七咒联动效果
        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "◆ 七咒联动 - 深渊凝视");

        if (player != null && hasCursedRing(player)) {
            list.add(TextFormatting.LIGHT_PURPLE + "  ✓ 联动已激活");
        } else {
            list.add(TextFormatting.DARK_RED + "  ✗ 需要七咒之戒");
        }

        list.add("");
        list.add(TextFormatting.AQUA + "【死亡保护】");
        list.add(TextFormatting.GRAY + "  致命伤害时自动触发");
        list.add(TextFormatting.RED + "  消耗 " + TextFormatting.GREEN + XP_LEVEL_COST + " 级经验");
        list.add(TextFormatting.GRAY + "  恢复 " + TextFormatting.GREEN + "4" + TextFormatting.GRAY + " 点血量");
        list.add(TextFormatting.AQUA + "  获得 " + TextFormatting.WHITE + "1.5秒无敌");
        list.add(TextFormatting.YELLOW + "  冷却：" + COOLDOWN_SECONDS + "秒");

        // 显示冷却状态
        if (player != null && hasVoidGaze(player)) {
            list.add("");
            if (CurseDeathHook.isOnCooldown(player)) {
                int remaining = CurseDeathHook.getRemainingCooldown(player);
                list.add(TextFormatting.RED + "⏳ 冷却中... " + remaining + "秒");
            } else {
                list.add(TextFormatting.GREEN + "✓ 已就绪");
            }
            list.add(TextFormatting.GRAY + "当前经验等级：" +
                    (player.experienceLevel >= XP_LEVEL_COST ?
                            TextFormatting.GREEN : TextFormatting.RED) +
                    player.experienceLevel);
        }

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"凝视深渊的代价，是被深渊侵蚀\"");
        list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
    }
}
