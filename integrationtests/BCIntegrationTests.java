/**
 * 
 */
package integrationtests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import dataLogger.Controller;
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.cell.StromaEdge;
import sim3d.cell.cognateBC;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author jason cosgrove, sjj509
 */
public class BCIntegrationTests {

	// a new schedule to step the tests
	private Schedule schedule = new Schedule();
	private ParticleMoles m_pParticle; // an instance of particle moles
	public static Document parameters; // parameter file in .xml format

	/**
	 * Load simulation parameters
	 */
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
		Settings.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (10.00 * Settings.DIFFUSION_COEFFICIENT));
		Settings.DIFFUSION_STEPS = (int) (60 / Settings.DIFFUSION_TIMESTEP);

		// useful for debugging to know what these values are
		System.out.println("coefficient: " + Settings.DIFFUSION_COEFFICIENT
				+ "timestep: " + Settings.DIFFUSION_STEPS + "steps: "
				+ Settings.DIFFUSION_TIMESTEP);

	}

	@Before
	public void setUp() throws Exception {

		// instantiate particlemoles
		m_pParticle = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				31, 31, 31);

		// initialise the BC environment
		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;

		// set receptors back to normal levels
		Settings.BC.ODE.LR = 0;
		Settings.BC.ODE.Rf = 30000;
		Settings.BC.ODE.Ri = 0;

	}

	@After
	public void tearDown() {
		m_pParticle.field = null;
		m_pParticle = null;
		ParticleMoles.reset();
		BC.drawEnvironment = null;
	}

	/*
	 * Ensure that the cell can enter a CXCL13 sensitive state sets a chemokine
	 * point source and checks that after a certain number of steps BCs
	 * accumulate within certain distance from the source
	 */
	@Test
	public void testCXCL13SENSITIVE() {

		
		double test1 = m_pParticle.field[15][15][15];

		System.out.println("test1: " + test1);

		m_pParticle.field[15][15][15] = (1 * Math.pow(10, -16));

		// make the BCs highly sensitive to chemokine
		Settings.CXCL13.DECAY_CONSTANT = 0.005;
		Settings.BC.SIGNAL_THRESHOLD = 1;
		// Settings.BC.ODE.Ka = 10e-10;

		// Settings.BC.ODE.Rf = 0;
		// Settings.BC.ODE.LR = 0;
		// Settings.BC.ODE.Ri = 0;

		// Let's diffuse a little
		Settings.DIFFUSION_STEPS = 2;

		// let the chemokine stabilise a bit
		for (int i = 0; i < 10; i++) {
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
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 100; j++) {
				bcCells[j].step(null);
			}

			m_pParticle.field[15][15][15] += 5 * Math.pow(10, -17);
			m_pParticle.step(null);
		}

		double avDistance = 0; // distance counter

		// calculate the distance between a BC and the chemokine source
		for (int i = 0; i < 100; i++) {
			Double3D bcLoc = new Double3D(bcCells[i].x - 15, bcCells[i].y - 15,
					bcCells[i].z - 15); // see how far they are from origin

			avDistance += bcLoc.length();
		}

		double test = m_pParticle.field[12][12][12];
		System.out.println("amount of chemokine is: " + test);
		System.out.println("avdist: " + (avDistance / 100));
		// assert that BCs accumulate at the source of chemokine
		assertThat(avDistance / 100, lessThan(9.0));

	}

	/*
	 * This test makes sure that cognate BCs enter the system in a NAIVE state
	 */
	@Test
	public void testNAIVE() {
		cognateBC cBC = new cognateBC(0);
		assertEquals(cBC.type, cognateBC.TYPE.NAIVE);
	}

	/**
	 * Test that the state of a cBC changes to PRIMED following uptake of
	 * antigen
	 */
	@Test
	public void testPRIMED() {

		// instantiate a BC and a stromal edge and get them to interact
		cognateBC cBC = new cognateBC(1);
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1));
		cBC.acquireAntigen(se);

		// assert that the BC is now primed
		assertEquals(cognateBC.TYPE.PRIMED, cBC.type);
	}

	/*
	 * This test makes sure that BCs and Stroma collide correctly
	 */
	@Test
	public void testCOLLISION() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		// generate a stromal cage, around the center of the grid
		int iEdges = 1000;
		Double3D[] points = Vector3DHelper.getEqDistPointsOnSphere(iEdges);
		Double3D d3Centre = new Double3D(15, 15, 15);
		points[0] = points[0].multiply(3).add(d3Centre);
		iEdges--;
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

		// determine the distance of each B cell from
		// the center of the grid
		double avDistance = 0;

		for (int i = 0; i < 100; i++) {
			Double3D bcLoc = new Double3D(bcCells[i].x - 15, bcCells[i].y - 15,
					bcCells[i].z - 15);

			avDistance += bcLoc.length();
		}

		// assert that no BCs have escaped the stroma cage
		assertThat(avDistance / 100, lessThan(3.0));

	}

	/**
	 * 
	 * * We want to test that the cell doesn't perfect go towards the chemokine
	 * gradient, but, for example, moves freely in a large area of medium-high
	 * concentration of chemokine, i.e. the stromal network
	 * 
	 * Can fail on occassion due to chance, but if ran multiple times then
	 * should pass if code is ok
	 */
	@Test
	public void testTRANSIENTSENSITIVITY() {
		for (int i = 0; i < 31; i++) {
			m_pParticle.field[15][15][i] = 4000;
		}

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
		for (int i = 0; i < 800; i++) {
			for (int j = 0; j < 250; j++) {
				bcCells[j].step(null);
			}

			for (int k = 0; k < 31; k++) {
				m_pParticle.field[15][15][k] = (1.7 * Math.pow(10, -9));
			}
			m_pParticle.step(null);
		}

		// determine where all of the cells are localised
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

	/**
	 * Test that the cell doesn't perfect go towards the chemokine gradient,
	 * but, for example, moves freely in a large area of medium-high
	 * concentration of chemokine, i.e. the stromal network
	 * 
	 * Can fail on occasion due to chance so needs to be run multiple times.
	 */
	@Test
	public void testNONCXCR5EXPRESSINGequalsDESENSITISED() {
		for (int i = 0; i < 31; i++) {
			m_pParticle.field[15][15][i] = 4000;
		}

		// get rid of all CXCR5
		Settings.BC.ODE.LR = 0;
		Settings.BC.ODE.Rf = 0;
		Settings.BC.ODE.Ri = 0;

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
		for (int i = 0; i < 800; i++) {
			for (int j = 0; j < 250; j++) {
				bcCells[j].step(null);
			}

			// chemokine diffuses faster than cells are updated
			for (int k = 0; k < 31; k++) {
				m_pParticle.field[15][15][k] = (1.7 * Math.pow(10, -9));
			}
			m_pParticle.step(null);
		}

		// determine where all of the cells are localised
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
