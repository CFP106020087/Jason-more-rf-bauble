package com.moremod.item.battery;

import com.moremod.network.QuantumEnergyNetwork;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 量子电池：
 * - Shift+右键任意 FE 方块建立链接
 * - Shift+空挥右键清除链接
 * - 背包/饰品内每 10t 远程拉电（跨维 90%效率），可选回充
 * - Tooltip 显示链接信息 + 远端当前能量
 */
public class ItemBatteryQuantum extends ItemBatteryBase {

    private static final int CAP   = 100_000_000;  // 100M
    private static final int IN    = 1_000_000;    // 1M/t
    private static final int OUT   = 1_000_000;    // 1M/t
    private static final int PULL  = 100_000;      // 量子侧每次拉电上限
    private static final int TICKI = 10;           // 每10t执行
    private static final float EFF = 0.90f;        // 跨维效率

    private static final boolean DEBUG = false;

    public ItemBatteryQuantum() {
        super("battery_quantum", 5, CAP, OUT, IN, "量子", TextFormatting.DARK_PURPLE);
    }

    // ========== 绑定/解绑 ==========
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) return EnumActionResult.PASS;

        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "✘ 此处没有有效方块"));
            return EnumActionResult.FAIL;
        }
        // 需要支持 FE
        IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, null);
        if (es == null) {
            for (EnumFacing f : EnumFacing.VALUES) {
                es = te.getCapability(CapabilityEnergy.ENERGY, f);
                if (es != null) break;
            }
        }
        if (es == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "✘ 该方块不支持能量能力（FE）"));
            return EnumActionResult.FAIL;
        }

        // 建立量子链接
        UUID netId = UUID.randomUUID();
        QuantumEnergyNetwork.registerDirectLink(netId, pos, world.provider.getDimension());

        NBTTagCompound q = stack.getOrCreateSubCompound("Quantum");
        q.setString("NetworkId", netId.toString());
        q.setInteger("X", pos.getX());
        q.setInteger("Y", pos.getY());
        q.setInteger("Z", pos.getZ());
        q.setInteger("Dim", world.provider.getDimension());
        q.setBoolean("Bidirectional", false); // 需要可回充就改 true

        String blockName = world.getBlockState(pos).getBlock().getLocalizedName();
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✔ 量子链接建立成功！\n" +
                        TextFormatting.YELLOW + "绑定方块: " + TextFormatting.WHITE + blockName + "\n" +
                        TextFormatting.YELLOW + "位置: " + TextFormatting.WHITE +
                        String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()) + "\n" +
                        TextFormatting.YELLOW + "维度: " + TextFormatting.WHITE + dimName(world.provider.getDimension())
        ));
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            if (player.isSneaking()) {
                if (hasLink(stack)) {
                    clearLink(stack);
                    player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "⚡ 量子链接已断开"));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.GRAY + "未建立量子链接"));
                }
            } else {
                // 基础电量
                super.onItemRightClick(world, player, hand);
                // 网络状态
                if (hasLink(stack)) {
                    UUID id = UUID.fromString(stack.getSubCompound("Quantum").getString("NetworkId"));
                    int net = QuantumEnergyNetwork.getNetworkPower(id);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "远端能量: " + TextFormatting.WHITE + (net >= 0 ? fmt(net) + " RF" : "未加载")));
                }
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private static boolean hasLink(ItemStack stack) {
        return stack.hasTagCompound()
                && stack.getTagCompound().hasKey("Quantum")
                && stack.getTagCompound().getCompoundTag("Quantum").hasKey("NetworkId");
    }

    private static void clearLink(ItemStack stack) {
        if (stack.hasTagCompound()) stack.getTagCompound().removeTag("Quantum");
    }

    // ========== 被动逻辑：量子取电/回充 ==========
    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        if (!hasLink(stack)) return;
        if (player.world.getTotalWorldTime() % TICKI != 0) return;

        NBTTagCompound q = stack.getSubCompound("Quantum");
        UUID netId = UUID.fromString(q.getString("NetworkId"));
        boolean bi = q.getBoolean("Bidirectional");

        IEnergyStorage bat = getEnergy(stack);
        if (bat == null) return;

        int space = bat.getMaxEnergyStored() - bat.getEnergyStored();
        if (space > 0) {
            int req = Math.min(space, PULL);
            int pulled = QuantumEnergyNetwork.requestEnergy(netId, req, player.dimension, EFF);
            if (pulled > 0) {
                bat.receiveEnergy(pulled, false);
                if (DEBUG) System.out.println("[QuantumBattery] pulled " + pulled);
            }
        } else if (bi && bat.getEnergyStored() > 0) {
            // 反向回充
            int send = Math.min(bat.getEnergyStored(), PULL);
            int sent = QuantumEnergyNetwork.sendEnergy(netId, send, player.dimension, EFF);
            if (sent > 0) bat.extractEnergy(sent, false);
        }
    }

    // ========== Tooltip 追加量子信息 ==========
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        super.addInformation(stack, world, tip, flag);
        if (!hasLink(stack)) {
            tip.add("");
            tip.add(TextFormatting.YELLOW + "Shift + 右键任意 FE 方块以建立量子链接");
            tip.add(TextFormatting.DARK_GRAY + "（发电机 / 电池 / 机器均可）");
            return;
        }
        NBTTagCompound q = stack.getSubCompound("Quantum");
        BlockPos p = new BlockPos(q.getInteger("X"), q.getInteger("Y"), q.getInteger("Z"));
        int dim = q.getInteger("Dim");
        tip.add("");
        tip.add(TextFormatting.LIGHT_PURPLE + "量子链接");
        tip.add(TextFormatting.GRAY + "坐标: " + TextFormatting.WHITE + String.format("[%d, %d, %d]", p.getX(), p.getY(), p.getZ()));
        tip.add(TextFormatting.GRAY + "维度: " + TextFormatting.WHITE + dimName(dim));

        // 显示远端当前能量（仅当世界/区块加载）
        try {
            UUID id = UUID.fromString(q.getString("NetworkId"));
            int net = QuantumEnergyNetwork.getNetworkPower(id);
            if (net >= 0) {
                tip.add(TextFormatting.AQUA + "远端能量: " + TextFormatting.WHITE + fmt(net) + " RF");
            } else {
                tip.add(TextFormatting.DARK_GRAY + "远端区块未加载");
            }
        } catch (Exception ignored) { }
    }

    private static String dimName(int d) {
        switch (d) { case 0: return "主世界"; case -1: return "下界"; case 1: return "末地"; default: return "维度 " + d; }
    }
}
