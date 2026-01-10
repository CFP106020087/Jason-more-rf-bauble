package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
 *
 * "王的恩典从不平等——只赐予臣服于他的人。"
 *
 * 致敬 SCP 基金会的缢王 (The Hanged King)
 *
 * 核心机制【王恩】：
 * - 攻击时触发"处刑"，概率 = 15% + 5% × 诅咒数量（上限75%）
 * - 敌人被吊起10秒，每秒受到 5% 最大生命值真伤
 * - 玩家同步自缢：每秒受到 5% × 承受比例 的真伤
 *
 * 欺骗机制：
 * - 诅咒 = 对缢王的臣服
 * - 诅咒越多，玩家承受的伤害比例越低
 * - 0诅咒：100%（真正的"公平"）
 * - 7诅咒：约63%
 * - 30诅咒：约16%
 * - 60诅咒：约5%
 *
 * 王恩的真相：这从来不是公平的交易
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemNooseOfHangedKing extends Item implements IBauble {

    // ========== 常量配置 ==========

    // 处刑触发
    private static final float BASE_EXECUTION_CHANCE = 0.15f;      // 基础15%
    private static final float CURSE_BONUS_PER_CURSE = 0.05f;      // 每诅咒+5%
    private static final float MAX_EXECUTION_CHANCE = 0.75f;       // 上限75%

    // 处刑效果
    private static final int EXECUTION_DURATION_TICKS = 200;       // 10秒
    private static final float DAMAGE_PERCENT_PER_SECOND = 0.05f;  // 5%最大生命值/秒

    // 王恩机制：诅咒减伤
    // 公式：playerRatio = MIN_PLAYER_RATIO + (1 - MIN_PLAYER_RATIO) × e^(-curses × CURSE_DECAY_RATE)
    private static final float MIN_PLAYER_RATIO = 0.04f;           // 最低承受4%
    private static final float CURSE_DECAY_RATE = 0.07f;           // 衰减系数

    // 七咒之戒的诅咒计数
    private static final int CURSED_RING_CURSE_COUNT = 7;

    // 被处刑的实体记录：实体ID -> 处刑数据
    private static final Map<Integer, ExecutionData> EXECUTED_ENTITIES = new ConcurrentHashMap<>();

    private static class ExecutionData {
        final long endTime;
        final double hangX;
        final double hangY;
        final double hangZ;
        final EntityPlayer executioner;
        final int dimensionId;
        final int curseCount;        // 处刑时的诅咒数，用于计算王恩减伤
        final float playerRatio;     // 玩家承受比例（处刑时锁定）

        ExecutionData(EntityPlayer executioner, double x, double y, double z, float hangHeight, int dimensionId, int curseCount) {
            this.endTime = System.currentTimeMillis() + (EXECUTION_DURATION_TICKS * 50L);
            this.hangX = x;
            this.hangY = y + hangHeight;
            this.hangZ = z;
            this.executioner = executioner;
            this.dimensionId = dimensionId;
            this.curseCount = curseCount;
            this.playerRatio = calculatePlayerRatio(curseCount);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > endTime;
        }

        int getRemainingSeconds() {
            long remaining = endTime - System.currentTimeMillis();
            return remaining > 0 ? (int) (remaining / 1000) : 0;
        }
    }

    /**
     * 计算玩家承受伤害比例（王恩机制）
     * 诅咒越多，比例越低
     */
    private static float calculatePlayerRatio(int curseCount) {
        // playerRatio = MIN + (1 - MIN) × e^(-curses × DECAY)
        return MIN_PLAYER_RATIO + (1.0f - MIN_PLAYER_RATIO) * (float) Math.exp(-curseCount * CURSE_DECAY_RATE);
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

        // 计算诅咒数量（七咒之戒 + 诅咒附魔）
        int curseCount = getCurseCount(player);

        // 计算触发概率：基础 15% + 5% × 诅咒数量
        float executionChance = Math.min(MAX_EXECUTION_CHANCE,
                BASE_EXECUTION_CHANCE + CURSE_BONUS_PER_CURSE * curseCount);

        // 概率触发
        if (player.world.rand.nextFloat() > executionChance) return;

        // 执行处刑（对所有生物有效，包括 Boss）
        // 代价由王恩机制处理：玩家同步自缢
        executeTarget(player, target, curseCount);
    }

    /**
     * 每 tick 更新被处刑的实体
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        World world = event.world;

        Iterator<Map.Entry<Integer, ExecutionData>> it = EXECUTED_ENTITIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ExecutionData> entry = it.next();
            int entityId = entry.getKey();
            ExecutionData data = entry.getValue();

            // 只处理同一维度的实体
            if (data.dimensionId != world.provider.getDimension()) {
                continue;
            }

            net.minecraft.entity.Entity entity = world.getEntityByID(entityId);
            if (entity == null || !(entity instanceof EntityLivingBase)) {
                it.remove();
                continue;
            }

            EntityLivingBase target = (EntityLivingBase) entity;

            // 检查处刑者是否还在线/存活
            if (data.executioner == null || !data.executioner.isEntityAlive()) {
                target.fallDistance = 0;
                it.remove();
                continue;
            }

            // 检查是否过期
            if (data.isExpired()) {
                // 处刑结束，让实体落下
                target.fallDistance = 0;
                it.remove();

                // 效果提示
                if (!data.executioner.world.isRemote) {
                    data.executioner.sendStatusMessage(new TextComponentString(
                            TextFormatting.GRAY + "处刑结束..."
                    ), true);
                }
                continue;
            }

            // ===== 完全定身：锁定位置和所有运动 =====
            target.setPosition(data.hangX, data.hangY, data.hangZ);
            target.motionX = 0;
            target.motionY = 0;
            target.motionZ = 0;
            target.velocityChanged = true;
            target.fallDistance = 0;
            target.onGround = false;

            // 禁止 AI 移动（如果有的话）
            if (target instanceof net.minecraft.entity.EntityLiving) {
                net.minecraft.entity.EntityLiving living = (net.minecraft.entity.EntityLiving) target;
                living.setNoAI(false); // 保持 AI 但位置被锁定
            }

            // 每秒造成窒息伤害（每 20 tick）
            if (world.getTotalWorldTime() % 20 == 0) {
                // ===== 敌人伤害 =====
                float targetMaxHealth = target.getMaxHealth();
                float targetDamage = Math.max(1.0f, targetMaxHealth * DAMAGE_PERCENT_PER_SECOND);

                TrueDamageHelper.applyWrappedTrueDamage(target, data.executioner, targetDamage,
                        TrueDamageHelper.TrueDamageFlag.EXECUTE);

                // ===== 王恩：玩家同步自缢 =====
                float playerMaxHealth = data.executioner.getMaxHealth();
                float playerDamage = playerMaxHealth * DAMAGE_PERCENT_PER_SECOND * data.playerRatio;
                playerDamage = Math.max(0.5f, playerDamage); // 最低0.5伤害

                // 玩家受到窒息伤害（无视护甲）
                data.executioner.attackEntityFrom(
                        new DamageSource("noose_self").setDamageBypassesArmor(),
                        playerDamage
                );

                // 粒子效果 - 敌人
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    // 窒息烟雾
                    ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                            target.posX, target.posY + target.height, target.posZ,
                            15, 0.2, 0.2, 0.2, 0.02);
                    // 绞索魔法效果
                    ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                            target.posX, target.posY + target.height + 0.5, target.posZ,
                            8, 0.1, 0.1, 0.1, 0.05);

                    // 玩家自缢效果
                    ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                            data.executioner.posX,
                            data.executioner.posY + data.executioner.height - 0.2,
                            data.executioner.posZ,
                            5, 0.1, 0.1, 0.1, 0.01);
                }

                // 显示状态
                if (!data.executioner.world.isRemote) {
                    int remaining = data.getRemainingSeconds();
                    int ratioPercent = (int) (data.playerRatio * 100);
                    data.executioner.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "☠ 王恩 " +
                            TextFormatting.GOLD + remaining + "s" +
                            TextFormatting.GRAY + " | 敌:" + TextFormatting.RED + String.format("%.1f", targetDamage) +
                            TextFormatting.GRAY + " 己:" + TextFormatting.DARK_RED + String.format("%.1f", playerDamage) +
                            TextFormatting.DARK_GRAY + " (" + ratioPercent + "%)"
                    ), true);
                }
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 计算玩家的诅咒数量
     * - 七咒之戒 = 7个诅咒
     * - 装备和饰品上的诅咒附魔各算1个
     */
    private static int getCurseCount(EntityPlayer player) {
        int count = 0;
        boolean ringCounted = false;

        // 检查装备栏（盔甲 + 手持物品）
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            count += countCursesOnItem(stack);
        }

        // 检查饰品栏
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack bauble = baubles.getStackInSlot(i);
            if (bauble.isEmpty()) continue;

            // 七咒之戒算7个诅咒（只算一次）
            if (!ringCounted &&
                    bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                count += CURSED_RING_CURSE_COUNT;
                ringCounted = true;
            }

            // 计算诅咒附魔
            count += countCursesOnItem(bauble);
        }

        return count;
    }

    /**
     * 计算单个物品上的诅咒附魔数量
     */
    private static int countCursesOnItem(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        int count = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment ench = entry.getKey();
            if (ench != null && ench.isCurse() && entry.getValue() > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 执行处刑（对所有生物有效）
     */
    private static void executeTarget(EntityPlayer player, EntityLivingBase target, int curseCount) {
        // 计算悬挂高度
        float hangHeight = 2.5f + player.world.rand.nextFloat() * 1.0f;

        // 记录处刑数据（包含诅咒数用于计算王恩减伤）
        ExecutionData data = new ExecutionData(player, target.posX, target.posY, target.posZ, hangHeight,
                player.world.provider.getDimension(), curseCount);
        EXECUTED_ENTITIES.put(target.getEntityId(), data);

        // 初始向上推力（视觉效果）
        target.motionY = 0.8;
        target.velocityChanged = true;

        // 效果提示
        String targetName = target.hasCustomName() ? target.getCustomNameTag() : target.getName();
        int ratioPercent = (int) (data.playerRatio * 100);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠ 王恩降临！" +
                TextFormatting.GRAY + " [" + targetName + "]" +
                TextFormatting.DARK_PURPLE + " [诅咒×" + curseCount + "]" +
                TextFormatting.DARK_GRAY + " 自损" + ratioPercent + "%"
        ));

        // 粒子和音效
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    target.posX, target.posY + 1, target.posZ,
                    30, 0.3, 0.5, 0.3, 0.05);
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    target.posX, target.posY + 1.5, target.posZ,
                    20, 0.2, 0.3, 0.2, 0.0);
            ws.playSound(null, target.getPosition(),
                    SoundEvents.ENTITY_LEASHKNOT_PLACE,
                    SoundCategory.HOSTILE, 1.0F, 0.5F);
        }
    }

    /**
     * 检查玩家是否佩戴缢王之索
     */
    public static boolean hasNoose(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack bauble = baubles.getStackInSlot(i);
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

    // ========== 物品信息 ==========

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
        list.add(TextFormatting.DARK_RED + "        ☬ " + TextFormatting.BOLD + "缢王之索" +
                TextFormatting.RESET + TextFormatting.DARK_RED + " ☬");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                "  \"王的恩典从不平等——");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                "   只赐予臣服于他的人。\"");
        list.add("");

        if (player == null || !hasCursedRing(player)) {
            list.add(TextFormatting.DARK_RED + "  ⚠ 唯有七咒之戒的承载者");
            list.add(TextFormatting.DARK_RED + "    方可触碰缢王的绳索");
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");
            return;
        }

        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");

        // 计算当前数值
        int curseCount = getCurseCount(player);
        float currentChance = Math.min(MAX_EXECUTION_CHANCE,
                BASE_EXECUTION_CHANCE + CURSE_BONUS_PER_CURSE * curseCount);
        int chancePercent = (int) (currentChance * 100);
        float playerRatio = calculatePlayerRatio(curseCount);
        int ratioPercent = (int) (playerRatio * 100);

        // 处刑效果
        list.add(TextFormatting.DARK_RED + "  ◈ 处刑");
        list.add(TextFormatting.GRAY + "    攻击时概率触发，将目标吊起");
        list.add(TextFormatting.DARK_GRAY + "    （概率: " + TextFormatting.RED + "15%" +
                TextFormatting.DARK_GRAY + " + " + TextFormatting.DARK_PURPLE + "5%×诅咒" +
                TextFormatting.DARK_GRAY + "，上限75%）");
        list.add(TextFormatting.GRAY + "    目标悬空 " + TextFormatting.GOLD + "10秒" +
                TextFormatting.GRAY + "，完全定身");
        list.add(TextFormatting.GRAY + "    每秒 " + TextFormatting.RED + "5%" +
                TextFormatting.GRAY + " 最大生命值" + TextFormatting.LIGHT_PURPLE + "真伤");
        list.add(TextFormatting.YELLOW + "    对 Boss 有效");
        list.add("");

        // 王恩机制
        list.add(TextFormatting.DARK_PURPLE + "  ◈ 王恩");
        list.add(TextFormatting.LIGHT_PURPLE + "    处刑期间，你也在自缢");
        list.add(TextFormatting.GRAY + "    每秒承受 " + TextFormatting.RED + "5%" +
                TextFormatting.GRAY + " × " + TextFormatting.DARK_PURPLE + "承受比例" +
                TextFormatting.GRAY + " 伤害");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "    诅咒越多，王越偏袒你：");
        list.add(TextFormatting.DARK_GRAY + "    0诅咒 → 100% | 7诅咒 → 63%");
        list.add(TextFormatting.DARK_GRAY + "    30诅咒 → 16% | 60诅咒 → 5%");
        list.add("");

        // 当前状态
        list.add(TextFormatting.DARK_GRAY + "────────────────────────────────────");
        list.add("");
        list.add(TextFormatting.GRAY + "  诅咒烙印: " + TextFormatting.DARK_PURPLE + curseCount);
        list.add(TextFormatting.GRAY + "  触发概率: " + TextFormatting.GOLD + chancePercent + "%");
        list.add(TextFormatting.GRAY + "  自损比例: " + TextFormatting.DARK_RED + ratioPercent + "%");

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "════════════════════════════════════");

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                    "  这根绳索来自Alagadda的宫廷，");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                    "  据说缢王曾用它亲手吊死自己。");
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                    "  \"公正\"只是宫廷的谎言。");
        } else {
            list.add(TextFormatting.DARK_GRAY + "        [ Shift - 残章 ]");
        }
    }
}
