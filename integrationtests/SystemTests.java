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
import sim3d.diffusion.Particle;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
/**
 * @author jason cosgrove
 * 		
 */
public class SystemTests
{
	
    
    /*
	 * Make sure that B cells can become primed
	 */
    @Test
	public void testShouldAcquireAntigen()
	{
    	long steps = 0;
    	long seed = System.currentTimeMillis();
    	SimulationEnvironment sim = new SimulationEnvironment(seed,IO.openXMLFile("/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"));
    	Settings.BC.COUNT=0;
    	Settings.BC.COGNATECOUNT=100;
    	
    	sim.start();
    	//sim.seedCognateCells(100);
    	
		do
		{	
			steps = sim.schedule.getSteps();		
			if (!sim.schedule.step(sim))
			break;	
		}while(steps < 300);	
		
		Bag cells =  sim.bcEnvironment.allObjects;
		
		int primedCount = 0;
		TYPE[] activationStatus = new TYPE[100];
		for(int i = 0; i < cells.size(); i++)
		{
			cognateBC cBC = (cognateBC) cells.get(i);
			activationStatus[i] = cBC.type;
			if(activationStatus[i]== TYPE.PRIMED)
			{
				primedCount +=1;
			}
		}
		
		//again not sure what these are doing
		assertThat(primedCount, greaterThan(20));

		// finish the simulation
		sim.finish();
	}

}
