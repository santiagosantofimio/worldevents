package dev.santiagosantofimio.worldevents.mob;

import com.mojang.math.Transformation;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.LocalizedBossBar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.CustomModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The mod's own creatures, made so vanilla clients can see them. An invisible vanilla mob does the moving, fighting
 * and colliding; item displays riding it show the creature's 3D model from the resource pack. Without the pack,
 * players see the display's base block floating where the creature is, with its name above it.
 *
 * <p>Each creature takes care of itself wherever it is: this class animates every loaded creature and runs its
 * abilities and boss bar, whether its event brought it or it was summoned with an egg or a command. Events only
 * decide when their creatures come and go.
 */
public final class Creature {
	public static final String PART_TAG = WorldEventsMod.MOD_ID + ".creature_part";
	/** Creatures summoned with an egg or a command: no event tracks them, and they stay until defeated. */
	public static final String SUMMONED_TAG = WorldEventsMod.MOD_ID + ".summoned";
	private static final int ANIMATION_TICKS = 10;
	private static final double BOSS_BAR_DISTANCE = 64.0;

	/**
	 * One visible part. {@code base} is the vanilla block shown without the resource pack; {@code offsetY} is the
	 * height of the part's centre above the middle of the mob's hitbox, in blocks.
	 */
	public record Part(Item base, String modelId, float scale, float offsetY, boolean fullBright, int glowColor, boolean doubleSize, Motion motion) {
		public static Part of(Item base, String modelId, float scale) {
			return new Part(base, modelId, scale, 0.0f, false, -1, false, Motion.NONE);
		}

		public Part above(float offsetY) {
			return new Part(base, modelId, scale, offsetY, fullBright, glowColor, doubleSize, motion);
		}

		/** Ignores light, so it stands out at night (glowing eyes, lava…). */
		public Part bright() {
			return new Part(base, modelId, scale, offsetY, true, glowColor, doubleSize, motion);
		}

		/** Glowing outline in this colour, visible through walls. */
		public Part outline(int rgb) {
			return new Part(base, modelId, scale, offsetY, fullBright, rgb, doubleSize, motion);
		}

		/**
		 * The model is twice the size of a block ({@code tools/import_model.py --big}), for tall creatures: at the
		 * same size on screen, the vanilla block shown without the resource pack is half as big.
		 */
		public Part big() {
			return new Part(base, modelId, scale, offsetY, fullBright, glowColor, true, motion);
		}

		public Part moving(Motion motion) {
			return new Part(base, modelId, scale, offsetY, fullBright, glowColor, doubleSize, motion);
		}

		/** How tall the part looks with the resource pack, in blocks. */
		public float height() {
			return doubleSize ? scale * 2.0f : scale;
		}
	}

	/** How a part moves on top of the mob. */
	public enum Motion {
		/** Stays put: for creatures whose mob already hops or walks. */
		NONE,
		/** Bobs gently up and down: for floating creatures. */
		FLOAT,
		/** Floats and pulses like a heartbeat. */
		PULSE
	}

	/** Spawns a creature at {@code pos}, named in {@code language}. {@code summoner} is null for the console. */
	@FunctionalInterface
	public interface Summoner {
		Mob summon(ServerLevel level, BlockPos pos, String language, ServerPlayer summoner);
	}

	/** What a creature does on its own, every tick ({@code tick} is the server's tick count). */
	@FunctionalInterface
	public interface Behaviour {
		Behaviour NONE = (level, mob, tick) -> {
		};

		void tick(ServerLevel level, Mob mob, int tick);
	}

	/** A boss's health bar: its colour, and the colour of the creature's name on it. */
	public record BossBar(BossEvent.BossBarColor color, ChatFormatting nameColor) {
	}

