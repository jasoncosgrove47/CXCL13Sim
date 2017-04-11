package sim3d;

import java.sql.Date;
import java.text.SimpleDateFormat;

import dataLogger.Controller;
import dataLogger.outputToCSV;
import sim3d.stroma.Stroma;
import sim3d.util.IO;

/**
 * Sets up and runs the simulation without a GUI
 * 
 * Need to ensure that Java has access to enough memory resources go to run
 * configurations and pass in -Xmx3000m
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 */


public class consoleRun {

	/*
	 * Path where file should be sent to
	 */
	public static String outputPath;

	/*
	 * name of output filename
	 */
	public static String outputFileName;

	/**
	 * Run the simulation
	 */
	
	public static void main(String[] args) {

		// output the start time
		long starttime = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		Date formattedstarttime = new Date(starttime);
		System.out.println("start time: " + sdf.format(formattedstarttime));

		// initialise the simulation
		// NOTE: make sure to use a proper seed, see the following:
		// http://www0.cs.ucl.ac.uk/staff/D.Jones/GoodPracticeRNG.pdf
		int seed = (int) (Integer.valueOf(args[3]) * System.currentTimeMillis());
		String paramFile = args[0];
		outputPath = args[1];
		outputFileName = args[2];
		SimulationEnvironment.simulation = new SimulationEnvironment(seed,
				IO.openXMLFile(paramFile));

		// start the simulation
		long steps = 0;
		SimulationEnvironment.simulation.start();
		System.out.println("FollicleSim v1.0 - Console Version");
		System.out
				.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");

		
		// the main loop which controls how long the simulation runs for
		do {
			steps = SimulationEnvironment.simulation.schedule.getSteps();
			System.out.println("Steps: " + steps);

			// run the simulation for 500 steps to allow it to reach
			// steady-state
			if (steps == 100) {

				// update the steadyState guard to begin recording data
				SimulationEnvironment.steadyStateReached = true;

				System.out
						.println("System has a reached a steady state, saving steady state information");

				// instantiate the experimental controller and
				// start to record data
				SimulationEnvironment.simulation.schedule
						.scheduleRepeating(Controller.getInstance());

				System.out.println("The experiment will now begin");
			}
			
			if (!SimulationEnvironment.simulation.schedule
					.step(SimulationEnvironment.simulation))
				break;
		} while (SimulationEnvironment.experimentFinished == false); //

		
		// finish the simulation
		SimulationEnvironment.simulation.finish();
		System.out.println("\nSimulation completed successfully!\n\n");

		// write the recorded data and raw data to a .csv file
		 outputToCSV.writeRawDataToFile("/Users/jc1571/Desktop/raw.csv" );
		 outputToCSV.writeDataToFile(outputPath + outputFileName);
		 
		if(Settings.calculateTopologyData){
		   outputToCSV.writeDegreesToFile(outputPath + "degrees.csv", Controller.degrees);
			outputToCSV.writeMatrixToFile(outputPath + "dist.csv", SimulationEnvironment.distMatrix);
			outputToCSV.writeMatrixToFile(outputPath + "adjacency.csv", SimulationEnvironment.a_matrix);
			outputToCSV.writeNodeInformationToFile(outputPath + "nodeInfo.csv", 
					Stroma.getNodes());
		}
		// Output the time taken for simulation to run
		long endtime = System.currentTimeMillis();


		System.out.println(calculateRunTime(starttime, endtime, sdf));
		
		System.exit(0);
	}

	private static String calculateRunTime(long startTime, long endTime, SimpleDateFormat sdf){
		
		Date formattedendtime = new Date(endTime);
		System.out.println("endtime: " + sdf.format(formattedendtime));
		long totaltime = endTime - startTime;
		// convert milliseconds to minutes
		return("total time taken to run: " + totaltime / 60000
				+ " minutes and " + (totaltime % 60000) / 1000 + " seconds");
		
		
		
	}


}
