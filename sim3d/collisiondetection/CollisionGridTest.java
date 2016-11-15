package sim3d.collisiondetection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.stroma.StromaEdge;

public class CollisionGridTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		
	}

	/**
	 * Test that add collisionpotential updates m_i3CollisionPoints
	 */
	@Test
	public void testAddCollisionPotential() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		cgGrid.addCollisionPotential(1, 1, 1, bc);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
		Int3D loc = new Int3D(1,1,1);
		assertTrue(cgGrid.getM_i3lCollisionPoints().contains(loc));
	}
	
	/**
	 * Test that getPoints returns the correct collidable
	 */
	@Test
	public void testGetPoints() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		cgGrid.addCollisionPotential(1, 1, 1, bc);
		
		Int3D loc = new Int3D(1,1,1);
		assertEquals(false, cgGrid.getPoints(loc).isEmpty());
		assertTrue( cgGrid.getPoints(loc).contains(bc));
	}
	
	/**
	 * Test that boxsphereintersect returns the appropriate boolean
	 * 
	 */
	@Test
	public void testBoxSphereIntersect() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		assertEquals(true,cgGrid.BoxSphereIntersect(5, 5, 5, 3, 5, 5, 5));
		assertEquals(false,cgGrid.BoxSphereIntersect(5, 5, 5, 3, 20, 20, 20));
		assertEquals(false,cgGrid.BoxSphereIntersect(5, 5, 5, 3, 3, 3, 3));
		
	}

	/**
	 * Test that addSphereToGrid adds a BC to the collision
	 * grid
	 */
	@Test
	public void testAddSphereToGrid() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC bc = new BC();
		
		Double3D loc = new Double3D(5,5,5);
		cgGrid.addSphereToGrid(bc,loc, 1);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
		assertTrue(cgGrid.getM_i3lCollisionPoints().contains(loc));
	}
	
	/**
	 * Test that addLineToGrid adds a stromaedge to the
	 * collisionGrid
	 */
	@Test
	public void testAddLineToGrid() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		Double3D loc1 = new Double3D(0,0,0);
		Double3D loc2 = new Double3D(1,1,1);
		StromaEdge se = new StromaEdge(loc1,loc2,StromaEdge.TYPE.FDC_edge);
		
		cgGrid.addLineToGrid(se, loc1, loc2, 1);
		assertEquals(false,cgGrid.getM_i3lCollisionPoints().isEmpty());
		assertTrue(cgGrid.getM_i3lCollisionPoints().contains(loc1));
	}
	
	
	/**
	 * Assert that setCollisionPoints adds points to the 
	 * collision points int3D list
	 */
	@Test
	public void testSetCollisionPoints() {
		
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		Int3D test = new Int3D(0,0,0);
		List<Int3D> testm_i3lCollisionPoints = new ArrayList<Int3D>();
		testm_i3lCollisionPoints.add(test);
		
		
		cgGrid.setM_i3lCollisionPoints(testm_i3lCollisionPoints);
		assertTrue(cgGrid.getM_i3lCollisionPoints().contains(test));
		
	}
	
	
	
}
