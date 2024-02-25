package org.tan.TownsAndNations.storage;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.tan.TownsAndNations.DataClass.RegionData;
import org.tan.TownsAndNations.DataClass.TownData;
import org.tan.TownsAndNations.DataClass.newChunkData.ClaimedChunk2;
import org.tan.TownsAndNations.DataClass.newChunkData.RegionClaimedChunk;
import org.tan.TownsAndNations.DataClass.newChunkData.TownClaimedChunk;
import org.tan.TownsAndNations.TownsAndNations;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class NewClaimedChunkStorage {

    private static final Map<String, ClaimedChunk2> claimedChunksMap = new HashMap<>();

    private static String getChunkKey(Chunk chunk) {
        return getChunkKey(chunk.getX(),chunk.getZ(),chunk.getWorld().getUID().toString());
    }
    private static String getChunkKey(ClaimedChunk2 chunk) {
        return getChunkKey(chunk.getX(),chunk.getZ(),chunk.getWorldUUID());
    }
    private static String getChunkKey(int x, int z, String chunkWorldUID){
        return x + "," + z + "," + chunkWorldUID;
    }


    public static Map<String, ClaimedChunk2> getClaimedChunksMap() {
        return claimedChunksMap;
    }

    public static boolean isChunkClaimed(Chunk chunk) {
        return claimedChunksMap.containsKey(getChunkKey(chunk));
    }

    public static String getChunkOwnerID(Chunk chunk) {
        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunk));
        return claimedChunk != null ? claimedChunk.getID() : null;
    }

    public static TownData getChunkOwnerTown(Chunk chunk) {
        if (!NewClaimedChunkStorage.isChunkClaimed(chunk))
            return null;
        return TownDataStorage.get(NewClaimedChunkStorage.getChunkOwnerID(chunk));
    }

    public static String getChunkOwnerName(Chunk chunk) {

        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunk));

        if(claimedChunk instanceof TownClaimedChunk){
            return TownDataStorage.get(claimedChunk.getID()).getName();
        }
        else if(claimedChunk instanceof RegionClaimedChunk){
            return RegionDataStorage.get(claimedChunk.getID()).getName();
        }
        return null;
    }

    public static boolean isOwner(Chunk chunk, String townID) {

        ClaimedChunk2 cChunk = claimedChunksMap.get(getChunkKey(chunk));
        if(cChunk instanceof TownClaimedChunk){
            return cChunk.getID().equals(townID);
        }
        else if(cChunk instanceof RegionClaimedChunk){
            return ((RegionClaimedChunk) cChunk).getRegion().isTownInRegion(TownDataStorage.get(townID));
        }
        return false;
    }

    public static void claimTownChunk(Chunk chunk, String ownerID) {
        claimedChunksMap.put(getChunkKey(chunk), new TownClaimedChunk(chunk, ownerID));
        saveStats();
    }
    public static void claimRegionChunk(Chunk chunk, String ownerID){
        claimedChunksMap.put(getChunkKey(chunk), new RegionClaimedChunk(chunk, ownerID));
        saveStats();
    }

    public static boolean isAdjacentChunkClaimedBySameTown(Chunk chunk, String townID) {

        List<String> adjacentChunkKeys = Arrays.asList(
                getChunkKey(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString())
        );

        for (String adjacentChunkKey : adjacentChunkKeys) {
            ClaimedChunk2 adjacentClaimedChunk = claimedChunksMap.get(adjacentChunkKey);
            if (adjacentClaimedChunk != null && adjacentClaimedChunk.getID().equals(townID)) {
                return true;
            }
        }
        return false;
    }

    public static void unclaimChunk(ClaimedChunk2 claimedChunk) {
        claimedChunksMap.remove(getChunkKey(claimedChunk));
        saveStats();
    }
    public static void unclaimChunk(Chunk chunk) {
        claimedChunksMap.remove(getChunkKey(chunk));
        saveStats();
    }
    public static void unclaimAllChunkFromRegion(RegionData regionData) {
        unclaimAllChunkFromID(regionData.getID());
    }
    public static void unclaimAllChunkFromTown(TownData townData) {
        unclaimAllChunkFromID(townData.getID());
    }

    public static void unclaimAllChunkFromID(String id) {
        Iterator<Map.Entry<String, ClaimedChunk2>> iterator = claimedChunksMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClaimedChunk2> entry = iterator.next();
            ClaimedChunk2 chunk = entry.getValue();
            if (chunk.getID().equals(id)) {
                iterator.remove();
            }
        }
    }



    public static ClaimedChunk2 get(Chunk chunk) {
        return claimedChunksMap.get(getChunkKey(chunk));
    }

    public static boolean isChunkClaimedByTownRegion(TownData townData, Chunk chunkToClaim) {

        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunkToClaim));

        if(claimedChunk instanceof RegionClaimedChunk){
            RegionData regionData = ((RegionClaimedChunk) claimedChunk).getRegion();
            return regionData.isTownInRegion(townData);
        }
        return false;

    }

    public static void loadStats() {
        Gson gson = new Gson();
        File file = new File(TownsAndNations.getPlugin().getDataFolder().getAbsolutePath() + "/TAN - Claimed Chunks.json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
                Map<String, JsonObject> jsonData = gson.fromJson(reader, type);

                for (Map.Entry<String, JsonObject> entry : jsonData.entrySet()) {
                    JsonObject chunkData = entry.getValue();
                    int x = chunkData.get("x").getAsInt();
                    int z = chunkData.get("z").getAsInt();
                    String worldUUID = chunkData.get("worldUUID").getAsString();
                    String ownerID = chunkData.get("ownerID").getAsString();

                    // Déterminer si c'est une ville ou une région en fonction de la première lettre de ownerID
                    if (ownerID.startsWith("T")) {
                        TownClaimedChunk townChunk = new TownClaimedChunk(x, z, worldUUID, ownerID);
                        claimedChunksMap.put(entry.getKey(), townChunk);
                    } else if (ownerID.startsWith("R")) {
                        RegionClaimedChunk regionChunk = new RegionClaimedChunk(x, z, worldUUID, ownerID);
                        claimedChunksMap.put(entry.getKey(), regionChunk);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void saveStats() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(TownsAndNations.getPlugin().getDataFolder().getAbsolutePath() + "/TAN - Claimed Chunks.json");
        file.getParentFile().mkdirs();
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(file, false);){
            gson.toJson(claimedChunksMap, (Appendable)writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
