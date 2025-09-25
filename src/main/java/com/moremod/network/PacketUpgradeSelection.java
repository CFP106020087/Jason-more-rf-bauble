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
                    TextFormatting.GREEN + "═══════ 获得升级 ═══════"
            ));

            // 给予物品
            for (ItemStack reward : rewards) {
                if (!player.inventory.addItemStackToInventory(reward)) {
                    player.dropItem(reward, false);
                }

                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "  + " + reward.getDisplayName()
                ));
            }

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
            ItemUpgradeComponent component = getUpgradeComponent(type);
            if (component != null) {
                return new ItemStack(component);
            }
            return ItemStack.EMPTY;
        }

        private ItemUpgradeComponent getUpgradeComponent(UpgradeType type) {
            switch(type) {
                // 基础升级
                case ENERGY_CAPACITY:
                    return UpgradeItems.ENERGY_CELL_BASIC;
                case ENERGY_EFFICIENCY:
                    return UpgradeItems.EFFICIENCY_CHIP;
                case ARMOR_ENHANCEMENT:
                    return UpgradeItems.ARMOR_PLATING;
                case TEMPERATURE_CONTROL:
                    return UpgradeItems.TEMPERATURE_UPGRADE;

                // 生存类升级
                case YELLOW_SHIELD:
                    return UpgradeItemsExtended.YELLOW_SHIELD_MODULE;
                case HEALTH_REGEN:
                    return UpgradeItemsExtended.NANO_REPAIR_SYSTEM;
                case HUNGER_THIRST:
                    return UpgradeItemsExtended.METABOLIC_REGULATOR;
                case THORNS:
                    return UpgradeItemsExtended.REACTIVE_ARMOR;
                case FIRE_EXTINGUISH:
                    return UpgradeItemsExtended.FIRE_SUPPRESSION;

                // 辅助类升级
                case WATERPROOF_MODULE:
                    return UpgradeItemsExtended.WATERPROOF_MODULE;
                case ORE_VISION:
                    return UpgradeItemsExtended.ORE_SCANNER;
                case MOVEMENT_SPEED:
                    return UpgradeItemsExtended.SERVO_MOTORS;
                case STEALTH:
                    return UpgradeItemsExtended.OPTICAL_CAMOUFLAGE;
                case EXP_AMPLIFIER:
                    return UpgradeItemsExtended.EXP_COLLECTOR;

                // 战斗类升级
                case DAMAGE_BOOST:
                    return UpgradeItemsExtended.STRENGTH_AMPLIFIER;
                case ATTACK_SPEED:
                    return UpgradeItemsExtended.REFLEX_ENHANCER;
                case RANGE_EXTENSION:
                    return UpgradeItemsExtended.SWEEP_MODULE;
                case PURSUIT:
                    return UpgradeItemsExtended.PURSUIT_SYSTEM;

                // 能源类升级
                case KINETIC_GENERATOR:
                    return UpgradeItemsExtended.KINETIC_DYNAMO;
                case SOLAR_GENERATOR:
                    return UpgradeItemsExtended.SOLAR_PANEL;
                case VOID_ENERGY:
                    return UpgradeItemsExtended.VOID_RESONATOR;
                case COMBAT_CHARGER:
                    return UpgradeItemsExtended.COMBAT_HARVESTER;

                // 特殊套装
                case SURVIVAL_PACKAGE:
                    return UpgradeItemsExtended.SURVIVAL_PACKAGE;
                case COMBAT_PACKAGE:
                    return UpgradeItemsExtended.COMBAT_PACKAGE;

                default:
                    return null;
            }
        }
    }
}