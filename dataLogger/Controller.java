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

	/**
	 * Initialise the network adjacency matrix, required for outputting to .csv
	 * later.
	 * 
	 * @return an n * n adjacency matrix where n is the amount of stroma + 1
	 *         for the column and row labels
	 */
	public static int[][] generateAdjacencyMatrix() {

		// calcualte the number of nodes in the network
		int[] nodesandedges = SimulationEnvironment.calculateNodesAndEdges();
		int numberofnodes = nodesandedges[0];
		
		// we add a plus one because we also want an index for each one
		int[][] adjacencyMatrix = new int[numberofnodes + 1][numberofnodes + 1];

		// add the nodes to the matrix headers, update the nodeinfo arraylist
		// and the nodeIndexMap
		adjacencyMatrix = initialiseNetworkInfo(adjacencyMatrix);

		return adjacencyMatrix;

	}

	/**
	 * Add the nodes to the matrix headers, update the nodeinfo arraylist and
	 * the nodeIndexMap
	 * 
	 * @param adjacencyMatrix
	 * @return
	 */
	private static int[][] initialiseNetworkInfo(int[][] adjacencyMatrix) {

		// the unique IDcode for each stroma node
		int nodeindex = 1;
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				//LECs are not included in topological analysis
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

	
	
	 public static int[][] updateAdjacencyMatrix(int[][] adjacencyMatrix){
		
		Bag stroma = SimulationEnvironment.getAllStroma();

		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				
				Stroma sc = (Stroma) stroma.get(i);
				if (sc.getStromatype() != Stroma.TYPE.LEC) {

					//don't add any self connections
					
					
					for(Stroma stromalcell : sc.getM_Nodes()){
						
						if(!stromalcell.equals(sc)){
						
						adjacencyMatrix[sc.getM_index()][stromalcell.getM_index()] = 1;
						adjacencyMatrix[stromalcell.getM_index()][sc.getM_index()] = 1;
						}
											
					}	
				}
			}
		}
		return adjacencyMatrix;
	}
	
	
	 
	 
	 

	 
	 
	 
	 /*

	  * 
	  * Because of the web like strucure of the FDC network, if two nodes are within
	  * the network and are not blocked by another node, then we assume that they are
	  * connected with one another in terms of the topology hi.
	  * 
	  * 
	  */
	 public static int[][] updateAdjacencyMatrixFDCs(int[][] adjacencyMatrix){
		 
		Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
					
				Stroma sc = (Stroma) stroma.get(i);
				Bag stroma2 = SimulationEnvironment.fdcEnvironment.getAllObjects();

				for (int j = 0; j < stroma2.size(); j++) {
					if (stroma2.get(j) instanceof Stroma) {
								
						Stroma sc2 = (Stroma) stroma2.get(j);
						boolean connected = true; //assume the two cells are connected hi	
						if(!sc.equals(sc2)){
							Bag stroma3 = SimulationEnvironment.fdcEnvironment.getAllObjects();	
								for (int k = 0; k < stroma3.size(); k++) {
									if (stroma3.get(k) instanceof Stroma) {
										Stroma sc3 = (Stroma) stroma2.get(j);
									
										if(isPointBetween(sc.getM_Location(),sc2.getM_Location(),sc3.getM_Location())){
											//System.out.println("FDCs aint connected");
											connected = false;
											break;
										}
									}
							}
							if(connected){
								adjacencyMatrix[sc.getM_index()][sc2.getM_index()] = 1;
								adjacencyMatrix[sc2.getM_index()][sc.getM_index()] = 1;
							}
							
						}			
					}
				}	
			}
		}
		return adjacencyMatrix;
	 }
	 
	 
		/**
		 * To determine if the path between two nodes is blocked by another node 
		 * we do the following:
		 * 
		 * Check if the points are collinear
		 * 	Collinear when the cross product of b-a and c-a is 0 (all 3 components are zero)  
		 * 
		 *  If so, also need to check if  if c's
		 * coordinates are between a's and b's.
		 * 
		 * This is true when the dot product of b-a and c-a 
		 * is positive and is less than the squared distance between a and b
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

			// this is our threshold needs to be the width of an edge i suppose so
			// lets say 3 microns
			// TODO we need to revisit this and see if its sensible
			double epsilon = 0.3;

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
	 
	
		 static boolean checkIfConnected(int[][] adjacencyMatrix, Stroma sc1, Stroma sc2){
			 
			 if (adjacencyMatrix[sc1.getM_index()][sc2.getM_index()] == 1) {
				 return true;
			 }
			 return false;
			 
		 }
		
		 
	/*

	
	public static int[][] updateAdjacencyForBranchConnections(int[][] adjacencyMatrix){
		
		//iterate through all of the branches
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {

			if (stroma.get(i) instanceof StromaEdge) {
					
				//only proceed if its a branch
				if(((StromaEdge)stroma.get(i)).isBranch() == true){

					//get the two points of the branch; these should correspond to 
					// the midpoints of two edges
					Double3D p1 = ((StromaEdge) stroma.get(i)).getPoint1();
					Double3D p2 = ((StromaEdge) stroma.get(i)).getPoint2();
					
					//how do we figure out which edges these midpoints belong to
					//were going to have to iterate through again arent we
					
					//OR we could be clever and just get the surrounding stroma and just check through
					//those, 
					

					Bag edges_1 = SimulationEnvironment.getAllStromaWithinDistance(p1, 5);


					Bag edges_2 = SimulationEnvironment.getAllStromaWithinDistance(p2, 5);
					
					//now iterate through until you find the one that you want....
					StromaEdge se_1 = null;
					StromaEdge se_2 = null;
					
					for(int k = 0; k < edges_1.size(); k++){
						if(edges_1.get(k) instanceof StromaEdge){
							
							//if its not a branch
							if(((StromaEdge)stroma.get(i)).isBranch() == false){
							
								if(((StromaEdge)edges_1.get(k)).midpoint == p1)
								{
									se_1 = ((StromaEdge)edges_1.get(k));
									//then we have our edge of interest we need to store this
									//information and then exit the loop
									break;
								}
							}
							//what do we do if it is a branch??
							else{
								System.out.println("there are edges that may be connected to other edges");
								
								//find out if they share any common points
								boolean test1 = (((StromaEdge)edges_1.get(k)).getPoint1()==p1);
								boolean test2 = (((StromaEdge)edges_1.get(k)).getPoint2()==p1);
								boolean test3 = (((StromaEdge)edges_1.get(k)).getPoint1()==p2);
								boolean test4 = (((StromaEdge)edges_1.get(k)).getPoint2()==p2);
								
							
								//if theres a connection there then they should share the points they
								// are connected to
								if(test1 || test2 || test3 || test4){
									
									//then the branches are connected
									//Imagine something similar but with one less branch
									// are all nodes still connected?
									//branch p1-p2 - p4-p3
									//branch p2-p3 - p1-p2
									
									//then yes in this scheme that would be the case
									
								}
								
								
								
							}
						}
					}
					
					for(int k = 0; k < edges_2.size(); k++){
						if(edges_2.get(k) instanceof StromaEdge){
							
							if(((StromaEdge)stroma.get(i)).isBranch() == false){
								
								if(((StromaEdge)edges_2.get(k)).midpoint == p1)
								{
									
										se_2 = ((StromaEdge)edges_2.get(k));
										//then we have our edge of interest we need to store this
										//information and then exit the loop
										break;
								}
							}
							else{
								
								System.out.println("there are edges that may be connected to other edges");
								//The points should join at some point so we should be able to find these
								//then there may be a branch nearby that we are connected to!!	
								boolean test1 = (((StromaEdge)edges_1.get(k)).getPoint1()==p1);
								boolean test2 = (((StromaEdge)edges_1.get(k)).getPoint2()==p1);
								boolean test3 = (((StromaEdge)edges_1.get(k)).getPoint1()==p2);
								boolean test4 = (((StromaEdge)edges_1.get(k)).getPoint2()==p2);
								
								//Imagine something similar but with one less branch
								// are all nodes still connected?
								//branch p1-p2 - p4-p3
								//branch p2-p3 - p1-p2
								
								//then yes in this scheme that would be the case
								
								}
						}
					}
					
					//now for each edge we need the associated nodes
					Double3D node1 = se_1.getPoint1();
					Double3D node2 = se_1.getPoint2();
					
					Double3D node3 = se_2.getPoint1();
					Double3D node4 = se_2.getPoint2();
					
					//now need to update the adjacency matrices for these 4 nodes
					//int index1 = NodeIndex.get(node1);//the index should respond ot position in the matrix
					//int index2 = NodeIndex.get(node2);
					//int index3 = NodeIndex.get(node3);//the index should respond ot position in the matrix
					//int index4 = NodeIndex.get(node4);
				
					//this code is truly dreadful
					adjacencyMatrix[index1][index2] = 1;
					adjacencyMatrix[index2][index1] = 1;
					adjacencyMatrix[index1][index3] = 1;
					adjacencyMatrix[index3][index1] = 1;
					adjacencyMatrix[index1][index4] = 1;
					adjacencyMatrix[index4][index1] = 1;
					adjacencyMatrix[index2][index3] = 1;
					adjacencyMatrix[index3][index2] = 1;
					adjacencyMatrix[index2][index4] = 1;
					adjacencyMatrix[index4][index2] = 1;
					adjacencyMatrix[index3][index4] = 1;
					adjacencyMatrix[index4][index3] = 1;
				


				}	
			}
		}
		return(adjacencyMatrix);
	}
	
	*/
	
	/**
	 * 
	 * Update the adjacency matrix with links for reticular cells. iterate
	 * through all of the edges obtain the associated nodes update the adjacency
	 * matrix
	 * 
	 * 
	 * 
	 * 
	 * @param adjacencyMatrix
	 * @return an updated adjacency matrix
	 */
	
	/*
	public static int[][] updateAdjacencyMatrixForRCs(int[][] adjacencyMatrix) {

		// we dont need all objects can just look in the immediate environment
		Bag stroma = SimulationEnvironment.getAllStroma();
		
		ArrayList<StromaEdge> edgesToDelete = new ArrayList<StromaEdge>();
		
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {
				if (((StromaEdge) stroma.get(i)).getStromaedgetype() == StromaEdge.TYPE.RC_edge
						|| ((StromaEdge) stroma.get(i)).getStromaedgetype() == StromaEdge.TYPE.MRC_edge) {

					
					StromaEdge se = ((StromaEdge) stroma.get(i));
					
					//get the nodes associated with that edge
					
					for(Stroma sc : se.m_Nodes){
						
						//adjacencyMatrix[index1][index2] = 1;
						//adjacencyMatrix[index2][index1] = 1;
						
						//update the adjacency matrix with some kind of unique index
					}
					
				}
				}
			}
		
		return adjacencyMatrix;
		}
	*/
	
	/*
					
					//get the two points of the edge
					Double3D p1 = ((StromaEdge) stroma.get(i)).getPoint1();
					Double3D p2 = ((StromaEdge) stroma.get(i)).getPoint2();

					//check to see if there are nodes associated with those edge points
					
							//in some cases there is no node there this is for the FDCs				
					if (NodeIndex.get(p1) != null && NodeIndex.get(p2) != null) {
			

							int index1 = NodeIndex.get(p1);//the index should respond ot position in the matrix
							int index2 = NodeIndex.get(p2);
							
							if(index1 == index2){
								System.out.println("indices are the same");
								System.out.println("p1: " + p1);
								System.out.println("p2: " + p2);
							}

							adjacencyMatrix[index1][index2] = 1;
							adjacencyMatrix[index2][index1] = 1;

					}
					
					
					//if one of the nodes are null check if there are any that are close. 
					else{
					
						
						//see if we are close to any nodes anyway
						int index1 = -1;
						int index2 = -1;
						for (nodeInfo temp : getNodeinformation()) {

						
							//check the distance between them hi
							if(temp.m_loc.distance(p1) < 0.05){
								index1 = NodeIndex.get(temp.m_loc);
							}
							else if(temp.m_loc.distance(p2) < 0.05){
								index2 = NodeIndex.get(temp.m_loc);
							}
		
						}
						
						
						//if the index's have been updated then assign the appropraite nodes
						if(index1 > -1 && index2 > -1){
							
							adjacencyMatrix[index1][index2] = 1;
							adjacencyMatrix[index2][index1] = 1;
						}
						
						else{
							
							//cant remove things when iterating through the bag
							//otherwise it throws an error
							
							System.out.println("edge deleted");
							
							edgesToDelete.add(((StromaEdge) stroma.get(i)));
							
							
							//doesnt seem to be close to anything so lets get rid of it
							//((StromaEdge) stroma.get(i)).getDrawEnvironment()
							//.remove(((StromaEdge) stroma.get(i)));
						
							//((StromaEdge) stroma.get(i)).stop();
							
						}

					}
	
				}
			}
		}
		
		
		//delete all of the stromal cells that were not close to other
		// nodes
		
		for(StromaEdge se : edgesToDelete){
			
			se.getDrawEnvironment().remove(se);
			se.stop();
			
		}
		

		return adjacencyMatrix;
	}
	
	*/

	/**
	 * The way we calculate topology for FDCs is more complex
	 * 
	 * Iterate through all of the nodes and provided there is no node blocking
	 * the path from nodeA to nodeB then we assume a connection. This is how we
	 * did the analysis for the in vivo datasets.
	 * 
	 * 
	 * 
	 * @param adjacencyMatrix
	 * @return an updated adjacency matrix
	 */

	
	/*
	public static int[][] updateAdjacencyMatrixForFDCs(int[][] adjacencyMatrix) {

		// obtain all of the FDC locations, again this is very inefficient, they need to be placed
		// on separate grids
		ArrayList<Double3D> fdclocations = new ArrayList<Double3D>();
		for (Stroma temp : Stroma.getNodeinformation()) {

			if (temp.getStromatype() == Stroma.TYPE.FDC) {
				fdclocations.add(temp.getM_location());
			}
		}

		// draw a line between each node with no repetitions
		for (int i = 0; i < fdclocations.size(); i++) {
			for (int j = 0; j < fdclocations.size(); j++) {

				//we dont want to connect a node to itself
				if (i != j) {

					// obtain the coordinates and query the MAP
					// for the associated indices required for updating
					// the adjacency matrix
					Double3D p1 = fdclocations.get(i);
					Double3D p2 = fdclocations.get(j);

					int index1 = NodeIndex.get(p1);
					int index2 = NodeIndex.get(p2);

					// if the two cells are connected with no
					// node inbetween them
					if (checkIfConnected(adjacencyMatrix, fdclocations, i, j, index1, index2)) {
						// then update the adjacency matrix

						adjacencyMatrix[index1][index2] = 1;
						adjacencyMatrix[index2][index1] = 1;

					}

				}
			}
		}

		// now we have all of the fdclocations, which to be honest we had
		// already
		return adjacencyMatrix;
	}
	
*/

	 

	 
	 
	// given two locations in a double3D arraylist, check all the other
	// double3Ds to see
	// if a node is blocking a connection between two other nodes.
	 
	 /*
	static boolean checkIfConnected(int[][] adjacencyMatrix, ArrayList<Double3D> fdclocations, int i, int j,
			int index1, int index2) {

		boolean placeConnection = true;

		// if there is already no connection already there see if there is a new
		// connection
		if (adjacencyMatrix[index1][index2] != 1) {

			for (Double3D p3 : fdclocations) {

				// if this isnt equal to our two points a and b
				if ((!p3.equals(fdclocations.get(i))) && (!p3.equals(fdclocations.get(j)))) {
					if (isPointBetween(fdclocations.get(i), fdclocations.get(j), p3)) {

						placeConnection = false;
						break;
					}
				}
			}

		}

		// if there is already a connection there then no point placing a new
		// connection
		// given we would have to iterate through all of the stroma
		else {
			placeConnection = false;
		}

		return placeConnection;

	}
	*/



	/**
	 * This class handles the data for each node
	 * we should attach the branches to this also
	 * 
	 * 
	 */
	/*
	static class nodeInfo {

		private Double3D m_loc; // the location of a node
		private Integer m_index; // its unique indexID
		private Stroma.TYPE m_type; // the stroma subtype: FDC,bRC, MRC

		ArrayList<StromaEdge> edges = new ArrayList<StromaEdge>();
		
		
		// constructor
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

	}

*/

	public Map<Integer, Integer> getDendritesVisited() {
		return dendritesVisited;
	}

	public Map<Integer, ArrayList<Integer>> getReceptors() {
		return receptors;
	}

	public Map<Integer, ArrayList<Double3D>> getCoordinates() {
		return Coordinates;
	}



}
