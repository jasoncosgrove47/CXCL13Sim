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
import sim3d.Settings;
import sim3d.cell.BC;

import sim3d.diffusion.ParticleMoles;
import sim3d.util.IO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * @author sjj509
 * 
 */
public class BCTest {
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
	public static void oneTimeSetUp() {

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

	@Before
	public void setUp() throws Exception {
		m_pParticle = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				31, 31, 31);

		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;
	}

	@After
	public void tearDown() {
		m_pParticle.field = null;
		m_pParticle = null;
		ParticleMoles.reset();
		BC.drawEnvironment = null;

	}


	// Test - see if receptor levels change in response to different ligand
	// inputs

	
	

	
	/*
	 * Test to see if the required guards are met then the BC
	 * changes to the appropriate state.
	 */
	
	@Test
	public void testReceptorsDynamic(){
		
		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -9));

		Settings.BC.ODE.Rf = 10000;
		Settings.BC.ODE.Ri = 10000;
		Settings.BC.ODE.LR = 10000;

		Settings.CXCL13.DECAY_CONSTANT = 0.5;

		Settings.BC.SIGNAL_THRESHOLD = 10;

		BC bc = new BC();
		
		bc.setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));

		
		for (int i = 0; i < 30 ; i ++){
			
			bc.step(null);
			m_pParticle.step(null);
		}
		
		assertThat(bc.m_iL_r , not(equalTo(10000))) ;
		
	}
	

	/*
	 * Make sure that the total number of receptors remains constant
	 */
	@Test
	public void testReceptorConservation() {
		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -9));

		Settings.BC.ODE.Rf = 1000;
		Settings.BC.ODE.Ri = 1000;
		Settings.BC.ODE.LR = 1000;

		Settings.CXCL13.DECAY_CONSTANT = 0.5;

		Settings.BC.SIGNAL_THRESHOLD = 10;

		// Let's diffuse a little
		Settings.DIFFUSION_STEPS = 2;
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);

		// Randomly place a BCs
		BC[] bcCells = new BC[1];
		for (int i = 0; i < 1; i++) {
			bcCells[i] = new BC();

			bcCells[i].setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));
		}
		// Let it move a bit
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 1; j++) {
				bcCells[j].step(null);// why are you passing in null
			}
			m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -9));
			m_pParticle.step(null);
		}

		int totalReceptorParams = (Settings.BC.ODE.Rf + Settings.BC.ODE.Ri + Settings.BC.ODE.LR);
		int totalReceptorSim = (bcCells[0].m_iL_r + bcCells[0].m_iR_i + bcCells[0].m_iR_free);

		assertEquals(totalReceptorSim, totalReceptorParams);// why is this
															// condition here?
	}

	


}
