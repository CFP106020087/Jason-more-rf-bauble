package com.moremod.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.mixinhelper.CapBypassFlag;
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
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber
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

    private static final String NBT_CAN_UNSHEATHE = "CanUnsheathe";
    private static final String UPGRADE_PREFIX = "upgrade_";

    // 自动攻击相关变量
    private static int attackTicker = 0;
    private static boolean wasAttacking = false;

    public ItemEnergySword() {
        super(ENERGY_MATERIAL);
        setRegistryName("energy_sword");
        setTranslationKey("energy_sword");
        setMaxStackSize(1);
        setMaxDamage(0);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        // 能量消耗
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

            if (canUnsheathe) {
                // 高能出鞘状态

                // 重要：在造成任何伤害之前设置标记
                CapBypassFlag.ASMODEUS_BYPASS.set(true);

                try {
                    // 清除无敌帧
                    target.hurtResistantTime = 0;
                    noImmunityTargets.put(target.getUniqueID(), NO_IMMUNITY_DURATION);

                    // 计算额外伤害
                    float extraDamage = 20.0F;

                    int sharp = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
                    if (sharp > 0) extraDamage += sharp * 1.25F;

                    if (target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) {
                        int smite = EnchantmentHelper.getEnchantmentLevel(Enchantments.SMITE, stack);
                        if (smite > 0) extraDamage += smite * 2.5F;
                    } else if (target.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD) {
                        int bane = EnchantmentHelper.getEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, stack);
                        if (bane > 0) extraDamage += bane * 2.5F;
                    }

                    // 造成额外能量伤害
                    DamageSource energySrc = new EntityDamageSource("moremod_energy", attacker)
                            .setDamageBypassesArmor()
                            .setMagicDamage();

                    // 在攻击之前再次确保标记被设置
                    CapBypassFlag.ASMODEUS_BYPASS.set(true);
                    target.attackEntityFrom(energySrc, extraDamage);

                    // 附加效果
                    int fire = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
                    if (fire > 0) target.setFire(fire * 4);

                    int kb = EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, stack);
                    if (kb > 0 && attacker instanceof EntityPlayer) {
                        target.knockBack(attacker, kb * 0.5F,
                                Math.sin(attacker.rotationYaw * 0.017453292F),
                                -Math.cos(attacker.rotationYaw * 0.017453292F));
                    }
                } finally {
                    // 延迟清除标记，让整个伤害处理流程都能使用到这个标记
                    if (target.world instanceof WorldServer) {
                        ((WorldServer)target.world).addScheduledTask(() -> {
                            CapBypassFlag.ASMODEUS_BYPASS.remove();
                        });
                    } else {
                        CapBypassFlag.ASMODEUS_BYPASS.remove();
                    }
                }
            }
        }

        return true;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntityPlayer() == null || event.getTarget() == null) return;

        ItemStack weapon = event.getEntityPlayer().getHeldItemMainhand();
        if (weapon.getItem() instanceof ItemEnergySword) {
            IEnergyStorage storage = weapon.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage != null && storage.getEnergyStored() > 0) {
                NBTTagCompound nbt = weapon.getTagCompound();
                if (nbt != null && nbt.getBoolean(NBT_CAN_UNSHEATHE)) {
                    // 在攻击事件触发时就设置标记
                    CapBypassFlag.ASMODEUS_BYPASS.set(true);

                    // 确保在攻击处理完成后清除
                    if (event.getEntityPlayer().world instanceof WorldServer) {
                        ((WorldServer)event.getEntityPlayer().world).addScheduledTask(() -> {
                            CapBypassFlag.ASMODEUS_BYPASS.remove();
                        });
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

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

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        IEnergyStorage st = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (st != null) {
            tip.add(TextFormatting.GRAY + "能量: " + st.getEnergyStored() + " / " + st.getMaxEnergyStored() + " RF");
            if (st.getEnergyStored() > 0) {
                boolean allow = false;
                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt != null) allow = nbt.getBoolean(NBT_CAN_UNSHEATHE);

                if (allow) {
                    tip.add(TextFormatting.RED + "高能状态：极速攻击、移除无敌帧");
                    tip.add(TextFormatting.GOLD + "基础伤害: 15 + 20 能量伤害");
                    tip.add(TextFormatting.AQUA + "攻击速度: 5.5（极快）");
                    tip.add(TextFormatting.YELLOW + "特殊效果: 目标5秒内无无敌帧");
                    tip.add(TextFormatting.LIGHT_PURPLE + "穿透护盾与限伤机制");
                } else {
                    tip.add(TextFormatting.YELLOW + "✗ 条件不足：需装备机械核心且运行等级总和 ≥ 30");
                    tip.add(TextFormatting.DARK_GRAY + "（示例：升级 Lv3 计 3；暂停的不计入）");
                }
            } else {
                tip.add(TextFormatting.DARK_GRAY + "能量耗尽：失去所有特殊效果");
            }
            if (flag.isAdvanced()) {
                tip.add(TextFormatting.DARK_PURPLE + "支持所有剑类附魔（含多数模组）");
            }
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyStorageImpl(stack, ENERGY_PER_HIT * MAX_HITS, ENERGY_PER_HIT * 10, 0);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot != EntityEquipmentSlot.MAINHAND) return map;

        boolean allow = false;

        try {
            if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.player != null) {
                    ItemStack clientMain = mc.player.getHeldItemMainhand();
                    NBTTagCompound nbt = clientMain != null ? clientMain.getTagCompound() : null;
                    if (nbt != null) allow = nbt.getBoolean(NBT_CAN_UNSHEATHE);
                }
            }
        } catch (Throwable ignored) {}

        if (allow) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 15.0, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_UUID, "Energy sword speed", 5.5, 0));
            map.put(EntityPlayer.REACH_DISTANCE.getName(),
                    new AttributeModifier(RANGE_MODIFIER_UUID, "Extra reach", 2.0, 0));
        } else {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 3.0, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_UUID, "Weapon modifier", -2.4, 0));
        }
        return map;
    }

    @Override
    public int getItemEnchantability() {
        return 22;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.enchantment.Enchantment ench) {
        if (super.canApplyAtEnchantingTable(stack, ench)) return true;
        if (ench.type == net.minecraft.enchantment.EnumEnchantmentType.WEAPON ||
                ench.type == net.minecraft.enchantment.EnumEnchantmentType.ALL) return true;

        try {
            Class<?> enumListClass = Class.forName("com.Shultrea.Rin.Enum.EnumList");
            if (checkSMEType(ench.type, enumListClass, "COMBAT_WEAPON") ||
                    checkSMEType(ench.type, enumListClass, "SWORD") ||
                    checkSMEType(ench.type, enumListClass, "COMBAT") ||
                    checkSMEType(ench.type, enumListClass, "ALL_TOOL") ||
                    checkSMEType(ench.type, enumListClass, "COMBAT_AXE")) return true;
        } catch (ClassNotFoundException ignored) {}

        ItemStack vanilla = new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
        if (ench.canApply(vanilla)) return true;

        String n = ench.getName().toLowerCase(Locale.ROOT);
        return n.contains("speed") || n.contains("haste") || n.contains("swift") || n.contains("quick")
                || n.contains("attack") || n.contains("damage") || n.contains("combat") || n.contains("blade");
    }

    private boolean checkSMEType(net.minecraft.enchantment.EnumEnchantmentType type, Class<?> list, String field) {
        try {
            Object sme = list.getField(field).get(null);
            return type == sme;
        } catch (Exception e) {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public static float getSwordState(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (storage == null) return 0.0f;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !player.getHeldItemMainhand().equals(stack)) {
            return 0.0f;
        }
        NBTTagCompound tag = stack.getTagCompound();
        boolean allow = tag != null && tag.getBoolean(NBT_CAN_UNSHEATHE);
        return allow ? 2.0f : 1.0f;
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

        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract() { return maxExtract > 0; }
        @Override public boolean canReceive() { return maxReceive > 0; }

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

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null) return;

        boolean isAttacking = mc.gameSettings.keyBindAttack.isKeyDown();

        if (isAttacking) {
            ItemStack main = mc.player.getHeldItemMainhand();
            if (main.getItem() instanceof ItemEnergySword) {
                IEnergyStorage st = main.getCapability(CapabilityEnergy.ENERGY, null);
                boolean powered = st != null && st.getEnergyStored() > 0;
                boolean allow = powered && canUnsheathe(mc.player, main);

                if (allow) {
                    boolean justStarted = !wasAttacking;  // 检测是否刚开始攻击
                    attackTicker++;

                    Entity target = getMouseOverEntity();
                    if (target != null) {
                        // 立即攻击（第一次）或按频率攻击
                        if (justStarted || attackTicker % 2 == 0) {
                            mc.playerController.attackEntity(mc.player, target);
                            mc.player.swingArm(EnumHand.MAIN_HAND);
                        }
                    } else {
                        // 空挥
                        if (justStarted || attackTicker % 2 == 0) {
                            mc.player.swingArm(EnumHand.MAIN_HAND);
                        }
                    }

                    // 重置攻击冷却
                    mc.player.resetCooldown();
                }
            }
        } else {
            attackTicker = 0;
        }

        wasAttacking = isAttacking;  // 更新攻击状态
    }

    @SideOnly(Side.CLIENT)
    private static Entity getMouseOverEntity() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null)
                ? mc.objectMouseOver.entityHit
                : null;
    }

    private static String canonicalId(String id) {
        if (id == null) return "";
        String s = id.toLowerCase(Locale.ROOT);
        return s.contains("waterproof") ? "waterproof_module" : s;
    }

    private static int countActiveModulesOnCore(ItemStack core) {
        if (core == null || core.isEmpty()) return 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        Map<String, Integer> levelById = new HashMap<>();

        for (String k : nbt.getKeySet()) {
            if (!k.startsWith(UPGRADE_PREFIX)) continue;
            int lv = nbt.getInteger(k);
            if (lv <= 0) continue;

            String raw = k.substring(UPGRADE_PREFIX.length());
            String id = canonicalId(raw);

            if (nbt.getBoolean("IsPaused_" + id) ||
                    nbt.getBoolean("IsPaused_" + id.toUpperCase(Locale.ROOT)) ||
                    nbt.getBoolean("IsPaused_" + id.toLowerCase(Locale.ROOT))) {
                continue;
            }
            levelById.merge(id, lv, Math::max);
        }

        int sum = 0;
        for (int v : levelById.values()) sum += v;
        return sum;
    }

    private static boolean canUnsheathe(EntityPlayer player, ItemStack swordStack) {
        if (player == null) return false;

        IEnergyStorage st = swordStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (st == null || st.getEnergyStored() <= 0) return false;

        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (core.isEmpty() || !ItemMechanicalCore.isMechanicalCore(core)) return false;

        return countActiveModulesOnCore(core) >= 30;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer p = event.player;
        if (p == null) return;

        ItemStack mh = p.getHeldItemMainhand();
        if (!(mh.getItem() instanceof ItemEnergySword)) return;

        boolean allow = canUnsheathe(p, mh);
        NBTTagCompound tag = mh.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            mh.setTagCompound(tag);
        }
        tag.setBoolean(NBT_CAN_UNSHEATHE, allow);
    }
}