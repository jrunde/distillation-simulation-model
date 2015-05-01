package com.biofuels.fof.kosomodel;

import java.util.LinkedList;
import java.util.ArrayList;

public class Farm {
    
    private String name;
    private String clientID;
    //  private Field[] fields;
    private LinkedList<Field> fields;
    private int capital=10000;
    private int costs=0;
    private double envScore;
    private double energyScore;
    private double econScore;
    private double envScoreRunning;
    private double energyScoreRunning;
    private double econScoreRunning;
    private int envRank;
    private int energyRank;
    private int econRank;
    private double overallScore;
    private double overallScoreRunning;
    private int overallRank;
    private int id;
    
    private double soilSubscore;
    private double waterSubscore;
    public double gbiSubscore;
    public double bciSubscore;
    public double emissionsSubscore;
    public double farmProductionEmissions; 
    public double farmSoilRespiration;
	private double emissions;
	public double netEnergy;
    public double cornEnergy;
    public double grassEnergy;
    public double alfalfaEnergy;
    public double farmCosts;
    public double farmRevenue; 
    
    private String currentUser;
    private boolean acceptCornContract;
    private boolean acceptSwitchgrassContract;
    private boolean ready;
    private boolean alwaysReady;
    private double phosphorous;
    private Game game;
    private FarmHistory history;
    
    
    public Farm(String name, int capital, int id, Game g, boolean alwaysReady) {
        this.name = name;
        this.capital = capital;
        this.game = g;
        this.id = id;
        history = new FarmHistory();
        fields = new LinkedList<Field>();
        this.alwaysReady = alwaysReady;
    }
    
    public LinkedList<Field> getFields(){
        return fields;
    }
    
    public int getID(){
        return this.id;
    }
    
    public Game getGame(){
    	return this.game;
    }
    
    public void recomputeScores(){
    	
        this.econScore = calcEconScore();
        this.envScore = calcEnvScore();
        this.energyScore = calcEnergyScore();
        
        int sum = this.game.getEconomyWeight() + this.game.getEnvironmentWeight() + 
            this.game.getEnergyWeight();
        double score = getEconScore() * this.game.getEconomyWeight() / sum;
        score += getEnvScore() * this.game.getEnvironmentWeight() / sum;
        score += getEnergyScore() * this.game.getEnergyWeight() / sum;
        
        setOverallScore(score);
    }
    
    public void recomputeRunningScores(){
        LinkedList<FarmHistory.HistoryYear> hist = history.getHistory();
        double total = hist.size();
        if(total == 0){
            setEconScoreRunning(econScore);
            setEnvScoreRunning(envScore);
            setEnergyScoreRunning(energyScore);
            setOverallScoreRunning(overallScore);
        }
        else{
            double envCalc=0, econCalc=0, enCalc=0, overallCalc=0;

            // enhanced loop including year 0 in running averages
            //for(FarmHistory.HistoryYear y:hist){
            //for(int x = 1; x < (total+1); x++){   /* THIS CAUSED INDEX OUT OF BOUNDS */
            for(int x = 0; x < total; x++){
            	FarmHistory.HistoryYear y = hist.get(x);
                envCalc += y.environmentScore;
                econCalc += y.economicsScore;
                enCalc += y.energyScore;
                overallCalc += y.sustainabilityScore;
            }
            setEnvScoreRunning((envCalc + envScore) / (total));
            setEconScoreRunning(econScore);
            setEnergyScoreRunning((enCalc + energyScore)/ (total));
            setOverallScoreRunning((overallCalc + overallScore) / (total));
        }
    }
    
    public double getEnvScore() {
        return envScore;
    }
    
/*    public double calcEmissionsSubscore(double n2o, double soilRespiration){
        double normN2o = ((n2o - 0.07905591) / 0.9518111);
        double normSoil = 0.5;
        
        if(getHistory().size() > 0){
            normSoil = (soilRespiration - getHistory().get(getHistory().size()-1).soilRespiration) / 19;
        }
        
        return (normSoil + normN2o) / 2;
    }*/
    
