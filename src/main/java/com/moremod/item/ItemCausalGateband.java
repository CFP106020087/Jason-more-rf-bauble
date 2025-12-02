package com.moremod.item;

import baubles.api.*;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.causal.CausalFieldManager;
import com.moremod.item.causal.EnergyHelper;
import com.moremod.creativetab.moremodCreativeTab;
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import com.moremod.compat.ChampionReflectionHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.*;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCausalGateband extends Item implements IBauble {

    // ===== 可調參（可改成 @Config） =====
    public static int REQUIRED_ACTIVE_MODULES = 1;
    public static int MIN_LEVEL_FOR_COUNT = 1;
    public static int FIELD_RADIUS = 6;
    public static int SILENCE_SECONDS = 30;     // 固定 30s
    public static int COOLDOWN_SECONDS = 30;
    public static int ACTIVATE_ENERGY = 0;      // 設 0 = 不耗能
    public static int KEEP_ALIVE_ENERGY_PER_TICK = 0;

    private static final String NBT_ACTIVE_UNTIL   = "GatebandActiveUntil";
    private static final String NBT_COOLDOWN_UNTIL = "GatebandCooldownUntil";
    private static final String NBT_CACHE_ACTIVE   = "CachedActive";
    private static final String NBT_CACHE_QUALI    = "CachedQualified";

    public ItemCausalGateband() {
        setRegistryName("causal_gateband");
        setTranslationKey("causal_gateband");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }

    // 佩戴條件（需要機械核心 + 模組數）
    @Override public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return false;
        EntityPlayer p = (EntityPlayer) wearer;
        CoreInfo info = analyzeCore(p);
        if (!info.hasCore) { if (!p.world.isRemote) tip(p, TextFormatting.RED+"⚠ 需要佩戴機械核心"); return false; }
        if (info.activeModules < REQUIRED_ACTIVE_MODULES) { if (!p.world.isRemote) tip(p, TextFormatting.RED+"⚠ 激活模組不足（"+info.activeModules+"/"+REQUIRED_ACTIVE_MODULES+"）"); return false; }
        return true;
    }
    @Override public void onEquipped(ItemStack st, EntityLivingBase w){ if (w instanceof EntityPlayer && !w.world.isRemote){ cacheInfo(st, analyzeCore((EntityPlayer) w)); tip((EntityPlayer) w, TextFormatting.GOLD+"✦ 因果闕帶就位（智能沉默）"); } }
    @Override public void onUnequipped(ItemStack st, EntityLivingBase w){ if (w instanceof EntityPlayer && !w.world.isRemote){ CausalFieldManager.deactivate((EntityPlayer) w); ensureTag(st).setLong(NBT_ACTIVE_UNTIL,0); tip((EntityPlayer) w, TextFormatting.GRAY+"✦ 因果闕帶已摘下"); } }

    // 被動偵測 → 沉默30s → 冷卻
    @Override public void onWornTick(ItemStack st, EntityLivingBase w){
        if (!(w instanceof EntityPlayer) || w.world.isRemote) return;
        EntityPlayer p = (EntityPlayer) w;
        long now = p.world.getTotalWorldTime();
        NBTTagCompound tag = ensureTag(st);
        long activeUntil = tag.getLong(NBT_ACTIVE_UNTIL);
        long cdUntil     = tag.getLong(NBT_COOLDOWN_UNTIL);

        if ((now & 19) == 0) cacheInfo(st, analyzeCore(p)); // 每秒更新 Tooltip 快取

        if (activeUntil > now) {
            if (KEEP_ALIVE_ENERGY_PER_TICK>0 && !EnergyHelper.drainCoreEnergy(p, KEEP_ALIVE_ENERGY_PER_TICK)){
                CausalFieldManager.deactivate(p); tag.setLong(NBT_ACTIVE_UNTIL,0); tag.setLong(NBT_COOLDOWN_UNTIL, now+COOLDOWN_SECONDS*20L);
                tip(p, TextFormatting.RED+"✖ 能量不足，沉默提前結束"); return;
            }
            CausalFieldManager.refreshCenter(p);
            return;
        }
        if (CausalFieldManager.isActive(p)) { // 剛結束 → 開冷卻
            CausalFieldManager.deactivate(p);
            tag.setLong(NBT_COOLDOWN_UNTIL, now+COOLDOWN_SECONDS*20L);
        }
        if (cdUntil > now) return;
        if ((now % 5) != 0) return; // 每 5 tick 掃一次

        if (hasEliteNearby(p, FIELD_RADIUS)) {
            if (ACTIVATE_ENERGY>0 && !EnergyHelper.drainCoreEnergy(p, ACTIVATE_ENERGY)) { tip(p, TextFormatting.RED+"⚠ 能量不足，無法啟動沉默"); return; }
            long until = now + SILENCE_SECONDS*20L;
            tag.setLong(NBT_ACTIVE_UNTIL, until);
            tag.setLong(NBT_COOLDOWN_UNTIL, until + COOLDOWN_SECONDS*20L);
            CausalFieldManager.activate(p, FIELD_RADIUS, until);
            tip(p, TextFormatting.GOLD+"◎ 因果闕帶觸發：沉默 "+SILENCE_SECONDS+"s，半徑 "+FIELD_RADIUS+" 格");
        }
    }

    private boolean hasEliteNearby(EntityPlayer p, int r){
        AxisAlignedBB box = new AxisAlignedBB(p.posX-r, p.posY-3, p.posZ-r, p.posX+r, p.posY+3, p.posZ+r);
        for (EntityLivingBase e : p.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (!e.isEntityAlive() || e==p) continue;
            // Infernal
            if (InfernalMobsCore.getIsRareEntity(e)) return true;
            // Champions
            if (e instanceof EntityLiving && ChampionReflectionHelper.isChampionsAvailable()) {
                Object chp = ChampionReflectionHelper.getChampionship((EntityLiving) e);
                Object rank = chp != null ? ChampionReflectionHelper.getRank(chp) : null;
                if (rank != null && ChampionReflectionHelper.getTier(rank) > 0) return true;
            }
        }
        return false;
    }

    // === 核心判定（使用 ItemMechanicalCore 的標準方法避免雙重計數） ===
    private static class CoreInfo { boolean hasCore; int activeModules; int qualifiedModules; }
    private CoreInfo analyzeCore(EntityPlayer player){
        CoreInfo info = new CoreInfo();
        IBaublesItemHandler b = BaublesApi.getBaublesHandler(player);
        if (b==null) return info;
        ItemStack core = ItemStack.EMPTY;
        for (int i=0;i<b.getSlots();i++){ ItemStack s=b.getStackInSlot(i); if(!s.isEmpty() && isMechanicalCore(s)){ core=s; info.hasCore=true; break; } }
        if (core.isEmpty()) return info;
        // 使用 ItemMechanicalCore 的標準方法，避免雙重計數
        info.activeModules = ItemMechanicalCore.getTotalActiveUpgradeLevel(core);
        info.qualifiedModules = info.activeModules; // 使用相同值，因為 getTotalActiveUpgradeLevel 已過濾
        return info;
    }
    private boolean isMechanicalCore(ItemStack s){
        if (s.isEmpty()) return false;
        String cn = s.getItem().getClass().getName();
        if (cn.contains("ItemMechanicalCore")) return true;
        if (s.getItem().getRegistryName()!=null){
            String name=s.getItem().getRegistryName().toString().toLowerCase();
            if (name.contains("mechanical") && name.contains("core")) return true;
        }
        return false;
    }

    private static NBTTagCompound ensureTag(ItemStack st){ NBTTagCompound t=st.getTagCompound(); if(t==null){t=new NBTTagCompound(); st.setTagCompound(t);} return t; }
    private static void tip(EntityPlayer p, String msg){ p.sendStatusMessage(new TextComponentString(msg), true); }
    private void cacheInfo(ItemStack st, CoreInfo i){ NBTTagCompound t=ensureTag(st); t.setInteger(NBT_CACHE_ACTIVE,i.activeModules); t.setInteger(NBT_CACHE_QUALI,i.qualifiedModules); }

    @SideOnly(Side.CLIENT)
    @Override public void addInformation(ItemStack st, @Nullable World w, List<String> tip, ITooltipFlag f){
        tip.add(TextFormatting.GOLD+"═══ 因果闕帶 Causal Gateband ═══");
        tip.add(TextFormatting.GRAY+"被動：偵測菁英 → 自動沉默 30s → 冷卻");
        tip.add(TextFormatting.AQUA+"半徑: "+TextFormatting.WHITE+FIELD_RADIUS+" 格");
        tip.add(TextFormatting.AQUA+"冷卻: "+TextFormatting.WHITE+COOLDOWN_SECONDS+" 秒");
        if (ACTIVATE_ENERGY>0||KEEP_ALIVE_ENERGY_PER_TICK>0) tip.add(TextFormatting.AQUA+"能耗: "+TextFormatting.WHITE+ACTIVATE_ENERGY+" 啟動 / "+KEEP_ALIVE_ENERGY_PER_TICK+"/t");
        int a=st.hasTagCompound()?st.getTagCompound().getInteger(NBT_CACHE_ACTIVE):0, q=st.hasTagCompound()?st.getTagCompound().getInteger(NBT_CACHE_QUALI):0;
        tip.add(TextFormatting.YELLOW+"核心激活模組: "+TextFormatting.WHITE+a+"（合格 "+q+"）");
        if (GuiScreen.isShiftKeyDown()) { tip.add(TextFormatting.GRAY+"• 對 Infernal / Champions 皆生效（場內暫時去詞條/降階）"); }
    }
}
