package com.moremod.module.example;

import com.moremod.module.api.IModuleContext;
import com.moremod.module.api.IModuleDescriptor;
import com.moremod.module.api.IModuleHost;
import com.moremod.module.base.AbstractModule;
import com.moremod.module.host.PlayerModuleHost;
import com.moremod.module.impl.ModuleDescriptorImpl;
import com.moremod.module.integration.EventBusIntegration;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;

/**
 * 能量增幅模块示例
 *
 * 功能:
 * - 每20tick给玩家添加速度效果
 * - 展示完整的模块生命周期
 * - 展示事件系统集成
 */
public class EnergyBoostModule extends AbstractModule {

    public static final String MODULE_ID = "moremod:energy_boost";

    private int tickCounter = 0;

    public EnergyBoostModule() {
        super(MODULE_ID, "能量增幅模块");
    }

    @Override
    protected IModuleDescriptor createDescriptor() {
        return new ModuleDescriptorImpl.Builder(MODULE_ID)
                .name("Energy Boost Module")
                .version("1.0.0")
                .description("提供速度增幅效果的示例模块")
                .authors("MoreMod Team")
                .priority(10)
                .optional(true)
                .build();
    }

    @Override
    public boolean init(@Nonnull IModuleContext context) {
        context.info("Initializing EnergyBoostModule");

        // 尝试注册事件监听器
        if (EventBusIntegration.isAvailable()) {
            EventBusIntegration.registerModuleListener(this, context);
            context.info("Event listener registered for EnergyBoostModule");
        } else {
            context.warn("Event bus not available, EnergyBoostModule will use tick-based updates");
        }

        return super.init(context);
    }

    @Override
    public boolean attach(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        if (!super.attach(host, context)) {
            return false;
        }

        // 发送激活消息
        if (host instanceof PlayerModuleHost) {
            EntityPlayer player = ((PlayerModuleHost) host).getPlayer();
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "⚡ " + TextFormatting.AQUA + "能量增幅模块已激活！"
                ));
            }
        }

        tickCounter = 0;
        return true;
    }

    @Override
    public void onTick(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        if (!(host instanceof PlayerModuleHost)) {
            return;
        }

        EntityPlayer player = ((PlayerModuleHost) host).getPlayer();
        if (player.world.isRemote) {
            return;
        }

        tickCounter++;

        // 每20tick (1秒) 应用效果
        if (tickCounter >= 20) {
            applyEnergyBoost(player, context);
            tickCounter = 0;
        }
    }

    @Override
    public void detach(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // 发送停用消息
        if (host instanceof PlayerModuleHost) {
            EntityPlayer player = ((PlayerModuleHost) host).getPlayer();
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "能量增幅模块已停用"
                ));
            }
        }

        super.detach(host, context);
    }

    @Override
    public void unload(@Nonnull IModuleContext context) {
        context.info("Unloading EnergyBoostModule");

        // 注销事件监听器
        if (EventBusIntegration.isAvailable()) {
            EventBusIntegration.unregisterModuleListener(this, context);
        }

        super.unload(context);
    }

    /**
     * 应用能量增幅效果
     */
    private void applyEnergyBoost(EntityPlayer player, IModuleContext context) {
        // 添加速度效果 (2级, 100tick = 5秒)
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1, true, false));

        // 添加急迫效果 (1级, 100tick = 5秒)
        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 100, 0, true, false));

        context.debug("Applied energy boost to player: " + player.getName());
    }

    /**
     * 可选：通过事件系统处理 tick（如果可用）
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isActive()) {
            return;
        }

        // 这里可以添加额外的事件驱动逻辑
        // 注意：这与 onTick() 不冲突，可以共存
    }
}
