Biofuel Game Model
===================

Separated from the main project since it can be run as a separate process. First, build the server with maven

> ./build_model.sh

or

> cd javaSim/Model
>
> mvn package

Then run sinat.rb with jruby to start the server
> rvm jruby exec ruby sinat.rb
