package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
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
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 破碎_枷锁 (Broken Shackles)
 *
 * 终局饰品 - 控制 + 牺牲机动
 *
 * 能力1: 强制减速光环
 *   - 8格内的敌对生物被强制减速（Slowness III）
 *
 * 能力2: 自身减速换防御
 *   - 移动速度 -30%
 *   - 所受伤害 -30%
 *
 * 不可卸下
 */
public class ItemBrokenShackles extends Item implements IBauble {

    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d1234567-89ab-cdef-0123-456789abcdef");

    public ItemBrokenShackles() {
        setRegistryName("broken_shackles");
        setTranslationKey("broken_shackles");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BELT;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            applySpeedReduction((EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeSpeedReduction((EntityPlayer) player);
        }
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (player.world.isRemote) return;
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 确保速度减益存在
        if (entity.ticksExisted % 100 == 0) {
            applySpeedReduction(player);
        }

        // 每10tick应用减速光环
        if (entity.ticksExisted % 10 == 0) {
            applySlowAura(player);
        }
    }

    /**
     * 应用减速光环
     */
    private void applySlowAura(EntityPlayer player) {
        double range = BrokenRelicConfig.shacklesAuraRange;
        int slowLevel = BrokenRelicConfig.shacklesSlowLevel;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range);

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e instanceof IMob && e.isEntityAlive()
        );

        for (EntityLivingBase target : entities) {
            // 应用短时间减速（15tick），避免无限叠加
            target.addPotionEffect(new PotionEffect(
                    MobEffects.SLOWNESS, 15, slowLevel, true, false
            ));
        }
    }

    /**
     * 应用自身速度减少
     */
    private void applySpeedReduction(EntityPlayer player) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
        if (existing == null) {
            double reduction = -BrokenRelicConfig.shacklesSelfSlow;
            speedAttr.applyModifier(new AttributeModifier(
                    SPEED_MODIFIER_UUID,
                    "Broken Shackles Speed Reduction",
                    reduction,
                    2 // 乘法
            ));
        }
    }

    /**
     * 移除速度减少
     */
    private void removeSpeedReduction(EntityPlayer player) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
        if (existing != null) {
            speedAttr.removeModifier(existing);
        }
    }

    /**
     * 获取伤害减免比例（由事件处理器调用）
     */
    public static float getDamageReduction() {
        return (float) BrokenRelicConfig.shacklesDamageReduction;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_枷锁");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Shackles");
        tooltip.add("");
        tooltip.add(TextFormatting.BLUE + "◆ 强制减速光环");
        tooltip.add(TextFormatting.GRAY + "  " + (int) BrokenRelicConfig.shacklesAuraRange + " 格内敌人减速");
        tooltip.add(TextFormatting.GRAY + "  (Slowness " + toRoman(BrokenRelicConfig.shacklesSlowLevel + 1) + ")");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "◆ 坚守阵地");
        tooltip.add(TextFormatting.RED + "  移动速度 -" + (int)(BrokenRelicConfig.shacklesSelfSlow * 100) + "%");
        tooltip.add(TextFormatting.GREEN + "  所受伤害 -" + (int)(BrokenRelicConfig.shacklesDamageReduction * 100) + "%");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"枷锁束缚了敌人\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"也束缚了自己\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    private String toRoman(int num) {
        String[] romans = {"I", "II", "III", "IV", "V"};
        return num > 0 && num <= 5 ? romans[num - 1] : String.valueOf(num);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
