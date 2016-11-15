package dataLogger;

import static org.junit.Assert.*;


import org.junit.Test;
import sim.util.Double3D;
public class ProcessDataTest {



	
	/**
	 * Tests the calculateTurningAngle method
	 */
	@Test
	public void testTurningAngle() {
		
		//test the anti-clockwise turn
		Double3D p1 = new Double3D (0,0,0); 
		Double3D p2 = new Double3D (1,0,0); 
		Double3D p3 = new Double3D (1,-1,0);
		
		double answer = ProcessData.calculateTurningAngle(p1,p2,p3);
		double expectedValue = 90;
		assertEquals(expectedValue, answer,0.1);
		
		
		
		//test the clockwise turn
		Double3D p4 = new Double3D (0,0,0); 
		Double3D p5 = new Double3D (1,0,0); 
		Double3D p6 = new Double3D (1,1,0);
		
		double answer2 = ProcessData.calculateTurningAngle(p4,p5,p6);
		double expectedValue2 = -90;
		assertEquals(expectedValue2, answer2,0.1);
		

	}

}
