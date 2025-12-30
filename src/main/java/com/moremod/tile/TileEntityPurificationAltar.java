package com.moremod.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 提纯祭坛 - TileEntity
 * 
 * 功能：
 * 1. 接受2-6个未鉴定宝石（左侧6个槽位）
 * 2. Reroll生成更高gemLevel的宝石
 * 3. 显示预测gemLevel
 * 4. 消耗经验
 * 
 * 槽位布局：
 * - 0-5: 输入槽（6个）
 * - 6: 输出槽（1个）
 */
public class TileEntityPurificationAltar extends TileEntity implements ITickable {
    
    // 常量
    private static final int INPUT_SLOTS = 6;
    private static final int OUTPUT_SLOT = 6;
    private static final int TOTAL_SLOTS = 7;
    
    // 库存：6个输入槽 + 1个输出槽
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityPurificationAltar.this.markDirty();
            // 内容变化时同步到客户端
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), 
                                       world.getBlockState(pos), 3);
            }
        }
    };
    
    // 提纯进度（用于动画）
    private int purifyProgress = 0;
    private int maxPurifyTime = 100; // 5秒（100 tick）
    private boolean isPurifying = false;
    
    // 粒子效果计时器
    private int particleTimer = 0;
    
    // 随机数生成器
    private final Random random = new Random();

    // 性能优化：缓存canPurify结果，避免每tick检查
    private boolean cachedCanPurify = false;
    private int checkCooldown = 0;
    private static final int CHECK_INTERVAL = 10; // 每10tick检查一次

    public TileEntityPurificationAltar() {
        // 构造函数
    }

    // ==========================================
    // ITickable 实现
    // ==========================================

    @Override
    public void update() {
        if (world.isRemote) {
            // 客户端：粒子效果
            if (isPurifying) {
                spawnPurifyingParticles();
            } else {
                // 使用缓存的canPurify结果，定期更新
                checkCooldown++;
                if (checkCooldown >= CHECK_INTERVAL) {
                    checkCooldown = 0;
                    cachedCanPurify = canPurify();
                }
                if (cachedCanPurify) {
                    spawnIdleParticles();
                }
            }
        } else {
            // 服务器：提纯逻辑
            if (isPurifying) {
                purifyProgress++;

                if (purifyProgress >= maxPurifyTime) {
                    // 完成提纯
                    finishPurifying();
                }
            }
        }
    }
    
    // ==========================================
    // Reroll 逻辑
    // ==========================================
    
    /**
     * 开始Reroll
     * 
     * @param player 玩家（用于扣除经验）
     * @return 是否成功开始
     */
    public boolean startPurifying(EntityPlayer player) {
        if (world.isRemote) return false;
        if (isPurifying) return false;
        if (!canPurify()) return false;
        
        // 检查经验
        int requiredXP = getRequiredXP();
        
        if (player.experienceLevel < requiredXP) {
            return false;
        }
        
        // 扣除经验
        player.addExperienceLevel(-requiredXP);
        
        // 开始提纯
        isPurifying = true;
        purifyProgress = 0;
        
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), 
                               world.getBlockState(pos), 3);
        
        return true;
    }
    
    /**
     * 完成Reroll
     */
    private void finishPurifying() {
        if (world.isRemote) return;
        
        // 收集输入宝石
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack.copy());
            }
        }
        
        if (inputGems.isEmpty()) {
            resetState();
            return;
        }
        
        // 执行Reroll
        ItemStack result = rerollGems(inputGems);
        
        if (!result.isEmpty()) {
            // 清空输入槽
            for (int i = 0; i < INPUT_SLOTS; i++) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            
            // 放入输出槽
            inventory.setStackInSlot(OUTPUT_SLOT, result);
            
            // 播放音效
            world.playSound(null, pos, 
                net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
                net.minecraft.util.SoundCategory.BLOCKS, 
                1.0F, 1.2F);
        }
        
        resetState();
    }
    
    /**
     * 重置状态
     */
    private void resetState() {
        isPurifying = false;
        purifyProgress = 0;
        
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), 
                               world.getBlockState(pos), 3);
    }
    
    /**
     * Reroll宝石 - 生成更高gemLevel的未鉴定宝石
     */
    private ItemStack rerollGems(List<ItemStack> inputGems) {
        if (inputGems.isEmpty()) return ItemStack.EMPTY;
        
        // 取第一个宝石作为模板
        ItemStack template = inputGems.get(0).copy();
        template.setCount(1);
        
        // 计算新的gemLevel
        int newGemLevel = calculateNewGemLevel(inputGems);
        
        // 获取或创建NBT
        NBTTagCompound nbt = template.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            template.setTagCompound(nbt);
        }
        
        // 检查是否有GemData子标签
        if (nbt.hasKey("GemData")) {
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");
            
            // 设置新的gemLevel
            gemData.setInteger("gemLevel", newGemLevel);
            
            // 保持未鉴定状态
            gemData.setByte("identified", (byte) 0);
            
            // 增加rerollCount
            int currentReroll = gemData.getInteger("rerollCount");
            gemData.setInteger("rerollCount", currentReroll + 1);
            
        } else {
            // 直接在根NBT设置
            nbt.setInteger("gemLevel", newGemLevel);
            nbt.setByte("identified", (byte) 0);
            
            int currentReroll = nbt.getInteger("rerollCount");
            nbt.setInteger("rerollCount", currentReroll + 1);
        }
        
        return template;
    }
    
    /**
     * 计算新的gemLevel
     * 
     * 公式：max(输入gemLevel) + baseBonus + randomBonus + sameBonus
     * 
     * baseBonus = count - 1 (投入越多提升越大)
     * randomBonus = 0 ~ 2 (随机额外提升)
     * sameBonus = 如果所有宝石等级相同，额外+2
     * 
     * 例如（全部Lv8）：
     * - 2个宝石：8 + 1 + (0~2) + 2 = 11 ~ 13
     * - 3个宝石：8 + 2 + (0~2) + 2 = 12 ~ 14
     * - 4个宝石：8 + 3 + (0~2) + 2 = 13 ~ 15
     * - 5个宝石：8 + 4 + (0~2) + 2 = 14 ~ 16
     * - 6个宝石：8 + 5 + (0~2) + 2 = 15 ~ 17
     */
    private int calculateNewGemLevel(List<ItemStack> inputGems) {
        int maxLevel = 0;
        
        for (ItemStack gem : inputGems) {
            int level = getGemLevel(gem);
            maxLevel = Math.max(maxLevel, level);
        }
        
        int count = inputGems.size();
        
        // 基础提升：count - 1
        int baseBonus = count - 1;
        
        // 随机提升：0, 1, 或 2
        int randomBonus = random.nextInt(3);
        
        // 额外奖励：如果所有宝石等级相同，额外+2
        final int finalMaxLevel = maxLevel;
        boolean allSameLevel = inputGems.stream()
            .allMatch(g -> getGemLevel(g) == finalMaxLevel);
        int sameBonus = allSameLevel ? 2 : 0;
        
        int result = maxLevel + baseBonus + randomBonus + sameBonus;
        System.out.println("[calculateNewGemLevel] maxLevel=" + maxLevel + 
            " + base=" + baseBonus + " + random=" + randomBonus + 
            " + same=" + sameBonus + " = " + result);
        
        return result;
    }
    
    /**
     * 从宝石获取gemLevel
     */
    private int getGemLevel(ItemStack gem) {
        if (gem.isEmpty() || !gem.hasTagCompound()) {
            return 1;
        }
        
        NBTTagCompound nbt = gem.getTagCompound();
        
        // 检查GemData子标签
        if (nbt.hasKey("GemData")) {
            return nbt.getCompoundTag("GemData").getInteger("gemLevel");
        }
        
        // 直接从根NBT读取
        return nbt.getInteger("gemLevel");
    }
    
    /**
     * 检查宝石是否未鉴定
     */
    private boolean isUnidentified(ItemStack gem) {
        if (gem.isEmpty()) {
            System.out.println("[isUnidentified] gem is empty");
            return false;
        }
        
        if (!gem.hasTagCompound()) {
            System.out.println("[isUnidentified] gem has no NBT");
            return false;
        }
        
        NBTTagCompound nbt = gem.getTagCompound();
        System.out.println("[isUnidentified] NBT: " + nbt.toString());
        
        // 检查GemData子标签
        if (nbt.hasKey("GemData")) {
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");
            byte identified = gemData.getByte("identified");
            System.out.println("[isUnidentified] GemData.identified = " + identified);
            return identified == 0;
        }
        
        // 直接从根NBT读取
        byte identified = nbt.getByte("identified");
        System.out.println("[isUnidentified] root.identified = " + identified);
        return identified == 0;
    }
    
    // ==========================================
    // 检测逻辑
    // ==========================================
    
    /**
     * 检查是否可以Reroll
     */
    public boolean canPurify() {
        // 输出槽不能有物品
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }

        // 收集输入宝石
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack);
            }
        }

        // 至少2个宝石，最多6个
        if (inputGems.size() < 2 || inputGems.size() > INPUT_SLOTS) {
            return false;
        }

        // 检查是否都是未鉴定的宝石
        for (ItemStack gem : inputGems) {
            if (!isUnidentified(gem)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * 获取输入宝石数量
     */
    public int getInputGemCount() {
        int count = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 预测Reroll后的最小gemLevel
     */
    public int getPredictedQuality() {
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack);
            }
        }
        
        if (inputGems.isEmpty()) {
            return 0;
        }
        
        int maxLevel = 0;
        for (ItemStack gem : inputGems) {
            maxLevel = Math.max(maxLevel, getGemLevel(gem));
        }
        
        int count = inputGems.size();
        int baseBonus = count - 1;
        
        // 检查是否全部相同等级
        final int finalMaxLevel = maxLevel;
        boolean allSameLevel = inputGems.stream()
            .allMatch(g -> getGemLevel(g) == finalMaxLevel);
        int sameBonus = allSameLevel ? 2 : 0;
        
        // 最小值 = max + base + 0(随机最小) + same
        return maxLevel + baseBonus + sameBonus;
    }
    
    /**
     * 获取预测的最大gemLevel（含随机）
     */
    public int getPredictedMaxQuality() {
        // 最大值 = 最小值 + 2(随机最大)
        return getPredictedQuality() + 2;
    }
    
    /**
     * 获取当前最高的输入gemLevel
     */
    public int getMaxInputGemLevel() {
        int maxLevel = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                maxLevel = Math.max(maxLevel, getGemLevel(stack));
            }
        }
        return maxLevel;
    }
    
    /**
     * 获取需要的经验等级
     * 
     * 公式：宝石数量 * (最高gemLevel / 2)
     */
    public int getRequiredXP() {
        int count = getInputGemCount();
        int maxLevel = getMaxInputGemLevel();
        return count * Math.max(1, maxLevel / 2);
    }
    
    // ==========================================
    // 粒子效果
    // ==========================================
    
    private void spawnPurifyingParticles() {
        if (world.rand.nextInt(2) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.8;
            double z = pos.getZ() + 0.5;
            
            double angle = (purifyProgress * 0.2) % 360;
            double radius = 0.3;
            double offsetX = Math.cos(Math.toRadians(angle)) * radius;
            double offsetZ = Math.sin(Math.toRadians(angle)) * radius;
            
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                x + offsetX, y + (purifyProgress * 0.01), z + offsetZ,
                0, 0.05, 0);
            
            if (world.rand.nextInt(3) == 0) {
                world.spawnParticle(EnumParticleTypes.PORTAL,
                    x + (world.rand.nextDouble() - 0.5),
                    y + world.rand.nextDouble() * 0.5,
                    z + (world.rand.nextDouble() - 0.5),
                    0, 0.1, 0);
            }
        }
    }
    
    private void spawnIdleParticles() {
        particleTimer++;
        
        if (particleTimer % 10 == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.6;
            double z = pos.getZ() + 0.5;
            
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                x + (world.rand.nextDouble() - 0.5) * 0.5,
                y,
                z + (world.rand.nextDouble() - 0.5) * 0.5,
                0, 0.03, 0);
        }
    }
    
    // ==========================================
    // 库存访问
    // ==========================================
    
    public ItemStackHandler getInventory() {
        return inventory;
    }
    
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }
    
    public void setStackInSlot(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }
    
    public boolean isPurifying() {
        return isPurifying;
    }
    
    public int getPurifyProgress() {
        return purifyProgress;
    }
    
    public int getMaxPurifyTime() {
        return maxPurifyTime;
    }
    
    public int getInputSlotCount() {
        return INPUT_SLOTS;
    }
    
    public int getOutputSlotIndex() {
        return OUTPUT_SLOT;
    }
    
    // ==========================================
    // NBT 序列化
    // ==========================================
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setBoolean("IsPurifying", isPurifying);
        compound.setInteger("PurifyProgress", purifyProgress);
        
        return compound;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        isPurifying = compound.getBoolean("IsPurifying");
        purifyProgress = compound.getInteger("PurifyProgress");
    }
    
    // ==========================================
    // 网络同步
    // ==========================================
    
    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = super.getUpdateTag();
        compound.setBoolean("IsPurifying", isPurifying);
        compound.setInteger("PurifyProgress", purifyProgress);
        return compound;
    }
    
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound compound = pkt.getNbtCompound();
        isPurifying = compound.getBoolean("IsPurifying");
        purifyProgress = compound.getInteger("PurifyProgress");
    }
}