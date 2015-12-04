package sim3d;

import java.util.ArrayList;

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
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class Demo extends SimState
{
	private static final long serialVersionUID = 1;
	
	/**
	 * main function renamed so I don't accidentally run this one instead of the
	 * DemoUI
	 */
	public static void main2( String[] args )
	{
		doLoop( Demo.class, args );
		System.exit( 0 );
	}
	
	/**
	 * Contains the BCs
	 */
	public Continuous3D	bcEnvironment;
						
	/**
	 * Contains the FDCs and their edges
	 */
	public Continuous3D	fdcEnvironment;
						
	/**
	 * Constructor
	 * 
	 * @param seed
	 *            Used by MASON for the random seed
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
		//Grapher.bcFRCEdgeNumberSeries = new double[Grapher.bcFRCEdgeNumberSeries.length];
		//Grapher.bcFRCEdgeSizeSeries = new double[Grapher.bcFRCEdgeSizeSeries.length];
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
		
		// Initialise the environments, tell the various classes what their draw
		// environment is!
		fdcEnvironment = new Continuous3D( Options.FDC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
		FDC.drawEnvironment = fdcEnvironment;
		StromaEdge.drawEnvironment = fdcEnvironment;
		
		bcEnvironment = new Continuous3D( Options.BC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
		BC.drawEnvironment = bcEnvironment;
		
		// Initialise a CollisionGrid
		CollisionGrid cgGrid = new CollisionGrid( Options.WIDTH, Options.HEIGHT, Options.DEPTH, 1 );
		schedule.scheduleRepeating( cgGrid, 3, 1 );
		
		// Generate some stroma
		ArrayList<FRCCell> frclCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D( Options.WIDTH - 2, Options.HEIGHT - 2, Options.DEPTH - 2,
				Options.FDC.COUNT, frclCellLocations, sealEdges );

		
		// Create the FDC objects, display them, schedule them, and then put
		// them on the collision grid
		for ( FRCCell frcCell : frclCellLocations )
		{
			// keep track of the data for the graphs
			//Grapher.bcFRCEdgeNumberSeries[Math.min( frcCell.iEdges - 1, 11 )]++;
			
			FDC fdc = new FDC();
			
			// This will register the FDC with the environment/display
			fdc.setObjectLocation(
					new Double3D( frcCell.d3Location.x + 1, frcCell.d3Location.y + 1, frcCell.d3Location.z + 1 ) );
					
			// Schedule the secretion of chemokine
			schedule.scheduleRepeating( fdc, 2, 1 );
			
			// TODO BC-FDC interactions not yet defined so no point adding this
			// yet
			// fdc.registerCollisions( cgGrid );
		}
		
		// Add the stroma edges to the display/CollisionGrid
		for ( StromaEdge seEdge : sealEdges )
		{
			Double3D d3Point = seEdge.getPoint1();
			Double3D d3Point2 = seEdge.getPoint1();
			
			// Check if it's out of bounds - if not then add the info to the
			// graphs
			if ( !(d3Point.x <= 0 || d3Point.x >= (Options.WIDTH - 2) || d3Point.y <= 0
					|| d3Point.y >= (Options.HEIGHT - 2) || d3Point.z <= 0 || d3Point.z >= (Options.DEPTH - 2))
					&& !(d3Point2.x <= 0 || d3Point2.x >= (Options.WIDTH - 2) || d3Point2.y <= 0
							|| d3Point2.y >= (Options.HEIGHT - 2) || d3Point2.z <= 0
							|| d3Point2.z >= (Options.DEPTH - 2)) )
			{
				int iCat = (int) (5 * (seEdge.getPoint2().subtract( seEdge.getPoint1() ).length() - 1.2));
				
				//Grapher.bcFRCEdgeSizeSeries[Math.max( 0, Math.min( iCat, 19 ) )]++;
			}
			
			// Register with display and CG
			seEdge.setObjectLocation( new Double3D( seEdge.x + 1, seEdge.y + 1, seEdge.z + 1 ) );
			seEdge.registerCollisions( cgGrid );
		}
		
		//Grapher.bcFRCEdgeNumberChart.updateChartWithin( 11312, 1000 );
		//Grapher.bcFRCEdgeSizeChart.updateChartWithin( 11313, 1000 );
		
		// All the static cells are in, now reset the collision data
		cgGrid.step( null );
		
		// BCs will need to update their collision profile each step so tell
		// them what CG to use
		BC.m_cgGrid = cgGrid;
		
		// Generate the BCs
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
		
		// add particles
		new Particle( schedule, Particle.TYPE.CXCL13, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
	}
}
