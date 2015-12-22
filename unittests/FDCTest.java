/**
 * 
 */
package unittests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim.util.MutableDouble3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.cell.StromaEdge;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
/**
 * @author sjj509
 * 		need to fix these so they work with XML inputs
 */
public class FDCTest
{
	private Schedule schedule = new Schedule();
	private Particle m_pParticle;
	public static Document parameters;	
	
	
	private static void loadParameters(){
		
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";		// set the seed for the simulation, be careful for when running on cluster																	
		parameters = IO.openXMLFile(paramFile);
		
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
	}
	
	
	@BeforeClass
    public static void oneTimeSetUp()
	{
		
		//load in all of the BC and FDC parameters but overwrite some of the options parameters to make the tests faster
		
		loadParameters();
		Settings.RNG = new MersenneTwisterFast();
		Settings.WIDTH = 31;
		Settings.HEIGHT = 31;
		Settings.DEPTH = 31;
		Settings.DIFFUSION_COEFFICIENT = 1.519 * Math.pow( 10, -10 );
		Settings.GRID_SIZE = 0.0001;
		Settings.DIFFUSION_TIMESTEP = Math.pow( Settings.GRID_SIZE, 2 ) / (3.7 * Settings.DIFFUSION_COEFFICIENT);
		Settings.DIFFUSION_STEPS	= (int) (1 / Settings.DIFFUSION_TIMESTEP);
    }


	@Before
	public void setUp() throws Exception
	{
		m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 31, 31, 31);
		
		BC.drawEnvironment = new Continuous3D( Settings.BC.DISCRETISATION, 31, 31, 31 );
	}

    @After
    public void tearDown() {
    	m_pParticle.field = null;
    	m_pParticle = null;
    	Particle.reset();
    	BC.drawEnvironment = null;
    }
	
    
    
    
    
    
    // Test - should put the Lymphocytes in all of their states and make sure that the state transitions occur as expected
    // Test - see if receptor levels change in response to different ligand inputs
    
    
    
    
    //TODO check that the stromal network forms properly
    //TODO check that the stroma can secrete chemokine and that it secretes before it diffuses
    //TODO check that the FDC can express antigen
    //TODO check that the FDC can lose antigen
    
    
    
    
    
    
    
    
}
