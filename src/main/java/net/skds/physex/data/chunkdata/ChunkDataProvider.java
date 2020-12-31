package net.skds.physex.data.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.skds.physex.PhysEX;

public class ChunkDataProvider implements ICapabilitySerializable<CompoundNBT> {

	public static final ResourceLocation CHUNK_RL = new ResourceLocation(PhysEX.MOD_ID, "ffldata");
	
	@CapabilityInject(ChunkData.class)
	public static final Capability<ChunkData> CHUNK_CAPABILITY = null;
	private ChunkData instance = CHUNK_CAPABILITY.getDefaultInstance();

	public final LazyOptional<ChunkData> LOPG; //some reference...

	
	public ChunkDataProvider() {
		LOPG = LazyOptional.of(() -> instance);
	}

	public void init(AttachCapabilitiesEvent<Chunk> e) {
		e.addCapability(CHUNK_RL, this);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		return CHUNK_CAPABILITY.orEmpty(cap, LOPG);
	}

	@Override
	public CompoundNBT serializeNBT() {
		return (CompoundNBT) CHUNK_CAPABILITY.getStorage().writeNBT(CHUNK_CAPABILITY, this.instance, null);
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		CHUNK_CAPABILITY.getStorage().readNBT(CHUNK_CAPABILITY, this.instance, null, nbt);
	}
}