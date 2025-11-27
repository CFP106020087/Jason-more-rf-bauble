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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 破碎_心核 (Broken Heartcore)
 *
 * 终局饰品 - 疯狂生存 + 自残风格
 *
 * 能力1: 极限生命上限压缩
 *   - 最大生命值强制压到固定值（默认10HP/5心）
 *
 * 能力2: 极高生命汲取
 *   - 造成伤害的80%立即治疗自己
 *   - 溢出的治疗转化为吸收之心（上限8HP）
 *
 * 不可卸下
 */
public class ItemBrokenHeart extends Item implements IBauble {

    private static final UUID HP_COMPRESS_UUID = UUID.fromString("c1234567-89ab-cdef-0123-456789abcdef");

    public ItemBrokenHeart() {
        setRegistryName("broken_heart");
        setTranslationKey("broken_heart");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            applyHPCompression((EntityPlayer) player);
            clearNegativeEffects(player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeHPCompression((EntityPlayer) player);
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

        // 每100tick确保HP压缩生效
        if (entity.ticksExisted % 100 == 0) {
            applyHPCompression(player);
        }

        // 清除负面效果
        if (entity.ticksExisted % 20 == 0) {
            clearNegativeEffects(entity);
        }
    }

    /**
     * 应用生命值压缩
     */
    private void applyHPCompression(EntityPlayer player) {
        IAttributeInstance maxHealthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // 移除旧的修改器
        AttributeModifier existing = maxHealthAttr.getModifier(HP_COMPRESS_UUID);
        if (existing != null) {
            maxHealthAttr.removeModifier(existing);
        }

        // 计算需要减少多少HP
        double currentMax = maxHealthAttr.getBaseValue();
        double targetMax = BrokenRelicConfig.heartcoreCompressedHP;
        double reduction = targetMax - currentMax; // 负数表示减少

        if (reduction < 0) {
            AttributeModifier mod = new AttributeModifier(
                    HP_COMPRESS_UUID,
                    "Broken Heartcore HP Compression",
                    reduction,
                    0 // Operation 0 = 加法
            );
            maxHealthAttr.applyModifier(mod);
        }

        // 如果当前血量超过最大值，调整
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 移除生命值压缩
     */
    private void removeHPCompression(EntityPlayer player) {
        IAttributeInstance maxHealthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        AttributeModifier existing = maxHealthAttr.getModifier(HP_COMPRESS_UUID);
        if (existing != null) {
            maxHealthAttr.removeModifier(existing);
        }
    }

    /**
     * 清除负面效果
     */
    private void clearNegativeEffects(EntityLivingBase entity) {
        entity.removePotionEffect(net.minecraft.init.MobEffects.WITHER);
        entity.removePotionEffect(net.minecraft.init.MobEffects.POISON);
        entity.removePotionEffect(net.minecraft.init.MobEffects.NAUSEA);

        // 清除模组出血效果
        entity.getActivePotionEffects().removeIf(effect -> {
            String effectName = effect.getPotion().getRegistryName().toString().toLowerCase();
            return effectName.contains("bleed") || effectName.contains("bleeding");
        });
    }

    /**
     * 应用生命汲取（由事件处理器调用）
     */
    public static void applyLifesteal(EntityPlayer player, float damageDealt) {
        if (player.world.isRemote) return;

        float healAmount = damageDealt * (float) BrokenRelicConfig.heartcoreLifestealRatio;

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float missingHealth = maxHealth - currentHealth;

        if (missingHealth > 0) {
            // 先治疗缺失的血量
            float actualHeal = Math.min(healAmount, missingHealth);
            player.heal(actualHeal);
            healAmount -= actualHeal;
        }

        // 溢出部分转为吸收之心
        if (healAmount > 0) {
            float currentAbsorption = player.getAbsorptionAmount();
            float maxAbsorption = (float) BrokenRelicConfig.heartcoreMaxAbsorption;

            if (currentAbsorption < maxAbsorption) {
                float newAbsorption = Math.min(currentAbsorption + healAmount, maxAbsorption);
                player.setAbsorptionAmount(newAbsorption);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_心核");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Heartcore");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 极限生命压缩");
        tooltip.add(TextFormatting.GRAY + "  最大生命值固定为 " + (int) BrokenRelicConfig.heartcoreCompressedHP + " HP");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 极高生命汲取");
        tooltip.add(TextFormatting.GRAY + "  伤害的 " + (int)(BrokenRelicConfig.heartcoreLifestealRatio * 100) + "% 转化为治疗");
        tooltip.add(TextFormatting.GRAY + "  溢出治疗转为吸收之心(上限 " + (int) BrokenRelicConfig.heartcoreMaxAbsorption + " HP)");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 免疫: 凋零/中毒/出血");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"心脏变为永恒机器\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"代价是再也无法成长\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
