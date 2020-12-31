package net.skds.physex.util.pars;

public class CustomBlockPars {

	FluidPars fluidPars = null;
	BlockPhysicsPars blockPhysicsPars = null;

	public void setFluidPars(FluidPars fluidPars) {
		this.fluidPars = fluidPars;
	}
	public void setBlockPhysicsPars(BlockPhysicsPars blockPhysicsPars) {
		this.blockPhysicsPars = blockPhysicsPars;
	}

	public FluidPars getFluidPars() {
		return fluidPars;
	}

	public BlockPhysicsPars getBlockPhysicsPars() {
		return blockPhysicsPars;
	}
}