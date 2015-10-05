package sim3d.diffusion.algorithms;

import sim3d.diffusion.Particle;

public class DiffusionAlgorithm
{
	protected int m_iWidth, m_iHeight, m_iDepth;
	protected double[][][] m_adDiffusionCoefficients;
	
	protected DiffusionAlgorithm(int iWidth, int iHeight, int iDepth)
	{
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
	}
	
	public void diffuse(Particle pSpace)
	{
		double adConcentrations[][][] = new double[m_iWidth][m_iHeight][m_iDepth];
		
		// Adjust for decay
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
		
		// so we don't have to keep recalculating these
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;
		
		for(int x = 0; x < m_iWidth; x++)
		{
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
					// and this will be calculated m_iWidth*m_iHeight*m_iDepth times in total
					if ( z == 0 )
					{
						zEdge = 1; 
					}
					else if ( z == iDepth )
					{
						zEdge = m_iDepth - 2;
					}
					
					// TODO is bitwise and (xEdge&yEdge&zEdge != -1) or just adding faster than this?
					// they're quicker operations, but will always go through all the cases whereas this
					// will stop as soon as one returns true
					if ( xEdge != -1 || yEdge != -1 || zEdge != -1 )
					{
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
						pSpace.field[(xEdge == -1) ? x : xEdge] // if it's -1, set it to the current x val
									[(yEdge == -1) ? y : yEdge] // etc.
									[(zEdge == -1) ? z : zEdge] // i.e. this one isn't on the edge
									 += newValue;
					}
					else
					{
						// sum up all the surrounding values multiplied by their respective coefficient
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
					zEdge = -1;
				}
				yEdge = -1;
			}
			xEdge = -1;
		}
	}
}
