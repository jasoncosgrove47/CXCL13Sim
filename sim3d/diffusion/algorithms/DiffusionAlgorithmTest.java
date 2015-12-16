/**
 * 
 */
package sim3d.diffusion.algorithms;

import static org.junit.Assert.assertEquals;






//import org.hamcrest.number.IsCloseTo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim3d.Options;
import sim3d.SimulationEnvironment;
import sim3d.diffusion.Particle;
import sim3d.util.IO;

/**
 * @author simonjarrett
 *
 */
public class DiffusionAlgorithmTest
{
	private Schedule schedule = new Schedule();
	private Particle m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 81, 81, 81);
	public static Document parameters;	
	
	private static void loadParameters()
	{
		
		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml";		// set the seed for the simulation, be careful for when running on cluster																	
		parameters = IO.openXMLFile(paramFile);
		Options.loadParameters(parameters);
		Options.BC.loadParameters(parameters);
		Options.BC.ODE.loadParameters(parameters);
		Options.FDC.loadParameters(parameters);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		
		//loadParameters();
		Options.RNG = new MersenneTwisterFast();
		Options.DIFFUSION_COEFFICIENT = 1.519 * Math.pow( 10, -10 );
		Options.GRID_SIZE = 0.00001;
		Options.DIFFUSION_TIMESTEP = Math.pow( Options.GRID_SIZE, 2 ) / (3.7 * Options.DIFFUSION_COEFFICIENT);
		Options.DIFFUSION_STEPS	= (int) (1 / Options.DIFFUSION_TIMESTEP);
	}

	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#
	 * diffuse(sim3d.diffusion.Particle)}.
	 * Still not fully sure what this is testing
	 */
	@Test
	public void testConservation()
	{		
		m_pParticle.field[25][25][25] = 100;
		m_pParticle.field[25][26][25] = 100;

		
		m_pParticle.m_dDecayRateInv = 1;
		for ( int i = 0; i < 10; i++ )
		{
			m_pParticle.step(null);
		}
		
		int iPartSum = 0;
		
		for ( int x = 0; x < 51; x++ )
		{
			for ( int y = 0; y < 51; y++ )
			{
				for ( int z = 0; z < 51; z++ )
				{
					iPartSum += m_pParticle.field[x][y][z];
				}
			}
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
		//fail("Test not yet implemented");
		
		double D = Options.DIFFUSION_COEFFICIENT;
		double t = 0.1; //timestep
		
		// double the mean displacement (from the mean square)
		double xMax = Math.sqrt(24*D*t);
		
		// 0.00001 = 10 microns / second
		assertThat(xMax * t, is(lessThan(0.00001)));
	}
	
	/**
	 * Test method for {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}.
	 * 
	 * Test that the mean squared displacement of the chemokine matches what we expect
	 */
	@Test
	public void testMeanSquare()
	{
		m_pParticle.setDiffusionAlgorithm(new Grajdeanu(Options.DIFFUSION_COEFFICIENT, 81,81,81));
		
		m_pParticle.field[40][40][40] = 1000;
		double iMeanSquare = 0;

		int iNumSteps = (int)(80.0/Options.DIFFUSION_STEPS) * Options.DIFFUSION_STEPS;
		
		for ( int i = 0; i < iNumSteps; i++ )
		{
			m_pParticle.step(null);
		
		for ( int x = 0; x < 81; x++ )
		{
			for ( int y = 0; y < 81; y++ )
			{
				for ( int z = 0; z < 81; z++ )
				{
					iMeanSquare += m_pParticle.field[x][y][z] * (Math.pow(Options.GRID_SIZE*(40-x), 2) + Math.pow(Options.GRID_SIZE*(40-y), 2) + Math.pow(Options.GRID_SIZE*(40-z), 2));
				}
			}
		}
		iMeanSquare /= 1000;
		System.out.println((i+1) * Options.DIFFUSION_STEPS * Options.DIFFUSION_TIMESTEP);
		System.out.println(iMeanSquare/(6 * (i+1) * Options.DIFFUSION_STEPS * Options.DIFFUSION_TIMESTEP));
		}

		assertThat(iMeanSquare/(6 * iNumSteps * Options.DIFFUSION_STEPS * Options.DIFFUSION_TIMESTEP), is(closeTo(Options.DIFFUSION_COEFFICIENT, Options.DIFFUSION_COEFFICIENT/2)));
	}
}
