package net.skds.physex.fluidphysics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEXConfig;

public class FFluidEQv2 extends FFluidBasic {

	Long2ObjectOpenHashMap<SelectedPos> currentIterator = new Long2ObjectOpenHashMap<>();
	Long2ObjectOpenHashMap<SelectedPos> nextIterator = new Long2ObjectOpenHashMap<>();
	HashSet<SelectedPos> setblockList = new HashSet<>();

	Int2ObjectOpenHashMap<HashSet<SelectedPos>> hsort = new Int2ObjectOpenHashMap<>();
	ArrayList<Integer> hsi = new ArrayList<>();

	HashSet<Long> ignore = new HashSet<>();

	final int celi;
	final int floor;
	final int eqDist;
	final int initL;
	final IntComparator ic = new IntComparator() {
		@Override
		public int compare(int k1, int k2) {
			return k1 - k2;
		}
	};

	int Hsum;
	int Hcount;
	int Hmid;
	int maxY;
	int minY;

	FFluidEQv2(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		super(w, pos, fwrkr, mode);
		this.initL = getAbsoluteLevel(pos.getY(), level);
		this.minY = initL;
		this.maxY = initL;
		this.celi = initL + 1;
		this.floor = initL - 1;
		this.eqDist = PhysEXConfig.COMMON.maxEqDist.get();
		Hcount = 1;
		Hsum = initL;
		Hmid = initL;
	}

	@Override
	public void execute() {
		if (getBlockState(pos.up()).getFluidState().isEmpty() && !FFluidStatic.canOnlyFullCube(state)
				&& !canPass(pos, pos.down(), state, getBlockState(pos.down()))) {
			// equalize();
			eq();
		}
	}

	private void eq() {
		/// long l = System.nanoTime();
		currentIterator.put(longpos, new SelectedPos(pos, state, level));
		goEQ();
		// System.out.println("x======================================x");
		hsi.sort(ic);
		if (maxY > minY + 1)
			iterate2();
		// System.out.println(1000000F/(System.nanoTime() - l) + " Poses:" + Hcount);
	}

	private boolean swap(SelectedPos p1, SelectedPos p2) {
		int l1 = p1.sL;
		int l2 = p2.sL;
		int delta = (l1 - l2) / 2;
		if (delta < 1) {
			return false;
		}

		l1 -= delta;
		l2 += delta;

		// System.out.println(p1.sL + " " + p2.sL);
		// setblockList.add(p1.getUpdated(l1));
		// setblockList.add(p2.getUpdated(l2));
		setState(p1.sPos, getUpdatedState(p1.sState, l1));
		setState(p2.sPos, getUpdatedState(p2.sState, l2));
		return true;
	}

	private void goEQ() {
		int iteration = 0;
		while (iteration < eqDist && !currentIterator.isEmpty()) {
			++iteration;
			currentIterator.forEach(this::eqIteration);
			currentIterator = nextIterator.clone();
			nextIterator = new Long2ObjectOpenHashMap<>();
			// if (iteration >= eqDist) {
			// System.out.println("x " + currentIterator.size());
			// }
		}
	}

