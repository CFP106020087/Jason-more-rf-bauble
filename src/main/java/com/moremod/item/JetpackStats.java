package com.moremod.item.jetpack;

public class JetpackStats {
    public final String registryName;
    public final int maxEnergy;
    public final int energyPerTick;
    public final double ascendSpeed;
    public final double moveSpeed;

    public JetpackStats(String registryName, int maxEnergy, int energyPerTick, double ascendSpeed, double moveSpeed) {
        this.registryName = registryName;
        this.maxEnergy = maxEnergy;
        this.energyPerTick = energyPerTick;
        this.ascendSpeed = ascendSpeed;
        this.moveSpeed = moveSpeed;
    }
}