	/**
	 * A kind of creature. Its id names its model, its entity tag ({@code worldevents.<id>}) and its name
	 * ({@code entity.worldevents.<id>}); {@code egg} is the vanilla spawn egg it is built on, which spawns it.
	 */
	public record Kind(String id, Item egg, Summoner summoner, Behaviour behaviour, BossBar bossBar) {
		public String tag() {
			return WorldEventsMod.MOD_ID + "." + id;
		}

		public String nameKey() {
			return "entity." + WorldEventsMod.MOD_ID + "." + id;
		}

		public boolean is(Entity entity) {
			return entity.entityTags().contains(tag());
		}

		public Kind acting(Behaviour behaviour) {
			return new Kind(id, egg, summoner, behaviour, bossBar);
		}

		public Kind boss(BossEvent.BossBarColor color, ChatFormatting nameColor) {
			return new Kind(id, egg, summoner, behaviour, new BossBar(color, nameColor));
		}
	}

	/** Parts by model id, so the animation can find a part's settings again after a restart. */
	private static final Map<String, Part> PARTS = new HashMap<>();
	private static final Map<String, Kind> KINDS = new LinkedHashMap<>();
	/** Every creature loaded right now, in any level. */
	private static final Set<Mob> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<Mob, LocalizedBossBar> BOSS_BARS = new WeakHashMap<>();
	/** Set while a creature is summoned, so it is marked as summoned before it enters the world. */
	private static boolean summoning;

	private Creature() {
	}

	public static Kind kind(String id, Item egg, Summoner summoner) {
		return new Kind(id, egg, summoner, Behaviour.NONE, null);
	}

	/** Makes the mod's kinds of creature known, in the order the creative tab shows their eggs. */
	public static void registerKinds(Kind... kinds) {
		for (Kind kind : kinds) {
			KINDS.put(kind.id(), kind);
		}
	}

	public static Kind kind(String id) {
		return KINDS.get(id);
	}

	public static Collection<Kind> kinds() {
		return Collections.unmodifiableCollection(KINDS.values());
	}

	/** The kind of creature {@code entity} is, or {@code null} if it is not one of the mod's creatures. */
	public static Kind kindOf(Entity entity) {
		for (Kind kind : KINDS.values()) {
			if (kind.is(entity)) {
				return kind;
			}
		}
		return null;
	}

	/** Makes a part known; creatures define their parts once, as constants. */
	public static Part register(Part part) {
		PARTS.put(part.modelId(), part);
		return part;
	}

	/**
	 * Summons a creature outside of any event, for eggs and {@code /worldevents summon}. Its event will neither
	 * track it nor take it away: it stays until defeated.
	 */
	public static Mob summon(Kind kind, ServerLevel level, BlockPos pos, String language, ServerPlayer summoner) {
		summoning = true;
		try {
			return kind.summoner().summon(level, pos, language, summoner);
		} finally {
			summoning = false;
		}
	}

	/**
	 * Tags a new creature with its kind before it is added to the level, keeps it from despawning, and marks it as
	 * summoned if it is: the load callbacks already see the tags.
	 */
	public static void mark(Mob mob, Kind kind) {
		mob.setPersistenceRequired();
		mob.addTag(kind.tag());
		if (summoning) {
			mob.addTag(SUMMONED_TAG);
		}
	}

	public static boolean isSummoned(Entity entity) {
		return entity.entityTags().contains(SUMMONED_TAG);
	}

	/** Whether {@code mob} acts now, once every {@code seconds}. Creatures of a kind take turns. */
	public static boolean everySeconds(Entity mob, int tick, int seconds) {
		return tick % 20 == 0 && Math.floorMod(tick / 20 + mob.getId(), seconds) == 0;
	}

