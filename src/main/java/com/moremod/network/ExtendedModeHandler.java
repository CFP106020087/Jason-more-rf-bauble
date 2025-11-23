package com.moremod.network;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemJetpackBauble;
import com.moremod.item.ItemCreativeJetpackBauble;
import baubles.api.BaublesApi;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 扩展的模式切换处理器
 * 同时支持喷气背包和机械核心
 */
public class ExtendedModeHandler implements IMessageHandler<MessageToggleJetpackMode, IMessage> {

    @Override
    public IMessage onMessage(MessageToggleJetpackMode msg, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;

        player.getServerWorld().addScheduledTask(() -> {
            // 先检查是否是机械核心的特殊模式值
            if (msg.toggleMode >= 3) {
                handleMechanicalCoreMode(player, msg.toggleMode);
                return;
            }

            // 否则按原逻辑处理喷气背包
            handleJetpackMode(player, msg);
        });

        return null;
    }

    /**
     * 处理机械核心的模式切换
     * 3 = 机械核心飞行开关
     * 4 = 机械核心悬停模式
     * 5 = 机械核心速度模式（3级飞行）
     */
    private void handleMechanicalCoreMode(EntityPlayerMP player, int mode) {
        // 查找装备的机械核心
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);

            if (!ItemMechanicalCore.isMechanicalCore(stack)) continue;

            // 检查是否有飞行模块
            int flightLevel = ItemMechanicalCore.getUpgradeLevel(stack,
                    ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);

