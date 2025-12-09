package com.moremod.item.ritual;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 命运苹果 (Fate Apple)
 *
 * 食用后重置玩家的附魔种子，改变附魔台的附魔结果
 * 灵感来自 ExtraUtils2 的 Magic Apple
 */
public class ItemFateApple extends ItemFood {

    public ItemFateApple() {
        super(4, 0.5F, false);
        setTranslationKey("moremod.fate_apple");
        setRegistryName("fate_apple");
        setAlwaysEdible(); // 可以在饱食度满时食用
        setMaxStackSize(16);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 20; // 1秒食用时间
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.EAT;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;

            if (!worldIn.isRemote) {
                // 使用反射重置附魔种子
                try {
                    // xpSeed 在混淆环境中的名称是 "field_175152_f"
                    Field xpSeedField = ObfuscationReflectionHelper.findField(EntityPlayer.class, "field_175152_f");
                    int oldSeed = xpSeedField.getInt(player);
                    xpSeedField.setInt(player, worldIn.rand.nextInt());
                } catch (Exception e) {
                    // 如果反射失败，尝试直接使用字段名
                    try {
                        Field xpSeedField = EntityPlayer.class.getDeclaredField("xpSeed");
                        xpSeedField.setAccessible(true);
                        xpSeedField.setInt(player, worldIn.rand.nextInt());
                    } catch (Exception ex) {
                        System.err.println("[MoreMod] Failed to reset xpSeed: " + ex.getMessage());
                    }
                }

                // 通知玩家
                player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "你感到命运的齿轮开始转动..."
                ));
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "(附魔种子已重置，附魔台结果将改变)"
                ));

                // 如果玩家正在使用附魔台，关闭界面
                if (player.openContainer instanceof ContainerEnchantment) {
                    player.closeScreen();
                    player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "请重新打开附魔台查看新的附魔选项"
                    ));
                }

                // 播放音效
                worldIn.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
        }

        return super.onItemUseFinish(stack, worldIn, entityLiving);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "命运之果");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "食用后重置你的附魔种子");
        tooltip.add(TextFormatting.GRAY + "改变附魔台显示的附魔选项");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "\"每一口都是新的开始\"");
    }
}
