package com.moremod.item.causal;

import com.moremod.compat.ChampionReflectionHelper;
import com.moremod.compat.InfernalReflectionHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 完全修复版 - 正确清空 affixData (使用反射，无硬依赖)
 */
public final class CombinedSuppressor {

    private static final String NBT_INF_SUPP  = "moremod:inf_suppressed";
    private static final String NBT_INF_BACK  = "moremod:inf_backup";
    private static final String NBT_CH_SUPP   = "moremod:ch_suppressed";
    private static final String NBT_CH_BACK   = "moremod:ch_backup";

    // 使用 Object 代替 MobModifier，因为使用反射
    private static final WeakHashMap<EntityLivingBase, Object> INF_CACHE = new WeakHashMap<>();

    public static void tick(EntityLivingBase e){
        if (e.world.isRemote) return;

        boolean in = CausalFieldManager.isInField(e.world, e.posX, e.posY, e.posZ);

        handleInfernal(e, in);

        if (e instanceof EntityLiving) {
            handleChampion((EntityLiving)e, in);
        }
    }

    private static void handleInfernal(EntityLivingBase e, boolean in){
        if (!InfernalReflectionHelper.isInfernalAvailable()) return;

        NBTTagCompound tag = e.getEntityData();
        boolean suppressed = tag.getBoolean(NBT_INF_SUPP);

        boolean isRare = InfernalReflectionHelper.isInRareMobs(e);

        if (in && !suppressed && isRare) {
            Object chain = InfernalReflectionHelper.getModifier(e);
            if (chain != null) {
                INF_CACHE.put(e, chain);
                String modNames = InfernalReflectionHelper.getLinkedModNameUntranslated(chain);
                if (modNames != null && !modNames.isEmpty()) {
                    tag.setString(NBT_INF_BACK, modNames);
                }
                System.out.println("[Infernal] 压制: " + e.getName() + " (Mods: " + modNames + ")");
            }

            InfernalReflectionHelper.removeEntFromElites(e);
            tag.setBoolean(NBT_INF_SUPP, true);
        }
        else if (!in && suppressed) {
            Object chain = INF_CACHE.remove(e);
            if (chain != null) {
                InfernalReflectionHelper.putRareMob(e, chain);
                InfernalReflectionHelper.onSpawningComplete(chain, e);
                System.out.println("[Infernal] 恢复: " + e.getName());
            } else {
                String modNames = tag.getString(NBT_INF_BACK);
                if (!modNames.isEmpty()) {
                    InfernalReflectionHelper.addEntityModifiersByString(e, modNames);
                    System.out.println("[Infernal] 从NBT恢复: " + e.getName());
                }
            }

            tag.removeTag(NBT_INF_BACK);
            tag.setBoolean(NBT_INF_SUPP, false);
        }
    }

    private static void handleChampion(EntityLiving e, boolean in){
        if (!ChampionReflectionHelper.isChampionsAvailable()) return;

        NBTTagCompound tag = e.getEntityData();
        boolean suppressed = tag.getBoolean(NBT_CH_SUPP);

        Object chp = ChampionReflectionHelper.getChampionship(e);
        if (chp == null) return;

        Object rank = ChampionReflectionHelper.getRank(chp);
        int tier = rank != null ? ChampionReflectionHelper.getTier(rank) : 0;

        if (in && !suppressed && tier > 0) {
            // 备份数据
            NBTTagCompound back = new NBTTagCompound();
            back.setInteger("tier", tier);
            back.setString("name", ChampionReflectionHelper.getName(chp));

            NBTTagCompound affixes = new NBTTagCompound();
            for (String affixId : ChampionReflectionHelper.getAffixes(chp)) {
                NBTTagCompound affixData = ChampionReflectionHelper.getAffixData(chp, affixId);
                affixes.setTag(affixId, affixData == null ? new NBTTagCompound() : affixData.copy());
            }
            back.setTag("affixes", affixes);

            tag.setTag(NBT_CH_BACK, back);
            tag.setBoolean(NBT_CH_SUPP, true);

            System.out.println("[Champions] 压制: " + e.getName() + " (Tier " + tier + ", " + affixes.getKeySet().size() + " 词条)");

            // 【关键修复1】正确清空所有数据
            ChampionReflectionHelper.setRank(chp, ChampionReflectionHelper.getEmptyRank());
            ChampionReflectionHelper.setName(chp, "");
            ChampionReflectionHelper.setAffixes(chp, new HashSet<>());           // 清空词条ID集合
            ChampionReflectionHelper.setAffixData(chp, new HashMap<>());         // ← 关键：清空词条数据Map！

            // 【关键修复2】强制同步到客户端
            syncChampionToClients(e, 0, new HashMap<>(), "");
        }
        else if (!in && suppressed) {
            if (tag.hasKey(NBT_CH_BACK, 10)) {
                NBTTagCompound back = tag.getCompoundTag(NBT_CH_BACK);

                int oldTier = back.getInteger("tier");
                String oldName = back.getString("name");

                ChampionReflectionHelper.setRank(chp, ChampionReflectionHelper.getRankForTier(oldTier));
                ChampionReflectionHelper.setName(chp, oldName);

                // 恢复词条数据
                NBTTagCompound affixes = back.getCompoundTag("affixes");
                Set<String> affixIds = affixes.getKeySet();

                // 重建 affixData Map
                Map<String, NBTTagCompound> affixDataMap = new HashMap<>();
                for (String affixId : affixIds) {
                    affixDataMap.put(affixId, affixes.getCompoundTag(affixId));
                }

                ChampionReflectionHelper.setAffixes(chp, affixIds);                  // 设置词条ID集合
                ChampionReflectionHelper.setAffixData(chp, affixDataMap);            // ← 关键：设置词条数据Map！

                System.out.println("[Champions] 恢复: " + e.getName() + " (Tier " + oldTier + ", " + affixIds.size() + " 词条)");

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
        if (!ChampionReflectionHelper.isChampionsAvailable()) return;
        if (!(entity.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) entity.world;

        try {
            // 获取所有追踪该实体的玩家
            Set<? extends EntityPlayer> trackingPlayers = world.getEntityTracker().getTrackingPlayers(entity);

            for (EntityPlayer player : trackingPlayers) {
                if (player instanceof EntityPlayerMP) {
                    // 创建同步包（格式与Champions完全一致）
                    Object packet = ChampionReflectionHelper.createPacketSyncAffix(
                            entity.getEntityId(),
                            tier,           // 压制时传0，恢复时传真实tier
                            affixData,      // 压制时传空Map，恢复时传真实数据
                            name            // 压制时传空字符串，恢复时传真实名字
                    );

                    ChampionReflectionHelper.sendPacketToPlayer(packet, (EntityPlayerMP) player);

                    System.out.println(String.format(
                            "[Champions] 同步到客户端: %s (Tier=%d, Affixes=%d)",
                            player.getName(), tier, affixData.size()
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("[Champions] 同步失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}