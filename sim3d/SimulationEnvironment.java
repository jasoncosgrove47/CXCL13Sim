package sim3d;

import java.util.ArrayList;

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
import sim3d.util.StromaGenerator;

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
	public static int totalNumberOfDendrites = 0;

	/**
	 * A static instance of the simulation that only get's set here
	 */
	public static SimulationEnvironment simulation;

	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D fdcEnvironment;

	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D frcEnvironment;

	/**
	 * Instance of the particle moles class
	 */
	public static Chemokine CXCL13;

	/**
	 * Instance of the particle moles class
	 */
	public static Chemokine CCL19;

	/**
	 * Instance of the particle moles class
	 */
	public static Chemokine EBI2L;

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
	public void setDisplayLevel(int m_iDisplayLevel) {
		Chemokine.setDisplayLevel(m_iDisplayLevel - 1);
		Chemokine.getInstance(Chemokine.TYPE.CXCL13).updateDisplay();
	}

	/*
	 * Scheduling a cell returns a stoppable object. We store the stoppable
	 * object as a variable within the BC class. Then the BC can access its
	 * stopper variable and call the stop method.
	 */
	public void scheduleStoppableCell(Lymphocyte bc) {
		bc.setStopper(schedule.scheduleRepeating(bc));
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

		// initialise the CXCL13 grid
		CCL19 = new Chemokine(schedule, Chemokine.TYPE.CCL19, Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH);

		// initialise the CXCL13 grid
		EBI2L = new Chemokine(schedule, Chemokine.TYPE.EBI2L, Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH);

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		initialiseStromalCell(cgGrid, Stroma.TYPE.FDC);
		initialiseStromalCell(cgGrid, Stroma.TYPE.FRC);
		initialiseStromalCell(cgGrid, Stroma.TYPE.MRC);
		seedSCS(cgGrid);

		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		// seed lymphocytes within the follicle
		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);
		seedCells(CELLTYPE.T);
	}

	/**
	 * Seed lymphatic endothelial cells at the subcapuslar sinus
	 * 
	 * @param cgGrid
	 */
	private void seedSCS(CollisionGrid cgGrid) {
		// now seed the LECs
		// iterate through the X and Z axes keeping Y fixed to seed the SCS
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {
				Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma clec = new Stroma(Stroma.TYPE.LEC);

				clec.setObjectLocation(new Double3D(x, Settings.HEIGHT, z));
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - 2, z));

				flec.registerCollisions(cgGrid);
				clec.registerCollisions(cgGrid);

			}
		}
	}

	/**
	 * Tests whether co-ordinates x,y are not in the circle centered at
	 * circleCentreX, circleCentreY with a specified radius
	 * 
	 * @return boolean determining whether inside (false) or outside (true) the
	 *         circle
	 */
	public boolean isWithinCircle(int x, int y, int circleCentreX,
			int circleCentreY, int radius) {

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
	public void seedCells(CELLTYPE celltype) {
		int count = 0; // the number of cells to seed

		// set the number of cells to seed
		if (celltype == CELLTYPE.B) {
			count = Settings.BC.COUNT;
		} else if (celltype == CELLTYPE.cB) {
			count = Settings.BC.COGNATECOUNT;
		} else if (celltype == CELLTYPE.T) {
			count = Settings.BC.COGNATECOUNT;
		}

		// seed the cells
		for (int i = 0; i < count; i++) {
			switch (celltype) {

			case B:
				BC bc = new BC();
				bc.setObjectLocation(generateCoordinateWithinCircle());
				scheduleStoppableCell(bc);
				// so we only have 1 BC updating the ODE graph
				if (i == 0) {
					bc.displayODEGraph = true;
				}
				break;

			case cB:
				cognateBC cbc = new cognateBC(i);
				cbc.setObjectLocation(generateCoordinateWithinCircle());
				scheduleStoppableCell(cbc);
				break;

			case T:
				TC tc = new TC();
				tc.setObjectLocation(generateCoordinateOutsideCircle());
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
	public Double3D generateCoordinateWithinCircle() {

		int x, y, z;
		do {
			x = random.nextInt(Settings.WIDTH - 2) + 1;
			y = random.nextInt(Settings.HEIGHT - 2) + 1;
			z = random.nextInt(Settings.DEPTH - 2) + 1;

			// keep generating new values while they are outside of the circle
			// the radius of the circle is 13 and it is inside this that we seed
			// the b cells

			// TODO we need to able to alter this value!!!
			// this is not consistent with our parameter values!!!
		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13) == false);

		return new Double3D(x, y, z);
	}

	
	/**
	 * @return a random Double3D inside a circle
	 */
	public Double3D generateCoordinateOutsideCircle() {

		int x, y, z;
		do {
			x = random.nextInt(Settings.WIDTH - 2) + 1;
			y = random.nextInt(Settings.HEIGHT - 2) + 1;
			z = random.nextInt(Settings.DEPTH - 2) + 1;

			// keep generating new values while they are outside of the circle
			// the radius of the circle is 13 and it is inside this that we seed
			// the b cells

			// TODO we need to able to alter this value!!!
			// this is not consistent with our parameter values!!!
		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13) == true);

		return new Double3D(x, y, z);
	}
	
	private void initialiseStromalCell(CollisionGrid cgGrid,
			Stroma.TYPE celltype) {

		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> stromalCellLocations = 
				new ArrayList<StromaGenerator.StromalCell>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		if (celltype == Stroma.TYPE.FDC) {
			StromaGenerator.generateStroma3D(Settings.WIDTH - 2,
					Settings.HEIGHT - 2, Settings.DEPTH - 2, 225,
					stromalCellLocations, sealEdges);
		} else {

			StromaGenerator.generateFRC3D(Settings.WIDTH - 2,
					Settings.HEIGHT - 3, Settings.DEPTH - 2,
					Settings.FDC.COUNT, stromalCellLocations, sealEdges);
		}

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (StromaGenerator.StromalCell sc : stromalCellLocations) {
			switch (celltype) {

			case FDC:
				seedFDC(sc);
				break;

			case FRC:
				seedFRC(sc);
				break;

			case MRC:
				seedMRC(sc);
				break;

			default:
				break;

			}

		}

		if (celltype == Stroma.TYPE.FDC) {
			// now need to generate the actual dendrites
			generateDendrites(cgGrid, sealEdges, StromaEdge.TYPE.FDC_edge);
		} else {
			generateDendrites(cgGrid, sealEdges, StromaEdge.TYPE.RC_edge);
		}

	}

	/**
	 * Seed FDC cells within the follicle
	 * 
	 * @param sc
	 */
	private void seedFDC(StromaGenerator.StromalCell sc) {
		Stroma stromalcell = new Stroma(Stroma.TYPE.FDC);
		stromalcell.setObjectLocation(new Double3D(sc.d3Location.x + 1,
				sc.d3Location.y + 1, sc.d3Location.z + 1));

		schedule.scheduleRepeating((Steppable) stromalcell, 2, 1);
	}

	/**
	 * Seed FRC cells within the follicle
	 * 
	 * @param sc
	 */
	private void seedFRC(StromaGenerator.StromalCell sc) {
		if (!isWithinCircle((int) sc.d3Location.x + 1,
				(int) sc.d3Location.y + 1, Settings.WIDTH / 2,
				Settings.HEIGHT / 2, 15)) {
			if (sc.d3Location.y < Settings.HEIGHT - 8) {
				Stroma frc = new Stroma(Stroma.TYPE.FRC);
				// This will register the FDC with the environment/display
				// to account for the border which is one gridspace in width
				frc.setObjectLocation(new Double3D(sc.d3Location.x + 1,
						sc.d3Location.y + 1, sc.d3Location.z + 1));

				schedule.scheduleRepeating((Steppable) frc, 2, 1);
			}
		}
	}

	private void seedMRC(StromaGenerator.StromalCell sc) {

		if (!isWithinCircle((int) sc.d3Location.x + 1,
				(int) sc.d3Location.y + 1, Settings.WIDTH / 2,
				Settings.HEIGHT / 2, 15)) {
			if (sc.d3Location.y >= Settings.HEIGHT - 8) {

				Stroma mrc = new Stroma(Stroma.TYPE.MRC);
				// This will register the FDC with the environment/display
				// to account for the border which is one gridspace in width
				mrc.setObjectLocation(new Double3D(sc.d3Location.x + 1,
						sc.d3Location.y + 1, sc.d3Location.z + 1));

				schedule.scheduleRepeating((Steppable) mrc, 2, 1);
			}

		}

		// also if they are directly under the SCS
		else if (isWithinCircle((int) sc.d3Location.x + 1,
				(int) sc.d3Location.y + 1, Settings.WIDTH / 2,
				Settings.HEIGHT / 2, 15)) {

			if (!isWithinCircle((int) sc.d3Location.x + 1,
					(int) sc.d3Location.y + 1, Settings.WIDTH / 2,
					Settings.HEIGHT / 2, 10)) {

				Stroma mrc = new Stroma(Stroma.TYPE.MRC);
				// This will register the FDC with the environment/display
				// to account for the border which is one gridspace in width
				mrc.setObjectLocation(new Double3D(sc.d3Location.x + 1,
						sc.d3Location.y + 1, sc.d3Location.z + 1));

				schedule.scheduleRepeating((Steppable) mrc, 2, 1);
			}

		}

	}

	// TODO this needs a lot of refactoring but passes the tests for the moment
	// anyway
	private void generateDendrites(CollisionGrid cgGrid,
			ArrayList<StromaEdge> sealEdges, StromaEdge.TYPE type) {

		if (type == StromaEdge.TYPE.RC_edge) {
			for (StromaEdge seEdge : sealEdges) {
				if (isWithinCircle((int) seEdge.x, (int) seEdge.y,
						Settings.WIDTH / 2, Settings.HEIGHT / 2, 15)) {

					if (!isWithinCircle((int) seEdge.x, (int) seEdge.y,
							Settings.WIDTH / 2, Settings.HEIGHT / 2, 10)) {
						seEdge.setM_col(Settings.FDC.DRAW_COLOR());
						// Register with display and CG
						seEdge.setObjectLocation(new Double3D(seEdge.x + 1,
								seEdge.y + 1, seEdge.z + 1));
						seEdge.registerCollisions(cgGrid);
					}
				}

				// if it's an FRC
				else if (seEdge.y < Settings.HEIGHT - 8) {
					seEdge.setM_col(Settings.FRC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x + 1,
							seEdge.y + 1, seEdge.z + 1));
					seEdge.registerCollisions(cgGrid);

				}
				// if it's an MRC
				else {
					seEdge.setM_col(Settings.FDC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x + 1,
							seEdge.y + 1, seEdge.z + 1));
					seEdge.registerCollisions(cgGrid);

				}

			}

		}

		if (type == StromaEdge.TYPE.FDC_edge) {
			// Add the stroma edges to the display/CollisionGrid
			// for all the dendrites generated by the stroma generator
			for (StromaEdge seEdge : sealEdges) {

				int branchesAdded = 0;
				// Register with display and CG
				seEdge.setObjectLocation(new Double3D(seEdge.x + 1,
						seEdge.y + 1, seEdge.z + 1));
				seEdge.registerCollisions(cgGrid);

				// add branches to neighbouring cells
				// see how many cells are with a certain threshold of the
				// midpoint of the stromal edge
				Bag neighbouredges = fdcEnvironment
						.getNeighborsExactlyWithinDistance(seEdge.midpoint, 2);

				// iterate through the neighbours and if its a stroma edge then
				// add a branch
				for (int j = 0; j < neighbouredges.size() - 1; j++) {
					if (neighbouredges.get(j) instanceof StromaEdge) {

						// add one branch to each
						if (branchesAdded < 1) {
							StromaEdge neighbouredge = (StromaEdge) neighbouredges
									.get(j);
							StromaEdge b = new StromaEdge(seEdge.midpoint,
									neighbouredge.midpoint,
									StromaEdge.TYPE.FDC_branch);
							b.setObjectLocation(new Double3D(b.x + 1, b.y + 1,
									b.z + 1));

							// now do the same for the branches!
							Bag branchedges = fdcEnvironment
									.getNeighborsExactlyWithinDistance(
											b.midpoint, 2);

							int subbranches = 0;

							for (int k = 0; k < branchedges.size() - 1; k++) {

								if (subbranches < 1) {
									if (branchedges.get(k) instanceof Stroma) {
										Stroma fdc = (Stroma) branchedges
												.get(k);
										if (fdc.getStromatype() == Stroma.TYPE.FDC) {
											StromaEdge b2 = new StromaEdge(
													b.midpoint,
													new Double3D(fdc.x, fdc.y,
															fdc.z),
													StromaEdge.TYPE.FDC_branch);

											b2.setObjectLocation(new Double3D(
													b2.x + 1, b2.y + 1,
													b2.z + 1));

											b2.registerCollisions(cgGrid);
										}
									}
									subbranches += 1;
								}
							}
							branchesAdded += 1;
						}
					}
				}
			}
			// calculate the total number of dendrites in the network
			totalNumberOfDendrites = calculateTotalNumberOfDendrites();
			System.out.println("totalStromaObjects: "
					+ fdcEnvironment.getAllObjects().size());
			System.out.println("totalNumberOfBranches: "
					+ totalNumberOfDendrites);
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

}
