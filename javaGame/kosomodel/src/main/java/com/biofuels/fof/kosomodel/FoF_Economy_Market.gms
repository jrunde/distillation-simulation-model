sets
product /feed, fuel/
crop /Corn, CornGrain, CornStover, Grass, Alfalfa/
feedCrop(crop) /CornGrain, CornStover, Alfalfa/
fuelCrop(crop) /CornGrain, CornStover, Grass/
cornParts(crop) /CornGrain, CornStover/
;

parameters
Yield(crop)
Demand(product)
prevSupply(product)
ConversionRate(fuelCrop)

CornPerc(cornParts) /CornGrain 0.5, CornStover 0.25/
share(feedCrop) /CornGrain 0.4, CornStover 0.2, Alfalfa 0.4/
;

scalar
S "Elasticity of substitution: 0 -> Complementary, inf -> perfect substitution" /2.5/
R "R = (s-1)/s";
R = (S-1)/S;

$if not set dbIn $abort 'no file name for in-database 1 file provided'
$gdxin %dbIn%
$load Yield, Demand, prevSupply, ConversionRate
$gdxin

Yield(cornParts) = Yield('corn')*CornPerc(cornParts);

variables
Y "Total normalized excess demand"
positive variables
alpha(crop) "percent of crop used to produce feed"
rawFeed(crop) "amount feed supplied from each crop"
supply(product) "Amount of each product produced"
;

equations
obj_def "Definition of excess demand"
rawFeed_def(crop)
fuelSupply_def
feedCES_def
;

obj_def..
Y =e= sum(product, sqr((demand(product) - (supply(product) + prevSupply(product)))/demand(product)));

rawFeed_def(feedCrop)..
0 =e= (rawFeed(feedCrop) - alpha(feedCrop)*yield(feedCrop)) / (demand('feed')*share(feedCrop));

fuelSupply_def..
0 =e= (supply('fuel') - sum(fuelCrop, (1-alpha(fuelCrop))*conversionRate(fuelCrop)*yield(fuelCrop)))/demand('fuel');

feedCES_def..
0 =e= (supply('feed') - sum(feedCrop, share(feedCrop)**(1/s) * rawFeed(feedCrop)**(R))**(1/R))/demand('feed');


alpha.up(crop) = 1;
alpha.lo('alfalfa') = 1;
alpha.up('grass') = 0;

alpha.lo('corngrain') = 0.01;
alpha.lo('cornstover') = 0.01;


option optcr = 0;
option nlp = conopt;

model market /all/;
solve market min Y using nlp;
