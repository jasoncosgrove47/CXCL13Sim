/**
 * 
 */
package dataLogger;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;

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

		Settings.calculateTopologyData = true;
		
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
	
	/*
	@Test
	public void testCheckIsConnected(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		

		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
		
		
		int[][] test = Controller.generateAdjacencyMatrix();
		
		
		ArrayList<Double3D> fdclocations = new ArrayList<Double3D>();
		for (nodeInfo temp : Controller.getNodeinformation()) {

			if (temp.getM_type() == Stroma.TYPE.FDC) {
				fdclocations.add(temp.getM_loc());
			}
		}
		

		//pick two random nodes, make sure there is no node between them
		// then place a node between them and recalculate?
		
		// draw a line between each node with no repetitions
		for (int i = 0; i < fdclocations.size(); i++) {
			for (int j = 0; j < fdclocations.size(); j++) {

				System.out.println("i: " + i);
				System.out.println("j: " + j);
				
				if (!fdclocations.get(i).equals(j)) {

					// obtain the coordinates and query the MAP
					// for the associated indices required for updating
					// the adjacency matrix
					Double3D p1 = fdclocations.get(i);
					Double3D p2 = fdclocations.get(j);

					int index1 = Controller.NodeIndex.get(p1);
					int index2 = Controller.NodeIndex.get(p2);
					
					Controller.checkIfConnected(test, fdclocations, i, j, index1, index2);
				}
			}
		}
		

	}
	*/
	
	@Test
	public void testForZeroEdgeNodes(){
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		

		SimulationEnvironment.simulation = new SimulationEnvironment(1234,parameters);
		SimulationEnvironment.simulation.start();
		
		
		
		double[][] test = Controller.generateAdjacencyMatrix();
		test = Controller.updateAdjacencyMatrix(test);
		//test = Controller.updateAdjacencyMatrixForFDCs(test);
		
		ArrayList<Integer> numEdgesPerNode = new ArrayList<Integer>();
		
		int numOfZeroEdgeNodes = 0;
		
		//we need to start from two because the 1st row will have a one in it
		for (int i = 2; i<test.length; i++){
			
			 int count = 0;
		     for (int j = 2; j<test.length; j++){
		        
		    	 System.out.println("i: " + i + " j: " + j);
		    	 
		    	 if(test[i][j]  == 1){
		    		 count +=1;
		    	 } 
		     }
		     
		     if(count == 0){
		    	 numOfZeroEdgeNodes +=1;
		     }
		     
		     //no count value should be less than zero
		     //assertTrue(count > 0);
		     numEdgesPerNode.add(count);
		        
		}
		System.out.println("number of zero edge nodes: " + numOfZeroEdgeNodes);
		
	}
	
	
	
	@Test
	public void testNumberOfDegreesPerNode(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		

		SimulationEnvironment.simulation = new SimulationEnvironment(1234,parameters);
		SimulationEnvironment.simulation.start();
		
		
		double[][] test = Controller.generateAdjacencyMatrix();
		test = Controller.updateAdjacencyMatrix(test);
		//test = Controller.updateAdjacencyMatrixForFDCs(test);
		
		ArrayList<Integer> numEdgesPerNode = new ArrayList<Integer>();
		
		//we need to start from two because the 1st row will have a one in it
		for (int i = 2; i<test.length; i++){
			
			 int count = 0;
		     for (int j = 2; j<test.length; j++){
		        
		    	 System.out.println("i: " + i + " j: " + j);
		    	 
		    	 if(test[i][j]  == 1){
		    		 count +=1;
		    	 } 
		     }
		     
		     numEdgesPerNode.add(count);
		     
		     
		}
		//what is the biggest degree value in the network
		System.out.println("max number of connections: " + Collections.max(numEdgesPerNode));
		System.out.println("min number of connections: " + Collections.min(numEdgesPerNode));
	}
	
	
	
	
	@Test
	public void testGenerateAdjacencyMatrix(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
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
			        
			    	 System.out.println("i: " + i + " j: " + j);
			    	 
			    	 if(test[i][j]  == 1){
			    		 noUpdates = false;
			    	 } 
			     }
			}
		
		
		//now make sure there are no 1's in the dataframe
		assertTrue(noUpdates);
	}
	
	
	/**
	 * This is a very naive test just looking to see if we've added 1's
	 * we'll need to revisit this at some point. 
	 */
	/*
	@Test
	public void testUpdateAdjacencyMatrixWithRCs(){
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		

		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
		
		int[][] test = Controller.generateAdjacencyMatrix();
		test = Controller.updateAdjacencyMatrix(test);
		
		//Quite a crude test, would want to check something more specific
		///are there any 1s in the matrix?
		boolean noUpdates = true;
		
	
		for (int i = 0; i<test.length; i++){
			for (int j = 0; j<test.length; j++){
			    if(test[i][j]  == 1){
			    	noUpdates = false;
			    	break;
			     }  	 
			   }
			}
	
		assertFalse(noUpdates);
	}
	
	@Test
	public void testUpdateAdjacencyMatrixWithFDCs(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		

		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
		
		
		int[][] test = Controller.generateAdjacencyMatrix();
		
		//Quite a crude test, would want to check something more specific
		///are there any 1s in the matrix?
		boolean noUpdates = true;
		
		int counter = 0;
		
		//need a counter otherwise we end up in an infinite loop
		while(noUpdates || counter < test.length){
			for (int i = 0; i<test.length; i++){
			     for (int j = 0; j<test.length; j++){
			        
			    	 if(test[i][j]  == 1){
			    		 noUpdates = false;

			    	 } 
			    	 
			    	 counter ++;
			     }
			}
		}

		
		assertFalse(noUpdates);
		
	}
	*/
	
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

}
