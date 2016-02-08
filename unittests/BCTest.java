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
import sim3d.cell.StromaEdge;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
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
				/ (40.15 * Settings.DIFFUSION_COEFFICIENT))/60;// need to recalibrate

		Settings.DIFFUSION_STEPS = (int) (1 / Settings.DIFFUSION_TIMESTEP);
		


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

	// Test - should put the Lymphocytes in all of their states and make sure
	// that the state transitions occur as expected
	// Test - see if receptor levels change in response to different ligand
	// inputs

	/*
	 * This is an integration test ensuring chemokine and migration work
	 * together
	 */
	@Test
	public void testChemotaxis() {
		m_pParticle.field[15][15][15] = (50 * Math.pow(10, -9));

		Settings.CXCL13.DECAY_CONSTANT = 0.9999;

		Settings.BC.SIGNAL_THRESHOLD = 5;
		Settings.BC.PERSISTENCE = 0.99;

		//Settings.BC.ODE.Ri = 0;

		// Let's diffuse a little
		Settings.DIFFUSION_STEPS = 2;
		
		//let the chemokine stabilise a bit
		for(int i=0;i< 600; i++)
		{
			m_pParticle.step(null);
		}
	

		// Randomly place 100 BCs
		BC[] bcCells = new BC[100];
		for (int i = 0; i < 100; i++) {
			bcCells[i] = new BC();

			bcCells[i].setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));
		}
		// Let them move a bit
		for (int i = 0; i < 600; i++) {
			for (int j = 0; j < 100; j++) {
				bcCells[j].step(null);
			}
			m_pParticle.field[15][15][15] = 50* Math.pow(10, -9);
			m_pParticle.step(null);
		}

		double avDistance = 0;
		double maxDist = 0;

		// not quite sure what this bit is doing
		for (int i = 0; i < 100; i++) {
			Double3D bcLoc = new Double3D(bcCells[i].x - 15, bcCells[i].y - 15,
					bcCells[i].z - 15);// why take 15 away

			avDistance += bcLoc.length();// add this vector? see how far they
											// are from origin?

			// why do we need maxDist, doesn't seem to be doing anything
			// do we need a maxDist criteria?
			if (maxDist < bcLoc.length()) {
				maxDist = bcLoc.length();
			}
		}

		assertThat(avDistance / 100, lessThan(10.0));// why is this condition
													// here?
	}

	/*
	 * This is an integration test ensuring chemokine and migration work
	 * together
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

	/*
	 * This test makes sure that BCs and Stroma integrate correctly
	 */
	@Test
	public void testShouldCollideWithStroma() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		int iEdges = 1000;

		Double3D[] points = Vector3DHelper.getEqDistPointsOnSphere(iEdges);

		Double3D d3Centre = new Double3D(15, 15, 15);

		points[0] = points[0].multiply(3).add(d3Centre); // what is this line
															// doing

		iEdges--; // what is this line doing
		for (int i = 0; i < iEdges; i++) {
			points[i + 1] = points[i + 1].multiply(3).add(d3Centre);
			StromaEdge seEdge = new StromaEdge(points[i], points[i + 1]);
			seEdge.registerCollisions(cgGrid);
		}

		// place 100 BCs in centre
		BC[] bcCells = new BC[100];
		for (int i = 0; i < 100; i++) {
			bcCells[i] = new BC();

			bcCells[i].setObjectLocation(d3Centre);
		}

		// Let them move a bit
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 100; j++) {
				bcCells[j].step(null);
			}
			cgGrid.step(null);
		}

		// again not fully sure what this bit is doing
		double avDistance = 0;
		double maxDist = 0;
		for (int i = 0; i < 100; i++) {
			Double3D bcLoc = new Double3D(bcCells[i].x - 15, bcCells[i].y - 15,
					bcCells[i].z - 15);

			avDistance += bcLoc.length();

			if (maxDist < bcLoc.length()) {
				maxDist = bcLoc.length();
			}
		}

		// again not sure what these are doing
		assertThat(avDistance / 100, lessThan(3.0));

		BC.m_cgGrid = null;
	}

	/**
	 * Another integration test for BCs and chemokine
	 * 
	 * We want to test that the cell doesn't perfect go towards the chemokine
	 * gradient, but, for example, moves freely in a large area of medium-high
	 * concentration of chemokine, i.e. the stromal network
	 * 
	 * TODO need to rename this test because it is confusing then need another
	 * test called testShouldMoveRandomly
	 * 
	 */
	@Test
	public void testShouldMoveRandomly() {
		for (int i = 0; i < 31; i++) {
			m_pParticle.field[15][15][i] = 4000;
		}

		// m_pParticle.m_dDecayRateInv = 1;

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

		// Randomly place 100 BCs
		BC[] bcCells = new BC[250];
		for (int i = 0; i < 250; i++) {
			bcCells[i] = new BC();

			bcCells[i].setObjectLocation(new Double3D(15, 15, 15));
		}

		// Let them move a bit
		for (int i = 0; i < 400; i++) {
			for (int j = 0; j < 250; j++) {
				bcCells[j].step(null);
			}

			for (int k = 0; k < 31; k++) {
				m_pParticle.field[15][15][k] = (1.7 * Math.pow(10, -9));
			}
			m_pParticle.step(null);
		}

		// not fully sure what this bit down does....
		int[] iaResults = new int[5];

		for (int i = 0; i < 250; i++) {
			iaResults[(int) (5 * (bcCells[i].z - 1) / 29.0)]++;
		}

		assertEquals("0-6", 50, iaResults[0], 15.0);
		assertEquals("6-12", 50, iaResults[1], 15.0);
		assertEquals("12-18", 50, iaResults[2], 15.0);
		assertEquals("18-24", 50, iaResults[3], 15.0);
		assertEquals("24-30", 50, iaResults[4], 15.0);
	}

}
