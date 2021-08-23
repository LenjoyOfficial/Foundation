package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.SkullCreator;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.SimpleEnchant;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * A utility class for storing and serializing itemstacks with preset inventory slots
 */
@AllArgsConstructor
public final class InventoryItem implements ConfigSerializable {
	@Getter
	@NonNull
	private final ItemStack item;
	private final int slot;

	private final boolean colorable;

	/**
	 * Adds the item stack to the slot in this instance in the given inventory.
	 *
	 * @param inventory
	 */
	public void addItem(final Inventory inventory) {
		Valid.checkNotNull(item, "Item for InventoryItem is null");
		Valid.checkBoolean(slot > -1, "Cannot add item " + item.getType() + " to inventory because slot is null");

		addItem(inventory, slot);
	}

	/**
	 * Adds the item to the given slot in the inventory
	 *
	 * @param inventory
	 * @param slot
	 */
	public void addItem(final Inventory inventory, final int slot) {
		Valid.checkNotNull(item, "Item for InventoryItem is null");

		if (inventory instanceof PlayerInventory) {
			final PlayerInventory pInventory = (PlayerInventory) inventory;

			switch (slot) {
				case 100:
					pInventory.setBoots(item);
					return;

				case 101:
					pInventory.setLeggings(item);
					return;

				case 102:
					pInventory.setChestplate(item);
					return;

				case 103:
					pInventory.setHelmet(item);
					return;
			}
		}

		inventory.setItem(slot, item);
	}

	/**
	 * Returns a new builder of this item with the replacer function applied to the name and lores
	 * <p>
	 * You can replace variables in a line of the lore with multiple lines joined with '\n'
	 *
	 * @param replacer
	 * @return
	 */
	public ItemCreator.ItemCreatorBuilder replaceVariables(final Function<String, String> replacer) {
		return replaceVariables(ItemCreator.of(item), replacer);
	}

	/**
	 * Colors this item and returns its builder, throwing an error if the item is not colorable
	 * but defined 'Colorable' as true
	 * <p>
	 * The replacer will be applied to the variables in the name and lores
	 * <p>
	 * You can replace variables in a line of the lore with multiple lines joined with '\n'
	 *
	 * @param color
	 * @param replacer
	 * @return the colored builder, or a simple builder of the item if it defined 'Colorable' as false or nothing
	 */
	public ItemCreator.ItemCreatorBuilder colorItem(@NonNull final CompColor color, final Function<String, String> replacer) {
		final CompMaterial material = CompMaterial.fromItem(item);
		final ItemCreator.ItemCreatorBuilder builder = ItemCreator.of(item);

		if (!colorable)
			return replaceVariables(builder, replacer);

		// Check if item's material is colorable
		final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_GLASS", "STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");
		boolean isColorable = false;

		for (final String colorable : colorableMaterials)
			if (material.toString().contains(colorable))
				isColorable = true;

		Valid.checkBoolean(isColorable, "Attempt to color non-colorable item " + material);

		return replaceVariables(builder.color(color), replacer);
	}

	/*
	 * Replaces the variables in the name and lores with the given replacements
	 */
	private ItemCreator.ItemCreatorBuilder replaceVariables(final ItemCreator.ItemCreatorBuilder builder, Function<String, String> replacer) {
		final ItemMeta meta = item.getItemMeta();

		// Initialize replacer
		if (replacer == null)
			replacer = Function.identity();

		if (meta != null) {
			if (meta.hasDisplayName())
				builder.name(replacer.apply(meta.getDisplayName()));

			if (meta.getLore() != null) {
				final String joined = replacer.apply(String.join("\n", meta.getLore()));

				// Replace the variables again if the replacements had placeholders in them
				final String[] split = StringUtils.splitPreserveAllTokens(replacer.apply(joined), '\n');

				builder.clearLores().lores(Arrays.asList(split));
			}
		}

		return builder;
	}

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Material", CompMaterial.fromItem(item).toString());

		if (item.getAmount() > 1)
			map.put("Count", item.getAmount());

