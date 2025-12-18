package com.moremod.mixin;

import com.moremod.item.ItemAstralPickaxe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin: 让星芒镐能够挖掘 AS 的不可破坏水晶
 */
@Mixin(Block.class)
public abstract class MixinBlockAstralCrystal {
    
    private static final float CRYSTAL_HARDNESS = 4.0F;
    
    @Inject(
        method = "getPlayerRelativeBlockHardness",
        at = @At("HEAD"),
        cancellable = true
    )
    private void moremod$onGetPlayerRelativeBlockHardness(
            IBlockState state, EntityPlayer player, World world, BlockPos pos,
            CallbackInfoReturnable<Float> cir) {
        
        // 检查玩家是否手持星芒镐
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !(held.getItem() instanceof ItemAstralPickaxe)) {
            return;
        }
        
        // 检查方块是否是 AS 的水晶类方块（通过类名避免硬依赖）
        Block block = state.getBlock();
        String className = block.getClass().getName();
        
        boolean isAstralCrystal = className.contains("BlockCollectorCrystal") ||
                                  className.contains("BlockCelestialCrystals") ||
                                  className.contains("BlockCelestialCollectorCrystal");
        
        if (isAstralCrystal) {
            // 返回可挖掘的硬度值
            int divisor = ForgeHooks.canHarvestBlock(block, player, world, pos) ? 30 : 100;
            float speed = player.getDigSpeed(state, pos);
            cir.setReturnValue(speed / CRYSTAL_HARDNESS / divisor);
        }
    }
}