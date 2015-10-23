package sim3d.collisiondetection;

import java.util.List;
import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim.util.Int3D;

public class CollisionGrid implements Steppable
{

	private static final long serialVersionUID = 1L;
	
	// Private Members
	// TODO LinkedList? since we are mostly adding and removing
	private List<Collidable>[][][] m_clGridSpaces;
	private int[][][] m_ia3GridUpdateStep;
	private List<Int3D> m_i3lCollisionPoints = new ArrayList<Int3D>();
	
	private double m_dDiscretisation;
	
	private int m_iWidth, m_iHeight, m_iDepth;
	
	private int m_iCurrentStep = 0;
	
	// Private Methods

	private void addCollisionPotential(int x, int y, int z, Collidable cObject)
	{
		if ( m_clGridSpaces[x][y][z] == null )
		{
			m_clGridSpaces[x][y][z] = new ArrayList();
		}
		if ( m_ia3GridUpdateStep[x][y][z] < m_iCurrentStep )
		{
			m_ia3GridUpdateStep[x][y][z] = m_iCurrentStep;
			
			int i = 0;
			
			while ( i < m_clGridSpaces[x][y][z].size() )
			{
				if ( !m_clGridSpaces[x][y][z].get(i).isStatic() )
				{
					m_clGridSpaces[x][y][z].remove(i);
				}
				else
				{
					i++;
				}
			}
		}
		
		m_clGridSpaces[x][y][z].add(cObject);
		
		if ( m_clGridSpaces[x][y][z].size() == 2 )
		{
			m_i3lCollisionPoints.add(new Int3D(x, y, z));
			
			// There's a potential collision so tell the cells, too
			m_clGridSpaces[x][y][z].get(0).addCollisionPoint(new Int3D(x, y, z));
			cObject.addCollisionPoint(new Int3D(x, y, z));
		}
		else if ( m_clGridSpaces[x][y][z].size() > 2 )
		{
			// There's a potential collision so tell the cells, too
			cObject.addCollisionPoint(new Int3D(x, y, z));
		}
	}
	
	// Public Methods
	@SuppressWarnings("unchecked")
	public CollisionGrid(int iWidth, int iHeight, int iDepth, double dDiscretisation)
	{
		m_dDiscretisation = dDiscretisation;

		m_iWidth = (int)Math.ceil(iWidth / dDiscretisation);
		m_iHeight = (int)Math.ceil(iHeight / dDiscretisation);
		m_iDepth = (int)Math.ceil(iDepth / dDiscretisation);
		
		m_clGridSpaces = new ArrayList[m_iWidth][m_iHeight][m_iDepth];
		m_ia3GridUpdateStep = new int[m_iWidth][m_iHeight][m_iDepth];
	}
	
	public List<Collidable> getPoints( Int3D i3Loc )
	{
		return m_clGridSpaces[i3Loc.x][i3Loc.y][i3Loc.z];
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

		int iXLow = (int)Math.max(0,(d3DiscretisedCentre.x-dDiscretisedRadius));
		int iXHigh = (int)Math.min(m_iWidth-1,(d3DiscretisedCentre.x+dDiscretisedRadius));

		int iYLow = (int)Math.max(0,(d3DiscretisedCentre.y-dDiscretisedRadius));
		int iYHigh = (int)Math.min(m_iHeight-1,(d3DiscretisedCentre.y+dDiscretisedRadius));

		int iZLow = (int)Math.max(0,(d3DiscretisedCentre.z-dDiscretisedRadius));
		int iZHigh = (int)Math.min(m_iDepth-1,(d3DiscretisedCentre.z+dDiscretisedRadius));
		
		for ( int x = iXLow; x <= iXHigh; x++ )
		{
			for ( int y = iYLow; y <= iYHigh; y++ )
			{
				for ( int z = iZLow; z <= iZHigh; z++ )
				{
					if ( BoxSphereIntersect(d3DiscretisedCentre.x, d3DiscretisedCentre.y, d3DiscretisedCentre.z, dRadiusSquare, x, y, z) )
					{
						addCollisionPotential(x, y, z, cObject);
						// TODO check for collisions here
					}
				}
			}
		}
	}
	
