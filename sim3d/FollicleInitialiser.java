package sim3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.Stroma;
import sim3d.stroma.Stroma.TYPE;
import sim3d.stroma.StromaEdge;
import sim3d.util.ParseStromaLocations;
import sim3d.util.StromaGenerator;

/**
 * Initialise the follicle microenvironment, generating stroma and the
 * subcapsular sinus. This class also handles data handling for stroma recording
 * which nodes are connected to which edges and vice versa
 * 
 * @author Jason Cosgrove
 */
public final class FollicleInitialiser {

	// a threshold distance within which we add connections
	// between stroma
	static double connectionDistThreshold = 1.0; 

	/**
	 * This method sets up the stroma within the follicle and the subcapsular
	 * sinus.
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 */
	public static void initialiseFollicle(CollisionGrid cgGrid) {

		GenerateSCS.seedSCS(cgGrid);
		generateStromalNetwork(cgGrid);
		GenerateSCS.generateMRCNetwork(cgGrid); // The MRC network is generated
												// separately to BRCs and MRCs
		//TODO at this point we assume that all protrusions are updated for each node
		//based on this we can assign the nodes we are connected to, which is used
		// to update the adjacency matrix. Pretty sure that this code is fine even
		//with stochastic placing of FDC nodes, not sure it is applicable when there
		// are branches added though

		
		//now that all of the protrusions are done we can update all edge connections
		updateNodeConnections();

	
		// use a threshold for one as distance required for a branch to form
		// these should be wrapped up in the seed stroma method
		//generateBranchesBetweenProtrusions(cgGrid, connectionDistThreshold, StromaEdge.TYPE.FDC_branch);
		//generateBranchesBetweenProtrusions(cgGrid, connectionDistThreshold, StromaEdge.TYPE.FDC_branch);
		//generateBranchesBetweenProtrusions(cgGrid,1.0, StromaEdge.TYPE.RC_branch);
		//generateBranchesBetweenProtrusions(cgGrid,0.5, StromaEdge.TYPE.MRC_branch);
		

		
		
	}

	/**
	 * Generates a stromal cell, adds it to the collisionGrid and updates its
	 * draw environment
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 * @param type
	 *            the type of stromal cell
	 * @param loc
	 *            the location of the stromal cell
	 */
	static void instantiateStromalCell(CollisionGrid cgGrid, Stroma.TYPE type, Double3D loc) {
		Stroma sc = new Stroma(type, loc);
		sc.setObjectLocation(loc);
		sc.registerCollisions(cgGrid);
		SimulationEnvironment.scheduleStoppableCell(sc);
	}

	
	
	//this method works on the assumption that all protrusions have been added
	// which im pretty sure is not the case for BRCs and mRCs
	private static void updateNodeConnections(){
	
		Bag stroma = SimulationEnvironment.getAllStroma();
		for(int i = 0; i < stroma.size(); i ++){
		
			if(stroma.get(i) instanceof Stroma){
				Stroma sc = (Stroma) stroma.get(i);
				
				if(sc.getStromatype() != Stroma.TYPE.LEC){
				
					
				updateNodeConnectionForNode(sc);
				if(sc.getM_Nodes().size() > 10){
					//System.out.println("stroma type: " + sc.getStromatype());
				}
				
				if(sc.getM_Nodes().size() == 0){
					//System.out.println("number of edges: " + sc.getM_Edges().size());
					//System.out.println("stroma type with zero nodes connected to: " + sc.getStromatype());
				}
				}

			}			
		}
	}
	
	
	
	static void placeFDCStochastically(double threshold, StromaGenerator.StromalCelltemp sc, CollisionGrid cgGrid, ArrayList<StromaEdge> sealEdges){
		double random = Settings.RNG.nextDouble();
		if(random > threshold){
			seedStromaNode(sc, cgGrid, sealEdges);
		}
		else{
			SimulationEnvironment.fdcEnvironment.remove(sc);
		}
		
	}
	
