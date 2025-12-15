package com.moremod.mixin.villager;

import com.moremod.item.MerchantPersuader;
import com.moremod.util.TradeDiscountHelper;
import com.moremod.system.humanity.HumanitySpectrumSystem;
import com.moremod.system.humanity.HumanityEffectsManager;
import com.moremod.sponsor.item.ZhuxianSword;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityVillager.class)
public abstract class MixinEntityVillager {

    @Unique
    private boolean hasPersuaderDiscount = false;

    @Unique
    private double persuaderDiscountRate = 0.0;

    @Unique
    private EntityPlayer lastPersuaderPlayer = null;

    // ========== 人性值系统价格调整 ==========
    @Unique
    private boolean hasHumanityPriceModifier = false;

    @Unique
    private double humanityPriceMultiplier = 1.0;

    @Unique
    private EntityPlayer lastInteractingPlayer = null;

    // ========== 诛仙剑-为生民立命折扣 ==========
    @Unique
    private boolean hasZhuxianDiscount = false;
    @Unique
    private static final double ZHUXIAN_DISCOUNT_RATE = 0.3; // 30%折扣

    // 🔒 防止人性值折扣重复叠加 - NBT标签
    @Unique
    private static final String NBT_HUMANITY_DISCOUNT_PLAYER = "MoreMod_HumanityDiscPlayer";
    @Unique
    private static final String NBT_HUMANITY_DISCOUNT_TIME = "MoreMod_HumanityDiscTime";
    @Unique
    private static final String NBT_HUMANITY_DISCOUNT_RATE = "MoreMod_HumanityDiscRate";
    @Unique
    private static final long DISCOUNT_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时后折扣过期

