package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import com.moremod.util.ThirstHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 香巴拉_圣域 (Shambhala Sanctuary) - 终极防线
 *
 * 能力：
 * - 创建保护光环
 * - 范围内友军获得伤害减免
 * - 持续消耗能量
 */
public class ItemShambhalaSanctuary extends ItemShambhalaBaubleBase {

    public ItemShambhalaSanctuary() {
        setRegistryName("shambhala_sanctuary");
        setTranslationKey("shambhala_sanctuary");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.world.isRemote) return;
        if (!ShambhalaHandler.isShambhala(player)) return;

        // 消耗能量维持圣域
        if (!ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.sanctuaryEnergyPerTick)) {
            return; // 能量不足，圣域停止
        }

        // 圣域粒子效果（每20tick）
        if (entity.ticksExisted % 20 == 0) {
            spawnSanctuaryParticles(player);
        }

        // 恢复圣域范围内的自己和友军的饥饿/口渴
        if (entity.ticksExisted % ShambhalaConfig.sanctuaryRestorationInterval == 0) {
            restoreAlliesInSanctuary(player);
        }
    }

    /**
     * 恢复圣域范围内所有友军（包括自己）的饥饿和口渴
     */
    private void restoreAlliesInSanctuary(EntityPlayer shambhalaPlayer) {
        double range = ShambhalaConfig.sanctuaryAuraRange;
        AxisAlignedBB aabb = new AxisAlignedBB(
                shambhalaPlayer.posX - range, shambhalaPlayer.posY - range, shambhalaPlayer.posZ - range,
                shambhalaPlayer.posX + range, shambhalaPlayer.posY + range, shambhalaPlayer.posZ + range
        );

        // 恢复自己
        restoreHungerAndThirst(shambhalaPlayer);

        // 恢复范围内的友方玩家
        List<EntityPlayer> nearbyPlayers = shambhalaPlayer.world.getEntitiesWithinAABB(
                EntityPlayer.class, aabb,
                p -> p != shambhalaPlayer && isAlly(shambhalaPlayer, p)
        );

        for (EntityPlayer ally : nearbyPlayers) {
            restoreHungerAndThirst(ally);
        }
    }

    /**
     * 恢复单个玩家的饥饿和口渴
     */
    private void restoreHungerAndThirst(EntityPlayer player) {
        int hungerRestore = ShambhalaConfig.sanctuaryHungerRestoration;
        int thirstRestore = ShambhalaConfig.sanctuaryThirstRestoration;

        // 恢复饥饿值（原版系统）
        int currentFood = player.getFoodStats().getFoodLevel();
        if (currentFood < 20) {
            // addStats 的第一个参数是食物值，第二个是饱和度
            player.getFoodStats().addStats(hungerRestore, 0.5f);
        }

        // 恢复口渴值（SimpleDifficulty）
        if (ThirstHelper.isAvailable()) {
            int currentThirst = ThirstHelper.getThirstLevel(player);
            if (currentThirst < 20) {
                ThirstHelper.addThirstLevel(player, thirstRestore);
                ThirstHelper.addThirstSaturation(player, 0.5f);
            }
        }
    }

    private void spawnSanctuaryParticles(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        double range = ShambhalaConfig.sanctuaryAuraRange;

        // 画圆形边界
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2;
            double x = player.posX + Math.cos(angle) * range;
            double z = player.posZ + Math.sin(angle) * range;
            world.spawnParticle(EnumParticleTypes.END_ROD, x, player.posY + 0.1, z,
                    1, 0, 0.1, 0, 0.01);
        }
    }

    /**
     * 检查实体是否在圣域范围内
     */
    public static boolean isInSanctuary(EntityPlayer shambhalaPlayer, EntityLivingBase target) {
        if (!ShambhalaHandler.isShambhala(shambhalaPlayer)) return false;
        double range = ShambhalaConfig.sanctuaryAuraRange;
        return target.getDistance(shambhalaPlayer) <= range;
    }

    /**
     * 检查实体是否是友方
     */
    public static boolean isAlly(EntityPlayer player, EntityLivingBase entity) {
        if (entity == player) return false;
        if (entity instanceof EntityPlayer) {
            return ((EntityPlayer) entity).isOnSameTeam(player);
        }
        // 村民、动物、铁傀儡等视为友方
        return entity instanceof EntityVillager ||
               entity instanceof EntityAnimal ||
               entity instanceof net.minecraft.entity.monster.EntityIronGolem;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_圣域");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Sanctuary - Final Bastion");
        tooltip.add("");
        tooltip.add(TextFormatting.WHITE + "◆ 圣域护盾");
        tooltip.add(TextFormatting.GRAY + "  光环范围: " + (int) ShambhalaConfig.sanctuaryAuraRange + " 格");
        tooltip.add(TextFormatting.GREEN + "  范围内友军减伤: " + (int)(ShambhalaConfig.sanctuaryAllyProtection * 100) + "%");
        tooltip.add("");
        tooltip.add(TextFormatting.WHITE + "◆ 生命维持");
        tooltip.add(TextFormatting.GRAY + "  每 " + (ShambhalaConfig.sanctuaryRestorationInterval / 20.0f) + " 秒恢复范围内友军:");
        tooltip.add(TextFormatting.GOLD + "  饥饿值 +" + ShambhalaConfig.sanctuaryHungerRestoration);
        if (ThirstHelper.SIMPLE_DIFFICULTY_LOADED) {
            tooltip.add(TextFormatting.AQUA + "  口渴值 +" + ShambhalaConfig.sanctuaryThirstRestoration);
        }
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "  能量消耗: " + ShambhalaConfig.sanctuaryEnergyPerTick + " RF/tick");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"在此圣域之内\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"众生皆受庇护\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
