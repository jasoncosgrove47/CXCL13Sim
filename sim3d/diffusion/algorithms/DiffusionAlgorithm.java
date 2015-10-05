package sim3d.diffusion.algorithms;

import sim3d.diffusion.Particle;

/**
 * Generic diffusion algorithm class. Only handles diffusion, not particle decay. It is up to the
 * implementing class to set the diffusion coefficients in m_adDiffusionCoefficients depending on
 * the algorithm used. These coefficients are used with the difference in concentration to determine
 * the amount diffused. Currently implemented as a continuous process in a discrete space - that is
 * the amount of particle in a particular grid space is represented as a double.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class DiffusionAlgorithm
{
	/**
	 * The height, width, and depth of the space being diffused in
	 */
	protected int m_iWidth, m_iHeight, m_iDepth;
	
	/**
	 * The relative amount of diffusion to each neighbour in the diffusion neighbourhood,
	 * usually proportional to the distance to the neighbour
	 */
	protected double[][][] m_adDiffusionCoefficients;
	
	/**
	 * Constructor. Sets member variables
	 * @param iWidth Width of space
	 * @param iHeight Height of space
	 * @param iDepth Depth of space
	 */
	protected DiffusionAlgorithm(int iWidth, int iHeight, int iDepth)
	{
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
	}
	
	/**
	 * Visitor design pattern that performs one diffusion step of the particle to the immediate
	 * diffusion neighbourhood using the coefficients set in m_adDiffusionCoefficients.
	 * @param pSpace The Particle object to visit
	 */
	public void diffuse(Particle pSpace)
	{
		// A temporary variable containing the previous value of pSpace.field
		double adConcentrations[][][] = new double[m_iWidth][m_iHeight][m_iDepth];
		
		// Copy the contents of field over
		// Note: we can't use clone() or just set the variable because of the fact that Java uses pointers
		// too much so they would both still reference the same variable.
		for(int x = 0; x < m_iWidth; x++)
		{
			for(int y = 0; y < m_iHeight; y++)
			{
				for(int z = 0; z < m_iDepth; z++)
				{
					adConcentrations[x][y][z] = pSpace.field[x][y][z];
				}
			}
		}
		
		// Make this a bit quicker by only doing it once for each x, and width times for each y
		int xEdge = -1,
			yEdge = -1,
			zEdge = -1;
		
		// Precalculate these values for the edge checks inside the for loops
		// so we don't have to keep recalculating them
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;
		
		// Loop through the x coordinates
		for(int x = 0; x < m_iWidth; x++)
		{
			// Check if this is on the x edge
			// So this will be calculated m_iWidth times in total
			if ( x == 0 )
			{
				xEdge = 1; 
			}
			else if ( x == iWidth )
			{
				xEdge = m_iWidth - 2;
			}
			
			// Loop through the y coordinates
			for(int y = 0; y < m_iHeight; y++)
			{
				// Check if this is on the y edge
				// So this will be calculated m_iWidth*m_iHeight times in total
				if ( y == 0 )
				{
					yEdge = 1; 
				}
				else if ( y == iHeight )
				{
					yEdge = m_iHeight - 2;
				}
				
				// Loop through the z coordinates 
				for(int z = 0; z < m_iDepth; z++)
				{
					// Check if this is on the z edge
					// and this will be calculated m_iWidth*m_iHeight*m_iDepth times in total
					if ( z == 0 )
					{
						zEdge = 1; 
					}
					else if ( z == iDepth )
					{
						zEdge = m_iDepth - 2;
					}
					
					// If we're on any of the edges...
					// TODO is bitwise and (xEdge&yEdge&zEdge != -1) or just adding faster than this?
					// they're quicker operations, but will always go through all the cases whereas this
					// will stop as soon as one returns true
					if ( xEdge != -1 || yEdge != -1 || zEdge != -1 )
					{
						// calculate the diffusion to this square
						double newValue = 0;
						for (int r = -1; r < 2; r++)
						{
							if ( x+r < 0 || x+r+1 > m_iWidth )
							{
								continue;
							}
							for (int s = -1; s < 2; s++)
							{
								if ( y+s < 0 || y+s+1 > m_iHeight )
								{
									continue;
								}
								for (int t = -1; t < 2; t++)
								{
									if ( z+t < 0 || z+t+1 > m_iDepth )
									{
										continue;
									}
									newValue += m_adDiffusionCoefficients[r+1][s+1][t+1] * (adConcentrations[x+r][y+s][z+t] - adConcentrations[x][y][z]);
								}
							}
						}
						// And just bounce it all back in
						pSpace.field[(xEdge == -1) ? x : xEdge] // if it's -1, set it to the current x val
									[(yEdge == -1) ? y : yEdge] // etc.
									[(zEdge == -1) ? z : zEdge] // i.e. this one isn't on the edge
									 += newValue;
					}
					else
					{
						// sum up all the surrounding values multiplied by their respective coefficient
						// and add it to the field
						// Note: this will maintain the sum of the field values because the interactions
						// are symmetrical with the surrounding squares (i.e. if space (1,1,1) gains 1 
						// from (1,1,2), then (1,1,2) loses 1 to (1,1,1))
						for (int r = -1; r < 2; r++)
						{
							for (int s = -1; s < 2; s++)
							{
								for (int t = -1; t < 2; t++)
								{
									pSpace.field[x][y][z] += m_adDiffusionCoefficients[r+1][s+1][t+1] * (adConcentrations[x+r][y+s][z+t] - adConcentrations[x][y][z]);
								}
							}
						}
					}
					
					// Set the edges back to -1 so that the next loop doesn't still think we're on the edge
					zEdge = -1;
				}
				yEdge = -1;
			}
			xEdge = -1;
		}
	}
}
