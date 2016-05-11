/**
 * 
 */
package integrationtests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.MutableDouble3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.cell.StromaEdge;
import sim3d.cell.cognateBC;
import sim3d.cell.cognateBC.TYPE;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
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
		SimulationEnvironment sim = new SimulationEnvironment(
				seed,
				IO.openXMLFile("/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"));

		// set the appropriate parameters
		Settings.BC.COUNT = 0;
		Settings.BC.COGNATECOUNT = 100;
		SimulationEnvironment.steadyStateReached = true;
		Settings.EXPERIMENTLENGTH = 400;
		sim.start();

		// run the simulation for 400 steps
		do {
			steps = sim.schedule.getSteps();
			if (!sim.schedule.step(sim))
				break;
		} while (steps < 400);

		// get all cognate B-cells
		Bag cells = BC.bcEnvironment.allObjects;

		// counter for the number of primed cells
		int primedCount = 0;

		// count the number of primed b cells
		for (int i = 0; i < cells.size(); i++) {
			cognateBC cBC = (cognateBC) cells.get(i);

			if (cBC.type == TYPE.PRIMED) {
				primedCount += 1;
			}
		}

		// assert that at least 20 of the cells have been primed
		assertThat(primedCount, greaterThan(20));

		// finish the simulation
		sim.finish();
	}

}
