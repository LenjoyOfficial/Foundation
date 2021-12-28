package org.mineacademy.fo.settings.model;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleProgressBar;

/**
 * An extension of SimpleDisplay allowing to show progress bars too
 */
public class SimpleProgressDisplay extends SimpleDisplay {

	/**
	 * The options for the progress bar
	 */
	private final SimpleProgressBar progressBar;

	/**
	 * The message shown when calling {@link #showProgress(Player, int, Function)}
	 * with 0 percent
	 */
	private final String emptyMessage;

	/**
	 * Creates a new instance from the given serialized map
	 *
	 * @param map
	 */
	public SimpleProgressDisplay(SerializedMap map) {
		super(map);

		progressBar = SimpleProgressBar.deserialize(map.getMap("Progress_Bar"));
		emptyMessage = Common.colorize(map.getString("Empty_Message"));
	}

	/**
	 * Shows this display with the progress bar formatted with the given progress
	 *
	 * @param players
	 * @param percent the progress percent from 0 to 100
	 */
	public void showProgress(final Iterable<Player> players, final int percent, final Function<String, String> replacer) {
		for (final Player player : players)
			showProgress(player, percent, replacer);
	}

	/**
	 * Shows this display with the progress bar formatted with the given progress
	 *
	 * @param player
	 * @param percent the progress percent from 0 to 100
	 */
	public void showProgress(final Player player, final int percent, final Function<String, String> replacer) {
		final String progressBar = this.progressBar.getProgressBar(percent);

		final Function<String, String> notNullReplacer = Common.getOrDefault(replacer, Function.identity());

		show(player, message -> percent == 0 && !emptyMessage.isEmpty() ?
				notNullReplacer.apply(emptyMessage) :
				notNullReplacer.apply(Replacer.replaceArray(message, "progressBar", progressBar)));
	}
}
