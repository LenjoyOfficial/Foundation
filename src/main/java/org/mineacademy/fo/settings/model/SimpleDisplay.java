package org.mineacademy.fo.settings.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

import java.lang.reflect.Constructor;
import java.util.function.Function;

/**
 * A class holding different optional display messages that can be shown to a player
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleDisplay {

	// --------------------------------------------------------------------------
	// Display data
	// --------------------------------------------------------------------------

	/**
	 * Should we add the {@link Common#getTellPrefix() tell prefix} to the chat message?
	 */
	private final boolean addPrefix;

	/**
	 * The chat message
	 */
	private final String message;

	/**
	 * The title with subtitle
	 */
	private final TitleHelper title;

	/**
	 * The message for action bar
	 */
	private final ActionBarHelper actionBar;

	/**
	 * The message for boss bar with color and style
	 */
	private final BossBarHelper bossBar;

	/**
	 * The replacer function applied to all the messages in this display
	 * before showing it to the player
	 */
	@Setter
	protected Function<String, String> replacer = Function.identity();

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
		this.actionBar = map.containsKey("ActionBar") ? new ActionBarHelper(map.getMap("ActionBar")) : null;
		this.bossBar = map.containsKey("BossBar") ? new BossBarHelper(map.getMap("BossBar")) : null;
	}

	/**
	 * Shows the message only if it is not null or empty
	 *
	 * @param player
	 */
	public void showMessage(final Player player) {
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
	public void showTitle(final Player player) {
		if (title != null)
			title.show(player);
	}

	/**
	 * Shows the action bar only if it is not null
	 *
	 * @param player
	 */
	public void showActionBar(final Player player) {
		if (actionBar != null)
			actionBar.show(player);
	}

	/**
	 * Shows the bossbar only if it is not null
	 *
	 * @param player
	 */
	public void showBossBar(final Player player) {
		if (bossBar != null)
			bossBar.show(player);
	}

	/**
	 * Shows all the displays to the given players with the variables being replaced
	 * with the given function
	 *
	 * @param players
	 */
	public void show(final Iterable<Player> players, final Function<String, String> replacer) {
		for (final Player player : players)
			show(player, replacer);
	}

	/**
	 * Shows all the displays to the player with the variables being replaced
	 * with the given function
	 *
	 * @param player
	 * @param replacer
	 */
	public void show(final Player player, final Function<String, String> replacer) {
		final Function<String, String> wasReplacer = this.replacer;

		// Nest this replacer with the old one
		this.replacer = replacer.andThen(wasReplacer);

		showMessage(player);
		showTitle(player);
		showActionBar(player);
		showBossBar(player);

		this.replacer = wasReplacer;
	}

	@AllArgsConstructor
	private final class TitleHelper {

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

		private TitleHelper(final SerializedMap map) {
			this(map.getString("Title"),
					map.getString("Subtitle"),
					map.getInteger("FadeIn", 20),
					map.getInteger("Stay", 40),
					map.getInteger("FadeOut", 20));
		}

		/**
		 * Shows the title with the subtitle and times in this instance to the given player.
		 *
		 * @param player
		 */
		public void show(final Player player) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer.apply(title), replacer.apply(subtitle));
		}
	}

	private final class ActionBarHelper {

		/**
		 * The message of the action bar
		 */
		@Setter
		private String message;

		private ActionBarHelper(final SerializedMap map) {
			this.message = Common.colorize(map.getString("Message"));
		}

		/**
		 * Shows this action bar to the given player
		 *
		 * @param player
		 */
		public void show(final Player player) {
			Remain.sendActionBar(player, replacer.apply(message));
		}
	}

	@AllArgsConstructor
	private final class BossBarHelper {
		/**
		 * The message above the bar
		 */
		private final String message;

		/**
		 * The color of the boss bar
		 */
		private final CompBarColor color;

		/**
		 * The style of the boss bar
		 */
		private final CompBarStyle style;

		/**
		 * The duration of showing this boss bar
		 */
		private final int timer;

		private BossBarHelper(final SerializedMap map) {
			this(Common.colorize(map.getString("Message", "")),
					CompBarColor.fromKey(map.getString("Color", "PURPLE")),
					CompBarStyle.fromKey(map.getString("Style", "SOLID")),
					map.getInteger("Timer", 0));
		}

		/**
		 * Shows this bossbar to the given player
		 *
		 * @param player
		 */
		public void show(final Player player) {
			if (timer > 0)
				Remain.sendBossbarTimed(player, replacer.apply(message), timer, color, style);
			else
				Remain.sendBossbarPercent(player, replacer.apply(message), 100, color, style);
		}
	}
}
