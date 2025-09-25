package com.moremod.curse;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CurseManager {

    private static final String CURSE_NBT_KEY = "moremodCursedKnightCurse";
    private static final UUID CURSE_UUID = UUID.fromString("c0fee-c0de-dead-beef-123456789abc");
    private static Map<UUID, Integer> playerCurseStacks = new HashMap<>();

    public static void applyCurse(EntityPlayer player) {
        UUID playerUUID = player.getUniqueID();
        int currentStacks = playerCurseStacks.getOrDefault(playerUUID, 0);
        currentStacks++;
        playerCurseStacks.put(playerUUID, currentStacks);

        // 更新玩家的最大生命值
        updatePlayerMaxHealth(player, currentStacks);

        // 保存到NBT
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistent = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persistent.setInteger(CURSE_NBT_KEY, currentStacks);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistent);
    }

    public static void removeCurse(EntityPlayer player) {
        UUID playerUUID = player.getUniqueID();
        playerCurseStacks.remove(playerUUID);

        // 恢复最大生命值
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        AttributeModifier existingModifier = maxHealth.getModifier(CURSE_UUID);
        if (existingModifier != null) {
            maxHealth.removeModifier(existingModifier);
        }

        // 清除NBT数据
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistent = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persistent.removeTag(CURSE_NBT_KEY);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistent);

        player.sendMessage(new TextComponentString("§a诅咒已被净化！"));
    }

    private static void updatePlayerMaxHealth(EntityPlayer player, int curseStacks) {
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);

        // 移除旧的修改器
        AttributeModifier existingModifier = maxHealth.getModifier(CURSE_UUID);
        if (existingModifier != null) {
            maxHealth.removeModifier(existingModifier);
        }

        // 计算减少量（每层诅咒减少5%）
        double reduction = -0.05 * curseStacks;

        // 添加新的修改器
        AttributeModifier modifier = new AttributeModifier(
                CURSE_UUID,
                "moremod Cursed Knight Curse",
                reduction,
                2 // MULTIPLY_TOTAL操作，百分比减少
        );

        maxHealth.applyModifier(modifier);

        // 确保当前生命值不超过新的最大值
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // 玩家登录时恢复诅咒状态
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return; // 死亡后不保留诅咒
        }

        EntityPlayer player = event.getEntityPlayer();
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistent = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        if (persistent.hasKey(CURSE_NBT_KEY)) {
            int curseStacks = persistent.getInteger(CURSE_NBT_KEY);
            playerCurseStacks.put(player.getUniqueID(), curseStacks);
            updatePlayerMaxHealth(player, curseStacks);
        }
    }
}