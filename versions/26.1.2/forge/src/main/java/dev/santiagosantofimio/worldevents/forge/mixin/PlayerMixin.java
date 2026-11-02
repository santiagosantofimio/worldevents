package dev.santiagosantofimio.worldevents.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Forge has no event for right-clicking an entity (only for the armor stand's precise click), so this adds one. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
	private void worldevents$useEntity(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = (Player) (Object) this;
		InteractionResult result = Hooks.useEntity(player, player.level(), hand, entity, new EntityHitResult(entity, location));
		if (result != InteractionResult.PASS) {
			cir.setReturnValue(result);
		}
	}
}
