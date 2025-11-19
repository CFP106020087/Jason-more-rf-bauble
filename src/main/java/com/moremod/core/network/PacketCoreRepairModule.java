package com.moremod.core.network;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;
import com.moremod.item.ItemMechanicalCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络包：修复模块
 *
 * 客户端 -> 服务端
 *
 * 服务端会验证：
 * - 玩家是否装备了核心
 * - 模块是否损坏
 * - 是否有足够的能量
 * - 目标等级是否有效
 */
public class PacketCoreRepairModule implements IMessage {

    private String upgradeId;
    private int targetLevel;
    private boolean fullRepair; // 是否完全修复

    // 无参构造函数（Forge需要）
    public PacketCoreRepairModule() {}

    /**
     * 部分修复到指定等级
     */
    public PacketCoreRepairModule(String upgradeId, int targetLevel) {
        this.upgradeId = upgradeId;
        this.targetLevel = targetLevel;
        this.fullRepair = false;
    }

    /**
     * 完全修复
     */
    public PacketCoreRepairModule(String upgradeId) {
        this.upgradeId = upgradeId;
        this.targetLevel = 0; // 完全修复时不使用此值
        this.fullRepair = true;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        upgradeId = ByteBufUtils.readUTF8String(buf);
        targetLevel = buf.readInt();
        fullRepair = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, upgradeId);
        buf.writeInt(targetLevel);
        buf.writeBoolean(fullRepair);
    }

    public static class Handler implements IMessageHandler<PacketCoreRepairModule, IMessage> {
        @Override
        public IMessage onMessage(PacketCoreRepairModule message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                handlePacket(message, player);
            });

            return null;
        }

        private void handlePacket(PacketCoreRepairModule message, EntityPlayerMP player) {
            // 查找装备的核心
            ItemStack core = findEquippedCore(player);
            if (core.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "未装备机械核心"));
                return;
            }

            // 获取Capability
            IMechanicalCoreData data = core.getCapability(
                    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
            if (data == null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "核心数据错误"));
                return;
            }

            // 规范化升级ID
            String canonId = UpgradeRegistry.canonicalIdOf(message.upgradeId);

            // 检查模块是否损坏
            if (!data.isDamaged(canonId)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "模块未损坏，无需修复"));
                return;
            }

            int currentMax = data.getOwnedMax(canonId);
            int originalMax = data.getOriginalMax(canonId);
            int damageCount = data.getDamageCount(canonId);

            // 确定目标等级
            int finalTargetLevel = message.fullRepair ? originalMax : message.targetLevel;

            // 验证目标等级
            if (finalTargetLevel > originalMax) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "无法修复到超过原始等级 (" + originalMax + ")"));
                return;
            }

            if (finalTargetLevel <= currentMax) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "已达到或超过目标等级"));
                return;
            }

            // 计算修复成本
            int levelDiff = finalTargetLevel - currentMax;
            int energyCost = calculateRepairCost(levelDiff, damageCount);

            // 检查并消耗能量
            IEnergyStorage energy = core.getCapability(
                    net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);

            if (energy == null || energy.getEnergyStored() < energyCost) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！需要 " + energyCost + " RF"));
                return;
            }

            energy.extractEnergy(energyCost, false);

            // 执行修复
            if (message.fullRepair) {
                data.fullRepair(canonId);
            } else {
                data.repair(canonId, finalTargetLevel);
            }

            // 发送成功消息
            String displayName = UpgradeRegistry.getDisplayName(canonId);
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "成功修复 " + displayName +
                    " 到等级 " + finalTargetLevel +
                    " (消耗 " + energyCost + " RF)"));

            // 同步到客户端
            if (core.hasTagCompound()) {
                core.getTagCompound().setTag("CoreData", data.serializeNBT());
            }
        }

        /**
         * 查找装备的机械核心
         */
        private ItemStack findEquippedCore(EntityPlayerMP player) {
            try {
                IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                if (baubles != null) {
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemMechanicalCore) {
                            return stack;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ItemStack.EMPTY;
        }

        /**
         * 计算修复成本
         *
         * 成本公式：(等级差 * 10000) + (损坏计数 * 5000)
         */
        private int calculateRepairCost(int levelDiff, int damageCount) {
            return (levelDiff * 10000) + (damageCount * 5000);
        }
    }
}
