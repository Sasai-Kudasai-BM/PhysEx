package net.skds.physex.util.blockupdate;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.mixins.other.ServerWorldAccessor;
import net.skds.physex.util.Interface.IServerChunkProvider;

public abstract class BasicExecutor implements Runnable {

	protected final BlockState nullreturnstate = Blocks.BARRIER.getDefaultState();
	protected final ServerWorld w;
	protected Set<BlockPos> banPoses = new HashSet<>();
	protected boolean cancel = false;

	protected BasicExecutor(ServerWorld w) {
		this.w = w;
	}

	protected BlockState setState(BlockPos pos, BlockState newState) {
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
		return oldState;
	}

	private IChunk chunkCash = null;
	private long chunkPosCash = 0;
	private boolean newChunkCash = true;

	protected IChunk getChunk(int blockX, int blockZ) {
		long lpos = ChunkPos.asLong(blockX >> 4, blockZ >> 4);
		if (newChunkCash || lpos != chunkPosCash) {
			newChunkCash = false;
			ServerChunkProvider prov = (ServerChunkProvider) w.getChunkProvider();
			chunkCash = ((IServerChunkProvider) prov).getCustomChunk(lpos);
			chunkPosCash = lpos;
		}
		return chunkCash;
	}

	protected IChunk getChunk(BlockPos cPos) {
		return getChunk(cPos.getX(), cPos.getZ());
	}

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
	}

	// ============== ENTITY ================= //

	protected void addEntity(Entity e) {
		BlockUpdataer.addEntity(e);
		/*
		if (((ServerWorldAccessor) w).hasDuplicateEntityInv(entityIn)) {
			return false;
		} //else {
		// if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new
		// net.minecraftforge.event.entity.EntityJoinWorldEvent(entityIn, this))) return
		// false;
		IChunk ichunk = this.getChunk((int) Math.floor(entityIn.getPosX()), (int) Math.floor(entityIn.getPosZ()));

		if (!(ichunk instanceof Chunk)) {
			return false;
		} else {
			ichunk.addEntity(entityIn);
			((ServerWorldAccessor) w).onEntityAddedInv(entityIn);
			return true;
		}
		// }
		*/
	}
}