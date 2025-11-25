package com.moremod.client.gui;

// 维度织机

// ⭐ 物品传输器GUI导入
import com.moremod.accessorybox.ContainerAccessoryBox;
import com.moremod.accessorybox.GuiAccessoryBox;
import com.moremod.tile.TileEntityItemTransporter;

// 🏭 装瓶机GUI导入
import com.moremod.container.ContainerBottlingMachine;
import com.moremod.tile.TileEntityBottlingMachine;

// 🏪 自动交易机GUI导入
import com.moremod.container.ContainerTradingStation;
import com.moremod.tile.TileTradingStation;

// 🌌 虚空背包链接GUI导入
import com.moremod.inventory.ContainerVoidBackpack;
import com.moremod.inventory.InventoryVoidBackpack;

// 🗡️ 劍升級台（material 版）GUI導入
import com.moremod.container.ContainerSwordUpgradeStationMaterial;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;

// 🧿 旧：宝石镶嵌台/旧升級台（若工程中仍存在，以下三项用于兼容旧世界）
import com.moremod.container.ContainerSwordUpgradeStation;
import com.moremod.tile.TileEntitySwordUpgradeStation;

// 💎 提取台GUI导入
import com.moremod.container.ContainerExtractionStation;
import com.moremod.tile.TileEntityExtractionStation;

// 🔮 提纯祭坛GUI导入
import com.moremod.container.ContainerPurificationAltar;
import com.moremod.tile.TileEntityPurificationAltar;

// 🎨 转移台GUI导入
import com.moremod.container.ContainerTransferStation;
import com.moremod.tile.TileEntityTransferStation;

