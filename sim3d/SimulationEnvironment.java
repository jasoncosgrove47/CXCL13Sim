package sim3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.ParseStromaLocations;
import sim3d.util.StromaGenerator;
import sim3d.util.StromaGenerator.StromalCell;

/**
 * This class sets up and runs the simulation absent of any GUI related function
 * as a MASON design pattern, in line with the MASON (Model/View/Controller).
 * 
 * Need to ensure that Java has access to enough memory resources go to run
 * configurations and pass in -Xmx3000m
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */

public class SimulationEnvironment extends SimState {

	/**
	 * this is the size of the FDC network
	 */
	private static int fdcNetRadius = 13;
	

	private static final long serialVersionUID = 1;

	/**
	 * Boolean which is true when the system has reached a steady state which
	 * signals the start of an experiment
	 */
	public static boolean steadyStateReached = false;

	/**
	 * Once the experiment has run for sufficient amount of time this boolean
	 * becomes true which shuts down the simulation.
	 */
	public static boolean experimentFinished = false;

	/**
	 * Total number of dendrites in the FDC network
	 */
	public static int totalNumberOfAPCs = 0;

	/**
	 * A static instance of the simulation that only get's set here
	 */
	public static SimulationEnvironment simulation;

	/**
	 * an arraylist containing all unique edges 
	 * Sometimes the generator leads to overlapping edges
	 * so we remove these 
	 */
	ArrayList<StromaEdge> m_edges;
	
	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D fdcEnvironment;

	/**
	 * Instance of the CXCL13 class
	 */
	public static Chemokine CXCL13;
	
	
	/**
	 * Instance of the CXCL13 class
	 */
	//public static Chemokine EBI2L;

	/*
	 * Parameter file: XML format
	 */
	public static Document parameters;

	/**
	 * ENUM for the cell types
	 */
	public enum CELLTYPE {
		B, cB, T
	}

	public TYPE celltype;

	/**
	 * Constructor for the simulation environment
	 * 
	 * @param seed
	 *            Used by MASON for the random seed
	 */
	public SimulationEnvironment(long seed, Document params) {
		super(seed);
		parameters = params;
		setupSimulationParameters();
		Settings.RNG = random; // We set the MASON random object to the static
								// Options class so we can access it everywhere
	}

	/**
	 * Load parameters from an external xml file to the static Options class
	 */
	public void setupSimulationParameters() {
		Settings.loadParameters(parameters);
		Settings.BC.loadParameters(parameters);
		Settings.TC.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
		Settings.bRC.loadParameters(parameters);
		Settings.MRC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.CXCL13.loadParameters(parameters);
		Settings.CCL19.loadParameters(parameters);
		Settings.EBI2L.loadParameters(parameters);
	}

	/**
	 * Destroy resources after use
	 */
	public void finish() {
		Chemokine.reset();
	}

	/**
	 * Accessor for the current display level - a z-index to use for displaying
	 * the diffusion
	 */
	public int getDisplayLevel() {
		// Add 1 so the scale goes from 1-10 and not 9
		return Chemokine.getDisplayLevel() + 1;

	}

	/**
	 * Setter for the current display level
	 */
	public void setDisplayLevel(int m_iDisplayLevel) {
		Chemokine.setDisplayLevel(m_iDisplayLevel - 1);
		Chemokine.getInstance(Chemokine.TYPE.CXCL13).updateDisplay();
	}

	/*
	 * Scheduling a cell returns a stoppable object. We store the stoppable
	 * object as a variable within the BC class. Then the BC can access its
	 * stopper variable and call the stop method.
	 */
	public void scheduleStoppableCell(Lymphocyte cell) {
		cell.setStopper(schedule.scheduleRepeating((Steppable) cell));
	}

	/**
	 * Override this method for stroma as they need to be later in the schedule than
	 * lymphocytes because they secrete chemokine, Otherwise you can get some
	 * weird artefacts
	 * @param cell
	 */
	public void scheduleStoppableCell(Stroma cell) {
		cell.setStopper(schedule.scheduleRepeating((Steppable) cell, 2, 1));	
	}
	
