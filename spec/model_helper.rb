require 'rspec'
require 'json'
require 'rspec/autorun'

scriptloc = File.expand_path(File.dirname(__FILE__))
$CLASSPATH << "#{scriptloc}/../*"
$CLASSPATH << "#{scriptloc}/../akka/*"
$CLASSPATH << "#{scriptloc}/../javaGame/kosomodel/target/kosomodel-0.0.1-SNAPSHOT"
$CLASSPATH << "#{scriptloc}/../javaGame/json-simple-1.1.1.jar"

require 'java'
require "#{scriptloc}/../scala-library.jar"
require "#{scriptloc}/../akka/config-1.0.0.jar"
require "#{scriptloc}/../akka/akka-actor_2.10-2.1.1.jar"
require "#{scriptloc}/../javaGame/kosomodel/target/kosomodel-0.0.1-SNAPSHOT.jar"
require "#{scriptloc}/../javaGame/json-simple-1.1.1.jar"

# require 'akka-project-in-java/src/main/java/akka/test/AkkaProjectInJava'
# require 'JavaActor.jar'

java_import 'java.io.Serializable'
java_import 'akka.actor.UntypedActor'
java_import 'akka.actor.ActorRef'
java_import 'akka.actor.ActorSystem'
java_import 'akka.actor.Props'
java_import 'scala.concurrent.Future'
java_import 'scala.concurrent.Future'
java_import 'scala.concurrent.Await'
java_import 'akka.util.Timeout'
java_import 'scala.concurrent.duration.Duration'
java_import 'akka.pattern.Patterns'
java_import 'java.util.concurrent.TimeUnit'

java_import "com.biofuels.fof.kosomodel.Handler"
java_import "com.biofuels.fof.kosomodel.EventMessage"
java_import "com.biofuels.fof.kosomodel.ActorSystemHelper"

require 'test_messages'

include TestMessages

class BaseActor < UntypedActor
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

class ModelWrapper < BaseActor
  def onReceive(msg)
    if msg.is_a? String
      # puts "got a string: #{msg}"
      if msg == "hello?"
        getSender().tell("hihi")
      end
    elsif msg.is_a? TestMessage
      @replyaddr = getSender()
      @replies = []
      @expected_replies = msg.test_message.expected_replies
      # @timeout = Timeout.new(Duration.create(15, TimeUnit::SECONDS));
      msg.handler.tell(msg.event)
    elsif msg.is_a? EventMessage
      if @expected_replies.is_a? Fixnum
        if @replies.count < @expected_replies
          @replies << msg.message
        elsif @replies.count > @expected_replies
          puts "unexpected number of replies"
          raise "unexpected number of replies" unless @expected_replies == -1
        end

        if @replies.count == @expected_replies
          if @expected_replies == 1
            ## making this/these here/when they come in a proper message might help concurrency issues?
            returnMsg = @replies[0]
          else
            returnMsg = @replies
          end
          if !returnMsg
            puts "return message nil :("
          end
          @replyaddr.tell(returnMsg)
        end
      else
        @replies << msg.message
        @replyaddr.tell(@replies) if JSON.parse(msg.message)["event"] == @expected_replies
      end

    elsif msg.test_message
      @replyaddr = getSender()
      @replies = []
      @expected_replies = msg.test_message.expected_replies
      # puts "testing #{msg.test_message.event.message}"
      # puts "expecting #{@expected_replies} replies"
      msg.test_message.handler.tell(msg.test_message.event)
    end
  end
end