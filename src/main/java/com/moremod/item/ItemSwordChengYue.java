package com.moremod.item;

import com.google.common.collect.Multimap;
import com.moremod.capability.ChengYueCapability;
import com.moremod.item.chengyue.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.core.controller.AnimationController;

import java.util.List;
import java.util.UUID;

import com.moremod.client.render.SwordChengYueRenderer;

/**
 * 澄月 - 无限进化的月之神器（平衡版 2.0）
 */
public class ItemSwordChengYue extends ItemSword implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemSwordChengYue() {
        super(ToolMaterial.DIAMOND);
        setRegistryName("sword_chengyue");
        setTranslationKey("sword_chengyue");
        setMaxStackSize(1);
        setMaxDamage(0);
        setCreativeTab(CreativeTabs.COMBAT);
    }

    // ==================== GeckoLib动画 ====================

    // 动画名称常量
    public static final String ANIM_FIRST_PERSON = "animation.moon_sword.first_person";
    public static final String ANIM_FIRST_ATTACK = "animation.moon_sword.first_attack";
    public static final String ANIM_FIRST_ATTACK2 = "animation.moon_sword.first_attack2";
    public static final String ANIM_THIRD_PERSON = "animation.moon_sword.third_person";
    public static final String ANIM_THIRD_ATTACK = "animation.moon_sword.third_attack";
    public static final String ANIM_THIRD_ATTACK2 = "animation.moon_sword.third_attack2";

    // 当前动画状态
    private static String currentAnimation = ANIM_FIRST_PERSON;
    private static boolean attackToggle = false; // 交替使用 attack1 和 attack2

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(
                this, "skill_controller", 0, this::skillPredicate
        ));
    }

    private PlayState skillPredicate(AnimationEvent<ItemSwordChengYue> event) {
        // 检查是否处于技能状态
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return PlayState.STOP;

        ChengYueCapability cap = mc.player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null || !cap.isSkillActive()) {
            return PlayState.STOP;
        }

        int skillType = cap.getSkillType();

        // skillType 1-6 对应 6 种动画
        String animName;
        switch (skillType) {
            case 1: animName = ANIM_FIRST_PERSON; break;
            case 2: animName = ANIM_FIRST_ATTACK; break;
            case 3: animName = ANIM_FIRST_ATTACK2; break;
            case 4: animName = ANIM_THIRD_PERSON; break;
            case 5: animName = ANIM_THIRD_ATTACK; break;
            case 6: animName = ANIM_THIRD_ATTACK2; break;
            default:
                // 默认根据视角选择
                boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;
                animName = isFirstPerson ? ANIM_FIRST_ATTACK : ANIM_THIRD_ATTACK;
                break;
        }

        event.getController().setAnimation(new software.bernie.geckolib3.core.builder.AnimationBuilder()
                .addAnimation(animName, false));

        return PlayState.CONTINUE;
    }

    /**
     * 触发技能动画（供外部调用）
     */
    public static void triggerSkillAnimation(EntityPlayer player) {
        if (player.world.isRemote) return;

        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap != null) {
            // 交替使用两种攻击动画
            attackToggle = !attackToggle;
            int skillType = attackToggle ? 1 : 2;
            cap.activateSkill(skillType);

            System.out.println("[ChengYue] 服务端触发技能动画: skillType=" + skillType);
        } else {
            System.out.println("[ChengYue] 警告: cap 为 null!");
        }
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @SideOnly(Side.CLIENT)
    public void registerISTER() {
        setTileEntityItemStackRenderer(new SwordChengYueRenderer());
    }

    // ==================== 物品更新 ====================

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        ChengYueNBT.init(stack);

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            if (!world.isRemote) {
                UUID bound = ChengYueNBT.getSoulBound(stack);
                if (bound == null) {
                    ChengYueNBT.setSoulBound(stack, player.getUniqueID());
                }
            }
        }
    }

    // ==================== 战斗逻辑 ====================

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        ChengYueNBT.init(stack);

        if (attacker.world.isRemote) return true;

        if (attacker instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) attacker;
            ChengYueLevel.addExp(stack, player, 10);

            // 触发拔刀技能动画
            triggerSkillAnimation(player);
        }

        return true;
    }

    // ==================== 右键查看属性 ====================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ChengYueNBT.init(stack);

        if (!world.isRemote) {
            if (player.isSneaking() && ChengYueFormManager.canManualSwitch(stack)) {
                ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);

                if (cap != null) {
                    int currentIndex = cap.getCurrentForm();
                    int nextIndex = (currentIndex + 1) % 8;
                    ChengYueMoonForm nextForm = ChengYueMoonForm.values()[nextIndex];

                    ChengYueFormManager.switchFormManual(stack, player, nextForm);
                }

                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            String stats = getDetailedStatusPanel(stack, player);
            player.sendMessage(new TextComponentString(stats));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private String getDetailedStatusPanel(ItemStack stack, EntityPlayer player) {
        StringBuilder sb = new StringBuilder();

        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);

        sb.append("§b━━━ 澄月属性面板 ━━━\n");
        sb.append(String.format("§f等级: §e%d §7| §f阶位: §d%s\n",
                level, ChengYueLevel.getStageRoman(stage)));

        sb.append("\n§c【战斗属性】\n");
        sb.append(String.format("§c攻击力: §f%.1f\n", ChengYueStats.getDamage(stack, player)));
        sb.append(String.format("§e攻速: §f%.2f\n", ChengYueStats.getAttackSpeed(stack, player.world)));
        sb.append(String.format("§6暴击率: §f%.1f%%\n", ChengYueStats.getCritChance(stack, player.world) * 100));
        sb.append(String.format("§6暴击伤害: §f×%.2f\n", ChengYueStats.getCritDamage(stack, player.world)));

        sb.append("\n");
        sb.append(ChengYueSweep.getSweepDescription(level));

        sb.append("\n§a【生存属性】\n");
        sb.append(ChengYueSurvival.getSurvivalInfo(stack, player.world));

        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap != null) {
            int combo = cap.getCombo();
            if (combo > 0) {
                float comboMult = ChengYueCombo.getComboMultiplier(combo);
                sb.append("\n§6【连击】×").append(combo);
                sb.append(String.format(" §7(×%.2f伤害)\n", comboMult));
            }
        }

        if (level >= 35 && cap != null) {
            sb.append("\n§d【形态系统】\n");
            ChengYueMoonForm form = ChengYueMoonForm.values()[cap.getCurrentForm()];
            sb.append(form.getDetailedDescription());
        }

        if (level >= 35 && cap != null) {
            sb.append("\n§b【月华】\n");
            sb.append(String.format("§b当前: §f%d/%d\n",
                    cap.getLunarPower(), cap.getMaxLunarPower()));
        }

        sb.append("\n");
        sb.append(ChengYueMoonMemory.getMemoryStatus(stack, player.world));
        sb.append("\n");
        sb.append(ChengYueMoonMemory.getMemoryEffects(stack, player.world));

        return sb.toString();
    }

    // ==================== 属性修改器（白值统一） ====================

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            ChengYueNBT.init(stack);

            multimap.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            multimap.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            int level = ChengYueNBT.getLevel(stack);
            int stage = ChengYueNBT.getStage(stack);

            // 和 ChengYueStats 保持同一套白值逻辑
            double base = 7.0;
            double levelBonus = 2.5 * Math.log(level + 1) / Math.log(2);
            double stageBonus = (stage - 1) * 3.0;
            double baseDamage = base + levelBonus + stageBonus;

            double attackSpeed = ChengYueStats.getAttackSpeed(stack, null);

            multimap.put(
                    SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(
                            ATTACK_DAMAGE_MODIFIER,
                            "Weapon modifier",
                            baseDamage,
                            0
                    )
            );

            multimap.put(
                    SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(
                            ATTACK_SPEED_MODIFIER,
                            "Weapon modifier",
                            attackSpeed - 4.0,
                            0
                    )
            );
        }

        return multimap;
    }

    // ==================== Tooltip ====================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);

        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "澄月 · " + ChengYueLevel.getStageRoman(stage));

        float progress = ChengYueLevel.getExpProgress(stack);
        tooltip.add(TextFormatting.GRAY + "Lv." + level + " " +
                makeProgressBar(progress, 10) + " " +
                String.format("%.0f%%", progress * 100));

        tooltip.add("");

        EntityPlayer clientPlayer = null;
        if (world != null && world.isRemote) {
            try {
                clientPlayer = Minecraft.getMinecraft().player;
            } catch (Exception ignored) {}
        }

        double damage = clientPlayer != null
                ? ChengYueStats.getDamage(stack, clientPlayer)
                : 7.0 + 2.5 * Math.log(level + 1) / Math.log(2) + (stage - 1) * 3.0;

        tooltip.add(TextFormatting.RED + "⚔ " +
                String.format("%.1f", damage) +
                TextFormatting.GRAY + " | " +
                TextFormatting.YELLOW + "⚡ " +
                String.format("%.2f", ChengYueStats.getAttackSpeed(stack, world)));

        tooltip.add(TextFormatting.GOLD + "☆ " +
                String.format("%.0f%%", ChengYueStats.getCritChance(stack, world) * 100) +
                TextFormatting.GRAY + " ×" +
                String.format("%.1f", ChengYueStats.getCritDamage(stack, world)));

        tooltip.add(TextFormatting.GREEN + "♥ " +
                String.format("%.0f%%", ChengYueStats.getLifeSteal(stack, world) * 100) +
                TextFormatting.GRAY + " | " +
                TextFormatting.BLUE + "◈ " +
                String.format("%.0f%%", ChengYueStats.getDamageReduction(stack, world) * 100));

        if (clientPlayer != null && level >= 35) {
            ChengYueCapability cap = clientPlayer.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null) {
                tooltip.add("");

                ChengYueMoonForm form = ChengYueMoonForm.values()[cap.getCurrentForm()];
                tooltip.add(form.getFullDisplayName());
                tooltip.add(TextFormatting.GRAY + form.getDescription());

                int lunar = cap.getLunarPower();
                int maxLunar = cap.getMaxLunarPower();
                tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ " + lunar + "/" + maxLunar +
                        TextFormatting.GRAY + " 月华");

                if (level >= 40) {
                    tooltip.add(TextFormatting.DARK_GRAY + "  Shift+右键切换形态");
                }
            }
        }

        tooltip.add("");

        if (clientPlayer != null) {
            ChengYueCapability cap = clientPlayer.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null) {
                int combo = cap.getCombo();
                if (combo > 0) {
                    float comboMult = ChengYueCombo.getComboMultiplier(combo);
                    tooltip.add(TextFormatting.GOLD + "连击 ×" + combo +
                            TextFormatting.GRAY + String.format(" (×%.2f)", comboMult));
                }
            }
        }

        if (world != null) {
            int currentPhase = ChengYueMoonPhase.getCurrentPhase(world);
            String phaseName = ChengYueMoonPhase.getPhaseName(currentPhase);
            String phaseIcon = ChengYueMoonPhase.getPhaseIcon(currentPhase);
            TextFormatting phaseColor = ChengYueMoonPhase.getPhaseColor(currentPhase);
            String phaseDesc = ChengYueMoonPhase.getPhaseDescription(world);

            tooltip.add("");
            tooltip.add(TextFormatting.GRAY + "月相: " +
                    phaseColor + phaseIcon + " " + phaseName);
            tooltip.add(TextFormatting.DARK_GRAY + "  " + phaseDesc);

            String memoryTooltip = ChengYueMoonMemory.getMemoryTooltip(stack);
            if (!memoryTooltip.isEmpty()) {
                tooltip.add(memoryTooltip);
            }
        }

        if (level >= 61) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_PURPLE + "【月殇】已解锁");
            tooltip.add(TextFormatting.GRAY + "攻击叠加易伤效果");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "右键查看详情");

        if (flag.isAdvanced()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "击杀: " + ChengYueNBT.getKillCount(stack));
            tooltip.add(TextFormatting.DARK_GRAY + "Boss: " + ChengYueNBT.getBossKills(stack));

            if (clientPlayer != null) {
                ChengYueCapability cap = clientPlayer.getCapability(ChengYueCapability.CAPABILITY, null);
                if (cap != null) {
                    tooltip.add(TextFormatting.DARK_GRAY + "最高连击: " + cap.getMaxCombo());
                }
            }

            java.util.UUID bound = ChengYueNBT.getSoulBound(stack);
            if (bound != null) {
                tooltip.add(TextFormatting.DARK_GRAY + "绑定: " + bound.toString().substring(0, 8) + "...");
            }
        }
    }

    private String makeProgressBar(float progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        bar.append(TextFormatting.GOLD);
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("▮");
            } else {
                bar.append(TextFormatting.DARK_GRAY).append("▯").append(TextFormatting.GOLD);
            }
        }
        return bar.toString();
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        ChengYueNBT.init(stack);
        int level = ChengYueNBT.getLevel(stack);
        return level >= 10;
    }

    // ==================== 附魔支持 ====================

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;  // 允许附魔（覆盖原版的isDamageable检查）
    }

    @Override
    public int getItemEnchantability() {
        return 22;  // 较高的附魔能力（金质=22, 钻石=10）
    }
}
