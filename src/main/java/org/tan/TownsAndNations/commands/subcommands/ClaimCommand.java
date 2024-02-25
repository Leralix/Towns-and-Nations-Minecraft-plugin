package org.tan.TownsAndNations.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.tan.TownsAndNations.DataClass.ClaimedChunkSettings;
import org.tan.TownsAndNations.DataClass.PlayerData;
import org.tan.TownsAndNations.DataClass.RegionData;
import org.tan.TownsAndNations.DataClass.TownData;
import org.tan.TownsAndNations.Lang.Lang;
import org.tan.TownsAndNations.commands.SubCommand;
import org.tan.TownsAndNations.enums.TownRolePermission;
import org.tan.TownsAndNations.storage.ClaimedChunkStorage;
import org.tan.TownsAndNations.storage.PlayerDataStorage;
import org.tan.TownsAndNations.storage.TownDataStorage;

import java.util.ArrayList;
import java.util.List;

import static org.tan.TownsAndNations.utils.ChatUtils.getTANString;

public class ClaimCommand extends SubCommand {
    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return Lang.CLAIM_CHUNK_COMMAND_DESC.get();
    }
    public int getArguments(){ return 1;}


    @Override
    public String getSyntax() {
        return "/tan claim <town/region>";
    }
    @Override
    public List<String> getTabCompleteSuggestions(Player player, String[] args){
        List<String> suggestions = new ArrayList<>();
        if (args.length == 2) {
            suggestions.add("town");
            suggestions.add("region");
        }
        return suggestions;
    }
    @Override
    public void perform(Player player, String[] args){

        //Incorrect syntax
        if (args.length != 2){
            player.sendMessage(getTANString() + Lang.CORRECT_SYNTAX_INFO.get(getSyntax()) );
            return;
        }

        if(args[1].equals("town")){
            claimChunkForTown(player);
        }
        else if(args[1].equals("region")){
            claimChunkForRegion(player);
        }
        else
            player.sendMessage(getTANString() + Lang.CORRECT_SYNTAX_INFO.get(getSyntax()) );

    }

    private void claimChunkForRegion(Player player) {

        //No town
        PlayerData playerStat = PlayerDataStorage.get(player.getUniqueId().toString());
        if(!playerStat.haveTown()){
            player.sendMessage(getTANString() + Lang.PLAYER_NO_TOWN.get());
            return;
        }

        //No region
        TownData townData = TownDataStorage.get(player);
        if(!townData.haveRegion()){
            player.sendMessage(getTANString() + Lang.TOWN_NO_REGION.get());
            return;
        }

        RegionData regionData = townData.getRegion();

        //Not leader of the region
        if(!regionData.isPlayerLeader(playerStat)){
            player.sendMessage(getTANString() + Lang.PLAYER_NOT_LEADER_OF_REGION.get());
            return;
        }





    }

    private void claimChunkForTown(Player player) {

        //No town
        PlayerData playerStat = PlayerDataStorage.get(player.getUniqueId().toString());
        if(!playerStat.haveTown()){
            player.sendMessage(getTANString() + Lang.PLAYER_NO_TOWN.get());
            return;
        }

        //No permission
        TownData townData = TownDataStorage.get(player);
        ClaimedChunkSettings townChunkInfo = townData.getChunkSettings();
        if(!playerStat.hasPermission(TownRolePermission.CLAIM_CHUNK)){
            if(!playerStat.isTownLeader()){
                player.sendMessage(getTANString() + Lang.PLAYER_NO_PERMISSION.get());
                return;
            }
        }

        //Chunk limit reached
        if(!townData.canClaimMoreChunk()){
            player.sendMessage(getTANString() + Lang.MAX_CHUNK_LIMIT_REACHED.get());
            return;
        }
        boolean isRegionClaimed = false;
        //Chunk already claimed
        Chunk chunkToClaim = player.getLocation().getChunk();
        if(ClaimedChunkStorage.isChunkClaimed(chunkToClaim)){
            //If chunk belongs to the region in which the town is, then the town can get the chunk
            if(ClaimedChunkStorage.isChunkClaimedByTownRegion(townData,chunkToClaim)){
                isRegionClaimed = true;
            }
            else{
                player.sendMessage(getTANString() + Lang.CHUNK_ALREADY_CLAIMED_WARNING.get(ClaimedChunkStorage.getChunkOwnerName(chunkToClaim)));
                return;
            }
        }

        if(townData.getNumberOfClaimedChunk() == 0){
            if(isRegionClaimed)
                ClaimedChunkStorage.unclaimChunk(chunkToClaim); //Unclaim the chunk so it can be claimed by the town afterward
            ClaimedChunkStorage.claimChunk(chunkToClaim,townData.getID());
            townData.addNumberOfClaimChunk(1);

            player.sendMessage(getTANString() + Lang.CHUNK_CLAIMED_SUCCESS.get(
                    townData.getNumberOfClaimedChunk(),
                    townData.getTownLevel().getChunkCap())
            );
            return;
        }

        if(!ClaimedChunkStorage.isAdjacentChunkClaimedBySameTown(chunkToClaim,townData.getID())){
            player.sendMessage(getTANString() + Lang.CHUNK_NOT_ADJACENT.get());
            return;
        }

        if(isRegionClaimed)
            ClaimedChunkStorage.unclaimChunk(chunkToClaim); //Unclaim the chunk so it can be claimed by the town afterward
        ClaimedChunkStorage.claimChunk(chunkToClaim,townData.getID());
        townData.addNumberOfClaimChunk(1);


        player.sendMessage(getTANString() + Lang.CHUNK_CLAIMED_SUCCESS.get(
                townData.getNumberOfClaimedChunk(),
                townData.getTownLevel().getChunkCap())
        );


    }

}


