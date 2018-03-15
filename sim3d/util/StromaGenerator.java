package sim3d.util;

import java.util.ArrayList;
import java.util.List;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.stroma.Stroma;
import sim3d.stroma.Stroma.TYPE;
import sim3d.stroma.StromaEdge;

/**
 * A Singleton class implementing the method of generating stroma proposed by
 * Kislitsyn et al. in
 * "Computational Approach to 3D Modeling of the Lymph Node Geometry",
 * Computation, 2015.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 * @author Jason Cosgrove- {@link simonjjarrett@gmail.com}
 */
public class StromaGenerator {
	
	/**
	 * Class to keep track of edges for each cell
	 * call it temp so we dont get confused with 
	 * the other stromal cell class
	 */
	public static class StromalCelltemp {
		/**
		 * Location of the cell
		 */
		public Double3D m_d3Location;

		/**
		 * The number of edges (dendrites) the cell has
		 */
		public int m_iEdges = 0;

		/**
		 * A list containing all the edges
		 * 
		 * NB: this will only be accurate for cells that haven't been generated.
		 * We're only interested in these because they are needed to generate
		 * the directions
		 */
		public ArrayList<Double3D> d3lEdges = new ArrayList<Double3D>();

		
		
		public Stroma.TYPE m_type;
		
		/**
		 * Constructor
		 * 
		 * @param x
		 *            X position of the cell
		 * @param y
		 *            Y position of the cell
		 * @param z
		 *            Z position of the cell
		 */
		public StromalCelltemp(double x, double y, double z, Stroma.TYPE type) {
			m_d3Location = new Double3D(x, y, z);
			this.m_type = type;
		}

		/**
		 * Constructor
		 * 
		 * @param d3Position
		 *            Position of the cell
		 */
		public StromalCelltemp(Double3D d3Position) {
			m_d3Location = d3Position;
		}
	}

	


