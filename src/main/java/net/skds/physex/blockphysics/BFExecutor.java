package net.skds.physex.blockphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.entity.AdvancedFallingBlockEntity;
import net.skds.physex.util.Interface.IBlockExtended;
import net.skds.physex.util.blockupdate.BasicExecutor;
import net.skds.physex.util.pars.BlockPhysicsPars;

public class BFExecutor extends BasicExecutor {

	protected final BFWorker worker;
	protected final BlockState state;
	protected final BlockPos pos;
	protected final long longpos;
	protected final Block block;
	protected final BlockPhysicsPars param;

	protected BFExecutor(ServerWorld w, BlockPos pos, BFWorker worker) {
		super(w);
		this.worker = worker;
		this.pos = pos;
		this.longpos = this.pos.toLong();
		this.state = getBlockState(pos);
		this.block = this.state.getBlock();
		this.param = ((IBlockExtended) block).getCustomBlockPars().getBlockPhysicsPars();
		//System.out.println(pos);
	}

	@Override
	public void run() {		
		Material material = state.getMaterial();
		Block block = state.getBlock();
		if (material == Material.AIR || material.isLiquid() || block == Blocks.BEDROCK || !state.getFluidState().isEmpty()) {
			return;
		}
		tryFall();
	}

	private void tryFall() {		
		BlockPos posd = pos.down();
		BlockState stated = getBlockState(posd);

		if (canMove(stated)) {
			fall();
		}
	}

	private void fall() {
		BlockState fallstate = setState(pos, Blocks.AIR.getDefaultState());
		AdvancedFallingBlockEntity entity = new AdvancedFallingBlockEntity(w, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, fallstate);
		///FallingBlockEntity entity = new FallingBlockEntity(w, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, fallstate);
		entity.fallTime = 1;
		
		addEntity(entity);
	}

	protected boolean canMove(BlockState state1) {		
		Material material = state1.getMaterial();
		//return material == Material.AIR || state.func_235714_a_(BlockTags.field_232872_am_) || material.isLiquid() || material.isReplaceable();
		return material == Material.AIR || material.isLiquid();
	}
	
}