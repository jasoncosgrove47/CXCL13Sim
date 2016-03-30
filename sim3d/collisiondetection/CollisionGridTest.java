package sim3d.collisiondetection;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.cell.StromaEdge;

public class CollisionGridTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddCollisionPotential() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		cgGrid.addCollisionPotential(1, 1, 1, bc);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
	}
	
	@Test
	public void testGetPoints() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		cgGrid.addCollisionPotential(1, 1, 1, bc);
		
		Int3D loc = new Int3D(1,1,1);
		assertEquals(false, cgGrid.getPoints(loc).isEmpty());

	}
	
	@Test
	public void testBoxSphereIntersect() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		assertEquals(true,cgGrid.BoxSphereIntersect(5, 5, 5, 3, 5, 5, 5));
		assertEquals(false,cgGrid.BoxSphereIntersect(5, 5, 5, 3, 20, 20, 20));
	}

	@Test
	public void testAddSphereToGrid() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		
		Double3D loc = new Double3D(5,5,5);
		cgGrid.addSphereToGrid(bc,loc, 1);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
	}
	
	@Test
	public void testAddLineToGrid() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		Double3D loc1 = new Double3D(0,0,0);
		Double3D loc2 = new Double3D(1,1,1);
		StromaEdge se = new StromaEdge(loc1,loc2);
		
		cgGrid.addLineToGrid(se, loc1, loc2, 1);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
	}
	
	@Test
	public void testSetCollisionPoints() {
		fail("not yet implemented");
	}
	
	
	
}
