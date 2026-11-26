package dev.santiagosantofimio.worldomens.fabric.mixin;

import dev.santiagosantofimio.worldomens.platform.Hooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private float worldomens$healthBefore;

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void worldomens$allowDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.level.isClientSide()) {
			return;
		}
		if (!Hooks.allowDamage(entity, source, amount)) {
			cir.setReturnValue(false);
			return;
		}
		worldomens$healthBefore = entity.getHealth() + entity.getAbsorptionAmount();
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void worldomens$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.level.isClientSide() && cir.getReturnValueZ()) {
			float taken = Math.max(0.0f, worldomens$healthBefore - entity.getHealth() - entity.getAbsorptionAmount());
			Hooks.afterDamage(entity, source, amount, taken, false);
		}
	}

	@Inject(method = "die", at = @At("TAIL"))
	private void worldomens$afterDeath(DamageSource source, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.level.isClientSide()) {
			Hooks.afterDeath(entity, source);
		}
	}
}
