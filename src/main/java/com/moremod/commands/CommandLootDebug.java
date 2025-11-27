package com.moremod.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

public class CommandLootDebug extends CommandBase {

    @Override
    public String getName() {
        return "lootdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/lootdebug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) sender;
        WorldServer world = (WorldServer) player.world;

        player.sendMessage(new TextComponentString("§e===== 戰利品表調試 ====="));

        // 測試多個可能的路徑
        String[] testPaths = {
                "moremod:dungeon/normal",
                "moremod:loot_tables/dungeon/normal",
                "moremod:chests/dungeon_normal",
                "minecraft:chests/simple_dungeon"  // 原版測試
        };

        for (String path : testPaths) {
            ResourceLocation loc = new ResourceLocation(path);
            LootTable table = world.getLootTableManager().getLootTableFromLocation(loc);

            if (table != null && table != LootTable.EMPTY_LOOT_TABLE) {
                player.sendMessage(new TextComponentString("§a✓ 找到: " + path));

                // 生成箱子測試
                BlockPos pos = player.getPosition().offset(player.getHorizontalFacing(), 2);
                world.setBlockState(pos, Blocks.CHEST.getDefaultState());
                TileEntityChest chest = (TileEntityChest) world.getTileEntity(pos);

                if (chest != null) {
                    chest.clear();
                    LootContext context = new LootContext.Builder(world).build();
                    table.fillInventory(chest, world.rand, context);

                    int count = 0;
                    for (int i = 0; i < chest.getSizeInventory(); i++) {
                        if (!chest.getStackInSlot(i).isEmpty()) {
                            count++;
                        }
                    }
                    player.sendMessage(new TextComponentString("  生成了 " + count + " 個物品"));
                }
            } else {
                player.sendMessage(new TextComponentString("§c✗ 未找到: " + path));
            }
        }
    }
}