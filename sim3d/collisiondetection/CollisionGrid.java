package sim3d.collisiondetection;

import java.util.ArrayList;

import sim.util.Double3D;

public class CollisionGrid
{
	// Private Members
	private ArrayList<Collidable>[][][] m_clGridSpaces;
	
	private double m_dDiscretisation;
	
	private int m_iWidth, m_iHeight, m_iDepth;
	
	// Public Methods
	//@SuppressWarnings("unchecked")
	public CollisionGrid(int iWidth, int iHeight, int iDepth, double dDiscretisation)
	{
		m_dDiscretisation = dDiscretisation;

		m_iWidth = (int)Math.ceil(iWidth / dDiscretisation);
		m_iHeight = (int)Math.ceil(iHeight / dDiscretisation);
		m_iDepth = (int)Math.ceil(iDepth / dDiscretisation);
		
		m_clGridSpaces = new ArrayList[m_iWidth][m_iHeight][m_iDepth];
	}
	
	public boolean BoxSphereIntersect(double dSphereX, double dSphereY, double dSphereZ, double dRadiusSquare,
									  int iBoxX, int iBoxY, int iBoxZ)
	{
		double dSum = 0;
		
		if ( dSphereX < iBoxX )
		{
			dSum += (dSphereX - iBoxX)*(dSphereX - iBoxX);
		}
		else if ( dSphereX > iBoxX + 1 )
		{
			dSum += (dSphereX - iBoxX + 1)*(dSphereX - iBoxX + 1);
		}
		
		if ( dSphereY < iBoxY )
		{
			dSum += (dSphereY - iBoxY)*(dSphereY - iBoxY);
		}
		else if ( dSphereY > iBoxY + 1 )
		{
			dSum += (dSphereY - iBoxY + 1)*(dSphereY - iBoxY + 1);
		}
		
		if ( dSphereZ < iBoxZ )
		{
			dSum += (dSphereZ - iBoxZ)*(dSphereZ - iBoxZ);
		}
		else if ( dSphereZ > iBoxZ + 1 )
		{
			dSum += (dSphereZ - iBoxZ + 1)*(dSphereZ - iBoxZ + 1);
		}
		
		return dSum*dSum < dRadiusSquare;
	}
	
	public void addSphereToGrid(Collidable cObject, Double3D d3Centre, double dRadius)
	{
		Double3D d3DiscretisedCentre = new Double3D(d3Centre.x/m_dDiscretisation,
													d3Centre.y/m_dDiscretisation,
													d3Centre.z/m_dDiscretisation);
		
		double dDiscretisedRadius = dRadius / m_dDiscretisation;
		
		// TODO can this be done quicker?
		// maybe convert it all to the discretised stuff first...
		
		// this will be used a lot - pre compute for speed
		double dRadiusSquare = dDiscretisedRadius * dDiscretisedRadius;

		int iXLow = (int)(d3DiscretisedCentre.x-dRadius);
		int iXHigh = (int)(d3DiscretisedCentre.x+dRadius);

		int iYLow = (int)(d3DiscretisedCentre.y-dRadius);
		int iYHigh = (int)(d3DiscretisedCentre.y+dRadius);

		int iZLow = (int)(d3DiscretisedCentre.z-dRadius);
		int iZHigh = (int)(d3DiscretisedCentre.z+dRadius);
		
		for ( int x = iXLow; x <= iXHigh; x++ )
		{
			for ( int y = iYLow; y <= iYHigh; y++ )
			{
				for ( int z = iZLow; z <= iZHigh; z++ )
				{
					if ( BoxSphereIntersect(d3DiscretisedCentre.x, d3DiscretisedCentre.y, d3DiscretisedCentre.z, dRadiusSquare, x, y, z) )
					{
						m_clGridSpaces[x][y][z].add(cObject);
						// TODO check for collisions here
					}
				}
			}
		}
	}
	
	public void addLineToGrid(Collidable cObject, Double3D d3Point1, Double3D d3Point2, double dRadius)
	{
		//TODO this
		Double3D d3DiscretisedCentre = new Double3D(d3Centre.x/m_dDiscretisation,
													d3Centre.y/m_dDiscretisation,
													d3Centre.z/m_dDiscretisation);
		
		double dDiscretisedRadius = dRadius / m_dDiscretisation;
		
		// TODO can this be done quicker?
		// maybe convert it all to the discretised stuff first...
		
		// this will be used a lot - pre compute for speed
		double dRadiusSquare = dDiscretisedRadius * dDiscretisedRadius;

		int iXLow = (int)(d3DiscretisedCentre.x-dRadius);
		int iXHigh = (int)(d3DiscretisedCentre.x+dRadius);

		int iYLow = (int)(d3DiscretisedCentre.y-dRadius);
		int iYHigh = (int)(d3DiscretisedCentre.y+dRadius);

		int iZLow = (int)(d3DiscretisedCentre.z-dRadius);
		int iZHigh = (int)(d3DiscretisedCentre.z+dRadius);
		
		for ( int x = iXLow; x <= iXHigh; x++ )
		{
			for ( int y = iYLow; y <= iYHigh; y++ )
			{
				for ( int z = iZLow; z <= iZHigh; z++ )
				{
					if ( BoxSphereIntersect(d3DiscretisedCentre.x, d3DiscretisedCentre.y, d3DiscretisedCentre.z, dRadiusSquare, x, y, z) )
					{
						m_clGridSpaces[x][y][z].add(cObject);
						// TODO check for collisions here
					}
				}
			}
		}
	}
}
