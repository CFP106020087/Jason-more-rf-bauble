// File: com/moremod/dimension/PersonalDimensionManager.java
package com.moremod.dimension;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 私人维度管理系统 - 智能加载优化版
 * ✅ 无玩家时自动卸载维度
 * ✅ 有玩家时自动加载维度
 * ✅ 大幅降低服务器资源消耗
 */
public class PersonalDimensionManager {

    public static final int PERSONAL_DIM_ID = 100;

    // 几何参数
    private static final int SPACE_WIDTH = 30, SPACE_HEIGHT = 15, SPACE_DEPTH = 30;
    private static final int WALL_THICKNESS = 1, SPACE_PADDING = 800;
    private static final int TERRITORY_RADIUS = 400;

    // 定时参数
    private static final long SAVE_INTERVAL = 300_000L;
    private static final long CLEANUP_INTERVAL = 600_000L;
    private static final long INACTIVE_THRESHOLD = 3_600_000L;

    // ✅ 新增：维度自动卸载参数
    private static final long DIMENSION_UNLOAD_DELAY = 30_000L; // 30秒无玩家后卸载
    private static long lastPlayerLeftTime = 0;
    private static boolean shouldCheckUnload = false;

    // 数据
    private static final Map<UUID, PersonalSpace> playerSpaces = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerToSpaceIndex = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> spaceIndexToPlayer = new ConcurrentHashMap<>();
    private static final Map<BlockPos, WeakReference<UUID>> spaceOwners = new ConcurrentHashMap<>();

    // 门洞 & 墙恢复 - 支持多门队列
    private static final Map<UUID, List<DoorHoleEntry>> doorHoleQueue = new ConcurrentHashMap<>();

    // 待生成
    private static final Map<UUID, Long> pendingGenerations = new ConcurrentHashMap<>();

    private static final BatchBlockUpdater batchUpdater = new BatchBlockUpdater();

    // 状态
    private static boolean isDimensionInitialized = false;
    private static boolean isDataLoaded = false;
    private static boolean isRegistered = false;

    private static final Object ALLOCATION_LOCK = new Object();
    private static final Object FILE_LOCK = new Object();
    private static long lastSaveTime = 0, lastCleanupTime = 0;

    // ---------------- 内部类 ----------------

    /**
     * 门洞条目 - 记录一组门洞及其恢复时间
     */
    public static class DoorHoleEntry {
        public final List<BlockPos> positions;
        public final long restoreTime;

        public DoorHoleEntry(List<BlockPos> positions, long restoreTime) {
            this.positions = new ArrayList<>(positions);
            this.restoreTime = restoreTime;
        }
    }

    private static class BatchBlockUpdater {
        private final Map<BlockPos, IBlockState> pending = new HashMap<>();
        private static final int BATCH = 100;
        synchronized void add(BlockPos p, IBlockState s){ pending.put(p, s); if(pending.size()>=BATCH) flush(); }
        synchronized void flush(){
            if(pending.isEmpty()) return;
            WorldServer w = DimensionManager.getWorld(PERSONAL_DIM_ID);
            if(w!=null) pending.forEach((p,s)-> w.setBlockState(p,s,2));
            pending.clear();
        }
        synchronized void flushToWorld(World w){
            if(pending.isEmpty()) return;
            pending.forEach((p,s)-> w.setBlockState(p,s,2));
            pending.clear();
        }
    }

    public static class PersonalSpace {
        public UUID playerId; public String playerName; public int index;
        public final BlockPos centerPos, innerMinPos, innerMaxPos, outerMinPos, outerMaxPos;
        public final long createdTime;
        public boolean isActive, isGenerated, hasVoidStructures;
        public long lastActiveTime;

        public PersonalSpace(UUID pid, String name, int idx){
            this.playerId=pid; this.playerName=name; this.index=idx;
            this.createdTime = System.currentTimeMillis();
            this.lastActiveTime = createdTime;
            this.isActive = true; this.isGenerated=false; this.hasVoidStructures=false;

            BlockPos grid = calculateSpiral(idx);
            int cx=grid.getX(), cz=grid.getZ(), cy=128;
            this.centerPos = new BlockPos(cx, cy, cz);
            this.innerMinPos = new BlockPos(cx - SPACE_WIDTH/2,  cy - SPACE_HEIGHT/2, cz - SPACE_DEPTH/2);
            this.innerMaxPos = new BlockPos(cx + SPACE_WIDTH/2,  cy + SPACE_HEIGHT/2, cz + SPACE_DEPTH/2);
            this.outerMinPos = new BlockPos(innerMinPos.getX()-WALL_THICKNESS, innerMinPos.getY()-WALL_THICKNESS, innerMinPos.getZ()-WALL_THICKNESS);
            this.outerMaxPos = new BlockPos(innerMaxPos.getX()+WALL_THICKNESS, innerMaxPos.getY()+WALL_THICKNESS, innerMaxPos.getZ()+WALL_THICKNESS);
        }

        public PersonalSpace(UUID pid, String name, int idx,
                             BlockPos c, BlockPos inMin, BlockPos inMax, BlockPos outMin, BlockPos outMax,
                             long created, long lastActive, boolean active, boolean gen, boolean structs){
            this.playerId=pid; this.playerName=name; this.index=idx;
            this.centerPos=c; this.innerMinPos=inMin; this.innerMaxPos=inMax; this.outerMinPos=outMin; this.outerMaxPos=outMax;
            this.createdTime=created; this.lastActiveTime=lastActive;
            this.isActive=active; this.isGenerated=gen; this.hasVoidStructures=structs;
        }

