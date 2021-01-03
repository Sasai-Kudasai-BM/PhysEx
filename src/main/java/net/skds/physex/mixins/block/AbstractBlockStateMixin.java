package net.skds.physex.mixins.block;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.blockphysics.BFManager;
import net.skds.physex.fluidphysics.FFluidStatic;
import net.skds.physex.registry.BlockStateProps;
import net.skds.physex.util.Interface.IBaseWL;

@Mixin(value = { AbstractBlockState.class })
public abstract class AbstractBlockStateMixin {

	
	@Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
	public void getFluidStateM(CallbackInfoReturnable<FluidState> ci) {
		if (PhysEXConfig.COMMON.finiteFluids.get()) {
			BlockState bs = (BlockState) (Object) this;
			if (bs.getBlock() instanceof IBaseWL) {
				int level = bs.get(BlockStateProps.FFLUID_LEVEL);
				FluidState fs;
				if (bs.get(BlockStateProperties.WATERLOGGED)) {
					level = (level == 0) ? PhysEXConfig.MAX_FLUID_LEVEL : level;
					if (level >= PhysEXConfig.MAX_FLUID_LEVEL) {
						fs = ((FlowingFluid) Fluids.WATER).getStillFluidState(false);
					} else if (level <= 0) {
						fs = Fluids.EMPTY.getDefaultState();
					} else {
						fs = ((FlowingFluid) Fluids.WATER).getFlowingFluidState(level, false);
					}
				} else {
					fs = Fluids.EMPTY.getDefaultState();
				}
				ci.setReturnValue(fs);
			}
		}
	}

	
	@Inject(method = "onBlockAdded", at = @At(value = "HEAD"), cancellable = false)
	public void onBlockAdded(World worldIn, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
		BlockState st = (BlockState) (Object) this;			
		if (st.getMaterial() != Material.AIR && st.getFluidState().isEmpty() && PhysEXConfig.COMMON.blockphysics.get() && !worldIn.isRemote && !isMoving) {
			BFManager.addUpdateTask((ServerWorld) worldIn, pos, (BlockState) (Object) this);
		}
	}

	
	@Inject(method = "ticksRandomly", at = @At(value = "HEAD"), cancellable = true)
	public void ticksRandomlyM(CallbackInfoReturnable<Boolean> ci) {
	}

	@Inject(method = "randomTick", at = @At(value = "HEAD"), cancellable = false)
	public void randomTick(ServerWorld w, BlockPos pos, Random randomIn, CallbackInfo ci) {
		BlockState st = (BlockState) (Object) this;			
		if (st.getMaterial() != Material.AIR && st.getFluidState().isEmpty() && PhysEXConfig.COMMON.blockphysics.get()) {
			BFManager.addRandomTask(w, pos, (BlockState) (Object) this);
		}
	}

	@Inject(method = "tick", at = @At(value = "HEAD"), cancellable = false)
	public void tickM(ServerWorld w, BlockPos pos, Random randomIn, CallbackInfo ci) {
		BlockState st = (BlockState) (Object) this;			
		if (st.getMaterial() != Material.AIR && st.getFluidState().isEmpty() && PhysEXConfig.COMMON.blockphysics.get()) {
			BFManager.addUpdateTask(w, pos, (BlockState) (Object) this);
		}
	}
	
	@Inject(method = "neighborChanged", at = @At(value = "HEAD"), cancellable = false)
	public void neighborChangedM(World worldIn, BlockPos posIn, Block blockIn, BlockPos fromPosIn, boolean isMoving, CallbackInfo ci) {
		//super.neighborChanged(worldIn, posIn, blockIn, fromPosIn, isMoving);
		if (((BlockState) (Object) this).getBlock()  instanceof IBaseWL && PhysEXConfig.COMMON.finiteFluids.get()) {
			BlockState s = (BlockState) (Object) this;
			fixFFLNoWL((World) worldIn, s, posIn);
			if (s.get(BlockStateProperties.WATERLOGGED))
				worldIn.getPendingFluidTicks().scheduleTick(posIn, s.getFluidState().getFluid(), FFluidStatic.getTickRate((FlowingFluid) s.getFluidState().getFluid(), worldIn));
		}
		BlockState st = (BlockState) (Object) this;
		if (st.getMaterial() != Material.AIR && st.getFluidState().isEmpty() && PhysEXConfig.COMMON.blockphysics.get() && !worldIn.isRemote && worldIn.getFluidState(fromPosIn).isEmpty()) {
			BFManager.addNeighborTask((ServerWorld) worldIn, posIn, (BlockState) (Object) this);
		}
	}
	
	@Inject(method = "updatePostPlacement", at = @At(value = "HEAD"), cancellable = false)
	public void updatePostPlacementM(Direction face, BlockState queried, IWorld worldIn, BlockPos currentPos, BlockPos offsetPos, CallbackInfoReturnable<BlockState> ci) {
		if (((BlockState) (Object) this).getBlock() instanceof IBaseWL && PhysEXConfig.COMMON.finiteFluids.get()) {
			BlockState s = (BlockState) (Object) this;
			fixFFLNoWL(worldIn, s, currentPos);
			if (s.get(BlockStateProperties.WATERLOGGED))
				worldIn.getPendingFluidTicks().scheduleTick(currentPos, s.getFluidState().getFluid(), FFluidStatic.getTickRate((FlowingFluid) s.getFluidState().getFluid(), worldIn));
		}
		BlockState st = (BlockState) (Object) this;
		if (st.getMaterial() != Material.AIR && st.getFluidState().isEmpty() && PhysEXConfig.COMMON.blockphysics.get() && worldIn instanceof ServerWorld) {
			BFManager.addUpdateTask((ServerWorld) worldIn, currentPos, (BlockState) (Object) this);
		}
	}

	private void fixFFLNoWL(IWorld w, BlockState s, BlockPos p) {
		if (!s.get(BlockStateProperties.WATERLOGGED) && s.get(BlockStateProps.FFLUID_LEVEL) > 0) {
			w.setBlockState(p, s.with(BlockStateProps.FFLUID_LEVEL, 0), 3);
		}
	}
}