package com.biofuels.fof.kosomodel.gameStage;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.Game;

// Interface for a game stage. This would be something like Contracts, Management, etc
//  This stage mechanism encapsulates all of the logic for running the stage on the back
//  end as well as managing sending the data needed for a given stage to the clients
//  attached to a given room.
//------------------------------------------------------------------------------
public abstract class GameStage {
//------------------------------------------------------------------------------
  protected Game game;
  protected boolean waitForModerator;

  public GameStage(Game g){
    game = g;
  }
  // Checks current game settings about whether this game stage should be entered or skipped
  public abstract boolean ShouldEnter();
  public abstract boolean passThrough();

  // Called when game enters the given stage
  public abstract void Enter();
  // Called when game exits the given stage
  public abstract void Exit();

  public abstract String getName();
  // Messages/Data/Events from client in JSON object format...
//  public void HandleClientData(JsonNode data);
  public abstract void HandleClientData(JSONObject data);

  public void setWaitForModerator(boolean wait){
    waitForModerator = wait;
  }

  public boolean getWaitForModerator(){
    return waitForModerator;
  }
}