/**
 * 
 */
package acceptanceTests;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.cell.StromaEdge;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.IO;
import sim3d.util.FRCStromaGenerator.FRCCell;


/**
 * Test some basic functionality of the FDC network
 */
public class FDCTest {
	


	@Before
	public void setUp() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		// Initialise the stromal grid
		Continuous3D fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				60, 60, 10);
		
		
	}

	/*
	 * Make sure that the FDC can display antigen
	 */
	@Test
	public void canDisplayAntigen() {
		
		ArrayList<FRCCell> d3lCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		
		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		for (StromaEdge seEdge : selEdges) {
			seEdge.getAntigen();
			assertThat(seEdge.getAntigen(), greaterThan(0));
		}
	}

	
	/**
	 * Make sure that the FDC can lose antigen
	 */
	@Test
	public void canLoseAntigen(){
		
		ArrayList<FRCCell> d3lCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		
		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		for (StromaEdge seEdge : selEdges) {
			seEdge.setAntigenLevelLowerHalf(seEdge.getAntigen());
			assertThat(seEdge.getAntigen(), lessThan(100));
		}
		
		
	}

	
	@Test
	public void testCXCL13SECRETING() {
		

		
		Schedule schedule = new Schedule();


		Continuous3D fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				60, 60, 10);
		
		FDC.drawEnvironment = fdcEnvironment;
	
		ParticleMoles m_pParticle = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				60, 60, 10);
		
		Settings.FDC.CXCL13_EMITTED = 100;
		
		FDC fdc = new FDC();
		
		fdc.setObjectLocation( new Double3D(15,15, 5 ));

		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
	
		
		double[][][] chemokine = ParticleMoles.get(ParticleMoles.TYPE.CXCL13,
				15, 15, 5);
		
	
		assertThat(chemokine[1][1][1], greaterThan(0.0));
		
	
		ParticleMoles.reset();
		FDC.drawEnvironment = null;

	}





}
