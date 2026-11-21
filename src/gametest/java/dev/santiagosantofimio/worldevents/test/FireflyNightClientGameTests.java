package dev.santiagosantofimio.worldevents.test;

import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.impl.FireflyNightEvent;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FireflyNightClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runCommand("difficulty peaceful");
            singleplayer.getServer().runCommand("time set midnight");
            singleplayer.getServer().runCommand("gamemode survival @a");
            AtomicReference<Float> direction = new AtomicReference<>();
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ServerLevel level = player.level();
                EventManager.stop(level);
                FireflyNightEvent event = (FireflyNightEvent) EventRegistry.get(FireflyNightEvent.ID);
                if (!EventManager.start(level, event)) {
                    throw new AssertionError("firefly night did not start");
                }
                ActiveEvent active = EventManager.active(level);
                event.tick(level, active);
                BlockPos target = FireflyNightEvent.target(active, player);
                if (target == null) {
                    throw new AssertionError("firefly light did not appear");
                }
                double dx = target.getX() + 0.5 - player.getX();
                double dz = target.getZ() + 0.5 - player.getZ();
                direction.set((float) Math.toDegrees(Math.atan2(-dx, dz)));
            });
            context.getInput().lookAt(direction.get(), 5.0f);
            context.waitTicks(120);
            context.takeScreenshot("worldevents_firefly_night");
        }
    }
}
