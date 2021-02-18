package org.mineacademy.fo.display;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.Remain;

import java.lang.reflect.Constructor;
import java.util.function.Function;

/**
 * A simple class for showing action bars with custom fade in, stay and fade out times.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleActionBar implements ConfigSerializable {
	private static Constructor<?> TITLE_TIMES_CONSTRUCTOR;

	static {
		try {
			TITLE_TIMES_CONSTRUCTOR = ReflectionUtil.getNMSClass("PacketPlayOutTitle").getConstructor(int.class, int.class, int.class);

		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	@Setter
	private String message;

	private final int fadeIn;
	private final int stay;
	private final int fadeOut;

	private final Object timesPacket;

	/**
	 * Shows this action bar to the given players
	 *
	 * @param players
	 */
	public void show(final Iterable<Player> players, final Function<String, String> replacer) {
		for (final Player player : players)
			show(player, replacer);
	}

	/**
	 * Shows this action bar to the given player
	 *
	 * @param player
	 */
	public void show(final Player player, final Function<String, String> replacer) {
		// Set the times for the action bar
		Remain.sendPacket(player, timesPacket);
		Remain.sendActionBar(player, replacer.apply(message));
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Message", message,
				"FadeIn", fadeIn,
				"Stay", stay,
				"FadeOut", fadeOut);
	}

	/**
	 * Create a new SimpleActionBar from a serialized map
	 *
	 * @param map
	 */
	public static SimpleActionBar deserialize(final SerializedMap map) {
		final String message = Common.colorize(map.getString("Message"));

		final int fadeIn = map.getInteger("FadeIn");
		final int stay = map.getInteger("Stay");
		final int fadeOut = map.getInteger("FadeOut");

		final Object timesPacket = ReflectionUtil.instantiate(TITLE_TIMES_CONSTRUCTOR, fadeIn, stay, fadeOut);

		return new SimpleActionBar(message, fadeIn, stay, fadeOut, timesPacket);
	}
}
