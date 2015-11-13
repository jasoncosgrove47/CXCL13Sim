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
public class FRCStromaGenerator {

	/**
	 * Generates a stromal network and returns the nodes in a 3D boolean array.
	 * @param iWidth Width of grid
	 * @param iHeight Height of grid
	 * @param iDepth Depth of grid
	 * @param iCellCount Max number of stromal cells (note: this is a upper bound only)
	 * @return
	 */
	public static int generateStroma3D(int iWidth, int iHeight, int iDepth, int iCellCount, ArrayList<Double3D> d3lCellLocations, List<StromaEdge> selEdges)
	{
		// It will be efficient to keep track of cells and locations separately
		ArrayList<Double3D>[][][] d3la3CellLocations = new ArrayList[iWidth][iHeight][iDepth];
		ArrayList<Double3D> d3lUnbranchedCells = new ArrayList<Double3D>();
		
		for ( int x = 0; x < iWidth; x++ )
		{
			for ( int y = 0; y < iHeight; y++ )
			{
				for ( int z = 0; z < iDepth; z++ )
				{
					d3la3CellLocations[x][y][z] = new ArrayList<Double3D>();
				}
			}
		}
		
		// Add one in the centre
		Double3D d3InitialCell = new Double3D(iWidth/2.0, iHeight/2.0, iDepth/2.0);
		d3lUnbranchedCells.add(d3InitialCell);
		d3la3CellLocations[iWidth/2][iHeight/2][iDepth/2].add( d3InitialCell );
		
		int iRemainingCells = iCellCount - 1;
		
		while ( iRemainingCells > 0 && d3lUnbranchedCells.size() > 0 )
		{
			//Double3D d3NextCell = pickNextCell(iWidth, iHeight, iDepth, d3lUnbranchedCells, d3la3CellLocations);
			Double3D d3NextCell = d3lUnbranchedCells.get( Options.RNG.nextInt(d3lUnbranchedCells.size()) );
			
			if ( d3NextCell == null )
			{
				break;
			}
			
			int iEdges = Math.min(iRemainingCells, (int)Math.round(Options.RNG.nextDouble()*(5)+2));
			if ( iRemainingCells == iCellCount - 1 )
			{
				// This is the first time so we want at few edges otherwise generation will break sometimes
				iEdges++;
			}
			
			Double3D[] d3aDirections = generateDirections(iWidth, iHeight, iDepth, d3la3CellLocations, d3NextCell, iEdges);
			
			iRemainingCells -= createNewCells(iWidth, iHeight, iDepth, d3lUnbranchedCells, d3la3CellLocations, d3NextCell, d3aDirections);
			
			for ( Double3D d3Direction : d3aDirections )
			{
				if ( d3Direction != null )
				{
					selEdges.add(new StromaEdge(new Double3D(d3NextCell),
												new Double3D(d3NextCell.x+d3Direction.x, d3NextCell.y+d3Direction.y, d3NextCell.z+d3Direction.z)));
				}
			}

			d3lCellLocations.add(d3NextCell);
			d3lUnbranchedCells.remove(d3NextCell);
		}
		
		return iCellCount - iRemainingCells;
	}
	
