package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class EventListener implements Listener
{
  public class MurderPunishment implements Runnable
  {
    private final LivingEntity entity;

    public MurderPunishment(LivingEntity _entity)
    {
      entity = _entity;
    }

    @Override
    public void run()
    {
      World worldIn = this.entity.getWorld();
      Location killerLocation = this.entity.getLocation();

      UntitledVeganPlugin.getInstance().getLogger().info("We are punishing " + this.entity.getName());
      if (worldIn.getHighestBlockAt(killerLocation).getLocation().getBlockY() <=
          killerLocation.getY()) // We are under the sky
      {
        UntitledVeganPlugin.getInstance().getLogger().info("Outside");
        this.entity.getWorld().strikeLightning(killerLocation);
        this.entity.damage(3.0);
        if (this.entity.isInWater())
          this.entity.damage(5.0);
        else
          this.entity.setFireTicks(100);
      }
      else // We are in a cave
      {
        worldIn.createExplosion(this.entity.getLocation(), 0, false, false);
        this.entity.damage(5.0 + 3.0);
        if (this.entity.isInWater())
          this.entity.damage(5.0);
        else
          this.entity.setFireTicks(100);
        UntitledVeganPlugin.getInstance().getLogger().info("In cave");
      }
    }
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
      Random rand = new Random();
      Bukkit.getScheduler().runTaskLater(UntitledVeganPlugin.getInstance(),
                                         new MurderPunishment(killer),
                                         rand.nextInt(110) + 10);
    }
  }
}
