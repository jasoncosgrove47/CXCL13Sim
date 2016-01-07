package dataLogger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.Int2D;
import sim3d.SimulationEnvironment;
import sim3d.consoleRun;
import sim3d.cell.cognateBC;
import sim3d.cell.cognateBC.TYPE;


@SuppressWarnings("serial")
public class Controller implements Steppable {

	/**
	 * All data collection is handled through this class, it has functionality to track 
	 * populations of cell types and single cell tracking experiments.
	 * 
	 * Basis is the MVC design pattern: http://www.tutorialspoint.com/design_pattern/mvc_pattern.htm
	 * 
	 * Model: Each cell type is responsible for maintaining it's own data in the form of a MAP
	 * 
	 * View:OutputToCSV or grapher modules display the data
	 * 
	 * Controller: DataLogger handles all of the dataCollection in the system
	 * 
	 * need to update so all of the relevant B cell getters and setters are in this class
	 */
	
	

	//boolean outputToGraph;
	private static Map<String, Integer> cellCounter = new HashMap<String, Integer>();
	
	private static Map<String, Integer> singleCellTracker = new HashMap<String, Integer>();
	
	private static int[] meanAffinity;
	
	private static int primedCells = 0;
	
	
	 //now we need a map where we store the index and systematically update the X,Y and Z coordinates
    private Map<Integer,ArrayList<Double>> X_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Y_Coordinates = new HashMap<Integer,ArrayList<Double>>();
    private Map<Integer,ArrayList<Double>> Z_Coordinates = new HashMap<Integer,ArrayList<Double>>();
	
	
	public Controller(){
		//initalise the MAP
		X_Coordinates.put(0, new ArrayList<Double>());
		Y_Coordinates.put(0, new ArrayList<Double>());
		Z_Coordinates.put(0, new ArrayList<Double>());
	}
	


	public void step(SimState state) 
	{
		
		//cellCounter = countCells(state);	
		//meanAffinity = measureAffinity(state);
		setPrimedCells(trackAntigenAcquisition());
	
	}

	private static int trackAntigenAcquisition()
	{
		
	    Bag cells =  consoleRun.simulation.bcEnvironment.allObjects;;
		
		int primedCount = 0;

		
	    for(int i = 0; i < cells.size(); i++)
	    {
	    	if(cells.get(i) instanceof cognateBC)
			{
				cognateBC cBC = (cognateBC) cells.get(i);
				if(cBC.type == TYPE.PRIMED)
				{
					primedCount +=1;
				}
			}
	    }
	    
	    return primedCount;
	}


	
	    




	public static int getPrimedCells() {
		return primedCells;
	}



	public static void setPrimedCells(int primedCells) {
		Controller.primedCells = primedCells;
	}



	public Map<Integer,ArrayList<Double>> getX_Coordinates() {
		return X_Coordinates;
	}







	public Map<Integer,ArrayList<Double>> getY_Coordinates() {
		return Y_Coordinates;
	}






