package sim3d.diffusion;
import java.util.EnumMap;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.DoubleGrid3D;
import sim3d.Options;

// TODO parametrise diffusion on a per solute basis

public class Particle extends DoubleGrid3D implements Steppable
{
	private static final long serialVersionUID = 1;

	// Static variables
	
	// Keep track of solute indices
	public static enum TYPE
	{
		CCL19,
		CCL21,
		CXCL13,
		EBI2L
	}
	
	private static int m_iDisplayLevel = 1;
	public static void setDisplayLevel(int iDisplayLevel)
	{
		m_iDisplayLevel = iDisplayLevel;
	}
	public static int getDisplayLevel()
	{
		return m_iDisplayLevel;
	}
	
	private static EnumMap<TYPE,Integer> ms_emTypeMap = new EnumMap<TYPE, Integer>(TYPE.class);
	
	private static Particle[] ms_pParticles = new Particle[4];
	
	// Static methods
	
	public static void reset()
	{
		ms_pParticles = new Particle[4];
		ms_emTypeMap = new EnumMap<TYPE, Integer>(TYPE.class);
	}
	
	public static Particle getInstance(TYPE pType)
	{
		return ms_pParticles[ms_emTypeMap.get(pType)];
	}
	
	public static void add(TYPE ParticleIndex, int x, int y, int z, double amount)
	{
		int index = ms_emTypeMap.get(ParticleIndex);
		final Particle pTarget = ms_pParticles[index];
		
		pTarget.add(x, y, z, amount);
	}
	
	public static void scale(TYPE ParticleIndex, int x, int y, int z, double factor)
	{
		int index = ms_emTypeMap.get(ParticleIndex);
		final Particle pTarget = ms_pParticles[index];
		
		pTarget.add(x, y, z, factor);
	}
	
	public static double[][][] get(TYPE ParticleIndex, int x, int y, int z)
	{
		int index = ms_emTypeMap.get(ParticleIndex);
		final Particle pTarget = ms_pParticles[index];
		
		return pTarget.getArea(x, y, z);
	}
	
	private int m_iWidth;
	private int m_iHeight;
	private int m_iDepth;
	
	private double[][][] m_adDiffusionCoefficients;
	public DoubleGrid2D m_dg2Display;
	
	public Particle(Schedule schedule, TYPE pType, int iWidth, int iHeight, int iDepth)
	{
		super(iWidth, iHeight, iDepth);
		
		 m_dg2Display = new DoubleGrid2D(iWidth, iHeight);
		
		// Register this in the EnumMap
		ms_emTypeMap.put(pType, ms_emTypeMap.size());
		
		// Set member variables
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
		
		// (in 2D) works out to be Gaussian blur convolution matrix {{1,2,1},{2,4,2},{1,2,1}}
		setDiffusionCoefficients(0.75, 2);
		
		// setup up stepping
		ms_pParticles[ms_emTypeMap.get(pType)] = this;
		schedule.scheduleRepeating(this);
	}
	
	
	public void updateDisplay()
	{
		for (int x = 0; x < m_iWidth; x++)
		{
			for (int y = 0; y < m_iHeight; y++)
			{
				m_dg2Display.set(x, y, field[x][y][m_iDisplayLevel]);
			}
		}
	}
	
	public void setDiffusionCoefficients(double dDispersePercent, double dDistanceFactor)
	{
		// initialise member variable
		m_adDiffusionCoefficients = new double[3][3][3];
		
		// used to calculate amount dispersed to the corresponding squares
		double[][][] adDistances = new double[3][3][3];
		
		// normalise so that the outer boxes sum to dDispersePercent
		double dTotalDistance = 0;
		for (int x = -1; x < 2; x++)
		{
			for (int y = -1; y< 2; y++)
			{
				for (int z = -1; z < 2; z++)
				{
					// Not concerned with the middle square
					if ( x == 0 && y == 0 && z == 0 )
					{
						continue;
					}
					
					adDistances[x+1][y+1][z+1] = Math.pow(Math.sqrt(x*x + y*y + z*z), dDistanceFactor);
					dTotalDistance += adDistances[x+1][y+1][z+1];
				}
			}
		}
		double dNormalisingCoefficient = dDispersePercent / dTotalDistance;
		
		// set the coefficients using distances and normalising constants
		for (int x = 0; x < 3; x++)
		{
			for (int y = 0; y< 3; y++)
			{
				for (int z = 0; z < 3; z++)
				{
					m_adDiffusionCoefficients[x][y][z] = dNormalisingCoefficient / adDistances[x][y][z];
				}
			}
		}
		
		// set the middle square
		m_adDiffusionCoefficients[1][1][1] = 1-dDispersePercent;
	}
	
