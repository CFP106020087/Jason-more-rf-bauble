package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.compat.IceAndFireReflectionHelper;
import com.moremod.init.ModItems;
import com.moremod.moremod;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 七圣遗物事件处理器
 *
 * 处理七圣物的被动效果（携带在背包或饰品栏时生效）
 *
 * 1. 霜华之露 - 攻击敌人时施加冰龙冰冻效果
 * 2. 安眠香囊 - 睡眠起床后给予大量正面buff
 * 3. 灵魂锚点 - 免疫吸取/位移（通过Mixin实现）
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class SacredRelicEventHandler {

    // 安眠香囊buff持续时间（半个游戏日 = 12000 ticks = 10分钟）
    private static final int SLUMBER_BUFF_DURATION = 12000;
    private static final int SLUMBER_BUFF_AMPLIFIER = 4; // 5级（0起始）

    // 霜华之露冰冻时间（ticks）
    private static final int FROST_DEW_FREEZE_TICKS = 100; // 5秒

    /**
     * 霜华之露：攻击敌人时施加Ice and Fire的冰冻效果
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource() == null || event.getSource().getTrueSource() == null) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        EntityLivingBase target = event.getEntityLiving();
        if (target == null || target == player) return;

        // 检查玩家是否携带霜华之露
        if (!hasRelic(player, ModItems.FROST_DEW)) return;

        // 尝试应用Ice and Fire的冰冻效果
        boolean frozen = IceAndFireReflectionHelper.setFrozen(target, FROST_DEW_FREEZE_TICKS);

        // 如果Ice and Fire不可用，给予缓慢和虚弱作为替代
        if (!frozen) {
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, FROST_DEW_FREEZE_TICKS, 2, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, FROST_DEW_FREEZE_TICKS, 1, false, true));
        }

        // 冰冻粒子效果
        if (target.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) target.world;
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.SNOW_SHOVEL,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * 安眠香囊：成功睡眠度过夜晚起床后给予大量正面buff
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否是自然醒来（不是被打断）
        // wakeImmediately = true 表示被打断，shouldSetSpawn = true 表示成功睡眠
        if (event.wakeImmediately()) return;

        // 检查是否成功度过夜晚（世界时间已更新到白天）
        long worldTime = player.world.getWorldTime() % 24000;
        if (worldTime >= 12000) return; // 还是夜晚，说明被打断了

        // 检查玩家是否携带安眠香囊
        if (!hasRelic(player, ModItems.SLUMBER_SACHET)) return;

        // 给予5级正面buff，持续半个游戏日
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.LUCK, SLUMBER_BUFF_DURATION, SLUMBER_BUFF_AMPLIFIER, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, SLUMBER_BUFF_DURATION, 0, false, true)); // 夜视只有1级

        // 发送消息
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "✦ " + TextFormatting.LIGHT_PURPLE + "安眠香囊的香气弥漫..." +
                        TextFormatting.WHITE + " 获得持续10分钟的强力增益！"
        ));

        // 粒子效果
        if (player.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) player.world;
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    player.posX, player.posY + 1, player.posZ,
                    30, 1.0, 1.0, 1.0, 0.1);
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.END_ROD,
                    player.posX, player.posY + 1, player.posZ,
                    20, 0.5, 0.5, 0.5, 0.05);
        }

        // 播放音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * 检查玩家是否携带指定圣物（背包或饰品栏）
     */
    public static boolean hasRelic(EntityPlayer player, Item relic) {
        if (relic == null) return false;

        // 检查背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == relic) {
                return true;
            }
        }

        // 检查饰品栏
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == relic) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查玩家是否携带灵魂锚点（供Mixin调用）
     */
    public static boolean hasSoulAnchor(EntityPlayer player) {
        return hasRelic(player, ModItems.SOUL_ANCHOR);
    }
}
