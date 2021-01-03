package net.skds.physex.fluidphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.util.blockupdate.BasicExecutor;

public abstract class FFluidBasic extends BasicExecutor {

	protected final FluidWorker fluidWorker;
	protected final FluidWorker.Mode mode;
	protected final int MFL = PhysEXConfig.MAX_FLUID_LEVEL;
	protected final Fluid fluid;
	protected final ServerWorld w;
	protected final BlockPos pos;
	protected final long longpos;

	protected int level = 0;
	protected FluidState fs;
	protected BlockState state;

	protected FFluidBasic(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		super(w);
		this.fluidWorker = fwrkr;
		this.w = w;
		this.mode = mode;
		this.state = getBlockState(pos);
		this.fs = this.state.getFluidState();
		this.fluid = fs.getFluid();
		this.pos = pos;
		this.longpos = pos.toLong();
		this.level = fs.getLevel();
	}

	protected int getAbsoluteLevel(int y, int l) {
		return (y * MFL) + l;
	}

	public static Runnable generate(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		switch (mode) {

			case DEFAULT:
				return new FFluidDefault(w, pos, fwrkr, mode);

			case EQUALIZER:
				return new FFluidEQ(w, pos, fwrkr, mode);

			default:
				return new FFluidDefault(w, pos, fwrkr, mode);
		}
	}

	@Override
	public void run() {
		if (validate(pos) && level > 0 && (fluid instanceof FlowingFluid)) {
			execute();
		}
		fluidWorker.unbanPoses(banPoses);
	}

	protected abstract void execute();

	protected boolean canOnlyFillCube(BlockState bs) {
		return FFluidStatic.canOnlyFullCube(bs);
	}

	protected boolean canOnlyFillCube(Block b) {
		return FFluidStatic.canOnlyFullCube(b);
	}

	protected boolean validate(BlockPos p) {
		boolean ss = fluidWorker.banPos(p);
		if (ss) {
			banPoses.add(p);
		} else {
			// fluidWorker.addNTTask(p.toLong(), 1);
		}
		return ss;
	}

	protected boolean validate(long p) {
		boolean ss = fluidWorker.banPos(p);
		if (ss) {
			banPoses.add(BlockPos.fromLong(p));
		} else {
			// fluidWorker.addNTTask(p.toLong(), 1);
		}
		return ss;
	}

	protected void addPassedEq(BlockPos addPos) {
		long l = addPos.toLong();
		fluidWorker.addEqLock(l);
		fluidWorker.addNTTask(l, FFluidStatic.getTickRate((FlowingFluid) fluid, w));
	}

	protected void addPassedEq(long l) {
		fluidWorker.addEqLock(l);
		fluidWorker.addNTTask(l, FFluidStatic.getTickRate((FlowingFluid) fluid, w));
	}

	protected boolean isPassedEq(BlockPos isPos) {
		long l = isPos.toLong();
		return fluidWorker.isEqLocked(l);
	}

	protected void flowFullCube(BlockPos pos2, BlockState state2) {
		if (state2 == null) {
			cancel = true;
			return;
		}
		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();

		state = getUpdatedState(state, level2);
		state2 = getUpdatedState(state2, level);
		setState(pos, state);
		setState(pos2, state2);
	}

	protected BlockState getUpdatedState(BlockState state0, int newLevel) {
		return FFluidStatic.getUpdatedState(state0, newLevel, fluid);
	}


	// ================ UTIL ================== //

	protected boolean isThisFluid(Fluid f2) {
		if (fluid == Fluids.EMPTY)
			return false;
		if (f2 == Fluids.EMPTY)
			return false;
		return fluid.isEquivalentTo(f2);
	}

	protected boolean canReach(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2) {
		if (state1 == nullreturnstate || state2 == nullreturnstate) {
			return false;
		}
		return FFluidStatic.canReach(pos1, pos2, state1, state2, fluid, w);
	}

}