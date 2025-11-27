package com.moremod.item.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.UpgradeType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 升级组件基类 - 支持統一的 UpgradeType 枚舉
 */
public class ItemUpgradeComponent extends Item {

    private final UpgradeType upgradeType;  // 使用新的枚舉類型
    private final String[] descriptions;
    private final int upgradeValue;
    private int stackSizeForDebug;

    // 特殊升级处理器
    private SpecialUpgradeHandler specialHandler;

    // 保留对字符串的引用（用于兼容）
    private final String upgradeTypeString;

    // 保留对旧枚举的引用（用于兼容）
    private ItemMechanicalCore.UpgradeType legacyType = null;

    /**
     * 特殊升级处理接口
     */
    public interface SpecialUpgradeHandler {
        boolean handleUpgrade(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack);
        boolean canApply(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack);
    }

    /**
     * 新构造函数 - 使用 UpgradeType 枚舉
     */
    public ItemUpgradeComponent(UpgradeType upgradeType, String[] descriptions, int upgradeValue) {
        super();
        this.upgradeType = upgradeType;
        this.upgradeTypeString = upgradeType.name();
        this.descriptions = descriptions;
        this.upgradeValue = upgradeValue;
        this.setMaxStackSize(16);

        // 嘗試匹配舊的枚舉類型
        tryMatchLegacyType();

        // 为特殊升级设置处理器
        setupSpecialHandlers();
    }

    /**
     * 舊构造函数 - 支持字符串类型（用於兼容）
     */
    public ItemUpgradeComponent(String upgradeTypeStr, String[] descriptions, int upgradeValue) {
        super();
        this.upgradeTypeString = upgradeTypeStr;
        this.upgradeType = UpgradeType.fromString(upgradeTypeStr);
        this.descriptions = descriptions;
        this.upgradeValue = upgradeValue;
        this.setMaxStackSize(16);

        // 嘗試匹配舊的枚舉類型
        tryMatchLegacyType();

        // 为特殊升级设置处理器
        setupSpecialHandlers();
    }

    /**
     * 旧构造函数 - 兼容枚举类型
     */
    public ItemUpgradeComponent(ItemMechanicalCore.UpgradeType type, String[] descriptions, int upgradeValue) {
        super();
        this.legacyType = type;
        this.upgradeTypeString = type.getKey();
        this.upgradeType = matchNewUpgradeType(type);
        this.descriptions = descriptions;
        this.upgradeValue = upgradeValue;
        this.setMaxStackSize(16);

        // 为特殊升级设置处理器
        setupSpecialHandlers();
    }

