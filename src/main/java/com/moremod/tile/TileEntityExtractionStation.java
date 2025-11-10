package com.moremod.tile;

import com.moremod.compat.crafttweaker.GemExtractionHelper;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityExtractionStation extends TileEntity implements ITickable {
    
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return GemNBTHelper.isGem(stack);
            }
            return false;
        }
    };
    
    private int workTicks = 0;
    private boolean isWorking = false;
    
    @Override
    public void update() {
        if (world.isRemote) {
            if (isWorking) {
                spawnParticles();
            }
            return;
        }
        
        if (isWorking) {
            workTicks++;
            if (workTicks >= 40) {
                isWorking = false;
                workTicks = 0;
            }
        }
    }
    
    private void spawnParticles() {
        if (world.rand.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                x, y, z, 
                (world.rand.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.rand.nextDouble() - 0.5) * 0.1
            );
        }
    }
    
    public ItemStack getInputStack() {
        return inventory.getStackInSlot(0);
    }
    
    public ItemStack getOutputStack() {
        return inventory.getStackInSlot(1);
    }
    
    public void setInputStack(ItemStack stack) {
        inventory.setStackInSlot(0, stack);
    }
    
    public void setOutputStack(ItemStack stack) {
        inventory.setStackInSlot(1, stack);
    }
    
    public boolean canExtract() {
        ItemStack input = getInputStack();
        return !input.isEmpty() 
            && GemNBTHelper.isIdentified(input) 
            && getOutputStack().isEmpty();
    }
    
    public List<IdentifiedAffix> getAffixes() {
        ItemStack input = getInputStack();
        if (input.isEmpty() || !GemNBTHelper.isIdentified(input)) {
            return null;
        }
        return GemNBTHelper.getAffixes(input);
    }
    
    public boolean extractAffix(int index, EntityPlayer player) {
        if (!canExtract()) return false;
        
        ItemStack input = getInputStack();
        ItemStack refined = GemExtractionHelper.extractAffix(input.copy(), index);
        
        if (refined.isEmpty()) return false;
        
        if (player.experienceLevel < 2 && !player.capabilities.isCreativeMode) {
            return false;
        }
        
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-2);
        }
        
        setOutputStack(refined);
        setInputStack(ItemStack.EMPTY);
        
        isWorking = true;
        workTicks = 0;
        markDirty();
        
        return true;
    }
    
    public boolean decomposeGem(EntityPlayer player) {
        if (!canExtract()) return false;
        
        ItemStack input = getInputStack();
        List<ItemStack> refined = GemExtractionHelper.decomposeGem(input.copy());
        
        if (refined.isEmpty()) return false;
        
        int xpCost = refined.size();
        if (player.experienceLevel < xpCost && !player.capabilities.isCreativeMode) {
            return false;
        }
        
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-xpCost);
        }
        
        setInputStack(ItemStack.EMPTY);
        
        for (ItemStack stack : refined) {
            if (!world.isRemote) {
                net.minecraft.entity.item.EntityItem entityItem = 
                    new net.minecraft.entity.item.EntityItem(world, 
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 
                        stack);
                world.spawnEntity(entityItem);
            }
        }
        
        isWorking = true;
        workTicks = 0;
        markDirty();
        
        return true;
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("inventory", inventory.serializeNBT());
        compound.setInteger("workTicks", workTicks);
        compound.setBoolean("isWorking", isWorking);
        return compound;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("inventory"));
        workTicks = compound.getInteger("workTicks");
        isWorking = compound.getBoolean("isWorking");
    }
    
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY 
            || super.hasCapability(capability, facing);
    }
    
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}