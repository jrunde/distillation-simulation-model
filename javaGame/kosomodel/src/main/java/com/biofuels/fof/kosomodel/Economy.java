package com.biofuels.fof.kosomodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSGlobals;
import com.gams.api.GAMSJob;
import com.gams.api.GAMSOptions;
import com.gams.api.GAMSParameter;
import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSWorkspaceInfo;

public class Economy {
	private int complexity;

	private double fuelPrice;
	private double feedPrice;
	private double baseFuelPrice, baseFeedPrice;
	private double stoverPrice, grainPrice, cornSubsidy;
	private double cornPrice;
	private double grassPrice;
	private double alfalfaPrice;
	private EconomyHistory history;
	private Game game;

	private Random normRandom;
	private double numFields;
	private double cornYield, grassYield, alfalfaYield;
	private double fuelDemand, feedDemand;  // L / Acre, kg/Acre
	private double fuelDemNorm, fuelDemStd;
	private double feedDemNorm, feedDemStd;
	
	double vegConvRate = 0.38;
    double grainConvRate = 0.4;

	private final String systemDirectory, workingDirectory;
	private GAMSWorkspace ws;
	private GAMSDatabase dbIn;
	private GAMSDatabase resultsDB;
	private GAMSOptions opts;
	private GAMSParameter yields, demands, prevSupply, convRate;

	private double cornGrainRatio, cornStoverRatio;
	private double fuelSupply, feedSupply;
	
	private CircularFifoQueue<Double[]> demandWindow;
	private CircularFifoQueue<Double[]> supplyWindow;

	public Economy(Game g){
		complexity = 0;
		history = new EconomyHistory();
		
		// Price is given in $/ton of crop
		cornPrice = 150; //http://www.indexmundi.com/commodities/?commodity=corn&months=60
		grassPrice = 80; //http://www.agmrc.org/commodities__products/biomass/switchgrass-profile/
		alfalfaPrice = 250; 

		// Current E-85 national avg price: 0.900827 $/L : http://www.afdc.energy.gov/fuels/prices.html
		// Average feed price: ??  http://www.ams.usda.gov/mnreports/lswfeedseed.pdf
		baseFeedPrice = 250;    // $/ton
		baseFuelPrice = .225;   // $/L

		// What to do with this subsidy thing?
		cornSubsidy = 0;  // $/ton

		game = g;

		normRandom = new Random();
		fuelDemNorm = 600*2.47105; // L/Acre
		feedDemNorm = 1000*2.47105; // kg/Acre
		fuelDemStd =  115.576*2.47105; //144.47;
		feedDemStd = 162.4*2.47105; //203;
		numFields = 0;
		
		demandWindow = new CircularFifoQueue<Double[]>(3);
		supplyWindow = new CircularFifoQueue<Double[]>(3);
		// Initialize queue to arrays of zeros;
		for (int i = 0; i < demandWindow.maxSize(); i++) {
			demandWindow.add(new Double[] {fuelDemNorm,feedDemNorm});
			supplyWindow.add(new Double[] {fuelDemNorm,feedDemNorm});
		}


		String uuid = UUID.randomUUID().toString();
		if(System.getenv("USER").equals("deploy")){
			workingDirectory = "/home/deploy/gams_tmp/gams_tmp" + uuid;
		} else if(System.getenv("USER").equals("proto")){
			workingDirectory = "/home/proto/gams_tmp/gams_tmp" + uuid;
		} else {
			workingDirectory = "/vagrant/gams_tmp/gams_tmp" + uuid;
		}
		createWorkingDirectory(workingDirectory);
		systemDirectory = System.getenv("GAMSDIR");

	}
	
	public void setBasePrices(double corn, double grass, double alfalfa) {
		this.cornPrice = corn;
		this.grassPrice = grass;
		this.alfalfaPrice = alfalfa; 

		this.baseFeedPrice = alfalfa;    // $/ton
		this.baseFuelPrice = grass/(1000*vegConvRate);   // $/L
	}
	
	public int getComplexity() {
		return complexity;
	}
	
	public void setComplexity(Integer complexity) {
		this.complexity = complexity;
	}
	
