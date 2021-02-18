package org.mineacademy.fo.display;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

import java.util.function.Function;

/**
 * A simple class for displaying complete bossbars with a color, style and a duration time
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleBossBar implements ConfigSerializable {
	private final String message;
	private final CompBarColor color;
	private final CompBarStyle style;

	private final int timer;

	/**
	 * Shows this bossbar to the given players
	 *
	 * @param players
	 */
	public void show(final Iterable<Player> players, final Function<String, String> replacer) {
		for (final Player player : players)
			show(player, replacer);
	}

	/**
	 * Shows this bossbar to the given player
	 *
	 * @param player
	 */
	public void show(final Player player, final Function<String, String> replacer) {
		if (timer > 0)
			Remain.sendBossbarTimed(player, replacer.apply(message), timer, color, style);
		else
			Remain.sendBossbarPercent(player, replacer.apply(message), 100, color, style);
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = SerializedMap.ofArray("Message", message,
				"Color", color.toString(),
				"Style", style.toString());

		if (timer > 0)
			map.put("Timer", timer);

		return map;
	}

	/**
	 * Creates a new SimpleBossBar from the serialized map
	 *
	 * @param map
	 * @return
	 */
	public static SimpleBossBar deserialize(final SerializedMap map) {
		final String message = Common.colorize(map.getString("Message", ""));
		final CompBarColor color = CompBarColor.fromKey(map.getString("Color", "PURPLE"));
		final CompBarStyle style = CompBarStyle.fromKey(map.getString("Style", "SOLID"));

		final int timer = map.getInteger("Timer", 0);

		return new SimpleBossBar(message, color, style, timer);
	}
}
