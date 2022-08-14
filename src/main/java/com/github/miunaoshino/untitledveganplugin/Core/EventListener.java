package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.Fishing.FishingHandler;
import com.github.miunaoshino.untitledveganplugin.Fishing.Punisher;
import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class EventListener implements Listener
{

  @EventHandler
  public void onLootGenerate(LootGenerateEvent e)
  {
    /*
          LootTable table = UntitledVeganPlugin.getInstance().getServer().getLootTable(UntitledVeganPlugin.feedingRodTable.get());
          LinkedList<ItemStack> drops = new LinkedList<>();

          LootContext context = new LootContext.Builder(player.getLocation())
                  .killer(player).build();
          table.populateLoot(new Random(), )
          e.setExpToDrop((int)Math.floor(e.getExpToDrop() * 1.5));
          ItemStack newDrop = new ItemStack(Material.SAND, 1);
          ((Item)e.getCaught()).setItemStack(newDrop);*/

    UntitledVeganPlugin.getInstance().getLogger().info("Loot table is " + e.getLootTable().getKey().toString());
    if (!e.getLootTable().equals(LootTables.FISHING))
      return;
    Entity entity = e.getEntity();
    UntitledVeganPlugin.getInstance().getLogger().info(entity.getName());
    if (!FishingHandler.isPlayerHoldingFeedingRod((Player)entity))
      return;
    if (e.isPlugin())
      return;
    UntitledVeganPlugin.getInstance().getLogger().info("it is fEEDING");
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e)
  {
    if (e.getItem() == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().info("Giving");
      ItemStack stack = new ItemStack(Material.FISHING_ROD, 1);
      ItemMeta meta = stack.getItemMeta().clone();
      meta.getPersistentDataContainer().set(UntitledVeganPlugin.fishingRodType.get(), PersistentDataType.STRING, "feeding_rod");
      meta.setDisplayName("Feeding rod");
      meta.addEnchant(Enchantment.LUCK, 3, true);
      stack.setItemMeta(meta);
      e.getPlayer().getInventory().addItem(stack);
    }
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
