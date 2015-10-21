package sim3d.util;

import java.util.ArrayList;
import java.util.List;

import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.cell.StromaEdge;

/**
 * A Singleton class implementing the method of generating stroma proposed by Kislitsyn et al.
 * in "Computational Approach to 3D Modeling of the Lymph Node Geometry", Computation, 2015.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 * TODO this will only create cells with integer coordinates. Surely could be done better!
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
	public static boolean[][][] generateStroma3D(int iWidth, int iHeight, int iDepth, int iCellCount, List<StromaEdge> selEdges)
	{
		boolean[][][] ba3CellLocations = new boolean[iWidth][iHeight][iDepth];
		
		// It will be efficient to keep track of cells and locations separately
		ArrayList<Int3D> i3lCellLocations = new ArrayList<Int3D>();
		
		// Add one in the centre
		i3lCellLocations.add(new Int3D(iWidth/2, iHeight/2, iDepth/2));
		ba3CellLocations[iWidth/2][iHeight/2][iDepth/2] = true;
		
		int iRemainingCells = iCellCount - 1;
		
		while ( iRemainingCells > 0 && i3lCellLocations.size() > 0 )
		{
			Double3D[] d3aDirections = new Double3D[0];
			
			Int3D i3NextCell = pickNextCell(iWidth, iHeight, iDepth, i3lCellLocations, ba3CellLocations);
			
			if ( i3NextCell == null )
			{
				break;
			}
			
			int iEdges = Math.min(iRemainingCells, (int)Math.round(Options.RNG.nextDouble()*(5)+2));
			if ( iRemainingCells == iCellCount - 1 )
			{
				// This is the first time so we want at least 2 edges otherwise generation will break sometimes
				// TODO there is still a very small chance it will break, but it's very rare
				iEdges++;
			}
			
			d3aDirections = generateDirections(iWidth, iHeight, iDepth, ba3CellLocations, i3NextCell, iEdges);
			
			iRemainingCells -= createNewCells(iWidth, iHeight, iDepth, i3lCellLocations, ba3CellLocations, i3NextCell, d3aDirections);
			
			for ( Double3D d3Direction : d3aDirections )
			{
				if ( d3Direction != null )
				{
					selEdges.add(new StromaEdge(new Double3D(i3NextCell),
												new Double3D(i3NextCell.x+d3Direction.x, i3NextCell.y+d3Direction.y, i3NextCell.z+d3Direction.z)));
				}
			}
			
			i3lCellLocations.remove(i3NextCell);
		}
		
		return ba3CellLocations;
	}
	
	private static int createNewCells(int iWidth, int iHeight, int iDepth, ArrayList<Int3D> i3lCellLocations, boolean[][][] ba3CellLocations, Int3D i3Origin, Double3D[] d3aDirections)
	{
		int iCellsCreated = 0;
		
		for ( int i = 0; i < d3aDirections.length; i++ )
		{
			int x, y, z;
			x = (int) Math.round(i3Origin.x + d3aDirections[i].x);
			y = (int) Math.round(i3Origin.y + d3aDirections[i].y);
			z = (int) Math.round(i3Origin.z + d3aDirections[i].z);

			Int3D i3NewPoint = new Int3D(x, y, z);
			
			// check if out of bounds
			if ( x < 0 || x >= iWidth || y < 0 || y >= iHeight || z < 0 || z >= iDepth )
			{
				// Set it to null so the parent knows it's not been created
				d3aDirections[i] = null;
				continue;
			}
			// Check if point already exists (in which case we don't remove the edge)
			else if ( ba3CellLocations[x][y][z] )
			{
				continue;
			}
			else 
			{
				ArrayList<Int3D> i3lAdjacent = getAdjacentCells(iWidth, iHeight, iDepth, ba3CellLocations, i3NewPoint, 1);
				
				// Now we check if there's already one adjacent in which case we don't create another
				// but move the vector to the existing cell
				if ( i3lAdjacent.size() > 0 )
				{
					// Pick a random adjacent location, likely only 1 though
					Int3D newLoc = i3lAdjacent.get(Options.RNG.nextInt(i3lAdjacent.size()));
					
					d3aDirections[i] = new Double3D(newLoc.x - i3Origin.x, newLoc.y - i3Origin.y, newLoc.z - i3Origin.z);
					
					continue;
				}
			}
			
			// make the edges match up with the new cells
			d3aDirections[i] = new Double3D(x - i3Origin.x, y - i3Origin.y, z - i3Origin.z);
			
			i3lCellLocations.add(i3NewPoint);
			ba3CellLocations[x][y][z] = true;
			iCellsCreated++;
		}
		
		return iCellsCreated;
	}
	
	private static Double3D[] generateDirections(int iWidth, int iHeight, int iDepth, boolean[][][] ba3CellLocations, Int3D i3Location, int iCellCount)
	{
 		Double3D[] d3aReturn = new Double3D[iCellCount];
		
 		do
 		{
	 		d3aReturn[0] = new Double3D();
	 		
			for(int i = 1; i < iCellCount; i++)
			{
				
				// -0.5x^4 + 13/3x^3 - 12x^2 + 61/6x + 3 
				// http://www.wolframalpha.com/input/?i=plot+-0.5x%5E4+%2B+13%2F3x%5E3+-+12x%5E2+%2B+61%2F6x+%2B+3.5+between+x%3D0+and+x%3D4
				// TODO I misread the paper and this is a PDF - we want the CDF
				double length = Options.RNG.nextDouble()*4;
				length = - 0.5*Math.pow(length,  4)
						 + 13.0/3.0*Math.pow(length,  3)
						 - 12*Math.pow(length,  2)
						 + 61.0/6.0*length
						 + 3.5;
				
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
				double length = Options.RNG.nextDouble()*4;
				length = - 0.5*Math.pow(length,  4)
						 + 13.0/3.0*Math.pow(length,  3)
						 - 12*Math.pow(length,  2)
						 + 61.0/6.0*length
						 + 3.5;
				
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
	
	private static Int3D pickNextCell(int iWidth, int iHeight, int iDepth, ArrayList<Int3D> i3lCellLocations, boolean[][][] ba3CellLocations)
	{
		int iIndex = 0;
		boolean bSuitable = false;
		
		do
		{
			if ( i3lCellLocations.size() == 0 )
			{
				return null;
			}
			iIndex = Options.RNG.nextInt(i3lCellLocations.size());
			
			ArrayList<Int3D> i3aAdjCells = getAdjacentCells(iWidth, iHeight, iDepth, ba3CellLocations, i3lCellLocations.get(iIndex), Options.FRCGenerator.MAX_EDGE_LENGTH());
			
			if ( i3aAdjCells.size() > 8 )
			{
				i3lCellLocations.remove(iIndex);
				continue;
			}
			
			double dProbability = Math.pow(1.0/i3aAdjCells.size(), 2);
			
			if ( dProbability > Options.RNG.nextDouble() )
			{
				bSuitable = true;
			}
		} while (bSuitable == false);
		
		
		return i3lCellLocations.get(iIndex);
	}
	
	public static ArrayList<Int3D> getAdjacentCells(int iWidth, int iHeight, int iDepth, boolean[][][] ba3CellLocations, Int3D i3Point, int iMaxDistance)
	{
		ArrayList<Int3D> i3lReturn = new ArrayList<Int3D>();

		// Precompute these for efficiency
		// +1 because we want the distance between the close edges of the cells
		int iXLim = Math.min(iMaxDistance + i3Point.x, iWidth-1);
		int iYLim = Math.min(iMaxDistance + i3Point.y, iHeight-1);
		int iZLim = Math.min(iMaxDistance + i3Point.z, iDepth-1);
		
		for ( int x = Math.max(0, i3Point.x-iMaxDistance); x <= iXLim; x++ )
		{
			for ( int y = Math.max(0, i3Point.y-iMaxDistance); y <= iYLim; y++ )
			{
				for ( int z = Math.max(0, i3Point.z-iMaxDistance); z <= iZLim; z++ )
				{
					if ( x == i3Point.x && y == i3Point.y && z == i3Point.z )
					{
						continue;
					}
					// if a cell lives at this location,
					// and the distance is less than the max distance
					// we take one away from the max distance because we want
					// the distance between the closer edges
					if ( ( ba3CellLocations[x][y][z] )
					  && ( calcDistance(i3Point, new Int3D(x, y, z)) <= iMaxDistance )
					   )
					{
						i3lReturn.add(new Int3D(x, y, z));
					}
				}
			}
		}
		
		return i3lReturn;
	}
	
	private static double calcDistance(Int3D i3Point1, Int3D i3Point2)
	{
		return ( Math.sqrt( Math.pow(Math.abs(i3Point1.x-i3Point2.x), 2)
				          + Math.pow(Math.abs(i3Point1.y-i3Point2.y), 2)
				          + Math.pow(Math.abs(i3Point1.z-i3Point2.z), 2)
						  )
		       );
	}
}
