package com.moremod.mixin.villager;

import net.minecraft.network.PacketBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理商人配方列表的网络同步，包含说服器折扣信息
 */
@Mixin(MerchantRecipeList.class)
public class MixinMerchantRecipeList extends ArrayList<MerchantRecipe> {

    @Unique
    private static final Map<Integer, Integer> originalPrices = new HashMap<>();

    /**
     * 写入数据包时包含说服器折扣信息
     * 使用 SRG 名称: func_151395_a
     */
    @Inject(at = @At("HEAD"), method = "func_151395_a", cancellable = true)
    public void writePersuaderDataToBuf(PacketBuffer buffer, CallbackInfo ci) {
        buffer.writeByte((byte)(this.size() & 255));

        // 写入每个配方
        for (int i = 0; i < this.size(); ++i) {
            MerchantRecipe recipe = this.get(i);

            // 保存原始价格
            originalPrices.put(i, recipe.getItemToBuy().getCount());

            buffer.writeItemStack(recipe.getItemToBuy());
            buffer.writeItemStack(recipe.getItemToSell());

            ItemStack secondItem = recipe.getSecondItemToBuy();
            buffer.writeBoolean(!secondItem.isEmpty());
            if (!secondItem.isEmpty()) {
                buffer.writeItemStack(secondItem);
            }

            buffer.writeBoolean(recipe.isRecipeDisabled());
            buffer.writeInt(recipe.getToolUses());
            buffer.writeInt(recipe.getMaxTradeUses());
        }

        // 写入原始价格信息供客户端使用
        for (int i = 0; i < this.size(); ++i) {
            buffer.writeInt(originalPrices.getOrDefault(i, this.get(i).getItemToBuy().getCount()));
        }

        ci.cancel();
    }

    /**
     * 从数据包读取说服器折扣信息
     * 使用 SRG 名称: func_151396_a
     */
    @Inject(at = @At("TAIL"),
            method = "func_151396_a",
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void readPersuaderDataFromBuf(PacketBuffer buffer,
                                                 CallbackInfoReturnable<MerchantRecipeList> cir,
                                                 MerchantRecipeList list) {
        // 读取原始价格信息
        for (int i = 0; i < list.size() && buffer.readableBytes() >= 4; i++) {
            int originalPrice = buffer.readInt();
            originalPrices.put(i, originalPrice);
        }
    }
}