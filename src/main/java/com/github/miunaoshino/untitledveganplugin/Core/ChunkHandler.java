package com.github.miunaoshino.untitledveganplugin.Core;

import com.github.miunaoshino.untitledveganplugin.UntitledVeganPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class ChunkHandler
{
  public static final int VERSION = 0;

  private static class ChunkData implements Serializable
  {
    private static class SerializableLocation implements Serializable
    {
      public double x;
      public double y;
      public double z;
      public float yaw;
      public float pitch;
      public String worldName;

      @Override
      public boolean equals(Object obj)
      {
        if (!(obj instanceof SerializableLocation))
          return (false);
        SerializableLocation other = (SerializableLocation) obj;
        return ((x == other.x) && (y == other.y) && (z == other.z) &&
                (yaw == other.yaw) && (pitch == other.pitch) &&
                worldName.contentEquals(other.worldName));
      }

      @Override
      public int hashCode()
      {
        return (Objects.hash(x, y, z, yaw, pitch, worldName));
      }

      public SerializableLocation()
      {
      }

      public SerializableLocation(ObjectInput in) throws IOException, ClassNotFoundException
      {
        worldName = (String)in.readObject();
        x = in.readDouble();
        y = in.readDouble();
        z = in.readDouble();
        pitch = in.readFloat();
        yaw = in.readFloat();
      }

      public SerializableLocation(Location location)
      {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.worldName = location.getWorld().getName();
      }

      @Override
      public String toString()
      {
        return (worldName + " " + x + " " + y + " " + z + " " + yaw + " " + pitch);
      }

      public void serialize(ObjectOutput out) throws IOException
      {
        out.writeObject(worldName);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeFloat(yaw);
        out.writeFloat(pitch);
      }


      public Location toBukkitLocation()
      {
        World world = Bukkit.getWorld(worldName);

        if (world == null)
          throw new IllegalArgumentException("unknown world");
        return (new Location(world, x, y, z, yaw, pitch));
      }
    }

    private static class BlockData implements Serializable
    {
      public BlockData()
      {

      }

      public BlockData(ObjectInput in) throws IOException, ClassNotFoundException
      {
        _crueltyFree = in.readBoolean();
        _additionalData = in.readInt();
        _name = (String)in.readObject();
        _lore = (List<String>)in.readObject();
      }

      public void serialize(ObjectOutput out) throws IOException
      {
        out.writeBoolean(_crueltyFree);
        out.writeInt(_additionalData);
        out.writeObject(_name);
        out.writeObject(_lore);
      }

      public boolean isCrueltyFree()
      {
        return (_crueltyFree);
      }

      public boolean _crueltyFree; // whether the item form was made cruelty free or not
      public int _additionalData; // additional material-specific data
      public String       _name;
      public List<String> _lore;
    }

    public final Map<SerializableLocation, BlockData> _blockMap = new HashMap<>();
  }

  public static void registerVeganBlockPlaced(BlockPlaceEvent e)
  {
    ItemStack itemPlaced = e.getItemInHand();
    ItemMeta itemMeta = itemPlaced.getItemMeta();
    Block block = e.getBlockPlaced();
    Chunk chunk = block.getChunk();
    ChunkData chunkData;

    chunkData = _dataMap.get(chunk);
    if (chunkData == null)
    {
      chunkData = new ChunkData();
      _dataMap.put(chunk, chunkData);
    }
    ChunkData.SerializableLocation sLocation = new ChunkData.SerializableLocation(block.getLocation());
    ChunkData.BlockData blockData = new ChunkData.BlockData();
    blockData._crueltyFree = true;
    blockData._additionalData = 0;
    blockData._name = itemMeta.getDisplayName();
    blockData._lore = itemMeta.getLore();
    chunkData._blockMap.put(sLocation, blockData);
  }

  public static void handleBlockDropItemEvent(BlockDropItemEvent e)
  {
    if (e.isCancelled()) // is this necessary?
      return;

    Chunk chunk = e.getBlock().getChunk();
    ChunkData data = _dataMap.getOrDefault(chunk, null);

    if (data == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("No data found for chunk " + chunk.toString());
      return;
    }
    Block block = e.getBlock();
    ChunkData.SerializableLocation sLocation = new ChunkData.SerializableLocation(block.getLocation());
    ChunkData.BlockData blockData = data._blockMap.remove(sLocation);

    if (blockData == null)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Block " + e.getBlock().toString() + " at " + sLocation + " has no data");
      return;
    }
    if (!blockData.isCrueltyFree())
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Block " + e.getBlock().toString() + " is not cruelty free");
      return;
    }
    for (Item item : e.getItems())
    {
      ItemStack itemStack = item.getItemStack();
      ItemMeta itemMeta = itemStack.getItemMeta();

      itemMeta.setDisplayName(blockData._name);
      itemMeta.setLore(blockData._lore);
      UntitledVeganPlugin.setPlantBased(itemMeta.getPersistentDataContainer());
      UntitledVeganPlugin.getInstance().getLogger().info(
              "Changed block drop " + itemStack + " to name " + itemMeta.getDisplayName() + " with lore " + itemMeta.getLore());
      itemStack.setItemMeta(itemMeta);
    }
  }

  public static void deserializeChunkData(Chunk chunk)
  {
    PersistentDataContainer pdc = chunk.getPersistentDataContainer();
    //pdc.remove(new NamespacedKey(UntitledVeganPlugin.getInstance(), "chunk_info"));

    byte[] data = pdc.getOrDefault(
            new NamespacedKey(UntitledVeganPlugin.getInstance(), "chunk_info"),
            PersistentDataType.BYTE_ARRAY,
            null);
    if (data == null || data.length == 0)
      return;


    try (ByteArrayInputStream stream = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(stream))
    {
      int version = in.readInt();
      if (version != VERSION)
        throw new IllegalArgumentException("Invalid version " + version);
      ChunkData chunkData = new ChunkData();

      while (stream.available() > 0)
      {
        ChunkData.SerializableLocation location = new ChunkData.SerializableLocation(in);
        ChunkData.BlockData blockData = new ChunkData.BlockData(in);

        chunkData._blockMap.put(location, blockData);
        UntitledVeganPlugin.getInstance().getLogger().info("Adding block " + location + " to chunk " + chunk);
      }
      _dataMap.put(chunk, chunkData);
    }
    catch (Exception e)
    {
      UntitledVeganPlugin.getInstance().getLogger().warning("Exception while loading chunk data for " + chunk);
      e.printStackTrace();
      _dataMap.remove(chunk);
      chunk.getPersistentDataContainer().remove(new NamespacedKey(UntitledVeganPlugin.getInstance(), "chunk_info"));
    }
  }

  public static void unloadChunkData(Chunk chunk)
  {
    _dataMap.remove(chunk);
  }

  public static void serializeChunkData(Chunk chunk)
  {
    ChunkData chunkData = _dataMap.getOrDefault(chunk, null);

    if (chunkData == null)
      return;
    PersistentDataContainer pdc = chunk.getPersistentDataContainer();
    if (chunkData._blockMap.isEmpty())
    {
      pdc.remove(new NamespacedKey(UntitledVeganPlugin.getInstance(), "chunk_info"));
      return;
    }

    try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(stream);)
    {
      out.writeInt(VERSION);
      for (var entry : chunkData._blockMap.entrySet())
      {
        entry.getKey().serialize(out);
        entry.getValue().serialize(out);
      }
      byte[] bytes = stream.toByteArray();
      pdc.set(new NamespacedKey(UntitledVeganPlugin.getInstance(), "chunk_info"),
              PersistentDataType.BYTE_ARRAY,
              bytes);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private final static Map<Chunk, ChunkData> _dataMap = new HashMap<>();
}
