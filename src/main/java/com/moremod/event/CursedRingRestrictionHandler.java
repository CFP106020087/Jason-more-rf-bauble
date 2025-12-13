package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class CursedRingRestrictionHandler {

    // 戒指的资源位置
    private static final ResourceLocation CURSED_RING_ID = new ResourceLocation("enigmaticlegacy", "cursed_ring");
    private static final ResourceLocation BLESSED_RING_ID = new ResourceLocation("enigmaticlegacy", "blessed_ring");

    // Cyclic方块的资源位置
    private static final ResourceLocation CYCLIC_ENCHANTER = new ResourceLocation("cyclicmagic", "block_enchanter");
    // block_disenchanter 不再受限制

    /**
     * 检查玩家是否装备了受限戒指
     * @return 0=没有, 1=七咒之戒, 2=祝福之戒
     */
    private static int hasRestrictedRing(EntityPlayer player) {
        if (player == null || player.world.isRemote) {
            return 0;
        }

        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem().getRegistryName() != null) {
                        ResourceLocation itemId = bauble.getItem().getRegistryName();
                        if (itemId.equals(CURSED_RING_ID)) {
                            return 1;
                        } else if (itemId.equals(BLESSED_RING_ID)) {
                            return 2;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }

        return 0;
    }

    /**
     * 检查方块是否是受限制的Cyclic方块
     * 只限制 block_enchanter，不限制 block_disenchanter
     */
    private static boolean isRestrictedBlock(Block block) {
        if (block == null || block.getRegistryName() == null) {
            return false;
        }

        ResourceLocation blockId = block.getRegistryName();
        return blockId.equals(CYCLIC_ENCHANTER);
    }

    /**
     * 阻止放置方块事件
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.getPlayer();

        if (player == null || player.world.isRemote) {
            return;
        }

        // 创造模式玩家豁免
        if (player.isCreative()) {
            return;
        }

        // 检查是否是受限制的方块
        if (!isRestrictedBlock(event.getPlacedBlock().getBlock())) {
            return;
        }

        // 检查玩家装备的戒指
        int ringType = hasRestrictedRing(player);
        if (ringType > 0) {
            event.setCanceled(true);

            String blockName = event.getPlacedBlock().getBlock().getLocalizedName();

            if (ringType == 1) {
                // 七咒之戒
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "✗ " +
                                TextFormatting.RED + "七咒之戒的诅咒阻止你放置" + blockName + "！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + "诅咒之力与能量操作的魔法产生了强烈的排斥..."
                ));
            } else {
                // 祝福之戒
                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "✗ " +
                                TextFormatting.YELLOW + "祝福之戒的神圣之力阻止你放置" + blockName + "！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "神圣的祝福与能量操作的魔法不能共存..."
                ));
            }

            player.playSound(net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.5F, 0.5F);
        }
    }

    /**
     * 阻止右键使用方块事件
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();

        if (player == null || event.getWorld().isRemote) {
            return;
        }

        // 创造模式玩家豁免
        if (player.isCreative()) {
            return;
        }

        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();

        if (!isRestrictedBlock(block)) {
            return;
        }

        int ringType = hasRestrictedRing(player);
        if (ringType > 0) {
            event.setCanceled(true);

            String blockName = block.getLocalizedName();

            if (ringType == 1) {
                // 七咒之戒
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "✗ " +
                                TextFormatting.RED + "七咒之戒的诅咒阻止你使用" + blockName + "！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "诅咒的力量排斥着能量构筑的魔法..."
                ));
            } else {
                // 祝福之戒
                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "✗ " +
                                TextFormatting.YELLOW + "祝福之戒的神圣之力阻止你使用" + blockName + "！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.WHITE + "神圣的光辉拒绝了能量构筑的魔法..."
                ));
            }

            event.getWorld().spawnParticle(
                    net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                    event.getPos().getX() + 0.5,
                    event.getPos().getY() + 1.0,
                    event.getPos().getZ() + 0.5,
                    0, 0.1, 0, 0
            );

            player.playSound(net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT, 0.3F, 0.1F);
        }
    }

    /**
     * 阻止左键破坏方块事件（可选）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();

        if (player == null || player.world.isRemote) {
            return;
        }

        if (player.isCreative()) {
            return;
        }

        if (!isRestrictedBlock(event.getState().getBlock())) {
            return;
        }

        int ringType = hasRestrictedRing(player);
        if (ringType > 0) {
            if (ringType == 1) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + "诅咒之力正在侵蚀这个方块..."
                ));
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "神圣之光正在净化这个方块..."
                ));
            }
        }
    }
}