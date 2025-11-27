package com.moremod.client;

import com.moremod.item.ItemEnergySword;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 能量剑客户端处理器 - 负责自动攻击逻辑
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = "moremod")
public class EnergySwordClientHandler {

    private static int attackTicker = 0;
    private static boolean isAutoAttacking = false;
    private static boolean mouseHeld = false;

    /**
     * 监听鼠标点击事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(MouseEvent event) {
        if (event.getButton() != 0) return; // 只处理左键
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        ItemStack held = mc.player.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemEnergySword)) return;

        // 检查能量和出鞘条件
        IEnergyStorage st = held.getCapability(CapabilityEnergy.ENERGY, null);
        boolean powered = st != null && st.getEnergyStored() > 0;
        boolean allow = powered && ItemEnergySword.canUnsheathe(mc.player, held);

        mouseHeld = event.isButtonstate();

        if (mouseHeld && allow) {
            // 拦截默认攻击，启动自动攻击
            event.setCanceled(true);
            performAttack(mc);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.resetCooldown();
            isAutoAttacking = true;
            attackTicker = 0;
        }

        if (!mouseHeld) {
            // 释放鼠标时停止自动攻击
            isAutoAttacking = false;
            attackTicker = 0;
        }
    }

    /**
     * 客户端Tick - 持续自动攻击
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.world == null) return;

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemEnergySword)) {
            isAutoAttacking = false;
            return;
        }

        // 检查条件
        IEnergyStorage st = stack.getCapability(CapabilityEnergy.ENERGY, null);
        boolean powered = st != null && st.getEnergyStored() > 0;
        boolean allow = powered && ItemEnergySword.canUnsheathe(mc.player, stack);

        if (!allow || !mouseHeld) {
            isAutoAttacking = false;
            return;
        }

        if (isAutoAttacking) {
            attackTicker++;
            if (attackTicker >= 3) { // 每3tick攻击一次
                performAttack(mc);
                mc.player.swingArm(EnumHand.MAIN_HAND);
                mc.player.resetCooldown();
                attackTicker = 0;
            }
        }
    }

    /**
     * 执行攻击动作
     */
    private static void performAttack(Minecraft mc) {
        Entity target = getMouseOverEntity(mc, 6.0D);
        if (target != null) {
            mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPacketUseEntity(target));
            mc.player.attackTargetEntityWithCurrentItem(target);
        } else {
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }
    }

    /**
     * 获取准星指向的实体
     */
    private static Entity getMouseOverEntity(Minecraft mc, double distance) {
        if (mc.getRenderViewEntity() == null) return null;
        
        Vec3d eyes = mc.getRenderViewEntity().getPositionEyes(1.0F);
        Vec3d look = mc.getRenderViewEntity().getLook(1.0F);
        Vec3d reach = eyes.add(look.x * distance, look.y * distance, look.z * distance);

        Entity pointed = null;
        double minDist = distance;
        List<Entity> list = mc.world.getEntitiesWithinAABBExcludingEntity(
                mc.getRenderViewEntity(),
                mc.getRenderViewEntity().getEntityBoundingBox()
                        .expand(look.x * distance, look.y * distance, look.z * distance)
                        .grow(1.0D)
        );

        for (Entity e : list) {
            if (!e.canBeCollidedWith()) continue;
            AxisAlignedBB aabb = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            RayTraceResult hit = aabb.calculateIntercept(eyes, reach);
            if (hit != null) {
                double dist = eyes.distanceTo(hit.hitVec);
                if (dist < minDist) {
                    minDist = dist;
                    pointed = e;
                }
            }
        }
        return pointed;
    }

    /**
     * 重置状态（用于切换物品等情况）
     */
    public static void reset() {
        isAutoAttacking = false;
        attackTicker = 0;
        mouseHeld = false;
    }
}