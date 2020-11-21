package org.mineacademy.fo.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.remain.CompRunnable;
import org.mineacademy.fo.remain.Remain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a displayable scoreboard for the players. It is working with packets, so you get absolute no lag
 * when using it.
 * <p>
 * Based on FastBoard by MrMicky
 *
 * @author MrMicky, Lenjoy
 */
public class SimpleScoreboard {

	// ------------------------------------------------------------------------------------------------------------
	// NMS constructors, methods and fields
	// ------------------------------------------------------------------------------------------------------------

	// Scoreboard
	private static final Method SB_GET_HANDLE;

	// Team
	private static final Constructor<?> TEAM_CONSTRUCTOR;
	private static final Method SET_PREFIX;
	private static final Method SET_SUFFIX;

	private static final Constructor<?> OBJECTIVE_PACKET;
	private static final Constructor<?> DISPLAY_OBJECTIVE_PACKET;
	private static final Constructor<?> SCORE_PACKET;
	private static final Constructor<?> TEAM_PACKET;

	// Score
	private static final Class<? extends Enum> SCORE_ACTION_ENUM;

	private static final Object SCORE_ACTION_CHANGE;
	private static final Object SCORE_ACTION_REMOVE;

	// Render type
	private static final Class<? extends Enum> RENDERTYPE_ENUM;

	private static final Object RENDERTYPE_INTEGER;

