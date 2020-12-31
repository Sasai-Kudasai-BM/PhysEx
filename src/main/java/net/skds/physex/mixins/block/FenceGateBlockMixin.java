package net.skds.physex.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.skds.physex.registry.BlockStateProps;
import net.skds.physex.util.Interface.IBaseWL;

@Mixin(value = { FenceGateBlock.class })
public abstract class FenceGateBlockMixin extends Block implements IBaseWL, IWaterLoggable {

	public FenceGateBlockMixin(Properties properties) {
		super(properties);
	}
	
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	protected void bbb(AbstractBlock.Properties properties, CallbackInfo ci) {
		this.setDefaultState(this.getDefaultState().with(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)));
	}

	protected void addCustomState(Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.WATERLOGGED);
		builder.add(BlockStateProps.FFLUID_LEVEL);
	}	
}