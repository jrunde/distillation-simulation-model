package com.biofuels.fof.kosomodel;
import java.util.Random;


public class Field {

	private Crop crop;
	private ManagementOptions management;
	public double costs;
	private double GBI; //Grassland Bird Index
	private double BCI; // Bio-control index (natural pest supression)
	public double respiration; //Grassland Bird Index
    public double n2o; //Grassland Bird Index
	public double fieldProductionEmissions; //total emissions from ag and refinery phases
	private double fieldAgEmissions;
	private double fieldRefineryEmissions;
	private double productionEnergyNet;
	private double refineryEnergyIn, refineryEnergyOut, refineryEnergyNet;
	private double netTotalEnergy;
	public double fieldRevenue;
	private double fieldWaterScore, fieldSoilScore, fieldEmissionsScore, fieldEnvScore;
	private double inorganic_nitrogen;
	private double organic_nitrogen;
	private double mineralized_nitrogen;
	private double nitrogen_to_crop;
	private double nitrogen_leached;
	private double rootNitrogenStorage;
	private double NUE;
	private double percRoot, percVeg, percGrain;
	private double totalYield;
	private double abovegroundYield;
	private double grainBiomass, vegBiomass, rootBiomass;
	private double stoverDistro, grainDistro; //Distribution percent of corn stover/grain to feed.
	private double proportion_veg_retained;
	private double initial_NUE, NUE_decay_rate, N_decay_point;
	public int x;
	public int y;
	private FieldHistory history;
	private double lastYield;

    public double maxNleached = 272/2.47105; // kg/acre, assuming max available N and left fallow (no uptake, all leached)
    public double maxON = 2850; // kg/acre
    
    public Field(int x, int y, double ON) {
		setCrop(Crop.FALLOW);
		management = new ManagementOptions();
		history = new FieldHistory();
		this.organic_nitrogen=ON;
		this.y=y;
		this.x=x;
		this.BCI=0;
		this.GBI=0;
	}
	
	public void setStoverDistro(double ratio) {
		this.stoverDistro = ratio;
	}
	
	public void setGrainDistro(double ratio) {
		this.grainDistro = ratio;
	}

	private class ManagementOptions{

		public ManagementOptions(boolean ... opts){
			if(opts.length > 0)
				till = opts[0];
			if(opts.length > 1)
				pesticide = opts[1];
			if(opts.length > 2)
				fertilize = opts[2];
		}

		private boolean till=true;
		private boolean pesticide=false;
		private boolean fertilize=true; //TODO: setting this to true will make fert high by default???
	}

	public Crop getCrop() {
		return crop;
	}

	public double getNitrogenLeached(){
	    return this.nitrogen_leached;	
	}
	
	public void setCrop(Crop crop) {
		this.crop = crop;
	}

	public double getGBI() {
		return GBI;
	}
	
	public double getBCI() {
		return BCI;
	}

	public double getRespiration() {
		return respiration;
	}

	public double getWater() {
        double nitrogenLeached = 1 - (this.nitrogen_leached / this.maxNleached);
        return nitrogenLeached;
		//if(this.getCrop() == Crop.CORN){
		//	return 0;
		//} else return 1;
	}
	
	public void setGBI(double GBI) {
		this.GBI = GBI;
	}
	
	public void setBCI(double BCI) {
		this.BCI = BCI;
	}

	public int[] getLoc(){
		return new int[]{this.x, this.y};
	}

	public boolean isTill() {
		if ((this.crop.equals(Crop.GRASS) && getCurrentCrop().equals(Crop.GRASS)) || (this.crop.equals(Crop.ALFALFA) && getCurrentCrop().equals(Crop.ALFALFA) || this.crop.equals(Crop.FALLOW))){
			return false;
		} else {
			return management.till;
		}
	}
	
	public void setTill(boolean till) {
		management.till = till;
	}

	public boolean isPesticide() {
		return management.pesticide;
	}

	public void setPesticide(boolean pesticide) {
		management.pesticide = pesticide;
	}

	public boolean isFertilize() {
		if (this.crop.equals(Crop.FALLOW)){
			return false;
		} else {
			return management.fertilize;
		}
	}
	
	public void setFertilize(boolean fertilize) {
			management.fertilize = fertilize;
	}
	
	public ManagementOptions getManagement() {
		return management;
	}

	public void setManagement(ManagementOptions management) {
		this.management = management;
	}


	public void updateN2o(){
		int c0 = 0;
		int grass = 0;
		int alfalfa = 0;
		if(this.crop == Crop.CORN){
			if(this.management.fertilize)
				c0 = 200;
			else
				c0 = 100;
		}
		else if(this.crop == Crop.GRASS){
			grass = 1;

			if(this.management.fertilize)
				c0 = 0;
			else
				c0 = 50;
		}
		else if(this.crop == Crop.ALFALFA){
			alfalfa = 1;
		}

		this.n2o = Math.exp(- 0.4136 + 0.0065 * c0 - 1.268 * grass - 0.023 * alfalfa - 0.472 - 0.14 - 0.42 - 0.109 + 0.285) * 298;
	}

	public double getN2o() {
		return n2o;
	}

	public double getOrganicN() {
		return this.organic_nitrogen;
	}
	
	public double getOrganicNforScore() {
		return this.organic_nitrogen + this.rootNitrogenStorage;
	}

	public void setOrganicN(float organicN) {
		this.organic_nitrogen = organicN;
	}
	
	public double getRootBiomass() {
		return this.rootBiomass;
	}
    
	public FieldHistory getHistory() {
		return history;
	}

	public void addHistoryYear() {
		System.out.println("Field history year added...");
        history.addYear(this.organic_nitrogen, BCI, GBI, n2o, respiration, fieldProductionEmissions, fieldRefineryEmissions, fieldProductionEmissions, costs, crop, lastYield, isFertilize(), isPesticide(), isTill(), this.productionEnergyNet, this.refineryEnergyIn, this.refineryEnergyOut, this.refineryEnergyNet, this.netTotalEnergy, this.getWater(), this.fieldRevenue, this.fieldSoilScore, this.fieldWaterScore, this.fieldEmissionsScore, this.fieldEnvScore);
	}

