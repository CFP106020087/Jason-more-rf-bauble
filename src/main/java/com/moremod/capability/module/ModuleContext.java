package com.moremod.capability.module;

import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 模块执行上下文
 *
 * 封装模块执行时需要的环境信息
 */
public class ModuleContext {

    private final World world;
    private final long worldTime;
    private final Side side;

    public ModuleContext(World world, long worldTime, Side side) {
        this.world = world;
        this.worldTime = worldTime;
        this.side = side;
    }

    public World getWorld() {
        return world;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public Side getSide() {
        return side;
    }

    public boolean isRemote() {
        return world.isRemote;
    }

    public boolean isServer() {
        return !world.isRemote;
    }
}
