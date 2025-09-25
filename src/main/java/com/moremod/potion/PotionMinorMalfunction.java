package com.moremod.potion;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 輕微故障藥水效果 - 淋雨時的輕度懲罰
 */
public class PotionMinorMalfunction extends Potion {

    // 自定義材質路徑（與完整故障共用）
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/gui/potion_effects.png");

    // 圖標位置（像素） - 使用不同位置
    private static final int ICON_U = 54;  // 第4個圖標位置
    private static final int ICON_V = 0;   // 第一行

    private final Random random = new Random();

    // 配置常量
    private static final int BASE_ENERGY_DRAIN = 5;        // 基礎能量消耗 FE/秒
    private static final int ENERGY_DRAIN_PER_LEVEL = 2;  // 每級額外消耗
    private static final int EFFECT_CHANCE_BASE = 30;     // 基礎效果觸發機率%
    private static final int EFFECT_CHANCE_PER_LEVEL = 20;// 每級額外機率%

    public PotionMinorMalfunction() {
        super(true, 0xFFAA44); // 橙色，表示輕微故障
        setPotionName("effect.moremod.minor_malfunction");
        setRegistryName("moremod", "minor_malfunction");
        // 不使用 setIconIndex
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        // 每20 ticks (1秒) 執行一次，比完整故障慢
        return duration % 20 == 0;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);

        if (core.isEmpty()) {
            // 沒有核心就不影響
            return;
        }

        IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(core);
        if (energy == null) {
            return;
        }

        // 輕微能量流失
        int energyDrain = calculateMinorEnergyDrain(amplifier);
        int actualDrained = energy.extractEnergy(energyDrain, false);

        // 顯示效果（每2秒一次）
        if (player.world.getTotalWorldTime() % 40 == 0) {
            displayMinorMalfunctionEffects(player, amplifier, actualDrained, energy);
        }

        // 輕微隨機效果（每5秒檢查一次）
        if (player.world.getTotalWorldTime() % 100 == 0) {
            applyMinorDebuffs(player, amplifier);
        }

