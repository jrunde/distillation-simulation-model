  scriptloc = File.expand_path(File.dirname(__FILE__))
  $CLASSPATH << "#{scriptloc}/../*"
  $CLASSPATH << "#{scriptloc}/../akka/*"

  require 'java'
  require "#{scriptloc}/../scala-library.jar"
  require "#{scriptloc}/../akka/config-1.0.0.jar"
  require "#{scriptloc}/../akka/akka-actor_2.10-2.1.1.jar"
  require "#{scriptloc}/../javaGame/akka_helper"
  require 'test_messages'
  # require 'akka-project-in-java/src/main/java/akka/test/AkkaProjectInJava'
  # require 'JavaActor.jar'

  java_import 'java.io.Serializable'
  java_import 'akka.actor.UntypedActor'
  java_import 'akka.actor.ActorRef'
  java_import 'akka.actor.ActorSystem'
  java_import 'akka.actor.Props'
  java_import 'scala.concurrent.Future'

  # java_import 'AkkaProjectInJava'
  # java_import 'AkkaProjectInJava.Counter'
module JavaLoaderRspec


  include AkkaHelper

end