    /**
     * 在玩家与村民交互时检查是否持有说服器
     * processInteract -> func_184645_a
     */
    @Inject(method = "func_184645_a", at = @At("HEAD"))
    public void onProcessInteract(EntityPlayer player, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        EntityVillager villager = (EntityVillager)(Object)this;

        if (!villager.world.isRemote) {
            // 记录交互玩家（用于人性值恢复）
            this.lastInteractingPlayer = player;

            // ========== 人性值系统价格调整 (防止重复叠加) ==========
            if (HumanitySpectrumSystem.isSystemActive(player)) {
                float priceMultiplier = HumanityEffectsManager.getTradePriceMultiplier(player);
                if (priceMultiplier != 1.0f && priceMultiplier < 999f) {
                    // 🔒 检查是否已经给这个村民应用过折扣
                    if (!moremod$hasExistingHumanityDiscount(villager, player, priceMultiplier)) {
                        this.hasHumanityPriceModifier = true;
                        this.humanityPriceMultiplier = priceMultiplier;

                        MerchantRecipeList recipes = villager.getRecipes(player);
                        if (recipes != null) {
                            applyHumanityPriceModifier(recipes);
                            // 🔒 标记折扣已应用
                            moremod$markHumanityDiscountApplied(villager, player, priceMultiplier);
                        }
                    }
                }
            }

            // ========== 诛仙剑-为生民立命折扣 ==========
            if (ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
                if (!hasZhuxianDiscount) {
                    hasZhuxianDiscount = true;
                    MerchantRecipeList recipes = villager.getRecipes(player);
                    if (recipes != null) {
                        for (MerchantRecipe recipe : recipes) {
                            TradeDiscountHelper.applyDiscount(recipe, ZHUXIAN_DISCOUNT_RATE, TradeDiscountHelper.DiscountSource.HUMANITY);
                        }
                    }
                    // 播放效果
                    villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                            net.minecraft.init.SoundEvents.ENTITY_VILLAGER_YES,
                            villager.getSoundCategory(), 1.0F, 1.5F);
                }
            }

            // ========== 说服器折扣 ==========
            ItemStack persuader = MerchantPersuader.getActivePersuader(player);

            if (!persuader.isEmpty() && persuader.getItem() instanceof MerchantPersuader) {
                MerchantPersuader persuaderItem = (MerchantPersuader) persuader.getItem();
                double discount = persuaderItem.getCurrentDiscount(persuader);

                if (discount > 0) {
                    this.hasPersuaderDiscount = true;
                    this.persuaderDiscountRate = discount;
                    this.lastPersuaderPlayer = player;

                    TradeDiscountHelper.savePlayerDiscount(
                            player.getUniqueID().toString(),
                            discount,
                            persuaderItem.getEnergyStored(persuader)
                    );

                    MerchantRecipeList recipes = villager.getRecipes(player);
                    if (recipes != null) {
                        updateTradeDiscounts(recipes);
                    }

                    playPersuaderEffect(villager);
                }
            } else {
                clearPersuaderDiscount(villager, player);
            }
        }
    }

    /**
     * 在使用交易时处理说服器效果和人性值恢复
     * useRecipe -> func_70933_a
     */
    @Inject(method = "func_70933_a", at = @At("TAIL"))
    public void onUseRecipe(MerchantRecipe recipe, CallbackInfo ci) {
        EntityVillager villager = (EntityVillager)(Object)this;

        if (!villager.world.isRemote) {
            // ========== 人性值系统：交易恢复人性 ==========
            EntityPlayer tradingPlayer = lastInteractingPlayer != null ? lastInteractingPlayer : lastPersuaderPlayer;
            if (tradingPlayer != null && HumanitySpectrumSystem.isSystemActive(tradingPlayer)) {
                HumanitySpectrumSystem.onVillagerTrade(tradingPlayer);
            }

            // ========== 说服器效果 ==========
            if (hasPersuaderDiscount && lastPersuaderPlayer != null) {
                ItemStack persuader = MerchantPersuader.getActivePersuader(lastPersuaderPlayer);

                if (!persuader.isEmpty() && persuader.getItem() instanceof MerchantPersuader) {
                    MerchantPersuader persuaderItem = (MerchantPersuader) persuader.getItem();
                    int originalPrice = TradeDiscountHelper.getOriginalPrice(recipe);

                    ItemStack soldItem = recipe.getItemToBuy().copy();
                    soldItem.setCount(originalPrice);

                    persuaderItem.onTradeCompleted(lastPersuaderPlayer, persuader, villager,
                            soldItem, originalPrice);
                }
            }
        }
    }

    /**
     * 每tick更新说服器状态
     * onUpdate -> func_70071_h_
     */
    @Inject(method = "func_70071_h_", at = @At("HEAD"))
    public void onUpdate(CallbackInfo ci) {
        EntityVillager villager = (EntityVillager)(Object)this;

        if (!villager.world.isRemote) {
            if (hasPersuaderDiscount && lastPersuaderPlayer != null) {
                ItemStack persuader = MerchantPersuader.getActivePersuader(lastPersuaderPlayer);

                if (persuader.isEmpty() || !(persuader.getItem() instanceof MerchantPersuader)) {
                    clearPersuaderDiscount(villager, lastPersuaderPlayer);
                } else {
                    MerchantPersuader persuaderItem = (MerchantPersuader) persuader.getItem();
                    double newDiscount = persuaderItem.getCurrentDiscount(persuader);

                    if (Math.abs(newDiscount - persuaderDiscountRate) > 0.01) {
                        persuaderDiscountRate = newDiscount;
                        MerchantRecipeList recipes = villager.getRecipes(lastPersuaderPlayer);
                        if (recipes != null) {
                            updateTradeDiscounts(recipes);
                        }
                    }
                }
            }

            if (hasPersuaderDiscount && villager.world.rand.nextInt(40) == 0) {
                villager.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                        villager.posX, villager.posY + 2.0, villager.posZ,
                        0, 0, 0);
            }
        }
    }

    /**
     * 当交易GUI关闭时清理状态
     * setCustomer -> func_70932_a_
     */
    @Inject(method = "func_70932_a_", at = @At("HEAD"))
    public void onSetCustomer(EntityPlayer player, CallbackInfo ci) {
        if (player == null && hasPersuaderDiscount) {
            EntityVillager villager = (EntityVillager)(Object)this;
            clearPersuaderDiscount(villager, lastPersuaderPlayer);
        }
    }

    @Unique
    private void updateTradeDiscounts(MerchantRecipeList recipes) {
        if (recipes != null && hasPersuaderDiscount) {
            for (MerchantRecipe recipe : recipes) {
                if (!TradeDiscountHelper.hasDiscount(recipe)) {
                    TradeDiscountHelper.saveOriginalPrices(recipe);
                }
                TradeDiscountHelper.applyDiscount(recipe, persuaderDiscountRate);
            }
        }
    }

    @Unique
    private void clearPersuaderDiscount(EntityVillager villager, EntityPlayer player) {
        if (player != null) {
            MerchantRecipeList recipes = villager.getRecipes(player);
            if (recipes != null && hasPersuaderDiscount) {
                for (MerchantRecipe recipe : recipes) {
                    TradeDiscountHelper.removeDiscount(recipe);
                }
            }
        }

        hasPersuaderDiscount = false;
        persuaderDiscountRate = 0.0;
        lastPersuaderPlayer = null;
    }

    @Unique
    private void playPersuaderEffect(EntityVillager villager) {
        for (int i = 0; i < 10; i++) {
            double d0 = villager.world.rand.nextGaussian() * 0.02D;
            double d1 = villager.world.rand.nextGaussian() * 0.02D;
            double d2 = villager.world.rand.nextGaussian() * 0.02D;

            villager.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    villager.posX + (villager.world.rand.nextFloat() * villager.width * 2.0F) - villager.width,
                    villager.posY + 1.0D + (villager.world.rand.nextFloat() * villager.height),
                    villager.posZ + (villager.world.rand.nextFloat() * villager.width * 2.0F) - villager.width,
                    d0, d1, d2);
        }

        villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                net.minecraft.init.SoundEvents.ENTITY_VILLAGER_YES,
                villager.getSoundCategory(), 1.0F, 1.2F);
    }

    // ========== 人性值价格调整 ==========

    @Unique
    private void applyHumanityPriceModifier(MerchantRecipeList recipes) {
        if (recipes == null || !hasHumanityPriceModifier) return;

        for (MerchantRecipe recipe : recipes) {
            // 应用价格倍率（使用HUMANITY来源）
            // 折扣: multiplier < 1 (例如 0.85 = -15%)
            // 加价: multiplier > 1 (例如 1.5 = +50%)
            double discount = 1.0 - humanityPriceMultiplier; // 转换为折扣率
            TradeDiscountHelper.applyDiscount(recipe, discount, TradeDiscountHelper.DiscountSource.HUMANITY);
        }
    }

    // ========== 🔒 防止人性值折扣重复叠加 ==========

    /**
     * 检查村民是否已经从此玩家获得过折扣
     * @return true = 已有折扣，不需要再次应用
     */
    @Unique
    private boolean moremod$hasExistingHumanityDiscount(EntityVillager villager, EntityPlayer player, float newMultiplier) {
        net.minecraft.nbt.NBTTagCompound data = villager.getEntityData();

        // 检查是否有折扣记录
        if (!data.hasKey(NBT_HUMANITY_DISCOUNT_PLAYER)) {
            return false;
        }

        String savedPlayerUUID = data.getString(NBT_HUMANITY_DISCOUNT_PLAYER);
        String currentPlayerUUID = player.getUniqueID().toString();

        // 检查是否是同一个玩家
        if (!savedPlayerUUID.equals(currentPlayerUUID)) {
            // 不同玩家，允许应用新折扣（会覆盖旧的）
            return false;
        }

        // 检查折扣是否过期
        long savedTime = data.getLong(NBT_HUMANITY_DISCOUNT_TIME);
        if (System.currentTimeMillis() - savedTime > DISCOUNT_EXPIRE_TIME) {
            // 折扣已过期，允许重新应用
            return false;
        }

        // 检查折扣率是否相同（人性值可能变化）
        float savedRate = data.getFloat(NBT_HUMANITY_DISCOUNT_RATE);
        if (Math.abs(savedRate - newMultiplier) > 0.01f) {
            // 折扣率变化了，允许更新（但不叠加）
            // 先清除旧折扣再应用新折扣
            return false;
        }

        // 相同玩家、未过期、相同折扣率 = 已有折扣，跳过
        System.out.println("[MoreMod] 🔒 跳过重复人性值折扣: 玩家=" + player.getName() + ", 村民已有折扣");
        return true;
    }

    /**
     * 标记村民已从此玩家获得折扣
     */
    @Unique
    private void moremod$markHumanityDiscountApplied(EntityVillager villager, EntityPlayer player, float multiplier) {
        net.minecraft.nbt.NBTTagCompound data = villager.getEntityData();
        data.setString(NBT_HUMANITY_DISCOUNT_PLAYER, player.getUniqueID().toString());
        data.setLong(NBT_HUMANITY_DISCOUNT_TIME, System.currentTimeMillis());
        data.setFloat(NBT_HUMANITY_DISCOUNT_RATE, multiplier);
        System.out.println("[MoreMod] 🔒 标记人性值折扣已应用: 玩家=" + player.getName() + ", 倍率=" + multiplier);
    }
}