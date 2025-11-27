package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 破碎_臂 (Broken Arm)
 *
 * 终局饰品 - 精准与毁灭的极致
 *
 * 能力1: 绝对暴击
 *   - 所有攻击必定暴击
 *   - 暴击伤害 ×3
 *
 * 能力2: 极限延伸
 *   - 攻击距离 +3 格
 *
 * 能力3: 护甲粉碎领域
 *   - 周围敌人护甲归零（属性修改光环）
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenArm extends ItemBrokenBaubleBase {

    private static final UUID ARMOR_SHRED_UUID = UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef");

    // 追踪已经被护甲粉碎的实体
    private static final Map<Integer, Long> shreddedEntities = new HashMap<>();

    public ItemBrokenArm() {
        setRegistryName("broken_arm");
        setTranslationKey("broken_arm");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (player.world.isRemote) return;
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 每5tick应用护甲粉碎光环
        if (entity.ticksExisted % 5 == 0) {
            applyArmorShredAura(player);
        }
    }

    /**
     * 应用护甲粉碎光环 - 使用属性修改将护甲归零
     */
    private void applyArmorShredAura(EntityPlayer player) {
        double range = BrokenRelicConfig.armArmorShredRange;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range);

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e instanceof IMob && e.isEntityAlive()
        );

        long currentTime = player.world.getTotalWorldTime();

        for (EntityLivingBase target : entities) {
            IAttributeInstance armorAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
            if (armorAttr == null) continue;

            // 应用护甲粉碎修改器（将护甲设为0）
            AttributeModifier shredMod = armorAttr.getModifier(ARMOR_SHRED_UUID);
            if (shredMod == null) {
                // 使用 -1.0 和 operation 2（乘法）将护甲归零
                armorAttr.applyModifier(new AttributeModifier(
                        ARMOR_SHRED_UUID,
                        "Broken Arm Armor Shred",
                        -1.0,
                        2 // 乘法 = 护甲 × 0
                ));
            }

            // 记录粉碎时间
            shreddedEntities.put(target.getEntityId(), currentTime);
        }

        // 清理离开范围的实体的护甲粉碎效果
        shreddedEntities.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > 10) {
                // 超过10tick没更新，说明已离开范围
                net.minecraft.entity.Entity e = player.world.getEntityByID(entry.getKey());
                if (e instanceof EntityLivingBase) {
                    IAttributeInstance armorAttr = ((EntityLivingBase) e).getEntityAttribute(
                            SharedMonsterAttributes.ARMOR);
                    if (armorAttr != null) {
                        armorAttr.removeModifier(ARMOR_SHRED_UUID);
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 获取暴击伤害倍率
     */
    public static float getCritMultiplier() {
        return (float) BrokenRelicConfig.armCritMultiplier;
    }

    /**
     * 获取护甲粉碎光环范围
     */
    public static float getArmorShredRange() {
        return (float) BrokenRelicConfig.armArmorShredRange;
    }

    /**
     * 获取攻击距离延长
     */
    public static float getRangeExtension() {
        return (float) BrokenRelicConfig.armRangeExtension;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_臂");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Arm");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 绝对暴击");
        tooltip.add(TextFormatting.GRAY + "  所有攻击必定暴击");
        tooltip.add(TextFormatting.YELLOW + "  暴击伤害 ×" + BrokenRelicConfig.armCritMultiplier);
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 极限延伸");
        tooltip.add(TextFormatting.GRAY + "  攻击距离 +" + (int) BrokenRelicConfig.armRangeExtension + " 格");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 护甲粉碎领域");
        tooltip.add(TextFormatting.GRAY + "  " + (int) BrokenRelicConfig.armArmorShredRange + " 格内敌人");
        tooltip.add(TextFormatting.AQUA + "  护甲归零");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"机械之臂，粉碎一切防御\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"在此领域内，护甲毫无意义\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
