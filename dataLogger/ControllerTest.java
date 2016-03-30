/**
 * 
 */
package dataLogger;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sim3d.Settings;
import sim3d.SimulationEnvironment;

/**
 * @author jc1571
 */
public class ControllerTest {

	/**
	 * Test that when the simulation starts, experimentFinished is set to false
	 */
	@Test
	public void testExperimentFinished() {
	
		assertEquals("experimentFinished should be false", false, SimulationEnvironment.experimentFinished);
	}
	
	
	

	
	
	/**
	 * Test that experimentFinished changes to true after experimentLength steps
	 */
	@Test
	public void testExperimentFinishedChanges() {
		
		
		Settings.EXPERIMENTLENGTH = 10;
		Controller controller = new Controller();
		
		for(int i =0; i < 11; i++){
			controller.step(null);
		}
		
		assertEquals("experimentFinished should be false", true, SimulationEnvironment.experimentFinished);
	}

}
