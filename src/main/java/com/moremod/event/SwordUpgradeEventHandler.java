package com.moremod.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import crafttweaker.api.minecraft.CraftTweakerMC;
import com.moremod.compat.crafttweaker.*;
import java.util.*;

public class SwordUpgradeEventHandler {

    private Map<EntityPlayer, Integer> tickCounters = new WeakHashMap<>();

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            ItemStack sword = player.getHeldItemMainhand();

            if (sword.getItem() instanceof ItemSword && hasUpgrades(sword)) {
                List<String> materials = getUpgradedMaterials(sword);

                for (String materialId : materials) {
                    List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);

                    for (IUpgradeEffect effect : effects) {
                        if (effect instanceof IOnHitEffect) {
                            ((IOnHitEffect) effect).onHit(
                                    CraftTweakerMC.getIPlayer(player),
                                    CraftTweakerMC.getIEntityLivingBase(event.getEntityLiving()),
                                    CraftTweakerMC.getIItemStack(sword),
                                    event.getAmount()
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            ItemStack sword = player.getHeldItemMainhand();

            if (sword.getItem() instanceof ItemSword && hasUpgrades(sword)) {
                List<String> materials = getUpgradedMaterials(sword);

                for (String materialId : materials) {
                    List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);

                    for (IUpgradeEffect effect : effects) {
                        if (effect instanceof IOnKillEffect) {
                            ((IOnKillEffect) effect).onKill(
                                    CraftTweakerMC.getIPlayer(player),
                                    CraftTweakerMC.getIEntityLivingBase(event.getEntityLiving()),
                                    CraftTweakerMC.getIItemStack(sword)
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        ItemStack sword = player.getHeldItemMainhand();

        if (sword.getItem() instanceof ItemSword && hasUpgrades(sword)) {
            List<String> materials = getUpgradedMaterials(sword);

            for (String materialId : materials) {
                List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);

                for (IUpgradeEffect effect : effects) {
                    if (effect instanceof IOnBlockBreakEffect) {
                        ((IOnBlockBreakEffect) effect).onBlockBreak(
                                CraftTweakerMC.getIPlayer(player),
                                CraftTweakerMC.getIWorld(event.getWorld()),
                                CraftTweakerMC.getIBlockPos(event.getPos()),
                                CraftTweakerMC.getIItemStack(sword)
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EntityPlayer player = event.player;
            ItemStack sword = player.getHeldItemMainhand();

            if (sword.getItem() instanceof ItemSword && hasUpgrades(sword)) {
                int tickCount = tickCounters.getOrDefault(player, 0) + 1;
                tickCounters.put(player, tickCount);

                if (tickCount % 20 == 0) { // 每秒執行一次
                    List<String> materials = getUpgradedMaterials(sword);

                    for (String materialId : materials) {
                        List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);

                        for (IUpgradeEffect effect : effects) {
                            if (effect instanceof IOnTickEffect) {
                                ((IOnTickEffect) effect).onTick(
                                        CraftTweakerMC.getIPlayer(player),
                                        CraftTweakerMC.getIItemStack(sword),
                                        tickCount / 20
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack sword = player.getHeldItemMainhand();

        if (sword.getItem() instanceof ItemSword && hasUpgrades(sword)) {
            List<String> materials = getUpgradedMaterials(sword);

            for (String materialId : materials) {
                List<IUpgradeEffect> effects = CTSwordUpgrade.getEffects(materialId);

                for (IUpgradeEffect effect : effects) {
                    if (effect instanceof IOnRightClickEffect) {
                        ((IOnRightClickEffect) effect).onRightClick(
                                CraftTweakerMC.getIPlayer(player),
                                CraftTweakerMC.getIWorld(event.getWorld()),
                                CraftTweakerMC.getIBlockPos(event.getPos()),
                                CraftTweakerMC.getIItemStack(sword)
                        );
                    }
                }
            }
        }
    }

    private boolean hasUpgrades(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("SwordUpgrades");
    }

    private List<String> getUpgradedMaterials(ItemStack stack) {
        List<String> materials = new ArrayList<>();

        if (!hasUpgrades(stack)) return materials;

        NBTTagCompound upgrades = stack.getTagCompound().getCompoundTag("SwordUpgrades");
        if (upgrades.hasKey("Inlays")) {
            NBTTagCompound inlays = upgrades.getCompoundTag("Inlays");
            int count = inlays.getInteger("Count");

            for (int i = 0; i < count; i++) {
                NBTTagCompound inlay = inlays.getCompoundTag("Inlay_" + i);
                materials.add(inlay.getString("Material"));
            }
        }

        return materials;
    }
}
