package org.mineacademy.fo.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The bukkit listener responsible for menus to function.
 */
public final class MenuListener implements Listener {

	private final Map<UUID, SwapData> cacheData = new HashMap<>();

	/**
	 * Handles closing menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMenuClose(final InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player))
			return;

		final Player player = (Player) event.getPlayer();
		final Menu menu = Menu.getMenu(player);
		if (menu != null) {
			menu.handleClose(event.getInventory());
			addItemsToPlayer(player);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMenuOpen(final InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player))
			return;
		if (event.getInventory().getType() == InventoryType.PLAYER)
			return;

		final Player player = (Player) event.getPlayer();
		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_14))
			Common.runLater(3, () -> {
				final Menu menu = Menu.getMenu(player);
				if (menu != null)
					cacheData.put(player.getUniqueId(), new SwapData(false, player.getInventory().getItemInOffHand()));
			});
	}

	/**
	 * Handles clicking in menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onMenuClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		final Player player = (Player) event.getWhoClicked();
		final Menu menu = Menu.getMenu(player);

		if (menu != null) {
			final ItemStack slotItem = event.getCurrentItem();
			final ItemStack cursor = event.getCursor();
			final Inventory clickedInv = Remain.getClickedInventory(event);

			final InventoryAction action = event.getAction();
			final MenuClickLocation whereClicked = clickedInv != null ? clickedInv.getType() == InventoryType.CHEST ? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY : MenuClickLocation.OUTSIDE;

			final boolean allowed = menu.isActionAllowed(whereClicked, event.getSlot(), slotItem, cursor);

			if (whereClicked == MenuClickLocation.MENU)
				try {
					final Button button = menu.getButton(slotItem);

					if (button != null)
						menu.onButtonClick(player, event.getSlot(), action, event.getClick(), button);
					else
						menu.onMenuClick(player, event.getSlot(), action, event.getClick(), cursor, slotItem, !allowed);

				} catch (final Throwable t) {
					Common.tell(player, "&cOups! There was a problem with this menu! Please contact the administrator to review the console for details.");
					player.closeInventory();

					Common.error(t, "Error clicking in menu " + menu);
				}

			if (!allowed) {
				event.setResult(Result.DENY);
				checkIfPlayerSwapItem(event, player);
				player.updateInventory();
			}
		}
	}

	private void checkIfPlayerSwapItem(InventoryClickEvent event, Player player) {

		if (event.getClick().toString().contains("SWAP_OFFHAND")) {
			SwapData data = cacheData.get(player.getUniqueId());
			ItemStack item = null;
			if (data != null) {
				item = data.getItemInOfBeforeOpenMenuHand();
			}
			cacheData.put(player.getUniqueId(), new SwapData(true, item));
		}
	}

	private void addItemsToPlayer(Player player) {

		SwapData data = cacheData.get(player.getUniqueId());
		if (data != null && data.isPlayerUseSwapoffhand())
			if (data.getItemInOfBeforeOpenMenuHand() != null && data.getItemInOfBeforeOpenMenuHand().getType() != Material.AIR)
				player.getInventory().setItemInOffHand(data.getItemInOfBeforeOpenMenuHand());
			else
				player.getInventory().setItemInOffHand(null);
		cacheData.remove(player.getUniqueId());
	}

	private static class SwapData {
		boolean playerUseSwapoffhand;
		ItemStack itemInOfBeforeOpenMenuHand;

		public SwapData(boolean playerUseSwapoffhand, ItemStack itemInOfBeforeOpenMenuHand) {
			this.playerUseSwapoffhand = playerUseSwapoffhand;
			this.itemInOfBeforeOpenMenuHand = itemInOfBeforeOpenMenuHand;
		}

		public boolean isPlayerUseSwapoffhand() {
			return playerUseSwapoffhand;
		}

		public ItemStack getItemInOfBeforeOpenMenuHand() {
			return itemInOfBeforeOpenMenuHand;
		}
	}
}
