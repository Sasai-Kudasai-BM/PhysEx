package net.skds.physex.fluidphysics;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class FluidTasksManager {

	public static Map<IWorld, WorldWorkSet> FWS = new HashMap<>();

	private static boolean tickdrop = true;
	private static long initT = System.nanoTime();
	private static int lastTime = 0;
	private static int lastFinTime = 0;

	public static void addFluidTask(World w, BlockPos pos, BlockState state) {
		WorldWorkSet wws = FWS.get(w);
		wws.addTask(w, state.getFluidState().getFluid(), pos);
		if (tickdrop) {
			lastFinTime = lastTime;
			tickdrop = false;
			initT = System.nanoTime();
		} else {
			lastTime = (int) (System.nanoTime() - initT);
		}
	}

	public static void loadWorld(IWorld w) {
		int threads = Runtime.getRuntime().availableProcessors();
		threads = 1;

		FluidWorker[] fwList = new FluidWorker[threads];
		for (int i = 0; i < threads; ++i) {
			fwList[i] = new FluidWorker(i, w, FluidWorker.Mode.DEFAULT);
		}

		FluidWorker[] fwListEQ = new FluidWorker[threads];
		for (int i = 0; i < threads; ++i) {
			fwListEQ[i] = new FluidWorker(i, w, FluidWorker.Mode.EQUALIZER);
		}
		FWS.put(w, new WorldWorkSet(w, fwList, fwListEQ));
	}

	public static int getLastTime() {
		return lastFinTime;
	}
	public static int getLastTimeMicros() {
		return lastFinTime / 1000;
	}

	public static void unloadWorld(IWorld w) {
		FWS.get(w).close();
	}

	public static void init() {
	}

	public static void tickIn() {
		tickdrop = true;
		FWS.forEach((w, wws) -> {
			wws.tickIn();
		});
	}

	public static void tickOut() {
		FWS.forEach((w, wws) -> {
			wws.tickOut();
		});
	}
}