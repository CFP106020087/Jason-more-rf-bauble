package com.moremod.capabilities.autoattack;

import com.moremod.capabilities.autoattack.IAutoAttackCombo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 自动攻击Capability注册
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AutoAttackCapabilityHandler {
    
    private static final ResourceLocation AUTO_ATTACK_CAP_LOC = 
        new ResourceLocation("moremod", "auto_attack_combo");
    
    /**
     * 注册Capability（在主mod类中调用）
     */
    public static void registerCapability() {
        net.minecraftforge.common.capabilities.CapabilityManager.INSTANCE.register(
            IAutoAttackCombo.class,
            new AutoAttackComboStorage(),
            AutoAttackCombo::new
        );
    }
    
    /**
     * 附加Capability到玩家
     */
    @SubscribeEvent
    public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            if (!event.getObject().hasCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null)) {
                event.addCapability(AUTO_ATTACK_CAP_LOC, new AutoAttackComboProvider());
            }
        }
    }
    
    /**
     * 玩家复制时保持Capability数据
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            IAutoAttackCombo oldCap = event.getOriginal().getCapability(
                AutoAttackComboProvider.AUTO_ATTACK_CAP, null
            );
            IAutoAttackCombo newCap = event.getEntityPlayer().getCapability(
                AutoAttackComboProvider.AUTO_ATTACK_CAP, null
            );
            
            if (oldCap != null && newCap != null) {
                // 死亡时重置连击数据
                newCap.resetCombo();
                newCap.setAutoAttacking(false);
            }
        }
    }
}
