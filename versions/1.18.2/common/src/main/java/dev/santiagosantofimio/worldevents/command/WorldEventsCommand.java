package dev.santiagosantofimio.worldevents.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.util.DropMath;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** /worldevents (alias /eventos): list, start, stop, give, summon, status, reload. Vanilla argument types only. */
public final class WorldEventsCommand {
	private WorldEventsCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(build("worldevents"));
		// A redirect alias would not run the bare command, so the tree is simply built twice.
		dispatcher.register(build("eventos"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
		return Commands.literal(name)
				.requires(source -> source.hasPermission(2))
				.executes(WorldEventsCommand::status)
				.then(Commands.literal("list").executes(WorldEventsCommand::list))
				.then(Commands.literal("start")
						.then(Commands.argument("id", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										EventRegistry.all().stream().map(WorldEvent::id), builder))
								.executes(WorldEventsCommand::start)))
				.then(Commands.literal("stop").executes(WorldEventsCommand::stop))
				.then(Commands.literal("give")
						.then(Commands.argument("targets", EntityArgument.players())
								.then(Commands.argument("item", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(giveableIds(), builder))
										.executes(context -> give(context, 1))
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(context -> give(context, IntegerArgumentType.getInteger(context, "count")))))))
				.then(Commands.literal("summon")
						.then(Commands.argument("creature", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Creature.kinds().stream().map(Creature.Kind::id), builder))
								.executes(context -> summon(context, new BlockPos(context.getSource().getPosition())))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> summon(context, BlockPosArgument.getSpawnablePos(context, "pos"))))))
				.then(Commands.literal("status").executes(WorldEventsCommand::status))
				.then(Commands.literal("reload").executes(WorldEventsCommand::reload));
	}

	private static String eventName(CommandSourceStack source, WorldEvent event) {
		return Lang.text(Lang.language(source), "event." + event.id() + ".name");
	}

	private static String dimensionName(ServerLevel level) {
		return level.dimension().location().toString();
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		WorldEventsData data = WorldEventsData.get(level);
		long now = level.getGameTime();
		source.sendSuccess(Lang.component(source, "command.list.header"), false);
		for (WorldEvent event : EventRegistry.all()) {
			EventSettings settings = event.settings();
			long cooldown = EventManager.cooldownLeft(data, event, now);
			String state = Lang.text(Lang.language(source), settings.enabled() ? "command.list.enabled" : "command.list.disabled");
			String timing = cooldown > 0
					? Lang.text(Lang.language(source), "command.list.cooldown", DropMath.formatTicks(cooldown))
					: Lang.text(Lang.language(source), "command.list.ready");
			Component line = Lang.component(source, "command.list.entry", eventName(source, event), event.id(), settings.weight(),
					state + ", " + timing).withStyle(settings.enabled() ? event.textColor() : ChatFormatting.GRAY);
			source.sendSuccess(line, false);
		}
		return EventRegistry.all().size();
	}

	private static int start(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "id");
		WorldEvent event = EventRegistry.get(id);
		if (event == null) {
			source.sendFailure(Lang.component(source, "command.start.unknown", id));
			return 0;
		}
		ServerLevel level = source.getLevel();
		if (!event.isAvailable(level)) {
			source.sendFailure(Lang.component(source, "command.start.unavailable", eventName(source, event)));
			return 0;
		}
		ActiveEvent running = EventManager.active(level);
		if (running != null) {
			source.sendFailure(Lang.component(source, "command.start.busy", eventName(source, running.event())));
			return 0;
		}
		EventManager.start(level, event);
		source.sendSuccess(Lang.component(source, "command.start.success", eventName(source, event), dimensionName(level)), true);
		return 1;
	}

	private static int stop(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		WorldEvent stopped = EventManager.stop(source.getLevel());
		if (stopped == null) {
			source.sendFailure(Lang.component(source, "command.stop.none"));
			return 0;
		}
		source.sendSuccess(Lang.component(source, "command.stop.success", eventName(source, stopped)), true);
		return 1;
	}

	/** The special items, then the creature eggs ({@code <creature>_spawn_egg}). */
	private static Stream<String> giveableIds() {
		return Stream.concat(EventItems.all().stream().map(EventItems.EventItem::id), Creature.kinds().stream().map(EventItems::eggId));
	}

	private static int give(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "item");
		EventItems.EventItem item = EventItems.get(id);
		Creature.Kind egg = item == null ? EventItems.eggKind(id) : null;
		if (item == null && egg == null) {
			source.sendFailure(Lang.component(source, "command.give.unknown", id));
			return 0;
		}
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
		for (ServerPlayer player : targets) {
			String language = Lang.language(player);
			ItemStack stack = item != null ? item.create(language, count) : EventItems.egg(egg, language, count);
			if (!player.getInventory().add(stack)) {
				// Inventory full: drop the rest at their feet.
				Drops.spawn(player.getLevel(), player.position(), stack);
			}
		}
		String language = Lang.language(source);
		String name = item != null ? Lang.text(language, item.nameKey()) : EventItems.eggName(egg, language);
		source.sendSuccess(Lang.component(source, "command.give.success", count, name, targets.size()), true);
		return targets.size();
	}

	/** Summons a creature outside of any event: it stays until defeated. */
	private static int summon(CommandContext<CommandSourceStack> context, BlockPos pos) {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "creature");
		Creature.Kind kind = Creature.kind(id);
		if (kind == null) {
			source.sendFailure(Lang.component(source, "command.summon.unknown", id));
			return 0;
		}
		ServerLevel level = source.getLevel();
		ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
		String name = Lang.text(Lang.language(source), kind.nameKey());
		String language = player != null ? Lang.language(player) : Lang.languageNear(level, Vec3.atCenterOf(pos), 64.0);
		if (!level.isLoaded(pos) || Creature.summon(kind, level, pos, language, player) == null) {
			source.sendFailure(Lang.component(source, "command.summon.failed", name));
			return 0;
		}
		source.sendSuccess(Lang.component(source, "command.summon.success", name), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		int active = 0;
		for (ServerLevel level : source.getServer().getAllLevels()) {
			WorldEventsData data = WorldEventsData.get(level);
			ActiveEvent running = data.active();
			Component line;
			if (running != null) {
				active++;
				line = Lang.component(source, "command.status.active", dimensionName(level), eventName(source, running.event()),
						DropMath.formatTicks(running.remainingTicks())).withStyle(running.event().textColor());
			} else if (EventManager.planned(level) != null) {
				line = Lang.component(source, "command.status.planned", dimensionName(level), eventName(source, EventManager.planned(level)));
			} else {
				long wait = EventManager.globalCooldownLeft(data, level.getGameTime());
				line = wait > 0
						? Lang.component(source, "command.status.idle", dimensionName(level), DropMath.formatTicks(wait))
						: Lang.component(source, "command.status.ready", dimensionName(level));
			}
			source.sendSuccess(line, false);
		}
		return active;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (ModConfig.load()) {
			source.sendSuccess(Lang.component(source, "command.reload.success"), true);
			return 1;
		}
		source.sendFailure(Lang.component(source, "command.reload.failure"));
		return 0;
	}
}
