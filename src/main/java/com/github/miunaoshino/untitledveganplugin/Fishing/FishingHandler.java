package com.github.miunaoshino.untitledveganplugin.Fishing;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class FishingHandler
{
  public enum FishingRodType
  {
    FISHING_ROD("fishing_rod"),
    FEEDING_ROD("feeding_rod");

    public final String value;

    FishingRodType(String _value)
    {
      this.value = _value;
    }

    public static FishingRodType fromString(String string)
    {
      return (Arrays.stream(FishingRodType.values())
	      .filter(a -> a.value.equals(string))
	      .findFirst()
	      .orElse(FishingRodType.FISHING_ROD));
    }
  }

  public static boolean isPlayerHoldingFeedingRod(Player player)
  {
    PersistentDataHolder pdh;

    pdh = Optional.of(player.getInventory().getItemInMainHand())
            .filter(item -> item.getType().equals(Material.FISHING_ROD))
            .orElse(player.getInventory().getItemInOffHand())
            .getItemMeta();
    return (getFishingRodType(pdh) == FishingRodType.FEEDING_ROD);
  }

  public static FishingRodType getFishingRodType(PersistentDataHolder pdh)
  {
    String fishingRodTypeName = pdh.getPersistentDataContainer().get(UntitledVeganPlugin.fishingRodType.get(), PersistentDataType.STRING);

    return (FishingRodType.fromString(fishingRodTypeName));
  }

  public static void onPlayerFish(PlayerFishEvent e)
  {
    Player player = e.getPlayer();
    Entity entity = e.getCaught(); // Entity caught by the player, Entity if fishing, and null if bobber has gotten stuck in the ground or nothing has been caught

    if (player == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Player is null during fish event");
      return;
    }
    PersistentDataHolder pdh;

    if (e.getState() == PlayerFishEvent.State.FISHING)
    {
      PlayerInventory inv = player.getInventory();
      pdh = Optional.of(inv.getItemInMainHand())
              .filter(item -> item.getType().equals(Material.FISHING_ROD))
              .orElse(inv.getItemInOffHand())
              .getItemMeta();
    }
    else
      pdh = e.getHook();

    FishingRodType type = getFishingRodType(pdh);

    switch (e.getState())
    {
      case FISHING -> {
	if (type == FishingRodType.FEEDING_ROD)
        {
          ItemMeta rodMeta = (ItemMeta)pdh;

          PersistentDataContainer pdc = e.getHook().getPersistentDataContainer();
          pdc.set(UntitledVeganPlugin.fishingRodType.get(), PersistentDataType.STRING, type.value);
          pdc.set(UntitledVeganPlugin.fishingRodData.get(), PersistentDataType.INTEGER, rodMeta.getEnchantLevel(Enchantment.LUCK) + (int)e.getPlayer().getAttribute(Attribute.GENERIC_LUCK).getValue());
        }
      }

      case CAUGHT_FISH -> {
	if (type == FishingRodType.FISHING_ROD)
        {
          Punisher.punishForMurder(player);
          break;
        }
        if (type == FishingRodType.FEEDING_ROD)
        {
          LootTable table = UntitledVeganPlugin.getInstance().getServer().getLootTable(UntitledVeganPlugin.feedingRodTable.get());

          if (table == null)
            return;

          int luck = pdh.getPersistentDataContainer().get(UntitledVeganPlugin.fishingRodData.get(), PersistentDataType.INTEGER);

          LootContext context = new LootContext.Builder(player.getLocation()).lootedEntity(e.getHook()).killer(player).luck(luck).build();

          Collection<ItemStack> newLoots = table.populateLoot(new Random(), context);
          ((Item)e.getCaught()).setItemStack(newLoots.stream().findFirst().get());
          UntitledVeganPlugin.getInstance().getLogger().info("Fishing with luck " + luck + ": loots were changed to " + newLoots.toString());
        }
      }
    }
  }
}
