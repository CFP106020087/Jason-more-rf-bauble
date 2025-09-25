package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 异界纤维 - 经验系统Mixin (仅修改经验值)
 */
@Mixin(EntityPlayer.class)
public class MixinOtherworldExperience {

    /**
     * 修改经验获取 - 异界布料增加经验倍率
     * 注意：由于目标方法不存在，我们改为注入onUpdate方法并手动处理
     */
    @ModifyVariable(
            method = "func_195068_e",  // addExperience的SRG名
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int otherworld_modifyExperience(int amount) {
        EntityPlayer player = (EntityPlayer)(Object)this;
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0) {
            // 获取异界数据
            NBTTagCompound otherworldData = getOtherworldData(player);
            int insight = otherworldData.getInteger("Insight");

            // 基础倍率：每件异界纤维经验翻倍
            int multiplier = 1 + otherworldCount;

            // 灵视加成：每25灵视额外+50%经验
            if (insight >= 25) {
                multiplier = (int)(multiplier * (1 + (insight / 25) * 0.5f));
            }

            return amount * multiplier;
        }

        return amount;
    }

    /**
     * 修改经验等级增加
     */
    @ModifyVariable(
            method = "func_82242_a",  // addExperienceLevel的SRG名
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int otherworld_modifyExperienceLevel(int levels) {
        EntityPlayer player = (EntityPlayer)(Object)this;
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0) {
            // 等级增加也受到加成
            return levels * (1 + otherworldCount);
        }
        return levels;
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