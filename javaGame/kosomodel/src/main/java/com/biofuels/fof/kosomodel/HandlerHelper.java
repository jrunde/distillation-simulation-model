package com.biofuels.fof.kosomodel;

import com.biofuels.fof.kosomodel.FieldHistory.HistoryYear;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import akka.actor.ActorRef;

public class HandlerHelper {

	public Map<String, Game> games = new HashMap<>();
	private ActorRef listener;
	private ActorRef handler;
	public HandlerHelper() {

	}

	public HandlerHelper(ActorRef listener, ActorRef handler) {
		this.listener = listener;
		this.handler = handler;
	}
	
	private boolean containsIgnoreCase(List<String> list, String s){
		Iterator<String> it = list.iterator();
		while(it.hasNext()){
			if(it.next().equalsIgnoreCase(s))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public String[] handle(String event){

		ArrayList<String> replies = new ArrayList<>();
		JSONObject eventObj = (JSONObject) JSONValue.parse(event);
		String clientID = "-1";

		if(eventObj.get("clientID") != null) clientID = (String) eventObj.get("clientID");

		String roomName = (String) eventObj.get("roomName");
		String roomID = (String) eventObj.get("roomID");
		if (roomID == null) roomID = roomName;
		String farmerName = (String) eventObj.get("userName");
		String deviseName = (String) eventObj.get("deviseName");

		switch (eventObj.get("event").toString()){
		case "validateRoom":
			if(games.get(roomName) != null || (roomName.length() >= 4 && roomName.substring(0, 4).equals("gen_"))) 
				sendMessage(buildJson(clientID.toString(), "validateRoom", "result", false));
			else
				sendMessage(buildJson(clientID.toString(), "validateRoom", "result", true));
			break;

		case "globalValidateRoom":
			boolean roomResult = false;
			boolean needsPass = false;
			boolean correctPass = false;
			if(games.get(roomName) != null){
				roomResult = true;
				if(games.get(roomName).hasPassword()){
					needsPass = true;
					if(games.get(roomName).getPassword().equals(eventObj.get("password")))
						correctPass = true;
				}
			}

			sendMessage(buildJson(clientID.toString(), "globalValidateRoom","roomResult",roomResult,"needsPassword",needsPass,
					"passwordResult",correctPass));
			break;

		case "globalJoinRoom":
			boolean joinResult = false;
			if(games.get(roomName) != null){
				if(games.get(roomName).hasPassword()){
					if(games.get(roomName).getPassword().equals(eventObj.get("password")))
						joinResult = true;
				}
				else
					joinResult = true;
			}
			sendMessage(buildJson(clientID.toString(), "globalJoinRoom","result",joinResult));
			break;

		case "clearAllGames":
			games.clear();
			break;

		case "createRoom":
			Long fieldCount = (Long)eventObj.get("fieldCount");
			Boolean soilVariation = (Boolean)eventObj.get("soilVariation");
			if(soilVariation == null)
				soilVariation = false ;

			if(games.get(roomName) != null){
				sendMessage(buildJson(clientID.toString(), "createRoom", "result", false));
			}
			else if(((String)eventObj.get("password")).length()>0){
				games.put(roomName, new Game(roomName, (String)eventObj.get("password"), (long)eventObj.get("playerCount"), soilVariation, this));
				games.get(roomName).changeSettings(fieldCount.intValue(), games.get(roomName).isContracts(), games.get(roomName).isManagement(),games.get(roomName).isHelpPopups());
				sendMessage(buildJson(clientID.toString(), "createRoom","result",true));
			}
			else{
				games.put(roomName, new Game(roomName, (long)eventObj.get("playerCount"), soilVariation, this));
				games.get(roomName).changeSettings(fieldCount.intValue(), games.get(roomName).isContracts(), games.get(roomName).isManagement(),games.get(roomName).isHelpPopups());
				sendMessage(buildJson(clientID.toString(), "createRoom","result",true));
			}

			//replies.add("{\"event\":\"createRoom\",\"result\":true}");
			break;

		case "validateUserName":
			roomResult = (roomExists(roomName) && !games.get(roomName).isFull());
			boolean nameResult = false;
			needsPass = false;
			correctPass = false;
			if(roomResult){
                String userName = ((String) eventObj.get("userName"));
				int l = userName.length();
				
				nameResult = !userName.equals("");
				nameResult = nameResult && (l < 5 || !userName.substring(l - 5, l).equals("(bot)"));
				nameResult = nameResult && !userName.equals("Your Farm");
				nameResult = nameResult && !userName.equals("**MODERATOR**");
				nameResult = nameResult && ((!farmerExistsInRoom(userName, roomName)));			
				nameResult = nameResult && !containsIgnoreCase(games.get(roomName).getBannedNames(), userName);
							
				needsPass = games.get(roomName).hasPassword();
				if (needsPass) {
					correctPass = games.get(roomName).getPassword().equals(eventObj.get("password"));
				}
			}
			sendMessage(buildJson(clientID.toString(), "validateUserName","roomResult",roomResult,"needsPassword",needsPass,
					"passwordResult",correctPass,"userNameResult",nameResult));
			break;                

		case "getFarmerList":
			broadcastFarmerList(roomID);
			broadcastGlobalInfo(roomID);
			break;

		case "kickPlayer":
			String farmer = (String) eventObj.get("player");
			Boolean result = games.get(roomID).removePlayer(farmer);
            
            JSONObject msg = new JSONObject();
			msg.put("event", "kickPlayer");
            msg.put("player", farmer);
			msg.put("roomID", roomID);
			msg.put("clientID", roomID);
			sendMessage(msg.toJSONString());
            
			broadcastFarmerList(roomID);
			break;

		case "plantField":
			games.get(roomID).getFarm(clientID).setField(((Long)eventObj.get("field")).intValue(),(String) eventObj.get("crop"));
			break;

		case "setFieldManagement":
			int field = ((Long)eventObj.get("field")).intValue();
			String technique = (String) eventObj.get("technique");
			boolean value = (boolean) eventObj.get("value");
			games.get(roomID).getFarm(clientID).changeFieldManagement(field, technique, value);
			break;

		case "getGameInfo":
			sendGetGameInfo(roomID, clientID);
			break;

		case "advanceStage":
			doAdvanceStage(roomID);
			break;

		case "farmerReady":
			Game g = games.get(roomID);
			g.getFarm(clientID).setReady(true);
			g.farmerReady(g.getFarm(clientID).getName());
			broadcastFarmerList(roomID);
			broadcastGlobalInfo(roomID);

			if(g.allReady())
				doAdvanceStage(roomID);
			break;

		case "getFarmData":
			g = games.get(roomID);
			if(g != null){
				if(g.getFarm(clientID)!=null) {
					sendGetFarmData(g.getFarm(clientID).getName(), roomID, clientID);
					sendGetGameInfo(roomID, clientID);
				} else if (g.getFarmForPlayer(farmerName) != null) {
					sendGetFarmData(g.getFarmForPlayer(farmerName).getName(), roomID, clientID);
					sendGetGameInfo(roomID, clientID);
				}
			}
			break;           

		case "setWaitForModerator":
			games.get(roomID).setWaitForModerator((String)eventObj.get("stage"), (boolean)eventObj.get("value"));
			break;

		case "endGame":
			/* This is not necessary
			for(Farm f:games.get(roomID).getFarms()){
				JSONObject msg1 = new JSONObject();
				msg1.put("event", "kickPlayer");
	            msg1.put("player", f.getName());
				msg1.put("roomID", roomID);
				msg1.put("clientID", roomID);
				sendMessage(msg1.toJSONString());
		    }*/
            
			games.remove(roomID);
			msg = new JSONObject();
			msg.put("event", "endGame");
			msg.put("roomID", roomID);
			msg.put("clientID", roomID);
			sendMessage(msg.toJSONString());
			break;

		case "getBotStrategy":
			String bot = eventObj.get("name").toString();
			msg = new JSONObject();
			msg.put("event", "getBotStrategy");
			msg.put("clientID", clientID);

			// Hook these up to the actual bot strategies
			Farm botFarm = games.get(roomID).getFarmForPlayer(bot);
			double weights[] = games.get(roomID).getBotMap().get(botFarm).getWeights();
			msg.put("economy", weights[0]);
			msg.put("environment", weights[1]);
			msg.put("energy", weights[2]);
			sendMessage(msg.toJSONString());
			break;

		case "assignBots":
			bot = eventObj.get("name").toString();
			double economy = Double.parseDouble(eventObj.get("economy").toString());
			double energy = Double.parseDouble(eventObj.get("energy").toString());
			double environment = Double.parseDouble(eventObj.get("environment").toString());
			double weightSum = economy + energy + environment;
			economy = economy/weightSum;
			energy = energy/weightSum;
			environment = environment/weightSum;

			Farm ourBotsFarm = games.get(roomID).getFarmForPlayer(bot);
			games.get(roomID).getBotMap().get(ourBotsFarm).updateWeights(economy, environment, energy);
			games.get(roomID).getBotMap().get(ourBotsFarm).run();

			break;

		case "applyModeratorSettings":
			Game game = games.get(roomID);

			// Set crop prices in game and in bots
			game.setPrices(
					Double.parseDouble(((JSONObject) eventObj.get("prices")).get("corn").toString()), 
					Double.parseDouble(((JSONObject) eventObj.get("prices")).get("grass").toString()), 
					Double.parseDouble(((JSONObject) eventObj.get("prices")).get("alfalfa").toString()));
			for(Bot b:game.getBots()){
				b.setPrices(
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("corn").toString()), 
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("grass").toString()), 
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("alfalfa").toString()));
			}

			// Reweight sustainability (bots are reassigned if box is checked)
			game.reweightSustainability(
					Integer.parseInt(((JSONObject) eventObj.get("sustainability")).get("economy").toString()),
					Integer.parseInt(((JSONObject) eventObj.get("sustainability")).get("energy").toString()), 
					Integer.parseInt(((JSONObject) eventObj.get("sustainability")).get("environment").toString()));

			// Change game settings. Update management boolean in bots.
			game.changeSettings(null, false, (boolean) eventObj.get("mgmtOptsOn"), 
					(boolean) eventObj.get("helpPopupsOn"));
			boolean dynamic = (boolean) eventObj.get("dynamicMarket");
			if (dynamic) game.getEconomy().setComplexity(1);
			else game.getEconomy().setComplexity(0);
			for(Bot b:game.getBots()){
				b.setManagement(game.isManagement());
			}

			//Adjust base prices of dynamic economy model (if game hasn't started).
			if ((boolean) eventObj.get("newBasePrices")) {
				game.getEconomy().setBasePrices(
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("corn").toString()), 
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("grass").toString()), 
						Double.parseDouble(((JSONObject) eventObj.get("prices")).get("alfalfa").toString()));
			}

			//Re-run the bots.
			for(Bot b:game.getBots()){
				(new Thread(b)).start();
			}

			sendGetGameInfo(roomID, clientID);
			break;

		case "togglePause":
			boolean state = games.get(roomID).togglePause();
			JSONObject pauseMsg = new JSONObject();
			pauseMsg.put("event", "togglePause");
			pauseMsg.put("clientID", roomID);
			pauseMsg.put("state", state);
			sendMessage(pauseMsg.toJSONString());
			break;

		case "newBot":
			games.get(roomID).addBot(roomID);
			break;

		case "joinRoom":
			boolean roomExist = roomExists(roomName);
			boolean shouldMakeNew = false;
			boolean shouldRejoin = false;

			if(roomExist){
				shouldMakeNew = !farmerExistsInRoom(farmerName, roomName) && !games.get(roomName).isFull();
				if(!shouldMakeNew && !games.get(roomName).isFull()){
					shouldRejoin = deviseName != null && games.get(roomName).getFarmForPlayer(farmerName).getCurrentUser().equals(deviseName);
				}
			}

			if(roomExist && !games.get(roomName).getBannedNames().contains(farmerName) && (shouldMakeNew || shouldRejoin) && games.get(roomName).getPassword().equals(eventObj.get("password")))
			{
				if(shouldMakeNew){
					games.get(roomName).addFarmer(farmerName, clientID, false);
					games.get(roomName).getFarmForPlayer(farmerName).setCurrentUser(deviseName);
				}
				else if(shouldRejoin){
					games.get(roomName).rejoinFarmer(farmerName, clientID);
				}
				sendMessage(buildJson(clientID.toString(), "joinRoom","result",true,"roomName",roomName,"userName",(String)eventObj.get("userName")));
				sendGetGameInfo(roomID, clientID);


				JSONArray list = new JSONArray();
				msg = new JSONObject();
				for(Farm f:games.get(roomName).getFarms()){
					JSONObject farm = new JSONObject();
					farm.put("name", f.getName());
					//farm.put("ready", true);
					list.add(farm);
				} 

				broadcastFarmerList(roomID);
				broadcastGlobalInfo(roomID);

				pauseMsg = new JSONObject();
				pauseMsg.put("event", "togglePause");
				pauseMsg.put("clientID", clientID);
				pauseMsg.put("state", games.get(roomName).isPaused());
				sendMessage(pauseMsg.toJSONString());
			}
			else
				sendMessage(buildJson(clientID.toString(), "joinRoom","result",false, "errorMessage", "We're sorry! For some unexpected reason, you were not able to join the game."));
			break;

		default:
		}

		String[] ret = new String[replies.size()];
		replies.toArray(ret);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public void doAdvanceStage(String roomID) {		
		if (games.get(roomID).getFarms().size() != games.get(roomID).getBots().size()) {
			
			System.out.println(games.get(roomID).getFarms().size());
			System.out.println(games.get(roomID).getBots().size());
			
			games.get(roomID).advanceStage();
			int stage = games.get(roomID).getStageNumber();
			String roundName = games.get(roomID).getStageName();
			int year = games.get(roomID).getYear();

			JSONObject replyAdvanceStage = new JSONObject();
			replyAdvanceStage.put("event", "advanceStage");
			replyAdvanceStage.put("stageNumber", stage);
			replyAdvanceStage.put("stageName", roundName);
			replyAdvanceStage.put("year", year);
			replyAdvanceStage.put("clientID", roomID);

			broadcastFarmerList(roomID);

			System.out.println("Advanced stage. New Stage == " + stage);
            System.out.println("Advanced stage. New Stage == " + roundName);
			// executes when in the planting stage
			if (stage == 0){		
				for(Farm fa:games.get(roomID).getFarms()){
					sendGetFarmData(fa.getName(), roomID, fa.getClientID());
				}
				sendGetGameInfo(roomID, roomID);
			}
			broadcastGlobalInfo(roomID);
			sendMessage(replyAdvanceStage.toJSONString());

			if (stage == 0) {
				for(Bot b:games.get(roomID).getBots())
					(new Thread(b)).start();
			}
		}

	}

	public void sendMessage(String message) {
		EventMessage msg = new EventMessage(message);
		listener.tell(msg, handler);
	}

	private boolean roomExists(String room){
		return games.get(room) != null;
	}

	private boolean farmerExistsInRoom(String farmer, String room){
		if(roomExists(room)){
			return games.get(room).hasFarmer(farmer);
		}
		return false;
	}

	private String buildJson(String clientID, String event, Object ... arguments){
		String start = "{\"event\":\""+event+"\",\"clientID\":\"" + clientID + "\",";
		StringBuilder sb = new StringBuilder(start);
		if(!(arguments.length % 2 == 0)){
			System.out.println("bad argument list; not an even number");
			return (sb.append("}")).toString();
		}
		for(int i = 0;i<arguments.length;i+=2){
			String str1 = arguments[i].toString();
			if(arguments[i] instanceof String){
				str1 = "\"" + arguments[i] + "\"";
			}
			String str2 = arguments[i+1].toString();
			if(arguments[i+1] instanceof String){
				str2 = "\"" + arguments[i+1] + "\"";
			}
			sb.append(str1);
			sb.append(":");
			sb.append(str2);

			if(i+2 == arguments.length){
				sb.append("}");
			}
			else{
				sb.append(",");
			}
		}
		return(sb.toString());
	}

	public void setListener(ActorRef actor) {
		this.listener = actor;
	}

	@SuppressWarnings("unchecked")
	public void sendGetFarmData(String farmerName, String roomID,  Object sendAddr){
		Game game = games.get(roomID);
		Farm farm = game.getFarmForPlayer(farmerName);
		Economy economy = game.getEconomy();
		LinkedList<com.biofuels.fof.kosomodel.FarmHistory.HistoryYear> farmHist = farm.getHistory();
		LinkedList<com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear> econHist = economy.getHistory();

		JSONObject reply = new JSONObject();
		JSONArray years = new JSONArray();
		reply.put("event", "getFarmData");

		for(int year=0; year<game.getYear(); year++){

			JSONObject farmData = new JSONObject();
			JSONArray fields = new JSONArray();

			// attach field data
			for(Field f:farm.getFields()){
				JSONObject field = new JSONObject();
				FieldHistory.HistoryYear fData = f.getHistory().getYear(year);
				field.put("x",f.getLoc()[0]);
				field.put("y",f.getLoc()[1]);
                
                if (fData == null) fData = f.getHistory().getYear(0);
                
                field.put("income", fData.fieldRevenue);
                field.put("costs", fData.costs);
                field.put("som", fData.SOM);
                field.put("bci", fData.BCI);
                field.put("gbi", fData.GBI);
                field.put("emissions", fData.emissions);
                field.put("n2o", fData.n2o);
                field.put("water", fData.fieldWater);
                field.put("respiration", fData.respiration);
                field.put("yield", fData.yield);
                field.put("netEnergy", fData.netTotalEnergy/40000);
                field.put("productionEnergy", fData.productionEnergy/40000);
                field.put("refineryEnergyIn", fData.refineryEnergyIn/40000);
                field.put("refineryEnergyOut", fData.refineryEnergyOut/40000);
                field.put("refinementEnergy", fData.refineryEnergy/40000);
                field.put("crop", fData.crop.toString());
                field.put("currentCrop", f.getCrop().toString());
                field.put("fertilize", fData.fertilizer);
                field.put("till", fData.till);
                if(game.getYear() == 1){
                    f.calcFieldScores();
                    field.put("fieldSoilScore", f.getFieldSoilScore());
                    field.put("fieldWaterScore", f.getFieldWaterScore());
                    field.put("fieldEmissionsScore", f.getFieldEmissionsScore());
                    field.put("fieldEnvScore", f.getFieldEnvScore());
                } else {
                    field.put("fieldSoilScore", fData.fieldSoilScore);
                    field.put("fieldWaterScore", fData.fieldWaterScore);
                    field.put("fieldEmissionsScore", fData.fieldEmissionsScore);
                    field.put("fieldEnvScore", fData.fieldEnvScore);
                }
				fields.add(field);
			}

			farmData.put("fields", fields);

			// create farm object for specific year from farm history
			com.biofuels.fof.kosomodel.FarmHistory.HistoryYear farmYear;
			farmYear = null;
			for(com.biofuels.fof.kosomodel.FarmHistory.HistoryYear h:farmHist){	
				if(h.year == year)
					farmYear = h;
			}

			// create economic object for specific year from economy history
			com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear econYear;
			econYear = null;
			for(com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear ey:econHist){	
				if(ey.year == year)
					econYear = ey;
			}

            if (farmYear == null || econYear == null) {
                for(com.biofuels.fof.kosomodel.FarmHistory.HistoryYear h:farmHist){	
                    if(h.year == 0) farmYear = h;
                }
                for(com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear ey:econHist){	
                    if(ey.year == 0) econYear = ey;
                }
            }
            
            // sustainability
            farmData.put("sustainabilityScore", farmYear.sustainabilityScore);
            farmData.put("sustainabilityRank", farmYear.sustainabilityRank);
            farmData.put("economyWeight", game.getEconomyWeight());
            farmData.put("energyWeight", game.getEnergyWeight());
            farmData.put("environmentWeight", game.getEnvironmentWeight());

            // economics
            farmData.put("cornIncome", farmYear.cornIncome);
            farmData.put("grassIncome", farmYear.switchgrassIncome);
            farmData.put("alfalfacropIncome", farmYear.alfalfaIncome);
            farmData.put("cornPrice", econYear.corn_price);
            farmData.put("alfalfaPrice", econYear.alfalfa_price);
            farmData.put("grassPrice", econYear.grass_price);
            farmData.put("feed_demand", econYear.feed_demand);
            farmData.put("fuel_demand", econYear.fuel_demand);
            farmData.put("feed_supply", econYear.feed_supply);
            farmData.put("fuel_supply", econYear.fuel_supply);
            farmData.put("costs", farmYear.costs);
            farmData.put("capital", farmYear.earnings);
            farmData.put("economicScore", farmYear.economicsScore);
            farmData.put("runningEconomicScore", farmYear.runningEconomicsScore);
            farmData.put("economicRank", farmYear.economicsRank);

            // energy
            farmData.put("cornYield", farmYear.cornYield);
            farmData.put("alfalfaYield", farmYear.alfalfaYield);
            farmData.put("grassYield", farmYear.grassYield);
            farmData.put("cornEnergy", farmYear.cornEnergy);
            farmData.put("alfalfaEnergy", farmYear.alfalfaEnergy);
            farmData.put("grassEnergy",  farmYear.grassEnergy);
            farmData.put("netEnergy",  farmYear.netEnergy);
            farmData.put("energyScore", farmYear.energyScore);
            farmData.put("runningEnergyScore", farmYear.runningEnergyScore);
            farmData.put("energyRank", farmYear.energyRank);

            // environment
            farmData.put("globalWater", farmYear.globalWater);
            farmData.put("globalEmissions", farmYear.globalEmissions);
            farmData.put("avgWater", farmYear.avgWater);
            farmData.put("avgEmissions", farmYear.avgEmissions);
            farmData.put("farmWater", farmYear.waterSubscore);
            farmData.put("totalEmissions", farmYear.emissions);
            farmData.put("emissionsSubscore", farmYear.emissionsSubscore);
            farmData.put("n2o", farmYear.n2o);
            farmData.put("som", farmYear.soilSubscore);
            farmData.put("gbi", farmYear.gbiSubscore);
            farmData.put("bci", farmYear.bciSubscore);
            farmData.put("respiration", farmYear.soilRespiration);
            farmData.put("environmentScore", farmYear.environmentScore);
            farmData.put("runningEnvironmentScore", farmYear.runningEnvironmentScore);
            farmData.put("environmentRank", farmYear.environmentRank);

            years.add(farmData);
        }

		reply.put("years", years);
		reply.put("clientID", sendAddr);
		reply.put("farmerName", farmerName);
		sendMessage(reply.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public void sendGetGameInfo(String roomID, Object sendAddr){
		Game game = games.get(roomID);
		int year = game.getYear();
		String stage = game.getStageName();
		boolean help = game.isHelpPopups();
		List<String> enabledStages = game.getEnabledStages();
		JSONArray stages = new JSONArray();
		JSONObject weights = new JSONObject();
		JSONObject bots = new JSONObject();
		JSONObject prices = new JSONObject();
		JSONObject strategy = new JSONObject();
		stages.addAll(enabledStages);
		JSONObject replyGameInfo = new JSONObject();
		replyGameInfo.put("event", "getGameInfo");
		replyGameInfo.put("year", year);
		replyGameInfo.put("stage", stage);
		replyGameInfo.put("helpPopupsOn", help);
		replyGameInfo.put("mgmtOptsOn", game.isManagement());

		weights.put("economy", game.getEconomyWeight());
		weights.put("energy", game.getEnergyWeight());
		weights.put("environment", game.getEnvironmentWeight());

		// create economic object for specific year from economy history
		com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear econYear;
		econYear = null;
		for(com.biofuels.fof.kosomodel.EconomyHistory.HistoryYear ey:game.getEconomy().getHistory()){	
			if(ey.year == year - 1) econYear = ey;
		}

		prices.put("corn", game.getEconomy().getCornPrice());
		prices.put("grass", game.getEconomy().getGrassPrice());
		prices.put("alfalfa", game.getEconomy().getAlfalfaPrice());

		int complexity = game.getEconomy().getComplexity();
		boolean dynamicMarket = false;
		if (complexity == 1) dynamicMarket = true;

		replyGameInfo.put("dynamicMarket", dynamicMarket);
		replyGameInfo.put("sustainabilityWeights", weights);
		replyGameInfo.put("bots", bots);
		replyGameInfo.put("prices", prices);
		replyGameInfo.put("enabledStages", stages);
		replyGameInfo.put("clientID", roomID);
		sendMessage(replyGameInfo.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private void broadcastGlobalInfo(String roomID){
		Game game = games.get(roomID);
		JSONObject reply = new JSONObject();
		reply.put("event", "globalInfo");
		reply.put("cornPrice", game.getEconomy().getCornPrice());
		reply.put("grassPrice", game.getEconomy().getGrassPrice());
		reply.put("alfalfaPrice", game.getEconomy().getAlfalfaPrice());
		reply.put("globalSustainability", game.globalSustain);
		reply.put("globalEconomy", game.globalEcon);
		reply.put("globalEnvironment", game.globalEnv);
		reply.put("globalEnergy", game.globalEnergy);
		reply.put("globalBCI", game.globalBCI);
		reply.put("globalGBI", game.globalGBI);
		reply.put("stageName", game.getStageName());
		reply.put("year", game.getYear());
		reply.put("clientID",roomID);
		sendMessage(reply.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private void broadcastFarmerList(String roomID){
		Game game = games.get(roomID);
		JSONArray list = new JSONArray();
		JSONObject msg = new JSONObject();
		for(Farm f:game.getFarms()){
			JSONObject farm = new JSONObject();
			farm.put("name", f.getName());
			farm.put("ready", f.isReady());
			farm.put("rank", f.getOverallRank());
			farm.put("economy", f.getEconRank());
			farm.put("energy",f.getEnergyRank());
			farm.put("environment", f.getEnvRank());
			list.add(farm);
		}
		msg.put("event", "getFarmerList");
		msg.put("clientID", roomID);
		msg.put("Farmers", list);
		sendMessage(msg.toJSONString());
	}
}