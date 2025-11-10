package com.moremod.tile;

import com.moremod.config.SwordMaterialData;
import com.moremod.config.UpgradeConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

/**
 * 寶石拆卸系統
 * 允許玩家花費經驗來拆下鑲嵌在劍上的寶石
 */
public class GemRemovalSystem {

    /**
     * 獲取劍上所有已鑲嵌的寶石信息
     */
    public static class InlayInfo {
        public String materialId;      // 材料 ID (例如: minecraft:diamond)
        public String displayName;      // 顯示名稱
        public int slot;               // 槽位索引 (0-4)
        public int removalCost;        // 拆卸經驗消耗

        public InlayInfo(String materialId, String displayName, int slot, int removalCost) {
            this.materialId = materialId;
            this.displayName = displayName;
            this.slot = slot;
            this.removalCost = removalCost;
        }
    }

    /**
     * 獲取劍上的所有鑲嵌信息
     */
    public static List<InlayInfo> getInlays(ItemStack sword) {
        List<InlayInfo> inlays = new ArrayList<>();

        if (!sword.hasTagCompound()) return inlays;

        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return inlays;

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return inlays;

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");
        int count = inlaysTag.getInteger("Count");

        for (int i = 0; i < count; i++) {
            NBTTagCompound inlay = inlaysTag.getCompoundTag("Inlay_" + i);
            String materialId = inlay.getString("Material");
            String name = inlay.getString("Name");

            // 計算拆卸成本（可以從配置讀取）
            int cost = calculateRemovalCost(materialId, i);

            inlays.add(new InlayInfo(materialId, name, i, cost));
        }

        return inlays;
    }

    /**
     * 計算拆卸成本
     * 基礎成本 = 材料稀有度 + 槽位索引 * 5
     */
    private static int calculateRemovalCost(String materialId, int slotIndex) {
        SwordMaterialData data = UpgradeConfig.getMaterialData(materialId);

        int baseCost = 5; // 基礎消耗

        if (data != null) {
            // 根據攻擊傷害計算稀有度
            baseCost += (int)(data.attackDamage * 2);
        }

        // 越後面的槽位越貴
        baseCost += slotIndex * 5;

        return baseCost;
    }

    /**
     * 拆卸指定槽位的寶石
     * @param sword 要拆卸的劍
     * @param slotIndex 槽位索引
     * @param player 玩家（用於扣除經驗）
     * @return 拆卸結果：[更新後的劍, 拆下的材料物品]
     */
    public static ItemStack[] removeGem(ItemStack sword, int slotIndex, EntityPlayer player) {
        if (!sword.hasTagCompound()) {
            return new ItemStack[]{sword, ItemStack.EMPTY};
        }

        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) {
            return new ItemStack[]{sword, ItemStack.EMPTY};
        }

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) {
            return new ItemStack[]{sword, ItemStack.EMPTY};
        }

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");
        int count = inlaysTag.getInteger("Count");

        if (slotIndex < 0 || slotIndex >= count) {
            return new ItemStack[]{sword, ItemStack.EMPTY};
        }

        // 獲取要拆除的寶石信息
        NBTTagCompound targetInlay = inlaysTag.getCompoundTag("Inlay_" + slotIndex);
        String materialId = targetInlay.getString("Material");

        // 檢查經驗是否足夠
        int cost = calculateRemovalCost(materialId, slotIndex);
        if (player.experienceLevel < cost) {
            player.sendMessage(new TextComponentString("§c經驗不足！需要 " + cost + " 級"));
            return new ItemStack[]{sword, ItemStack.EMPTY};
        }

        // 扣除經驗
        player.addExperienceLevel(-cost);

        // 獲取材料數據並減少屬性
        SwordMaterialData data = UpgradeConfig.getMaterialData(materialId);
        if (data != null) {
            float currentAttack = upgradeData.getFloat("AttackBonus");
            float currentSpeed = upgradeData.getFloat("SpeedBonus");
            upgradeData.setFloat("AttackBonus", currentAttack - data.attackDamage);
            upgradeData.setFloat("SpeedBonus", currentSpeed - data.attackSpeed);

            // 減少額外屬性
            if (data.extraAttributes != null && upgradeData.hasKey("ExtraAttributes")) {
                NBTTagCompound extraAttrs = upgradeData.getCompoundTag("ExtraAttributes");
                for (String key : data.extraAttributes.keySet()) {
                    double current = extraAttrs.getDouble(key);
                    extraAttrs.setDouble(key, current - data.extraAttributes.get(key));
                }
                upgradeData.setTag("ExtraAttributes", extraAttrs);
            }
        }

        // 重組鑲嵌列表（移除指定槽位）
        NBTTagCompound newInlays = new NBTTagCompound();
        int newIndex = 0;
        for (int i = 0; i < count; i++) {
            if (i != slotIndex) {
                NBTTagCompound inlay = inlaysTag.getCompoundTag("Inlay_" + i);
                newInlays.setTag("Inlay_" + newIndex, inlay);
                newIndex++;
            }
        }
        newInlays.setInteger("Count", newIndex);
        upgradeData.setTag("Inlays", newInlays);

        // 更新劍的 NBT
        tag.setTag("SwordUpgrades", upgradeData);
        sword.setTagCompound(tag);

        // 創建返回的材料物品
        ItemStack returnedGem = createItemFromMaterialId(materialId);

        player.sendMessage(new TextComponentString("§a成功拆除寶石！消耗 " + cost + " 級經驗"));

        return new ItemStack[]{sword, returnedGem};
    }

    /**
     * 根據材料 ID 創建物品
     */
    private static ItemStack createItemFromMaterialId(String materialId) {
        // 解析 "minecraft:diamond" 格式
        String[] parts = materialId.split(":");
        if (parts.length != 2) {
            return ItemStack.EMPTY;
        }

        net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(materialId);
        if (item != null) {
            return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    /**
     * 獲取劍的鑲嵌槽位數量
     */
    public static int getInlayCount(ItemStack sword) {
        if (!sword.hasTagCompound()) return 0;

        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return 0;

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return 0;

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");
        return inlaysTag.getInteger("Count");
    }

    /**
     * 檢查劍是否有鑲嵌
     */
    public static boolean hasInlays(ItemStack sword) {
        return getInlayCount(sword) > 0;
    }
}