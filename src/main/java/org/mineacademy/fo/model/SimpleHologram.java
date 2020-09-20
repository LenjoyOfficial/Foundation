package org.mineacademy.fo.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
public abstract class SimpleHologram {

	private static final Constructor<?> SPAWN_ENTITY_LIVING_PACKET;
	private static final Constructor<?> DESTROY_ENTITIES_PACKET;

	static {
		final Class<?> nmsLivingEntity = ReflectionUtil.getNMSClass("EntityLiving", "net.minecraft.world.entity.EntityLiving");

		SPAWN_ENTITY_LIVING_PACKET = ReflectionUtil.getConstructor(
				ReflectionUtil.getNMSClass("PacketPlayOutSpawnEntityLiving",
						"net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity" + (MinecraftVersion.atLeast(MinecraftVersion.V.v1_19) ? "" : "Living")),
				nmsLivingEntity);

		DESTROY_ENTITIES_PACKET = ReflectionUtil.getConstructor(
				ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy", "net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy"),
				int[].class);
	}

	/**
	 * The distance between each line of lore for this item
	 */
	@Getter
	@Setter
	private static double loreLineHeight = 0.3D;

	/**
	 * A registry of created animated items
	 */
	@Getter
	private static volatile Set<SimpleHologram> registeredItems = new HashSet<>();

	/**
	 * The ticking task responsible for calling {@link #onTick()}
	 */
	private static volatile BukkitTask tickingTask = null;

	/**
	 * The armor stand names, each line spawns another invisible stand
	 */
	@Getter
	private final List<ArmorStand> loreEntities = new ArrayList<>();

	/**
	 * The spawning location
	 */
	private final Location lastTeleportLocation;

	/**
	 * The lore over the item
	 */
	@Getter
	private final List<String> loreLines = new ArrayList<>();

	/**
	 * Optional particles spawning below the hologram
	 */
	@Getter
	private final List<Tuple<CompParticle, Object>> particles = new ArrayList<>();

	/**
	 * The displayed entity
	 */
	@Getter
	private Entity entity;

	/**
	 * The players' UUID this hologram is hidden from
	 */
	private final Set<UUID> hiddenFromPlayers = new HashSet<>();

	/*
	 * A private flag to help with teleporting of this entity
	 */
	private Location pendingTeleport = null;

	/*
	 * Constructs a new item and registers it
	 */
	protected SimpleHologram(Location spawnLocation) {
		this.lastTeleportLocation = spawnLocation.clone();

		registeredItems.add(this);

		onReload();
	}

	/**
	 * Restart ticking task on reload
	 *
	 * @deprecated internal use only, do not call
	 */
	@Deprecated
	public static void onReload() {
		if (tickingTask != null)
			tickingTask.cancel();

		tickingTask = scheduleTickingTask();
	}

