package org.mineacademy.fo.display;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ColoredRanges;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.CompChatColor;

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
	 * The colors of the progressed parts in ranges
	 */
	private final ColoredRanges ranges;

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

		ranges = new ColoredRanges(map.getMap("RangeColors"));
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
		final CompChatColor color = ranges.getColor(progress);

		final String progressBar = color.toString() + Common.fancyBar(progress, progressChar, size, remainingChar, remainingColor);

		show(player, message -> percent == 0 && !emptyMessage.isEmpty() ?
				replacer != null ? replacer.apply(emptyMessage) : emptyMessage :
				Replacer.replaceArray(replacer != null ? replacer.apply(message) : message,
						"progressBar", progressBar));
	}
}
