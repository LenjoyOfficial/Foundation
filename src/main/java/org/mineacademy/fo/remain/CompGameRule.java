package org.mineacademy.fo.remain;

import javax.annotation.Nullable;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public enum CompGameRule {
	/**
	 * Whether command blocks should notify admins when they perform commands.
	 * Controls if command block executions are broadcast to ops in chat.
	 */
	COMMAND_BLOCK_OUTPUT("command_block_output", "commandBlockOutput"),

	/**
	 * Whether the daylight cycle and moon phases progress.
	 * Controls if time advances with day/night cycle.
	 */
	ADVANCE_TIME("advance_time", "doDaylightCycle"),

	/**
	 * Whether non-mob entities (like boats, minecarts, items from item frames) drop items when broken.
	 */
	ENTITY_DROPS("entity_drops", "doEntityDrops"),

	/**
	 * Whether mobs drop their loot (items and experience) when killed.
	 */
	MOB_DROPS("mob_drops", "doMobLoot"),

	/**
	 * Whether mobs spawn naturally.
	 * Does not affect spawners or structure spawns.
	 */
	SPAWN_MOBS("spawn_mobs", "doMobSpawning"),

	/**
	 * Whether blocks drop items when broken (not by explosions or creative mode).
	 */
	BLOCK_DROPS("block_drops", "doTileDrops"),

	/**
	 * Whether players keep their inventory and experience after death.
	 */
	KEEP_INVENTORY("keep_inventory", "keepInventory"),

	/**
	 * Whether admin commands (like /give, /tp) are logged to the server log.
	 */
	LOG_ADMIN_COMMANDS("log_admin_commands", "logAdminCommands"),

	/**
	 * Whether mobs can grief the world (e.g., creepers explode blocks, endermen pick up blocks).
	 */
	MOB_GRIEFING("mob_griefing", "mobGriefing"),

	/**
	 * Whether players naturally regenerate health when hunger is full.
	 */
	NATURAL_HEALTH_REGENERATION("natural_health_regeneration", "naturalRegeneration"),

	/**
	 * Whether reduced debug screen info is shown (hides coordinates, etc.).
	 */
	REDUCED_DEBUG_INFO("reduced_debug_info", "reducedDebugInfo"),

	/**
	 * Whether command feedback (success/failure messages) is sent to the player.
	 */
	SEND_COMMAND_FEEDBACK("send_command_feedback", "sendCommandFeedback"),

	/**
	 * Whether death messages are shown in chat.
	 */
	SHOW_DEATH_MESSAGES("show_death_messages", "showDeathMessages"),

	/**
	 * Controls the rate of random block ticks (crop growth, leaf decay, etc.).
	 * Default is {@code 3}; higher values make processes faster.
	 */
	RANDOM_TICK_SPEED("random_tick_speed", "randomTickSpeed"),

	/**
	 * Whether spectators generate new chunks when moving around.
	 */
	SPECTATORS_GENERATE_CHUNKS("spectators_generate_chunks", "spectatorsGenerateChunks"),

	/**
	 * Whether the server checks for elytra flight cheating (speed/movement validation).
	 * Set to {@code false} to disable the check.
	 */
	ELYTRA_MOVEMENT_CHECK("elytra_movement_check", "disableElytraMovementCheck"),

	/**
	 * The radius around world spawn where players respawn if no bed.
	 */
	RESPAWN_RADIUS("respawn_radius", "spawnRadius"),

	/**
	 * Whether weather cycles (rain, thunder) progress naturally.
	 */
	ADVANCE_WEATHER("advance_weather", "doWeatherCycle"),

	/**
	 * Maximum number of entities that can be crammed in one block before taking suffocation damage.
	 */
	MAX_ENTITY_CRAMMING("max_entity_cramming", "maxEntityCramming"),

	/**
	 * Whether crafting recipes require the exact arrangement as discovered in the recipe book.
	 */
	LIMITED_CRAFTING("limited_crafting", "doLimitedCrafting"),

	/**
	 * Whether advancement completion messages are announced in chat.
	 */
	SHOW_ADVANCEMENT_MESSAGES("show_advancement_messages", "announceAdvancements"),

	/**
	 * Maximum length of a command chain (function or command block chain).
	 */
	MAX_COMMAND_SEQUENCE_LENGTH("max_command_sequence_length", "maxCommandChainLength"),

	/**
	 * The function that runs every game tick.
	 */
	@Deprecated
	GAME_LOOP_FUNCTION("gameLoopFunction"),

	/**
	 * Whether raids can occur (inverted: true disables raids).
	 */
	RAIDS("raids", "disableRaids"),

	/**
	 * Whether phantoms spawn from insomnia.
	 */
	SPAWN_PHANTOMS("spawn_phantoms", "doInsomnia"),

	/**
	 * Whether players respawn immediately without the death screen.
	 */
	IMMEDIATE_RESPAWN("immediate_respawn", "doImmediateRespawn"),

	/**
	 * Whether players take drowning damage.
	 */
	DROWNING_DAMAGE("drowning_damage", "drowningDamage"),

	/**
	 * Whether players take fall damage.
	 */
	FALL_DAMAGE("fall_damage", "fallDamage"),

	/**
	 * Whether players take fire/lava damage.
	 */
	FIRE_DAMAGE("fire_damage", "fireDamage"),

	/**
	 * Whether pillager patrols spawn naturally.
	 */
	SPAWN_PATROLS("spawn_patrols", "doPatrolSpawning"),

	/**
	 * Whether wandering traders spawn naturally.
	 */
	SPAWN_WANDERING_TRADERS("spawn_wandering_traders", "doTraderSpawning"),

	/**
	 * Whether angered neutral mobs forgive players who die and respawn.
	 */
	FORGIVE_DEAD_PLAYERS("forgive_dead_players", "forgiveDeadPlayers"),

	/**
	 * Whether angered neutral mobs attack any nearby player (universal anger).
	 */
	UNIVERSAL_ANGER("universal_anger", "universalAnger"),

	/**
	 * Whether players take freeze damage in powder snow.
	 */
	FREEZE_DAMAGE("freeze_damage", "freezeDamage"),

	/**
	 * Percentage of players that must sleep to skip the night.
	 */
	PLAYERS_SLEEPING_PERCENTAGE("players_sleeping_percentage", "playersSleepingPercentage"),

	/**
	 * Whether wardens spawn naturally in ancient cities.
	 */
	SPAWN_WARDENS("spawn_wardens", "doWardenSpawning"),

	/**
	 * Whether block explosion drops have decay (chance to not drop based on distance).
	 */
	BLOCK_EXPLOSION_DROP_DECAY("block_explosion_drop_decay", "blockExplosionDropDecay"),

	/**
	 * Whether mob (creeper/ghast) explosion drops have decay.
	 */
	MOB_EXPLOSION_DROP_DECAY("mob_explosion_drop_decay", "mobExplosionDropDecay"),

	/**
	 * Whether TNT explosion drops have decay.
	 */
	TNT_EXPLOSION_DROP_DECAY("tnt_explosion_drop_decay", "tntExplosionDropDecay"),

	/**
	 * Whether water source blocks convert to flowing water under certain conditions.
	 */
	WATER_SOURCE_CONVERSION("water_source_conversion", "waterSourceConversion"),

	/**
	 * Whether lava source blocks convert to flowing lava under certain conditions.
	 */
	LAVA_SOURCE_CONVERSION("lava_source_conversion", "lavaSourceConversion"),

	/**
	 * Whether certain sound events are heard globally by all players.
	 */
	GLOBAL_SOUND_EVENTS("global_sound_events", "globalSoundEvents"),

	/**
	 * Maximum height snow can accumulate in a single block.
	 */
	MAX_SNOW_ACCUMULATION_HEIGHT("max_snow_accumulation_height", "snowAccumulationHeight"),

	/**
	 * Whether vines spread to adjacent blocks.
	 */
	SPREAD_VINES("spread_vines", "doVinesSpread"),

	/**
	 * Maximum number of block modifications allowed in a single command (anti-grief).
	 */
	MAX_BLOCK_MODIFICATIONS("max_block_modifications", "commandModificationBlockLimit"),

	/**
	 * Whether ender pearls disappear when the player dies.
	 */
	ENDER_PEARLS_VANISH_ON_DEATH("ender_pearls_vanish_on_death", "enderPearlsVanishOnDeath"),

	/**
	 * Whether projectiles (arrows, tridents, etc.) can break certain blocks.
	 */
	PROJECTILES_CAN_BREAK_BLOCKS("projectiles_can_break_blocks", "projectilesCanBreakBlocks"),

	/**
	 * Maximum number of command forks allowed.
	 */
	MAX_COMMAND_FORKS("max_command_forks", "maxCommandForkCount"),

	/**
	 * Default delay (in ticks) for players entering Nether portals in survival/adventure.
	 */
	PLAYERS_NETHER_PORTAL_DEFAULT_DELAY("players_nether_portal_default_delay", "playersNetherPortalDefaultDelay"),

	/**
	 * Delay (in ticks) for players in creative mode entering Nether portals.
	 */
	PLAYERS_NETHER_PORTAL_CREATIVE_DELAY("players_nether_portal_creative_delay", "playersNetherPortalCreativeDelay"),

	/**
	 * Radius of spawn chunks that are always loaded.
	 */
	@Deprecated
	SPAWN_CHUNK_RADIUS("spawnChunkRadius"),

	/**
	 * Whether the server performs player movement validation checks.
	 */
	PLAYER_MOVEMENT_CHECK("player_movement_check", "disablePlayerMovementCheck"),

	/**
	 * Maximum speed of minecarts.
	 */
	MAX_MINECART_SPEED("max_minecart_speed", "minecartMaxSpeed"),

	/**
	 * Whether primed TNT explodes.
	 */
	TNT_EXPLODES("tnt_explodes", "tntExplodes"),

	/**
	 * Whether fire continues to tick/update when far from players.
	 */
	@Deprecated
	ALLOW_FIRE_TICKS_AWAY_FROM_PLAYER("allowFireTicksAwayFromPlayer"),

	/**
	 * Whether the locator bar (on maps?) is shown.
	 */
	LOCATOR_BAR("locator_bar", "locatorBar"),

	/**
	 * Whether PvP (player vs player damage) is enabled.
	 */
	PVP("pvp"),

	/**
	 * Whether players can enter the Nether using portals.
	 */
	ALLOW_ENTERING_NETHER_USING_PORTALS("allow_entering_nether_using_portals", "allowEnteringNetherUsingPortals"),

	/**
	 * Whether hostile monsters spawn naturally.
	 */
	SPAWN_MONSTERS("spawn_monsters", "spawnMonsters"),

	/**
	 * Whether command blocks are enabled and can execute commands.
	 */
	COMMAND_BLOCKS_WORK("command_blocks_work", "commandBlocksEnabled"),

	/**
	 * Whether monster spawners work and spawn mobs.
	 */
	SPAWNER_BLOCKS_WORK("spawner_blocks_work", "spawnerBlocksEnabled"),

	/**
	 * Radius around players where fire spreads faster or is allowed.
	 */
	FIRE_SPREAD_RADIUS_AROUND_PLAYER("fire_spread_radius_around_player");

	@NonNull
	private final String key;

	@Nullable
	private String legacyKey;

	/**
	 * Finds the Bukkit {@link GameRule} for this enum value on the current Minecraft version,
	 * or {@code null} if not supported on this version (or prior to Minecraft 1.13).
	 *
	 * @return the matching {@link GameRule}, or {@code null} if unavailable
	 */
	@Nullable
	@SuppressWarnings("deprecation")
	public GameRule<?> getGameRule() {
		if (MinecraftVersion.olderThan(V.v1_13))
			return null;

		if (MinecraftVersion.newerThan(V.v1_21) || (MinecraftVersion.equals(V.v1_21) && MinecraftVersion.getSubversion() >= 11)) {
			final NamespacedKey namespacedKey = NamespacedKey.fromString(this.key);

			return namespacedKey != null ? Registry.GAME_RULE.get(namespacedKey) : null;
		}

		return GameRule.getByName(this.legacyKey != null ? this.legacyKey : this.key);
	}

	/**
	 * Attempts to find a {@link CompGameRule} matching the given modern key, legacy key, or enum name.
	 *
	 * @param key the game rule key or name
	 * @return the matching {@link CompGameRule}, or {@code null} if not found
	 */
	@Nullable
	public static CompGameRule fromKey(@NonNull String key) {
		if (key.startsWith("minecraft:"))
			key = key.substring("minecraft:".length());

		for (final CompGameRule rule : values())
			if (rule.key.equalsIgnoreCase(key) || (rule.legacyKey != null && rule.legacyKey.equalsIgnoreCase(key)) || rule.name().equalsIgnoreCase(key))
				return rule;

		return null;
	}
}