        // 偶爾的干擾效果
        if (player.world.getTotalWorldTime() % 60 == 0 && random.nextInt(100) < 20) {
            applyInterferenceEffects(player, amplifier);
        }
    }

    /**
     * 計算輕微能量流失
     */
    private int calculateMinorEnergyDrain(int amplifier) {
        return BASE_ENERGY_DRAIN + (amplifier * ENERGY_DRAIN_PER_LEVEL);
    }

    /**
     * 顯示輕微故障效果
     */
    private void displayMinorMalfunctionEffects(EntityPlayer player, int amplifier, int drained, IEnergyStorage energy) {
        // 能量狀態顯示
        if (drained > 0 && random.nextInt(100) < 50) {
            int current = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();
            float percent = (float) current / max * 100;

            TextFormatting color;
            if (percent > 70) {
                color = TextFormatting.YELLOW;
            } else if (percent > 40) {
                color = TextFormatting.GOLD;
            } else {
                color = TextFormatting.RED;
            }

            // 顯示能量流失訊息
            player.sendStatusMessage(new TextComponentString(
                    color + String.format("⚡ %.1f%% ", percent) +
                            TextFormatting.YELLOW + "[-" + drained + " RF/s] " +
                            TextFormatting.GRAY + "電路受潮"
            ), true);
        }

        // 隨機故障訊息
        if (random.nextInt(100) < 20 + amplifier * 10) {
            sendMinorMalfunctionMessage(player, amplifier);
        }
    }

    /**
     * 發送輕微故障訊息
     */
    private void sendMinorMalfunctionMessage(EntityPlayer player, int amplifier) {
        String[] normalMessages = {
                "電路受潮警告",
                "檢測到濕度過高",
                "外殼滲水",
                "絕緣性能下降",
                "連接器氧化",
                "防護塗層劣化"
        };

        String[] severeMessages = {
                "電路板積水",
                "多處短路警告",
                "系統進水預警",
                "防護失效警告"
        };

        String message;
        TextFormatting color;

        if (amplifier >= 1 && player.world.isThundering()) {
            // 雷雨天的嚴重訊息
            message = severeMessages[random.nextInt(severeMessages.length)];
            color = TextFormatting.GOLD;

            // 較高機率輕微亂碼
            if (random.nextInt(100) < 20) {
                message = lightlyCorruptMessage(message);
            }
        } else {
            // 一般訊息
            message = normalMessages[random.nextInt(normalMessages.length)];
            color = TextFormatting.YELLOW;

            // 低機率輕微亂碼
            if (amplifier >= 1 && random.nextInt(100) < 10) {
                message = lightlyCorruptMessage(message);
            }
        }

        player.sendMessage(new TextComponentString(color + "⚡ " + message));
    }

    /**
     * 輕微文字損壞
     */
    private String lightlyCorruptMessage(String message) {
        StringBuilder corrupted = new StringBuilder();
        String corruptChars = "?!#*";

        for (char c : message.toCharArray()) {
            if (random.nextInt(100) < 8) { // 8%機率損壞
                corrupted.append(corruptChars.charAt(random.nextInt(corruptChars.length())));
            } else {
                corrupted.append(c);
            }
        }
        return corrupted.toString();
    }

    /**
     * 應用輕微負面效果
     */
    private void applyMinorDebuffs(EntityPlayer player, int amplifier) {
        // 計算觸發機率
        int chance = EFFECT_CHANCE_BASE + (amplifier * EFFECT_CHANCE_PER_LEVEL);

        // 雷雨天增加機率
        if (player.world.isThundering()) {
            chance += 20;
        }

        if (random.nextInt(100) >= chance) {
            return;
        }

        // 輕微效果池（短時間、低等級）
        int effect = random.nextInt(5);
        switch (effect) {
            case 0: // 輕微減速
                player.addPotionEffect(new PotionEffect(
                        MobEffects.SLOWNESS,
                        60 + amplifier * 20,  // 3-4秒
                        0,
                        false,
                        false
                ));
                break;

            case 1: // 輕微挖掘疲勞
                player.addPotionEffect(new PotionEffect(
                        MobEffects.MINING_FATIGUE,
                        40 + amplifier * 20,  // 2-3秒
                        0,
                        false,
                        false
                ));
                break;

            case 2: // 偶爾視線模糊（僅高等級或雷雨）
                if (amplifier >= 1 || player.world.isThundering()) {
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.NAUSEA,
                            20 + amplifier * 10,  // 1-1.5秒
                            0,
                            false,
                            false
                    ));
                }
                break;

            case 3: // 輕微虛弱
                player.addPotionEffect(new PotionEffect(
                        MobEffects.WEAKNESS,
                        80 + amplifier * 20,  // 4-5秒
                        0,
                        false,
                        false
                ));
                break;

            case 4: // 輕微飢餓（消耗飽食度）
                if (player.getFoodStats().getFoodLevel() > 6) {
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.HUNGER,
                            60 + amplifier * 20,  // 3-4秒
                            amplifier,
                            false,
                            false
                    ));
                }
                break;
        }
    }

    /**
     * 干擾效果（視覺/聽覺干擾）
     */
    private void applyInterferenceEffects(EntityPlayer player, int amplifier) {
        // 隨機視角抖動（很輕微）
        if (amplifier >= 1 && random.nextInt(100) < 10) {
            float yawOffset = (random.nextFloat() - 0.5f) * 2;
            float pitchOffset = (random.nextFloat() - 0.5f) * 1;
            player.rotationYaw += yawOffset;
            player.rotationPitch += pitchOffset;

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "訊號干擾"
            ), true);
        }

        // 雷雨天的額外干擾
        if (player.world.isThundering() && random.nextInt(100) < 15) {
            // 瞬間失明（模擬電路故障）
            player.addPotionEffect(new PotionEffect(
                    MobEffects.BLINDNESS,
                    10,  // 0.5秒
                    0,
                    false,
                    false
            ));

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⚡ 電磁干擾！"
            ));
        }
    }

    @Override
    public boolean isBadEffect() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        mc.getTextureManager().bindTexture(TEXTURE);

        // 根據情況選擇圖標
        int iconU = ICON_U;
        int iconV = ICON_V;

        // 雷雨天使用不同圖標
        if (mc.world != null && mc.world.isThundering()) {
            iconU = 72;  // 第5個位置 - 雷雨圖標
        } else if (effect.getAmplifier() >= 1) {
            iconU = 54;  // 第4個位置 - 加強版圖標
        } else {
            iconU = 54;  // 第4個位置 - 基礎圖標
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

        // 根據情況選擇圖標
        int iconU = ICON_U;
        int iconV = ICON_V;

        // 雷雨天使用不同圖標
        if (mc.world != null && mc.world.isThundering()) {
            iconU = 72;  // 雷雨圖標
        } else if (effect.getAmplifier() >= 1) {
            iconU = 54;  // 加強版圖標
        } else {
            iconU = 54;  // 基礎圖標
        }

        // HUD圖標可以有脈動效果
        float scale = 1.0f;
        if (effect.getDuration() <= 100 && effect.getDuration() % 20 < 10) {
            // 最後5秒閃爍
            scale = 0.8f;
        }

        if (scale < 1.0f) {
            // 縮小的圖標（閃爍效果）
            int offset = (int)((1.0f - scale) * 9);
            Gui.drawModalRectWithCustomSizedTexture(
                    x + 3 + offset, y + 3 + offset,
                    iconU, iconV,
                    (int)(18 * scale), (int)(18 * scale),
                    256, 256
            );
        } else {
            // 正常圖標
            Gui.drawModalRectWithCustomSizedTexture(
                    x + 3, y + 3,
                    iconU, iconV,
                    18, 18,
                    256, 256
            );
        }
    }
}