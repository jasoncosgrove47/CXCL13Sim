package dataLogger;

import java.util.ArrayList;
import java.util.Collections;

import sim.engine.SimState;
import sim3d.SimulationEnvironment;
import sim3d.util.CSVFileOutput6;


public class outputToCSV {
	/**
	 * Creates a datalogger, and outputs the data to .csv
	 * might make more sense to instantiate datalogger and use these to provide methods such that 
	 * they can just be called elsewhere as we need.
	 * 
	 * 
	 * also need to account for single cell tracking mechanisms. 
	 */

	
	public static ArrayList<Integer> DataOutput = new ArrayList<Integer>(Collections.nCopies(1, 0));
	public static ArrayList<String> headers = new ArrayList<String>() {{
		add("Antigen Acquisition");	
	}};
	
	
	
	/*
	 * Access the datalogger hashtable and store it in an array ready for csv output
	 * should rename to process for csv output
	 * should just be appendint, ie put not set 
	 */
	public static void updateArrayList( int primedCells)
	{	
		DataOutput.set(0,primedCells);

	}


    public static void writeCSV(ArrayList data, ArrayList headers, String filePath, String outputFileName)
    {
    	CSVFileOutput6 fo = new CSVFileOutput6(filePath,outputFileName,headers);
    	fo.writeDataToFile(data);
    }

}
