package net.skds.physex.blockphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.blockphysics.BFTask.Type;
import net.skds.physex.util.blockupdate.WWSGlobal;

public class BFManager {

	public static void addTask(ServerWorld w, BlockPos pos, BlockState state, BFTask.Type type) {
		Material material = state.getMaterial();
		Block block = state.getBlock();
		if (material == Material.AIR || material.isLiquid() || block == Blocks.BEDROCK || !state.getFluidState().isEmpty()) {
			return;
		}
		//WWS wws = WWSGlobal.get(w).blocks;
		//wws.addTask(pos, type);
	}

	public static void addRandomTask(ServerWorld w, BlockPos pos, BlockState state) {
		addTask(w, pos, state, Type.RANDOM);
		//w.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 2);
	}

	public static void addUpdateTask(ServerWorld w, BlockPos pos, BlockState state) {		
		addTask(w, pos, state, Type.UPDATE);
	}

	public static void addNeighborTask(ServerWorld w, BlockPos pos, BlockState state) {		
		addTask(w, pos, state, Type.NEIGHBOR);
		//w.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 2);
	}
}