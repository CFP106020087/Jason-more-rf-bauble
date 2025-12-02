package com.moremod.causal;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import c4.champions.common.capability.*;
import c4.champions.common.rank.RankManager;
import c4.champions.network.NetworkHandler;
import c4.champions.network.PacketSyncAffix;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;

import java.util.*;

/**
 * 完全修复版 - 正确清空 affixData
 */
public final class CombinedSuppressor {

    private static final String NBT_INF_SUPP  = "moremod:inf_suppressed";
    private static final String NBT_INF_BACK  = "moremod:inf_backup";
    private static final String NBT_CH_SUPP   = "moremod:ch_suppressed";
    private static final String NBT_CH_BACK   = "moremod:ch_backup";

    private static final WeakHashMap<EntityLivingBase, MobModifier> INF_CACHE = new WeakHashMap<>();

    public static void tick(EntityLivingBase e){
        if (e.world.isRemote) return;

        boolean in = CausalFieldManager.isInField(e.world, e.posX, e.posY, e.posZ);

        handleInfernal(e, in);

        if (e instanceof EntityLiving) {
            handleChampion((EntityLiving)e, in);
        }
    }

    private static void handleInfernal(EntityLivingBase e, boolean in){
        NBTTagCompound tag = e.getEntityData();
        boolean suppressed = tag.getBoolean(NBT_INF_SUPP);

        Map<EntityLivingBase, MobModifier> raresMap = InfernalMobsCore.proxy.getRareMobs();
        boolean isRare = raresMap.containsKey(e);

        if (in && !suppressed && isRare) {
            MobModifier chain = raresMap.get(e);
            if (chain != null) {
                INF_CACHE.put(e, chain);
                String modNames = chain.getLinkedModNameUntranslated();
                if (modNames != null && !modNames.isEmpty()) {
                    tag.setString(NBT_INF_BACK, modNames);
                }
            }

            InfernalMobsCore.removeEntFromElites(e);
            tag.setBoolean(NBT_INF_SUPP, true);
        }
        else if (!in && suppressed) {
            MobModifier chain = INF_CACHE.remove(e);
            if (chain != null) {
                raresMap.put(e, chain);
                chain.onSpawningComplete(e);
            } else {
                String modNames = tag.getString(NBT_INF_BACK);
                if (!modNames.isEmpty()) {
                    InfernalMobsCore.instance().addEntityModifiersByString(e, modNames);
                }
            }

            tag.removeTag(NBT_INF_BACK);
            tag.setBoolean(NBT_INF_SUPP, false);
        }
    }

    private static void handleChampion(EntityLiving e, boolean in){
        NBTTagCompound tag = e.getEntityData();
        boolean suppressed = tag.getBoolean(NBT_CH_SUPP);

        Capability<IChampionship> cap = CapabilityChampionship.CHAMPION_CAP;
        if (!e.hasCapability(cap, null)) return;

        IChampionship chp = e.getCapability(cap, null);
        if (chp == null) return;

        int tier = chp.getRank() != null ? chp.getRank().getTier() : 0;

        if (in && !suppressed && tier > 0) {
            // 备份数据
            NBTTagCompound back = new NBTTagCompound();
            back.setInteger("tier", tier);
            back.setString("name", chp.getName() == null ? "" : chp.getName());

            NBTTagCompound affixes = new NBTTagCompound();
            for (String affixId : chp.getAffixes()) {
                NBTTagCompound affixData = chp.getAffixData(affixId);
                affixes.setTag(affixId, affixData == null ? new NBTTagCompound() : affixData.copy());
            }
            back.setTag("affixes", affixes);

            tag.setTag(NBT_CH_BACK, back);
            tag.setBoolean(NBT_CH_SUPP, true);

            // 【关键修复1】正确清空所有数据
            chp.setRank(RankManager.getEmptyRank());
            chp.setName("");
            chp.setAffixes(new HashSet<>());           // 清空词条ID集合
            chp.setAffixData(new HashMap<>());         // ← 关键：清空词条数据Map！

            // 【关键修复2】强制同步到客户端
            syncChampionToClients(e, 0, new HashMap<>(), "");
        }
        else if (!in && suppressed) {
            if (tag.hasKey(NBT_CH_BACK, 10)) {
                NBTTagCompound back = tag.getCompoundTag(NBT_CH_BACK);

                int oldTier = back.getInteger("tier");
                String oldName = back.getString("name");

                chp.setRank(RankManager.getRankForTier(oldTier));
                chp.setName(oldName);

                // 恢复词条数据
                NBTTagCompound affixes = back.getCompoundTag("affixes");
                Set<String> affixIds = affixes.getKeySet();

                // 重建 affixData Map
                Map<String, NBTTagCompound> affixDataMap = new HashMap<>();
                for (String affixId : affixIds) {
                    affixDataMap.put(affixId, affixes.getCompoundTag(affixId));
                }

                chp.setAffixes(affixIds);                  // 设置词条ID集合
                chp.setAffixData(affixDataMap);            // ← 关键：设置词条数据Map！

                // 强制同步到客户端
                syncChampionToClients(e, oldTier, affixDataMap, oldName);
            }

            tag.removeTag(NBT_CH_BACK);
            tag.setBoolean(NBT_CH_SUPP, false);
        }
    }

    /**
     * 强制同步Champions数据到所有追踪该实体的客户端
     * 使用 Champions 自己的同步包格式
     */
    private static void syncChampionToClients(EntityLiving entity, int tier, Map<String, NBTTagCompound> affixData, String name) {
        if (!(entity.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) entity.world;

        try {
            // 获取所有追踪该实体的玩家
            Set<? extends EntityPlayer> trackingPlayers = world.getEntityTracker().getTrackingPlayers(entity);

            for (EntityPlayer player : trackingPlayers) {
                if (player instanceof EntityPlayerMP) {
                    // 创建同步包（格式与Champions完全一致）
                    PacketSyncAffix packet = new PacketSyncAffix(
                            entity.getEntityId(),
                            tier,           // 压制时传0，恢复时传真实tier
                            affixData,      // 压制时传空Map，恢复时传真实数据
                            name            // 压制时传空字符串，恢复时传真实名字
                    );

                    NetworkHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) player);
                }
            }
        } catch (Exception ex) {
            // Sync failed silently
        }
    }
}