/**
 * 
 */
package dataLogger;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;

import sim3d.util.IO;

/**
 * @author jc1571
 */
public class ControllerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
		Settings.calculateTopologyData = true;
	}
	
	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}

	@Test
	public void testIsPointBetween(){
	
		//generate two points on a line
		Double3D pointa = new Double3D(0,0,0);
		Double3D pointb = new Double3D(10,10,0);
		
		
		//generate our test points
		// to check if they lie between point a and point b
		Double3D pointc = new Double3D(5,5,0); //this point is inside the line
		Double3D pointd = new Double3D(5,1,0); // this point is not on the line
		Double3D pointe = new Double3D(11,11,0); //this point is collinear but not within the bounds
		
		assertTrue(Controller.isPointBetween(pointa,pointb,pointc));
		assertFalse(Controller.isPointBetween(pointa,pointb,pointd));
		assertFalse(Controller.isPointBetween(pointa,pointb,pointe));
	}
	

	/**
	 * TODO is this actually testing anything, there is no assert
	 */
	
	@Test
	public void testForZeroEdgeNodes(){
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		
		SimulationEnvironment.simulation = new SimulationEnvironment(1234,parameters);
		SimulationEnvironment.simulation.start();
				
		double[][] test = Controller.createMatrix(false);

		ArrayList<Integer> numEdgesPerNode = new ArrayList<Integer>();
		
		int numOfZeroEdgeNodes = 0;
		
		//we need to start from two because the 1st row will have a one in it
		for (int i = 2; i<test.length; i++){
			
			int count = 0;
		    for (int j = 2; j<test.length; j++){
		    	if(test[i][j]  == 1){
		    		count +=1;
		    	} 
		    }
		     
		    if(count == 0){
		    	 numOfZeroEdgeNodes +=1;
		    }
		     numEdgesPerNode.add(count);       
		}
	}
	
	
	
	@Test
	public void testNumberOfDegreesPerNode(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		
		SimulationEnvironment.simulation = new SimulationEnvironment(1234,parameters);
		SimulationEnvironment.simulation.start();
		double[][] test = Controller.createMatrix(false);
		
		ArrayList<Integer> numEdgesPerNode = new ArrayList<Integer>();
		
		//we need to start from two because the 1st row will have a one in it
		for (int i = 2; i<test.length; i++){
			 int count = 0;
		     for (int j = 2; j<test.length; j++){
		        
		    	 if(test[i][j]  == 1){
		    		 count +=1;
		    	 } 
		     }
		     numEdgesPerNode.add(count);   
		}
	}
	
	
	
	
	@Test
	public void testGenerateAdjacencyMatrix(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		
		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
		
		double[][] test = Controller.generateAdjacencyMatrix();
		
		int numNodes = SimulationEnvironment.calculateNodesAndEdges()[0];
		assertNotNull(test);
		assertEquals(test.length,numNodes +1);
		
		//there should be no ones in the dataframe at this point
		boolean noUpdates = true;
		
		//we need to start from two because the 1st row will have a one in it
		for (int i = 2; i<test.length; i++){
			 for (int j = 2; j<test.length; j++){
			    if(test[i][j]  == 1){
			    	noUpdates = false;
			    } 
			}
		}
		
		//now make sure there are no 1's in the dataframe
		assertTrue(noUpdates);
	}
	

	
	/**
	 * Test that when the simulation starts experimentFinished is set to false
	 */
	@Test
	public void testExperimentFinished() {
		// upon starting, experimentFinished should be set to false
		assertEquals("experimentFinished should be false", false, SimulationEnvironment.experimentFinished);
	}

	/**
	 * Test that experimentFinished changes to true after experimentLength steps
	 */
	@Test
	public void testExperimentFinishedChanges() {

		Settings.EXPERIMENTLENGTH = 10;
		Controller controller = new Controller();

		// step contoller ExperimentLength times times
		for (int i = 0; i < Settings.EXPERIMENTLENGTH + 1; i++) {
			controller.step(null);
		}

		// check that the experiment finished guard has been updated
		assertEquals("experimentFinished should be false", true, SimulationEnvironment.experimentFinished);
	}
	
	//TODO all of these tests need to be better
	
	@Test
	public void testGetFDCDendritesVisited() {
		Controller controller = new Controller();			
		Map<Integer, Integer> fdcdendritesVisited = controller.getFDCDendritesVisited();
		assertNotNull(fdcdendritesVisited);	
	}
	
	@Test
	public void testGetMRCDendritesVisited() {
		Controller controller = new Controller();
		Map<Integer, Integer> fdcdendritesVisited = controller.getFDCDendritesVisited();
		assertNotNull(fdcdendritesVisited);
	}
	
	@Test
	public void getRcdendritesVisited() {
		Controller controller = new Controller();
		Map<Integer, Integer> fdcdendritesVisited = controller.getFDCDendritesVisited();
		assertNotNull(fdcdendritesVisited);
	}

	@Test
	public void getCoordinates() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Double3D>> coords = controller.getCoordinates();
		assertNotNull(coords);
	}

	@Test
	public void testGetChemokinefield() {
		Controller controller = new Controller();
		double[][] chemokineField = new double[1][1];
		controller.setChemokinefield(chemokineField);
		double[][] cF = Controller.getChemokinefield();
		assertNotNull(cF);
		
	}


	@Test
	public void testGetTotaldendritesVisited() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Double3D>> coords = controller.getCoordinates();
		assertNotNull(coords);
	}


	@Test
	public void testGetCheckpointsReached() {
		Controller controller = new Controller();
		Map<Integer, Integer> checkpoints = new HashMap<Integer, Integer>();
		controller.setCheckpointsReached(checkpoints);	
		assertNotNull(controller);
	}

	@Test
	public void testGetFreereceptors() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Integer>> freereceptors = new HashMap<Integer, ArrayList<Integer>>();
		controller.setFreereceptors(freereceptors);
		assertNotNull(controller.getFreereceptors());
	}


	@Test
	public void testGetSignallingreceptors() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Integer>> freereceptors = new HashMap<Integer, ArrayList<Integer>>();
		controller.setSignallingreceptors(freereceptors);
		assertNotNull(controller.getSignallingreceptors());
	}


	@Test
	public void testGetDesensitisedreceptors() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Integer>> freereceptors = new HashMap<Integer, ArrayList<Integer>>();
		controller.setDesensitisedreceptors(freereceptors);
		assertNotNull(controller.getDesensitisedreceptors());
	}


	@Test
	public void testGetInternalisedreceptors() {
		Controller controller = new Controller();
		Map<Integer, ArrayList<Integer>> freereceptors = new HashMap<Integer, ArrayList<Integer>>();
		controller.setInternalisedreceptors(freereceptors);
		assertNotNull(controller.getInternalisedreceptors());
	}




}
