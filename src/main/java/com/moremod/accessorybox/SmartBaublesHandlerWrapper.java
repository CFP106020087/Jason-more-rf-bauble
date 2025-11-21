package com.moremod.accessorybox;

import baubles.api.IBauble;
import baubles.api.cap.BaublesContainer;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.SlotUnlockManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 智能饰品 Handler 包装器：
 * - 保留 Baubles 原有行为（inner）
 * - 额外挂两层防线：
 *   1) 槽位是否已解锁（SlotUnlockManager）
 *   2) 槽位类型是否匹配（AMULET/RING/...，TRINKET 例外）
 *
 * 仅拦截 isItemValidForSlot，用于阻止「右键快速佩戴」作弊。
 * 其他方法全部直接委派给原始 handler，保证同步与事件逻辑不变。
 */
public class SmartBaublesHandlerWrapper extends BaublesContainer {

    private final IBaublesItemHandler inner;

    public SmartBaublesHandlerWrapper(IBaublesItemHandler inner) {
        super();
        this.inner = inner;
    }

    // ============================================
    // 核心：覆盖 isItemValidForSlot（右键佩戴入口）
    // ============================================
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack, EntityLivingBase player) {

        // 1) 先尊重原始 Baubles 判定（包括 canEquip + BaubleType.hasSlot 等）
        if (!inner.isItemValidForSlot(slot, stack, player)) {
            return false;
        }

        // 2) 必须是 IBauble
        if (!(stack.getItem() instanceof IBauble)) {
            return false;
        }

        // 3) 槽位解锁检查（只对玩家做，防止空指针）
        if (player instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) player;
            // 这里会自动放行：
            // - slot < 7（原版 7 个永远可用）
            // - 配置中标记为默认解锁的额外槽位
            // - 被永久/临时解锁过的槽位
            if (!SlotUnlockManager.getInstance().isSlotUnlocked(ep, slot)) {
                return false; // 🔒 锁着的格子：禁止佩戴
            }
        }

        // 4) 类型匹配
        IBauble bauble = (IBauble) stack.getItem();
        baubles.api.BaubleType type = bauble.getBaubleType(stack);

        // TRINKET：万能槽位 → 任何「已解锁」的格子都可以戴
        //（类型不再限制，但仍然服从解锁系统）
        if (type == baubles.api.BaubleType.TRINKET) {
            return true;
        }

        // 其他类型：必须落在它们对应的类型槽位上
        baubles.api.BaubleType expected = SlotLayoutHelper.getExpectedTypeForSlot(slot);
        return expected == type;
    }

    // ============================================
    // 其余全部方法 → 直接委派给 inner
    // ============================================

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inner.setStackInSlot(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return inner.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inner.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isEventBlocked() {
        return inner.isEventBlocked();
    }

    @Override
    public void setEventBlock(boolean blockEvents) {
        inner.setEventBlock(blockEvents);
    }

    @Override
    public boolean isChanged(int slot) {
        return inner.isChanged(slot);
    }

    @Override
    public void setChanged(int slot, boolean change) {
        inner.setChanged(slot, change);
    }

    @Override
    public void setPlayer(EntityLivingBase player) {
        inner.setPlayer(player);
    }
}