	protected static int createNewCells(int iWidth, int iHeight, int iDepth, ArrayList<Double3D> d3lCellLocations, ArrayList<Double3D>[][][] d3la3CellLocations, Double3D d3Origin, Double3D[] d3aDirections)
	{
		int iCellsCreated = 0;
		
		for ( int i = 0; i < d3aDirections.length; i++ )
		{
			double x, y, z;
			x = d3Origin.x + d3aDirections[i].x;
			y = d3Origin.y + d3aDirections[i].y;
			z = d3Origin.z + d3aDirections[i].z;

			Double3D d3NewPoint = new Double3D(x, y, z);
			
			// check if out of bounds
			if ( x < 0 || x >= iWidth || y < 0 || y >= iHeight || z < 0 || z >= iDepth )
			{
				double dCoeff = 1;
				if ( x < 0 )
				{
					dCoeff = Math.min( dCoeff, -d3Origin.x/d3aDirections[i].x);
				}
				else if ( x > iWidth )
				{
					dCoeff = Math.min( dCoeff, (iWidth-d3Origin.x)/d3aDirections[i].x);
				}
				
				if ( y < 0 )
				{
					dCoeff = Math.min( dCoeff, -d3Origin.y/d3aDirections[i].y);
				}
				else if ( y > iHeight )
				{
					dCoeff = Math.min( dCoeff, (iHeight-d3Origin.y)/d3aDirections[i].y);
				}
				
				if ( z < 0 )
				{
					dCoeff = Math.min( dCoeff, -d3Origin.z/d3aDirections[i].z);
				}
				else if ( z > iDepth )
				{
					dCoeff = Math.min( dCoeff, (iDepth-d3Origin.z)/d3aDirections[i].z);
				}
				
				if ( dCoeff > 0 )
				{
					d3aDirections[i] = d3aDirections[i].multiply( dCoeff );
				}
				else
				{
					// Set it to null so the parent knows it's not been created
					d3aDirections[i] = null;
				}
				
				continue;
			}
			else 
			{
				ArrayList<Double3D> d3lAdjacent = getAdjacentCells(iWidth, iHeight, iDepth, d3la3CellLocations, d3NewPoint, 1.6);
				
				if ( d3lAdjacent.size() != 0 )
				{
					Double3D newLoc = d3lAdjacent.get( Options.RNG.nextInt(d3lAdjacent.size()) );
					d3aDirections[i] = new Double3D(newLoc.x - d3Origin.x, newLoc.y - d3Origin.y, newLoc.z - d3Origin.z);
					
					continue;
				}
			}
			
			// make the edges match up with the new cells
			//d3aDirections[i] = new Double3D(x - d3Origin.x, y - d3Origin.y, z - d3Origin.z);

			d3lCellLocations.add(d3NewPoint);
			d3la3CellLocations[(int)x][(int)y][(int)z].add(d3NewPoint);
			iCellsCreated++;
		}
		
		return iCellsCreated;
	}
	
	protected static Double3D[] generateDirections(int iWidth, int iHeight, int iDepth, ArrayList<Double3D>[][][] d3la3CellLocations, Double3D d3Location, int iCellCount)
	{
 		Double3D[] d3aReturn = new Double3D[iCellCount];
		
 		do
 		{
	 		d3aReturn[0] = new Double3D();
	 		
			for(int i = 1; i < iCellCount; i++)
			{
				
				// -0.5x^4 + 13/3x^3 - 12x^2 + 61/6x + 3 
				// http://www.wolframalpha.com/input/?i=plot+-0.5x%5E4+%2B+13%2F3x%5E3+-+12x%5E2+%2B+61%2F6x+%2B+3.5+between+x%3D0+and+x%3D4
				// http://www.wolframalpha.com/input/?i=integrate+0.4-%28190+%2B+110%28x-0.4%29+-+100%28x-0.4%29%28x-1.4%29+%2B+35+%28x-0.4%29%28x-1.4%29%28x-2.4%29+-+%2895%2F12%29%28x-0.4%29%28x-1.4%29%28x-2.4%29%28x-3.4%29%29%2F878.906+between+x+%3D+0+and+5
				// http://www.wolframalpha.com/input/?i=0.392281+x-0.342923+x%5E2%2B0.151204+x%5E3-0.0270696+x%5E4%2B0.00180148+x%5E5+between+x+%3D+0+and+5
				double length = Options.RNG.nextDouble()*5;
				length =   0.00180148*Math.pow(length,  5)
						 - 0.0270696*Math.pow(length,  4)
						 + 0.1511204*Math.pow(length,  3)
						 - 0.342923*Math.pow(length,  2)
						 + 0.392281*length;
				
				length = 1.1 + length*3;
				
				if ( iDepth == 1 )
				{
					d3aReturn[i] = Vector3DHelper.getRandomDirection();
					d3aReturn[i] = new Double3D(d3aReturn[i].x, d3aReturn[i].y, 0).normalize().multiply((length+1)*3);
				}
				else
				{
					d3aReturn[i] = Vector3DHelper.getRandomDirection().multiply(length);
				}
				
				d3aReturn[0] = d3aReturn[0].subtract(d3aReturn[i]);
			}
			
			if ( iCellCount == 1 )
			{
				// We've only got one so doesn't make sense to balance them out
				// TODO this function doesn't take previous nodes into consideration
				double length = Options.RNG.nextDouble()*5;
				length =   0.00180148*Math.pow(length,  5)
						 - 0.0270696*Math.pow(length,  4)
						 + 0.1511204*Math.pow(length,  3)
						 - 0.342923*Math.pow(length,  2)
						 + 0.392281*length;
				
				length = 1.1 + length*3;
				
				d3aReturn[0] = Vector3DHelper.getRandomDirection().multiply(length);
			}
			else
			{
				// add some noise
				d3aReturn[0] = d3aReturn[0].add(Vector3DHelper.getRandomDirection().multiply(Options.RNG.nextDouble()*0.4));
			}
			// just check we aren't making a huge edge!
 		} while (d3aReturn[0].length() > 6);
		
		return d3aReturn;
	}
	
