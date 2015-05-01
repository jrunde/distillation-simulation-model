package com.biofuels.fof.kosomodel;

import java.util.ArrayList;

public class SimpleBot implements Bot {
//	private HandlerHelper dispatch;
	private Farm farm;
	
	public SimpleBot(Farm f){
//		this.dispatch = dispatch;
		farm = f;
	}
	
	public void run(){
//		ArrayList<String> plants = new ArrayList<String>();
		for(Field f:farm.getFields()){
			// if(f.getSOC() < 30 || (f.getSOC() < 70 && f.getCrop() == Crop.GRASS))
			if(Math.random() < 0.5)
			    f.setCrop(Crop.GRASS);
			else
				f.setCrop(Crop.CORN);
		}
//		return plants;
	}

	@Override
	public void manage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setManagement(Boolean management) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPrices(double corn, double grass, double alfalfa) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateWeights(double ec, double env, double en) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void makeReady() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double[] getWeights() {
		// TODO Auto-generated method stub
		return null;
	}
}
