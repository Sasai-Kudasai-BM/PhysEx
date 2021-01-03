package net.skds.physex.blockphysics;

import net.minecraft.util.math.BlockPos;

public class BFTask {

	private final Type type;
	private final BlockPos pos;

	public BFTask(Type type, BlockPos pos) {
		this.type = type;
		this.pos = pos;
	}

	public Type getType() {
		return type;
	}
	
	public BlockPos getPos() {
		return pos;
	}

	public static enum Type {
		UPDATE, RANDOM, NEIGHBOR, WEAK;
	}	
}