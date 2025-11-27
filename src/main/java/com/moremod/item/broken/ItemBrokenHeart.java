package com.moremod.item.broken;

import baubles.api.BaubleType;
<<<<<<< HEAD
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
=======
import baubles.api.IBauble;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
>>>>>>> origin/newest
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
<<<<<<< HEAD
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
=======

/**
 * 破碎之心
 * Broken Heart
 *
 * 破碎之神的核心组件：
 * - 永久免疫恐惧
 * - 免疫 Wither、Poison、Bleeding
 * - 免疫 FirstAid 部位伤害
 * - 生命值不会低于最小值（除停机模式）
 *
 * 不可卸下
 */
public class ItemBrokenHeart extends Item implements IBauble {
>>>>>>> origin/newest

    public ItemBrokenHeart() {
        setRegistryName("broken_heart");
        setTranslationKey("broken_heart");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
<<<<<<< HEAD
=======
        setMaxStackSize(1);
>>>>>>> origin/newest
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
<<<<<<< HEAD
        if (player instanceof EntityPlayer) {
            applyHPCompression((EntityPlayer) player);
=======
        // 装备时移除负面效果
        if (player instanceof EntityPlayer) {
>>>>>>> origin/newest
            clearNegativeEffects(player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
<<<<<<< HEAD
        if (player instanceof EntityPlayer) {
            removeHPCompression((EntityPlayer) player);
        }
=======
        // 卸下时无特殊操作
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        // 破碎之神不能卸下
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
>>>>>>> origin/newest
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

<<<<<<< HEAD
        // 每100tick确保HP压缩生效
        if (entity.ticksExisted % 100 == 0) {
            applyHPCompression(player);
        }

        // 清除负面效果
        if (entity.ticksExisted % 20 == 0) {
            clearNegativeEffects(entity);
        }

        // 确保HP不会低于1（不朽效果）
        if (!player.world.isRemote && player.getHealth() < 1.0f && player.isEntityAlive()) {
            player.setHealth(1.0f);
=======
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 每 tick 检查并移除负面效果
        clearNegativeEffects(entity);

        // 确保最小血量（除非在停机模式）
        if (!BrokenGodHandler.isInShutdown(player)) {
            float minHealth = (float) BrokenGodConfig.minimumHealth;
            if (entity.getHealth() < minHealth && entity.getHealth() > 0) {
                entity.setHealth(minHealth);
            }
>>>>>>> origin/newest
        }
    }

    /**
<<<<<<< HEAD
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
=======
     * 清除特定负面效果
     */
    private void clearNegativeEffects(EntityLivingBase entity) {
        // Wither
        entity.removePotionEffect(net.minecraft.init.MobEffects.WITHER);

        // Poison
        entity.removePotionEffect(net.minecraft.init.MobEffects.POISON);

        // Blindness (由破碎之眼处理，这里也加一层保护)
        // entity.removePotionEffect(net.minecraft.init.MobEffects.BLINDNESS);

        // Nausea (恐惧/混乱)
        entity.removePotionEffect(net.minecraft.init.MobEffects.NAUSEA);

        // Hunger (模拟"无痛")
        // entity.removePotionEffect(net.minecraft.init.MobEffects.HUNGER);

        // 尝试清除模组添加的出血效果
>>>>>>> origin/newest
        entity.getActivePotionEffects().removeIf(effect -> {
            String effectName = effect.getPotion().getRegistryName().toString().toLowerCase();
            return effectName.contains("bleed") || effectName.contains("bleeding");
        });
    }

<<<<<<< HEAD
    /**
     * 计算狂战士伤害倍率（由事件处理器调用）
     * 血量越低伤害越高，满血×1，1HP时×5
     *
     * 公式: multiplier = maxMultiplier - (maxMultiplier - 1) * (currentHP / maxHP)
     */
    public static float getBerserkerMultiplier(EntityPlayer player) {
        float currentHP = player.getHealth();
        float maxHP = player.getMaxHealth();
        float maxMultiplier = (float) BrokenRelicConfig.heartcoreBerserkerMaxMultiplier;

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
        tooltip.add(TextFormatting.GREEN + "◆ 完全生命汲取");
        tooltip.add(TextFormatting.GRAY + "  伤害的 " + (int)(BrokenRelicConfig.heartcoreLifestealRatio * 100) + "% 转化为治疗");
        tooltip.add(TextFormatting.GRAY + "  溢出转吸收之心 (上限 " + (int) BrokenRelicConfig.heartcoreMaxAbsorption + " HP)");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 狂战士");
        tooltip.add(TextFormatting.GRAY + "  血量越低伤害越高");
        tooltip.add(TextFormatting.YELLOW + "  最高 ×" + (int) BrokenRelicConfig.heartcoreBerserkerMaxMultiplier + " 倍伤害");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 不朽");
        tooltip.add(TextFormatting.GRAY + "  HP不会低于1");
        tooltip.add(TextFormatting.GRAY + "  免疫: 凋零/中毒/出血");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"不朽引擎，永不停息\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"越接近死亡，越是强大\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
=======
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "破碎之心");
        tooltip.add(TextFormatting.GRAY + "Broken Heart");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 永久免疫恐惧");
        tooltip.add(TextFormatting.AQUA + "◆ 无痛模式");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫凋零效果");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫中毒效果");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫出血效果");
        tooltip.add(TextFormatting.YELLOW + "◆ 生命值稳定器");
        tooltip.add(TextFormatting.GRAY + "  (最低保持 " + BrokenGodConfig.minimumHealth + " HP)");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"心脏不再跳动\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"但齿轮永不停止\"");
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
>>>>>>> origin/newest
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
<<<<<<< HEAD
        return true;
=======
        return true; // 发光效果
>>>>>>> origin/newest
    }
}
