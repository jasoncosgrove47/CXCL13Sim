package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import sim3d.SimulationEnvironment;

import sim3d.util.CSVFileOutput6;


public class outputToCSV {
	/**
	 * Forms the view component of the MVC (see controller)
	 * responsible for exporting data in .csv format
	 * 
	 * @author jason cosgrove
	 */
	

	
	
	/**
	 * Given a pathname this method will write a formatted .csv file with
	 * the cellID, Timepoint, X,Y and Z coordinates (as strings)
	 * @param input path and filename as one string
	 */
	public static void generateCSVFile(String fileName) 
	{
		 
		FileWriter writer;
		 
		try 
		{
			writer = new FileWriter(fileName);
			//set the data headings
			writer.append("TrackID");
			writer.append(',');
			writer.append("Timepoint");
			writer.append(',');
			writer.append("CentroidX");
			writer.append(',');
			writer.append("CentroidY");
			writer.append(',');
			writer.append("CentroidZ");
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
			    	
			    	//need to multiply by 10 because each gridspace equals 10 microns
			    	//therefore the output we get will be directly comparable with experimental
			    	Double x = Xcoords.get(i)*10;
			    	Double y = Ycoords.get(i)*10;
			    	Double z = Zcoords.get(i)*10;
				
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
	

	

		/*
		
		
		public static ArrayList<Integer> DataOutput = new ArrayList<Integer>(Collections.nCopies(6, 0));
		public static ArrayList<String> headers = new ArrayList<String>() {{
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		    add("X-coordinates");
		}};
		
		
		public static void updateArrayList( int primedCells)
		{	
			
			DataOutput.set(0,primedCells);
		}
		
		


    public static void writeCSV(ArrayList data, ArrayList headers, String filePath, String outputFileName)
    {
    	CSVFileOutput6 fo = new CSVFileOutput6(filePath,outputFileName,headers);
    	fo.writeDataToFile(data);
    }

*/


}
