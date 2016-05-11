package sim3d;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.Matchers.*;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.SimulationEnvironment.CELLTYPE;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.cell.StromaEdge;
import sim3d.cell.branch;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.util.IO;


public class SimulationEnvironmentTest {

	SimulationEnvironment sim;
	
	@Before
	public void setUp() throws Exception {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));
		sim.setupSimulationParameters();
	}

	/*
	 * test that the parameters are read in by asserting that
	 * the depth of the grid is greater than zero
	 * 
	 * A better test for this would be to put 
	 * in a specific parameter check that its correct 
	 * then redo with a different value
	 * 
	 */
	@Test
	public void testSetUpSimulationParameters() {
		// see if the depth of the simulation has been set
		assertThat(Settings.DEPTH, greaterThan(0));
	}

	/**
	 * Test that we can schedule a BCell
	 */
	@Test
	public void testScheduleStoppableCell() {

		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		Document parameters = IO.openXMLFile(paramFile);
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));
		
		Chemokine m_pParticle = new Chemokine(sim.schedule, Chemokine.TYPE.CXCL13,
				31, 31, 31);

		BC.bcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);
		BC.drawEnvironment = BC.bcEnvironment;
		BC bc = new BC();
		
		bc.setObjectLocation(new Double3D(
					Settings.RNG.nextInt(14) + 8, Settings.RNG.nextInt(14) + 8,
					Settings.RNG.nextInt(14) + 8));

		// schedule the agent then run the stop method
		sim.scheduleStoppableCell(bc);

		//this will throw an error if anything is wrong with the method
		try { bc.step(null);}
		catch (Exception e) {
		     fail("Exception " + e);
		}
	}

	/**
	 * Test whether coordinates are within a circle of radius r
	 */
	@Test
	public void testIsWithinCircle() {
	
		assertEquals(false, sim.isWithinCircle(10, 10, 5, 5, 1));
		assertEquals(true, sim.isWithinCircle(5, 5, 5, 5, 1));
	}

	/**
	 * Test that seed cells adds the correct amount of cells to the grid
	 */
	@Test
	public void testSeedCells() {

		Settings.BC.COUNT = 10;
		Settings.BC.COGNATECOUNT = 1;
		
		// Initialise the B cell grid
		BC.bcEnvironment = new Continuous3D(5, 10, 10, 10);
		BC.drawEnvironment = BC.bcEnvironment;
		sim.seedCells(CELLTYPE.B);
		int numofcells = BC.bcEnvironment.allObjects.numObjs;
		assertEquals(10, numofcells);
		sim.seedCells(CELLTYPE.cB);
		numofcells = BC.bcEnvironment.allObjects.numObjs;
		assertEquals(11, numofcells);
	}

	
	/**
	 * Test that seed cells adds the correct amount of cells to the grid
	 */
	@Test
	public void testDisplayLevel() {

		Chemokine m_pParticle = new Chemokine(sim.schedule, Chemokine.TYPE.CXCL13,
				31, 31, 31);

		sim.setDisplayLevel(10);
		assertEquals(sim.getDisplayLevel(),10 );
		
	}
	
	
	/**
	 * Test that generateCoordinatesWithinCircle doesn't 
	 * return coordinates outside a circle of radius r
	 */
	@Test
	public void testGenerateCoordinatesWithinCircle() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));

		Double3D test = sim.generateCoordinateWithinCircle();

		assertEquals(true, sim.isWithinCircle((int) test.x, (int) test.y,
				(Settings.WIDTH / 2) + 1, (Settings.HEIGHT / 2) + 1, 13));

	}

	/**
	 * Test that initialiseFDC adds fdcs to the fdcenvironment grid
	 */
	@Test
	public void testInitialiseFDC() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));

		// due to all the dependecies its easier to run start() than to call
		// just the initialise FDC method
		sim.start();
		
		//assert that the FDC grid is not empty
		assertThat(sim.fdcEnvironment.allObjects.numObjs, greaterThan(0));

		//now assert that instances of StromaEdge and FDC have been placed
		Bag test = sim.fdcEnvironment.getAllObjects();
		Iterator itr = test.iterator();
		int SEcounter = 0;
		int FDCcounter = 0;
		while(itr.hasNext()) {
		         Object element = itr.next();
		         if(element instanceof StromaEdge){
		        	 SEcounter +=1;
		         }
		         else if(element instanceof FDC){
		        	 FDCcounter +=1;
		         }
		      }
		assertTrue(SEcounter > 0);
		assertTrue(FDCcounter > 0);
		
	}

	/**
	 * Test that we can add branches to branches, to provide a more web like
	 * morphology for FDCs
	 */
	@Test
	public void testAddBranchesToBranches() {
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));

		// due to all the dependecies its easier to run start() than to call
		// just the initialise FDC method
		sim.start();

		int counter = 0;
		Bag Stroma = sim.fdcEnvironment.getAllObjects();
		for (int i = 0; i < Stroma.size(); i++) {
			if (Stroma.get(i) instanceof branch) {
				counter += 1;
			}
		}
		assertThat(counter, greaterThan(0));
	}

}