    public double calcEnvScore() {
        
        double avgON = 0;
        double avgGBI = 0;
        double avgBCI = 0;
        // double maxON = 2850;
        double totalNleached = 0;
        double waterSumScore = 0;
        double soilSumScore = 0;
        double emissionsSumScore = 0;
        // double maxNleached = 272/2.47105; //kg/acre, assuming max available N and left fallow (no uptake, all leached) ALSO LOCATED IN GAME.JAVA!!!
        int ESdenominator = 2;
        int GBI_bin = 0;
        int BCI_bin = 0;
        
        for(Field f:fields){
            avgGBI += f.getGBI();
            avgBCI += f.getBCI();
            waterSumScore += f.getFieldWaterScore();
            soilSumScore += f.getFieldSoilScore();
            emissionsSumScore += f.getFieldEmissionsScore();
        }
        

        this.soilSubscore = soilSumScore / fields.size();
        
        this.waterSubscore = waterSumScore / fields.size();
        
        this.emissionsSubscore = emissionsSumScore / fields.size();
    	
        double waterValue = this.waterSubscore;
        
        if(this.game.getGameSettings().get("globalWater"))
            waterValue = this.game.getGlobalWater();
        
        if(this.game.getGameSettings().get("bci"))
            this.bciSubscore = avgBCI / fields.size();
        ESdenominator += 1;
        BCI_bin = 1;
        
        if(this.game.getGameSettings().get("gbi"))
            this.gbiSubscore = avgGBI / fields.size();
        ESdenominator += 1;
        GBI_bin = 1;
        
        System.out.println("---EMISSIONS---");
    	System.out.println("  Emissions level = " + this.emissions);
    	System.out.println("  Emissions subscore = " + emissionsSubscore);

        
        // flexible approach to include possibility of turning GBI and BCI on/off
        //return (soilSubscore + waterValue + (bciSubscore * BCI_bin) + (gbiSubscore * GBI_bin)) / ESdenominator;
        return (soilSubscore + waterValue + bciSubscore + this.emissionsSubscore) / 4;
    }
    
    public int getCapital() {
        return capital;
    }
    
    public double getEnergyScore() {
        return energyScore;
    }
    
    public double calcEnergyScore() {
        // recalculated (and rounded up for safety)
        // double EMAX = 232000 * 40;
    	// double EMAX = 1337.678; // grass, SOC @ 190, low N
        // double EMIN = -315.213; // all corn, no yield, max inputs (or, cc with all mgmt)
        //double EMAX = 475000 ;   // MJ per field CONFIRMED
        //double EMIN = -100000; // MJ per field CONFIRMED
    	double EMAX = 2020000 * 2.47105;  /* Maximum net energy per field assuming 25 year max ON & root */
    	double EMIN = -225000 * 2.47105;  /* Minimum net energy per field */
    	double result = ((this.netEnergy / fields.size()) - EMIN) / (EMAX - EMIN);
        return result;
    }
    
    public double getEconScore() {
        return econScore;
    }
    
    public double calcEconScore() {
        if(this.capital > 0)
            return (double)this.capital / (double)game.getLargestEarnings();
        else
            return 0;
    }
    
    public String getName() {
        return name;
    }
    
