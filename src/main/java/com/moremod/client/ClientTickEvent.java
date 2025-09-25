// ClientTickEvent.java
package com.moremod.client;

import baubles.api.BaublesApi;
import com.moremod.item.ItemJetpackBauble;
import com.moremod.item.ItemCreativeJetpackBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.util.text.TextFormatting;

@SideOnly(Side.CLIENT)
public class ClientTickEvent {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // 保留空方法或删除此方法都可（如无必要）
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        for (int i = 0; i < BaublesApi.getBaublesHandler(mc.player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(mc.player).getStackInSlot(i);

            // 检查是否是喷气背包类型的物品（支持普通和创造模式）
            if (stack.getItem() instanceof ItemJetpackBauble || stack.getItem() instanceof ItemCreativeJetpackBauble) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) continue;

                boolean jetpackOn = tag.getBoolean("JetpackEnabled");
                boolean hoverOn = tag.getBoolean("HoverEnabled");

                // 速度模式信息（仅创造模式喷气背包有）
                String speedInfo = "";
                if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    ItemCreativeJetpackBauble creativeJetpack = (ItemCreativeJetpackBauble) stack.getItem();
                    ItemCreativeJetpackBauble.SpeedMode speedMode = creativeJetpack.getSpeedMode(stack);
                    speedInfo = " | " + speedMode.getColor() + speedMode.getName();
                }

                // 获取能量存储
                IEnergyStorage storage = null;
                if (stack.getItem() instanceof ItemJetpackBauble) {
                    storage = ItemJetpackBauble.getEnergyStorage(stack);
                } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    storage = ItemCreativeJetpackBauble.getEnergyStorage(stack);
                }

                // 能量信息显示
                String energyInfo;
                if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    energyInfo = "∞"; // 创造模式显示无限符号
                } else {
                    int percent = (storage != null && storage.getMaxEnergyStored() > 0)
                            ? (storage.getEnergyStored() * 100 / storage.getMaxEnergyStored()) : 0;
                    energyInfo = percent + "%";
                }

                // 构建HUD显示文本
                String jetpackType = "";
                if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    jetpackType = TextFormatting.GOLD + "[Creative Jetpack] ";
                } else {
                    jetpackType = TextFormatting.YELLOW + "喷气背包";
                }

                String hud = jetpackType +
                        TextFormatting.YELLOW + (jetpackOn ? "开" : "关") + " | " +
                        (hoverOn ? "悬浮 开" : "悬浮 关") +
                        speedInfo + TextFormatting.YELLOW + " | " +
                        energyInfo;

                mc.fontRenderer.drawStringWithShadow(hud, 5, 5, 0xFFFFFF);
                break; // 只显示第一个找到的喷气背包
            }
        }
    }
}