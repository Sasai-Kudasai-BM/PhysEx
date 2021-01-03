package net.skds.physex.mixins.other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;

@Mixin(value = { ServerWorld.class })
public interface ServerWorldAccessor {

	@Invoker("hasDuplicateEntity")
	public boolean hasDuplicateEntityInv(Entity entityIn);

	@Invoker("onEntityAdded")
	public void onEntityAddedInv(Entity entityIn);
}