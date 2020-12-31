package net.skds.physex.util.pars;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.physex.util.Interface.IBlockExtended;

public class ParsApplier {

	public static void applyFluidPars(ParsGroup<FluidPars> FG) {
		for (Block b : FG.blocks) {
			CustomBlockPars pars = ((IBlockExtended) b).getCustomBlockPars();
			pars.setFluidPars(FG.param);
		}
	}

	public static void applyBlockPhysicsPars(ParsGroup<BlockPhysicsPars> BG) {
		for (Block b : BG.blocks) {
			CustomBlockPars pars = ((IBlockExtended) b).getCustomBlockPars();
			pars.setBlockPhysicsPars(BG.param);
		}
	}

	public static void refresh() {
		
		JsonConfigReader reader = new JsonConfigReader();
		reader.run();

		ForgeRegistries.BLOCKS.getValues().forEach(block -> {
			((IBlockExtended) block).setCustomBlockPars(new CustomBlockPars());
		});

		reader.FP.forEach((name, pars) -> {
			applyFluidPars(pars);
		});
		reader.BFP.forEach((name, pars) -> {
			applyBlockPhysicsPars(pars);
		});
	}

	public static class ParsGroup<A> {
		public final Set<Block> blocks;
		public final A param;

		ParsGroup(A p, Set<Block> blockList) {
			param = p;
			blocks = blockList;
		}
	}
}