package com.moremod.upgrades.platform;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.UpgradeType;
import com.moremod.upgrades.api.IUpgradeModule;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 模块升级物品基类
 *
 * 与 ModulePlatform 配套使用，简化模块物品创建。
 *
 * 使用方式：
 * <pre>{@code
 * public class SpeedModuleItem extends ModuleUpgradeItem {
 *     public SpeedModuleItem() {
 *         super(SpeedModule.INSTANCE, 1);
 *     }
 * }
 * }</pre>
 *
 * @author Module Platform
 * @see BaseUpgradeModule
 * @see ModulePlatform
 */
public class ModuleUpgradeItem extends Item {

    /** 关联的运行时模块 */
    protected final IUpgradeModule module;

    /** 此物品提供的升级等级数 */
    protected final int upgradeValue;

    /** 物品描述（可选） */
    protected String[] descriptions;

    /** 升级类型（用于兼容旧系统） */
    protected UpgradeType upgradeType;

    /**
     * 构造函数
     *
     * @param module 关联的运行时模块（必须已注册到 ModuleRegistry）
     * @param upgradeValue 此物品提供的升级等级数（通常为 1）
     */
    public ModuleUpgradeItem(@Nonnull IUpgradeModule module, int upgradeValue) {
        super();
        this.module = module;
        this.upgradeValue = upgradeValue;
        this.descriptions = new String[0];
        this.setMaxStackSize(16);

        // 尝试从模块ID匹配UpgradeType
        this.upgradeType = UpgradeType.fromString(module.getModuleId());
        if (this.upgradeType == null) {
            // 如果无法匹配，使用能量容量作为默认
            this.upgradeType = UpgradeType.ENERGY_CAPACITY;
        }
    }

    /**
     * 设置物品描述（用于tooltip）
     *
     * @param descriptions 描述文本数组
     * @return this（支持链式调用）
     */
    public ModuleUpgradeItem setDescriptions(String... descriptions) {
        this.descriptions = descriptions;
        return this;
    }

    /**
     * 设置升级类型（用于兼容旧系统）
     *
     * @param upgradeType 升级类型
     * @return this（支持链式调用）
     */
    public ModuleUpgradeItem setUpgradeType(@Nonnull UpgradeType upgradeType) {
        this.upgradeType = upgradeType;
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 模块信息
        tooltip.add(upgradeType.getColor() + "▶ " + module.getDisplayName() + " 模块");
        tooltip.add(TextFormatting.GRAY + "类别: " + upgradeType.getCategory().getColor() +
                upgradeType.getCategory().getName());

        // 最大等级
        tooltip.add(TextFormatting.GRAY + "最大等级: " + TextFormatting.WHITE + module.getMaxLevel());

        // 升级值
        if (upgradeValue > 1) {
            tooltip.add(TextFormatting.GOLD + "✦ 提供 " + upgradeValue + " 级升级");
        }

        tooltip.add("");

        // 自定义描述
        if (descriptions.length > 0) {
            for (String desc : descriptions) {
                tooltip.add(desc);
            }
        } else {
            // 使用模块的默认描述
            addModuleDescription(tooltip);
        }

        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "使用方法:");
        tooltip.add(TextFormatting.GRAY + "• 将机械核心装备到头部饰品栏");
        tooltip.add(TextFormatting.GRAY + "• 手持此组件右键升级");

