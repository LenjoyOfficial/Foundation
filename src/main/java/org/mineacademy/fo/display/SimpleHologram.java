package org.mineacademy.fo.display;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * A simple hologram creator
 */
public final class SimpleHologram {
	private static final Constructor<?> SPAWN_ENTITY_PACKET;
	private static final Constructor<?> DESTROY_ENTITY_PACKET;

	private static final Method GET_HANDLE;

	static {
		SPAWN_ENTITY_PACKET = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutSpawnEntityLiving"), ReflectionUtil.getNMSClass("EntityLiving"));
		DESTROY_ENTITY_PACKET = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy"), int[].class);

		GET_HANDLE = ReflectionUtil.getMethod(ReflectionUtil.getOBCClass("entity.CraftArmorStand"), "getHandle");
	}

	/**
	 * The current location of the lowest armor stand
	 */
	@Setter
	private Location location;

	/**
	 * The current lines the hologram is showing
	 */
	private final String[] lines;

	/**
	 * The armor stands displaying the lines
	 */
	private final ArmorStand[] armorStands;

	/**
	 * Has this hologram been spawned?
	 */
	@Getter
	private boolean created = false;

	/**
	 * The list of players from whom this hologram is hidden
	 */
	private final StrictSet<UUID> hiddenPlayers = new StrictSet<>();

	/**
	 * Creates a new Hologram. & colors are supported for the lines.
	 *
	 * @param location
	 * @param lines
	 */
	public SimpleHologram(final Location location, final String... lines) {
		Valid.checkBoolean(lines.length > 0, "Lines are null");

		this.location = location;
		this.lines = StringUtils.split(Common.colorize(lines), "\n");
		this.armorStands = new ArmorStand[this.lines.length];
	}

	/**
	 * Spawns the invisible armor stands with their respective lines as their names.
	 *
	 * @return this
	 */
	public SimpleHologram create() {
		Valid.checkNotNull(location, "Location is null");

		final Location spawnLoc = location.clone().add(0, (lines.length - 1) * 0.3, 0);

		for (int i = 0; i < lines.length; i++) {
			final ArmorStand armorStand = location.getWorld().spawn(spawnLoc, ArmorStand.class);
			spawnLoc.subtract(0, 0.3, 0);

			armorStand.setSmall(true);
			armorStand.setVisible(false);
			armorStand.setGravity(false);
			armorStand.setMarker(true);

			CompProperty.INVULNERABLE.apply(armorStand, true);
			Remain.setCustomName(armorStand, lines[i]);

			armorStands[i] = armorStand;
		}

		created = true;
		return this;
	}

	/**
	 * Sets the text at the given index of the lines.
	 *
	 * @param index   throws an error if null
	 * @param newLine the new line for replacement
	 */
	public void setLine(final int index, final String newLine) {
		Valid.checkNotNull(index, "Index is null");
		Valid.checkBoolean(Valid.isInRange(index, 0, lines.length - 1), index + " is less than 0 or greater than the size of lines!");

		Remain.setCustomName(armorStands[index], Common.getOrEmpty(newLine));
	}

	/**
	 * Removes the line at the given index. The above armor stands will automatically
	 * teleport themselves lower to fill out the removed line's space.
	 *
	 * @param index the index of the line in the lines
	 */
	public void removeLine(final int index) {
		Valid.checkNotNull(index, "Index is null");
		Valid.checkBoolean(Valid.isInRange(index, 0, lines.length - 1), index + " is less than 0 or greater than the size of lines!");

		for (int i = 0; i < lines.length; i++) {
			// teleporting the armor stands lower whose index is lower than 'index' (it is above the armor stand at 'index')
			if (i < index)
				armorStands[i].teleport(armorStands[i].getLocation().subtract(0, 0.3, 0));

				// moving the lines and armor stands lower with one index in the arrays whose index is greater than 'index'
			else if (i > index) {
				lines[i - 1] = lines[i];
				armorStands[i - 1] = armorStands[i];

				if (i == lines.length - 1) {
					lines[i] = null;
					armorStands[i] = null;
				}
			}
		}
	}

	/**
	 * Shows this hologram to the given player, throwing an exception if already shown
	 *
	 * @param player
	 */
	public void showTo(final Player player) {
		Valid.checkBoolean(created, "Hologram hasn't been created yet");
		Valid.checkBoolean(hiddenPlayers.contains(player.getUniqueId()), "Player " + player.getName() + " already sees this hologram!");

		for (final ArmorStand as : armorStands)
			Remain.sendPacket(player, ReflectionUtil.instantiate(SPAWN_ENTITY_PACKET, ReflectionUtil.invoke(GET_HANDLE, as)));

		hiddenPlayers.remove(player.getUniqueId());
	}

	/**
	 * Hides this hologram from the given player, throwing an exception if already hidden
	 *
	 * @param player
	 */
	public void hideFrom(final Player player) {
		Valid.checkBoolean(created, "Hologram hasn't been created yet");
		Valid.checkBoolean(!hiddenPlayers.contains(player.getUniqueId()), "Hologram " + player.getName() + " already hidden from player " + player.getName() + "!");

		final int[] ids = new int[armorStands.length];
		int index = 0;

		for (final ArmorStand as : armorStands)
			ids[index++] = as.getEntityId();

		Remain.sendPacket(player, ReflectionUtil.instantiate(DESTROY_ENTITY_PACKET, ids));
		hiddenPlayers.add(player.getUniqueId());
	}

	/**
	 * Teleports the hologram to the given location.
	 *
	 * @param location throw an error if null
	 */
	public void teleport(@NonNull final Location location) {
		Valid.checkBoolean(created, "Hologram hasn't been created yet");

		this.location = location;

		if (armorStands.length > 0) {
			final Location spawnLoc = location.clone().add(0, (lines.length - 1) * 0.3, 0);

			for (final ArmorStand as : armorStands) {
				as.teleport(spawnLoc);
				spawnLoc.subtract(0, 0.3, 0);
			}
		}
	}

	/**
	 * Removes the armor stands. You can spawn them back with {@link #create()}.
	 */
	public void remove() {
		Valid.checkBoolean(created, "Hologram already despawned");

		Arrays.asList(armorStands).forEach(Entity::remove);
		Arrays.fill(armorStands, null);

		created = false;
	}
}
