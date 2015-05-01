package com.biofuels.fof.kosomodel;

import java.util.LinkedList;

import com.biofuels.fof.kosomodel.FieldHistory.HistoryYear;

public class EconomyHistory {
	 class HistoryYear {
		    public int year;
		    public double corn_price;
		    public double grass_price;
		    public double alfalfa_price;
		    public double corn_quantity;
		    public double grass_quantity;
		    public double feed_demand;
		    public double fuel_demand;
		    public double feed_supply;
		    public double fuel_supply;
		  }

		  private LinkedList<HistoryYear> history;
		  public EconomyHistory(){
		    history = new LinkedList<>();
		  }
		  public LinkedList<HistoryYear> getHistory() {
		    return history;
		  }

		  public void addYear(double corn_p, double grass_p, double alfalfa_p, double corn, double grass, double feedDemand, double fuelDemand, double feedSupply, double fuelSupply){
			    HistoryYear newYear = new HistoryYear();
			    newYear.year = history.size();
			    newYear.corn_price = corn_p;
			    newYear.grass_price = grass_p;
			    newYear.alfalfa_price = alfalfa_p;
			    newYear.corn_quantity = corn;
			    newYear.grass_quantity = grass;
			    newYear.feed_demand = feedDemand;
			    newYear.fuel_demand = fuelDemand;
			    newYear.feed_supply = feedSupply;
			    newYear.fuel_supply = fuelSupply;
			    this.history.add(newYear);
		  }
}