	protected static Double3D pickNextCell(int iWidth, int iHeight, int iDepth, ArrayList<Double3D> d3lCellLocations, ArrayList<Double3D>[][][] d3la3CellLocations)
	{
		int iIndex = 0;
		boolean bSuitable = false;
		
		do
		{
			if ( d3lCellLocations.size() == 0 )
			{
				return null;
			}
			iIndex = Options.RNG.nextInt(d3lCellLocations.size());
			
			ArrayList<Double3D> i3aAdjCells = getAdjacentCells(iWidth, iHeight, iDepth, d3la3CellLocations, d3lCellLocations.get(iIndex), Options.FRCGenerator.MAX_EDGE_LENGTH());
			
			double dProbability = Math.pow(1.0/i3aAdjCells.size(), 2);
			
			if ( dProbability > Options.RNG.nextDouble() )
			{
				bSuitable = true;
			}
		} while (bSuitable == false);
		
		
		return d3lCellLocations.get(iIndex);
	}
	
	public static ArrayList<Double3D> getAdjacentCells(int iWidth, int iHeight, int iDepth, ArrayList<Double3D>[][][] d3la3CellLocations, Double3D d3Point, double iMaxDistance)
	{
		ArrayList<Double3D> d3lReturn = new ArrayList<Double3D>();

		// Precompute these for efficiency
		// +1 because we want the distance between the close edges of the cells
		int iXLim = (int)Math.min(iMaxDistance + d3Point.x, iWidth-1);
		int iYLim = (int)Math.min(iMaxDistance + d3Point.y, iHeight-1);
		int iZLim = (int)Math.min(iMaxDistance + d3Point.z, iDepth-1);
		
		for ( int x = (int)Math.max(0, d3Point.x-iMaxDistance); x <= iXLim; x++ )
		{
			for ( int y = (int)Math.max(0, d3Point.y-iMaxDistance); y <= iYLim; y++ )
			{
				for ( int z = (int)Math.max(0, d3Point.z-iMaxDistance); z <= iZLim; z++ )
				{
					if ( x == d3Point.x && y == d3Point.y && z == d3Point.z )
					{
						continue;
					}
					// if a cell lives at this location,
					// and the distance is less than the max distance
					// we take one away from the max distance because we want
					// the distance between the closer edges
					for ( Double3D d3CollisionPoint : d3la3CellLocations[x][y][z] )
					{
						if ( calcDistance(d3Point, d3CollisionPoint) <= iMaxDistance )
						{
							d3lReturn.add(d3CollisionPoint);
						}
					}
				}
			}
		}
		
		return d3lReturn;
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
