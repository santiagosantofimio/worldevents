package dev.santiagosantofimio.worldevents.test;

import com.mojang.authlib.GameProfile;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.EventScores;
import dev.santiagosantofimio.worldevents.event.impl.FireflyNightEvent;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class FireflyNightGameTests {
	@GameTest(environment = "worldevents-test:firefly_night", maxTicks = 100)
	public void lightsRewardExplorationAndKeepProgress(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long originalTime = level.getDayTime();
		EventManager.stop(level);
		WorldEventsData.get(level).setPlan(null);
		FireflyNightEvent event = (FireflyNightEvent) EventRegistry.get(FireflyNightEvent.ID);
		ServerPlayer player = createPlayer(level);
		ServerPlayer other = createPlayer(level);
		other.setGameMode(GameType.SPECTATOR);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
		for (int x = -20; x <= 20; x++) {
			for (int z = -20; z <= 20; z++) {
				level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
				for (int y = 0; y <= 5; y++) {
					level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
		player.teleportTo(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 0, 0, true);
		helper.runAfterDelay(5, () -> {
			try {
				helper.assertTrue(event.isAvailable(level), "missing overworld availability");
				helper.assertFalse(event.isAvailable(level.getServer().getLevel(Level.NETHER)), "available in Nether");
				helper.assertFalse(event.isAvailable(level.getServer().getLevel(Level.END)), "available in End");
				level.setDayTime(6000);
				helper.assertFalse(event.canStart(level), "natural daytime start");
				level.setDayTime(18000);
				helper.assertTrue(event.canStart(level), "cannot start at night");
				helper.assertTrue(EventManager.start(level, event), "event failed to start");
				ActiveEvent active = EventManager.active(level);
				int xp = player.totalExperience;
				event.tick(level, active);
				BlockPos light = FireflyNightEvent.target(active, player);
				helper.assertTrue(light != null, "no light on open ground");
				helper.assertTrue(level.canSeeSky(light), "light under a roof");
				ActiveEvent pending = ActiveEvent.CODEC.parse(NbtOps.INSTANCE,
						ActiveEvent.CODEC.encodeStart(NbtOps.INSTANCE, active).getOrThrow()).getOrThrow();
				helper.assertTrue(light.equals(FireflyNightEvent.target(pending, player)), "pending light lost on reload");
				helper.assertTrue(FireflyNightEvent.target(active, other) == null, "spectator received a light");
				helper.assertTrue(Lang.text("es_es", "event.firefly_night.name").equals("Noche de luciérnagas"), "Spanish title");
				helper.assertTrue(Lang.text("en_us", "event.firefly_night.name").equals("Firefly Night"), "English title");
				event.tick(level, active);
				helper.assertValueEqual(player.totalExperience, xp, "standing still earned XP");
				helper.assertTrue(EventScores.ranking(active).isEmpty(), "standing still earned points");
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(light.getX() + 0.5, light.getY(), light.getZ() + 0.5);
				event.tick(level, active);
				helper.assertValueEqual(player.totalExperience, xp, "spectator earned XP");
				player.setGameMode(GameType.SURVIVAL);
				for (int found = 1; found <= 5; found++) {
					light = FireflyNightEvent.target(active, player);
					helper.assertTrue(light != null, "missing light " + found);
					player.teleportTo(light.getX() + 0.5, light.getY(), light.getZ() + 0.5);
					event.tick(level, active);
					helper.assertValueEqual(player.totalExperience, xp + 5 * found, "XP per light");
					ActiveEvent restored = ActiveEvent.CODEC.parse(NbtOps.INSTANCE,
							ActiveEvent.CODEC.encodeStart(NbtOps.INSTANCE, active).getOrThrow()).getOrThrow();
					helper.assertValueEqual(restored.data().getCompoundOrEmpty(player.getUUID().toString()).getIntOr("found", 0), found, "saved finds");
					event.tick(level, restored);
					helper.assertValueEqual(player.totalExperience, xp + 5 * found, "reload repeated reward");
					player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
					event.tick(level, active);
					if (found < 5) {
						helper.assertTrue(FireflyNightEvent.target(active, player) != null, "next light missing");
					}
				}
				helper.assertTrue(Advancements.has(player, "follow_the_lights"), "missing advancement");
				helper.assertValueEqual(EventScores.ranking(active).getFirst().points(), EventManager.isConvergence(active) ? 20 : 10, "exploration score");
				event.tick(level, active);
				helper.assertTrue(FireflyNightEvent.target(active, player) == null, "light beyond reward limit");
				helper.assertValueEqual(player.totalExperience, xp + 25, "reward limit");
				other.setGameMode(GameType.SURVIVAL);
				other.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
				event.tick(level, active);
				BlockPos otherLight = FireflyNightEvent.target(active, other);
				helper.assertTrue(otherLight != null, "second player lost their own route");
				other.teleportTo(otherLight.getX() + 0.5, otherLight.getY(), otherLight.getZ() + 0.5);
				event.tick(level, active);
				helper.assertValueEqual(other.totalExperience, 5, "second player's reward");
				helper.assertValueEqual(player.totalExperience, xp + 25, "shared rewards");
				level.setDayTime(0);
				helper.assertTrue(event.shouldEndEarly(level, active), "did not end at dawn");
				EventManager.stop(level);
				helper.assertTrue(EventManager.active(level) == null, "event still running");
				helper.succeed();
			} finally {
				EventManager.stop(level);
				level.getServer().getPlayerList().remove(player);
				level.getServer().getPlayerList().remove(other);
				level.setDayTime((int) originalTime);
			}
		});
	}

	private static ServerPlayer createPlayer(ServerLevel level) {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		return player;
	}
}
