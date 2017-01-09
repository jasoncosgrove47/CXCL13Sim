package sim3d.cell;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.util.List;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.diffusion.Chemokine;
import sim3d.stroma.StromaEdge;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;

public class TCTest {

	TC tc;
	private Chemokine m_pParticle;
	private Chemokine m_pParticle2;
	private Schedule schedule = new Schedule();
	public static Document parameters;
	
	/**
	 * Initialise the simulation parameters
	 */
	private static void loadParameters() {

		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
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

		Settings.CXCL13.DIFFUSION_COEFFICIENT = 0.0000000000076;
		Settings.GRID_SIZE = 0.00001;

		// NEED TO DIVIDE THE WHOLE THING BY 60 AS DIFFUSION UPDATES
		// EVERY SECOND BUT CELLS EVERY 1 MIN
		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (40.15 * Settings.CCL19.DIFFUSION_COEFFICIENT));// need
																													// to
		Settings.CXCL13.DIFFUSION_STEPS = (int) (60 / Settings.CCL19.DIFFUSION_TIMESTEP);


		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	
	@Before
	public void setUp() throws Exception {
		tc = new TC();
		

		m_pParticle = new Chemokine(schedule, Chemokine.TYPE.CXCL13,
				31, 31, 31);
		
		m_pParticle2 = new Chemokine(schedule, Chemokine.TYPE.EBI2L,
				31, 31, 31);

		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;

		tc.setObjectLocation(new Double3D(Settings.RNG.nextInt(14) + 8,
				Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8));

		//for (int i = 0; i < 3; i++) {

			//tc.step(null);
//			m_pParticle.step(null);
//			m_pParticle2.step(null);
	//	}
		
		
		
	}

	@After
	public void tearDown() throws Exception {
	}



	
	/**
	 * Test that getCollisionClass returns the correct enum for a B cell
	 */
	@Test
	public void testGetCollisionClass() {
		assertEquals(tc.getCollisionClass(), CLASS.LYMPHOCYTE);
	}

	/**
	 * Check that we can add collision points
	 */
	@Test
	public void testAddCollisionPoints() {

		Int3D test = new Int3D(1, 2, 3);
		Int3D test2 = new Int3D(41, 41, 41);
		tc.addCollisionPoint(test);

		// assert that the correct collision points have ben added
		// to the m_i3lCollisionPoints hashset
		assertEquals(true, tc.getM_i3lCollisionPoints().contains(test));
		assertEquals(false, tc.getM_i3lCollisionPoints().contains(test2));
	}

	/**
	 * Tests that register collisions can add data to the collisionGrid
	 * 
	 */
	@Test
	public void testRegisterCollisions() {

		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		
		// generate some coordinates and register collisions
		TC tc = new TC();
		Double3D loc1 = new Double3D(5, 5, 5);
		Double3D loc2 = new Double3D(6, 6, 6);

	
		tc.getM_d3aMovements().add(loc1);
		//bc.m_d3aMovements.add(loc2);
		tc.registerCollisions(cgGrid);

		
		// assert that the collision data is added
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
		
		// assert that the correct data is added
		Int3D validate = new Int3D(5, 5, 5);
		assertTrue(cgGrid.getM_i3lCollisionPoints().contains(validate));
			
		
	}
	
	
	
	
	/**
	 * test that a BC can't be accessed once marked as dead
	 */
	@Test
	public void testRemoveDeadCell() {

		tc.setObjectLocation(new Double3D(1, 1, 1));
		tc.setStopper(schedule.scheduleRepeating(tc));
		tc.removeDeadCell(BC.bcEnvironment);
		assertEquals(false, BC.bcEnvironment.exists(tc));
	}
	
	/**
	 * If a B cell doesn't collide with a stromal grid then it should return a
	 * false
	 */
	@Test
	public void testCollideStromaEdge() {



		Double3D loc1 = new Double3D(0, 0, 0);
		Double3D loc2 = new Double3D(1, 1, 1);
		tc.setObjectLocation(loc1);
		StromaEdge se = new StromaEdge(loc1, loc2,StromaEdge.TYPE.RC_edge);
		tc.getM_d3aMovements().add(new Double3D(loc2));

		// assert that the stroma and BC collide
		boolean test = tc.collideStromaEdge(se, 1);
		assertEquals(true, test);

		// assert that the stroma and BC don't collide
		Double3D loc3 = new Double3D(5, 5, 5);
		tc.setObjectLocation(loc3);
		boolean test2 = tc.collideStromaEdge(se, 1);
		assertEquals(false, test2);

	}

