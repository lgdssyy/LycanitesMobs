package com.lycanitesmobs.core.spawner.location;

import com.google.gson.JsonObject;
import com.lycanitesmobs.core.spawning.CoordSorterFurthest;
import com.lycanitesmobs.core.spawning.CoordSorterNearest;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomSpawnLocation extends SpawnLocation {
	/** How many random positions to select. **/
	public int limit = 32;

	/** If true, positions that can see the sky are allowed. **/
	public boolean surface = true;

	/** If true positions that can't see the sky are allowed. **/
	public boolean underground = true;

	/** If true positions require a solid walkable block underneath. **/
	public boolean solidGround = false;

	/** The block type to spawn in, by default this is air but can be set to water, etc. **/
	public Block insideBlock = Blocks.AIR;


    @Override
    public void fromJSON(JsonObject json) {
		super.fromJSON(json);
    }

    @Override
    public List<BlockPos> getSpawnPositions(World world, EntityPlayer player, BlockPos triggerPos) {
        List<BlockPos> spawnPositions = new ArrayList<>();

		for(int i = 0; i < this.limit; i++) {
			BlockPos randomPos = this.getRandomPosition(world, player, triggerPos);
			if(randomPos != null) {
				spawnPositions.add(randomPos);
			}
		}

        return this.sortSpawnPositions(spawnPositions, triggerPos);
    }

	/** Gets a random spawn position.
	 * @param world The world to search for coordinates in.
	 * @param player Player that triggered the spawn.
	 * @param triggerPos The trigger position to search around.
	 * @return Returns a BlockPos or null if no coord was found.
	 */
	public BlockPos getRandomPosition(World world, EntityPlayer player, BlockPos triggerPos) {
		int[] xz = this.getRandomXZCoord(world, triggerPos.getX(), triggerPos.getZ());
		int x = xz[0];
		int z = xz[1];
		int y = this.getRandomYCoord(world, new BlockPos(x, triggerPos.getY(), z));
		return y > -1 ? new BlockPos(x, y, z) : null;
	}

	/**
	 * Gets a random XZ position from the provided XZ position using the provided range and range max radii.
	 * @param world The world that the coordinates are being selected in, mainly for getting Random.
	 * @param originX The origin x position.
	 * @param originZ The origin z position.
	 * @return An integer array containing two ints the X and Z position.
	 */
	public int[] getRandomXZCoord(World world, int originX, int originZ) {
		float xScale = world.rand.nextFloat();
		float zScale = world.rand.nextFloat();
		float minScale = (float)(rangeMin) / (float)(rangeMin);

		if(xScale + zScale < minScale * 2) {
			float xShare = world.rand.nextFloat();
			float zShare = 1.0F - xShare;
			xScale += minScale * xShare;
			zScale += minScale * zShare;
		}

		int x = Math.round(rangeMax * xScale);
		int z = Math.round(rangeMax * zScale);

		if(world.rand.nextBoolean())
			x = originX + x;
		else
			x = originX - x;

		if(world.rand.nextBoolean())
			z = originZ + z;
		else
			z = originZ - z;

		return new int[] {x, z};
	}

	/**
	 * Gets a random Y position from the provided XYZ position using the provided range and range max radiuses.
	 * @param world The world that the coordinates are being selected in, mainly for getting Random.
	 * @param triggerPos The position to search from using XZ coords and up and down within range of the Y coord.
	 * @return The y position, -1 if a valid position could not be found.
	 */
	public int getRandomYCoord(World world, BlockPos triggerPos) {
		int originX = triggerPos.getX();
		int originY = triggerPos.getY();
		int originZ = triggerPos.getZ();
		int minY = Math.max(originY - rangeMax, 0);
		int maxY = originY + rangeMax;
		List<Integer> yCoordsLow = new ArrayList<Integer>();
		List<Integer> yCoordsHigh = new ArrayList<Integer>();

		// Get Every Valid Y Pos:
		for(int nextY = minY; nextY <= maxY; nextY++) {
			// If the next Y coord to check is not within the min range, boost it up to the min range:
			if(nextY > originY - rangeMin && nextY < originY + rangeMin)
				nextY = originY + rangeMin;

			BlockPos spawnPos = new BlockPos(originX, nextY, originZ);
			IBlockState blockState = world.getBlockState(spawnPos);
			Block block = blockState.getBlock();

			// If block is the inside block or if checking for a solid surface, if the block is a solid surface to spawn on.
			if(block != null && ((!solidSurface && block == insideBlock) || (solidSurface && this.validGroundBlock(blockState, world, spawnPos)))) {
				// Make sure the block above is within range:
				if(nextY + 1 > originY - minY && nextY + 1 < originY - maxY)
					continue;

				// If above ground:
				if(world.canBlockSeeSky(spawnPos)) {
					BlockPos checkPos = spawnPos;
					if(solidSurface)
						checkPos = checkPos.add(0, 1, 0);
					if(underground || world.getBlockState(checkPos).getBlock() != insideBlock)
						break;
					if(!solidSurface) {
						int skyCoord = nextY;
						int skyRange = Math.min(world.getHeight() - 1, maxY) - skyCoord;
						// Get random y coord within inside block:
						if(skyRange > 1) {
							if(insideBlock != Blocks.AIR)
								skyRange = this.getInsideBlockHeight(world, checkPos, insideBlock);
							nextY += world.rand.nextInt(skyRange);
						}
						if(skyRange == 1)
							nextY = 1;
					}
					if(nextY + 1 <= 64)
						yCoordsLow.add(nextY + 1);
					else
						yCoordsHigh.add(nextY + 1);
					break;
				}

				else if(this.doesCoordHaveSpace(world, spawnPos.add(0, 1, 0), insideBlock)) {
					if(nextY + 1 <= 64)
						yCoordsLow.add(nextY + 1);
					else
						yCoordsHigh.add(nextY + 1);
					nextY += 2;
				}
			}
		}

		// Pick Random Y Pos:
		int y = -1;
		if(yCoordsHigh.size() > 0 && (yCoordsLow.size() <= 0 || world.rand.nextFloat() > 0.25F)) {
			if(yCoordsHigh.size() == 1)
				y = yCoordsHigh.get(0);
			else
				y = yCoordsHigh.get(world.rand.nextInt(yCoordsHigh.size() - 1));
		}
		else if(yCoordsLow.size() > 0) {
			if(yCoordsLow.size() == 1)
				y = yCoordsLow.get(0);
			else
				y = yCoordsLow.get(world.rand.nextInt(yCoordsLow.size() - 1));
		}

		return y;
	}

	/** Returns the height of insideBlocks from the starting position checking upwards until the insideBlock is no longer found. **/
	public int getInsideBlockHeight(World world, BlockPos startPos, Block insideBlock) {
		int y;
		for(y = startPos.getY(); y < world.getActualHeight(); y++) {
			BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
			if(world.getBlockState(checkPos).getBlock() != insideBlock)
				break;
		}
		return y - startPos.getY();
	}

	/** Returns true if the specified block is suitable for spawning land mobs on top of. **/
	public boolean validGroundBlock(IBlockState blockState, World world, BlockPos pos) {
		if(blockState == null)
			return false;
		try {
			if(blockState.isNormalCube())
				return true;
		} catch(Exception e) {}
		try {
			if (blockState.isSideSolid(world, pos, EnumFacing.UP))
				return true;
			if (blockState.isSideSolid(world, pos, EnumFacing.DOWN))
				return true;
		} catch(Exception e) {}
		return false;
	}
}