	/**
	 * Hides {@code mob} and puts the parts on it. Call after the mob has its final size (scale attribute) and has
	 * been added to the level.
	 */
	public static void dress(Mob mob, Part... parts) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return;
		}
		mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		for (Part part : parts) {
			Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.EVENT);
			if (display == null) {
				continue;
			}
			display.setItemStack(EventItems.cosmetic(part.base(), part.modelId()));
			display.setItemTransform(ItemDisplayContext.NONE);
			display.setPosRotInterpolationDuration(2);
			display.setTransformationInterpolationDuration(ANIMATION_TICKS);
			display.setViewRange(2.0f);
			display.setShadowRadius(0.4f * part.height());
			display.setShadowStrength(0.7f);
			if (part.fullBright()) {
				display.setBrightnessOverride(Brightness.FULL_BRIGHT);
			}
			if (part.glowColor() >= 0) {
				display.setGlowingTag(true);
				display.setGlowColorOverride(part.glowColor());
			}
			display.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0.0f);
			level.addFreshEntity(display);
			display.startRiding(mob, true, true);
			// Tagged only once it rides the mob: the load callback removes parts without a creature.
			display.addTag(PART_TAG);
			setPose(display, mob, part, 0.0f, 1.0f);
		}
		if (mob.getCustomName() != null) {
			addNameLabel(level, mob, parts);
		}
		LOADED.add(mob);
	}

	/**
	 * The creature's name, floating above its model and always facing the viewer. A vanilla name tag would not
	 * show: the mob is invisible and carries passengers.
	 */
	private static void addNameLabel(ServerLevel level, Mob mob, Part... parts) {
		Display.TextDisplay label = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.EVENT);
		if (label == null) {
			return;
		}
		float top = 0.0f;
		for (Part part : parts) {
			top = Math.max(top, part.offsetY() + part.height() / 2.0f);
		}
		label.setText(mob.getCustomName());
		label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		label.setViewRange(0.5f);
		label.snapTo(mob.getX(), mob.getY(), mob.getZ(), 0.0f, 0.0f);
		level.addFreshEntity(label);
		label.startRiding(mob, true, true);
		label.addTag(PART_TAG);
		float attach = (float) (mob.getPassengerRidingPosition(label).y - mob.getY());
		float y = mob.getBbHeight() / 2.0f + top + 0.25f - attach;
		label.setTransformation(new Transformation(new Vector3f(0.0f, y, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
	}

	/** Every loaded creature: turns and animates its parts, runs its abilities and keeps its boss bar up to date. */
	private static void tickAll(MinecraftServer server) {
		int tick = server.getTickCount();
		for (Mob mob : new ArrayList<>(LOADED)) {
			if (mob.isRemoved()) {
				LOADED.remove(mob);
				clearBossBar(mob);
				continue;
			}
			Kind kind = kindOf(mob);
			if (kind == null || !mob.isAlive() || !(mob.level() instanceof ServerLevel level) || !level.tickRateManager().runsNormally()) {
				continue;
			}
			animate(mob, level.getGameTime());
			kind.behaviour().tick(level, mob, tick);
			if (kind.bossBar() != null && tick % 20 == 0) {
				updateBossBar(mob, kind);
			}
		}
	}

	/** Keeps the parts turned like the mob, and animates them every few ticks. */
	private static void animate(Mob mob, long gameTime) {
		boolean animate = gameTime % ANIMATION_TICKS == 0;
		float phase = (gameTime + mob.getId() * 7L) * 0.08f;
		for (Entity passenger : mob.getPassengers()) {
			if (!(passenger instanceof Display.ItemDisplay display) || !display.entityTags().contains(PART_TAG)) {
				continue;
			}
			if (Math.abs(Mth.wrapDegrees(display.getYRot() - mob.getYRot())) > 1.0f) {
				display.setYRot(mob.getYRot());
			}
			Part part = animate ? partOf(display) : null;
			if (part != null && part.motion() != Motion.NONE) {
				float bob = Mth.sin(phase) * 0.12f;
				float pulse = part.motion() == Motion.PULSE ? 1.0f + Mth.sin(phase * 2.0f) * 0.05f : 1.0f;
				setPose(display, mob, part, bob, pulse);
			}
		}
	}

	private static void updateBossBar(Mob mob, Kind kind) {
		LocalizedBossBar bar = BOSS_BARS.computeIfAbsent(mob,
				key -> new LocalizedBossBar(kind.bossBar().color(), BossEvent.BossBarOverlay.NOTCHED_10, false));
		bar.update(MobSpawning.playersNear(mob, BOSS_BAR_DISTANCE),
				language -> Component.literal(Lang.text(language, kind.nameKey())).withStyle(kind.bossBar().nameColor()),
				mob.getHealth() / mob.getMaxHealth());
	}

	private static void clearBossBar(Entity mob) {
		LocalizedBossBar bar = BOSS_BARS.remove(mob);
		if (bar != null) {
			bar.clear();
		}
	}

	/** The boss bar title {@code player} sees for {@code boss}, or {@code null}. */
	public static Component bossBarTitle(Mob boss, ServerPlayer player) {
		LocalizedBossBar bar = BOSS_BARS.get(boss);
		return bar != null ? bar.titleFor(player) : null;
	}

	/** Removes the creature and its parts at once. */
	public static void remove(Mob mob) {
		discardParts(mob);
		clearBossBar(mob);
		mob.discard();
	}

	public static void discardParts(Entity mob) {
		for (Entity passenger : new ArrayList<>(mob.getPassengers())) {
			if (passenger.entityTags().contains(PART_TAG)) {
				passenger.discard();
			}
		}
	}

	public static List<Display.ItemDisplay> parts(Entity mob) {
		List<Display.ItemDisplay> parts = new ArrayList<>();
		for (Entity passenger : mob.getPassengers()) {
			if (passenger instanceof Display.ItemDisplay display && display.entityTags().contains(PART_TAG)) {
				parts.add(display);
			}
		}
		return parts;
	}

	public static void registerHooks() {
		// The parts go with the creature: no floating model left behind.
		Hooks.AFTER_DEATH.register((entity, source) -> {
			clearBossBar(entity);
			if (!parts(entity).isEmpty() && entity.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
						20, entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.05);
				discardParts(entity);
			}
		});
		// A hit shows on the model too, since the mob itself is invisible.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && !parts(entity).isEmpty() && entity.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
						4, entity.getBbWidth() / 3, entity.getBbHeight() / 3, entity.getBbWidth() / 3, 0.1);
			}
		});
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			// A part whose creature is gone (an old save, a crash) is removed.
			if (entity instanceof Display.ItemDisplay && entity.entityTags().contains(PART_TAG) && entity.getVehicle() == null) {
				DeferredDiscard.discard(entity);
			} else if (entity instanceof Mob mob && kindOf(mob) != null) {
				LOADED.add(mob);
			}
		});
		Hooks.END_SERVER_TICK.register(Creature::tickAll);
		// In singleplayer one game can host several worlds, one after another.
		Hooks.SERVER_STOPPED.register(server -> {
			LOADED.clear();
			BOSS_BARS.values().forEach(LocalizedBossBar::clear);
			BOSS_BARS.clear();
		});
	}

	private static Part partOf(Display.ItemDisplay display) {
		CustomModelData data = display.getItemStack().get(DataComponents.CUSTOM_MODEL_DATA);
		if (data == null || data.strings().isEmpty()) {
			return null;
		}
		String id = data.strings().get(0);
		return PARTS.get(id.substring(id.indexOf(':') + 1));
	}

	private static void setPose(Display.ItemDisplay display, Mob mob, Part part, float bob, float pulse) {
		// The display sits on the mob's passenger attachment point; move it to the middle of the hitbox.
		float attach = (float) (mob.getPassengerRidingPosition(display).y - mob.getY());
		float y = mob.getBbHeight() / 2.0f + part.offsetY() - attach + bob;
		float scale = part.scale() * pulse;
		display.setTransformation(new Transformation(
				new Vector3f(0.0f, y, 0.0f), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
		display.setTransformationInterpolationDelay(0);
	}
}
