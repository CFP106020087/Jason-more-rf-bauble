package com.moremod.sponsor.network;

import com.moremod.sponsor.item.ZhuxianSword;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 切换技能网络包
 */
public class PacketToggleSkill implements IMessage {

    private String skillKey;

    public PacketToggleSkill() {
    }

    public PacketToggleSkill(String skillKey) {
        this.skillKey = skillKey;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        skillKey = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, skillKey);
    }

    /**
     * 服务器端处理
     */
    public static class Handler implements IMessageHandler<PacketToggleSkill, IMessage> {

        @Override
        public IMessage onMessage(PacketToggleSkill message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack sword = ZhuxianSword.getZhuxianSword(player);
                if (sword.isEmpty()) return;

                ZhuxianSword item = (ZhuxianSword) sword.getItem();
                String key = message.skillKey;

                // 验证技能键
                if (isValidSkillKey(key)) {
                    if (key.equals(ZhuxianSword.NBT_FORMATION_ACTIVE)) {
                        // 剑阵需要绝仙形态
                        if (item.getForm(sword) == ZhuxianSword.SwordForm.JUEXIAN) {
                            item.setFormationActive(sword, !item.isFormationActive(sword));
                        }
                    } else {
                        item.toggleSkill(sword, key);
                    }
                }
            });

            return null;
        }

        private boolean isValidSkillKey(String key) {
            return key != null && (
                key.equals(ZhuxianSword.NBT_SKILL_TIANXIN) ||
                key.equals(ZhuxianSword.NBT_SKILL_LIMING) ||
                key.equals(ZhuxianSword.NBT_SKILL_JUEXUE) ||
                key.equals(ZhuxianSword.NBT_SKILL_TAIPING) ||
                key.equals(ZhuxianSword.NBT_FORMATION_ACTIVE)
            );
        }
    }
}
