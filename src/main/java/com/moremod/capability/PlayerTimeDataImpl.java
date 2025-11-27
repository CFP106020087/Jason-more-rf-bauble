package com.moremod.capability;

public class PlayerTimeDataImpl implements IPlayerTimeData {

    private int totalDaysPlayed = 0;  // 佩戴的天数
    private long lastLoginTime = 0;
    public long totalPlayTime = 0;   // 佩戴的总tick数
    private boolean hasEquippedTemporalHeart = false;  // 是否正在装备
    private boolean firstTimeEquip = true;  // 是否是第一次装备

    @Override
    public int getTotalDaysPlayed() {
        return totalDaysPlayed;
    }

    @Override
    public void setTotalDaysPlayed(int days) {
        this.totalDaysPlayed = days;
    }

    @Override
    public long getLastLoginTime() {
        return lastLoginTime;
    }

    @Override
    public void setLastLoginTime(long time) {
        this.lastLoginTime = time;
    }

    @Override
    public long getTotalPlayTime() {
        return totalPlayTime;
    }

    @Override
    public void setTotalPlayTime(long time) {
        this.totalPlayTime = time;
    }

    @Override
    public boolean hasEquippedTemporalHeart() {
        return hasEquippedTemporalHeart;
    }

    @Override
    public void setHasEquippedTemporalHeart(boolean equipped) {
        this.hasEquippedTemporalHeart = equipped;
    }

    @Override
    public boolean isFirstTimeEquip() {
        return firstTimeEquip;
    }

    @Override
    public void setFirstTimeEquip(boolean first) {
        this.firstTimeEquip = first;
    }

    @Override
    public void addPlayTime(long ticks) {
        this.totalPlayTime += ticks;
        // 24000 ticks = 1 MC天
        // 测试时可以改为 1200 让时间过得更快（1分钟 = 1天）
        this.totalDaysPlayed = (int) (this.totalPlayTime / 24000);
        // this.totalDaysPlayed = (int) (this.totalPlayTime / 1200);  // 测试用
    }

    @Override
    public void copyFrom(IPlayerTimeData other) {
        this.totalDaysPlayed = other.getTotalDaysPlayed();
        this.lastLoginTime = other.getLastLoginTime();
        this.totalPlayTime = other.getTotalPlayTime();
        this.hasEquippedTemporalHeart = other.hasEquippedTemporalHeart();
        this.firstTimeEquip = other.isFirstTimeEquip();
    }
}