	/**
	 * CHeck to see what nodes our node is connected to.
	 * 
	 * OK so as far as i can tell this method works
	 * 
	 * now just need to see what happens when i get rid of nodes
	 * @param sc
	 */
	static void updateNodeConnectionForNode(Stroma sc){
		

		//these are ordered lists, as such it is safe to update them, whilst iterating
		// over them. 
		ArrayList<StromaEdge> tempEdges = new ArrayList<StromaEdge>();
		ArrayList<Double3D> locations = new ArrayList<Double3D>();
		

		//update tempedges and locations with all of the 
		// node protrusions
		
		if(sc.getM_Edges().size() == 0){
			System.out.println("no protrusions assinged" + sc.getStromatype());
			
		}
		
		tempEdges.addAll(sc.getM_Edges());
		for(StromaEdge se : tempEdges){

			
			if(!locations.contains(se.getPoint1())){
				locations.add(se.getPoint1());
			}
			if(!locations.contains(se.getPoint2())){
				locations.add(se.getPoint2());
			}
		}
		

		
		//check each point we are connected to and see what nodes are there
		for(int i = 0; i < locations.size(); i ++){

			Double3D newloc = locations.get(i);
			
			//see if there is a node at the location
			Stroma neighbour = getNodeAtLocation(sc.getDrawEnvironment(),newloc);
			
			//are there any nodes associated with this newloc, update the relevant arrays
			//sometimes we pick up an LEC on the way
			if(neighbour != null && neighbour.getStromatype() != Stroma.TYPE.LEC){
				//if there is a stromal cell at this location
				if(!sc.getM_Nodes().contains(neighbour)){
					sc.getM_Nodes().add(neighbour);
				}
				
			}
			else{ //if there are no nodes we need to get all edges associated with this edge
				 // so that we can check to see if they have any nodes attached to them. 
	
				//this code does not account for branches
				double threshold = Settings.DOUBLE3D_PRECISION;
				//get the edges connected to this edge
				//want a good bit of leeway here as the edge could be long, and not sure how it gets the n
				Bag otherEdges = sc.getDrawEnvironment().getNeighborsExactlyWithinDistance(newloc, 5);
		
				for(int j = 0; j < otherEdges.size(); j ++){
					if(otherEdges.get(j) instanceof StromaEdge){
						
						StromaEdge otherEdge = (StromaEdge) otherEdges.get(j);
						
						
						if(otherEdge.getPoint1().distance(newloc) < threshold){
							
							if(!tempEdges.contains(otherEdge)){
								tempEdges.add(otherEdge);
								if(!locations.contains(otherEdge.getPoint1())){
									locations.add(otherEdge.getPoint1());
								}
								if(!locations.contains(otherEdge.getPoint2())){
									locations.add(otherEdge.getPoint2());
								}
								
							}
						}
						
						else if(otherEdge.getPoint2().distance(newloc) < threshold){
							if(!tempEdges.contains(otherEdge)){
								tempEdges.add(otherEdge);
								//because of the floating point numbers best add the two to be safe
								if(!locations.contains(otherEdge.getPoint1())){
									locations.add(otherEdge.getPoint1());
								}
								if(!locations.contains(otherEdge.getPoint2())){
									locations.add(otherEdge.getPoint2());
								}
							}
						}	
					}
				}			
			}
		}
		
		if(sc.getM_Nodes().size()==0){
			
			System.out.println("edgetype with no nodes" + sc.getStromatype());
			
			for(StromaEdge se : sc.getM_Edges()){
				//System.out.println("node loc: " + sc.getM_Location());
				//System.out.println("p1: " + se.getPoint1());
				//System.out.println("p2: " + se.getPoint2());
				
			}
			
		}
	}
	
	
	
	/**
	 * This method is responsible for generating the FDC and BRC stromal network
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 */
	private static void generateStromalNetwork(CollisionGrid cgGrid) {
		// Generate some stroma
		ArrayList<StromaGenerator.StromalCelltemp> stromalCellLocations = new ArrayList<StromaGenerator.StromalCelltemp>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		StromaGenerator.generateStroma3D_Updated(Settings.WIDTH - 2, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 2),
				Settings.DEPTH - 2, Settings.bRC.COUNT, stromalCellLocations, sealEdges);

		
		for (StromaGenerator.StromalCelltemp sc : stromalCellLocations) {	
			
			//seedStromaNode(sc, cgGrid, sealEdges);
			
	
			
			if(sc.m_type == Stroma.TYPE.FDC){
				double threshold = 0.6;
				placeFDCStochastically(threshold,sc,cgGrid,sealEdges);
			}
			else{
			seedStromaNode(sc, cgGrid, sealEdges);
			}
		
	
		
		}

