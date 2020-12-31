package net.skds.physex.mixins.other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.skds.physex.PhysEXConfig;

@Mixin(value = { BoatEntity.class })
public class BoatEntityMixin {

	@Inject(method = "Lnet/minecraft/entity/item/BoatEntity;getUnderwaterStatus()Lnet/minecraft/entity/item/BoatEntity$Status;", at = @At(value = "RETURN", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;isSource()Z")), cancellable = true)
	public void aaa(CallbackInfoReturnable<BoatEntity.Status> ci) {
		if (PhysEXConfig.COMMON.finiteFluids.get()) {
			ci.setReturnValue(BoatEntity.Status.IN_WATER);
		}
	}

	@Redirect(method = "Lnet/minecraft/entity/item/BoatEntity;checkInWater()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(D)I", ordinal = 1))
	public int ccc(double d) {
		if (PhysEXConfig.COMMON.finiteFluids.get()) {
			return MathHelper.ceil(d) + 1;
		}
		return MathHelper.ceil(d);
	}
}