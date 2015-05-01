package com.biofuels.fof.kosomodel;

// package com.gams.examples.transport;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSOptions;
import com.gams.api.GAMSParameter;
import com.gams.api.GAMSParameterRecord;
import com.gams.api.GAMSSet;
import com.gams.api.GAMSSetRecord;
import com.gams.api.GAMSSymbol;
import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSJob;
import com.gams.api.GAMSWorkspaceInfo;
import com.gams.api.GAMSGlobals;


public class GamsBot implements Bot/*, Runnable*/ {
	private GAMSWorkspace ws;
	private GAMSDatabase dbIn;
	private GAMSDatabase resultsDB;
	private GAMSOptions opts;

	private GAMSSet field;
	private GAMSParameter job_capital, job_price, job_ec_score, job_weights, job_z1_last, job_grainRatio, job_stoverRatio, job_prev_ON, job_prev_Root, z2_up;

	private double ec_weight_val, env_weight_val, en_weight_val;
	private Farm farm;
	private final String systemDirectory;
	private String workingDirectory;
	private boolean management;
	private ConcurrentHashMap<Field,String> field_names;

	private double cornPrice;
	private double grassPrice;
	private double alfalfaPrice;

	// public int[] arguments	= new int[] {Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType (), Syntax.NumberType ()};

	public GamsBot(boolean management, double ec_weight_val, double env_weight_val, double en_weight_val, Farm f){

		field_names = new ConcurrentHashMap<>();
		this.management = management;
		farm = f;
		
		// Initiate to same prices as the starting economy!
		this.cornPrice = f.getGame().getEconomy().getCornPrice();
		this.grassPrice = f.getGame().getEconomy().getGrassPrice();
		this.alfalfaPrice = f.getGame().getEconomy().getAlfalfaPrice();

		if (Math.abs(ec_weight_val + env_weight_val + en_weight_val - 1) > 0.001) {
			System.out.println("Objective weights didn't add to 1! Exiting...");
			System.out.println("\tEconomic weight:			" + String.valueOf(ec_weight_val));
			System.out.println("\tEnvironmental weight: " + String.valueOf(env_weight_val));
			System.out.println("\tEnergy weight:				" + String.valueOf(en_weight_val));
			System.exit(1);
		} else {
			this.ec_weight_val = ec_weight_val;
			this.env_weight_val = env_weight_val;
			this.en_weight_val = en_weight_val;
		}

		String uuid = UUID.randomUUID().toString();
		if(System.getenv("USER").equals("deploy")){
			workingDirectory = "/home/deploy/gams_tmp";
		} else if(System.getenv("USER").equals("proto")){
			workingDirectory = "/home/proto/gams_tmp";
		} else {
			workingDirectory = "/vagrant/gams_tmp";
		}

		File dir = new File(workingDirectory);
		System.out.println("Exists: " + dir.exists());
		// If gams_tmp folder gets too large, clean out old folders.
		farm.getGame().cleanDirectory(dir, 1000); // Max size is ## MB!
		
		workingDirectory += "/gams_tmp" + uuid;

		System.out.println("User variable reported as: " + System.getenv("USER"));
        System.out.println("working directory is: " + workingDirectory);
		createWorkingDirectory(workingDirectory);
		systemDirectory = System.getenv("GAMSDIR");
	}

	public void updateWeights(double ec, double env, double en){
		this.ec_weight_val = ec;
		this.env_weight_val = env;
		this.en_weight_val = en;
	}
	
	public double[] getWeights() {
		double[] weights = {this.ec_weight_val, this.env_weight_val, this.en_weight_val};
		return weights;
	}

	public void setManagement(Boolean management){
		this.management = management;
	}

	public void setPrices(double corn, double grass, double alfalfa){
		this.cornPrice = corn;
		this.grassPrice = grass;
		this.alfalfaPrice = alfalfa;
	}