        public void updateActivity(){ lastActiveTime = System.currentTimeMillis(); }

        public boolean isInInnerSpace(BlockPos p){
            return p.getX()>=innerMinPos.getX() && p.getX()<=innerMaxPos.getX()
                    && p.getY()>=innerMinPos.getY() && p.getY()<=innerMaxPos.getY()
                    && p.getZ()>=innerMinPos.getZ() && p.getZ()<=innerMaxPos.getZ();
        }
        public boolean isInOuterSpace(BlockPos p){
            return p.getX()>=outerMinPos.getX() && p.getX()<=outerMaxPos.getX()
                    && p.getY()>=outerMinPos.getY() && p.getY()<=outerMaxPos.getY()
                    && p.getZ()>=outerMinPos.getZ() && p.getZ()<=outerMaxPos.getZ();
        }
        public boolean isWall(BlockPos p){ return isInOuterSpace(p) && !isInInnerSpace(p); }

        public boolean isInTerritory(BlockPos p){
            return Math.abs(p.getX()-centerPos.getX())<=TERRITORY_RADIUS
                    && Math.abs(p.getZ()-centerPos.getZ())<=TERRITORY_RADIUS;
        }

        public NBTTagCompound toNBT(){
            NBTTagCompound t=new NBTTagCompound();
            t.setString("playerName", playerName);
            t.setInteger("index", index);
            t.setBoolean("isGenerated", isGenerated);
            t.setBoolean("hasVoidStructures", hasVoidStructures);
            t.setLong("createdTime", createdTime);
            t.setLong("lastActiveTime", lastActiveTime);
            t.setBoolean("isActive", isActive);
            t.setInteger("cx", centerPos.getX()); t.setInteger("cy", centerPos.getY()); t.setInteger("cz", centerPos.getZ());
            writePos(t,"inMin",innerMinPos); writePos(t,"inMax",innerMaxPos); writePos(t,"outMin",outerMinPos); writePos(t,"outMax",outerMaxPos);
            t.setInteger("dataVersion",2);
            return t;
        }
        public static PersonalSpace fromNBT(UUID pid, String name, NBTTagCompound tag){
            int idx = tag.getInteger("index");
            long created=tag.getLong("createdTime");
            long last= tag.hasKey("lastActiveTime")?tag.getLong("lastActiveTime"):System.currentTimeMillis();
            boolean active=tag.getBoolean("isActive");
            boolean gen   =tag.getBoolean("isGenerated");
            boolean structs= tag.hasKey("hasVoidStructures") && tag.getBoolean("hasVoidStructures");

            if(tag.hasKey("cx")){
                BlockPos c=new BlockPos(tag.getInteger("cx"), tag.getInteger("cy"), tag.getInteger("cz"));
                BlockPos inMin=readPos(tag,"inMin"), inMax=readPos(tag,"inMax"), outMin=readPos(tag,"outMin"), outMax=readPos(tag,"outMax");
                return new PersonalSpace(pid,name,idx,c,inMin,inMax,outMin,outMax,created,last,active,gen,structs);
            }else{
                PersonalSpace ps=new PersonalSpace(pid,name,idx);
                ps.isActive=active; ps.isGenerated=gen; ps.hasVoidStructures=structs; ps.lastActiveTime=last;
                return ps;
            }
        }

        private static void writePos(NBTTagCompound r,String k,BlockPos p){ NBTTagCompound t=new NBTTagCompound(); t.setInteger("x",p.getX()); t.setInteger("y",p.getY()); t.setInteger("z",p.getZ()); r.setTag(k,t); }
        private static BlockPos readPos(NBTTagCompound r,String k){ NBTTagCompound t=r.getCompoundTag(k); return new BlockPos(t.getInteger("x"),t.getInteger("y"),t.getInteger("z")); }

        private static BlockPos calculateSpiral(int i){
            if(i==0) return new BlockPos(0,0,0);
            int k=(int)Math.ceil((Math.sqrt(i+1)-1)/2); int t=2*k+1; int m=t*t; t=t-1; int x,z;
            if(i>=m-t){ x=k-(m-i); z=-k; }
            else{ m=m-t; if(i>=m-t){ x=-k; z=-k+(m-i); }
            else{ m=m-t; if(i>=m-t){ x=-k+(m-i); z=k; } else { x=k; z=k-(m-t-i); } } }
            x*=SPACE_PADDING; z*=SPACE_PADDING; return new BlockPos(x,0,z);
        }
    }

    // -------------- 初始化 / 世界加载 --------------

    public static void init(){
        if(!isRegistered) isRegistered=true;
    }

    public static void reset(){
        batchUpdater.flush();
        playerSpaces.clear(); playerToSpaceIndex.clear(); spaceIndexToPlayer.clear(); spaceOwners.clear();
        doorHoleQueue.clear(); pendingGenerations.clear();
        isDimensionInitialized=false; isDataLoaded=false; lastSaveTime=0; lastCleanupTime=0;
        shouldCheckUnload = false;
        lastPlayerLeftTime = 0;
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load e){
        if(e.getWorld().isRemote) return;
        int dim=e.getWorld().provider.getDimension();
        if(dim==0 && !isDataLoaded){
            loadPlayerSpaces();
            loadBindings();
            isDataLoaded=true;
        }
    }

