package net.skds.physex.fluidphysics;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.util.blockupdate.BlockUpdataer;
import net.skds.physex.util.Interface.IServerChunkProvider;

public abstract class FFluidBasic implements Runnable {

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
	protected boolean cancel = false;
	protected Set<BlockPos> banPoses = new HashSet<>();

	protected FFluidBasic(ServerWorld w, BlockPos pos, FluidWorker fwrkr, FluidWorker.Mode mode) {
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

			case GSS:
				return new FFluidGSS(w, pos, fwrkr, mode);

			default:
				return new FFluidDefault(w, pos, fwrkr, mode);
		}
	}

	@Override
	public void run() {
		//System.out.println(pos);
		if (validate(pos) && level > 0) {
			execute();
		}
		fluidWorker.unbanPoses(banPoses);
		//if (cancel) {
		//	System.out.println("CCCC");
		//}
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

	// ================ WORLD STAFF ================== //

	protected void setState(BlockPos pos, BlockState newState) {
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

			if ((newState.getFluidState().isEmpty() ^ oldState.getFluidState().isEmpty())
					&& (newState.getOpacity(w, pos) != oldState.getOpacity(w, pos)
							|| newState.getLightValue(w, pos) != oldState.getLightValue(w, pos)
							|| newState.isTransparent() || oldState.isTransparent())) {
				w.getChunkProvider().getLightManager().checkBlock(pos);
			}

			BlockUpdataer.addUpdate(w, pos, newState, oldState, 3);
		}
	}

	private IChunk chunkCash = null;
	private long chunkPosCash = 0;
	private boolean newChunkCash = true;

	protected IChunk getChunk(BlockPos cPos) {
		long lpos = ChunkPos.asLong(cPos.getX() >> 4, cPos.getZ() >> 4);
		if (newChunkCash || lpos != chunkPosCash) {
			newChunkCash = false;
			ServerChunkProvider prov = (ServerChunkProvider) w.getChunkProvider();
			chunkCash = ((IServerChunkProvider) prov).getCustomChunk(lpos);
			chunkPosCash = lpos;
		}
		return chunkCash;
	}

	protected final BlockState nullreturnstate = Blocks.BARRIER.getDefaultState();

	protected BlockState getBlockState(BlockPos pos) {
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

	protected BlockState setFinBlockState(BlockPos pos, BlockState state) {
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

		fluidWorker.setblockCheck();
		return setted;

		// return null;
		// */
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