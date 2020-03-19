package sim3d.stroma;

import static org.junit.Assert.*;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.Test;

import sim.util.Bag;
import sim.util.Double3D;

import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.util.IO;

public class StromaEdgeTest {

	
	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	/**
	 * We need to make sure that all stromaEdges location is equal to point 1
	 * and also need to check that point 2 corresponds to p1 + edge vector,
	 * would also like to check that the midpoint is correct
	 */
	@Test
	public void testSetObjectLocation() {

		// i think this is worth testing as an integratino test
		long steps = 0;
		long seed = System.currentTimeMillis();
		SimulationEnvironment.simulation = new SimulationEnvironment(seed,
				IO.openXMLFile("/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml"));

		// set the appropriate parameters
		Settings.BC.COUNT = 0;
		Settings.BC.COGNATECOUNT = 100;
		SimulationEnvironment.steadyStateReached = true;
		Settings.EXPERIMENTLENGTH = 400;
		SimulationEnvironment.simulation.start();

		// run the simulation for 400 steps
		do {
			steps = SimulationEnvironment.simulation.schedule.getSteps();
			if (!SimulationEnvironment.simulation.schedule.step(SimulationEnvironment.simulation))
				break;
		} while (steps < 10);

		boolean correctLocation = true;
		Bag stroma = SimulationEnvironment.getAllStroma();

		// check that for each stromaedge the location is
		// set to point 1
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {

				StromaEdge se = (StromaEdge) stroma.get(i);

				// if they arent in the same place
				if (se.getPoint1().distance(se.getM_Location()) > Settings.DOUBLE3D_PRECISION) {// if

					// update to false
					correctLocation = false;
					break;
				}
			}
		}
		assertTrue(correctLocation);
	}

	/**
	 * Test that getModel returns a TransformGroup object
	 */
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1, 1, 1), StromaEdge.TYPE.FDC_edge);
		TransformGroup localTG = se.getModel(se, null);
		assertTrue(localTG instanceof TransformGroup);
	}

	/**
	 * Assert that getCollisionClass returns the correct enum
	 */
	@Test
	public void testGetCollisionClass() {
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1, 1, 1), StromaEdge.TYPE.FDC_edge);
		assertEquals(se.getCollisionClass(), CLASS.STROMA_EDGE);
	}

	/**
	 * Test that the edge has the correct amount of antigen at the start of a
	 * simulation
	 */
	@Test
	public void testGetAntigen() {
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1, 1, 1), StromaEdge.TYPE.FDC_edge);
		assertTrue(se.getAntigenLevel() == 400);
	}

	/**
	 * Test that registerCollisions updates m_i3lCollsionPoints
	 */
	@Test
	public void testRegisterCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

		StromaEdge se = new StromaEdge(new Double3D(0, 0, 0), new Double3D(1, 1, 1), StromaEdge.TYPE.FDC_edge);
		se.registerCollisions(cgGrid);
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}
}
