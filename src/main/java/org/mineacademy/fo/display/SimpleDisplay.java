package org.mineacademy.fo.display;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.Remain;

import java.util.function.Function;

/**
 * A class holding different optional display messages that can be shown to a player
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SimpleDisplay {
	private final String message;
	private final boolean addPrefix;

	private final TitleHelper title;
	private final SimpleActionBar actionBar;
	private final SimpleBossBar bossBar;

	/**
	 * Creates a new SimpleDisplay from a map. If a display type is not set, it will be declared as null
	 * and won't be shown to the player
	 *
	 * @param map
	 */
	public SimpleDisplay(final SerializedMap map) {
		this.message = Common.colorize(map.getString("Message"));
		this.addPrefix = map.getBoolean("AddPrefix", true);

		this.title = map.containsKey("Title") ? new TitleHelper(map.getMap("Title")) : null;
		this.actionBar = map.containsKey("ActionBar") ? SimpleActionBar.deserialize(map.getMap("ActionBar")) : null;
		this.bossBar = map.containsKey("BossBar") ? SimpleBossBar.deserialize(map.getMap("BossBar")) : null;
	}

	/**
	 * Shows the message only if it is not null or empty
	 *
	 * @param player
	 */
	public void showMessage(final Player player, final Function<String, String> replacer) {
		if (!message.isEmpty())
			if (addPrefix)
				Common.tell(player, replacer.apply(message));
			else
				Common.tellNoPrefix(player, replacer.apply(message));
	}

	/**
	 * Shows the title only if it is not null
	 *
	 * @param player
	 */
	public void showTitle(final Player player, final Function<String, String> replacer) {
		if (title != null)
			title.show(player, replacer);
	}

	/**
	 * Shows the action bar only if it is not null
	 *
	 * @param player
	 */
	public void showActionBar(final Player player, final Function<String, String> replacer) {
		if (actionBar != null)
			actionBar.show(player, replacer);
	}

	/**
	 * Shows the bossbar only if it is not null
	 *
	 * @param player
	 */
	public void showBossBar(final Player player, final Function<String, String> replacer) {
		if (bossBar != null)
			bossBar.show(player, replacer);
	}

	/**
	 * Shows all the displays to the given players
	 *
	 * @param players
	 */
	public void show(final Iterable<Player> players, final Function<String, String> replacer) {
		for (final Player player : players)
			show(player, replacer);
	}

	/**
	 * Shows all the displays to the player only if they're not null
	 *
	 * @param player
	 */
	public void show(final Player player, final Function<String, String> replacer) {
		showMessage(player, replacer);
		showTitle(player, replacer);
		showActionBar(player, replacer);
		showBossBar(player, replacer);
	}
}

@Getter
@AllArgsConstructor
final class TitleHelper {
	/**
	 * The title
	 */
	@Getter
	@Setter
	private String title;

	/**
	 * The subtitle under the title
	 */
	@Getter
	@Setter
	private String subtitle;

	/**
	 * Fade in of the title in ticks
	 */
	private final int fadeIn;

	/**
	 * Time in ticks during which the title will stay on screen after fade in
	 */
	private final int stay;

	/**
	 * Fade out of the title in ticks
	 */
	private final int fadeOut;

	/**
	 * Create new SimpleTitle from the serialized map
	 *
	 * @param map
	 */
	TitleHelper(final SerializedMap map) {
		this(map.getString("Title"), map.getString("Subtitle"), map.getInteger("FadeIn", 20), map.getInteger("Stay", 40), map.getInteger("FadeOut", 20));
	}

	/**
	 * Shows the title with the subtitle and times in this instance to the given player.
	 *
	 * @param player
	 */
	public void show(final Player player, final Function<String, String> replacer) {
		Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer.apply(title), replacer.apply(subtitle));
	}
}

