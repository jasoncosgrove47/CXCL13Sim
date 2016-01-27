package dataLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import sim.util.Double3D;
import sim3d.SimulationEnvironment;
import sim3d.util.CSVFileOutput6;


public final class outputToCSV {
	/**
	 * Forms the view component of the MVC (see controller)
	 * responsible for processing data and exporting in .csv format
	 * 
	 * @author jason cosgrove
	 */
	
	
	/**
	 * process migration data and send processed 
	 * and raw data to csv files
	 */
	public static void processData(String processedFileName, String rawFileName)
	{
	
		FileWriter rawDataWriter;
		FileWriter processedDataWriter;
		
		
		try
		{
			processedDataWriter = new FileWriter(processedFileName);
			//set the data headings
			processedDataWriter.append("TrackID");
			processedDataWriter.append(',');
			processedDataWriter.append("dT");
			processedDataWriter.append(',');
			processedDataWriter.append("MC");
			processedDataWriter.append(',');
			processedDataWriter.append("MI");
			processedDataWriter.append(',');
			processedDataWriter.append("Speed");
			processedDataWriter.append('\n');
			
			
			rawDataWriter = new FileWriter(rawFileName);
			//set the data headings
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
			
		
			//for each tracker cell
			for(Integer key : SimulationEnvironment.getController().getX_Coordinates().keySet())
			{		
				double[] results = processMigrationData(key, rawDataWriter);
			    
			    //write the data out to the file
	            processedDataWriter.append(Integer.toString(key));
	            processedDataWriter.append(',');
	            processedDataWriter.append(Double.toString(results[0]));
	            processedDataWriter.append(',');
	            processedDataWriter.append(Double.toString(results[1]));
	            processedDataWriter.append(',');
	            processedDataWriter.append(Double.toString(results[2]));
	            processedDataWriter.append(',');
	            processedDataWriter.append(Double.toString(results[3]));
	            processedDataWriter.append('\n'); 
	               
		  }
		
		//close the file stream
		processedDataWriter.flush();
        processedDataWriter.close();
        
        rawDataWriter.flush();
        rawDataWriter.close();
		
		}
		catch (IOException e) {e.printStackTrace();}	 
	}
		
	
	
	
	/**
	 * Calculates the speed, motility coefficient and meandering index for each cell
	 * @param key for an individual cell
	 * @return a double array with relevant motility parameters
	 * @throws IOException 
	 */
	private static double[] processMigrationData(Integer key, FileWriter rawDataWriter) throws IOException
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
	    	
	    	double x = 0, y = 0, z = 0;
	    	
		    for(int i = 0; i < Xcoords.size();i++)
		    {
		    	
		    	x = Xcoords.get(i)*10;
		    	y = Ycoords.get(i)*10;
		    	z = Zcoords.get(i)*10;
		    	
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
		    double time = Xcoords.size();
		    
		    double meanderingIndex = totalDisplacement/netDisplacement;
		    double motilityCoefficient = (Math.pow(netDisplacement,2)/(6*time));
		    double speed = totalDisplacement/time;
		    
		    double[] output = {time, motilityCoefficient, meanderingIndex,speed,x,y,z};
		    
		    return output;
		}
}
