package sim3d.diffusion;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sim.engine.Schedule;
import sim3d.Settings;

public class ParticleMolesTest {


	/**
	 * test method for update display
	 */
	@Test
	public void testUpdateDisplay() {
		
		Settings.CXCL13.DECAY_CONSTANT = 0.001;
		
		Schedule schedule = new Schedule();
		
		ParticleMoles m_pParticlemoles = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				41, 41, 41);
		
		m_pParticlemoles.field[20][20][1] = 100;


		for(int i =0; i < 5; i++){
			m_pParticlemoles.updateDisplay();
			m_pParticlemoles.step(null);
		}
		
		
		assertTrue(m_pParticlemoles.m_ig2Display.get(20, 20) > 0);
	}
	
	/**
	 * test that the method returns the total
	 * amount of chemokine on the grid
	 */
	@Test
	public void testCalculateTotalChemokineLevels() {
		
		Schedule schedule = new Schedule();
		ParticleMoles m_pParticlemoles = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				41, 41, 41);
		m_pParticlemoles.field[20][20][1] = 100.0;
		
		
		double test = m_pParticlemoles.calculateTotalChemokineLevels();
		assertEquals(100,test,0.1);
	}

}
