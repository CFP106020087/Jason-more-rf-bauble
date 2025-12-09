package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemCurseSpread extends Item implements IBauble {

    // 基础范围：8格
    public static final double BASE_RANGE = 8.0;
    // 每个额外诅咒增加范围：0.2格
    public static final double RANGE_PER_CURSE = 0.2;

    // NBT标签名
    public static final String NBT_CURSE_SPREAD = "CurseSpreadLevel";
    public static final String NBT_CURSE_EXPIRE = "CurseSpreadExpire";

    // 护甲削弱使用的 AttributeModifier UUID
    private static final UUID CURSE_ARMOR_DEBUFF_ID = UUID.fromString("b3b7cda0-2ed3-4ad3-8d20-2b10b914f9e1");
    private static final String CURSE_ARMOR_DEBUFF_NAME = "CurseSpreadArmorDebuff";
    
    // 玩家生命值削弱使用的 AttributeModifier UUID
    private static final UUID CURSE_HEALTH_DEBUFF_ID = UUID.fromString("c4c8deb1-3fe4-5be4-9e31-3c21c925e0f2");
    private static final String CURSE_HEALTH_DEBUFF_NAME = "CurseSpreadHealthDebuff";
    // 生命值削弱百分比：25%
    private static final double HEALTH_REDUCTION_PERCENT = 0.25;

    public ItemCurseSpread() {
        this.setMaxStackSize(1);
        this.setTranslationKey("curse_spread");
        this.setRegistryName("curse_spread");
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.HEAD;
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
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer))
            return;

        EntityPlayer p = (EntityPlayer) player;
        applyHealthReduction(p);
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer))
            return;

        EntityPlayer p = (EntityPlayer) player;
        clearHealthReduction(p);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase livingPlayer) {
        // 調試：每5秒輸出一次 onWornTick 被調用（在服務端）
        if (!livingPlayer.world.isRemote && livingPlayer instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) livingPlayer;
            if (p.ticksExisted % 100 == 0) {
                int curseAmt = getCurseAmount(p);
                System.out.println(String.format("[CurseSpread] onWornTick 被調用 - 玩家: %s, 詛咒數: %d, 創造模式: %s",
                        p.getName(), curseAmt, p.isCreative()));
            }
        }

        if (livingPlayer.world.isRemote || !(livingPlayer instanceof EntityPlayer))
            return;

        EntityPlayer player = (EntityPlayer) livingPlayer;

        if (player.isCreative() || player.isSpectator())
            return;

        // 每10 ticks（0.5秒）触发一次
        if (player.ticksExisted % 10 != 0)
            return;

        int curseAmount = getCurseAmount(player);

        // 調試：輸出詛咒數量（每2秒一次）
        if (player.ticksExisted % 40 == 0) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    String.format("§8[蔓延詛咒調試] 詛咒數量: %d (需要>=7)", curseAmount)
            ));
        }

        // 至少需要7个诅咒（来自七咒之戒）
        if (curseAmount < 7)
            return;

        // 计算影响范围
        double range = calculateRange(curseAmount);

        markCursedEntities(player, range, curseAmount);
    }

    /**
     * 计算影响范围 - 线性增长
     * 公式：8 + 额外诅咒数 × 0.2
     */
    public static double calculateRange(int curseLevel) {
        if (curseLevel < 7)
            return BASE_RANGE;

        int extraCurses = curseLevel - 7;
        return BASE_RANGE + extraCurses * RANGE_PER_CURSE;
    }

    /**
     * 计算伤害削弱倍率（实体造成的伤害）
     *
     * 分段线性：
     * 7诅咒：80%伤害
     * 50诅咒：50%伤害
     * 107诅咒：30%伤害
     */
    public static double getDamageReductionMultiplier(int curseLevel) {
        if (curseLevel < 7)
            return 1.0;

        if (curseLevel <= 50) {
            // 7-50诅咒：80% -> 50%
            double progress = (curseLevel - 7) / 43.0;
            return 0.8 - progress * 0.3;
        } else {
            // 50-107诅咒：50% -> 30%
            double progress = Math.min((curseLevel - 50) / 57.0, 1.0);
            return 0.5 - progress * 0.2;
        }
    }

    /**
     * 计算护甲削弱百分比
     *
     * 分段线性：
     * 7诅咒：削弱10%
     * 50诅咒：削弱30%
     * 107诅咒：削弱60%
     */
    public static double getArmorReductionPercent(int curseLevel) {
        if (curseLevel < 7)
            return 0.0;

        if (curseLevel <= 50) {
            // 7-50诅咒：10% -> 30%
            double progress = (curseLevel - 7) / 43.0;
            return 0.1 + progress * 0.2;
        } else {
            // 50-107诅咒：30% -> 60%
            double progress = Math.min((curseLevel - 50) / 57.0, 1.0);
            return 0.3 + progress * 0.3;
        }
    }

    /**
     * 计算伤害增幅倍率（实体受到的伤害）
     *
     * 分段线性：
     * 7诅咒：150%（1.5倍）
     * 50诅咒：200%（2倍）
     * 107诅咒：300%（3倍）
     */
    public static double getDamageAmplificationMultiplier(int curseLevel) {
        if (curseLevel < 7)
            return 1.0;

        if (curseLevel <= 50) {
            // 7-50诅咒：1.5倍 -> 2倍
            double progress = (curseLevel - 7) / 43.0;
            return 1.5 + progress * 0.5;
        } else {
            // 50-107诅咒：2倍 -> 3倍
            double progress = Math.min((curseLevel - 50) / 57.0, 1.0);
            return 2.0 + progress * 1.0;
        }
    }

    /**
     * 应用玩家生命值削弱
     */
    private static void applyHealthReduction(EntityPlayer player) {
        IAttributeInstance healthAttribute = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttribute == null)
            return;

        // 先移除旧的 Debuff（如果有）
        AttributeModifier old = healthAttribute.getModifier(CURSE_HEALTH_DEBUFF_ID);
        if (old != null) {
            healthAttribute.removeModifier(old);
        }

        // 使用 MULTIPLY_TOTAL 操作：最终生命值 = base * (1 + amount)
        // 削弱 25%，则 amount = -0.25
        AttributeModifier debuff = new AttributeModifier(
                CURSE_HEALTH_DEBUFF_ID,
                CURSE_HEALTH_DEBUFF_NAME,
                -HEALTH_REDUCTION_PERCENT,
                2 // 2 = MULTIPLY_TOTAL
        );
        healthAttribute.applyModifier(debuff);

        // 确保玩家当前生命值不超过新的最大值
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 清除玩家生命值削弱
     */
    private static void clearHealthReduction(EntityPlayer player) {
        IAttributeInstance healthAttribute = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttribute == null)
            return;

        AttributeModifier old = healthAttribute.getModifier(CURSE_HEALTH_DEBUFF_ID);
        if (old != null) {
            healthAttribute.removeModifier(old);
        }
    }

    /**
     * 使用 AttributeModifier 应用护甲削弱到实体
     */
    public static void applyArmorReduction(EntityLivingBase entity, int curseLevel) {
        IAttributeInstance armorAttribute = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttribute == null)
            return;

        // 先移除旧的 Debuff（如果有）
        AttributeModifier old = armorAttribute.getModifier(CURSE_ARMOR_DEBUFF_ID);
        if (old != null) {
            armorAttribute.removeModifier(old);
        }

        double reductionPercent = getArmorReductionPercent(curseLevel);
        if (reductionPercent <= 0.0) {
            return;
        }

        // 使用 MULTIPLY_TOTAL 操作：最终护甲 = base * (1 + amount)
        // 我们要削弱 X%，则 amount = -X
        AttributeModifier debuff = new AttributeModifier(
                CURSE_ARMOR_DEBUFF_ID,
                CURSE_ARMOR_DEBUFF_NAME,
                -reductionPercent,
                2 // 2 = MULTIPLY_TOTAL
        );
        armorAttribute.applyModifier(debuff);
    }

    /**
     * 清除护甲削弱（在诅咒过期或不再受影响时调用）
     */
    public static void clearArmorReduction(EntityLivingBase entity) {
        IAttributeInstance armorAttribute = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttribute == null)
            return;

        AttributeModifier old = armorAttribute.getModifier(CURSE_ARMOR_DEBUFF_ID);
        if (old != null) {
            armorAttribute.removeModifier(old);
        }
    }

    /**
     * 标记周围实体为被诅咒状态
     */
    private void markCursedEntities(EntityPlayer player, double range, int curseAmount) {
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                getBoundingBoxAroundEntity(player, range)
        );

        long currentTime = player.world.getTotalWorldTime();

        // 調試：每5秒輸出一次範圍內實體數量
        if (player.ticksExisted % 100 == 0 && !entities.isEmpty()) {
            int count = 0;
            for (EntityLivingBase e : entities) {
                if (!(e instanceof EntityPlayer) && !e.isOnSameTeam(player)) {
                    count++;
                }
            }
            if (count > 0) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        String.format("§5[蔓延詛咒] 範圍 %.1f 格內有 %d 個敵人被詛咒 (等級 %d)", range, count, curseAmount)
                ));
            }
        }

        for (EntityLivingBase entity : entities) {
            // 排除玩家
            if (entity instanceof EntityPlayer)
                continue;

            // 排除队友
            if (entity.isOnSameTeam(player))
                continue;

            // 标记实体
            NBTTagCompound entityData = entity.getEntityData();
            int oldLevel = entityData.getInteger(NBT_CURSE_SPREAD);

            entityData.setInteger(NBT_CURSE_SPREAD, curseAmount);
            // 设置过期时间：2秒后过期（40 ticks）
            entityData.setLong(NBT_CURSE_EXPIRE, currentTime + 40);

            // 应用护甲削弱（仅在服务端）
            if (!entity.world.isRemote) {
                if (oldLevel != curseAmount) {
                    applyArmorReduction(entity, curseAmount);

                    // 調試輸出
                    double armorReduction = getArmorReductionPercent(curseAmount);
                    System.out.println(String.format("[CurseSpread] 對 %s 應用護甲削弱: %.0f%%",
                            entity.getName(), armorReduction * 100));
                }
            }
        }
    }

    /**
     * 检查实体是否被诅咒蔓延影响
     */
    public static boolean isCursedBySpread(EntityLivingBase entity) {
        if (entity == null || entity.world == null)
            return false;

        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(NBT_CURSE_SPREAD))
            return false;

        long expireTime = data.getLong(NBT_CURSE_EXPIRE);
        long currentTime = entity.world.getTotalWorldTime();

        // 检查是否过期
        if (currentTime > expireTime) {
            data.removeTag(NBT_CURSE_SPREAD);
            data.removeTag(NBT_CURSE_EXPIRE);
            // 恢复护甲
            if (!entity.world.isRemote) {
                clearArmorReduction(entity);
            }
            return false;
        }

        return true;
    }

    /**
     * 获取实体的诅咒等级
     * ✅ 修复：独立检查诅咒等级，避免重复调用 isCursedBySpread 导致竞态条件
     */
    public static int getCurseSpreadLevel(EntityLivingBase entity) {
        if (entity == null || entity.world == null)
            return 0;

        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(NBT_CURSE_SPREAD))
            return 0;

        // 检查是否过期
        long expireTime = data.getLong(NBT_CURSE_EXPIRE);
        long currentTime = entity.world.getTotalWorldTime();

        if (currentTime > expireTime) {
            // 过期了，清理数据（但不在这里清理，让定期更新处理）
            return 0;
        }

        return data.getInteger(NBT_CURSE_SPREAD);
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        // 遍历所有饰品槽
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
     * 获取玩家的诅咒数量
     */
    private static int getCurseAmount(EntityPlayer player) {
        int count = 0;
        boolean ringCounted = false;

        // 检查装备和饰品上的诅咒附魔
        for (ItemStack stack : getFullEquipment(player)) {
            if (stack.isEmpty()) continue;

            // 七咒之戒本身算7个诅咒（只算一次）
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
    private static java.util.List<ItemStack> getFullEquipment(EntityPlayer player) {
        java.util.List<ItemStack> equipmentStacks = new java.util.ArrayList<>();

        equipmentStacks.add(player.getHeldItemMainhand());
        equipmentStacks.add(player.getHeldItemOffhand());
        equipmentStacks.addAll(player.inventory.armorInventory);

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            equipmentStacks.add(BaublesApi.getBaubles(player).getStackInSlot(i));
        }

        return equipmentStacks;
    }

    /**
     * 创建实体周围的AABB
     */
    private static AxisAlignedBB getBoundingBoxAroundEntity(EntityPlayer entity, double radius) {
        return new AxisAlignedBB(
                entity.posX - radius, entity.posY - radius, entity.posZ - radius,
                entity.posX + radius, entity.posY + radius, entity.posZ + radius
        );
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer clientPlayer = net.minecraft.client.Minecraft.getMinecraft().player;

        // 如果玩家没有佩戴七咒之戒，显示装备条件
        if (clientPlayer == null || !hasCursedRing(clientPlayer)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "需要佩戴七咒之戒才能装备");
            return;
        }

        // 获取当前诅咒数量
        int curseAmount = getCurseAmount(clientPlayer);

        if (curseAmount < 7) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "需要至少7个诅咒才能生效");
            return;
        }

        // 计算当前数值
        double currentRange = calculateRange(curseAmount);
        double damageOut = getDamageReductionMultiplier(curseAmount);
        double armorReduction = getArmorReductionPercent(curseAmount);
        double damageIn = getDamageAmplificationMultiplier(curseAmount);

        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "将你的诅咒传播给周围的敌人");
        list.add("");
        list.add(TextFormatting.GOLD + "当前效果：");
        list.add(TextFormatting.GRAY + "诅咒数量：" + TextFormatting.GOLD + curseAmount);
        list.add(TextFormatting.GRAY + "影响范围：" + TextFormatting.AQUA + String.format("%.1f", currentRange) + "格");
        list.add("");
        list.add(TextFormatting.RED + "敌人攻击力：" + String.format("%.0f%%", damageOut * 100));
        list.add(TextFormatting.YELLOW + "敌人护甲削弱：" + String.format("%.0f%%", armorReduction * 100));
        list.add(TextFormatting.DARK_RED + "敌人受到伤害：" + String.format("%.0f%%", damageIn * 100));
        
        // 显示负面效果
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "━━━━ " + TextFormatting.DARK_RED + "代价" + TextFormatting.DARK_GRAY + " ━━━━");
        list.add(TextFormatting.RED + "▪ 你的最大生命值 -25%");
        list.add(TextFormatting.RED + "▪ 被诅咒的敌人有30%概率不掉落任何物品");

        // Shift显示详细信息
        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(TextFormatting.GRAY + "━━━━━━━━━━━━━━━━━━");
            list.add(TextFormatting.GRAY + "诅咒越多，效果越强");
            list.add(TextFormatting.GRAY + "50个诅咒时达到七咒标准");
            list.add(TextFormatting.GRAY + "107个诅咒时达到极限");
        } else {
            list.add("");
            list.add(TextFormatting.GRAY + "按住 " + TextFormatting.GREEN + "Shift" + TextFormatting.GRAY + " 查看更多");
        }
    }

    private String formatExample(int curses, String label) {
        double range = calculateRange(curses);
        double damageOut = getDamageReductionMultiplier(curses);
        double armorReduction = getArmorReductionPercent(curses);
        double damageIn = getDamageAmplificationMultiplier(curses);

        return String.format("%s: §b%.0f§7格 | §c%.0f%%§7攻 | §e-%.0f%%§7甲 | §4%.0f%%§7伤",
                label, range, damageOut * 100, armorReduction * 100, damageIn * 100);
    }
}