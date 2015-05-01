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

  it "creates and talk to actor" do
    future = Patterns.ask(@listener, "hello?", @timeout);
    result = Await.result(future, @timeout.duration());
    result.should == "hihi"
    # should be "hello"
  end

  it "asks if a room is open" do
    @template["roomName"] = "new Room"
    askActor(ValidateRoomMessage)["result"].should == true
  end

  it "creates room" do
    @template["roomName"] = "new Room"
    askActor(CreateRoomMessage)["result"].should == true
  end

  it "joins created games" do
    askActor(JoinGameMessage)["result"].should == true
  end

  it "gets field location" do
    askActor(JoinGameMessage)["result"].should == true
    askActor(LoadFieldsMessage)["fields"][0]["x"].should_not be nil
    askActor(LoadFieldsMessage)["fields"][0]["y"].should_not be nil
  end

  it "has different locations for different fields" do
    askActor(JoinGameMessage)["result"].should == true
    fields = askActor(LoadFieldsMessage)["fields"]
    f1 = fields[0]
    f2 = fields[1]
    (f1["x"] == f2["x"] and f1["y"] == f2["y"]).should == false
  end

  it "has different locations for different farmers" do
    askActor(JoinGameMessage)["result"].should == true
    fields1 = askActor(LoadFieldsMessage)["fields"]

    @template["clientID"] = "1"
    @template["userName"] = "Bo Farmer"
    askActor(JoinGameMessage)["result"].should == true
    fields2 = askActor(LoadFieldsMessage)["fields"]
    f1 = fields1[0]
    f2 = fields2[0]
    (f1["x"] == f2["x"] and f1["y"] == f2["y"]).should == false

    f1["x"].to_i.should == 1
    f1["y"].to_i.should == 2

    fields1[1]["x"].to_i.should == 2
    fields1[1]["y"].to_i.should == 2

    @template["clientID"] = "2"
    @template["userName"] = "Go Farmer"
    askActor(JoinGameMessage)["result"].should == true

    @template["clientID"] = "3"
    @template["userName"] = "Zoe Farmer"
    askActor(JoinGameMessage)["result"].should == true
    fields4 = askActor(LoadFieldsMessage)["fields"]

    fields4[2]["x"].to_i.should == 3
    fields4[2]["y"].to_i.should == 3

    fields4[3]["x"].to_i.should == 4
    fields4[3]["y"].to_i.should == 3
  end

  it "has different starting SOC values for each field" do
    askActor(JoinGameMessage)["result"].should == true
    fields = askActor(LoadFieldsMessage)["fields"]

    fields[0]["SOM"].should_not == fields[1]["SOM"]
  end

  it "has Grassland Bird Index per field" do
    askActor(JoinGameMessage)["result"].should == true

    @template["event"] = "advanceStage"
    askActor(GenericMessage, 3)

    reset_template!

    askActor(LoadFieldsMessage,1)

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, 8)

    farm = repl.select{|evt| evt["event"] == "getFarmInfo"}

    histories = repl.select{|evt| evt["event"] == "getLatestFieldHistory"}

    histories.first["fields"].first["GBI"].should > 0;
  end

  it "changes GBI from year to year" do
    askActor(JoinGameMessage)["result"].should == true

    @template["event"] = "advanceStage"
    askActor(GenericMessage, 3)

    reset_template!

    askActor(LoadFieldsMessage,1)

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, 8)

    reset_template!
    @template["crop"] = "grass"
    askActor(PlantMessage, 0)

    reset_template!
    @template["event"] = "advanceStage"
    askActor(GenericMessage, 3)

    reset_template!

    askActor(LoadFieldsMessage,1)

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, 8)

    farm = repl.select{|evt| evt["event"] == "getFarmInfo"}

    histories = repl.select{|evt| evt["event"] == "getLatestFieldHistory"}

    histories.first["fields"].first["GBI"].should < histories.last["fields"].first["GBI"]

  end

  it "creates a map of the game using ruby-mapnik" do
    askActor(JoinGameMessage)["result"].should == true
    field_locs = askActor(LoadFieldsMessage)["fields"].map{|f| [f["x"],f["y"]]}

    @template["event"] = "advanceStage"
    askActor(GenericMessage, 3)

    reset_template!

    askActor(LoadFieldsMessage,1)

    @template["event"] = "advanceStage"
    askActor(GenericMessage, 8)

    reset_template!
    @template["crop"] = "grass"
    askActor(PlantMessage, 0)

    reset_template!
    @template["event"] = "advanceStage"
    askActor(GenericMessage, 3)

    reset_template!

    askActor(LoadFieldsMessage,1)

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, 8)

    farm = repl.select{|evt| evt["event"] == "getFarmInfo"}

    histories = repl.select{|evt| evt["event"] == "getLatestFieldHistory"}

    gbis = histories.last["fields"].map{|f| f["GBI"]}


  end

  it "can also wait for a specific message" do
    askActor(JoinGameMessage)["result"].should == true
    field_locs = askActor(LoadFieldsMessage)["fields"].map{|f| [f["x"],f["y"]]}

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, "advanceStage")

    repl.select{|r| r["event"] == "advanceStage"}.first.should_not be nil
  end

  it "receives world dump at year end" do
    askActor(JoinGameMessage)["result"].should == true
    field_locs = askActor(LoadFieldsMessage)["fields"].map{|f| [f["x"],f["y"]]}

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    field_dump = repl.select{|r| r["event"] == "fieldDump"}.first
    field_dump.should_not be nil

    field_dump
    field_dump["fields"].first["x"].should > -1
    field_dump["fields"].first["y"].should > -1
  end

  it "can output fields for map making" do

    @template["clientID"] = "1"
    @template["userName"] = "Bo Farmer"
    askActor(JoinGameMessage)["result"].should == true

    @template["clientID"] = "2"
    @template["userName"] = "Go Farmer"
    askActor(JoinGameMessage)["result"].should == true

    reset_template!
    @template["clientID"] = "2"
    @template["crop"] = "grass"
    askActor(PlantMessage, 0)

    reset_template!
    @template["clientID"] = "4"
    @template["userName"] = "Mo Farmer"
    askActor(JoinGameMessage)["result"].should == true

    reset_template!
    @template["clientID"] = "4"
    @template["crop"] = "corn"
    askActor(PlantMessage, 0)

    reset_template!
    @template["clientID"] = "3"
    @template["userName"] = "Zoe Farmer"
    askActor(JoinGameMessage)["result"].should == true

    @template["event"] = "advanceStage"
    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    repl = askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")

    field_dump = repl.select{|r| r["event"] == "fieldDump"}.first

    # puts field_dump["fields"]

    open('mapnik/test.json','w'){|f| f.write field_dump.to_json}

  end

  it "can kick players" do
    @template["clientID"] = "1"
    @template["userName"] = "Bo Farmer"
    askActor(JoinGameMessage)["result"].should == true

    @template["clientID"] = "2"
    @template["userName"] = "Go Farmer"
    askActor(JoinGameMessage)["result"].should == true

    reset_template!
    @template["event"] = "kickPlayer"
    @template["player"] = "Go Farmer"
    askActor(GenericMessage)["result"].should == true

    askActor(GenericMessage)["result"].should == false

  end

  it "has global sustainability scores" do
    @template["clientID"] = "1"
    @template["userName"] = "Bo Farmer"
    askActor(JoinGameMessage)["result"].should == true

    @template["clientID"] = "2"
    @template["userName"] = "Go Farmer"
    askActor(JoinGameMessage)["result"].should == true

    reset_template!
    @template["clientID"] = "2"
    @template["crop"] = "corn"
    askActor(PlantMessage, 0)

    reset_template!
    @template["event"] = "advanceStage"
    askActor(GenericMessage, "advanceStage")
    repl = askActor(GenericMessage, "advanceStage")
    global = repl.select{|evt| evt["event"] == "globalInfo"}.first

    global["globalSustainability"].should > 0
    global["globalEnvironment"].should > 0
    global["globalEconomy"].should > 0
    global["globalEnergy"].should > 0

    global["globalGBI"].should > 0
  end

  it "disables initial soil variation" do
    @template["soilVariation"] = false
    @template["roomName"] = "aNewRoom"
    askActor(CreateRoomMessage)["result"].should == true

    reset_template!
    @template["roomName"] = "aNewRoom"
    @template["roomID"] = "aNewRoom"
    askActor(JoinGameMessage, "togglePause")

    reset_template!
    @template["event"] = "loadFromServer"
    @template["roomName"] = "aNewRoom"
    repl = askActor(GenericMessage, 1)

    repl["fields"].first["SOM"].should == repl["fields"].last["SOM"]

    # puts repl
  end

  it "adds bot" do
    @template["event"] = "newBot"
    repl = askActor(GenericMessage, 0)
    # first_year()
  end
end