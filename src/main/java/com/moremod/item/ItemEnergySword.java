package com.yourmod.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentDurability;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class ItemEnergySword extends ItemSword {

    public static final int ENERGY_PER_HIT = 200;
    public static final int MAX_HITS = 200;

    public static final ToolMaterial ENERGY_MATERIAL = EnumHelper.addToolMaterial(
            "ENERGY_SWORD", 6, 2048, 5.0F, 7.0F, 15);

    public static final UUID RANGE_MODIFIER_UUID = UUID.nameUUIDFromBytes("ENERGY_SWORD_REACH".getBytes());

    public ItemEnergySword() {
        super(ENERGY_MATERIAL);
        setRegistryName("energy_sword");
        setTranslationKey("energy_sword");
        setMaxStackSize(1);
        setMaxDamage(0); // 不使用原版耐久
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        if (!EnchantmentDurability.negateDamage(stack, unbreaking, attacker.getRNG())) {
            IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage instanceof EnergyStorageImpl) {
                ((EnergyStorageImpl) storage).setEnergyStored(Math.max(0, storage.getEnergyStored() - ENERGY_PER_HIT));
            }
        }

        if (!target.world.isRemote) {
            target.hurtResistantTime = 0;
            target.attackEntityFrom(DamageSource.OUT_OF_WORLD, 10.0F);
        }

        return true;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null ? 1.0 - ((double) storage.getEnergyStored() / storage.getMaxEnergyStored()) : 1.0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (storage != null) {
            tooltip.add(TextFormatting.GRAY + "能量: " + storage.getEnergyStored() + " / " + storage.getMaxEnergyStored() + " RF");
            if (storage.getEnergyStored() > 0) {
                tooltip.add(TextFormatting.RED + "高能状态：快速攻击、虚空打击");
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "能量耗尽：削弱伤害与速度");
            }
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyStorageImpl(stack, ENERGY_PER_HIT * MAX_HITS, ENERGY_PER_HIT, 0);
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(@Nonnull EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers;
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        boolean powered = storage != null && storage.getEnergyStored() > 0;

        if (slot != EntityEquipmentSlot.MAINHAND) return HashMultimap.create();
        if (powered) {
            modifiers = super.getItemAttributeModifiers(slot);
        } else {
            modifiers = HashMultimap.create(); // 无能量时取消属性
        }

        modifiers.put(EntityPlayer.REACH_DISTANCE.getName(),
                new AttributeModifier(RANGE_MODIFIER_UUID, "Extra reach", powered ? 1.5 : 0, 0));

        return modifiers;
    }

    // ========== 三状态模型控制 ==========
    @SideOnly(Side.CLIENT)
    public static int getSwordState(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (storage == null) return 0;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !player.getHeldItemMainhand().equals(stack)) {
            return 0; // 未装备 → 刀鞘
        }

        int energy = storage.getEnergyStored();
        int max = storage.getMaxEnergyStored();

        if (energy == 0) return 1; // 装备 + 无能量 → 刀带刀鞘
        return 2;                 // 装备 + 有能量 → 出刀
    }

    // ========== 能量存储类 ==========
    public static class EnergyStorageImpl implements IEnergyStorage, ICapabilityProvider {
        private final ItemStack stack;
        private final int capacity;
        private final int maxReceive;
        private final int maxExtract;

        public EnergyStorageImpl(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) return 0;
            int stored = getEnergyStored();
            int received = Math.min(capacity - stored, Math.min(this.maxReceive, maxReceive));
            if (!simulate) setEnergyStored(stored + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract()) return 0;
            int stored = getEnergyStored();
            int extracted = Math.min(stored, Math.min(this.maxExtract, maxExtract));
            if (!simulate) setEnergyStored(stored - extracted);
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null ? tag.getInteger("Energy") : 0;
        }

        public void setEnergyStored(int energy) {
            stack.setTagInfo("Energy", new NBTTagInt(energy));
        }

        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract() { return maxExtract > 0; }
        @Override public boolean canReceive() { return maxReceive > 0; }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this) : null;
        }
    }

    // ========== 可选：自动挥动（装饰效果） ==========
    private static boolean swingLeft = true;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null) return;

        if (mc.gameSettings.keyBindAttack.isKeyDown()) {
            ItemStack held = mc.player.getHeldItemMainhand();
            if (held.getItem() instanceof ItemEnergySword) {
                Entity target = getMouseOverEntity();
                if (target != null) {
                    mc.playerController.attackEntity(mc.player, target);
                }

                mc.player.swingArm(swingLeft ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
                swingLeft = !swingLeft;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static Entity getMouseOverEntity() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null)
                ? mc.objectMouseOver.entityHit
                : null;
    }
}
