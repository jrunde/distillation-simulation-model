package com.biofuels.fof.kosomodel.gameStage;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.Game;

//------------------------------------------------------------------------------
public class GameStage_Grow extends GameStage {

  public GameStage_Grow(Game g) {
    super(g);
    // TODO Auto-generated constructor stub
  }
  public boolean ShouldEnter() { return false; }
  public void Enter() {}
  public void Exit() {}
  public void HandleClientData(JSONObject data) {}
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Grow";
  }
  @Override
  public boolean passThrough() {
    // TODO Auto-generated method stub
    return false;
  }
}