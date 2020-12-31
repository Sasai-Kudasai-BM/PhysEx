package net.skds.physex.data.bucketdata;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.skds.physex.fluidphysics.FFLEntry;

public class AdvancedBucketDataStorage implements IStorage<AdvancedBucketData> {

	@Override
	public INBT writeNBT(Capability<AdvancedBucketData> capability, AdvancedBucketData instance, Direction side) {
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
	public void readNBT(Capability<AdvancedBucketData> capability, AdvancedBucketData instance, Direction side, INBT nbt) {
		ListNBT listNBT = (ListNBT) ((CompoundNBT) nbt).get("FFLEntries");
		if (listNBT != null) {
			for (int i = 0; i < listNBT.size(); ++i) {
				instance.putFFLEntry(new FFLEntry(listNBT.getCompound(i)));
			}
		}
	}
}