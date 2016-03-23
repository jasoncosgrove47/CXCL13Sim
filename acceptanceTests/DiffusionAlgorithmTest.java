/**
 * 
 */
package acceptanceTests;

import static org.junit.Assert.assertEquals;


//import org.hamcrest.number.IsCloseTo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim3d.Settings;
import sim3d.diffusion.Particle;
import sim3d.diffusion.algorithms.DiffusionAlgorithm;
import sim3d.diffusion.algorithms.Grajdeanu;
import sim3d.util.IO;

/**
 * @author simonjarrett
 * 
 */
public class DiffusionAlgorithmTest {
	private Schedule schedule = new Schedule();
	private Particle m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13,
			41, 41, 41);// this should be particle moles
	public static Document parameters;

	private static void loadParameters() {

		String paramFile = "/Users/jc1571/Dropbox/LymphSim/Simulation/LymphSimParameters.xml"; 
																								
		parameters = IO.openXMLFile(paramFile);
		Settings.loadParameters(parameters);
		Settings.BC.loadParameters(parameters);
		Settings.BC.ODE.loadParameters(parameters);
		Settings.FDC.loadParameters(parameters);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		// loadParameters();
		Settings.RNG = new MersenneTwisterFast();
		Settings.DIFFUSION_COEFFICIENT = 7.6e-12;

		Settings.GRID_SIZE = 0.00001;

		Settings.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2)
				/ (40.00 * Settings.DIFFUSION_COEFFICIENT));// was 40.15, 10 gives a near enough value

		//multiply by 60 as we want to update diffusion in seconds and not minutes
		Settings.DIFFUSION_STEPS = (int) (1 / Settings.DIFFUSION_TIMESTEP);
		

		System.out.println("coefficient: " + Settings.DIFFUSION_COEFFICIENT
				+ "timestep: " + Settings.DIFFUSION_STEPS + "steps: "
				+ Settings.DIFFUSION_TIMESTEP);

	}

	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithm# diffuse(sim3d.diffusion.Particle)}
	 */
	@Test
	public void testConservation() {
		m_pParticle.field[20][20][20] = 100;
		m_pParticle.field[21][21][21] = 100;

		Settings.CXCL13.DECAY_CONSTANT = 1;

		for (int i = 0; i < 10; i++) {
			m_pParticle.step(null);
		}

		int iPartSum = 0;

		for (int x = 0; x < 41; x++) {
			for (int y = 0; y < 41; y++) {
				for (int z = 0; z < 41; z++) {
					iPartSum += m_pParticle.field[x][y][z];
				}
			}
		}

		assertEquals(200, iPartSum);
	}

	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}
	 */
	@Test
	public void testCourant() {
		// TODO this test requires a little thinking. We need to work out u and
		// whether or not it is too high, but this depends on the time step we use, the
		// max expected amount of chemokine, and the diffusion coefficient.

		double D = Settings.DIFFUSION_COEFFICIENT;
		double t = 0.1; // timestep

		// double the mean displacement (from the mean square)
		double xMax = Math.sqrt(24 * D * t);

		// 0.00001 = 10 microns / second
		assertThat(xMax * t, is(lessThan(0.00001)));
	}

	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithm#diffuse(sim3d.diffusion.Particle)}
	 * Test that the mean squared displacement of the chemokine matches what we expect
	 * 
	 * This won't pass everytime because the algorithm needs to be updated 
	 * 
	 * Don't trust this as the other one passes even with a ridiculous
	 * diffusion pattern
	 * 
	 * might do my own version of this test
	 * 
	 */
	@Test
	public void testMeanSquare() {

		Settings.GRID_SIZE = 0.00001;
		Settings.CXCL13.DECAY_CONSTANT = 1;
		
		Settings.DIFFUSION_STEPS = 20;
		
		DiffusionAlgorithm da = new Grajdeanu(
				Settings.DIFFUSION_COEFFICIENT, 41, 41, 41);
		
		m_pParticle.setDiffusionAlgorithm(da);

		m_pParticle.field[20][20][20] = 1000;
		double iMeanSquare = 0; // = <x^2> when divided my number of particles

		
		//calculates teh number of times needed to loop
		int iNumSteps = (int) (20.0 / Settings.DIFFUSION_STEPS)
				* Settings.DIFFUSION_STEPS;
		
		//TODO i should probably just reset the diffusion steps here no?

		for (int i = 0; i < iNumSteps; i++) {
			m_pParticle.step(null);

			iMeanSquare = 0;
			for (int x = 0; x < 41; x++) {
				for (int y = 0; y < 41; y++) {
					for (int z = 0; z < 41; z++) {
						
						//squared distance from center space
						//multiply by 10 because we want the distance in microns
						iMeanSquare += m_pParticle.field[x][y][z]
								* (Math.pow(Settings.GRID_SIZE * (20 - x), 2)
										+ Math.pow(Settings.GRID_SIZE
												* (20 - y), 2) + Math.pow(
										Settings.GRID_SIZE * (20 - z), 2));
					}
				}
			}
			
			
			
			//divide the squared distance by num of particles to
			// get the mean displacement
			iMeanSquare /= 1000;

		}

		
		
		
		//assert that D = <x^2>/6t
		assertThat(
				iMeanSquare
						/ (6 * iNumSteps * Settings.DIFFUSION_STEPS * Settings.DIFFUSION_TIMESTEP),
				is(closeTo(Settings.DIFFUSION_COEFFICIENT,
						Settings.DIFFUSION_COEFFICIENT / 2)));
	}
	
	
	/**
	 * New test mean squared
	 * pick a random grid space 50 microns away from chemokine source
	 * 
	 */
	
	
	
	
	
	
	
}