	public double getLastYield() {
		return lastYield;
	}

	public void setLastYield(double lastYield) {
		this.lastYield = lastYield;
	}

	public Crop getLastCrop(){
		if(history.getHistory().size() > 1){
			return history.getHistory().get(history.getHistory().size()-2).crop;
		}
		else{
			return Crop.FALLOW;
		}
	}

  public Crop getCurrentCrop(){
    if(history.getHistory().size() > 0){
      return history.getHistory().get(history.getHistory().size()-1).crop;
    }
    else{
      return Crop.FALLOW;
    }
  }
	
	
  
  
    // FERTILITY

	public double calculateInorganicNitrogen(){
		double fertWeight;
		double available_N;
		double volitilization_rate = 0.3;
		double mineralization_rate = 0.01;
		
		if(this.isTill() == true)
			mineralization_rate = 0.03;
		if(this.crop.equals(Crop.CORN)){
			if(this.management.fertilize == true){
				fertWeight = 200; // / 2.47105;	
			} else {
				fertWeight = 100; // / 2.47105;			
			}} 
		else if(this.crop.equals(Crop.GRASS)){
			if(this.management.fertilize == true){
				fertWeight = 60; // / 2.47105;	
			} else {
				fertWeight = 30; // / 2.47105;					
			}} 
		else {
			if(this.management.fertilize == true)
				fertWeight =  30; // / 2.47105;		
			else
				fertWeight = 0;
		} 
 
		this.mineralized_nitrogen = (this.organic_nitrogen * mineralization_rate);
		available_N = fertWeight * (1 - volitilization_rate) + (this.mineralized_nitrogen); // / 2.47105);	
		return available_N; // returned in the form of kg/ha
	}


	
	

