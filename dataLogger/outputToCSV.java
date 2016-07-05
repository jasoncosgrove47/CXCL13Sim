package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import sim.util.Double3D;
import sim3d.SimulationEnvironment;

public final class outputToCSV {
	/**
	 * Forms the view component of the MVC (see controller) responsible for
	 * processing data and exporting in .csv format
	 * 
	 * @author jason cosgrove
	 */

	/**
	 * processes migration data and sends processed data to csv files
	 */
	public static void writeDataToFile(String processedFileName) {

		FileWriter processedDataWriter;

		
		//the number of unique dendrites visited
		double dendritesVisited;
		// the percentage of the network the B-cell has scanned
		double networkScanned;

		try {
			
			processedDataWriter = new FileWriter(processedFileName);

			// set the data headings
			processedDataWriter.append("TrackID");
			processedDataWriter.append(',');
			processedDataWriter.append("dT");
			processedDataWriter.append(',');
			processedDataWriter.append("MC");
			processedDataWriter.append(',');
			processedDataWriter.append("MI");
			processedDataWriter.append(',');
			processedDataWriter.append("Speed");
			processedDataWriter.append(',');
			processedDataWriter.append("dendritesVisited");
			processedDataWriter.append('\n');

			// for each tracker cell
			for (Integer key : Controller.getInstance().getX_Coordinates()
					.keySet()) {
				double[] results = processMigrationData(key);

				// calculate the percentage of the network scanned
				dendritesVisited = (double) Controller.getInstance()
						.getDendritesVisited().get(key);

				//divide the number of dendrites visited by the total number of dendrites
				networkScanned = (dendritesVisited / SimulationEnvironment.totalNumberOfDendrites);

				// write the data out to the file
				processedDataWriter.append(Integer.toString(key));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[0]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[1]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[2]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[3]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(networkScanned));
				processedDataWriter.append('\n');

			}

			// close the file stream
			processedDataWriter.flush();
			processedDataWriter.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method which calculates the speed, motility coefficient 
	 * and meandering index for each cell
	 * 
	 * @param key
	 *            for an individual cell
	 * @return a double array with relevant motility parameters
	 * @throws IOException
	 */
	static double[] processMigrationData(Integer key)
			throws IOException {
		
		
		
		// get the cell's x,y and z coordinates for every timestep
		ArrayList<Double> Xcoords = Controller.getInstance().getX_Coordinates()
				.get(key);
		ArrayList<Double> Ycoords = Controller.getInstance().getY_Coordinates()
				.get(key);
		ArrayList<Double> Zcoords = Controller.getInstance().getZ_Coordinates()
				.get(key);

		
		
		Double3D startLocation = null; 		// starting position
		Double3D endLocation = null; 		// final position
		Double3D previousLocation = null;	// location at the previous timestep
		Double3D thisLocation = null; 		// location at this timestep
		double totalDisplacement = 0.0;     // total path length
		double netDisplacement = 0.0;       // euclidean distance between start and end points

		double x = 0, y = 0, z = 0;

		// for each timepoint
		for (int i = 0; i < Xcoords.size(); i++) {
			// get the x,y and z coordinates of each cell
			// multiply by 10 because each gridspace
			// equals 10 microns
			x = Xcoords.get(i) * 10;
			y = Ycoords.get(i) * 10;
			z = Zcoords.get(i) * 10;

			thisLocation = new Double3D(x, y, z);

			// for each timepoint
			if (i == 0) {
				startLocation = thisLocation;
				previousLocation = thisLocation;
			} else {
				// calculate the displacement between this
				// timestep and the last one
				totalDisplacement += previousLocation.distance(thisLocation);

				// if this is the last coordinate of the track then we need to
				// mark it
				if (i == Xcoords.size() - 1) {
					endLocation = thisLocation;
				}
			}
			previousLocation = thisLocation;
		}

		
		//calculate the total time
		double time = Xcoords.size();
		
		//calculate the net displacement travelled
		netDisplacement = startLocation.distance(endLocation);
		
		//calculate the meandering Index
		double meanderingIndex = totalDisplacement / netDisplacement;
		
		//calculate the motility Coefficient
		double motilityCoefficient = (Math.pow(netDisplacement, 2) / (6 * time));
		
		//calculate the speed
		double speed = totalDisplacement / time;

		//store all motility parameters in an output array
		double[] output = { time, motilityCoefficient, meanderingIndex, speed,
				x, y, z };

		return output;
	}

	/**
	 * processes migration data and sends processed and raw data to csv files
	 * TODO needs refactoring DRY!!!!
	 */
	public static void writeDataToFile(String processedFileName,
			String rawFileName) {

		FileWriter rawDataWriter;
		FileWriter processedDataWriter;

		double dendritesVisited;
		// the percentage of the network the B-cell has scanned
		double networkScanned;

		try {
			processedDataWriter = new FileWriter(processedFileName);
			rawDataWriter = new FileWriter(rawFileName);

			processedDataWriter.append("TrackID");
			processedDataWriter.append(',');
			processedDataWriter.append("dT");
			processedDataWriter.append(',');
			processedDataWriter.append("MC");
			processedDataWriter.append(',');
			processedDataWriter.append("MI");
			processedDataWriter.append(',');
			processedDataWriter.append("Speed");
			processedDataWriter.append(',');
			processedDataWriter.append("dendritesVisited");
			processedDataWriter.append('\n');

			// set the data headings

			rawDataWriter.append("TrackID");
			rawDataWriter.append(',');
			rawDataWriter.append("Timepoint");
			rawDataWriter.append(',');
			rawDataWriter.append("CentroidX");
			rawDataWriter.append(',');
			rawDataWriter.append("CentroidY");
			rawDataWriter.append(',');
			rawDataWriter.append("CentroidZ");
			rawDataWriter.append('\n');

			// for each tracker cell
			for (Integer key : Controller.getInstance().getX_Coordinates()
					.keySet()) {
				double[] results = processMigrationData(key, rawDataWriter);

				// calculate the percentage of the network scanned

				dendritesVisited = (double) Controller.getInstance()
						.getDendritesVisited().get(key);

				networkScanned = (dendritesVisited / SimulationEnvironment.totalNumberOfDendrites);

				// write the data out to the file
				processedDataWriter.append(Integer.toString(key));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[0]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[1]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[2]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(results[3]));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(networkScanned));
				processedDataWriter.append('\n');

			}

			// close the file stream
			processedDataWriter.flush();
			processedDataWriter.close();

			// rawDataWriter.flush();
			// rawDataWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the speed, motility coefficient and meandering index for each
	 * cell
	 * 
	 * @param key
	 *            for an individual cell
	 * @return a double array with relevant motility parameters
	 * @throws IOException
	 */
	private static double[] processMigrationData(Integer key,
			FileWriter rawDataWriter) throws IOException {
		// get all of their x,y and z coordinates
		ArrayList<Double> Xcoords = Controller.getInstance().getX_Coordinates()
				.get(key);
		ArrayList<Double> Ycoords = Controller.getInstance().getY_Coordinates()
				.get(key);
		ArrayList<Double> Zcoords = Controller.getInstance().getZ_Coordinates()
				.get(key);

		Double3D startLocation = null; // starting position
		Double3D endLocation = null; // final position
		Double3D previousLocation = null;// location at the previous timestep
		Double3D thisLocation = null; // location at this timestep

		// BigDecimals are more precise than doubles
		// to avoid rounding errors
		double totalDisplacement = 0.0;
		double netDisplacement = 0.0;

		double x = 0, y = 0, z = 0;

		// for each timepoint
		for (int i = 0; i < Xcoords.size(); i++) {
			// get the x,y and z coordinates of each cell
			// multiply by 10 because each gridspace
			// equals 10 microns, and we want output in microns
			// and not metres
			x = Xcoords.get(i) * 10;
			y = Ycoords.get(i) * 10;
			z = Zcoords.get(i) * 10;

			// update raw data file

			rawDataWriter.append(Integer.toString(key));
			rawDataWriter.append(',');
			rawDataWriter.append(Integer.toString(i));
			rawDataWriter.append(',');
			rawDataWriter.append(Double.toString(x));
			rawDataWriter.append(',');
			rawDataWriter.append(Double.toString(y));
			rawDataWriter.append(',');
			rawDataWriter.append(Double.toString(z));
			rawDataWriter.append('\n');

			thisLocation = new Double3D(x, y, z);

			// for each timepoint
			if (i == 0) {
				startLocation = thisLocation;
				previousLocation = thisLocation;
			} else {
				// calculate the displacement between this
				// timestep and the last one
				totalDisplacement += previousLocation.distance(thisLocation);

				// if this is the last coordinate of the track then we need to
				// mark it
				if (i == Xcoords.size() - 1) {
					endLocation = thisLocation;
				}
			}
			previousLocation = thisLocation;
		}

		double time = Xcoords.size();
		netDisplacement = startLocation.distance(endLocation);
		double meanderingIndex = totalDisplacement / netDisplacement;
		double motilityCoefficient = (Math.pow(netDisplacement, 2) / (6 * time));
		double speed = totalDisplacement / time;

		double[] output = { time, motilityCoefficient, meanderingIndex, speed,
				x, y, z };

		return output;
	}
}
