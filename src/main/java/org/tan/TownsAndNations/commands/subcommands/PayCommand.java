package org.tan.TownsAndNations.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.tan.TownsAndNations.DataClass.PlayerData;
import org.tan.TownsAndNations.Lang.Lang;
import org.tan.TownsAndNations.commands.SubCommand;
import org.tan.TownsAndNations.storage.PlayerDataStorage;

import java.util.ArrayList;
import java.util.List;

import static org.tan.TownsAndNations.utils.ChatUtils.getTANString;


public class PayCommand extends SubCommand  {
    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public String getDescription() {
        return "give money to another player";
    }

    @Override
    public String getSyntax() {
        return "/tan pay <playerName> <amount>";
    }

    public int getArguments(){
        return 3;
    }

    @Override
    public List<String> getTabCompleteSuggestions(Player player, String[] args){
        List<String> suggestions = new ArrayList<>();
        if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
        }
        if (args.length == 3) {
            suggestions.add("<amount>");
        }
        return suggestions;
    }

    @Override
    public void perform(Player player, String[] args){
        if (args.length < 3){
            player.sendMessage(getTANString() + Lang.NOT_ENOUGH_ARGS_ERROR.getTranslation());
            player.sendMessage(getTANString() + Lang.CORRECT_SYNTAX_INFO.getTranslation(getSyntax()));
        }
        else if(args.length == 3){
            Player receiver = Bukkit.getServer().getPlayer(args[1]);

            if(receiver == null){
                player.sendMessage(getTANString() + Lang.PLAYER_NOT_FOUND);
                return;
            }

            PlayerData receiverDataClass = PlayerDataStorage.get(receiver);
            PlayerData senderDataClass = PlayerDataStorage.get(player);
            int amount;

            try{
                amount = Integer.parseInt(args[2]);

            } catch (NumberFormatException e) {
                player.sendMessage(getTANString() + Lang.PAY_INVALID_SYNTAX.getTranslation());
                return;
            }
            if(amount <1){
                player.sendMessage(getTANString() + Lang.PAY_MINIMUM_REQUIRED.getTranslation());
                return;
            }
            if(senderDataClass.getBalance() < amount){
                player.sendMessage(getTANString() + Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.getTranslation(
                        amount - senderDataClass.getBalance()));
                return;
            }

            senderDataClass.removeFromBalance(amount);
            receiverDataClass.addToBalance(amount);
            player.sendMessage(getTANString() + Lang.PAY_CONFIRMED_SENDER.getTranslation(amount,receiver.getName()));
            receiver.sendMessage(getTANString() + Lang.PAY_CONFIRMED_RECEIVER.getTranslation(amount,player.getName()));
        }
        else {
            player.sendMessage(getTANString() + Lang.TOO_MANY_ARGS_ERROR.getTranslation());
            player.sendMessage(getTANString() + Lang.CORRECT_SYNTAX_INFO.getTranslation(getSyntax()));
        }
    }



}


