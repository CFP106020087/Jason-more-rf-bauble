package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 荆棘王冠之碎片 (Shard of the Thorns)
 *
 * 外观：染血的生锈铁刺，用黑色的线穿起来
 *
 * 基础效果：攻击时有 15% 概率对自身造成 1 点伤害（流血）
 *
 * 七咒联动效果：
 * - 佩戴七咒之戒时，记录最近 5 秒内受到的所有伤害（包括七咒翻倍后的伤害）
 * - 下一次攻击会附加记录伤害的 50% 作为真实伤害爆发
 *
 * 代价：释放爆发伤害后进入 3 秒虚弱期，期间无法累积伤害
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemThornShard extends Item implements IBauble {

    // 伤害记录窗口（毫秒）
    private static final long DAMAGE_WINDOW_MS = 5000;
    // 真伤倍率
    private static final float TRUE_DAMAGE_MULTIPLIER = 0.5f;
    // 虚弱期时长（毫秒）
    private static final long WEAKNESS_DURATION_MS = 3000;
    // 自伤概率
    private static final float SELF_DAMAGE_CHANCE = 0.15f;
    // 自伤伤害
    private static final float SELF_DAMAGE_AMOUNT = 1.0f;

    // 玩家伤害记录：UUID -> 时间戳伤害列表
    private static final Map<UUID, LinkedList<DamageRecord>> DAMAGE_RECORDS = new ConcurrentHashMap<>();
    // 玩家虚弱状态：UUID -> 虚弱结束时间
    private static final Map<UUID, Long> WEAKNESS_END_TIMES = new ConcurrentHashMap<>();

    private static class DamageRecord {
        final long timestamp;
        final float damage;

        DamageRecord(float damage) {
            this.timestamp = System.currentTimeMillis();
            this.damage = damage;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > DAMAGE_WINDOW_MS;
        }
    }

    public ItemThornShard() {
        this.setMaxStackSize(1);
        this.setTranslationKey("thorn_shard");
        this.setRegistryName("thorn_shard");
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.CHARM;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 必须佩戴七咒之戒才能装备
        if (!(player instanceof EntityPlayer))
            return false;

        EntityPlayer p = (EntityPlayer) player;
        return hasCursedRing(p);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {
        // 客户端不处理
        if (player.world.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        UUID uuid = p.getUniqueID();

        // 清理过期的伤害记录
        LinkedList<DamageRecord> records = DAMAGE_RECORDS.get(uuid);
        if (records != null) {
            records.removeIf(DamageRecord::isExpired);
        }

        // 清理过期的虚弱状态
        Long weaknessEnd = WEAKNESS_END_TIMES.get(uuid);
        if (weaknessEnd != null && System.currentTimeMillis() > weaknessEnd) {
            WEAKNESS_END_TIMES.remove(uuid);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            UUID uuid = player.getUniqueID();
            DAMAGE_RECORDS.remove(uuid);
            WEAKNESS_END_TIMES.remove(uuid);
        }
    }

    // ========== 事件处理 ==========

    /**
     * 记录玩家受到的伤害（用于联动效果）
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否佩戴荆棘碎片
        if (!hasThornShard(player)) return;

        // 检查是否有七咒联动
        if (!hasCursedRing(player)) return;

        // 检查是否在虚弱期
        if (isInWeaknessPeriod(player)) return;

        // 记录伤害
        float damage = event.getAmount();
        UUID uuid = player.getUniqueID();

        LinkedList<DamageRecord> records = DAMAGE_RECORDS.computeIfAbsent(uuid, k -> new LinkedList<>());
        records.add(new DamageRecord(damage));

        // 显示累积伤害
        float totalDamage = calculateStoredDamage(player);
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_RED + "⚔ 荆棘记录: " +
                TextFormatting.GOLD + String.format("%.1f", totalDamage) +
                TextFormatting.GRAY + " (5秒内)"
        ), true);
    }

    /**
     * 攻击时释放累积伤害 + 基础自伤效果
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerAttack(LivingAttackEvent event) {
        // 获取攻击者
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        // 检查是否佩戴荆棘碎片
        if (!hasThornShard(player)) return;

        // ===== 基础效果：攻击自伤 =====
        if (player.world.rand.nextFloat() < SELF_DAMAGE_CHANCE) {
            // 延迟执行自伤，避免在攻击事件中造成问题
            player.world.getMinecraftServer().addScheduledTask(() -> {
                player.attackEntityFrom(new DamageSource("thornBleed").setDamageBypassesArmor(), SELF_DAMAGE_AMOUNT);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "✦ 荆棘刺入肌肤..."
                ), true);
            });
        }

        // ===== 七咒联动效果：伤害爆发 =====
        if (!hasCursedRing(player)) return;
        if (isInWeaknessPeriod(player)) return;

        float storedDamage = calculateStoredDamage(player);
        if (storedDamage <= 0) return;

        // 计算真实伤害
        float trueDamage = storedDamage * TRUE_DAMAGE_MULTIPLIER;

        // 释放真实伤害
        player.world.getMinecraftServer().addScheduledTask(() -> {
            TrueDamageHelper.applyWrappedTrueDamage(target, player, trueDamage,
                    TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

            // 清空伤害记录
            DAMAGE_RECORDS.remove(player.getUniqueID());

            // 进入虚弱期
            WEAKNESS_END_TIMES.put(player.getUniqueID(), System.currentTimeMillis() + WEAKNESS_DURATION_MS);

            // 效果提示
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "☠ 荆棘爆发！" +
                    TextFormatting.GOLD + String.format(" %.1f", trueDamage) +
                    TextFormatting.RED + " 真实伤害！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "进入3秒虚弱期，无法累积伤害..."
            ));

            // 粒子效果
            spawnBurstEffects(player, target);
        });
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴荆棘碎片
     */
    public static boolean hasThornShard(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemThornShard) {
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

    /**
     * 检查玩家是否在虚弱期
     */
    private static boolean isInWeaknessPeriod(EntityPlayer player) {
        Long endTime = WEAKNESS_END_TIMES.get(player.getUniqueID());
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    /**
     * 计算存储的伤害总和
     */
    private static float calculateStoredDamage(EntityPlayer player) {
        LinkedList<DamageRecord> records = DAMAGE_RECORDS.get(player.getUniqueID());
        if (records == null || records.isEmpty()) return 0;

        float total = 0;
        for (DamageRecord record : records) {
            if (!record.isExpired()) {
                total += record.damage;
            }
        }
        return total;
    }

    /**
     * 爆发粒子效果
     */
    private static void spawnBurstEffects(EntityPlayer player, EntityLivingBase target) {
        if (!(player.world instanceof net.minecraft.world.WorldServer)) return;
        net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) player.world;

        // 玩家周围红色粒子
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.REDSTONE,
                player.posX, player.posY + 1, player.posZ,
                30, 0.5, 0.5, 0.5, 0);

        // 目标位置爆炸效果
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.DAMAGE_INDICATOR,
                target.posX, target.posY + target.height / 2, target.posZ,
                20, 0.3, 0.3, 0.3, 0.1);

        // 连接线
        double dx = target.posX - player.posX;
        double dy = target.posY - player.posY;
        double dz = target.posZ - player.posZ;
        for (int i = 0; i < 10; i++) {
            double t = i / 10.0;
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT,
                    player.posX + dx * t, player.posY + 1 + dy * t, player.posZ + dz * t,
                    1, 0, 0, 0, 0.05);
        }

        // 音效
        world.playSound(null, target.getPosition(),
                net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 0.5F);
    }

    // ========== 物品信息 ==========

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "染血的生锈铁刺，用黑色的线穿起来");

        // 如果玩家没有佩戴七咒之戒，显示装备条件
        if (player == null || !hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "⚠ 需要佩戴七咒之戒才能装备");
            list.add("");
        }

        list.add("");

        // 基础效果
        list.add(TextFormatting.GRAY + "▪ 攻击时 " + TextFormatting.RED + "15%" +
                TextFormatting.GRAY + " 概率自伤 " + TextFormatting.RED + "1" + TextFormatting.GRAY + " 点");

        // 七咒联动（总是显示，因为必须有七咒才能装备）
        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "◆ 七咒联动");

        if (player != null && hasCursedRing(player)) {
            list.add(TextFormatting.LIGHT_PURPLE + "  ✓ 联动已激活");
        } else {
            list.add(TextFormatting.DARK_RED + "  ✗ 需要七咒之戒");
        }

        list.add(TextFormatting.GRAY + "  记录 5 秒内受到的所有伤害");
        list.add(TextFormatting.GRAY + "  下次攻击释放 " + TextFormatting.GOLD + "50%" +
                TextFormatting.GRAY + " 作为" + TextFormatting.RED + "真实伤害");

        // 代价
        list.add("");
        list.add(TextFormatting.DARK_RED + "◆ 代价");
        list.add(TextFormatting.RED + "  释放后进入 3 秒虚弱期");
        list.add(TextFormatting.RED + "  虚弱期间无法累积伤害");

        // 当前状态
        if (player != null && hasThornShard(player)) {
            float stored = calculateStoredDamage(player);
            boolean weak = isInWeaknessPeriod(player);

            list.add("");
            list.add(TextFormatting.GOLD + "当前状态:");
            if (weak) {
                list.add(TextFormatting.RED + "  ⚠ 虚弱期中...");
            } else {
                list.add(TextFormatting.GRAY + "  累积伤害: " + TextFormatting.GOLD + String.format("%.1f", stored));
                list.add(TextFormatting.GRAY + "  下次爆发: " + TextFormatting.RED + String.format("%.1f", stored * TRUE_DAMAGE_MULTIPLIER) + " 真伤");
            }
        }

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "七咒翻倍的伤害也会被记录");
            list.add(TextFormatting.GRAY + "适合以伤换伤的玩法风格");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多");
        }
    }
}
