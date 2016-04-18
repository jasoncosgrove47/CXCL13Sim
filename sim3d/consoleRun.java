package sim3d;

import java.sql.Date;
import java.text.SimpleDateFormat;

import dataLogger.Controller;
import dataLogger.WriteObjects;
import dataLogger.outputToCSV;
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
		System.out.println("StromaSim v1.0 - Console Version");
		System.out
				.println("\nAuthor: Jason Cosgrove, York Computational Immunology Lab");

		// the main loop which controls how long the simulation runs for
		do {
			steps = SimulationEnvironment.simulation.schedule.getSteps();
			System.out.println("Steps: " + steps);

			// run the simulation for 500 steps to allow it to reach
			// steady-state
			if (steps == 300) {

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
		outputToCSV.writeDataToFile(outputPath + outputFileName,"/Users/jc1571/Desktop/raw.csv" );
		//outputToCSV.writeDataToFile(outputPath + outputFileName);
		outputToCSV.outputReceptors(outputPath + "/receptors.csv" );
		
		// Output the time taken for simulation to run
		long endtime = System.currentTimeMillis();
		Date formattedendtime = new Date(endtime);
		System.out.println("endtime: " + sdf.format(formattedendtime));
		long totaltime = endtime - starttime;
		// convert milliseconds to minutes
		System.out.println("total time taken to run: " + totaltime / 60000
				+ " minutes and " + (totaltime % 60000) / 1000 + " seconds");

		System.exit(0);
	}

	/**
	 * Save the steady state to an output file
	 */
	private void saveToSteadyState(){
		// write the steady state out to file so we can observe it later
		WriteObjects wo = new WriteObjects();
		wo.writeFDC(SimulationEnvironment.simulation);
		wo.writeBC(SimulationEnvironment.simulation);
		wo.writeCXCL13(SimulationEnvironment.simulation);
		
	}
	
}
