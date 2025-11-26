package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 异界纤维 - 视觉效果和状态更新Mixin
 */
@Mixin(EntityPlayer.class)
public class MixinOtherworldVisuals {

    /**
     * 玩家更新时处理异界效果
     * 使用SRG名称 func_70071_h_ 对应 onUpdate
     */
    @Inject(
            method = "func_70071_h_",  // onUpdate的SRG名
            at = @At("HEAD")
    )
    private void otherworld_visualEffects(CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer)(Object)this;
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount > 0 && !player.world.isRemote) {
            // 获取异界数据
            NBTTagCompound otherworldData = null;
            ItemStack targetArmor = null;

            for (ItemStack armor : player.getArmorInventoryList()) {
                if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                    otherworldData = FabricWeavingSystem.getFabricData(armor);
                    targetArmor = armor;
                    break;
                }
            }

            if (otherworldData != null && targetArmor != null) {
                int sanity = otherworldData.getInteger("Sanity");
                int insight = otherworldData.getInteger("Insight");

                // 每秒更新状态（20 ticks = 1秒）
                if (player.ticksExisted % 20 == 0) {
                    updateOtherworldStatus(player, targetArmor, otherworldData, otherworldCount);
                }

                // 理智低时的幻觉效果（每5秒触发）
                if (sanity < 30 && player.ticksExisted % 100 == 0) {
                    applyHallucinationEffects(player, sanity);
                }

                // 高灵视时的特殊效果（每10秒触发）
                if (insight > 80 && player.ticksExisted % 200 == 0) {
                    applyInsightEffects(player, insight);
                }

                // 显示状态栏（每2秒）
                if (player.ticksExisted % 40 == 0) {
                    showStatusBar(player, sanity, insight);
                }
            }
        }
    }

    /**
     * 更新异界状态
     */
    private void updateOtherworldStatus(EntityPlayer player, ItemStack armor, NBTTagCompound data, int count) {
        int insight = data.getInteger("Insight");
        int sanity = data.getInteger("Sanity");

        // 穿戴异界纤维会缓慢增加灵视，降低理智
        data.setInteger("Insight", Math.min(100, insight + count));
        data.setInteger("Sanity", Math.max(0, sanity - count));

        FabricWeavingSystem.updateFabricData(armor, data);
    }

    /**
     * 应用幻觉效果
     */
    private void applyHallucinationEffects(EntityPlayer player, int sanity) {
        // 随机传送视角
        float randomYaw = player.rotationYaw + (player.world.rand.nextFloat() - 0.5f) * 60;
        float randomPitch = player.rotationPitch + (player.world.rand.nextFloat() - 0.5f) * 30;
        player.setPositionAndRotation(player.posX, player.posY, player.posZ, randomYaw, randomPitch);

        // 发送幻觉消息
        String[] whispers = {
                "§k他们在看着你...",
                "§k不要相信你的眼睛...",
                "§k醒来...醒来...",
                "§k真相就在眼前...",
                "§k你不是真实的...",
                "§k他们知道了...",
                "§k时间在倒流...",
                "§k墙壁在呼吸..."
        };

        String message = whispers[player.world.rand.nextInt(whispers.length)];
        player.sendMessage(new TextComponentString(message));

        // 理智极低时应用负面效果
        if (sanity < 10) {
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:nausea"), 100, 0));
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:blindness"), 60, 0));
        } else if (sanity < 20) {
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:nausea"), 60, 0));
        }
    }

    /**
     * 应用高灵视效果
     */
    private void applyInsightEffects(EntityPlayer player, int insight) {
        // 高灵视消息
        player.sendMessage(new TextComponentString("§d[灵视] 你感受到了异界的呼唤..."));

        // 灵视越高，获得的增益越强
        if (insight >= 90) {
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:night_vision"), 400, 0));
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:speed"), 200, 1));
            player.sendMessage(new TextComponentString("§d禁忌的知识流入你的意识..."));
        } else {
            player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("minecraft:night_vision"), 200, 0));
        }
    }

    /**
     * 显示状态栏
     */
    private void showStatusBar(EntityPlayer player, int sanity, int insight) {
        String statusColor;
        String statusText;

        if (sanity > 70) {
            statusColor = TextFormatting.GREEN.toString();
            statusText = "稳定";
        } else if (sanity > 40) {
            statusColor = TextFormatting.YELLOW.toString();
            statusText = "不安";
        } else if (sanity > 20) {
            statusColor = TextFormatting.RED.toString();
            statusText = "混乱";
        } else {
            statusColor = TextFormatting.DARK_PURPLE.toString();
            statusText = "疯狂";
        }

        String message = String.format("%s[理智: %d%%] §d[灵视: %d%%] %s状态: %s",
                statusColor, sanity, insight, statusColor, statusText);

        player.sendStatusMessage(new TextComponentString(message), true);
    }
}