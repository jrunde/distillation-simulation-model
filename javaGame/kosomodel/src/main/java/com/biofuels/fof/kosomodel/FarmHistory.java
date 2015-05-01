package com.biofuels.fof.kosomodel;

import java.util.LinkedList;

import com.biofuels.fof.kosomodel.FieldHistory.HistoryYear;

public class FarmHistory {
  public class HistoryYear {
    public int year;
    public int earnings;
    public int costs;
    public double soilSubscore;
    public double waterSubscore;
    public double globalWater;
    public double globalEmissions;
    public double avgWater;
    public double avgEmissions;
    public double bciSubscore;
    public double gbiSubscore;
    public double emissionsSubscore;
    public double emissions;
    public double n2o;
    public double soilRespiration;
    public double netEnergy;
    public double sustainabilityScore;
    public double economicsScore;
    public double environmentScore;
    public double energyScore;
    public double runningEconomicsScore;
    public double runningEnvironmentScore;
    public double runningEnergyScore;
    public double sustainabilityRank;
    public double economicsRank;
    public double environmentRank;
    public double energyRank;
    public double cornIncome;
    public double alfalfaIncome;
    public double switchgrassIncome;
    public double cornYield;
    public double alfalfaYield;
    public double grassYield;
    public double cornEnergy;
    public double grassEnergy;
    public double alfalfaEnergy;
    
    public String toString() {
                return "\nYear: " + this.year + "\n\tCapitol: " + this.earnings + 
                "\n\tEnvironmental Score: " + this.environmentScore + "\n\tEnergy Score: " + this.energyScore + 
                "\n\tSustainability Score: " + this.sustainabilityScore + "\n\tEconomic Score: " + this.economicsScore + 
                "\n\tCorn Energy: " + this.cornEnergy + "\n\tGrass Energy: " + this.grassEnergy + 
                "\n\tAlfalfa Energy: " + this.alfalfaEnergy + "\n\tNet Energy: " + this.netEnergy + 
                "\n\tSOM: " + this.soilSubscore + "\n\tFarm Water: " + this.waterSubscore + 
                "\n\tGlobal Water: " + this.globalWater + "\n\tN2O: " + this.n2o +
                "\n\tGBI: " + this.gbiSubscore + "\n\tBCI: " + this.bciSubscore + 
                "\n\tCorn Yield: " + this.cornYield + "\n\tGrass Yield: " + this.grassYield + 
                "\n\tAlfalfa Yield: " + this.alfalfaYield + "\n\tEmissions: " + this.emissions + "\n";
        }
  }

  private LinkedList<HistoryYear> history;
  public FarmHistory(){
    history = new LinkedList<>();
  }
  public LinkedList<HistoryYear> getHistory() {
    return history;
  }
  public HistoryYear getYear(int year){
	  for(HistoryYear h:history){
		  if(h.year == year)
			  return h;
	  }
	  return null;
  }

  public void addYear(int earnings, int costs, double soil, double water, double globalWater, double globalEmissions, double avgWater, double avgEmissions, double bci, double gbi, double emissionsSubscore, double emissions,  double n2o,  double soilRespiration,  double sustainability, double economics, double environment, double netEnergy, double cornEnergy, double grassEnergy, double alfalfaEnergy, 
      double energyScore, double runningEconomicsScore, double runningEnvironmentScore, double runningEnergyScore, 
      int sustainabilityRank, int economicsRank, int environmentRank,
      int energyRank, double cornIncome, double grassIncome, double alfalfaIncome, double cornYield, double grassYield, double alfalfaYield){

        HistoryYear newYear = new HistoryYear();
        newYear.year = history.size();
        newYear.earnings = earnings;
        newYear.costs = costs;
        newYear.soilSubscore = soil;
        newYear.waterSubscore = water;
        newYear.globalWater = globalWater;
        newYear.globalEmissions = globalEmissions;
        newYear.avgWater = avgWater;
        newYear.avgEmissions = avgEmissions;
        newYear.bciSubscore = bci;
        newYear.gbiSubscore = gbi;
        newYear.emissions = emissions;
        newYear.emissionsSubscore = emissionsSubscore;
        newYear.n2o = n2o;
        newYear.soilRespiration = soilRespiration;
        newYear.sustainabilityScore = sustainability;
        newYear.environmentScore = environment;
        newYear.energyScore = energyScore;
        newYear.runningEconomicsScore = runningEconomicsScore;
        newYear.runningEnvironmentScore = runningEnvironmentScore;
        newYear.runningEnergyScore = runningEnergyScore;
        newYear.economicsScore = economics;
        newYear.cornIncome = cornIncome;
        newYear.switchgrassIncome = grassIncome;
        newYear.alfalfaIncome = alfalfaIncome;
        newYear.cornYield = cornYield;
        newYear.grassYield = grassYield;
        newYear.alfalfaYield = alfalfaYield;
        newYear.sustainabilityRank = sustainabilityRank;
        newYear.economicsRank = economicsRank;
        newYear.environmentRank = environmentRank;
        newYear.energyRank = energyRank;
        newYear.cornEnergy = cornEnergy;
        newYear.alfalfaEnergy = alfalfaEnergy;
        newYear.grassEnergy = grassEnergy;
        newYear.netEnergy = netEnergy;
        this.history.add(newYear);
  }
}