	/**
	 * Sets up a simulation run and initialises the environments.
	 */
	public void start() {
		// start the simulation
		super.start();

		// Initialise the stromal grid
		fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		Stroma.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;

		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// initialise chemokines
		CXCL13 = new Chemokine(schedule, Chemokine.TYPE.CXCL13, Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH);
		
		//EBI2L = new Chemokine(schedule, Chemokine.TYPE.EBI2L, Settings.WIDTH,
		//		Settings.HEIGHT, Settings.DEPTH);

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		
		
		

		seedStromaNodes(cgGrid);
		fitSCS();
		//seedSCS(cgGrid);
		//initialiseFollicle(cgGrid);
		
		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		// seed lymphocytes within the follicle
		//seedCells(CELLTYPE.B);
		//seedCells(CELLTYPE.cB);
		//seedCells(CELLTYPE.T);
		
		totalNumberOfAPCs = calculateTotalNumberOfAPCs();
	}

	
	/**
	 * To seed the SCS we created a regression line that goes through the X and Y coordinates.
	 * 
	 * This was done in R and will need to be changed as we want to scale between zero and one
	 * @param x
	 */
	private void fitSCS(){
		
		
		//we would need a range of X values from start to end of the sinus
		int x1 = 4;
		int x2 = 41;	
		
		for(int i = x1; i < x2;i++){
			
			
			double y = 9.5266687 -(0.5291553 * i) + (0.0206240* Math.pow(i, 2));
			
			
			//Stroma flec = new Stroma(Stroma.TYPE.LEC);
			Stroma clec = new Stroma(Stroma.TYPE.LEC);

			//we add the 0.5 to the y as we dont want the LECs on teh MRCs but just above them
			clec.setObjectLocation(new Double3D(i, y - 1.0, 2));

			//Stroma flec = new Stroma(Stroma.TYPE.LEC);
			Stroma flec = new Stroma(Stroma.TYPE.LEC);

			//we add the 0.5 to the y as we dont want the LECs on teh MRCs but just above them
			flec.setObjectLocation(new Double3D(i, y - 3.0, 2));
			
			
		}
				
		
		
	
	}
	
	
	private void seedStromaNodes(CollisionGrid cgGrid){
		
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
			
			Stroma mrc = new Stroma(Stroma.TYPE.MRC);
			mrc.setObjectLocation(new Double3D(mrcnodes.get(i).x, mrcnodes.get(i).y, mrcnodes.get(i).z));
			mrc.registerCollisions(cgGrid);
			scheduleStoppableCell(mrc);
			
			
		
			
		}
		
		for(int i = 0 ; i < fdcnodes.size(); i ++){
			
			Stroma fdc = new Stroma(Stroma.TYPE.FDC);
			fdc.setObjectLocation(new Double3D(fdcnodes.get(i).x, fdcnodes.get(i).y, fdcnodes.get(i).z));
			fdc.registerCollisions(cgGrid);
			scheduleStoppableCell(fdc);
			
		}
		
