package org.mineacademy.fo.remain;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.internal.ParticleInternals;

import lombok.Getter;

import java.util.Arrays;

/**
 * Wrapper for {@link Particle}
 */
public enum CompParticle {

	EXPLOSION_NORMAL,
	EXPLOSION_LARGE,
	EXPLOSION_HUGE,
	FIREWORKS_SPARK,
	WATER_BUBBLE,
	WATER_SPLASH,
	WATER_WAKE,
	SUSPENDED,
	SUSPENDED_DEPTH,
	CRIT,
	CRIT_MAGIC,
	SMOKE_NORMAL,
	SMOKE_LARGE,
	SPELL,
	SPELL_INSTANT,
	SPELL_MOB,
	SPELL_MOB_AMBIENT,
	SPELL_WITCH,
	DRIP_WATER,
	DRIP_LAVA,
	VILLAGER_ANGRY,
	VILLAGER_HAPPY,
	TOWN_AURA,
	NOTE,
	PORTAL,
	ENCHANTMENT_TABLE,
	FLAME,
	LAVA,
	FOOTSTEP,
	CLOUD,
	REDSTONE,
	SNOWBALL,
	SNOW_SHOVEL,
	SLIME,
	HEART,
	BARRIER,
	ITEM_CRACK,
	BLOCK_CRACK,
	BLOCK_DUST,
	WATER_DROP,
	ITEM_TAKE,
	MOB_APPEARANCE,
	DRAGON_BREATH,
	END_ROD,
	DAMAGE_INDICATOR,
	SWEEP_ATTACK,
	FALLING_DUST,
	TOTEM,
	SPIT;

	// Hardcoded for best performance
	private static final boolean hasNewMaterials = MinecraftVersion.atLeast(V.v1_13);

	/**
	 * Internal use
	 *
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	@Getter
	private MaterialData data;

	/**
	 * Internal use
	 *
	 * @param data
	 * @return
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	public CompParticle setWoolData(final int data) {
		this.data = new MaterialData(CompMaterial.WHITE_WOOL.getMaterial(), (byte) data);

		return this;
	}

	/**
	 * Internal use
	 *
	 * @param material
	 * @param data
	 * @return
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	public CompParticle setData(final Material material, final int data) {
		this.data = new MaterialData(material, (byte) data);

		return this;
	}

	/**
	 * Spawns the particle at the given location
	 *
	 * @param location the location where to spawn
	 */
	public final void spawn(final Location location) {
		spawn(location, null);
	}

	/**
	 * Spawns the particle at the given location with extra data
	 *
	 * @param location
	 * @param extra
	 */
	public final void spawn(final Location location, final Double extra) {
		spawn(location, 1, 0, 0, 0, extra);
	}

	/**
	 * Spawns the particle at the given location with the count and offsets
	 *
	 * @param location
	 * @param count
	 * @param offsetX
	 * @param offsetY
	 * @param offsetZ
	 * @param extra
	 */
	public final void spawn(final Location location, final int count, final double offsetX, final double offsetY, final double offsetZ, final Double extra) {
		for (final Player player : location.getWorld().getPlayers())
			spawnFor(player, location, count, offsetX, offsetY, offsetZ, extra);
	}

	/**
	 * Spawns the particle at the given location with extra material data
	 *
	 * @param location
	 * @param data
	 */
	public final void spawnWithData(final Location location, final CompMaterial data) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null)
				if (hasNewMaterials)
					location.getWorld().spawnParticle(particle, location, 1, data.getMaterial().createBlockData());
				else
					location.getWorld().spawnParticle(particle, location, 1, data.getMaterial().getNewData((byte) data.getData()));

		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.sendColor(location, DyeColor.getByWoolData((byte) data.getData()).getColor());
		}
	}

	/**
	 * Spawns the particle at the given location with the given color
	 *
	 * @param location
	 * @param color
	 */
	public final void spawnWithColor(final Location location, final Color color) {
		if (!Arrays.asList("REDSTONE", "SPELL_MOB", "SPELL_MOB_AMBIENT").contains(toString()))
			throw new FoException("Particle must be REDSTONE, SPELL_MOB or SPELL_MOB_AMBIENT! Got " + toString());

		final double red = (double) color.getRed() / 255;
		final double green = (double) color.getGreen() / 255;
		final double blue = (double) color.getBlue() / 255;

		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null)
				location.getWorld().spawnParticle(particle, location, 0, red, green, blue);

		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.sendColor(location, color);
		}
	}

	/**
	 * Spawns the particle at the given location only visible for the given player
	 *
	 * @param player
	 * @param location
	 */
	public final void spawnFor(final Player player, final Location location) {
		spawnFor(player, location, 1, 0, 0, 0, 0D);
	}

	/**
	 * Spawns the particle at the given location only visible for the given player
	 * adding additional extra data
	 *
	 * @param player
	 * @param location
	 * @param count
	 * @param offsetX
	 * @param offsetY
	 * @param offsetZ
	 * @param extra
	 */
	public final void spawnFor(final Player player, final Location location, final int count, final double offsetX, final double offsetY, final double offsetZ, final Double extra) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null) {
				if (hasNewMaterials)
					if (particle.getDataType() == org.bukkit.block.data.BlockData.class) {
						org.bukkit.block.data.BlockData opt = org.bukkit.Material.END_ROD.createBlockData(); // GRAVEL

						if (data != null)
							opt = Bukkit.getUnsafe().fromLegacy(data.getItemType(), data.getData());

						player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra != null ? extra : 0D, opt);
						return;
					}

				player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra != null ? extra : 0D);
			}
		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.send(player, location, (float) offsetX, (float) offsetY, (float) offsetZ, extra != null ? extra.floatValue() : 0F, count);
		}
	}
}