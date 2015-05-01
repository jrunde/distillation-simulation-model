package com.biofuels.fof.kosomodel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import akka.actor.*;


import org.json.simple.*;

public class Handler extends UntypedActor{

  HandlerHelper eh = new HandlerHelper(getContext().actorFor("/user/listener"), getSelf());

  @SuppressWarnings({ "unchecked", "deprecation" })
public void onReceive(Object message) throws Exception {
	ActorRef listener = null;
    if (message instanceof EventMessage) {
        String client = (String)((JSONObject) JSONValue.parse(((EventMessage)message).message)).get("clientID");
        try {
            eh.handle(((EventMessage)message).message);
        } catch (Exception e) {
    	  
            // print out the exception in the console
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            System.out.println(client + " \nexception: " + exceptionAsString);
            
            // send the client an appropriate error message
            String sorry = "Your game of Fields of Fuel has encountered an unexpected error. Refreshing your browser window should correct the problem. However, in the unlikely event that it persists, please end your game and start a new one. Thanks for your patience!";
            JSONObject msg = new JSONObject();
            msg.put("event", "modelError");
            msg.put("shortMessage", "We're really sorry!");
            msg.put("longMessage", sorry);
            msg.put("clientID", client);
            getContext().actorFor("../listener").tell(new EventMessage(msg.toJSONString()), getSelf());
        }
    }


    else if(message instanceof ActorRef){
    	listener = (ActorRef) message;
    	eh.setListener((ActorRef) message);
    }
  }
}