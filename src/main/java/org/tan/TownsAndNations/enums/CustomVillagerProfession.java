package org.tan.TownsAndNations.enums;

import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.tan.TownsAndNations.Lang.Lang;
import org.tan.TownsAndNations.utils.DropChances;

public enum CustomVillagerProfession {

    GOLDSMITH(Lang.VILLAGER_GOLDSMITH.getTranslation(), Profession.TOOLSMITH, DropChances.getRareStone()),
    BOTANIST(Lang.VILLAGER_BOTANIST.getTranslation(), Profession.FARMER, DropChances.getRareWood()),
    COOK(Lang.VILLAGER_COOK.getTranslation(), Profession.BUTCHER, DropChances.getRareCrops());

    private final String name;
    private final Profession profession;
    private final ItemStack buyingItem;

    CustomVillagerProfession(String name, Profession profession, ItemStack _buyingItem){
        this.name = name;
        this.profession = profession;
        this.buyingItem = _buyingItem;
    }
    public static CustomVillagerProfession getVillager(String tag){
        return CustomVillagerProfession.valueOf(tag);
    }
    public String getDisplayName(){
        return this.name;
    }
    public Profession getProfession(){
        return this.profession;
    }

    public ItemStack getBuyingItem(){
        return this.buyingItem;
    }
    public static CustomVillagerProfession fromString(String name) {
        try {
            return CustomVillagerProfession.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
