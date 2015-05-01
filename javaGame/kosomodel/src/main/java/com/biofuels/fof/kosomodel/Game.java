package com.biofuels.fof.kosomodel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.biofuels.fof.kosomodel.gameStage.GameStage;

public class Game {
    
	private final String roomName;
	private final boolean hasPassword;
	private boolean contracts=false;
	private boolean management=false;
	private boolean soilVariation;
	private final String password;
	private ConcurrentHashMap<String, Farm> farms;  //used because doesn't allow annoying null mappings
	private ConcurrentHashMap<Farm, Bot> bots;  //used because doesn't allow annoying null mappings
	private ConcurrentHashMap<Point, Field> fieldLocations ;
	private ConcurrentHashMap<String, Boolean> gameSettings = new ConcurrentHashMap<>();
	private ArrayList<String> bannedNames;
	private long maxPlayers;
	private RoundManager roundManager;
	private Economy economy;
    
	private boolean helpPopups = true;
	private boolean paused = true;
    
	private int readyFarmers;  // Number of farmers ready to move to next stage
	private List<String> readyFarms = new ArrayList<String>();
	
	private int gameYear=1;
	private int fieldsPerFarm=2;
   
	private double waterSummation;
	private double emissionSummation;
	private double globalEmissions;
	private double globalWater;
	private double avgWater;
	private double avgEmissions;
	public double globalBCI;
	public double globalGBI;
    
	public double globalSustain;
	public double globalEnv;
	public double globalEcon;
	public double globalEnergy;
    
    private int economyWeight;
    private int energyWeight;
    private int environmentWeight;
    
    private HandlerHelper gamesHelper;
    
    private File dataLog;
	
	private NameStorage nameStore = new NameStorage();
    
	
    
    
	public Game(String name, long maxPlayers, boolean soilVariation, HandlerHelper hh) {
		roomName = name;
		farms = new ConcurrentHashMap<>();
		bots = new ConcurrentHashMap<>();
		fieldLocations = new ConcurrentHashMap<>();
		hasPassword = false;
		gameSettings.put("gbi",false);
		gameSettings.put("bci", true);
		gameSettings.put("globalWater",false);
		this.soilVariation = soilVariation;
		password = "";
		setEconomy(new Economy(this));
		roundManager = new RoundManager();
		roundManager.Init(this);
		this.maxPlayers = maxPlayers;
		roundManager.AdvanceStage();
        
        economyWeight = 1;
        energyWeight = 1;
        environmentWeight = 1;
        
        gamesHelper = hh;
        
        bannedNames = new ArrayList<String>(populateBannedNames());
        
        dataLog = createDataLog();
	}
    
	public Game(String name, String pass, long maxPlayers, boolean soilVariation, HandlerHelper hh) {
		roomName = name;
		farms = new ConcurrentHashMap<>();
		bots = new ConcurrentHashMap<>();
		hasPassword = true;
		gameSettings.put("gbi",true);
		gameSettings.put("bci", true);
		gameSettings.put("globalWater",true);
		this.soilVariation = soilVariation;
		password = pass;
		this.maxPlayers = maxPlayers;
		setEconomy(new Economy(this));
		roundManager = new RoundManager();
		roundManager.Init(this);
		roundManager.AdvanceStage();
        
        economyWeight = 1;
        energyWeight = 1;
        environmentWeight = 1;
        
        gamesHelper = hh;
        
        bannedNames = new ArrayList<String>(populateBannedNames());
        
        dataLog = createDataLog();
	}
	