            if (flightLevel <= 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚙ 未安装飞行模块！"));
                return;
            }

            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }

            switch (mode) {
                case 3: // 飞行模块开关
                    boolean enabled = !nbt.getBoolean("FlightModuleEnabled");
                    nbt.setBoolean("FlightModuleEnabled", enabled);

                    player.sendMessage(new TextComponentString(
                            enabled ? TextFormatting.GREEN + "⚙ 飞行模块：已激活" :
                                    TextFormatting.RED + "⚙ 飞行模块：已关闭"));

                    if (!enabled && !player.capabilities.isCreativeMode) {
                        player.capabilities.allowFlying = false;
                        player.capabilities.isFlying = false;
                        player.sendPlayerAbilities();
                    }

                    // 播放开关音效
                    player.world.playSound(null, player.getPosition(),
                            enabled ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT,
                            SoundCategory.PLAYERS, 0.5F, 1.0F);
                    break;

                case 4: // 悬停模式（需要2级以上）
                    if (flightLevel < 2) {
                        // 基础飞行模块不支持悬停
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "⚙ 基础飞行模块不支持悬停！" +
                                        TextFormatting.GRAY + " (需要高级飞行模块)"));

                        player.world.playSound(null, player.getPosition(),
                                SoundEvents.BLOCK_ANVIL_LAND,
                                SoundCategory.PLAYERS, 0.3F, 1.5F);
                        return;
                    }

                    boolean hover = !nbt.getBoolean("FlightHoverMode");
                    nbt.setBoolean("FlightHoverMode", hover);

                    player.sendMessage(new TextComponentString(
                            hover ? TextFormatting.AQUA + "⚙ 悬停模式：开启 " +
                                    TextFormatting.GRAY + "(消耗双倍能量)" :
                                    TextFormatting.YELLOW + "⚙ 悬停模式：关闭"));

                    // 播放切换音效
                    player.world.playSound(null, player.getPosition(),
                            SoundEvents.UI_BUTTON_CLICK,
                            SoundCategory.PLAYERS, 0.5F, hover ? 0.8F : 1.2F);
                    break;

                case 5: // 速度模式（仅3级）
                    if (flightLevel >= 3) {
                        ItemMechanicalCore.cycleSpeedMode(stack);
                        ItemMechanicalCore.SpeedMode newMode = ItemMechanicalCore.getSpeedMode(stack);

                        TextFormatting color = TextFormatting.YELLOW;
                        String icon = "→";
                        float pitch = 1.0F;

                        if (newMode == ItemMechanicalCore.SpeedMode.FAST) {
                            color = TextFormatting.GOLD;
                            icon = "⇒";
                            pitch = 1.2F;
                        } else if (newMode == ItemMechanicalCore.SpeedMode.ULTRA) {
                            color = TextFormatting.LIGHT_PURPLE;
                            icon = "⟹";
                            pitch = 1.5F;
                        }

                        player.sendMessage(new TextComponentString(
                                TextFormatting.GRAY + "⚙ 速度模式: " + color + icon + " " + newMode.getName()));

                        // 根据模式播放不同音高的音效
                        player.world.playSound(null, player.getPosition(),
                                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                SoundCategory.PLAYERS, 0.5F, pitch);

                        // 极速模式额外音效
                        if (newMode == ItemMechanicalCore.SpeedMode.ULTRA) {
                            player.world.playSound(null, player.getPosition(),
                                    SoundEvents.ENTITY_ENDERDRAGON_FLAP,
                                    SoundCategory.PLAYERS, 0.3F, 0.8F);
                        }
                    } else {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "⚙ 需要3级飞行模块才能切换速度模式！"));

                        player.world.playSound(null, player.getPosition(),
                                SoundEvents.BLOCK_ANVIL_LAND,
                                SoundCategory.PLAYERS, 0.3F, 1.5F);
                    }
                    break;
            }

            // 同步到客户端（如果有类似的同步机制）
            // 注意：机械核心可能需要自己的同步方式

            return; // 找到并处理了机械核心，结束
        }

        // 没有找到带飞行模块的机械核心
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "⚙ 未装备带飞行模块的机械核心"));
    }

    /**
     * 处理普通喷气背包（复制原逻辑）
     */
    private void handleJetpackMode(EntityPlayerMP player, MessageToggleJetpackMode msg) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);

            if (stack.getItem() instanceof ItemJetpackBauble ||
                    stack.getItem() instanceof ItemCreativeJetpackBauble) {

                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    stack.setTagCompound(tag);
                }

                // 🎯 获取喷气背包等级
                int jetpackTier = 1;
                boolean canHover = false;
                boolean canChangeSpeed = false;

                if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                    // 创造喷气背包拥有所有功能
                    jetpackTier = 3;
                    canHover = true;
                    canChangeSpeed = true;
                } else if (stack.getItem() instanceof ItemJetpackBauble) {
                    // 根据物品名称判断等级
                    String itemName = stack.getItem().getRegistryName().toString().toLowerCase();

                    if (itemName.contains("basic") || itemName.contains("t1") || itemName.contains("tier1")) {
                        jetpackTier = 1;
                        canHover = false;
                        canChangeSpeed = false;
                    } else if (itemName.contains("advanced") || itemName.contains("t2") || itemName.contains("tier2")) {
                        jetpackTier = 2;
                        canHover = true;
                        canChangeSpeed = false;
                    } else if (itemName.contains("ultimate") || itemName.contains("t3") || itemName.contains("tier3")) {
                        jetpackTier = 3;
                        canHover = true;
                        canChangeSpeed = true;
                    } else {
                        // 尝试从NBT读取
                        if (tag.hasKey("jetpackTier")) {
                            jetpackTier = tag.getInteger("jetpackTier");
                            canHover = jetpackTier >= 2;
                            canChangeSpeed = jetpackTier >= 3;
                        }
                    }
                }

                if (msg.toggleMode == 1) { // 喷气背包开关
                    boolean current = tag.getBoolean("JetpackEnabled");
                    tag.setBoolean("JetpackEnabled", !current);
                    player.sendMessage(new TextComponentString(
                            "Jetpack: " + (!current ? "ON" : "OFF")));

                } else if (msg.toggleMode == 0) { // 悬停模式
                    if (!canHover) {
                        // T1喷气背包不能悬停
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "基础喷气背包不支持悬停模式！"));

                        player.world.playSound(null, player.getPosition(),
                                SoundEvents.BLOCK_ANVIL_LAND,
                                SoundCategory.PLAYERS, 0.3F, 1.5F);
                        return;
                    }

                    boolean current = tag.getBoolean("HoverEnabled");
                    tag.setBoolean("HoverEnabled", !current);
                    player.sendMessage(new TextComponentString(
                            "Hover: " + (!current ? "ON" : "OFF")));

                } else if (msg.toggleMode == 2) { // 速度模式切换
                    if (!canChangeSpeed) {
                        // 只有T3能切换速度
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "需要终极喷气背包才能切换速度模式！"));

                        player.world.playSound(null, player.getPosition(),
                                SoundEvents.BLOCK_ANVIL_LAND,
                                SoundCategory.PLAYERS, 0.3F, 1.5F);
                        return;
                    }

                    // 处理普通喷气背包的速度切换
                    if (stack.getItem() instanceof ItemJetpackBauble) {
                        ItemJetpackBauble jetpack = (ItemJetpackBauble) stack.getItem();
                        jetpack.nextSpeedMode(stack, player);
                    } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                        ItemCreativeJetpackBauble creativeJetpack =
                                (ItemCreativeJetpackBauble) stack.getItem();
                        creativeJetpack.nextSpeedMode(stack, player);
                    }
                }

                // 同步 NBT 到客户端
                PacketHandler.INSTANCE.sendTo(
                        new MessageSyncJetpackTagToClient(i, stack),
                        player
                );
                break;
            }
        }
    }
}