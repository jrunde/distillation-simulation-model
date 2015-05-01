require_relative 'model_helper.rb'
require_relative 'test_methods.rb'

describe ModelWrapper do

  include TestMethods

  before(:all) do
    @system = ActorSystem.create("BiofuelsTest")
    @listener = @system.actorOf(Props.new(ModelWrapper), "listener")
  end

  after(:all) do
    @system.shutdown
    @system.await_termination
  end

  before(:each) do
    @timeout = Timeout.new(Duration.create(3, TimeUnit::SECONDS));
    jside = ActorSystemHelper.new
    @handler = jside.makenew(@system, Handler, "handler")  # = system.actorOf(pro, "counter")
    @handler.tell(@listener)
    @template = {
      "roomName" => "noNameRoom",
      "clientID" => "0",
      "password" => "",
      "deviseName" => "fake@fake.com",
      "userName" => "Joe Farmer",
      "roomID" => "noNameRoom"
    }
    askActor(CreateRoomMessage)["result"].should == true

  end

  after(:each) do
    stopped = Patterns.gracefulStop(@handler, Duration.create(5, TimeUnit::SECONDS), @system);
    result = Await.result(stopped, Duration.create(6, TimeUnit::SECONDS));
  end


  # it "sends alfalfa crop price" do
  #   repl = first_year()
  #   global = repl.select{|evt| evt["event"] == "globalInfo"}.first
  #   global["alfalfaPrice"].to_i.should > 0
  # end

  it "has a farm level water score" do
    first_year()
    reset_template!
    @template["event"] = "getLatestFarmerHistory"
    repl = askActor(GenericMessage, 1)
    repl["yearInfo"]["farmWater"].should > 0
  end

  it "has a farm level Emissions score" do
    first_year()
    reset_template!
    @template["event"] = "getLatestFarmerHistory"
    repl = askActor(GenericMessage, 1)
    repl["yearInfo"]["emissions"].should_not == 0
  end

  it "has a farm level n2o score" do
    first_year()
    reset_template!
    @template["event"] = "getLatestFarmerHistory"
    repl = askActor(GenericMessage, 1)
    repl["yearInfo"]["n2o"].should_not == 0
  end

  it "has a farm level Soil Respiration score" do
    reset_template!
    first_year("grass")

    reset_template!
    @template["event"] = "getLatestFarmerHistory"

    repl = askActor(GenericMessage, 1)
    repl["yearInfo"]["soilRespiration"].should_not == 0
  end
end
