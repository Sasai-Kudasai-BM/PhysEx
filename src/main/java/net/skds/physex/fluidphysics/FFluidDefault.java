package net.skds.physex.fluidphysics;

import static net.skds.physex.PhysEXConfig.MAX_FLUID_LEVEL;

import java.util.Iterator;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEXConfig;

public class FFluidDefault extends FFluidBasic {

	BlockState downstate;
	BlockState[] nbs;
	int[] nbl;
	boolean[] nbc;
	boolean dcFlag = false;
	boolean dc = false;
	boolean sc = false;
	int lmin = MAX_FLUID_LEVEL;
	int c = 0;
	int sum;

	FFluidDefault(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		super(w, pos, fwrkr, mode);

		this.sum = this.level;
		nbs = new BlockState[4];
		nbl = new int[4];
		nbc = new boolean[4];
	}

	@Override
	protected void execute() {

		BlockPos posD = pos.down();
		if (posD.getY() < 0) {
			level = 0;
			state = getUpdatedState(state, level);
			sc = true;
			setState(pos, state);
			return;
		}
		if (!validate(posD)) {
			return;
		}
		downstate = getBlockState(posD);
		if (downstate == null) {
			cancel = true;
			return;
		}
		if (canFlow(pos, posD, state, downstate, true, false)) {
			if (FFluidStatic.canOnlyFullCube(state) || FFluidStatic.canOnlyFullCube(downstate)) {
				int l = state.getFluidState().getLevel();
				int ld = downstate.getFluidState().getLevel();
				if (ld == 0 && l == MAX_FLUID_LEVEL) {
					flowFullCube(posD, downstate);
					return;
				}
			} else {
				flowDown(posD, downstate);
				addPassedEq(posD);
				setState(pos, state);
				setState(posD, downstate);
				return;
			}
		}

		if (FFluidStatic.canOnlyFullCube(state) && !dc) {
			Iterator<Direction> dirs = Direction.Plane.HORIZONTAL.iterator();
			while (dirs.hasNext()) {
				Direction dir = dirs.next();
				BlockPos pos2 = pos.offset(dir);
				if (!validate(pos2)) {
					return;
				}
				BlockState state2 = getBlockState(pos2);
				if (state2.getFluidState().isEmpty() && !FFluidStatic.canOnlyFullCube(state2)
						&& canReach(pos, pos2, state, state2)) {
					flowFullCube(pos2, state2);
					return;
				}
			}
		}

		Iterator<Direction> dirs = Direction.Plane.HORIZONTAL.iterator();
		int i = 0;
		while (dirs.hasNext()) {
			if (level <= 0) {
				break;
			}
			Direction dir = dirs.next();
			BlockPos pos2 = pos.offset(dir);
			if (!validate(pos2)) {
				return;
			}
			BlockState state2 = getBlockState(pos2);

			if (FFluidStatic.canOnlyFullCube(state2) && canFlow(pos, pos2, state, state2, true, false) && !dc) {
				BlockPos posu = pos.up();
				BlockState stateu = getBlockState(posu);
				if (stateu.getFluidState().getLevel() > 0 && canFlow(posu, pos, stateu, state, true, true)) {
					reset(-1);
					flowFullCube(pos2, state2);
					return;
				}
			}

			if (canFlow(pos, pos2, state, state2, false, false)) {
				FluidState fs2 = state2.getFluidState();
				int level2 = fs2.getLevel();
				if (level2 < lmin) {
					reset(level2);
				}
				if (level2 <= lmin && level2 < level) {
					nbs[i] = state2;
					nbl[i] = level2;
					nbc[i] = true;
					sum += level2;
					sc = true;
					++c;
				}
			}
			++i;
		}

		if (c > 0) {

			int level2 = sum / (c + 1);

			int d = sum % (c + 1);
			level = level2 + d;
			state = getUpdatedState(state, level);
			int r = -1;
			if (d > 0) {
				r = w.getRandom().nextInt(c);
			}

			i = 0;
			dirs = Direction.Plane.HORIZONTAL.iterator();
			while (dirs.hasNext()) {
				Direction dir = dirs.next();
				BlockPos pos2 = pos.offset(dir);
				if (nbc[i]) {
					if (r == 0 && canFlow(pos, pos2, state, nbs[i], false, false)) {
						nbs[i] = getUpdatedState(nbs[i], level2);
						flowTo(pos2, nbs[i], i);
					} else {
						nbs[i] = getUpdatedState(nbs[i], level2);
					}
					if (!cancel && sc) {
						setState(pos2, nbs[i]);
					}
					--r;
				}
				++i;
			}
		}

		if (sc && !cancel) {
			setState(pos, state);
		}
		if (dc && sc && !cancel) {
			setState(posD, downstate);
		}

		// if (getBlockState(pos.up()).getFluidState().isEmpty() &&
		// !FFluidStatic.canOnlyFullCube(state)
		// && !canFlow(pos, posD, state, downstate, true, false) && !cancel)

		//System.out.println(state.getFluidState() + "  " + sc);
		if (getBlockState(pos.up()).getFluidState().isEmpty() && !FFluidStatic.canOnlyFullCube(state) && !dc && !sc
				&& !cancel) {
			// equalize();
			fluidWorker.addEQTask(pos.toLong());
		}
	}

