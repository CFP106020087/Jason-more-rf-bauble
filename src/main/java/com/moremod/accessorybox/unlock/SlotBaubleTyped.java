package com.moremod.accessorybox.unlock;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.container.SlotBauble;
import com.moremod.accessorybox.SlotLayoutHelper;
import com.moremod.accessorybox.client.ExtraSlotsToggle;
import com.moremod.accessorybox.compat.AccessoryBoxCrTCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 额外槽位专用 Slot
 * - 动态坐标：根据 EX 开关自动调整位置
 * - 类型限制和解锁检查
 * - CraftTweaker 事件支持
 */
public class SlotBaubleTyped extends SlotBauble {

    private final BaubleType expectedType;
    private final EntityPlayer player;
    private final int logicalSlotId;  // 逻辑槽位ID（7, 8, 9...）

    // 原始坐标（创建时的坐标）
    private final int originalX;
    private final int originalY;

    // 是否是额外槽位
    private final boolean isExtraSlot;

    public SlotBaubleTyped(EntityPlayer player,
                           IBaublesItemHandler handler,
                           int baubleSlotId,
                           int x, int y,
                           BaubleType expectedType) {
        super(player, handler, baubleSlotId, x, y);
        this.player = player;
        this.logicalSlotId = SlotLayoutHelper.toLogicalSlot(baubleSlotId);
        this.expectedType = expectedType;
        this.originalX = x;
        this.originalY = y;
        this.isExtraSlot = SlotLayoutHelper.isExtraSlot(logicalSlotId);

        // 初始化时就设置正确的坐标
        updatePosition();
    }

    /**
     * ⭐ 关键：在每次访问坐标前动态更新
     * 这样 Minecraft/Baubles 渲染时会自动获取正确的坐标
     *
     * 注意：不能加 @SideOnly(Side.CLIENT)，因为构造函数在服务器端也会调用
     * 通过运行时检查来避免在服务器端执行客户端逻辑
     */
    private void updatePosition() {
        // 只在客户端且是额外槽位时才更新坐标
        if (!isExtraSlot || player == null || player.world == null || !player.world.isRemote) {
            return;
        }

        try {
            if (ExtraSlotsToggle.isVisible()) {
                // EX 开启：使用原始坐标
                this.xPos = originalX;
                this.yPos = originalY;
            } else {
                // EX 关闭：移到屏幕外
                this.xPos = -9999;
                this.yPos = -9999;
            }
        } catch (Throwable t) {
            // 如果出错（比如 ExtraSlotsToggle 在服务器端不存在），使用原始坐标
            this.xPos = originalX;
            this.yPos = originalY;
        }
    }

    /**
     * ⭐ Minecraft 在渲染/交互前会调用这些方法来获取坐标
     * 我们在这里确保坐标是最新的
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean isEnabled() {
        // 双重保险：确保只在客户端更新
        if (player != null && player.world != null && player.world.isRemote) {
            updatePosition();
        }

        try {
            return ExtraSlotsToggle.isVisible();
        } catch (Throwable t) {
            // 如果 ExtraSlotsToggle 出现问题，默认启用
            return true;
        }
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBauble)) return false;

        // 客户端检查：EX 隐藏 → 不准塞入
        if (player.world.isRemote) {
            try {
                if (!ExtraSlotsToggle.isVisible()) return false;
            } catch (Throwable ignored) {
                // 服务器端忽略此检查
            }
        }

        // 槽位未解锁 → 拒绝
        if (!SlotUnlockManager.getInstance().isSlotUnlocked(player, logicalSlotId)) return false;

        IBauble bauble = (IBauble) stack.getItem();
        BaubleType itemType = bauble.getBaubleType(stack);

        // TRINKET 可以放在任何槽位
        if (itemType == BaubleType.TRINKET) {
            return bauble.canEquip(stack, player);
        }

        // TRINKET 槽位可以接受任何类型
        if (expectedType == BaubleType.TRINKET) {
            return bauble.canEquip(stack, player);
        }

        // 其他需要类型匹配
        if (itemType != expectedType) return false;

        return bauble.canEquip(stack, player);
    }

    @Override
    public void putStack(ItemStack stack) {
        ItemStack oldStack = this.getStack();

        if (!oldStack.isEmpty() && oldStack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireUnequipPre(player, logicalSlotId, oldStack);

            if (!canceled) {
                ((IBauble) oldStack.getItem()).onUnequipped(oldStack, player);
                AccessoryBoxCrTCompat.fireUnequipPost(player, logicalSlotId, oldStack);
            } else {
                return;
            }
        }

        super.putStack(stack);

        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireEquipPre(player, logicalSlotId, stack);

            if (!canceled) {
                ((IBauble) stack.getItem()).onEquipped(stack, player);
                AccessoryBoxCrTCompat.fireEquipPost(player, logicalSlotId, stack);
            } else {
                super.putStack(oldStack);
            }
        }
    }

    @Override
    public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireUnequipPre(playerIn, logicalSlotId, stack);

            if (!canceled) {
                ((IBauble) stack.getItem()).onUnequipped(stack, playerIn);
                AccessoryBoxCrTCompat.fireUnequipPost(playerIn, logicalSlotId, stack);
                return super.onTake(playerIn, stack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        return super.onTake(playerIn, stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        // 客户端检查：EX 隐藏 → 不准取出
        if (playerIn.world.isRemote) {
            try {
                if (!ExtraSlotsToggle.isVisible()) return false;
            } catch (Throwable ignored) {
                // 服务器端忽略此检查
            }
        }

        ItemStack stack = this.getStack();
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            return ((IBauble) stack.getItem()).canUnequip(stack, playerIn);
        }

        return super.canTakeStack(playerIn);
    }

    public int getLogicalSlotId() {
        return logicalSlotId;
    }

    public BaubleType getExpectedType() {
        return expectedType;
    }
}