  // YIELD
  
    
    public double calculateYield() {
    	double corn_grain_nitrogen = 0.0076 / 2.47105;  //Amt of nitrogen in corn-grain
    	double corn_veg_nitrogen = 0.0036 / 2.47105;    //Amt of nitrogen in corn-veg						
    	double corn_root_nitrogen = 0.0030 / 2.47105;   //Amt of nitrogen in corn-root  (unknown)	
    	double grass_grain_nitrogen = 0;
    	double grass_veg_nitrogen = 0.0005 / 2.47105;								
    	double grass_root_nitrogen = 0.0085 / 2.47105;									
    	double alfalfa_grain_nitrogen = 0;
    	double alfalfa_veg_nitrogen = 0.0124 / 2.47105;    //(unknown)
    	double alfalfa_root_nitrogen = 0.0144 / 2.47105;   //(unknown)
    	
    	double corn_grain_to_veg = 0.5;  //Ratio of biomass in grain to veg
    	double grass_grain_to_veg = 0;
    	double alfalfa_grain_to_veg = 0;
    	
    	double corn_root_to_shoot = 0.18;  		//Shoot = grain + veg [Above ground components]
    	double grass_root_to_shoot_est = 75; 
    	double grass_root_to_shoot_cont = 3; // root:shoot ratios for switchgrass in Frank et al. (2004) range from 0.27 - 0.36 (over three years)
    	double alfalfa_root_to_shoot = 0.87;
    	
    	double rootN, grainN, vegN;
    	//double proportion_veg_retained;         // proportion of veg retained that is returned to the SOM pool
    	double proportion_roots_retained;       // proportion of the roots that live to the following year
    	//double previousRootBiomass = 0;         
    	//double abovegroundYield;
    	abovegroundYield = 0;
    	
    	this.NUE_decay_rate = 0.03;
    	
    	
        switch (this.crop){
	        case CORN:
	        	if(getCurrentCrop() == Crop.GRASS){
	        		this.organic_nitrogen += this.rootBiomass * grass_root_nitrogen;  
	        	}
	    		rootN = corn_root_nitrogen;
	    		grainN = corn_grain_nitrogen;
	    		vegN = corn_veg_nitrogen;
	    		this.proportion_veg_retained = 0.5;
	    		proportion_roots_retained = 0;
	        	calcTissueDistribution(corn_root_to_shoot, corn_grain_to_veg, proportion_roots_retained);
	        	//this.NUE =  0.33 *(corn_grain_nitrogen + corn_veg_nitrogen + corn_root_nitrogen)/corn_grain_nitrogen;
	        	this.initial_NUE = 0.8295833;
	        	this.N_decay_point = 200;
	    		break;
	    		
	        case GRASS:
	    		proportion_roots_retained = 0.75;
	        	if(this.crop == Crop.GRASS && getCurrentCrop() != Crop.GRASS){
	        		calcTissueDistribution(grass_root_to_shoot_est, grass_grain_to_veg, proportion_roots_retained);
	        		this.proportion_veg_retained = 1;  // - now retains all aboveground biomass first year 
	        		this.initial_NUE = 0.5;
		        } else {
	        		calcTissueDistribution(grass_root_to_shoot_cont, grass_grain_to_veg, proportion_roots_retained);
	        		this.proportion_veg_retained = 0;  
	        		this.initial_NUE = 0.95;
	        		//previousRootBiomass = this.rootBiomass * proportion_roots_retained;
	        		}
	        	this.N_decay_point = 75;
	    		rootN = grass_root_nitrogen;
	    		grainN = grass_grain_nitrogen;
	    		vegN = grass_veg_nitrogen;

	    		break;
	    		
	        case ALFALFA:
	        	if(getCurrentCrop() == Crop.GRASS){
	        		this.organic_nitrogen += this.rootBiomass * grass_root_nitrogen; 
	        	}
        		rootN = alfalfa_root_nitrogen;
	    		grainN = alfalfa_grain_nitrogen;
	    		vegN = alfalfa_veg_nitrogen;	 
	    		this.proportion_veg_retained = 0;
	    		proportion_roots_retained = 0; //FIXME: JUST A GUESS
	        	calcTissueDistribution(alfalfa_root_to_shoot, alfalfa_grain_to_veg, proportion_roots_retained);
	        	this.initial_NUE = 0.8295833;
	        	this.N_decay_point = 200;
	    		break;
	    		
	    	default: 									// case for fallow
	        	if(getCurrentCrop() == Crop.GRASS){
	        		this.organic_nitrogen += this.rootBiomass * grass_root_nitrogen;  
	        	}
	    		calcTissueDistribution(0, 0, 0);			// does this return div by 0?
	    		rootN = 0;
	    		grainN = 0;
	    		vegN = 0;
	    		this.proportion_veg_retained = 0;
	    		proportion_roots_retained = 0;
	        	this.initial_NUE = 0.0;
	        	this.N_decay_point = 0;
	    		break;
    	}
    	


  	
    	double plantUsageRate = this.percRoot*rootN + this.percGrain*grainN + this.percVeg*vegN; 

    // Interactions with nitrogen
    	System.out.println("====================================");
    	System.out.println("--NITROGEN--");
    	System.out.println("  Till is " + this.isTill());
		System.out.println("  Crop = " + this.crop);
		System.out.println("  Organic Nitrogen (PRE) = " + this.organic_nitrogen);
		System.out.println("  Inrganic Nitrogen (PRE) = " + this.inorganic_nitrogen);
    	this.inorganic_nitrogen = calculateInorganicNitrogen();
    	this.NUE = calcNUE(this.initial_NUE, this.NUE_decay_rate, inorganic_nitrogen, this.N_decay_point);
    	this.nitrogen_to_crop = inorganic_nitrogen * this.NUE; 
		this.nitrogen_leached = inorganic_nitrogen * (1 - this.NUE);
	
	// Yield calculations
		
		if(this.crop == Crop.ALFALFA){
			this.totalYield = (6000 * 2.47105) + (50 * 2.47105 * this.nitrogen_to_crop * NUE);
		} else if(this.crop == Crop.GRASS || this.crop == Crop.CORN){
			this.totalYield = this.nitrogen_to_crop / plantUsageRate;
		} else {
			this.totalYield = 0;
		}
				
    	double residualNitrogen = (rootBiomass * rootN * (1 - proportion_roots_retained)) + (vegBiomass * vegN * proportion_veg_retained); // calculated in kg/ha
		this.organic_nitrogen = this.organic_nitrogen - this.mineralized_nitrogen + residualNitrogen; // organic nitrogen for next time step = nitrogen pool - nitrogen removed + nitrogen replenished via residue // calculated in kg/ha
		abovegroundYield = totalYield * (this.percVeg + this.percGrain); // calculated in kg/ha
		
		// BCI Adjustment
		double BCIYieldAdj = 1 - (0.2 * (1 - this.getBCI()));
		abovegroundYield *= BCIYieldAdj;
		
		this.grainBiomass = abovegroundYield * (this.percGrain/(this.percGrain + this.percVeg)); // calculated in kg/ha
		this.vegBiomass = abovegroundYield * ( this.percVeg/(this.percGrain + this.percVeg)); // calculated in kg/ha
		this.rootBiomass = this.totalYield * percRoot*(1-proportion_roots_retained) + this.rootBiomass*proportion_roots_retained; // calculated in kg/ha
		this.rootNitrogenStorage = this.rootBiomass * proportion_roots_retained * rootN;
		
		System.out.println("  Initial NUE = " + this.initial_NUE);
		System.out.println("  calculated NUE = " + this.NUE);
		System.out.println("  Organic Nitrogen (POST) = " + this.organic_nitrogen);
		System.out.println("  Mineralized Nitrogen = " + this.mineralized_nitrogen);
		System.out.println("  Inrganic Nitrogen (POST) = " + this.inorganic_nitrogen);
		System.out.println("  Leached Nitrogen = " + this.nitrogen_leached);
		System.out.println("--BIOMASS--");
		System.out.println("  Plant N usage rate = " + plantUsageRate);
		System.out.println("  Precent Roots = " + this.percRoot);
		System.out.println("  Precent Vegetative = " + this.percVeg);
		System.out.println("  Precent Grain = " + this.percGrain);
		System.out.println("  Total biomass = " + this.totalYield);
		System.out.println("  grain biomass = " + this.grainBiomass);
		System.out.println("  Vegetation Biomass = " + this.vegBiomass);
		System.out.println("  Root Biomass = " + this.rootBiomass);
		System.out.println("  Aboveground Yield = " + abovegroundYield);
		
		return abovegroundYield;  // returned in kg/ha
    }	

    public double getAbovegroundYield() {
    	return this.abovegroundYield;  // returned in kg/ha
    }
    
    
    
    void calcTissueDistribution(double root_to_shoot, double grain_to_veg, double p_root_retained){   
		this.percRoot = root_to_shoot*(1 - p_root_retained)/(1+(root_to_shoot*(1-p_root_retained)));	
		this.percGrain = (1 - this.percRoot) * grain_to_veg; 
		this.percVeg = 1 - this.percRoot - this.percGrain;
    }

    

    
    double calcNUE(double init_NUE, double NUE_rate, double available_N, double N_decay_point){
    	double NUE_modifier = 1 / (1 + ((1/init_NUE)-1)*Math.exp(NUE_rate * (available_N - N_decay_point)));
    	NUE = init_NUE * NUE_modifier;
    	return NUE;
    }
    
    
/*	public double calculateYield() {
		double B0Corn = 0.1164;
		double B0Grass = -0.9556;
		double B0Alfalfa = -0.08196;

		double B1Corn = 2.8849;
		double B1Grass = 2.4093;
		double B1Alfalfa = 2.40141;

		double B2Corn = 0.1;
		double B2Grass = 0.1;
		double B2Alfalfa = 0.0;
		
		double BCIYieldAdj = 1 - (0.2 * (1 - this.getBCI()));

		int cornVal = this.getCrop() == Crop.CORN ? 1 : 0;
		int grassVal = this.getCrop() == Crop.GRASS ? 1 : 0;
    	int alfalfaVal = this.getCrop() == Crop.ALFALFA ? 1 : 0;

		int fertilizerVal = this.management.fertilize ? 1 : 0;

		//disable first year grass yield. Could be done in ternary operator above but
		//  here for now in case it doesn't work right
		if(grassVal == 1 && getCurrentCrop() != Crop.GRASS){
			grassVal = 0;
		}

		double B0 = B0Corn * cornVal + B0Grass * grassVal + B0Alfalfa * alfalfaVal;
		double B1 = B1Corn * cornVal + B1Grass * grassVal + B1Alfalfa * alfalfaVal;
		double B2 = B2Corn * cornVal + B2Grass * grassVal + B2Alfalfa * alfalfaVal;

	    double yield = (B0 + B1 * Math.log(this.getSOC())) * ((B2 * fertilizerVal) + BCIYieldAdj);
	    	    
        // Convert from yield per hectare to yield for 40 acres
	    yield *= 40;
	    yield /= 2.47105;
	    return yield > 0 ? yield : 0 ;
	}
*/
	
	
	
// COSTS
	