	private List<String> populateBannedNames(){
		Path path = Paths.get(System.getProperty("user.dir"), "badWordList.txt");
		System.out.println("Bad word list path: " + path);
		List<String> badWords = null;
		try {
			badWords = Files.readAllLines(path, Charset.defaultCharset());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return badWords;
	}
	
	private File createDataLog(){
		
		// Create directory to store logs (if doesn't exist)
		final String workingDirectory;
		if(System.getenv("USER").equals("deploy")){
			workingDirectory = "/home/deploy/data_logs";
		} else if(System.getenv("USER").equals("proto")){
			workingDirectory = "/home/proto/data_logs";
		} else {
			workingDirectory = "/vagrant/data_logs";
		}
		File dir = new File(workingDirectory);
		boolean dirCreated = dir.mkdir();
		System.out.println("Log directory created: " + dirCreated);
		
		// Check size of directory and delete old files if necessary
		boolean deleted = cleanDirectory(dir, 1000); //Set size limit to 1000MB
		if (deleted) {
			System.out.println("Directory size within limits!");
		}
		
		// Create new data log for game and generate headers.
		String name = getRoomName();
		String date = new SimpleDateFormat("MM_dd_yyyy_hhmma_z").format(new Date());
		String logid = "/" + date + "__" + name + ".csv";
		File data = new File(workingDirectory + logid);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(data)));
			pw.print("Round, Player, "
					+ "Field, OrganicN, RootBio, "
					+ "LastYear's Crop, ThisYear's Crop, Management On?, Tilled:, Fertilized:, "
					+ "Corn Price, GrassPrice, AlfalfaPrice, "
					+ "EcScore, EcRank, RunningEcScore, "
					+ "EnScore, EnRank, RunningEnScore, "
					+ "EnvScore, EnvRank, RunningEnvScore, "
					+ "OvScore, OvRank, RunningOvScore, "
					+ "NumPlayers\n");
		} catch (IOException e) {
			System.out.println("Log creation failed. Message: " + e.getMessage());
			e.printStackTrace();
		}		
		pw.close();
		// Return handle to data log so it can be written to later.
		return data;
	}
	
	/**
	 * Checks directory size and deletes oldest files if it is too large
	 * @param directory Directory to check.
	 * @param max Maximum size in MB
	 * @return True only if files were deleted.
	 */
	public boolean cleanDirectory(File directory, int max) {
		int maxSize = max; // Maximum directory size of ## MB.
		int dirSize = (int) getDirSizeMB(directory); //Return size in megabytes.
		
		if (dirSize < maxSize) {
			System.out.println("Current Directory: " + directory.getPath());
			System.out.println("Current Size: " + dirSize + " MB");
			System.out.println("Max Size: " + maxSize + " MB");
			return false; // Size within limits, do nothing.
		}
		
		int counter = 0;
		int itrLimit = 50; // Prevent infinite looping in case of error.
		while (dirSize > maxSize && counter < itrLimit) {
			
			File oldestFile = null;
			Date oldestDate = new Date();
			
			//Get list of all files in directory
			File[] fileList = directory.listFiles();
			//Look for oldest file
			for (File f:fileList) {
				Date currDate = new Date(f.lastModified());
				if (currDate.before(oldestDate)) {
					oldestDate = currDate;
					oldestFile = f;
				}
			}
			//Delete the oldest file/directory
			boolean deleted = delete(oldestFile);
			if(!deleted){System.out.println("Error: Nothing was deleted!");}
			
			//Recompute directory size
			dirSize = (int) getDirSizeMB(directory);
			System.out.println("New directory size: " + dirSize + " MB");
			
			counter++;
		}
		
		return true;
	}
	
	/**
	 * Recursively deletes all files in a directory, then the directory itself.
	 * @param dir Directory to be deleted.
	 * @return True only if files were deleted.
	 */
	private boolean delete(File dir) {
		String fileName = dir.getName();
		boolean deleted = false;
		if (dir.isFile()) {
			deleted = dir.delete();
			if (deleted) {System.out.println(fileName + " deleted!");}
			else {System.out.println("Deletion of " + fileName + "failed!");}
		}
		else {
			for (File file : dir.listFiles() ) {
				if (file.isFile()) {file.delete();}
				else {this.delete(file);}
			}
			deleted = dir.delete();
			if (deleted) {System.out.println(fileName + " deleted!");}
			else {System.out.println("Deletion of " + fileName + "failed!");}
		}
		return deleted;
	}
	
	/**
	 * Recursively finds the total size of a directory.
	 * @param dir Directory whose size is to be calculated
	 * @return Size of directory in bytes
	 */
	private long getDirSize(File dir) {
		long size = 0;
		if (dir.isFile()) {size = dir.length();} 
		else {
			for (File file : dir.listFiles()) {
				if (file.isFile()) {size += file.length();} 
				else {size += this.getDirSize(file);}
			} //For loop
		} //Else Statement
		return size; //Return bytes!
	}
	
	private long getDirSizeMB(File dir) {
		return getDirSize(dir)/1024/1024; // Returns directory size in MB.
	}
	
	public File getDataLog() {
		return dataLog;
	}
    
	public String getRoomName() {
		return roomName;
	}
    
	public Boolean isSoilVariation() {
		return soilVariation;
	}
    
	public ArrayList<String> getBannedNames() {
		return bannedNames;
	}
    
	public ConcurrentHashMap<String,Boolean> getGameSettings(){
		return gameSettings;
	}
	
	public Collection<Bot> getBots() {
		return bots.values();
	}
	
	public ConcurrentHashMap<Farm,Bot> getBotMap(){
		return bots;
	}
    
	public int[] coordinates(int farmNumber){
        
		if(farmNumber == 1)
            return new int[]{1,1};
        
		int square = 4;
		int x = 1, y = 1, i = 1;
		int xoff = (int) (Math.sqrt(square) - 1), yoff = (int) (Math.sqrt(square) - 1);
        
		while(true){
			y = 1;
			x += 1;
            
			i ++;
			if(farmNumber == i)
				return new int[]{x,y};
            
			while(xoff>0 && yoff>0){
				x -= xoff; y += yoff;
                
				i ++;
				if(farmNumber == i)
					return new int[]{x,y};
                
				yoff -= 1;
				x += xoff; y -= yoff;
                
				i ++;
				if(farmNumber == i)
					return new int[]{x,y};
                
				xoff -= 1;
			}
            
			square = (int) ((Math.sqrt(square)+1)*(Math.sqrt(square)+1));
			xoff = (int) (Math.sqrt(square) - 1); yoff = (int) (Math.sqrt(square) - 1);
		}
	}
    
	public int[] farmDimensions(int nFields){
		if(nFields==1)
			return new int[]{1,1};
		else if(nFields <= 4)
			return new int[]{2,2};
		else  //if(nFields <= 6)
			return new int[]{2,3};
	}
    
	public Double[] generateSoil(int base){
		Double[] ret = new Double[getFieldsPerFarm()];
        
		double amt = (double) base ;// (float) (base * ret.length);
        
		for(int i=0; i<ret.length;i++){
			if(soilVariation)
				//ret[i] = (Double) (((double) i+1) / ((double) ret.length / 2)) * amt ;
			    ret[i] = ((double) (i+1)/ (double) ret.length) * amt;
			else
				//ret[i] = (double) base / 2;
				ret[i] = (double) 1400;
		}
		Collections.shuffle(Arrays.asList(ret));
		return ret;
	}
    
	public boolean hasFarmer(String name) {
        
        name = name.toLowerCase();
		for(Farm f:farms.values()){
			if (f.getName().toLowerCase().equals(name))
				return true;
		}
		return false;
	}
    
	public Farm addFarmer(String newPlayer, String clientID, boolean alwaysReady) {
		Farm f = new Farm(newPlayer, 10000, farms.size()+1, this, alwaysReady);
		int[] loc = coordinates(farms.size()+1);
		int[] dims = farmDimensions(getFieldsPerFarm());
		Double[] organicNstarts = generateSoil(2200);
        
		f.setClientID(clientID);
        
		for(int i = 0;i<getFieldsPerFarm();i++){
			int fieldx = loc[0] +  (loc[0] - 1) * (dims[0] - 1) + (i % dims[0]);
			int fieldy = (loc[1] + (loc[1] - 1) * (dims[1] - 1) + dims[1] - 1) - (i / dims[0]);
            
			Field fi = new Field(fieldx,fieldy, organicNstarts[i]);
            fi.addHistoryYear();
			fieldLocations.put(new Point(fieldx,fieldy),fi);
			f.getFields().add(fi);
		}
        f.addHistoryYear();
		farms.put(clientID, f);
		return f;
	}
    
	public ArrayList<Field> neighbors(int x, int y){
		ArrayList<Field> hood = new ArrayList<>();
        
		for(int i=-1;i<2;i++){
			for(int j=-1;j<2;j++){
				Field fi = fieldLocations.get(new Point(x+i,y+j));
				if(fi != null)
					hood.add(fi);
			}
		}
        
		return hood;
	}
    
	public ArrayList<Farm> dumpFields(){
		ArrayList<Farm> fields = new ArrayList<>();
        
		for(Farm f:farms.values()){
			fields.add(f);
			// for(Field fi:f.getFields()){
            // fields.add(fi);
			// }
		}
        
		return fields;
	}
    
	public Boolean hasPassword(){
		return hasPassword;
	}
    
	public String getPassword(){
		return password;
	}
    
	public boolean isHelpPopups(){
		return helpPopups;
	}
    
	public long getMaxPlayers() {
		return maxPlayers;
	}
    
	public void setPrices(double corn, double grass, double alfalfa){
		economy.setCornPrice(corn);
		economy.setGrassPrice(grass);
		economy.setAlfalfaPrice(alfalfa);
		for (Bot b: getBots()){
			b.setPrices(corn,  grass,  alfalfa);
		}
	}
    
	public boolean isContracts() {
		return contracts;
	}
    
	public void setContracts(boolean contracts) {
		this.contracts = contracts;
	}
    
	public boolean isManagement() {
		return management;
	}
    
	public void setManagement(boolean management) {
		this.management = management;
		for (Bot b: getBots()){
			b.setManagement(management);
		}
	}
    
	public boolean isFull(){
		return(farms.size() >= maxPlayers);
	}
    
	public void setField(int clientID, int field, Crop crop){
		farms.get(clientID).getFields().get(field).setCrop(crop);
	}
    
	public ArrayList<String> getFieldsFor(Integer clientID) {
		ArrayList<String> cropList = new ArrayList<>();
		for(Field f:farms.get(clientID).getFields()){
			cropList.add(f.getCrop().toString());
		}
		return cropList;
	}
    
	public ArrayList<Farm> getFarms() {
        
		return new ArrayList<>(farms.values());
	}
    
	public Farm getFarmForPlayer(String name) {
		for(Farm f:farms.values()){
			if (f.getName().equals(name))
				return f;
		}
		return null;
	}
    
	public Boolean removePlayer(String name){
		Farm f = getFarmForPlayer(name);
		if(f == null) {
			System.out.println("Error: Player not removed! Could not find farm"
					+ " with name " + name);
			return false ;
		}
		
		f.setReady(false);		
		if (readyFarms.contains(name)){
			readyFarms.remove(name);
			readyFarmers--;
		}
        
		bannedNames.add(name);
		bots.remove(f);
		Farm removed = farms.remove(f.getClientID());
		if(removed == null) {
			System.out.println("Error: farm not removed from hashmap! ClientID "
					+ "did not match key in farms hashmap.");
			return false;
		}
		System.out.println(removed.getName() + " has been removed.");
	
		
		if (allReady()) {
			callHelpersAdvanceStage();
		}
        
		return true ;
	}
    
	public void rejoinFarmer(String farmerName, String clientID) {
        
		Farm farm = getFarmForPlayer(farmerName);
		farms.remove(getFarmForPlayer(farmerName).getClientID());
		farm.setClientID(clientID);
		farms.put(clientID, farm);
	}
    
	public Farm getFarm(String clientID) {       
		return farms.get(clientID);
	}
    
	public double getGlobalWater() {
		return globalWater;
	}
	
	public double getGlobalEmissions(){
		return globalEmissions;
	}
	
	public double getAvgWater(){
		return avgWater;
	}
	
	public double getAvgEmissions(){
		return avgEmissions;
	}
    
	public void updateGlobals(){
        
        
		//get phos
		double cornCount = 0.0;
		emissionSummation = 0.0;
		waterSummation = 0.0;
        
		for (Farm fa:farms.values()) {
            
			for(Field f:fa.getFields()){
				waterSummation += f.getNitrogenLeached();
				emissionSummation += f.fieldProductionEmissions;
				System.out.println("Field level emissions (for summation) = " + f.fieldProductionEmissions);
				if(f.getCrop() == Crop.CORN){
					cornCount ++;
				}				
			}
		}
        
        
        
		double cornRatio = cornCount / (farms.size() * getFieldsPerFarm());
		double phosphorous = Math.pow(10, (.79 * cornRatio) - 1.44);
        
		//globalWater = 1 - ((phosphorous - 0.0363) / 0.1876);
		double maxNleached = 272/2.47105; //kg/acre, assuming max available N and left fallow (no uptake, all leached) ALSO FOUND IN FARM.JAVA!!!
		globalWater = waterSummation;
		globalEmissions = emissionSummation;
		avgWater = 1 - (globalWater / (maxNleached * this.fieldsPerFarm * this.farms.size()));
		avgEmissions = globalEmissions / this.farms.size();
        
	}
    
	public void rerankFarms(){
		ArrayList<Double> econScores = new ArrayList<>();
		ArrayList<Double> envScores = new ArrayList<>();
		ArrayList<Double> energyScores = new ArrayList<>();
		ArrayList<Double> sustainabilityScores = new ArrayList<>();
        
		for(Farm f:farms.values()){
			econScores.add(f.getEconScoreRunning());
			envScores.add(f.getEnvScoreRunning());
			energyScores.add(f.getEnergyScoreRunning());
		}
        
		Collections.sort(econScores);
		Collections.sort(envScores);
		Collections.sort(energyScores);
        
		//FIXME Should not be running calculations twice (??? still here?)
		for(Farm f:farms.values()){
			f.setEconRank(econScores.size() - econScores.lastIndexOf((double)f.getEconScoreRunning()));
			f.setEnvRank(envScores.size() - envScores.lastIndexOf(f.getEnvScoreRunning()));
			f.setEnergyRank(energyScores.size() - energyScores.lastIndexOf(f.getEnergyScoreRunning()));
            
			sustainabilityScores.add(f.getOverallScore());
		}
        
		Collections.sort(sustainabilityScores);
        
		for(Farm f:farms.values()){
			f.setOverallRank(sustainabilityScores.size() - sustainabilityScores.lastIndexOf(f.getOverallScore()));
		}
        
		Double sum = 0.0;
		Double nfarmers = (double) getFarms().size();
        
		for(Double d:econScores)
			sum += d;
		globalEcon = sum / nfarmers;
        
		sum = 0.0;
		for(Double d:envScores)
			sum += d;
		globalEnv = sum / nfarmers;
        
		sum = 0.0;
		for(Double d:energyScores)
			sum += d;
		globalEnergy = sum / nfarmers;
        
		sum = 0.0;
		for(Double d:sustainabilityScores)
			sum += d;
		globalSustain = sum / nfarmers;
        
		double bciSum = 0.0;
		for(Farm fa:farms.values())
			bciSum += fa.bciSubscore;
		globalBCI = bciSum / (double) getFarms().size();
		
		double gbiSum = 0.0;
		for(Farm fa:farms.values())
			gbiSum += fa.gbiSubscore;
		globalGBI = gbiSum / (double) getFarms().size();
	}
    
	public void changeSettings(Integer fields, boolean contracts, boolean management, boolean help) {
		if(fields != null){
            int currFields = fieldsPerFarm;
            fieldsPerFarm = fields;
            if(farms.size()>0){
                currFields = ((Farm)farms.values().toArray()[0]).getFields().size();
            }
            for(Farm fa:farms.values()){
                fa.setReady(false);
            }
            resetReadyFarmers();
            
            if(fields < currFields){
                System.out.println("destroying fields not implemented yet");
            }
            else if(fields > currFields){
                for(Farm f:farms.values()){
					int[] loc = coordinates(f.getID());
                    
                    for(int i = 0;i<fields - currFields;i++){
                        f.getFields().add(new Field(loc[0],loc[1],50));
                    }
                }
            }
        }
		this.contracts = contracts;
		setManagement(management);
		//this.management = management;
		roundManager.resetStages();
        
		this.helpPopups = help;
        
	}
    
	public List<Field> getFields(String clientID) {
		return farms.get(clientID).getFields();
	}
    
	public int getYear() {
		return gameYear;
	}
    
	public void setYear(int year){
		gameYear = year;
	}
    
	public int getStageNumber() {
		return roundManager.getCurrentStageNumber();
	}
    
	public List<String> getEnabledStages() {
		ArrayList<String> ret = new ArrayList<String>();
		List<GameStage> stages = roundManager.getEnabledStages();
		for (GameStage s:stages){
			ret.add(s.getName());
		}
		return ret;
	}
    
	public void advanceStage() {
		//roundManager.AdvanceStage();
		for(Farm fa:farms.values()){
			fa.setReady(false);
		}
		resetReadyFarmers();
		roundManager.AdvanceStage();
		if (this.getStageNumber() == 0)
			setYear(getYear()+1);
	}
	
	public void callHelpersAdvanceStage(){
		gamesHelper.doAdvanceStage(roomName);
	}
	
	public boolean allReady(){
        //System.out.println("readies, need: " + (getFarms().size() - bots.size()) + ", have: " + getReadyFarmers());
		//return (getReadyFarmers() == (getFarms().size() - bots.size())) && !isWaitForModerator();
		return (getReadyFarmers() >= (getFarms().size())) && !isWaitForModerator();
	}
	
	public void farmerReady(String farmName) {
		if (!readyFarms.contains(farmName)){
			readyFarms.add(farmName);
			readyFarmers++;
		}
	}
    
	public int getReadyFarmers() {
		return readyFarmers;
	}
    
	public void resetReadyFarmers() {
		readyFarms.clear();
		this.readyFarmers = 0;
	}
    
	public boolean isFinalRound() {
		return false;
	}
    
	public String getStageName() {
		return roundManager.getCurrentStageName();
	}
    
	public int getCapitalRank(Farm farm) {
        
		return -1;
	}
	
	public void findFarmerYields() {
		for (Farm f:farms.values()) {
			for (Field fi:f.getFields()) {
				double yield = fi.calculateYield(); // calculate yield (kg per ha)
			}
		}
	}
    
	public void sellFarmerCrops() {
		
        
        // for each farm
		for(Farm f:farms.values()){
        
            // initialize round farm values
			f.farmRevenue = 0;
			
            f.cornEnergy = 0;
			f.grassEnergy = 0;
			f.alfalfaEnergy = 0;
            f.netEnergy = 0;
            f.farmProductionEmissions = 0;
            
			f.farmCosts = 0.0;

            
            // for each field
			for(Field fi:f.getFields()){
				System.out.println("    Field-level summation of costs:");
				System.out.println("        farm-level cost = $" + f.farmCosts);

				//double yield = fi.calculateYield(); // calculate yield (kg per ha)
				double yield = fi.getAbovegroundYield(); //kg/ha
				double fieldEnergy = fi.calculateEnergy(); // calculate energy   (returns MJ of energy per 40 acre field )
				f.netEnergy += fieldEnergy; // update farm net energy value
				f.farmProductionEmissions += fi.updateProductionEmissions(yield); // calculate production emissions

				// update farm crop energy values
				if (fi.getCrop().equals(Crop.CORN)) f.cornEnergy += fieldEnergy;
				else if (fi.getCrop().equals(Crop.GRASS)) f.grassEnergy += fieldEnergy;
				else if (fi.getCrop().equals(Crop.ALFALFA)) f.alfalfaEnergy += fieldEnergy;


				fi.setLastYield(yield); // update yield - kg/ha
				//fi.updateSOC(); // update soil health
                f.farmCosts += fi.getCosts(); // update costs ($/field)
                f.farmRevenue += fi.getFieldRevenue(economy); // update revenue
			}

            // System.out.println("farm respiration = " + f.getRespiration() + ", farm production emissions = " + f.productionEmissions + ", total emissions = " + (f.getRespiration() + f.productionEmissions));
            // set farm costs
			f.setCosts((int)f.farmCosts);
            
			// update farm emissions
			f.updateEmissions();
			
            // set farm capital
			System.out.println("FARM REVENUE FOR " + f.getName());
			System.out.println("     Value of farm-level costs = $" + f.farmCosts);
			System.out.println("     Value of farm-level revenue = $" + f.farmRevenue);
			System.out.println("     Farm net = $" + (f.farmRevenue - f.farmCosts));
			double capital = f.getCapital() + f.farmRevenue - f.farmCosts;
			int capitalInt = (int)capital;
			// System.out.println("     Value passed to farm.setCaptial = $" + capitalInt);
			f.setCapital(capitalInt);
		}
	}
    
    
	public void clearFields() {
		for(Farm f:farms.values()){
			for(Field fi:f.getFields()){
				if(fi.getCrop().equals(Crop.CORN))
					fi.setCrop(Crop.FALLOW);
			}
		}
	}
    
	public int getFieldsPerFarm() {
		return fieldsPerFarm;
	}
    
	public int getLargestEarnings() {
		int max = -1;
		for(Farm f:farms.values()){
			if(f.getCapital() > max){
				max = f.getCapital();
			}
		}
		return max;
	}
    
	public void setWaitForModerator(String stage, boolean value) {
		roundManager.setWaitForModerator(stage, value);
	}
    
	public boolean isWaitForModerator() {
		return roundManager.isWaitForModerator();
	}
    
	public Economy getEconomy() {
		return economy;
	}
    
	private void setEconomy(Economy economy) {
		this.economy = economy;
        this.economy.addEconomyYear();
	}
    
	public boolean togglePause() {
		setPaused(!isPaused());
		return isPaused();
	}
    
	public boolean isPaused() {
		return paused;
	}
    
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
    
	//public void addBot(String roomID, double ec, double env, double en)) {
	public void addBot(String roomID) {
        
		String id = UUID.randomUUID().toString();
		
        //		String name = (Long.toString(System.nanoTime(),32))+ " (bot)";
		String name = nameStore.take() + " (bot)";
		//Farm f = addFarmer(name, id, true); // 'true' parameter makes bots always ready (never waited on)
		Farm f = addFarmer(name, id, false);
		//SimpleBot b = new SimpleBot(f);
		GamsBot b = new GamsBot(this.management, 1.0/3, 1.0/3, 1.0/3, f);
		// GamsBot b = new GamsBot(this.management, ec, env, en, f);
		bots.put(f, b);
		
		//String threadID = String.valueOf((int)(Math.random()*1000000000));
		if(roundManager.getCurrentStageName() == "Plant") {
			//(new Thread(b, threadID)).start();
			(new Thread(b)).start();
		}	
	}
    
    public void reweightSustainability(int economy, int energy, int environment) {
        
        this.economyWeight = economy;
        this.energyWeight = energy;
        this.environmentWeight = environment;
    }
    
    public int getEconomyWeight() {
        
        return this.economyWeight;
    }
    
    public int getEnergyWeight() {
        
        return this.energyWeight;
    }
    
    public int getEnvironmentWeight() {
        
        return this.environmentWeight;
    }
}
