package net.skds.physex.util.blockupdate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.physex.Events;
import net.skds.physex.PhysEXConfig;
import net.skds.physex.fluidphysics.FluidTasksManager;

public class BlockUpdataer {

    private static final ConcurrentLinkedQueue<Entity> entitiesToAdd = new ConcurrentLinkedQueue<>();

    private static float upus = 0.1F;
    private static int avgTime = 0;

    private static int avgTickTime = 0;
    private static int[] timeTickList = new int[14];
    private static int indexT2 = 0;

    private static float RemMRUC = 0;
    private static int MRUC = 0;
    private static int updateCounter = 0;
    private static int indexUp = 0;
    private static int indexT = 0;
    private static int indexForFill = 0;
    private static float[] upusList = new float[32];
    private static int[] timeList = new int[32];

    public static float getUPus() {
        return upus;
    }

    public static int getAvgTime() {
        return avgTime;
    }

    public static boolean applyTask(float weight) {
        boolean bl = RemMRUC > 0F;
        if (bl) {
            RemMRUC -= weight;
        }
        return bl;
    }

    public static int getMaxRecomendedUpdateCount() {
        return MRUC;
    }

    private static void calcUPus(int time) {
        // long T1 = System.nanoTime();
        int lenUp = upusList.length;
        if (indexUp >= lenUp) {
            indexUp = 0;
            indexForFill = lenUp;
        }
        indexForFill = Math.max(indexUp, indexForFill);

        if (time > 0) {
            upusList[indexUp] = (float) (1F * (float) updateCounter / (float) time);
            ++indexUp;
        }
        if (indexForFill > 0) {
            float sum = 0;
            for (int i = 0; i < indexForFill; ++i) {
                sum += upusList[i];
            }
            upus = sum / indexForFill;
        }

        int lenT = timeList.length;
        ++indexT;
        if (indexT >= lenT) {
            indexT = 0;
        }
        timeList[indexT] = time;
        int sum = 0;
        int c = 0;
        for (int i : timeList) {
            sum += i;
            ++c;
        }
        avgTime = sum / c;
    }

    private static void calcMidTickTime() {

        int lenT = timeTickList.length;
        ++indexT2;
        if (indexT2 >= lenT) {
            indexT2 = 0;
        }
        int t = Events.getLastTickTime() / 1000;
        timeTickList[indexT2] = t;
        int sum = 0;
        int c = 0;
        for (int i : timeTickList) {
            sum += i;
            ++c;
        }
        avgTickTime = sum / c;
    }

    private static void calcMRUC() {
        int RT = 50_000 - avgTickTime;
        int RTavgF = RT;
        int PreMRUC = (int) ((float) RTavgF * upus);
        MRUC = Math.max(PhysEXConfig.COMMON.minBlockUpdates.get(), PreMRUC);
        RemMRUC = MRUC;
        // System.out.println(avgTickTime);
    }

    // private static Map<ServerWorld, ConcurrentHashMap<Long, UpdateTask>>
    // BLOCK_UPDATES_WORLDS = new HashMap<>();
    private static Map<ServerWorld, ConcurrentLinkedQueue<UpdateTask>> BLOCK_UPDATES_WORLDS = new HashMap<>();

    public static void onWorldLoad(ServerWorld w) {
        ConcurrentLinkedQueue<UpdateTask> map = new ConcurrentLinkedQueue<>();
        BLOCK_UPDATES_WORLDS.put(w, map);
    }

    public static void onWorldUnload(ServerWorld w) {
        BLOCK_UPDATES_WORLDS.remove(w);
        entitiesToAdd.forEach((e) -> {
            if (e.world == w) {

                e.world.addEntity(e);
                entitiesToAdd.remove(e);
            }
        });
    }

    public static void addUpdate(ServerWorld w, BlockPos pos, BlockState newState, BlockState oldState, int flags) {
        if (newState == oldState) {
            return;
            // System.out.println(newState);
        }
        ConcurrentLinkedQueue<UpdateTask> bul = BLOCK_UPDATES_WORLDS.get(w);
        if (bul == null) {
            return;
        }
        /*
         * long lp = pos.toLong(); UpdateTask ot = bul.get(lp); if (ot != null) {
         * ot.newState(newState); } else { bul.put(lp, new UpdateTask(pos, newState,
         * oldState, flags)); }
         */
        bul.add(new UpdateTask(pos, newState, oldState, flags));
    }

    private static int inint = 0;

    public static void tick(boolean in) {
        long initT = System.nanoTime();
        Entity e;

        if (in) {

            while ((e = entitiesToAdd.poll()) != null) {
                e.world.addEntity(e);
            }
            BLOCK_UPDATES_WORLDS.forEach((w, set) -> {
                while (!set.isEmpty()) {
                    UpdateTask task = set.poll();
                    task.update(w);
                    ++updateCounter;
                }
            });
        }

        int time = (int) (System.nanoTime() - initT) / 1000;
        if (in) {
            inint = time;
        } else {
            calcMidTickTime();
            calcUPus((int) (time + inint + FluidTasksManager.getLastTimeMicros()));
            calcMRUC();
            updateCounter = 0;
            // System.out.println(MRUC);
        }
    }

    public static void addEntity(Entity e) {
        entitiesToAdd.add(e);
    }
}