
module TestMessages
  BASICMESSAGE = {
    "clientID" => "0"
  }

  class ExpectReplies
    include Serializable

    attr_reader :expected

    def initialize(expected)
      @expected = expected
    end
  end

  class TestMessage
    include Serializable

    attr_reader :event, :handler, :expected_replies

    def initialize(message, handler, expected_replies)
      @event = EventMessage.new(message.to_json)
      @handler = handler
      @expected_replies = expected_replies
    end
  end

  class GenericMessage
    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["clientID"] = "0"
      message["event"] = "no event"
      message = message.merge(options)
      @test_message =  TestMessage.new(message, handler, expected_replies)
    end
  end

  class ValidateRoomMessage
    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["clientID"] = BASICMESSAGE["clientID"]
      message["roomName"] = "noName"
      message["event"] = "validateRoom"
      message = message.merge(options)
      @test_message =  TestMessage.new(message, handler, expected_replies)
    end
  end

  class CreateRoomMessage

    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["clientID"] = "0"
      message["roomName"] = "noName"
      message["soilVariation"] = true
      message["password"] = ""
      message["event"] = "createRoom"
      message["playerCount"] = 8
      message["fieldCount"] = 4
      message = message.merge(options)
      @test_message = TestMessage.new(message, handler, expected_replies)
    end
  end

  class ValidateUserMessage

    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["clientID"] = "0"
      message["roomName"] = "noName"

      message["password"] = ""
      message["event"] = "validateUserName"
      message = message.merge(options)
      @test_message = TestMessage.new(message, handler, expected_replies)
    end
  end

  class JoinGameMessage

    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=4)
      message = Hash.new
      message["clientID"] = "0"
      message["roomName"] = "noName"
      message["password"] = ""
      message["userName"] = "noNameUser"
      message["event"] = "joinRoom"
      message = message.merge(options)
      @test_message = TestMessage.new(message, handler, expected_replies)
    end
  end

  class LoadFieldsMessage

    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["event"] = "loadFromServer"
      message = message.merge(options)
      @test_message = TestMessage.new(message, handler, expected_replies)
    end
  end

  class PlantMessage

    include Serializable

    attr_reader :test_message

    def initialize(handler, options={}, expected_replies=1)
      message = Hash.new
      message["event"] = "plantField"
      message["field"] = 0
      message["crop"] = "corn"
      message = message.merge(options)
      @test_message = TestMessage.new(message, handler, expected_replies)
    end
  end

end