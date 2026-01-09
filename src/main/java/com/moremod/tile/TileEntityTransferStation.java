package com.moremod.tile;

import com.moremod.compat.crafttweaker.GemExtractionHelper;
import com.moremod.compat.crafttweaker.GemLootRuleManager;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.TransferRuneManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 转移台 TileEntity - 增强版
 * 支持配置化的转移符文系统
 *
 * 槽位分配（匹配Container布局）：
 * 0 - 源宝石（左上，精炼宝石，必须是单词条）
 * 1 - 输出槽（中心，只读）
 * 2 - 目标宝石（右上，已鉴定的宝石）
 * 3 - 符文槽（左下，转移符文）
 */
public class TileEntityTransferStation extends TileEntity implements ITickable {

    private static final Random RANDOM = new Random();

    // 物品处理器
    private ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityTransferStation.this.markDirty();
            // 同步到客户端
            if (!world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos),
                        world.getBlockState(pos), 3);
            }

            // 如果改变了符文槽，更新符文信息
            if (slot == 3) {
                updateRuneInfo();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return true;

            switch (slot) {
                case 0: // 源宝石槽（左上）- 必须是精炼宝石
                    return GemNBTHelper.isIdentified(stack) &&
                            GemExtractionHelper.isRefined(stack);

                case 1: // 输出槽（中心）- 不允许放入
                    return false;

                case 2: // 目标宝石槽（右上）- 必须是已鉴定的宝石
                    return GemNBTHelper.isIdentified(stack);

                case 3: // 符文槽（左下）- 检查是否是有效的转移符文
                    return TransferRuneManager.isValidRune(stack);

                default:
                    return false;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 输出槽（1）总是可以取出
            if (slot == 1) {
                return super.extractItem(slot, amount, simulate);
            }

            // 其他槽位也允许取出
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 输出槽不允许插入
            if (slot == 1) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    };

    // 粒子效果计时器
    private int particleTick = 0;

    // 转移状态
    private boolean canTransfer = false;
    private String errorMessage = "";

    // 符文信息缓存
    private float currentSuccessRate = 1.0f;
    private int currentXpCost = 0;
    private int maxAffixLimit = 6;

    // 性能优化：状态检查节流
    private int statusCheckCooldown = 0;
    private static final int STATUS_CHECK_INTERVAL = 10; // 每10tick检查一次

    // ==========================================
    // 核心业务逻辑
    // ==========================================

    @Override
    public void update() {
        if (world.isRemote) {
            // 客户端：粒子效果
            updateParticles();
            return;
        }

        // 服务端：定期检测是否可以转移（节流优化）
        statusCheckCooldown++;
        if (statusCheckCooldown >= STATUS_CHECK_INTERVAL) {
            statusCheckCooldown = 0;
            updateTransferStatus();
        }
    }

    /**
     * 更新转移状态
     */
    private void updateTransferStatus() {
        ItemStack sourceGem = inventory.getStackInSlot(0);   // 左上
        ItemStack targetGem = inventory.getStackInSlot(2);   // 右上
        ItemStack material = inventory.getStackInSlot(3);    // 左下

        // 保存旧状态用于比较
        boolean oldCanTransfer = canTransfer;
        String oldErrorMessage = errorMessage;

        // 重置状态
        canTransfer = false;
        errorMessage = "";

        // 使用标志位来避免多重嵌套
        boolean checksPass = true;

        // 检查源宝石
        if (checksPass && sourceGem.isEmpty()) {
            errorMessage = "请放入源宝石";
            checksPass = false;
        }

        if (checksPass && !GemNBTHelper.isIdentified(sourceGem)) {
            errorMessage = "源宝石未鉴定";
            checksPass = false;
        }

        if (checksPass && !GemExtractionHelper.isRefined(sourceGem)) {
            errorMessage = "需要精炼宝石（单词条）";
            checksPass = false;
        }

        // 检查目标宝石
        if (checksPass && targetGem.isEmpty()) {
            errorMessage = "请放入目标宝石";
            checksPass = false;
        }

        if (checksPass && !GemNBTHelper.isIdentified(targetGem)) {
            errorMessage = "目标宝石未鉴定";
            checksPass = false;
        }

        // 检查材料（转移符文）
        if (checksPass && material.isEmpty()) {
            errorMessage = "需要转移符文";
            checksPass = false;
        }

        if (checksPass && !TransferRuneManager.isValidRune(material)) {
            errorMessage = "无效的转移符文";
            checksPass = false;
        }

        // 更新符文信息
        if (checksPass) {
            updateRuneInfo();
        }

        // 检查目标宝石词条数
        if (checksPass) {
            int targetAffixCount = GemNBTHelper.getAffixCount(targetGem);
            if (targetAffixCount >= maxAffixLimit) {
                errorMessage = String.format("目标宝石已满（%d/%d词条）",
                        targetAffixCount, maxAffixLimit);
                checksPass = false;
            }
        }

        // 检查是否有重复词条类型
        if (checksPass) {
            String sourceAffixType = GemExtractionHelper.getAffixTypeName(sourceGem);
            if (sourceAffixType != null && !sourceAffixType.isEmpty()) {
                try {
                    if (GemExtractionHelper.hasAffixType(targetGem, sourceAffixType)) {
                        errorMessage = "目标宝石已有此类型词条";
                        checksPass = false;
                    }
                } catch (NoSuchMethodError e) {
                    // 如果方法不存在，跳过此检查
                }
            }
        }

        // 检查输出槽
        if (checksPass && !inventory.getStackInSlot(1).isEmpty()) {
            errorMessage = "请先取出输出槽物品";
            checksPass = false;
        }

        // 所有检查通过
        if (checksPass) {
            canTransfer = true;
            errorMessage = "";
        }

        // 如果状态发生变化，通知客户端同步
        if (canTransfer != oldCanTransfer || !errorMessage.equals(oldErrorMessage)) {
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /**
     * 更新当前符文信息
     */
    private void updateRuneInfo() {
        ItemStack material = inventory.getStackInSlot(3);  // 左下符文槽
        if (material.isEmpty()) {
            currentSuccessRate = 1.0f;
            currentXpCost = TransferRuneManager.getBaseXpCost();
            maxAffixLimit = GemLootRuleManager.getMaxAffixes();
            return;
        }

        TransferRuneManager.RuneData data = TransferRuneManager.getRuneData(material);
        if (data != null) {
            currentSuccessRate = data.successRate;
            currentXpCost = TransferRuneManager.getBaseXpCost() + data.xpCost;
            maxAffixLimit = data.getMaxAffixes();
        }
    }

    /**
     * 执行转移操作
     *
     * @param player 执行转移的玩家
     * @return 是否成功
     */
    public boolean performTransfer(EntityPlayer player) {
        if (!canTransfer) {
            return false;
        }

        // 检查经验
        if (player.experienceLevel < currentXpCost) {
            errorMessage = "经验不足";
            return false;
        }

        ItemStack sourceGem = inventory.getStackInSlot(0).copy();  // 左上
        ItemStack targetGem = inventory.getStackInSlot(2).copy();  // 右上
        ItemStack material = inventory.getStackInSlot(3);          // 左下

        // 检查成功率
        boolean success = RANDOM.nextFloat() < currentSuccessRate;

        if (success) {
            // 执行转移
            ItemStack result = GemExtractionHelper.transferAffix(sourceGem, targetGem);

            if (result.isEmpty()) {
                errorMessage = "转移失败：内部错误";
                return false;
            }

            // 成功：输出结果到中心槽
            inventory.setStackInSlot(1, result);

            // 播放成功音效
            playSuccessSound();

            // 生成成功粒子
            spawnSuccessParticles();

        } else {
            // 失败处理
            handleTransferFailure(player);

            // 播放失败音效
            playFailureSound();

            // 生成失败粒子
            spawnFailureParticles();
        }

        // 无论成功失败都要消耗
        // 消耗材料
        material.shrink(1);
        if (material.isEmpty()) {
            inventory.setStackInSlot(3, ItemStack.EMPTY);
        }

        // 消耗经验
        player.addExperienceLevel(-currentXpCost);

        // 清空输入槽
        inventory.setStackInSlot(0, ItemStack.EMPTY);  // 源
        inventory.setStackInSlot(2, ItemStack.EMPTY);  // 目标

        return success;
    }

    /**
     * 处理转移失败
     */
    private void handleTransferFailure(EntityPlayer player) {
        if (TransferRuneManager.isDestroyOnFail()) {
            // 销毁源宝石
            errorMessage = "转移失败！源宝石已销毁";
        } else {
            // 返还源宝石
            ItemStack sourceGem = inventory.getStackInSlot(0).copy();
            if (!player.inventory.addItemStackToInventory(sourceGem)) {
                // 如果背包满了，掉落地上
                player.dropItem(sourceGem, false);
            }
            errorMessage = "转移失败！";
        }

        // 目标宝石总是返还
        ItemStack targetGem = inventory.getStackInSlot(2).copy();
        if (!player.inventory.addItemStackToInventory(targetGem)) {
            player.dropItem(targetGem, false);
        }
    }

    /**
     * 粒子效果更新
     */
    private void updateParticles() {
        if (!canTransfer) return;

        particleTick++;
        if (particleTick % 10 != 0) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // 根据成功率决定粒子颜色
        EnumParticleTypes particleType;
        if (currentSuccessRate >= 0.95f) {
            particleType = EnumParticleTypes.ENCHANTMENT_TABLE; // 绿色
        } else if (currentSuccessRate >= 0.9f) {
            particleType = EnumParticleTypes.PORTAL; // 紫色
        } else {
            particleType = EnumParticleTypes.FLAME; // 橙色（风险）
        }

        for (int i = 0; i < 3; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = world.rand.nextDouble() * 0.5;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.5;

            world.spawnParticle(particleType,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.05, 0);
        }
    }

    /**
     * 生成成功粒子
     */
    private void spawnSuccessParticles() {
        if (world.isRemote) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 30; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.8;
            double offsetY = world.rand.nextDouble() * 0.8;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.8;

            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.1, 0);
        }
    }

    /**
     * 生成失败粒子
     */
    private void spawnFailureParticles() {
        if (world.isRemote) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 20; i++) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    x, y, z,
                    (world.rand.nextDouble() - 0.5) * 0.2,
                    world.rand.nextDouble() * 0.2,
                    (world.rand.nextDouble() - 0.5) * 0.2);
        }
    }

    /**
     * 播放成功音效
     */
    private void playSuccessSound() {
        if (!world.isRemote) {
            world.playSound(null, pos,
                    net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                    net.minecraft.util.SoundCategory.BLOCKS,
                    1.0F, 1.0F);
        }
    }

    /**
     * 播放失败音效
     */
    private void playFailureSound() {
        if (!world.isRemote) {
            world.playSound(null, pos,
                    net.minecraft.init.SoundEvents.ENTITY_ITEM_BREAK,
                    net.minecraft.util.SoundCategory.BLOCKS,
                    1.0F, 0.8F);
        }
    }

    // ==========================================
    // Getter方法（供GUI和Container使用）
    // ==========================================

    public boolean canTransfer() {
        return canTransfer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * 获取当前需要的经验值
     */
    public int getRequiredXp() {
        return currentXpCost;
    }

    /**
     * 获取当前成功率
     */
    public float getSuccessRate() {
        return currentSuccessRate;
    }

    /**
     * 获取当前词条上限
     */
    public int getMaxAffixLimit() {
        return maxAffixLimit;
    }

    // ==========================================
    // NBT保存/加载
    // ==========================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setFloat("SuccessRate", currentSuccessRate);
        compound.setInteger("XpCost", currentXpCost);
        compound.setInteger("MaxAffixes", maxAffixLimit);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        currentSuccessRate = compound.getFloat("SuccessRate");
        currentXpCost = compound.getInteger("XpCost");
        maxAffixLimit = compound.getInteger("MaxAffixes");
    }

    // ==========================================
    // 网络同步
    // ==========================================

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setBoolean("CanTransfer", canTransfer);
        tag.setString("ErrorMsg", errorMessage);
        tag.setFloat("SuccessRate", currentSuccessRate);
        tag.setInteger("XpCost", currentXpCost);
        tag.setInteger("MaxAffixes", maxAffixLimit);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);
        canTransfer = tag.getBoolean("CanTransfer");
        errorMessage = tag.getString("ErrorMsg");
        currentSuccessRate = tag.getFloat("SuccessRate");
        currentXpCost = tag.getInteger("XpCost");
        maxAffixLimit = tag.getInteger("MaxAffixes");
    }

    // ==========================================
    // Capability支持
    // ==========================================

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}