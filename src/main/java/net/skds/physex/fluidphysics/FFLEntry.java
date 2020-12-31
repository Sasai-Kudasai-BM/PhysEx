package net.skds.physex.fluidphysics;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;

public class FFLEntry {
	private int localPos;
	private Fluid fluid;
	private int level;

	private static final String fluidKey = "Fluid";
	private static final String levelKey = "Level";
	private static final String locPosKey = "LocalPos";

	public FFLEntry(CompoundNBT nbt) {
		this.fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(nbt.getString(fluidKey)));
		this.level = nbt.getInt(levelKey);
		this.localPos = nbt.getInt(locPosKey);
	}

	public FFLEntry(FluidState fs, BlockPos pos) {

		this.fluid = fs.getFluid();
		this.level = fs.getLevel();
		this.localPos = keyFromCord(pos);
	}

	public FFLEntry(Fluid f, int lvl, BlockPos pos) {

		this.fluid = f;
		this.level = lvl;
		this.localPos = keyFromCord(pos);
	}

	public static int keyFromCord(BlockPos pos) {
		int lx = pos.getX() & 15;
		int ly = pos.getY() & 255;
		int lz = pos.getZ() & 15;
		
		return (ly << 8 | lx << 4 | lz);
	}

	public CompoundNBT serialize() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString(fluidKey, fluid.getRegistryName().toString());
		nbt.putInt(levelKey, level);
		nbt.putInt(locPosKey, localPos);
		return nbt;
	}

	public Fluid getFluid() {
		return fluid;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setFluid(Fluid fluid) {
		this.fluid = fluid;
	}

	public int getIntPos() {
		return localPos;
	}

	public BlockPos getLocalPos() {
		return new BlockPos((localPos >> 4) & 15, localPos >> 8, localPos & 15);
	}

	public String toString() {
		return serialize().getString();
	}
}