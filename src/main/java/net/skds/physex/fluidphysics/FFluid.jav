package net.skds.physex.fluidphysics;

import static net.skds.physex.PhysEXConfig.COMMON;
import static net.skds.physex.PhysEXConfig.MAX_FLUID_LEVEL;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.util.BlockUpdataer;
import net.skds.physex.util.Interface.IServerChunkProvider;

public class FFluid {

	final FluidWorker fluidWorker;
	final FluidWorker.Mode mode;

	int level = 0;
	FluidState fs;
	BlockState state;
	BlockState downstate;
	World w;
	BlockPos pos;
	Fluid fluid;
	BlockState[] nbs;
	int[] nbl;
	boolean[] nbc;
	boolean dc = false;
	boolean sc = false;
	boolean cancel = false;
	int lmin = MAX_FLUID_LEVEL;
	int c = 0;
	int sum;
	Set<BlockPos> banPoses = new HashSet<>();

	FFluid(World w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
		this.fluidWorker = fwrkr;
		this.w = w;
		this.mode = mode;
		this.state = getBlockState(pos);
		if (this.state != null) {
			this.fs = this.state.getFluidState();
			this.fluid = fs.getFluid();
			this.pos = pos;
			this.level = fs.getLevel();
			this.sum = this.level;

			if (level == 0) {
				return;
			}
			if (validate(pos)) {
				if (fluidWorker.getSqDistToNBP(pos) > (16 * 1000)) {
					GSS();
					// System.out.println("x");
					fluidWorker.unbanPoses(banPoses);
					return;
				}
				if (mode == FluidWorker.Mode.EQUALIZER) {
					if (getBlockState(pos.up()).getFluidState().isEmpty() && !FFluidStatic.canOnlyFullCube(state)
							&& !canFlow(pos, pos.down(), state, getBlockState(pos.down()), true, false))
						equalize();
				} else {
					tryFlow();
				}
				fluidWorker.unbanPoses(banPoses);
			}
		}
	}

	private boolean validate(BlockPos p) {
		boolean ss = fluidWorker.banPos(p);
		if (ss) {
			banPoses.add(p);
		} else {
			// fluidWorker.addNTTask(p.toLong(), 1);
		}
		return ss;
	}

