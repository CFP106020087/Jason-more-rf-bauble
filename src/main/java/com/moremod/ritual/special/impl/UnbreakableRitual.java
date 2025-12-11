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
 * 不可破坏仪式
 * 使物品获得永不损坏属性
 *
 * 配方：
 * - 中心：任意有耐久的物品
 * - 基座：地狱之星×2 + 黑曜石×2 + 钻石×4
 *
 * 效果：
 * - 为物品添加 Unbreakable NBT 标签
 * - 物品将永不损坏
 *
 * 参数：
 * - 阶层：3（大师祭坛）
 * - 时间：400 tick (20秒)
 * - 失败率：20%
 * - 失败惩罚：物品损失50%耐久
 */
public class UnbreakableRitual extends AbstractSpecialRitual {

    public static final String ID = "unbreakable";

    public UnbreakableRitual() {
        super(
                3,       // 三阶祭坛
                400,     // 20秒
                0.20f,   // 20%失败率
                300000   // 300k RF/基座
        );

        // 定义基座材料要求
        addPedestalRequirement(Items.NETHER_STAR, 2);
        addPedestalRequirement(new ItemStack(Blocks.OBSIDIAN), 2);
        addPedestalRequirement(Items.DIAMOND, 4);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "不可破坏";
    }

    @Override
    public String getDescription() {
        return "使物品永不损坏";
    }

    @Override
    public boolean isValidCenterItem(ItemStack centerItem) {
        if (centerItem.isEmpty()) {
            return false;
        }

        // 必须是有耐久度的物品
        if (!centerItem.isItemStackDamageable()) {
            return false;
        }

        // 已经是不可破坏的物品不能再次处理
        if (centerItem.hasTagCompound()) {
            NBTTagCompound nbt = centerItem.getTagCompound();
            if (nbt.getBoolean("Unbreakable")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onStart(RitualContext context) {
        super.onStart(context);

        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "永恒之力正在注入物品..."));
        }
    }

    @Override
    public void onTick(RitualContext context, int progress) {
        super.onTick(context, progress);

        // 不可破坏特有的金色粒子效果
        if (progress % 8 == 0 && context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            double x = context.corePos.getX() + 0.5;
            double y = context.corePos.getY() + 1.5;
            double z = context.corePos.getZ() + 0.5;

            // 金色粒子环绕
            for (int i = 0; i < 4; i++) {
                double angle = (progress / 8.0 + i * 0.25) * Math.PI * 2;
                double radius = 0.8;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;

                ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        px, y, pz, 1,
                        0, 0.05, 0, 0.01);
            }
        }
    }

    @Override
    public ItemStack onComplete(RitualContext context) {
        ItemStack result = context.centerItem.copy();

        // 添加 Unbreakable NBT 标签
        NBTTagCompound nbt = result.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            result.setTagCompound(nbt);
        }

        nbt.setBoolean("Unbreakable", true);

        // 修复耐久（可选）
        result.setItemDamage(0);

        // 成功特效
        playSuccessSound(context.world, context.corePos);
        spawnSuccessParticles(context.world, context.corePos);

        // 额外的金色粒子爆发
        if (context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                    context.corePos.getX() + 0.5,
                    context.corePos.getY() + 1.5,
                    context.corePos.getZ() + 0.5,
                    50, 0.5, 0.5, 0.5, 0.1);
        }

        // 发送成功消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✦ " +
                            TextFormatting.WHITE + result.getDisplayName() +
                            TextFormatting.GOLD + " 获得了永恒之力！"));
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "此物品将永不损坏"));
        }

        return result;
    }

    @Override
    public ItemStack onFail(RitualContext context) {
        ItemStack result = context.centerItem.copy();

        // 失败惩罚：损失50%耐久
        int maxDamage = result.getMaxDamage();
        int currentDamage = result.getItemDamage();
        int damageToAdd = maxDamage / 2;
        int newDamage = Math.min(maxDamage - 1, currentDamage + damageToAdd);
        result.setItemDamage(newDamage);

        // 失败音效
        context.world.playSound(null, context.corePos,
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS,
                1.0f, 0.5f);

        // 失败粒子
        if (context.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) context.world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    context.corePos.getX() + 0.5,
                    context.corePos.getY() + 1.5,
                    context.corePos.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.05);
        }

        // 发送失败消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 仪式失败！物品损失了 50% 耐久"));
        }

        return result;
    }

    @Override
    public int getParticleColor() {
        return 0xFFD700; // 金色
    }
}
