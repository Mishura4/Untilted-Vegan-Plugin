package com.github.miunaoshino.untitledveganplugin;

import com.github.miunaoshino.untitledveganplugin.Core.EventListener;
import com.github.miunaoshino.untitledveganplugin.Core.UVPData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.ConfigurationException;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

public class UntitledVeganPlugin extends JavaPlugin
{
  private static final int CONFIG_VERSION = 2;
  private static UntitledVeganPlugin INSTANCE;

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
}
