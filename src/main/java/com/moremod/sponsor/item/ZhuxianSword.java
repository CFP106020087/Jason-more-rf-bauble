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
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // 形态解锁追踪（每个形态需持有20秒=400 ticks）
    public static final String NBT_FORM_TIME_ZHUXIAN = "ZhuxianFormTimeZhuxian";
    public static final String NBT_FORM_TIME_LUXIAN = "ZhuxianFormTimeLuxian";
    public static final String NBT_FORM_TIME_XIANXIAN = "ZhuxianFormTimeXianxian";
    public static final String NBT_FORM_TIME_JUEXIAN = "ZhuxianFormTimeJuexian";
    public static final String NBT_GUIXU_UNLOCKED = "ZhuxianGuixuUnlocked";
    public static final int FORM_UNLOCK_TIME = 400; // 20秒 = 400 ticks

    // 形态枚举
    public enum SwordForm {
        ZHUXIAN(0, "诛仙", 15.0f, TextFormatting.RED),
        LUXIAN(1, "戮仙", 40.0f, TextFormatting.DARK_RED),
        XIANXIAN(2, "陷仙", 50.0f, TextFormatting.GOLD),
        JUEXIAN(3, "绝仙", 100.0f, TextFormatting.DARK_PURPLE),
        GUIXU(4, "归墟", 999.0f, TextFormatting.DARK_GRAY);  // 第五形态：归墟

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

        /**
         * 获取该形态对应的技能Key
         * 诛仙->不死, 戮仙->范围真伤, 陷仙->太平领域, 绝仙->剑阵, 归墟->灭世
         */
        public String getBoundSkillKey() {
            switch (this) {
                case ZHUXIAN: return NBT_SKILL_LIMING;      // 不死锁血
                case LUXIAN: return NBT_SKILL_JUEXUE;       // 范围真伤
                case XIANXIAN: return NBT_SKILL_TAIPING;    // 太平领域+村民保护
                case JUEXIAN: return NBT_FORMATION_ACTIVE;  // 剑阵
                case GUIXU: return NBT_FORMATION_ACTIVE;    // 灭世（复用剑阵开关）
                default: return null;
            }
        }

        /**
         * 获取该形态对应的技能名称
         */
        public String getBoundSkillName() {
            switch (this) {
                case ZHUXIAN: return "为生民立命";   // 不死
                case LUXIAN: return "为往圣继绝学"; // 范围真伤
                case XIANXIAN: return "为万世开太平"; // 太平领域
                case JUEXIAN: return "诛仙剑阵";    // 剑阵
                case GUIXU: return "归墟灭世";      // 灰烬化
                default: return "";
            }
        }
    }

    /**
     * 获取技能对应的形态
     */
    public static SwordForm getSkillBoundForm(String skillKey) {
        if (NBT_SKILL_LIMING.equals(skillKey)) return SwordForm.ZHUXIAN;
        if (NBT_SKILL_JUEXUE.equals(skillKey)) return SwordForm.LUXIAN;
        if (NBT_SKILL_TAIPING.equals(skillKey)) return SwordForm.XIANXIAN;
        if (NBT_FORMATION_ACTIVE.equals(skillKey)) return SwordForm.JUEXIAN;
        return null;
    }

    /**
     * 检查技能是否可以在当前形态使用
     */
    public static boolean canUseSkillInForm(String skillKey, SwordForm currentForm) {
        SwordForm requiredForm = getSkillBoundForm(skillKey);
        return requiredForm == null || requiredForm == currentForm;
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

    // ==================== 技能状态备份（防止物品掉落导致检测失败） ====================
    // 参考香巴拉的 shambhalaBackup 实现

    /** 诛仙剑持有者UUID备份（物品可能掉落，但状态仍然有效） */
    private static final Set<UUID> zhuxianHolderBackup = new HashSet<>();

    /** 技能状态备份：玩家UUID -> 技能Key -> 是否激活 */
    private static final Map<UUID, Map<String, Boolean>> skillStateBackup = new HashMap<>();

    /** 形态备份：玩家UUID -> 当前形态 */
    private static final Map<UUID, SwordForm> formBackup = new HashMap<>();

    /**
     * 注册玩家为诛仙剑持有者（当玩家装备剑时调用）
     */
    public static void registerHolder(EntityPlayer player) {
        zhuxianHolderBackup.add(player.getUniqueID());
    }

    /**
     * 取消注册诛仙剑持有者（当玩家死亡后或主动放弃时调用）
     */
    public static void unregisterHolder(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        zhuxianHolderBackup.remove(playerId);
        skillStateBackup.remove(playerId);
        formBackup.remove(playerId);
    }

    /**
     * 更新形态备份
     */
    public static void updateFormBackup(EntityPlayer player, SwordForm form) {
        formBackup.put(player.getUniqueID(), form);
    }

    /**
     * 获取备份的形态
     */
    public static SwordForm getFormFromBackup(EntityPlayer player) {
        return formBackup.get(player.getUniqueID());
    }

    /**
     * 检查玩家是否是诛仙剑持有者（包括备份检查）
     */
    public static boolean isHolder(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        // 优先检查备份
        if (zhuxianHolderBackup.contains(playerId)) {
            return true;
        }
        // 检查是否实际持有
        ItemStack sword = getZhuxianSword(player);
        if (!sword.isEmpty()) {
            zhuxianHolderBackup.add(playerId);
            return true;
        }
        return false;
    }

    /**
     * 更新技能状态备份
     */
    public static void updateSkillBackup(EntityPlayer player, String skillKey, boolean active) {
        UUID playerId = player.getUniqueID();
        skillStateBackup.computeIfAbsent(playerId, k -> new HashMap<>()).put(skillKey, active);
    }

    /**
     * 从备份获取技能状态
     */
    public static boolean getSkillFromBackup(EntityPlayer player, String skillKey) {
        UUID playerId = player.getUniqueID();
        Map<String, Boolean> skills = skillStateBackup.get(playerId);
        if (skills != null) {
            return skills.getOrDefault(skillKey, false);
        }
        return false;
    }

    /**
     * 清理玩家状态（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        zhuxianHolderBackup.remove(playerId);
        skillStateBackup.remove(playerId);
        formBackup.remove(playerId);
    }

    /**
     * 清空所有状态（世界卸载时调用）
     */
    public static void clearAllState() {
        zhuxianHolderBackup.clear();
        skillStateBackup.clear();
        formBackup.clear();
    }

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

    /**
     * 切换技能并同步到备份
     * 必须使用此方法来确保死亡保护正常工作
     */
    public void toggleSkillWithBackup(ItemStack stack, String skillKey, EntityPlayer player) {
        NBTTagCompound tag = getOrCreateTag(stack);
        boolean newState = !tag.getBoolean(skillKey);
        tag.setBoolean(skillKey, newState);

        // 同步到备份
        updateSkillBackup(player, skillKey, newState);
        registerHolder(player);
    }

    /**
     * 同步剑的所有状态到备份（技能+形态）
     * 在玩家装备剑或技能改变时调用
     */
    public void syncAllSkillsToBackup(ItemStack stack, EntityPlayer player) {
        registerHolder(player);
        // 同步形态
        updateFormBackup(player, getForm(stack));
        // 同步技能
        updateSkillBackup(player, NBT_SKILL_TIANXIN, isSkillActive(stack, NBT_SKILL_TIANXIN));
        updateSkillBackup(player, NBT_SKILL_LIMING, isSkillActive(stack, NBT_SKILL_LIMING));
        updateSkillBackup(player, NBT_SKILL_JUEXUE, isSkillActive(stack, NBT_SKILL_JUEXUE));
        updateSkillBackup(player, NBT_SKILL_TAIPING, isSkillActive(stack, NBT_SKILL_TAIPING));
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
                SwordForm nextForm = getNextAvailableForm(stack, currentForm);
                setForm(stack, nextForm);

                // 解锁归墟时特殊提示
                if (nextForm == SwordForm.GUIXU) {
                    player.sendStatusMessage(new TextComponentString(
                        nextForm.color + "【" + nextForm.name + "】" + TextFormatting.DARK_RED + " 万物归于虚无..."
                    ), true);
                } else {
                    player.sendStatusMessage(new TextComponentString(
                        nextForm.color + "切换至: " + nextForm.name + TextFormatting.GRAY + " (基础伤害: " + nextForm.baseDamage + ")"
                    ), true);
                }
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

        // 诛仙形态：真伤流血（20%最大生命，5秒）
        if (form == SwordForm.ZHUXIAN && isSkillActive(stack, NBT_SKILL_LIMING)) {
            target.getEntityData().setBoolean("ZhuxianBleeding", true);
            target.getEntityData().setLong("ZhuxianBleedEndTime", player.world.getTotalWorldTime() + 100);
        }

        // 戮仙形态：范围真伤打击（攻击时周围8格敌人受到50%真伤）
        if (form == SwordForm.LUXIAN && isSkillActive(stack, NBT_SKILL_JUEXUE)) {
            float aoeDamage = damage * 0.5f;
            dealAoeTrueDamage(player, target, 8.0, aoeDamage);
        }

        // 其他形态的范围伤害
        if (form != SwordForm.LUXIAN && hasAoe(stack)) {
            float aoeDamage = damage * 0.5f;
            dealAoeTrueDamage(player, target, 5.0, aoeDamage);
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

        // 同步技能状态到备份（每5秒同步一次，避免性能问题）
        if (world.getTotalWorldTime() % 100 == 0) {
            syncAllSkillsToBackup(stack, player);
        }

        SwordForm form = getForm(stack);

        // 追踪形态持有时间（用于解锁归墟）
        trackFormHoldTime(stack, form);

        // 绝仙形态：清除所有debuff
        if (form == SwordForm.JUEXIAN || form == SwordForm.GUIXU) {
            player.getActivePotionEffects().removeIf(effect -> {
                return effect.getPotion().isBadEffect();
            });
        }

        // 诛仙剑阵：对周围所有生物打雷 + 999999真伤
        if (form == SwordForm.JUEXIAN && isFormationActive(stack)) {
            if (world.getTotalWorldTime() % 20 == 0) { // 每秒
                summonFormationLightning(player, world, 10.0);
            }
        }

        // 归墟灭世：直接灰烬化周围生物
        if (form == SwordForm.GUIXU && isFormationActive(stack)) {
            if (world.getTotalWorldTime() % 20 == 0) { // 每秒
                disintegrateNearbyEntities(player, world, 60.0);
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

        // 形态效果说明（含绑定技能）
        tooltip.add(TextFormatting.GOLD + "◆ 形态效果:");
        switch (form) {
            case ZHUXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 击杀获得力量II+急迫III");
                tooltip.add(TextFormatting.GRAY + "  - 每10击杀+1伤害(最高40)");
                tooltip.add(TextFormatting.GRAY + "  - 无视无敌帧，真实伤害");
                tooltip.add(TextFormatting.GREEN + "  ◇ 为生民立命: " + TextFormatting.WHITE + "锁血20%不死 + 真伤流血");
                tooltip.add(skillFormStatus(stack, form));
                break;
            case LUXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 获得急迫IX+力量V");
                tooltip.add(TextFormatting.GRAY + "  - 无视无敌帧，真实伤害");
                tooltip.add(TextFormatting.GREEN + "  ◇ 为往圣继绝学: " + TextFormatting.WHITE + "攻击时8格范围50%真伤");
                tooltip.add(skillFormStatus(stack, form));
                break;
            case XIANXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 每次攻击造成范围伤害");
                tooltip.add(TextFormatting.GRAY + "  - 获得力量XV+急迫IX+抗性IV");
                tooltip.add(TextFormatting.GRAY + "  - 击杀获得免疫一次伤害");
                tooltip.add(TextFormatting.GREEN + "  ◇ 为万世开太平: " + TextFormatting.WHITE + "和平领域+怪物混乱+村民保护");
                tooltip.add(skillFormStatus(stack, form));
                break;
            case JUEXIAN:
                tooltip.add(TextFormatting.GRAY + "  - 可切换武器类型(右键)");
                tooltip.add(TextFormatting.GRAY + "  - 800%暴击伤害");
                tooltip.add(TextFormatting.GRAY + "  - 免疫所有负面效果");
                tooltip.add(TextFormatting.GREEN + "  ◇ 诛仙剑阵: " + TextFormatting.WHITE + "雷击+999999真伤(除村民)");
                tooltip.add(TextFormatting.GRAY + "    状态: " + (isFormationActive(stack) ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
                break;
            case GUIXU:
                tooltip.add(TextFormatting.GRAY + "  - 免疫所有负面效果");
                tooltip.add(TextFormatting.GRAY + "  - 万物归于虚无");
                tooltip.add(TextFormatting.DARK_RED + "  ◇ 归墟灭世: " + TextFormatting.WHITE + "60格灰烬化(掉落灰烬)");
                tooltip.add(TextFormatting.GRAY + "    状态: " + (isFormationActive(stack) ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭"));
                break;
        }

        // 归墟解锁进度（只在非归墟形态显示）
        if (form != SwordForm.GUIXU) {
            if (isGuixuUnlocked(stack)) {
                tooltip.add(TextFormatting.DARK_GRAY + "◆ 归墟: " + TextFormatting.GREEN + "已解锁");
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "◆ 归墟: " + TextFormatting.YELLOW + String.format("%.1f%%", getUnlockProgress(stack)) + TextFormatting.GRAY + " (每形态持有20秒)");
            }
        }

        tooltip.add("");

        // 横渠四句诗词
        tooltip.add(TextFormatting.AQUA + "◆ 横渠四句:");
        tooltip.add(TextFormatting.WHITE + "  为天地立心，为生民立命，");
        tooltip.add(TextFormatting.WHITE + "  为往圣继绝学，为万世开太平。");

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "潜行+右键: 切换形态");
        tooltip.add(TextFormatting.DARK_GRAY + "快捷键: 切换当前形态技能");
    }

    /**
     * 显示形态绑定技能状态
     */
    private String skillFormStatus(ItemStack stack, SwordForm form) {
        String skillKey = form.getBoundSkillKey();
        boolean active = isSkillActive(stack, skillKey);
        return TextFormatting.GRAY + "    状态: " + (active ? TextFormatting.GREEN + "开启" : TextFormatting.RED + "关闭");
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

    /**
     * 诛仙剑阵：对周围所有生物召唤雷电 + 999999真伤
     * 每秒召唤一次，对范围内所有生物（除玩家外）
     */
    private void summonFormationLightning(EntityPlayer player, World world, double radius) {
        if (world.isRemote) return;

        // 获取范围内所有生物（除了玩家和村民）
        world.getEntitiesWithinAABB(EntityLivingBase.class,
            player.getEntityBoundingBox().grow(radius),
            entity -> entity != player && !entity.isDead
                && !(entity instanceof EntityPlayer)
                && !(entity instanceof EntityVillager)  // 保护村民
        ).forEach(entity -> {
            // 召唤雷电打在目标位置
            EntityLightningBolt lightning = new EntityLightningBolt(world, entity.posX, entity.posY, entity.posZ, false);
            world.addWeatherEffect(lightning);

            // 造成999999真伤（雷击后立即真伤）
            TrueDamageHelper.applyWrappedTrueDamage(entity, player, 999999f, TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
        });
    }

    /**
     * 归墟灭世：对周围生物进行灰烬化（直接删除，无掉落）
     */
    private void disintegrateNearbyEntities(EntityPlayer player, World world, double radius) {
        if (world.isRemote) return;

        // 获取范围内所有生物（除了玩家和村民）
        world.getEntitiesWithinAABB(EntityLivingBase.class,
            player.getEntityBoundingBox().grow(radius),
            entity -> entity != player && !entity.isDead
                && !(entity instanceof EntityPlayer)
                && !(entity instanceof EntityVillager)
        ).forEach(entity -> {
            disintegrate(entity, world);
        });
    }

    /**
     * 灰烬化单个实体
     * 直接删除实体，生成大量灰烬粒子效果，掉落灰烬（火药）
     */
    private void disintegrate(EntityLivingBase entity, World world) {
        if (world.isRemote) return;

        double x = entity.posX;
        double y = entity.posY + entity.height / 2;
        double z = entity.posZ;

        // 灰烬粒子爆炸
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            // 大量烟雾（灰烬）
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z,
                    80, 0.6, 0.6, 0.6, 0.15);

            // 火焰残渣
            ws.spawnParticle(EnumParticleTypes.FLAME, x, y, z,
                    40, 0.4, 0.4, 0.4, 0.08);

            // 末地烬效果（紫色）
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, x, y, z,
                    30, 0.5, 0.5, 0.5, 0.03);

            // 爆炸粒子
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, x, y, z,
                    5, 0.3, 0.3, 0.3, 0);
        }

        // 音效：烈焰人死亡 + 虚空音效
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1.5f, 0.3f);
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);

        // 掉落灰烬（火药作为灰烬替代物）
        // 根据实体大小决定掉落数量：1-3个
        int ashCount = 1 + world.rand.nextInt(3);
        entity.entityDropItem(new ItemStack(Items.GUNPOWDER, ashCount), 0.0f);

        // 直接移除实体 - 无死亡动画、无敌也没用
        entity.setDead();
    }

    // ==================== 形态解锁系统 ====================

    /**
     * 追踪形态持有时间
     */
    private void trackFormHoldTime(ItemStack stack, SwordForm form) {
        if (form == SwordForm.GUIXU) return; // 归墟形态不需要追踪

        NBTTagCompound tag = getOrCreateTag(stack);
        String timeKey = getFormTimeKey(form);
        if (timeKey == null) return;

        int time = tag.getInteger(timeKey);
        if (time < FORM_UNLOCK_TIME) {
            tag.setInteger(timeKey, time + 1);

            // 检查是否所有形态都达到20秒
            checkAndUnlockGuixu(stack);
        }
    }

    /**
     * 获取形态对应的时间追踪NBT键
     */
    private String getFormTimeKey(SwordForm form) {
        switch (form) {
            case ZHUXIAN: return NBT_FORM_TIME_ZHUXIAN;
            case LUXIAN: return NBT_FORM_TIME_LUXIAN;
            case XIANXIAN: return NBT_FORM_TIME_XIANXIAN;
            case JUEXIAN: return NBT_FORM_TIME_JUEXIAN;
            default: return null;
        }
    }

    /**
     * 检查并解锁归墟形态
     */
    private void checkAndUnlockGuixu(ItemStack stack) {
        if (isGuixuUnlocked(stack)) return;

        NBTTagCompound tag = getOrCreateTag(stack);
        int t1 = tag.getInteger(NBT_FORM_TIME_ZHUXIAN);
        int t2 = tag.getInteger(NBT_FORM_TIME_LUXIAN);
        int t3 = tag.getInteger(NBT_FORM_TIME_XIANXIAN);
        int t4 = tag.getInteger(NBT_FORM_TIME_JUEXIAN);

        if (t1 >= FORM_UNLOCK_TIME && t2 >= FORM_UNLOCK_TIME &&
            t3 >= FORM_UNLOCK_TIME && t4 >= FORM_UNLOCK_TIME) {
            tag.setBoolean(NBT_GUIXU_UNLOCKED, true);
        }
    }

    /**
     * 检查归墟是否已解锁
     */
    public boolean isGuixuUnlocked(ItemStack stack) {
        return getOrCreateTag(stack).getBoolean(NBT_GUIXU_UNLOCKED);
    }

    /**
     * 获取解锁进度百分比
     */
    public float getUnlockProgress(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        int t1 = Math.min(tag.getInteger(NBT_FORM_TIME_ZHUXIAN), FORM_UNLOCK_TIME);
        int t2 = Math.min(tag.getInteger(NBT_FORM_TIME_LUXIAN), FORM_UNLOCK_TIME);
        int t3 = Math.min(tag.getInteger(NBT_FORM_TIME_XIANXIAN), FORM_UNLOCK_TIME);
        int t4 = Math.min(tag.getInteger(NBT_FORM_TIME_JUEXIAN), FORM_UNLOCK_TIME);
        return (t1 + t2 + t3 + t4) / (4.0f * FORM_UNLOCK_TIME) * 100f;
    }

    /**
     * 获取下一个可用形态（跳过未解锁的归墟）
     */
    private SwordForm getNextAvailableForm(ItemStack stack, SwordForm current) {
        int nextId = (current.id + 1) % SwordForm.values().length;
        SwordForm nextForm = SwordForm.fromId(nextId);

        // 如果下一个是归墟但未解锁，跳到诛仙
        if (nextForm == SwordForm.GUIXU && !isGuixuUnlocked(stack)) {
            return SwordForm.ZHUXIAN;
        }

        return nextForm;
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
     * 检查技能是否激活（支持物品掉落后的备份检测）
     *
     * 技能必须满足两个条件：
     * 1. 技能NBT开关为ON
     * 2. 当前形态与技能绑定的形态匹配
     *
     * 参考香巴拉的 isShambhala 实现
     */
    public static boolean isPlayerSkillActive(EntityPlayer player, String skillKey) {
        // 获取当前形态（优先从剑，其次从备份）
        SwordForm currentForm = null;
        ItemStack sword = getZhuxianSword(player);

        if (!sword.isEmpty()) {
            ZhuxianSword item = (ZhuxianSword) sword.getItem();
            currentForm = item.getForm(sword);

            // 同步到备份
            updateFormBackup(player, currentForm);
            registerHolder(player);

            // 检查技能开关和形态匹配
            boolean skillOn = item.isSkillActive(sword, skillKey);
            if (skillOn) {
                updateSkillBackup(player, skillKey, true);
            }

            // 技能必须开启 且 形态匹配
            SwordForm requiredForm = getSkillBoundForm(skillKey);
            return skillOn && (requiredForm == null || requiredForm == currentForm);
        }

        // 物品掉落时使用备份
        currentForm = getFormFromBackup(player);
        if (currentForm == null) {
            return false;
        }

        // 检查备份的技能状态和形态匹配
        boolean skillOn = getSkillFromBackup(player, skillKey);
        SwordForm requiredForm = getSkillBoundForm(skillKey);
        return skillOn && (requiredForm == null || requiredForm == currentForm);
    }

    /**
     * 检查玩家是否曾经是诛仙剑持有者（用于death hook）
     */
    public static boolean wasHolder(EntityPlayer player) {
        return zhuxianHolderBackup.contains(player.getUniqueID());
    }
}
