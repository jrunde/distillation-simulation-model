$title Fields of Fuel Model
option limrow = 100, limcol = 100;
$Oninline
option optcr = 0;

sets
ST "Different Scoring Categories" /Ec 'Economy', En 'Energy', Env 'Environment'/
crop "Crop Types" /corn, grass, alfalfa/
perr(crop) "Perennials" /grass, alfalfa/
notGrass(crop) "All crops that are not grass" /corn, alfalfa/
mgnt "Management options" /fert, till/
field(*) "Bot's Fields"
;

**************************************************************************
*********************** Data Input (From Java) ***************************
**************************************************************************

parameter Weights(ST);
parameters
Price(crop)
prevZ1(field, crop) "Previous planting decisions"
prevOrgNit(field) "Organic nitrogen levels on each field after last year's harvest"
prevRootBio(field) "Previous root biomass left on field after last year's harvest"
;


Scalars
capital_previous
z2_up "Used for fixing z2 vars if managment is turned off"
ecScore_previous
*Assume last year's corn distributions
stoverDistro "Percent of corn stover used for feed"
grainDistro "Percent of corn grain used for feed"
;

$if not set dbIn $abort 'no file name for in-database 1 file provided'
$gdxin %dbIn%
$load field
$load Price, Weights, z2_up, prevZ1, prevOrgNit, prevRootBio
$load ecScore_previous, capital_previous, stoverDistro, grainDistro
$gdxin


**************************************************************************
*************************** Hueristics ***********************************
**************************************************************************

*On starting field values and prices, the maximum increase in farm capital
*possible for the first three years is about $17k, $28k, and $33k.
*Furthermore, with maximum ON and roots after 25 years, the maximum increase in
*farm capital is ~$35k. Therefore, we will split the difference between these,
*assuming that one player is generally maximizing his/her economic potential,
*but isn't necessarily able to build the field health necessary to acheive
*the maximum gains.
*scalar maxIncrease /28500/;
scalar maxIncrease /40000/;
maxIncrease = maxIncrease*2.47105;
scalar maxCapital; maxCapital = (capital_previous/ecScore_previous)+maxIncrease;

*Assume the proportion of neightboring fields that plant grass is dependent on
*both the current price of grass (higher the price, the more likely neighbors
*are to plant grass), as well as the previous year's decision to plant.
scalar pGrass "Proportion of neighboring fields that plant grass";
pGrass = 0;
loop(field, pGrass = pGrass + 0.125$(prevZ1(field,'grass') gt 0));
pGrass = pGrass + .125$(price('grass') > 50) + .125$(price('grass') > 100) + .125$(price('grass') > 125) + .125$(price('grass') > 150);

*Assume that corn distributions are average of previous year's and 50/50 (stabilize the hueristic):
stoverDistro = (stoverDistro + 0.5)/2;
grainDistro = (grainDistro + 0.5)/2;


**************************************************************************
************************ Overarching Model *******************************
**************************************************************************

free variables
overallScore "Overall Sustainability Score to maximize"
Score(ST)    "Individual compononents of overallScore"
;

binary variables
z1(field, crop) "Indicates decision to plant 'crop' on 'field'"
z2(field, mgnt) "Indicates management decisions on each field"
;

equations
overallScore_def "Definition of overallScore"
crop_cons(field) "Only one crop can be planted per field"
;

*Equation Listing:
overallScore_def..
overallScore =e= sum(ST, Score(ST)*Weights(ST));

crop_cons(field)..
sum(crop, z1(field,crop)) =e= 1;


**************************************************************************
*********************** Indicator Variables ******************************
**************************************************************************

binary variables
d_est(field, crop)   "Indicates the crop is being established this year on the field"
d_cont(field, crop)  "Indicates the crop is a continuing perennial on the field"
d_ct(field,crop) "Indicates decision to till the crop on the field"
d_f(field,crop) "Indicates decision to fertilize the crop on the field"

d_t(field,crop) "Indicates crop is actually tilled (can't till continuting perennials)"
;

equations
d_est_cons(field)  "At most one crop is established per field"
d_cont_cons(field) "At most one perennial is continued per field"
d_ct_cons(field) "At most one crop is tilled per field"
d_t_cons(field) "At most one crop is actually tilled per field"
d_f_cons(field) "At most one crop is fertilized per field"

est_corn(field,crop) "Corn must always be established"
est_perr(field,crop) "First year perrenials are established"
cont_perr(field,crop) "2nd+ year perrenials are continued"
estORcont(field) "Each field has an established or continued crop"

till_control(field,crop) "Controls the d_t variable"
fert_control(field,crop) "Controls the d_f variable"
till_back1(field,crop)   "BiDirectional constraint"
till_back2(field,crop)   "BiDirectional constraint"
fert_back1(field,crop)   "BiDirectional constraint"
fert_back2(field,crop)   "BiDirectional constraint"

till_on(field,crop) "Turns tilling on if d_ct & d_est"
till_on2(field,crop) "BiDirectional constraint"
till_on3(field,crop) "BiDirectional constraint"
;

