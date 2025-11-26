package com.moremod.dungeon.schematic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import com.moremod.schematic.Schematic;
import java.util.*;

public class AdvancedSchematicBuilder {

    private final Schematic schematic;
    public final Random rand = new Random();

    public AdvancedSchematicBuilder(int width, int height, int length) {
        this.schematic = new Schematic((short)width, (short)height, (short)length);
    }

    public AdvancedSchematicBuilder setBlock(int x, int y, int z, IBlockState state) {
        if (isInBounds(x, y, z)) {
            schematic.setBlockState(x, y, z, state);
        }
        return this;
    }

    public AdvancedSchematicBuilder setBlock(BlockPos pos, IBlockState state) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    public AdvancedSchematicBuilder fillArea(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    setBlock(x, y, z, state);
                }
            }
        }
        return this;
    }

    public AdvancedSchematicBuilder clearArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        return fillArea(x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
    }

    public AdvancedSchematicBuilder hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
        fillArea(x1, y1, z1, x2, y1, z2, state);
        fillArea(x1, y2, z1, x2, y2, z2, state);
        fillArea(x1, y1, z1, x2, y2, z1, state);
        fillArea(x1, y1, z2, x2, y2, z2, state);
        fillArea(x1, y1, z1, x1, y2, z2, state);
        fillArea(x2, y1, z1, x2, y2, z2, state);
        return this;
    }

    private BlockPos offsetByDirection(BlockPos start, EnumFacing direction, int horizontal, int vertical) {
        switch (direction) {
            case NORTH: return start.add(-horizontal, vertical, 0);
            case SOUTH: return start.add(horizontal, vertical, 0);
            case EAST:  return start.add(0, vertical, horizontal);
            case WEST:  return start.add(0, vertical, -horizontal);
            default:    return start.add(0, vertical, 0);
        }
    }

    private void removeWall(boolean[][] maze, BlockPos current, BlockPos next) {
        int cx = current.getX(), cz = current.getZ();
        int nx = next.getX(), nz = next.getZ();
        int wallX = (cx + nx) / 2;
        int wallZ = (cz + nz) / 2;
        if (wallX >= 0 && wallX < maze.length && wallZ >= 0 && wallZ < maze[0].length) {
            maze[wallX][wallZ] = false;
        }
        if (nx >= 0 && nx < maze.length && nz >= 0 && nz < maze[0].length) {
            maze[nx][nz] = false;
        }
    }

    private List<BlockPos> getUnvisitedNeighbors(boolean[][] maze, BlockPos current) {
        List<BlockPos> neighbors = new ArrayList<>();
        int x = current.getX(), z = current.getZ();
        int[][] directions = {{0, -2}, {2, 0}, {0, 2}, {-2, 0}};
        for (int[] dir : directions) {
            int nx = x + dir[0], nz = z + dir[1];
            if (nx >= 0 && nx < maze.length && nz >= 0 && nz < maze[0].length) {
                if (maze[nx][nz]) neighbors.add(new BlockPos(nx, 0, nz));
            }
        }
        return neighbors;
    }

    public AdvancedSchematicBuilder generateCircularRoom(BlockPos center, int radius, IBlockState wall) {
        for (int x = 0; x < schematic.width; x++) {
            for (int z = 0; z < schematic.length; z++) {
                for (int y = 0; y < schematic.height; y++) {
                    double dist = Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2));
                    if (y == 0 || y == schematic.height - 1) {
                        if (dist <= radius + 0.5) schematic.setBlockState(x, y, z, wall);
                    } else {
                        if (dist >= radius - 0.5 && dist <= radius + 0.5) schematic.setBlockState(x, y, z, wall);
                    }
                }
            }
        }
        return this;
    }

    public AdvancedSchematicBuilder generateMaze(int gridSize, IBlockState wallBlock) {
        int mazeWidth = schematic.width / gridSize;
        int mazeHeight = schematic.length / gridSize;
        boolean[][] maze = generateMazeGrid(mazeWidth, mazeHeight);
        for (int x = 0; x < mazeWidth; x++) {
            for (int z = 0; z < mazeHeight; z++) {
                if (maze[x][z]) {
                    fillArea(x * gridSize, 1, z * gridSize,
                            (x + 1) * gridSize - 1, schematic.height - 2, (z + 1) * gridSize - 1,
                            wallBlock);
                }
            }
        }
        return this;
    }

    private boolean[][] generateMazeGrid(int width, int height) {
        boolean[][] maze = new boolean[width][height];
        for (int x = 0; x < width; x++) Arrays.fill(maze[x], true);

        Stack<BlockPos> stack = new Stack<>();
        BlockPos current = new BlockPos(0, 0, 0);
        maze[0][0] = false;
        stack.push(current);

        while (!stack.isEmpty()) {
            List<BlockPos> neighbors = getUnvisitedNeighbors(maze, current);
            if (!neighbors.isEmpty()) {
                BlockPos next = neighbors.get(rand.nextInt(neighbors.size()));
                removeWall(maze, current, next);
                current = next;
                stack.push(current);
            } else if (!stack.isEmpty()) {
                current = stack.pop();
            }
        }
        return maze;
    }

    public AdvancedSchematicBuilder addArch(BlockPos start, EnumFacing direction, int width, int height) {
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = offsetByDirection(start, direction, w, h);
                boolean isArch = h < height - 2 ||
                        (h == height - 2 && (w == 0 || w == width - 1)) ||
                        (h == height - 1 && w > 0 && w < width - 1);
                if (isArch && isInBounds(pos)) {
                    schematic.setBlockState(pos.getX(), pos.getY(), pos.getZ(), Blocks.STONEBRICK.getDefaultState());
                }
            }
        }
        return this;
    }

    public AdvancedSchematicBuilder addSpiralStaircase(BlockPos center, int radius, int startY, int endY) {
        double angleStep = Math.PI / 4;
        int steps = (endY - startY) * 4;
        for (int i = 0; i < steps; i++) {
            double angle = i * angleStep;
            int x = center.getX() + (int) (Math.cos(angle) * radius);
            int z = center.getZ() + (int) (Math.sin(angle) * radius);
            int y = startY + i / 4;
            if (isInBounds(x, y, z)) {
                schematic.setBlockState(x, y, z, Blocks.STONE_BRICK_STAIRS.getDefaultState());
                if (isInBounds(x, y + 1, z)) {
                    schematic.setBlockState(x, y + 1, z, Blocks.IRON_BARS.getDefaultState());
                }
            }
        }
        return this;
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < schematic.width &&
                y >= 0 && y < schematic.height &&
                z >= 0 && z < schematic.length;
    }

    private boolean isInBounds(BlockPos pos) {
        return isInBounds(pos.getX(), pos.getY(), pos.getZ());
    }

    public Schematic build() {
        return schematic;
    }
}
