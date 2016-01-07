package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
	
	//private static ArrayList<Double> x = new ArrayList<Double>();
	//private static ArrayList<Double> y;
	//private static ArrayList<Double> z;
	



	
	
	/**
	 * Given a pathname this method will write a csv file with
	 * the cellID, Timepoint, X,Y and Z coordinates from a simulation
	 * in a formatted way
	 * @param fileName
	 */
	public static void generateCSVFile(String fileName) 
	{
		 
		FileWriter writer;
		 
		try 
		{
			writer = new FileWriter(fileName);
		
			//set the data headings
			writer.append("Cell");
			writer.append(',');
			writer.append("Timepoint");
			writer.append(',');
			writer.append("X-coordinate");
			writer.append(',');
			writer.append("Y-coordinate");
			writer.append(',');
			writer.append("Z-coordinate");
			writer.append('\n');
		 
			//for each indexed cell
			for(Integer key : SimulationEnvironment.getController().getX_Coordinates().keySet())
			{
			    //get the double arrayList from the coordinate map
			    ArrayList<Double> Xcoords = SimulationEnvironment.getController().getX_Coordinates().get(key);
			    ArrayList<Double> Ycoords = SimulationEnvironment.getController().getY_Coordinates().get(key);
			    ArrayList<Double> Zcoords = SimulationEnvironment.getController().getZ_Coordinates().get(key);
			 
			    //get the X,Y and Z coords for each timepoint
			    // and write them out to file
			    for(int i = 0; i < Xcoords.size();i++)
			    {
			    	int timepoint = i;
			    	Double x = Xcoords.get(i);
			    	Double y = Ycoords.get(i);
			    	Double z = Zcoords.get(i);
				
			    	//write the data out to the file
	                writer.append(Integer.toString(key));
	                writer.append(',');
	                writer.append(Integer.toString(timepoint));
	                writer.append(',');
	                writer.append(Double.toString(x));
	                writer.append(',');
	                writer.append(Double.toString(y));
	                writer.append(',');
	                writer.append(Double.toString(z));
	                writer.append('\n'); 
			    }  
			}
			
			//close the file stream
			writer.flush();
	        writer.close();
			   
		} 
		 catch (IOException e) {e.printStackTrace();}	 
	}
	

	

		
		
		
		public static ArrayList<Integer> DataOutput = new ArrayList<Integer>(Collections.nCopies(6, 0));
		public static ArrayList<String> headers = new ArrayList<String>() {{
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
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
