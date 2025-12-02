package com.moremod.item.chengyue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 澄月 - 生存机制（强化版）
 * 
 * 包含：
 * 1. 月之庇护（强化版）：
 *    - 80%减伤
 *    - 持续5分钟
 *    - 冷却5分钟
 *    - 记忆第一次受到的伤害类型，对该类型提供额外防护
 * 2. 生命偷取：攻击时回血
 * 3. 闪避机制：概率完全闪避攻击
 */
public class ChengYueSurvival {
    
    // NBT键名
    private static final String KEY_AEGIS_ACTIVE = "AegisActive";
    private static final String KEY_AEGIS_DURATION_END = "AegisEndTime";
    private static final String KEY_AEGIS_DAMAGE_TYPE = "AegisDamageType";
    private static final String KEY_AEGIS_LAST_USE = "AegisLastUse";
    
    // 月之庇护参数（强化版）
    private static final long AEGIS_DURATION_TICKS = 7200L; // 6分钟（原5分钟）
    private static final long AEGIS_COOLDOWN_TICKS = 4800L; // 4分钟（原5分钟）
    private static final float AEGIS_REDUCTION = 0.85f; // 85%减伤（原80%）
    private static final float AEGIS_MEMORY_REDUCTION = 0.92f; // 记忆类型减伤 92%（原80%）
    
    // ==================== 月之庇护（强化版）====================
    