	public LinkedList<EconomyHistory.HistoryYear> getHistory() {
		return history.getHistory();
	}
	
	public void addEconomyYear(){		
		cornGrainRatio = 0; cornStoverRatio = 0;
		
		// Find total plant yields (in kg/Acre) & number of fields
		numFields = 0;
		cornYield = 1; grassYield = 1; alfalfaYield = 1; // Setting corn = 1 guarantees GAMS provides a cornratio output.
		for (Farm f:game.getFarms()) {
			for (Field fi:f.getFields()) {
				numFields++;
				if (fi.getCrop().equals(Crop.CORN)){
					cornYield += fi.getAbovegroundYield()/2.47015;
				}
				else if (fi.getCrop().equals(Crop.GRASS)){
					grassYield += fi.getAbovegroundYield()/2.47105;
				}
				else if (fi.getCrop().equals(Crop.ALFALFA)){
					alfalfaYield += fi.getAbovegroundYield()/2.47105;
				}
			}
		}

		if(complexity == 1){
			adjustPrices();
		}
		
		// Set the corn grain and stover distribution ratios for every field (must be same for all players)
		for (Farm f:game.getFarms()) {
			for (Field fi:f.getFields()) { 
				fi.setStoverDistro(cornStoverRatio);
				fi.setGrainDistro(cornGrainRatio);
			}
		}
		
		//System.out.println(complexity + " corn: " + cornPrice + " grass: " + grassPrice);
		history.addYear(cornPrice, grassPrice, alfalfaPrice, cornYield*2.47105, grassYield*2.47105, feedDemand, fuelDemand, feedSupply, fuelSupply);  //Yields passed in kg/ha basis.
	}