	// Add or remove(!) amounts
	public void add(int x, int y, int z, double amount)
	{
		field[x%m_iWidth][y%m_iHeight][z%m_iDepth] = Math.max(0, field[x%m_iWidth][y%m_iHeight][z%m_iDepth] + amount);
	}
	
	public void scale(int x, int y, int z, double factor)
	{
		field[x%m_iWidth][y%m_iHeight][z%m_iDepth] = field[x%m_iWidth][y%m_iHeight][z%m_iDepth] * factor;
	}
	
	public double[][][] getArea(int x, int y, int z)
	{
		double[][][] adReturn = new double[3][3][3];
		
		for ( int r = 0; r < 3; r++ )
		{
			if ( x+r-1 < 0 || x+r-1 >= m_iWidth )
			{
				continue;
			}
			for ( int s = 0; s < 3; s++ )
			{
				if ( y+s-1 < 0 || y+s-1 >= m_iHeight )
				{
					continue;
				}
				for ( int t = 0; t < 3; t++ )
				{
					if ( z+t-1 < 0 || z+t-1 >= m_iDepth )
					{
						continue;
					}
					adReturn[r][s][t] = field[x+r-1][y+s-1][z+t-1];
				}
			}
		}
		
		return adReturn;
	}
	
	// Actually the inverse of the decay rate to improve efficiency
	public double m_dDecayRateInv = 0.99;
	
	public void step( final SimState state )
	{
		diffuse();
		for (int i = 0; i < Options.DIFFUSION_STEPS; i++)
		{
		}
		updateDisplay();
	}
	public void diffuse()
	{
		//TODO the scheduler is doing things in the wrong order, make sure the diffusion comes last!!!!
		double adConcentrations[][][] = new double[m_iWidth][m_iHeight][m_iDepth];
		//field = new double[m_iWidth][m_iHeight][m_iDepth];
		
		// Adjust for decay
		for(int x = 0; x < m_iWidth; x++)
		{
			for(int y = 0; y < m_iHeight; y++)
			{
				for(int z = 0; z < m_iDepth; z++)
				{
					adConcentrations[x][y][z] = field[x][y][z];// * m_dDecayRateInv;
					field[x][y][z] = 0;
				}
			}
		}
		
		// Make this a bit quicker by only doing it once for each x, and width times for each y
		int xEdge = -1,
			yEdge = -1,
			zEdge = -1;
		
		// so we don't have to keep recalculating these
		int iWidth = m_iWidth -1;
		int iHeight = m_iHeight -1;
		int iDepth = m_iDepth -1;
		
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
									newValue += m_adDiffusionCoefficients[r+1][s+1][t+1] * adConcentrations[x+r][y+s][z+t];
								}
							}
						}
						// so these will always be a step late. I don't think that's too much of an issue
						// TODO consider using the dirichlet code and loop through these cases individually 
						/*field[(xEdge == -1) ? x : xEdge] // if it's -1, set it to the current x val
							 [(yEdge == -1) ? y : yEdge] // etc.
							 [(zEdge == -1) ? z : zEdge] // i.e. this one isn't on the edge
									 += newValue;*/
					}
					else
					{
						if ( x * y * z == 1 )
						{
							x=x;
						}
						// sum up all the surrounding values multiplied by their respective coefficient
						double newValue = 0;
						for (int r = -1; r < 2; r++)
						{
							for (int s = -1; s < 2; s++)
							{
								for (int t = -1; t < 2; t++)
								{
									newValue += m_adDiffusionCoefficients[r+1][s+1][t+1] * adConcentrations[x+r][y+s][z+t];
								}
							}
						}
						field[x][y][z] = newValue;
					}
					zEdge = -1;
				}
				yEdge = -1;
			}
			xEdge = -1;
		}
	}
}