*Only 1 crop can exist on a field at a time
d_est_cons(field) .. sum(crop, d_est(field,crop)) =l= 1;
d_cont_cons(field) .. sum(crop, d_cont(field,crop)) =l= 1;
d_ct_cons(field) .. sum(crop, d_ct(field,crop)) =l= 1;
d_t_cons(field) .. sum(crop, d_t(field,crop)) =l= 1;
d_f_cons(field) .. sum(crop, d_f(field,crop)) =l= 1;

*Crop is established if corn or first year grass/alfalfa (cont otherwise)
est_corn(field,crop)$(not perr(crop)) .. d_est(field,crop) =e= z1(field,crop);
est_perr(field,perr) .. d_est(field,perr) =e= z1(field,perr)$(prevZ1(field,perr) lt 1);
cont_perr(field,perr) .. 1 + d_cont(field,perr) =g= z1(field,perr) + prevZ1(field,perr);
estORcont(field) .. sum(crop, d_est(field,crop) + d_cont(field,crop)) =e= 1;

*Crop on field is tilled if z1 & z2
till_control(field,crop) .. 1 + d_ct(field,crop) =g= z1(field,crop) + z2(field,'till');
till_back1(field,crop)   .. d_ct(field,crop) =l= z1(field,crop);
till_back2(field,crop)   .. d_ct(field,crop) =l= z2(field,'till');
fert_control(field,crop) .. 1 + d_f(field,crop) =g= z1(field,crop) + z2(field,'fert');
fert_back1(field,crop)   .. d_f(field,crop) =l= z1(field,crop);
fert_back2(field,crop)   .. d_f(field,crop) =l= z2(field,'fert');

*Actually allow tilling only if crop is established on field this year
till_on(field,crop)  .. 1 + d_t(field,crop) =g= d_ct(field,crop) + d_est(field,crop);
till_on2(field,crop) .. d_t(field,crop) =l= d_ct(field,crop);
till_on3(field,crop) .. d_t(field,crop) =l= d_est(field,crop);

**************************************************************************
********************** Yield/Nitrogen Model ******************************
**************************************************************************

set
tissue /root, veg, grain/
above(tissue) /veg, grain/
;

positive variables
yield(field,crop) "Kg of crop produced per acre [0 for unplanted crops]"
BCI(field) "BCI of each field on farm"
totalYield(field,crop) "Above ground and below ground yield of crop"
NUE(field, crop) "Nitrogen usage efficiency-rate of each crop"
nit_2_crop(field,crop)  "Available nitrogen that goes to the crop"
nit_leached(field,crop) "Nitrogen leached to water"

inorg_nit(field,crop) "Available inorganic nitrogen on each field"
mineralizedNit_tilled(field,crop) "Mineralized organic nitrogen on each field"
mineralizedNit_notTilled(field,crop) "Different mineralization rate if tilled/notTilled"
tissueBiomass(field,crop,tissue) "Amount of biomass in each tissue of each crop on each field"
residualNit(field,crop) "Dead crop material in field from previous year's harvest"
orgNit(field,crop) "Organic nitrogen level on each field after this year's harvest"
;

parameters
containedNit(crop,tissue) /corn.grain 0.0076, corn.veg 0.0036, corn.root 0.0030,
                           grass.grain 0, grass.veg 0.0005, grass.root 0.0085,
                           alfalfa.grain 0, alfalfa.veg 0.0124, alfalfa.root 0.0144/
grain2veg(crop) /corn 0.5, grass 0, alfalfa 0/
root2shoot(crop) /corn 0.18, grass 75, alfalfa 0.87/
root2shoot2(crop) /grass 3/
root_retained(crop) "proportion of living roots retained each year" /corn 0, grass 0.75, alfalfa 0/
veg_retained(crop) "proportion of dead veg left on field after last year's harvest" /corn 0.5, grass 1, alfalfa 0/
veg_retained2(crop) /grass 0/
;
containedNit(crop,tissue) = containedNit(crop,tissue) / 2.47105;


parameters
perc_Bio(crop,tissue) "Percent biomass in each tissue type"
perc_Bio2(crop,tissue) "Percent biomass in each tissue type for continuing perennials"
usage_Rate(crop) "Biomass yield per unit nitrogen"
usage_Rate2(crop) "Used for continuing grass"
initial_NUE(crop)  "Nitrogen usage efficiency"
initial_NUE2(crop)  "Used for continuing grass"
decay_rate_NUE  "Used to calculate NUE based on current nitrogen levels" /0.03/
decay_point_NUE(crop) "Level of inorganic_nitrogen at which NUE  is unchanged"

;
perc_Bio(crop,'root') = root2shoot(crop)*(1 - root_retained(crop))/(1+(root2shoot(crop)*(1-root_retained(crop))));
perc_Bio(crop,'grain') = grain2veg(crop) * (1 - perc_Bio(crop,'root'));
perc_Bio(crop,'veg') = 1 - perc_Bio(crop,'root') - perc_Bio(crop,'grain');
perc_Bio2(crop,'root') = root2shoot2(crop)*(1 - root_retained(crop))/(1+(root2shoot2(crop)*(1-root_retained(crop))));
perc_Bio2(crop,'grain') = grain2veg(crop) * (1 - perc_Bio2(crop,'root'));
perc_Bio2(crop,'veg') = 1 - perc_Bio2(crop,'root') - perc_Bio2(crop,'grain');
usage_Rate(crop) = sum(tissue, perc_Bio(crop,tissue)*containedNit(crop,tissue));
usage_Rate2(crop) = sum(tissue, perc_Bio2(crop,tissue)*containedNit(crop,tissue));
initial_NUE(notGrass) = 0.8295833;
decay_point_NUE(notGrass) = 200;
initial_NUE('grass') = 0.5;
initial_NUE2('grass') = 0.95;
decay_point_NUE('grass') = 75;

