package com.moremod.capability;

import com.moremod.network.PacketSyncPlayerTime;
import com.moremod.network.PacketHandler;
import com.moremod.item.ItemTemporalHeart;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

public class PlayerTimeDataCapability {

    @CapabilityInject(IPlayerTimeData.class)
    public static final Capability<IPlayerTimeData> PLAYER_TIME_CAP = null;

    public static IPlayerTimeData get(EntityPlayer player) {
        if (player.hasCapability(PLAYER_TIME_CAP, null)) {
            return player.getCapability(PLAYER_TIME_CAP, null);
        }
        return null;
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation("moremod", "player_time"),
                    new PlayerTimeDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 无论是死亡还是跨维度传送（如末地传送门返回主世界）都要复制数据
        // event.isWasDeath() == false 时表示维度切换
        IPlayerTimeData oldData = get(event.getOriginal());
        IPlayerTimeData newData = get(event.getEntityPlayer());
        if (oldData != null && newData != null) {
            newData.copyFrom(oldData);
        }
    }

    /**
     * 核心方法 - 只在佩戴饰品时增加时间
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote) {
            EntityPlayer player = event.player;
            IPlayerTimeData data = get(player);

            if (data != null) {
                // 检查玩家是否佩戴了时光之心
                boolean isWearing = isWearingTemporalHeart(player);

                if (isWearing) {
                    // 只在佩戴时增加时间
                    data.addPlayTime(1);

                    // 每60秒（1200 ticks）同步一次到客户端
                    if (player.world.getTotalWorldTime() % 1200 == 0) {
                        if (player instanceof EntityPlayerMP) {
                            PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                                    data.getTotalDaysPlayed(),
                                    data.getTotalPlayTime(),
                                    data.hasEquippedTemporalHeart(),
                                    data.getLastLoginTime()
                            );
                            PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) player);

                            // 调试信息
                            // System.out.println("[TemporalHeart] " + player.getName() +
                            //                  " - 佩戴天数: " + data.getTotalDaysPlayed() +
                            //                  " - 佩戴Ticks: " + data.getTotalPlayTime());
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查玩家是否佩戴了时光之心
     */
    private static boolean isWearingTemporalHeart(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemTemporalHeart) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        IPlayerTimeData data = get(event.player);
        if (data != null) {
            data.setLastLoginTime(event.player.world.getTotalWorldTime());

            // 如果玩家曾经装备过时光之心，自动应用永久属性
            if (data.hasEquippedTemporalHeart()) {
                event.player.world.getMinecraftServer().addScheduledTask(() -> {
                    applyPermanentAttributes(event.player, data);
                });
            }

            // 登录时立即同步数据到客户端
            if (event.player instanceof EntityPlayerMP) {
                PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                        data.getTotalDaysPlayed(),
                        data.getTotalPlayTime(),
                        data.hasEquippedTemporalHeart(),
                        data.getLastLoginTime()
                );
                event.player.world.getMinecraftServer().addScheduledTask(() -> {
                    PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) event.player);
                });
            }
        }
    }

    private static void applyPermanentAttributes(EntityPlayer player, IPlayerTimeData data) {
        int totalDays = data.getTotalDaysPlayed();
        double healthBonus = totalDays * 1.5;   // 6倍增强
        double damageBonus = totalDays * 0.6;   // 6倍增强

        UUID PERMANENT_HEALTH_UUID = UUID.fromString("123E4567-E89B-12D3-A456-426614174000");
        UUID PERMANENT_DAMAGE_UUID = UUID.fromString("987FBC97-4BED-5078-AF07-9141BA07C9F3");

        // 移除旧修饰器
        player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .removeModifier(PERMANENT_HEALTH_UUID);
        player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                .removeModifier(PERMANENT_DAMAGE_UUID);

        // 应用新修饰器
        if (healthBonus > 0) {
            AttributeModifier healthModifier = new AttributeModifier(
                    PERMANENT_HEALTH_UUID, "temporal_heart_permanent_health", healthBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                    .applyModifier(healthModifier);
        }

        if (damageBonus > 0) {
            AttributeModifier damageModifier = new AttributeModifier(
                    PERMANENT_DAMAGE_UUID, "temporal_heart_permanent_damage", damageBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .applyModifier(damageModifier);
        }
    }
}