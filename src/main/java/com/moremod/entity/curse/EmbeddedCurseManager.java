package com.moremod.entity.curse;

import com.moremod.core.CurseDeathHook;
import com.moremod.init.ModItems;
import com.moremod.item.curse.ItemSacredRelic;
import com.moremod.item.curse.ItemSacredRelic.RelicType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

import java.util.*;

/**
 * 嵌入遗物管理器 (Embedded Relic Manager)
 *
 * 当七咒玩家在三阶祭坛完成仪式后，七圣遗物可以嵌入体内
 * 嵌入的遗物可抵消七咒之戒的对应诅咒
 *
 * 七圣遗物与诅咒对应关系：
 * - 圣光之心 (SACRED_HEART): 抵消 受到伤害加倍
 * - 和平徽章 (PEACE_EMBLEM): 抵消 中立生物攻击
 * - 守护鳞片 (GUARDIAN_SCALE): 抵消 护甲效力降低30%
 * - 勇气之刃 (COURAGE_BLADE): 抵消 对怪物伤害降低50%
 * - 霜华之露 (FROST_DEW): 抵消 着火永燃
 * - 灵魂锚点 (SOUL_ANCHOR): 抵消 死亡灵魂破碎
 * - 安眠香囊 (SLUMBER_SACHET): 抵消 失眠症
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EmbeddedCurseManager {

    private static final String NBT_KEY = "moremod_embedded_relics";

    /**
     * 嵌入的遗物类型 - 基于 ItemSacredRelic.RelicType
     */
    public enum EmbeddedRelicType {
        SACRED_HEART("sacred_heart", "圣光之心", "抵消: 受到伤害加倍"),
        PEACE_EMBLEM("peace_emblem", "和平徽章", "抵消: 中立生物攻击"),
        GUARDIAN_SCALE("guardian_scale", "守护鳞片", "抵消: 护甲效力降低30%"),
        COURAGE_BLADE("courage_blade", "勇气之刃", "抵消: 对怪物伤害降低50%"),
        FROST_DEW("frost_dew", "霜华之露", "抵消: 着火永燃"),
        SOUL_ANCHOR("soul_anchor", "灵魂锚点", "抵消: 死亡灵魂破碎"),
        SLUMBER_SACHET("slumber_sachet", "安眠香囊", "抵消: 失眠症");

        private final String id;
        private final String displayName;
        private final String effect;

        EmbeddedRelicType(String id, String displayName, String effect) {
            this.id = id;
            this.displayName = displayName;
            this.effect = effect;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEffect() { return effect; }

        public static EmbeddedRelicType fromId(String id) {
            for (EmbeddedRelicType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }

        public static EmbeddedRelicType fromItem(Item item) {
            if (item instanceof ItemSacredRelic) {
                ItemSacredRelic relic = (ItemSacredRelic) item;
                RelicType relicType = relic.getRelicType();
                if (relicType != null) {
                    return fromId(relicType.getId());
                }
            }
            return null;
        }

        /**
         * 从 ItemSacredRelic.RelicType 转换
         */
        public static EmbeddedRelicType fromRelicType(RelicType relicType) {
            if (relicType == null) return null;
            return fromId(relicType.getId());
        }
    }

    // 保留旧类型别名，确保兼容性
    @Deprecated
    public static class EmbeddedCurseType {
        public static final EmbeddedRelicType VOID_GAZE = EmbeddedRelicType.SACRED_HEART;
        public static final EmbeddedRelicType GLUTTONOUS_PHALANX = EmbeddedRelicType.PEACE_EMBLEM;
        public static final EmbeddedRelicType THORN_SHARD = EmbeddedRelicType.GUARDIAN_SCALE;
        public static final EmbeddedRelicType CRYSTALLIZED_RESENTMENT = EmbeddedRelicType.COURAGE_BLADE;
        public static final EmbeddedRelicType NOOSE_OF_HANGED_KING = EmbeddedRelicType.FROST_DEW;
        public static final EmbeddedRelicType SCRIPT_OF_FIFTH_ACT = EmbeddedRelicType.SOUL_ANCHOR;
    }

    // 内存缓存
    private static final Map<UUID, Set<EmbeddedRelicType>> PLAYER_EMBEDDED = new HashMap<>();

    /**
     * 检查玩家是否有嵌入的遗物
     */
    public static boolean hasEmbeddedRelic(EntityPlayer player, EmbeddedRelicType type) {
        Set<EmbeddedRelicType> embedded = getEmbeddedRelics(player);
        return embedded.contains(type);
    }

    /**
     * 兼容旧方法名
     */
    @Deprecated
    public static boolean hasEmbeddedCurse(EntityPlayer player, EmbeddedRelicType type) {
        return hasEmbeddedRelic(player, type);
    }

    /**
     * 获取玩家所有嵌入的遗物
     */
    public static Set<EmbeddedRelicType> getEmbeddedRelics(EntityPlayer player) {
        UUID uuid = player.getUniqueID();

        // 先检查缓存
        if (PLAYER_EMBEDDED.containsKey(uuid)) {
            return PLAYER_EMBEDDED.get(uuid);
        }

        // 从NBT加载
        Set<EmbeddedRelicType> embedded = new HashSet<>();
        NBTTagCompound persistent = getPlayerPersistentData(player);

        if (persistent.hasKey(NBT_KEY)) {
            NBTTagList list = persistent.getTagList(NBT_KEY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                EmbeddedRelicType type = EmbeddedRelicType.fromId(list.getStringTagAt(i));
                if (type != null) {
                    embedded.add(type);
                }
            }
        }

        PLAYER_EMBEDDED.put(uuid, embedded);
        return embedded;
    }

    /**
     * 兼容旧方法名
     */
    @Deprecated
    public static Set<EmbeddedRelicType> getEmbeddedCurses(EntityPlayer player) {
        return getEmbeddedRelics(player);
    }

    /**
     * 嵌入遗物
     * @return true如果成功嵌入
     */
    public static boolean embedRelic(EntityPlayer player, EmbeddedRelicType type) {
        if (type == null) return false;

        Set<EmbeddedRelicType> embedded = getEmbeddedRelics(player);

        // 检查是否已嵌入
        if (embedded.contains(type)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "你已经嵌入了 " + type.getDisplayName() + "！"
            ));
            return false;
        }

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "需要佩戴七咒之戒才能进行嵌入仪式！"
            ));
            return false;
        }

        // 添加到缓存和NBT
        embedded.add(type);
        saveEmbeddedRelics(player, embedded);

        // 通知玩家
        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_PURPLE + "═══════════════════════════════════"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "★ " + type.getDisplayName() + " 已嵌入体内！"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "效果: " + TextFormatting.AQUA + type.getEffect()
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "七咒诅咒已被抵消！"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_PURPLE + "═══════════════════════════════════"
        ));

        return true;
    }

    /**
     * 兼容旧方法名
     */
    @Deprecated
    public static boolean embedCurse(EntityPlayer player, EmbeddedRelicType type) {
        return embedRelic(player, type);
    }

    /**
     * 移除嵌入的遗物（用于特殊情况）
     */
    public static boolean removeEmbeddedRelic(EntityPlayer player, EmbeddedRelicType type) {
        Set<EmbeddedRelicType> embedded = getEmbeddedRelics(player);

        if (!embedded.contains(type)) {
            return false;
        }

        embedded.remove(type);
        saveEmbeddedRelics(player, embedded);

        return true;
    }

    /**
     * 兼容旧方法名
     */
    @Deprecated
    public static boolean removeEmbeddedCurse(EntityPlayer player, EmbeddedRelicType type) {
        return removeEmbeddedRelic(player, type);
    }

    /**
     * 获取嵌入的遗物数量
     */
    public static int getEmbeddedCount(EntityPlayer player) {
        return getEmbeddedRelics(player).size();
    }

    /**
     * 计算七咒诅咒减免倍率
     * 每嵌入一个遗物减少约14%的诅咒效果（7个全嵌入约100%减免）
     */
    public static float getCurseReductionMultiplier(EntityPlayer player) {
        int count = getEmbeddedCount(player);
        // 每个遗物减少 1/7 ≈ 14.3% 的诅咒
        float reduction = count / 7.0f;
        return Math.min(1.0f, reduction);
    }

    // ========== 内部方法 ==========

    private static void saveEmbeddedRelics(EntityPlayer player, Set<EmbeddedRelicType> embedded) {
        PLAYER_EMBEDDED.put(player.getUniqueID(), embedded);

        NBTTagCompound persistent = getPlayerPersistentData(player);
        NBTTagList list = new NBTTagList();
        for (EmbeddedRelicType type : embedded) {
            list.appendTag(new NBTTagString(type.getId()));
        }
        persistent.setTag(NBT_KEY, list);
    }

    private static NBTTagCompound getPlayerPersistentData(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    // ========== 事件处理 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // 加载嵌入数据到缓存
        Set<EmbeddedRelicType> embedded = getEmbeddedRelics(player);

        if (!embedded.isEmpty()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "体内嵌入的七圣遗物: " +
                TextFormatting.GOLD + embedded.size() + "/7"
            ));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 死亡/维度传送时保留嵌入数据
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        NBTTagCompound oldPersistent = getPlayerPersistentData(oldPlayer);
        if (oldPersistent.hasKey(NBT_KEY)) {
            NBTTagCompound newPersistent = getPlayerPersistentData(newPlayer);
            newPersistent.setTag(NBT_KEY, oldPersistent.getTag(NBT_KEY).copy());
        }

        // 清除旧缓存
        PLAYER_EMBEDDED.remove(oldPlayer.getUniqueID());
    }

    /**
     * 检查物品是否是可嵌入的七圣遗物
     */
    public static boolean isEmbeddableSacredRelic(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemSacredRelic;
    }

    /**
     * 兼容旧方法名
     */
    @Deprecated
    public static boolean isEmbeddableCurseAccessory(ItemStack stack) {
        return isEmbeddableSacredRelic(stack);
    }

    /**
     * 从物品获取嵌入类型
     */
    public static EmbeddedRelicType getTypeFromItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return EmbeddedRelicType.fromItem(stack.getItem());
    }
}
