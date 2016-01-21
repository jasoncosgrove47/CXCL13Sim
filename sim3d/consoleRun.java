package sim3d;

import java.awt.Color;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dataLogger.outputToCSV;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.cell.cognateBC.TYPE;
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

	/*
	 * Path where file should be sent to
	 */
	public static String outputPath =  "/Users/jc1571/Desktop/"; 
	
	/*
	 * The class responsible for initialising the simulation
	 * irrespective of a GUI or console run.
	 */
	public static SimulationEnvironment simulation;
	
	/*
	 * name of output filename
	 */
	public static String outputFileName = "foo.csv";	
	
	private static final long serialVersionUID = 1;
	

	/**
	 * Runs the simulation
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
		String paramFile = args[0];							
		simulation = new SimulationEnvironment(seed,IO.openXMLFile(paramFile));							

		//start the simulation
		long steps = 0;
		simulation.start();
		System.out.println("StromaSim v1.0 - Console Version");
		System.out.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");
		
	
		do
		{	
			steps = simulation.schedule.getSteps();		
			System.out.println("Steps: " + steps);		
			if (!simulation.schedule.step(simulation))
			break;	
		} while(simulation.experimentFinished == false);
		
		// finish the simulation
		simulation.finish();	
		System.out.println("\nSimulation completed successfully!\n\n");
		
		outputToCSV.generateCSVFile("/Users/jc1571/Desktop/rawData.csv");
		outputToCSV.processData("/Users/jc1571/Desktop/processedData.csv");
		
		// Output the time taken for simulation to run
		long endtime = System.currentTimeMillis();
		Date formattedendtime = new Date(endtime); 
		System.out.println("endtime: " + sdf.format(formattedendtime));

		long totaltime= endtime - starttime;
		System.out.println("total time taken to run: " + totaltime/60000 + " minutes and " + (totaltime % 60000)/1000 +" seconds"); //convert milliseconds to minutes
		
		System.exit(0);	
	}
	    
	 
}
