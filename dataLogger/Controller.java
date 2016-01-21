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
	 * Model: Each cognate B-cell is responsible for maintaining it's own data 
	 * Controller: DataLogger contains data maps which B cells write to 
	 * 			   (more efficient than iterating through each cell to do so)
	 * View: OutputToCSV or grapher modules display the data
	 * 
	 * @author jason cosgrove
	 */
	
	

	/**
	 * Counts the entire number of cells in the simulation
	 */
	private static Map<String, Integer> cellCounter = new HashMap<String, Integer>();
	
	/*
	 * Counter for the number of antigen primed B cells in the simulation
	 */
	private static int primedCells = 0;
	
	
	/**
	 * Coordinate maps 
	 * Key: the index of each individual cognateBC
	 * Value: An arraylist containing the cells position 
	 * 		  in a given dimension for a given timestep
	 */
    private Map<Integer,ArrayList<Double>> X_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Y_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Z_Coordinates = new HashMap<Integer,ArrayList<Double>>();
	
	/*
	 * Constructor for the controller class
	 */
	public Controller(){}
	

	
	int experimentCounter = 0;
	
	/**
	 * Shouldnt need to implement steppable if the B cells are writing to it. 
	 * But useful for debugging.
	 */
	public void step(SimState state) {
		
		experimentCounter ++;
		int lengthOfExperiment = 30;
		
		if(experimentCounter > lengthOfExperiment){
			SimulationEnvironment.experimentFinished = true;
		}
		
		
		
	}

	
	// getters and setters for the controlller
	public static int getPrimedCells() {return primedCells;}
	public static void setPrimedCells(int primedCells) {Controller.primedCells = primedCells;}

	public Map<Integer,ArrayList<Double>> getX_Coordinates() {return X_Coordinates;}
	public Map<Integer,ArrayList<Double>> getY_Coordinates() {return Y_Coordinates;}
	public Map<Integer,ArrayList<Double>> getZ_Coordinates() {return Z_Coordinates;}


	
}
