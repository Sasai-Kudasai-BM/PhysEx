package net.skds.physex.fluidphysics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class WorldWorkSet {
	FluidWorker[] fluidWorkers;
	FluidWorker[] fluidEQWorkers;
	ArrayList<FluidWorker> Workers = new ArrayList<>();
	final ServerWorld world;
	// public final UpdateWorker upwrkr;
	ConcurrentSet<Long> eqTasks = new ConcurrentSet<>();
	ConcurrentSet<Long> tasks = new ConcurrentSet<>();
	ConcurrentSet<Long> lockedEq = new ConcurrentSet<>();
	ConcurrentSet<Long> banPos = new ConcurrentSet<>();
	ConcurrentSet<Long> banPosOld = new ConcurrentSet<>();
	ConcurrentHashMap<Integer, ConcurrentSet<Long>> nextTicksTasks = new ConcurrentHashMap<>();
	// ConcurrentSet<Long> gssTasks = new ConcurrentSet<>();
	public Set<BlockPos> PLAYERS = new HashSet<>();

	final int timeShOffset = 2;

	public WorldWorkSet(IWorld w, FluidWorker[] fws, FluidWorker[] eqfws) {
		world = (ServerWorld) w;
		// upwrkr = BlockUpdataer.getWorkerForWorld((ServerWorld) w);
		fluidWorkers = fws;
		fluidEQWorkers = eqfws;
		for (FluidWorker fw : fluidWorkers) {
			fw.addOwner(this);
			Workers.add(fw);
			fw.start();
		}
		for (FluidWorker fw : fluidEQWorkers) {
			fw.addOwner(this);
			Workers.add(fw);
			fw.start();
		}
	}

	public void clearTasks(long l) {
		eqTasks.remove(l);
		// tasks.remove(l);
	}

	public boolean isPosReady(BlockPos pos) {
		return !banPos.contains(pos.toLong());
	}

	public boolean banPos(BlockPos pos) {
		long lp = pos.toLong();
		boolean ss = banPos.add(lp);
		return ss;
	}

	public boolean banPos(long pos) {
		boolean ss = banPos.add(pos);
		return ss;
	}

	public boolean banPoses(Set<BlockPos> poses) {
		Set<Long> blocked = new HashSet<>();
		for (BlockPos pos : poses) {
			long lp = pos.toLong();
			boolean ss = banPos.add(lp);
			if (!ss) {
				banPos.removeAll(blocked);
				return false;
			}
			blocked.add(lp);
		}
		return true;
	}

	public void unbanPoses(Set<BlockPos> poses) {
		for (BlockPos pos : poses) {
			long l = pos.toLong();
			banPos.remove(l);
			banPosOld.remove(l);
		}
	}

	public void unbanPosesL(Set<Long> poses) {
		for (long pos : poses) {
			banPos.remove(pos);
			banPosOld.remove(pos);
		}
	}

	public void addEqLock(long l) {
		lockedEq.add(l);
	}

	public boolean isEqLocked(long l) {
		return lockedEq.contains(l);
	}

	public void tickIn() {
		unparkDefault();
		unparkEQ();
		bpClean();
		updatePlayers();

		for (FluidWorker fw : fluidWorkers) {
			fw.onTick();
		}
		nextTicksTasks.forEach((tick, tasksList) -> {
			if (tick <= 0) {
				lockedEq.removeAll(tasksList);
				tasks.addAll(tasksList);
			} else {
				nextTicksTasks.put(tick - 1, tasksList);
			}
			nextTicksTasks.remove(tick);
		});

	}

	public void tickOut() {
		unparkDefault();
		unparkEQ();
	}

	public double getSqDistToNBP(BlockPos pos) {
		double dist = Double.MAX_VALUE;
		for (BlockPos pos2 : PLAYERS) {
			double dx = (pos.getX() - pos2.getX());
			double dz = (pos.getZ() - pos2.getZ());

			dist = Math.min(dist, (dx * dx) + (dz * dz));
		}
		return dist;
	}

	void bpClean() {
		banPos.forEach(l -> {
			if (banPosOld.contains(l)) {
				banPos.remove(l);
				// System.out.println(BlockPos.fromLong(l) + " Pizdos");
			}
		});

		banPosOld.clear();
		banPosOld.addAll(banPos);
	}

	public void close() {
		for (FluidWorker fw : Workers) {
			fw.cont = false;
			LockSupport.unpark(fw);
		}
		tasks.forEach(lpos -> {
			BlockPos pos = BlockPos.fromLong(lpos);
			world.getPendingFluidTicks().scheduleTick(pos, world.getFluidState(pos).getFluid(), timeShOffset);
		});

		eqTasks.forEach(lpos -> {
			BlockPos pos = BlockPos.fromLong(lpos);
			world.getPendingFluidTicks().scheduleTick(pos, world.getFluidState(pos).getFluid(), timeShOffset);
		});

		nextTicksTasks.forEach((t, set) -> {
			set.forEach(lpos -> {
				BlockPos pos = BlockPos.fromLong(lpos);
				world.getPendingFluidTicks().scheduleTick(pos, world.getFluidState(pos).getFluid(), timeShOffset + t);
			});
		});
	}

	public void addTask(World w, Fluid f, BlockPos p) {

		long l = p.toLong();
		/*
		 * final int d = 64; final int d2 = d * d;
		 * 
		 * if (BlockUpdataer.getAvgTime() > 8) { int dst = (int) getSqDistToNBP(p); if
		 * (dst > d2) { w.getPendingFluidTicks().scheduleTick(p, f,
		 * FFluidStatic.getTickRate((FlowingFluid) f, w) * 4 + 1); } else if (dst >
		 * 4*d2) { w.getPendingFluidTicks().scheduleTick(p, f,
		 * FFluidStatic.getTickRate((FlowingFluid) f, w) * 8 + 1); } else {
		 * ++tasksCount; ++tasksCountPerTick; tasks.add(l); } //
		 */
		// } else {
		tasks.add(l);
		// }
	}

	public void addEQTask(long l) {
		eqTasks.add(l);
	}

	public void addNTTask(long l, int tick) {
		nextTicksTasks.putIfAbsent(tick, new ConcurrentSet<Long>());
		ConcurrentSet<Long> set = nextTicksTasks.get(tick);
		if (set != null) {
			set.add(l);
		}
	}

	public boolean hasTask() {
		return !tasks.isEmpty();
	}

	public void unparkDefault() {
		for (FluidWorker fw : fluidWorkers) {
			LockSupport.unpark(fw);
		}
	}

	public void unparkEQ() {
		for (FluidWorker fw : fluidEQWorkers) {
			LockSupport.unpark(fw);
		}
	}

	private void updatePlayers() {
		List<? extends PlayerEntity> players = world.getPlayers();
		Set<BlockPos> np = new HashSet<>();
		for (PlayerEntity p : players) {
			BlockPos pos = p.func_233580_cy_();
			np.add(pos);
		}
		PLAYERS = np;
	}
}