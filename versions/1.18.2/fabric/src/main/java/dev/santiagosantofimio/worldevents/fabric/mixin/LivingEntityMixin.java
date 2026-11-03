package dev.santiagosantofimio.worldevents.fabric.mixin;

import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The damage and death hooks, which Fabric API for 1.18 does not have. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Health and absorption before the hit, to work out how much it took. */
	@Unique
	private float worldevents$healthBefore;

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void worldevents$allowDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.level.isClientSide()) {
			return;
		}
		if (!Hooks.allowDamage(entity, source, amount)) {
			cir.setReturnValue(false);
			return;
		}
		worldevents$healthBefore = entity.getHealth() + entity.getAbsorptionAmount();
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void worldevents$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.level.isClientSide() && cir.getReturnValueZ()) {
			float taken = Math.max(0.0f, worldevents$healthBefore - entity.getHealth() - entity.getAbsorptionAmount());
			Hooks.afterDamage(entity, source, amount, taken, false);
		}
	}

	@Inject(method = "die", at = @At("TAIL"))
	private void worldevents$afterDeath(DamageSource source, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.level.isClientSide()) {
			Hooks.afterDeath(entity, source);
		}
	}
}
