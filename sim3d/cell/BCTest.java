package sim3d.cell;

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
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import ec.util.MersenneTwisterFast;



public class BCTest {

	BC bc = new BC();
	
	private Schedule schedule = new Schedule();
	private ParticleMoles m_pParticle;
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

	/**
	 * Check that we can add collision points
	 */
	@Test
	public void testAddCollisionPoints(){
		
		Int3D test = new Int3D(1,2,3);
		Int3D test2 = new Int3D(41,41,41);
		bc.addCollisionPoint(test);
	
		assertEquals(true,bc.m_i3lCollisionPoints.contains(test));
		assertEquals(false,bc.m_i3lCollisionPoints.contains(test2));
	}
	
	/**
	 * Test that determinespacetomove() returns true
	 * when there is space to move 
	 */
	@Test	
	public void testDetermineSpaceToMove(){
		//no other cells around so should return true
		boolean test = bc.determineSpaceToMove(bc.x+0.2,bc.y+0.2,bc.z+ 0.2);
		assertEquals(true,test);
	}
	
	/**
	 * Test that determinespacetomove() returns 
	 * false when there isn't space to move
	 */
	@Test
	public void testdetermineSpaceToMove2(){
		Double3D location = new Double3D(bc.x,bc.y,bc.z);
		
		//crowd bc with lots of other agents
		for(int i =0; i < 30; i++)
		{
			BC bcTemp = new BC();
			bcTemp.setObjectLocation(location);
		}
		
		//should not be any space to move so should return false
		boolean test = bc.determineSpaceToMove(bc.x+0.2,bc.y+0.2,bc.z+ 0.2);
		assertEquals(false,test);
		
	}

	/**
	 * Test that getCollisionClass returns the 
	 * correct enum for a B cell
	 */
	@Test
	public void testGetCollisionClass(){
	
		assertEquals(bc.getCollisionClass() ,CLASS.BC); 
	}
	
	/**
	 * Test that calculateWhereToMoveNext can
	 * update the m_d3aMovements array
	 */
	@Test
	public void testCalculateWhereToMoveNext(){
		
		bc.m_d3aMovements = new ArrayList<Double3D>();
		bc.calculateWhereToMoveNext();
		
		//assert movements list has been updated
		assertEquals(false,bc.m_d3aMovements.isEmpty());
	}
	
	/**
	 * Test that perform saved movements takes data
	 * from m_d3aMovements and updates cells location 
	 * accordingly
	 */
	@Test
	public void testPerformSavedMovements(){
		
		bc.m_d3aMovements = new ArrayList<Double3D>();
		bc.m_d3aMovements.add(new Double3D(1,1,1));
		
		Double3D targetLocation = new Double3D(bc.x+1,bc.y+1,bc.z+1);
		
		bc.performSavedMovements();
		assertEquals(new Double3D(bc.x,bc.y,bc.z), targetLocation);
		
	}
	
	/**
	 * test that a BC can't be accessed 
	 * once marked as dead
	 */
	@Test
	public void testRemoveDeadCell(){
		BC bcTemp = new BC();
		bcTemp.setObjectLocation(new Double3D(bc.x+1,bc.y+1,bc.z+1));
		bcTemp.setStopper(schedule.scheduleRepeating(bcTemp));
		bcTemp.removeDeadCell(BC.bcEnvironment);
		assertEquals(false, BC.bcEnvironment.exists(bcTemp));
	}
	
	
	/**
	 * Test that getLigandBinding can detect chemokine
	 */
	@Test
	public void testGetLigandBinding(){
		
		
		m_pParticle.field[(int) bc.x][(int) bc.y][(int) bc.z] = (1.7 * Math.pow(10, -5));
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);
		m_pParticle.step(null);

		double[] results;
		results = bc.calculateLigandBindingNew();
		
		
		
