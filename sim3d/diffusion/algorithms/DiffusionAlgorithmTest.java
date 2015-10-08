/**
 * 
 */
package sim3d.diffusion.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import sim.engine.Schedule;
import sim3d.diffusion.Particle;

/**
 * @author simonjarrett
 *
 */
public class DiffusionAlgorithmTest {

	private static final double DELTA = 1e-15;
	
	private Schedule schedule = new Schedule();
	private Particle m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 23, 23, 23);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 */
	@Test
	public void testConservation() {		
		m_pParticle.field[11][11][11] = 1;
		
		double dPartSum = 0.0;
		
		// Sanity checking. Make sure the particle field is empty
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					dPartSum += m_pParticle.field[x][y][z];
				}
			}
		}
		
		m_pParticle.m_dDecayRateInv = 1;
		
		for ( int i = 0; i < 60; i++ )
		{
			m_pParticle.step(null);
		}
		
		assertEquals(dPartSum, 1.0, DELTA);
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
		
		double dPartSum = 0.0;
		
		// Sanity checking. Make sure the particle field is empty
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					dPartSum += m_pParticle.field[x][y][z];
				}
			}
		}
		
		m_pParticle.m_dDecayRateInv = 1;
		
		for ( int i = 0; i < 60; i++ )
		{
			m_pParticle.step(null);
		}
		
		assertEquals(dPartSum, 1.0, DELTA);
	}
	
	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 */
	@Test
	public void testMeanSquare()
	{
		m_pParticle.setDiffusionAlgorithm(new Grajdeanu(1.0, 23,23,23));
		
		m_pParticle.field[11][11][11] = 1;

		for ( int i = 0; i < 10; i++ )
		{
			m_pParticle.step(null);
		}
		
		double dMeanSquare = 0;
		
		for ( int x = 0; x < 23; x++ )
		{
			for ( int y = 0; y < 23; y++ )
			{
				for ( int z = 0; z < 23; z++ )
				{
					dMeanSquare += m_pParticle.field[x][y][z] * ((11-x)*(11-x) + (11-y)*(11-y) + (11-z)*(11-z));
				}
			}
		}
		
		assertEquals(1.0, dMeanSquare, DELTA);
	}
}
