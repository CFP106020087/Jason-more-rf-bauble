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

import java.util.*;

/**
 * 灵魂绑定版死亡处理器（修复版）
 * - 死亡：记录槽位；执行惩罚（使用统一的损坏/修复机制）
 * - 掉落：拦截核心，序列化保存到 PlayerPersisted
 * - Clone：从 PlayerPersisted 反序列化并放回（优先原 Baubles 槽）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SoulboundDeathHandler {

    // ===== PlayerPersisted 键 =====
    private static final String PERSISTED = "PlayerPersisted";
    private static final String K_CORE_NBT = "moremod_SoulboundCoreNbt";     // ItemStack NBT
    private static final String K_CORE_IN_BAUBLES = "moremod_CoreInBaubles"; // 是否在 Baubles
    private static final String K_CORE_SLOT = "moremod_CoreSlot";            // 槽位索引

    // ===== 修复系统键（与 EnergyPunishmentSystem 统一） =====
    private static final String K_ORIGINAL_MAX = "OriginalMax_";
    private static final String K_OWNED_MAX = "OwnedMax_";
    private static final String K_DAMAGE_COUNT = "DamageCount_";
    private static final String K_WAS_PUNISHED = "WasPunished_";

    // 发电模块保护列表 + 关键字
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

        // 记录核心所在槽位（用于 Clone 时尽量放回原位）
        recordCoreLocation(player);

        // 找核心执行惩罚
        ItemStack core = findCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        applyDeathPunishment(core, player);
    }

    private static void recordCoreLocation(EntityPlayer p){
        NBTTagCompound ed = p.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) ed.setTag(PERSISTED, new NBTTagCompound());
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        // 先看 Baubles
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
            // 背包
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

    // ===== 2) 掉落：把核心序列化存进 PlayerPersisted，避免丢失 =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer p = event.getEntityPlayer();
        if (p.world.isRemote) return;

        ListIterator<net.minecraft.entity.item.EntityItem> it = event.getDrops().listIterator();
        while (it.hasNext()){
            net.minecraft.entity.item.EntityItem ent = it.next();
            ItemStack s = ent.getItem();
            if (ItemMechanicalCore.isMechanicalCore(s)){
                // 保存到 PlayerPersisted
                NBTTagCompound ed = p.getEntityData();
                if (!ed.hasKey(PERSISTED, 10)) ed.setTag(PERSISTED, new NBTTagCompound());
                NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

                NBTTagCompound coreNbt = new NBTTagCompound();
                s.writeToNBT(coreNbt);
                persisted.setTag(K_CORE_NBT, coreNbt);

                // 移除掉落实体，避免掉落
                it.remove();
                ent.setDead();

                System.out.println("[SoulboundDeath] 捕获核心到 PlayerPersisted，阻止掉落");
                break; // 只应有一个核心
            }
        }
    }

    // 备用：防止通过其他通道掉落
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event){
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        event.getDrops().removeIf(ei -> ItemMechanicalCore.isMechanicalCore(ei.getItem()));
    }

    // ===== 3) Clone：从 PlayerPersisted 复原核心，优先回原 Baubles 槽 =====
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
                    // 优先原槽位
                    if (slotIdx >= 0 && slotIdx < h.getSlots() && h.getStackInSlot(slotIdx).isEmpty()) {
                        h.setStackInSlot(slotIdx, core.copy());
                        placed = true;
                    } else {
                        // 找空槽
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
            // 放背包
            placed = newP.inventory.addItemStackToInventory(core.copy());
        }

        if (placed) {
            // 应急充能
            applyEmergencyCharge(core, newP);

            // 清除持久化的备份，避免复制
            persisted.removeTag(K_CORE_NBT);
            persisted.removeTag(K_CORE_IN_BAUBLES);
            persisted.removeTag(K_CORE_SLOT);

            newP.sendMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "⚙ 机械核心与你的灵魂绑定，已随你复生"));
        }
    }

    // ===== 修改后的惩罚逻辑（使用统一的损坏/修复机制） =====
    private static void applyDeathPunishment(ItemStack core, EntityPlayer player){
        // 收集"已安装"的模块
        List<String> installed = getInstalledUpgrades(core);
        installed.removeIf(SoulboundDeathHandler::isGeneratorModule);
        installed.removeIf(id -> ItemMechanicalCore.isUpgradePaused(core, id)); // 排除暂停的

        if (installed.isEmpty()){
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚠ 无可降级的活跃非发电模块"));
            return;
        }

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String target = installed.get(player.world.rand.nextInt(installed.size()));
        String upperId = target.toUpperCase(Locale.ROOT);

        // 获取当前的拥有等级
        int currentOwnedMax = getOwnedMax(core, target);
        if (currentOwnedMax <= 0) {
            // 如果没有 OwnedMax，以当前等级作为初始值
            currentOwnedMax = getCurrentLevel(core, target);
            if (currentOwnedMax > 0) {
                setOwnedMaxEverywhere(core, target, currentOwnedMax);
            }
        }

        // 第一次损坏时记录原始等级
        if (!nbt.hasKey(K_ORIGINAL_MAX + upperId) && currentOwnedMax > 0) {
            nbt.setInteger(K_ORIGINAL_MAX + upperId, currentOwnedMax);
            nbt.setInteger(K_ORIGINAL_MAX + target, currentOwnedMax);

            // 添加惩罚标记
            nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
            nbt.setBoolean(K_WAS_PUNISHED + target, true);
        }

        // 降级
        int newOwnedMax = Math.max(0, currentOwnedMax - 1);
        setOwnedMaxEverywhere(core, target, newOwnedMax);

        // 增加损坏计数（死亡损坏较轻，只加1）
        int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
        nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + 1);
        nbt.setInteger(K_DAMAGE_COUNT + target, damageCount + 1);

        // 调整当前等级（不能超过新的 OwnedMax）
        int currentLevel = getCurrentLevel(core, target);
        if (currentLevel > newOwnedMax) {
            setLevelEverywhere(core, target, newOwnedMax);
        }

        // 获取原始最大等级用于显示
        int originalMax = nbt.getInteger(K_ORIGINAL_MAX + upperId);
        if (originalMax <= 0) originalMax = currentOwnedMax; // 兜底

        // 发送死亡消息
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

        int target = es.getMaxEnergyStored() / 5; // 20%
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

    // ===== 工具：升级读写（含别名/所有系统） =====
    private static int getCurrentLevel(ItemStack core, String id){
        NBTTagCompound nbt = core.getTagCompound();
        int lv = 0;

        if (nbt != null){
            // 检查是否暂停
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

        // 防水别名互通
        if (v == 0 && isWaterproof(id)){
            for (String wid : WATERPROOF_IDS){
                v = Math.max(v, Math.max(nbt.getInteger("OwnedMax_" + wid),
                        Math.max(nbt.getInteger("OwnedMax_" + wid.toUpperCase(Locale.ROOT)),
                                nbt.getInteger("OwnedMax_" + wid.toLowerCase(Locale.ROOT)))));
            }
        }
        return v;
    }

    private static void setOwnedMaxEverywhere(ItemStack core, String id, int val){
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String[] vs = {id, id.toUpperCase(Locale.ROOT), id.toLowerCase(Locale.ROOT)};

        for (String k : vs) {
            nbt.setInteger("OwnedMax_" + k, val);
        }

        if (isWaterproof(id)) {
            for (String wid : WATERPROOF_IDS){
                nbt.setInteger("OwnedMax_" + wid, val);
                nbt.setInteger("OwnedMax_" + wid.toUpperCase(Locale.ROOT), val);
                nbt.setInteger("OwnedMax_" + wid.toLowerCase(Locale.ROOT), val);
            }
        }
    }

    private static void setLevelEverywhere(ItemStack core, String id, int val){
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String[] vs = {id, id.toUpperCase(Locale.ROOT), id.toLowerCase(Locale.ROOT)};

        for (String k : vs) {
            nbt.setInteger("upgrade_" + k, val);
        }

        // 旧枚举系统
        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)){
                    ItemMechanicalCore.setUpgradeLevel(core, t, val);
                    break;
                }
            }
        } catch (Throwable ignored) {}

        // 扩展系统
        try {
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id, val);
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id.toUpperCase(Locale.ROOT), val);
            ItemMechanicalCoreExtended.setUpgradeLevel(core, id.toLowerCase(Locale.ROOT), val);
        } catch (Throwable ignored) {}

        // 防水别名
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

        // 工具类（规范键）
        try {
            UpgradeKeys.setLevel(core, id, val);
        } catch (Throwable ignored) {}
    }

    private static List<String> getInstalledUpgrades(ItemStack core){
        Set<String> set = new HashSet<>();

        // 旧枚举
        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()){
                if (ItemMechanicalCore.getUpgradeLevel(core, t) > 0) {
                    set.add(t.getKey());
                }
            }
        } catch (Throwable ignored) {}

        // 扩展
        try {
            Map<String, ItemMechanicalCoreExtended.UpgradeInfo> all = ItemMechanicalCoreExtended.getAllUpgrades();
            for (Map.Entry<String, ItemMechanicalCoreExtended.UpgradeInfo> e : all.entrySet()){
                if (ItemMechanicalCoreExtended.getUpgradeLevel(core, e.getKey()) > 0) {
                    set.add(e.getKey());
                }
            }
        } catch (Throwable ignored) {}

        // NBT 兜底（HasUpgrade_ / upgrade_ / OwnedMax_）
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