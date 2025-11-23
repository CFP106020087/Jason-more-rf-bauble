package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 时空碎片矿石
 * 在私人维度虚空中生成的特殊矿石
 */
public class BlockSpacetimeShard extends Block {

    public BlockSpacetimeShard() {
        super(Material.ROCK);
        this.setRegistryName("moremod", "spacetime_shard_ore");
        this.setTranslationKey("spacetime_shard_ore");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2); // 需要铁镐
        setSoundType(SoundType.GLASS);
        setLightLevel(0.7F); // 发光
    }

    @Override
    public int quantityDropped(Random random) {
        return 1 + random.nextInt(3); // 掉落1-3个
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        // 返回时空碎片物品
        return Item.getByNameOrId("moremod:spacetime_shard");
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune > 0) {
            int bonus = random.nextInt(fortune + 2) - 1;
            if (bonus < 0) bonus = 0;
            return this.quantityDropped(random) * (bonus + 1);
        }
        return this.quantityDropped(random);
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random rand = world instanceof World ? ((World)world).rand : new Random();
        return MathHelper.getInt(rand, 3, 7);
    }


    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(3) == 0) {
            double x = pos.getX() + 0.5D + (rand.nextDouble() - 0.5D);
            double y = pos.getY() + 0.5D + (rand.nextDouble() - 0.5D);
            double z = pos.getZ() + 0.5D + (rand.nextDouble() - 0.5D);

            // 时空粒子效果
            world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z,
                    (rand.nextDouble() - 0.5D) * 0.5D,
                    (rand.nextDouble() - 0.5D) * 0.5D,
                    (rand.nextDouble() - 0.5D) * 0.5D);

            if (rand.nextInt(5) == 0) {
                world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "蕴含时空之力的神秘矿石");
        tooltip.add(TextFormatting.GRAY + "只存在于维度虚空中");
        tooltip.add(TextFormatting.AQUA + "需要铁镐或更好的工具");
    }
}