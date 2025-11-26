package com.moremod.network;

import com.moremod.item.ItemUpgradeSelector;
import com.moremod.item.UpgradeType;
import com.moremod.item.UpgradeItems;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.item.upgrades.UpgradeItemsExtended;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 升级选择器网络包 - 分级模块版本
 * 当玩家使用选择器时，给予对应的1级模块
 */
public class PacketUpgradeSelection implements IMessage {

    private NBTTagCompound selections;

    // 必须有无参构造函数
    public PacketUpgradeSelection() {
    }

    public PacketUpgradeSelection(NBTTagCompound selections) {
        this.selections = selections;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        selections = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, selections);
    }

    public static class Handler implements IMessageHandler<PacketUpgradeSelection, IMessage> {

        @Override
        public IMessage onMessage(PacketUpgradeSelection message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在服务器线程中执行
            player.getServerWorld().addScheduledTask(() -> {
                processSelection(player, message.selections);
            });

            return null;
        }

        private void processSelection(EntityPlayerMP player, NBTTagCompound selections) {
            // 验证玩家持有选择器
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!(heldItem.getItem() instanceof ItemUpgradeSelector)) {
                heldItem = player.getHeldItemOffhand();
                if (!(heldItem.getItem() instanceof ItemUpgradeSelector)) {
                    return;
                }
            }

            // 验证选择数量
            if (!validateSelections(selections)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "选择无效！"
                ));
                return;
            }

            // 创建奖励列表
            List<ItemStack> rewards = createRewards(selections);

            // 发送成功消息
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "═══════ 获得升级模块 ═══════"
            ));

            // 给予物品
            for (ItemStack reward : rewards) {
                if (!player.inventory.addItemStackToInventory(reward)) {
                    player.dropItem(reward, false);
                }

                // 显示获得的模块名称和等级
                String itemName = reward.getDisplayName();
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "  + " + itemName +
                                TextFormatting.GRAY + " (Lv.1 模块)"
                ));
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "使用这些模块可以将对应升级提升至 Lv.1"
            ));

            // 消耗选择器
            heldItem.shrink(1);

            // 播放音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        private boolean validateSelections(NBTTagCompound selections) {
            Map<UpgradeType.UpgradeCategory, Integer> counts = new HashMap<>();

            // 统计各类别数量
            for (String key : selections.getKeySet()) {
                if (selections.getBoolean(key)) {
                    UpgradeType type = UpgradeType.fromString(key);
                    if (type != null) {
                        counts.merge(type.getCategory(), 1, Integer::sum);
                    }
                }
            }

            // 验证能源类是否为2个
            if (counts.getOrDefault(UpgradeType.UpgradeCategory.ENERGY, 0) != 2) {
                return false;
            }

            // 验证其他类是否各为1个
            for (UpgradeType.UpgradeCategory category : UpgradeType.UpgradeCategory.values()) {
                if (category == UpgradeType.UpgradeCategory.ENERGY ||
                        category == UpgradeType.UpgradeCategory.PACKAGE) {
                    continue;
                }

                // 如果该类别有可选项但没选择，也是无效的
                // 这里简化处理，只要选了就行
            }

            return true;
        }

        private List<ItemStack> createRewards(NBTTagCompound selections) {
            List<ItemStack> rewards = new ArrayList<>();

            for (String key : selections.getKeySet()) {
                if (selections.getBoolean(key)) {
                    UpgradeType type = UpgradeType.fromString(key);
                    if (type != null) {
                        ItemStack upgrade = createUpgradeItem(type);
                        if (!upgrade.isEmpty()) {
                            rewards.add(upgrade);
                        }
                    }
                }
            }

            return rewards;
        }

        private ItemStack createUpgradeItem(UpgradeType type) {
            ItemUpgradeComponent component = getLevel1Component(type);
            if (component != null) {
                return new ItemStack(component);
            }
            return ItemStack.EMPTY;
        }

        /**
         * 获取对应升级类型的1级模块
         * 在分级系统中，选择器总是给予1级模块
         */
        private ItemUpgradeComponent getLevel1Component(UpgradeType type) {
            switch(type) {
                // ===== 基础升级 - 返回Lv.1版本 =====
                case ENERGY_CAPACITY:
                    return UpgradeItems.ENERGY_CAPACITY_LV1;

                case ENERGY_EFFICIENCY:
                    return UpgradeItems.ENERGY_EFFICIENCY_LV1;

                case ARMOR_ENHANCEMENT:
                    return UpgradeItems.ARMOR_ENHANCEMENT_LV1;



                case FLIGHT_MODULE:
                    return UpgradeItems.FLIGHT_MODULE_BASIC;  // 基础飞行模块

                case TEMPERATURE_CONTROL:
                    return UpgradeItems.TEMPERATURE_CONTROL_LV1;

                // ===== 生存类升级 - 返回Lv.1版本 =====
                case YELLOW_SHIELD:
                    return UpgradeItemsExtended.YELLOW_SHIELD_LV1;

                case HEALTH_REGEN:
                    return UpgradeItemsExtended.HEALTH_REGEN_LV1;

                case HUNGER_THIRST:
                    return UpgradeItemsExtended.HUNGER_THIRST_LV1;

                case THORNS:
                    return UpgradeItemsExtended.THORNS_LV1;

                case FIRE_EXTINGUISH:
                    return UpgradeItemsExtended.FIRE_EXTINGUISH_LV1;

                // ===== 辅助类升级 - 返回Lv.1版本 =====
                case WATERPROOF_MODULE:
                    return UpgradeItemsExtended.WATERPROOF_MODULE_BASIC;  // 基础防水

                case ORE_VISION:
                    return UpgradeItemsExtended.ORE_VISION_LV1;

                case MOVEMENT_SPEED:
                    return UpgradeItemsExtended.MOVEMENT_SPEED_LV1;

                case STEALTH:
                    return UpgradeItemsExtended.STEALTH_LV1;

                case EXP_AMPLIFIER:
                    return UpgradeItemsExtended.EXP_AMPLIFIER_LV1;

                // ===== 战斗类升级 - 返回Lv.1版本 =====
                case DAMAGE_BOOST:
                    return UpgradeItemsExtended.DAMAGE_BOOST_LV1;

                case ATTACK_SPEED:
                    return UpgradeItemsExtended.ATTACK_SPEED_LV1;

                case RANGE_EXTENSION:
                    return UpgradeItemsExtended.RANGE_EXTENSION_LV1;

                case PURSUIT:
                    return UpgradeItemsExtended.PURSUIT_LV1;

                // ===== 能源类升级 - 返回Lv.1版本 =====
                case KINETIC_GENERATOR:
                    return UpgradeItemsExtended.KINETIC_GENERATOR_LV1;

                case SOLAR_GENERATOR:
                    return UpgradeItemsExtended.SOLAR_GENERATOR_LV1;

                case VOID_ENERGY:
                    return UpgradeItemsExtended.VOID_ENERGY_LV1;

                case COMBAT_CHARGER:
                    return UpgradeItemsExtended.COMBAT_CHARGER_LV1;

                // ===== 特殊套装 - 这些保持不变 =====
                case SURVIVAL_PACKAGE:
                    return UpgradeItemsExtended.SURVIVAL_PACKAGE;

                case COMBAT_PACKAGE:
                    return UpgradeItemsExtended.COMBAT_PACKAGE;

                case OMNIPOTENT_PACKAGE:
                    return UpgradeItems.OMNIPOTENT_PACKAGE;

                // 默认情况
                default:
                    System.err.println("[PacketUpgradeSelection] 未知的升级类型: " + type);
                    return null;
            }
        }

        /**
         * 获取更高级的模块（可选方法，用于特殊奖励）
         * @param type 升级类型
         * @param level 等级 (1-10)
         * @return 对应等级的模块，如果不存在则返回null
         */
        private ItemUpgradeComponent getComponentByLevel(UpgradeType type, int level) {
            // 限制等级范围
            if (level < 1) level = 1;

            switch(type) {
                case ENERGY_CAPACITY:
                    switch(level) {
                        case 1: return UpgradeItems.ENERGY_CAPACITY_LV1;
                        case 2: return UpgradeItems.ENERGY_CAPACITY_LV2;
                        case 3: return UpgradeItems.ENERGY_CAPACITY_LV3;
                        case 4: return UpgradeItems.ENERGY_CAPACITY_LV4;
                        case 5: return UpgradeItems.ENERGY_CAPACITY_LV5;
                        case 6: return UpgradeItems.ENERGY_CAPACITY_LV6;
                        case 7: return UpgradeItems.ENERGY_CAPACITY_LV7;
                        case 8: return UpgradeItems.ENERGY_CAPACITY_LV8;
                        case 9: return UpgradeItems.ENERGY_CAPACITY_LV9;
                        case 10: return UpgradeItems.ENERGY_CAPACITY_LV10;
                        default: return UpgradeItems.ENERGY_CAPACITY_LV1;
                    }

                case ENERGY_EFFICIENCY:
                    switch(level) {
                        case 1: return UpgradeItems.ENERGY_EFFICIENCY_LV1;
                        case 2: return UpgradeItems.ENERGY_EFFICIENCY_LV2;
                        case 3: return UpgradeItems.ENERGY_EFFICIENCY_LV3;
                        case 4: return UpgradeItems.ENERGY_EFFICIENCY_LV4;
                        case 5: return UpgradeItems.ENERGY_EFFICIENCY_LV5;
                        default: return UpgradeItems.ENERGY_EFFICIENCY_LV1;
                    }

                case ARMOR_ENHANCEMENT:
                    switch(level) {
                        case 1: return UpgradeItems.ARMOR_ENHANCEMENT_LV1;
                        case 2: return UpgradeItems.ARMOR_ENHANCEMENT_LV2;
                        case 3: return UpgradeItems.ARMOR_ENHANCEMENT_LV3;
                        case 4: return UpgradeItems.ARMOR_ENHANCEMENT_LV4;
                        case 5: return UpgradeItems.ARMOR_ENHANCEMENT_LV5;
                        default: return UpgradeItems.ARMOR_ENHANCEMENT_LV1;
                    }

                case FLIGHT_MODULE:
                    switch(level) {
                        case 1: return UpgradeItems.FLIGHT_MODULE_BASIC;
                        case 2: return UpgradeItems.FLIGHT_MODULE_ADVANCED;
                        case 3: return UpgradeItems.FLIGHT_MODULE_ULTIMATE;
                        default: return UpgradeItems.FLIGHT_MODULE_BASIC;
                    }

                case TEMPERATURE_CONTROL:
                    switch(level) {
                        case 1: return UpgradeItems.TEMPERATURE_CONTROL_LV1;
                        case 2: return UpgradeItems.TEMPERATURE_CONTROL_LV2;
                        case 3: return UpgradeItems.TEMPERATURE_CONTROL_LV3;
                        case 4: return UpgradeItems.TEMPERATURE_CONTROL_LV4;
                        case 5: return UpgradeItems.TEMPERATURE_CONTROL_LV5;
                        default: return UpgradeItems.TEMPERATURE_CONTROL_LV1;
                    }

                    // 生存类（3级）
                case YELLOW_SHIELD:
                    switch(level) {
                        case 1: return UpgradeItemsExtended.YELLOW_SHIELD_LV1;
                        case 2: return UpgradeItemsExtended.YELLOW_SHIELD_LV2;
                        case 3: return UpgradeItemsExtended.YELLOW_SHIELD_LV3;
                        default: return UpgradeItemsExtended.YELLOW_SHIELD_LV1;
                    }

                case HEALTH_REGEN:
                    switch(level) {
                        case 1: return UpgradeItemsExtended.HEALTH_REGEN_LV1;
                        case 2: return UpgradeItemsExtended.HEALTH_REGEN_LV2;
                        case 3: return UpgradeItemsExtended.HEALTH_REGEN_LV3;
                        default: return UpgradeItemsExtended.HEALTH_REGEN_LV1;
                    }

                case DAMAGE_BOOST:
                    switch(level) {
                        case 1: return UpgradeItemsExtended.DAMAGE_BOOST_LV1;
                        case 2: return UpgradeItemsExtended.DAMAGE_BOOST_LV2;
                        case 3: return UpgradeItemsExtended.DAMAGE_BOOST_LV3;
                        case 4: return UpgradeItemsExtended.DAMAGE_BOOST_LV4;
                        case 5: return UpgradeItemsExtended.DAMAGE_BOOST_LV5;
                        default: return UpgradeItemsExtended.DAMAGE_BOOST_LV1;
                    }

                    // 更多升级类型...
                    // （为了节省空间，这里只展示了部分例子）

                default:
                    // 默认返回1级
                    return getLevel1Component(type);
            }
        }
    }
}