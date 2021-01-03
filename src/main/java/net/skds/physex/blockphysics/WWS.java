package net.skds.physex.blockphysics;

import java.util.concurrent.locks.LockSupport;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.util.blockupdate.WWSGlobal;

public class WWS {

	public final WWSGlobal glob;

	private final ServerWorld world;
	private final BFWorker worker;	
	private ConcurrentSet<BFTask> tasks = new ConcurrentSet<>();


	public WWS(ServerWorld w, WWSGlobal owner) {
		this.world = w;
		this.glob = owner;
		this.worker = new BFWorker(this);
	}

	public void addTask(BlockPos pos, BFTask.Type type) {
		BFTask task = new BFTask(type, pos);
		tasks.add(task);
	}

	public ServerWorld getWorld() {
		return world;
	}

	public ConcurrentSet<BFTask> getTaskList() {
		return tasks;
	}

	public void stop() {
		worker.stp();
		LockSupport.unpark(worker);
	}

	public void unpark() {
		LockSupport.unpark(worker);
	}

	public void tickIn() {
		unpark();

	}

	public void tickOut() {
		unpark();		
	}
}