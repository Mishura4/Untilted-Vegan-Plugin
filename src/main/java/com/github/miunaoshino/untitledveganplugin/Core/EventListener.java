package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.Crafting.RecipeHandler;
import com.github.miunaoshino.untitledveganplugin.Fishing.FishingHandler;
import com.github.miunaoshino.untitledveganplugin.Fishing.Punisher;
import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.event.ItemEvent;
import java.util.*;
import java.util.stream.Collectors;

public class EventListener implements Listener
{
  public void replaceFishingRod(CraftingInventory inventory)
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
      pdh.getPersistentDataContainer().remove(UntitledVeganPlugin.fishingRodType.get());
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

  @EventHandler
  public void onPreCraftItemEvent(PrepareItemCraftEvent e)
  {
    RecipeHandler.onPreCraftItemEvent(e);
  }

  @EventHandler
  public void onCraftItemEvent(CraftItemEvent e)
  {
    RecipeHandler.onCraftItemEvent(e);
  }

  @EventHandler
  public void onFurnaceSmeltEvent(FurnaceSmeltEvent e)
  {
    RecipeHandler.onFurnaceSmeltEvent(e);
  }

  @EventHandler
  public void onFurnaceStartSmeltEvent(FurnaceStartSmeltEvent e)
  {
    RecipeHandler.onFurnaceStartSmeltEvent(e);
  }

  @EventHandler
  public void onChunkLoadEvent(ChunkLoadEvent e)
  {
    ChunkHandler.deserializeChunkData(e.getChunk());
  }

  @EventHandler
  public void onChunkUnloadEvent(ChunkUnloadEvent e)
  {
    if (!e.isSaveChunk())
    {
      return;
    }
    ChunkHandler.serializeChunkData(e.getChunk());
    ChunkHandler.unloadChunkData(e.getChunk());
  }

  @EventHandler
  public void onBlockDropItemEvent(BlockDropItemEvent e)
  {
    ChunkHandler.handleBlockDropItemEvent(e);
  }

  @EventHandler
  public void onBlockPlacedEvent(BlockPlaceEvent e)
  {
    ItemStack itemPlaced = e.getItemInHand();
    if (!UntitledVeganPlugin.isPlantBased(itemPlaced))
      return;
    ChunkHandler.registerVeganBlockPlaced(e);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e)
  {
//    if (e.getItem() == null)
//    {
//      UntitledVeganPlugin.getInstance().getLogger().info("Giving");
//      ItemStack stack = new ItemStack(Material.FISHING_ROD, 1);
//      ItemMeta meta = stack.getItemMeta().clone();
//      meta.getPersistentDataContainer().set(UntitledVeganPlugin.fishingRodType.get(), PersistentDataType.STRING, "feeding_rod");
//      meta.setDisplayName("Feeding rod");
//      meta.addEnchant(Enchantment.LUCK, 3, true);
//      stack.setItemMeta(meta);
//      e.getPlayer().getInventory().addItem(stack);
//    }
  }

  @EventHandler
  public void onPlayerFish(PlayerFishEvent e)
  {
    FishingHandler.onPlayerFish(e);
  }

  @EventHandler
  public void onWorldSave(WorldSaveEvent e)
  {
    UntitledVeganPlugin.getInstance().savePlayerData();
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent e)
  {
    LivingEntity entity = e.getEntity();
    LivingEntity killer;


    if ((killer = entity.getKiller()) == null)
      return;

    if (killer instanceof Tameable)
    {
      killer = (LivingEntity) ((Tameable) killer).getOwner();
      if (killer == null)
        return;
    }

    if (!UntitledVeganPlugin.isAFriend(entity, killer))
      return;

    if (!killer.isInvulnerable())
    {
      if (killer instanceof Player)
      {
        if (((Player) killer).getGameMode() != GameMode.SURVIVAL) return;
        UntitledVeganPlugin.getInstance()
                           .registerFriendKill(entity, (Player) killer);
      }
      UntitledVeganPlugin.getInstance().getLogger().info(killer.getName() + " killed " + entity.getName() + ", a friend");
      Punisher.punishForMurder(killer);
    }
  }
}
