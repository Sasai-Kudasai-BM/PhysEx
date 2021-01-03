package net.skds.physex.util.blockupdate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.blockphysics.WWS;
import net.skds.physex.fluidphysics.WorldWorkSet;

public class WWSGlobal {

	private static final Map<ServerWorld, WWSGlobal> MAP = new HashMap<>();

	final ServerWorld world;

	public final WWS blocks;
	public final WorldWorkSet fluids;
	public Set<BlockPos> PLAYERS = new HashSet<>();
	
	ConcurrentSet<Long> banPos = new ConcurrentSet<>();
	ConcurrentSet<Long> banPosOld = new ConcurrentSet<>();

	public WWSGlobal(ServerWorld w) {
		world = w;
		blocks = new WWS(w, this);
		fluids = new WorldWorkSet(w, this);
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

	public void stop() {
		blocks.stop();
		fluids.close();
	}

	public void tickIn() {
		updatePlayers();
		bpClean();
		fluids.tickIn();
		blocks.tickIn();
	}

	public void tickOut() {
		fluids.tickOut();
		blocks.tickOut();
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
	
	public double getSqDistToNBP(BlockPos pos) {
		double dist = Double.MAX_VALUE;
		for (BlockPos pos2 : PLAYERS) {
			double dx = (pos.getX() - pos2.getX());
			double dz = (pos.getZ() - pos2.getZ());

			dist = Math.min(dist, (dx * dx) + (dz * dz));
		}
		return dist;
	}

	public static WWSGlobal get(ServerWorld w) {
		return MAP.get(w);
	}
	public static WWSGlobal get(World w) {
		return MAP.get(w);
	}

	public static void loadWorld(ServerWorld w) {
		WWSGlobal wwsg = new WWSGlobal(w);
		MAP.put(w, wwsg);
	}

	public static void unloadWorld(ServerWorld w) {
		WWSGlobal wwsg = MAP.get(w);
		if (wwsg != null) {
			wwsg.stop();
			MAP.remove(w);
		}
	}

	public static void tickInG() {
		MAP.forEach((w, wwsg) -> {
			wwsg.tickIn();
		});
	}

	public static void tickOutG() {		
		MAP.forEach((w, wwsg) -> {
			wwsg.tickOut();
		});
	}
}