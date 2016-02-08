/**
 * 
 */
package unittests;


import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.diffusion.Particle;
import sim3d.util.IO;


/**
 * @author sjj509 need to fix these so they work with XML inputs
 */
public class FDCTest {
	private Schedule schedule = new Schedule();
	private Particle m_pParticle;
	public static Document parameters;

	private static void loadParameters() {

		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
	}

	@BeforeClass
	public static void oneTimeSetUp() {

		// load in all of the BC and FDC parameters but overwrite some of the
		// options parameters to make the tests faster

		loadParameters();
		Settings.RNG = new MersenneTwisterFast();
		Settings.WIDTH = 31;
		Settings.HEIGHT = 31;
		Settings.DEPTH = 31;
		Settings.DIFFUSION_COEFFICIENT = 1.519 * Math.pow(10, -10);
		Settings.GRID_SIZE = 0.0001;
		Settings.DIFFUSION_TIMESTEP = Math.pow(Settings.GRID_SIZE, 2)
				/ (3.7 * Settings.DIFFUSION_COEFFICIENT);
		Settings.DIFFUSION_STEPS = (int) (1 / Settings.DIFFUSION_TIMESTEP);
	}

	@Before
	public void setUp() throws Exception {
		m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 31, 31, 31);

		BC.drawEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31,
				31, 31);
	}

	@After
	public void tearDown() {
		m_pParticle.field = null;
		m_pParticle = null;
		Particle.reset();
		BC.drawEnvironment = null;
	}

	// Test - should put the Lymphocytes in all of their states and make sure
	// that the state transitions occur as expected
	// Test - see if receptor levels change in response to different ligand
	// inputs

	// TODO check that the stromal network forms properly
	// TODO check that the stroma can secrete chemokine and that it secretes
	// before it diffuses
	// TODO check that the FDC can express antigen
	// TODO check that the FDC can lose antigen

}
