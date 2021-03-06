package net.skds.physex;

import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.skds.physex.fluidphysics.FFluidStatic;
import net.skds.physex.util.blockupdate.BlockUpdataer;
import net.skds.physex.util.blockupdate.WWSGlobal;
import net.skds.physex.util.pars.ParsApplier;

public class Events {

    private static long inTickTime = System.nanoTime();
    private static int lastTickTime = 0;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void test(PistonEvent.Pre e) {
        FFluidStatic.onPistonPre(e);
    }

    // @SubscribeEvent
    // public void attachCapability(AttachCapabilitiesEvent<Chunk> e) {
    // new ChunkDataProvider().init(e);
    // }

    @SubscribeEvent
    public void onBucketEvent(FillBucketEvent e) {
        if (PhysEXConfig.COMMON.finiteFluids.get()) {
            FFluidStatic.onBucketEvent(e);
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent e) {
        FFluidStatic.onBlockPlace(e);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {

        if (!e.getWorld().isRemote()) {
            ServerWorld w = (ServerWorld) e.getWorld();
            WWSGlobal.unloadWorld(w);
            BlockUpdataer.onWorldUnload((ServerWorld) e.getWorld());
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {

        if (!e.getWorld().isRemote()) {
            ServerWorld w = (ServerWorld) e.getWorld();            
            WWSGlobal.loadWorld(w);
            BlockUpdataer.onWorldLoad((ServerWorld) e.getWorld());
        }
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent e) {
        // ParsApplier.refresh();
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent.CustomTagTypes e) {
        ParsApplier.refresh();
        // System.out.println("hhhhhhhhhhhhhhhhhhhh");
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        boolean in = event.phase == Phase.START;
        if (in) {
            inTickTime = System.nanoTime();
            WWSGlobal.tickInG();
        }
        BlockUpdataer.tick(in);
        if (!in) {
            WWSGlobal.tickOutG();
            
            lastTickTime = (int) (System.nanoTime() - inTickTime);
        }
    }

    public static int getLastTickTime() {
        return lastTickTime;
    }

    public static int getRemainingTickTimeNanos() {
        return 50_000_000 - (int) (System.nanoTime() - inTickTime);
    }

    public static int getRemainingTickTimeMicros() {
        return getRemainingTickTimeNanos() / 1000;
    }

    public static int getRemainingTickTimeMilis() {
        return getRemainingTickTimeNanos() / 1000_000;
    }

}