parameters fertWeight(crop), extraFertWeight(crop);
fertWeight('corn') = 100/2.47105;
fertWeight('grass') = 30/2.47105;
fertWeight('alfalfa') = 0/2.47105;
extraFertWeight('corn') = (200-100)/2.47105;
extraFertWeight(perr) = 30/2.47015;

scalars
volitilization_rate "Nitrogen lost to environment" /0.3/
mineralization_rate "Nitrogen that mineralizes"   /0.01/
extraMin_rate       "Extra nitrogen that mineralizes due to tilling" /0.02/
;

equations
yield_def_notGrass(field,crop)
yield_def_Grass(field,crop)
totalYield_def_notGrass(field,crop)
totalYield_def_Grass(field,crop)
NUE_def_notGrass(field, crop)
NUE_def_Grass(field, crop)
nit2crop_def(field,crop)

nitLeached_def(field,crop)

inorgNit_def(field,crop)
minNitTilled_def1(field,crop)
minNitTilled_def2(field,crop)
minNitTilled_def3(field,crop)
minNitNotTilled_def1(field,crop)
minNitNotTilled_def2(field,crop)
minNitNotTilled_def3(field,crop)

tissueBiomass_def_notGrass(field,crop,tissue)
tissueBiomass_def_Grass(field,crop,tissue)
residNit_def_notGrass(field,crop)
residNit_def_Grass(field,crop)
orgNit_def_notGrass(field,crop)
orgNit_def_Grass(field,crop)
;

*Equation Listing
yield_def_notGrass(field,notGrass)..
yield(field,notGrass) =e= totalYield(field,notGrass)*sum(above, perc_bio(notGrass,above))*(1 - (0.2 * (1 - BCI(field))));
yield_def_Grass(field,crop)$(not notGrass(crop))..
yield(field,crop) =e= totalYield(field,crop)*sum(above, perc_bio(crop,above)$(prevZ1(field,crop) eq 0) + perc_bio2(crop,above)$(prevZ1(field,crop) gt 0));

totalYield_def_notGrass(field,notGrass)..
(totalYield(field,notGrass)*usage_Rate(notGrass) - nit_2_crop(field,notGrass))$(ord(notGrass) eq 1) + (totalYield(field,notGrass) - 2.47105*(6000*z1(field,notGrass) + 50*nit_2_crop(field,notGrass)*NUE(field, 'alfalfa')))$(ord(notGrass) gt 1) =e= 0;
totalYield_def_Grass(field,crop)$(not notGrass(crop))..
totalYield(field,crop)*(usage_Rate(crop)$(prevZ1(field,crop) eq 0) + usage_Rate2(crop)$(prevZ1(field,crop) gt 0)) =e= nit_2_crop(field,crop);

NUE_def_notGrass(field, notGrass)..
NUE(field, notGrass) =e= initial_NUE(notGrass) * ( 1 / (1 + ((1/initial_NUE(notGrass))-1)*exp(decay_rate_NUE * (inorg_nit(field,notGrass) - decay_point_NUE(notGrass)))) );
NUE_def_Grass(field, crop)$(not notGrass(crop))..
NUE(field, crop) =e= (initial_NUE(crop)$(prevZ1(field,crop) eq 0) + initial_NUE2(crop)$(prevZ1(field,crop) gt 0)) * ( 1 / (1 + ((1/(initial_NUE(crop)$(prevZ1(field,crop) eq 0) + initial_NUE2(crop)$(prevZ1(field,crop) gt 0)))-1)*exp(decay_rate_NUE * (inorg_nit(field,crop) - decay_point_NUE(crop)))) );

nit2crop_def(field,crop)..
nit_2_crop(field,crop) =e= inorg_nit(field,crop)*NUE(field, crop);

nitLeached_def(field,crop)..
nit_leached(field,crop) =e= inorg_nit(field,crop)*(1 - NUE(field, crop));

inorgNit_def(field,crop)..
inorg_nit(field,crop) =e= (fertWeight(crop)*2.47105*z1(field,crop) + extraFertWeight(crop)*2.47105*d_f(field,crop))*(1-volitilization_rate) + ((mineralizedNit_tilled(field,crop)+mineralizedNit_notTilled(field,crop)));

scalar M "Upper bound on mineralized organic nitrogen" /3500/;
minNitTilled_def1(field,crop)..
mineralizedNit_tilled(field,crop) =l= M*d_t(field,crop);
minNitTilled_def2(field,crop)..
mineralizedNit_tilled(field,crop) + M*d_t(field,crop) =l= M + prevOrgNit(field)*(mineralization_rate + extraMin_rate)*z1(field,crop);
minNitTilled_def3(field,crop)..
mineralizedNit_tilled(field,crop) - M*d_t(field,crop) =g= -M + prevOrgNit(field)*(mineralization_rate + extraMin_rate)*z1(field,crop);

