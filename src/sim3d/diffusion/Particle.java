package sim3d.diffusion;
import java.util.EnumMap;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.DoubleGrid3D;
import sim3d.Options;
import sim3d.diffusion.algorithms.DiffusionAlgorithm;

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
	
	private DiffusionAlgorithm m_daDiffusionAlgorithm;
	
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
		m_daDiffusionAlgorithm = new sim3d.diffusion.algorithms.Grajdeanu(10, iWidth, iHeight, iDepth);
		
		// setup up stepping
		ms_pParticles[ms_emTypeMap.get(pType)] = this;
		schedule.scheduleRepeating(this, 1, 1);
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
	public double m_dDecayRateInv = 0.9;
	
	public void step( final SimState state )
	{
		decay();
		
		for (int i = 0; i < Options.DIFFUSION_STEPS; i++)
		{
			m_daDiffusionAlgorithm.diffuse(this);
		}
		
		updateDisplay();
	}
	
	public void decay()
	{
		// Adjust for decay
		for(int x = 0; x < m_iWidth; x++)
		{
			for(int y = 0; y < m_iHeight; y++)
			{
				for(int z = 0; z < m_iDepth; z++)
				{
					field[x][y][z] *=  m_dDecayRateInv;
				}
			}
		}
	}
}
