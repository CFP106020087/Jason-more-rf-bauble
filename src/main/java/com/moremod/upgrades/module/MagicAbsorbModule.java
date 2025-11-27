package com.moremod.upgrades.module;

import com.moremod.upgrades.api.IUpgradeModule;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 魔力吸收模块
 *
 * 说明：
 *  - 真正的“魔力→物理伤害转换”逻辑在 EventHandler 里处理
 *  - 这里只定义：模块ID、名称、最大等级、能量、装备/卸下提示等
 */
public class MagicAbsorbModule implements IUpgradeModule {

    public static final MagicAbsorbModule INSTANCE = new MagicAbsorbModule();

    @Override
    public String getModuleId() {
        return "MAGIC_ABSORB";
    }

    @Override
    public String getDisplayName() {
        return "魔力吸收模块";
    }

    @Override
    public int getMaxLevel() {
        // ✅ 支持多等级，比如 3 级
        return 3;
    }

    @Override
    public void onTick(EntityPlayer player, ItemStack core, int level) {
        // 目前没有被动tick效果，先留空
    }

    @Override
    public void onEquip(EntityPlayer player, ItemStack core, int level) {
        if (player.world.isRemote) return;

        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 魔力吸收模块已安装 (等级 " + level + ")"
        ));
    }

    @Override
    public void onUnequip(EntityPlayer player, ItemStack core, int level) {
        if (player.world.isRemote) return;

        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "魔力吸收模块已卸下"
        ));
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 如果以后觉得太强，可以按等级增加耗能
        // 例如 return 10 * level;
        return 0;
    }

    @Override
    public boolean canRunWithEnergyStatus(EnergyDepletionManager.EnergyStatus status) {
        // 先设定为在任何能量状态都能工作
        return true;
    }

    @Override
    public void handleEvent(Event event, EntityPlayer player, ItemStack core, int level) {
        // 魔力吸收完全交给 EventHandler 处理，这里不做事件逻辑
    }
}
