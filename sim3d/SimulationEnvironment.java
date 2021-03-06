package sim3d;

import java.util.ArrayList;

import org.w3c.dom.Document;
import dataLogger.Controller;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;

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
	public static int fdcNetRadius = 12;

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
	public static int totalNumberOfFDCEdges = 0;
	public static int totalNumberOfMRCEdges = 0;
	public static int totalNumberOfBRCEdges = 0;
	public static int totalNumberOfEdges = 0;

	/**
	 * A static instance of the simulation that only get's set here
	 */
	public static SimulationEnvironment simulation;

	/**
	 * an arraylist containing all unique edges Sometimes the generator leads to
	 * overlapping edges so we remove these
	 */
	ArrayList<StromaEdge> m_edges;

	
	/*
	 * 3D grid for Stroma
	 */
	public static Continuous3D fdcEnvironment;
	public static Continuous3D brcEnvironment;
	public static Continuous3D mrcEnvironment;

	/**
	 * Matrices for storing stroma data
	 */
	public static double[][] distMatrix;
	public static double[][] a_matrix;

	/**
	 * Boolean array to store checkpoints
	 */
	public static boolean checkPoints[][][];

	/**
	 * Instance of the CXCL13 class
	 */
	public static Chemokine CXCL13;

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
		Settings.FDC.loadParameters(parameters);
		Settings.bRC.loadParameters(parameters);
		Settings.MRC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.CXCL13.loadParameters(parameters);

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

	/**
	 * Setter for the current display level. Would be nice to have a slider to
	 * scroll up through the z axis
	 * 
	 * @param m_iDisplayLevel
	 *            where on the z axis to visualise the chemokine field
	 * 
	 */
	public void setDisplayLevel(int m_iDisplayLevel) {
		Chemokine.setDisplayLevel(m_iDisplayLevel - 1);
		Chemokine.getInstance(Chemokine.TYPE.CXCL13).updateDisplay();
	}

	/*
	 * Scheduling a cell returns a stoppable object. We store the stoppable
	 * object as a variable within the BC class. Then the BC can access its
	 * stopper variable and call the stop method.
	 * 
	 * @param cell the lymphocyte to schedule
	 */
	public void scheduleStoppableCell(Lymphocyte cell) {
		cell.setStopper(simulation.schedule.scheduleRepeating((Steppable) cell));

	}

	/**
	 * Override this method for stroma as they need to be later in the schedule
	 * than lymphocytes because they secrete chemokine, Otherwise you can get
	 * some weird artefacts
	 * 
	 * @param cell
	 *            the stromal cell to schedule
	 */
	public static void scheduleStoppableCell(Stroma cell) {
		cell.setStopper(simulation.schedule.scheduleRepeating((Steppable) cell, 2, 1));
	}

	/**
	 * Override this method for stroma as they need to be later in the schedule
	 * than lymphocytes because they secrete chemokine, Otherwise you can get
	 * some weird artefacts
	 * 
	 * @param edge
	 *            the edge to schedule
	 */
	public static void scheduleStoppableEdge(StromaEdge edge) {
		edge.setStopper(simulation.schedule.scheduleRepeating((Steppable) edge, 2, 1));
	}

	/**
	 * Sets up a simulation run and initialises the environments.
	 */
	public void start() {
		// start the simulation
		super.start();

		// Initialise the stromal grid
		fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		mrcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		brcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT,
				Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// initialise chemokines
		CXCL13 = new Chemokine(schedule, Chemokine.TYPE.CXCL13, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		// initialise checkpoints
		initialiseCheckpoints();

		// intialise the follicleEnvironment
		FollicleInitialiser.initialiseFollicle(cgGrid);

		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		// seed lymphocytes within the follicle
		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);

		/// this should throw an error now because we dont have a useful index

		if (Settings.calculateTopologyData) {
			a_matrix = Controller.createMatrix(false);
			distMatrix = Controller.createMatrix(true);
		}

		totalNumberOfFDCEdges = calculateEdgesOfType(StromaEdge.TYPE.FDC_edge);

		totalNumberOfMRCEdges = calculateEdgesOfType(StromaEdge.TYPE.MRC_edge);

		totalNumberOfBRCEdges = calculateEdgesOfType(StromaEdge.TYPE.RC_edge);

		totalNumberOfEdges = totalNumberOfFDCEdges + totalNumberOfMRCEdges + totalNumberOfBRCEdges;
	}

	/**
	 * This methods initialises the checkpoints, the metric we use to quantify
	 * scanning Currently each gridspace is set to true and thus each gridspace
	 * is a checkpoint but could adapt to have less in future studies.
	 */
	public void initialiseCheckpoints() {

		checkPoints = new boolean[Settings.WIDTH][Settings.HEIGHT][Settings.DEPTH];

		// Currently each gridspace is set to true and thus each gridspace is a
		// checkpoint
		// but could adapt to have less in future studies.

		for (int a = 0; a < Settings.WIDTH; a++) {
			for (int b = 0; b < Settings.HEIGHT; b++) {
				for (int c = 0; c < Settings.DEPTH; c++) {
					checkPoints[a][b][c] = true;
				}
			}
		}
	}

	/**
	 * Tests whether co-ordinates x,y are in the circle centered at
	 * circleCentreX, circleCentreY with a specified radius
	 * 
	 * @param x
	 *            the x coordinate of the point to test
	 * @param y
	 *            the y coordinate of the point to test
	 * @param circleCentreX
	 *            the x coordinate of the circle center
	 * @param circleCentreY
	 *            the y coordinate of the circle center
	 * @param radius
	 *            the radius of the circle
	 * @return boolean determining whether inside (false) or outside (true) the
	 *         circle
	 */
	public static boolean isWithinCircle(double x, double y, double circleCentreX, double circleCentreY, int radius) {

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
	 * 
	 * @param celltype
	 * 			the cell type to seed
	 */
	void seedCells(CELLTYPE celltype) {

		int count = 0; // the number of cells to seed

		// set the number of cells to seed
		if (celltype == CELLTYPE.B) {
			count = Settings.BC.COUNT;
		} else if (celltype == CELLTYPE.cB) {
			count = Settings.BC.COGNATECOUNT;
		}

		// seed the cells
		for (int i = 0; i < count; i++) {
			switch (celltype) {

			case B:
				BC bc = new BC();

				int x = 1 + Settings.RNG.nextInt(Settings.WIDTH - 2);
				int y = 1 + Settings.RNG.nextInt(Settings.HEIGHT - 2);
				int z = 1 + Settings.RNG.nextInt(Settings.DEPTH - 2);

				bc.setObjectLocation(new Double3D(x, y, z));
				scheduleStoppableCell(bc);
				// so we only have 1 BC updating the ODE graph
				if (i == 0) {
					bc.displayODEGraph = true;
				}
				break;

			case cB:
				cognateBC cbc = new cognateBC(i);
				int x1 = 1 + Settings.RNG.nextInt(1 + Settings.WIDTH - 2);
				int y1 = 1 + Settings.RNG.nextInt(1 + Settings.HEIGHT - 2);
				int z1 = 1 + Settings.RNG.nextInt(1 + Settings.DEPTH - 2);
				cbc.setObjectLocation(new Double3D(x1, y1, z1));

				scheduleStoppableCell(cbc);
				break;

			default:
				break;
			}
		}
	}

	
	/**
	 * Generate a random coordinate within a circle located at the center of the environment
	 * 
	 * @param circleRadius
	 * 			the radius of the circle
	 * @return 
	 * 		 	a random Double3D inside a circle
	 */
	static Double3D generateCoordinateWithinCircle(int circleRadius) {

		int x, y, z;

		// keep generating new values while they are outside of the circle
		// the radius of the circle is 13 and it is inside this that we seed
		// the b cells
		do {
			x = Settings.RNG.nextInt(Settings.WIDTH - 2) + 1;
			y = Settings.RNG.nextInt(Settings.HEIGHT - 2) + 1;
			z = Settings.RNG.nextInt(Settings.DEPTH - 2) + 1;

		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1, (Settings.HEIGHT / 2) + 1, circleRadius) == false);

		return new Double3D(x, y, z);
	}

	/**
	 * Return all objects on the 3 stroma grids
	 * 
	 * @return 
	 * 			a bag containing all stromal cells in the sim
	 */
	public static Bag getAllStroma() {

		Bag stroma = new Bag();
		if (fdcEnvironment != null) {
			stroma.addAll(fdcEnvironment.getAllObjects());
		}
		if (brcEnvironment != null) {
			stroma.addAll(brcEnvironment.getAllObjects());
		}
		if (mrcEnvironment != null) {
			stroma.addAll(mrcEnvironment.getAllObjects());
		}

		return stroma;
	}

	/**
	 * Return all objects on the 3 stroma grids within a threshold distance
	 * 
	 * @return 
	 * 			a bag containing all stromal cells in the sim
	 */
	public static Bag getAllStromaWithinDistance(Double3D loc, double dist) {

		Bag stroma = new Bag();

		if (fdcEnvironment != null) {
			stroma.addAll(fdcEnvironment.getNeighborsExactlyWithinDistance(loc, dist));
		}
		if (brcEnvironment != null) {
			stroma.addAll(brcEnvironment.getNeighborsExactlyWithinDistance(loc, dist));
		}
		if (mrcEnvironment != null) {
			stroma.addAll(mrcEnvironment.getNeighborsExactlyWithinDistance(loc, dist));
		}

		return stroma;
	}

	/**
	 * this calculates the total number of stromal cells and the edges but is
	 * not a measure of theprotrusions or of the topology
	 * 
	 * @return
	 * 			an int array where the first element is the number of nodes in the sim
	 * 			and the second element is the number of edges in the sim
	 */
	public static int[] calculateNodesAndEdges() {

		int nodes = 0;
		int edges = 0;
		int[] output = new int[2];

		Bag stroma = getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {

				edges++;
			}

			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() != Stroma.TYPE.LEC) {
					nodes++;
				}
			}
		}

		output[0] = nodes;
		output[1] = edges;

		return output;
	}

	/**
	 * Measure all edges of a certain type of stromal cell
	 * 
	 * @param edgetype
	 * 		the edge type you want to quantify
	 * @return number of edges for a given stroma edge type
	 */
	private int calculateEdgesOfType(StromaEdge.TYPE edgetype) {

		Bag stroma = getAllStroma();
		int APcounter = 0;

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {

				StromaEdge.TYPE type = ((StromaEdge) stroma.get(i)).getStromaedgetype();

				// we want to include FDCs, MRCs and bRCs
				if (type == edgetype) {
					APcounter += 1;
				}
			}
		}
		return APcounter;
	}

}
