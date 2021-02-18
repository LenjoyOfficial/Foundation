package org.mineacademy.fo.model;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.menu.model.InventoryDrawer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A special class for customizing different slots for items in a menu
 * <p>
 * Used in {@link org.mineacademy.fo.settings.YamlConfig}
 * <p>
 * Extend this class and write public methods that call {@link #fillInventory(InventoryDrawer, char, List)}
 * and {@link #addConstantItem(InventoryDrawer, char)}
 */
public class MenuTemplate {
	private final StrictMap<Character, Tuple<List<Integer>, ItemStack>> slots = new StrictMap<>();

	@Getter
	private final int inventorySize;

	/**
	 * Creates a new template from a serialized map
	 *
	 * @param of
	 */
	protected MenuTemplate(final SerializedMap of) {
		final List<String> layout = of.getStringList("Layout");
		final StrictMap<Character, SerializedMap> types = new StrictMap<>();

		int slot = 0;

		for (final String row : layout)
			for (final char item : row.toCharArray()) {
				if (item != ' ') {
					final SerializedMap itemMap = types.getOrPut(item, of.getMap(item + ""));

					if (!slots.contains(item)) {
						final ItemStack display = itemMap.containsKey("Display") ? InventoryItem.toItem(itemMap.getMap("Display")) : null;

						slots.put(item, new Tuple<>(Lists.newArrayList(slot++), display));
						continue;
					}

					slots.get(item).getKey().add(slot);
				}

				slot++;
			}

		// Rewriting slot lists to Arrays.asList lists for better performance
		for (final Map.Entry<Character, Tuple<List<Integer>, ItemStack>> type : slots.entrySet()) {
			final Tuple<List<Integer>, ItemStack> data = type.getValue();

			type.setValue(new Tuple<>(Arrays.asList(data.getKey().toArray(new Integer[0])), data.getValue()));
		}

		this.inventorySize = layout.size() * 9;
	}

	/**
	 * Returns the defined slots for the given type, or an empty list if the type is incorrect
	 *
	 * @param type
	 * @return
	 */
	public final List<Integer> getSlots(final char type) {
		final Tuple<List<Integer>, ItemStack> slots = this.slots.get(type);

		return slots != null ? slots.getKey() : new ArrayList<>();
	}

	/**
	 * Adds the defined item of the given type to the drawer, doing nothing if
	 * the type doesn't exist (It may be removed from the config layout)
	 *
	 * @param drawer
	 * @param type
	 */
	protected void addConstantItem(final InventoryDrawer drawer, final char type) {
		final Tuple<List<Integer>, ItemStack> item = slots.get(type);
		Valid.checkNotNull(item.getValue(), "Cannot add null item of slot type " + type + " to inventory");

		for (final Integer slot : item.getKey())
			drawer.setItem(slot, item.getValue());
	}

	/**
	 * Convenience method for scanning all of the slot types and adding the ones
	 * into the drawer that have a display item defined
	 * <p>
	 * Use this if the menu has a lot of constant types and you don't want to put them
	 * each in with {@link #addConstantItem(InventoryDrawer, char)}
	 *
	 * @param drawer
	 */
	public void addConstantItems(final InventoryDrawer drawer) {
		for (final Map.Entry<Character, Tuple<List<Integer>, ItemStack>> type : slots.entrySet())
			if (type.getValue().getValue() != null)
				addConstantItem(drawer, type.getKey());
	}

	/**
	 * Fills the inventory with the given items at the slots of the given type
	 *
	 * @param drawer
	 * @param type   the type of the slots
	 * @param items
	 */
	protected final void fillInventory(@NonNull final InventoryDrawer drawer, @NonNull final char type, @NonNull final List<ItemStack> items) {
		Valid.checkBoolean(slots.contains(type), "Slots " + slots.toString() + " doesn't contain " + type);

		final List<Integer> slots = this.slots.get(type).getKey();

		for (int i = 0; i < items.size(); i++) {
			final ItemStack item = items.get(i);

			if (item != null)
				drawer.setItem(slots.get(i), item);
		}
	}
}
