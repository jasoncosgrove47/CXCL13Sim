package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import sim.util.Double3D;
import sim3d.SimulationEnvironment;
import sim3d.util.CSVFileOutput6;


public class outputToCSV {
	/**
	 * Forms the view component of the MVC (see controller)
	 * responsible for processing data and exporting in .csv format
	 * 
	 * 
	 * TODO code needs refactoring
	 * 
	 * @author jason cosgrove
	 */
	

	
	
	
	
	/**
	 * Calculates the speed, motility coefficient and meandering index for each cell
	 * 
	 * 
	 */
	public static void processData(String fileName)
	{
	
		FileWriter writer;
		try
		{
			writer = new FileWriter(fileName);
			//set the data headings
			writer.append("TrackID");
			writer.append(',');
			writer.append("dT");
			writer.append(',');
			writer.append("MC");
			writer.append(',');
			writer.append("MI");
			writer.append(',');
			writer.append("Speed");
			writer.append('\n');
			
			//for each tracker cell
			for(Integer key : SimulationEnvironment.getController().getX_Coordinates().keySet())
			{
			    //get all of their x,y and z coordinates
			    ArrayList<Double> Xcoords = SimulationEnvironment.getController().getX_Coordinates().get(key);
			    ArrayList<Double> Ycoords = SimulationEnvironment.getController().getY_Coordinates().get(key);
			    ArrayList<Double> Zcoords = SimulationEnvironment.getController().getZ_Coordinates().get(key);
			 
				Double3D startLocation = null; // the starting position of the cell
		    	Double3D endLocation = null;  // the final position of the cell
		    	Double3D previousLocation = null; // cells location at the previous timestep
		    	Double3D thisLocation = null; //cells location at this timestep 
		    	
		    	double totalDisplacement = 0;
		    	double netDisplacement = 0;
		    	
			    for(int i = 0; i < Xcoords.size();i++)
			    {
			    	//for each timepoint
			    	if(i==0)
			    	{
			    		//multiply by 10 because each gridspace equals 10 microns
			    		startLocation = new Double3D(Xcoords.get(i)*10, Ycoords.get(i)*10,  Zcoords.get(i)*10);
			    		previousLocation = new Double3D(Xcoords.get(i)*10, Ycoords.get(i)*10,  Zcoords.get(i)*10);
			    		thisLocation = new Double3D(Xcoords.get(i)*10, Ycoords.get(i)*10,  Zcoords.get(i)*10);
			    	}
			    	else
			    	{
			    		thisLocation = new Double3D(Xcoords.get(i)*10, Ycoords.get(i)*10,  Zcoords.get(i)*10);
			    		
			    		//calculate the displacement between this timestep and the last one
			    		totalDisplacement += previousLocation.distance(thisLocation);
			    		
			    		//if this is the last coordinate of the track then we need to mark it
			    		if(i == Xcoords.size()-1)
			    		{	
				    		endLocation = thisLocation;
				    	}	
			    		
			    	}
			    	
			    	previousLocation = thisLocation;
			    }
			    
			   
			    netDisplacement = startLocation.distance(endLocation);			
			    int time = Xcoords.size();
			    
			    double meanderingIndex = totalDisplacement/netDisplacement;
			    double motilityCoefficient = (Math.pow(netDisplacement,2)/(6*time));
			    double speed = totalDisplacement/time;
			    
			    //write the data out to the file
	            writer.append(Integer.toString(key));
	            writer.append(',');
	            writer.append(Integer.toString(time));
	            writer.append(',');
	            writer.append(Double.toString(motilityCoefficient));
	            writer.append(',');
	            writer.append(Double.toString(meanderingIndex));
	            writer.append(',');
	            writer.append(Double.toString(speed));
	            writer.append('\n'); 
 
		  }
		
		//close the file stream
		writer.flush();
        writer.close();
		
		}
		catch (IOException e) {e.printStackTrace();}	 
	}
		
	
	
	
	
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
