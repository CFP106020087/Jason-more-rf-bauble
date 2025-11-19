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
 * 网络包：设置升级等级
 *
 * 客户端 -> 服务端
 *
 * 服务端会验证：
 * - 玩家是否装备了核心
 * - 是否有足够的能量
 * - 等级是否在有效范围内
 * - 是否超过拥有的最大等级
 */
public class PacketCoreSetLevel implements IMessage {

    private String upgradeId;
    private int newLevel;

    // 无参构造函数（Forge需要）
    public PacketCoreSetLevel() {}

    public PacketCoreSetLevel(String upgradeId, int newLevel) {
        this.upgradeId = upgradeId;
        this.newLevel = newLevel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        upgradeId = ByteBufUtils.readUTF8String(buf);
        newLevel = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, upgradeId);
        buf.writeInt(newLevel);
    }

    public static class Handler implements IMessageHandler<PacketCoreSetLevel, IMessage> {
        @Override
        public IMessage onMessage(PacketCoreSetLevel message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                handlePacket(message, player);
            });

            return null;
        }

        private void handlePacket(PacketCoreSetLevel message, EntityPlayerMP player) {
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

            // 获取当前等级和拥有的最大等级
            int currentLevel = data.getLevel(canonId);
            int ownedMax = data.getOwnedMax(canonId);

            // 验证新等级
            int requestedLevel = Math.max(0, message.newLevel);

            // 检查是否超过拥有的最大等级
            if (requestedLevel > ownedMax) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "等级超过拥有的最大值 (" + ownedMax + ")"));
                return;
            }

            // 检查能量消耗（可选，根据需求启用）
            if (requestedLevel > currentLevel) {
                int energyCost = calculateEnergyCost(currentLevel, requestedLevel);
                if (!consumeEnergy(core, energyCost)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "能量不足 (需要 " + energyCost + " RF)"));
                    return;
                }
            }

            // 设置新等级
            data.setLevel(canonId, requestedLevel);

            // 发送成功消息
            String displayName = UpgradeRegistry.getDisplayName(canonId);
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "已设置 " + displayName +
                    " 等级为 " + requestedLevel));

            // 同步到客户端（通过NBT更新）
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
         * 计算能量消耗（升级时消耗能量）
         */
        private int calculateEnergyCost(int from, int to) {
            // 可选：升级时消耗能量
            // 默认不消耗（设置为0）
            return 0;

            // 示例：每提升1级消耗1000 RF
            // return (to - from) * 1000;
        }

        /**
         * 消耗能量
         */
        private boolean consumeEnergy(ItemStack core, int amount) {
            if (amount <= 0) {
                return true; // 不需要消耗能量
            }

            IEnergyStorage energy = core.getCapability(
                    net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);

            if (energy == null) {
                return false;
            }

            int extracted = energy.extractEnergy(amount, true);
            if (extracted < amount) {
                return false; // 能量不足
            }

            energy.extractEnergy(amount, false);
            return true;
        }
    }
}
