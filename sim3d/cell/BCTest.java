package sim3d.cell;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.IO;
import ec.util.MersenneTwisterFast;

public class BCTest {

	
	BC bc = new BC();
	
	
	private Schedule schedule = new Schedule();
	private ParticleMoles m_pParticle;
	public static Document parameters;

	private static void loadParameters() {

		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		parameters = IO.openXMLFile(paramFile);
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
	}
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		// load in all of the BC and FDC parameters but overwrite some of the
		// options parameters to make the tests faster
		loadParameters();
		Settings.RNG = new MersenneTwisterFast();
		Settings.WIDTH = 31;
		Settings.HEIGHT = 31;
		Settings.DEPTH = 31;
		
		
		Settings.DIFFUSION_COEFFICIENT = 0.0000000000076;
		Settings.GRID_SIZE = 0.00001;

		//NEED TO DIVIDE THE WHOLE THING BY 60 AS DIFFUSION UPDATES
		// EVERY SECOND BUT CELLS EVERY 1 MIN
		Settings.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2)
				/ (40.15 * Settings.DIFFUSION_COEFFICIENT));// need to recalibrate

		Settings.DIFFUSION_STEPS = (int) (60 / Settings.DIFFUSION_TIMESTEP);
		


		System.out.println("coefficient: " + Settings.DIFFUSION_COEFFICIENT
				+ "timestep: " + Settings.DIFFUSION_STEPS + "steps: "
				+ Settings.DIFFUSION_TIMESTEP);
		
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		m_pParticle = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				31, 31, 31);

		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;
	
	
		
		bc.setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));

		
		for (int i = 0; i < 3 ; i ++){
			
			bc.step(null);
			m_pParticle.step(null);
		}
		
		
	}

	@After
	public void tearDown() throws Exception {
		m_pParticle.field = null;
		m_pParticle = null;
		ParticleMoles.reset();
		BC.drawEnvironment = null;
		
		
	}

	
	@Test
	public void testAddCollisionPoints(){
		
		Int3D test = new Int3D(1,2,3);
		Int3D test2 = new Int3D(41,41,41);
		bc.addCollisionPoint(test);
	
		assertEquals(true,bc.m_i3lCollisionPoints.contains(test));
		assertEquals(false,bc.m_i3lCollisionPoints.contains(test2));
	}
	
	
	
	/**
	@Test
	public void test() {
		
		//make sure that the relevant datastructure gets updated
		bc.addCollisionPoint(i3Point);
		
		
		//surround the BC with loads of cells and make sure it cant move
		//No surrounding cells should be able to move
		bc.determineSpaceToMove(x, y, z);
		bc.checkFreeSpaceAlongPath(x, y, z, oldLocation);
		
		//assert that class is BC and that it is not cBC
		bc.getClass();
		bc.getCollisionClass();
		
		//need to put the cell very near stroma for this to work
		bc.getPositionAlongStroma();
		
		//if collisions are registered make sure htey are handled
		bc.handleCollisions(cgGrid);
		
		//make sure the collisions can be registered with the grid
		bc.registerCollisions(cgGrid);
		
		//make sure you cant access teh cell anymore once removed
		bc.removeDeadCell(randomSpace);
		
		//make sure that the BC moves once its stepped...
		bc.step();
		
		fail("Not yet implemented");
		
		
		
	}

*/

}
