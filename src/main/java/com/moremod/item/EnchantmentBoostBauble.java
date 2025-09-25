package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.enchantment.EnchantmentBoostHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * moremod 附魔增强饰品 (1.12.2 Baubles)
 * 佩戴时可按键激活60秒附魔增强
 */
public class EnchantmentBoostBauble extends Item implements IBauble, EnchantmentBoostHelper.IEnchantmentBooster {

    private final int boostAmount;
    private final BaubleType baubleType;
    private static final int COOLDOWN_TICKS = 2400; // 120秒 = 2400 ticks
    private static final int BOOST_DURATION_SECONDS = 60; // 60秒

    public EnchantmentBoostBauble(String name, int boostAmount) {
        this.boostAmount = boostAmount;
        this.baubleType = BaubleType.RING; // 默认戒指

        setRegistryName(name);
        setTranslationKey("moremod." + name);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return baubleType;
    }

    @Override
    public int getBoostAmount(ItemStack stack, EntityPlayer player) {
        return boostAmount;
    }

    /**
     * 激活增强效果的方法
     * 这个方法需要在你的按键处理器中调用
     */
    public void activateBoost(EntityPlayer player, ItemStack stack) {
        if (!player.world.isRemote) {
            NBTTagCompound nbt = getOrCreateNBT(stack);
            long currentTime = player.world.getTotalWorldTime();
            long cooldownEnd = nbt.getLong("cooldownEnd");

            // 检查是否在冷却中
            if (currentTime >= cooldownEnd) {
                // 激活增强效果 - 三个参数：player, boostAmount, durationSeconds
                EnchantmentBoostHelper.activateBoost(player, boostAmount, BOOST_DURATION_SECONDS);

                // 设置原版冷却
                player.getCooldownTracker().setCooldown(this, COOLDOWN_TICKS);

                // 保存状态到NBT
                nbt.setLong("boostEnd", currentTime + (BOOST_DURATION_SECONDS * 20)); // 转换为ticks
                nbt.setLong("cooldownEnd", currentTime + COOLDOWN_TICKS);

                // 音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        net.minecraft.util.SoundCategory.PLAYERS,
                        0.5F, 1.0F);
            }
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer) player;

            // 服务端：更新NBT状态
            if (!player.world.isRemote) {
                NBTTagCompound nbt = getOrCreateNBT(stack);
                long currentTime = player.world.getTotalWorldTime();
                long boostEnd = nbt.getLong("boostEnd");

                // 如果增强时间结束，清除增强状态
                if (boostEnd > 0 && currentTime >= boostEnd) {
                    nbt.setLong("boostEnd", 0);
                }

                // 同步原版冷却
                long cooldownEnd = nbt.getLong("cooldownEnd");
                if (cooldownEnd > currentTime && !entityPlayer.getCooldownTracker().hasCooldown(this)) {
                    // 重新设置冷却（用于同步）
                    int remainingTicks = (int)(cooldownEnd - currentTime);
                    entityPlayer.getCooldownTracker().setCooldown(this, remainingTicks);
                }
            }

            // 客户端：显示粒子效果
            if (player.world.isRemote) {
                NBTTagCompound nbt = getOrCreateNBT(stack);
                long currentTime = player.world.getTotalWorldTime();
                long boostEnd = nbt.getLong("boostEnd");

                if (boostEnd > currentTime && player.ticksExisted % 10 == 0) {
                    double x = player.posX + (player.world.rand.nextDouble() - 0.5) * player.width;
                    double y = player.posY + player.height * 0.75;
                    double z = player.posZ + (player.world.rand.nextDouble() - 0.5) * player.width;

                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.SPELL_WITCH,
                            x, y, z,
                            0.0, 0.1, 0.0
                    );
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "附魔增强饰品");
        tooltip.add(TextFormatting.GRAY + "佩戴时按 " + TextFormatting.YELLOW + "[G键]" + TextFormatting.GRAY + " 激活");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "▸ 主手附魔等级 +" + boostAmount);
        tooltip.add(TextFormatting.AQUA + "▸ 持续时间: 60秒");
        tooltip.add(TextFormatting.RED + "▸ 冷却时间: 120秒");

        // 从NBT读取状态
        if (world != null) {
            NBTTagCompound nbt = getOrCreateNBT(stack);
            long currentTime = world.getTotalWorldTime();
            long boostEnd = nbt.getLong("boostEnd");
            long cooldownEnd = nbt.getLong("cooldownEnd");

            tooltip.add("");

            // 显示增强状态
            if (boostEnd > currentTime) {
                int remaining = (int)((boostEnd - currentTime) / 20);
                tooltip.add(TextFormatting.LIGHT_PURPLE + "✦ 增强中 - 剩余 " + remaining + " 秒");
            }
            // 显示冷却状态
            else if (cooldownEnd > currentTime) {
                int remaining = (int)((cooldownEnd - currentTime) / 20);
                tooltip.add(TextFormatting.GRAY + "⏳ 冷却中 - 剩余 " + remaining + " 秒");
            }
            // 显示就绪状态
            else {
                tooltip.add(TextFormatting.GREEN + "✔ 就绪");
            }
        }
    }

    /**
     * 获取或创建NBT标签
     */
    private NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        // 可选：只在增强激活时显示附魔光效
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            long boostEnd = nbt.getLong("boostEnd");
            // 如果想要始终显示光效，改为 return true;
            return boostEnd > 0; // 只在增强时发光
        }
        return true; // 默认始终显示附魔光效
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    /**
     * 确保NBT数据在物品复制时保留
     */
    @Override
    public boolean getShareTag() {
        return true;
    }

    /**
     * 防止物品在创造模式下被意外破坏
     */
    @Override
    public boolean isDamageable() {
        return false;
    }
}