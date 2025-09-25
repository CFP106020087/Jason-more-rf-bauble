package com.moremod.client.gui;

import com.moremod.accessorybox.ContainerAccessoryBox;
import com.moremod.accessorybox.GuiAccessoryBox;
import com.moremod.client.gui.ContainerDimensionLoom;
import com.moremod.client.gui.GuiDimensionLoom;
import com.moremod.tile.TileEntityDimensionLoom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 合并处理器：同时处理 机械核心GUI、饰品盒GUI 与 维度织布机GUI */
public class GuiHandler implements IGuiHandler {


    // GUI ID 常量
    public static final int GUI_MECHANICAL_CORE = 0;      // 机械核心
    public static final int ACCESSORY_BOX_GUI_ID = 1;    // 饰品盒
    public static final int DIMENSION_LOOM_GUI = 2;       // 维度织布机
    public static final int GUI_SAGE_BOOK = 3;

    // ======== SERVER ========
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case GUI_SAGE_BOOK:

                    return new ContainerSageBook(player);



            case GUI_MECHANICAL_CORE:
                // 机械核心没有容器
                return null;

            case ACCESSORY_BOX_GUI_ID:
                // 饰品盒容器
                return new ContainerAccessoryBox(player.inventory, player);

            case DIMENSION_LOOM_GUI:
                // 维度织布机容器
                BlockPos pos = new BlockPos(x, y, z);
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TileEntityDimensionLoom) {
                    return new ContainerDimensionLoom(player.inventory, (TileEntityDimensionLoom) te);
                }

                break;

            default:
                System.out.println("[MoreMod] 未知的服务端GUI ID: " + ID);
                break;

        }

        return null;
    }

    // ======== CLIENT ========
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case GUI_MECHANICAL_CORE:
                // 机械核心GUI
                System.out.println("[MoreMod] 打开机械核心GUI");
                return new MechanicalCoreGui(player);
            case GUI_SAGE_BOOK:
                return new GuiSageBook(player);

            case ACCESSORY_BOX_GUI_ID:
                // 饰品盒GUI
                System.out.println("[MoreMod] 打开饰品盒GUI");
                return new GuiAccessoryBox(player.inventory, player);

            case DIMENSION_LOOM_GUI:
                // 维度织布机GUI
                System.out.println("[MoreMod] 打开维度织布机GUI");
                BlockPos pos = new BlockPos(x, y, z);
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TileEntityDimensionLoom) {
                    return new GuiDimensionLoom(player.inventory, (TileEntityDimensionLoom) te);
                }
                break;

            default:
                System.out.println("[MoreMod] 未知的客户端GUI ID: " + ID);
                break;
        }

        return null;
    }
}