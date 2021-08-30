package org.mineacademy.fo.menu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.SimpleEnchant;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.model.SimplePotionData;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * Our core class for easy and comfortable item creation.
 * <p>
 * You can use this to make named items with incredible speed and quality.
 */
final @Builder
public class ItemCreator implements ConfigSerializable {

	/**
	 * The initial item stack
	 */
	private final ItemStack item;

	/**
	 * The initial material
	 */
	private final CompMaterial material;

	/**
	 * The amount of the item
	 */
	@Builder.Default
	private final int amount = 1;

	/**
	 * The item damage
	 */
	@Builder.Default
	private final int damage = -1;

	/**
	 * The player inventory slot to be placed in when calling {@link #give(Player)}
	 */
	@Builder.Default
	private final int slot = -1;

	/**
	 * The item name, colors are replaced
	 */
	private final String name;

	/**
	 * The lore for this item, colors are replaced
	 */
	@Singular
	private final List<String> lores;

	/**
	 * The enchants applied for the item
	 */
	@Singular
	private final List<SimpleEnchant> enchants;

	/**
	 * The item flags
	 */
	@Singular
	private List<CompItemFlag> flags;

	/**
	 * The patterns if this is a banner
	 */
	@Singular
	private final List<Pattern> patterns;

	/**
	 * Is the item unbreakable?
	 */
	private Boolean unbreakable;

	/**
	 * The dye color in case your item is compatible
	 */
	private final CompColor color;

	/**
	 * Should we hide all tags from the item (enchants, etc.)?
	 */
	@Builder.Default
	private boolean hideTags = false;

	/**
	 * Should we add glow to the item? (adds a fake enchant and uses
	 * {@link ItemFlag} to hide it)
	 * <p>
	 * The enchant is visible on older MC versions.
	 */
	private final boolean glow;

	/**
	 * The skull owner, in case it applies
	 */
	private final String skullOwner;

	/**
	 * The skull skin in base64, in case this is a skull
	 */
	private final String skullSkin;

	/**
	 * The list of NBT tags with their key-value pairs
	 */
	@Singular
	private final Map<String, String> tags;

	/**
	 * If this is a book, you can set its new pages here, supports json pages starting with [JSON]
	 */
	@Singular
	private final List<BaseComponent[]> bookPages;

	/**
	 * If this a book, you can set its author here
	 */
	private final String bookAuthor;

	/**
	 * If this a book, you can set its title here
	 */
	private final String bookTitle;

	/**
	 * The potion data if this is a potion
	 */
	private final SimplePotionData potionData;

	/**
	 * The potion effects in case this is a potion
	 */
	@Singular
	private final List<PotionEffect> potionEffects;

	/**
	 * The power of this firework
	 */
	@Builder.Default
	private final int fireworkPower = -1;

	/**
	 * The firework effects in case this is a firework
	 */
	@Singular
	private final List<FireworkEffect> fireworkEffects;

	/**
	 * The item meta, overriden by other fields
	 */
	private final ItemMeta meta;

	// ----------------------------------------------------------------------------------------
	// Convenience give methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly adding this item into a players inventory
	 * at the slot this item creator specified. Throws an exception if the slot
	 * is not defined.
	 *
	 * @param player
	 */
	public void give(final Player player) {
		Valid.checkBoolean(this.slot != -1, "Slot is not defined in " + serialize().toStringFormatted());

		give(player, this.slot);
	}

	/**
	 * Adds this item into the player's inventory at the given slot
	 * <p>
	 * See {@link org.bukkit.inventory.PlayerInventory#setItem(int, ItemStack)}
	 * for information on slots.
	 *
	 * @param player
	 * @param slot
	 */
	public void give(final Player player, int slot) {
		player.getInventory().setItem(slot, this.make());
	}

	// ----------------------------------------------------------------------------------------
	// Constructing items
	// ----------------------------------------------------------------------------------------

	/**
	 * Returns a copy of this item creator with the given replacer
	 * applied to the name and lore, allowing you to call any of the
	 * {@link #make()} methods below
	 *
	 * @param replacer
	 * @return
	 */
	public ItemCreator replaceVariables(@NonNull final Function<String, String> replacer) {
		final ItemCreatorBuilder builder = of(make()).slot(this.slot);

		if (this.name != null)
			builder.name(replacer.apply(this.name));

		if (this.lores != null && !this.lores.isEmpty()) {
			final String joined = replacer.apply(String.join("\n", this.lores));

			// Replace the variables again if the replacements had placeholders in them
			final String[] split = StringUtils.splitPreserveAllTokens(replacer.apply(joined), '\n');

			builder.clearLores().lores(Arrays.asList(split));
		}

		return builder.build();
	}

