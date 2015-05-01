require 'spoon'

def pid_exists?
  File.exist?('model.pid')
end

def remove_pid
  File.unlink('model.pid')
end

def run
  exec "jruby sinat.rb #{ARGV[1..-1].join(" ")}"
end

def start

  if pid_exists?
    puts "server already started! (try stop or restart)"
    exit(0)
  end

  pid = Spoon.spawnp 'jruby', 'sinat.rb', *ARGV[1..-1]

  open('model.pid','w'){|f| f.write pid}
end

def stop
  if File.exist? 'model.pid'
    pid = IO.read 'model.pid'
  else
    puts "no process running (no pid file at #{`pwd`[0..-2]}/model.pid)"
    return
  end

  begin
    puts "killing #{pid.to_i}"
    Process.kill("SIGINT", pid.to_i)
    sleep(1)
  rescue Errno::ESRCH
    puts "no process at #{pid}, deleting pidfile"
  end

  remove_pid
end

def restart
  stop
  start
end

if ARGV[0] == "start"
  start
elsif ARGV[0] == "stop"
  stop
elsif ARGV[0] == "restart"
  restart
elsif ARGV[0] == "run"
  run
else
  puts "usage: server_control <start|stop|restart>"
end