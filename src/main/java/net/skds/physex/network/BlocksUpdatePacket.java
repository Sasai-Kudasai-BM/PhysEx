package net.skds.physex.network;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.skds.physex.util.blockupdate.UpdateTask;

public class BlocksUpdatePacket {

	private static final String POS_KEY = "pos";
	private static final String NEW_STATE_KEY = "ns";
	private static final String OLD_STATE_KEY = "os";
	private static final String FLAGS_KEY = "f";
	private static final String DROP_KEY = "d";

	private Queue<UpdateTask> queue;

	public BlocksUpdatePacket(Queue<UpdateTask> queue) {
		this.queue = queue;
	}

	public BlocksUpdatePacket(PacketBuffer buffer) {
		queue = new LinkedBlockingQueue<>();
		// CompoundNBT nbt = buffer.readCompoundTag();
		// ListNBT nbtlist = (ListNBT) ((CompoundNBT) nbt).get(LIST_KEY);
		// for (int i = 0; i < nbtlist.size(); ++i) {

		// for ( int i = buffer.capacity(); i > 0; --i) {
		boolean bl = true;
		int i = 0;
		// long l = System.nanoTime();
		while (bl) {
			try {
				CompoundNBT enbt = buffer.readCompoundTag();
				BlockPos pos = NBTUtil.readBlockPos(enbt.getCompound(POS_KEY));
				BlockState oldState = NBTUtil.readBlockState(enbt.getCompound(OLD_STATE_KEY));
				BlockState newState = NBTUtil.readBlockState(enbt.getCompound(NEW_STATE_KEY));
				int flags = enbt.getInt(FLAGS_KEY);
				boolean drop = enbt.getBoolean(DROP_KEY);

				UpdateTask task = new UpdateTask(pos, newState, oldState, flags, drop);

				queue.add(task);
				++i;
			} catch (Exception e) {
				bl = false;
				// System.out.println((System.nanoTime()- l) / 1000);
				break;
			}
		}
		if (i > 0) {
			System.out.println(i + " lol");
		}
	}

	void encoder(PacketBuffer buffer) {
		queue.forEach((task) -> {
			CompoundNBT nbt = new CompoundNBT();
			nbt.put(POS_KEY, NBTUtil.writeBlockPos(task.pos));
			nbt.put(OLD_STATE_KEY, NBTUtil.writeBlockState(task.oldState));
			nbt.put(NEW_STATE_KEY, NBTUtil.writeBlockState(task.newState));
			nbt.putInt(FLAGS_KEY, task.flags);
			nbt.putBoolean(FLAGS_KEY, task.drop);
			buffer.writeCompoundTag(nbt);
		});
	}

	public static BlocksUpdatePacket decoder(PacketBuffer buffer) {
		return new BlocksUpdatePacket(buffer);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			ClientWorld w = (ClientWorld) minecraft.player.world;
			queue.forEach((task) -> {
				task.updateClient(w);
			});
			// System.out.println(queue);
		});

		context.get().setPacketHandled(true);
	}
}