package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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
 * - 攻击时触发"处刑"，概率 = 15% + 5% × 诅咒附魔数量
 * - 目标被提至半空 10 秒，完全定身，每秒受到 5% 最大生命值真伤
 * - 对 Boss 同样有效
 *
 * 代价【同感痛苦】：
 * - 每次触发处刑，玩家失去 1 格氧气
 * - 如果在水下或氧气耗尽，直接扣血
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemNooseOfHangedKing extends Item implements IBauble {

    // 基础处刑触发概率
    private static final float BASE_EXECUTION_CHANCE = 0.15f;
    // 每个诅咒附魔增加的概率
    private static final float CURSE_BONUS_PER_ENCHANT = 0.05f;
    // 最大触发概率
    private static final float MAX_EXECUTION_CHANCE = 0.75f;
    // 处刑持续时间（tick）
    private static final int EXECUTION_DURATION_TICKS = 200; // 10秒
    // 每秒伤害（最大生命值百分比）
    private static final float DAMAGE_PERCENT_PER_SECOND = 0.05f;
    // 氧气消耗量
    private static final int AIR_COST = 30; // 约 1 格氧气
    // 氧气不足时的伤害
    private static final float SUFFOCATION_DAMAGE = 2.0f;

    // 被处刑的实体记录：实体ID -> 处刑数据
    private static final Map<Integer, ExecutionData> EXECUTED_ENTITIES = new ConcurrentHashMap<>();

    private static class ExecutionData {
        final long endTime;
        final double hangX;
        final double hangY;
        final double hangZ;
        final EntityPlayer executioner;
        final int dimensionId;

        ExecutionData(EntityPlayer executioner, double x, double y, double z, float hangHeight, int dimensionId) {
            this.endTime = System.currentTimeMillis() + (EXECUTION_DURATION_TICKS * 50L);
            this.hangX = x;
            this.hangY = y + hangHeight;
            this.hangZ = z;
            this.executioner = executioner;
            this.dimensionId = dimensionId;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > endTime;
        }

        int getRemainingSeconds() {
            long remaining = endTime - System.currentTimeMillis();
            return remaining > 0 ? (int) (remaining / 1000) : 0;
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

        // 计算触发概率：基础 15% + 5% × 诅咒附魔数量
        int curseCount = countCurseEnchantments(player);
        float executionChance = Math.min(MAX_EXECUTION_CHANCE,
                BASE_EXECUTION_CHANCE + CURSE_BONUS_PER_ENCHANT * curseCount);

        // 概率触发
        if (player.world.rand.nextFloat() > executionChance) return;

        // 执行处刑（对所有生物有效，包括 Boss）
        executeTarget(player, target, curseCount);

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

            // 检查是否过期
            if (data.isExpired()) {
                // 处刑结束，让实体落下
                target.fallDistance = 0;
                it.remove();

                // 效果提示
                if (data.executioner != null && !data.executioner.world.isRemote) {
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
                float maxHealth = target.getMaxHealth();
                float damage = maxHealth * DAMAGE_PERCENT_PER_SECOND;
                damage = Math.max(1.0f, damage);

                // 使用 TrueDamageHelper 造成真伤
                TrueDamageHelper.applyWrappedTrueDamage(target, data.executioner, damage,
                        TrueDamageHelper.TrueDamageFlag.EXECUTE);

                // 粒子效果
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
                    // 伤害指示
                    ws.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                            target.posX, target.posY + target.height / 2, target.posZ,
                            3, 0.2, 0.2, 0.2, 0.1);
                }

                // 显示剩余时间
                if (data.executioner != null && !data.executioner.world.isRemote) {
                    int remaining = data.getRemainingSeconds();
                    data.executioner.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "☠ 处刑中... " +
                            TextFormatting.GOLD + remaining + "s" +
                            TextFormatting.GRAY + " | " +
                            TextFormatting.RED + String.format("%.0f", damage) + " 真伤/秒"
                    ), true);
                }
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 计算玩家身上的诅咒附魔数量（包括饰品栏）
     */
    private static int countCurseEnchantments(EntityPlayer player) {
        int count = 0;

        // 检查装备栏（盔甲 + 手持物品）
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            count += countCursesOnItem(stack);
        }

        // 检查饰品栏
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                count += countCursesOnItem(bauble);
            }
        } catch (Exception ignored) {}

        return count;
    }

    /**
     * 计算单个物品上的诅咒附魔数量
     */
    private static int countCursesOnItem(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        int count = 0;
        NBTTagList enchantments = stack.getEnchantmentTagList();

        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound tag = enchantments.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);

            if (ench != null && ench.isCurse()) {
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

        // 记录处刑数据（锁定当前位置，包含维度ID）
        EXECUTED_ENTITIES.put(target.getEntityId(),
            new ExecutionData(player, target.posX, target.posY, target.posZ, hangHeight,
                player.world.provider.getDimension()));

        // 初始向上推力（视觉效果）
        target.motionY = 0.8;
        target.velocityChanged = true;

        // 计算当前触发概率用于显示
        float currentChance = Math.min(MAX_EXECUTION_CHANCE,
                BASE_EXECUTION_CHANCE + CURSE_BONUS_PER_ENCHANT * curseCount);

        // 效果提示
        String targetName = target.hasCustomName() ? target.getCustomNameTag() : target.getName();
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠ 处刑！" +
                TextFormatting.GRAY + " [" + targetName + "] 被吊起 " +
                TextFormatting.GOLD + "10" + TextFormatting.GRAY + " 秒" +
                (curseCount > 0 ? TextFormatting.DARK_PURPLE + " [诅咒×" + curseCount + "]" : "")
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

        // 计算当前概率
        int curseCount = player != null ? countCurseEnchantments(player) : 0;
        float currentChance = Math.min(MAX_EXECUTION_CHANCE,
                BASE_EXECUTION_CHANCE + CURSE_BONUS_PER_ENCHANT * curseCount);
        int chancePercent = (int) (currentChance * 100);

        list.add("");
        list.add(TextFormatting.GOLD + "◆ 窒息之握");
        list.add(TextFormatting.GRAY + "  攻击时触发" + TextFormatting.DARK_RED + "「处刑」");
        list.add(TextFormatting.GRAY + "  概率: " + TextFormatting.RED + "15%" +
                TextFormatting.GRAY + " + " + TextFormatting.DARK_PURPLE + "5%×诅咒数");
        if (player != null && curseCount > 0) {
            list.add(TextFormatting.DARK_PURPLE + "  当前: " + TextFormatting.GOLD + chancePercent + "%" +
                    TextFormatting.GRAY + " (" + curseCount + " 个诅咒)");
        }
        list.add(TextFormatting.GRAY + "  目标被吊至半空 " + TextFormatting.GOLD + "10" +
                TextFormatting.GRAY + " 秒");
        list.add(TextFormatting.GRAY + "  期间" + TextFormatting.AQUA + "完全定身" +
                TextFormatting.GRAY + "，无法移动");
        list.add(TextFormatting.GRAY + "  每秒受到 " + TextFormatting.RED + "5%" +
                TextFormatting.GRAY + " 最大生命值" + TextFormatting.LIGHT_PURPLE + "真伤");
        list.add(TextFormatting.YELLOW + "  对 Boss 同样有效！");

        list.add("");
        list.add(TextFormatting.DARK_RED + "◆ 代价：同感痛苦");
        list.add(TextFormatting.RED + "  每次处刑消耗 1 格氧气");
        list.add(TextFormatting.RED + "  氧气耗尽时直接扣血");

        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "10 秒内造成 50% 最大生命值真伤");
            list.add(TextFormatting.GRAY + "配合其他伤害可轻松击杀高血量目标");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "诅咒加成:");
            list.add(TextFormatting.GRAY + "  绑定诅咒、消失诅咒等都算");
            list.add(TextFormatting.GRAY + "  最高叠加至 " + TextFormatting.GOLD + "75%" + TextFormatting.GRAY + " 概率");
        } else {
            list.add("");
            list.add(TextFormatting.DARK_GRAY + "按住 Shift 查看更多");
        }
    }
}
