package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import javax.annotation.Nullable;
import java.util.List;

public class ItemEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 500000;
    public static final int COST_PER_BLOCK = 1000;

    public ItemEnergyBarrier() {
        setRegistryName("energy_barrier");
        setTranslationKey("energy_barrier");
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
        tooltip.add(TextFormatting.GREEN + "完全格挡所有类型伤害");
        tooltip.add(TextFormatting.GRAY + "需要外部能量设备充电");
        tooltip.add(TextFormatting.DARK_GRAY + "每次格挡消耗 " + COST_PER_BLOCK + " RF");
    }

    // 移除右键充能功能 - 现在显示详细信息
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            double energyPercent = (double) energy / MAX_ENERGY * 100;

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "=== 能量护盾状态 ==="
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前能量：" + energy + "/" + MAX_ENERGY + " RF (" +
                            String.format("%.1f", energyPercent) + "%)"
            ));

            if (energy < COST_PER_BLOCK) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 能量严重不足！无法提供任何保护！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "需要至少 " + COST_PER_BLOCK + " RF 才能格挡一次攻击"
                ));
            } else {
                int blocksLeft = energy / COST_PER_BLOCK;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 护盾活跃中，可完全格挡约 " + blocksLeft + " 次攻击"
                ));

                // 根据剩余能量给出不同的提醒
                if (energyPercent < 10) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 能量严重不足，建议立即充电！"
                    ));
                } else if (energyPercent < 25) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 能量偏低，建议及时充电"
                    ));
                } else if (energyPercent >= 90) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "✓ 能量充足，护盾处于最佳状态"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "护盾类型：" + TextFormatting.LIGHT_PURPLE + "全能量护盾"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "保护范围：爆炸、火焰、魔法、投射物、近战攻击"
            ));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderEnergyBarrier(stack);
    }

    private static class CapabilityProviderEnergyBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderEnergyBarrier(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = ItemEnergyBarrier.getEnergyStored(stack);
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) ItemEnergyBarrier.setEnergyStored(stack, stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = ItemEnergyBarrier.getEnergyStored(stack);
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) ItemEnergyBarrier.setEnergyStored(stack, stored - extracted);
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return ItemEnergyBarrier.getEnergyStored(stack);
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
        return source.isExplosion() ||
                source.isFireDamage() ||
                source.isMagicDamage() ||
                source.isProjectile() ||
                source.damageType.equals("player") ||
                source.damageType.equals("mob");
    }

    public static boolean handleDamageBlock(LivingAttackEvent event, ItemStack stack) {
        if (!shouldBlockDamage(event.getSource())) return false;

        int energy = getEnergyStored(stack);
        if (energy < COST_PER_BLOCK) return false;

        setEnergyStored(stack, energy - COST_PER_BLOCK);
        event.setCanceled(true);

        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            if (entity instanceof EntityPlayer) {
                ((EntityPlayer) entity).sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GREEN + "[能量护盾] 已屏蔽 " + getDamageTypeName(event.getSource()) +
                                        TextFormatting.YELLOW + " (剩余：" + getEnergyStored(stack) + " RF)"
                        ), true);
            }
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                    entity.getSoundCategory(), 0.4F, 1.5F);
        }
        return true;
    }

    // 获取伤害类型的友好名称
    private static String getDamageTypeName(DamageSource source) {
        if (source.isExplosion()) return "爆炸伤害";
        if (source.isFireDamage()) return "火焰伤害";
        if (source.isMagicDamage()) return "魔法伤害";
        if (source.isProjectile()) return "投射物伤害";
        if (source.damageType.equals("player")) return "玩家攻击";
        if (source.damageType.equals("mob")) return "怪物攻击";
        return source.damageType + "伤害";
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }
}