minNitNotTilled_def1(field,crop)..
mineralizedNit_notTilled(field,crop) =l= M*(1-d_t(field,crop));
minNitNotTilled_def2(field,crop)..
mineralizedNit_notTilled(field,crop) + M*(1-d_t(field,crop)) =l= M + prevOrgNit(field)*mineralization_rate*z1(field,crop);
minNitNotTilled_def3(field,crop)..
mineralizedNit_notTilled(field,crop) - M*(1-d_t(field,crop)) =g= -M + prevOrgNit(field)*mineralization_rate*z1(field,crop);


tissueBiomass_def_notGrass(field,notGrass,tissue)..
tissueBiomass(field,notGrass,tissue) =e= (totalYield(field,notGrass)*perc_Bio(notGrass,tissue))$(ord(tissue) ne 1) + (totalYield(field,notGrass)*perc_Bio(notGrass,tissue)*(1-root_retained(notGrass)) + (z1(field,notGrass)*prevRootBio(field))*root_retained(notGrass))$(ord(tissue) eq 1);
tissueBiomass_def_Grass(field,crop,tissue)$(not notGrass(crop))..
tissueBiomass(field,crop,tissue) =e= (totalYield(field,crop)*(perc_bio(crop,tissue)$(prevZ1(field,crop) eq 0) + perc_bio2(crop,tissue)$(prevZ1(field,crop) gt 0)))$(ord(tissue) ne 1) + (totalYield(field,crop)*(perc_bio(crop,tissue)$(prevZ1(field,crop) eq 0) + perc_bio2(crop,tissue)$(prevZ1(field,crop) gt 0))*(1-root_retained(crop)) + (z1(field,crop)*prevRootBio(field))*root_retained(crop))$(ord(tissue) eq 1);

residNit_def_notGrass(field,notGrass)..
residualNit(field,notGrass) =e= (tissueBiomass(field,notGrass,'root') * containedNit(notGrass,'root') * (1-root_retained(notGrass))) + (tissueBiomass(field,notGrass,'veg') * containedNit(notGrass,'veg') * veg_retained(notGrass));
residNit_def_Grass(field,crop)$(not notGrass(crop))..
residualNit(field,crop) =e= (tissueBiomass(field,crop,'root') * containedNit(crop,'root') * (1-root_retained(crop))) + (tissueBiomass(field,crop,'veg') * containedNit(crop,'veg') * (veg_retained(crop)$(prevZ1(field,crop) eq 0) + veg_retained2(crop)$(prevZ1(field,crop) gt 0)));

orgNit_def_notGrass(field,notGrass)..
orgNit(field,notGrass) =e= prevOrgNit(field)*z1(field,notGrass) - (mineralizedNit_tilled(field,notGrass)+mineralizedNit_notTilled(field,notGrass)) + residualNit(field,notGrass) + (prevRootBio(field)*containedNit('grass','root')*z1(field,notGrass))$(prevZ1(field,'grass') eq 1);
orgNit_def_Grass(field,crop)$(not notGrass(crop))..
orgNit(field,crop) =e= prevOrgNit(field)*z1(field,crop) - (mineralizedNit_tilled(field,crop)+mineralizedNit_notTilled(field,crop)) + residualNit(field,crop);


**************************************************************************
************************ Economy Model ***********************************
**************************************************************************

scalar fieldSize /40/;
parameters
estCost(crop)  "Cost of planting crop" /corn 422.04, grass 268.73, alfalfa 279.02/
contCost(crop) "Cost of continuing perennial" /grass 182.16, alfalfa 185.59/
FertCost(crop)  "Low-fert cost of fertilizing" /corn 51.79, grass 0, alfalfa 0/
extraFertCost(crop)  "Extra cost for choosing high-fert" /grass 44.66, alfalfa 0/
tillCost(crop)      /corn 44.96, grass 0, alfalfa 0/
;
extraFertCost('corn') = 103.57 - 51.79;

variables
capital "Overall farm capital";
positive variables
farmRevenue "Revenue generated each round from farm"
  fieldRevenue(field) "Revenue gained from each field"
farmCosts "Cost of operating farm each round"
  plantCost(field)
  mgntCost(field)
;

equations
econScore_def "Definition of EconScore"
capital_def   "Definition of current year capital"
farmRevenue_def "Definition of farm's revenue this round"
  fieldRev_def(field) "Definition of each field's revenue this round"
farmCosts_def "Definition of current year farm operating cost"
  plantCost_def(field)
  mgntCost_def(field)
;

*Equation Listing:
econScore_def..
maxCapital*Score('Ec') =e= capital;

capital_def..
Capital =e= capital_previous + farmRevenue - farmCosts;

farmRevenue_def..
farmRevenue =e= sum(field, fieldRevenue(field));
*   fieldRev_def(field).. fieldRevenue(field) =e= (price('corn')*(tissueBiomass(field,'corn','grain')/2.47105) + price('grass')*((tissueBiomass(field,'corn','veg')/2.47105)*(1-veg_retained('corn'))+(tissueBiomass(field,'grass','veg')/2.47105)$(prevZ1(field,'grass') eq 1)) + price('alfalfa')*(tissueBiomass(field,'alfalfa','veg')/2.47105)*(1-veg_retained('alfalfa')))*40/1000;
   fieldRev_def(field).. fieldRevenue(field) =e= sum(crop, price(crop) * ((tissueBiomass(field,crop,'grain') + tissueBiomass(field,crop,'veg')*(1-veg_retained(crop))))*(40/2471.05));

