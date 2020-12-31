package net.skds.physex.fluidphysics;

import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class FFluidGSS extends FFluidBasic {

	final Direction[] dirs = { Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST };
	final int y0 = pos.getY();
	final int rad = 12;

	Int2ObjectOpenHashMap<Layer> layers = new Int2ObjectOpenHashMap<>();
	Set<BlockPos> passed = new HashSet<>();
	Set<Passing> passing = new HashSet<>();
	Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
	int fluidAmount = 0;
	int volume = 0;
	double midX;
	double midZ;
	BlockPos midPos;
	int midCount = 0;
	int downLayer = 0;

	int maxL;
	int minL;
	boolean triggered = false;

	FFluidGSS(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		super(w, pos, fwrkr, mode);

		int h = (pos.getY() * MFL) + level;
		maxL = h;
		minL = h;
	}

	@Override
	protected void execute() {
		if (canOnlyFillCube(state)) {
			return;
		}
		collectPlaces(rad);
		if (maxL - 1 > minL) {

			// System.out.println(maxL + " " + minL + " " + pos);
			avgMid();
			fillPlaces();
		}
	}

	private void collectPlaces(int r) {
		boolean stop = false;
		int way = 0;

		passing.add(new Passing(pos, state));
		// addPlace(new Place(pos, state));
		collectPlace(pos, pos, fs, state);

		while (!stop && way < r) {
			++way;
			Set<Passing> passing2 = new HashSet<>();
			for (Passing pas : passing) {
				BlockPos posn = pas.pos;
				BlockState staten = pas.state;
				FluidState fsn = staten.getFluidState();
				for (Direction dir : dirs) {
					BlockPos pos2 = posn.offset(dir);
					long longpos2 = pos2.toLong();
					fluidWorker.clearTasks(longpos2);
					if (passed.contains(pos2) || !validate(pos2)) {
						passed.add(pos2);
						continue;
					}
					BlockState state2 = getBlockState(pos2);
					FluidState fs2 = state2.getFluidState();
					if (canReach(posn, pos2, staten, state2)
							&& ((fs2.isEmpty() && !fsn.isEmpty()) || fs2.getFluid().isEquivalentTo(fluid))) {

						if (canOnlyFillCube(state2)) {
							continue;
						}
						boolean flag = fsn.getLevel() >= MFL && posn.getY() != y0;
						collectPlace(posn, pos2, fs2, state2, !flag);
						passing2.add(new Passing(pos2, state2));
					}
				}

				BlockPos posnd = posn.down();
				BlockState statend = getBlockState(posnd);
				FluidState fsnd = statend.getFluidState();
				if (canReach(posn, posnd, staten, statend) && (fsnd.isEmpty() || fsnd.getFluid().isEquivalentTo(fluid))
						&& !canOnlyFillCube(statend)) {
					if (statend.getFluidState().getLevel() < MFL || fsn.isEmpty()) {
						collectPlace(posn, posnd, fsnd, statend);

					}
					passing2.add(new Passing(posnd, statend));
				}
			}
			passing = passing2;
		}
	}

	private static class Passing {
		private final BlockPos pos;
		private final BlockState state;

		Passing(BlockPos pos, BlockState state) {
			this.pos = pos;
			this.state = state;
		}
	}

	private void adjMid(BlockPos p, int lvl) {
		++midCount;
		float mp = ((float) lvl) / MFL;
		mp += p.getY();
		midX += mp * p.getX();
		midZ += mp * p.getZ();
	}

	private void avgMid() {
		midPos = new BlockPos(midX / midCount, y0, midZ / midCount);
	}

	private void collectPlace(BlockPos pos1, BlockPos pos2, FluidState fs2, BlockState state2, boolean... pars) {
		if (passed.add(pos2)) {
			int level2 = fs2.getLevel();
			adjMid(pos2, level2);
			fluidAmount += level2;
			volume += MFL;

			int h = (pos2.getY() * MFL) + level2;
			maxL = Math.max(maxL, h);
			if (pars.length == 0 || pars[0])
				minL = Math.min(minL, h);
			addPlace(new Place(pos2, state2));
		}
	}

	private void addPlace(Place place) {
		int index = y0 - place.p.getY();
		downLayer = Math.max(downLayer, index);
		Layer layer = layers.get(index);
		if (layer == null) {
			layer = new Layer();
			layers.put(index, layer);
		}
		layer.add(place);
	}

	private void fillPlaces() {
		for (int i = downLayer; i >= 0; --i) {
			Layer layer = layers.get(i);
			// final int i2 = i;
			if (layer == null) {
				return;
			}
			fluidAmount = layer.calcVolume(fluidAmount);
			layer.places.forEach(place -> {
				fillPlace(layer.level, place);
				// System.out.println("Layer: " + i2 + " " + place.p);
			});
		}
	}

	private void fillPlace(int l, Place place) {
		int l2 =  place.isExtra ? l + 1 : l;
		if (place.ol == l2) {
			return;
		}
		BlockState st = getUpdatedState(place.s, l2);		
		setState(place.p, st);
	}

	// private int getDist2MidSq(BlockPos p) {
	// int dx = p.getX() - midPos.getX();
	// int dz = p.getZ() - midPos.getZ();
	// return (dx * dx) + (dz *dz);
	// }

	private class Layer {
		public final HashSet<Place> places = new HashSet<>();
		public int level = 0;

		public void add(Place p) {
			places.add(p);
		}

		public int calcVolume(int v) {
			int sz = places.size();
			int vol = sz * MFL;
			if (v >= vol) {
				level = MFL;
				return v - vol;
			}
			level = v / sz;
			int extraV = v % sz;
			//if (v > 0) {
			//	System.out.println(level + " v:" + v + " sz:" + sz + " extraV:" + extraV);
			//}
			if (extraV != 0) {
				findExtraPlaces(extraV);
			}
			return 0;
		}

		private void findExtraPlaces(int count) {			
			for (Place place : places) {
				if (level < MFL) {
					--count;
					place.isExtra = true;
					// setState(place.p, Blocks.LAPIS_BLOCK.getDefaultState());
				}
				if (count <= 0) {
					return;
				}
			}
		}
	}

	private static class Place {
		public final BlockPos p;
		public final BlockState s;
		public final int ol;
		public boolean isExtra;

		public Place(BlockPos p, BlockState s) {
			this.p = p;
			this.s = s;
			this.ol = this.s.getFluidState().getLevel();
		}
	}
}