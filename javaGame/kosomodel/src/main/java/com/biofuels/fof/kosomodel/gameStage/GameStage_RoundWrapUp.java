package com.biofuels.fof.kosomodel.gameStage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.Bot;
import com.biofuels.fof.kosomodel.Farm;
import com.biofuels.fof.kosomodel.Field;
import com.biofuels.fof.kosomodel.Game;


//------------------------------------------------------------------------------
public class GameStage_RoundWrapUp extends GameStage {

	public GameStage_RoundWrapUp(Game g) {
		super(g);
	}
	public boolean ShouldEnter() {return true; }

	public void Enter() {
		//Make all bots ready in the harvest phase.
		for(Bot b:game.getBots()){
			b.makeReady();
		}
		
		//Log Player Decisions:
		File data = game.getDataLog();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(data, true)));
		} catch (IOException e) {
			System.out.println("Appending file failed. Message:" + e.getMessage());
			e.printStackTrace();
		}

		Iterator<Farm> farmIt = game.getFarms().iterator();
		while(farmIt.hasNext()){
			Farm nextFarm = farmIt.next();
			int fNum = 1;
			for(Field f:nextFarm.getFields()){
				pw.print(nextFarm.getHistory().size() + ", " + nextFarm.getName() + ", ");
				pw.print(fNum + ", " + f.getOrganicN() + ", " + f.getRootBiomass() + ", ");
				pw.print(f.getCurrentCrop() + ", " + f.getCrop() + ", " + game.isManagement() + ", " + 
						f.isTill() + ", " + f.isFertilize() + ", ");
				pw.print(game.getEconomy().getCornPrice() + ", " +
						game.getEconomy().getGrassPrice() + ", " + 
						game.getEconomy().getAlfalfaPrice() + ", ");
				pw.print(nextFarm.getEconScore() + ", " + nextFarm.getEconRank() + ", " + nextFarm.getEconScoreRunning() + ", " +
						nextFarm.getEnergyScore() + ", " + nextFarm.getEnergyRank() + ", " + nextFarm.getEnergyScoreRunning() + ", " +
						nextFarm.getEnvScore() + ", " + nextFarm.getEnvRank() + ", " + nextFarm.getEnvScoreRunning() + ", " +
						nextFarm.getOverallScore() + ", " + nextFarm.getOverallRank() + ", " + nextFarm.getOverallScoreRunning() + ", " +
						game.getFarms().size() + "\n");
				fNum++;
			}
		}
		pw.close();		
	}

	public void Exit() {
		//compute new SOM for each fields
		for (Farm fa:game.getFarms()){
			fa.updatePhosphorous();
			fa.updateBCI();
			fa.updateGBI();
			// for(Field fi:fa.getFields()){
			// fi.updateSOC(); // moved to game
			// fi.updateN2o(); // no longer used (replaced with productionEmissions
			// fi.updateEmissions(); // moved to game
			// }
		}
		
		// calculate yields
		game.findFarmerYields();
		
		// update economy
		game.getEconomy().addEconomyYear();

		// sell farmer crops
		game.sellFarmerCrops();

		// update global scores
		game.updateGlobals();

		// add field histories
		for (Farm fa:game.getFarms()){
			for(Field fi:fa.getFields()){
				fi.calcFieldScores();
				fi.addHistoryYear();
			}
		}

		// recompute each farm's scores
		for (Farm fa:game.getFarms()){
			fa.recomputeScores();
			fa.recomputeRunningScores();
		}

		// rerank the farms
		game.rerankFarms();

		// update the crop yields for each farm
		double grass=0;
		double corn=0;
		// TODO: alfalfa crop yield?
		for (Farm fa:game.getFarms()){
			fa.addHistoryYear();

			grass += fa.getHistory().getLast().grassYield;
			corn += fa.getHistory().getLast().cornYield;
		}

		// Update bot price recognition.
		for (Bot b: game.getBots()){
			b.setPrices(game.getEconomy().getCornPrice(), game.getEconomy().getGrassPrice(), game.getEconomy().getAlfalfaPrice());
		}

		// clear the fields for the next year
		game.clearFields();

		/*		for(Farm fa:game.getFarms()){
            sendGetFarmData(fa.getName(), game.getRoomName(), fa.getClientID());
		}   */

	}
	public void HandleClientData(JSONObject data) {}
	@Override
	public String getName() {
		return "Round Wrap Up";
	}
	@Override
	public boolean passThrough() {
		return false;
	}
}