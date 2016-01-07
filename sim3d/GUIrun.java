package sim3d;

import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.engine.*;
import sim.display.*;
import sim.display3d.Display3D;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleColorMap;
import sim3d.util.IO;

import java.awt.Color;

import javax.swing.*;

import dataLogger.Grapher;

/**
 * Sets up and runs the simulation
 * 
 * Need to ensure that Java has access to enough memory resources
 * go to run configurations and pass in -Xmx3000m
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 */
public class GUIrun extends GUIState
{

	public static String paramFile;
	/**
	 * Returns the name of the simulation - a MASON thing
	 */
	public static String getName()
	{
		return "StromaSim";
	}
	
	/**
	 * Main entry into SimSim. Expects no args.
	 */
	public static void main( String[] args )
	{
		paramFile = args[0]; 
		new GUIrun().createController();
	}
	
	/**
	 * Frames to display the various graphs
	 */
	public JFrame				chartFrame;
	public JFrame				chartFrame2;
	public JFrame				chartFrame3;
	public JFrame				chartFrameAntigen;							
	/**
	 * The main display
	 */
	public JFrame				d3DisplayFrame;
								
	/**
	 * The chempkine display
	 */
	public JFrame				jfChemokine;
								
	/**
	 * The 3D display object
	 */
	public Display2D			display2D;
								
	/**
	 * The 3D display object
	 */
	public Display3D			display3D;
								
	/**
	 * Portrayal for BCs
	 */
	ContinuousPortrayal3D		bcPortrayal			= new ContinuousPortrayal3D();
													
	/**
	 * Portrayal for FDCs
	 */
	ContinuousPortrayal3D		fdcPortrayal		= new ContinuousPortrayal3D();
													
	/**
	 * a 2D portrayal that will show a plane of the particles
	 */
	FastValueGridPortrayal2D	particlePortrayal	= new FastValueGridPortrayal2D();
													
	/**
	 * Constructor - creates a Demo object
	 */
	public GUIrun()
	{
		super( new SimulationEnvironment( System.currentTimeMillis(),IO.openXMLFile(paramFile) ) );
	}
	
	/**
	 * Constructor
	 * @param state a previously saved state to load
	 */
	public GUIrun( SimState state )
	{
		super( state );
	}
	
	/**
	 * End of a simulation run
	 */
	public void finish()
	{
		super.finish();
	}
	
	/**
	 * Accessor for state
	 */
	public Object getSimulationInspectedObject()
	{
		return state;
	}
	
	/**
	 * Initialise the GUI for a simulation run
	 */
	public void init( Controller c )
	{
		super.init( c );
		
		// make the displayer
		display3D = new Display3D( 600, 600, this );
		display2D = new Display2D( 600, 600, this );
		
		// Move the camera to a sensible position
		display3D.translate( -Settings.WIDTH / 2.0, -Settings.WIDTH / 2.0, 0 );
		display3D.scale( 2.0 / Settings.WIDTH );
		
		// Setup the display frame
		d3DisplayFrame = display3D.createFrame();
		d3DisplayFrame.setTitle( "Demo3D" );
		// register the frame so it appears in the "Display" list
		c.registerFrame( d3DisplayFrame );
		d3DisplayFrame.setVisible( true );
		
		// Add the portrayals to the display
		display3D.attach( fdcPortrayal, "FDC" );
		display3D.attach( bcPortrayal, "BC" );
		
		jfChemokine = display2D.createFrame();
		jfChemokine.setTitle( "Chemokine" );
		// register the frame so it appears in the "Display" list
		c.registerFrame( jfChemokine );
		jfChemokine.setVisible( true );
		jfChemokine.setLocation( 700, 0 );
		jfChemokine.setBackground( Color.black );
		particlePortrayal.setMap( new ParticleColorMap() );
		display2D.attach( particlePortrayal, "CXCL13" );
		
		// Load the graphing functionality
		Grapher.init();
		Grapher.schedule = state.schedule;
		
		// Setup the graph displays...
		
		// The ODE line graph
		chartFrame = Grapher.chart.createFrame();
		chartFrame.setVisible( true );
		chartFrame.pack();
		chartFrame.setLocation( 0, 700 );
		c.registerFrame( chartFrame );
		
		// The ODE line graph
		chartFrameAntigen = Grapher.chartAntigen.createFrame();
		chartFrameAntigen.setVisible( true );
		chartFrameAntigen.pack();
		chartFrameAntigen.setLocation( 0, 700 );
		c.registerFrame( chartFrameAntigen );
				
		
		/**
		// FDC Edge size bar chart
		chartFrame2 = Grapher.bcFRCEdgeSizeChart.createFrame();
		chartFrame2.setVisible( true );
		chartFrame2.pack();
		chartFrame2.setLocation( 0, 700 );
		c.registerFrame( chartFrame2 );
		
		// FDC edge number bar chart
		chartFrame3 = Grapher.bcFRCEdgeNumberChart.createFrame();
		chartFrame3.setVisible( true );
		chartFrame3.pack();
		chartFrame3.setLocation( 0, 700 );
		c.registerFrame( chartFrame3 );
		*/
	}
	
	/**
	 * Load a previously saved simulation state
	 */
	public void load( SimState state )
	{
		super.load( state );
		setupPortrayals();
	}
	
	/**
	 * Destructor, essentially
	 */
	public void quit()
	{
		super.quit();
		
		if ( d3DisplayFrame != null )
			d3DisplayFrame.dispose();
		d3DisplayFrame = null;
		display3D = null;
		
		Grapher.finish();
		if ( chartFrame != null )
			chartFrame.dispose();
		chartFrame = null;
		
		if ( chartFrameAntigen != null )
			chartFrameAntigen.dispose();
		chartFrameAntigen = null;
		
	}
	
	/**
	 * Initialise the portrayals
	 */
	public void setupPortrayals()
	{
		// tell the portrayals what to portray and how to portray them
		fdcPortrayal.setField( FDC.drawEnvironment );
		bcPortrayal.setField( BC.drawEnvironment );
		particlePortrayal.setField( Particle.getInstance( Particle.TYPE.CXCL13 ).m_ig2Display );
		
		// p3dParticles.setField( Particle.getInstance( Particle.TYPE.CXCL13 )
		// );
		// p3dParticles.setMap(new ParticleColorMap());
		// p3dParticles.setMap( new sim.util.gui.SimpleColorMap( 0.0, 3000, new
		// Color( 0, 0, 0, 0 ), Color.WHITE ) );
		
		display3D.createSceneGraph();
		display3D.reset();
	}
	
	/**
	 * Start a simulation run
	 */
	public void start()
	{
		super.start();
		setupPortrayals();
		Grapher.start();
	}
	
}
