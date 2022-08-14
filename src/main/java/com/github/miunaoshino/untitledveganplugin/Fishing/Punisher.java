package com.github.miunaoshino.untitledveganplugin.Fishing;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class Punisher
{
  public static class MurderPunishment implements Runnable
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

  public static void punishForMurder(LivingEntity murderer)
  {
    Random rand = new Random();
    Bukkit.getScheduler().runTaskLater(UntitledVeganPlugin.getInstance(),
	    new Punisher.MurderPunishment(murderer),
	    rand.nextInt(110) + 10);
  }
}
