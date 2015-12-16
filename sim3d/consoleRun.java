package sim3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.IO;
import sim3d.util.Vector3DHelper;
import sim3d.util.FRCStromaGenerator.FRCCell;

/**
 * Sets up and runs the simulation
 * 
 * Need to ensure that Java has access to enough memory resources
 * go to run configurations and pass in -Xmx3000m
 * 
 * @author Jason Cosgrove  - {@link jc1571@york.ac.uk}
 */


public class consoleRun 
{
	
	private static final long serialVersionUID = 1;
	
	/**
	 * main method
	 */
	public static void main( String[] args )
	{
		int seed= (int) (System.currentTimeMillis());	
		String paramFile = args[0];		// set the seed for the simulation, be careful for when running on cluster																	
		SimulationEnvironment simulation = new SimulationEnvironment(seed,IO.openXMLFile(paramFile));								// instantiate the simulation

		long steps = 0;
		simulation.start();
		
		System.out.println("StromaSim v1.0 - Console Version (No Visualisation)");
		System.out.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");
		
		do
		{
			steps = simulation.schedule.getSteps();		
			System.out.println("Steps: " + steps);
			if (!simulation.schedule.step(simulation))
			break;
			
		}while(steps < 100);	
		
		simulation.finish();												// finish the simulation
		System.out.println("\nSimulation completed successfully!\n\n");
		System.exit(0);														// exit the simulation
		
		//doLoop( Demo.class, args );//change this to run for a fixed amount of timesteps
		//System.exit( 0 );
	}
	
	
	
	
}
