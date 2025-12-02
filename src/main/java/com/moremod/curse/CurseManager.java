package com.moremod.entity.curse;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CurseManager {

    private static final String CURSE_NBT_KEY = "moremodCursedKnightCurse";
    private static final UUID CURSE_UUID = UUID.fromString("c0fee-c0de-dead-beef-123456789abc");
    private static Map<UUID, Integer> playerCurseStacks = new HashMap<>();

    // 最大诅咒层数限制
    private static final int MAX_CURSE_STACKS = 10;

    public static void applyCurse(EntityPlayer player) {
        UUID playerUUID = player.getUniqueID();
        int currentStacks = playerCurseStacks.getOrDefault(playerUUID, 0);

        // 限制最大层数
        if (currentStacks >= MAX_CURSE_STACKS) {
            player.sendMessage(new TextComponentString("§4诅咒已达到最大层数！"));
            return;
        }

        currentStacks++;
        playerCurseStacks.put(playerUUID, currentStacks);

        // 更新玩家的最大生命值
        updatePlayerMaxHealth(player, currentStacks);

        // 显示诅咒层数
        player.sendMessage(new TextComponentString("§c诅咒层数: " + currentStacks + "/" + MAX_CURSE_STACKS));

        // 保存到NBT
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistent = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persistent.setInteger(CURSE_NBT_KEY, currentStacks);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistent);
    }

    public static void removeCurse(EntityPlayer player) {
        UUID playerUUID = player.getUniqueID();

        // 检查是否有诅咒
        if (!playerCurseStacks.containsKey(playerUUID) || playerCurseStacks.get(playerUUID) == 0) {
            player.sendMessage(new TextComponentString("§e你身上没有诅咒需要净化。"));
            return;
        }

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

        player.sendMessage(new TextComponentString("§a诅咒已被净化！你的生命值已恢复正常。"));
    }

    public static int getCurseLevel(EntityPlayer player) {
        return playerCurseStacks.getOrDefault(player.getUniqueID(), 0);
    }

    public static boolean isCursed(EntityPlayer player) {
        return getCurseLevel(player) > 0;
    }

    private static void updatePlayerMaxHealth(EntityPlayer player, int curseStacks) {
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);

        // 移除旧的修改器
        AttributeModifier existingModifier = maxHealth.getModifier(CURSE_UUID);
        if (existingModifier != null) {
            maxHealth.removeModifier(existingModifier);
        }

        // 计算减少量（每层诅咒减少5%，最多50%）
        double reduction = Math.min(-0.05 * curseStacks, -0.5);

        // 添加新的修改器
        AttributeModifier modifier = new AttributeModifier(
                CURSE_UUID,
                "moremod Weeping Angel Curse",
                reduction,
                2 // MULTIPLY_TOTAL操作，百分比减少
        );

        maxHealth.applyModifier(modifier);

        // 确保当前生命值不超过新的最大值
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // 玩家克隆事件（用于处理死亡/维度传送）
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 死亡后减少一半诅咒层数（而不是完全清除）
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();

            NBTTagCompound oldData = oldPlayer.getEntityData();
            NBTTagCompound oldPersistent = oldData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

            if (oldPersistent.hasKey(CURSE_NBT_KEY)) {
                int oldStacks = oldPersistent.getInteger(CURSE_NBT_KEY);
                int newStacks = oldStacks / 2; // 死亡后保留一半诅咒

                if (newStacks > 0) {
                    playerCurseStacks.put(newPlayer.getUniqueID(), newStacks);
                    updatePlayerMaxHealth(newPlayer, newStacks);

                    NBTTagCompound newData = newPlayer.getEntityData();
                    NBTTagCompound newPersistent = newData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
                    newPersistent.setInteger(CURSE_NBT_KEY, newStacks);
                    newData.setTag(EntityPlayer.PERSISTED_NBT_TAG, newPersistent);

                    newPlayer.sendMessage(new TextComponentString("§c死亡减轻了诅咒，但仍有 " + newStacks + " 层诅咒残留。"));
                }
            }
        } else {
            // 维度传送时保留诅咒
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

    // 玩家登录事件
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persistent = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        if (persistent.hasKey(CURSE_NBT_KEY)) {
            int curseStacks = persistent.getInteger(CURSE_NBT_KEY);
            playerCurseStacks.put(player.getUniqueID(), curseStacks);
            updatePlayerMaxHealth(player, curseStacks);
            player.sendMessage(new TextComponentString("§c警告：你身上仍有 " + curseStacks + " 层诅咒！"));
        }
    }

    // 玩家重生事件
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        UUID playerUUID = player.getUniqueID();

        if (playerCurseStacks.containsKey(playerUUID)) {
            int curseStacks = playerCurseStacks.get(playerUUID);
            updatePlayerMaxHealth(player, curseStacks);
        }
    }
}