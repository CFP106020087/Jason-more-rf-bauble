package com.moremod.time;

import com.moremod.moremod;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = moremod.MODID)
public class TimeControllerTicker {

    // 是否跨维度统一（若 true，会把 gamerule 与时间一起推到所有维度）
    private static final boolean GLOBAL_TIME = false;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.world.isRemote || !(e.world instanceof WorldServer)) {
            return;
        }
        WorldServer ws = (WorldServer) e.world;

        // 选出当前维度“最终生效”的控制令牌（参考 Lucerna 的 TokenMap 思路）
        TimeControllerManager.Token token = TimeControllerManager.INSTANCE.tickAndPick(ws);
        if (token == null) return;

        applyControl(ws, token);
    }

    private static void applyControl(WorldServer ws, TimeControllerManager.Token t) {
        WorldInfo wi = ws.getWorldInfo();

        switch (t.mode) {
            case PAUSE:
                setDoDaylightCycle(ws, false);
                long paused = t.pausedWorldTime >= 0 ? t.pausedWorldTime : wi.getWorldTime();
                setTime(ws, paused, wi.getWorldTotalTime());
                break;

            case DAY_ONLY:
                setDoDaylightCycle(ws, false);
                long dayBase = (wi.getWorldTime() / 24000L) * 24000L;
                setTime(ws, dayBase + 6000L, wi.getWorldTotalTime());
                break;

            case NIGHT_ONLY:
                setDoDaylightCycle(ws, false);
                long nightBase = (wi.getWorldTime() / 24000L) * 24000L;
                setTime(ws, nightBase + 18000L, wi.getWorldTotalTime());
                break;

            case REVERSE: {
                setDoDaylightCycle(ws, false);
                int step = Math.max(1, t.speedLevel * t.speedLevel);
                long newT = mod24000(wi.getWorldTime() - step);
                long newTotal = Math.max(0, wi.getWorldTotalTime() - step);
                setTime(ws, newT, newTotal);
                break;
            }

            case SLOW: {
                setDoDaylightCycle(ws, false);
                int interval = Math.max(2, 20 - t.speedLevel); // 越高越快
                if (ws.getTotalWorldTime() % interval == 0) {
                    setTime(ws, wi.getWorldTime() + 1, wi.getWorldTotalTime() + 1);
                }
                break;
            }

            case ACCELERATE: {
                setDoDaylightCycle(ws, true);
                int extra = t.speedLevel * t.speedLevel * 2; // 0,2,8,18...
                // 可选：最低额外速度
                // if (extra == 0) extra = 1;
                setTime(ws, wi.getWorldTime() + extra, wi.getWorldTotalTime() + extra);
                break;
            }
        }

        // 节流同步到客户端：每 10 tick
        if (ws.getTotalWorldTime() % 10 == 0) {
            syncTimeToClients(ws);
        }
    }

    private static long mod24000(long v) {
        long m = v % 24000L;
        if (m < 0) m += 24000L;
        return m;
    }

    private static void setDoDaylightCycle(WorldServer ws, boolean enable) {
        if (GLOBAL_TIME) {
            for (WorldServer w : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
                if (w.getGameRules().getBoolean("doDaylightCycle") != enable) {
                    w.getGameRules().setOrCreateGameRule("doDaylightCycle", String.valueOf(enable));
                }
            }
        } else {
            if (ws.getGameRules().getBoolean("doDaylightCycle") != enable) {
                ws.getGameRules().setOrCreateGameRule("doDaylightCycle", String.valueOf(enable));
            }
        }
    }

    private static void setTime(WorldServer ws, long worldTime, long totalTime) {
        if (GLOBAL_TIME) {
            for (WorldServer w : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
                w.getWorldInfo().setWorldTime(worldTime);
                w.getWorldInfo().setWorldTotalTime(totalTime);
                w.setWorldTime(worldTime);
            }
        } else {
            ws.getWorldInfo().setWorldTime(worldTime);
            ws.getWorldInfo().setWorldTotalTime(totalTime);
            ws.setWorldTime(worldTime);
        }
    }

    private static void syncTimeToClients(WorldServer ws) {
        boolean cycle = ws.getGameRules().getBoolean("doDaylightCycle");
        long t = ws.getWorldTime();
        long T = ws.getWorldInfo().getWorldTotalTime();
        SPacketTimeUpdate pkt = new SPacketTimeUpdate(T, t, cycle);
        if (GLOBAL_TIME) {
            FMLCommonHandler.instance().getMinecraftServerInstance()
                    .getPlayerList().sendPacketToAllPlayers(pkt);
        } else {
            ws.getMinecraftServer().getPlayerList().sendPacketToAllPlayers(pkt);
        }
    }
}
