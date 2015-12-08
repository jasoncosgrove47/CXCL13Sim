package sim3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.Vector3DHelper;
import sim3d.util.FRCStromaGenerator.FRCCell;

/**
 * Sets up and runs the simulation
 * 
 * @author Jason Cosgrove  - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class Demo extends SimState
{
	
	private static final long serialVersionUID = 1;
	
	/**
	 * main method
	 */
	public static void main( String[] args )
	{
		int seed= (int) (System.currentTimeMillis());					// set the seed for the simulation, be careful for when running on cluster																	
		Demo simulation = new Demo(seed);								// instantiate the simulation

		long steps = 0;
		simulation.start();
		
		System.out.println("GSim v1.0 - Console Version (No Visualisation)");
		System.out.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");
		
		do
		{
			steps = simulation.schedule.getSteps();		
			System.out.println("Steps: " + steps);
			if (!simulation.schedule.step(simulation))
			break;
			
		}while(steps < 500);	
		
		simulation.finish();												// finish the simulation
		System.out.println("\nSimulation completed successfully!\n\n");
		System.exit(0);														// exit the simulation
		
		//doLoop( Demo.class, args );//change this to run for a fixed amount of timesteps
		//System.exit( 0 );
	}
	
	
	public Continuous3D	bcEnvironment;  // 3D grid where B cells reside
	public Continuous3D	fdcEnvironment; // contains stroma and their edges
			
	
	/**
	 * Constructor
	 * 
	 * @param seed  Used by MASON for the random seed
	 */
	public Demo( long seed )
	{
		super( seed );
		
		// We set the MASON random object to the static Options class so we can
		// access it everywhere
		// Also allows us to easily change it, and test easier!
		Options.RNG = random;
	}
	
	/**
	 * Adds a slider for the display level in the MASON console
	 */
	public Object domDisplayLevel()
	{
		return new sim.util.Interval( 1, Options.DEPTH );
	}
	
	/**
	 * Destroy resources after use.
	 */
	public void finish()
	{
		Grapher.bcFRCEdgeNumberSeries = new double[Grapher.bcFRCEdgeNumberSeries.length];
		Grapher.bcFRCEdgeSizeSeries = new double[Grapher.bcFRCEdgeSizeSeries.length];
		Particle.reset();
	}
	
	/**
	 * Accessor for the current display level - a z-index to use for displaying
	 * the diffusion
	 * 
	 * @return
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
		super.start();
		Grapher.init();
		// Initialise the environments, tell the various classes 
		// what their draw environment is
		fdcEnvironment = new Continuous3D( Options.FDC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;
		
		bcEnvironment = new Continuous3D( Options.BC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
		BC.drawEnvironment = bcEnvironment;
		
		// Initialise a CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid( Options.WIDTH, Options.HEIGHT, Options.DEPTH, 1 );
		schedule.scheduleRepeating( cgGrid, 3, 1 );
		
		initialiseStroma(cgGrid); //initialise the stromal network

		// BCs will need to update their collision profile each 
		// step so tell them what collision grid to use
		BC.m_cgGrid = cgGrid;
		addLymphocytes(); //add lymphocytes to the grid
		new Particle( schedule, Particle.TYPE.CXCL13, Options.WIDTH, Options.HEIGHT, Options.DEPTH );		// add particles
	}
	
	
	/**
	 * Adds Lymphocytes to their respective gridspaces 
	 */
	private void addLymphocytes(){
		for ( int x = 0; x < Options.BC.COUNT; x++ )
		{
			// Randomly locate the cells
			Double3D loc = new Double3D( random.nextInt( Options.WIDTH - 2 ) + 1,
					random.nextInt( Options.HEIGHT - 2 ) + 1, random.nextInt( Options.DEPTH - 2 ) + 1 );

			BC bc = new BC();
			
			// Register with display
			bc.setObjectLocation( loc );
			
			schedule.scheduleRepeating( bc, 0, 1 );
			
			// so we only have 1 BC updating the ODE graph
			if ( x == 0 )
			{
				bc.displayGraph = true;
			}
		}
		
	}
	
	/*
	 * Generate and initialise a stromal network
	 */
	private void initialiseStroma(CollisionGrid cgGrid){
		
		// Generate some stroma
				ArrayList<FRCCell> frclCellLocations = new ArrayList<FRCCell>();
				ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
				FRCStromaGenerator.generateStroma3D( Options.WIDTH - 2, Options.HEIGHT - 2, Options.DEPTH - 2,
						Options.FDC.COUNT, frclCellLocations, sealEdges );

				// Create the FDC objects, display them, schedule them, and then put
				// them on the collision grid
				for ( FRCCell frcCell : frclCellLocations ) //why does this say FRC but then it moves onto FDCs?
				{
					Grapher.bcFRCEdgeNumberSeries[Math.min( frcCell.iEdges - 1, 11 )]++;
					FDC fdc = new FDC();
					
					// This will register the FDC with the environment/display
					// to account for the border which is one gridspace in width
					fdc.setObjectLocation(
							new Double3D( frcCell.d3Location.x + 1, frcCell.d3Location.y + 1, frcCell.d3Location.z + 1 ) );
							
					// Schedule the secretion of chemokine, needs to be ordered so that a chemokine 
					// can't diffuse before it is secreted
					schedule.scheduleRepeating( fdc, 2, 1 );
					
					// TODO BC-FDC interactions not yet defined so no point adding this
					// yet
					// fdc.registerCollisions( cgGrid );
				}
				
				// Add the stroma edges to the display/CollisionGrid
				for ( StromaEdge seEdge : sealEdges )
				{
					Double3D d3Point = seEdge.getPoint1();
					Double3D d3Point2 = seEdge.getPoint2();//TODO why does this call getPoint1 again surely its point 2
					
					// Check if it's out of bounds - if not then add the info to the
					// graphs
					// Don't need this code if the graphs aren't working
				
					if ( !(d3Point.x <= 0 || d3Point.x >= (Options.WIDTH - 2) || d3Point.y <= 0
							|| d3Point.y >= (Options.HEIGHT - 2) || d3Point.z <= 0 || d3Point.z >= (Options.DEPTH - 2))
							&& !(d3Point2.x <= 0 || d3Point2.x >= (Options.WIDTH - 2) || d3Point2.y <= 0
									|| d3Point2.y >= (Options.HEIGHT - 2) || d3Point2.z <= 0
									|| d3Point2.z >= (Options.DEPTH - 2)) )
					{
						int iCat = (int) (5 * (seEdge.getPoint2().subtract( seEdge.getPoint1() ).length() - 1.2));
						
						Grapher.bcFRCEdgeSizeSeries[Math.max( 0, Math.min( iCat, 19 ) )]++;
					}
					
					// Register with display and CG
					seEdge.setObjectLocation( new Double3D( seEdge.x + 1, seEdge.y + 1, seEdge.z + 1 ) );
					seEdge.registerCollisions( cgGrid );
				}

				
				// All the static cells are in, now reset the collision data
				cgGrid.step( null );
				
				Grapher.bcFRCEdgeNumberChart.updateChartWithin( 11312, 1000 );
				Grapher.bcFRCEdgeSizeChart.updateChartWithin( 11313, 1000 );
		
	}
	
}
