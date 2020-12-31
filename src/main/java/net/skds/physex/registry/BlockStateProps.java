package net.skds.physex.registry;

import net.minecraft.state.IntegerProperty;
import net.skds.physex.PhysEXConfig;

public class BlockStateProps {

	public static final IntegerProperty FFLUID_LEVEL = IntegerProperty.create("ffluid_level", 0, PhysEXConfig.MAX_FLUID_LEVEL);
	
}