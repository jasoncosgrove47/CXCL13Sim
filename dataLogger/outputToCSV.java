package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import sim3d.SimulationEnvironment;
import sim3d.stroma.Stroma;

public final class outputToCSV {
	/**
	 * Forms the view component of the MVC (see controller) responsible for
	 * exporting in .csv format
	 * 
	 * @author jason cosgrove
	 */

	
	/**
	 * This method writes out a .csv file with the number of edges connected to a given node
	 * @param 
	 * 		filename where to send the csv
	 * @param 
	 * 		degrees an arraylist containing the number of edges per node
	 */
	public static void writeDegreesToFile(String filename, ArrayList<Integer> degrees) {
		FileWriter processedDataWriter;

		try {

			processedDataWriter = new FileWriter(filename);
			processedDataWriter.append("degree");
			processedDataWriter.append('\n');

			for (Integer temp : degrees) {

				processedDataWriter.append(Integer.toString(temp));

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
	 * This method writes all of the node details to file. details include the node ID, 
	 * subset type and 3D coordinates
	 * @param 
	 * 		filename where to send the .csv
	 * @param 
	 * 		nodeinformation a Stroma arraylist containing all key information
	 */
	public static void writeNodeInformationToFile(String filename, ArrayList<Stroma> nodeinformation) {
		FileWriter processedDataWriter;

		try {

			
			processedDataWriter = new FileWriter(filename);
			processedDataWriter.append("x");
			processedDataWriter.append(',');
			processedDataWriter.append("y");
			processedDataWriter.append(',');
			processedDataWriter.append("z");
			processedDataWriter.append(',');
			processedDataWriter.append("ID");
			processedDataWriter.append(',');
			processedDataWriter.append("subset");
			processedDataWriter.append('\n');

			for (Stroma temp : nodeinformation) {

				processedDataWriter.append(Double.toString(temp.getM_Location().x * 10));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(temp.getM_Location().y * 10));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(temp.getM_Location().z * 10));
				processedDataWriter.append(',');
				processedDataWriter.append(Integer.toString(temp.getM_index()));
				processedDataWriter.append(',');
				processedDataWriter.append(temp.getStromatype().toString());
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
	 * Write a matrix out to .csv file
	 * 
	 * @param filename
	 *            where to export the matrix
	 * @param a_matrix
	 *            the matrix to export
	 */
	public static void writeMatrixToFile(String filename, double[][] a_matrix) {
		FileWriter processedDataWriter;

		try {

			processedDataWriter = new FileWriter(filename);

			for (int j = 0; j < a_matrix.length; j++) {
				for (int k = 0; k < a_matrix.length; k++) {

					processedDataWriter.append(Double.toString(a_matrix[j][k]));
					processedDataWriter.append(',');

				}
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
	 * processes migration data and sends processed data to csv files
	 */
	public static void writeDataToFile(String processedFileName) {

		FileWriter processedDataWriter;

		// the number of unique dendrites visited
		double fdcdendritesVisited;

		// the number of unique dendrites visited
		double mrcdendritesVisited;
		
		double brcdendritesVisited;
		
		double totaldendritesVisited;

		// the percentage of the network the B-cell has scanned
		double fdcnetworkScanned;

		// the percentage of the network the B-cell has scanned
		double mrcnetworkScanned;
		
		// the percentage of the network the B-cell has scanned
		double brcnetworkScanned;
		
		// the percentage of the network the B-cell has scanned
		double totalnetworkScanned;
		
		// the percentage of the network the B-cell has scanned
		int checkPointsReached;

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
			processedDataWriter.append("fdcdendritesVisited");
			processedDataWriter.append(',');
			processedDataWriter.append("mrcdendritesVisited");
			processedDataWriter.append(',');
			processedDataWriter.append("brcdendritesVisited");
			processedDataWriter.append(',');
			processedDataWriter.append("totaldendritesVisited");
			processedDataWriter.append(',');
			processedDataWriter.append("checkPointsReached");
			processedDataWriter.append('\n');

			// for each tracker cell
			for (Integer key : Controller.getInstance().getCoordinates().keySet()) {
				double[] results = ProcessData.processMigrationData(key);

				
				
				checkPointsReached = Controller.getInstance().getCheckpointsReached().get(key);
				
				// calculate the percentage of the network scanned
				fdcdendritesVisited = (double) Controller.getInstance().getFDCDendritesVisited().get(key);

				// divide the number of dendrites visited by the total number of
				// dendrites
				fdcnetworkScanned = (fdcdendritesVisited / SimulationEnvironment.totalNumberOfFDCEdges);

				
				// calculate the percentage of the network scanned
				mrcdendritesVisited = (double) Controller.getInstance().getMRCDendritesVisited().get(key);

				// divide the number of dendrites visited by the total number of
				// dendrites

			
				mrcnetworkScanned = (mrcdendritesVisited / SimulationEnvironment.totalNumberOfMRCEdges);
				
				// calculate the percentage of the network scanned
				brcdendritesVisited = (double) Controller.getInstance().getRcdendritesVisited().get(key);

				// divide the number of dendrites visited by the total number of
				// dendrites

			
				brcnetworkScanned = (brcdendritesVisited / SimulationEnvironment.totalNumberOfBRCEdges);
				
				totaldendritesVisited = (double) Controller.getInstance().getTotaldendritesVisited().get(key);

				// divide the number of dendrites visited by the total number of
				// dendrites

			
				totalnetworkScanned = (totaldendritesVisited / SimulationEnvironment.totalNumberOfEdges);
				

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
				processedDataWriter.append(Double.toString(fdcnetworkScanned));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(mrcnetworkScanned));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(brcnetworkScanned));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(totalnetworkScanned));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(checkPointsReached));
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
	 * Write the unprocessed migration data to .csv files
	 */
	public static void writeRawDataToFile(String rawFileName) {

		FileWriter rawDataWriter;

		try {

			rawDataWriter = new FileWriter(rawFileName);
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
			rawDataWriter.append(',');
			rawDataWriter.append("FreeReceptor");
			rawDataWriter.append(',');
			rawDataWriter.append("SignallingReceptor");
			rawDataWriter.append(',');
			rawDataWriter.append("InternalisedReceptor");
			rawDataWriter.append(',');
			rawDataWriter.append("DesensitisedReceptor");
			rawDataWriter.append(',');
			rawDataWriter.append("turningAngle");
			rawDataWriter.append('\n');

			// for each tracker cell
			for (Integer key : Controller.getInstance().getCoordinates().keySet()) {
				ProcessData.processRawData(key, rawDataWriter);
			}

			rawDataWriter.flush();
			rawDataWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
