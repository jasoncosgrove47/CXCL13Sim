package sim3d.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.media.j3d.TransformGroup;

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
import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.diffusion.Chemokine;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import ec.util.MersenneTwisterFast;

public class Algorithm1Test {

	BC bc = new BC();
	private Schedule schedule = new Schedule();
	private Chemokine m_pParticle;
	public static Document parameters;

	/**
	 * Initialise the simulation parameters
	 */
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

		// NEED TO DIVIDE THE WHOLE THING BY 60 AS DIFFUSION UPDATES
		// EVERY SECOND BUT CELLS EVERY 1 MIN
		Settings.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (40.15 * Settings.DIFFUSION_COEFFICIENT));// need
																													// to
		Settings.DIFFUSION_STEPS = (int) (60 / Settings.DIFFUSION_TIMESTEP);

		System.out.println("coefficient: " + Settings.DIFFUSION_COEFFICIENT
				+ "timestep: " + Settings.DIFFUSION_STEPS + "steps: "
				+ Settings.DIFFUSION_TIMESTEP);

	}

	@Before
	public void setUp() throws Exception {

		m_pParticle = new Chemokine(schedule, Chemokine.TYPE.CXCL13,
				31, 31, 31);

		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;

		bc.setObjectLocation(new Double3D(Settings.RNG.nextInt(14) + 8,
				Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8));

		for (int i = 0; i < 3; i++) {

			bc.step(null);
			m_pParticle.step(null);
		}
	}

	@After
	public void tearDown() throws Exception {
		m_pParticle.field = null;
		m_pParticle = null;
		Chemokine.reset();
		BC.drawEnvironment = null;
	}



	/**
	 * Test that determinespacetomove() returns true when 
	 * there is space to move
	 */
	@Test
	public void testDetermineSpaceToMove() {
		// no other cells around so should return true
		Algorithm1 a1 = new Algorithm1();
		
		boolean test = a1.determineSpaceToMove(bc.x + 0.2, bc.y + 0.2,
				bc.z + 0.2);
		assertEquals(true, test);
	}

	/**
	 * Test that determinespacetomove() returns false 
	 * when there isn't space to move
	 */
	@Test
	public void testdetermineSpaceToMove2() {
		
		Algorithm1 a1 = new Algorithm1();
		
		Double3D location = new Double3D(bc.x, bc.y, bc.z);

		
		
		// crowd bc with lots of other agents
		for (int i = 0; i < 30; i++) {
			BC bcTemp = new BC();
			bcTemp.setObjectLocation(location);
		}

		// no space to move so should return false
		boolean test = a1.determineSpaceToMove(bc.x + 0.2, bc.y + 0.2,
				bc.z + 0.2);
		assertEquals(false, test);

	}


	/**
	 * Test that calculateWhereToMoveNext can update the m_d3aMovements array.
	 * TODO this test could definitely be refined
	 */
	@Test
	public void testCalculateWhereToMoveNext() {
		
		Algorithm1 a1 = new Algorithm1();
		
		bc.setM_d3aMovements(new ArrayList<Double3D>());
		a1.calculateWhereToMoveNext(bc);
		// assert movements list has been updated
		assertEquals(false, bc.getM_d3aMovements().isEmpty());
	}

	
	/**
	 * Test that perform saved movements takes data from m_d3aMovements and
	 * updates cells location accordingly
	 */
	@Test
	public void testPerformSavedMovements() {

		Algorithm1 a1 = new Algorithm1();
		
		bc.setM_d3aMovements(new ArrayList<Double3D>());
		bc.getM_d3aMovements().add(new Double3D(1, 1, 1));

		Double3D targetLocation = new Double3D(bc.x + 1, bc.y + 1, bc.z + 1);

		a1.performSavedMovements(bc);
		assertEquals(new Double3D(bc.x, bc.y, bc.z), targetLocation);

	}

	/**
	 * test that a BC can't be accessed once marked as dead
	 */
	@Test
	public void testRemoveDeadCell() {
		BC bcTemp = new BC();
		bcTemp.setObjectLocation(new Double3D(bc.x + 1, bc.y + 1, bc.z + 1));
		bcTemp.setStopper(schedule.scheduleRepeating(bcTemp));
		bcTemp.removeDeadCell(BC.bcEnvironment);
		assertEquals(false, BC.bcEnvironment.exists(bcTemp));
	}

	/**
	 * Test that getLigandBinding can detect chemokine Integration tests to
	 * ensure that the method can detect gradients
	 * 
	 * TODO refine
	 * 
	 * we can calculate how much chemokine is in the surrounding gridspaces
	 * and make sure that the results reflect that
	 */
	@Test
	public void testGetLigandBinding() {

		Algorithm1 a1 = new Algorithm1();
		
		m_pParticle.field[(int) bc.x][(int) bc.y][(int) bc.z] = (1.7 * Math
				.pow(10, -5));
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);

		double[] results;
		results = a1.calculateLigandBindingMoles(bc);

		assertNotNull(results[0]);

	}

	/**
	 * Test that no ligand binds if there is no chemokine there
	 */
	@Test
	public void testGetLigandBinding2() {
		
		Algorithm1 a1 = new Algorithm1();
		
		double[] results;
		results = a1.calculateLigandBindingMoles(bc);
		assertThat(results[0], equalTo(0.0));
	}

	/**
	 * test that getMoveDirection returns a double3D Integration tests ensure
	 * that the correct direction is provided
	 * 
	 * TODO refine we could put the chemokine north of the cell and see 
	 * if it orientates towards that direction
	 * 
	 * 
	 */
	@Test
	public void testGetMoveDirection() {

		Algorithm1 a1 = new Algorithm1();
		
		m_pParticle.field[(int) bc.x][(int) bc.y][(int) bc.z] = (1.7 * Math
				.pow(10, -5));
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);

		Double3D test = a1.getMoveDirection(bc);
		assertNotNull(test);

	}







	/**
	 * Assert that receptor numbers can change over time
	 */
	@Test
	public void testReceptorStepDynamic() {

		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -9));

		Settings.BC.ODE.Rf = 10000;
		Settings.BC.ODE.Ri = 10000;
		Settings.BC.ODE.LR = 10000;

		Settings.CXCL13.DECAY_CONSTANT = 0.5;

		Settings.BC.SIGNAL_THRESHOLD = 10;

		BC bc = new BC();

		bc.setObjectLocation(new Double3D(Settings.RNG.nextInt(14) + 8,
				Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8));

		for (int i = 0; i < 30; i++) {

			bc.step(null);
			m_pParticle.step(null);
		}

		assertThat(bc.m_iL_r, not(equalTo(10000)));

	}

	/**
	 * Assert that the total number of receptors remains constant TODO simplify
	 */
	@Test
	public void testReceptorStepConservation() {
		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -9));

		Settings.BC.ODE.Rf = 1000;
		Settings.BC.ODE.Ri = 1000;
		Settings.BC.ODE.LR = 1000;
		Settings.CXCL13.DECAY_CONSTANT = 0.5;
		Settings.BC.SIGNAL_THRESHOLD = 10;
		Settings.DIFFUSION_STEPS = 2;

		// Let's diffuse a little
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