    /**
     * 尝试激活月之庇护
     * 
     * @param player 玩家
     * @param stack 澄月物品
     * @param damage 即将受到的伤害
     * @param source 伤害来源
     * @return true如果成功激活
     */
    public static boolean tryActivateAegis(EntityPlayer player, ItemStack stack, float damage, DamageSource source) {
        if (stack.isEmpty() || player.world.isRemote) {
            return false;
        }
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        World world = player.world;
        long currentTime = world.getTotalWorldTime();
        
        // 检查是否已经激活
        if (isAegisActive(stack, currentTime)) {
            return false; // 已经激活，不需要再激活
        }
        
        // 检查冷却
        long lastUse = nbt.getLong(KEY_AEGIS_LAST_USE);
        if (currentTime - lastUse < AEGIS_COOLDOWN_TICKS) {
            return false; // 还在冷却中
        }
        
        // 激活条件：伤害 >= 玩家当前生命的15%（更容易触发）
        float threshold = player.getHealth() * 0.15f;
        
        if (damage >= threshold) {
            // 激活月之庇护！
            nbt.setBoolean(KEY_AEGIS_ACTIVE, true);
            nbt.setLong(KEY_AEGIS_DURATION_END, currentTime + AEGIS_DURATION_TICKS);
            nbt.setLong(KEY_AEGIS_LAST_USE, currentTime);
            
            // 记录第一次受到的伤害类型
            String damageType = getDamageTypeName(source);
            nbt.setString(KEY_AEGIS_DAMAGE_TYPE, damageType);
            
            // 通知玩家
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "【月之庇护】" +
                TextFormatting.GOLD + " 激活！" +
                TextFormatting.GRAY + " (持续6分钟)"
            ));
            
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "对 " +
                TextFormatting.YELLOW + damageType +
                TextFormatting.GRAY + " 类型伤害提供额外防护！"
            ));
            
            // 音效
            world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                ForgeRegistries.SOUND_EVENTS.getValue(
                    new net.minecraft.util.ResourceLocation("block.enchantment_table.use")
                ),
                SoundCategory.PLAYERS,
                1.5f, 1.2f
            );
            
            // 粒子效果
            spawnAegisActivationParticles(player);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 应用月之庇护减伤
     * 
     * @param player 玩家
     * @param stack 澄月物品
     * @param damage 原始伤害
     * @param source 伤害来源
     * @return 减伤后的伤害
     */
    public static float applyAegisReduction(EntityPlayer player, ItemStack stack, float damage, DamageSource source) {
        if (stack.isEmpty() || player.world.isRemote) {
            return damage;
        }
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        World world = player.world;
        long currentTime = world.getTotalWorldTime();
        
        // 检查是否激活
        if (!isAegisActive(stack, currentTime)) {
            return damage;
        }
        
        // 检查是否过期
        long endTime = nbt.getLong(KEY_AEGIS_DURATION_END);
        if (currentTime >= endTime) {
            // 过期，停用
            deactivateAegis(stack, player);
            return damage;
        }
        
        // 应用减伤
        String rememberedType = nbt.getString(KEY_AEGIS_DAMAGE_TYPE);
        String currentType = getDamageTypeName(source);
        
        // 如果是记忆的伤害类型，额外减伤
        boolean isMemorizedType = rememberedType.equals(currentType);
        float reduction = isMemorizedType ? AEGIS_MEMORY_REDUCTION : AEGIS_REDUCTION; // 记忆类型92%，其他类型85%
        
        float reducedDamage = damage * (1.0f - reduction);
        
        // 显示提示
        if (isMemorizedType) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "【庇护】" +
                TextFormatting.GOLD + " ★ " +
                TextFormatting.GRAY + String.format("-%.1f", damage - reducedDamage)
            ), true);
        } else {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "【庇护】" +
                TextFormatting.GRAY + String.format("-%.1f", damage - reducedDamage)
            ), true);
        }
        
        return reducedDamage;
    }
    
    /**
     * 检查月之庇护是否激活
     */
    public static boolean isAegisActive(ItemStack stack, long currentTime) {
        if (stack.isEmpty()) return false;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        if (!nbt.getBoolean(KEY_AEGIS_ACTIVE)) {
            return false;
        }
        
        // 检查是否过期
        long endTime = nbt.getLong(KEY_AEGIS_DURATION_END);
        return currentTime < endTime;
    }
    
    /**
     * 停用月之庇护
     */
    private static void deactivateAegis(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty()) return;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        nbt.setBoolean(KEY_AEGIS_ACTIVE, false);
        nbt.removeTag(KEY_AEGIS_DURATION_END);
        nbt.removeTag(KEY_AEGIS_DAMAGE_TYPE);
        
        if (player != null && !player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "【月之庇护】效果结束"
            ));
        }
    }
    
    /**
     * 获取月之庇护剩余时间（秒）
     */
    public static int getAegisRemainingTime(ItemStack stack, World world) {
        if (stack.isEmpty()) return 0;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        long currentTime = world.getTotalWorldTime();
        
        if (!isAegisActive(stack, currentTime)) {
            return 0;
        }
        
        long endTime = nbt.getLong(KEY_AEGIS_DURATION_END);
        long remaining = endTime - currentTime;
        
        return (int)(remaining / 20); // ticks转秒
    }
    
    /**
     * 获取月之庇护冷却剩余时间（秒）
     */
    public static int getAegisCooldownRemaining(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        long currentTime = world.getTotalWorldTime();
        long lastUse = nbt.getLong(KEY_AEGIS_LAST_USE);
        
        long elapsed = currentTime - lastUse;
        if (elapsed >= AEGIS_COOLDOWN_TICKS) {
            return 0; // 已冷却完毕
        }
        
        return (int)((AEGIS_COOLDOWN_TICKS - elapsed) / 20);
    }
    
    /**
     * 检查月之庇护是否可用
     */
    public static boolean isAegisReady(ItemStack stack, World world) {
        long currentTime = world.getTotalWorldTime();
        
        // 如果正在激活中，不算Ready
        if (isAegisActive(stack, currentTime)) {
            return false;
        }
        
        return getAegisCooldownRemaining(stack, world) == 0;
    }
    
    /**
     * 获取伤害类型名称
     */
    private static String getDamageTypeName(DamageSource source) {
        if (source.isFireDamage()) return "火焰";
        if (source.isMagicDamage()) return "魔法";
        if (source.isExplosion()) return "爆炸";
        if (source.isProjectile()) return "远程";
        if (source == DamageSource.FALL) return "摔落";
        if (source == DamageSource.DROWN) return "溺水";
        if (source == DamageSource.STARVE) return "饥饿";
        if (source == DamageSource.WITHER) return "凋零";
        if (source.getTrueSource() != null) return "近战";
        return "未知";
    }
    
    // ==================== 生命偷取 ====================
    
    /**
     * 应用生命偷取
     * 
     * @param attacker 攻击者
     * @param stack 澄月物品
     * @param damageDealt 实际造成的伤害
     */
    public static void applyLifeSteal(EntityLivingBase attacker, ItemStack stack, float damageDealt) {
        if (!(attacker instanceof EntityPlayer) || stack.isEmpty()) {
            return;
        }
        
        EntityPlayer player = (EntityPlayer) attacker;
        ChengYueNBT.init(stack);
        
        // 获取吸血比例（使用月相记忆系统）
        float lifeStealPercent = ChengYueStats.getLifeSteal(stack, player.world);
        
        // 计算回复量
        float healing = damageDealt * lifeStealPercent;
        
        // 应用回复（不超过最大生命值）
        if (healing > 0) {
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            
            if (currentHealth < maxHealth) {
                float actualHealing = Math.min(healing, maxHealth - currentHealth);
                player.heal(actualHealing);
                
                // 显示回复提示（可选，避免刷屏）
                if (actualHealing >= 1.0f) {
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "+" + String.format("%.1f", actualHealing) + " ♥"
                    ), true);
                }
            }
        }
    }
    
    // ==================== 受伤回复机制（新增）====================

    /**
     * 尝试受伤时回复生命
     *
     * @param player 玩家
     * @param stack 澄月物品
     * @param damage 实际受到的伤害
     * @return 回复的生命值
     */
    public static float tryHurtHeal(EntityPlayer player, ItemStack stack, float damage) {
        if (stack.isEmpty() || player.world.isRemote) {
            return 0.0f;
        }

        ChengYueNBT.init(stack);

        // 获取触发概率
        float chance = ChengYueStats.getHurtHealChance(stack);
        if (chance <= 0) return 0.0f;

        // 概率判定
        if (player.world.rand.nextFloat() >= chance) {
            return 0.0f;
        }

        // 计算回复量
        float healPercent = ChengYueStats.getHurtHealPercent(stack);
        float healing = damage * healPercent;

        if (healing > 0.5f) {
            player.heal(healing);

            // 显示提示
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "【月华反哺】" +
                TextFormatting.GREEN + " +" + String.format("%.1f", healing) + " ♥"
            ), true);

            // 粒子效果
            if (!player.world.isRemote) {
                for (int i = 0; i < 8; i++) {
                    double offsetX = (player.world.rand.nextDouble() - 0.5) * 1.0;
                    double offsetY = player.world.rand.nextDouble() * 2.0;
                    double offsetZ = (player.world.rand.nextDouble() - 0.5) * 1.0;

                    player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.HEART,
                        player.posX + offsetX,
                        player.posY + offsetY,
                        player.posZ + offsetZ,
                        0, 0.1, 0
                    );
                }
            }
        }

        return healing;
    }

    // ==================== 闪避机制 ====================

    /**
     * 尝试闪避攻击
     *
     * @param player 玩家
     * @param stack 澄月物品
     * @param source 伤害来源
     * @return true如果成功闪避
     */
    public static boolean tryDodge(EntityPlayer player, ItemStack stack, DamageSource source) {
        if (stack.isEmpty() || player.world.isRemote) {
            return false;
        }
        
        // 某些伤害类型无法闪避
        if (source.isUnblockable() || source.isDamageAbsolute() || 
            source.isMagicDamage() || source == DamageSource.OUT_OF_WORLD) {
            return false;
        }
        
        ChengYueNBT.init(stack);
        
        // 获取闪避率（使用月相记忆系统）
        float dodgeChance = ChengYueStats.getDodgeChance(stack, player.world);
        
        // 闪避判定
        if (dodgeChance > 0 && player.world.rand.nextFloat() < dodgeChance) {
            // 成功闪避！
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "【闪避】"
            ));
            
            // 音效
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                ForgeRegistries.SOUND_EVENTS.getValue(
                    new net.minecraft.util.ResourceLocation("entity.bat.takeoff")
                ),
                SoundCategory.PLAYERS,
                0.5f, 1.5f
            );
            
            return true;
        }
        
        return false;
    }
    
    // ==================== 减伤计算 ====================
    
    /**
     * 计算最终伤害（应用被动减伤）
     * 
     * @param damage 原始伤害
     * @param stack 澄月物品
     * @param world 世界
     * @return 减伤后的伤害
     */
    public static float applyDamageReduction(float damage, ItemStack stack, World world) {
        if (stack.isEmpty()) {
            return damage;
        }
        
        ChengYueNBT.init(stack);
        
        // 获取减伤比例（使用月相记忆系统）
        float reduction = ChengYueStats.getDamageReduction(stack, world);
        
        // 应用减伤
        return damage * (1.0f - reduction);
    }
    
    // ==================== 粒子效果 ====================
    
    /**
     * 生成月之庇护激活粒子效果
     */
    private static void spawnAegisActivationParticles(EntityPlayer player) {
        if (player.world.isRemote) return;
        
        // 大量粒子爆发
        for (int i = 0; i < 50; i++) {
            double offsetX = (player.world.rand.nextDouble() - 0.5) * 2.5;
            double offsetY = player.world.rand.nextDouble() * 2.5;
            double offsetZ = (player.world.rand.nextDouble() - 0.5) * 2.5;
            
            player.world.spawnParticle(
                net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                player.posX + offsetX,
                player.posY + offsetY,
                player.posZ + offsetZ,
                0, 0.8, 0
            );
        }
        
        // 环形粒子
        for (int i = 0; i < 36; i++) {
            double angle = (i * 10) * Math.PI / 180;
            double offsetX = Math.cos(angle) * 2.0;
            double offsetZ = Math.sin(angle) * 2.0;
            
            player.world.spawnParticle(
                net.minecraft.util.EnumParticleTypes.END_ROD,
                player.posX + offsetX,
                player.posY + 1.0,
                player.posZ + offsetZ,
                0, 0, 0
            );
        }
    }
    
    /**
     * 生成月之庇护持续粒子效果（应该在tick中调用）
     */
    public static void spawnAegisParticles(EntityPlayer player) {
        if (player.world.isRemote) return;
        
        // 环绕粒子
        double angle = (player.world.getTotalWorldTime() * 5) * Math.PI / 180;
        double offsetX = Math.cos(angle) * 1.5;
        double offsetZ = Math.sin(angle) * 1.5;
        
        player.world.spawnParticle(
            net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
            player.posX + offsetX,
            player.posY + 1.0,
            player.posZ + offsetZ,
            0, 0.3, 0
        );
    }
    
    // ==================== 显示信息 ====================
    
    /**
     * 获取月之庇护状态文本（HUD用）
     */
    public static String getAegisStatus(ItemStack stack, World world) {
        long currentTime = world.getTotalWorldTime();
        
        if (isAegisActive(stack, currentTime)) {
            int remaining = getAegisRemainingTime(stack, world);
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            
            ChengYueNBT.init(stack);
            String damageType = stack.getTagCompound().getString(KEY_AEGIS_DAMAGE_TYPE);
            
            return TextFormatting.AQUA + "【庇护】" +
                   TextFormatting.GOLD + " " + damageType +
                   TextFormatting.WHITE + String.format(" %d:%02d", minutes, seconds);
        }
        
        int cdRemaining = getAegisCooldownRemaining(stack, world);
        
        if (cdRemaining == 0) {
            return TextFormatting.GREEN + "月之庇护 ✓";
        } else {
            int minutes = cdRemaining / 60;
            int seconds = cdRemaining % 60;
            return TextFormatting.RED + String.format("月之庇护 %d:%02d", minutes, seconds);
        }
    }
    
    /**
     * 获取生存属性摘要
     */
    public static String getSurvivalInfo(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        StringBuilder sb = new StringBuilder();
        
        // 生命偷取
        float lifeSteal = ChengYueStats.getLifeSteal(stack, world);
        sb.append(String.format("§2生命偷取: §f%.1f%%\n", lifeSteal * 100));
        
        // 减伤
        float reduction = ChengYueStats.getDamageReduction(stack, world);
        sb.append(String.format("§9伤害减免: §f%.1f%%\n", reduction * 100));
        
        // 闪避
        float dodge = ChengYueStats.getDodgeChance(stack, world);
        if (dodge > 0) {
            sb.append(String.format("§b闪避几率: §f%.1f%%\n", dodge * 100));
        }
        
        // 月之庇护
        long currentTime = world.getTotalWorldTime();
        
        sb.append("\n§b【月之庇护】\n");
        sb.append(String.format("§7减伤: §f%.0f%% §7(记忆类型: §6%.0f%%§7)\n", AEGIS_REDUCTION * 100, AEGIS_MEMORY_REDUCTION * 100));
        sb.append("§7持续: §f6分钟\n");
        sb.append("§7冷却: §f4分钟\n");
        
        if (isAegisActive(stack, currentTime)) {
            int remaining = getAegisRemainingTime(stack, world);
            String damageType = stack.getTagCompound().getString(KEY_AEGIS_DAMAGE_TYPE);
            sb.append(String.format("§a激活中: §f%d秒 (§6%s§f)\n", remaining, damageType));
        } else {
            int cdRemaining = getAegisCooldownRemaining(stack, world);
            if (cdRemaining > 0) {
                sb.append(String.format("§c冷却中: §f%d秒\n", cdRemaining));
            } else {
                sb.append("§a已就绪\n");
            }
        }
        
        return sb.toString();
    }
}