	/**
	 * 
	 * When generated stroma we can get overlapping edges, this method
	 * checks for overlaps and returns a new arraylist with all duplicated
	 * edges removed. 
	 * 
	 * @param sealEdges
	 * @return
	 */
	private static List<StromaEdge> removeOverlappingEdges(List<StromaEdge> sealEdges){

		List<StromaEdge> updatedEdges = sealEdges;
		List<StromaEdge> edgesToRemove = new ArrayList<StromaEdge>();

		for (int i = 0; i < sealEdges.size(); i++) {
			  for (int j = i+1; j < sealEdges.size(); j++) {
				  
				  //get the end points of each of the two edges
				  Double3D i_p1 = new Double3D(sealEdges.get(i).getPoint1().x +1,
						  sealEdges.get(i).getPoint1().y+1, sealEdges.get(i).getPoint1().z +1);
				  Double3D i_p2 = new Double3D(sealEdges.get(i).getPoint2().x +1,
						  sealEdges.get(i).getPoint2().y+1, sealEdges.get(i).getPoint2().z +1);
					
				  Double3D j_p1 = new Double3D(sealEdges.get(j).getPoint1().x +1,
						  sealEdges.get(j).getPoint1().y+1, sealEdges.get(j).getPoint1().z +1);
				  Double3D j_p2 = new Double3D(sealEdges.get(j).getPoint2().x +1,
						  sealEdges.get(j).getPoint2().y+1, sealEdges.get(j).getPoint2().z +1);
				  
				//calculate the distances between all of the points. 
				double d1 = calcDistance(i_p1, j_p1);
				double d2 = calcDistance(i_p1, j_p2);
				double d3 = calcDistance(i_p2, j_p1);
				double d4 = calcDistance(i_p2, j_p2);
				
				//if the cells are too close to one another then remove them
				// the threshold value was determined by trial and error
				double thresholdDistance = Settings.DOUBLE3D_PRECISION; 
				
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
	 * Generates a stromal network and returns the nodes in a 3D boolean array.
	 * 
	 * This is for an FDC NETWORK
	 * 
	 * @param iWidth
	 *            Width of grid
	 * @param iHeight
	 *            Height of grid
	 * @param iDepth
	 *            Depth of grid
	 * @param iCellCount
	 *            Max number of stromal cells (note: this is a upper bound only)
	 * @return 
	 */
	public static int generateStroma3D_Updated(int iWidth, int iHeight, int iDepth,
			int iCellCount, ArrayList<StromalCelltemp> stromalCellLocations,
			List<StromaEdge> selEdges) {
		// It will be efficient to keep track of cells and locations separately
		@SuppressWarnings("unchecked")
		// because it is of an abstract interface
		ArrayList<StromalCelltemp>[][][] stromala3CellLocations = new ArrayList[iWidth][iHeight][iDepth];
		ArrayList<StromalCelltemp> stromalUnbranchedCells = new ArrayList<StromalCelltemp>();

		for (int x = 0; x < iWidth; x++) {
			for (int y = 0; y < iHeight; y++) {
				for (int z = 0; z < iDepth; z++) {
					stromala3CellLocations[x][y][z] = new ArrayList<StromalCelltemp>();
				}
			}
		}


		StromalCelltemp stromalInitialCell = new StromalCelltemp(iWidth / 2.0, iHeight / 2.0,
				iDepth / 2.0, Stroma.TYPE.FDC);
		stromalUnbranchedCells.add(stromalInitialCell);
		stromala3CellLocations[iWidth / 2][iHeight / 2][iDepth / 2]
				.add(stromalInitialCell);

		int iRemainingCells = iCellCount - 1;

		while (iRemainingCells > 0 && stromalUnbranchedCells.size() > 0) {
			
			
			//need to make sure that there are no overlaps at this point
			
			StromalCelltemp nextCell = pickNextCell(iWidth, iHeight, iDepth,
					stromalUnbranchedCells, stromala3CellLocations);

			if (nextCell == null) {
				break;
			}
			
			
			Stroma.TYPE celltype;
			StromaEdge.TYPE edgetype;
			
			//if its an FDC
			if(SimulationEnvironment.isWithinCircle(nextCell.m_d3Location.x, nextCell.m_d3Location.y, 
					(Settings.WIDTH / 2) ,(Settings.HEIGHT / 2) , SimulationEnvironment.fdcNetRadius)){
				
				celltype = Stroma.TYPE.FDC;
				edgetype = StromaEdge.TYPE.FDC_edge;
			
			}
			else{ //its a reticular cell
				
				celltype = Stroma.TYPE.bRC;
				edgetype = StromaEdge.TYPE.RC_edge;
				
			}

			// Calculate the number of edges to make
			// Values were fitted to match the FRC paper
			int iEdges = calculateEdgeNumber(celltype, iRemainingCells,nextCell);
				

		
			
			// This is the first time so we want at few edges otherwise
			// generation will break sometimes
			if (iRemainingCells == iCellCount - 1) {
				iEdges++;
			}
			
		
			// if we have edges to add
			if (iEdges > 0) {
				// Get some directions
				Double3D[] d3aDirections;
								
				d3aDirections = generateDirections_Updated(iWidth, iHeight,
						iDepth, stromala3CellLocations, nextCell, iEdges, celltype);
				

				// Create some cells at this direction
				iRemainingCells -= createNewCells(iWidth, iHeight, iDepth,
						stromalUnbranchedCells, stromala3CellLocations, nextCell,
						d3aDirections);

				for (Double3D d3Direction : d3aDirections) {

					if (d3Direction != null) {
						// Add the edges

						selEdges.add(new StromaEdge(nextCell.m_d3Location,
								new Double3D(nextCell.m_d3Location.x
										+ d3Direction.x,
										nextCell.m_d3Location.y
												+ d3Direction.y,
										nextCell.m_d3Location.z
												+ d3Direction.z), edgetype));
						}	
				}
			}
		
			//do a final check so there are no overlaps
			boolean include = true;
			for(int x = 0; x < stromalCellLocations.size(); x++){
				
				if(calcDistance(nextCell.m_d3Location,
						stromalCellLocations.get(x).m_d3Location) < 0.1){
					include = false;
				}
			}
			
			// Move the cell to the locations list
			
			if(include){
				
			stromalCellLocations.add(nextCell);
			}
			
			stromalUnbranchedCells.remove(nextCell);
		}
		
		//now we need to process the stroma making sure there are no overlapping edges or zero length edges
		selEdges = removeOverlappingEdges(selEdges);
		

		
		return iCellCount - iRemainingCells;
	}
	

	
	
	/**
	 * Generates the directions in which we should create cells, making sure
	 * that the directions follow the pattern seen in stroma
	 * 
	 * @param iWidth
	 *            Width of the space
	 * @param iHeight
	 *            Height of the space
	 * @param iDepth
	 *            Depth of the space
	 * @param frcla3CellLocations
	 *            3D array of lists of cell locations
	 * @param frcLocation
	 *            Location of the cell to expand from
	 * @param iCellCount
	 *            Number of cells to create
	 * @return An array of vectors with the new locations relative to the given
	 *         cell
	 */
	protected static Double3D[] generateDirections_Updated(int iWidth, int iHeight,
			int iDepth, ArrayList<StromalCelltemp>[][][] frcla3CellLocations,
			StromalCelltemp frcLocation, int iCellCount, Stroma.TYPE celltype) {
		Double3D[] d3aReturn = new Double3D[iCellCount];

		boolean bFail = false;

		do {
			bFail = false;

			//where we store the new locations
			d3aReturn[0] = new Double3D();

			// account for the edges already going to this cell
			for (Double3D d3Point : frcLocation.d3lEdges) {
				d3aReturn[0].subtract(d3Point);
			}

			
			//for each cell, determine a suitable length
			// make sure there are no cells which are too close
			
			
			for (int i = 1; i < iCellCount; i++) {

				// This distribution... It approximately matches the paper, and
				// was derived using the newton divided differences method giving the following function
				// http://www.wolframalpha.com/input/?i=0.392281+x-0.342923+x%5E2%2B0.151204+x%5E3-0.0270696+x%5E4%2B0.00180148+x%5E5+between+x+%3D+0+and+5
				// divided differences allows you to interplaote a continuous
				// distribution from discrete data

				//very useful approach: see the following for more details
				// http://mathforcollege.com/nm/mws/gen/05inp/mws_gen_inp_txt_ndd.pdf
				//http://ww2.odu.edu/~agodunov/teaching/notes/Nm01_interpolation.pdf
				double length = calculateEdgeLength(celltype);
	
			
				// 2D special case
				if (iDepth == 1) {
					d3aReturn[i] = Vector3DHelper.getRandomDirection();
					//what is this line doing?
					d3aReturn[i] = new Double3D(d3aReturn[i].x, d3aReturn[i].y,
							0).normalize().multiply((length + 1) * 3);
				} else {
					d3aReturn[i] = Vector3DHelper.getRandomDirection()
							.multiply(length);
				}

				
				
				// If there's a cell there, try again..?
				if (getAdjacentCells(iWidth, iHeight, iDepth,
						frcla3CellLocations,
						new StromalCelltemp(frcLocation.m_d3Location.add(d3aReturn[i])),
						1.0).size() > 0) {
					
					i--;//what is this doing?
					continue;
				}

				// Make sure we don't collide with other edges already made
				boolean bCollision = false;
				for (int j = 0; j < i; j++) {
					if (calcDistance(d3aReturn[i], d3aReturn[j]) < 1.0) {
						bCollision = true;
						break;
					}
				}
				if (bCollision) {
					i--;
					continue;
				}

				d3aReturn[0] = d3aReturn[0].subtract(d3aReturn[i]);
			}

			// add some noise
			d3aReturn[0] = d3aReturn[0].add(Vector3DHelper.getRandomDirection()
					.multiply(Settings.RNG.nextDouble() * 0.04));

			// If there are any cells too close to the final edge, try it all
			// again!
			if (getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations,
					new StromalCelltemp(frcLocation.m_d3Location.add(d3aReturn[0])), 1.2)
					.size() > 0) {
				bFail = true;
				continue;
			}

			// Again, avoid self collision
			boolean bCollision = false;
			for (int i = 1; i < iCellCount; i++) {
				if (calcDistance(d3aReturn[i], d3aReturn[0]) < 1.0) {
					bCollision = true;
					break;
				}
			}
			if (bCollision) {
				bFail = true;
				continue;
			}

			// just check we aren't making a huge edge!
		} while (!bFail && d3aReturn[0].length() > 5.2 
				&& d3aReturn[0].length() < 1.0);

		
		return d3aReturn;
	}
	
	

	
	private static int calculateEdgeNumber(Stroma.TYPE celltype, int iRemainingCells, StromalCelltemp frcNextCell){
		
		//lets add a plus one given these cells are denser than the FRCs
		
		 if(celltype == Stroma.TYPE.bRC){

			 int target = 0;
			 if(Settings.RNG.nextDouble() > 0.5){
				 target = 3;//was 3
			 }
			 else{
				 target = 4;//was 4
			 }
			 
				return Math.max(0,Math.min(iRemainingCells,
					target)// was 2 + 3
				 		- frcNextCell.m_iEdges);
			 
		 }
		 
		 //add 3 to the number of FDC edges as they have much higher connectivity
		 else if(celltype == Stroma.TYPE.FDC){
			 

			 int target;
			 if(Settings.RNG.nextDouble() > 0.4){
				 target = 3;
			 }
			 else{
				 target = 4;
			 }
			 
			 return Math.max(0,Math.min(iRemainingCells,
						target)- frcNextCell.m_iEdges);
	
		 }
		 
		 return 0;
	}
	
	private static double calculateEdgeLength(Stroma.TYPE celltype){
		
		 if(celltype == Stroma.TYPE.bRC){
				
				return Settings.RNG.nextGaussian()*0.1 + 3.9;
				
		 }
		 else if(celltype == Stroma.TYPE.FDC){
		
				return Settings.RNG.nextGaussian()*0.3 + 5.2;	//in generate directions we have a limit on this value, we need to make sure that these two are consistent
				
		 }
		 
		 return 0;
	}
	


	
	/**
	 * Get all cells within a certain distance
	 * 
	 * @param iWidth
	 *            Width of the space
	 * @param iHeight
	 *            Height of the space
	 * @param iDepth
	 *            Depth of the space
	 * @param frcla3CellLocations
	 *            3D array of lists of cell locations
	 * @param frcPoint
	 *            Point to search from
	 * @param iMaxDistance
	 *            Max distance of cells from the point
	 * @return A list of cells within the specified distance from the point
	 */
	public static ArrayList<StromalCelltemp> getAdjacentCells(int iWidth, int iHeight,
			int iDepth, ArrayList<StromalCelltemp>[][][] frcla3CellLocations,
			StromalCelltemp frcPoint, double iMaxDistance) {
		ArrayList<StromalCelltemp> frclReturn = new ArrayList<StromalCelltemp>();

		// Precompute these for efficiency
		// +1 because we want the distance between the close edges of the cells
		int iXLim = (int) Math.min(iMaxDistance + frcPoint.m_d3Location.x,
				iWidth - 1);
		int iYLim = (int) Math.min(iMaxDistance + frcPoint.m_d3Location.y,
				iHeight - 1);
		int iZLim = (int) Math.min(iMaxDistance + frcPoint.m_d3Location.z,
				iDepth - 1);

		for (int x = (int) Math.max(0, frcPoint.m_d3Location.x - iMaxDistance); x <= iXLim; x++) {
			for (int y = (int) Math
					.max(0, frcPoint.m_d3Location.y - iMaxDistance); y <= iYLim; y++) {
				for (int z = (int) Math.max(0, frcPoint.m_d3Location.z
						- iMaxDistance); z <= iZLim; z++) {
					if (x == frcPoint.m_d3Location.x
							&& y == frcPoint.m_d3Location.y
							&& z == frcPoint.m_d3Location.z) {
						continue;
					}
					// if a cell lives at this location,
					// and the distance is less than the max distance
					// we take one away from the max distance because we want
					// the distance between the closer edges
					for (StromalCelltemp frcCollisionPoint : frcla3CellLocations[x][y][z]) {
						if (calcDistance(frcPoint, frcCollisionPoint) <= iMaxDistance) {
							frclReturn.add(frcCollisionPoint);
						}
					}
				}
			}
		}

	

		return frclReturn;
	}

	/**
	 * Helper function to calculate the distance between two points
	 * 
	 * @param i3Point1
	 *            The first point
	 * @param i3Point2
	 *            The second point
	 * @return The distance between the points
	 */
	protected static double calcDistance(Double3D i3Point1, Double3D i3Point2) {
		return (Math.sqrt(Math.pow(i3Point1.x - i3Point2.x, 2)
				+ Math.pow(i3Point1.y - i3Point2.y, 2)
				+ Math.pow(i3Point1.z - i3Point2.z, 2)));
	}

	/**
	 * Helper function to calculate the distance between two FRCCells
	 * 
	 * @param i3Point1
	 *            The first cell
	 * @param i3Point2
	 *            The second cell
	 * @return The distance between the cells
	 */
	protected static double calcDistance(StromalCelltemp i3Point1, StromalCelltemp i3Point2) {
		return calcDistance(i3Point1.m_d3Location, i3Point2.m_d3Location);
	}

	/**
	 * Add cells to the grid and add the edges to reach them
	 * 
	 * @param iWidth
	 *            Width of the space
	 * @param iHeight
	 *            Height of the space
	 * @param iDepth
	 *            Depth of the space
	 * @param frclCellLocations
	 *            List of all cell locations
	 * @param frcla3CellLocations
	 *            3D array of lists of cell locations
	 * @param frcOrigin
	 *            The point to add cells from
	 * @param d3aDirections
	 *            The directions to add cells towards
	 * @return The number of cells actually created
	 */
	protected static int createNewCells(int iWidth, int iHeight, int iDepth,
			ArrayList<StromalCelltemp> frclCellLocations,
			ArrayList<StromalCelltemp>[][][] frcla3CellLocations, StromalCelltemp frcOrigin,
			Double3D[] d3aDirections) {
		
	
		int iCellsCreated = 0;

		for (int i = 0; i < d3aDirections.length; i++) {
			double x, y, z;
			x = frcOrigin.m_d3Location.x + d3aDirections[i].x;
			y = frcOrigin.m_d3Location.y + d3aDirections[i].y;
			z = frcOrigin.m_d3Location.z + d3aDirections[i].z;
			
			Stroma.TYPE type = determineStromaType(x,y);
			

			StromalCelltemp frcNewPoint = new StromalCelltemp(x, y, z,type);

			// check if out of bounds
			if (x < 0 || x >= iWidth || y < 0 || y >= iHeight || z < 0
					|| z >= iDepth) {
				double dCoeff = 1;
				if (x < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.m_d3Location.x
							/ d3aDirections[i].x);
				} else if (x > iWidth) {
					dCoeff = Math.min(dCoeff, (iWidth - frcOrigin.m_d3Location.x)
							/ d3aDirections[i].x);
				}

				if (y < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.m_d3Location.y
							/ d3aDirections[i].y);
				} else if (y > iHeight) {
					dCoeff = Math.min(dCoeff,
							(iHeight - frcOrigin.m_d3Location.y)
									/ d3aDirections[i].y);
				}

				if (z < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.m_d3Location.z
							/ d3aDirections[i].z);
				} else if (z > iDepth) {
					dCoeff = Math.min(dCoeff, (iDepth - frcOrigin.m_d3Location.z)
							/ d3aDirections[i].z);
				}


				// Set it to null so the parent knows it's not been created
				d3aDirections[i] = null;


				frcOrigin.m_iEdges++;

				continue;
			}

			// Check if there already exists a cell within 1.6 grid spaces
			// If we don't do this, the grid is ridiculously dense
			ArrayList<StromalCelltemp> d3lAdjacent = getAdjacentCells(iWidth, iHeight,
					iDepth, frcla3CellLocations, frcNewPoint, 1.6);
			
			

			// If there is one (and it isn't itself)
			if (d3lAdjacent.size() > 1
					|| (d3lAdjacent.size() == 1 && d3lAdjacent.get(0) != frcOrigin)) {
				// Pick a random one (probably only one)
				StromalCelltemp newLoc = d3lAdjacent.get(Settings.RNG
						.nextInt(d3lAdjacent.size()));
				

				while (newLoc == frcOrigin ) {
					newLoc = d3lAdjacent.get(Settings.RNG.nextInt(d3lAdjacent
							.size()));
					
				}

				// draw an edge to the existing cell
				d3aDirections[i] = new Double3D(newLoc.m_d3Location.x
						- frcOrigin.m_d3Location.x, newLoc.m_d3Location.y
						- frcOrigin.m_d3Location.y, newLoc.m_d3Location.z
						- frcOrigin.m_d3Location.z);

				frcNewPoint = newLoc;
			} else {
				// add a new cell
				frclCellLocations.add(frcNewPoint);
				frcla3CellLocations[(int) x][(int) y][(int) z].add(frcNewPoint);
				iCellsCreated++;
			}

			// We only really need to tell the new point, not the origin,
			// because this is only used when
			// generating directions to add new points
			frcNewPoint.d3lEdges.add(d3aDirections[i].negate());

			// tell the FRCCells of the new edges
			frcNewPoint.m_iEdges++;
			frcOrigin.m_iEdges++;
		}

