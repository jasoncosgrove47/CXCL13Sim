package sim3d;


import java.util.ArrayList;

import org.w3c.dom.Document;

import dataLogger.Controller;
import dataLogger.ReadObjects;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.FRCStromaGenerator.FRCCell;


/**
 * TODO this also needs to be a singleton class
 * 
 * 
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

	
	public static int totalNumberOfDendrites = 0;
	
	
	/**
	 * A static instance of the simulation that only
	 * get's set here - there may be a better way
	 * to do this TODO
	 */
	public static SimulationEnvironment simulation;

	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D fdcEnvironment;
	
	
	public static ParticleMoles particlemoles;

	/*
	 * Parameter file: XML format
	 */
	public static Document parameters;

	/*
	 * Controller responsible for recording data from the simulation
	 */
	//private static Controller controller;

	/**
	 * ENUM for the cell types
	 */
	public enum CELLTYPE {
		B, cB, T
	}

	// what type of cell we are dealing with
	public TYPE celltype;

	/**
	 * Constructor
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
	 * Adds a slider for the display level in the MASON console
	 */
	public Object domDisplayLevel() {
		return new sim.util.Interval(1, Settings.DEPTH);
	}

	/**
	 * Destroy resources after use 
	 */
	public void finish() {
		Particle.reset();
	}

	/**
	 * Accessor for the current display level - a z-index to use for displaying
	 * the diffusion
	 */
	public int getDisplayLevel() {
		return Particle.getDisplayLevel() + 1; // Add 1 so the scale goes from 1
											   // to 10 and not 0 to 9!
	}

	/**
	 * Setter for the current display level
	 */
	public void setDisplayLevel(int m_iDisplayLevel) {
		Particle.setDisplayLevel(m_iDisplayLevel - 1);
		Particle.getInstance(Particle.TYPE.CXCL13).updateDisplay();
	}

	/**
	 * This is used for high throughput experimentation and allows the system to
	 * reach a steady state before recording B-cell migration, ensures that
	 * outputs from parameter sweeps are not due to recording before the system
	 * reaches steady state
	 */
	public void Adaptivestart() {

		super.start();

		// Initialise the stromal grid
		fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;

		// Initialise the B cell grid
		// better to have it in BC
		// as can then access it from test cases
		// without instantiating simulation environment
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION,
				Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH);
		BC.drawEnvironment = BC.bcEnvironment;

		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);

		// initialise the relevant agents
		initialiseStroma(cgGrid); // initialise the stromal network
		particlemoles  = new ParticleMoles(schedule,
				ParticleMoles.TYPE.CXCL13, Settings.WIDTH, Settings.HEIGHT,
				Settings.DEPTH);

		// let chemokine stabilise before adding the other cells
		//runChemokineUntilSteadyState();


		//let chemokine stabilise before adding other cells
		for (int i = 0; i < 500; i++) {
			schedule.step(this);
		}
		
		
		// set the collision grid for B cells
		BC.m_cgGrid = cgGrid;

		// seed our tracker cells and let them reach steady state before seeding
		// our non-tracker cells
		seedCells(CELLTYPE.cB);
		//runBCellsUntilSteadyState();
		seedCells(CELLTYPE.B);

		// step a little bit more so the cells have more time to stabilise
		for (int i = 0; i < 50; i++) {
			schedule.step(this);

		}

		// now initialise the datalogger

		schedule.scheduleRepeating(Controller.getInstance());

		// set the steady state guard to true so
		// b cells can begin recording data
		steadyStateReached = true;
	}

	/*
	 * let's chemokine diffuse until a steady state is reached to speed up the
	 * time taken to run one simulation
	 */
	public void runChemokineUntilSteadyState() {

		double totalConc_Tminus10;
		double totalConc_T;
		double[][][] ia3Concs;

		// let the chemokine diffuse a little bit
		for (int i = 0; i < 10; i++) {
			schedule.step(this);
			ia3Concs = ParticleMoles
					.get(ParticleMoles.TYPE.CXCL13, Settings.WIDTH / 2,
							Settings.HEIGHT / 2, Settings.DEPTH / 2);

			if (i == 0) {
				totalConc_Tminus10 = ia3Concs[2][1][1] + ia3Concs[0][1][1]
						+ ia3Concs[1][2][1] + ia3Concs[1][0][1]
						+ ia3Concs[1][1][2] + ia3Concs[1][1][0];
			}

			if (i == 9) {
				totalConc_T = ia3Concs[2][1][1] + ia3Concs[0][1][1]
						+ ia3Concs[1][2][1] + ia3Concs[1][0][1]
						+ ia3Concs[1][1][2] + ia3Concs[1][1][0];
			}
		}

		// TODO potential here infinite loops, need some additional control to
		// make sure that this doesn't happen
		// if chemokine hasn't stabilised at this point, continue to step until
		// it has
		do {
			ia3Concs = ParticleMoles
					.get(ParticleMoles.TYPE.CXCL13, Settings.WIDTH / 2,
							Settings.HEIGHT / 2, Settings.DEPTH / 2);
			totalConc_Tminus10 = ia3Concs[2][1][1] + ia3Concs[0][1][1]
					+ ia3Concs[1][2][1] + ia3Concs[1][0][1] + ia3Concs[1][1][2]
					+ ia3Concs[1][1][0];


			
			
			for (int i = 0; i < 10; i++) {
				schedule.step(this);
			}

			ia3Concs = ParticleMoles
					.get(ParticleMoles.TYPE.CXCL13, Settings.WIDTH / 2,
							Settings.HEIGHT / 2, Settings.DEPTH / 2);
			totalConc_T = ia3Concs[2][1][1] + ia3Concs[0][1][1]
					+ ia3Concs[1][2][1] + ia3Concs[1][0][1] + ia3Concs[1][1][2]
					+ ia3Concs[1][1][0];
		}

		while ( (totalConc_Tminus10 - totalConc_T) >0.001);
		System.out.println("Chemokine has stabilised: "
				+ this.schedule.getSteps());
	}

	/**
	 * Run BCs and track their receptor status to see if it has stabilised
	 * before recording any data, need to do something for cases where it
	 * doesn't stabilise
	 */
	public void runBCellsUntilSteadyState() {

		cognateBC cbc = new cognateBC(1000000);

		cbc.setObjectLocation(generateCoordinateWithinCircle());
		scheduleStoppableCell(cbc);

		int LR_Tminus20 = 0;
		int LR_T = 10000;

		// give the B cells more time to reach steady state
		for (int i = 0; i < 101; i++) {
			schedule.step(this);
			if (i == 10) {
				LR_Tminus20 = cbc.m_iL_r;
			}
			if (i == 50) {
				LR_T = cbc.m_iL_r;
			}
		}

		do {
			LR_Tminus20 = cbc.m_iL_r; // calculate LR at start of loop
			for (int i = 0; i < 50; i++) {
				schedule.step(this);
			}
			LR_T = cbc.m_iL_r; // record LR at end of loop
			
			
		//TODO we have set an arbritrary threshold so this needs refining	
		} while (Math.sqrt(Math.pow((LR_T - LR_Tminus20), 2)) > 1000); 
		

		System.out.println("B cells have stabilised: "
				+ this.schedule.getSteps());
		cbc.removeDeadCell(BC.bcEnvironment); // get rid of these cells as we
												// don't need them anymore

	}

	/*
	 * Scheduling a cell returns a stoppable object. we store the stoppable
	 * object as a variable within the BC class.
	 * 
	 * Then the BC can access its stopper variable and call the stop method.
	 * @param
	 */
	public void scheduleStoppableCell(BC lymphocyte) {

		//
		lymphocyte.setStopper(schedule.scheduleRepeating(lymphocyte));

	}

	/**
	 * Sets up a simulation run. (Re)initialises the environments, generates a
	 * stromal network and BCs randomly
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
		//BC.bcEnvironment = (Continuous3D) ReadObjects.restoreBC();
		
		BC.drawEnvironment = BC.bcEnvironment;

	
		particlemoles = new ParticleMoles(schedule,ParticleMoles.TYPE.CXCL13, 
				Settings.WIDTH, Settings.HEIGHT,Settings.DEPTH);
		
		//let diffusion stabilise before adding the cells
		for (int i = 0; i < 200; i++) {
			schedule.step(this);
		}
		
		
		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);
		
		
		//initialiseStroma(cgGrid); // initialise the stromal network
		initialiseFDC(cgGrid);
		
		
		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		
		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);
		
		
	}

	
	/*
	 * Starts sim from a predefined steady state
	 */
	public void startSS() {
		// start the simulation
		super.start();

		fdcEnvironment = (Continuous3D) ReadObjects.restoreFDC();		
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;
		//need to readd all of the stroma to the schedule yo!!don't forget the ordering though
		
		
		
		Bag FDCs = fdcEnvironment.getAllObjects();
		for(Object fdc : FDCs)
		{
			

			if(fdc instanceof FDC)
			{
			
				Double3D loc = fdcEnvironment.getObjectLocation(fdc);
				((DrawableCell3D) fdc).setObjectLocation(loc);
				schedule.scheduleRepeating((Steppable) fdc, 2, 1);
			}
			else if(fdc instanceof StromaEdge)
			{
				
			}
		}
		
		
		// Initialise the B cell grid
		BC.bcEnvironment = (Continuous3D) ReadObjects.restoreBC();
		BC.drawEnvironment = BC.bcEnvironment;


		Bag BCs = BC.bcEnvironment.getAllObjects();
		for(Object bc : BCs)
		{	
			if(bc instanceof BC)
			{	
				Double3D loc = BC.bcEnvironment.getObjectLocation(bc);
				((DrawableCell3D) bc).setObjectLocation(loc);			
				scheduleStoppableCell((BC) bc);
			}
			else if(bc instanceof cognateBC)
			{
				Double3D loc = BC.bcEnvironment.getObjectLocation(bc);
				((DrawableCell3D) bc).setObjectLocation(loc);
				scheduleStoppableCell((cognateBC) bc);
			}
		}

		
		//why isnt this working
		particlemoles = (ParticleMoles) ReadObjects.restoreCXCL13();
		ParticleMoles.ms_emTypeMap.put(ParticleMoles.TYPE.CXCL13, ParticleMoles.ms_emTypeMap.size());
		ParticleMoles.ms_pParticles[ParticleMoles.ms_emTypeMap.get(ParticleMoles.TYPE.CXCL13)] = particlemoles;
		schedule.scheduleRepeating(particlemoles, 3, 1);
	
	
		
		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid(Settings.WIDTH,
				Settings.HEIGHT, Settings.DEPTH, 1);
		schedule.scheduleRepeating(cgGrid, 3, 1);


		// BCs will need to update their collision profile each
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		
		
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
		
		//squared distance from test.x to center.x 
		double termOne = Math.pow((x - circleCentreX), 2); 
		
		//squared distance from test.y to center.y 
		double termTwo = Math.pow((y - circleCentreY), 2); 

		//test whether the point is in the circle - pythagoras
		if ((termOne + termTwo) < Math.pow(radius, 2)) 
		{
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

				if (i == 0) {
					bc.displayODEGraph = true;
				} // so we only have 1 BC updating the ODE graph

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
	 * @return a random Double3D from within the follicle
	 */
	public Double3D generateCoordinateWithinCircle() {

		int x, y, z;

		do {
			x = random.nextInt(Settings.WIDTH - 2) + 1;
			y = random.nextInt(Settings.HEIGHT - 2) + 1;
			z = random.nextInt(Settings.DEPTH - 2) + 1;

		} while (isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13) == false);

		return new Double3D(x, y, z);
	}

	

	
	
	/*
	 * Generate and initialise a stromal network
	 */
	private void initialiseStroma(CollisionGrid cgGrid) {

		// Generate some stroma
		ArrayList<FRCCell> frclCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
		/*
		FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);
        */
		
		FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (FRCCell frcCell : frclCellLocations) // why does this say FRC but
													// then it moves onto FDCs?
		{
			// Grapher.bcFRCEdgeNumberSeries[Math.min( frcCell.iEdges - 1, 11
			// )]++;
			FDC fdc = new FDC();

			// This will register the FDC with the environment/display
			// to account for the border which is one gridspace in width
			fdc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
					frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));

			// Schedule the secretion of chemokine, needs to be ordered so that
			// a chemokine
			// can't diffuse before it is secreted
			schedule.scheduleRepeating(fdc, 2, 1);

			// TODO BC-FDC interactions not yet defined so no point adding this yet
			// fdc.registerCollisions( cgGrid );
		}

		// Add the stroma edges to the display/CollisionGrid
		for (StromaEdge seEdge : sealEdges) {
			Double3D d3Point = seEdge.getPoint1();
			Double3D d3Point2 = seEdge.getPoint2();

			// Check if it's out of bounds - if not then add the info to the
			// graphs
			// Don't need this code if the graphs aren't working

			if (!(d3Point.x <= 0 || d3Point.x >= (Settings.WIDTH - 2)
					|| d3Point.y <= 0 || d3Point.y >= (Settings.HEIGHT - 2)
					|| d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2))
					&& !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2)
							|| d3Point2.y <= 0
							|| d3Point2.y >= (Settings.HEIGHT - 2)
							|| d3Point2.z <= 0 || d3Point2.z >= (Settings.DEPTH - 2))) {
				int iCat = (int) (5 * (seEdge.getPoint2()
						.subtract(seEdge.getPoint1()).length() - 1.2));
				// Grapher.bcFRCEdgeSizeSeries[Math.max( 0, Math.min( iCat, 19 )
				// )]++;
			}
			// Register with display and CG
			seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
					seEdge.z + 1));
			seEdge.registerCollisions(cgGrid);
		}

		// All the static cells are in, now reset the collision data
		cgGrid.step(null);
		
		//count the entire number of dendrites so we can get a percentage scanned measure
		totalNumberOfDendrites = sealEdges.size();
		
		
	}


	
	

	/*
	 * Generate and initialise a stromal network
	 */
	private void initialiseFDC(CollisionGrid cgGrid) {

		// Generate some stroma
		ArrayList<FRCStromaGenerator.FRCCell> frclCellLocations = new ArrayList<FRCStromaGenerator.FRCCell>();
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
		/*
		FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);
        */
		
		FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2,
				Settings.HEIGHT - 2, Settings.DEPTH - 2, Settings.FDC.COUNT,
				frclCellLocations, sealEdges);

		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for (FRCStromaGenerator.FRCCell frcCell : frclCellLocations) // why does this say FRC but
													// then it moves onto FDCs?
		{
			// Grapher.bcFRCEdgeNumberSeries[Math.min( frcCell.iEdges - 1, 11
			// )]++;
			FDC fdc = new FDC();

			// This will register the FDC with the environment/display
			// to account for the border which is one gridspace in width
			fdc.setObjectLocation(new Double3D(frcCell.d3Location.x + 1,
					frcCell.d3Location.y + 1, frcCell.d3Location.z + 1));
			
			schedule.scheduleRepeating((Steppable) fdc, 2, 1);

		
		}

		// Add the stroma edges to the display/CollisionGrid
		for (StromaEdge seEdge : sealEdges) {
			Double3D d3Point = seEdge.getPoint1();
			Double3D d3Point2 = seEdge.getPoint2();

			// Check if it's out of bounds - if not then add the info to the
			// graphs
			// Don't need this code if the graphs aren't working

			if (!(d3Point.x <= 0 || d3Point.x >= (Settings.WIDTH - 2)
					|| d3Point.y <= 0 || d3Point.y >= (Settings.HEIGHT - 2)
					|| d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2))
					&& !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2)
							|| d3Point2.y <= 0
							|| d3Point2.y >= (Settings.HEIGHT - 2)
							|| d3Point2.z <= 0 || d3Point2.z >= (Settings.DEPTH - 2))) {
				int iCat = (int) (5 * (seEdge.getPoint2()
						.subtract(seEdge.getPoint1()).length() - 1.2));
				// Grapher.bcFRCEdgeSizeSeries[Math.max( 0, Math.min( iCat, 19 )
				// )]++;
			}
			
		
			int branchesAdded = 0;
			
			// Register with display and CG
			seEdge.setObjectLocation(new Double3D(seEdge.x + 1, seEdge.y + 1,
					seEdge.z + 1));
			
			seEdge.registerCollisions(cgGrid);
			

			
			//add bracnhes to neighbouring cells
			Bag neighbouredges = fdcEnvironment.getNeighborsExactlyWithinDistance(seEdge.midpoint, 2);
			
			for(int j=0; j <neighbouredges.size() - 1;j++)
			{
				if(neighbouredges.get(j) instanceof StromaEdge)
				{
					
					if(branchesAdded < 1)
					{
						StromaEdge neighbouredge = (StromaEdge) neighbouredges.get(j);
						branch b = new branch(seEdge.midpoint,neighbouredge.midpoint);
						b.setObjectLocation(new Double3D(b.x + 1, b.y + 1,
							b.z + 1));	
						
						
						Bag branchedges = fdcEnvironment.getNeighborsExactlyWithinDistance(b.midpoint, 2);
						
						int subbranches = 0;
						
						for(int k=0; k < branchedges.size() - 1;k++)
						{
							
							if(subbranches < 1)
							{
								if(branchedges.get(k) instanceof FDC)
								{
									FDC fdc = (FDC) branchedges.get(k);
									branch b2 = new branch(b.midpoint,new Double3D(fdc.x, fdc.y, fdc.z));
									b2.setObjectLocation(new Double3D(b2.x + 1, b2.y + 1,
										b2.z + 1));	
									
									b2.registerCollisions(cgGrid);
								
								}
							
								subbranches +=1;
							}
						}		
						branchesAdded += 1;
					}
				}	
			}
		}

		
		//addBranchestoBranches(0.3);
		
		//count the entire number of dendrites so we can get a percentage scanned measure
		totalNumberOfDendrites = sealEdges.size();
		
		
	}
	
	

	/**
	 * Add branches to the stroma to get a more web-like morphology
	 * @param sealEdges
	 */
	public void addBranchestoBranches( double distance)
	{
		
		Bag Stroma = null;
		Stroma = fdcEnvironment.getAllObjects();
		for(int i=0; i <Stroma.size();i++)
		{
			if(Stroma.get(i) instanceof branch)
			{
				branch se = (branch) Stroma.get(i);
				
				//calculate midpoint and then use this 
				//to see what might be close
				Bag neighbouredges = null;
				neighbouredges = fdcEnvironment.getNeighborsExactlyWithinDistance(se.midpoint, distance);
				
				int branchesAdded = 0;
				
				for(int j=0; j <neighbouredges.size() - 1;j++)
				{
					if(branchesAdded < 2)
					{
					
						if(neighbouredges.get(j) instanceof branch && branchesAdded < 2)
						{
						
							branch neighbouredge = (branch) neighbouredges.get(j);
							branch b = new branch(se.midpoint,neighbouredge.midpoint);
							b.setObjectLocation(new Double3D(b.x + 1, b.y + 1,
								b.z + 1));
						
							branchesAdded +=1;
						
						}
					}
				}
			}
		}	
	}
	
	
	
}
