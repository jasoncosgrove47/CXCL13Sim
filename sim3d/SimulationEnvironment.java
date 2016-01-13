package sim3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dataLogger.Controller;
import ec.util.MersenneTwisterFast;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.Vector3DHelper;
import sim3d.util.FRCStromaGenerator.FRCCell;

/**
 * 
 *  This class sets up and runs the simulation absent of 
 *  any GUI related function as a MASON design pattern, 
 *  in line with the MASON (Model/View/Controller).
 * 
 *  Need to ensure that Java has access to enough memory resources
 *  go to run configurations and pass in -Xmx3000m
 * 
 *  @author Jason Cosgrove  - {@link jc1571@york.ac.uk}
 *  @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class SimulationEnvironment extends SimState
{
	
	private static final long serialVersionUID = 1;
	
	
	/*
	 * 3D grid where B cells and cBs exist
	 */
	public Continuous3D	bcEnvironment; 
	
	/*
	 * 3D grid for Stroma
	 */
	public Continuous3D	fdcEnvironment; 
	
	/*
	 * Parameter file: XML format
	 */
	public static Document parameters;	
	
	/*
	 * Controller responsible for recording 
	 * data from the simulation
	 */
	private static Controller controller;
	

	/**
	 * ENUM for the cell types
	 */
	public enum CELLTYPE
	{
		B, cB, T
	}
	public TYPE celltype;
	
	
	/**
	 * Constructor
	 * @param seed  Used by MASON for the random seed
	 */
	public SimulationEnvironment( long seed, Document params )
	{
		super( seed );
		parameters = params;
		setupSimulationParameters();
		Settings.RNG = random; // We set the MASON random object to the static Options class so we can access it everywhere
	}
	
	
	/**
	 * Load parameters from an external xml 
	 * file to the static Options class
	 */
	 public void setupSimulationParameters()
	 {
		 Settings.loadParameters(parameters);
		 Settings.BC.loadParameters(parameters);
		 Settings.FDC.loadParameters(parameters);
		 Settings.BC.ODE.loadParameters(parameters);
		 Settings.CXCL13.loadParameters(parameters);
	 }
	
	 
	/**
	 * Adds a slider for the display level in the MASON console
	 */
	public Object domDisplayLevel()
	{
		return new sim.util.Interval( 1, Settings.DEPTH );
	}
	
	
	/**
	 * Destroy resources after use
	 */
	public void finish()
	{
		Particle.reset();
	}
	
	
	/**
	 * Accessor for the current display level - a z-index to use for displaying
	 * the diffusion
	 */
	public int getDisplayLevel()
	{
		// Add 1 so the scale goes from 1 to 10 and not 0 to 9!
		return Particle.getDisplayLevel() + 1;
	}
	
	
	/**
	 * Setter for the current display level
	 */
	public void setDisplayLevel( int m_iDisplayLevel )
	{
		Particle.setDisplayLevel( m_iDisplayLevel - 1 );
		Particle.getInstance( Particle.TYPE.CXCL13 ).updateDisplay();
	}
	
	
	/**
	 * Sets up a simulation run. (Re)initialises the environments, generates a
	 * stromal network and BCs randomly
	 */
	public void start()
	{
		//start the simulation
		super.start();
	
		//initialise the controller and add to the schedule, 
		//TODO: should be scheduled last
		controller = new Controller();
		schedule.scheduleRepeating(getController());
		
		//Initialise the stromal grid
		fdcEnvironment = new Continuous3D( Settings.FDC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH );
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;
		
		//Initialise the B cell grid
		bcEnvironment = new Continuous3D( Settings.BC.DISCRETISATION, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH );
		BC.drawEnvironment = bcEnvironment;
		
		// Initialise the CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid( Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH, 1 );
		schedule.scheduleRepeating( cgGrid, 3, 1 );
		initialiseStroma(cgGrid); //initialise the stromal network

		// BCs will need to update their collision profile each 
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;

		seedCells(CELLTYPE.B);
		seedCells(CELLTYPE.cB);
		//seedCognateCells(Settings.BC.COGNATECOUNT);
		//new Particle( schedule, Particle.TYPE.CXCL13, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH );// add particles
		new ParticleMoles( schedule, ParticleMoles.TYPE.CXCL13, Settings.WIDTH, Settings.HEIGHT, Settings.DEPTH );
	}
	
	
	/**
	 * Tests whether co-ordinates x,y are not in the circle centered at circleCentreX, circleCentreY with a specified radius
	 * @return boolean determining whether inside (false) or outside (true) the circle
	 */
	public boolean isWithinCircle(int x,int y,int circleCentreX, int circleCentreY, int radius)
		{
		double termOne = Math.pow((x - circleCentreX),2);	//calculate the distance from test.x to centre.x and square it
		double termTwo = Math.pow((y - circleCentreY),2); 	//calculate the distance from test.y to centre.y and square it
		
		if( (termOne + termTwo) < Math.pow(radius, 2)) 		//test whether the point is in the circle using pythagoras theorem
		{
			return true;
		}	
		else return false;
	}
	
	
	/**
	 * Seeds B cells
	 */
   public void seedCells(CELLTYPE celltype)
   {
	   
	   int count = 0; //the number of cells to seed
	   
	   //set the number of cells to seed
	   if(celltype==CELLTYPE.B){count = Settings.BC.COUNT;}
	   else if(celltype==CELLTYPE.cB){count = Settings.BC.COGNATECOUNT;}
			
	   
	   //seed the cells
	   for ( int i = 0; i < count; i++ )
	   {	
		   switch(celltype)
			{
				case B: 	 //if it's a B cell
					BC bc = new BC();
					bc.setObjectLocation( generateCoordinateWithinFollicle());
					schedule.scheduleRepeating( bc, 0, 1 );
					if ( i == 0 ){bc.displayODEGraph = true;} // so we only have 1 BC updating the ODE graph
					
					break;
					
				case cB: // if it's a cognate B cell
					 cognateBC cbc = new cognateBC(i);
					 cbc.setObjectLocation( generateCoordinateWithinFollicle());
					 schedule.scheduleRepeating( cbc, 0, 1 );
					 if ( i == 0 ){cbc.displayAntigenGraph = true;}
					
					break;
					
				default:
					break;
			}
		}
	}
		
  
	
   
   
   /**
    * Generates a random coordinate within the follicle
    * could be optimised
    * @return a random Double3D from within the follicle
    */
   public Double3D generateCoordinateWithinFollicle(){
	   
	   int x,y,z;
	   
	   do
	   {
		   x = random.nextInt( Settings.WIDTH - 2) + 1 ;
		   y = random.nextInt( Settings.HEIGHT - 2 ) + 1;
		   z = random.nextInt( Settings.DEPTH - 2 ) + 1;
		   
	   } while (isWithinCircle(x,y ,( Settings.WIDTH /2 ) + 1, ( Settings.HEIGHT / 2 ) + 1, 20) == false);
	   
	   return new Double3D(x,y,z);
   }

	
	/*
	 * Generate and initialise a stromal network
	 */
	private void initialiseStroma(CollisionGrid cgGrid){
		
		// Generate some stroma
		ArrayList<FRCCell> frclCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(Settings.WIDTH - 2, Settings.HEIGHT - 2, Settings.DEPTH - 2,
				Settings.FDC.COUNT, frclCellLocations, sealEdges );

		// Create the FDC objects, display them, schedule them, and then put them on the collision grid
		for ( FRCCell frcCell : frclCellLocations ) //why does this say FRC but then it moves onto FDCs?
		{
			//Grapher.bcFRCEdgeNumberSeries[Math.min( frcCell.iEdges - 1, 11 )]++;
			FDC fdc = new FDC();
			
			// This will register the FDC with the environment/display
			// to account for the border which is one gridspace in width
			fdc.setObjectLocation(
				new Double3D( frcCell.d3Location.x + 1, frcCell.d3Location.y + 1, frcCell.d3Location.z + 1 ) );
							
			// Schedule the secretion of chemokine, needs to be ordered so that a chemokine 
			// can't diffuse before it is secreted
			schedule.scheduleRepeating( fdc, 2, 1 );
					
			// TODO BC-FDC interactions not yet defined so no point adding this yet
			// fdc.registerCollisions( cgGrid );
		}
				
		// Add the stroma edges to the display/CollisionGrid
		for ( StromaEdge seEdge : sealEdges )
		{
			Double3D d3Point = seEdge.getPoint1();
			Double3D d3Point2 = seEdge.getPoint2();
					
			// Check if it's out of bounds - if not then add the info to the graphs
			// Don't need this code if the graphs aren't working
				
			if ( !(d3Point.x <= 0 || d3Point.x >= (Settings.WIDTH - 2) || d3Point.y <= 0
					|| d3Point.y >= (Settings.HEIGHT - 2) || d3Point.z <= 0 || d3Point.z >= (Settings.DEPTH - 2))
					&& !(d3Point2.x <= 0 || d3Point2.x >= (Settings.WIDTH - 2) || d3Point2.y <= 0
							|| d3Point2.y >= (Settings.HEIGHT - 2) || d3Point2.z <= 0
							|| d3Point2.z >= (Settings.DEPTH - 2)) )
			{
					int iCat = (int) (5 * (seEdge.getPoint2().subtract( seEdge.getPoint1() ).length() - 1.2));
					//Grapher.bcFRCEdgeSizeSeries[Math.max( 0, Math.min( iCat, 19 ) )]++;
			}
					// Register with display and CG
					seEdge.setObjectLocation( new Double3D( seEdge.x + 1, seEdge.y + 1, seEdge.z + 1 ) );
					seEdge.registerCollisions( cgGrid );
		}

				// All the static cells are in, now reset the collision data
				cgGrid.step( null );
	}

	//getters and setters
	public static Controller getController() {return controller;}
	public static void       setController(Controller controller) 
	{
		SimulationEnvironment.controller = controller;
	}
	
}