	/*
	 * Helper method to start main anim ticking task
	 */
	private static BukkitTask scheduleTickingTask() {
		return Common.runTimer(1, () -> {

			for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
				final SimpleHologram model = it.next();

				if (model.isSpawned())
					if (!model.getEntity().isValid() || model.getEntity().isDead()) {
						model.removeLore();
						model.getEntity().remove();

						it.remove();
					} else
						model.tick();
			}
		});
	}

	/**
	 * Spawns this hologram entity
	 *
	 * @return
	 */
	public SimpleHologram spawn() {
		Valid.checkBoolean(!this.isSpawned(), this + " is already spawned!");
		Valid.checkNotEmpty(this.loreLines, "Call lore() first before calling spawn method for " + this);

		this.entity = this.createEntity();
		Valid.checkNotNull(this.entity, "Failed to spawn entity from " + this);

		this.drawLore(getLastTeleportLocation());

		return this;
	}

	/**
	 * Core implementation method to spawn your entity
	 *
	 * @return
	 */
	protected abstract Entity createEntity();

	/*
	 * Set a lore for this armor stand
	 */
	private void drawLore(Location location) {

		// Lower the hologram a little bit if the entity is a small armor stand
		if (this.entity instanceof ArmorStand && ((ArmorStand) this.entity).isSmall())
			location.subtract(0, 0.5, 0);

		// Start creating from the top
		location.add(0, 2 + loreLineHeight * (loreLines.size() - 1), 0);

		final World world = location.getWorld();

		for (final String loreLine : this.loreLines) {
			final ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);

			armorStand.setGravity(false);
			armorStand.setVisible(false);
			armorStand.setMarker(true);

			Remain.setCustomName(armorStand, loreLine);

			location.subtract(0, loreLineHeight, 0);

			this.loreEntities.add(armorStand);
		}
	}

	/*
	 * Iterate the ticking mechanism of this entity
	 */
	private void tick() {

		if (this.pendingTeleport != null) {
			this.entity.teleport(this.pendingTeleport);

			// The first line is on the top
			pendingTeleport.add(0, loreLineHeight * (loreLines.size() - 1), 0);

			for (final ArmorStand loreEntity : this.loreEntities) {
				loreEntity.teleport(this.pendingTeleport);

				pendingTeleport.subtract(0, loreLineHeight, 0);
			}

			this.pendingTeleport = null;
			return;
		}

		this.onTick();

		for (final Tuple<CompParticle, Object> tuple : this.particles) {
			final CompParticle particle = tuple.getKey();
			final Object extra = tuple.getValue();

			if (extra instanceof CompMaterial)
				particle.spawn(this.getLocation(), (CompMaterial) extra);

			else if (extra instanceof Double)
				particle.spawn(this.getLocation(), (double) extra);
		}
	}

	/**
	 * Called automatically where you can animate this armor stand
	 */
	protected void onTick() {
	}

	/**
	 * Return true if this armor stand is spawned
	 *
	 * @return
	 */
	public final boolean isSpawned() {
		return this.entity != null;
	}

	/**
	 * Deletes all text that the armor stand has
	 */
	public final void removeLore() {
		this.loreEntities.forEach(ArmorStand::remove);
		this.loreEntities.clear();
	}

	/**
	 * @param lore
	 * @return
	 */
	public final SimpleHologram setLore(String... lore) {
		this.loreLines.clear();
		this.loreLines.addAll(Arrays.asList(lore));

		// Update lore on the spawned hologram
		if (isSpawned()) {
			removeLore();
			drawLore(getLastTeleportLocation());
		}

		return this;
	}

	/**
	 * Updates the line at the index to the given line
	 *
	 * @param index
	 * @param line
	 * @return
	 */
	public final SimpleHologram setLine(int index, String line) {
		Valid.checkBoolean(index >= 0 && index < this.loreLines.size(), "Index " + index + " is out of range for " + this);
		final ArmorStand stand = this.loreEntities.get(index);

		Remain.setCustomName(stand, line);

		return this;
	}

	/**
	 * Add particle effect for this hologram
	 *
	 * @param particle
	 */
	public final void addParticleEffect(CompParticle particle) {
		this.addParticleEffect(particle, null);
	}

	/**
	 * Add particle effect for this hologram
	 *
	 * @param particle
	 * @param data
	 */
	public final void addParticleEffect(CompParticle particle, CompMaterial data) {
		this.particles.add(new Tuple<>(particle, data));
	}

	/**
	 * Shows this hologram to the player via packets, returning if it's already shown
	 *
	 * @param player
	 */
	public final void showTo(Player player) {
		this.checkSpawned("showTo");

		if (!isHiddenFrom(player))
			return;

		// Show the hidden entity too
		Remain.sendPacket(player, ReflectionUtil.instantiate(SPAWN_ENTITY_LIVING_PACKET, Remain.getHandleEntity(entity)));

		for (final ArmorStand stand : loreEntities)
			Remain.sendPacket(player, ReflectionUtil.instantiate(SPAWN_ENTITY_LIVING_PACKET, Remain.getHandleEntity(stand)));

		hiddenFromPlayers.remove(player.getUniqueId());
	}

	/**
	 * Hides this hologram from the player via packets, returning if it's already hidden
	 *
	 * @param player
	 */
	public final void hideFrom(Player player) {
		this.checkSpawned("hideFrom");

		if (isHiddenFrom(player))
			return;

		final int[] ids = new int[loreEntities.size() + 1];

		// Hide the entity too
		ids[0] = entity.getEntityId();

		for (int i = 0; i < loreEntities.size(); i++)
			ids[i + 1] = loreEntities.get(i).getEntityId();

		Remain.sendPacket(player, ReflectionUtil.instantiate(DESTROY_ENTITIES_PACKET, ids));

		hiddenFromPlayers.add(player.getUniqueId());
	}

	/**
	 * Checks if the given player can see this hologram
	 *
	 * @param player
	 * @return true if the player can NOT see this hologram
	 */
	public final boolean isHiddenFrom(Player player) {
		return hiddenFromPlayers.contains(player.getUniqueId());
	}

	/**
	 * Return the current armor stand location
	 *
	 * @return
	 */
	public final Location getLocation() {
		this.checkSpawned("getLocation");

		return this.entity.getLocation();
	}

	/**
	 * Returns a copy of the last teleport location
	 *
	 * @return
	 */
	public final Location getLastTeleportLocation() {
		return this.lastTeleportLocation.clone();
	}

	/**
	 * Teleport this hologram with its lores to the given location
	 *
	 * @param location
	 */
	public final void teleport(Location location) {
		if (this.pendingTeleport != null)
			return;

		this.checkSpawned("teleport");

		this.lastTeleportLocation.setX(location.getX());
		this.lastTeleportLocation.setY(location.getY());
		this.lastTeleportLocation.setZ(location.getZ());

		this.pendingTeleport = location;
	}

	/**
	 * Deletes this armor stand
	 */
	public final void remove() {
		this.removeLore();

		if (this.entity != null) {
			this.entity.remove();

			this.entity = null;
		}

		registeredItems.remove(this);
	}

	/*
	 * A helper method to check if this entity is spawned
	 */
	private void checkSpawned(String method) {
		Valid.checkBoolean(this.isSpawned(), this + " is not spawned, cannot call " + method + "!");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SimpleHologram{spawnLocation=" + Common.shortLocation(this.lastTeleportLocation) + ", spawned=" + this.isSpawned() + "}";
	}

	/**
	 * Deletes all floating items on the server
	 */
	public static final void deleteAll() {

		for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
			final SimpleHologram item = it.next();

			if (item.isSpawned())
				item.getEntity().remove();

			item.removeLore();
			it.remove();
		}
	}
}
