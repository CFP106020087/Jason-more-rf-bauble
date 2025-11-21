package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 异界纤维 - 经验球Mixin
 */
@Mixin(EntityXPOrb.class)
public class MixinOtherworldXPOrb {

    @Shadow
    private int xpValue;

    /**
     * 修改经验球的经验值
     * 使用SRG名称 func_70100_b_ 对应 onCollideWithPlayer
     */
    @ModifyVariable(
            method = "func_70100_b_",  // onCollideWithPlayer的SRG名
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private EntityPlayer otherworld_modifyXPValue(EntityPlayer player) {
        if (player.world.isRemote) {
            return player;
        }

        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0) {
            // 获取异界数据
            NBTTagCompound otherworldData = getOtherworldData(player);
            int insight = otherworldData.getInteger("Insight");
            int sanity = otherworldData.getInteger("Sanity");

            // 基础加成：每件装备额外100%
            int bonusMultiplier = otherworldCount;

            // 灵视额外加成
            if (insight > 50) {
                bonusMultiplier += insight / 50;
            }

            // 理智过低时的负面效果
            if (sanity < 30 && player.world.rand.nextFloat() < 0.1f) {
                player.sendStatusMessage(new TextComponentString(
                        "§5知识的代价正在侵蚀你的心智..."), true);
                updateOtherworldSanity(player, -1);
            }

            // 高灵视时的提示
            if (insight > 80 && player.world.rand.nextFloat() < 0.05f) {
                player.sendStatusMessage(new TextComponentString(
                        "§d禁忌的知识流入你的意识..."), true);
            }

            // 修改经验值（防止整数溢出）
            long newValue = (long) this.xpValue * (1 + bonusMultiplier);
            if (newValue > Integer.MAX_VALUE) {
                this.xpValue = Integer.MAX_VALUE;
            } else if (newValue < Integer.MIN_VALUE) {
                this.xpValue = Integer.MIN_VALUE;
            } else {
                this.xpValue = (int) newValue;
            }
        }

        return player;
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

    /**
     * 更新异界理智值
     */
    private void updateOtherworldSanity(EntityPlayer player, int change) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                NBTTagCompound data = FabricWeavingSystem.getFabricData(armor);
                int sanity = data.getInteger("Sanity");
                data.setInteger("Sanity", Math.max(0, Math.min(100, sanity + change)));
                FabricWeavingSystem.updateFabricData(armor, data);
                break;
            }
        }
    }
}