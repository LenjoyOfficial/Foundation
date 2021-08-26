package org.mineacademy.fo.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.NonNull;

/**
 * Compatibility class to support both new {@link org.bukkit.potion.PotionData}
 * and old {@link org.bukkit.potion.Potion} for potions' main data
 */
@Getter
public final class SimplePotionData {
	/**
	 * The type of the potion
	 */
	@NonNull
	private final PotionType type;

	/**
	 * Is this a splash potion?
	 */
	private final boolean splash;

	/**
	 * Does this potion have extended duration?
	 */
	private final boolean extended;

	/**
	 * Is this potion upgraded to level 2?
	 */
	private final boolean upgraded;

	/**
	 * Construct a new potion data, throwing an exception if both extended and
	 * upgraded are true
	 *
	 * @param type
	 * @param splash
	 * @param extended
	 * @param upgraded
	 */
	public SimplePotionData(@NonNull final PotionType type, final boolean splash, final boolean extended, final boolean upgraded) {
		Valid.checkBoolean(!(extended && upgraded), (splash ? "Splash potion " : "Potion ") + type + " cannot be both extended and upgraded");

		this.type = type;
		this.splash = splash;
		this.extended = extended;
		this.upgraded = upgraded;
	}

	/**
	 * Apply this potion data to the given item
	 *
	 * @param item
	 */
	public void apply(final ItemStack item) {
		if (MinecraftVersion.atLeast(V.v1_9)) {
			final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

			// Only check here for ItemMeta for compatibility below 1.4.7
			Valid.checkBoolean(meta instanceof org.bukkit.inventory.meta.PotionMeta, "Can only apply potion for items with PotionMeta not: " + meta);

			if (splash)
				CompMaterial.SPLASH_POTION.setType(item);

			final org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
			final org.bukkit.potion.PotionData data = new org.bukkit.potion.PotionData(type, extended, upgraded);

			potionMeta.setBasePotionData(data);

			item.setItemMeta(potionMeta);

		} else {
			final Potion data = new Potion(type, upgraded ? 1 : 2, splash, extended);

			data.apply(item);
		}
	}

	/**
	 * Constructs a new CompPotionData from the given item.
	 *
	 * @param item
	 * @return a CompPotionData from the item, or null if item is null
	 */
	public static SimplePotionData ofItem(ItemStack item) {
		if (item == null)
			return null;

		final boolean splash;

		final PotionType type;
		final boolean extended;
		final boolean upgraded;

		if (MinecraftVersion.atLeast(V.v1_9)) {
			final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

			// Only check here for ItemMeta for compatibility below 1.4.7
			Valid.checkBoolean(meta instanceof org.bukkit.inventory.meta.PotionMeta, "Can only apply potion for items with PotionMeta not: " + meta);

			final org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
			final org.bukkit.potion.PotionData potionData = potionMeta.getBasePotionData();

			splash = CompMaterial.fromItem(item) == CompMaterial.SPLASH_POTION;
			type = potionData.getType();
			extended = potionData.isExtended();
			upgraded = potionData.isUpgraded();

		} else {
			final Potion potionData = Potion.fromItemStack(item);

			splash = potionData.isSplash();
			type = potionData.getType();
			extended = potionData.hasExtendedDuration();
			upgraded = potionData.getLevel() == 2;
		}

		return new SimplePotionData(type, splash, extended, upgraded);
	}
}
