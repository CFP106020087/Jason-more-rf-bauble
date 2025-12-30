package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 附魔增强方块 - 通过祭坛仪式创建
 * 放置在附魔台附近可以增强附魔等级上限
 */
public class BlockEnchantingBooster extends Block {

    public static final PropertyEnum<BoosterType> TYPE = PropertyEnum.create("type", BoosterType.class);

    public BlockEnchantingBooster() {
        super(Material.ROCK);
        setRegistryName("moremod", "enchanting_booster");
        setTranslationKey("moremod.enchanting_booster");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(3.0F);
        setResistance(15.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(TYPE, BoosterType.ARCANE_STONE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        // 确保meta值在有效范围内，防止无效状态
        if (meta < 0 || meta >= BoosterType.values().length) {
            meta = 0;
        }
        return getDefaultState().withProperty(TYPE, BoosterType.byMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMeta();
    }

    /**
     * 确保从世界获取的状态是正确的
     * 防止区块加载时状态不一致导致贴图消失
     */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        // 状态完全由meta决定，无需额外处理，但显式返回确保一致性
        return state;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (BoosterType type : BoosterType.values()) {
            items.add(new ItemStack(this, 1, type.getMeta()));
        }
    }

    /**
     * 提供附魔增益 - 类似书架
     */
    @Override
    public float getEnchantPowerBonus(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) return 0;
        return state.getValue(TYPE).getEnchantBonus();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            BoosterType type = state.getValue(TYPE);
            playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.AQUA + "附魔增益: +" + TextFormatting.GOLD + type.getEnchantBonus() +
                TextFormatting.GRAY + " (等效于 " + (type.getEnchantBonus() / 1.0f) + " 个书架)"
            ));
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        BoosterType type = BoosterType.byMeta(stack.getMetadata());
        tooltip.add(TextFormatting.AQUA + "附魔增益: " + TextFormatting.GOLD + "+" + type.getEnchantBonus());
        tooltip.add(TextFormatting.GRAY + "等效于 " + (type.getEnchantBonus() / 1.0f) + " 个书架");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + type.getDescription());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
        BoosterType type = state.getValue(TYPE);

        // 高级方块显示粒子效果
        if (type.getEnchantBonus() >= 2.0f && rand.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;

            worldIn.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, y, z, 0, 0.2, 0);
        }

        // 最高级方块显示更多粒子
        if (type == BoosterType.SOUL_LIBRARY && rand.nextInt(3) == 0) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();

            worldIn.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        BoosterType type = state.getValue(TYPE);
        return type.getLightLevel();
    }

    /**
     * 使用相邻方块亮度 - 对于发光方块返回true可以改善渲染
     * 这有助于防止光照更新时贴图消失的问题
     */
    @Override
    public boolean getUseNeighborBrightness(IBlockState state) {
        return state.getValue(TYPE).getLightLevel() > 0;
    }

    /**
     * 附魔增强方块类型
     */
    public enum BoosterType implements IStringSerializable {
        ARCANE_STONE(0, "arcane_stone", 1.0f, 0, "蕴含微弱魔力的石头"),
        ENCHANTED_BOOKSHELF(1, "enchanted_bookshelf", 2.0f, 3, "经过仪式强化的书架"),
        KNOWLEDGE_CRYSTAL(2, "knowledge_crystal", 3.0f, 7, "凝聚了无数知识的水晶"),
        SOUL_LIBRARY(3, "soul_library", 5.0f, 11, "封印了学者灵魂的图书馆碎片");

        private final int meta;
        private final String name;
        private final float enchantBonus;
        private final int lightLevel;
        private final String description;

        BoosterType(int meta, String name, float enchantBonus, int lightLevel, String description) {
            this.meta = meta;
            this.name = name;
            this.enchantBonus = enchantBonus;
            this.lightLevel = lightLevel;
            this.description = description;
        }

        public int getMeta() {
            return meta;
        }

        @Override
        public String getName() {
            return name;
        }

        public float getEnchantBonus() {
            return enchantBonus;
        }

        public int getLightLevel() {
            return lightLevel;
        }

        public String getDescription() {
            return description;
        }

        public static BoosterType byMeta(int meta) {
            if (meta < 0 || meta >= values().length) {
                meta = 0;
            }
            return values()[meta];
        }
    }
}