	public double getCosts() {
		
        double fieldSize = 40.0;
		double CORN_EST = 422.04;
		double CORN_HIGH_N = 103.57;
		double CORN_LOW_N = 51.79;
		double CORN_TILL = 44.96;
        
		double GRASS_EST = 268.73;
		double GRASS_CONT = 182.16;
		double GRASS_HIGH_N = 44.66;
		double GRASS_LOW_N = 0;
        
        double ALFALFA_EST = 279.02;
        double ALFALFA_CONT = 185.59;
        
        if (this.getCrop().equals(Crop.CORN)){
			this.costs = CORN_EST * fieldSize;
            if (this.isFertilize()) this.costs += CORN_HIGH_N * fieldSize;
			else this.costs += CORN_LOW_N * fieldSize;
			
            if (this.isTill()) this.costs += CORN_TILL * fieldSize;
		}
		else if (this.getCrop().equals(Crop.GRASS)){
			if (!this.getHistory().getHistory().isEmpty() && this.getHistory().getHistory().getLast().crop.equals(Crop.GRASS))
				this.costs = GRASS_CONT * fieldSize;
			else this.costs = GRASS_EST * fieldSize;
			
            if (this.isFertilize()) this.costs += GRASS_HIGH_N * fieldSize;
			else this.costs += GRASS_LOW_N * fieldSize;
		}
		else if (this.getCrop().equals(Crop.ALFALFA)){
			if (!this.getHistory().getHistory().isEmpty() &&
               this.getHistory().getHistory().getLast().crop.equals(Crop.ALFALFA))
				this.costs = ALFALFA_CONT * fieldSize;
			else this.costs = ALFALFA_EST * fieldSize;
		}
        
        return this.costs; // returned in terms of dollars per 40 acre field
        
	}
	
	
	
	// REVENUE
    
		public double getFieldRevenue(Economy economy){
	        // update total revenue value
			this.fieldRevenue = 0;
			this.fieldRevenue = 
					getCornRevenue(economy) + 
					getGrassRevenue(economy) + 
					getAlfalfaRevenue(economy);
	        
			/*
	        if (this.crop.equals(Crop.CORN)) {
	        	this.fieldRevenue = economy.getCornPrice() * (((this.grainBiomass + (this.vegBiomass * (1-proportion_veg_retained)))/2.47105) * 40 / 1000); //Gives revenue in $/field.
	        }
	        else if (this.crop.equals(Crop.GRASS)) {
	        	this.fieldRevenue = economy.getGrassPrice() * ((this.vegBiomass*(1-proportion_veg_retained)/2.47105) * 40 / 1000); //Gives revenue in $/field.
	        }
	        else if (this.crop.equals(Crop.ALFALFA)) {
	        	this.fieldRevenue = economy.getAlfalfaPrice() * ((this.vegBiomass*(1-proportion_veg_retained)/2.47105) * 40 / 1000); //Gives revenue in $/field.
	        }
	        */
	          
	        return this.fieldRevenue;
		}
		
		public double getCornRevenue(Economy economy){
			if (this.crop.equals(Crop.CORN)) {
	        	return (economy.getCornPrice() * (((this.grainBiomass + (this.vegBiomass * (1-proportion_veg_retained)))/2.47105) * 40 / 1000)); //Gives revenue in $/field.
	        }
			return 0;
		}
		public double getGrassRevenue(Economy economy){
			if (this.crop.equals(Crop.GRASS)) {
	        	return (economy.getGrassPrice() * ((this.vegBiomass*(1-proportion_veg_retained)/2.47105) * 40 / 1000)); //Gives revenue in $/field.
	        }
			return 0;
		}
		public double getAlfalfaRevenue(Economy economy){
			if (this.crop.equals(Crop.ALFALFA)) {
	        	return (economy.getAlfalfaPrice() * ((this.vegBiomass*(1-proportion_veg_retained)/2.47105) * 40 / 1000)); //Gives revenue in $/field.
	        }
			return 0;
		}
	  


	
	
// ENERGY
	
	public double calculateEnergy() {
		this.netTotalEnergy = (calcRefineryPhaseEnergy() - calcAgPhaseEnergy());  // TODO : Removed /1000 (Now returns MJ instead of GJ - fix for display)
//		System.out.print(", net energy = " + this.netTotalEnergy + "\n");
	    return this.netTotalEnergy; // returns MJ of energy per 40 acre field
	}
	

