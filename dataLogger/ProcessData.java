package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import sim.util.Double3D;

public class ProcessData {

	
	/**
	 * Helper method which calculates the speed, motility coefficient and
	 * meandering index for each cell
	 * 
	 * @param key
	 *            for an individual cell
	 * @return a double array with relevant motility parameters
	 * @throws IOException
	 */
	static double[] processMigrationData(Integer key) throws IOException {

		// get the cell's x,y and z coordinates for every timestep
		ArrayList<Double> Xcoords = Controller.getInstance().getX_Coordinates()
				.get(key);
		ArrayList<Double> Ycoords = Controller.getInstance().getY_Coordinates()
				.get(key);
		ArrayList<Double> Zcoords = Controller.getInstance().getZ_Coordinates()
				.get(key);

		Double3D startLocation = null; // starting position
		Double3D endLocation = null; // final position
		Double3D previousLocation = null; // location at the previous timestep
		Double3D thisLocation = null; // location at this timestep
		double totalDisplacement = 0.0; // total path length
		double netDisplacement = 0.0; // euclidean distance between start and
										// end points

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

		// calculate the total time
		double time = Xcoords.size();

		
		netDisplacement = calculateNetDisplacement(startLocation,endLocation);
		
		double meanderingIndex = calculateMeanderingIndex(totalDisplacement,netDisplacement, time);
		
		double motilityCoefficient = calculateMotilityCoefficient(netDisplacement, time);
		
		double speed = calculateSpeed(totalDisplacement,time);
	
		// store all motility parameters in an output array
		double[] output = { time, motilityCoefficient, meanderingIndex, speed,
				x, y, z };

		return output;
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
	static void processRawData(Integer key,
			FileWriter rawDataWriter) throws IOException {
		// get all of their x,y and z coordinates
		ArrayList<Double> Xcoords = Controller.getInstance().getX_Coordinates()
				.get(key);
		ArrayList<Double> Ycoords = Controller.getInstance().getY_Coordinates()
				.get(key);
		ArrayList<Double> Zcoords = Controller.getInstance().getZ_Coordinates()
				.get(key);
		ArrayList<Integer> Receptors = Controller.getInstance().getReceptors()
				.get(key);

		double x = 0, y = 0, z = 0;
		int r = 0;

		// for each timepoint
		for (int i = 0; i < Xcoords.size(); i++) {
			// get the x,y and z coordinates of each cell
			// multiply by 10 because each gridspace
			// equals 10 microns, and we want output in microns
			// and not metres
			x = Xcoords.get(i) * 10;
			y = Ycoords.get(i) * 10;
			z = Zcoords.get(i) * 10;
			r = Receptors.get(i);

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
			rawDataWriter.append(',');
			rawDataWriter.append(Integer.toString(r));
			rawDataWriter.append('\n');

		}
	}
	
	
	
	
	private static double calculateNetDisplacement(Double3D startLocation, Double3D endLocation){
		return startLocation.distance(endLocation);
	}
	
	
	private static double calculateSpeed(double totalDisplacement, double time){
		return totalDisplacement/time;
	}
	
	
	private static double calculateMotilityCoefficient(double netDisplacement, double time){		
		return (Math.pow(netDisplacement, 2) / (6 * time));
	}
	
	
	private static double calculateMeanderingIndex(double totalDisplacement,double netDisplacement, double time){
		return (netDisplacement / totalDisplacement)
		* Math.sqrt(time);
		
	}
	

	
	
	
}
