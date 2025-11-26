package com.moremod.item;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 行為分析晶片事件處理器
 * 負責監聽遊戲事件並記錄玩家行為數據
 */
public class BehaviorChipEventHandler {

    // ===== 移動追蹤 =====
    private static final Map<UUID, Double> lastX = new HashMap<>();
    private static final Map<UUID, Double> lastY = new HashMap<>();
    private static final Map<UUID, Double> lastZ = new HashMap<>();
    private static final Map<UUID, Integer> lastDimension = new HashMap<>();

    // ===== 玩家 Tick 事件 =====
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 只追蹤裝備了晶片的玩家
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        UUID uuid = player.getUniqueID();

        // 記錄遊玩時間（每秒記錄一次）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            BehaviorDataTracker.recordPlayTime(player, 20);
        }

        // 追蹤移動（每 10 tick 記錄一次）
        if (player.world.getTotalWorldTime() % 10 == 0) {
            Double prevX = lastX.get(uuid);
            Double prevY = lastY.get(uuid);
            Double prevZ = lastZ.get(uuid);

            if (prevX != null && prevY != null && prevZ != null) {
                double dx = player.posX - prevX;
                double dy = player.posY - prevY;
                double dz = player.posZ - prevZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > 0.1) { // 避免記錄微小抖動
                    BehaviorDataTracker.recordMovement(player, distance);
                }
            }

            lastX.put(uuid, player.posX);
            lastY.put(uuid, player.posY);
            lastZ.put(uuid, player.posZ);
        }

        // 追蹤維度變化
        int currentDim = player.dimension;
        Integer prevDim = lastDimension.get(uuid);
        if (prevDim != null && prevDim != currentDim) {
            BehaviorDataTracker.recordDimensionChange(player);
        }
        lastDimension.put(uuid, currentDim);

        // 應用玩家類型特定的被動效果
        applyPassiveEffects(player);
    }

    // ===== 實體死亡事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();

            if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

            EntityLivingBase target = event.getEntityLiving();

            // 記錄擊殺怪物
            if (target instanceof IMob) {
                BehaviorDataTracker.recordMobKill(player);
            }
            // 記錄擊殺玩家
            else if (target instanceof EntityPlayer) {
                BehaviorDataTracker.recordPlayerKill(player);
            }
        }

        // 記錄玩家死亡
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (ItemBehaviorAnalysisChip.hasChipEquipped(player)) {
                BehaviorDataTracker.recordDeath(player);
            }
        }
    }

    // ===== 傷害事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        DamageSource source = event.getSource();

        // 記錄玩家受到傷害
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (ItemBehaviorAnalysisChip.hasChipEquipped(player)) {
                BehaviorDataTracker.recordDamage(player, event.getAmount(), true);
            }
        }

        // 記錄玩家造成傷害
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            if (ItemBehaviorAnalysisChip.hasChipEquipped(player)) {
                BehaviorDataTracker.recordDamage(player, event.getAmount(), false);
            }
        }
    }

    // ===== 方塊放置事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        BehaviorDataTracker.recordBlockPlaced(player);

        // 建築型玩家額外效果：10% 機率不消耗方塊
        if (ItemBehaviorAnalysisChip.getPlayerType(player) == ItemBehaviorAnalysisChip.PlayerType.BUILDER) {
            if (player.world.rand.nextFloat() < 0.1f) {
                // 返還一個方塊
                ItemStack stack = event.getItemInHand();
                if (!stack.isEmpty() && stack.getCount() > 0) {
                    stack.grow(1);
                }
            }
        }
    }

    // ===== 方塊破壞事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        IBlockState state = event.getState();
        Block block = state.getBlock();

        // 判斷是否為礦物
        boolean isOre = isOreBlock(block);
        BehaviorDataTracker.recordBlockMined(player, isOre);

        // 採礦型玩家額外效果：增加挖掘速度
        if (ItemBehaviorAnalysisChip.getPlayerType(player) == ItemBehaviorAnalysisChip.PlayerType.MINER) {
            // 挖掘速度增益已在主類中通過屬性修改器實現
            // 這裡可以添加額外效果，如礦物額外掉落
            if (isOre && player.world.rand.nextFloat() < 0.15f) {
                // 15% 機率雙倍掉落（通過設置經驗值）
                event.setExpToDrop(event.getExpToDrop() * 2);
            }
        }
    }

    // ===== 合成事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onItemCrafted(ItemCraftedEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        BehaviorDataTracker.recordCrafting(player);
    }

    // ===== 挖掘速度事件 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) return;
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        ItemBehaviorAnalysisChip.PlayerType type = ItemBehaviorAnalysisChip.getPlayerType(player);

        // 採礦型玩家：挖掘速度 +30%
        if (type == ItemBehaviorAnalysisChip.PlayerType.MINER) {
            event.setNewSpeed(event.getNewSpeed() * 1.3f);
        }
        // 建築型玩家：挖掘速度 +15%（建築也需要拆除）
        else if (type == ItemBehaviorAnalysisChip.PlayerType.BUILDER) {
            event.setNewSpeed(event.getNewSpeed() * 1.15f);
        }
    }

    // ===== 玩家登出事件 =====
    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        // 保存數據
        BehaviorDataTracker.saveData(player);

        // 清除緩存
        UUID uuid = player.getUniqueID();
        lastX.remove(uuid);
        lastY.remove(uuid);
        lastZ.remove(uuid);
        lastDimension.remove(uuid);
        BehaviorDataTracker.clearData(uuid);
    }

    // ===== 輔助方法 =====

    /**
     * 判斷方塊是否為礦物
     */
    private boolean isOreBlock(Block block) {
        return block == Blocks.COAL_ORE ||
                block == Blocks.IRON_ORE ||
                block == Blocks.GOLD_ORE ||
                block == Blocks.DIAMOND_ORE ||
                block == Blocks.EMERALD_ORE ||
                block == Blocks.LAPIS_ORE ||
                block == Blocks.REDSTONE_ORE ||
                block == Blocks.LIT_REDSTONE_ORE ||
                block == Blocks.QUARTZ_ORE ||
                block.getTranslationKey().toLowerCase().contains("ore");
    }

    /**
     * 應用玩家類型特定的被動效果
     */
    private void applyPassiveEffects(EntityPlayer player) {
        ItemBehaviorAnalysisChip.PlayerType type = ItemBehaviorAnalysisChip.getPlayerType(player);

        switch (type) {
            case EXPLORER:
                // 探索型：夜視效果（每 10 秒檢查一次）
                if (player.world.getTotalWorldTime() % 200 == 0) {
                    player.addPotionEffect(
                            new net.minecraft.potion.PotionEffect(
                                    net.minecraft.init.MobEffects.NIGHT_VISION,
                                    300, // 15 秒
                                    0,
                                    false,
                                    false
                            )
                    );
                }
                break;

            case FARMER:
                // 農業型：飽食度恢復更快
                if (player.world.getTotalWorldTime() % 100 == 0) {
                    if (player.getFoodStats().getFoodLevel() < 20) {
                        player.getFoodStats().addStats(1, 0.5f);
                    }
                }
                break;

            case COMBAT:
                // 戰鬥型：戰鬥時獲得力量效果
                if (player.world.getTotalWorldTime() % 40 == 0) {
                    // 檢查附近是否有敵對生物
                    List<EntityLivingBase> nearbyEntities = player.world.getEntitiesWithinAABB(
                            EntityLivingBase.class,
                            player.getEntityBoundingBox().grow(16)
                    );

                    boolean hasNearbyEnemy = false;
                    for (EntityLivingBase entity : nearbyEntities) {
                        if (entity instanceof IMob && entity != player) {
                            hasNearbyEnemy = true;
                            break;
                        }
                    }

                    if (hasNearbyEnemy) {
                        player.addPotionEffect(
                                new net.minecraft.potion.PotionEffect(
                                        net.minecraft.init.MobEffects.STRENGTH,
                                        100, // 5 秒
                                        0,
                                        false,
                                        false
                                )
                        );
                    }
                }
                break;
        }
    }

    // ===== 額外功能：作物收穫檢測 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onCropHarvest(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null || player.world.isRemote) return;
        if (!ItemBehaviorAnalysisChip.hasChipEquipped(player)) return;

        Block block = event.getState().getBlock();

        // 檢測作物方塊
        if (isCropBlock(block)) {
            BehaviorDataTracker.recordCropHarvest(player);

            // 農業型玩家：額外掉落
            if (ItemBehaviorAnalysisChip.getPlayerType(player) == ItemBehaviorAnalysisChip.PlayerType.FARMER) {
                if (player.world.rand.nextFloat() < 0.25f) {
                    // 25% 機率額外掉落
                    event.getDrops().addAll(event.getDrops());
                }
            }
        }
    }

    /**
     * 判斷是否為作物方塊
     */
    private boolean isCropBlock(Block block) {
        return block == Blocks.WHEAT ||
                block == Blocks.CARROTS ||
                block == Blocks.POTATOES ||
                block == Blocks.BEETROOTS ||
                block == Blocks.MELON_BLOCK ||
                block == Blocks.PUMPKIN ||
                block == Blocks.COCOA ||
                block == Blocks.NETHER_WART ||
                block.getTranslationKey().toLowerCase().contains("crop");
    }
}