farmCosts_def..
farmCosts =e= sum(field, plantCost(field) + mgntCost(field));
  plantCost_def(field).. plantCost(field) =e= fieldSize*sum(crop, estCost(crop)*d_est(field,crop) + contCost(crop)*d_cont(field,crop));
  mgntCost_def(field).. mgntCost(field) =e= fieldSize*sum(crop, FertCost(crop)*z1(field,crop) + extraFertCost(crop)*d_f(field,crop) + tillCost(crop)*d_t(field,crop));


**************************************************************************
************************* Energy Model ***********************************
**************************************************************************

scalars
Emax, Emin, CoprodCred, PestProdEn, PestAppEn,
TillEn, HarvestEn, PlantEn, FertAppCost;
Emax = 2020000*2.47105;  /* Maximum net energy per field assuming 25 year max ON & root */
Emin = -225000*2.47105;  /* Minimum net energy per field */
CoprodCred = 4;
PestProdEn = (2.4*356)/2.47105;
PestAppEn = 157.5/2.47105;
TillEn = 4488/2.47105;
HarvestEn = 574/2.47105;
PlantEn = 8.741223*53.36;
FertAppCost = 506/2.47105;

parameters
grainConvRate(crop) /corn 0.4/
vegConvRate(crop) /corn 0.38, grass 0.38, alfalfa 0.38/
EnergyUseRate(tissue) /grain 12.58, veg 3.1/
NitroEn(crop) /corn 40.469/
ExtraNitroEn(crop)
;
ExtraNitroEn(perr) = 20.235;
ExtraNitroEn('corn') = 80.9373-40.469;

free variables
netEnergy "Overall farm energy gain/loss"
RefPhaseEn(field) "Net refinery phase energy";
positive variables
  RefEnOut(field)
  RefEnIn(field)
    EthanolProd(field,crop) "Ethanol produced on field by crop"
      grainEthanol(field,crop)
      cellulosicEthanol(field,crop)
AgPhaseEn(field) "Net agriculture phase energy"
  CropTrnsEn(field,crop) "Energy cost of transporting crop MJ/acre"
  AgEnIn(field)
    farmEn(field) "Energy req to plant/harvest crops"
    fertEn(field) "Energy req to fertilize crops"
      fertProdEn(field,crop)
      fertAppEn(field,crop)
;

equations
energyScore_def "Definition of EconScore"

netEnergy_def   "Definition net farm energy production/use"
 refPhaseEn_def(field)  "Definition of refinery phase energy"
   refEnOut_def(field)
   refEnIn_def(field)
     EthProd_def(field,crop)
       grainEthanol_def(field,crop)
       cellulosicEthanol_def_Corn(field,crop)
       cellulosicEthanol_def_alfalfa(field,crop)
       cellulosicEthanol_def_Grass(field,crop)
 AgPhaseEn_def(field) "Definition of agriculture phase energy"
   CropTrns_def(field,crop)
   AgEnIn_def(field)
     farmEn_def(field)
     fertEn_def(field)
       fertProdEn_def(field,crop)
       fertAppEn_def(field,crop)
;

*Equation Listing:
energyScore_def..
(Emax-Emin)*Score('En') =e= (1/card(field))*netEnergy - Emin;

netEnergy_def..
netEnergy =e= sum(field, RefPhaseEn(field) - AgPhaseEn(field));

 refPhaseEn_def(field)..
 RefPhaseEn(field) =e= (-RefEnIn(field) + RefEnOut(field))*40;

   refEnOut_def(field).. RefEnOut(field) =e= sum(crop, EthanolProd(field,crop)*(21.2 + CoprodCred));
   refEnIn_def(field).. RefEnIn(field) =e= sum(crop, grainEthanol(field,crop)*EnergyUseRate('grain') + cellulosicEthanol(field,crop)*EnergyUseRate('veg'));
     EthProd_def(field,crop).. EthanolProd(field,crop) =e= grainEthanol(field,crop) + cellulosicEthanol(field,crop);
       grainEthanol_def(field,crop).. grainEthanol(field,crop) =e= tissueBiomass(field,crop,'grain')*(1-grainDistro)*grainConvRate(crop)/2.47105;
       cellulosicEthanol_def_Corn(field,'corn').. cellulosicEthanol(field,'corn') =e= (1 - veg_retained('corn'))*tissueBiomass(field,'corn','veg')*(1-stoverDistro)*vegConvRate('corn')/2.47105;
       cellulosicEthanol_def_alfalfa(field,'alfalfa').. cellulosicEthanol(field,'alfalfa') =e= 0;
       cellulosicEthanol_def_Grass(field,'grass').. cellulosicEthanol(field,'grass') =e= (tissueBiomass(field,'grass','veg')*vegConvRate('grass')/2.47105)$(prevZ1(field,'grass') gt 0);

 AgPhaseEn_def(field)..
 AgPhaseEn(field) =e= (AgEnIn(field) + sum(crop, CropTrnsEn(field,crop)))*40;

   cropTrns_def(field,crop).. CropTrnsEn(field,crop) =e= (0.256/2.47015)*yield(field,crop)*(1$perr(crop) + 0.75$(not perr(crop)));
   AgEnIn_def(field).. AgEnIn(field) =e= farmEn(field) + fertEn(field) + PestProdEn + PestAppEn + sum(crop, TillEn*d_t(field,crop));
     farmEn_def(field).. farmEn(field) =e= PlantEn*sum(crop,d_est(field,crop)) + HarvestEn*(z1(field,'corn') + z1(field,'alfalfa') + d_cont(field,'grass'));
     fertEn_def(field).. fertEn(field) =e= sum(crop, fertProdEn(field,crop) + fertAppEn(field,crop));
       fertProdEn_def(field,crop).. fertProdEn(field,crop) =e= 60.04*(NitroEn(crop)*z1(field,crop) + ExtraNitroEn(crop)*d_f(field,crop));
       fertAppEn_def(field,crop).. fertAppEn(field,crop) =e= fertAppCost*(d_f(field,crop)$perr(crop) + z1(field,crop)$(not perr(crop)));


