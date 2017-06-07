/**
 * 
 */
package sim3d.diffusion.algorithms;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.diffusion.Chemokine;

/**
 * @author simonjarrett, jason cosgrove
 */
public class TestDiffusionAlgorithmMultiThread {

	private Schedule schedule = new Schedule();
	public static Document parameters;

	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
		
		// loadParameters();
		Settings.RNG = new MersenneTwisterFast();
		Settings.CXCL13.DIFFUSION_COEFFICIENT = 7.6e-12;

		Settings.GRID_SIZE = 0.00001;

		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (8.00 * Settings.CXCL13.DIFFUSION_COEFFICIENT));

		//  by 60 as we want to update diffusion in seconds and not
		// minutes
		Settings.CXCL13.DIFFUSION_STEPS = (int) (60 / Settings.CXCL13.DIFFUSION_TIMESTEP);


	}

	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithmOLD#diffuse(sim3d.diffusion.Particle)}
	 */
	@Test
	public void testCourant() {
		// this test requires a little thinking. We need to work out u and
		// whether or not it is too high, but this depends on the time step we
		// use, the
		// max expected amount of chemokine, and the diffusion coefficient.

		double D = Settings.CXCL13.DIFFUSION_COEFFICIENT;
		double t = 0.1; // TODO this needs to be the diffusion timestep

		// double the mean displacement (from the mean square)
		double xMax = Math.sqrt(12 * D * t);

		//test that the distance travelled (displacememt by time, is less than one gridspace)
		// 0.00001 = 10 microns 
		assertThat(xMax * t, is(lessThan(0.00001)));
	}

	
	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithmOLD# diffuse(sim3d.diffusion.Particle)}
	 * 
	 *  
	 *  TODO: fails when you run it in the analysis pipeline but is fine when you run it on its own
	 */
	@Test
	public void testConservation() {

		//need to slow the diffusion constant so stuff doesnt leave the sim
		Settings.CXCL13.DIFFUSION_COEFFICIENT = 7.6e-12;
		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (8.00 * Settings.CXCL13.DIFFUSION_COEFFICIENT));

		//will make it very slow but you need to make sure chemokine cant leave the sim
		//another approach is to dynamically change the boundary condition which would probably be 
		// better
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);// this should be
														// particle moles

		m_pParticlemoles.field[20][20][20] = 100.0;
		m_pParticlemoles.field[21][21][21] = 100.0;

		Settings.CXCL13.DECAY_CONSTANT = 0.0;

		for (int i = 0; i < 10; i++) {
			m_pParticlemoles.step(null);
		}

		double iPartSum = 0;

		for (int x = 0; x < 41; x++) {
			for (int y = 0; y < 41; y++) {
				for (int z = 0; z < 41; z++) {
					iPartSum += m_pParticlemoles.field[x][y][z];
				}
			}
		}

		assertEquals(200, iPartSum, 0.01);
		
		

	}


	@Test
	public void testMeanSquareSingleThread() {
		
		Settings.CXCL13.DIFFUSION_COEFFICIENT = 7.6e-12;
		Settings.GRID_SIZE = 0.00001;
		Settings.CXCL13.DECAY_CONSTANT = 0;
		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (8* Settings.CXCL13.DIFFUSION_COEFFICIENT));
		
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);
		
		//test diffusion using a single core
		DiffusionAlgorithmMultiThread da = new Grajdeanu(Settings.CXCL13.DIFFUSION_COEFFICIENT,
				41, 41, 41,1,Settings.CXCL13.DIFFUSION_TIMESTEP);

		m_pParticlemoles.setDiffusionAlgorithm(da);
		m_pParticlemoles.field[20][20][20] = 1000;
		double iMeanSquare = 0; // = <x^2> when divided my number of particles

		int iNumSteps = 10;
	
		for (int i = 0; i < iNumSteps; i++) {
			m_pParticlemoles.step(null);
		}

		iMeanSquare = 0;
		for (int x = 0; x < 41; x++) {
			for (int y = 0; y < 41; y++) {
				for (int z = 0; z < 41; z++) {
					// squared distance from center space
					iMeanSquare += m_pParticlemoles.field[x][y][z]
							* (Math.pow(Settings.GRID_SIZE * (20 - x), 2)
									+ Math.pow(Settings.GRID_SIZE
											* (20 - y), 2) + Math.pow(
									Settings.GRID_SIZE * (20 - z), 2));
				}
			}
		}
		
		// divide the squared distance by num of particles to
		// get the mean displacement
		iMeanSquare /= 1000;
		
		// assert that D = <x^2>/6t
		// BUT diffusion coefficient is in seconds and not minutes so 
		// you would need to do a conversion...
		assertThat(
				//multiply t by 60 as we want the time in seconds and not minutes
					iMeanSquare / (6 * m_pParticlemoles.getM_diffTime()*60),
					is(closeTo(Settings.CXCL13.DIFFUSION_COEFFICIENT,
							Settings.CXCL13.DIFFUSION_COEFFICIENT / 2)));
		
	}
	
	
	
	@Test
	public void testMeanSquareSingleThread2() {
		
		Settings.CXCL13.DIFFUSION_COEFFICIENT = 20.0e-12;
		Settings.GRID_SIZE = 0.00001;
		Settings.CXCL13.DECAY_CONSTANT = 0;
		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (6.05* Settings.CXCL13.DIFFUSION_COEFFICIENT));
		
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 81, 81, 81);
		
		//test diffusion using a single core
		DiffusionAlgorithmMultiThread da = new Grajdeanu(Settings.CXCL13.DIFFUSION_COEFFICIENT,
				81, 81, 81,1,Settings.CXCL13.DIFFUSION_TIMESTEP);

		m_pParticlemoles.setDiffusionAlgorithm(da);
		m_pParticlemoles.field[40][40][40] = 1000000;
		double iMeanSquare = 0; // = <x^2> when divided my number of particles

		int iNumSteps = 10;
	
		for (int i = 0; i < iNumSteps; i++) {
			m_pParticlemoles.step(null);
		}

		iMeanSquare = 0;
		
		
		for (int x = 0; x < 81; x++) {
			for (int y = 0; y < 81; y++) {
				for (int z = 0; z < 81; z++) {
					// squared distance from center space
					iMeanSquare += m_pParticlemoles.field[x][y][z]
							//multiply by 10 to get it in microns
							* (Math.pow(10 * (40 - x), 2)
									+ Math.pow(10
											* (40 - y), 2) + Math.pow(
									10 * (40 - z), 2));
				}
			}
		}
		// divide the squared distance by num of particles to
		// get the mean displacement
		iMeanSquare /= 1000000;

		System.out.println("mean distance travelled: " + Math.sqrt(iMeanSquare));
		
		assertThat(Math.sqrt(iMeanSquare), is(closeTo(268, 15)));
		
		iMeanSquare = 0;
	
		for (int x = 0; x < 81; x++) {
			for (int y = 0; y < 81; y++) {
				for (int z = 0; z < 81; z++) {
					// squared distance from center space
					iMeanSquare += m_pParticlemoles.field[x][y][z]
							* (Math.pow(Settings.GRID_SIZE * (40 - x), 2)
									+ Math.pow(Settings.GRID_SIZE
											* (40 - y), 2) + Math.pow(
									Settings.GRID_SIZE * (40 - z), 2));
				}
			}
		}
	
		iMeanSquare /= 1000000;
		
		assertThat(
				//multiply t by 60 as we want the time in seconds and not minutes
				//the m diff time should be in minutes already no?
					iMeanSquare / (6 * m_pParticlemoles.getM_diffTime()*60),
					is(closeTo(Settings.CXCL13.DIFFUSION_COEFFICIENT,2.5e-12)));
		
	}
	
	/**
	 * Test method for
	 * {@link sim3d.diffusion.algorithms.DiffusionAlgorithmOLD#diffuse(sim3d.diffusion.Particle)}
	 * Test that the mean squared displacement of the chemokine matches what we
	 * expect
	 * 
	 * If you increase the diffusion constant then you need ot change the iMunSteps variable as otherwise
	 * stuff will diffuse out of the sim...
	 * 
	 * Best to try a range of diffusion constants and a range of iNumSteps to make sure...
	 * 
	 * 
	 * this tests the adaptive diffusion method
	 * 
	 */
	@Test
	public void testMeanSquareMultiThread() {
		
		Settings.CXCL13.DIFFUSION_COEFFICIENT = 7.6e-12;
		Settings.GRID_SIZE = 0.00001;
		Settings.CXCL13.DECAY_CONSTANT = 0;
		Settings.CXCL13.DIFFUSION_TIMESTEP = (Math.pow(Settings.GRID_SIZE, 2) / (8* Settings.CXCL13.DIFFUSION_COEFFICIENT));
		
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);
		
		//test diffusion using multiple cores.
		DiffusionAlgorithmMultiThread da = new Grajdeanu(Settings.CXCL13.DIFFUSION_COEFFICIENT,
				41, 41, 41,4,Settings.CXCL13.DIFFUSION_TIMESTEP);

		m_pParticlemoles.setDiffusionAlgorithm(da);
		m_pParticlemoles.field[20][20][20] = 1000;
		double iMeanSquare = 0; // = <x^2> when divided my number of particles

		int iNumSteps = 10;
	
		for (int i = 0; i < iNumSteps; i++) {
			m_pParticlemoles.step(null);
		}

		iMeanSquare = 0;
		for (int x = 0; x < 41; x++) {
			for (int y = 0; y < 41; y++) {
				for (int z = 0; z < 41; z++) {
					// squared distance from center space
					iMeanSquare += m_pParticlemoles.field[x][y][z]
							* (Math.pow(Settings.GRID_SIZE * (20 - x), 2)
									+ Math.pow(Settings.GRID_SIZE
											* (20 - y), 2) + Math.pow(
									Settings.GRID_SIZE * (20 - z), 2));
				}
			}
		}
		
		// divide the squared distance by num of particles to
		// get the mean displacement
		iMeanSquare /= 1000;
		
		// assert that D = <x^2>/6t
		// BUT diffusion coefficient is in seconds and not minutes so 
		// you would need to do a conversion...
		assertThat(
				//multiply t by 60 as we want the time in seconds and not minutes
					iMeanSquare / (6 * m_pParticlemoles.getM_diffTime()*60),
					is(closeTo(Settings.CXCL13.DIFFUSION_COEFFICIENT,
							Settings.CXCL13.DIFFUSION_COEFFICIENT / 2)));
		
	}

}
