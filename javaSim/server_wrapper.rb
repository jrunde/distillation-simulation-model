scriptloc = File.expand_path(File.dirname(__FILE__))
$CLASSPATH << "#{scriptloc}"
$CLASSPATH << "#{scriptloc}/json-simple-1.1.1.jar"
$CLASSPATH << "#{scriptloc}/akka-actor_2.10-2.1.1.jar"
$CLASSPATH << "#{scriptloc}/kosomodel/target/kosomodel-0.0.1-SNAPSHOT"

require 'java'
require 'json'
require 'redis'
require 'uri'
require 'json-simple-1.1.1.jar'
require 'kosomodel/target/kosomodel-0.0.1-SNAPSHOT.jar'
require 'akka_helper'

require 'childprocess'
require_relative '../bot/bot_master.rb'

java_import "com.biofuels.fof.kosomodel.Handler"
java_import "com.biofuels.fof.kosomodel.EventMessage"
java_import "com.biofuels.fof.kosomodel.ActorSystemHelper"


include AkkaHelper

class ServerWrapper

  def open_pipes(mode)
    scriptloc = File.expand_path(File.dirname(__FILE__))
    if mode=="pipe"
      puts 'server opening pipes'
      puts @rpipe = open(File.join(scriptloc, "../pipes/rubypipe"),'r+')
      puts @wpipe = open(File.join(scriptloc, "../pipes/javapipe"),'w+')
    else
      puts 'connecting to redis server'
      @red = Redis.new ## DEVELOPMENT
      # end
    end
    # puts "connected to redis #{@red}"
    # @event_handler = EventHandler.new
  end

  def write_pipe(msg)
    @wpipe.puts(msg)
    @wpipe.flush
  end

  def read_pipe
    @rpipe.gets
  end

  def watch(mode)
    open_pipes(mode)
    loop do
      # puts str
      #   write_pipe(msg)

      if mode=="pipe"
        str = read_pipe
      else
        str = read_queue
      end
      puts "handling #{str}"
      handle_event(str)

      @handler.tell(EventMessage.new(str))

    end
  end

  def handle_event(string)
    js = JSON.parse(string)
    if js["event"] == "newBot"
      #start_bot(js)
    end
  end

  def start_bot(json)
    @bot_master ||= FoFBot::BotMaster.new
    puts "starting bot "
    puts "for #{json["roomID"]}"
    @bot_master.start(json["roomID"], json["args"])
  end

  def write_queue(msg)
    @red.lpush("fromJava",msg)
  end

  def publish(msg)
    @red.publish('rubyonrails',msg)
  end

  def read_queue
    ret = nil
    until ret
      begin
        ret = @red.brpop("toJava#{@redis_channel}")
      rescue
        puts "couldn't connect to redis, retrying"

        @red = Redis.new
      end
    end
    ret[1] #
  end

  def sleepmode
    while (!(poppy = @red.bpop))
      sleep (60)
    end

  end

  def shutdown
    (@bot_master ||= FoFBot::BotMaster.new).stop_all()
    @system.shutdown()
    @system.await_termination
  end

  def do_akka(redis_channel)

    @redis_channel = redis_channel
    jside = ActorSystemHelper.new
    @system = ActorSystem.create("Biofuels")


    trap(:INT) do
      puts "akka runner interrupted"
      shutdown()
      exit!
    end
    at_exit do
      puts "akka runner shutting down"
      if $!
        puts "error! #{$!}"
      else
        puts "Shutting down #{@system}"
      end
    end

    @handler = jside.makenew(@system, Handler, "handler")  # = system.actorOf(pro, "counter")
    @listener = @system.actorOf(Props.new(ServerListener), "listener")
    puts @listener
    @listener.tell(ConnectMessage.new(redis_channel))

    @handler.tell(@listener)
    watch('redis')
  end
end
