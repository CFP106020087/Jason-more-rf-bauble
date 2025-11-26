package com.moremod.event;

import com.moremod.tile.TileEntityProtectionField;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 保护领域事件处理器
 * 使用与不朽护符相同的机制，确保完全的保护效果
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ProtectionFieldHandler {

    // 缓存活跃的保护领域发生器
    private static final int SEARCH_RADIUS = 128; // 搜索半径

    /**
     * 查找玩家附近的所有活跃保护领域发生器
     */
    private static List<TileEntityProtectionField> findNearbyGenerators(World world, BlockPos playerPos) {
        List<TileEntityProtectionField> generators = new ArrayList<>();

        if (world.isRemote) {
            return generators;
        }

        // 简单的范围搜索方法 - 更兼容1.12.2
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x += 16) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y += 16) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z += 16) {
                    BlockPos checkPos = playerPos.add(x, y, z);

                    // 检查位置是否在加载的区块内
                    if (!world.isBlockLoaded(checkPos)) {
                        continue;
                    }

                    // 获取该位置的TileEntity
                    TileEntity te = world.getTileEntity(checkPos);
                    if (te instanceof TileEntityProtectionField) {
                        TileEntityProtectionField generator = (TileEntityProtectionField) te;
                        if (generator.isActive()) {
                            generators.add(generator);
                        }
                    }
                }
            }
        }

        return generators;
    }

    /**
     * 检查玩家是否在任何保护领域内
     */
    private static boolean isPlayerProtected(EntityPlayer player) {
        if (player == null || player.world.isRemote) {
            return false;
        }

        // 创造模式和观察者模式不需要保护
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        List<TileEntityProtectionField> generators = findNearbyGenerators(player.world, player.getPosition());

        for (TileEntityProtectionField generator : generators) {
            if (generator.isPlayerInRange(player)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 核心保护机制 - 阻止敌对生物锁定受保护的玩家
     * 使用与不朽护符相同的强制保护逻辑
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        // 检查是否是非玩家实体尝试锁定玩家
        if (!(event.getEntityLiving() instanceof EntityPlayer) && event.getTarget() instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) event.getTarget();

            // 如果玩家在保护领域内
            if (isPlayerProtected(targetPlayer)) {
                EntityLivingBase attacker = event.getEntityLiving();

                // 如果是EntityLiving（大部分敌对生物）
                if (attacker instanceof EntityLiving) {
                    EntityLiving mob = (EntityLiving) attacker;

                    // 立即清除目标
                    mob.setAttackTarget(null);

                    // 设置假目标，防止生物继续追踪
                    if (targetPlayer.equals(mob.getRevengeTarget())) {
                        mob.setRevengeTarget(new DummyTarget(mob.world));
                    }

                    // 清除导航
                    if (mob.getNavigator() != null) {
                        mob.getNavigator().clearPath();
                    }
                }
            }
        }
    }

    /**
     * 持续检查并清理目标（确保保护效果）
     * 每tick都检查，确保没有遗漏
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityLiving && !event.getEntityLiving().world.isRemote) {
            EntityLiving mob = (EntityLiving) event.getEntityLiving();

            // 检查当前攻击目标
            if (mob.getAttackTarget() instanceof EntityPlayer) {
                EntityPlayer target = (EntityPlayer) mob.getAttackTarget();
                if (isPlayerProtected(target)) {
                    // 强制清除目标
                    mob.setAttackTarget(null);
                    mob.setRevengeTarget(new DummyTarget(mob.world));
                    mob.getNavigator().clearPath();

                    // 重置AI任务
                    mob.setAttackTarget(null);
                    mob.setLastAttackedEntity(null);

                    // 让生物看向其他方向
                    mob.getLookHelper().setLookPosition(
                            mob.posX + mob.world.rand.nextGaussian() * 10,
                            mob.posY,
                            mob.posZ + mob.world.rand.nextGaussian() * 10,
                            10.0F, 10.0F
                    );
                }
            }

            // 检查复仇目标
            if (mob.getRevengeTarget() instanceof EntityPlayer) {
                EntityPlayer target = (EntityPlayer) mob.getRevengeTarget();
                if (isPlayerProtected(target)) {
                    mob.setRevengeTarget(new DummyTarget(mob.world));
                }
            }
        }
    }

    /**
     * 假目标实体类 - 用于迷惑敌对生物
     * 与不朽护符使用相同的机制
     */
    public static class DummyTarget extends EntityLivingBase {
        public DummyTarget(World world) {
            super(world);
            this.setInvisible(true);
            this.setSize(0.0F, 0.0F);
            this.setEntityInvulnerable(true);
            this.isDead = true; // 标记为死亡，防止被持续追踪
        }

        @Override
        public void onUpdate() {
            // 立即标记为死亡
            this.isDead = true;
        }

        @Override
        public boolean isEntityAlive() {
            return false;
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
            // 不做任何事
        }

        @Override
        public Iterable<ItemStack> getArmorInventoryList() {
            return java.util.Collections.emptyList();
        }

        @Override
        public EnumHandSide getPrimaryHand() {
            return EnumHandSide.RIGHT;
        }
    }
}