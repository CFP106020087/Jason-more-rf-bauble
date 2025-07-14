package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBasicEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 100000;
    public static final int COST_PER_BLOCK = 1000;

    public ItemBasicEnergyBarrier() {
        setRegistryName("basic_energy_barrier");
        setTranslationKey("basic_energy_barrier");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BODY;
    }

    public static int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getInteger("Energy") : 0;
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Energy", Math.max(0, Math.min(MAX_ENERGY, amount)));
    }

    public boolean consumeEnergy(ItemStack stack, int amount) {
        int stored = getEnergyStored(stack);
        if (stored >= amount) {
            setEnergyStored(stack, stored - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - ((double) getEnergyStored(stack) / MAX_ENERGY);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);
        tooltip.add(TextFormatting.YELLOW + "能量：" + energy + " / " + MAX_ENERGY + " RF");
        tooltip.add(TextFormatting.BLUE + "100% 格挡近战攻击");
        tooltip.add(TextFormatting.GRAY + "需要外部能量设备充电");
        tooltip.add(TextFormatting.DARK_GRAY + "每次格挡消耗 " + COST_PER_BLOCK + " RF");
    }

    // 移除右键充能功能 - 现在显示护盾状态信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;

            player.sendMessage(new TextComponentString(
                    TextFormatting.BLUE + "=== 基础能量护盾状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + energy + "/" + MAX_ENERGY + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            if (energy < COST_PER_BLOCK) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量不足！无法提供保护！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "需要至少 " + COST_PER_BLOCK + " RF 才能格挡一次攻击"
                ));
            } else {
                int blocksLeft = energy / COST_PER_BLOCK;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 护盾活跃中，可格挡约 " + blocksLeft + " 次攻击"
                ));

                // 根据剩余能量给出提醒
                if (energyPercent < 15) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 能量严重不足，建议立即充电！"
                    ));
                } else if (energyPercent < 30) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                    ));
                } else if (energyPercent >= 85) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "✓ 能量充足，护盾运行良好"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "护盾类型：" + TextFormatting.AQUA + "基础近战护盾"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "保护范围：仅近战攻击（100% 格挡概率）"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "注意：无法防护投射物、魔法、爆炸等远程伤害"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderBasicEnergyBarrier(stack);
    }

    private static class CapabilityProviderBasicEnergyBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderBasicEnergyBarrier(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) setEnergyStored(stack, stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) setEnergyStored(stack, stored - extracted);
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return ItemBasicEnergyBarrier.getEnergyStored(stack);
                }

                @Override
                public int getMaxEnergyStored() {
                    return MAX_ENERGY;
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? (T) wrapper : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return new NBTTagCompound();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {}
    }

    public static boolean shouldBlockDamage(DamageSource source) {
        // 只格挡近战攻击
        return isMeleeDamage(source);
    }

    public static boolean isMeleeDamage(DamageSource source) {
        return source.getImmediateSource() instanceof net.minecraft.entity.Entity &&
                !source.isProjectile() &&
                !source.isMagicDamage() &&
                !source.isExplosion() &&
                !source.isFireDamage();
    }

    // 获取伤害类型的友好名称
    public static String getDamageTypeName(DamageSource source) {
        if (isMeleeDamage(source)) {
            if (source.getTrueSource() instanceof EntityPlayer) return "玩家近战攻击";
            if (source.getTrueSource() instanceof net.minecraft.entity.monster.IMob) return "怪物近战攻击";
            return "近战攻击";
        }
        return source.damageType + "伤害";
    }

    // 处理伤害格挡的方法（需要在事件处理器中调用）
    public static boolean handleDamageBlock(net.minecraftforge.event.entity.living.LivingAttackEvent event, ItemStack stack) {
        if (!shouldBlockDamage(event.getSource())) return false;

        int energy = getEnergyStored(stack);
        if (energy < COST_PER_BLOCK) return false;

        // 100% 格挡近战攻击 - 移除概率检测
        // if (event.getEntityLiving().getRNG().nextFloat() > 0.75f) return false;

        setEnergyStored(stack, energy - COST_PER_BLOCK);
        event.setCanceled(true);

        net.minecraft.entity.EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            if (entity instanceof EntityPlayer) {
                ((EntityPlayer) entity).sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.BLUE + "[基础护盾] 已格挡 " + getDamageTypeName(event.getSource()) +
                                        TextFormatting.YELLOW + " (剩余：" + getEnergyStored(stack) + " RF)"
                        ), true);
            }
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                    entity.getSoundCategory(), 0.5F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }
}