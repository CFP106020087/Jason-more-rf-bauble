package com.moremod.item;

import com.moremod.capability.ChengYueCapability;
import com.moremod.item.chengyue.*;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// ★ GeckoLib 1.12.2
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.core.controller.AnimationController;

import com.moremod.client.render.SwordChengYueRenderer;

import com.google.common.collect.Multimap;

/**
 * 澄月 - 无限进化的月之神器
 *
 * 第一阶段功能（0-30级）：
 * - 月相系统：8种月相各有加成
 * - 等级成长：0-30级，3个阶位
 * - 生存机制：生命偷取 + 月之庇护
 * - 连击系统：连击越高伤害越高（使用Capability）
 * - 主动技能：月华斩（前方AOE）
 *
 * 第二阶段功能（35级+）：
 * - 月华系统：资源管理
 * - 形态系统：8种战斗形态
 *
 * 第三阶段功能（61级+）：
 * - 月殇系统：叠加易伤
 */
public class ItemSwordChengYue extends ItemSword implements IAnimatable {

    // GeckoLib 动画工厂
    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemSwordChengYue() {
        super(ToolMaterial.DIAMOND);
        setRegistryName("sword_chengyue");
        setTranslationKey("sword_chengyue");
        setMaxStackSize(1);
        setMaxDamage(0); // 不使用耐久系统
        setCreativeTab(CreativeTabs.COMBAT);
    }

