package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import javax.xml.stream.events.Namespace;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UVPPlayerData
{
  private static Set<EntityType> passiveTypes = getPassiveTypes();

  private static final Set<EntityType> getPassiveTypes()
  {
    Set<EntityType> passiveTypes = new HashSet<>();
    for (EntityType type : EntityType.values())
    {
      if (UntitledVeganPlugin.isAFriend(type.getEntityClass()))
      {
	passiveTypes.add(type);
	UntitledVeganPlugin.getInstance().getLogger().info("Registered " + type.getKey() + " as a friend");
      }
    }
    return (passiveTypes);
  }

  private Map<NamespacedKey, Integer> killMap = new HashMap<>(passiveTypes.size());

  public UVPPlayerData()
  {
  }

  public UVPPlayerData(File dataFile)
  {
    try (FileReader reader = new FileReader(dataFile))
    {
      JsonElement root = JsonParser.parseReader(reader);

      if (!root.isJsonObject())
	throw new UVPData.DataParseException("Root is not a JSON Object");

      JsonObject killCounts = root.getAsJsonObject().getAsJsonObject("kill_count");

      if (killCounts.isJsonObject())
      {
	for (Map.Entry<String, JsonElement> child : killCounts.entrySet())
	{

	  this.killMap.put(NamespacedKey.fromString(child.getKey()), child.getValue().getAsInt());
	}
      }
    }
    catch (UVPData.DataParseException e)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Could not parse data file " +
							    dataFile.getPath() + " : " +
							    Optional.of(e.getMessage()).map((s) -> s.isEmpty() ? e.getCause().getMessage() : s).get());
    }
    catch (IOException e)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Could not parse data file " +
							    dataFile.getPath() + " : " +
							    Optional.of(e.getMessage()).map((s) -> s.isEmpty() ? e.getCause().getMessage() : s).get());
    }
  }

  public void registerKill(LivingEntity entity)
  {
    Integer count = this.killMap.get(entity.getType().getKey());

    if (count == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().info("count for " + entity.getType().getKey() + " was not found, adding");
      count = Integer.valueOf(0);
    }
    this.killMap.put(entity.getType().getKey(), count + 1);
  }

  public int getKillCount(LivingEntity entity)
  {
    return (this.killMap.getOrDefault(entity.getType().getKey(), 0));
  }

  public JsonObject toJson()
  {
    JsonObject obj = new JsonObject();

    JsonObject killCount = new JsonObject();
    for (Map.Entry<NamespacedKey, Integer> entry : this.killMap.entrySet())
    {
      killCount.addProperty(entry.getKey().toString(), entry.getValue().intValue());
    }

    obj.add("kill_count", killCount);
    return (obj);
  }
}
