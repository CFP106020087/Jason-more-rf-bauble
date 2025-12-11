package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.mixin.mixinhelper.CapBypassFlag;
import net.minecraft.enchantment.EnchantmentDurability;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * ⚡ 能量剑（完全版）
 * 支持自动连续攻击、高能出鞘状态、穿透防御与限伤。
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemEnergySword extends ItemSword {

    public static final int ENERGY_PER_HIT = 200;
    public static final int MAX_HITS = 200;
    public static final int NO_IMMUNITY_DURATION = 100;

    private static final Map<UUID, Integer> noImmunityTargets = new HashMap<>();

    public static final ToolMaterial ENERGY_MATERIAL = net.minecraftforge.common.util.EnumHelper.addToolMaterial(
            "ENERGY_SWORD", 6, 2048, 5.0F, 15.0F, 22
    );

    public static final UUID RANGE_MODIFIER_UUID = UUID.nameUUIDFromBytes("ENERGY_SWORD_REACH".getBytes());
    public static final UUID ATTACK_SPEED_UUID = UUID.nameUUIDFromBytes("ENERGY_SWORD_SPEED".getBytes());

    // 自动攻击攻速补正
    // MC冷却公式：冷却tick = 40 / 攻速
    // 3tick冷却需要：攻速 = 40/3 ≈ 13.33 → 加成 = 13.33 - 4.0 = 9.33
    // 使用 10.0 确保 3tick 攻击能满伤
    private static final UUID AUTO_ATTACK_SPEED_UUID = UUID.fromString("d8f3a1b2-c4e5-6f78-9a0b-1c2d3e4f5a6b");
    private static final String AUTO_ATTACK_SPEED_NAME = "Energy Sword Auto Attack Speed";
    private static final float AUTO_ATTACK_SPEED_BONUS = 10.0f;

    private static final String NBT_CAN_UNSHEATHE = "CanUnsheathe";
    private static final String UPGRADE_PREFIX = "upgrade_";

    public ItemEnergySword() {
        super(ENERGY_MATERIAL);
        setRegistryName("energy_sword");
        setTranslationKey("energy_sword");
        setMaxStackSize(1);
        setMaxDamage(0);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    // =========================
    // 基础打击与能量消耗
    // =========================
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
            IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
            boolean powered = storage != null && storage.getEnergyStored() > 0;
            boolean canUnsheathe = powered && (attacker instanceof EntityPlayer) && canUnsheathe((EntityPlayer) attacker, stack);

            // ⭐ 修复：只要有能量就启用i-frame无视（自动攻击模式）
            // canUnsheathe 仅用于额外的穿甲伤害
            if (powered) {
                // 无敌帧无视：允许快速连击
                target.hurtResistantTime = 0;
                noImmunityTargets.put(target.getUniqueID(), NO_IMMUNITY_DURATION);
            }

            // 出鞘状态：额外穿甲伤害
            if (canUnsheathe) {
                CapBypassFlag.ASMODEUS_BYPASS.set(true);
                try {
                    float extraDamage = 20.0F;
                    int sharp = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
                    if (sharp > 0) extraDamage += sharp * 1.25F;

                    DamageSource src = new EntityDamageSource("moremod_energy", attacker)
                            .setDamageBypassesArmor()
                            .setMagicDamage();
                    target.attackEntityFrom(src, extraDamage);
                } finally {
                    if (target.world instanceof WorldServer) {
                        ((WorldServer)target.world).addScheduledTask(CapBypassFlag.ASMODEUS_BYPASS::remove);
                    } else {
                        CapBypassFlag.ASMODEUS_BYPASS.remove();
                    }
                }
            }
        }
        return true;
    }

    // =========================
    // 世界Tick - 清除无敌时间
    // =========================
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        // ⭐ 关键修复：使用 START 阶段，确保在数据包处理之前清除i-frame
        // 这样当客户端的自动攻击包到达时，目标的hurtResistantTime已经被清零
        if (event.phase != TickEvent.Phase.START || event.world.isRemote) return;
        Iterator<Map.Entry<UUID, Integer>> it = noImmunityTargets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            UUID id = e.getKey();
            int remain = e.getValue();
            for (Entity en : event.world.loadedEntityList) {
                if (en instanceof EntityLivingBase && en.getUniqueID().equals(id)) {
                    ((EntityLivingBase) en).hurtResistantTime = 0;
                    break;
                }
            }
            if (remain <= 1) it.remove();
            else e.setValue(remain - 1);
        }
    }

    // =========================
    // 玩家Tick - 更新出鞘状态 + 攻速补正
    // =========================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return; // 服务端处理属性

        EntityPlayer p = event.player;
        ItemStack mh = p.getHeldItemMainhand();

        // 获取攻速属性实例
        IAttributeInstance attackSpeedAttr = p.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeedAttr == null) return;

        // 检查是否手持能量剑
        if (!(mh.getItem() instanceof ItemEnergySword)) {
            // 没有持有能量剑，移除攻速加成
            removeAutoAttackSpeedModifier(attackSpeedAttr);
            return;
        }

        boolean allow = canUnsheathe(p, mh);
        NBTTagCompound tag = mh.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            mh.setTagCompound(tag);
        }
        tag.setBoolean(NBT_CAN_UNSHEATHE, allow);

        // 检查能量
        IEnergyStorage st = mh.getCapability(CapabilityEnergy.ENERGY, null);
        boolean powered = st != null && st.getEnergyStored() > 0;

        // 有能量时就添加攻速加成（自动攻击不依赖出鞘条件）
        if (powered) {
            applyAutoAttackSpeedModifier(attackSpeedAttr);
        } else {
            removeAutoAttackSpeedModifier(attackSpeedAttr);
        }
    }

    /**
     * 应用自动攻击攻速修饰符
     */
    private static void applyAutoAttackSpeedModifier(IAttributeInstance attackSpeed) {
        AttributeModifier existing = attackSpeed.getModifier(AUTO_ATTACK_SPEED_UUID);
        if (existing == null) {
            AttributeModifier modifier = new AttributeModifier(
                    AUTO_ATTACK_SPEED_UUID,
                    AUTO_ATTACK_SPEED_NAME,
                    AUTO_ATTACK_SPEED_BONUS,
                    0  // 加法运算
            );
            attackSpeed.applyModifier(modifier);
        }
    }

    /**
     * 移除自动攻击攻速修饰符
     */
    private static void removeAutoAttackSpeedModifier(IAttributeInstance attackSpeed) {
        AttributeModifier existing = attackSpeed.getModifier(AUTO_ATTACK_SPEED_UUID);
        if (existing != null) {
            attackSpeed.removeModifier(existing);
        }
    }

    // =========================
    // 能量显示
    // =========================
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null
                ? 1.0 - ((double) storage.getEnergyStored() / Math.max(1, storage.getMaxEnergyStored()))
                : 1.0;
    }

    /**
     * 添加Tooltip信息（通过ClientProxy调用客户端代码）
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        try {
            Class<?> clientUtils = Class.forName("com.moremod.client.EnergySwordClientUtils");
            clientUtils.getMethod("addInformation", ItemStack.class, World.class, List.class, ITooltipFlag.class)
                    .invoke(null, stack, world, tip, flag);
        } catch (Exception e) {
            // 静默失败，服务端不需要tooltip
        }
    }

    // =========================
    // 能量存储实现
    // =========================
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyStorageImpl(stack, ENERGY_PER_HIT * MAX_HITS, ENERGY_PER_HIT * 10, 0);
    }

    public static class EnergyStorageImpl implements IEnergyStorage, ICapabilityProvider {
        private final ItemStack stack;
        private final int capacity, maxReceive, maxExtract;

        public EnergyStorageImpl(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
        }

        @Override
        public int receiveEnergy(int mr, boolean sim) {
            if (!canReceive()) return 0;
            int stored = getEnergyStored();
            int recv = Math.min(capacity - stored, Math.min(this.maxReceive, mr));
            if (!sim) setEnergyStored(stored + recv);
            return recv;
        }

        @Override
        public int extractEnergy(int me, boolean sim) {
            if (!canExtract()) return 0;
            int stored = getEnergyStored();
            int ext = Math.min(stored, Math.min(this.maxExtract, me));
            if (!sim) setEnergyStored(stored - ext);
            return ext;
        }

        @Override
        public int getEnergyStored() {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null ? tag.getInteger("Energy") : 0;
        }

        public void setEnergyStored(int e) {
            stack.setTagInfo("Energy", new NBTTagInt(e));
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return maxExtract > 0;
        }

        @Override
        public boolean canReceive() {
            return maxReceive > 0;
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing face) {
            return cap == CapabilityEnergy.ENERGY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing face) {
            return cap == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this) : null;
        }
    }

    // =========================
    // 辅助方法
    // =========================
    public static boolean canUnsheathe(EntityPlayer player, ItemStack swordStack) {
        if (player == null) return false;
        IEnergyStorage st = swordStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (st == null || st.getEnergyStored() <= 0) return false;
        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (core.isEmpty() || !ItemMechanicalCore.isMechanicalCore(core)) return false;
        return countActiveModulesOnCore(core) >= 30;
    }

    public static int countActiveModulesOnCore(ItemStack core) {
        if (core == null || core.isEmpty()) return 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;
        int sum = 0;
        for (String k : nbt.getKeySet()) {
            if (!k.startsWith(UPGRADE_PREFIX)) continue;
            int lv = nbt.getInteger(k);
            if (lv <= 0) continue;
            if (nbt.getBoolean("IsPaused_" + k)) continue;
            sum += lv;
        }
        return sum;
    }
}