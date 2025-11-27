package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 异界纤维 - 时运增强Mixin
 */
@Mixin(Block.class)
public abstract class MixinOtherworldFortune {

    @Shadow
    public abstract void dropBlockAsItem(World worldIn, BlockPos pos, IBlockState state, int fortune);

    /**
     * 修改方块收获时的时运效果
     * 使用SRG名称 func_180657_a 对应 harvestBlock
     */
    @Inject(
            method = "func_180657_a",  // harvestBlock的SRG名
            at = @At("HEAD"),
            cancellable = true
    )
    private void otherworld_modifyFortune(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state,
                                          TileEntity te, ItemStack stack, CallbackInfo ci) {
        // 获取基础时运等级
        int baseFortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);

        // 检查异界布料数量
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0) {
            // 获取异界数据
            NBTTagCompound otherworldData = getOtherworldData(player);
            int insight = otherworldData.getInteger("Insight");
            int sanity = otherworldData.getInteger("Sanity");

            // 每件异界纤维+3时运
            int bonusFortune = otherworldCount * 3;

            // 灵视加成（每20灵视+1时运）
            bonusFortune += insight / 20;

            // 理智惩罚：理智过低时时运可能失效
            if (sanity < 20) {
                // 20%概率时运失效
                if (worldIn.rand.nextFloat() < 0.2f) {
                    bonusFortune = 0;
                    player.sendStatusMessage(new TextComponentString(
                            "§5异界的混乱干扰了你的时运..."), true);
                }
            }

            baseFortune += bonusFortune;
        }

        // 使用增强后的时运值掉落物品
        this.dropBlockAsItem(worldIn, pos, state, baseFortune);

        // 取消原始方法的继续执行
        ci.cancel();
    }

    /**
     * 获取玩家的异界布料数据
     */
    private NBTTagCompound getOtherworldData(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                return FabricWeavingSystem.getFabricData(armor);
            }
        }
        NBTTagCompound defaultData = new NBTTagCompound();
        defaultData.setInteger("Insight", 0);
        defaultData.setInteger("Sanity", 100);
        return defaultData;
    }
}