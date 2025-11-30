package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
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
 * 终局饰品 - 不朽引擎
 *
 * 能力1: 极限生命压缩
 *   - 最大生命值固定为10HP
 *
 * 能力2: 完全生命汲取
 *   - 100%伤害转化为治疗
 *   - 溢出治疗转为吸收之心（上限20HP）
 *
 * 能力3: 狂战士
 *   - 血量越低伤害越高
 *   - 最高×5倍伤害（1HP时）
 *
 * 能力4: 不朽
 *   - HP不会低于1
 *   - 免疫凋零/中毒/出血
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenHeart extends ItemBrokenBaubleBase {

    private static final UUID HP_COMPRESS_UUID = UUID.fromString("c1234567-89ab-cdef-0123-456789abcdef");

    public ItemBrokenHeart() {
        setRegistryName("broken_heart");
        setTranslationKey("broken_heart");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
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

    /** 固定的目标最大生命值 */
    private static final double TARGET_MAX_HP = 10.0;

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 每tick检查并强制HP压缩（防止其他mod覆盖）
        if (!player.world.isRemote) {
            enforceHPCompression(player);
        }

        // 清除负面效果
        if (entity.ticksExisted % 20 == 0) {
            clearNegativeEffects(entity);
        }

        // 确保HP不会低于1（不朽效果）
        if (!player.world.isRemote && player.getHealth() < 1.0f && player.isEntityAlive()) {
            player.setHealth(1.0f);
        }
    }

    /**
     * 强制应用生命值压缩（每tick检查）
     * 使用更激进的方式确保max HP始终为目标值
     */
    private void enforceHPCompression(EntityPlayer player) {
        IAttributeInstance maxHealthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // 获取当前实际最大生命值（包含所有修改器）
        double currentMaxHP = maxHealthAttr.getAttributeValue();

        // 如果当前最大HP不等于目标值，重新计算修改器
        if (Math.abs(currentMaxHP - TARGET_MAX_HP) > 0.01) {
            // 移除旧的修改器
            AttributeModifier existing = maxHealthAttr.getModifier(HP_COMPRESS_UUID);
            if (existing != null) {
                maxHealthAttr.removeModifier(existing);
            }

            // 重新计算：需要从当前值（移除我们的修改器后）到达目标值
            double valueWithoutOurMod = maxHealthAttr.getAttributeValue();
            double reduction = TARGET_MAX_HP - valueWithoutOurMod;

            AttributeModifier mod = new AttributeModifier(
                    HP_COMPRESS_UUID,
                    "Broken Heartcore HP Compression",
                    reduction,
                    0 // Operation 0 = 加法
            );
            maxHealthAttr.applyModifier(mod);
        }

        // 如果当前血量超过最大值，调整
        if (player.getHealth() > TARGET_MAX_HP) {
            player.setHealth((float) TARGET_MAX_HP);
        }
    }

    /**
     * 应用生命值压缩（装备时调用）
     */
    private void applyHPCompression(EntityPlayer player) {
        enforceHPCompression(player);
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
     * 计算狂战士伤害倍率（由事件处理器调用）
     * 血量越低伤害越高，满血×1，1HP时×5
     *
     * 公式: multiplier = maxMultiplier - (maxMultiplier - 1) * (currentHP / maxHP)
     */
    public static float getBerserkerMultiplier(EntityPlayer player) {
        float currentHP = player.getHealth();
        float maxHP = player.getMaxHealth();
        float maxMultiplier = (float) BrokenGodConfig.heartcoreBerserkerMaxMultiplier;

        if (maxHP <= 0) return 1.0f;

        float hpRatio = Math.max(0, Math.min(1, currentHP / maxHP));
        // 满血时 = 1，空血时 = maxMultiplier
        return maxMultiplier - (maxMultiplier - 1.0f) * hpRatio;
    }

    /**
     * 应用生命汲取（由事件处理器调用）
     */
    public static void applyLifesteal(EntityPlayer player, float damageDealt) {
        if (player.world.isRemote) return;

        float healAmount = damageDealt * (float) BrokenGodConfig.heartcoreLifestealRatio;

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
            float maxAbsorption = (float) BrokenGodConfig.heartcoreMaxAbsorption;

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
        tooltip.add(TextFormatting.GRAY + "  最大生命值固定为 " + (int) BrokenGodConfig.heartcoreCompressedHP + " HP");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 完全生命汲取");
        tooltip.add(TextFormatting.GRAY + "  伤害的 " + (int)(BrokenGodConfig.heartcoreLifestealRatio * 100) + "% 转化为治疗");
        tooltip.add(TextFormatting.GRAY + "  溢出转吸收之心 (上限 " + (int) BrokenGodConfig.heartcoreMaxAbsorption + " HP)");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 狂战士");
        tooltip.add(TextFormatting.GRAY + "  血量越低伤害越高");
        tooltip.add(TextFormatting.YELLOW + "  最高 ×" + (int) BrokenGodConfig.heartcoreBerserkerMaxMultiplier + " 倍伤害");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 不朽");
        tooltip.add(TextFormatting.GRAY + "  HP不会低于1");
        tooltip.add(TextFormatting.GRAY + "  免疫: 凋零/中毒/出血");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"不朽引擎，永不停息\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"越接近死亡，越是强大\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
