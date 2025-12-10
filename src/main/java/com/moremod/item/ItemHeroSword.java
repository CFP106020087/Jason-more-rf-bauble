package com.moremod.item;

import com.moremod.client.render.HeroSwordRenderer;
import com.moremod.item.herosword.HeroSwordActiveSkill;
import com.moremod.item.herosword.HeroSwordNBT;
import com.moremod.item.herosword.HeroSwordStats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 勇者之剑 - 成长型终极武器
 *
 * 四大技能体系：
 * 1. 巨像杀手 - 血量差距增伤
 * 2. 宿命重担 - 受击/Debuff/压血增伤
 * 3. 宿命裁决 - 真伤转换
 * 4. 终局审判 - 主动处决
 */
public class ItemHeroSword extends ItemSword implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemHeroSword(ToolMaterial material) {
        super(material);
        this.setRegistryName("hero_sword");
        this.setTranslationKey("hero_sword");
        this.setMaxStackSize(1);
        this.setMaxDamage(0);  // 永不损坏
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(
                this, "controller", 0, this::predicate
        ));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // ==================== 渲染器注册 ====================

    @SideOnly(Side.CLIENT)
    public void initModel() {
        this.setTileEntityItemStackRenderer(HeroSwordRenderer.INSTANCE);
    }

    // ==================== 主动技能：终局审判（右键）====================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        HeroSwordNBT.init(stack);

        if (!world.isRemote) {
            boolean success = HeroSwordActiveSkill.tryCast(world, player, stack);

            if (!success) {
                // 显示剩余冷却
                long now = world.getTotalWorldTime();
                long last = HeroSwordNBT.getSkillCooldown(stack);
                long cooldown = HeroSwordStats.getExecuteCooldown(stack);
                long remaining = cooldown - (now - last);

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "终局审判冷却中: " + (remaining/20) + "秒"
                ), true);
            }

            // 创造模式Shift+右键显示调试信息
            if (player.isSneaking() && player.isCreative()) {
                HeroSwordStats.debugStats(player, stack);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // ==================== Tooltip ====================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack,
                               @Nullable World world,
                               List<String> tooltip,
                               ITooltipFlag flagIn) {

        HeroSwordNBT.init(stack);

        // 获取数据
        int level = HeroSwordNBT.getLevel(stack);
        int exp = HeroSwordNBT.getExp(stack);
        int expToNext = HeroSwordNBT.getExpToNext(stack);
        int totalKills = HeroSwordNBT.getTotalKills(stack);
        int bossKills = HeroSwordNBT.getBossKills(stack);
        String title = HeroSwordNBT.getGrowthTitle(stack);

        // ========== 标题 ==========
        tooltip.add(TextFormatting.DARK_PURPLE + "══════ " +
                TextFormatting.LIGHT_PURPLE + "勇者之剑" +
                TextFormatting.DARK_PURPLE + " ══════");

        // ========== 成长信息 ==========
        tooltip.add(TextFormatting.GOLD + title +
                TextFormatting.WHITE + " Lv." + level);

        // 经验条
        if (level < 100) {
            float progress = (float)exp / expToNext;
            tooltip.add(TextFormatting.GRAY + "经验: " +
                    makeProgressBar(progress, 15) +
                    TextFormatting.WHITE + " " + exp + "/" + expToNext);
        } else {
            tooltip.add(TextFormatting.YELLOW + "★ 满级 ★");
        }

        // 击杀统计
        String killInfo = TextFormatting.DARK_GRAY + "击杀: " + totalKills;
        if (bossKills > 0) {
            killInfo += TextFormatting.GOLD + " (Boss: " + bossKills + ")";
        }
        tooltip.add(killInfo);

        // ========== 武器基础 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.WHITE + "【基础属性】");
        double damage = this.getAttackDamage() + 1.0D;  // +1 for player base
        tooltip.add(TextFormatting.GRAY + "攻击力: " +
                TextFormatting.RED + String.format("%.1f", damage));

        // ========== 技能1：巨像杀手 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 巨像杀手");
        tooltip.add(TextFormatting.GRAY + "对血量高于你的敌人造成额外伤害");

        float giantCap = Math.min(5.0F, 2.5F + (level / 10.0F * 0.2F));
        tooltip.add(TextFormatting.GRAY + "倍率上限: " +
                TextFormatting.YELLOW + String.format("×%.1f", giantCap));

        // ========== 技能2：宿命重担 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "◆ 宿命重担");
        tooltip.add(TextFormatting.GRAY + "受击、Debuff、低血量时获得增伤");

        int hits = HeroSwordNBT.getHitsTaken(stack);
        if (hits > 0) {
            String hitColor = hits < 20 ? TextFormatting.YELLOW.toString() :
                    hits < 50 ? TextFormatting.GOLD.toString() :
                            TextFormatting.RED.toString();
            tooltip.add(hitColor + "▸ 受击层数: " + hits + "/100");

            // 显示当前倍率
            float burdenMult = HeroSwordStats.getFateBurdenMultiplier(stack, null);
            tooltip.add(TextFormatting.GRAY + "▸ 当前倍率: " +
                    TextFormatting.WHITE + String.format("×%.2f", burdenMult));
        }

        float burdenCap = Math.min(3.0F, 2.0F + (level / 10.0F * 0.1F));
        tooltip.add(TextFormatting.GRAY + "倍率上限: " +
                TextFormatting.YELLOW + String.format("×%.1f", burdenCap));

        // ========== 技能3：宿命裁决 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 宿命裁决");
        tooltip.add(TextFormatting.GRAY + "攻击时概率将伤害转为真实伤害");

        float trueChance = HeroSwordStats.getTrueDamageChance(stack) * 100;
        float trueConv = HeroSwordStats.getTrueDamageConversion(stack) * 100;

        tooltip.add(TextFormatting.GRAY + "触发率: " +
                TextFormatting.LIGHT_PURPLE + String.format("%.0f%%", trueChance) +
                TextFormatting.GRAY + " | 转换: " +
                TextFormatting.LIGHT_PURPLE + String.format("%.0f%%", trueConv));

        // ========== 技能4：终局审判 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 终局审判 " +
                TextFormatting.DARK_GRAY + "[右键]");
        tooltip.add(TextFormatting.GRAY + "处决范围内的低血量敌人");

        float threshold = HeroSwordStats.getExecuteThreshold(stack) * 100;
        float range = HeroSwordStats.getExecuteRange(stack);
        int maxTargets = HeroSwordStats.getMaxExecuteTargets(stack);

        tooltip.add(TextFormatting.GRAY + "阈值: " +
                TextFormatting.RED + String.format("%.0f%%", threshold) +
                TextFormatting.GRAY + " | 范围: " +
                TextFormatting.WHITE + String.format("%.0f格", range) +
                TextFormatting.GRAY + " | 数量: " +
                TextFormatting.WHITE + maxTargets);

        // 冷却显示
        if (world != null) {
            long now = world.getTotalWorldTime();
            long lastCast = HeroSwordNBT.getSkillCooldown(stack);
            long cooldown = HeroSwordStats.getExecuteCooldown(stack);
            long remaining = Math.max(0, cooldown - (now - lastCast));

            if (remaining > 0) {
                tooltip.add(TextFormatting.RED + "▸ 冷却: " + (remaining/20) + "秒");
            } else {
                tooltip.add(TextFormatting.GREEN + "▸ 技能就绪");
            }
        }

        // ========== 成长提示 ==========
        if (level < 100 && !flagIn.isAdvanced()) {
            tooltip.add("");
            if (level < 30) {
                tooltip.add(TextFormatting.DARK_GRAY + "提示: 击杀敌人获得经验，Boss提供大量经验");
            } else if (level < 60) {
                tooltip.add(TextFormatting.DARK_GRAY + "提示: " + (100-level) + "级后达到满级");
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "提示: 即将达到传说境界");
            }
        }

        // ========== 高级信息 ==========
        if (flagIn.isAdvanced()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "=== Debug Info ===");
            tooltip.add(TextFormatting.DARK_GRAY + "Level: " + level + "/100");
            tooltip.add(TextFormatting.DARK_GRAY + "Exp: " + exp + "/" + expToNext);
            tooltip.add(TextFormatting.DARK_GRAY + "Total Kills: " + totalKills);
            tooltip.add(TextFormatting.DARK_GRAY + "Boss Kills: " + bossKills);
            tooltip.add(TextFormatting.DARK_GRAY + "Hit Stacks: " + hits);
            tooltip.add(TextFormatting.DARK_GRAY + "True Procs: " + HeroSwordNBT.getTrueDamageProcs(stack));

            if (world != null) {
                boolean combat = HeroSwordNBT.isInCombat(stack, world.getTotalWorldTime());
                tooltip.add(TextFormatting.DARK_GRAY + "In Combat: " + combat);
            }
        }
    }

    /**
     * 创建渐变进度条
     */
    private String makeProgressBar(float progress, int length) {
        int filled = (int)(progress * length);
        StringBuilder sb = new StringBuilder();

        // 根据进度选择颜色
        String color = progress < 0.3F ? TextFormatting.RED.toString() :
                progress < 0.7F ? TextFormatting.YELLOW.toString() :
                        TextFormatting.GREEN.toString();

        sb.append(color);
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                sb.append("▮");
            } else {
                sb.append(TextFormatting.DARK_GRAY).append("▯").append(color);
            }
        }

        return sb.toString();
    }

    // ========== 物品属性 ==========

    @Override
    public boolean isDamageable() {
        return false;  // 永不损坏
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        // 30级以上显示附魔光效
        return HeroSwordNBT.getLevel(stack) >= 30;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;  // 允许附魔
    }

    @Override
    public int getItemEnchantability() {
        return 15;  // 中等附魔能力
    }
}