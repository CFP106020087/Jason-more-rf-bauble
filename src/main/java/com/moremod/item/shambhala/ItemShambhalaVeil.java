package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 香巴拉_宁静 (Shambhala Veil) - 宁静光环
 *
 * 能力：
 * - 主动技能：消耗大量能量，消除范围内所有生物的仇恨
 * - 符合"圣域"主题：正面对抗，让敌人放弃攻击
 */
public class ItemShambhalaVeil extends ItemShambhalaBaubleBase {

    // 冷却追踪
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public ItemShambhalaVeil() {
        setRegistryName("shambhala_veil");
        setTranslationKey("shambhala_veil");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 主动技能在 ShambhalaEventHandler 中通过按键触发
    }

    /**
     * 执行宁静光环技能
     * @return 是否成功执行
     */
    public static boolean activatePeaceAura(EntityPlayer player) {
        if (player.world.isRemote) return false;
        if (!ShambhalaHandler.isShambhala(player)) return false;

        // 检查冷却
        UUID playerId = player.getUniqueID();
        long now = player.world.getTotalWorldTime();
        Long lastUse = cooldowns.get(playerId);
        if (lastUse != null && now - lastUse < ShambhalaConfig.veilSkillCooldown) {
            int remaining = (int) ((ShambhalaConfig.veilSkillCooldown - (now - lastUse)) / 20);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "宁静光环冷却中: " + remaining + "秒"
            ), true);
            return false;
        }

        // 检查并消耗能量
        if (!ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.veilSkillEnergyCost)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "能量不足！需要 " + ShambhalaConfig.veilSkillEnergyCost + " RF"
            ), true);
            return false;
        }

        // 记录冷却
        cooldowns.put(playerId, now);

        // 执行仇恨消除
        double range = ShambhalaConfig.veilSkillRange;
        AxisAlignedBB aabb = new AxisAlignedBB(
                player.posX - range, player.posY - range, player.posZ - range,
                player.posX + range, player.posY + range, player.posZ + range
        );

        List<EntityLiving> mobs = player.world.getEntitiesWithinAABB(EntityLiving.class, aabb);
        int affected = 0;

        // 创建假目标用于迷惑怪物（参考 ProtectionFieldHandler 和 ImmortalAmulet）
        DummyTarget dummyTarget = new DummyTarget(player.world);

        for (EntityLiving mob : mobs) {
            // 清除所有仇恨相关
            if (mob.getAttackTarget() != null) {
                mob.setAttackTarget(null);
                affected++;
            }

            // 使用假目标替换复仇目标，防止生物继续追踪
            mob.setRevengeTarget(dummyTarget);
            mob.setLastAttackedEntity(null);

            // 清除导航路径
            if (mob.getNavigator() != null) {
                mob.getNavigator().clearPath();
            }

            // 让生物看向随机方向
            mob.getLookHelper().setLookPosition(
                    mob.posX + mob.world.rand.nextGaussian() * 10,
                    mob.posY,
                    mob.posZ + mob.world.rand.nextGaussian() * 10,
                    10.0F, 10.0F
            );
        }

        // 特效
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            // 光环扩散粒子
            for (int i = 0; i < 100; i++) {
                double angle = (i / 100.0) * Math.PI * 2;
                double r = range * (i % 10) / 10.0;
                double x = player.posX + Math.cos(angle) * r;
                double z = player.posZ + Math.sin(angle) * r;
                ws.spawnParticle(EnumParticleTypes.END_ROD, x, player.posY + 1, z, 1, 0, 0.1, 0, 0.02);
            }
            // 中心光柱
            for (int y = 0; y < 20; y++) {
                ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        player.posX, player.posY + y * 0.5, player.posZ,
                        3, 0.2, 0, 0.2, 0.01);
            }
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);

        // 消息
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "☀ 宁静光环 " + TextFormatting.GRAY +
                "- 影响了 " + affected + " 个生物 (-" + ShambhalaConfig.veilSkillEnergyCost + " RF)"
        ), true);

        return true;
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    public static int getRemainingCooldown(EntityPlayer player) {
        Long lastUse = cooldowns.get(player.getUniqueID());
        if (lastUse == null) return 0;
        long elapsed = player.world.getTotalWorldTime() - lastUse;
        if (elapsed >= ShambhalaConfig.veilSkillCooldown) return 0;
        return (int) ((ShambhalaConfig.veilSkillCooldown - elapsed) / 20);
    }

    /**
     * 清理玩家冷却数据
     */
    public static void cleanupPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * 清空所有状态
     */
    public static void clearAllState() {
        cooldowns.clear();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_宁静");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Veil - Peace Aura");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 宁静光环 [主动技能]");
        tooltip.add(TextFormatting.GRAY + "  按下技能键消除 " + (int) ShambhalaConfig.veilSkillRange + " 格内所有仇恨");
        tooltip.add(TextFormatting.RED + "  能量消耗: " + ShambhalaConfig.veilSkillEnergyCost + " RF");
        tooltip.add(TextFormatting.YELLOW + "  冷却时间: " + (ShambhalaConfig.veilSkillCooldown / 20) + " 秒");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"放下你的仇恨\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"这里是永恒的宁静之地\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    /**
     * 假目标实体类 - 用于迷惑敌对生物
     * 参考 ImmortalAmulet 和 ProtectionFieldHandler 的实现
     */
    public static class DummyTarget extends EntityLivingBase {
        public DummyTarget(World world) {
            super(world);
            this.setInvisible(true);
            this.setSize(0.0F, 0.0F);
            this.setEntityInvulnerable(true);
            this.isDead = true; // 标记为死亡，防止被持续追踪
        }

        @Override
        public void onUpdate() {
            // 立即标记为死亡
            this.isDead = true;
        }

        @Override
        public boolean isEntityAlive() {
            return false;
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
            // 不做任何事
        }

        @Override
        public Iterable<ItemStack> getArmorInventoryList() {
            return java.util.Collections.emptyList();
        }

        @Override
        public EnumHandSide getPrimaryHand() {
            return EnumHandSide.RIGHT;
        }
    }
}
