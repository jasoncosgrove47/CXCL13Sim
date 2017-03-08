package sim3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import sim.util.Bag;
import sim.util.Double3D;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;

public class GenerateSCS {

	static double threshold = 1.85; // a threshold distance within which we add
									// connections between stroma
	static double mrcSeedProbability = 0.46; // the probability we add an MRC
											// undeneath an LEC

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
	 *            the collision grid for the stroma
	 */
	static void seedSCS(CollisionGrid cgGrid) {

		// iterate through the X and Z axes keeping Y fixed to seed the SCS
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {

				// TODO should these cells be on the collision grid?
				// seed the lymphatic endothelium cells, these are just nodes
				// without any edges
				Stroma flec = new Stroma(Stroma.TYPE.LEC, new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH), z));
				Stroma clec = new Stroma(Stroma.TYPE.LEC, new Double3D(x, Settings.HEIGHT, z));

				clec.setObjectLocation(new Double3D(x, Settings.HEIGHT, z));
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - Settings.bRC.SCSDEPTH, z));

				
				
				
			}
		}
		
		generateMRCNodes(192, cgGrid); //this hard coded value needs to be an external parameter
		
	
	}
		

		

	
	

	private static void generateMRCNodes(int NumberOfNodes, CollisionGrid cgGrid){
		int counter = 0;
		
		do{
			
			double x = Settings.RNG.nextInt(Settings.WIDTH );
			double z = 1 + Settings.RNG.nextInt(Settings.DEPTH -2);

			
			Double3D loc = new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH +0.5), z);
			//make sure that the mrcs are not to close to one another
			
			//make sure that the cells arent too close to one another. 
			Bag mrcs = SimulationEnvironment.mrcEnvironment.getObjectsAtLocation(loc);
			int mrcCounter = 0;
			
			//make sure we're not placing cells in the same location. 
			if(mrcs!= null){
				for(int i = 0; i < mrcs.size(); i ++){
					if(mrcs.get(i) instanceof Stroma){
						Stroma sc = (Stroma) mrcs.get(i);
						if(sc.getStromatype() == Stroma.TYPE.MRC){
							mrcCounter +=1;
							break;
						}
					}
				}
			}
		
			if(mrcCounter == 0){
	
				Stroma sc = new Stroma(Stroma.TYPE.MRC, loc);//dont put MRCs or MRC edges on the collisiongrid hi
				sc.setObjectLocation(loc);
				SimulationEnvironment.scheduleStoppableCell(sc);
				
				//FollicleInitialiser.instantiateStromalCell(cgGrid, Stroma.TYPE.MRC, loc);
				counter += 1;
			}
			
			
		}
		while(counter < NumberOfNodes);
	
	}

	/**
	 * 
	 * Helper method that takes a map of associated nodes and genereates an edge
	 * between them
	 * 
	 * @param connectionsToAdd
	 *            A map containing pairs of MRCs to connect with one another
	 * @param cgGrid
	 *            the collision grid to add the connection to
	 */
	private static void seedMRCEdges(Map<Stroma, ArrayList<Stroma>> connectionsToAdd, CollisionGrid cgGrid) {

		// iterate through the map and get the pairs of MRCs
		for (Map.Entry<Stroma, ArrayList<Stroma>> entry : connectionsToAdd.entrySet()) {
			Stroma sc = entry.getKey();
			ArrayList<Stroma> neighbours = entry.getValue();

			// Place an edge between the two associated nodes
			for (Stroma neighbour : neighbours) {

				
				if(!sc.equals(neighbour)){
				
				Double3D loc = sc.getM_Location();
				Double3D neighbourloc = neighbour.getM_Location();

				StromaEdge seEdge = new StromaEdge(loc, neighbourloc, StromaEdge.TYPE.MRC_edge);
				seEdge.setObjectLocation(seEdge.getPoint1());
				
				SimulationEnvironment.scheduleStoppableCell(seEdge);
				//seEdge.registerCollisions(cgGrid);
				
				
				
				sc.getM_Edges().add(seEdge);
				neighbour.getM_Edges().add(seEdge);
				
				
				//sc.getM_Nodes().add(neighbour);
				//neighbour.getM_Nodes().add(sc);
				}
			}
		}
	}

	/**
	 * 
	 * Generates the MRC network, this is done separately to the BRCs and FDCs
	 * as it is also connected to the SCS
	 * 
	 * @param cgGrid
	 *            the collision grid for the stroma
	 */
	static void generateMRCNetwork(CollisionGrid cgGrid) {

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
							.getNeighborsExactlyWithinDistance(sc.getM_Location(), threshold, false); 

					// update the connections between neighbouring MRC nodes in
					// the connectionsToAdd map
					addMRCConnections(neighbours, cgGrid, sc, connectionsToAdd);
				}
			}
		}

		// now instantiate the connections and conenct the MRC network to the
		// BRC network
		seedMRCEdges(connectionsToAdd, cgGrid);
		connectMRCtoRC(cgGrid);
	}

	/**
	 * This updates the connectionsToAdd map based on neighbouring cells It also
	 * checks that we are not adding a stroma edge between a stroma node and
	 * itself
	 * 
	 * @param neighbours
	 *            a Bag of neighbouring cells obtained from the MRC grid
	 * @param cgGrid
	 *            the collision grid to add the edge to
	 * @param loc
	 *            the location of the stromal node we are adding edges to
	 * @param sc
	 *            the stroma node itself
	 * @param connectionsToAdd
	 *            a list of stromal cells and the other stroma that they are
	 *            connected to
	 */
	private static void addMRCConnections(Bag neighbours, CollisionGrid cgGrid, Stroma sc,
			Map<Stroma, ArrayList<Stroma>> connectionsToAdd) {

		// iterate through the neighbours and if an MRC and the original stroma
		// cell
		// then add a connection
		for (int j = 0; j < neighbours.size(); j++) {
			if (neighbours.get(j) instanceof Stroma) {
				if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC) {
					if (!sc.equals(neighbours.get(j))) {

						Stroma neighbour = (Stroma) neighbours.get(j);

						// if its an MRC and nodes arent connected already
						if (!Stroma.AreStromaNodesConnected(sc, neighbour)) {

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
	}

	/**
	 * 
	 * make connections between MRCs and BRCs that are smaller than a threshold
	 * distance apart.
	 * 
	 * @param cgGrid
	 *            the collision grid to add the edge to
	 *                     
	 */
	private static void connectMRCtoRC(CollisionGrid cgGrid) {

		Bag stroma = SimulationEnvironment.mrcEnvironment.getAllObjects();

		
		Map<Stroma,ArrayList<Stroma>> connectionsToAdd = new HashMap<Stroma,ArrayList<Stroma>>();
		
		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {

					// get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = sc.getM_Location();
					Bag neighbours = SimulationEnvironment.brcEnvironment.getNeighborsExactlyWithinDistance(loc,
							threshold + 0.4, false);

					
					// add connections between all of the neighbours
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {

							//FollicleInitialiser.addEdgeBetweenNodes(sc, (Stroma) neighbours.get(j), cgGrid,
								//	StromaEdge.TYPE.MRC_edge);
							Stroma neighbour = (Stroma) neighbours.get(j);
			
							if(!sc.equals(neighbour)){
							
								if(!sc.getM_Nodes().contains(neighbour)){
							
									if (connectionsToAdd.get(sc) == null) {
										connectionsToAdd.put(sc, new ArrayList<Stroma>());
									}
							
									connectionsToAdd.get(sc).add(neighbour);
									
								}
							}					

						}
					}
				}
			}
		}

			for (Entry<Stroma, ArrayList<Stroma>> entry : connectionsToAdd.entrySet()) {

				// get the edge and the edges it is connected to
				Stroma sc1 = entry.getKey();

				
				ArrayList<Stroma> nodesToConnectTo = entry.getValue();

				for (Stroma sc2 : nodesToConnectTo) {

					// make sure there are no self connections
					if (!sc1.equals(sc2)) {

						StromaEdge seEdge = new StromaEdge(sc1.getM_Location(), sc2.getM_Location(), StromaEdge.TYPE.MRC_edge);
						seEdge.setObjectLocation(seEdge.getPoint1());
						
						
						SimulationEnvironment.scheduleStoppableCell(seEdge);
						seEdge.registerCollisions(cgGrid);	
						
						
						sc1.getM_Edges().add(seEdge);
						sc2.getM_Edges().add(seEdge);
						
						//need to update the nodes that we are connected to. 
						//sc1.getM_Nodes().add(sc2);
						//sc2.getM_Nodes().add(sc1);
						
					}

				}
			}	
	}
	
	

	/**
	 * Fit a quadratic regression line using a set of coefficients
	 * This is done to set the SCS if we read in the network directly
	 * from the data. 
	 * 
	 * Bo = intercept
	 * B1 = the 1st regression coefficient
	 * B2 = the 2nd regression coefficient
	 * 
	 * For the moment we use the coeffs: 
	 *  y = 9.5266687 -(0.5291553 * i) + (0.0206240* Math.pow(i, 2));
	 */
	@SuppressWarnings("unused")
	private static void fitSCS(double Bo, double B1 , double B2, int xMin, int xMax){
		
		for(int i = xMin; i < xMax;i++){
			
			//these numbers were obtained by fitting a polynomial 
			//regression (2nd order) to the X and Y coordinates
			// of the MRCs
			double y = Bo +(B1 * i) - (B2* Math.pow(i, 2));
	
			for(int j = 0; j < 10;j++){

				//Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma clec = new Stroma(Stroma.TYPE.LEC,new Double3D(i, y+1, j));		
				//we add the 0.5 to the y as we dont want the LECs on the MRCs but just above them
				clec.setObjectLocation(new Double3D(i, y+1, j));
				//Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma flec = new Stroma(Stroma.TYPE.LEC,new Double3D(i, y - 1, j));
				//we add the 0.5 to the y as we dont want the LECs on teh MRCs but just above them
				flec.setObjectLocation(new Double3D(i, y - 1, j));
			}
		}		
	}
	

}
