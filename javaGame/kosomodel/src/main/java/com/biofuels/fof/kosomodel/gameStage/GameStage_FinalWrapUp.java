package com.biofuels.fof.kosomodel.gameStage;
import org.json.simple.*;

import com.biofuels.fof.kosomodel.Game;

//------------------------------------------------------------------------------
public class GameStage_FinalWrapUp extends GameStage {

  public GameStage_FinalWrapUp(Game g) {
    super(g);
    // TODO Auto-generated constructor stub
  }
  public boolean ShouldEnter() {return game.isFinalRound(); }
  public void Enter() {}
  public void Exit() {}
  public void HandleClientData(JSONObject data) {}
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Final Wrap Up";
  }
  @Override
  public boolean passThrough() {
    // TODO Auto-generated method stub
    return false;
  }
}