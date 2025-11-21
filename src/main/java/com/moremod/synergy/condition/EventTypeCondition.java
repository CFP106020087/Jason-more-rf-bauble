package com.moremod.synergy.condition;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 事件类型条件 - 检查触发的事件类型
 *
 * 说明：
 * - 用于过滤特定类型的事件
 * - 例如：只在 LivingHurtEvent 中触发
 */
public class EventTypeCondition implements ISynergyCondition {

    private final Set<Class<? extends Event>> allowedEventTypes;

    @SafeVarargs
    public EventTypeCondition(Class<? extends Event>... eventTypes) {
        this.allowedEventTypes = new HashSet<>(Arrays.asList(eventTypes));
    }

    @Override
    public boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (event == null) {
            // 如果没有事件（如在 Tick 中），则不满足条件
            return false;
        }

        // 检查事件类型是否在允许列表中（包含子类）
        for (Class<? extends Event> allowedType : allowedEventTypes) {
            if (allowedType.isInstance(event)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("EventType[");
        boolean first = true;
        for (Class<? extends Event> type : allowedEventTypes) {
            if (!first) sb.append(", ");
            sb.append(type.getSimpleName());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