	/**
	 * Assert that the putative movements of the cell are 
	 * updated if there is a
	 * stromal cell in the way
	 */
	@Test
	public void testUpdateMovementToAccountForCollision() {

		
		TC tc = new TC();
		Double3D loc1 = new Double3D(0, 0, 0);
		Double3D loc2 = new Double3D(1, 1, 1);

		// set the BC and SE location
		tc.setObjectLocation(loc1);
		StromaEdge se = new StromaEdge(loc1, loc2,StromaEdge.TYPE.RC_edge);
		tc.getM_d3aMovements().add(new Double3D(loc2));

		// assert that the cell is moving towards loc2
		assertEquals(loc2, tc.getM_d3aMovements().get(0));

		// assert that movement has been updated because
		// there is a stroma edge in the way
		tc.collideStromaEdge(se, 1);
		assertNotEquals(loc2, tc.getM_d3aMovements().get(0));

	}

	/**
	 * Updates the length between a BC and a stroma
	 * 
	 * 3 different tests to be done here case 1: e <= 0 case 2: e >= f case 3:
	 * else
	 */
	@Test
	public void testUpdateLength() {

		// case 1: e <= 0
		double length = 1;
		double e = -1;
		double f = 1;
		Double3D ac = new Double3D(2, 2, 2);
		double output = tc.updateLength(length, ac, ac, e, f, ac);
		double newlength = Vector3DHelper.dotProduct(ac, ac);
		assertEquals(output, newlength, 0.1);

		// case 2: e >= f
		double e2 = 24;
		double f2 = Vector3DHelper.dotProduct(ac, ac);
		Double3D ac2 = new Double3D(1, 1, 1);
		double output2 = tc.updateLength(length, ac, ac2, e2, f2, ac);
		double length2 = Vector3DHelper.dotProduct(ac2, ac2);
		assertEquals(output2, length2, 0.1);

		// case 3: else
		double e3 = 1;
		double f3 = Vector3DHelper.dotProduct(ac, ac);
		Double3D ac3 = new Double3D(1, 1, 1);
		double output3 = tc.updateLength(length, ac, ac3, e3, f3, ac);
		double length3 = Vector3DHelper.dotProduct(ac, ac) - e3 * e3 / f3;
		assertEquals(output3, length3, 0.1);

	}

	/**
	 * Assert that calculate SNew returns a non-zero output TODO need to make
	 * sure that the correct data is being returned
	 */
	@Test
	public void testCalculateSNew() {

		// generate input variables
		double s = 2;
		double length = 10;
		Double3D d1 = new Double3D(1, 1, 1);
		Double3D d2 = new Double3D(2, 2, 2);

		// test that the output is non-zero
		double output = 0;
		output = tc.calculateSNew(s, length, d1, d2);
		assertTrue(output > 0);

	}

	/**
	 * Assert that calculate FindClosestPointsBetween returns a non-zero output
	 * TODO need to make sure that the correct data is being returned.
	 */
	@Test
	public void testFindClosestPointsBetween() {

		StromaEdge seEdge = new StromaEdge(new Double3D(1, 1, 1), new Double3D(
				2, 2, 2),StromaEdge.TYPE.FDC_edge);

		// generate input data
		Double3D d1 = new Double3D(2, 2, 2); // destination where cell wants to
												// go
		Double3D p1 = new Double3D(1, 1, 1); // cells current position
		Double3D p2 = seEdge.getPoint1();
		Double3D d2 = seEdge.getPoint2().subtract(p2);
		double s = 0;
		double t = 0;
		Double3D r = p1.subtract(p2);
		double a = Vector3DHelper.dotProduct(d1, d1); // segment s1 // always
														// positive
		double b = Vector3DHelper.dotProduct(d1, d2);
		double c = Vector3DHelper.dotProduct(d1, r);
		double e = Vector3DHelper.dotProduct(d2, d2);
		double f = Vector3DHelper.dotProduct(d2, r);
		double denom = a * e - b * b; // >= 0

		// assert that the method returns a List<Double>
		List<Double> closestPoints = null;
		closestPoints = tc.findClosestPointsBetween(0, p1, p2, d1, d2, denom,
				s, t, a, b, c, e, f);
		assertTrue(closestPoints.size() > 0);
	}

