package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityProtectionField;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 保护领域发生器 - 消耗RF能量产生保护领域
 * 攻击或开箱会导致10秒保护失效
 * moremod Mod
 */
public class ProtectionFieldGenerator extends Block implements ITileEntityProvider {

    public static final ProtectionFieldGenerator INSTANCE = new ProtectionFieldGenerator();

    public ProtectionFieldGenerator() {
        super(Material.IRON);
        this.setHardness(5.0F);
        this.setResistance(10.0F);
        this.setTranslationKey("protection_field_generator");
        this.setRegistryName("moremod", "protection_field_generator");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        this.setLightLevel(0.5F);  // 默认发光等级
    }

    @Override
    public int getLightValue(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityProtectionField) {
            TileEntityProtectionField generator = (TileEntityProtectionField) te;
            // 激活时发出更强的光
            return generator.isActive() ? 15 : 7;
        }
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityProtectionField();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityProtectionField) {
                TileEntityProtectionField generator = (TileEntityProtectionField) te;

                // 显示状态信息
                int energy = generator.getEnergyStored();
                int maxEnergy = generator.getMaxEnergyStored();
                boolean active = generator.isActive();
                int range = generator.getRange();

                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "=== 保护领域发生器 ===" + TextFormatting.RESET));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "能量: " + TextFormatting.WHITE +
                                String.format("%,d / %,d RF", energy, maxEnergy)));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "状态: " +
                                (active ? TextFormatting.GREEN + "激活" : TextFormatting.RED + "未激活")));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "保护范围: " + TextFormatting.WHITE + range + " 格"));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "能量消耗: " + generator.getEnergyPerTick() + " RF/tick"));

                // 检查玩家是否在冷却中
                int cooldown = TileEntityProtectionField.getRemainingCooldown(playerIn);
                if (cooldown > 0) {
                    playerIn.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 保护失效中: " + TextFormatting.WHITE + cooldown + " 秒"));
                } else if (generator.isPlayerInRange(playerIn) && active) {
                    playerIn.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ 你正在受到保护"));
                }

                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + "提示: 攻击或开箱会暂时失效10秒"));
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "消耗RF能量产生保护领域");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "功能:");
        tooltip.add(TextFormatting.GRAY + "  • 领域内玩家不会被敌对生物发现");
        tooltip.add(TextFormatting.GRAY + "  • 敌对生物无法锁定领域内玩家");
        tooltip.add(TextFormatting.GRAY + "  • 粒子效果标示保护范围");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "注意:");
        tooltip.add(TextFormatting.GRAY + "  • 攻击任何实体将暂停保护10秒");
        tooltip.add(TextFormatting.GRAY + "  • 打开箱子等容器将暂停保护10秒");
        tooltip.add(TextFormatting.GRAY + "  • 冷却期间粒子变为红色警告");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "能量存储: " + TextFormatting.WHITE + "1,000,000 RF");
        tooltip.add(TextFormatting.YELLOW + "消耗: " + TextFormatting.WHITE + "100 RF/tick");
        tooltip.add(TextFormatting.YELLOW + "默认范围: " + TextFormatting.WHITE + "16 格");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "右键点击查看状态");
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityProtectionField) {
            TileEntityProtectionField generator = (TileEntityProtectionField) te;
            float ratio = (float) generator.getEnergyStored() / generator.getMaxEnergyStored();
            return (int) (ratio * 15);
        }
        return 0;
    }
}