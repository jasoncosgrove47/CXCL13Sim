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
 *
 */
public class ControllerTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testExperimentFinished() {
	
		assertEquals("experimentFinished should be false", false, SimulationEnvironment.experimentFinished);
	}
	
	@Test
	public void testExperimentFinishedChanges() {
		
		
		Settings.EXPERIMENTLENGTH = 10;
		Controller controller = new Controller();
		
		for(int i =0; i < 12; i++){
			controller.step();
		}
		
		assertEquals("experimentFinished should be false", true, SimulationEnvironment.experimentFinished);
	}

}
