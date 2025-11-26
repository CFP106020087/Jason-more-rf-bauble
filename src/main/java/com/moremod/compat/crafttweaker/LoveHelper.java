package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.entity.IEntityAgeable;
import crafttweaker.api.entity.IEntityAnimal;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.UUID;

@ZenRegister
@ZenClass("mods.moremod.LoveHelper")
public class LoveHelper {

    private static final String TAG_ACTOR_DEBOUNCE = "moremod$breedActorUntil";
    private static final String TAG_PARENT_LOCK    = "moremod$breedParentUntil";

    /* ===================== 基础判断 ===================== */
    @ZenMethod
    public static boolean isAnimal(IEntity any) {
        if (any == null) return false;
        Object o = any.getInternal();

        // 原版动物
        if (o instanceof EntityAnimal) {
            return true;
        }

        // Lycanites Mobs
        if (isLycanitesCreature(o)) {
            return true;
        }

        return false;
    }

    @ZenMethod
    public static boolean isVillager(IEntity any) {
        return any != null && any.getInternal() instanceof EntityVillager;
    }

    @ZenMethod
    public static boolean sendChatIfPlayer(IEntity who, String msg) {
        if (who == null || msg == null) return false;
        Object o = who.getInternal();
        if (o instanceof EntityPlayer) {
            ((EntityPlayer) o).sendMessage(new TextComponentString(msg));
            return true;
        }
        return false;
    }

