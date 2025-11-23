package com.moremod.eventHandler;

import com.moremod.moremod;
import com.moremod.capability.IMechCoreData;
import com.moremod.capability.MechCoreDataProvider;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.config.EnergyBalanceConfig;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Capability 生命周期事件处理器
 *
 * 职责：
 *  ✓ Attach - 附加 Capability 到玩家
 *  ✓ Clone - 死亡复制数据
 *  ✓ Save/Load - 存档序列化（由 Provider 自动处理）
 */
public class CapabilityEventHandler {

    private static final Logger logger = LogManager.getLogger(CapabilityEventHandler.class);

    private static final ResourceLocation MECH_CORE_CAP_ID = new ResourceLocation(
        moremod.MODID,
        "mech_core_data"
    );

    /**
     * 附加 Capability 到玩家实体
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getObject();

            // 附加 MechCoreData Capability
            event.addCapability(
                MECH_CORE_CAP_ID,
                new MechCoreDataProvider()
            );
        }
    }

    /**
     * 玩家死亡/重生时复制 Capability 数据
     *
     * 机械核心永不丢失
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        IMechCoreData oldData = oldPlayer.getCapability(IMechCoreData.CAPABILITY, null);
        IMechCoreData newData = newPlayer.getCapability(IMechCoreData.CAPABILITY, null);

        if (oldData != null && newData != null) {
            // 复制所有数据
            newData.copyFrom(oldData);

            // 标记为需要同步
            newData.markDirty();

            if (!newPlayer.world.isRemote) {
                logger.debug(
                    "Cloned MechCoreData for player: {}",
                    newPlayer.getName()
                );
            }
        }
    }

    /**
     * 玩家登录时从ItemStack NBT初始化Capability数据
     *
     * 解决退出游戏后升级数据丢失的问题
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (!player.world.isRemote) {
            // 查找装备的机械核心
            ItemStack coreStack = findMechanicalCore(player);

            if (!coreStack.isEmpty() && ItemMechanicalCore.isMechanicalCore(coreStack)) {
                IMechCoreData capData = player.getCapability(IMechCoreData.CAPABILITY, null);

                if (capData != null && coreStack.hasTagCompound()) {
                    NBTTagCompound nbt = coreStack.getTagCompound();

                    // 从ItemStack NBT加载升级数据到Capability
                    if (nbt.hasKey("MechanicalCoreData")) {
                        NBTTagCompound mechData = nbt.getCompoundTag("MechanicalCoreData");

                        // 加载所有基础升级
                        for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                            String key = type.getKey();
                            if (mechData.hasKey(key)) {
                                int level = mechData.getInteger(key);
                                if (level > 0) {
                                    capData.setModuleLevel(key, level);
                                }
                            }
                        }

                        // 标记为需要同步
                        capData.markDirty();

                        logger.info(
                            "Loaded MechanicalCore data from ItemStack NBT for player: {}",
                            player.getName()
                        );
                    }

                    // 从旧的 NBT 格式迁移 OriginalMax 到 Capability
                    for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                        String key = type.getKey();
                        String upperId = key.toUpperCase();

                        // 尝试从多个变体读取 OriginalMax
                        int originalMax = Math.max(
                            nbt.getInteger("OriginalMax_" + key),
                            Math.max(
                                nbt.getInteger("OriginalMax_" + upperId),
                                nbt.getInteger("OriginalMax_" + key.toLowerCase())
                            )
                        );

                        if (originalMax > 0) {
                            capData.setOriginalMaxLevel(upperId, originalMax);
                        }
                    }

                    // ✅ 计算并更新 maxEnergy（基于 ENERGY_CAPACITY 等级）
                    updateMaxEnergyFromCapability(capData, nbt);
                }
            }
        }
    }

    /**
     * 查找玩家装备的机械核心
     */
    private ItemStack findMechanicalCore(EntityPlayer player) {
        // 检查 Baubles 饰品栏
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (ItemMechanicalCore.isMechanicalCore(stack)) {
                    return stack;
                }
            }
        }

        // 检查主副手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (ItemMechanicalCore.isMechanicalCore(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (ItemMechanicalCore.isMechanicalCore(offHand)) {
            return offHand;
        }

        // 检查物品栏
        for (ItemStack stack : player.inventory.mainInventory) {
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 定期将Capability数据同步到ItemStack NBT（每60秒）
     *
     * 防止数据丢失
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // 每60秒（1200 ticks）同步一次
        if (player.world.getTotalWorldTime() % 1200 != 0) return;

        syncCapabilityToNBT(player);
    }

    /**
     * 将Capability数据同步到ItemStack NBT的通用方法
     */
    private void syncCapabilityToNBT(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);

        if (!coreStack.isEmpty() && ItemMechanicalCore.isMechanicalCore(coreStack)) {
            IMechCoreData capData = player.getCapability(IMechCoreData.CAPABILITY, null);

            if (capData != null) {
                // ✅ 定期更新 maxEnergy（确保能量容量模块生效）
                NBTTagCompound nbt = coreStack.hasTagCompound() ? coreStack.getTagCompound() : null;
                updateMaxEnergyFromCapability(capData, nbt);

                if (capData.isDirty()) {
                    // 保存Capability数据到ItemStack NBT
                    if (!coreStack.hasTagCompound()) {
                        coreStack.setTagCompound(new NBTTagCompound());
                    }

                    NBTTagCompound nbtData = coreStack.getTagCompound();
                    NBTTagCompound mechData = new NBTTagCompound();

                    // 保存所有基础升级
                    for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                        String key = type.getKey();
                        int level = capData.getModuleLevel(key);
                        if (level > 0) {
                            mechData.setInteger(key, level);
                        }
                    }

                    nbtData.setTag("MechanicalCoreData", mechData);

                    // ✅ 同步 OriginalMax 到 NBT（只写大写变体，减少写入开销）
                    for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                        String key = type.getKey();
                        String upperId = key.toUpperCase();
                        int originalMax = capData.getOriginalMaxLevel(upperId);

                        if (originalMax > 0) {
                            nbtData.setInteger("OriginalMax_" + upperId, originalMax);
                            // ✅ 只写大写变体（读取时会回退到多变体兼容）
                        }
                    }

                    // 清除dirty标记
                    capData.clearDirty();

                    logger.debug(
                        "Synced MechanicalCore Capability to ItemStack NBT for player: {}",
                        player.getName()
                    );
                }
            }
        }
    }

    /**
     * 玩家登出时将Capability数据保存回ItemStack NBT
     *
     * 确保数据持久化
     */
    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        if (!player.world.isRemote) {
            // 强制同步一次，无论是否标记为dirty
            ItemStack coreStack = findMechanicalCore(player);

            if (!coreStack.isEmpty() && ItemMechanicalCore.isMechanicalCore(coreStack)) {
                IMechCoreData capData = player.getCapability(IMechCoreData.CAPABILITY, null);

                if (capData != null) {
                    // 保存Capability数据到ItemStack NBT
                    if (!coreStack.hasTagCompound()) {
                        coreStack.setTagCompound(new NBTTagCompound());
                    }

                    NBTTagCompound nbt = coreStack.getTagCompound();
                    NBTTagCompound mechData = new NBTTagCompound();

                    // 保存所有基础升级
                    for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                        String key = type.getKey();
                        int level = capData.getModuleLevel(key);
                        if (level > 0) {
                            mechData.setInteger(key, level);
                        }
                    }

                    nbt.setTag("MechanicalCoreData", mechData);

                    // 保存 OriginalMax 到 NBT（向后兼容）
                    // ✅ 保存 OriginalMax 到 NBT（只写大写变体）
                    for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                        String key = type.getKey();
                        String upperId = key.toUpperCase();
                        int originalMax = capData.getOriginalMaxLevel(upperId);

                        if (originalMax > 0) {
                            nbt.setInteger("OriginalMax_" + upperId, originalMax);
                            // ✅ 只写大写变体（减少 67% 的写入操作）
                        }
                    }

                    logger.info(
                        "Saved MechanicalCore data to ItemStack NBT on logout for player: {}",
                        player.getName()
                    );
                }
            }

            logger.debug(
                "Player logout: {} (Capability will be auto-cleaned)",
                player.getName()
            );
        }
    }

    /**
     * 根据 ENERGY_CAPACITY 等级更新 Capability 的 maxEnergy
     */
    private void updateMaxEnergyFromCapability(IMechCoreData capData, NBTTagCompound nbt) {
        if (capData == null) return;

        // 获取 ENERGY_CAPACITY 等级（优先从 Capability，降级到 NBT）
        int capacityLevel = capData.getModuleLevel("ENERGY_CAPACITY");
        if (capacityLevel == 0) {
            capacityLevel = capData.getModuleLevel("energy_capacity");
        }

        // 如果 Capability 中没有，尝试从 NBT 读取（兼容性）
        if (capacityLevel == 0 && nbt != null) {
            // 检查是否被禁用或暂停
            if (!nbt.getBoolean("Disabled_energy_capacity") && !nbt.getBoolean("IsPaused_energy_capacity")) {
                capacityLevel = nbt.getInteger("upgrade_energy_capacity");
            }
        }

        // 计算 maxEnergy
        int maxEnergy = EnergyBalanceConfig.BASE_ENERGY_CAPACITY
                      + capacityLevel * EnergyBalanceConfig.ENERGY_PER_CAPACITY_LEVEL;

        // 更新到 Capability
        capData.setMaxEnergy(maxEnergy);

        logger.debug("Updated maxEnergy: {} (capacityLevel: {})", maxEnergy, capacityLevel);
    }
}
