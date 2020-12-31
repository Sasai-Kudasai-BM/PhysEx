package net.skds.physex.mixins.other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import net.skds.physex.util.Interface.IServerChunkProvider;

@Mixin(value = { ServerChunkProvider.class })
public class ServerChunkProviderMixin implements IServerChunkProvider {

    public IChunk getCustomChunk(long l) {
        /*
         * for (int j = 0; j < 4; ++j) { if (l == this.recentPositions[j] &&
         * this.recentStatuses[j] == ChunkStatus.FULL) { IChunk ichunk =
         * this.recentChunks[j]; return ichunk instanceof Chunk ? (Chunk) ichunk : null;
         * } }
         */
        ChunkHolder chunkHolder = this.func_217213_a(l);
        if (chunkHolder == null) {
            return null;
        }
        return chunkHolder.func_219287_e();
    }

    @Shadow
    private ChunkHolder func_217213_a(long l) {
        return null;
    }
    /*
    @Inject(method = "Lnet/minecraft/world/server/ServerChunkProvider;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;", at = @At(value = "HEAD", ordinal = 0), cancellable = true)
    private void gc(int x, int z, ChunkStatus status, boolean b, CallbackInfoReturnable<IChunk> ci) {
        if (Thread.currentThread() instanceof ForceChunkTaker) {
            long lpos = ChunkPos.asLong(x >> 4, z >> 4);
            ChunkHolder chunkHolder = this.func_217213_a(lpos);
            if (chunkHolder != null) {
                IChunk c = chunkHolder.func_219287_e();
                if (c != null) {
                    ci.setReturnValue(c);
                }
            }
        }
    }
    */
}