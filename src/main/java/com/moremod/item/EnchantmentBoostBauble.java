package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 附魔增强饰品 - 纯NBT版本（超级简化）
 * 不需要ASM，只管理饰品自身的激活状态
 */
public class EnchantmentBoostBauble extends Item implements IBauble {

    private final int boostAmount;
    private static final int COOLDOWN_SECONDS = 120;
    private static final int DURATION_SECONDS = 60;

    public EnchantmentBoostBauble(String name, int boostAmount) {
        this.boostAmount = boostAmount;
        setRegistryName(name);
        setTranslationKey("moremod." + name);
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);

    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    /**
     * 获取原始增幅值
     */
    public int getRawBoostAmount() {
        return boostAmount;
    }

    /**
     * 工具方法：检查玩家是否佩戴增强饰品
     */
    public static boolean hasBoostBauble(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof EnchantmentBoostBauble) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[moremod] Error checking boost bauble: " + e.getMessage());
        }
        return false;
    }

    /**
     * 工具方法：获取饰品原始增幅值
     */
    public static int getBaubleBoostAmount(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof EnchantmentBoostBauble) {
                        return ((EnchantmentBoostBauble) bauble.getItem()).getRawBoostAmount();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[moremod] Error getting bauble boost amount: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
            if (tryActivateBoost(player, stack)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }
        return super.onItemRightClick(world, player, hand);
    }

    /**
     * ✅ 纯NBT版本：只设置饰品自身的激活状态
     * 物品的增强由 EnchantmentBoostHandler 自动处理
     */
    public boolean tryActivateBoost(EntityPlayer player, ItemStack baubleStack) {
        if (!baubleStack.hasTagCompound()) {
            baubleStack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound tag = baubleStack.getTagCompound();
        long now = System.currentTimeMillis();
        long cooldownEnd = tag.getLong("cooldown_end");

        // 检查冷却
        if (now < cooldownEnd) {
            int remaining = (int)((cooldownEnd - now) / 1000);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "冷却中... 剩余 " + remaining + " 秒"), true);
            return false;
        }

        // 检查是否已激活
        if (tag.getBoolean("moremod:boost_active")) {
            long endTime = tag.getLong("moremod:boost_end");
            if (now < endTime) {
                int remaining = (int)((endTime - now) / 1000);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "附魔增强已激活！剩余 " + remaining + " 秒"), true);
                return false;
            }
        }

        // 检查主手是否有可附魔物品
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !mainHand.isItemEnchanted()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "主手需要持有已附魔的物品"), true);
            return false;
        }

        // ✅ 只设置饰品NBT（物品增强由Handler自动处理）
        tag.setBoolean("moremod:boost_active", true);
        tag.setInteger("moremod:boost_level", boostAmount);
        tag.setLong("moremod:boost_end", now + (DURATION_SECONDS * 1000L));
        tag.setLong("cooldown_end", now + (COOLDOWN_SECONDS * 1000L));

        // 反馈
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "✦ 附魔增强已激活！主手武器 +" + boostAmount + " 级"), true);

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.5F, 1.2F);

        System.out.println("[moremod] 饰品激活 - 玩家: " + player.getName() +
                ", 增幅值: " + boostAmount);

        return true;
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase entity) {
        if (!entity.world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            if (stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();
                boolean wasActive = tag.getBoolean("moremod:boost_active");

                // 清除饰品NBT状态
                tag.removeTag("moremod:boost_active");
                tag.removeTag("moremod:boost_level");
                tag.removeTag("moremod:boost_end");

                if (wasActive) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GRAY + "附魔增强已失效（饰品已卸下）"), true);

                    System.out.println("[moremod] 饰品卸下 - 玩家: " + player.getName());
                }
            }
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (!entity.world.isRemote && entity.ticksExisted % 20 == 0) {
            if (stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();

                if (tag.hasKey("moremod:boost_end")) {
                    long endTime = tag.getLong("moremod:boost_end");
                    if (System.currentTimeMillis() > endTime) {
                        // 过期清理
                        tag.removeTag("moremod:boost_active");
                        tag.removeTag("moremod:boost_level");
                        tag.removeTag("moremod:boost_end");

                        if (entity instanceof EntityPlayer) {
                            ((EntityPlayer) entity).sendStatusMessage(new TextComponentString(
                                    TextFormatting.GRAY + "附魔增强已失效"), true);
                        }
                    }
                }
            }
        }
    }

    /**
     * 动态Tooltip - 显示状态、剩余时间、冷却等信息
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 基础信息
        tooltip.add(TextFormatting.GOLD + "✦ 附魔增强戒指 ✦");
        tooltip.add(TextFormatting.GRAY + "类型: " + TextFormatting.AQUA + "饰品戒指");
        tooltip.add("");

        // 增幅信息
        tooltip.add(TextFormatting.YELLOW + "增幅等级: " + TextFormatting.WHITE + "+" + boostAmount + " 级");
        tooltip.add(TextFormatting.YELLOW + "持续时间: " + TextFormatting.WHITE + DURATION_SECONDS + " 秒");
        tooltip.add(TextFormatting.YELLOW + "冷却时间: " + TextFormatting.WHITE + COOLDOWN_SECONDS + " 秒");
        tooltip.add("");

        // 动态状态信息
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            long now = System.currentTimeMillis();

            // 检查激活状态
            boolean isActive = tag.getBoolean("moremod:boost_active");
            long boostEnd = tag.getLong("moremod:boost_end");
            long cooldownEnd = tag.getLong("cooldown_end");

            if (isActive && now < boostEnd) {
                // 显示激活状态和剩余时间
                int remainingSeconds = (int)((boostEnd - now) / 1000);
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;

                tooltip.add(TextFormatting.GREEN + "▶ 状态: " + TextFormatting.GREEN + TextFormatting.BOLD + "已激活");

                if (minutes > 0) {
                    tooltip.add(TextFormatting.GREEN + "  剩余: " + minutes + " 分 " + seconds + " 秒");
                } else {
                    tooltip.add(TextFormatting.GREEN + "  剩余: " + seconds + " 秒");
                }

                // 进度条
                float progress = (float)(boostEnd - now) / (float)(DURATION_SECONDS * 1000L);
                String progressBar = createProgressBar(progress, 20, '▮', '▯');
                tooltip.add(TextFormatting.GREEN + "  " + progressBar);

            } else if (now < cooldownEnd) {
                // 显示冷却状态
                int cooldownRemaining = (int)((cooldownEnd - now) / 1000);
                int minutes = cooldownRemaining / 60;
                int seconds = cooldownRemaining % 60;

                tooltip.add(TextFormatting.RED + "▶ 状态: " + TextFormatting.RED + TextFormatting.BOLD + "冷却中");

                if (minutes > 0) {
                    tooltip.add(TextFormatting.RED + "  冷却: " + minutes + " 分 " + seconds + " 秒");
                } else {
                    tooltip.add(TextFormatting.RED + "  冷却: " + seconds + " 秒");
                }

                // 冷却进度条
                float cooldownProgress = 1.0f - ((float)(cooldownEnd - now) / (float)(COOLDOWN_SECONDS * 1000L));
                String cooldownBar = createProgressBar(cooldownProgress, 20, '▮', '▯');
                tooltip.add(TextFormatting.RED + "  " + cooldownBar);

            } else {
                // 就绪状态
                tooltip.add(TextFormatting.AQUA + "▶ 状态: " + TextFormatting.AQUA + TextFormatting.BOLD + "就绪");
                tooltip.add(TextFormatting.GRAY + "  可以激活");
            }
        } else {
            // 未使用过
            tooltip.add(TextFormatting.AQUA + "▶ 状态: " + TextFormatting.AQUA + TextFormatting.BOLD + "就绪");
            tooltip.add(TextFormatting.GRAY + "  可以激活");
        }

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.DARK_GRAY + "使用方法:");
        tooltip.add(TextFormatting.DARK_GRAY + "• 佩戴在饰品栏");
        tooltip.add(TextFormatting.DARK_GRAY + "• 主手持有已附魔物品");
        tooltip.add(TextFormatting.DARK_GRAY + "• 按 " + TextFormatting.DARK_AQUA + "[G]" + TextFormatting.DARK_GRAY + " 键激活");

        // Shift详细信息
        if (flagIn.isAdvanced()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "=== 详细信息 ===");
            tooltip.add(TextFormatting.DARK_GRAY + "• 增强主手物品的所有附魔");
            tooltip.add(TextFormatting.DARK_GRAY + "• 效果在切换物品后保持");
            tooltip.add(TextFormatting.DARK_GRAY + "• 卸下饰品会立即失效");

            // 显示原始NBT数据（调试用）
            if (stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("moremod:boost_active")) {
                    tooltip.add("");
                    tooltip.add(TextFormatting.DARK_PURPLE + "NBT数据:");
                    tooltip.add(TextFormatting.DARK_PURPLE + "• boost_active: " + tag.getBoolean("moremod:boost_active"));
                    tooltip.add(TextFormatting.DARK_PURPLE + "• boost_level: " + tag.getInteger("moremod:boost_level"));
                }
            }
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY +  "按住 Shift 查看详情");
        }
    }

    /**
     * 创建进度条字符串
     */
    private String createProgressBar(float progress, int length, char fillChar, char emptyChar) {
        StringBuilder bar = new StringBuilder();
        int filled = (int)(progress * length);

        bar.append(TextFormatting.WHITE + "[");

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                // 根据进度选择颜色
                if (progress > 0.6f) {
                    bar.append(TextFormatting.GREEN);
                } else if (progress > 0.3f) {
                    bar.append(TextFormatting.YELLOW);
                } else {
                    bar.append(TextFormatting.RED);
                }
                bar.append(fillChar);
            } else {
                bar.append(TextFormatting.DARK_GRAY);
                bar.append(emptyChar);
            }
        }

        bar.append(TextFormatting.WHITE + "] ");
        bar.append(String.format("%.0f%%", progress * 100));

        return bar.toString();
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getBoolean("moremod:boost_active");
        }
        return false;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        // 根据状态改变稀有度颜色
        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("moremod:boost_active")) {
            return EnumRarity.EPIC; // 激活时紫色
        }
        return EnumRarity.RARE; // 普通状态蓝色
    }

    @Override
    public boolean getShareTag() {
        return true;
    }
}