package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.core.CurseDeathHook;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 怨念结晶 - 七咒之戒联动饰品
 * Crystallized Resentment
 *
 * 效果：
 * - 正面：真伤光环 - 每秒对周围敌人造成其最大生命值1%的真实伤害
 * - 负面：无法获得再生效果
 *
 * 机制：
 * - 需要佩戴七咒之戒才能装备
 * - 真伤光环由 onWornTick 实现
 * - 再生禁止由 onWornTick 实现
 */
public class ItemCrystallizedResentment extends Item implements IBauble {

    // 真伤光环半径
    public static final double AURA_RADIUS = 4.0;
    // 真伤光环伤害百分比 (1% = 0.01)
    public static final float AURA_DAMAGE_PERCENT = 0.01f;
    // 最小伤害（防止对低血量怪无效）
    public static final float MIN_DAMAGE = 1.0f;
    // 伤害间隔 (tick) - 每秒一次
    public static final int DAMAGE_INTERVAL = 20;

    // 真实伤害源 - 无视护甲
    private static final DamageSource TRUE_DAMAGE = new DamageSource("resentment")
            .setDamageBypassesArmor()
            .setMagicDamage();

    public ItemCrystallizedResentment() {
        this.setMaxStackSize(1);
        this.setTranslationKey("crystallized_resentment");
        this.setRegistryName("crystallized_resentment");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.HEAD; // 头部槽位，避免与其他七咒饰品冲突
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return false;
        return CurseDeathHook.hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        p.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.DARK_PURPLE + "怨念结晶低语：" +
                TextFormatting.GRAY + "让仇恨成为你的武器..."
        ));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊处理
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (entity.world.isRemote) return;
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;

        // 移除再生效果
        if (player.isPotionActive(MobEffects.REGENERATION)) {
            player.removePotionEffect(MobEffects.REGENERATION);
        }

        // 真伤光环 - 每秒对周围敌对生物造成真实伤害
        if (player.ticksExisted % DAMAGE_INTERVAL == 0) {
            applyTrueDamageAura(player);
        }
    }

    /**
     * 应用真伤光环 - 对敌人造成其最大生命值1%的真实伤害
     */
    private void applyTrueDamageAura(EntityPlayer player) {
        AxisAlignedBB auraBox = player.getEntityBoundingBox().grow(AURA_RADIUS);
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                auraBox,
                e -> e != player && e instanceof IMob && e.isEntityAlive()
        );

        for (EntityLivingBase target : entities) {
            // 计算百分比伤害
            float maxHealth = (float) target.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue();
            float damage = Math.max(MIN_DAMAGE, maxHealth * AURA_DAMAGE_PERCENT);
            target.attackEntityFrom(TRUE_DAMAGE, damage);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴怨念结晶
     */
    public static boolean isWearing(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() && bauble.getItem() instanceof ItemCrystallizedResentment) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ========== 物品属性 ==========

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add(TextFormatting.DARK_PURPLE + "===========================");
        list.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.BOLD + "怨念结晶");
        list.add(TextFormatting.DARK_GRAY + "Crystallized Resentment");
        list.add("");
        list.add(TextFormatting.GRAY + "凝聚了无尽怨恨的黑色结晶");

        if (player == null || !CurseDeathHook.hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "! 需要佩戴七咒之戒才能装备");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "===========================");
            return;
        }

        list.add("");
        list.add(TextFormatting.GREEN + ". 正面效果");
        list.add(TextFormatting.GRAY + "  真伤光环: 每秒对 " + TextFormatting.YELLOW + (int)AURA_RADIUS + "格" +
                TextFormatting.GRAY + " 内敌人造成");
        list.add(TextFormatting.GRAY + "  其最大生命值 " + TextFormatting.RED + (int)(AURA_DAMAGE_PERCENT * 100) + "%" +
                TextFormatting.GRAY + " 的真实伤害");
        list.add(TextFormatting.DARK_GRAY + "  (最低 " + (int)MIN_DAMAGE + " 点)");

        list.add("");
        list.add(TextFormatting.RED + ". 负面效果");
        list.add(TextFormatting.DARK_RED + "  无法获得再生效果");

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"怨念无形，却能伤人\"");
        list.add(TextFormatting.DARK_PURPLE + "===========================");
    }
}
