package dev.santiagosantofimio.worldevents.forge.test;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.test.LoaderBridgeTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/** Registers the test function that {@code test_instance/loader_bridge.json} runs. Only built with {@code -Pgametests}. */
@Mod.EventBusSubscriber(modid = WorldEventsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WorldEventsForgeTests {
	private WorldEventsForgeTests() {
	}

	@SubscribeEvent
	public static void register(RegisterEvent event) {
		event.register(Registries.TEST_FUNCTION, helper -> helper.register(
				Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, LoaderBridgeTests.NAME), (Consumer<GameTestHelper>) LoaderBridgeTests::run));
	}
}
