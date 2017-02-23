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

import dataLogger.Controller;
import dataLogger.ProcessData;
import dataLogger.outputToCSV;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.FollicleInitialiser;
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
	public static int fdcNetRadius = 15;
	

	
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
	public static Continuous3D fdcEnvironment;
	public static Continuous3D brcEnvironment;
	public static Continuous3D mrcEnvironment;


	
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
		cell.setStopper(simulation.schedule.scheduleRepeating((Steppable) cell));
		
	}

	
	
	/**
	 * Override this method for stroma as they need to be later in the schedule than
	 * lymphocytes because they secrete chemokine, Otherwise you can get some
	 * weird artefacts
	 * @param cell
	 */
	public static void scheduleStoppableCell(Stroma cell) {
		cell.setStopper(simulation.schedule.scheduleRepeating((Steppable) cell, 2, 1));	
	}
	
	
	/**
	 * Override this method for stroma as they need to be later in the schedule than
	 * lymphocytes because they secrete chemokine, Otherwise you can get some
	 * weird artefacts
	 * @param cell
	 */
	public static void scheduleStoppableCell(StromaEdge cell) {
		cell.setStopper(simulation.schedule.scheduleRepeating((Steppable) cell, 2, 1));	
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

		mrcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		
		brcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		

		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// initialise chemokines
		CXCL13 = new Chemokine(schedule, Chemokine.TYPE.CXCL13, Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH);
		


		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1); //TODO does this mean that this is scheduled last? surely it should be one of the first things

	
	
		
		FollicleInitialiser.initialiseFollicle(cgGrid);
		
		
	
		
		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		// seed lymphocytes within the follicle
		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);
		//seedCells(CELLTYPE.T);
		
		//totalNumberOfAPCs = calculateTotalNumberOfAPCs();
		
		//int[] numbers = calculateNodesAndEdges();
		
		//System.out.println("nodes: " + numbers[0]);
		//System.out.println("edges: " + numbers[1]);
		
		///this should throw an error now because we dont have a useful index
		
		//int[][] a_matrix = Controller.generateAdjacencyMatrix();
		//a_matrix = Controller.updateAdjacencyMatrixForRCs(a_matrix);
		//a_matrix = Controller.updateAdjacencyMatrixForFDCs(a_matrix);
		//a_matrix = Controller.updateAdjacencyForBranchConnections(a_matrix);
		
		//System.out.println("FDC matrix updates are switched off for the moment");
		

		//outputToCSV.writeNodeInformationToFile("/Users/jc1571/Desktop/nodeInfo.csv", Stroma.getNodeinformation());
		//outputToCSV.writeAdjacencyMatrixToFile("/Users/jc1571/Desktop/adjacency.csv", a_matrix);
		
	}

	

	
	/**
	 * In case we evr need to just read the stroma in straight from the node data
	 * @param cgGrid
	 */
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
			scheduleStoppableCell(mrc);
			
			
		}
		
		for(int i = 0 ; i < fdcnodes.size(); i ++){
			
			Stroma fdc = new Stroma(Stroma.TYPE.FDC,new Double3D(fdcnodes.get(i).x, fdcnodes.get(i).y, fdcnodes.get(i).z));
			fdc.setObjectLocation(new Double3D(fdcnodes.get(i).x, fdcnodes.get(i).y, fdcnodes.get(i).z));
			fdc.registerCollisions(cgGrid);
			scheduleStoppableCell(fdc);
			
		}
		
		for(int i = 0 ; i < brcnodes.size(); i ++){
			
			Stroma brc = new Stroma(Stroma.TYPE.bRC,new Double3D(brcnodes.get(i).x, brcnodes.get(i).y, brcnodes.get(i).z));
			brc.setObjectLocation(new Double3D(brcnodes.get(i).x, brcnodes.get(i).y, brcnodes.get(i).z));
			brc.registerCollisions(cgGrid);
			scheduleStoppableCell(brc);	
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
	

	/**
	 * Tests whether co-ordinates x,y are in the circle centered at
	 * circleCentreX, circleCentreY with a specified radius
	 * 
	 * @return boolean determining whether inside (false) or outside (true) the
	 *         circle
	 */
	public static boolean isWithinCircle(double x, double y, double circleCentreX,
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
	

	


	
	
	//TODO DRY: this is in the other class: should we make a generic methods class to make this cleaner...
	public static double calcDistance(Double3D i3Point1, Double3D i3Point2) {
		return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2)
				+ Math.pow(i3Point1.y - i3Point2.y, 2)
				+ Math.pow(i3Point1.z - i3Point2.z, 2)));
	}
	


	
	public static Bag getAllStroma(){
		Bag stroma = fdcEnvironment.getAllObjects();
		System.out.println("size is: " + stroma.size());
		stroma.addAll(brcEnvironment.getAllObjects());
		System.out.println("size 2 is: " + stroma.size());
		stroma.addAll(mrcEnvironment.getAllObjects());
		System.out.println("size 3 is: " + stroma.size());
		
		return stroma;
		
	}
	
	
	
	public static Bag getAllStromaWithinDistance(Double3D loc, double dist){
		Bag stroma = fdcEnvironment.getNeighborsExactlyWithinDistance(loc, dist);
		System.out.println("size is: " + stroma.size());
		stroma.addAll(brcEnvironment.getNeighborsExactlyWithinDistance(loc, dist));
		System.out.println("size 2 is: " + stroma.size());
		stroma.addAll(mrcEnvironment.getNeighborsExactlyWithinDistance(loc, dist));
		System.out.println("size 3 is: " + stroma.size());
		
		return stroma;
		
	}

	/**
	 * Calculate the number of dendrites in the FDC network excluding the FDC
	 * nodes
	 * 
	 * @return the number of dendrites as an integer value
	 */
	private int calculateTotalNumberOfDendrites() {

		Bag stroma = getAllStroma();
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
	
	
	
	//this calculates the total number of stromal cells and the edges but is not a measure of the
	//protrusions or of the topology
	public static int[] calculateNodesAndEdges(){
		
		int nodes = 0;
		int edges = 0;
		int[] output = new int[2];
		
		Bag stroma = getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {
			
				edges ++;
			}
		
			else if(stroma.get(i) instanceof Stroma){
				if(((Stroma)stroma.get(i)).getStromatype()!=Stroma.TYPE.LEC){
					nodes ++;
				}
			}
		}
		
		output[0] = nodes;
		output[1] = edges;

		return output;
		

	}
	
	
	
	
	
	private int calculateTotalNumberOfAPCs(){
		
		Bag stroma = getAllStroma();
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
