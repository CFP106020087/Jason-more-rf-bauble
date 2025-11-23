package com.moremod.tile;

import com.moremod.compat.crafttweaker.GemExtractionHelper;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 提纯祭坛 - TileEntity
 * 
 * 功能：
 * 1. 接受2-5个相同类型精炼宝石
 * 2. 提纯生成高品质宝石
 * 3. 显示预测品质
 * 4. 消耗经验
 */
public class TileEntityPurificationAltar extends TileEntity implements ITickable {
    
    // 库存：5个输入槽 + 1个输出槽
    private final ItemStackHandler inventory = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityPurificationAltar.this.markDirty();
            // 内容变化时同步到客户端
            if (!world.isRemote) {
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
            } else if (canPurify()) {
                spawnIdleParticles();
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
    // 提纯逻辑
    // ==========================================
    
    /**
     * 开始提纯
     * 
     * @param player 玩家（用于扣除经验）
     * @return 是否成功开始
     */
    public boolean startPurifying(EntityPlayer player) {
        if (world.isRemote) return false;
        if (isPurifying) return false;
        if (!canPurify()) return false;
        
        // 检查经验
        int gemCount = getInputGemCount();
        int requiredXP = gemCount * 2;
        
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
     * 完成提纯
     */
    private void finishPurifying() {
        if (world.isRemote) return;
        
        // 收集输入宝石
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack.copy());
            }
        }
        
        // 提纯
        ItemStack result = GemExtractionHelper.purifyAffixes(inputGems);
        
        if (!result.isEmpty()) {
            // 清空输入槽
            for (int i = 0; i < 5; i++) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            
            // 放入输出槽
            inventory.setStackInSlot(5, result);
            
            // 播放音效
            world.playSound(null, pos, 
                net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
                net.minecraft.util.SoundCategory.BLOCKS, 
                1.0F, 1.0F);
        }
        
        // 重置状态
        isPurifying = false;
        purifyProgress = 0;
        
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), 
                               world.getBlockState(pos), 3);
    }
    
    // ==========================================
    // 检测逻辑
    // ==========================================
    
    /**
     * 检查是否可以提纯
     */
    public boolean canPurify() {
        // 输出槽不能有物品
        if (!inventory.getStackInSlot(5).isEmpty()) {
            return false;
        }
        
        // 收集输入宝石
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack);
            }
        }
        
        // 至少2个宝石
        if (inputGems.size() < 2 || inputGems.size() > 5) {
            return false;
        }
        
        // 检查是否都是已鉴定的宝石
        for (ItemStack gem : inputGems) {
            if (!GemNBTHelper.isIdentified(gem)) {
                return false;
            }
            
            // 检查是否是精炼宝石（单词条）
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            if (affixes.size() != 1) {
                return false;
            }
        }
        
        // 检查是否相同类型
        String firstAffixId = null;
        for (ItemStack gem : inputGems) {
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            if (affixes.isEmpty()) return false;
            
            String affixId = affixes.get(0).getAffix().getId();
            
            if (firstAffixId == null) {
                firstAffixId = affixId;
            } else if (!firstAffixId.equals(affixId)) {
                return false; // 类型不同
            }
        }
        
        return true;
    }
    
    /**
     * 获取输入宝石数量
     */
    public int getInputGemCount() {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 预测提纯后的品质
     */
    public int getPredictedQuality() {
        List<ItemStack> inputGems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputGems.add(stack);
            }
        }
        
        return GemExtractionHelper.predictPurifyQuality(inputGems);
    }
    
    /**
     * 获取需要的经验等级
     */
    public int getRequiredXP() {
        return getInputGemCount() * 2;
    }
    
    // ==========================================
    // 粒子效果
    // ==========================================
    
    /**
     * 生成提纯中的粒子效果
     */
    private void spawnPurifyingParticles() {
        if (world.rand.nextInt(2) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.8;
            double z = pos.getZ() + 0.5;
            
            // 螺旋上升的粒子
            double angle = (purifyProgress * 0.2) % 360;
            double radius = 0.3;
            double offsetX = Math.cos(Math.toRadians(angle)) * radius;
            double offsetZ = Math.sin(Math.toRadians(angle)) * radius;
            
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                x + offsetX, y + (purifyProgress * 0.01), z + offsetZ,
                0, 0.05, 0);
            
            // 金色粒子
            if (world.rand.nextInt(5) == 0) {
                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    x + (world.rand.nextDouble() - 0.5),
                    y + world.rand.nextDouble(),
                    z + (world.rand.nextDouble() - 0.5),
                    0, 0.05, 0);
            }
        }
    }
    
    /**
     * 生成待机粒子效果
     */
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