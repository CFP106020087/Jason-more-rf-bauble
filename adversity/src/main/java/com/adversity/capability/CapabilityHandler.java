package com.adversity.capability;

import com.adversity.Adversity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Capability 注册和管理
 */
public class CapabilityHandler {

    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Adversity.MODID, "adversity_data");
    public static final ResourceLocation PLAYER_DIFFICULTY_ID = new ResourceLocation(Adversity.MODID, "player_difficulty");

    @CapabilityInject(IAdversityCapability.class)
    public static Capability<IAdversityCapability> ADVERSITY_CAPABILITY = null;

    @CapabilityInject(IPlayerDifficulty.class)
    public static Capability<IPlayerDifficulty> PLAYER_DIFFICULTY_CAPABILITY = null;

    /**
     * 注册 Capability
     */
    public static void register() {
        // 怪物词条 Capability
        CapabilityManager.INSTANCE.register(
            IAdversityCapability.class,
            new AdversityCapabilityStorage(),
            AdversityCapability::new
        );

        // 玩家难度 Capability
        CapabilityManager.INSTANCE.register(
            IPlayerDifficulty.class,
            new Capability.IStorage<IPlayerDifficulty>() {
                @Nullable
                @Override
                public NBTBase writeNBT(Capability<IPlayerDifficulty> capability, IPlayerDifficulty instance, EnumFacing side) {
                    return instance.serializeNBT();
                }

                @Override
                public void readNBT(Capability<IPlayerDifficulty> capability, IPlayerDifficulty instance, EnumFacing side, NBTBase nbt) {
                    if (nbt instanceof NBTTagCompound) {
                        instance.deserializeNBT((NBTTagCompound) nbt);
                    }
                }
            },
            PlayerDifficulty::new
        );

        Adversity.LOGGER.info("Adversity Capabilities registered");
    }

    /**
     * 附加 Capability 到实体
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();

        // 玩家 - 附加难度设置
        if (entity instanceof EntityPlayer) {
            event.addCapability(PLAYER_DIFFICULTY_ID, new PlayerDifficultyProvider());
        }

        // EntityLiving（怪物、动物等）- 附加词条数据
        if (entity instanceof EntityLiving) {
            event.addCapability(CAPABILITY_ID, new AdversityCapabilityProvider());
        }
    }

    /**
     * 玩家死亡后复活时保留数据
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            IPlayerDifficulty oldCap = getPlayerDifficulty(event.getOriginal());
            IPlayerDifficulty newCap = getPlayerDifficulty(event.getEntityPlayer());

            if (oldCap != null && newCap != null) {
                newCap.deserializeNBT(oldCap.serializeNBT());
            }
        }
    }

    /**
     * 获取实体的 Adversity Capability
     */
    @Nullable
    public static IAdversityCapability getCapability(Entity entity) {
        if (entity == null || ADVERSITY_CAPABILITY == null) {
            return null;
        }

        if (entity.hasCapability(ADVERSITY_CAPABILITY, null)) {
            return entity.getCapability(ADVERSITY_CAPABILITY, null);
        }

        return null;
    }

    /**
     * 获取玩家的难度设置
     */
    @Nullable
    public static IPlayerDifficulty getPlayerDifficulty(EntityPlayer player) {
        if (player == null || PLAYER_DIFFICULTY_CAPABILITY == null) {
            return null;
        }

        if (player.hasCapability(PLAYER_DIFFICULTY_CAPABILITY, null)) {
            return player.getCapability(PLAYER_DIFFICULTY_CAPABILITY, null);
        }

        return null;
    }

    /**
     * 检查实体是否有 Adversity Capability
     */
    public static boolean hasCapability(Entity entity) {
        return entity != null && ADVERSITY_CAPABILITY != null
            && entity.hasCapability(ADVERSITY_CAPABILITY, null);
    }
}
