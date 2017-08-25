package dataLogger;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import sim.util.Double3D;
import sim3d.SimulationEnvironment;

public class ProcessDataTest {

	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}
	
	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	
	
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
	
	
	/**
	 * Tests the calculateTurningAngle method
	 */
	@Test
	public void testCalculateMotilityCoefficient() {
		
		double netDisplacement = 100;
		double time = 5;
		double mc = ProcessData.calculateMotilityCoefficient(netDisplacement, time);

		assertEquals(mc,333,1);
	}
	
	
	/**
	 * Tests the calculateTurningAngle method
	 */
	@Test
	public void testCalculateMeanderingIndex() {
		
		double totalDisplacement = 100;
		double netDisplacement = 100;
		double time = 1;
		
		double mi = ProcessData.calculateMeanderingIndex(totalDisplacement,  netDisplacement,  time) ;
		
		assertEquals(mi,1,0.01);
				
		
		double totalDisplacement2 = 100;
		double netDisplacement2 = 50;
		double time2 = 1;
		
		double mi2 = ProcessData.calculateMeanderingIndex(totalDisplacement2, netDisplacement2,time2) ;
		
		assertEquals(mi2,0.5,0.01);
		
	}
	

	@Test
	public void testCalculateSpeed() {
		
		double totalDisplacement = 20;
		double time = 5;
		double speed = ProcessData.calculateSpeed(totalDisplacement, time);
		assertEquals(speed,4,0.01);
		
	}
	
	@Test
	public void testProcessMigrationData() throws IOException{
		
		Integer key = 0;
	
		

		Map<Integer, ArrayList<Double3D>> coords = new HashMap<Integer, ArrayList<Double3D>>();
		
		
		ArrayList<Double3D> d3ds =  new ArrayList<Double3D>();
		
		d3ds.add(new Double3D(1,2,2));
		d3ds.add(new Double3D(5,2,2));
		d3ds.add(new Double3D(5,1,3));
		
		coords.put(0, d3ds);
		coords.put(1, d3ds);
		
		Controller.getInstance().setCoordinates(coords);

		
		double[] output = ProcessData.processMigrationData(key);
		assertNotNull(output);
		
	}
	
	@Test
	public void testProcessRawData() throws IOException{
		Integer key = 1;
		String rawFileName = "/Users/jc1571/Desktop/test.csv";
		FileWriter rawDataWriter = new FileWriter(rawFileName);
		
		Map<Integer, ArrayList<Double3D>> coords = new HashMap<Integer, ArrayList<Double3D>>();
		
		ArrayList<Double3D> d3ds =  new ArrayList<Double3D>();
		
		d3ds.add(new Double3D(1,2,2));
		d3ds.add(new Double3D(5,2,2));
		d3ds.add(new Double3D(5,1,3));
		
		coords.put(0, d3ds);
		coords.put(1, d3ds);
		
		 Map<Integer, ArrayList<Integer>> freereceptors = new HashMap<Integer, ArrayList<Integer>>();

		 ArrayList<Integer> receptors = new ArrayList<Integer>();
		 receptors.add(200);
		 receptors.add(100);
		 receptors.add(300);
		 
		 freereceptors.put(0, receptors);
		 freereceptors.put(1, receptors);
		 
		 Controller.getInstance().setFreereceptors(freereceptors);
		 Controller.getInstance().setSignallingreceptors(freereceptors);
		 Controller.getInstance().setDesensitisedreceptors(freereceptors);
		 Controller.getInstance().setInternalisedreceptors(freereceptors);
		 
		Controller.getInstance().setCoordinates(coords);

		ProcessData.processRawData(key,  rawDataWriter);
		
		boolean fileExists = false;
		File f = new File(rawFileName);
		if(f.exists() ) { 
		    fileExists = true;
		}
		
		assertTrue(fileExists);
	}
	


	@Test
	public void testCalculatePreviousLocation() {

		ArrayList<Double3D> Coords = new ArrayList<Double3D>();
		Coords.add(new Double3D(1,1,1));
		Coords.add(new Double3D(2,2,2));
		Coords.add(new Double3D(3,3,3));
		
		 Double3D d3 = ProcessData.calculatePreviousLocation(2, Coords) ;
		 assertEquals(new Double3D(20,20,20),d3);
	}

	@Test
	public void testCalculateNextLocation() {
		ArrayList<Double3D> Coords = new ArrayList<Double3D>();
		Coords.add(new Double3D(1,1,1));
		Coords.add(new Double3D(2,2,2));
		Coords.add(new Double3D(3,3,3));
		
		 Double3D d3 = ProcessData.calculateNextLocation(1, Coords) ;
		 assertEquals(new Double3D(30,30,30),d3);
		
	}



	
}
