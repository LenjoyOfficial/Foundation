package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

import lombok.RequiredArgsConstructor;

/**
 * Represents a chat message surrounded by chat-wide line on the top and bottom:
 * <p>
 * -----------------------------------
 * Hello this is a test!
 * -----------------------------------
 * <p>
 * You can also specify \<center\> in front of the text to center it.
 */
public final class BoxedMessage {

	/**
	 * All message recipients
	 */
	private final Iterable<? extends CommandSender> recipients;

	/**
	 * The sender of the message
	 */
	private final Player sender;

	/**
	 * The color of the frame
	 */
	private final ChatColor frameColor;

	/**
	 * The messages to send
	 */
	private final String[] messages;

	/**
	 * Create a new boxed message from the given messages
	 * without sending it to any player
	 *
	 * @param messages
	 */
	public BoxedMessage(final String... messages) {
		this(ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Creates a new boxed message from the given frame color and messages
	 *
	 * @param frameColor
	 * @param messages
	 */
	public BoxedMessage(final ChatColor frameColor, final String... messages) {
		this(null, null, frameColor, messages);
	}

	/**
	 * Create a new boxed message
	 *
	 * @param recipients
	 * @param sender
	 * @param messages
	 */
	private BoxedMessage(final Iterable<? extends CommandSender> recipients, final Player sender, final ChatColor frameColor, final String[] messages) {
		this.recipients = recipients == null ? null : Common.toList(recipients); // Make a copy to prevent changes in the list on send
		this.sender = sender;
		this.messages = messages;
		this.frameColor = frameColor;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helper methods
	// ------------------------------------------------------------------------------------------------------------

	private void launch() {
		Common.runLater(2, () -> {
			final String oldTellPrefix = Common.getTellPrefix();
			Common.setTellPrefix("");

			this.sendFrame();

			Common.setTellPrefix(oldTellPrefix);
		});
	}

	private void sendFrame() {
		this.sendLine();
		this.sendFrameInternals0();
		this.sendLine();
	}

	private void sendFrameInternals0() {
		for (final String message : messages)
			for (final String part : message.split("\n"))
				send(part);
	}

	private void sendLine() {
		send(frameColor + Common.chatLineSmooth());
	}

	private void send(String message) {
		message = this.centerMessage0(message);

		if (this.recipients == null)
			this.broadcast0(message);

		else
			this.tell0(message);
	}

	private String centerMessage0(String message) {
		if (message.startsWith("<center>"))
			return ChatUtil.center(message.replaceFirst("<center>(\\s|)", ""));

		return message;
	}

	private void broadcast0(String message) {
		if (this.sender != null)
			Common.broadcast(message, this.sender);
		else
			Common.broadcastTo(Remain.getOnlinePlayers(), message);
	}

	private void tell0(String message) {
		if (this.sender != null)
			message = message.replace("{player}", Common.resolveSenderName(this.sender));

		Common.broadcastTo(this.recipients, message);
	}

	/**
	 * Finds the given variables (you do not need to put {} brackets, we put them there)
	 * and replaces them with instances
	 *
	 * @param variables
	 * @return
	 */
	public Replacer find(final String... variables) {
		return new Replacer(variables);
	}

	public String getMessage() {
		return this.messages.length == 0 ? "" : String.join("\n", this.messages);
	}

	@Override
	public String toString() {
		return "Boxed{" + String.join(", ", this.messages) + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast this message to everyone on the message
	 */
	public void broadcast() {
		broadcast(null, frameColor, messages);
	}

	/**
	 * Broadcast this message to all players as the sender,
	 * replacing {player} with the sender name
	 *
	 * @param sender
	 */
	public void broadcastAs(final Player sender) {
		new BoxedMessage(null, sender, frameColor, messages).launch();
	}

	/**
	 * Sends this message to the recipient
	 *
	 * @param recipient
	 */
	public void tell(final CommandSender recipient) {
		tell(null, Arrays.asList(recipient), frameColor, messages);
	}

	/**
	 * Sends this message to given recipients
	 *
	 * @param recipients
	 */
	public void tell(final Iterable<? extends CommandSender> recipients) {
		tell(null, recipients, frameColor, messages);
	}

	/**
	 * Sends this message to the given recipient
	 * replacing {player} with the sender name
	 *
	 * @param receiver
	 * @param sender
	 */
	public void tellAs(final CommandSender receiver, final Player sender) {
		tell(sender, Arrays.asList(receiver), frameColor, messages);
	}

	/**
	 * Sends this message to the given recipients
	 * replacing {player} with the sender name
	 *
	 * @param receivers
	 * @param sender
	 */
	public void tellAs(final Iterable<? extends CommandSender> receivers, final Player sender) {
		new BoxedMessage(receivers, sender, frameColor, messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Send this message to everyone
	 *
	 * @param messages
	 */
	public static void broadcast(final String... messages) {
		broadcast(ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Send this message to everyone with a custom frame color
	 *
	 * @param messages
	 */
	public static void broadcast(final ChatColor frameColor, final String... messages) {
		broadcast(null, frameColor, messages);
	}

	/**
	 * Sends this message to the all players as the sender
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(final Player sender, final String... messages) {
		broadcast(sender, ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Sends this message to the all players as the sender with a custom frame color
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(final Player sender, final ChatColor frameColor, final String... messages) {
		new BoxedMessage(null, sender, frameColor, messages).launch();
	}

	/**
	 * Sends the message to the recipient
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(final CommandSender recipient, final String... messages) {
		tell(recipient, ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Sends the message to the recipient with a custom frame color
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(final CommandSender recipient, final ChatColor frameColor, final String... messages) {
		tell(Arrays.asList(recipient), frameColor, messages);
	}

	/**
	 * Sends the message to the given recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void tell(final Iterable<? extends CommandSender> recipients, final String... messages) {
		tell(recipients, ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Sends the message to the given recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void tell(final Iterable<? extends CommandSender> recipients, final ChatColor frameColor, final String... messages) {
		tell(null, recipients, frameColor, messages);
	}

	/**
	 * Sends this message to a recipient as sender
	 *
	 * @param sender
	 * @param receiver
	 * @param messages
	 */
	public static void tell(final Player sender, final CommandSender receiver, final String... messages) {
		tell(sender, receiver, ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Sends this message to a recipient as sender
	 *
	 * @param sender
	 * @param receiver
	 * @param messages
	 */
	public static void tell(final Player sender, final CommandSender receiver, final ChatColor frameColor, final String... messages) {
		tell(sender, Collections.singletonList(receiver), frameColor, messages);
	}

	/**
	 * Sends this message to recipients as sender
	 *
	 * @param sender
	 * @param receivers
	 * @param messages
	 */
	public static void tell(final Player sender, final Iterable<? extends CommandSender> receivers, final String... messages) {
		tell(sender, receivers, ChatColor.DARK_GRAY, messages);
	}

	/**
	 * Sends this message to recipients as sender with a custom frame color
	 *
	 * @param sender
	 * @param receivers
	 * @param messages
	 */
	public static void tell(final Player sender, final Iterable<? extends CommandSender> receivers, final ChatColor frameColor, final String... messages) {
		new BoxedMessage(receivers, sender, frameColor, messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacer
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Utility class for quickly replacing variables
	 */
	@RequiredArgsConstructor
	public class Replacer {

		/**
		 * The placeholder names to replace
		 */
		private final String[] variables;

		/**
		 * Replace the variables we store with the given object replacements
		 *
		 * @param replacements
		 * @return
		 */
		public final BoxedMessage replace(Object... replacements) {
			String message = String.join("%delimiter%", BoxedMessage.this.messages);

			for (int i = 0; i < this.variables.length; i++) {
				String find = this.variables[i];

				{ // Auto insert brackets
					if (!find.startsWith("{"))
						find = "{" + find;

					if (!find.endsWith("}"))
						find = find + "}";
				}

				final Object rep = i < replacements.length ? replacements[i] : null;

				message = message.replace(find, rep != null ? Objects.toString(rep) : "");
			}

			final String[] copy = message.split("%delimiter%");

			return new BoxedMessage(recipients, sender, frameColor, copy);
		}
	}
}