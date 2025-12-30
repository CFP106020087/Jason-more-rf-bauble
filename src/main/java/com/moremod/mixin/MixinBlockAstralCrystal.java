package com.moremod.mixin;

import com.moremod.item.ItemAstralPickaxe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin: 让星芒镐能够挖掘 AS 的不可破坏水晶
 * 使用反射避免硬依赖
 */
@Mixin(Block.class)
public abstract class MixinBlockAstralCrystal {

    @Unique
    private static final float CRYSTAL_HARDNESS = 4.0F;

    // ===== 反射缓存 =====
    @Unique
    private static final Map<Class<?>, Boolean> moremod$classCache = new ConcurrentHashMap<>();

    @Unique
    private static final Map<Class<?>, Method> moremod$isPlayerMadeCache = new ConcurrentHashMap<>();

    @Unique
    private static Boolean moremod$asLoaded = null;

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

        // 检查 AS 是否加载
        if (!moremod$isAstralSorceryLoaded()) {
            return;
        }

        // 检查方块是否是 AS 的水晶类方块
        Block block = state.getBlock();
        if (!moremod$isAstralCrystalBlock(block)) {
            return;
        }

        // 检查是否是自然生成的水晶（需要特殊处理）
        boolean needsOverride = moremod$isNaturalCrystal(world, pos);

        if (needsOverride) {
            // 星芒镐可以挖掘自然生成的水晶
            float speed = player.getDigSpeed(state, pos);
            int divisor = 30;
            cir.setReturnValue(speed / CRYSTAL_HARDNESS / divisor);
        }
    }

    /**
     * 检查 AS 是否加载
     */
    @Unique
    private static boolean moremod$isAstralSorceryLoaded() {
        if (moremod$asLoaded == null) {
            try {
                Class.forName("hellfirepvp.astralsorcery.AstralSorcery");
                moremod$asLoaded = true;
            } catch (ClassNotFoundException e) {
                moremod$asLoaded = false;
            }
        }
        return moremod$asLoaded;
    }

    /**
     * 检查方块是否是 AS 水晶类（带缓存）
     */
    @Unique
    private static boolean moremod$isAstralCrystalBlock(Block block) {
        Class<?> blockClass = block.getClass();

        return moremod$classCache.computeIfAbsent(blockClass, clazz -> {
            // 遍历类继承链检查
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                String name = current.getName();
                if (name.contains("BlockCollectorCrystal") ||
                        name.contains("BlockCelestialCrystal") ||
                        name.contains("BlockCelestialCollectorCrystal")) {
                    return true;
                }
                current = current.getSuperclass();
            }
            return false;
        });
    }

    /**
     * 检查是否是自然生成的水晶（非玩家放置）
     */
    @Unique
    private static boolean moremod$isNaturalCrystal(World world, BlockPos pos) {
        try {
            TileEntity te = world.getTileEntity(pos);
            if (te == null) {
                return true; // 无 TE，视为可挖掘
            }

            Class<?> teClass = te.getClass();

            // 查找并缓存 isPlayerMade 方法
            Method isPlayerMade = moremod$isPlayerMadeCache.computeIfAbsent(teClass, clazz -> {
                // 遍历类继承链查找方法
                Class<?> current = clazz;
                while (current != null && current != Object.class) {
                    try {
                        Method m = current.getDeclaredMethod("isPlayerMade");
                        m.setAccessible(true);
                        return m;
                    } catch (NoSuchMethodException ignored) {}

                    // 也尝试 getPlayerMade
                    try {
                        Method m = current.getDeclaredMethod("getPlayerMade");
                        m.setAccessible(true);
                        return m;
                    } catch (NoSuchMethodException ignored) {}

                    current = current.getSuperclass();
                }
                return null;
            });

            if (isPlayerMade != null) {
                Object result = isPlayerMade.invoke(te);
                if (result instanceof Boolean) {
                    // 如果是玩家放置的，不需要我们干预（原版逻辑已允许）
                    // 如果不是玩家放置的，需要我们干预
                    return !(Boolean) result;
                }
            }

            // 找不到方法，默认允许挖掘
            return true;

        } catch (Exception e) {
            // 反射失败，默认允许挖掘
            return true;
        }
    }
}