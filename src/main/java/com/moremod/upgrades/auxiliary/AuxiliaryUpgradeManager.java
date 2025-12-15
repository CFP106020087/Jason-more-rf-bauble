package com.moremod.upgrades.auxiliary;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumHandSide;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 辅助类升级效果管理器 - 完整修复版
 */
@Mod.EventBusSubscriber
public class AuxiliaryUpgradeManager {

    // ====== 全局调试与限频 ======
    public static boolean DEBUG_MODE = false;
    private static final Map<UUID, Map<String, Long>> lastMessageTimes = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 3000L;

    private static void debug(String s) { if (DEBUG_MODE) System.out.println("[AuxiliaryUpgrade] " + s); }

    private static boolean canSendMessage(EntityPlayer p, String type) {
        UUID id = p.getUniqueID();
        Map<String, Long> m = lastMessageTimes.computeIfAbsent(id, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long last = m.get(type);
        if (last == null || now - last >= MESSAGE_COOLDOWN) { m.put(type, now); return true; }
        return false;
    }

    // ========================= 矿物透视 =========================
    public static class OreVisionSystem {
        private static final Set<Block> ALL_ORE_BLOCKS = new HashSet<>();
        private static final List<Block> DISCOVERED_ORE_TYPES = new ArrayList<>();
        private static final Map<Block, String> ORE_DISPLAY_NAMES = new HashMap<>();
        private static final Map<BlockPos, Block> oreCache = new ConcurrentHashMap<>();
        private static final Set<ChunkPos> scannedChunks = new HashSet<>();

        private static boolean initialized = false;
        private static boolean renderingOres = false;
        private static boolean wasActiveLastTick = false;
        private static EntityPlayer currentPlayer = null;
        private static int currentLevel = 0;

        private static long lastFullScanTime = 0;
        private static long lastQuickScan = 0;
        private static long lastEnergyCheck = 0;
        private static BlockPos lastPlayerPos = BlockPos.ORIGIN;

        private static final long FULL_SCAN_COOLDOWN = 5000L;
        private static final long QUICK_SCAN_INTERVAL = 10L;
        private static final int MAX_RENDER_DISTANCE = 48;
        private static final int MAX_ORES_TO_RENDER = 500;
        private static int selectedOreIndex = -1;

        static {
            // 原版矿物
            Collections.addAll(ALL_ORE_BLOCKS,
                    Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE,
                    Blocks.EMERALD_ORE, Blocks.REDSTONE_ORE, Blocks.LIT_REDSTONE_ORE,
                    Blocks.LAPIS_ORE, Blocks.QUARTZ_ORE);

            ORE_DISPLAY_NAMES.put(Blocks.COAL_ORE, "煤炭");
            ORE_DISPLAY_NAMES.put(Blocks.IRON_ORE, "铁");
            ORE_DISPLAY_NAMES.put(Blocks.GOLD_ORE, "金");
            ORE_DISPLAY_NAMES.put(Blocks.DIAMOND_ORE, "钻石");
            ORE_DISPLAY_NAMES.put(Blocks.EMERALD_ORE, "绿宝石");
            ORE_DISPLAY_NAMES.put(Blocks.REDSTONE_ORE, "红石");
            ORE_DISPLAY_NAMES.put(Blocks.LIT_REDSTONE_ORE, "红石");
            ORE_DISPLAY_NAMES.put(Blocks.LAPIS_ORE, "青金石");
            ORE_DISPLAY_NAMES.put(Blocks.QUARTZ_ORE, "石英");
        }

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.PlaceEvent event) {
            if (event.getWorld().isRemote || event.getPlayer() == null) return;
            Block b = event.getPlacedBlock().getBlock();
            if (!ALL_ORE_BLOCKS.contains(b)) return;
            if (!renderingOres || currentPlayer == null) return;

            BlockPos pos = event.getPos();
            int range = 8 * currentLevel;
            if (pos.distanceSq(currentPlayer.getPosition()) <= range * range) {
                oreCache.put(pos, b);
                if (!DISCOVERED_ORE_TYPES.contains(b)) DISCOVERED_ORE_TYPES.add(b);
                debug("玩家放置矿物已添加到透视: " + pos);
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getWorld().isRemote) return;
            oreCache.remove(event.getPos());
        }

        public static void initializeOreDictionary() {
            if (initialized) return;
            initialized = true;

            debug("开始初始化矿物透视系统...");

            // 0. 立即添加特定模组矿物（不在线程中，确保扫描前完成）
            // Astral Sorcery 矿物
            addModOreByName("astralsorcery:blockcustomore", "星辉矿石");
            addModOreByName("astralsorcery:blockcelestialcrystals", "天辉水晶");
            addModOreByName("astralsorcery:celestialcrystals", "天辉水晶");
            addModOreByName("astralsorcery:blockcollectorcrystal", "集星水晶");
            addModOreByName("astralsorcery:blockcelestialcollectorcrystal", "天辉收集水晶");
            addModOreByName("astralsorcery:blockcelestialcrystalcluster", "天辉水晶簇");
            addModOreByName("astralsorcery:celestialcrystalcluster", "天辉水晶簇");
            addModOreByName("astralsorcery:block_celestial_crystals", "天辉水晶");
            addModOreByName("astralsorcery:celestial_crystals", "天辉水晶");
            addModOreByName("astralsorcery:rockcrystalore", "岩石水晶矿");
            addModOreByName("astralsorcery:rock_crystal_ore", "岩石水晶矿");
            // NetherBound 下界合金
            addModOreByName("nb:netherite_ore", "下界合金矿");
            addModOreByName("nb:ancient_debris", "远古残骸");
            addModOreByName("nb:nether_gold_ore", "下界金矿");
            addModOreByName("nb:quartz_ore", "下界石英矿");
            // Future MC / 其他 1.16 backport 模组
            addModOreByName("minecraft:ancient_debris", "远古残骸");
            addModOreByName("futuremc:ancient_debris", "远古残骸");
            addModOreByName("nether_backport:ancient_debris", "远古残骸");
            addModOreByName("nether_backport:netherite_ore", "下界合金矿");

            System.out.println("[OreVision] 已添加特殊模组矿物，当前矿物总数: " + ALL_ORE_BLOCKS.size());

            // 1. 后台线程加载矿物词典（耗时操作）
            new Thread(() -> {
                // 从矿物词典加载所有矿物（支持所有模组）
                for (String oreName : OreDictionary.getOreNames()) {
                    // 支持所有矿物词典前缀: ore, denseore, poorore, oreNether, oreEnd
                    if ((oreName.startsWith("ore") || oreName.startsWith("denseore") ||
                         oreName.startsWith("poorore") || oreName.contains("Ore")) &&
                        !oreName.contains("Nugget") && !oreName.contains("Block") && !oreName.contains("Storage")) {
                        for (ItemStack ore : OreDictionary.getOres(oreName, false)) {
                            if (ore.getItem() instanceof ItemBlock) {
                                Block block = ((ItemBlock) ore.getItem()).getBlock();
                                ALL_ORE_BLOCKS.add(block);
                                ORE_DISPLAY_NAMES.putIfAbsent(block, getOreDisplayName(oreName, block));
                            }
                        }
                    }
                }

                // 2. 扫描所有注册方块（无数量限制，支持所有模组矿物）
                for (Block block : ForgeRegistries.BLOCKS) {
                    if (block.getRegistryName() == null) continue;
                    String rn = block.getRegistryName().toString().toLowerCase();
                    String path = block.getRegistryName().getPath().toLowerCase();

                    // 检测矿物命名模式: xxx_ore, ore_xxx, xxxore, orexxx
                    boolean isOreByName = path.endsWith("_ore") || path.startsWith("ore_") ||
                                          path.endsWith("ore") || path.contains("_ore_") ||
                                          rn.contains(":ore") || rn.contains("_ore:");

                    // 排除非矿物方块
                    boolean isExcluded = rn.contains("storage") || rn.contains("block_") ||
                                        rn.contains("_block") || rn.contains("bricks") ||
                                        rn.contains("stairs") || rn.contains("slab");

                    if (isOreByName && !isExcluded) {
                        if (isLikelyOre(block)) {
                            ALL_ORE_BLOCKS.add(block);
                            ORE_DISPLAY_NAMES.putIfAbsent(block, extractOreName(rn));
                        }
                    }
                }
                debug("矿物透视系统初始化完成，已加载 " + ALL_ORE_BLOCKS.size() + " 种矿物");
            }, "OreVision-Init").start();
        }

        private static String getOreDisplayName(String oreName, Block block) {
            // 提取矿物名称（移除前缀）
            String name = oreName.toLowerCase(Locale.ROOT);
            if (name.startsWith("denseore")) name = name.substring(8);
            else if (name.startsWith("poorore")) name = name.substring(7);
            else if (name.startsWith("ore")) name = name.substring(3);

            // 常见矿物翻译（支持各种模组）
            switch (name) {
                // 基础金属
                case "copper": return "铜";
                case "tin": return "锡";
                case "silver": return "银";
                case "lead": return "铅";
                case "aluminum": case "aluminium": case "bauxite": return "铝";
                case "nickel": return "镍";
                case "platinum": return "铂";
                case "uranium": return "铀";
                case "zinc": return "锌";
                case "titanium": return "钛";
                case "tungsten": case "wolframium": return "钨";
                case "cobalt": return "钴";
                case "ardite": return "阿迪特";
                case "osmium": return "锇";
                case "iridium": return "铱";
                case "mithril": case "mana": return "秘银";
                case "adamantine": case "adamantium": return "精金";
                // 宝石类
                case "ruby": return "红宝石";
                case "sapphire": return "蓝宝石";
                case "peridot": return "橄榄石";
                case "topaz": return "黄玉";
                case "amethyst": return "紫水晶";
                case "apatite": return "磷灰石";
                case "certusquartz": return "赛特斯石英";
                case "chargedcertusquartz": return "充能石英";
                // 魔法/科技模组
                case "yellorite": case "yellorium": return "黄铀";
                case "draconium": return "龙矿石";
                case "inferium": return "下级精华矿";
                case "prosperity": return "繁荣矿";
                case "soulium": return "灵魂矿";
                case "niter": case "saltpeter": return "硝石";
                case "sulfur": return "硫磺";
                case "cinnabar": return "朱砂";
                case "nikolite": case "electrotine": return "蓝石";
                case "dimensional": case "dimensionalshard": return "维度碎片";
                // 其他
                default:
                    if (!name.isEmpty()) {
                        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
            }
            String rn = String.valueOf(block.getRegistryName());
            return rn.contains(":") ? rn.substring(rn.indexOf(':') + 1) : rn;
        }

        private static String extractOreName(String rn) {
            String name = rn.contains(":") ? rn.substring(rn.indexOf(':') + 1) : rn;
            name = name.replace("_ore", "").replace("ore_", "").replace("ore", "");
            if (!name.isEmpty()) name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return name.isEmpty() ? "未知矿物" : name;
        }

        /**
         * 通过注册名添加特定模组矿物
         */
        private static void addModOreByName(String registryName, String displayName) {
            try {
                net.minecraft.util.ResourceLocation rl = new net.minecraft.util.ResourceLocation(registryName);
                Block block = ForgeRegistries.BLOCKS.getValue(rl);
                if (block != null && block != Blocks.AIR) {
                    ALL_ORE_BLOCKS.add(block);
                    ORE_DISPLAY_NAMES.put(block, displayName);
                    debug("已添加模组矿物: " + registryName + " -> " + displayName);
                }
            } catch (Exception e) {
                debug("无法添加模组矿物: " + registryName + " - " + e.getMessage());
            }
        }

        private static boolean isLikelyOre(Block block) {
            try {
                IBlockState st = block.getDefaultState();
                Material m = st.getMaterial();
                if (m != Material.ROCK && m != Material.IRON) return false;
                float hard = st.getBlockHardness(null, null);
                return hard >= 1.5F && hard <= 5.0F;
            } catch (Throwable t) {
                return false;
            }
        }

        @SideOnly(Side.CLIENT)
        public static void scanForOres(EntityPlayer player, int level, boolean forceRescan) {
            if (level <= 0) return;

            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
            if (core.isEmpty()) return;

            if (!ItemMechanicalCore.isUpgradeEnabled(core, "ORE_VISION")) {
                if (renderingOres) {
                    renderingOres = false;
                    oreCache.clear();
                    scannedChunks.clear();
                    lastEnergyCheck = 0;
                    wasActiveLastTick = false;
                }
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastEnergyCheck > 1000) {
                if (!ItemMechanicalCore.consumeEnergyForUpgrade(core, "ORE_VISION", 50 + (level * 10))) {
                    renderingOres = false;
                    oreCache.clear();
                    scannedChunks.clear();
                    lastEnergyCheck = 0;
                    wasActiveLastTick = false;
                    return;
                }
                lastEnergyCheck = now;
            }

            if (!initialized) initializeOreDictionary();

            currentPlayer = player;
            currentLevel = level;

            boolean needFull = forceRescan || (now - lastFullScanTime > FULL_SCAN_COOLDOWN)
                    || player.getPosition().distanceSq(lastPlayerPos) > 256;

            if (needFull) {
                performFullScan(player, level);
                lastFullScanTime = now;
                lastPlayerPos = player.getPosition();
            } else if (now - lastQuickScan > QUICK_SCAN_INTERVAL) {
                performQuickScan(player, level);
                lastQuickScan = now;
            }
        }

        private static void performFullScan(EntityPlayer player, int level) {
            int range = 8 * level;
            BlockPos pp = player.getPosition();

            oreCache.entrySet().removeIf(e -> e.getKey().distanceSq(pp) > range * range * 2);
            scannedChunks.clear();
            Set<Block> found = new HashSet<>();

            int chunkRange = (range >> 4) + 1;
            for (int cx = -chunkRange; cx <= chunkRange; cx++) {
                for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                    ChunkPos cp = new ChunkPos((pp.getX() >> 4) + cx, (pp.getZ() >> 4) + cz);
                    if (!player.world.isBlockLoaded(new BlockPos(cp.x << 4, 64, cp.z << 4))) continue;
                    scanChunk(player, cp, pp, range, found);
                    scannedChunks.add(cp);
                }
            }
            if (!found.isEmpty()) updateDiscoveredOres(found);
        }

        private static void performQuickScan(EntityPlayer player, int level) {
            BlockPos pp = player.getPosition();
            int r = Math.min(16, 8 * level);
            Set<Block> found = new HashSet<>();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                BlockPos pos = pp.add(x, y, z);
                if (oreCache.containsKey(pos)) continue;
                IBlockState st = player.world.getBlockState(pos);
                Block b = st.getBlock();
                if (ALL_ORE_BLOCKS.contains(b)) {
                    oreCache.put(pos, b);
                    found.add(b);
                }
            }
            if (!found.isEmpty()) updateDiscoveredOres(found);
        }

