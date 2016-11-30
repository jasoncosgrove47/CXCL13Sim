package sim3d;

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

	
	private static int follicleRadius = 13;
	
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


	//edges with all overlaps removed
	ArrayList<StromaEdge> m_edges;
	
	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D fdcEnvironment;

	/*
	 * 3D grid for Stroma
	 */
	//public Continuous3D frcEnvironment;

	/**
	 * Instance of the CXCL13 class
	 */
	public static Chemokine CXCL13;

	/**
	 * Instance of the CCL19 class
	 */
	public static Chemokine CCL19;

	/**
	 * Instance of the EBI2L class
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
		Settings.TC.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
		Settings.FRC.loadParameters(parameters);
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
		initialiseStromalCell(cgGrid, Stroma.TYPE.bRC);

		seedSCS(cgGrid);
		

		shapeNetwork();
			
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
				flec.setObjectLocation(new Double3D(x, Settings.HEIGHT - Settings.FRC.SCSDEPTH, z));

				flec.registerCollisions(cgGrid);
				clec.registerCollisions(cgGrid);
				
				double random = Settings.RNG.nextDouble();
				if(random > 0.1){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, Settings.HEIGHT - (Settings.FRC.SCSDEPTH + 0.3), z));
					mrc.registerCollisions(cgGrid);
					scheduleStoppableCell(mrc);
				}
				
				
				if(random > 0.1){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, Settings.HEIGHT - (Settings.FRC.SCSDEPTH + 0.8), z));
					mrc.registerCollisions(cgGrid);
					scheduleStoppableCell(mrc);
				}

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
	boolean isWithinCircle(int x, int y, int circleCentreX,
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
				bc.setObjectLocation(generateCoordinateWithinCircle(follicleRadius));
				scheduleStoppableCell(bc);
				// so we only have 1 BC updating the ODE graph
				if (i == 0) {
					bc.displayODEGraph = true;
				}
				break;

			case cB:
				cognateBC cbc = new cognateBC(i);
				cbc.setObjectLocation(generateCoordinateWithinCircle(follicleRadius));
				scheduleStoppableCell(cbc);
				break;

			case T:
				TC tc = new TC();
				tc.setObjectLocation(generateCoordinateOutsideCircle(follicleRadius));

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
	 * Generate the stromal network, need to rename this method as its confusing
	 * @param cgGrid
	 * @param celltype
	 */
	private void initialiseStromalCell(CollisionGrid cgGrid,
			Stroma.TYPE celltype) {

		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> stromalCellLocations = 
				new ArrayList<StromaGenerator.StromalCell>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

		 if(celltype == Stroma.TYPE.bRC) {

			StromaGenerator.generateFRC3D(Settings.WIDTH - 2,
					Settings.HEIGHT - (Settings.FRC.SCSDEPTH+1), Settings.DEPTH - 2,
					Settings.FRC.COUNT, stromalCellLocations, sealEdges);
			
			
			
			m_edges = checkOverlaps(sealEdges);
			
			//set sealEdges to null so we can't use it again.
			sealEdges = null;
			
			//need to generate dendrites before nodes as we get rid of overlaps
			// having the nodes on the graph makes that a mess
			generateDendrites(cgGrid, m_edges, StromaEdge.TYPE.RC_edge);
			
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
		
		//can use a stromaMap here if we need to
		
		for(int x = 10; x < 32; x ++ ){
			for(int y = 10; y < 39; y ++ ){
				for(int z = 2;z<Settings.DEPTH-1;z++){
			
		
				if(isWithinCircle(x, y, (Settings.WIDTH / 2) + 1,
							(Settings.HEIGHT / 2)+2 , 12)){
				double random = Settings.RNG.nextDouble();
				if(random > 0.8){
					
					
					
					
					//if its within the follicle then stochastically generate an FDC node
					Stroma stromalcell = new Stroma(Stroma.TYPE.FDC);
					stromalcell.setObjectLocation(new Double3D(x,y, z));
					
				
					
					scheduleStoppableCell(stromalcell);
					
					//dont need to worry about the plus one stuff for the FDCs
					Double3D loc = new Double3D(x,y,z);
					Bag neighbours = new Bag(); 
							
					stromalcell.getDrawEnvironment().
					getNeighborsExactlyWithinDistance(loc, 2,false,true,false,neighbours);
		
					//System.out.println(neighbours.size());
				
					for(int i = 0; i < neighbours.size();i++){
						
						
						if(neighbours.get(i) instanceof Stroma){
							
							
							Stroma neighbour = (Stroma) neighbours.get(i);
						
							if(neighbour.getStromatype() == Stroma.TYPE.FDC){
								
								

								//dont want to include an edge to yourself
									if(!neighbour.equals(stromalcell)){
									
									Double3D nloc = new Double3D(neighbour.x, neighbour.y, neighbour.z);
						
									StromaEdge se = new StromaEdge(loc,nloc,StromaEdge.TYPE.RC_edge);
						
									se.setObjectLocation(new Double3D(se.x,
										se.y , se.z));
								
									

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
	 * Prune the network
	 */
	private void shapeNetwork(){

		//Bag fdcs = Stroma.drawEnvironment.getAllObjects();
		
		Bag fdcs = fdcEnvironment.getAllObjects();
		
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
					
					if(isWithinCircle((int)fdc.x, (int)fdc.y, (Settings.WIDTH / 2) + 1,
							(Settings.HEIGHT / 2)+2 , 12)){
						
						deleteRC(fdc);
						
					}
				}
				
			}	
		}
	}

		

	
	private void seedRC(StromaGenerator.StromalCell sc,CollisionGrid cgGrid,ArrayList<StromaEdge> edges){
		
				
		Stroma frc = new Stroma(Stroma.TYPE.bRC);
		
		Double3D loc = new Double3D(sc.d3Location.x+1,
				sc.d3Location.y +1, sc.d3Location.z +1);
		// This will register the FRC with the environment/display
		// to account for the border which is one gridspace in width
		frc.setObjectLocation(loc);


		scheduleStoppableCell(frc);
		

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
			if(distance < 0.1 || distance2 < 0.1){
				frc.m_dendrites.add(seEdge);	
			}
			
			
			
	
		}
	
	}
		
	

	/**
	 * Remove an RC from the grid and the schedule
	 * @param frc
	 */
	private void deleteRC(Stroma frc){
		
		frc.getDrawEnvironment().remove(frc);
		frc.stop();
		for(int i = 0; i < frc.m_dendrites.size(); i ++){			
			frc.m_dendrites.get(i).getDrawEnvironment()
				.remove(frc.m_dendrites.get(i));

		}

	}



		


	
	/**
	 * CHECK THE STROMAEDGE ARRAY TO MAKE SURE THERE ARE NO DUPLICATES
	 * 
	 * need to deal with this properly but for now this is fine
	 * @param sealEdges
	 * @return
	 */
	private ArrayList<StromaEdge> checkOverlaps(ArrayList<StromaEdge> sealEdges){

		ArrayList<StromaEdge> updatedEdges = sealEdges;
		
		ArrayList<StromaEdge> edgesToRemove = new ArrayList<StromaEdge>();

		for (int i = 0; i < sealEdges.size(); i++) {
			  for (int j = i+1; j < sealEdges.size(); j++) {
				  
				  Double3D i_p1 = new Double3D(sealEdges.get(i).x +1,
						  sealEdges.get(i).y+1, sealEdges.get(i).z +1);
				  Double3D i_p2 = new Double3D(sealEdges.get(i).getPoint2().x +1,
						  sealEdges.get(i).getPoint2().y+1, sealEdges.get(i).getPoint2().z +1);
					
				  Double3D j_p1 = new Double3D(sealEdges.get(j).x +1,
						  sealEdges.get(j).y+1, sealEdges.get(j).z +1);
				  Double3D j_p2 = new Double3D(sealEdges.get(j).getPoint2().x +1,
						  sealEdges.get(j).getPoint2().y+1, sealEdges.get(j).getPoint2().z +1);
				  
				
				
				//sometimes get edges that are very close so need to account for this
				double d1 = calcDistance(i_p1, j_p1);
				double d2 = calcDistance(i_p1, j_p2);
				double d3 = calcDistance(i_p2, j_p1);
				double d4 = calcDistance(i_p2, j_p2);
				
				
				
				if(d1 < 0.15 && d4 < 0.15){
					edgesToRemove.add(sealEdges.get(i));
				}
				
				else if(d2 < 0.15 && d3 < 0.15){
					edgesToRemove.add(sealEdges.get(i));
				}
				
				
			  }
			}

		//now remove all of the overlapping edges in the removal list
		for(int x = 0; x < edgesToRemove.size(); x ++){
			
			//System.out.println(edgesToRemove.get(x));
			updatedEdges.remove(edgesToRemove.get(x));
		}
		
		return updatedEdges;
	}
	
	
	
	protected static double calcDistance(Double3D i3Point1, Double3D i3Point2) {
		return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2)
				+ Math.pow(i3Point1.y - i3Point2.y, 2)
				+ Math.pow(i3Point1.z - i3Point2.z, 2)));
	}

	// TODO this needs refactoring but going to change the stroma stuff soon anyway
	// anyway
	private void generateDendrites(CollisionGrid cgGrid,
			ArrayList<StromaEdge> edges, StromaEdge.TYPE type) {

		
		if (type == StromaEdge.TYPE.RC_edge) {
			for (StromaEdge seEdge : edges) {
				

					seEdge.setM_col(Settings.FRC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x +1,
							seEdge.y +1, seEdge.z +1));
						
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

}
