package org.mineacademy.fo.model;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.RequiredArgsConstructor;

/**
 * A class for pairing number ranges with chat colors
 * and then getting the chat color for a value
 * <p>
 * Examples: coloring the time left in a countdown, displaying a mob's health
 */
@RequiredArgsConstructor
public class ColoredRanges implements ConfigSerializable {

	/**
	 * The default color to use for a value that isn't in any of the
	 * specified ranges
	 */
	private final ChatColor defaultColor;

	/**
	 * The colors for the number ranges
	 */
	private final Map<RangedValue, ChatColor> ranges;

	/**
	 * Formats the given number with its range color. Returns "" if value is null
	 *
	 * @param value
	 * @return
	 */
	public String formatWithColor(final Number value) {
		if (value == null)
			return "";

		return getColor(value).toString() + value;
	}

	/**
	 * Returns true if the given number is within any colored range
	 *
	 * @param value
	 * @return
	 */
	public boolean hasColor(final Number value) {
		for (final RangedValue range : ranges.keySet())
			if (range.isWithin(value))
				return true;

		return false;
	}

	/**
	 * Returns the chat color for the number range that contains the given value,
	 * or {@link #defaultColor} if the value is null or isn't in any range
	 *
	 * @param value
	 * @return
	 */
	public ChatColor getColor(final Number value) {
		if (value == null)
			return defaultColor;

		ChatColor color = defaultColor;

		for (final Map.Entry<RangedValue, ChatColor> range : ranges.entrySet())
			if (range.getKey().isWithin(value)) {
				color = range.getValue();

				break;
			}

		return color;
	}

	// -----------------------------------------------------------------------------------
	// Serializing
	// -----------------------------------------------------------------------------------

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = SerializedMap.of("Default_Color", defaultColor);

		for (final Map.Entry<RangedValue, ChatColor> entry : ranges.entrySet())
			map.put(entry.getKey().toLine(), entry.getValue());

		return map;
	}

	/**
	 * Creates a new instance from the given serialized map
	 *
	 * @param map
	 * @return
	 */
	public static ColoredRanges deserialize(final SerializedMap map) {
		final ChatColor defaultColor = ChatColor.valueOf(map.getString("Default_Color", "GREEN"));
		final Map<RangedValue, ChatColor> ranges = new HashMap<>();

		for (final String range : map.keySet())
			if (!"Default_Color".equals(range))
				ranges.put(RangedValue.parse(range), ChatColor.valueOf(map.getString(range)));

		return new ColoredRanges(defaultColor, ranges);
	}
}