	private void eqIteration(long l, SelectedPos sp) {
		if (!sp.inEmpty) {
			BlockPos posu = sp.sPos.up();
			BlockState statu = getBlockState(posu);
			if (!statu.getFluidState().isEmpty() && canPass(posu, sp.sPos, statu, sp.sState)) {
				sp = new SelectedPos(posu, statu, statu.getFluidState().getLevel());
			}
		} else {
			BlockPos posd = sp.sPos.down();
			BlockState stated = getBlockState(posd);
			if (canPass(sp.sPos, posd, sp.sState, stated)) {
				sp = new SelectedPos(posd, stated, stated.getFluidState().getLevel());
			}
		}

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos pos2 = sp.sPos.offset(dir);
			long longpos2 = pos2.toLong();
			BlockState state2 = getBlockState(pos2);
			if (validateOrIgnore(longpos2)) {
				if (canPass(sp.sPos, pos2, sp.sState, state2, sp.inEmpty)) {
					FluidState fs2 = state2.getFluidState();
					int level2 = fs2.getLevel();
					SelectedPos p2 = new SelectedPos(pos2, state2, level2);
					if (adjLev(pos2.getY(), level2)) {
						addPassedEq(longpos2);
					} else {
					}

					nextIterator.put(longpos2, p2);
					memGoodPos(l, p2);
				}
			}
		}
	}

	private void iterate2() {
		boolean stop = false;
		while (!stop && hsi.size() > 1) {
			int i = hsi.get(0);
			int i2 = hsi.get(hsi.size() - 1);
			hsi.remove(0);
			hsi.remove(hsi.size() - 1);
			HashSet<SelectedPos> set = hsort.get(i);
			Iterator<SelectedPos> set2 = hsort.get(i2).iterator();
			l1: for (SelectedPos p : set) {
				if (!set2.hasNext()) {
					if (!hsi.isEmpty()) {
						i2 = hsi.get(hsi.size() - 1);
						hsi.remove(hsi.size() - 1);
						set2 = hsort.get(i2).iterator();
					} else {
						break l1;
					}
				}
				SelectedPos p2 = set2.next();
				stop = !swap(p, p2);
			}
		}
	}

	private void memGoodPos(long l, SelectedPos p) {
		fluidWorker.clearTasks(l);
		ignore.add(l);
		int delta = initL - getAbsoluteLevel(p.sPos.getY(), p.sL);
		HashSet<SelectedPos> hs = hsort.get(delta);
		if (hs == null) {
			hsi.add(delta);
			hs = new HashSet<>();
			hsort.put(delta, hs);
		}
		hs.add(p);
	}

	private boolean adjLev(int y, int lev) {
		int i = getAbsoluteLevel(y, lev);
		maxY = Math.max(maxY, i);
		minY = Math.min(minY, i);
		Hsum += i;
		++Hcount;
		Hmid = Hsum / Hcount;
		return maxY > minY + 1;
	}

	private boolean canPass(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2) {
		boolean wp = (state2.getBlock() instanceof IWaterLoggable) ? fluid.isEquivalentTo(Fluids.WATER)
				: !state2.getFluidState().isEmpty();
		return wp && !isPassedEq(pos2) && canReach(pos1, pos2, state1, state2);
	}

	private boolean canPass(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, boolean inE) {
		boolean wp = (state2.getBlock() instanceof IWaterLoggable) ? fluid.isEquivalentTo(Fluids.WATER)
				: !(state2.getFluidState().isEmpty() && inE);
		return wp && !isPassedEq(pos2) && canReach(pos1, pos2, state1, state2);
	}

	private boolean validateOrIgnore(long l) {
		if (ignore.contains(l) || !validate(l)) {
			return false;
		}
		return true;
	}

	private class SelectedPos {
		public final BlockPos sPos;
		public final BlockState sState;
		public final int sL;
		public final boolean inEmpty;
		// public final int aL;

		private SelectedPos(BlockPos sPos, BlockState sState, int sL) {
			this.sL = sL;
			this.sPos = sPos;
			this.sState = sState;
			this.inEmpty = false;
			;
			// this.aL = getAbsoluteLevel(sPos.getY(), sL);
		}

		private SelectedPos(BlockPos sPos, BlockState sState, int sL, boolean inEmpty) {
			this.sL = sL;
			this.sPos = sPos;
			this.sState = sState;
			this.inEmpty = inEmpty;
			// this.aL = getAbsoluteLevel(sPos.getY(), sL);
		}

		@SuppressWarnings("unused")
		private SelectedPos getUpdated(int l) {
			BlockState s2 = getUpdatedState(sState, l);
			return new SelectedPos(sPos, s2, l);
		}
	}
}