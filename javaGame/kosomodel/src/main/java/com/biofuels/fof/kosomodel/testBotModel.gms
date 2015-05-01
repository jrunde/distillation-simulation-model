*$Title Fields of Fuel
$offdigit

option optca=0, optcr=0;

Sets
        crop   'crop'         /corn, grass, alfalfa/
        field  'fields'
        mgnt   'management'   /till, fert/
        type   'Fuel Stock'   /cell, grain/
;


Parameters
        beta0(crop) /corn 0.1164, grass -0.9556, alfalfa -0.08196/
        beta1(crop) /corn 2.8849, grass 2.4093, alfalfa 2.40141/
        beta2(crop) /corn 1.1, grass 1.1/
        planting_cost(crop) /corn 1001.06, grass 605.69, alfalfa 346.29/
        soc_param(crop) /corn -0.2, grass 0.17, alfalfa 0.25/
        soc_previous(field)
        capital_previous
        ec_score_previous
        price(crop) 'price per ton. Based on supply&demand'
        ec_weight
        env_weight
        en_weight
				z2_up
        ;


Table  comp(crop, type) ' '
            cell    grain
corn        0.25     0.5
grass        1       0
alfalfa      0       0
;

Table mgnt_cost(crop, mgnt)
             till      fert
corn         44.48    127.97
grass         0        71.66
alfalfa       0         0
;

Scalar
        Emax    'max energy per field'    /226088/
        Socmax  'max SOC'                 /190/
        a       'cellulosic conversion rate per kg  [MJ/kg]'  /17700/
        b       'grain conversion rate per kg [MJ/kg]'        /18800/
;

$if not set dbIn $abort 'no file name for in-database 1 file provided'
$gdxin %dbIn%
$load field, soc_previous, price, capital_previous, ec_score_previous
$load ec_weight, env_weight, en_weight, z2_up
$gdxin


Variables
        env_score       'environmental impact score'
        c_score         'carbon score'
        p_score         'phosphorus runoff score'
        p_local         'used to compute p_score'
        r(field)        'rate of change in soil organic carbon'
        delta(field)    'amount of change in soil organic carbon from t to t+1'
        ec_score        'economic score'
        en_score        'energy score'
        objective       'objective'
        yield(field)    'kg of crop produced per field'
        soc_current(field)     'soil organic carbon'
        cost(field)            'Cost field (dependent on crop planted)'
        profit(field)          ' (Price*yield - cost)  '
        total_net_income       'sum across all fields of profit'
        capital_current        'total net income'
        energy_produced(field) 'energy produced on each field'
        previous_max_capital   'max capital from previous round'
;

Binary variables
        z1(crop, field)
        z2(mgnt, field)
;

Positive variable yield(field), energy_produced(field), cost(field),
                  en_score, env_score, c_score, p_score, p_local, soc_current;

Equations
        soc_def(field)          'definition of SOC'
        r_def(field)            'def of r'
        delta_def(field)        'def of delta'
        c_score_def             'definition of c_score'
        p_local_def             'definition of p_local'
        p_score_def             'definition of p_score'
        env_score_def           'definition of environmental impact score'
        crop_constraint(field)  'only one crop per field'
        objective_eqn
        yield_def(field)         'definition of biomass yield'
        energy_def(field)        'energy produced on each field'
        en_score_def             'definition of energy score'
        cost_def(field)          'definition of planting costs per field'
        profit_def(field)        'definition of net profit per field'
        total_income_def         'definition of total net income per round'
        capital_def              'definition of new capital'
        max_capital_def          'definition of previous max capital'
        ec_score_def             'definition of economic score'
;

soc_def(field)   ..  soc_current(field) =e= soc_previous(field) + delta(field);
r_def(field)     ..  r(field) =e= 1 + (sum(crop, soc_param(crop) * z1(crop,field))) + 0.1*(1 - z2('till',field)) - 0.2*z2('fert',field);
delta_def(field) ..  delta(field) =e= ((r(field)-1) * (1- (soc_previous(field) / Socmax)))*soc_previous(field);
p_local_def      ..  p_local =e= 10**(( 0.79*(sum(field, z1('corn',field))/ card(field))
                                       * (1-0.2*(1-(sum(field,z2('till',field))/card(field))))) - 1.44);

c_score_def      ..  c_score =e= sum(field, soc_current(field))/card(field)/Socmax;
p_score_def      ..  p_score =e= 1-((p_local - 0.0363)/0.1876);
env_score_def    ..  env_score =e= 0.5*c_score + 0.5*p_score;

yield_def(field) ..  yield(field) =e= sum(crop, z1(crop,field)*(beta0(crop) + beta1(crop)*Log(soc_previous(field)) * beta2(crop) * z2('fert', field)));

energy_def(field)..  energy_produced(field) =e= z1('corn',field)*yield(field)*(.5*18800 + .25*17700)
                                                       + z1('grass',field)*yield(field)*(17700);
en_score_def     ..  en_score =e= sum(field, energy_produced(field)) / (Emax * card(field));

cost_def(field)  ..  cost(field) =e= sum(crop, z1(crop,field)*planting_cost(crop) + sum(mgnt, z2(mgnt,field)*mgnt_cost(crop,mgnt)));
profit_def(field)..  profit(field) =e= sum(crop, z1(crop,field)*price(crop)*yield(field)) - cost(field);
total_income_def ..  total_net_income =e= sum(field, profit(field));
capital_def      ..  capital_current =e= capital_previous + total_net_income;
max_capital_def  ..  previous_max_capital * ec_score_previous =e= 1.3 * capital_previous;
ec_score_def     ..  ec_score * previous_max_capital =e= capital_current;

crop_constraint(field).. sum(crop, z1(crop,field)) =e= 1;

objective_eqn    .. objective =e= ec_weight*ec_score + env_weight*env_score + en_weight*en_score;

z2.up(mgnt,field) = z2_up;

model fof equations / all /;

previous_max_capital.l = capital_previous;
*option mip = cplex;
*option nlp = lindoglobal;
*option minlp = lindoglobal;
solve fof max objective use minlp;

Display z1.l, z2.l, ec_score.l, en_score.l, env_score.l, objective.l;

parameter solveTime;
solveTime = fof.etSolve;

