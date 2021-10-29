package org.mineacademy.fo.model;

import static org.mineacademy.fo.ReflectionUtil.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a displayable scoreboard for the players. It is working with packets, so you get absolutely no lag
 * when using it.
 * <p>
 * Based on FastBoard by MrMicky
 *
 * @author MrMicky, Lenjoy
 */
public class PacketScoreboard {

	// Cached color codes for best performance
	private static final String[] CHAT_COLORS = Common.convert(ChatColor.values(), ChatColor::toString).toArray(new String[0]);

	private static final Object UNSAFE;
	private static final Method ALLOCATE_INSTANCE;

	// ------------------------------------------------------------------------------------------------------------
	// NMS constructors, methods and fields
	// ------------------------------------------------------------------------------------------------------------

	private static final Class<?> OBJECTIVE_PACKET;
	private static final Class<?> DISPLAY_OBJECTIVE_PACKET;
	private static final Class<?> SCORE_PACKET;
	private static final Class<?> TEAM_PACKET;

	// 1.17 team packet
	private static Class<?> TEAM_PARAMETERS;

	// Chat format
	private static Class<? extends Enum> CHAT_FORMAT_ENUM;
	private static Object CHAT_FORMAT_RESET;

	// Score
	private static final Class<? extends Enum> SCORE_ACTION_ENUM;

	private static final Object SCORE_ACTION_CHANGE;
	private static final Object SCORE_ACTION_REMOVE;

	// Render type
	private static final Class<? extends Enum> RENDERTYPE_ENUM;

	private static final Object RENDERTYPE_INTEGER;

	static {
		// Initializing Unsafe
		final Class<Object> unsafe = lookupClass("sun.misc.Unsafe");

		UNSAFE = getStaticFieldContent(unsafe, "theUnsafe");
		ALLOCATE_INSTANCE = getMethod(unsafe, "allocateInstance", Class.class);

		final boolean atLeast1_17 = MinecraftVersion.atLeast(V.v1_17);

		OBJECTIVE_PACKET = getNMSClass("PacketPlayOutScoreboardObjective", "net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective");
		DISPLAY_OBJECTIVE_PACKET = getNMSClass("PacketPlayOutScoreboardDisplayObjective", "net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective");

		SCORE_PACKET = getNMSClass("PacketPlayOutScoreboardScore", "net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore");

		// 1.13 moved ScoreAction to ScoreboardServer from the packet class
		SCORE_ACTION_ENUM = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ?
				getNMSClass("ScoreboardServer$Action", "net.minecraft.server.ScoreboardServer$Action").asSubclass(Enum.class) :
				SCORE_PACKET.getDeclaredClasses()[0].asSubclass(Enum.class);

		SCORE_ACTION_CHANGE = lookupEnumSilent(SCORE_ACTION_ENUM, "CHANGE");
		SCORE_ACTION_REMOVE = lookupEnumSilent(SCORE_ACTION_ENUM, "REMOVE");

		TEAM_PACKET = getNMSClass("PacketPlayOutScoreboardTeam", "net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam");

		if (atLeast1_17) {
			TEAM_PARAMETERS = TEAM_PACKET.getDeclaredClasses()[0];

			CHAT_FORMAT_ENUM = getNMSClass("EnumChatFormat", "net.minecraft.EnumChatFormat").asSubclass(Enum.class);
			CHAT_FORMAT_RESET = lookupEnumSilent(CHAT_FORMAT_ENUM, "RESET");
		}

		RENDERTYPE_ENUM = getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay",
				"net.minecraft.world.scores.criteria.IScoreboardCriteria$EnumScoreboardHealthDisplay").asSubclass(Enum.class);
		RENDERTYPE_INTEGER = lookupEnumSilent(RENDERTYPE_ENUM, "INTEGER");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private final static List<PacketScoreboard> registeredBoards = new ArrayList<>();