**************************************************************************
*********************** Environment Model ********************************
**************************************************************************

positive variables
SoilSubScore
WaterValue
BCISubScore
EmissionsSubScore
;

equation envScore_def "Definition of EnvScore";

envScore_def..
4*Score('env') =e= SoilSubScore + WaterValue + BCISubScore + EmissionsSubScore;

*           **********************************************
*           ************* Soil Sub-Model *****************
*           **********************************************

   scalars maxONAvg, minONAvg; maxONAvg = 1600; minONAvg = 1200;
   scalar maxOrgNit; maxOrgNit = (maxONAvg + minONAvg)/2;
   positive variable avgON;
   equation avgON_def, SoilSS_def;
   avgON_def..
   avgON =e= (1/card(field))*sum((field,crop), orgNit(field,crop) + (tissueBiomass(field,crop,'root')*root_retained(crop)*containedNit(crop,'root')) );
   SoilSS_def..
   SoilSubScore =e= (avgON - minONAvg) / (maxONAvg - minONAvg);

*           **********************************************
*           ************ Water Sub-Model *****************
*           **********************************************

   scalar maxNleached; maxNleached = 272/2.47105;
   equation WaterSS_def;
   WaterSS_def..
   WaterValue =e= 1 - (sum((field,crop), nit_leached(field,crop)) / (maxNleached * card(field)));

*           **********************************************
*           ************* BCI Sub-Model ******************
*           **********************************************

*    positive variable BCI(field) "BCI of each field on farm";
    equation BCI_def(field), BCIScore_def;
    BCI_def(field)..
    BCI(field) =e= (1/1.06)*(0.25 + (0.19)*z1(field,'grass') + (0.62)*PGrass);
    BCIScore_def..
    BCISubScore =e= (1/card(field))*sum(field, BCI(field));

*           **********************************************
*           ********** Emissions Sub-Model ***************
*           **********************************************

    scalars minEmiss /-1912/, maxEmiss /22000/;
    scalars plantCO2, harvCO2, fertCO2, tillCO2, sprayCO2;
    plantCO2 = 16.4; harvCO2 = 37.05; fertCO2 = 11.3; tillCO2 = 289.7; sprayCO2 = 10.16;
    scalar CNratio /50/;

    parameters
    celluloseFeedstock(crop) /corn 0.25, grass 1/
    cornGrainFeedstock(crop) /corn 0.5/;

    variables
    emissions
      farmProdEm(field)
        AgPhaseEm(field)
        RefPhaseEm(field,crop)
      farmSoilResp(field)
        amtRespired(field)
        amtSequestered(field)
    ;

    equations
    EmissionsSubScore_def
    emissions_def
      soilResp_def(field)
        Respired_def(field)
        Sequestered_def(field)
      prodEm_def(field)
        AgPhase_def(field)
        RefPhase_def(field,crop)
    ;

*   Equation Listing:
    EmissionsSubScore_def..
    EmissionsSubScore =e= 1 - ((emissions - minEmiss)/(maxEmiss-minEmiss));

    emissions_def..
    emissions =e= sum(field, farmSoilResp(field) + farmProdEm(field));
      soilResp_def(field).. farmSoilResp(field)*2.47105 =e= amtSequestered(field) - amtRespired(field);
        Respired_def(field).. amtRespired(field) =e= sum(crop, mineralizedNit_tilled(field,crop)+mineralizedNit_notTilled(field,crop))*CNratio;
        Sequestered_def(field).. amtSequestered(field) =e= sum(crop, nit_2_crop(field,crop))*CNratio;
      prodEm_def(field).. farmProdEm(field) =e= AgPhaseEm(field) + sum(crop, RefPhaseEm(field,crop));
        AgPhase_def(field).. AgPhaseEm(field) =e= sum(crop, plantCO2*d_est(field,crop) + harvCO2*(z1(field,'corn') + z1(field,'alfalfa') + d_cont(field,'grass')) + fertCO2*(fertWeight(crop)*z1(field,crop) + extraFertWeight(crop)*d_f(field,crop)) + tillCO2*d_t(field,crop) + sprayCO2);
        RefPhase_def(field,crop).. RefPhaseEm(field,crop) =e= (47/1000*celluloseFeedstock(crop)*((1-stoverDistro)$(ord(crop) eq 1) + 1$(ord(crop) eq 2)) + 419.57/1000*cornGrainFeedstock(crop)*(1-grainDistro))*yield(field,crop);

