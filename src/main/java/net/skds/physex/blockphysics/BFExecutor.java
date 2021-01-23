package net.skds.physex.blockphysics;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.blockphysics.BFTask.Type;
import net.skds.physex.entity.AdvancedFallingBlockEntity;
import net.skds.physex.fluidphysics.FFluidStatic;
import net.skds.physex.util.Interface.IBlockExtended;
import net.skds.physex.util.blockupdate.BasicExecutor;
import net.skds.physex.util.pars.BlockPhysicsPars;

public class BFExecutor extends BasicExecutor {

	// private final Long2ObjectOpenHashMap<BFSnapshot> snaps = new
	// Long2ObjectOpenHashMap<>();

	private final float G = (float) 9.81E-6;

	private final BFWorker worker;
	private final BlockState state;
	private final BlockPos pos;
	private final Block block;
	private final BFTask.Type type;
	private final BlockPhysicsPars param;

	public BFExecutor(ServerWorld w, BlockPos pos, BFWorker worker, BFTask.Type type) {
		super(w);
		this.worker = worker;
		this.pos = pos;
		this.state = getBlockState(pos);
		this.block = this.state.getBlock();
		this.param = getParam(block);
		this.type = type;
		// System.out.println(pos);
	}

	@Override
	public void run() {
		runS();
	}

	public boolean runS() {
		if (isAir(state) || !param.falling || block == Blocks.BEDROCK || (block instanceof FlowingFluidBlock)) {
			return false;
		}
		boolean b = tryFall();
		return b;
	}

	private boolean tryFall() {
		BlockPos posd = pos.down();
		BlockState stated = getBlockState(posd);

		if (type == Type.DOWNRAY) {
			// System.out.println("x");
			if (!checkDownray(pos, state, param)) {
				// fall();
				return true;
			}
		} else {
			if (canFall(stated)) {

				if (!checkSnap(pos, state, param)) {
					fall();
					return true;
				}
			} else {
				addDownrayTask(posd);
			}
		}
		return false;
	}

	private void fall(BlockPos pss) {
		setState(pss.down(), Blocks.AIR.getDefaultState(), true);
		BlockState fallstate = setState(pss, Blocks.AIR.getDefaultState(), false);
		AdvancedFallingBlockEntity entity = new AdvancedFallingBlockEntity(w, pss.getX() + 0.5, pss.getY(),
				pss.getZ() + 0.5, fallstate);

		if (fallstate != null && fallstate.hasTileEntity()) {
			TileEntity te = getTileEntity(pss);
			if (te != null) {
				entity.tileEntityData = te.serializeNBT();
				te.remove();
			} else {
				System.out.println("no entity");
			}
		}

		BlockPhysicsPars fallParam = getParam(fallstate);

		entity.fallTime = 1;
		entity.bounce = fallParam.bounce;
		addEntity(entity);
	}

	private void fall() {
		fall(pos);
	}

	private boolean checkSnap(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		boolean usp = false;
		if (type != Type.DOWNRAY) {
			BlockPos posu = pos.up();
			BlockState stateu = getBlockState(posu);
			usp = canSupport(state0, stateu, pos0, posu, param0, getParam(stateu), param0.mass * G, param0.arc > 0);
		}
		if (param0.hanging && usp && checkUpray(pos0, state0, param0)) {
			return true;
		}

		boolean bl = false;
		if (param0.celling) {
			bl = bl || checkCelling(pos0, state0, param0);
		}

		bl = bl || checkPane(pos0, state0, param0, usp);

		return bl;
	}

	private boolean checkUpray(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		return checkRay(pos0, state0, param0, true);
	}

	private boolean checkDownray(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		return checkRay(pos0, state0, param0, false);
	}

