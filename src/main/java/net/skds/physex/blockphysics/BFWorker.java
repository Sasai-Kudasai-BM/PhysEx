package net.skds.physex.blockphysics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.locks.LockSupport;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.util.Tuple;
import net.skds.physex.util.blockupdate.BlockUpdataer;

public class BFWorker extends Thread {

	private final WWS owner;
	private boolean work = true;

	private final Comparator<Tuple<BFTask, Integer>> comp = new Comparator<Tuple<BFTask, Integer>>() {

		@Override
		public int compare(Tuple<BFTask, Integer> o1, Tuple<BFTask, Integer> o2) {
			return o1.getB() - o2.getB();
		}
	};

	public BFWorker(WWS owner) {
		this.owner = owner;
		setDaemon(true);
		setName("BF-Worker-" + this.owner.getWorld().func_234923_W_().func_240901_a_().toString());
		start();
	}

	public void stp() {
		work = false;
	}

	@Override
	public synchronized void run() {
		while (work) {
			LockSupport.park();
			rv();
		}
	}

	private void rv() {
		ConcurrentSet<BFTask> taskList = owner.getTaskList();

		ArrayList<Tuple<BFTask, Integer>> ct = new ArrayList<>();
		for (BFTask t : taskList) {
			int dist = (int) owner.glob.getSqDistToNBP(t.getPos());
			Tuple<BFTask, Integer> tup = new Tuple<BFTask, Integer>(t, dist);
			ct.add(tup);
		}
		ct.sort(comp);
		//if (ct.size() > 0) {

			//System.out.println(ct.size());
		//}

		for (Tuple<BFTask, Integer> tup : ct) {
			final BFTask task = tup.getA();
			taskList.remove(tup.getA());
			if (BlockUpdataer.applyTask(0.5F)) {
				Runnable ex = new BFExecutor(owner.getWorld(), task.getPos(), this);
				ex.run();
				ex = null;

			} else {
			}
		}

	}
}