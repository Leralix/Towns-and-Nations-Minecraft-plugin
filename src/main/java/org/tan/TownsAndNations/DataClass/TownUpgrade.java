package org.tan.TownsAndNations.DataClass;

import java.util.List;
import java.util.Map;

public class TownUpgrade {
    private String name;
    private String materialCode;
    private int col;
    private int row;
    private int maxLevel;
    private List<Integer> cost;
    private List<String> prerequisites;
    private Map<String, Integer> benefits;



    public TownUpgrade(String name, int col, int row, String materialCode, int maxLevel, List<Integer> cost, List<String> prerequisites, Map<String, Integer> benefits) {
        this.name = name;
        this.col = col;
        this.row = row;
        this.materialCode = materialCode;
        this.maxLevel = maxLevel;
        this.cost = cost;
        this.prerequisites = prerequisites;
        this.benefits = benefits;
    }


    public String getName() {
        return name;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public int getCost(int level) {
        if(cost.size() <= level)
            return cost.get(cost.size()-1);
        return cost.get(level);
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public Map<String, Integer> getBenefits() {
        return benefits;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
