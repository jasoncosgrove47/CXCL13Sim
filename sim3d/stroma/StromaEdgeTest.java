package sim3d.stroma;

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
import sim3d.cell.BC;
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
				1, 1), StromaEdge.TYPE.FDC_edge);
		TransformGroup localTG = se.getModel(se, null);
		assertTrue(localTG instanceof TransformGroup);

	}

	/**
	 * Assert that getCollisionClass returns the correct enum
	 */
	@Test
	public void testGetCollisionClass() {
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1,
				1, 1), StromaEdge.TYPE.FDC_edge);
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
				1, 1), StromaEdge.TYPE.FDC_edge);
		assertTrue(se.getAntigenLevel() == 400);
	}



	/**
	 * Test that registerCollisions updates m_i3lCollsionPoints
	 */
	@Test
	public void testRegisterCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1, 1, 1), StromaEdge.TYPE.FDC_branch);
		se.registerCollisions(cgGrid);
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}
}
