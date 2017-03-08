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

	
	
	
	public static void writeNodeInformationToFile(String filename, ArrayList<Stroma> nodeinformation){
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
				
				processedDataWriter.append(Double.toString(temp.getM_Location().x));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(temp.getM_Location().y));
				processedDataWriter.append(',');
				processedDataWriter.append(Double.toString(temp.getM_Location().z));
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
	
	
	
	public static void writeAdjacencyMatrixToFile(String filename,int[][] a_matrix){
		FileWriter processedDataWriter;


		try {

			processedDataWriter = new FileWriter(filename);

			for(int j = 0; j < a_matrix.length ; j++){
				for(int k = 0; k < a_matrix.length ;k++){
					
					processedDataWriter.append(Integer.toString(a_matrix[j][k]));
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
			for (Integer key : Controller.getInstance().getCoordinates().keySet()) {
				double[] results = ProcessData.processMigrationData(key);

				// calculate the percentage of the network scanned
				dendritesVisited = (double) Controller.getInstance().getDendritesVisited().get(key);

				// divide the number of dendrites visited by the total number of
				// dendrites
				networkScanned = (dendritesVisited / SimulationEnvironment.totalNumberOfAPCs);

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
	 * Write the unprocessed raw data to .csv files
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
			rawDataWriter.append("Receptor");
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
