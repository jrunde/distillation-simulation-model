package com.biofuels.fof.kosomodel;

public interface Bot extends Runnable {
	public void run();
	public void manage();
	public void setManagement(Boolean management);
	public void setPrices(double corn, double grass, double alfalfa);
	public void updateWeights(double ec, double env, double en);
	public double[] getWeights();
	public void makeReady();
}