	/**
	 * Clears registered boards, usually called on reload
	 */
	public static final void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * Removes all scoreboard for a player
	 *
	 * @param player
	 */
	public static final void clearBoardsFor(final Player player) {
		for (final PacketScoreboard scoreboard : registeredBoards)
			if (scoreboard.isViewing(player))
				scoreboard.hide(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stored scoreboard lines
	 */
	@Getter
	private final List<String> rows = new ArrayList<>();

	/**
	 * A list of viewed scoreboards
	 */
	private final StrictList<ViewedScoreboard> scoreboards = new StrictList<>();

	/**
	 * Should we update this scoreboard asynchronously?
	 */
	@Getter
	private final boolean async;

	/**
	 * The title of this scoreboard
	 */
	@Getter
	private String defaultTitle;

	/**
	 * Create a new scoreboard
	 */
	public PacketScoreboard(final boolean async) {
		this.async = async;

		registeredBoards.add(this);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Add new rows - they will be put onto the scoreboards on #updateScoreboards()
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final String... entries) {
		addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final List<String> entries) {
		rows.addAll(Common.colorize(entries));
	}

	/**
	 * Remove all rows
	 */
	public final void clearRows() {
		rows.clear();
	}

	/**
	 * Remove row at the given index
	 *
	 * @param index
	 */
	public final void removeRow(final int index) {
		rows.remove(index);
	}

	/**
	 * Remove row that contains the given text
	 *
	 * @param thatContains
	 */
	public final void removeRow(final String thatContains) {
		rows.removeIf(row -> row.contains(thatContains));
	}

	/**
	 * Updates the scoreboard title for the given player
	 *
	 * @param player
	 * @param title
	 */
	public final void setTitleFor(final Player player, final String title) {
		if (MinecraftVersion.olderThan(V.v1_13))
			Valid.checkBoolean(title.length() <= 32, "Title " + title + " is longer than 32 characters for " + player.getName());

		for (final ViewedScoreboard board : scoreboards)
			if (board.getViewer().equals(player)) {
				board.setTitle(Common.colorize(title));
				board.updateObjective(2);
			}
	}

	/**
	 * Update the title across all the viewed scoreboards
	 *
	 * @param title
	 */
	public void setTitle(final String title) {
		if (MinecraftVersion.olderThan(V.v1_13))
			Valid.checkBoolean(title.length() <= 32, "Title " + title + " is longer than 32 characters");

		defaultTitle = Common.colorize(title);

		for (final ViewedScoreboard board : scoreboards) {
			board.setTitle(defaultTitle);
			board.updateObjective(2);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Update / Stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Updates the scoreboard of the viewers with the current rows, replacing variables in them
	 */
	public final void updateScoreboards() {
		final BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				for (final ViewedScoreboard board : scoreboards) {
					try {
						// Replace the variables in the rows
						final List<String> playerRows = new ArrayList<>();

						for (final String row : rows) {
							final List<String> newRows = Common.colorize(replaceVariables(row, board.getViewer()));

							if (MinecraftVersion.olderThan(V.v1_13))
								for (final String newRow : newRows)
									Valid.checkBoolean(newRow != null && newRow.length() <= 32, "Line (" + newRow + ") is null or longer than 32!");

							playerRows.addAll(newRows);
						}

						Valid.checkBoolean(playerRows.size() <= 16, "New lines' size is greater than 16! Lines: " + String.join(" ", playerRows));

						// Get the size of the scoreboard's internal rows because we haven't updated them yet
						final int oldRowsSize = board.getRows().size();

						// Update the rows list
						for (int i = 0; i < playerRows.size(); i++) {
							final String row = Common.colorize(playerRows.get(i));

							if (i < oldRowsSize) {
								// Update the already existing line, if the old one != the new one
								if (!board.getRows().get(i).equals(row)) {
									board.getRows().set(i, row);
									board.updateRow(i, 2);
								}

							} else {
								// Or create new one
								board.getRows().add(row);
								board.updateRow(i, 0);
							}

							board.updateScore(i, ScoreAction.CHANGE, playerRows.size());
						}

						// If old lines' size was greater than the new lines' one, then remove the unused lines
						if (oldRowsSize > playerRows.size())
							for (int i = oldRowsSize - 1; i >= playerRows.size(); i--) {
								board.updateRow(i, 1);
								board.updateScore(i, ScoreAction.REMOVE, oldRowsSize);

								board.getRows().remove(i);
							}

					} catch (final Throwable t) {
						final String lines = String.join(" ", rows);

						Common.error(t,
								"Error displaying scoreboard for " + board.getViewer().getName(),
								"Entries: " + lines,
								"%error",
								"Stopping rendering for safety.");

						stop();
						break;
					}
				}
			}
		};

		if (async)
			runnable.runTaskAsynchronously(SimplePlugin.getInstance());
		else
			runnable.runTask(SimplePlugin.getInstance());
	}

	/**
	 * Returns the new row(s) with the variables replaced in the original row for the player
	 *
	 * @param player
	 * @return
	 */
	protected List<String> replaceVariables(final String row, final Player player) {
		return Collections.singletonList(row);
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {
		for (final Iterator<ViewedScoreboard> iterator = scoreboards.iterator(); iterator.hasNext(); ) {
			final ViewedScoreboard board = iterator.next();

			for (int i = 0; i < board.getRows().size(); i++) {
				board.updateRow(i, 1);
				board.updateScore(i, ScoreAction.REMOVE, board.getRows().size());
			}

			board.updateObjective(1);
			iterator.remove();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		Valid.checkBoolean(!isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + getDefaultTitle());

		scoreboards.add(new ViewedScoreboard(player, defaultTitle));
	}

	/**
	 * Hide this scoreboard from the player
	 *
	 * @param player
	 */
	public final void hide(final Player player) {
		Valid.checkBoolean(isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + getDefaultTitle());

		for (final Iterator<ViewedScoreboard> iterator = scoreboards.iterator(); iterator.hasNext(); ) {
			final ViewedScoreboard viewed = iterator.next();

			if (viewed.getViewer().equals(player)) {
				for (int i = 0; i < viewed.getRows().size(); i++) {
					viewed.updateRow(i, 1);
					viewed.updateScore(i, ScoreAction.REMOVE, viewed.getRows().size());
				}

				viewed.updateObjective(1);
				iterator.remove();

				break;
			}
		}
	}

	/**
	 * Returns true if the given player is viewing the scoreboard
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		for (final ViewedScoreboard viewed : scoreboards)
			if (viewed.getViewer().equals(player))
				return true;

		return false;
	}

	@Override
	public final String toString() {
		return "Scoreboard{title=" + getDefaultTitle() + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stores a viewed scoreboard per player
	 */
	private static class ViewedScoreboard {

		/**
		 * The viewer
		 */
		@Getter
		private final Player viewer;

		/**
		 * The objective
		 */
		private final String objectiveID;

		/**
		 * The internal title
		 */
		@Setter
		private String title;

		/**
		 * The internal rows
		 */
		@Getter
		private final List<String> rows = new ArrayList<>();

		private ViewedScoreboard(final Player viewer, final String title) {
			this.viewer = viewer;
			this.title = title;
			this.objectiveID = viewer.getName();

			updateObjective(0);
		}

		// ---------------------------------------------------------------
		// Packet Factory
		// ---------------------------------------------------------------

		/**
		 * Create or remove the objective, or update its display name
		 */
		private void updateObjective(final int mode) {
			final Object packet = newInstance(OBJECTIVE_PACKET);

			setDeclaredField(packet, String.class, 0, objectiveID);
			setDeclaredField(packet, int.class, MinecraftVersion.atLeast(V.v1_17) ? 3 : 0, mode);

			if (mode != 1) {
				if (title != null)
					Valid.checkBoolean(title.length() <= 32, "Title (" + title + ") size is greater than 32");

				setChatComponentField(packet, 1, Common.getOrEmpty(title));

				if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_8))
					setDeclaredField(packet, RENDERTYPE_ENUM, 0, RENDERTYPE_INTEGER);
			}

			Remain.sendPacket(viewer, packet);

			// If creating the objective, send a display packet to show the scoreboard to the player
			if (mode == 0) {
				final Object displayPacket = newInstance(DISPLAY_OBJECTIVE_PACKET);

				setDeclaredField(displayPacket, int.class, 0, 1);
				setDeclaredField(displayPacket, String.class, 0, objectiveID);

				Remain.sendPacket(viewer, displayPacket);
			}
		}

		/**
		 * Update the score for the row at the index
		 *
		 * @param index
		 * @param action
		 * @param size
		 */
		private void updateScore(final int index, final ScoreAction action, final int size) {
			final Object packet = newInstance(SCORE_PACKET);

			setDeclaredField(packet, String.class, 0, CHAT_COLORS[index]);
			setDeclaredField(packet, SCORE_ACTION_ENUM, 0, action == ScoreAction.CHANGE ? SCORE_ACTION_CHANGE : SCORE_ACTION_REMOVE);

			if (action == ScoreAction.CHANGE) {
				setDeclaredField(packet, String.class, 1, objectiveID);
				setDeclaredField(packet, int.class, 0, size - 1 - index);
			}

			Remain.sendPacket(viewer, packet);
		}

		/**
		 * Update the row at the index by sending a team packet with the updated team
		 *
		 * @param index
		 * @param mode
		 */
		private void updateRow(final int index, final int mode) {
			final Object packet = newInstance(TEAM_PACKET);

			setDeclaredField(packet, String.class, 0, viewer.getName() + "-" + index);
			setDeclaredField(packet, int.class, MinecraftVersion.atLeast(V.v1_13) ? MinecraftVersion.atLeast(V.v1_17) ? 7 : 0 : 1, mode);

			if (mode != 1) {
				final String row = this.rows.get(index);
				String prefix;
				String suffix = "";

				if (row.isEmpty())
					prefix = CHAT_COLORS[index];

				else if (row.length() <= 16)
					prefix = row;

				else {
					// Don't split at a color code
					final int splitIndex = row.charAt(15) == ChatColor.COLOR_CHAR ? 15 : 16;
					prefix = row.substring(0, splitIndex);
					suffix = row.substring(splitIndex);

					final String lastColors = ChatColor.getLastColors(prefix);
					ChatColor firstColor = null;

					// Get the suffix's first color
					if (suffix.length() >= 2 && suffix.charAt(0) == ChatColor.COLOR_CHAR)
						firstColor = ChatColor.getByChar(suffix.charAt(1));

					// Add prefix's last color if suffix's first color is format code (e.g. bold)
					final boolean addLastColor = firstColor == null || firstColor.isFormat();

					suffix = (addLastColor ? (lastColors.isEmpty() ? ChatColor.RESET.toString() : lastColors) : "") + suffix;
				}

				if (MinecraftVersion.olderThan(V.v1_13)) {
					if (prefix.length() > 16)
						prefix = prefix.substring(0, 16);

					if (suffix.length() > 16)
						suffix = suffix.substring(0, 16);
				}

				// Update the NMS team with the new prefix and suffix
				if (MinecraftVersion.atLeast(V.v1_17)) {
					final Object parameters = newInstance(TEAM_PARAMETERS);

					setChatComponentField(parameters, 0, "");
					setChatComponentField(parameters, 1, prefix);
					setChatComponentField(parameters, 2, suffix);
					setDeclaredField(parameters, String.class, 0, "always");
					setDeclaredField(parameters, String.class, 1, "always");
					setDeclaredField(parameters, CHAT_FORMAT_ENUM, 0, CHAT_FORMAT_RESET);

					setDeclaredField(packet, Optional.class, 0, Optional.of(parameters));

				} else {
					setChatComponentField(packet, 2, prefix);
					setChatComponentField(packet, 3, suffix);
				}

				if (mode == 0)
					setDeclaredField(packet, Collection.class, 0, Collections.singletonList(CHAT_COLORS[index]));
			}

			Remain.sendPacket(viewer, packet);
		}
	}

	/*
	 * Wrapper method for creating a new instance of the given class compatible with all versions
	 */
	private static Object newInstance(final Class<?> clazz) {
		return MinecraftVersion.atLeast(V.v1_17) ? invoke(ALLOCATE_INSTANCE, UNSAFE, clazz) : instantiate(clazz);
	}

	/**
	 * A compatibility enum for NMS score action enum
	 */
	private enum ScoreAction {
		CHANGE,
		REMOVE
	}
}