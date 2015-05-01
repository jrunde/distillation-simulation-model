#!/usr/bin/env ruby
trap(:INT) do
  puts "trapped"
  exit()
end

at_exit do
  puts "pid #{settings.j_process}"
  Process.kill("SIGINT", settings.j_process) if settings.j_process
  # settings.j_process.stop if settings.j_process
end

require 'sinatra'
require 'spoon'
require 'json'

configure do
  set :j_process, nil
  set :logging, false
end

get '/' do
  "Biofuels Game Model Control \n If you just woke up the server, try '/start' to get the model running again"
end

get '/hi' do
  "Hello World!"
end

get '/start' do
  unless settings.j_process

    # process = ChildProcess.build("ruby", "load_akka.rb")
    process = Spoon.spawnp("ruby", "load_akka.rb")

    # process.io.inherit!
    # process.start

    settings.j_process = process
    "Started server. Don't go running this twice now..."
  else
    "Server already started"
  end
end

