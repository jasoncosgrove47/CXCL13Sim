package dataLogger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.Int2D;
import sim3d.SimulationEnvironment;
import sim3d.consoleRun;
import sim3d.cell.cognateBC;
import sim3d.cell.cognateBC.TYPE;


@SuppressWarnings("serial")
public class Controller implements Steppable {

	/**
	 * All data collection is handled through this class, it has functionality to track 
	 * populations of cell types and single cell tracking experiments.
	 * 
	 * MVC design pattern: http://www.tutorialspoint.com/design_pattern/mvc_pattern.htm
	 * 
	 * Model: 		SimulationEnvironment. Each cognate B-cell is responsible for 
	 * 				maintaining it's own data 
	 * Controller:  DataLogger contains data maps which B cells write to 
	 * 			    (more efficient than iterating through each cell to do so)
	 * View: 		GUIrun or consoleRun are responsible for running the model and
	 * 		 		instantiate OutputToCSV or Grapher to display the data
	 * 
	 * @author jason cosgrove
	 */
	
	/*
	 * Counter for the number of antigen primed B cells in the simulation
	 */
	private static int primedCells = 0;
	

	/**
	 * Timer for the experiment, incremented in timsteps
	 * of the simulation
	 */
	private int experimentTimer;
	private int lengthOfExperiment;
	
	/**
	 * Coordinate maps 
	 * Key: 	the index of each individual cognateBC
	 * Value: 	An arraylist containing the cells position 
	 * 		  	in a given dimension for a given timestep
	 */
    private Map<Integer,ArrayList<Double>> X_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Y_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Z_Coordinates = new HashMap<Integer,ArrayList<Double>>();
	
	/*
	 * Constructor for the controller class
	 */
	public Controller()
	{	
		experimentTimer = 0;
		lengthOfExperiment = 30;
	}
	
	/**
	 * Controls the length of an experiment and signals to the main class
	 * when an experiment is finished
	 */
	public void step(SimState state) 
	{
		experimentTimer ++;
		if(experimentTimer > lengthOfExperiment)
		{
			SimulationEnvironment.experimentFinished = true;
		}
	}

	
	// getters and setters for the controlller
	// better to have these private as we don't
	// wan't any external classes changing them
	public static int getPrimedCells() {return primedCells;}
	public static void setPrimedCells(int primedCells) {Controller.primedCells = primedCells;}

	public Map<Integer,ArrayList<Double>> getX_Coordinates() {return X_Coordinates;}
	public Map<Integer,ArrayList<Double>> getY_Coordinates() {return Y_Coordinates;}
	public Map<Integer,ArrayList<Double>> getZ_Coordinates() {return Z_Coordinates;}
	
}
