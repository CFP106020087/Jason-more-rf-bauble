package com.moremod.item;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 玩家行為數據
 */
public class BehaviorData {
    
    // ===== 戰鬥數據 =====
    private int mobKills = 0;           // 擊殺怪物數
    private int playerKills = 0;        // 擊殺玩家數
    private double damageTaken = 0;     // 受到的傷害
    private double damageDealt = 0;     // 造成的傷害
    
    // ===== 建築數據 =====
    private int blocksPlaced = 0;       // 放置方塊數
    private int craftingCount = 0;      // 合成次數
    
    // ===== 採礦數據 =====
    private int blocksMined = 0;        // 挖掘方塊數
    private int oresMined = 0;          // 挖掘礦物數
    
    // ===== 探索數據 =====
    private double distanceTraveled = 0; // 移動距離
    private int dimensionChanges = 0;    // 維度切換次數
    
    // ===== 農業數據 =====
    private int cropsHarvested = 0;     // 收穫作物數
    private int animalsBreed = 0;       // 繁殖動物數
    
    // ===== 其他數據 =====
    private int deaths = 0;             // 死亡次數
    private long playTime = 0;          // 遊玩時間（ticks）
    
    // ===== Getters =====
    public int getMobKills() { return mobKills; }
    public int getPlayerKills() { return playerKills; }
    public double getDamageTaken() { return damageTaken; }
    public double getDamageDealt() { return damageDealt; }
    public int getBlocksPlaced() { return blocksPlaced; }
    public int getCraftingCount() { return craftingCount; }
    public int getBlocksMined() { return blocksMined; }
    public int getOresMined() { return oresMined; }
    public double getDistanceTraveled() { return distanceTraveled; }
    public int getDimensionChanges() { return dimensionChanges; }
    public int getCropsHarvested() { return cropsHarvested; }
    public int getAnimalsBreed() { return animalsBreed; }
    public int getDeaths() { return deaths; }
    public long getPlayTime() { return playTime; }
    
    // ===== 增加數據 =====
    public void addMobKill() { mobKills++; }
    public void addPlayerKill() { playerKills++; }
    public void addDamageTaken(double amount) { damageTaken += amount; }
    public void addDamageDealt(double amount) { damageDealt += amount; }
    public void addBlockPlaced() { blocksPlaced++; }
    public void addCrafting() { craftingCount++; }
    public void addBlockMined() { blocksMined++; }
    public void addOreMined() { oresMined++; }
    public void addDistance(double distance) { distanceTraveled += distance; }
    public void addDimensionChange() { dimensionChanges++; }
    public void addCropHarvest() { cropsHarvested++; }
    public void addAnimalBreed() { animalsBreed++; }
    public void addDeath() { deaths++; }
    public void addPlayTime(long ticks) { playTime += ticks; }
    
    // ===== NBT 序列化 =====
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("MobKills", mobKills);
        nbt.setInteger("PlayerKills", playerKills);
        nbt.setDouble("DamageTaken", damageTaken);
        nbt.setDouble("DamageDealt", damageDealt);
        nbt.setInteger("BlocksPlaced", blocksPlaced);
        nbt.setInteger("CraftingCount", craftingCount);
        nbt.setInteger("BlocksMined", blocksMined);
        nbt.setInteger("OresMined", oresMined);
        nbt.setDouble("DistanceTraveled", distanceTraveled);
        nbt.setInteger("DimensionChanges", dimensionChanges);
        nbt.setInteger("CropsHarvested", cropsHarvested);
        nbt.setInteger("AnimalsBreed", animalsBreed);
        nbt.setInteger("Deaths", deaths);
        nbt.setLong("PlayTime", playTime);
    }
    
    public void readFromNBT(NBTTagCompound nbt) {
        mobKills = nbt.getInteger("MobKills");
        playerKills = nbt.getInteger("PlayerKills");
        damageTaken = nbt.getDouble("DamageTaken");
        damageDealt = nbt.getDouble("DamageDealt");
        blocksPlaced = nbt.getInteger("BlocksPlaced");
        craftingCount = nbt.getInteger("CraftingCount");
        blocksMined = nbt.getInteger("BlocksMined");
        oresMined = nbt.getInteger("OresMined");
        distanceTraveled = nbt.getDouble("DistanceTraveled");
        dimensionChanges = nbt.getInteger("DimensionChanges");
        cropsHarvested = nbt.getInteger("CropsHarvested");
        animalsBreed = nbt.getInteger("AnimalsBreed");
        deaths = nbt.getInteger("Deaths");
        playTime = nbt.getLong("PlayTime");
    }
    
    // ===== 重置數據（用於新週期） =====
    public void reset() {
        mobKills = 0;
        playerKills = 0;
        damageTaken = 0;
        damageDealt = 0;
        blocksPlaced = 0;
        craftingCount = 0;
        blocksMined = 0;
        oresMined = 0;
        distanceTraveled = 0;
        dimensionChanges = 0;
        cropsHarvested = 0;
        animalsBreed = 0;
        deaths = 0;
        playTime = 0;
    }
    
    // ===== 衰減數據（保留一定比例） =====
    public void decay(double factor) {
        mobKills = (int)(mobKills * factor);
        playerKills = (int)(playerKills * factor);
        damageTaken *= factor;
        damageDealt *= factor;
        blocksPlaced = (int)(blocksPlaced * factor);
        craftingCount = (int)(craftingCount * factor);
        blocksMined = (int)(blocksMined * factor);
        oresMined = (int)(oresMined * factor);
        distanceTraveled *= factor;
        dimensionChanges = (int)(dimensionChanges * factor);
        cropsHarvested = (int)(cropsHarvested * factor);
        animalsBreed = (int)(animalsBreed * factor);
    }
    
    @Override
    public String toString() {
        return String.format(
            "BehaviorData[戰鬥:%d/%d, 建築:%d, 採礦:%d/%d, 探索:%.1f, 農業:%d/%d]",
            mobKills, playerKills, blocksPlaced, blocksMined, oresMined,
            distanceTraveled, cropsHarvested, animalsBreed
        );
    }
}
