scriptloc = File.expand_path(File.dirname(__FILE__))

require_relative "server_wrapper"


s = ServerWrapper.new

s.do_akka(ENV["REDIS_CHANNEL"] || "edge") #redis channel
