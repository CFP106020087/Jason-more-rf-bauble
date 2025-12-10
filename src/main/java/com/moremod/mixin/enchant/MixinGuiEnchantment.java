package com.moremod.mixin.enchant;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEnchantment;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.IOException;

/**
 * 修复附魔等级>100时无法点击的问题
 *
 * 问题原因:
 * - 原版 GuiEnchantment.mouseClicked() 中检查 player.experienceLevel >= enchantLevels[id]
 * - 当附魔等级超过某个阈值时，客户端的检查可能出现问题
 *
 * 解决方案:
 * - 覆盖mouseClicked方法，移除客户端的等级检查
 * - 只要槽位有效且有附魔选项，就允许点击
 * - 实际的等级检查由服务端在 ContainerEnchantment.enchantItem() 中执行
 */
@Mixin(GuiEnchantment.class)
public abstract class MixinGuiEnchantment extends GuiContainer {

    public MixinGuiEnchantment(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    /**
     * @author MoreMod
     * @reason 修复高等级附魔无法点击的问题
     *
     * 原版代码检查: player.experienceLevel >= enchantLevels[slot]
     * 当附魔等级超过100时，这个检查可能因为某些原因失败
     * 我们移除客户端检查，让服务端来验证
     */
    @Overwrite
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        ContainerEnchantment container = (ContainerEnchantment) this.inventorySlots;

        // 遍历三个附魔槽位
        for (int slot = 0; slot < 3; slot++) {
            // 计算相对于按钮区域的坐标 (原版逻辑)
            // 按钮区域起始于 guiLeft + 60, guiTop + 14 + 19*slot
            int buttonRelX = mouseX - (this.guiLeft + 60);
            int buttonRelY = mouseY - (this.guiTop + 14 + 19 * slot);

            // 检查是否在按钮范围内 (宽108, 高19)
            if (buttonRelX >= 0 && buttonRelX < 108 && buttonRelY >= 0 && buttonRelY < 19) {
                // 检查这个槽位是否有附魔选项
                if (container.enchantLevels[slot] > 0) {
                    // ✅ 移除客户端等级检查，直接发送附魔请求
                    // 服务端会在 ContainerEnchantment.enchantItem() 中验证等级
                    if (this.mc.player != null && this.mc.playerController != null) {
                        System.out.println("[MoreMod-Enchant] 客户端点击附魔槽位 " + slot +
                            ", 需要等级=" + container.enchantLevels[slot] +
                            ", 玩家等级=" + this.mc.player.experienceLevel);

                        this.mc.playerController.sendEnchantPacket(
                            this.inventorySlots.windowId, slot);
                    }
                }
                return;
            }
        }
    }
}
