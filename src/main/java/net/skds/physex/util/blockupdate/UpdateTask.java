package net.skds.physex.util.blockupdate;

import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.fluidphysics.FFluidStatic;
import net.skds.physex.util.Interface.IFlowingFluid;

public class UpdateTask {
	private final BlockPos pos;
	private BlockState newState;
	private final BlockState oldState;
	private final int flags;

	UpdateTask(BlockPos pos, BlockState newState, BlockState oldState, int flags) {
		this.pos = pos;
		this.flags = flags;
		this.newState = newState;
		this.oldState = oldState;
	}

	public void newState(BlockState ns) {
		newState = ns;
	}

	@SuppressWarnings("deprecation")
	public void update(ServerWorld w) {
		//System.out.println("x");
		Fluid fluid = newState.getFluidState().getFluid();
		if (fluid != Fluids.EMPTY && !oldState.isAir()
				&& !FFluidStatic.isSameFluid(fluid, oldState.getFluidState().getFluid())
				&& !(oldState.getBlock() instanceof IWaterLoggable)) {
			((IFlowingFluid) fluid).beforeReplacingBlockCustom(w, pos, oldState);
		}
		if (newState != oldState) {
			w.markAndNotifyBlock(pos, w.getChunkAt(pos), newState, newState, flags, 512);
		}
	}
}