	private boolean checkRay(BlockPos pos0, BlockState state0, BlockPhysicsPars param0, boolean up) {
		BlockPos pos2 = pos0;
		BlockState state2 = state0;
		BlockState state1;
		BlockPos pos1;
		BlockPhysicsPars par1;
		BlockPhysicsPars par2 = param0;
		float force = param0.mass * G;
		boolean sucess = false;
		boolean ds = false;
		while (pos2.getY() < 255 && pos2.getY() > 0 && !sucess) {
			par1 = par2;
			pos1 = pos2;
			state1 = state2;
			pos2 = up ? pos2.up() : pos2.down();
			state2 = getBlockState(pos2);
			par2 = getParam(state2);
			force += (par2.mass * G);
			// System.out.println(state2 + " " + par2 + pos2);

			if ((!par2.fragile && !par2.falling) || (!par1.fragile && !par1.falling)) {
				return true;
			}

			ds = canSupport(state1, state2, pos1, pos2, par1, par2, force, false);

			if (!ds && !checkPane(pos1, state1, par1, true)) {
				if (!up) {
					fall(pos1);
					// System.out.println(state1 + " " + par1 + pos1);
					// System.out.println(getParam(Blocks.BEDROCK));
				}
				return false;
			} else if (!ds) {
				return true;
			}
		}
		return sucess;
	}

	private boolean checkCelling(BlockPos pos1, BlockState state1, BlockPhysicsPars par1) {

		BlockPos pos2 = pos1.offset(Direction.EAST);
		BlockState state2 = getBlockState(pos2);
		BlockPhysicsPars par2 = getParam(state2);
		if (canSupport(state1, state2, pos1, pos2, par1, par2, par1.mass * G, true)) {
			BlockPos pos3 = pos1.offset(Direction.WEST);
			BlockState state3 = getBlockState(pos3);
			BlockPhysicsPars par3 = getParam(state3);
			if (canSupport(state1, state3, pos1, pos3, par1, par3, par1.mass * G, true)) {
				return true;
			}
		}
		pos2 = pos1.offset(Direction.NORTH);
		state2 = getBlockState(pos2);
		par2 = getParam(state2);
		if (canSupport(state1, state2, pos1, pos2, par1, par2, par1.mass * G, true)) {
			BlockPos pos3 = pos1.offset(Direction.SOUTH);
			BlockState state3 = getBlockState(pos3);
			BlockPhysicsPars par3 = getParam(state3);
			if (canSupport(state1, state3, pos1, pos3, par1, par3, par1.mass * G, true)) {
				return true;
			}
		}

		return false;
	}

	private boolean checkPane(BlockPos pos, BlockState state, BlockPhysicsPars param, boolean usp) {

		Set<BlockPos> posset = new HashSet<>();

		int dist = Math.max(param.linear, param.radial);
		if (usp) {
			dist = Math.max(param.arc, dist);
		}
		if (dist < 1) {
			return false;
		}
		for (Direction dir : FFluidStatic.getRandomizedDirections(w.rand, false)) {
			int i = 0;
			float force = param.mass * G;
			BlockPos pos1 = pos;
			BlockState state1 = state;
			BlockPhysicsPars par1 = param;
			posset.add(pos);
			while (i < dist) {
				++i;

				BlockPos pos2 = pos.offset(dir, i);
				BlockState state2 = getBlockState(pos2);
				BlockPhysicsPars par2 = getParam(state2);
				posset.add(pos2);

				if (canSupport(state1, state2, pos1, pos2, par1, par2, force)) {
					if (supportDown(pos2, state2, par2, force)) {
						return true;
					} else {
						int remainDist = param.radial - i;
						if (remainDist > 0) {
							if (radialize(pos2, state2, par2, dir.rotateY(), force, posset, remainDist)
									|| radialize(pos2, state2, par2, dir.rotateYCCW(), force, posset, remainDist)) {
								return true;
							}

						}
					}
				} else {
					break;
				}
				force += (par2.mass * G);
				pos1 = pos2;
				state1 = state2;
				par1 = par2;
			}
		}

		return false;
	}

	private boolean radialize(BlockPos pos0, BlockState state0, BlockPhysicsPars par0, Direction dir0, float force0,
			Set<BlockPos> set, int dist0) {

		Set<Quad> ths = new HashSet<>();
		Set<Quad> next = new HashSet<>();

		BlockPos pos1 = pos0.offset(dir0);
		if (set.contains(pos1)) {
			return false;
		}
		next.add(new Quad(pos0, state0, par0, dist0));

		while (!next.isEmpty()) {
			ths = next;
			next = new HashSet<>();
			for (Quad q : ths) {
				set.add(q.pos);
				for (Direction dir : FFluidStatic.getRandomizedDirections(w.rand, false)) {
					if (supportDown(q.pos, q.state, q.par, force0 + (q.par.mass * G))) {
						return true;
					}
					if (q.dist < 1) {
						break;
					}
					BlockPos pos2 = q.pos.offset(dir);
					if (set.contains(pos2) || onLine(pos2)) {
						continue;
					}
					// debug(q.pos);
					BlockState state2 = getBlockState(pos2);
					BlockPhysicsPars par2 = getParam(state2);
					if (canSupport(q.state, state2, q.pos, pos2, q.par, par2, force0 + (q.par.mass * G))) {
						next.add(new Quad(pos2, state2, par2, q.dist - 1));
					}
				}
			}
		}

		return false;
	}

