package com.moremod.mixin.villager;

import com.moremod.item.MerchantPersuader;
import com.moremod.util.TradeDiscountHelper;
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

    /**
     * 在玩家与村民交互时检查是否持有说服器
     * processInteract -> func_184645_a
     */
    @Inject(method = "func_184645_a", at = @At("HEAD"))
    public void onProcessInteract(EntityPlayer player, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        EntityVillager villager = (EntityVillager)(Object)this;

        if (!villager.world.isRemote) {
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
     * 在使用交易时处理说服器效果
     * useRecipe -> func_70933_a
     */
    @Inject(method = "func_70933_a", at = @At("TAIL"))
    public void onUseRecipe(MerchantRecipe recipe, CallbackInfo ci) {
        EntityVillager villager = (EntityVillager)(Object)this;

        if (!villager.world.isRemote && hasPersuaderDiscount && lastPersuaderPlayer != null) {
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
}