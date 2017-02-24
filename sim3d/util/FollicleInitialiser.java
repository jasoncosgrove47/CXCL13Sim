package sim3d.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.StromaGenerator.StromalCell;

/**
 * 
 * @author Jason Cosgrove
 */
public final class FollicleInitialiser {

	public static void initialiseFollicle(CollisionGrid cgGrid) {

		seedSCS(cgGrid);
		seedStroma(cgGrid);
		generateMRCNetwork(cgGrid);

		//use a threshold for one as distance required for a branch to form
		//these should be wrapped up in the seed stroma method
		generateBranchesBetweenProtrusions(cgGrid, 1.0,true);
		generateBranchesBetweenProtrusions(cgGrid, 1.0,false);


	}

	/**
	 * Seed the cells at the subcapuslar sinus
	 * 
	 * iterate through the X and Z axes keeping Y fixed to seed the SCS The MRCs
	 * are generated stochastically: we iterate through the X and Z axes keeping
	 * Y fixed to seed the SCS for each discrete location we generate a random
	 * number and if this exceeds some threshold value then we add an MRC
	 * location
	 * 
	 * @param cgGrid
	 */
	private static void seedSCS(CollisionGrid cgGrid) {

		// iterate through the X and Z axes keeping Y fixed to seed the SCS
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {

				// seed the lymphatic endothelium
				Stroma flec = new Stroma(Stroma.TYPE.LEC,new Double3D(x, Settings.HEIGHT - Settings.bRC.SCSDEPTH, z));
				Stroma clec = new Stroma(Stroma.TYPE.LEC,new Double3D(x, Settings.HEIGHT, z));

				// why isnt this already taken care of in the constructor
				clec.setObjectLocation(new Double3D(x, Settings.HEIGHT, z));
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - Settings.bRC.SCSDEPTH, z));

