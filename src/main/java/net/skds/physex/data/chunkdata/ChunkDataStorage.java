package net.skds.physex.data.chunkdata;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.skds.physex.fluidphysics.FFLEntry;

public class ChunkDataStorage implements IStorage<ChunkData> {

	@Override
	public INBT writeNBT(Capability<ChunkData> capability, ChunkData instance, Direction side) {
		CompoundNBT nbt = new CompoundNBT();

		ListNBT listNBT = new ListNBT();
		Int2ObjectOpenHashMap<FFLEntry> entryList = instance.getFFLEntryList();
		entryList.forEach((i, e) -> {
			listNBT.add(e.serialize());
		});
		nbt.put("FFLEntries", listNBT);
		return nbt;
	}

	@Override
	public void readNBT(Capability<ChunkData> capability, ChunkData instance, Direction side, INBT nbt) {
		ListNBT listNBT = (ListNBT) ((CompoundNBT) nbt).get("FFLEntries");
		if (listNBT != null) {
			for (int i = 0; i < listNBT.size(); ++i) {
				instance.putFFLEntry(new FFLEntry(listNBT.getCompound(i)));
			}
		}
	}
}