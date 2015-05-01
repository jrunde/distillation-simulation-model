package com.biofuels.fof.kosomodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.*;

import com.biofuels.fof.kosomodel.gameStage.*;

public class RoundManager {

  private List<GameStage>   mGameStageSequence;
  private Iterator<GameStage> mGameStageIterator;
  private GameStage     mCurrentGameStage;
  private Game        game;

  //--------------------------------------------------------------------------
  public void Init(Game game) {
    this.game = game;

    mGameStageSequence = new ArrayList<GameStage>();

    // Ordered by how we want them to play out, naturally.
    mGameStageSequence.add(new GameStage_Contracts(game));
    mGameStageSequence.add(new GameStage_Plant(game));
    mGameStageSequence.add(new GameStage_Manage(game));
    mGameStageSequence.add(new GameStage_Grow(game));
    mGameStageSequence.add(new GameStage_RoundWrapUp(game));
    mGameStageSequence.add(new GameStage_FinalWrapUp(game));

    mGameStageIterator = mGameStageSequence.iterator();
    mCurrentGameStage = mGameStageSequence.get(0);

  }

  //--------------------------------------------------------------------------
  public void AdvanceStage() {	  
    GameStage nextStage;
    if (mGameStageIterator.hasNext()) {
      nextStage = mGameStageIterator.next();
      
    } else {
      // wrap around to the start
      mGameStageIterator = mGameStageSequence.iterator();
      nextStage = mGameStageIterator.next();
    }
    
    if (nextStage.ShouldEnter() && !nextStage.passThrough()) {
      // exit old, set new, enter new
      mCurrentGameStage.Exit();
      mCurrentGameStage = nextStage;
      mCurrentGameStage.Enter();
      
    } else {
      // FIXME: prevent chance of endless recursion....
      this.AdvanceStage();
    }
  }

  public int getCurrentStageNumber(){
    return getEnabledStages().indexOf(mCurrentGameStage);
  }

  public int getTotalStages(){
    return getEnabledStages().size();
  }

  public List<GameStage> getEnabledStages(){
    ArrayList<GameStage> stages = new ArrayList<>();
    for(GameStage s:mGameStageSequence){
      if(s.ShouldEnter())
        stages.add(s);
    }
    return stages;
  }

  //--------------------------------------------------------------------------
  public void RouteClientData(JSONObject data) {

    // TODO: could give the RoundManager a chance to sniff out the data to
    //  see if there is anything it should handle? E.g., AdvancingStages, etc?

    mCurrentGameStage.HandleClientData(data);
  }

  public String getCurrentStageName() {
    // TODO Auto-generated method stub
    return mCurrentGameStage.getName();
  }

  public void resetStages() {
    // TODO Auto-generated method stub
    mGameStageIterator = mGameStageSequence.iterator();
    AdvanceStage();
  }

  public void setWaitForModerator(String stage, boolean value){
    for(GameStage s:mGameStageSequence){
      if(s.getName().equals(stage)){
        s.setWaitForModerator(value);
      }
    }
  }

  public boolean isWaitForModerator() {
    // TODO Auto-generated method stub
    return mCurrentGameStage.getWaitForModerator();
  }
}