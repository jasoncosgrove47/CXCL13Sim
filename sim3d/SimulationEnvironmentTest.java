package sim3d;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.SimulationEnvironment.CELLTYPE;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.cell.StromaEdge;
import sim3d.cell.branch;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.util.IO;

public class SimulationEnvironmentTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		//start the simulation and run it for 20 steps and
		//make sure that all of the grid are set up and
		// that the agents are on teh schedule etc
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetUpSimulationParameters() {
		
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		sim.setupSimulationParameters();
		//see if the depth of the simulation has been set
		assertThat(Settings.DEPTH, greaterThan(0));
	
	}
		
	@Test
	public void testScheduleStoppableCell() {
		
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		//schedule the agent then run the stop method
		BC bc = new BC();
		sim.scheduleStoppableCell(bc);
		bc.stop();
	}
	
	@Test
	public void testIsWithinCircle() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		assertEquals(false,sim.isWithinCircle(10, 10, 5, 5, 1));
		assertEquals(true,sim.isWithinCircle(5, 5, 5, 5, 1));
	}

	@Test
	public void testSeedCells() {
		
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		Settings.BC.COUNT = 10;
		Settings.BC.COGNATECOUNT = 0;
		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(5,10, 10, 10);
		BC.drawEnvironment = BC.bcEnvironment;
		sim.seedCells(CELLTYPE.B);
		int numofcells = BC.bcEnvironment.allObjects.numObjs;
		assertEquals(10,numofcells);
	}

	@Test
	public void testGenerateCoordinatesWithinCircle() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		Double3D test = sim.generateCoordinateWithinCircle();
	
		assertEquals(true,sim.isWithinCircle((int) test.x,(int) test.y, (Settings.WIDTH / 2) + 1,
				(Settings.HEIGHT / 2) + 1, 13)) ;
		
	}
	
	@Test
	public void testInitialiseFDC() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		//due to all the dependecies its easier to run start() than to call
		// just the initialise FDC method
		sim.start();
		assertThat(sim.fdcEnvironment.allObjects.numObjs, greaterThan(0));
		
	}
	
	@Test
	public void testAddBranchesToBranches() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
		SimulationEnvironment sim = new SimulationEnvironment(0, IO.openXMLFile(paramFile));
		
		//due to all the dependecies its easier to run start() than to call
		// just the initialise FDC method
		sim.start();

		int counter = 0;
		Bag Stroma = sim.fdcEnvironment.getAllObjects();
		for(int i=0; i <Stroma.size();i++)
		{
			if(Stroma.get(i) instanceof branch)
			{
				counter +=1;
			}
		}
		assertThat(counter, greaterThan(0));
	}



	
}
