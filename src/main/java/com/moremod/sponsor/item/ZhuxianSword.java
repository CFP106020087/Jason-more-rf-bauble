package com.moremod.sponsor.item;

import com.moremod.util.combat.TrueDamageHelper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 诛仙四剑 - 四形态剑
 *
 * 形态1: 诛仙 - 基础伤害15，击杀获得力量2+急迫3，每10击杀+1伤害(最高40)
 * 形态2: 戮仙 - 基础伤害40，急迫9+力量5，每10击杀获得范围伤害
 * 形态3: 陷仙 - 基础伤害50，每次攻击范围伤害，力量15+急迫9+抗性，击杀免疫一次伤害
 * 形态4: 绝仙 - 基础伤害100，可切换武器类型，20%流血，800%暴击，免疫所有debuff，可开启诛仙剑阵
 *
 * 横渠四句技能:
 * - 为天地立心: 抗性7，+30%经验，-20%附魔消耗
 * - 为生民立命: 村民无敌，绿宝石特价，血量不低于20%
 * - 为往圣继绝学: 村民附近敌对生物每秒5%真伤
 * - 为万世开太平: 右键长按3秒，太平领域10秒
 */
public class ZhuxianSword extends ItemSword {

    // NBT Keys
    public static final String NBT_FORM = "ZhuxianForm";
    public static final String NBT_KILL_COUNT = "ZhuxianKillCount";
    public static final String NBT_TOTAL_KILLS = "ZhuxianTotalKills";
    public static final String NBT_BONUS_DAMAGE = "ZhuxianBonusDamage";
    public static final String NBT_HAS_AOE = "ZhuxianHasAoe";
    public static final String NBT_IMMUNITY_CHARGES = "ZhuxianImmunityCharges";
    public static final String NBT_WEAPON_TYPE = "ZhuxianWeaponType";
    public static final String NBT_FORMATION_ACTIVE = "ZhuxianFormationActive";
    public static final String NBT_SKILL_TIANXIN = "ZhuxianSkillTianxin";
    public static final String NBT_SKILL_LIMING = "ZhuxianSkillLiming";
    public static final String NBT_SKILL_JUEXUE = "ZhuxianSkillJuexue";
    public static final String NBT_SKILL_TAIPING = "ZhuxianSkillTaiping";
    public static final String NBT_TAIPING_END_TIME = "ZhuxianTaipingEndTime";

    // 形态枚举
    public enum SwordForm {
        ZHUXIAN(0, "诛仙", 15.0f, TextFormatting.RED),
        LUXIAN(1, "戮仙", 40.0f, TextFormatting.DARK_RED),
        XIANXIAN(2, "陷仙", 50.0f, TextFormatting.GOLD),
        JUEXIAN(3, "绝仙", 100.0f, TextFormatting.DARK_PURPLE);

        public final int id;
        public final String name;
        public final float baseDamage;
        public final TextFormatting color;

        SwordForm(int id, String name, float baseDamage, TextFormatting color) {
            this.id = id;
            this.name = name;
            this.baseDamage = baseDamage;
            this.color = color;
        }

        public static SwordForm fromId(int id) {
            for (SwordForm form : values()) {
                if (form.id == id) return form;
            }
            return ZHUXIAN;
        }
    }

    // 武器类型（绝仙形态可切换）
    public enum WeaponType {
        SWORD(0, "剑", 1.6f),
        HAMMER(1, "战锤", 0.8f),
        AXE(2, "斧头", 1.0f),
        SCYTHE(3, "镰刀", 1.4f);

        public final int id;
        public final String name;
        public final float attackSpeed;

        WeaponType(int id, String name, float attackSpeed) {
            this.id = id;
            this.name = name;
            this.attackSpeed = attackSpeed;
        }

        public static WeaponType fromId(int id) {
            for (WeaponType type : values()) {
                if (type.id == id) return type;
            }
            return SWORD;
        }

        public WeaponType next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public ZhuxianSword() {
        super(ToolMaterial.DIAMOND);
        this.setRegistryName("moremod", "zhuxian_sword");
        this.setTranslationKey("moremod.zhuxian_sword");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setMaxDamage(-1); // 不可破坏
    }

    // ==================== NBT 操作 ====================

