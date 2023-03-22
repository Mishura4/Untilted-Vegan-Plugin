package com.github.miunaoshino.untitledveganplugin;

import com.github.miunaoshino.untitledveganplugin.Core.EventListener;
import com.github.miunaoshino.untitledveganplugin.Core.UVPData;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.ConfigurationException;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class UntitledVeganPlugin extends JavaPlugin
{
  private static final int CONFIG_VERSION = 2;
  private static UntitledVeganPlugin INSTANCE;

  private static NamespacedKey fishingRodTypeKey;
  private static NamespacedKey fishingRodDataKey;
  private static NamespacedKey plantBasedPropertyKey;
  private static NamespacedKey feedingRodTableKey;
  private static NamespacedKey feedingRodRecipeKey;

  public static final Supplier<NamespacedKey> fishingRodType =
          () -> (fishingRodTypeKey == null ? fishingRodTypeKey = new NamespacedKey(INSTANCE, "fishing_rod_type") : fishingRodTypeKey);
  public static final Supplier<NamespacedKey> fishingRodData =
          () -> (fishingRodDataKey == null ? fishingRodDataKey = new NamespacedKey(INSTANCE, "fishing_rod_data") : fishingRodDataKey);

  public static final Supplier<NamespacedKey> feedingRodTable =
          () -> (feedingRodTableKey == null ? feedingRodTableKey = new NamespacedKey(INSTANCE, "gameplay/feeding") : feedingRodTableKey);

  public static final Supplier<NamespacedKey> plantBasedProperty =
          () -> (plantBasedPropertyKey == null ? plantBasedPropertyKey = new NamespacedKey(INSTANCE, "is_plant_based") : plantBasedPropertyKey);

  private EventListener eventListener;
  private UVPData data;

  public static UntitledVeganPlugin getInstance()
  {
    return (INSTANCE);
  }

  public UntitledVeganPlugin()
  {
    UntitledVeganPlugin.INSTANCE = this;
  }

  public static boolean isAFriend(LivingEntity friend, LivingEntity pov)
  {
    if (!isAFriend(friend.getClass()))
      return (false);
    if (friend instanceof Wolf)
      return (((Wolf)friend).isAngry() && ((Wolf)friend).getOwner() != pov);
    return (true);
  }

  public static boolean isAFriend(Class<? extends Entity> c)
  {
    if (c == null || c == EntityType.UNKNOWN.getEntityClass())
      return (false);
    return ((Animals.class.isAssignableFrom(c) && !Hoglin.class.isAssignableFrom(c)) ||
            Ambient.class.isAssignableFrom(c) ||
            Fish.class.isAssignableFrom(c) ||
            Squid.class.isAssignableFrom(c) ||
            Strider.class.isAssignableFrom(c));
  }

  public static boolean isPlantBased(ItemStack stack)
  {
    ItemMeta meta = stack.getItemMeta();
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    if (pdc == null)
      return (false);
    return (isPlantBased(pdc));
  }

  public static boolean isPlantBased(PersistentDataContainer pdc)
  {
    return (pdc.getOrDefault(plantBasedProperty.get(), PersistentDataType.BYTE, (byte)0) == (byte)1);
  }

  public static void setPlantBased(ItemStack stack)
  {
    ItemMeta meta = stack.getItemMeta();
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    setPlantBased(pdc);
  }

  public static void setPlantBased(PersistentDataContainer pdc)
  {
    pdc.set(UntitledVeganPlugin.plantBasedProperty.get(), PersistentDataType.BYTE, (byte)1);
  }

  public void registerFriendKill(LivingEntity friend, Player killer)
  {
    this.data.registerFriendKill(friend, killer);
  }

  public void savePlayerData()
  {
    this.data.savePlayerData();
  }

  @Override
  public void onEnable()
  {
    this.eventListener = new EventListener();
    this.data = new UVPData(getDataFolder());
    this.data.load();
    getServer().getPluginManager().registerEvents(this.eventListener, this);
  }

  @Override
  public void onDisable()
  {
    savePlayerData();
  }

  public static boolean isPotentialAnimalProduct(Material itemType)
  {
    if (_animalProductItems.contains(itemType))
      return (true);
    for (Tag<Material> animalTag : _animalProductTags)
    {
      if (animalTag.isTagged(itemType))
        return (true);
    }
    return (false);
  }

  private static final Set<Material> _animalProductItems = ImmutableSet.of(
          Material.BEEF,
          Material.CHICKEN,
          Material.PORKCHOP,
          Material.COD,
          Material.SALMON,
          Material.MILK_BUCKET,
          Material.WHITE_WOOL,
          Material.INK_SAC,
          Material.GLOW_INK_SAC,
          Material.HONEY_BOTTLE,
          Material.HONEYCOMB,
          Material.LEATHER,
          Material.FEATHER,
          Material.RABBIT_FOOT,
          Material.RABBIT_HIDE,
          Material.RABBIT_STEW,
          Material.SUSPICIOUS_STEW,
          Material.HONEY_BLOCK,
          Material.HONEYCOMB_BLOCK,
          Material.EGG,
          Material.COOKED_BEEF,
          Material.COOKED_SALMON,
          Material.COOKED_CHICKEN,
          Material.COOKED_COD,
          Material.COOKED_MUTTON,
          Material.COOKED_PORKCHOP,
          Material.COOKED_RABBIT,
          Material.MUTTON,
          Material.RABBIT
  );

  private static final Set<Tag<Material>> _animalProductTags = ImmutableSet.of(
          Tag.ITEMS_FISHES,
          Tag.WOOL,
          Tag.WOOL_CARPETS,
          Tag.CANDLES,
          Tag.CANDLE_CAKES
  );
}
