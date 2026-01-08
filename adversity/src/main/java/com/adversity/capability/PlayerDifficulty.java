package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 玩家难度设置实现
 */
public class PlayerDifficulty implements IPlayerDifficulty {

    private float difficultyMultiplier = 1.0f;
    private boolean difficultyDisabled = false;
    private int killCount = 0;

    @Override
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    @Override
    public void setDifficultyMultiplier(float multiplier) {
        this.difficultyMultiplier = Math.max(0.0f, Math.min(2.0f, multiplier));
    }

    @Override
    public boolean isDifficultyDisabled() {
        return difficultyDisabled;
    }

    @Override
    public void setDifficultyDisabled(boolean disabled) {
        this.difficultyDisabled = disabled;
    }

    @Override
    public int getKillCount() {
        return killCount;
    }

    @Override
    public void addKill() {
        this.killCount++;
    }

    @Override
    public void resetKillCount() {
        this.killCount = 0;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("difficultyMultiplier", difficultyMultiplier);
        nbt.setBoolean("difficultyDisabled", difficultyDisabled);
        nbt.setInteger("killCount", killCount);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        this.difficultyMultiplier = nbt.hasKey("difficultyMultiplier") ? nbt.getFloat("difficultyMultiplier") : 1.0f;
        this.difficultyDisabled = nbt.getBoolean("difficultyDisabled");
        this.killCount = nbt.getInteger("killCount");
    }
}
