package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.moremod.network.PacketHandler;
import com.moremod.network.PacketUpdateVillagerTransformer;
import com.moremod.upgrades.EnergyEfficiencyManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class VillagerProfessionTool extends Item {

    // ===== 配置常量 =====
    private static int MAX_ENERGY;
    private static int ENERGY_PER_USE;
    private static int MAX_TRANSFORM_ATTEMPTS;
    private static double FAILURE_CHANCE;

    static {
        com.moremod.config.ItemConfig.ensureLoaded();
        MAX_ENERGY = com.moremod.config.ItemConfig.VillagerTransformer.maxEnergy;
        ENERGY_PER_USE = com.moremod.config.ItemConfig.VillagerTransformer.energyPerUse;
        MAX_TRANSFORM_ATTEMPTS = com.moremod.config.ItemConfig.VillagerTransformer.maxTransformAttempts;
        FAILURE_CHANCE = com.moremod.config.ItemConfig.VillagerTransformer.failureChance;
    }

    // 职业映射表
    private static final Map<String, ProfessionCareerPair> PROFESSION_MAP = new LinkedHashMap<>();

    // 实体黑名单
    private static final Set<String> ENTITY_BLACKLIST = new HashSet<>(Arrays.asList(
            "iceandfire:snowvillager",
            "primitivemobs:sheepman",
            "rats:plague_doctor",
            "toroquest:toroquest_toro_villager"
    ));

    // 村民信息类
    public static class VillagerInfo {
        public final String identifier;
        public final VillagerProfession profession;
        public final VillagerCareer career;
        public final int careerId;

        public VillagerInfo(String identifier, VillagerProfession profession, VillagerCareer career, int careerId) {
            this.identifier = identifier;
            this.profession = profession;
            this.career = career;
            this.careerId = careerId;
        }
    }

    // 存储所有可用的村民职业组合
    private static final List<VillagerInfo> ALL_VILLAGER_INFOS = new ArrayList<>();

    // 职业图标映射
    private static final Map<String, ItemStack> PROFESSION_ICONS = new HashMap<>();

    // 职业名称汉化映射
    private static final Map<String, String> PROFESSION_NAMES_CN = new HashMap<>();

    static {
        // 初始化职业映射
        PROFESSION_MAP.put("farmer", new ProfessionCareerPair("minecraft:farmer", "farmer"));
        PROFESSION_MAP.put("fisherman", new ProfessionCareerPair("minecraft:farmer", "fisherman"));
        PROFESSION_MAP.put("shepherd", new ProfessionCareerPair("minecraft:farmer", "shepherd"));
        PROFESSION_MAP.put("fletcher", new ProfessionCareerPair("minecraft:farmer", "fletcher"));
        PROFESSION_MAP.put("librarian", new ProfessionCareerPair("minecraft:librarian", "librarian"));
        PROFESSION_MAP.put("cartographer", new ProfessionCareerPair("minecraft:librarian", "cartographer"));
        PROFESSION_MAP.put("cleric", new ProfessionCareerPair("minecraft:priest", "cleric"));
        PROFESSION_MAP.put("armorer", new ProfessionCareerPair("minecraft:smith", "armor"));
        PROFESSION_MAP.put("weaponsmith", new ProfessionCareerPair("minecraft:smith", "weapon"));
        PROFESSION_MAP.put("toolsmith", new ProfessionCareerPair("minecraft:smith", "tool"));
        PROFESSION_MAP.put("butcher", new ProfessionCareerPair("minecraft:butcher", "butcher"));
        PROFESSION_MAP.put("leatherworker", new ProfessionCareerPair("minecraft:butcher", "leather"));
        PROFESSION_MAP.put("nitwit", new ProfessionCareerPair("minecraft:nitwit", "nitwit"));

        // 初始化职业名称汉化
        PROFESSION_NAMES_CN.put("farmer", "农民");
        PROFESSION_NAMES_CN.put("fisherman", "渔夫");
        PROFESSION_NAMES_CN.put("shepherd", "牧羊人");
        PROFESSION_NAMES_CN.put("fletcher", "制箭师");
        PROFESSION_NAMES_CN.put("librarian", "图书管理员");
        PROFESSION_NAMES_CN.put("cartographer", "制图师");
        PROFESSION_NAMES_CN.put("cleric", "牧师");
        PROFESSION_NAMES_CN.put("armorer", "盔甲匠");
        PROFESSION_NAMES_CN.put("weaponsmith", "武器匠");
        PROFESSION_NAMES_CN.put("toolsmith", "工具匠");
        PROFESSION_NAMES_CN.put("butcher", "屠夫");
        PROFESSION_NAMES_CN.put("leatherworker", "皮革工");
        PROFESSION_NAMES_CN.put("nitwit", "傻子");

        // 初始化所有职业和Career组合
        initializeVillagerInfos();
        initializeProfessionIcons();
    }

    private static void initializeVillagerInfos() {
        for (VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS.getValuesCollection()) {
            List<VillagerCareer> careers = getProfessionCareers(profession);

            if (careers != null && !careers.isEmpty()) {
                for (VillagerCareer career : careers) {
                    int careerId = getCareerId(career);
                    String identifier = getProfessionName(profession) + ":" + career.getName();
                    ALL_VILLAGER_INFOS.add(new VillagerInfo(identifier, profession, career, careerId));
                }
            } else {
                String identifier = getProfessionName(profession);
                ALL_VILLAGER_INFOS.add(new VillagerInfo(identifier, profession, null, 0));
            }
        }
    }

    private static void initializeProfessionIcons() {
        PROFESSION_ICONS.put("minecraft:farmer", new ItemStack(Items.WHEAT));
        PROFESSION_ICONS.put("minecraft:librarian", new ItemStack(Items.BOOK));
        PROFESSION_ICONS.put("minecraft:priest", new ItemStack(Items.GOLDEN_APPLE));
        PROFESSION_ICONS.put("minecraft:smith", new ItemStack(Items.IRON_INGOT));
        PROFESSION_ICONS.put("minecraft:butcher", new ItemStack(Items.PORKCHOP));
        PROFESSION_ICONS.put("minecraft:nitwit", new ItemStack(Items.EMERALD));
    }

    public VillagerProfessionTool() {
        super();
        this.setMaxStackSize(1);
        this.setRegistryName("moremod", "villager_energy_transformer");
        this.setTranslationKey("moremod.villager_energy_transformer");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        this.setMaxDamage(0);
    }

    // ===== 能量系统 =====
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilitySerializable<NBTTagCompound>() {
            private final IEnergyStorage energyStorage = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (!canReceive()) return 0;

                    int energy = getEnergyStored();
                    int energyReceived = Math.min(MAX_ENERGY - energy, maxReceive);

                    if (!simulate && energyReceived > 0) {
                        setEnergy(energy + energyReceived);
                    }

                    return energyReceived;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (!canExtract()) return 0;

                    int energy = getEnergyStored();
                    int energyExtracted = Math.min(energy, maxExtract);

                    if (!simulate && energyExtracted > 0) {
                        setEnergy(energy - energyExtracted);
                    }

                    return energyExtracted;
                }

                @Override
                public int getEnergyStored() {
                    if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Energy")) {
                        return stack.getTagCompound().getInteger("Energy");
                    }
                    return 0;
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

                private void setEnergy(int energy) {
                    if (!stack.hasTagCompound()) {
                        stack.setTagCompound(new NBTTagCompound());
                    }
                    stack.getTagCompound().setInteger("Energy", energy);
                }
            };

            @Override
            public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                if (capability == CapabilityEnergy.ENERGY) {
                    return CapabilityEnergy.ENERGY.cast(energyStorage);
                }
                return null;
            }

            @Override
            public NBTTagCompound serializeNBT() {
                return new NBTTagCompound();
            }

            @Override
            public void deserializeNBT(NBTTagCompound nbt) {
                // 能量已经存储在物品的NBT中
            }
        };
    }

    // ===== 检查是否是傻子 =====
    private boolean isNitwit(EntityVillager villager) {
        VillagerProfession profession = villager.getProfessionForge();
        if (profession == null) return false;

        String professionName = profession.getRegistryName().toString();
        return professionName.equals("minecraft:nitwit");
    }

    // ===== 村民交互 =====
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (!(target instanceof EntityVillager)) {
            return false;
        }

        EntityVillager villager = (EntityVillager) target;

        if (villager.isChild()) {
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "无法转换儿童村民！"));
            }
            return true;
        }

        // 检查是否是傻子
        if (isNitwit(villager)) {
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "傻子村民无法转换职业！"));
                villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
            return true;
        }

        // 检查黑名单
        String entityClassName = villager.getClass().getName();
        boolean isBlacklisted = false;

        for (String blacklisted : ENTITY_BLACKLIST) {
            if (entityClassName.contains(blacklisted.replace(":", ".")) ||
                    (villager.getClass().getSimpleName().toLowerCase().contains(blacklisted.split(":")[1]))) {
                isBlacklisted = true;
                break;
            }
        }

        if (isBlacklisted) {
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "这种类型的村民无法转换！"));
            }
            return true;
        }

        // 计算实际能量消耗
        int actualCost = EnergyEfficiencyManager.calculateActualCost(player, ENERGY_PER_USE);

        // 检查能量
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.extractEnergy(actualCost, true) < actualCost) {
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！需要 " + actualCost + " FE" +
                                (actualCost < ENERGY_PER_USE ? TextFormatting.GREEN + " (已优化)" : "")
                ));
                villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
            return true;
        }

        if (player.isSneaking()) {
            // Shift+右键村民：使用选定的职业转换
            if (!player.world.isRemote) {
                ItemStack currentItem = player.getHeldItem(hand);
                String selectedProfession = getSelectedProfession(currentItem);

                if (selectedProfession == null || selectedProfession.isEmpty()) {
                    player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "未选择职业！按住Shift右键空气打开选择界面"));
                    return true;
                }

                // 扣除能量
                energy.extractEnergy(actualCost, false);

                // 显示节省提示
                if (actualCost < ENERGY_PER_USE) {
                    EnergyEfficiencyManager.showEfficiencySaving(player, ENERGY_PER_USE, actualCost);
                }

                // 检查是否失败（60%概率）
                Random rand = player.world.rand;
                if (rand.nextDouble() < FAILURE_CHANCE) {
                    // 失败，变成傻子
                    if (transformToNitwit(villager)) {
                        villager.playSound(SoundEvents.ENTITY_VILLAGER_HURT, 1.0F, 0.8F);
                        player.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5F, 0.8F);

                        spawnFailureParticles(player.world, villager);

                        player.sendMessage(new TextComponentString(TextFormatting.RED + "转换失败！村民变成了傻子！"));
                    }
                } else {
                    // 成功转换
                    if (transformVillager(villager, selectedProfession)) {
                        villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
                        player.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5F, 1.0F);

                        spawnTransformParticles(player.world, villager);

                        String profName = PROFESSION_NAMES_CN.getOrDefault(selectedProfession, formatProfessionName(selectedProfession));
                        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "村民已转换为" + profName + "！"));
                    } else {
                        player.sendMessage(new TextComponentString(TextFormatting.RED + "转换村民失败！"));
                        villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                    }
                }
            }
        } else {
            if (!player.world.isRemote) {
                player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "按住Shift右键以转换村民"));
            }
        }

        return true;
    }

    // ===== 转换为傻子 =====
    private boolean transformToNitwit(EntityVillager oldVillager) {
        VillagerRegistry.VillagerProfession nitwitProfession = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(
                new ResourceLocation("minecraft:nitwit")
        );

        if (nitwitProfession == null) return false;

        World world = oldVillager.world;

        // 创建傻子村民
        EntityVillager newVillager = new EntityVillager(world);
        newVillager.setProfession(nitwitProfession);

        // 复制属性
        copyVillagerData(oldVillager, newVillager);

        // 移除旧村民，生成新村民
        oldVillager.setDead();
        world.spawnEntity(newVillager);

        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isSneaking()) {
            // Shift+右键空气：打开GUI（仅客户端）
            if (world.isRemote) {
                openProfessionGui(stack, hand);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void openProfessionGui(ItemStack stack, EnumHand hand) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiVillagerSelector(stack, hand));
    }

    // ===== 村民转换逻辑 =====
    private boolean transformVillager(EntityVillager oldVillager, String professionKey) {
        ProfessionCareerPair pair = PROFESSION_MAP.get(professionKey.toLowerCase());
        if (pair == null) return false;

        VillagerRegistry.VillagerProfession profession = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(
                new ResourceLocation(pair.professionName)
        );

        if (profession == null) return false;

        VillagerRegistry.VillagerCareer career = getCareer(profession, pair.careerName);
        if (career == null) return false;

        World world = oldVillager.world;

        // 尝试创建正确的村民
        EntityVillager newVillager = createVillagerWithCareer(profession, career, world);
        if (newVillager == null) return false;

        // 复制属性
        copyVillagerData(oldVillager, newVillager);

        // 移除旧村民，生成新村民
        oldVillager.setDead();
        world.spawnEntity(newVillager);

        return true;
    }

    private void copyVillagerData(EntityVillager from, EntityVillager to) {
        to.setPositionAndRotation(from.posX, from.posY, from.posZ, from.rotationYaw, from.rotationPitch);
        to.rotationYawHead = from.getRotationYawHead();
        to.setNoAI(from.isAIDisabled());

        if (from.hasCustomName()) {
            to.setCustomNameTag(from.getCustomNameTag());
            to.setAlwaysRenderNameTag(from.getAlwaysRenderNameTag());
        }

        // 复制其他数据
        to.setHealth(from.getHealth());
    }

    private EntityVillager createVillagerWithCareer(VillagerRegistry.VillagerProfession profession,
                                                    VillagerRegistry.VillagerCareer career, World world) {
        int targetCareerId = getCareerId(career) + 1;

        for (int i = 0; i < MAX_TRANSFORM_ATTEMPTS; i++) {
            EntityVillager villager = new EntityVillager(world);
            villager.setProfession(profession);

            try {
                if (BUYING_LIST_FIELD != null) {
                    BUYING_LIST_FIELD.set(villager, null);
                }

                if (POPULATE_BUYING_LIST_METHOD != null) {
                    POPULATE_BUYING_LIST_METHOD.invoke(villager);
                }

                if (CAREER_ID_FIELD != null) {
                    int villagercareerId = CAREER_ID_FIELD.getInt(villager);

                    if (villagercareerId == targetCareerId) {
                        return villager;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            villager.setDead();
        }

        return null;
    }

    // ===== 反射辅助方法 =====
    private static Field CAREER_ID_FIELD;
    private static Field BUYING_LIST_FIELD;
    private static java.lang.reflect.Method POPULATE_BUYING_LIST_METHOD;

    static {
        try {
            CAREER_ID_FIELD = findField(EntityVillager.class, "careerId", "field_175563_bv");
            CAREER_ID_FIELD.setAccessible(true);

            BUYING_LIST_FIELD = findField(EntityVillager.class, "buyingList", "field_70963_i");
            BUYING_LIST_FIELD.setAccessible(true);

            POPULATE_BUYING_LIST_METHOD = findMethod(EntityVillager.class, "populateBuyingList", "func_175554_cu");
            POPULATE_BUYING_LIST_METHOD.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[VillagerEnergyTransformer] Failed to initialize reflection fields:");
            e.printStackTrace();
        }
    }

    private static Field findField(Class<?> clazz, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // 继续尝试下一个名称
            }
        }
        throw new NoSuchFieldException("Could not find field with names: " + Arrays.toString(names));
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String... names) throws NoSuchMethodException {
        for (String name : names) {
            try {
                return clazz.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                // 继续尝试下一个名称
            }
        }
        throw new NoSuchMethodException("Could not find method with names: " + Arrays.toString(names));
    }

    public static String getProfessionName(VillagerProfession profession) {
        try {
            Field nameField = VillagerProfession.class.getDeclaredField("name");
            nameField.setAccessible(true);
            return ((ResourceLocation) nameField.get(profession)).toString();
        } catch (Exception e) {
            return profession.getRegistryName().toString();
        }
    }

    @Nullable
    private static List<VillagerCareer> getProfessionCareers(VillagerProfession profession) {
        try {
            Field careersField = VillagerProfession.class.getDeclaredField("careers");
            careersField.setAccessible(true);
            return (List<VillagerCareer>) careersField.get(profession);
        } catch (Exception e) {
            return null;
        }
    }

    private static VillagerRegistry.VillagerCareer getCareer(VillagerRegistry.VillagerProfession profession, String careerName) {
        try {
            Field careersField = profession.getClass().getDeclaredField("careers");
            careersField.setAccessible(true);
            List<VillagerRegistry.VillagerCareer> careers = (List<VillagerRegistry.VillagerCareer>) careersField.get(profession);

            for (VillagerRegistry.VillagerCareer career : careers) {
                if (career.getName().equalsIgnoreCase(careerName)) {
                    return career;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getCareerId(VillagerCareer career) {
        try {
            Field careerIdField = VillagerCareer.class.getDeclaredField("id");
            careerIdField.setAccessible(true);
            return (int) careerIdField.get(career);
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== NBT 数据管理 =====
    private static String getSelectedProfession(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("SelectedProfession")) {
            return nbt.getString("SelectedProfession");
        }
        // 默认选择第一个
        return new ArrayList<>(PROFESSION_MAP.keySet()).get(0);
    }

    public static void setSelectedProfession(ItemStack stack, String profession) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setString("SelectedProfession", profession);
    }

    // ===== 视觉效果 =====
    private void spawnTransformParticles(World world, EntityVillager villager) {
        Random rand = world.rand;
        for (int i = 0; i < 20; i++) {
            double px = villager.posX + (rand.nextDouble() - 0.5) * 2.0;
            double py = villager.posY + rand.nextDouble() * 2.0;
            double pz = villager.posZ + (rand.nextDouble() - 0.5) * 2.0;
            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, px, py, pz, 0, 0, 0);
        }

        for (int i = 0; i < 10; i++) {
            double px = villager.posX + (rand.nextDouble() - 0.5) * 1.5;
            double py = villager.posY + rand.nextDouble() * 2.0;
            double pz = villager.posZ + (rand.nextDouble() - 0.5) * 1.5;
            world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 0, 0, 0);
        }
    }

    private void spawnFailureParticles(World world, EntityVillager villager) {
        Random rand = world.rand;
        for (int i = 0; i < 30; i++) {
            double px = villager.posX + (rand.nextDouble() - 0.5) * 2.0;
            double py = villager.posY + rand.nextDouble() * 2.0;
            double pz = villager.posZ + (rand.nextDouble() - 0.5) * 2.0;
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 0, 0, 0);
        }

        for (int i = 0; i < 15; i++) {
            double px = villager.posX + (rand.nextDouble() - 0.5) * 1.5;
            double py = villager.posY + rand.nextDouble() * 2.0;
            double pz = villager.posZ + (rand.nextDouble() - 0.5) * 1.5;
            world.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY, px, py, pz, 0, 0.1, 0);
        }
    }

    // ===== 辅助方法 =====
    private static String formatProfessionName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    // ===== 物品信息显示 =====
    @Override
    @SideOnly(Side.CLIENT)  // 添加客户端注解
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        // 能量信息
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            int stored = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();
            int percentage = (int)((stored / (float)max) * 100);

            TextFormatting color = TextFormatting.RED;
            if (percentage > 75) color = TextFormatting.GREEN;
            else if (percentage > 50) color = TextFormatting.YELLOW;
            else if (percentage > 25) color = TextFormatting.GOLD;

            tooltip.add(color + "能量: " + String.format("%,d", stored) + "/" + String.format("%,d", max) + " FE (" + percentage + "%)");
            tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");
        }

        // 选定的职业
        String selected = getSelectedProfession(stack);
        if (selected != null && !selected.isEmpty()) {
            String profName = PROFESSION_NAMES_CN.getOrDefault(selected, formatProfessionName(selected));
            tooltip.add(TextFormatting.GREEN + "当前选择: " + TextFormatting.WHITE + profName);
        }

        // 使用说明
        EntityPlayer player = Minecraft.getMinecraft().player;
        int actualCost = player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, ENERGY_PER_USE) : ENERGY_PER_USE;

        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "使用方法:");
        tooltip.add(TextFormatting.GRAY + "• Shift+右键空气打开选择界面");
        tooltip.add(TextFormatting.GRAY + "• Shift+右键村民进行转换");
        tooltip.add(TextFormatting.DARK_GRAY + "• 每次转换消耗 " + String.format("%,d", actualCost) + " FE" +
                (actualCost < ENERGY_PER_USE ? TextFormatting.GREEN + " (已优化)" : ""));
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "警告: 60%概率失败并变成傻子！");
        tooltip.add(TextFormatting.DARK_RED + "傻子无法再次转换职业！");

        // 详细信息（按Shift）
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "可用职业:");
            for (String prof : PROFESSION_MAP.keySet()) {
                String profName = PROFESSION_NAMES_CN.getOrDefault(prof, formatProfessionName(prof));
                String mark = prof.equals(selected) ? "▶ " : "  ";
                tooltip.add(TextFormatting.GRAY + mark + profName);
            }

            // 充电信息
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "可使用以下设备充电:");
            tooltip.add(TextFormatting.DARK_GRAY + "• RF/FE 充电设备");
            tooltip.add(TextFormatting.DARK_GRAY + "• 能量单元");
            tooltip.add(TextFormatting.DARK_GRAY + "• 电容库");

            // 显示当前效率
            if (player != null && actualCost < ENERGY_PER_USE) {
                int saved = ENERGY_PER_USE - actualCost;
                int percentage = (int)((saved / (float)ENERGY_PER_USE) * 100);
                tooltip.add("");
                tooltip.add(TextFormatting.GREEN + "能量效率: 节省 " + percentage + "% (" + saved + " FE/次)");
            }
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看更多信息>");
        }
    }

    // ===== 耐久度条显示 =====
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            return 1.0 - ((double) energy.getEnergyStored() / energy.getMaxEnergyStored());
        }
        return 1.0;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            float f = (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
            return MathHelper.hsvToRGB(f / 3.0F, 1.0F, 1.0F);
        }
        return super.getRGBDurabilityForDisplay(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    // ===== 创造模式充能 =====
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null) {
                energy.receiveEnergy(MAX_ENERGY, false);
            }
        }
    }

    // ===== 内部类 =====
    private static class ProfessionCareerPair {
        public final String professionName;
        public final String careerName;

        ProfessionCareerPair(String professionName, String careerName) {
            this.professionName = professionName;
            this.careerName = careerName;
        }
    }

    // ===== GUI类（修复版） =====
    @SideOnly(Side.CLIENT)
    public static class GuiVillagerSelector extends GuiScreen {
        private final ItemStack transformerStack;
        private final EnumHand hand;
        private List<String> professionKeys;
        private int scrollOffset = 0;
        private static final int BUTTONS_PER_PAGE = 6;
        private String selectedProfession;
        private String tempSelectedProfession;

        public GuiVillagerSelector(ItemStack stack, EnumHand hand) {
            this.transformerStack = stack;
            this.hand = hand;
            this.selectedProfession = getSelectedProfession(stack);
            this.tempSelectedProfession = this.selectedProfession;
        }

        @Override
        public void initGui() {
            // 获取所有可用的职业键（排除傻子）
            professionKeys = new ArrayList<>();
            for (String key : PROFESSION_MAP.keySet()) {
                if (!key.equals("nitwit")) {  // 排除傻子
                    professionKeys.add(key);
                }
            }
            updateButtons();
        }

        private void updateButtons() {
            buttonList.clear();

            int centerX = width / 2;
            int centerY = height / 2;
            int startY = centerY - 90;  // 基准位置
            int buttonStartY = startY + 35;  // 按钮实际开始位置

            // 职业选择按钮
            for (int i = 0; i < Math.min(BUTTONS_PER_PAGE, professionKeys.size() - scrollOffset); i++) {
                int index = scrollOffset + i;
                if (index < professionKeys.size()) {
                    String profKey = professionKeys.get(index);
                    String displayName = PROFESSION_NAMES_CN.getOrDefault(profKey, formatProfessionName(profKey));

                    // 如果是当前临时选中的职业，添加标记
                    if (profKey.equals(tempSelectedProfession)) {
                        displayName = "▶ " + displayName + " ◀";
                    }

                    GuiButton button = new GuiButton(i, centerX - 100, buttonStartY + i * 22, 200, 20, displayName);
                    button.enabled = true;
                    buttonList.add(button);
                }
            }

            // 翻页按钮
            int pageButtonY = buttonStartY + BUTTONS_PER_PAGE * 22 + 5;
            if (scrollOffset > 0) {
                buttonList.add(new GuiButton(100, centerX - 100, pageButtonY, 95, 20, "◄ 上一页"));
            }
            if (scrollOffset + BUTTONS_PER_PAGE < professionKeys.size()) {
                buttonList.add(new GuiButton(101, centerX + 5, pageButtonY, 95, 20, "下一页 ►"));
            }

            // 确认和取消按钮（增加间距避免重叠）
            int confirmButtonY = pageButtonY + 45;  // 增加间距
            buttonList.add(new GuiButton(102, centerX - 100, confirmButtonY, 95, 20, "确认选择"));
            buttonList.add(new GuiButton(103, centerX + 5, confirmButtonY, 95, 20, "取消"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id >= 0 && button.id < BUTTONS_PER_PAGE) {
                // 选择职业
                int index = scrollOffset + button.id;
                if (index < professionKeys.size()) {
                    tempSelectedProfession = professionKeys.get(index);
                    updateButtons();

                    // 播放点击音效
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            } else if (button.id == 100) {
                // 上一页
                scrollOffset = Math.max(0, scrollOffset - BUTTONS_PER_PAGE);
                updateButtons();
            } else if (button.id == 101) {
                // 下一页
                scrollOffset = Math.min(professionKeys.size() - BUTTONS_PER_PAGE, scrollOffset + BUTTONS_PER_PAGE);
                updateButtons();
            } else if (button.id == 102) {
                // 确认选择
                if (!tempSelectedProfession.equals(selectedProfession)) {
                    // 发送数据包到服务器
                    PacketHandler.INSTANCE.sendToServer(new PacketUpdateVillagerTransformer(tempSelectedProfession, hand == EnumHand.MAIN_HAND));
                }
                mc.displayGuiScreen(null);
            } else if (button.id == 103) {
                // 取消
                mc.displayGuiScreen(null);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            int centerX = width / 2;
            int centerY = height / 2;
            int startY = centerY - 90;  // 基准位置
            int buttonStartY = startY + 35;  // 按钮实际开始位置

            // === 顶部信息区域 ===

            // 标题
            String title = "选择村民职业";
            drawCenteredString(fontRenderer, title, centerX, startY - 40, 0xFFFFFF);

            // 警告信息
            drawCenteredString(fontRenderer, TextFormatting.RED + "警告: 60%概率失败！", centerX, startY - 25, 0xFFFFFF);
            drawCenteredString(fontRenderer, TextFormatting.DARK_RED + "失败将变成无法转换的傻子！", centerX, startY - 12, 0xFFFFFF);

            // === 当前选择显示区域 ===
            String currentProfName = PROFESSION_NAMES_CN.getOrDefault(selectedProfession, formatProfessionName(selectedProfession));
            String currentText = "当前职业: " + TextFormatting.YELLOW + currentProfName;
            drawCenteredString(fontRenderer, currentText, centerX, startY + 5, 0xFFFFFF);

            // 如果有新选择，显示在下方
            if (!tempSelectedProfession.equals(selectedProfession)) {
                String tempProfName = PROFESSION_NAMES_CN.getOrDefault(tempSelectedProfession, formatProfessionName(tempSelectedProfession));
                String tempText = "将要改为: " + TextFormatting.GREEN + tempProfName;
                drawCenteredString(fontRenderer, tempText, centerX, startY + 18, 0xFFFFFF);
            }

            // === 按钮区域的图标 ===

            // 绘制职业图标（使用buttonStartY）
            for (int i = 0; i < BUTTONS_PER_PAGE && (scrollOffset + i) < professionKeys.size(); i++) {
                String profKey = professionKeys.get(scrollOffset + i);

                // 获取对应的图标
                ProfessionCareerPair pair = PROFESSION_MAP.get(profKey);
                ItemStack icon = null;
                if (pair != null) {
                    icon = PROFESSION_ICONS.get(pair.professionName);
                }
                if (icon == null) {
                    icon = new ItemStack(Items.EMERALD);
                }

                int iconX = centerX - 120;
                int iconY = buttonStartY + i * 22 + 2;  // 使用buttonStartY

                // 高亮当前临时选择
                if (profKey.equals(tempSelectedProfession)) {
                    drawRect(iconX - 2, iconY - 2, iconX + 18, iconY + 18, 0xFF00FF00);
                    drawRect(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xFF000000);
                }

                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(icon, iconX, iconY);
                RenderHelper.disableStandardItemLighting();

                // 标记当前职业（星号在右边）
                if (profKey.equals(selectedProfession)) {
                    drawString(fontRenderer, TextFormatting.GOLD + "★", centerX + 105, buttonStartY + i * 22 + 6, 0xFFFFFF);
                }
            }

            // === 底部信息区域 ===

            // 页码信息（放在翻页按钮下方）
            if (professionKeys.size() > BUTTONS_PER_PAGE) {
                int currentPage = (scrollOffset / BUTTONS_PER_PAGE) + 1;
                int totalPages = (professionKeys.size() + BUTTONS_PER_PAGE - 1) / BUTTONS_PER_PAGE;
                String pageInfo = "第 " + currentPage + "/" + totalPages + " 页";

                // 计算页码显示位置（在翻页按钮下方）
                int pageButtonY = buttonStartY + BUTTONS_PER_PAGE * 22 + 5;
                drawCenteredString(fontRenderer, pageInfo, centerX, pageButtonY + 25, 0xAAAAAA);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            if (keyCode == 1) {  // ESC键
                mc.displayGuiScreen(null);
            }
            super.keyTyped(typedChar, keyCode);
        }
    }
}