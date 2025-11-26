package com.moremod.network;

import com.moremod.capabilities.autoattack.AutoAttackComboProvider;
import com.moremod.capabilities.autoattack.IAutoAttackCombo;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 → 服务器：触发自动攻击
 * 
 * 客户端检测到按住左键时发送此消息
 */
public class MessageAutoAttackTrigger implements IMessage {
    
    private boolean isAttacking;
    
    public MessageAutoAttackTrigger() {
    }
    
    public MessageAutoAttackTrigger(boolean isAttacking) {
        this.isAttacking = isAttacking;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.isAttacking = buf.readBoolean();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isAttacking);
    }
    
    public static class Handler implements IMessageHandler<MessageAutoAttackTrigger, IMessage> {
        @Override
        public IMessage onMessage(final MessageAutoAttackTrigger message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            
            player.getServer().addScheduledTask(() -> {
                IAutoAttackCombo cap = player.getCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null);
                if (cap != null) {
                    cap.setAutoAttacking(message.isAttacking);
                    
                    // 停止攻击时，延迟重置连击（给予1秒窗口期）
                    if (!message.isAttacking) {
                        cap.setComboTime(20); // 1秒窗口期
                    } else {
                        // 重新开始攻击，保持连击
                        if (cap.getComboTime() > 0) {
                            // 在窗口期内，不重置连击
                        }
                    }
                }
            });
            
            return null;
        }
    }
}
