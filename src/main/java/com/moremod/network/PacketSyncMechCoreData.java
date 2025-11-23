package com.moremod.network;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.ModuleContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 网络包：同步 MechCoreData 到客户端
 *
 * 用途：
 *  - 服务端 → 客户端同步 Capability 数据
 *  - 支持 GUI 实时显示
 *  - 支持 HUD 显示
 */
public class PacketSyncMechCoreData implements IMessage {

    private int energy;
    private int maxEnergy;
    private NBTTagCompound moduleData;

    /** 无参构造（网络反序列化需要） */
    public PacketSyncMechCoreData() {}

    /**
     * 构造包（服务端创建）
     */
    public PacketSyncMechCoreData(IMechCoreData data) {
        this.energy = data.getEnergy();
        this.maxEnergy = data.getMaxEnergy();

        // 序列化模块容器数据
        this.moduleData = new NBTTagCompound();
        data.getModuleContainer().serializeNBT(this.moduleData);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.energy = buf.readInt();
        this.maxEnergy = buf.readInt();
        this.moduleData = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(energy);
        buf.writeInt(maxEnergy);
        ByteBufUtils.writeTag(buf, moduleData);
    }

    /**
     * 处理器：客户端接收并应用数据
     */
    public static class Handler implements IMessageHandler<PacketSyncMechCoreData, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncMechCoreData message, MessageContext ctx) {
            // 在客户端主线程处理
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;

                if (player == null) return;

                // 获取客户端 Capability
                IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);

                if (data == null) return;

                // 更新能量数据
                data.setEnergy(message.energy);
                data.setMaxEnergy(message.maxEnergy);

                // 更新模块容器数据
                if (message.moduleData != null) {
                    data.getModuleContainer().deserializeNBT(message.moduleData);
                }
            });

            return null;
        }
    }
}
