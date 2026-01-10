package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 荆棘王冠之碎片
 *
 * "王冠上的每一根刺，都曾饮过君王的鲜血。如今它渴望更多。"
 *
 * 核心机制：
 * 1. 荆棘自噬：攻击时自伤（优先扣四肢）
 * 2. 王座之血：5秒内受伤总量转化为攻击加成，与诅咒数联动
 * 3. 荆棘再生：血量越低回血越快
 *
 * 代价：
 * - 累积伤害超过最大血量5倍时立即死亡并清空物品（无任何提示）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemThornShard extends Item implements IBauble {

    // ========== 常量配置 ==========

    // 自伤
    private static final float SELF_DAMAGE_AMOUNT = 5.0f;

    // 王座之血
    private static final long BLOOD_WINDOW_MS = 5000;           // 5秒窗口
    private static final float BLOOD_BASE_MULTIPLIER = 0.06f;   // 基础6%每点伤害
    private static final float CURSE_BONUS_PER_CURSE = 0.1f;    // 每个诅咒+10%
    private static final float DEATH_THRESHOLD_MULTIPLIER = 5.0f; // 累积超过5倍最大血量=死

    // 荆棘再生
    private static final float BASE_REGEN_PER_SECOND = 0.5f;    // 基础0.5/秒
    private static final float REGEN_LOW_HP_BONUS = 1.0f;       // 低血量最多+1倍

    // 四肢保护阈值
    private static final float LIMB_MIN_THRESHOLD = 1.0f;

    // ========== 数据存储 ==========

    // 玩家王座之血数据
    private static final Map<UUID, BloodData> BLOOD_DATA = new ConcurrentHashMap<>();
    // 上次再生tick时间
    private static final Map<UUID, Long> LAST_REGEN_TIME = new ConcurrentHashMap<>();

    private static class BloodData {
        float totalDamage = 0;
        long lastDamageTime = 0;

        void addDamage(float damage) {
            long now = System.currentTimeMillis();
            if (now - lastDamageTime > BLOOD_WINDOW_MS) {
                totalDamage = 0;
            }
            totalDamage += damage;
            lastDamageTime = now;
        }

        float getCurrentBlood() {
            long now = System.currentTimeMillis();
            if (now - lastDamageTime > BLOOD_WINDOW_MS) {
                return 0;
            }
            return totalDamage;
        }

        boolean isActive() {
            return System.currentTimeMillis() - lastDamageTime <= BLOOD_WINDOW_MS;
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

        // 检查死亡阈值
        checkBloodOverload(player, uuid);
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            UUID uuid = player.getUniqueID();
            BLOOD_DATA.remove(uuid);
            LAST_REGEN_TIME.remove(uuid);
        }
    }

    // ========== 荆棘再生 ==========

    private void handleThornRegeneration(EntityPlayer player, UUID uuid) {
        long now = System.currentTimeMillis();
        Long lastTime = LAST_REGEN_TIME.get(uuid);
        if (lastTime != null && now - lastTime < 1000) return;
        LAST_REGEN_TIME.put(uuid, now);

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) return;

        // 回血公式：基础 × (2 - 当前血量比例)
        float healthRatio = currentHealth / maxHealth;
        float regenAmount = BASE_REGEN_PER_SECOND * (1 + REGEN_LOW_HP_BONUS * (1 - healthRatio));

        // 普通治疗（FirstAid会自动处理部位分配）
        player.heal(regenAmount);
    }

    // ========== 死亡检查 ==========

    private void checkBloodOverload(EntityPlayer player, UUID uuid) {
        BloodData data = BLOOD_DATA.get(uuid);
        if (data == null || !data.isActive()) return;

        float threshold = player.getMaxHealth() * DEATH_THRESHOLD_MULTIPLIER;
        if (data.totalDamage >= threshold) {
            // 篡位者的末路：清空所有物品（除了七咒之戒）
            clearAllItemsExceptCursedRing(player);

            // 使用高额普通伤害
            player.attackEntityFrom(
                new DamageSource("thornOverload").setDamageBypassesArmor(),
                Float.MAX_VALUE / 2
            );
            BLOOD_DATA.remove(uuid);
        }
    }

    /**
     * 清空玩家所有物品，只保留七咒之戒
     */
    private void clearAllItemsExceptCursedRing(EntityPlayer player) {
        // 清空背包
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            player.inventory.mainInventory.set(i, ItemStack.EMPTY);
        }

        // 清空盔甲栏
        for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
            player.inventory.armorInventory.set(i, ItemStack.EMPTY);
        }

        // 清空副手
        for (int i = 0; i < player.inventory.offHandInventory.size(); i++) {
            player.inventory.offHandInventory.set(i, ItemStack.EMPTY);
        }

        // 清空饰品栏（保留七咒之戒）
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack bauble = baubles.getStackInSlot(i);
            if (!bauble.isEmpty()) {
                // 只保留七咒之戒
                if (bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                    continue;
                }
                baubles.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ========== 事件处理 ==========

    /**
     * 玩家受伤时累积
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!hasThornShard(player)) return;

        UUID uuid = player.getUniqueID();
        BloodData data = BLOOD_DATA.computeIfAbsent(uuid, k -> new BloodData());
        data.addDamage(event.getAmount());
    }

    /**
     * FirstAid兼容 - 所有伤害优先转移到四肢
     * 参考 IntegratedShieldSystem.prioritizeLimbDamage
     */
    @Optional.Method(modid = "firstaid")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFirstAidDamage(ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!hasThornShard(player)) return;

        ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel before = event.getBeforeDamage();
        ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel after = event.getAfterDamage();

        // 计算要害受到的伤害
        float headDamage = Math.max(0, before.HEAD.currentHealth - after.HEAD.currentHealth);
        float bodyDamage = Math.max(0, before.BODY.currentHealth - after.BODY.currentHealth);
        float vitalDamage = headDamage + bodyDamage;

        if (vitalDamage <= 0) return;

        // 计算四肢可承受的伤害
        ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart[] limbs = {
            after.LEFT_ARM, after.RIGHT_ARM,
            after.LEFT_LEG, after.RIGHT_LEG
        };

        float limbCapacity = 0;
        for (ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart limb : limbs) {
            float canTake = limb.currentHealth - LIMB_MIN_THRESHOLD;
            if (canTake > 0) limbCapacity += canTake;
        }

        // 转移伤害
        float shift = Math.min(vitalDamage, limbCapacity);
        if (shift <= 0) return;

        // 恢复要害血量
        float headShare = headDamage / vitalDamage;
        float bodyShare = bodyDamage / vitalDamage;

        after.HEAD.currentHealth += shift * headShare;
        after.BODY.currentHealth += shift * bodyShare;

        // 限制不超过原血量
        if (after.HEAD.currentHealth > before.HEAD.currentHealth) {
            after.HEAD.currentHealth = before.HEAD.currentHealth;
        }
        if (after.BODY.currentHealth > before.BODY.currentHealth) {
            after.BODY.currentHealth = before.BODY.currentHealth;
        }

        // 将伤害分配到四肢
        float remaining = shift;
        for (ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart limb : limbs) {
            if (remaining <= 0) break;
            float canTake = limb.currentHealth - LIMB_MIN_THRESHOLD;
            if (canTake <= 0) continue;

            float taken = Math.min(canTake, remaining);
            limb.currentHealth -= taken;
            remaining -= taken;
        }
    }

    /**
     * 玩家攻击时：自伤 + 应用伤害加成
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (!hasThornShard(player)) return;

        UUID uuid = player.getUniqueID();

        // 执行自伤（延迟执行避免事件冲突）
        // 使用普通伤害，FirstAid会自动处理，然后被我们的onFirstAidDamage拦截优先扣四肢
        player.world.getMinecraftServer().addScheduledTask(() -> {
            player.attackEntityFrom(
                new DamageSource("thornSelf").setDamageBypassesArmor(),
                SELF_DAMAGE_AMOUNT
            );
        });

        // 应用伤害加成
        BloodData data = BLOOD_DATA.get(uuid);
        if (data != null && data.isActive()) {
            float bonus = calculateDamageBonus(player, data.getCurrentBlood());
            if (bonus > 0) {
                float newDamage = event.getAmount() * (1 + bonus);
                event.setAmount(newDamage);
            }
        }
    }

    /**
     * 计算伤害加成
     */
    private static float calculateDamageBonus(EntityPlayer player, float bloodAmount) {
        if (bloodAmount <= 0) return 0;

        float baseBonus = bloodAmount * BLOOD_BASE_MULTIPLIER;
        int curseCount = getCurseAmount(player);
        float curseMultiplier = 1 + curseCount * CURSE_BONUS_PER_CURSE;

        return baseBonus * curseMultiplier;
    }

    /**
     * 获取玩家的诅咒数量（参考诅咒蔓延的实现）
     * - 七咒之戒算7个
     * - 装备和饰品上的诅咒附魔各算1个
     */
    private static int getCurseAmount(EntityPlayer player) {
        int count = 0;
        boolean ringCounted = false;

        for (ItemStack stack : getFullEquipment(player)) {
            if (stack.isEmpty()) continue;

            // 七咒之戒算7个诅咒（只算一次）
            if (!ringCounted &&
                    stack.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(stack.getItem().getRegistryName().getPath())) {
                count += 7;
                ringCounted = true;
            }

            // 计算诅咒附魔数量
            count += getCurseEnchantmentCount(stack);
        }

        return count;
    }

    /**
     * 获取物品上的诅咒附魔数量
     */
    private static int getCurseEnchantmentCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        int count = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (enchantment != null && enchantment.isCurse() && level > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取玩家所有装备（包括饰品）
     */
    private static List<ItemStack> getFullEquipment(EntityPlayer player) {
        List<ItemStack> equipmentStacks = new ArrayList<>();

        equipmentStacks.add(player.getHeldItemMainhand());
        equipmentStacks.add(player.getHeldItemOffhand());
        equipmentStacks.addAll(player.inventory.armorInventory);

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            equipmentStacks.add(baubles.getStackInSlot(i));
        }

        return equipmentStacks;
    }

    // ========== 辅助方法 ==========

    public static boolean hasThornShard(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack bauble = baubles.getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemThornShard) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCursedRing(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack bauble = baubles.getStackInSlot(i);
            if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static float getCurrentBlood(EntityPlayer player) {
        BloodData data = BLOOD_DATA.get(player.getUniqueID());
        return data != null ? data.getCurrentBlood() : 0;
    }

    public static float getCurrentDamageBonus(EntityPlayer player) {
        return calculateDamageBonus(player, getCurrentBlood(player));
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
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                 "  \"王冠上的每一根刺，都曾饮过");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                 "   君王的鲜血。如今它渴望更多。\"");
        list.add("");

        if (player == null || !hasCursedRing(player)) {
            list.add(TextFormatting.DARK_RED + "  ⚠ 唯有七咒之戒的承载者");
            list.add(TextFormatting.DARK_RED + "    方可触碰这残破的王权");
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
            return;
        }

        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");

        // 荆棘自噬
        list.add(TextFormatting.DARK_RED + "  ◈ 荆棘自噬");
        list.add(TextFormatting.GRAY + "    每一次挥击，荆棘都会深深刺入血肉");
        list.add(TextFormatting.DARK_GRAY + "    （造成 " + TextFormatting.RED + "5" +
                 TextFormatting.DARK_GRAY + " 点自伤，优先侵蚀四肢）");
        list.add("");

        // 王座之血
        list.add(TextFormatting.DARK_RED + "  ◈ 王座之血");
        list.add(TextFormatting.GRAY + "    鲜血浸透荆棘，痛苦化作力量");
        list.add(TextFormatting.GRAY + "    五秒内承受的苦难，将铸成利刃");
        list.add("");

        // 诅咒烙印
        int curses = getCurseAmount(player);
        float multiplier = 1 + curses * CURSE_BONUS_PER_CURSE;

        list.add(TextFormatting.DARK_PURPLE + "  ◈ 诅咒烙印");
        list.add(TextFormatting.LIGHT_PURPLE + "    身负的诅咒越深，王冠便越锋利");
        list.add(TextFormatting.DARK_GRAY + "    （每个诅咒附魔 +10% 倍率）");
        list.add(TextFormatting.DARK_GRAY + "    当前烙印: " + TextFormatting.DARK_PURPLE + curses +
                 TextFormatting.DARK_GRAY + " → 伤害倍率 " + TextFormatting.RED +
                 String.format("×%.1f", multiplier));
        list.add("");

        // 荆棘再生
        list.add(TextFormatting.DARK_GREEN + "  ◈ 荆棘再生");
        list.add(TextFormatting.GRAY + "    濒死之际，荆棘反哺其主");
        list.add(TextFormatting.DARK_GRAY + "    （血量越低，回复越快）");
        list.add("");

        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");

        // 代价
        list.add(TextFormatting.BLACK + "" + TextFormatting.BOLD + "  ✦ 篡位者的末路");
        list.add(TextFormatting.DARK_RED + "    当承受的苦难超越肉体所能承载...");
        list.add(TextFormatting.DARK_RED + "    你将失去除七咒之戒外的" + TextFormatting.RED +
                 TextFormatting.BOLD + "一切" + TextFormatting.RESET + TextFormatting.DARK_RED + "。");
        list.add(TextFormatting.DARK_GRAY + "    （背包、装备、饰品，尽数湮灭）");
        list.add("");

        // 当前状态
        if (hasThornShard(player)) {
            float blood = getCurrentBlood(player);
            float bonus = getCurrentDamageBonus(player);
            float threshold = player.getMaxHealth() * DEATH_THRESHOLD_MULTIPLIER;

            if (blood > 0) {
                list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
                list.add("");
                list.add(TextFormatting.GRAY + "  王座之血: " + TextFormatting.DARK_RED +
                         String.format("%.1f", blood) + TextFormatting.DARK_GRAY + " / " +
                         String.format("%.0f", threshold));
                if (bonus > 0) {
                    list.add(TextFormatting.GRAY + "  力量增幅: " + TextFormatting.RED + "+" +
                             String.format("%.0f%%", bonus * 100));
                }
            }
        }

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                     "  这碎片来自一位无名君王的王冠。");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                     "  据说他在加冕之夜便被荆棘刺穿,");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                     "  鲜血染红了整座王座。");
        } else {
            list.add(TextFormatting.DARK_GRAY + "        [ Shift - 残章 ]");
        }
    }
}
