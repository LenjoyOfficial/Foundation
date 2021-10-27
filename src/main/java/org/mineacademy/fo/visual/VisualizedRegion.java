package org.mineacademy.fo.visual;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompParticle;

import lombok.Setter;

/**
 * A simply way to visualize two locations in the world
 */
public final class VisualizedRegion extends Region {

	/**
	 * A list of players who can see the particles
	 */
	private final List<Player> viewers = new ArrayList<>();

	/**
	 * Should we visualize the walls or just the edges?
	 */
	@Setter
	private boolean fillWalls;

	/**
	 * The task responsible for sending particles
	 */
	private BukkitTask task;

	/**
	 * The particle that is being sent out
	 */
	@Setter
	private CompParticle particle = CompParticle.VILLAGER_HAPPY;

	/**
	 * Create a new visualizable region
	 *
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final Location primary, final Location secondary) {
		this(primary, secondary, true);
	}

	/**
	 * Create a new visualizable region
	 *
	 * @param primary
	 * @param secondary
	 * @param fillWalls if set to false, only the region's edges will be visualized
	 */
	public VisualizedRegion(final Location primary, final Location secondary, final boolean fillWalls) {
		super(primary, secondary);

		this.fillWalls = fillWalls;
	}

	/**
	 * Create a visualizable region
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final String name, final Location primary, final Location secondary) {
		this(name, primary, secondary, true);
	}

	/**
	 * Create a visualizable region
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 * @param fillWalls if set to false, only the region's edges will be visualized
	 */
	public VisualizedRegion(final String name, final Location primary, final Location secondary, final boolean fillWalls) {
		super(name, primary, secondary);

		this.fillWalls = fillWalls;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Rendering
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Shows the region to the given player for the given duration,
	 * the hides it
	 *
	 * @param player
	 * @param durationTicks
	 */
	public void showParticles(final Player player, final int durationTicks) {
		showParticles(player);

		Common.runLater(durationTicks, () -> {
			if (canSeeParticles(player))
				hideParticles(player);
		});
	}

	/**
	 * Shows the region to the given player
	 *
	 * @param player
	 */
	public void showParticles(final Player player) {
		Valid.checkBoolean(!canSeeParticles(player), "Player " + player.getName() + " already sees region " + this);
		Valid.checkBoolean(isWhole(), "Cannot show particles of an incomplete region " + this);

		viewers.add(player);

		if (task == null)
			startVisualizing();
	}

	/**
	 * Hides the region from the given player
	 *
	 * @param player
	 */
	public void hideParticles(final Player player) {
		Valid.checkBoolean(canSeeParticles(player), "Player " + player.getName() + " is not seeing region " + this);

		viewers.remove(player);

		if (viewers.isEmpty() && task != null)
			stopVisualizing();
	}

	/**
	 * Return true if the given player can see the region particles
	 *
	 * @param player
	 *
	 * @return
	 */
	public boolean canSeeParticles(final Player player) {
		return viewers.contains(player);
	}

	/*
	 * Starts visualizing this region if it is whole
	 */
	private void startVisualizing() {
		Valid.checkBoolean(task == null, "Already visualizing region " + this + "!");
		Valid.checkBoolean(isWhole(), "Cannot visualize incomplete region " + this + "!");

		task = Common.runTimer(23, new BukkitRunnable() {
			@Override
			public void run() {
				if (viewers.isEmpty()) {
					stopVisualizing();

					return;
				}

				final Set<Location> blocks = BlockUtil.getBoundingBox(getPrimary(), getSecondary(), fillWalls);

				for (final Location location : blocks)
					for (final Player viewer : viewers) {
						final Location viewerLocation = viewer.getLocation();

						if (viewerLocation.getWorld().equals(location.getWorld()) && viewerLocation.distance(location) < 100)
							particle.spawn(viewer, location);
					}

			}
		});
	}

	/*
	 * Stops the region from being visualized
	 */
	private void stopVisualizing() {
		Valid.checkNotNull(task, "Region " + this + " not visualized");

		task.cancel();
		task = null;

		viewers.clear();
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = super.serialize();

		map.put("Visualize_Walls", fillWalls);

		return map;
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 *
	 * @return
	 */
	public static VisualizedRegion deserialize(final SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = SerializeUtil.deserializeLocationD(map.getString("Primary"));
		final Location sec = SerializeUtil.deserializeLocationD(map.getString("Secondary"));
		final boolean fillWalls = map.getBoolean("Visualize_Walls", true);

		return new VisualizedRegion(name, prim, sec, fillWalls);
	}
}
