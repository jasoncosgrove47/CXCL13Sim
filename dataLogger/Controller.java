package dataLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Attribute.Array;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.stroma.Stroma;
import sim3d.stroma.Stroma.TYPE;
import sim3d.stroma.StromaEdge;
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
	private Map<Integer, Integer> dendritesVisited = new HashMap<Integer, Integer>();
	private Map<Integer, ArrayList<Integer>> receptors = new HashMap<Integer, ArrayList<Integer>>();

     // this is the node index for the stroma nodes plus their locations
	//TODO this seems messy...
	static Map< Double3D, Integer> NodeIndex = new HashMap< Double3D, Integer>();
	
	//this is an arraylist containing the information
	private static ArrayList<nodeInfo> nodeinformation = new ArrayList<nodeInfo>();
	
	static class nodeInfo{
		
		public nodeInfo(Double3D loc, int i, TYPE type) {
			setM_loc(loc);
			setM_index(i);
			setM_type(type);
		}
	
		
		public Double3D getM_loc() {
			return m_loc;
		}
		public void setM_loc(Double3D m_loc) {
			this.m_loc = m_loc;
		}


		public Integer getM_index() {
			return m_index;
		}


		public void setM_index(Integer m_index) {
			this.m_index = m_index;
		}


		public Stroma.TYPE getM_type() {
			return m_type;
		}


		public void setM_type(Stroma.TYPE m_type) {
			this.m_type = m_type;
		}


		private Double3D m_loc;
		private Integer m_index;
		private Stroma.TYPE m_type;
	}
	
	
	/**
	 * Controls the length of an experiment and signals to the main class when
	 * an experiment is finished
	 */
	public void step(SimState state) {

		// increment the experiment timer
		experimentTimer++;

		// stop the experiment once the counter reaches
		// lengthOfExperiment
		if (experimentTimer > lengthOfExperiment) {
			SimulationEnvironment.experimentFinished = true;
		}
	}
	
	
	
	public static int[][] generateAdjacencyMatrix(){

		
			int[] nodesandedges = SimulationEnvironment.calculateNodesAndEdges();
			int numberofnodes = nodesandedges[0];

			// we add a plus one because we also want an index for each one
			int[][] adjacencyMatrix = new int[numberofnodes + 1][numberofnodes + 1];
		
			
			int nodeindex = 0;
			
					
			Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();
			for (int i = 0; i < stroma.size(); i++) {
				
				if(stroma.get(i) instanceof Stroma){
					
					if(((Stroma)stroma.get(i)).getStromatype()!=Stroma.TYPE.LEC){
					
						Double3D loc = new Double3D(((Stroma) stroma.get(i)).x,
								((Stroma) stroma.get(i)).y,((Stroma) stroma.get(i)).z);
						//give it a unique ID number and store the location

						adjacencyMatrix[0][nodeindex] = nodeindex;
						adjacencyMatrix[nodeindex][0] = nodeindex;
						nodeindex += 1;
						
						
						//we need to store the location a map and another data type for later use?
						NodeIndex.put(loc, nodeindex);
						
						Stroma.TYPE type = ((Stroma)stroma.get(i)).getStromatype();
						nodeInfo ni = new nodeInfo(loc,nodeindex , type);
						
						getNodeinformation().add(ni);
						
						
					}	
				}
			}
			
			
			return adjacencyMatrix;
			
			/*
			for (Map.Entry<Integer, Double3D> entry : NodeIndex.entrySet())
			{
			    System.out.println(entry.getKey() + "/" + entry.getValue());
			}
			
			
		
			for(int j = 0; j < adjacencyMatrix.length ; j++){
				for(int k = 0; k < adjacencyMatrix.length ;k++){
					
					System.out.print(adjacencyMatrix[j][k] + " ");
					
				}	
				System.out.println();
			}
			
			*/
			

			
	}
	
	
	
	/**
	 * The way we calculate topology for RCs is different to FDCs given their very different topologies
	 * 
	 * The RCs are a case of iterating through all of the edges, determining the associated nodes and then
	 * updating the adjacency matrix. 
	 * 
	 * 
	 * @param adjacencyMatrix
	 * @return
	 */
	public static int[][] updateAdjacencyMatrixForRCs(int[][] adjacencyMatrix){
		
		
		//this bit updates all of the bRCs anyway
		Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();
		for (int i = 0; i < stroma.size(); i++) {
			if(stroma.get(i) instanceof StromaEdge){
				if(((StromaEdge)stroma.get(i)).getStromaedgetype()==StromaEdge.TYPE.RC_edge ||
						((StromaEdge)stroma.get(i)).getStromaedgetype()==StromaEdge.TYPE.MRC_edge ){
					
					Double3D p1 = ((StromaEdge)stroma.get(i)).getPoint1();
					Double3D p2 = ((StromaEdge)stroma.get(i)).getPoint2();
				
					//in some instances there may be no node there
					//so we need to check for this.
					if(NodeIndex.get(p1) !=null){
						if(NodeIndex.get(p2) !=null){
							
							int index1 = NodeIndex.get(p1);
							int index2 = NodeIndex.get(p2);
			
							
							adjacencyMatrix[index1][index2] = 1;
							adjacencyMatrix[index2][index1] = 1;
							
						}
						
					}


				}	
			}
		}
		
		return adjacencyMatrix;
	}
	
	
	/**
	 * The way we calculate topology for FDCs is more complex
	 * 
	 * Iterate through all of the nodes and provided there is no node
	 * blocking the path from nodeA to nodeB then we assume a connection. 
	 * This is how we did the analysis for the in vivo datasets. 
	 * 
	 * 
	 * @param adjacencyMatrix
	 * @return
	 */
	public static int[][] updateAdjacencyMatrixForFDCs(int[][] adjacencyMatrix){
			
		
		//obtain all of the FDC locations
		ArrayList<Double3D> fdclocations = new ArrayList<Double3D>();
		for (nodeInfo temp : getNodeinformation()) {
			
			if(temp.getM_type() == Stroma.TYPE.FDC){
				fdclocations.add(temp.getM_loc());
			}
		}
		
		
		//draw a line between each node with no repetitions
		for( int i = 0; i < fdclocations.size(); i ++){
			for( int j = 0; j < fdclocations.size(); j ++){
			
				if(!fdclocations.get(i).equals(j)){
					
			
					if(checkIfConnected(fdclocations,i,j)){
						//then update the adjacency matrix
						
						
						Double3D p1 = fdclocations.get(i);
						Double3D p2 = fdclocations.get(j);
						
						int index1 = NodeIndex.get(p1);
						int index2 = NodeIndex.get(p2);
											
						adjacencyMatrix[index1][index2] = 1;
						adjacencyMatrix[index2][index1] = 1;
						
						
					}
						
				}	
			}
		}
		

		//now we have all of the fdclocations, which to be honest we had already
		return adjacencyMatrix;
	}
	

	
	//given two locations in a double3D arraylist, check all the other double3Ds to see
	// if a node is blocking a connection between two other nodes. 
	private static boolean checkIfConnected(ArrayList<Double3D> fdclocations, int i , int j){
		
		boolean placeConnection = true;
		for (Double3D p3 : fdclocations){
			
			//if this isnt equal to our two points a and b
			if((!p3.equals(fdclocations.get(i) )) && (!p3.equals(fdclocations.get(j)))    ){
				if(isPointBetween(fdclocations.get(i), fdclocations.get(j), p3)){
					
					placeConnection = false;	
				}	
			}
		}
		
		return placeConnection;
		
	}
	
	
	
	/**
	 * Check if the cross product of b-a and c-a is 0 (all 3 components are zero) 
	 * that means all the points are collinear. 
	 * If they are, check if c's coordinates are between a's and b's.
	 * 
	 * 
	 * TODO 
	 * need to walk through this calculation with simon and make sure that it makes sense
	 * 
	 * @param a
	 * @param b
	 * @param c
	 * @return 
	 */
	private static boolean isPointBetween(Double3D a, Double3D b , Double3D c){
		
		Double3D c_a = new Double3D(c.x - a.x, c.y - a.y, c.z - a.z);
		Double3D b_a = new Double3D(b.x - a.x, b.y - a.y, b.z - a.z);
		
		Double3D crossproduct = Vector3DHelper.crossProduct(c_a, b_a);
		
		double dotproduct = Vector3DHelper.dotProduct(b_a, c_a);
		
		double squaredLength = Math.pow(a.distance(b),2);
		

		//this is our threshold needs to be the width of an edge i suppose so lets say 3 microns
		double epsilon = 0.3;
		

		//check to see if they are colinear
		
		if (crossproduct.length() > epsilon){
			return false;
		}
		
		//check if b lies within a-c by assessing whether the dot product of b-a and c-a 
		// is positive and  is less than the squared distance between a and b
		else if(dotproduct < 0){
			return false;
		}
		else if(dotproduct > squaredLength){
			return false;
		}
		else return true;
	    
		
		
	}
	

	

	public Map<Integer, Integer> getDendritesVisited() {
		return dendritesVisited;
	}

	public Map<Integer, ArrayList<Integer>> getReceptors() {
		return receptors;
	}

	public Map<Integer, ArrayList<Double3D>> getCoordinates() {
		return Coordinates;
	}


	public static ArrayList<nodeInfo> getNodeinformation() {
		return nodeinformation;
	}


	public static void setNodeinformation(ArrayList<nodeInfo> nodeinformation) {
		Controller.nodeinformation = nodeinformation;
	}

}
