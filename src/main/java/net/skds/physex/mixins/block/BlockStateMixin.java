package net.skds.physex.mixins.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.fluidphysics.FFluidStatic;
import net.skds.physex.registry.BlockStateProps;
import net.skds.physex.util.Interface.IBaseWL;

@Mixin(value = { BlockState.class })
public abstract class BlockStateMixin extends AbstractBlockState {

	protected BlockStateMixin(Block p_i231870_1_, ImmutableMap<Property<?>, Comparable<?>> p_i231870_2_,
			MapCodec<BlockState> p_i231870_3_) {
		super(p_i231870_1_, p_i231870_2_, p_i231870_3_);
	}

	public FluidState getFluidState() {
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
				return fs;
			}
		}
		return super.getFluidState();
	}
	
	public void neighborChanged(World worldIn, BlockPos posIn, Block blockIn, BlockPos fromPosIn, boolean isMoving) {
		//super.neighborChanged(worldIn, posIn, blockIn, fromPosIn, isMoving);
		if (((BlockState) (Object) this).getBlock()  instanceof IBaseWL && PhysEXConfig.COMMON.finiteFluids.get()) {
			BlockState s = (BlockState) (Object) this;
			fixFFLNoWL((World) worldIn, s, posIn);
			if (s.get(BlockStateProperties.WATERLOGGED))
				worldIn.getPendingFluidTicks().scheduleTick(posIn, s.getFluidState().getFluid(), FFluidStatic.getTickRate((FlowingFluid) s.getFluidState().getFluid(), worldIn));
		}
		super.neighborChanged(worldIn, posIn, blockIn, fromPosIn, isMoving);
	}	

	public BlockState updatePostPlacement(Direction face, BlockState queried, IWorld worldIn, BlockPos currentPos, BlockPos offsetPos) {
		if (((BlockState) (Object) this).getBlock() instanceof IBaseWL && PhysEXConfig.COMMON.finiteFluids.get()) {
			BlockState s = (BlockState) (Object) this;
			fixFFLNoWL(worldIn, s, currentPos);
			if (s.get(BlockStateProperties.WATERLOGGED))
				worldIn.getPendingFluidTicks().scheduleTick(currentPos, s.getFluidState().getFluid(), FFluidStatic.getTickRate((FlowingFluid) s.getFluidState().getFluid(), worldIn));
		}		
		return super.updatePostPlacement(face, queried, worldIn, currentPos, offsetPos);
	}

	private void fixFFLNoWL(IWorld w, BlockState s, BlockPos p) {
		if (!s.get(BlockStateProperties.WATERLOGGED) && s.get(BlockStateProps.FFLUID_LEVEL) > 0) {
			w.setBlockState(p, s.with(BlockStateProps.FFLUID_LEVEL, 0), 3);
		}
	}
}