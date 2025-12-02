// BloodyThirstMaskEventHandler.java - 事件处理器
package com.moremod.events;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.items.BloodyThirstMask;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityElderGuardian;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "moremod")
public class BloodyThirstMaskEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        ItemStack mask = findEquippedBloodyThirstMask(player);

        if (!mask.isEmpty() && mask.getItem() instanceof BloodyThirstMask) {
            EntityLivingBase killed = event.getEntityLiving();
            String speciesId = getSpeciesId(killed);

            // 检查是否是新物种
            if (addNewSpecies(mask, speciesId, killed)) {
                // 新物种击杀！
                BloodyThirstMask item = (BloodyThirstMask) mask.getItem();
                int speciesCount = getSpeciesCount(mask);
                int tier = item.getMaskTier(speciesCount);
                int oldTier = item.getMaskTier(speciesCount - 1);

                // 显示击杀信息
                String speciesName = getSpeciesDisplayName(killed);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "☠ 新物种击杀: " + speciesName +
                                " (" + speciesCount + " 种)"), true);

                // 检查是否升级
                if (tier > oldTier) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GOLD + "嗜血面具升级! " +
                                    item.getMaskTierName(tier)), true);

                    // 升级特效
                    spawnUpgradeParticles(player);
                }

                // 击杀特效
                spawnKillParticles(killed);

                // 调试信息
                System.out.println("[DEBUG] 新物种击杀: " + speciesName + " (" + speciesId + ") - 总计: " + speciesCount);
            } else {
                // 重复击杀同类
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GRAY + "已击杀过的物种: " + getSpeciesDisplayName(killed)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        ItemStack mask = findEquippedBloodyThirstMask(player);

        if (!mask.isEmpty() && mask.getItem() instanceof BloodyThirstMask) {
            BloodyThirstMask item = (BloodyThirstMask) mask.getItem();
            int speciesCount = getSpeciesCount(mask);
            float lifestealPercent = item.getLifestealAmount(speciesCount);

            if (lifestealPercent > 0 && event.getAmount() > 0) {
                float healAmount = event.getAmount() * (lifestealPercent / 100.0f);
                player.heal(healAmount);

                // 吸血粒子效果
                spawnLifestealParticles(player);
            }
        }
    }

    // 获取生物的种类ID
    private static String getSpeciesId(EntityLivingBase entity) {
        // 使用实体的注册名作为种类ID
        String entityName = entity.getClass().getSimpleName();

        // 对于一些特殊情况，进行细分
        if (entity instanceof EntityVillager) {
            EntityVillager villager = (EntityVillager) entity;
            return entityName + "_" + villager.getProfession();
        }

        return entityName;
    }

    // 获取生物的显示名称
    private static String getSpeciesDisplayName(EntityLivingBase entity) {
        if (entity instanceof EntityVillager) {
            EntityVillager villager = (EntityVillager) entity;
            return "村民(" + villager.getProfessionForge().getRegistryName() + ")";
        }

        // 返回本地化名称，如果没有就用类名
        String displayName = entity.getDisplayName().getUnformattedText();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }

        return entity.getClass().getSimpleName();
    }

    // 添加新物种到击杀列表
    private static boolean addNewSpecies(ItemStack mask, String speciesId, EntityLivingBase killed) {
        NBTTagCompound nbt = mask.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            mask.setTagCompound(nbt);
        }

        // 获取已击杀物种列表
        NBTTagList speciesList;
        if (nbt.hasKey("killedSpecies")) {
            speciesList = nbt.getTagList("killedSpecies", 8); // 8 = NBTTagString
        } else {
            speciesList = new NBTTagList();
        }

        // 检查是否已经击杀过这个物种
        Set<String> existingSpecies = new HashSet<>();
        for (int i = 0; i < speciesList.tagCount(); i++) {
            existingSpecies.add(speciesList.getStringTagAt(i));
        }

        if (existingSpecies.contains(speciesId)) {
            return false; // 已经击杀过
        }

        // 添加新物种
        speciesList.appendTag(new NBTTagString(speciesId));
        nbt.setTag("killedSpecies", speciesList);

        // 同时保存物种的显示名称（用于展示）
        NBTTagList displayNames;
        if (nbt.hasKey("speciesDisplayNames")) {
            displayNames = nbt.getTagList("speciesDisplayNames", 8);
        } else {
            displayNames = new NBTTagList();
        }
        displayNames.appendTag(new NBTTagString(getSpeciesDisplayName(killed)));
        nbt.setTag("speciesDisplayNames", displayNames);

        return true; // 新物种
    }

    // 获取已击杀的物种数量
    private static int getSpeciesCount(ItemStack mask) {
        NBTTagCompound nbt = mask.getTagCompound();
        if (nbt == null || !nbt.hasKey("killedSpecies")) {
            return 0;
        }

        NBTTagList speciesList = nbt.getTagList("killedSpecies", 8);
        return speciesList.tagCount();
    }

    // 获取已击杀的物种列表（用于工具提示）
    public static Set<String> getKilledSpecies(ItemStack mask) {
        Set<String> species = new HashSet<>();
        NBTTagCompound nbt = mask.getTagCompound();
        if (nbt != null && nbt.hasKey("killedSpecies")) {
            NBTTagList speciesList = nbt.getTagList("killedSpecies", 8);
            for (int i = 0; i < speciesList.tagCount(); i++) {
                species.add(speciesList.getStringTagAt(i));
            }
        }
        return species;
    }

    // 获取已击杀物种的显示名称列表
    public static Set<String> getKilledSpeciesDisplayNames(ItemStack mask) {
        Set<String> names = new HashSet<>();
        NBTTagCompound nbt = mask.getTagCompound();
        if (nbt != null && nbt.hasKey("speciesDisplayNames")) {
            NBTTagList namesList = nbt.getTagList("speciesDisplayNames", 8);
            for (int i = 0; i < namesList.tagCount(); i++) {
                names.add(namesList.getStringTagAt(i));
            }
        }
        return names;
    }

    // 查找装备的嗜血面具
    private static ItemStack findEquippedBloodyThirstMask(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() instanceof BloodyThirstMask) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    // 击杀特效
    private static void spawnKillParticles(EntityLivingBase killed) {
        for (int i = 0; i < 15; i++) {
            killed.world.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    killed.posX + (killed.world.rand.nextDouble() - 0.5) * 2,
                    killed.posY + killed.world.rand.nextDouble() * 2,
                    killed.posZ + (killed.world.rand.nextDouble() - 0.5) * 2,
                    0, 0.1, 0);
        }
    }

    // 升级特效
    private static void spawnUpgradeParticles(EntityPlayer player) {
        for (int i = 0; i < 30; i++) {
            player.world.spawnParticle(net.minecraft.util.EnumParticleTypes.TOTEM,
                    player.posX + (player.world.rand.nextDouble() - 0.5) * 3,
                    player.posY + player.world.rand.nextDouble() * 3,
                    player.posZ + (player.world.rand.nextDouble() - 0.5) * 3,
                    (player.world.rand.nextDouble() - 0.5) * 0.2,
                    player.world.rand.nextDouble() * 0.2,
                    (player.world.rand.nextDouble() - 0.5) * 0.2);
        }
    }

    // 吸血特效
    private static void spawnLifestealParticles(EntityPlayer player) {
        if (player.world.rand.nextInt(3) == 0) {
            player.world.spawnParticle(net.minecraft.util.EnumParticleTypes.HEART,
                    player.posX + (player.world.rand.nextDouble() - 0.5) * 1,
                    player.posY + 1 + player.world.rand.nextDouble() * 1,
                    player.posZ + (player.world.rand.nextDouble() - 0.5) * 1,
                    0, 0.1, 0);
        }
    }
}

// 在RegisterItem.java中注册（已有代码，确保正确引用）
// public static final Item BLOODY_THIRST_MASK = new BloodyThirstMask();

// 语言文件 assets/moremod/lang/zh_cn.lang
/*
item.bloody_thirst_mask.name=嗜血面具
*/

// 合成配方示例
/*
public static void registerBloodyThirstMaskRecipe() {
    ItemStack result = new ItemStack(RegisterItem.BLOODY_THIRST_MASK);
    
    GameRegistry.addShapedRecipe(
        new ResourceLocation("moremod", "bloody_thirst_mask"),
        null,
        result,
        "RLR",
        "LHL", 
        "RLR",
        'R', Items.REDSTONE,
        'H', Items.NETHER_STAR,
        'L', Items.LEATHER
    );
}
*/