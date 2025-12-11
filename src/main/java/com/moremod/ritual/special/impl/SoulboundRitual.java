package com.moremod.ritual.special.impl;

import com.moremod.ritual.special.AbstractSpecialRitual;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/**
 * 灵魂束缚仪式
 * 使物品获得死亡不掉落属性
 *
 * 配方：
 * - 中心：任意物品
 * - 基座：末影珍珠×4 + 恶魂之泪×2 + 金块×2
 *
 * 效果：
 * - 为物品添加 Soulbound NBT 标签
 * - 死亡时物品不会掉落
 *
 * 参数：
 * - 阶层：3（大师祭坛）
 * - 时间：300 tick (15秒)
 * - 失败率：10%
 * - 失败惩罚：物品被虚空吞噬
 */
public class SoulboundRitual extends AbstractSpecialRitual {

    public static final String ID = "soulbound";

    public SoulboundRitual() {
        super(
                3,       // 三阶祭坛
                300,     // 15秒
                0.10f,   // 10%失败率
                200000   // 200k RF/基座
        );

        // 定义基座材料要求
        addPedestalRequirement(Items.ENDER_PEARL, 4);
        addPedestalRequirement(Items.GHAST_TEAR, 2);
        addPedestalRequirement(new ItemStack(Blocks.GOLD_BLOCK), 2);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "灵魂束缚";
    }

    @Override
    public String getDescription() {
        return "使物品获得死亡不掉落属性";
    }

    @Override
    public boolean isValidCenterItem(ItemStack centerItem) {
        // 接受任何非空物品
        if (centerItem.isEmpty()) {
            return false;
        }

        // 已经有 Soulbound 标签的物品不能再次处理
        if (centerItem.hasTagCompound()) {
            NBTTagCompound nbt = centerItem.getTagCompound();
            if (nbt.getBoolean("Soulbound") || nbt.getBoolean("soulbound")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onStart(RitualContext context) {
        super.onStart(context);

        // 额外的开始特效
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "灵魂之力正在与物品建立连接..."));
        }
    }

    @Override
    public void onTick(RitualContext context, int progress) {
        super.onTick(context, progress);

        // 灵魂束缚特有的粒子效果
        if (progress % 10 == 0 && context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            double x = context.corePos.getX() + 0.5;
            double y = context.corePos.getY() + 1.5;
            double z = context.corePos.getZ() + 0.5;

            // 紫色灵魂粒子螺旋上升
            double angle = (progress / 10.0) * Math.PI;
            double radius = 0.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + (progress / (float) getDuration()) * 2.0;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    px, py, pz, 5,
                    0.1, 0.1, 0.1, 0.02);
        }
    }

    @Override
    public ItemStack onComplete(RitualContext context) {
        ItemStack result = context.centerItem.copy();

        // 添加 Soulbound NBT 标签
        NBTTagCompound nbt = result.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            result.setTagCompound(nbt);
        }

        nbt.setBoolean("Soulbound", true);

        // 可选：记录绑定者信息
        if (context.player != null) {
            nbt.setString("SoulboundOwner", context.player.getName());
            nbt.setString("SoulboundOwnerUUID", context.player.getUniqueID().toString());
        }

        // 成功特效
        playSuccessSound(context.world, context.corePos);
        spawnSuccessParticles(context.world, context.corePos);

        // 额外的紫色粒子
        if (context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    context.corePos.getX() + 0.5,
                    context.corePos.getY() + 1.5,
                    context.corePos.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.05);
        }

        // 发送成功消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✦ " +
                            TextFormatting.WHITE + result.getDisplayName() +
                            TextFormatting.LIGHT_PURPLE + " 已与你的灵魂绑定！"));
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "死亡时此物品将不会掉落"));
        }

        return result;
    }

    @Override
    public ItemStack onFail(RitualContext context) {
        // 失败时物品被虚空吞噬
        context.world.playSound(null, context.corePos,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS,
                1.0f, 0.5f);

        // 虚空吞噬粒子
        if (context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    context.corePos.getX() + 0.5,
                    context.corePos.getY() + 1.5,
                    context.corePos.getZ() + 0.5,
                    100, 0.5, 0.5, 0.5, 0.5);
        }

        // 发送失败消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "✗ 仪式失败！" +
                            TextFormatting.RED + context.centerItem.getDisplayName() +
                            TextFormatting.DARK_RED + " 被虚空吞噬了..."));
        }

        // 返回空物品（物品被消耗）
        return ItemStack.EMPTY;
    }

    @Override
    public int getParticleColor() {
        return 0x9933FF; // 紫色
    }
}
