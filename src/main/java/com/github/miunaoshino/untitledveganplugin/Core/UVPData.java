package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.bukkit.entity.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class UVPData
{
  private final File               playerDataFolder;
  private Map<UUID, UVPPlayerData> playerDataMap;

  public void registerFriendKill(LivingEntity friend, Player killer)
  {
    UUID id = killer.getUniqueId();
    UVPPlayerData data = this.playerDataMap.get(id);

    if (data == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().info("data for " + id.toString() + " was not found, adding");
      this.playerDataMap.put(id, data = new UVPPlayerData());
    }
    data.registerKill(friend);
    UntitledVeganPlugin.getInstance().getLogger().info(killer.getName() + " has killed " + data.getKillCount(friend) + " " + friend.getName());
  }

  public static class DataParseException extends Exception
  {
    DataParseException(String message)
    {
      super(message);
    }
  }

  public UVPData(File pluginDataFolder)
  {
    this.playerDataFolder = new File(pluginDataFolder, "playerdata");
    makeDirectoryIfNotExists(this.playerDataFolder);
    this.playerDataMap = new HashMap<>(this.playerDataFolder.listFiles().length);
  }

  public void load()
  {
    this.playerDataMap.clear();
    for (File dataFile : this.playerDataFolder.listFiles())
    {
      UVPPlayerData playerData = new UVPPlayerData(dataFile);
      UUID uuid = UUID.fromString(dataFile.getName());

      if (uuid == null)
      {
        UntitledVeganPlugin.getInstance().getLogger().warning("Could not parse uuid for " + dataFile.getName());
      }
      else
      {
        this.playerDataMap.put(uuid, playerData);
      }
    }
  }

  public void savePlayerData()
  {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    for (Map.Entry<UUID, UVPPlayerData> playerData : this.playerDataMap.entrySet())
    {
      File file = new File(playerDataFolder, playerData.getKey().toString());
      try (FileWriter writer = new FileWriter(file))
      {
        writer.write(gson.toJson(playerData.getValue().toJson()));
      }
      catch (IOException e)
      {
        UntitledVeganPlugin.getInstance().getLogger().warning("Could not save player data " +
                                                              file.getPath() + " : " +
                                                              Optional.of(e.getMessage()).map((s) -> s.isEmpty() ? e.getCause().getMessage() : s).get());
      }
    }
  }

  public void save()
  {
    savePlayerData();
  }

  private void makeDirectoryIfNotExists(File dir)
  {
    if (!dir.exists())
    {
      try
      {
        dir.mkdirs();
      }
      catch (SecurityException e)
      {
        UntitledVeganPlugin.getInstance().getLogger().warning("Could not create data directory " +
                                                              dir.getPath() + " : " +
                                                              Optional.of(e.getMessage()).map((s) -> s.isEmpty() ? e.getCause().getMessage() : s).get());
      }
    }
  }
}