	public String gamsRun() {
		// create a workspace
		GAMSWorkspaceInfo	wsInfo = new GAMSWorkspaceInfo();
		// NOTE: doesn't like "~"
		wsInfo.setWorkingDirectory(workingDirectory);

		/* Set DebugLevel to control output.
		 	• OFF =("off")
			  = 0 with string representation "off", No Debug
			• KEEP_FILES =("keepﬁles")
			  = 1 with string representation "keepﬁles", Keep temporary ﬁles
			• SHOW_LOG =("showlog")
			  = 2 with string representation "showlog", Send GAMS log to standard output and keep temporary ﬁles
			• VERBOSE =("verbose")
			  = 3 with string representation "verbose", Send highly technical info and GAMS log to standard output and keep temporary ﬁle
			• UNDEFINED_LEVEL =("undeﬁned")
			  >= 4 with string representation "undeﬁned", Undeﬁned variable type  */

		//wsInfo.setDebugLevel(GAMSGlobals.DebugLevel.SHOW_LOG);
		wsInfo.setDebugLevel(GAMSGlobals.DebugLevel.OFF);


		//wsInfo.setSystemDirectory(systemDirectory);
		//wsInfo.setSystemDirectory("/usr/gams/gams24.2_linux_x86_32_sfx");
		ws = new GAMSWorkspace(wsInfo);

		// create database object
		dbIn	= ws.addDatabase("dbIn");


		// Add database records to relate to the set levels and add values
		// start with single dimension values
		// declare parameters in database
		field = dbIn.addSet("field", "fields");
		
		job_capital = dbIn.addParameter("capital_previous", 0, "");
		job_capital.addRecord().setValue(farm.getCapital());
		
		job_grainRatio = dbIn.addParameter("grainDistro", 0, "");
		job_grainRatio.addRecord().setValue(farm.getGame().getEconomy().getGrainRatio());
		job_stoverRatio = dbIn.addParameter("stoverDistro", 0, "");
		job_stoverRatio.addRecord().setValue(farm.getGame().getEconomy().getStoverRatio());
		
		job_weights = dbIn.addParameter("Weights", 1, "");
		// Set Weights
		job_weights.addRecord("Ec").setValue(this.ec_weight_val);
		job_weights.addRecord("En").setValue(this.en_weight_val);
		job_weights.addRecord("Env").setValue(this.env_weight_val);

		job_ec_score = dbIn.addParameter("ecScore_previous", 0, "");
		job_ec_score.addRecord().setValue((farm.getEconScore() != 0) ? farm.getEconScore() : 0.1);
		
		z2_up = dbIn.addParameter("z2_up", 0);
		z2_up.addRecord().setValue(this.management ? 1 : 0);
		
		job_price = dbIn.addParameter("Price", 1, "");
		// Set price values
		job_price.addRecord("corn").setValue(this.cornPrice);
		job_price.addRecord("grass").setValue(this.grassPrice);
		job_price.addRecord("alfalfa").setValue(this.alfalfaPrice);
		
		job_z1_last = dbIn.addParameter("prevZ1", 2, "");
		job_prev_ON = dbIn.addParameter("prevOrgNit", 1, "");
		job_prev_Root = dbIn.addParameter("prevRootBio", 1, "");
		// Set field names, prevz1
		int i=0;
		String name;		
		for(Field fi:farm.getFields()){
			i++;
			name = "f"+i;
			field_names.put(fi,name);
			field.addRecord(name).setText(name);
	
			job_prev_ON.addRecord(name).setValue( fi.getOrganicN() );
			job_prev_Root.addRecord(name).setValue( fi.getRootBiomass() );
			
			// Set prevZ1
			for (Crop crop:Crop.values()){
				String crop_name = crop.name().toLowerCase();
				Vector<String> keys = new Vector<String>();
				keys.add(name);
				keys.add(crop_name);
				if (fi.getCrop().equals(crop)) {
					job_z1_last.addRecord(keys).setValue(1);
				}
				else {
					job_z1_last.addRecord(keys).setValue(0);
				}
			}
		}

		String message = "";
		// Display contents of database being sent to GAMs solver:
		///////////////////////////////////////////////////////////////////////
		message += "dbIn Contents (sent to GAMs): \n" + "\n";	
		Iterator<GAMSSymbol<?>> dbInIterator = dbIn.iterator();
		while(dbInIterator.hasNext()){
			Object curr = dbInIterator.next();
			if (curr instanceof GAMSSet){
				message += ((GAMSSet) curr).getName() + ":"+ "\n";
				Iterator<?> setIter = ((GAMSSet) curr).iterator();
				while(setIter.hasNext()){
					message += ((GAMSSetRecord) setIter.next()).getText()+ "\n";
				}
				message += "\n";
			}
			else if (curr instanceof GAMSParameter){
				Iterator<?> setIter = ((GAMSParameter) curr).iterator();
				while(setIter.hasNext()){
					message += ((GAMSParameter) curr).getName() + ":  " + ((GAMSParameterRecord) setIter.next()).getValue()+ "\n";
				}
			}
		}
		message += "\n";
		///////////////////////////////////////////////////////////////////////

		// define options
		opts = ws.addOptions();
		opts.defines("dbIn", "dbIn");

		//System.out.println("\n EconScore = " + farm.getEconScore() + "\n");

		// retrieve GAMSVariable "q" from GAMSJob's output databases
		// create GAMSJob "netlogo_job" from the "model"
		//GAMSJob gamsBotJob = ws.addJobFromFile("testBotModel.gms");
		GAMSJob gamsBotJob = ws.addJobFromFile("FoF_Bot.gms");
		gamsBotJob.run(opts, dbIn);
		resultsDB = gamsBotJob.OutDB();

		return message;
	}


