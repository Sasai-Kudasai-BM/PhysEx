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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BFTask)) {
			return false;
		} else {
			BFTask task = (BFTask) obj;
			if (this.type != task.type) {
				return false;
			} else {
				return this.pos.equals(task.pos);
			}
		}
	}

	public static enum Type {
		UPDATE, RANDOM, NEIGHBOR, DOWNRAY, UPRAY;
	}
}