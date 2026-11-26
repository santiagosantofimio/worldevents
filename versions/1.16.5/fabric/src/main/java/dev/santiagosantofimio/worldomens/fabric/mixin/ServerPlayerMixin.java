package dev.santiagosantofimio.worldomens.fabric.mixin;

import dev.santiagosantofimio.worldomens.fabric.FabricPlatform;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "updateOptions", at = @At("HEAD"))
	private void worldomens$rememberLanguage(ServerboundClientInformationPacket packet, CallbackInfo ci) {
		FabricPlatform.rememberLanguage((ServerPlayer) (Object) this, ((ClientInformationAccessor) packet).worldomens$language());
	}
}
