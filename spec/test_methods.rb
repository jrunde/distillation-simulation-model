module TestMethods
  def askActor(message_type, expect_count=1, options=@template)
    message = message_type.new(@handler, options, expect_count)
    # puts "asking #{message.test_message.event.message}"
    future = Patterns.ask(@listener, message, @timeout)
    if expect_count != 0
      result = Await.result(future, @timeout.duration())
      if result.is_a? Array
        result.map! { |r| JSON.parse(r) }
      else
        result = JSON.parse(result)
      end
    else
      result = "No result asked for!"
    end
    result
  end

  def advanceYear(years=1, stages=2)
    @template["event"] = "advanceStage"
    years.times do
      stages.times do
        reply = askActor(GenericMessage, 1)
      end
    end
  end

  def reset_template!
    @template = {
      "roomName" => "noNameRoom",
      "clientID" => "0",
      "password" => "",
      "deviseName" => "fake@fake.com",
      "userName" => "Joe Farmer",
      "roomID" => "noNameRoom"
    }
  end

  def first_year(crop="corn")
    askActor(JoinGameMessage)["result"]

    reset_template!
    @template["crop"] = crop
    askActor(PlantMessage, 0)

    reset_template!
    @template["event"] = "advanceStage"
    askActor(GenericMessage, "advanceStage")
    askActor(GenericMessage, "advanceStage")

    # askActor(JoinGameMessage)["result"].should == true

    # reset_template!
    # @template["crop"] = "corn"
    # askActor(PlantMessage, 0)

    # reset_template!
    # @template["event"] = "advanceStage"
    # askActor(GenericMessage, "advanceStage")
    # askActor(GenericMessage, "advanceStage")
  end

end