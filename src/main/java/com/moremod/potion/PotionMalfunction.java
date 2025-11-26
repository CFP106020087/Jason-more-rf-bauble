package com.moremod.potion;

import com.moremod.config.MalfunctionConfig;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * 完整故障藥水效果 - 水中觸發的嚴重故障
 */
public class PotionMalfunction extends Potion {

    // 自定義材質路徑
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/gui/potion_effects1.png");

    // 圖標位置（像素）
    private static final int ICON_U = 0;  // 第一個圖標 X座標
    private static final int ICON_V = 0;  // 第一行 Y座標

    private final Random random = new Random();

    // 自定義傷害源
    public static final DamageSource MALFUNCTION_DAMAGE = new DamageSource("malfunction")
            .setDamageBypassesArmor()
            .setMagicDamage();

    // 緩存解析後的配置
    private static Map<String, Integer> potionMaxLevels = null;
    private static Map<String, int[]> potionDurations = null;
    private static List<Potion> allowedPotions = null;
    private static long configCacheTime = 0;
    private static final long CONFIG_CACHE_DURATION = 60000; // 1分鐘緩存

    public PotionMalfunction() {
        super(true, 0xFF4444); // 紅色，表示嚴重故障
        setPotionName("effect.moremod.malfunction");
        setRegistryName("moremod", "malfunction");
        // 不使用 setIconIndex，改用自定義渲染
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        // 每10 ticks (0.5秒) 執行一次
        return duration % 10 == 0;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;

        // 獲取機械核心
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) {
            // 沒有核心，直接扣血
            if (MalfunctionConfig.damage.damageEnabled) {
                applyDamageEffect(player, amplifier);
            }
            return;
        }

        // 獲取能量存儲
        IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(core);
        if (energy == null) {
            if (MalfunctionConfig.damage.damageEnabled) {
                applyDamageEffect(player, amplifier);
            }
            return;
        }

        // 計算能量流失量
        int energyDrain = calculateEnergyDrain(amplifier);

        // 嘗試消耗能量
        int actualDrained = energy.extractEnergy(energyDrain, false);

