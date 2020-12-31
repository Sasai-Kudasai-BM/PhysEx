package net.skds.physex;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

public class PhysEXConfig {

    public static final PhysEXConfig COMMON;
    private static final ForgeConfigSpec SPEC;


    //public static final boolean FINITE_WATER;
    //public static final int MAX_EQ_DIST;

    public static final int MAX_FLUID_LEVEL = 8;


    public final ForgeConfigSpec.BooleanValue slide;
    public final ForgeConfigSpec.BooleanValue finiteFluids;
    public final ForgeConfigSpec.IntValue maxSlideDist;
    public final ForgeConfigSpec.IntValue maxEqDist;
    public final ForgeConfigSpec.IntValue maxBucketDist;
    

    //public final ForgeConfigSpec.ConfigValue<ArrayList<String>> ss;
    //private final ForgeConfigSpec.IntValue maxFluidLevel;


    PhysEXConfig(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder.translation(PhysEX.MOD_ID + ".config." + name);

        innerBuilder.push("general");

        finiteFluids = builder.apply("setFiniteFluids").comment("OwO").define("setFiniteFluids", true);
        slide = builder.apply("setSlide").comment("Will fluids slide down from hills").define("setSlide", true);
        maxEqDist = builder.apply("setMaxEqualizeDistance").comment("UwU").defineInRange("setMaxEqualizeDistance", 16, 0, 256);
        maxSlideDist = builder.apply("setMaxSlidingDistance").comment("-_-").defineInRange("setMaxSlidingDistance", 5, 0, 256);
        maxBucketDist = builder.apply("setMaxBucketDistance").comment("^u^").defineInRange("setMaxBucketDistance", 8, 0, MAX_FLUID_LEVEL);

        //innerBuilder.pop().push("EX");
        //ss=null;
        //ArrayList<String> list = new ArrayList<>();
        //list.add("e");
        //list.add("gg");
        //ss = builder.apply("biba").define("biba", list);
        innerBuilder.pop();
        
    }    
    
    static {
        Pair<PhysEXConfig, ForgeConfigSpec> cm = new ForgeConfigSpec.Builder().configure(PhysEXConfig::new);
        COMMON = cm.getLeft();
        SPEC = cm.getRight();

        //FINITE_WATER = COMMON.finiteWater.get();
        //MAX_EQ_DIST = COMMON.maxEqDist.get();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(Type.COMMON, SPEC);
    }
}