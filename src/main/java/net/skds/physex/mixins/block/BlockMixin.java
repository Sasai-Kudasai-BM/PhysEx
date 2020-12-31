package net.skds.physex.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateContainer.Builder;
import net.skds.physex.util.pars.CustomBlockPars;
import net.skds.physex.util.Interface.IBlockExtended;

@Mixin(value = Block.class )
public class BlockMixin implements IBlockExtended {

	private CustomBlockPars customBlockPars = new CustomBlockPars();

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;fillStateContainer(Lnet/minecraft/state/StateContainer$Builder;)V"))
	protected void aaa(Block b, StateContainer.Builder<Block, BlockState> builder) {
		fillStateContainer(builder);
		addCustomState(builder);
	}

	protected void addCustomState(Builder<Block, BlockState> builder) {
	}	

	@Shadow
	private void fillStateContainer(Builder<Block, BlockState> builder) {
	}

	@Override
	public CustomBlockPars getCustomBlockPars() {
		return customBlockPars;
	}

	@Override
	public void setCustomBlockPars(CustomBlockPars pars) {
		customBlockPars = pars;
	}
}