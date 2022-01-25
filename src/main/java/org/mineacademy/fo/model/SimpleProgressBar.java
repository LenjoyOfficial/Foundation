package org.mineacademy.fo.model;

import org.bukkit.ChatColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SimpleProgressBar implements ConfigSerializable {

	/**
	 * The size of the progress bar
	 */
	@Getter
	private final int size;

	/**
	 * The character of the progressed part
	 */
	private final char progressChar;

	/**
	 * The character of the remaining part
	 */
	private final char remainingChar;

	/**
	 * The color of the remaining part
	 */
	private final ChatColor remainingColor;

	/**
	 * The colors of the progressed parts in ranges
	 */
	private final ColoredRanges ranges;

	/**
	 * Returns a progress bar text with the given progress that's <= this progress bar's size,
	 * colored as defined in this SimpleProgressBar
	 *
	 * @param progress
	 * @return
	 */
	public String getProgressBar(final int progress) {
		return getProgressBar((float) progress / size);
	}

	/**
	 * Returns a progress bar text with progress equal to the given percentage,
	 * colored as defined in this SimpleProgressBar
	 *
	 * @param percent the percentage from 0 to 1
	 * @return
	 */
	public String getProgressBar(final float percent) {
		final int finalProgress = (int) (size * percent);
		final ChatColor color = ranges.getColor(finalProgress);

		return color.toString() + Common.fancyBar(finalProgress, progressChar, size, remainingChar, remainingColor);
	}

	// -----------------------------------------------------------------------------------
	// Serializing
	// -----------------------------------------------------------------------------------

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Size", size,
				"Progress_Char", progressChar,
				"Remaining_Char", remainingChar,
				"Remaining_Color", remainingColor,
				"Range_Colors", ranges
		);
	}

	/**
	 * Deserializes the given map into a SimpleProgressBar
	 *
	 * @param map
	 * @return
	 */
	public static SimpleProgressBar deserialize(SerializedMap map) {
		final int size = map.getInteger("Size");
		final char progressChar = map.getString("Progress_Char").charAt(0);
		final char remainingChar = map.getString("Remaining_Char").charAt(0);
		final ChatColor remainingColor = ChatColor.valueOf(map.getString("Remaining_Color"));

		final ColoredRanges ranges = ColoredRanges.deserialize(map.getMap("Range_Colors"));

		return new SimpleProgressBar(size, progressChar, remainingChar, remainingColor, ranges);
	}
}
