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
import sim3d.stroma.FDC;
import sim3d.stroma.FRC;
import sim3d.stroma.LEC;
import sim3d.stroma.MRC;
import sim3d.stroma.ReticularFiber;
import sim3d.stroma.StromaEdge;
import sim3d.stroma.branch;
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
	 * object as a variable within the BC class.
	 * Then the BC can access its stopper variable and call the stop method.
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
		
		frcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		
		FDC.drawEnvironment = fdcEnvironment;
		//StromaEdge.drawEnvironment = fdcEnvironment;
		
		StromaEdge.drawEnvironment = fdcEnvironment;
		System.out.println("before changing: " + StromaEdge.drawEnvironment.equals(frcEnvironment));
		
		
		
		FRC.drawEnvironment = frcEnvironment;
		ReticularFiber.drawEnvironment = frcEnvironment;
		
		System.out.println("after changing: " + StromaEdge.drawEnvironment.equals(frcEnvironment));
		
		
		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// initialise chemokines
		CXCL13 = new Chemokine(schedule, Chemokine.TYPE.CXCL13,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		
		// initialise the CXCL13 grid
		CCL19 = new Chemokine(schedule, Chemokine.TYPE.CCL19,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		// initialiseStroma(cgGrid); // initialise the stromal network
		initialiseFDC(cgGrid);
		initialiseFRC(cgGrid);
		
		
	
		
		
		//now seed the LECs
		//iterate through the X and Z axes keeping Y fixed to seed the SCS
		for(int x = 0; x < Settings.WIDTH; x ++){
			for(int z = 0; z < Settings.DEPTH; z ++){
			LEC flec = new LEC();
			LEC clec = new LEC();
			
			clec.setObjectLocation(new Double3D(x,Settings.HEIGHT,z));
			flec.setObjectLocation(new Double3D(x,Settings.HEIGHT-2,z));
			
			flec.registerCollisions(cgGrid);
			clec.registerCollisions(cgGrid);
		
			}
		}
		
		
		
		
		
		
		
		
		
		
		
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
	 * Seeds B cells into the FDC network
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
			
			//TODO we need to able to alter this value!!!
			// this is not consistent with our parameter values!!!
		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13) == false);

		return new Double3D(x, y, z);
	}

	/*
	 * Generate and initialise an FRC network
	 */
	
	/*
	  private void initialiseStroma(CollisionGrid cgGrid) {
	  
		  // Generate some stroma 
		  
		  ArrayList<FRCCell> frclCellLocations = new ArrayList<FRCCell>(); 
		  ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
	  
		  //generate the stromal network 
		  //but fine for now 
		  FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				  Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				  frclCellLocations, sealEdges);
	  
		  // Create the FDC objects, display them, schedule them, and then put 
		  //them on the collision grid 
	  
		  for (FRCCell frcCell : frclCellLocations) {
	  
			  FDC fdc = new FDC();
	  
			  // This will register the FDC with the environment/display 
			  // to account for the border which is one gridspace in width 
	  
			  fdc.setObjectLocation(new
					  Double3D(frcCell.d3Location.x + 1, frcCell.d3Location.y + 1,
							  frcCell.d3Location.z + 1));
	  
			  // Schedule the secretion of chemokine, needs to be ordered so that 
			  // a chemokine 
			  // can't diffuse before it is secreted
			  schedule.scheduleRepeating(fdc, 2, 1);
	  
			  // fdc.registerCollisions( cgGrid ); }
	  
			  // Add the stroma edges to the display/CollisionGrid 
	  
			  for (StromaEdge seEdge : sealEdges) { 
				  
				  Double3D d3Point = seEdge.getPoint1(); 
			  }
			  
			  Double3D d3Point2 = seEdge.getPoint2();
	  
			  // Check if it's out of bounds 
			  if (!(d3Point.x <= 0 || d3Point.x >=
					  (Settings.WIDTH - 2) || d3Point.y <= 0 || d3Point.y >= (Settings.HEIGHT -
							  2) || d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2)) &&
							  !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2) || d3Point2.y <=
							  0 || d3Point2.y >= (Settings.HEIGHT - 2) || d3Point2.z <= 0 || d3Point2.z
							  >= (Settings.DEPTH - 2))) {
	  
				  //TODO assess whether iCat is needed int iCat = (int) (5 *
				  (seEdge.getPoint2() .subtract(seEdge.getPoint1()).length() - 1.2));
	  
			  } // Register with display and CG seEdge.setObjectLocation(new
			  Double3D(seEdge.x + 1, seEdge.y + 1, seEdge.z + 1));
			  seEdge.registerCollisions(cgGrid); }
	  
		  	// All the static cells are in, now reset the collision data
		  	cgGrid.step(null);
	  
		  	//count the entire number of dendrites so we can get a percentage scanned measure 
		  	totalNumberOfDendrites = sealEdges.size();
	  }
	*/

	/*
	 * Generate and initialise a stromal network 
	 * an FDC is the same as an FRC network, except
	 * we add additional branches which connect
	 * dendrites to give a web-like morphology
	 * comparable to those observed in the 
	 * CXCL13 reporter mice.
	 * 
	 * 
	 * TODO I think a visitor design pattern would come in handy
	 * here so that later on we can implement an FDC or an FRC
	 * without cluttering up the simulaiton environment class
	 * 

	 */
	void initialiseFDC(CollisionGrid cgGrid) {

		
		//TODO we should really change this or it will get confusing....
		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> frclCellLocations = new ArrayList<StromaGenerator.StromalCell>();
		
		//ArrayList<StromaGenerator2.StromalCell> frclCellLocations = new ArrayList<StromaGenerator2.StromalCell>();
		
		//this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		
		//TODO this needs to be though about carefully
		
		Double3D center = new Double3D(Settings.WIDTH/2,Settings.HEIGHT/2,Settings.DEPTH/2);
		
		StromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, 225,
				frclCellLocations, sealEdges);
		
		
		//StromaGenerator2.generateStroma3D(35,
		//		35, Settings.DEPTH - 2,25,25,0, Settings.FDC.COUNT,
		//		frclCellLocations, sealEdges);

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (StromaGenerator.StromalCell frcCell : frclCellLocations) {

			FDC fdc = new FDC();

			// This will register the FDC with the environment/display
			// to account for the border which is one gridspace in width
			fdc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
					frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

			schedule.scheduleRepeating((Steppable) fdc, 2, 1);

		}

		//now need to generate the actual dendrites
		generateFDCDendrites(cgGrid, sealEdges);
			
	}

	
	
	
	public void generateFDCDendrites(CollisionGrid cgGrid,ArrayList<StromaEdge> sealEdges){
		// Add the stroma edges to the display/CollisionGrid
		// for all the dendrites generated by the stroma generator
				for (StromaEdge seEdge : sealEdges) {
					Double3D d3Point = seEdge.getPoint1();
					Double3D d3Point2 = seEdge.getPoint2();

				
	
					
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

							//add one branch to each
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
				//calculate the total number of dendrites in the network
				totalNumberOfDendrites = calculateTotalNumberOfDendrites();
				System.out.println("totalStromaObjects: " + fdcEnvironment.getAllObjects().size());
				System.out.println("totalNumberOfBranches: " + totalNumberOfDendrites);
				
	}
	
	
	
	/**
	 * Calculate the number of dendrites in the FDC network
	 * excluding the FDC nodes
	 * @return the number of dendrites as an integer value
	 */
	private int calculateTotalNumberOfDendrites(){
		
		Bag stroma = fdcEnvironment.getAllObjects();
		int FDCcounter = 0;
		
		
		//we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for(int i= 0 ; i < stroma.size(); i++){
			if(stroma.get(i) instanceof FDC){
				FDCcounter += 1;
			}
		}
		
		//subtract number of nodes to get number of edges
		int number = stroma.size() - FDCcounter;
		
		return number;
		
	}
	
	
	
	/*
	 * Generate and initialise a stromal network 
	 * an FDC is the same as an FRC network, except
	 * we add additional branches which connect
	 * dendrites to give a web-like morphology
	 * comparable to those observed in the 
	 * CXCL13 reporter mice.
	 * 
	 * 
	 * TODO I think a visitor design pattern would come in handy
	 * here so that later on we can implement an FDC or an FRC
	 * without cluttering up the simulaiton environment class
	 * 

	 */
	void initialiseFRC(CollisionGrid cgGrid) {

		
		//TODO we should really change this or it will get confusing....
		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> frclCellLocations = new ArrayList<StromaGenerator.StromalCell>();
		
		//this is for the dendrites
		ArrayList<ReticularFiber> sealEdges = new ArrayList<ReticularFiber>();

		//need to set it so it doesnt overlap with the FDCs...
		StromaGenerator.generateFRC3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 3, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (StromaGenerator.StromalCell frcCell : frclCellLocations) {

			

			//TODO we need to make sure these guys are going onto the FRC grid
			//but lets do that later
			if(!isWithinCircle((int)frcCell.d3Location.x + 1,(int)frcCell.d3Location.y + 1,Settings.WIDTH/2,Settings.HEIGHT/2,15))
			{	
				if(frcCell.d3Location.y < Settings.HEIGHT - 10)
				{
					FRC frc = new FRC();
					// This will register the FDC with the environment/display
					// to account for the border which is one gridspace in width
					frc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
						frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

					schedule.scheduleRepeating((Steppable) frc, 2, 1);
				}	
				else{
					MRC mrc = new MRC();
					// This will register the FDC with the environment/display
					// to account for the border which is one gridspace in width
					mrc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
						frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

					schedule.scheduleRepeating((Steppable) mrc, 2, 1);
				}
			}
			
			//TODO we need to make sure these guys are going onto the FRC grid
			//but lets do that later
			//also if they are directly under the SCS
			if(isWithinCircle((int)frcCell.d3Location.x + 1,(int)frcCell.d3Location.y + 1,Settings.WIDTH/2,Settings.HEIGHT/2,15))
			{
				
				if(!isWithinCircle((int)frcCell.d3Location.x + 1,(int)frcCell.d3Location.y + 1,Settings.WIDTH/2,Settings.HEIGHT/2,10))
				{
				
					MRC mrc = new MRC();
					// This will register the FDC with the environment/display
					// to account for the border which is one gridspace in width
					mrc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
						frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

					schedule.scheduleRepeating((Steppable) mrc, 2, 1);
				}
				
			}
			
			

			

		}

		//now need to generate the actual dendrites
		generateFRCDendrites(cgGrid, sealEdges);
			
	}

	
	/**
	 * Diabolical code, needs refactoring
	 * @param cgGrid
	 * @param sealEdges
	 */
	
	public void generateFRCDendrites(CollisionGrid cgGrid,ArrayList<ReticularFiber> sealEdges){
		// Add the stroma edges to the display/CollisionGrid
		// for all the dendrites generated by the stroma generator
				for (ReticularFiber seEdge : sealEdges) {


		
	
					
				
				
				if(isWithinCircle((int)seEdge.x,(int)seEdge.y,Settings.WIDTH/2,Settings.HEIGHT/2,15))
				{
					
					if(!isWithinCircle((int)seEdge.x,(int)seEdge.y,Settings.WIDTH/2,Settings.HEIGHT/2,10))
					{
						seEdge.setM_col(Settings.FDC.DRAW_COLOR());
						// Register with display and CG
						seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
								seEdge.z + 1));
						seEdge.registerCollisions(cgGrid);
					}
				}
				
				else if (seEdge.y < Settings.HEIGHT-9){
					seEdge.setM_col(Settings.FRC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
							seEdge.z + 1));
					seEdge.registerCollisions(cgGrid);
					
				}
				
				else{
					seEdge.setM_col(Settings.FDC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
							seEdge.z + 1));
					seEdge.registerCollisions(cgGrid);
					
					
				}
				
				
				
			}
			
	}
	
	
	

}