	private void adjustPrices() {

		// Create a small increase in annual demand for fuel
		fuelDemNorm += fuelDemNorm*0.05;

		// Allow pertebations in annual demand (weather effects)
		fuelDemand = (fuelDemNorm + normRandom.nextGaussian()*fuelDemStd)*numFields;  // L/Acre
		feedDemand = (feedDemNorm + normRandom.nextGaussian()*feedDemStd)*numFields;  // kg/Acre
		
		demandWindow.add(new Double[] {fuelDemand, feedDemand});  //Store demands in a 3 year window
		//Use weighted 3 year window of demands
		fuelDemand = demandWindow.get(0)[0]/4.0 + demandWindow.get(1)[0]/2.0 + demandWindow.get(2)[0];
		feedDemand = demandWindow.get(0)[1]/4.0 + demandWindow.get(1)[1]/2.0 + demandWindow.get(2)[1];
		fuelSupply = supplyWindow.get(1)[0]/4.0 + supplyWindow.get(2)[0]/2.0;
		feedSupply = supplyWindow.get(1)[1]/4.0 + supplyWindow.get(2)[1]/2.0;

		// Create GAMS workspace and pass parameters to economy QCP:
		GAMSWorkspaceInfo	wsInfo = new GAMSWorkspaceInfo();
		wsInfo.setWorkingDirectory(workingDirectory);
		wsInfo.setDebugLevel(GAMSGlobals.DebugLevel.OFF);
		ws = new GAMSWorkspace(wsInfo);
		dbIn	= ws.addDatabase("dbIn");

		yields = dbIn.addParameter("Yield", 1, "");
		yields.addRecord("Corn").setValue(this.cornYield);
		yields.addRecord("Grass").setValue(this.grassYield);
		yields.addRecord("Alfalfa").setValue(this.alfalfaYield);
		demands = dbIn.addParameter("Demand", 1, "");
		demands.addRecord("fuel").setValue(fuelDemand);  
		demands.addRecord("feed").setValue(feedDemand);
		prevSupply = dbIn.addParameter("prevSupply", 1, "");
		prevSupply.addRecord("fuel").setValue(fuelSupply);
		prevSupply.addRecord("feed").setValue(feedSupply);
		convRate = dbIn.addParameter("ConversionRate", 1, "");
		convRate.addRecord("cornGrain").setValue(grainConvRate);
		convRate.addRecord("cornStover").setValue(vegConvRate);
		convRate.addRecord("Grass").setValue(vegConvRate);

		opts = ws.addOptions();
		opts.defines("dbIn", "dbIn");
		GAMSJob gamsBotJob = ws.addJobFromFile("FoF_Economy_Market.gms");
		gamsBotJob.run(opts, dbIn);
		resultsDB = gamsBotJob.OutDB();

		// Retrieve corn distribution and supply/demand from GAMS
		cornGrainRatio = resultsDB.getVariable("alpha").findRecord("CornGrain").getLevel();   //Percent of grain used for feed.
		cornStoverRatio = resultsDB.getVariable("alpha").findRecord("CornStover").getLevel();  //Percent of stover used for feed.
		fuelSupply = resultsDB.getVariable("supply").findRecord("fuel").getLevel();
		feedSupply = resultsDB.getVariable("supply").findRecord("feed").getLevel();
		
		supplyWindow.add(new Double[] {fuelSupply, feedSupply}); //Store supply's in a 3 year window
		fuelSupply = supplyWindow.get(0)[0]/4.0 + supplyWindow.get(1)[0]/2.0 + supplyWindow.get(2)[0];
		feedSupply = supplyWindow.get(0)[1]/4.0 + supplyWindow.get(1)[1]/2.0 + supplyWindow.get(2)[1];

		System.out.println();
		System.out.println("GrainRatio: " + cornGrainRatio);
		System.out.println("StoverRatio: " + cornStoverRatio);
		System.out.println("Ethonal Demand: " + fuelDemand);
		System.out.println("Ethonal Supply: " + fuelSupply);
		System.out.println("Feed Demand: " + feedDemand);
		System.out.println("Feed Supply: " + feedSupply);
		System.out.println();
		System.out.println("Old corn price: " + getCornPrice());
		System.out.println("Old grass price: " + getGrassPrice());
		System.out.println("Old  price: " + getAlfalfaPrice());


		// Adjust prices based on marginal values.
		double fuelSignal = resultsDB.getEquation("fuelSupply_def").getFirstRecord().getMarginal();
		double feedSignal = resultsDB.getEquation("feedCES_def").getFirstRecord().getMarginal();
		
		double grainSignal = resultsDB.getEquation("rawFeed_def").findRecord("CornGrain").getMarginal();
		double stoverSignal = resultsDB.getEquation("rawFeed_def").findRecord("CornStover").getMarginal();
		double alfalfaSignal = resultsDB.getEquation("rawFeed_def").findRecord("Alfalfa").getMarginal();
		double totalFeedSignal = grainSignal + stoverSignal + alfalfaSignal;
		
		// Prevent extraordinary price changes (positive infinity edge cases)
		if (fuelSignal > 5) {fuelSignal = 5;}
		if (feedSignal > 5) {feedSignal = 5;}
		
		double fuelChange = fuelSignal*baseFuelPrice;
		double feedChange = feedSignal*baseFeedPrice;
		fuelPrice = baseFuelPrice + fuelChange*1.25;
		feedPrice = baseFeedPrice + feedChange*0.75;
		
		// Temper price changes near zero.
		while (fuelPrice <= baseFuelPrice/8) {
			fuelChange *= 0.9;
			fuelPrice = baseFuelPrice + fuelChange*1.25;
			System.out.println("Fuel change adjusted by 10%!");
		}
		while (feedPrice <= baseFeedPrice/8) {
			feedChange *= 0.9;
			feedPrice = baseFeedPrice + feedChange*0.75;
			System.out.println("Feed change adjusted by 10%!");
		}
		
		System.out.println();
		System.out.println("fuelSignal: " + fuelSignal);
		System.out.println("feedSignal: " + feedSignal);
		System.out.println("grainSignal: " + grainSignal);
		System.out.println("stoverSignal: " + stoverSignal);
		System.out.println("alfalfaSignal: " + alfalfaSignal);
		System.out.println();
		System.out.println("New fuel price: " + fuelPrice);
		System.out.println("New feed price: " + feedPrice);


		// Set crop prices based on feed/fuel prices
		// Acct for relative feed values between alfalfa and corn (in order to insentivize proper feed ratios).
		double stoverFeedPrice, grainFeedPrice;
		if (feedChange >= 0) {
			stoverFeedPrice = baseFeedPrice + feedChange*(stoverSignal/totalFeedSignal);
			grainFeedPrice = baseFeedPrice + feedChange*(grainSignal/totalFeedSignal);
			alfalfaPrice = baseFeedPrice + feedChange*(alfalfaSignal/totalFeedSignal);
		}
		else {
			stoverFeedPrice = baseFeedPrice + feedChange*((totalFeedSignal - stoverSignal)/totalFeedSignal);
			grainFeedPrice = baseFeedPrice + feedChange*((totalFeedSignal - grainSignal)/totalFeedSignal);
			alfalfaPrice = baseFeedPrice + feedChange*((totalFeedSignal - alfalfaSignal)/totalFeedSignal);
		}
				
		stoverPrice = cornStoverRatio*stoverFeedPrice + (1-cornStoverRatio)*(vegConvRate*1000*fuelPrice);
		grainPrice = cornGrainRatio*grainFeedPrice + (1-cornGrainRatio)*(grainConvRate*1000*fuelPrice);
		
		setCornPrice(Math.round((2.0/3.0)*grainPrice + (1.0/3.0)*stoverPrice + cornSubsidy));
		setGrassPrice(Math.round(vegConvRate*1000*fuelPrice));
		setAlfalfaPrice(Math.round(alfalfaPrice));
		
		// Prevent (Should be impossible) case of negative prices.
		if (this.getCornPrice() <= 0) {
			if (stoverFeedPrice <= 0) { stoverFeedPrice = baseFeedPrice/20.0; }
			if (grainFeedPrice <= 0) { grainFeedPrice = baseFeedPrice/20.0; }
			stoverPrice = cornStoverRatio*stoverFeedPrice + (1-cornStoverRatio)*(vegConvRate*1000*fuelPrice);
			grainPrice = cornGrainRatio*grainFeedPrice + (1-cornGrainRatio)*(grainConvRate*1000*fuelPrice);
			setCornPrice(Math.round((2.0/3.0)*grainPrice + (1.0/3.0)*stoverPrice + cornSubsidy));
		}
		if (this.getGrassPrice() <= 0) {
			setGrassPrice(Math.round(vegConvRate*1000*baseFuelPrice/20.0));
		}
		if (this.getAlfalfaPrice() <= 0) {
			setAlfalfaPrice(Math.round(baseFeedPrice/20.0));
		}

		System.out.println();
		System.out.println("New corn price: " + getCornPrice());
		System.out.println("New grass price: " + getGrassPrice());
		System.out.println("New alfalfa price: " + getAlfalfaPrice());
		System.out.println();

	}
	
