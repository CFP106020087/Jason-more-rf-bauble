package com.moremod.eventHandler;

import com.moremod.capabilities.autoattack.AutoAttackComboProvider;
import com.moremod.capabilities.autoattack.IAutoAttackCombo;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class ServerAutoAttackHandler {

    private static final int COMBO_TIMEOUT = 60;
    private static final float MAX_COMBO_POWER = 5.0f;
    private static boolean debugMode = false;

    @SubscribeEvent
    public static void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        IAutoAttackCombo cap = player.getCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null);

        if (cap == null) return;

        if (cap.getComboTime() > 0) {
            cap.setComboTime(cap.getComboTime() - 1);

            if (cap.getComboTime() <= 0) {
                if (debugMode) {
                    System.out.println("[AutoAttack] 连击超时，重置");
                }
                cap.resetCombo();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        IAutoAttackCombo cap = attacker.getCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null);

        if (cap == null) return;

        if (!cap.isAutoAttacking()) {
            return;
        }

        ItemStack weapon = attacker.getHeldItemMainhand();
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) {
            return;
        }

        float comboBonus = getComboBonus(weapon);
        if (comboBonus <= 0) {
            return;
        }

        cap.setComboCount(cap.getComboCount() + 1);
        float newPower = Math.min(
                cap.getComboPower() + comboBonus,
                MAX_COMBO_POWER
        );
        cap.setComboPower(newPower);

        cap.setComboTime(COMBO_TIMEOUT);

        float currentDamage = event.getAmount();
        float newDamage = currentDamage * cap.getComboPower();
        event.setAmount(newDamage);

        if (debugMode) {
            System.out.println(String.format("[AutoAttack] 连击×%d, 倍率×%.2f, 伤害: %.1f -> %.1f",
                    cap.getComboCount(), cap.getComboPower(), currentDamage, newDamage));
        }

        if (cap.getComboCount() % 5 == 0) {
            attacker.sendMessage(new net.minecraft.util.text.TextComponentString(
                    String.format("§6连击×%d §7倍率: §c×%.2f",
                            cap.getComboCount(),
                            cap.getComboPower())
            ));
        }
    }

    /**
     * ✅ 修复版：正确读取 GemData.affixes 路径
     */
    private static float getComboBonus(ItemStack weapon) {
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return 0.0f;
        }

        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        float totalBonus = 0.0f;

        for (ItemStack gem : gems) {
            if (gem.isEmpty()) continue;

            NBTTagCompound nbt = gem.getTagCompound();
            if (nbt == null) continue;

            // ✅ 关键修复：先获取 GemData 标签
            if (!nbt.hasKey("GemData")) continue;
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");

            // 检查是否鉴定
            if (!gemData.hasKey("identified") || gemData.getByte("identified") != 1) {
                continue;
            }

            // ✅ 关键修复：从 gemData 读取 affixes
            if (!gemData.hasKey("affixes")) continue;

            try {
                NBTTagList affixList = gemData.getTagList("affixes", 10);

                for (int i = 0; i < affixList.tagCount(); i++) {
                    NBTTagCompound affixTag = affixList.getCompoundTagAt(i);

                    String id = affixTag.getString("id");

                    // 检查是否是连击增伤词条
                    if (id.contains("combo_damage")) {
                        if (affixTag.hasKey("value")) {
                            float value = affixTag.getFloat("value");
                            totalBonus += value;

                            if (debugMode) {
                                System.out.println("[AutoAttack] 连击词条: " + id + " = " + value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (debugMode) {
                    System.err.println("[AutoAttack] 读取连击词条失败: " + e.getMessage());
                }
            }
        }

        return totalBonus;
    }
}