	/**
	 * Assert that model movements adds a child to the BCs transformgroup
	 */
	@Test
	public void testModelMovements() {

		// generate input data and initialise system
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		TC tc = new TC();
		TransformGroup localTG = tc.getModel(tc, null);
		tc.getM_d3aCollisions().add(new Double3D(1, 1, 1));

		// assert that the correct number of children
		// are added to the transformGroup object
		tc.modelMovements(tc.getM_d3aCollisions(), tc, localTG);
		assertEquals(2, localTG.numChildren());

	}

	/**
	 * Assert that modelCollisions adds a child to the BCs transformgroup
	 */
	@Test
	public void testModelCollisions() {

		// generate input data and initialise system
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		TC tc = new TC();
		TransformGroup localTG = tc.getModel(tc, null);
		tc.getM_d3aCollisions().add(new Double3D(1, 1, 1));

		// assert that the correct number of children
		// are added to the transformGroup object
		tc.modelCollisions(tc.getM_d3aCollisions(), tc, localTG);
		assertEquals(2, localTG.numChildren());

	}

	/**
	 * Assert that getModel returns a TransformGroup object
	 */
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		TC tc = new TC();
		TransformGroup localTG = tc.getModel(tc, null);
		assertTrue(localTG instanceof TransformGroup);
	}

	/**
	 * move the cell beyond the grid and see if handlebounce updates the
	 */
	@Test
	public void testHandleBounce() {

		// Generate input data
		TC tc = new TC();
		Double3D loc = new Double3D(32, 32, 32);
		tc.getM_d3aMovements().add(new Double3D(30, 30, 30));
		tc.getM_d3aMovements().add(loc);

		// assert that the cells putative location has been changed
		// from loc
		tc.handleBounce();
		Double3D test = tc.getM_d3aMovements().get(1);
		assertNotEquals(loc, test);

	}

	/**
	 * Assert that receptor numbers can change over time
	 */
	@Test
	public void testReceptorStepDynamic() {

		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -22));

		Settings.BC.ODE.Rf = 10000;
		Settings.BC.ODE.Ri = 10000;
		Settings.BC.ODE.LR = 10000;

		Settings.CCL19.DECAY_CONSTANT = 0.5;

		Settings.BC.SIGNAL_THRESHOLD = 10;

		TC tc = new TC();

		tc.setObjectLocation(new Double3D(Settings.RNG.nextInt(14) + 8,
				Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8));

		for (int i = 0; i < 30; i++) {

			System.out.println("iteration is: " + i);
			tc.step(null);
			//m_pParticle.step(null);
		}

		assertThat(tc.getM_LR(Lymphocyte.Receptor.CCR7), not(equalTo(10000)));

	}

	/**
	 * Assert that the total number of receptors remains constant 
	 */
	@Test
	public void testReceptorStepConservation() {
		m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -22));

		Settings.BC.ODE.Rf = 1000;
		Settings.BC.ODE.Ri = 1000;
		Settings.BC.ODE.LR = 1000;
		Settings.CCL19.DECAY_CONSTANT = 0.5;
		Settings.BC.SIGNAL_THRESHOLD = 10;
		Settings.CCL19.DIFFUSION_STEPS = 2;

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
		TC[] tcCells = new TC[1];
		for (int i = 0; i < 1; i++) {
			tcCells[i] = new TC();

			tcCells[i].setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));
		}
		// Let it move a bit
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 1; j++) {
				tcCells[j].step(null);// why are you passing in null
			}
			m_pParticle.field[15][15][15] = (1.7 * Math.pow(10, -22));
			m_pParticle.step(null);
		}

		
		int totalReceptorParams = (Settings.BC.ODE.Rf + Settings.BC.ODE.Ri + Settings.BC.ODE.LR);
		int totalReceptorSim = (tcCells[0].getM_LR(Lymphocyte.Receptor.CXCR5) + 
				tcCells[0].getM_Ri(Lymphocyte.Receptor.CXCR5) + tcCells[0].getM_Rf(Lymphocyte.Receptor.CXCR5) + tcCells[0].getM_Rd(Lymphocyte.Receptor.CXCR5));

		assertEquals(totalReceptorSim, totalReceptorParams);// why is this
															// condition here?
	}

}

