package sim3d;

import java.sql.Date;
import java.text.SimpleDateFormat;
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
	public static String outputPath = "/Users/jc1571/Desktop/";

	/*
	 * name of output filename
	 */
	public static String outputFileName = "foo.csv";

	/**
	 * Runs the simulation
	 */
	public static void main(String[] args) {
		// output the start time
		long starttime = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		Date formattedstarttime = new Date(starttime);
		System.out.println("start time: " + sdf.format(formattedstarttime));

		// initialise the simulation
		int seed = (int) (System.currentTimeMillis());
		String paramFile = args[0];
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
			System.out.println("test does print work ");
			if (!SimulationEnvironment.simulation.schedule
					.step(SimulationEnvironment.simulation))
				break;
		} while (SimulationEnvironment.experimentFinished == false);

		// finish the simulation
		SimulationEnvironment.simulation.finish();
		System.out.println("\nSimulation completed successfully!\n\n");

		outputToCSV.writeDataToFile("/Users/jc1571/Desktop/processedData.csv",
				"/Users/jc1571/Desktop/rawData.csv");

		// Output the time taken for simulation to run
		long endtime = System.currentTimeMillis();
		Date formattedendtime = new Date(endtime);
		System.out.println("endtime: " + sdf.format(formattedendtime));
		long totaltime = endtime - starttime;
		//convert milliseconds to minutes
		System.out.println("total time taken to run: " + totaltime / 60000
				+ " minutes and " + (totaltime % 60000) / 1000 + " seconds"); 

		System.exit(0);
	}

}