		//when we instantiate the edges we change their location, so this needs to be accounted for
		ArrayList<StromaEdge> updatedEdges = seedEdges(cgGrid, sealEdges);
		//set thsi to null so we cant use it again anywhere. 
		sealEdges = null;
		
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for(int i = 0; i < stroma.size(); i ++){
			if(stroma.get(i) instanceof Stroma){
				Stroma sc = (Stroma) stroma.get(i);
				
				if(sc.getStromatype() != Stroma.TYPE.LEC){
					//the MRC protrusions are assigned in the other class
					assignProtrusions(updatedEdges, sc.getM_Location(), sc);
				}
				
				
			}
			
		}
		
		//once we have assigned all of the protrusions we can
		//now update all node connections

		
					
	}
	
	
	private static int countFDCEdges(){
	//now all nodes and edges are on the network and the nodes
	//have kept track of what nodes they are connected to
		int counter = 0;
	
		Bag cells = SimulationEnvironment.fdcEnvironment.getAllObjects();
		for(int i = 0; i < cells.size(); i ++){
			if(cells.get(i) instanceof Stroma){
				Stroma sc = (Stroma) cells.get(i);
				counter += sc.getM_Edges().size();
			}
		}
		return counter;
	}
	

	/**
	 * Seed the a stroma node generated by the StromaGenerator3D class place on
	 * the grid and assign edge connections
	 * 
	 * @param sc
	 *            the stroma node we want to seed
	 * @param cgGrid
	 *            the collision grid we want to add the node to
	 * @param edges
	 *            the edges its connected to
	 */
	private static void seedStromaNode(StromaGenerator.StromalCelltemp sc, CollisionGrid cgGrid,
			ArrayList<StromaEdge> edges) {
		Double3D loc = new Double3D(sc.m_d3Location.x + 1, sc.m_d3Location.y + 1, sc.m_d3Location.z + 1);
		Stroma stromalcell = new Stroma(sc.m_type, loc);
		stromalcell.setObjectLocation(loc);
		SimulationEnvironment.scheduleStoppableCell(stromalcell);
		
		// so what happens if we dont actually place all of the nodes. 
		//thats fine actually it will only account for direct connections to what we already have
		//assignProtrusions(edges, loc, stromalcell);
		
	}

	/**
	 * Given a double3D return the stroma node at that location if there is
	 * nothing there then return null.
	 * 
	 * TODO we also need to account for times when there are multiple objects in
	 * the same location...
	 * 
	 * this code will not account for connections between FDCs and BRCs
	 * but still doesnt seem to be correct.
	 * 
	 * 
	 * @param env
	 * @param loc
	 * @return
	 */
	private static Stroma getNodeAtLocation(Continuous3D env, Double3D loc) {

		Bag othernodesandedges = new Bag();
		//othernodesandedges = env.getObjectsAtLocation(loc);
		othernodesandedges = env.getNeighborsExactlyWithinDistance(loc,0.5);

		Stroma othernode = null;


		//should allow some give with this as they might not be exactly placed correctly
		double smallestDist = 0.5;
		
		
		
		// TODO this is quite messy, rewrite MASON function so it
		// returns an empty bag and not a null
		if (othernodesandedges != null) {

			for (int i = 0; i < othernodesandedges.size(); i++) {
				if (othernodesandedges.get(i) instanceof Stroma) {
					
					othernode = (Stroma) othernodesandedges.get(i);
					
					//what do we do if we have multiple 
					double dist = ((Stroma) othernodesandedges.get(i)).getM_Location().distance(loc);
					
					
					if(dist < smallestDist){
					    smallestDist = dist;
					    othernode = (Stroma) othernodesandedges.get(i);
					}
			

				}
			}
		}
		
		return othernode;
	}



	/**
	 * Remove an RC and its associated dendrites from the grid and the schedule
	 * 
	 * @param frc
	 *            the stromal cell to delete
	 */
	public static void deleteSC(Stroma sc) {

		// remove the frc from grid and schedule
		sc.getDrawEnvironment().remove(sc);
		sc.stop();

		// now get rid of all associated edges, would need to update
		// some of the connections if that is the case
		for (StromaEdge se : sc.getM_Edges()) {
			se.getDrawEnvironment().remove(se);
			se.stop();
		}
	}

	/**
	 * This method takes the stroma edges contained generated by
	 * StromaGenerator3D and places them on the stroma and collision grid as
	 * well as adding them to the schedule.
	 * 
	 * @param cgGrid
	 *            the collision grid to add to the edges
	 * @param edges
	 *            the list of edges to seed, created by StromaGenerator3D
	 * @return 
	 */
	private static ArrayList<StromaEdge> seedEdges(CollisionGrid cgGrid, ArrayList<StromaEdge> edges) {

		ArrayList<StromaEdge> updatedEdges = new ArrayList<StromaEdge>();
		
		
		for (StromaEdge tempEdge : edges) {

			//we need to create a new edge so we keep everything consistent, otherwise the coordinates get
			//messed up
			Double3D loc1 = new Double3D(tempEdge.getPoint1().x + 1, tempEdge.getPoint1().y + 1, tempEdge.getPoint1().z + 1);
			Double3D loc2 = new Double3D(tempEdge.getPoint2().x + 1, tempEdge.getPoint2().y + 1, tempEdge.getPoint2().z + 1);
			StromaEdge se = new StromaEdge(loc1,loc2,tempEdge.getStromaedgetype());
			
			se.setObjectLocation(loc1);
			Stroma sc1 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1());
			Stroma sc2 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2());

			// if a node doesnt have two associated nodes then we delete it.
			if (sc1 != null && sc2 != null) {
				
				addEdgeToSchedule(sc1, sc2,se, cgGrid, se.getStromaedgetype());
				updatedEdges.add(se);
								
			} else if(sc1 != null){
				//might be a connection between brc and fdc so need to query the other grid yo
				Stroma sc3 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1());
				Stroma sc4 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2());
				
				
				if (sc3 != null) {
					addEdgeToSchedule(sc1, sc3,se, cgGrid, se.getStromaedgetype());	
					updatedEdges.add(se);
				}
				
				else if(sc4 !=null){
					addEdgeToSchedule(sc1, sc4,se, cgGrid, se.getStromaedgetype());
					updatedEdges.add(se);
				}
				//appears to have aberrant connections so lets get rid of it. 
				else{
					se.getDrawEnvironment().remove(se);
					
				}
			}
			
			 else if(sc2 != null){
					//might be a connection between brc and fdc so need to query the other grid yo
					Stroma sc3 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1());
					Stroma sc4 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2());
					
					
					if (sc3 != null) {
						addEdgeToSchedule(sc2, sc3,se, cgGrid, se.getStromaedgetype());	
						updatedEdges.add(se);
					}
					
					else if(sc4 !=null){
						addEdgeToSchedule(sc2, sc4,se, cgGrid, se.getStromaedgetype());
						updatedEdges.add(se);
					}
					//appears to have aberrant connections so lets get rid of it. 
					else{
						se.getDrawEnvironment().remove(se);
						
					}
				}
			
			else{
				se.getDrawEnvironment().remove(se);
				
			}
		}
		
		return updatedEdges;
	}

	/**
	 * this method instantiates the edge, places it on the collision grid,
	 * updates the draw environment and updates the node.edges list and the
	 * edge.nodes list to keep track of all connections
	 * 
	 * @param sc1
	 *            a stromal cell
	 * @param sc2
	 *            a stromal cell
	 * @param cgGrid
	 *            the collisionGrid we add the edge to
	 * @param type
	 *            the type of stroma edge we want to join the two cells with
	 */

	static void addEdgeToSchedule(Stroma sc1, Stroma sc2,StromaEdge seEdge, CollisionGrid cgGrid, StromaEdge.TYPE type) {


		seEdge.setObjectLocation(seEdge.getPoint1());
		
		SimulationEnvironment.scheduleStoppableCell(seEdge);
		seEdge.registerCollisions(cgGrid);

		// update the connections
		//if(!sc1.getM_Edges().contains(seEdge)) sc1.getM_Edges().add(seEdge);
		//if(!sc2.getM_Edges().contains(seEdge)) sc2.getM_Edges().add(seEdge);


		//seEdge.m_Nodes.add(sc2); // the nodes an edge is connected to
		//seEdge.m_Nodes.add(sc1);

		//sc1.getM_Nodes().add(sc2); // the nodes a node is connected to
		//sc2.getM_Nodes().add(sc1);
	}
	
	
	/**
	 * Assign the edges that are directly linked to a node: ie the protrusions
	 * 
	 * @param edges
	 *            list of all the edge locations
	 * @param loc
	 *            the frcs location
	 * @param frc
	 */
	private static void assignProtrusions(ArrayList<StromaEdge> edges, Double3D loc, Stroma sc) {

		// update the node and edge connections for all edges
		for (StromaEdge seEdge : edges) {

		
		    Double3D edgeloc = new Double3D(seEdge.getPoint1().x, seEdge.getPoint1().y,
					seEdge.getPoint1().z );
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x, seEdge.getPoint2().y,
					seEdge.getPoint2().z);



			if (loc.distance(edgeloc) < Settings.DOUBLE3D_PRECISION) {

					sc.getM_Edges().add(seEdge);
					seEdge.m_Nodes.add(sc);
				
				
			} else if (loc.distance(edgeloc2) < Settings.DOUBLE3D_PRECISION) {

					sc.getM_Edges().add(seEdge);
					seEdge.m_Nodes.add(sc);
				
			}
		}
		
		
		//no node updates until the other method
		// update all of the edge connections for each node
		for (StromaEdge se : sc.getM_Edges()) {
			// update the node information
			//sc.getM_Nodes().addAll(se.m_Nodes);
			// remove any self connections
			//sc.getM_Nodes().remove(sc);
		}

	}

	
	/**
	 * Add branches between stroma edges
	 * 
	 * @param connectionsToAdd
	 *            A map containing the edges to connect to
	 * @param cgGrid
	 *            the collision grid to add the branch to
	 * @param type
	 *            the type of branch we want to add to the grid
	 */
	/*
	private static void addFDCEdge(Map<Stroma, ArrayList<Stroma>> connectionsToAdd, CollisionGrid cgGrid,
			StromaEdge.TYPE type) {

		for (Map.Entry<Stroma, ArrayList<Stroma>> entry : connectionsToAdd.entrySet()) {

			// get the edge and the edges it is connected to
			Stroma se1 = entry.getKey();
			ArrayList<Stroma> sesToConnectTo = entry.getValue();

			for (Stroma se2 : sesToConnectTo) {

				// make sure there are no self connections
				if (!se1.equals(se2)) {

					addEdgeBetweenNodes(se1,se2,cgGrid, StromaEdge.TYPE.FDC_edge);
							
				}
			}
		}
	}
	*/
	
	/**
	private static Map<Stroma, ArrayList<Stroma>> connectFDCNodes(CollisionGrid cgGrid, double threshold){
		// remember that an arraylist will allow duplicate entries but a MAP
		// wont
		Map<Stroma, ArrayList<Stroma>> connectionsToAdd = new HashMap<Stroma, ArrayList<Stroma>>();

		// set the environment and type of edge based on the boolean input FDC
		//StromaEdge.TYPE type= StromaEdge.TYPE.FDC_branch; // the type of stromal cell
		Continuous3D env = SimulationEnvironment.fdcEnvironment; // the stromal cell environment

		// iterate through all the objects in the environment
		Bag stroma = env.getAllObjects();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				// get all the neighbours within 15 microns away
				Stroma se = (Stroma) stroma.get(i);
				//check the nodeList to see if they are conected

				Double3D loc = se.getM_location();
				Bag neighbours = env.getNeighborsExactlyWithinDistance(loc, threshold, false);

				for(int j = 0; j < neighbours.size(); j ++){
					if (neighbours.get(j) instanceof Stroma) {
						Stroma neighbour = (Stroma) neighbours.get(j);
						if(!se.getM_Nodes().contains(neighbour)){
							
							if (connectionsToAdd.get(se) == null) {
								connectionsToAdd.put(se, new ArrayList<Stroma>());
							}

							connectionsToAdd.get(se).add(neighbour);
							//then make a connection yo
							//remember that 
						}
					}
				}
			}
		}
		
		addFDCEdge(connectionsToAdd,cgGrid,StromaEdge.TYPE.FDC_edge);
		
		return connectionsToAdd;
	}
	
	*/
	
	/*
	 * To get the correct network topology we must also account for branches
	 * between protrusions!
	 */
	/*
	private static void generateBranchesBetweenProtrusions(CollisionGrid cgGrid, double threshold, StromaEdge.TYPE type) {

		// remember that an arraylist will allow duplicate entries but a MAP
		// wont
		Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd = new HashMap<StromaEdge, ArrayList<StromaEdge>>();

		// set the environment and type of edge based on the boolean input FDC
		//StromaEdge.TYPE type; // the type of stromal cell
		Continuous3D env = null; // the stromal cell environment

		if (type == StromaEdge.TYPE.FDC_branch) {
			env = SimulationEnvironment.fdcEnvironment;

		} else if (type == StromaEdge.TYPE.RC_branch){
			env = SimulationEnvironment.brcEnvironment;
		}
		else if (type == StromaEdge.TYPE.MRC_branch){
			env = SimulationEnvironment.mrcEnvironment;

		}

		// iterate through all the objects in the environment
		Bag stroma = env.getAllObjects();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {
				// get all the neighbours within 15 microns away
				StromaEdge se = (StromaEdge) stroma.get(i);
				Double3D loc = se.getMidpoint(); // get the midpoint of the edge
				Bag neighbours = env.getNeighborsExactlyWithinDistance(loc, threshold, false);

				// update the connections map
				checkForBranchConnection(se, neighbours, threshold, loc, connectionsToAdd);

			}
		}
		// instantiate the edges in the connectionsToAdd map
		addBranch(connectionsToAdd, cgGrid, type);
	}

*/
	/**
	 * Check if two edges are connected by a branch
	 * 
	 * @param se
	 *            the stroma edge to add branches to
	 * @param neighbours
	 *            all stroma nodes and edges within a threshold distance
	 * @param threshold
	 *            the distance to search for neighbours
	 * @param loc
	 *            the location of the edge
	 * @param connectionsToAdd
	 *            the map to update
	 */
	/*
	private static void checkForBranchConnection(StromaEdge se, Bag neighbours, double threshold, Double3D loc,
			Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd) {

		// iterate through all of the neighbours
		for (int j = 0; j < neighbours.size(); j++) {

			// if its an edge and its not this stroma edge then
			if (neighbours.get(j) instanceof StromaEdge && !se.equals(neighbours.get(j))) {

				StromaEdge neighbour = ((StromaEdge) neighbours.get(j));
				Double3D neighbourloc = neighbour.getMidpoint();

				// if the midpoint is less than a certain distance away then add
				// a connection
				double dist = loc.distance(neighbourloc);
				if (dist < threshold && dist > 0) {

					// update the connection map
					if (connectionsToAdd.get(se) == null) {
						connectionsToAdd.put(se, new ArrayList<StromaEdge>());
					}

					connectionsToAdd.get(se).add(neighbour);

				}
			}
		}
	}

*/


	/**
	 * Add branches between stroma edges
	 * 
	 * @param connectionsToAdd
	 *            A map containing the edges to connect to
	 * @param cgGrid
	 *            the collision grid to add the branch to
	 * @param type
	 *            the type of branch we want to add to the grid
	 */
	/*
	private static void addBranch(Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd, CollisionGrid cgGrid,
			StromaEdge.TYPE type) {

		for (Map.Entry<StromaEdge, ArrayList<StromaEdge>> entry : connectionsToAdd.entrySet()) {

			// get the edge and the edges it is connected to
			StromaEdge se1 = entry.getKey();
			ArrayList<StromaEdge> sesToConnectTo = entry.getValue();

			for (StromaEdge se2 : sesToConnectTo) {

				// make sure there are no self connections
				if (!se1.equals(se2)) {

					// generate a branch between the two edges
					Double3D loc = se1.getMidpoint();
					Double3D neighbourloc = se2.getMidpoint();

					// TODO this needs to be sorted, pretty sure this will mess
					// up the location etc

					StromaEdge branch = new StromaEdge(loc, neighbourloc, type);
					branch.setObjectLocation(new Double3D(branch.x, branch.y, branch.z));

					SimulationEnvironment.scheduleStoppableCell(branch);
					branch.registerCollisions(cgGrid);

					// update of the connection info
					//updateNodeInfoAfterBranchAddition(se1, se2);

					// an edge can be connected to another edge if joined by a
					// branch
					//se1.m_Edges.add(se2);
					//se2.m_Edges.add(se1);

					//updateBranchConnectionInfo(se1, branch);
					//updateBranchConnectionInfo(se2, branch);

				}
			}
		}
	}

*/
	/**
	 * Update nodeInformation following the addition of a branch
	 * 
	 * @param se1
	 *            edge to add a branch to
	 * @param se2
	 *            edge to add a branch to
	 */
	/*
	private static void updateNodeInfoAfterBranchAddition(StromaEdge se1, StromaEdge se2) {
		// remember that each node will have a number of edges already
		for (Stroma node : se1.m_Nodes) {
			node.getM_Edges().add(se2); // update the nodes edges
			node.getM_Nodes().addAll(se2.m_Nodes); // update the nodes nodes
			node.getM_Nodes().remove(node); // make sure there are no self
											// connections
		}

		for (Stroma node : se2.m_Nodes) {
			node.getM_Edges().add(se1);
			node.getM_Nodes().addAll(se1.m_Nodes);
			// make sure there are no self connections
			node.getM_Nodes().remove(node);
		}
	}
	*/

	/**
	 * edges with multiple branches can affect topology so they need to be
	 * accounted for separately
	 * 
	 * @param se
	 *            the stroma edge to update info for
	 */
	/*
	private static void updateBranchConnectionInfo(StromaEdge se, StromaEdge branch) {
		// add the branch to the edge
		se.m_Branches.add(branch);

		// in this case an edge has multiple branches and this can affect
		// connectivity
		if (se.m_Branches.size() > 1) {

			// get each edge associated with an edge via branches
			for (StromaEdge se_connectedto : se.m_Edges) {

				// get the two nodes associated with this edge
				// and update the relevant node connections
				for (Stroma node : se_connectedto.m_Nodes) {

					// connect the node to the nodes associated with
					// the stroma edge this edge is connected to
					node.getM_Nodes().addAll(se.m_Nodes);
					// and vice versa
					for (Stroma node2 : se.m_Nodes) {
						node2.getM_Nodes().add(node);
					}
				}
			}
		}
	}
	
	*/
	
	/**
	 * In case we evr need to just read the stroma in straight from the node data
	 * @param cgGrid
	 */
	@SuppressWarnings("unused")
	private void readInStromaFromData(CollisionGrid cgGrid){
		
		ArrayList<Double3D> mrcnodes = new ArrayList<Double3D>();
		ArrayList<Double3D> fdcnodes = new ArrayList<Double3D>();
		ArrayList<Double3D> brcnodes = new ArrayList<Double3D>();
		
		
		try {
		mrcnodes = ParseStromaLocations.readStroma("/Users/jc1571/Desktop/mrcnodes.csv");
		fdcnodes = ParseStromaLocations.readStroma("/Users/jc1571/Desktop/fdcnodes.csv");
		brcnodes = ParseStromaLocations.readStroma("/Users/jc1571/Desktop/brcnodes.csv");
		
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		for(int i = 0 ; i < mrcnodes.size(); i ++){
			
			Stroma mrc = new Stroma(Stroma.TYPE.MRC, new Double3D(mrcnodes.get(i).x, mrcnodes.get(i).y, mrcnodes.get(i).z));
			mrc.setObjectLocation(new Double3D(mrcnodes.get(i).x, mrcnodes.get(i).y, mrcnodes.get(i).z));
			mrc.registerCollisions(cgGrid);
			SimulationEnvironment.scheduleStoppableCell(mrc);
			
			
		}
		
		for(int i = 0 ; i < fdcnodes.size(); i ++){
			
			Stroma fdc = new Stroma(Stroma.TYPE.FDC,new Double3D(fdcnodes.get(i).x, fdcnodes.get(i).y, fdcnodes.get(i).z));
			fdc.setObjectLocation(new Double3D(fdcnodes.get(i).x, fdcnodes.get(i).y, fdcnodes.get(i).z));
			fdc.registerCollisions(cgGrid);
			SimulationEnvironment.scheduleStoppableCell(fdc);
			
		}
		
		for(int i = 0 ; i < brcnodes.size(); i ++){
			
			Stroma brc = new Stroma(Stroma.TYPE.bRC,new Double3D(brcnodes.get(i).x, brcnodes.get(i).y, brcnodes.get(i).z));
			brc.setObjectLocation(new Double3D(brcnodes.get(i).x, brcnodes.get(i).y, brcnodes.get(i).z));
			brc.registerCollisions(cgGrid);
			SimulationEnvironment.scheduleStoppableCell(brc);	
		}
		
	}

}
