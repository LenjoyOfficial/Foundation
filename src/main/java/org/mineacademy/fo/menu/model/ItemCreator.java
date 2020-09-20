package org.mineacademy.fo.menu.model;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
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
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
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
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * ItemCreator allows you to create highly customized {@link ItemStack}
 * easily, simply call the static "of" methods, customize your item and then
 * call {@link #make()} to turn it into a Bukkit ItemStack.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemCreator implements ConfigSerializable {

	/**
	 * The {@link ItemStack}, if any, to start building with. Either this, or {@link #material} must be set.
	 */
	@Nullable
	private ItemStack item;

	/**
	 * The item meta, if any, to start building with. Parameters above
	 * will override this.
	 */
	@Nullable
	private ItemMeta meta;

	/**
	 * The {@link CompMaterial}, if any, to start building with. Either this, or {@link #item} must be set.
	 */
	@Nullable
	private CompMaterial material;

	/**
	 * The amount of the item.
	 */
	private int amount = -1;

	/**
	 * The item damage.
	 */
	private int damage = -1;

	/**
	 * The player inventory slot to be placed in when calling {@link #give(Player)}
	 */
	private int slot = -1;

	/**
	 * The item name (& color codes are replaced automatically).
	 */
	@Getter
	private String name;

	/**
	 * The lore for this item (& color codes are replaced automatically).
	 */
	private final List<String> lores = new ArrayList<>();

	/**
	 * The enchants applied to the item.
	 */
	private final List<SimpleEnchant> enchants = new ArrayList<>();

	/**
	 * The {@link CompItemFlag}.
	 */
	private final List<CompItemFlag> flags = new ArrayList<>();

	/**
	 * The patterns if this is a banner.
	 */
	private final List<Pattern> patterns = new ArrayList<>();

	/**
	 * Is the item unbreakable?
	 */
	private boolean unbreakable = false;

	/**
	 * The color in case your item is either of {@link LeatherArmorMeta},
	 * or from a selected list of compatible items such as stained glass, wool, etc.
	 */
	@Nullable
	private CompColor color;

	/**
	 * Should we hide all tags from the item (enchants, attributes, etc.)?
	 */
	private boolean hideTags = false;

	/**
	 * The custom model data of the item
	 */
	private Integer modelData;

	/**
	 * Should we add glow to the item? (adds a fake enchant and uses {@link ItemFlag}
	 * to hide it). The enchant is visible on older MC versions.
	 */
	private boolean glow = false;

	/**
	 * The skull owner, in case the item is a skull.
	 */
	@Nullable
	private String skullOwner;

	/**
	 * The skull skin in base64, in case this is a skull
	 */
	@Nullable
	private String skullSkin;

	/**
	 * The list of custom hidden data injected to the item.
	 */
	private final Map<String, String> tags = new HashMap<>();

	/**
	 * If this is a book, you can set its new pages here.
	 */
	private final List<BaseComponent[]> bookPages = new ArrayList<>();

	/**
	 * If this a book, you can set its author here.
	 */
	@Nullable
	private String bookAuthor;

	/**
	 * If this a book, you can set its title here.
	 */
	@Nullable
	private String bookTitle;

	/**
	 * The potion data if this is a potion
	 */
	@Nullable
	private SimplePotionData potionData;

	/**
	 * The potion effects in case this is a potion
	 */
	private final List<PotionEffect> potionEffects = new ArrayList<>();

	/**
	 * The power of this firework
	 */
	private int fireworkPower = -1;

	/**
	 * The firework effects in case this is a firework
	 */
	private final List<FireworkEffect> fireworkEffects = new ArrayList<>();

	// ----------------------------------------------------------------------------------------
	// Builder methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Set the ItemStack for this item. We will reapply all other properties
	 * on this ItemStack, make sure they are compatible (such as skullOwner requiring a skull ItemStack, etc.)
	 *
	 * @param item
	 * @return
	 */
	public ItemCreator item(ItemStack item) {
		this.item = item;

		return this;
	}

	/**
	 * Set the ItemMeta we use to start building. All other properties in this
	 * class will build on this meta and take priority.
	 *
	 * @param meta
	 * @return
	 */
	public ItemCreator meta(ItemMeta meta) {
		this.meta = meta;

		return this;
	}

	/**
	 * Set the Material for the item. If {@link #item} is set,
	 * this material will take priority.
	 *
	 * @param material
	 * @return
	 */
	public ItemCreator material(CompMaterial material) {
		this.material = material;

		return this;
	}

	/**
	 * Set the amount of ItemStack to create.
	 *
	 * @param amount
	 * @return
	 */
	public ItemCreator amount(int amount) {
		this.amount = amount;

		return this;
	}

	/**
	 * Set the damage to the ItemStack. Notice that this only
	 * works for certain items, such as tools.
	 *
	 * See {@link Damageable#setDamage(int)}
	 *
	 * @param damage
	 * @return
	 */
	public ItemCreator damage(int damage) {
		this.damage = damage;

		return this;
	}

	/**
	 * Set the inventory slot the ItemStack will be placed in when calling
	 * {@link #give(org.bukkit.entity.Player)}
	 *
	 * @param slot
	 * @return
	 */
	public ItemCreator slot(int slot) {
		this.slot = slot;

		return this;
	}

	/**
	 * Set a custom name for the item (& color codes are replaced automatically).
	 *
	 * @param name
	 * @return
	 */
	public ItemCreator name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Remove any previous lore from the item. Useful if you initiated this
	 * class with an ItemStack or set {@link #item} already, to clear old lore off of it.
	 *
	 * @return
	 */
	public ItemCreator clearLore() {
		this.lores.clear();

		return this;
	}

	/**
	 * Append the given lore to the end of existing item lore.
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}

	/**
	 * Append the given lore to the end of existing item lore.
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(List<String> lore) {
		this.lores.addAll(lore);

		return this;
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment) {
		return this.enchant(enchantment, 1);
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment, int level) {
		this.enchants.add(new SimpleEnchant(enchantment, level));

		return this;
	}

	/**
	 * Add the given flags to the item.
	 *
	 * @param flags
	 * @return
	 */
	public ItemCreator flags(CompItemFlag... flags) {
		this.flags.addAll(Arrays.asList(flags));

		return this;
	}

	/**
	 * Add the given flag to the item.
	 *
	 * @param flag
	 * @return
	 */
	public ItemCreator flag(CompItemFlag flag) {
		this.flags.add(flag);

		return this;
	}

	/**
	 * Add the given banner patterns to the item, only works if it's a banner.
	 *
	 * @param color
	 * @param type
	 * @return
	 */
	public ItemCreator pattern(DyeColor color, PatternType type) {
		this.patterns.add(new Pattern(color, type));

		return this;
	}

	/**
	 * Add the given banner patterns to the item, only works if it's a banner.
	 *
	 * @param patterns
	 * @return
	 */
	public ItemCreator patterns(Pattern... patterns) {
		return this.patterns(Arrays.asList(patterns));
	}

	/**
	 * Add the given banner patterns to the item, only works if it's a banner.
	 *
	 * @param patterns
	 * @return
	 */
	public ItemCreator patterns(List<Pattern> patterns) {
		this.patterns.addAll(patterns);

		return this;
	}

	/**
	 * Set the item to be unbreakable.
	 *
	 * @param unbreakable
	 * @return
	 */
	public ItemCreator unbreakable(boolean unbreakable) {
		this.unbreakable = unbreakable;

		return this;
	}

	/**
	 * Set the stained or dye color in case your item is either of {@link LeatherArmorMeta},
	 * or from a selected list of compatible items such as stained glass, wool, etc.
	 *
	 * @param color
	 * @return
	 */
	public ItemCreator color(CompColor color) {
		this.color = color;

		return this;
	}

	/**
	 * Removes all enchantment, attribute and other tags appended
	 * at the end of item lore, typically with blue color.
	 *
	 * @param hideTags
	 * @return
	 */
	public ItemCreator hideTags(boolean hideTags) {
		this.hideTags = hideTags;

		return this;
	}

	/**
	 * Makes this item glow. Ignored if enchantments exists. Call {@link #hideTags(boolean)}
	 * to hide enchantment lores instead.
	 *
	 * @param glow
	 * @return
	 */
	public ItemCreator glow(boolean glow) {
		this.glow = glow;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * See {@link SkullCreator}
	 *
	 * @param skullOwner
	 * @return
	 */
	public ItemCreator skullOwner(String skullOwner) {
		this.skullOwner = skullOwner;

		return this;
	}

	/**
	 * Set the custom skull skin in base64 for this item, only works
	 * if the item is a skull.
	 *
	 * See {@link SkullCreator}
	 *
	 * @param skullSkin
	 * @return
	 */
	public ItemCreator skullSkin(String skullSkin) {
		this.skullSkin = skullSkin;

		return this;
	}

	/**
	 * Places an invisible custom tag to the item, for most server instances it
	 * will persist across saves/restarts (you should check just to be safe).
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public ItemCreator tag(String key, String value) {
		this.tags.put(key, value);

		return this;
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(BaseComponent[]... pages) {
		return this.bookPages(Arrays.asList(pages));
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(List<BaseComponent[]> pages) {
		this.bookPages.addAll(pages);

		return this;
	}

	/**
	 * If this is a book, set its author.
	 *
	 * @param bookAuthor
	 * @return
	 */
	public ItemCreator bookAuthor(String bookAuthor) {
		this.bookAuthor = bookAuthor;

		return this;
	}

	/**
	 * If this is a book, set its title.
	 *
	 * @param bookTitle
	 * @return
	 */
	public ItemCreator bookTitle(String bookTitle) {
		this.bookTitle = bookTitle;

		return this;
	}

	/**
	 * Set the potion data, only works if the item is a potion.
	 *
	 * @param potionData
	 * @return
	 */
	public ItemCreator potionData(SimplePotionData potionData) {
		this.potionData = potionData;

		return this;
	}

	/**
	 * Add the given potion effects to this item, only works if it's a potion
	 *
	 * @param effects
	 * @return
	 */
	public ItemCreator potionEffects(PotionEffect... effects) {
		return this.potionEffects(Arrays.asList(effects));
	}

	/**
	 * Add the given potion effects to this item, only works if it's a potion
	 *
	 * @param effects
	 * @return
	 */
	public ItemCreator potionEffects(List<PotionEffect> effects) {
		this.potionEffects.addAll(effects);

		return this;
	}

	/**
	 * Set the power of the firework, that changes the flight time of it
	 *
	 * @param fireworkPower
	 * @return
	 */
	public ItemCreator fireworkPower(int fireworkPower) {
		this.fireworkPower = fireworkPower;

		return this;
	}

	/**
	 * Add the given firework effects to this item, only works if it's a firework
	 *
	 * @param effects
	 * @return
	 */
	public ItemCreator fireworkEffects(FireworkEffect... effects) {
		return this.fireworkEffects(Arrays.asList(effects));
	}

	/**
	 * Add the given firework effects to this item, only works if it's a firework
	 *
	 * @param effects
	 * @return
	 */
	public ItemCreator fireworkEffects(List<FireworkEffect> effects) {
		this.fireworkEffects.addAll(effects);

		return this;
	}

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
		// Avoid calling Valid#checkBoolean because it will serialize this creator even if slot != -1
		if (this.slot == -1)
			throw new FoException("Slot is not defined in " + serialize().toStringFormatted());

		give(player, this.slot);
	}

	/**
	 * Adds this item into the player's inventory at the given slot.
	 * <p>
	 * See {@link org.bukkit.inventory.PlayerInventory#setItem(int, ItemStack)}
	 * for information on slots.
	 *
	 * @param player
	 * @param slot
	 */
	public void give(final Player player, final int slot) {
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
	 *
	 * @return
	 */
	public ItemCreator replaceVariables(@NonNull final Function<String, String> replacer) {
		final ItemCreator creator = this.copy();

		if (this.name != null)
			creator.name = replacer.apply(this.name);

		if (!this.lores.isEmpty()) {
			final String joined = replacer.apply(String.join("\n", this.lores));

			// Replace the variables again if the replacements had placeholders in them
			final String[] split = StringUtils.splitPreserveAllTokens(replacer.apply(joined), '\n');

			creator.clearLore().lore(split);
		}

		return creator;
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
	 * @return the new menu tool with all attributes hidden
	 */
	public ItemStack makeMenuTool() {
		this.hideTags = true;

		return this.make();
	}

	/**
	 * Construct a valid {@link ItemStack} from all parameters of this class.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {

		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		Valid.checkBoolean(this.material != null || this.item != null, "Material or item must be set!");

		ItemStack compiledItem = this.item != null ? this.item.clone() : this.material.toItem();

		Object compiledMeta = Remain.hasItemMeta() ? this.meta != null ? this.meta.clone() : compiledItem.getItemMeta() : null;

		// Override with given material
		if (this.material != null) {
			compiledItem.setType(this.material.getMaterial());

			if (MinecraftVersion.olderThan(V.v1_13))
				compiledItem.setData(new MaterialData(this.material.getMaterial(), this.material.getData()));
		}

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Apply specific material color if possible
		color:
		if (this.color != null) {

			if (compiledItem.getType().toString().contains("LEATHER")) {
				if (MinecraftVersion.atLeast(V.v1_4)) {
					Valid.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);

					((LeatherArmorMeta) compiledMeta).setColor(this.color.getColor());
				}
			} else {

				// Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
				final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_CLAY",
						"STAINED_GLASS", "STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

				if (MinecraftVersion.atLeast(V.v1_13)) {
					final String dye = color.getDye().toString();

					for (final String material : colorableMaterials) {
						final String suffix = "_" + material;

						if (compiledItem.getType().toString().endsWith(suffix)) {
							compiledItem.setType(Material.valueOf(dye + suffix));

							break color;
						}
					}

				} else {
					for (String material : colorableMaterials)
						if (compiledItem.getType().toString().endsWith(material)) {
							final byte dataValue = color.getDye().getWoolData();

							compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
							compiledItem.setDurability(dataValue);

							break;
						}
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

				if (entityRaw.equals("MONSTER_EGG") && this.material != null && this.material.toString().endsWith("SPAWN_EGG"))
					entityRaw = this.material.toString().replace("_SPAWN_EGG", "");

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

		if (damage != -1) {

			try {
				ReflectionUtil.invoke("setDurability", compiledItem, (short) this.damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(this.damage);
			} catch (final Throwable t) {
			}
		}

		if (skullOwner != null && compiledMeta instanceof SkullMeta)
			((SkullMeta) compiledMeta).setOwner(skullOwner);

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (!this.bookPages.isEmpty())
				Remain.setPages(bookMeta, bookPages);

			if (this.bookAuthor != null)
				bookMeta.setAuthor(Common.colorize(bookAuthor));

			if (this.bookTitle != null)
				bookMeta.setTitle(Common.colorize(bookTitle));

			// Fix "Corrupted NBT tag" error when any of these fields are not set
			if (bookMeta.getPages() == null)
				bookMeta.setPages("");

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor("Anonymous");

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle("Book");
		}

		if (!patterns.isEmpty() && compiledMeta instanceof BannerMeta)
			for (final Pattern pattern : patterns)
				((BannerMeta) compiledMeta).addPattern(pattern);

		if (compiledMeta instanceof FireworkMeta) {
			final FireworkMeta fireworkMeta = (FireworkMeta) compiledMeta;

			if (fireworkPower != -1)
				fireworkMeta.setPower(fireworkPower);

			if (!fireworkEffects.isEmpty())
				fireworkMeta.addEffects(fireworkEffects);
		}

		if (compiledMeta instanceof ItemMeta) {
			if (glow) {
				((ItemMeta) compiledMeta).addEnchant(Enchantment.DURABILITY, 1, true);

				this.flags.add(CompItemFlag.HIDE_ENCHANTS);
			}

			if (!enchants.isEmpty())
				for (final SimpleEnchant enchant : enchants)
					if (compiledMeta instanceof EnchantmentStorageMeta)
						((EnchantmentStorageMeta) compiledMeta).addStoredEnchant(enchant.getEnchant(), enchant.getLevel(), true);

					else
						((ItemMeta) compiledMeta).addEnchant(enchant.getEnchant(), enchant.getLevel(), true);

			if (!potionEffects.isEmpty() && compiledMeta instanceof PotionMeta)
				for (final PotionEffect effect : potionEffects)
					((PotionMeta) compiledMeta).addCustomEffect(effect, true);

			if (this.name != null && !"".equals(this.name))
				((ItemMeta) compiledMeta).setDisplayName(Common.colorize("&r&f" + name));

			if (!this.lores.isEmpty()) {
				final List<String> coloredLores = new ArrayList<>();

				for (final String lore : this.lores)
					if (lore != null)
						for (final String subLore : lore.split("\n"))
							coloredLores.add(Common.colorize("&7" + subLore));

				((ItemMeta) compiledMeta).setLore(coloredLores);
			}
		}

		if (this.unbreakable) {
			this.flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			this.flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			CompProperty.UNBREAKABLE.apply(compiledMeta, true);
		}

		if (this.hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!this.flags.contains(f))
					this.flags.add(f);

		for (final CompItemFlag flag : this.flags)
			try {
				((ItemMeta) compiledMeta).addItemFlags(ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Set custom model data
		if (this.modelData != null && MinecraftVersion.atLeast(V.v1_14))
			try {
				((ItemMeta) compiledMeta).setCustomModelData(this.modelData);
			} catch (final Throwable t) {
			}

		// Override with custom amount if set
		if (this.amount != -1)
			compiledItem.setAmount(this.amount);

		// Apply Bukkit metadata
		if (compiledMeta instanceof ItemMeta)
			compiledItem.setItemMeta((ItemMeta) compiledMeta);

		//
		// From now on we have to re-set the item
		//

		// Apply custom enchantment lores
		compiledItem = Common.getOrDefault(SimpleEnchantment.addEnchantmentLores(compiledItem), compiledItem);

		// Apply the skull skin AFTER we applied the item meta
		if (skullSkin != null && compiledMeta instanceof SkullMeta)
			compiledItem = SkullCreator.itemWithBase64(compiledItem, skullSkin);

		// SimplePotionData#apply also gets the ItemMeta from the item
		if (potionData != null && compiledMeta instanceof PotionMeta)
			potionData.apply(compiledItem);

		// 1.7.10 hack to add glow, requires no enchants
		if (this.glow && MinecraftVersion.equals(V.v1_7) && this.enchants.isEmpty()) {
			final NBTItem nbtItem = new NBTItem(compiledItem);

			nbtItem.removeKey("ench");
			nbtItem.addCompound("ench");

			compiledItem = nbtItem.getItem();
		}

		// Apply NBT tags
		if (MinecraftVersion.atLeast(V.v1_8))
			for (final Entry<String, String> entry : tags.entrySet())
				compiledItem = CompMetadata.setMetadata(compiledItem, entry.getKey(), entry.getValue());

		else if (!tags.isEmpty() && item != null)
			Common.log("Item had unsupported tags " + tags + " that are not supported on MC " + MinecraftVersion.getServerVersion() + " Item: " + compiledItem);

		return compiledItem;
	}

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {

		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		Valid.checkBoolean(material != null || item != null, "Material or item must be set!");

		if (material != null)
			Valid.checkNotNull(material.getMaterial(), "Material#getMaterial cannot be null for " + material);

		final SerializedMap map = new SerializedMap();
		final CompMaterial mat = material != null ? material : CompMaterial.fromItem(item);

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

		if (!lores.isEmpty())
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

			if (!potionEffects.isEmpty()) {
				final SerializedMap effectsMap = new SerializedMap();

				for (final PotionEffect effect : potionEffects) {
					final SerializedMap effectMap = new SerializedMap();

					effectMap.put("Duration", effect.getDuration() * 20);
					effectMap.put("Amplifier", effect.getAmplifier());

					effectsMap.put(effect.getType().getName(), effectMap);
				}
			}
		}

		if (meta instanceof EnchantmentStorageMeta && !enchants.isEmpty()) {
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

		if (!patterns.isEmpty() && meta instanceof BannerMeta) {
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

			if (!fireworkEffects.isEmpty())
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
		if (!(meta instanceof EnchantmentStorageMeta) && !enchants.isEmpty()) {
			final SerializedMap enchantsMap = new SerializedMap();

			for (final SimpleEnchant enchant : enchants)
				enchantsMap.put(enchant.getEnchant().getName(), enchant.getLevel());

			map.put("Enchantments", enchantsMap);
		}

		// Flags
		if (!flags.isEmpty())
			map.put("Flags", flags);

		// Unbreakable
		if (unbreakable)
			map.put("Unbreakable", unbreakable);

		return map;
	}

	/**
	 * Makes a new ItemCreator with all attributes copied into it
	 *
	 * @return
	 */
	public ItemCreator copy() {
		final ItemCreator creator = new ItemCreator();

		creator.item = this.item != null ? this.item.clone() : null;
		creator.meta = this.meta != null ? this.meta.clone() : null;
		creator.material = material;
		creator.amount = amount;
		creator.damage = damage;
		creator.slot = slot;
		creator.name = name;

		creator.lore(this.lores);
		creator.enchants.addAll(this.enchants);
		creator.flags.addAll(this.flags);
		creator.patterns.addAll(this.patterns);

		creator.unbreakable = this.unbreakable;
		creator.color = this.color;
		creator.hideTags = this.hideTags;
		creator.skullOwner = this.skullOwner;
		creator.skullSkin = this.skullSkin;

		creator.tags.putAll(this.tags);
		creator.bookPages.addAll(this.bookPages);
		creator.bookAuthor = this.bookAuthor;
		creator.bookTitle = this.bookTitle;

		creator.potionData = this.potionData;
		creator.potionEffects.addAll(this.potionEffects);

		creator.fireworkPower = this.fireworkPower;
		creator.fireworkEffects.addAll(this.fireworkEffects);

		return creator;
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
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, Common.toArray(lore));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final String... lore) {
		return new ItemCreator().material(material).name(name).lore(lore).hideTags(true);
	}

	/**
	 * Convenience method to get a wool
	 *
	 * @param color the wool color
	 * @return the new item creator
	 */
	public static ItemCreator ofWool(final CompColor color) {
		return of(CompMaterial.makeWool(color, 1)).color(color);
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType) {
		return of(CompMonsterEgg.makeEgg(entityType));
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType, String name, String... lore) {
		return of(CompMonsterEgg.makeEgg(entityType)).name(name).lore(lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect) {
		return ofPotion(potionEffect, 1);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param durationTicks
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level) {
		return ofPotion(potionEffect, durationTicks, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int level) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, String name, String... lore) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param effect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffect effect, String name, String... lore) {
		return ofPotion(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier() + 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param level
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level, String name, String... lore) {
		final ItemStack item = new ItemStack(CompMaterial.POTION.getMaterial());
		Remain.setPotion(item, potionEffect, durationTicks, level);

		final ItemCreator builder = of(item);

		if (name != null)
			builder.name(name);

		if (lore != null)
			builder.lore(lore);

		return builder;
	}

	/**
	 * Convenience method to get the creator of an existing itemstack
	 *
	 * @param item existing itemstack
	 * @return the new item creator
	 */
	public static ItemCreator of(final ItemStack item) {
		final ItemCreator builder = new ItemCreator();
		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.getLore() != null)
			builder.lore(meta.getLore());

		return builder.item(item);
	}

	/**
	 * Get a new item creator from material
	 *
	 * @param mat existing material
	 * @return the new item creator
	 */
	public static ItemCreator of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return new ItemCreator().material(mat);
	}

	/**
	 * Deserializes an item from the given serialized map
	 *
	 * @param map
	 *
	 * @return
	 */
	public static ItemCreator of(final SerializedMap map) {
		final CompMaterial material = map.getMaterial("Material");
		final ItemCreator builder = new ItemCreator();

		if (material != null)
			builder.material(material);

		if (map.containsKey("Amount"))
			builder.amount(map.getInteger("Amount"));

		// Player inventory slot
		if (map.containsKey("Inventory_Slot"))
			builder.slot(map.getInteger("Inventory_Slot"));

		// Display
		builder.name(map.getString("Name"));

		if (map.containsKey("Lore"))
			builder.lore(map.getStringList("Lore"));

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
						type = PotionType.getByEffect(SerializeUtil.deserialize(PotionEffectType.class, rawType));

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
					final PotionEffectType potion = SerializeUtil.deserialize(PotionEffectType.class, effectType);

					builder.potionEffects(new PotionEffect(potion, durationTicks, amplifier));

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
					final Enchantment enchantment = SerializeUtil.deserialize(Enchantment.class, enchantType);

					builder.enchant(enchantment, level);

				} catch (final Exception e) {
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

					builder.bookPages(components);

				} catch (final Exception e) {
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

					builder.patterns(new Pattern(color.getDye(), patternType));

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

					builder.fireworkEffects(FireworkEffect.builder()
							.with(type)
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
				final Enchantment enchant = SerializeUtil.deserialize(Enchantment.class, enchantType);
				final int level = enchants.getInteger(enchantType);

				builder.enchant(enchant, level);

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
		if (map.containsKey("Unbreakable"))
			builder.unbreakable(map.getBoolean("Unbreakable"));

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
	 *
	 * @return
	 */
	public static ItemCreator deserialize(final SerializedMap map) {
		return of(map);
	}
}