	public void addLineToGrid(Collidable cObject, Double3D d3Point1, Double3D d3Point2, double dRadius)
	{
		//TODO this
		Double3D d3DiscretisedPoint1 = new Double3D(d3Point1.x/m_dDiscretisation,
													d3Point1.y/m_dDiscretisation,
													d3Point1.z/m_dDiscretisation);
		Double3D d3DiscretisedPoint2 = new Double3D(d3Point2.x/m_dDiscretisation,
													d3Point2.y/m_dDiscretisation,
													d3Point2.z/m_dDiscretisation);
		
		double dDiscretisedRadius = dRadius / m_dDiscretisation;
		
		// this will be used a lot - pre compute for speed
		// Add 0.5 as this is approximately a cube
		double dRadiusSquare = (0.5+dDiscretisedRadius) * (0.5+dDiscretisedRadius);

		int iXLow, iXHigh, iYLow, iYHigh, iZLow, iZHigh;
		
		if (d3DiscretisedPoint1.x < d3DiscretisedPoint2.x)
		{
			iXLow = (int)Math.max(0,(d3DiscretisedPoint1.x-dDiscretisedRadius));
			iXHigh = (int)Math.min(m_iWidth-1,(d3DiscretisedPoint2.x+dDiscretisedRadius));
		}
		else
		{
			iXLow = (int)Math.max(0,(d3DiscretisedPoint2.x-dDiscretisedRadius));
			iXHigh = (int)Math.min(m_iWidth-1,(d3DiscretisedPoint1.x+dDiscretisedRadius));
		}

		if (d3DiscretisedPoint1.y < d3DiscretisedPoint2.y)
		{
			iYLow = (int)Math.max(0,(d3DiscretisedPoint1.y-dDiscretisedRadius));
			iYHigh = (int)Math.min(m_iHeight-1,(d3DiscretisedPoint2.y+dDiscretisedRadius));
		}
		else
		{
			iYLow = (int)Math.max(0,(d3DiscretisedPoint2.y-dDiscretisedRadius));
			iYHigh = (int)Math.min(m_iHeight-1,(d3DiscretisedPoint1.y+dDiscretisedRadius));
		}
		
		if (d3DiscretisedPoint1.z < d3DiscretisedPoint2.z)
		{
			iZLow = (int)Math.max(0,(d3DiscretisedPoint1.z-dDiscretisedRadius));
			iZHigh = (int)Math.min(m_iDepth-1,(d3DiscretisedPoint2.z+dDiscretisedRadius));
		}
		else
		{
			iZLow = (int)Math.max(0,(d3DiscretisedPoint2.z-dDiscretisedRadius));
			iZHigh = (int)Math.min(m_iDepth-1,(d3DiscretisedPoint1.z+dDiscretisedRadius));
		}
		
		for ( int x = iXLow; x <= iXHigh; x++ )
		{
			for ( int y = iYLow; y <= iYHigh; y++ )
			{
				for ( int z = iZLow; z <= iZHigh; z++ )
				{
					// https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf p130
					
					double length = 0;
					
					Double3D ab = d3DiscretisedPoint1.subtract(d3DiscretisedPoint2);
					// add 0.5 so it's the centre of the square
					Double3D ac = new Double3D(x + 0.5 - d3DiscretisedPoint1.x,
											   y + 0.5 - d3DiscretisedPoint1.y,
											   z + 0.5 - d3DiscretisedPoint1.z);
					
					double e = ac.x*ab.x + ac.y*ab.y + ac.z*ab.z;
					
					if ( e <= 0 )
					{
						length = ac.x*ac.x + ac.y*ac.y + ac.z*ac.z;
					}
					else
					{
						double f = ab.x*ab.x + ab.y*ab.y + ab.z*ab.z;
						
						if ( e >= f )
						{
							Double3D bc = new Double3D(x + 0.5 - d3DiscretisedPoint2.x,
													   y + 0.5 - d3DiscretisedPoint2.y,
													   z + 0.5 - d3DiscretisedPoint2.z);
							
							length = bc.x*bc.x + bc.y*bc.y + bc.z*bc.z;
						}
						else
						{
							length = ac.x*ac.x + ac.y*ac.y + ac.z*ac.z - e * e / f;
						}
					}
					
					if ( length <= dRadiusSquare )
					{
						addCollisionPotential(x, y, z, cObject);
					}
				}
			}
		}
	}

	@Override
	public void step(SimState state)
	{
		//System.out.println(m_i3lCollisionPoints.size());
		
		for ( Int3D i3CollisionPoint : m_i3lCollisionPoints )
		{
			for ( Collidable cObj : getPoints(i3CollisionPoint) )
			{
				cObj.handleCollisions(this);
			}
		}
		
		m_i3lCollisionPoints.clear();
		m_iCurrentStep++;
	}
}
