package com.moremod.fabric.handler;

import com.moremod.fabric.data.UpdatedFabricPlayerData.FabricType;
import com.moremod.fabric.system.FabricWeavingSystem;
import com.moremod.init.ModItems;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 织布拆解器丢出移除机制
 * 当织布拆解器与织有布料的盔甲同时丢在地上时，自动移除织布并返还材料
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FabricRemoverDropHandler {

    private static final double INTERACTION_RADIUS = 1.5; // 交互半径

    private static final double PLAYER_SEARCH_RADIUS = 16.0; // 只在玩家附近搜索

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        World world = event.world;

        // 每10tick检查一次（0.5秒）
        if (world.getTotalWorldTime() % 10 != 0) return;

        // 优化：无玩家时直接返回
        if (world.playerEntities.isEmpty()) return;

        // 优化：只在玩家附近搜索织布拆解器，而不是遍历全世界
        for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
            AxisAlignedBB playerArea = new AxisAlignedBB(
                player.posX - PLAYER_SEARCH_RADIUS, player.posY - PLAYER_SEARCH_RADIUS, player.posZ - PLAYER_SEARCH_RADIUS,
                player.posX + PLAYER_SEARCH_RADIUS, player.posY + PLAYER_SEARCH_RADIUS, player.posZ + PLAYER_SEARCH_RADIUS
            );

            List<EntityItem> nearbyRemovers = world.getEntitiesWithinAABB(EntityItem.class, playerArea,
                e -> !e.isDead && e.getItem().getItem() == ModItems.FABRIC_REMOVER);

            for (EntityItem removerEntity : nearbyRemovers) {
                ItemStack removerStack = removerEntity.getItem();

                // 检查耐久
                if (removerStack.getItemDamage() >= removerStack.getMaxDamage()) continue;

                // 搜索附近的盔甲掉落物
                AxisAlignedBB searchArea = new AxisAlignedBB(
                    removerEntity.posX - INTERACTION_RADIUS,
                    removerEntity.posY - INTERACTION_RADIUS,
                    removerEntity.posZ - INTERACTION_RADIUS,
                    removerEntity.posX + INTERACTION_RADIUS,
                    removerEntity.posY + INTERACTION_RADIUS,
                    removerEntity.posZ + INTERACTION_RADIUS
                );

                List<EntityItem> nearbyItems = world.getEntitiesWithinAABB(EntityItem.class, searchArea,
                    e -> e != removerEntity && !e.isDead);

                for (EntityItem armorEntity : nearbyItems) {
                    ItemStack armorStack = armorEntity.getItem();

                    // 检查是否是盔甲
                    if (!(armorStack.getItem() instanceof ItemArmor)) continue;

                    // 检查是否有织布
                    if (!FabricWeavingSystem.hasFabric(armorStack)) continue;

                    // 获取织布类型
                    FabricType fabricType = FabricWeavingSystem.getFabricType(armorStack);
                    if (fabricType == null) continue;

                    // 移除织布
                    if (FabricWeavingSystem.removeFabric(armorStack)) {
                        // 返还织布材料
                        ItemStack fabricItem = getFabricItem(fabricType);
                        if (!fabricItem.isEmpty()) {
                            EntityItem fabricEntity = new EntityItem(world,
                                removerEntity.posX, removerEntity.posY + 0.5, removerEntity.posZ,
                                fabricItem);
                            fabricEntity.setPickupDelay(20);
                            world.spawnEntity(fabricEntity);
                        }

                        // 扣除拆解器耐久
                        removerStack.setItemDamage(removerStack.getItemDamage() + 1);

                        // 如果耐久用尽，移除拆解器
                        if (removerStack.getItemDamage() >= removerStack.getMaxDamage()) {
                            removerEntity.setDead();
                        }

                        // 播放效果
                        playRemovalEffect(world, removerEntity.posX, removerEntity.posY, removerEntity.posZ);

                        // 每次只处理一件盔甲
                        break;
                    }
                }
            }
        }
    }

    /**
     * 根据织布类型获取对应的物品
     */
    private static ItemStack getFabricItem(FabricType type) {
        switch (type) {
            // 高级织布
            case ABYSS:
                return new ItemStack(ModItems.ABYSSAL_FABRIC);
            case TEMPORAL:
                return new ItemStack(ModItems.CHRONO_FABRIC);
            case SPATIAL:
                return new ItemStack(ModItems.SPACETIME_FABRIC);
            case OTHERWORLD:
                return new ItemStack(ModItems.OTHERWORLDLY_FIBER);

            // 基础织布
            case RESILIENT:
                return new ItemStack(ModItems.RESILIENT_FIBER);
            case VITAL:
                return new ItemStack(ModItems.VITAL_THREAD);
            case LIGHT:
                return new ItemStack(ModItems.LIGHT_WEAVE);
            case PREDATOR:
                return new ItemStack(ModItems.PREDATOR_CLOTH);
            case SIPHON:
                return new ItemStack(ModItems.SIPHON_WRAP);

            default:
                return ItemStack.EMPTY;
        }
    }

    /**
     * 播放移除效果
     */
    private static void playRemovalEffect(World world, double x, double y, double z) {
        // 音效
        world.playSound(null, x, y, z,
            SoundEvents.BLOCK_CLOTH_BREAK, SoundCategory.BLOCKS,
            1.0F, 1.2F);

        // 粒子效果
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                x, y + 0.5, z, 10, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
