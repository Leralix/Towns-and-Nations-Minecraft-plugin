package org.leralix.tan.dataclass.territory;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.*;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.newhistory.ChunkPaymentHistory;
import org.leralix.tan.dataclass.newhistory.PlayerDonationHistory;
import org.leralix.tan.dataclass.newhistory.SalaryPaymentHistory;
import org.leralix.tan.dataclass.territory.economy.Budget;
import org.leralix.tan.dataclass.territory.economy.ChunkUpkeepLine;
import org.leralix.tan.dataclass.territory.economy.SalaryPaymentLine;
import org.leralix.tan.dataclass.wars.CurrentAttacks;
import org.leralix.tan.dataclass.wars.PlannedAttack;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.gui.PlayerGUI;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.enums.SoundEnum;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.newsletter.news.DiplomacyProposalNL;
import org.leralix.tan.newsletter.NewsletterStorage;
import org.leralix.tan.newsletter.news.JoinRegionProposalNL;
import org.leralix.tan.storage.CurrentAttacksStorage;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlannedAttackStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.*;
import org.leralix.tan.utils.config.ConfigTag;
import org.leralix.tan.utils.config.ConfigUtil;

import java.util.*;
import java.util.function.Consumer;

import static org.leralix.tan.enums.SoundEnum.*;
import static org.leralix.tan.utils.ChatUtils.getTANString;

public abstract class TerritoryData {

//    private String ID;
//    private String name;
//    private String description;
//    private String leaderID;
//    private String overlordID;
//    private Long dateTimeCreated;
//    private String iconMaterial;
//    private int balance;
//    private TownRelations relations;
    Integer color;
    Integer defaultRankID;
    private Map<Integer, RankData> ranks;
    private Collection<String> attackIncomingList;
    private Collection<String> currentAttackList;
    private HashMap<String, Integer> availableClaims;
    private Map<String, DiplomacyProposal> diplomacyProposals;
    List<String> overlordsProposals;

    protected TerritoryData(){
        ranks = new HashMap<>();
        registerNewRank("default");

        attackIncomingList = new ArrayList<>();
        currentAttackList = new ArrayList<>();
        availableClaims = new HashMap<>();
        diplomacyProposals = new HashMap<>();
        overlordsProposals = new ArrayList<>();
    }

    public abstract String getID();
    public abstract String getName();
    public abstract int getHierarchyRank();
    public abstract String getColoredName();
    public abstract void rename(Player player, int cost, String name);
    public abstract String getLeaderID();
    public abstract PlayerData getLeaderData();
    public abstract void setLeaderID(String leaderID);
    public boolean isLeader(PlayerData playerData){
        return isLeader(playerData.getID());
    }
    public abstract boolean isLeader(String playerID);
    public abstract String getDescription();
    public abstract void setDescription(String newDescription);
    public abstract ItemStack getIconItem();
    public abstract void setIcon(ItemStack icon);
    public abstract Collection<String> getPlayerIDList();
    public boolean isPlayerIn(PlayerData playerData){
        return isPlayerIn(playerData.getID());
    }
    public boolean isPlayerIn(String playerID){
        return getPlayerIDList().contains(playerID);
    }

