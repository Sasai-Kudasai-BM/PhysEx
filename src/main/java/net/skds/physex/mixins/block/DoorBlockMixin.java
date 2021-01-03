package net.skds.physex.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.skds.physex.registry.BlockStateProps;
import net.skds.physex.util.Interface.IBaseWL;

@Mixin(value = { DoorBlock.class })
public abstract class DoorBlockMixin extends Block implements IBaseWL, IWaterLoggable {

	public DoorBlockMixin(Properties properties) {
		super(properties);
	}
	
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	protected void bbb(AbstractBlock.Properties properties, CallbackInfo ci) {
		this.setDefaultState(this.getDefaultState().with(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)));
	}

	protected void addCustomState(Builder<Block, BlockState> builder) {

		try {
			builder.add(BlockStateProperties.WATERLOGGED);			
		} catch (Exception e) {
		}
		builder.add(BlockStateProps.FFLUID_LEVEL);
	}
}