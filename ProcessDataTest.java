package dataLogger;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sim.util.Double3D;

public class ProcessDataTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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

	
	/**
	 * 
	 * TODO need clockwise and anti-clockwise tests
	 */
	@Test
	public void testTurningAngle() {
		
		Double3D p1 = new Double3D (0,0,0); 
		Double3D p2 = new Double3D (1,0,0); 
		Double3D p3 = new Double3D (1,1,0);
		
		double answer = ProcessData.calculateTurningAngle(p1,p2,p3);
		
		//TODO need to figure out what the expected value for these inputs is
		double expectedValue = 45;
		
		fail("Not yet implemented");
	}

}
