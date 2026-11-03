package dev.santiagosantofimio.worldevents.fabric.mixin;

import dev.santiagosantofimio.worldevents.fabric.FabricPlatform;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers each player's game language, which the mod's texts are picked by. 1.18 servers do not keep it. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "updateOptions", at = @At("HEAD"))
	private void worldevents$rememberLanguage(ServerboundClientInformationPacket packet, CallbackInfo ci) {
		FabricPlatform.rememberLanguage((ServerPlayer) (Object) this, packet.language());
	}
}
