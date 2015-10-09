/**
 * 
 */
package sim3d.diffusion.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim3d.Options;
import sim3d.diffusion.Particle;

/**
 * @author simonjarrett
 *
 */
public class DiffusionAlgorithmTest
{
	private Schedule schedule = new Schedule();
	private Particle m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 23, 23, 23);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		Options.RNG = new MersenneTwisterFast();
	}

	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 */
	@Test
	public void testConservation() {		
		m_pParticle.field[11][11][11] = 100;
		m_pParticle.field[11][12][11] = 100;
		
		int iPartSum = 0;
		
		// Sanity checking. Make sure the particle field is empty
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					iPartSum += m_pParticle.field[x][y][z];
				}
			}
		}
		
		m_pParticle.m_dDecayRateInv = 1;
		
		for ( int i = 0; i < 10; i++ )
		{
			m_pParticle.step(null);
		}
		
		assertEquals(200, iPartSum);
	}
	
	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 */
	@Test
	public void testCourant()
	{	
		// TODO this test requires a little thinking. We need to work out u and whether
		// or not it is too high, but this depends on the time step we use, the max expected
		// amount of chemokine, and the diffusion coefficient.
		fail("Test not yet implemented");
		
		m_pParticle.setDiffusionAlgorithm(new Grajdeanu(1.0, 23,23,23));
		
		m_pParticle.field[11][11][11] = 1;
		
		int iPartSum = 0;
		
		// Sanity checking. Make sure the particle field is empty
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					iPartSum += m_pParticle.field[x][y][z];
				}
			}
		}
		
		m_pParticle.m_dDecayRateInv = 1;
		
		for ( int i = 0; i < 60; i++ )
		{
			m_pParticle.step(null);
		}
		
		assertEquals(1, iPartSum);
	}
	
	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 */
	@Test
	public void testMeanSquare()
	{
		m_pParticle.setDiffusionAlgorithm(new Grajdeanu(1.0, 23,23,23));
		
		m_pParticle.field[11][11][11] = 1000;

		for ( int i = 0; i < 10; i++ )
		{
			m_pParticle.step(null);
		}
		
		int iMeanSquare = 0;
		
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					iMeanSquare += m_pParticle.field[x][y][z] * ((11-x)*(11-x) + (11-y)*(11-y) + (11-z)*(11-z));
				}
			}
		}
		
		assertEquals(1, iMeanSquare);
	}
}
