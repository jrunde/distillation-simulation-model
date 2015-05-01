export CLASSPATH=./javaGame:./javaGame/json-simple-1.1.1.jar:${GAMSDIR}/apifiles/Java/api/GAMSJavaAPI.jar
export LD_LIBRARY_PATH=${GAMSDIR}/apifiles/Java/api:${LD_LIBRARY_PATH}
jruby javaGame/server_runner.rb

