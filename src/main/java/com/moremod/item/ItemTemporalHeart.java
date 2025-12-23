package com.moremod.item;

import com.moremod.capability.IPlayerTimeData;
import com.moremod.capability.PlayerTimeDataCapability;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.network.PacketSyncPlayerTime;
import com.moremod.network.PacketHandler;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemTemporalHeart extends Item implements IBauble {

    // 临时属性修饰器的UUID
    private static final UUID HEALTH_UUID = UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B");
    private static final UUID DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    // 永久属性修饰器的UUID
    private static final UUID PERMANENT_HEALTH_UUID = UUID.fromString("123E4567-E89B-12D3-A456-426614174000");
    private static final UUID PERMANENT_DAMAGE_UUID = UUID.fromString("987FBC97-4BED-5078-AF07-9141BA07C9F3");

    public ItemTemporalHeart() {
        super();
        this.setRegistryName("temporal_heart");
        this.setTranslationKey("temporal_heart");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onWornTick(ItemStack itemStack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            EntityPlayer entityPlayer = (EntityPlayer) player;

            if (player.world.getTotalWorldTime() % 20 == 0) {
                updatePlayerAttributes(entityPlayer);
                spawnParticles(entityPlayer);

                // 每分钟显示状态 (测试用)
                if (player.world.getTotalWorldTime() % 1200 == 0) {
                    IPlayerTimeData timeData = PlayerTimeDataCapability.get(entityPlayer);
                    if (timeData != null) {
                        int days = timeData.getTotalDaysPlayed();
                        double health = days * 0.5;  // 再砍一半
                        double damage = days * 0.2;  // 再砍一半
                        entityPlayer.sendMessage(new TextComponentString(
                                TextFormatting.AQUA + String.format("当前状态 - 佩戴天数: %d, 生命: +%.1f, 攻击: +%.1f",
                                        days, health, damage)));

                        // 佩戴时每分钟强制同步一次
                        if (entityPlayer instanceof EntityPlayerMP) {
                            PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                                    timeData.getTotalDaysPlayed(),
                                    timeData.getTotalPlayTime(),
                                    timeData.hasEquippedTemporalHeart(),
                                    timeData.getLastLoginTime()
                            );
                            PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) entityPlayer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEquipped(ItemStack itemStack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            EntityPlayer entityPlayer = (EntityPlayer) player;

            IPlayerTimeData timeData = PlayerTimeDataCapability.get(entityPlayer);
            if (timeData != null) {
                // 检查是否是第一次装备
                if (timeData.isFirstTimeEquip()) {
                    // 第一次装备：重置数据
                    timeData.setTotalDaysPlayed(0);
                    timeData.setTotalPlayTime(0);
                    timeData.setFirstTimeEquip(false);  // 标记为已装备过
                    timeData.setHasEquippedTemporalHeart(true);

                    entityPlayer.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "归一心元石首次激活！开始记录佩戴时间..."));
                } else {
                    // 不是第一次：继续之前的计时
                    timeData.setHasEquippedTemporalHeart(true);

                    entityPlayer.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "归一心元石重新激活！当前佩戴天数: " +
                                    timeData.getTotalDaysPlayed() + "天"));
                }

                removePermanentAttributes(entityPlayer);

                // 同步数据
                if (entityPlayer instanceof EntityPlayerMP) {
                    PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                            timeData.getTotalDaysPlayed(),
                            timeData.getTotalPlayTime(),
                            timeData.hasEquippedTemporalHeart(),
                            timeData.getLastLoginTime()
                    );
                    PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) entityPlayer);
                }
            }

            updatePlayerAttributes(entityPlayer);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemStack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            EntityPlayer entityPlayer = (EntityPlayer) player;
            removePlayerAttributes(entityPlayer);
            applyPermanentAttributes(entityPlayer);

            // 拿下时也同步数据
            IPlayerTimeData timeData = PlayerTimeDataCapability.get(entityPlayer);
            if (timeData != null && entityPlayer instanceof EntityPlayerMP) {
                PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                        timeData.getTotalDaysPlayed(),
                        timeData.getTotalPlayTime(),
                        timeData.hasEquippedTemporalHeart(),
                        timeData.getLastLoginTime()
                );
                PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) entityPlayer);
            }

            entityPlayer.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "归一心元石的力量已融入你的身体，永远不会消失..."));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
                               List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player != null) {
            IPlayerTimeData timeData = PlayerTimeDataCapability.get(player);
            if (timeData != null) {
                int totalDays = timeData.getTotalDaysPlayed();
                double healthBonus = totalDays * 0.5;   // 再砍一半
                double damageBonus = totalDays * 0.2;   // 再砍一半

                tooltip.add("");
                tooltip.add(TextFormatting.GOLD + "归一心元石");
                tooltip.add(TextFormatting.GRAY + "随着佩戴时间增长而变强的神秘饰品");
                tooltip.add("");
                tooltip.add(TextFormatting.RED + "生命值: +" + String.format("%.1f", healthBonus) + "❤");
                tooltip.add(TextFormatting.BLUE + "攻击力: +" + String.format("%.1f", damageBonus) + "⚔");
                tooltip.add("");
                tooltip.add(TextFormatting.YELLOW + "佩戴天数: " + totalDays + "天");

                if (timeData.isFirstTimeEquip()) {
                    tooltip.add(TextFormatting.GREEN + "状态: 未激活");
                } else if (timeData.hasEquippedTemporalHeart()) {
                    tooltip.add(TextFormatting.GREEN + "状态: 已激活");
                } else {
                    tooltip.add(TextFormatting.GRAY + "状态: 休眠中");
                }

                tooltip.add(TextFormatting.DARK_GRAY + "装备在项链槽位");

                int nextMilestone = getNextMilestone(totalDays);
                if (nextMilestone > 0) {
                    tooltip.add("");
                    tooltip.add(TextFormatting.GREEN + "距离下个里程碑: " +
                            (nextMilestone - totalDays) + "天");
                }

                if (timeData.getTotalPlayTime() > 0) {
                    double realHours = (timeData.getTotalPlayTime() / 20.0) / 3600.0;
                    tooltip.add(TextFormatting.DARK_GRAY + String.format("佩戴时间: %.1f小时", realHours));
                }
            } else {
                tooltip.add("");
                tooltip.add(TextFormatting.GOLD + "归一心元石");
                tooltip.add(TextFormatting.GRAY + "随着佩戴时间增长而变强的神秘饰品");
                tooltip.add("");
                tooltip.add(TextFormatting.YELLOW + "正在加载数据...");
                tooltip.add(TextFormatting.DARK_GRAY + "装备在项链槽位");
            }
        }
    }

    private void updatePlayerAttributes(EntityPlayer player) {
        IPlayerTimeData timeData = PlayerTimeDataCapability.get(player);
        if (timeData == null) return;

        int totalDays = timeData.getTotalDaysPlayed();
        double healthBonus = totalDays * 0.5;   // 再砍一半
        double damageBonus = totalDays * 0.2;   // 再砍一半

        removePlayerAttributes(player);

        if (healthBonus > 0) {
            AttributeModifier healthModifier = new AttributeModifier(
                    HEALTH_UUID, "temporal_heart_health", healthBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                    .applyModifier(healthModifier);
        }

        if (damageBonus > 0) {
            AttributeModifier damageModifier = new AttributeModifier(
                    DAMAGE_UUID, "temporal_heart_damage", damageBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .applyModifier(damageModifier);
        }

        if (healthBonus > 0) {
            player.heal((float) healthBonus);
        }
    }

    private void removePlayerAttributes(EntityPlayer player) {
        player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .removeModifier(HEALTH_UUID);
        player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                .removeModifier(DAMAGE_UUID);
    }

    private void removePermanentAttributes(EntityPlayer player) {
        player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .removeModifier(PERMANENT_HEALTH_UUID);
        player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                .removeModifier(PERMANENT_DAMAGE_UUID);
    }

    private void applyPermanentAttributes(EntityPlayer player) {
        IPlayerTimeData timeData = PlayerTimeDataCapability.get(player);
        if (timeData == null) return;

        int totalDays = timeData.getTotalDaysPlayed();
        double healthBonus = totalDays * 0.5;   // 再砍一半
        double damageBonus = totalDays * 0.2;   // 再砍一半

        removePermanentAttributes(player);

        if (healthBonus > 0) {
            AttributeModifier permanentHealthModifier = new AttributeModifier(
                    PERMANENT_HEALTH_UUID, "temporal_heart_permanent_health", healthBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                    .applyModifier(permanentHealthModifier);
        }

        if (damageBonus > 0) {
            AttributeModifier permanentDamageModifier = new AttributeModifier(
                    PERMANENT_DAMAGE_UUID, "temporal_heart_permanent_damage", damageBonus, 0);
            player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .applyModifier(permanentDamageModifier);
        }

        if (healthBonus > 0) {
            player.heal((float) healthBonus);
        }
    }

    private void spawnParticles(EntityPlayer player) {
        if (player.world.isRemote) return;

        IPlayerTimeData timeData = PlayerTimeDataCapability.get(player);
        if (timeData == null) return;

        int totalDays = timeData.getTotalDaysPlayed();
        EnumParticleTypes particleType = getParticleType(totalDays);

        for (int i = 0; i < 3; i++) {
            double x = player.posX + (player.world.rand.nextDouble() - 0.5) * 2;
            double y = player.posY + 1 + player.world.rand.nextDouble();
            double z = player.posZ + (player.world.rand.nextDouble() - 0.5) * 2;

            player.world.spawnParticle(particleType, x, y, z, 0, 0.1, 0);
        }
    }

    private EnumParticleTypes getParticleType(int days) {
        if (days < 10) {
            return EnumParticleTypes.END_ROD;
        } else if (days < 30) {
            return EnumParticleTypes.DRAGON_BREATH;
        } else if (days < 100) {
            return EnumParticleTypes.FLAME;
        } else {
            return EnumParticleTypes.TOTEM;
        }
    }

    private int getNextMilestone(int currentDays) {
        int[] milestones = {10, 30, 50, 100, 200, 365, 500, 1000};
        for (int milestone : milestones) {
            if (currentDays < milestone) {
                return milestone;
            }
        }
        return 0;
    }
}