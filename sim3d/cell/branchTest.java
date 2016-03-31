package sim3d.cell;

import static org.junit.Assert.*;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;

public class branchTest {

	static branch b;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		b = new branch(new Double3D(0,0,0), new Double3D(1,1,1));
	}

	/**
	 * Assert that getCollisionClass returns the correct enum
	 */
	@Test
	public void testGetCollisionClass() {
		assertEquals(b.getCollisionClass() ,CLASS.BRANCH); 
	}
	
	/**
	 * Assert that a branch has the correct amount of antigen
	 * at the start of a simulation
	 */
	@Test
	public void testAntigenLevel() {
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		branch c = new branch(new Double3D(0,0,0), new Double3D(1,1,1));
		assertThat(c.getAntigen(), equalTo(200));
	}

	/**
	 * Assert that register collisions adds data to the 
	 * getM_i3lCollisionPoints <Int3D> List.
	 */
	@Test
	public void testRegisterCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		branch c = new branch(new Double3D(0,0,0), new Double3D(1,1,1));
		c.registerCollisions(cgGrid);
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}

	/**
	 * Assert that getModel returns a TransformGroup object. 
	 */
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		branch c = new branch(new Double3D(0,0,0), new Double3D(1,1,1));
		TransformGroup localTG = c.getModel(c,null);
		assertTrue(localTG instanceof TransformGroup);
	
	}
	
}