	public void tryFlow() {
		nbs = new BlockState[4];
		nbl = new int[4];
		nbc = new boolean[4];

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

		if (getBlockState(pos.up()).getFluidState().isEmpty() && !FFluidStatic.canOnlyFullCube(state)
				&& !canFlow(pos, posD, state, downstate, true, false) && !cancel) {
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

	public void equalize() {
		boolean slide = PhysEXConfig.COMMON.slide.get();
		// boolean slide = false;
		// setState(pos.add(0, 16, 0), Blocks.STONE.getDefaultState());
		boolean slided = false;
		int i0 = w.getRandom().nextInt(4);
		if (slide && !canReach(pos, pos.down(), state, getBlockState(pos.down())) && level == 1) {
			slided = slide();
		}
		if (!slided) {
			for (int index = 0; index < 4; ++index) {
				if (level <= 0) {
					break;
				}
				if (cancel) {
					return;
				}
				Direction dir = Direction.byHorizontalIndex((index + i0) % 4);
				equalizeLine(dir, false, COMMON.maxEqDist.get());
			}
		}
	}

	public boolean slide() {
		// setState(pos.add(0, 16, 0), Blocks.STONE.getDefaultState());
		// System.out.println("x");
		int slideDist = PhysEXConfig.COMMON.maxSlideDist.get();
		int lenmin = slideDist;

		boolean selPosb = false;
		BlockPos selPos = pos;
		BlockState selState = state;

		boolean[] diag2 = { false, true };

		/// System.out.println("len");
		for (Direction dir : FFluidStatic.getRandomizedDirections(w.getRandom(), false)) {
			for (boolean diag : diag2) {

				boolean selPosb2 = false;
				BlockPos selPos2 = pos;
				int dist = 0;
				int len = lenmin;
				BlockPos pos2 = pos;
				BlockPos pos1 = pos;
				boolean cont = true;
				boolean side = false;
				BlockState state1 = state;
				BlockState state2 = state;
				boolean bl = false;

				// System.out.println(len);
				wh: while (cont && len > 0) {
					pos1 = pos2;
					state1 = state2;
					if (diag) {
						if (side) {
							dir = dir.rotateY();
							side = !side;
						} else {
							dir = dir.rotateYCCW();
							side = !side;
						}
					}
					pos2 = pos1.offset(dir);
					state2 = getBlockState(pos2);
					FluidState fs2 = state2.getFluidState();
					if (canReach(pos1, pos2, state1, state2)
							&& (fs2.isEmpty() || (fs2.getLevel() < 2 && fs2.getFluid().isEquivalentTo(fluid)))) {
						if ((state1.getBlock() instanceof IWaterLoggable || state2.getBlock() instanceof IWaterLoggable)
								&& !(fluid instanceof WaterFluid)) {
							break wh;
						}
						if (dist > 0 && !selPosb2 && fs2.isEmpty()) {
							selPosb2 = true;
							selPos2 = pos1;
						}
						bl = (canFlow(pos1, pos1.down(), state1, getBlockState(pos1.down()), true, false))
								&& !FFluidStatic.canOnlyFullCube(state2);
					} else {
						break wh;
					}
					--len;
					if (bl && !cancel && selPosb2) {
						lenmin = Math.min(dist, lenmin);
						selPos = selPos2;
						selState = state1;
						selPosb = true;
					}
					++dist;
				}
			}
		}
		if (selPosb && validate(selPos)) {
			selState = getBlockState(selPos);
			selState = flowToPosEq(pos, selPos, selState, -1);
			setState(selPos, selState);
			setState(pos, state);
			return true;
		}
		return false;
	}

	void addPassedEq(BlockPos addPos) {
		long l = addPos.toLong();
		fluidWorker.addEqLock(l);
		fluidWorker.addNTTask(addPos.toLong(), FFluidStatic.getTickRate((FlowingFluid) fluid, w));
	}

	boolean isPassedEq(BlockPos isPos) {
		long l = isPos.toLong();
		return fluidWorker.isEqLocked(l);
	}

	public void equalizeLine(Direction dir, boolean diag, int len) {
		// len = (int) ((float) len * fluidWorker.eqSpeed);
		// System.out.println(fluidWorker.eqSpeed);
		// len=8;
		BlockPos pos2 = pos;
		BlockPos pos1 = pos;
		int len2 = len;
		boolean cont = true;
		boolean side = false;
		BlockState state1 = state;
		BlockState state2 = state;
		int hmod = 0;
		boolean bl = false;

		boolean blocked = false;

		while (cont && len > 0) {

			// if (diag) setState(pos1.down(), Blocks.BIRCH_LOG.getDefaultState());
			// setState(pos1.add(0, 16, 0), Blocks.STONE.getDefaultState());

			if (!diag && len2 - len == 1) {
				equalizeLine(dir, true, len);
			}

			if (diag) {
				if (side) {
					dir = dir.rotateY();
					side = !side;
				} else {
					dir = dir.rotateYCCW();
					side = !side;
				}
			}
			pos1 = pos2;
			state1 = state2;

			BlockPos pos1u = pos1.up();
			BlockState state1u = getBlockState(pos1u);
			FluidState fs1u = state1u.getFluidState();

			if (!blocked && canReach(pos1u, pos1, state1u, state1)
					&& (!fs1u.isEmpty() && isThisFluid(fs1u.getFluid()))) {
				// state1 = state1u;
				// System.out.println("x");
				pos2 = pos1u;
				state2 = state1u;
				++hmod;
				bl = true;
			} else {
				pos2 = pos1.offset(dir);
				state2 = getBlockState(pos2);
			}

			FluidState fs2 = state2.getFluidState();

			if (isPassedEq(pos2)) {
				// fluidWorker.addNTTask(pos2.toLong(), FFluidStatic.getTickRate((FlowingFluid)
				// fluid, w));
				// fluidWorker.addNTTask(pos.toLong(), FFluidStatic.getTickRate((FlowingFluid)
				// fluid, w));
				// FluidTasksManager.addNTTask(w, pos1, FFluidStatic.getTickRate((FlowingFluid)
				// fluid, w));
				// System.out.println(pos2);
				break;
			}

			if (canReach(pos1, pos2, state1, state2)
					&& (!fs2.isEmpty() && isThisFluid(fs2.getFluid()) || (fs2.isEmpty() && level > 1))) {
				if ((state1.getBlock() instanceof IWaterLoggable || state2.getBlock() instanceof IWaterLoggable)
						&& !(fluid instanceof WaterFluid)) {
					// System.out.println("dd");
					break;
				}
				bl = true;
				blocked = false;

			} else {
				// pos1 = pos2;
				pos2 = pos1.down();
				state1 = state2;
				state2 = getBlockState(pos2);
				fs2 = state2.getFluidState();
				if (canReach(pos1, pos2, state1, state2)
						&& (!fs2.isEmpty() && isThisFluid(fs2.getFluid()) || fs2.isEmpty())) {
					--hmod;
					bl = true;
					blocked = true;

				} else {
					break;
				}
			}

			if (bl && !cancel && validate(pos2)) {
				int level2 = fs2.getLevel();
				int hmod2 = hmod >= 1 ? 1 : hmod <= -1 ? -1 : 0;
				if (MathHelper.abs(level2 - (hmod2 * MAX_FLUID_LEVEL) - level) > 1
						&& !FFluidStatic.canOnlyFullCube(state2)) {
					state2 = flowToPosEq(pos, pos2, state2, hmod);
					setState(pos2, state2);
					setState(pos, state);
					// System.out.println(level + " ss: " + level2 + state2);
					addPassedEq(pos2);
					return;
				}
			}
			--len;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean GSS() {
		final int MFL = MAX_FLUID_LEVEL;
		final Direction[] dirs = { Direction.DOWN, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST };
		final int y0 = pos.getY();
		final int rad = 16;

		Set<BlockPos> passed = new HashSet<>();
		Set<BlockPos> passing = new HashSet<>();
		boolean stop = false;
		Set<BlockPos>[] layers = new HashSet[rad + 1];
		for (int i = 0; i < rad + 1; ++i) {
			layers[i] = new HashSet<>();
		}
		Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
		Set<Long> u2 = new HashSet<>();

		int fluidAmount = level;
		int volume = 0;
		int way = 0;

		passing.add(pos);
		while (!stop && way < rad) {
			++way;

			Set<BlockPos> passing2 = new HashSet<>();
			for (BlockPos posn : passing) {
				passed.add(posn);
				for (Direction dir : dirs) {
					BlockPos pos2 = posn.offset(dir);
					if (passed.contains(pos2) || !validate(pos2)) {
						continue;
					}
					BlockState staten = getBlockState(posn);
					// FluidState fsn = staten.getFluidState();
					BlockState state2 = getBlockState(pos2);
					FluidState fs2 = state2.getFluidState();
					if (canReach(posn, pos2, staten, state2)
							&& (fs2.isEmpty() || fs2.getFluid().isEquivalentTo(fluid))) {
						passing2.add(pos2);
						long longpos2 = pos2.toLong();
						fluidWorker.clearTasks(longpos2);
						int layer = rad - y0 + pos2.getY();

						// System.out.println("YZ: " + y0 + " Y: " + pos2.getY() + " layer: " + layer);

						layers[layer].add(pos2);
						states.put(longpos2, state2);

						int level2 = fs2.getLevel();
						fluidAmount += level2;
						volume += MFL;
						if (way == rad) {
							u2.add(longpos2);
						}
					}
				}
			}
			passing = passing2;
		}
		for (int i = 0; i < layers.length; ++i) {
			for (BlockPos p : layers[i]) {
				long longp = p.toLong();
				int am = fluidAmount >= MFL ? MFL : fluidAmount;
				fluidAmount -= am;
				BlockState statep = states.get(longp);
				BlockState statep2 = getUpdatedState(statep, am);
				if (statep != statep2 || true) {
					if (u2.contains(longp)) {
						setState(pos, Blocks.BLUE_STAINED_GLASS.getDefaultState());
					} else {
						//BlockState oldState = setFinBlockState(pos, statep2);
						//if (oldState != null) {
						//	BlockUpdataer.addUpdate(w, pos, statep2, oldState, 50);
						//}
						
						setState(pos, Blocks.STONE.getDefaultState());
					}
				}
			}
		}

		return true;
	}

	private void flowDown(BlockPos pos2, BlockState state2) {
		if (state2 == null) {
			cancel = true;
			return;
		}
		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();

		level2 += level;
		if (level2 > MAX_FLUID_LEVEL) {
			level = level2 - MAX_FLUID_LEVEL;
			level2 = MAX_FLUID_LEVEL;
		} else {
			level = 0;
		}
		state = getUpdatedState(state, level);
		downstate = getUpdatedState(state2, level2);
		sc = true;
		dc = true;
	}

	private void flowFullCube(BlockPos pos2, BlockState state2) {
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

	/*
	 * private void flowDownStatic(BlockPos pos1, BlockPos pos2, BlockState state1,
	 * BlockState state2) { if (state2 == null) { cancel = true; return; }
	 * FluidState fs1 = state2.getFluidState(); FluidState fs2 =
	 * state2.getFluidState(); int level1 = fs1.getLevel(); int level2 =
	 * fs2.getLevel();
	 * 
	 * level2 += level1; if (level2 > MAX_FLUID_LEVEL) { level1 = level2 -
	 * MAX_FLUID_LEVEL; level2 = MAX_FLUID_LEVEL; } else { level1 = 0; } state1 =
	 * getUpdatedState(state1, level1); state2 = getUpdatedState(state2, level2);
	 * 
	 * setState(pos1, state1); setState(pos2, state2);
	 * 
	 * }
	 */

	private void flowTo(BlockPos pos2, BlockState state2, int side) {
		if (state2 == null) {
			cancel = true;
			return;
		}
		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();

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

	private BlockState flowToPosEq(BlockPos pos1, BlockPos pos2, BlockState state2, int l) {

		BlockState state2n = state2;

		FluidState fs2 = state2.getFluidState();
		int level2 = fs2.getLevel();
		int delta = (level - level2) / 2;
		// l = 0;
		if (l != 0) {
			if (l == -1) {
				level2 += level;
				if (level2 > MAX_FLUID_LEVEL) {
					level = level2 - MAX_FLUID_LEVEL;
					level2 = MAX_FLUID_LEVEL;
				} else {
					level = 0;
				}
			} else {
				// System.out.println(l);
				level += level2;
				if (level > MAX_FLUID_LEVEL) {
					level2 = level - MAX_FLUID_LEVEL;
					level = MAX_FLUID_LEVEL;
				} else {
					level2 = 0;
				}
			}
			state = getUpdatedState(state, level);
			state2n = getUpdatedState(state2, level2);

		} else if (MathHelper.abs(delta) >= 1) {

			level -= delta;
			level2 += delta;
			// System.out.println("Delta " + level + " ss: " + level2);
			state = getUpdatedState(state, level);
			state2n = getUpdatedState(state2, level2);

		} else if (level2 == 0) {
			level2 = level;
			level = 0;
			state = getUpdatedState(state, level);
			state2n = getUpdatedState(state2, level2);
		}
		return state2n;

	}

	private BlockState getUpdatedState(BlockState state0, int newLevel) {
		return FFluidStatic.getUpdatedState(state0, newLevel, fluid);
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

		if ((state1.getBlock() instanceof IWaterLoggable || state2.getBlock() instanceof IWaterLoggable)
				&& !(fluid instanceof WaterFluid)) {
			return false;
		}

		if (!canReach(pos1, pos2, state1, state2)) {
			return false;
		}

		FluidState fs2 = state2.getFluidState();
		// if ((!fs2.isEmpty() && !isThisFluid(fs2.getFluid())) &&
		// !state1.getFluidState().canDisplace(w, pos2,
		// state2.getFluidState().getFluid(), FFluidStatic.dirFromVec(pos1, pos2)))
		// return false;

		int level2 = fs2.getLevel();
		if (level2 >= MAX_FLUID_LEVEL && !ignoreLevels) {
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
		}

		return true;
	}

	// ================ WORLD STAFF ================== //

	private void setState(BlockPos pos, BlockState newState) {
		/*
		 * Sets a block state into this world.Flags are as follows: 1 will cause a block
		 * update. 2 will send the change to clients. 4 will prevent the block from
		 * being re-rendered. 8 will force any re-renders to run on the main thread
		 * instead 16 will prevent neighbor reactions (e.g. fences connecting, observers
		 * pulsing). 32 will prevent neighbor reactions from spawning drops. 64 will
		 * signify the block is being moved. Flags can be OR-ed
		 */
		BlockState oldState = setFinBlockState(pos, newState);
		if (oldState != null) {
			BlockUpdataer.addUpdate(w, pos, newState, oldState, 3);
		}
	}

	private IChunk chunkCash = null;
	private long chunkPosCash = 0;
	private boolean newChunkCash = true;

	private IChunk getChunk(BlockPos cPos) {
		long lpos = ChunkPos.asLong(cPos.getX() >> 4, cPos.getZ() >> 4);
		if (newChunkCash || lpos != chunkPosCash) {
			newChunkCash = false;
			ServerChunkProvider prov = (ServerChunkProvider) w.getChunkProvider();
			chunkCash = ((IServerChunkProvider) prov).getCustomChunk(lpos);
			chunkPosCash = lpos;
		}

		return chunkCash;
	}

	final BlockState nullreturnstate = Blocks.BARRIER.getDefaultState();

	private BlockState getBlockState(BlockPos pos) {
		IChunk chunk = getChunk(pos);
		if (chunk == null) {
			cancel = true;
			return nullreturnstate;
		}
		BlockState ssss = chunk.getBlockState(pos);
		if (ssss == null) {
			return nullreturnstate;
		}

		return ssss;
	}

	private BlockState setFinBlockState(BlockPos pos, BlockState state) {
		IChunk chunk = getChunk(pos);
		// return chunk.setBlockState(pos, state, false);
		// *
		if (!(chunk instanceof Chunk)) {
			return null;
		}
		ChunkSection[] chunksections = chunk.getSections();
		ChunkSection sec = chunksections[pos.getY() >> 4];
		if (sec == null) {
			sec = new ChunkSection(pos.getY() >> 4 << 4);
			chunksections[pos.getY() >> 4] = sec;
		}
		BlockState setted = chunksections[pos.getY() >> 4].setBlockState(pos.getX() & 15, pos.getY() & 15,
				pos.getZ() & 15, state, false);
		return setted;

		// return null;
		// */
	}

	// ================ UTIL ================== //

	public boolean isThisFluid(Fluid f2) {
		if (fluid == Fluids.EMPTY)
			return false;
		if (f2 == Fluids.EMPTY)
			return false;
		return fluid.isEquivalentTo(f2);
	}

	private boolean canReach(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2) {

		return FFluidStatic.canReach(pos1, pos2, state1, state2, fluid, w);
	}

}