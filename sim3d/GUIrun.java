package sim3d;

import sim.portrayal.grid.FastHexaValueGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.util.gui.SimpleColorMap;
import sim.engine.*;
import sim.display.*;
import sim.display3d.Display3D;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.diffusion.ParticleColorMap;
import sim3d.diffusion.ParticleMoles;
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
	 * Main entry into the sim
	 */
	public static void main( String[] args )
	{
		paramFile = args[0]; 
		new GUIrun().createController();
	}
	
	/**
	 * Frames to display the various graphs
	 */
	public JFrame	chartFrame;
	public JFrame	chartFrame2;
	public JFrame	chartFrame3;
	public JFrame	chartFrameAntigen;	
	
	
	
	public java.awt.Color blue0		= new Color(30,40,190,180);
 	public java.awt.Color blueLow 	= new Color(30,40,190,0);
 	public java.awt.Color blue1 	= new Color(30,40,210,200);
 	public java.awt.Color blue2 	= new Color(200,40,230,220);
 	public java.awt.Color blue3 	= new Color(30,200,255,255);
	
 	public SimpleColorMap CXCL13ColorMap  = new SimpleColorMap(0.01 * Math.pow(10, -9), 3 * Math.pow(10, -9), blueLow, blue3);
	
	/**
	 * The main display
	 */
	public JFrame	d3DisplayFrame;
								
	/**
	 * The chempkine display
	 */
	public JFrame	jfChemokine;
								
	/**
	 * The 3D display object
	 */
	public Display2D	display2D;
								
	/**
	 * The 3D display object
	 */
	public Display3D	display3D;
								
	/**
	 * Portrayal for BCs
	 */
	ContinuousPortrayal3D bcPortrayal = new ContinuousPortrayal3D();
													
	/**
	 * Portrayal for FDCs
	 */
	ContinuousPortrayal3D fdcPortrayal = new ContinuousPortrayal3D();
													
	/**
	 * a 2D portrayal that will show a plane of the particles
	 */
	public FastHexaValueGridPortrayal2D CXCL13PortrayalFast	 = new FastHexaValueGridPortrayal2D();
	
	
	public JFrame 	  chemokineDisplayFrame;
	
	public Display2D  ChemokineDisplay;
	
	/**
	 * Constructor - creates a Simulation object
	 */
	public GUIrun()
	{
		super( new SimulationEnvironment(System.currentTimeMillis(),IO.openXMLFile(paramFile) ) );
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
		
		ChemokineDisplay = new Display2D(600,600, this);//has to be hard coded otherwise gets called before the parameter files and goes a bit wacky!
		
										// don't clip the underlying field portrayal to the fields height and width
		ChemokineDisplay.setClipping(false); 
		

	
		// don't clip the underlying field portrayal to the fields height and width
		chemokineDisplayFrame = ChemokineDisplay.createFrame(); 
		chemokineDisplayFrame.setTitle("Chemokine Fields");
		c.registerFrame(chemokineDisplayFrame);						
		chemokineDisplayFrame.setVisible(true);
			
	
		ChemokineDisplay.attach(CXCL13PortrayalFast, "CXCL13 Gradient");

		
		// Load the graphing functionality
		Grapher.init();
		Grapher.schedule = state.schedule;
		
		
		
		// ODE graphing
		chartFrame = Grapher.chart.createFrame();
		chartFrame.setVisible( true );
		chartFrame.pack();
		chartFrame.setLocation( 0, 700 );
		c.registerFrame( chartFrame );
		
		// Antigen graphing
		chartFrameAntigen = Grapher.chartAntigen.createFrame();
		chartFrameAntigen.setVisible( true );
		chartFrameAntigen.pack();
		chartFrameAntigen.setLocation( 0, 700 );
		c.registerFrame( chartFrameAntigen );
				
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
		
		if(chemokineDisplayFrame != null) chemokineDisplayFrame.dispose();
		chemokineDisplayFrame = null;
		ChemokineDisplay = null;
		
	}
	
	/**
	 * Initialise the portrayals
	 */
	public void setupPortrayals()
	{
		// tell the portrayals what to portray and how to portray them
		fdcPortrayal.setField( FDC.drawEnvironment );
		bcPortrayal.setField( BC.drawEnvironment );

		
	    //CXCL13 Portrayals
		CXCL13PortrayalFast.setField(ParticleMoles.getInstance( ParticleMoles.TYPE.CXCL13 ).m_ig2Display);
		CXCL13PortrayalFast.setMap(CXCL13ColorMap);
		
		
		
		display3D.createSceneGraph();
		display3D.reset();
		
		ChemokineDisplay.reset();									// causes the display to re-register itself with the GUIState after every step 
		ChemokineDisplay.setBackdrop(Color.black);
		ChemokineDisplay.repaint();									// show the initial arrangement of the simulation prior to running
		
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