	 private double calcAgPhaseEnergy(){ // all ag phase costs calculated as fixed costs per area of land, returned as total for entire field (40 acres)
	      double agEnergyIn = 0;	      
	      double plantingEnergy = 0;
	      double fertApplicationEnergy = 0;
	      double fertProductionEnergy = 0;
	      double pesticideApplicationEnergy = 0;
	      double pesticideProductionEnergy = 0;
	      double harvestEnergy = 0;
	      double cropTransportationEnergy;
	      double transportCost = 0.256; // MJ/kg
	      double tillEnergy = 0;
	      double N = 0;
	      // Fert application Rate
	      if(this.getCrop() == Crop.CORN){
	        if(this.management.fertilize == true){
	          N = 80.9373; //kg of N per ac @ high application rate (=200kgN/ha)
	          fertApplicationEnergy = 506 / 2.47105;
	        } else {
	          N = 40.469; //kg of N per ac @ low application rate (=100kgN/ha)
	          fertApplicationEnergy = 506 / 2.47105;
	        }
	      } else {
	        if(this.management.fertilize == true){
	          N = 20.235; //kg of N per ac @ high application rate (=50kgN/ha)
	          fertApplicationEnergy = 506 / 2.47105;
	        } else {
	          N = 0; //kg of N per ac @ low application rate (=0kgN/ha)
	          fertApplicationEnergy = 0;
	        }
	      }
	      if(this.isTill() == true){
	        tillEnergy = 4488 / 2.47105;
	      }
	      pesticideProductionEnergy = 2.4 * 356 / 2.47105; // per acre
	      pesticideApplicationEnergy = 157.5 / 2.47105; // per acre
	      if(this.getCrop() == Crop.GRASS){
	    	  if(getCurrentCrop() != Crop.GRASS){
	    		  harvestEnergy = 0;
	    		  plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre   
	    	  } else {
	    	      harvestEnergy = 574 / 2.47105;
	    	      plantingEnergy = 0; //MJ of energy from seed (Wang) per acre
	    	  }
	      } else if(this.getCrop() == Crop.ALFALFA){
	    	  if(getCurrentCrop() != Crop.ALFALFA){
	    		  harvestEnergy = 574 / 2.47105;
	    		  plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre   
	    	  } else {
	    	      harvestEnergy = 574 / 2.47105;
	    	      plantingEnergy = 0; //MJ of energy from seed (Wang) per acre
	    	  } 
	      } else if(this.getCrop() == Crop.CORN){
	    	  harvestEnergy = 574 / 2.47105;
	    	  plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre
	      } else { // fallow
	    	  harvestEnergy = 0;
	    	  plantingEnergy = 0; //MJ of energy from seed (Wang) per acre	    
	    	  fertApplicationEnergy = 0;
	    	  pesticideApplicationEnergy = 0;
	    	  pesticideProductionEnergy = 0;
	    	  harvestEnergy = 0;
	    	  tillEnergy = 0;
	      }
	      if(this.crop == Crop.CORN){
	          cropTransportationEnergy = transportCost * ((0.5 * this.vegBiomass + this.grainBiomass)/2.47105); // biomass reported in kg/ha, transportCost in MJ/kg - removed "* 1000", converted biomass to kg/ac 
	        } else {
	      	cropTransportationEnergy = transportCost * (this.vegBiomass)/2.47105;  // biomass reported in kg/ha, transportCost in MJ/kg - removed "* 1000", converted biomass to kg/ac 
	        }
	      fertProductionEnergy = N * 60.04;
//	      System.out.println("PRODUCTION PHASE");
//	      System.out.println("planting = " + plantingEnergy + " MJ/ac");
//	      System.out.println("fert application = " + fertApplicationEnergy + " MJ/ac");
//	      System.out.println("fert production = " + fertProductionEnergy + " MJ/ac");
//	      System.out.println("pesticide application = " + pesticideApplicationEnergy + " MJ/ac");
//	      System.out.println("pesticide production = " + pesticideProductionEnergy+ " MJ/ac");
//	      System.out.println("harvest = " + harvestEnergy + " MJ/ac");
//	      System.out.println("Transportation Energy = " + cropTransportationEnergy);
//	      System.out.println("till = " + tillEnergy + " MJ/ac");
	      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy); // returns energy used per acre
	      this.productionEnergyNet = (agEnergyIn + cropTransportationEnergy) * 40; // moved 40 to outside of addition
	      return this.productionEnergyNet; // returns production energy in MJ/40 acre field
	    }


  	 
    private double calcRefineryPhaseEnergy(){ // all refinery side calculations variable costs calculated as a function of yield (kg per acre)
      double coproductCredit = 4; // MJ/l
      double vegConversionRate = 0.38;
      double grainConversionRate = 0.4;
      double grainEnergyUsageRate = 12.58; // Cost 12.58 MJ of energy to make 1 L of fuel from corn
      double vegEnergyUsageRate = 3.1; // Cost 3.1 MJ of energy to make 1 L of fuel from grass
      double cellulosicEthanol = 0;
      double grainEthanol = 0;
      
      if (this.crop.equals(Crop.CORN)){
    	  grainEthanol = this.grainBiomass*(1-grainDistro) / 2.47105 * grainConversionRate; //removed * 1000, added /2.47105 - now returns ethanol per acre
    	  cellulosicEthanol = 0.5*this.vegBiomass*(1-stoverDistro) / 2.47105 * vegConversionRate; // returns ethanol per acre
      } 
      else if (this.crop.equals(Crop.GRASS)){
    	  cellulosicEthanol = (1 - this.proportion_veg_retained) * this.vegBiomass / 2.47105 * vegConversionRate; //removed * 1000, added /2.47105 - change 'vegUsageRate to '(1 - this.proportion_veg_retained)' - now returns ethanol per acre
      }
      double ethanolProduced = grainEthanol + cellulosicEthanol; // litres of ethanol per acre

      this.refineryEnergyIn = grainEnergyUsageRate * grainEthanol + vegEnergyUsageRate * cellulosicEthanol * 40; // returned as MJ/40 acre field
      System.out.println("refineryEnergyIn = " + this.refineryEnergyIn);
      this.refineryEnergyOut = ethanolProduced * (21.2 + coproductCredit) * 40; //energy output after feedstock is converted to ethanol (in MJ) - comes out to MJ/acre of production, returned as MJ/40 acre field
      
      this.refineryEnergyNet = refineryEnergyOut - refineryEnergyIn; // Net energy measured in MJ/acre
      /*System.out.println("REFINERY PHASE");
      System.out.println("yield passed in = " + yield);
      System.out.println("ethanol produced = " + ethanolProduced + " l");
      System.out.println("energy used = " + refineryEnergyIn + " MJ");
      System.out.println("energy produced = " + refineryEnergyOut + " MJ");
      System.out.println("NET energy produced = " + refineryEnergyNet + " MJ");*/
      
      return this.refineryEnergyNet; // returned as MJ/40 acre field
    }

    
    
