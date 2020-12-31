package net.skds.physex.data.bucketdata;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.skds.physex.fluidphysics.FFLEntry;

public class AdvancedBucketData {

	private Int2ObjectOpenHashMap<FFLEntry> entryList = new Int2ObjectOpenHashMap<>();

	public Int2ObjectOpenHashMap<FFLEntry> getFFLEntryList() {
		return entryList;
	}

	public void putFFLEntry(FFLEntry entry) {
		entryList.put(entry.getIntPos(), entry);
	}
}