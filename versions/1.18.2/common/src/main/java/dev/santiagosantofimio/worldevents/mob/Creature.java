package dev.santiagosantofimio.worldevents.mob;

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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;

/**
 * The mod's own creatures, made so vanilla clients can see them. An invisible vanilla mob does the moving, fighting
 * and colliding; invisible armor stands that follow it wear the creature's 3D model from the resource pack on their
 * heads. Without the pack, players see the base block floating where the creature is, with its name above it.
 *
 * <p>Each creature takes care of itself wherever it is: this class animates every loaded creature and runs its
 * abilities and boss bar, whether its event brought it or it was summoned with an egg or a command. Events only
 * decide when their creatures come and go.
 */
public final class Creature {
	public static final String PART_TAG = WorldEventsMod.MOD_ID + ".creature_part";
	/** Creatures summoned with an egg or a command: no event tracks them, and they stay until defeated. */
	public static final String SUMMONED_TAG = WorldEventsMod.MOD_ID + ".summoned";
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
			return entity.getTags().contains(tag());
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
	/** The parts each creature wears, and the armor stands that show them right now. */
	private static final Map<Mob, List<Part>> DRESSES = new WeakHashMap<>();
	private static final Map<Mob, List<ArmorStand>> PART_STANDS = new WeakHashMap<>();
	private static final Map<Mob, Float> LABEL_HEIGHT = new WeakHashMap<>();
	private static final Map<String, List<Part>> DRESS_BY_KIND = new HashMap<>();
	private static final String MODEL_TAG_PREFIX = WorldEventsMod.MOD_ID + ".model:";
	private static final String LABEL_TAG = WorldEventsMod.MOD_ID + ".label";
	/** Height of the middle of an armor stand's head above its feet: where a head item is drawn. */
	private static final double HEAD_CENTER = 1.6875;
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
		return entity.getTags().contains(SUMMONED_TAG);
	}

	/** Whether {@code mob} acts now, once every {@code seconds}. Creatures of a kind take turns. */
	public static boolean everySeconds(Entity mob, int tick, int seconds) {
		return tick % 20 == 0 && Math.floorMod(tick / 20 + mob.getId(), seconds) == 0;
	}

	/**
	 * Hides {@code mob} and gives it its parts. Call after the mob has been added to the level.
	 *
	 * <p>Minecraft 1.18 has no display entities, so each part is an invisible marker armor stand wearing the part's
	 * item on its head; the legacy resource pack scales the model in the item's head transform. The stands follow the
	 * mob every tick. They are never kept: a creature loaded from disk gets new ones, and stale ones are removed.
	 */
	public static void dress(Mob mob, Part... parts) {
		if (!(mob.level instanceof ServerLevel level)) {
			return;
		}
		mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
		mob.setCustomNameVisible(false);
		DRESSES.put(mob, List.of(parts));
		putOnParts(level, mob);
		LOADED.add(mob);
	}

	private static void putOnParts(ServerLevel level, Mob mob) {
		List<Part> dress = DRESSES.get(mob);
		if (dress == null) {
			return;
		}
		List<ArmorStand> stands = new ArrayList<>();
		float top = 0.0f;
		for (Part part : dress) {
			ArmorStand stand = marker(level, mob);
			if (stand == null) {
				continue;
			}
			stand.setItemSlot(EquipmentSlot.HEAD, EventItems.cosmetic(part.base(), part.modelId()));
			if (part.glowColor() >= 0) {
				stand.setGlowingTag(true);
			}
			stand.addTag(PART_TAG);
			stand.addTag(MODEL_TAG_PREFIX + part.modelId());
			level.addFreshEntity(stand);
			stands.add(stand);
			top = Math.max(top, part.offsetY() + part.height() / 2.0f);
		}
		if (mob.getCustomName() != null) {
			ArmorStand label = marker(level, mob);
			if (label != null) {
				label.setCustomName(mob.getCustomName());
				label.setCustomNameVisible(true);
				label.addTag(PART_TAG);
				label.addTag(LABEL_TAG);
				level.addFreshEntity(label);
				stands.add(label);
			}
		}
		PART_STANDS.put(mob, stands);
		LABEL_HEIGHT.put(mob, top + 0.25f);
		follow(mob, 0L);
	}

	/** An invisible armor stand that cannot be hit, does not fall and is never saved for long. */
	private static ArmorStand marker(ServerLevel level, Mob mob) {
		ArmorStand stand = EntityType.ARMOR_STAND.create(level);
		if (stand == null) {
			return null;
		}
		CompoundTag tag = stand.saveWithoutId(new CompoundTag());
		tag.putBoolean("Marker", true);
		tag.putBoolean("Invisible", true);
		tag.putBoolean("NoGravity", true);
		tag.putBoolean("Silent", true);
		tag.putBoolean("Invulnerable", true);
		tag.putInt("DisabledSlots", 0x1F1F1F);
		stand.load(tag);
		stand.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0.0f);
		return stand;
	}

	/** Every loaded creature: moves its parts along, runs its abilities and keeps its boss bar up to date. */
	private static void tickAll(MinecraftServer server) {
		int tick = server.getTickCount();
		for (Mob mob : new ArrayList<>(LOADED)) {
			if (mob.isRemoved()) {
				LOADED.remove(mob);
				discardParts(mob);
				clearBossBar(mob);
				continue;
			}
			Kind kind = kindOf(mob);
			if (kind == null || !mob.isAlive() || !(mob.level instanceof ServerLevel level)) {
				continue;
			}
			if (!PART_STANDS.containsKey(mob) && DRESSES.containsKey(mob)) {
				putOnParts(level, mob);
			}
			follow(mob, level.getGameTime());
			kind.behaviour().tick(level, mob, tick);
			if (kind.bossBar() != null && tick % 20 == 0) {
				updateBossBar(mob, kind);
			}
		}
	}

	/**
	 * Puts every part where it belongs on the mob, turned like it. The part's centre is where the stand's head is:
	 * {@link #HEAD_CENTER} above its feet. Floating parts bob up and down.
	 */
	private static void follow(Mob mob, long gameTime) {
		List<ArmorStand> stands = PART_STANDS.get(mob);
		List<Part> dress = DRESSES.get(mob);
		if (stands == null || dress == null) {
			return;
		}
		float phase = (gameTime + mob.getId() * 7L) * 0.08f;
		double middle = mob.getY() + mob.getBbHeight() / 2.0;
		for (ArmorStand stand : stands) {
			double y;
			if (stand.getTags().contains(LABEL_TAG)) {
				y = middle + LABEL_HEIGHT.getOrDefault(mob, 0.5f);
			} else {
				Part part = partOf(stand);
				if (part == null) {
					continue;
				}
				float bob = part.motion() != Motion.NONE ? Mth.sin(phase) * 0.12f : 0.0f;
				y = middle + part.offsetY() + bob - HEAD_CENTER;
			}
			stand.moveTo(mob.getX(), y, mob.getZ(), mob.getYRot(), 0.0f);
			stand.setYHeadRot(mob.getYRot());
			stand.setYBodyRot(mob.getYRot());
		}
	}

	private static void updateBossBar(Mob mob, Kind kind) {
		LocalizedBossBar bar = BOSS_BARS.computeIfAbsent(mob,
				key -> new LocalizedBossBar(kind.bossBar().color(), BossEvent.BossBarOverlay.NOTCHED_10, false));
		bar.update(MobSpawning.playersNear(mob, BOSS_BAR_DISTANCE),
				language -> new TextComponent(Lang.text(language, kind.nameKey())).withStyle(kind.bossBar().nameColor()),
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
		List<ArmorStand> stands = PART_STANDS.remove(mob);
		if (stands != null) {
			stands.forEach(Entity::discard);
		}
	}

	/** The armor stands that show the creature, if it is one and has them. */
	public static List<ArmorStand> parts(Entity mob) {
		List<ArmorStand> stands = PART_STANDS.get(mob);
		return stands != null ? stands : List.of();
	}

	public static void registerHooks() {
		// The parts go with the creature: no floating model left behind.
		Hooks.AFTER_DEATH.register((entity, source) -> {
			clearBossBar(entity);
			if (!parts(entity).isEmpty() && entity.level instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
						20, entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.05);
				discardParts(entity);
			}
		});
		// A hit shows on the model too, since the mob itself is invisible.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && !parts(entity).isEmpty() && entity.level instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
						4, entity.getBbWidth() / 3, entity.getBbHeight() / 3, entity.getBbWidth() / 3, 0.1);
			}
		});
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			// A part whose creature is gone (an old save, a crash) is removed.
			if (entity instanceof ArmorStand && entity.getTags().contains(PART_TAG)) {
				// Parts are made anew for each creature that loads; saved ones are stale.
				if (!isTracked(entity)) {
					DeferredDiscard.discard(entity);
				}
			} else if (entity instanceof Mob mob && kindOf(mob) != null) {
				LOADED.add(mob);
				// Loaded from disk: its parts come back on its first tick.
				DRESSES.computeIfAbsent(mob, Creature::dressOf);
			}
		});
		Hooks.END_SERVER_TICK.register(Creature::tickAll);
		// In singleplayer one game can host several worlds, one after another.
		Hooks.SERVER_STOPPED.register(server -> {
			LOADED.clear();
			DRESSES.clear();
			PART_STANDS.clear();
			BOSS_BARS.values().forEach(LocalizedBossBar::clear);
			BOSS_BARS.clear();
		});
	}

	private static Part partOf(ArmorStand stand) {
		for (String tag : stand.getTags()) {
			if (tag.startsWith(MODEL_TAG_PREFIX)) {
				return PARTS.get(tag.substring(MODEL_TAG_PREFIX.length()));
			}
		}
		return null;
	}

	private static boolean isTracked(Entity stand) {
		for (List<ArmorStand> stands : PART_STANDS.values()) {
			if (stands.contains(stand)) {
				return true;
			}
		}
		return false;
	}

	/** The parts a creature of this kind wears, for one loaded from disk. */
	private static List<Part> dressOf(Mob mob) {
		Kind kind = kindOf(mob);
		return kind != null ? DRESS_BY_KIND.getOrDefault(kind.id(), List.of()) : List.of();
	}

	/** Makes the parts of a kind known, so a creature loaded from disk can wear them again. */
	public static void registerDress(Kind kind, Part... parts) {
		DRESS_BY_KIND.put(kind.id(), List.of(parts));
	}
}