	/**
	 * Constructs a new {@link DummyButton} from this item
	 *
	 * @return a new dummy button
	 */
	public DummyButton makeButton() {
		return Button.makeDummy(this);
	}

	/**
	 * Make an unbreakable item with all attributes hidden, suitable for menu use.
	 *
	 * @return the new menu tool, unbreakable with all attributes hidden
	 */
	public ItemStack makeMenuTool() {
		unbreakable = true;
		hideTags = true;

		return make();
	}

	/**
	 * @deprecated pending removal, this simply calls {@link #make()}
	 *
	 * @return
	 */
	@Deprecated
	public ItemStack makeSurvival() {
		return make();
	}

	/**
	 * Construct a valid {@link ItemStack} from all parameters above.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {
		//
		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		//
		Valid.checkBoolean(material != null || item != null, "Material or item must be set!");

		if (material != null)
			Valid.checkNotNull(material.getMaterial(), "Material#getMaterial cannot be null for " + material);

		final ItemStack compiledItem = item != null ? item.clone() : new ItemStack(material.getMaterial(), amount);
		ItemMeta compiledMeta = meta != null ? meta.clone() : compiledItem.getItemMeta();

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Override with given material
		if (material != null) {
			final Material mat = material.getMaterial();
			final byte data = material.getData();

			compiledItem.setType(mat);

			if (data != 0 && MinecraftVersion.olderThan(V.v1_13)) {
				compiledItem.setData(new MaterialData(mat, data));
				compiledItem.setDurability(data);
			}
		}

		// Apply specific material color if possible
		color:
		if (color != null) {

			if (compiledItem.getType().toString().contains("LEATHER")) {
				if (MinecraftVersion.atLeast(V.v1_4)) {
					Valid.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);

					((LeatherArmorMeta) compiledMeta).setColor(color.getColor());
				}
			}

			else {

				// Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
				if (MinecraftVersion.atLeast(V.v1_13)) {
					final String dye = color.getDye().toString();
					final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_GLASS",
							"STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

					for (final String material : colorableMaterials) {
						final String suffix = "_" + material;

						if (compiledItem.getType().toString().endsWith(suffix)) {
							compiledItem.setType(Material.valueOf(dye + suffix));

							break color;
						}
					}
				}

				else {
					final byte dataValue = color.getDye().getWoolData();

					compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
					compiledItem.setDurability(dataValue);
				}
			}
		}

		// Fix monster eggs
		if (compiledItem.getType().toString().endsWith("SPAWN_EGG") || compiledItem.getType().toString().equals("MONSTER_EGG")) {

			EntityType entity = null;

			if (MinecraftVersion.olderThan(V.v1_13)) { // Try to find it if already exists
				CompMonsterEgg.acceptUnsafeEggs = true;
				final EntityType pre = CompMonsterEgg.getEntity(compiledItem);
				CompMonsterEgg.acceptUnsafeEggs = false;

				if (pre != null && pre != EntityType.UNKNOWN)
					entity = pre;
			}

			if (entity == null) {
				final String itemName = compiledItem.getType().toString();

				String entityRaw = itemName.replace("_SPAWN_EGG", "");

				if (entityRaw.equals("MONSTER_EGG") && material != null && material.toString().endsWith("SPAWN_EGG"))
					entityRaw = material.toString().replace("_SPAWN_EGG", "");

				if ("MOOSHROOM".equals(entityRaw))
					entityRaw = "MUSHROOM_COW";

				else if ("ZOMBIE_PIGMAN".equals(entityRaw))
					entityRaw = "PIG_ZOMBIE";

				try {
					entity = EntityType.valueOf(entityRaw);

				} catch (final Throwable t) {

					// Probably version incompatible
					Common.log("The following item could not be transformed into " + entityRaw + " egg, item: " + compiledItem);
				}
			}

			if (entity != null)
				compiledMeta = CompMonsterEgg.setEntity(compiledItem, entity).getItemMeta();
		}

		flags = Common.toList(flags);

		if (damage != -1) {

			try {
				ReflectionUtil.invoke("setDurability", compiledItem, (short) damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(damage);
			} catch (final Throwable t) {
			}
		}

		if (color != null && compiledItem.getType().toString().contains("LEATHER"))
			((LeatherArmorMeta) compiledMeta).setColor(color.getColor());

		if (skullOwner != null && compiledMeta instanceof SkullMeta)
			((SkullMeta) compiledMeta).setOwner(skullOwner);

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (bookPages != null)
				Remain.setPages(bookMeta, bookPages);

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor(Common.colorize(bookAuthor));

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle(Common.colorize(bookTitle));
		}

		if (patterns != null && compiledMeta instanceof BannerMeta)
			for (final Pattern pattern : patterns)
				((BannerMeta) compiledMeta).addPattern(pattern);

		if (compiledMeta instanceof FireworkMeta) {
			FireworkMeta fireworkMeta = (FireworkMeta) compiledMeta;

			if (fireworkPower != -1)
				fireworkMeta.setPower(fireworkPower);

			if (fireworkEffects != null && !fireworkEffects.isEmpty())
				fireworkMeta.addEffects(fireworkEffects);
		}

		if (glow) {
			compiledMeta.addEnchant(Enchantment.DURABILITY, 1, true);

			flags.add(CompItemFlag.HIDE_ENCHANTS);
		}

		if (enchants != null)
			for (final SimpleEnchant ench : enchants)
				if (compiledMeta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) compiledMeta).addStoredEnchant(ench.getEnchant(), ench.getLevel(), true);
				else
					compiledMeta.addEnchant(ench.getEnchant(), ench.getLevel(), true);

		if (name != null && !"".equals(name))
			compiledMeta.setDisplayName(Common.colorize("&r&f" + name));

		if (lores != null && !lores.isEmpty()) {
			final List<String> coloredLores = new ArrayList<>();

			for (final String lore : lores)
				coloredLores.add(Common.colorize("&7" + lore));

			compiledMeta.setLore(coloredLores);
		}

		if (potionEffects != null && !potionEffects.isEmpty() && compiledMeta instanceof PotionMeta)
			for (PotionEffect effect : potionEffects)
				((PotionMeta) compiledMeta).addCustomEffect(effect, true);

		if (unbreakable != null) {
			flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			CompProperty.UNBREAKABLE.apply(compiledMeta, true);
		}

		if (hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!flags.contains(f))
					flags.add(f);

		for (final CompItemFlag flag : flags)
			try {
				compiledMeta.addItemFlags(ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Apply Bukkit metadata
		compiledItem.setItemMeta(compiledMeta);

		//
		// From now on we have to re-set the item
		//

		// Apply custom enchantment lores
		ItemStack finalItem = compiledItem;
		final ItemStack enchantedIs = SimpleEnchantment.addEnchantmentLores(compiledItem);

		if (enchantedIs != null)
			finalItem = enchantedIs;

		// Apply the skull skin AFTER we applied the item meta
		if (skullSkin != null && compiledMeta instanceof SkullMeta)
			finalItem = SkullCreator.itemWithBase64(finalItem, skullSkin);

		// SimplePotionData#apply also gets the ItemMeta from the item
		if (potionData != null && compiledMeta instanceof PotionMeta)
			potionData.apply(compiledItem);

		// Apply NBT tags
		if (tags != null)
			if (MinecraftVersion.atLeast(V.v1_8))
				for (final Entry<String, String> entry : tags.entrySet())
					finalItem = CompMetadata.setMetadata(finalItem, entry.getKey(), entry.getValue());

			else if (!tags.isEmpty() && item != null)
				Common.log("Item had unsupported tags " + tags + " that are not supported on MC " + MinecraftVersion.getServerVersion() + " Item: " + finalItem);

		return finalItem;
	}

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		//
		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		//
		Valid.checkBoolean(material != null || item != null, "Material or item must be set!");

		if (material != null)
			Valid.checkNotNull(material.getMaterial(), "Material#getMaterial cannot be null for " + material);

		final SerializedMap map = new SerializedMap();
		final CompMaterial mat = Common.getOrDefault(material, CompMaterial.fromItem(item));

		// Create new item meta to only serialize data relevant to the material
		final ItemMeta meta = Bukkit.getItemFactory().getItemMeta(mat.getMaterial());

		if (mat != CompMaterial.AIR)
			map.put("Material", mat);

		if (amount > 1)
			map.put("Amount", amount);

		// Player inventory slot
		if (slot != -1)
			map.put("Inventory_Slot", slot);

		// Display
		if (!Valid.isNullOrEmpty(name))
			map.put("Name", name);

		if (lores != null && !lores.isEmpty())
			map.put("Lore", lores);

		// -----------------------------------------------------------
		// ItemMeta
		// -----------------------------------------------------------

		if (meta instanceof PotionMeta) {
			if (potionData != null) {
				final SerializedMap mainEffectMap = new SerializedMap();

				mainEffectMap.put("Type", potionData.getType());
				mainEffectMap.put("Extended", potionData.isExtended());
				mainEffectMap.put("Upgraded", potionData.isUpgraded());

				map.put("Main_Effect", mainEffectMap);
			}

			if (potionEffects != null && !potionEffects.isEmpty()) {
				final SerializedMap effectsMap = new SerializedMap();

				for (final PotionEffect effect : potionEffects) {
					final SerializedMap effectMap = new SerializedMap();

					effectMap.put("Duration", effect.getDuration() * 20);
					effectMap.put("Amplifier", effect.getAmplifier());

					effectsMap.put(effect.getType().getName(), effectMap);
				}
			}
		}

		if (meta instanceof EnchantmentStorageMeta && enchants != null && !enchants.isEmpty()) {
			final SerializedMap enchantsMap = new SerializedMap();

			for (final SimpleEnchant enchant : enchants)
				enchantsMap.put(enchant.getEnchant().getName(), enchant.getLevel());

			map.put("Stored_Enchants", enchantsMap);
		}

		if (damage != -1 && !(MinecraftVersion.atLeast(V.v1_13) && !(meta instanceof Damageable)))
			map.put("Damage", damage);

		if (meta instanceof SkullMeta) {
			map.putIfExist("Skull_Owner", skullOwner);
			map.putIfExist("Skull_Skin", skullSkin);
		}

		if (meta instanceof BookMeta) {
			if (!Valid.isNullOrEmpty(bookAuthor))
				map.put("Author", bookAuthor);

			if (!Valid.isNullOrEmpty(bookTitle))
				map.put("Title", bookTitle);

			map.put("Pages", Common.convert(this.bookPages, page -> "[JSON]" + Remain.toJson(page)));
		}

		if (color != null && meta instanceof LeatherArmorMeta) {
			final Color rawColor = color.getColor();

			map.put("Armor_Color", rawColor.getRed() + ", " + rawColor.getGreen() + ", " + rawColor.getBlue());
		}

		if (patterns != null && !patterns.isEmpty() && meta instanceof BannerMeta) {
			final List<SerializedMap> patterns = new ArrayList<>();

			for (final Pattern pattern : this.patterns)
				patterns.add(SerializedMap.ofArray(
						"Type", pattern.getPattern(),
						"Color", pattern.getColor()
				));

			map.put("Patterns", patterns);
		}

		if (meta instanceof FireworkMeta) {
			final List<SerializedMap> effectsList = new ArrayList<>();

			if (fireworkEffects != null && !fireworkEffects.isEmpty())
				for (final FireworkEffect effect : fireworkEffects) {
					final SerializedMap effectMap = new SerializedMap();

					effectMap.put("Type", effect.getType());
					effectMap.putIfTrue("Flicker", effect.hasFlicker());
					effectMap.putIfTrue("Trail", effect.hasTrail());

					final List<Color> colors = effect.getColors();

					if (!colors.isEmpty())
						effectMap.put("Colors", Common.convert(colors, color -> color.getRed() + ", " + color.getGreen() + ", " + color.getBlue()));

					final List<Color> fadeColors = effect.getColors();

					if (!fadeColors.isEmpty())
						effectMap.put("Fade_Colors", Common.convert(fadeColors, color -> color.getRed() + ", " + color.getGreen() + ", " + color.getBlue()));

					effectsList.add(effectMap);
				}

			if (fireworkPower != -1)
				map.put("Power", fireworkPower);

			if (!effectsList.isEmpty())
				map.put("Firework_Effects", effectsList);
		}

		// -----------------------------------------------------------
		// Universal
		// -----------------------------------------------------------

		// Enchantments
		if (!(meta instanceof EnchantmentStorageMeta) && enchants != null && !enchants.isEmpty()) {
			final SerializedMap enchantsMap = new SerializedMap();

			for (final SimpleEnchant enchant : enchants)
				enchantsMap.put(enchant.getEnchant().getName(), enchant.getLevel());

			map.put("Enchantments", enchantsMap);
		}

		// Flags
		if (flags != null && !flags.isEmpty())
			map.put("Flags", flags);

		// Unbreakable
		if (unbreakable != null)
			map.put("Unbreakable", unbreakable);

		return map;
	}

	// ----------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreatorBuilder of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, lore.toArray(new String[0]));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreatorBuilder of(final String material, final String name, @NonNull final Collection<String> lore) {
		return of(CompMaterial.valueOf(material), name, lore.toArray(new String[0]));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreatorBuilder of(final CompMaterial material, final String name, @NonNull final String... lore) {
		return ItemCreator.builder().material(material).name("&r" + name).lores(Arrays.asList(lore)).hideTags(true);
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreatorBuilder of(final String material, final String name, @NonNull final String... lore) {
		return ItemCreator.builder().material(CompMaterial.valueOf(material)).name("&r" + name).lores(Arrays.asList(lore)).hideTags(true);
	}

	/**
	 * Convenience method to get a wool
	 *
	 * @param color the wool color
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder ofWool(final CompColor color) {
		return of(CompMaterial.makeWool(color, 1)).color(color);
	}

	/**
	 * Convenience method to get the creator of an existing itemstack
	 *
	 * @param item existing itemstack
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder of(final ItemStack item) {
		final ItemCreatorBuilder builder = ItemCreator.builder();
		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.getLore() != null)
			builder.lores(meta.getLore());

		return builder.item(item);
	}

	/**
	 * Get a new item creator from material
	 *
	 * @param mat existing material
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return ItemCreator.builder().material(mat);
	}

	/**
	 * Deserializes an item from the given serialized map
	 *
	 * @param map
	 * @return
	 */
	public static ItemCreatorBuilder of(SerializedMap map) {
		final CompMaterial material = map.getMaterial("Material", CompMaterial.AIR);
		final ItemCreatorBuilder builder = of(material);

		builder.amount(map.getInteger("Amount", 1));
		builder.damage(material.getData());

		// Player inventory slot
		if (map.containsKey("Inventory_Slot"))
			builder.slot(map.getInteger("Inventory_Slot"));

		// Display
		builder.name(map.getString("Name"));

		if (map.containsKey("Lore"))
			builder.lores(map.getStringList("Lore"));

		// -----------------------------------------------------------
		// ItemMeta
		// -----------------------------------------------------------

		// PotionMeta
		if (map.containsKey("Main_Effect")) {
			final SerializedMap mainEffect = map.getMap("Main_Effect");

			final boolean extended = mainEffect.getBoolean("Extended", false);
			final boolean upgraded = mainEffect.getBoolean("Upgraded", false);

			if (extended && upgraded)
				Common.warning("Potion " + map.toStringFormatted() + " cannot be both extended AND upgraded");

			else
				try {
					final String rawType = mainEffect.getString("Type");
					PotionType type = ReflectionUtil.lookupEnumSilent(PotionType.class, rawType);

					if (type == null)
						type = PotionType.getByEffect(ItemUtil.findPotion(rawType));

					builder.potionData(new SimplePotionData(type, material == CompMaterial.SPLASH_POTION, extended, upgraded));

				} catch (final Exception e) {
					Common.error(e, "Error when setting main potion effect from " + map.toStringFormatted());
				}
		}

		if (map.containsKey("Potion_Effects")) {
			final SerializedMap potionEffects = map.getMap("Potion_Effects");

			for (final String effectType : potionEffects.keySet()) {
				final SerializedMap effect = potionEffects.getMap(effectType);

				final int durationTicks = effect.getInteger("Duration") * 20;
				final int amplifier = effect.getInteger("Amplifier");

				try {
					final PotionEffectType potion = ItemUtil.findPotion(effectType);

					builder.potionEffect(new PotionEffect(potion, durationTicks, amplifier));

				} catch (final Exception e) {
					Common.error(e, "Error when adding potion effect from " + map.toStringFormatted());
				}
			}
		}

		// EnchantmentStorageMeta
		if (map.containsKey("Stored_Enchants")) {
			final SerializedMap storedEnchantments = map.getMap("Stored_Enchants");

			for (final String enchantType : storedEnchantments.keySet()) {
				final int level = storedEnchantments.getInteger(enchantType);

				try {
					final Enchantment enchantment = ItemUtil.findEnchantment(enchantType);

					builder.enchant(new SimpleEnchant(enchantment, level));

				} catch (Exception e) {
					Common.error(e, "Error when adding stored enchant from " + map.toStringFormatted());
				}
			}
		}

		// Damageable
		if (map.containsKey("Damage"))
			builder.damage(map.getInteger("Damage"));

		// SkullMeta
		builder.skullOwner(map.getString("SkullOwner"));
		builder.skullSkin(map.getString("SkullSkin"));

		// BookMeta
		builder.bookAuthor(map.getString("Author"));
		builder.bookTitle(map.getString("Title"));

		if (map.containsKey("Pages"))
			for (final String page : map.getStringList("Pages"))
				try {
					// Convert pages to JSON if they are not already JSON
					final BaseComponent[] components = Remain.toComponent(page.startsWith("[JSON]") ? page.substring(6) : Remain.toJson(page));

					builder.bookPage(components);

				} catch (Exception e) {
					Common.error(e, "Error when trying to add a page from " + map.toStringFormatted());
				}

		// LeatherArmorMeta
		if (map.containsKey("Armor_Color")) {
			final String[] values = StringUtils.splitByWholeSeparator(map.getString("Armor_Color"), ", ");

			if (values.length != 3)
				Common.warning("Malformed RGB armor color " + Arrays.toString(values) + ", should be in format '<red>, <green>, <blue>'!");

			else
				builder.color(CompColor.fromColor(Color.fromRGB(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]))));
		}

		// BannerMeta
		if (map.containsKey("Patterns"))
			for (final SerializedMap patternMap : map.getMapList("Patterns")) {
				try {
					final PatternType patternType = ReflectionUtil.lookupEnumSilent(PatternType.class, patternMap.getString("Type"));
					final CompColor color = CompColor.fromName(patternMap.getString("Color"));

					builder.pattern(new Pattern(color.getDye(), patternType));

				} catch (final Exception e) {
					Common.error(e, "Error when adding banner pattern from " + map.toStringFormatted());
				}
			}

		// FireworkMeta
		builder.fireworkPower(map.getInteger("Power", 0));

		if (map.containsKey("Firework_Effects"))
			for (final SerializedMap effectMap : map.getMapList("Firework_Effects")) {
				final boolean flicker = effectMap.getBoolean("Flicker");
				final boolean trail = effectMap.getBoolean("Trail");

				try {
					final FireworkEffect.Type type = ReflectionUtil.lookupEnumSilent(FireworkEffect.Type.class, effectMap.getString("Type"));

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

					builder.fireworkEffect(FireworkEffect.builder()
							.flicker(flicker)
							.trail(trail)
							.withColor(colors)
							.withFade(fadeColors)
							.build());

				} catch (final Exception e) {
					Common.error(e, "Error when adding firework effect from " + map.toStringFormatted());
				}
			}

		// -----------------------------------------------------------
		// Universal
		// -----------------------------------------------------------

		// Enchantments
		final SerializedMap enchants = map.getMap("Enchantments");

		for (final String enchantType : enchants.keySet())
			try {
				final Enchantment enchant = ItemUtil.findEnchantment(enchantType);
				final int level = enchants.getInteger(enchantType);

				builder.enchant(new SimpleEnchant(enchant, level));

			} catch (final Exception e) {
				Common.error(e, "Error when adding enchantment from " + map.toStringFormatted());
			}

		// Item flags
		if (map.containsKey("Flags")) {
			final List<String> flags = map.getStringList("Flags");

			for (final CompItemFlag flag : CompItemFlag.values())
				if (flags.contains(flag.toString()))
					builder.flag(flag);
		}

		// Make unbreakable
		builder.unbreakable(map.getBoolean("Unbreakable", false));

		// Add glow
		builder.glow(map.getBoolean("Glow", false));

		return builder;
	}

	/**
	 * Builds an item creator from this map
	 * <p>
	 * You won't be able to modify this item anymore!
	 *
	 * @param map
	 * @return
	 */
	public static ItemCreator deserialize(SerializedMap map) {
		return of(map).build();
	}
}