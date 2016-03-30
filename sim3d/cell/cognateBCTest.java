package sim3d.cell;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import dataLogger.Controller;
import ec.util.MersenneTwisterFast;

public class cognateBCTest {

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
	public void testUpdatePosition() {
		
		BC.bcEnvironment = new Continuous3D(5, 5, 5,5);
		BC.drawEnvironment = BC.bcEnvironment;
		
		cognateBC cBC = new cognateBC(1);
		cBC.setObjectLocation(new Double3D(1,1,1));
		cBC.updatePosition();
		boolean test = Controller.getInstance().getX_Coordinates().isEmpty();
		assertEquals(false, test);
	}
	
	
	@Test
	public void testGetModel() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;

	
		cognateBC cBC = new cognateBC(1);
		TransformGroup localTG = cBC.getModel(cBC,null);
	
		assertNotNull(localTG);
	
	}
	
	
	/**
	 * TODO not quite sure what best 
	 * unit test for this one is 
	 */
	@Test
	public void testAcquireAntigen() {
		
		cognateBC cBC = new cognateBC(1);
		Settings.FDC.STARTINGANTIGENLEVEL = 400;
		
		StromaEdge se = new StromaEdge(new Double3D(0,0,0), new Double3D(1,1,1));
		cBC.acquireAntigen(se);
		
		int test = Controller.getInstance().getDendritesVisited().get(cBC.getIndex());
		assertEquals(1, test);
	}

	@Test
	public void testIndex() {
		BC.bcEnvironment = new Continuous3D(5, 5, 5,5);
		BC.drawEnvironment = BC.bcEnvironment;
		
		cognateBC cBC = new cognateBC(1);
		
		int test = cBC.getIndex();
		
		assertEquals(test, 1);
		
		
	}
	
}