		for(int i = 0 ; i < brcnodes.size(); i ++){
			
			Stroma brc = new Stroma(Stroma.TYPE.bRC);
			brc.setObjectLocation(new Double3D(brcnodes.get(i).x, brcnodes.get(i).y, brcnodes.get(i).z));
			brc.registerCollisions(cgGrid);
			scheduleStoppableCell(brc);	
		}
		
	}
	
	
	private void seedStromaEdges(CollisionGrid cgGrid){
		
		Bag stroma = fdcEnvironment.getAllObjects();
		
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				
				
				/*
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.bRC) {
				
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = fdcEnvironment.getNeighborsExactlyWithinDistance(loc,4.0, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.bRC){

								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								seEdge.setStopper(schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
							}
						}
					}
					
				}
				*/
				

				
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.FDC) {
					
					
					
					
				}
				
				
				
				
				else if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {
					
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = fdcEnvironment.getNeighborsExactlyWithinDistance(loc,2.8, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC){

								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								seEdge.setStopper(schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
							}
						}
					}
					
				}
				
				
			}
		}
		
	}
	
	
	
	private void initialiseFollicle(CollisionGrid cgGrid){
		
		//seed the SCS
		seedSCS(cgGrid);
		
		//now initialise the stroma and shape the network
		initialiseStromaNetwork(cgGrid, Stroma.TYPE.FDC);
		initialiseStromaNetwork(cgGrid, Stroma.TYPE.bRC);

		//get rid of any FRCs in the follicle, and connect the reticular 
		// cells to the FRCs
		shapeNetwork();
		connectRCtoFDC(cgGrid);
	}
	
	
	
	/**
	 * Seed the cells at the subcapuslar sinus
	 * 
	 * iterate through the X and Z axes keeping Y fixed to seed the SCS
	 * The MRCs are generated stochastically: we iterate through the X and Z 
	 * axes keeping Y fixed to seed the SCS for each discrete location we
	 * generate a random number and if this exceeds some threshold value
	 * then we add an MRC location
	 * 
	 * @param cgGrid
	 */
	private void seedSCS(CollisionGrid cgGrid) {
	
		// iterate through the X and Z axes keeping Y fixed to seed the SCS	
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {
				
				//seed the lymphatic endothelium
				Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma clec = new Stroma(Stroma.TYPE.LEC);

				clec.setObjectLocation(new Double3D(x, Settings.HEIGHT, z));
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - Settings.bRC.SCSDEPTH, z));

				flec.registerCollisions(cgGrid);
				clec.registerCollisions(cgGrid);
				
				//now seed the MRCs
				double random = Settings.RNG.nextDouble();
				if(random > 0.15){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 0.3), z));
					mrc.registerCollisions(cgGrid);
					scheduleStoppableCell(mrc);
				}
				
				//sometimes we see two MRCs overlapping so we do it again. 
				if(random > 0.15){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, Settings.HEIGHT - (Settings.bRC.SCSDEPTH + 0.8), z));
					mrc.registerCollisions(cgGrid);
					scheduleStoppableCell(mrc);
				}

			}
		}
	}

	/**
	 * Tests whether co-ordinates x,y are in the circle centered at
	 * circleCentreX, circleCentreY with a specified radius
	 * 
	 * @return boolean determining whether inside (false) or outside (true) the
	 *         circle
	 */
	boolean isWithinCircle(double x, double y, double circleCentreX,
			double circleCentreY, int radius) {

		// squared distance from test.x to center.x
		double termOne = Math.pow((x - circleCentreX), 2);

		// squared distance from test.y to center.y
		double termTwo = Math.pow((y - circleCentreY), 2);

		// test whether the point is in the circle - pythagoras
		if ((termOne + termTwo) < Math.pow(radius, 2)) {
			return true;
		} else
			return false;
	}

	/**
	 * Seeds B cells into the FDC network
	 */
	void seedCells(CELLTYPE celltype) {
		
		int count = 0; // the number of cells to seed

		// set the number of cells to seed
		if (celltype == CELLTYPE.B) {
			count = Settings.BC.COUNT;
		} else if (celltype == CELLTYPE.cB) {
			count = Settings.BC.COGNATECOUNT;
		} else if (celltype == CELLTYPE.T) {
			count = Settings.TC.COUNT;
		}

		// seed the cells
		for (int i = 0; i < count; i++) {
			switch (celltype) {

			case B:
				BC bc = new BC();
				bc.setObjectLocation(generateCoordinateWithinCircle(fdcNetRadius));
				scheduleStoppableCell(bc);
				// so we only have 1 BC updating the ODE graph
				if (i == 0) {
					bc.displayODEGraph = true;
				}
				break;

			case cB:
				cognateBC cbc = new cognateBC(i);
				cbc.setObjectLocation(generateCoordinateWithinCircle(fdcNetRadius));
				scheduleStoppableCell(cbc);
				break;

			case T:
				TC tc = new TC();
				tc.setObjectLocation(generateCoordinateOutsideCircle(fdcNetRadius));

				scheduleStoppableCell(tc);
				break;

			default:
				break;
			}
		}
	}

	/**
	 * @return a random Double3D inside a circle
	 */
	Double3D generateCoordinateWithinCircle(int circleRadius) {

		int x, y, z;
		do {
			x = random.nextInt(Settings.WIDTH - 2) + 1;
			y = random.nextInt(Settings.HEIGHT - 2) + 1;
			z = random.nextInt(Settings.DEPTH - 2) + 1;

			// keep generating new values while they are outside of the circle
			// the radius of the circle is 13 and it is inside this that we seed
			// the b cells


		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, circleRadius) == false);

		return new Double3D(x, y, z);
	}

	/**
	 * @return a random Double3D inside a circle
	 */
	private Double3D generateCoordinateOutsideCircle(int circleRadius) {

		int x, y, z;
		do {
			x = random.nextInt(Settings.WIDTH - 2) + 1;
			y = random.nextInt(Settings.HEIGHT - 2) + 1;
			z = random.nextInt(Settings.DEPTH - 2) + 1;

			// keep generating new values while they are outside of the circle
			// the radius of the circle is 13 and it is inside this that we seed
			// the b cells
		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, circleRadius) == true);

		return new Double3D(x, y, z);
	}
	
	/**
	 * We generate the reticular network seperately to the FDC network
	 * To generate connections between the different cells as we observe 
	 * in vivo we make connections between stromal cells that are smaller
	 * than a threshold distance apart. 
	 * @param cgGrid
	 */
	private void connectRCtoFDC(CollisionGrid cgGrid){
		
		Bag stroma = fdcEnvironment.getAllObjects();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.bRC) {
				
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = fdcEnvironment.getNeighborsExactlyWithinDistance(loc, 2, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.FDC){

								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								seEdge.setStopper(schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
							}
						}
					}	
				}
			}
		}	
	}
	
	
	/**
	 * Generate the stromal network, need to rename this method as its confusing
	 * @param cgGrid
	 * @param celltype
	 */
	private void initialiseStromaNetwork(CollisionGrid cgGrid,
			Stroma.TYPE celltype) {

		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> stromalCellLocations = 
				new ArrayList<StromaGenerator.StromalCell>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		 if(celltype == Stroma.TYPE.bRC) {

			StromaGenerator.generateFRC3D(Settings.WIDTH - 2,
					Settings.HEIGHT - (Settings.bRC.SCSDEPTH+1), Settings.DEPTH - 2,
					Settings.bRC.COUNT, stromalCellLocations, sealEdges);
			

			m_edges = checkOverlaps(sealEdges);
			
			//set sealEdges to null so we can't use it again.
			sealEdges = null;
			
			//need to generate dendrites before nodes as we get rid of overlaps
			// having the nodes on the graph makes that a mess
			seedReticularEdges(cgGrid, m_edges, StromaEdge.TYPE.RC_edge);
			
			for (StromaGenerator.StromalCell sc : stromalCellLocations) {
				seedRC(sc,cgGrid,m_edges);
			}	
		}
			 
		if (celltype == Stroma.TYPE.FDC) {
			seedFDC();
		} 
	}

	/**
	 * Seed FDC cells within the follicle
	 * 
	 * @param sc
	 */
	private void seedFDC() {
		
		
		//this is hardcoded but there must be a better way to do this
		for(int x = 8; x < 34; x ++ ){
			for(int y = 8; y < 41; y ++ ){
				for(int z = 2;z<Settings.DEPTH-1;z++){
			
					//see if the point is within the FDC network
				if(isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
							(Settings.HEIGHT / 2)+2 , 13)){
					
				//generate a random double
				double random = Settings.RNG.nextDouble();
				
				//the random number has to be greater than this value to make an FDC
				double fdcThreshold = 0.8;
				
				
				if(random > fdcThreshold){
					
					//if its within the follicle then stochastically generate an FDC node
					Stroma stromalcell = new Stroma(Stroma.TYPE.FDC);
					stromalcell.setObjectLocation(new Double3D(x,y, z));
					
					scheduleStoppableCell(stromalcell);
					
					//dont need to worry about the plus one stuff for the FDCs
					Double3D loc = new Double3D(x,y,z);
					Bag neighbours = new Bag(); 	
					
					
					//get all FDCs within 20microns
					stromalcell.getDrawEnvironment().
					getNeighborsExactlyWithinDistance(loc, 2,false,true,false,neighbours);
		
	
					for(int i = 0; i < neighbours.size();i++){
						if(neighbours.get(i) instanceof Stroma){
							
							
							Stroma neighbour = (Stroma) neighbours.get(i);
							if(neighbour.getStromatype() == Stroma.TYPE.FDC){
								
									//dont want to include an edge to yourself
									if(!neighbour.equals(stromalcell)){
									Double3D nloc = new Double3D(neighbour.x, neighbour.y, neighbour.z);
						
									
									StromaEdge se = new StromaEdge(loc,nloc,StromaEdge.TYPE.FDC_edge);
						
									se.setObjectLocation(new Double3D(se.x,
										se.y , se.z));
								
									se.setStopper(schedule.scheduleRepeating((Steppable) se, 2, 1));

									stromalcell.m_dendrites.add(se);
									neighbour.m_dendrites.add(se);
									
									}	
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
	 * Prune the network, we sometimes gets nodes with no edges because we chop out the centre
	 * so this method handles all of this
	 */
	private void shapeNetwork(){		
		
		 // From the MASON manual: The provided Bag is to be treated as read-only and not to be modified,
		 //	and it may change at any time without warning.
		 // therefore do not edit a bag whilst iterating through!!!!!!!
		 
		Bag fdcs = new Bag(fdcEnvironment.getAllObjects());

		
		for(int i = 0; i < fdcs.size();i++){
			
			if(fdcs.get(i) instanceof Stroma){
				Stroma fdc = (Stroma) fdcs.get(i);
	
				if (fdc.getStromatype() == Stroma.TYPE.FDC){
					//get rid of any FDCs which arent connected to any dendrites
					if(fdc.m_dendrites.size() == 0){
						fdc.getDrawEnvironment().remove(fdc);
						fdc.stop();
					}					
				}		
				else if(fdc.getStromatype() == Stroma.TYPE.bRC){

					//get rid of any FDCs which arent connected to any dendrites
					if(fdc.m_dendrites.size() == 0){
						deleteRC(fdc);
					}
					
					if(isWithinCircle(fdc.x, fdc.y, (Settings.WIDTH / 2) + 1,
							(Settings.HEIGHT / 2)+2 , 13)){
						deleteRC(fdc);
					}
				}
			}	
		}
	}

		

	/**
	 * Seed the reticular network from the arraylist generated by StromaGenerator3D
	 * @param sc
	 * @param cgGrid
	 * @param edges
	 */
	private void seedRC(StromaGenerator.StromalCell sc,CollisionGrid cgGrid,ArrayList<StromaEdge> edges){
			
		Stroma frc = new Stroma(Stroma.TYPE.bRC);
		Double3D loc = new Double3D(sc.d3Location.x+1,
				sc.d3Location.y +1, sc.d3Location.z +1);
		// This will register the FRC with the environment/display
		// to account for the border which is one gridspace in width
		frc.setObjectLocation(loc);
		scheduleStoppableCell(frc);
		
		
		//add associatedDendrites:TODO can encapsulate this as a seperate method
		for (StromaEdge seEdge : edges)
		{
			
			
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x ,seEdge.getPoint1().y,seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x,seEdge.getPoint2().y,seEdge.getPoint2().z);
			
			if(loc.x == edgeloc.x && loc.y == edgeloc.y && loc.z == edgeloc.z)
			{
				  frc.m_dendrites.add(seEdge);		
			}
			else if(loc.x == edgeloc2.x && loc.y == edgeloc2.y && loc.z == edgeloc2.z)
			{
				  frc.m_dendrites.add(seEdge);		
			}
			
			double distance = calcDistance(loc,edgeloc);
			double distance2 = calcDistance(loc,edgeloc2);
			
			//sometimes they are very close but not perfectly in the same place. 
			//so we set some threshold distance, this was determined empirically
			double distanceThreshold = 0.1;
			if(distance < distanceThreshold || distance2 < distanceThreshold){
				frc.m_dendrites.add(seEdge);	
			}
		}
	}
		
	
	/**
	 * Remove an RC and its associated dendrites
	 * from the grid and the schedule
	 * @param frc
	 */
	private void deleteRC(Stroma frc){
		
		//remove the frc from grid and schedule
		frc.getDrawEnvironment().remove(frc);
		frc.stop();
		
		//now this for all associated dendrites. 
		for(int i = 0; i < frc.m_dendrites.size(); i ++){			
			frc.m_dendrites.get(i).getDrawEnvironment()
				.remove(frc.m_dendrites.get(i));
			frc.m_dendrites.get(i).stop();

		}
	}


	/**
	 * CHECK THE STROMAEDGE ARRAY TO MAKE SURE THERE ARE NO DUPLICATES
	 * 
	 * When generated stroma we can get overlapping edges, this method
	 * checks for overlaps and returns a new arraylist with all duplicated
	 * edges removed. 
	 * 
	 * TODO: might be cleaner to have this in the generate stroma class
	 * @param sealEdges
	 * @return
	 */
	private ArrayList<StromaEdge> checkOverlaps(ArrayList<StromaEdge> sealEdges){

		ArrayList<StromaEdge> updatedEdges = sealEdges;
		ArrayList<StromaEdge> edgesToRemove = new ArrayList<StromaEdge>();

		for (int i = 0; i < sealEdges.size(); i++) {
			  for (int j = i+1; j < sealEdges.size(); j++) {
				  
				  //get the end points of each of the two edges
				  Double3D i_p1 = new Double3D(sealEdges.get(i).x +1,
						  sealEdges.get(i).y+1, sealEdges.get(i).z +1);
				  Double3D i_p2 = new Double3D(sealEdges.get(i).getPoint2().x +1,
						  sealEdges.get(i).getPoint2().y+1, sealEdges.get(i).getPoint2().z +1);
					
				  Double3D j_p1 = new Double3D(sealEdges.get(j).x +1,
						  sealEdges.get(j).y+1, sealEdges.get(j).z +1);
				  Double3D j_p2 = new Double3D(sealEdges.get(j).getPoint2().x +1,
						  sealEdges.get(j).getPoint2().y+1, sealEdges.get(j).getPoint2().z +1);
				  
				//calculate the distances between all of the points. 
				double d1 = calcDistance(i_p1, j_p1);
				double d2 = calcDistance(i_p1, j_p2);
				double d3 = calcDistance(i_p2, j_p1);
				double d4 = calcDistance(i_p2, j_p2);
				
				//if the cells are too close to one another then remove them
				// the threshold value was determined by trial and error
				double thresholdDistance = 0.15; 
				
				if(d1 < thresholdDistance && d4 < thresholdDistance){
					edgesToRemove.add(sealEdges.get(i));
				}
				else if(d2 < thresholdDistance && d3 < thresholdDistance){
					edgesToRemove.add(sealEdges.get(i));
				}
			}
		}

		//now remove all of the overlapping edges in the removal list
		for(int x = 0; x < edgesToRemove.size(); x ++){

			updatedEdges.remove(edgesToRemove.get(x));
		}
		//return the updated array
		return updatedEdges;
	}
	
	
	//TODO DRY: this is in the other class: should we make a generic methods class to make this cleaner...
	protected static double calcDistance(Double3D i3Point1, Double3D i3Point2) {
		return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2)
				+ Math.pow(i3Point1.y - i3Point2.y, 2)
				+ Math.pow(i3Point1.z - i3Point2.z, 2)));
	}
	

	/**
	 * This method takes the stroma edges contained generated by
	 * StromaGenerator3D and places them on the stroma and collision
	 * grid as well as adding them to the schedule. 
	 * @param cgGrid
	 * @param edges
	 * @param type
	 */
	private void seedReticularEdges(CollisionGrid cgGrid,
			ArrayList<StromaEdge> edges, StromaEdge.TYPE type) {

		
		if (type == StromaEdge.TYPE.RC_edge) {
			for (StromaEdge seEdge : edges) {
				
					//seEdge.setM_col(Settings.bRC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x +1,
							seEdge.y +1, seEdge.z +1));

					seEdge.setStopper(schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
					seEdge.registerCollisions(cgGrid);			
				
			}
		}
	}

	/**
	 * Calculate the number of dendrites in the FDC network excluding the FDC
	 * nodes
	 * 
	 * @return the number of dendrites as an integer value
	 */
	private int calculateTotalNumberOfDendrites() {

		Bag stroma = fdcEnvironment.getAllObjects();
		int FDCcounter = 0;

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.FDC) {
					FDCcounter += 1;
				}
			}
		}

		// subtract number of nodes to get number of edges
		int number = stroma.size() - FDCcounter;
		return number;

	}
	
	
	private int calculateTotalNumberOfAPCs(){
		
		Bag stroma = fdcEnvironment.getAllObjects();
		int FDCcounter = 0;

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				
				
				Stroma.TYPE type = ((Stroma) stroma.get(i)).getStromatype();
				
				//we want to include FDCs, MRCs and bRCs
				if (type != Stroma.TYPE.bRC ) {
					FDCcounter += 1;
				}
			}
		}
		return FDCcounter;
	}

}