    private NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    public SwordForm getForm(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        return SwordForm.fromId(tag.getInteger(NBT_FORM));
    }

    public void setForm(ItemStack stack, SwordForm form) {
        getOrCreateTag(stack).setInteger(NBT_FORM, form.id);
    }

    public int getKillCount(ItemStack stack) {
        return getOrCreateTag(stack).getInteger(NBT_KILL_COUNT);
    }

    public void addKill(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        int kills = tag.getInteger(NBT_KILL_COUNT) + 1;
        int totalKills = tag.getInteger(NBT_TOTAL_KILLS) + 1;
        tag.setInteger(NBT_KILL_COUNT, kills);
        tag.setInteger(NBT_TOTAL_KILLS, totalKills);

        SwordForm form = getForm(stack);

        // 诛仙形态：每10击杀+1伤害（最高40）
        if (form == SwordForm.ZHUXIAN && kills >= 10) {
            tag.setInteger(NBT_KILL_COUNT, 0);
            float bonus = tag.getFloat(NBT_BONUS_DAMAGE);
            if (bonus < 25) { // 15 + 25 = 40
                tag.setFloat(NBT_BONUS_DAMAGE, bonus + 1);
            }
        }

        // 戮仙形态：每10击杀获得范围伤害
        if (form == SwordForm.LUXIAN && kills >= 10) {
            tag.setInteger(NBT_KILL_COUNT, 0);
            if (!tag.getBoolean(NBT_HAS_AOE)) {
                tag.setBoolean(NBT_HAS_AOE, true);
            }
        }

        // 陷仙形态：每次击杀获得免疫一次伤害
        if (form == SwordForm.XIANXIAN) {
            int charges = tag.getInteger(NBT_IMMUNITY_CHARGES);
            tag.setInteger(NBT_IMMUNITY_CHARGES, charges + 1);
        }
    }

    public float getBonusDamage(ItemStack stack) {
        return getOrCreateTag(stack).getFloat(NBT_BONUS_DAMAGE);
    }

    public boolean hasAoe(ItemStack stack) {
        SwordForm form = getForm(stack);
        if (form == SwordForm.XIANXIAN || form == SwordForm.JUEXIAN) {
            return true; // 陷仙和绝仙始终有范围伤害
        }
        return getOrCreateTag(stack).getBoolean(NBT_HAS_AOE);
    }

    public int getImmunityCharges(ItemStack stack) {
        return getOrCreateTag(stack).getInteger(NBT_IMMUNITY_CHARGES);
    }

    public boolean consumeImmunityCharge(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        int charges = tag.getInteger(NBT_IMMUNITY_CHARGES);
        if (charges > 0) {
            tag.setInteger(NBT_IMMUNITY_CHARGES, charges - 1);
            return true;
        }
        return false;
    }

    public WeaponType getWeaponType(ItemStack stack) {
        return WeaponType.fromId(getOrCreateTag(stack).getInteger(NBT_WEAPON_TYPE));
    }

    public void setWeaponType(ItemStack stack, WeaponType type) {
        getOrCreateTag(stack).setInteger(NBT_WEAPON_TYPE, type.id);
    }

    public boolean isFormationActive(ItemStack stack) {
        return getOrCreateTag(stack).getBoolean(NBT_FORMATION_ACTIVE);
    }

    public void setFormationActive(ItemStack stack, boolean active) {
        getOrCreateTag(stack).setBoolean(NBT_FORMATION_ACTIVE, active);
    }

    // 技能开关
    public boolean isSkillActive(ItemStack stack, String skillKey) {
        return getOrCreateTag(stack).getBoolean(skillKey);
    }

    public void toggleSkill(ItemStack stack, String skillKey) {
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setBoolean(skillKey, !tag.getBoolean(skillKey));
    }

    // ==================== 伤害计算 ====================

    public float getTotalDamage(ItemStack stack) {
        SwordForm form = getForm(stack);
        float damage = form.baseDamage;

        // 诛仙形态的击杀加成
        if (form == SwordForm.ZHUXIAN) {
            damage += getBonusDamage(stack);
        }

        return damage;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();

        if (slot == EntityEquipmentSlot.MAINHAND) {
            float damage = getTotalDamage(stack);
            WeaponType weaponType = getWeaponType(stack);
            float attackSpeed = weaponType.attackSpeed;

            // 绝仙形态根据武器类型调整攻速
            if (getForm(stack) == SwordForm.JUEXIAN) {
                attackSpeed = weaponType.attackSpeed;
            }

            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", damage, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", attackSpeed - 4.0, 0));
        }