    // ✅ 优化：只在需要时加载维度，不强制保持加载
    private static void ensureDimensionLoaded(){
        if(isDimensionInitialized) return;
        WorldServer overworld=DimensionManager.getWorld(0);
        if(overworld==null) return;
        if(!DimensionManager.isDimensionRegistered(PERSONAL_DIM_ID)){
            PersonalDimensionType.registerDimension();
            if(!DimensionManager.isDimensionRegistered(PERSONAL_DIM_ID)) return;
        }

        // ✅ 移除了 keepDimensionLoaded(true)，让维度可以自动卸载
        isDimensionInitialized=true;
        System.out.println("[私人维度] ✅ 维度已加载（智能管理模式）");
    }

    private static WorldServer getPersonalDimensionWorld(){
        WorldServer w = DimensionManager.getWorld(PERSONAL_DIM_ID);
        if(w!=null) return w;
        ensureDimensionLoaded();
        try{
            DimensionManager.initDimension(PERSONAL_DIM_ID);
            w = DimensionManager.getWorld(PERSONAL_DIM_ID);
            if(w != null) {
                System.out.println("[私人维度] ✅ 维度世界已初始化");
            }
            return w;
        }catch(Throwable t){
            System.err.println("[私人维度] ❌ 维度初始化失败: " + t.getMessage());
            return null;
        }
    }

