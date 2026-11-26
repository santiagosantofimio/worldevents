package dev.santiagosantofimio.worldomens.fabric.mixin;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundClientInformationPacket.class)
public interface ClientInformationAccessor {
	@Accessor("language")
	String worldomens$language();
}
