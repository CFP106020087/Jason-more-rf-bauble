package com.moremod.fabric.sanity;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import com.moremod.fabric.handler.FabricEventHandler; // 新增导入
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 完整的理智藥水系統 - 修復版
 * 包含物品定義、註冊和百分比傷害整合
 * 修復了與FabricEventHandler的數據同步問題
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class CompleteSanitySystem {

    // ========== 物品實例 ==========
    public static Item SANITY_ELIXIR_BASIC;
    public static Item SANITY_ELIXIR_REFINED;
    public static Item SANITY_ELIXIR_PURE;
    public static Item SANITY_ELIXIR_EMERGENCY;

    // ========== 自定義傷害源 ==========
    public static final DamageSource ABYSSAL_GAZE = new DamageSource("abyssal_gaze")
            .setDamageBypassesArmor()
            .setDamageIsAbsolute()
            .setMagicDamage();

    /**
     * 物品註冊事件
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        // 創建並註冊藥水
        SANITY_ELIXIR_BASIC = new SanityElixir(ElixirTier.BASIC);
        SANITY_ELIXIR_REFINED = new SanityElixir(ElixirTier.REFINED);
        SANITY_ELIXIR_PURE = new SanityElixir(ElixirTier.PURE);
        SANITY_ELIXIR_EMERGENCY = new SanityElixir(ElixirTier.EMERGENCY);

        registry.register(SANITY_ELIXIR_BASIC);
        registry.register(SANITY_ELIXIR_REFINED);
        registry.register(SANITY_ELIXIR_PURE);
        registry.register(SANITY_ELIXIR_EMERGENCY);
    }

    /**
     * 註冊配方 - 在主類的init階段調用
     */
    public static void registerRecipes() {
        // 基礎理智藥水
        GameRegistry.addShapelessRecipe(
                new ResourceLocation("moremod", "sanity_elixir_basic"),
                null,
                new ItemStack(SANITY_ELIXIR_BASIC, 3),
                Ingredient.fromItem(Items.MILK_BUCKET),
                Ingredient.fromItem(Items.ENDER_PEARL),
                Ingredient.fromItem(Items.CHORUS_FRUIT),
                Ingredient.fromItem(Items.GLASS_BOTTLE),
                Ingredient.fromItem(Items.GLASS_BOTTLE),
                Ingredient.fromItem(Items.GLASS_BOTTLE)
        );

        // 精煉理智藥水
        GameRegistry.addShapelessRecipe(
                new ResourceLocation("moremod", "sanity_elixir_refined"),
                null,
                new ItemStack(SANITY_ELIXIR_REFINED, 1),
                Ingredient.fromItem(SANITY_ELIXIR_BASIC),
                Ingredient.fromItem(Items.GOLDEN_APPLE),
                Ingredient.fromItem(Items.PRISMARINE_SHARD),
                Ingredient.fromItem(Items.GHAST_TEAR)
        );

        // 純淨理智藥水
        GameRegistry.addShapedRecipe(
                new ResourceLocation("moremod", "sanity_elixir_pure"),
                null,
                new ItemStack(SANITY_ELIXIR_PURE, 1),
                "DND",
                "SRS",
                "DND",
                'R', new ItemStack(SANITY_ELIXIR_REFINED),
                'N', new ItemStack(Items.NETHER_STAR),
                'D', new ItemStack(Items.DRAGON_BREATH),
                'S', new ItemStack(Items.SHULKER_SHELL)
        );

        // 緊急理智藥劑
        GameRegistry.addShapelessRecipe(
                new ResourceLocation("moremod", "sanity_elixir_emergency"),
                null,
                new ItemStack(SANITY_ELIXIR_EMERGENCY, 3),
                Ingredient.fromItem(Items.POTIONITEM), // 治療藥水II
                Ingredient.fromItem(Items.GUNPOWDER),
                Ingredient.fromItem(Items.GLOWSTONE_DUST),
                Ingredient.fromItem(Items.REDSTONE)
        );
    }

    /**
     * 藥水等級枚舉
     */
    public enum ElixirTier {
        BASIC("basic", 40, 15, "§7理智靈藥", 0x7B68EE, 600),
        REFINED("refined", 60, 30, "§b精煉理智靈藥", 0x4169E1, 1200),
        PURE("pure", 100, 50, "§d純淨理智靈藥", 0x9370DB, 2400),
        EMERGENCY("emergency", 30, 10, "§c緊急理智藥劑", 0xFF6B6B, 0);

        public final String name;
        public final float sanityRestore;
        public final float insightReduce;
        public final String displayName;
        public final int color;
        public final int protectionDuration;

        ElixirTier(String name, float sanity, float insight, String display, int color, int protection) {
            this.name = name;
            this.sanityRestore = sanity;
            this.insightReduce = insight;
            this.displayName = display;
            this.color = color;
            this.protectionDuration = protection;
        }
    }

    /**
     * 理智藥水物品類
     */
    public static class SanityElixir extends Item {

        private final ElixirTier elixirTier;
        private static final int USE_DURATION = 16;

        public SanityElixir(ElixirTier tier) {
            this.elixirTier = tier;
            this.setMaxStackSize(tier == ElixirTier.EMERGENCY ? 64 : 16);
            this.setCreativeTab(CreativeTabs.BREWING);
            this.setTranslationKey("sanity_elixir_" + tier.name);
            this.setRegistryName("sanity_elixir_" + tier.name);
            this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        }

        @Override
        public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
            EntityPlayer player = entityLiving instanceof EntityPlayer ? (EntityPlayer)entityLiving : null;
            if (player == null) return stack;

            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP)player, stack);
            }

            player.addStat(StatList.getObjectUseStats(this));

            if (!worldIn.isRemote) {
                updateSanityAndInsight(player);
                applyProtectionEffects(player);
                applySpecialEffects(player, worldIn);
            }

            // 返回空瓶
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);

                if (elixirTier != ElixirTier.EMERGENCY) {
                    if (stack.isEmpty()) {
                        return new ItemStack(Items.GLASS_BOTTLE);
                    }

                    ItemStack bottleStack = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.inventory.addItemStackToInventory(bottleStack)) {
                        player.dropItem(bottleStack, false);
                    }
                }
            }

            return stack;
        }

        /**
         * 修復版：同時更新裝備NBT和PlayerFabricData
         */
        private void updateSanityAndInsight(EntityPlayer player) {
            // 获取FabricEventHandler中的PlayerFabricData
            FabricEventHandler.PlayerFabricData playerData =
                    FabricEventHandler.getPlayerData(player);

            int otherworldCount = 0;
            float totalSanityRestore = elixirTier.sanityRestore;
            float totalInsightReduce = elixirTier.insightReduce;
            boolean criticalBonus = false;
            boolean hasUpdated = false;

            // 用于存储最终显示的值
            int finalSanity = 0;
            int finalInsight = 0;
            float actualSanityRestore = totalSanityRestore;
            float actualInsightReduce = totalInsightReduce;

            // 遍历所有护甲槽位
            for (ItemStack armor : player.getArmorInventoryList()) {
                if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                    otherworldCount++;
                    NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(armor);

                    int currentSanity = fabricData.getInteger("Sanity");
                    int currentInsight = fabricData.getInteger("Insight");

                    // 危急奖励判定
                    if (currentSanity < 20 || currentInsight > 80) {
                        criticalBonus = true;
                    }

                    // 计算实际恢复量
                    actualSanityRestore = criticalBonus ? totalSanityRestore * 1.5f : totalSanityRestore;
                    actualInsightReduce = criticalBonus ? totalInsightReduce * 1.5f : totalInsightReduce;

                    int newSanity = Math.min(100, currentSanity + (int)actualSanityRestore);
                    int newInsight = Math.max(0, currentInsight - (int)actualInsightReduce);

                    // 更新装备NBT
                    fabricData.setInteger("Sanity", newSanity);
                    fabricData.setInteger("Insight", newInsight);

                    // 关键修改：同时更新PlayerFabricData
                    playerData.sanity = newSanity;
                    playerData.insight = newInsight;
                    playerData.otherworldCount = otherworldCount;
                    hasUpdated = true;

                    // 保存最终值用于消息显示
                    finalSanity = newSanity;
                    finalInsight = newInsight;

                    // 保护效果
                    if (elixirTier.protectionDuration > 0) {
                        long protectionEndTime = System.currentTimeMillis() + (elixirTier.protectionDuration * 50);
                        fabricData.setLong("ElixirProtection", protectionEndTime);
                        fabricData.setString("ProtectionLevel", elixirTier.name);
                    }

                    // 纯净药水清除负面效果
                    if (elixirTier == ElixirTier.PURE) {
                        fabricData.setInteger("AbyssGazeStacks", 0);
                        fabricData.setInteger("ForbiddenKnowledge",
                                Math.max(0, fabricData.getInteger("ForbiddenKnowledge") - 1));

                        // 同步到PlayerFabricData
                        playerData.abyssGazeStacks = 0;
                        playerData.forbiddenKnowledge = Math.max(0, playerData.forbiddenKnowledge - 1);
                    }

                    // 保存更新后的NBT到装备
                    NBTTagCompound armorTag = armor.getTagCompound();
                    if (armorTag != null) {
                        armorTag.setTag("WovenFabric", fabricData);
                    }
                }
            }

            // 在循环外发送一次消息
            if (hasUpdated) {
                // 发送饮用消息
                player.sendStatusMessage(new TextComponentString(
                        String.format("%s§r 飲用！§a+%d理智 §5-%d靈視 %s",
                                elixirTier.displayName,
                                (int)actualSanityRestore,
                                (int)actualInsightReduce,
                                criticalBonus ? "§e§l危急獎勵！" : "")), false);

                // 发送当前状态
                player.sendStatusMessage(new TextComponentString(
                        String.format("§7當前：理智%d/100 靈視%d/100", finalSanity, finalInsight)), true);

                // 如果有多件异界织印装备，提示更新了所有装备
                if (otherworldCount > 1) {
                    player.sendStatusMessage(new TextComponentString(
                            String.format("§7已更新所有 %d 件異界織印裝備", otherworldCount)), true);
                }

                // 立即同步更新后的数据，确保不会被覆盖
                FabricEventHandler.syncAllFabricDataToArmor(player, playerData);
            } else {
                // 如果没有找到任何异界织印装备（理论上不会发生，因为之前已经检查过）
                player.sendStatusMessage(new TextComponentString("§c未找到異界織印裝備"), true);
            }
        }

        /**
         * 修復版：更新PlayerFabricData中的保護狀態
         */
        private void applyProtectionEffects(EntityPlayer player) {
            // 獲取PlayerFabricData以更新保護狀態
            FabricEventHandler.PlayerFabricData playerData =
                    FabricEventHandler.getPlayerData(player);

            switch(elixirTier) {
                case BASIC:
                    player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, elixirTier.protectionDuration, 0));
                    player.sendStatusMessage(new TextComponentString("§7獲得30秒傷害減免"), true);
                    break;

                case REFINED:
                    player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, elixirTier.protectionDuration, 1));
                    player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 400, 0));
                    player.sendStatusMessage(new TextComponentString("§b獲得60秒強化保護與生命恢復"), true);
                    break;

                case PURE:
                    player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, elixirTier.protectionDuration, 2));
                    player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 600, 1));
                    player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 1200, 1));

                    NBTTagCompound playerNBT = player.getEntityData();
                    playerNBT.setBoolean("AbyssalImmunity", true);
                    playerNBT.setLong("ImmunityEndTime", System.currentTimeMillis() + elixirTier.protectionDuration * 50);

                    player.sendStatusMessage(new TextComponentString("§d§l獲得120秒完全心靈守護！"), false);
                    player.sendStatusMessage(new TextComponentString("§d免疫虛空侵蝕、理智崩潰和深淵凝視"), true);
                    break;

                case EMERGENCY:
                    player.heal(player.getMaxHealth() * 0.5f);
                    player.addPotionEffect(new PotionEffect(MobEffects.INSTANT_HEALTH, 1, 2));
                    player.hurtResistantTime = 60;
                    player.sendStatusMessage(new TextComponentString("§c緊急治療！恢復50%生命值"), true);
                    break;
            }

            // 立即同步保護狀態
            FabricEventHandler.syncAllFabricDataToArmor(player, playerData);
        }

        private void applySpecialEffects(EntityPlayer player, World world) {
            if (!(world instanceof WorldServer)) return;

            WorldServer ws = (WorldServer) world;

            switch(elixirTier) {
                case PURE:
                    for (int i = 0; i < 72; i++) {
                        double angle = (Math.PI * 2 * i) / 72;
                        double radius = 3;
                        double x = player.posX + Math.cos(angle) * radius;
                        double z = player.posZ + Math.sin(angle) * radius;

                        ws.spawnParticle(EnumParticleTypes.END_ROD,
                                x, player.posY + 1, z,
                                1, 0, 0.5, 0, 0.02);
                    }
                    break;

                case REFINED:
                    for (int i = 0; i < 40; i++) {
                        double angle = (Math.PI * 2 * i) / 40;
                        double y = i * 0.05;
                        double radius = 1 * (1 - i / 40.0);

                        double x = player.posX + Math.cos(angle) * radius;
                        double z = player.posZ + Math.sin(angle) * radius;

                        ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                                x, player.posY + y, z, 1, 0, 0.1, 0, 0.02);
                    }
                    break;

                case BASIC:
                    for (int i = 0; i < 20; i++) {
                        double angle = (Math.PI * 2 * i) / 20;
                        double y = i * 0.05;

                        ws.spawnParticle(EnumParticleTypes.SPELL,
                                player.posX + Math.cos(angle) * 0.5,
                                player.posY + y,
                                player.posZ + Math.sin(angle) * 0.5,
                                1, 0, 0.1, 0, 0.02);
                    }
                    break;

                case EMERGENCY:
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                            player.posX, player.posY + 1, player.posZ,
                            1, 0, 0, 0.0, 0);
                    break;
            }

            SoundEvent sound = elixirTier == ElixirTier.PURE ? SoundEvents.ENTITY_PLAYER_LEVELUP :
                    elixirTier == ElixirTier.EMERGENCY ? SoundEvents.ENTITY_GENERIC_EXPLODE :
                            SoundEvents.ENTITY_GENERIC_DRINK;

            world.playSound(null, player.getPosition(), sound, SoundCategory.PLAYERS,
                    0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        public int getMaxItemUseDuration(ItemStack stack) {
            return elixirTier == ElixirTier.EMERGENCY ? 8 : USE_DURATION;
        }

        @Override
        public EnumAction getItemUseAction(ItemStack stack) {
            return EnumAction.DRINK;
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
            ItemStack itemstack = playerIn.getHeldItem(handIn);

            int otherworldCount = FabricWeavingSystem.countPlayerFabric(playerIn,
                    UpdatedFabricPlayerData.FabricType.OTHERWORLD);
            if (otherworldCount == 0) {
                playerIn.sendStatusMessage(new TextComponentString("§c需要裝備異界織印才能飲用理智藥水"), true);
                return new ActionResult<>(EnumActionResult.FAIL, itemstack);
            }

            playerIn.setActiveHand(handIn);
            return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public boolean hasEffect(ItemStack stack) {
            return elixirTier == ElixirTier.PURE || elixirTier == ElixirTier.EMERGENCY;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            tooltip.add(elixirTier.displayName);
            tooltip.add("");

            tooltip.add(String.format("§a恢復 %d 理智值", (int)elixirTier.sanityRestore));
            tooltip.add(String.format("§5降低 %d 靈視值", (int)elixirTier.insightReduce));

            switch(elixirTier) {
                case BASIC:
                    tooltip.add("§7提供30秒傷害減免(25%)");
                    break;
                case REFINED:
                    tooltip.add("§b提供60秒中等保護(50%)");
                    tooltip.add("§b附帶生命恢復");
                    break;
                case PURE:
                    tooltip.add("§d提供120秒完全心靈守護");
                    tooltip.add("§d§l免疫所有百分比傷害");
                    tooltip.add("§d清除深淵凝視");
                    break;
                case EMERGENCY:
                    tooltip.add("§c立即恢復50%最大生命");
                    tooltip.add("§c緊急情況使用");
                    break;
            }

            tooltip.add("");
            tooltip.add("§e§l無飲用冷卻");
            tooltip.add("§c需要裝備異界織印才能飲用");
            tooltip.add("§6危急狀態時效果+50%");
        }
    }

    // ========== 百分比傷害系統整合 ==========

    /**
     * 修改版百分比傷害處理 - 檢測藥水保護
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player,
                UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        if (otherworldCount == 0) return;

        // 檢查完全免疫
        if (hasAbyssalImmunity(player)) {
            if (player.ticksExisted % 100 == 0) {
                showImmunityEffect(player);
            }
            return;
        }

        // 獲取保護等級
        String protectionLevel = getProtectionLevel(player);
        float damageReduction = getDamageReduction(protectionLevel);

        NBTTagCompound data = getOtherworldData(player);
        int insight = data.getInteger("Insight");
        int sanity = data.getInteger("Sanity");

        if (player.ticksExisted % 100 == 0) {
            // 靈視侵蝕
            if (insight > 70) {
                float damage = calculateInsightErosion(player, insight, sanity);
                damage *= (1 - damageReduction);

                if (damage > 0.01f) {
                    applyPercentageDamage(player, damage, "§5靈視侵蝕", protectionLevel);
                }
            }

            // 理智崩潰
            if (sanity < 20) {
                float damage = calculateSanityCollapse(player, sanity, insight);
                damage *= (1 - damageReduction);

                if (damage > 0.01f) {
                    applyPercentageDamage(player, damage, "§4理智崩潰", protectionLevel);
                }
            }

            // 深淵凝視
            if (insight > 90 && sanity < 10) {
                float damage = calculateAbyssalGaze(player);
                if (damage > 0) {
                    if (protectionLevel.equals("pure")) {
                        damage = 0;
                        player.sendStatusMessage(new TextComponentString(
                                "§d§l心靈守護抵禦了深淵凝視！"), true);
                    } else {
                        damage *= (1 - Math.min(damageReduction, 0.5f));
                    }

                    if (damage > 0.01f) {
                        applyPercentageDamage(player, damage, "§0§l深淵凝視", protectionLevel);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player,
                UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        if (otherworldCount == 0) return;

        if (hasAbyssalImmunity(player)) return;

        String protectionLevel = getProtectionLevel(player);
        float damageReduction = getDamageReduction(protectionLevel);

        NBTTagCompound data = getOtherworldData(player);
        int insight = data.getInteger("Insight");
        int sanity = data.getInteger("Sanity");

        if (insight > 60) {
            float chance = calculateVoidBacklashChance(insight, sanity);
            chance *= (1 - damageReduction * 0.5f);

            if (player.world.rand.nextFloat() < chance) {
                float damage = calculateVoidBacklashDamage(player, insight, sanity);
                damage *= (1 - damageReduction);

                // 創建 final 變數供 lambda 使用
                final float finalDamage = damage;
                final String finalProtectionLevel = protectionLevel;

                player.world.getMinecraftServer().addScheduledTask(() -> {
                    if (finalDamage > 0.01f) {
                        applyPercentageDamage(player, finalDamage, "§5虛空反噬", finalProtectionLevel);
                    }
                });
            }
        }
    }

    // ========== 輔助方法 ==========

    private static boolean hasElixirProtection(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(armor);
                long protectionTime = fabricData.getLong("ElixirProtection");
                if (System.currentTimeMillis() < protectionTime) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getProtectionLevel(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(armor);
                if (System.currentTimeMillis() < fabricData.getLong("ElixirProtection")) {
                    return fabricData.getString("ProtectionLevel");
                }
            }
        }
        return "";
    }

    private static boolean hasAbyssalImmunity(EntityPlayer player) {
        NBTTagCompound playerData = player.getEntityData();
        if (playerData.hasKey("AbyssalImmunity")) {
            long immunityEnd = playerData.getLong("ImmunityEndTime");
            if (System.currentTimeMillis() < immunityEnd) {
                return true;
            } else {
                playerData.removeTag("AbyssalImmunity");
                playerData.removeTag("ImmunityEndTime");
            }
        }
        return false;
    }

    private static float getDamageReduction(String protectionLevel) {
        switch(protectionLevel) {
            case "basic": return 0.25f;
            case "refined": return 0.50f;
            case "pure": return 1.00f;
            default: return 0f;
        }
    }

    private static void showImmunityEffect(EntityPlayer player) {
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;

            for (int i = 0; i < 18; i++) {
                double angle = (Math.PI * 2 * i) / 18;
                double x = player.posX + Math.cos(angle) * 1.5;
                double z = player.posZ + Math.sin(angle) * 1.5;

                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        x, player.posY + 1, z,
                        1, 0, 0.2, 0, 0.01);
            }
        }

        NBTTagCompound playerData = player.getEntityData();
        long immunityEnd = playerData.getLong("ImmunityEndTime");
        int secondsRemaining = (int)((immunityEnd - System.currentTimeMillis()) / 1000);

        if (secondsRemaining > 0 && secondsRemaining % 10 == 0) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§d§l心靈守護剩餘：%d秒", secondsRemaining)), true);
        }
    }

    private static float calculateInsightErosion(EntityPlayer player, int insight, int sanity) {
        float basePercent = ((insight - 70) / 10.0f) * 0.02f;
        float sanityMultiplier = 1.0f;
        if (sanity < 50) {
            sanityMultiplier = 1.0f + (50 - sanity) * 0.01f;
        }
        int count = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        float resistance = 1.0f - (count * 0.05f);
        return basePercent * sanityMultiplier * resistance;
    }

    private static float calculateSanityCollapse(EntityPlayer player, int sanity, int insight) {
        if (sanity >= 20) return 0;
        float basePercent = ((20 - sanity) / 5.0f) * 0.05f;
        if (insight > 50) {
            basePercent *= 1.5f;
        }
        if (player.world.rand.nextFloat() > 0.3f) {
            return 0;
        }
        return basePercent;
    }

    private static float calculateAbyssalGaze(EntityPlayer player) {
        if (player.world.rand.nextFloat() > 0.1f) {
            return 0;
        }
        return 0.15f + player.world.rand.nextFloat() * 0.1f;
    }

    private static float calculateVoidBacklashChance(int insight, int sanity) {
        float chance = 0.05f;
        if (insight > 70) {
            chance += (insight - 70) * 0.005f;
        }
        if (sanity < 30) {
            chance += (30 - sanity) * 0.005f;
        }
        return Math.min(chance, 0.3f);
    }

    private static float calculateVoidBacklashDamage(EntityPlayer player, int insight, int sanity) {
        float basePercent = 0.05f + (insight / 100.0f) * 0.1f;
        if (sanity < 20) {
            basePercent *= 1.5f;
        }
        return basePercent;
    }

    private static void applyPercentageDamage(EntityPlayer player, float percentage, String source, String protection) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float damage;

        if (source.contains("侵蝕") || source.contains("深淵")) {
            damage = maxHealth * percentage;
        } else {
            damage = currentHealth * percentage;
        }

        damage = Math.max(damage, 1.0f);

        if (damage >= currentHealth) {
            damage = currentHealth - 1.0f;
            if (damage <= 0) return;
        }

        player.attackEntityFrom(ABYSSAL_GAZE, damage);

        String damageText = String.format("%.1f", damage);
        String percentText = String.format("%.1f%%", percentage * 100);
        String protectionText = "";

        if (!protection.isEmpty()) {
            float reduction = getDamageReduction(protection);
            if (reduction > 0) {
                protectionText = String.format(" §a(已減免%.0f%%)", reduction * 100);
            }
        }

        player.sendStatusMessage(new TextComponentString(
                source + " " + TextFormatting.RED + "-" + damageText +
                        TextFormatting.GRAY + " (" + percentText + ")" + protectionText), true);
    }

    private static NBTTagCompound getOtherworldData(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                return FabricWeavingSystem.getFabricData(armor);
            }
        }
        NBTTagCompound defaultData = new NBTTagCompound();
        defaultData.setInteger("Insight", 0);
        defaultData.setInteger("Sanity", 100);
        return defaultData;
    }
}