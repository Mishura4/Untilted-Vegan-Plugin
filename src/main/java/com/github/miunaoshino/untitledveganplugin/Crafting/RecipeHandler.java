package com.github.miunaoshino.untitledveganplugin.Crafting;

import com.github.miunaoshino.untitledveganplugin.Fishing.FishingHandler;
import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class RecipeHandler
{
  static private void replaceFishingRod(CraftingInventory inventory)
  {
    List<ItemStack> items = Arrays.stream(inventory.getMatrix()).filter(itemStack -> itemStack != null).collect(
            Collectors.toList());

    if (items.size() != 1)
      return;
    ItemStack rod = items.get(0);
    if (rod.getType() != Material.FISHING_ROD)
      return;
    ItemMeta pdh = rod.getItemMeta().clone();

    if (FishingHandler.getFishingRodType(pdh) == FishingHandler.FishingRodType.FEEDING_ROD)
    {
      pdh.getPersistentDataContainer().remove(
              UntitledVeganPlugin.fishingRodType.get());
      if (pdh.getDisplayName().contentEquals("Feeding rod"))
        pdh.setDisplayName(null);
      pdh.setLore(Collections.emptyList());
      inventory.getResult().setItemMeta(pdh);
    }
    else if (FishingHandler.getFishingRodType(pdh) == FishingHandler.FishingRodType.FISHING_ROD)
    {
      pdh.getPersistentDataContainer().set(UntitledVeganPlugin.fishingRodType.get(), PersistentDataType.STRING, "feeding_rod");
      pdh.setDisplayName("Feeding rod");
      pdh.setLore(Arrays.asList("Feeding rod", "Friendly to the fish!"));
      inventory.getResult().setItemMeta(pdh);
    }
  }

  public static void onPreCraftItemEvent(PrepareItemCraftEvent e)
  {
    NamespacedKey key;
    Map<String, Consumer<CraftingInventory>> recipeMap;

    if (e.getRecipe() instanceof ShapelessRecipe)
    {
      key = ((ShapelessRecipe) e.getRecipe()).getKey();
      recipeMap = _recipeMap;
    }
    else if (e.getRecipe() instanceof ShapedRecipe)
    {
      key = ((ShapedRecipe) e.getRecipe()).getKey();
      recipeMap = _recipeMap;
    }
    else
      return;
    var handler = recipeMap.get(key.toString());

    if (handler != null) // We have a handler - this is a vegan recipe
    {
      handler.accept(e.getInventory());
      return;
    }
    // No handler - this is an external recipe
    // We check every single item in the recipe against our map of tags and items that may be animal products
    // If they are animal products, we check that they have the PB property, if every animal product has it, we mark as plant-based
    // Otherwise return early, we don't do anything

    int animalProductsCount = 0;
    for (ItemStack item : e.getInventory().getMatrix())
    {
      if (item == null)
        continue;
      if (UntitledVeganPlugin.isPotentialAnimalProduct(item.getType()))
      {
        ++animalProductsCount;
        if (!UntitledVeganPlugin.isPlantBased(item))
          return;
      }
    }
    if (animalProductsCount == 0) // Not a recipe that uses animal products
      return;
    ItemStack result = e.getInventory().getResult();
    ItemMeta meta = result.getItemMeta();

    UntitledVeganPlugin.setPlantBased(meta.getPersistentDataContainer());
    meta.setLore(ImmutableList.of("Cruelty-free!"));
    result.setItemMeta(meta);
  }

  public static void onCraftItemEvent(CraftItemEvent e)
  {
  }

  @EventHandler
  static public void onFurnaceSmeltEvent(FurnaceSmeltEvent e)
  {
    Map.Entry<Material, Material> key = new AbstractMap.SimpleImmutableEntry<>(e.getSource().getType(), e.getResult().getType());
    var handler = _furnaceRecipeMap.get(key);

    UntitledVeganPlugin.getInstance().getLogger().warning("My handler is " + String.valueOf(handler));
    if (handler == null) // No handler for this recipe
      return;
    handler.accept(e.getSource(), e.getResult());
  }

  @EventHandler
  static public void onFurnaceStartSmeltEvent(FurnaceStartSmeltEvent e)
  {
//    NamespacedKey key = (e.getRecipe()).getKey();
//    var handler = _furnaceRecipeMap.get(key.toString());
//
//    if (handler == null) // No handler for this recipe
//      return;
//    handler.accept(e.getSource(), e.ge);
  }

  private static void setNameAndComment(ItemStack item, String name, List<String> comments)
  {

  }

  private static void replaceInfo(ItemStack itemStack, String name, List<String> lore)
  {
    ItemMeta meta = itemStack.getItemMeta();
    meta.setDisplayName(name);
    meta.setLore(lore);
    UntitledVeganPlugin.setPlantBased(meta.getPersistentDataContainer());
    itemStack.setItemMeta(meta);
  }
  private static void replaceInfo(ItemStack itemStack, String name, String lore)
  {
    replaceInfo(itemStack, name, ImmutableList.of(lore));
  }
  private static void replaceInfo(ItemStack itemStack, String name)
  {
    replaceInfo(itemStack, name, Collections.emptyList());
  }

  private static void rawBeefSeitan(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Raw Beef Seitan", "Not tested on animals!");
  }

  private static void rawPorkSeitan(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Raw Pork Seitan", "Only 150% of daily Sodium intake");
  }

  private static void rawSalmonSeitan(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Raw Salmon Seitan", "Uncanny color!");
  }

  private static void rawCodSeitan(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Raw Cod Seitan", "Not very popular in cafeterias");
  }

  private static void feather(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Artificial Feather", "Chickens love it!");
  }

  private static void inkSac(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Ink Bottle", "Keep away from fire");
  }

  private static void glowInkSac(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Glow Ink Sac", "No, the powder is NOT nooch!");
  }

  private static void mushroomLeather(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Mushroom Leather", "Made with mushrooms!");
  }

  private static void honeycomb(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Berry Wax", "Pretend the berries were orange");
  }

  private static void honeyBottle(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Berry Syrup Bottle", "Delicious liquid sugar!");
  }

  private static void milk(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Wheat Milk", "Gluten-free! Maybe.");
  }

  private static void zombieLeather(ItemStack itemStack)
  {
    replaceInfo(itemStack, "Zombie Leather", "Is killing the dead ethical?");
  }

  private static void woolBase(ItemStack itemStack)
  {
    replaceInfo(itemStack, "(Not) Wool", "Questionably sourced");
  }

  private static void mushroomArmor(CraftingInventory inventory, String name)
  {
    ItemStack commonItem = null;
    boolean allSame = true;
    for (ItemStack item : inventory.getMatrix())
    {
      if (item == null)
        continue;
      if (commonItem == null)
        commonItem = item;
      else if (allSame && !(item.isSimilar(commonItem)))
        allSame = false;
      if (!UntitledVeganPlugin.isPlantBased(item))
        return;
    }
    ItemStack result = inventory.getResult();
    ItemMeta resultMeta = result.getItemMeta();
    if (allSame)
    {
      ItemMeta commonMeta = commonItem.getItemMeta();
      resultMeta.setDisplayName(commonMeta.getDisplayName() + ' ' + name);
      resultMeta.setLore(commonMeta.getLore());
    }
    else
      resultMeta.setLore(ImmutableList.of("Cruelty-free!"));
    UntitledVeganPlugin.setPlantBased(resultMeta.getPersistentDataContainer());
    result.setItemMeta(resultMeta);
  }

  private static void veganizeFurnace(ItemStack source, ItemStack result)
  {
    if (UntitledVeganPlugin.isPlantBased(source))
    {
      ItemMeta inMeta = source.getItemMeta();
      ItemMeta outMeta = result.getItemMeta();
      PersistentDataContainer outPdc = outMeta.getPersistentDataContainer();

      if (inMeta.getDisplayName().startsWith("Raw "))
        outMeta.setDisplayName(inMeta.getDisplayName().substring(4));
      outMeta.setLore(inMeta.getLore());
      outPdc.set(UntitledVeganPlugin.plantBasedProperty.get(), PersistentDataType.BYTE, (byte)1);
      result.setItemMeta(outMeta);
    }
  }

  private static final Map<String, Consumer<CraftingInventory>> _recipeMap =
    ImmutableMap.ofEntries(
      Map.entry("untitledveganplugin:fishing_rod", (CraftingInventory inventory) -> replaceFishingRod(inventory)),
      Map.entry("untitledveganplugin:leather_mushroom", (CraftingInventory inventory) -> mushroomLeather(inventory.getResult())),
      Map.entry("untitledveganplugin:leather", (CraftingInventory inventory) -> zombieLeather(inventory.getResult())),
      Map.entry("untitledveganplugin:wool", (CraftingInventory inventory) -> woolBase(inventory.getResult())),
      Map.entry("minecraft:white_wool_from_string", (CraftingInventory inventory) -> woolBase(inventory.getResult())),
      Map.entry("untitledveganplugin:beef", (CraftingInventory inventory) -> rawBeefSeitan(inventory.getResult())),
      Map.entry("untitledveganplugin:pork", (CraftingInventory inventory) -> rawPorkSeitan(inventory.getResult())),
      Map.entry("untitledveganplugin:salmon", (CraftingInventory inventory) -> rawSalmonSeitan(inventory.getResult())),
      Map.entry("untitledveganplugin:cod", (CraftingInventory inventory) -> rawCodSeitan(inventory.getResult())),
      Map.entry("untitledveganplugin:milk", (CraftingInventory inventory) -> milk(inventory.getResult())),
      Map.entry("untitledveganplugin:honeycomb", (CraftingInventory inventory) -> honeycomb(inventory.getResult())),
      Map.entry("untitledveganplugin:honey_bottle", (CraftingInventory inventory) -> honeyBottle(inventory.getResult())),
      Map.entry("untitledveganplugin:feather", (CraftingInventory inventory) -> feather(inventory.getResult())),
      Map.entry("untitledveganplugin:ink_sac", (CraftingInventory inventory) -> inkSac(inventory.getResult())),
      Map.entry("untitledveganplugin:glow_ink_sac", (CraftingInventory inventory) -> glowInkSac(inventory.getResult())),
      Map.entry("minecraft:leather_helmet", (CraftingInventory inventory) -> mushroomArmor(inventory, "Cap")),
      Map.entry("minecraft:leather_leggings", (CraftingInventory inventory) -> mushroomArmor(inventory, "Pants")),
      Map.entry("minecraft:leather_chestplate", (CraftingInventory inventory) -> mushroomArmor(inventory, "Tunic")),
      Map.entry("minecraft:leather_boots", (CraftingInventory inventory) -> mushroomArmor(inventory, "Boots"))
    );

  private static final Map<Map.Entry<Material, Material>, BiConsumer<ItemStack, ItemStack>> _furnaceRecipeMap =
    ImmutableMap.ofEntries(
      Map.entry(Map.entry(Material.BEEF, Material.COOKED_BEEF), (ItemStack source, ItemStack result) -> veganizeFurnace(source, result)),
      Map.entry(Map.entry(Material.SALMON, Material.COOKED_SALMON), (ItemStack source, ItemStack result) -> veganizeFurnace(source, result))
    );
}