// ENVIRONMENT SCORING ON FIELD
    
    public void calcFieldScores(){
	    // water score
	    	this.fieldWaterScore = 1 - (this.nitrogen_leached / maxNleached);	        
    
	    // soil score
	        // Max avg obtained after 10 years of alfalfa + fert : 1690
	        // Starting avg: 1400
	        // Worst avg obtained after 10 years grass w/ NO-fert : 1285
	    	// double maxAvg = 1600; double minAvg = 1200;
	    	double maxAvg = 2500; double minAvg = 500; // ballpark estimates, adjusted from farm-level averages (above)
		    fieldSoilScore = (getOrganicNforScore() - minAvg) / (maxAvg - minAvg);
		    
		// emissions score
		    double minEmissions = -1912;
		    double maxEmissions = 9320;
		    fieldEmissionsScore = 1 - (((this.fieldProductionEmissions) - minEmissions) / (maxEmissions - minEmissions));
		    
	    // summary environmental score
		    this.fieldEnvScore = (this.fieldWaterScore + this.fieldSoilScore + this.GBI + this.fieldEmissionsScore) / 4;
    }
    
    
    public double getFieldWaterScore(){
    	return this.fieldWaterScore;
    }
    
    public double getFieldSoilScore(){
    	return this.fieldSoilScore;
    }
    
    public double getFieldBGI(){
    	return this.GBI;
    }
    
    public double getFieldEmissionsScore(){
    	return this.fieldEmissionsScore;
    }
    
    public double getFieldEnvScore(){
    	return this.fieldEnvScore;
    }
    
   
