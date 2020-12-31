package net.skds.physex.mixins.other;

//import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

//import io.netty.util.internal.ConcurrentSet;
import net.minecraft.util.palette.PalettedContainer;

@Mixin(value = { PalettedContainer.class })
public class PalettedContainerMixin {

	//private ConcurrentSet<Thread> queue = new ConcurrentSet<>();

	@Final
	@Shadow
	private ReentrantLock lock = new ReentrantLock();

	@Overwrite
	public void lock() {
		lock.lock();
		/*while (this.lock.isLocked() && !this.lock.isHeldByCurrentThread()) {
			Thread ct = Thread.currentThread();
			queue.add(ct);
			LockSupport.parkNanos(100_000L);
			lock.lock();
			queue.remove(ct);
			// LockSupport.park(this);
		}
		*/
	}

	@Overwrite
	public void unlock() {
		this.lock.unlock();
		/*if (!queue.isEmpty()) {
			for (Thread t : queue) {
				LockSupport.unpark(t);
				queue.remove(t);
			}
		}
		*/
	}
}