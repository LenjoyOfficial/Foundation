package org.mineacademy.fo.model;

import java.util.HashMap;
import java.util.Map;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompColor;

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
	private final CompColor defaultColor;

	/**
	 * The colors for the number ranges
	 */
	private final Map<RangedValue, CompColor> ranges;

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
	public CompColor getColor(final Number value) {
		if (value == null)
			return defaultColor;

		CompColor color = defaultColor;

		for (final Map.Entry<RangedValue, CompColor> range : ranges.entrySet())
			if (range.getKey().isWithin(value)) {
				color = range.getValue();

				break;
			}

		return color;
	}

	// -----------------------------------------------------------------------------------
	// Serializing
	// -----------------------------------------------------------------------------------

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = SerializedMap.of("Default_Color", defaultColor.getName());

		for (final Map.Entry<RangedValue, CompColor> entry : ranges.entrySet())
			map.put(entry.getKey().toLine(), entry.getValue().getName());

		return map;
	}

	/**
	 * Creates a new instance from the given serialized map
	 *
	 * @param map
	 * @return
	 */
	public static ColoredRanges deserialize(final SerializedMap map) {
		final CompColor defaultColor = CompColor.fromName(map.getString("Default_Color", "GREEN"));
		final Map<RangedValue, CompColor> ranges = new HashMap<>();

		for (final String range : map.keySet())
			if (!"Default_Color".equals(range))
				ranges.put(RangedValue.parse(range), CompColor.fromName(map.getString(range)));

		return new ColoredRanges(defaultColor, ranges);
	}
}
