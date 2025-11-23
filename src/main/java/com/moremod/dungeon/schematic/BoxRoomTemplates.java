// ====================
// 文件: BoxRoomTemplates.java (修正版)
// 路径: com/moremod/dungeon/schematic/BoxRoomTemplates.java
// 使用正确的setBlockState方法
// ====================
package com.moremod.dungeon.schematic;

import com.moremod.schematic.Schematic;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class BoxRoomTemplates {

    public static Schematic entrance() {
        Schematic s = new Schematic((short)26, (short)8, (short)26);
        IBlockState floor = Blocks.STONE.getDefaultState();
        IBlockState pillar = Blocks.STONEBRICK.getDefaultState();

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        for (int y = 1; y <= 4; y++) {
            s.setBlockState(5, y, 5, pillar);
            s.setBlockState(20, y, 5, pillar);
            s.setBlockState(5, y, 20, pillar);
            s.setBlockState(20, y, 20, pillar);
        }

        s.setBlockState(13, 2, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(13, 2, 23, Blocks.TORCH.getDefaultState());

        return s;
    }

    public static Schematic treasure() {
        return EnhancedRoomTemplates.treasureRoom();
    }

    public static Schematic library() {
        Schematic s = new Schematic((short)26, (short)8, (short)26);
        IBlockState floor = Blocks.PLANKS.getDefaultState();
        IBlockState shelf = Blocks.BOOKSHELF.getDefaultState();

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        for (int x = 2; x < 24; x += 4) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 2; z < 24; z++) {
                    if (z % 4 != 0) {
                        s.setBlockState(x, y, z, shelf);
                    }
                }
            }
        }

        s.setBlockState(13, 1, 13, Blocks.ENCHANTING_TABLE.getDefaultState());

        return s;
    }

    public static Schematic alchemy() {
        Schematic s = new Schematic((short)26, (short)8, (short)26);

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, Blocks.STONEBRICK.getDefaultState());
            }
        }

        for (int i = 0; i < 4; i++) {
            int x = 8 + (i % 2) * 10;
            int z = 8 + (i / 2) * 10;
            s.setBlockState(x, 1, z, Blocks.BREWING_STAND.getDefaultState());
            s.setBlockState(x, 1, z + 1, Blocks.CAULDRON.getDefaultState());
        }

        return s;
    }

    public static Schematic greenhouse() {
        Schematic s = new Schematic((short)26, (short)8, (short)26);

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, Blocks.GRASS.getDefaultState());
            }
        }

        for (int x = 4; x < 22; x += 3) {
            for (int z = 4; z < 22; z += 3) {
                s.setBlockState(x, 0, z, Blocks.FARMLAND.getDefaultState());
                // 成熟小麦
                s.setBlockState(x, 1, z, Blocks.WHEAT.getStateFromMeta(7));
            }
        }

        s.setBlockState(13, 0, 13, Blocks.WATER.getDefaultState());

        return s;
    }

    public static Schematic fountain() {
        Schematic s = new Schematic((short)26, (short)8, (short)26);

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, Blocks.STONE.getDefaultState());
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                if (dist == 3) {
                    s.setBlockState(13 + dx, 1, 13 + dz, Blocks.STONE_BRICK_STAIRS.getDefaultState());
                } else if (dist == 2) {
                    s.setBlockState(13 + dx, 0, 13 + dz, Blocks.WATER.getDefaultState());
                }
            }
        }

        for (int y = 1; y <= 3; y++) {
            s.setBlockState(13, y, 13, Blocks.QUARTZ_BLOCK.getDefaultState());
        }
        s.setBlockState(13, 4, 13, Blocks.WATER.getDefaultState());

        return s;
    }

    public static Schematic arena() {
        return EnhancedRoomTemplates.bossArena();
    }

    public static Schematic miniMaze() {
        return EnhancedRoomTemplates.mazeRoom();
    }

    public static Schematic combatRoom() {
        return EnhancedRoomTemplates.combatRoom();
    }

    public static Schematic trapRoom() {
        return EnhancedRoomTemplates.trapRoom();
    }
}