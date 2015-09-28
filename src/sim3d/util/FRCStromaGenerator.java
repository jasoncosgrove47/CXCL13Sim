package sim3d.util;

import java.util.ArrayList;

import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;

public class FRCStromaGenerator {

	public static boolean[][][] generateStroma3D(int iWidth, int iHeight, int iDepth, int iCellCount)
	{
		boolean[][][] ba3CellLocations = new boolean[iWidth][iHeight][iDepth];
		
		// It will be efficient to keep track of cells and locations separately
		ArrayList<Int3D> i3lCellLocations = new ArrayList<Int3D>(); 
		i3lCellLocations.add(new Int3D(iWidth/2, iHeight/2, iDepth/2));
		ba3CellLocations[iWidth/2][iHeight/2][iDepth/2] = true;
		
		int iRemainingCells = iCellCount - 1;
		
		while ( iRemainingCells > 0 && i3lCellLocations.size() > 0 )
		{
			Double3D[] d3aDirections = new Double3D[0];
			Int3D i3NextCell;
			
			boolean bSuccess = false;
			do
			{
				i3NextCell = pickNextCell(iWidth, iHeight, iDepth, i3lCellLocations, ba3CellLocations);
				
				if ( i3NextCell == null )
				{
					break;
				}
				
				int iEdges = getAdjacentCells(iWidth, iHeight, iDepth, ba3CellLocations, i3NextCell, Options.FRCGenerator.MAX_EDGE_LENGTH).size();
				
				iEdges = Math.min(iRemainingCells, (int)Math.round(Options.RNG.nextDouble()*(6-iEdges)+1));
				
				if ( iEdges < 0 )
				{
					continue;
				}
				
				d3aDirections = generateDirections(iWidth, iHeight, iDepth, ba3CellLocations, i3NextCell, iEdges);
				
				if ( d3aDirections != null )
				{
					bSuccess = true;
				}
			} while (!bSuccess);
			
			if (bSuccess)
			{
				iRemainingCells -= createNewCells(iWidth, iHeight, iDepth, i3lCellLocations, ba3CellLocations, i3NextCell, d3aDirections);
			}
		}
		
		return ba3CellLocations;
	}
	
	private static int createNewCells(int iWidth, int iHeight, int iDepth, ArrayList<Int3D> i3lCellLocations, boolean[][][] ba3CellLocations, Int3D i3Origin, Double3D[] d3aDirections)
	{
		int iCellsCreated = 0;
		
		for ( Double3D d3Direction : d3aDirections )
		{
			int x, y, z;
			x = (int) Math.round(i3Origin.x + d3Direction.x);
			y = (int) Math.round(i3Origin.y + d3Direction.y);
			z = (int) Math.round(i3Origin.z + d3Direction.z);
			
			Int3D i3NewPoint = new Int3D(x, y, z);
			
			// Check if one already exists nearby
			if(ba3CellLocations[x][y][z])
			{
				continue;
			}
			
			i3lCellLocations.add(i3NewPoint);
			ba3CellLocations[x][y][z] = true;
			iCellsCreated++;
		}
		
		return iCellsCreated;
	}
	
	private static Double3D[] generateDirections(int iWidth, int iHeight, int iDepth, boolean[][][] ba3CellLocations, Int3D i3Location, int iCellCount)
	{
 		Double3D[] d3aReturn = new Double3D[iCellCount];
		boolean bSuccess = false;
		
		int iOuterSkips = 0;
		do
		{
			if ( iOuterSkips > 1000 )
			{
				return null;
			}
			
			int iInnerSkips = 0;
			Double3D d3VectorSum = new Double3D();
			
			for(int i = 0; i < iCellCount; i++)
			{
				if ( iInnerSkips > 1000 )
				{
					return null;
				}
				
				// -0.5x^4 + 13/3x^3 - 12x^2 + 61/6x + 2 
				
				double length = Options.RNG.nextDouble()*4;
				length = - 0.5*Math.pow(length,  4)
						 + 13.0/3.0*Math.pow(length,  3)
						 - 12*Math.pow(length,  2)
						 + 61.0/6.0*length
						 + 3.5;
				
				d3aReturn[i] = Vector3DHelper.getRandomDirection().multiply(length);

				int x, y, z;
				x = (int) Math.round(i3Location.x + d3aReturn[i].x);
				y = (int) Math.round(i3Location.y + d3aReturn[i].y);
				z = (int) Math.round(i3Location.z + d3aReturn[i].z);
				
				if ( x < 0 || x >= iWidth
				  || y < 0 || y >= iHeight
				  || z < 0 || z >= iDepth
				  || ba3CellLocations[x][y][z])
				{
					i--;
					iInnerSkips++;
					continue;
				}
				
				d3VectorSum = d3VectorSum.add(d3aReturn[i]);
			}
			
			if ( d3VectorSum.length() < 2 )
			{
				bSuccess = true;
			}
			else
			{
				iOuterSkips++;
			}
		} while (!bSuccess);
		
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
			
			ArrayList<Int3D> i3aAdjCells = getAdjacentCells(iWidth, iHeight, iDepth, ba3CellLocations, i3lCellLocations.get(iIndex), Options.FRCGenerator.MAX_EDGE_LENGTH);
			
			if ( i3aAdjCells.size() > 7 )
			{
				i3lCellLocations.remove(iIndex);
				continue;
			}
			
			//TODO we could do a weighted thing here so if there are more closer ones then it's less likely
			
			double dProbability = Math.pow(1.0/i3aAdjCells.size(), 2);
			
			if ( dProbability > Options.RNG.nextDouble() )
			{
				bSuitable = true;
			}
		} while (bSuitable == false);
		
		
		return i3lCellLocations.get(iIndex);
	}
	
	private static ArrayList<Int3D> getAdjacentCells(int iWidth, int iHeight, int iDepth, boolean[][][] ba3CellLocations, Int3D i3Point, int iMaxDistance)
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
