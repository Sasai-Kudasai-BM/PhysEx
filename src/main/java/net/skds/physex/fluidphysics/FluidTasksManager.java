package net.skds.physex.fluidphysics;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.util.blockupdate.WWSGlobal;

public class FluidTasksManager {

	private static boolean tickdrop = true;
	private static long initT = System.nanoTime();
	private static int lastTime = 0;
	private static int lastFinTime = 0;

	public static void addFluidTask(ServerWorld w, BlockPos pos, BlockState state) {
		WorldWorkSet wws = WWSGlobal.get(w).fluids;
		wws.addTask(w, state.getFluidState().getFluid(), pos);
		if (tickdrop) {
			lastFinTime = lastTime;
			tickdrop = false;
			initT = System.nanoTime();
		} else {
			lastTime = (int) (System.nanoTime() - initT);
		}
	}

	public static int getLastTime() {
		return lastFinTime;
	}
	public static int getLastTimeMicros() {
		return lastFinTime / 1000;
	}
}