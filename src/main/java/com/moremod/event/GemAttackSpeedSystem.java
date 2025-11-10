package com.moremod.event;

import com.moremod.compat.crafttweaker.GemSocketHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * 宝石攻速系统 - 精确补偿伤害衰减
 * 
 * 原理：
 * 1. 读取宝石的攻速倍率（如19.4）
 * 2. 计算实际攻击间隔（tick）
 * 3. 精确计算需要的攻速属性，完全抵消伤害衰减
 * 
 * 公式：
 * - 攻击间隔(tick) = 计算方式取决于实现（见下方）
 * - 需要攻速值 = 20 / 攻击间隔(tick)
 * - 攻速加成 = 需要攻速值 - 4.0（基础）
 * 
 * 效果：
 * - 攻击频率提升
 * - 攻速属性同步提升
 * - 伤害不衰减！
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class GemAttackSpeedSystem {
    
    private static final UUID ATTACK_SPEED_UUID = 
        UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final String MODIFIER_NAME = "Gem Attack Speed";
    
    private static final float BASE_ATTACK_SPEED = 4.0f;
    private static boolean debugMode = true; // 默认开启调试
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        
        EntityPlayer player = event.player;
        ItemStack mainHand = player.getHeldItemMainhand();
        
        // 计算需要的攻速加成
        AttackSpeedData data = calculateAttackSpeed(mainHand);
        
        // 应用属性修饰符
        updateAttackSpeedModifier(player, data);
    }
    
    /**
     * 计算攻速数据
     */
    private static AttackSpeedData calculateAttackSpeed(ItemStack weapon) {
        AttackSpeedData data = new AttackSpeedData();
        
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) {
            return data;
        }
        
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return data;
        }
        
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        float totalMultiplier = 0.0f;
        int gemCount = 0;
        
        for (ItemStack gem : gems) {
            if (gem.isEmpty()) continue;
            
            NBTTagCompound nbt = gem.getTagCompound();
            if (nbt == null) continue;
            
            if (!nbt.hasKey("GemData")) continue;
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");
            
            if (!gemData.hasKey("identified") || gemData.getByte("identified") != 1) {
                continue;
            }
            
            if (!gemData.hasKey("affixes")) continue;
            
            try {
                NBTTagList affixList = gemData.getTagList("affixes", 10);
                
                for (int i = 0; i < affixList.tagCount(); i++) {
                    NBTTagCompound affixTag = affixList.getCompoundTagAt(i);
                    String id = affixTag.getString("id");
                    
                    if (id.contains("auto_attack")) {
                        if (affixTag.hasKey("value")) {
                            float value = affixTag.getFloat("value");
                            totalMultiplier += value;
                            gemCount++;
                            
                            if (debugMode) {
                                System.out.println("[AttackSpeed] 宝石词条: " + id + " = " + value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (debugMode) {
                    e.printStackTrace();
                }
            }
        }
        
        if (gemCount == 0 || totalMultiplier <= 0) {
            return data;
        }
        
        data.hasAutoAttack = true;
        data.multiplier = totalMultiplier;
        
        // ==================== 核心计算 ====================
        
        // 方案1：线性间隔计算（原ClientAutoAttackHandler的方式）
        // interval = 10 / multiplier
        float interval1 = 10.0f / totalMultiplier;
        
        // 方案2：分段间隔计算（平衡版的方式）
        float interval2;
        if (totalMultiplier >= 90.0f) {
            interval2 = 1.0f;
        } else if (totalMultiplier >= 48.0f) {
            interval2 = 2.0f;
        } else if (totalMultiplier >= 24.0f) {
            interval2 = 3.0f;
        } else if (totalMultiplier >= 12.0f) {
            interval2 = 5.0f;
        } else {
            interval2 = Math.max(2.0f, 10.0f / totalMultiplier);
        }
        
        // 选择使用哪个方案（可以在这里切换）
        float attackInterval = interval2; // 使用方案2（平衡版）
        
        // 计算需要的攻速值
        // 公式：攻速值 = 20 / 攻击间隔(tick)
        float requiredAttackSpeed = 20.0f / attackInterval;
        
        // 计算攻速加成
        // 加成 = 需要攻速 - 基础攻速(4.0)
        float attackSpeedBonus = requiredAttackSpeed - BASE_ATTACK_SPEED;
        
        data.attackInterval = attackInterval;
        data.requiredAttackSpeed = requiredAttackSpeed;
        data.attackSpeedBonus = attackSpeedBonus;
        
        if (debugMode) {
            System.out.println(String.format(
                "[AttackSpeed] ========== 计算结果 ==========\n" +
                "[AttackSpeed] 宝石数量: %d\n" +
                "[AttackSpeed] 攻速倍率: %.2f\n" +
                "[AttackSpeed] 攻击间隔: %.2f tick\n" +
                "[AttackSpeed] 需要攻速: %.2f\n" +
                "[AttackSpeed] 攻速加成: %.2f\n" +
                "[AttackSpeed] 最终攻速: %.2f\n" +
                "[AttackSpeed] ====================================",
                gemCount,
                totalMultiplier,
                attackInterval,
                requiredAttackSpeed,
                attackSpeedBonus,
                requiredAttackSpeed
            ));
        }
        
        return data;
    }
    
    /**
     * 更新攻速属性修饰符
     */
    private static void updateAttackSpeedModifier(EntityPlayer player, AttackSpeedData data) {
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;
        
        AttributeModifier oldModifier = attackSpeed.getModifier(ATTACK_SPEED_UUID);
        if (oldModifier != null) {
            attackSpeed.removeModifier(oldModifier);
        }
        
        if (data.hasAutoAttack && data.attackSpeedBonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                ATTACK_SPEED_UUID,
                MODIFIER_NAME,
                data.attackSpeedBonus,
                0  // 加法
            );
            
            attackSpeed.applyModifier(modifier);
            
            if (debugMode) {
                System.out.println(String.format(
                    "[AttackSpeed] ✅ 应用属性修饰符 +%.2f (总攻速: %.2f)",
                    data.attackSpeedBonus,
                    attackSpeed.getAttributeValue()
                ));
            }
        }
    }
    
    /**
     * 攻速数据类
     */
    private static class AttackSpeedData {
        boolean hasAutoAttack = false;
        float multiplier = 0.0f;
        float attackInterval = 0.0f;
        float requiredAttackSpeed = 0.0f;
        float attackSpeedBonus = 0.0f;
    }
    
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
}