package sim3d.cell;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;

public class StromaEdgeTest {

	/**
	 * Test that getModel returns a TransformGroup object
	 */
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1));
		TransformGroup localTG = se.getModel(se, null);
		assertTrue(localTG instanceof TransformGroup);

	}

	/**
	 * Assert that getCollisionClass returns the correct enum
	 */
	@Test
	public void testGetCollisionClass() {
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1));
		assertEquals(se.getCollisionClass(), CLASS.STROMA_EDGE);
	}

	/**
	 * Test that the edge has the correct amount of antigen at the start of a
	 * simulation
	 */
	@Test
	public void testGetAntigen() {
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1));
		assertTrue(se.getAntigen() == 400);
	}

	/**
	 * Test that the edge has the correct amount of antigen at the start of a
	 * simulation
	 */
	@Test
	public void testGetAntigenLowerandUpperHalf() {
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1));
		assertTrue(se.getAntigenLevelLowerEdge() == 200);
		assertTrue(se.getAntigenLevelUpperEdge() == 200);
	}

	/**
	 * Test that registerCollisions updates m_i3lCollsionPoints
	 */
	@Test
	public void testRegisterCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		StromaEdge se = new branch(new Double3D(0, 0, 0), new Double3D(1, 1, 1));
		se.registerCollisions(cgGrid);
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}
}
