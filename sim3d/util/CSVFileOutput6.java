
package sim3d.util;

import java.io.*;
import java.util.ArrayList;

public class CSVFileOutput6
{
	/**
	 * Path to where the simulation output should be stored
	 */
	private String filePath;
	
	/**
	 * Name to give to the results file
	 */
	private String fileName;
	
	/**
	 * CSV headers, in an arraylist (simulation responses to be analysed in SPARTAN)
	 */
	private ArrayList<String> headers;
	
	/**
	 * File writer for cells away from a forming patch
	 */
	public FileWriter simOutputWriter;
	
	/**
	 * Constructor: Store path to directory where output is to be stored, file name, and the array of simulation
	 * measures being examined in SPARTAN
	 * 
	 * @param fp	Path to directory storing results
	 * @param fname	Name to give the results file
	 * @param head	Arraylist of simulation output measures that are being analysed in SPARTAN
	 */
	public CSVFileOutput6(String fp, String fname, ArrayList<String> head)
	{
		this.filePath = fp;
		this.fileName = fname;
		this.headers = head;
	}
		
	/**
	 * Method to create the file and write the simulation responses to it. Assumes we are going to send this an array of simulation responses
	 * at a particular timepoint, but we can change this as necessary
	 * 
	 * @param simResponses	Arraylist containing values for simulation responses to be output at a particular simulation timepoint
	 */
	public void writeDataToFile(ArrayList<Double> simResponses)
	{
		try 
		{
			simOutputWriter = new FileWriter(filePath+"/"+fileName);
				
			// Write the headers to the CSV file
			this.writeCSVFileHeader();
			
			// Now write the simulation responses - we need to complete this bit when paired with the simulation
			writeSimulationResponses(simResponses);
            this.simOutputWriter.close();
		}
		catch(IOException e)
		{
			System.out.println("I/O Error: "+e);
		}
	}
	
	
	/**
	 * Write the CSV file header, containing output measures to be analysed in SPARTAN
	 * 
	 */
	public void writeCSVFileHeader()
	{
		try
		{
			// For loop used to iterate through until one is left in array (see -1), such that divider is not placed after last element in header
			for(int i=0;i<headers.size()-1;i++)
			{
				this.simOutputWriter.append(headers.get(i)+",");
			}
						
			// Now write the last element and new line
			this.simOutputWriter.append((headers.get(headers.size()-1))+"\n");
			
		}
		catch(IOException e)
		{
			System.out.println("I/O Error: "+e);
		}
	}
	
	/**
	 * Write the CSV file data - assumed at this point to be sent in here in an arraylist (but we can change this later. This is appended to the file
	 * in a format ready for analysis in SPARTAN
	 * 
	 * @param fout	Output stream to be written to
	 * @param simResponses	Arraylist containing the value of each simulation response to be analysed
	 */
	public void writeSimulationResponses(ArrayList<Double> simResponses)
	{
		try
		{
			// For loop used to iterate through until one is left in array (see -1), such that divider is not placed after last element in the results
			for(int i=0;i<simResponses.size()-1;i++)
			{
				this.simOutputWriter.append(simResponses.get(i)+",");
			}
						
			// Now write the last element and new line
			this.simOutputWriter.append((simResponses.get(simResponses.size()-1))+"\n");
			
		}
		catch(IOException e)
		{
			System.out.println("I/O Error: "+e);
		}
	}

	/**
	 * Example of applying the above
	 * 
	 * @param args
	 */
	
	/*
	public static void main(String[] args) 
	{	
		ArrayList<String> headers = new ArrayList<String>() {{
		    add("response1");
		    add("response2");
		    add("response3");
		}};
	
		// Initialise the class
		CSVFileOutput6 fo = new CSVFileOutput6("/home/kieran/Desktop/","foTest.csv",headers);
		
		// Some test simulation responses
		ArrayList<Double> exampleResponses = new ArrayList<Double>() {{
		    add(0.1245);
		    add(3.132);
		    add(2.15667);
		}};
		
		// Now write these responses
		fo.writeDataToFile(exampleResponses);
		
		
	}
*/
	
	
	
}
