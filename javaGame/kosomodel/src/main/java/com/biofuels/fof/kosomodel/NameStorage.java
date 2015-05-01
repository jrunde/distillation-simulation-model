package com.biofuels.fof.kosomodel;

import java.util.ArrayList;
import java.util.Random;

public class NameStorage {
	private ArrayList<String> names;
	private String[] nameList = new String[]{
			"Gas Rancher", "Fuel Fielder", "Fuelsteader", "Farmer Will", "Farmer Steve", "Farmer Leith", 
			"Farmer Rosemary", "Farmer Jeff", "Farmer Ben", "Farmer Ferris", "Farmer James", "Farmer Nate", "Power Seeder"
	};
	
	private Random rand;
	
	public NameStorage(){
		names = new ArrayList<>();
		for(int i=0;i<nameList.length;i++)
			names.add(nameList[i]);
		
		rand = new Random();
	}
	
	public String take(){
		return names.remove(rand.nextInt(names.size()));
	}
}
