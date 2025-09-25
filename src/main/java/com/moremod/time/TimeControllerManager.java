package com.moremod.time;

import com.moremod.tile.TileEntityTimeController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeControllerManager {

    public static final TimeControllerManager INSTANCE = new TimeControllerManager();

    // 每个维度 -> (方块坐标 -> 令牌)
    private final Map<Integer, Map<BlockPos, Token>> tokensByDim = new ConcurrentHashMap<>();

    // 令牌存活的 Tick（TE 每 tick 心跳续命；若掉线则过期）
    private static final int TTL_TICKS = 6;

    public static class Token {
        public final BlockPos pos;
        public TileEntityTimeController.Mode mode;
        public int speedLevel;
        public long pausedWorldTime;   // 仅 PAUSE 用；其它模式可设 -1
        public int ttl;

        public Token(BlockPos pos, TileEntityTimeController.Mode mode, int speed, long pausedWorldTime) {
            this.pos = pos;
            this.mode = mode;
            this.speedLevel = speed;
            this.pausedWorldTime = pausedWorldTime;
            this.ttl = TTL_TICKS;
        }
    }

    private TimeControllerManager() {}

    public void heartbeat(WorldServer ws, BlockPos pos,
                          TileEntityTimeController.Mode mode,
                          int speedLevel,
                          long pausedWorldTime) {
        int dim = ws.provider.getDimension();
        Map<BlockPos, Token> dimMap = tokensByDim.computeIfAbsent(dim, d -> new ConcurrentHashMap<>());
        Token t = dimMap.get(pos);
        if (t == null) {
            t = new Token(pos, mode, speedLevel, pausedWorldTime);
            dimMap.put(pos, t);
        } else {
            t.mode = mode;
            t.speedLevel = speedLevel;
            t.pausedWorldTime = pausedWorldTime;
            t.ttl = TTL_TICKS;
        }
    }

    public void remove(WorldServer ws, BlockPos pos) {
        Map<BlockPos, Token> dimMap = tokensByDim.get(ws.provider.getDimension());
        if (dimMap != null) dimMap.remove(pos);
    }

    /** 在世界 tick(END) 调用：递减 TTL、清理过期，并选择一个“生效令牌”。 */
    public Token tickAndPick(WorldServer ws) {
        int dim = ws.provider.getDimension();
        Map<BlockPos, Token> dimMap = tokensByDim.get(dim);
        if (dimMap == null || dimMap.isEmpty()) return null;

        dimMap.values().removeIf(t -> {
            t.ttl--;
            return t.ttl <= 0;
        });
        if (dimMap.isEmpty()) return null;

        // 选择优先级最高的控制器；同优先级取速度更高的
        return dimMap.values().stream()
                .max(Comparator.<Token>comparingInt(t -> priority(t.mode))
                        .thenComparingInt(t -> t.speedLevel))
                .orElse(null);
    }

    /** 模式优先级：PAUSE > DAY_ONLY > NIGHT_ONLY > REVERSE > SLOW > ACCELERATE */
    private static int priority(TileEntityTimeController.Mode m) {
        switch (m) {
            case PAUSE:      return 6;
            case DAY_ONLY:   return 5;
            case NIGHT_ONLY: return 4;
            case REVERSE:    return 3;
            case SLOW:       return 2;
            case ACCELERATE: return 1;
            default:         return 0;
        }
    }
}
