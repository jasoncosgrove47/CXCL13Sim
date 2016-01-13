package sim3d.diffusion;

import java.util.EnumMap;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.DoubleGrid3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.diffusion.algorithms.DiffusionAlgorithm;

/**
 * Same as Particle but accounts for Moles not absoloute molecules
 * TODO: Should really just inherit from particle and overwrite 
 * what is necessary but this is fine for now
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class ParticleMoles extends DoubleGrid3D implements Steppable
{
	/**
	 * ENUM for the chemokine types
	 */
	public static enum TYPE
	{
		CCL19, CCL21, CXCL13, EBI2L
	}
	
	// Static variables
	
	/**
	 * The z-index to display
	 */
	private static int m_iDisplayLevel = 1;
	
	/**
	 * Gives each ENUM an array index
	 */
	private static EnumMap<TYPE, Integer>	ms_emTypeMap	= new EnumMap<TYPE, Integer>( TYPE.class );
	
	/**
	 * The instances of Particle being handled
	 */
	private static ParticleMoles[]				ms_pParticles	= new ParticleMoles[4];
	
	private static final long serialVersionUID = 1;
	
	/**
	 * Add or remove chemokine from a grid space
	 * @param ParticleType The ENUM for the type of particle
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @param amount Positive or negative absolute change in particle amount
	 */
	public static void add( TYPE ParticleType, int x, int y, int z, double amount )
	{
		int index = ms_emTypeMap.get( ParticleType );
		final ParticleMoles pTarget = ms_pParticles[index];
		
		// NB: this function will make sure the amount is always positive in the grid
		pTarget.add( x, y, z, amount );
	}
	
	/**
	 * Gets the amount of particle in the immediate vicinity of the given position in a 3x3x3 array
	 * @param ParticleType The ENUM for the type of particle
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @return a 3x3x3 array containing the amount in the neighbouring grid spaces
	 */
	public static double[][][] get( TYPE ParticleType, int x, int y, int z )
	{
		int index = ms_emTypeMap.get( ParticleType );
		final ParticleMoles pTarget = ms_pParticles[index];
		
		return pTarget.getArea( x, y, z );
	}
															
	// Static methods
	
	/**
	 * Accessor for m_iDisplayLevel
	 */
	public static int getDisplayLevel()
	{
		return m_iDisplayLevel;
	}
	
	/**
	 * Accessor for the particle instances
	 * @param pType The ENUM for the type of particle
	 * @return The Particle object
	 */
	public static ParticleMoles getInstance( TYPE pType )
	{
		return ms_pParticles[ms_emTypeMap.get( pType )];
	}
	
	/**
	 * Resets all particle to their initial state
	 */
	public static void reset()
	{
		ms_pParticles = new ParticleMoles[4];
		ms_emTypeMap = new EnumMap<TYPE, Integer>( TYPE.class );
	}
	
	/**
	 * Scale the amount of chemokine in a grid space.
	 * NB: does not check if this value is positive
	 * @param ParticleType The ENUM for the type of particle
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @param factor The coefficient of multiplication
	 */
	public static void scale( TYPE ParticleType, int x, int y, int z, double factor )
	{
		int index = ms_emTypeMap.get( ParticleType );
		final ParticleMoles pTarget = ms_pParticles[index];
		
		pTarget.scale( x, y, z, factor );
	}
	
	/**
	 * Setter for m_iDisplayLevel
	 */
	public static void setDisplayLevel( int iDisplayLevel )
	{
		m_iDisplayLevel = iDisplayLevel;
	}
	
	/**
	 * The coefficient used for particle decay
	 * TODO: this needs to be an external input and should definitely not be this high
	 */
	public double m_dDecayRateInv = 0.9;
	
	/**
	 * A 2D grid containing the values using m_iDisplayIndex as the z-index
	 */
	public DoubleGrid2D			m_ig2Display;
	
	/**
	 * The DiffusionAlgorithm to use
	 */
	private DiffusionAlgorithm	m_daDiffusionAlgorithm;
	
	/**
	 * Width of the particle diffusion space
	 */
	private int					m_iDepth;
	
	/**
	 * Height of the particle diffusion space
	 */
	private int					m_iHeight;

	/**
	 * Depth of the particle diffusion space
	 */
	private int					m_iWidth;
	
	/**
	 * Constructor
	 * @param schedule The MASON Schedule object
	 * @param pType The ENUM for the type of particle this is
	 * @param iWidth Width of the grid
	 * @param iHeight Height of the grid
	 * @param iDepth Depth of the grid
	 */
	public ParticleMoles( Schedule schedule, TYPE pType, int iWidth, int iHeight, int iDepth )
	{
		super( iWidth, iHeight, iDepth );
		
		m_ig2Display = new DoubleGrid2D( iWidth, iHeight );
		
		// Register this in the EnumMap
		ms_emTypeMap.put( pType, ms_emTypeMap.size() );
		
		// Set member variables
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
		
		//TODO I don't think this diffusion coefficient is being used anymore
		m_daDiffusionAlgorithm = new sim3d.diffusion.algorithms.Grajdeanu( 10, iWidth, iHeight, iDepth );
		
		// setup up stepping
		ms_pParticles[ms_emTypeMap.get( pType )] = this;
		
		// 3 so out of sync with agents
		schedule.scheduleRepeating( this, 3, 1 );
	}
	
	/**
	 * Add or remove chemokine from a grid space
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @param amount Positive or negative absolute change in particle amount
	 */
	public void add( int x, int y, int z, double amount )
	{
		field[x % m_iWidth][y % m_iHeight][z % m_iDepth] = Math.max( 0,
				field[x % m_iWidth][y % m_iHeight][z % m_iDepth] + amount );
	}
	
	/**
	 * Simulate decay of the chemokine using the m_dDecayRateInv
	 * TODO change to exponential decay
	 */
	public void decay()
	{
		// Adjust for decay
		for ( int x = 0; x < m_iWidth; x++ )
		{
			for ( int y = 0; y < m_iHeight; y++ )
			{
				for ( int z = 0; z < m_iDepth; z++ )
				{
					// add 0.5 so it rounds (1.6 -> 2) instead of just flooring the value (1.6 -> 1)
					// TODO need this bit of code explained
					field[x][y][z] = (field[x][y][z] * Settings.CXCL13.DECAY_CONSTANT);
					//field[x][y][z] = (int) (0.5 + field[x][y][z] * m_dDecayRateInv);
				}
			}
		}
	}
	
	/**
	 * Gets the amount of particle in the immediate vicinity of the given position in a 3x3x3 array
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @return a 3x3x3 array containing the amount in the neighbouring grid spaces
	 */
	public double[][][] getArea( int x, int y, int z )
	{
		double[][][] aiReturn = new double[3][3][3];
		
		
		//TODO well isn't this a bit horrible...
		for ( int r = 0; r < 3; r++ )
		{
			// Check if we're out of bounds
			if ( x + r - 1 < 0 || x + r - 1 >= m_iWidth )
			{
				continue;
			}
			for ( int s = 0; s < 3; s++ )
			{
				// Check if we're out of bounds
				if ( y + s - 1 < 0 || y + s - 1 >= m_iHeight )
				{
					continue;
				}
				for ( int t = 0; t < 3; t++ )
				{
					// Check if we're out of bounds
					if ( z + t - 1 < 0 || z + t - 1 >= m_iDepth )
					{
						continue;
					}
					aiReturn[r][s][t] =  field[x + r - 1][y + s - 1][z + t - 1];
				}
			}
		}
		
		return aiReturn;
	}
	
	/**
	 * Scale the amount of chemokine in a grid space.
	 * NB: does not check if this value is positive
	 * @param x X position on the grid
	 * @param y Y position on the grid
	 * @param z Z position on the grid
	 * @param factor The coefficient of multiplication
	 */
	public void scale( int x, int y, int z, double factor )
	{
		field[x % m_iWidth][y % m_iHeight][z
				% m_iDepth] = (int) (0.5 + field[x % m_iWidth][y % m_iHeight][z % m_iDepth] * factor);
	}
	
	/**
	 * Setter for m_daDiffusionAlgorithm
	 */
	public void setDiffusionAlgorithm( DiffusionAlgorithm daDiffAlg )
	{
		m_daDiffusionAlgorithm = daDiffAlg;
	}
	
	/**
	 * Carries out the decay and diffusion of particles, and updates the 2D display
	 */
	public void step( final SimState state )
	{
	
		
		decay();
		for ( int i = 0; i < Settings.DIFFUSION_STEPS; i++ )
		{
			m_daDiffusionAlgorithm.diffuse( this );
	
		}
		
		updateDisplay();
	}

	/**
	 * Updates the 2D display
	 * TODO: this would be a lot more efficient if the first index was z. Probably not worth it, thinking about it, though.
	 */
	public void updateDisplay()
	{
		for ( int x = 0; x < m_iWidth; x++ )
		{
			for ( int y = 0; y < m_iHeight; y++ )
			{
				m_ig2Display.set( x, y, field[x][y][m_iDisplayLevel] );
			
			}
		}
	}
}
