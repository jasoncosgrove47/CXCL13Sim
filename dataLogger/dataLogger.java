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
import sim.util.Int2D;
import sim2d.SimulationEnvironment;
import sim2d.cell.Cell;
import sim2d.cell.impl.BCell;
import sim2d.cell.impl.CRC;
import sim2d.cell.impl.Centroblast;
import sim2d.cell.impl.Centrocyte;
import sim2d.cell.impl.FDC;
import sim2d.cell.impl.FRC;
import sim2d.cell.impl.TFh;
import sim2d.util.DrawGraph;


@SuppressWarnings("serial")
public class dataLogger implements Steppable {

	/**
	 * All data collection is handled through this class, it has functionality to track 
	 * populations of cell types and single cell tracking experiments.
	 * 
	 * TO BE INSTANTIATED ONCE STATICALLY IN THE SIMULATION ENVIRONMENT CLASS
	 */
	
	
	public DrawGraph cellCounterGraph;	
	private DrawGraph affinityGraph;
	//boolean outputToGraph;
	private static Map<String, Integer> cellCounter = new HashMap<String, Integer>();
	
	private static Map<String, Integer> singleCellTracker = new HashMap<String, Integer>();
	
	private static int[] meanAffinity;
	
	
	public dataLogger(){}
	
	//if being called by GUI then use this constructor
	public dataLogger(String title, Map<String, Color> cols){
		
		DrawGraph.SeriesAttributes[] attributes = new DrawGraph.SeriesAttributes[cols.size()];
		int i = 0;
		for(String key : cols.keySet())
		{
			attributes[i] = new DrawGraph.SeriesAttributes(key, cols.get(key), false);
			i++;
		}
		
		cellCounterGraph = new DrawGraph(title, "time", "cells", attributes);
		affinityGraph = new DrawGraph(title, "distanceFromOptimal", "cells");
	
		
		
	}

	public void step(SimState state) 
	{
		
		cellCounter = countCells(state);	
		meanAffinity = measureAffinity(state);

	
	}

	
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
	
	
	
	
	
	/**
	 * Also need to store the cells coordinates a
	 * @param state
	 * @return
	 */
	private Map<String, Integer> countCells(SimState state)
	{

		ArrayList<Cell> cells = new ArrayList<Cell>(); //add all the cells to the thingamabobby
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
	
		/* iterate over each cell in the simulation and calculate its type */
		for(Cell cell : cells)
		{
			int i;
			
			if(cell instanceof Centrocyte)
			{
				i = cellCounter.get("centrocyte");
				cellCounter.put("centrocyte", i+1);	
			}
			else if(cell instanceof Centroblast)
			{
				i = cellCounter.get("centroblast");
				cellCounter.put("centroblast", i+1);	
			}
			else if(cell instanceof TFh)
			{
				i = cellCounter.get("TFh");
				cellCounter.put("TFh", i+1);
			}
			else if(cell instanceof FRC)
			{
				//i =cellCounter.get("FRC");
				//cellCounter.put("FRC", i+1);
			}
			else if(cell instanceof FDC)
			{
				//i = cellCounter.get("FDC");
				//cellCounter.put("FDC", i+1);
			}
			else if(cell instanceof CRC)
			{
				//i = cellCounter.get("CRC");
				//cellCounter.put("CRC", i+1);
			}
		}
		
		//have to do this here, otherwise it won't update properly...
	  	//outputToGraph.getCellCounterGraph().logValue("centroblast", state.schedule.getTime(), cellCounter.get("centroblast"));
	  	//outputToGraph.getCellCounterGraph().logValue("centrocyte", state.schedule.getTime(), cellCounter.get("centrocyte"));
	  	//outputToGraph.getCellCounterGraph().logValue("TFh", state.schedule.getTime(), cellCounter.get("TFh"));
	  	//outputToGraph.getCellCounterGraph().logValue("FDC", state.schedule.getTime(), cellCounter.get("FDC"));
	  	//outputToGraph.getCellCounterGraph().logValue("FRC", state.schedule.getTime(), cellCounter.get("FRC"));
	  //	outputToGraph.getCellCounterGraph().logValue("CRC", state.schedule.getTime(), cellCounter.get("CRC"));
	
		for(String key : cellCounter.keySet())
		{	if (key != null && cellCounter.keySet() != null)
			{
				outputToGraph.getCellCounterGraph().logValue(key, state.schedule.getTime(), cellCounter.get(key));
			}
		}
		
		return cellCounter;
		
	}
		
	
	/**
	 * Should only be called on one cell
	 * @param cell
	 */
	private void trackCell(BCell cell)
	{
		singleCellTracker.put("ACKR3",cell.getACKR3expression() );
		singleCellTracker.put("ACKR4", cell.getACKR4expression());
		singleCellTracker.put("CXCR5", cell.getCXCR4expression());
		singleCellTracker.put("CXCR4",cell.getCXCR5expression());
		singleCellTracker.put("CCR7", cell.getCCR7expression());
		singleCellTracker.put("S1PR2", cell.getS1PR2expression());
		singleCellTracker.put("Affinity", cell.getAffinityScore());
	
	}
	
	
	
	/**
	 * Returns an array of all cells in the compartment. Used for collecting data on cell populations
	 * and drawing graphs.
	 */
	public Collection<Cell> getAllCells(Continuous2D cellGrid)
	{
		Bag b = cellGrid.allObjects;
		return new ArrayList<Cell>(b);			// do not return the bag itself, because modifying it is dangerous. 
	}
	
	/**
	 * Returns an array of all cells in the compartment. Used for collecting data on cell populations
	 * and drawing graphs.
	 */
	public Collection<Cell> getAllCells(ObjectGrid2D cellGrid)
	{
		Bag b = cellGrid.elements();
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

	public DrawGraph getAffinityGraph() {
		return affinityGraph;
	}

	public void setAffinityGraph(DrawGraph affinityGraph) {
		this.affinityGraph = affinityGraph;
	}


	
	
	
	
	
}
