package com.biofuels.fof.kosomodel.gameStage;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.Game;
//------------------------------------------------------------------------------
public class GameStage_Contracts extends GameStage {

  public GameStage_Contracts(Game g) {
    super(g);
    // TODO Auto-generated constructor stub
  }
  public boolean ShouldEnter() {return this.game.isContracts(); }
  public void Enter() {}
  public void Exit() {}
  public void HandleClientData(JSONObject data) {}
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Accept/Reject Contracts";
  }
  @Override
  public boolean passThrough() {
    // TODO Auto-generated method stub
    return false;
  }
}