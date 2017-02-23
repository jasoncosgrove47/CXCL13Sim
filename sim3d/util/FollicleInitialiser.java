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
 * A Singleton class to generate follicular stroma "Computational Approach to 3D
 * Modeling of the Lymph Node Geometry", Computation, 2015.
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

		// iterate through all the stroma cells and check each MRC node
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) stroma.get(i);
				Double3D loc = new Double3D(sc.x, sc.y, sc.z);
				Bag neighbours = SimulationEnvironment.mrcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.0,
						false);

				addMRCConnections(neighbours, cgGrid, loc, sc);
				// if the type of stromal cell is another MRC then generate a
				// new edge betwen them and
				// add to schedule etc

			}
		}

		//connectMRCtoRC(cgGrid);
	}

	private static void addMRCConnections(Bag neighbours, CollisionGrid cgGrid, Double3D loc, Stroma sc) {
		// need to keep traack of who is connected with who
		for (int j = 0; j < neighbours.size(); j++) {
			if (neighbours.get(j) instanceof Stroma) {
				if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC) {

					if (!sc.equals(neighbours.get(j))) {

						Stroma neighbour = (Stroma) neighbours.get(j);

						// is the MRC connected to anything else
						if (!Stroma.AreStromaNodesConnected(sc, neighbour)) {

							addEdge(sc, neighbour, cgGrid, StromaEdge.TYPE.MRC_edge);

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

							if (sc.equals(neighbours.get(j)))
								// TODO really need
													// to assess
													// these equals
														// comparison
																// can lead to
																// trouble
								// TODO: see
								// http://stackoverflow.com/questions/16069106/how-to-compare-two-java-objects
								addEdge(sc, (Stroma) neighbours.get(j), cgGrid, StromaEdge.TYPE.MRC_edge);

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
		placeNode(sc, cgGrid, edges, stromalcell);

	}

	private static void placeNode(StromaGenerator.StromalCell sc, CollisionGrid cgGrid, ArrayList<StromaEdge> edges,
			Stroma frc) {
		Double3D loc = new Double3D(sc.m_d3Location.x + 1, sc.m_d3Location.y + 1, sc.m_d3Location.z + 1);
		// This will register the FRC with the environment/display
		// to account for the border which is one gridspace in width
		frc.setObjectLocation(loc);
		SimulationEnvironment.scheduleStoppableCell(frc);

		updateEdgeConnections(edges, loc, frc);
	}

	// update the edges and nodes and their connections
	// TODO we need to link associated nodes here also...
	private static void updateEdgeConnections(ArrayList<StromaEdge> edges, Double3D loc, Stroma frc) {

		// add associatedDendrites:TODO can encapsulate this as a seperate
		// method
		// this doesnt account for edges generated for MRCs
		for (StromaEdge seEdge : edges) {

			// get both edges of the
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x, seEdge.getPoint1().y, seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x, seEdge.getPoint2().y, seEdge.getPoint2().z);

			if (loc.x == edgeloc.x && loc.y == edgeloc.y && loc.z == edgeloc.z) {
				frc.m_Edges.add(seEdge);
				seEdge.m_Nodes.add(frc);
			} else if (loc.x == edgeloc2.x && loc.y == edgeloc2.y && loc.z == edgeloc2.z) {
				frc.m_Edges.add(seEdge);
				seEdge.m_Nodes.add(frc);
			}

			double distance = loc.distance(edgeloc);
			double distance2 = loc.distance(edgeloc2);

			// sometimes they are very close but not perfectly in the same
			// place.
			// so we set some threshold distance, this was determined
			// empirically
			double distanceThreshold = 0.1;
			if (distance < distanceThreshold || distance2 < distanceThreshold) {

				// this determines how many protrusions per node
				frc.m_Edges.add(seEdge);
				seEdge.m_Nodes.add(frc);
			}
		}

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

		// now this for all associated dendrites.
		for (int i = 0; i < frc.m_Edges.size(); i++) {
			frc.m_Edges.get(i).getDrawEnvironment().remove(frc.m_Edges.get(i));
			frc.m_Edges.get(i).stop();

		}
	}

	/**
	 * CHECK THE STROMAEDGE ARRAY TO MAKE SURE THERE ARE NO DUPLICATES
	 * 
	 * When generated stroma we can get overlapping edges, this method checks
	 * for overlaps and returns a new arraylist with all duplicated edges
	 * removed.
	 * 
	 * TODO: might be cleaner to have this in the generate stroma class
	 * 
	 * @param sealEdges
	 * @return
	 */
	/*
	 * private static ArrayList<StromaEdge> checkOverlaps(ArrayList<StromaEdge>
	 * sealEdges){
	 * 
	 * ArrayList<StromaEdge> updatedEdges = sealEdges; ArrayList<StromaEdge>
	 * edgesToRemove = new ArrayList<StromaEdge>();
	 * 
	 * for (int i = 0; i < sealEdges.size(); i++) { for (int j = i+1; j <
	 * sealEdges.size(); j++) {
	 * 
	 * //get the end points of each of the two edges Double3D i_p1 = new
	 * Double3D(sealEdges.get(i).x +1, sealEdges.get(i).y+1, sealEdges.get(i).z
	 * +1); Double3D i_p2 = new Double3D(sealEdges.get(i).getPoint2().x +1,
	 * sealEdges.get(i).getPoint2().y+1, sealEdges.get(i).getPoint2().z +1);
	 * 
	 * Double3D j_p1 = new Double3D(sealEdges.get(j).x +1, sealEdges.get(j).y+1,
	 * sealEdges.get(j).z +1); Double3D j_p2 = new
	 * Double3D(sealEdges.get(j).getPoint2().x +1,
	 * sealEdges.get(j).getPoint2().y+1, sealEdges.get(j).getPoint2().z +1);
	 * 
	 * //calculate the distances between all of the points. double d1 =
	 * calcDistance(i_p1, j_p1); double d2 = calcDistance(i_p1, j_p2); double d3
	 * = calcDistance(i_p2, j_p1); double d4 = calcDistance(i_p2, j_p2);
	 * 
	 * //if the cells are too close to one another then remove them // the
	 * threshold value was determined by trial and error double
	 * thresholdDistance = 0.15;
	 * 
	 * if(d1 < thresholdDistance && d4 < thresholdDistance){
	 * edgesToRemove.add(sealEdges.get(i)); } else if(d2 < thresholdDistance &&
	 * d3 < thresholdDistance){ edgesToRemove.add(sealEdges.get(i)); } } }
	 * 
	 * //now remove all of the overlapping edges in the removal list for(int x =
	 * 0; x < edgesToRemove.size(); x ++){
	 * 
	 * updatedEdges.remove(edgesToRemove.get(x)); } //return the updated array
	 * return updatedEdges; }
	 */

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

			// seEdge.setM_col(Settings.bRC.DRAW_COLOR());
			// Register with display and CG
			seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));

			SimulationEnvironment.scheduleStoppableCell(seEdge);
			seEdge.registerCollisions(cgGrid);

		}
	}

	// TODO DRY: this is in the other class: should we make a generic methods
	// class to make this cleaner...
	/**
	 * protected static double calcDistance2(Double3D i3Point1, Double3D
	 * i3Point2) { return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2) +
	 * Math.pow(i3Point1.y - i3Point2.y, 2) + Math.pow(i3Point1.z - i3Point2.z,
	 * 2))); }
	 */





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
		Map<StromaEdge, StromaEdge> connectionsToAdd = new HashMap<StromaEdge, StromaEdge>();
		
		
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

					addBranches(se, neighbours,  threshold, loc, cgGrid, type,connectionsToAdd);

				}
			}
		
		//cant change agents when looping over a bag, gets you into all sorts of bother
		 addBranch(connectionsToAdd,cgGrid,type);
			
		}

	
	
	private static void addBranches(StromaEdge se, Bag neighbours, double threshold, 
			Double3D loc,CollisionGrid cgGrid, StromaEdge.TYPE type,Map<StromaEdge, StromaEdge> connectionsToAdd){
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

					
					
					connectionsToAdd.put(se, neighbour);
					
					//TODO this is going to fuck up the bag iteration
					// NB need some way of dealing with this, for now its fine. 
					//AND so it has, well need to store all of the info somewhere we can use it later
					//addBranch(se, neighbour,cgGrid,type);
					
				}

			}
		}
		
	}

	
	private static void addBranch(Map<StromaEdge, StromaEdge> connectionsToAdd,CollisionGrid cgGrid, StromaEdge.TYPE type) {

		

		
		
		for(Map.Entry<StromaEdge, StromaEdge> entry: connectionsToAdd.entrySet()){
			
			
			StromaEdge se1 = entry.getKey();
			StromaEdge se2 = entry.getValue();
			Double3D loc = se1.midpoint;
			Double3D neighbourloc = se2.midpoint;

			StromaEdge seEdge = new StromaEdge(loc, neighbourloc, type);

			seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));

			SimulationEnvironment.scheduleStoppableCell(seEdge);
			seEdge.registerCollisions(cgGrid);

			//update branch connections
			se1.m_Branches.add(seEdge);
			se2.m_Branches.add(seEdge);
			
			//update edge connections
			se1.m_Edges.add(se2);
			se2.m_Edges.add(se1);
			
			//TODO how do we update node connections??
			// Need to consider instances where we have multiple branches connected together. 
			
			
			
		}
		

		
		
		


	}
	
	
	
	
	
	private static void addEdge(Stroma sc1, Stroma sc2, CollisionGrid cgGrid, StromaEdge.TYPE type) {

		
		Double3D loc = sc1.getM_location();
		Double3D neighbourloc = sc2.getM_location();

		StromaEdge seEdge = new StromaEdge(loc, neighbourloc, type);

		seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));

		SimulationEnvironment.scheduleStoppableCell(seEdge);
		seEdge.registerCollisions(cgGrid);

		sc1.m_Edges.add(seEdge);
		seEdge.m_Nodes.add(sc1);

		sc2.m_Edges.add(seEdge);
		seEdge.m_Nodes.add(sc2);


		// TODO we also need to add a connection between the two stromal cells.
		sc1.m_Nodes.add(sc2);
		sc2.m_Nodes.add(sc1);
		
	}

}
