package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 消息效果
 *
 * 向玩家发送消息或在 Action Bar 显示文本。
 */
public class MessageEffect implements ISynergyEffect {

    public enum MessageType {
        ACTION_BAR,     // Action Bar 显示
        CHAT,           // 聊天消息
        TITLE,          // 标题显示
        DEBUG           // 调试日志
    }

    private final MessageType type;
    private final String message;
    private final TextFormatting color;

    public MessageEffect(MessageType type, String message, TextFormatting color) {
        this.type = type;
        this.message = message;
        this.color = color != null ? color : TextFormatting.WHITE;
    }

    @Override
    public void apply(SynergyContext context) {
        String formattedMessage = color + message;

        switch (type) {
            case ACTION_BAR:
                context.getPlayer().sendStatusMessage(
                        new TextComponentString(formattedMessage),
                        true  // Action Bar
                );
                break;

            case CHAT:
                context.getPlayer().sendMessage(
                        new TextComponentString(formattedMessage)
                );
                break;

            case TITLE:
                // 简化的标题显示（使用 Action Bar 替代）
                context.getPlayer().sendStatusMessage(
                        new TextComponentString(formattedMessage),
                        true
                );
                break;

            case DEBUG:
                System.out.println("[Synergy] " + message +
                        " - Player: " + context.getPlayer().getName() +
                        " - Modules: " + context.getActiveModuleIds());
                break;
        }
    }

    @Override
    public String getDescription() {
        return "Show message: " + message;
    }

    @Override
    public int getPriority() {
        // 消息效果最后执行
        return 999;
    }

    // ==================== 静态工厂方法 ====================

    public static MessageEffect actionBar(String message) {
        return new MessageEffect(MessageType.ACTION_BAR, message, TextFormatting.GOLD);
    }

    public static MessageEffect actionBar(String message, TextFormatting color) {
        return new MessageEffect(MessageType.ACTION_BAR, message, color);
    }

    public static MessageEffect chat(String message) {
        return new MessageEffect(MessageType.CHAT, message, TextFormatting.WHITE);
    }

    public static MessageEffect chat(String message, TextFormatting color) {
        return new MessageEffect(MessageType.CHAT, message, color);
    }

    public static MessageEffect debug(String message) {
        return new MessageEffect(MessageType.DEBUG, message, null);
    }

    /**
     * 创建 Synergy 触发提示
     */
    public static MessageEffect synergyActivated(String synergyName) {
        return new MessageEffect(
                MessageType.ACTION_BAR,
                "⚡ " + synergyName + " 触发!",
                TextFormatting.LIGHT_PURPLE
        );
    }
}