    // -------------- 玩家事件 --------------

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e){
        if(e.player.world.isRemote) return;
        if(!isDataLoaded){ loadPlayerSpaces(); loadBindings(); isDataLoaded=true; }
        PersonalSpace s = getOrCreateSpaceWithBinding(e.player);
        if(!s.playerId.equals(e.player.getUniqueID())){
            s.playerId=e.player.getUniqueID(); s.playerName=e.player.getName(); savePlayerSpaces();
        }
        s.updateActivity();
        if(!s.isGenerated && pendingGenerations.size()<20){
            pendingGenerations.put(e.player.getUniqueID(), System.currentTimeMillis()+3000L);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e){
        if(e.player.world.isRemote) return;
        pendingGenerations.remove(e.player.getUniqueID());
        PersonalSpace s=playerSpaces.get(e.player.getUniqueID());
        if(s!=null) s.updateActivity();

        // ✅ 检查是否需要卸载维度
        if(e.player.dimension == PERSONAL_DIM_ID) {
            checkDimensionUnload();
        }

        savePlayerSpaces();
    }

    // ✅ 新增：检查维度是否应该卸载
    private static void checkDimensionUnload() {
        WorldServer w = DimensionManager.getWorld(PERSONAL_DIM_ID);
        if(w != null) {
            List<EntityPlayer> players = getPlayersInDimension(w);
            if(players.isEmpty()) {
                lastPlayerLeftTime = System.currentTimeMillis();
                shouldCheckUnload = true;
                System.out.println("[私人维度] ⏳ 维度中无玩家，将在" + (DIMENSION_UNLOAD_DELAY/1000) + "秒后卸载");
            }
        }
    }

    // ✅ 新增：安全卸载维度
    private static void unloadDimensionSafely() {
        WorldServer w = DimensionManager.getWorld(PERSONAL_DIM_ID);
        if(w == null) return;

        // 最后保存一次
        System.out.println("[私人维度] 💾 卸载前保存数据...");
        batchUpdater.flush();
        savePlayerSpaces();
        saveBindings();

        // 清理WorldProvider
        if(w.provider instanceof PersonalDimensionWorldProvider) {
            ((PersonalDimensionWorldProvider) w.provider).cleanup();
        }

        // 清理生成处理器缓存
        PersonalDimensionSpawnHandler.onWorldUnload();

        // 标记为未初始化，下次需要时会重新加载
        isDimensionInitialized = false;

        System.out.println("[私人维度] ✅ 维度已安全卸载，节省资源");
    }

    public static void teleportToPersonalSpace(EntityPlayer p){
        if(p.world.isRemote) return;
        if(!isDataLoaded){ loadPlayerSpaces(); loadBindings(); isDataLoaded=true; }
        PersonalSpace s = getOrCreateSpaceWithBinding(p);
        s.updateActivity();

        // ✅ 取消卸载计划（有玩家要进入）
        shouldCheckUnload = false;

        WorldServer w = getPersonalDimensionWorld();
        if(w==null){
            p.sendStatusMessage(new TextComponentString(TextFormatting.RED+"无法加载私人维度！"), true);
            return;
        }

        if(!s.isGenerated && probeAndFixGenerated(w, s)){
            // 探测到已生成
        }

        if(!s.isGenerated){
            generateCompleteSpace(w, s);
        }

        if(!s.hasVoidStructures && p.dimension==PERSONAL_DIM_ID){
            generatePlayerStructures(w, s, p);
            s.hasVoidStructures=true;
        }

        if(p.dimension!=PERSONAL_DIM_ID){
            p.changeDimension(PERSONAL_DIM_ID, new PersonalTeleporter(w, s.centerPos));
        }else{
            ((EntityPlayerMP)p).connection.setPlayerLocation(
                    s.centerPos.getX()+0.5, s.centerPos.getY(), s.centerPos.getZ()+0.5,
                    p.rotationYaw, p.rotationPitch
            );
        }
        p.sendStatusMessage(new TextComponentString(TextFormatting.GREEN+"已传送到你的私人空间"), true);
    }

    private static PersonalSpace getOrCreateSpaceWithBinding(EntityPlayer player){
        UUID pid=player.getUniqueID(); String name=player.getName();
        synchronized(ALLOCATION_LOCK){
            PersonalSpace ex=playerSpaces.get(pid);
            if(ex!=null) return ex;

            Integer bound=playerToSpaceIndex.get(pid);
            if(bound!=null){
                UUID rev = spaceIndexToPlayer.get(bound);
                if(pid.equals(rev)){
                    PersonalSpace restored = new PersonalSpace(pid, name, bound);
                    playerSpaces.put(pid, restored);
                    registerSpaceOwnership(restored);
                    return restored;
                }else{
                    System.err.println("[私人维度] 绑定冲突：为 "+name+" 重新分配空间");
                }
            }
            int idx=allocateNewSpaceIndex();
            PersonalSpace s=new PersonalSpace(pid,name,idx);
            playerSpaces.put(pid,s);
            playerToSpaceIndex.put(pid,idx);
            spaceIndexToPlayer.put(idx,pid);
            registerSpaceOwnership(s);
            saveBindings(); savePlayerSpaces();
            return s;
        }
    }

    private static int allocateNewSpaceIndex(){
        int i=0; while(spaceIndexToPlayer.containsKey(i)) i++; return i;
    }

    private static void registerSpaceOwnership(PersonalSpace s){
        for(int x=s.outerMinPos.getX(); x<=s.outerMaxPos.getX(); x+=16)
            for(int z=s.outerMinPos.getZ(); z<=s.outerMaxPos.getZ(); z+=16)
                spaceOwners.put(new BlockPos(x,0,z), new WeakReference<>(s.playerId));
    }

    // -------------- Tick：延迟生成/恢复/保存清理 --------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END) return;

        // ✅ 检查是否需要卸载维度
        if(shouldCheckUnload) {
            long now = System.currentTimeMillis();
            if(now - lastPlayerLeftTime >= DIMENSION_UNLOAD_DELAY) {
                WorldServer w = DimensionManager.getWorld(PERSONAL_DIM_ID);
                if(w != null) {
                    List<EntityPlayer> players = getPlayersInDimension(w);
                    if(players.isEmpty()) {
                        unloadDimensionSafely();
                        shouldCheckUnload = false;
                    } else {
                        // 有玩家重新进入，取消卸载
                        shouldCheckUnload = false;
                        System.out.println("[私人维度] ✅ 检测到玩家，取消卸载");
                    }
                }
            }
        }

        // 待生成
        if(!pendingGenerations.isEmpty()){
            long now=System.currentTimeMillis();
            Iterator<Map.Entry<UUID,Long>> it=pendingGenerations.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<UUID,Long> en=it.next();
                if(now>=en.getValue()){
                    PersonalSpace s=playerSpaces.get(en.getKey());
                    if(s!=null && !s.isGenerated){
                        WorldServer w=DimensionManager.getWorld(PERSONAL_DIM_ID);
                        if(w!=null){
                            if(!probeAndFixGenerated(w,s)){
                                generateCompleteSpace(w,s);
                            }
                        }
                    }
                    it.remove(); break;
                }
            }
        }

        // 墙恢复 - 处理多门队列
        if(!doorHoleQueue.isEmpty()){
            long now=System.currentTimeMillis();
            WorldServer w=DimensionManager.getWorld(PERSONAL_DIM_ID);
            if(w!=null){
                // 使用快照避免并发修改
                List<UUID> playerIds = new ArrayList<>(doorHoleQueue.keySet());
                for(UUID playerId : playerIds){
                    List<DoorHoleEntry> entries = doorHoleQueue.get(playerId);
                    if(entries == null) continue;

                    PersonalSpace s = playerSpaces.get(playerId);
                    if(s==null || !s.isGenerated) continue;

                    // 收集需要恢复的门洞
                    List<DoorHoleEntry> toRestore = new ArrayList<>();
                    synchronized(entries) {
                        Iterator<DoorHoleEntry> it = entries.iterator();
                        while(it.hasNext()){
                            DoorHoleEntry entry = it.next();
                            if(now >= entry.restoreTime && entry.restoreTime != Long.MAX_VALUE){
                                toRestore.add(entry);
                                it.remove();
                            }
                        }
                    }

                    // 在同步块外执行恢复
                    for(DoorHoleEntry entry : toRestore){
                        restoreDoorHoles(w, s, entry.positions);
                    }
                }
                // 清理空列表
                doorHoleQueue.entrySet().removeIf(en -> en.getValue().isEmpty());
            }
        }

        // 定期保存/清理
        if(e.side.isServer()){
            long now=System.currentTimeMillis();
            if(now-lastSaveTime>SAVE_INTERVAL){ batchUpdater.flush(); savePlayerSpaces(); saveBindings(); lastSaveTime=now; }
            if(now-lastCleanupTime>CLEANUP_INTERVAL){ cleanupInactiveSpaces(); lastCleanupTime=now; }
        }
    }

    // -------------- 方块保护 --------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getWorld().provider.getDimension()!=PERSONAL_DIM_ID) return;
        EntityPlayer p=e.getPlayer(); BlockPos pos=e.getPos(); IBlockState st=e.getState();
        PersonalSpace s=findSpaceByPos(pos); if(s==null) return;
        s.updateActivity();
        if(st.getBlock().getRegistryName()!=null){
            String id=st.getBlock().getRegistryName().toString();
            if(id.contains("unbreakable_barrier") && s.isWall(pos)){
                e.setCanceled(true);
                p.sendStatusMessage(new TextComponentString(TextFormatting.RED+"⚠ 维度锚定墙壁不可破坏！"), true);
                return;
            }
        }
        if(!s.playerId.equals(p.getUniqueID()) && s.isInInnerSpace(pos)){
            e.setCanceled(true);
            p.sendStatusMessage(new TextComponentString(TextFormatting.RED+"你不能破坏其他玩家的私人空间！"), true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent e){
        if(e.getWorld().provider.getDimension()!=PERSONAL_DIM_ID) return;
        EntityPlayer p=e.getPlayer(); BlockPos pos=e.getPos();
        PersonalSpace s=findSpaceByPos(pos); if(s==null) return;
        s.updateActivity();
        if(s.isWall(pos)){
            IBlockState st=e.getWorld().getBlockState(pos);
            if(st.getBlock().getRegistryName()!=null){
                String id=st.getBlock().getRegistryName().toString();
                if(id.contains("unbreakable_barrier")){
                    e.setCanceled(true);
                    p.sendStatusMessage(new TextComponentString(TextFormatting.RED+"不能在维度锚定墙壁上放置方块！"), true);
                    return;
                }
            }
        }
        if(!s.playerId.equals(p.getUniqueID()) && s.isInInnerSpace(pos)){
            e.setCanceled(true);
            p.sendStatusMessage(new TextComponentString(TextFormatting.RED+"你不能在其他玩家的私人空间放置方块！"), true);
        }
    }

    // -------------- 生成 --------------

    private static void generateCompleteSpace(World w, PersonalSpace s){
        if(s.isGenerated) return;
        for(int cx=(s.outerMinPos.getX()>>4); cx<=(s.outerMaxPos.getX()>>4); cx++)
            for(int cz=(s.outerMinPos.getZ()>>4); cz<=(s.outerMaxPos.getZ()>>4); cz++)
                w.getChunk(cx,cz);

        IBlockState wall = getAnchorWallBlockSafe();
        generateWallsBatched(w,s,wall);
        clearInteriorBatched(w,s);
        generateFloorBatched(w,s);
        addInfrastructureBatched(w,s);
        batchUpdater.flushToWorld(w);

        s.isGenerated=true;
        savePlayerSpaces();
    }

    private static void generateWallsBatched(World w, PersonalSpace s, IBlockState wall){
        int y=s.outerMinPos.getY();
        for(int x=s.outerMinPos.getX(); x<=s.outerMaxPos.getX(); x++)
            for(int z=s.outerMinPos.getZ(); z<=s.outerMaxPos.getZ(); z++)
                batchUpdater.add(new BlockPos(x,y,z), wall);
        y=s.outerMaxPos.getY();
        for(int x=s.outerMinPos.getX(); x<=s.outerMaxPos.getX(); x++)
            for(int z=s.outerMinPos.getZ(); z<=s.outerMaxPos.getZ(); z++)
                batchUpdater.add(new BlockPos(x,y,z), wall);
        for(y=s.outerMinPos.getY()+1; y<s.outerMaxPos.getY(); y++){
            for(int x=s.outerMinPos.getX(); x<=s.outerMaxPos.getX(); x++){
                batchUpdater.add(new BlockPos(x,y,s.outerMinPos.getZ()), wall);
                batchUpdater.add(new BlockPos(x,y,s.outerMaxPos.getZ()), wall);
            }
            for(int z=s.outerMinPos.getZ()+1; z<s.outerMaxPos.getZ(); z++){
                batchUpdater.add(new BlockPos(s.outerMinPos.getX(),y,z), wall);
                batchUpdater.add(new BlockPos(s.outerMaxPos.getX(),y,z), wall);
            }
        }
    }

    private static void clearInteriorBatched(World w, PersonalSpace s){
        IBlockState air=Blocks.AIR.getDefaultState();
        for(int x=s.innerMinPos.getX(); x<=s.innerMaxPos.getX(); x++)
            for(int y=s.innerMinPos.getY(); y<=s.innerMaxPos.getY(); y++)
                for(int z=s.innerMinPos.getZ(); z<=s.innerMaxPos.getZ(); z++){
                    BlockPos p=new BlockPos(x,y,z);
                    if(w.getBlockState(p).getBlock()!=Blocks.AIR) batchUpdater.add(p, air);
                }
    }

    private static void generateFloorBatched(World w, PersonalSpace s){
        int y=s.innerMinPos.getY();
        for(int x=s.innerMinPos.getX(); x<=s.innerMaxPos.getX(); x++)
            for(int z=s.innerMinPos.getZ(); z<=s.innerMaxPos.getZ(); z++){
                BlockPos p=new BlockPos(x,y,z);
                int dx=Math.abs(x-s.centerPos.getX()), dz=Math.abs(z-s.centerPos.getZ());
                if(dx<=1 && dz<=1) batchUpdater.add(p, Blocks.SEA_LANTERN.getDefaultState());
                else if(dx<=2 && dz<=2) batchUpdater.add(p, Blocks.QUARTZ_BLOCK.getDefaultState());
                else batchUpdater.add(p, Blocks.STONE.getDefaultState());
            }
    }

    private static void addInfrastructureBatched(World w, PersonalSpace s){
        int fy=s.innerMinPos.getY()+1;
        BlockPos[] corners={
                new BlockPos(s.innerMinPos.getX()+2,fy,s.innerMinPos.getZ()+2),
                new BlockPos(s.innerMaxPos.getX()-2,fy,s.innerMinPos.getZ()+2),
                new BlockPos(s.innerMinPos.getX()+2,fy,s.innerMaxPos.getZ()-2),
                new BlockPos(s.innerMaxPos.getX()-2,fy,s.innerMaxPos.getZ()-2)
        };
        for(BlockPos c:corners){ batchUpdater.add(c,Blocks.GLOWSTONE.getDefaultState()); batchUpdater.add(c.up(),Blocks.END_ROD.getDefaultState()); }
        int ly=s.innerMinPos.getY()+4;
        for(int i=s.innerMinPos.getX()+10; i<s.innerMaxPos.getX(); i+=10){
            batchUpdater.add(new BlockPos(i,ly,s.innerMinPos.getZ()+1),Blocks.GLOWSTONE.getDefaultState());
            batchUpdater.add(new BlockPos(i,ly,s.innerMaxPos.getZ()-1),Blocks.GLOWSTONE.getDefaultState());
        }
    }

    /**
     * 恢复指定的门洞位置
     */
    private static void restoreDoorHoles(World w, PersonalSpace s, List<BlockPos> holes){
        if(holes==null || holes.isEmpty()) return;
        IBlockState wall=getAnchorWallBlockSafe();
        for(BlockPos p:holes) {
            if(s.isWall(p)) {
                w.setBlockState(p, wall, 2);
            }
        }
    }

    private static IBlockState getAnchorWallBlockSafe(){
        try{ Block b = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR; if(b!=null) return b.getDefaultState(); }
        catch(Throwable ignore){}
        return Blocks.BEDROCK.getDefaultState();
    }

    // -------------- "存在性探测 + 自愈" --------------

    private static boolean probeAndFixGenerated(WorldServer w, PersonalSpace s){
        if(s.isGenerated) return true;
        for(int cx=(s.outerMinPos.getX()>>4); cx<=(s.outerMaxPos.getX()>>4); cx++)
            for(int cz=(s.outerMinPos.getZ()>>4); cz<=(s.outerMaxPos.getZ()>>4); cz++)
                w.getChunk(cx, cz);

        List<BlockPos> samples=new ArrayList<>(16);
        BlockPos min=s.outerMinPos, max=s.outerMaxPos;
        int yBot=min.getY(), yTop=max.getY(), yMid=(yBot+yTop)/2;

        samples.add(new BlockPos(min.getX(), yBot, min.getZ()));
        samples.add(new BlockPos(max.getX(), yBot, min.getZ()));
        samples.add(new BlockPos(min.getX(), yBot, max.getZ()));
        samples.add(new BlockPos(max.getX(), yBot, max.getZ()));
        samples.add(new BlockPos(min.getX(), yTop, min.getZ()));
        samples.add(new BlockPos(max.getX(), yTop, min.getZ()));
        samples.add(new BlockPos(min.getX(), yTop, max.getZ()));
        samples.add(new BlockPos(max.getX(), yTop, max.getZ()));
        samples.add(new BlockPos(min.getX(), yMid, (min.getZ()+max.getZ())/2));
        samples.add(new BlockPos(max.getX(), yMid, (min.getZ()+max.getZ())/2));
        samples.add(new BlockPos((min.getX()+max.getX())/2, yMid, min.getZ()));
        samples.add(new BlockPos((min.getX()+max.getX())/2, yMid, max.getZ()));

        int anchorHits=0, checked=0;
        for(BlockPos p:samples){
            if(!w.isBlockLoaded(p)) continue;
            Block b = w.getBlockState(p).getBlock();
            if(b==Blocks.BEDROCK || (b.getRegistryName()!=null && b.getRegistryName().toString().contains("unbreakable_barrier"))){
                anchorHits++;
            }
            checked++;
        }

        BlockPos floorCenter=new BlockPos(s.centerPos.getX(), s.innerMinPos.getY(), s.centerPos.getZ());
        boolean hasCenterLamp=false;
        if(w.isBlockLoaded(floorCenter)){
            Block fb=w.getBlockState(floorCenter).getBlock();
            hasCenterLamp = (fb==Blocks.SEA_LANTERN || fb==Blocks.QUARTZ_BLOCK || fb==Blocks.STONE);
        }

        if(checked>=4 && anchorHits>=3){
            s.isGenerated=true;
            savePlayerSpaces();
            return true;
        }

        if(hasCenterLamp){
            s.isGenerated=true;
            savePlayerSpaces();
            return true;
        }

        return false;
    }

    // -------------- 辅助 --------------

    private static PersonalSpace findSpaceByPos(BlockPos p){
        for(PersonalSpace s:playerSpaces.values()) if(s.isInOuterSpace(p)) return s; return null;
    }

    public static PersonalSpace getPlayerSpace(UUID id){ return playerSpaces.get(id); }
    public static boolean hasPersonalSpace(UUID id){ return playerSpaces.containsKey(id); }
    public static Collection<PersonalSpace> getAllSpaces(){ return playerSpaces.values(); }

    public static UUID findSpaceOwner(BlockPos p){
        for(Map.Entry<UUID,PersonalSpace> e:playerSpaces.entrySet())
            if(e.getValue().isInTerritory(p)) return e.getKey();
        return null;
    }

    public static List<EntityPlayer> getPlayersInDimension(World w){
        List<EntityPlayer> list=new ArrayList<>();
        if(w instanceof WorldServer){
            for(EntityPlayer p:((WorldServer)w).playerEntities)
                if(p.dimension==PERSONAL_DIM_ID && !p.isDead) list.add(p);
        }
        return list;
    }

    public static boolean hasPlayersInDimension(World w){
        return w.provider.getDimension()==PERSONAL_DIM_ID && !getPlayersInDimension(w).isEmpty();
    }

    private static void cleanupInactiveSpaces(){
        long now=System.currentTimeMillis();
        List<UUID> toMark=playerSpaces.entrySet().stream()
                .filter(en-> now - en.getValue().lastActiveTime > INACTIVE_THRESHOLD)
                .filter(en-> !isPlayerOnline(en.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        for(UUID id:toMark){ PersonalSpace s=playerSpaces.get(id); if(s!=null) s.isActive=false; }
        spaceOwners.entrySet().removeIf(en->{ WeakReference<UUID> ref=en.getValue(); return ref==null || ref.get()==null; });
    }

    private static boolean isPlayerOnline(UUID id){
        WorldServer w=DimensionManager.getWorld(0); if(w==null) return false;
        return w.getMinecraftServer().getPlayerList().getPlayers().stream().anyMatch(p->p.getUniqueID().equals(id));
    }

    // -------------- 数据 I/O --------------

    public static void savePlayerSpaces(){ savePlayerSpacesInternal(); }

    private static void savePlayerSpacesInternal(){
        synchronized(FILE_LOCK){
            File f=getSaveFile(); if(f==null) return;
            try{
                NBTTagCompound root=new NBTTagCompound(), spaces=new NBTTagCompound();
                for(Map.Entry<UUID,PersonalSpace> en:playerSpaces.entrySet())
                    spaces.setTag(en.getKey().toString(), en.getValue().toNBT());
                root.setTag("spaces",spaces); root.setInteger("version",2);
                if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
                try(FileOutputStream fos=new FileOutputStream(f);
                    DataOutputStream dos=new DataOutputStream(fos)){
                    net.minecraft.nbt.CompressedStreamTools.writeCompressed(root,dos);
                }
            }catch(Exception ex){ ex.printStackTrace(); }
        }
    }

    public static void saveBindings(){
        synchronized(FILE_LOCK){
            File f=getBindingFile(); if(f==null) return;
            try{
                NBTTagCompound root=new NBTTagCompound();
                NBTTagCompound u2i=new NBTTagCompound();
                for(Map.Entry<UUID,Integer> en:playerToSpaceIndex.entrySet())
                    u2i.setInteger(en.getKey().toString(), en.getValue());
                root.setTag("uuidToIndex",u2i);
                NBTTagCompound i2u=new NBTTagCompound();
                for(Map.Entry<Integer,UUID> en:spaceIndexToPlayer.entrySet())
                    i2u.setString(String.valueOf(en.getKey()), en.getValue().toString());
                root.setTag("indexToUuid",i2u);
                root.setInteger("version",1);
                root.setLong("lastModified",System.currentTimeMillis());
                if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
                try(FileOutputStream fos=new FileOutputStream(f);
                    DataOutputStream dos=new DataOutputStream(fos)){
                    net.minecraft.nbt.CompressedStreamTools.writeCompressed(root,dos);
                }
            }catch(Exception ex){ ex.printStackTrace(); }
        }
    }

    private static void loadPlayerSpaces(){
        synchronized(FILE_LOCK){
            File f=getSaveFile(); if(f==null || !f.exists()) return;
            try(FileInputStream fis=new FileInputStream(f);
                DataInputStream dis=new DataInputStream(fis)){
                NBTTagCompound root= net.minecraft.nbt.CompressedStreamTools.readCompressed(dis);
                playerSpaces.clear(); spaceOwners.clear();
                NBTTagCompound spaces=root.getCompoundTag("spaces");
                for(String key: spaces.getKeySet()){
                    try{
                        UUID pid=UUID.fromString(key);
                        NBTTagCompound data=spaces.getCompoundTag(key);
                        String name=data.getString("playerName");
                        PersonalSpace s=PersonalSpace.fromNBT(pid,name,data);
                        playerSpaces.put(pid,s);
                        playerToSpaceIndex.put(pid, s.index);
                        spaceIndexToPlayer.put(s.index, pid);
                        registerSpaceOwnership(s);
                    }catch(Exception ex){ ex.printStackTrace(); }
                }
            }catch(Exception ex){ ex.printStackTrace(); }
        }
    }

    private static void loadBindings(){
        synchronized(FILE_LOCK){
            File f=getBindingFile(); if(f==null || !f.exists()) return;
            try(FileInputStream fis=new FileInputStream(f);
                DataInputStream dis=new DataInputStream(fis)){
                NBTTagCompound root= net.minecraft.nbt.CompressedStreamTools.readCompressed(dis);
                playerToSpaceIndex.clear(); spaceIndexToPlayer.clear();
                if(root.hasKey("uuidToIndex")){
                    NBTTagCompound u2i=root.getCompoundTag("uuidToIndex");
                    for(String k:u2i.getKeySet())
                        playerToSpaceIndex.put(UUID.fromString(k), u2i.getInteger(k));
                }
                if(root.hasKey("indexToUuid")){
                    NBTTagCompound i2u=root.getCompoundTag("indexToUuid");
                    for(String k:i2u.getKeySet())
                        spaceIndexToPlayer.put(Integer.parseInt(k), UUID.fromString(i2u.getString(k)));
                }
            }catch(Exception ex){ ex.printStackTrace(); }
        }
    }

    private static File getSaveFile(){
        WorldServer overworld=DimensionManager.getWorld(0);
        if(overworld==null) return null;
        File dir=overworld.getSaveHandler().getWorldDirectory();
        return new File(dir,"data/personal_dimensions.dat");
    }

    private static File getBindingFile(){
        WorldServer overworld=DimensionManager.getWorld(0);
        if(overworld==null) return null;
        File dir=overworld.getSaveHandler().getWorldDirectory();
        return new File(dir,"data/personal_dimension_bindings.dat");
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save e){
        if(e.getWorld().isRemote) return;
        int dim=e.getWorld().provider.getDimension();
        long now=System.currentTimeMillis();
        if(dim==0){
            if(now-lastSaveTime>SAVE_INTERVAL){
                batchUpdater.flush();
                savePlayerSpacesInternal(); saveBindings();
                lastSaveTime=now;
            }
        }else if(dim==PERSONAL_DIM_ID){
            batchUpdater.flushToWorld(e.getWorld());
            savePlayerSpacesInternal();
        }
    }

    // -------------- 钥匙/门洞 API --------------

    /**
     * 添加一组门洞到队列，并设置恢复时间
     * 支持多门同时存在，每组门洞独立计时
     */
    public static void setDoorHoles(UUID playerId, List<BlockPos> holes){
        if(holes==null || holes.isEmpty()) return;
        List<DoorHoleEntry> queue = doorHoleQueue.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized(queue) {
            // 添加到队列 - 恢复时间由 scheduleWallRestore 设置
            queue.add(new DoorHoleEntry(holes, Long.MAX_VALUE));
        }
    }

    /**
     * 设置最近添加的门洞的恢复时间
     */
    public static void scheduleWallRestore(UUID playerId, int ticks){
        if(ticks<=0) ticks=1;
        long restoreTime = System.currentTimeMillis() + ticks * 50L;

        List<DoorHoleEntry> queue = doorHoleQueue.get(playerId);
        if(queue != null) {
            synchronized(queue) {
                // 找到最后一个未设置恢复时间的条目（restoreTime == Long.MAX_VALUE）
                for(int i = queue.size() - 1; i >= 0; i--){
                    DoorHoleEntry entry = queue.get(i);
                    if(entry.restoreTime == Long.MAX_VALUE){
                        // 替换为有正确恢复时间的新条目
                        queue.set(i, new DoorHoleEntry(entry.positions, restoreTime));
                        break;
                    }
                }
            }
        }
    }

    /**
     * 记录单个门洞位置（用于逐块添加）
     */
    public static void recordDoorHole(UUID playerId, BlockPos pos){
        List<DoorHoleEntry> queue = doorHoleQueue.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized(queue) {
            // 添加到最后一个条目，如果没有则创建新条目
            if(queue.isEmpty() || queue.get(queue.size()-1).restoreTime != Long.MAX_VALUE){
                List<BlockPos> newList = new ArrayList<>();
                newList.add(pos);
                queue.add(new DoorHoleEntry(newList, Long.MAX_VALUE));
            } else {
                queue.get(queue.size()-1).positions.add(pos);
            }
        }
    }

    // -------------- 玩家专属建筑 --------------

    private static void generatePlayerStructures(WorldServer world, PersonalSpace space, EntityPlayer player){
        int count=3+world.rand.nextInt(5);
        for(int i=0;i<count;i++){
            double ang=world.rand.nextDouble()*Math.PI*2;
            int dist=100+world.rand.nextInt(100);
            int x=space.centerPos.getX()+(int)(Math.cos(ang)*dist);
            int z=space.centerPos.getZ()+(int)(Math.sin(ang)*dist);
            int y=96+world.rand.nextInt(64);
            BlockPos pos=new BlockPos(x,y,z);
            if(space.isInTerritory(pos)){
                VoidStructureGenerator.StructureType t= VoidStructureGenerator.selectRandomStructure();
                VoidStructureGenerator.generateStructureOptimized(world,pos,t);
            }
        }
    }
}