package sim3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;

public class GenerateSCS {
	/**
	 * This class generates the lymphatic endothelium, the MRC network and
	 * connects the MRCs to the BRC Network
	 */

	/**
	 * Seed the cells at the subcapuslar sinus
	 * 
	 * iterate through the X and Z axes keeping Y fixed to seed the SCS The MRC
	 * locations are generated stochastically
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 */
	static void seedSCS() {

		// iterate through the X and Z axes keeping Y fixed to seed the SCS
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {

				// LECs dont get added to schedule or to the collision grid,
				// handlebounce will sort them out.
				Stroma flec = new Stroma(Stroma.TYPE.LEC,
						new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH), z));
				Stroma clec = new Stroma(Stroma.TYPE.LEC, new Double3D(x, Settings.HEIGHT, z));

				clec.setObjectLocation(new Double3D(x, Settings.HEIGHT, z));
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - Settings.bRC.SCSDEPTH, z));

			}
		}
		generateMRCNodes(Settings.MRC.COUNT);
	}

	/**
	 * Stochastically place MRCs just under the SCS
	 * 
	 * @param NumberOfNodes
	 *            the number of MRCs to place
	 * @param cgGrid
	 *            the collision grid to add the MRCs to
	 */
	private static void generateMRCNodes(int NumberOfNodes) {
		int iCounter = 0;

		do {
			double test_x = Settings.RNG.nextDouble() * Settings.WIDTH;
			double test_z = 0.5 + Settings.RNG.nextDouble() * (Settings.DEPTH - 2.5);
			// double x = Settings.RNG.nextInt(Settings.WIDTH);
			// double z = 1 + Settings.RNG.nextInt(Settings.DEPTH - 2);
			Double3D loc = new Double3D(test_x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 0.5), test_z);

			// make sure that the mrcs are not to close to one another
			// if not then add the cell to the grid and schedule
			// but we dont put them on the collision grid
			// as interactions with MRCs are handled by bounceYAxis
			if (!checkForMRCsAtLocation(loc)) {
				Stroma sc = new Stroma(Stroma.TYPE.MRC, loc);
				sc.setObjectLocation(loc);
				SimulationEnvironment.scheduleStoppableCell(sc);
				iCounter += 1;
			}
		} while (iCounter < NumberOfNodes);
	}

	/**
	 * See if there are any MRCs at double3D loc 
	 * 
	 * @param loc
	 *            the double3D location to check.
	 * @return true if there is an MRC at loc
	 */
	private static boolean checkForMRCsAtLocation(Double3D loc) {

		// make sure that the cells arent too close to one another.
		// Bag bagMrcs =
		// SimulationEnvironment.mrcEnvironment.getObjectsAtLocation(loc);
		Bag bagMrcs = SimulationEnvironment.mrcEnvironment.getNeighborsExactlyWithinDistance(loc, 1.25);
		boolean bMrcAtLocation = false;

		// make sure we're not placing cells in the same location.
		if (bagMrcs != null) {
			for (int i = 0; i < bagMrcs.size(); i++) {
				if (bagMrcs.get(i) instanceof Stroma) {
					Stroma sc = (Stroma) bagMrcs.get(i);
					if (sc.getStromatype() == Stroma.TYPE.MRC) {
						bMrcAtLocation = true;
						break;
					}
				}
			}
		}
		return bMrcAtLocation;
	}

	/**
	 * 
	 * Helper method that takes a map of associated nodes and genereates an edge
	 * between them
	 * 
	 * @param connectionsToAdd
	 *            A map containing pairs of MRCs to connect with one another
	 */
	private static void seedMRCEdges(Map<Stroma, ArrayList<Stroma>> connectionsToAdd) {

		// iterate through the map and get the pairs of MRCs
		for (Map.Entry<Stroma, ArrayList<Stroma>> entry : connectionsToAdd.entrySet()) {
			Stroma sc = entry.getKey();
			ArrayList<Stroma> neighbours = entry.getValue();


			// Place an edge between the two associated nodes
			for (Stroma neighbour : neighbours) {

				if (!sc.equals(neighbour) && !Stroma.AreStromaNodesConnected(sc, neighbour)){
						//&& sc.getM_Nodes().size() < sizeThreshold && neighbour.getM_Nodes().size() < 6) {

					Double3D loc = sc.getM_Location();
					Double3D neighbourloc = neighbour.getM_Location();

					StromaEdge seEdge = new StromaEdge(loc, neighbourloc, StromaEdge.TYPE.MRC_edge);
					seEdge.setObjectLocation(seEdge.getPoint1());

					SimulationEnvironment.scheduleStoppableEdge(seEdge);

					sc.getM_Edges().add(seEdge);
					neighbour.getM_Edges().add(seEdge);
					// update the protrusions
					sc.getM_Nodes().add(neighbour);
					neighbour.getM_Nodes().add(sc);

				}
			}
		}
	}

	/**
	 * Generates the MRC network, this is done separately to the BRCs and FDCs
	 * as it is also connected to the SCS
	 */
	static void generateMRCNetwork() {

		// get all elements of the MRC grid, this will contain MRC
		// nodes and edges and LEC nodes
		Bag stroma = SimulationEnvironment.mrcEnvironment.getAllObjects();

		// Bags are read only so we need a map to store connections between
		// edges
		Map<Stroma, ArrayList<Stroma>> connectionsToAdd = new HashMap<Stroma, ArrayList<Stroma>>();

		// iterate through all the stroma cells and check each MRC node
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				Stroma sc = (Stroma) stroma.get(i);
				if (sc.getStromatype() == Stroma.TYPE.MRC) {
					Bag neighbours = SimulationEnvironment.mrcEnvironment
							.getNeighborsExactlyWithinDistance(sc.getM_Location(), 3.0, false);

					// update the connections between neighbouring MRC nodes
					connectionsToAdd = addMRCConnections(neighbours, sc, connectionsToAdd);
				}
			}
		}

		// now instantiate the connections and connect the MRC network to the
		// BRC network
		
		//we need to do checks here that the node connections is below a certain threshold
		seedMRCEdges(connectionsToAdd);
		connectToRC();
	}

	/**
	 * 
	 * @return an arraylist of BRCs above a threshold height
	 */
	private static ArrayList<Stroma> getBRCsAboveThresholdYCoord(double threshold) {
		Bag brcs = SimulationEnvironment.brcEnvironment.getAllObjects();
		ArrayList<Stroma> brcNodes = new ArrayList<Stroma>();

		// get all of the brcs that are close to the SCS
		for (int i = 0; i < brcs.size(); i++) {
			if (brcs.get(i) instanceof Stroma) {
				Stroma sc = ((Stroma) brcs.get(i));
				// we only want the nodes closest to the mrc network

				if (sc.getM_Location().y > threshold) { // height greater than
														// 30 is close to the
														// scs
					brcNodes.add(sc);
				}
			}
		}

		return brcNodes;

	}

	/**
	 * Given a bag of neighbours, find the closest MRC to the stromal cell brc
	 * 
	 * @param neighbours
	 *            bag of stromal cells
	 * @param brc
	 *            the cell which we want to connect to
	 * @return the closest mrc to brc
	 */
	private static Stroma findClosestMRC(Bag neighbours, Stroma brc) {

		Stroma nodeToConnectTo = null;
		double minDist = 5;
		if (!neighbours.isEmpty()) {
			// find the closest mrc to us
			for (int i = 0; i < neighbours.size(); i++) {
				if (neighbours.get(i) instanceof Stroma) {
					Stroma sc = (Stroma) neighbours.get(i);
					if (sc.getStromatype() == Stroma.TYPE.MRC) {

						if (!brc.equals(sc) && !brc.getM_Nodes().contains(sc)) {

							double dist = brc.getM_Location().distance(sc.getM_Location());
							if (dist < minDist) {
								minDist = dist;
								nodeToConnectTo = sc;

							}
						}
					}
				}
			}
		}

		return nodeToConnectTo;
	}

	/**
	 * Given a bag of neighbours, find the closest MRC_edge to the stromal cell
	 * brc
	 * 
	 * @param neighbours
	 *            bag of stromal cells
	 * @param brc
	 *            the cell which we want to connect to
	 * @return the closest mrc to brc
	 */
	private static StromaEdge findClosestEdge(Bag neighbours, StromaEdge seEdge) {

		StromaEdge edgeToConnectTo = null;

		if (!neighbours.isEmpty()) {

			double minDist = 5;

			for (int i = 0; i < neighbours.size(); i++) {
				if (neighbours.get(i) instanceof StromaEdge) {

					StromaEdge se = (StromaEdge) neighbours.get(i);
					if (!seEdge.equals(se)) {
						double dist = seEdge.getM_Location().distance(se.getM_Location());
						if (dist < minDist) {
							minDist = dist;
							edgeToConnectTo = se;

						}
					}
				}
			}
		}

		return edgeToConnectTo;
	}

	/**
	 * This method may exist elsewhere but im pretty sure MRC is a special case,
	 * will need to refactor
	 * 
	 * @return
	 */
	private static StromaEdge addMRCEdge(Stroma brc, Stroma nodeToConnectTo) {
		StromaEdge seEdge = new StromaEdge(brc.getM_Location(), nodeToConnectTo.getM_Location(),
				StromaEdge.TYPE.MRC_edge);
		seEdge.setObjectLocation(seEdge.getPoint1());

		SimulationEnvironment.scheduleStoppableEdge(seEdge);

		brc.getM_Nodes().add(nodeToConnectTo);
		nodeToConnectTo.getM_Nodes().add(brc);

		brc.getM_Edges().add(seEdge);
		nodeToConnectTo.getM_Edges().add(seEdge);

		seEdge.m_Nodes.add(brc);
		seEdge.m_Nodes.add(nodeToConnectTo);

		return seEdge;
	}

	/**
	 * This method may exist elsewhere but im pretty sure MRC is a special case,
	 * will need to refactor
	 * 
	 * @return
	 */
	private static StromaEdge addMRCBranch(StromaEdge seEdge, StromaEdge edgeToConnectTo) {
		StromaEdge branch = new StromaEdge(seEdge.getMidpoint(), edgeToConnectTo.getMidpoint(),
				StromaEdge.TYPE.MRC_branch);
		branch.setObjectLocation(branch.getPoint1());
		SimulationEnvironment.scheduleStoppableEdge(branch);
		seEdge.m_Branches.add(branch);
		edgeToConnectTo.m_Branches.add(branch);
		branch.m_Edges.add(edgeToConnectTo);
		branch.m_Edges.add(seEdge);
		return branch;
	}

	/**
	 * TODO add comments
	 * 
	 */
	private static void connectToRC() {

		ArrayList<Stroma> brcNodes = getBRCsAboveThresholdYCoord(Settings.HEIGHT - 5);

		// now for each of these brcs add a connection to the closest mrc hi
		for (Stroma brc : brcNodes) {

			for (int x = 0; x < 2; x++) {

				Bag neighbours = SimulationEnvironment.mrcEnvironment
						.getNeighborsExactlyWithinDistance(brc.getM_Location(), 4);

				Stroma nodeToConnectTo = null;
				nodeToConnectTo = findClosestMRC(neighbours, brc);
				if (nodeToConnectTo != null) {

					StromaEdge seEdge = addMRCEdge(brc, nodeToConnectTo);

					//Bag branchneighbours = SimulationEnvironment.mrcEnvironment
						//	.getNeighborsExactlyWithinDistance(seEdge.getMidpoint(), 4);

					//StromaEdge edgeToConnectTo = findClosestEdge(branchneighbours, seEdge);

					//if (edgeToConnectTo != null && edgeToConnectTo.getStromaedgetype() == StromaEdge.TYPE.MRC_edge
						//	&& !seEdge.equals(edgeToConnectTo)) {

						//addMRCBranch(seEdge, edgeToConnectTo);
					//}
				}
			}
		}
	}

	/**
	 * This updates the connectionsToAdd map based on neighbouring cells It also
	 * checks that we are not adding a stroma edge between a stroma node and
	 * itself
	 * 
	 * @param neighbours
	 *            a Bag of neighbouring cells obtained from the MRC grid
	 * @param loc
	 *            the location of the stromal node we are adding edges to
	 * @param sc
	 *            the stroma node itself
	 * @param connectionsToAdd
	 *            a list of stromal cells and the other stroma that they are
	 *            connected to
	 * @return
	 */
	private static Map<Stroma, ArrayList<Stroma>> addMRCConnections(Bag neighbours, Stroma sc,
			Map<Stroma, ArrayList<Stroma>> connectionsToAdd) {

		// iterate through the neighbours and if an MRC and the original stroma
		// cell then add a connection
		for (int j = 0; j < neighbours.size(); j++) {
			

			if (neighbours.get(j) instanceof Stroma) {
				if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC
						|| ((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.bRC) {
					if (!sc.equals(neighbours.get(j))) {

					
						Stroma neighbour = (Stroma) neighbours.get(j);

						// if its an MRC and nodes arent connected already, and
						// there isnt a point in the way
						if (!Stroma.AreStromaNodesConnected(sc, neighbour)
								&& !FollicleInitialiser.checkForPointsInTheWay(sc, neighbour, 1.75)) {

							// Bags in MASON are read only so seed stroma
							// outside loop
							if (connectionsToAdd.get(sc) == null) {

								connectionsToAdd.put(sc, new ArrayList<Stroma>());
							}
							connectionsToAdd.get(sc).add(neighbour);

							
						}
					}
				}
			}
		}
		return connectionsToAdd;
	}

	/**
	 * make connections between MRCs and BRCs that are smaller than a threshold
	 * distance apart.
	 */
	@SuppressWarnings("unused")
	private static void connectMRCtoRC(int mean, int sd) {

		Bag stroma = SimulationEnvironment.mrcEnvironment.getAllObjects();
		Map<Stroma, ArrayList<Stroma>> connectionsToAdd = new HashMap<Stroma, ArrayList<Stroma>>();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {

					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = sc.getM_Location();

					Bag neighbours = SimulationEnvironment.brcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.2,
							false);

					connectionsToAdd = addMRCConnections(neighbours, sc, connectionsToAdd);

				}
			}

		}
		seedMRCEdges(connectionsToAdd);
	}

	/**
	 * Fit a quadratic regression line using a set of coefficients This is done
	 * to set the SCS if we read in the network directly from the data.
	 * 
	 * Bo = intercept B1 = the 1st regression coefficient B2 = the 2nd
	 * regression coefficient
	 * 
	 * For the moment we use the coeffs: y = 9.5266687 -(0.5291553 * i) +
	 * (0.0206240* Math.pow(i, 2));
	 */
	@SuppressWarnings("unused")
	private static void fitSCS(double Bo, double B1, double B2, int xMin, int xMax) {

		for (int i = xMin; i < xMax; i++) {

			// these numbers were obtained by fitting a polynomial
			// regression (2nd order) to the X and Y coordinates
			// of the MRCs
			double y = Bo + (B1 * i) - (B2 * Math.pow(i, 2));

			for (int j = 0; j < 10; j++) {

				// Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma clec = new Stroma(Stroma.TYPE.LEC, new Double3D(i, y + 1, j));
				// we add the 0.5 to the y as we dont want the LECs on the MRCs
				// but just above them
				clec.setObjectLocation(new Double3D(i, y + 1, j));
				// Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma flec = new Stroma(Stroma.TYPE.LEC, new Double3D(i, y - 1, j));
				// we add the 0.5 to the y as we dont want the LECs on teh MRCs
				// but just above them
				flec.setObjectLocation(new Double3D(i, y - 1, j));
			}
		}
	}

}
