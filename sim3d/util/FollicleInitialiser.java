package sim3d.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;

/**
 * A Singleton class to generate follicular stroma
 * "Computational Approach to 3D Modeling of the Lymph Node Geometry",
 * Computation, 2015.
 * 
 * @author Jason Cosgrove
 */
public class FollicleInitialiser {
	
	
	/**
	 * an arraylist containing all unique edges 
	 * Sometimes the generator leads to overlapping edges
	 * so we remove these 
	 */
	static ArrayList<StromaEdge> m_edges;
	
	
	
	public static void initialiseFollicle(CollisionGrid cgGrid){
		//fit the SCS
		//lets try a different shape for the lols
		//actual data equals 9.5266687,0.5291553,0.0206240
		
		seedSCS(cgGrid);
	
		seedStroma(cgGrid,Stroma.TYPE.bRC);
		
		
		connectMRCNetwork(cgGrid);
		connectMRCtoRC(cgGrid);
		
	}
	
	
	/**
	 * Seed the cells at the subcapuslar sinus
	 * 
	 * iterate through the X and Z axes keeping Y fixed to seed the SCS
	 * The MRCs are generated stochastically: we iterate through the X and Z 
	 * axes keeping Y fixed to seed the SCS for each discrete location we
	 * generate a random number and if this exceeds some threshold value
	 * then we add an MRC location
	 * 
	 * @param cgGrid
	 */
	private static void seedSCS(CollisionGrid cgGrid) {
	
		// iterate through the X and Z axes keeping Y fixed to seed the SCS	
		for (int x = 0; x < Settings.WIDTH; x++) {
			for (int z = 1; z < Settings.DEPTH - 1; z++) {
				
				double y = 42.5672065 +(0.2289705 * x) - (0.0055798* Math.pow(x, 2));
				
				//seed the lymphatic endothelium
				Stroma flec = new Stroma(Stroma.TYPE.LEC);
				Stroma clec = new Stroma(Stroma.TYPE.LEC);

				clec.setObjectLocation(new Double3D(x, y, z));
				flec.setObjectLocation(new Double3D(x, y - Settings.bRC.SCSDEPTH, z));

				flec.registerCollisions(cgGrid);
				clec.registerCollisions(cgGrid);
				
				//now seed the MRCs
				double random = Settings.RNG.nextDouble();
				if(random > 0.6){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, y- (Settings.bRC.SCSDEPTH + 0.5), z));
					mrc.registerCollisions(cgGrid);
					SimulationEnvironment.scheduleStoppableCell(mrc);
				}
				
				//sometimes we see two MRCs overlapping so we do it again. 
				else if(random > 0.9){
					Stroma mrc = new Stroma(Stroma.TYPE.MRC);
					mrc.setObjectLocation(new Double3D(x, y- (Settings.bRC.SCSDEPTH + 0.9), z));
					mrc.registerCollisions(cgGrid);
					SimulationEnvironment.scheduleStoppableCell(mrc);
				}

			}
		}
	}

	
	
	private static void seedStroma(CollisionGrid cgGrid, Stroma.TYPE celltype){
		// Generate some stroma
		ArrayList<StromaGenerator.StromalCell> stromalCellLocations = 
				new ArrayList<StromaGenerator.StromalCell>();
		// this is for the dendrites
		ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>();

			int numOfCells = 0;
		
			if(celltype == Stroma.TYPE.FDC){
				numOfCells = Settings.FDC.COUNT;
			}
			else // its arecticular cell
			{
				numOfCells = Settings.bRC.COUNT;
			}



			StromaGenerator.generateStroma3D_Updated(Settings.WIDTH - 2,
					Settings.HEIGHT - (Settings.bRC.SCSDEPTH+2), Settings.DEPTH - 2,
					numOfCells, stromalCellLocations, sealEdges);
			
			
			
			m_edges = checkOverlaps(sealEdges);
			
			//set sealEdges to null so we can't use it again.
			sealEdges = null;
			
			
			if(celltype == Stroma.TYPE.bRC){
			
			//TODO need to adapt this so we can account for both RCs and FDCs
			// need to generate dendrites before nodes as we get rid of overlaps
			// having the nodes on the graph makes that a mess
				seedEdges(cgGrid, m_edges, StromaEdge.TYPE.RC_edge);
			
				for (StromaGenerator.StromalCell sc : stromalCellLocations) {
					seedRC(sc,cgGrid,m_edges, Stroma.TYPE.bRC);
				}	
			}
			else if(celltype == Stroma.TYPE.FDC){
				
				seedEdges(cgGrid, m_edges, StromaEdge.TYPE.FDC_edge);
				
				for (StromaGenerator.StromalCell sc : stromalCellLocations) {
					seedRC(sc,cgGrid,m_edges, Stroma.TYPE.FDC);
				}
				
			}
			
	
	}
	
	
	/**
	 * We generate the reticular network seperately to the FDC network
	 * To generate connections between the different cells as we observe 
	 * in vivo we make connections between stromal cells that are smaller
	 * than a threshold distance apart. 
	 * @param cgGrid
	 */
	private static void connectRCtoFDC(CollisionGrid cgGrid){
		
		Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.bRC) {
				
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = SimulationEnvironment.fdcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.7, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.FDC){

								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								SimulationEnvironment.scheduleStoppableCell(seEdge);
								
								seEdge.setStopper(SimulationEnvironment.simulation.schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
							}
						}
					}	
				}
			}
		}	
	}
	
	
	
	/**
	 * We generate the reticular network seperately to the FDC network
	 * To generate connections between the different cells as we observe 
	 * in vivo we make connections between stromal cells that are smaller
	 * than a threshold distance apart. 
	 * @param cgGrid
	 */
	private static void connectMRCNetwork(CollisionGrid cgGrid){
		
		Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {

					
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = SimulationEnvironment.fdcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.0, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.MRC){

								
								double random = Settings.RNG.nextDouble();
							
								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								SimulationEnvironment.scheduleStoppableCell(seEdge);
								
								seEdge.setStopper(SimulationEnvironment.simulation.schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
								
							}	
							
						}
					}	
				}
			}
		}	
	}
	
	
	
	/**
	 * We generate the reticular network seperately to the FDC network
	 * To generate connections between the different cells as we observe 
	 * in vivo we make connections between stromal cells that are smaller
	 * than a threshold distance apart. 
	 * @param cgGrid
	 */
	private static void connectMRCtoRC(CollisionGrid cgGrid){
		
		Bag stroma = SimulationEnvironment.fdcEnvironment.getAllObjects();

		// we want to count only branches and dendrites so
		// we need to know how many FDC nodes there are
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				if (((Stroma) stroma.get(i)).getStromatype() == Stroma.TYPE.MRC) {
				
					//get all the neighbours within 20 microns away
					Stroma sc = (Stroma) stroma.get(i);
					Double3D loc = new Double3D(sc.x,sc.y,sc.z);
					Bag neighbours = SimulationEnvironment.fdcEnvironment.getNeighborsExactlyWithinDistance(loc, 2.0, false);
					
					//if the type of stromal cell is an FDC then generate a new edge betwen them and
					//add to schedule etc
					for (int j = 0; j < neighbours.size(); j++) {
						if (neighbours.get(j) instanceof Stroma) {
							if (((Stroma) neighbours.get(j)).getStromatype() == Stroma.TYPE.bRC){

								Stroma neighbour = (Stroma) neighbours.get(j);
								Double3D neighbourloc = new Double3D(neighbour.x, neighbour.y,
										neighbour.z);
								StromaEdge seEdge = new StromaEdge(loc,neighbourloc,StromaEdge.TYPE.RC_edge);

								//assing the dendrite to the FRC
								//TODO should we also add this to the FDC. 
								sc.m_dendrites.add(seEdge);	
								seEdge.setObjectLocation(new Double3D(seEdge.x,
										seEdge.y , seEdge.z ));

								SimulationEnvironment.scheduleStoppableCell(seEdge);
								
								seEdge.setStopper(SimulationEnvironment.simulation.schedule.scheduleRepeating((Steppable) seEdge, 2, 1));		
								seEdge.registerCollisions(cgGrid);	
								
							}	
							
						}
					}	
				}
			}
		}	
	}
	
	
	



	
	/**
	 * Seed the reticular network from the arraylist generated by StromaGenerator3D
	 * @param sc
	 * @param cgGrid
	 * @param edges
	 */
	private static void seedRC(StromaGenerator.StromalCell sc,CollisionGrid cgGrid,ArrayList<StromaEdge> edges, Stroma.TYPE celltype){
			
		Stroma frc;
		
		if(celltype == Stroma.TYPE.bRC){
			frc = new Stroma(Stroma.TYPE.bRC);
		
		}
		else{
			frc = new Stroma(Stroma.TYPE.FDC);
		}
		
		
		Double3D loc = new Double3D(sc.d3Location.x+1,
				sc.d3Location.y +1, sc.d3Location.z +1);
		// This will register the FRC with the environment/display
		// to account for the border which is one gridspace in width
		frc.setObjectLocation(loc);
		SimulationEnvironment.scheduleStoppableCell(frc);
		
		
		//add associatedDendrites:TODO can encapsulate this as a seperate method
		for (StromaEdge seEdge : edges)
		{
			
			
			Double3D edgeloc = new Double3D(seEdge.getPoint1().x ,seEdge.getPoint1().y,seEdge.getPoint1().z);
			Double3D edgeloc2 = new Double3D(seEdge.getPoint2().x,seEdge.getPoint2().y,seEdge.getPoint2().z);
			
			if(loc.x == edgeloc.x && loc.y == edgeloc.y && loc.z == edgeloc.z)
			{
				  frc.m_dendrites.add(seEdge);		
			}
			else if(loc.x == edgeloc2.x && loc.y == edgeloc2.y && loc.z == edgeloc2.z)
			{
				  frc.m_dendrites.add(seEdge);		
			}
			
			double distance = calcDistance(loc,edgeloc);
			double distance2 = calcDistance(loc,edgeloc2);
			
			//sometimes they are very close but not perfectly in the same place. 
			//so we set some threshold distance, this was determined empirically
			double distanceThreshold = 0.1;
			if(distance < distanceThreshold || distance2 < distanceThreshold){
				frc.m_dendrites.add(seEdge);	
			}
		}
	}
		
	
	/**
	 * Remove an RC and its associated dendrites
	 * from the grid and the schedule
	 * @param frc
	 */
	private static void deleteRC(Stroma frc){
		
		//remove the frc from grid and schedule
		frc.getDrawEnvironment().remove(frc);
		frc.stop();
		
		//now this for all associated dendrites. 
		for(int i = 0; i < frc.m_dendrites.size(); i ++){			
			frc.m_dendrites.get(i).getDrawEnvironment()
				.remove(frc.m_dendrites.get(i));
			frc.m_dendrites.get(i).stop();

		}
	}

	
	/**
	 * CHECK THE STROMAEDGE ARRAY TO MAKE SURE THERE ARE NO DUPLICATES
	 * 
	 * When generated stroma we can get overlapping edges, this method
	 * checks for overlaps and returns a new arraylist with all duplicated
	 * edges removed. 
	 * 
	 * TODO: might be cleaner to have this in the generate stroma class
	 * @param sealEdges
	 * @return
	 */
	private static ArrayList<StromaEdge> checkOverlaps(ArrayList<StromaEdge> sealEdges){

		ArrayList<StromaEdge> updatedEdges = sealEdges;
		ArrayList<StromaEdge> edgesToRemove = new ArrayList<StromaEdge>();

		for (int i = 0; i < sealEdges.size(); i++) {
			  for (int j = i+1; j < sealEdges.size(); j++) {
				  
				  //get the end points of each of the two edges
				  Double3D i_p1 = new Double3D(sealEdges.get(i).x +1,
						  sealEdges.get(i).y+1, sealEdges.get(i).z +1);
				  Double3D i_p2 = new Double3D(sealEdges.get(i).getPoint2().x +1,
						  sealEdges.get(i).getPoint2().y+1, sealEdges.get(i).getPoint2().z +1);
					
				  Double3D j_p1 = new Double3D(sealEdges.get(j).x +1,
						  sealEdges.get(j).y+1, sealEdges.get(j).z +1);
				  Double3D j_p2 = new Double3D(sealEdges.get(j).getPoint2().x +1,
						  sealEdges.get(j).getPoint2().y+1, sealEdges.get(j).getPoint2().z +1);
				  
				//calculate the distances between all of the points. 
				double d1 = calcDistance(i_p1, j_p1);
				double d2 = calcDistance(i_p1, j_p2);
				double d3 = calcDistance(i_p2, j_p1);
				double d4 = calcDistance(i_p2, j_p2);
				
				//if the cells are too close to one another then remove them
				// the threshold value was determined by trial and error
				double thresholdDistance = 0.15; 
				
				if(d1 < thresholdDistance && d4 < thresholdDistance){
					edgesToRemove.add(sealEdges.get(i));
				}
				else if(d2 < thresholdDistance && d3 < thresholdDistance){
					edgesToRemove.add(sealEdges.get(i));
				}
			}
		}

		//now remove all of the overlapping edges in the removal list
		for(int x = 0; x < edgesToRemove.size(); x ++){

			updatedEdges.remove(edgesToRemove.get(x));
		}
		//return the updated array
		return updatedEdges;
	}
	
	
	
	/**
	 * This method takes the stroma edges contained generated by
	 * StromaGenerator3D and places them on the stroma and collision
	 * grid as well as adding them to the schedule. 
	 * @param cgGrid
	 * @param edges
	 * @param type
	 */
	private static void seedEdges(CollisionGrid cgGrid,
			ArrayList<StromaEdge> edges, StromaEdge.TYPE type) {

			for (StromaEdge seEdge : edges) {
				
					//seEdge.setM_col(Settings.bRC.DRAW_COLOR());
					// Register with display and CG
					seEdge.setObjectLocation(new Double3D(seEdge.x +1,
							seEdge.y +1, seEdge.z +1));

					SimulationEnvironment.scheduleStoppableCell(seEdge);	
					seEdge.registerCollisions(cgGrid);			
				
			}
	}
	

	//TODO DRY: this is in the other class: should we make a generic methods class to make this cleaner...
	protected static double calcDistance(Double3D i3Point1, Double3D i3Point2) {
		return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2)
				+ Math.pow(i3Point1.y - i3Point2.y, 2)
				+ Math.pow(i3Point1.z - i3Point2.z, 2)));
	}
	
	
	/**
	 * Prune the network, we sometimes gets nodes with no edges because we chop out the centre
	 * so this method handles all of this
	 */
	private static void shapeNetwork(){		
		
		 // From the MASON manual: The provided Bag is to be treated as read-only and not to be modified,
		 //	and it may change at any time without warning.
		 // therefore do not edit a bag whilst iterating through!!!!!!!
		Bag fdcs = new Bag(SimulationEnvironment.fdcEnvironment.getAllObjects());

		
		for(int i = 0; i < fdcs.size();i++){
			
			if(fdcs.get(i) instanceof Stroma){
				Stroma fdc = (Stroma) fdcs.get(i);
	
				if (fdc.getStromatype() == Stroma.TYPE.FDC){
					//get rid of any FDCs which arent connected to any dendrites
					if(fdc.m_dendrites.size() == 0){
						fdc.getDrawEnvironment().remove(fdc);
						fdc.stop();
					}					
				}		
				else if(fdc.getStromatype() == Stroma.TYPE.bRC){

					//get rid of any FDCs which arent connected to any dendrites
					if(fdc.m_dendrites.size() == 0){
						deleteRC(fdc);
					}
					
					if(SimulationEnvironment.isWithinCircle(fdc.x, fdc.y, (Settings.WIDTH / 2),
							(Settings.HEIGHT / 2) , 13)){
						deleteRC(fdc);
					}
				}
			}	
		}
	}

	
	
	
}