package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 荆棘王冠之碎片 - 重新设计
 *
 * "王冠上的每一根刺，都曾饮过君王的鲜血。如今它渴望更多。"
 *
 * 核心机制：
 * 1. 荆棘自噬：攻击时自伤1.5点（优先扣四肢）
 * 2. 血怒累积：5秒内受伤总量转化为攻击加成，与诅咒数联动
 * 3. 荆棘再生：血量越低回血越快
 *
 * 代价：
 * - 血食（静默）：荆棘再生消耗饱食度
 * - 血债：累积伤害超过最大血量10倍时立即死亡
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemThornShard extends Item implements IBauble {

    // ========== 常量配置 ==========

    // 自伤
    private static final float SELF_DAMAGE_AMOUNT = 1.5f;

    // 血怒累积
    private static final long RAGE_WINDOW_MS = 5000;           // 5秒窗口
    private static final float RAGE_BASE_MULTIPLIER = 0.03f;   // 基础3%每点伤害
    private static final float CURSE_BONUS_PER_CURSE = 0.5f;   // 每个诅咒+50%
    private static final float DEATH_THRESHOLD_MULTIPLIER = 10.0f; // 累积超过10倍最大血量=死

    // 荆棘再生
    private static final float BASE_REGEN_PER_SECOND = 0.5f;   // 基础0.5/秒
    private static final float REGEN_LOW_HP_BONUS = 1.0f;      // 低血量最多+1倍

    // 血食代价
    private static final float HUNGER_COST_PER_HEAL = 1.0f;    // 每回复1血消耗1饱食度

    // ========== 数据存储 ==========

    // 玩家血怒数据
    private static final Map<UUID, RageData> RAGE_DATA = new ConcurrentHashMap<>();
    // 上次再生tick时间
    private static final Map<UUID, Long> LAST_REGEN_TIME = new ConcurrentHashMap<>();

    private static class RageData {
        float totalDamage = 0;
        long lastDamageTime = 0;

        void addDamage(float damage) {
            long now = System.currentTimeMillis();
            // 超过窗口则重置
            if (now - lastDamageTime > RAGE_WINDOW_MS) {
                totalDamage = 0;
            }
            totalDamage += damage;
            lastDamageTime = now;
        }

        float getCurrentRage() {
            long now = System.currentTimeMillis();
            if (now - lastDamageTime > RAGE_WINDOW_MS) {
                return 0;
            }
            return totalDamage;
        }

        boolean isActive() {
            return System.currentTimeMillis() - lastDamageTime <= RAGE_WINDOW_MS;
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
        if (!(player instanceof EntityPlayer)) return false;
        return hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        UUID uuid = player.getUniqueID();

        // 荆棘再生
        handleThornRegeneration(player, uuid);

        // 检查血债死亡
        checkRageOverload(player, uuid);
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            UUID uuid = player.getUniqueID();
            RAGE_DATA.remove(uuid);
            LAST_REGEN_TIME.remove(uuid);
        }
    }

    // ========== 荆棘再生 ==========

    private void handleThornRegeneration(EntityPlayer player, UUID uuid) {
        // 每秒执行一次
        long now = System.currentTimeMillis();
        Long lastTime = LAST_REGEN_TIME.get(uuid);
        if (lastTime != null && now - lastTime < 1000) return;
        LAST_REGEN_TIME.put(uuid, now);

        // 计算回血量
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) return;

        // 回血公式：基础 × (2 - 当前血量比例)
        float healthRatio = currentHealth / maxHealth;
        float regenAmount = BASE_REGEN_PER_SECOND * (1 + REGEN_LOW_HP_BONUS * (1 - healthRatio));

        // 检查饱食度（静默代价）
        FoodStats food = player.getFoodStats();
        float hungerCost = regenAmount * HUNGER_COST_PER_HEAL;

        if (food.getFoodLevel() >= hungerCost) {
            // 有足够饱食度，执行回血
            // 消耗饱食度（静默，无提示）
            food.setFoodLevel(Math.max(0, food.getFoodLevel() - (int) Math.ceil(hungerCost)));

            // 执行回血 - FirstAid兼容
            healPlayerFirstAidCompat(player, regenAmount);
        }
        // 饱食度不足时不回血，也不提示
    }

    /**
     * FirstAid兼容的治疗 - 优先治疗四肢
     */
    private void healPlayerFirstAidCompat(EntityPlayer player, float amount) {
        // 尝试使用FirstAid API
        if (tryFirstAidHeal(player, amount)) {
            return;
        }
        // 回退到普通治疗
        player.heal(amount);
    }

    @Optional.Method(modid = "firstaid")
    private boolean tryFirstAidHeal(EntityPlayer player, float amount) {
        try {
            ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel damageModel =
                ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.getExtendedHealthSystem(player);

            if (damageModel == null) return false;

            // 优先治疗四肢
            ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart[] limbs = {
                damageModel.LEFT_ARM, damageModel.RIGHT_ARM,
                damageModel.LEFT_LEG, damageModel.RIGHT_LEG
            };

            float remaining = amount;
            for (ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart limb : limbs) {
                if (remaining <= 0) break;
                float canHeal = limb.getMaxHealth() - limb.currentHealth;
                if (canHeal > 0) {
                    float healed = Math.min(canHeal, remaining);
                    limb.currentHealth += healed;
                    remaining -= healed;
                }
            }

            // 剩余治疗头和身体
            if (remaining > 0) {
                float headHeal = Math.min(damageModel.HEAD.getMaxHealth() - damageModel.HEAD.currentHealth, remaining / 2);
                damageModel.HEAD.currentHealth += headHeal;
                remaining -= headHeal;
            }
            if (remaining > 0) {
                float bodyHeal = Math.min(damageModel.BODY.getMaxHealth() - damageModel.BODY.currentHealth, remaining);
                damageModel.BODY.currentHealth += bodyHeal;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 血债检查 ==========

    private void checkRageOverload(EntityPlayer player, UUID uuid) {
        RageData data = RAGE_DATA.get(uuid);
        if (data == null || !data.isActive()) return;

        float threshold = player.getMaxHealth() * DEATH_THRESHOLD_MULTIPLIER;
        if (data.totalDamage >= threshold) {
            // 血债已满，执行死亡
            // 使用高额普通伤害，避免清空背包
            player.attackEntityFrom(
                new DamageSource("thornOverload").setDamageBypassesArmor(),
                Float.MAX_VALUE / 2  // 极高伤害但不是无限
            );

            // 清除数据
            RAGE_DATA.remove(uuid);
        }
    }

    // ========== 事件处理 ==========

    /**
     * 玩家受伤时累积血怒
     */
    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!hasThornShard(player)) return;

        // 累积伤害
        UUID uuid = player.getUniqueID();
        RageData data = RAGE_DATA.computeIfAbsent(uuid, k -> new RageData());
        data.addDamage(event.getAmount());
    }

    /**
     * 玩家攻击时：自伤 + 应用血怒加成
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (!hasThornShard(player)) return;

        UUID uuid = player.getUniqueID();

        // 1. 执行自伤（延迟执行避免事件冲突）
        player.world.getMinecraftServer().addScheduledTask(() -> {
            applySelfDamage(player);
        });

        // 2. 应用血怒伤害加成
        RageData data = RAGE_DATA.get(uuid);
        if (data != null && data.isActive()) {
            float bonus = calculateDamageBonus(player, data.getCurrentRage());
            if (bonus > 0) {
                float newDamage = event.getAmount() * (1 + bonus);
                event.setAmount(newDamage);
            }
        }
    }

    /**
     * 执行自伤 - FirstAid兼容，优先扣四肢
     */
    private static void applySelfDamage(EntityPlayer player) {
        // 尝试FirstAid兼容自伤
        if (tryFirstAidSelfDamage(player, SELF_DAMAGE_AMOUNT)) {
            // 自伤也计入血怒
            UUID uuid = player.getUniqueID();
            RageData data = RAGE_DATA.computeIfAbsent(uuid, k -> new RageData());
            data.addDamage(SELF_DAMAGE_AMOUNT);
            return;
        }

        // 回退到普通自伤
        player.attackEntityFrom(
            new DamageSource("thornSelf").setDamageBypassesArmor(),
            SELF_DAMAGE_AMOUNT
        );
    }

    @Optional.Method(modid = "firstaid")
    private static boolean tryFirstAidSelfDamage(EntityPlayer player, float damage) {
        try {
            ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel damageModel =
                ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.getExtendedHealthSystem(player);

            if (damageModel == null) return false;

            // 优先扣四肢
            ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart[] limbs = {
                damageModel.LEFT_ARM, damageModel.RIGHT_ARM,
                damageModel.LEFT_LEG, damageModel.RIGHT_LEG
            };

            float remaining = damage;
            for (ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart limb : limbs) {
                if (remaining <= 0) break;
                // 不让四肢低于1血
                float canTake = limb.currentHealth - 1.0f;
                if (canTake > 0) {
                    float taken = Math.min(canTake, remaining);
                    limb.currentHealth -= taken;
                    remaining -= taken;
                }
            }

            // 如果四肢扣不完，剩余扣身体（不扣头）
            if (remaining > 0 && damageModel.BODY.currentHealth > 2.0f) {
                float bodyTake = Math.min(damageModel.BODY.currentHealth - 2.0f, remaining);
                damageModel.BODY.currentHealth -= bodyTake;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算伤害加成
     */
    private static float calculateDamageBonus(EntityPlayer player, float rageAmount) {
        if (rageAmount <= 0) return 0;

        // 基础加成
        float baseBonus = rageAmount * RAGE_BASE_MULTIPLIER;

        // 诅咒联动
        int curseCount = countCurses(player);
        float curseMultiplier = 1 + curseCount * CURSE_BONUS_PER_CURSE;

        return baseBonus * curseMultiplier;
    }

    /**
     * 计算玩家身上的诅咒数量
     */
    private static int countCurses(EntityPlayer player) {
        int count = 0;
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty()) {
                String name = bauble.getItem().getRegistryName() != null ?
                    bauble.getItem().getRegistryName().getPath() : "";
                // 计算诅咒类饰品
                if (name.equals("cursed_ring") ||           // 七咒之戒
                    name.equals("thorn_shard") ||           // 荆棘王冠之碎片
                    name.equals("script_of_fifth_act") ||   // 第五幕剧本
                    name.equals("noose_of_hanged_king") ||  // 缢王之索
                    name.equals("eye_of_void") ||           // 虚无之眸
                    name.equals("gluttony_finger_bone") ||  // 饕餮指骨
                    name.equals("grudge_crystal") ||        // 怨念结晶
                    name.equals("curse_spread")) {          // 诅咒蔓延
                    count++;
                }
            }
        }
        return count;
    }

    // ========== 辅助方法 ==========

    public static boolean hasThornShard(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemThornShard) {
                return true;
            }
        }
        return false;
    }

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
     * 获取当前血怒值（供外部查询）
     */
    public static float getCurrentRage(EntityPlayer player) {
        RageData data = RAGE_DATA.get(player.getUniqueID());
        return data != null ? data.getCurrentRage() : 0;
    }

    /**
     * 获取当前伤害加成（供外部查询）
     */
    public static float getCurrentDamageBonus(EntityPlayer player) {
        return calculateDamageBonus(player, getCurrentRage(player));
    }

    // ========== Tooltip ==========

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
        list.add(TextFormatting.DARK_RED + "        ☬ " + TextFormatting.BOLD + "荆棘王冠之碎片" +
                 TextFormatting.RESET + TextFormatting.DARK_RED + " ☬");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
        list.add("");
        list.add(TextFormatting.GRAY + "  \"王冠上的每一根刺，都曾饮过");
        list.add(TextFormatting.GRAY + "   君王的鲜血。如今它渴望更多。\"");
        list.add("");

        // 装备条件
        if (player == null || !hasCursedRing(player)) {
            list.add(TextFormatting.DARK_RED + "  ⚠ 需要佩戴七咒之戒才能装备");
            list.add("");
        }

        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");

        // 荆棘自噬
        list.add(TextFormatting.RED + "  ▣ 荆棘自噬");
        list.add(TextFormatting.GRAY + "    攻击时，荆棘刺入己身");
        list.add(TextFormatting.GRAY + "    自伤 " + TextFormatting.RED + "1.5" +
                 TextFormatting.GRAY + " 点（优先四肢）");
        list.add("");

        // 血怒累积
        list.add(TextFormatting.DARK_RED + "  ▣ 血怒累积");
        list.add(TextFormatting.GRAY + "    5秒内受到的伤害转化为攻击力");
        list.add(TextFormatting.GRAY + "    加成 = 累积伤害 × " + TextFormatting.GOLD + "3%" +
                 TextFormatting.GRAY + " × 诅咒倍率");
        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "    ◆ 诅咒联动");
        list.add(TextFormatting.LIGHT_PURPLE + "      每件诅咒饰品 +50% 倍率");

        if (player != null) {
            int curses = countCurses(player);
            float multiplier = 1 + curses * CURSE_BONUS_PER_CURSE;
            list.add(TextFormatting.GRAY + "      当前: " + TextFormatting.GOLD + curses +
                     TextFormatting.GRAY + " 诅咒 → " + TextFormatting.RED +
                     String.format("%.0f%%", multiplier * 100) + TextFormatting.GRAY + " 倍率");
        }
        list.add("");

        // 荆棘再生
        list.add(TextFormatting.GREEN + "  ▣ 荆棘再生");
        list.add(TextFormatting.GRAY + "    血量越低，回血越快");
        list.add(TextFormatting.GRAY + "    基础 " + TextFormatting.GREEN + "0.5" +
                 TextFormatting.GRAY + "/秒，低血量最高 " + TextFormatting.GREEN + "1.0" +
                 TextFormatting.GRAY + "/秒");
        list.add("");

        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");

        // 代价
        list.add(TextFormatting.DARK_RED + "  ✦ 血债");
        list.add(TextFormatting.RED + "    累积伤害超过最大血量" + TextFormatting.DARK_RED + "十倍");
        list.add(TextFormatting.RED + "    时，" + TextFormatting.DARK_RED + "立即死亡");
        list.add("");

        // 当前状态
        if (player != null && hasThornShard(player)) {
            list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
            list.add("");

            float rage = getCurrentRage(player);
            float bonus = getCurrentDamageBonus(player);
            float threshold = player.getMaxHealth() * DEATH_THRESHOLD_MULTIPLIER;
            float ratio = rage / threshold;

            list.add(TextFormatting.GOLD + "  ◈ 当前状态");

            // 血怒条
            String rageBar = buildProgressBar(ratio, 20);
            TextFormatting rageColor = ratio > 0.7f ? TextFormatting.DARK_RED :
                                       ratio > 0.4f ? TextFormatting.RED : TextFormatting.YELLOW;
            list.add(TextFormatting.GRAY + "    血怒: " + rageColor + rageBar);
            list.add(TextFormatting.GRAY + "          " + String.format("%.1f", rage) +
                     " / " + String.format("%.0f", threshold));

            if (bonus > 0) {
                list.add(TextFormatting.GRAY + "    攻击加成: " + TextFormatting.RED + "+" +
                         String.format("%.0f%%", bonus * 100));
            }
            // 不显示任何危险警告，保持静默
        }

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "设计理念:");
            list.add(TextFormatting.GRAY + "\"痛苦是力量的源泉，");
            list.add(TextFormatting.GRAY + " 但贪婪会吞噬一切。\"");
        } else {
            list.add(TextFormatting.DARK_GRAY + "        按住 Shift 查看更多");
        }
    }

    /**
     * 构建进度条
     */
    private static String buildProgressBar(float ratio, int length) {
        ratio = Math.min(1.0f, Math.max(0, ratio));
        int filled = (int) (ratio * length);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }
}
