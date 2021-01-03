package net.skds.physex.fluidphysics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.fluidphysics.FluidWorker.Mode;
import net.skds.physex.util.blockupdate.WWSGlobal;

public class WorldWorkSet {
	public final WWSGlobal glob;
	final FluidWorker fluidWorker, fluidEQWorker;
	final ServerWorld world;
	// public final UpdateWorker upwrkr;
	ConcurrentSet<Long> eqTasks = new ConcurrentSet<>();
	ConcurrentSet<Long> tasks = new ConcurrentSet<>();
	ConcurrentSet<Long> lockedEq = new ConcurrentSet<>();
	ConcurrentHashMap<Integer, ConcurrentSet<Long>> nextTicksTasks = new ConcurrentHashMap<>();
	// ConcurrentSet<Long> gssTasks = new ConcurrentSet<>();

	final int timeShOffset = 2;

	public WorldWorkSet(ServerWorld w, WWSGlobal owner) {
		world = (ServerWorld) w;
		glob = owner;
		// upwrkr = BlockUpdataer.getWorkerForWorld((ServerWorld) w);
		fluidWorker = new FluidWorker(world, Mode.DEFAULT, this);
		fluidEQWorker = new FluidWorker(world, Mode.EQUALIZER, this);
	}

	public void clearTasks(long l) {
		eqTasks.remove(l);
		// tasks.remove(l);
	}

	public boolean isPosReady(BlockPos pos) {
		return glob.isPosReady(pos);
	}

	public boolean banPos(BlockPos pos) {
		return glob.banPos(pos);
	}

	public boolean banPos(long pos) {
		return glob.banPos(pos);
	}

	public boolean banPoses(Set<BlockPos> poses) {
		return glob.banPoses(poses);
	}

	public void unbanPoses(Set<BlockPos> poses) {
		glob.unbanPoses(poses);
	}

	public void unbanPosesL(Set<Long> poses) {
		glob.unbanPosesL(poses);
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

		fluidWorker.onTick();
		fluidEQWorker.onTick();
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

	public void close() {
		fluidWorker.cont = false;
		fluidEQWorker.cont = false;
		LockSupport.unpark(fluidWorker);
		LockSupport.unpark(fluidEQWorker);

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
		LockSupport.unpark(fluidWorker);
	}

	public void unparkEQ() {
		LockSupport.unpark(fluidEQWorker);
	}
}