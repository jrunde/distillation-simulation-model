package com.biofuels.fof.kosomodel;

import akka.actor.*;

public class ActorSystemHelper {
    public ActorSystem getSystem(){
       return(ActorSystem.create("TestSystem"));
    }

    public ActorRef makenew(ActorSystem sys, Class<? extends UntypedActor> c, String name){
      return sys.actorOf(new Props(c), name);
    }
}