	//Previously called "plant"
	public void run(){

		System.out.println(farm.getName() + "is now running");

		//Display current values, before bots.run() is called.
		int fieldNum = 0;
		String botMessage = "";
		botMessage +="\n\nGamsBot.plant() has been called for " + farm.getName() + ": \n";
		botMessage += "Current Values (Before run): " + "\n";
		botMessage += "Capital: " + farm.getCapital() + "\n";
		botMessage += "EnvScore: " + farm.getEnvScore() + "\n";
		botMessage += "EconScore: " + farm.getEconScore() + "\n";
		botMessage += "EnergyScore: " + farm.getEnergyScore() + "\n";
		botMessage += "Management Setting: " + this.management + "\n";
		for(Field f:farm.getFields()){
			fieldNum++;
			botMessage += farm.getName() + fieldNum + ":  ON:" + f.getOrganicN() + "\n";
			botMessage += farm.getName() + fieldNum + ":  RootBio:" + f.getRootBiomass() + "\n";
			botMessage += farm.getName() + fieldNum + ":  Crop:" + f.getCrop() + "\n";
			botMessage += farm.getName() + fieldNum + ":  Till:" + f.isTill() + "\n";
			botMessage += farm.getName() + fieldNum + ":  Fert:" + f.isFertilize() + "\n";
			botMessage += "\n";
		}
		//botMessage += "\n";

		// Solve the model each planting phase and carry over decisions to the management phase
		//this.run();
		//(new Thread(this)).start();
		//botMessage += this.gamsRun();
		this.gamsRun();

		//Display how long gams run took to solve:
		botMessage += "Time to solve: " + resultsDB.getParameter("solveTime").getFirstRecord().getValue();
		//Display which path was chosen (Perrenial or not)
		botMessage += "\nPerennial Path Score: " + resultsDB.getParameter("overall").getFirstRecord().getValue();
		botMessage += "\nNonPerennial Path Score: " + resultsDB.getParameter("overall2").getFirstRecord().getValue() + "\n\n";

		//Display GAMs decisions & Set crop values
		//botMessage += "\nGAMs output decisions: " +"\n";
		String name;
		for(Field f:farm.getFields()){
			name = field_names.get(f);
			//botMessage += "z1(" + name + ",grass) = " + resultsDB.getVariable("z1").findRecord(name, "grass").getLevel()+ "\n";
			//botMessage += "z1(" + name + ",corn) = " + resultsDB.getVariable("z1").findRecord(name, "corn").getLevel() + "\n";
			//botMessage += "z1(" + name + ",alfalfa) = " + resultsDB.getVariable("z1").findRecord(name, "alfalfa").getLevel() + "\n";
			if(resultsDB.getVariable("z1").findRecord(name, "grass").getLevel() == 1) {
				//botMessage += name + ": Grass" + "\n";
				f.setCrop(Crop.GRASS);
			} else if(resultsDB.getVariable("z1").findRecord(name, "corn").getLevel() == 1) {
				//botMessage += name + ": Corn" + "\n";
				f.setCrop(Crop.CORN);
			} else if(resultsDB.getVariable("z1").findRecord(name, "alfalfa").getLevel() == 1) {
				//botMessage += name + ": alfalfa" + "\n";
				f.setCrop(Crop.ALFALFA);
			}
			//botMessage += "\n";
		}

		//Set farm status to 'Ready' if bot is still in the game (not removed)
		botMessage += "Farm ready status: " + farm.isReady() + "\n";
		
		if(farm.getGame().getBots().contains(this)) {
			botMessage += farm.getName() + " is still in the game and is now ready.\n";
			makeReady();
		} else {
			botMessage += farm.getName() + " finished running, but has been removed"
					+ " from the game and will NOT run ready method!\n";
		}
		botMessage += "Farm ready status: " + farm.isReady() + "\n\n";
		//botMessage += "Farm status is now set to ready. \n\n";

		//Display field values after assignments were made (verification)
		botMessage += "Field Values (After run & assignment): " + "\n";
		fieldNum = 0;
		for(Field f:farm.getFields()){
			fieldNum++;
			botMessage += farm.getName() + fieldNum + ":  Crop:" + f.getCrop() + "\n";
			botMessage += farm.getName() + fieldNum + ":  Till:" + f.isTill() + "\n";
			botMessage += farm.getName() + fieldNum + ":  Fert:" + f.isFertilize() + "\n";
			botMessage += "\n";
		}
		System.out.print(botMessage);
		
		//Removes the directory files too soon(not available for next run)
		//finalize();  

		System.out.println(farm.getName() + "has finished running");
	}
	
