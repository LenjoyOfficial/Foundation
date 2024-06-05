package org.mineacademy.fo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.Common;
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

	// Legacy
	private static final int EXTENDED_BIT = 64;
	private static final int POTION_BIT = 15;
	private static final int SPLASH_BIT = 16384;
	private static final int TIER_BIT = 32;
	private static final int TIER_SHIFT = 5;
	private static final int NAME_BIT = 63;

	private static final Map<String, String> FIX_LONG_STRONG_POTIONS = new HashMap<String, String>() {{
		put("LONG_SPEED", "LONG_SWIFTNESS");
		put("STRONG_SPEED", "STRONG_SWIFTNESS");
		put("LONG_REGEN", "LONG_REGENERATION");
		put("STRONG_REGEN", "STRONG_REGENERATION");
		put("LONG_JUMP", "LONG_LEAPING");
		put("STRONG_JUMP", "STRONG_LEAPING");
		put("STRONG_INSTANT_HEAL", "STRONG_HEALING");
		put("STRONG_INSTANT_DAMAGE", "STRONG_HARMING");
	}};

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

			// Extended and upgraded PotionTypes are separated now
			if (MinecraftVersion.atLeast(V.v1_20)) {
				String string = (upgraded ? "STRONG_" : extended ? "LONG_" : "") + type;

				string = FIX_LONG_STRONG_POTIONS.getOrDefault(string, string);
				potionMeta.setBasePotionType(PotionType.valueOf(string));

			} else {
				final org.bukkit.potion.PotionData data = new org.bukkit.potion.PotionData(type, extended, upgraded);
				potionMeta.setBasePotionData(data);
			}

			item.setItemMeta(potionMeta);

		} else {
			short damage;
			final int level = upgraded ? 2 : 1;

			if (this.type == PotionType.WATER) {
				damage = 0;

			} else {
				damage = (short) (level - 1);
				damage = (short)(damage << 5);
				damage |= (short) (this.type.ordinal() > 6 ? this.type.ordinal() + 1 : this.type.ordinal()); // potion damage value

				if (this.splash) {
					damage = (short)(damage | SPLASH_BIT);
				}

				if (this.extended) {
					damage = (short)(damage | EXTENDED_BIT);
				}
			}

			item.setDurability(damage);
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

		final PotionType type;
		final boolean splash;
		final boolean extended;
		final boolean upgraded;

		if (MinecraftVersion.atLeast(V.v1_9)) {
			final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

			// Only check here for ItemMeta for compatibility below 1.4.7
			Valid.checkBoolean(meta instanceof org.bukkit.inventory.meta.PotionMeta, "Can only apply potion for items with PotionMeta not: " + meta);

			final org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
			splash = CompMaterial.fromItem(item) == CompMaterial.SPLASH_POTION;

			// Extended and upgraded PotionTypes are separated now
			if (MinecraftVersion.atLeast(V.v1_20)) {
				type = potionMeta.getBasePotionType();
				final String string = type.toString();

				extended = string.startsWith("LONG_");
				upgraded = string.startsWith("STRONG_");

			} else {
				final org.bukkit.potion.PotionData potionData = potionMeta.getBasePotionData();

				type = potionData.getType();
				extended = potionData.isExtended();
				upgraded = potionData.isUpgraded();
			}

		} else {
			final int damage = item.getDurability();
			final int ordinal = (damage & POTION_BIT) > 6 ? (damage & POTION_BIT) - 1 : damage & POTION_BIT;
			boolean upgr = false;

			type = PotionType.values()[ordinal];
			splash = (damage & SPLASH_BIT) > 0;

			if (type != null && type != PotionType.WATER) {
				final int level = ((damage & TIER_BIT) >> TIER_SHIFT) + 1;
				upgr = level > 1;
			}

			upgraded = upgr;
			extended = (type == null || !type.isInstant()) && (damage & EXTENDED_BIT) > 0;
		}

		return new SimplePotionData(type, splash, extended, upgraded);
	}
}
