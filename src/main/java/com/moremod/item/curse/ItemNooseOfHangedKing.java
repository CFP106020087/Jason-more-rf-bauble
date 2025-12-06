package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缢王之索 (Noose of the Hanged King)
 * "绳索不仅仅是用来勒紧脖子的，它也是通往王座的升降机。"
 *
 * 饰品类型：项链 (Necklace)
 *
 * 外观：一根粗糙的、甚至能看到倒刺的麻绳圈
 *
 * 基础效果【窒息之握】：
 * - 攻击时 15% 概率触发"处刑"
 * - 目标被提至半空 3 秒，每秒受到 4% 最大生命值窒息伤害（无视护甲）
 * - 对 Boss 无效，但会造成短暂僵直/减速
 *
 * 代价【同感痛苦】：
 * - 每次触发处刑，玩家失去 1 格氧气
 * - 如果在水下或氧气耗尽，直接扣血
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemNooseOfHangedKing extends Item implements IBauble {

    // 处刑触发概率
    private static final float EXECUTION_CHANCE = 0.15f;
    // 处刑持续时间（tick）
    private static final int EXECUTION_DURATION_TICKS = 60; // 3秒
    // 每秒伤害（最大生命值百分比）
    private static final float DAMAGE_PERCENT_PER_SECOND = 0.04f;
    // Boss 减速时间（tick）
    private static final int BOSS_SLOW_DURATION = 40; // 2秒
    // 氧气消耗量
    private static final int AIR_COST = 30; // 约 1 格氧气
    // 氧气不足时的伤害
    private static final float SUFFOCATION_DAMAGE = 2.0f;

    // 被处刑的实体记录：实体ID -> 处刑结束时间
    private static final Map<Integer, ExecutionData> EXECUTED_ENTITIES = new ConcurrentHashMap<>();

    private static class ExecutionData {
        final long endTime;
        final double originalY;
        final float hangHeight;
        final EntityPlayer executioner;

        ExecutionData(EntityPlayer executioner, double originalY, float hangHeight) {
            this.endTime = System.currentTimeMillis() + (EXECUTION_DURATION_TICKS * 50L);
            this.originalY = originalY;
            this.hangHeight = hangHeight;
            this.executioner = executioner;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > endTime;
        }
    }

    public ItemNooseOfHangedKing() {
        this.setMaxStackSize(1);
        this.setTranslationKey("noose_of_hanged_king");
        this.setRegistryName("noose_of_hanged_king");
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.AMULET; // 项链
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return false;
        return hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {
        // 清理过期的处刑记录
        EXECUTED_ENTITIES.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 卸下时释放所有该玩家造成的处刑
        if (player instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) player;
            EXECUTED_ENTITIES.entrySet().removeIf(entry ->
                entry.getValue().executioner.equals(p));
        }
    }

    // ========== 事件处理 ==========

    /**
     * 攻击时触发处刑
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        // 检查是否佩戴缢王之索
        if (!hasNoose(player)) return;

        // 检查是否有七咒联动
        if (!hasCursedRing(player)) return;

        // 检查目标是否已经在被处刑
        if (EXECUTED_ENTITIES.containsKey(target.getEntityId())) return;

        // 15% 概率触发
        if (player.world.rand.nextFloat() > EXECUTION_CHANCE) return;

        // 判断是否是 Boss
        boolean isBoss = isBossEntity(target);

        if (isBoss) {
            // Boss: 施加减速效果
            applyBossEffect(player, target);
        } else {
            // 普通怪物: 执行处刑
            executeTarget(player, target);
        }

        // 代价：玩家失去氧气
        applySuffocationCost(player);
    }

    /**
     * 每 tick 更新被处刑的实体
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        World world = event.world;
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, ExecutionData>> it = EXECUTED_ENTITIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ExecutionData> entry = it.next();
            int entityId = entry.getKey();
            ExecutionData data = entry.getValue();

            net.minecraft.entity.Entity entity = world.getEntityByID(entityId);
            if (entity == null || !(entity instanceof EntityLivingBase)) {
                it.remove();
                continue;
            }

            EntityLivingBase target = (EntityLivingBase) entity;

            // 检查是否过期
            if (data.isExpired()) {
                // 处刑结束，让实体落下
                target.motionY = -0.5;
                target.fallDistance = 0;
                it.remove();

                // 效果提示
                if (data.executioner != null) {
                    data.executioner.sendStatusMessage(new TextComponentString(
                            TextFormatting.GRAY + "处刑结束..."
                    ), true);
                }
                continue;
            }

            // 保持悬空
            double targetY = data.originalY + data.hangHeight;
            if (target.posY < targetY) {
                target.motionY = 0.15;
            } else {
                target.motionY = 0;
                target.setPosition(target.posX, targetY, target.posZ);
            }
            target.fallDistance = 0;
            target.onGround = false;

            // 每秒造成窒息伤害（每 20 tick）
            if (world.getTotalWorldTime() % 20 == 0) {
                float maxHealth = target.getMaxHealth();
                float damage = maxHealth * DAMAGE_PERCENT_PER_SECOND;
                damage = Math.max(1.0f, damage);

                // 使用真伤
                TrueDamageHelper.applyTrueDamage(target, data.executioner, damage);

                // 粒子效果
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                            target.posX, target.posY + target.height, target.posZ,
                            10, 0.2, 0.2, 0.2, 0.02);
                    ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                            target.posX, target.posY + target.height + 0.5, target.posZ,
                            5, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 执行处刑（普通怪物）
     */
    private static void executeTarget(EntityPlayer player, EntityLivingBase target) {
        // 记录处刑数据
        float hangHeight = 2.0f + player.world.rand.nextFloat() * 1.5f;
        EXECUTED_ENTITIES.put(target.getEntityId(),
            new ExecutionData(player, target.posY, hangHeight));

        // 初始向上推力
        target.motionY = 0.8;
        target.velocityChanged = true;

        // 效果提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠ 处刑！" +
                TextFormatting.GRAY + " 目标被吊起 " +
                TextFormatting.GOLD + "3" + TextFormatting.GRAY + " 秒"
        ));

        // 粒子和音效
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    target.posX, target.posY + 1, target.posZ,
                    20, 0.3, 0.5, 0.3, 0.05);
            ws.playSound(null, target.getPosition(),
                    SoundEvents.ENTITY_LEASHKNOT_PLACE,
                    SoundCategory.HOSTILE, 1.0F, 0.5F);
        }
    }

    /**
     * 对 Boss 施加效果（无法吊起，改为减速）
     */
    private static void applyBossEffect(EntityPlayer player, EntityLivingBase target) {
        // 施加缓慢 III
        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, BOSS_SLOW_DURATION, 2));
        // 施加虚弱 II
        target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, BOSS_SLOW_DURATION, 1));

        // 短暂僵直（通过设置无敌帧）
        target.hurtResistantTime = 10;

        // 效果提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠ 绞索缠绕！" +
                TextFormatting.GRAY + " Boss 被减速 " +
                TextFormatting.GOLD + "2" + TextFormatting.GRAY + " 秒"
        ));

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    target.posX, target.posY + target.height, target.posZ,
                    30, 0.5, 0.5, 0.5, 0.02);
        }
    }

    /**
     * 对玩家施加窒息代价
     */
    private static void applySuffocationCost(EntityPlayer player) {
        int currentAir = player.getAir();

        if (currentAir > AIR_COST) {
            // 正常消耗氧气
            player.setAir(currentAir - AIR_COST);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "你感到窒息..." +
                    TextFormatting.GRAY + " (-1 氧气)"
            ), true);
        } else {
            // 氧气不足，直接扣血
            player.setAir(0);
            player.attackEntityFrom(DamageSource.DROWN, SUFFOCATION_DAMAGE);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "同感痛苦！" +
                    TextFormatting.GRAY + " 受到窒息伤害"
            ), true);
        }
    }

    /**
     * 检查是否是 Boss 实体
     */
    private static boolean isBossEntity(EntityLivingBase entity) {
        // 检查是否有 Boss 血条
        if (!entity.isNonBoss()) return true;

        // 检查最大生命值（超过 100 视为 Boss）
        double maxHealth = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
        if (maxHealth >= 100) return true;

        // 检查类名是否包含 Boss
        String className = entity.getClass().getSimpleName().toLowerCase();
        return className.contains("boss");
    }

    /**
     * 检查玩家是否佩戴缢王之索
     */
    public static boolean hasNoose(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemNooseOfHangedKing) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    // ========== 物品信息 ==========

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "\"绳索不仅仅是用来勒紧脖子的，");
        list.add(TextFormatting.DARK_GRAY + "  它也是通往王座的升降机。\"");

        if (player == null || !hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "⚠ 需要佩戴七咒之戒才能装备");
        }

        list.add("");
        list.add(TextFormatting.GOLD + "◆ 窒息之握");
        list.add(TextFormatting.GRAY + "  攻击时 " + TextFormatting.RED + "15%" +
                TextFormatting.GRAY + " 概率触发" + TextFormatting.DARK_RED + "「处刑」");
        list.add(TextFormatting.GRAY + "  目标被吊至半空 " + TextFormatting.GOLD + "3" +
                TextFormatting.GRAY + " 秒");
        list.add(TextFormatting.GRAY + "  每秒受到 " + TextFormatting.RED + "4%" +
                TextFormatting.GRAY + " 最大生命值窒息伤害");
        list.add(TextFormatting.DARK_PURPLE + "  (无视护甲)");

        list.add("");
        list.add(TextFormatting.YELLOW + "◇ 对 Boss");
        list.add(TextFormatting.GRAY + "  无法吊起，改为施加");
        list.add(TextFormatting.GRAY + "  " + TextFormatting.AQUA + "缓慢III" +
                TextFormatting.GRAY + " + " + TextFormatting.RED + "虚弱II" +
                TextFormatting.GRAY + " 持续 2 秒");

        list.add("");
        list.add(TextFormatting.DARK_RED + "◆ 代价：同感痛苦");
        list.add(TextFormatting.RED + "  每次处刑消耗 1 格氧气");
        list.add(TextFormatting.RED + "  氧气耗尽时直接扣血");

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "吊起敌人后可以安全输出");
            list.add(TextFormatting.GRAY + "但要注意氧气管理");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多");
        }
    }
}