		assertNotNull(results[0]);
		
	}
	
	/**
	 * Test that no ligand binds if 
	 * there is no chemokine there
	 */
	@Test
	public void testGetLigandBinding2(){
		double[] results;
		results = bc.calculateLigandBindingNew();
		assertThat(results[0] , equalTo(0.0)); 
	}

	/**
	 * TODO
	 */
	@Test
	public void testGetMoveDirection(){
		double[] results;
		results = bc.calculateLigandBindingNew();
		assertThat(results[0] , equalTo(0.0)); 
	}

	/**
	 * TODO need a sensible way of having a collision grid ot detect collisions
	 */
	@Test
	public void testRegisterCollisions(){
		// TODO needs some thinking we need a way to initialise the
		// collision grid
		// could generate a stromal network 
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		//generate a stromal cage, around the center of the grid
		int iEdges = 1000;
		Double3D[] points = Vector3DHelper.getEqDistPointsOnSphere(iEdges);
		Double3D d3Centre = new Double3D(15, 15, 15);
		points[0] = points[0].multiply(3).add(d3Centre); 

		iEdges--; // what is this line doing
		for (int i = 0; i < iEdges; i++) {
			points[i + 1] = points[i + 1].multiply(3).add(d3Centre);
			StromaEdge seEdge = new StromaEdge(points[i], points[i + 1]);
			seEdge.registerCollisions(cgGrid);
		}

		// Randomly place a BCs
		BC[] bcCells = new BC[10];
		for (int i = 0; i < 10; i++) {
			bcCells[i] = new BC();

			bcCells[i].setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));
		}
		// Let it move a bit
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				bcCells[j].step();// why are you passing in null
			}
			
		}
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}
		
	/**
	 * If a B cell doesn't collide with a stromal grid then it should return a false
	 * 
	 * 
	 * TODO i think the positive test for this is accounted for in integration tests
	 */
	@Test
	public void testCollideStromaEdge(){

		BC bc = new BC();
	
		Double3D loc1 = new Double3D(0,0,0);
		Double3D loc2 = new Double3D(1,1,1);
		
		bc.setObjectLocation(loc1);
		StromaEdge se = new StromaEdge(loc1,loc2);
		bc.m_d3aMovements.add(new Double3D(loc2));
		boolean test = bc.collideStromaEdge(se, 1);
		assertEquals(true, test);	
	}
	
	/**
	 * =
	 */
	@Test
	public void testUpdateMovementToAccountForCollision(){

		BC bc = new BC();
		
		Double3D loc1 = new Double3D(0,0,0);
		Double3D loc2 = new Double3D(1,1,1);
		
		bc.setObjectLocation(loc1);
		StromaEdge se = new StromaEdge(loc1,loc2);
		bc.m_d3aMovements.add(new Double3D(loc2));
		
		assertEquals(loc2,bc.m_d3aMovements.get(0));
		
		bc.collideStromaEdge(se, 1);
		Double3D test = bc.m_d3aMovements.get(0);
		
		assertNotEquals(loc2,test);
		
	}
	
	/**
	 * 3 different tests to be done here
	 * case 1: e <= 0
	 * case 2: e >= f
	 * case 3: else
	 * TODO
	 */
	@Test
	public void testUpdateLength(){

		// case 1: e <= 0
		double length = 1;
		double e = -1;
		double f =1;
		Double3D ac = new Double3D(2,2,2);
		double output = bc.updateLength(length, ac, ac, e, f, ac);
		double newlength = Vector3DHelper.dotProduct(ac, ac);
		assertEquals(output, newlength,0.1);
		
		// case 2: e >= f
		double e2 = 24;
		double f2 = Vector3DHelper.dotProduct(ac, ac);
		Double3D ac2 = new Double3D(1,1,1);
		double output2 = bc.updateLength(length, ac, ac2, e2, f2, ac);
		double length2 = Vector3DHelper.dotProduct(ac2, ac2);
		assertEquals(output2, length2,0.1);
	
		// case 3: else
		double e3 = 1;
		double f3 = Vector3DHelper.dotProduct(ac, ac);
		Double3D ac3 = new Double3D(1,1,1);
		double output3 = bc.updateLength(length, ac, ac3, e3, f3, ac);
		double length3 = Vector3DHelper.dotProduct(ac, ac) - e3 * e3 / f3;
		assertEquals(output3, length3,0.1);
		
	}
	
	/**
	 * TODO
	 */
	@Test
	public void testCalculateSNew(){

		double s = 2;
		double length = 10;
		Double3D d1 = new Double3D(1,1,1);
		Double3D d2 = new Double3D(2,2,2);
		
		double output = 0;
		output = bc.calculateSNew(s,length, d1,d2);
		assertTrue(output > 0);
		
	}
	
	//TODO
	@Test
	public void testFindClosestPointsBetween(){

		StromaEdge seEdge = new StromaEdge(new Double3D(1,1,1),new Double3D(2,2,2));
		
		Double3D d1 = new Double3D(2,2,2); //destination where cell wants to go
		Double3D p1 = new Double3D(1,1,1); // cells current position	
	
		Double3D p2 = seEdge.getPoint1();
		Double3D d2 = seEdge.getPoint2().subtract(p2);
		
		double s = 0;
		double t = 0;
		
		Double3D r = p1.subtract(p2); // p1 - p2
		double a = Vector3DHelper.dotProduct(d1, d1); // squared length of												// segment s1											// always positive
		double b = Vector3DHelper.dotProduct(d1, d2);
		double c = Vector3DHelper.dotProduct(d1, r);
		double e = Vector3DHelper.dotProduct(d2, d2); // squared length of
														
		double f = Vector3DHelper.dotProduct(d2, r);
		double denom = a * e - b * b; // >= 0
		
		List<Double> closestPoints = null;
		closestPoints = bc.findClosestPointsBetween(0, p1, p2, d1,d2, denom, s,t, a,b,c,e,f);
			
		assertTrue(closestPoints.size() > 0);

	}
	

	@Test
	public void testModelMovements() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		BC bc = new BC();
		TransformGroup localTG = bc.getModel(bc,null);
			
		bc.m_d3aCollisions.add(new Double3D(1,1,1));
		bc.modelMovements(bc.m_d3aCollisions, bc, localTG);
	
		assertEquals(2,localTG.numChildren());
	
	}
	
	@Test
	public void testModelCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

	
		BC bc = new BC();
		TransformGroup localTG = bc.getModel(bc,null);
			
		bc.m_d3aCollisions.add(new Double3D(1,1,1));
		
		bc.modelCollisions(bc.m_d3aCollisions, bc, localTG);
	
		assertEquals(2,localTG.numChildren());
	
	}
	
	
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

	
		BC bc = new BC();
		TransformGroup localTG = bc.getModel(bc,null);
	
		assertNotNull(localTG);
	
	}
	
	
	//move the cell beyond the grid and see if handlebounce updates the coordinates correctly
	@Test
	public void testHandleBounce(){

		//make a small grid, let the cells migrate around
		//make sure that none of them are outside of the gridspace
	
		BC bc = new BC();
		
		Double3D loc = new Double3D(32,32,32);
		bc.m_d3aMovements.add(new Double3D(30,30,30));
		bc.m_d3aMovements.add(loc);
		bc.handleBounce();
		
		Double3D test = bc.m_d3aMovements.get(1);
		
		assertNotEquals(loc,test);
	
	}

	/**
	 * Test to see if the required guards are met then the BC
	 * changes to the appropriate state.
	 */
	@Test
	public void testReceptorStepDynamic(){
		
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
	public void testReceptorStepConservation() {
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