    /* ===================== Lycanites Mobs 检测 ===================== */
    private static boolean isLycanitesCreature(Object entity) {
        if (entity == null) return false;
        try {
            Class<?> lycanitesClass = Class.forName("com.lycanitesmobs.core.entity.AgeableCreatureEntity");
            return lycanitesClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /* ===================== 去抖/冷却工具 ===================== */
    private static boolean checkAndSetCooldown(Entity e, String tag, int ticks) {
        if (e == null || e.world == null) return false;
        NBTTagCompound data = e.getEntityData();
        long now = e.world.getTotalWorldTime();
        long until = data.getLong(tag);
        if (until > now) return false;
        data.setLong(tag, now + Math.max(1, ticks));
        return true;
    }

    @ZenMethod
    public static boolean debounceActor(IEntity actor, int ticks) {
        if (actor == null) return false;
        Object o = actor.getInternal();
        if (!(o instanceof Entity)) return false;
        return checkAndSetCooldown((Entity) o, TAG_ACTOR_DEBOUNCE, ticks);
    }

    private static boolean lockParents(Entity a, Entity b, int ticks) {
        boolean okA = checkAndSetCooldown(a, TAG_PARENT_LOCK, ticks);
        boolean okB = checkAndSetCooldown(b, TAG_PARENT_LOCK, ticks);
        return okA && okB;
    }

    /* ===================== 动物：发情 ===================== */
    @ZenMethod
    public static boolean setAnimalInLove(IEntityAnimal ctAnimal, @Optional IPlayer ctPlayer) {
        if (ctAnimal == null) return false;
        Object o = ctAnimal.getInternal();
        if (!(o instanceof EntityAnimal)) return false;
        EntityAnimal a = (EntityAnimal) o;
        if (a.isChild()) a.setGrowingAge(0);
        EntityPlayer p = null;
        if (ctPlayer != null) {
            Object raw = ctPlayer.getInternal();
            if (raw instanceof EntityPlayer) p = (EntityPlayer) raw;
        }
        a.setInLove(p);
        return true;
    }

    @ZenMethod
    public static boolean setAnimalInLoveEntity(IEntity any) {
        if (any == null) return false;
        Object o = any.getInternal();

        // 原版动物
        if (o instanceof EntityAnimal) {
            EntityAnimal a = (EntityAnimal) o;
            if (a.isChild()) a.setGrowingAge(0);
            a.setInLove(null);
            return true;
        }

        // Lycanites Mobs
        if (isLycanitesCreature(o)) {
            return setLycanitesInLove(o);
        }

        return false;
    }

    @ZenMethod
    public static boolean setAnimalInLoveByUUID(IEntity anchor, String otherUuid, double radius) {
        if (anchor == null || otherUuid == null) return false;
        Object o = anchor.getInternal();
        if (!(o instanceof Entity)) return false;
        Entity center = (Entity) o;
        World w = center.world;
        if (w.isRemote) return false;

        Entity target = findByUUIDAround(w, center, otherUuid, radius);
        if (target == null) return false;

        // 原版动物
        if (target instanceof EntityAnimal) {
            EntityAnimal a = (EntityAnimal) target;
            if (a.isChild()) a.setGrowingAge(0);
            a.setInLove(null);
            return true;
        }

        // Lycanites Mobs
        if (isLycanitesCreature(target)) {
            return setLycanitesInLove(target);
        }

        return false;
    }

    /* ===================== Lycanites Mobs 繁殖逻辑 ===================== */
    private static boolean setLycanitesInLove(Object entity) {
        if (entity == null) return false;
        try {
            Class<?> lycanitesClass = Class.forName("com.lycanitesmobs.core.entity.AgeableCreatureEntity");
            if (!lycanitesClass.isInstance(entity)) return false;

            // 检查是否成年: canBreed()
            Object canBreed = lycanitesClass.getMethod("canBreed").invoke(entity);
            if (!(boolean) canBreed) {
                // 如果是幼体，强制成年
                lycanitesClass.getMethod("setGrowingAge", int.class).invoke(entity, 0);
            }

            // 调用 breed() 进入发情状态
            Object success = lycanitesClass.getMethod("breed").invoke(entity);
            return (boolean) success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ===================== 村民：愿意/配对/强制生娃 ===================== */
    @ZenMethod
    public static boolean setVillagerWillingEntity(IEntity any) {
        if (any == null) return false;
        Object o = any.getInternal();
        if (!(o instanceof EntityVillager)) return false;
        EntityVillager v = (EntityVillager) o;
        if (v.isChild()) v.setGrowingAge(0);
        v.setIsWillingToMate(true);
        return true;
    }

    @ZenMethod
    public static boolean pairVillagersEntity(IEntity any, double radius) {
        if (any == null) return false;
        Object o = any.getInternal();
        if (!(o instanceof EntityVillager)) return false;
        EntityVillager a = (EntityVillager) o;
        World w = a.world;
        if (w.isRemote) return false;

        AxisAlignedBB box = a.getEntityBoundingBox().grow(radius);
        List<EntityVillager> list = w.getEntitiesWithinAABB(EntityVillager.class, box, v -> v != a && !v.isChild());
        if (list.isEmpty()) {
            a.setGrowingAge(0);
            a.setIsWillingToMate(true);
            return false;
        }
        EntityVillager b = list.get(0);
        a.setGrowingAge(0);
        b.setGrowingAge(0);
        a.setIsWillingToMate(true);
        b.setIsWillingToMate(true);
        return true;
    }

    @ZenMethod
    public static boolean forceVillagerBabyByUUID(IEntity anchor, String partnerUUID, double radius, int parentCooldownTicks) {
        if (anchor == null || partnerUUID == null) return false;
        Object o = anchor.getInternal();
        if (!(o instanceof EntityVillager)) return false;
        EntityVillager a = (EntityVillager) o;
        World w = a.world;
        if (w.isRemote) return false;

        Entity partner = findByUUIDAround(w, a, partnerUUID, radius);
        if (!(partner instanceof EntityVillager)) return false;
        EntityVillager b = (EntityVillager) partner;

        if (!lockParents(a, b, Math.max(1, parentCooldownTicks))) return false;

        a.setGrowingAge(0);
        b.setGrowingAge(0);
        a.setIsWillingToMate(true);
        b.setIsWillingToMate(true);

        EntityAgeable baby = a.createChild(b);
        if (baby == null) return false;

        double mx = (a.posX + b.posX) * 0.5;
        double my = Math.max(a.posY, b.posY);
        double mz = (a.posZ + b.posZ) * 0.5;
        baby.setGrowingAge(-24000);
        baby.setLocationAndAngles(mx, my, mz, w.rand.nextFloat() * 360f, 0);
        w.spawnEntity(baby);

        a.setIsWillingToMate(false);
        b.setIsWillingToMate(false);
        a.setGrowingAge(6000);
        b.setGrowingAge(6000);
        return true;
    }

    /* ===================== 工具：按 UUID 在附近找实体 ===================== */
    private static Entity findByUUIDAround(World w, Entity center, String uuidStr, double radius) {
        try {
            UUID uid = UUID.fromString(uuidStr);
            AxisAlignedBB box = center.getEntityBoundingBox().grow(radius);
            List<Entity> list = w.getEntitiesWithinAABB(Entity.class, box, e -> e.getUniqueID().equals(uid));
            return list.isEmpty() ? null : list.get(0);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}