	static {
		SB_GET_HANDLE = ReflectionUtil.getMethod(ReflectionUtil.getOBCClass("scoreboard.CraftScoreboard"), "getHandle");

		final Class<?> teamClass = ReflectionUtil.getNMSClass("ScoreboardTeam");
		TEAM_CONSTRUCTOR = ReflectionUtil.getConstructor(teamClass, ReflectionUtil.getNMSClass("Scoreboard"), String.class);
		SET_PREFIX = ReflectionUtil.getMethod(teamClass, "setPrefix", MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? ReflectionUtil.CHAT_COMPONENT_CLASS : String.class);
		SET_SUFFIX = ReflectionUtil.getMethod(teamClass, "setSuffix", MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? ReflectionUtil.CHAT_COMPONENT_CLASS : String.class);

		OBJECTIVE_PACKET = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardObjective"));
		DISPLAY_OBJECTIVE_PACKET = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardDisplayObjective"));

		final Class<?> scorePacketClass = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore");
		SCORE_PACKET = ReflectionUtil.getConstructor(scorePacketClass);
		SCORE_ACTION_ENUM = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? ReflectionUtil.getNMSClass("ScoreboardServer$Action").asSubclass(Enum.class) :
				scorePacketClass.getDeclaredClasses()[0].asSubclass(Enum.class);

		SCORE_ACTION_CHANGE = ReflectionUtil.lookupEnumSilent(SCORE_ACTION_ENUM, "CHANGE");
		SCORE_ACTION_REMOVE = ReflectionUtil.lookupEnumSilent(SCORE_ACTION_ENUM, "REMOVE");

		TEAM_PACKET = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardTeam"), teamClass, int.class);

		RENDERTYPE_ENUM = ReflectionUtil.getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay").asSubclass(Enum.class);
		RENDERTYPE_INTEGER = ReflectionUtil.lookupEnumSilent(RENDERTYPE_ENUM, "INTEGER");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private final static List<SimpleScoreboard> registeredBoards = new ArrayList<>();

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
		for (final SimpleScoreboard scoreboard : registeredBoards)
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
	private String title;

	/**
	 * Create a new scoreboard
	 */
	public SimpleScoreboard(final boolean async) {
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
		rows.addAll(entries);
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
	public final void updateTitleFor(final Player player, final String title) {
		if (MinecraftVersion.olderThan(V.v1_13))
			Valid.checkBoolean(title.length() <= 32, "Title " + title + " is longer than 32 characters for " + player.getName());

		for (final ViewedScoreboard board : scoreboards)
			if (board.getViewer().equals(player)) {
				board.setInternalTitle(title);
				board.updateObjective(2);
			}
	}

	/**
	 * Make a static title across all the viewed scoreboards
	 *
	 * @param title
	 */
	public void setStaticTitle(final String title) {
		if (MinecraftVersion.olderThan(V.v1_13))
			Valid.checkBoolean(title.length() <= 32, "Title " + title + " is longer than 32 characters");

		for (final ViewedScoreboard board : scoreboards) {
			board.setInternalTitle(title);
			board.updateObjective(2);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Update / Stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Updates this scoreboard
	 */
	public final void updateScoreboards() {
		final CompRunnable runnable = new CompRunnable() {
			@Override
			public void run() {

				for (final ViewedScoreboard board : scoreboards) {
					try {
						// Replace the variables in the rows
						final List<String> playerRows = new ArrayList<>();

						for (final String row : rows) {
							final List<String> newLines = replaceVariables(row, board.getViewer());

							if (MinecraftVersion.olderThan(V.v1_13))
								for (final String newLine : newLines)
									Valid.checkBoolean(newLine != null && newLine.length() <= 32, "Line (" + newLine + ") is null or longer than 32!");

							playerRows.addAll(newLines);
						}

						Valid.checkBoolean(playerRows.size() <= 16, "New lines' size is greater than 16! Lines: " + String.join(" ", playerRows));

						// Get the size of the scoreboard's internal lines because we haven't updated them yet
						final int oldRowsSize = board.getRows().size();

						// Update the lines list
						for (int i = 0; i < playerRows.size(); i++) {
							final String row = playerRows.get(i);

							if (i < oldRowsSize) {
								// Update the already existing line, if the old one != the new one
								if (!board.getRows().get(i).getLine().equals(row)) {
									board.getRows().get(i).setLine(Common.colorize(row), i);
									board.updateRow(i, 2);
								}

							} else {
								// Or create new one
								board.getRows().add(board.new ScoreboardRow(Common.colorize(row), i));
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
			Common.runAsync(runnable);
		else
			Common.runLater(runnable);
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
		Valid.checkBoolean(!isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + getTitle());

		scoreboards.add(new ViewedScoreboard(player));
	}

	/**
	 * Hide this scoreboard from the player
	 *
	 * @param player
	 */
	public final void hide(final Player player) {
		Valid.checkBoolean(isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + getTitle());

		for (final ViewedScoreboard viewed : scoreboards)
			if (viewed.getViewer().equals(player)) {
				for (int i = 0; i < viewed.getRows().size(); i++) {
					viewed.updateRow(i, 1);
					viewed.updateScore(i, ScoreAction.REMOVE, viewed.getRows().size());
				}

				viewed.updateObjective(1);
				scoreboards.remove(viewed);

				break;
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
		return "Scoreboard{title=" + getTitle() + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stores a viewed scoreboard per player
	 */
	@Getter
	@Setter
	private static class ViewedScoreboard {

		/**
		 * The viewer
		 */
		private final Player viewer;

		/**
		 * The scoreboard
		 */
		private final Object scoreboard;

		/**
		 * The objective
		 */
		private final String objectiveID;

		/**
		 * The internal title
		 */
		@Setter(value = AccessLevel.PRIVATE)
		private String internalTitle;

		/**
		 * The internal rows
		 */
		private final List<ScoreboardRow> rows = new LinkedList<>();

		private ViewedScoreboard(final Player viewer) {
			this.viewer = viewer;
			this.scoreboard = ReflectionUtil.invoke(SB_GET_HANDLE, viewer.getScoreboard());

			this.objectiveID = viewer.getName();

			updateObjective(0);
		}

		// ---------------------------------------------------------------
		// Packet Factory
		// ---------------------------------------------------------------

		/**
		 * Create or remove the objective, or update it's display name
		 */
		private void updateObjective(final int mode) {
			final Object packet = ReflectionUtil.instantiate(OBJECTIVE_PACKET);

			ReflectionUtil.setDeclaredField(packet, String.class, 0, objectiveID);
			ReflectionUtil.setDeclaredField(packet, int.class, 0, mode);

			if (mode != 1) {
				if (internalTitle != null)
					Valid.checkBoolean(internalTitle.length() <= 32, "Title (" + internalTitle + ") size is greater than 32");

				ReflectionUtil.setChatComponentField(packet, 1, Common.getOrEmpty(internalTitle));

				if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_8))
					ReflectionUtil.setDeclaredField(packet, RENDERTYPE_ENUM, 0, RENDERTYPE_INTEGER);
			}

			Remain.sendPacket(viewer, packet);

			// If creating the objective, send a display packet to show the scoreboard to the player
			if (mode == 0) {
				final Object displayPacket = ReflectionUtil.instantiate(DISPLAY_OBJECTIVE_PACKET);

				ReflectionUtil.setDeclaredField(displayPacket, int.class, 0, 1);
				ReflectionUtil.setDeclaredField(displayPacket, String.class, 0, objectiveID);

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
			final Object packet = ReflectionUtil.instantiate(SCORE_PACKET);

			ReflectionUtil.setDeclaredField(packet, String.class, 0, ChatColor.values()[index].toString());
			ReflectionUtil.setDeclaredField(packet, SCORE_ACTION_ENUM, 0, action == ScoreAction.CHANGE ? SCORE_ACTION_CHANGE : SCORE_ACTION_REMOVE);

			if (action == ScoreAction.CHANGE) {
				ReflectionUtil.setDeclaredField(packet, String.class, 1, objectiveID);
				ReflectionUtil.setDeclaredField(packet, int.class, 0, size - 1 - index);
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
			final Object packet = ReflectionUtil.instantiate(TEAM_PACKET, rows.get(index).getTeam(), mode);

			if (mode == 0)
				ReflectionUtil.setDeclaredField(packet, Collection.class, 0, Collections.singletonList(ChatColor.values()[index].toString()));

			Remain.sendPacket(viewer, packet);
		}

		/**
		 * A row for the scoreboard.
		 */
		private class ScoreboardRow {
			/**
			 * The NMS team needed for displaying
			 */
			@Getter
			private final Object team;

			/**
			 * The raw line this instance is representing
			 */
			@Getter
			private String line;

			private ScoreboardRow(final String line, final int index) {
				this.team = ReflectionUtil.instantiate(TEAM_CONSTRUCTOR, scoreboard, viewer.getName() + "-" + index);

				setLine(line, index);
			}

			/**
			 * Sets the row to the new value
			 *
			 * @param row
			 * @param index
			 */
			private void setLine(final String row, final int index) {
				this.line = row;

				String prefix;
				String suffix = "";

				if (row.isEmpty())
					prefix = ChatColor.values()[index].toString();

				else if (row.length() <= 16)
					prefix = row;

				else {
					// Don't split at a color code
					final int splitIndex = row.charAt(15) == ChatColor.COLOR_CHAR ? 15 : 16;
					prefix = row.substring(0, splitIndex);
					suffix = row.substring(splitIndex);

					final String lastColors = ChatColor.getLastColors(prefix);
					ChatColor firstColor = null;

					// Get the suffix's last color
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

				// Finally update the NMS team with the new prefix and suffix
				ReflectionUtil.invoke(SET_PREFIX, team, MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? Remain.toIChatBaseComponent(Remain.toJson(prefix)) : prefix);
				ReflectionUtil.invoke(SET_SUFFIX, team, MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? Remain.toIChatBaseComponent(Remain.toJson(suffix)) : suffix);
			}
		}
	}

	/**
	 * A little enum for compatibility
	 */
	private enum ScoreAction {
		CHANGE, REMOVE
	}
}