        // 如果能量不足或已耗盡
        if (actualDrained < energyDrain || energy.getEnergyStored() <= 0) {
            // 開始扣血
            if (MalfunctionConfig.damage.damageEnabled) {
                applyDamageEffect(player, amplifier);
            }

            // 發送警告
            if (player.world.getTotalWorldTime() % 20 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 核心能量耗盡！系統損壞中！"
                ), true);
            }
        }

        // 顯示能量流失效果
        displayEnergyDrainEffect(player, actualDrained, energy);

        // 隨機負面效果（基於配置）
        int effectInterval = MalfunctionConfig.effects.effectInterval * 20; // 轉換為ticks
        if (player.world.getTotalWorldTime() % effectInterval == 0) {
            applyConfiguredDebuffs(player, amplifier);
        }

        // 故障消息和視覺效果
        if (player.world.getTotalWorldTime() % 40 == 0) {
            displayMalfunctionEffects(player, amplifier, energy);
        }
    }

    /**
     * 計算能量流失量（基於配置）
     */
    private int calculateEnergyDrain(int amplifier) {
        double base = MalfunctionConfig.energy.baseEnergyDrainRate / 2.0; // 每0.5秒執行一次
        double multiplier = 1.0 + (amplifier * MalfunctionConfig.energy.energyDrainMultiplier);
        return (int)(base * multiplier);
    }

    /**
     * 應用傷害效果（基於配置）
     */
    private void applyDamageEffect(EntityPlayer player, int amplifier) {
        // 根據配置的間隔
        int damageInterval = MalfunctionConfig.damage.damageInterval * 20; // 轉換為ticks
        if (player.world.getTotalWorldTime() % damageInterval != 0) {
            return;
        }

        // 計算傷害
        double damage = MalfunctionConfig.damage.baseDamage +
                (amplifier * MalfunctionConfig.damage.damagePerLevel);

        // 創建傷害源
        DamageSource source = MalfunctionConfig.damage.bypassArmor ?
                MALFUNCTION_DAMAGE : new DamageSource("malfunction");

        // 造成傷害
        player.attackEntityFrom(source, (float)damage);

        // 傷害提示
        String damageMsg = String.format("系統損壞 -%.1f❤", damage / 2);
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_RED + "⚠ " + damageMsg
        ), false);
    }

    /**
     * 應用配置的負面效果
     */
    private void applyConfiguredDebuffs(EntityPlayer player, int amplifier) {
        // 更新緩存
        updateConfigCache();

        if (allowedPotions == null || allowedPotions.isEmpty()) {
            return;
        }

        // 獲取當前活躍的負面效果數量
        int activeEffects = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect() && effect.getPotion() != this) {
                activeEffects++;
            }
        }

        // 檢查是否達到上限
        if (activeEffects >= MalfunctionConfig.effects.maxSimultaneousEffects) {
            return;
        }

        // 隨機選擇效果
        List<Potion> availablePotions = new ArrayList<>();
        for (Potion potion : allowedPotions) {
            if (!player.isPotionActive(potion)) {
                availablePotions.add(potion);
            }
        }

        if (availablePotions.isEmpty()) {
            return;
        }

        // 應用隨機效果
        int effectsToApply = Math.min(
                MalfunctionConfig.effects.maxSimultaneousEffects - activeEffects,
                1 + amplifier
        );

        for (int i = 0; i < effectsToApply && !availablePotions.isEmpty(); i++) {
            Potion selectedPotion = availablePotions.get(random.nextInt(availablePotions.size()));
            availablePotions.remove(selectedPotion);

            // 獲取配置的等級和持續時間
            int maxLevel = getMaxLevel(selectedPotion, amplifier);
            int[] durationRange = getDuration(selectedPotion);

            int effectLevel = Math.min(random.nextInt(amplifier + 1), maxLevel);
            int effectDuration = (durationRange[0] + random.nextInt(
                    durationRange[1] - durationRange[0] + 1)) * 20; // 轉換為ticks

            player.addPotionEffect(new PotionEffect(
                    selectedPotion,
                    effectDuration,
                    effectLevel,
                    false,
                    true
            ));
        }
    }

    /**
     * 更新配置緩存
     */
    private void updateConfigCache() {
        long currentTime = System.currentTimeMillis();
        if (allowedPotions != null && currentTime - configCacheTime < CONFIG_CACHE_DURATION) {
            return;
        }

        // 解析藥水白名單
        allowedPotions = new ArrayList<>();
        for (String potionName : MalfunctionConfig.effects.potionWhitelist) {
            ResourceLocation loc = new ResourceLocation(potionName);
            Potion potion = ForgeRegistries.POTIONS.getValue(loc);
            if (potion != null && potion != this) {
                allowedPotions.add(potion);
            }
        }

        // 解析最大等級
        potionMaxLevels = new HashMap<>();
        for (String entry : MalfunctionConfig.effects.potionMaxLevels) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                String potionName = parts[0] + ":" + parts[1];
                try {
                    int maxLevel = Integer.parseInt(parts[2]);
                    potionMaxLevels.put(potionName, maxLevel);
                } catch (NumberFormatException e) {
                    // 忽略格式錯誤的條目
                }
            }
        }

        // 解析持續時間
        potionDurations = new HashMap<>();
        for (String entry : MalfunctionConfig.effects.potionDurations) {
            String[] parts = entry.split(":");
            if (parts.length >= 4) {
                String potionName = parts[0] + ":" + parts[1];
                try {
                    int minDuration = Integer.parseInt(parts[2]);
                    int maxDuration = Integer.parseInt(parts[3]);
                    potionDurations.put(potionName, new int[]{minDuration, maxDuration});
                } catch (NumberFormatException e) {
                    // 忽略格式錯誤的條目
                }
            }
        }

        configCacheTime = currentTime;
    }

    /**
     * 獲取藥水的最大等級
     */
    private int getMaxLevel(Potion potion, int malfunctionLevel) {
        String key = potion.getRegistryName().toString();
        if (potionMaxLevels != null && potionMaxLevels.containsKey(key)) {
            return potionMaxLevels.get(key);
        }
        return malfunctionLevel;
    }

    /**
     * 獲取藥水的持續時間範圍
     */
    private int[] getDuration(Potion potion) {
        String key = potion.getRegistryName().toString();
        if (potionDurations != null && potionDurations.containsKey(key)) {
            return potionDurations.get(key);
        }
        return new int[]{3, 8}; // 默認3-8秒
    }

    /**
     * 顯示能量流失效果
     */
    private void displayEnergyDrainEffect(EntityPlayer player, int drained, IEnergyStorage energy) {
        if (drained <= 0) return;

        if (player.world.getTotalWorldTime() % 40 == 0) {
            int current = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();
            float percent = (float) current / max * 100;

            TextFormatting color;
            if (percent > 50) {
                color = TextFormatting.YELLOW;
            } else if (percent > 20) {
                color = TextFormatting.GOLD;
            } else if (percent > 5) {
                color = TextFormatting.RED;
            } else {
                color = TextFormatting.DARK_RED;
            }

            int drainPerSecond = drained * 2;
            player.sendStatusMessage(new TextComponentString(
                    color + String.format("⚡ %.1f%% ", percent) +
                            TextFormatting.RED + "[-" + drainPerSecond + " RF/s]"
            ), true);
        }
    }

    /**
     * 顯示故障效果和消息
     */
    private void displayMalfunctionEffects(EntityPlayer player, int amplifier, IEnergyStorage energy) {
        if (random.nextInt(100) < 30 + amplifier * 20) {
            sendMalfunctionMessage(player, amplifier, energy);
        }

        // 嚴重故障的額外效果
        if (amplifier >= 2) {
            if (random.nextInt(100) < 5) {
                double offsetX = (random.nextDouble() - 0.5) * 3;
                double offsetZ = (random.nextDouble() - 0.5) * 3;
                player.setPositionAndUpdate(
                        player.posX + offsetX,
                        player.posY,
                        player.posZ + offsetZ
                );
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 空間定位系統故障！"
                ));
            }

            if (random.nextInt(100) < 2) {
                player.dropItem(false);
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 物品保持系統失效！"
                ));
            }
        }
    }

    private void sendMalfunctionMessage(EntityPlayer player, int amplifier, IEnergyStorage energy) {
        String[] messages = {
                "檢測到能量泄漏...",
                "警告：防護系統失效",
                "錯誤：絕緣層損壞",
                "系統錯誤 0xDEADBEEF",
                "核心溫度異常",
                "電路短路警告"
        };

        TextFormatting color = amplifier == 0 ? TextFormatting.YELLOW :
                amplifier == 1 ? TextFormatting.GOLD :
                        TextFormatting.RED;

        String message = messages[random.nextInt(messages.length)];
        if (amplifier >= 1 && random.nextInt(100) < 30) {
            message = corruptMessage(message);
        }

        player.sendMessage(new TextComponentString(color + message));
    }

    private String corruptMessage(String message) {
        StringBuilder corrupted = new StringBuilder();
        String corruptChars = "!@#$%^&*_+-=[]{}|;:,.<>?/~`";

        for (char c : message.toCharArray()) {
            if (random.nextInt(100) < 15) {
                corrupted.append(corruptChars.charAt(random.nextInt(corruptChars.length())));
            } else {
                corrupted.append(c);
            }
        }

        return corrupted.toString();
    }

    @Override
    public boolean isBadEffect() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        mc.getTextureManager().bindTexture(TEXTURE);

        // 根據等級選擇不同圖標
        int iconU = ICON_U;
        int iconV = ICON_V;

        int amplifier = effect.getAmplifier();
        if (amplifier >= 2) {
            iconU = 36;  // 嚴重故障圖標 (第3個位置)
        } else if (amplifier >= 1) {
            iconU = 18;  // 中度故障圖標 (第2個位置)
        }

        Gui.drawModalRectWithCustomSizedTexture(
                x + 6, y + 7,           // 介面位置
                iconU, iconV,           // 材質圖中的位置
                18, 18,                 // 圖標大小
                256, 256                // 材質圖總大小
        );
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(TEXTURE);

        // 根據等級選擇不同圖標
        int iconU = ICON_U;
        int iconV = ICON_V;

        int amplifier = effect.getAmplifier();
        if (amplifier >= 2) {
            iconU = 36;  // 嚴重故障圖標
        } else if (amplifier >= 1) {
            iconU = 18;  // 中度故障圖標
        }

        Gui.drawModalRectWithCustomSizedTexture(
                x + 3, y + 3,           // HUD位置
                iconU, iconV,           // 材質圖中的位置
                18, 18,                 // 圖標大小
                256, 256                // 材質圖總大小
        );
    }
}