**************************************************************************
**************************************************************************
**************************************************************************

*Prevent any management decisions (UB of 0) if management is turned off.
if (z2_up gt 0,
       z2.up(field,mgnt) = z2_up;
else
       z2.fx(field,'fert') = 1;
       z2.up(field,'till') = z2_up;
);


model FoF /all/;

option lp = cplex;
option nlp = conopt;

*Set initial values for NLP solver
z1.l(field,crop) = 0.0001;
z1.l(field,'grass') = 1;
z2.l(field,mgnt) = 1;

parameter solveTime;
solveTime = 0;

*Use loops to look forward in time and decide if planting perrenials is worth
*the drawback of losing first year yield!

*First, save parameters to reset data with later:
parameters pz1(field,crop), pON(field), pRB(field), pC, pEc, pMc, pPg;
pz1(field,crop) = prevZ1(field, crop);
pON(field) = prevOrgNit(field);
pRB(field) = prevRootBio(field);
pC = capital_previous;
pEc = ecScore_previous;
pMc = maxCapital;
pPg = pGrass;


$if not set T $set T 4
set T /1*%T%/;
parameters z1_decisions(field,crop,T), z2_decisions(field,mgnt,T)
           fieldON(field,T), fieldRoots(field,T), prices(crop,T)
           scores(ST,T), overallScores(T);

set commodities /feed, fuel/;
alias(commodities, C);
parameters
basePrice(C) /feed 250, fuel 0.225/
demand(C) /feed 20000, fuel 10000/
supply(C)
cPrice(C)
cPrices(C,T), cPrices2(C,T)
supplies(C,T), supplies2(C,T)
;
demand(C) = demand(C)*2.47105;

* For first loop overtime ignoring perrenial effects.
loop(T,

  loop((field,crop), prevZ1(field,crop) = 1;);

  display prevOrgNit, prevRootBio;

  solve FoF using minlp max overallScore;
  z1_decisions(field,crop,T) = z1.l(field,crop);
  z2_decisions(field,mgnt,T) = z2.l(field,mgnt);

* Update all values that depend on last year decisions
  fieldON(field,T) = sum(crop, orgNit.l(field,crop));
  fieldRoots(field,T) = sum(crop, tissueBiomass.l(field,crop,'root'));
  prevZ1(field,crop) = z1.l(field,crop);
  prevOrgNit(field) = sum(crop, orgNit.l(field,crop));
  prevRootBio(field) = sum(crop, tissueBiomass.l(field,crop,'root'));
  capital_previous = capital.l;
  ecScore_previous = Score.l('Ec');
  maxCapital = (capital_previous/ecScore_previous)+maxIncrease;
  pGrass = 0;
  loop(field, pGrass = pGrass + 0.125$(prevZ1(field,'grass') gt 0));
  pGrass = pGrass + .125$(price('grass') > 50) + .125$(price('grass') > 100) + .125$(price('grass') > 125) + .125$(price('grass') > 150);

* Update economy as if only player
  prices(crop,T) = Price(crop);
  supply('feed') = sum(field, yield.l(field,'alfalfa') + yield.l(field,'corn')*(0.5*grainDistro + 0.25*stoverDistro)) + 5000*2.47105;
  cPrice('feed') = basePrice('feed') + basePrice('feed')*((demand('feed') - supply('feed'))/demand('feed'))*0.75;
  supply('fuel') = sum(field, (yield.l(field,'grass') + yield.l(field,'corn')*0.25*(1-stoverDistro))*0.38 + yield.l(field,'corn')*0.5*(1-grainDistro)*0.4) + 5000*2.47105;
  cPrice('fuel') = basePrice('fuel') + basePrice('fuel')*((demand('fuel') - supply('fuel'))/demand('fuel'))*1.25;
  supplies(C,T) = supply(C);
  cPrices(C,T) = cPrice(C);

  Price('corn') = (2/3)*(grainDistro*cPrice('feed') + (1-grainDistro)*0.4*1000*cPrice('fuel')) + (1/3)*(stoverDistro*cPrice('feed') + (1-stoverDistro)*0.38*1000*cPrice('fuel'));
  Price('grass') = 0.38*1000*cPrice('fuel');
  Price('alfalfa') = cPrice('feed');

* Keep track of scores
  scores(ST,T) = score.l(ST);
  overallScores(T) = overallScore.l;

  solveTime = solveTime + FoF.etSolve;
);


**************************************************************************
*Reset all values!

Price(crop) = prices(crop,'1');
prevZ1(field, crop) = pz1(field,crop);
prevOrgNit(field) = pON(field);
prevRootBio(field) = pRB(field);
capital_previous = pC;
ecScore_previous = pEc;
maxCapital = pMc;
pGrass = pPg;

