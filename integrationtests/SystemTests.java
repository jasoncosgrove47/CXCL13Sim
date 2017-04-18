/**
 * 
 */
package integrationtests;

import static org.junit.Assert.*;
import org.junit.Test;
import sim.util.Bag;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.cell.cognateBC;
import sim3d.cell.cognateBC.TYPE;
import sim3d.stroma.Stroma;
import sim3d.util.IO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author jason cosgrove
 */
public class SystemTests {

	/*
	 * Make sure that B cells can become primed
	 */
	@Test
	public void testShouldAcquireAntigen() { 
		// set up the simulation
		
		long steps = 0;
		long seed = System.currentTimeMillis();
		SimulationEnvironment.simulation = null;
		SimulationEnvironment.simulation= new SimulationEnvironment(
				seed,
				IO.openXMLFile("/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml"));

		// set the appropriate parameters
		Settings.HEIGHT = 40;
		Settings.WIDTH = 40;
		Settings.DEPTH = 10;
		//Settings.BC.COGNATECOUNT = 100;
	
		//Settings.BC.COUNT = 0;
		Settings.BC.COGNATECOUNT = 100;
		SimulationEnvironment.steadyStateReached = true;
		//Settings.EXPERIMENTLENGTH = 400;
		SimulationEnvironment.simulation.start();
		
		// run the simulation for 400 steps
		do {
			steps = SimulationEnvironment.simulation.schedule.getSteps();
			if (!SimulationEnvironment.simulation.schedule.step(SimulationEnvironment.simulation))
				break;
		} while (steps < 400);

		// get all cognate B-cells
		Bag cells = BC.bcEnvironment.allObjects;

		// counter for the number of primed cells
		int primedCount = 0;

		// count the number of primed b cells
		for (int i = 0; i < cells.size(); i++) {
			
			if(cells.get(i) instanceof cognateBC){
				cognateBC cBC = (cognateBC) cells.get(i);

				if (cBC.type == TYPE.PRIMED) {
					primedCount += 1;
				}	
			}
		}

		// assert that at least 20 of the cells have been primed
		assertThat(primedCount, greaterThan(20));

		// finish the simulation
		SimulationEnvironment.simulation.finish();
	}


	@Test
	public void testNetworkConnections() {
		long steps = 0;
		long seed = System.currentTimeMillis();
		SimulationEnvironment.simulation = null;
		SimulationEnvironment.simulation= new SimulationEnvironment(
				seed,
				IO.openXMLFile("/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml"));

		// set the appropriate parameters
		Settings.HEIGHT = 40;
		Settings.WIDTH = 40;
		Settings.DEPTH = 10;
		//Settings.BC.COGNATECOUNT = 100;
	
		//Settings.BC.COUNT = 0;
		Settings.BC.COGNATECOUNT = 100;
		SimulationEnvironment.steadyStateReached = true;
		//Settings.EXPERIMENTLENGTH = 400;
		SimulationEnvironment.simulation.start();
		
		// run the simulation for 400 steps
		do {
			steps = SimulationEnvironment.simulation.schedule.getSteps();
			if (!SimulationEnvironment.simulation.schedule.step(SimulationEnvironment.simulation))
				break;
		} while (steps < 10);
		
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		
		boolean differentSubtypesConnected = false;
		for(int i=0; i < stroma.size(); i ++){
			if(stroma.get(i) instanceof Stroma){
				Stroma sc = (Stroma) stroma.get(i);
				
				if(sc.getStromatype() == Stroma.TYPE.MRC){
					for(Stroma node : sc.getM_Nodes()){
						if(node.getStromatype() != sc.getStromatype()){
							
							System.out.println(node.getStromatype());
							

						}
					}
				}
			}
		}
		
		assertTrue(differentSubtypesConnected);
		//assertTrue(MatrixCorrect);
		
	}
	
}
