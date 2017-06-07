package sim3d.cell;


import static org.junit.Assert.*;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.StromaEdge;
import dataLogger.Controller;
import ec.util.MersenneTwisterFast;


public class cognateBCTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
	
		
	}

	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	/**
	 * Assert that a cognate BC can update its position with
	 * the controller
	 */
	@Test
	public void testUpdatePosition() {
		
		BC.bcEnvironment = new Continuous3D(5, 5, 5,5);
		BC.drawEnvironment = BC.bcEnvironment;
		cognateBC cBC = new cognateBC(1);
		cBC.setObjectLocation(new Double3D(1,1,1));
		
		cBC.updatePosition();
		boolean test = Controller.getInstance().getCoordinates().isEmpty();
		assertEquals(false, test);
	}
	
	/**
	 * Assert that getModel returns an instance of TransformGroup
	 */
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		cognateBC cBC = new cognateBC(1);
		TransformGroup localTG = cBC.getModel(cBC,null);
		assertTrue(localTG instanceof TransformGroup);
	
	}
	
	
	/**
	 * Test that a cognate Bcell can acquire antigen
	 * and can update dendritesVisited in controller
	 */
	@Test
	public void testAcquireAntigen() {
		
		cognateBC cBC = new cognateBC(1);
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		
		StromaEdge se = new StromaEdge(new Double3D(0,0,0), new Double3D(1,1,1),StromaEdge.TYPE.FDC_edge);
		cBC.acquireAntigenEdge(se);
		
		int test = Controller.getInstance().getFDCDendritesVisited().get(cBC.getIndex());
		assertEquals(1, test);
		
		//test that the cBC can only acquire antigen from a unique stromal cell once
		cBC.acquireAntigenEdge(se);
		int test2 = Controller.getInstance().getFDCDendritesVisited().get(cBC.getIndex());
		assertEquals(1, test2);
		
		//test the cBC has captured antigen and is now primed
		assertEquals(cBC.getAntigenCaptured(),1);
		assertEquals(cBC.type, TYPE.PRIMED);
	}

	
	/**
	 * Test that getIndex returns the correct index for a given cell
	 */
	@Test
	public void testIndex() {
		BC.bcEnvironment = new Continuous3D(5, 5, 5,5);
		BC.drawEnvironment = BC.bcEnvironment;
		
		cognateBC cBC = new cognateBC(123);
		int test = cBC.getIndex();
		assertEquals(test, 123);
		
		
	}
	
}