**************************************************************************


parameters z1_decisions2(field,crop,T), z2_decisions2(field,mgnt,T)
           fieldON2(field,T), fieldRoots2(field,T), prices2(crop,T)
           scores2(ST,T), overallScores2(T);

* For second loop, take perrenial effects into account
loop(T,

  solve FoF using minlp max overallScore;
  z1_decisions2(field,crop,T) = z1.l(field,crop);
  z2_decisions2(field,mgnt,T) = z2.l(field,mgnt);

* Update all values that depend on last year decisions
  fieldON2(field,T) = sum(crop, orgNit.l(field,crop));
  fieldRoots2(field,T) = sum(crop, tissueBiomass.l(field,crop,'root'));
  prevZ1(field,crop) = z1.l(field,crop);
  prevOrgNit(field) = sum(crop, orgNit.l(field,crop));
  prevRootBio(field) = sum(crop, tissueBiomass.l(field,crop,'root'));
  capital_previous = capital.l;
  ecScore_previous = Score.l('Ec');
  maxCapital = (capital_previous/ecScore_previous)+maxIncrease;
  pGrass = 0;
  loop(field, pGrass = pGrass + 0.125$(prevZ1(field,'grass') gt 0));
  pGrass = pGrass + .125$(price('grass') > 50) + .125$(price('grass') > 100) + .125$(price('grass') > 125) + .125$(price('grass') > 150);

* Update economy as if only player
  prices2(crop,T) = Price(crop);
  supply('feed') = sum(field, yield.l(field,'alfalfa') + yield.l(field,'corn')*(0.5*grainDistro + 0.25*stoverDistro)) + 5000*2.47105;
  cPrice('feed') = basePrice('feed') + basePrice('feed')*((demand('feed') - supply('feed'))/demand('feed'))*0.75;
  supply('fuel') = sum(field, (yield.l(field,'grass') + yield.l(field,'corn')*0.25*(1-stoverDistro))*0.38 + yield.l(field,'corn')*0.5*(1-grainDistro)*0.4) + 5000*2.47105;
  cPrice('fuel') = basePrice('fuel') + basePrice('fuel')*((demand('fuel') - supply('fuel'))/demand('fuel'))*1.25;
  supplies2(C,T) = supply(C);
  cPrices2(C,T) = cPrice(C);

  Price('corn') = (2/3)*(grainDistro*cPrice('feed') + (1-grainDistro)*0.4*1000*cPrice('fuel')) + (1/3)*(stoverDistro*cPrice('feed') + (1-stoverDistro)*0.38*1000*cPrice('fuel'));
  Price('grass') = 0.38*1000*cPrice('fuel');
  Price('alfalfa') = cPrice('feed');

* Keep track of scores
  scores2(ST,T) = score.l(ST);
  overallScores2(T) = overallScore.l;

  solveTime = solveTime + FoF.etSolve;
);


*Compare overall results of two loops:
parameter scoreTotal, scoreTotal2,
          totalON, totalON2, maxON1, maxON2, maxON,
          totalR, totalR2, maxR1, maxR2, maxR,
          overall, overall2;
scoreTotal = sum(T, overallScores(T)); scoreTotal2 = sum(T, overallScores2(T));
maxON1 = smax(T, sum(field, fieldON(field,T)));
maxON2 = smax(T, sum(field, fieldON2(field,T)));
maxON = max(maxON1,maxON2);
totalON = sum(T, sum(field,fieldON(field,T))/maxON);
totalON2 = sum(T, sum(field,fieldON2(field,T))/maxON);
maxR1 = smax(T, sum(field, fieldRoots(field,T)));
maxR2 = smax(T, sum(field, fieldRoots2(field,T)));
maxR = max(maxR1,maxR2);
totalR = sum(T, sum(field,fieldRoots(field,T))/maxON);
totalR2 = sum(T, sum(field,fieldRoots2(field,T))/maxON);

*Find a weighted average of two cases, and penalize first case as it ignores perrenial effects.
overall = (1 - 1/(3.5*card(T)))*(0.7*scoreTotal/max(scoreTotal,scoreTotal2) + 0.15*totalON/max(totalON,totalON2) + 0.15*totalR/max(totalR,totalR2));
overall2 = (0.7*scoreTotal2/max(scoreTotal,scoreTotal2) + 0.15*totalON2/max(totalON,totalON2) + 0.15*totalR2/max(totalR,totalR2));

*Set planting decision to the first year decision made by the greater of the two cases:
if(overall gt overall2,
   z1.l(field,crop) = z1_decisions(field,crop,'1');
   z2.l(field,mgnt) = z2_decisions(field,mgnt,'1');
else
   z1.l(field,crop) = z1_decisions2(field,crop,'1');
   z2.l(field,mgnt) = z2_decisions2(field,mgnt,'1');
);


display overall, overall2, z1.l, z2.l;
display scoreTotal, scoreTotal2, totalON, totalON2, totalR, totalR2;


Display z1_decisions, z2_decisions, overallScores, scores, fieldON, fieldRoots, supplies, cPrices, prices;
Display z1_decisions2, z2_decisions2, overallScores2, scores2, fieldON2, fieldRoots2, supplies2, cPrices2, prices2;

Display solveTime;

