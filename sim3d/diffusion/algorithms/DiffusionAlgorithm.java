package sim3d.diffusion.algorithms;

import sim3d.Options;
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
		int[][][] ia3Concentrations = new int[m_iWidth][m_iHeight][m_iDepth];
		
		// Copy the contents of field over
		// Note: we can't use clone() or just set the variable because of the fact that Java uses pointers
		// too much so they would both still reference the same variable.
		for(int x = 0; x < m_iWidth; x++)
		{
			for(int y = 0; y < m_iHeight; y++)
			{
				for(int z = 0; z < m_iDepth; z++)
				{
					ia3Concentrations[x][y][z] = pSpace.field[x][y][z];
				}
			}
		}
		
		// Precalculate these values for the edge checks inside the for loops
		// so we don't have to keep recalculating them
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;
		
		// Loop through the grid spaces ignoring borders for now
		for(int x = 1; x < iWidth; x++)
		{
			for(int y = 1; y < iHeight; y++)
			{
				for(int z = 1; z < iDepth; z++)
				{
					int iCount = 0;
					for (int r = -1; r < 2; r++)
					{
						for (int s = -1; s < 2; s++)
						{
							for (int t = -1; t < 2; t++)
							{
								//pSpace.field[x][y][z] += m_adDiffusionCoefficients[r+1][s+1][t+1] * (adConcentrations[x+r][y+s][z+t] - adConcentrations[x][y][z]);
								// D*dT/dX^2 from http://pauli.uni-muenster.de/tp/fileadmin/lehre/NumMethoden/WS0910/ScriptPDE/Heat.pdf
								int iDelta = (int) (m_adDiffusionCoefficients[r+1][s+1][t+1] * (ia3Concentrations[x][y][z] - ia3Concentrations[x+r][y+s][z+t]));
								if ( iDelta > 0 )
								{
									pSpace.field[x+r][y+s][z+t] += iDelta;
									pSpace.field[x][y][z] -= iDelta;
								}
								iCount += (int) (m_adDiffusionCoefficients[r+1][s+1][t+1] * (ia3Concentrations[x][y][z]));
							}
						}
					}
					for ( int iRemainder = ia3Concentrations[x][y][z] - iCount; iRemainder > 0; iRemainder--)
					{
						int iRandom = Options.RNG.nextInt(27);
						pSpace.field[x+(iRandom/9-1)][y+((iRandom%9)/3-1)][z+((iRandom%3)-1)]++;
						pSpace.field[x][y][z]--;
					}
				}
			}
		}
		
		// Now enforce the boundary condition
		// Code is a little inefficient, but it's clean at least
		int xEdge = -1,
			yEdge = -1,
			zEdge = -1;
		
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
					
					if ( xEdge != -1 || yEdge != -1 || zEdge != -1 )
					{
						// just bounce it all back in
						pSpace.field[(xEdge == -1) ? x : xEdge] // if it's -1, set it to the current x val
									[(yEdge == -1) ? y : yEdge] // etc.
									[(zEdge == -1) ? z : zEdge] // i.e. this one isn't on the edge
									+= pSpace.field[x][y][z];
						pSpace.field[x][y][z] = 0;
					}
					else if ( xEdge == -1 && yEdge == -1 )
					{
						// this will be z = 0 and we skip to the end to save time 
						z = m_iDepth - 2;
					}
					
					zEdge = -1;
				}
				
				yEdge = -1;
			}
			
			xEdge = -1;
		}
	}
}
