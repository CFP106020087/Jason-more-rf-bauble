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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
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

    // 活跃的宁静光环（持续性效果）
    private static final Map<UUID, AuraData> activeAuras = new HashMap<>();

    /**
     * 光环数据类 - 追踪持续性宁静光环
     */
    public static class AuraData {
        public final int dimension;
        public final Vec3d position;
        public final double range;
        public final long expiryTick;

        public AuraData(int dimension, Vec3d position, double range, long expiryTick) {
            this.dimension = dimension;
            this.position = position;
            this.range = range;
            this.expiryTick = expiryTick;
        }

        public boolean isExpired(long currentTick) {
            return currentTick >= expiryTick;
        }

        public boolean isInRange(EntityLivingBase entity) {
            if (entity.world.provider.getDimension() != dimension) return false;
            double distSq = entity.getDistanceSq(position.x, position.y, position.z);
            return distSq <= range * range;
        }
    }

    public ItemShambhalaVeil() {
        setRegistryName("shambhala_veil");
        setTranslationKey("shambhala_veil");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;  // 修复：从 CHARM 改为 HEAD（槽位 4）
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
     * 创建持续性的宁静光环，范围内的生物会持续被清除仇恨
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

        // 创建持续性光环
        double range = ShambhalaConfig.veilSkillRange;
        long duration = ShambhalaConfig.veilAuraDuration;
        Vec3d auraCenter = new Vec3d(player.posX, player.posY, player.posZ);

        AuraData aura = new AuraData(
                player.world.provider.getDimension(),
                auraCenter,
                range,
                now + duration
        );
        activeAuras.put(playerId, aura);

        // 立即对范围内生物执行一次仇恨清除
        int affected = clearAggroInAura(player.world, aura);

        // 特效 - 光环边界粒子
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            // 光环边界圆圈
            for (int i = 0; i < 72; i++) {
                double angle = (i / 72.0) * Math.PI * 2;
                double x = player.posX + Math.cos(angle) * range;
                double z = player.posZ + Math.sin(angle) * range;
                ws.spawnParticle(EnumParticleTypes.END_ROD, x, player.posY + 0.5, z, 2, 0, 0.5, 0, 0.01);
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
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.0f, 1.5f);

        // 消息
        int durationSeconds = (int) (duration / 20);
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "☀ 宁静光环激活 " + TextFormatting.GRAY +
                "- 持续 " + durationSeconds + " 秒 | 影响 " + affected + " 个生物"
        ), true);

        return true;
    }

    /**
     * 清除光环范围内生物的仇恨
     */
    public static int clearAggroInAura(World world, AuraData aura) {
        if (world.isRemote) return 0;
        if (world.provider.getDimension() != aura.dimension) return 0;

        AxisAlignedBB aabb = new AxisAlignedBB(
                aura.position.x - aura.range, aura.position.y - aura.range, aura.position.z - aura.range,
                aura.position.x + aura.range, aura.position.y + aura.range, aura.position.z + aura.range
        );

        List<EntityLiving> mobs = world.getEntitiesWithinAABB(EntityLiving.class, aabb);
        int affected = 0;

        // 创建假目标用于迷惑怪物
        DummyTarget dummyTarget = new DummyTarget(world);

        for (EntityLiving mob : mobs) {
            // 检查是否在圆形范围内
            double distSq = mob.getDistanceSq(aura.position.x, aura.position.y, aura.position.z);
            if (distSq > aura.range * aura.range) continue;

            // 清除所有仇恨相关
            if (mob.getAttackTarget() != null) {
                mob.setAttackTarget(null);
                affected++;
            }

            // 使用假目标替换复仇目标
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

        return affected;
    }

    /**
     * 每tick处理所有活跃的光环
     * 由 ShambhalaEventHandler 调用
     */
    public static void tickAuras(World world) {
        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        int dimension = world.provider.getDimension();

        // 遍历并处理所有活跃光环
        Iterator<Map.Entry<UUID, AuraData>> iter = activeAuras.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, AuraData> entry = iter.next();
            AuraData aura = entry.getValue();

            // 检查过期
            if (aura.isExpired(now)) {
                iter.remove();
                continue;
            }

            // 只处理同维度的光环
            if (aura.dimension != dimension) continue;

            // 每10tick清除一次仇恨（避免性能问题）
            if (now % 10 == 0) {
                clearAggroInAura(world, aura);
            }

            // 每20tick播放持续粒子效果
            if (now % 20 == 0 && world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                // 光环边界粒子
                for (int i = 0; i < 36; i++) {
                    double angle = (i / 36.0) * Math.PI * 2;
                    double x = aura.position.x + Math.cos(angle) * aura.range;
                    double z = aura.position.z + Math.sin(angle) * aura.range;
                    ws.spawnParticle(EnumParticleTypes.END_ROD, x, aura.position.y + 0.5, z, 1, 0, 0.2, 0, 0.005);
                }
            }
        }
    }

    /**
     * 检查生物是否在任何活跃的宁静光环中
     */
    public static boolean isInPeaceAura(EntityLivingBase entity) {
        if (entity == null || entity.world.isRemote) return false;

        long now = entity.world.getTotalWorldTime();

        for (AuraData aura : activeAuras.values()) {
            if (aura.isExpired(now)) continue;
            if (aura.isInRange(entity)) return true;
        }

        return false;
    }

    /**
     * 获取活跃光环数量（用于调试）
     */
    public static int getActiveAuraCount() {
        return activeAuras.size();
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
     * 清理玩家冷却数据和光环
     */
    public static void cleanupPlayer(UUID playerId) {
        cooldowns.remove(playerId);
        activeAuras.remove(playerId);
    }

    /**
     * 清空所有状态
     */
    public static void clearAllState() {
        cooldowns.clear();
        activeAuras.clear();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_宁静");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Veil - Peace Aura");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 宁静光环 [主动技能]");
        tooltip.add(TextFormatting.GRAY + "  在脚下创建 " + (int) ShambhalaConfig.veilSkillRange + " 格宁静圣域");
        tooltip.add(TextFormatting.WHITE + "  进入圣域的生物会持续被清除仇恨");
        tooltip.add(TextFormatting.YELLOW + "  持续时间: " + (ShambhalaConfig.veilAuraDuration / 20) + " 秒");
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