    public Collection<String> getOrderedPlayerIDList(){
        List<String> sortedList = new ArrayList<>();
        List<PlayerData> playerDataSorted = getPlayerDataList().stream()
                .sorted(Comparator.comparingInt(playerData -> -this.getRank(playerData.getRankID(this)).getLevel()))
                .toList();

        for(PlayerData playerData : playerDataSorted){
            sortedList.add(playerData.getID());
        }
        return sortedList;
    }
    public abstract Collection<PlayerData> getPlayerDataList();
    public abstract ClaimedChunkSettings getChunkSettings();
    public abstract TownRelations getRelations();
    public void setRelation(TerritoryData otherTerritory, TownRelation relation){
        TownRelation actualRelation = getRelationWith(otherTerritory);
        if(relation.isSuperiorTo(actualRelation)){
            broadCastMessageWithSound(Lang.BROADCAST_RELATION_IMPROVE.get(getColoredName(), otherTerritory.getColoredName(),relation.getColoredName()), GOOD);
            otherTerritory.broadCastMessageWithSound(Lang.BROADCAST_RELATION_IMPROVE.get(otherTerritory.getColoredName(), getColoredName(),relation.getColoredName()), GOOD);
        }
        else{
            broadCastMessageWithSound(Lang.BROADCAST_RELATION_WORSEN.get(getColoredName(), otherTerritory.getColoredName(),relation.getColoredName()), BAD);
            otherTerritory.broadCastMessageWithSound(Lang.BROADCAST_RELATION_WORSEN.get(otherTerritory.getColoredName(), getColoredName(),relation.getColoredName()), BAD);
        }

        this.getRelations().setRelation(relation,otherTerritory);
        otherTerritory.getRelations().setRelation(relation,this);

        TeamUtils.updateAllScoreboardColor();
    }


    private Map<String, DiplomacyProposal> getDiplomacyProposals(){
        if(diplomacyProposals == null)
            diplomacyProposals = new HashMap<>();
        return diplomacyProposals;
    }

    public void removeDiplomaticProposal(TerritoryData proposingTerritory){
        removeDiplomaticProposal(proposingTerritory.getID());
    }
    public void removeDiplomaticProposal(String proposingTerritoryID){
        getDiplomacyProposals().remove(proposingTerritoryID);
    }
    private void addDiplomaticProposal(TerritoryData proposingTerritory, TownRelation wantedRelation){
        getDiplomacyProposals().put(proposingTerritory.getID(), new DiplomacyProposal(proposingTerritory.getID(), getID(), wantedRelation));
        NewsletterStorage.registerNewsletter(new DiplomacyProposalNL(proposingTerritory.getID(), getID(), wantedRelation));
    }

    public void receiveDiplomaticProposal(TerritoryData proposingTerritory, TownRelation wantedRelation) {
        removeDiplomaticProposal(proposingTerritory);
        addDiplomaticProposal(proposingTerritory, wantedRelation);
    }

    public Collection<DiplomacyProposal> getAllDiplomacyProposal(){
        return getDiplomacyProposals().values();
    }

    public TownRelation getRelationWith(TerritoryData territoryData){
        return getRelationWith(territoryData.getID());
    }
    public TownRelation getRelationWith(String territoryID){
        TownRelation relation = getRelations().getRelationWith(territoryID);

        if(relation != TownRelation.NEUTRAL)
            return relation;

        if(getID().equals(territoryID))
            return TownRelation.SELF;

        if(haveOverlord() && getOverlord().getID().equals(territoryID))
            return TownRelation.OVERLORD;

        if(getVassalsID().contains(territoryID))
            return TownRelation.VASSAL;
        
        return TownRelation.NEUTRAL;
    }

    public abstract void addToBalance(double balance);

    public abstract void removeFromBalance(double balance);

    public abstract void broadCastMessage(String message);

    public abstract void broadCastMessageWithSound(String message, SoundEnum soundEnum, boolean addPrefix);

    public abstract void broadCastMessageWithSound(String message, SoundEnum soundEnum);
    public abstract boolean haveNoLeader();

