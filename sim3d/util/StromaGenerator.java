package sim3d.util;

import java.util.ArrayList;
import java.util.List;

import sim.util.Double3D;
import sim3d.Settings;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;

/**
 * A Singleton class implementing the method of generating stroma proposed by
 * Kislitsyn et al. in
 * "Computational Approach to 3D Modeling of the Lymph Node Geometry",
 * Computation, 2015.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class StromaGenerator {
	/**
	 * Class to keep track of edges for each cell
	 */
	public static class StromalCell {
		/**
		 * Location of the cell
		 */
		public Double3D d3Location;

		/**
		 * The number of edges (dendrites) the cell has
		 */
		public int iEdges = 0;

		/**
		 * A list containing all the edges
		 * 
		 * NB: this will only be accurate for cells that haven't been generated.
		 * We're only interested in these because they are needed to generate
		 * the directions
		 */
		ArrayList<Double3D> d3lEdges = new ArrayList<Double3D>();

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
		public StromalCell(double x, double y, double z) {
			d3Location = new Double3D(x, y, z);
		}

		/**
		 * Constructor
		 * 
		 * @param d3Position
		 *            Position of the cell
		 */
		public StromalCell(Double3D d3Position) {
			d3Location = d3Position;
		}
	}

	
	/**
	 * Generates a stromal network and returns the nodes in a 3D boolean array.
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
	public static int generateFRC3D(int iWidth, int iHeight, int iDepth,
			int iCellCount, ArrayList<StromalCell> frclCellLocations,
			List<StromaEdge> selEdges) {
		// It will be efficient to keep track of cells and locations separately
		@SuppressWarnings("unchecked")
		// because it is of an abstract interface
		ArrayList<StromalCell>[][][] frcla3CellLocations = new ArrayList[iWidth][iHeight][iDepth];
		ArrayList<StromalCell> frclUnbranchedCells = new ArrayList<StromalCell>();

		for (int x = 0; x < iWidth; x++) {
			for (int y = 0; y < iHeight; y++) {
				for (int z = 0; z < iDepth; z++) {
					frcla3CellLocations[x][y][z] = new ArrayList<StromalCell>();
				}
			}
		}

		// Add one in the centre, TODO input this as a parameter
		StromalCell frcInitialCell = new StromalCell(iWidth / 2.0, iHeight / 2.0,
				iDepth / 2.0);
		frclUnbranchedCells.add(frcInitialCell);
		frcla3CellLocations[iWidth / 2][iHeight / 2][iDepth / 2]
				.add(frcInitialCell);

		int iRemainingCells = iCellCount - 1;

		while (iRemainingCells > 0 && frclUnbranchedCells.size() > 0) {
			StromalCell frcNextCell = pickNextCell(iWidth, iHeight, iDepth,
					frclUnbranchedCells, frcla3CellLocations);
			// FRCCell frcNextCell = frclUnbranchedCells.get(
			// Options.RNG.nextInt(frclUnbranchedCells.size()) );

			if (frcNextCell == null) {
				break;
			}

			// Calculate the number of edges to make
			// Values were fitted to match the FRC paper
			int iEdges = Math
					.max(0,
							Math.min(iRemainingCells,
									(int) (Math.pow(Settings.RNG.nextDouble(),
											1.5) * (2.1) + 2.9))
									- frcNextCell.iEdges);

			// This is the first time so we want at few edges otherwise
			// generation will break sometimes
			if (iRemainingCells == iCellCount - 1) {
				iEdges++;
			}

			// if we have edges to add
			if (iEdges > 0) {
				// Get some directions
				Double3D[] d3aDirections = generateDirections(iWidth, iHeight,
						iDepth, frcla3CellLocations, frcNextCell, iEdges);

				// Create some cells at this direction
				iRemainingCells -= createNewCells(iWidth, iHeight, iDepth,
						frclUnbranchedCells, frcla3CellLocations, frcNextCell,
						d3aDirections);

				for (Double3D d3Direction : d3aDirections) {
					if (d3Direction != null) {
						// Add the edges
						selEdges.add(new StromaEdge(frcNextCell.d3Location,
								new Double3D(frcNextCell.d3Location.x
										+ d3Direction.x,
										frcNextCell.d3Location.y
												+ d3Direction.y,
										frcNextCell.d3Location.z
												+ d3Direction.z), StromaEdge.TYPE.RC_edge));
					}
						
				}
			}

			// Move the cell to the locations list
			frclCellLocations.add(frcNextCell);
			frclUnbranchedCells.remove(frcNextCell);
		}

		return iCellCount - iRemainingCells;
	}

	
	
	
	
	
	/**
	 * Generates a stromal network and returns the nodes in a 3D boolean array.
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
	public static int generateStroma3D(int iWidth, int iHeight, int iDepth,
			int iCellCount, ArrayList<StromalCell> frclCellLocations,
			List<StromaEdge> selEdges) {
		// It will be efficient to keep track of cells and locations separately
		@SuppressWarnings("unchecked")
		// because it is of an abstract interface
		ArrayList<StromalCell>[][][] frcla3CellLocations = new ArrayList[iWidth][iHeight][iDepth];
		ArrayList<StromalCell> frclUnbranchedCells = new ArrayList<StromalCell>();

		for (int x = 0; x < iWidth; x++) {
			for (int y = 0; y < iHeight; y++) {
				for (int z = 0; z < iDepth; z++) {
					frcla3CellLocations[x][y][z] = new ArrayList<StromalCell>();
				}
			}
		}

		// Add one in the centre, TODO input this as a parameter
		StromalCell frcInitialCell = new StromalCell(iWidth / 2.0, iHeight / 2.0,
				iDepth / 2.0);
		frclUnbranchedCells.add(frcInitialCell);
		frcla3CellLocations[iWidth / 2][iHeight / 2][iDepth / 2]
				.add(frcInitialCell);

		int iRemainingCells = iCellCount - 1;

		while (iRemainingCells > 0 && frclUnbranchedCells.size() > 0) {
			StromalCell frcNextCell = pickNextCell(iWidth, iHeight, iDepth,
					frclUnbranchedCells, frcla3CellLocations);
			// FRCCell frcNextCell = frclUnbranchedCells.get(
			// Options.RNG.nextInt(frclUnbranchedCells.size()) );

			if (frcNextCell == null) {
				break;
			}

			// Calculate the number of edges to make
			// Values were fitted to match the FRC paper
			int iEdges = Math
					.max(0,
							Math.min(iRemainingCells,
									(int) (Math.pow(Settings.RNG.nextDouble(),
											1.5) * (2.1) + 2.9))
									- frcNextCell.iEdges);

			// This is the first time so we want at few edges otherwise
			// generation will break sometimes
			if (iRemainingCells == iCellCount - 1) {
				iEdges++;
			}

			// if we have edges to add
			if (iEdges > 0) {
				// Get some directions
				Double3D[] d3aDirections = generateDirections(iWidth, iHeight,
						iDepth, frcla3CellLocations, frcNextCell, iEdges);

				// Create some cells at this direction
				iRemainingCells -= createNewCells(iWidth, iHeight, iDepth,
						frclUnbranchedCells, frcla3CellLocations, frcNextCell,
						d3aDirections);

				for (Double3D d3Direction : d3aDirections) {
					if (d3Direction != null) {
						// Add the edges
						selEdges.add(new StromaEdge(frcNextCell.d3Location,
								new Double3D(frcNextCell.d3Location.x
										+ d3Direction.x,
										frcNextCell.d3Location.y
												+ d3Direction.y,
										frcNextCell.d3Location.z
												+ d3Direction.z), StromaEdge.TYPE.FDC_edge));
					}
						
				}
			}

			// Move the cell to the locations list
			frclCellLocations.add(frcNextCell);
			frclUnbranchedCells.remove(frcNextCell);
		}

		return iCellCount - iRemainingCells;
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
	public static ArrayList<StromalCell> getAdjacentCells(int iWidth, int iHeight,
			int iDepth, ArrayList<StromalCell>[][][] frcla3CellLocations,
			StromalCell frcPoint, double iMaxDistance) {
		ArrayList<StromalCell> frclReturn = new ArrayList<StromalCell>();

		// Precompute these for efficiency
		// +1 because we want the distance between the close edges of the cells
		int iXLim = (int) Math.min(iMaxDistance + frcPoint.d3Location.x,
				iWidth - 1);
		int iYLim = (int) Math.min(iMaxDistance + frcPoint.d3Location.y,
				iHeight - 1);
		int iZLim = (int) Math.min(iMaxDistance + frcPoint.d3Location.z,
				iDepth - 1);

		for (int x = (int) Math.max(0, frcPoint.d3Location.x - iMaxDistance); x <= iXLim; x++) {
			for (int y = (int) Math
					.max(0, frcPoint.d3Location.y - iMaxDistance); y <= iYLim; y++) {
				for (int z = (int) Math.max(0, frcPoint.d3Location.z
						- iMaxDistance); z <= iZLim; z++) {
					if (x == frcPoint.d3Location.x
							&& y == frcPoint.d3Location.y
							&& z == frcPoint.d3Location.z) {
						continue;
					}
					// if a cell lives at this location,
					// and the distance is less than the max distance
					// we take one away from the max distance because we want
					// the distance between the closer edges
					for (StromalCell frcCollisionPoint : frcla3CellLocations[x][y][z]) {
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
	protected static double calcDistance(StromalCell i3Point1, StromalCell i3Point2) {
		return calcDistance(i3Point1.d3Location, i3Point2.d3Location);
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
			ArrayList<StromalCell> frclCellLocations,
			ArrayList<StromalCell>[][][] frcla3CellLocations, StromalCell frcOrigin,
			Double3D[] d3aDirections) {
		int iCellsCreated = 0;

		for (int i = 0; i < d3aDirections.length; i++) {
			double x, y, z;
			x = frcOrigin.d3Location.x + d3aDirections[i].x;
			y = frcOrigin.d3Location.y + d3aDirections[i].y;
			z = frcOrigin.d3Location.z + d3aDirections[i].z;

			StromalCell frcNewPoint = new StromalCell(x, y, z);

			// check if out of bounds
			if (x < 0 || x >= iWidth || y < 0 || y >= iHeight || z < 0
					|| z >= iDepth) {
				double dCoeff = 1;
				if (x < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.d3Location.x
							/ d3aDirections[i].x);
				} else if (x > iWidth) {
					dCoeff = Math.min(dCoeff, (iWidth - frcOrigin.d3Location.x)
							/ d3aDirections[i].x);
				}

				if (y < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.d3Location.y
							/ d3aDirections[i].y);
				} else if (y > iHeight) {
					dCoeff = Math.min(dCoeff,
							(iHeight - frcOrigin.d3Location.y)
									/ d3aDirections[i].y);
				}

				if (z < 0) {
					dCoeff = Math.min(dCoeff, -frcOrigin.d3Location.z
							/ d3aDirections[i].z);
				} else if (z > iDepth) {
					dCoeff = Math.min(dCoeff, (iDepth - frcOrigin.d3Location.z)
							/ d3aDirections[i].z);
				}

				// TODO how do we handle the edges?
				/*
				 * if ( false && dCoeff > 0 ) { d3aDirections[i] =
				 * d3aDirections[i].multiply( dCoeff ); } else {/
				 */
				// Set it to null so the parent knows it's not been created
				d3aDirections[i] = null;
				/* }/ * */

				frcOrigin.iEdges++;

				continue;
			}

			// Check if there already exists a cell within 1.6 grid spaces
			// If we don't do this, the grid is ridiculously dense
			ArrayList<StromalCell> d3lAdjacent = getAdjacentCells(iWidth, iHeight,
					iDepth, frcla3CellLocations, frcNewPoint, 1.6);

			// If there is one (and it isn't itself)
			if (d3lAdjacent.size() > 1
					|| (d3lAdjacent.size() == 1 && d3lAdjacent.get(0) != frcOrigin)) {
				// Pick a random one (probably only one)
				StromalCell newLoc = d3lAdjacent.get(Settings.RNG
						.nextInt(d3lAdjacent.size()));

				while (newLoc == frcOrigin) {
					newLoc = d3lAdjacent.get(Settings.RNG.nextInt(d3lAdjacent
							.size()));
				}

				// draw an edge to the existing cell
				d3aDirections[i] = new Double3D(newLoc.d3Location.x
						- frcOrigin.d3Location.x, newLoc.d3Location.y
						- frcOrigin.d3Location.y, newLoc.d3Location.z
						- frcOrigin.d3Location.z);

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
			frcNewPoint.iEdges++;
			frcOrigin.iEdges++;
		}

		return iCellsCreated;
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
	protected static Double3D[] generateDirections(int iWidth, int iHeight,
			int iDepth, ArrayList<StromalCell>[][][] frcla3CellLocations,
			StromalCell frcLocation, int iCellCount) {
		Double3D[] d3aReturn = new Double3D[iCellCount];

		boolean bFail = false;

		do {
			bFail = false;

			d3aReturn[0] = new Double3D();

			// account for the edges already going to this cell
			for (Double3D d3Point : frcLocation.d3lEdges) {
				d3aReturn[0].subtract(d3Point);
			}

			for (int i = 1; i < iCellCount; i++) {

				// This distribution... It approximately matches the paper, and
				// was derived using the newton divided differences method giving the following function
				// http://www.wolframalpha.com/input/?i=0.392281+x-0.342923+x%5E2%2B0.151204+x%5E3-0.0270696+x%5E4%2B0.00180148+x%5E5+between+x+%3D+0+and+5
				// divided differences allows you to interplaote a continuous
				// distribution from discrete data

				
				//very useful approach: see the following for more details
				// http://mathforcollege.com/nm/mws/gen/05inp/mws_gen_inp_txt_ndd.pdf
				//http://ww2.odu.edu/~agodunov/teaching/notes/Nm01_interpolation.pdf
				
				
				double length = Settings.RNG.nextDouble() * 2.6;
				length = 0.00180148 * Math.pow(length, 5) - 0.0270696
						* Math.pow(length, 4) + 0.151204 * Math.pow(length, 3)
						- 0.342923 * Math.pow(length, 2) + 0.392281 * length;

				// Yay! More magic numbers
				length = 1.3 + length * 3.5;

				// 2D special case
				if (iDepth == 1) {
					d3aReturn[i] = Vector3DHelper.getRandomDirection();
					d3aReturn[i] = new Double3D(d3aReturn[i].x, d3aReturn[i].y,
							0).normalize().multiply((length + 1) * 3);
				} else {
					d3aReturn[i] = Vector3DHelper.getRandomDirection()
							.multiply(length);
				}

				// If there's a cell there, try again..?
				if (getAdjacentCells(iWidth, iHeight, iDepth,
						frcla3CellLocations,
						new StromalCell(frcLocation.d3Location.add(d3aReturn[i])),
						1.1).size() > 0) {
					i--;
					continue;
				}

				// Make sure we don't collide with other edges already made
				boolean bCollision = false;
				for (int j = 0; j < i; j++) {
					if (calcDistance(d3aReturn[i], d3aReturn[j]) < 1.1) {
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
					new StromalCell(frcLocation.d3Location.add(d3aReturn[0])), 1.2)
					.size() > 0) {
				bFail = true;
				continue;
			}

			// Again, avoid self collision
			boolean bCollision = false;
			for (int i = 1; i < iCellCount; i++) {
				if (calcDistance(d3aReturn[i], d3aReturn[0]) < 1.1) {
					bCollision = true;
					break;
				}
			}
			if (bCollision) {
				bFail = true;
				continue;
			}

			// just check we aren't making a huge edge!
		} while (!bFail && d3aReturn[0].length() > 4
				&& d3aReturn[0].length() < 1.1);

		return d3aReturn;
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
	protected static StromalCell pickNextCell(int iWidth, int iHeight, int iDepth,
			ArrayList<StromalCell> frclCellLocations,
			ArrayList<StromalCell>[][][] frcla3CellLocations) {
		if (frclCellLocations.size() == 0) {
			return null;
		}

		StromalCell frcOrigin = new StromalCell(iWidth / 2.0, iHeight / 2.0,
				iDepth / 2.0);
		StromalCell frcClosest = frclCellLocations.get(0);
		double dDist = calcDistance(frcClosest, frcOrigin);

		int iMax = frclCellLocations.size();

		for (int i = 1; i < iMax; i++) {
			StromalCell frcCandidate = frclCellLocations.get(i);
			double dNewDist = calcDistance(frcCandidate, frcOrigin);
			if (dNewDist < dDist) {
				frcClosest = frcCandidate;
				dDist = dNewDist;
			}
		}

		return frcClosest;
	}
}
