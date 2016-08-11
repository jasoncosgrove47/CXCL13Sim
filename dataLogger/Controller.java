package dataLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;

@SuppressWarnings("serial")
public class Controller implements Steppable {

	/**
	 * This class controls an in silico experiments. All data collection is 
	 * handled through this singleton class, it has functionality to track 
	 * populations of cell types and single cell tracking experiments.
	 * 
	 * Uses the MVC design pattern. Model: SimulationEnvironment. Each cognate 
	 * B-cell is responsible for maintaining it's own data Controller: 
	 * DataLogger contains data maps which B cells write to (more efficient than 
	 * iterating through each cell to do so) View: GUIrun or consoleRun are 
	 * responsible for running the model and instantiate OutputToCSV or Grapher 
	 * to display the data
	 * 
	 * @author Jason Cosgrove
	 */
	
	
	/**
	 * The single instance of the class
	 */
	private static Controller instance = null;
	
	
	/*
	 * Constructor for the class
	 */
	protected Controller(){
		//counter to record the duration of an experiment
		experimentTimer = 0;
		//the length of an experiment, in minutes
		lengthOfExperiment = Settings.EXPERIMENTLENGTH;
	}
	
	/**
	 * Returns the sole instance of the class
	 * @return a controller object
	 */
	public static Controller getInstance(){
		if(instance ==null)
		{
			instance = new Controller();
		}
		return instance;
	}
	


	/**
	 * Timer for the experiment, incremented in timesteps of the simulation
	 */
	private int experimentTimer = 0;

	/*
	 * The duration of an in silico experiment
	 */
	private int lengthOfExperiment;

	/**
	 * Coordinate maps 
	 * Key: the index of each individual cognateBC 
	 * Value: An arraylist containing the cells position 
	 * in a given dimension for a given timestep
	 */
	

		
	
	private Map<Integer, ArrayList<Double3D>> Coordinates = new HashMap<Integer, ArrayList<Double3D>>();
	
	
	//need to initialise this
	//TODO where the hell is this getting initialised
	private Map<Integer,Integer> dendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer,ArrayList<Integer>> receptors = new HashMap<Integer, ArrayList<Integer>>();

	/**
	 * Controls the length of an experiment and signals to the main class when
	 * an experiment is finished
	 */
	public void step(SimState state) {
		
		//increment the experiment timer
		experimentTimer++;
	
		// stop the experiment once the counter reaches
		// lengthOfExperiment
		if (experimentTimer > lengthOfExperiment) {
			SimulationEnvironment.experimentFinished = true;
		}
	}
	



	public Map<Integer,Integer> getDendritesVisited() {
		return dendritesVisited;
	}

	public Map<Integer,ArrayList<Integer>> getReceptors() {
		return receptors;
	}

	public Map<Integer, ArrayList<Double3D>> getCoordinates() {
		return Coordinates;
	}

}
