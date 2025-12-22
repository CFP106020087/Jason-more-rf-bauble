package com.moremod.mixin.enchant;

import com.moremod.item.EnchantmentBoostBauble;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * MoreMod - 附魔增强 Mixin
 * 参考 EnigmaticLegacy 实现
 *
 * 当玩家佩戴 EnchantmentBoostBauble 时：
 * - 附魔时获得双重附魔效果（原始 + 额外高等级roll合并）
 * - 青金石消耗显示为无限
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment extends Container {

    @Final
    @Shadow
    private World world;

    @Final
    @Shadow
    private BlockPos position;

    @Shadow
    protected abstract List<EnchantmentData> getEnchantmentList(ItemStack stack, int enchantSlot, int level);

    /**
     * 附魔时注入 - 佩戴饰品时提供双重附魔效果
     */
    @Inject(at = @At(value = "HEAD"), method = "enchantItem", cancellable = true)
    private void moremod$onEnchantedItem(EntityPlayer player, int clickedID, CallbackInfoReturnable<Boolean> info) {
        ContainerEnchantment container = (ContainerEnchantment) (Object) this;

        // 检查玩家是否佩戴附魔增强饰品
        if (EnchantmentBoostBauble.hasBoostBauble(player)) {
            ItemStack itemstack = container.tableInventory.getStackInSlot(0);
            int levelsRequired = clickedID + 1;

            if (container.enchantLevels[clickedID] > 0 && !itemstack.isEmpty() &&
                (player.experienceLevel >= clickedID && player.experienceLevel >= container.enchantLevels[clickedID] || player.capabilities.isCreativeMode)) {

                ItemStack enchantedItem = itemstack;

                if (!this.world.isRemote) {
                    List<EnchantmentData> list = getEnchantmentList(itemstack, clickedID, container.enchantLevels[clickedID]);

                    if (!list.isEmpty()) {
                        // 获取饰品增幅等级
                        int boostAmount = EnchantmentBoostBauble.getBaubleBoostAmount(player);
                        int bonusLevel = Math.min(container.enchantLevels[clickedID] + boostAmount, 50);

                        // 双重roll - 额外生成一个更高等级的附魔结果
                        ItemStack doubleRoll = EnchantmentHelper.addRandomEnchantment(
                            player.getRNG(),
                            enchantedItem.copy(),
                            bonusLevel,
                            true
                        );

                        player.onEnchant(itemstack, levelsRequired);
                        boolean flag = itemstack.getItem() == Items.BOOK;

                        if (flag) {
                            enchantedItem = new ItemStack(Items.ENCHANTED_BOOK);
                            container.tableInventory.setInventorySlotContents(0, enchantedItem);
                        }

                        // 应用原始附魔
                        for (int j = 0; j < list.size(); ++j) {
                            EnchantmentData enchantmentdata = list.get(j);

                            if (flag) {
                                ItemEnchantedBook.addEnchantment(enchantedItem, enchantmentdata);
                            } else {
                                enchantedItem.addEnchantment(enchantmentdata.enchantment, enchantmentdata.enchantmentLevel);
                            }
                        }

                        // 合并双重roll的附魔
                        enchantedItem = mergeEnchantments(enchantedItem, doubleRoll, false);
                        container.tableInventory.setInventorySlotContents(0, enchantedItem);

                        player.addStat(StatList.ITEM_ENCHANTED);

                        if (player instanceof EntityPlayerMP) {
                            CriteriaTriggers.ENCHANTED_ITEM.trigger((EntityPlayerMP) player, itemstack, levelsRequired);
                        }

                        container.tableInventory.markDirty();
                        container.xpSeed = player.getXPSeed();
                        this.onCraftMatrixChanged(container.tableInventory);
                        this.world.playSound(null, this.position, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F);
                    }
                }

                info.setReturnValue(true);
            } else {
                info.setReturnValue(false);
            }
        }
    }

    /**
     * 客户端青金石显示 - 佩戴饰品时显示为无限
     */
    @SideOnly(Side.CLIENT)
    @Inject(at = @At("HEAD"), method = "getLapisAmount", cancellable = true)
    public void moremod$onGetLapisAmount(CallbackInfoReturnable<Integer> info) {
        Object forgottenObject = this;
        ContainerEnchantment container = (ContainerEnchantment) forgottenObject;
        EntityPlayer containerUser = null;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof InventoryPlayer) {
                InventoryPlayer playerInv = (InventoryPlayer) slot.inventory;
                containerUser = playerInv.player;
                break;
            }
        }

        if (containerUser != null) {
            if (EnchantmentBoostBauble.hasBoostBauble(containerUser)) {
                info.setReturnValue(64);
            }
        }
    }

    /**
     * 合并两个物品的附魔
     * @param target 目标物品
     * @param source 来源物品（提取附魔）
     * @param keepHigher 是否保留较高等级
     * @return 合并后的物品
     */
    private static ItemStack mergeEnchantments(ItemStack target, ItemStack source, boolean keepHigher) {
        if (source.isEmpty() || !source.isItemEnchanted()) {
            return target;
        }

        java.util.Map<net.minecraft.enchantment.Enchantment, Integer> targetEnch = EnchantmentHelper.getEnchantments(target);
        java.util.Map<net.minecraft.enchantment.Enchantment, Integer> sourceEnch = EnchantmentHelper.getEnchantments(source);

        for (java.util.Map.Entry<net.minecraft.enchantment.Enchantment, Integer> entry : sourceEnch.entrySet()) {
            net.minecraft.enchantment.Enchantment ench = entry.getKey();
            int sourceLevel = entry.getValue();

            if (targetEnch.containsKey(ench)) {
                int targetLevel = targetEnch.get(ench);
                if (keepHigher) {
                    targetEnch.put(ench, Math.max(targetLevel, sourceLevel));
                } else {
                    // 合并：如果等级相同则+1，否则取较高
                    if (targetLevel == sourceLevel && targetLevel < ench.getMaxLevel() + 2) {
                        targetEnch.put(ench, targetLevel + 1);
                    } else {
                        targetEnch.put(ench, Math.max(targetLevel, sourceLevel));
                    }
                }
            } else {
                // 检查兼容性
                boolean compatible = true;
                for (net.minecraft.enchantment.Enchantment existing : targetEnch.keySet()) {
                    if (!ench.isCompatibleWith(existing)) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    targetEnch.put(ench, sourceLevel);
                }
            }
        }

        EnchantmentHelper.setEnchantments(targetEnch, target);
        return target;
    }
}
