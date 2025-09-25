package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 异界纤维 - 掠夺增强Mixin
 */
@Mixin(EnchantmentHelper.class)
public class MixinOtherworldLooting {

    /**
     * 修改掠夺等级 - 异界纤维提供额外掠夺
     * 使用SRG名称 func_185283_h 对应 getLootingModifier
     */
    @Inject(
            method = "func_185283_h",  // getLootingModifier的SRG名
            at = @At("RETURN"),
            cancellable = true
    )
    private static void otherworld_getLootingLevel(EntityLivingBase living, CallbackInfoReturnable<Integer> cir) {
        int baseLoot = cir.getReturnValue();

        if (living instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) living;
            int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

            if (otherworldCount > 0) {
                // 每件异界纤维+5掠夺等级
                int bonusLooting = otherworldCount * 5;

                // 从装备NBT获取灵视和理智数据
                int totalInsight = 0;
                int totalSanity = 0;

                for (ItemStack armor : player.getArmorInventoryList()) {
                    if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                        NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(armor);
                        totalInsight += fabricData.getInteger("Insight");
                        totalSanity += fabricData.getInteger("Sanity");
                    }
                }

                // 平均值
                int avgInsight = totalInsight / Math.max(1, otherworldCount);
                int avgSanity = totalSanity / Math.max(1, otherworldCount);

                // 根据灵视值额外增加
                bonusLooting += avgInsight / 10; // 每10灵视+1掠夺

                // 疯狂加成：理智越低，掠夺越高
                if (avgSanity < 50) {
                    bonusLooting += (50 - avgSanity) / 5; // 最多额外+10
                }

                cir.setReturnValue(baseLoot + bonusLooting);
            }
        }
    }
}