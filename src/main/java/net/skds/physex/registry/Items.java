package net.skds.physex.registry;

import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.physex.PhysEX;
import net.skds.physex.item.AdvancedBucket;

public class Items {
	
    public static final ItemGroup CTAB = (new ItemGroup(ItemGroup.getGroupCountSafe(), "PhysEx") {
    
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ADVANCED_BUCKET.get());
        }
    }).setTabPath("physex");


    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PhysEX.MOD_ID);
    
	public static final RegistryObject<Item> ADVANCED_BUCKET = ITEMS.register("advanced_bucket", () -> AdvancedBucket.getBucketForReg(Fluids.EMPTY));
	
	public static void register() {
		ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}