    // ==================== GeckoLib动画 ====================

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(
                this, "idle", 0, this::idlePredicate
        ));
    }

    private PlayState idlePredicate(AnimationEvent<ItemSwordChengYue> event) {
        return PlayState.STOP;
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

            // 服务端逻辑
            if (!world.isRemote) {
                // ✨ 修复：灵魂绑定（使用UUID）
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

        // 服务端处理
        if (attacker instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) attacker;

            // ✨ 修复：连击在EventHandler中处理，这里只获取经验
            ChengYueLevel.addExp(stack, player, 10);

            // 注意：连击和月华的增加已经在 ChengYueEventHandler 中处理了
        }

        return true;
    }

    // ==================== 右键查看属性 ====================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ChengYueNBT.init(stack);

        if (!world.isRemote) {
            // Shift+右键：切换形态（40级+）
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

            // 普通右键：查看详细属性面板
            String stats = getDetailedStatusPanel(stack, player);
            player.sendMessage(new TextComponentString(stats));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 获取详细属性面板
     */
    private String getDetailedStatusPanel(ItemStack stack, EntityPlayer player) {
        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("§b━━━ 澄月属性面板 ━━━\n");

        // 基础信息
        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);
        sb.append(String.format("§f等级: §e%d §7| §f阶位: §d%d\n", level, stage));

        // 战斗属性
        sb.append("\n§c【战斗属性】\n");
        sb.append(String.format("§c攻击力: §f%.1f\n", ChengYueStats.getDamage(stack, player)));
        sb.append(String.format("§e攻击速度: §f%.2f\n", ChengYueStats.getAttackSpeed(stack, player.world)));
        sb.append(String.format("§6暴击率: §f%.1f%%\n", ChengYueStats.getCritChance(stack, player.world) * 100));
        sb.append(String.format("§6暴击伤害: §f×%.2f\n", ChengYueStats.getCritDamage(stack, player.world)));

        // 范围攻击
        sb.append("\n");
        sb.append(ChengYueSweep.getSweepDescription(level));

        // 生存属性
        sb.append("\n§a【生存属性】\n");
        sb.append(ChengYueSurvival.getSurvivalInfo(stack, player.world));

        // ✨ 修复：从Capability获取连击信息
        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap != null) {
            int combo = cap.getCombo();
            if (combo > 0) {
                float comboMult = ChengYueCombo.getComboMultiplier(combo);
                sb.append("\n§6【连击】×").append(combo);
                sb.append(String.format(" §7(×%.2f伤害)\n", comboMult));
            }
        }

        // ✨ 修复：形态信息
        if (level >= 35 && cap != null) {
            sb.append("\n§d【形态系统】\n");
            ChengYueMoonForm form = ChengYueMoonForm.values()[cap.getCurrentForm()];
            sb.append(form.getDetailedDescription());
        }

        // ✨ 修复：月华信息
        if (level >= 35 && cap != null) {
            sb.append("\n§b【月华】\n");
            sb.append(String.format("§b当前: §f%d/%d\n",
                    cap.getLunarPower(), cap.getMaxLunarPower()));
        }

        // 月相记忆
        sb.append("\n");
        sb.append(ChengYueMoonMemory.getMemoryStatus(stack, player.world));
        sb.append("\n");
        sb.append(ChengYueMoonMemory.getMemoryEffects(stack, player.world));

        return sb.toString();
    }

    // ==================== 属性修改器 ====================

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            ChengYueNBT.init(stack);

            // 移除默认属性
            multimap.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            multimap.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            // ✨ 修复：基础攻击力（不包含形态加成）
            int level = ChengYueNBT.getLevel(stack);
            int stage = ChengYueNBT.getStage(stack);
            double baseDamage = 10.0 + 8.0 * Math.log(level + 1) / Math.log(2) + stage * 6.0;

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
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        ChengYueNBT.init(stack);

        // 基础信息
        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);

        // ===== 简洁的标题 =====
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "澄月 · " + ChengYueLevel.getStageRoman(stage));

        // ===== 等级和经验（紧凑）=====
        float progress = ChengYueLevel.getExpProgress(stack);
        tooltip.add(TextFormatting.GRAY + "Lv." + level + " " +
                makeProgressBar(progress, 10) + " " +
                String.format("%.0f%%", progress * 100));

        tooltip.add("");

        // ✨ 修复：尝试获取玩家
        EntityPlayer player = null;
        if (world != null && world.isRemote) {
            try {
                player = net.minecraft.client.Minecraft.getMinecraft().player;
            } catch (Exception e) {
                // 无法获取玩家
            }
        }

        // ===== 战斗属性（紧凑排版）=====
        double damage = player != null ? ChengYueStats.getDamage(stack, player) :
                (10.0 + 8.0 * Math.log(level + 1) / Math.log(2) + stage * 6.0);

        tooltip.add(TextFormatting.RED + "⚔ " +
                String.format("%.1f", damage) +
                TextFormatting.GRAY + " | " +
                TextFormatting.YELLOW + "⚡ " +
                String.format("%.2f", ChengYueStats.getAttackSpeed(stack, world)));

        tooltip.add(TextFormatting.GOLD + "☆ " +
                String.format("%.0f%%", ChengYueStats.getCritChance(stack, world) * 100) +
                TextFormatting.GRAY + " ×" +
                String.format("%.1f", ChengYueStats.getCritDamage(stack, world)));

        // ===== 生存属性 =====
        tooltip.add(TextFormatting.GREEN + "♥ " +
                String.format("%.0f%%", ChengYueStats.getLifeSteal(stack, world) * 100) +
                TextFormatting.GRAY + " | " +
                TextFormatting.BLUE + "◈ " +
                String.format("%.0f%%", ChengYueStats.getDamageReduction(stack, world) * 100));

        // ===== 第二阶段：形态和月华 =====
        if (level >= 35 && player != null) {
            ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);

            if (cap != null) {
                tooltip.add("");

                // 当前形态
                ChengYueMoonForm form = ChengYueMoonForm.values()[cap.getCurrentForm()];
                tooltip.add(form.getFullDisplayName());
                tooltip.add(TextFormatting.GRAY + form.getDescription());

                // 月华值
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

        // ===== 连击状态 =====
        if (player != null) {
            ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null) {
                int combo = cap.getCombo();
                if (combo > 0) {
                    float comboMult = ChengYueCombo.getComboMultiplier(combo);
                    tooltip.add(TextFormatting.GOLD + "连击 ×" + combo +
                            TextFormatting.GRAY + String.format(" (×%.2f)", comboMult));
                }
            }
        }

        // ===== 当前月相 =====
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
        }

        // ===== 月相记忆 =====
        if (world != null) {
            String memoryTooltip = ChengYueMoonMemory.getMemoryTooltip(stack);
            if (!memoryTooltip.isEmpty()) {
                tooltip.add(memoryTooltip);
            }
        }

        // ===== 月殇系统 =====
        if (level >= 61) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_PURPLE + "【月殇】已解锁");
            tooltip.add(TextFormatting.GRAY + "攻击叠加易伤效果");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "右键查看详情");

        // ===== F3+H高级信息 =====
        if (flag.isAdvanced()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "击杀: " + ChengYueNBT.getKillCount(stack));
            tooltip.add(TextFormatting.DARK_GRAY + "Boss: " + ChengYueNBT.getBossKills(stack));

            if (player != null) {
                ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
                if (cap != null) {
                    tooltip.add(TextFormatting.DARK_GRAY + "最高连击: " + cap.getMaxCombo());
                }
            }

            // ✨ 灵魂绑定信息
            UUID bound = ChengYueNBT.getSoulBound(stack);
            if (bound != null) {
                tooltip.add(TextFormatting.DARK_GRAY + "绑定: " + bound.toString().substring(0, 8) + "...");
            }
        }
    }

    /**
     * 紧凑的进度条
     */
    private String makeProgressBar(float progress, int length) {
        int filled = (int)(progress * length);
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

    // ==================== 发光效果 ====================

    @Override
    public boolean hasEffect(ItemStack stack) {
        ChengYueNBT.init(stack);
        int level = ChengYueNBT.getLevel(stack);
        return level >= 10; // 10级以上发光
    }
}