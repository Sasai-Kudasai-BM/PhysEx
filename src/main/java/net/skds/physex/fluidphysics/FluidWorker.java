package net.skds.physex.fluidphysics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEX;
import net.skds.physex.util.blockupdate.BlockUpdataer;

public class FluidWorker extends Thread {

	public boolean cont = true;
	public final Mode mode;
	private final boolean equalizer;
	private final WorldWorkSet owner;

	public FluidWorker(ServerWorld w, Mode mode, WorldWorkSet ws) {
		this.mode = mode;
		this.owner = ws;
		this.equalizer = mode == Mode.EQUALIZER;
		setDaemon(true);
		setName("Fluid-Worker-" + (equalizer ? "equalizer" : "normal") + "-" + ((ServerWorld) w).func_234923_W_().func_240901_a_().toString());
		start();
	}

	private final Comparator<Tuple<Long, Integer>> comp = new Comparator<Tuple<Long, Integer>>() {

		@Override
		public int compare(Tuple<Long, Integer> o1, Tuple<Long, Integer> o2) {
			return o1.getB() - o2.getB();
		}
	};

	public void onTick() {
	}

	public double getSqDistToNBP(BlockPos pos) {
		return owner.glob.getSqDistToNBP(pos);
	}

	public boolean banPos(BlockPos pos) {
		return owner.banPos(pos);
	}

	public boolean banPos(long pos) {
		return owner.banPos(pos);
	}

	public boolean isPosReady(BlockPos pos) {
		return owner.isPosReady(pos);
	}

	public boolean banPoses(Set<BlockPos> poses) {
		return owner.banPoses(poses);
	}

	public void unbanPoses(Set<BlockPos> poses) {
		owner.unbanPoses(poses);
	}

	public void addNTTask(long l, int t) {
		owner.addNTTask(l, t);
	}

	public void clearTasks(long l) {
		owner.clearTasks(l);
	}

	public void addEQTask(long l) {
		owner.addEQTask(l);
		// owner.unparkEQ();
	}

	public void addEqLock(long l) {
		owner.addEqLock(l);
	}

	public boolean isEqLocked(long l) {
		return owner.isEqLocked(l);
	}

	public synchronized void run() {
		while (cont) {
			LockSupport.park(this);
			// unparkedTime = System.currentTimeMillis();
			rv();
			// finishedTasks = 0;
			// blockUpdates = 0;
		}
	}

	private void rv() {
		ConcurrentSet<Long> taskList = equalizer ? owner.eqTasks : owner.tasks;
		// System.out.println(BlockUpdataer.getUPms());
		ArrayList<Tuple<Long, Integer>> ct = new ArrayList<>();
		for (long l : taskList) {
			int dist = (int) owner.glob.getSqDistToNBP(BlockPos.fromLong(l));
			Tuple<Long, Integer> tup = new Tuple<Long, Integer>(l, dist);
			ct.add(tup);
		}
		ct.sort(comp);

		for (Tuple<Long, Integer> tup : ct) {
			long l = tup.getA();
			taskList.remove(l);
			if (BlockUpdataer.applyTask(1.25F)) {
				BlockPos pos = BlockPos.fromLong(l);
				try {
					Runnable ffl = FFluidBasic.generate((ServerWorld) owner.world, pos, this, mode);
					ffl.run();
					ffl = null;
				} catch (Exception e) {
					PhysEX.LOGGER.warn("Exeption while running fluid task ", e);
				}
			} else {				
				addNTTask(l, 1);
			}
		}
	}

	public static enum Mode {
		DEFAULT, EQUALIZER;
	}
}