	//Sets farm status to ready.
	public void makeReady(){
		farm.setReady(true);
		farm.getGame().farmerReady(farm.getName());
		if(farm.getGame().allReady()){
			farm.getGame().callHelpersAdvanceStage();
		}
	}

	public void manage(){

		//Display current values before bots.manage() is run
		int fieldNum = 0;
		System.out.println("\n\nGamsBot.manage() has been called: \n");
		System.out.println("Current Values (Before manageing): ");
		for(Field f:farm.getFields()){
			fieldNum++;
			System.out.println(farm.getName() + fieldNum + ":  Crop:" + f.getCrop());
			System.out.println(farm.getName() + fieldNum + ":  Till:" + f.isTill());
			System.out.println(farm.getName() + fieldNum + ":  Fert:" + f.isFertilize());
			System.out.println();
		}


		//Display GAMs decisions & Set management decisions.
		System.out.println("GAMs output decisions: \n");
		String name;
		for(Field f:farm.getFields()){
			name = field_names.get(f);
			System.out.println("z2(" + name + ",till) = " + resultsDB.getVariable("z2").findRecord(name, "till").getLevel());
			System.out.println("z2(" + name + ",fert) = " + resultsDB.getVariable("z2").findRecord(name, "fert").getLevel());
			if(resultsDB.getVariable("z2").findRecord(name, "till").getLevel() == 0) {
				System.out.println(name + ": Don't till");
				f.setTill(false);
			} else if(resultsDB.getVariable("z2").findRecord(name, "till").getLevel() == 1) {
				System.out.println(name + ": till");
				f.setTill(true);
			} if(resultsDB.getVariable("z2").findRecord(name, "fert").getLevel() == 0) {
				System.out.println(name + ": Don't fertilize");
				f.setFertilize(false);
			} else if(resultsDB.getVariable("z2").findRecord(name, "fert").getLevel() == 1) {
				System.out.println(name + ": fertilize");
				f.setFertilize(true);
			}
			System.out.println();
		}

		//Set farm status to 'Ready'
		System.out.println("Farm ready status: " + farm.isReady() + "\n");
		makeReady();
		System.out.println("Farm ready status: " + farm.isReady() + "\n");

		//Display field values after assignments were made (verification).
		System.out.println("\nField Values (After management assignment): ");
		fieldNum = 0;
		for(Field f:farm.getFields()){
			fieldNum++;
			System.out.println(farm.getName() + fieldNum + ":  Crop:" + f.getCrop());
			System.out.println(farm.getName() + fieldNum + ":  Till:" + f.isTill());
			System.out.println(farm.getName() + fieldNum + ":  Fert:" + f.isFertilize());
			System.out.println();
		}
		System.out.println();
	}


	public static void createWorkingDirectory(String directory) {
		String working_dir = System.getProperty("user.dir");
		System.out.println("First working dir: " + working_dir);
		File dir = new File(directory);
		dir.mkdir();	
		//File inFile = new File(working_dir + "/javaGame/kosomodel/src/main/java/com/biofuels/fof/kosomodel/testBotModel.gms");
		//File outFile = new File(directory + "/testBotModel.gms");
		File inFile = new File(working_dir + "/javaGame/kosomodel/src/main/java/com/biofuels/fof/kosomodel/FoF_Bot.gms");
		File outFile = new File(directory + "/FoF_Bot.gms");
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



	static void cleanup(String directory)	{
		File directoryToDelete = new File(directory);
		String files[] = directoryToDelete.list();
		for (String file : files) {
			File fileToDelete = new File(directoryToDelete, file);
			try {
				fileToDelete.delete();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		try {
			directoryToDelete.delete();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	protected void finalize() {
		cleanup(workingDirectory);
	}
}

