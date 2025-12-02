package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.util.UpgradeKeys;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.moremod.system.ascension.ShambhalaHandler;
import com.moremod.system.ascension.BrokenGodHandler;

import java.util.*;

/**
 * 灵魂绑定版死亡处理器（修复版）
 * ✅ 修复：确保在降级前记录 OriginalMax
 * ✅ 修复：香巴拉/破碎之神死亡保护时跳过惩罚
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SoulboundDeathHandler {

    // ===== PlayerPersisted 键 =====
    private static final String PERSISTED = "PlayerPersisted";
    private static final String K_CORE_NBT = "moremod_SoulboundCoreNbt";
    private static final String K_CORE_IN_BAUBLES = "moremod_CoreInBaubles";
    private static final String K_CORE_SLOT = "moremod_CoreSlot";

    // ===== 修复系统键（与 EnergyPunishmentSystem 统一） =====
    private static final String K_ORIGINAL_MAX = "OriginalMax_";
    private static final String K_OWNED_MAX = "OwnedMax_";
    private static final String K_DAMAGE_COUNT = "DamageCount_";
    private static final String K_WAS_PUNISHED = "WasPunished_";

    // 发电模块保护列表
    private static final Set<String> GENERATORS = new HashSet<>(Arrays.asList(
            "SOLAR_GENERATOR","KINETIC_GENERATOR","THERMAL_GENERATOR","VOID_ENERGY","COMBAT_CHARGER"
    ));

    private static boolean isGeneratorModule(String id){
        if (id == null) return false;
        String u = id.toUpperCase(Locale.ROOT);
        return GENERATORS.contains(u) || u.contains("GENERATOR") || u.contains("CHARGER");
    }

    // 防水别名
    private static final Set<String> WATERPROOF_IDS = new HashSet<>(Arrays.asList(
            "WATERPROOF_MODULE","WATERPROOF"
    ));

    private static boolean isWaterproof(String id){
        return id != null && (WATERPROOF_IDS.contains(id.toUpperCase(Locale.ROOT)) ||
                id.toUpperCase(Locale.ROOT).contains("WATERPROOF"));
    }

    // ===== 1) 死亡：记录槽位 + 惩罚 =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event){
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || player.isCreative() || player.isSpectator()) return;

        // ✅ 检查香巴拉/破碎之神死亡保护
        // 如果死亡会被ASM阻止，则跳过惩罚
        if (isDeathProtected(player)) {
            return;
        }

        recordCoreLocation(player);

        ItemStack core = findCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        applyDeathPunishment(core, player);
    }

    /**
     * 检查玩家是否受到死亡保护（香巴拉或破碎之神）
     * 如果有保护且有能量，死亡会被ASM阻止，此时不应触发惩罚
     */
    private static boolean isDeathProtected(EntityPlayer player) {
        try {
            // 检查香巴拉保护
            if (ShambhalaHandler.isShambhala(player)) {
                int energy = ShambhalaHandler.getCurrentEnergy(player);
                if (energy > 0) {
                    // 香巴拉有能量 = 死亡会被阻止
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            // 检查破碎之神保护
            if (BrokenGodHandler.isBrokenGod(player)) {

                    // 破碎之神有能量 = 死亡会被阻止
                    return true;

            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static void recordCoreLocation(EntityPlayer p){
        NBTTagCompound ed = p.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) ed.setTag(PERSISTED, new NBTTagCompound());
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        boolean inBaubles = false;
        int slotIdx = -1;
        try {
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (ItemMechanicalCore.isMechanicalCore(s)) {
                        inBaubles = true;
                        slotIdx = i;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (!inBaubles) {
            for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
                ItemStack s = p.inventory.getStackInSlot(i);
                if (ItemMechanicalCore.isMechanicalCore(s)) {
                    slotIdx = i;
                    break;
                }
            }
        }

        persisted.setBoolean(K_CORE_IN_BAUBLES, inBaubles);
        persisted.setInteger(K_CORE_SLOT, slotIdx);
    }

    // ===== 2) 掉落：把核心序列化存进 PlayerPersisted =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer p = event.getEntityPlayer();
        if (p.world.isRemote) return;

        ListIterator<net.minecraft.entity.item.EntityItem> it = event.getDrops().listIterator();
        while (it.hasNext()){
            net.minecraft.entity.item.EntityItem ent = it.next();
            ItemStack s = ent.getItem();
            if (ItemMechanicalCore.isMechanicalCore(s)){
                NBTTagCompound ed = p.getEntityData();
                if (!ed.hasKey(PERSISTED, 10)) ed.setTag(PERSISTED, new NBTTagCompound());
                NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

                NBTTagCompound coreNbt = new NBTTagCompound();
                s.writeToNBT(coreNbt);
                persisted.setTag(K_CORE_NBT, coreNbt);

                it.remove();
                ent.setDead();
                break;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event){
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        event.getDrops().removeIf(ei -> ItemMechanicalCore.isMechanicalCore(ei.getItem()));
    }

    // ===== 3) Clone：从 PlayerPersisted 复原核心 =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event){
        if (!event.isWasDeath()) return;
        EntityPlayer newP = event.getEntityPlayer();
        if (newP.world.isRemote) return;

        NBTTagCompound ed = newP.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) return;
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        if (!persisted.hasKey(K_CORE_NBT, 10)) return;

        ItemStack core = new ItemStack(persisted.getCompoundTag(K_CORE_NBT));
        if (core.isEmpty() || !ItemMechanicalCore.isMechanicalCore(core)) return;

        boolean inBaubles = persisted.getBoolean(K_CORE_IN_BAUBLES);
        int slotIdx = persisted.getInteger(K_CORE_SLOT);

        boolean placed = false;
        if (inBaubles) {
            try {
                IBaublesItemHandler h = BaublesApi.getBaublesHandler(newP);
                if (h != null) {
                    if (slotIdx >= 0 && slotIdx < h.getSlots() && h.getStackInSlot(slotIdx).isEmpty()) {
                        h.setStackInSlot(slotIdx, core.copy());
                        placed = true;
                    } else {
                        for (int i = 0; i < h.getSlots(); i++) {
                            if (h.getStackInSlot(i).isEmpty()) {
                                h.setStackInSlot(i, core.copy());
                                placed = true;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (!placed) {
            placed = newP.inventory.addItemStackToInventory(core.copy());
        }

        if (placed) {
            applyEmergencyCharge(core, newP);

            persisted.removeTag(K_CORE_NBT);
            persisted.removeTag(K_CORE_IN_BAUBLES);
            persisted.removeTag(K_CORE_SLOT);

            newP.sendMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "⚙ 机械核心与你的灵魂绑定，已随你复生"));
        }
    }

    // ===== ✅ 修复：死亡惩罚逻辑 =====
    private static void applyDeathPunishment(ItemStack core, EntityPlayer player){
        List<String> installed = getInstalledUpgrades(core);
        installed.removeIf(SoulboundDeathHandler::isGeneratorModule);
        installed.removeIf(id -> ItemMechanicalCore.isUpgradePaused(core, id));

        if (installed.isEmpty()){
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚠ 无可降级的活跃非发电模块"));
            return;
        }

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String target = installed.get(player.world.rand.nextInt(installed.size()));
        String upperId = target.toUpperCase(Locale.ROOT);
        String lowerId = target.toLowerCase(Locale.ROOT);

        // 1. 在降级前，立即读取并记录当前状态
        int currentOwnedMax = getOwnedMax(core, target);
        int currentLevel = getCurrentLevel(core, target);

        // 如果没有 OwnedMax，使用当前等级初始化
        if (currentOwnedMax <= 0 && currentLevel > 0) {
            currentOwnedMax = currentLevel;
            setOwnedMaxEverywhere(core, target, currentOwnedMax);
        }

        // 2. 如果是第一次损坏，立即记录 OriginalMax
        if (!nbt.hasKey(K_ORIGINAL_MAX + upperId) && currentOwnedMax > 0) {
            // 记录降级前的值（所有变体）
            nbt.setInteger(K_ORIGINAL_MAX + upperId, currentOwnedMax);
            nbt.setInteger(K_ORIGINAL_MAX + target, currentOwnedMax);
            nbt.setInteger(K_ORIGINAL_MAX + lowerId, currentOwnedMax);

            // 防水别名也记录
            if (isWaterproof(target)) {
                for (String wid : WATERPROOF_IDS) {
                    nbt.setInteger(K_ORIGINAL_MAX + wid, currentOwnedMax);
                    nbt.setInteger(K_ORIGINAL_MAX + wid.toUpperCase(Locale.ROOT), currentOwnedMax);
                    nbt.setInteger(K_ORIGINAL_MAX + wid.toLowerCase(Locale.ROOT), currentOwnedMax);
                }
            }
        }

        // 3. 设置惩罚标记
        nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
        nbt.setBoolean(K_WAS_PUNISHED + target, true);
        nbt.setBoolean(K_WAS_PUNISHED + lowerId, true);

        if (isWaterproof(target)) {
            for (String wid : WATERPROOF_IDS) {
                nbt.setBoolean(K_WAS_PUNISHED + wid, true);
                nbt.setBoolean(K_WAS_PUNISHED + wid.toUpperCase(Locale.ROOT), true);
                nbt.setBoolean(K_WAS_PUNISHED + wid.toLowerCase(Locale.ROOT), true);
            }
        }

        // 4. 增加损坏计数
        int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
        nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + 1);
        nbt.setInteger(K_DAMAGE_COUNT + target, damageCount + 1);
        nbt.setInteger(K_DAMAGE_COUNT + lowerId, damageCount + 1);

        // 累计总损坏次数
        int totalDamageCount = nbt.getInteger("TotalDamageCount_" + upperId);
        nbt.setInteger("TotalDamageCount_" + upperId, totalDamageCount + 1);
        nbt.setInteger("TotalDamageCount_" + target, totalDamageCount + 1);
        nbt.setInteger("TotalDamageCount_" + lowerId, totalDamageCount + 1);

        // 5. 然后才降级（在记录之后）
        int newOwnedMax = Math.max(0, currentOwnedMax - 1);
        setOwnedMaxSafe(core, target, newOwnedMax);

        // 6. 验证 OriginalMax 是否被保留（静默校验，不输出日志）
        int verifyOriginalMax = nbt.getInteger(K_ORIGINAL_MAX + upperId);
        if (verifyOriginalMax != currentOwnedMax && verifyOriginalMax > 0) {
            // 若不一致，这里保持静默（可在未来接入可开关的调试系统）
        }

        // 7. 调整当前等级
        if (currentLevel > newOwnedMax) {
            setLevelEverywhere(core, target, newOwnedMax);
        }

        // 获取 OriginalMax 用于显示
        int originalMax = nbt.getInteger(K_ORIGINAL_MAX + upperId);
        if (originalMax <= 0) originalMax = currentOwnedMax;

        // 发送消息
        player.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "☠ 死亡惩罚"));

        if (newOwnedMax == 0){
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ " + getDisplayName(target) + " 完全损坏 [0/" + originalMax + "]" +
                            TextFormatting.YELLOW + " (通过GUI修复)"));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ " + getDisplayName(target) +
                            " 损坏至 Lv." + newOwnedMax + "/" + originalMax +
                            TextFormatting.AQUA + " (可通过GUI修复)"));
        }

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 0.8f);
    }

    // ===== 应急充能（20%） =====
    private static void applyEmergencyCharge(ItemStack core, EntityPlayer player){
        IEnergyStorage es = ItemMechanicalCore.getEnergyStorage(core);
        if (es == null || es.getMaxEnergyStored() <= 0) return;

        int target = es.getMaxEnergyStored() / 5;
        if (es.getEnergyStored() < target){
            es.receiveEnergy(target - es.getEnergyStored(), false);
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "⚡ 复活应急充能已注入（20%能量）"));
        }
    }

    // ===== 工具：查找核心 =====
    private static ItemStack findCore(EntityPlayer p){
        try {
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
            if (h != null){
                for (int i = 0; i < h.getSlots(); i++){
                    ItemStack s = h.getStackInSlot(i);
                    if (ItemMechanicalCore.isMechanicalCore(s)) return s;
                }
            }
        } catch (Throwable ignored) {}

        for (int i = 0; i < p.inventory.getSizeInventory(); i++){
            ItemStack s = p.inventory.getStackInSlot(i);
            if (ItemMechanicalCore.isMechanicalCore(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    // ===== 工具：升级读写 =====
    private static int getCurrentLevel(ItemStack core, String id){
        NBTTagCompound nbt = core.getTagCompound();
        int lv = 0;

        if (nbt != null){
            if (nbt.getBoolean("IsPaused_" + id) ||
                    nbt.getBoolean("IsPaused_" + id.toUpperCase(Locale.ROOT)) ||
                    nbt.getBoolean("IsPaused_" + id.toLowerCase(Locale.ROOT))) {
                return 0;
            }

            lv = Math.max(lv, nbt.getInteger("upgrade_" + id));
            lv = Math.max(lv, nbt.getInteger("upgrade_" + id.toUpperCase(Locale.ROOT)));
            lv = Math.max(lv, nbt.getInteger("upgrade_" + id.toLowerCase(Locale.ROOT)));
        }

        try {
            lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id));
        } catch (Throwable ignored) {}

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)){
                    lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, t));
                    break;
                }
            }
        } catch (Throwable ignored) {}

        return lv;
    }

    private static int getOwnedMax(ItemStack core, String id){
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        int v = Math.max(nbt.getInteger("OwnedMax_" + id),
                Math.max(nbt.getInteger("OwnedMax_" + id.toUpperCase(Locale.ROOT)),
                        nbt.getInteger("OwnedMax_" + id.toLowerCase(Locale.ROOT))));

        if (v == 0 && isWaterproof(id)){
            for (String wid : WATERPROOF_IDS){
                v = Math.max(v, Math.max(nbt.getInteger("OwnedMax_" + wid),
                        Math.max(nbt.getInteger("OwnedMax_" + wid.toUpperCase(Locale.ROOT)),
                                nbt.getInteger("OwnedMax_" + wid.toLowerCase(Locale.ROOT)))));
            }
        }
        return v;
    }

    /**
     * ✅ 安全设置 OwnedMax，确保不覆盖 OriginalMax
     */
    private static void setOwnedMaxSafe(ItemStack core, String id, int val){
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String upperId = id.toUpperCase(Locale.ROOT);
        String lowerId = id.toLowerCase(Locale.ROOT);
        String[] variants = {id, upperId, lowerId};

        // 备份 OriginalMax
        Map<String, Integer> originalMaxBackup = new HashMap<>();
        for (String variant : variants) {
            int origVal = nbt.getInteger(K_ORIGINAL_MAX + variant);
            if (origVal > 0) {
                originalMaxBackup.put(variant, origVal);
            }
        }

        // 防水别名也备份
        if (isWaterproof(id)) {
            for (String wid : WATERPROOF_IDS) {
                String[] wvariants = {wid, wid.toUpperCase(Locale.ROOT), wid.toLowerCase(Locale.ROOT)};
                for (String wv : wvariants) {
                    int origVal = nbt.getInteger(K_ORIGINAL_MAX + wv);
                    if (origVal > 0) {
                        originalMaxBackup.put(wv, origVal);
                    }
                }
            }
        }

        // 设置 OwnedMax
        for (String k : variants) {
            nbt.setInteger("OwnedMax_" + k, val);
        }

        if (isWaterproof(id)) {
            for (String wid : WATERPROOF_IDS){
                nbt.setInteger("OwnedMax_" + wid, val);
                nbt.setInteger("OwnedMax_" + wid.toUpperCase(Locale.ROOT), val);
                nbt.setInteger("OwnedMax_" + wid.toLowerCase(Locale.ROOT), val);
            }
        }

        // 恢复 OriginalMax
        for (Map.Entry<String, Integer> entry : originalMaxBackup.entrySet()) {
            nbt.setInteger(K_ORIGINAL_MAX + entry.getKey(), entry.getValue());
        }
    }

    /**
     * ✅ 旧方法保留（向后兼容）
     */
    private static void setOwnedMaxEverywhere(ItemStack core, String id, int val){
        setOwnedMaxSafe(core, id, val);
    }

    private static void setLevelEverywhere(ItemStack core, String id, int val){
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String[] vs = {id, id.toUpperCase(Locale.ROOT), id.toLowerCase(Locale.ROOT)};

        for (String k : vs) {
            nbt.setInteger("upgrade_" + k, val);
        }

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)){
                    ItemMechanicalCore.setUpgradeLevel(core, t, val);
                    break;
                }
            }
        } catch (Throwable ignored) {}

        try {
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id, val);
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id.toUpperCase(Locale.ROOT), val);
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id.toLowerCase(Locale.ROOT), val);
        } catch (Throwable ignored) {}

        if (isWaterproof(id)){
            for (String wid : WATERPROOF_IDS){
                nbt.setInteger("upgrade_" + wid, val);
                nbt.setInteger("upgrade_" + wid.toUpperCase(Locale.ROOT), val);
                nbt.setInteger("upgrade_" + wid.toLowerCase(Locale.ROOT), val);
                try {
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, wid, val);
                } catch (Throwable ignored) {}
            }
        }

        try {
            UpgradeKeys.setLevel(core, id, val);
        } catch (Throwable ignored) {}
    }

    private static List<String> getInstalledUpgrades(ItemStack core){
        Set<String> set = new HashSet<>();

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (ItemMechanicalCore.getUpgradeLevel(core, t) > 0) {
                    set.add(t.getKey());
                }
            }
        } catch (Throwable ignored) {}

        try {
            Map<String, ItemMechanicalCoreExtended.UpgradeInfo> all = ItemMechanicalCoreExtended.getAllUpgrades();
            for (Map.Entry<String, ItemMechanicalCoreExtended.UpgradeInfo> e : all.entrySet()){
                if (ItemMechanicalCoreExtended.getUpgradeLevel(core, e.getKey()) > 0) {
                    set.add(e.getKey());
                }
            }
        } catch (Throwable ignored) {}

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            for (String k : nbt.getKeySet()){
                if (k.startsWith("upgrade_") && nbt.getInteger(k) > 0){
                    set.add(k.substring("upgrade_".length()));
                } else if (k.startsWith("HasUpgrade_") && nbt.getBoolean(k)) {
                    set.add(k.substring("HasUpgrade_".length()));
                } else if (k.startsWith("OwnedMax_") && nbt.getInteger(k) > 0) {
                    set.add(k.substring("OwnedMax_".length()));
                }
            }
        }

        return new ArrayList<>(set);
    }

    private static String getDisplayName(String id){
        try {
            ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
            if (info != null && info.displayName != null) return info.displayName;
        } catch (Throwable ignored) {}

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                    return t.getDisplayName();
                }
            }
        } catch (Throwable ignored) {}

        return id.replace("_"," ").replace("-"," ");
    }
}