    public abstract ItemStack getIcon();
    public abstract ItemStack getIconWithInformations();
    public ItemStack getIconWithInformationAndRelation(TerritoryData territoryData){
        ItemStack icon = getIconWithInformations();

        ItemMeta meta = icon.getItemMeta();
        if(meta != null){
            List<String> lore = meta.getLore();

            if(territoryData != null && lore != null){
                TownRelation relation = getRelationWith(territoryData);
                String relationName;
                if(relation == null){
                    relationName = Lang.GUI_TOWN_RELATION_NEUTRAL.get();
                }
                else {
                    relationName = relation.getColor() + relation.getName();
                }
                lore.add(Lang.GUI_TOWN_INFO_TOWN_RELATION.get(relationName));
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    public Collection<String> getAttacksInvolvedID(){
        if(attackIncomingList == null)
            this.attackIncomingList = new ArrayList<>();
        return attackIncomingList;
    }
    public Collection<PlannedAttack> getAttacksInvolved(){
        Collection<PlannedAttack> res = new ArrayList<>();
        for(String attackID : getAttacksInvolvedID()){
            PlannedAttack plannedAttack = PlannedAttackStorage.get(attackID);
            res.add(plannedAttack);
        }
        return res;
    }
    public void addPlannedAttack(PlannedAttack war){
        getAttacksInvolvedID().add(war.getID());

    }
    public void removePlannedAttack(PlannedAttack war){
        getAttacksInvolvedID().remove(war.getID());

    }


    public Collection<String> getCurrentAttacksID(){
        if(currentAttackList == null)
            this.currentAttackList = new ArrayList<>();
        return currentAttackList;
    }
    public Collection<CurrentAttacks> getCurrentAttacks(){
        Collection<CurrentAttacks> res = new ArrayList<>();
        for(String attackID : getCurrentAttacksID()){
            CurrentAttacks attackInvolved = CurrentAttacksStorage.get(attackID);
            res.add(attackInvolved);
        }
        return res;
    }

    public void addCurrentAttack(CurrentAttacks currentAttacks){
        getAttacksInvolvedID().add(currentAttacks.getId());

    }
    public void removeCurrentAttack(CurrentAttacks currentAttacks){
        getAttacksInvolvedID().remove(currentAttacks.getId());
    }

    public abstract boolean atWarWith(String territoryID);


    public abstract double getBalance();

    public abstract TerritoryData getOverlord();
    public abstract void removeOverlord();
    public void setOverlord(TerritoryData overlord){
        getOverlordsProposals().remove(overlord.getID());
        broadCastMessageWithSound(getTANString() + Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(this.getColoredName(), overlord.getColoredName()), GOOD);
        setOverlordPrivate(overlord);
    }
    protected abstract void setOverlordPrivate(TerritoryData newOverlord);

    public void addVassal(TerritoryData vassal){
        NewsletterStorage.removeVassalisationProposal(this, vassal);
        broadCastMessageWithSound(getTANString() + Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(vassal.getColoredName(), getColoredName()), GOOD);
        addVassalPrivate(vassal);
    }
    protected abstract void addVassalPrivate (TerritoryData vassal);
    public void removeVassal(TerritoryData territoryToRemove){
        removeVassal(territoryToRemove.getID());
    }
    public abstract void removeVassal(String townID);

    public abstract boolean isCapital();

    public TerritoryData getCapital(){
        return TerritoryUtil.getTerritory(getCapitalID());
    }
    public abstract String getCapitalID();


    public abstract int getChildColorCode();

    public int getChunkColorCode(){
        if(color == null)
            return getChildColorCode();
        return color;
    }

    public String getChunkColorInHex() {
        return String.format("#%06X", getChunkColorCode());
    }

    public ChatColor getChunkColor() {
        return ChatColor.of(getChunkColorInHex());
    }

    public void setChunkColor(int color) {
        this.color = color;
    }

    public abstract boolean haveOverlord();


    public Map<String, Integer> getAvailableEnemyClaims() {
        if(availableClaims == null)
            availableClaims = new HashMap<>();
        return availableClaims;
    }

    public void addAvailableClaims(String territoryID, int amount){
        getAvailableEnemyClaims().merge(territoryID, amount, Integer::sum);
    }
    public void consumeEnemyClaim(String territoryID){
        getAvailableEnemyClaims().merge(territoryID, -1, Integer::sum);
        if(getAvailableEnemyClaims().get(territoryID) <= 0)
            getAvailableEnemyClaims().remove(territoryID);
    }

    public void claimChunk(Player player){
        Chunk chunk = player.getLocation().getChunk();
        claimChunk(player, chunk);
    }

    public abstract void claimChunk(Player player,Chunk chunk);


    public void castActionToAllPlayers(Consumer<Player> action){
        for(PlayerData playerData : getPlayerDataList()){
            Player player = playerData.getPlayer();
            if(player != null)
                action.accept(player);
        }
    }

    public void delete(){
        NewClaimedChunkStorage.unclaimAllChunksFromTerritory(this); //Unclaim all chunk from town

        castActionToAllPlayers(HumanEntity::closeInventory);

        if(haveOverlord())
            getOverlord().removeVassal(this);

        for(TerritoryData territory : getVassals()){
            territory.removeOverlord();
        }

        getRelations().cleanAll(this);   //Cancel all Relation between the deleted territory and other territories
        PlannedAttackStorage.territoryDeleted(this);
    }

    public boolean canConquerChunk(ClaimedChunk2 chunk) {
        if(getAvailableEnemyClaims().containsKey(chunk.getOwnerID())){
            consumeEnemyClaim(chunk.getOwnerID());
            return true;
        }
        return false;
    }

    public void addDonation(Player player, double amount) {
        double playerBalance = EconomyUtil.getBalance(player);

        if(playerBalance < amount ){
            player.sendMessage(ChatUtils.getTANString() + Lang.PLAYER_NOT_ENOUGH_MONEY.get());
            return;
        }
        if(amount <= 0 ){
            player.sendMessage(ChatUtils.getTANString() + Lang.PAY_MINIMUM_REQUIRED.get());
            return;
        }


        EconomyUtil.removeFromBalance(player,amount);
        addToBalance(amount);

        TownsAndNations.getPlugin().getDatabaseHandler().addTransactionHistory(new PlayerDonationHistory(this, player, amount));
        player.sendMessage(ChatUtils.getTANString() + Lang.PLAYER_SEND_MONEY_SUCCESS.get(amount, getColoredName()));
        SoundUtil.playSound(player, MINOR_LEVEL_UP);
    }

    public abstract void openMainMenu(Player player);

    public abstract boolean canHaveVassals();
    public abstract boolean canHaveOverlord();

    public abstract List<String> getVassalsID();
    public List<TerritoryData> getVassals(){
        List<TerritoryData> res = new ArrayList<>();
        for(String vassalID : getVassalsID()){
            TerritoryData vassal = TerritoryUtil.getTerritory(vassalID);
            if(vassal != null)
                res.add(vassal);
        }
        return res;
    }
    public int getVassalCount(){
        return getVassalsID().size();
    }

    public boolean isVassal(TerritoryData territoryData) {
        return isVassal(territoryData.getID());
    }
    public abstract boolean isVassal(String territoryID);

    public boolean isCapitalOf(TerritoryData territoryData) {
        return isCapitalOf(territoryData.getID());
    }
    public abstract boolean isCapitalOf(String territoryID);
    public abstract boolean isLeaderOnline();

    public abstract Collection<TerritoryData> getPotentialVassals();

    private List<String> getOverlordsProposals(){
        if(overlordsProposals == null)
            overlordsProposals = new ArrayList<>();
        return overlordsProposals;
    }

    public void addVassalisationProposal(TerritoryData proposal){
        getOverlordsProposals().add(proposal.getID());
        broadCastMessageWithSound(Lang.REGION_DIPLOMATIC_INVITATION_RECEIVED_1.get(proposal.getColoredName(), getColoredName()), MINOR_GOOD);
        NewsletterStorage.registerNewsletter(new JoinRegionProposalNL(proposal, this));
    }

    public void removeVassalisationProposal(TerritoryData proposal){
        getOverlordsProposals().remove(proposal.getID());
    }

    public boolean containsVassalisationProposal(TerritoryData proposal){
        return getOverlordsProposals().contains(proposal.getID());
    }

    public int getNumberOfVassalisationProposals(){
        return getOverlordsProposals().size();
    }

    public List<GuiItem> getAllSubjugationProposals(Player player, int page){
        ArrayList<GuiItem> proposals = new ArrayList<>();
        for(String proposalID : getOverlordsProposals()) {
            TerritoryData proposalOverlord = TerritoryUtil.getTerritory(proposalID);
            if (proposalOverlord == null)
                continue;
            ItemStack territoryItem = proposalOverlord.getIconWithInformations();
            HeadUtils.addLore(territoryItem, Lang.LEFT_CLICK_TO_ACCEPT.get(), Lang.RIGHT_CLICK_TO_REFUSE.get());
            GuiItem acceptInvitation = ItemBuilder.from(territoryItem).asGuiItem(event -> {
                event.setCancelled(true);
                if(event.isLeftClick()){
                    setOverlord(proposalOverlord);
                    proposalOverlord.addVassal(this);
                    broadCastMessageWithSound(getTANString() + Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(this.getColoredName(), proposalOverlord.getName()), GOOD);
                    PlayerGUI.openHierarchyMenu(player, this);
                }
                if(event.isRightClick()){
                    getOverlordsProposals().remove(proposalID);
                    PlayerGUI.openChooseOverlordMenu(player, this, page);
                }


            });
            proposals.add(acceptInvitation);
        }
        return proposals;
    }

    protected Map<Integer, RankData> getRanks(){
        if(ranks == null) {
            ranks = new HashMap<>();
            registerNewRank("default");
        }
        return ranks;
    }
    public Collection<RankData> getAllRanks(){
        return getRanks().values();
    }

    public Collection<RankData> getAllRanksSorted(){
        return getRanks().values().stream()
                .sorted(Comparator.comparingInt(p -> -p.getLevel()))
                .toList();
    }

    public RankData getRank(int rankID){
        return getRanks().get(rankID);
    }
    public abstract RankData getRank(PlayerData playerData);
    public RankData getRank(Player player){
        return getRank(PlayerDataStorage.get(player));
    }
    public int getNumberOfRank(){
        return getRanks().size();
    }

    public boolean isRankNameUsed(String message) {
        if(ConfigUtil.getCustomConfig(ConfigTag.MAIN).getBoolean("AllowNameDuplication",false))
            return false;

        for (RankData rank : getAllRanks()) {
            if (rank.getName().equals(message)) {
                return true;
            }
        }
        return false;
    }

    public RankData registerNewRank(String rankName){
        int nextRankId = 0;
        for(RankData rank : getAllRanks()){
            if(rank.getID() >= nextRankId)
                nextRankId = rank.getID() + 1;
        }

        RankData newRank = new RankData(nextRankId, rankName);
        getRanks().put(nextRankId,newRank);
        return newRank;
    }

    public void removeRank(int key){
        getRanks().remove(key);
    }

    public int getDefaultRankID() {
        if(defaultRankID == null){
            defaultRankID = getAllRanks().iterator().next().getID(); //If no default rank is set, we take the first one
        }
        return defaultRankID;
    }

    public void setDefaultRank(int rankID) {
        this.defaultRankID = rankID;
    }

    public abstract List<GuiItem> getOrderedMemberList(PlayerData playerData);


    public boolean doesPlayerHavePermission(Player player, RolePermission townRolePermission) {
        return doesPlayerHavePermission(PlayerDataStorage.get(player), townRolePermission);
    }
    public boolean doesPlayerHavePermission(PlayerData playerData, RolePermission townRolePermission) {
        //Player is not in the territory, he has no permission
        if(!this.isPlayerIn(playerData)){
            return false; //Later implement
        }
        //Leader have all rights
        if(isLeader(playerData))
            return true;

        return getRank(playerData).hasPermission(townRolePermission);
    }

    public void setPlayerRank(PlayerData playerStat, RankData rankData) {
        getRank(playerStat).removePlayer(playerStat);
        rankData.addPlayer(playerStat);
        specificSetPlayerRank(playerStat, rankData.getID());
    }

    protected abstract void specificSetPlayerRank(PlayerData playerStat, int rankID);

    public Budget getBudget() {
        Budget budget = new Budget();
        addCommonTaxes(budget);
        addSpecificTaxes(budget);
        return budget;
    }

    private void addCommonTaxes(Budget budget) {
        budget.addProfitLine(new SalaryPaymentLine(this));
        budget.addProfitLine(new ChunkUpkeepLine(this));
    }

    protected abstract void addSpecificTaxes(Budget budget);

    public int getNumberOfClaimedChunk(){
        return NewClaimedChunkStorage.getAllChunkFrom(this).size();
    }

    public abstract double getChunkUpkeepCost();

    public abstract double getTax();

    public abstract void addToTax(double i);
    public abstract void removeToTax(double i);

    public void executeTasks(){
        collectTaxes();
        paySalaries();
        payChunkUpkeep();
    }

    private void paySalaries() {
        for (RankData rank : getAllRanks()){
            int rankSalary = rank.getSalary();
            List<String> playerIdList = rank.getPlayersID();
            double costOfSalary = (double) playerIdList.size() * rankSalary;

            if(rankSalary == 0 || costOfSalary > getBalance() ){
                continue;
            }
            removeFromBalance(costOfSalary);
            for(String playerId : playerIdList){
                PlayerData playerData = PlayerDataStorage.get(playerId);
                playerData.addToBalance(rankSalary);
                TownsAndNations.getPlugin().getDatabaseHandler().addTransactionHistory(new SalaryPaymentHistory(this, String.valueOf(rank.getID()), costOfSalary));
            }
        }
    }

    private void payChunkUpkeep() {
        double upkeepCost = this.getChunkUpkeepCost();

        int numberClaimedChunk = getNumberOfClaimedChunk();
        double totalUpkeep = numberClaimedChunk * upkeepCost;
        if (totalUpkeep > getBalance()){
            deletePortionOfChunk();
            TownsAndNations.getPlugin().getDatabaseHandler().addTransactionHistory(new ChunkPaymentHistory(this,-1));
        }
        else{
            removeFromBalance(totalUpkeep);
            TownsAndNations.getPlugin().getDatabaseHandler().addTransactionHistory(new ChunkPaymentHistory(this,totalUpkeep));
        }

    }

    private void deletePortionOfChunk() {
        int minNbOfUnclaimedChunk = ConfigUtil.getCustomConfig(ConfigTag.MAIN).getInt("minimumNumberOfChunksUnclaimed",5);
        int nbOfUnclaimedChunk = 0;
        double minPercentageOfChunkToKeep = ConfigUtil.getCustomConfig(ConfigTag.MAIN).getDouble("percentageOfChunksUnclaimed",10) / 100;


        Collection<ClaimedChunk2> allChunkFrom = NewClaimedChunkStorage.getAllChunkFrom(this);
        for(ClaimedChunk2 claimedChunk2 : allChunkFrom){
            if(Math.random() < minPercentageOfChunkToKeep){
                NewClaimedChunkStorage.unclaimChunk(claimedChunk2);
                nbOfUnclaimedChunk++;
            }
        }
        if(nbOfUnclaimedChunk < minNbOfUnclaimedChunk){
            for(ClaimedChunk2 claimedChunk2 : allChunkFrom){
                NewClaimedChunkStorage.unclaimChunk(claimedChunk2);
                nbOfUnclaimedChunk++;
                if(nbOfUnclaimedChunk >= minNbOfUnclaimedChunk)
                    break;
            }
        }


    }


    protected abstract void collectTaxes();
}