	private void reset(int level2) {
		nbs = new BlockState[4];
		nbc = new boolean[4];
		nbl = new int[4];
		lmin = level2;
		sum = level;
		c = 0;
	}

	private void flowDown(BlockPos pos2, BlockState state2) {
		if (state2 == null) {
			cancel = true;
			return;
		}
		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();
		if (!fluid.isEquivalentTo(fs2.getFluid())) {
			level2 = 0;
		}

		dcFlag = level > 1;

		level2 += level;
		if (level2 > MAX_FLUID_LEVEL) {
			dcFlag = false;
			level = level2 - MAX_FLUID_LEVEL;
			level2 = MAX_FLUID_LEVEL;
		} else {
			level = 0;
		}
		if (dcFlag) {
			++level;
			--level2;
		}
		state = getUpdatedState(state, level);
		downstate = getUpdatedState(state2, level2);
		sc = true;
		dc = true;
	}

	private void flowTo(BlockPos pos2, BlockState state2, int side) {
		if (state2 == null) {
			cancel = true;
			return;
		}
		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();
		if (!fluid.isEquivalentTo(fs2.getFluid())) {
			level2 = 0;
		}

		if (level > level2 + 1) {
			int delta = (level - level2) / 2;
			level -= delta;
			level2 += delta;
			state = getUpdatedState(state, level);
			nbs[side] = getUpdatedState(state2, level2);
			nbc[side] = true;
			sc = true;
		} else if (level == 1 && level2 == 0) {
			level2 = level;
			level = 0;
			state = getUpdatedState(state, level);
			nbs[side] = getUpdatedState(state2, level2);
			nbc[side] = true;
			sc = true;
		}

	}

	private boolean canFlow(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, boolean down,
			boolean ignoreLevels) {
		if (state2 == null) {
			cancel = true;
			return false;
		}
		if ((FFluidStatic.canOnlyFullCube(state2) || FFluidStatic.canOnlyFullCube(state)) && !down) {
			return false;
		}
		if (FFluidStatic.canOnlyFullCube(state2) && state1.getFluidState().getLevel() < PhysEXConfig.MAX_FLUID_LEVEL) {
			return false;
		}

		//if ((state1.getBlock() instanceof IWaterLoggable || state2.getBlock() instanceof IWaterLoggable)
		//		&& !(fluid instanceof WaterFluid)) {
		//	return false;
		//}

		if (!canReach(pos1, pos2, state1, state2)) {
			return false;
		}

		FluidState fs2 = state2.getFluidState();
		// if ((!fs2.isEmpty() && !isThisFluid(fs2.getFluid())) &&
		// !state1.getFluidState().canDisplace(w, pos2,
		// state2.getFluidState().getFluid(), FFluidStatic.dirFromVec(pos1, pos2)))
		// return false;

		int level2 = fs2.getLevel();
		if (level2 >= MAX_FLUID_LEVEL && !ignoreLevels && fluid.isEquivalentTo(fs2.getFluid())) {
			return false;
		}

		if (level == 1 && !down && !ignoreLevels) {
			if (fs2.isEmpty()) {
				pos1 = pos2;
				pos2 = pos2.down();
				state1 = state2;
				state2 = getBlockState(pos2);
				if (isThisFluid(state2.getFluidState().getFluid()) || state2.getFluidState().isEmpty()) {
					return canFlow(pos1, pos2, state1, state2, true, false);
				} else {
					return false;
				}
			} else {
				return (level2 + 2 < level);
			}
		} else if (!down && level2 + 1 >= level ) {
			return false;
		}

		return true;
	}
}