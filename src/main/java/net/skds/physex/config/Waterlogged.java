package net.skds.physex.config;

import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec;
import net.skds.physex.PhysEX;

public class Waterlogged {

    public final ForgeConfigSpec.BooleanValue waterloggedDoors, waterloggedLeaves;

    public Waterlogged(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder .translation(PhysEX.MOD_ID + ".config." + name);

        innerBuilder.push("Waterlogged States");

        waterloggedDoors = builder.apply("addWaterloggedDoors").define("addWaterloggedDoors", true);
        waterloggedLeaves = builder.apply("addWaterloggedLeaves").define("addWaterloggedLeaves", true);

        innerBuilder.pop();
    }
}