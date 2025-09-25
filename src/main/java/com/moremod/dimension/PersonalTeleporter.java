package com.moremod.dimension;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/**
 * 私人维度传送器
 */
public class PersonalTeleporter extends Teleporter {

    private final BlockPos targetPos;

    public PersonalTeleporter(WorldServer world, BlockPos targetPos) {
        super(world);
        this.targetPos = targetPos;
    }

    @Override
    public void placeInPortal(Entity entity, float rotationYaw) {
        entity.setPosition(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
        placeInPortal(entity, rotationYaw);
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return true;
    }
}