package com.moremod.causal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import java.util.*;

public final class CausalFieldManager {
    private static class Field {
        final int dim;
        int r;
        long until;
        double x, y, z;

        Field(int d, double x, double y, double z, int r, long u){
            this.dim=d;
            this.x=x; this.y=y; this.z=z;
            this.r=r;
            this.until=u;
        }
    }

    private static final Map<UUID, Field> FIELDS = new HashMap<>();

    public static void activate(EntityPlayer p, int r, long until){
        FIELDS.put(p.getUniqueID(),
                new Field(p.dimension, p.posX, p.posY, p.posZ, r, until));
    }

    public static void refreshCenter(EntityPlayer p){
        Field f=FIELDS.get(p.getUniqueID());
        if(f!=null) {
            f.x=p.posX;
            f.y=p.posY;
            f.z=p.posZ;
        }
    }

    public static void deactivate(EntityPlayer p){
        FIELDS.remove(p.getUniqueID());
    }

    public static boolean isActive(EntityPlayer p){
        return FIELDS.containsKey(p.getUniqueID());
    }

    /**
     * 檢查坐標是否在任何沉默場內
     * 使用圓柱體範圍：XZ 平面用半徑，Y 軸用固定高度
     */
    public static boolean isInField(World w, double x, double y, double z){
        if (FIELDS.isEmpty()) return false;
        long now = w.getTotalWorldTime();
        Iterator<Map.Entry<UUID,Field>> it = FIELDS.entrySet().iterator();

        while (it.hasNext()){
            Field f = it.next().getValue();
            if (f.until<=now){
                it.remove();
                continue;
            }
            if (f.dim != w.provider.getDimension()) continue;

            double dx = f.x - x;
            double dy = f.y - y;
            double dz = f.z - z;

            // 圓柱體檢測：XZ 半徑 + Y 高度限制
            if (dx*dx + dz*dz <= (double)f.r*f.r && Math.abs(dy) <= 6) {
                return true;
            }
        }
        return false;
    }
}