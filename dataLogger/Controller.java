package dataLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.stroma.Stroma;
import sim3d.util.Vector3DHelper;

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
	 * iterating through each cell to do so).
	 * 
	 * This class also handles all of the network data, storing the node
	 * information (location, index and type) in a nodeinfo object. This data
	 * can then be outputted as a node information file an an adjacency matrix
	 * for topology analysis
	 * 
	 * View: GUIrun or consoleRun are responsible for running the model and
	 * instantiate OutputToCSV or Grapher to display the data
	 * 
	 * 
	 * 
	 * @author Jason Cosgrove
	 */

	public static ArrayList<Integer> degrees = new ArrayList<Integer>();

	/**
	 * The single instance of the class
	 */
	private static Controller instance = null;

	/*
	 * Constructor for the class
	 */
	protected Controller() {
		// counter to record the duration of an experiment
		experimentTimer = 0;
		// the length of an experiment, in minutes
		lengthOfExperiment = Settings.EXPERIMENTLENGTH;
	}

	/**
	 * Returns the sole instance of the class
	 * 
	 * @return a controller object
	 */
	public static Controller getInstance() {
		if (instance == null) {
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
	 * Coordinate maps Key: the index of each individual cognateBC Value: An
	 * arraylist containing the cells position in a given dimension for a given
	 * timestep
	 */
	private Map<Integer, ArrayList<Double3D>> Coordinates = new HashMap<Integer, ArrayList<Double3D>>();
	private Map<Integer, Integer> fdcdendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> mrcdendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> rcdendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> totaldendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> checkpointsReached = new HashMap<Integer, Integer>();
	private Map<Integer, ArrayList<Integer>> receptors = new HashMap<Integer, ArrayList<Integer>>();

	private static double[][] chemokinefield;
	
	/**
	 * Controls the length of an experiment and signals to the main class when
	 * an experiment is finished
	 */
	public void step(SimState state) {

		// increment the experiment timer
		experimentTimer++;

		if(experimentTimer == 5){
			setChemokinefield(SimulationEnvironment.CXCL13.recordChemokineField());
		}
		// stop the experiment once the counter reaches
		// lengthOfExperiment
		if (experimentTimer > lengthOfExperiment) {
			SimulationEnvironment.experimentFinished = true;
		
		}
		
	}

	/**
	 * Initialise the network adjacency matrix, required for outputting to .csv
	 * later.
	 * 
	 * @return an n * n adjacency matrix where n is the amount of stroma + 1 for
	 *         the column and row labels
	 */
	public static double[][] generateAdjacencyMatrix() {

		// calcualte the number of nodes in the network
		int[] nodesandedges = SimulationEnvironment.calculateNodesAndEdges();
		int numberofnodes = nodesandedges[0];

		// we add a plus one because we also want an index for each one
		double[][] adjacencyMatrix = new double[numberofnodes + 1][numberofnodes + 1];

		// add the nodes to the matrix headers, update the nodeinfo arraylist
		// and the nodeIndexMap
		adjacencyMatrix = initialiseNetworkInfo(adjacencyMatrix);

		return adjacencyMatrix;

	}

	/**
	 * This method creates and updates adjacency and distance matrices for
	 * analysing stroma topology
	 * 
	 * @param matrix
	 * @param dist
	 * @return
	 */
	public static double[][] createMatrix(boolean dist) {
		// this initialises the matrix
		double[][] matrix = generateAdjacencyMatrix();
		Bag stroma = SimulationEnvironment.getAllStroma();

		// iterate through all of the stromal cells and
		// update their positions in the adjacency matrix
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {

				Stroma sc = (Stroma) stroma.get(i);
				if (sc.getStromatype() != Stroma.TYPE.LEC) {

					int counter = 0;
					// don't add any self connections
					// there should be no LECs in the node list, must be MRCs.
					for (Stroma stromalcell : sc.getM_Nodes()) {
						if (!stromalcell.equals(sc)) {

							if (dist) {

								// multiply by 10 because we want it in microns
								double distance = sc.getM_Location().distance(stromalcell.getM_Location()) * 10;
								matrix[sc.getM_index()][stromalcell.getM_index()] = distance;
								matrix[stromalcell.getM_index()][sc.getM_index()] = distance;
							} else {
								counter += 1;
								matrix[sc.getM_index()][stromalcell.getM_index()] = 1;
								matrix[stromalcell.getM_index()][sc.getM_index()] = 1;
							}
						}
					}
					degrees.add(counter);
				}
			}
		}
		return matrix;
	}

	/**
	 * Add the nodes to the matrix headers, update the nodeinfo arraylist and
	 * the nodeIndexMap
	 * 
	 * @param adjacencyMatrix
	 * @return
	 */
	private static double[][] initialiseNetworkInfo(double[][] adjacencyMatrix) {

		// the unique IDcode for each stroma node
		int nodeindex = 1;

		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				// LECs are not included in topological analysis
				if (((Stroma) stroma.get(i)).getStromatype() != Stroma.TYPE.LEC) {
					((Stroma) stroma.get(i)).setM_index(nodeindex);

					// give it a unique ID number and store the location
					adjacencyMatrix[0][nodeindex] = nodeindex;
					adjacencyMatrix[nodeindex][0] = nodeindex;

					nodeindex += 1;
				}
			}
		}
		return adjacencyMatrix;
	}

	/**
	 * To determine if the path between two nodes is blocked by another node we
	 * do the following:
	 * 
	 * Check if the points are collinear Collinear when the cross product of b-a
	 * and c-a is 0 (all 3 components are zero)
	 * 
	 * If so, also need to check if if c's coordinates are between a's and b's.
	 * 
	 * This is true when the dot product of b-a and c-a is positive and is less
	 * than the squared distance between a and b
	 * 
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	static boolean isPointBetween(Double3D a, Double3D b, Double3D c) {

		Double3D c_a = new Double3D(c.x - a.x, c.y - a.y, c.z - a.z);
		Double3D b_a = new Double3D(b.x - a.x, b.y - a.y, b.z - a.z);

		Double3D crossproduct = Vector3DHelper.crossProduct(c_a, b_a);

		double dotproduct = Vector3DHelper.dotProduct(b_a, c_a);

		double squaredLength = Math.pow(a.distance(b), 2);

		// a threshold value that equals 10 microns
		double epsilon = 1;
		// check to see if they are collinear

		if (crossproduct.length() > epsilon) {
			return false;
		}

		// check if b lies within a-c by assessing whether the dot product of
		// b-a and c-a
		// is positive and is less than the squared distance between a and b
		else if (dotproduct < 0) {
			return false;
		} else if (dotproduct > squaredLength) {
			return false;
		} else
			return true;
	}

	/**
	 * Check if two nodes are already connected on the basis of the adjacency
	 * matrix
	 * 
	 * @param adjacencyMatrix
	 *            the adjacency matrix to query
	 * @param sc1
	 *            stromal cell one
	 * @param sc2
	 *            stromal cell two
	 * @return true if the nodes are connected
	 */
	static boolean checkIfConnected(double[][] adjacencyMatrix, Stroma sc1, Stroma sc2) {

		if (adjacencyMatrix[sc1.getM_index()][sc2.getM_index()] == 1) {
			return true;
		}
		return false;
	}

	public Map<Integer, Integer> getFDCDendritesVisited() {
		return fdcdendritesVisited;
	}
	
	public Map<Integer, Integer> getMRCDendritesVisited() {
		return mrcdendritesVisited;
	}

	public Map<Integer, ArrayList<Integer>> getReceptors() {
		return receptors;
	}

	public Map<Integer, ArrayList<Double3D>> getCoordinates() {
		return Coordinates;
	}

	public static double[][] getChemokinefield() {
		return chemokinefield;
	}

	public void setChemokinefield(double[][] chemokinefield) {
		Controller.chemokinefield = chemokinefield;
	}

	public Map<Integer, Integer> getRcdendritesVisited() {
		return rcdendritesVisited;
	}

	public void setRcdendritesVisited(Map<Integer, Integer> rcdendritesVisited) {
		this.rcdendritesVisited = rcdendritesVisited;
	}

	public Map<Integer, Integer> getTotaldendritesVisited() {
		return totaldendritesVisited;
	}

	public void setTotaldendritesVisited(Map<Integer, Integer> totaldendritesVisited) {
		this.totaldendritesVisited = totaldendritesVisited;
	}

	public Map<Integer, Integer> getCheckpointsReached() {
		return checkpointsReached;
	}

	public void setCheckpointsReached(Map<Integer, Integer> checkpointsReached) {
		this.checkpointsReached = checkpointsReached;
	}

}
