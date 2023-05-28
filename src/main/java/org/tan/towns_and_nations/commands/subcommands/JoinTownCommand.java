package org.tan.towns_and_nations.commands.subcommands;

import org.bukkit.entity.Player;
import org.tan.towns_and_nations.commands.SubCommand;
import org.tan.towns_and_nations.utils.PlayerStatStorage;
import org.tan.towns_and_nations.utils.TownDataStorage;
import org.tan.towns_and_nations.utils.TownInviteDataStorage;

import java.util.ArrayList;

public class JoinTownCommand extends SubCommand {
    @Override
    public String getName() {
        return "join";
    }


    @Override
    public String getDescription() {
        return "join a town that invited you";
    }

    public int getArguments() {
        return 99;
    }


    @Override
    public String getSyntax() {
        return "/tan join <Town ID>";
    }

    @Override
    public void perform(Player player, String[] args) {


        if (args.length == 1) {
            player.sendMessage("Not enough arguments");
            player.sendMessage("Correct Syntax: /tan join <Town ID>");
        } else {

            String townID = args[1];

            ArrayList<String> townInvited = TownInviteDataStorage.checkInvitation(player.getUniqueId().toString());

            for (String town : townInvited){

                System.out.println(townID);
                System.out.println(town);

                if(town.equals(townID)){
                    TownDataStorage.getTown(townID).addPlayer(player.getUniqueId().toString());
                    PlayerStatStorage.findStatUUID(player.getUniqueId().toString()).setTownId(townID);
                    player.sendMessage("Vous avez rejoins la ville");



                }

            }
            System.out.println("testJointown");
            // Code tentative pour faire /join avec le nom de la ville
            /*

            args = Arrays.copyOfRange(args, 1, args.length);


            String townName = "";
            for (String arg : args) {
                player.sendMessage(arg);
                townName = townName + arg + " ";

            }
            player.sendMessage(townName);

            if(townName.equals(TownInviteDataStorage.getInvitation(player.getUniqueId().toString()))){

            }
            */
        }


    }
}



