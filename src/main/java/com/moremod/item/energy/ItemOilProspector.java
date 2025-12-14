package com.moremod.item.energy;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.world.OilExtractionData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 石油探勘工具
 *
 * 功能：
 * - 右鍵使用檢測當前區塊是否有石油礦脈
 * - 顯示石油儲量和深度
 * - 石油礦脈基於區塊種子生成（一致性）
 */
public class ItemOilProspector extends Item {

    // 石油生成配置
    private static final double OIL_CHANCE = 0.15;        // 15% 區塊有石油
    private static final int MIN_OIL_AMOUNT = 60000;      // 最小儲量 60k mB (6倍)
    private static final int MAX_OIL_AMOUNT = 3000000;    // 最大儲量 3M mB (6倍)
    private static final int MIN_DEPTH = 10;              // 最淺深度 Y=10
    private static final int MAX_DEPTH = 40;              // 最深深度 Y=40
    private static final int SCAN_COOLDOWN = 100;         // 5秒冷卻

    public ItemOilProspector() {
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("oil_prospector");
        setTranslationKey("oil_prospector");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 檢查冷卻
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        long lastUse = nbt.getLong("LastUse");
        long currentTime = world.getTotalWorldTime();

        if (currentTime - lastUse < SCAN_COOLDOWN) {
            int remaining = (int)((SCAN_COOLDOWN - (currentTime - lastUse)) / 20);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "探勘冷卻中... " + remaining + "秒"
            ), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        nbt.setLong("LastUse", currentTime);

        // 執行探勘
        performProspecting(world, player);

        // 播放音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.PLAYERS, 1.0F, 1.0F);

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void performProspecting(World world, EntityPlayer player) {
        BlockPos pos = player.getPosition();
        ChunkPos chunkPos = new ChunkPos(pos);

        // 基於區塊和世界種子計算石油
        OilVeinData oilData = getOilVeinData(world, chunkPos);

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 石油探勘報告 ==="
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "區塊: " + chunkPos.x + ", " + chunkPos.z
        ));

        if (oilData.hasOil) {
            // 獲取已提取量，計算剩餘量
            OilExtractionData extractionData = OilExtractionData.get(world);
            int extracted = extractionData.getExtractedAmount(chunkPos);
            int remaining = Math.max(0, oilData.amount - extracted);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 發現石油礦脈！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "總儲量: " + formatAmount(oilData.amount) + " mB"
            ));

            // 顯示剩餘量
            if (extracted > 0) {
                if (remaining > 0) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "剩餘可採: " + formatAmount(remaining) + " mB (" +
                            (remaining * 100 / oilData.amount) + "%)"
                    ));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 此區塊石油已被開採完畢！"
                    ));
                }
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "深度: Y=" + oilData.depth
            ));

            if (remaining > 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "在此區塊建造抽油機即可開採"
                ));
            }

            // 檢查周圍區塊
            int nearbyOilChunks = countNearbyOilChunks(world, chunkPos);
            if (nearbyOilChunks > 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "附近還有 " + nearbyOilChunks + " 個區塊有石油！"
                ));
            }
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 此區塊沒有石油礦脈"
            ));

            // 提示最近的石油區塊
            ChunkPos nearest = findNearestOilChunk(world, chunkPos, 8);
            if (nearest != null) {
                int dx = (nearest.x - chunkPos.x) * 16;
                int dz = (nearest.z - chunkPos.z) * 16;
                String direction = getDirectionString(dx, dz);
                int distance = (int) Math.sqrt(dx * dx + dz * dz);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "最近的石油: " + direction + " 約 " + distance + " 格"
                ));
            }
        }
    }

    /**
     * 獲取區塊的石油礦脈數據
     * 基於世界種子和區塊位置計算，保證一致性
     */
    public static OilVeinData getOilVeinData(World world, ChunkPos chunkPos) {
        long seed = world.getSeed();
        Random rand = new Random(seed ^ ((long) chunkPos.x * 341873128712L + (long) chunkPos.z * 132897987541L));

        boolean hasOil = rand.nextDouble() < OIL_CHANCE;

        if (!hasOil) {
            return new OilVeinData(false, 0, 0);
        }

        int amount = MIN_OIL_AMOUNT + rand.nextInt(MAX_OIL_AMOUNT - MIN_OIL_AMOUNT);
        int depth = MIN_DEPTH + rand.nextInt(MAX_DEPTH - MIN_DEPTH);

        return new OilVeinData(true, amount, depth);
    }

    private int countNearbyOilChunks(World world, ChunkPos center) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos nearby = new ChunkPos(center.x + dx, center.z + dz);
                if (getOilVeinData(world, nearby).hasOil) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    private ChunkPos findNearestOilChunk(World world, ChunkPos center, int radius) {
        ChunkPos nearest = null;
        double minDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos check = new ChunkPos(center.x + dx, center.z + dz);
                if (getOilVeinData(world, check).hasOil) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = check;
                    }
                }
            }
        }

        return nearest;
    }

    private String getDirectionString(int dx, int dz) {
        StringBuilder sb = new StringBuilder();
        if (dz < 0) sb.append("北");
        else if (dz > 0) sb.append("南");
        if (dx > 0) sb.append("東");
        else if (dx < 0) sb.append("西");
        return sb.length() > 0 ? sb.toString() : "這裡";
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + "右鍵使用探測石油礦脈");
        tooltip.add(TextFormatting.YELLOW + "顯示區塊石油儲量和深度");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "石油可用於發電或合成");
    }

    /**
     * 石油礦脈數據
     */
    public static class OilVeinData {
        public final boolean hasOil;
        public final int amount;      // mB
        public final int depth;       // Y level

        public OilVeinData(boolean hasOil, int amount, int depth) {
            this.hasOil = hasOil;
            this.amount = amount;
            this.depth = depth;
        }
    }
}