        // 能量消耗信息
        addEnergyCostInfo(tooltip);
    }

    /**
     * 添加模块描述到tooltip（子类可重写）
     *
     * @param tooltip tooltip列表
     */
    @SideOnly(Side.CLIENT)
    protected void addModuleDescription(List<String> tooltip) {
        // 子类可以重写此方法以提供自定义描述
        tooltip.add(TextFormatting.GRAY + "升级 " + module.getDisplayName());
    }

    /**
     * 添加能量消耗信息到tooltip（子类可重写）
     *
     * @param tooltip tooltip列表
     */
    @SideOnly(Side.CLIENT)
    protected void addEnergyCostInfo(List<String> tooltip) {
        int baseCost = module.getPassiveEnergyCost(1);
        if (baseCost > 0) {
            tooltip.add("");
            tooltip.add(TextFormatting.AQUA + "能量消耗:");
            tooltip.add(TextFormatting.GRAY + "• 基础: " + baseCost + " RF/tick/级");

            if (module.getMaxLevel() > 1) {
                int maxCost = module.getPassiveEnergyCost(module.getMaxLevel());
                tooltip.add(TextFormatting.GRAY + "• 最高: " + maxCost + " RF/tick");
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack upgradeStack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            // 查找装备的机械核心
            ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(playerIn);

            if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.RED + "未找到装备的机械核心！请先装备到头部饰品栏。"
                ));
                return new ActionResult<>(EnumActionResult.FAIL, upgradeStack);
            }

            // 执行升级
            boolean success = performUpgrade(playerIn, coreStack, upgradeStack);

            if (success) {
                // 消耗升级组件
                if (!playerIn.isCreative()) {
                    upgradeStack.shrink(1);
                }

                // 播放升级效果
                playUpgradeEffect(worldIn, playerIn, coreStack);

                return new ActionResult<>(EnumActionResult.SUCCESS, upgradeStack);
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, upgradeStack);
    }

    /**
     * 执行升级逻辑
     *
     * @param player 玩家
     * @param coreStack 核心物品
     * @param upgradeStack 升级物品
     * @return 是否成功升级
     */
    protected boolean performUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack, @Nonnull ItemStack upgradeStack) {
        String moduleId = module.getModuleId();

        // 检查是否可以升级
        if (!canUpgrade(player, coreStack)) {
            return false;
        }

        // 应用升级
        boolean success = ItemMechanicalCoreExtended.addUpgradeLevel(coreStack, moduleId, upgradeValue);

        if (success) {
            int newLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, moduleId);

            // 发送升级消息
            onUpgradeSuccess(player, coreStack, newLevel);

            // 更新模块平台状态
            ModuleDataStorage.touchModule(coreStack, moduleId);
        }

        return success;
    }

    /**
     * 检查是否可以升级（子类可重写以添加自定义条件）
     *
     * @param player 玩家
     * @param coreStack 核心物品
     * @return 是否可以升级
     */
    protected boolean canUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        String moduleId = module.getModuleId();

        if (!ItemMechanicalCoreExtended.canUpgrade(coreStack, moduleId)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + module.getDisplayName() + " 已达到最大等级！"
            ));
            return false;
        }

        return true;
    }

    /**
     * 升级成功时调用（子类可重写以自定义消息）
     *
     * @param player 玩家
     * @param coreStack 核心物品
     * @param newLevel 新等级
     */
    protected void onUpgradeSuccess(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack, int newLevel) {
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + upgradeType.getColor() + module.getDisplayName() +
                        TextFormatting.WHITE + " 升级至 Lv." + newLevel
        ));
    }

    /**
     * 播放升级效果
     *
     * @param world 世界
     * @param player 玩家
     * @param coreStack 核心物品
     */
    protected void playUpgradeEffect(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
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

    // ===== Getter 方法 =====

    /**
     * 获取关联的运行时模块
     *
     * @return 运行时模块
     */
    @Nonnull
    public IUpgradeModule getModule() {
        return module;
    }

    /**
     * 获取模块ID
     *
     * @return 模块ID
     */
    @Nonnull
    public String getModuleId() {
        return module.getModuleId();
    }

    /**
     * 获取升级值
     *
     * @return 升级值
     */
    public int getUpgradeValue() {
        return upgradeValue;
    }

    /**
     * 获取升级类型
     *
     * @return 升级类型
     */
    @Nonnull
    public UpgradeType getUpgradeType() {
        return upgradeType;
    }
}
