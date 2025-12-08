package com.moremod.item.ritual;

import com.moremod.MoreMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 灵魂果实 (Soul Fruit)
 *
 * 食用后获得强大的临时增益效果
 * 但会消耗一定经验值
 */
public class ItemSoulFruit extends ItemFood {

    public ItemSoulFruit() {
        super(2, 0.1F, false);
        setUnlocalizedName("moremod.soul_fruit");
        setRegistryName("soul_fruit");
        setCreativeTab(MoreMod.CREATIVE_TAB);
        setAlwaysEdible();
        setMaxStackSize(8);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32; // 1.6秒
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.EAT;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;

            if (!worldIn.isRemote) {
                // 检查经验
                if (player.experienceLevel < 5 && !player.isCreative()) {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "需要至少5级经验才能承受灵魂果实的力量！"
                    ));
                    return stack; // 不消耗
                }

                // 消耗5级经验
                if (!player.isCreative()) {
                    player.addExperienceLevel(-5);
                }

                // 获得强大增益 (持续3分钟)
                int duration = 3600; // 3分钟

                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, duration, 2)); // 力量III
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, duration, 1)); // 速度II
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, duration, 1)); // 抗性II
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, duration, 0)); // 生命恢复I
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, duration, 0)); // 夜视

                // 通知玩家
                player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "灵魂能量涌入你的身体！"
                ));
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "获得: 力量III, 速度II, 抗性II, 生命恢复, 夜视"
                ));
                player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "持续时间: 3分钟"
                ));

                // 音效和粒子
                worldIn.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
        }

        return super.onItemUseFinish(stack, worldIn, entityLiving);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "蕴含灵魂能量的神秘果实");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "食用后获得 (3分钟):");
        tooltip.add(TextFormatting.RED + "  力量 III");
        tooltip.add(TextFormatting.AQUA + "  速度 II");
        tooltip.add(TextFormatting.GOLD + "  抗性提升 II");
        tooltip.add(TextFormatting.GREEN + "  生命恢复 I");
        tooltip.add(TextFormatting.BLUE + "  夜视");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_RED + "代价: 消耗 5 级经验");
    }
}
