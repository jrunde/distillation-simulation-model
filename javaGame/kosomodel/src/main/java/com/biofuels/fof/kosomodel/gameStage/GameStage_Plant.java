package com.biofuels.fof.kosomodel.gameStage;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.Game;
import com.biofuels.fof.kosomodel.Bot;

//------------------------------------------------------------------------------
public class GameStage_Plant extends GameStage {

  public GameStage_Plant(Game g) {
    super(g);
  }
  public boolean ShouldEnter() {return true; }
  public void Enter() {
//	  for(Bot b:game.getBots())
//		  b.plant();
  }
  public void Exit() {}
  public void HandleClientData(JSONObject data) {}
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Plant";
  }
  @Override
  public boolean passThrough() {
    // TODO Auto-generated method stub
    return false;
  }
}
