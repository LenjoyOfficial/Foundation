package org.mineacademy.fo.display;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.CompColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * An extension of SimpleDisplay allowing to show progress bars too
 */
public class SimpleProgressDisplay extends SimpleDisplay {
	/**
	 * The size of the progress bar
	 */
	@Getter
	private final int size;

	/**
	 * The message shown when calling {@link #showProgress(Player, int, Function)}
	 * with 0 percent
	 */
	private final String emptyMessage;

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
	 * The color of the progressed part for different ranges.
	 * <p>
	 * For example: 1-2 progressed character -> red color, 3-4 -> orange color etc.
	 */
	private final Map<RangedValue, CompColor> ranges = new HashMap<>();

	/**
	 * Creates a new instance from the given serialized map
	 *
	 * @param map
	 */
	public SimpleProgressDisplay(SerializedMap map) {
		super(map);

		emptyMessage = Common.colorize(map.getString("EmptyMessage"));

		map = map.getMap("ProgressBar");
		this.size = map.getInteger("Size");

		this.progressChar = map.getString("ProgressChar").charAt(0);
		this.remainingChar = map.getString("RemainingChar").charAt(2);
		this.remainingColor = ChatColor.getByChar(map.getString("RemainingChar").charAt(1));

		final SerializedMap rangeMap = map.getMap("RangeColors");

		for (final String key : rangeMap.keySet())
			ranges.put(RangedValue.parse(key), CompColor.fromName(rangeMap.getString(key)));
	}

	/**
	 * Shows this display with the progress bar formatted with the given progress
	 *
	 * @param players
	 * @param percent  the progress percent from 0 to 100
	 * @param replacer
	 */
	public void showProgress(final Iterable<Player> players, final int percent, final Function<String, String> replacer) {
		for (final Player player : players)
			showProgress(player, percent, replacer);
	}

	/**
	 * Shows this display with the progress bar formatted with the given progress
	 *
	 * @param player
	 * @param percent  the progress percent from 0 to 100
	 * @param replacer
	 */
	public void showProgress(final Player player, final int percent, final Function<String, String> replacer) {
		final int progress = size * percent / 100;
		CompColor color = CompColor.GREEN;

		for (final Map.Entry<RangedValue, CompColor> range : ranges.entrySet())
			if (range.getKey().isWithin(progress))
				color = range.getValue();

		final String progressBar = color.getChatColor().toString() + Common.fancyBar(progress, progressChar, size, remainingChar, remainingColor);

		show(player, message -> percent == 0 && !emptyMessage.isEmpty() ?
				replacer != null ? replacer.apply(emptyMessage) : emptyMessage :
				Replacer.replaceArray(replacer != null ? replacer.apply(message) : message,
						"progressBar", progressBar));
	}
}