	public Map<Integer,ArrayList<Double>> getZ_Coordinates() {
		return Z_Coordinates;
	}




	
	/**
	public int[] measureAffinity(SimState state){
		
		ArrayList<Cell> GCBcells = new ArrayList<Cell>(); //add all the cells to the thingamabobby
		GCBcells.addAll(getAllCells(SimulationEnvironment.getBCellGrid()));
		int totalAffinity = 0;
		int numberOfCentrocytes =0;
		int CBAffinity = 0;
		int numberOfCBs =0;
		int[] output = new int[]{0,0};
		
		for(Cell cell : GCBcells)
		{
			if(cell instanceof Centrocyte)
			{
				Centrocyte cc = (Centrocyte) cell;
				totalAffinity += cc.getAffinityScore();
				numberOfCentrocytes +=1;
			}
			
			if(cell instanceof Centroblast)
			{
				Centroblast cb = (Centroblast) cell;
				CBAffinity += cb.getAffinityScore();
				numberOfCBs +=1;
			}
			
		}
		
		if(numberOfCentrocytes > 0)
		{
			//outputToGraph.getAffinityGraph().logValue("CC distance from optimal", state.schedule.getTime(), totalAffinity/numberOfCentrocytes);
			output[0] =  totalAffinity/numberOfCentrocytes;
		}
		
		
		
		if(numberOfCBs > 0)
		{
			outputToGraph.getAffinityGraph().logValue("CB distance from optimal", state.schedule.getTime(), CBAffinity/numberOfCBs);
			output[1] = CBAffinity/numberOfCBs;
		}
		else
		{
			//outputToGraph.getAffinityGraph().logValue("CC distance from optimal", state.schedule.getTime(), 0);
			outputToGraph.getAffinityGraph().logValue("CB distance from optimal", state.schedule.getTime(), 0);
			output[0] = 0;
			output[1] = 0;
		}
		return output;
		
	}
	
		
	
	public int[] trackMigration(SimState state){
		
		ArrayList<Cell> GCBcells = new ArrayList<Cell>(); //add all the cells to the thingamabobby
		GCBcells.addAll(getAllCells(SimulationEnvironment.getBCellGrid()));
		int[] coordinates = null;
		
		//i really need a MAP and then for every cell i get their locaiton on the map and add coordinates to their int array
		
		
		
		for(Cell cell : GCBcells)
		{
			if(cell instanceof Centrocyte)
			{
				Centrocyte cc = (Centrocyte) cell;
				Int2D myLocation = cc.getDiscretizedLocation(SimulationEnvironment.getBCellGrid());
				coordinates[0] = myLocation.x;
				coordinates[1] = myLocation.y;
				
			}
			
			if(cell instanceof Centroblast)
			{
				Centroblast cb = (Centroblast) cell;
				Int2D myLocation = cb.getDiscretizedLocation(SimulationEnvironment.getBCellGrid());
				coordinates[0] = myLocation.x;
				coordinates[1] = myLocation.y;
			}
			
		}
		
	return coordinates;
		
	}
	*/
	

	
	/**
	 * Also need to store the cells coordinates a
	 * @param state
	 * @return
	 */
	
	/**
	private Map<String, Integer> countCells(SimState state)
	{

		ArrayList<Object> cells = new ArrayList<Object>(); //add all the cells to the thingamabobby
		Map<String, Integer> cellCounter = new HashMap<String, Integer>();
		
		cells.addAll(getAllCells(SimulationEnvironment.getStromalGrid())); //add all cells from each grid
		cells.addAll(getAllCells(SimulationEnvironment.getBCellGrid()));
		cells.addAll(getAllCells(SimulationEnvironment.getTCellGrid()));

		//Map<String, Integer> c = new HashMap<String, Integer>();
	
		cellCounter.put("centroblast", 0);
		cellCounter.put("centrocyte", 0);
		cellCounter.put("TFh", 0);
		//cellCounter.put("FRC", 0);
		//cellCounter.put("CRC", 0);
		//cellCounter.put("FDC", 0);
	

		for(Object cell : cells)
		{
			int i;
			
			if(cell instanceof cognateBC)
			{
				i = cellCounter.get("centrocyte");
				cellCounter.put("centrocyte", i+1);	
			}
		
		}
		
	
		
		return cellCounter;
		
	}
		*/
	


	
	
/**
	public Collection<Cell> getAllCells(Continuous2D cellGrid)
	{
		Bag b = cellGrid.allObjects;
		return new ArrayList<Cell>(b);			// do not return the bag itself, because modifying it is dangerous. 
	}
	
	
	
	
	
	public Map<String, Integer> getCellCounts() 
	{
		// creating a dataloggerSystem.out.println("cellCounter is" + cellCounter);
		return cellCounter;
	}
	public void setCellCounts(Map<String, Integer> c) {dataLogger.cellCounter = c;}

	public static int[] getMeanAffinity() {
		return meanAffinity;
	}

	public static void setMeanAffinity(int[] meanAffinity) {
		dataLogger.meanAffinity = meanAffinity;
	}

*/

	
	
	
	
	
}