	private boolean onLine(BlockPos pos2) {
		return pos.getX() == pos2.getX() || pos.getZ() == pos2.getZ();
	}

	private boolean supportDown(BlockPos pos0, BlockState state0, BlockPhysicsPars par0, float force) {
		BlockPos posd = pos0.down();
		BlockState stated = getBlockState(posd);
		BlockPhysicsPars pard = getParam(stated);
		// float force2 = force + (pard.mass * G);
		if (canSupport(state0, stated, pos0, posd, par0, pard, force)) {
			addDownrayTask(posd);
			return true;
		}
		// debug(pos0);
		// System.out.println(state0 + " " + stated);
		return false;
	}

	private boolean canFall(BlockState stated) {
		float force = param.mass * G;
		boolean support = canSupport(state, stated, pos, pos.down(), param, getParam(stated), force);

		return !support;
	}

	private boolean canSupport(BlockState state1, BlockState state2, BlockPos pos1, BlockPos pos2,
			BlockPhysicsPars par1, BlockPhysicsPars par2, float force, boolean... flags) {

		if (isAir(state2) || par2.fragile || state2.getBlock() instanceof FlowingFluidBlock) {
			return false;
		}
		Direction dir = dirFromVec(pos1, pos2);
		boolean arc = false;
		if (flags.length > 0) {
			arc = flags[0];
		}

		// Direction dir = dirFromVec(pos1, pos2);
		VoxelShape voxelShape2 = state2.getCollisionShape(w, pos2);
		// VoxelShape voxelShape1 = state1.getCollisionShape(w, pos1);
		if (voxelShape2.isEmpty()) {
			return false;
		}
		// return VoxelShapes.doAdjacentCubeSidesFillSquare(voxelShape1, voxelShape2,
		// dir);

		if (par2.strength < force/* || par1.strength < force */) {
			return false;
		}

		if (!par2.falling && !par2.fragile) {
			return true;
		}

		boolean empty = par1.attachIgnore.isEmpty();
		boolean empty2 = par2.attachIgnore.isEmpty();

		if (dir != Direction.DOWN && !arc && !par1.selfList.contains(state2.getBlock())) {
			if (par1.attach) {
				if (!empty && par1.attachIgnore.contains(state2.getBlock())) {
					return false;
				} else {
					// return true;
				}
			} else {
				if (!empty && par1.attachIgnore.contains(state2.getBlock())) {
					// return true;
				} else {
					return false;
				}
			}
			if (par2.attach) {
				if (!empty2 && par2.attachIgnore.contains(state1.getBlock())) {
					return false;
				} else {
					// return true;
				}
			} else {
				if (!empty2 && par2.attachIgnore.contains(state1.getBlock())) {
					// return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}

	private BlockPhysicsPars getParam(Block b) {
		BlockPhysicsPars par = ((IBlockExtended) b).getCustomBlockPars().getBlockPhysicsPars();
		if (par == null) {
			boolean empty = b.getDefaultState().getCollisionShape(w, pos).isEmpty();
			@SuppressWarnings("deprecation")
			float res = b.getExplosionResistance();
			par = new BlockPhysicsPars(b, empty, res);
		}
		return par;
	}

	private BlockPhysicsPars getParam(BlockState s) {
		return getParam(s.getBlock());
	}

	private void addDownrayTask(BlockPos p) {
		BFTask newTask = new BFTask(Type.DOWNRAY, p);
		worker.owner.addTask(newTask);
	}

	private static class Quad {
		public final BlockPos pos;
		public final BlockState state;
		public final BlockPhysicsPars par;
		public final int dist;

		public Quad(BlockPos pos, BlockState state, BlockPhysicsPars par, int dist) {
			this.pos = pos;
			this.state = state;
			this.par = par;
			this.dist = dist;
		}
	}
}