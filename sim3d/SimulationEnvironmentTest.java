package sim3d;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.Matchers.*;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.SimulationEnvironment.CELLTYPE;
import sim3d.cell.BC;
import sim3d.diffusion.Chemokine;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.IO;


public class SimulationEnvironmentTest {

	SimulationEnvironment sim;
	Chemokine m_pParticle;
	
	@Before
	public void setUp() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
		
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment.simulation = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));
		SimulationEnvironment.simulation.setupSimulationParameters();
		
	}

	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
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

	
	@Test
	public void testNetworkGenerationForZeroLengthEdges(){
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		
		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
	
		boolean anyEdgesHaveLengthZero = false;
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof StromaEdge) {
	
					Double3D p1 = ((StromaEdge) stroma.get(i)).getPoint1();
					Double3D p2 = ((StromaEdge) stroma.get(i)).getPoint2();
					
					if(p1.distance(p2) < Settings.DOUBLE3D_PRECISION){	
						System.out.println("stromaedge type: " + 
								((StromaEdge) stroma.get(i)).getStromaedgetype());
						anyEdgesHaveLengthZero = true;
					
					}
			}
		}
	
		assertFalse(anyEdgesHaveLengthZero);
	}
	
	
	
	@Test
	public void testNetworkGenerationForRepeatNodes(){
		
		
		Document parameters;
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		parameters = IO.openXMLFile(paramFile);
		
		SimulationEnvironment.simulation = new SimulationEnvironment(123,parameters);
		SimulationEnvironment.simulation.start();
	
		
		ArrayList<Double3D> locList = new ArrayList<Double3D>();
		
		boolean duplicates = false;
		
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
	
					Stroma sc = (Stroma) stroma.get(i);
				
					Double3D loc = sc.getDrawEnvironment().getObjectLocationAsDouble3D(sc);
							
					if(locList.contains(loc)){ //is the correct comparison here???
						duplicates = true;
					}
					else{
						//add it to the list
						locList.add(loc);
					}
					
			}
		}
		
		assertFalse(duplicates);
	}
	
	
	/**
	 * Test that we can schedule a BCell
	 */
	@Test
	public void testScheduleStoppableCell() {

		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		Document parameters = IO.openXMLFile(paramFile);
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));
		
		
		m_pParticle = new Chemokine(sim.schedule, Chemokine.TYPE.CXCL13,
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
	
		assertEquals(false, SimulationEnvironment.isWithinCircle(10, 10, 5, 5, 1));
		assertEquals(true, SimulationEnvironment.isWithinCircle(5, 5, 5, 5, 1));
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
		SimulationEnvironment.simulation.seedCells(CELLTYPE.B);
		int numofcells = BC.bcEnvironment.allObjects.numObjs;
		assertEquals(10, numofcells);
		SimulationEnvironment.simulation.seedCells(CELLTYPE.cB);
		numofcells = BC.bcEnvironment.allObjects.numObjs;
		assertEquals(11, numofcells);
	}

	
	/**
	 * Test that seed cells adds the correct amount of cells to the grid
	 */
	@Test
	public void testDisplayLevel() {

		m_pParticle = new Chemokine(SimulationEnvironment.simulation.schedule, Chemokine.TYPE.CXCL13,
				31, 31, 31);

		SimulationEnvironment.simulation.setDisplayLevel(10);
		assertEquals(SimulationEnvironment.simulation.getDisplayLevel(),10 );
		
	}
	
	
	
	/**
	 * Test that generateCoordinatesWithinCircle doesn't 
	 * return coordinates outside a circle of radius r
	 */
	@Test
	public void testGenerateCoordinatesWithinCircle() {

		int radius = 13;
		
		Double3D test = SimulationEnvironment.generateCoordinateWithinCircle(radius);

		assertEquals(true, SimulationEnvironment.isWithinCircle((int) test.x, (int) test.y,
				(Settings.WIDTH / 2) + 1, (Settings.HEIGHT / 2) + 1, radius));

	}

	/**
	 * Test that initialiseFDC adds fdcs to the fdcenvironment grid
	 */
	@Test
	public void testInitialiseFDC() {
		String paramFile = "/Users/jc1571/Dropbox/CXCL13Sim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment sim = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));

		
		// due to all the dependecies its easier to run start() than to call
		// just the initialise FDC method
		sim.start();
		
		//assert that the FDC grid is not empty
		assertThat(SimulationEnvironment.getAllStroma().numObjs, greaterThan(0));

		//now assert that instances of StromaEdge and FDC have been placed
		Bag test = SimulationEnvironment.getAllStroma();
		Iterator<?> itr = test.iterator();
		int SEcounter = 0;
		int FDCcounter = 0;
		while(itr.hasNext()) {
		         Object element = itr.next();
		         if(element instanceof StromaEdge){
		        	 SEcounter +=1;
		         }
		         else if(element instanceof Stroma){
		        	 if( ((Stroma) element).getStromatype() == Stroma.TYPE.FDC)
		        	 FDCcounter +=1;
		         }
		      }
		System.out.println(FDCcounter);

		assertTrue(SEcounter > 0);
		assertTrue(FDCcounter > 0);
		
	}



}