        return modifiers;
    }

    // ==================== 右键交互 ====================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 潜行+右键：切换形态
        if (player.isSneaking()) {
            if (!world.isRemote) {
                SwordForm currentForm = getForm(stack);
                SwordForm nextForm = SwordForm.fromId((currentForm.id + 1) % SwordForm.values().length);
                setForm(stack, nextForm);
                player.sendStatusMessage(new TextComponentString(
                    nextForm.color + "切换至: " + nextForm.name + TextFormatting.GRAY + " (基础伤害: " + nextForm.baseDamage + ")"
                ), true);
            }
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        }

        // 绝仙形态：右键切换武器类型
        if (getForm(stack) == SwordForm.JUEXIAN) {
            if (!world.isRemote) {
                WeaponType currentType = getWeaponType(stack);
                WeaponType nextType = currentType.next();
                setWeaponType(stack, nextType);
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "武器形态: " + nextType.name
                ), true);
            }
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        }

        // 为万世开太平技能：长按右键
        if (isSkillActive(stack, NBT_SKILL_TAIPING)) {
            player.setActiveHand(hand);
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        }

        return ActionResult.newResult(EnumActionResult.PASS, stack);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        if (isSkillActive(stack, NBT_SKILL_TAIPING)) {
            return EnumAction.BOW; // 长按效果
        }
        return EnumAction.NONE;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000; // 长时间
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
        if (!(player instanceof EntityPlayer)) return;
        EntityPlayer entityPlayer = (EntityPlayer) player;

        int usedTicks = getMaxItemUseDuration(stack) - count;

        // 为万世开太平：长按3秒（60 ticks）
        if (isSkillActive(stack, NBT_SKILL_TAIPING) && usedTicks >= 60) {
            if (!player.world.isRemote) {
                // 检查经验
                if (entityPlayer.experienceLevel >= 10) {
                    entityPlayer.addExperienceLevel(-10);

                    // 设置太平领域结束时间
                    long endTime = player.world.getTotalWorldTime() + 200; // 10秒
                    getOrCreateTag(stack).setLong(NBT_TAIPING_END_TIME, endTime);

                    entityPlayer.sendStatusMessage(new TextComponentString(
                        TextFormatting.AQUA + "☮ 太平领域已激活！(10秒)"
                    ), true);
                } else {
                    entityPlayer.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "需要10级经验！"
                    ), false);
                }
            }
            entityPlayer.stopActiveHand();
        }
    }

    // ==================== 攻击处理 ====================

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!(attacker instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer) attacker;

        SwordForm form = getForm(stack);

        // 无视无敌帧（所有形态）
        target.hurtResistantTime = 0;

        // 计算真伤
        float damage = getTotalDamage(stack);

        // 绝仙形态：800%暴击
        if (form == SwordForm.JUEXIAN) {
            if (player.getRNG().nextFloat() < 0.3f) { // 30%暴击率
                damage *= 8.0f;
                if (!player.world.isRemote) {
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "★ 暴击！" + TextFormatting.RED + String.format("%.1f", damage) + " 伤害"
                    ), true);
                }
            }
        }

        // 应用真伤
        TrueDamageHelper.applyWrappedTrueDamage(target, player, damage, TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

        // 范围伤害
        if (hasAoe(stack)) {
            float aoeDamage = damage * 0.5f; // 范围伤害为50%
            dealAoeTrueDamage(player, target, 5.0, aoeDamage);
        }

        // 绝仙形态：20%最大生命流血
        if (form == SwordForm.JUEXIAN) {
            // 流血效果在事件处理器中实现
            target.getEntityData().setBoolean("ZhuxianBleeding", true);
            target.getEntityData().setLong("ZhuxianBleedEndTime", player.world.getTotalWorldTime() + 100);
        }

        // 应用BUFF给玩家
        applyFormBuffs(player, form, stack);

        return true;
    }

    /**
     * 根据形态应用BUFF
     */
    private void applyFormBuffs(EntityPlayer player, SwordForm form, ItemStack stack) {
        switch (form) {
            case ZHUXIAN:
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, 1)); // 力量2
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 2)); // 急迫3
                break;
            case LUXIAN:
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, 4)); // 力量5
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 8)); // 急迫9
                break;
            case XIANXIAN:
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, 14)); // 力量15
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 8)); // 急迫9
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 200, 3)); // 抗性4
                break;
            case JUEXIAN:
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, 14)); // 力量15
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 8)); // 急迫9
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 200, 6)); // 抗性7
                break;
        }

        // 横渠四句技能 - 为天地立心：抗性7（单手持剑时）
        if (isSkillActive(stack, NBT_SKILL_TIANXIN)) {
            if (player.getHeldItemOffhand().isEmpty()) {
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 6)); // 抗性7
            }
        }
    }

    // ==================== 被动效果 ====================

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (!(entity instanceof EntityPlayer)) return;
        if (world.isRemote) return;

        EntityPlayer player = (EntityPlayer) entity;

        // 只有手持时生效
        if (!isSelected && player.getHeldItemOffhand() != stack) return;

        SwordForm form = getForm(stack);

        // 绝仙形态：清除所有debuff
        if (form == SwordForm.JUEXIAN) {
            player.getActivePotionEffects().removeIf(effect -> {
                return effect.getPotion().isBadEffect();
            });
        }

        // 诛仙剑阵
        if (form == SwordForm.JUEXIAN && isFormationActive(stack)) {
            if (world.getTotalWorldTime() % 20 == 0) { // 每秒
                dealAoeTrueDamage(player, null, 10.0, 999999);
            }
        }

        // 太平领域效果
        long taipingEndTime = getOrCreateTag(stack).getLong(NBT_TAIPING_END_TIME);
        if (taipingEndTime > world.getTotalWorldTime()) {
            // 移动速度+20%
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 40, 0));

            // 领域内敌对生物停止攻击（在事件处理器中实现）
        }
    }

    // ==================== Tooltip ====================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        SwordForm form = getForm(stack);

        // 主标题
        tooltip.add(form.color + "【" + form.name + "】" + TextFormatting.GRAY + " - 四剑之一");
        tooltip.add("");

        // 诗句
        tooltip.add(TextFormatting.DARK_RED + "\"诛仙\"利，\"戮仙\"亡，");
        tooltip.add(TextFormatting.DARK_RED + "\"陷仙\"四处起红光，");
        tooltip.add(TextFormatting.DARK_RED + "\"绝仙\"大罗神仙血染裳。");
        tooltip.add("");

        // 当前状态
        tooltip.add(TextFormatting.YELLOW + "▶ 当前形态: " + form.color + form.name);
        tooltip.add(TextFormatting.GRAY + "  基础伤害: " + TextFormatting.RED + String.format("%.1f", getTotalDamage(stack)));

        if (form == SwordForm.ZHUXIAN) {
            tooltip.add(TextFormatting.GRAY + "  击杀加成: " + TextFormatting.GREEN + "+" + getBonusDamage(stack));
            tooltip.add(TextFormatting.GRAY + "  击杀进度: " + TextFormatting.WHITE + getKillCount(stack) + "/10");
        }

        if (form == SwordForm.LUXIAN) {
            tooltip.add(TextFormatting.GRAY + "  范围伤害: " + (hasAoe(stack) ? TextFormatting.GREEN + "已解锁" : TextFormatting.RED + "未解锁 (" + getKillCount(stack) + "/10)"));
        }

        if (form == SwordForm.XIANXIAN) {
            tooltip.add(TextFormatting.GRAY + "  免疫次数: " + TextFormatting.AQUA + getImmunityCharges(stack));
        }

        if (form == SwordForm.JUEXIAN) {
            tooltip.add(TextFormatting.GRAY + "  武器类型: " + TextFormatting.LIGHT_PURPLE + getWeaponType(stack).name);
            tooltip.add(TextFormatting.GRAY + "  剑阵: " + (isFormationActive(stack) ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
        }

        tooltip.add("");

        // 形态效果说明
        tooltip.add(TextFormatting.GOLD + "◆ 形态效果:");
        switch (form) {
            case ZHUXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 击杀获得力量II+急迫III");
                tooltip.add(TextFormatting.GRAY + "  - 每10击杀+1伤害(最高40)");
                tooltip.add(TextFormatting.GRAY + "  - 无视无敌帧，真实伤害");
                break;
            case LUXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 获得急迫IX+力量V");
                tooltip.add(TextFormatting.GRAY + "  - 每10击杀解锁范围伤害");
                tooltip.add(TextFormatting.GRAY + "  - 无视无敌帧，真实伤害");
                break;
            case XIANXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 每次攻击造成范围伤害");
                tooltip.add(TextFormatting.GRAY + "  - 获得力量XV+急迫IX+抗性IV");
                tooltip.add(TextFormatting.GRAY + "  - 击杀获得免疫一次伤害");
                break;
            case JUEXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 可切换武器类型(右键)");
                tooltip.add(TextFormatting.GRAY + "  - 20%最大生命流血");
                tooltip.add(TextFormatting.GRAY + "  - 800%暴击伤害");
                tooltip.add(TextFormatting.GRAY + "  - 免疫所有负面效果");
                tooltip.add(TextFormatting.GRAY + "  - 可开启诛仙剑阵(999999真伤)");
                break;
        }

        tooltip.add("");

        // 横渠四句
        tooltip.add(TextFormatting.AQUA + "◆ 横渠四句 (主动技能):");
        tooltip.add(TextFormatting.WHITE + "  为天地立心，为生民立命，");
        tooltip.add(TextFormatting.WHITE + "  为往圣继绝学，为万世开太平。");
        tooltip.add("");
        tooltip.add(skillStatus(stack, NBT_SKILL_TIANXIN, "为天地立心", "抗性VII+30%经验-20%附魔消耗"));
        tooltip.add(skillStatus(stack, NBT_SKILL_LIMING, "为生民立命", "村民无敌+特价+血量≥20%"));
        tooltip.add(skillStatus(stack, NBT_SKILL_JUEXUE, "为往圣继绝学", "村民附近敌人5%/秒真伤"));
        tooltip.add(skillStatus(stack, NBT_SKILL_TAIPING, "为万世开太平", "太平领域(右键长按3秒)"));

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "潜行+右键: 切换形态");
        tooltip.add(TextFormatting.DARK_GRAY + "使用快捷键切换技能");
    }

    private String skillStatus(ItemStack stack, String key, String name, String desc) {
        boolean active = isSkillActive(stack, key);
        return (active ? TextFormatting.GREEN + "  ✓ " : TextFormatting.DARK_GRAY + "  ○ ") +
               TextFormatting.YELLOW + name + TextFormatting.GRAY + " - " + desc;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        SwordForm form = getForm(stack);
        return form.color + form.name + "剑";
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getForm(stack) == SwordForm.JUEXIAN || isFormationActive(stack);
    }

    // ==================== AOE伤害辅助方法 ====================

    /**
     * 对范围内的敌对生物造成真伤
     */
    private void dealAoeTrueDamage(EntityPlayer attacker, EntityLivingBase center, double radius, float damage) {
        EntityLivingBase centerEntity = center != null ? center : attacker;
        if (centerEntity.world.isRemote) return;

        centerEntity.world.getEntitiesWithinAABB(EntityLivingBase.class,
            centerEntity.getEntityBoundingBox().grow(radius),
            entity -> entity != attacker && entity != center && !entity.isDead && entity instanceof EntityMob
        ).forEach(entity -> {
            double dist = entity.getDistance(centerEntity);
            if (dist <= radius) {
                TrueDamageHelper.applyWrappedTrueDamage(entity, attacker, damage, TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
            }
        });
    }

    // ==================== 静态工具方法 ====================

    /**
     * 检查玩家是否持有诛仙剑
     */
    public static ItemStack getZhuxianSword(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        if (main.getItem() instanceof ZhuxianSword) {
            return main;
        }
        ItemStack off = player.getHeldItemOffhand();
        if (off.getItem() instanceof ZhuxianSword) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 检查技能是否激活
     */
    public static boolean isPlayerSkillActive(EntityPlayer player, String skillKey) {
        ItemStack sword = getZhuxianSword(player);
        if (sword.isEmpty()) return false;
        return ((ZhuxianSword) sword.getItem()).isSkillActive(sword, skillKey);
    }
}