	public double getStoverRatio() {
		return cornStoverRatio;
	}
	
	public double getGrainRatio() {
		return cornGrainRatio;
	}

	public double getCornPrice() {
		return cornPrice;
	}

	public void setCornPrice(double cornPrice) {
		this.cornPrice = cornPrice;
	}

	public void setGrassPrice(double grassPrice) {
		this.grassPrice = grassPrice;
	}

	public double getGrassPrice() {
		return grassPrice;
	}

	public void setAlfalfaPrice(double alfalfaPrice) {
		this.alfalfaPrice = alfalfaPrice;
	}

	public double getAlfalfaPrice() {
		return alfalfaPrice;
	}

	public static void createWorkingDirectory(String directory) {
		String working_dir = System.getProperty("user.dir");
		File dir = new File(directory);
		dir.mkdir();
		File inFile = new File(working_dir + "/javaGame/kosomodel/src/main/java/com/biofuels/fof/kosomodel/FoF_Economy_Market.gms");
		File outFile = new File(directory + "/FoF_Economy_Market.gms");
		try {
			copyFile(inFile, outFile);
		} catch (IOException e) {
			System.out.println("Second working dir: " + working_dir);
			System.out.println("User variable reported as: " + System.getenv("USER"));
			System.out.println("working directory is: " + directory);
			e.printStackTrace();
		}
	}

	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if(destination != null) {
				destination.close();
			}
		}
	}


}