// EMISSIONS   
    
    public double updateProductionEmissions(double yield){
        // emissions = respiration + n2o; // old approach
        // NOTE: only reflects emissions from actions, not soil-atmosphere exchanged (respiration)
    	System.out.println("---EMISSIONS---");
    	System.out.println("     Respiration = " + calcRespiration());
    	System.out.println("     Ag Phase = " + calcAgPhaseEmissions());
    	System.out.println("     Refinery = " + calcRefineryPhaseEmissions(yield));
    	fieldProductionEmissions = calcRespiration() + calcAgPhaseEmissions() + calcRefineryPhaseEmissions(yield);
        System.out.println("     Total Emissions (production, refinery, and respiration) = " + fieldProductionEmissions);
        return fieldProductionEmissions;
      }
    
    	
    public double calcRespiration(){		
    	double amountRespired = 0;
    	double amountSequestered = 0;
    	double C_N_ratio = 50; // assume an average of 50:1 C:N ratio - alfalfa = 17.1, corn ~50, SG ~ 76 (Jardat et al 2009)
    	amountRespired = this.mineralized_nitrogen * C_N_ratio;
    	amountSequestered = this.nitrogen_to_crop * C_N_ratio;
    	return (amountSequestered - amountRespired)/2.47105; // kg/acre
    }
    
    
  	public double calcAgPhaseEmissions(){
  		double fertWeight = 0;
  		double fertCo2 = 11.3; 
  		double plantingCo2 = 16.4;   // kg/ac
  		double sprayCo2 = 10.16;     // kg/ac
  		double harvestCo2 = 37.05;   // kg/ac
  		double tillCo2 = 289.7;      // kg/ac
  		int plantOn = 0;
  		int harvestOn = 0;
  		int tillOn = 0;
  		
  		if(this.crop.equals(Crop.CORN)){
			if(this.management.fertilize == true){
				fertWeight = 200 / 2.47105;	
			} else {
				fertWeight = 100 / 2.47105;			
			}} 
		else if(this.crop.equals(Crop.GRASS)){
			if(this.management.fertilize == true){
				fertWeight = 60 / 2.47105;	
			} else {
				fertWeight = 30 / 2.47105;					
			}} 
		else {
			if(this.management.fertilize == true){
				fertWeight =  30 / 2.47105;		
			} else {
				fertWeight = 0;
			}
		} 
  		
  		if(this.crop.equals(Crop.CORN)){
  			plantOn = 1;
  			harvestOn = 1;
  			if(this.isTill() == true){
  				tillOn = 1;
  			}
  		} 
  		if(this.crop.equals(Crop.GRASS)){
  			if(!getCurrentCrop().equals(Crop.GRASS)){ // don't harvest first year
  				plantOn = 1;
  			  	harvestOn = 0;
  	  			if(this.isTill() == true){
  	  				tillOn = 1;
  	  			}
  			}
  			if(getCurrentCrop().equals(Crop.GRASS)){ // don't plant subsequent years
  				plantOn = 0;
  				harvestOn = 1;
  			}
  		}
  		if(this.crop.equals(Crop.ALFALFA)){
  			harvestOn = 1;
  			if(!getCurrentCrop().equals(Crop.ALFALFA)){
  				plantOn = 1;
  			} else {
  				plantOn = 0;
  			}
  			if(this.isTill() == true) {
  				tillOn = 1;
  			}
  		}
  		this.fieldAgEmissions = plantingCo2 * plantOn + harvestCo2 * harvestOn + fertCo2 * fertWeight + sprayCo2 + tillCo2 * tillOn;
  		//this.fieldAgEmissions = (this.fieldAgEmissions); // returns kgs / acre
  		return this.fieldAgEmissions;
  	}
  	
  	
  	public double calcRefineryPhaseEmissions(double yield){
  		double celluloseFeedstock = 0;
  		double cornGrainFeedstock = 0;
  		// Adjust amount of corn feedstock to account for proportion of corn components actually used for fuel:
  		if(this.crop == Crop.CORN){
  			celluloseFeedstock = yield * 0.25 * (1-stoverDistro);
  			cornGrainFeedstock = yield * 0.50 * (1-grainDistro);
  		}
  		if(this.crop == Crop.GRASS){
  			celluloseFeedstock = yield;
  		}
  		// refineryEmissions = (0.0344 * celluloseFeedstock) + (2506.42 * cornGrainFeedstock); // kg
  		this.fieldRefineryEmissions = (47/1000 * celluloseFeedstock) + (419.57/1000 * cornGrainFeedstock); // g CO2/kg feedstock = kg/ton
  		return this.fieldRefineryEmissions;  //returns kgs / acre
  	}
    
    
    
    
    
    
    
    
    
    
    
    
    
    


 /*	 
	
	// Equation: https://glbrc.wikispaces.com/Soil+Organic+Carbon
	
	public void updateSOC() {
		int cornVal = this.crop == Crop.CORN ? 1 : 0;
		int grassVal = this.crop == Crop.GRASS ? 1 : 0;
		int alfalfaVal = this.crop == Crop.ALFALFA ? 1 : 0;
		int noTill = this.management.till ? 0 : 1;
		int highFert = this.management.fertilize ? 1 : 0;

		int MAXSOC = 190 ;

		double B0 = 0.2;
		double B1 = 0.17;
		double B2 = 0.19;
		double B3 = 0.1;
		double B4 = 0.2;

		double r = 1 + (-(B0 * cornVal) + (B1 * grassVal) + (B2 * alfalfaVal) + (B3 * noTill) - (B4 * highFert));
		double delta = ((r-1)*(1 - (SOC/MAXSOC))) * SOC;

		double oldSOC = SOC;
		SOC += delta;

		if (SOC > MAXSOC)
			SOC = MAXSOC;
		else if (SOC < 1.4869)
			SOC = 1.4869; // need to keep yields positive

		this.respiration = ((oldSOC - SOC) * (44/12) * (40 / 2.47105)) / 20; // tons per field - note: included division by twenty to have realistic estimates of sequestration/respiration over one year to match with other CO2 sources (ag and refinery phase). Did this because our formula is intended to calc carbon change over 20 year period
	}
 
    private double calcAgPhaseEnergy(){
 	      double agEnergyIn = 0;
 	      double refineryEnergyIn = 0;
 	      double energyOut = 0;
 	      double netEnergyYield = 0;
 	      
 	      double plantingEnergy = 0;
 	      double fertApplicationEnergy = 0;
 	      double fertProductionEnergy = 0;
 	      double pesticideApplicationEnergy = 0;
 	      double pesticideProductionEnergy = 0;
 	      double harvestEnergy = 0;
 	      double tillEnergy = 0;
 	      double N = 0;
 	      // Fert application Rate
 	      if(this.getCrop() == crop.Corn){
 	        this.management.fertilize == true){
 	          N = 80.9373; //kg of N per ac @ high application rate (=50kgN/ha)
 	          fertApplicationEnergy = 506 / 2.47105;
 	        } else {
 	          N = 40.469; //kg of N per ac @ low application rate (=0kgN/ha)
 	          fertApplicationEnergy = 506 / 2.47105;
 	        }
 	      } else {
 	      if(this.management.fertilize == true){
 	          N = 20.235; //kg of N per ac @ high application rate (=50kgN/ha)
 	          fertApplicationEnergy = 506 / 2.47105;
 	        } else {
 	          N = 0; //kg of N per ac @ low application rate (=0kgN/ha)
 	          fertApplicationEnergy = 0;
 	        }
 	      }
 	      if(this.management.till == true){
 	        tillEnergy = 4488 / 2.47105;
 	      }
 	      pesticideProductionEnergy = 2.4 * 356 / 2.47105; // per acre
 	      pesticideApplicationEnergy = 157.5 / 2.47105; // per acre
 	      harvestEnergy = 574 / 2.47105;
 	      plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre
 	      fertProductionEnergy = N * 60.04;
 	      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy);
 	      return agEnergyIn;
 	    }
    
	 private double calcCornEnergy(double cornGrainYield) {
      double agEnergyIn = 0;
      double refineryEnergyIn = 0;
      double energyOut = 0;
      double netEnergyYield = 0;
      
      double plantingEnergy = 0;
      double fertApplicationEnergy = 0;
      double fertProductionEnergy = 0;
      double pesticideApplicationEnergy = 0;
      double pesticideProductionEnergy = 0;
      double harvestEnergy = 0;
      double tillEnergy = 0;
      double N = 0;
      if(this.management.fertilize == true){
        N = 80.9373; //kg of N per ac @ high application rate (=50kgN/ha)
        fertApplicationEnergy = 506 / 2.47105;
      } else {
        N = 40.469; //kg of N per ac @ low application rate (=0kgN/ha)
        fertApplicationEnergy = 506 / 2.47105;
      }
      if(this.management.till == true){
        tillEnergy = 4488 / 2.47105;
      }
      pesticideProductionEnergy = 2.4 * 356 / 2.47105; // per acre
      pesticideApplicationEnergy = 157.5 / 2.47105; // per acre
      harvestEnergy = 574 / 2.47105;
      plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre
      fertProductionEnergy = N * 60.04;
      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy);
      double cropTransportationEnergy = 0.6234 * cornGrainYield * 1000;
      double refiningEnergy = 27.85 * cornGrainYield * 1000; //MJ/kg
      refineryEnergyIn = cropTransportationEnergy + refiningEnergy;
      energyOut = cornGrainYield * 1000 * 0.4 * 21.2; //energy output after feedstock is converted to ethanol (in MJ)
      netEnergyYield = energyOut - agEnergyIn - refineryEnergyIn;
      //System.out.print("CORN GRAIN: Ag energy in = " + agEnergyIn + ", refining_energy = " + refineryEnergyIn + ", energy out = " + energyOut);
      return netEnergyYield;
    }
	
	
	 private double calcCelluloseEnergy(double celluloseYield) {
	    double agEnergyIn = 0;
	    double refineryEnergyIn = 0;
	    double energyOut = 0;
	    double netEnergyYield = 0;
	    
	    double plantingEnergy = 0;
	    double fertApplicationEnergy = 0;
	    double fertProductionEnergy = 0;
	    double pesticideApplicationEnergy = 0;
	    double pesticideProductionEnergy = 0;
	    double harvestEnergy = 0;
	    double tillEnergy = 0;
	    double N = 0;
    	if(this.management.fertilize == true){
    		N = 20.235; //kg of N per ac @ high application rate (=50kgN/ha)
    		fertApplicationEnergy = 506 / 2.47105;
    	} else {
    		N = 0; //kg of N per ac @ low application rate (=0kgN/ha)
    		fertApplicationEnergy = 0;
    	}
      if(this.getCrop() == Crop.GRASS && getCurrentCrop() != Crop.GRASS){ // first year grass
        plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre
        harvestEnergy = 0;
      }
      if(this.getCrop() == Crop.GRASS && getCurrentCrop() == Crop.GRASS){ // 2+ years grass (has yield, but no need to replant)
        plantingEnergy = 0;
        harvestEnergy = 574 / 2.47105; // from wang per acre
      }
      if(this.getCrop() == crop.CORN){
        harvestEnergy = 574 / 2.47105; // from wang per acre - stover only has harvest energy - planting energy applied during corn calculations
      }
      if(this.getCrop() == Crop.GRASS && this.management.till == true){
        tillEnergy = 4488 / 2.47105;
      }
      pesticideProductionEnergy = 2.4 * 356 / 2.47105; // per acre
      pesticideApplicationEnergy = 157.5 / 2.47105; // per acre
      fertProductionEnergy = N * 60.04;
      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy);
      double cropTransportationEnergy = 0.6234 * celluloseYield * 1000;
      double refiningEnergy = 0.1579 * celluloseYield * 1000;
      refineryEnergyIn = cropTransportationEnergy + refiningEnergy;
      energyOut = celluloseYield * 1000 * 0.38 * 21.2; //energy output after feedstock is converted to ethanol (in MJ)
      netEnergyYield = energyOut - agEnergyIn - refineryEnergyIn;  
      //System.out.print("CELLULOSIC: Ag energy in = " + agEnergyIn + ", refining_energy = " + refineryEnergyIn + ", energy out = " + energyOut);
      return netEnergyYield;
    }


	 private double calcStoverEnergy(double cornYield) {
	    double agEnergyIn = 0;
	    double refineryEnergyIn = 0;
	    double energyOut = 0;
	    double netEnergyYield = 0;
	    
	    double plantingEnergy = 0;
	    double fertApplicationEnergy = 0;
	    double fertProductionEnergy = 0;
	    double pesticideApplicationEnergy = 0;
	    double pesticideProductionEnergy = 0;
	    double harvestEnergy = 0;
	    double tillEnergy = 0;
	    double N = 0;
      harvestEnergy = 574 / 2.47105; // debatable - doubling harvest energy?
      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy);
      double cropTransportationEnergy = 0.6234 * cornYield * 1000;
      double refiningEnergy = 0.1579 * cornYield * 1000;
      refineryEnergyIn = cropTransportationEnergy + refiningEnergy;
      energyOut = celluloseYield * 1000 * 0.38 * 21.2; //energy output after feedstock is converted to ethanol (in MJ)
      netEnergyYield = energyOut - agEnergyIn - refineryEnergyIn;  
      //System.out.print("CELLULOSIC: Ag energy in = " + agEnergyIn + ", refining_energy = " + refineryEnergyIn + ", energy out = " + energyOut);
      return netEnergyYield;
    }

	 private double calcAlfalfaEnergy(double alfalfaYield) {
	    double agEnergyIn = 0;
	    double refineryEnergyIn = 0;
	    double energyOut = 0;
	    double netEnergyYield = 0;
	    
	    double plantingEnergy = 0;
	    double fertApplicationEnergy = 0;
	    double fertProductionEnergy = 0;
	    double pesticideApplicationEnergy = 0;
	    double pesticideProductionEnergy = 0;
	    double harvestEnergy = 0;
	    double tillEnergy = 0;
	    double N = 0;
    	if(this.management.fertilize == true){
    		N = 20.235; //kg of N per ac @ high application rate (=50kgN/ha)
    		fertApplicationEnergy = 506 / 2.47105;
    	} else {
    		N = 0; //kg of N per ac @ low application rate (=0kgN/ha)
    		fertApplicationEnergy = 0;
    	}
      if(this.management.till == true){
        tillEnergy = 4488 / 2.47105;
      }
      pesticideProductionEnergy = 2.4 * 356 / 2.47105; // per acre
      pesticideApplicationEnergy = 157.5 / 2.47105; // per acre
      harvestEnergy = 574 / 2.47105;
      plantingEnergy = 8.741223 * 53.36; //MJ of energy from seed (Wang) per acre
      fertProductionEnergy = N * 60.04;
      agEnergyIn = (plantingEnergy + fertApplicationEnergy + fertProductionEnergy + pesticideApplicationEnergy + pesticideProductionEnergy + harvestEnergy + tillEnergy) * 40;
      double cropTransportationEnergy = 0.6234 * alfalfaYield;
      refineryEnergyIn = cropTranspotationEnergy;
      energyOut = 0;
      netEnergyYield = energyOut - agEnergyIn - refineryEnergyIn;
      //System.out.print("ALFALFA: Ag energy in = " + agEnergyIn + ", refining_energy = " + refineryEnergyIn + ", energy out = " + energyOut);
      return netEnergyYield;
	}
*/
    
    public String toString() {
        return "\n\tSoil organic N: " + this.organic_nitrogen + "\n\tBCI: " + this.BCI + "\n\tGBI: " + this.GBI + 
        "\n\tN2O: " + this.n2o + "\n\tRespiration: " + this.respiration + 
        "\n\tEmissions: " + this.fieldProductionEmissions + "\n\tCosts: " + this.costs + "\n\tCrop: " + this.crop + 
        "\n\tYield: " + this.lastYield + "\n\tFertilizer: " + this.management.fertilize + 
        "\n\tTill: " + this.management.till + "\n";
    }
}
	
	