		if (item.hasItemMeta()) {
			final ItemMeta meta = item.getItemMeta();

			if (meta.hasDisplayName())
				map.put("Name", Common.revertColorizing(meta.getDisplayName()));

			if (meta.getLore() != null)
				map.put("Lore", Common.revertColorizing(meta.getLore().toArray(new String[0])));

			// -----------------------------------------------------------
			// ItemMeta
			// -----------------------------------------------------------

			// Potion effects
			if (meta instanceof PotionMeta) {
				final PotionConverter cache = new PotionConverter(item, (PotionMeta) meta);

				map.put(cache.serialize());

			} else if (meta instanceof EnchantmentStorageMeta) {
				// Enchanted books
				final EnchantmentStorageConverter cache = new EnchantmentStorageConverter(item, (EnchantmentStorageMeta) meta);

				map.put("Stored_Enchantments", cache);

			} else if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) && meta instanceof Damageable) {
				final DamageableConverter converter = new DamageableConverter(item, (Damageable) meta);

				map.put(converter.serialize());

			} else if (meta instanceof SkullMeta) {
				final SkullConverter converter = new SkullConverter(item, (SkullMeta) meta);

				map.put(converter.serialize());

			} else if (meta instanceof BookMeta) {
				final BookConverter converter = new BookConverter(item, (BookMeta) meta);

				map.put(converter.serialize());

			} else if (meta instanceof LeatherArmorMeta) {
				final LeatherArmorConverter converter = new LeatherArmorConverter(item, (LeatherArmorMeta) meta);

				map.put(converter.serialize());

			} else if (meta instanceof BannerMeta) {
				final BannerConverter converter = new BannerConverter(item, (BannerMeta) meta);

				map.put("Patterns", converter);

			} else if (meta instanceof FireworkMeta) {
				final FireworkConverter converter = new FireworkConverter(item, (FireworkMeta) meta);

				map.put(converter.serialize());
			}

			// Enchants
			if (meta.hasEnchants()) {
				final SerializedMap enchantMap = new SerializedMap();

				for (final Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet())
					enchantMap.put(enchant.getKey().getName(), enchant.getValue());

				map.put("Enchantments", enchantMap);
			}

			// Flags
			map.put("Flags", Common.toList(meta.getItemFlags()));

			// Unbreakable
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_12) && meta.isUnbreakable())
				map.put("Unbreakable", true);

			else {
				final Object metaSpigot = ReflectionUtil.invoke("spigot", meta);
				final boolean isUnbreakable = ReflectionUtil.invoke("isUnbreakable", metaSpigot);

				if (isUnbreakable)
					map.put("Unbreakable", true);
			}
		}

		return map;
	}

	/**
	 * Creates an InventoryItem from the given map
	 *
	 * @param map
	 * @return
	 */
	public static InventoryItem deserialize(final SerializedMap map) {
		// Slot converting
		int slot = -1;

		if (map.containsKey("Slot"))
			try {
				slot = map.getInteger("Slot");

			} catch (final Exception e) {
				switch (map.getString("Slot")) {
					case "helmet":
						slot = 103;
						break;

					case "chestplate":
						slot = 102;
						break;

					case "leggings":
						slot = 101;
						break;

					case "boots":
						slot = 100;
						break;
				}
			}

		return new InventoryItem(toItem(map), slot, map.getBoolean("Colorable", false));
	}

	/**
	 * Creates an item stack from the given serialized map
	 *
	 * @param map
	 * @return
	 */
	public static ItemStack toItem(final SerializedMap map) {
		final ItemCreator.ItemCreatorBuilder builder = ItemCreator.builder().amount(map.getInteger("Count", 1));

		if (map.containsKey("Name"))
			builder.name(Common.colorize(map.getString("Name")));

		if (map.containsKey("Lore"))
			builder.lores(Common.colorize(map.getStringList("Lore")));

		// Material
		final CompMaterial material = map.getMaterial("Material", CompMaterial.STONE);
		final ItemMeta meta = Bukkit.getItemFactory().getItemMeta(material.getMaterial());

		builder.material(material);
		builder.damage(material.getData());

		// -----------------------------------------------------------
		// ItemMeta
		// -----------------------------------------------------------

		if (meta instanceof PotionMeta) {
			final PotionConverter converter = PotionConverter.deserialize(map);

			converter.apply(builder, (PotionMeta) meta);

		} else if (meta instanceof EnchantmentStorageMeta) {
			final EnchantmentStorageConverter converter = map.get("Stored_Enchantments", EnchantmentStorageConverter.class);

			converter.apply(builder, (EnchantmentStorageMeta) meta);

		} else if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) && meta instanceof Damageable) {
			final DamageableConverter converter = DamageableConverter.deserialize(map);

			converter.apply(builder, (Damageable) meta);

		} else if (meta instanceof SkullMeta) {
			final SkullConverter converter = SkullConverter.deserialize(map);

			converter.apply(builder, (SkullMeta) meta);

		} else if (meta instanceof BookMeta) {
			final BookConverter converter = BookConverter.deserialize(map);

			converter.apply(builder, (BookMeta) meta);

		} else if (meta instanceof LeatherArmorMeta) {
			final LeatherArmorConverter converter = LeatherArmorConverter.deserialize(map);

			converter.apply(builder, (LeatherArmorMeta) meta);

		} else if (meta instanceof BannerMeta) {
			final BannerConverter converter = BannerConverter.deserialize(map);

			converter.apply(builder, (BannerMeta) meta);

		} else if (meta instanceof FireworkMeta) {
			final FireworkConverter converter = FireworkConverter.deserialize(map);

			converter.apply(builder, (FireworkMeta) meta);
		}

		builder.meta(meta);

		// Enchantments
		final SerializedMap enchants = map.getMap("Enchantments");

		for (final String enchantment : enchants.keySet())
			try {
				builder.enchant(new SimpleEnchant(ItemUtil.findEnchantment(enchantment), enchants.getInteger(enchantment)));

			} catch (final Exception e) {
				Common.logFramed("Unknown enchantment " + enchantment + " when parsing " + enchants);
			}

		// Item flags
		if (map.containsKey("Flags")) {
			final List<String> flags = map.getStringList("Flags");

			for (final CompItemFlag flag : CompItemFlag.values())
				if (flags.contains(flag.toString()))
					builder.flag(flag);
		}

		// Unbreakable
		if (map.containsKey("Unbreakable"))
			builder.unbreakable(map.getBoolean("Unbreakable"));

		return builder.build().make();
	}

	// -----------------------------------------------------------------------------------
	// Classes for serializing and deserializing ItemMetas
	// -----------------------------------------------------------------------------------

	@RequiredArgsConstructor
	private static final class PotionConverter implements ConfigSerializable {
		private final boolean splash;

		// Main effect data
		private final PotionType mainType;
		private final boolean extended;
		private final boolean upgraded;

		private final List<PotionEffect> effects;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a PotionMeta from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private PotionConverter(final ItemStack item, final PotionMeta meta) {
			splash = CompMaterial.fromItem(item) == CompMaterial.SPLASH_POTION;

			// 1.8 and older versions don't have PotionData
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_9)) {
				final PotionData mainEffect = meta.getBasePotionData();

				mainType = mainEffect.getType();
				extended = mainEffect.isExtended();
				upgraded = mainEffect.isUpgraded();

			} else {
				final Potion mainEffect = Potion.fromItemStack(item);

				mainType = mainEffect.getType();
				extended = mainEffect.hasExtendedDuration();
				upgraded = mainEffect.getLevel() == 2;
			}


			effects = new ArrayList<>();

			if (meta.hasCustomEffects())
				effects.addAll(meta.getCustomEffects());
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Main_Effect", SerializedMap.ofArray(
					"Type", mainType.getEffectType(),
					"Extended", extended,
					"Upgraded", upgraded)
			);

			if (!effects.isEmpty()) {
				final SerializedMap potionMap = new SerializedMap();

				for (final PotionEffect effect : effects) {
					potionMap.put(effect.getType().toString(), SerializedMap.ofArray("Amplifier", effect.getAmplifier(),
							"Duration", effect.getDuration()));
				}

				map.put("Potion_Effects", potionMap);
			}

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized PotionMeta
		 *
		 * @param map
		 * @return
		 */
		public static PotionConverter deserialize(final SerializedMap map) {
			final boolean splash = map.getMaterial("Material") == CompMaterial.SPLASH_POTION;

			PotionType mainType = null;
			boolean extended = false;
			boolean upgraded = false;

			if (map.containsKey("Main_Effect")) {
				final SerializedMap mainEffect = map.getMap("Main_Effect");

				try {
					mainType = PotionType.getByEffect(ItemUtil.findPotion(mainEffect.getString("Type")));
					extended = mainEffect.getBoolean("Extended", false);
					upgraded = mainEffect.getBoolean("Upgraded", false);

				} catch (final Exception e) {
					Common.error(e, "Unknown potion type " + mainType);

					mainType = null;
				}
			}

			final SerializedMap effectsMap = map.getMap("Potion_Effects");
			final List<PotionEffect> effects = new ArrayList<>();

			for (final String effectType : effectsMap.keySet()) {
				final SerializedMap effect = effectsMap.getMap(effectType);

				try {
					effects.add(new PotionEffect(ItemUtil.findPotion(effectType),
							effect.getInteger("Duration"),
							effect.getInteger("Amplifier")));

				} catch (final Exception e) {
					Common.error(e, "Unknown potion type " + effectType);
				}
			}

			return new PotionConverter(splash, mainType, extended, upgraded, effects);
		}

		/**
		 * Applies the cached values onto the item builder and the PotionMeta
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final PotionMeta meta) {
			// 1.8 and older versions don't have PotionData
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_9))
				meta.setBasePotionData(new PotionData(Common.getOrDefault(mainType, PotionType.WATER),
						extended,
						upgraded));

			else {
				final Potion potion = new Potion(Common.getOrDefault(mainType, PotionType.WATER),
						upgraded ? 2 : 1,
						splash,
						extended);

				builder.damage(potion.toDamageValue());
			}

			for (final PotionEffect effect : effects)
				meta.addCustomEffect(effect, true);
		}
	}

	@RequiredArgsConstructor
	public static final class EnchantmentStorageConverter implements ConfigSerializable {
		private final Map<Enchantment, Integer> storedEnchants;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for an EnchantmentStorageCache from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private EnchantmentStorageConverter(final ItemStack item, final EnchantmentStorageMeta meta) {
			storedEnchants = new HashMap<>();

			storedEnchants.putAll(meta.getStoredEnchants());
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			for (final Map.Entry<Enchantment, Integer> enchant : storedEnchants.entrySet())
				map.put(enchant.getKey().getName(), enchant.getValue());

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized EnchantmentStorageCache
		 *
		 * @param map
		 * @return
		 */
		public static EnchantmentStorageConverter deserialize(final SerializedMap map) {
			final Map<Enchantment, Integer> enchants = new HashMap<>();

			for (final String enchantment : map.keySet()) {
				final int level = map.getInteger(enchantment);

				try {
					enchants.put(ItemUtil.findEnchantment(enchantment), level);

				} catch (final Exception e) {
					Common.error(e, "Error when parsing enchantment " + enchantment + " with level " + level);
				}
			}

			return new EnchantmentStorageConverter(enchants);
		}

		/**
		 * Applies the cached values onto the item builder and the EnchantmentStorageCache
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final EnchantmentStorageMeta meta) {
			for (final Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet())
				meta.addStoredEnchant(entry.getKey(), entry.getValue(), false);
		}
	}

	@RequiredArgsConstructor
	private static final class DamageableConverter implements ConfigSerializable {
		private final int damage;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a Damageable meta from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private DamageableConverter(final ItemStack item, final Damageable meta) {
			damage = meta.getDamage();
		}

		@Override
		public SerializedMap serialize() {
			return damage > 0 ? SerializedMap.of("Damage", damage) : new SerializedMap();
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized Damageable meta
		 *
		 * @param map
		 * @return
		 */
		public static DamageableConverter deserialize(final SerializedMap map) {
			final int damage = map.getInteger("Damage", 0);

			return new DamageableConverter(damage);
		}

		/**
		 * Applies the cached values onto the item builder and the Damageable meta
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final Damageable meta) {
			builder.damage(0);
		}
	}

	@RequiredArgsConstructor
	private static final class SkullConverter implements ConfigSerializable {
		private final String owner;

		private final String skinBase64;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a SkullMeta from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private SkullConverter(final ItemStack item, final SkullMeta meta) {
			owner = meta.getOwner();

			// Get head texture in Base64
			final Object profile = ReflectionUtil.getFieldContent(meta, "profile");
			final Object propertiesMap = ReflectionUtil.invoke("getProperties", profile);

			final Collection<Object> propertyList = ReflectionUtil.invoke("get", propertiesMap, "textures");

			if (propertyList.size() > 0) {
				skinBase64 = ReflectionUtil.invoke("getValue", propertyList.iterator().next());
			} else
				skinBase64 = null;
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.putIfExist("Skull_Owner", owner);
			map.putIfExist("Skull_Skin", skinBase64);

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized SkullMeta
		 *
		 * @param map
		 * @return
		 */
		public static SkullConverter deserialize(final SerializedMap map) {
			final String owner = map.getString("Skull_Owner");
			final String skinBase64 = map.getString("Skull_Skin");

			return new SkullConverter(owner, skinBase64);
		}

		/**
		 * Applies the cached values onto the item builder and the SkullMeta
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final SkullMeta meta) {
			if (owner != null)
				meta.setOwner(owner);

			if (skinBase64 != null)
				// Reflection call because there is no public way to mutate item meta
				ReflectionUtil.invokeStatic(SkullCreator.class, "mutateItemMeta", meta, skinBase64);
		}
	}

	@RequiredArgsConstructor
	private static final class BookConverter implements ConfigSerializable {
		private final String author;
		private final String title;

		private final List<BaseComponent[]> pages;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a BookMeta from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private BookConverter(final ItemStack item, final BookMeta meta) {
			author = meta.getAuthor();
			title = meta.getTitle();

			pages = Remain.getPages(meta);
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.putIfExist("Author", author);
			map.putIfExist("Title", Common.revertColorizing(title));

			map.put("Pages", Common.convert(this.pages, page -> "[JSON]" + Remain.toJson(page)));

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized BookMeta
		 *
		 * @param map
		 * @return
		 */
		public static BookConverter deserialize(final SerializedMap map) {
			final String author = map.getString("Author");
			final String title = Common.colorize(map.getString("Title"));

			final List<BaseComponent[]> pages = new ArrayList<>();

			if (map.containsKey("Pages"))
				pages.addAll(Common.convert(map.getStringList("Pages"),
						// Convert pages to JSON if they are not already JSON
						page -> Remain.toComponent(page.startsWith("[JSON]") ? page : Remain.toJson(page.substring(6)))));

			return new BookConverter(author, title, pages);
		}

		/**
		 * Applies the cached values onto the item builder and the BookMeta
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final BookMeta meta) {
			meta.setAuthor(author);
			meta.setTitle(title);

			Remain.setPages(meta, pages);
		}
	}

	@RequiredArgsConstructor
	private static final class LeatherArmorConverter implements ConfigSerializable {
		private final Color color;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a LeatherArmorMeta from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private LeatherArmorConverter(final ItemStack item, final LeatherArmorMeta meta) {
			color = meta.getColor();
		}

		@Override
		public SerializedMap serialize() {
			return SerializedMap.of("Armor_Color", color.getRed() + ", " + color.getGreen() + ", " + color.getBlue());
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized LeatherArmorMeta
		 *
		 * @param map
		 * @return
		 */
		public static LeatherArmorConverter deserialize(final SerializedMap map) {
			Color color = null;

			if (map.containsKey("Armor_Color")) {
				final String rawColor = map.getString("Armor_Color");

				final String[] split = StringUtils.splitByWholeSeparator(rawColor, ", ");
				Valid.checkBoolean(split.length == 3, "Malformed RGB armor color '" + rawColor + "', should be in format '<red>, <green>, <blue>'!");

				color = Color.fromRGB(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
			}

			return new LeatherArmorConverter(color);
		}

		/**
		 * Applies the cached values onto the item builder and the LeatherArmorMeta
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final LeatherArmorMeta meta) {
			if (color != null)
				builder.color(CompColor.fromColor(color));
		}
	}

	@RequiredArgsConstructor
	private static final class BannerConverter implements ConfigSerializable {
		private final List<Pattern> patterns;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a META from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private BannerConverter(final ItemStack item, final BannerMeta meta) {
			patterns = meta.getPatterns();
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			for (final Pattern pattern : patterns)
				map.put(pattern.getPattern().name(), pattern.getColor());

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized META
		 *
		 * @param map
		 * @return
		 */
		public static BannerConverter deserialize(final SerializedMap map) {
			final SerializedMap patternMap = map.getMap("Patterns");

			final List<Pattern> patterns = new ArrayList<>();

			for (final String pattern : patternMap.keySet()) {
				final String color = patternMap.getString(pattern);

				try {
					patterns.add(new Pattern(ReflectionUtil.lookupEnum(DyeColor.class, color, "Unknown dye color " + color),
							ReflectionUtil.lookupEnum(PatternType.class, pattern, "Unknown pattern type " + pattern)));

				} catch (final Exception e) {
					Common.error(e, "Error when parsing pattern " + pattern + " with color " + color);
				}
			}

			return new BannerConverter(patterns);
		}

		/**
		 * Applies the cached values onto the item builder and the META
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final BannerMeta meta) {
			builder.patterns(patterns);
		}
	}

	@RequiredArgsConstructor
	private static final class FireworkConverter implements ConfigSerializable {
		private final int power;

		private final List<FireworkEffect> effects;

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a META from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private FireworkConverter(final ItemStack item, final FireworkMeta meta) {
			power = meta.getPower();

			effects = meta.getEffects();
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = SerializedMap.of("Power", power);
			final Set<SerializedMap> effectsList = new HashSet<>();

			for (final FireworkEffect effect : effects) {
				final SerializedMap effectMap = new SerializedMap();

				effectMap.putIfTrue("Flicker", effect.hasFlicker());
				effectMap.putIfTrue("Trail", effect.hasTrail());

				final List<Color> colors = effect.getColors();

				if (!colors.isEmpty())
					effectMap.put("Colors", Common.convert(colors, color -> color.getRed() + ", " + color.getGreen() + ", " + color.getBlue()));

				final List<Color> fadeColors = effect.getColors();

				if (!fadeColors.isEmpty())
					effectMap.put("Fade_Colors", Common.convert(fadeColors, color -> color.getRed() + ", " + color.getGreen() + ", " + color.getBlue()));
			}

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized META
		 *
		 * @param map
		 * @return
		 */
		public static FireworkConverter deserialize(final SerializedMap map) {
			final int power = map.getInteger("Power", 0);

			final List<FireworkEffect> effects = new ArrayList<>();

			for (final SerializedMap effectMap : map.getMapList("Firework_Effects")) {
				final String rawType = effectMap.getString("Type");

				// Whether the particles should flicker after exploded
				final boolean flicker = effectMap.getBoolean("Flicker");

				// If the particles after explosiong should leave a trail
				final boolean trail = effectMap.getBoolean("Trail");

				try {
					final FireworkEffect.Type type = ReflectionUtil.lookupEnum(FireworkEffect.Type.class, rawType,
							"Unknown firework effect type " + rawType);

					// Primary color of the effect
					final List<Color> colors = new ArrayList<>();

					if (effectMap.containsKey("Colors"))
						for (final String color : effectMap.getStringList("Colors")) {
							final String[] split = StringUtils.splitByWholeSeparator(color, ", ");

							colors.add(Color.fromRGB(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2])));
						}

					// Fading colors of the effect
					final List<Color> fadeColors = new ArrayList<>();

					if (effectMap.containsKey("Fade_Colors"))
						for (final String color : effectMap.getStringList("Fade_Colors")) {
							final String[] split = StringUtils.splitByWholeSeparator(color, ", ");

							fadeColors.add(Color.fromRGB(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2])));
						}

					effects.add(FireworkEffect.builder()
							.flicker(flicker)
							.trail(trail)
							.withColor(colors)
							.withFade(fadeColors)
							.build());

				} catch (final Exception e) {
					Common.error(e, "Error when parsing firework effect " + effectMap);
				}
			}

			return new FireworkConverter(power, effects);
		}

		/**
		 * Applies the cached values onto the item builder and the META
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final FireworkMeta meta) {
			meta.setPower(power);
			meta.addEffects(effects);
		}
	}

	@RequiredArgsConstructor
	private static final class MetaConverter implements ConfigSerializable {

		// -----------------------------------------------------------------------------------
		// Serialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache for a META from an existing item
		 *
		 * @param item
		 * @param meta
		 */
		private MetaConverter(final ItemStack item, final Damageable meta) {
		}

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			return map;
		}

		// -----------------------------------------------------------------------------------
		// Deserialization
		// -----------------------------------------------------------------------------------

		/**
		 * Creates a new cache from a serialized META
		 *
		 * @param map
		 * @return
		 */
		public static MetaConverter deserialize(final SerializedMap map) {
			return new MetaConverter();
		}

		/**
		 * Applies the cached values onto the item builder and the META
		 *
		 * @param builder
		 * @param meta
		 */
		private void apply(final ItemCreator.ItemCreatorBuilder builder, final Damageable meta) {

		}
	}
}
