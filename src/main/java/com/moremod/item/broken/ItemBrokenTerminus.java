package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破碎_终结 (Broken Terminus)
 *
 * 终局饰品 - 终焉之力（顶点饰品）
 *
 * 能力1: 伤害倍增
 *   - 所有造成的伤害 ×2
 *
 * 能力2: 死亡收割
 *   - 击杀敌人回复 5 HP
 *   - 击杀敌人获得 10 吸收之心
 *
 * 无负面效果 - 顶点饰品纯增益
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenTerminus extends ItemBrokenBaubleBase {

    public ItemBrokenTerminus() {
        setRegistryName("broken_terminus");
        setTranslationKey("broken_terminus");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
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
        // 效果在事件处理器中处理
    }

    /**
     * 获取伤害倍率
     */
    public static float getDamageMultiplier() {
        return (float) BrokenGodConfig.terminusDamageMultiplier;
    }

    /**
     * 击杀回复HP
     */
    public static float getKillHeal() {
        return (float) BrokenGodConfig.terminusKillHeal;
    }

    /**
     * 击杀获得吸收之心
     */
    public static float getKillAbsorption() {
        return (float) BrokenGodConfig.terminusKillAbsorption;
    }

    /**
     * 应用击杀效果（由事件处理器调用）
     */
    public static void applyKillEffect(EntityPlayer player) {
        if (player.world.isRemote) return;

        // 回复HP
        float healAmount = getKillHeal();
        player.heal(healAmount);

        // 获得吸收之心
        float absorptionGain = getKillAbsorption();
        float currentAbsorption = player.getAbsorptionAmount();
        player.setAbsorptionAmount(currentAbsorption + absorptionGain);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "破碎_终结");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Terminus");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 伤害倍增");
        tooltip.add(TextFormatting.YELLOW + "  所有伤害 ×" + (int) BrokenGodConfig.terminusDamageMultiplier);
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 死亡收割");
        tooltip.add(TextFormatting.GRAY + "  击杀敌人回复 " + (int) BrokenGodConfig.terminusKillHeal + " HP");
        tooltip.add(TextFormatting.GRAY + "  击杀敌人获得 " + (int) BrokenGodConfig.terminusKillAbsorption + " 吸收");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "※ 顶点饰品 - 纯增益");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"终焉之力，毁灭与重生\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"每一次杀戮都让自己更强\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 破碎系列顶点饰品");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