    public String getClientID() {
        return clientID;
    }
    
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }
    
    public boolean isAcceptCornContract() {
        return acceptCornContract;
    }
    
    public void setAcceptCornContract(boolean acceptCornContract) {
        this.acceptCornContract = acceptCornContract;
    }
    
    public boolean isAcceptSwitchgrassContract() {
        return acceptSwitchgrassContract;
    }
    
    public void setAcceptSwitchgrassContract(boolean acceptSwitchgrassContract) {
        this.acceptSwitchgrassContract = acceptSwitchgrassContract;
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }
    
    public void setField(Integer fieldNum, String crop) {
        switch (crop){
            case "grass":
                fields.get(fieldNum).setCrop(Crop.GRASS);
                break;
            case "corn":
                fields.get(fieldNum).setCrop(Crop.CORN);
                break;
            case "alfalfa":
                fields.get(fieldNum).setCrop(Crop.ALFALFA);
                break;
            case "none":
                fields.get(fieldNum).setCrop(Crop.FALLOW);
                break;
        }
    }
    
    public void changeFieldManagement(int fieldnum, String technique, boolean value) {
        Field field = fields.get(fieldnum);
        switch (technique){
            case "fertilizer":
                field.setFertilize(value);
                break;
            case "pesticide":
                field.setPesticide(value);
                break;
            case "tillage":
                field.setTill(value);
                break;
        }
    }
    
    public void setCapital(int i) {
        capital = i;
    }
    
    public void setCosts(int i) {
        costs = i;
    }
    
    public int getCosts(){
        return costs;
    }
    
    public boolean isReady() {
        return (ready || alwaysReady);
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public double getPhosphorous() {
        return phosphorous;
    }
    
    public void updatePhosphorous() {
        double cornCount =0;
        for(Field f:this.fields){
            if(f.getCrop() == Crop.CORN){
                cornCount ++;
            }
        }
        double cornRatio = cornCount / this.fields.size();
        this.phosphorous = Math.pow(10, (.79 * cornRatio) - 1.44);
        
    }
    
    public void updateGBI() {
        for(Field f:this.fields){
            ArrayList<Field> neig = game.neighbors(f.x,f.y);
            int ncorn = 0;
            int ngrass = 0;
            for(Field other:neig){
                if(other.getCrop() == Crop.CORN)
                    ncorn ++;
                if(other.getCrop() == Crop.GRASS)
                    ngrass ++;
            }
            double pcorn = ((double) ncorn) / ((double) neig.size());
            double pgrass = ((double) ngrass) / ((double) neig.size());
            
            double gbilogit = -4.47 + 2.95 * pcorn + 5.17 * pgrass;
            double gbi_scaled = (1 / ((1 / Math.exp(gbilogit)) + 1)) / 0.67;
            
            f.setGBI(gbi_scaled);
        }
    }
    
    public void updateBCI() {
        for(Field f:this.fields){
            ArrayList<Field> neig = game.neighbors(f.x,f.y);
            double localBCI = 0; 
            int ngrass = 0;
            if(f.getCrop().equals(Crop.GRASS)) {
                localBCI = 1;
            } 
            
            if(f.getCrop().equals(Crop.ALFALFA)){
            	localBCI = 0.5;
            }
            
            for(Field other:neig){
                if(other.getCrop() == Crop.GRASS)
                    ngrass ++;
            }
            
            double pgrass = ((double) ngrass) / ((double) neig.size());
            
            double bci_scaled = (0.25 + 0.19 * localBCI + 0.62 * pgrass) / 1.06;
            
            f.setBCI(bci_scaled);
        }
    }
    
    public int getEnvRank() {
        return envRank;
    }
    
    public void setEnvRank(int envRank) {
        this.envRank = envRank;
    }
    
    public int getEnergyRank() {
        return energyRank;
    }
    
    public void setEnergyRank(int energyRank) {
        this.energyRank = energyRank;
    }
    
    public int getEconRank() {
        return econRank;
    }
    
    public void setEconRank(int econRank) {
        this.econRank = econRank;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(double d) {
        this.overallScore = d;
    }
    
    public int getOverallRank() {
        return overallRank;
    }
    
    public void setOverallRank(int overallRank) {
        this.overallRank = overallRank;
    }
    
    public LinkedList<FarmHistory.HistoryYear> getHistory() {
        return history.getHistory();
    }
    
    public double calcN2o(){
        double n2o = 0.0;
        
        for(Field f:fields){
            n2o += f.getN2o();
        }
        
        return n2o;
    }
    
    public double getRespiration(){
        double resp = 0.0;
        
        for(Field f:fields){
            resp += f.calcRespiration();
        }
        
        return resp;
    }
    
    private double calcTotalGrassYield() {
        double yield = 0;
        for(Field f:fields){
            if(f.getCrop() == Crop.GRASS){
                yield += f.getLastYield();
            }
        }
        return yield;
    }
    
    private double calcTotalCornYield() {
        double yield = 0;
        for(Field f:fields){
            if(f.getCrop() == Crop.CORN){
                yield += f.getLastYield(); //kg/
            }
        }
        return yield;
    }
    
    private double calcTotalAlfalfaYield() {
        double yield = 0;
        for(Field f:fields){
            if(f.getCrop() == Crop.ALFALFA){
                yield += f.getLastYield();
            }
        }
        return yield;
    }
    
    private double calcAvgGrassYield() {
        double yield = 0;
        double count = 0;
        for(Field f:fields){
            if(f.getCrop() == Crop.GRASS){
                yield += f.getLastYield();
                count ++;
            }
        }
        return yield/count;
    }
    
    private double calcAvgCornYield() {
        double yield = 0;
        double count = 0;
        for(Field f:fields){
            if(f.getCrop() == Crop.CORN){
                yield += f.getLastYield();
                count ++;
            }
        }
        return yield/count;
    }
    
    public double getEnvScoreRunning() {
        return envScoreRunning;
    }
    
    private void setEnvScoreRunning(double envScoreRunning) {
        this.envScoreRunning = envScoreRunning;
    }
    
    public double getEnergyScoreRunning() {
        return energyScoreRunning;
    }
    
    private void setEnergyScoreRunning(double energyScoreRunning) {
        this.energyScoreRunning = energyScoreRunning;
    }
    
    public double getEconScoreRunning() {
        return econScoreRunning;
    }
    
    public void setEconScoreRunning(double econScoreRunning) {
        this.econScoreRunning = econScoreRunning;
    }
    
    public double getOverallScoreRunning() {
        return overallScoreRunning;
    }
    
    public void setOverallScoreRunning(double overallScoreRunning) {
        this.overallScoreRunning = overallScoreRunning;
    }
    
    public double getNetEnergy() {
        return this.netEnergy;
    }
    
    public void updateEmissions(){
    	// this.farmSoilRespiration = getRespiration();
    	this.emissions = this.farmProductionEmissions;
    }
    
    public double getEmissions(){
        return this.emissions;   
    }
    public void addHistoryYear() {
    	
    	// WHAT ARE THESE USED FOR?
        double n2o = calcN2o();        
        double soilRespiration = getRespiration();
        double kg_ha_to_tons_ac = 0.00110231 / 2.47105;
        double field_size = 40;
        
        double cornYield = this.calcTotalCornYield()*kg_ha_to_tons_ac*field_size;
        double grassYield = calcTotalGrassYield()*kg_ha_to_tons_ac*field_size;;
        double alfalfaYield = calcTotalAlfalfaYield()*kg_ha_to_tons_ac*field_size;;
        
        double cornIncome = 0;
        double grassIncome = 0;
        double alfalfaIncome = 0;
        Economy economy = game.getEconomy();
        for (Field f:fields) {
        	cornIncome += f.getCornRevenue(economy);
        	grassIncome += f.getGrassRevenue(economy);
        	alfalfaIncome += f.getAlfalfaRevenue(economy);
        }
        
        history.addYear(capital, costs, soilSubscore, waterSubscore, game.getGlobalWater(), game.getGlobalEmissions(), game.getAvgWater(), game.getAvgEmissions(), bciSubscore, gbiSubscore, emissionsSubscore, getEmissions(), n2o, soilRespiration, this.getOverallScore(), getEconScore(), getEnvScore(), getNetEnergy(), cornEnergy, grassEnergy, alfalfaEnergy,
                        getEnergyScore(), getEconScoreRunning(), getEnvScoreRunning(), getEnergyScoreRunning(), getOverallRank(), getEconRank(), getEnvRank(), getEnergyRank(), 
                        cornIncome, grassIncome, alfalfaIncome, cornYield, grassYield, alfalfaYield);
    }
    
    
    
    public String toString() {
        
        return "\nName: " + this.name + "\n\tCapitol: " + this.capital + 
        "\n\tEnvironmental Score: " + this.envScore + "\n\tEnergy Score: " + this.energyScore + 
        "\n\tSustainability Score: " + this.overallScore + "\n\tEconomic Score: " + this.econScoreRunning + 
        "\n\tCorn Energy: " + this.cornEnergy + "\n\tGrass Energy: " + this.grassEnergy + 
        "\n\tAlfalfa Energy: " + this.alfalfaEnergy + "\n\tNet Energy: " + this.netEnergy + 
        "\n\tSOM: " + this.soilSubscore + "\n\tWater Quality: " + this.waterSubscore +  
        "\n\tGBI: " + this.gbiSubscore + "\n\tBCI: " + this.bciSubscore + 
        "\n\tEmissions: " + this.emissionsSubscore + "\n";
    }
}