    /**
     * 嘗試匹配舊的枚舉類型
     */
    private void tryMatchLegacyType() {
        if (legacyType == null && upgradeType != null) {
            // 嘗試通過名稱匹配
            for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                if (type.name().equals(upgradeType.name()) ||
                        type.getKey().equals(upgradeTypeString)) {
                    legacyType = type;
                    break;
                }
            }
        }
    }

    /**
     * 將舊的 UpgradeType 匹配到新的系統
     */
    private UpgradeType matchNewUpgradeType(ItemMechanicalCore.UpgradeType oldType) {
        // 嘗試直接匹配名稱
        UpgradeType newType = UpgradeType.fromString(oldType.name());
        if (newType != null) {
            return newType;
        }

        // 如果無法匹配，返回能量容量作為默認
        return UpgradeType.ENERGY_CAPACITY;
    }

    /**
     * 設置特殊升級處理器
     */
    private void setupSpecialHandlers() {
        // 飛行模塊的特殊處理
        if (upgradeType == UpgradeType.FLIGHT_MODULE ||
                "FLIGHT_MODULE".equals(upgradeTypeString) ||
                "flight_module".equals(upgradeTypeString)) {
            this.specialHandler = new FlightModuleHandler();
        }
        // 套裝升級的特殊處理
        else if (getRegistryName() != null && getRegistryName().toString().contains("package")) {
            this.specialHandler = new PackageUpgradeHandler();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (upgradeType != null) {
            tooltip.add(upgradeType.getColor() + "▶ " + upgradeType.getDisplayName() + " 升級組件");
            tooltip.add(TextFormatting.GRAY + "類別: " + upgradeType.getCategory().getColor() +
                    upgradeType.getCategory().getName());

            // 獲取最大等級信息
            ItemMechanicalCoreExtended.UpgradeInfo info =
                    ItemMechanicalCoreExtended.getUpgradeInfo(upgradeTypeString);
            if (info != null) {
                tooltip.add(TextFormatting.GRAY + "最大等級: " + TextFormatting.WHITE + info.maxLevel);
            }

            if (upgradeValue > 1) {
                tooltip.add(TextFormatting.GOLD + "✦ 提供 " + upgradeValue + " 級升級");
            }
        }

        tooltip.add("");

        // 添加描述
        for (String desc : descriptions) {
            tooltip.add(desc);
        }

        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "使用方法:");
        tooltip.add(TextFormatting.GRAY + "• 將機械核心裝備到頭部飾品欄");
        tooltip.add(TextFormatting.GRAY + "• 手持此組件右鍵升級");

        // 特殊升級的額外提示
        if (specialHandler != null) {
            tooltip.add("");
            if (specialHandler instanceof FlightModuleHandler) {
                tooltip.add(TextFormatting.LIGHT_PURPLE + "✦ 特殊升級：需要對應等級");
            } else if (specialHandler instanceof PackageUpgradeHandler) {
                tooltip.add(TextFormatting.DARK_PURPLE + "✦ 套裝升級：同時提升多項能力");
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack upgradeStack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            // 查找裝備的機械核心
            ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(playerIn);

            if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.RED + "未找到裝備的機械核心！請先裝備到頭部飾品欄。"
                ));
                return new ActionResult<>(EnumActionResult.FAIL, upgradeStack);
            }

            // 使用特殊處理器或默認處理
            boolean success = false;
            if (specialHandler != null) {
                if (specialHandler.canApply(playerIn, coreStack, upgradeStack)) {
                    success = specialHandler.handleUpgrade(playerIn, coreStack, upgradeStack);
                } else {
                    return new ActionResult<>(EnumActionResult.FAIL, upgradeStack);
                }
            } else {
                success = applyDefaultUpgrade(playerIn, coreStack, upgradeStack);
            }

            if (success) {
                // 消耗升級組件
                if (!playerIn.isCreative()) {
                    upgradeStack.shrink(1);
                }

                // 播放升級效果
                playUpgradeEffect(worldIn, playerIn, coreStack);

                return new ActionResult<>(EnumActionResult.SUCCESS, upgradeStack);
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, upgradeStack);
    }

    /**
     * 默認升級處理
     */
    private boolean applyDefaultUpgrade(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack) {
        // 檢查是否可以升級
        if (!ItemMechanicalCoreExtended.canUpgrade(coreStack, upgradeTypeString)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + upgradeType.getDisplayName() + " 已達到最大等級！"
            ));
            return false;
        }

        // 應用升級
        boolean success = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, upgradeTypeString, upgradeValue);

        if (success) {
            int newLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, upgradeTypeString);

            // 發送升級消息
            if (legacyType != null) {
                com.moremod.upgrades.UpgradeEffectManager.playUpgradeEffect(
                        player,
                        legacyType,
                        newLevel
                );
            }

            // 額外的升級消息
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ " + upgradeType.getColor() + upgradeType.getDisplayName() +
                            TextFormatting.WHITE + " 升級至 Lv." + newLevel
            ));
        }

        return success;
    }

    /**
     * 播放升級效果
     */
    private void playUpgradeEffect(World world, EntityPlayer player, ItemStack coreStack) {
        // 播放音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                net.minecraft.util.SoundCategory.PLAYERS,
                1.0F, 1.0F + world.rand.nextFloat() * 0.2F
        );

        // 粒子效果
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 2;
            double offsetY = world.rand.nextDouble() * 2;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 2;

            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.PORTAL,
                    player.posX + offsetX,
                    player.posY + offsetY,
                    player.posZ + offsetZ,
                    0, 0.1, 0
            );
        }
    }

    // ===== 特殊升級處理器實現 =====

    /**
     * 飛行模塊處理器 - 修復版本
     */
    private class FlightModuleHandler implements SpecialUpgradeHandler {
        @Override
        public boolean canApply(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack) {
            // 獲取當前飛行模塊等級 - 檢查多個可能的key
            int currentLevel = 0;

            // 檢查舊系統的等級
            currentLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);

            // 如果舊系統沒有，檢查新系統
            if (currentLevel == 0) {
                currentLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "FLIGHT_MODULE");
            }

            // 再檢查小寫的key
            if (currentLevel == 0) {
                currentLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "flight_module");
            }

            String registryName = getRegistryName() != null ? getRegistryName().toString() : "";

            // 基礎飛行模塊 - 只能在等級0時安裝
            if (registryName.contains("flight_module_basic")) {
                if (currentLevel > 0) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "已安裝飛行模塊！當前等級: " + currentLevel
                    ));
                    return false;
                }
                return true;
            }

            // 高級飛行模塊 - 需要等級1
            if (registryName.contains("flight_module_advanced")) {
                if (currentLevel == 0) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "需要先安裝基礎飛行模塊！"
                    ));
                    return false;
                }
                if (currentLevel >= 2) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "已安裝高級或更高級的飛行模塊！"
                    ));
                    return false;
                }
                return true;
            }

            // 終極飛行模塊 - 需要等級2
            if (registryName.contains("flight_module_ultimate")) {
                if (currentLevel < 2) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "需要先安裝高級飛行模塊！當前等級: " + currentLevel
                    ));
                    return false;
                }
                if (currentLevel >= 3) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "已達到最高等級！"
                    ));
                    return false;
                }
                return true;
            }

            // 默認情況
            return false;
        }

        @Override
        public boolean handleUpgrade(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack) {
            String registryName = getRegistryName() != null ? getRegistryName().toString() : "";
            int targetLevel = 1;

            if (registryName.contains("flight_module_basic")) {
                targetLevel = 1;
            } else if (registryName.contains("flight_module_advanced")) {
                targetLevel = 2;
            } else if (registryName.contains("flight_module_ultimate")) {
                targetLevel = 3;
            }

            // 同時設置到多個系統以確保兼容性

            // 設置到舊系統
            ItemMechanicalCore.setUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE, targetLevel);

            // 設置到新系統（大寫）
            ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "FLIGHT_MODULE", targetLevel);

            // 也設置小寫版本以防萬一
            ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "flight_module", targetLevel);

            // 確保NBT標籤也正確設置
            net.minecraft.nbt.NBTTagCompound nbt = coreStack.getTagCompound();
            if (nbt == null) {
                nbt = new net.minecraft.nbt.NBTTagCompound();
                coreStack.setTagCompound(nbt);
            }
            nbt.setInteger("upgrade_flight_module", targetLevel);
            nbt.setInteger("upgrade_FLIGHT_MODULE", targetLevel);

            // 初始化飛行模塊狀態
            if (!nbt.hasKey("FlightModuleEnabled")) {
                nbt.setBoolean("FlightModuleEnabled", true);
            }

            // 特殊消息
            switch (targetLevel) {
                case 1:
                    player.sendMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "✦ 飛行系統已激活！按V鍵起飛！"
                    ));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "提示：裝備核心後，按住空格上升，Shift下降"
                    ));
                    break;
                case 2:
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "✦ 飛行系統升級！懸停模式已解鎖！"
                    ));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "提示：按V鍵切換懸停模式"
                    ));
                    break;
                case 3:
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_PURPLE + "✦✦ 終極飛行系統已啟動！速度模式已解鎖！"
                    ));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "提示：按C鍵切換速度模式"
                    ));
                    break;
            }

            // 調試信息
            System.out.println("[FlightModule] Upgraded to level " + targetLevel + " for player " + player.getName());
            System.out.println("[FlightModule] Current level check: " +
                    ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE));

            return true;
        }
    }

    /**
     * 套裝升級處理器
     */
    /**
     * 套裝升級處理器 - 完整版本
     */
    private class PackageUpgradeHandler implements SpecialUpgradeHandler {
        @Override
        public boolean canApply(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack) {
            // 套裝升級總是可以應用（除非某個組件已滿級）
            // 這裡可以添加更詳細的檢查邏輯
            String registryName = getRegistryName() != null ? getRegistryName().toString() : "";

            // 檢查是否還有可升級的空間
            if (registryName.contains("omnipotent")) {
                // 檢查三個升級是否都已滿級
                boolean canUpgradeEnergy = ItemMechanicalCoreExtended.canUpgrade(coreStack, "ENERGY_CAPACITY");
                boolean canUpgradeEfficiency = ItemMechanicalCoreExtended.canUpgrade(coreStack, "ENERGY_EFFICIENCY");
                boolean canUpgradeArmor = ItemMechanicalCoreExtended.canUpgrade(coreStack, "ARMOR_ENHANCEMENT");

                if (!canUpgradeEnergy && !canUpgradeEfficiency && !canUpgradeArmor) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "全能芯片的所有升級項目都已達到最大等級！"
                    ));
                    return false;
                }
                return true;
            } else if (registryName.contains("survival")) {
                // 檢查生存套裝的升級
                boolean canUpgradeShield = ItemMechanicalCoreExtended.canUpgrade(coreStack, "YELLOW_SHIELD");
                boolean canUpgradeRegen = ItemMechanicalCoreExtended.canUpgrade(coreStack, "HEALTH_REGEN");
                boolean canUpgradeHunger = ItemMechanicalCoreExtended.canUpgrade(coreStack, "HUNGER_THIRST");

                if (!canUpgradeShield && !canUpgradeRegen && !canUpgradeHunger) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "生存套裝的所有升級項目都已達到最大等級！"
                    ));
                    return false;
                }
                return true;
            } else if (registryName.contains("combat")) {
                // 檢查戰鬥套裝的升級
                boolean canUpgradeDamage = ItemMechanicalCoreExtended.canUpgrade(coreStack, "DAMAGE_BOOST");
                boolean canUpgradeSpeed = ItemMechanicalCoreExtended.canUpgrade(coreStack, "ATTACK_SPEED");
                boolean canUpgradeRange = ItemMechanicalCoreExtended.canUpgrade(coreStack, "RANGE_EXTENSION");

                if (!canUpgradeDamage && !canUpgradeSpeed && !canUpgradeRange) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "戰鬥套裝的所有升級項目都已達到最大等級！"
                    ));
                    return false;
                }
                return true;
            }

            // 默認情況下允許應用
            return true;
        }

        @Override
        public boolean handleUpgrade(EntityPlayer player, ItemStack coreStack, ItemStack upgradeStack) {
            boolean anySuccess = false;
            String registryName = getRegistryName() != null ? getRegistryName().toString() : "";

            // 根據套裝類型應用多個升級
            if (registryName.contains("omnipotent")) {
                // 全能強化芯片 - 同時提升三種能力
                boolean energySuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "ENERGY_CAPACITY", 1);
                boolean efficiencySuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "ENERGY_EFFICIENCY", 1);
                boolean armorSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "ARMOR_ENHANCEMENT", 1);

                anySuccess = energySuccess || efficiencySuccess || armorSuccess;

                if (anySuccess) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "✦ 全能強化芯片已應用！"
                    ));

                    // 詳細顯示哪些升級成功了
                    StringBuilder successMsg = new StringBuilder(TextFormatting.GOLD.toString());
                    if (energySuccess) successMsg.append("能量容量 ");
                    if (efficiencySuccess) successMsg.append("能量效率 ");
                    if (armorSuccess) successMsg.append("護甲強化 ");
                    successMsg.append("已提升！");

                    player.sendMessage(new TextComponentString(successMsg.toString()));

                    // 如果有升級達到滿級，提示
                    if (!energySuccess || !efficiencySuccess || !armorSuccess) {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.YELLOW + "部分升級已達到最大等級"
                        ));
                    }
                }
            } else if (registryName.contains("survival")) {
                // 生存套裝
                boolean shieldSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "YELLOW_SHIELD", 1);
                boolean regenSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "HEALTH_REGEN", 1);
                boolean hungerSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "HUNGER_THIRST", 1);

                anySuccess = shieldSuccess || regenSuccess || hungerSuccess;

                if (anySuccess) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✦ 生存強化套裝已應用！"
                    ));

                    StringBuilder successMsg = new StringBuilder(TextFormatting.GRAY.toString());
                    if (shieldSuccess) successMsg.append("護盾 ");
                    if (regenSuccess) successMsg.append("生命恢復 ");
                    if (hungerSuccess) successMsg.append("飢餓管理 ");
                    successMsg.append("已提升！");

                    player.sendMessage(new TextComponentString(successMsg.toString()));
                }
            } else if (registryName.contains("combat")) {
                // 戰鬥套裝
                boolean damageSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "DAMAGE_BOOST", 1);
                boolean speedSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "ATTACK_SPEED", 1);
                boolean rangeSuccess = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, "RANGE_EXTENSION", 1);

                anySuccess = damageSuccess || speedSuccess || rangeSuccess;

                if (anySuccess) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "✦ 戰鬥強化套裝已應用！"
                    ));

                    StringBuilder successMsg = new StringBuilder(TextFormatting.GRAY.toString());
                    if (damageSuccess) successMsg.append("傷害加成 ");
                    if (speedSuccess) successMsg.append("攻擊速度 ");
                    if (rangeSuccess) successMsg.append("攻擊範圍 ");
                    successMsg.append("已提升！");

                    player.sendMessage(new TextComponentString(successMsg.toString()));
                }
            }

            if (!anySuccess) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "套裝中的所有升級都已達到最大等級！"
                ));
            }

            return anySuccess;
        }
    }

    // Getter方法
    public String getUpgradeType() {
        return upgradeTypeString;
    }

    /**
     * 獲取新的 UpgradeType 枚舉
     */
    public UpgradeType getUpgradeTypeEnum() {
        return upgradeType;
    }

    /**
     * 獲取舊版升級類型（用於兼容）
     */
    public ItemMechanicalCore.UpgradeType getLegacyUpgradeType() {
        return legacyType != null ? legacyType : ItemMechanicalCore.UpgradeType.ENERGY_CAPACITY;
    }

    public int getUpgradeValue() {
        return upgradeValue;
    }

    public void setStackSizeForDebug(int size) {
        this.stackSizeForDebug = size;
    }

    public int getStackSizeForDebug() {
        return stackSizeForDebug;
    }
}