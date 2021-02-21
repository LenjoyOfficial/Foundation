package org.mineacademy.fo.model;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompChatColor;

import java.util.HashMap;
import java.util.Map;

/**
 * A special class for pairing number ranges with chat colors
 * and then getting the chat color for a value
 * <p>
 * Examples: coloring the time left in a countdown, displaying a mob's health
 */
public class ColoredRanges {
	/**
	 * The default color to use for a value that isn't in any of the
	 * specified ranges
	 */
	private final CompChatColor defaultColor;

	/**
	 * The colors for the number ranges
	 */
	private final Map<RangedValue, CompChatColor> ranges = new HashMap<>();

	/**
	 * Creates a new instance from the given serialized map
	 *
	 * @param map
	 */
	public ColoredRanges(final SerializedMap map) {
		this.defaultColor = CompChatColor.of(map.getString("DefaultColor", "GREEN"));

		for (final String range : map.keySet())
			ranges.put(RangedValue.parse(range), CompChatColor.of(map.getString(range)));
	}

	/**
	 * Formats the given number with its range color, or "" if value is null
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
	 * Returns the chat color for the number range that contains the given value,
	 * or {@link #defaultColor} if the value is null or isn't in any range
	 *
	 * @param value
	 * @return
	 */
	public CompChatColor getColor(final Number value) {
		if (value == null)
			return defaultColor;

		CompChatColor color = defaultColor;

		for (final Map.Entry<RangedValue, CompChatColor> range : ranges.entrySet())
			if (range.getKey().isWithin(value)) {
				color = range.getValue();

				break;
			}

		return color;
	}
}
