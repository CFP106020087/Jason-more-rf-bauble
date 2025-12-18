package com.moremod.entity;

import com.moremod.config.CapsuleConfig;
import com.moremod.item.ItemStructureCapsule;
import com.moremod.structure.StructureData;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * 投掷的胶囊实体
 */
public class EntityThrownCapsule extends EntityThrowable {

    // 同步胶囊物品数据到客户端（用于渲染）
    private static final DataParameter<ItemStack> CAPSULE_STACK = 
        EntityDataManager.createKey(EntityThrownCapsule.class, DataSerializers.ITEM_STACK);

    private ItemStack capsuleStack = ItemStack.EMPTY;



    public EntityThrownCapsule(World world) {
        super(world);
    }

    public EntityThrownCapsule(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower);
        this.capsuleStack = stack.copy();
        this.dataManager.set(CAPSULE_STACK, this.capsuleStack);
    }

    public EntityThrownCapsule(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CAPSULE_STACK, ItemStack.EMPTY);
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote) {
            spawnParticles();
            return;
        }

        if (capsuleStack.isEmpty() || !(capsuleStack.getItem() instanceof ItemStructureCapsule)) {
            dropCapsule();
            return;
        }

        ItemStructureCapsule capsuleItem = (ItemStructureCapsule) capsuleStack.getItem();
        BlockPos impactPos = getImpactBlockPos(result);

        boolean success;
        boolean wasEmpty = ItemStructureCapsule.isEmpty(capsuleStack);

        if (wasEmpty) {
            // 空胶囊：捕获结构
            success = captureStructure(impactPos, capsuleItem.getCaptureSize());
        } else {
            // 已存储：释放结构
            success = deployStructure(impactPos, capsuleItem.getCaptureSize());
        }

        if (success) {
            // 播放成功音效
            world.playSound(null, posX, posY, posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS,
                1.0F, 1.0F);

            // 单次使用模式
            if (CapsuleConfig.singleUse) {
                if (wasEmpty) {
                    // 捕获成功：返回存储状态的胶囊（可以释放一次）
                    dropCapsule();
                } else {
                    // 释放成功：消耗胶囊，不返回
                    notifyThrower(TextFormatting.YELLOW + "胶囊已消耗");
                }
            } else {
                // 非单次使用模式：返回胶囊
                dropCapsule();
            }
        } else {
            // 播放失败音效
            world.playSound(null, posX, posY, posZ,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS,
                1.0F, 0.5F);

            // 失败时返回胶囊
            dropCapsule();
        }

        this.setDead();
    }

    /**
     * 获取碰撞点的方块位置
     */
    private BlockPos getImpactBlockPos(RayTraceResult result) {
        if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            // 碰到方块，在碰撞面的外侧
            return result.getBlockPos().offset(result.sideHit);
        } else {
            // 碰到实体或其他，使用当前位置
            return new BlockPos(posX, posY, posZ);
        }
    }

    /**
     * 捕获结构
     */
    private boolean captureStructure(BlockPos center, int size) {
        int radius = (size - 1) / 2;

        StructureData data = new StructureData();
        List<BlockPos> captured = data.captureFromWorld(world, center, radius,
            CapsuleConfig.getExcludedBlocks());

        // 收集区域内的掉落物品
        ItemStructureCapsule capsuleItem = (ItemStructureCapsule) capsuleStack.getItem();
        int invSize = capsuleItem.getInventorySize();
        collectItemsInArea(center, radius, invSize);

        if (captured.isEmpty()) {
            // 没有捕获到任何方块（但可能收集了物品）
            return ItemStructureCapsule.getStoredItemCount(capsuleStack) > 0;
        }

        // ========== NBT 大小保护 ==========
        // 检查方块数量
        int blockCount = data.getBlockCount();
        if (blockCount > CapsuleConfig.maxBlockCount) {
            // 方块数量超限，取消捕获
            notifyThrower(TextFormatting.RED + "捕获失败：方块数量 " + blockCount +
                " 超过上限 " + CapsuleConfig.maxBlockCount);
            return false;
        }

        // 估算 NBT 大小
        NBTTagCompound testNBT = new NBTTagCompound();
        data.writeToNBT(testNBT);
        int estimatedSize = estimateNBTSize(testNBT);
        if (estimatedSize > CapsuleConfig.maxNBTSize) {
            // NBT 过大，取消捕获
            notifyThrower(TextFormatting.RED + "捕获失败：数据大小 " + (estimatedSize / 1024) +
                "KB 超过上限 " + (CapsuleConfig.maxNBTSize / 1024) + "KB");
            return false;
        }

        // 从世界中移除捕获的方块
        StructureData.removeBlocksFromWorld(world, captured);

        // 保存到胶囊
        ItemStructureCapsule.saveStructure(capsuleStack, data);

        // 通知成功
        notifyThrower(TextFormatting.GREEN + "捕获成功：" + blockCount + " 个方块");

        return true;
    }

    /**
     * 估算 NBT 数据大小（字节）
     */
    private int estimateNBTSize(NBTTagCompound nbt) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.CompressedStreamTools.writeCompressed(nbt, baos);
            return baos.size();
        } catch (Exception e) {
            // 粗略估算：每个方块约 100 字节
            return nbt.getTagList("blocks", 10).tagCount() * 100;
        }
    }

    /**
     * 通知投掷者
     */
    private void notifyThrower(String message) {
        EntityLivingBase thrower = getThrower();
        if (thrower instanceof EntityPlayer) {
            ((EntityPlayer) thrower).sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(message), true);
        }
    }

    /**
     * 收集区域内的掉落物品
     */
    private void collectItemsInArea(BlockPos center, int radius, int maxSlots) {
        net.minecraft.util.math.AxisAlignedBB area = new net.minecraft.util.math.AxisAlignedBB(
            center.getX() - radius, center.getY(), center.getZ() - radius,
            center.getX() + radius + 1, center.getY() + radius * 2 + 1, center.getZ() + radius + 1
        );

        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area);
        for (EntityItem entityItem : items) {
            ItemStack itemStack = entityItem.getItem();
            ItemStack remaining = ItemStructureCapsule.addItemToStorage(capsuleStack, itemStack, maxSlots);
            if (remaining.isEmpty()) {
                entityItem.setDead();
            } else {
                entityItem.setItem(remaining);
            }
        }
    }

    /**
     * 释放结构
     */
    private boolean deployStructure(BlockPos deployPos, int size) {
        StructureData data = ItemStructureCapsule.loadStructure(capsuleStack);
        boolean hasStructure = (data != null && !data.isEmpty());
        boolean hasItems = ItemStructureCapsule.getStoredItemCount(capsuleStack) > 0;

        if (!hasStructure && !hasItems) {
            return false;
        }

        int radius = (size - 1) / 2;
        // 调整部署位置为结构的最小角
        BlockPos minCorner = deployPos.add(-radius, 0, -radius);

        boolean structureSuccess = true;
        if (hasStructure) {
            // 使用跳过不可破坏方块的部署方法
            structureSuccess = data.deployToWorldSkipUnbreakable(world, minCorner, CapsuleConfig.getOverridableBlocks());
        }

        // 释放存储的物品
        if (hasItems) {
            dropStoredItems(deployPos);
        }

        if (structureSuccess || hasItems) {
            // 清空胶囊
            ItemStructureCapsule.clearStructure(capsuleStack);
            ItemStructureCapsule.clearStoredItems(capsuleStack);
        }

        return structureSuccess || hasItems;
    }

    /**
     * 释放存储的物品
     */
    private void dropStoredItems(BlockPos pos) {
        java.util.List<ItemStack> items = ItemStructureCapsule.getStoredItems(capsuleStack);
        for (ItemStack item : items) {
            EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
            entityItem.setPickupDelay(10);
            entityItem.motionX = (rand.nextDouble() - 0.5) * 0.3;
            entityItem.motionY = 0.2;
            entityItem.motionZ = (rand.nextDouble() - 0.5) * 0.3;
            world.spawnEntity(entityItem);
        }
    }

    /**
     * 掉落胶囊物品
     */
    private void dropCapsule() {
        if (!capsuleStack.isEmpty()) {
            EntityItem entityItem = new EntityItem(world, posX, posY, posZ, capsuleStack.copy());
            entityItem.setPickupDelay(10);
            
            // 给一点向上的速度
            entityItem.motionY = 0.2;
            
            world.spawnEntity(entityItem);
        }
    }

    /**
     * 生成粒子效果
     */
    private void spawnParticles() {
        for (int i = 0; i < 20; i++) {
            double dx = (rand.nextDouble() - 0.5) * 2;
            double dy = (rand.nextDouble() - 0.5) * 2;
            double dz = (rand.nextDouble() - 0.5) * 2;
            world.spawnParticle(EnumParticleTypes.PORTAL, 
                posX, posY, posZ, dx, dy, dz);
        }
    }

    // ============== NBT 保存/加载 ==============

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        if (!capsuleStack.isEmpty()) {
            NBTTagCompound stackNBT = new NBTTagCompound();
            capsuleStack.writeToNBT(stackNBT);
            nbt.setTag("CapsuleStack", stackNBT);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("CapsuleStack")) {
            capsuleStack = new ItemStack(nbt.getCompoundTag("CapsuleStack"));
            this.dataManager.set(CAPSULE_STACK, capsuleStack);
        }
    }

    // ============== 渲染相关 ==============

    public ItemStack getCapsuleStack() {
        return this.dataManager.get(CAPSULE_STACK);
    }

    @Override
    protected float getGravityVelocity() {
        return 0.05F; // 比普通投掷物稍慢的下落速度
    }
}