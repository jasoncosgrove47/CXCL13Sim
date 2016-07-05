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

	/**
	 * Instance of the particle moles class
	 */
	public static Chemokine particlemoles;

	/*
	 * Parameter file: XML format
	 */
	public static Document parameters;

	/**
	 * ENUM for the cell types
	 */
	public enum CELLTYPE {
		B, cB
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
	 * object as a variable within the BC class.
	 * Then the BC can access its stopper variable and call the stop method.
	 */
	public void scheduleStoppableCell(BC bc) {
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
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;

		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// initialise the CXCL13 grid
		particlemoles = new Chemokine(schedule, Chemokine.TYPE.CXCL13,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		// initialiseStroma(cgGrid); // initialise the stromal network
		initialiseFDC(cgGrid);

		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		// seed B-cells within the follicle
		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);

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
	 * Seeds B cells
	 */
	public void seedCells(CELLTYPE celltype) {
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
		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13) == false);

		return new Double3D(x, y, z);
	}

	/*
	 * Generate and initialise an FRC network
	 */
	/*
	 * private void initialiseStroma(CollisionGrid cgGrid) {
	 * 
	 * // Generate some stroma ArrayList<FRCCell> frclCellLocations = new
	 * ArrayList<FRCCell>(); ArrayList<StromaEdge> sealEdges = new
	 * ArrayList<StromaEdge>();
	 * 
	 * //generate the stromal network 
	 * but fine for now FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
	 * Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
	 * frclCellLocations, sealEdges);
	 * 
	 * // Create the FDC objects, display them, schedule them, and then put //
	 * them on the collision grid for (FRCCell frcCell : frclCellLocations) {
	 * 
	 * FDC fdc = new FDC();
	 * 
	 * // This will register the FDC with the environment/display // to account
	 * for the border which is one gridspace in width fdc.setObjectLocation(new
	 * Double3D(frcCell.d3Location.x + 1, frcCell.d3Location.y + 1,
	 * frcCell.d3Location.z + 1));
	 * 
	 * // Schedule the secretion of chemokine, needs to be ordered so that // a
	 * chemokine // can't diffuse before it is secreted
	 * schedule.scheduleRepeating(fdc, 2, 1);
	 * 
	 * // fdc.registerCollisions( cgGrid ); }
	 * 
	 * // Add the stroma edges to the display/CollisionGrid for (StromaEdge
	 * seEdge : sealEdges) { Double3D d3Point = seEdge.getPoint1(); Double3D
	 * d3Point2 = seEdge.getPoint2();
	 * 
	 * // Check if it's out of bounds if (!(d3Point.x <= 0 || d3Point.x >=
	 * (Settings.WIDTH - 2) || d3Point.y <= 0 || d3Point.y >= (Settings.HEIGHT -
	 * 2) || d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2)) &&
	 * !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2) || d3Point2.y <=
	 * 0 || d3Point2.y >= (Settings.HEIGHT - 2) || d3Point2.z <= 0 || d3Point2.z
	 * >= (Settings.DEPTH - 2))) {
	 * 
	 * //TODO assess whether iCat is needed int iCat = (int) (5 *
	 * (seEdge.getPoint2() .subtract(seEdge.getPoint1()).length() - 1.2));
	 * 
	 * } // Register with display and CG seEdge.setObjectLocation(new
	 * Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));
	 * seEdge.registerCollisions(cgGrid); }
	 * 
	 * // All the static cells are in, now reset the collision data
	 * cgGrid.step(null);
	 * 
	 * //count the entire number of dendrites so we can get a percentage scanned
	 * measure totalNumberOfDendrites = sealEdges.size();
	 * 
	 * }
	 */

	/*
	 * Generate and initialise a stromal network 
	 */
	void initialiseFDC(CollisionGrid cgGrid) {

		// Generate some stroma
		ArrayList<StromaGenerator.FRCCell> frclCellLocations = new ArrayList<StromaGenerator.FRCCell>();
		
		//this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		StromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (StromaGenerator.FRCCell frcCell : frclCellLocations) {

			FDC fdc = new FDC();

			// This will register the FDC with the environment/display
			// to account for the border which is one gridspace in width
			fdc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
					frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

			schedule.scheduleRepeating((Steppable) fdc, 2, 1);

		}

		//now need to generate the actual dendrites
		generateDendrites(cgGrid, sealEdges);
			
	}

	
	
	
	public void generateDendrites(CollisionGrid cgGrid,ArrayList<StromaEdge> sealEdges){
		// Add the stroma edges to the display/CollisionGrid
		// for all the dendrites generated by the stroma generator
				for (StromaEdge seEdge : sealEdges) {
					Double3D d3Point = seEdge.getPoint1();
					Double3D d3Point2 = seEdge.getPoint2();

					// Check if it's out of bounds
					if (!(d3Point.x <= 0 || d3Point.x >= (Settings.WIDTH - 2)
							|| d3Point.y <= 0 || d3Point.y >= (Settings.HEIGHT - 2)
							|| d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2))
							&& !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2)
									|| d3Point2.y <= 0
									|| d3Point2.y >= (Settings.HEIGHT - 2)
									|| d3Point2.z <= 0 || d3Point2.z >= (Settings.DEPTH - 2))) {

						
						//TODO what is this doing, need to discuss with Simon
						int iCat = (int) (5 *
								  (seEdge.getPoint2() .subtract(seEdge.getPoint1()).length() - 1.2));
						
					}
	
					
					int branchesAdded = 0;
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
							seEdge.z + 1));
					seEdge.registerCollisions(cgGrid);

					
					
					
					// add branches to neighbouring cells
					
					//see how many cells are with a certain threshold of the midpoint of the stromal edge
					Bag neighbouredges = fdcEnvironment
							.getNeighborsExactlyWithinDistance(seEdge.midpoint, 2);

					//iterate through the neighbours and if its a stroma edge then add a branch
					for (int j = 0; j < neighbouredges.size() - 1; j++) {
						if (neighbouredges.get(j) instanceof StromaEdge) {

							//only add one branch for each or it gets crazy
							if (branchesAdded < 1) {
								StromaEdge neighbouredge = (StromaEdge) neighbouredges
										.get(j);
								branch b = new branch(seEdge.midpoint,
										neighbouredge.midpoint);
								b.setObjectLocation(new Double3D(b.x + 1, b.y + 1,
										b.z + 1));

								//now do the same for the branches!
								Bag branchedges = fdcEnvironment
										.getNeighborsExactlyWithinDistance(b.midpoint,
												2);

								int subbranches = 0;

								for (int k = 0; k < branchedges.size() - 1; k++) {

									if (subbranches < 1) {
										if (branchedges.get(k) instanceof FDC) {
											FDC fdc = (FDC) branchedges.get(k);
											branch b2 = new branch(b.midpoint,
													new Double3D(fdc.x, fdc.y, fdc.z));
											b2.setObjectLocation(new Double3D(b2.x + 1,
													b2.y + 1, b2.z + 1));

											b2.registerCollisions(cgGrid);

											
										}

										subbranches += 1;
									}
								}
								branchesAdded += 1;
							}
						}
					}
				}

				
				
				//TODO test case to make sure that this is sensible
				
				//now calculate the total number of dendrites
				Bag stroma = fdcEnvironment.getAllObjects();
				int FDCcounter = 0;
				
				for(int i= 0 ; i < stroma.size(); i++){
					if(stroma.get(i) instanceof FDC){
						FDCcounter += 1;
					}
				}
				
				totalNumberOfDendrites = stroma.size() - FDCcounter;
				
	}
	
	

}
