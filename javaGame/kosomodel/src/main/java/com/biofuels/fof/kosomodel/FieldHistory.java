package com.biofuels.fof.kosomodel;

import java.util.LinkedList;

public class FieldHistory {
  class HistoryYear {
    public int year;
    public double SOM;
    public double BCI;
    public double GBI;
    public double n2o;
    public double respiration;
    public double emissions;
    public double refineryEmissions;
    public double productionEmissions;
    public double costs;
    public Crop crop;
    public double yield;
    boolean fertilizer;
    boolean pesticide;
    boolean till;
    public double productionEnergy;
    public double refineryEnergyIn, refineryEnergyOut, refineryEnergy;
    public double fieldSoilScore, fieldWaterScore, fieldEmissionsScore, fieldEnvScore;
    public double netTotalEnergy;
    public double fieldWater;
    public double fieldRevenue;
  }

  private LinkedList<HistoryYear> history;
  public FieldHistory(){
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

  public void addYear(double SOM, double BCI, double GBI, double n2o, double respiration, double emissions, double refineryEmissions, double productionEmissions, double costs, Crop crop, double yield, boolean fertilizer, boolean pesticide, boolean till, double productionEnergy, double refineryEnergyIn, double refineryEnergyOut, double refineryEnergy, double netTotalEnergy, double fieldWater, double fieldRevenue, double fieldSoilScore, double fieldWaterScore, double fieldEmissionsScore, double fieldEnvironmentalScore){
    HistoryYear newYear = new HistoryYear();
    newYear.year = history.size();
    newYear.crop = crop;
    newYear.yield = yield;
    newYear.SOM = SOM;
    newYear.BCI = BCI;
    newYear.GBI = GBI;
    newYear.n2o = n2o;
    newYear.respiration = respiration;
    newYear.emissions = emissions;
    newYear.refineryEmissions = refineryEmissions;
    newYear.productionEmissions = productionEmissions;
    newYear.costs = costs;
    newYear.fertilizer = fertilizer;
    newYear.pesticide = pesticide;
    newYear.till = till;
    newYear.productionEnergy = productionEnergy;
    newYear.refineryEnergyIn = refineryEnergyIn;
    newYear.refineryEnergyOut = refineryEnergyOut;
    newYear.refineryEnergy = refineryEnergy;
    newYear.netTotalEnergy = netTotalEnergy;
    newYear.fieldWater = fieldWater;
    newYear.fieldRevenue = fieldRevenue;
    newYear.fieldSoilScore = fieldSoilScore;
    newYear.fieldWaterScore = fieldWaterScore;
    newYear.fieldEmissionsScore = fieldEmissionsScore;
    newYear.fieldEnvScore = fieldEnvironmentalScore;
    this.history.add(newYear);
  }
  
  public String toString() {
    String msg = "";
    for (HistoryYear h:history) {
        msg += "\nYear: " + h.year + "\n\tSOM: " + h.SOM + "\n\tBCI: " + h.BCI + "\n\tGBI: " + h.GBI + "\n\tN2O: " + h.n2o + "\n\tRespiration: " + h.respiration + "\n\tEmissions: " + h.emissions + "\n\tCosts: " + h.costs + "\n\tCrop: " + h.crop + "\n\tYield: " + h.yield + "\n\tFertilizer: " + h.fertilizer +  "\n\tPesticide: " + h.pesticide +  "\n\tTill: " + h.till + "\n";
    }
    return msg;
  }
}
