package sim3d.diffusion;

import static org.junit.Assert.*;


import org.junit.Test;

import sim.engine.Schedule;
import sim3d.Settings;

public class ChemokineTest {

	/**
	 * test method for update display
	 */
	@Test
	public void testUpdateDisplay() {

		Settings.CXCL13.DECAY_CONSTANT = 0.001;

		Schedule schedule = new Schedule();

		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);

		m_pParticlemoles.field[20][20][1] = 100;

		for (int i = 0; i < 5; i++) {
			m_pParticlemoles.updateDisplay();
			m_pParticlemoles.step(null);
		}

		assertTrue(m_pParticlemoles.m_ig2Display.get(20, 20) > 0);

		// test that the getters and setters for display work as intended
		Chemokine.setDisplayLevel(2);
		assertEquals(Chemokine.m_iDisplayLevel, 2);
	}

	/**
	 * test that the method returns the total amount of chemokine on the grid
	 */
	@Test
	public void testCalculateTotalChemokineLevels() {

		Schedule schedule = new Schedule();
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);
		m_pParticlemoles.field[20][20][1] = 100.0;

		double test = m_pParticlemoles.calculateTotalChemokineLevels();
		assertEquals(100, test, 0.1);
	}

	/**
	 * test that the method returns the total amount of chemokine on the grid
	 */

	@Test
	public void testScale() {

		Schedule schedule = new Schedule();
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 41, 41, 41);

		m_pParticlemoles.field[1][1][1] = 100.0;

		Chemokine.scale(Chemokine.TYPE.CXCL13, 1, 1, 1, 2.0);

		assertEquals(m_pParticlemoles.field[1][1][1], 200, 0.1);
	}

	/**
	 * test that the method returns the total amount of chemokine on the grid
	 */

	@Test
	public void testGetArea() {

		Schedule schedule = new Schedule();
		Chemokine m_pParticlemoles = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 11, 11, 11);

		// check that when no chemokine it returns zero
		double[][][] aiReturn = m_pParticlemoles.getArea(21, 21, 21);
		assertEquals(aiReturn[1][1][1], 0, 0.1);

		// check that with chemokine it returns correct amount
		m_pParticlemoles.field[1][1][1] = 100.0;
		double[][][] aiReturn2 = m_pParticlemoles.getArea(1, 1, 1);
		assertEquals(aiReturn2[1][1][1], 100, 0.1);

	}

}
