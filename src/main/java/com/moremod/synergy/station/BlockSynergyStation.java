package com.moremod.synergy.station;

import com.moremod.client.gui.GuiHandler;
import com.moremod.moremod;
import com.moremod.system.humanity.HumanityEffectsManager;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Synergy 链结站方块
 *
 * 玩家可以在这个方块上配置模块链结。
 */
public class BlockSynergyStation extends Block implements ITileEntityProvider {

    public BlockSynergyStation() {
        super(Material.IRON);
        setTranslationKey("synergy_station");
        setRegistryName("synergy_station");
        setHardness(3.5F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySynergyStation();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            // 检查人性值是否允许使用链结站
            if (!HumanityEffectsManager.canUseSynergyStation(playerIn)) {
                playerIn.sendStatusMessage(new TextComponentString(
                        "§8§o机械识别系统拒绝访问...你的存在太过异常"), true);
                return true;
            }

            // GUI ID 29 - 在 GuiHandler 中注册
            playerIn.openGui(moremod.INSTANCE, GuiHandler.SYNERGY_STATION_GUI,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        // 1. 获取 TileEntity
        TileEntity tileentity = worldIn.getTileEntity(pos);

        // 2. 检查是否是我们的链结站 TE
        if (tileentity instanceof TileEntitySynergyStation) {
            TileEntitySynergyStation station = (TileEntitySynergyStation) tileentity;

            // 3. 如果当前处于激活状态，强制为对应的玩家停用 Synergy
            if (station.isActivated()) {
                // 注意：这里我们可能拿不到“破坏方块的玩家”实体，
                // 但 TE 里存了 activatedByPlayerUUID，我们可以用那个！
                java.util.UUID playerId = station.getActivatedByPlayerUUID();
                String synergyId = station.getMatchedSynergyId();

                if (playerId != null && synergyId != null) {
                    EntityPlayer player = worldIn.getPlayerEntityByUUID(playerId);
                    // 只有当玩家在线且就在附近时，才能通过常规手段移除
                    // 如果玩家下线了，SynergyManager 应该有自己的清理机制（比如 PlayerLoggedOutEvent）
                    if (player != null) {
                        // 强制调用 Manager 移除效果
                        com.moremod.synergy.core.SynergyManager.getInstance()
                                .deactivateSynergyForPlayer(player, synergyId);

                        // 可选：给玩家发个消息告诉他链结断了
                        player.sendStatusMessage(new TextComponentString(
                                "§c⚠ 链结基站信号丢失，Synergy 强制中断。"), true);
                    }
                }
            }
        }

        // 4. 继续原本的破坏逻辑
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
