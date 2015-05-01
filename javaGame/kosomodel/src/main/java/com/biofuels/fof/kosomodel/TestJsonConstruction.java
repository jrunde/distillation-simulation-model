package com.biofuels.fof.kosomodel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class TestJsonConstruction {
	
	// NEW APPROACH
	public void testJsonConstructor() {
		
	};
		
	@SuppressWarnings("unchecked")
	public String getJsonString() {
		//    List<Field> fields = games.get(roomID).getFarm(clientID).getFields();
			JSONObject reply = new JSONObject();

			
			for(int y=0; y<2; y=y+1){
				JSONArray fields = new JSONArray();
				JSONObject farmData = new JSONObject();

				for(int f=0; f<4; f=f+1){
				  JSONObject field = new JSONObject();
				  field.put("fieldNumber", f);	
				  field.put("SOM", 0.87);
				  field.put("BCI", 0.5);
				  field.put("GBI", 0.66);
				  field.put("emissions",  1300);
				  field.put("cost", 300.0);
				  field.put("crop", "corn");
				  field.put("fertilize", 1);
				  field.put("till", 0);
				  fields.add(field);
				}
				
				// SUSTAINABILITY
				JSONObject sustainabilityData = new JSONObject();
				
				sustainabilityData.put("environmentScore", 0.87);
				sustainabilityData.put("economicScore", 0.55);
				sustainabilityData.put("energyScore", 0.62);
				sustainabilityData.put("overallScore", 0.68 );
				sustainabilityData.put("rank", 1);
				
				// ECONOMICS
				JSONObject economicsData = new JSONObject();
				
				JSONObject revenue = new JSONObject();
				revenue.put("corn", 1500);
				revenue.put("grass", 0);
				revenue.put("alfalfa", 860);
				
				JSONObject prices = new JSONObject();
				prices.put("corn", 250);
				prices.put("alfalfa", 150);
				prices.put("grass", 80);
				
				economicsData.put("revenue", revenue);	
				economicsData.put("prices", prices);
				economicsData.put("costs", 800);
				economicsData.put("capital", 32000);
				economicsData.put("economicScore", 0.55);
				economicsData.put("economicRank", 2);
				
				//ENERGY
				JSONObject energyData = new JSONObject();
				
				JSONObject yield = new JSONObject();
				yield.put("corn", 32.47);
				yield.put("alfalfa", 0.0);
				yield.put("grass", 5.4);
				
				JSONObject energyProduced = new JSONObject();
				energyProduced.put("corn", 479000);
				energyProduced.put("alfalfa", 0.0);
				energyProduced.put("grass", 32000);
				
				energyData.put("yield", yield);
				energyData.put("energyProduced", energyProduced);
				energyData.put("energyScore", 0.475);
				energyData.put("energyRank", 3);
				
				
				//ENVIRONMENT
				JSONObject environmentData = new JSONObject();
				
				JSONObject waterData = new JSONObject();
				waterData.put("global", 0.47);
				waterData.put("farm-level", 0.88);
				
				JSONObject emissionsData = new JSONObject();
				emissionsData.put("totalEmissions", 120.0);
				emissionsData.put("N20", 100.0);
				emissionsData.put("respiration", 20);
				
				environmentData.put("waterData", waterData);
				environmentData.put("emissionsData", emissionsData);
				environmentData.put("environmentScore", 0.87);
				environmentData.put("environmentRank", 3);
				
				//FARM
				farmData.put("year", y);
				farmData.put("fields", fields);
				farmData.put("sustainabilityData", sustainabilityData);
				farmData.put("economicsData", economicsData);
				farmData.put("energyData",  energyData);
				farmData.put("environmentData", environmentData);
			
			reply.put("year_" + y, farmData);
			
			}

			String jsonString = JSONValue.toJSONString(reply);
			return jsonString;

		}
		    
}