        private static void scanChunk(EntityPlayer player, ChunkPos cp, BlockPos pp, int range, Set<Block> found) {
            Chunk ch = player.world.getChunk(cp.x, cp.z);
            if (ch == null || !ch.isLoaded()) return;
            int baseX = cp.x << 4, baseZ = cp.z << 4;
            int minY = Math.max(0, pp.getY() - range);
            int maxY = Math.min(255, pp.getY() + range);

            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                        if (pos.distanceSq(pp) > range * range) continue;
                        IBlockState st = ch.getBlockState(pos);
                        Block b = st.getBlock();
                        if (ALL_ORE_BLOCKS.contains(b)) {
                            oreCache.put(pos, b);
                            found.add(b);
                        }
                    }
        }

        private static void updateDiscoveredOres(Set<Block> found) {
            boolean any = false;
            for (Block b : found) if (!DISCOVERED_ORE_TYPES.contains(b)) { DISCOVERED_ORE_TYPES.add(b); any = true; }
            if (any) {
                DISCOVERED_ORE_TYPES.sort((a, b) -> {
                    String na = ORE_DISPLAY_NAMES.getOrDefault(a, String.valueOf(a.getRegistryName()));
                    String nb = ORE_DISPLAY_NAMES.getOrDefault(b, String.valueOf(b.getRegistryName()));
                    return na.compareTo(nb);
                });
            }
        }

        private static boolean shouldRenderOre(Block b) {
            if (selectedOreIndex == -1) return true;
            return selectedOreIndex >= 0 && selectedOreIndex < DISCOVERED_ORE_TYPES.size()
                    && DISCOVERED_ORE_TYPES.get(selectedOreIndex) == b;
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public static void onRenderWorldLast(RenderWorldLastEvent event) {
            if (!renderingOres || oreCache.isEmpty() || currentPlayer == null) return;
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) return;

            float pt = event.getPartialTicks();
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();

            double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
            double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
            double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

            List<Map.Entry<BlockPos, Block>> list = new ArrayList<>();
            for (Map.Entry<BlockPos, Block> e : oreCache.entrySet()) {
                if (!shouldRenderOre(e.getValue())) continue;
                BlockPos pos = e.getKey();
                double d2 = player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (d2 <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) list.add(e);
            }
            list.sort(Comparator.comparingDouble(a -> player.getDistanceSq(a.getKey())));

            int rendered = 0;
            for (Map.Entry<BlockPos, Block> e : list) {
                if (rendered++ >= MAX_ORES_TO_RENDER) break;
                BlockPos pos = e.getKey();
                Block b = e.getValue();
                setColorForOre(b);

                double d = player.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                float alpha = 1.0F - (float) (d / MAX_RENDER_DISTANCE) * 0.5F;
                AxisAlignedBB box = new AxisAlignedBB(pos).offset(-px, -py, -pz);
                RenderGlobal.drawSelectionBoundingBox(box, 1.0F, 1.0F, 1.0F, alpha);
            }

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        private static void setColorForOre(Block b) {
            if (b == Blocks.DIAMOND_ORE)       GlStateManager.color(0.2F, 1.0F, 1.0F, 0.8F);
            else if (b == Blocks.EMERALD_ORE)  GlStateManager.color(0.2F, 1.0F, 0.2F, 0.8F);
            else if (b == Blocks.GOLD_ORE)     GlStateManager.color(1.0F, 0.8F, 0.2F, 0.8F);
            else if (b == Blocks.IRON_ORE)     GlStateManager.color(0.8F, 0.5F, 0.3F, 0.8F);
            else if (b == Blocks.REDSTONE_ORE || b == Blocks.LIT_REDSTONE_ORE)
                GlStateManager.color(1.0F, 0.2F, 0.2F, 0.8F);
            else if (b == Blocks.LAPIS_ORE)    GlStateManager.color(0.2F, 0.2F, 1.0F, 0.8F);
            else if (b == Blocks.COAL_ORE)     GlStateManager.color(0.3F, 0.3F, 0.3F, 0.8F);
            else if (b == Blocks.QUARTZ_ORE)   GlStateManager.color(1.0F, 1.0F, 1.0F, 0.8F);
            else {
                int h = b.hashCode();
                float r = ((h & 0xFF) / 255.0F) * 0.5F + 0.5F;
                float g = (((h >> 8) & 0xFF) / 255.0F) * 0.5F + 0.5F;
                float bl = (((h >> 16) & 0xFF) / 255.0F) * 0.5F + 0.5F;
                GlStateManager.color(r, g, bl, 0.8F);
            }
        }

        public static void toggleOreVision(EntityPlayer p, boolean enable) {
            if (enable) {
                ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
                if (core.isEmpty()) {
                    p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "未装备机械核心"), true);
                    return;
                }
                if (!ItemMechanicalCore.isUpgradeEnabled(core, "ORE_VISION")
                        || !ItemMechanicalCore.consumeEnergyForUpgrade(core, "ORE_VISION", 100)) {
                    p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚡ 能量不足，矿物透视无法开启"), true);
                    return;
                }
                if (!initialized) initializeOreDictionary();
                int lvl = ItemMechanicalCore.getUpgradeLevel(core, "ORE_VISION");
                scanForOres(p, lvl, true);

                String filter = selectedOreIndex == -1 ? "全部" :
                        (selectedOreIndex < DISCOVERED_ORE_TYPES.size()
                                ? ORE_DISPLAY_NAMES.getOrDefault(DISCOVERED_ORE_TYPES.get(selectedOreIndex), "未知")
                                : "全部");
                p.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "⛏ 矿物透视已启动 - " + TextFormatting.AQUA + filter), true);

                renderingOres = true;
                wasActiveLastTick = true;
            } else {
                if (wasActiveLastTick) p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "矿物透视已关闭"), true);
                renderingOres = false;
                oreCache.clear();
                scannedChunks.clear();
                lastEnergyCheck = 0;
                wasActiveLastTick = false;
            }
        }

        public static void cycleOreCategory(EntityPlayer p) {
            if (!renderingOres) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "请先开启矿物透视"), true);
                return;
            }
            if (DISCOVERED_ORE_TYPES.isEmpty()) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "未发现矿物"), true);
                return;
            }
            selectedOreIndex++;
            if (selectedOreIndex >= DISCOVERED_ORE_TYPES.size()) selectedOreIndex = -1;

            String filterText;
            if (selectedOreIndex == -1) filterText = "全部 (" + DISCOVERED_ORE_TYPES.size() + " 种)";
            else {
                Block b = DISCOVERED_ORE_TYPES.get(selectedOreIndex);
                String name = ORE_DISPLAY_NAMES.getOrDefault(b, String.valueOf(b.getRegistryName()));
                filterText = name + " (" + (selectedOreIndex + 1) + "/" + DISCOVERED_ORE_TYPES.size() + ")";
            }
            p.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "⛏ 矿物过滤: " + TextFormatting.AQUA + filterText), true);
        }

        public static boolean isOreVisionActive() { return renderingOres; }

        @SideOnly(Side.CLIENT)
        public static void updateScan(EntityPlayer p, int level) {
            if (renderingOres && p.world.getTotalWorldTime() % 40 == 0) scanForOres(p, level, false);
        }

        public static void reset() {
            selectedOreIndex = -1;
            DISCOVERED_ORE_TYPES.clear();
            oreCache.clear();
            scannedChunks.clear();
            lastFullScanTime = 0;
            lastPlayerPos = BlockPos.ORIGIN;
            lastEnergyCheck = 0;
            renderingOres = false;
            wasActiveLastTick = false;
        }
    }

    // ========================= 移动速度 =========================
    public static class MovementSpeedSystem {
        private static final String NBT_SPEED_MODIFIER = "MechanicalCoreSpeed";
        private static final String NBT_SPEED_APPLIED = "MechanicalCoreSpeedApplied";
        private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8499b04-0e66-4726-ab29-64469d734e0d");
        private static final Map<UUID, Boolean> lastSpeedState = new HashMap<>();

        public static void updateSpeed(EntityPlayer p, ItemStack core) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, "MOVEMENT_SPEED");
            if (level <= 0) { resetSpeed(p); return; }

            if (!ItemMechanicalCore.isUpgradeEnabled(core, "MOVEMENT_SPEED")) { resetSpeed(p); return; }
            if (p.world.getTotalWorldTime() % 20 == 0) {
                if (!ItemMechanicalCore.consumeEnergyForUpgrade(core, "MOVEMENT_SPEED", 8 * level)) {
                    resetSpeed(p); return;
                }
            }

            UUID id = p.getUniqueID();
            Boolean last = lastSpeedState.get(id);
            if (last == null || !last) {
                p.getEntityData().setBoolean(NBT_SPEED_APPLIED, true);
                if (canSendMessage(p, "speed_activate"))
                    p.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "⚡ 移动加速已激活"), true);
                lastSpeedState.put(id, true);
            }

            float bonus = 0.2F * level;
            net.minecraft.entity.ai.attributes.IAttributeInstance attr =
                    p.getAttributeMap().getAttributeInstance(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);

            if (attr == null) return; // 极少数情况下可能为 null
            attr.removeModifier(SPEED_MODIFIER_UUID);
            net.minecraft.entity.ai.attributes.AttributeModifier mod =
                    new net.minecraft.entity.ai.attributes.AttributeModifier(SPEED_MODIFIER_UUID, "Mechanical Core Speed", bonus, 2);
            attr.applyModifier(mod);
            p.getEntityData().setInteger(NBT_SPEED_MODIFIER, level);
        }

        public static void resetSpeed(EntityPlayer p) {
            net.minecraft.entity.ai.attributes.IAttributeInstance attr =
                    p.getAttributeMap().getAttributeInstance(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);
            if (attr != null) attr.removeModifier(SPEED_MODIFIER_UUID);
            p.getEntityData().setInteger(NBT_SPEED_MODIFIER, 0);
            p.getEntityData().setBoolean(NBT_SPEED_APPLIED, false);
            lastSpeedState.put(p.getUniqueID(), false);
        }
    }

    // ========================= 隐身系统 (不朽护符同款逻辑) =========================
    public static class StealthSystem {
        private static final boolean DEBUG_MODE_LOCAL = false;

        private static final Map<UUID, Integer> stealthPlayers = new ConcurrentHashMap<>();
        private static final Map<UUID, Long> lastDisableMessageTime = new ConcurrentHashMap<>();
        private static final Map<UUID, Boolean> wasStealthActive = new ConcurrentHashMap<>();

        private static final Map<UUID, Long> stealthStartTime = new ConcurrentHashMap<>();
        private static final Map<UUID, Long> stealthCooldownEnd = new ConcurrentHashMap<>();
        private static final Map<UUID, Integer> consecutiveUses = new ConcurrentHashMap<>();

        private static final long[] DURATION_BY_LEVEL = { 30000L, 45000L, 60000L };
        private static final long[] COOLDOWN_BY_LEVEL = { 20000L, 30000L, 45000L };
        private static final float CONSECUTIVE_PENALTY = 1.5f;
        private static final long MESSAGE_COOLDOWN_LOCAL = 3000L;
        private static final long CONSECUTIVE_RESET_TIME = 120000L;

        // 清除仇恨范围 (按等级)
        private static final double[] CLEAR_AGGRO_RANGE = { 16.0, 24.0, 32.0 };

        /**
         * 阻止敌对生物锁定玩家 - 不朽护符同款逻辑
         * 当隐身激活时，怪物无法将玩家设为攻击目标
         */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
            // 只处理怪物锁定玩家的情况
            if (!(event.getTarget() instanceof EntityPlayer)) return;
            if (event.getEntityLiving() instanceof EntityPlayer) return;

            EntityPlayer targetPlayer = (EntityPlayer) event.getTarget();

            // 检查玩家是否有隐身效果
            if (!isStealthActive(targetPlayer)) return;

            EntityLivingBase attacker = event.getEntityLiving();

            if (attacker instanceof EntityLiving) {
                EntityLiving livingAttacker = (EntityLiving) attacker;

                // 清除攻击目标
                livingAttacker.setAttackTarget(null);

                // 使用假目标替换复仇目标，防止生物继续追踪
                if (targetPlayer.equals(attacker.getRevengeTarget())) {
                    attacker.setRevengeTarget(new DummyTarget(attacker.world));
                }

                // 清除导航路径
                if (livingAttacker.getNavigator() != null) {
                    livingAttacker.getNavigator().clearPath();
                }

                // 让生物看向随机方向
                livingAttacker.getLookHelper().setLookPosition(
                        livingAttacker.posX + livingAttacker.world.rand.nextGaussian() * 10,
                        livingAttacker.posY,
                        livingAttacker.posZ + livingAttacker.world.rand.nextGaussian() * 10,
                        10.0F, 10.0F
                );
            }
        }

        /**
         * 清除玩家周围所有生物的仇恨
         */
        public static int clearNearbyAggro(EntityPlayer player, double range) {
            if (player.world.isRemote) return 0;

            AxisAlignedBB aabb = new AxisAlignedBB(
                    player.posX - range, player.posY - range, player.posZ - range,
                    player.posX + range, player.posY + range, player.posZ + range
            );

            List<EntityLiving> mobs = player.world.getEntitiesWithinAABB(EntityLiving.class, aabb);
            int affected = 0;

            DummyTarget dummyTarget = new DummyTarget(player.world);

            for (EntityLiving mob : mobs) {
                // 跳过玩家自身

                // 检查是否在圆形范围内
                double distSq = mob.getDistanceSq(player.posX, player.posY, player.posZ);
                if (distSq > range * range) continue;

                // 清除所有仇恨相关
                if (mob.getAttackTarget() != null) {
                    mob.setAttackTarget(null);
                    affected++;
                }

                // 使用假目标替换复仇目标
                mob.setRevengeTarget(dummyTarget);
                mob.setLastAttackedEntity(null);

                // 清除导航路径
                if (mob.getNavigator() != null) {
                    mob.getNavigator().clearPath();
                }

                // 让生物看向随机方向
                mob.getLookHelper().setLookPosition(
                        mob.posX + mob.world.rand.nextGaussian() * 10,
                        mob.posY,
                        mob.posZ + mob.world.rand.nextGaussian() * 10,
                        10.0F, 10.0F
                );
            }

            return affected;
        }

        public static void updateStealth(EntityPlayer p, ItemStack core) {
            try {
                if (!isStealthActive(p)) return;
                if (p == null || p.isDead || p.world == null) return;

                UUID id = p.getUniqueID();
                int level = getStealthLevel(p);

                Long start = stealthStartTime.get(id);
                if (start != null) {
                    long now = System.currentTimeMillis();
                    long duration = DURATION_BY_LEVEL[Math.min(level - 1, 2)];
                    long elapsed = now - start;

                    if (p.world.getTotalWorldTime() % 20 == 0) {
                        long remain = duration - elapsed;
                        if (remain > 0 && remain <= 10000) {
                            int sec = (int) (remain / 1000);
                            TextFormatting color = sec <= 5 ? TextFormatting.RED : TextFormatting.YELLOW;
                            p.sendStatusMessage(new TextComponentString(color + "⏱ 隐身剩余: " + sec + "秒"), true);
                        }
                    }
                    if (elapsed >= duration) {
                        disableStealthWithCooldown(p, "持续时间结束");
                        return;
                    }
                }

                int uses = consecutiveUses.getOrDefault(id, 0);
                int baseCost = 50 - level * 10;
                int energyCost = baseCost + (uses * 10);
                if (!ItemMechanicalCore.consumeEnergyForUpgrade(core, "STEALTH", energyCost)) {
                    disableStealthWithCooldown(p, "能量耗尽");
                    return;
                }

                // 维持效果（每秒）
                if (p.world.getTotalWorldTime() % 20 == 0) {
                    maintainStealthEffects(p, level);
                    // 每秒清除一次周围仇恨
                    double range = CLEAR_AGGRO_RANGE[Math.min(level - 1, 2)];
                    clearNearbyAggro(p, range);
                }

                // 高等级粒子
                if (level >= 3 && p.world.getTotalWorldTime() % 10 == 0 && !p.world.isRemote) spawnStealthParticles(p);

            } catch (Throwable t) { if (DEBUG_MODE_LOCAL) t.printStackTrace(); }
        }

        private static void disableStealthWithCooldown(EntityPlayer p, String reason) {
            if (p == null) return;
            UUID id = p.getUniqueID();
            Integer lvl = stealthPlayers.get(id);
            if (lvl == null) lvl = 1;

            long baseCd = COOLDOWN_BY_LEVEL[Math.min(lvl - 1, 2)];
            int uses = consecutiveUses.getOrDefault(id, 0);
            long cd = (long) (baseCd * Math.pow(CONSECUTIVE_PENALTY, uses));
            stealthCooldownEnd.put(id, System.currentTimeMillis() + cd);
            consecutiveUses.put(id, uses + 1);

            internalDisableStealth(p, true);
            int sec = (int) (cd / 1000);
            String msg = String.format("%s隐身已关闭 (%s) - 冷却: %d秒", TextFormatting.GRAY, reason, sec);
            if (uses > 0) msg += String.format(" %s(连续使用×%d)", TextFormatting.YELLOW, uses + 1);
            p.sendStatusMessage(new TextComponentString(msg), true);
            stealthStartTime.remove(id);
        }

        public static void disableStealth(EntityPlayer p) {
            if (p == null) return;
            UUID id = p.getUniqueID();
            Long lastMsg = lastDisableMessageTime.get(id);
            long now = System.currentTimeMillis();
            boolean show = lastMsg == null || now - lastMsg >= 1000;
            if (show) lastDisableMessageTime.put(id, now);

            Integer lvl = stealthPlayers.get(id);
            if (lvl != null) {
                long baseCd = COOLDOWN_BY_LEVEL[Math.min(lvl - 1, 2)] / 2;
                stealthCooldownEnd.put(id, System.currentTimeMillis() + baseCd);
                if (show) p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "隐身已关闭 - 冷却: " + (baseCd / 1000) + "秒"), true);
            }
            internalDisableStealth(p, false);
            wasStealthActive.put(id, false);
            stealthStartTime.remove(id);
        }

        private static void internalDisableStealth(EntityPlayer p, boolean showMessage) {
            if (p == null) return;
            UUID id = p.getUniqueID();
            Integer level = stealthPlayers.remove(id);

            p.getEntityData().setBoolean("MechanicalCoreStealthActive", false);
            p.getEntityData().removeTag("MechanicalCoreStealthLevel");
            p.setInvisible(false);
            p.setSilent(false);
            p.removePotionEffect(MobEffects.INVISIBILITY);

            if (showMessage && level != null && level > 0)
                p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "隐身已关闭"), true);
        }

        public static boolean isInCooldown(EntityPlayer p) {
            if (p == null) return false;
            Long end = stealthCooldownEnd.get(p.getUniqueID());
            if (end == null) return false;
            long now = System.currentTimeMillis();
            if (now >= end) {
                Long lastUse = stealthStartTime.get(p.getUniqueID());
                if (lastUse == null || now - end > CONSECUTIVE_RESET_TIME) {
                    consecutiveUses.remove(p.getUniqueID());
                }
                stealthCooldownEnd.remove(p.getUniqueID());
                return false;
            }
            return true;
        }

        public static int getRemainingCooldown(EntityPlayer p) {
            Long end = stealthCooldownEnd.get(p.getUniqueID());
            if (end == null) return 0;
            long r = end - System.currentTimeMillis();
            return r > 0 ? (int) (r / 1000) : 0;
        }

        public static void enableStealth(EntityPlayer p, int level) {
            if (p == null) return;
            if (isInCooldown(p)) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "隐身冷却中... 剩余 " + getRemainingCooldown(p) + " 秒"), true);
                return;
            }
            level = Math.min(level, 3);
            UUID id = p.getUniqueID();

            stealthPlayers.put(id, level);
            wasStealthActive.put(id, true);
            stealthStartTime.put(id, System.currentTimeMillis());

            p.getEntityData().setBoolean("MechanicalCoreStealthActive", true);
            p.getEntityData().setInteger("MechanicalCoreStealthLevel", level);
            p.setInvisible(true);
            if (level >= 2) p.setSilent(true);
            p.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

            // 激活时立即清除周围仇恨
            double range = CLEAR_AGGRO_RANGE[Math.min(level - 1, 2)];
            int cleared = clearNearbyAggro(p, range);

            long dur = DURATION_BY_LEVEL[Math.min(level - 1, 2)];
            int sec = (int) (dur / 1000);
            String msg = getStealthMessage(level) + String.format(" %s(持续%d秒)", TextFormatting.WHITE, sec);
            if (cleared > 0) msg += String.format(" %s清除%d个仇恨", TextFormatting.AQUA, cleared);
            int uses = consecutiveUses.getOrDefault(id, 0);
            if (uses > 0) msg += String.format(" %s连续×%d", TextFormatting.YELLOW, uses + 1);
            p.sendStatusMessage(new TextComponentString(msg), true);
        }

        public static void toggleStealth(EntityPlayer p, boolean enable) {
            if (p == null) return;
            if (enable) {
                if (isStealthActive(p)) {
                    Long start = stealthStartTime.get(p.getUniqueID());
                    if (start != null) {
                        int level = getStealthLevel(p);
                        long dur = DURATION_BY_LEVEL[Math.min(level - 1, 2)];
                        long remain = dur - (System.currentTimeMillis() - start);
                        if (remain > 0) p.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "隐身剩余: " + (remain / 1000) + "秒"), true);
                    }
                    return;
                }
                if (isInCooldown(p)) {
                    p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⏱ 隐身冷却中... 剩余 " + getRemainingCooldown(p) + " 秒"), true);
                    return;
                }
                ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
                if (core.isEmpty()) { p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "未装备机械核心"), true); return; }
                int level = ItemMechanicalCore.getUpgradeLevel(core, "STEALTH");
                if (level <= 0) { p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "未安装隐身升级"), true); return; }
                if (!ItemMechanicalCore.isUpgradeActive(core, "STEALTH")) {
                    p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚡ 能量不足，隐身无法开启"), true); return;
                }
                enableStealth(p, level);
            } else {
                if (!isStealthActive(p)) return;
                disableStealth(p);
            }
        }

        public static void toggle(EntityPlayer p) { toggleStealth(p, !isStealthActive(p)); }
        public static boolean isStealthActive(EntityPlayer p) { return stealthPlayers.getOrDefault(p.getUniqueID(), 0) > 0; }
        public static int getStealthLevel(EntityPlayer p) { return stealthPlayers.getOrDefault(p.getUniqueID(), 0); }

        private static void maintainStealthEffects(EntityPlayer p, int level) {
            p.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 100, 0, false, false));
            if (level >= 2) p.setSilent(true);
            if (level >= 3) p.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 1, false, false));
        }

        private static void spawnStealthParticles(EntityPlayer p) {
            for (int i = 0; i < 3; i++) {
                p.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        p.posX + (Math.random() - 0.5) * 0.5,
                        p.posY + Math.random() * 2,
                        p.posZ + (Math.random() - 0.5) * 0.5, 0, 0.01, 0);
            }
        }

        private static String getStealthMessage(int level) {
            switch (level) {
                case 1: return TextFormatting.GRAY + "👤 基础隐身已激活";
                case 2: return TextFormatting.DARK_GRAY + "🌫 高级隐身已激活";
                case 3: return TextFormatting.DARK_PURPLE + "👻 完美隐身已激活";
                default: return TextFormatting.GRAY + "隐身已激活";
            }
        }

        public static String getStealthStatusInfo(EntityPlayer p) {
            if (isStealthActive(p)) {
                int l = getStealthLevel(p);
                Long st = stealthStartTime.get(p.getUniqueID());
                if (st != null) {
                    long dur = DURATION_BY_LEVEL[Math.min(l - 1, 2)];
                    long rem = dur - (System.currentTimeMillis() - st);
                    if (rem > 0) return "激活 L" + l + " (剩余" + (rem / 1000) + "秒)";
                }
                return "激活 L" + l;
            } else if (isInCooldown(p)) {
                int remain = getRemainingCooldown(p);
                int uses = consecutiveUses.getOrDefault(p.getUniqueID(), 0);
                return "冷却中 (" + remain + "秒) 连续×" + uses;
            } else return "就绪";
        }

        public static void resetAll() {
            stealthPlayers.clear();
            lastDisableMessageTime.clear();
            wasStealthActive.clear();
            stealthStartTime.clear();
            stealthCooldownEnd.clear();
            consecutiveUses.clear();
        }

        /**
         * 假目标实体类 - 用于迷惑敌对生物 (不朽护符同款)
         */
        public static class DummyTarget extends EntityLivingBase {
            public DummyTarget(World world) {
                super(world);
                this.setInvisible(true);
                this.setSize(0.0F, 0.0F);
                this.setEntityInvulnerable(true);
                this.isDead = true;
            }

            @Override
            public void onUpdate() {
                this.isDead = true;
            }

            @Override
            public boolean isEntityAlive() {
                return false;
            }

            @Override
            public boolean canBeCollidedWith() {
                return false;
            }

            @Override
            public boolean canBePushed() {
                return false;
            }

            @Override
            public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
                return ItemStack.EMPTY;
            }

            @Override
            public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
            }

            @Override
            public Iterable<ItemStack> getArmorInventoryList() {
                return java.util.Collections.emptyList();
            }

            @Override
            public EnumHandSide getPrimaryHand() {
                return EnumHandSide.RIGHT;
            }
        }
    }

    // ========================= 经验增幅 =========================
    // ========================= 经验增幅 =========================
    public static class ExpAmplifierSystem {
        private static final Map<UUID, Long> lastKillTime = new HashMap<>();
        private static final Map<UUID, Integer> killCombo = new HashMap<>();
        private static final Set<Integer> processingEntities = new HashSet<>();
        private static final Map<UUID, Long> lastEnchantBonusCheck = new HashMap<>();

        private static final long COMBO_TIMEOUT = 5000L;
        private static final long ENCHANT_BONUS_CHECK_COOLDOWN_MS = 250L;
        private static final String BONUS_ORB_TAG = "MechanicalCoreBonusOrb";

        /**
         * 杀怪时增加额外经验（生成附加经验球）
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onEntityDeath(LivingDeathEvent event) {
            if (event.getEntity().world.isRemote) return;
            if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

            int entityId = event.getEntity().getEntityId();
            if (!processingEntities.add(entityId)) return; // 防重复

            try {
                EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
                EntityLivingBase entity = event.getEntityLiving();

                ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
                if (core.isEmpty()) return;

                int level = ItemMechanicalCore.getUpgradeLevel(core, "EXP_AMPLIFIER");
                if (level <= 0) return;
                if (!ItemMechanicalCore.isUpgradeEnabled(core, "EXP_AMPLIFIER")) return;

                int baseExp = computeBaseExperience(entity);
                if (baseExp <= 0) return;

                int energyCost = Math.max(10, baseExp * 3);
                if (!ItemMechanicalCore.consumeEnergyForUpgrade(core, "EXP_AMPLIFIER", energyCost)) return;

                // 连杀系统
                UUID pid = player.getUniqueID();
                long now = System.currentTimeMillis();
                Long last = lastKillTime.get(pid);
                int combo = 0;
                if (last != null && now - last < COMBO_TIMEOUT) {
                    combo = killCombo.getOrDefault(pid, 0) + 1;
                    killCombo.put(pid, Math.min(combo, 10));
                } else {
                    killCombo.put(pid, 0);
                }
                lastKillTime.put(pid, now);

                float baseMultiplier = 1.0F + (0.5F * level);
                float comboBonus = combo * 0.1F;
                float totalMultiplier = baseMultiplier + comboBonus;

                int bonusExp = (int) (baseExp * (totalMultiplier - 1.0F));
                if (bonusExp > 0) {
                    spawnBonusExperience(player.world, entity.posX, entity.posY, entity.posZ, bonusExp);
                    showKillBonusEffect(player, entity, baseExp, bonusExp, level, combo);
                }
            } finally {
                processingEntities.remove(entityId);
            }
        }

        private static int computeBaseExperience(EntityLivingBase e) {
            if (e instanceof EntityMob) {
                if (e instanceof net.minecraft.entity.boss.EntityWither ||
                        e instanceof net.minecraft.entity.boss.EntityDragon) return 50;
                if (e instanceof net.minecraft.entity.monster.EntityEnderman ||
                        e instanceof net.minecraft.entity.monster.EntityCreeper ||
                        e instanceof net.minecraft.entity.monster.EntityWitch ||
                        e instanceof net.minecraft.entity.monster.EntityBlaze) return 10;
                return 5;
            } else if (e instanceof EntityAnimal) {
                return 1;
            } else if (e instanceof EntityVillager) {
                return 0;
            }
            return 0;
        }

        private static void spawnBonusExperience(World w, double x, double y, double z, int total) {
            while (total > 0) {
                int orb = Math.min(total, 10);
                EntityXPOrb xp = new EntityXPOrb(w, x, y, z, orb);
                xp.getEntityData().setBoolean(BONUS_ORB_TAG, true);
                xp.motionX = (w.rand.nextDouble() - 0.5) * 0.2;
                xp.motionY = 0.3 + w.rand.nextDouble() * 0.2;
                xp.motionZ = (w.rand.nextDouble() - 0.5) * 0.2;
                w.spawnEntity(xp);
                total -= orb;
            }
        }

        private static void showKillBonusEffect(EntityPlayer p, EntityLivingBase e, int base, int bonus, int level, int combo) {
            StringBuilder msg = new StringBuilder();
            msg.append(TextFormatting.GOLD).append("⚔ 击杀奖励 +").append(bonus).append(" EXP");
            if (combo > 0) {
                msg.append(TextFormatting.LIGHT_PURPLE).append(" 连杀x").append(combo + 1);
            }
            p.sendStatusMessage(new TextComponentString(msg.toString()), true);

            float pitch = 1.0F + (combo * 0.1F);
            p.world.playSound(null, p.posX, p.posY, p.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, pitch);
        }

        /**
         * 拾取经验球时增幅 - 修复版本
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onPlayerPickupXp(PlayerPickupXpEvent event) {
            if (event.isCanceled()) return;

            EntityPlayer p = event.getEntityPlayer();
            EntityXPOrb orb = event.getOrb();

            // 跳过我们生成的奖励球，避免双重增幅
            if (orb.getEntityData().getBoolean(BONUS_ORB_TAG)) return;

            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
            if (core.isEmpty()) return;

            int level = ItemMechanicalCore.getUpgradeLevel(core, "EXP_AMPLIFIER");
            if (level <= 0) return;

            if (!ItemMechanicalCore.isUpgradeEnabled(core, "EXP_AMPLIFIER")) return;

            // 能量消耗
            int cost = Math.max(5, orb.getXpValue() * 2);
            if (!ItemMechanicalCore.consumeEnergyForUpgrade(core, "EXP_AMPLIFIER", cost)) return;

            // 计算增幅
            float multiplier = 1.0F + (0.5F * level);
            int original = orb.getXpValue();
            int newValue = (int) (original * multiplier);

            // 直接修改经验球的值，让原版系统处理
            orb.xpValue = newValue;

            // 显示提示（降低频率避免刷屏）
            if (p.world.getTotalWorldTime() % 20 == 0) {
                int bonus = newValue - original;
                p.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✨ 经验增幅 +" + bonus +
                                TextFormatting.AQUA + " [x" + String.format(Locale.ROOT, "%.1f", multiplier) + "]"
                ), true);
            }

            // 粒子效果
            if (!p.world.isRemote) {
                for (int i = 0; i < 3; i++) {
                    p.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                            p.posX + (Math.random() - 0.5),
                            p.posY + 1.0 + Math.random(),
                            p.posZ + (Math.random() - 0.5),
                            0, 0.1, 0);
                }
            }
        }

        /**
         * 提供给附魔事件的等级加成
         */
        public static int getEnchantmentBonus(EntityPlayer player) {
            if (player == null) return 0;

            long now = System.currentTimeMillis();
            Long last = lastEnchantBonusCheck.get(player.getUniqueID());
            if (last == null || now - last >= ENCHANT_BONUS_CHECK_COOLDOWN_MS) {
                lastEnchantBonusCheck.put(player.getUniqueID(), now);
            }

            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
            if (core.isEmpty()) return 0;

            int level = ItemMechanicalCore.getUpgradeLevel(core, "EXP_AMPLIFIER");
            if (level <= 0) return 0;

            if (!ItemMechanicalCore.isUpgradeEnabled(core, "EXP_AMPLIFIER")) return 0;

            switch (level) {
                case 1: return 5;
                case 2: return 10;
                case 3: return 15;
                default: return 0;
            }
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.world.getTotalWorldTime() % 200 != 0) return;

            long now = System.currentTimeMillis();
            lastKillTime.entrySet().removeIf(e -> now - e.getValue() > COMBO_TIMEOUT * 2);
            killCombo.keySet().removeIf(id -> !lastKillTime.containsKey(id));
            processingEntities.clear();
        }

        public static void resetPlayerCombo(EntityPlayer p) {
            UUID id = p.getUniqueID();
            lastKillTime.remove(id);
            killCombo.remove(id);
        }

        public static int getPlayerCombo(EntityPlayer p) {
            return killCombo.getOrDefault(p.getUniqueID(), 0);
        }
    }
    // ========================= Tick 与调试 =========================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer p = event.player;
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
        if (core.isEmpty()) return;

        IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
        if (storage == null) return;

        int cur = storage.getEnergyStored();
        int max = storage.getMaxEnergyStored();
        if (max == 0) return;
        float pct = (float) cur / max;

        if (pct < 0.03f) {
            MovementSpeedSystem.resetSpeed(p);
            StealthSystem.disableStealth(p);
            if (p.world.isRemote) OreVisionSystem.reset();
            if (canSendMessage(p, "critical_energy")) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.DARK_RED + "⚠ 能量危急！所有系统关闭 (" +
                        String.format(Locale.ROOT, "%.1f%%", pct * 100) + ")"), true);
            }
        } else if (pct < 0.05f) {
            MovementSpeedSystem.updateSpeed(p, core);
            StealthSystem.disableStealth(p);
            if (p.world.isRemote) OreVisionSystem.reset();
            if (canSendMessage(p, "emergency_energy")) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚡ 能量紧急，仅保留移动系统 (" +
                        String.format(Locale.ROOT, "%.1f%%", pct * 100) + ")"), true);
            }
        } else if (pct < 0.15f) {
            MovementSpeedSystem.updateSpeed(p, core);
            if (StealthSystem.isStealthActive(p)) StealthSystem.disableStealth(p);
            if (p.world.isRemote && OreVisionSystem.isOreVisionActive()) OreVisionSystem.reset();
            if (canSendMessage(p, "power_saving")) {
                p.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "⚡ 省电模式，高耗能功能已禁用 (" +
                        String.format(Locale.ROOT, "%.1f%%", pct * 100) + ")"), true);
            }
        } else {
            MovementSpeedSystem.updateSpeed(p, core);
            StealthSystem.updateStealth(p, core);
            if (p.world.isRemote) {
                int oreLevel = ItemMechanicalCore.getUpgradeLevel(core, "ORE_VISION");
                if (oreLevel > 0) OreVisionSystem.updateScan(p, oreLevel);
            }
        }
    }

    public static class DebugHelper {
        public static String getSystemStatus(EntityPlayer p) {
            StringBuilder sb = new StringBuilder();
            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
            if (core.isEmpty()) return "未装备机械核心";

            sb.append("=== 系统状态 ===\n");
            IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
            if (storage != null) {
                int cur = storage.getEnergyStored(), max = storage.getMaxEnergyStored();
                float per = max == 0 ? 0 : (cur * 100f / max);
                sb.append(String.format(Locale.ROOT, "能量: %d/%d (%.1f%%)\n", cur, max, per));
                if (per < 3) sb.append("状态: 危急模式\n");
                else if (per < 5) sb.append("状态: 紧急模式\n");
                else if (per < 15) sb.append("状态: 省电模式\n");
                else sb.append("状态: 正常模式\n");
            }
            int stealthLevel = ItemMechanicalCore.getUpgradeLevel(core, "STEALTH");
            boolean stealthActive = StealthSystem.isStealthActive(p);
            sb.append("隐身: 等级").append(stealthLevel).append(" 状态:").append(stealthActive ? "激活" : "关闭").append("\n");

            int speedLevel = ItemMechanicalCore.getUpgradeLevel(core, "MOVEMENT_SPEED");
            boolean speedActive = p.getEntityData().getBoolean("MechanicalCoreSpeedApplied");
            sb.append("速度: 等级").append(speedLevel).append(" 状态:").append(speedActive ? "激活" : "关闭").append("\n");

            int oreLevel = ItemMechanicalCore.getUpgradeLevel(core, "ORE_VISION");
            boolean oreActive = OreVisionSystem.isOreVisionActive();
            sb.append("矿物透视: 等级").append(oreLevel).append(" 状态:").append(oreActive ? "激活" : "关闭");
            if (oreActive) sb.append(" (缓存:").append(OreVisionSystem.oreCache.size()).append("个矿物)");
            sb.append("\n");

            int expLevel = ItemMechanicalCore.getUpgradeLevel(core, "EXP_AMPLIFIER");
            sb.append("经验增幅: 等级").append(expLevel).append("\n");
            return sb.toString();
        }

        public static void toggleDebugMode(EntityPlayer p) {
            DEBUG_MODE = !DEBUG_MODE;
            p.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "调试模式: " + (DEBUG_MODE ? "开启" : "关闭")), true);
            if (DEBUG_MODE) p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "调试信息将输出到控制台"), false);
        }

        public static void forceRefreshOreVision(EntityPlayer p) {
            if (OreVisionSystem.isOreVisionActive()) {
                OreVisionSystem.reset();
                ItemStack core = ItemMechanicalCore.getCoreFromPlayer(p);
                int lvl = ItemMechanicalCore.getUpgradeLevel(core, "ORE_VISION");
                if (lvl > 0) OreVisionSystem.scanForOres(p, lvl, true);
                p.sendStatusMessage(new TextComponentString(TextFormatting.GREEN + "矿物透视缓存已刷新"), true);
            } else {
                p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "矿物透视未激活"), true);
            }
        }

        public static void resetAllSystems(EntityPlayer p) {
            MovementSpeedSystem.resetSpeed(p);
            StealthSystem.resetAll();
            OreVisionSystem.reset();
            ExpAmplifierSystem.resetPlayerCombo(p);
            p.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "所有系统已重置"), true);
        }
    }
}
