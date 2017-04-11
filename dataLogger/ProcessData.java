package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import sim.util.Double3D;

import sim3d.util.Vector3DHelper;

public class ProcessData {
	/**
	 * This class is responsible for calculating the migration metrics following
	 * single cell tracking experiments
	 */

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

		ArrayList<Double3D> Coords = Controller.getInstance().getCoordinates().get(key);

		Double3D startLocation = null;  // starting position
		Double3D endLocation = null;    // final position
		Double3D previousLocation = null; // location at the previous timestep
		Double3D thisLocation = null;   // location at this timestep
		double totalDisplacement = 0.0; // total path length
		double netDisplacement = 0.0;   // euclidean distance between start and
										// end points
		double x = 0, y = 0, z = 0;

		// for each timepoint
		for (int i = 0; i < Coords.size(); i++) {

			// get the x,y and z coordinates of each cell
			// multiply by 10 because each gridspace
			// equals 10 microns
			x = Coords.get(i).x * 10;
			y = Coords.get(i).y * 10;
			z = Coords.get(i).z * 10;

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
				if (i == Coords.size() - 1) {
					endLocation = thisLocation;
				}
			}
			previousLocation = thisLocation;
		}

		// calculate the total time
		double time = Coords.size();

		// calculate the net displacement travelled
		netDisplacement = startLocation.distance(endLocation);
		double meanderingIndex = calculateMeanderingIndex(totalDisplacement, netDisplacement, time);
		double motilityCoefficient = calculateMotilityCoefficient(netDisplacement, time);
		double speed = calculateSpeed(totalDisplacement, time);

		// store all motility parameters in an output array
		double[] output = { time, motilityCoefficient, meanderingIndex, speed, x, y, z };

		return output;
	}

	/**
	 * Calculates the speed, motility coefficient and meandering index for each
	 * cell
	 * 
	 * @param key
	 *            for an individual cell
	 * @throws IOException
	 */
	static void processRawData(Integer key, FileWriter rawDataWriter) throws IOException {

		ArrayList<Integer> Receptors = Controller.getInstance().getReceptors().get(key);

		ArrayList<Double3D> Coords = Controller.getInstance().getCoordinates().get(key);

		double x = 0, y = 0, z = 0;
		int r = 0;

		// for each timepoint
		for (int i = 0; i < Coords.size(); i++) {
			// get the x,y and z coordinates of each cell
			// multiply by 10 because each gridspace
			// equals 10 microns, and we want output in microns
			// and not metres
			x = Coords.get(i).x * 10;
			y = Coords.get(i).y * 10;
			z = Coords.get(i).z * 10;
			r = Receptors.get(i);

			Double3D thisLocation = new Double3D(x, y, z);
			Double3D nextLocation = calculateNextLocation(i, Coords);
			Double3D previousLocation = calculatePreviousLocation(i, Coords);

			double turningAngle;

			if (nextLocation != null && previousLocation != null) {

				turningAngle = calculateTurningAngle(previousLocation, thisLocation, nextLocation);
			} else
				turningAngle = 210;

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
			rawDataWriter.append(',');
			rawDataWriter.append(Double.toString(turningAngle));
			rawDataWriter.append('\n');

		}
	}

	/**
	 * Calculate the motility coefficient of a cell
	 */
	private static double calculateMotilityCoefficient(double netDisplacement, double time) {
		return (Math.pow(netDisplacement, 2) / (6 * time));
	}

	/*
	 * Determine the previous x,y and z coordinates in the arraylist
	 */
	private static Double3D calculatePreviousLocation(int i, ArrayList<Double3D> Coords) {

		if (i > 0) {
			double x = Coords.get(i - 1).x * 10;
			double y = Coords.get(i - 1).y * 10;
			double z = Coords.get(i - 1).z * 10;

			return new Double3D(x, y, z);

		} else
			return null;
	}

	/*
	 * Obtain the next set of cooordinates in the arraylist
	 */
	private static Double3D calculateNextLocation(int i, ArrayList<Double3D> Coords) {
		if (i < Coords.size() - 1) {
			//we multiply by 10 here to get the x,y and z coordinates
			// in microns
			double x = Coords.get(i + 1).x * 10;
			double y = Coords.get(i + 1).y * 10;
			double z = Coords.get(i + 1).z * 10;

			return new Double3D(x, y, z);
		}
		else
			return null;
	}

	/**
	 * Calculate the meandering index of a cell
	 * 
	 * @param totalDisplacement
	 * @param netDisplacement
	 * @param time
	 * @return
	 */
	private static double calculateMeanderingIndex(double totalDisplacement, double netDisplacement, double time) {
		// calculate meandering index
		double mi = netDisplacement / totalDisplacement;
		// correct for time dependency of tracking
		// as discussed in analysing immune cell
		// migration de Boer 2009
		double miCorrected = mi * Math.sqrt(time);
		return miCorrected;
	}

	
	/**
	 * Calculates the velocity of a cell
	 * 
	 * @param totalDisplacement
	 *            total distance travelled during a tracking expt
	 * @param time
	 *            total duration of tracking expt
	 * @return the velocity
	 */
	private static double calculateSpeed(double totalDisplacement, double time) {
		return totalDisplacement / time;
	}

	/*
	 * Calculate turning angle: taken from matlab script provided by Jens Stein
	 * from the University of Bern. The
	 * angle between two 3D vectors is given by the acos of the dot product of
	 * two normalised vectors.
	 * 
	 *
	 * the angle between two vectors P and Q is: arcos((P.Q)/(|P||Q|))
	 * 
	 * theta <- acos( sum(a*b) / ( sqrt(sum(a * a)) * sqrt(sum(b * b)) ) )
	 * 
	 * the cross product then gives you the direction along the Z axis (the
	 * z-component of the Double3D). A positive angle means you are going
	 * anti-clockwise (right hand rule) so we call this a negative angle
	 * 
	 */
	static double calculateTurningAngle(Double3D p1, Double3D p2, Double3D p3) {

		// calculate vector1 and vector 2 from the three points

		double v1_x, v1_y, v1_z, v2_x, v2_y, v2_z;

		v1_x = p1.x - p2.x;
		v1_y = p1.y - p2.y;
		v1_z = p1.z - p2.z;

		v2_x = p2.x - p3.x;
		v2_y = p2.y - p3.y;
		v2_z = p2.z - p3.z;

		Double3D v1 = new Double3D(v1_x, v1_y, v1_z);
		Double3D v2 = new Double3D(v2_x, v2_y, v2_z);

		double angleRadians = (Math.acos(Vector3DHelper.dotProduct(v1.normalize(), v2.normalize())));
		// convert into degrees
		double angleDegrees = Math.toDegrees(angleRadians);

		// now we need the z component of the vector
		Double3D crossProduct = Vector3DHelper.crossProduct(v1, v2);

		// A positive crossproduct along z ( the sign of the
		// z component) is counter clockwise. We call this a negative turn
		// so reverse the sign on the angle
		if (crossProduct.z > 0) {
			return -angleDegrees;
		} else {
			return angleDegrees;
		}

	}
	


}