		return iCellsCreated;
	}


	


	/**
	 * Determines which cell to expand from next. Currently expands from the one
	 * closest to the origin
	 * 
	 * @param iWidth
	 *            Width of the space
	 * @param iHeight
	 *            Height of the space
	 * @param iDepth
	 *            Depth of the space
	 * @param frclCellLocations
	 *            List of all cell locations
	 * @param frcla3CellLocations
	 *            3D array of lists of cell locations
	 * @return
	 */
	protected static StromalCelltemp pickNextCell(int iWidth, int iHeight, int iDepth,
			ArrayList<StromalCelltemp> frclCellLocations,
			ArrayList<StromalCelltemp>[][][] frcla3CellLocations) {
		if (frclCellLocations.size() == 0) {
			return null;
		}

		//The origin cell is always an FDC
		StromalCelltemp frcOrigin = new StromalCelltemp(iWidth / 2.0, iHeight / 2.0,
				iDepth / 2.0, Stroma.TYPE.FDC);
		StromalCelltemp frcClosest = frclCellLocations.get(0);
		double dDist = calcDistance(frcClosest, frcOrigin);

		int iMax = frclCellLocations.size();

		for (int i = 1; i < iMax; i++) {
			StromalCelltemp frcCandidate = frclCellLocations.get(i);
			double dNewDist = calcDistance(frcCandidate, frcOrigin);
			if (dNewDist < dDist) {
				frcClosest = frcCandidate;
				dDist = dNewDist;
			}
		}

		return frcClosest;
	}
	
	
	/**
	 * Nodes in the center of the follicle are FDCs while those
	 * on the outside are bRCs, given a double3D this method returns a stroma type
	 * @return 
	 */
	private static TYPE determineStromaType(double x, double y){
		
		if(SimulationEnvironment.isWithinCircle(x, y, 
				(Settings.WIDTH / 2) ,(Settings.HEIGHT / 2) , SimulationEnvironment.fdcNetRadius)){
			
			return Stroma.TYPE.FDC;
		}
		else return Stroma.TYPE.bRC;
	}
	
	
	
}