				// now seed the MRCs
				// this is done by probabalistically
				// placing MRC nodes under the SCS
				double random = Settings.RNG.nextDouble();
				if (random > 0.5) {
					Stroma mrc = new Stroma(Stroma.TYPE.MRC,new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 1.0), z));
					mrc.setObjectLocation(new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 1.0), z));
					mrc.registerCollisions(cgGrid);
					SimulationEnvironment.scheduleStoppableCell(mrc);
				}
			}
		}
	}

	/**
	 * This method is responsible for generating the stromal network
	 * 
	 * @param cgGrid
	 */
	private static void seedStroma(CollisionGrid cgGrid) {
		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> stromalCellLocations = new ArrayList<StromaGenerator.StromalCell>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		StromaGenerator.generateStroma3D_Updated(Settings.WIDTH - 2, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 2),
				Settings.DEPTH - 2, Settings.bRC.COUNT, stromalCellLocations, sealEdges);

		// now place the nodes and edges on the grid
		for (StromaGenerator.StromalCell sc : stromalCellLocations) {
			seedStromaNode(sc, cgGrid, sealEdges);
		}
		seedEdges(cgGrid, sealEdges);

	}

	/**
	 * Are two MRC nodes connected? we should really just be able to query some
	 * kind of data structure to know this.
	 * 
	 * @param p1
	 *            MRC node 1
	 * @param p2
	 *            MRC node 2
	 * @return
	 */
	/*
	 * private static boolean IsMRCAlreadyConnected(Double3D p1, Double3D p2){
	 * 
	 * //obtain all stromal cells within 40 microns //TODO this is very
	 * inefficient, why not get all MRCs? //might make sense to have them on
	 * different grids. Bag stroma =
	 * SimulationEnvironment.mrcEnvironment.getNeighborsWithinDistance(p1, 8);
	 * 
	 * //iterate through and see if we are conncted to anything else for (int i
	 * = 0; i < stroma.size(); i++) { if (stroma.get(i) instanceof StromaEdge) {
	 * //if its an MRC edge
	 * 
	 * //get the midpoint between the two MRCs? Double3D midpointToCheck = new
	 * Double3D((p1.x + p2.x) / 2, (p1.y + p2.y) / 2, (p1.z + p2.z) / 2);
	 * 
	 * //do any edges have the same midpoint i.e is there a connection between
	 * them if(((StromaEdge) stroma.get(i)).midpoint == midpointToCheck){ return
	 * true; } } }
	 * 
	 * return false; }
	 * 
	 */

	private static void seedMRCConnection(Map<Stroma, ArrayList<Stroma>> connectionsToAdd,CollisionGrid cgGrid) {

		
		
		for(Map.Entry<Stroma, ArrayList<Stroma>> entry: connectionsToAdd.entrySet()){
			
			
			Stroma sc = entry.getKey();
			ArrayList<Stroma> neighbours = entry.getValue();
			
			for(Stroma neighbour : neighbours ){
			
				//System.out.println("stroma type: " + sc.getStromatype());
				//System.out.println("neighbour type: " + neighbour.getStromatype());
				addEdgeBetweenNodes(sc, neighbour, cgGrid, StromaEdge.TYPE.MRC_edge);
			}

			}
			
		}
	
	
	/**
	 * TODO what is wrong with this method!!!!!!
	 * 
	 * 
	 * IM SURE WE ARE GETTING OVERLAPPING EDGES WITH THIS METHOD.
	 * 
	 * @param cgGrid
	 */
	private static void generateMRCNetwork(CollisionGrid cgGrid) {

		// TODO really inefficient to get all stromal cells, much more efficient
		// to give them their own grid...
		Bag stroma = SimulationEnvironment.mrcEnvironment.getAllObjects();
		
		Map<Stroma,ArrayList< Stroma>> connectionsToAdd = new HashMap<Stroma, ArrayList<Stroma>>();
		

		// iterate through all the stroma cells and check each MRC node
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) stroma.get(i);
				
				
				if(sc.getStromatype() == Stroma.TYPE.MRC){
					Double3D loc = new Double3D(sc.x, sc.y, sc.z);
					Bag neighbours = SimulationEnvironment.mrcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.0,
						false);

			
					//TODO this has gone mental we need to store the info somewhere sensible and then
					// generate it after the iterations. 
					addMRCConnections(neighbours, cgGrid, loc, sc, connectionsToAdd);
					// if the type of stromal cell is another MRC then generate a
					// new edge betwen them and
					// add to schedule etc

				}
			}
		}

		seedMRCConnection(connectionsToAdd,cgGrid);
		
		connectMRCtoRC(cgGrid);
	}

	private static void addMRCConnections(Bag neighbours, CollisionGrid cgGrid, Double3D loc, Stroma sc
			,Map<Stroma, ArrayList<Stroma>> connectionsToAdd) {
		// need to keep traack of who is connected with who
		for (int j = 0; j < neighbours.size(); j++) {
			if (neighbours.get(j) instanceof Stroma) {
				if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC) {

					if (!sc.equals(neighbours.get(j))) {

						Stroma neighbour = (Stroma) neighbours.get(j);

						// if its an MRC and nodes arent connected already
						if (neighbour.getStromatype() == Stroma.TYPE.MRC && 
								!Stroma.AreStromaNodesConnected(sc, neighbour)) {


							
							//addEdge(sc, neighbour, cgGrid, StromaEdge.TYPE.MRC_edge);

							if(connectionsToAdd.get(sc) == null){
								
								connectionsToAdd.put(sc, new ArrayList<Stroma>());
							}
							connectionsToAdd.get(sc).add(neighbour);
				
						}
					}

				}
			}
		}

	}

	/**
	 * We generate the reticular network seperately to the FDC network To
	 * generate connections between the different cells as we observe in vivo we
	 * make connections between stromal cells that are smaller than a threshold
	 * distance apart.
	 * 
	 * @param cgGrid
	 */
	private static void connectMRCtoRC(CollisionGrid cgGrid) {

		Bag stroma = SimulationEnvironment.mrcEnvironment.getAllObjects();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {

					// get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x, sc.y, sc.z);
					Bag neighbours = SimulationEnvironment.brcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.2,
							false);

					// if the type of stromal cell is an FDC then generate a new
					// edge betwen them and
					// add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {

					
							
								if (!sc.equals(neighbours.get(j)))
								// TODO really need to assess these equals comparisons
								// TODO: see
								// http://stackoverflow.com/questions/16069106/how-to-compare-two-java-objects
								addEdgeBetweenNodes(sc, (Stroma) neighbours.get(j), cgGrid, StromaEdge.TYPE.MRC_edge);

						
						}
					}
				}
			}
		}
	}

	/**
	 * Seed the reticular network from the arraylist generated by
	 * StromaGenerator3D
	 * 
	 * 
	 * the type should be sorted in the stromagenerator class so we dont have to
	 * deal with it
	 * 
	 * @param sc
	 * @param cgGrid
	 * @param edges
	 */
	private static void seedStromaNode(StromaGenerator.StromalCell sc, CollisionGrid cgGrid,
			ArrayList<StromaEdge> edges) {
		Double3D loc = new Double3D(sc.m_d3Location.x + 1, sc.m_d3Location.y + 1, sc.m_d3Location.z + 1);
		Stroma stromalcell = new Stroma(sc.m_type,loc);
		placeNode(sc, cgGrid, edges, stromalcell,loc);

	}

	private static void placeNode(StromaGenerator.StromalCell sc, CollisionGrid cgGrid, ArrayList<StromaEdge> edges,
			Stroma frc, Double3D loc) {

		// This will register the FRC with the environment/display
		// to account for the border which is one gridspace in width
		frc.setObjectLocation(loc);
		SimulationEnvironment.scheduleStoppableCell(frc);

		updateEdgeConnections(edges, loc, frc);
	}

	
	private static Stroma getNodeAtLocation(Continuous3D env, Double3D loc){
		Bag othernodesandedges = env.getObjectsAtLocation(loc);
		
		Stroma othernode = null;
		for(int i= 0; i < othernodesandedges.size(); i ++){
			if(othernodesandedges.get(i) instanceof Stroma){
				othernode = (Stroma) othernodesandedges.get(i);
				break;
			}
			
		}
		
		return othernode;

	}
	
	
	

	
	// update the edges and nodes and their connections
	// TODO we need to link associated nodes here also...
	/**
	 * 
	 * @param edges list of all the edge locations
	 * @param loc the frcs location
	 * @param frc
	 */
	private static void updateEdgeConnections(ArrayList<StromaEdge> edges, Double3D loc, Stroma frc) {

		
		
		// add associatedDendrites:TODO can encapsulate this as a seperate
		// method
		// this doesnt account for edges generated for MRCs
		for (StromaEdge seEdge : edges) {

			// get both edges of the
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x, seEdge.getPoint1().y, seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x, seEdge.getPoint2().y, seEdge.getPoint2().z);

			
			if (loc.x == edgeloc.x && loc.y == edgeloc.y && loc.z == edgeloc.z) {
				
				//get the stromal cell at the other end of the edge
				Stroma othernode = getNodeAtLocation(frc.getDrawEnvironment(), edgeloc2);
				frc.getM_Nodes().add(othernode);
				othernode.getM_Nodes().add(frc);
				
				
				frc.getM_Edges().add(seEdge);
				seEdge.m_Nodes.add(frc);
			} else if (loc.x == edgeloc2.x && loc.y == edgeloc2.y && loc.z == edgeloc2.z) {
				
				//get the stromal cell at the other end of the edge
				Stroma othernode = getNodeAtLocation(frc.getDrawEnvironment(), edgeloc2);
				frc.getM_Nodes().add(othernode);
				othernode.getM_Nodes().add(frc);
				
				frc.getM_Edges().add(seEdge);
				seEdge.m_Nodes.add(frc);
			}

	
		}
		
		//no edge should have more than two nodes at this point

	}

	/**
	 * Remove an RC and its associated dendrites from the grid and the schedule
	 * 
	 * @param frc
	 */
	private static void deleteRC(Stroma frc) {

		// remove the frc from grid and schedule
		frc.getDrawEnvironment().remove(frc);
		frc.stop();

		//now get rid of all associated edges, would need to update some of the connections if that
		// is the case
		for(StromaEdge se : frc.getM_Edges()){
			
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
	 * @param edges
	 * @param type
	 */
	private static void seedEdges(CollisionGrid cgGrid, ArrayList<StromaEdge> edges) {

		for (StromaEdge seEdge : edges) {
			

			//addEdgeBetweenNodes(sc1, sc2,  cgGrid, seEdge.getStromaedgetype());
			
			seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));
			Stroma sc1 = getNodeAtLocation(seEdge.getDrawEnvironment(),seEdge.getPoint1());
			Stroma sc2 = getNodeAtLocation(seEdge.getDrawEnvironment(),seEdge.getPoint1());
			
		
			//if a node doesnt have two associated nodes then we delete it. 
			if(sc1 != null && sc2 != null){
				
				addEdgeBetweenNodes(sc1,  sc2, cgGrid, seEdge.getStromaedgetype());
				
			}
			else{
				
				//System.out.println("doesnt have two nodes associated with the location");	
				//if these are both null then we should delete the edge surely?
				seEdge.getDrawEnvironment().remove(seEdge);
				
			}
			
		}
	}


	/*
	 * To get the correct network topology we must also account for branches
	 * between protrusions!
	 * 
	 * set FDC 
	 * 
	 */
	private static void generateBranchesBetweenProtrusions(CollisionGrid cgGrid, double threshold, boolean FDC) {


		StromaEdge.TYPE type;
		Continuous3D env;
		
		//not sure that this is sensible as wont allow duplicate entries
		Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd = new HashMap<StromaEdge, ArrayList<StromaEdge>>();
		
		
		//need some kind of map hi
		
		if(FDC){
			env = SimulationEnvironment.fdcEnvironment;
			type = StromaEdge.TYPE.FDC_branch;
			
		}else{
			env= SimulationEnvironment.brcEnvironment;
			type = StromaEdge.TYPE.RC_branch;
		}
	
		Bag stroma = env.getAllObjects();
		
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {


					// get all the neighbours within 20 microns away
					StromaEdge se = (StromaEdge) stroma.get(i);
					Double3D loc = se.midpoint; // get the midpoint of the edge
					Bag neighbours = env.getNeighborsExactlyWithinDistance(loc, 1.5,false);

					checkForBranchConnection(se, neighbours,  threshold, 
							loc, cgGrid, type,connectionsToAdd);

				}
			}
		
		//cant change agents when looping over a bag, gets you into all sorts of bother
		 addBranch(connectionsToAdd,cgGrid,type);
			
		}

	
	
	private static void checkForBranchConnection(StromaEdge se, Bag neighbours, double threshold, 
			Double3D loc,CollisionGrid cgGrid, StromaEdge.TYPE type,Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd){
		// need to check that we are not adding a branch to urselve
		for (int j = 0; j < neighbours.size(); j++) {
			// System.out.println("j: " + j);

			//if its an edge and its not this stroma edge then
			if (neighbours.get(j) instanceof StromaEdge && !se.equals(neighbours.get(j))) {

				StromaEdge neighbour = ((StromaEdge) neighbours.get(j));
				
				Double3D neighbourloc = neighbour.midpoint;

				// we dont want to add a node to ourselves

				//double threshold = 1.0;
				double dist = loc.distance(neighbourloc);
				if (dist < threshold && dist > 0) {
					
					if(connectionsToAdd.get(se) == null){
						connectionsToAdd.put(se, new ArrayList<StromaEdge>());
					}
		
					connectionsToAdd.get(se).add( neighbour);

				}

			}
		}
		
	}

	

		
	private static void addEdgeBetweenNodes(Stroma sc1, Stroma sc2, CollisionGrid cgGrid, StromaEdge.TYPE type) {

		
		Double3D loc = sc1.getM_location();
		Double3D neighbourloc = sc2.getM_location();

		StromaEdge seEdge = new StromaEdge(loc, neighbourloc, type);

		seEdge.setObjectLocation(new Double3D(seEdge.x , seEdge.y , seEdge.z ));

		SimulationEnvironment.scheduleStoppableCell(seEdge);
		seEdge.registerCollisions(cgGrid);

		sc1.getM_Edges().add(seEdge);
		seEdge.m_Nodes.add(sc1);

		sc2.getM_Edges().add(seEdge);
		seEdge.m_Nodes.add(sc2);


		// TODO we also need to add a connection between the two stromal cells.
		sc1.getM_Nodes().add(sc2);
		sc2.getM_Nodes().add(sc1);
		
	}
	
	
	
	
	
	private static void addBranch(Map<StromaEdge, ArrayList<StromaEdge>> connectionsToAdd,CollisionGrid cgGrid, StromaEdge.TYPE type) {

		
		for(Map.Entry<StromaEdge, ArrayList<StromaEdge>> entry: connectionsToAdd.entrySet()){
			
			
			StromaEdge se1 = entry.getKey();
			ArrayList<StromaEdge> se2aList = entry.getValue();
			
			for(StromaEdge se2 : se2aList){
			
				//make sure there are no self connections
				if(!se1.equals(se2)){
				
				Double3D loc = se1.midpoint;
				Double3D neighbourloc = se2.midpoint;
	
				StromaEdge seEdge = new StromaEdge(loc, neighbourloc, type);
	
				seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));
	
				SimulationEnvironment.scheduleStoppableCell(seEdge);
				seEdge.registerCollisions(cgGrid);
	
				
				//remember that each node will have a number of edges already
				for(Stroma node : se1.m_Nodes){
					//do we add the branch to the node or not?
					//for now lets just do it
	
					node.getM_Edges().add(se2);
					node.getM_Nodes().addAll(se2.m_Nodes);
					//make sure there are no self connections
					node.getM_Nodes().remove(node);
				}
				
				for(Stroma node : se2.m_Nodes){
					//do we add the branch to the node or not?
					//for now lets just do it. 
					//node.m_Edges.add(seEdge);				
					node.getM_Edges().add(se1);
					node.getM_Nodes().addAll(se1.m_Nodes);
					//make sure there are no self connections
					node.getM_Nodes().remove(node);
					
				}
				
				
				//an edge can be connected to another edge if joined by a branch
				se1.m_Edges.add(se2);
				se2.m_Edges.add(se1);
				
				//keep track of branches so we can assess if there are multiple connections
				se1.m_Branches.add(seEdge);
				se2.m_Branches.add(seEdge);
				
				
				checkForMultipleBranches(se1);
				checkForMultipleBranches(se2);
	
				}
				
		}
		}
		
	}
	
	
/**
 * edges with multiple branches can affect topology so they need to be accounted for separately
 * @param se
 */
	private static void checkForMultipleBranches(StromaEdge se){
		//in this case an edge has multiple branches and this can affect connectivity
		if(se.m_Branches.size() > 1 ){
			
			//get each edge associated with an edge via branches
			for(StromaEdge se_connectedto : se.m_Edges){
				
				//get the two nodes associated with this edge
				//and update the relevant node connections
				for(Stroma node : se_connectedto.m_Nodes){
					
					//connect the node to the nodes associated with 
					//the stroma edge this edge is connected to
					node.getM_Nodes().addAll(se.m_Nodes);
					// and vice versa
					for(Stroma node2 : se.m_Nodes){
						node2.getM_Nodes().add(node);
					}
				}			
			}	
		}
	
	}
	

	
	


}
