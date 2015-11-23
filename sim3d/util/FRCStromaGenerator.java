package sim3d.util;

import java.util.ArrayList;
import java.util.List;

import sim.util.Double3D;
import sim3d.Options;
import sim3d.cell.StromaEdge;

/**
 * A Singleton class implementing the method of generating stroma proposed by Kislitsyn et al.
 * in "Computational Approach to 3D Modeling of the Lymph Node Geometry", Computation, 2015.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class FRCStromaGenerator
{
	/**
	 * Class to keep track of edges for each cell
	 *
	 */
	public static class FRCCell
	{
		public Double3D d3Location;
		public int iEdges = 0;
		ArrayList<Double3D> d3lEdges = new ArrayList<Double3D>();
		public FRCCell (double x, double y, double z)
		{
			d3Location = new Double3D(x, y, z);
		}
		/**
		 * @param double3d
		 */
		public FRCCell( Double3D double3d )
		{
			d3Location = double3d;
		}
	}
	
	/**
	 * Generates a stromal network and returns the nodes in a 3D boolean array.
	 * @param iWidth Width of grid
	 * @param iHeight Height of grid
	 * @param iDepth Depth of grid
	 * @param iCellCount Max number of stromal cells (note: this is a upper bound only)
	 * @return
	 */
	public static int generateStroma3D(int iWidth, int iHeight, int iDepth, int iCellCount, ArrayList<FRCCell> frclCellLocations, List<StromaEdge> selEdges)
	{
		// It will be efficient to keep track of cells and locations separately
		@SuppressWarnings( "unchecked" ) // because it is of an abstract interface
		ArrayList<FRCCell>[][][] frcla3CellLocations = new ArrayList[iWidth][iHeight][iDepth];
		ArrayList<FRCCell> frclUnbranchedCells = new ArrayList<FRCCell>();
		
		for ( int x = 0; x < iWidth; x++ )
		{
			for ( int y = 0; y < iHeight; y++ )
			{
				for ( int z = 0; z < iDepth; z++ )
				{
					frcla3CellLocations[x][y][z] = new ArrayList<FRCCell>();
				}
			}
		}
		
		// Add one in the centre
		FRCCell frcInitialCell = new FRCCell(iWidth/2.0, iHeight/2.0, iDepth/2.0);
		frclUnbranchedCells.add(frcInitialCell);
		frcla3CellLocations[iWidth/2][iHeight/2][iDepth/2].add( frcInitialCell );
		
		int iRemainingCells = iCellCount - 1;
		
		while ( iRemainingCells > 0 && frclUnbranchedCells.size() > 0 )
		{
			FRCCell frcNextCell = pickNextCell(iWidth, iHeight, iDepth, frclUnbranchedCells, frcla3CellLocations);
			//FRCCell frcNextCell = frclUnbranchedCells.get( Options.RNG.nextInt(frclUnbranchedCells.size()) );
			
			if ( frcNextCell == null )
			{
				break;
			}
			
			//ArrayList<FRCCell> i3aAdjCells = getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations, frcNextCell, 1.7);
			
			int iEdges = Math.max( 0, Math.min(iRemainingCells, (int)(Math.pow(Options.RNG.nextDouble(),1.5)*(2.1)+2.9)) - frcNextCell.iEdges );
			if ( iRemainingCells == iCellCount - 1 )
			{
				// This is the first time so we want at few edges otherwise generation will break sometimes
				iEdges++;
			}
			
			if ( iEdges > 0 )
			{
				Double3D[] d3aDirections = generateDirections(iWidth, iHeight, iDepth, frcla3CellLocations, frcNextCell, iEdges);
				
				iRemainingCells -= createNewCells(iWidth, iHeight, iDepth, frclUnbranchedCells, frcla3CellLocations, frcNextCell, d3aDirections);
				
				for ( Double3D d3Direction : d3aDirections )
				{
					if ( d3Direction != null )
					{
						selEdges.add(new StromaEdge(frcNextCell.d3Location,
													new Double3D(frcNextCell.d3Location.x+d3Direction.x, frcNextCell.d3Location.y+d3Direction.y, frcNextCell.d3Location.z+d3Direction.z)));
					}
				}
			}

			frclCellLocations.add(frcNextCell);
			frclUnbranchedCells.remove(frcNextCell);
		}
		
		return iCellCount - iRemainingCells;
	}
	
	protected static int createNewCells(int iWidth, int iHeight, int iDepth, ArrayList<FRCCell> frclCellLocations, ArrayList<FRCCell>[][][] frcla3CellLocations, FRCCell frcOrigin, Double3D[] d3aDirections)
	{
		int iCellsCreated = 0;
		
		for ( int i = 0; i < d3aDirections.length; i++ )
		{
			double x, y, z;
			x = frcOrigin.d3Location.x + d3aDirections[i].x;
			y = frcOrigin.d3Location.y + d3aDirections[i].y;
			z = frcOrigin.d3Location.z + d3aDirections[i].z;

			FRCCell frcNewPoint = new FRCCell(x, y, z);
			
			// check if out of bounds
			if ( x < 0 || x >= iWidth || y < 0 || y >= iHeight || z < 0 || z >= iDepth )
			{
				double dCoeff = 1;
				if ( x < 0 )
				{
					dCoeff = Math.min( dCoeff, -frcOrigin.d3Location.x/d3aDirections[i].x);
				}
				else if ( x > iWidth )
				{
					dCoeff = Math.min( dCoeff, (iWidth-frcOrigin.d3Location.x)/d3aDirections[i].x);
				}
				
				if ( y < 0 )
				{
					dCoeff = Math.min( dCoeff, -frcOrigin.d3Location.y/d3aDirections[i].y);
				}
				else if ( y > iHeight )
				{
					dCoeff = Math.min( dCoeff, (iHeight-frcOrigin.d3Location.y)/d3aDirections[i].y);
				}
				
				if ( z < 0 )
				{
					dCoeff = Math.min( dCoeff, -frcOrigin.d3Location.z/d3aDirections[i].z);
				}
				else if ( z > iDepth )
				{
					dCoeff = Math.min( dCoeff, (iDepth-frcOrigin.d3Location.z)/d3aDirections[i].z);
				}
				
				// TODO how do we handle the edges?
				/*if ( false && dCoeff > 0 )
				{
					d3aDirections[i] = d3aDirections[i].multiply( dCoeff );
				}
				else
				{/**/
					// Set it to null so the parent knows it's not been created
					d3aDirections[i] = null;
				/*}/**/
				
				frcOrigin.iEdges++;
				
				continue;
			}
			
			ArrayList<FRCCell> d3lAdjacent = getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations, frcNewPoint, 1.6);
			
			if ( d3lAdjacent.size() > 1 || ( d3lAdjacent.size() == 1 && d3lAdjacent.get(0) != frcOrigin ) )
			{
				FRCCell newLoc = d3lAdjacent.get( Options.RNG.nextInt(d3lAdjacent.size()) );
				
				while ( newLoc == frcOrigin )
				{
					newLoc = d3lAdjacent.get( Options.RNG.nextInt(d3lAdjacent.size()) );
				}
				
				d3aDirections[i] = new Double3D(newLoc.d3Location.x - frcOrigin.d3Location.x, newLoc.d3Location.y - frcOrigin.d3Location.y, newLoc.d3Location.z - frcOrigin.d3Location.z);
				
				frcNewPoint = newLoc;
			}
			else
			{
				frclCellLocations.add(frcNewPoint);
				frcla3CellLocations[(int)x][(int)y][(int)z].add(frcNewPoint);
				iCellsCreated++;
			}
			frcNewPoint.d3lEdges.add(d3aDirections[i].negate());
			
			frcNewPoint.iEdges++;
			frcOrigin.iEdges++;
		}
		
		return iCellsCreated;
	}
	
	protected static Double3D[] generateDirections(int iWidth, int iHeight, int iDepth, ArrayList<FRCCell>[][][] frcla3CellLocations, FRCCell frcLocation, int iCellCount)
	{
 		Double3D[] d3aReturn = new Double3D[iCellCount];

		boolean bFail = false;
		
 		do
 		{
 			bFail = false;
 			
	 		d3aReturn[0] = new Double3D();

	 		for ( Double3D d3Point : frcLocation.d3lEdges )
	 		{
	 			d3aReturn[0].subtract(d3Point);
	 		}
	 		
			for(int i = 1; i < iCellCount; i++)
			{
				
				// -0.5x^4 + 13/3x^3 - 12x^2 + 61/6x + 3 
				// http://www.wolframalpha.com/input/?i=plot+-0.5x%5E4+%2B+13%2F3x%5E3+-+12x%5E2+%2B+61%2F6x+%2B+3.5+between+x%3D0+and+x%3D4
				// http://www.wolframalpha.com/input/?i=integrate+0.4-%28190+%2B+110%28x-0.4%29+-+100%28x-0.4%29%28x-1.4%29+%2B+35+%28x-0.4%29%28x-1.4%29%28x-2.4%29+-+%2895%2F12%29%28x-0.4%29%28x-1.4%29%28x-2.4%29%28x-3.4%29%29%2F878.906+between+x+%3D+0+and+5
				// http://www.wolframalpha.com/input/?i=0.392281+x-0.342923+x%5E2%2B0.151204+x%5E3-0.0270696+x%5E4%2B0.00180148+x%5E5+between+x+%3D+0+and+5
				double length = Options.RNG.nextDouble()*2.6;
				length =   0.00180148*Math.pow(length,  5)
						 - 0.0270696*Math.pow(length,  4)
						 + 0.151204*Math.pow(length,  3)
						 - 0.342923*Math.pow(length,  2)
						 + 0.392281*length;
				
				length = 1.3 + length*3.5;
				
				if ( iDepth == 1 )
				{
					d3aReturn[i] = Vector3DHelper.getRandomDirection();
					d3aReturn[i] = new Double3D(d3aReturn[i].x, d3aReturn[i].y, 0).normalize().multiply((length+1)*3);
				}
				else
				{
					d3aReturn[i] = Vector3DHelper.getRandomDirection().multiply(length);
				}
				
				if ( getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations, new FRCCell(frcLocation.d3Location.add(d3aReturn[i])), 1.1).size() > 0 )
				{
					i--;
					continue;
				}
				
				boolean bCollision = false;
				for ( int j = 0; j < i; j++ )
				{
					if ( calcDistance(d3aReturn[i], d3aReturn[j]) < 1.1 )
					{
						bCollision = true;
						break;
					}
				}
				if ( bCollision )
				{
					i--;
					continue;
				}
				
				d3aReturn[0] = d3aReturn[0].subtract(d3aReturn[i]);
			}
			
			// add some noise
			d3aReturn[0] = d3aReturn[0].add(Vector3DHelper.getRandomDirection().multiply(Options.RNG.nextDouble()*0.04));
			
			if ( getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations, new FRCCell(frcLocation.d3Location.add(d3aReturn[0])), 1.2).size() > 0 )
			{
				bFail = true;
				continue;
			}
			
			boolean bCollision = false;
			for ( int i = 1; i < iCellCount; i++ )
			{
				if ( calcDistance(d3aReturn[i], d3aReturn[0]) < 1.1 )
				{
					bCollision = true;
					break;
				}
			}
			if ( bCollision )
			{
				bFail = true;
				continue;
			}
			
			// just check we aren't making a huge edge!
 		} while (!bFail && d3aReturn[0].length() > 4 && d3aReturn[0].length() < 1.1);
		
		return d3aReturn;
	}
	
	protected static FRCCell pickNextCell(int iWidth, int iHeight, int iDepth, ArrayList<FRCCell> frclCellLocations, ArrayList<FRCCell>[][][] frcla3CellLocations)
	{
		boolean bSuitable = true;
		
		do
		{
			if ( frclCellLocations.size() == 0 )
			{
				return null;
			}

			FRCCell frcOrigin = new FRCCell(iWidth/2.0, iHeight/2.0, iDepth/2.0);
			FRCCell frcClosest = frclCellLocations.get( 0 );
			double dDist = calcDistance( frcClosest, frcOrigin );
			
			int iMax = frclCellLocations.size();
			
			for ( int i = 1; i < iMax; i++ )
			{
				FRCCell frcCandidate = frclCellLocations.get( i );
				double dNewDist = calcDistance( frcCandidate, frcOrigin );
				if ( dNewDist < dDist )
				{
					frcClosest = frcCandidate;
					dDist = dNewDist;
				}
			}
			
			return frcClosest;
			/*iIndex = Options.RNG.nextInt(frclCellLocations.size());
			
			ArrayList<FRCCell> i3aAdjCells = getAdjacentCells(iWidth, iHeight, iDepth, frcla3CellLocations, frclCellLocations.get(iIndex), Options.FRCGenerator.MAX_EDGE_LENGTH());
			
			double dProbability = Math.pow(1.0/i3aAdjCells.size(), 2);
			
			if ( dProbability > Options.RNG.nextDouble() )
			{
				bSuitable = true;
			}*/
		} while (bSuitable == false);
		
		
		//return frclCellLocations.get(iIndex);
	}
	
	public static ArrayList<FRCCell> getAdjacentCells(int iWidth, int iHeight, int iDepth, ArrayList<FRCCell>[][][] frcla3CellLocations, FRCCell frcPoint, double iMaxDistance)
	{
		ArrayList<FRCCell> frclReturn = new ArrayList<FRCCell>();

		// Precompute these for efficiency
		// +1 because we want the distance between the close edges of the cells
		int iXLim = (int)Math.min(iMaxDistance + frcPoint.d3Location.x, iWidth-1);
		int iYLim = (int)Math.min(iMaxDistance + frcPoint.d3Location.y, iHeight-1);
		int iZLim = (int)Math.min(iMaxDistance + frcPoint.d3Location.z, iDepth-1);
		
		for ( int x = (int)Math.max(0, frcPoint.d3Location.x-iMaxDistance); x <= iXLim; x++ )
		{
			for ( int y = (int)Math.max(0, frcPoint.d3Location.y-iMaxDistance); y <= iYLim; y++ )
			{
				for ( int z = (int)Math.max(0, frcPoint.d3Location.z-iMaxDistance); z <= iZLim; z++ )
				{
					if ( x == frcPoint.d3Location.x && y == frcPoint.d3Location.y && z == frcPoint.d3Location.z )
					{
						continue;
					}
					// if a cell lives at this location,
					// and the distance is less than the max distance
					// we take one away from the max distance because we want
					// the distance between the closer edges
					for ( FRCCell frcCollisionPoint : frcla3CellLocations[x][y][z] )
					{
						if ( calcDistance(frcPoint, frcCollisionPoint) <= iMaxDistance )
						{
							frclReturn.add(frcCollisionPoint);
						}
					}
				}
			}
		}
		
		return frclReturn;
	}
	
	protected static double calcDistance(FRCCell i3Point1, FRCCell i3Point2)
	{
		return calcDistance( i3Point1.d3Location, i3Point2.d3Location );
	}
	
	protected static double calcDistance(Double3D i3Point1, Double3D i3Point2)
	{
		return ( Math.sqrt( Math.pow(i3Point1.x-i3Point2.x, 2)
				          + Math.pow(i3Point1.y-i3Point2.y, 2)
				          + Math.pow(i3Point1.z-i3Point2.z, 2)
						  )
		       );
	}
}
