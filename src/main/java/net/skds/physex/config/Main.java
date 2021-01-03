package net.skds.physex.config;

import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec;
import net.skds.physex.PhysEX;
import net.skds.physex.PhysEXConfig;

public class Main {

    public final ForgeConfigSpec.BooleanValue blockphysics, fluidRender, slide, finiteFluids;
    public final ForgeConfigSpec.IntValue maxSlideDist, maxEqDist, maxBucketDist, minBlockUpdates;

    // public final ForgeConfigSpec.ConfigValue<ArrayList<String>> ss;
    // private final ForgeConfigSpec.IntValue maxFluidLevel;

    public Main(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder .translation(PhysEX.MOD_ID + ".config." + name);

        innerBuilder.push("General");

        minBlockUpdates = builder.apply("minBlockUpdates").comment("Minimal block updates per tick").defineInRange("minBlockUpdates", 500, 0, 100_000);

        innerBuilder.pop().push("Visual");

        fluidRender = builder.apply("setFluidRenderType").comment("Tweaks shape of fluid blocks.\n False -> default \n True -> New advanced").define("setFluidRenderType", true);

        innerBuilder.pop().push("Fluids");

        finiteFluids = builder.apply("setFiniteFluids").comment("OwO").define("setFiniteFluids", true);
        slide = builder.apply("setSlide").comment("Will fluids slide down from hills").define("setSlide", true);
        maxEqDist = builder.apply("setMaxEqualizeDistance").comment("UwU").defineInRange("setMaxEqualizeDistance", 16, 0, 256);
        maxSlideDist = builder.apply("setMaxSlidingDistance").comment("-_-").defineInRange("setMaxSlidingDistance", 5, 0, 256);
        maxBucketDist = builder.apply("setMaxBucketDistance").comment("^u^").defineInRange("setMaxBucketDistance", 8, 0, PhysEXConfig.MAX_FLUID_LEVEL);

        innerBuilder.pop().push("Blocks");

        blockphysics = builder.apply("enableBlockPhysics").comment("!!! DON'T TOUCH IT !!!\n Work in progress...").define("enableBlockPhysics", false);

        innerBuilder.pop();
    }
}