package com.moremod.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityTimeController extends TileEntity implements ITickable {

    public enum Mode {
        ACCELERATE("加速"),
        SLOW("减速"),
        PAUSE("暂停"),
        REVERSE("倒流"),
        DAY_ONLY("永昼"),
        NIGHT_ONLY("永夜");

        private final String zhName;
        Mode(String zhName) {
            this.zhName = zhName;
        }
        public String zhName() {
            return zhName;
        }
    }

    private boolean active = false;
    private Mode mode = Mode.PAUSE;
    private int speedLevel = 0;
    private int tickCounter = 0;
    private long pausedWorldTime = -1;
    private long pausedTotalTime = -1;
    private boolean wasDaylightCycle = true;

    // 替换 update()：不直接 setWorldTime，改为心跳
    @Override
    public void update() {
        if (world == null || world.isRemote || !active) return;
        if (!(world instanceof net.minecraft.world.WorldServer)) return;

        net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) world;

        long paused = -1;
        if (mode == Mode.PAUSE) {
            if (pausedWorldTime < 0) {
                pausedWorldTime = ws.getWorldTime();
                pausedTotalTime = ws.getWorldInfo().getWorldTotalTime();
            }
            paused = pausedWorldTime;
        } else {
            pausedWorldTime = -1;
            pausedTotalTime = -1;
        }

        // 上报令牌（心跳），TTL 会在 TimeControllerManager 中续命
        com.moremod.time.TimeControllerManager.INSTANCE.heartbeat(
                ws, pos, mode, speedLevel, paused
        );
    }

    // 方块被移除/区块卸载时，删除令牌（可选）
    public void invalidate() {
        if (!world.isRemote && world instanceof net.minecraft.world.WorldServer) {
            com.moremod.time.TimeControllerManager.INSTANCE.remove(
                    (net.minecraft.world.WorldServer) world, pos
            );
        }
        super.invalidate();
    }


    private void executeTimeControl(WorldServer worldServer) {
        WorldInfo worldInfo = worldServer.getWorldInfo();

        // 保存原始昼夜循环状态
        if (!active && wasDaylightCycle != world.getGameRules().getBoolean("doDaylightCycle")) {
            wasDaylightCycle = world.getGameRules().getBoolean("doDaylightCycle");
        }

        switch (mode) {
            case PAUSE:
                handlePause(worldServer, worldInfo);
                break;
            case ACCELERATE:
                handleAccelerate(worldServer, worldInfo);
                break;
            case SLOW:
                handleSlow(worldServer, worldInfo);
                break;
            case REVERSE:
                handleReverse(worldServer, worldInfo);
                break;
            case DAY_ONLY:
                handleDayOnly(worldServer, worldInfo);
                break;
            case NIGHT_ONLY:
                handleNightOnly(worldServer, worldInfo);
                break;
        }

        // 强制同步到所有客户端
        if (tickCounter % 20 == 0) {
            syncTimeToClients(worldServer);
        }
    }

    private void handlePause(WorldServer worldServer, WorldInfo worldInfo) {
        // 禁用原版昼夜循环
        if (world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }

        // 首次暂停时记录时间
        if (pausedWorldTime < 0) {
            pausedWorldTime = worldInfo.getWorldTime();
            pausedTotalTime = worldInfo.getWorldTotalTime();
        }

        // 强制设置时间 - 关键：需要直接操作WorldInfo的内部时间
        setWorldTimeDirectly(worldServer, pausedWorldTime, pausedTotalTime);
    }

    private void handleAccelerate(WorldServer worldServer, WorldInfo worldInfo) {
        pausedWorldTime = -1;
        pausedTotalTime = -1;

        // 启用昼夜循环以获得基础速度
        if (!world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "true");
        }

        // 额外增加时间
        long currentTime = worldInfo.getWorldTime();
        long totalTime = worldInfo.getWorldTotalTime();
        int acceleration = speedLevel * speedLevel * 2;

        setWorldTimeDirectly(worldServer, currentTime + acceleration, totalTime + acceleration);
    }

    private void handleSlow(WorldServer worldServer, WorldInfo worldInfo) {
        pausedWorldTime = -1;
        pausedTotalTime = -1;

        // 禁用原版昼夜循环
        if (world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }

        // 减速：只在特定间隔推进时间
        tickCounter++;
        if (tickCounter >= (speedLevel * 4)) {
            long currentTime = worldInfo.getWorldTime();
            long totalTime = worldInfo.getWorldTotalTime();
            setWorldTimeDirectly(worldServer, currentTime + 1, totalTime + 1);
            tickCounter = 0;
        }
    }

    private void handleReverse(WorldServer worldServer, WorldInfo worldInfo) {
        pausedWorldTime = -1;
        pausedTotalTime = -1;

        // 禁用原版昼夜循环
        if (world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }

        // 时间倒流
        long currentTime = worldInfo.getWorldTime();
        long totalTime = worldInfo.getWorldTotalTime();
        int reverseSpeed = speedLevel * speedLevel;

        long newTime = currentTime - reverseSpeed;
        // 处理循环
        if (newTime < 0) {
            newTime = 24000L + (newTime % 24000L);
        }

        setWorldTimeDirectly(worldServer, newTime, Math.max(0, totalTime - reverseSpeed));
    }

    private void handleDayOnly(WorldServer worldServer, WorldInfo worldInfo) {
        pausedWorldTime = -1;
        pausedTotalTime = -1;

        // 禁用原版昼夜循环
        if (world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }

        // 保持在正午
        long dayTime = 6000L;
        long currentTime = worldInfo.getWorldTime();
        long currentDay = (currentTime / 24000L) * 24000L;

        setWorldTimeDirectly(worldServer, currentDay + dayTime, worldInfo.getWorldTotalTime());
    }

    private void handleNightOnly(WorldServer worldServer, WorldInfo worldInfo) {
        pausedWorldTime = -1;
        pausedTotalTime = -1;

        // 禁用原版昼夜循环
        if (world.getGameRules().getBoolean("doDaylightCycle")) {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }

        // 保持在午夜
        long nightTime = 18000L;
        long currentTime = worldInfo.getWorldTime();
        long currentDay = (currentTime / 24000L) * 24000L;

        setWorldTimeDirectly(worldServer, currentDay + nightTime, worldInfo.getWorldTotalTime());
    }

    /**
     * 直接设置世界时间 - 这是关键方法
     * 参考AS的做法，需要同时更新多个地方
     */
    private void setWorldTimeDirectly(WorldServer worldServer, long worldTime, long totalTime) {
        WorldInfo worldInfo = worldServer.getWorldInfo();

        // 1. 更新WorldInfo（持久化数据）
        worldInfo.setWorldTime(worldTime);
        worldInfo.setWorldTotalTime(totalTime);

        // 2. 更新World对象（运行时数据）
        worldServer.setWorldTime(worldTime);

        // 3. 如果是主世界，更新所有维度
        if (worldServer.provider.getDimension() == 0) {
            for (WorldServer otherWorld : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
                if (otherWorld != worldServer && otherWorld.provider.getDimension() != 0) {
                    otherWorld.setWorldTime(worldTime);
                }
            }
        }
    }

    /**
     * 强制同步时间到所有客户端
     */
    private void syncTimeToClients(WorldServer worldServer) {
        long worldTime = worldServer.getWorldTime();
        long totalTime = worldServer.getTotalWorldTime();

        // 发送时间更新包到所有玩家
        SPacketTimeUpdate packet = new SPacketTimeUpdate(totalTime, worldTime,
                world.getGameRules().getBoolean("doDaylightCycle"));

        worldServer.getMinecraftServer().getPlayerList().sendPacketToAllPlayers(packet);
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;

            if (!active) {
                // 关闭时重置
                pausedWorldTime = -1;
                pausedTotalTime = -1;
                tickCounter = 0;

                // 恢复原始昼夜循环状态
                if (!world.isRemote) {
                    world.getGameRules().setOrCreateGameRule("doDaylightCycle",
                            String.valueOf(wasDaylightCycle));
                }
            }

            markDirty();
        }
    }

    public void setSpeedLevel(int level) {
        int clamped = Math.max(0, Math.min(15, level));
        if (this.speedLevel != clamped) {
            this.speedLevel = clamped;
            markDirty();
        }
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void cycleMode() {
        Mode[] modes = Mode.values();
        mode = modes[(mode.ordinal() + 1) % modes.length];
        tickCounter = 0;
        pausedWorldTime = -1;
        pausedTotalTime = -1;
        markDirty();
    }

    public String getModeName() {
        return mode.zhName() + " (等级: " + speedLevel + ")";
    }



    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("Active", active);
        nbt.setInteger("Speed", speedLevel);
        nbt.setInteger("Mode", mode.ordinal());
        nbt.setInteger("TickCounter", tickCounter);
        nbt.setLong("PausedWorldTime", pausedWorldTime);
        nbt.setLong("PausedTotalTime", pausedTotalTime);
        nbt.setBoolean("WasDaylightCycle", wasDaylightCycle);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.active = nbt.getBoolean("Active");
        this.speedLevel = nbt.getInteger("Speed");
        this.tickCounter = nbt.getInteger("TickCounter");
        this.pausedWorldTime = nbt.getLong("PausedWorldTime");
        this.pausedTotalTime = nbt.getLong("PausedTotalTime");
        this.wasDaylightCycle = nbt.getBoolean("WasDaylightCycle");

        int m = nbt.getInteger("Mode");
        if (m >= 0 && m < Mode.values().length) {
            this.mode = Mode.values()[m];
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}