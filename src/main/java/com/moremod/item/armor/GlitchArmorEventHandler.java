package com.moremod.item.armor;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 故障盔甲事件處理器
 * 處理所有盔甲能力的邏輯
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class GlitchArmorEventHandler {

    private static final Random RANDOM = new Random();

    // ========== 數據存儲 ==========

    // 玩家存檔點（頭盔能力）
    private static final Map<UUID, CheckpointData> CHECKPOINTS = new HashMap<>();

    // 緩衝區傷害存儲（護腿能力）
    private static final Map<UUID, BufferData> DAMAGE_BUFFERS = new HashMap<>();

    // NULL觸發冷卻
    private static final Map<UUID, Long> NULL_COOLDOWNS = new HashMap<>();
    private static final long NULL_COOLDOWN_MS = 30000; // 30秒冷卻

    // 閃爍無敵狀態
    private static final Map<UUID, Long> FLICKER_END_TIMES = new HashMap<>();

    // BSOD時停狀態
    private static final Map<UUID, BSODData> BSOD_ACTIVE = new HashMap<>();

    // ========== 閃爍狀態共享 ==========

    /**
     * 設置閃爍無敵狀態（供FirstAid兼容層調用）
     */
    public static void setFlickerState(UUID playerId, long endTime) {
        FLICKER_END_TIMES.put(playerId, endTime);
    }

    /**
     * 檢查閃爍無敵狀態
     */
    public static boolean isInFlickerState(UUID playerId) {
        Long flickerEnd = FLICKER_END_TIMES.get(playerId);
        return flickerEnd != null && System.currentTimeMillis() < flickerEnd;
    }

    // ========== 胸甲：空指針異常 (NULL) ==========

    /**
     * 非FirstAid版本的NULL能力
     * 在LivingDamageEvent中處理致命傷害
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 檢查是否穿戴胸甲
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.CHEST)) return;

        // 檢查是否在閃爍無敵狀態
        Long flickerEnd = FLICKER_END_TIMES.get(player.getUniqueID());
        if (flickerEnd != null && System.currentTimeMillis() < flickerEnd) {
            event.setCanceled(true);
            spawnGlitchParticles(player, 5);
            return;
        }

        // 檢查是否為致命傷害
        float newHealth = player.getHealth() - event.getAmount();
        if (newHealth > 0) return; // 不是致命傷害

        // 檢查冷卻
        Long lastNull = NULL_COOLDOWNS.get(player.getUniqueID());
        if (lastNull != null && System.currentTimeMillis() - lastNull < NULL_COOLDOWN_MS) {
            return; // 冷卻中
        }

        // 20%機率觸發NULL
        float nullChance = 0.20f;
        // 全套增加10%
        if (ItemGlitchArmor.hasFullSet(player)) {
            nullChance += 0.10f;
        }

        if (RANDOM.nextFloat() < nullChance) {
            triggerNullException(player, event);
        }
    }

    private static void triggerNullException(EntityPlayer player, LivingDamageEvent event) {
        // 完全取消傷害
        event.setCanceled(true);

        // 記錄冷卻
        NULL_COOLDOWNS.put(player.getUniqueID(), System.currentTimeMillis());

        // 設置1秒閃爍無敵
        FLICKER_END_TIMES.put(player.getUniqueID(), System.currentTimeMillis() + 1000);

        // 視覺和音效
        spawnNullEffect(player);

        // 訊息
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "§l[NULL] " +
                TextFormatting.GRAY + "NullPointerException: damage == null"), true);

        // 少量回血
        player.heal(2.0f);
    }

    private static void spawnNullEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 播放音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 2.0f);

        // 故障粒子效果
        for (int i = 0; i < 30; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 2;
            double offsetY = RANDOM.nextDouble() * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 2;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0, 0.1);
        }

        // 閃電效果（視覺）
        world.spawnParticle(EnumParticleTypes.END_ROD,
                player.posX, player.posY + 1, player.posZ,
                20, 0.5, 0.5, 0.5, 0.1);
    }

    // ========== 胸甲：段錯誤 (Segmentation Fault) ==========

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 檢查胸甲
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.CHEST)) return;

        // 5%機率觸發段錯誤
        if (RANDOM.nextFloat() < 0.05f) {
            // 傷害翻倍
            event.setAmount(event.getAmount() * 2);

            // 傳染給周圍敵人
            EntityLivingBase target = event.getEntityLiving();
            List<EntityMob> nearby = target.world.getEntitiesWithinAABB(EntityMob.class,
                    target.getEntityBoundingBox().grow(4.0), e -> e != target);

            float spreadDamage = event.getAmount() * 0.5f;
            for (EntityMob mob : nearby) {
                mob.attackEntityFrom(DamageSource.causePlayerDamage(player), spreadDamage);
            }

            // 視覺效果
            if (target.world instanceof WorldServer) {
                WorldServer world = (WorldServer) target.world;
                world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        target.posX, target.posY + 1, target.posZ,
                        15, 0.5, 0.5, 0.5, 0.1);
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "[SIGSEGV] " +
                    TextFormatting.GRAY + "Segmentation fault (core dumped)"), true);
        }
    }

    // ========== 頭盔：記憶溢出 ==========

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (!(event.getEntityLiving() instanceof EntityMob)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 檢查頭盔
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.HEAD)) return;

        // 30%機率產生記憶殘留
        if (RANDOM.nextFloat() < 0.30f) {
            EntityMob deadMob = (EntityMob) event.getEntityLiving();

            // 標記這個位置，5秒後生成友方實體
            // （簡化實現：給予玩家增益）
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 0)); // 5秒力量

            if (player.world instanceof WorldServer) {
                WorldServer world = (WorldServer) player.world;
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        deadMob.posX, deadMob.posY + 1, deadMob.posZ,
                        20, 0.5, 0.5, 0.5, 0.05);
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "[Memory] " +
                    TextFormatting.GRAY + "記憶殘留已激活"), true);
        }
    }

    // ========== 護腿：緩衝區溢出 ==========

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamageForBuffer(LivingDamageEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 檢查護腿
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.LEGS)) return;

        // 50%傷害進入緩衝區
        float bufferedDamage = event.getAmount() * 0.5f;
        event.setAmount(event.getAmount() * 0.5f);

        // 存儲緩衝傷害
        BufferData buffer = DAMAGE_BUFFERS.computeIfAbsent(player.getUniqueID(),
                k -> new BufferData());
        buffer.addDamage(bufferedDamage);

        spawnGlitchParticles(player, 3);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 處理緩衝區返還
        BufferData buffer = DAMAGE_BUFFERS.get(player.getUniqueID());
        if (buffer != null) {
            float heal = buffer.processHeal();
            if (heal > 0) {
                player.heal(heal);
                if (heal > 0.5f) {
                    spawnGlitchParticles(player, 2);
                }
            }
        }

        // 處理閃爍視覺效果
        Long flickerEnd = FLICKER_END_TIMES.get(player.getUniqueID());
        if (flickerEnd != null && System.currentTimeMillis() < flickerEnd) {
            // 每隔幾tick產生閃爍粒子
            if (player.ticksExisted % 3 == 0) {
                spawnGlitchParticles(player, 5);
            }
        }

        // 處理BSOD時停
        BSODData bsod = BSOD_ACTIVE.get(player.getUniqueID());
        if (bsod != null && bsod.isActive()) {
            processBSOD(player, bsod);
        }
    }

    // ========== 靴子：堆疊下溢 ==========

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 檢查靴子
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.FEET)) return;

        // 計算本應造成的傷害
        float fallDamage = event.getDistance() - 3.0f;
        if (fallDamage <= 0) {
            event.setCanceled(true);
            return;
        }

        // 取消自身傷害
        event.setCanceled(true);

        // 對周圍敵人造成傷害
        List<EntityLivingBase> nearby = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(3.0),
                e -> e != player && e instanceof EntityMob);

        for (EntityLivingBase entity : nearby) {
            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), fallDamage);
        }

        // 落地視覺效果
        if (!nearby.isEmpty() && player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    player.posX, player.posY, player.posZ,
                    1, 0, 0, 0.0, 0);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "[Stack Underflow] " +
                    TextFormatting.GRAY + String.format("轉移 %.1f 傷害", fallDamage)), true);
        }
    }

    // ========== Shift+右鍵交互 ==========

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        if (!player.isSneaking()) return;

        // 頭盔：存檔讀取
        if (ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.HEAD)) {
            handleCheckpointAbility(player);
        }

        // 護腿：釋放緩存AOE
        if (ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.LEGS)) {
            handleBufferRelease(player);
        }
    }

    /**
     * 頭盔：存檔點能力
     * 第一次使用保存位置，第二次使用傳送回去
     */
    private static void handleCheckpointAbility(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        CheckpointData checkpoint = CHECKPOINTS.get(uuid);

        if (checkpoint == null || !checkpoint.isValid()) {
            // 保存當前位置
            CheckpointData newCheckpoint = new CheckpointData(player.getPosition(), player.dimension);
            CHECKPOINTS.put(uuid, newCheckpoint);

            // 效果
            if (player.world instanceof WorldServer) {
                WorldServer world = (WorldServer) player.world;
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        player.posX, player.posY + 1, player.posZ,
                        20, 0.5, 0.5, 0.5, 0.05);
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5f, 1.5f);
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "[Checkpoint] " +
                    TextFormatting.GRAY + "位置已保存 (10秒內再次使用傳送回此處)"), true);
        } else {
            // 傳送回保存的位置
            if (checkpoint.dimensionId != player.dimension) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "[Error] 跨維度傳送失敗"), true);
                CHECKPOINTS.remove(uuid);
                return;
            }

            BlockPos pos = checkpoint.position;
            player.setPositionAndUpdate(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            // 效果
            if (player.world instanceof WorldServer) {
                WorldServer world = (WorldServer) player.world;
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        player.posX, player.posY + 1, player.posZ,
                        50, 0.5, 1, 0.5, 0.1);
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "[Checkpoint] " +
                    TextFormatting.GRAY + "已傳送回存檔點"), true);

            CHECKPOINTS.remove(uuid);
        }
    }

    /**
     * 護腿：釋放緩存傷害為AOE
     */
    private static void handleBufferRelease(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        BufferData buffer = DAMAGE_BUFFERS.get(uuid);

        if (buffer == null || buffer.getTotalBuffered() <= 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "[Buffer] 緩存為空"), true);
            return;
        }

        float totalDamage = buffer.getTotalBuffered();
        buffer.entries.clear(); // 清空緩存

        // 對周圍敵人造成傷害
        List<EntityLivingBase> enemies = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(5.0),
                e -> e != player && e instanceof EntityMob);

        if (enemies.isEmpty()) {
            // 沒有敵人，傷害轉為自我治療
            player.heal(totalDamage * 0.5f);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "[Buffer] " +
                    TextFormatting.GRAY + String.format("無敵人，轉換為治療 +%.1f", totalDamage * 0.5f)), true);
        } else {
            // 對每個敵人造成傷害
            float damagePerEnemy = totalDamage * 1.5f; // 150%傷害加成
            for (EntityLivingBase enemy : enemies) {
                enemy.attackEntityFrom(DamageSource.causePlayerDamage(player), damagePerEnemy);
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "[Buffer Overflow] " +
                    TextFormatting.GRAY + String.format("釋放 %.1f 傷害給 %d 個敵人", damagePerEnemy, enemies.size())), true);
        }

        // 效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    player.posX, player.posY + 0.5, player.posZ,
                    5, 2, 0.5, 2, 0);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        }
    }

    // ========== 靴子：踩踏崩潰 ==========

    private static final Map<UUID, BlockPos> LAST_BLOCK_POS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerMove(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 檢查靴子
        if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.FEET)) return;

        // 檢查是否在地面上
        if (!player.onGround) return;

        BlockPos currentPos = player.getPosition().down();
        UUID uuid = player.getUniqueID();
        BlockPos lastPos = LAST_BLOCK_POS.get(uuid);

        // 檢查是否移動到新方塊
        if (lastPos != null && lastPos.equals(currentPos)) return;
        LAST_BLOCK_POS.put(uuid, currentPos);

        // 1%機率觸發崩潰
        if (RANDOM.nextFloat() < 0.01f) {
            triggerBlockCollapse(player, currentPos);
        }
    }

    private static void triggerBlockCollapse(EntityPlayer player, BlockPos pos) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 暫時將方塊變成空氣（視覺效果）
        // 使用粒子模擬崩潰
        world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                30, 0.3, 0.1, 0.3, 0.05,
                net.minecraft.block.Block.getStateId(world.getBlockState(pos)));

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.5f, 0.5f);

        // 給周圍敵人造成輕微傷害
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(pos).grow(2.0),
                e -> e != player && e instanceof EntityMob);

        for (EntityLivingBase entity : nearby) {
            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 2.0f);
            entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1)); // 2秒緩慢
        }

        if (!nearby.isEmpty()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "[Stack Underflow] " +
                    TextFormatting.GRAY + "地面崩潰！"), true);
        }
    }

    // ========== 全套：藍屏死機 (BSOD) ==========

    /**
     * 啟動BSOD領域（由外部調用）
     */
    public static boolean activateBSOD(EntityPlayer player) {
        if (!ItemGlitchArmor.hasFullSet(player)) return false;

        // 檢查能量（需要與機械核心聯動）
        // TODO: 檢查50,000 RF消耗

        BSODData bsod = new BSODData(System.currentTimeMillis() + 5000); // 5秒持續
        BSOD_ACTIVE.put(player.getUniqueID(), bsod);

        // 初始效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.BLUE + "§l[BSOD] " +
                TextFormatting.WHITE + "A problem has been detected..."), false);

        return true;
    }

    private static void processBSOD(EntityPlayer player, BSODData bsod) {
        // 獲取範圍內的敵人
        List<EntityLivingBase> enemies = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(10.0),
                e -> e != player && e instanceof EntityMob);

        // 凍結敵人
        for (EntityLivingBase entity : enemies) {
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
            entity.setNoGravity(true);

            // 累積傷害
            bsod.accumulatedDamage += 0.5f;

            // 記錄受影響的敵人
            bsod.affectedEntities.add(entity);
        }

        // 玩家攻擊力加成
        if (!player.isPotionActive(MobEffects.STRENGTH)) {
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20, 2)); // 3倍攻擊力
        }

        // 粒子效果
        if (player.ticksExisted % 5 == 0 && player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double x = player.posX + Math.cos(angle) * 10;
                double z = player.posZ + Math.sin(angle) * 10;
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        x, player.posY + 1, z, 1, 0, 0, 0.0, 0);
            }
        }

        // 時間到，結算
        if (!bsod.isActive()) {
            endBSOD(player, bsod);
            BSOD_ACTIVE.remove(player.getUniqueID());
        }
    }

    private static void endBSOD(EntityPlayer player, BSODData bsod) {
        // 對所有受影響敵人造成累積傷害的200%
        float finalDamage = bsod.accumulatedDamage * 2.0f;

        for (EntityLivingBase entity : bsod.affectedEntities) {
            entity.setNoGravity(false);
            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), finalDamage);
        }

        // 爆炸效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                    player.posX, player.posY + 1, player.posZ, 3, 2, 1, 2.0, 0);
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.BLUE + "[BSOD] " +
                TextFormatting.WHITE + String.format("System restored. Damage dealt: %.1f", finalDamage)), false);
    }

    // ========== 工具方法 ==========

    private static void spawnGlitchParticles(EntityPlayer player, int count) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < count; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 1.5;
            double offsetY = RANDOM.nextDouble() * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 1.5;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0.0, 0);
        }
    }

    // ========== 數據類 ==========

    private static class CheckpointData {
        BlockPos position;
        int dimensionId;
        long createdTime;

        CheckpointData(BlockPos pos, int dim) {
            this.position = pos;
            this.dimensionId = dim;
            this.createdTime = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - createdTime < 10000; // 10秒有效
        }
    }

    private static class BufferData {
        final List<DamageEntry> entries = new ArrayList<>();

        void addDamage(float amount) {
            entries.add(new DamageEntry(amount, System.currentTimeMillis()));
        }

        float processHeal() {
            long now = System.currentTimeMillis();
            float totalHeal = 0;

            Iterator<DamageEntry> iter = entries.iterator();
            while (iter.hasNext()) {
                DamageEntry entry = iter.next();
                // 10秒後返還為治療
                if (now - entry.time >= 10000) {
                    totalHeal += entry.amount;
                    iter.remove();
                }
            }

            return totalHeal;
        }

        float getTotalBuffered() {
            return (float) entries.stream().mapToDouble(e -> e.amount).sum();
        }

        private static class DamageEntry {
            float amount;
            long time;

            DamageEntry(float amount, long time) {
                this.amount = amount;
                this.time = time;
            }
        }
    }

    private static class BSODData {
        long endTime;
        float accumulatedDamage = 0;
        Set<EntityLivingBase> affectedEntities = new HashSet<>();

        BSODData(long endTime) {
            this.endTime = endTime;
        }

        boolean isActive() {
            return System.currentTimeMillis() < endTime;
        }
    }
}
