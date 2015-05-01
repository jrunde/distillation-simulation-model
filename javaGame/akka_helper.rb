require 'java'
module AkkaHelper
  class Base < UntypedActor
    def self.create(*args)
      self.new(*args)
    end

    def self.build(*args)
      return Akka::UntypedActor.actorOf(self)  if args.empty?
      Akka::UntypedActor.actorOf { self.new *args }
    end

    def self.spawn(*args)
      build(*args).start
    end
  end

  class ConnectMessage
    include Serializable

    attr_reader :uri

    def initialize(uri)
      @uri = uri
    end
  end

  # class EventMessage
  #   include Serializable

  #   attr_reader :event

  #   def initialize(event)
  #     @event = event
  #   end
  # end

  class ServerListener < Base
    def onReceive(msg)
      if msg.is_a? ConnectMessage
        @red = Redis.new
        @chan = msg.uri

        # if msg.uri
        #   uri = msg.uri
        #   @red = Redis.new(:host => uri.host, :port => uri.port, :password => uri.password)
        # else
        #   puts "connecting to local redis"
        # end
      elsif msg.is_a? EventMessage
        if @red
          # puts "writing #{msg.message} to pipe"
          # @red.lpush("fromJava",msg.message)
            @red.publish(:"toRuby#{@chan}",msg.message)
        else
          puts "Redis sever not specified, trying to reconnect"
          @uri = URI.parse("redis://redistogo:1f736fa2a27319dc45b7ebb470e04bbe@dory.redistogo.com:10177/")
          @red = Redis.new(:host => @uri.host, :port => @uri.port, :password => @uri.password)
        end
      else
      end
    end
  end
end