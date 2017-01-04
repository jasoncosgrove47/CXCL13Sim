package sim3d.util;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import sim.util.Double3D;


public class ParseStromaLocations {

	/**
	 * This class is responsible for reading in the .csv containing stroma
	 * nodes and their locations obtained from the topological analysis. 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 */
	
	
	//step one read in the .csv file
	
	
	String csvFile_mrc = "/Users/jc1571/Desktop/mrcnodes.csv"; //make sure that the headers are removed
	String csvFile_brc = "/Users/jc1571/Desktop/brcnodes.csv";
	String csvFile_fdc = "/Users/jc1571/Desktop/fdcnodes.csv";
	
	
	
    @SuppressWarnings("resource")
	public static ArrayList<Double3D> readStroma (String csvFile) throws NumberFormatException, IOException {

    	
        BufferedReader br = null;
        String line = "";
        String csvSplitBy = ",";
        ArrayList<Double3D> locations = new ArrayList <Double3D>();
    	
        try {

        	br = new BufferedReader(new FileReader(csvFile));
        	while ((line = br.readLine()) != null) {

        		// use comma as separator
        		String[] country = line.split(csvSplitBy);
       
        		//convert to strings and divide by 10 as each gridspace equals 10 microns
        		double x = Double.parseDouble(country[0])/10 + 5; //add 5 so its not in the very corner
        		double y = Double.parseDouble(country[1])/10 + 5; //add 5 so its not in the very corner
        		double z = Double.parseDouble(country[2])/10;
        		Double3D coord = new Double3D(x, y, z);
        		
        		locations.add(coord);
        	
        }
        	return locations;

    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
		return locations; 
	
   }
    
    
   
	
}
