package dev.santiagosantofimio.worldevents.neoforge.test;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.test.LoaderBridgeTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the test function that {@code test_instance/loader_bridge.json} runs. Only built with {@code -Pgametests}. */
@Mod(WorldEventsMod.MOD_ID)
public class WorldEventsNeoForgeTests {
	public WorldEventsNeoForgeTests(IEventBus modBus) {
		DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, WorldEventsMod.MOD_ID);
		functions.register(LoaderBridgeTests.NAME, () -> LoaderBridgeTests::run);
		functions.register(modBus);
	}
}
