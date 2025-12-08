package com.moremod.entity.curse;

import com.moremod.core.CurseDeathHook;
import com.moremod.fabric.data.UpdatedFabricPlayerData.FabricType;
import com.moremod.init.ModItems;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
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
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 嵌入诅咒管理器
 * Embedded Curse Manager
 *
 * 当七咒玩家在三阶祭坛完成仪式后，诅咒饰品可以嵌入体内
 * 嵌入的饰品提供正面效果并抵消七咒的部分诅咒
 *
 * 七咒饰品对应的嵌入效果：
 * - 虚无之眸 (void_gaze): 死亡保护 + 经验加成
 * - 荆棘碎片 (thorn_shard): 伤害反弹 + 伤害爆发
 * - 怨念结晶 (crystallized_resentment): 真伤光环
 * - 饕餮指骨 (gluttonous_phalanx): 掠夺加成
 * - 绞王之索 (noose_of_hanged_king): 窒息免疫
 * - 第五幕剧本 (script_of_fifth_act): 暴击加成
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EmbeddedCurseManager {

    private static final String NBT_KEY = "moremod_embedded_curses";

    /**
     * 可嵌入的诅咒饰品类型
     *
     * 七咒对应关系：
     * 1. 受伤加倍 → VOID_GAZE (虚无之眸)
     * 2. 中立生物攻击 → GLUTTONOUS_PHALANX (饕餮指骨)
     * 3. 护甲降低30% → THORN_SHARD (荆棘碎片)
     * 4. 伤害降低50% → CRYSTALLIZED_RESENTMENT (怨念结晶)
     * 5. 着火永燃 → NOOSE_OF_HANGED_KING (绞王之索)
     * 6. 灵魂破碎 → SCRIPT_OF_FIFTH_ACT (第五幕剧本)
     * 7. 失眠症 → 嵌入任意3个饰品解除
     */
    public enum EmbeddedCurseType {
        VOID_GAZE("void_gaze", "虚无之眸", "抵消: 受伤加倍"),
        GLUTTONOUS_PHALANX("gluttonous_phalanx", "饕餮指骨", "抵消: 中立生物攻击"),
        THORN_SHARD("thorn_shard", "荆棘碎片", "抵消: 护甲降低30%"),
        CRYSTALLIZED_RESENTMENT("crystallized_resentment", "怨念结晶", "抵消: 伤害降低50%"),
        NOOSE_OF_HANGED_KING("noose_of_hanged_king", "绞王之索", "抵消: 着火永燃"),
        SCRIPT_OF_FIFTH_ACT("script_of_fifth_act", "第五幕剧本", "抵消: 灵魂破碎");

        private final String id;
        private final String displayName;
        private final String effect;

        EmbeddedCurseType(String id, String displayName, String effect) {
            this.id = id;
            this.displayName = displayName;
            this.effect = effect;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEffect() { return effect; }

        public static EmbeddedCurseType fromId(String id) {
            for (EmbeddedCurseType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }

        public static EmbeddedCurseType fromItem(Item item) {
            if (item == null || item.getRegistryName() == null) return null;
            String path = item.getRegistryName().getPath();
            return fromId(path);
        }
    }

    // 内存缓存
    private static final Map<UUID, Set<EmbeddedCurseType>> PLAYER_EMBEDDED = new HashMap<>();

    /**
     * 检查玩家是否有嵌入的诅咒饰品
     */
    public static boolean hasEmbeddedCurse(EntityPlayer player, EmbeddedCurseType type) {
        Set<EmbeddedCurseType> embedded = getEmbeddedCurses(player);
        return embedded.contains(type);
    }

    /**
     * 获取玩家所有嵌入的诅咒
     */
    public static Set<EmbeddedCurseType> getEmbeddedCurses(EntityPlayer player) {
        UUID uuid = player.getUniqueID();

        // 先检查缓存
        if (PLAYER_EMBEDDED.containsKey(uuid)) {
            return PLAYER_EMBEDDED.get(uuid);
        }

        // 从NBT加载
        Set<EmbeddedCurseType> embedded = new HashSet<>();
        NBTTagCompound persistent = getPlayerPersistentData(player);

        if (persistent.hasKey(NBT_KEY)) {
            NBTTagList list = persistent.getTagList(NBT_KEY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                EmbeddedCurseType type = EmbeddedCurseType.fromId(list.getStringTagAt(i));
                if (type != null) {
                    embedded.add(type);
                }
            }
        }

        PLAYER_EMBEDDED.put(uuid, embedded);
        return embedded;
    }

    /**
     * 嵌入诅咒饰品
     * @return true如果成功嵌入
     */
    public static boolean embedCurse(EntityPlayer player, EmbeddedCurseType type) {
        if (type == null) return false;

        Set<EmbeddedCurseType> embedded = getEmbeddedCurses(player);

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
        saveEmbeddedCurses(player, embedded);

        // 应用效果
        applyEmbeddedEffect(player, type);

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
            TextFormatting.GREEN + "七咒诅咒程度减轻..."
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_PURPLE + "═══════════════════════════════════"
        ));

        return true;
    }

    /**
     * 移除嵌入的诅咒（用于特殊情况）
     */
    public static boolean removeEmbeddedCurse(EntityPlayer player, EmbeddedCurseType type) {
        Set<EmbeddedCurseType> embedded = getEmbeddedCurses(player);

        if (!embedded.contains(type)) {
            return false;
        }

        embedded.remove(type);
        saveEmbeddedCurses(player, embedded);

        // 移除效果
        removeEmbeddedEffect(player, type);

        return true;
    }

    /**
     * 获取嵌入的诅咒数量 - 用于计算诅咒减免
     */
    public static int getEmbeddedCount(EntityPlayer player) {
        return getEmbeddedCurses(player).size();
    }

    /**
     * 计算七咒诅咒减免倍率
     * 每嵌入一个饰品减少约14%的诅咒效果（7个全嵌入约100%减免）
     */
    public static float getCurseReductionMultiplier(EntityPlayer player) {
        int count = getEmbeddedCount(player);
        // 每个饰品减少 1/7 ≈ 14.3% 的诅咒
        float reduction = count / 7.0f;
        return Math.min(1.0f, reduction);
    }

    // ========== 内部方法 ==========

    private static void saveEmbeddedCurses(EntityPlayer player, Set<EmbeddedCurseType> embedded) {
        PLAYER_EMBEDDED.put(player.getUniqueID(), embedded);

        NBTTagCompound persistent = getPlayerPersistentData(player);
        NBTTagList list = new NBTTagList();
        for (EmbeddedCurseType type : embedded) {
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

    /**
     * 应用嵌入效果
     */
    private static void applyEmbeddedEffect(EntityPlayer player, EmbeddedCurseType type) {
        switch (type) {
            case VOID_GAZE:
                // 虚无之眸：效果由EmbeddedVoidGazeHandler处理
                break;
            case GLUTTONOUS_PHALANX:
                // 饕餮指骨：效果由EmbeddedLootingHandler处理
                break;
            case NOOSE_OF_HANGED_KING:
                // 绞王之索：窒息免疫由EmbeddedBreathHandler处理
                break;
            case SCRIPT_OF_FIFTH_ACT:
                // 第五幕剧本：暴击由EmbeddedCritHandler处理
                break;
            case THORN_SHARD:
            case CRYSTALLIZED_RESENTMENT:
                // 这些效果在各自的事件处理器中检查embedded状态
                break;
        }
    }

    /**
     * 移除嵌入效果
     */
    private static void removeEmbeddedEffect(EntityPlayer player, EmbeddedCurseType type) {
        // 大部分效果是tick-based，移除缓存后自动失效
    }

    // ========== 事件处理 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // 加载嵌入数据到缓存
        Set<EmbeddedCurseType> embedded = getEmbeddedCurses(player);

        if (!embedded.isEmpty()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "你体内的诅咒饰品: " +
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
     * 检查物品是否是可嵌入的诅咒饰品
     */
    public static boolean isEmbeddableCurseAccessory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return EmbeddedCurseType.fromItem(stack.getItem()) != null;
    }

    /**
     * 从物品获取嵌入类型
     */
    public static EmbeddedCurseType getTypeFromItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return EmbeddedCurseType.fromItem(stack.getItem());
    }
}
