package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 玩家状态条件
 *
 * 检查玩家的各种状态（潜行、冲刺、空中、水中等）。
 */
public class PlayerStateCondition implements ISynergyCondition {

    public enum StateType {
        SNEAKING("潜行中"),
        SPRINTING("冲刺中"),
        IN_AIR("空中"),
        ON_GROUND("地面"),
        IN_WATER("水中"),
        IN_LAVA("岩浆中"),
        BURNING("燃烧中"),
        STANDING_STILL("站桩中"),
        WET("潮湿"),
        INVISIBLE("隐身中"),
        GLOWING("发光中"),
        ELYTRA_FLYING("鞘翅飞行");

        private final String displayName;

        StateType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final StateType stateType;
    private final boolean requireState;
    private final int standingTicksRequired;  // 用于 STANDING_STILL

    public PlayerStateCondition(StateType stateType, boolean requireState) {
        this.stateType = stateType;
        this.requireState = requireState;
        this.standingTicksRequired = 30; // 默认 1.5 秒
    }

    public PlayerStateCondition(StateType stateType, boolean requireState, int standingTicks) {
        this.stateType = stateType;
        this.requireState = requireState;
        this.standingTicksRequired = standingTicks;
    }

    @Override
    public boolean test(SynergyContext context) {
        EntityPlayer player = context.getPlayer();
        boolean hasState = checkState(player, context);
        return requireState == hasState;
    }

    private boolean checkState(EntityPlayer player, SynergyContext context) {
        switch (stateType) {
            case SNEAKING:
                return player.isSneaking();
            case SPRINTING:
                return player.isSprinting();
            case IN_AIR:
                return !player.onGround;
            case ON_GROUND:
                return player.onGround;
            case IN_WATER:
                return player.isInWater();
            case IN_LAVA:
                return player.isInLava();
            case BURNING:
                return player.isBurning();
            case STANDING_STILL:
                SynergyPlayerState state = SynergyPlayerState.get(player);
                return state.isStandingStill(standingTicksRequired);
            case WET:
                return player.isWet();
            case INVISIBLE:
                return player.isInvisible();
            case GLOWING:
                return player.isGlowing();
            case ELYTRA_FLYING:
                return player.isElytraFlying();
            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        String prefix = requireState ? "" : "Not ";
        return prefix + stateType.getDisplayName();
    }

    // ==================== 静态工厂方法 ====================

    public static PlayerStateCondition isSneaking() {
        return new PlayerStateCondition(StateType.SNEAKING, true);
    }

    public static PlayerStateCondition isSprinting() {
        return new PlayerStateCondition(StateType.SPRINTING, true);
    }

    public static PlayerStateCondition isInAir() {
        return new PlayerStateCondition(StateType.IN_AIR, true);
    }

    public static PlayerStateCondition isOnGround() {
        return new PlayerStateCondition(StateType.ON_GROUND, true);
    }

    public static PlayerStateCondition isInWater() {
        return new PlayerStateCondition(StateType.IN_WATER, true);
    }

    public static PlayerStateCondition isStandingStill(int ticks) {
        return new PlayerStateCondition(StateType.STANDING_STILL, true, ticks);
    }

    public static PlayerStateCondition isStandingStill() {
        return new PlayerStateCondition(StateType.STANDING_STILL, true);
    }

    public static PlayerStateCondition not(StateType stateType) {
        return new PlayerStateCondition(stateType, false);
    }
}
