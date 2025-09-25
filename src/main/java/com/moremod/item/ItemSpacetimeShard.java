package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时空碎片
 * 从时空碎片矿石中获得的材料
 */
public class ItemSpacetimeShard extends Item {

    public ItemSpacetimeShard() {
        setRegistryName("spacetime_shard");
        setTranslationKey("spacetime_shard");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(64);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        // 偶尔产生粒子效果
        if (world.isRemote && world.rand.nextInt(20) == 0) {
            double x = entity.posX + (world.rand.nextDouble() - 0.5);
            double y = entity.posY + world.rand.nextDouble();
            double z = entity.posZ + (world.rand.nextDouble() - 0.5);

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL,
                    x, y, z, 0, 0, 0);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "蕴含时空之力的神秘碎片");
        tooltip.add(TextFormatting.GRAY + "来自维度虚空的稀有材料");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "用途：");
        tooltip.add(TextFormatting.WHITE + "- 制作高级维度道具");
        tooltip.add(TextFormatting.WHITE + "- 强化传送装置");
        tooltip.add(TextFormatting.WHITE + "- 时空科技的核心材料");
    }
}