// ⚡ Synergy 链结站GUI导入
import com.moremod.synergy.station.ContainerSynergyStation;
import com.moremod.synergy.station.GuiSynergyStation;
import com.moremod.synergy.station.TileEntitySynergyStation;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiHandler implements IGuiHandler {

    // GUI ID 常量
    public static final int GUI_MECHANICAL_CORE   = 0;
    public static final int ACCESSORY_BOX_GUI_ID  = 1;
    public static final int DIMENSION_LOOM_GUI    = 2;
    public static final int GUI_SAGE_BOOK         = 3;
    public static final int ACCESSORY_BOX_T1_GUI  = 11;
    public static final int ACCESSORY_BOX_T2_GUI  = 12;
    public static final int ACCESSORY_BOX_T3_GUI  = 13;
    public static final int ITEM_TRANSPORTER_GUI  = 20;
    public static final int BOTTLING_MACHINE_GUI  = 21;
    public static final int TRADING_STATION_GUI   = 22;
    public static final int VOID_BACKPACK_GUI     = 23;

    // 旧：宝石镶嵌台/旧升級台（兼容旧世界）
    public static final int SWORD_UPGRADE_STATION_GUI = 24;

    // 新：劍升級台（material 版）
    public static final int SWORD_UPGRADE_STATION_MATERIAL_GUI = 25;

    // 💎 提取台
    public static final int GEM_EXTRACTION_STATION_GUI = 26;

    // 🔮 提纯祭坛
    public static final int PURIFICATION_ALTAR_GUI = 27;

    // 🎨 转移台
    public static final int TRANSFER_STATION_GUI = 28;

    // ⚡ Synergy 链结站
    public static final int SYNERGY_STATION_GUI = 29;

    // ---------------- Server ----------------
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        System.out.println("[GuiHandler-Server] GUI ID: " + ID);
        try {
            Object result;
            switch (ID) {
                case GUI_SAGE_BOOK: {
                    EnumHand hand = (x == 1) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
                    result = new ContainerSageBook(player, hand);
                    break;
                }
                case GUI_MECHANICAL_CORE: {
                    result = null;
                    break;
                }
                case ACCESSORY_BOX_GUI_ID: {
                    result = new ContainerAccessoryBox(player.inventory, player, 3);
                    break;
                }
                case ACCESSORY_BOX_T1_GUI: {
                    result = new ContainerAccessoryBox(player.inventory, player, 1);
                    break;
                }
                case ACCESSORY_BOX_T2_GUI: {
                    result = new ContainerAccessoryBox(player.inventory, player, 2);
                    break;
                }
                case ACCESSORY_BOX_T3_GUI: {
                    result = new ContainerAccessoryBox(player.inventory, player, 3);
                    break;
                }
                case DIMENSION_LOOM_GUI: {
                    result = createDimensionLoomContainer(player, world, x, y, z);
                    break;
                }
                case ITEM_TRANSPORTER_GUI: {
                    result = createItemTransporterContainer(player, world, x, y, z);
                    break;
                }
                case BOTTLING_MACHINE_GUI: {
                    result = createBottlingMachineContainer(player, world, x, y, z);
                    break;
                }
                case TRADING_STATION_GUI: {
                    result = createTradingStationContainer(player, world, x, y, z);
                    break;
                }
                case VOID_BACKPACK_GUI: {
                    result = createVoidBackpackContainer(player, world, x);
                    break;
                }
                case SWORD_UPGRADE_STATION_GUI: { // 旧ID=24：兼容旧TE与material TE
                    result = createSwordUpgradeStationContainer(player, world, x, y, z);
                    break;
                }
                case SWORD_UPGRADE_STATION_MATERIAL_GUI: { // 新ID=25
                    result = createSwordUpgradeStationMaterialContainer(player, world, x, y, z);
                    break;
                }
                case GEM_EXTRACTION_STATION_GUI: { // 💎 ID=26
                    result = createExtractionStationContainer(player, world, x, y, z);
                    break;
                }
                case PURIFICATION_ALTAR_GUI: { // 🔮 ID=27
                    result = createPurificationAltarContainer(player, world, x, y, z);
                    break;
                }
                case TRANSFER_STATION_GUI: { // 🎨 ID=28
                    result = createTransferStationContainer(player, world, x, y, z);
                    break;
                }
                case SYNERGY_STATION_GUI: { // ⚡ ID=29
                    result = createSynergyStationContainer(player, world, x, y, z);
                    break;
                }
                default:
                    result = null;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------------- Client ----------------
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        System.out.println("[GuiHandler-Client] GUI ID: " + ID);
        try {
            Object result;
            switch (ID) {
                case GUI_MECHANICAL_CORE: {
                    result = new MechanicalCoreGui(player);
                    break;
                }
                case GUI_SAGE_BOOK: {
                    EnumHand hand = (x == 1) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
                    result = new GuiSageBook(player, hand);
                    break;
                }
                case ACCESSORY_BOX_GUI_ID: {
                    result = new GuiAccessoryBox(player.inventory, player, 3);
                    break;
                }
                case ACCESSORY_BOX_T1_GUI: {
                    result = new GuiAccessoryBox(player.inventory, player, 1);
                    break;
                }
                case ACCESSORY_BOX_T2_GUI: {
                    result = new GuiAccessoryBox(player.inventory, player, 2);
                    break;
                }
                case ACCESSORY_BOX_T3_GUI: {
                    result = new GuiAccessoryBox(player.inventory, player, 3);
                    break;
                }
                case DIMENSION_LOOM_GUI: {
                    result = createDimensionLoomGui(player, world, x, y, z);
                    break;
                }
                case ITEM_TRANSPORTER_GUI: {
                    result = createItemTransporterGui(player, world, x, y, z);
                    break;
                }
                case BOTTLING_MACHINE_GUI: {
                    result = createBottlingMachineGui(player, world, x, y, z);
                    break;
                }
                case TRADING_STATION_GUI: {
                    result = createTradingStationGui(player, world, x, y, z);
                    break;
                }
                case VOID_BACKPACK_GUI: {
                    result = createVoidBackpackGui(player, world, x);
                    break;
                }
                case SWORD_UPGRADE_STATION_GUI: { // 旧ID=24：兼容旧TE与material TE
                    result = createSwordUpgradeStationGui(player, world, x, y, z);
                    break;
                }
                case SWORD_UPGRADE_STATION_MATERIAL_GUI: { // 新ID=25
                    result = createSwordUpgradeStationMaterialGui(player, world, x, y, z);
                    break;
                }
                case GEM_EXTRACTION_STATION_GUI: { // 💎 ID=26
                    result = createExtractionStationGui(player, world, x, y, z);
                    break;
                }
                case PURIFICATION_ALTAR_GUI: { // 🔮 ID=27
                    result = createPurificationAltarGui(player, world, x, y, z);
                    break;
                }
                case TRANSFER_STATION_GUI: { // 🎨 ID=28
                    result = createTransferStationGui(player, world, x, y, z);
                    break;
                }
                case SYNERGY_STATION_GUI: { // ⚡ ID=29
                    result = createSynergyStationGui(player, world, x, y, z);
                    break;
                }
                default:
                    result = null;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------- 原有模块 ----------
    private Object createDimensionLoomContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof com.moremod.tile.TileEntityDimensionLoom) {
            return new ContainerDimensionLoom(player.inventory, (com.moremod.tile.TileEntityDimensionLoom) te);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createDimensionLoomGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof com.moremod.tile.TileEntityDimensionLoom) {
            return new GuiDimensionLoom(player.inventory, (com.moremod.tile.TileEntityDimensionLoom) te);
        }
        return null;
    }

    private Object createItemTransporterContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityItemTransporter) {
            return new ContainerItemTransporter(player, (TileEntityItemTransporter) te);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createItemTransporterGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityItemTransporter) {
            return new GuiItemTransporter(player, (TileEntityItemTransporter) te);
        }
        return null;
    }

    private Object createBottlingMachineContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBottlingMachine) {
            return new ContainerBottlingMachine(player.inventory, (TileEntityBottlingMachine) te);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createBottlingMachineGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBottlingMachine) {
            return new GuiBottlingMachine(player.inventory, (TileEntityBottlingMachine) te);
        }
        return null;
    }

    private Object createTradingStationContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileTradingStation) {
            return new ContainerTradingStation(player, (TileTradingStation) te);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createTradingStationGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileTradingStation) {
            return new GuiTradingStation(player, (TileTradingStation) te);
        }
        return null;
    }

    private Object createVoidBackpackContainer(EntityPlayer player, World world, int size) {
        InventoryVoidBackpack voidInv = InventoryVoidBackpack.get(world);
        if (voidInv != null) {
            return new ContainerVoidBackpack(player, voidInv, size);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createVoidBackpackGui(EntityPlayer player, World world, int size) {
        InventoryVoidBackpack voidInv = InventoryVoidBackpack.get(world);
        if (voidInv != null) {
            return new GuiVoidBackpack(player.inventory, voidInv, size);
        }
        return null;
    }

    // ====== 旧ID=24：宝石镶嵌台/旧升級台 —— 兼容实现 ======
    private Object createSwordUpgradeStationContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        // 先匹配 material TE（如果你已经把旧台子替换成了 material 实现）

        // 兼容：旧 TE / 旧容器
        if (te instanceof TileEntitySwordUpgradeStation) {
            System.out.println("[GuiHandler] 使用旧 Container 打开旧ID=24");
            return new ContainerSwordUpgradeStation(player.inventory, (TileEntitySwordUpgradeStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 SwordUpgrade TE (ID=24): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createSwordUpgradeStationGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntitySwordUpgradeStationMaterial) {
            System.out.println("[GuiHandler] 使用 material GUI 打开旧ID=24");
            return new GuiSwordUpgradeStationMaterial(player.inventory, (TileEntitySwordUpgradeStationMaterial) te);
        }
        if (te instanceof TileEntitySwordUpgradeStation) {
            System.out.println("[GuiHandler] 使用旧 GUI 打开旧ID=24");
            return new GuiSwordUpgradeStation(player.inventory, (TileEntitySwordUpgradeStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 SwordUpgrade TE(客户端, ID=24): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ====== 新ID=25：material 版 ======
    private Object createSwordUpgradeStationMaterialContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntitySwordUpgradeStationMaterial) {
            return new ContainerSwordUpgradeStationMaterial(player.inventory, (TileEntitySwordUpgradeStationMaterial) te);
        }
        // 兼容：如果有人把旧台子绑到了新ID
        if (te instanceof TileEntitySwordUpgradeStation) {
            return new ContainerSwordUpgradeStation(player.inventory, (TileEntitySwordUpgradeStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 SwordUpgrade TE (新ID=25): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createSwordUpgradeStationMaterialGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntitySwordUpgradeStationMaterial) {
            return new GuiSwordUpgradeStationMaterial(player.inventory, (TileEntitySwordUpgradeStationMaterial) te);
        }
        if (te instanceof TileEntitySwordUpgradeStation) {
            return new GuiSwordUpgradeStation(player.inventory, (TileEntitySwordUpgradeStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 SwordUpgrade TE(客户端, 新ID=25): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ====== 💎 ID=26：提取台 ======
    private Object createExtractionStationContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityExtractionStation) {
            System.out.println("[GuiHandler] 打开提取台 Container");
            return new ContainerExtractionStation(player.inventory, (TileEntityExtractionStation) te, player);
        }
        System.out.println("[GuiHandler] ❌ 未识别的提取台 TE: " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createExtractionStationGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityExtractionStation) {
            System.out.println("[GuiHandler] 打开提取台 GUI");
            return new GuiExtractionStation(player.inventory, (TileEntityExtractionStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的提取台 TE(客户端): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ====== 🔮 ID=27：提纯祭坛 ======
    private Object createPurificationAltarContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityPurificationAltar) {
            System.out.println("[GuiHandler] 打开提纯祭坛 Container");
            return new ContainerPurificationAltar(player.inventory, (TileEntityPurificationAltar) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的提纯祭坛 TE: " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createPurificationAltarGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityPurificationAltar) {
            System.out.println("[GuiHandler] 打开提纯祭坛 GUI");
            return new GuiPurificationAltarCodeDrawn(player.inventory, (ContainerPurificationAltar) createPurificationAltarContainer(player, world, x, y, z));
        }
        System.out.println("[GuiHandler] ❌ 未识别的提纯祭坛 TE(客户端): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ====== 🎨 ID=28：转移台 ======
    private Object createTransferStationContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityTransferStation) {
            System.out.println("[GuiHandler] 打开转移台 Container");
            return new ContainerTransferStation(player.inventory, (TileEntityTransferStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的转移台 TE: " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }
    @SideOnly(Side.CLIENT)
    private Object createTransferStationGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityTransferStation) {
            System.out.println("[GuiHandler] 打开转移台 GUI");

            // ⭐ 正确：先创建 Container
            ContainerTransferStation container =
                    new ContainerTransferStation(player.inventory, (TileEntityTransferStation) te);

            // ⭐ 再把 Container 传给 GUI（你最新版 GUI 的构造函数）
            return new GuiTransferStationCodeDrawn(player.inventory, container);
        }

        System.out.println("[GuiHandler] ❌ 未识别的转移台 TE(客户端): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ====== ⚡ ID=29：Synergy 链结站 ======
    private Object createSynergyStationContainer(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntitySynergyStation) {
            System.out.println("[GuiHandler] 打开 Synergy 链结站 Container");
            return new ContainerSynergyStation(player.inventory, (TileEntitySynergyStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 Synergy 链结站 TE: " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    @SideOnly(Side.CLIENT)
    private Object createSynergyStationGui(EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntitySynergyStation) {
            System.out.println("[GuiHandler] 打开 Synergy 链结站 GUI");
            return new GuiSynergyStation(player, (TileEntitySynergyStation) te);
        }
        System.out.println("[GuiHandler] ❌ 未识别的 Synergy 链结站 TE(客户端): " + (te == null ? "null" : te.getClass().getName()));
        return null;
    }

    // ---------- 工具方法 ----------
    public static int getAccessoryBoxGuiId(int tier) {
        switch (tier) {
            case 1: return ACCESSORY_BOX_T1_GUI;
            case 2: return ACCESSORY_BOX_T2_GUI;
            case 3: default: return ACCESSORY_BOX_T3_GUI;
        }
    }

    public static boolean isAccessoryBoxGui(int guiId) {
        return guiId == ACCESSORY_BOX_GUI_ID ||
                guiId == ACCESSORY_BOX_T1_GUI ||
                guiId == ACCESSORY_BOX_T2_GUI ||
                guiId == ACCESSORY_BOX_T3_GUI;
    }

    public static boolean requiresTileEntity(int guiId) {
        return guiId == DIMENSION_LOOM_GUI ||
                guiId == ITEM_TRANSPORTER_GUI ||
                guiId == BOTTLING_MACHINE_GUI ||
                guiId == TRADING_STATION_GUI ||
                guiId == SWORD_UPGRADE_STATION_GUI ||            // 旧
                guiId == SWORD_UPGRADE_STATION_MATERIAL_GUI ||   // 新
                guiId == GEM_EXTRACTION_STATION_GUI ||           // 💎 提取台
                guiId == PURIFICATION_ALTAR_GUI ||               // 🔮 提纯祭坛
                guiId == TRANSFER_STATION_GUI ||                 // 🎨 转移台
                guiId == SYNERGY_STATION_GUI;                    // ⚡ Synergy 链结站
    }

    public static boolean isItemBasedGui(int guiId) {
        return isAccessoryBoxGui(guiId) ||
                guiId == GUI_SAGE_BOOK ||
                guiId == VOID_BACKPACK_GUI;
    }
}