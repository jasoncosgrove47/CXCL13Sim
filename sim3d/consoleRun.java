package sim3d;

import java.awt.Color;
import java.sql.Date;
import java.text.SimpleDateFormat;
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
 * Sets up and runs the simulation without a GUI
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
		// output the start time
		long starttime = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");    
		Date formattedstarttime = new Date(starttime);
		System.out.println("start time: " + sdf.format(formattedstarttime));
		
		//initialise the simulation
		int seed= (int) (System.currentTimeMillis());	
		String paramFile = args[0];		// set the seed for the simulation, be careful for when running on cluster																	
		SimulationEnvironment simulation = new SimulationEnvironment(seed,IO.openXMLFile(paramFile));								// instantiate the simulation

		//start the simulation
		long steps = 0;
		simulation.start();
		System.out.println("StromaSim v1.0 - Console Version");
		System.out.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");
		
		// run the simulation for a fixed amount of time
		// TODO maybe input as an external parameter to see what happens
		do
		{
	
			steps = simulation.schedule.getSteps();		
			System.out.println("Steps: " + steps);
			if (!simulation.schedule.step(simulation))
			break;
		
			
		}while(steps < 4320);	
		
		// finish the simulation
		simulation.finish();	
		System.out.println("\nSimulation completed successfully!\n\n");
		
		// Output the time taken for simulation to run
		// TODO amount of memory may also be useful
		long endtime = System.currentTimeMillis();
		Date formattedendtime = new Date(endtime); 
		System.out.println("endtime: " + sdf.format(formattedendtime));

		
		
		
		long totaltime= endtime - starttime;
		System.out.println("total time taken to run: " + totaltime/60000 + " minutes and " + (totaltime % 60000)/1000 +" seconds"); //convert milliseconds to minutes
		
		System.exit(0);	
	}
	
}
