package sim3d;


import java.util.ArrayList;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.cell.Lymphocyte;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.StromaGenerator;

/**
 * Initialise the follicle microenvironment, generating stroma and the
 * subcapsular sinus. This class also handles data handling for stroma recording
 * which nodes are connected to which edges and vice versa.
 * 
 * @author Jason Cosgrove
 */
public final class FollicleInitialiser {

	/**
	 * This method sets up the stroma within the follicle and the subcapsular
	 * sinus.
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 */
	public static void initialiseFollicle(CollisionGrid cgGrid) {

		GenerateSCS.seedSCS();
		generateStromalNetwork(cgGrid);
		GenerateSCS.generateMRCNetwork(cgGrid);
		shapeFDCNetwork(cgGrid);

		if (Settings.calculateTopologyData) {
			generateBranches(cgGrid, true, true, false);
			printNodeNumbers();
		}

		updateNodeConnections();

	}

	/**
	 * Generate branches between stroma edges
	 * 
	 * @param cgGrid
	 *            the collision grid to add the branch to
	 * @param threshold
	 *            distance threshold to make a branch connection
	 * @param FDC
	 *            boolean to generate branches between FDCs
	 * @param BRC
	 *            boolean to generate branches between BRCs
	 * @param MRC
	 *            boolean to generate branches between MRCs
	 */
	private static void generateBranches(CollisionGrid cgGrid, boolean FDC, boolean BRC, boolean MRC) {

		if (FDC) {
			generateBranchesDynamically(cgGrid, SimulationEnvironment.fdcEnvironment, StromaEdge.TYPE.FDC_edge, 18, 5,
					10);
		}
		//18,5,9

		if (BRC) {
			generateBranchesDynamically(cgGrid, SimulationEnvironment.brcEnvironment, StromaEdge.TYPE.RC_edge, 4, 4, 10);//was 6,4,5
		} //6,4,5

		if (MRC) {
			generateBranchesDynamically(cgGrid, SimulationEnvironment.mrcEnvironment, StromaEdge.TYPE.MRC_edge, 4, 0,
					4); //was 4,0,4

		}//4,0,4
	}

	

	
	
