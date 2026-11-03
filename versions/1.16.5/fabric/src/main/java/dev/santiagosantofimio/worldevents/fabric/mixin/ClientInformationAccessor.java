package dev.santiagosantofimio.worldevents.fabric.mixin;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The language in the client settings packet, which 1.16 only reads on the client. */
@Mixin(ServerboundClientInformationPacket.class)
public interface ClientInformationAccessor {
	@Accessor("language")
	String worldevents$language();
}
