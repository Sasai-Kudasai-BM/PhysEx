package net.skds.physex.blockphysics;

import java.util.HashSet;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.skds.physex.util.pars.BlockPhysicsPars;

public class BFSnapshot {

	//public BFSnapshot[] nbs = new BFSnapshot[6];
	public final BlockState stste;
	public final BlockPhysicsPars bfp;
	public final BlockPos pos;
	public final HashSet<BlockPos> posFrom = new HashSet<>();

	public BFSnapshot(BlockState state, BlockPos pos, BlockPhysicsPars pars, BFSnapshot snapFrom) {
		this.stste = state;
		this.bfp = pars;
		this.pos = pos;
		this.posFrom.addAll(snapFrom.posFrom);
		this.posFrom.add(snapFrom.pos);
		//addNB(snapFrom, snapFrom.pos);
	}

	//public void addNB(BFSnapshot snap, Direction dir) {
	//	int i = dir.getIndex();
	//	nbs[i] = snap;
	//}

	//public void addNB(BFSnapshot snap, BlockPos pos2) {
	//	Direction dir = Direction.getFacingFromVector(pos2.getX() - pos.getX(), pos2.getY() - pos.getY(), pos2.getZ() - pos.getZ());
	//	addNB(snap, dir);
	//}	
}