	/**
	 * Initialise the FDC network,
	 * 
	 * @param cgGrid
	 *            the collisionGrid to add the nodes and edges to
	 */
	private static void shapeFDCNetwork(CollisionGrid cgGrid) {

		
		//get all FDCs and add connections based on a distance threhsold
		Bag FDCs = SimulationEnvironment.fdcEnvironment.getAllObjects();

		for (int i = 0; i < FDCs.size(); i++) {
			if (FDCs.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) FDCs.get(i);

				Bag neighbours = SimulationEnvironment.getAllStromaWithinDistance(sc.getM_Location(), 3.0);

				for (int j = 0; j < neighbours.size(); j++) {
					if (neighbours.get(j) instanceof Stroma) {
						if (!sc.getM_Nodes().contains(neighbours.get(j))) {

							Stroma neighbour = (Stroma) neighbours.get(j);
							if (neighbour.getStromatype() == Stroma.TYPE.FDC) {

								//make sure the edge doesnt already exist
								if (doesEdgeAlreadyExist(sc, neighbour) == false) {
									//if you allow edges to pass straight through nodes you end up with
									// an incorrect morphology
									if (checkForPointsInTheWay(sc, neighbour, 8) == false) {

										StromaEdge senew = new StromaEdge(sc.getM_Location(), neighbour.getM_Location(),
												StromaEdge.TYPE.FDC_edge);

										senew.setObjectLocation(new Double3D(senew.x, senew.y, senew.z));

										SimulationEnvironment.scheduleStoppableEdge(senew);
										senew.registerCollisions(cgGrid);

										senew.m_Nodes.add(sc);
										senew.m_Nodes.add(neighbour);
										sc.getM_Edges().add(senew);
										neighbour.getM_Edges().add(senew);
										sc.getM_Nodes().add(neighbour);
										neighbour.getM_Nodes().add(sc);

									}
								}
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Select a random edge from a bag of neighbouring cells and add it to an
	 * arrayList
	 * 
	 * @param edgeConnectionsToAdd
	 * 				an arraylist of stroma edges
	 * @param neighbours
	 * 				a bag of stroma
	 * @param sc
	 * 				a stromal cell
	 * @param random_index
	 * 				a random integer between 0 and the size of the bag
	 * @return
	 * 				an arraylist of stroma edges
	 */
	private static ArrayList<StromaEdge> updateEdgeConnections(ArrayList<StromaEdge> edgeConnectionsToAdd,
			Bag neighbours, Stroma sc, int random_index) {
		StromaEdge neighbour = (StromaEdge) neighbours.get(random_index);
		// make sure we dont already have this protrusion, we check for branches
		// later on.
		if (!sc.getM_Edges().contains(neighbour)) {
			edgeConnectionsToAdd.add(neighbour);
		}
		return edgeConnectionsToAdd;

	}

	/**
	 * Select a random node from a bag of neighbouring cells and add it to an
	 * arrayList
	 * 
	 * @param nodeConnectionsToAdd
	 * 					an arraylist of stromal cells
	 * @param neighbours
	 * 					a Bag of stroma
	 * @param sc
	 * 					a stromal cell
	 * @param random_index
	 * 					a random value ranging between 0 and the size of the bag
	 * @return
	 * 					an arraylist of stromal cells
	 */
	private static ArrayList<Stroma> updateNodeList(ArrayList<Stroma> nodeConnectionsToAdd, 
			Bag neighbours, Stroma sc,int random_index) {
		Stroma neighbour = (Stroma) neighbours.get(random_index);

		if (neighbour.getStromatype() != Stroma.TYPE.LEC) {
			// i think we need to do a check to make sure there are no
			// neighbours in the way
			if (!neighbour.equals(sc) && !sc.getM_Nodes().contains(neighbour)) {
				if (!checkForPointsInTheWay(sc, neighbour, 0.35)) {
					nodeConnectionsToAdd.add(neighbour);
				}
			}
		}

		return nodeConnectionsToAdd;
	}

	/**
	 * Check if there is a branch between two stromaEdges.
	 * 
	 * @param neighbour
	 * 			a stromal cell
	 * @param sc
	 * 			a stromal cell
	 * @return
	 * 			a boolean stating whether an edge exists between two nodes
	 */
	private static boolean doesEdgeAlreadyExist(Stroma sc, Stroma neighbour) {
		// need to see if there is not already a branch in place
		boolean doesBranchExist = false;

		if (!sc.getM_Edges().isEmpty()) {
			for (StromaEdge branch : sc.getM_Edges()) {

				Double3D p1 = neighbour.getM_Location();
				Double3D p2 = sc.getM_Location();

				Double3D p3 = branch.getPoint1();
				Double3D p4 = branch.getPoint2();

				if (p1.distance(p3) < Settings.DOUBLE3D_PRECISION || 
					p1.distance(p4) < Settings.DOUBLE3D_PRECISION) {
					if (p2.distance(p3) < Settings.DOUBLE3D_PRECISION
							|| p2.distance(p4) < Settings.DOUBLE3D_PRECISION) {
						doesBranchExist = true;
					}
				}
			}
		}

		return doesBranchExist;
	}

	/**
	 * Check if there is a branch between two stromaEdges.
	 * 
	 * @param neighbour
	 * 				a stromal cell
	 * @param nearestProtrusion
	 * 				a stromal edge
	 * @return
	 * 				a boolean on whether the edge and the node are connected
	 */
	private static boolean doesBranchAlreadyExist(StromaEdge neighbour, StromaEdge nearestProtrusion) {
		// need to see if there is not already a branch in place
		boolean doesBranchExist = false;

		if (!nearestProtrusion.m_Branches.isEmpty()) {
			for (StromaEdge branch : nearestProtrusion.m_Branches) {
				Double3D p1 = neighbour.getMidpoint();
				Double3D p2 = nearestProtrusion.getMidpoint();

				Double3D p3 = branch.getPoint1();
				Double3D p4 = branch.getPoint2();

				if (p1.distance(p3) < Settings.DOUBLE3D_PRECISION || 
					p1.distance(p4) < Settings.DOUBLE3D_PRECISION) {
					if (p2.distance(p3) < Settings.DOUBLE3D_PRECISION
							|| p2.distance(p4) < Settings.DOUBLE3D_PRECISION) {
						doesBranchExist = true;
					}
				}
			}
		}

		return doesBranchExist;
	}

	/**
	 * Return the closest protrusion between a stromal cell and a neighbouring
	 * stromaEdge
	 * 
	 * @param sc
	 *            the stromal cell
	 * @param neighbour
	 *            a neighbouring stroma edge
	 * @return the closest protrusion
	 */
	private static StromaEdge getClosestProtrusion(Stroma sc, StromaEdge neighbour) {

		double minDist = 10;
		StromaEdge nearestProtrusion = null;

		Double3D p1 = neighbour.getPoint1();
		Double3D p2 = neighbour.getPoint2();

		for (StromaEdge se : sc.getM_Edges()) {
			boolean connect = true;

			double dist = se.getMidpoint().distance(neighbour.getMidpoint());
			Double3D p3 = se.getPoint1();
			Double3D p4 = se.getPoint2();

			// need to check that the midpoints of one branch
			// dont correspond to the end points of another
			if (p1.distance(se.getMidpoint()) < Settings.DOUBLE3D_PRECISION
					|| p2.distance(se.getMidpoint()) < Settings.DOUBLE3D_PRECISION) {
				connect = false;
			}
			if (p3.distance(neighbour.getMidpoint()) < Settings.DOUBLE3D_PRECISION
					|| p4.distance(neighbour.getMidpoint()) < Settings.DOUBLE3D_PRECISION) {
				connect = false;
			}

			if (dist < minDist && connect) {
				minDist = dist;
				nearestProtrusion = se;
			}
		}

		return nearestProtrusion;
	}


	/**
	 * add a protrusion between two stromal cells
	 * @param neighbour
	 * 					a stromal cell
	 * @param loc
	 * 					the location of the stromal cell
	 * @param cgGrid
	 * 					the collision grid
	 * @param type
	 * 					the type of edge to add
	 * @param sc
	 * 					a stromal cell
	 */
	private static void addProtrusion(Stroma neighbour, Double3D loc, CollisionGrid cgGrid, StromaEdge.TYPE type,
			Stroma sc) {
		Double3D neighbourloc = neighbour.getM_Location();

		StromaEdge branch = new StromaEdge(loc, neighbourloc, type);
		branch.setObjectLocation(new Double3D(branch.x, branch.y, branch.z));

		SimulationEnvironment.scheduleStoppableEdge(branch);

		branch.registerCollisions(cgGrid);
		// its a protrusion so its fine to add to the protrusions list

		sc.getM_Edges().add(branch);
		neighbour.getM_Edges().add(branch);
		branch.m_Nodes.add(sc);
		branch.m_Nodes.add(neighbour);
	}

	
	
	/**
	 * 
	 * add connections between stromal cells
	 * @param neighbours
	 * 			A bag of stroma objects
	 * @param sc
	 * 			A stromal cell
	 * @param loc
	 * 			a double3D
	 * @param type
	 * 			a type of stromal cells
	 * @param cgGrid
	 * 			a collision grid
	 */
	private static void addConnections(Bag neighbours, Stroma sc, Double3D loc, StromaEdge.TYPE type,
			CollisionGrid cgGrid) {
		ArrayList<Stroma> nodeConnectionsToAdd = new ArrayList<Stroma>();
		ArrayList<StromaEdge> edgeConnectionsToAdd = new ArrayList<StromaEdge>();

		int random_index = Settings.RNG.nextInt(neighbours.size());
		if (neighbours.get(random_index) instanceof Stroma) {

			nodeConnectionsToAdd = updateNodeList(nodeConnectionsToAdd, neighbours, sc, random_index);

		} else { // then we have an edge
			edgeConnectionsToAdd = updateEdgeConnections(edgeConnectionsToAdd, neighbours, sc, random_index);
		}

		if (!nodeConnectionsToAdd.isEmpty()) { 

			// in this case we are adding a protrusion
			for (Stroma neighbour : nodeConnectionsToAdd) {
				boolean doesBranchExist = doesEdgeAlreadyExist(sc, neighbour);
				if (!doesBranchExist) {
					addProtrusion(neighbour, loc, cgGrid, type, sc);
				}
			}
		}

		if (!edgeConnectionsToAdd.isEmpty()) {

			// find the closest protrusion

			StromaEdge neighbour = (StromaEdge) neighbours.get(random_index);
			StromaEdge nearestProtrusion = getClosestProtrusion(sc, neighbour);

			if (nearestProtrusion != null) {

				
				//what is all this checking, that we dont allow connections between MRCs?
				boolean proceed = true;
				if (sc.getStromatype() == Stroma.TYPE.MRC) {

					for (Stroma node : neighbour.m_Nodes) {
						if (node.getStromatype() == Stroma.TYPE.MRC) {
							proceed = false;
						}
					}

					for (Stroma node : nearestProtrusion.m_Nodes) {
						if (node.getStromatype() == Stroma.TYPE.MRC) {
							proceed = false;
						}
					}
				}

				if (proceed) {
					// need to see if there is not already a branch in place
					boolean doesBranchExist = doesBranchAlreadyExist(neighbour, nearestProtrusion);

					Double3D branchloc = new Double3D((neighbour.getMidpoint().x + nearestProtrusion.x) / 2,
							(neighbour.getMidpoint().y + nearestProtrusion.y) / 2,
							(neighbour.getMidpoint().z + nearestProtrusion.z) / 2);

					Bag stromaInVicinity = sc.getDrawEnvironment().getNeighborsExactlyWithinDistance(branchloc, 1);

					double threshold = 0;
					if (sc.getStromatype() == Stroma.TYPE.FDC) {
						threshold = 2;
					} else {
						threshold = 1;
					}

					if (!doesBranchExist && stromaInVicinity.size() < threshold) {

						StromaEdge branch = new StromaEdge(neighbour.getMidpoint(), nearestProtrusion.getMidpoint(),
								type);
						branch.setObjectLocation(new Double3D(branch.x, branch.y, branch.z));

						SimulationEnvironment.scheduleStoppableEdge(branch);

						branch.registerCollisions(cgGrid);
						neighbour.m_Branches.add(branch);
						nearestProtrusion.m_Branches.add(branch);
						branch.m_Edges.add(neighbour);
						branch.m_Edges.add(nearestProtrusion);
					}

					updateNodeConnectionForNodeOtherGrids(sc);

				}
			}
		}

	}

	/**
	 * Dynamically add branches until some threshold (with mean and sd supplied
	 * is reached) We also set a max value to make sure the degree doesnt get
	 * too high
	 * 
	 * @param cgGrid
	 * 				a collision grid
	 * @param env
	 * 				the environment ot query
	 * @param type
	 * 				the type of stromal edge
	 * @param mean
	 * 				the mean number of branches to make
	 * @param sd
	 * 				the standard deviation of branches to make
	 * @param maxVal
	 * 				the max number of branches to make
	 */
	private static void generateBranchesDynamically(CollisionGrid cgGrid, Continuous3D env, StromaEdge.TYPE type,
			int mean, int sd, int maxVal) {

		Bag RCs = env.getAllObjects();

		for (int i = 0; i < RCs.size(); i++) {
			if (RCs.get(i) instanceof Stroma) {

				if (i % 30 == 0) {
					updateNodeConnections();
				}

				Stroma sc = (Stroma) RCs.get(i);
				if (sc.getStromatype() != Stroma.TYPE.LEC) {

					int targetConnections = 0;

					if (sc.getStromatype() == Stroma.TYPE.FDC) {


						targetConnections = (int) (Settings.RNG.nextInt(sd) + mean);

					} else if (sc.getStromatype() == Stroma.TYPE.bRC) {
						targetConnections = (int) (Settings.RNG.nextInt(sd) + mean);

					}

					else {
						// dont allow negative numbers hi
						targetConnections = (int) (Settings.RNG.nextGaussian() * sd + mean);
					}
					Double3D loc = sc.getM_Location(); // get the midpoint of the edge

					Bag neighbours = new Bag();
					neighbours = env.getNeighborsExactlyWithinDistance(loc, 3.5, false);//was 3.5


					int count = 0;

					while (sc.getM_Nodes().size() < targetConnections && count < 200) {
						count += 1;
						// what do we do if its a branch
						addConnections(neighbours, sc, loc, type, cgGrid);
					}

					// make sure the node doesnt have too many connections
					if (sc.getM_Nodes().size() > maxVal) {
						pruneConnections(sc, maxVal);
					}
				}
			}
		}
	}

	/**
	 * If a stromal cell is connected to do many other nodes, stochastically
	 * delete some branches until it has degree less than value maxVal
	 * 
	 * @param sc
	 *            the stromal cell to prune
	 * @param maxVal
	 *            the max value of degree centrality per node
	 */
	private static void pruneConnections(Stroma sc, int maxVal) {
		for (StromaEdge se : sc.getM_Edges()) {
			if (sc.getM_Nodes().size() < maxVal)
				break;
			ArrayList<StromaEdge> branchesToRemove = new ArrayList<StromaEdge>();
			for (StromaEdge branch : se.m_Branches) {
				branchesToRemove.add(branch);
			}

			// now actually get rid of the nodes
			for (StromaEdge branch : branchesToRemove) {
				if (sc.getM_Nodes().size() < maxVal)
					break;
				
				deleteSEdge(branch);

				updateNodeConnectionForNodeOtherGrids(sc);

			}

		}
	}
	

	

	/**
	 * Print the number of stroma nodes in the simulaiton to console
	 */
	private static void printNodeNumbers() {

		System.out.println("FDC numbers: " + countNodes(SimulationEnvironment.fdcEnvironment, Stroma.TYPE.FDC));
		System.out.println("BRC numbers: " + countNodes(SimulationEnvironment.brcEnvironment, Stroma.TYPE.bRC));
		System.out.println("MRC numbers: " + countNodes(SimulationEnvironment.mrcEnvironment, Stroma.TYPE.MRC));
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
	 *            
	 */
	static void instantiateStromalCell(CollisionGrid cgGrid, Stroma.TYPE type, Double3D loc) {
		Stroma sc = new Stroma(type, loc);
		sc.setObjectLocation(loc);
		sc.registerCollisions(cgGrid);
		SimulationEnvironment.scheduleStoppableCell(sc);
	}

	/**
	 * update connection data for each node and edge
	 */
	private static void updateNodeConnections() {

		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) stroma.get(i);

				if (sc.getStromatype() != Stroma.TYPE.LEC) {
					updateNodeConnectionForNodeOtherGrids(sc);
				}
			}
		}
	}

	/**
	 * Given two stroma nodes, check if there are any nodes directly between them 
	 * i.e. a threshold distance away from a straight line connecting the two nodes 
	 * 
	 * note this only checks for one grid, need a method to check across
	 * multiple grids
	 * 
	 * @param sc
	 * 			a stromal cell
	 * @param neighbour
	 * 			a stromal cell
	 * @return
	 * 			a boolean on whether there is a node in the way
	 */
	static boolean checkForPointsInTheWay(Stroma sc, Stroma neighbour, double threshold) {

	
		// check to see if there are any cells blocking the path.
		Double3D midpoint = new Double3D((sc.x + neighbour.x) / 2, (sc.y + neighbour.y) / 2, (sc.z + neighbour.z) / 2);

		Bag pointsInTheWay = sc.getDrawEnvironment().getNeighborsExactlyWithinDistance(midpoint,
				midpoint.distance(sc.getM_Location()));
		boolean pathBlocked = false;

		for (int x = 0; x < pointsInTheWay.size(); x++) {
			if (pointsInTheWay.get(x) instanceof Stroma) {

				Stroma point = (Stroma) pointsInTheWay.get(x);

				if (!point.equals(sc) && !point.equals(neighbour) && point.getStromatype() != Stroma.TYPE.LEC) {

					// this methods returns the double3D on the line segment
					// that is closest to the point p3
					Double3D closestDist = Lymphocyte.closestPointToStroma(sc.getM_Location(),
							neighbour.getM_Location(), point.getM_Location());

					// this is the distance between point and the line specified
					// between sc and neighbour
					double dist = closestDist.distance(point.getM_Location());

					// check to see if there is any node in the way
					pathBlocked = dist < threshold;

					if (pathBlocked) {
						break;
					}

				}
			}
		}
		return pathBlocked;
	}

	/**
	 * Check to see what nodes our node is connected to.
	 * 
	 * @param sc
	 * 			a stromal cell
	 */
	static void updateNodeConnectionForNodeOtherGrids(Stroma sc) {

		// these are ordered lists, as such it is safe to update them, whilst
		// iterating
		// over them.
		ArrayList<StromaEdge> tempEdges = new ArrayList<StromaEdge>();
		ArrayList<Double3D> locations = new ArrayList<Double3D>();

		// update tempedges and locations with all of the
		// node protrusions
		initialiseConnectionLists(sc, locations, tempEdges);

		// check each point we are connected to and see what nodes are there
		for (int i = 0; i < locations.size(); i++) {

			Double3D newloc = locations.get(i);

			// see if there is a node at the location, the true is to search all
			// grids
			Stroma neighbour = getNodeAtLocation(sc.getDrawEnvironment(), newloc, true);

			// are there any nodes associated with this newloc, update the
			// relevant arrays
			// sometimes we pick up an LEC on the way, make sure that we are not
			// updating
			// between the same node
			if (neighbour != null && !neighbour.equals(sc) && neighbour.getStromatype() != Stroma.TYPE.LEC) {
				// if there is a stromal cell at this location

				// if there are no nodes blocking the way, lets say stroma nodes
				// are 13 microns diameter
				if (checkForPointsInTheWay(sc, neighbour, 0.35) == false) { // was
																			// 0.35

					// lets constrain the distances we can add to be less than
					// 100 microns
					// in marios analysis you cant tell if they are connected
					// once a certain distance apart so easiest
					// to cnstrain here

					if (!sc.getM_Nodes().contains(neighbour)) {
						sc.getM_Nodes().add(neighbour);
					}
					if (!neighbour.getM_Nodes().contains(sc)) {
						neighbour.getM_Nodes().add(sc);
					}
				}

			} else { // if there are no nodes we need to get all edges
						// associated with this edge
						// so that we can check to see if they have any nodes
						// attached to them.

				// we would want to query the other grid to see if htere is
				// anode there hi
				// this code does not account for branches
				addConnectedEdges(sc, newloc, locations, tempEdges, true);

			}
		}
	}

	/**
	 * Helper method to update the locations and tempEdges lists with the
	 * protrusions for each stroma
	 * 
	 * @param sc
	 * 					a stromal cell
	 * @param locations
	 * 					an arraylist of double3Ds
	 * @param tempEdges
	 * 					an arraylist of stroma edges 
	 */
	private static void initialiseConnectionLists(Stroma sc, ArrayList<Double3D> locations,
			ArrayList<StromaEdge> tempEdges) {
		// update tempedges and locations with all of the
		// node protrusions
		tempEdges.addAll(sc.getM_Edges());
		for (StromaEdge se : tempEdges) {

			if (!locations.contains(se.getPoint1())) {
				locations.add(se.getPoint1());
			}
			if (!locations.contains(se.getPoint2())) {
				locations.add(se.getPoint2());
			}
			if (!locations.contains(se.getMidpoint())) {
				locations.add(se.getMidpoint());
			}
		}
	}

	/**
	 * Helper method to update the locations and tempEdges list when assigning
	 * which nodes are associated together.
	 * 
	 * @param otherEdge
	 * 					a stroma edge
	 * @param locations
	 * 					an arraylist of double3Ds
	 * @param tempEdges
	 * 					an arraylist of stroma edges 
	 */
	private static void updateConnectionLists(StromaEdge otherEdge, ArrayList<Double3D> locations,
			ArrayList<StromaEdge> tempEdges) {
		if (!tempEdges.contains(otherEdge)) {
			tempEdges.add(otherEdge);
			if (!locations.contains(otherEdge.getPoint1())) {
				locations.add(otherEdge.getPoint1());
			}
			if (!locations.contains(otherEdge.getPoint2())) {
				locations.add(otherEdge.getPoint2());
			}
			if (!locations.contains(otherEdge.getMidpoint())) {
				locations.add(otherEdge.getMidpoint());
			}

		}
	}

	/**
	 * Helper method that looks for all edges associated with another edge
	 * 
	 * @param sc
	 * 			a stromal cell
	 * @param newloc
	 * 			a Double3D
	 * @param locations
	 * 			an arraylist of locations
	 * @param tempEdges
	 *  		an arraylist of stroma edges
	 * @param allGrids
	 * 			set to true if you want to query all grids
	 */
	private static void addConnectedEdges(Stroma sc, Double3D newloc, ArrayList<Double3D> locations,
			ArrayList<StromaEdge> tempEdges, boolean allGrids) {

		// this code does not account for branches
		double threshold = Settings.DOUBLE3D_PRECISION;

		// get the edges connected to this edge
		Bag otherEdges = new Bag();

		if (allGrids) {
			otherEdges = SimulationEnvironment.getAllStromaWithinDistance(newloc, 5);

		} else {
			otherEdges = sc.getDrawEnvironment().getNeighborsExactlyWithinDistance(newloc, 5);
		}
		
		
		//iterate through and see if the edges are sufficiently close enough to be connected
		for (int j = 0; j < otherEdges.size(); j++) {
			if (otherEdges.get(j) instanceof StromaEdge) {

				StromaEdge otherEdge = (StromaEdge) otherEdges.get(j);

				if (otherEdge.getPoint1().distance(newloc) < threshold
						|| otherEdge.getPoint2().distance(newloc) < threshold
						|| otherEdge.getMidpoint().distance(newloc) < threshold) {

					if (!tempEdges.contains(otherEdge)) {

						updateConnectionLists(otherEdge, locations, tempEdges);

					}
				}
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

			seedStromaNode(sc, cgGrid, sealEdges);

		}

		// when we instantiate the edges we change their location, so this needs
		// to be accounted for
		ArrayList<StromaEdge> updatedEdges = seedEdges(cgGrid, sealEdges);

		// set thsi to null so we cant use it again anywhere.
		sealEdges = null;


		Bag stroma2 = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma2.size(); i++) {
			if (stroma2.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) stroma2.get(i);

				if (sc.getStromatype() != Stroma.TYPE.LEC) {
					// the MRC protrusions are assigned in the other class
					assignProtrusions(updatedEdges, sc);

				}
			}
		}
	}

	/**
	 * For a given stroma type return the number of nodes of that type on the
	 * grid env
	 * 
	 * @param env
	 *            the grid to query
	 * @param type
	 *            the subset of stromal cells to count
	 * @return 
	 * 				the number of stroma of a given type on a given grid
	 */
	private static int countNodes(Continuous3D env, Stroma.TYPE type) {

		int iCounter = 0;
		Bag stroma = env.getAllObjects();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				// MRCs also have LECs on the same grid so we should do an
				// additional check
				if (type == Stroma.TYPE.MRC) {
					Stroma sc = (Stroma) stroma.get(i);
					if (sc.getStromatype() != Stroma.TYPE.LEC) {
						iCounter += 1;
					}
				} else {
					iCounter += 1;
				}
			}
		}
		return iCounter;
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
		Double3D d3loc = new Double3D(sc.m_d3Location.x + 1, sc.m_d3Location.y + 1, sc.m_d3Location.z + 1);
		Stroma stromalcell = new Stroma(sc.m_type, d3loc);
		stromalcell.setObjectLocation(d3loc);
		SimulationEnvironment.scheduleStoppableCell(stromalcell);
	}

	/**
	 * Given a double3D return the stroma node at that location if there is
	 * nothing there then return null.
	 * 
	 * 
	 * @param env 
	 * 				the grid to query
	 * @param loc 
	 * 				the Double3D location to query
	 * @param allGrids 
	 * 				set to true if you want to query all grids
	 * 
	 * @return 	
	 * 				a stromal cell if it exists, otherwise return null
	 */
	private static Stroma getNodeAtLocation(Continuous3D env, Double3D loc, boolean allGrids) {

		Bag othernodesandedges = new Bag();
		if (allGrids) {
			othernodesandedges = SimulationEnvironment.getAllStromaWithinDistance(loc, 0.1);
		} else {
			othernodesandedges = env.getNeighborsExactlyWithinDistance(loc, 0.1);
		}
		Stroma othernode = null;

		// should allow some give with this as they might not be exactly placed
		// correctly
		double smallestDist = 0.1;


		if (othernodesandedges != null) {

			for (int i = 0; i < othernodesandedges.size(); i++) {
				if (othernodesandedges.get(i) instanceof Stroma) {

					othernode = (Stroma) othernodesandedges.get(i);

					if (othernode.getStromatype() != Stroma.TYPE.LEC) {

						// what do we do if we have multiple
						double dist = ((Stroma) othernodesandedges.get(i)).getM_Location().distance(loc);

						if (dist < smallestDist) {
							smallestDist = dist;
							othernode = (Stroma) othernodesandedges.get(i);
						}
					}
				}
			}
		}
		return othernode;
	}

	/**
	 * Remove an RC and its associated dendrites from the grid and the schedule
	 * 
	 * @param sc
	 *            the stromal cell to delete          
	 */
	public static void deleteSC(Stroma sc) {

		// now get rid of all associated edges, would need to update
		// some of the connections if that is the case
		for (StromaEdge se : sc.getM_Edges()) {
			deleteSEdge(se);
		}

		// remove the frc from grid and schedule
		sc.getDrawEnvironment().remove(sc);
		sc.stop();
		sc.setM_isStatic(false);

	}

	/**
	 * Remove an RC and its associated dendrites from the grid and the schedule
	 * 
	 * @param se
	 *            the stroma edge to delete
	 */
	public static void deleteSEdge(StromaEdge se) {

		// remove the frc from grid and schedule
		se.getDrawEnvironment().remove(se);
		se.stop();
		se.setM_isStatic(false);

		// make sure the edges know we're not connected
		for (StromaEdge edge : se.m_Edges) {
			edge.m_Branches.remove(se);
		}

		for (Stroma sc : se.m_Nodes) {// each edge should only know its two
										// associated nodes
			sc.getM_Edges().remove(se);
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
	 * 			  an arraylist of stroma edges
	 */
	private static ArrayList<StromaEdge> seedEdges(CollisionGrid cgGrid, ArrayList<StromaEdge> edges) {

		ArrayList<StromaEdge> updatedEdges = new ArrayList<StromaEdge>();

		for (StromaEdge tempEdge : edges) {

			// we need to create a new edge so we keep everything consistent,
			// otherwise the coordinates get
			// messed up
			Double3D loc1 = new Double3D(tempEdge.getPoint1().x + 1, tempEdge.getPoint1().y + 1,
					tempEdge.getPoint1().z + 1);
			Double3D loc2 = new Double3D(tempEdge.getPoint2().x + 1, tempEdge.getPoint2().y + 1,
					tempEdge.getPoint2().z + 1);

			StromaEdge se = new StromaEdge(loc1, loc2, tempEdge.getStromaedgetype());

			se.setObjectLocation(loc1);
			Stroma sc1 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1(), false);
			Stroma sc2 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2(), false);

			// if a node doesnt have two associated nodes then we delete it.
			if (sc1 != null && sc2 != null) {

				addEdgeToSchedule(sc1, sc2, se, cgGrid, se.getStromaedgetype());
				updatedEdges.add(se);

			} else if (sc1 != null) {
				// might be a connection between brc and fdc so need to query
				// the other grid yo
				Stroma sc3 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1(), false);
				Stroma sc4 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2(), false);

				if (sc3 != null) {
					addEdgeToSchedule(sc1, sc3, se, cgGrid, se.getStromaedgetype());
					updatedEdges.add(se);
				} else if (sc4 != null) {
					addEdgeToSchedule(sc1, sc4, se, cgGrid, se.getStromaedgetype());
					updatedEdges.add(se);
				}
			} else if (sc2 != null) {
				// might be a connection between brc and fdc so need to query
				// the other grid yo
				Stroma sc3 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint1(), false);
				Stroma sc4 = getNodeAtLocation(se.getDrawEnvironment(), se.getPoint2(), false);

				if (sc3 != null) {
					addEdgeToSchedule(sc2, sc3, se, cgGrid, se.getStromaedgetype());
					updatedEdges.add(se);
				}

				else if (sc4 != null) {
					addEdgeToSchedule(sc2, sc4, se, cgGrid, se.getStromaedgetype());
					updatedEdges.add(se);
				}
			} else {

				// these are edges connected to edges,
				// we need to stochastically delete these
				// otherwise the networks are fully connected/
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
	static void addEdgeToSchedule(Stroma sc1, Stroma sc2, StromaEdge seEdge, CollisionGrid cgGrid,
			StromaEdge.TYPE type) {
		seEdge.setObjectLocation(seEdge.getPoint1());
		SimulationEnvironment.scheduleStoppableEdge(seEdge);
		seEdge.registerCollisions(cgGrid);
	}

	/**
	 * Assign the edges that are directly linked to a node: ie the protrusions
	 * 
	 * @param edges
	 *            list of all the edge locations
	 * @param sc
	 *            a stromal cell
	 *            
	 * @return the number of protrusions from a node
	 */
	static int countProtrusions(ArrayList<StromaEdge> edges, Stroma sc) {

		int numOfProtrusions = 0;

		Double3D loc = sc.getM_Location();
		// update the node and edge connections for all edges
		for (StromaEdge seEdge : edges) {
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x, seEdge.getPoint1().y, seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x, seEdge.getPoint2().y, seEdge.getPoint2().z);

			if (loc.distance(edgeloc) < Settings.DOUBLE3D_PRECISION) {
				numOfProtrusions += 1;

			} else if (loc.distance(edgeloc2) < Settings.DOUBLE3D_PRECISION) {
				numOfProtrusions += 1;
			}
		}
		return numOfProtrusions;
	}

	/**
	 * Assign the edges that are directly linked to a node: ie the protrusions
	 * 
	 * @param edges
	 *            list of all the edge locations
	 * @param sc
	 *            a stromal cell
	 */
	static void assignProtrusions(ArrayList<StromaEdge> edges, Stroma sc) {

		Double3D loc = sc.getM_Location();
		// update the node and edge connections for all edges
		for (StromaEdge seEdge : edges) {
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x, seEdge.getPoint1().y, seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x, seEdge.getPoint2().y, seEdge.getPoint2().z);

			if (loc.distance(edgeloc) < Settings.DOUBLE3D_PRECISION) {
				if (!sc.getM_Edges().contains(seEdge)) {
					sc.getM_Edges().add(seEdge);
					seEdge.m_Nodes.add(sc);
				}

			} else if (loc.distance(edgeloc2) < Settings.DOUBLE3D_PRECISION) {
				if (!sc.getM_Edges().contains(seEdge)) {
					sc.getM_Edges().add(seEdge);
					seEdge.m_Nodes.add(sc);
				}
			}
		}
	}



}
