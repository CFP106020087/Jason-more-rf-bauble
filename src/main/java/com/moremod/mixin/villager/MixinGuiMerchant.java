package com.moremod.mixin.villager;

import com.moremod.item.MerchantPersuader;
import com.moremod.util.TradeDiscountHelper;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMerchant.class)
public abstract class MixinGuiMerchant extends GuiContainer {

    @Shadow @Final
    private IMerchant field_147037_w; // merchant

    @Shadow
    private int field_147041_z; // selectedMerchantRecipe

    @Unique
    private ItemStack cachedPersuader = ItemStack.EMPTY;

    @Unique
    private double cachedDiscountRate = 0.0;

    @Unique
    private boolean hasRenderedDiscount = false;

    public MixinGuiMerchant(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    /**
     * 在绘制前景层时显示折扣信息
     * 使用 SRG 名称: func_146979_b
     */
    @Inject(at = @At("HEAD"), method = "func_146979_b")
    protected void onDrawGuiContainerForegroundLayer(int mouseX, int mouseY, CallbackInfo ci) {
        // 重置渲染标记
        hasRenderedDiscount = false;

        // 检查玩家是否持有说服器
        EntityPlayer player = mc.player;
        ItemStack persuader = MerchantPersuader.getActivePersuader(player);

        if (!persuader.isEmpty() && persuader.getItem() instanceof MerchantPersuader) {
            MerchantPersuader persuaderItem = (MerchantPersuader) persuader.getItem();
            double discountRate = persuaderItem.getCurrentDiscount(persuader);

            // 缓存说服器信息
            cachedPersuader = persuader;
            cachedDiscountRate = discountRate;

            if (discountRate > 0 && !hasRenderedDiscount) {
                hasRenderedDiscount = true;

                // 绘制说服器状态指示器
                drawPersuaderStatus(discountRate, persuaderItem.getEnergyStored(persuader));

                // 如果有选中的交易，显示折扣信息
                MerchantRecipeList recipes = field_147037_w.getRecipes(player);
                if (recipes != null && !recipes.isEmpty() &&
                        field_147041_z >= 0 && field_147041_z < recipes.size()) {

                    MerchantRecipe recipe = recipes.get(field_147041_z);
                    drawDiscountInfo(recipe, discountRate);
                }
            }
        } else {
            cachedPersuader = ItemStack.EMPTY;
            cachedDiscountRate = 0.0;
        }
    }

    /**
     * 绘制说服器状态指示器
     */
    @Unique
    private void drawPersuaderStatus(double discountRate, int energy) {
        // 在GUI顶部绘制说服器状态
        int statusX = 5;
        int statusY = -25;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();

        // 背景框
        drawRect(statusX, statusY, statusX + 166, statusY + 20, 0xCC000000);

        // 说服器图标和文字
        String statusText = String.format("说服器生效: %.1f%% 折扣", discountRate * 100);
        fontRenderer.drawStringWithShadow(statusText, statusX + 3, statusY + 3, 0xFFFFD700);

        // 能量条
        int energyBarX = statusX + 3;
        int energyBarY = statusY + 13;
        int energyBarWidth = 160;
        int energyBarHeight = 4;

        // 能量条背景
        drawRect(energyBarX, energyBarY, energyBarX + energyBarWidth,
                energyBarY + energyBarHeight, 0xFF000000);

        // 能量条填充
        MerchantPersuader persuaderItem = (MerchantPersuader) cachedPersuader.getItem();
        int maxEnergy = persuaderItem.getMaxEnergyStored(cachedPersuader);
        int filledWidth = (int)((double)energy / maxEnergy * (energyBarWidth - 2));

        // 根据能量百分比决定颜色
        int color = energy > maxEnergy * 0.5 ? 0xFF00FF00 :
                energy > maxEnergy * 0.25 ? 0xFFFFFF00 : 0xFFFF0000;

        drawRect(energyBarX + 1, energyBarY + 1,
                energyBarX + 1 + filledWidth, energyBarY + energyBarHeight - 1, color);

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /**
     * 绘制交易折扣信息
     */
    @Unique
    private void drawDiscountInfo(MerchantRecipe recipe, double discountRate) {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();

        // 检查是否有第二个物品
        boolean hasSecondItem = !recipe.getSecondItemToBuy().isEmpty();

        // 处理第一个交易物品的折扣显示（只在没有第二个物品时显示）
        if (!hasSecondItem && !recipe.getItemToBuy().isEmpty()) {
            int priceX = 36;
            int priceY = 24;
            int currentPrice1 = recipe.getItemToBuy().getCount();
            int originalPrice1 = TradeDiscountHelper.getOriginalPrice(recipe);

            // 如果没有保存的原始价格，根据折扣率反推
            if (originalPrice1 <= 0 || originalPrice1 == currentPrice1) {
                originalPrice1 = (int) Math.round(currentPrice1 / (1.0 - discountRate));
            }

            // 显示折扣
            if (discountRate > 0 && originalPrice1 > currentPrice1) {
                drawSimpleDiscountDisplay(priceX, priceY, originalPrice1, currentPrice1);
            }
        }

        // 处理第二个交易物品的折扣显示（如果存在，只显示第二个）
        if (hasSecondItem) {
            int secondPriceX = 62;
            int secondPriceY = 24;
            int currentPrice2 = recipe.getSecondItemToBuy().getCount();
            int originalPrice2 = TradeDiscountHelper.getSecondOriginalPrice(recipe);

            // 如果没有保存的原始价格，根据折扣率计算
            if (originalPrice2 <= 0 || originalPrice2 == currentPrice2) {
                originalPrice2 = (int) Math.round(currentPrice2 / (1.0 - discountRate));
            }

            if (discountRate > 0 && originalPrice2 > currentPrice2) {
                drawSimpleDiscountDisplay(secondPriceX, secondPriceY, originalPrice2, currentPrice2);
            }
        }

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /**
     * 简化的折扣显示 - 只显示红色下划线强调新价格
     */
    @Unique
    private void drawSimpleDiscountDisplay(int x, int y, int originalPrice, int discountedPrice) {
        // 在物品数量下方绘制红色下划线以强调折扣价
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 300); // 确保在最上层

        // 绘制红色下划线
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        // 在物品槽的数字位置更下方画红线
        int underlineY = y + 18; // 调整到更下面的位置
        int underlineX = x + 2;
        int underlineWidth = 12;

        // 绘制红色强调下划线（稍粗一些）
        drawRect(underlineX, underlineY, underlineX + underlineWidth, underlineY + 2, 0xFFFF0000);

        // 可选：在旁边显示一个小的折扣百分比标签
        int saved = originalPrice - discountedPrice;
        double savePercent = (double)saved / originalPrice * 100;
        if (savePercent >= 10) { // 只在折扣超过10%时显示
            String saveText = String.format("-%.0f%%", savePercent);
            int labelX = x + 16;
            int labelY = y + 12;

            // 小标签背景
            int labelWidth = fontRenderer.getStringWidth(saveText) + 2;
            drawRect(labelX, labelY, labelX + labelWidth, labelY + 8, 0xBB000000);

            // 标签文字（更小）
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 1.0F);
            fontRenderer.drawString(saveText,
                    (int)((labelX + 1) / 0.75F),
                    (int)((labelY + 1) / 0.75F), 0xFFFFD700);
            GlStateManager.popMatrix();
        }

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        // 重置颜色
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 绘制说服器光效
     */
    @Unique
    private void drawPersuaderGlow(int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        // 绘制光晕效果
        GlStateManager.color(1.0F, 0.843F, 0.0F, 0.3F); // 金色光晕

        for (int i = 0; i < 3; i++) {
            int size = 20 + i * 2;
            int offset = -i;
            drawGradientRect(
                    x + offset - 2, y + offset - 2,
                    x + size + 2, y + size + 2,
                    0x20FFD700, 0x00FFD700
            );
        }

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}